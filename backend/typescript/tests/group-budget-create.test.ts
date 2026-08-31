/**
 * Group budget create + PATCH + pulse readback.
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
    [userId, email, 'Group Budget Test']
  );
}

describe('group budget create and patch', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('create with groupSetup seeds budgetTotal on pulse finance', async () => {
    const orgUid = `gb-org-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@gb.local`);

    const mint = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gb-mint-${randomUUID()}`)
      .send({ title: 'Budget Trip', momentTypeCode: 'TRIP' });
    assert.equal(mint.status, 201, JSON.stringify(mint.body));

    const created = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gb-moment-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: 'Budget Trip',
        inviteCode: mint.body.data.inviteCode,
        groupSetup: {
          budgetAmount: '84000',
          budgetCurrencyCode: 'INR',
          destinationText: 'Goa, India',
        },
      });
    assert.equal(created.status, 201, JSON.stringify(created.body));
    const momentId = created.body.data.momentId as string;

    const finance = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(finance.status, 200, JSON.stringify(finance.body));
    const totals = finance.body.data.payload.totals as Array<{ budgetTotal: string; currencyCode: string }>;
    assert.ok(totals.some((t) => t.currencyCode === 'INR' && t.budgetTotal === '84000.0000'));

    const pulse = await request(app)
      .get(`/v1/group/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    const pulseTotals = pulse.body.data.payload.finance.totals as Array<{ budgetTotal: string }>;
    assert.ok(pulseTotals.some((t) => t.budgetTotal === '84000.0000'));

    const dest = await getPool().query<{ destination_text: string }>(
      `SELECT destination_text FROM collaboration.shared_experience_context WHERE moment_id = $1`,
      [momentId]
    );
    assert.equal(dest.rows[0]?.destination_text, 'Goa, India');
  });

  it('PATCH updates budgetTotal on finance projection', async () => {
    const orgUid = `gb-patch-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@gb.local`);

    const created = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gb-patch-moment-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: 'Patch Budget Trip',
        groupSetup: { budgetAmount: '50000', budgetCurrencyCode: 'INR' },
      });
    assert.equal(created.status, 201, JSON.stringify(created.body));
    const momentId = created.body.data.momentId as string;

    const patched = await request(app)
      .patch(`/v1/group/moments/${momentId}/budget`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `gb-patch-${randomUUID()}`)
      .send({ budgetAmount: '95000', budgetCurrencyCode: 'INR' });
    assert.equal(patched.status, 200, JSON.stringify(patched.body));
    assert.equal(patched.body.data.budgetAmount, '95000');

    const finance = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(finance.status, 200, JSON.stringify(finance.body));
    const totals = finance.body.data.payload.totals as Array<{ budgetTotal: string }>;
    assert.ok(totals.some((t) => t.budgetTotal === '95000.0000'));
  });
});
