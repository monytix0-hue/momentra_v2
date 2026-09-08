import type { PoolClient } from 'pg';
import { refreshGroupLeanKpis, type GroupLeanKpiRow } from './group-lean-kpis';
import {
  REAL_CREATOR_MOMENT_DAILY_FILTER,
  REAL_CREATOR_MOMENT_DAILY_JOIN,
  realUserExists,
} from './real-users';

export interface FounderLeanKpiRow {
  kpiCode: string;
  periodType: string;
  periodStart: string;
  periodEnd: string;
  numerator: number | null;
  denominator: number | null;
  kpiValue: number | null;
  sampleSize: number | null;
}

export interface FounderLeanRefreshResult {
  founder: FounderLeanKpiRow[];
  group: GroupLeanKpiRow[];
  cohortsUpserted: number;
}

const FOUNDER_DAY_CODES = [
  'KPI_001_NEW_REGISTERED_USERS',
  'KPI_006_USER_ACTIVATION_RATE',
  'KPI_012_MOMENTS_CREATED',
  'KPI_016_MOMENT_ACTIVATION_RATE',
  'KPI_017_MOMENT_COMPLETION_RATE',
  'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
  'KPI_024_D30_MEANINGFUL_RETENTION',
  'KPI_025_SECOND_MOMENT_RATE',
  'KPI_032_INVITE__JOIN_CONVERSION',
  'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
] as const;

/**
 * Materialize Founder Lean KPIs into undomain'd analytics_mart.kpi_period rows
 * (so analytics_mart.v_founder_latest can serve them), plus WAM week history,
 * second-moment cohorts, and Group Lean refresh.
 */
