/**
 * Life Ops Money Quick Add — POST /moments/:id/movements (transfer + savings deposit).
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
    [userId, email, 'Movement Record Test']
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
    .set('Idempotency-Key', `mv-moment-${randomUUID()}`)
    .send({
      domainCode: 'PERSONAL',
      momentTypeCode: typeCode,
      title: `Movement ${uid.slice(0, 8)}`,
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function createMovement(uid: string, momentId: string, body: Record<string, unknown>) {
  return request(app)
    .post(`/v1/moments/${momentId}/movements`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `mv-${randomUUID()}`)
    .send(body);
}

describe('Life Ops movement record', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('POST transfer movement returns 201', async () => {
    const uid = `mv-tr-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mv.local`);
    const momentId = await createPersonalMoment(uid);
    const res = await createMovement(uid, momentId, {
      movementType: 'TRANSFER',
      amount: '500.00',
      currencyCode: 'INR',
      description: 'Transfer · One-time · Now',
    });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.equal(res.body.data.movementType, 'TRANSFER');
    assert.equal(res.body.data.amount, '500.0000');
    assert.ok(res.body.data.movementId);
  });

  it('POST savings deposit movement returns 201', async () => {
    const uid = `mv-sv-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@mv.local`);
    const momentId = await createPersonalMoment(uid);
    const res = await createMovement(uid, momentId, {
      movementType: 'SAVINGS_DEPOSIT',
      amount: '1200.50',
      currencyCode: 'INR',
      description: 'House Fund · One-time · Now',
    });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.equal(res.body.data.movementType, 'SAVINGS_DEPOSIT');
    assert.equal(res.body.data.amount, '1200.5000');
    assert.ok(res.body.data.movementId);
  });
});
