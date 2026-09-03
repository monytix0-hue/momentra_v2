/** Notification Worker — FCM push from recent domain events. */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getMessaging, type Messaging } from 'firebase-admin/messaging';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const POLL_MS = parseInt(process.env.NOTIFICATION_POLL_MS ?? '10000', 10);

function initFirebaseAdmin(): Messaging | null {
  if (getApps().length === 0) {
    const credJson = process.env.FIREBASE_CREDENTIALS_JSON;
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
      return null;
    }
  }
  try {
    return getMessaging();
  } catch {
    return null;
  }
}

async function ensureDispatchTable(pool: Pool): Promise<void> {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS platform.notification_dispatch (
      domain_event_id uuid PRIMARY KEY,
      user_id uuid NOT NULL,
      event_name text NOT NULL,
      sent_count int NOT NULL DEFAULT 0,
      sent_at timestamptz NOT NULL DEFAULT now()
    )
  `);
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

function notificationCopy(eventName: string, payload?: Record<string, unknown>): { title: string; body: string } {
  switch (eventName) {
    case 'ExpenseCreated':
    case 'GroupExpenseRecorded':
      return { title: 'New expense', body: 'An expense was recorded in your moment.' };
    case 'PollCreated':
      return { title: 'New poll', body: 'A poll needs your vote.' };
    case 'TaskCreated':
      return { title: 'New task', body: 'A task was added to your moment.' };
    case 'DeviceRegistered':
      return { title: 'Device linked', body: 'Push notifications are enabled for this device.' };
    case 'GroupInviteMinted':
      return { title: 'Invite ready', body: 'A group invite link was created.' };
    case 'GroupInviteRedeemed':
      return {
        title: 'Someone joined',
        body: 'A member joined your group moment.',
      };
    default:
      return { title: 'Momentra update', body: `Activity: ${eventName}` };
  }
}

async function momentParticipantUserIds(
  pool: Pool,
  momentId: string | null | undefined,
  excludeUserId?: string
): Promise<string[]> {
  if (!momentId) return [];
  const rows = await pool.query<{ user_id: string }>(
    `SELECT user_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE' AND user_id IS NOT NULL`,
    [momentId]
  );
  return rows.rows.map((r) => r.user_id).filter((id) => id !== excludeUserId);
}

async function loop(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const messaging = initFirebaseAdmin();
  await ensureDispatchTable(pool);
  console.log(
    JSON.stringify({
      worker: 'notification-worker',
      status: 'started',
      fcm: messaging != null,
    })
  );

  while (true) {
    try {
      const events = await pool.query<{
        domain_event_id: string;
        actor_user_id: string;
        event_name: string;
        scope_id: string | null;
        payload: Record<string, unknown> | null;
      }>(
        `SELECT de.domain_event_id, de.actor_user_id, de.event_name, de.scope_id, de.payload
         FROM events.domain_event de
         LEFT JOIN platform.notification_dispatch nd ON nd.domain_event_id = de.domain_event_id
         WHERE nd.domain_event_id IS NULL
           AND de.recorded_at > now() - interval '15 minutes'
         ORDER BY de.recorded_at ASC
         LIMIT 20`
      );

      for (const ev of events.rows) {
        const copy = notificationCopy(ev.event_name, ev.payload ?? undefined);
        const momentId =
          (typeof ev.payload?.momentId === 'string' ? ev.payload.momentId : null) ??
          ev.scope_id;
        let recipients: string[] = [ev.actor_user_id];
        if (ev.event_name === 'GroupInviteRedeemed' || ev.event_name === 'GroupInviteMinted') {
          // Notify other members / host — not only the actor.
          recipients = await momentParticipantUserIds(pool, momentId, undefined);
          if (recipients.length === 0) recipients = [ev.actor_user_id];
        }
        let sentCount = 0;
        if (messaging) {
          for (const userId of recipients) {
            sentCount += await sendPushToUser(messaging, pool, userId, copy.title, copy.body);
          }
        }
        await pool.query(
          `INSERT INTO platform.notification_dispatch (domain_event_id, user_id, event_name, sent_count)
           VALUES ($1, $2, $3, $4)
           ON CONFLICT (domain_event_id) DO NOTHING`,
          [ev.domain_event_id, ev.actor_user_id, ev.event_name, sentCount]
        );
        console.log(
          JSON.stringify({
            worker: 'notification-worker',
            action: messaging ? 'sent_fcm' : 'skipped_no_fcm_config',
            eventName: ev.event_name,
            userId: ev.actor_user_id,
            recipientCount: recipients.length,
            sentCount,
          })
        );
      }
    } catch (e) {
      console.error(JSON.stringify({ worker: 'notification-worker', error: String(e) }));
    }
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
}

loop().catch((e) => {
  console.error(e);
  process.exit(1);
});
