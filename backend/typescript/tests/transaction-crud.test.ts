/**
 * S9-QH Transaction CRUD — GET/PATCH/DELETE expense, attachments, accounts, income, void filtering.
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
    [userId, email, 'QH Transaction CRUD Test']
  );
}

async function firstPersonalTypeCode(): Promise<string> {
  const types = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
  );
  assert.ok(types.rows[0], 'Need at least one PERSONAL moment type');
  return types.rows[0].code;
}

async function createPersonalMoment(uid: string): Promise<string> {
  const typeCode = await firstPersonalTypeCode();
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `qh-moment-${randomUUID()}`)
    .send({
      domainCode: 'PERSONAL',
      momentTypeCode: typeCode,
      title: `QH CRUD ${uid.slice(0, 8)}`,
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function createExpense(uid: string, momentId: string, body: Record<string, unknown>) {
  return request(app)
    .post(`/v1/moments/${momentId}/expenses`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `qh-exp-${randomUUID()}`)
    .send(body);
}

describe('S9-QH transaction CRUD', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('GET expense returns full detail with attachmentIds', async () => {
    const uid = `qh-get-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);
    const create = await createExpense(uid, momentId, {
      amount: '250.00',
      currencyCode: 'INR',
      merchantName: 'Detail Cafe',
      categoryCode: 'FOOD',
      subcategoryCode: 'CAFE',
    });
    assert.equal(create.status, 201, JSON.stringify(create.body));
    const expenseId = create.body.data.expenseId;

    const get = await request(app)
      .get(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(get.status, 200, JSON.stringify(get.body));
    assert.equal(get.body.data.expenseId, expenseId);
    assert.equal(get.body.data.subcategoryCode, 'CAFE');
    assert.ok(Array.isArray(get.body.data.attachmentIds));
  });

  it('PATCH expense updates fields, writes audit/event, rejects transactionType', async () => {
    const uid = `qh-patch-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);
    const create = await createExpense(uid, momentId, {
      amount: '100.00',
      currencyCode: 'INR',
      merchantName: 'Before',
    });
    assert.equal(create.status, 201);
    const expenseId = create.body.data.expenseId;

    const rejectType = await request(app)
      .patch(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ transactionType: 'INCOME' });
    assert.equal(rejectType.status, 400);

    const newEffectiveAt = new Date(Date.now() - 86400000).toISOString();
    const patch = await request(app)
      .patch(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({
        amount: '150.00',
        merchantName: 'After',
        subcategoryCode: 'GROCERIES',
        paymentMethodCode: 'UPI',
        effectiveAt: newEffectiveAt,
      });
    assert.equal(patch.status, 200, JSON.stringify(patch.body));
    assert.equal(patch.body.data.amount, '150.0000');

    const audits = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM audit.audit_record
       WHERE resource_id = $1 AND action_code = 'EXPENSE_UPDATE'`,
      [expenseId]
    );
    assert.equal(audits.rows[0]?.n, '1');

    const events = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.domain_event
       WHERE aggregate_id = $1 AND event_name = 'ExpenseUpdated'`,
      [expenseId]
    );
    assert.equal(events.rows[0]?.n, '1');
  });

  it('DELETE voids expense and hides from activity projection', async () => {
    const uid = `qh-void-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);
    const create = await createExpense(uid, momentId, {
      amount: '33.00',
      currencyCode: 'INR',
      merchantName: 'Void Me',
    });
    assert.equal(create.status, 201);
    const expenseId = create.body.data.expenseId;

    const del = await request(app)
      .delete(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(del.status, 200, JSON.stringify(del.body));
    assert.equal(del.body.data.status, 'VOIDED');

    const row = await getPool().query<{ status: string }>(
      `SELECT status FROM finance.expense WHERE expense_id = $1`,
      [expenseId]
    );
    assert.equal(row.rows[0]?.status, 'VOIDED');

    const activity = await request(app)
      .get('/v1/personal/activity')
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(activity.status, 200);
    const items = activity.body.data?.items ?? [];
    const visible = items.filter(
      (i: { activityPayload?: { expenseId?: string; status?: string } }) =>
        i.activityPayload?.expenseId === expenseId && i.activityPayload?.status !== 'VOIDED'
    );
    assert.equal(visible.length, 0);
  });

  it('lists financial accounts for user scope', async () => {
    const uid = `qh-acct-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const res = await request(app)
      .get('/v1/financial-accounts')
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.ok(Array.isArray(res.body.data));
    assert.ok(res.body.data.length >= 1);
  });

  it('uploads media and attaches to expense', async () => {
    const uid = `qh-att-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);
    const create = await createExpense(uid, momentId, {
      amount: '11.00',
      currencyCode: 'INR',
      merchantName: 'Receipt',
    });
    assert.equal(create.status, 201);
    const expenseId = create.body.data.expenseId;

    const intent = await request(app)
      .post('/v1/media/uploads')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `qh-up-${randomUUID()}`)
      .send({
        contentType: 'image/jpeg',
        byteSize: 1024,
        scopeType: 'MOMENT',
        scopeId: momentId,
      });
    assert.equal(intent.status, 201, JSON.stringify(intent.body));
    const uploadId = intent.body.data.uploadId;

    const complete = await request(app)
      .post(`/v1/media/uploads/${uploadId}/complete`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `qh-cmp-${randomUUID()}`)
      .send({ storageKey: `dev/${uploadId}` });
    assert.equal(complete.status, 200, JSON.stringify(complete.body));

    const attach = await request(app)
      .post(`/v1/moments/${momentId}/expenses/${expenseId}/attachments`)
      .set('X-Dev-Firebase-Uid', uid)
      .send({ uploadId });
    assert.equal(attach.status, 201, JSON.stringify(attach.body));

    const get = await request(app)
      .get(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(get.status, 200);
    assert.ok(get.body.data.attachmentIds.includes(uploadId));
  });

  it('creates and voids personal income via movement resource', async () => {
    const uid = `qh-inc-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);

    const create = await request(app)
      .post(`/v1/moments/${momentId}/income`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `qh-inc-${randomUUID()}`)
      .send({
        amount: '5000.00',
        currencyCode: 'INR',
        merchantName: 'Salary',
        categoryCode: 'INCOME',
      });
    assert.equal(create.status, 201, JSON.stringify(create.body));
    const incomeId = create.body.data.incomeId;

    const del = await request(app)
      .delete(`/v1/moments/${momentId}/income/${incomeId}`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(del.status, 200, JSON.stringify(del.body));
    assert.equal(del.body.data.status, 'VOIDED');
  });

  it('creates recurring schedule and generates idempotent instance', async () => {
    const uid = `qh-rec-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@qh.local`);
    const momentId = await createPersonalMoment(uid);
    const today = new Date().toISOString().slice(0, 10);

    const schedule = await request(app)
      .post(`/v1/moments/${momentId}/recurring-schedules`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `qh-rec-${randomUUID()}`)
      .send({
        resourceKind: 'EXPENSE',
        templatePayload: {
          amount: '99.00',
          currencyCode: 'INR',
          merchantName: 'Rent',
          categoryCode: 'HOUSING',
        },
        frequency: 'MONTHLY',
        intervalCount: 1,
        startDate: today,
      });
    assert.equal(schedule.status, 201, JSON.stringify(schedule.body));
    const scheduleId = schedule.body.data.recurringScheduleId;

    const gen1 = await request(app)
      .post(`/v1/moments/${momentId}/recurring-schedules/${scheduleId}/generate`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `qh-gen-${randomUUID()}`);
    assert.equal(gen1.status, 201, JSON.stringify(gen1.body));

    const gen2 = await request(app)
      .post(`/v1/moments/${momentId}/recurring-schedules/${scheduleId}/generate`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', gen1.headers['idempotency-key'] ?? `qh-gen-${randomUUID()}`);
    assert.ok(gen2.status === 201 || gen2.status === 200);
  });

  it('outsider cannot PATCH or DELETE expense', async () => {
    const uidA = `qh-a-${randomUUID().slice(0, 8)}`;
    const uidB = `qh-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@qh.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@qh.local`);
    const momentId = await createPersonalMoment(uidA);
    const create = await createExpense(uidA, momentId, { amount: '5.00', currencyCode: 'INR' });
    assert.equal(create.status, 201);
    const expenseId = create.body.data.expenseId;

    const patch = await request(app)
      .patch(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uidB)
      .send({ amount: '999.00' });
    assert.ok(patch.status === 403 || patch.status === 404);

    const del = await request(app)
      .delete(`/v1/moments/${momentId}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(del.status === 403 || del.status === 404);
  });
});
