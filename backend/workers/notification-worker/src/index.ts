/** Notification Worker — FCM push from domain events (stub when FCM not configured). */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const POLL_MS = parseInt(process.env.NOTIFICATION_POLL_MS ?? '10000', 10);

async function loop(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  console.log(JSON.stringify({ worker: 'notification-worker', status: 'started', fcm: !!process.env.FCM_SERVER_KEY }));

  while (true) {
    try {
      const events = await pool.query<{ domain_event_id: string; actor_user_id: string; event_name: string }>(
        `SELECT de.domain_event_id, de.actor_user_id, de.event_name
         FROM events.domain_event de
         WHERE de.recorded_at > now() - interval '1 minute'
         ORDER BY de.recorded_at DESC
         LIMIT 5`
      );
      for (const ev of events.rows) {
        const dedupeKey = `${ev.event_name}:${ev.actor_user_id}:${ev.domain_event_id}`;
        console.log(JSON.stringify({ worker: 'notification-worker', dedupeKey, action: 'would_send_fcm' }));
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
