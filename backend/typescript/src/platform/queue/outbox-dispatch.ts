import type { PoolClient } from 'pg';
import { getPool, withTransaction } from '../database/pool';
import { enqueueOutboxJob, getOutboxQueue } from '../queue/outbox-queue';
import { enqueueAnalyticsJob, getAnalyticsQueue } from '../queue/analytics-queue';
import { redisConfigured } from '../redis/client';

export type OutboxClaimedRow = {
  outbox_event_id: string;
  domain_event_id: string;
  topic_code: string;
  partition_key: string | null;
};

export function failedBackoffSeconds(attemptCount: number): number {
  return Math.min(300, Math.pow(2, Math.max(1, attemptCount)) * 2);
}

export async function reclaimStaleProcessing(
  timeoutSec: number,
  workerLog = true
): Promise<number> {
  const r = await getPool().query<{ n: string }>(
    `WITH reclaimed AS (
       UPDATE events.outbox_event
       SET status = 'PENDING',
           locked_by = NULL,
           locked_at = NULL,
           available_at = now(),
           updated_at = now(),
           attempt_count = GREATEST(attempt_count - 1, 0)
       WHERE status = 'PROCESSING'
         AND locked_at IS NOT NULL
         AND locked_at < now() - make_interval(secs => $1)
       RETURNING outbox_event_id
     )
     SELECT COUNT(*)::text AS n FROM reclaimed`,
    [timeoutSec]
  );
  const n = parseInt(r.rows[0]?.n ?? '0', 10);
  if (workerLog && n > 0) {
    console.log(
      JSON.stringify({
        level: 'info',
        worker: 'outbox-dispatcher',
        msg: 'outbox_reclaim_stale_processing',
        count: n,
        timeoutSec,
      })
    );
  }
  return n;
}

export async function requeueFailedWithBackoff(workerLog = true): Promise<number> {
  const r = await getPool().query<{ n: string }>(
    `WITH promoted AS (
       UPDATE events.outbox_event
       SET status = 'PENDING',
           available_at = now(),
           last_error_code = NULL,
           last_error_message = NULL,
           updated_at = now()
       WHERE status = 'FAILED'
         AND attempt_count < max_attempts
         AND available_at <= now()
       RETURNING outbox_event_id
     ),
     dead AS (
       UPDATE events.outbox_event
       SET status = 'DEAD_LETTER',
           locked_by = NULL,
           locked_at = NULL,
           updated_at = now()
       WHERE status = 'FAILED'
         AND attempt_count >= max_attempts
       RETURNING outbox_event_id
     )
     SELECT (SELECT COUNT(*)::text FROM promoted) AS n`
  );
  const n = parseInt(r.rows[0]?.n ?? '0', 10);
  if (workerLog && n > 0) {
    console.log(
      JSON.stringify({
        level: 'info',
        worker: 'outbox-dispatcher',
        msg: 'outbox_requeue_failed',
        count: n,
      })
    );
  }
  return n;
}

export async function claimOutboxBatch(
  batchSize: number,
  workerId: string
): Promise<OutboxClaimedRow[]> {
  return withTransaction(async (client) => {
    const locked = await client.query<OutboxClaimedRow>(
      `SELECT outbox_event_id, domain_event_id, topic_code, partition_key
       FROM events.outbox_event
       WHERE status = 'PENDING' AND available_at <= now()
       ORDER BY created_at
       LIMIT $1
       FOR UPDATE SKIP LOCKED`,
      [batchSize]
    );

    for (const row of locked.rows) {
      await client.query(
        `UPDATE events.outbox_event
         SET status = 'PROCESSING',
             locked_by = $2,
             locked_at = now(),
             attempt_count = attempt_count + 1,
             updated_at = now()
         WHERE outbox_event_id = $1`,
        [row.outbox_event_id, workerId]
      );
    }
    return locked.rows;
  });
}

export async function markOutboxPublished(outboxEventId: string): Promise<void> {
  await getPool().query(
    `UPDATE events.outbox_event
     SET status = 'PUBLISHED',
         published_at = now(),
         locked_by = NULL,
         locked_at = NULL,
         updated_at = now()
     WHERE outbox_event_id = $1`,
    [outboxEventId]
  );
}

export async function releaseOutboxToPendingNoRedis(outboxEventId: string): Promise<void> {
  await getPool().query(
    `UPDATE events.outbox_event
     SET status = 'PENDING',
         locked_by = NULL,
         locked_at = NULL,
         attempt_count = GREATEST(attempt_count - 1, 0),
         available_at = now() + interval '15 seconds',
         updated_at = now(),
         last_error_code = 'REDIS_UNSET',
         last_error_message = 'REDIS_URL unset — enqueue skipped; left PENDING'
     WHERE outbox_event_id = $1`,
    [outboxEventId]
  );
}

export async function markOutboxFailed(outboxEventId: string, err: unknown): Promise<void> {
  const msg = String(err).slice(0, 500);
  await getPool().query(
    `UPDATE events.outbox_event
     SET status = CASE WHEN attempt_count >= max_attempts THEN 'DEAD_LETTER' ELSE 'FAILED' END,
         last_error_code = 'DISPATCH_FAILED',
         last_error_message = $2,
         locked_by = NULL,
         locked_at = NULL,
         available_at = CASE
           WHEN attempt_count >= max_attempts THEN available_at
           ELSE now() + make_interval(secs => LEAST(300, (POWER(2, GREATEST(attempt_count, 1))::int * 2)))
         END,
         updated_at = now()
     WHERE outbox_event_id = $1`,
    [outboxEventId, msg]
  );
}

