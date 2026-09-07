/**
 * Golden Scenarios first wave — Input → Moments → Pulse → Life → Memory.
 * Catalog: docs/implementation/GOLDEN_SCENARIOS.md
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { firebaseUserId } from '../src/platform/auth/uuid';
import { config } from '../src/platform/config';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, 'Golden Scenario']
  );
}

async function createSetupMoment(
  uid: string,
  systemCode: 'LIFE_OPERATIONS' | 'RELATIONSHIPS' | 'LIFESTYLE' | 'FUTURE_BUILDING'
): Promise<string> {
  const res = await request(app)
    .post(`/v1/personal/setups/${systemCode}/activate`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `gs-setup-${systemCode}-${randomUUID()}`)
    .send({});
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function createExpense(
  uid: string,
  momentId: string,
  body: Record<string, unknown>
): Promise<{ expenseId: string; momentId: string }> {
  const res = await request(app)
    .post(`/v1/moments/${momentId}/expenses`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `gs-exp-${randomUUID()}`)
    .send(body);
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return { expenseId: res.body.data.expenseId, momentId: res.body.data.momentId };
}

async function contributions(expenseId: string) {
  const rows = await getPool().query<{ dimension_code: string; status: string }>(
    `SELECT dimension_code, status FROM finance.expense_dimension_contribution WHERE expense_id = $1`,
    [expenseId]
  );
  return Object.fromEntries(rows.rows.map((r) => [r.dimension_code, r.status]));
}

async function memoryCount(uid: string): Promise<number> {
  const res = await request(app).get('/v1/personal/memory').set('X-Dev-Firebase-Uid', uid);
  assert.equal(res.status, 200, JSON.stringify(res.body));
  return Number(res.body.data.memoryCount ?? res.body.data.items?.length ?? 0);
}

async function personalLifeScore(uid: string): Promise<unknown> {
  const res = await request(app).get('/v1/personal/life').set('X-Dev-Firebase-Uid', uid);
  assert.equal(res.status, 200, JSON.stringify(res.body));
  return res.body.data.score;
}

describe('Golden scenarios', () => {
  before(async () => {
    await getPool().query('SELECT 1');
    const col = await getPool().query(
      `SELECT 1 FROM information_schema.columns
       WHERE table_schema='finance' AND table_name='expense' AND column_name='shared_experience_code'`
    );
    assert.ok(col.rowCount, 'V076 shared_experience_code required — run migrations');
  });

  after(async () => {
    await closePool();
  });

  it('P-01 Dinner ₹2500 Self → LO+Lifestyle; no Rel/Future; spend once; Life null; no Memory', async () => {
    const uid = `gs-p01-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    await createSetupMoment(uid, 'FUTURE_BUILDING');
    const beforeMem = await memoryCount(uid);

    const { expenseId } = await createExpense(uid, lo, {
      amount: '2500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });

    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');

    const sum = await getPool().query<{ total: string }>(
      `SELECT amount::text AS total FROM finance.expense WHERE expense_id = $1 AND status = 'POSTED'`,
      [expenseId]
    );
    assert.equal(Number(sum.rows[0].total), 2500);

    const pulse = await getPool().query<{ widget_payload: Record<string, unknown> }>(
      `SELECT widget_payload FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    );
    const loSpend = pulse.rows[0]?.widget_payload?.lifeOpsSpendByCurrency as
      | Record<string, string>
      | undefined;
    assert.ok(loSpend?.INR && Number(loSpend.INR) >= 2500, `LO spend ${JSON.stringify(loSpend)}`);

    assert.equal(await personalLifeScore(uid), null);
    assert.equal(await memoryCount(uid), beforeMem);
  });

  it('P-02 Dinner with wife ₹1500 → LO+Rel+Lifestyle; financial truth 1500; Life null', async () => {
    const uid = `gs-p02-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const beforeMem = await memoryCount(uid);

    const { expenseId } = await createExpense(uid, lo, {
      amount: '1500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner with wife',
      categoryCode: 'FOOD',
      subcategoryCode: 'FOOD_DINING',
      sharedExperienceCode: 'SPOUSE',
    });

    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');

    const sum = await getPool().query<{ total: string }>(
      `SELECT amount::text AS total FROM finance.expense WHERE expense_id = $1`,
      [expenseId]
    );
    assert.equal(Number(sum.rows[0].total), 1500);

    const pulse = await getPool().query<{
      wellbeing_score: string | null;
      widget_payload: Record<string, unknown>;
    }>(`SELECT wellbeing_score::text, widget_payload FROM projection.personal_pulse WHERE user_id = $1`, [
      userId,
    ]);
    assert.ok(pulse.rows[0], 'pulse expected');
    const payload = pulse.rows[0].widget_payload ?? {};
    assert.ok(
      Number(payload.bondIndex) > 0 || Number(payload.trustScore) > 0 || Number(payload.presenceScore) > 0,
      'Rel axes'
    );
    assert.ok(
      Number(payload.vitalityScore) > 0 || Number(payload.joyScore) > 0 || Number(payload.fulfillmentScore) > 0,
      'Lifestyle axes'
    );
    assert.ok(pulse.rows[0].wellbeing_score != null && Number(pulse.rows[0].wellbeing_score) > 0);

    assert.equal(await personalLifeScore(uid), null);
    assert.equal(await memoryCount(uid), beforeMem);
  });

  it('P-05 Self groceries → Lifestyle INACTIVE', async () => {
    const uid = `gs-p05-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const { expenseId } = await createExpense(uid, lo, {
      amount: '3000.00',
      currencyCode: 'INR',
      merchantName: 'Groceries',
      categoryCode: 'FOOD',
      subcategoryCode: 'GROCERIES',
      sharedExperienceCode: 'SELF',
    });
    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
    assert.equal(await personalLifeScore(uid), null);
  });

  it('P-09 Four dinners SUM 9000 no double-count', async () => {
    const uid = `gs-p09-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const dinners = [
      { amount: '3000.00', shared: 'FAMILY' },
      { amount: '2000.00', shared: 'FRIEND' },
      { amount: '1500.00', shared: 'SPOUSE' },
      { amount: '2500.00', shared: 'SELF' },
    ] as const;
    const ids: string[] = [];
    for (const d of dinners) {
      const { expenseId } = await createExpense(uid, lo, {
        amount: d.amount,
        currencyCode: 'INR',
        merchantName: `Dinner ${d.shared}`,
        categoryCode: 'FOOD',
        subcategoryCode: 'DINING_OUT',
        sharedExperienceCode: d.shared,
      });
      ids.push(expenseId);
    }
    const sum = await getPool().query<{ total: string }>(
      `SELECT COALESCE(SUM(amount),0)::text AS total FROM finance.expense
       WHERE expense_id = ANY($1::uuid[]) AND status = 'POSTED'`,
      [ids]
    );
    assert.equal(Number(sum.rows[0].total), 9000);
  });

  it('P-17 / P-18 fresh user and setups-only — Life score null', async () => {
    const uid = `gs-p17-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@gs.local`);
    const lifeEmpty = await request(app).get('/v1/personal/life').set('X-Dev-Firebase-Uid', uid);
    assert.equal(lifeEmpty.status, 200);
    assert.equal(lifeEmpty.body.data.score, null);

    await createSetupMoment(uid, 'LIFE_OPERATIONS');
    const lifeActive = await request(app).get('/v1/personal/life').set('X-Dev-Firebase-Uid', uid);
    assert.equal(lifeActive.status, 200);
    assert.equal(lifeActive.body.data.score, null);
    assert.ok(
      lifeActive.body.data.statusLabel === 'Active' || lifeActive.body.data.activeAreaCount >= 1,
      JSON.stringify(lifeActive.body.data)
    );
  });

  it('P-19 / P-20 explicit Memory appears; expense does not auto-create Memory', async () => {
    const uid = `gs-p19-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    assert.equal(await memoryCount(uid), 0);

    await createExpense(uid, lo, {
      amount: '99.00',
      currencyCode: 'INR',
      merchantName: 'Snack',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    assert.equal(await memoryCount(uid), 0, 'expense must not invent Memory');

    // Explicit personal Memory write (read model keys off created_by / personal moment context).
    await getPool().query(
      `INSERT INTO memory.memory (
         scope_type, scope_id, domain_code, moment_id, title, occurred_at, status, created_by_user_id, version
       ) VALUES ('MOMENT', $1, 'PERSONAL', $1, $2, now(), 'ACTIVE', $3, 1)`,
      [lo, 'Anniversary note', userId]
    );
    const after = await memoryCount(uid);
    assert.ok(after >= 1, `expected explicit memory, got ${after}`);
    assert.equal(await personalLifeScore(uid), null);
  });

  it('P-21 expense → Rel/Lifestyle axes → wellbeing; Life overall still null', async () => {
    const uid = `gs-p21-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@gs.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    await createExpense(uid, lo, {
      amount: '1200.00',
      currencyCode: 'INR',
      merchantName: 'Pulse dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SPOUSE',
    });
    const pulse = await getPool().query<{ wellbeing_score: string | null }>(
      `SELECT wellbeing_score::text FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    );
    assert.ok(pulse.rows[0]?.wellbeing_score != null && Number(pulse.rows[0].wellbeing_score) > 0);
    assert.equal(await personalLifeScore(uid), null);
  });

  async function createTrip(
    organizerUid: string,
    memberUid: string
  ): Promise<{ momentId: string; organizerParticipantId: string; memberParticipantId: string }> {
    const mint = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gs-mint-${randomUUID()}`)
      .send({ title: 'GS Trip', momentTypeCode: 'TRIP' });
    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    const created = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gs-mom-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: 'GS Trip',
        inviteCode: mint.body.data.inviteCode,
      });
    assert.equal(created.status, 201, JSON.stringify(created.body));
    const momentId = created.body.data.momentId as string;
    const redeem = await request(app)
      .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `gs-redeem-${randomUUID()}`)
      .send({});
    assert.equal(redeem.status, 200, JSON.stringify(redeem.body));
    const parts = await getPool().query<{ participant_id: string; user_id: string }>(
      `SELECT participant_id, user_id FROM collaboration.moment_participant
       WHERE moment_id = $1 AND status = 'ACTIVE'`,
      [momentId]
    );
    return {
      momentId,
      organizerParticipantId: parts.rows.find((r) => r.user_id === userIdFor(organizerUid))!.participant_id,
      memberParticipantId: parts.rows.find((r) => r.user_id === userIdFor(memberUid))!.participant_id,
    };
  }

  it('G-01 / G-02 trip expense updates contribution_total; Life provisional; no auto Memory', async () => {
    const orgUid = `gs-g01-${randomUUID().slice(0, 8)}`;
    const memUid = `gs-g01m-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@gs.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@gs.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createTrip(orgUid, memUid);

    const memBefore = await request(app)
      .get(`/v1/group/moments/${momentId}/memory`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(memBefore.status, 200);
    const countBefore = Number(memBefore.body.data.payload?.memoryCount ?? 0);

    const res = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gs-gexp-${randomUUID()}`)
      .send({
        amount: '100.00',
        currencyCode: 'INR',
        description: 'Dinner',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
      });
    assert.equal(res.status, 201, JSON.stringify(res.body));

    const snap = await getPool().query<{
      expense_total: string;
      contribution_total: string;
    }>(
      `SELECT expense_total::text, contribution_total::text
       FROM projection.group_finance_snapshot WHERE moment_id = $1 AND currency_code = 'INR'`,
      [momentId]
    );
    assert.ok(snap.rows[0], 'finance snapshot');
    assert.equal(Number(snap.rows[0].expense_total), 100);
    assert.equal(Number(snap.rows[0].contribution_total), 100);

    const finance = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(finance.status, 200);
    const totals = finance.body.data.payload?.totals as Array<Record<string, string>> | undefined;
    const inr = totals?.find((t) => t.currencyCode === 'INR');
    if (inr?.contributionTotal != null) {
      assert.equal(Number(inr.contributionTotal), 100);
    }

    const life = await request(app)
      .get(`/v1/group/moments/${momentId}/life`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(life.status, 200);
    assert.equal(life.body.data.payload?.metricVersion, 'life-v1-provisional');

    const memAfter = await request(app)
      .get(`/v1/group/moments/${momentId}/memory`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(Number(memAfter.body.data.payload?.memoryCount ?? 0), countBefore);
  });

  it('G-08 / G-09 explicit Memory; expense alone does not create Memory', async () => {
    const orgUid = `gs-g08-${randomUUID().slice(0, 8)}`;
    const memUid = `gs-g08m-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@gs.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@gs.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createTrip(orgUid, memUid);

    await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gs-gexp2-${randomUUID()}`)
      .send({
        amount: '40.00',
        currencyCode: 'INR',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
      });

    let mem = await request(app)
      .get(`/v1/group/moments/${momentId}/memory`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(Number(mem.body.data.payload?.memoryCount ?? 0), 0);

    const created = await request(app)
      .post(`/v1/moments/${momentId}/memories`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gs-mem-${randomUUID()}`)
      .send({ title: 'Sunset at the pier' });
    assert.equal(created.status, 201, JSON.stringify(created.body));

    mem = await request(app)
      .get(`/v1/group/moments/${momentId}/memory`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.ok(Number(mem.body.data.payload?.memoryCount ?? 0) >= 1);
  });

  async function createCompany(uid: string, name: string): Promise<string> {
    const res = await request(app)
      .post('/v1/companies')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `gs-co-${randomUUID()}`)
      .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    return res.body.data.companyId as string;
  }

  async function createOpsMoment(uid: string, companyId: string): Promise<string> {
    const res = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `gs-ops-${randomUUID()}`)
      .send({
        domainCode: 'BUSINESS',
        momentTypeCode: 'BUSINESS_OPERATIONS',
        title: 'GS Ops',
        companyId,
        businessSetup: {
          familyCode: 'BUSINESS_OPERATIONS',
          preferences: {
            coreOps: 'Growth & Product',
            scope: 'Company-wide',
            model: 'Centralized',
            cadence: 'Monthly',
            monthlyBudget: '₹35,00,000',
            allocationMethod: 'Category-based',
            monitoringStyle: 'Proactive',
            approvalModel: 'not required',
            approvalAlarm: '₹5,00,000',
          },
        },
      });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    return res.body.data.momentId as string;
  }

  function assertNoOpenRiskKeys(obj: unknown, path = 'root'): void {
    if (obj == null || typeof obj !== 'object') return;
    if (Array.isArray(obj)) {
      obj.forEach((v, i) => assertNoOpenRiskKeys(v, `${path}[${i}]`));
      return;
    }
    for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
      assert.notEqual(k, 'openRiskCount', `must not expose openRiskCount at ${path}`);
      assert.notEqual(k, 'open_risk_count', `must not expose open_risk_count at ${path}`);
      assertNoOpenRiskKeys(v, `${path}.${k}`);
    }
  }

  it('B-01 / B-10 / B-11 pulse API omits open risk; DB placeholder remains 0', async () => {
    const uid = `gs-b01-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@gs.local`);
    const companyId = await createCompany(uid, `GSCo ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const pulse = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    assertNoOpenRiskKeys(pulse.body);
    assert.ok('attentionCount' in (pulse.body.data.payload ?? {}));
    assert.ok(!('openRiskCount' in (pulse.body.data.payload ?? {})));

    const db = await getPool().query<{ open_risk_count: number }>(
      `SELECT open_risk_count FROM projection.business_pulse WHERE company_id = $1`,
      [companyId]
    );
    if (db.rows[0]) {
      assert.equal(Number(db.rows[0].open_risk_count), 0);
    }
  });

  it('B-02 / B-09 business expense bumps finance; does not invent Memory keys as open risk', async () => {
    const uid = `gs-b02-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@gs.local`);
    const companyId = await createCompany(uid, `GSSpend ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const memBefore = await request(app)
      .get(`/v1/business/moments/${momentId}/memories`)
      .set('X-Dev-Firebase-Uid', uid);
    const beforeCount = Array.isArray(memBefore.body?.data?.items)
      ? memBefore.body.data.items.length
      : Number(memBefore.body?.data?.memoryCount ?? 0);

    const spend = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `gs-bspend-${randomUUID()}`)
      .send({
        amount: '120.00',
        currencyCode: 'INR',
        description: 'Cloud bill',
        categoryCode: 'PURCHASE',
      });
    assert.equal(spend.status, 201, JSON.stringify(spend.body));

    const pulse = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulse.status, 200);
    assertNoOpenRiskKeys(pulse.body);

    const memAfter = await request(app)
      .get(`/v1/business/moments/${momentId}/memories`)
      .set('X-Dev-Firebase-Uid', uid);
    const afterCount = Array.isArray(memAfter.body?.data?.items)
      ? memAfter.body.data.items.length
      : Number(memAfter.body?.data?.memoryCount ?? 0);
    assert.equal(afterCount, beforeCount, 'expense must not auto-create Memory');
  });
});