export async function refreshFounderLeanKpis(client: PoolClient): Promise<FounderLeanRefreshResult> {
  const now = new Date();
  const dayStart = startOfUtcDay(now);
  const dayEnd = addDays(dayStart, 1);
  const dayStartStr = isoDate(dayStart);
  const dayEndStr = isoDate(dayEnd);

  const weekStart = startOfUtcIsoWeek(now);
  const weekEnd = addDays(weekStart, 7);
  const monthStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1));
  const monthEnd = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 1));

  const founder: FounderLeanKpiRow[] = [];

  // KPI_001 — New Registered Users (today)
  {
    const r = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n
       FROM analytics_core.user_lifecycle_fact ulf
       WHERE ulf.registered_at >= $1::timestamptz AND ulf.registered_at < $2::timestamptz
         AND ${realUserExists('ulf.user_id')}`,
      [dayStart.toISOString(), dayEnd.toISOString()]
    );
    const n = Number(r.rows[0]?.n ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_001_NEW_REGISTERED_USERS',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: n,
        denominator: null,
        kpiValue: n,
        sampleSize: n,
      })
    );
  }

  // KPI_006 — User Activation Rate (eligible registered >= 14d ago; activated within 14d of register)
  {
    const r = await client.query<{ activated: string; eligible: string }>(
      `SELECT
         COUNT(*) FILTER (
           WHERE activated_at IS NOT NULL
             AND activated_at <= registered_at + INTERVAL '14 days'
         )::text AS activated,
         COUNT(*)::text AS eligible
       FROM analytics_core.user_lifecycle_fact ulf
       WHERE ulf.registered_at <= now() - INTERVAL '14 days'
         AND ${realUserExists('ulf.user_id')}`
    );
    const num = Number(r.rows[0]?.activated ?? 0);
    const den = Number(r.rows[0]?.eligible ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_006_USER_ACTIVATION_RATE',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: den > 0 ? (num * 100) / den : null,
        sampleSize: den,
      })
    );
  }

  // KPI_012 — Moments Created (today)
  {
    const r = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n
       FROM analytics_core.moment_lifecycle_fact mlf
       WHERE mlf.created_at >= $1::timestamptz AND mlf.created_at < $2::timestamptz
         AND ${realUserExists('mlf.creator_user_id')}`,
      [dayStart.toISOString(), dayEnd.toISOString()]
    );
    const n = Number(r.rows[0]?.n ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_012_MOMENTS_CREATED',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: n,
        denominator: null,
        kpiValue: n,
        sampleSize: n,
      })
    );
  }

  // KPI_013 — WAM (current ISO week) + last 12 weeks history
  for (let i = 0; i < 12; i++) {
    const ws = addDays(weekStart, -7 * i);
    const we = addDays(ws, 7);
    const r = await client.query<{ n: string }>(
      `SELECT COUNT(DISTINCT md.moment_id)::text AS n
       FROM analytics_core.moment_daily md
       ${REAL_CREATOR_MOMENT_DAILY_JOIN}
       WHERE md.activity_date >= $1::date
         AND md.activity_date < $2::date
         AND md.meaningfully_active_flag = TRUE
         AND ${REAL_CREATOR_MOMENT_DAILY_FILTER}`,
      [isoDate(ws), isoDate(we)]
    );
    const n = Number(r.rows[0]?.n ?? 0);
    const row = await upsertUndomainKpi(client, {
      kpiCode: 'KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM',
      periodType: 'week',
      periodStart: isoDate(ws),
      periodEnd: isoDate(we),
      numerator: n,
      denominator: null,
      kpiValue: n,
      sampleSize: n,
    });
    if (i === 0) founder.push(row);
  }

  // KPI_014 — MAM (current calendar month)
  {
    const r = await client.query<{ n: string }>(
      `SELECT COUNT(DISTINCT md.moment_id)::text AS n
       FROM analytics_core.moment_daily md
       ${REAL_CREATOR_MOMENT_DAILY_JOIN}
       WHERE md.activity_date >= $1::date
         AND md.activity_date < $2::date
         AND md.meaningfully_active_flag = TRUE
         AND ${REAL_CREATOR_MOMENT_DAILY_FILTER}`,
      [isoDate(monthStart), isoDate(monthEnd)]
    );
    const n = Number(r.rows[0]?.n ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM',
        periodType: 'month',
        periodStart: isoDate(monthStart),
        periodEnd: isoDate(monthEnd),
        numerator: n,
        denominator: null,
        kpiValue: n,
        sampleSize: n,
      })
    );
  }

  // KPI_016 — Moment Activation Rate (created >= 7d ago; activated within 7d)
  {
    const r = await client.query<{ activated: string; eligible: string }>(
      `SELECT
         COUNT(*) FILTER (
           WHERE activated_at IS NOT NULL
             AND activated_at <= created_at + INTERVAL '7 days'
         )::text AS activated,
         COUNT(*)::text AS eligible
       FROM analytics_core.moment_lifecycle_fact mlf
       WHERE mlf.created_at <= now() - INTERVAL '7 days'
         AND mlf.cancelled_at IS NULL
         AND ${realUserExists('mlf.creator_user_id')}`
    );
    const num = Number(r.rows[0]?.activated ?? 0);
    const den = Number(r.rows[0]?.eligible ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_016_MOMENT_ACTIVATION_RATE',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: den > 0 ? (num * 100) / den : null,
        sampleSize: den,
      })
    );
  }

  // KPI_017 — Moment Completion Rate (activated >= 30d ago)
  {
    const r = await client.query<{ completed: string; eligible: string }>(
      `SELECT
         COUNT(*) FILTER (WHERE completed_at IS NOT NULL)::text AS completed,
         COUNT(*)::text AS eligible
       FROM analytics_core.moment_lifecycle_fact mlf
       WHERE mlf.activated_at IS NOT NULL
         AND mlf.activated_at <= now() - INTERVAL '30 days'
         AND mlf.cancelled_at IS NULL
         AND ${realUserExists('mlf.creator_user_id')}`
    );
    const num = Number(r.rows[0]?.completed ?? 0);
    const den = Number(r.rows[0]?.eligible ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_017_MOMENT_COMPLETION_RATE',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: den > 0 ? (num * 100) / den : null,
        sampleSize: den,
      })
    );
  }

  // KPI_018 — Meaningful Actions per Active Moment (today)
  {
    const r = await client.query<{ actions: string; moments: string }>(
      `SELECT
         COALESCE(SUM(md.meaningful_action_count), 0)::text AS actions,
         COUNT(*) FILTER (WHERE md.meaningfully_active_flag)::text AS moments
       FROM analytics_core.moment_daily md
       ${REAL_CREATOR_MOMENT_DAILY_JOIN}
       WHERE md.activity_date = $1::date
         AND ${REAL_CREATOR_MOMENT_DAILY_FILTER}`,
      [dayStartStr]
    );
    const num = Number(r.rows[0]?.actions ?? 0);
    const den = Number(r.rows[0]?.moments ?? 0);
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: den > 0 ? num / den : null,
        sampleSize: den,
      })
    );
  }

  // KPI_024 — D30 Meaningful Retention
  {
    const fromCohort = await client.query<{
      retained: string;
      eligible: string;
      rate: string | null;
    }>(
      `SELECT
         COALESCE(SUM(d30_retained), 0)::text AS retained,
         COALESCE(SUM(d30_eligible), 0)::text AS eligible,
         CASE WHEN SUM(d30_eligible) > 0
           THEN (SUM(d30_retained)::numeric * 100.0 / SUM(d30_eligible))::text
           ELSE NULL END AS rate
       FROM analytics_mart.user_retention_cohort
       WHERE cohort_type = 'activated'`
    );
    let num = Number(fromCohort.rows[0]?.retained ?? 0);
    let den = Number(fromCohort.rows[0]?.eligible ?? 0);
    let value = fromCohort.rows[0]?.rate != null ? Number(fromCohort.rows[0].rate) : null;

    if (den === 0) {
      const computed = await client.query<{ retained: string; eligible: string }>(
        `WITH eligible AS (
           SELECT ulf.user_id, ulf.activated_at
           FROM analytics_core.user_lifecycle_fact ulf
           WHERE ulf.activated_at IS NOT NULL
             AND ulf.activated_at <= now() - INTERVAL '30 days'
             AND ${realUserExists('ulf.user_id')}
         ),
         retained AS (
           SELECT DISTINCT e.user_id
           FROM eligible e
           JOIN analytics_core.user_daily ud ON ud.user_id = e.user_id
           WHERE ud.meaningfully_active_flag = TRUE
             AND ud.activity_date >= (e.activated_at::date + 30)
             AND ud.activity_date <= (e.activated_at::date + 36)
         )
         SELECT
           (SELECT COUNT(*) FROM retained)::text AS retained,
           (SELECT COUNT(*) FROM eligible)::text AS eligible`
      );
      num = Number(computed.rows[0]?.retained ?? 0);
      den = Number(computed.rows[0]?.eligible ?? 0);
      value = den > 0 ? (num * 100) / den : null;
    }

    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_024_D30_MEANINGFUL_RETENTION',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: value,
        sampleSize: den,
      })
    );
  }

  // KPI_025 — Second Moment Rate + cohort table upsert
  const cohortsUpserted = await upsertSecondMomentCohorts(client);
  {
    const r = await client.query<{
      creators: string;
      eligible: string;
      rate: string | null;
    }>(
      `SELECT
         COALESCE(SUM(second_moment_creators), 0)::text AS creators,
         COALESCE(SUM(second_moment_eligible), 0)::text AS eligible,
         CASE WHEN SUM(second_moment_eligible) > 0
           THEN (SUM(second_moment_creators)::numeric * 100.0 / SUM(second_moment_eligible))::text
           ELSE NULL END AS rate
       FROM analytics_mart.moment_repeat_cohort`
    );
    let num = Number(r.rows[0]?.creators ?? 0);
    let den = Number(r.rows[0]?.eligible ?? 0);
    let value = r.rows[0]?.rate != null ? Number(r.rows[0].rate) : null;

    if (den === 0) {
      const computed = await client.query<{ creators: string; eligible: string }>(
        `SELECT
           COUNT(*) FILTER (
             WHERE second_moment_created_at IS NOT NULL
               AND second_moment_created_at <= first_moment_created_at + INTERVAL '60 days'
           )::text AS creators,
           COUNT(*)::text AS eligible
         FROM analytics_core.user_lifecycle_fact ulf
         WHERE ulf.first_moment_created_at IS NOT NULL
           AND ulf.first_moment_created_at <= now() - INTERVAL '60 days'
           AND ${realUserExists('ulf.user_id')}`
      );
      num = Number(computed.rows[0]?.creators ?? 0);
      den = Number(computed.rows[0]?.eligible ?? 0);
      value = den > 0 ? (num * 100) / den : null;
    }

    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: 'KPI_025_SECOND_MOMENT_RATE',
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: num,
        denominator: den,
        kpiValue: value,
        sampleSize: den,
      })
    );
  }

  // KPI_032 / KPI_034 — reuse group views into undomain'd founder slice
  for (const { code, view } of [
    { code: 'KPI_032_INVITE__JOIN_CONVERSION', view: 'analytics_mart.v_kpi_032_invite_join_conversion' },
    { code: 'KPI_034_PARTICIPANT__CREATOR_CONVERSION', view: 'analytics_mart.v_kpi_034_participant_creator_conversion' },
  ]) {
    const r = await client.query<{
      kpi_value: string | null;
      numerator: string | null;
      denominator: string | null;
      sample_size: string | null;
    }>(`SELECT kpi_value::text, numerator::text, denominator::text, sample_size::text FROM ${view}`);
    const row = r.rows[0];
    founder.push(
      await upsertUndomainKpi(client, {
        kpiCode: code,
        periodType: 'day',
        periodStart: dayStartStr,
        periodEnd: dayEndStr,
        numerator: row?.numerator != null ? Number(row.numerator) : null,
        denominator: row?.denominator != null ? Number(row.denominator) : null,
        kpiValue: row?.kpi_value != null ? Number(row.kpi_value) : null,
        sampleSize: row?.sample_size != null ? Number(row.sample_size) : null,
      })
    );
  }

  // Extra Product/VC supporting KPIs (same undomain slice for product/vc tabs)
  await materializeSupportingKpis(client, dayStartStr, dayEndStr, monthStart, monthEnd);

  const group = await refreshGroupLeanKpis(client, { periodStart: dayStart, periodEnd: dayEnd });

  // Ensure founder list includes only the canonical 12 cards in pack order
  const byCode = new Map(founder.map((f) => [f.kpiCode, f]));
  const ordered: FounderLeanKpiRow[] = [];
  for (const code of [
    'KPI_001_NEW_REGISTERED_USERS',
    'KPI_006_USER_ACTIVATION_RATE',
    'KPI_012_MOMENTS_CREATED',
    'KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM',
    'KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM',
    'KPI_016_MOMENT_ACTIVATION_RATE',
    'KPI_017_MOMENT_COMPLETION_RATE',
    'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
    'KPI_024_D30_MEANINGFUL_RETENTION',
    'KPI_025_SECOND_MOMENT_RATE',
    'KPI_032_INVITE__JOIN_CONVERSION',
    'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
  ]) {
    const row = byCode.get(code);
    if (row) ordered.push(row);
  }

  void FOUNDER_DAY_CODES;
  return { founder: ordered, group, cohortsUpserted };
}

