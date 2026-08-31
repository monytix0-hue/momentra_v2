/**
 * Deterministic analytics engine (S8-E).
 * Computes metrics in TypeScript/SQL — FastAPI is optional for narrative only.
 */
import type { PoolClient } from 'pg';
import { createHash } from 'crypto';
import { hasActiveConsent } from './consent';
import {
  ANALYTICS_CONTRACT_VERSION,
  callNarrativeCompute,
  type AuthorizedFact,
} from '../../platform/ai/fastapi-client';
import { cacheGetJson, cacheSetJson, logCacheStaleServed } from '../../platform/redis/client';
import {
  ANALYTICS_INSIGHT_TTL_SEC,
  analyticsInsightCacheKey,
} from '../../platform/redis/key-contract';

export type StaleMeta = {
  computedAt: string | null;
  dataThrough: string | null;
  status: 'READY' | 'STALE' | 'UNAVAILABLE' | 'PENDING' | 'EMPTY';
  version: string | null;
};

export type MetricRead = {
  metricCode: string;
  numericValue: number | null;
  textValue: string | null;
  scopeType: string;
  scopeId: string;
} & StaleMeta;

export type InsightRead = {
  insightId: string;
  source: 'DETERMINISTIC' | 'AI';
  insightCode: string;
  title: string;
  body: string | null;
  severity: string | null;
} & StaleMeta;

function dedupeKey(parts: Array<string | null | undefined>): string {
  return parts.map((p) => p ?? '').join('|');
}

function hashDedupe(key: string): string {
  return createHash('sha256').update(key).digest('hex').slice(0, 32);
}

async function resolveMetric(
  client: PoolClient,
  code: string,
): Promise<{ metricDefinitionId: string; metricVersionId: string; versionNumber: number } | null> {
  const row = await client.query<{
    metric_definition_id: string;
    metric_version_id: string;
    version_number: number;
  }>(
    `SELECT d.metric_definition_id, v.metric_version_id, v.version_number
     FROM analytics.metric_definition d
     JOIN analytics.metric_version v ON v.metric_definition_id = d.metric_definition_id
     WHERE d.code = $1 AND d.status = 'ACTIVE'
       AND v.status IN ('ACTIVE', 'DRAFT')
     ORDER BY CASE v.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, v.version_number DESC
     LIMIT 1`,
    [code],
  );
  if (!row.rowCount) return null;
  const r = row.rows[0];
  return {
    metricDefinitionId: r.metric_definition_id,
    metricVersionId: r.metric_version_id,
    versionNumber: r.version_number,
  };
}

