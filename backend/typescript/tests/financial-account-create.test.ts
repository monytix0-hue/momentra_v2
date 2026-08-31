/**
 * POST /v1/financial-accounts — create + list provisioning.
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

function auth(uid: string) {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

async function ensureUser(uid: string): Promise<void> {
  const userId = firebaseUserId(projectId, uid);
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, `${uid}@facct.local`, 'Financial Account Test']
  );
}

describe('financial account create', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('POST creates account and GET lists it', async () => {
    const uid = `facct-${randomUUID().slice(0, 8)}`;
    await ensureUser(uid);
    const name = `HDFC Savings ${uid.slice(-4)}`;

    const created = await request(app)
      .post('/v1/financial-accounts')
      .set({ ...auth(uid), 'Idempotency-Key': `facct-create-${randomUUID()}` })
      .send({
        accountType: 'BANK',
        accountName: name,
        currencyCode: 'INR',
        institutionName: 'HDFC',
      });

    assert.equal(created.status, 201, JSON.stringify(created.body));
    assert.ok(created.body.data.financialAccountId);
    assert.equal(created.body.data.accountName, name);
    assert.equal(created.body.data.accountType, 'BANK');
    assert.equal(created.body.data.currencyCode, 'INR');

    const listed = await request(app).get('/v1/financial-accounts').set(auth(uid));
    assert.equal(listed.status, 200, JSON.stringify(listed.body));
    assert.ok(Array.isArray(listed.body.data));
    assert.ok(
      listed.body.data.some(
        (row: { financialAccountId: string; accountName: string }) =>
          row.financialAccountId === created.body.data.financialAccountId && row.accountName === name
      ),
      'Created account should appear in list'
    );
  });
});
