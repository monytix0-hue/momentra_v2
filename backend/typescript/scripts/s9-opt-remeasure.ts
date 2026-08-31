/**
 * S9-G-OPT + S9-H-OPT remeasure — same harness class as s9-gh-scale-measure.
 * Skips heavy historical volume; focuses on P1 latency targets.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import { writeFileSync } from 'fs';
import { join } from 'path';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool, prewarmPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { clearKnownUserProfiles } from '../src/platform/auth';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

type Tier = 'small' | 'medium' | 'large';
const GROUP_MEMBERS: Record<Tier, number> = { small: 5, medium: 25, large: 100 };

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

async function sampleGet(path: string, headers: Record<string, string>, n: number) {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBytes = 0;
  let positionRows: number | undefined;
  let positionsTruncated: boolean | undefined;
  let positionsSemantics: string | undefined;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app).get(path).set(headers);
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBytes = JSON.stringify(res.body).length;
    const payload = res.body?.data?.payload ?? res.body?.data;
    const finance = payload?.finance ?? payload;
    const positions = finance?.positions;
    if (Array.isArray(positions)) positionRows = positions.length;
    if (typeof finance?.positionsTruncated === 'boolean') {
      positionsTruncated = finance.positionsTruncated;
    }
    if (typeof finance?.positionsSemantics === 'string') {
      positionsSemantics = finance.positionsSemantics;
    }
  }
  return summarize(path, samples, {
    status: lastStatus,
    approxBodyBytes: lastBytes,
    positionRows,
    positionsTruncated,
    positionsSemantics,
  });
}

async function timedPost(
  path: string,
  headers: Record<string, string>,
  bodyFactory: () => unknown,
  n: number
) {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBody: unknown;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app)
      .post(path)
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send(bodyFactory());
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBody = res.body;
  }
  return summarize(path, samples, { status: lastStatus, lastBody });
}

async function ensureOwner(uid: string) {
  const headers = authFor(uid);
  const me = await request(app).get('/v1/me').set(headers);
  if (me.status !== 200) throw new Error(`me failed ${me.status}`);
  return { headers, userId: me.body.data.userId as string };
}

async function seedGroupMembers(momentId: string, countExtra: number) {
  const pool = getPool();
  for (let i = 0; i < countExtra; i++) {
    const party = await pool.query<{ external_party_id: string }>(
      `INSERT INTO core.external_party (party_type, display_name, status)
       VALUES ('PERSON', $1, 'ACTIVE') RETURNING external_party_id`,
      [`S9OPT Member ${i + 1}`]
    );
    await pool.query(
      `INSERT INTO collaboration.moment_participant (
         moment_id, external_party_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)`,
      [momentId, party.rows[0]!.external_party_id]
    );
  }
}

async function listParticipantIds(momentId: string): Promise<string[]> {
  const r = await getPool().query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE' ORDER BY joined_at NULLS LAST, participant_id`,
    [momentId]
  );
  return r.rows.map((x) => x.participant_id);
}

async function measureGroup() {
  const results: Record<string, unknown> = {};
  for (const tier of ['small', 'medium', 'large'] as Tier[]) {
    const members = GROUP_MEMBERS[tier];
    console.error(`Group ${tier}: ${members} members...`);
    const uid = `s9opt-g-${tier}-${randomUUID().slice(0, 6)}`;
    const { headers } = await ensureOwner(uid);

    const created = await request(app)
      .post('/v1/moments')
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: `S9OPT-G ${tier}` });
    if (created.status >= 400) {
      throw new Error(`create moment ${created.status} ${JSON.stringify(created.body)}`);
    }
    const momentId = created.body.data.momentId as string;
    await seedGroupMembers(momentId, members - 1);
    const participantIds = await listParticipantIds(momentId);
    const paidBy = participantIds[0]!;

    // Warm projections with one expense
    await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({
        amount: '25.00',
        currencyCode: 'USD',
        description: 'warmup',
        paidByParticipantId: paidBy,
        splitStrategy: 'EQUAL',
        splitInputs: participantIds.map((participantId) => ({ participantId })),
      });

    const expenseSubmit = await timedPost(
      `/v1/moments/${momentId}/group-expenses`,
      headers,
      () => ({
        amount: '10.00',
        currencyCode: 'USD',
        description: 'S9OPT live',
        paidByParticipantId: paidBy,
        splitStrategy: 'EQUAL',
        splitInputs: participantIds.map((participantId) => ({ participantId })),
      }),
      tier === 'large' ? 5 : 5
    );

    // Warm then measure pulse
    await request(app).get(`/v1/group/moments/${momentId}/pulse`).set(headers);
    const pulse = await sampleGet(`/v1/group/moments/${momentId}/pulse`, headers, 10);
    const finance = await sampleGet(`/v1/group/moments/${momentId}/finance`, headers, 5);

    results[tier] = {
      members,
      momentId,
      expenseSubmit,
      pulse,
      finance,
      targets: {
        expenseP95Under2s: expenseSubmit.p95 < 2000,
        pulseP95Under400: pulse.p95 <= 400,
      },
    };
  }
  return results;
}

async function measureBusiness() {
  console.error('Business OPT measures...');
  const uid = `s9opt-h-${randomUUID().slice(0, 6)}`;
  const { headers } = await ensureOwner(uid);

  // Populated inventory: personal + group + business
  await request(app)
    .post('/v1/moments')
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send({ domainCode: 'PERSONAL', momentTypeCode: 'PERSONAL_LIFE', title: 'S9OPT personal' });

  await request(app)
    .post('/v1/moments')
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: 'S9OPT group' });

  const co = await request(app)
    .post('/v1/companies')
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send({ displayName: 'S9OPT Co', legalName: 'S9OPT Legal' });
  if (co.status >= 400) throw new Error(`company ${co.status} ${JSON.stringify(co.body)}`);
  const companyId = co.body.data.companyId as string;

  const moments: string[] = [];
  for (let i = 0; i < 5; i++) {
    const m = await request(app)
      .post('/v1/moments')
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({
        domainCode: 'BUSINESS',
        companyId,
        momentTypeCode: 'TEAM_OPERATIONS',
        title: `S9OPT biz ${i}`,
      });
    if (m.status >= 400) throw new Error(`biz moment ${m.status} ${JSON.stringify(m.body)}`);
    moments.push(m.body.data.momentId as string);
  }
  const primaryMoment = moments[0]!;

  // Seed some inventory rows
  for (let i = 0; i < 10; i++) {
    await request(app)
      .post(`/v1/moments/${primaryMoment}/business-expenses`)
      .set({ ...headers, 'Idempotency-Key': randomUUID() })
      .send({ amount: '12.50', currencyCode: 'USD', description: `seed ${i}` });
  }

  await request(app).get('/v1/me').set(headers);
  const meBootstrap = await sampleGet('/v1/me', headers, 10);

  const expenseSubmit = await timedPost(
    `/v1/moments/${primaryMoment}/business-expenses`,
    headers,
    () => ({ amount: '15.00', currencyCode: 'USD', description: 'S9OPT live' }),
    8
  );
  const revenueSubmit = await timedPost(
    `/v1/moments/${primaryMoment}/revenues`,
    headers,
    () => ({ amount: '20.00', currencyCode: 'USD', description: 'S9OPT rev' }),
    5
  );
  const invoiceSubmit = await timedPost(
    `/v1/moments/${primaryMoment}/invoices`,
    headers,
    () => ({
      invoiceNumber: `INV-OPT-${randomUUID().slice(0, 8)}`,
      invoiceDate: new Date().toISOString().slice(0, 10),
      currencyCode: 'USD',
      lines: [{ description: 'Live', quantity: '1', unitPrice: '50.00' }],
    }),
    5
  );

  // Invoice conflict semantics
  const dupNumber = `INV-DUP-${randomUUID().slice(0, 6)}`;
  const first = await request(app)
    .post(`/v1/moments/${primaryMoment}/invoices`)
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send({
      invoiceNumber: dupNumber,
      invoiceDate: new Date().toISOString().slice(0, 10),
      currencyCode: 'USD',
      lines: [{ description: 'A', quantity: '1', unitPrice: '10.00' }],
    });
  const second = await request(app)
    .post(`/v1/moments/${primaryMoment}/invoices`)
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send({
      invoiceNumber: dupNumber,
      invoiceDate: new Date().toISOString().slice(0, 10),
      currencyCode: 'USD',
      lines: [{ description: 'B', quantity: '1', unitPrice: '10.00' }],
    });

  return {
    companyId,
    primaryMoment,
    meBootstrap,
    expenseSubmit,
    revenueSubmit,
    invoiceSubmit,
    invoiceConflict: {
      firstStatus: first.status,
      secondStatus: second.status,
      secondErrorCode: second.body?.code ?? second.body?.error?.errorCode ?? null,
      secondMessage: second.body?.message ?? second.body?.error?.message ?? null,
    },
    targets: {
      meP95Improved: meBootstrap.p95 < 750,
      expenseP95Under750: expenseSubmit.p95 < 750,
      revenueP95Under750: revenueSubmit.p95 < 750,
      invoiceP95Under750: invoiceSubmit.p95 < 750,
      invoiceConflictMapped:
        second.status === 409 &&
        String(second.body?.code ?? second.body?.error?.errorCode ?? '') === 'INVOICE_NUMBER_CONFLICT',
    },
  };
}

async function main() {
  clearKnownUserProfiles();
  await prewarmPool(config.database.poolMin);

  const only = (process.env.S9_OPT_ONLY ?? 'all').toLowerCase();
  const group = only === 'business' ? { skipped: true } : await measureGroup();
  const business = only === 'group' ? { skipped: true } : await measureBusiness();

  const report = {
    capturedAt: new Date().toISOString(),
    stage: 'S9-G-OPT+S9-H-OPT',
    before: {
      groupExpenseP95: { small: 3646, medium: 10361, large: 35431 },
      groupPulseP95: { small: 712, medium: 691, large: 800 },
      businessWritesP95: '~1800-2000',
      mePopulatedP95: '~760-960',
    },
    group,
    business,
  };

  const outPath = join(process.cwd(), 'scripts', 's9-opt-remeasure-latest.json');
  writeFileSync(outPath, JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  console.error(`Wrote ${outPath}`);
  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
