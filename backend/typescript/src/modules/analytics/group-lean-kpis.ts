import type { PoolClient } from 'pg';

const GROUP_KPI_VIEWS: Array<{ code: string; view: string }> = [
  { code: 'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT', view: 'analytics_mart.v_kpi_020_avg_participants_per_group_moment' },
  { code: 'KPI_030_INVITATIONS_PER_GROUP_MOMENT', view: 'analytics_mart.v_kpi_030_invitations_per_group_moment' },
  { code: 'KPI_031_INVITE_OPEN_RATE', view: 'analytics_mart.v_kpi_031_invite_open_rate' },
  { code: 'KPI_032_INVITE__JOIN_CONVERSION', view: 'analytics_mart.v_kpi_032_invite_join_conversion' },
  { code: 'KPI_033_INVITED_USER_ACTIVATION_RATE', view: 'analytics_mart.v_kpi_033_invited_user_activation_rate' },
  { code: 'KPI_034_PARTICIPANT__CREATOR_CONVERSION', view: 'analytics_mart.v_kpi_034_participant_creator_conversion' },
  { code: 'KPI_035_VIRAL_COEFFICIENT', view: 'analytics_mart.v_kpi_035_viral_coefficient' },
];

export interface GroupLeanKpiRow {
  kpiCode: string;
  numerator: number | null;
  denominator: number | null;
  kpiValue: number | null;
  sampleSize: number | null;
}

/**
 * Materialize Group Lean KPIs 20 + 30–35 into analytics_mart.kpi_period for the current UTC day.
 */
export async function refreshGroupLeanKpis(
  client: PoolClient,
  opts?: { periodStart?: Date; periodEnd?: Date }
): Promise<GroupLeanKpiRow[]> {
  const periodStart = opts?.periodStart ?? startOfUtcDay(new Date());
  const periodEnd = opts?.periodEnd ?? addDays(periodStart, 1);
  const out: GroupLeanKpiRow[] = [];

  for (const { code, view } of GROUP_KPI_VIEWS) {
    const r = await client.query<{
      kpi_value: string | null;
      numerator: string | null;
      denominator: string | null;
      sample_size: string | null;
    }>(`SELECT kpi_value::text, numerator::text, denominator::text, sample_size::text FROM ${view}`);
    const row = r.rows[0];
    const numerator = row?.numerator != null ? Number(row.numerator) : null;
    const denominator = row?.denominator != null ? Number(row.denominator) : null;
    const kpiValue = row?.kpi_value != null ? Number(row.kpi_value) : null;
    const sampleSize = row?.sample_size != null ? Number(row.sample_size) : null;

    await client.query(
      `DELETE FROM analytics_mart.kpi_period
       WHERE kpi_code = $1
         AND period_type = 'day'
         AND period_start = $2::date
         AND period_end = $3::date
         AND formula_version = 1
         AND moment_domain = 'group'
         AND moment_category IS NULL
         AND moment_type IS NULL
         AND acquisition_source IS NULL`,
      [code, periodStart.toISOString().slice(0, 10), periodEnd.toISOString().slice(0, 10)]
    );

    await client.query(
      `INSERT INTO analytics_mart.kpi_period (
         period_type, period_start, period_end, kpi_code,
         numerator, denominator, kpi_value, sample_size,
         moment_domain, formula_version, calculated_at
       ) VALUES (
         'day', $1::date, $2::date, $3,
         $4, $5, $6, $7,
         'group', 1, now()
       )`,
      [
        periodStart.toISOString().slice(0, 10),
        periodEnd.toISOString().slice(0, 10),
        code,
        numerator,
        denominator,
        kpiValue,
        sampleSize,
      ]
    );

    out.push({ kpiCode: code, numerator, denominator, kpiValue, sampleSize });
  }

  // Viral funnel snapshot for dashboards
  const invites = await client.query<{ n: string }>(
    `SELECT COUNT(DISTINCT properties->>'invite_id')::text AS n
     FROM analytics_raw.events
     WHERE event_name='participant_invited' AND moment_domain='group' AND is_valid`
  );
  const opens = await client.query<{ n: string }>(
    `SELECT COUNT(DISTINCT properties->>'invite_id')::text AS n
     FROM analytics_raw.events
     WHERE event_name='invite_opened' AND is_valid`
  );
  const joins = await client.query<{ n: string }>(
    `SELECT COUNT(DISTINCT properties->>'invite_id')::text AS n
     FROM analytics_raw.events
     WHERE event_name='participant_joined' AND is_valid AND properties->>'invite_id' IS NOT NULL`
  );

  await client.query(
    `INSERT INTO analytics_mart.viral_funnel (
       period_type, period_start, period_end, invites_issued, invites_opened, joins, formula_version
     ) VALUES ('day', $1::date, $2::date, $3, $4, $5, 1)
     ON CONFLICT (period_type, period_start, formula_version) DO UPDATE SET
       period_end = EXCLUDED.period_end,
       invites_issued = EXCLUDED.invites_issued,
       invites_opened = EXCLUDED.invites_opened,
       joins = EXCLUDED.joins,
       calculated_at = now()`,
    [
      periodStart.toISOString().slice(0, 10),
      periodEnd.toISOString().slice(0, 10),
      Number(invites.rows[0]?.n ?? 0),
      Number(opens.rows[0]?.n ?? 0),
      Number(joins.rows[0]?.n ?? 0),
    ]
  );

  return out;
}

function startOfUtcDay(d: Date): Date {
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
}

function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + n);
  return x;
}
