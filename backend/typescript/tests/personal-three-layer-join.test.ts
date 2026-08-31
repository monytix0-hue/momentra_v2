/**
 * Personal three-layer golden path — one write per family, then SQL + GET pulse/activity/axis.
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
    [userId, email, 'Personal Join']
  );
}

async function personalType(codeHint: string): Promise<string> {
  const types = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type
     WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE'
       AND (code = $1 OR code ILIKE $2)
     LIMIT 1`,
    [codeHint, `%${codeHint.split('_')[0]}%`]
  );
  if (types.rows[0]) return types.rows[0].code;
  const any = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
  );
  assert.ok(any.rows[0], 'Need PERSONAL moment type');
  return any.rows[0].code;
}

async function createPersonalMoment(
  uid: string,
  momentTypeCode: string,
  systemCode: string,
  title: string
): Promise<string> {
  const createRes = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `pj-create-${randomUUID()}`)
    .send({
      domainCode: 'PERSONAL',
      momentTypeCode,
      title,
      personalSetup: { systemCode, preferences: {} },
    });
  assert.ok([200, 201].includes(createRes.status), JSON.stringify(createRes.body));
  return createRes.body.data.momentId as string;
}

async function assertPulseAndActivity(uid: string, momentId: string, activityNeedle: string): Promise<void> {
  const pulse = await request(app)
    .get('/v1/personal/pulse')
    .query({ momentId })
    .set('X-Dev-Firebase-Uid', uid);
  assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
  assert.ok(pulse.body.data.projectionVersion >= 1);

  const activity = await request(app)
    .get('/v1/personal/activity')
    .query({ momentId, limit: 20 })
    .set('X-Dev-Firebase-Uid', uid);
  assert.equal(activity.status, 200, JSON.stringify(activity.body));
  const items = activity.body.data.items as { title: string; activityCode: string }[];
  const needle = activityNeedle.toLowerCase();
  assert.ok(
    items.some(
      (i) =>
        i.title.toLowerCase().includes(needle) ||
        i.activityCode.toLowerCase().includes(needle.replace(/\s+/g, '_'))
    ),
    JSON.stringify(items)
  );
}

describe('Personal three-layer join proof', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('V019/V050 Personal action codes map on Personal moment types', async () => {
    const required = [
      'EXPENSE_CREATE',
      'MOVEMENT_RECORD',
      'LIFE_OBSERVATION_RECORD',
      'LIFESTYLE_ACTIVITY_CREATE',
      'RELATIONSHIP_ACTIVITY_RECORD',
      'GOAL_CREATE',
      'MILESTONE_CREATE',
    ];
    const rows = await getPool().query<{ code: string; n: string }>(
      `SELECT c.code, COUNT(*)::text AS n
       FROM core.capability c
       JOIN core.moment_type_capability mtc ON mtc.capability_id = c.capability_id AND mtc.status = 'ACTIVE'
       JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id
       WHERE mt.domain_code = 'PERSONAL' AND mt.status = 'ACTIVE' AND c.status = 'ACTIVE'
         AND c.code = ANY($1::text[])
       GROUP BY c.code`,
      [required]
    );
    const found = new Set(rows.rows.map((r) => r.code));
    for (const code of required) {
      assert.ok(found.has(code), `Missing V019 map for Personal: ${code}`);
    }
  });

  it('Life Ops: attention write → SQL → pulse/activity + attention GET', async () => {
    const uid = `pj-lo-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@pj.local`);
    const typeCode = await personalType('LIFE_RHYTHM');
    const momentId = await createPersonalMoment(uid, typeCode, 'LIFE_OPERATIONS', 'PJ Life Ops');

    const write = await request(app)
      .post(`/v1/moments/${momentId}/attention-captures`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `pj-att-${randomUUID()}`)
      .send({
        categoryCode: 'WORK',
        intensityCode: 'MODERATE',
        timeBlockCode: 'MORNING',
        energyRemaining: 3,
        note: 'PJ attention proof',
      });
    assert.equal(write.status, 201, JSON.stringify(write.body));
    const captureId = write.body.data.attentionCaptureId as string;
    assert.ok(captureId);

    const sql = await getPool().query(
      `SELECT 1 FROM analytics.attention_capture WHERE attention_capture_id = $1 AND user_id = $2`,
      [captureId, userIdFor(uid)]
    );
    assert.equal(sql.rowCount, 1);

    await assertPulseAndActivity(uid, momentId, 'attention');

    const list = await request(app).get('/v1/personal/attention').set('X-Dev-Firebase-Uid', uid);
    assert.equal(list.status, 200);
    assert.ok(
      (list.body.data.items as { attentionCaptureId: string }[]).some((i) => i.attentionCaptureId === captureId)
    );

    const life = await request(app).get('/v1/personal/life').set('X-Dev-Firebase-Uid', uid);
    assert.equal(life.status, 200);
    assert.equal(life.body.data.dataQuality, 'REAL');
    assert.ok(life.body.data.activeAreaCount >= 1);
  });

  it('Future: future-item write → SQL → pulse/activity + axis', async () => {
    const uid = `pj-fb-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@pj.local`);
    const typeCode = await personalType('FUTURE_GOAL');
    const momentId = await createPersonalMoment(uid, typeCode, 'FUTURE_BUILDING', 'PJ Future');

    const write = await request(app)
      .post(`/v1/moments/${momentId}/future-items`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `pj-fut-${randomUUID()}`)
      .send({ kind: 'MILESTONE', title: 'PJ milestone proof' });
    assert.equal(write.status, 201, JSON.stringify(write.body));
    const itemId = write.body.data.itemId as string;
    assert.ok(itemId);

    const sql = await getPool().query(
      `SELECT 1 FROM personal.future_progress_observation WHERE future_progress_observation_id = $1`,
      [itemId]
    );
    assert.equal(sql.rowCount, 1);

    await assertPulseAndActivity(uid, momentId, 'milestone');

    const axis = await request(app)
      .get(`/v1/personal/moments/${momentId}/future-axis-snapshot`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(axis.status, 200, JSON.stringify(axis.body));
  });

  it('Lifestyle: activity write → SQL → pulse/activity + vitality', async () => {
    const uid = `pj-ls-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@pj.local`);
    const typeCode = await personalType('LIFESTYLE');
    const momentId = await createPersonalMoment(uid, typeCode, 'LIFESTYLE', 'PJ Lifestyle');

    const write = await request(app)
      .post(`/v1/moments/${momentId}/lifestyle-activities`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `pj-ls-${randomUUID()}`)
      .send({ lifestyleContext: 'EXPERIENCE', title: 'PJ sunset walk', wellbeingRating: 8 });
    assert.equal(write.status, 201, JSON.stringify(write.body));
    assert.ok(write.body.data.activityId);

    const sql = await getPool().query(
      `SELECT 1 FROM personal.lifestyle_activity WHERE lifestyle_activity_id = $1`,
      [write.body.data.activityId]
    );
    assert.equal(sql.rowCount, 1);

    await assertPulseAndActivity(uid, momentId, 'sunset');

    const runtime = await request(app)
      .get(`/v1/personal/moments/${momentId}/lifestyle-runtime-summary`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(runtime.status, 200, JSON.stringify(runtime.body));
  });

  it('Relationships: activity write → SQL → pulse/activity + bond', async () => {
    const uid = `pj-rel-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@pj.local`);
    const typeCode = await personalType('RELATIONSHIP_CONNECTION');
    const momentId = await createPersonalMoment(uid, typeCode, 'RELATIONSHIPS', 'PJ Relationships');

    const write = await request(app)
      .post(`/v1/moments/${momentId}/relationship-activities`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `pj-rel-${randomUUID()}`)
      .send({ activityKind: 'SUPPORT', displayName: 'Partner', note: 'PJ check-in' });
    assert.equal(write.status, 201, JSON.stringify(write.body));
    assert.ok(write.body.data.activityId);

    const sql = await getPool().query(
      `SELECT 1 FROM personal.relationship_activity WHERE relationship_activity_id = $1`,
      [write.body.data.activityId]
    );
    assert.equal(sql.rowCount, 1);

    await assertPulseAndActivity(uid, momentId, 'partner');

    const bond = await request(app)
      .get(`/v1/personal/moments/${momentId}/relationships-runtime-summary`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(bond.status, 200, JSON.stringify(bond.body));
  });
});
