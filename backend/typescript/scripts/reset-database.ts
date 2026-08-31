/**
 * Reset disposable development database to empty Momentra state.
 * Drops all product schemas and migration ledger. Does NOT drop PostgreSQL extensions.
 *
 * Usage: tsx scripts/reset-database.ts [--confirm]
 */
import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const SCHEMAS = [
  'shared',
  'projection',
  'ai',
  'platform',
  'audit',
  'events',
  'memory',
  'analytics',
  'governance',
  'finance',
  'work',
  'business',
  'collaboration',
  'personal',
  'core',
  'security',
];

async function main(): Promise<void> {
  if (!process.argv.includes('--confirm')) {
    console.error('Refusing to reset without --confirm');
    process.exit(1);
  }

  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL_DIRECT or DATABASE_URL required');

  const pool = new Pool({ connectionString: dbUrl });
  const client = await pool.connect();

  try {
    console.log('Resetting Momentra schemas...');
    await client.query('BEGIN');

    await client.query('DROP TABLE IF EXISTS public.momentra_migration_ledger CASCADE');

    for (const schema of SCHEMAS) {
      await client.query(`DROP SCHEMA IF EXISTS ${schema} CASCADE`);
      console.log('  dropped schema:', schema);
    }

    await client.query('COMMIT');
    console.log('Database reset complete — ready for clean migration from V001.');
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
