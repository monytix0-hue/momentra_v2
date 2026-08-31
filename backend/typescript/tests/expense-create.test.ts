/**
 * Phase 8 Personal Expense.Create — idempotency, money validation, audit/event/outbox, activity, isolation.
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
    [userId, email, 'Phase8 Expense Test']
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
    .set('Idempotency-Key', `p8-moment-${randomUUID()}`)
    .send({
      domainCode: 'PERSONAL',
      momentTypeCode: typeCode,
      title: `Expense Moment ${uid.slice(0, 8)}`,
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

describe('Phase 8 expense create', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('creates Personal expense with decimal amount and projectionHints', async () => {
    const uid = `p8-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);

    const res = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-exp-${randomUUID()}`)
      .send({
        amount: '1250.50',
        currencyCode: 'INR',
        description: 'Lunch',
        merchantName: 'Cafe',
      });

    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.ok(res.body.data.expenseId);
    assert.equal(res.body.data.momentId, momentId);
    assert.equal(res.body.data.amount, '1250.5000');
    assert.equal(res.body.data.currencyCode, 'INR');
    assert.equal(res.body.data.status, 'POSTED');
    assert.equal(res.body.resourceVersion, 1);
    assert.ok(res.body.projectionHints?.some((h: { projection: string }) => h.projection === 'personal.activity'));
    assert.ok(res.body.projectionHints?.some((h: { projection: string }) => h.projection === 'personal.pulse'));
  });

  it('rejects invalid amount and currency', async () => {
    const uid = `p8-val-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);

    const zero = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-zero-${randomUUID()}`)
      .send({ amount: '0', currencyCode: 'INR' });
    assert.equal(zero.status, 400);

    const neg = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-neg-${randomUUID()}`)
      .send({ amount: '-10', currencyCode: 'INR' });
    assert.equal(neg.status, 400);

    const badCurrency = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-cur-${randomUUID()}`)
      .send({ amount: '10.00', currencyCode: 'IN' });
    assert.equal(badCurrency.status, 400);

    const splitsRejected = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-split-${randomUUID()}`)
      .send({
        amount: '10.00',
        currencyCode: 'INR',
        splits: [{ strategy: 'EQUAL' }],
      });
    assert.equal(splitsRejected.status, 400);
  });

  it('idempotency replay returns same expense; conflict on different body', async () => {
    const uid = `p8-idem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);
    const idem = `p8-idem-${randomUUID()}`;
    const body = { amount: '99.99', currencyCode: 'USD', description: 'Replay' };

    const first = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    assert.equal(first.status, 201, JSON.stringify(first.body));

    const second = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    assert.equal(second.status, 201);
    assert.equal(second.body.data.expenseId, first.body.data.expenseId);

    const conflict = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send({ amount: '1.00', currencyCode: 'USD' });
    assert.equal(conflict.status, 409);
  });

  it('concurrent same Idempotency-Key yields one expense', async () => {
    const uid = `p8-conc-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);
    const idem = `p8-conc-${randomUUID()}`;
    const body = { amount: '42.00', currencyCode: 'EUR' };

    const [a, b] = await Promise.all([
      request(app)
        .post(`/v1/moments/${momentId}/expenses`)
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', idem)
        .send(body),
      request(app)
        .post(`/v1/moments/${momentId}/expenses`)
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', idem)
        .send(body),
    ]);

    assert.equal(a.status, 201, JSON.stringify(a.body));
    assert.equal(b.status, 201, JSON.stringify(b.body));
    assert.equal(a.body.data.expenseId, b.body.data.expenseId);

    const count = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.expense WHERE moment_id = $1 AND amount = 42`,
      [momentId]
    );
    assert.equal(count.rows[0]?.n, '1');
  });

  it('writes audit, domain event, and outbox', async () => {
    const uid = `p8-audit-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);

    const res = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-audit-${randomUUID()}`)
      .send({ amount: '15.00', currencyCode: 'INR', description: 'Audit expense' });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const expenseId = res.body.data.expenseId;

    const audits = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM audit.audit_record
       WHERE resource_type = 'EXPENSE' AND resource_id = $1 AND action_code = 'EXPENSE_CREATE'`,
      [expenseId]
    );
    assert.equal(audits.rows[0]?.n, '1');

    const events = await getPool().query<{ event_id: string }>(
      `SELECT domain_event_id::text AS event_id FROM events.domain_event
       WHERE aggregate_id = $1 AND event_name = 'ExpenseRecorded'`,
      [expenseId]
    );
    assert.equal(events.rows.length, 1);

    const outbox = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.outbox_event
       WHERE domain_event_id = $1::uuid`,
      [events.rows[0].event_id]
    );
    assert.equal(outbox.rows[0]?.n, '1');
  });

  it('read-after-create appears in personal activity', async () => {
    const uid = `p8-act-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase8.local`);
    const momentId = await createPersonalMoment(uid);

    const create = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p8-act-${randomUUID()}`)
      .send({ amount: '7.25', currencyCode: 'INR', description: 'Visible expense' });
    assert.equal(create.status, 201, JSON.stringify(create.body));
    const expenseId = create.body.data.expenseId;

    const activity = await request(app)
      .get('/v1/personal/activity')
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(activity.status, 200, JSON.stringify(activity.body));
    const page = activity.body.data;
    const list = Array.isArray(page?.items) ? page.items : Array.isArray(page) ? page : [];
    const found = list.some(
      (row: { title?: string; activityCode?: string }) =>
        row.title === 'Visible expense' || row.activityCode === 'EXPENSE_RECORDED'
    );
    assert.ok(found, `expense activity not found: ${JSON.stringify(activity.body)}`);

    const dbRow = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.expense WHERE expense_id = $1`,
      [expenseId]
    );
    assert.equal(dbRow.rows[0]?.n, '1');
  });

  it('outsider cannot create on another user personal moment', async () => {
    const uidA = `p8-a-${randomUUID().slice(0, 8)}`;
    const uidB = `p8-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@phase8.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@phase8.local`);
    const momentId = await createPersonalMoment(uidA);

    const res = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `p8-iso-${randomUUID()}`)
      .send({ amount: '5.00', currencyCode: 'INR' });
    assert.ok(res.status === 403 || res.status === 404, JSON.stringify(res.body));
  });

  it('unauthenticated create returns 401', async () => {
    const prev = process.env.ALLOW_DEV_AUTH;
    process.env.ALLOW_DEV_AUTH = '0';
    try {
      const res = await request(app)
        .post(`/v1/moments/${randomUUID()}/expenses`)
        .set('Idempotency-Key', `p8-401-${randomUUID()}`)
        .send({ amount: '1.00', currencyCode: 'INR' });
      assert.equal(res.status, 401);
    } finally {
      process.env.ALLOW_DEV_AUTH = prev ?? '1';
    }
  });
});
