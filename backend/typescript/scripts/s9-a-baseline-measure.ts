/**
 * S9-A measurement harness — READ/MEASURE ONLY. No optimization patches.
 * Captures endpoint percentiles + /v1/me waterfall decomposition.
 */
process.env.ALLOW_DEV_AUTH = '1';

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { config } from '../src/platform/config';
import { provisionUserProfile } from '../src/platform/auth';
import * as projectionService from '../src/modules/projection/service';
import * as businessService from '../src/modules/business/service';
import type { RequestContext } from '../src/platform/request-context/context';
import { getMeBootstrap } from '../src/modules/device/bootstrap';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';
const uid = `s9-base-${randomUUID().slice(0, 8)}`;
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

async function timed<T>(fn: () => Promise<T>): Promise<{ ms: number; value: T }> {
  const t0 = performance.now();
  const value = await fn();
  return { ms: Math.round((performance.now() - t0) * 100) / 100, value };
}

async function decomposeMe(userId: string) {
  const ctx: RequestContext = {
    requestId: randomUUID(),
    correlationId: randomUUID(),
    userId,
    firebaseUid: uid,
    firebaseProjectId: projectId,
    roles: ['USER'],
    permissions: [],
    email: undefined,
    displayName: undefined,
  };

  const provision = await timed(() => provisionUserProfile(userId));

  const client = await getPool().connect();
  const stages: Record<string, number> = {};
  try {
    const profile = await timed(() =>
      client.query(
        `SELECT email, display_name, timezone, locale, status
         FROM core.user_profile WHERE user_id = $1`,
        [userId],
      ),
    );
    stages.profile = profile.ms;

    const personal = await timed(() =>
      projectionService.listPersonalMoments(client, userId, undefined, 20),
    );
    stages.personalMoments = personal.ms;

    const group = await timed(() => projectionService.listGroupMoments(client, ctx, undefined, 20));
    stages.groupMoments = group.ms;

    const business = await timed(() =>
      projectionService.listBusinessMoments(client, ctx, undefined, 20),
    );
    stages.businessMoments = business.ms;

    const companies = await timed(() => businessService.listCompanies(client, ctx));
    stages.companies = companies.ms;

    const capabilities = await timed(() =>
      client.query(
        `SELECT DISTINCT c.code
         FROM core.capability c
         JOIN core.moment_type_capability mtc ON mtc.capability_id = c.capability_id
         JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id
         JOIN core.moment m ON m.moment_type_id = mt.moment_type_id
         WHERE m.status = 'ACTIVE'
           AND (
             EXISTS (
               SELECT 1 FROM personal.personal_moment_context pmc
               WHERE pmc.moment_id = m.moment_id AND pmc.user_id = $1
             )
             OR EXISTS (
               SELECT 1 FROM collaboration.moment_participant mp
               WHERE mp.moment_id = m.moment_id AND mp.user_id = $1 AND mp.status = 'ACTIVE'
             )
             OR EXISTS (
               SELECT 1 FROM business.business_moment_context bmc
               JOIN business.company_membership cm ON cm.company_id = bmc.company_id
               WHERE bmc.moment_id = m.moment_id AND cm.user_id = $1 AND cm.status = 'ACTIVE'
             )
           )
         ORDER BY c.code
         LIMIT 200`,
        [userId],
      ),
    );
    stages.capabilities = capabilities.ms;

    const fullBootstrap = await timed(() => getMeBootstrap(client, ctx));
    stages.fullGetMeBootstrap = fullBootstrap.ms;

    const sequentialSum =
      stages.profile +
      stages.personalMoments +
      stages.groupMoments +
      stages.businessMoments +
      stages.companies +
      stages.capabilities;

    return {
      provisionUserProfileMs: provision.ms,
      dbQueryCountInBootstrap: 6,
      redisUsed: false,
      parallelized: false,
      stagesMs: stages,
      sequentialStageSumMs: Math.round(sequentialSum * 100) / 100,
      inventoryCounts: {
        personal: personal.value.items.length,
        group: group.value.items.length,
        business: business.value.items.length,
        companies: companies.value.items.length,
        capabilities: capabilities.value.rows.length,
      },
      responseShapeBytes: JSON.stringify(fullBootstrap.value).length,
    };
  } finally {
    client.release();
  }
}