async function upsertNumericMetric(
  client: PoolClient,
  args: {
    metricCode: string;
    scopeType: string;
    scopeId: string;
    numericValue: number;
    evidenceCount: number;
    triggerEventId?: string | null;
    timeWindow: string;
    sourceVersion: string;
    userId: string;
  },
): Promise<boolean> {
  const meta = await resolveMetric(client, args.metricCode);
  if (!meta) return false;

  const key = dedupeKey([
    args.userId,
    args.scopeType,
    args.scopeId,
    args.metricCode,
    args.timeWindow,
    args.sourceVersion,
  ]);
  const keyHash = hashDedupe(key);

  // Idempotency: skip if identical observation recently written for same dedupe in input_snapshot
  const existing = await client.query(
    `SELECT 1 FROM analytics.calculation_run
     WHERE metric_definition_id = $1 AND scope_type = $2 AND scope_id = $3
       AND status = 'SUCCEEDED'
       AND input_snapshot->>'dedupeKey' = $4
     LIMIT 1`,
    [meta.metricDefinitionId, args.scopeType, args.scopeId, keyHash],
  );
  if (existing.rowCount) return false;

  const run = await client.query<{ calculation_run_id: string }>(
    `INSERT INTO analytics.calculation_run (
       metric_definition_id, metric_version_id, scope_type, scope_id,
       trigger_type, trigger_event_id, status, input_snapshot, started_at, completed_at
     ) VALUES ($1,$2,$3,$4,'EVENT',$5,'SUCCEEDED',$6::jsonb,now(),now())
     RETURNING calculation_run_id`,
    [
      meta.metricDefinitionId,
      meta.metricVersionId,
      args.scopeType,
      args.scopeId,
      args.triggerEventId ?? null,
      JSON.stringify({
        dedupeKey: keyHash,
        timeWindow: args.timeWindow,
        sourceVersion: args.sourceVersion,
        contractVersion: ANALYTICS_CONTRACT_VERSION,
      }),
    ],
  );
  const runId = run.rows[0].calculation_run_id;

  const obs = await client.query<{ metric_observation_id: string; observed_at: Date }>(
    `INSERT INTO analytics.metric_observation (
       metric_definition_id, metric_version_id, calculation_run_id,
       scope_type, scope_id, observed_at, numeric_value, evidence_count
     ) VALUES ($1,$2,$3,$4,$5,now(),$6,$7)
     RETURNING metric_observation_id, observed_at`,
    [
      meta.metricDefinitionId,
      meta.metricVersionId,
      runId,
      args.scopeType,
      args.scopeId,
      args.numericValue,
      args.evidenceCount,
    ],
  );
  const obsId = obs.rows[0].metric_observation_id;
  const observedAt = obs.rows[0].observed_at;

  await client.query(
    `INSERT INTO analytics.metric_current (
       metric_definition_id, metric_version_id, metric_observation_id,
       scope_type, scope_id, observed_at, numeric_value, updated_at
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,now())
     ON CONFLICT (metric_definition_id, scope_type, scope_id) DO UPDATE SET
       metric_version_id = EXCLUDED.metric_version_id,
       metric_observation_id = EXCLUDED.metric_observation_id,
       observed_at = EXCLUDED.observed_at,
       numeric_value = EXCLUDED.numeric_value,
       boolean_value = NULL,
       text_value = NULL,
       category_value = NULL,
       json_value = NULL,
       updated_at = now()`,
    [
      meta.metricDefinitionId,
      meta.metricVersionId,
      obsId,
      args.scopeType,
      args.scopeId,
      observedAt,
      args.numericValue,
    ],
  );
  return true;
}

async function insertDeterministicInsight(
  client: PoolClient,
  args: {
    scopeType: string;
    scopeId: string;
    insightCode: string;
    title: string;
    body: string;
    severity: string;
    dedupeKey: string;
  },
): Promise<void> {
  await client.query(
    `INSERT INTO analytics.deterministic_insight (
       scope_type, scope_id, insight_code, title, body, severity, status, generated_at
     ) VALUES ($1,$2,$3,$4,$5,$6,'ACTIVE',now())
     ON CONFLICT DO NOTHING`,
    [args.scopeType, args.scopeId, args.insightCode, args.title, args.body, args.severity],
  ).catch(async () => {
    // No unique constraint on insight — soft dedupe via recent identical code
    const recent = await client.query(
      `SELECT 1 FROM analytics.deterministic_insight
       WHERE scope_type = $1 AND scope_id = $2 AND insight_code = $3
         AND status = 'ACTIVE' AND generated_at > now() - interval '1 hour'
       LIMIT 1`,
      [args.scopeType, args.scopeId, args.insightCode],
    );
    if (recent.rowCount) return;
    await client.query(
      `INSERT INTO analytics.deterministic_insight (
         scope_type, scope_id, insight_code, title, body, severity, status, generated_at
       ) VALUES ($1,$2,$3,$4,$5,$6,'ACTIVE',now())`,
      [args.scopeType, args.scopeId, args.insightCode, args.title, args.body, args.severity],
    );
  });
}

