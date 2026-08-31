/**
 * S2 Personal vertical slice — life/memory mount, relationship write, isolation basics.
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
    [userId, email, 'S2 Test']
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

describe('S2 Personal slice', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('GET /personal/life is live with REAL honest empties', async () => {
    const uid = `s2-life-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s2.local`);

    const res = await request(app).get('/v1/personal/life').set('X-Dev-Firebase-Uid', uid);
    assert.equal(res.status, 200);
    assert.equal(res.body.data.dataQuality, 'REAL');
    assert.equal(res.body.data.sectionQuality?.score, 'EMPTY_SUPPORTED');
    assert.equal(res.body.data.sectionQuality?.activeAreaCount, 'REAL_DATA');
    assert.equal(res.body.data.score, 0);
    assert.deepEqual(res.body.data.areaScores, []);
  });

  it('GET /personal/memory is live empty (honest)', async () => {
    const uid = `s2-mem-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s2.local`);

    const res = await request(app).get('/v1/personal/memory').set('X-Dev-Firebase-Uid', uid);
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'EMPTY');
    assert.deepEqual(res.body.data.items, []);
    assert.equal(res.body.data.memoryCount ?? 0, 0);
  });

  it('GET /personal/attention is live empty (honest)', async () => {
    const uid = `s2-att-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s2.local`);

    const res = await request(app).get('/v1/personal/attention').set('X-Dev-Firebase-Uid', uid);
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'EMPTY');
    assert.deepEqual(res.body.data.items, []);
  });

  it('relationship-activities write → activity + pulse bump (no fake bond scores)', async () => {
    const uid = `s2-rel-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s2.local`);
    const typeCode = await personalType('RELATIONSHIP_CONNECTION');

    const createRes = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s2-rel-create-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'S2 relationships',
        personalSetup: { systemCode: 'RELATIONSHIPS', preferences: {} },
      });
    assert.ok([200, 201].includes(createRes.status), JSON.stringify(createRes.body));
    const momentId = createRes.body.data.momentId as string;

    const writeRes = await request(app)
      .post(`/v1/moments/${momentId}/relationship-activities`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s2-rel-act-${randomUUID()}`)
      .send({
        activityKind: 'SUPPORT',
        displayName: 'Partner',
        note: 'Checked in after work',
      });
    assert.equal(writeRes.status, 201, JSON.stringify(writeRes.body));
    assert.ok(writeRes.body.data.activityId);
    assert.ok(
      (writeRes.body.projectionHints ?? []).some((h: { code?: string }) =>
        String(h.code ?? h).toLowerCase().includes('activity')
      ) || writeRes.body.projectionHints
    );

    const activityRes = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId, limit: 10 })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(activityRes.status, 200);
    assert.ok(
      (activityRes.body.data.items as { title: string; activityCode: string }[]).some(
        (i) =>
          i.activityCode.includes('RELATIONSHIP') ||
          i.title.toLowerCase().includes('checked in') ||
          i.title.toLowerCase().includes('partner')
      ),
      JSON.stringify(activityRes.body.data.items)
    );

    const pulseRes = await request(app)
      .get('/v1/personal/pulse')
      .query({ momentId })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulseRes.status, 200);
    assert.ok(pulseRes.body.data.projectionVersion >= 1);
    // Must not invent bond axis fields
    assert.equal(pulseRes.body.data.trustScore, undefined);
    assert.equal(pulseRes.body.data.careScore, undefined);
  });

  it('multi-Moment activity stays scoped by momentId', async () => {
    const uid = `s2-iso-${randomUUID().slice(0, 8)}`;
    const userId = userIdFor(uid);
    await ensureUser(userId, `${uid}@s2.local`);
    const lifeType = await personalType('LIFE_RHYTHM');
    const futureType = await personalType('FUTURE_GOAL');

    const a = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s2-iso-a-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: lifeType,
        title: 'Moment A Life',
        personalSetup: { systemCode: 'LIFE_OPERATIONS', preferences: {} },
      });
    const b = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s2-iso-b-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: futureType,
        title: 'Moment B Future',
        personalSetup: { systemCode: 'FUTURE_BUILDING', preferences: {} },
      });
    assert.ok([200, 201].includes(a.status));
    assert.ok([200, 201].includes(b.status));
    const momentA = a.body.data.momentId as string;
    const momentB = b.body.data.momentId as string;

    await request(app)
      .post(`/v1/moments/${momentA}/expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s2-iso-exp-${randomUUID()}`)
      .send({ amount: '12.50', currencyCode: 'USD', description: 'A only expense' });

    const actA = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: momentA, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    const actB = await request(app)
      .get('/v1/personal/activity')
      .query({ momentId: momentB, limit: 20 })
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(actA.status, 200);
    assert.equal(actB.status, 200);
    const titlesA = (actA.body.data.items as { title: string }[]).map((i) => i.title);
    const titlesB = (actB.body.data.items as { title: string }[]).map((i) => i.title);
    assert.ok(titlesA.some((t) => t.toLowerCase().includes('a only') || t.toLowerCase().includes('expense')));
    assert.ok(!titlesB.some((t) => t.toLowerCase().includes('a only')));
  });
});
