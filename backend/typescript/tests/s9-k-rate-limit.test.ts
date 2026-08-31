/**
 * S9-K — rate limit mounted on /v1; enforce with injectable limiter.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool } from '../src/platform/database/pool';
import {
  MemoryRateLimiter,
  setRateLimiter,
  resetRateLimiterFromEnv,
} from '../src/platform/rate-limit/interface';

const app = createApp();

describe('S9-K rate limit', () => {
  after(async () => {
    resetRateLimiterFromEnv();
    await closePool();
  });

  it('returns 429 when per-user limit exceeded', async () => {
    setRateLimiter(new MemoryRateLimiter(60, 3));
    const uid = `s9k-${randomUUID().slice(0, 8)}`;
    const auth = { 'X-Dev-Firebase-Uid': uid };

    await request(app).get('/v1/me').set(auth).expect(200);
    await request(app).get('/v1/me').set(auth).expect(200);
    await request(app).get('/v1/me').set(auth).expect(200);
    const limited = await request(app).get('/v1/me').set(auth).expect(429);
    assert.equal(limited.body.code, 'RATE_LIMITED');
    assert.ok(limited.headers['retry-after']);
  });

  it('isolates limits per user', async () => {
    setRateLimiter(new MemoryRateLimiter(60, 1));
    const a = { 'X-Dev-Firebase-Uid': `s9k-a-${randomUUID().slice(0, 6)}` };
    const b = { 'X-Dev-Firebase-Uid': `s9k-b-${randomUUID().slice(0, 6)}` };

    await request(app).get('/v1/me').set(a).expect(200);
    await request(app).get('/v1/me').set(a).expect(429);
    await request(app).get('/v1/me').set(b).expect(200);
  });
});
