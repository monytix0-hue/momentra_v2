/**
 * Master Expense dimensional routing — T1–T10 + four-dinner financial total.
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
import { isLifestyleEligible } from '../src/modules/finance/lifestyle-eligibility';

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
    [userId, email, 'Expense Dimensions Test']
  );
}

async function createSetupMoment(
  uid: string,
  systemCode: 'LIFE_OPERATIONS' | 'RELATIONSHIPS' | 'LIFESTYLE' | 'FUTURE_BUILDING'
): Promise<string> {
  const res = await request(app)
    .post(`/v1/personal/setups/${systemCode}/activate`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `dim-setup-${systemCode}-${randomUUID()}`)
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
    .set('Idempotency-Key', `dim-exp-${randomUUID()}`)
    .send(body);
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return { expenseId: res.body.data.expenseId, momentId: res.body.data.momentId };
}

async function contributions(expenseId: string) {
  const rows = await getPool().query<{
    dimension_code: string;
    status: string;
  }>(
    `SELECT dimension_code, status FROM finance.expense_dimension_contribution WHERE expense_id = $1`,
    [expenseId]
  );
  return Object.fromEntries(rows.rows.map((r) => [r.dimension_code, r.status]));
}

describe('lifestyle eligibility mapping', () => {
  it('maps dining vs groceries vs bills', () => {
    assert.equal(isLifestyleEligible('FOOD', 'DINING_OUT'), true);
    assert.equal(isLifestyleEligible('FOOD', 'FOOD_DINING'), true);
    assert.equal(isLifestyleEligible('FOOD', 'GROCERIES'), false);
    assert.equal(isLifestyleEligible('FOOD', null), true);
    assert.equal(isLifestyleEligible('CAFE', null), true);
    assert.equal(isLifestyleEligible('ENTERTAINMENT', null), true);
    assert.equal(isLifestyleEligible('BILLS', null), false);
    assert.equal(isLifestyleEligible('HEALTH', null), false);
  });
});

describe('Master Expense dimensional contributions', () => {
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

  it('T1 Self dinner → LO yes, Rel no, Lifestyle yes, FB no', async () => {
    const uid = `dim-t1-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');

    const { expenseId, momentId } = await createExpense(uid, lo, {
      amount: '2500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    assert.equal(momentId, lo);
    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');
  });

  it('T2 Self groceries → Lifestyle no', async () => {
    const uid = `dim-t2-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
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
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');
  });

  it('T3–T5 Dinner with spouse/family/friends → Rel + Lifestyle', async () => {
    const uid = `dim-t345-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');

    for (const [shared, amount, name] of [
      ['SPOUSE', '1500.00', 'Dinner with wife'],
      ['FAMILY', '3000.00', 'Dinner with family'],
      ['FRIEND', '2000.00', 'Dinner with friends'],
    ] as const) {
      const { expenseId } = await createExpense(uid, lo, {
        amount,
        currencyCode: 'INR',
        merchantName: name,
        categoryCode: 'FOOD',
        subcategoryCode: 'FOOD_DINING',
        sharedExperienceCode: shared,
      });
      const c = await contributions(expenseId);
      assert.equal(c.LIFE_OPERATIONS, 'ACTIVE', shared);
      assert.equal(c.RELATIONSHIPS, 'ACTIVE', shared);
      assert.equal(c.LIFESTYLE, 'ACTIVE', shared);
      assert.equal(c.FUTURE_BUILDING, 'INACTIVE', shared);
    }
  });

  it('T6–T7 edit SELF↔FRIEND toggles Relationships only', async () => {
    const uid = `dim-t67-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');

    const { expenseId } = await createExpense(uid, lo, {
      amount: '1000.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    let c = await contributions(expenseId);
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');

    const patchFriend = await request(app)
      .patch(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ sharedExperienceCode: 'FRIEND' });
    assert.equal(patchFriend.status, 200, JSON.stringify(patchFriend.body));
    c = await contributions(expenseId);
    assert.equal(c.RELATIONSHIPS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');

    const patchSelf = await request(app)
      .patch(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ sharedExperienceCode: 'SELF' });
    assert.equal(patchSelf.status, 200, JSON.stringify(patchSelf.body));
    c = await contributions(expenseId);
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
  });

  it('T8 category Dinner→Bills removes Lifestyle', async () => {
    const uid = `dim-t8-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'LIFESTYLE');

    const { expenseId } = await createExpense(uid, lo, {
      amount: '500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    assert.equal((await contributions(expenseId)).LIFESTYLE, 'ACTIVE');

    const patch = await request(app)
      .patch(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ categoryCode: 'BILLS', subcategoryCode: 'BILLS' });
    assert.equal(patch.status, 200, JSON.stringify(patch.body));
    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
  });

  it('T9 idempotent retry does not duplicate contributions', async () => {
    const uid = `dim-t9-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const key = `dim-idem-${randomUUID()}`;
    const body = {
      amount: '111.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'FRIEND',
    };
    const r1 = await request(app)
      .post(`/v1/moments/${lo}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.equal(r1.status, 201, JSON.stringify(r1.body));
    const r2 = await request(app)
      .post(`/v1/moments/${lo}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.ok([200, 201].includes(r2.status), JSON.stringify(r2.body));
    assert.equal(r1.body.data.expenseId, r2.body.data.expenseId);

    const count = await getPool().query(
      `SELECT COUNT(*)::int AS n FROM finance.expense_dimension_contribution WHERE expense_id = $1`,
      [r1.body.data.expenseId]
    );
    assert.equal(count.rows[0].n, 4);
  });

  it('T10 void deactivates contributions', async () => {
    const uid = `dim-t10-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const { expenseId } = await createExpense(uid, lo, {
      amount: '222.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SPOUSE',
    });
    const voidRes = await request(app)
      .delete(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(voidRes.status, 200, JSON.stringify(voidRes.body));
    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'INACTIVE');
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
  });

  it('Four-dinner financial total remains 9000 (no double-count)', async () => {
    const uid = `dim-4d-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@dim.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    await createSetupMoment(uid, 'FUTURE_BUILDING');

    const dinners = [
      { amount: '3000.00', shared: 'FAMILY', name: 'Dinner with family' },
      { amount: '2000.00', shared: 'FRIEND', name: 'Dinner with friends' },
      { amount: '1500.00', shared: 'SPOUSE', name: 'Dinner with wife' },
      { amount: '2500.00', shared: 'SELF', name: 'Dinner alone' },
    ] as const;

    const ids: string[] = [];
    for (const d of dinners) {
      const { expenseId } = await createExpense(uid, lo, {
        amount: d.amount,
        currencyCode: 'INR',
        merchantName: d.name,
        categoryCode: 'FOOD',
        subcategoryCode: 'DINING_OUT',
        sharedExperienceCode: d.shared,
      });
      ids.push(expenseId);
    }

    const sum = await getPool().query<{ total: string }>(
      `SELECT COALESCE(SUM(amount),0)::text AS total
       FROM finance.expense
       WHERE expense_id = ANY($1::uuid[]) AND status = 'POSTED'`,
      [ids]
    );
    assert.equal(Number(sum.rows[0].total), 9000);

    const rowCount = await getPool().query(
      `SELECT COUNT(*)::int AS n FROM finance.expense WHERE expense_id = ANY($1::uuid[])`,
      [ids]
    );
    assert.equal(rowCount.rows[0].n, 4);

    // Canonical home is LO even if posted via Future moment path.
    const future = await getPool().query<{ moment_id: string }>(
      `SELECT moment_id FROM personal.life_system_setup
       WHERE user_id = $1 AND system_code = 'FUTURE_BUILDING' AND status = 'ACTIVE' LIMIT 1`,
      [userId]
    );
    const futureMomentId = future.rows[0]!.moment_id;
    const viaFuture = await createExpense(uid, futureMomentId, {
      amount: '10.00',
      currencyCode: 'INR',
      merchantName: 'Should land on LO',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    assert.equal(viaFuture.momentId, lo);
    const fb = await contributions(viaFuture.expenseId);
    assert.equal(fb.FUTURE_BUILDING, 'INACTIVE');
  });

  it('Expense → dimensions → Rel/Lifestyle axes → overall wellbeing + LO spend signal', async () => {
    const uid = `dim-pulse-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@dim.local`);
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

    const pulse = await getPool().query<{
      wellbeing_score: string | null;
      widget_payload: Record<string, unknown>;
    }>(
      `SELECT wellbeing_score::text, widget_payload
       FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    );
    assert.ok(pulse.rows[0], 'personal_pulse row expected');
    const payload = pulse.rows[0].widget_payload ?? {};
    assert.ok(
      Number(payload.bondIndex) > 0 || Number(payload.trustScore) > 0 || Number(payload.presenceScore) > 0,
      'Relationships axes should move'
    );
    assert.ok(
      Number(payload.vitalityScore) > 0 ||
        Number(payload.joyScore) > 0 ||
        Number(payload.fulfillmentScore) > 0,
      'Lifestyle axes should move'
    );
    const loSpend = payload.lifeOpsSpendByCurrency as Record<string, string> | undefined;
    assert.ok(loSpend?.INR, 'lifeOpsSpendByCurrency.INR expected');
    assert.ok(Number(loSpend!.INR) >= 1200, `LO spend signal ${loSpend!.INR}`);
    assert.ok(
      pulse.rows[0].wellbeing_score != null && Number(pulse.rows[0].wellbeing_score) > 0,
      'overall wellbeing should recompute from family axes'
    );
  });
});
