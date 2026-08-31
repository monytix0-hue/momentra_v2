/**
 * Phase 1 database foundation tests — direct PostgreSQL, no API layer.
 * Requires migrations applied. Uses postgres role for setup, momentra_app for RLS checks.
 */
import { Pool, PoolClient } from 'pg';
import dotenv from 'dotenv';
import path from 'path';
import { randomUUID } from 'crypto';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

type TestResult = { name: string; pass: boolean; detail?: string };

const results: TestResult[] = [];

function pass(name: string, detail?: string): void {
  results.push({ name, pass: true, detail });
  console.log('PASS', name, detail ?? '');
}

function fail(name: string, detail?: string): void {
  results.push({ name, pass: false, detail });
  console.error('FAIL', name, detail ?? '');
}

async function ensureRlsTestRole(client: PoolClient): Promise<void> {
  await client.query(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'momentra_rls_test') THEN
        CREATE ROLE momentra_rls_test NOLOGIN NOINHERIT;
      END IF;
    END $$;
  `);
  await client.query('GRANT USAGE ON SCHEMA core, business, shared, collaboration, security TO momentra_rls_test');
  await client.query('GRANT SELECT ON core.user_profile TO momentra_rls_test');
  await client.query('GRANT SELECT, INSERT ON business.company_location TO momentra_rls_test');
  await client.query('GRANT SELECT, INSERT, UPDATE ON shared.poll, shared.poll_option, shared.poll_vote TO momentra_rls_test');
  await client.query(`
    GRANT EXECUTE ON FUNCTION
      security.current_user_id(),
      security.has_database_role(text),
      security.is_backend_app(),
      security.is_analytics_worker(),
      security.is_memory_worker(),
      security.is_projection_worker(),
      security.owns_personal_moment(uuid),
      security.is_active_group_participant(uuid),
      security.is_active_company_member(uuid),
      security.can_access_moment(uuid),
      security.can_access_scope(text, uuid)
    TO momentra_rls_test
  `);
  await client.query('GRANT momentra_rls_test TO CURRENT_USER');
}

async function asAppUser(client: PoolClient, userId: string, fn: () => Promise<void>): Promise<void> {
  await client.query('BEGIN');
  try {
    await client.query('GRANT momentra_app TO CURRENT_USER');
    await client.query('SET LOCAL ROLE momentra_app');
    await client.query(`SELECT set_config('request.jwt.claim.sub', $1, true)`, [userId]);
    await fn();
    await client.query('COMMIT');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    await client.query('RESET ROLE');
  }
}

async function asRlsUser(client: PoolClient, userId: string, fn: () => Promise<void>): Promise<void> {
  await client.query('BEGIN');
  try {
    await ensureRlsTestRole(client);
    await client.query('SET LOCAL ROLE momentra_rls_test');
    await client.query(`SELECT set_config('request.jwt.claim.sub', $1, true)`, [userId]);
    await fn();
    await client.query('COMMIT');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    await client.query('RESET ROLE');
  }
}

async function testMigrationLedger(pool: Pool): Promise<void> {
  const r = await pool.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM public.momentra_migration_ledger WHERE migration_file LIKE 'V0%'`
  );
  const count = parseInt(r.rows[0]?.n ?? '0', 10);
  // V030/V034 are repeatable validation scripts and are intentionally excluded from the ledger.
  if (count >= 32) pass('migration-ledger', `${count} schema migrations recorded (V030/V034 validation excluded)`);
  else fail('migration-ledger', `expected >= 32, got ${count}`);
}

async function testDuplicateStableCodes(pool: Pool): Promise<void> {
  const r = await pool.query<{ n: string }>(`
    SELECT COUNT(*)::text AS n FROM (
      SELECT domain_code, code FROM core.moment_type GROUP BY domain_code, code HAVING COUNT(*) > 1
    ) d
  `);
  if (parseInt(r.rows[0]?.n ?? '0', 10) === 0) pass('duplicate-moment-type-codes');
  else fail('duplicate-moment-type-codes', r.rows[0]?.n);
}

