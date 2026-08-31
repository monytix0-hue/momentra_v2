/**
 * S7 account / consent / devices list tests.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';
const uid = `s7-acct-${randomUUID().slice(0, 8)}`;

function auth() {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

describe('S7 account + consents + devices', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('PATCH /me updates displayName', async () => {
    await request(app).get('/v1/me').set(auth()).expect(200);
    const res = await request(app)
      .patch('/v1/me')
      .set(auth())
      .send({ displayName: 'S7 Tester' })
      .expect(200);
    assert.equal(res.body.data.displayName, 'S7 Tester');
  });

  it('GET /me/devices + register + list', async () => {
    const deviceId = `dev-${randomUUID().slice(0, 8)}`;
    await request(app)
      .post('/v1/me/devices')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ deviceId, platform: 'ANDROID', pushToken: 'tok-s7' })
      .expect(201);
    const list = await request(app).get('/v1/me/devices').set(auth()).expect(200);
    assert.ok(list.body.data.items.some((d: { deviceId: string }) => d.deviceId === deviceId));
  });

  it('consent grant + list + withdraw', async () => {
    await request(app)
      .post('/v1/me/consents/grant')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ purposeCode: 'PERSONAL_ANALYTICS' })
      .expect(201);
    const listed = await request(app).get('/v1/me/consents').set(auth()).expect(200);
    const row = listed.body.data.purposes.find((p: { code: string }) => p.code === 'PERSONAL_ANALYTICS');
    assert.equal(row?.granted, true);
    await request(app)
      .post('/v1/me/consents/withdraw')
      .set({ ...auth(), 'Idempotency-Key': randomUUID() })
      .send({ purposeCode: 'PERSONAL_ANALYTICS' })
      .expect(200);
  });

  it('DELETE /me soft-deletes profile', async () => {
    const res = await request(app).delete('/v1/me').set(auth()).expect(200);
    assert.equal(res.body.data.status, 'DELETED');
  });
});
