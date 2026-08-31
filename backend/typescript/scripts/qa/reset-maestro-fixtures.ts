/**
 * Reset domain state for known Maestro QA identities only.
 *
 * Guards: NODE_ENV != production && QA_FIXTURES_ENABLED=true (+ local DB check).
 *
 * Does NOT delete Firebase Auth users or core.user_profile rows.
 * Archives QA-owned moments/companies and deactivates QA memberships/invites
 * so seed can rebuild deterministic prerequisites.
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/reset-maestro-fixtures.ts
 */
import { readFileSync, existsSync } from 'fs';
import path from 'path';
import { getPool, closePool } from '../../src/platform/database/pool';
import {
  assertQaFixturesSafe,
  QA_FIXTURE_ALIASES,
  defaultQaEmail,
  type QaFixtureAlias,
} from './qa-env-guard';

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

function resolveEmails(): string[] {
  const emails: string[] = [];
  for (const alias of QA_FIXTURE_ALIASES) {
    const envKey = `${alias}_EMAIL` as const;
    const email = (process.env[envKey] || defaultQaEmail(alias as QaFixtureAlias)).trim().toLowerCase();
    if (email) emails.push(email);
  }
  // Legacy smoke user — only reset if explicitly opted in
  if (process.env.QA_RESET_INCLUDE_MAESTRO === '1' && process.env.MAESTRO_EMAIL) {
    emails.push(process.env.MAESTRO_EMAIL.trim().toLowerCase());
  }
  return [...new Set(emails)];
}

async function main(): Promise<void> {
  const repoRoot = path.resolve(__dirname, '../../../..');
  loadDotEnvFile(path.join(repoRoot, '.maestro', '.env.maestro.local'));
  assertQaFixturesSafe('reset-maestro-fixtures');

  const emails = resolveEmails();
  if (!emails.length) throw new Error('No QA emails resolved');

  const pool = getPool();
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const users = await client.query<{ user_id: string; email: string }>(
      `SELECT user_id, lower(email) AS email
       FROM core.user_profile
       WHERE lower(email) = ANY($1::text[])
         AND status = 'ACTIVE'`,
      [emails]
    );

    const userIds = users.rows.map((r) => r.user_id);
    if (!userIds.length) {
      await client.query('COMMIT');
      console.log(
        JSON.stringify({ ok: true, reset: false, reason: 'no_matching_profiles', emails }, null, 2)
      );
      return;
    }

    const moments = await client.query<{ n: string }>(
      `UPDATE core.moment
       SET status = 'ARCHIVED', version = version + 1, updated_at = now()
       WHERE created_by_user_id = ANY($1::uuid[])
         AND status <> 'ARCHIVED'
       RETURNING moment_id`,
      [userIds]
    );

    const invites = await client.query(
      `UPDATE collaboration.moment_invite
       SET status = 'REVOKED', updated_at = now(), version = version + 1
       WHERE created_by_user_id = ANY($1::uuid[])
         AND status IN ('PENDING', 'ACTIVE')
       RETURNING invite_id`,
      [userIds]
    );

    const companies = await client.query(
      `UPDATE business.company
       SET status = 'INACTIVE', version = version + 1, updated_at = now()
       WHERE created_by_user_id = ANY($1::uuid[])
         AND status = 'ACTIVE'
       RETURNING company_id`,
      [userIds]
    );

    const memberships = await client.query(
      `UPDATE business.company_membership
       SET status = 'INACTIVE', version = version + 1, updated_at = now()
       WHERE user_id = ANY($1::uuid[])
         AND status IN ('ACTIVE', 'INVITED')
       RETURNING company_membership_id`,
      [userIds]
    );

    const participants = await client.query(
      `UPDATE collaboration.moment_participant
       SET status = 'LEFT', updated_at = now(), version = version + 1
       WHERE user_id = ANY($1::uuid[])
         AND status IN ('ACTIVE', 'INVITED')
       RETURNING participant_id`,
      [userIds]
    );

    await client.query('COMMIT');

    console.log(
      JSON.stringify(
        {
          ok: true,
          emails,
          userIds,
          archivedMoments: moments.rows.length,
          revokedInvites: invites.rows.length,
          archivedCompanies: companies.rows.length,
          removedMemberships: memberships.rows.length,
          leftParticipants: participants.rows.length,
        },
        null,
        2
      )
    );
  } catch (e) {
    await client.query('ROLLBACK').catch(() => undefined);
    throw e;
  } finally {
    client.release();
  }
}

main()
  .catch((e) => {
    console.error('[reset-maestro-fixtures] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
