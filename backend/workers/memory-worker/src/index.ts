/** Memory Worker — memory/pattern/learning (D12 stub). */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

const POLL_MS = 20000;

async function loop(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  console.log(JSON.stringify({ worker: 'memory-worker', status: 'started' }));
  while (true) {
    try {
      const mem = await pool.query(`SELECT COUNT(*)::int AS n FROM memory.memory`);
      console.log(JSON.stringify({ worker: 'memory-worker', memoryCount: mem.rows[0]?.n ?? 0 }));
    } catch (e) {
      console.error(JSON.stringify({ worker: 'memory-worker', error: String(e) }));
    }
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
}

loop().catch((e) => { console.error(e); process.exit(1); });
