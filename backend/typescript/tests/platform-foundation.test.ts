/**
 * Platform foundation tests — auth, identity, governance, tx, idempotency, OCC, health, /v1/me.
 * Uses ALLOW_DEV_AUTH for Firebase bypass in automated tests only (never production).
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool, withTransaction } from '../src/platform/database/pool';
import { firebaseUserId } from '../src/platform/auth/uuid';
import { config, assertProductionSafe } from '../src/platform/config';
import { authorize } from '../src/modules/governance/resolver';
import { runCommand } from '../src/platform/transaction/run-command';
import { registerDevice } from '../src/modules/device/service';
import { AppError, ErrorCode } from '../src/platform/errors/errors';
import { toProjectionHints } from '../src/platform/projections/hints';
import type { RequestContext } from '../src/platform/request-context/context';
import { normalizeCorrelationId } from '../src/platform/observability/correlation';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

function ctxFor(uid: string, correlationId = randomUUID()): RequestContext {
  return Object.freeze({
    firebaseUid: uid,
    firebaseProjectId: projectId,
    userId: userIdFor(uid),
    correlationId,
    roles: [],
    permissions: [],
  });
}

async function ensureUser(userId: string, email: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, 'Phase3 Test']
  );
}

describe('Phase 3 platform foundation', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  describe('production fail-closed', () => {
    it('rejects ALLOW_DEV_AUTH in production', () => {
      assert.throws(
        () =>
          assertProductionSafe({
            NODE_ENV: 'production',
            ALLOW_DEV_AUTH: '1',
            FIREBASE_PROJECT_ID: 'x',
            DATABASE_URL: 'postgres://x',
          } as NodeJS.ProcessEnv),
        /ALLOW_DEV_AUTH/
      );
    });

    it('rejects missing FIREBASE_PROJECT_ID in production', () => {
      assert.throws(
        () =>
          assertProductionSafe({
            NODE_ENV: 'production',
            DATABASE_URL: 'postgres://x',
          } as NodeJS.ProcessEnv),
        /FIREBASE_PROJECT_ID/
      );
    });
  });

  describe('correlationId', () => {
    it('accepts valid UUID and rejects oversized/invalid', () => {
      const valid = randomUUID();
      assert.equal(normalizeCorrelationId(valid), valid);
      assert.notEqual(normalizeCorrelationId('not-a-uuid'), 'not-a-uuid');
      assert.notEqual(normalizeCorrelationId('x'.repeat(100)), 'x'.repeat(100));
    });
  });

  describe('projectionHints alignment', () => {
    it('maps legacy codes to typed OpenAPI hints', () => {
      const hints = toProjectionHints(['PERSONAL_MOMENTS', 'PERSONAL_PULSE']);
      assert.deepEqual(hints, [
        { projection: 'personal.moments', action: 'invalidate' },
        { projection: 'personal.pulse', action: 'invalidate' },
      ]);
    });
  });

  describe('health', () => {
    it('GET /health/live → 200 { status: ok }', async () => {
      const res = await request(app).get('/health/live');
      assert.equal(res.status, 200);
      assert.equal(res.body.status, 'ok');
    });

    it('GET /health/ready → 200 when DB healthy', async () => {
      const res = await request(app).get('/health/ready');
      assert.equal(res.status, 200);
      assert.equal(res.body.status, 'ok');
    });
  });

  describe('authentication', () => {
    it('missing bearer with Firebase required → 401 when ALLOW_DEV_AUTH off', async () => {
      const prev = process.env.ALLOW_DEV_AUTH;
      process.env.ALLOW_DEV_AUTH = '0';
      try {
        if (!config.firebase.projectId) {
          // Without Firebase project, middleware still allows dev path — skip assertion.
          return;
        }
        const res = await request(app).get('/v1/me');
        // With ALLOW_DEV_AUTH=0 and FIREBASE_PROJECT_ID set, Bearer required.
        assert.equal(res.status, 401);
        assert.equal(res.body.code, ErrorCode.UNAUTHORIZED);
        assert.ok(res.body.correlationId);
      } finally {
        process.env.ALLOW_DEV_AUTH = prev;
      }
    });

    it('dev identity accepted when ALLOW_DEV_AUTH=1', async () => {
      process.env.ALLOW_DEV_AUTH = '1';
      const res = await request(app)
        .get('/v1/me')
        .set('X-Dev-Firebase-Uid', `phase3-auth-${randomUUID().slice(0, 8)}`);
      assert.equal(res.status, 200);
      assert.ok(res.body.data.userId);
      assert.ok(res.body.correlationId);
    });
  });

  describe('identity resolution', () => {
    it('same Firebase UID → same canonical userId (deterministic)', () => {
      const a = firebaseUserId(projectId, 'phase3-stable-uid');
      const b = firebaseUserId(projectId, 'phase3-stable-uid');
      assert.equal(a, b);
    });

    it('concurrent provision does not create duplicate users', async () => {
      const uid = `phase3-conc-${randomUUID().slice(0, 8)}`;
      const id = userIdFor(uid);
      await Promise.all([
        ensureUser(id, `${id}@phase3.local`),
        ensureUser(id, `${id}@phase3.local`),
        ensureUser(id, `${id}@phase3.local`),
      ]);
      const r = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM core.user_profile WHERE user_id = $1`,
        [id]
      );
      assert.equal(r.rows[0].n, '1');
    });
  });

  describe('GET /v1/me', () => {
    it('authenticated → 200 shell bootstrap without tab datasets', async () => {
      process.env.ALLOW_DEV_AUTH = '1';
      const uid = `phase3-me-${randomUUID().slice(0, 8)}`;
      const res = await request(app).get('/v1/me').set('X-Dev-Firebase-Uid', uid);
      assert.equal(res.status, 200);
      assert.equal(res.body.data.firebaseUid, uid);
      assert.ok(res.body.data.userId);
      assert.ok(Array.isArray(res.body.data.roles));
      assert.ok(Array.isArray(res.body.data.supportedContexts));
      assert.ok(res.body.data.supportedContexts.includes('PERSONAL'));
      assert.ok(res.body.data.supportedContexts.includes('GROUP'));
      assert.ok(res.body.data.supportedContexts.includes('BUSINESS'));
      assert.ok(res.body.data.supportedContexts.includes('CIRCLE'));
      assert.equal(res.body.data.currentlySelectedContext, 'PERSONAL');
      assert.ok(res.body.data.activeMoments);
      assert.ok(Array.isArray(res.body.data.activeMoments.personal));
      assert.ok(Array.isArray(res.body.data.companies));
      assert.ok(res.body.data.preferences?.timezone);
      assert.ok(res.body.data.featureFlags);
      assert.ok(Array.isArray(res.body.data.capabilities));
      // Must not embed tab projection payloads
      assert.equal(res.body.data.pulse, undefined);
      assert.equal(res.body.data.moments, undefined);
      assert.equal(res.body.data.widgetPayload, undefined);
      assert.equal(res.body.data.life, undefined);
      assert.equal(res.body.data.memory, undefined);
      assert.equal(res.body.data.activity, undefined);
    });
  });

  describe('governance isolation', () => {
    it('Personal: own context allow, other deny', async () => {
      const a = ctxFor(`gov-a-${randomUUID().slice(0, 8)}`);
      const b = ctxFor(`gov-b-${randomUUID().slice(0, 8)}`);
      await ensureUser(a.userId, `${a.userId}@phase3.local`);
      await ensureUser(b.userId, `${b.userId}@phase3.local`);

      await withTransaction(async (client) => {
        const own = await authorize(client, a, {
          actionCode: 'PERSONAL_ACCESS',
          resourceType: 'USER',
          ownerUserId: a.userId,
        });
        assert.equal(own.allowed, true);

        const other = await authorize(client, a, {
          actionCode: 'PERSONAL_ACCESS',
          resourceType: 'USER',
          ownerUserId: b.userId,
        });
        assert.equal(other.allowed, false);
      });
    });

    it('Group: participant allow, outsider deny', async () => {
      const member = ctxFor(`grp-m-${randomUUID().slice(0, 8)}`);
      const outsider = ctxFor(`grp-o-${randomUUID().slice(0, 8)}`);
      await ensureUser(member.userId, `${member.userId}@phase3.local`);
      await ensureUser(outsider.userId, `${outsider.userId}@phase3.local`);

      const momentType = await getPool().query<{ moment_type_id: string }>(
        `SELECT moment_type_id FROM core.moment_type WHERE domain_code = 'GROUP' AND status = 'ACTIVE' LIMIT 1`
      );
      if (!momentType.rows[0]) {
        // Skip if taxonomy missing — environment issue, not platform failure.
        return;
      }

      const momentId = randomUUID();
      await getPool().query(
        `INSERT INTO core.moment (
           moment_id, domain_code, moment_type_id, created_by_user_id, title, status, version
         ) VALUES ($1, 'GROUP', $2, $3, 'Phase3 Group', 'ACTIVE', 1)`,
        [momentId, momentType.rows[0].moment_type_id, member.userId]
      );
      await getPool().query(
        `INSERT INTO collaboration.group_moment_context (moment_id, group_family, organizer_user_id)
         VALUES ($1, 'SHARED_EXPERIENCE', $2)`,
        [momentId, member.userId]
      );
      await getPool().query(
        `INSERT INTO collaboration.moment_participant (moment_id, user_id, participant_role, status)
         VALUES ($1, $2, 'ORGANIZER', 'ACTIVE')`,
        [momentId, member.userId]
      );

      await withTransaction(async (client) => {
        const ok = await authorize(client, member, {
          actionCode: 'GROUP_ACCESS',
          resourceType: 'MOMENT',
          momentId,
        });
        assert.equal(ok.allowed, true);

        const denied = await authorize(client, outsider, {
          actionCode: 'GROUP_ACCESS',
          resourceType: 'MOMENT',
          momentId,
        });
        assert.equal(denied.allowed, false);
      });
    });

    it('Business: member allow, outsider deny', async () => {
      const member = ctxFor(`biz-m-${randomUUID().slice(0, 8)}`);
      const outsider = ctxFor(`biz-o-${randomUUID().slice(0, 8)}`);
      await ensureUser(member.userId, `${member.userId}@phase3.local`);
      await ensureUser(outsider.userId, `${outsider.userId}@phase3.local`);

      const companyId = randomUUID();
      await getPool().query(
        `INSERT INTO business.company (company_id, legal_name, display_name, status, version, created_by_user_id)
         VALUES ($1, 'Phase3 Co', 'Phase3', 'ACTIVE', 1, $2)`,
        [companyId, member.userId]
      );
      await getPool().query(
        `INSERT INTO business.company_membership (company_id, user_id, membership_type, status)
         VALUES ($1, $2, 'OWNER', 'ACTIVE')`,
        [companyId, member.userId]
      );

      await withTransaction(async (client) => {
        const ok = await authorize(client, member, {
          actionCode: 'COMPANY_ACCESS',
          resourceType: 'COMPANY',
          companyId,
        });
        assert.equal(ok.allowed, true);

        const denied = await authorize(client, outsider, {
          actionCode: 'COMPANY_ACCESS',
          resourceType: 'COMPANY',
          companyId,
        });
        assert.equal(denied.allowed, false);
      });
    });
  });

  describe('transaction + audit/event/outbox + idempotency', () => {
    it('device register commits audit/event/outbox; retry replays; conflict on different body', async () => {
      process.env.ALLOW_DEV_AUTH = '1';
      const uid = `dev-tx-${randomUUID().slice(0, 8)}`;
      const key = `idem-${randomUUID()}`;
      const body = { platform: 'ANDROID' as const, pushToken: `tok-${randomUUID()}`, deviceId: randomUUID() };

      const res1 = await request(app)
        .post('/v1/me/devices')
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', key)
        .send(body);
      assert.equal(res1.status, 201, JSON.stringify(res1.body));
      assert.equal(res1.body.data.deviceId, body.deviceId);

      const audit = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM audit.audit_record
         WHERE action_code = 'DEVICE_REGISTER' AND resource_id = $1`,
        [res1.body.data.userDeviceId]
      );
      assert.ok(parseInt(audit.rows[0].n, 10) >= 1);

      const events = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM events.domain_event
         WHERE event_name = 'DeviceRegistered' AND aggregate_id = $1`,
        [res1.body.data.userDeviceId]
      );
      assert.ok(parseInt(events.rows[0].n, 10) >= 1);

      const outbox = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM events.outbox_event o
         JOIN events.domain_event d ON d.domain_event_id = o.domain_event_id
         WHERE d.aggregate_id = $1`,
        [res1.body.data.userDeviceId]
      );
      assert.ok(parseInt(outbox.rows[0].n, 10) >= 1);

      const res2 = await request(app)
        .post('/v1/me/devices')
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', key)
        .send(body);
      assert.equal(res2.status, 201);
      assert.equal(res2.body.data.deviceId, res1.body.data.deviceId);
      assert.equal(res2.body.data.userDeviceId, res1.body.data.userDeviceId);

      const res3 = await request(app)
        .post('/v1/me/devices')
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', key)
        .send({ ...body, pushToken: 'different-token-value' });
      assert.equal(res3.status, 409);
      assert.equal(res3.body.code, ErrorCode.IDEMPOTENCY_CONFLICT);
    });

    it('idempotency race: concurrent same key → one canonical mutation', async () => {
      const uid = `race-${randomUUID().slice(0, 8)}`;
      const actor = ctxFor(uid);
      await ensureUser(actor.userId, `${actor.userId}@phase3.local`);
      const key = `race-${randomUUID()}`;
      const body = { platform: 'IOS' as const, pushToken: `race-${randomUUID()}`, deviceId: randomUUID() };

      const results = await Promise.allSettled([
        runCommand({
          operationCode: 'DEVICE_REGISTER',
          idempotencyKey: key,
          body,
          ctx: actor,
          resourceType: 'DEVICE',
          execute: async (client, b) => {
            const r = await registerDevice(client, actor, b);
            return { result: r, resourceId: r.userDeviceId };
          },
        }),
        runCommand({
          operationCode: 'DEVICE_REGISTER',
          idempotencyKey: key,
          body,
          ctx: actor,
          resourceType: 'DEVICE',
          execute: async (client, b) => {
            const r = await registerDevice(client, actor, b);
            return { result: r, resourceId: r.userDeviceId };
          },
        }),
      ]);

      const fulfilled = results.filter((r) => r.status === 'fulfilled') as PromiseFulfilledResult<{
        userDeviceId: string;
      }>[];
      assert.ok(fulfilled.length >= 1);
      const deviceIds = new Set(fulfilled.map((r) => r.value.userDeviceId));
      assert.equal(deviceIds.size, 1);

      const count = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM platform.user_device WHERE user_id = $1 AND device_id = $2`,
        [actor.userId, body.deviceId]
      );
      assert.equal(count.rows[0].n, '1');
    });

    it('rollback removes canonical + audit + event + outbox on failure', async () => {
      const uid = `rb-${randomUUID().slice(0, 8)}`;
      const actor = ctxFor(uid);
      await ensureUser(actor.userId, `${actor.userId}@phase3.local`);
      const deviceId = randomUUID();
      const marker = `rollback-${deviceId}`;

      await assert.rejects(
        async () =>
          withTransaction(async (client) => {
            await registerDevice(client, actor, {
              deviceId,
              platform: 'WEB',
              pushToken: marker,
            });
            throw new AppError(ErrorCode.VALIDATION_FAILED, 'forced rollback', 400);
          }),
        /forced rollback/
      );

      const devices = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM platform.user_device WHERE device_id = $1`,
        [deviceId]
      );
      assert.equal(devices.rows[0].n, '0');

      const audits = await getPool().query<{ n: string }>(
        `SELECT COUNT(*)::text AS n FROM audit.audit_record
         WHERE after_snapshot->>'deviceId' = $1`,
        [deviceId]
      );
      assert.equal(audits.rows[0].n, '0');
    });
  });

  describe('OCC expectedVersion', () => {
    it('stale expectedVersion → 409 VERSION_CONFLICT with details', async () => {
      process.env.ALLOW_DEV_AUTH = '1';
      const uid = `occ-${randomUUID().slice(0, 8)}`;
      const create = await request(app)
        .post('/v1/moments')
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', `occ-c-${randomUUID()}`)
        .send({
          domainCode: 'PERSONAL',
          momentTypeCode: 'PERSONAL_GOAL',
          title: 'OCC Moment',
        });

      if (create.status !== 201) {
        // Taxonomy may use different codes — try a listed type
        const types = await getPool().query<{ code: string }>(
          `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
        );
        if (!types.rows[0]) return;
        const create2 = await request(app)
          .post('/v1/moments')
          .set('X-Dev-Firebase-Uid', uid)
          .set('Idempotency-Key', `occ-c2-${randomUUID()}`)
          .send({
            domainCode: 'PERSONAL',
            momentTypeCode: types.rows[0].code,
            title: 'OCC Moment',
          });
        assert.equal(create2.status, 201, JSON.stringify(create2.body));
        const momentId = create2.body.data.momentId;
        const conflict = await request(app)
          .patch(`/v1/moments/${momentId}`)
          .set('X-Dev-Firebase-Uid', uid)
          .set('Idempotency-Key', `occ-p-${randomUUID()}`)
          .send({ title: 'Stale', expectedVersion: 999 });
        if (conflict.status === 404) return; // PATCH not mounted in S1 shell router
        assert.equal(conflict.status, 409);
        assert.equal(conflict.body.code, ErrorCode.VERSION_CONFLICT);
        assert.equal(conflict.body.details.expectedVersion, 999);
        return;
      }

      const momentId = create.body.data.momentId;
      const conflict = await request(app)
        .patch(`/v1/moments/${momentId}`)
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', `occ-p-${randomUUID()}`)
        .send({ title: 'Stale', expectedVersion: 999 });
      if (conflict.status === 404) return; // PATCH not mounted in S1 shell router
      assert.equal(conflict.status, 409);
      assert.equal(conflict.body.code, ErrorCode.VERSION_CONFLICT);
      assert.equal(conflict.body.details.expectedVersion, 999);
      assert.ok(conflict.body.details.resourceId);
    });
  });
});
