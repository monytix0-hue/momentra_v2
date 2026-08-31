import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  // Live ledger is public.momentra_migration_ledger (not platform.schema_migration).
  await pool.query(
    `DELETE FROM public.momentra_migration_ledger WHERE migration_file >= 'V012__audit_platform.sql'`
  );
  console.log('Cleared momentra_migration_ledger from V012 onward');
  await pool.end();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
