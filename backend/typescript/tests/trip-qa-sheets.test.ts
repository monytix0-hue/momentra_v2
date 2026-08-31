/**
 * Trip Quick Add — memory media attach + poll multi + moment-bound invite.
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
    [userId, email, 'Trip QA Sheets']
  );
}

async function createTrip(organizerUid: string): Promise<{ momentId: string; title: string }> {
  const title = `Trip Sheets ${randomUUID().slice(0, 8)}`;
  const mint = await request(app)
    .post('/v1/group/invites')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `qa-mint-${randomUUID()}`)
    .send({ title, momentTypeCode: 'TRIP' });
  assert.equal(mint.status, 201, JSON.stringify(mint.body));

  const created = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `qa-moment-${randomUUID()}`)
    .send({
      domainCode: 'GROUP',
      momentTypeCode: 'TRIP',
      title,
      inviteCode: mint.body.data.inviteCode,
    });
  assert.equal(created.status, 201, JSON.stringify(created.body));
  return { momentId: created.body.data.momentId as string, title };
}

describe('Trip Quick Add sheets backends', () => {
  const orgUid = `trip-qa-org-${randomUUID()}`;
  const otherUid = `trip-qa-other-${randomUUID()}`;

  before(async () => {
    await getPool().query('SELECT 1');
    await ensureUser(userIdFor(orgUid), `${orgUid}@example.com`);
    await ensureUser(userIdFor(otherUid), `${otherUid}@example.com`);
  });

  after(async () => {
    await closePool();
  });

  it('createPlanningItem persists dueAt', async () => {
    const { momentId } = await createTrip(orgUid);
    const dueAt = new Date(Date.now() + 86400000).toISOString();
    const res = await request(app)
      .post(`/v1/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `plan-${randomUUID()}`)
      .send({ title: 'Day hike', dueAt });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const row = await getPool().query<{ due_at: Date | null }>(
      `SELECT due_at FROM collaboration.planning_item WHERE planning_item_id = $1`,
      [res.body.data.planningItemId]
    );
    assert.ok(row.rows[0]?.due_at);
    assert.equal(new Date(row.rows[0]!.due_at!).toISOString(), dueAt);
  });

  it('createPoll persists closesAt and MULTI_CHOICE', async () => {
    const { momentId } = await createTrip(orgUid);
    const closesAt = new Date(Date.now() + 3600000).toISOString();
    const res = await request(app)
      .post(`/v1/moments/${momentId}/polls`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `poll-${randomUUID()}`)
      .send({
        question: 'Where to eat?',
        options: ['A', 'B'],
        closesAt,
        pollType: 'MULTI_CHOICE',
      });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const row = await getPool().query<{ poll_type: string; closes_at: Date | null }>(
      `SELECT poll_type, closes_at FROM shared.poll WHERE poll_id = $1`,
      [res.body.data.pollId]
    );
    assert.equal(row.rows[0]?.poll_type, 'MULTI_CHOICE');
    assert.ok(row.rows[0]?.closes_at);
  });

  it('mintInvite with momentId binds invite to trip', async () => {
    const { momentId, title } = await createTrip(orgUid);
    const mint = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `bound-${randomUUID()}`)
      .send({ title, momentTypeCode: 'TRIP', momentId });
    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    assert.equal(mint.body.data.momentId, momentId);
    assert.equal(mint.body.data.status, 'ACTIVE');
  });

  it('createMemory + attach media after completed upload', async () => {
    const { momentId } = await createTrip(orgUid);
    const mem = await request(app)
      .post(`/v1/moments/${momentId}/memories`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `mem-${randomUUID()}`)
      .send({ title: 'Temple walk', capturedAt: new Date().toISOString() });
    assert.equal(mem.status, 201, JSON.stringify(mem.body));
    const memoryId = mem.body.data.memoryId as string;

    const intent = await request(app)
      .post('/v1/media/uploads')
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `up-${randomUUID()}`)
      .send({
        contentType: 'image/jpeg',
        byteSize: 128,
        scopeType: 'MOMENT',
        scopeId: momentId,
      });
    assert.equal(intent.status, 201, JSON.stringify(intent.body));
    const signedUrl = intent.body.data.signedUrl as string;
    const storageKey = intent.body.data.storageKey as string;
    const uploadId = intent.body.data.uploadId as string;
    assert.ok(signedUrl.includes('supabase'), `expected supabase signed URL, got ${signedUrl}`);
    assert.ok(storageKey);

    // Mark complete without requiring live PUT (bucket may be empty in CI);
    // production clients PUT first. completeUpload only checks ownership + key match.
    const complete = await request(app)
      .post(`/v1/media/uploads/${uploadId}/complete`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `done-${randomUUID()}`)
      .send({ storageKey });
    assert.equal(complete.status, 200, JSON.stringify(complete.body));

    const attach = await request(app)
      .post(`/v1/moments/${momentId}/memories/${memoryId}/media`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `att-${randomUUID()}`)
      .send({ uploadId });
    assert.equal(attach.status, 201, JSON.stringify(attach.body));

    const evidence = await getPool().query(
      `SELECT 1 FROM memory.memory_evidence
       WHERE memory_id = $1 AND source_type = 'MEDIA' AND source_id = $2`,
      [memoryId, uploadId]
    );
    assert.equal(evidence.rowCount, 1);

    const steal = await request(app)
      .post(`/v1/moments/${momentId}/memories/${memoryId}/media`)
      .set('X-Dev-Firebase-Uid', otherUid)
      .set('Idempotency-Key', `steal-${randomUUID()}`)
      .send({ uploadId });
    assert.ok([403, 404].includes(steal.status), JSON.stringify(steal.body));
  });
});
