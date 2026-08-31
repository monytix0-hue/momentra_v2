/**
 * S9-G + S9-H scale seed + measure — diagnose first (no optimization patches).
 * Small: API-faithful. Medium/Large volume: bulk SQL for historical rows + live API samples.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool, prewarmPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { clearKnownUserProfiles } from '../src/platform/auth';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

type Tier = 'small' | 'medium' | 'large';

const GROUP_TIERS: Record<Tier, { members: number; expenses: number }> = {
  small: { members: 5, expenses: 25 },
  medium: { members: 25, expenses: 500 },
  large: { members: 100, expenses: 5000 },
};

const BUSINESS_TIERS: Record<Tier, { companies: number; moments: number; rows: number }> = {
  small: { companies: 1, moments: 2, rows: 100 },
  medium: { companies: 3, moments: 10, rows: 2000 },
  large: { companies: 10, moments: 50, rows: 10000 },
};

function pct(sorted: number[], p: number): number {
  if (!sorted.length) return 0;
  const i = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return Math.round(sorted[i]! * 100) / 100;
}

function summarize(label: string, samples: number[], extra: Record<string, unknown> = {}) {
  const sorted = [...samples].sort((a, b) => a - b);
  return {
    label,
    n: sorted.length,
    p50: pct(sorted, 50),
    p95: pct(sorted, 95),
    p99: pct(sorted, 99),
    min: Math.round((sorted[0] ?? 0) * 100) / 100,
    max: Math.round((sorted[sorted.length - 1] ?? 0) * 100) / 100,
    ...extra,
  };
}

function authFor(uid: string) {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

async function sampleGet(
  path: string,
  headers: Record<string, string>,
  n: number,
): Promise<ReturnType<typeof summarize> & { status: number; approxBodyBytes: number; positionRows?: number }> {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBytes = 0;
  let positionRows: number | undefined;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app).get(path).set(headers);
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBytes = JSON.stringify(res.body).length;
    const positions = res.body?.data?.payload?.positions ?? res.body?.data?.positions;
    if (Array.isArray(positions)) positionRows = positions.length;
  }
  return summarize(path, samples, { status: lastStatus, approxBodyBytes: lastBytes, positionRows });
}

async function timedPost(
  path: string,
  headers: Record<string, string>,
  body: unknown,
  n: number,
) {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBytes = 0;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app)
      .post(path)
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send(body);
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBytes = JSON.stringify(res.body).length;
  }
  return summarize(path, samples, { status: lastStatus, approxBodyBytes: lastBytes });
}

async function ensureOwner(uid: string) {
  const headers = authFor(uid);
  const me = await request(app).get('/v1/me').set(headers);
  if (me.status !== 200) throw new Error(`me failed ${me.status}`);
  return { headers, userId: me.body.data.userId as string };
}

async function seedGroupMembers(momentId: string, countExtra: number): Promise<string[]> {
  const pool = getPool();
  // External-party participants as ACTIVE (fixture-only; not production invite path)
  const ids: string[] = [];
  for (let i = 0; i < countExtra; i++) {
    const party = await pool.query<{ external_party_id: string }>(
      `INSERT INTO core.external_party (party_type, display_name, status)
       VALUES ('PERSON', $1, 'ACTIVE') RETURNING external_party_id`,
      [`S9G Member ${i + 1}`],
    );
    const mp = await pool.query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, external_party_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
       RETURNING participant_id`,
      [momentId, party.rows[0]!.external_party_id],
    );
    ids.push(mp.rows[0]!.participant_id);
  }
  return ids;
}

async function listParticipantIds(momentId: string): Promise<string[]> {
  const r = await getPool().query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE' ORDER BY joined_at NULLS LAST, participant_id`,
    [momentId],
  );
  return r.rows.map((x) => x.participant_id);
}

/** Bulk historical expenses + projections (set-based; measure-only volume). */
async function bulkSeedGroupExpenses(
  momentId: string,
  ownerUserId: string,
  paidByParticipantId: string,
  participantIds: string[],
  count: number,
) {
  const pool = getPool();
  const currency = 'USD';
  const amount = 100;
  const shareTargets = participantIds.slice(0, Math.min(participantIds.length, 10));
  const share = Math.floor((amount / shareTargets.length) * 10000) / 10000;

  // Events + expenses in one shot
  await pool.query(
    `WITH ev AS (
       INSERT INTO events.domain_event (
         event_name, domain_code, aggregate_type, aggregate_id,
         scope_type, scope_id, actor_user_id, payload, correlation_id, occurred_at
       )
       SELECT 'GroupExpenseRecorded', 'GROUP', 'EXPENSE', gen_random_uuid(),
              'MOMENT', $1::uuid, $2::uuid, '{}'::jsonb, gen_random_uuid(),
              now() - (g.i || ' seconds')::interval
       FROM generate_series(1, $3) AS g(i)
       RETURNING domain_event_id, aggregate_id
     ),
     exp AS (
       INSERT INTO finance.expense (
         expense_id, moment_id, domain_code, created_by_user_id, description,
         amount, currency_code, effective_at, status, posted_at, version
       )
       SELECT aggregate_id, $1::uuid, 'GROUP', $2::uuid, 'S9G bulk', $4, $5, now(), 'POSTED', now(), 1
       FROM ev
       RETURNING expense_id
     ),
     ctx AS (
       INSERT INTO finance.group_expense_context (expense_id, moment_id, paid_by_participant_id)
       SELECT expense_id, $1::uuid, $6::uuid FROM exp
     ),
     act AS (
       INSERT INTO projection.recent_activity (
         user_id, source_event_id, domain_code, scope_type, scope_id,
         activity_code, title, occurred_at, activity_payload, projection_version
       )
       SELECT $2::uuid, domain_event_id, 'GROUP', 'MOMENT', $1::uuid,
              'GROUP_EXPENSE_RECORDED', 'S9G bulk', now(), '{}'::jsonb, 1
       FROM ev
       ON CONFLICT DO NOTHING
     )
     SELECT COUNT(*)::int AS n FROM exp`,
    [momentId, ownerUserId, count, amount.toFixed(4), currency, paidByParticipantId],
  );

  // Shares for capped participant set × all expenses
  await pool.query(
    `INSERT INTO finance.expense_share (expense_id, moment_id, participant_id, share_amount, status)
     SELECT e.expense_id, $1::uuid, p.participant_id, $2::numeric, 'ALLOCATED'
     FROM finance.expense e
     CROSS JOIN UNNEST($3::uuid[]) AS p(participant_id)
     WHERE e.moment_id = $1 AND e.description = 'S9G bulk'`,
    [momentId, share.toFixed(4), shareTargets],
  );

  await pool.query(
    `INSERT INTO finance.participant_obligation (
       moment_id, participant_id, source_type, source_id, currency_code,
       original_amount, settled_amount, status, version
     )
     SELECT $1::uuid, es.participant_id, 'EXPENSE_SHARE', es.expense_share_id, $2,
            es.share_amount, 0, 'OPEN', 1
     FROM finance.expense_share es
     JOIN finance.expense e ON e.expense_id = es.expense_id
     WHERE e.moment_id = $1 AND e.description = 'S9G bulk'
       AND es.participant_id <> $3::uuid`,
    [momentId, currency, paidByParticipantId],
  );

  await pool.query(
    `INSERT INTO projection.group_finance_snapshot (
       moment_id, currency_code, expense_total, outstanding_total,
       snapshot_payload, projection_version
     ) VALUES ($1, $2, $3, $3, jsonb_build_object('expenseCount', $4::int), 1)
     ON CONFLICT (moment_id, currency_code) DO UPDATE SET
       expense_total = EXCLUDED.expense_total,
       outstanding_total = EXCLUDED.outstanding_total,
       snapshot_payload = EXCLUDED.snapshot_payload,
       projection_version = projection.group_finance_snapshot.projection_version + 1,
       updated_at = now()`,
    [momentId, currency, (amount * count).toFixed(4), count],
  );

  for (const pid of participantIds) {
    await pool.query(
      `INSERT INTO projection.group_finance_position (
         moment_id, participant_id, currency_code,
         paid_total, allocated_total, payable_total, receivable_total, net_position, projection_version
       ) VALUES ($1, $2, $3, $4, $5, $5, 0, 0, 1)
       ON CONFLICT (moment_id, participant_id, currency_code) DO UPDATE SET
         paid_total = EXCLUDED.paid_total,
         allocated_total = EXCLUDED.allocated_total,
         payable_total = EXCLUDED.payable_total,
         projection_version = projection.group_finance_position.projection_version + 1,
         updated_at = now()`,
      [
        momentId,
        pid,
        currency,
        pid === paidByParticipantId ? (amount * count).toFixed(4) : '0',
        (share * count).toFixed(4),
      ],
    );
  }
}