async function personalSpendMom(
  client: PoolClient,
  userId: string,
  momentId: string | null,
): Promise<{ spend: number; momPct: number | null; currency: string; evidence: number }> {
  const rows = await client.query<{ total: string; currency_code: string; bucket: string }>(
    `SELECT COALESCE(SUM(e.amount),0)::text AS total,
            e.currency_code,
            CASE WHEN e.effective_at >= now() - interval '30 days' THEN 'curr' ELSE 'prev' END AS bucket
     FROM finance.expense e
     JOIN finance.personal_expense_context pec ON pec.expense_id = e.expense_id
     WHERE pec.user_id = $1
       AND ($2::uuid IS NULL OR e.moment_id = $2::uuid)
       AND e.status = 'POSTED'
       AND e.effective_at >= now() - interval '60 days'
     GROUP BY e.currency_code, bucket`,
    [userId, momentId],
  );

  let curr = 0;
  let prev = 0;
  let currency = 'INR';
  let evidence = 0;
  for (const r of rows.rows) {
    const n = Number(r.total) || 0;
    currency = r.currency_code || currency;
    evidence += n > 0 ? 1 : 0;
    if (r.bucket === 'curr') curr += n;
    else prev += n;
  }
  const momPct = prev > 0 ? ((curr - prev) / prev) * 100 : null;
  return { spend: curr, momPct, currency, evidence: evidence || (curr > 0 ? 1 : 0) };
}

async function groupContributionPct(
  client: PoolClient,
  momentId: string,
): Promise<{ pct: number; evidence: number } | null> {
  const row = await client.query<{ paid: string; allocated: string }>(
    `SELECT COALESCE(SUM(paid_total),0)::text AS paid,
            COALESCE(SUM(allocated_total),0)::text AS allocated
     FROM projection.group_finance_position
     WHERE moment_id = $1`,
    [momentId],
  ).catch(() => null);
  if (!row?.rowCount) {
    const snap = await client.query<{ contribution_total: string; expense_total: string }>(
      `SELECT contribution_total::text, expense_total::text
       FROM projection.group_finance_snapshot
       WHERE moment_id = $1
       LIMIT 1`,
      [momentId],
    ).catch(() => null);
    if (!snap?.rowCount) return null;
    const contribution = Number(snap.rows[0].contribution_total) || 0;
    const expense = Number(snap.rows[0].expense_total) || 0;
    if (expense <= 0) return { pct: 0, evidence: 0 };
    return { pct: Math.min(100, (contribution / expense) * 100), evidence: 1 };
  }
  const paid = Number(row.rows[0].paid) || 0;
  const allocated = Number(row.rows[0].allocated) || 0;
  if (allocated <= 0) return { pct: 0, evidence: 0 };
  return { pct: Math.min(100, (paid / allocated) * 100), evidence: 1 };
}

async function businessBurnAndRunway(
  client: PoolClient,
  companyId: string,
  momentId: string | null,
): Promise<{ burn: number; runway: number | null; currency: string; evidence: number }> {
  const burnRow = await client.query<{ total: string; currency_code: string }>(
    `SELECT COALESCE(SUM(e.amount),0)::text AS total, e.currency_code
     FROM finance.expense e
     JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
     WHERE bec.company_id = $1
       AND ($2::uuid IS NULL OR e.moment_id = $2::uuid)
       AND e.status = 'POSTED'
       AND e.effective_at >= now() - interval '30 days'
     GROUP BY e.currency_code
     ORDER BY SUM(e.amount) DESC
     LIMIT 1`,
    [companyId, momentId],
  );

  let burn = 0;
  let currency = 'INR';
  let evidence = 0;
  if (burnRow.rowCount) {
    burn = Number(burnRow.rows[0].total) || 0;
    currency = burnRow.rows[0].currency_code || 'INR';
    evidence = burn > 0 ? 1 : 0;
  }

  const cashRow = await client.query<{ buffer: string }>(
    `SELECT COALESCE(
       (preferences->>'availableCash')::numeric,
       (preferences->>'operatingCashBuffer')::numeric,
       0
     )::text AS buffer
     FROM business.business_system_setup
     WHERE company_id = $1 AND status = 'ACTIVE'
     ORDER BY updated_at DESC NULLS LAST
     LIMIT 1`,
    [companyId],
  ).catch(() => null);

  let runway: number | null = null;
  if (cashRow?.rowCount && burn > 0) {
    const buffer = Number(cashRow.rows[0].buffer) || 0;
    if (buffer > 0) {
      runway = Math.round((buffer / burn) * 10) / 10;
      evidence += 1;
    }
  }
  return { burn, runway, currency, evidence };
}