async function testFinanceNumericTypes(pool: Pool): Promise<void> {
  const r = await pool.query<{ n: string }>(`
    SELECT COUNT(*)::text AS n FROM information_schema.columns
    WHERE table_schema = 'finance'
      AND column_name ~ '(amount|balance|total|paid|share|opening)'
      AND data_type IN ('real', 'double precision')
  `);
  if (parseInt(r.rows[0]?.n ?? '0', 10) === 0) pass('finance-decimal-types');
  else fail('finance-decimal-types', 'float columns found');
}

async function testFinanceDecimalMath(pool: Pool): Promise<void> {
  const r = await pool.query<{ sum: string }>(`SELECT (0.1::numeric + 0.2::numeric)::text AS sum`);
  if (r.rows[0]?.sum === '0.3') pass('finance-decimal-math');
  else fail('finance-decimal-math', `0.1+0.2=${r.rows[0]?.sum}`);
}

async function testCrossUserIsolation(pool: Pool): Promise<void> {
  const client = await pool.connect();
  const userA = randomUUID();
  const userB = randomUUID();
  try {
    await client.query('BEGIN');
    await client.query(
      `INSERT INTO core.user_profile (user_id, display_name, email, status)
       VALUES ($1, 'User A', 'a@test.local', 'ACTIVE'), ($2, 'User B', 'b@test.local', 'ACTIVE')`,
      [userA, userB]
    );
    await client.query('COMMIT');

    await asRlsUser(client, userA, async () => {
      const r = await client.query(`SELECT COUNT(*)::int AS n FROM core.user_profile WHERE user_id = $1`, [userB]);
      if (r.rows[0]?.n === 0) pass('rls-cross-user-deny');
      else fail('rls-cross-user-deny', `saw ${r.rows[0]?.n} rows`);
    });

    await asRlsUser(client, userA, async () => {
      const r = await client.query(`SELECT COUNT(*)::int AS n FROM core.user_profile WHERE user_id = $1`, [userA]);
      if (r.rows[0]?.n === 1) pass('rls-self-access');
      else fail('rls-self-access');
    });
  } finally {
    await client.query('DELETE FROM core.user_profile WHERE user_id IN ($1, $2)', [userA, userB]);
    client.release();
  }
}

