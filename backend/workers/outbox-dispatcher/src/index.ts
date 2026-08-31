import dotenv from 'dotenv';
import path from 'path';
import { randomUUID } from 'crypto';
import { closePool } from '../../../typescript/src/platform/database/pool';
import {
  claimOutboxBatch,
  dispatchOneOutboxEvent,
  logOutboxStatusCounts,
  reclaimStaleProcessing,
  requeueFailedWithBackoff,
} from '../../../typescript/src/platform/queue/outbox-dispatch';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const POLL_MS = parseInt(process.env.OUTBOX_POLL_MS ?? '2000', 10);
const BATCH_SIZE = parseInt(process.env.OUTBOX_BATCH_SIZE ?? '10', 10);
const LOCK_TIMEOUT_SEC = parseInt(process.env.OUTBOX_LOCK_TIMEOUT_SEC ?? '300', 10);
const WORKER_ID = process.env.OUTBOX_WORKER_ID ?? `outbox-${randomUUID().slice(0, 8)}`;

const analyticsFailRef = { value: 0 };
let loggedNoRedisSkip = false;

async function dispatchBatch(): Promise<number> {
  const rows = await claimOutboxBatch(BATCH_SIZE, WORKER_ID);
  let published = 0;
  for (const row of rows) {
    const result = await dispatchOneOutboxEvent(row, { analyticsFailCountRef: analyticsFailRef });
    if (result.outcome === 'published') {
      published += 1;
      if (result.analyticsFailed) {
        console.log(
          JSON.stringify({
            level: 'warn',
            worker: 'outbox-dispatcher',
            msg: 'analytics_enqueue_failed',
            eventId: row.outbox_event_id,
            analyticsEnqueueFailCount: analyticsFailRef.value,
          })
        );
      }
      console.log(
        JSON.stringify({
          level: 'info',
          worker: 'outbox-dispatcher',
          eventId: row.outbox_event_id,
          topic: row.topic_code,
          bullmq: true,
          analytics: result.analytics ?? false,
        })
      );
    } else if (result.outcome === 'pending_no_redis') {
      if (!loggedNoRedisSkip) {
        loggedNoRedisSkip = true;
        console.log(
          JSON.stringify({
            level: 'warn',
            worker: 'outbox-dispatcher',
            msg: 'outbox_enqueue_skipped_no_redis',
            hint: 'Left PENDING; will retry when REDIS_URL is available',
          })
        );
      }
    } else {
      console.error(
        JSON.stringify({
          level: 'error',
          worker: 'outbox-dispatcher',
          eventId: row.outbox_event_id,
          msg: 'dispatch_failed',
        })
      );
    }
  }
  return published;
}

async function run(): Promise<void> {
  console.log(
    JSON.stringify({
      level: 'info',
      worker: 'outbox-dispatcher',
      msg: 'started',
      workerId: WORKER_ID,
      lockTimeoutSec: LOCK_TIMEOUT_SEC,
    })
  );
  let ticks = 0;
  while (true) {
    try {
      await reclaimStaleProcessing(LOCK_TIMEOUT_SEC);
      await requeueFailedWithBackoff();
      const count = await dispatchBatch();
      ticks += 1;
      if (ticks % 30 === 0) {
        await logOutboxStatusCounts(analyticsFailRef.value);
      }
      if (count === 0) {
        await new Promise((r) => setTimeout(r, POLL_MS));
      }
    } catch (e) {
      console.error(JSON.stringify({ level: 'error', worker: 'outbox-dispatcher', err: String(e) }));
      await new Promise((r) => setTimeout(r, POLL_MS));
    }
  }
}

run().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});

process.on('SIGTERM', async () => {
  await closePool();
  process.exit(0);
});

process.on('SIGINT', async () => {
  await closePool();
  process.exit(0);
});
