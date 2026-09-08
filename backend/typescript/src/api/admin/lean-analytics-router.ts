import { Router } from 'express';
import { getPool } from '../../platform/database/pool';
import { refreshFounderLeanKpis } from '../../modules/analytics/founder-lean-kpis';
import { realUserExists } from '../../modules/analytics/real-users';

export const adminLeanAnalyticsRouter = Router();

const FOUNDER_KPI_CODES = [
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
] as const;

const PRODUCT_KPI_CODES = [
  'KPI_009_MOMENT_CREATION_COMPLETION_RATE',
  'KPI_010_MOMENT_CREATION_DROP_OFF',
  'KPI_016_MOMENT_ACTIVATION_RATE',
  'KPI_017_MOMENT_COMPLETION_RATE',
  'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
  'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT',
  'KPI_030_INVITATIONS_PER_GROUP_MOMENT',
  'KPI_031_INVITE_OPEN_RATE',
  'KPI_032_INVITE__JOIN_CONVERSION',
  'KPI_033_INVITED_USER_ACTIVATION_RATE',
  'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
  'KPI_036_QUICK_ADD_COMPLETION_RATE',
  'KPI_037_FEATURE_ADOPTION_RATE',
  'KPI_038_CRITICAL_FLOW_ABANDONMENT_RATE',
  'KPI_041_CRITICAL_OPERATION_FAILURE_RATE',
  'KPI_042_CRASH_FREE_SESSION_RATE',
] as const;

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

function mapKpiRow(row: {
  kpi_code: string;
  kpi_name?: string | null;
  category?: string | null;
  audience?: string | null;
  numerator: string | null;
  denominator: string | null;
  kpi_value: string | null;
  sample_size: string | null;
  period_type?: string | null;
  period_start?: string | null;
  period_end?: string | null;
  formula_version?: number | null;
  calculated_at?: string | null;
  moment_domain?: string | null;
}) {
  return {
    kpiCode: row.kpi_code,
    kpiName: row.kpi_name ?? null,
    category: row.category ?? null,
    audience: row.audience ?? null,
    numerator: row.numerator != null ? Number(row.numerator) : null,
    denominator: row.denominator != null ? Number(row.denominator) : null,
    kpiValue: row.kpi_value != null ? Number(row.kpi_value) : null,
    sampleSize: row.sample_size != null ? Number(row.sample_size) : null,
    periodType: row.period_type ?? null,
    periodStart: row.period_start ?? null,
    periodEnd: row.period_end ?? null,
    formulaVersion: row.formula_version ?? null,
    calculatedAt: row.calculated_at ?? null,
    momentDomain: row.moment_domain ?? null,
  };
}

