/**
 * S9-F Personal performance harness (+ feeds S9-J API half of visible-ready).
 * Engineering targets (not automatic FAIL if geo RTT floors):
 *   Pulse warm p95 <= 300ms, Activity page1 p95 <= 300ms,
 *   Moment-switch API pair <= 500ms visible-ready proxy,
 *   Expense submit p95 <= 1000ms preferred.
 */
process.env.ALLOW_DEV_AUTH = '1';
process.env.RATE_LIMIT_DISABLED = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import { writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool, prewarmPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { clearKnownUserProfiles } from '../src/platform/auth';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

async function firstPersonalTypeCode(): Promise<string> {
  const types = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
  );
  if (!types.rows[0]) throw new Error('Need PERSONAL moment type');
  return types.rows[0].code;
}

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
    min: Math.round((sorted[0] ?? 0) * 100) / 100,
    max: Math.round((sorted[sorted.length - 1] ?? 0) * 100) / 100,
    ...extra,
  };
}

function authFor(uid: string) {
  return { 'X-Dev-Firebase-Uid': uid, 'X-Firebase-Project-Id': projectId };
}

async function sampleGet(path: string, headers: Record<string, string>, n: number) {
  const samples: number[] = [];
  let lastStatus = 0;
  let lastBytes = 0;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    const res = await request(app).get(path).set(headers);
    samples.push(performance.now() - t0);
    lastStatus = res.status;
    lastBytes = JSON.stringify(res.body).length;
    if (res.status >= 500) {
      throw new Error(`GET ${path} status ${res.status}`);
    }
  }
  return summarize(path, samples, { status: lastStatus, approxBodyBytes: lastBytes });
}

async function timedGet(path: string, headers: Record<string, string>) {
  const t0 = performance.now();
  const res = await request(app).get(path).set(headers);
  return { ms: performance.now() - t0, status: res.status, body: res.body };
}

async function timedPost(path: string, headers: Record<string, string>, body: unknown) {
  const t0 = performance.now();
  const res = await request(app)
    .post(path)
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send(body);
  return { ms: performance.now() - t0, status: res.status, body: res.body };
}

/** Client-visible Pulse ready proxy: sequential pulse then activity (pre-fix) vs parallel max. */
async function pulseVisibleReadySamples(
  headers: Record<string, string>,
  momentId: string | undefined,
  n: number,
  mode: 'sequential' | 'parallel'
) {
  const qPulse = momentId ? `?momentId=${momentId}` : '';
  const qAct = momentId
    ? `?momentId=${momentId}&limit=20`
    : `?limit=20`;
  const samples: number[] = [];
  let lastStatus = 0;
  for (let i = 0; i < n; i++) {
    const t0 = performance.now();
    if (mode === 'sequential') {
      const a = await request(app).get(`/v1/personal/pulse${qPulse}`).set(headers);
      const b = await request(app).get(`/v1/personal/activity${qAct}`).set(headers);
      lastStatus = Math.max(a.status, b.status);
    } else {
      const [a, b] = await Promise.all([
        request(app).get(`/v1/personal/pulse${qPulse}`).set(headers),
        request(app).get(`/v1/personal/activity${qAct}`).set(headers),
      ]);
      lastStatus = Math.max(a.status, b.status);
    }
    if (lastStatus >= 500) throw new Error(`pulse_visible_ready_${mode} status ${lastStatus}`);
    samples.push(performance.now() - t0);
  }
  return summarize(`pulse_visible_ready_${mode}`, samples, {
    momentId: momentId ?? null,
    status: lastStatus,
  });
}

