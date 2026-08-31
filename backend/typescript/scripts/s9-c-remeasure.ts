/**
 * S9-C re-measure: cold with prewarm(min=2) vs warm; preserve warm ≤150ms p95.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPoolStats, prewarmPool } from '../src/platform/database/pool';
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

async function measureColdWarm(label: string, doPrewarm: boolean) {
  clearKnownUserProfiles();
  await closePool();
  const app = createApp();
  let prewarm: { warmed: number; durationMs: number } | null = null;
  if (doPrewarm) {
    prewarm = await prewarmPool(config.database.poolMin);
  }
  const uid = `s9c-r-${randomUUID().slice(0, 8)}`;
  const auth = { 'X-Dev-Firebase-Uid': uid };

  const t0 = performance.now();
  const coldRes = await request(app).get('/v1/me').set(auth);
  const coldMs = Math.round((performance.now() - t0) * 100) / 100;
  if (coldRes.status !== 200) throw new Error(`${label} cold ${coldRes.status}`);

  const warmSamples: number[] = [];
  for (let i = 0; i < 20; i++) {
    const t = performance.now();
    const res = await request(app).get('/v1/me').set(auth);
    warmSamples.push(performance.now() - t);
    if (res.status !== 200) throw new Error(`${label} warm ${res.status}`);
  }

  return {
    label,
    prewarm,
    coldMs,
    warm: summarize('/v1/me_warm', warmSamples),
    poolAfter: getPoolStats(),
  };
}

async function main() {
  const withoutPrewarm = await measureColdWarm('no_prewarm', false);
  const withPrewarm = await measureColdWarm('prewarm_min', true);

  const report = {
    capturedAt: new Date().toISOString(),
    stage: 'S9-C',
    targets: { warmP95Ms: 150, coldMateriallyReducedFrom: 1350 },
    config: {
      poolMax: config.database.poolMax,
      poolMin: config.database.poolMin,
      poolIdleMs: config.database.poolIdleMs,
      connectionTimeoutMs: config.database.connectionTimeoutMs,
    },
    withoutPrewarm,
    withPrewarm,
    passWarmP95: withPrewarm.warm.p95 <= 150 && withoutPrewarm.warm.p95 <= 150,
    coldImprovementVs1350: {
      noPrewarmMs: withoutPrewarm.coldMs,
      withPrewarmMs: withPrewarm.coldMs,
      reductionPctWithPrewarm: Math.round((1 - withPrewarm.coldMs / 1350) * 1000) / 10,
    },
  };
  console.log(JSON.stringify(report, null, 2));
  await closePool();
  if (!report.passWarmP95) process.exitCode = 2;
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
