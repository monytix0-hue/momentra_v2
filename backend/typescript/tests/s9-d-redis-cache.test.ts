/**
 * S9-D — Redis key contract, fail-open, rate-limit memory fallback on soft-fail.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import {
  CompositeRateLimiter,
  MemoryRateLimiter,
  RedisRateLimiter,
  setRateLimiter,
  resetRateLimiterFromEnv,
} from '../src/platform/rate-limit/interface';
import {
  analyticsInsightCacheKey,
  assertKeyMatchesContract,
  REDIS_KEY_CONTRACT,
  rateLimitBucketKey,
} from '../src/platform/redis/key-contract';
import {
  disableRedisForTests,
  enableRedisForTests,
  closeRedis,
} from '../src/platform/redis/client';
import { config } from '../src/platform/config';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

describe('S9-D Redis key contract', () => {
  it('documents authorization scope and invalidation owner for every key', () => {
    assert.ok(REDIS_KEY_CONTRACT.length >= 2);
    for (const row of REDIS_KEY_CONTRACT) {
      assert.ok(row.prefix.length > 0);
      assert.ok(row.authorizationScope.includes('userId') || row.authorizationScope.includes('IP'));
      assert.ok(row.invalidationOwner.length > 0);
      assert.ok(row.reader.length > 0);
    }
  });

  it('builds scoped analytics keys that include userId and scope ids', () => {
    const key = analyticsInsightCacheKey('u1', 'MOMENT', 'm1');
    assert.equal(key, 'analytics:insight:u1:MOMENT:m1');
    assert.ok(assertKeyMatchesContract(key));
    assert.throws(() => analyticsInsightCacheKey('', 'MOMENT', 'm1'));
  });

  it('builds rate-limit keys under rl: prefix', () => {
    const key = rateLimitBucketKey('user-abc', 60, 1_700_000_000_000);
    assert.ok(key.startsWith('rl:user-abc:'));
    assert.ok(assertKeyMatchesContract(key));
  });
});

describe('S9-D rate-limit soft-fail → memory', () => {
  after(() => {
    resetRateLimiterFromEnv();
  });

  it('falls through to memory when Redis soft-fails', async () => {
    disableRedisForTests();
    const limiter = new CompositeRateLimiter(new RedisRateLimiter(60, 2), new MemoryRateLimiter(60, 2));
    const k = `s9d-${randomUUID().slice(0, 6)}`;
    assert.equal((await limiter.check(k)).allowed, true);
    assert.equal((await limiter.check(k)).allowed, true);
    assert.equal((await limiter.check(k)).allowed, false);
    enableRedisForTests();
  });
});

describe('S9-D Redis-down canonical commands', () => {
  after(async () => {
    enableRedisForTests();
    resetRateLimiterFromEnv();
    await closeRedis();
    await closePool();
  });

  it('GET /v1/me succeeds with Redis disabled', async () => {
    disableRedisForTests();
    setRateLimiter(new MemoryRateLimiter(60, 1000));
    const uid = `s9d-me-${randomUUID().slice(0, 6)}`;
    const res = await request(app)
      .get('/v1/me')
      .set({ 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId });
    assert.equal(res.status, 200);
    assert.ok(res.body.data?.userId);
  });

  it('POST group expense still works with Redis disabled (canonical write)', async () => {
    disableRedisForTests();
    setRateLimiter(new MemoryRateLimiter(60, 1000));
    const uid = `s9d-exp-${randomUUID().slice(0, 6)}`;
    const auth = { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
    const me = await request(app).get('/v1/me').set(auth);
    assert.equal(me.status, 200);

    const moment = await request(app)
      .post('/v1/moments')
      .set({ ...auth, 'Idempotency-Key': randomUUID() })
      .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: 'S9D Redis down' });
    assert.ok(moment.status === 200 || moment.status === 201, JSON.stringify(moment.body));
    const momentId = moment.body.data.momentId as string;

    const parts = await getPool().query<{ participant_id: string }>(
      `SELECT participant_id FROM collaboration.moment_participant
       WHERE moment_id = $1 AND status = 'ACTIVE' LIMIT 1`,
      [momentId]
    );
    const paidBy = parts.rows[0]!.participant_id;

    const exp = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set({ ...auth, 'Idempotency-Key': randomUUID() })
      .send({
        amount: '12.00',
        currencyCode: 'USD',
        description: 'S9D',
        paidByParticipantId: paidBy,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: paidBy }],
      });
    assert.ok(exp.status < 500, `status=${exp.status} body=${JSON.stringify(exp.body)}`);
    assert.equal(exp.status, 201);
    assert.ok(exp.body.data?.expenseId);
  });
});
