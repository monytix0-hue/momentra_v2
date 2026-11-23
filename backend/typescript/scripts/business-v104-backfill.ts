/**
 * P2.1 V104 ledger backfill CLI.
 *   npx tsx scripts/business-v104-backfill.ts --dry-run
 *   npx tsx scripts/business-v104-backfill.ts --apply
 * Never runs V103.
 */
import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { runLedgerBackfill } from '../src/modules/business/ledger-backfill';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });
dotenv.config({ path: path.resolve(__dirname, '../.env') });

async function main() {
  const apply = process.argv.includes('--apply');
  const mode = apply ? 'APPLY' : 'DRY_RUN';
  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL required');

  const pool = new Pool({ connectionString: dbUrl });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const report = await runLedgerBackfill(client, {
      mode,
      actorLabel: `cli:${mode.toLowerCase()}`,
    });
    await client.query('COMMIT');
    console.log(JSON.stringify(report, null, 2));
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