async function testCompanyLocationIsolation(pool: Pool): Promise<void> {
  const client = await pool.connect();
  const userA = randomUUID();
  const userB = randomUUID();
  const companyA = randomUUID();
  const companyB = randomUUID();
  const locationB = randomUUID();
  try {
    await client.query('BEGIN');
    await client.query(
      `INSERT INTO core.user_profile (user_id, display_name, email, status)
       VALUES ($1, 'Member A', 'ma@test.local', 'ACTIVE'), ($2, 'Member B', 'mb@test.local', 'ACTIVE')`,
      [userA, userB]
    );
    await client.query(
      `INSERT INTO business.company (company_id, legal_name, display_name, status, created_by_user_id)
       VALUES ($1, 'Company A Ltd', 'Company A', 'ACTIVE', $3),
              ($2, 'Company B Ltd', 'Company B', 'ACTIVE', $4)`,
      [companyA, companyB, userA, userB]
    );
    await client.query(
      `INSERT INTO business.company_membership (company_membership_id, company_id, user_id, membership_type, status)
       VALUES ($1, $2, $3, 'OWNER', 'ACTIVE'), ($4, $5, $6, 'OWNER', 'ACTIVE')`,
      [randomUUID(), companyA, userA, randomUUID(), companyB, userB]
    );
    await client.query(
      `INSERT INTO business.company_location (company_location_id, company_id, name, status)
       VALUES ($1, $2, 'HQ B', 'ACTIVE')`,
      [locationB, companyB]
    );
    await client.query('COMMIT');

    await asAppUser(client, userA, async () => {
      const r = await client.query(
        `SELECT COUNT(*)::int AS n FROM business.company_location WHERE company_location_id = $1`,
        [locationB]
      );
      if (r.rows[0]?.n === 0) pass('rls-company-location-cross-company-deny');
      else fail('rls-company-location-cross-company-deny');
    });

    await client.query('BEGIN');
    await client.query('GRANT momentra_app TO CURRENT_USER');
    await client.query('SET LOCAL ROLE momentra_app');
    await client.query(`SELECT set_config('request.jwt.claim.sub', $1, true)`, [userA]);
    let rejected = false;
    try {
      await client.query(
        `INSERT INTO business.company_location (company_location_id, company_id, name, status)
         VALUES ($1, $2, 'Sneak', 'ACTIVE')`,
        [randomUUID(), companyB]
      );
    } catch {
      rejected = true;
    }
    await client.query('ROLLBACK');
    if (rejected) pass('rls-company-location-cross-company-write-deny');
    else fail('rls-company-location-cross-company-write-deny', 'insert succeeded');
  } finally {
    await client.query('DELETE FROM business.company_location WHERE company_id IN ($1, $2)', [companyA, companyB]);
    await client.query('DELETE FROM business.company_membership WHERE company_id IN ($1, $2)', [companyA, companyB]);
    await client.query('DELETE FROM business.company WHERE company_id IN ($1, $2)', [companyA, companyB]);
    await client.query('DELETE FROM core.user_profile WHERE user_id IN ($1, $2)', [userA, userB]);
    client.release();
  }
}

