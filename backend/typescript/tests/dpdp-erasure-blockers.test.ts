/**
 * DPDP principal-owned erasure blockers: sole organizer with other members.
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
const ownerUid = `dpdp-own-${randomUUID().slice(0, 8)}`;
const memberUid = `dpdp-mem-${randomUUID().slice(0, 8)}`;

function auth(uid: string) {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

describe('DPDP erasure blockers', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('blocks erase when user is group organizer with other active members', async () => {
    const ownerMe = await request(app).get('/v1/me').set(auth(ownerUid)).expect(200);
    const memberMe = await request(app).get('/v1/me').set(auth(memberUid)).expect(200);
    const ownerId = ownerMe.body.data.userId as string;
    const memberId = memberMe.body.data.userId as string;

    const pool = getPool();
    const momentId = randomUUID();
    const type = await pool.query<{ moment_type_id: string }>(
      `SELECT moment_type_id FROM core.moment_type WHERE domain_code = 'GROUP' LIMIT 1`,
    );
    assert.ok(type.rowCount, 'need a GROUP moment_type');

    await pool.query(
      `INSERT INTO core.moment (
         moment_id, domain_code, moment_type_id, created_by_user_id, title, status, version
       ) VALUES ($1, 'GROUP', $2, $3, 'DPDP blocker group', 'ACTIVE', 1)`,
      [momentId, type.rows[0].moment_type_id, ownerId],
    );
    await pool.query(
      `INSERT INTO collaboration.group_moment_context (
         moment_id, domain_code, group_family, organizer_user_id, status, version
       ) VALUES ($1, 'GROUP', 'SHARED_EXPERIENCE', $2, 'ACTIVE', 1)`,
      [momentId, ownerId],
    );
    await pool.query(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, version, joined_at
       ) VALUES
         ($1, $2, 'ORGANIZER', 'ACTIVE', 1, now()),
         ($1, $3, 'PARTICIPANT', 'ACTIVE', 1, now())`,
      [momentId, ownerId, memberId],
    );

    const preflight = await request(app)
      .get('/v1/me/erasure-preflight')
      .set(auth(ownerUid))
      .expect(200);
    assert.equal(preflight.body.data.canErase, false);
    assert.ok(
      preflight.body.data.blockers.some(
        (b: { kind: string; id: string }) =>
          b.kind === 'GROUP_ORGANIZER_TRANSFER_REQUIRED' && b.id === momentId,
      ),
    );

    const del = await request(app).delete('/v1/me').set(auth(ownerUid)).expect(400);
    assert.equal(del.body.code, 'VALIDATION_FAILED');
  });
});
