/**
 * Outbox status legality + BullMQ enqueue (optional when REDIS_URL set).
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'crypto';
import { config } from '../src/platform/config';
import { withTransaction, getPool, closePool } from '../src/platform/database/pool';
import { firebaseUserId } from '../src/platform/auth/uuid';
import { enqueueOutboxJob } from '../src/platform/queue/outbox-queue';
import { insertDomainEventAndOutbox } from '../src/platform/events/outbox';
import type { RequestContext } from '../src/platform/request-context/context';

process.env.ALLOW_DEV_AUTH = '1';

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

describe('outbox V011 legality', () => {
  it('PENDING → PROCESSING → PUBLISHED with published_at', async () => {
    const ctx = ctxFor(`outbox-${randomUUID().slice(0, 8)}`);
    await getPool().query(
      `INSERT INTO core.user_profile (user_id, email, display_name, status)
       VALUES ($1, $2, 'Outbox', 'ACTIVE') ON CONFLICT (user_id) DO NOTHING`,
      [ctx.userId, `${ctx.userId}@outbox.local`]
    );

    const { outboxEventId, domainEventId } = await withTransaction(async (client) => {
      return insertDomainEventAndOutbox(client, ctx, {
        eventName: 'OutboxLegalityProbe',
        domainCode: 'PLATFORM',
        aggregateType: 'DEVICE',
        aggregateId: randomUUID(),
        payload: { probe: true },
      });
    });

    await withTransaction(async (client) => {
      await client.query(
        `UPDATE events.outbox_event
         SET status = 'PROCESSING', locked_by = 'test', locked_at = now(), attempt_count = 1, updated_at = now()
         WHERE outbox_event_id = $1 AND status = 'PENDING'`,
        [outboxEventId]
      );
    });

    // Enqueue is best-effort (no Redis still OK).
    await enqueueOutboxJob({
      outboxEventId,
      domainEventId,
      topicCode: 'OUTBOXLEGALITYPROBE',
      partitionKey: ctx.userId,
    }).catch(() => false);

    await getPool().query(
      `UPDATE events.outbox_event
       SET status = 'PUBLISHED', published_at = now(), locked_by = NULL, locked_at = NULL, updated_at = now()
       WHERE outbox_event_id = $1`,
      [outboxEventId]
    );

    const row = await getPool().query<{ status: string; published_at: Date | null }>(
      `SELECT status, published_at FROM events.outbox_event WHERE outbox_event_id = $1`,
      [outboxEventId]
    );
    assert.equal(row.rows[0]?.status, 'PUBLISHED');
    assert.ok(row.rows[0]?.published_at);
  });
});

process.on('beforeExit', async () => {
  await closePool();
});