async function scaleProbe() {
  const client = await getPool().connect();
  try {
    const counts = await client.query<{
      members: string;
      expenses: string;
      activity: string;
      positions: string;
      companies: string;
    }>(
      `SELECT
         (SELECT COUNT(*)::text FROM collaboration.moment_participant) AS members,
         (SELECT COUNT(*)::text FROM finance.expense) AS expenses,
         (SELECT COUNT(*)::text FROM projection.recent_activity) AS activity,
         (SELECT COUNT(*)::text FROM projection.group_finance_position) AS positions,
         (SELECT COUNT(*)::text FROM business.company) AS companies`,
    );
    const posLatency = await timed(() =>
      client.query(`SELECT COUNT(*) FROM projection.group_finance_position`),
    );
    const activityLatency = await timed(() =>
      client.query(
        `SELECT recent_activity_id FROM projection.recent_activity
         ORDER BY occurred_at DESC LIMIT 21`,
      ),
    );
    return {
      globalCounts: counts.rows[0],
      probes: {
        groupFinancePositionCountMs: posLatency.ms,
        recentActivityFirstPageMs: activityLatency.ms,
      },
      scaleFixtures: {
        small: { members: 5, financeRecords: 25, status: 'NOT_SEEDED_THIS_RUN' },
        medium: { members: 25, financeRecords: 500, status: 'NOT_SEEDED_THIS_RUN' },
        large: { members: 100, financeRecords: 5000, status: 'NOT_SEEDED_THIS_RUN' },
        note: 'S9-A records fixture intent + current DB size. Controlled Small/Med/Large seeds deferred to S9-G/H measurement once authorized; do not optimize before evidence.',
      },
    };
  } finally {
    client.release();
  }
}

async function main() {
  // Cold + warm /v1/me
  const coldSamples: number[] = [];
  {
    const t0 = performance.now();
    const res = await request(app).get('/v1/me').set(auth);
    coldSamples.push(performance.now() - t0);
    if (res.status !== 200) throw new Error(`cold /v1/me status ${res.status}`);
  }
  await sampleGet('/v1/me', 2); // additional warm
  const me = await sampleGet('/v1/me', 20);
  const meBody = await request(app).get('/v1/me').set(auth);
  const userId = meBody.body.data.userId as string;

  const pulse = await sampleGet('/v1/personal/pulse', 10);
  const activity = await sampleGet('/v1/personal/activity?limit=20', 10);
  const activityNext = await sampleGet(
    `/v1/personal/activity?limit=20${meBody.body.data ? '' : ''}`,
    5,
  );
  // second page if cursor available
  const actFirst = await request(app).get('/v1/personal/activity?limit=20').set(auth);
  const nextCursor = actFirst.body?.data?.nextCursor as string | null | undefined;
  const activityPage2 = nextCursor
    ? await sampleGet(`/v1/personal/activity?limit=20&cursor=${encodeURIComponent(nextCursor)}`, 5)
    : { label: 'activity-page2', n: 0, note: 'no nextCursor (empty/small dataset)' };

  const life = await sampleGet('/v1/personal/life', 10);
  const insights = await sampleGet('/v1/analytics/insights', 5);
  const healthReady = await sampleGet('/health/ready', 10, {});

  // Group / business pulse if inventory exists
  let groupPulse: unknown = { note: 'no group moment for user' };
  let businessPulse: unknown = { note: 'no business moment for user' };
  const groupMoments = meBody.body.data?.activeMoments?.group as Array<{ momentId: string }> | undefined;
  const businessMoments = meBody.body.data?.activeMoments?.business as Array<{ momentId: string }> | undefined;
  if (groupMoments?.[0]?.momentId) {
    groupPulse = await sampleGet(`/v1/group/moments/${groupMoments[0].momentId}/pulse`, 5);
  }
  if (businessMoments?.[0]?.momentId) {
    businessPulse = await sampleGet(`/v1/business/moments/${businessMoments[0].momentId}/pulse`, 5);
  }

  const decomposition = await decomposeMe(userId);
  // Repeat waterfall stages 5x for stable stage p50
  const stageRuns: Record<string, number[]> = {};
  for (let i = 0; i < 5; i++) {
    const d = await decomposeMe(userId);
    for (const [k, v] of Object.entries(d.stagesMs)) {
      (stageRuns[k] ??= []).push(v);
    }
  }
  const stageSummaries = Object.fromEntries(
    Object.entries(stageRuns).map(([k, samples]) => [k, summarize(k, samples)]),
  );

  const scale = await scaleProbe();

  const report = {
    capturedAt: new Date().toISOString(),
    env: {
      nodeEnv: process.env.NODE_ENV ?? null,
      allowDevAuth: true,
      dbPoolMax: config.database.poolMax,
      dbPoolIdleMs: config.database.poolIdleMs,
      dbStatementTimeoutMs: config.database.statementTimeoutMs,
    },
    userId,
    coldMeMs: Math.round(coldSamples[0]! * 100) / 100,
    endpoints: {
      me,
      pulse,
      activity,
      activityRepeat: activityNext,
      activityPage2,
      life,
      insights,
      healthReady,
      groupPulse,
      businessPulse,
    },
    meWaterfall: {
      singleRun: decomposition,
      stageP50Over5: stageSummaries,
    },
    scale,
    methodologyNotes: [
      'Dev-auth in-process via supertest; includes Node + remote Postgres RTT.',
      'Does NOT include production Firebase token verify cost or native render.',
      'Auth path always runs provisionUserProfile upsert (separate pool query) before handler.',
      'getMeBootstrap: 6 sequential SQL; no Redis; no Promise.all.',
    ],
  };

  console.log(JSON.stringify(report, null, 2));
  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