async function seedGroupTier() {
  const results: Record<string, unknown> = {};
  for (const tier of ['small', 'medium', 'large'] as Tier[]) {
    const spec = GROUP_TIERS[tier];
    const uid = `s9g-${tier}-${randomUUID().slice(0, 6)}`;
    const { headers, userId } = await ensureOwner(uid);

    const created = await request(app)
      .post('/v1/moments')
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: `S9G ${tier}` });
    if (created.status !== 201 && created.status !== 200) {
      throw new Error(`create moment ${tier} ${created.status} ${JSON.stringify(created.body)}`);
    }
    const momentId = created.body.data.momentId as string;

    await seedGroupMembers(momentId, spec.members - 1);
    const participantIds = await listParticipantIds(momentId);
    const paidBy = participantIds[0]!;

    const seedT0 = performance.now();
    if (tier === 'small') {
      for (let i = 0; i < spec.expenses; i++) {
        const res = await request(app)
          .post(`/v1/moments/${momentId}/group-expenses`)
          .set({ ...headers, 'Idempotency-Key': randomUUID() })
          .send({
            amount: '25.00',
            currencyCode: 'USD',
            description: `S9G small ${i}`,
            paidByParticipantId: paidBy,
            splitStrategy: 'EQUAL',
            splitInputs: participantIds.map((participantId) => ({ participantId })),
          });
        if (res.status >= 400) throw new Error(`expense small ${res.status} ${JSON.stringify(res.body)}`);
      }
    } else {
      await bulkSeedGroupExpenses(momentId, userId, paidBy, participantIds, spec.expenses);
    }
    const seedMs = Math.round(performance.now() - seedT0);

    // Live write samples (always API)
    const expenseSubmit = await timedPost(
      `/v1/moments/${momentId}/group-expenses`,
      headers,
      {
        amount: '10.00',
        currencyCode: 'USD',
        description: 'S9G live sample',
        paidByParticipantId: paidBy,
        splitStrategy: 'EQUAL',
        splitInputs: participantIds.map((participantId) => ({ participantId })),
      },
      tier === 'large' ? 3 : 5,
    );

    const pulse = await sampleGet(`/v1/group/moments/${momentId}/pulse`, headers, 8);
    const finance = await sampleGet(`/v1/group/moments/${momentId}/finance`, headers, 5);
    const activity1 = await sampleGet(`/v1/group/moments/${momentId}/activity?limit=20`, headers, 8);
    const actPage = await request(app)
      .get(`/v1/group/moments/${momentId}/activity?limit=20`)
      .set(headers);
    const cursor = actPage.body?.data?.nextCursor as string | null | undefined;
    const activity2 = cursor
      ? await sampleGet(
          `/v1/group/moments/${momentId}/activity?limit=20&cursor=${encodeURIComponent(cursor)}`,
          headers,
          5,
        )
      : { note: 'no nextCursor' };
    const participants = await sampleGet(`/v1/group/moments/${momentId}/participants`, headers, 5);

    // Second moment for switch
    const m2 = await request(app)
      .post('/v1/moments')
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: `S9G ${tier} switch` });
    const momentId2 = m2.body.data.momentId as string;
    const switchSamples: number[] = [];
    for (let i = 0; i < 5; i++) {
      const t0 = performance.now();
      await request(app).get(`/v1/group/moments/${momentId}/pulse`).set(headers);
      await request(app).get(`/v1/group/moments/${momentId2}/pulse`).set(headers);
      switchSamples.push(performance.now() - t0);
    }

    // Invite redeem (small/medium only — correctness path)
    let inviteRedeem: unknown = { skipped: tier === 'large' };
    if (tier !== 'large') {
      const mint = await request(app)
        .post('/v1/group/invites')
        .set({ ...headers, 'Idempotency-Key': randomUUID() })
        .send({ title: `S9G invite ${tier}`, momentTypeCode: 'TRIP' });
      if (mint.status < 400) {
        const code = mint.body.data.code ?? mint.body.data.inviteCode;
        const momentFromInvite = mint.body.data.momentId;
        const guestUid = `s9g-guest-${randomUUID().slice(0, 6)}`;
        const guest = await ensureOwner(guestUid);
        const t0 = performance.now();
        const redeemed = await request(app)
          .post(`/v1/group/invites/${code}/redeem`)
          .set({ ...guest.headers, 'Idempotency-Key': randomUUID() })
          .send({});
        inviteRedeem = {
          mintStatus: mint.status,
          redeemStatus: redeemed.status,
          ms: Math.round(performance.now() - t0),
          momentId: momentFromInvite,
        };
      } else {
        inviteRedeem = { mintStatus: mint.status, body: mint.body };
      }
    }

    const counts = await getPool().query(
      `SELECT
         (SELECT COUNT(*)::int FROM collaboration.moment_participant WHERE moment_id = $1) AS members,
         (SELECT COUNT(*)::int FROM finance.expense WHERE moment_id = $1) AS expenses,
         (SELECT COUNT(*)::int FROM projection.group_finance_position WHERE moment_id = $1) AS positions,
         (SELECT COUNT(*)::int FROM projection.recent_activity WHERE scope_id = $1) AS activity`,
      [momentId],
    );

    results[tier] = {
      spec,
      momentId,
      seedMs,
      seedMode: tier === 'small' ? 'api' : 'bulk_sql+api_samples',
      counts: counts.rows[0],
      measures: {
        pulse,
        finance,
        expenseSubmit,
        activity1,
        activity2,
        participants,
        momentSwitchPair: summarize('moment_switch_pair', switchSamples),
        inviteRedeem,
      },
    };
  }
  return results;
}

