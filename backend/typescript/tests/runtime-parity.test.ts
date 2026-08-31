/**
 * Runtime OpenAPI shape checks for Phase 3 implemented vertical slices.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool } from '../src/platform/database/pool';
import { toProjectionHints } from '../src/platform/projections/hints';

const app = createApp();

describe('OpenAPI runtime parity (Phase 3)', () => {
  after(async () => {
    await closePool();
  });

  it('health/live matches health.yaml', async () => {
    const res = await request(app).get('/health/live');
    assert.equal(res.status, 200);
    assert.deepEqual(Object.keys(res.body).sort(), ['status']);
    assert.equal(res.body.status, 'ok');
  });

  it('health/ready matches health.yaml when ready', async () => {
    const res = await request(app).get('/health/ready');
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'ok');
  });

  it('GET /v1/me envelope has data + correlationId + shell inventory', async () => {
    const res = await request(app)
      .get('/v1/me')
      .set('X-Dev-Firebase-Uid', `parity-${randomUUID().slice(0, 8)}`);
    assert.equal(res.status, 200);
    assert.ok(res.body.data);
    assert.ok(res.body.correlationId);
    assert.match(res.body.correlationId, /^[0-9a-f-]{36}$/i);
    assert.ok(res.body.data.userId);
    assert.ok(res.body.data.firebaseUid);
    assert.ok(res.body.data.activeMoments);
    assert.ok(Array.isArray(res.body.data.companies));
    assert.ok(Array.isArray(res.body.data.supportedContexts));
  });

  it('device register returns command envelope; projectionHints typed helper', async () => {
    const hints = toProjectionHints(['personal.moments']);
    assert.equal(hints[0].projection, 'personal.moments');
    assert.equal(hints[0].action, 'invalidate');

    const res = await request(app)
      .post('/v1/me/devices')
      .set('X-Dev-Firebase-Uid', `parity-dev-${randomUUID().slice(0, 8)}`)
      .set('Idempotency-Key', `parity-${randomUUID()}`)
      .send({ platform: 'ANDROID', pushToken: `p-${randomUUID()}` });
    assert.equal(res.status, 201);
    assert.ok(res.body.data.deviceId);
    assert.ok(res.body.correlationId);
    assert.equal(res.body.error, undefined);
  });

  it('error envelope is flat OpenAPI shape', async () => {
    process.env.ALLOW_DEV_AUTH = '0';
    try {
      const res = await request(app).get('/v1/me');
      assert.equal(res.status, 401);
      assert.equal(res.body.code, 'UNAUTHORIZED');
      assert.ok(res.body.message);
      assert.ok(res.body.correlationId);
      assert.equal(res.body.error, undefined);
    } finally {
      process.env.ALLOW_DEV_AUTH = '1';
    }
  });
});
