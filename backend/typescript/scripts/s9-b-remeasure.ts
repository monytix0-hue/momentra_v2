/**
 * S9-B re-measure harness — same methodology as S9-A for before/after.
 * Also reports ensureUserProfile warm behavior + parallel bootstrap wall-clock.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import {
  clearKnownUserProfiles,
  ensureUserProfile,
  forgetKnownUserProfile,
} from '../src/platform/auth';
import type { RequestContext } from '../src/platform/request-context/context';
import { getMeBootstrap } from '../src/modules/device/bootstrap';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';
const uid = `s9-b-${randomUUID().slice(0, 8)}`;
const auth = { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };

function pct(sorted: number[], p: number): number {
  if (!sorted.length) return 0;
  const i = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return Math.round(sorted[i]! * 100) / 100;
}

function summarize(label: string, samples: number[], extra: Record<string, unknown> = {}) {
  const sorted = [...samples].sort((a, b) => a - b);
  return {
    label,
    n: sorted.length,
    p50: pct(sorted, 50),
    p95: pct(sorted, 95),
    p99: pct(sorted, 99),
    min: Math.round(sorted[0]! * 100) / 100,
    max: Math.round(sorted[sorted.length - 1]! * 100) / 100,
    ...extra,
  };
}

async function sampleGet(path: string, n: number, headers: Record<string, string> = auth) {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBytes = 0;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app).get(path).set(headers);
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBytes = JSON.stringify(res.body).length;
  }
  return summarize(path, samples, { status: lastStatus, approxBodyBytes: lastBytes });
}

async function main() {
  clearKnownUserProfiles();

  const tCold = performance.now();
  const coldRes = await request(app).get('/v1/me').set(auth);
  const coldMeMs = Math.round((performance.now() - tCold) * 100) / 100;
  if (coldRes.status !== 200) throw new Error(`cold /v1/me ${coldRes.status}`);
  const userId = coldRes.body.data.userId as string;
  const payloadBytes = JSON.stringify(coldRes.body).length;

  // Warm ensure path (should be cached after cold /me)
  const ensureWarm: number[] = [];
  const ensureResults: string[] = [];
  for (let i = 0; i < 10; i++) {
    const t0 = performance.now();
    const r = await ensureUserProfile(userId);
    ensureWarm.push(performance.now() - t0);
    ensureResults.push(r);
  }

  // First ensure after forget → SELECT (existed)
  forgetKnownUserProfile(userId);
  const tSelect = performance.now();
  const ensureAfterForget = await ensureUserProfile(userId);
  const ensureSelectMs = Math.round((performance.now() - tSelect) * 100) / 100;

  // Parallel bootstrap wall-clock (5 pool checkouts)
  const ctx: RequestContext = {
    correlationId: randomUUID(),
    userId,
    firebaseUid: uid,
    firebaseProjectId: projectId,
    roles: ['USER'],
    permissions: [],
  };
  const pool = getPool();
  const parallelSamples: number[] = [];
  let peakTotalCount = 0;
  for (let i = 0; i < 5; i++) {
    const t0 = performance.now();
    await getMeBootstrap(null, ctx);
    parallelSamples.push(performance.now() - t0);
    peakTotalCount = Math.max(peakTotalCount, pool.totalCount);
  }

  // Endpoint warm sample (same as S9-A: n=20 after warm)
  await sampleGet('/v1/me', 2);
  const me = await sampleGet('/v1/me', 20);
  const healthReady = await sampleGet('/health/ready', 10, {});

  const report = {
    capturedAt: new Date().toISOString(),
    stage: 'S9-B',
    target: { warmMeP95Ms: 400 },
    env: {
      dbPoolMax: config.database.poolMax,
      dbPoolIdleMs: config.database.poolIdleMs,
    },
    userId,
    coldMeMs,
    endpoints: { me, healthReady },
    provision: {
      warmEnsure: summarize('ensureUserProfile_warm', ensureWarm, {
        results: ensureResults,
      }),
      afterForget: { result: ensureAfterForget, ms: ensureSelectMs },
      behavior: 'warm path uses in-process known-user skip (no SELECT/UPSERT)',
    },
    bootstrap: {
      parallelWallClock: summarize('getMeBootstrap_parallel', parallelSamples),
      peakPoolTotalCountDuringMeasure: peakTotalCount,
      poolMax: config.database.poolMax,
      queryModel:
        'Promise.all of 5 independent pool checkouts; capabilities skipped when inventory empty',
      approxDbRoundTripLayersEmpty: 1,
    },
    payloadApproxBytes: payloadBytes,
    cache: {
      meResponseCache: 'none',
      provisionKnownUserHitRateWarm: ensureResults.filter((r) => r === 'cached').length / ensureResults.length,
    },
    s9aBefore: {
      warmP50: 790.51,
      warmP95: 835.55,
      cold: 1427.06,
      provisionEveryRequestMs: 111,
      sequentialBootstrapMs: 673,
      queryCountHandler: 6,
    },
    passWarmP95: me.p95 <= 400,
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
