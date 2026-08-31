/**
 * S1 Personal vertical slice integration tests.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { performance } from 'node:perf_hooks';
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
    [userId, email, 'S1 Test']
  );
}

async function firstPersonalTypeCode(): Promise<string> {
  const types = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
  );
  assert.ok(types.rows[0], 'Need PERSONAL moment type');
  return types.rows[0].code;
}

function percentile(samples: number[], p: number): number {
  const sorted = [...samples].sort((a, b) => a - b);
  const idx = Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length));
  return sorted[idx] ?? 0;
}

describe('S1 Personal slice', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('rejects unauthenticated requests', async () => {
    const prev = process.env.ALLOW_DEV_AUTH;
    process.env.ALLOW_DEV_AUTH = '0';
    try {
      const res = await request(app).get('/v1/personal/pulse');
      assert.equal(res.status, 401);
    } finally {
      process.env.ALLOW_DEV_AUTH = prev ?? '1';
    }
  });

  it('full personal path: create moment → expense → activity → pulse', async () => {
    const uid = `s1-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s1.local`);
    const typeCode = await firstPersonalTypeCode();

    const createRes = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s1-create-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'S1 life rhythm',
        personalSetup: {
          systemCode: 'LIFE_OPERATIONS',
          preferences: { lifeFocus: 'Daily balance' },
        },
      });
    assert.equal(createRes.status, 201, JSON.stringify(createRes.body));
    const momentId = createRes.body.data.momentId as string;
    assert.ok(createRes.body.projectionHints?.length);

    const expenseRes = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s1-exp-${randomUUID()}`)
      .send({ amount: '42.5000', currencyCode: 'USD', description: 'Coffee' });
    assert.equal(expenseRes.status, 201, JSON.stringify(expenseRes.body));
    assert.ok(
      expenseRes.body.projectionHints?.some(
        (h: { projection: string }) => h.projection === 'personal.activity'
      )
    );

    const activityRes = await request(app)
      .get('/v1/personal/activity')
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(activityRes.status, 200);
    assert.ok(
      activityRes.body.data.items.some(
        (i: { title: string }) => i.title === 'Coffee' || i.activityCode === 'EXPENSE_RECORDED'
      ),
      'expense visible in activity'
    );

    const pulseRes = await request(app)
      .get('/v1/personal/pulse')
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulseRes.status, 200);
    assert.ok(pulseRes.body.data.projectionVersion >= 1);
    const spend = pulseRes.body.data.widgetPayload?.spendByCurrency;
    assert.ok(spend?.USD, 'pulse spend updated');

    const secondCreateRes = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s1-create-2-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'S1 future build',
        personalSetup: {
          systemCode: 'FUTURE_BUILDING',
          preferences: { building: 'Career growth' },
        },
      });
    assert.equal(secondCreateRes.status, 201, JSON.stringify(secondCreateRes.body));
    const secondMomentId = secondCreateRes.body.data.momentId as string;

    const scopedActivityRes = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: secondMomentId })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(scopedActivityRes.status, 200);
    assert.equal(scopedActivityRes.body.data.items.length, 0, 'second moment activity starts isolated');

    const scopedPulseRes = await request(app)
      .get('/v1/personal/pulse')
      .query({ momentId: secondMomentId })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(scopedPulseRes.status, 200);
    assert.equal(
      Object.keys(scopedPulseRes.body.data.widgetPayload?.spendByCurrency ?? {}).length,
      0,
      'second moment spend starts isolated'
    );

    const audit = await getPool().query(
      `SELECT 1 FROM audit.audit_record WHERE actor_user_id = $1 AND action_code = 'EXPENSE_CREATE'`,
      [userId]
    );
    assert.ok(audit.rowCount && audit.rowCount > 0);

    const outbox = await getPool().query(
      `SELECT 1 FROM events.outbox_event oe
       JOIN events.domain_event de ON de.domain_event_id = oe.domain_event_id
       WHERE de.aggregate_type = 'EXPENSE'`,
      []
    );
    assert.ok(outbox.rowCount && outbox.rowCount > 0);
  });

  it('expense idempotency replays same result', async () => {
    const uid = `s1-idem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@s1.local`);
    const typeCode = await firstPersonalTypeCode();
    const create = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s1-m-${randomUUID()}`)
      .send({ domainCode: 'PERSONAL', momentTypeCode: typeCode, title: 'Idem moment' });
    const momentId = create.body.data.momentId;
    const idem = `s1-idem-${randomUUID()}`;
    const body = { amount: '10.00', currencyCode: 'EUR' };
    const first = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    const second = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    assert.equal(first.status, 201);
    assert.equal(second.status, 201);
    assert.equal(first.body.data.expenseId, second.body.data.expenseId);
  });

  it('cross-user isolation on personal moment expense', async () => {
    const uidA = `s1a-${randomUUID().slice(0, 8)}`;
    const uidB = `s1b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@s1.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@s1.local`);
    const typeCode = await firstPersonalTypeCode();
    const create = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `s1-iso-${randomUUID()}`)
      .send({ domainCode: 'PERSONAL', momentTypeCode: typeCode, title: 'Private' });
    const momentId = create.body.data.momentId;
    const blocked = await request(app)
      .post(`/v1/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `s1-b-${randomUUID()}`)
      .send({ amount: '5.00', currencyCode: 'USD' });
    assert.ok(blocked.status === 403 || blocked.status === 404, `expected 403/404 got ${blocked.status}`);
  });

  it('performance: warm pulse and activity reads', async () => {
    const uid = `s1-perf-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@perf.local`);
    const pulseSamples: number[] = [];
    const activitySamples: number[] = [];
    for (let i = 0; i < 10; i++) {
      const t0 = performance.now();
      await request(app).get('/v1/personal/pulse').set('X-Dev-Firebase-Uid', uid);
      pulseSamples.push(performance.now() - t0);
      const t1 = performance.now();
      await request(app).get('/v1/personal/activity').set('X-Dev-Firebase-Uid', uid);
      activitySamples.push(performance.now() - t1);
    }
    const pulseP95 = percentile(pulseSamples, 95);
    const activityP95 = percentile(activitySamples, 95);
    assert.ok(pulseP95 < 2000, `pulse p95 ${pulseP95}ms`);
    assert.ok(activityP95 < 2000, `activity p95 ${activityP95}ms`);
  });
});
