/**
 * S9-C cold/pool measurement — measure before changing min/prewarm.
 * Does not print connection strings or secrets.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import { Client, Pool } from 'pg';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { clearKnownUserProfiles } from '../src/platform/auth';

function pct(sorted: number[], p: number): number {
  if (!sorted.length) return 0;
  const i = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return Math.round(sorted[i]! * 100) / 100;
}

function summarize(label: string, samples: number[]) {
  const sorted = [...samples].sort((a, b) => a - b);
  return {
    label,
    n: sorted.length,
    p50: pct(sorted, 50),
    p95: pct(sorted, 95),
    min: Math.round(sorted[0]! * 100) / 100,
    max: Math.round(sorted[sorted.length - 1]! * 100) / 100,
  };
}

function parseDbMeta(url: string) {
  try {
    const u = new URL(url);
    return {
      host: u.hostname,
      port: u.port || '5432',
      isSupabasePooler: u.hostname.includes('pooler.supabase.com'),
      likelyTransactionPooler: u.port === '6543',
      likelySessionPooler: u.port === '5432' && u.hostname.includes('pooler'),
    };
  } catch {
    return { host: 'unparsed', port: '?', isSupabasePooler: false };
  }
}

async function timeConnectAndQuery(n: number) {
  const samples: number[] = [];
  for (let i = 0; i < n; i++) {
    const client = new Client({ connectionString: config.database.url });
    const t0 = performance.now();
    await client.connect();
    const connectMs = performance.now() - t0;
    const t1 = performance.now();
    await client.query('SELECT 1');
    const queryMs = performance.now() - t1;
    await client.end();
    samples.push(connectMs);
    if (i === 0) {
      (samples as unknown as { firstQueryMs?: number }).firstQueryMs = queryMs;
    }
  }
  return {
    connect: summarize('fresh_Client.connect', samples),
    firstSelect1AfterConnectMs: Math.round(((samples as unknown as { firstQueryMs?: number }).firstQueryMs ?? 0) * 100) / 100,
  };
}

async function timeParallelConnects(width: number, rounds: number) {
  const wall: number[] = [];
  for (let r = 0; r < rounds; r++) {
    const clients = Array.from({ length: width }, () => new Client({ connectionString: config.database.url }));
    const t0 = performance.now();
    await Promise.all(clients.map((c) => c.connect()));
    wall.push(performance.now() - t0);
    await Promise.all(clients.map((c) => c.end()));
  }
  return summarize(`parallel_connect_width_${width}`, wall);
}

async function timePoolCheckout(pool: Pool, n: number) {
  // warm one
  {
    const c = await pool.connect();
    await c.query('SELECT 1');
    c.release();
  }
  const samples: number[] = [];
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const c = await pool.connect();
    samples.push(performance.now() - t0);
    c.release();
  }
  return summarize('warm_pool_checkout', samples);
}

async function timeColdPoolFirstQuery(min: number) {
  const pool = new Pool({
    connectionString: config.database.url,
    max: 10,
    min,
    idleTimeoutMillis: 30000,
    allowExitOnIdle: false,
  });
  const t0 = performance.now();
  if (min > 0) {
    // node-pg does not auto-create min on construct; prewarm manually
    const clients = await Promise.all(
      Array.from({ length: min }, async () => {
        const c = await pool.connect();
        await c.query('SELECT 1');
        return c;
      }),
    );
    for (const c of clients) c.release();
  }
  const prewarmMs = performance.now() - t0;

  const t1 = performance.now();
  await Promise.all(
    Array.from({ length: 5 }, async () => {
      const c = await pool.connect();
      try {
        await c.query('SELECT 1');
      } finally {
        c.release();
      }
    }),
  );
  const fiveParallelAfterPrewarmMs = performance.now() - t1;
  await pool.end();
  return {
    min,
    prewarmMs: Math.round(prewarmMs * 100) / 100,
    fiveParallelSelect1Ms: Math.round(fiveParallelAfterPrewarmMs * 100) / 100,
  };
}

async function measureMeColdWarm() {
  clearKnownUserProfiles();
  // Force new pool by closing app pool — createApp uses shared getPool
  await closePool();
  const app = createApp();
  const uid = `s9c-${randomUUID().slice(0, 8)}`;
  const auth = { 'X-Dev-Firebase-Uid': uid };

  const t0 = performance.now();
  const cold = await request(app).get('/v1/me').set(auth);
  const coldMs = performance.now() - t0;
  if (cold.status !== 200) throw new Error(`cold status ${cold.status}`);

  const warmSamples: number[] = [];
  for (let i = 0; i < 20; i++) {
    const t = performance.now();
    const res = await request(app).get('/v1/me').set(auth);
    warmSamples.push(performance.now() - t);
    if (res.status !== 200) throw new Error(`warm status ${res.status}`);
  }
  warmSamples.sort((a, b) => a - b);

  return {
    coldMs: Math.round(coldMs * 100) / 100,
    warm: summarize('/v1/me_warm', warmSamples),
    poolAfter: {
      totalCount: getPool().totalCount,
      idleCount: getPool().idleCount,
      waitingCount: getPool().waitingCount,
    },
  };
}

async function explainCritical() {
  const pool = getPool();
  const plans: Record<string, string> = {};
  const q = async (name: string, sql: string, params: unknown[]) => {
    const r = await pool.query(`EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) ${sql}`, params);
    plans[name] = r.rows.map((row: { 'QUERY PLAN': string }) => row['QUERY PLAN']).join('\n');
  };
  // Use a random uuid — plans still show scan type
  const uid = '00000000-0000-0000-0000-000000000001';
  await q(
    'user_profile_by_id',
    'SELECT email, display_name, timezone, locale, status FROM core.user_profile WHERE user_id = $1',
    [uid],
  );
  await q(
    'personal_moments_limit',
    `SELECT moment_id, title, status, moment_type_code, display_rank
     FROM projection.personal_moments
     WHERE user_id = $1 AND ($2::int IS NULL OR display_rank > $2)
     ORDER BY display_rank ASC LIMIT $3`,
    [uid, null, 21],
  );
  await q(
    'companies_for_user',
    `SELECT c.company_id, c.display_name
     FROM business.company c
     JOIN business.company_membership cm ON cm.company_id = c.company_id
     WHERE cm.user_id = $1 AND cm.status = 'ACTIVE'
     ORDER BY c.display_name`,
    [uid],
  );
  return plans;
}

async function main() {
  const meta = parseDbMeta(config.database.url);
  const connect = await timeConnectAndQuery(5);
  const parallel1 = await timeParallelConnects(1, 3);
  const parallel5 = await timeParallelConnects(5, 3);
  const freshPool = new Pool({ connectionString: config.database.url, max: 10 });
  const checkout = await timePoolCheckout(freshPool, 20);
  await freshPool.end();

  const prewarmTradeoffs = [];
  for (const min of [0, 1, 2, 3]) {
    prewarmTradeoffs.push(await timeColdPoolFirstQuery(min));
  }

  const me = await measureMeColdWarm();
  const plans = await explainCritical();

  // Index usage sniff (safe)
  const idx = await getPool().query<{
    indexrelname: string;
    idx_scan: string;
  }>(
    `SELECT indexrelname::text, idx_scan::text
     FROM pg_stat_user_indexes
     WHERE schemaname IN ('core','projection','business','collaboration','finance','personal')
     ORDER BY idx_scan DESC NULLS LAST
     LIMIT 25`,
  );

  console.log(
    JSON.stringify(
      {
        capturedAt: new Date().toISOString(),
        stage: 'S9-C-measure',
        dbMeta: meta,
        poolConfig: {
          max: config.database.poolMax,
          idleMs: config.database.poolIdleMs,
          statementTimeoutMs: config.database.statementTimeoutMs,
          minConfigured: false,
        },
        connect,
        parallelConnect: { width1: parallel1, width5: parallel5 },
        warmCheckout: checkout,
        prewarmTradeoffs,
        meEndpoint: me,
        indexScansTop25: idx.rows,
        explainHasIndexScan: Object.fromEntries(
          Object.entries(plans).map(([k, v]) => [
            k,
            {
              usesIndex: /Index (Scan|Only Scan)/i.test(v),
              usesSeqScan: /Seq Scan/i.test(v),
              planHead: v.split('\n').slice(0, 6).join(' | '),
            },
          ]),
        ),
      },
      null,
      2,
    ),
  );
  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