async function seedBusinessTier() {
  const results: Record<string, unknown> = {};
  for (const tier of ['small', 'medium', 'large'] as Tier[]) {
    const spec = BUSINESS_TIERS[tier];
    const uid = `s9h-${tier}-${randomUUID().slice(0, 6)}`;
    const { headers, userId } = await ensureOwner(uid);

    const companies: string[] = [];
    const moments: string[] = [];
    const seedT0 = performance.now();

    for (let c = 0; c < spec.companies; c++) {
      const co = await request(app)
        .post('/v1/companies')
        .set({ ...headers, 'Idempotency-Key': randomUUID() })
        .send({ displayName: `S9H ${tier} Co ${c}`, legalName: `S9H ${tier} Legal ${c}` });
      if (co.status >= 400) throw new Error(`company ${co.status} ${JSON.stringify(co.body)}`);
      const companyId = co.body.data.companyId as string;
      companies.push(companyId);

      await request(app)
        .post(`/v1/companies/${companyId}/teams`)
        .set({ ...headers, 'Idempotency-Key': randomUUID() })
        .send({ name: `Team ${c}` });

      await request(app)
        .post(`/v1/companies/${companyId}/vendors`)
        .set({ ...headers, 'Idempotency-Key': randomUUID() })
        .send({ name: `Vendor ${c}` });
    }

    const momentsPerCompany = Math.ceil(spec.moments / spec.companies);
    for (let i = 0; i < spec.moments; i++) {
      const companyId = companies[i % companies.length]!;
      const m = await request(app)
        .post('/v1/moments')
        .set({ ...headers, 'Idempotency-Key': randomUUID() })
        .send({
          domainCode: 'BUSINESS',
          companyId,
          momentTypeCode: 'TEAM_OPERATIONS',
          title: `S9H ${tier} M${i}`,
        });
      if (m.status >= 400) throw new Error(`biz moment ${m.status} ${JSON.stringify(m.body)}`);
      moments.push(m.body.data.momentId as string);
    }

    const primaryMoment = moments[0]!;
    const rowsPerMoment = Math.ceil(spec.rows / moments.length);

    if (tier === 'small') {
      for (let i = 0; i < rowsPerMoment; i++) {
        const kind = i % 3;
        if (kind === 0) {
          await request(app)
            .post(`/v1/moments/${primaryMoment}/business-expenses`)
            .set({ ...headers, 'Idempotency-Key': randomUUID() })
            .send({ amount: '12.50', currencyCode: 'USD', description: `exp ${i}` });
        } else if (kind === 1) {
          await request(app)
            .post(`/v1/moments/${primaryMoment}/revenues`)
            .set({ ...headers, 'Idempotency-Key': randomUUID() })
            .send({ amount: '40.00', currencyCode: 'USD', description: `rev ${i}` });
        } else {
          await request(app)
            .post(`/v1/moments/${primaryMoment}/invoices`)
            .set({ ...headers, 'Idempotency-Key': randomUUID() })
            .send({
              invoiceNumber: `INV-${tier}-${i}`,
              invoiceDate: new Date().toISOString().slice(0, 10),
              currencyCode: 'USD',
              lines: [{ description: 'Line', quantity: '1', unitPrice: '99.00' }],
            });
        }
      }
    } else {
      // Bulk activity + expense rows across moments (set-based)
      for (const momentId of moments) {
        await getPool().query(
          `WITH ev AS (
             INSERT INTO events.domain_event (
               event_name, domain_code, aggregate_type, aggregate_id,
               scope_type, scope_id, actor_user_id, payload, correlation_id, occurred_at
             )
             SELECT 'BusinessExpenseRecorded', 'BUSINESS', 'EXPENSE', gen_random_uuid(),
                    'MOMENT', $1::uuid, $2::uuid, '{}'::jsonb, gen_random_uuid(),
                    now() - (g.i || ' seconds')::interval
             FROM generate_series(1, $3) AS g(i)
             RETURNING domain_event_id, aggregate_id
           ),
           exp AS (
             INSERT INTO finance.expense (
               expense_id, moment_id, domain_code, created_by_user_id, description,
               amount, currency_code, effective_at, status, posted_at, version
             )
             SELECT aggregate_id, $1::uuid, 'BUSINESS', $2::uuid, 'S9H bulk', 10, 'USD', now(), 'POSTED', now(), 1
             FROM ev
           ),
           act AS (
             INSERT INTO projection.recent_activity (
               user_id, source_event_id, domain_code, scope_type, scope_id,
               activity_code, title, occurred_at, activity_payload, projection_version
             )
             SELECT $2::uuid, domain_event_id, 'BUSINESS', 'MOMENT', $1::uuid,
                    'BUSINESS_EXPENSE', 'S9H bulk', now(), '{}'::jsonb, 1
             FROM ev
             ON CONFLICT DO NOTHING
           )
           SELECT 1`,
          [momentId, userId, rowsPerMoment],
        );
      }
    }
    const seedMs = Math.round(performance.now() - seedT0);

    const pulse = await sampleGet(`/v1/business/moments/${primaryMoment}/pulse`, headers, 8);
    const activity1 = await sampleGet(`/v1/business/moments/${primaryMoment}/activity?limit=20`, headers, 8);
    const actPage = await request(app)
      .get(`/v1/business/moments/${primaryMoment}/activity?limit=20`)
      .set(headers);
    const cursor = actPage.body?.data?.nextCursor as string | null | undefined;
    const activity2 = cursor
      ? await sampleGet(
          `/v1/business/moments/${primaryMoment}/activity?limit=20&cursor=${encodeURIComponent(cursor)}`,
          headers,
          5,
        )
      : { note: 'no nextCursor' };

    const expenseSubmit = await timedPost(
      `/v1/moments/${primaryMoment}/business-expenses`,
      headers,
      { amount: '15.00', currencyCode: 'USD', description: 'S9H live' },
      5,
    );
    const revenueSubmit = await timedPost(
      `/v1/moments/${primaryMoment}/revenues`,
      headers,
      { amount: '20.00', currencyCode: 'USD', description: 'S9H live rev' },
      3,
    );
    const invoiceSubmit = await timedPost(
      `/v1/moments/${primaryMoment}/invoices`,
      headers,
      {
        invoiceNumber: `INV-LIVE-${tier}-${randomUUID().slice(0, 4)}`,
        invoiceDate: new Date().toISOString().slice(0, 10),
        currencyCode: 'USD',
        lines: [{ description: 'Live', quantity: '1', unitPrice: '50.00' }],
      },
      3,
    );

    // Approval if invoice created an approval id
    let approval: unknown = { note: 'no approval id on invoice response' };
    const inv = await request(app)
      .post(`/v1/moments/${primaryMoment}/invoices`)
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({
        invoiceNumber: `INV-APR-${tier}-${randomUUID().slice(0, 4)}`,
        invoiceDate: new Date().toISOString().slice(0, 10),
        currencyCode: 'USD',
        lines: [{ description: 'Apr', quantity: '1', unitPrice: '10.00' }],
      });
    const approvalId =
      inv.body?.data?.approvalRequestId ?? inv.body?.data?.approvalId ?? null;
    if (approvalId) {
      approval = await timedPost(
        `/v1/approvals/${approvalId}/decide`,
        headers,
        { decision: 'APPROVE' },
        1,
      );
    } else {
      approval = { invoiceStatus: inv.status, keys: Object.keys(inv.body?.data ?? {}) };
    }

    const members = await sampleGet(`/v1/companies/${companies[0]}/members`, headers, 5);
    const teams = await sampleGet(`/v1/companies/${companies[0]}/teams`, headers, 5);
    const companyList = await sampleGet('/v1/companies', headers, 5);
    const meBootstrap = await sampleGet('/v1/me', headers, 5);

    // Company / moment switch
    const switchSamples: number[] = [];
    if (moments.length >= 2) {
      for (let i = 0; i < 5; i++) {
        const t0 = performance.now();
        await request(app).get(`/v1/business/moments/${moments[0]}/pulse`).set(headers);
        await request(app).get(`/v1/business/moments/${moments[1]}/pulse`).set(headers);
        switchSamples.push(performance.now() - t0);
      }
    }

    const companySwitchSamples: number[] = [];
    if (companies.length >= 2 && moments.length >= 2) {
      const mA = moments[0]!;
      const mB = moments.find((_, idx) => companies[idx % companies.length] !== companies[0]) ?? moments[1]!;
      for (let i = 0; i < 5; i++) {
        const t0 = performance.now();
        await request(app).get(`/v1/companies/${companies[0]}`).set(headers);
        await request(app).get(`/v1/business/moments/${mA}/pulse`).set(headers);
        await request(app).get(`/v1/companies/${companies[1]}`).set(headers);
        await request(app).get(`/v1/business/moments/${mB}/pulse`).set(headers);
        companySwitchSamples.push(performance.now() - t0);
      }
    }

    results[tier] = {
      spec,
      companies,
      primaryMoment,
      momentCount: moments.length,
      seedMs,
      seedMode: tier === 'small' ? 'api' : 'bulk_sql+api_samples',
      measures: {
        pulse,
        activity1,
        activity2,
        expenseSubmit,
        revenueSubmit,
        invoiceSubmit,
        approval,
        members,
        teams,
        companyList,
        meBootstrap,
        momentSwitchPair: switchSamples.length
          ? summarize('moment_switch_pair', switchSamples)
          : { note: 'need 2 moments' },
        companySwitch: companySwitchSamples.length
          ? summarize('company_switch', companySwitchSamples)
          : { note: 'need 2 companies' },
      },
    };
  }
  return results;
}