async function materializeSupportingKpis(
  client: PoolClient,
  dayStartStr: string,
  dayEndStr: string,
  monthStart: Date,
  monthEnd: Date
): Promise<void> {
  // KPI_015 — Moments per Active User (month)
  {
    const u = await client.query<{ pairs: string; users: string }>(
      `WITH active_users AS (
         SELECT DISTINCT ud.user_id
         FROM analytics_core.user_daily ud
         WHERE ud.activity_date >= $1::date
           AND ud.activity_date < $2::date
           AND ud.meaningfully_active_flag = TRUE
           AND ${realUserExists('ud.user_id')}
       ),
       pairs AS (
         SELECT DISTINCT e.moment_id, e.user_id
         FROM analytics_raw.events e
         LEFT JOIN analytics_core.activity_type_registry a
           ON a.activity_type = e.properties->>'activity_type'
          AND a.is_meaningful
          AND a.active_from <= e.occurred_at
          AND (a.active_to IS NULL OR a.active_to > e.occurred_at)
         WHERE e.is_valid
           AND e.user_id IS NOT NULL
           AND e.moment_id IS NOT NULL
           AND e.occurred_at >= $1::timestamptz
           AND e.occurred_at < $2::timestamptz
           AND ${realUserExists('e.user_id')}
           AND (
             e.event_name IN (
               'moment_created',
               'participant_invited',
               'participant_joined',
               'expense_added',
               'contribution_recorded',
               'split_created',
               'memory_created',
               'moment_completed'
             )
             OR (
               e.event_name = 'moment_activity_completed'
               AND a.activity_type IS NOT NULL
             )
           )
       )
       SELECT
         (SELECT COUNT(*)::text FROM pairs) AS pairs,
         (SELECT COUNT(*)::text FROM active_users) AS users`,
      [monthStart.toISOString(), monthEnd.toISOString()]
    );
    const num = Number(u.rows[0]?.pairs ?? 0);
    const den = Number(u.rows[0]?.users ?? 0);
    await upsertUndomainKpi(client, {
      kpiCode: 'KPI_015_MOMENTS_PER_ACTIVE_USER',
      periodType: 'month',
      periodStart: isoDate(monthStart),
      periodEnd: isoDate(monthEnd),
      numerator: num,
      denominator: den,
      kpiValue: den > 0 ? num / den : null,
      sampleSize: den,
    });
  }

  // KPI_009 / 010 placeholders from events if present
  {
    const started = await client.query<{ n: string }>(
      `SELECT COUNT(DISTINCT properties->>'creation_flow_id')::text AS n
       FROM analytics_raw.events
       WHERE event_name = 'moment_creation_started' AND is_valid
         AND occurred_at >= $1::date AND occurred_at < $2::date`,
      [dayStartStr, dayEndStr]
    );
    const created = await client.query<{ n: string }>(
      `SELECT COUNT(DISTINCT COALESCE(properties->>'creation_flow_id', event_id::text))::text AS n
       FROM analytics_raw.events
       WHERE event_name = 'moment_created' AND is_valid
         AND occurred_at >= $1::date AND occurred_at < $2::date`,
      [dayStartStr, dayEndStr]
    );
    const s = Number(started.rows[0]?.n ?? 0);
    const c = Number(created.rows[0]?.n ?? 0);
    await upsertUndomainKpi(client, {
      kpiCode: 'KPI_009_MOMENT_CREATION_COMPLETION_RATE',
      periodType: 'day',
      periodStart: dayStartStr,
      periodEnd: dayEndStr,
      numerator: c,
      denominator: s,
      kpiValue: s > 0 ? (c * 100) / s : null,
      sampleSize: s,
    });
  }

  // KPI_036 — Quick Add Completion Rate (join started→outcome on quick_add_flow_id)
  {
    const r = await client.query<{ started: string; completed: string }>(
      `WITH s AS (
         SELECT DISTINCT properties->>'quick_add_flow_id' AS flow_id
         FROM analytics_raw.events
         WHERE event_name = 'quick_add_started'
           AND is_valid
           AND properties->>'quick_add_flow_id' IS NOT NULL
           AND occurred_at >= $1::date
           AND occurred_at < $2::date
       ),
       c AS (
         SELECT DISTINCT properties->>'quick_add_flow_id' AS flow_id
         FROM analytics_raw.events
         WHERE is_valid
           AND properties->>'quick_add_flow_id' IS NOT NULL
           AND event_name IN ('expense_added', 'contribution_recorded', 'moment_activity_completed')
       )
       SELECT
         COUNT(s.flow_id)::text AS started,
         COUNT(c.flow_id)::text AS completed
       FROM s
       LEFT JOIN c USING (flow_id)`,
      [dayStartStr, dayEndStr]
    );
    const s = Number(r.rows[0]?.started ?? 0);
    const c = Number(r.rows[0]?.completed ?? 0);
    await upsertUndomainKpi(client, {
      kpiCode: 'KPI_036_QUICK_ADD_COMPLETION_RATE',
      periodType: 'day',
      periodStart: dayStartStr,
      periodEnd: dayEndStr,
      numerator: c,
      denominator: s,
      kpiValue: s > 0 ? (c * 100) / s : null,
      sampleSize: s,
    });
  }
}

