import { Queue, Worker, type Job, type ConnectionOptions } from 'bullmq';

export const ANALYTICS_QUEUE_NAME = 'momentra-analytics';

export interface AnalyticsJobPayload {
  outboxEventId: string;
  domainEventId: string;
  topicCode: string;
  userId: string;
  context: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  companyId?: string | null;
  momentId?: string | null;
  correlationId?: string | null;
}

function redisConnection(): ConnectionOptions | null {
  const url = process.env.REDIS_URL?.trim();
  if (!url) return null;
  return { url };
}

let analyticsQueue: Queue<AnalyticsJobPayload> | null = null;

export function getAnalyticsQueue(): Queue<AnalyticsJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  if (!analyticsQueue) {
    analyticsQueue = new Queue<AnalyticsJobPayload>(ANALYTICS_QUEUE_NAME, { connection: conn });
  }
  return analyticsQueue;
}

export async function enqueueAnalyticsJob(payload: AnalyticsJobPayload): Promise<boolean> {
  const queue = getAnalyticsQueue();
  if (!queue) return false;
  // Idempotent job id per outbox + consumer
  await queue.add(payload.topicCode, payload, {
    jobId: `analytics:${payload.outboxEventId}`,
    removeOnComplete: 1000,
    removeOnFail: 5000,
    attempts: 3,
    backoff: { type: 'exponential', delay: 3000 },
  });
  return true;
}

export function startAnalyticsWorker(
  processor: (job: Job<AnalyticsJobPayload>) => Promise<void>,
): Worker<AnalyticsJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  return new Worker<AnalyticsJobPayload>(ANALYTICS_QUEUE_NAME, processor, {
    connection: conn,
    concurrency: 3,
  });
}

export async function closeAnalyticsQueue(): Promise<void> {
  if (analyticsQueue) {
    await analyticsQueue.close();
    analyticsQueue = null;
  }
}
