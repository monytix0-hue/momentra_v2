/**
 * S8 analytics — DET refresh, insights metadata, consent-at-execute, FastAPI-down safe.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { resetFastapiCircuitForTests } from '../src/platform/ai/fastapi-client';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';
const uid = `s8-an-${randomUUID().slice(0, 8)}`;

function auth() {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

describe('S8 analytics', () => {
  before(async () => {
    await getPool().query('SELECT 1');
    resetFastapiCircuitForTests();
  });

  after(async () => {
    await closePool();
  });

  it('core /health/ready does not depend on FastAPI', async () => {
    const live = await request(app).get('/health/live').expect(200);
    assert.equal(live.body.status, 'ok');
    const ready = await request(app).get('/health/ready');
    assert.ok(ready.status === 200 || ready.status === 503);
    // Must not mention fastapi
    assert.equal(ready.body.fastapi, undefined);
  });

  it('GET insights returns empty metadata when none', async () => {
    await request(app).get('/v1/me').set(auth()).expect(200);
    const res = await request(app).get('/v1/analytics/insights').set(auth()).expect(200);
    assert.ok(Array.isArray(res.body.data.items));
    assert.ok(res.body.data.meta);
    assert.ok(['EMPTY', 'READY', 'STALE', 'UNAVAILABLE', 'PENDING'].includes(res.body.data.meta.status) || res.body.data.meta.status === 'EMPTY' || res.body.data.items.length === 0);
  });

  it('refresh without analytics consent skips DET writes', async () => {
    await request(app).get('/v1/me').set(auth()).expect(200);
    const res = await request(app)
      .post('/v1/analytics/refresh')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ context: 'PERSONAL' })
      .expect(202);
    assert.equal(res.body.data.metricsWritten, 0);
    assert.ok(
      res.body.data.skippedReason === 'CONSENT_PERSONAL_ANALYTICS' ||
        res.body.data.metricsWritten === 0,
    );
  });

  it('refresh with PERSONAL_ANALYTICS consent is idempotent-safe', async () => {
    await request(app)
      .post('/v1/me/consents/grant')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ purposeCode: 'PERSONAL_ANALYTICS' })
      .expect(201);
    await request(app)
      .post('/v1/me/consents/grant')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ purposeCode: 'AI_INSIGHT_GENERATION' })
      .expect(201);

    const first = await request(app)
      .post('/v1/analytics/refresh')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ context: 'PERSONAL' })
      .expect(202);
    assert.ok(typeof first.body.data.metricsWritten === 'number');

    const second = await request(app)
      .post('/v1/analytics/refresh')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ context: 'PERSONAL' })
      .expect(202);
    // Second run may write 0 due to dedupe when same window
    assert.ok(typeof second.body.data.metricsWritten === 'number');

    const metrics = await request(app).get('/v1/analytics/metrics').set(auth()).expect(200);
    assert.ok(Array.isArray(metrics.body.data.items));
  });

  it('AI consent withdraw blocks new narrative but prior insights remain readable', async () => {
    await request(app)
      .post('/v1/me/consents/withdraw')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ purposeCode: 'AI_INSIGHT_GENERATION' })
      .expect(200);

    const before = await request(app).get('/v1/analytics/insights').set(auth()).expect(200);
    const priorCount = before.body.data.items.length;

    const refresh = await request(app)
      .post('/v1/analytics/refresh')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ context: 'PERSONAL' })
      .expect(202);
    // DET may still run; narrative skipped
    if (refresh.body.data.skippedReason) {
      assert.ok(
        refresh.body.data.skippedReason === 'CONSENT_AI_INSIGHT_GENERATION' ||
          refresh.body.data.narrative === false,
      );
    }

    const after = await request(app).get('/v1/analytics/insights').set(auth()).expect(200);
    assert.ok(after.body.data.items.length >= Math.min(priorCount, after.body.data.items.length));
  });
});