async function upsertSecondMomentCohorts(client: PoolClient): Promise<number> {
  const rows = await client.query<{
    cohort_month: string;
    first_time_creators: string;
    second_moment_eligible: string;
    second_moment_creators: string;
  }>(
    `SELECT
       date_trunc('month', ulf.first_moment_created_at)::date::text AS cohort_month,
       COUNT(*)::text AS first_time_creators,
       COUNT(*) FILTER (WHERE first_moment_created_at <= now() - INTERVAL '60 days')::text AS second_moment_eligible,
       COUNT(*) FILTER (
         WHERE first_moment_created_at <= now() - INTERVAL '60 days'
           AND second_moment_created_at IS NOT NULL
           AND second_moment_created_at <= first_moment_created_at + INTERVAL '60 days'
       )::text AS second_moment_creators
     FROM analytics_core.user_lifecycle_fact ulf
     WHERE ulf.first_moment_created_at IS NOT NULL
       AND ${realUserExists('ulf.user_id')}
     GROUP BY 1
     ORDER BY 1`
  );

  let n = 0;
  for (const row of rows.rows) {
    const eligible = Number(row.second_moment_eligible);
    const creators = Number(row.second_moment_creators);
    const rate = eligible > 0 ? (creators * 100) / eligible : null;
    await client.query(
      `INSERT INTO analytics_mart.moment_repeat_cohort (
         cohort_month, first_time_creators, second_moment_eligible, second_moment_creators,
         second_moment_rate, formula_version, calculated_at
       ) VALUES ($1::date, $2, $3, $4, $5, 1, now())
       ON CONFLICT (cohort_month, formula_version) DO UPDATE SET
         first_time_creators = EXCLUDED.first_time_creators,
         second_moment_eligible = EXCLUDED.second_moment_eligible,
         second_moment_creators = EXCLUDED.second_moment_creators,
         second_moment_rate = EXCLUDED.second_moment_rate,
         calculated_at = now()`,
      [row.cohort_month, Number(row.first_time_creators), eligible, creators, rate]
    );
    n += 1;
  }

  // Drop cohorts left behind by earlier refreshes (e.g. a month whose only
  // creators turned out to be dev/placeholder accounts).
  await client.query(
    `DELETE FROM analytics_mart.moment_repeat_cohort
     WHERE formula_version = 1
       AND NOT (cohort_month = ANY($1::date[]))`,
    [rows.rows.map((r) => r.cohort_month)]
  );

  return n;
}

