/** Scheduler — time-based reminders into the domain-event → notification pipeline. */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { runReminderTick } from '../../typescript/src/modules/notifications/reminders';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const INTERVAL_MS = parseInt(process.env.SCHEDULER_INTERVAL_MS ?? '60000', 10);

async function tick(pool: Pool): Promise<void> {
  const result = await runReminderTick(pool);
  console.log(
    JSON.stringify({
      worker: 'scheduler',
      action: 'tick',
      checks: ['weekly_reminders', 'task_due', 'group_setup_reminders', 'digest_flush'],
      ...result,
      at: new Date().toISOString(),
    })
  );
}

async function loop(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  console.log(JSON.stringify({ worker: 'scheduler', status: 'started', intervalMs: INTERVAL_MS }));
  const shutdown = async () => {
    await pool.end();
    process.exit(0);
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  while (true) {
    try {
      await tick(pool);
    } catch (e) {
      console.error(JSON.stringify({ worker: 'scheduler', error: String(e) }));
    }
    await new Promise((r) => setTimeout(r, INTERVAL_MS));
  }
}

loop().catch((e) => {
  console.error(e);
  process.exit(1);
});