async function main() {
  clearKnownUserProfiles();
  await prewarmPool();
  const personalType = await firstPersonalTypeCode();

  const emptyUid = `s9f-empty-${randomUUID().slice(0, 8)}`;
  const emptyAuth = authFor(emptyUid);

  // --- EMPTY inventory ---
  const emptyColdMe = await timedGet('/v1/me', emptyAuth);
  await sampleGet('/v1/me', emptyAuth, 2);
  const emptyWarmMe = await sampleGet('/v1/me', emptyAuth, 10);
  const emptyPulse = await sampleGet('/v1/personal/pulse', emptyAuth, 10);
  const emptyActivity = await sampleGet('/v1/personal/activity?limit=20', emptyAuth, 10);
  const emptyLife = await sampleGet('/v1/personal/life', emptyAuth, 5);
  const emptyMemory = await sampleGet('/v1/personal/memory', emptyAuth, 5);
  const emptyMoments = await sampleGet('/v1/personal/moments', emptyAuth, 5);
  const emptySetups = await sampleGet('/v1/personal/setups', emptyAuth, 5);
  const health = await sampleGet('/health/ready', {}, 10);

  // --- SETUP user with 2 moments + activity volume ---
  const setupUid = `s9f-setup-${randomUUID().slice(0, 8)}`;
  const setupAuth = authFor(setupUid);
  await timedGet('/v1/me', setupAuth);

  const m1 = await timedPost('/v1/moments', setupAuth, {
    domainCode: 'PERSONAL',
    momentTypeCode: personalType,
    title: 'S9F Goal A',
  });
  const m2 = await timedPost('/v1/moments', setupAuth, {
    domainCode: 'PERSONAL',
    momentTypeCode: personalType,
    title: 'S9F Goal B',
  });
  const momentIdA = m1.body?.data?.momentId as string;
  const momentIdB = m2.body?.data?.momentId as string;
  if (!momentIdA || !momentIdB) {
    throw new Error(`moment create failed: ${JSON.stringify({ m1: m1.body, m2: m2.body })}`);
  }

  const expenseSamples: number[] = [];
  let expenseStatuses: number[] = [];
  for (let i = 0; i < 15; i++) {
    const r = await timedPost(`/v1/moments/${momentIdA}/expenses`, setupAuth, {
      amount: `${(10 + i).toFixed(2)}`,
      currencyCode: 'USD',
      description: `S9F timed ${i}`,
    });
    expenseSamples.push(r.ms);
    expenseStatuses.push(r.status);
    if (r.status >= 500) throw new Error(`expense status ${r.status}`);
  }
  // Extra volume on A for activity pagination (not timed)
  for (let i = 0; i < 20; i++) {
    const r = await timedPost(`/v1/moments/${momentIdA}/expenses`, setupAuth, {
      amount: `${(30 + i).toFixed(2)}`,
      currencyCode: 'USD',
      description: `S9F page ${i}`,
    });
    if (r.status >= 500) throw new Error(`expense fill status ${r.status}`);
  }
  // A few on B so switch targets have data
  for (let i = 0; i < 3; i++) {
    await timedPost(`/v1/moments/${momentIdB}/expenses`, setupAuth, {
      amount: `${(5 + i).toFixed(2)}`,
      currencyCode: 'USD',
      description: `S9F B ${i}`,
    });
  }

  // Warm personal reads (populated)
  const warmMe = await sampleGet('/v1/me', setupAuth, 10);
  const warmPulse = await sampleGet(`/v1/personal/pulse?momentId=${momentIdA}`, setupAuth, 15);
  const warmPulseB = await sampleGet(`/v1/personal/pulse?momentId=${momentIdB}`, setupAuth, 10);
  const warmActivity = await sampleGet(
    `/v1/personal/activity?momentId=${momentIdA}&limit=20`,
    setupAuth,
    15
  );
  const actFirst = await timedGet(`/v1/personal/activity?momentId=${momentIdA}&limit=20`, setupAuth);
  const nextCursor = actFirst.body?.data?.nextCursor as string | null | undefined;
  const activityPage2 = nextCursor
    ? await sampleGet(
        `/v1/personal/activity?momentId=${momentIdA}&limit=20&cursor=${encodeURIComponent(nextCursor)}`,
        setupAuth,
        10
      )
    : { label: 'activity-page2', n: 0, note: 'no nextCursor' };

  const warmLife = await sampleGet('/v1/personal/life', setupAuth, 8);
  const warmMemory = await sampleGet('/v1/personal/memory', setupAuth, 8);
  const warmMoments = await sampleGet('/v1/personal/moments', setupAuth, 8);
  const warmSetups = await sampleGet('/v1/personal/setups', setupAuth, 5);

  // Visible-ready BEFORE heavy write fill — measure after timed expenses only would skew;
  // measure after all writes with status guards (pool should still be healthy).
  const pulseSeq = await pulseVisibleReadySamples(setupAuth, momentIdA, 8, 'sequential');
  const pulsePar = await pulseVisibleReadySamples(setupAuth, momentIdA, 8, 'parallel');

  const switchParallelSamples: number[] = [];
  for (let i = 0; i < 8; i++) {
    const dest = i % 2 === 0 ? momentIdB : momentIdA;
    const t0 = performance.now();
    const [p, a] = await Promise.all([
      request(app).get(`/v1/personal/pulse?momentId=${dest}`).set(setupAuth),
      request(app).get(`/v1/personal/activity?momentId=${dest}&limit=20`).set(setupAuth),
    ]);
    if (p.status !== 200 || a.status !== 200) {
      throw new Error(`moment switch status pulse=${p.status} act=${a.status}`);
    }
    switchParallelSamples.push(performance.now() - t0);
  }
  const momentSwitchParallel = summarize(
    'moment_switch_visible_ready_parallel_tab_fetch',
    switchParallelSamples
  );

  const switchSeqSamples: number[] = [];
  for (let i = 0; i < 8; i++) {
    const dest = i % 2 === 0 ? momentIdB : momentIdA;
    const t0 = performance.now();
    const p = await request(app).get(`/v1/personal/pulse?momentId=${dest}`).set(setupAuth);
    const a = await request(app)
      .get(`/v1/personal/activity?momentId=${dest}&limit=20`)
      .set(setupAuth);
    if (p.status !== 200 || a.status !== 200) {
      throw new Error(`moment switch seq status pulse=${p.status} act=${a.status}`);
    }
    switchSeqSamples.push(performance.now() - t0);
  }
  const momentSwitchSequential = summarize(
    'moment_switch_visible_ready_sequential_client',
    switchSeqSamples
  );

  // Duplicate /me check: two consecutive /v1/me should not be required for tab refresh
  const scopedRefreshNote = {
    invariant:
      'Tab/moment refresh uses personalTabRefreshToken → personal/* only; must not require second /v1/me',
    measured: 'API-only; client unit tests assert token bump without bootstrap call',
  };

  const report = {
    capturedAt: new Date().toISOString(),
    block: 'S9-F',
    env: {
      nodeEnv: process.env.NODE_ENV ?? null,
      allowDevAuth: true,
      dbPoolMax: config.database.poolMax,
      healthReadyP50FloorMs: health.p50,
    },
    targets: {
      personalPulseWarmP95Ms: 300,
      activityFirstPageP95Ms: 300,
      momentSwitchVisibleReadyMs: 500,
      expenseSubmitP95Ms: 1000,
      note: 'Engineering targets; geo RTT floor may prevent hard FAIL',
    },
    empty: {
      coldMeMs: Math.round(emptyColdMe.ms * 100) / 100,
      warmMe: emptyWarmMe,
      pulse: emptyPulse,
      activity: emptyActivity,
      life: emptyLife,
      memory: emptyMemory,
      moments: emptyMoments,
      setups: emptySetups,
    },
    setup: {
      momentCreateAMs: Math.round(m1.ms * 100) / 100,
      momentCreateBMs: Math.round(m2.ms * 100) / 100,
      momentIdA,
      momentIdB,
      expenseSubmit: summarize('POST /expenses', expenseSamples, {
        statuses: [...new Set(expenseStatuses)],
        okRate: expenseStatuses.filter((s) => s === 201 || s === 200).length / expenseStatuses.length,
      }),
      warmMe,
      pulseA: warmPulse,
      pulseB: warmPulseB,
      activityPage1: warmActivity,
      activityPage2,
      life: warmLife,
      memory: warmMemory,
      moments: warmMoments,
      setups: warmSetups,
      momentSwitchSequential,
      momentSwitchParallel,
      pulseVisibleReadySequential: pulseSeq,
      pulseVisibleReadyParallel: pulsePar,
      waterfallFinding: {
        clientPulseTab:
          'PersonalPulseActiveContent awaits getPulse then getActivity sequentially — visible-ready ≈ sum',
        fixInScope: 'Parallelize tab fetches (S9-F/J client); do not reopen S9-B/C/G/H',
        measuredDeltaMs: {
          sequentialP95: pulseSeq.p95,
          parallelP95: pulsePar.p95,
          savingsP95: Math.round((pulseSeq.p95 - pulsePar.p95) * 100) / 100,
        },
      },
      scopedRefreshNote,
    },
    health,
    vsTargets: {
      pulseWarmP95: { value: warmPulse.p95, target: 300, meet: warmPulse.p95 <= 300 },
      activityPage1P95: { value: warmActivity.p95, target: 300, meet: warmActivity.p95 <= 300 },
      momentSwitchParallelP95: {
        value: momentSwitchParallel.p95,
        target: 500,
        meet: momentSwitchParallel.p95 <= 500,
      },
      expenseSubmitP95: {
        value: pct([...expenseSamples].sort((a, b) => a - b), 95),
        target: 1000,
        meet: pct([...expenseSamples].sort((a, b) => a - b), 95) <= 1000,
      },
    },
  };

  const outDir = join(__dirname, '../../../docs/implementation');
  mkdirSync(outDir, { recursive: true });
  const jsonPath = join(outDir, 'S9_F_PERSONAL_MEASURE.json');
  writeFileSync(jsonPath, JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  console.error(`Wrote ${jsonPath}`);
  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