export type AnalyticsJobScope = {
  userId: string;
  context: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  companyId?: string | null;
  momentId?: string | null;
  triggerEventId?: string | null;
  correlationId?: string | null;
  timeWindow?: string;
  sourceVersion?: string;
};

/**
 * Run DET metrics (+ optional FastAPI narrative). Consent re-checked at execute time.
 */
export async function runAnalyticsJob(client: PoolClient, job: AnalyticsJobScope): Promise<{
  metricsWritten: number;
  narrative: boolean;
  skippedReason?: string;
}> {
  const timeWindow = job.timeWindow ?? 'P30D';
  const sourceVersion = job.sourceVersion ?? '1';
  let metricsWritten = 0;
  const facts: AuthorizedFact[] = [];
  let currency: string | undefined;

  if (job.context === 'PERSONAL') {
    const ok = await hasActiveConsent(client, job.userId, 'PERSONAL_ANALYTICS');
    if (!ok) return { metricsWritten: 0, narrative: false, skippedReason: 'CONSENT_PERSONAL_ANALYTICS' };

    const spend = await personalSpendMom(client, job.userId, job.momentId ?? null);
    currency = spend.currency;
    const scopeType = job.momentId ? 'MOMENT' : 'USER';
    const scopeId = job.momentId ?? job.userId;

    if (spend.evidence > 0) {
      // Use BUDGET_UTILIZATION as spend-proxy numeric when budget unknown — store MoM in json via text metric alternate
      const wrote = await upsertNumericMetric(client, {
        metricCode: 'BUDGET_UTILIZATION',
        scopeType,
        scopeId,
        numericValue: spend.spend,
        evidenceCount: spend.evidence,
        triggerEventId: job.triggerEventId,
        timeWindow,
        sourceVersion,
        userId: job.userId,
      });
      if (wrote) metricsWritten += 1;
      facts.push({ code: 'spendTotal', value: spend.spend, unit: spend.currency });
      if (spend.momPct != null) facts.push({ code: 'momChangePct', value: spend.momPct });

      await insertDeterministicInsight(client, {
        scopeType,
        scopeId,
        insightCode: 'SPEND_WINDOW_FACT',
        title: 'Spending this window',
        body: `Recorded spend is ${spend.spend} ${spend.currency} for ${timeWindow}.`,
        severity: 'INFO',
        dedupeKey: hashDedupe(dedupeKey([job.userId, scopeId, 'SPEND_WINDOW_FACT', timeWindow])),
      });
    }
  } else if (job.context === 'GROUP' && job.momentId) {
    const ok = await hasActiveConsent(client, job.userId, 'PERSONAL_ANALYTICS');
    if (!ok) return { metricsWritten: 0, narrative: false, skippedReason: 'CONSENT_PERSONAL_ANALYTICS' };

    const contrib = await groupContributionPct(client, job.momentId);
    if (contrib) {
      const wrote = await upsertNumericMetric(client, {
        metricCode: 'GROUP_CONTRIBUTION_COMPLETION',
        scopeType: 'MOMENT',
        scopeId: job.momentId,
        numericValue: contrib.pct,
        evidenceCount: contrib.evidence,
        triggerEventId: job.triggerEventId,
        timeWindow,
        sourceVersion,
        userId: job.userId,
      });
      if (wrote) metricsWritten += 1;
      facts.push({ code: 'contributionPct', value: contrib.pct });
      await insertDeterministicInsight(client, {
        scopeType: 'MOMENT',
        scopeId: job.momentId,
        insightCode: 'GROUP_CONTRIBUTION_FACT',
        title: 'Contribution completion',
        body: `Group contribution completion is about ${contrib.pct.toFixed(1)}%.`,
        severity: 'INFO',
        dedupeKey: hashDedupe(dedupeKey([job.momentId, 'GROUP_CONTRIBUTION_FACT', timeWindow])),
      });
    }
  } else if (job.context === 'BUSINESS' && job.companyId) {
    const ok = await hasActiveConsent(client, job.userId, 'BUSINESS_ANALYTICS');
    if (!ok) return { metricsWritten: 0, narrative: false, skippedReason: 'CONSENT_BUSINESS_ANALYTICS' };

    const br = await businessBurnAndRunway(client, job.companyId, job.momentId ?? null);
    currency = br.currency;
    const scopeType = job.momentId ? 'MOMENT' : 'COMPANY';
    const scopeId = job.momentId ?? job.companyId;

    if (br.evidence > 0) {
      const wroteBurn = await upsertNumericMetric(client, {
        metricCode: 'BUSINESS_BURN_RATE',
        scopeType,
        scopeId,
        numericValue: br.burn,
        evidenceCount: br.evidence,
        triggerEventId: job.triggerEventId,
        timeWindow,
        sourceVersion,
        userId: job.userId,
      });
      if (wroteBurn) metricsWritten += 1;
      facts.push({ code: 'burnRate', value: br.burn, unit: br.currency });

      if (br.runway != null) {
        const wroteRunway = await upsertNumericMetric(client, {
          metricCode: 'BUSINESS_RUNWAY_MONTHS',
          scopeType,
          scopeId,
          numericValue: br.runway,
          evidenceCount: br.evidence,
          triggerEventId: job.triggerEventId,
          timeWindow,
          sourceVersion,
          userId: job.userId,
        });
        if (wroteRunway) metricsWritten += 1;
        facts.push({ code: 'runwayMonths', value: br.runway });

        // Projection must source runway from metric_current — best-effort update
        if (job.companyId) {
          await client.query(
            `UPDATE projection.business_pulse
             SET runway_months = $2, updated_at = now()
             WHERE company_id = $1`,
            [job.companyId, br.runway],
          ).catch(() => undefined);
        }

        await insertDeterministicInsight(client, {
          scopeType,
          scopeId,
          insightCode: 'BUSINESS_RUNWAY_FACT',
          title: 'Runway',
          body: `Approximate runway is ${br.runway} months at current burn.`,
          severity: br.runway < 3 ? 'HIGH' : 'INFO',
          dedupeKey: hashDedupe(dedupeKey([scopeId, 'BUSINESS_RUNWAY_FACT', timeWindow])),
        });
      }
    }
  }

  // Optional narrative — requires AI consent at execute time; FastAPI optional
  let narrative = false;
  if (facts.length) {
    const aiOk = await hasActiveConsent(client, job.userId, 'AI_INSIGHT_GENERATION');
    if (!aiOk) {
      return { metricsWritten, narrative: false, skippedReason: 'CONSENT_AI_INSIGHT_GENERATION' };
    }

    const scopeType =
      job.momentId ? 'MOMENT' : job.companyId && job.context === 'BUSINESS' ? 'COMPANY' : 'USER';
    const scopeId = job.momentId ?? job.companyId ?? job.userId;

    const remote = await callNarrativeCompute({
      contractVersion: ANALYTICS_CONTRACT_VERSION,
      userId: job.userId,
      context: job.context,
      companyId: job.companyId,
      momentId: job.momentId,
      currency,
      timeWindow,
      sourceVersion,
      authorizedFacts: facts,
      correlationId: job.correlationId,
      purpose: 'narrative',
    });

    const title = remote?.title ?? 'Insight';
    const body =
      remote?.body ??
      facts.map((f) => `${f.code}=${String(f.value)}`).join('; ');
    const insightCode = remote?.insightCode ?? 'TEMPLATE_SUMMARY';
    const now = new Date().toISOString();

    await insertDeterministicInsight(client, {
      scopeType,
      scopeId,
      insightCode,
      title,
      body,
      severity: remote?.severity ?? 'INFO',
      dedupeKey: hashDedupe(dedupeKey([scopeId, insightCode, timeWindow])),
    });
    narrative = true;

    const cacheKey = analyticsInsightCacheKey(job.userId, scopeType, scopeId);
    await cacheSetJson(
      cacheKey,
      {
        computedAt: now,
        dataThrough: now,
        status: 'READY',
        version: sourceVersion,
        title,
        body,
        provider: remote?.provider ?? 'template-local',
      },
      ANALYTICS_INSIGHT_TTL_SEC,
    );
  }

  return { metricsWritten, narrative };
}