async function upsertUndomainKpi(
  client: PoolClient,
  row: {
    kpiCode: string;
    periodType: string;
    periodStart: string;
    periodEnd: string;
    numerator: number | null;
    denominator: number | null;
    kpiValue: number | null;
    sampleSize: number | null;
  }
): Promise<FounderLeanKpiRow> {
  await client.query(
    `DELETE FROM analytics_mart.kpi_period
     WHERE kpi_code = $1
       AND period_type = $2
       AND period_start = $3::date
       AND period_end = $4::date
       AND formula_version = 1
       AND moment_domain IS NULL
       AND moment_category IS NULL
       AND moment_type IS NULL
       AND acquisition_source IS NULL`,
    [row.kpiCode, row.periodType, row.periodStart, row.periodEnd]
  );

  await client.query(
    `INSERT INTO analytics_mart.kpi_period (
       period_type, period_start, period_end, kpi_code,
       numerator, denominator, kpi_value, sample_size,
       moment_domain, formula_version, calculated_at
     ) VALUES (
       $1, $2::date, $3::date, $4,
       $5, $6, $7, $8,
       NULL, 1, now()
     )`,
    [
      row.periodType,
      row.periodStart,
      row.periodEnd,
      row.kpiCode,
      row.numerator,
      row.denominator,
      row.kpiValue,
      row.sampleSize,
    ]
  );

  return {
    kpiCode: row.kpiCode,
    periodType: row.periodType,
    periodStart: row.periodStart,
    periodEnd: row.periodEnd,
    numerator: row.numerator,
    denominator: row.denominator,
    kpiValue: row.kpiValue,
    sampleSize: row.sampleSize,
  };
}

function startOfUtcDay(d: Date): Date {
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
}

function startOfUtcIsoWeek(d: Date): Date {
  const day = startOfUtcDay(d);
  // ISO week: Monday = 0 … Sunday = 6
  const dow = (day.getUTCDay() + 6) % 7;
  return addDays(day, -dow);
}

function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + n);
  return x;
}

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}
