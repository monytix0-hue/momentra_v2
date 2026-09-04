import { Queue, Worker, type Job, type ConnectionOptions } from 'bullmq';

export const NOTIFICATION_QUEUE_NAME = 'momentra-notifications';

export interface NotificationJobPayload {
  outboxEventId: string;
  domainEventId: string;
  topicCode: string;
  eventName: string;
}

function redisConnection(): ConnectionOptions | null {
  const url = process.env.REDIS_URL?.trim();
  if (!url) return null;
  return { url };
}

let notificationQueue: Queue<NotificationJobPayload> | null = null;

export function getNotificationQueue(): Queue<NotificationJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  if (!notificationQueue) {
    notificationQueue = new Queue<NotificationJobPayload>(NOTIFICATION_QUEUE_NAME, {
      connection: conn,
    });
  }
  return notificationQueue;
}

export async function enqueueNotificationJob(payload: NotificationJobPayload): Promise<boolean> {
  const queue = getNotificationQueue();
  if (!queue) return false;
  await queue.add(payload.eventName, payload, {
    jobId: `notif:${payload.domainEventId}`,
    removeOnComplete: 1000,
    removeOnFail: 5000,
    attempts: 3,
    backoff: { type: 'exponential', delay: 3000 },
  });
  return true;
}

export function startNotificationWorker(
  processor: (job: Job<NotificationJobPayload>) => Promise<void>
): Worker<NotificationJobPayload> | null {
  const conn = redisConnection();
  if (!conn) return null;
  return new Worker<NotificationJobPayload>(NOTIFICATION_QUEUE_NAME, processor, {
    connection: conn,
    concurrency: 3,
  });
}

export async function closeNotificationQueue(): Promise<void> {
  if (notificationQueue) {
    await notificationQueue.close();
    notificationQueue = null;
  }
}