export async function listMetricsForScope(
  client: PoolClient,
  scopeType: string,
  scopeId: string,
): Promise<MetricRead[]> {
  const rows = await client.query<{
    code: string;
    numeric_value: string | null;
    text_value: string | null;
    observed_at: Date;
    version_number: number;
  }>(
    `SELECT d.code, c.numeric_value::text, c.text_value, c.observed_at, v.version_number
     FROM analytics.metric_current c
     JOIN analytics.metric_definition d ON d.metric_definition_id = c.metric_definition_id
     JOIN analytics.metric_version v ON v.metric_version_id = c.metric_version_id
     WHERE c.scope_type = $1 AND c.scope_id = $2
     ORDER BY d.code`,
    [scopeType, scopeId],
  );
  const now = Date.now();
  return rows.rows.map((r) => {
    const ageMs = now - new Date(r.observed_at).getTime();
    const status: StaleMeta['status'] = ageMs > 24 * 3600_000 ? 'STALE' : 'READY';
    return {
      metricCode: r.code,
      numericValue: r.numeric_value != null ? Number(r.numeric_value) : null,
      textValue: r.text_value,
      scopeType,
      scopeId,
      computedAt: r.observed_at.toISOString(),
      dataThrough: r.observed_at.toISOString(),
      status,
      version: String(r.version_number),
    };
  });
}

