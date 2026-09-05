/**
 * Organizer-only guest members + expense split inclusion.
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

async function ensureUser(userId: string, email: string, name: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, name]
  );
}

async function createGroupWithMember(
  organizerUid: string,
  memberUid: string
): Promise<{ momentId: string; organizerParticipantId: string; memberParticipantId: string }> {
  const mint = await request(app)
    .post('/v1/group/invites')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `guest-mint-${randomUUID()}`)
    .send({ title: 'Guest Trip', momentTypeCode: 'TRIP' });
  assert.equal(mint.status, 201, JSON.stringify(mint.body));

  const created = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `guest-moment-${randomUUID()}`)
    .send({
      domainCode: 'GROUP',
      momentTypeCode: 'TRIP',
      title: 'Guest Trip',
      inviteCode: mint.body.data.inviteCode,
    });
  assert.equal(created.status, 201, JSON.stringify(created.body));
  const momentId = created.body.data.momentId as string;

  const redeem = await request(app)
    .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
    .set('X-Dev-Firebase-Uid', memberUid)
    .set('Idempotency-Key', `guest-redeem-${randomUUID()}`)
    .send({});
  assert.equal(redeem.status, 200, JSON.stringify(redeem.body));

  const parts = await getPool().query<{ participant_id: string; user_id: string }>(
    `SELECT participant_id, user_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE' AND user_id IS NOT NULL`,
    [momentId]
  );
  return {
    momentId,
    organizerParticipantId: parts.rows.find((r) => r.user_id === userIdFor(organizerUid))!.participant_id,
    memberParticipantId: parts.rows.find((r) => r.user_id === userIdFor(memberUid))!.participant_id,
  };
}

describe('Group guest members', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('organizer can add guest; member cannot; guest is ACTIVE and splittable', async () => {
    const organizerUid = `guest-org-${randomUUID().slice(0, 8)}`;
    const memberUid = `guest-mem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(organizerUid), `${organizerUid}@guest.local`, 'Organizer');
    await ensureUser(userIdFor(memberUid), `${memberUid}@guest.local`, 'Member');

    const { momentId, organizerParticipantId } = await createGroupWithMember(organizerUid, memberUid);

    const denied = await request(app)
      .post(`/v1/moments/${momentId}/participants`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `guest-denied-${randomUUID()}`)
      .send({ displayName: 'Walk-in Guest' });
    assert.equal(denied.status, 403, JSON.stringify(denied.body));

    const added = await request(app)
      .post(`/v1/moments/${momentId}/participants`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `guest-add-${randomUUID()}`)
      .send({ displayName: 'Walk-in Guest' });
    assert.equal(added.status, 201, JSON.stringify(added.body));
    const guestParticipantId = added.body.data.participantId as string;

    const listed = await request(app)
      .get(`/v1/group/moments/${momentId}/participants`)
      .set('X-Dev-Firebase-Uid', organizerUid);
    assert.equal(listed.status, 200, JSON.stringify(listed.body));
    const guest = (listed.body.data.participants as Array<Record<string, unknown>>).find(
      (p) => p.participantId === guestParticipantId
    );
    assert.ok(guest);
    assert.equal(guest.isGuest, true);
    assert.equal(guest.roleLabel, 'Guest');
    assert.equal(guest.status, 'ACTIVE');
    assert.equal(guest.displayName, 'Walk-in Guest');
    assert.equal(guest.userId, null);

    const expense = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `guest-exp-${randomUUID()}`)
      .send({
        amount: '100.00',
        currencyCode: 'INR',
        description: 'Guest dinner',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [
          { participantId: organizerParticipantId },
          { participantId: guestParticipantId },
        ],
      });
    assert.equal(expense.status, 201, JSON.stringify(expense.body));
  });
});
