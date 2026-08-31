/**
 * Post-Maestro backend verification helper for critical finance writes.
 *
 * Usage (after a Maestro critical run):
 *   QA_FIXTURES_ENABLED=true ALLOW_DEV_AUTH=1 \
 *   npx tsx scripts/qa/verify-maestro-finance.ts --alias QA_PERSONAL --note MAESTRO-20260827190000
 */
process.env.ALLOW_DEV_AUTH = process.env.ALLOW_DEV_AUTH || '1';

import { readFileSync, existsSync } from 'fs';
import path from 'path';
import request from 'supertest';
import { createApp } from '../../src/app';
import { config } from '../../src/platform/config';
import { closePool, getPool } from '../../src/platform/database/pool';
import { assertQaFixturesSafe, defaultQaEmail, type QaFixtureAlias } from './qa-env-guard';

function loadDotEnvFile(filePath: string): void {
  if (!existsSync(filePath)) return;
  for (const line of readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq <= 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
}

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

async function main(): Promise<void> {
  const repoRoot = path.resolve(__dirname, '../../../..');
  loadDotEnvFile(path.join(repoRoot, '.maestro', '.env.maestro.local'));
  assertQaFixturesSafe('verify-maestro-finance');

  const alias = (arg('--alias') || 'QA_PERSONAL') as QaFixtureAlias;
  const note = arg('--note') || `MAESTRO-${process.env.MAESTRO_RUN_ID || ''}`;
  if (!note || note.endsWith('-')) throw new Error('Pass --note MAESTRO-<runId> or set MAESTRO_RUN_ID');

  const email = (process.env[`${alias}_EMAIL`] || defaultQaEmail(alias)).toLowerCase();
  const profile = await getPool().query<{ user_id: string }>(
    `SELECT user_id FROM core.user_profile WHERE lower(email) = $1 AND status = 'ACTIVE'`,
    [email]
  );
  const userId = profile.rows[0]?.user_id;
  if (!userId) throw new Error(`No profile for ${email}`);

  const expenses = await getPool().query<{ expense_id: string; description: string | null }>(
    `SELECT expense_id, description
     FROM finance.expense
     WHERE created_by_user_id = $1
       AND description ILIKE '%' || $2 || '%'
     ORDER BY created_at DESC
     LIMIT 5`,
    [userId, note]
  );

  const events = await getPool().query<{ domain_event_id: string }>(
    `SELECT domain_event_id FROM events.domain_event
     WHERE payload::text ILIKE '%' || $1 || '%'
        OR correlation_id::text ILIKE '%' || $1 || '%'
     ORDER BY created_at DESC
     LIMIT 5`,
    [note]
  ).catch(() => ({ rows: [] as { domain_event_id: string }[] }));

  const result = {
    ok: expenses.rows.length >= 1,
    alias,
    email,
    userId,
    note,
    expenseCount: expenses.rows.length,
    expenses: expenses.rows,
    matchingEvents: events.rows.length,
  };
  console.log(JSON.stringify(result, null, 2));
  if (!result.ok) process.exitCode = 1;
}

main()
  .catch((e) => {
    console.error('[verify-maestro-finance] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
