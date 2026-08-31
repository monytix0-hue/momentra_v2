import { Queue, Worker, type Job, type ConnectionOptions } from 'bullmq';

export const OUTBOX_QUEUE_NAME = 'momentra-outbox';

export interface OutboxJobPayload {
  outboxEventId: string;
  domainEventId: string;
  topicCode: string;
  partitionKey: string | null;
}

function redisConnection(): ConnectionOptions | null {
  const url = process.env.REDIS_URL?.trim();
  if (!url) return null;
  return { url };
}

let outboxQueue: Queue<OutboxJobPayload> | null = null;

export function getOutboxQueue(): Queue<OutboxJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  if (!outboxQueue) {
    outboxQueue = new Queue<OutboxJobPayload>(OUTBOX_QUEUE_NAME, { connection: conn });
  }
  return outboxQueue;
}

export async function enqueueOutboxJob(payload: OutboxJobPayload): Promise<boolean> {
  const queue = getOutboxQueue();
  if (!queue) return false;
  const started = Date.now();
  await queue.add(payload.topicCode, payload, {
    jobId: payload.outboxEventId,
    removeOnComplete: 1000,
    removeOnFail: 5000,
    attempts: 5,
    backoff: { type: 'exponential', delay: 2000 },
  });
  console.log(
    JSON.stringify({
      level: 'info',
      msg: 'bullmq_enqueued',
      outboxEventId: payload.outboxEventId,
      topic: payload.topicCode,
      durationMs: Date.now() - started,
    })
  );
  return true;
}

/** Bounded processor — transport proof only; product rebuilds are later phases. */
export function startOutboxWorker(
  processor: (job: Job<OutboxJobPayload>) => Promise<void>
): Worker<OutboxJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  return new Worker<OutboxJobPayload>(OUTBOX_QUEUE_NAME, processor, {
    connection: conn,
    concurrency: 5,
  });
}

export async function closeQueues(): Promise<void> {
  if (outboxQueue) {
    await outboxQueue.close();
    outboxQueue = null;
  }
}
