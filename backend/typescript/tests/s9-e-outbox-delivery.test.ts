/**
 * S9-E — outbox delivery: no false PUBLISHED, lease reclaim, FAILED backoff,
 * business-invariant duplicate delivery (obligations / activity).
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool, withTransaction } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { insertDomainEventAndOutbox } from '../src/platform/events/outbox';
import type { RequestContext } from '../src/platform/request-context/context';
import { firebaseUserId } from '../src/platform/auth/uuid';
import {
  claimOutboxBatch,
  dispatchOneOutboxEvent,
  failedBackoffSeconds,
  forceStaleProcessingLock,
  reclaimStaleProcessing,
  requeueFailedWithBackoff,
} from '../src/platform/queue/outbox-dispatch';
import { disableRedisForTests, enableRedisForTests, closeRedis } from '../src/platform/redis/client';
import { clearKnownUserProfiles } from '../src/platform/auth';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

function ctxFor(uid: string): RequestContext {
  return Object.freeze({
    firebaseUid: uid,
    firebaseProjectId: projectId,
    userId: firebaseUserId(projectId, uid),
    correlationId: randomUUID(),
    roles: [],
    permissions: [],
  });
}

async function ensureProfile(userId: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, 'S9E', 'ACTIVE') ON CONFLICT (user_id) DO NOTHING`,
    [userId, `${userId}@s9e.local`]
  );
}

describe('S9-E outbox delivery semantics', () => {
  after(async () => {
    enableRedisForTests();
    await closeRedis();
    await closePool();
  });

  it('failedBackoffSeconds is bounded and increasing', () => {
    assert.ok(failedBackoffSeconds(1) >= 2);
    assert.ok(failedBackoffSeconds(5) > failedBackoffSeconds(2));
    assert.equal(failedBackoffSeconds(20), 300);
  });

  it('does not mark PUBLISHED when Redis unset (leaves PENDING)', async () => {
    disableRedisForTests();
    const ctx = ctxFor(`s9e-nr-${randomUUID().slice(0, 6)}`);
    await ensureProfile(ctx.userId);

    const { outboxEventId, domainEventId } = await withTransaction(async (client) => {
      return insertDomainEventAndOutbox(client, ctx, {
        eventName: 'S9ENoRedisProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
    });

    // Claim specifically this row (avoid oldest-first SKIP LOCKED racing other PENDING rows).
    await getPool().query(
      `UPDATE events.outbox_event
       SET status = 'PROCESSING', locked_by = 's9e-test', locked_at = now(),
           attempt_count = attempt_count + 1, updated_at = now()
       WHERE outbox_event_id = $1 AND status = 'PENDING'`,
      [outboxEventId]
    );

    const result = await dispatchOneOutboxEvent({
      outbox_event_id: outboxEventId,
      domain_event_id: domainEventId,
      topic_code: 'S9ENOREDISPROBE',
      partition_key: ctx.userId,
    });
    assert.equal(result.outcome, 'pending_no_redis');

    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM events.outbox_event WHERE outbox_event_id = $1`,
      [outboxEventId]
    );
    assert.equal(st.rows[0]?.status, 'PENDING');
    enableRedisForTests();
  });

  it('reclaims stale PROCESSING after lease timeout', async () => {
    const ctx = ctxFor(`s9e-rc-${randomUUID().slice(0, 6)}`);
    await ensureProfile(ctx.userId);
    const { outboxEventId } = await withTransaction(async (client) => {
      return insertDomainEventAndOutbox(client, ctx, {
        eventName: 'S9EReclaimProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
    });

    await withTransaction(async (client) => {
      await forceStaleProcessingLock(client, outboxEventId, 'dead-worker', 600);
    });

    // Live lease (fresh lock) must NOT be stolen — seed another row with fresh lock
    const { outboxEventId: freshId } = await withTransaction(async (client) => {
      const r = await insertDomainEventAndOutbox(client, ctx, {
        eventName: 'S9EFreshLeaseProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
      await client.query(
        `UPDATE events.outbox_event
         SET status = 'PROCESSING', locked_by = 'live', locked_at = now(),
             attempt_count = 1, updated_at = now()
         WHERE outbox_event_id = $1`,
        [r.outboxEventId]
      );
      return r;
    });

    const n = await reclaimStaleProcessing(300, false);
    assert.ok(n >= 1);

    const stale = await getPool().query<{ status: string }>(
      `SELECT status FROM events.outbox_event WHERE outbox_event_id = $1`,
      [outboxEventId]
    );
    assert.equal(stale.rows[0]?.status, 'PENDING');

    const fresh = await getPool().query<{ status: string }>(
      `SELECT status FROM events.outbox_event WHERE outbox_event_id = $1`,
      [freshId]
    );
    assert.equal(fresh.rows[0]?.status, 'PROCESSING');
  });

  it('FAILED with attempt_count >= max_attempts → DEAD_LETTER on requeue pass', async () => {
    const ctx = ctxFor(`s9e-dl-${randomUUID().slice(0, 6)}`);
    await ensureProfile(ctx.userId);
    const { outboxEventId } = await withTransaction(async (client) => {
      return insertDomainEventAndOutbox(client, ctx, {
        eventName: 'S9EDeadLetterProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
    });

    await getPool().query(
      `UPDATE events.outbox_event
       SET status = 'FAILED',
           attempt_count = max_attempts,
           available_at = now() - interval '1 minute',
           locked_by = NULL,
           locked_at = NULL,
           updated_at = now()
       WHERE outbox_event_id = $1`,
      [outboxEventId]
    );

    await requeueFailedWithBackoff(false);

    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM events.outbox_event WHERE outbox_event_id = $1`,
      [outboxEventId]
    );
    assert.equal(st.rows[0]?.status, 'DEAD_LETTER');
  });

  it('FAILED with attempts remaining and available_at reached → PENDING', async () => {
    const ctx = ctxFor(`s9e-rt-${randomUUID().slice(0, 6)}`);
    await ensureProfile(ctx.userId);
    const { outboxEventId } = await withTransaction(async (client) => {
      return insertDomainEventAndOutbox(client, ctx, {
        eventName: 'S9ERetryProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
    });

    await getPool().query(
      `UPDATE events.outbox_event
       SET status = 'FAILED',
           attempt_count = 1,
           max_attempts = 10,
           available_at = now() - interval '1 second',
           locked_by = NULL,
           locked_at = NULL,
           updated_at = now()
       WHERE outbox_event_id = $1`,
      [outboxEventId]
    );

    await requeueFailedWithBackoff(false);
    const st = await getPool().query<{ status: string }>(
      `SELECT status FROM events.outbox_event WHERE outbox_event_id = $1`,
      [outboxEventId]
    );
    assert.equal(st.rows[0]?.status, 'PENDING');
  });
});

describe('S9-E business invariants — Group expense duplicate delivery', () => {
  after(async () => {
    clearKnownUserProfiles();
    await closePool();
  });

  it('replay of same domain event does not duplicate obligations or activity', async () => {
    clearKnownUserProfiles();
    const uid = `s9e-g-${randomUUID().slice(0, 6)}`;
    const auth = { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };

    const me = await request(app).get('/v1/me').set(auth);
    assert.equal(me.status, 200);
    const userId = me.body.data.userId as string;

    const created = await request(app)
      .post('/v1/moments')
      .set({ ...auth, 'Idempotency-Key': randomUUID() })
      .send({ domainCode: 'GROUP', momentTypeCode: 'TRIP', title: 'S9E invariant' });
    assert.ok(created.status < 300, JSON.stringify(created.body));
    const momentId = created.body.data.momentId as string;

    const parts = await getPool().query<{ participant_id: string }>(
      `SELECT participant_id FROM collaboration.moment_participant
       WHERE moment_id = $1 AND status = 'ACTIVE' ORDER BY joined_at NULLS LAST`,
      [momentId]
    );
    // Seed a second participant for EQUAL split
    const party = await getPool().query<{ external_party_id: string }>(
      `INSERT INTO core.external_party (party_type, display_name, status)
       VALUES ('PERSON', 'S9E P2', 'ACTIVE') RETURNING external_party_id`
    );
    const p2 = await getPool().query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, external_party_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
       RETURNING participant_id`,
      [momentId, party.rows[0]!.external_party_id]
    );
    const paidBy = parts.rows[0]?.participant_id ?? p2.rows[0]!.participant_id;
    const participantIds = [
      ...parts.rows.map((r) => r.participant_id),
      p2.rows[0]!.participant_id,
    ].filter((v, i, a) => a.indexOf(v) === i);

    const exp = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set({ ...auth, 'Idempotency-Key': randomUUID() })
      .send({
        amount: '20.00',
        currencyCode: 'USD',
        description: 'S9E inv',
        paidByParticipantId: paidBy,
        splitStrategy: 'EQUAL',
        splitInputs: participantIds.map((participantId) => ({ participantId })),
      });
    assert.equal(exp.status, 201, JSON.stringify(exp.body));
    const expenseId = exp.body.data.expenseId as string;

    const obl1 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.participant_obligation
       WHERE moment_id = $1 AND source_type = 'EXPENSE_SHARE'`,
      [momentId]
    );
    const act1 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM projection.recent_activity
       WHERE scope_id = $1::uuid AND activity_code = 'GROUP_EXPENSE_RECORDED'`,
      [momentId]
    );
    const share1 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.expense_share WHERE expense_id = $1`,
      [expenseId]
    );

    // Simulate worker re-ACK / duplicate delivery of the domain event (consumer_state upsert).
    const ev = await getPool().query<{ domain_event_id: string }>(
      `SELECT domain_event_id FROM events.domain_event
       WHERE aggregate_id = $1::uuid AND aggregate_type = 'EXPENSE'
       ORDER BY occurred_at DESC LIMIT 1`,
      [expenseId]
    );
    const domainEventId = ev.rows[0]!.domain_event_id;

    for (let i = 0; i < 3; i++) {
      await getPool().query(
        `INSERT INTO events.event_consumer_state (
           consumer_code, domain_event_id, status, started_at, completed_at, attempt_count
         ) VALUES ('PROJECTION_WORKER', $1, 'SUCCEEDED', now(), now(), 1)
         ON CONFLICT (consumer_code, domain_event_id) DO UPDATE SET
           status = 'SUCCEEDED',
           completed_at = now(),
           attempt_count = events.event_consumer_state.attempt_count + 1,
           updated_at = now()`,
        [domainEventId]
      );
      // Re-attempt activity insert (idempotent on user_id + source_event_id)
      await getPool().query(
        `INSERT INTO projection.recent_activity (
           user_id, source_event_id, domain_code, scope_type, scope_id,
           activity_code, title, occurred_at, activity_payload, projection_version
         ) VALUES ($1, $2, 'GROUP', 'MOMENT', $3, 'GROUP_EXPENSE_RECORDED', 'dup', now(), '{}'::jsonb, 1)
         ON CONFLICT (user_id, source_event_id) DO NOTHING`,
        [userId, domainEventId, momentId]
      );
    }

    const obl2 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.participant_obligation
       WHERE moment_id = $1 AND source_type = 'EXPENSE_SHARE'`,
      [momentId]
    );
    const act2 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM projection.recent_activity
       WHERE scope_id = $1::uuid AND activity_code = 'GROUP_EXPENSE_RECORDED'`,
      [momentId]
    );
    const share2 = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.expense_share WHERE expense_id = $1`,
      [expenseId]
    );

    assert.equal(obl2.rows[0]!.n, obl1.rows[0]!.n, 'obligations must not duplicate');
    assert.equal(act2.rows[0]!.n, act1.rows[0]!.n, 'activity must not duplicate');
    assert.equal(share2.rows[0]!.n, share1.rows[0]!.n, 'shares must not duplicate');

    const consumer = await getPool().query<{ attempt_count: number }>(
      `SELECT attempt_count FROM events.event_consumer_state
       WHERE consumer_code = 'PROJECTION_WORKER' AND domain_event_id = $1`,
      [domainEventId]
    );
    assert.ok((consumer.rows[0]?.attempt_count ?? 0) >= 3);
  });
});

describe('S9-E chaos mini-batch (50 events)', () => {
  after(async () => {
    enableRedisForTests();
    await closeRedis();
    await closePool();
  });

  it('inserts 50 outbox rows; with Redis disabled none falsely PUBLISHED', async () => {
    disableRedisForTests();
    const ctx = ctxFor(`s9e-ch-${randomUUID().slice(0, 6)}`);
    await ensureProfile(ctx.userId);

    const ids: string[] = [];
    for (let i = 0; i < 50; i++) {
      const { outboxEventId } = await withTransaction(async (client) => {
        return insertDomainEventAndOutbox(client, ctx, {
          eventName: 'S9EChaosProbe',
          domainCode: 'PLATFORM',
          aggregateType: 'DEVICE',
          aggregateId: randomUUID(),
          payload: { i },
        });
      });
      ids.push(outboxEventId);
    }

    // Dispatch several claims
    for (let round = 0; round < 10; round++) {
      const claimed = await claimOutboxBatch(10, `chaos-${round}`);
      for (const row of claimed) {
        if (ids.includes(row.outbox_event_id)) {
          await dispatchOneOutboxEvent(row);
        }
      }
    }

    const pub = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.outbox_event
       WHERE outbox_event_id = ANY($1::uuid[]) AND status = 'PUBLISHED'`,
      [ids]
    );
    assert.equal(pub.rows[0]!.n, '0', 'must not falsely PUBLISH without Redis');

    const pending = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.outbox_event
       WHERE outbox_event_id = ANY($1::uuid[]) AND status IN ('PENDING','PROCESSING')`,
      [ids]
    );
    assert.ok(parseInt(pending.rows[0]!.n, 10) >= 40);

    enableRedisForTests();
  });
});
