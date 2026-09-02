/**
 * Q0 — Dev/test-only Master Certification write verifier.
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true ALLOW_DEV_AUTH=1 \
 *   npx tsx scripts/qa/verify-maestro-cert.ts \
 *     --run-id QA-20260827-0042 \
 *     --correlation-id qa-20260827-personal-lifeops-expense-001 \
 *     --expect personal-expense \
 *     [--note MAESTRO-...] \
 *     [--moment-id <uuid>] \
 *     [--sibling-moment-id <uuid>]
 *
 * Refuses production via qa-env-guard.
 */
process.env.ALLOW_DEV_AUTH = process.env.ALLOW_DEV_AUTH || '1';

import { readFileSync, existsSync, mkdirSync, writeFileSync } from 'fs';
import path from 'path';
import { closePool, getPool } from '../../src/platform/database/pool';
import { assertQaFixturesSafe, defaultQaEmail, type QaFixtureAlias } from './qa-env-guard';

type ExpectKind =
  | 'personal-expense'
  | 'group-expense'
  | 'contribution'
  | 'observation'
  | 'future-item'
  | 'lifestyle'
  | 'relationship'
  | 'business-expense'
  | 'revenue'
  | 'invoice'
  | 'approval'
  | 'settlement'
  | 'setup'
  | 'generic';

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

type CheckResult = { name: string; status: 'PASS' | 'FAIL' | 'SKIP'; detail?: unknown };

async function countByCorrelation(
  sql: string,
  params: unknown[]
): Promise<{ count: number; rows: Record<string, unknown>[] }> {
  try {
    const r = await getPool().query(sql, params);
    return { count: r.rowCount ?? r.rows.length, rows: r.rows as Record<string, unknown>[] };
  } catch (e) {
    return { count: 0, rows: [{ error: String(e) }] };
  }
}