export async function listInsightsForScope(
  client: PoolClient,
  scopeType: string,
  scopeId: string,
  userId: string,
): Promise<InsightRead[]> {
  const cacheKey = analyticsInsightCacheKey(userId, scopeType, scopeId);
  const cached = await cacheGetJson<{
    computedAt: string;
    dataThrough: string;
    status: string;
    version: string;
    title: string;
    body: string;
  }>(cacheKey);

  const det = await client.query<{
    deterministic_insight_id: string;
    insight_code: string;
    title: string;
    body: string | null;
    severity: string;
    generated_at: Date;
  }>(
    `SELECT deterministic_insight_id, insight_code, title, body, severity, generated_at
     FROM analytics.deterministic_insight
     WHERE scope_type = $1 AND scope_id = $2 AND status = 'ACTIVE'
     ORDER BY generated_at DESC
     LIMIT 20`,
    [scopeType, scopeId],
  );

  const ai = await client.query<{
    ai_insight_id: string;
    insight_code: string;
    title: string;
    body: string | null;
    created_at: Date;
  }>(
    `SELECT ai_insight_id, insight_code, title, body, created_at
     FROM ai.ai_insight
     WHERE scope_type = $1 AND scope_id = $2 AND status = 'ACTIVE'
     ORDER BY created_at DESC
     LIMIT 20`,
    [scopeType, scopeId],
  ).catch(() => ({ rows: [] as Array<{
    ai_insight_id: string;
    insight_code: string;
    title: string;
    body: string | null;
    created_at: Date;
  }> }));

  const out: InsightRead[] = [
    ...det.rows.map((r) => ({
      insightId: r.deterministic_insight_id,
      source: 'DETERMINISTIC' as const,
      insightCode: r.insight_code,
      title: r.title,
      body: r.body,
      severity: r.severity,
      computedAt: r.generated_at.toISOString(),
      dataThrough: r.generated_at.toISOString(),
      status: 'READY' as const,
      version: '1',
    })),
    ...ai.rows.map((r) => ({
      insightId: r.ai_insight_id,
      source: 'AI' as const,
      insightCode: r.insight_code,
      title: r.title,
      body: r.body,
      severity: null,
      computedAt: r.created_at.toISOString(),
      dataThrough: r.created_at.toISOString(),
      status: 'READY' as const,
      version: '1',
    })),
  ];

  if (!out.length && cached) {
    logCacheStaleServed(cacheKey);
    return [
      {
        insightId: 'cache',
        source: 'DETERMINISTIC',
        insightCode: 'CACHED',
        title: cached.title,
        body: cached.body,
        severity: 'INFO',
        computedAt: cached.computedAt,
        dataThrough: cached.dataThrough,
        status: 'STALE',
        version: cached.version,
      },
    ];
  }
  if (!out.length) {
    return [];
  }
  return out;
}