function mapAnalyticsContext(domainCode: string | null): 'PERSONAL' | 'GROUP' | 'BUSINESS' | null {
  if (domainCode === 'PERSONAL' || domainCode === 'GROUP' || domainCode === 'BUSINESS') return domainCode;
  return null;
}

export async function maybeEnqueueAnalyticsForOutbox(row: OutboxClaimedRow): Promise<boolean> {
  const ev = await getPool().query<{
    domain_code: string | null;
    scope_type: string | null;
    scope_id: string | null;
    actor_user_id: string | null;
    event_name: string;
    payload: Record<string, unknown> | null;
  }>(
    `SELECT domain_code, scope_type, scope_id, actor_user_id, event_name, payload
     FROM events.domain_event WHERE domain_event_id = $1`,
    [row.domain_event_id]
  );
  if (!ev.rowCount) return false;
  const e = ev.rows[0]!;
  const financeLike =
    /expense|revenue|settlement|invoice|contribution|finance/i.test(e.event_name) ||
    /expense|revenue|settlement|invoice/i.test(row.topic_code);
  if (!financeLike) return false;
  const context = mapAnalyticsContext(e.domain_code);
  if (!context) return false;
  const userId = e.actor_user_id;
  if (!userId) return false;
  const momentId =
    e.scope_type === 'MOMENT' ? e.scope_id : ((e.payload?.momentId as string | undefined) ?? null);
  const companyId =
    e.scope_type === 'COMPANY'
      ? e.scope_id
      : ((e.payload?.companyId as string | undefined) ?? null);
  return enqueueAnalyticsJob({
    outboxEventId: row.outbox_event_id,
    domainEventId: row.domain_event_id,
    topicCode: row.topic_code,
    userId,
    context,
    companyId,
    momentId,
    correlationId: row.outbox_event_id,
  });
}

export type DispatchOneResult = {
  outcome: 'published' | 'pending_no_redis' | 'failed';
  analytics?: boolean;
  analyticsFailed?: boolean;
};

/**
 * Primary enqueue is the delivery boundary.
 * Analytics fan-out failure does not roll back PUBLISHED.
 */
export async function dispatchOneOutboxEvent(
  row: OutboxClaimedRow,
  opts?: { analyticsFailCountRef?: { value: number } }
): Promise<DispatchOneResult> {
  try {
    const enqueued = await enqueueOutboxJob({
      outboxEventId: row.outbox_event_id,
      domainEventId: row.domain_event_id,
      topicCode: row.topic_code,
      partitionKey: row.partition_key,
    });

    if (!enqueued) {
      await releaseOutboxToPendingNoRedis(row.outbox_event_id);
      return { outcome: 'pending_no_redis' };
    }

    let analytics = false;
    let analyticsFailed = false;
    try {
      analytics = await maybeEnqueueAnalyticsForOutbox(row);
    } catch {
      analyticsFailed = true;
      if (opts?.analyticsFailCountRef) {
        opts.analyticsFailCountRef.value += 1;
      }
    }

    await markOutboxPublished(row.outbox_event_id);
    return { outcome: 'published', analytics, analyticsFailed };
  } catch (e) {
    await markOutboxFailed(row.outbox_event_id, e);
    return { outcome: 'failed' };
  }
}

export async function logOutboxStatusCounts(analyticsEnqueueFailCount = 0): Promise<Record<string, number>> {
  const r = await getPool().query<{ status: string; n: string }>(
    `SELECT status, COUNT(*)::text AS n FROM events.outbox_event GROUP BY status`
  );
  const counts: Record<string, number> = {};
  for (const row of r.rows) {
    counts[row.status] = parseInt(row.n, 10);
  }

  let outboxJobCounts: Record<string, number> | null = null;
  let analyticsJobCounts: Record<string, number> | null = null;
  if (redisConfigured()) {
    try {
      const oq = getOutboxQueue();
      const aq = getAnalyticsQueue();
      if (oq) outboxJobCounts = await oq.getJobCounts('waiting', 'active', 'completed', 'failed', 'delayed');
      if (aq) analyticsJobCounts = await aq.getJobCounts('waiting', 'active', 'completed', 'failed', 'delayed');
    } catch {
      // Redis flaky — outbox counts still useful
    }
  }

  console.log(
    JSON.stringify({
      level: 'info',
      worker: 'outbox-dispatcher',
      msg: 'outbox_status_counts',
      counts,
      bullmqOutbox: outboxJobCounts,
      bullmqAnalytics: analyticsJobCounts,
      analyticsEnqueueFailCount,
      redisConfigured: redisConfigured(),
    })
  );
  return counts;
}

/** Test helper: force PROCESSING with old locked_at. */
export async function forceStaleProcessingLock(
  client: PoolClient,
  outboxEventId: string,
  lockedBy: string,
  lockedAtAgeSec: number
): Promise<void> {
  await client.query(
    `UPDATE events.outbox_event
     SET status = 'PROCESSING',
         locked_by = $2,
         locked_at = now() - make_interval(secs => $3),
         attempt_count = GREATEST(attempt_count, 1),
         updated_at = now()
     WHERE outbox_event_id = $1`,
    [outboxEventId, lockedBy, lockedAtAgeSec]
  );
}
