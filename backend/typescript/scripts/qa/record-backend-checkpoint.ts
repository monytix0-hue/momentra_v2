/**
 * S9-QA-F..I — Append a Backend_Checkpoints-style JSONL sample during certification runs.
 *
 * Usage (after a shard / at milestone):
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/record-backend-checkpoint.ts \
 *     --platform android --wave F --milestone 250 --run-id 20260902
 */
import { appendFileSync, mkdirSync, existsSync, readFileSync, writeFileSync } from 'fs';
import path from 'path';
import { performance } from 'perf_hooks';
import { assertQaFixturesSafe } from './qa-env-guard';
import { closePool, getPool } from '../../src/platform/database/pool';

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

async function timeQuery(label: string, sql: string): Promise<{ label: string; ms: number; ok: boolean }> {
  const t0 = performance.now();
  try {
    await getPool().query(sql);
    return { label, ms: Number((performance.now() - t0).toFixed(2)), ok: true };
  } catch (e) {
    return { label, ms: Number((performance.now() - t0).toFixed(2)), ok: false };
  }
}

function percentile(sorted: number[], p: number): number {
  if (!sorted.length) return 0;
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[Math.max(0, idx)];
}

async function sampleLatencies(samples: number): Promise<{ p50: number; p95: number; p99: number; n: number }> {
  const times: number[] = [];
  for (let i = 0; i < samples; i++) {
    const t0 = performance.now();
    try {
      await getPool().query('SELECT 1');
      times.push(performance.now() - t0);
    } catch {
      times.push(performance.now() - t0);
    }
  }
  times.sort((a, b) => a - b);
  return {
    n: times.length,
    p50: Number(percentile(times, 50).toFixed(2)),
    p95: Number(percentile(times, 95).toFixed(2)),
    p99: Number(percentile(times, 99).toFixed(2)),
  };
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
  assertQaFixturesSafe('record-backend-checkpoint');

  const platform = arg('--platform') || 'android';
  const wave = arg('--wave') || 'E';
  const milestone = Number(arg('--milestone') || '0');
  const runId = arg('--run-id') || process.env.MAESTRO_RUN_ID || 'local';
  const samples = Number(arg('--samples') || '20');

  const poolStats = await sampleLatencies(samples);
  const probes = await Promise.all([
    timeQuery('recent_activity', 'SELECT count(*) FROM projection.recent_activity'),
    timeQuery('expense', 'SELECT count(*) FROM finance.expense'),
    timeQuery('domain_event', 'SELECT count(*) FROM events.domain_event'),
  ]);

  const row = {
    when: new Date().toISOString(),
    platform,
    wave,
    milestone,
    runId,
    dbRoundTrip: poolStats,
    probes,
  };

  const outDir = path.join(repoRoot, 'docs', 'qa', 'reconciliation');
  mkdirSync(outDir, { recursive: true });
  const jsonl = path.join(outDir, 'backend_checkpoints.jsonl');
  appendFileSync(jsonl, JSON.stringify(row) + '\n', 'utf8');

  const mdPath = path.join(outDir, 'BACKEND_CHECKPOINTS.md');
  const line = `| ${row.when} | ${platform} | ${wave} | ${milestone} | ${poolStats.p50} | ${poolStats.p95} | ${poolStats.p99} | ${runId} |\n`;
  if (!existsSync(mdPath)) {
    writeFileSync(
      mdPath,
      `# Backend checkpoints (S9-QA-F..I)\n\n| When (UTC) | Platform | Wave | Milestone | p50 ms | p95 ms | p99 ms | Run ID |\n|------------|----------|------|----------:|-------:|-------:|-------:|--------|\n` +
        line,
      'utf8'
    );
  } else {
    appendFileSync(mdPath, line, 'utf8');
  }

  console.log(JSON.stringify(row, null, 2));
  console.log(`[record-backend-checkpoint] appended ${jsonl}`);
}

main()
  .catch((e) => {
    console.error('[record-backend-checkpoint] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
