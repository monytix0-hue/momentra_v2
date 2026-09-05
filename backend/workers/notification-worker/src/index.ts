/** Notification Worker — FCM push via BullMQ + slow Postgres backfill. */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getMessaging, type Messaging } from 'firebase-admin/messaging';
import {
  PEER_PUSH_EVENT_NAMES,
  alreadySucceeded,
  bumpDeliveryStats,
  categoryEnabled,
  claimRecipient,
  deepLinkForEvent,
  enrichPayload,
  insertInboxRow,
  isPeerPushEvent,
  loadDomainEvent,
  markSent,
  markSucceeded,
  notificationCategory,
  notificationCopy,
  notificationPriority,
  releaseClaim,
  resolveActorDisplayName,
  resolveRecipients,
  shouldDigest,
  shouldSkipPushForPayload,
  type DomainEventRow,
} from '../../typescript/src/platform/notifications/dispatch';
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

function isInvalidTokenError(e: unknown): boolean {
  const code =
    e && typeof e === 'object' && 'code' in e ? String((e as { code?: string }).code) : '';
  const msg = String(e);
  return (
    code.includes('registration-token-not-registered') ||
    code.includes('invalid-registration-token') ||
    /not-registered|invalid-registration-token|registration-token-not-registered/i.test(msg)
  );
}

async function revokeToken(pool: Pool, pushToken: string): Promise<void> {
  await pool.query(
    `UPDATE platform.user_device
     SET revoked_at = now(), updated_at = now()
     WHERE push_token = $1 AND revoked_at IS NULL`,
    [pushToken]
  );
}

async function sendPushToUser(
  messaging: Messaging,
  pool: Pool,
  userId: string,
  title: string,
  body: string,
  data: Record<string, string>
): Promise<{ sent: number; revoked: number }> {
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
  let revoked = 0;
  for (const row of devices.rows) {
    try {
      await messaging.send({
        token: row.push_token,
        notification: { title, body },
        data: { ...data, platform: row.platform },
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
      if (isInvalidTokenError(e)) {
        await revokeToken(pool, row.push_token);
        revoked += 1;
        console.error(
          JSON.stringify({
            worker: 'notification-worker',
            action: 'token_revoked',
            userId,
            error: String(e),
          })
        );
      } else {
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
  }
  return { sent, revoked };
}

async function processDomainEvent(
  pool: Pool,
  messaging: Messaging | null,
  ev: DomainEventRow
): Promise<{
  recipientCount: number;
  sentCount: number;
  inboxCount: number;
  digestCount: number;
  revokedTokens: number;
  skipped: string | null;
}> {
  if (!isPeerPushEvent(ev.event_name)) {
    return {
      recipientCount: 0,
      sentCount: 0,
      inboxCount: 0,
      digestCount: 0,
      revokedTokens: 0,
      skipped: 'not_allowlisted',
    };
  }
  if (shouldSkipPushForPayload(ev.event_name, ev.payload)) {
    await markSucceeded(pool, ev.domain_event_id);
    return {
      recipientCount: 0,
      sentCount: 0,
      inboxCount: 0,
      digestCount: 0,
      revokedTokens: 0,
      skipped: 'notify_members_false',
    };
  }
  if (await alreadySucceeded(pool, ev.domain_event_id)) {
    return {
      recipientCount: 0,
      sentCount: 0,
      inboxCount: 0,
      digestCount: 0,
      revokedTokens: 0,
      skipped: 'already_succeeded',
    };
  }

  const actorName = await resolveActorDisplayName(pool, ev.actor_user_id);
  const payload = enrichPayload(ev, actorName);
  const category = notificationCategory(ev.event_name);
  const priority = notificationPriority(ev.event_name);
  const copy = notificationCopy(ev.event_name, payload);
  const deepLink = deepLinkForEvent(ev.event_name, payload);
  const momentId =
    (typeof payload.momentId === 'string' ? payload.momentId : null) ?? ev.scope_id;

  const recipients = await resolveRecipients(pool, { ...ev, payload });
  let sentCount = 0;
  let inboxCount = 0;
  let digestCount = 0;
  let revokedTokens = 0;

  for (const prefs of recipients) {
    if (!categoryEnabled(prefs.notification_categories, category)) {
      continue;
    }
    const claimed = await claimRecipient(
      pool,
      ev.domain_event_id,
      prefs.user_id,
      ev.event_name,
      category,
      priority
    );
    if (!claimed) continue;

    try {
      const digest = shouldDigest(prefs, priority);
      await insertInboxRow(pool, {
        userId: prefs.user_id,
        domainEventId: ev.domain_event_id,
        eventName: ev.event_name,
        category,
        priority,
        title: copy.title,
        body: copy.body,
        momentId,
        deepLink,
        actorUserId: ev.actor_user_id,
        actorDisplayName: actorName,
        digestPending: digest,
        pushedAt: digest ? null : new Date(),
      });
      inboxCount += 1;

      let n = 0;
      if (digest) {
        digestCount += 1;
        await markSent(pool, ev.domain_event_id, prefs.user_id, 0, 'digest_batched');
      } else if (messaging) {
        const data: Record<string, string> = {
          eventName: ev.event_name,
          category,
          priority,
          domainEventId: ev.domain_event_id,
        };
        if (momentId) data.momentId = momentId;
        if (deepLink) data.deepLink = deepLink;
        if (actorName) data.actorDisplayName = actorName;
        const result = await sendPushToUser(
          messaging,
          pool,
          prefs.user_id,
          copy.title,
          copy.body,
          data
        );
        n = result.sent;
        revokedTokens += result.revoked;
        await markSent(
          pool,
          ev.domain_event_id,
          prefs.user_id,
          n,
          n === 0 ? 'no_devices_or_all_failed' : null
        );
      } else {
        await markSent(pool, ev.domain_event_id, prefs.user_id, 0, 'fcm_unconfigured');
      }
      sentCount += n;
    } catch (e) {
      await releaseClaim(pool, ev.domain_event_id, prefs.user_id);
      throw e;
    }
  }

  await bumpDeliveryStats(pool, ev.event_name, {
    attempted: recipients.length,
    sent: sentCount,
    failed: Math.max(0, recipients.length - sentCount - digestCount),
    revokedToken: revokedTokens,
    digestBatched: digestCount,
    inbox: inboxCount,
  });

  await markSucceeded(pool, ev.domain_event_id);
  return {
    recipientCount: recipients.length,
    sentCount,
    inboxCount,
    digestCount,
    revokedTokens,
    skipped: null,
  };
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
      priority: payload.priority ?? notificationPriority(ev.event_name),
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
