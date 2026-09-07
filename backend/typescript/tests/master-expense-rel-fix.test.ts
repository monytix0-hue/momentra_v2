/**
 * Master Expense fan-out feed + Relationships activity delete (R1–R10).
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
    [userId, email, 'ME Rel Fix']
  );
}

async function createSetupMoment(
  uid: string,
  systemCode: 'LIFE_OPERATIONS' | 'RELATIONSHIPS' | 'LIFESTYLE' | 'FUTURE_BUILDING'
): Promise<string> {
  const res = await request(app)
    .post(`/v1/personal/setups/${systemCode}/activate`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `mrf-setup-${systemCode}-${randomUUID()}`)
    .send({});
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function createExpense(uid: string, momentId: string, body: Record<string, unknown>) {
  const res = await request(app)
    .post(`/v1/moments/${momentId}/expenses`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `mrf-exp-${randomUUID()}`)
    .send(body);
  return res;
}

async function contributions(expenseId: string) {
  const rows = await getPool().query<{ dimension_code: string; status: string }>(
    `SELECT dimension_code, status FROM finance.expense_dimension_contribution WHERE expense_id = $1`,
    [expenseId]
  );
  return Object.fromEntries(rows.rows.map((r) => [r.dimension_code, r.status]));
}

describe('Master Expense fan-out + Rel activity delete', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('R6 FRIEND dining — LO+Rel+Lifestyle; feed visible; amount once', async () => {
    const uid = `mrf-r6-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    const rel = await createSetupMoment(uid, 'RELATIONSHIPS');
    const ls = await createSetupMoment(uid, 'LIFESTYLE');

    const res = await createExpense(uid, lo, {
      amount: '2000.00',
      currencyCode: 'INR',
      merchantName: 'Dinner with friends',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'FRIEND',
    });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const expenseId = res.body.data.expenseId as string;
    const c = await contributions(expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'ACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');

    const sum = await getPool().query<{ n: string }>(
      `SELECT amount::text AS n FROM finance.expense WHERE expense_id = $1`,
      [expenseId]
    );
    assert.equal(Number(sum.rows[0].n), 2000);

    const relFeed = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: rel, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(relFeed.status, 200);
    const relItems = relFeed.body.data.items as Array<{
      activityCode: string;
      activityPayload?: { source?: string; activityId?: string; expenseId?: string };
    }>;
    assert.ok(
      relItems.some(
        (i) =>
          i.activityPayload?.source === 'MASTER_EXPENSE' &&
          i.activityPayload?.expenseId === expenseId &&
          i.activityCode.includes('RELATIONSHIP')
      ),
      JSON.stringify(relItems)
    );

    const lsFeed = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: ls, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(lsFeed.status, 200);
    const lsItems = lsFeed.body.data.items as Array<{
      activityPayload?: { source?: string; expenseId?: string };
    }>;
    assert.ok(
      lsItems.some((i) => i.activityPayload?.source === 'MASTER_EXPENSE' && i.activityPayload?.expenseId === expenseId),
      JSON.stringify(lsItems)
    );
  });

  it('R7 SELF dining — Rel NO; Lifestyle YES', async () => {
    const uid = `mrf-r7-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const res = await createExpense(uid, lo, {
      amount: '2500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SELF',
    });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const c = await contributions(res.body.data.expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(c.FUTURE_BUILDING, 'INACTIVE');
  });

  it('R8 SELF utility — Rel NO; Lifestyle NO', async () => {
    const uid = `mrf-r8-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const res = await createExpense(uid, lo, {
      amount: '2500.00',
      currencyCode: 'INR',
      merchantName: 'Electricity Bill',
      categoryCode: 'BILLS',
      subcategoryCode: 'BILLS',
      sharedExperienceCode: 'SELF',
    });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const c = await contributions(res.body.data.expenseId);
    assert.equal(c.LIFE_OPERATIONS, 'ACTIVE');
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
  });

  it('R9 FRIEND→SELF removes Rel contribution; Lifestyle remains', async () => {
    const uid = `mrf-r9-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    const rel = await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const created = await createExpense(uid, lo, {
      amount: '1800.00',
      currencyCode: 'INR',
      merchantName: 'Dinner friends',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'FRIEND',
    });
    assert.equal(created.status, 201);
    const expenseId = created.body.data.expenseId as string;
    const link = await getPool().query<{ linked_resource_id: string }>(
      `SELECT linked_resource_id FROM finance.expense_dimension_contribution
       WHERE expense_id = $1 AND dimension_code = 'RELATIONSHIPS'`,
      [expenseId]
    );
    const activityId = link.rows[0]!.linked_resource_id;

    const patch = await request(app)
      .patch(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ sharedExperienceCode: 'SELF' });
    assert.equal(patch.status, 200, JSON.stringify(patch.body));
    const c = await contributions(expenseId);
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'ACTIVE');
    assert.equal(Number(patch.body.data.amount ?? 1800), 1800);

    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM personal.relationship_activity WHERE relationship_activity_id = $1`,
      [activityId]
    );
    assert.equal(st.rows[0].status, 'VOIDED');

    const feed = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: rel, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    const items = feed.body.data.items as Array<{ activityPayload?: { activityId?: string; status?: string } }>;
    assert.ok(!items.some((i) => i.activityPayload?.activityId === activityId && i.activityPayload?.status !== 'VOIDED'));
  });

  it('R10 void expense clears derived Rel/Lifestyle + feed', async () => {
    const uid = `mrf-r10-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    const rel = await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const created = await createExpense(uid, lo, {
      amount: '900.00',
      currencyCode: 'INR',
      merchantName: 'Dinner family',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'FAMILY',
    });
    const expenseId = created.body.data.expenseId as string;
    const link = await getPool().query<{ linked_resource_id: string }>(
      `SELECT linked_resource_id FROM finance.expense_dimension_contribution
       WHERE expense_id = $1 AND dimension_code = 'RELATIONSHIPS'`,
      [expenseId]
    );
    const activityId = link.rows[0]!.linked_resource_id;

    const voidRes = await request(app)
      .delete(`/v1/moments/${lo}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(voidRes.status, 200, JSON.stringify(voidRes.body));
    const c = await contributions(expenseId);
    assert.equal(c.RELATIONSHIPS, 'INACTIVE');
    assert.equal(c.LIFESTYLE, 'INACTIVE');
    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM personal.relationship_activity WHERE relationship_activity_id = $1`,
      [activityId]
    );
    assert.equal(st.rows[0].status, 'VOIDED');

    const feed = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: rel, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    const items = feed.body.data.items as Array<{ activityPayload?: { activityId?: string } }>;
    assert.ok(!items.some((i) => i.activityPayload?.activityId === activityId));
  });

  it('missing Rel setup rejects non-SELF expense (no silent drop)', async () => {
    const uid = `mrf-miss-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    await createSetupMoment(uid, 'LIFESTYLE');
    const res = await createExpense(uid, lo, {
      amount: '500.00',
      currencyCode: 'INR',
      merchantName: 'Dinner',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'FRIEND',
    });
    assert.equal(res.status, 400, JSON.stringify(res.body));
  });

  it('R1–R5 manual Rel activity delete + idempotent + deny ME-derived + ownership', async () => {
    const uid = `mrf-del-${randomUUID().slice(0, 8)}`;
    const otherUid = `mrf-oth-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@mrf.local`);
    await ensureUser(userIdFor(otherUid), `${otherUid}@mrf.local`);
    const lo = await createSetupMoment(uid, 'LIFE_OPERATIONS');
    const rel = await createSetupMoment(uid, 'RELATIONSHIPS');
    await createSetupMoment(uid, 'LIFESTYLE');

    const a1 = await request(app)
      .post(`/v1/moments/${rel}/relationship-activities`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `mrf-ra1-${randomUUID()}`)
      .send({ activityKind: 'SUPPORT', displayName: 'Call with family' });
    assert.equal(a1.status, 201, JSON.stringify(a1.body));
    const activityId = a1.body.data.activityId as string;

    const a2 = await request(app)
      .post(`/v1/moments/${rel}/relationship-activities`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `mrf-ra2-${randomUUID()}`)
      .send({ activityKind: 'SHARED_EXPERIENCE', displayName: 'Manual shared' });
    assert.equal(a2.status, 201);

    const beforeBond = await request(app)
      .get(`/v1/personal/moments/${rel}/relationships-bond-snapshot`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(beforeBond.status, 200, JSON.stringify(beforeBond.body));
    const beforeCount = Number(beforeBond.body.data.counts?.support ?? 0);

    const pulseBefore = await getPool().query<{ wellbeing_score: string | null }>(
      `SELECT wellbeing_score::text FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    );

    const del = await request(app)
      .delete(`/v1/moments/${rel}/relationship-activities/${activityId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(del.status, 200, JSON.stringify(del.body));
    assert.equal(del.body.data.status, 'VOIDED');

    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM personal.relationship_activity WHERE relationship_activity_id = $1`,
      [activityId]
    );
    assert.equal(st.rows[0].status, 'VOIDED');

    const feed = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: rel, limit: 30 })
      .set('X-Dev-Firebase-Uid', uid);
    const items = feed.body.data.items as Array<{ activityPayload?: { activityId?: string } }>;
    assert.ok(!items.some((i) => i.activityPayload?.activityId === activityId));

    const afterBond = await request(app)
      .get(`/v1/personal/moments/${rel}/relationships-bond-snapshot`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.ok(Number(afterBond.body.data.counts?.support ?? 0) < beforeCount || beforeCount === 0);

    const pulseAfter = await getPool().query<{ wellbeing_score: string | null }>(
      `SELECT wellbeing_score::text FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    );
    assert.ok(pulseAfter.rows[0], 'pulse row');
    void pulseBefore;

    // R5 double delete idempotent
    const del2 = await request(app)
      .delete(`/v1/moments/${rel}/relationship-activities/${activityId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(del2.status, 200);
    assert.equal(del2.body.data.status, 'VOIDED');

    // R4 ownership
    const deny = await request(app)
      .delete(`/v1/moments/${rel}/relationship-activities/${a2.body.data.activityId}`)
      .set('X-Dev-Firebase-Uid', otherUid);
    assert.ok([403, 404].includes(deny.status), JSON.stringify(deny.body));

    // ME-derived cannot be deleted via Rel void
    const exp = await createExpense(uid, lo, {
      amount: '700.00',
      currencyCode: 'INR',
      merchantName: 'Dinner spouse',
      categoryCode: 'FOOD',
      subcategoryCode: 'DINING_OUT',
      sharedExperienceCode: 'SPOUSE',
    });
    assert.equal(exp.status, 201, JSON.stringify(exp.body));
    const meLink = await getPool().query<{ linked_resource_id: string }>(
      `SELECT linked_resource_id FROM finance.expense_dimension_contribution
       WHERE expense_id = $1 AND dimension_code = 'RELATIONSHIPS' AND status = 'ACTIVE'`,
      [exp.body.data.expenseId]
    );
    const meAct = meLink.rows[0]!.linked_resource_id;
    const block = await request(app)
      .delete(`/v1/moments/${rel}/relationship-activities/${meAct}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(block.status, 409, JSON.stringify(block.body));
  });
});