async function testPollLifecycle(pool: Pool): Promise<void> {
  const client = await pool.connect();
  const owner = randomUUID();
  const voter = randomUUID();
  const outsider = randomUUID();
  const momentId = randomUUID();
  const pollId = randomUUID();
  const optionId = randomUUID();
  const groupTypeId = await pool.query<{ id: string }>(
    `SELECT moment_type_id::text AS id FROM core.moment_type WHERE domain_code='GROUP' AND code='TRIP' LIMIT 1`
  );
  try {
    await client.query('BEGIN');
    await client.query(
      `INSERT INTO core.user_profile (user_id, display_name, email, status)
       VALUES ($1, 'Owner', 'o@test.local', 'ACTIVE'),
              ($2, 'Voter', 'v@test.local', 'ACTIVE'),
              ($3, 'Outsider', 'out@test.local', 'ACTIVE')`,
      [owner, voter, outsider]
    );
    await client.query(
      `INSERT INTO core.moment (moment_id, domain_code, moment_type_id, title, status, created_by_user_id, timezone)
       VALUES ($1, 'GROUP', $2, 'Trip Poll Test', 'ACTIVE', $3, 'UTC')`,
      [momentId, groupTypeId.rows[0]?.id, owner]
    );
    await client.query(
      `INSERT INTO collaboration.group_moment_context (moment_id, group_family, organizer_user_id, status)
       VALUES ($1, 'SHARED_EXPERIENCE', $2, 'ACTIVE')`,
      [momentId, owner]
    );
    await client.query(
      `INSERT INTO collaboration.moment_participant (participant_id, moment_id, user_id, participant_role, status)
       VALUES ($1, $2, $3, 'ORGANIZER', 'ACTIVE'), ($4, $2, $5, 'PARTICIPANT', 'ACTIVE')`,
      [randomUUID(), momentId, owner, randomUUID(), voter]
    );
    await client.query('COMMIT');

    await asAppUser(client, owner, async () => {
      await client.query(
        `INSERT INTO shared.poll (poll_id, moment_id, domain_code, question, status, created_by_user_id)
         VALUES ($1, $2, 'GROUP', 'Where?', 'OPEN', $3)`,
        [pollId, momentId, owner]
      );
      await client.query(
        `INSERT INTO shared.poll_option (poll_option_id, poll_id, option_text, sort_order)
         VALUES ($1, $2, 'Beach', 0)`,
        [optionId, pollId]
      );
    });
    pass('poll-create-by-member');

    await asAppUser(client, voter, async () => {
      await client.query(
        `INSERT INTO shared.poll_vote (poll_id, poll_option_id, moment_id, voter_user_id)
         VALUES ($1, $2, $3, $4)`,
        [pollId, optionId, momentId, voter]
      );
    });
    pass('poll-vote-by-member');

    await asAppUser(client, outsider, async () => {
      let denied = false;
      try {
        await client.query(`SELECT 1 FROM shared.poll WHERE poll_id = $1`, [pollId]);
        const r = await client.query(`SELECT COUNT(*)::int AS n FROM shared.poll WHERE poll_id = $1`, [pollId]);
        if (r.rows[0]?.n === 0) denied = true;
      } catch {
        denied = true;
      }
      if (denied) pass('poll-outsider-deny');
      else fail('poll-outsider-deny');
    });

    await client.query(`UPDATE shared.poll SET status = 'CLOSED' WHERE poll_id = $1`, [pollId]);

    await asAppUser(client, voter, async () => {
      let rejected = false;
      try {
        await client.query(
          `INSERT INTO shared.poll_vote (poll_id, poll_option_id, moment_id, voter_user_id)
           VALUES ($1, $2, $3, $4)`,
          [pollId, optionId, momentId, voter]
        );
      } catch {
        rejected = true;
      }
      if (rejected) pass('poll-closed-vote-deny');
      else fail('poll-closed-vote-deny');
    });
  } finally {
    await client.query('DELETE FROM shared.poll_vote WHERE poll_id = $1', [pollId]);
    await client.query('DELETE FROM shared.poll_option WHERE poll_id = $1', [pollId]);
    await client.query('DELETE FROM shared.poll WHERE poll_id = $1', [pollId]);
    await client.query('DELETE FROM collaboration.moment_participant WHERE moment_id = $1', [momentId]);
    await client.query('DELETE FROM collaboration.group_moment_context WHERE moment_id = $1', [momentId]);
    await client.query('DELETE FROM core.moment WHERE moment_id = $1', [momentId]);
    await client.query('DELETE FROM core.user_profile WHERE user_id IN ($1, $2, $3)', [owner, voter, outsider]);
    client.release();
  }
}

async function testSeedIdempotency(pool: Pool): Promise<void> {
  const before = await pool.query<{ n: string }>(`SELECT COUNT(*)::text AS n FROM core.moment_type`);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query(`
      INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, sort_order, status)
      SELECT moment_type_id, moment_category_id, domain_code, code, display_name, sort_order, status
      FROM core.moment_type WHERE code = 'TRIP' AND domain_code = 'GROUP'
      ON CONFLICT DO NOTHING
    `);
    await client.query('ROLLBACK');
  } finally {
    client.release();
  }
  const after = await pool.query<{ n: string }>(`SELECT COUNT(*)::text AS n FROM core.moment_type`);
  if (before.rows[0]?.n === after.rows[0]?.n) pass('seed-idempotency-snapshot');
  else fail('seed-idempotency-snapshot');
}

async function main(): Promise<void> {
  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL_DIRECT or DATABASE_URL required');

  const pool = new Pool({ connectionString: dbUrl });

  console.log('Phase 1 database tests\n');

  await testMigrationLedger(pool);
  await testDuplicateStableCodes(pool);
  await testFinanceNumericTypes(pool);
  await testFinanceDecimalMath(pool);
  await testCrossUserIsolation(pool);
  await testCompanyLocationIsolation(pool);
  await testPollLifecycle(pool);
  await testSeedIdempotency(pool);

  const passed = results.filter((r) => r.pass).length;
  const total = results.length;
  console.log(`\nResults: ${passed}/${total}`);

  await pool.end();
  if (passed !== total) process.exit(1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
