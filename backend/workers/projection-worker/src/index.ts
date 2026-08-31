import dotenv from 'dotenv';
import path from 'path';
import { closePool, withTransaction } from '../../../typescript/src/platform/database/pool';
import { startOutboxWorker } from '../../../typescript/src/platform/queue/outbox-queue';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

/**
 * Bounded projection consumer — records event_consumer_state only.
 * Full projection rebuild algorithms are deferred past S0.
 */
async function markConsumerProcessed(domainEventId: string, outboxEventId: string): Promise<void> {
  await withTransaction(async (client) => {
    await client.query(
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
    console.log(
      JSON.stringify({
        level: 'info',
        worker: 'projection-worker',
        outboxEventId,
        domainEventId,
        msg: 'consumer_ack_bounded',
      })
    );
  });
}

async function run(): Promise<void> {
  console.log(JSON.stringify({ level: 'info', worker: 'projection-worker', msg: 'started' }));

  const worker = startOutboxWorker(async (job) => {
    const started = Date.now();
    await markConsumerProcessed(job.data.domainEventId, job.data.outboxEventId);
    console.log(
      JSON.stringify({
        level: 'info',
        worker: 'projection-worker',
        msg: 'job_done',
        durationMs: Date.now() - started,
        topic: job.data.topicCode,
      })
    );
  });

  if (!worker) {
    console.log(
      JSON.stringify({
        level: 'warn',
        worker: 'projection-worker',
        msg: 'REDIS_URL unset — BullMQ worker idle; outbox still PUBLISHED by dispatcher',
      })
    );
    // Keep process alive for Dokploy; poll no-op.
    while (true) {
      await new Promise((r) => setTimeout(r, 30000));
    }
  }

  worker.on('failed', (job, err) => {
    console.error(
      JSON.stringify({
        level: 'error',
        worker: 'projection-worker',
        jobId: job?.id,
        err: String(err),
      })
    );
  });
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
