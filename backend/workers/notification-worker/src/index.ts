/** Notification Worker — FCM push via BullMQ + slow Postgres backfill. */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getMessaging, type Messaging } from 'firebase-admin/messaging';
import {
  isPeerPushEvent,
  notificationCopy,
  PEER_PUSH_EVENT_NAMES,
  shouldSkipPushForPayload,
} from '../../typescript/src/platform/notifications/allowlist';
import { startNotificationWorker } from '../../typescript/src/platform/queue/notification-queue';
import type { NotificationJobPayload } from '../../typescript/src/platform/queue/notification-queue';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const BACKFILL_POLL_MS = parseInt(process.env.NOTIFICATION_POLL_MS ?? '60000', 10);
const ANDROID_CHANNEL_ID = 'momentra_updates';
const ALLOWLIST_EVENTS = [...PEER_PUSH_EVENT_NAMES];

function initFirebaseAdmin(): Messaging | null {
  if (getApps().length === 0) {
    const credJson =
      process.env.FIREBASE_CREDENTIALS_JSON ||
      process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    const projectId = process.env.FIREBASE_PROJECT_ID;
    if (credJson) {
      const cred = JSON.parse(credJson) as Record<string, string>;
      initializeApp({
        credential: cert(cred),
        projectId: projectId || cred.project_id,
      });
    } else if (projectId) {
      initializeApp({ projectId });
    } else {
      console.warn(
        JSON.stringify({
          worker: 'notification-worker',
          warning: 'Firebase Admin not configured — push disabled',
        })
      );
      return null;
    }
  }
  try {
    return getMessaging();
  } catch {
    return null;
  }
}

type DomainEventRow = {
  domain_event_id: string;
  actor_user_id: string;
  event_name: string;
  scope_id: string | null;
  payload: Record<string, unknown> | null;
};

async function loadDomainEvent(pool: Pool, domainEventId: string): Promise<DomainEventRow | null> {
  const r = await pool.query<DomainEventRow>(
    `SELECT domain_event_id, actor_user_id, event_name, scope_id, payload
     FROM events.domain_event WHERE domain_event_id = $1`,
    [domainEventId]
  );
  return r.rows[0] ?? null;
}

async function alreadySucceeded(pool: Pool, domainEventId: string): Promise<boolean> {
  const r = await pool.query(
    `SELECT 1 FROM events.event_consumer_state
     WHERE consumer_code = 'NOTIFICATION_WORKER'
       AND domain_event_id = $1
       AND status = 'SUCCEEDED'
     LIMIT 1`,
    [domainEventId]
  );
  return (r.rowCount ?? 0) > 0;
}

async function markSucceeded(pool: Pool, domainEventId: string): Promise<void> {
  await pool.query(
    `INSERT INTO events.event_consumer_state (
       consumer_code, domain_event_id, status, started_at, completed_at, attempt_count
     ) VALUES ('NOTIFICATION_WORKER', $1, 'SUCCEEDED', now(), now(), 1)
     ON CONFLICT (consumer_code, domain_event_id) DO UPDATE SET
       status = 'SUCCEEDED',
       completed_at = now(),
       attempt_count = events.event_consumer_state.attempt_count + 1,
       updated_at = now()`,
    [domainEventId]
  );
}

async function resolvePeerRecipients(
  pool: Pool,
  momentId: string | null | undefined,
  actorUserId: string
): Promise<string[]> {
  if (!momentId) return [];
  const rows = await pool.query<{ user_id: string }>(
    `SELECT mp.user_id
     FROM collaboration.moment_participant mp
     JOIN core.user_profile up ON up.user_id = mp.user_id
     WHERE mp.moment_id = $1
       AND mp.status = 'ACTIVE'
       AND mp.user_id IS NOT NULL
       AND mp.user_id <> $2
       AND mp.notify_on_changes = true
       AND up.push_notifications_enabled = true
       AND up.status = 'ACTIVE'`,
    [momentId, actorUserId]
  );
  return rows.rows.map((r) => r.user_id);
}

async function claimRecipient(
  pool: Pool,
  domainEventId: string,
  userId: string,
  eventName: string
): Promise<boolean> {
  const r = await pool.query(
    `INSERT INTO platform.notification_dispatch (domain_event_id, user_id, event_name, sent_count)
     VALUES ($1, $2, $3, 0)
     ON CONFLICT (domain_event_id, user_id) DO NOTHING
     RETURNING user_id`,
    [domainEventId, userId, eventName]
  );
  return (r.rowCount ?? 0) > 0;
}

async function releaseClaim(pool: Pool, domainEventId: string, userId: string): Promise<void> {
  await pool.query(
    `DELETE FROM platform.notification_dispatch
     WHERE domain_event_id = $1 AND user_id = $2 AND sent_count = 0`,
    [domainEventId, userId]
  );
}

async function markSent(
  pool: Pool,
  domainEventId: string,
  userId: string,
  sentCount: number
): Promise<void> {
  await pool.query(
    `UPDATE platform.notification_dispatch
     SET sent_count = $3, sent_at = now()
     WHERE domain_event_id = $1 AND user_id = $2`,
    [domainEventId, userId, sentCount]
  );
}

