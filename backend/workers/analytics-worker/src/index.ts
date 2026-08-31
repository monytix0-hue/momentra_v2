/** Analytics Worker — S8 DET metrics + optional FastAPI narrative. */
import dotenv from 'dotenv';
import path from 'path';
import { closePool, withTransaction } from '../../../typescript/src/platform/database/pool';
import { startAnalyticsWorker, type AnalyticsJobPayload } from '../../../typescript/src/platform/queue/analytics-queue';
import { runAnalyticsJob } from '../../../typescript/src/modules/analytics/engine';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

async function processJob(job: AnalyticsJobPayload): Promise<void> {
  await withTransaction(async (client) => {
    const result = await runAnalyticsJob(client, {
      userId: job.userId,
      context: job.context,
      companyId: job.companyId,
      momentId: job.momentId,
      triggerEventId: job.domainEventId,
      correlationId: job.correlationId ?? job.outboxEventId,
    });
    await client.query(
      `INSERT INTO events.event_consumer_state (
         consumer_code, domain_event_id, status, started_at, completed_at, attempt_count
       ) VALUES ('ANALYTICS_WORKER', $1, 'SUCCEEDED', now(), now(), 1)
       ON CONFLICT (consumer_code, domain_event_id) DO UPDATE SET
         status = 'SUCCEEDED',
         completed_at = now(),
         attempt_count = events.event_consumer_state.attempt_count + 1,
         updated_at = now()`,
      [job.domainEventId],
    ).catch(() => undefined);
    console.log(
      JSON.stringify({
        level: 'info',
        worker: 'analytics-worker',
        msg: 'job_done',
        topic: job.topicCode,
        metricsWritten: result.metricsWritten,
        narrative: result.narrative,
        skippedReason: result.skippedReason,
      }),
    );
  });
}

async function run(): Promise<void> {
  console.log(JSON.stringify({ level: 'info', worker: 'analytics-worker', msg: 'started' }));
  const worker = startAnalyticsWorker(async (bullJob) => {
    await processJob(bullJob.data);
  });
  if (!worker) {
    console.log(
      JSON.stringify({
        level: 'warn',
        worker: 'analytics-worker',
        msg: 'REDIS_URL unset — idle; use POST /v1/analytics/refresh for sync DET',
      }),
    );
    setInterval(() => undefined, 60_000);
    return;
  }
  const shutdown = async () => {
    await worker.close();
    await closePool();
    process.exit(0);
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

run().catch((e) => {
  console.error(e);
  process.exit(1);
});
