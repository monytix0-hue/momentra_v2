import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const r = await pool.query<{ migration_file: string }>(
    `SELECT migration_file FROM public.momentra_migration_ledger ORDER BY migration_file`
  );
  console.log('Applied migrations:');
  for (const row of r.rows) console.log(' ', row.migration_file);

  const checks = [
    ['company_location', `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='business' AND table_name='company_location') AS ok`],
    ['shared.poll', `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='shared' AND table_name='poll') AS ok`],
    ['platform.user_device', `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='platform' AND table_name='user_device') AS ok`],
  ];
  for (const [name, sql] of checks) {
    const x = await pool.query<{ ok: boolean }>(sql);
    console.log(`${name}:`, x.rows[0]?.ok ?? false);
  }
  await pool.end();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