async function main(): Promise<void> {
  const repoRoot = path.resolve(__dirname, '../../../..');
  loadDotEnvFile(path.join(repoRoot, '.maestro', '.env.maestro.local'));
  assertQaFixturesSafe('verify-maestro-cert');

  const runId = arg('--run-id') || process.env.MAESTRO_RUN_ID || '';
  const correlationId = arg('--correlation-id') || '';
  const expect = (arg('--expect') || 'personal-expense') as ExpectKind;
  const note = arg('--note') || (runId ? `MAESTRO-${runId}` : '');
  const momentId = arg('--moment-id');
  const siblingMomentId = arg('--sibling-moment-id');
  const catalogId = arg('--catalog-id') || '';
  const alias = (arg('--alias') || 'QA_PERSONAL') as QaFixtureAlias;
  const amount = arg('--amount');
  const participants = Number(arg('--participants') || '0');

  // Auto-map catalog_id → expect kind when provided
  let resolvedExpect = expect;
  if (catalogId) {
    if (/Contribute/i.test(catalogId)) resolvedExpect = 'contribution';
    else if (/Settle/i.test(catalogId)) resolvedExpect = 'settlement';
    else if (/Revenue|Income/i.test(catalogId)) resolvedExpect = 'revenue';
    else if (/Invoice/i.test(catalogId)) resolvedExpect = 'invoice';
    else if (/G\d{2}:Expense/i.test(catalogId)) resolvedExpect = 'group-expense';
    else if (/B\d{2}:(Expense|Spend)/i.test(catalogId)) resolvedExpect = 'business-expense';
    else if (/P\d:Income|P\d:Expense/i.test(catalogId)) resolvedExpect = 'personal-expense';
  }
  // use resolvedExpect below — patch switch to use it
  const expectKind = resolvedExpect;

  if (!runId && !correlationId && !note) {
    throw new Error('Pass --run-id and/or --correlation-id and/or --note');
  }

  const checks: CheckResult[] = [];
  const pool = getPool();

  const corr = correlationId || null;
  const notePattern = note || null;

  // --- Canonical ---
  let canonicalSql = '';
  let canonicalParams: unknown[] = [];
  switch (expectKind) {
    case 'personal-expense':
    case 'business-expense':
    case 'group-expense':
      canonicalSql = `
        SELECT expense_id, moment_id, amount, currency_code, description, created_by_user_id, created_at
        FROM finance.expense
        WHERE ($1::text IS NULL OR description ILIKE '%' || $1 || '%')
          AND ($2::text IS NULL OR expense_id::text IN (
            SELECT audit_resource_id::text FROM audit.audit_record WHERE correlation_id = $2
            UNION
            SELECT aggregate_id::text FROM events.domain_event WHERE correlation_id = $2
          ))
          AND ($3::uuid IS NULL OR moment_id = $3)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [notePattern, corr, momentId || null];
      break;
    case 'contribution':
      canonicalSql = `
        SELECT contribution_id, moment_id, amount, created_at
        FROM finance.contribution
        WHERE ($1::text IS NULL OR contribution_id::text IN (
            SELECT aggregate_id::text FROM events.domain_event WHERE correlation_id = $1
          ))
          AND ($2::uuid IS NULL OR moment_id = $2)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [corr, momentId || null];
      break;
    case 'observation':
      canonicalSql = `
        SELECT life_operation_observation_id AS id, moment_id, created_at
        FROM personal.life_operation_observation
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'future-item':
      // V003: future_* tables (opportunity / pivot / learning / progress) — no future_item.
      canonicalSql = `
        SELECT id, moment_id, created_at FROM (
          SELECT future_opportunity_id AS id, moment_id, created_at FROM personal.future_opportunity
          UNION ALL
          SELECT future_pivot_id AS id, moment_id, created_at FROM personal.future_pivot
          UNION ALL
          SELECT future_learning_activity_id AS id, moment_id, created_at FROM personal.future_learning_activity
          UNION ALL
          SELECT future_progress_observation_id AS id, moment_id, created_at FROM personal.future_progress_observation
        ) future_rows
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'lifestyle':
      canonicalSql = `
        SELECT lifestyle_activity_id AS id, moment_id, created_at
        FROM personal.lifestyle_activity
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'relationship':
      canonicalSql = `
        SELECT relationship_activity_id AS id, moment_id, created_at
        FROM personal.relationship_activity
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'revenue':
      canonicalSql = `
        SELECT revenue_id AS id, moment_id, amount, created_at
        FROM finance.revenue
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'invoice':
      canonicalSql = `
        SELECT invoice_id AS id, moment_id, created_at
        FROM finance.invoice
        WHERE ($1::uuid IS NULL OR moment_id = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [momentId || null];
      break;
    case 'approval':
      canonicalSql = `
        SELECT approval_request_id AS id, status, created_at
        FROM governance.approval_request
        WHERE ($1::text IS NULL OR correlation_id = $1 OR approval_request_id::text = $1)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [corr];
      break;
    case 'settlement':
      canonicalSql = `
        SELECT settlement_id AS id, moment_id, amount, created_at
        FROM finance.settlement
        WHERE ($1::text IS NULL OR description ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%')
          AND ($2::uuid IS NULL OR moment_id = $2)
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [notePattern, momentId || null];
      break;
    default:
      canonicalSql = `
        SELECT domain_event_id, event_name, aggregate_id, correlation_id, created_at
        FROM events.domain_event
        WHERE ($1::text IS NULL OR correlation_id = $1)
           OR ($2::text IS NULL OR payload::text ILIKE '%' || $2 || '%')
        ORDER BY created_at DESC LIMIT 10`;
      canonicalParams = [corr, notePattern];
  }

  const canonical = await countByCorrelation(canonicalSql, canonicalParams);
  checks.push({
    name: 'Canonical record',
    status: canonical.count >= 1 ? 'PASS' : 'FAIL',
    detail: { count: canonical.count, sample: canonical.rows[0] },
  });

  // --- Audit ---
  const audit = await countByCorrelation(
    `SELECT audit_record_id, action_code, resource_type, resource_id, correlation_id, created_at
     FROM audit.audit_record
     WHERE ($1::text IS NULL OR correlation_id = $1)
        OR ($2::text IS NULL OR after_snapshot::text ILIKE '%' || $2 || '%')
     ORDER BY created_at DESC LIMIT 10`,
    [corr, notePattern]
  );
  checks.push({
    name: 'Audit',
    status: audit.count >= 1 ? 'PASS' : corr ? 'FAIL' : 'SKIP',
    detail: { count: audit.count, sample: audit.rows[0] },
  });

  // --- Domain event (correct schema: events.domain_event) ---
  const events = await countByCorrelation(
    `SELECT domain_event_id, event_name, aggregate_id, correlation_id, created_at
     FROM events.domain_event
     WHERE ($1::text IS NULL OR correlation_id = $1)
        OR ($2::text IS NULL OR payload::text ILIKE '%' || $2 || '%')
     ORDER BY created_at DESC LIMIT 10`,
    [corr, notePattern]
  );
  checks.push({
    name: 'Domain event',
    status: events.count >= 1 ? 'PASS' : corr || notePattern ? 'FAIL' : 'SKIP',
    detail: { count: events.count, sample: events.rows[0] },
  });

  // --- Outbox ---
  const outbox = await countByCorrelation(
    `SELECT o.outbox_event_id, o.domain_event_id, o.topic_code, o.created_at
     FROM events.outbox_event o
     JOIN events.domain_event e ON e.domain_event_id = o.domain_event_id
     WHERE ($1::text IS NULL OR e.correlation_id = $1)
        OR ($2::text IS NULL OR e.payload::text ILIKE '%' || $2 || '%')
     ORDER BY o.created_at DESC LIMIT 10`,
    [corr, notePattern]
  );
  checks.push({
    name: 'Outbox',
    status: outbox.count >= 1 ? 'PASS' : events.count >= 1 ? 'FAIL' : 'SKIP',
    detail: { count: outbox.count, sample: outbox.rows[0] },
  });

  // --- Projection / recent activity ---
  const activity = await countByCorrelation(
    `SELECT recent_activity_id, moment_id, activity_code, title, created_at
     FROM projection.recent_activity
     WHERE ($1::uuid IS NULL OR moment_id = $1)
        OR ($2::text IS NULL OR title ILIKE '%' || $2 || '%' OR payload::text ILIKE '%' || $2 || '%')
     ORDER BY created_at DESC LIMIT 10`,
    [momentId || null, notePattern]
  );
  checks.push({
    name: 'Projection / Activity',
    status: activity.count >= 1 ? 'PASS' : momentId || notePattern ? 'FAIL' : 'SKIP',
    detail: { count: activity.count, sample: activity.rows[0] },
  });

  // --- Expected scope ---
  if (momentId && canonical.rows[0] && 'moment_id' in canonical.rows[0]) {
    const got = String(canonical.rows[0].moment_id);
    checks.push({
      name: 'Expected scope',
      status: got === momentId ? 'PASS' : 'FAIL',
      detail: { expected: momentId, actual: got },
    });
  } else {
    checks.push({ name: 'Expected scope', status: 'SKIP', detail: 'no --moment-id or no moment on row' });
  }

  // --- No duplicate ---
  checks.push({
    name: 'No duplicate record',
    status: canonical.count === 1 ? 'PASS' : canonical.count === 0 ? 'FAIL' : 'FAIL',
    detail: { count: canonical.count },
  });

  // --- No cross-Moment write ---
  if (siblingMomentId && notePattern) {
    const sibling = await countByCorrelation(
      `SELECT expense_id FROM finance.expense
       WHERE moment_id = $1 AND description ILIKE '%' || $2 || '%'`,
      [siblingMomentId, notePattern]
    );
    checks.push({
      name: 'No cross-Moment write',
      status: sibling.count === 0 ? 'PASS' : 'FAIL',
      detail: { siblingMomentId, count: sibling.count },
    });
  } else {
    checks.push({ name: 'No cross-Moment write', status: 'SKIP' });
  }

  // --- Pulse / finance projection (best-effort; schema may vary) ---
  if (momentId) {
    let pulse = await countByCorrelation(
      `SELECT moment_id, updated_at FROM projection.moment_finance_snapshot
       WHERE moment_id = $1
       ORDER BY updated_at DESC NULLS LAST LIMIT 5`,
      [momentId]
    );
    if (pulse.count === 0 && pulse.rows[0] && 'error' in (pulse.rows[0] as object)) {
      pulse = await countByCorrelation(
        `SELECT moment_id FROM projection.recent_activity WHERE moment_id = $1 LIMIT 5`,
        [momentId]
      );
    }
    checks.push({
      name: 'Pulse / finance projection',
      status: pulse.count >= 1 ? 'PASS' : 'FAIL',
      detail: { count: pulse.count, sample: pulse.rows[0] },
    });
  } else {
    checks.push({ name: 'Pulse / finance projection', status: 'SKIP', detail: 'no --moment-id' });
  }

  // --- Amount match when provided ---
  if (amount && canonical.rows[0] && 'amount' in canonical.rows[0]) {
    const got = Number(canonical.rows[0].amount);
    const want = Math.abs(Number(amount));
    checks.push({
      name: 'Calculation amount',
      status: Math.abs(got - want) < 0.015 ? 'PASS' : 'FAIL',
      detail: { expected: want, actual: got },
    });
  } else {
    checks.push({ name: 'Calculation amount', status: 'SKIP' });
  }

  // --- Group split remainder proof ---
  let splitProof: unknown = null;
  if (expectKind === 'group-expense' && canonical.rows[0] && 'expense_id' in canonical.rows[0]) {
    const expenseId = canonical.rows[0].expense_id;
    const shares = await pool.query(
      `SELECT participant_user_id, share_amount, share_type
       FROM finance.expense_share WHERE expense_id = $1 ORDER BY share_amount DESC`,
      [expenseId]
    );
    const ctx = await pool.query(
      `SELECT * FROM finance.group_expense_context WHERE expense_id = $1`,
      [expenseId]
    ).catch(() => ({ rows: [] }));
    const obligations = await pool.query(
      `SELECT * FROM finance.obligation WHERE expense_id = $1`,
      [expenseId]
    ).catch(() => ({ rows: [] }));

    const total = Number(amount || canonical.rows[0].amount || 0);
    const n = participants || shares.rows.length || 0;
    let expected: number[] = [];
    if (n > 0 && total > 0) {
      const cents = Math.round(total * 100);
      const base = Math.floor(cents / n);
      const rem = cents - base * n;
      expected = Array.from({ length: n }, (_, i) => (base + (i < rem ? 1 : 0)) / 100);
      // Backend often puts remainder on last participant — also accept reverse remainder
    }
    const actual = shares.rows.map((r) => Number(r.share_amount));
    const sumActual = actual.reduce((a, b) => a + b, 0);
    const sumOk = Math.abs(sumActual - total) < 0.01;
    splitProof = {
      expenseId,
      total,
      participants: n,
      expectedEqualVariants: expected,
      actual,
      differenceFromTotal: Number((sumActual - total).toFixed(4)),
      group_expense_context: ctx.rows.length >= 1 ? 'PASS' : 'FAIL',
      expense_share: shares.rows.length >= 1 ? 'PASS' : 'FAIL',
      obligations: obligations.rows.length >= 0 ? (obligations.rows.length >= 1 ? 'PASS' : 'SKIP') : 'FAIL',
    };
    checks.push({
      name: 'Group split sum',
      status: sumOk && shares.rows.length >= 1 ? 'PASS' : 'FAIL',
      detail: splitProof,
    });
  }

  const failed = checks.filter((c) => c.status === 'FAIL');
  const result = {
    ok: failed.length === 0,
    runId,
    correlationId: corr,
    note,
    expect: expectKind,
    catalogId: catalogId || undefined,
    alias,
    email: defaultQaEmail(alias),
    checks,
    splitProof,
    generatedAt: new Date().toISOString(),
  };

  const outDir = path.join(repoRoot, '.maestro', 'reports', runId || 'no-run-id', 'backend');
  mkdirSync(outDir, { recursive: true });
  const outFile = path.join(
    outDir,
    `verify_${expect}_${(corr || note || 'na').replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 80)}.json`
  );
  writeFileSync(outFile, JSON.stringify(result, null, 2), 'utf8');

  console.log(JSON.stringify(result, null, 2));
  console.log(`[verify-maestro-cert] wrote ${outFile}`);
  if (!result.ok) process.exitCode = 1;
}

main()
  .catch((e) => {
    console.error('[verify-maestro-cert] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
