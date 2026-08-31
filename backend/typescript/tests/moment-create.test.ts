/**
 * Phase 6 moment create tests — idempotency, setup preferences, governance, audit/event/outbox.
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
    [userId, email, 'Phase6 Test']
  );
}

async function firstPersonalTypeCode(): Promise<string> {
  const types = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
  );
  assert.ok(types.rows[0], 'Need at least one PERSONAL moment type');
  return types.rows[0].code;
}

describe('Phase 6 moment create', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('creates PERSONAL moment with validated personalSetup preferences', async () => {
    const uid = `p6-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase6.local`);
    const typeCode = await firstPersonalTypeCode();
    const idem = `p6-life-${randomUUID()}`;

    const res = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'My life operations rhythm',
        personalSetup: {
          systemCode: 'LIFE_OPERATIONS',
          preferences: {
            lifeFocus: 'Daily balance',
            primaryNeed: 'More breathing room',
          },
        },
      });

    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.ok(res.body.data.momentId);
    assert.ok(res.body.data.setupId);
    assert.equal(res.body.resourceVersion, 1);
    assert.ok(res.body.projectionHints?.some((h: { projection: string }) => h.projection === 'personal.moments'));

    const setup = await getPool().query<{ preferences: Record<string, unknown> }>(
      `SELECT preferences FROM personal.life_system_setup WHERE life_system_setup_id = $1`,
      [res.body.data.setupId]
    );
    assert.equal(setup.rows[0]?.preferences.lifeFocus, 'Daily balance');
    assert.equal(setup.rows[0]?.preferences.primaryNeed, 'More breathing room');
  });

  it('rejects unknown personalSetup preference keys', async () => {
    const uid = `p6-bad-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase6.local`);
    const typeCode = await firstPersonalTypeCode();

    const res = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p6-bad-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'Bad prefs',
        personalSetup: {
          systemCode: 'LIFE_OPERATIONS',
          preferences: { junkField: 'nope' },
        },
      });

    assert.equal(res.status, 400);
    assert.match(res.body.message ?? '', /Unknown preference key/i);
  });

  it('idempotency replay returns same moment', async () => {
    const uid = `p6-idem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase6.local`);
    const typeCode = await firstPersonalTypeCode();
    const idem = `p6-idem-${randomUUID()}`;
    const body = {
      domainCode: 'PERSONAL',
      momentTypeCode: typeCode,
      title: 'Idempotent Moment',
    };

    const first = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    assert.equal(first.status, 201, JSON.stringify(first.body));

    const second = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', idem)
      .send(body);
    assert.equal(second.status, 201);
    assert.equal(second.body.data.momentId, first.body.data.momentId);
  });

  it('writes audit and domain event on create', async () => {
    const uid = `p6-audit-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@phase6.local`);
    const typeCode = await firstPersonalTypeCode();

    const res = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `p6-audit-${randomUUID()}`)
      .send({
        domainCode: 'PERSONAL',
        momentTypeCode: typeCode,
        title: 'Audit Moment',
      });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    const momentId = res.body.data.momentId;

    const audits = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM audit.audit_record
       WHERE resource_type = 'MOMENT' AND resource_id = $1 AND action_code = 'MOMENT_CREATE'`,
      [momentId]
    );
    assert.equal(audits.rows[0]?.n, '1');

    const events = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.domain_event
       WHERE aggregate_id = $1 AND event_name = 'MomentCreated'`,
      [momentId]
    );
    assert.equal(events.rows[0]?.n, '1');
  });

  it('unauthenticated create returns 401', async () => {
    const prev = process.env.ALLOW_DEV_AUTH;
    process.env.ALLOW_DEV_AUTH = '0';
    try {
      const res = await request(app)
        .post('/v1/moments')
        .set('Idempotency-Key', `p6-401-${randomUUID()}`)
        .send({ domainCode: 'PERSONAL', momentTypeCode: 'LIFE_RHYTHM', title: 'No auth' });
      assert.equal(res.status, 401);
    } finally {
      process.env.ALLOW_DEV_AUTH = prev ?? '1';
    }
  });
});
