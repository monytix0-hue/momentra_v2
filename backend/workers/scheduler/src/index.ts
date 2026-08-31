/** Scheduler — time-based triggers into command/event pipeline (stub). */
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const INTERVAL_MS = parseInt(process.env.SCHEDULER_INTERVAL_MS ?? '60000', 10);

async function tick(): Promise<void> {
  console.log(JSON.stringify({
    worker: 'scheduler',
    action: 'tick',
    checks: ['overdue_tasks', 'consent_expiry', 'projection_rebuild_schedule'],
    at: new Date().toISOString(),
  }));
}

async function loop(): Promise<void> {
  console.log(JSON.stringify({ worker: 'scheduler', status: 'started' }));
  while (true) {
    try {
      await tick();
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