function classifyGrowth(label: string, smallP95: number, medP95: number, largeP95: number) {
  const ratioML = largeP95 / Math.max(smallP95, 1);
  const ratioMM = medP95 / Math.max(smallP95, 1);
  let shape: 'flat' | 'linear' | 'super_linear' | 'projection_bounded' = 'flat';
  if (ratioML < 1.5 && ratioMM < 1.5) shape = 'flat';
  else if (ratioML < 3) shape = 'linear';
  else shape = 'super_linear';
  // Positions/payload bounded by members not expenses → note separately in report
  return { label, smallP95, medP95, largeP95, ratioMedVsSmall: Math.round(ratioMM * 100) / 100, ratioLargeVsSmall: Math.round(ratioML * 100) / 100, shape };
}

async function main() {
  clearKnownUserProfiles();
  await prewarmPool(config.database.poolMin);

  console.error('Seeding+measuring Group tiers...');
  const group = await seedGroupTier();
  console.error('Seeding+measuring Business tiers...');
  const business = await seedBusinessTier();

  const gPulse = classifyGrowth(
    'group_pulse',
    (group.small as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
    (group.medium as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
    (group.large as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
  );
  const gExpense = classifyGrowth(
    'group_expense_submit',
    (group.small as { measures: { expenseSubmit: { p95: number } } }).measures.expenseSubmit.p95,
    (group.medium as { measures: { expenseSubmit: { p95: number } } }).measures.expenseSubmit.p95,
    (group.large as { measures: { expenseSubmit: { p95: number } } }).measures.expenseSubmit.p95,
  );
  const gActivity = classifyGrowth(
    'group_activity_p1',
    (group.small as { measures: { activity1: { p95: number } } }).measures.activity1.p95,
    (group.medium as { measures: { activity1: { p95: number } } }).measures.activity1.p95,
    (group.large as { measures: { activity1: { p95: number } } }).measures.activity1.p95,
  );

  const bPulse = classifyGrowth(
    'business_pulse',
    (business.small as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
    (business.medium as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
    (business.large as { measures: { pulse: { p95: number } } }).measures.pulse.p95,
  );
  const bMe = classifyGrowth(
    'business_me_bootstrap',
    (business.small as { measures: { meBootstrap: { p95: number } } }).measures.meBootstrap.p95,
    (business.medium as { measures: { meBootstrap: { p95: number } } }).measures.meBootstrap.p95,
    (business.large as { measures: { meBootstrap: { p95: number } } }).measures.meBootstrap.p95,
  );

  const report = {
    capturedAt: new Date().toISOString(),
    stage: 'S9-G+H',
    note: 'Diagnose-only. Bulk SQL used for Med/Large historical volume; live API samples for writes.',
    group,
    business,
    growth: { group: { gPulse, gExpense, gActivity }, business: { bPulse, bMe } },
  };
  console.log(JSON.stringify(report, null, 2));
  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
