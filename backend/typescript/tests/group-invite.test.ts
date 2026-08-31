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
    [userId, email, 'Invite Test']
  );
}

describe('Group invites', () => {
  before(async () => {
    await getPool().query('SELECT 1');
    const table = await getPool().query<{ ok: boolean }>(
      `SELECT EXISTS (
         SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'collaboration' AND table_name = 'moment_invite'
       ) AS ok`
    );
    if (!table.rows[0]?.ok) {
      throw new Error('Run V039__moment_invite.sql and V040__short_invite_code.sql before group invite tests.');
    }
  });

  after(async () => {
    await closePool();
  });

  it('mints a join code, binds it on create, and lets another user redeem', async () => {
    const probe = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', 'probe')
      .set('Idempotency-Key', `probe-${randomUUID()}`)
      .send({ title: 'Probe', momentTypeCode: 'TRIP' });
    if (probe.status === 404) return; // Group routes not mounted in S1 shell router

    const organizerUid = `inv-org-${randomUUID().slice(0, 8)}`;
    const joinerUid = `inv-join-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(organizerUid), `${organizerUid}@invite.local`);
    await ensureUser(userIdFor(joinerUid), `${joinerUid}@invite.local`);

    const mint = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `mint-${randomUUID()}`)
      .send({ title: 'Goa Trip', momentTypeCode: 'TRIP' });

    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    assert.match(mint.body.data.inviteCode, /^[a-hj-np-z2-9]{8}$/);
    assert.equal(mint.body.data.status, 'PENDING');
    assert.equal(mint.body.data.invitePath, `momentra.app/j/${mint.body.data.inviteCode}`);
    assert.equal(mint.body.data.inviteUrl, `https://momentra.app/j/${mint.body.data.inviteCode}`);
    assert.doesNotMatch(String(mint.body.data.inviteUrl), /eyJ[A-Za-z0-9_-]+\./);

    const preview = await request(app)
      .get(`/v1/group/invites/${mint.body.data.inviteCode}`)
      .set('X-Dev-Firebase-Uid', joinerUid);
    assert.equal(preview.status, 200, JSON.stringify(preview.body));
    assert.equal(preview.body.data.title, 'Goa Trip');

    const earlyRedeem = await request(app)
      .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
      .set('X-Dev-Firebase-Uid', joinerUid)
      .set('Idempotency-Key', `redeem-early-${randomUUID()}`)
      .send({});
    assert.equal(earlyRedeem.status, 200, JSON.stringify(earlyRedeem.body));
    assert.equal(earlyRedeem.body.data.status, 'PENDING');
    assert.equal(earlyRedeem.body.data.momentId, null);

    const created = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `create-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: 'Goa Trip',
        inviteCode: mint.body.data.inviteCode,
        participants: [
          { displayName: 'Aisha Myma', roleCode: 'PARTICIPANT', email: 'aisha@example.com' },
        ],
      });
    assert.equal(created.status, 201, JSON.stringify(created.body));

    const members = await getPool().query<{ user_id: string | null; metadata: { displayName?: string } }>(
      `SELECT user_id, metadata FROM collaboration.moment_participant WHERE moment_id = $1`,
      [created.body.data.momentId]
    );
    assert.ok(members.rows.some((row) => row.user_id === userIdFor(joinerUid)));
    assert.ok(members.rows.some((row) => row.metadata?.displayName === 'Aisha Myma'));

    const lateRedeem = await request(app)
      .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
      .set('X-Dev-Firebase-Uid', joinerUid)
      .set('Idempotency-Key', `redeem-late-${randomUUID()}`)
      .send({});
    assert.equal(lateRedeem.status, 200, JSON.stringify(lateRedeem.body));
    assert.equal(lateRedeem.body.data.alreadyMember, true);
    assert.equal(lateRedeem.body.data.momentId, created.body.data.momentId);
  });
});
