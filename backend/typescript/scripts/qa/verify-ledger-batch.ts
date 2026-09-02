/**
 * S9-QA-D/J — Batch-verify ledger correlation notes against DB.
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/verify-ledger-batch.ts \
 *     --platform android --csv .maestro/data/pilot/android_pilot_150.csv --limit 20
 */
import { readFileSync, existsSync, writeFileSync, mkdirSync } from 'fs';
import path from 'path';
import { closePool, getPool } from '../../src/platform/database/pool';
import { assertQaFixturesSafe } from './qa-env-guard';

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

function parseCsv(text: string): Record<string, string>[] {
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (!lines.length) return [];
  const headers = lines[0].split(',');
  // Simple splitter — joined CSV may contain quoted commas; prefer first-pass for pilot
  return lines.slice(1).map((line) => {
    const cols: string[] = [];
    let cur = '';
    let q = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"') {
        if (q && line[i + 1] === '"') {
          cur += '"';
          i++;
        } else q = !q;
      } else if (ch === ',' && !q) {
        cols.push(cur);
        cur = '';
      } else cur += ch;
    }
    cols.push(cur);
    const row: Record<string, string> = {};
    headers.forEach((h, idx) => {
      row[h] = cols[idx] ?? '';
    });
    return row;
  });
}

async function main() {
  const repoRoot = path.resolve(__dirname, '../../../..');
  const envLocal = path.join(repoRoot, '.maestro', '.env.maestro.local');
  if (existsSync(envLocal)) {
    for (const line of readFileSync(envLocal, 'utf8').split(/\r?\n/)) {
      const t = line.trim();
      if (!t || t.startsWith('#')) continue;
      const eq = t.indexOf('=');
      if (eq > 0 && process.env[t.slice(0, eq)] === undefined) {
        process.env[t.slice(0, eq)] = t.slice(eq + 1);
      }
    }
  }
  assertQaFixturesSafe('verify-ledger-batch');

  const platform = arg('--platform') || 'android';
  const csvRel =
    arg('--csv') ||
    `.maestro/data/pilot/${platform}_pilot_150.csv`;
  const limit = Number(arg('--limit') || '50');
  const csvPath = path.isAbsolute(csvRel) ? csvRel : path.join(repoRoot, csvRel);
  if (!existsSync(csvPath)) throw new Error(`Missing ${csvPath}`);

  const rows = parseCsv(readFileSync(csvPath, 'utf8'))
    .filter((r) => r.join_status && !r.join_status.startsWith('SKIP'))
    .slice(0, limit);

  const pool = getPool();
  const results: Array<{
    txnId: string;
    note: string;
    expect: string;
    found: number;
    status: string;
  }> = [];

  for (const r of rows) {
    const note = r.correlation_note || r.Description;
    const semantic = r.Semantic_Type;
    let expect = 'expense';
    if (semantic === 'Contribution') expect = 'contribution';
    if (semantic === 'Income') expect = 'revenue-or-income';
    if (semantic === 'Transfer') expect = 'movement';

    let found = 0;
    try {
      if (expect === 'contribution') {
        const q = await pool.query(
          `SELECT count(*)::int AS c FROM finance.contribution
           WHERE description ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%'`,
          [note.slice(0, 48)]
        );
        found = q.rows[0]?.c ?? 0;
      } else if (expect === 'movement') {
        const q = await pool.query(
          `SELECT count(*)::int AS c FROM finance.movement
           WHERE description ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%'`,
          [note.slice(0, 48)]
        );
        found = q.rows[0]?.c ?? 0;
      } else if (expect === 'revenue-or-income') {
        const q = await pool.query(
          `SELECT (
             (SELECT count(*) FROM finance.revenue WHERE description ILIKE '%' || $1 || '%') +
             (SELECT count(*) FROM finance.income WHERE description ILIKE '%' || $1 || '%') +
             (SELECT count(*) FROM finance.expense WHERE description ILIKE '%' || $1 || '%')
           )::int AS c`,
          [note.slice(0, 48)]
        );
        found = q.rows[0]?.c ?? 0;
      } else {
        const q = await pool.query(
          `SELECT count(*)::int AS c FROM finance.expense
           WHERE description ILIKE '%' || $1 || '%'`,
          [note.slice(0, 48)]
        );
        found = q.rows[0]?.c ?? 0;
      }
    } catch (e) {
      results.push({
        txnId: r.Txn_ID,
        note,
        expect,
        found: 0,
        status: `ERROR:${String(e).slice(0, 80)}`,
      });
      continue;
    }

    results.push({
      txnId: r.Txn_ID,
      note,
      expect,
      found,
      status: found === 1 ? 'PASS' : found === 0 ? 'MISSING' : 'DUPLICATE',
    });
  }

  const summary = {
    platform,
    csv: csvRel,
    checked: results.length,
    pass: results.filter((r) => r.status === 'PASS').length,
    missing: results.filter((r) => r.status === 'MISSING').length,
    duplicate: results.filter((r) => r.status === 'DUPLICATE').length,
    error: results.filter((r) => r.status.startsWith('ERROR')).length,
    results,
  };

  const outDir = path.join(repoRoot, '.maestro', 'reports');
  mkdirSync(outDir, { recursive: true });
  const out = path.join(outDir, `verify_ledger_${platform}_${Date.now()}.json`);
  writeFileSync(out, JSON.stringify(summary, null, 2) + '\n', 'utf8');
  console.log(JSON.stringify({ ok: true, ...summary, results: undefined, out }, null, 2));

  await closePool();
  if (summary.missing > 0 || summary.duplicate > 0) process.exitCode = 1;
}

main().catch(async (e) => {
  console.error(e);
  process.exitCode = 1;
  try {
    await closePool();
  } catch {
    /* ignore */
  }
});