adminLeanAnalyticsRouter.get('/founder', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        kpi_code: string;
        kpi_name: string | null;
        category: string | null;
        audience: string | null;
        numerator: string | null;
        denominator: string | null;
        kpi_value: string | null;
        sample_size: string | null;
        period_type: string | null;
        period_start: string | null;
        period_end: string | null;
        formula_version: number | null;
      }>(
        `SELECT f.kpi_code,
                r.kpi_name,
                r.category,
                r.audience,
                f.numerator::text,
                f.denominator::text,
                f.kpi_value::text,
                f.sample_size::text,
                f.period_type,
                f.period_start::text,
                f.period_end::text,
                f.formula_version
         FROM analytics_mart.v_founder_latest f
         LEFT JOIN analytics_core.kpi_registry r ON r.kpi_code = f.kpi_code
         WHERE f.kpi_code = ANY($1::text[])`,
        [FOUNDER_KPI_CODES as unknown as string[]]
      );
      const byCode = new Map(r.rows.map((row) => [row.kpi_code, mapKpiRow(row)]));
      const cards = FOUNDER_KPI_CODES.map((code) => {
        const existing = byCode.get(code);
        if (existing) return existing;
        return {
          kpiCode: code,
          kpiName: null,
          category: null,
          audience: null,
          numerator: null,
          denominator: null,
          kpiValue: null,
          sampleSize: null,
          periodType: null,
          periodStart: null,
          periodEnd: null,
          formulaVersion: null,
          calculatedAt: null,
          momentDomain: null,
        };
      });

      // Fill names from registry when missing
      const names = await client.query<{ kpi_code: string; kpi_name: string; category: string; audience: string }>(
        `SELECT kpi_code, kpi_name, category, audience
         FROM analytics_core.kpi_registry
         WHERE kpi_code = ANY($1::text[])`,
        [FOUNDER_KPI_CODES as unknown as string[]]
      );
      const nameMap = new Map(names.rows.map((n) => [n.kpi_code, n]));
      return cards.map((c) => {
        const meta = nameMap.get(c.kpiCode);
        return {
          ...c,
          kpiName: c.kpiName ?? meta?.kpi_name ?? c.kpiCode,
          category: c.category ?? meta?.category ?? null,
          audience: c.audience ?? meta?.audience ?? null,
        };
      });
    });
    res.json({ data: { cards: data } });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.get('/wam-trend', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        period_start: string;
        wam: string | null;
        numerator: string | null;
        denominator: string | null;
      }>(
        `SELECT period_start::text, wam::text, numerator::text, denominator::text
         FROM analytics_mart.v_wam_12_weeks
         ORDER BY period_start ASC`
      );
      return r.rows.map((row) => ({
        periodStart: row.period_start,
        wam: row.wam != null ? Number(row.wam) : null,
        numerator: row.numerator != null ? Number(row.numerator) : null,
        denominator: row.denominator != null ? Number(row.denominator) : null,
      }));
    });
    res.json({ data: { items: data } });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.get('/second-moment-cohorts', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        cohort_month: string;
        first_time_creators: string;
        second_moment_eligible: string;
        second_moment_creators: string;
        second_moment_rate: string | null;
      }>(
        `SELECT cohort_month::text,
                first_time_creators::text,
                second_moment_eligible::text,
                second_moment_creators::text,
                second_moment_rate::text
         FROM analytics_mart.v_second_moment_cohorts
         ORDER BY cohort_month DESC
         LIMIT 24`
      );
      return r.rows.map((row) => ({
        cohortMonth: row.cohort_month,
        firstTimeCreators: Number(row.first_time_creators),
        secondMomentEligible: Number(row.second_moment_eligible),
        secondMomentCreators: Number(row.second_moment_creators),
        secondMomentRate: row.second_moment_rate != null ? Number(row.second_moment_rate) : null,
      }));
    });
    res.json({ data: { items: data } });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.get('/product', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        kpi_code: string;
        kpi_name: string | null;
        category: string | null;
        audience: string | null;
        numerator: string | null;
        denominator: string | null;
        kpi_value: string | null;
        sample_size: string | null;
        period_type: string | null;
        period_start: string | null;
        period_end: string | null;
        formula_version: number | null;
        moment_domain: string | null;
        calculated_at: string | null;
      }>(
        `SELECT DISTINCT ON (k.kpi_code, COALESCE(k.moment_domain, ''))
           k.kpi_code,
           r.kpi_name,
           r.category,
           r.audience,
           k.numerator::text,
           k.denominator::text,
           k.kpi_value::text,
           k.sample_size::text,
           k.period_type,
           k.period_start::text,
           k.period_end::text,
           k.formula_version,
           k.moment_domain,
           k.calculated_at::text
         FROM analytics_mart.kpi_period k
         LEFT JOIN analytics_core.kpi_registry r ON r.kpi_code = k.kpi_code
         WHERE k.kpi_code = ANY($1::text[])
         ORDER BY k.kpi_code, COALESCE(k.moment_domain, ''), k.period_start DESC, k.calculated_at DESC`,
        [PRODUCT_KPI_CODES as unknown as string[]]
      );

      // Prefer undomain'd row when both exist
      const preferred = new Map<string, ReturnType<typeof mapKpiRow>>();
      for (const row of r.rows) {
        const mapped = mapKpiRow(row);
        const existing = preferred.get(mapped.kpiCode);
        if (!existing || (existing.momentDomain != null && mapped.momentDomain == null)) {
          preferred.set(mapped.kpiCode, mapped);
        }
      }

      const names = await client.query<{ kpi_code: string; kpi_name: string; category: string }>(
        `SELECT kpi_code, kpi_name, category FROM analytics_core.kpi_registry WHERE kpi_code = ANY($1::text[])`,
        [PRODUCT_KPI_CODES as unknown as string[]]
      );
      const nameMap = new Map(names.rows.map((n) => [n.kpi_code, n]));

      const cards = PRODUCT_KPI_CODES.map((code) => {
        const existing = preferred.get(code);
        const meta = nameMap.get(code);
        if (existing) {
          return {
            ...existing,
            kpiName: existing.kpiName ?? meta?.kpi_name ?? code,
            category: existing.category ?? meta?.category ?? null,
          };
        }
        return {
          kpiCode: code,
          kpiName: meta?.kpi_name ?? code,
          category: meta?.category ?? null,
          audience: null,
          numerator: null,
          denominator: null,
          kpiValue: null,
          sampleSize: null,
          periodType: null,
          periodStart: null,
          periodEnd: null,
          formulaVersion: null,
          calculatedAt: null,
          momentDomain: null,
        };
      });

      const sections = {
        onboarding: cards.filter((c) =>
          ['KPI_009_MOMENT_CREATION_COMPLETION_RATE', 'KPI_010_MOMENT_CREATION_DROP_OFF'].includes(c.kpiCode)
        ),
        momentHealth: cards.filter((c) =>
          [
            'KPI_016_MOMENT_ACTIVATION_RATE',
            'KPI_017_MOMENT_COMPLETION_RATE',
            'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
          ].includes(c.kpiCode)
        ),
        network: cards.filter((c) =>
          [
            'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT',
            'KPI_030_INVITATIONS_PER_GROUP_MOMENT',
            'KPI_031_INVITE_OPEN_RATE',
            'KPI_032_INVITE__JOIN_CONVERSION',
            'KPI_033_INVITED_USER_ACTIVATION_RATE',
            'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
          ].includes(c.kpiCode)
        ),
        reliability: cards.filter((c) =>
          [
            'KPI_036_QUICK_ADD_COMPLETION_RATE',
            'KPI_037_FEATURE_ADOPTION_RATE',
            'KPI_038_CRITICAL_FLOW_ABANDONMENT_RATE',
            'KPI_041_CRITICAL_OPERATION_FAILURE_RATE',
            'KPI_042_CRASH_FREE_SESSION_RATE',
          ].includes(c.kpiCode)
        ),
      };

      return {
        sections,
        decisionHints: [
          {
            signal: 'Registrations↑, Activation↓',
            interpretation: 'Acquisition quality/onboarding deteriorating',
            action: 'Inspect source-level activation and onboarding/creation funnel',
          },
          {
            signal: 'Creation completion↑, Moment activation↓',
            interpretation: 'Users can create but do not know what to do next',
            action: 'Improve post-create next action, prompts and collaboration setup',
          },
          {
            signal: 'Activation↑, Completion↓',
            interpretation: 'Moments become active but execution fails',
            action: 'Inspect lifecycle steps and high-failure Moment types',
          },
          {
            signal: 'Completion↑, Second Moment↓',
            interpretation: 'One-time utility; weak repeat discovery',
            action: 'Improve post-completion journey and cross-Moment discovery',
          },
          {
            signal: 'Group participation↑, Participant→Creator↓',
            interpretation: 'Participants use Momentra but do not become creators',
            action: 'Improve participant onboarding and Create discovery',
          },
          {
            signal: 'WAM↑, actions/Moment↓',
            interpretation: 'Quantity rising but depth weakening',
            action: 'Inspect Moment types, user quality and feature adoption',
          },
        ],
      };
    });
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.get('/vc', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const latest = await client.query<{
        kpi_code: string;
        kpi_name: string | null;
        numerator: string | null;
        denominator: string | null;
        kpi_value: string | null;
        sample_size: string | null;
        period_type: string | null;
        period_start: string | null;
      }>(
        `SELECT DISTINCT ON (kpi_code)
           k.kpi_code,
           r.kpi_name,
           k.numerator::text,
           k.denominator::text,
           k.kpi_value::text,
           k.sample_size::text,
           k.period_type,
           k.period_start::text
         FROM analytics_mart.kpi_period k
         LEFT JOIN analytics_core.kpi_registry r ON r.kpi_code = k.kpi_code
         WHERE k.moment_domain IS NULL
           AND k.kpi_code = ANY($1::text[])
         ORDER BY k.kpi_code, k.period_start DESC, k.calculated_at DESC`,
        [
          [
            'KPI_006_USER_ACTIVATION_RATE',
            'KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM',
            'KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM',
            'KPI_015_MOMENTS_PER_ACTIVE_USER',
            'KPI_016_MOMENT_ACTIVATION_RATE',
            'KPI_017_MOMENT_COMPLETION_RATE',
            'KPI_024_D30_MEANINGFUL_RETENTION',
            'KPI_025_SECOND_MOMENT_RATE',
            'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
          ],
        ]
      );
      const byCode = new Map(latest.rows.map((row) => [row.kpi_code, row]));

      const mau = await client.query<{ n: string }>(
        `SELECT COUNT(DISTINCT ud.user_id)::text AS n
         FROM analytics_core.user_daily ud
         WHERE ud.activity_date >= date_trunc('month', now())::date
           AND ud.meaningfully_active_flag = TRUE
           AND ${realUserExists('ud.user_id')}`
      );
      const activatedUsers = await client.query<{ n: string }>(
        `SELECT COUNT(*)::text AS n
         FROM analytics_core.user_lifecycle_fact ulf
         WHERE ulf.activated_at IS NOT NULL
           AND ${realUserExists('ulf.user_id')}`
      );

      const pick = (code: string, label: string) => {
        const row = byCode.get(code);
        return {
          id: code,
          label,
          kpiCode: code,
          kpiValue: row?.kpi_value != null ? Number(row.kpi_value) : null,
          numerator: row?.numerator != null ? Number(row.numerator) : null,
          denominator: row?.denominator != null ? Number(row.denominator) : null,
          sampleSize: row?.sample_size != null ? Number(row.sample_size) : null,
          periodType: row?.period_type ?? null,
          periodStart: row?.period_start ?? null,
        };
      };

      return {
        metrics: [
          {
            id: 'activated_users',
            label: 'Activated Users',
            kpiCode: null,
            kpiValue: Number(activatedUsers.rows[0]?.n ?? 0),
            numerator: Number(activatedUsers.rows[0]?.n ?? 0),
            denominator: null,
            sampleSize: Number(activatedUsers.rows[0]?.n ?? 0),
            periodType: 'all_time',
            periodStart: null,
          },
          {
            id: 'mau',
            label: 'MAU (meaningful)',
            kpiCode: null,
            kpiValue: Number(mau.rows[0]?.n ?? 0),
            numerator: Number(mau.rows[0]?.n ?? 0),
            denominator: null,
            sampleSize: Number(mau.rows[0]?.n ?? 0),
            periodType: 'month',
            periodStart: null,
          },
          pick('KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM', 'Weekly Active Moments'),
          pick('KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM', 'Monthly Active Moments'),
          pick('KPI_015_MOMENTS_PER_ACTIVE_USER', 'Moments per Active User'),
          pick('KPI_016_MOMENT_ACTIVATION_RATE', 'Moment Activation Rate'),
          pick('KPI_017_MOMENT_COMPLETION_RATE', 'Moment Completion Rate'),
          pick('KPI_024_D30_MEANINGFUL_RETENTION', 'D30 Meaningful Retention'),
          pick('KPI_025_SECOND_MOMENT_RATE', 'Second Moment Rate'),
          pick('KPI_034_PARTICIPANT__CREATOR_CONVERSION', 'Participant → Creator Conversion'),
          pick('KPI_006_USER_ACTIVATION_RATE', 'User Activation Rate'),
        ].slice(0, 11),
      };
    });
    // Pack §3.3 is 9 metrics — keep Activated, MAU, WAM, MAM, Moments/AU, Activation, Completion, D30, Second Moment
    // Participant→Creator and User Activation are useful; trim to preferred 9:
    const preferredIds = [
      'activated_users',
      'mau',
      'KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM',
      'KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM',
      'KPI_015_MOMENTS_PER_ACTIVE_USER',
      'KPI_016_MOMENT_ACTIVATION_RATE',
      'KPI_017_MOMENT_COMPLETION_RATE',
      'KPI_024_D30_MEANINGFUL_RETENTION',
      'KPI_025_SECOND_MOMENT_RATE',
    ];
    const metrics = preferredIds
      .map((id) => data.metrics.find((m) => m.id === id || m.kpiCode === id))
      .filter(Boolean);
    res.json({ data: { metrics } });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.get('/group-kpis', async (_req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        kpi_code: string;
        numerator: string | null;
        denominator: string | null;
        kpi_value: string | null;
        sample_size: string | null;
        period_start: string;
        calculated_at: string;
      }>(
        `SELECT DISTINCT ON (kpi_code)
           kpi_code, numerator::text, denominator::text, kpi_value::text, sample_size::text,
           period_start::text, calculated_at::text
         FROM analytics_mart.kpi_period
         WHERE moment_domain = 'group'
           AND kpi_code IN (
             'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT',
             'KPI_030_INVITATIONS_PER_GROUP_MOMENT',
             'KPI_031_INVITE_OPEN_RATE',
             'KPI_032_INVITE__JOIN_CONVERSION',
             'KPI_033_INVITED_USER_ACTIVATION_RATE',
             'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
             'KPI_035_VIRAL_COEFFICIENT'
           )
         ORDER BY kpi_code, period_start DESC, calculated_at DESC`
      );
      return r.rows.map((row) => ({
        kpiCode: row.kpi_code,
        numerator: row.numerator != null ? Number(row.numerator) : null,
        denominator: row.denominator != null ? Number(row.denominator) : null,
        kpiValue: row.kpi_value != null ? Number(row.kpi_value) : null,
        sampleSize: row.sample_size != null ? Number(row.sample_size) : null,
        periodStart: row.period_start,
        calculatedAt: row.calculated_at,
      }));
    });
    res.json({ data: { kpis: data } });
  } catch (e) {
    next(e);
  }
});

adminLeanAnalyticsRouter.post('/refresh', async (_req, res, next) => {
  try {
    const result = await withDb((client) => refreshFounderLeanKpis(client));
    res.json({
      data: {
        founderCount: result.founder.length,
        groupCount: result.group.length,
        cohortsUpserted: result.cohortsUpserted,
        founder: result.founder,
        group: result.group,
      },
    });
  } catch (e) {
    next(e);
  }
});
