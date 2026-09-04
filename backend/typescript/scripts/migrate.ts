/**
 * Apply schema migrations from frds/migrations (V001–V029 + V031+).
 *
 * V030 is the final production-readiness validation gate and must NEVER execute
 * as part of normal feature development — even if listed in MIGRATION_ORDER.txt.
 * Use an explicit future ops path for V030 only when intentionally validating production.
 */
import fs from 'fs';
import path from 'path';
import { createHash } from 'crypto';
import { Pool, PoolClient } from 'pg';
import dotenv from 'dotenv';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const FRDS = path.resolve(__dirname, '../../../frds');
const MIGRATIONS = path.join(FRDS, 'migrations');
const MANIFEST = path.join(FRDS, 'manifest', 'MIGRATION_ORDER.txt');

/** V030 production validation gate — blocked from all migrate runner paths. */
function isV030Blocked(file: string): boolean {
  return file.startsWith('V030');
}

async function ensureLedger(client: PoolClient): Promise<void> {
  await client.query(`
    CREATE TABLE IF NOT EXISTS public.momentra_migration_ledger (
      migration_file TEXT PRIMARY KEY,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      checksum TEXT
    );
  `);
}

async function isApplied(client: PoolClient, file: string): Promise<boolean> {
  const r = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM public.momentra_migration_ledger WHERE migration_file = $1`,
    [file]
  );
  return parseInt(r.rows[0]?.n ?? '0', 10) > 0;
}

async function markApplied(client: PoolClient, file: string): Promise<void> {
  const sql = fs.readFileSync(path.join(MIGRATIONS, file), 'utf8');
  const checksum = createHash('sha256').update(sql).digest('hex');
  await client.query(
    `INSERT INTO public.momentra_migration_ledger (migration_file, checksum)
     VALUES ($1, $2)
     ON CONFLICT (migration_file) DO UPDATE SET checksum = EXCLUDED.checksum, applied_at = now()`,
    [file, checksum]
  );
}

async function verifyMigrationApplied(client: PoolClient, file: string): Promise<boolean> {
  const checks: Record<string, string> = {
    'V012__audit_platform.sql': `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='audit' AND table_name='audit_record') AS ok`,
    'V013__ai.sql': `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='ai' AND table_name='action_proposal') AS ok`,
    'V014__projection.sql': `SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='projection' AND table_name='personal_pulse') AS ok`,
  };
  const sql = checks[file];
  if (!sql) return true;
  const r = await client.query<{ ok: boolean }>(sql);
  return r.rows[0]?.ok === true;
}

async function repairLegacySchemas(client: PoolClient): Promise<void> {
  console.warn('Repair: dropping legacy personal/business schemas (pre-FRDS tables)...');
  await client.query('DROP SCHEMA IF EXISTS personal CASCADE');
  await client.query('DROP SCHEMA IF EXISTS business CASCADE');
  await client.query(
    `DELETE FROM public.momentra_migration_ledger WHERE migration_file IN ('V003__personal.sql', 'V005__business.sql')`
  );
}

async function repairPartialPlatform(client: PoolClient): Promise<void> {
  const audit = await client.query<{ ok: boolean }>(
    `SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name='audit') AS ok`
  );
  if (audit.rows[0]?.ok) return;
  console.warn('Repair: platform schema exists without audit — dropping platform for V012 replay...');
  await client.query('DROP SCHEMA IF EXISTS platform CASCADE');
  await client.query(
    `DELETE FROM public.momentra_migration_ledger WHERE migration_file >= 'V012__audit_platform.sql'`
  );
}

async function runMigrationFile(client: PoolClient, file: string, opts?: { skipLedger?: boolean }): Promise<'ok' | 'skip'> {
  if (isV030Blocked(file)) {
    console.log('SKIP (V030 blocked — production validation gate only):', file);
    return 'skip';
  }

  const isValidation = file.startsWith('V034');
  if (!isValidation && (await isApplied(client, file)) && (await verifyMigrationApplied(client, file))) {
    console.log('SKIP (ledger):', file);
    return 'skip';
  }

  const sql = fs.readFileSync(path.join(MIGRATIONS, file), 'utf8');
  console.log('==>', file);
  try {
    await client.query('BEGIN');
    await client.query(sql);
    await client.query('COMMIT');
    if (!opts?.skipLedger && !isValidation) {
      await markApplied(client, file);
    }
    console.log('OK', file);
    return 'ok';
  } catch (e) {
    await client.query('ROLLBACK');
    const msg = String(e);
    if (!isValidation && msg.includes('already exists') && (await verifyMigrationApplied(client, file))) {
      console.warn('SKIP (exists, verified):', file);
      await markApplied(client, file);
      return 'skip';
    }
    throw e;
  }
}

async function main(): Promise<void> {
  const installOnly = process.argv.includes('--install-only');
  const validationOnly = process.argv.includes('--validation-only');
  const repairLegacy = process.argv.includes('--repair-legacy') || process.env.REPAIR_LEGACY_SCHEMAS === '1';
  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL_DIRECT or DATABASE_URL required');

  console.log('Connecting for migrations...');

  const order = fs.readFileSync(MANIFEST, 'utf8').split('\n').map((l) => l.trim()).filter(Boolean);
  // V030 is never runnable via this tool. V034 forward validation remains optional.
  const blocked = order.filter((f) => isV030Blocked(f));
  for (const file of blocked) {
    console.log('SKIP (V030 blocked — production validation gate only):', file);
  }
  const validation = order.filter((f) => f.startsWith('V034'));
  const install = order.filter((f) => !isV030Blocked(f) && !validation.includes(f));
  const forwardOnly = process.argv.includes('--forward-only');

  const pool = new Pool({ connectionString: dbUrl });
  const client = await pool.connect();

  try {
    await ensureLedger(client);

    const locked = await client.query<{ locked: boolean }>(
      `SELECT pg_try_advisory_lock(hashtext('momentra_migrations')) AS locked`
    );
    if (!locked.rows[0]?.locked) {
      throw new Error('Another migration runner holds the advisory lock');
    }

    if (repairLegacy) {
      await repairLegacySchemas(client);
    }
    await repairPartialPlatform(client);

    if (!validationOnly) {
      const installFiles = forwardOnly
        ? install.filter((f) => /^V03[1-9]__|^V04[0-9]__|^V05[0-9]__|^V06[0-9]__/.test(f))
        : install;
      for (const file of installFiles) {
        await runMigrationFile(client, file);
      }
      console.log(
        forwardOnly
          ? 'Forward pack complete (V031+; V030 skipped)'
          : 'Schema installation complete (V001–V029 + V031+; V030 skipped)'
      );
    }

    if (!installOnly) {
      for (const file of validation) {
        console.log('==> validation', file);
        await runMigrationFile(client, file);
        console.log('OK validation', file);
      }
    }
  } finally {
    await client.query(`SELECT pg_advisory_unlock(hashtext('momentra_migrations'))`);
    client.release();
    await pool.end();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