async function sendPushToUser(
  messaging: Messaging,
  pool: Pool,
  userId: string,
  title: string,
  body: string
): Promise<number> {
  const devices = await pool.query<{ push_token: string; platform: string }>(
    `SELECT push_token, platform
     FROM platform.user_device
     WHERE user_id = $1
       AND revoked_at IS NULL
       AND push_token IS NOT NULL
       AND push_token <> ''`,
    [userId]
  );
  let sent = 0;
  for (const row of devices.rows) {
    try {
      await messaging.send({
        token: row.push_token,
        notification: { title, body },
        data: { platform: row.platform },
        android: {
          priority: 'high',
          notification: {
            channelId: ANDROID_CHANNEL_ID,
            defaultSound: true,
            sound: 'default',
          },
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
            },
          },
        },
      });
      sent += 1;
    } catch (e) {
      console.error(
        JSON.stringify({
          worker: 'notification-worker',
          action: 'send_failed',
          userId,
          error: String(e),
        })
      );
    }
  }
  return sent;
}

async function processDomainEvent(
  pool: Pool,
  messaging: Messaging | null,
  ev: DomainEventRow
): Promise<{ recipientCount: number; sentCount: number; skipped: string | null }> {
  if (!isPeerPushEvent(ev.event_name)) {
    return { recipientCount: 0, sentCount: 0, skipped: 'not_allowlisted' };
  }
  if (shouldSkipPushForPayload(ev.event_name, ev.payload)) {
    await markSucceeded(pool, ev.domain_event_id);
    return { recipientCount: 0, sentCount: 0, skipped: 'notify_members_false' };
  }
  if (await alreadySucceeded(pool, ev.domain_event_id)) {
    return { recipientCount: 0, sentCount: 0, skipped: 'already_succeeded' };
  }

  const momentId =
    (typeof ev.payload?.momentId === 'string' ? ev.payload.momentId : null) ?? ev.scope_id;
  const recipients = await resolvePeerRecipients(pool, momentId, ev.actor_user_id);
  const copy = notificationCopy(ev.event_name, ev.payload);
  let sentCount = 0;

  for (const userId of recipients) {
    const claimed = await claimRecipient(pool, ev.domain_event_id, userId, ev.event_name);
    if (!claimed) continue;
    try {
      let n = 0;
      if (messaging) {
        n = await sendPushToUser(messaging, pool, userId, copy.title, copy.body);
      }
      await markSent(pool, ev.domain_event_id, userId, n);
      sentCount += n;
    } catch (e) {
      await releaseClaim(pool, ev.domain_event_id, userId);
      throw e;
    }
  }

  await markSucceeded(pool, ev.domain_event_id);
  return { recipientCount: recipients.length, sentCount, skipped: null };
}

async function processJob(
  pool: Pool,
  messaging: Messaging | null,
  payload: NotificationJobPayload
): Promise<void> {
  const ev = await loadDomainEvent(pool, payload.domainEventId);
  if (!ev) {
    console.warn(
      JSON.stringify({
        worker: 'notification-worker',
        action: 'event_missing',
        domainEventId: payload.domainEventId,
      })
    );
    return;
  }
  const result = await processDomainEvent(pool, messaging, ev);
  console.log(
    JSON.stringify({
      worker: 'notification-worker',
      action: messaging ? 'processed' : 'processed_no_fcm',
      eventName: ev.event_name,
      domainEventId: ev.domain_event_id,
      ...result,
    })
  );
}

async function runBackfill(pool: Pool, messaging: Messaging | null): Promise<void> {
  while (true) {
    try {
      const events = await pool.query<DomainEventRow>(
        `SELECT de.domain_event_id, de.actor_user_id, de.event_name, de.scope_id, de.payload
         FROM events.domain_event de
         LEFT JOIN events.event_consumer_state ecs
           ON ecs.domain_event_id = de.domain_event_id
          AND ecs.consumer_code = 'NOTIFICATION_WORKER'
          AND ecs.status = 'SUCCEEDED'
         WHERE ecs.domain_event_id IS NULL
           AND de.recorded_at > now() - interval '15 minutes'
           AND de.event_name = ANY($1::text[])
         ORDER BY de.recorded_at ASC
         LIMIT 20`,
        [ALLOWLIST_EVENTS]
      );

      for (const ev of events.rows) {
        const result = await processDomainEvent(pool, messaging, ev);
        console.log(
          JSON.stringify({
            worker: 'notification-worker',
            action: 'backfill',
            eventName: ev.event_name,
            domainEventId: ev.domain_event_id,
            ...result,
          })
        );
      }
    } catch (e) {
      console.error(JSON.stringify({ worker: 'notification-worker', backfillError: String(e) }));
    }
    await new Promise((r) => setTimeout(r, BACKFILL_POLL_MS));
  }
}

async function loop(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const messaging = initFirebaseAdmin();

  console.log(
    JSON.stringify({
      worker: 'notification-worker',
      status: 'started',
      fcm: messaging != null,
      backfillPollMs: BACKFILL_POLL_MS,
    })
  );

  const worker = startNotificationWorker(async (job) => {
    await processJob(pool, messaging, job.data);
  });

  if (!worker) {
    console.log(
      JSON.stringify({
        level: 'warn',
        worker: 'notification-worker',
        msg: 'REDIS_URL unset — backfill poll only',
      })
    );
  }

  const shutdown = async () => {
    if (worker) await worker.close();
    await pool.end();
    process.exit(0);
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  await runBackfill(pool, messaging);
}

loop().catch((e) => {
  console.error(e);
  process.exit(1);
});
