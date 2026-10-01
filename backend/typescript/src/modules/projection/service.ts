import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGroupMember } from '../collaboration/group-membership';
import { countMediaForMemories, listMediaForMemories } from '../memory/memory-attachments';
import { listMetricsForScope, PHASE7_PULSE_METRIC_CODES } from '../analytics/engine';
import {
  assembleTypedSignals,
  computeLifeModuleScores,
  formatScore,
  loadLifeTrendSeries,
  mapFamily,
} from '../business/business-life-enrichment';
import {
  personalFamilyFromMomentTypeCode,
  personalFamilyLabel,
  type PersonalFamilyCode,
} from '../personal/moment-family';
import { lifeActivityCountExclusionSql } from './life-activity-count';

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}

/** life-v1-provisional: clamp 0–100; null when domain has zero signal. */
function lifeDomainMetric(
  hasSignal: boolean,
  raw: number
): { score: number | null; label: string; status: string } {
  if (!hasSignal) {
    return { score: null, label: '—', status: 'EMPTY' };
  }
  const score = Math.max(0, Math.min(100, Math.round(raw)));
  const status = score >= 80 ? 'STRONG' : score >= 55 ? 'STABLE' : 'ATTENTION';
  return { score, label: String(score), status };
}

function lifeBalanceBar(
  hasSignal: boolean,
  raw: number,
  labels: [string, string, string, string]
): { value: number | null; label: string } {
  if (!hasSignal) {
    return { value: null, label: '—' };
  }
  const value = Math.max(0, Math.min(100, Math.round(raw)));
  const label =
    value >= 85 ? labels[3] : value >= 70 ? labels[2] : value >= 50 ? labels[1] : labels[0];
  return { value, label };
}

type LifeDomainKey = 'experience' | 'purchase' | 'living' | 'goal' | 'community';

function buildLifeDrivers(
  domains: Record<LifeDomainKey, { score: number | null; label: string; status: string }>
): Array<{ domain: string; title: string; detail: string }> {
  const scored = (Object.entries(domains) as Array<[LifeDomainKey, { score: number | null }]>)
    .filter(([, d]) => d.score != null)
    .map(([k, d]) => ({ domain: k, score: d.score as number }))
    .sort((a, b) => a.score - b.score);
  if (scored.length === 0) return [];
  const lowest = scored[0];
  const titles: Record<LifeDomainKey, string> = {
    experience: 'Experience needs momentum',
    purchase: 'Purchase balance is thin',
    living: 'Living ops are quiet',
    goal: 'Goals need progress',
    community: 'Community signal is low',
  };
  return [
    {
      domain: lowest.domain,
      title: titles[lowest.domain],
      detail: `Lowest domain score is ${lowest.score}. Add activity in this area to lift Group Health.`,
    },
  ];
}

export interface PersonalPulseDto {
  userId: string;
  attentionCount: number;
  activeMomentCount: number;
  recoveryScore: string | null;
  moodState: string | null;
  rhythmScore: string | null;
  wellbeingScore: string | null;
  widgetPayload: Record<string, unknown>;
  projectionVersion: number;
  updatedAt: string;
}

export async function getPersonalPulse(
  client: PoolClient,
  userId: string,
  momentId?: string
): Promise<PersonalPulseDto> {
  const row = await client.query<{
    user_id: string;
    attention_count: number;
    active_moment_count: number;
    recovery_score: string | null;
    mood_state: string | null;
    rhythm_score: string | null;
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown>;
    projection_version: string;
    updated_at: Date;
  }>(
    `SELECT user_id, attention_count, active_moment_count, recovery_score, mood_state,
            rhythm_score, wellbeing_score, widget_payload, projection_version, updated_at
     FROM projection.personal_pulse WHERE user_id = $1`,
    [userId]
  );

  if (!row.rows[0]) {
    return {
      userId,
      attentionCount: 0,
      activeMomentCount: 0,
      recoveryScore: null,
      moodState: null,
      rhythmScore: null,
      wellbeingScore: null,
      widgetPayload: {},
      projectionVersion: 0,
      updatedAt: new Date().toISOString(),
    };
  }

  const r = row.rows[0];
  const basePayload: Record<string, unknown> = { ...(r.widget_payload ?? {}) };
  // Phase 7 curated metric bundle (server-authored). UI shell unchanged; clients may ignore.
  const phase7ScopeType = momentId ? 'MOMENT' : 'USER';
  const phase7ScopeId = momentId ?? userId;
  try {
    const phase7Metrics = await listMetricsForScope(client, phase7ScopeType, phase7ScopeId);
    const curated = new Set<string>(PHASE7_PULSE_METRIC_CODES);
    const phase7Bundle = phase7Metrics.filter((m) => curated.has(m.metricCode));
    if (phase7Bundle.length > 0) {
      basePayload.phase7Metrics = phase7Bundle.map((m) => ({
        metricCode: m.metricCode,
        numericValue: m.numericValue,
        textValue: m.textValue,
        status: m.status,
        version: m.version,
        computedAt: m.computedAt,
      }));
    }
  } catch {
    // Catalogue may not be migrated yet — leave Pulse payload intact.
  }

  const dto: PersonalPulseDto = {
    userId: r.user_id,
    attentionCount: r.attention_count,
    activeMomentCount: r.active_moment_count,
    recoveryScore: r.recovery_score,
    moodState: r.mood_state,
    rhythmScore: r.rhythm_score,
    wellbeingScore: r.wellbeing_score,
    widgetPayload: basePayload,
    projectionVersion: parseInt(r.projection_version, 10),
    updatedAt: r.updated_at.toISOString(),
  };
  if (!momentId) return dto;

  const scopedExpense = await client.query<{
    currency_code: string;
    spend_amount: string;
    last_expense_at: Date | null;
  }>(
    `SELECT e.currency_code,
            SUM(e.amount)::text AS spend_amount,
            MAX(e.posted_at) AS last_expense_at
       FROM finance.personal_expense_context pec
       JOIN finance.expense e ON e.expense_id = pec.expense_id
      WHERE pec.user_id = $1
        AND pec.moment_id = $2
        AND e.status = 'POSTED'
      GROUP BY e.currency_code`,
    [userId, momentId]
  );
  const scopedPayload: Record<string, unknown> = { ...(dto.widgetPayload ?? {}) };
  const spendByCurrency = Object.fromEntries(
    scopedExpense.rows.map((expense) => [expense.currency_code, expense.spend_amount])
  );
  if (Object.keys(spendByCurrency).length > 0) {
    scopedPayload.spendByCurrency = spendByCurrency;
  } else {
    delete scopedPayload.spendByCurrency;
  }
  const lastExpenseAt = scopedExpense.rows
    .map((expense) => expense.last_expense_at)
    .filter((value): value is Date => value instanceof Date)
    .sort((a, b) => b.getTime() - a.getTime())[0];
  if (lastExpenseAt) {
    scopedPayload.lastExpenseAt = lastExpenseAt.toISOString();
  } else {
    delete scopedPayload.lastExpenseAt;
  }
  return {
    ...dto,
    widgetPayload: scopedPayload,
  };
}

export async function listPersonalMoments(
  client: PoolClient,
  userId: string,
  cursor: string | undefined,
  limit: number
): Promise<CursorPage<{ momentId: string; title: string; status: string; momentTypeCode: string }>> {
  const safeLimit = Math.min(Math.max(limit, 1), 50);
  const rows = await client.query<{
    moment_id: string;
    title: string;
    status: string;
    moment_type_code: string;
    display_rank: number;
  }>(
    `WITH combined AS (
       SELECT pm.moment_id, pm.title, pm.status, pm.moment_type_code, pm.display_rank,
              mc.code AS category_code, m.created_at
       FROM projection.personal_moments pm
       JOIN core.moment m ON m.moment_id = pm.moment_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
       WHERE pm.user_id = $1 AND m.status IN ('ACTIVE', 'DRAFT')
       UNION ALL
       SELECT m.moment_id, m.title, m.status, mt.code AS moment_type_code,
              CASE mc.code
                WHEN 'LIFE_OPERATIONS' THEN 1
                WHEN 'FUTURE_BUILDING' THEN 2
                WHEN 'LIFESTYLE' THEN 3
                WHEN 'RELATIONSHIPS' THEN 4
                ELSE (EXTRACT(EPOCH FROM m.updated_at)::bigint % 1000000000)::int
              END AS display_rank,
              mc.code AS category_code, m.created_at
       FROM core.moment m
       JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id AND pmc.user_id = $1
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
       WHERE m.domain_code = 'PERSONAL' AND m.status IN ('ACTIVE', 'DRAFT')
         AND NOT EXISTS (
           SELECT 1 FROM projection.personal_moments pm
           WHERE pm.user_id = $1 AND pm.moment_id = m.moment_id
         )
     ),
     ranked AS (
       SELECT moment_id, title, status, moment_type_code, display_rank,
              ROW_NUMBER() OVER (
                PARTITION BY CASE
                  WHEN category_code IN ('LIFE_OPERATIONS', 'FUTURE_BUILDING', 'LIFESTYLE', 'RELATIONSHIPS')
                  THEN category_code
                  ELSE moment_id::text
                END
                ORDER BY
                  CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END,
                  CASE WHEN display_rank BETWEEN 1 AND 4 THEN 0 ELSE 1 END,
                  created_at ASC
              ) AS area_rank
       FROM combined
     )
     SELECT moment_id, title, status, moment_type_code, display_rank
     FROM ranked
     WHERE area_rank = 1
       AND ($2::int IS NULL OR display_rank > $2)
     ORDER BY display_rank ASC
     LIMIT $3`,
    [userId, cursor ? parseInt(cursor, 10) : null, safeLimit + 1]
  );

  const hasMore = rows.rows.length > safeLimit;
  const items = rows.rows.slice(0, safeLimit).map((r) => ({
    momentId: r.moment_id,
    title: r.title,
    status: r.status,
    momentTypeCode: r.moment_type_code,
  }));
  const nextCursor = hasMore ? String(rows.rows[safeLimit - 1].display_rank) : null;
  return { items, nextCursor };
}

/** Per-section honesty for Life (S2 G3). */
export type LifeSectionQuality = 'REAL_DATA' | 'EMPTY_SUPPORTED' | 'API_GAP' | 'DEFERRED';

export type PersonalLifeCtaAction = 'LOG_RECOVERY' | 'LOG_SPEND' | 'OPEN_ADD' | 'NONE';

export interface PersonalLifeByFamilyDto {
  familyCode: PersonalFamilyCode;
  label: string;
  expenseTotal: string;
  incomeTotal: string;
  periodLogs: number;
  moodOrRecoveryLogs: number;
}

export interface PersonalLifeHighlightDto {
  familyCode: PersonalFamilyCode;
  title: string;
  occurredAt: string;
  activityCode: string;
}

/** Personal Life Health dashboard — Figma `1047:7689` / body `1047:7707`. User-scoped, cross-moment. */
export interface PersonalLifeThisWeekDto {
  expenseTotal: string;
  incomeTotal: string;
  currencyCode: string | null;
  spendByCurrency: Record<string, string>;
  periodLogs: number;
  moodOrRecoveryLogs: number;
  periodStart: string;
  periodEnd: string;
  /** Per-family rollup for Life chip filters (only families with activity or active setup). */
  byFamily: PersonalLifeByFamilyDto[];
  /** Recent PERSONAL activity with family derived from moment type. */
  highlights: PersonalLifeHighlightDto[];
}

export interface PersonalLifeJourneyItemDto {
  icon: string;
  title: string;
  when: string;
  value: string;
  tone: 'up' | 'down' | 'neutral';
  familyCode: PersonalFamilyCode;
  momentId?: string;
  activityCode?: string;
}

export interface PersonalLifeDto {
  userId: string;
  activeAreaCount: number;
  /** REAL after Personal join — honest empties / area counts; no invented Figma scores. */
  dataQuality: 'FIGMA_SEEDED' | 'REAL';
  sectionQuality: Record<string, LifeSectionQuality>;
  /** Null until Life pattern scoring exists — never invent 0 as health. */
  score: number | null;
  scoreMax: number;
  statusLabel: string;
  trendLabel: string;
  insight: string;
  areaScores: { code: string; label: string; score: number | null; color: string }[];
  drift: {
    title: string;
    headline: string;
    body: string;
    ctaLabel: string;
  };
  leverage: {
    title: string;
    actionTitle: string;
    actionBody: string;
    ctaLabel: string;
    /** Stable client routing — do not parse ctaLabel. */
    ctaAction: PersonalLifeCtaAction;
    impacts: { label: string; delta: string; tone: 'up' | 'down' | 'neutral' }[];
  };
  balance: { code: string; label: string; score: number; badge: string; badgeTone: string }[];
  emotionalTrend: {
    subtitle: string;
    series: { code: string; label: string; color: string; points: number[] }[];
  };
  dominantEmotion: {
    title: string;
    headline: string;
    segments: { label: string; percent: number; color: string }[];
  };
  happyDrivers: { title: string; subtitle: string; items: string[] };
  journey: {
    title: string;
    subtitle: string;
    items: PersonalLifeJourneyItemDto[];
  };
  aiInsights: { title: string; lead: string; body: string };
  /** Rolling 7-day spend + check-in counts across the user's Personal moments. */
  thisWeek: PersonalLifeThisWeekDto;
  projectionVersion: number;
  updatedAt: string;
}

const LIFE_HONEST_EMPTY_SECTION_QUALITY: Record<string, LifeSectionQuality> = {
  score: 'EMPTY_SUPPORTED',
  statusLabel: 'EMPTY_SUPPORTED',
  trendLabel: 'EMPTY_SUPPORTED',
  insight: 'EMPTY_SUPPORTED',
  areaScores: 'EMPTY_SUPPORTED',
  drift: 'EMPTY_SUPPORTED',
  leverage: 'EMPTY_SUPPORTED',
  balance: 'EMPTY_SUPPORTED',
  emotionalTrend: 'EMPTY_SUPPORTED',
  dominantEmotion: 'EMPTY_SUPPORTED',
  happyDrivers: 'EMPTY_SUPPORTED',
  journey: 'EMPTY_SUPPORTED',
  aiInsights: 'EMPTY_SUPPORTED',
  thisWeek: 'EMPTY_SUPPORTED',
  activeAreaCount: 'REAL_DATA',
};

function emptyThisWeek(): PersonalLifeThisWeekDto {
  const periodEnd = new Date();
  const periodStart = new Date(periodEnd.getTime() - 7 * 86400000);
  return {
    expenseTotal: '0',
    incomeTotal: '0',
    currencyCode: null,
    spendByCurrency: {},
    periodLogs: 0,
    moodOrRecoveryLogs: 0,
    periodStart: periodStart.toISOString(),
    periodEnd: periodEnd.toISOString(),
    byFamily: [],
    highlights: [],
  };
}

const ALL_PERSONAL_FAMILIES: PersonalFamilyCode[] = [
  'LIFE_OPERATIONS',
  'FUTURE_BUILDING',
  'LIFESTYLE',
  'RELATIONSHIPS',
];

function deriveLifeLeverageCta(thisWeek: PersonalLifeThisWeekDto, hasActivity: boolean): {
  ctaAction: PersonalLifeCtaAction;
  ctaLabel: string;
  actionTitle: string;
  actionBody: string;
} {
  if (!hasActivity && thisWeek.periodLogs === 0 && (parseFloat(thisWeek.expenseTotal) || 0) === 0) {
    return {
      ctaAction: 'NONE',
      ctaLabel: 'Coming soon',
      actionTitle: 'Not available yet',
      actionBody: 'Leverage suggestions appear after you log Everyday recovery or spend.',
    };
  }
  if (thisWeek.moodOrRecoveryLogs === 0) {
    return {
      ctaAction: 'LOG_RECOVERY',
      ctaLabel: 'Log recovery',
      actionTitle: 'Check in on recovery',
      actionBody: 'A quick recovery log keeps Everyday rhythm visible on Life.',
    };
  }
  if ((parseFloat(thisWeek.expenseTotal) || 0) === 0) {
    return {
      ctaAction: 'LOG_SPEND',
      ctaLabel: 'Log spend',
      actionTitle: 'Capture this week\'s spend',
      actionBody: 'Everyday spend keeps This week and Life filters honest.',
    };
  }
  return {
    ctaAction: 'OPEN_ADD',
    ctaLabel: 'Add something',
    actionTitle: 'Keep the week moving',
    actionBody: 'Log mood, recovery, or spend from Add.',
  };
}

/** Aggregate last-7d Personal spend + activity for Life "This week" card. */
async function buildPersonalLifeThisWeek(
  client: PoolClient,
  userId: string
): Promise<PersonalLifeThisWeekDto> {
  const days = 7;
  const periodEnd = new Date();
  const periodStart = new Date(periodEnd.getTime() - days * 86400000);

  const spendRows = await client
    .query<{ currency_code: string; spend_amount: string }>(
      `SELECT e.currency_code, COALESCE(SUM(e.amount), 0)::text AS spend_amount
       FROM finance.expense e
       WHERE e.created_by_user_id = $1
         AND e.status IN ('POSTED', 'DRAFT')
         AND e.effective_at >= now() - ($2 || ' days')::interval
         AND EXISTS (
           SELECT 1 FROM personal.personal_moment_context pmc
           WHERE pmc.moment_id = e.moment_id AND pmc.user_id = $1
         )
       GROUP BY e.currency_code
       ORDER BY SUM(e.amount) DESC`,
      [userId, String(days)]
    )
    .catch(() => ({ rows: [] as Array<{ currency_code: string; spend_amount: string }> }));

  const spendByCurrency = Object.fromEntries(
    spendRows.rows.map((r) => [r.currency_code, r.spend_amount])
  );
  const expenseTotal = spendRows.rows
    .reduce((sum, r) => sum + (parseFloat(r.spend_amount) || 0), 0)
    .toFixed(2);
  const currencyCode = spendRows.rows[0]?.currency_code ?? null;

  const incomeRow = await client
    .query<{ income_total: string | null }>(
      `SELECT COALESCE(SUM(fm.amount), 0)::text AS income_total
       FROM finance.financial_movement fm
       WHERE fm.source_type = 'PERSONAL_INCOME'
         AND fm.status = 'POSTED'
         AND fm.effective_at >= now() - ($2 || ' days')::interval
         AND EXISTS (
           SELECT 1 FROM personal.personal_moment_context pmc
           WHERE pmc.moment_id = fm.source_id AND pmc.user_id = $1
         )`,
      [userId, String(days)]
    )
    .catch(() => ({ rows: [] as Array<{ income_total: string | null }> }));

  const actRow = await client
    .query<{ period_logs: string; mood_recovery_logs: string }>(
      `SELECT
         COUNT(*)::text AS period_logs,
         COUNT(*) FILTER (
           WHERE UPPER(activity_code) LIKE '%MOOD%'
              OR UPPER(activity_code) LIKE '%RECOVERY%'
              OR UPPER(activity_code) LIKE '%WELLBEING%'
         )::text AS mood_recovery_logs
       FROM projection.recent_activity
       WHERE user_id = $1
         AND domain_code = 'PERSONAL'
         AND occurred_at >= now() - ($2 || ' days')::interval
         AND COALESCE(activity_payload->>'status', 'POSTED') <> 'VOIDED'
         AND ${lifeActivityCountExclusionSql()}`,
      [userId, String(days)]
    )
    .catch(() => ({ rows: [] as Array<{ period_logs: string; mood_recovery_logs: string }> }));

  const activeSetups = await client
    .query<{ system_code: string }>(
      `SELECT DISTINCT system_code
       FROM personal.life_system_setup
       WHERE user_id = $1 AND status = 'ACTIVE'`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{ system_code: string }> }));
  const activeFamilySet = new Set(
    activeSetups.rows.map((r) => personalFamilyFromMomentTypeCode(r.system_code))
  );

  const spendByFamily = await client
    .query<{ moment_type_code: string; spend_amount: string }>(
      `SELECT mt.code AS moment_type_code, COALESCE(SUM(e.amount), 0)::text AS spend_amount
       FROM finance.expense e
       JOIN core.moment m ON m.moment_id = e.moment_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       JOIN personal.personal_moment_context pmc ON pmc.moment_id = e.moment_id AND pmc.user_id = $1
       WHERE e.created_by_user_id = $1
         AND e.status IN ('POSTED', 'DRAFT')
         AND e.effective_at >= now() - ($2 || ' days')::interval
       GROUP BY mt.code`,
      [userId, String(days)]
    )
    .catch(() => ({ rows: [] as Array<{ moment_type_code: string; spend_amount: string }> }));

  const incomeByFamily = await client
    .query<{ moment_type_code: string; income_amount: string }>(
      `SELECT mt.code AS moment_type_code, COALESCE(SUM(fm.amount), 0)::text AS income_amount
       FROM finance.financial_movement fm
       JOIN core.moment m ON m.moment_id = fm.source_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       JOIN personal.personal_moment_context pmc ON pmc.moment_id = fm.source_id AND pmc.user_id = $1
       WHERE fm.source_type = 'PERSONAL_INCOME'
         AND fm.status = 'POSTED'
         AND fm.effective_at >= now() - ($2 || ' days')::interval
       GROUP BY mt.code`,
      [userId, String(days)]
    )
    .catch(() => ({ rows: [] as Array<{ moment_type_code: string; income_amount: string }> }));

  const activityByFamily = await client
    .query<{ moment_type_code: string | null; period_logs: string; mood_recovery_logs: string }>(
      `SELECT mt.code AS moment_type_code,
              COUNT(*)::text AS period_logs,
              COUNT(*) FILTER (
                WHERE UPPER(ra.activity_code) LIKE '%MOOD%'
                   OR UPPER(ra.activity_code) LIKE '%RECOVERY%'
                   OR UPPER(ra.activity_code) LIKE '%WELLBEING%'
              )::text AS mood_recovery_logs
       FROM projection.recent_activity ra
       LEFT JOIN core.moment m ON m.moment_id = ra.scope_id::uuid AND m.domain_code = 'PERSONAL'
       LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE ra.user_id = $1
         AND ra.domain_code = 'PERSONAL'
         AND ra.occurred_at >= now() - ($2 || ' days')::interval
         AND COALESCE(ra.activity_payload->>'status', 'POSTED') <> 'VOIDED'
         AND ${lifeActivityCountExclusionSql('ra')}
       GROUP BY mt.code`,
      [userId, String(days)]
    )
    .catch(() => ({
      rows: [] as Array<{ moment_type_code: string | null; period_logs: string; mood_recovery_logs: string }>,
    }));

  const familyAgg = new Map<
    PersonalFamilyCode,
    { expense: number; income: number; periodLogs: number; moodOrRecoveryLogs: number }
  >();
  for (const fam of ALL_PERSONAL_FAMILIES) {
    familyAgg.set(fam, { expense: 0, income: 0, periodLogs: 0, moodOrRecoveryLogs: 0 });
  }
  for (const r of spendByFamily.rows) {
    const fam = personalFamilyFromMomentTypeCode(r.moment_type_code);
    const cur = familyAgg.get(fam)!;
    cur.expense += parseFloat(r.spend_amount) || 0;
  }
  for (const r of incomeByFamily.rows) {
    const fam = personalFamilyFromMomentTypeCode(r.moment_type_code);
    const cur = familyAgg.get(fam)!;
    cur.income += parseFloat(r.income_amount) || 0;
  }
  for (const r of activityByFamily.rows) {
    const fam = personalFamilyFromMomentTypeCode(r.moment_type_code);
    const cur = familyAgg.get(fam)!;
    cur.periodLogs += parseInt(r.period_logs ?? '0', 10);
    cur.moodOrRecoveryLogs += parseInt(r.mood_recovery_logs ?? '0', 10);
  }

  const byFamily: PersonalLifeByFamilyDto[] = ALL_PERSONAL_FAMILIES.filter((fam) => {
    const cur = familyAgg.get(fam)!;
    return (
      activeFamilySet.has(fam) ||
      cur.expense > 0 ||
      cur.income > 0 ||
      cur.periodLogs > 0
    );
  }).map((fam) => {
    const cur = familyAgg.get(fam)!;
    return {
      familyCode: fam,
      label: personalFamilyLabel(fam),
      expenseTotal: cur.expense.toFixed(2),
      incomeTotal: cur.income.toFixed(2),
      periodLogs: cur.periodLogs,
      moodOrRecoveryLogs: cur.moodOrRecoveryLogs,
    };
  });

  const highlightRows = await client
    .query<{ title: string; occurred_at: Date; activity_code: string; moment_type_code: string | null }>(
      `SELECT ra.title, ra.occurred_at, ra.activity_code, mt.code AS moment_type_code
       FROM projection.recent_activity ra
       LEFT JOIN core.moment m ON m.moment_id = ra.scope_id::uuid AND m.domain_code = 'PERSONAL'
       LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE ra.user_id = $1
         AND ra.domain_code = 'PERSONAL'
         AND COALESCE(ra.activity_payload->>'status', 'POSTED') <> 'VOIDED'
         AND ${lifeActivityCountExclusionSql('ra')}
       ORDER BY ra.occurred_at DESC
       LIMIT 8`,
      [userId]
    )
    .catch(() => ({
      rows: [] as Array<{
        title: string;
        occurred_at: Date;
        activity_code: string;
        moment_type_code: string | null;
      }>,
    }));

  const highlights: PersonalLifeHighlightDto[] = highlightRows.rows.map((r) => ({
    familyCode: personalFamilyFromMomentTypeCode(r.moment_type_code),
    title: r.title,
    occurredAt: r.occurred_at.toISOString(),
    activityCode: r.activity_code,
  }));

  return {
    expenseTotal: spendRows.rows.length ? expenseTotal : '0',
    incomeTotal: incomeRow.rows[0]?.income_total ?? '0',
    currencyCode,
    spendByCurrency,
    periodLogs: parseInt(actRow.rows[0]?.period_logs ?? '0', 10),
    moodOrRecoveryLogs: parseInt(actRow.rows[0]?.mood_recovery_logs ?? '0', 10),
    periodStart: periodStart.toISOString(),
    periodEnd: periodEnd.toISOString(),
    byFamily,
    highlights,
  };
}

/** Honest Life shell — no invented Figma scores. Overall score null until Life pattern scoring exists. */
function honestEmptyLife(userId: string, activeAreaCount: number): PersonalLifeDto {
  return {
    userId,
    activeAreaCount,
    dataQuality: 'REAL',
    sectionQuality: { ...LIFE_HONEST_EMPTY_SECTION_QUALITY },
    score: null,
    scoreMax: 100,
    statusLabel: activeAreaCount > 0 ? 'Active' : 'No areas yet',
    trendLabel: '',
    insight: 'No insights yet',
    areaScores: [],
    drift: {
      title: 'Life Drift',
      headline: 'Not available yet',
      body: 'Drift alerts appear when analytics has enough Personal activity to compare areas.',
      ctaLabel: 'Coming soon',
    },
    leverage: {
      title: 'Highest Life Leverage',
      actionTitle: 'Not available yet',
      actionBody: 'Leverage suggestions require analytics refresh — core Momentra works without them.',
      ctaLabel: 'Coming soon',
      ctaAction: 'NONE',
      impacts: [],
    },
    balance: [],
    emotionalTrend: { subtitle: 'No trend data yet', series: [] },
    dominantEmotion: {
      title: 'Dominant Emotion',
      headline: 'No mood history yet',
      segments: [],
    },
    happyDrivers: { title: 'What Makes You Happy', subtitle: 'Highest Return Drivers', items: [] },
    journey: { title: 'Life Journey', subtitle: 'Key shifts', items: [] },
    thisWeek: emptyThisWeek(),
    aiInsights: {
      title: 'Insights',
      lead: 'No insights yet',
      body: 'Insights appear after analytics refresh when consent is granted. Core Momentra works without them.',
    },
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

function numPayload(v: unknown): number | null {
  if (v == null) return null;
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? Math.round(n) : null;
}

function avgNums(parts: Array<number | null>): number | null {
  const nums = parts.filter((n): n is number => n != null);
  if (!nums.length) return null;
  return Math.round(nums.reduce((a, b) => a + b, 0) / nums.length);
}

/** Family pattern chips for Life — from Pulse axes when present; not a Pulse rollup overall score. */
function areaScoreFromPulse(
  systemCode: string,
  pulse: {
    recovery_score: string | null;
    rhythm_score: string | null;
    widget_payload: Record<string, unknown> | null;
  } | undefined
): number | null {
  if (!pulse) return null;
  const p = pulse.widget_payload ?? {};
  switch (systemCode) {
    case 'LIFE_OPERATIONS':
      return avgNums([
        pulse.recovery_score != null ? Number(pulse.recovery_score) : null,
        pulse.rhythm_score != null ? Number(pulse.rhythm_score) : null,
        numPayload(p.lifeOpsWellbeingScore),
      ]);
    case 'FUTURE_BUILDING':
      return avgNums([
        numPayload(p.visionScore),
        numPayload(p.growthScore),
        numPayload(p.momentumScore),
        numPayload(p.disciplineScore),
      ]);
    case 'LIFESTYLE':
      return (
        numPayload(p.vitalityScore) ??
        avgNums([numPayload(p.joyScore), numPayload(p.fulfillmentScore), numPayload(p.explorationScore)])
      );
    case 'RELATIONSHIPS':
      return (
        numPayload(p.bondIndex) ??
        avgNums([
          numPayload(p.trustScore),
          numPayload(p.careScore),
          numPayload(p.supportScore),
          numPayload(p.presenceScore),
        ])
      );
    default:
      return null;
  }
}

export async function getPersonalLife(client: PoolClient, userId: string): Promise<PersonalLifeDto> {
  const areas = await client
    .query<{ system_code: string }>(
      `SELECT DISTINCT system_code
       FROM personal.life_system_setup
       WHERE user_id = $1 AND status = 'ACTIVE'
       ORDER BY system_code
       LIMIT 8`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{ system_code: string }> }));
  const activeAreaCount = Math.min(4, areas.rows.length);
  const base = honestEmptyLife(userId, activeAreaCount);

  const pulse = await client
    .query<{
      recovery_score: string | null;
      rhythm_score: string | null;
      widget_payload: Record<string, unknown> | null;
    }>(
      `SELECT recovery_score, rhythm_score, widget_payload
       FROM projection.personal_pulse WHERE user_id = $1`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{
      recovery_score: string | null;
      rhythm_score: string | null;
      widget_payload: Record<string, unknown> | null;
    }> }));
  const pulseRow = pulse.rows[0];

  const areaLabel: Record<string, { label: string; color: string }> = {
    LIFE_OPERATIONS: { label: 'Everyday', color: '#3B82F6' },
    FUTURE_BUILDING: { label: 'Future', color: '#10B981' },
    LIFESTYLE: { label: 'Lifestyle', color: '#F59E0B' },
    RELATIONSHIPS: { label: 'People', color: '#E12A9E' },
  };
  const areaScores = areas.rows.slice(0, 4).map((r) => {
    const meta = areaLabel[r.system_code] ?? { label: r.system_code, color: '#8C8C9E' };
    return {
      code: r.system_code,
      label: meta.label,
      score: areaScoreFromPulse(r.system_code, pulseRow),
      color: meta.color,
    };
  });

  const journeyRows = await client
    .query<{
      title: string;
      occurred_at: Date;
      activity_code: string;
      scope_id: string | null;
      moment_type_code: string | null;
    }>(
      `SELECT ra.title, ra.occurred_at, ra.activity_code, ra.scope_id::text AS scope_id,
              mt.code AS moment_type_code
       FROM projection.recent_activity ra
       LEFT JOIN core.moment m ON m.moment_id = ra.scope_id::uuid AND m.domain_code = 'PERSONAL'
       LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE ra.user_id = $1
         AND ra.domain_code = 'PERSONAL'
         AND COALESCE(ra.activity_payload->>'status', 'POSTED') <> 'VOIDED'
       ORDER BY ra.occurred_at DESC
       LIMIT 12`,
      [userId]
    )
    .catch(() => ({
      rows: [] as Array<{
        title: string;
        occurred_at: Date;
        activity_code: string;
        scope_id: string | null;
        moment_type_code: string | null;
      }>,
    }));

  const journeyItems: PersonalLifeJourneyItemDto[] = journeyRows.rows.map((r) => ({
    icon: '•',
    title: r.title,
    when: r.occurred_at.toISOString(),
    value: r.activity_code,
    tone: 'neutral' as const,
    familyCode: personalFamilyFromMomentTypeCode(r.moment_type_code),
    momentId: r.scope_id ?? undefined,
    activityCode: r.activity_code,
  }));

  const hasAreaScores = areaScores.some((a) => a.score != null);
  const thisWeek = await buildPersonalLifeThisWeek(client, userId);
  const hasThisWeekData =
    (parseFloat(thisWeek.expenseTotal) || 0) > 0 ||
    (parseFloat(thisWeek.incomeTotal) || 0) > 0 ||
    thisWeek.periodLogs > 0;
  const leverageCta = deriveLifeLeverageCta(thisWeek, journeyItems.length > 0 || hasThisWeekData);
  const sectionQuality: Record<string, LifeSectionQuality> = {
    ...LIFE_HONEST_EMPTY_SECTION_QUALITY,
    activeAreaCount: 'REAL_DATA',
    score: 'EMPTY_SUPPORTED',
    areaScores: areaScores.length ? (hasAreaScores ? 'REAL_DATA' : 'EMPTY_SUPPORTED') : 'EMPTY_SUPPORTED',
    journey: journeyItems.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    thisWeek: hasThisWeekData ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    leverage: leverageCta.ctaAction === 'NONE' ? 'EMPTY_SUPPORTED' : 'REAL_DATA',
  };

  const leverage = {
    ...base.leverage,
    ...leverageCta,
  };

  const insight = await client
    .query<{ title: string; body: string | null; generated_at: Date }>(
      `SELECT title, body, generated_at
       FROM analytics.deterministic_insight
       WHERE scope_type = 'USER' AND scope_id = $1 AND status = 'ACTIVE'
       ORDER BY generated_at DESC
       LIMIT 1`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{ title: string; body: string | null; generated_at: Date }> }));

  if (insight.rows[0]) {
    const row = insight.rows[0];
    sectionQuality.insight = 'REAL_DATA';
    sectionQuality.aiInsights = 'REAL_DATA';
    return {
      ...base,
      areaScores,
      leverage,
      journey: { title: 'Life Journey', subtitle: journeyItems.length ? 'Recent activity' : 'Key shifts', items: journeyItems },
      thisWeek,
      sectionQuality,
      insight: row.title,
      aiInsights: { title: 'Insights', lead: row.title, body: row.body ?? row.title },
      updatedAt: row.generated_at.toISOString(),
    };
  }

  return {
    ...base,
    areaScores,
    leverage,
    journey: { title: 'Life Journey', subtitle: journeyItems.length ? 'Recent activity' : 'Key shifts', items: journeyItems },
    thisWeek,
    sectionQuality,
  };
}

/** Per-section honesty for Memory (M4). */
export type MemorySectionQuality = 'REAL_DATA' | 'EMPTY_SUPPORTED' | 'API_GAP' | 'DEFERRED';

export type MemoryHighlightsSource = 'MEMORY' | 'ACTIVITY' | 'MIXED';

export interface PersonalMemoryHighlightDto {
  title: string;
  occurredAt: string;
  familyCode?: PersonalFamilyCode;
  activityCode?: string;
  memoryId?: string;
}

export interface PersonalMemoryPatternWhyItemDto {
  kind: 'ACTIVITY' | 'OCCURRENCE' | 'DRIVER';
  label: string;
  occurredAt?: string;
}

export interface PersonalMemoryDto {
  userId: string;
  /** Compatibility: raw memory rows. */
  items: Array<{
    memoryId: string;
    title: string | null;
    occurredAt: string | null;
    momentId: string | null;
    summary?: string | null;
  }>;
  memoryCount: number;
  periodLabel: string;
  periodStart: string;
  periodEnd: string;
  /** Server-authored only when backed by enough real data; otherwise null. */
  heroSentence: string | null;
  counts: {
    memories: number;
    activities: number;
    highlights: number;
  };
  highlights: PersonalMemoryHighlightDto[];
  highlightsSource: MemoryHighlightsSource;
  primaryPattern: {
    title: string;
    body: string;
    confidence: number | null;
  } | null;
  patternWhy: PersonalMemoryPatternWhyItemDto[] | null;
  returnBehaviours: Array<{ label: string; strengthLabel?: string }>;
  evolution: {
    thenLabel: string;
    nowLabel: string;
    summary: string;
  } | null;
  evolutionDetail: {
    thenSummary: string;
    nowSummary: string;
    notes: string[];
  } | null;
  /** Signed media for Relive / hero mosaic — omit when empty. */
  reliveMedia: Array<{
    memoryId: string;
    title: string | null;
    downloadUrl: string;
  }>;
  sectionQuality: Record<string, MemorySectionQuality>;
  dataQuality: 'REAL';
  projectionVersion: number;
  updatedAt: string;
}

const MEMORY_HONEST_EMPTY_SECTION_QUALITY: Record<string, MemorySectionQuality> = {
  hero: 'EMPTY_SUPPORTED',
  highlights: 'EMPTY_SUPPORTED',
  pattern: 'EMPTY_SUPPORTED',
  patternWhy: 'EMPTY_SUPPORTED',
  returnBehaviours: 'EMPTY_SUPPORTED',
  evolution: 'EMPTY_SUPPORTED',
  evolutionDetail: 'EMPTY_SUPPORTED',
  relive: 'EMPTY_SUPPORTED',
};

function formatMonthLabel(d: Date): string {
  return d.toLocaleString('en-US', { month: 'long', year: 'numeric', timeZone: 'UTC' });
}

function startOfUtcMonth(d: Date): Date {
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1, 0, 0, 0, 0));
}

function endOfUtcMonth(d: Date): Date {
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0, 23, 59, 59, 999));
}

/** M4 Memory projection — reflection over time; omit weak sections (no client rescue). */
export async function getPersonalMemory(
  client: PoolClient,
  userId: string
): Promise<PersonalMemoryDto> {
  const now = new Date();
  const periodStart = startOfUtcMonth(now);
  const periodEnd = endOfUtcMonth(now);
  const prevMonthAnchor = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 15));
  const prevStart = startOfUtcMonth(prevMonthAnchor);
  const prevEnd = endOfUtcMonth(prevMonthAnchor);
  const periodLabel = formatMonthLabel(now);
  const prevLabel = formatMonthLabel(prevMonthAnchor);

  const memoryRows = await client
    .query<{
      memory_id: string;
      title: string | null;
      summary: string | null;
      occurred_at: Date | null;
      moment_id: string | null;
      created_at: Date;
    }>(
      `SELECT m.memory_id, m.title, m.summary, m.occurred_at, m.moment_id, m.created_at
       FROM memory.memory m
       WHERE m.status = 'ACTIVE'
         AND (
           m.created_by_user_id = $1
           OR EXISTS (
             SELECT 1 FROM personal.personal_moment_context pmc
             WHERE pmc.moment_id = m.moment_id AND pmc.user_id = $1
           )
         )
       ORDER BY COALESCE(m.occurred_at, m.created_at) DESC
       LIMIT 100`,
      [userId]
    )
    .catch(() => ({
      rows: [] as Array<{
        memory_id: string;
        title: string | null;
        summary: string | null;
        occurred_at: Date | null;
        moment_id: string | null;
        created_at: Date;
      }>,
    }));

  const items = memoryRows.rows.map((r) => ({
    memoryId: r.memory_id,
    title: r.title,
    occurredAt: r.occurred_at?.toISOString() ?? null,
    momentId: r.moment_id,
    summary: r.summary,
  }));
  const memoryCount = items.length;

  const inPeriod = (iso: string | null, start: Date, end: Date): boolean => {
    if (!iso) return false;
    const t = Date.parse(iso);
    if (!Number.isFinite(t)) return false;
    return t >= start.getTime() && t <= end.getTime();
  };

  const periodMemories = items.filter((m) =>
    inPeriod(m.occurredAt ?? null, periodStart, periodEnd)
  );

  const activityRows = await client
    .query<{
      title: string;
      occurred_at: Date;
      activity_code: string;
      moment_type_code: string | null;
    }>(
      `SELECT ra.title, ra.occurred_at, ra.activity_code, mt.code AS moment_type_code
       FROM projection.recent_activity ra
       LEFT JOIN core.moment m ON m.moment_id = ra.scope_id::uuid AND m.domain_code = 'PERSONAL'
       LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE ra.user_id = $1
         AND ra.domain_code = 'PERSONAL'
         AND COALESCE(ra.activity_payload->>'status', 'POSTED') <> 'VOIDED'
         AND ra.occurred_at >= $2
         AND ra.occurred_at <= $3
       ORDER BY ra.occurred_at DESC
       LIMIT 40`,
      [userId, periodStart.toISOString(), periodEnd.toISOString()]
    )
    .catch(() => ({
      rows: [] as Array<{
        title: string;
        occurred_at: Date;
        activity_code: string;
        moment_type_code: string | null;
      }>,
    }));

  const prevActivityCount = await client
    .query<{ c: string }>(
      `SELECT COUNT(*)::text AS c
       FROM projection.recent_activity ra
       WHERE ra.user_id = $1
         AND ra.domain_code = 'PERSONAL'
         AND COALESCE(ra.activity_payload->>'status', 'POSTED') <> 'VOIDED'
         AND ra.occurred_at >= $2
         AND ra.occurred_at <= $3`,
      [userId, prevStart.toISOString(), prevEnd.toISOString()]
    )
    .catch(() => ({ rows: [{ c: '0' }] }));

  const prevMemoryCount = items.filter((m) =>
    inPeriod(m.occurredAt ?? null, prevStart, prevEnd)
  ).length;
  const prevActs = parseInt(prevActivityCount.rows[0]?.c ?? '0', 10) || 0;
  const periodActs = activityRows.rows.length;

  // Highlights: prefer memory rows, fill from activities; cap 5.
  const memoryHighlights: PersonalMemoryHighlightDto[] = (periodMemories.length ? periodMemories : items)
    .filter((m) => (m.title ?? '').trim().length > 0)
    .slice(0, 5)
    .map((m) => ({
      title: (m.title ?? '').trim(),
      occurredAt: m.occurredAt ?? periodEnd.toISOString(),
      memoryId: m.memoryId,
    }));

  const activityHighlights: PersonalMemoryHighlightDto[] = activityRows.rows
    .filter((r) => (r.title ?? '').trim().length > 0)
    .map((r) => ({
      title: r.title.trim(),
      occurredAt: r.occurred_at.toISOString(),
      familyCode: personalFamilyFromMomentTypeCode(r.moment_type_code),
      activityCode: r.activity_code,
    }));

  let highlights: PersonalMemoryHighlightDto[] = [];
  let highlightsSource: MemoryHighlightsSource = 'ACTIVITY';
  if (memoryHighlights.length > 0 || activityHighlights.length > 0) {
    const seen = new Set<string>();
    for (const h of [...memoryHighlights, ...activityHighlights]) {
      const key = h.title.toLowerCase();
      if (seen.has(key)) continue;
      seen.add(key);
      highlights.push(h);
      if (highlights.length >= 5) break;
    }
    const hasMem = highlights.some((h) => !!h.memoryId);
    const hasAct = highlights.some((h) => !!h.activityCode);
    highlightsSource = hasMem && hasAct ? 'MIXED' : hasMem ? 'MEMORY' : 'ACTIVITY';
  }

  // Primary pattern from memory.pattern (USER scope).
  const patternRows = await client
    .query<{
      pattern_id: string;
      title: string;
      description: string | null;
      confidence: string | null;
      first_detected_at: Date;
      last_detected_at: Date;
    }>(
      `SELECT pattern_id, title, description, confidence::text, first_detected_at, last_detected_at
       FROM memory.pattern
       WHERE scope_type = 'USER'
         AND scope_id = $1
         AND status IN ('ACTIVE', 'CONFIRMED')
       ORDER BY COALESCE(confidence, 0) DESC, last_detected_at DESC
       LIMIT 1`,
      [userId]
    )
    .catch(() => ({
      rows: [] as Array<{
        pattern_id: string;
        title: string;
        description: string | null;
        confidence: string | null;
        first_detected_at: Date;
        last_detected_at: Date;
      }>,
    }));

  const patternRow = patternRows.rows[0] ?? null;
  const confNum =
    patternRow?.confidence != null && patternRow.confidence !== ''
      ? Number(patternRow.confidence)
      : null;
  const confidenceOk = confNum == null || (Number.isFinite(confNum) && confNum >= 0.35);
  const primaryPattern =
    patternRow && confidenceOk
      ? {
          title: patternRow.title,
          body: (patternRow.description ?? '').trim() || patternRow.title,
          confidence: confNum != null && Number.isFinite(confNum) ? confNum : null,
        }
      : null;

  let patternWhy: PersonalMemoryPatternWhyItemDto[] | null = null;
  if (primaryPattern && patternRow) {
    const occ = await client
      .query<{ occurred_at: Date; significance: string | null }>(
        `SELECT occurred_at, significance::text
         FROM memory.pattern_occurrence
         WHERE pattern_id = $1
         ORDER BY occurred_at DESC
         LIMIT 5`,
        [patternRow.pattern_id]
      )
      .catch(() => ({ rows: [] as Array<{ occurred_at: Date; significance: string | null }> }));

    const evidence: PersonalMemoryPatternWhyItemDto[] = occ.rows.map((o) => ({
      kind: 'OCCURRENCE' as const,
      label: `Seen again · ${formatMonthLabel(o.occurred_at)}`,
      occurredAt: o.occurred_at.toISOString(),
    }));

    // Supporting recent activities as concrete drivers (titles only).
    for (const a of activityRows.rows.slice(0, 3)) {
      evidence.push({
        kind: 'ACTIVITY',
        label: a.title.trim(),
        occurredAt: a.occurred_at.toISOString(),
      });
    }
    patternWhy = evidence.length ? evidence.slice(0, 6) : null;
  }

  // Return behaviours from memory.learning only — no client-style heuristics.
  const learningRows = await client
    .query<{ title: string; learning_text: string; learning_type: string }>(
      `SELECT title, learning_text, learning_type
       FROM memory.learning
       WHERE scope_type = 'USER'
         AND scope_id = $1
         AND status = 'ACTIVE'
       ORDER BY updated_at DESC
       LIMIT 4`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{ title: string; learning_text: string; learning_type: string }> }));

  const returnBehaviours = learningRows.rows
    .map((r) => ({
      label: (r.title || r.learning_text).trim(),
      strengthLabel: undefined as string | undefined,
    }))
    .filter((r) => r.label.length > 0)
    .slice(0, 4);

  // Evolution requires a true earlier-period comparative basis.
  const hasThenBasis = prevActs > 0 || prevMemoryCount > 0;
  const hasNowBasis = periodActs > 0 || periodMemories.length > 0 || memoryCount > 0;
  let evolution: PersonalMemoryDto['evolution'] = null;
  let evolutionDetail: PersonalMemoryDto['evolutionDetail'] = null;
  if (hasThenBasis && hasNowBasis) {
    const thenParts: string[] = [];
    if (prevMemoryCount > 0) thenParts.push(`${prevMemoryCount} memor${prevMemoryCount === 1 ? 'y' : 'ies'}`);
    if (prevActs > 0) thenParts.push(`${prevActs} activit${prevActs === 1 ? 'y' : 'ies'}`);
    const nowParts: string[] = [];
    const nowMem = periodMemories.length;
    if (nowMem > 0) nowParts.push(`${nowMem} memor${nowMem === 1 ? 'y' : 'ies'}`);
    if (periodActs > 0) nowParts.push(`${periodActs} activit${periodActs === 1 ? 'y' : 'ies'}`);
    evolution = {
      thenLabel: prevLabel,
      nowLabel: periodLabel,
      summary: `From ${thenParts.join(', ') || 'a quieter month'} to ${nowParts.join(', ') || 'this month'}.`,
    };
    evolutionDetail = {
      thenSummary: `${prevLabel}: ${thenParts.join(', ') || 'no logged memories or activities'}.`,
      nowSummary: `${periodLabel}: ${nowParts.join(', ') || 'still gathering'}.`,
      notes: primaryPattern
        ? [`Pattern in focus: ${primaryPattern.title}`]
        : [],
    };
  }

  // Hero sentence only when backed.
  const highlightCount = highlights.length;
  const heroBacked =
    memoryCount >= 2 ||
    (memoryCount >= 1 && highlightCount >= 2) ||
    (primaryPattern != null && (memoryCount >= 1 || periodActs >= 3));
  let heroSentence: string | null = null;
  if (heroBacked) {
    if (primaryPattern && memoryCount >= 1) {
      heroSentence = `${periodLabel} · ${memoryCount} memor${memoryCount === 1 ? 'y' : 'ies'}, with a pattern worth noticing.`;
    } else if (memoryCount >= 2) {
      heroSentence = `${periodLabel} · ${memoryCount} memories to revisit.`;
    } else if (periodActs >= 3) {
      heroSentence = `${periodLabel} · ${periodActs} things logged — a month taking shape.`;
    }
  }

  // Relive media — only when real MEMORY attachments exist.
  const memoryIds = items.map((m) => m.memoryId).filter(Boolean);
  const mediaByMemory = await listMediaForMemories(client, memoryIds, 2).catch(
    () => new Map() as Awaited<ReturnType<typeof listMediaForMemories>>
  );
  const reliveMedia: PersonalMemoryDto['reliveMedia'] = [];
  for (const item of items) {
    const media = mediaByMemory.get(item.memoryId) ?? [];
    for (const m of media) {
      if (!m.downloadUrl) continue;
      reliveMedia.push({
        memoryId: item.memoryId,
        title: item.title,
        downloadUrl: m.downloadUrl,
      });
      if (reliveMedia.length >= 12) break;
    }
    if (reliveMedia.length >= 12) break;
  }

  const sectionQuality: Record<string, MemorySectionQuality> = {
    ...MEMORY_HONEST_EMPTY_SECTION_QUALITY,
    hero: periodActs > 0 || memoryCount > 0 ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    highlights: highlights.length > 0 ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    pattern: primaryPattern ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    patternWhy: patternWhy && patternWhy.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    returnBehaviours: returnBehaviours.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    evolution: evolution ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    evolutionDetail: evolutionDetail ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    relive: reliveMedia.length > 0 ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
  };

  // Clear fields when EMPTY_SUPPORTED so clients never "rescue".
  if (sectionQuality.patternWhy === 'EMPTY_SUPPORTED') {
    patternWhy = null;
  }
  if (sectionQuality.evolution === 'EMPTY_SUPPORTED') {
    evolution = null;
    evolutionDetail = null;
  }

  return {
    userId,
    items,
    memoryCount,
    periodLabel,
    periodStart: periodStart.toISOString(),
    periodEnd: periodEnd.toISOString(),
    heroSentence,
    counts: {
      memories: memoryCount,
      activities: periodActs,
      highlights: highlightCount,
    },
    highlights,
    highlightsSource: highlights.length ? highlightsSource : 'ACTIVITY',
    primaryPattern,
    patternWhy: sectionQuality.patternWhy === 'REAL_DATA' ? patternWhy : null,
    returnBehaviours: sectionQuality.returnBehaviours === 'REAL_DATA' ? returnBehaviours : [],
    evolution,
    evolutionDetail: sectionQuality.evolutionDetail === 'REAL_DATA' ? evolutionDetail : null,
    reliveMedia: sectionQuality.relive === 'REAL_DATA' ? reliveMedia : [],
    sectionQuality,
    dataQuality: 'REAL',
    projectionVersion: 1,
    updatedAt: now.toISOString(),
  };
}

export async function getPersonalAttention(
  client: PoolClient,
  userId: string
): Promise<{
  userId: string;
  items: Array<{
    attentionCaptureId: string;
    momentId: string;
    categoryCode: string;
    intensityCode: string;
    timeBlockCode: string;
    energyRemaining: number | null;
    observedAt: string;
    note: string | null;
  }>;
}> {
  const rows = await client
    .query<{
      attention_capture_id: string;
      moment_id: string;
      category_code: string;
      intensity_code: string;
      time_block_code: string;
      energy_remaining: number | null;
      observed_at: Date;
      note: string | null;
    }>(
      `SELECT attention_capture_id, moment_id, category_code, intensity_code, time_block_code,
              energy_remaining, observed_at, note
       FROM analytics.attention_capture
       WHERE user_id = $1 AND status = 'ACTIVE'
       ORDER BY observed_at DESC
       LIMIT 100`,
      [userId]
    )
    .catch(() => ({
      rows: [] as Array<{
        attention_capture_id: string;
        moment_id: string;
        category_code: string;
        intensity_code: string;
        time_block_code: string;
        energy_remaining: number | null;
        observed_at: Date;
        note: string | null;
      }>,
    }));

  return {
    userId,
    items: rows.rows.map((r) => ({
      attentionCaptureId: r.attention_capture_id,
      momentId: r.moment_id,
      categoryCode: r.category_code,
      intensityCode: r.intensity_code,
      timeBlockCode: r.time_block_code,
      energyRemaining: r.energy_remaining,
      observedAt: r.observed_at.toISOString(),
      note: r.note,
    })),
  };
}

export async function getPersonalActivity(
  client: PoolClient,
  userId: string,
  momentId: string | undefined,
  cursor: string | undefined,
  limit: number
): Promise<CursorPage<{ activityCode: string; title: string; occurredAt: string; activityPayload: Record<string, unknown> }>> {
  const safeLimit = Math.min(Math.max(limit, 1), 50);
  let cursorOccurredAt: string | null = null;
  let cursorId: string | null = null;
  if (cursor) {
    const parts = cursor.split('|');
    if (parts.length === 2) {
      cursorOccurredAt = parts[0];
      cursorId = parts[1];
    }
  }

  const rows = await client.query<{
    activity_code: string;
    title: string;
    occurred_at: Date;
    recent_activity_id: string;
    activity_payload: Record<string, unknown> | null;
  }>(
    `SELECT activity_code, title, occurred_at, recent_activity_id, activity_payload
     FROM projection.recent_activity
     WHERE user_id = $1
       AND domain_code = 'PERSONAL'
       AND ($4::uuid IS NULL OR scope_id = $4::uuid)
       AND COALESCE(activity_payload->>'status', 'POSTED') <> 'VOIDED'
       AND (
         $2::timestamptz IS NULL
         OR (occurred_at, recent_activity_id) < ($2::timestamptz, $3::uuid)
       )
     ORDER BY occurred_at DESC, recent_activity_id DESC
     LIMIT $5`,
    [userId, cursorOccurredAt, cursorId, momentId ?? null, safeLimit + 1]
  );
  const hasMore = rows.rows.length > safeLimit;
  const slice = rows.rows.slice(0, safeLimit);
  const items = slice.map((r) => ({
    activityCode: r.activity_code,
    title: r.title,
    occurredAt: r.occurred_at.toISOString(),
    activityPayload: r.activity_payload ?? {},
  }));
  const last = slice[slice.length - 1];
  const nextCursor =
    hasMore && last ? `${last.occurred_at.toISOString()}|${last.recent_activity_id}` : null;
  return { items, nextCursor };
}

export async function listGroupMoments(
  client: PoolClient,
  ctx: RequestContext,
  cursor: string | undefined,
  limit: number,
  lifecycle: 'active' | 'completed' = 'active'
): Promise<
  CursorPage<{
    momentId: string;
    title: string;
    status: string;
    groupFamily: string;
    momentTypeCode: string;
    participantCount: number;
  }>
> {
  const safeLimit = Math.min(Math.max(limit, 1), 100);
  const params: unknown[] = [ctx.userId, safeLimit + 1];
  let cursorClause = '';
  if (cursor) {
    cursorClause = 'AND m.updated_at < $3::timestamptz';
    params.push(cursor);
  }
  const statusClause =
    lifecycle === 'completed'
      ? `m.status = 'COMPLETED'`
      : `(
         m.status = 'ACTIVE'
         OR (m.status = 'DRAFT' AND (gmc.organizer_user_id = $1 OR m.created_by_user_id = $1))
       )`;
  const rows = await client.query<{
    moment_id: string;
    title: string;
    status: string;
    group_family: string;
    moment_type_code: string;
    updated_at: Date;
    participant_count: string;
  }>(
    `SELECT m.moment_id, m.title, m.status, gmc.group_family, mt.code AS moment_type_code, m.updated_at,
            (
              SELECT COUNT(*)::text
              FROM collaboration.moment_participant mpc
              WHERE mpc.moment_id = m.moment_id AND mpc.status = 'ACTIVE'
            ) AS participant_count
     FROM collaboration.group_moment_context gmc
     JOIN core.moment m ON m.moment_id = gmc.moment_id
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     JOIN collaboration.moment_participant mp ON mp.moment_id = m.moment_id AND mp.user_id = $1
     WHERE mp.status = 'ACTIVE'
       AND ${statusClause} ${cursorClause}
     ORDER BY m.updated_at DESC
     LIMIT $2`,
    params
  );
  const hasMore = rows.rows.length > safeLimit;
  const slice = rows.rows.slice(0, safeLimit);
  return {
    items: slice.map((r) => ({
      momentId: r.moment_id,
      title: r.title,
      status: r.status,
      groupFamily: r.group_family,
      momentTypeCode: r.moment_type_code,
      participantCount: Number(r.participant_count) || 0,
    })),
    nextCursor: hasMore ? slice[slice.length - 1].updated_at.toISOString() : null,
  };
}

const PULSE_POSITION_TOP = 20;
const FINANCE_POSITION_DEFAULT_LIMIT = 50;

export async function getGroupMomentProjection(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  facet: 'pulse' | 'life' | 'memory' | 'finance' | 'actions'
): Promise<Record<string, unknown>> {
  if (facet === 'actions') {
    await assertGroupMember(client, ctx, momentId);
    const actions = await getAvailableActions(client, ctx, { momentId, domain: 'GROUP' });
    return { momentId, availableActions: actions.actions.map((a) => ({ ...a, enabled: true })) };
  }

  if (facet === 'life' || facet === 'memory') {
    await assertGroupMember(client, ctx, momentId);
    const row = await client.query<{ title: string; group_family: string }>(
      `SELECT m.title, gmc.group_family
       FROM collaboration.group_moment_context gmc
       JOIN core.moment m ON m.moment_id = gmc.moment_id
       WHERE gmc.moment_id = $1`,
      [momentId]
    );
    const title = row.rows[0]?.title ?? '';
    const groupFamily = row.rows[0]?.group_family ?? '';
    if (facet === 'life') {
      /**
       * life-v1-provisional metrics — derived from live collab/finance tables.
       * Not analytics.metric_current; formulas may change under metricVersion bumps.
       */
      const [planning, bookings, updates, participants, financeSnap, polls, purchases, residents] =
        await Promise.all([
          client.query<{
            planning_item_id: string;
            title: string;
            due_at: Date | null;
            status: string;
            created_at: Date;
            category_code: string | null;
            location: string | null;
            priority_code: string | null;
          }>(
            `SELECT planning_item_id, title, due_at, status, created_at,
                    category_code, location, priority_code
             FROM collaboration.planning_item
             WHERE moment_id = $1 ORDER BY COALESCE(due_at, created_at) ASC LIMIT 50`,
            [momentId]
          ),
          client.query<{
            booking_id: string;
            provider_name: string | null;
            status: string;
            created_at: Date;
          }>(
            `SELECT booking_id, provider_name, status, created_at FROM collaboration.booking
             WHERE moment_id = $1 ORDER BY COALESCE(start_at, booked_at, created_at) ASC LIMIT 50`,
            [momentId]
          ),
          client.query<{
            group_update_id: string;
            body: string;
            created_at: Date;
            urgency_code: string;
          }>(
            `SELECT group_update_id, body, created_at, urgency_code FROM collaboration.group_update
             WHERE moment_id = $1 ORDER BY created_at DESC LIMIT 20`,
            [momentId]
          ),
          client.query<{ c: string }>(
            `SELECT COUNT(*)::text AS c FROM collaboration.moment_participant
             WHERE moment_id = $1 AND status = 'ACTIVE'`,
            [momentId]
          ),
          client.query<{
            currency_code: string;
            outstanding_total: string;
            expense_total: string;
            budget_total: string;
            contribution_total: string;
          }>(
            `SELECT currency_code,
                    outstanding_total::text,
                    expense_total::text,
                    budget_total::text,
                    contribution_total::text
             FROM projection.group_finance_snapshot
             WHERE moment_id = $1 LIMIT 1`,
            [momentId]
          ),
          client.query<{ c: string }>(
            `SELECT COUNT(*)::text AS c FROM shared.poll WHERE moment_id = $1`,
            [momentId]
          ),
          client.query<{ c: string }>(
            `SELECT COUNT(*)::text AS c FROM collaboration.purchase_item WHERE moment_id = $1`,
            [momentId]
          ),
          client.query<{ c: string }>(
            `SELECT COUNT(*)::text AS c FROM collaboration.resident WHERE moment_id = $1`,
            [momentId]
          ),
        ]);

      const planningCount = planning.rows.length;
      const bookingCount = bookings.rows.length;
      const updateCount = updates.rows.length;
      const openTaskCount = planning.rows.filter((r) => r.status === 'OPEN' || r.status === 'IN_PROGRESS').length;
      const doneTaskCount = planning.rows.filter(
        (r) => r.status === 'DONE' || r.status === 'COMPLETED' || r.status === 'CLOSED'
      ).length;
      const participantCount = Number(participants.rows[0]?.c ?? 0);
      const pollCount = Number(polls.rows[0]?.c ?? 0);
      const purchaseItemCount = Number(purchases.rows[0]?.c ?? 0);
      const residentCount = Number(residents.rows[0]?.c ?? 0);
      const finance = financeSnap.rows[0] ?? null;
      const expenseTotal = finance ? Number(finance.expense_total) : 0;
      const budgetTotal = finance ? Number(finance.budget_total) : 0;
      const contributionTotal = finance ? Number(finance.contribution_total) : 0;
      const hasFinanceSignal = finance != null && (expenseTotal > 0 || contributionTotal > 0 || budgetTotal > 0);

      const experience = lifeDomainMetric(
        planningCount > 0 || updateCount > 0,
        50 + Math.min(planningCount, 10) * 3 + Math.min(updateCount, 10) * 3 + Math.min(participantCount, 8) * 2
      );
      const purchase = lifeDomainMetric(
        hasFinanceSignal || purchaseItemCount > 0,
        (() => {
          if (budgetTotal > 0) {
            const util = Math.min(1, expenseTotal / budgetTotal);
            return 55 + (1 - Math.abs(util - 0.65)) * 40 + Math.min(purchaseItemCount, 5) * 2;
          }
          return 50 + Math.min(contributionTotal > 0 ? 20 : 0, 20) + Math.min(purchaseItemCount, 8) * 5;
        })()
      );
      const living = lifeDomainMetric(
        residentCount > 0 || bookingCount > 0,
        45 + Math.min(residentCount, 8) * 6 + Math.min(bookingCount, 8) * 5
      );
      const goal = lifeDomainMetric(
        planningCount > 0,
        planningCount === 0
          ? 0
          : (doneTaskCount / planningCount) * 70 + Math.min(planningCount, 10) * 3
      );
      const community = lifeDomainMetric(
        participantCount > 0 || updateCount > 0 || pollCount > 0,
        40 + Math.min(participantCount, 10) * 4 + Math.min(updateCount, 8) * 3 + Math.min(pollCount, 5) * 5
      );

      const domains = { experience, purchase, living, goal, community };
      const scored = Object.values(domains)
        .map((d) => d.score)
        .filter((s): s is number => s != null);
      const healthScore =
        scored.length === 0 ? null : Math.round(scored.reduce((a, b) => a + b, 0) / scored.length);

      const balance = {
        participation: lifeBalanceBar(
          participantCount > 0,
          40 + Math.min(participantCount, 12) * 5,
          ['Needs Attention', 'Stable', 'Healthy', 'Optimal']
        ),
        contribution: lifeBalanceBar(
          hasFinanceSignal || contributionTotal > 0 || expenseTotal > 0,
          (() => {
            const funded = Math.max(contributionTotal, expenseTotal);
            if (funded <= 0) return 45;
            if (budgetTotal > 0) {
              return 50 + Math.min(40, Math.round((funded / budgetTotal) * 40));
            }
            return 50 + Math.min(20, Math.round((contributionTotal / Math.max(expenseTotal, 1)) * 30));
          })(),
          ['Needs Attention', 'Stable', 'Healthy', 'Optimal']
        ),
        coordination: lifeBalanceBar(
          updateCount > 0 || bookingCount > 0 || openTaskCount > 0,
          45 + Math.min(updateCount, 8) * 4 + Math.min(bookingCount, 6) * 3 + (openTaskCount > 0 ? 8 : 0),
          ['Needs Attention', 'Stable', 'On Track', 'Optimal']
        ),
        progress: lifeBalanceBar(
          planningCount > 0,
          planningCount === 0 ? 0 : 35 + (doneTaskCount / planningCount) * 55 + Math.min(planningCount, 8) * 2,
          ['Needs Attention', 'Stable', 'On Track', 'Optimal']
        ),
        community: lifeBalanceBar(
          participantCount > 0 || updateCount > 0,
          40 + Math.min(participantCount, 10) * 4 + Math.min(updateCount, 8) * 3,
          ['Needs Attention', 'Stable', 'Strong', 'Optimal']
        ),
      };

      const drivers = buildLifeDrivers(domains);

      const activity = [
        ...updates.rows.map((r) => ({
          kind: 'UPDATE' as const,
          id: r.group_update_id,
          title: r.body.slice(0, 80),
          at: r.created_at.toISOString(),
        })),
        ...planning.rows.map((r) => ({
          kind: 'PLAN' as const,
          id: r.planning_item_id,
          title: r.title,
          at: (r.due_at ?? r.created_at).toISOString(),
        })),
        ...bookings.rows.map((r) => ({
          kind: 'BOOKING' as const,
          id: r.booking_id,
          title: r.provider_name ?? 'Booking',
          at: r.created_at.toISOString(),
        })),
      ]
        .sort((a, b) => (a.at < b.at ? 1 : -1))
        .slice(0, 12);

      const hasLive =
        planningCount > 0 ||
        bookingCount > 0 ||
        updateCount > 0 ||
        finance != null ||
        pollCount > 0 ||
        purchaseItemCount > 0 ||
        residentCount > 0;

      return {
        momentId,
        facet,
        title,
        groupFamily,
        status: hasLive ? 'OK' : 'EMPTY',
        payload: {
          dataQuality: hasLive ? 'LIVE' : 'EMPTY',
          metricVersion: 'life-v1-provisional',
          sections: {
            planning: planningCount > 0 ? 'LIVE' : 'EMPTY',
            participation: participantCount > 0 ? 'LIVE' : 'EMPTY',
            operations: bookingCount > 0 || residentCount > 0 ? 'LIVE' : 'EMPTY',
            finance: finance != null ? 'LIVE' : 'EMPTY',
          },
          openTaskCount,
          participantCount,
          counts: {
            participantCount,
            openTaskCount,
            planningCount,
            bookingCount,
            updateCount,
            pollCount,
            purchaseItemCount,
            residentCount,
            expenseTotal: finance?.expense_total ?? null,
            budgetTotal: finance?.budget_total ?? null,
            contributionTotal: finance?.contribution_total ?? null,
          },
          domains,
          health: {
            score: healthScore,
            label: healthScore == null ? '—' : String(healthScore),
          },
          balance,
          drivers,
          activity,
          planningItems: planning.rows.map((r) => ({
            planningItemId: r.planning_item_id,
            title: r.title,
            dueAt: r.due_at?.toISOString() ?? null,
            status: r.status,
            categoryCode: r.category_code,
            location: r.location,
            priorityCode: r.priority_code,
            createdAt: r.created_at.toISOString(),
          })),
          bookings: bookings.rows.map((r) => ({
            bookingId: r.booking_id,
            title: r.provider_name,
            status: r.status,
          })),
          updates: updates.rows.map((r) => ({
            updateId: r.group_update_id,
            message: r.body,
            createdAt: r.created_at.toISOString(),
            urgencyCode: r.urgency_code ?? 'NORMAL',
          })),
          financeHint: finance
            ? {
                currencyCode: finance.currency_code,
                outstandingTotal: finance.outstanding_total,
              }
            : null,
        },
      };
    }
    const memories = await client.query<{
      memory_id: string;
      title: string | null;
      occurred_at: Date | null;
      memory_type: string;
    }>(
      `SELECT memory_id, title, occurred_at, memory_type FROM memory.memory
       WHERE moment_id = $1 AND status = 'ACTIVE'
       ORDER BY COALESCE(occurred_at, created_at) DESC
       LIMIT 100`,
      [momentId]
    );
    const memoryIds = memories.rows.map((r) => r.memory_id);
    const [mediaByMemory, mediaCounts] = await Promise.all([
      listMediaForMemories(client, memoryIds, null),
      countMediaForMemories(client, memoryIds),
    ]);
    const items = memories.rows.map((r) => {
      const media = mediaByMemory.get(r.memory_id) ?? [];
      return {
        memoryId: r.memory_id,
        title: r.title,
        occurredAt: r.occurred_at?.toISOString() ?? null,
        memoryType: r.memory_type,
        media,
        mediaCount: mediaCounts.get(r.memory_id) ?? media.length,
      };
    });
    return {
      momentId,
      facet,
      title,
      groupFamily,
      status: items.length > 0 ? 'OK' : 'EMPTY',
      payload: {
        dataQuality: items.length > 0 ? 'LIVE' : 'EMPTY',
        items,
        memoryCount: items.length,
      },
    };
  }

  if (facet === 'finance' || facet === 'pulse') {
    // S9-G-OPT: membership + meta + finance in one RTT (was assert + meta + N finance queries).
    const mode = facet === 'pulse' ? 'pulse' : 'finance';
    const limit = mode === 'pulse' ? PULSE_POSITION_TOP : FINANCE_POSITION_DEFAULT_LIMIT;
    const semantics =
      mode === 'pulse' ? 'viewer_plus_top_by_abs_net' : 'top_by_abs_net_paginated_default_50';

    const bundled = await client.query<{
      participant_id: string;
      title: string;
      group_family: string;
      participant_count: string;
      attention_count: number | null;
      task_open_count: number | null;
      widget_payload: Record<string, unknown> | null;
      totals: Array<Record<string, unknown>> | null;
      positions: Array<Record<string, unknown>> | null;
      viewer: Array<Record<string, unknown>> | null;
      position_total_count: string;
    }>(
      `SELECT mp.participant_id, m.title, gmc.group_family,
              (SELECT COUNT(*)::text FROM collaboration.moment_participant x
               WHERE x.moment_id = $1 AND x.status = 'ACTIVE') AS participant_count,
              gp.attention_count, gp.task_open_count, gp.widget_payload,
              (SELECT COALESCE(jsonb_agg(t ORDER BY t->>'currencyCode'), '[]'::jsonb)
               FROM (
                 SELECT jsonb_build_object(
                   'currencyCode', currency_code,
                   'expenseTotal', expense_total::text,
                   'budgetTotal', budget_total::text,
                   'contributionTotal', contribution_total::text,
                   'settledTotal', settled_total::text,
                   'outstandingTotal', outstanding_total::text,
                   'expenseCount', COALESCE((snapshot_payload->>'expenseCount')::int, 0)
                 ) AS t
                 FROM projection.group_finance_snapshot WHERE moment_id = $1
               ) s
              ) AS totals,
              (SELECT COALESCE(jsonb_agg(p), '[]'::jsonb)
               FROM (
                 SELECT jsonb_build_object(
                   'participantId', gfp.participant_id,
                   'displayName', COALESCE(up.display_name, ep.display_name, mp_pos.metadata->>'displayName'),
                   'currencyCode', gfp.currency_code,
                   'paidTotal', gfp.paid_total::text,
                   'allocatedTotal', gfp.allocated_total::text,
                   'contributionTotal', gfp.contribution_total::text,
                   'payableTotal', gfp.payable_total::text,
                   'receivableTotal', gfp.receivable_total::text,
                   'settledTotal', gfp.settled_total::text,
                   'netPosition', gfp.net_position::text
                 ) AS p
                 FROM projection.group_finance_position gfp
                 LEFT JOIN collaboration.moment_participant mp_pos
                   ON mp_pos.moment_id = gfp.moment_id AND mp_pos.participant_id = gfp.participant_id
                 LEFT JOIN core.user_profile up ON up.user_id = mp_pos.user_id
                 LEFT JOIN core.external_party ep ON ep.external_party_id = mp_pos.external_party_id
                 WHERE gfp.moment_id = $1
                 ORDER BY ABS(gfp.net_position) DESC, gfp.currency_code, gfp.participant_id
                 LIMIT $3
               ) x
              ) AS positions,
              (SELECT COALESCE(jsonb_agg(v), '[]'::jsonb)
               FROM (
                 SELECT jsonb_build_object(
                   'participantId', gfp.participant_id,
                   'displayName', COALESCE(up.display_name, ep.display_name, mp_view.metadata->>'displayName'),
                   'currencyCode', gfp.currency_code,
                   'paidTotal', gfp.paid_total::text,
                   'allocatedTotal', gfp.allocated_total::text,
                   'contributionTotal', gfp.contribution_total::text,
                   'payableTotal', gfp.payable_total::text,
                   'receivableTotal', gfp.receivable_total::text,
                   'settledTotal', gfp.settled_total::text,
                   'netPosition', gfp.net_position::text
                 ) AS v
                 FROM projection.group_finance_position gfp
                 LEFT JOIN collaboration.moment_participant mp_view
                   ON mp_view.moment_id = gfp.moment_id AND mp_view.participant_id = gfp.participant_id
                 LEFT JOIN core.user_profile up ON up.user_id = mp_view.user_id
                 LEFT JOIN core.external_party ep ON ep.external_party_id = mp_view.external_party_id
                 WHERE gfp.moment_id = $1 AND gfp.participant_id = mp.participant_id
                 ORDER BY ABS(gfp.net_position) DESC, ABS(gfp.paid_total) + ABS(gfp.allocated_total) DESC, gfp.currency_code
                 LIMIT 20
               ) y
              ) AS viewer,
              (SELECT COUNT(*)::text FROM projection.group_finance_position WHERE moment_id = $1) AS position_total_count
       FROM collaboration.group_moment_context gmc
       JOIN core.moment m ON m.moment_id = gmc.moment_id AND m.domain_code = 'GROUP'
       JOIN collaboration.moment_participant mp
         ON mp.moment_id = gmc.moment_id AND mp.user_id = $2 AND mp.status = 'ACTIVE'
       LEFT JOIN projection.group_pulse gp ON gp.moment_id = gmc.moment_id
       WHERE gmc.moment_id = $1`,
      [momentId, ctx.userId, limit]
    );
    if (!bundled.rows[0]) {
      throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active member of this group moment.', 403);
    }
    const row = bundled.rows[0];
    const totalsRaw = (row.totals ?? []) as Array<Record<string, unknown>>;
    const mappedPositions = (row.positions ?? []) as Array<Record<string, unknown>>;
    const viewerRows = (row.viewer ?? []) as Array<Record<string, unknown>>;
    const positionTotalCount = parseInt(row.position_total_count ?? '0', 10);
    const expenseCount = totalsRaw.reduce((acc, t) => acc + Number(t.expenseCount ?? 0), 0);
    const totals = totalsRaw.map(({ expenseCount: _e, ...rest }) => rest);
    const empty = totals.length === 0 && positionTotalCount === 0;
    // viewerPosition = primary visual emphasis only (max abs net). Full positions[]/totals[] remain authoritative.
    const viewerPosition = empty ? null : (viewerRows[0] ?? null);
    const financePayload = {
      dataQuality: empty ? ('EMPTY' as const) : ('OK' as const),
      expenseCount: empty ? 0 : expenseCount,
      totals: empty ? [] : totals,
      positions: empty ? [] : mappedPositions,
      viewerPosition,
      positionTotalCount: empty ? 0 : positionTotalCount,
      positionsTruncated: !empty && positionTotalCount > mappedPositions.length,
      positionsSemantics: semantics,
    };
    const title = row.title ?? '';
    const groupFamily = row.group_family ?? '';
    if (facet === 'finance') {
      return {
        momentId,
        facet,
        title,
        groupFamily,
        status: financePayload.dataQuality === 'EMPTY' ? 'EMPTY' : 'OK',
        payload: financePayload,
      };
    }
    return {
      momentId,
      facet,
      title,
      groupFamily,
      status: financePayload.dataQuality === 'EMPTY' && row.attention_count == null ? 'EMPTY' : 'OK',
      payload: {
        dataQuality: financePayload.dataQuality,
        participantCount: parseInt(row.participant_count ?? '0', 10),
        attentionCount: row.attention_count ?? 0,
        openTaskCount: row.task_open_count ?? 0,
        widgetPayload: row.widget_payload ?? {},
        finance: financePayload,
      },
    };
  }

  return {
    momentId,
    facet,
    payload: {},
  };
}

export async function listBusinessMoments(
  client: PoolClient,
  ctx: RequestContext,
  cursor: string | undefined,
  limit: number
): Promise<
  CursorPage<{ momentId: string; title: string; status: string; businessFamily: string; companyId: string }>
> {
  const safeLimit = Math.min(Math.max(limit, 1), 100);
  const params: unknown[] = [ctx.userId, safeLimit + 1];
  let cursorClause = '';
  if (cursor) {
    cursorClause = 'AND m.updated_at < $3::timestamptz';
    params.push(cursor);
  }
  const rows = await client.query<{
    moment_id: string;
    title: string;
    status: string;
    business_family: string;
    company_id: string;
    updated_at: Date;
  }>(
    `SELECT m.moment_id, m.title, m.status, bmc.business_family, bmc.company_id, m.updated_at
     FROM business.business_moment_context bmc
     JOIN core.moment m ON m.moment_id = bmc.moment_id
     JOIN business.company_membership cm ON cm.company_id = bmc.company_id AND cm.user_id = $1
     WHERE cm.status = 'ACTIVE'
       AND (
         m.status = 'ACTIVE'
         OR (m.status = 'DRAFT' AND m.created_by_user_id = $1)
       ) ${cursorClause}
     ORDER BY m.updated_at DESC
     LIMIT $2`,
    params
  );
  const hasMore = rows.rows.length > safeLimit;
  const slice = rows.rows.slice(0, safeLimit);
  return {
    items: slice.map((r) => ({
      momentId: r.moment_id,
      title: r.title,
      status: r.status,
      businessFamily: r.business_family,
      companyId: r.company_id,
    })),
    nextCursor: hasMore ? slice[slice.length - 1].updated_at.toISOString() : null,
  };
}

export async function getBusinessMomentProjection(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  facet: 'pulse' | 'life' | 'memory' | 'finance' | 'actions'
): Promise<Record<string, unknown>> {
  if (facet === 'pulse' || facet === 'finance') {
    return getBusinessPulseOrFinance(client, ctx, momentId, facet);
  }

  const { assertCompanyMomentAccess } = await import('../business/membership');
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  if (facet === 'actions') {
    const actions = await getAvailableActions(client, ctx, { momentId, domain: 'BUSINESS' });
    return {
      momentId,
      companyId: scope.companyId,
      businessFamily: scope.businessFamily,
      availableActions: actions.actions.map((a) => ({ ...a, enabled: true })),
    };
  }

  const row = await client.query<{ title: string }>(
    `SELECT m.title FROM core.moment m WHERE m.moment_id = $1`,
    [momentId]
  );
  const title = row.rows[0]?.title ?? '';

  if (facet === 'life') {
    const life = await client.query<{
      team_operations_payload: Record<string, unknown>;
      runway_payload: Record<string, unknown>;
      business_operations_payload: Record<string, unknown>;
      vendor_operations_payload: Record<string, unknown>;
    }>(
      `SELECT team_operations_payload, runway_payload, business_operations_payload,
              vendor_operations_payload
       FROM projection.business_life WHERE company_id = $1`,
      [scope.companyId]
    );
    const teamPayload = { ...(life.rows[0]?.team_operations_payload ?? {}) };
    const runwayPayload = { ...(life.rows[0]?.runway_payload ?? {}) };
    const opsPayload = { ...(life.rows[0]?.business_operations_payload ?? {}) };
    const vendorPayload = life.rows[0]?.vendor_operations_payload ?? {};
    const teamActive = Object.keys(teamPayload).length > 0;
    const runwayActive = Object.keys(runwayPayload).length > 0;
    const opsActive = Object.keys(opsPayload).length > 0;
    const vendorActive = Object.keys(vendorPayload).length > 0;
    const has = !!life.rows[0] && (teamActive || runwayActive || opsActive || vendorActive);

    const [pulseRows, moduleRows, signalRows, activityRows, journeyRows, trends] = await Promise.all([
      client.query<{
        attention_count: number;
        active_moment_count: number;
        runway_months: string | null;
        financial_health_score: string | null;
      }>(
        `SELECT attention_count, active_moment_count, runway_months::text,
                financial_health_score::text
         FROM projection.business_pulse WHERE company_id = $1`,
        [scope.companyId]
      ),
      client.query<{ n: string }>(
        `SELECT COUNT(DISTINCT business_family)::text AS n
         FROM business.business_moment_context
         WHERE company_id = $1 AND status = 'ACTIVE'`,
        [scope.companyId]
      ),
      client.query<{
        issue_id: string;
        title: string;
        severity: string;
        status: string;
        business_family: string | null;
      }>(
        `SELECT i.issue_id, i.title, i.severity, i.status, bmc.business_family
         FROM business.issue i
         LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = i.moment_id
         WHERE i.company_id = $1
           AND i.status IN ('OPEN', 'IN_PROGRESS', 'BLOCKED')
         ORDER BY
           CASE i.severity
             WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3
           END,
           i.opened_at DESC
         LIMIT 6`,
        [scope.companyId]
      ),
      client.query<{
        activity_code: string;
        title: string;
        occurred_at: Date;
        activity_payload: Record<string, unknown> | null;
        business_family: string | null;
      }>(
        `SELECT ra.activity_code, ra.title, ra.occurred_at, ra.activity_payload, bmc.business_family
         FROM projection.recent_activity ra
         JOIN business.business_moment_context bmc
           ON bmc.moment_id = ra.scope_id::uuid AND bmc.company_id = $1 AND bmc.status = 'ACTIVE'
         WHERE ra.scope_type = 'MOMENT'
           AND ra.domain_code = 'BUSINESS'
         ORDER BY ra.occurred_at DESC
         LIMIT 8`,
        [scope.companyId]
      ),
      client.query<{
        family_code: string;
        title: string;
        created_at: Date;
      }>(
        `SELECT DISTINCT ON (family_code) family_code, title, created_at
         FROM business.business_system_setup
         WHERE company_id = $1 AND status = 'ACTIVE'
         ORDER BY family_code, created_at ASC`,
        [scope.companyId]
      ),
      loadLifeTrendSeries(client, scope.companyId),
    ]);

    const pulse = pulseRows.rows[0];
    const activeModuleCount = Number(moduleRows.rows[0]?.n ?? 0);

    const moduleScores = await computeLifeModuleScores(
      client,
      ctx,
      scope.companyId,
      pulse
    );

    const signals = await assembleTypedSignals(
      client,
      ctx,
      scope.companyId,
      signalRows.rows
    );

    const activity = activityRows.rows.map((r) => ({
      activityCode: r.activity_code,
      title: r.title,
      occurredAt: r.occurred_at.toISOString(),
      family: mapFamily(r.business_family),
      description:
        typeof r.activity_payload?.description === 'string'
          ? r.activity_payload.description
          : null,
    }));

    const journey = journeyRows.rows
      .map((r) => ({
        familyCode: r.family_code,
        family: mapFamily(r.family_code),
        title: r.title,
        createdAt: r.created_at.toISOString(),
      }))
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));

    return {
      momentId,
      facet,
      title,
      companyId: scope.companyId,
      businessFamily: scope.businessFamily,
      status: has || activeModuleCount > 0 || signals.length > 0 || activity.length > 0 ? 'OK' : 'EMPTY',
      payload: {
        dataQuality: has || activeModuleCount > 0 ? 'OK' : 'EMPTY',
        sections: {
          teamOperations: teamActive ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
          runway: runwayActive ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
          businessOperations: opsActive ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
          vendorOperations: vendorActive ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
          healthTrends: trends.series.length >= 2 ? 'OK' : trends.series.length === 1 ? 'EMPTY_SUPPORTED' : 'EMPTY_SUPPORTED',
        },
        teamOperationsPayload: teamPayload,
        runwayPayload,
        businessOperationsPayload: opsPayload,
        vendorOperationsPayload: vendorPayload,
        kpis: {
          activeModuleCount,
          activeMomentCount: pulse?.active_moment_count ?? 0,
          runwayMonths: pulse?.runway_months ?? null,
          financialHealthScore: pulse?.financial_health_score ?? null,
          attentionCount: pulse?.attention_count ?? 0,
        },
        modules: {
          teamOperations: {
            active: teamActive,
            statusLabel:
              typeof teamPayload.statusLabel === 'string' ? teamPayload.statusLabel : null,
            score: formatScore(moduleScores.teamScore),
          },
          runway: {
            active: runwayActive,
            statusLabel:
              typeof runwayPayload.statusLabel === 'string' ? runwayPayload.statusLabel : null,
            runwayMonths: pulse?.runway_months ?? null,
            score: formatScore(moduleScores.runwayScore),
            revenueMomPct: moduleScores.revenueMomPct,
            expenseMomPct: moduleScores.expenseMomPct,
          },
          businessOperations: {
            active: opsActive,
            statusLabel:
              typeof opsPayload.statusLabel === 'string' ? opsPayload.statusLabel : null,
            score: formatScore(moduleScores.opsScore),
          },
          vendorOperations: {
            active: vendorActive,
            statusLabel:
              typeof vendorPayload.statusLabel === 'string' ? vendorPayload.statusLabel : null,
            score: formatScore(moduleScores.vendorScore),
          },
        },
        signals,
        activity,
        journey,
        trends,
      },
    };
  }

  if (facet === 'memory') {
    const memory = await client.query<{
      memory_count: number;
      recent_memory_payload: Record<string, unknown>;
    }>(
      `SELECT memory_count, recent_memory_payload FROM projection.business_memory WHERE company_id = $1`,
      [scope.companyId]
    );
    const count = memory.rows[0]?.memory_count ?? 0;
    const payload = memory.rows[0]?.recent_memory_payload ?? {};
    const { mergeBusinessMemoryFamilies } = await import('../business/business-projection');
    const merged = mergeBusinessMemoryFamilies(payload);
    let items: Array<Record<string, unknown>> = merged.items;
    let riskCount = merged.riskCount;
    let successCount = merged.successCount;
    if (!merged.hasFamilies) {
      const live = await client.query<{
        memory_id: string;
        title: string;
        summary: string | null;
        memory_type: string;
        occurred_at: Date | null;
        business_family: string;
      }>(
        `SELECT m.memory_id, m.title, m.summary, m.memory_type, m.occurred_at, bmc.business_family
         FROM memory.memory m
         JOIN business.business_moment_context bmc
           ON bmc.moment_id = m.moment_id AND bmc.company_id = $1 AND bmc.status = 'ACTIVE'
         WHERE m.status = 'ACTIVE'
         ORDER BY
           CASE bmc.business_family
             WHEN 'BUSINESS_RUNWAY' THEN 0
             WHEN 'BUSINESS_OPERATIONS' THEN 1
             WHEN 'TEAM_OPERATIONS' THEN 2
             ELSE 3
           END,
           COALESCE(m.occurred_at, m.created_at) DESC,
           m.memory_id DESC
         LIMIT 50`,
        [scope.companyId]
      );
      items = live.rows.map((r) => ({
        memoryId: r.memory_id,
        title: r.title,
        body: r.summary,
        memoryType: r.memory_type,
        occurredAt: r.occurred_at?.toISOString() ?? null,
        businessFamily: r.business_family,
      }));
      riskCount = 0;
      successCount = 0;
    }
    const itemCount = items.length;
    return {
      momentId,
      facet,
      title,
      companyId: scope.companyId,
      businessFamily: scope.businessFamily,
      status: itemCount > 0 ? 'OK' : 'EMPTY',
      payload: {
        dataQuality: itemCount > 0 ? 'OK' : 'EMPTY',
        memoryCount: merged.hasFamilies ? itemCount : count || itemCount,
        items,
        recentMemoryPayload: payload,
        patternCount: payload.patternCount ?? 0,
        learningCount: merged.hasFamilies ? itemCount : (payload.learningCount ?? count),
        riskCount,
        successCount,
      },
    };
  }

  return {
    momentId,
    facet,
    payload: {},
  };
}

/** S9-G-OPT parity: membership + title + finance + pulse + activity preview in one RTT. */
async function getBusinessPulseOrFinance(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  facet: 'pulse' | 'finance'
): Promise<Record<string, unknown>> {
  const bundled = await client.query<{
    company_id: string;
    business_family: string;
    title: string;
    attention_count: number | null;
    active_moment_count: number | null;
    runway_months: string | null;
    financial_health_score: string | null;
    widget_payload: Record<string, unknown> | null;
    totals: Array<Record<string, unknown>> | null;
    snapshot_payload: Record<string, unknown> | null;
    activity: Array<Record<string, unknown>> | null;
  }>(
    `SELECT bmc.company_id, bmc.business_family, m.title,
            gp.attention_count, gp.active_moment_count, gp.runway_months::text,
            gp.financial_health_score::text, gp.widget_payload,
            (SELECT COALESCE(jsonb_agg(t ORDER BY t->>'currencyCode'), '[]'::jsonb)
             FROM (
               SELECT jsonb_build_object(
                 'currencyCode', currency_code,
                 'expenseTotal', expense_total::text,
                 'revenueTotal', revenue_total::text,
                 'invoiceOutstandingTotal', invoice_outstanding_total::text
               ) AS t
               FROM projection.business_finance_snapshot WHERE company_id = bmc.company_id
             ) s
            ) AS totals,
            (SELECT snapshot_payload FROM projection.business_finance_snapshot
             WHERE company_id = bmc.company_id LIMIT 1) AS snapshot_payload,
            (SELECT COALESCE(jsonb_agg(act.a ORDER BY act.occurred_at DESC, act.recent_activity_id DESC), '[]'::jsonb)
             FROM (
               SELECT jsonb_build_object(
                 'activityCode', ra.activity_code,
                 'title', ra.title,
                 'occurredAt', to_char(ra.occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                 'activityPayload', COALESCE(ra.activity_payload, '{}'::jsonb),
                 'actorDisplayName', NULLIF(btrim(up.display_name), '')
               ) AS a,
               ra.occurred_at,
               ra.recent_activity_id
               FROM projection.recent_activity ra
               LEFT JOIN core.user_profile up ON up.user_id = ra.user_id
               WHERE ra.scope_type = 'MOMENT'
                 AND ra.scope_id = bmc.moment_id
                 AND ra.domain_code = 'BUSINESS'
                 AND EXISTS (
                   SELECT 1 FROM business.business_moment_context gate
                   WHERE gate.moment_id = ra.scope_id
                     AND gate.company_id = bmc.company_id
                     AND gate.status = 'ACTIVE'
                 )
               ORDER BY ra.occurred_at DESC, ra.recent_activity_id DESC
               LIMIT 5
             ) act
            ) AS activity
     FROM business.business_moment_context bmc
     JOIN core.moment m ON m.moment_id = bmc.moment_id AND m.domain_code = 'BUSINESS'
     JOIN business.company_membership cm
       ON cm.company_id = bmc.company_id AND cm.user_id = $2 AND cm.status = 'ACTIVE'
     LEFT JOIN projection.business_pulse gp ON gp.company_id = bmc.company_id
     WHERE bmc.moment_id = $1 AND bmc.status = 'ACTIVE'`,
    [momentId, ctx.userId]
  );
  if (!bundled.rows[0]) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active company member for this business moment.', 403);
  }
  const row = bundled.rows[0];
  const totals = (row.totals ?? []) as Array<Record<string, unknown>>;
  const financePayload = {
    dataQuality: totals.length ? ('OK' as const) : ('EMPTY' as const),
    totals,
    snapshotPayload: row.snapshot_payload ?? {},
  };
  const title = row.title ?? '';
  if (facet === 'finance') {
    return {
      momentId,
      facet,
      title,
      companyId: row.company_id,
      businessFamily: row.business_family,
      status: financePayload.dataQuality === 'EMPTY' ? 'EMPTY' : 'OK',
      payload: financePayload,
    };
  }

  const family = (row.business_family ?? '').toUpperCase();
  const isOps = family.includes('OPERATIONS') && !family.includes('TEAM');
  let operations: Record<string, unknown> | undefined;
  if (isOps) {
    const { loadOpsPulseExtras } = await import('../business/operations-precision');
    operations = await loadOpsPulseExtras(client, row.company_id, momentId);
  }

  const hasPulse = row.attention_count != null || row.active_moment_count != null;
  return {
    momentId,
    facet: 'pulse',
    title,
    companyId: row.company_id,
    businessFamily: row.business_family,
    status: financePayload.dataQuality === 'EMPTY' && !hasPulse && !operations ? 'EMPTY' : 'OK',
    payload: {
      dataQuality: financePayload.dataQuality,
      attentionCount: row.attention_count ?? 0,
      activeMomentCount: row.active_moment_count ?? 0,
      runwayMonths: row.runway_months ?? null,
      financialHealthScore: row.financial_health_score ?? null,
      widgetPayload: row.widget_payload ?? {},
      finance: financePayload,
      activity: row.activity ?? [],
      ...(operations ? { operations } : {}),
    },
  };
}

export async function getBusinessMomentActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  cursor: string | undefined,
  limit: number
): Promise<
  CursorPage<{
    activityCode: string;
    title: string;
    occurredAt: string;
    activityPayload: Record<string, unknown>;
    actorDisplayName: string | null;
  }>
> {
  const { assertCompanyMomentAccess } = await import('../business/membership');
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const safeLimit = Math.min(Math.max(limit, 1), 50);
  let cursorOccurredAt: string | null = null;
  let cursorId: string | null = null;
  if (cursor) {
    const parts = cursor.split('|');
    if (parts.length === 2) {
      cursorOccurredAt = parts[0];
      cursorId = parts[1];
    }
  }

  const rows = await client.query<{
    activity_code: string;
    title: string;
    occurred_at: Date;
    recent_activity_id: string;
    activity_payload: Record<string, unknown> | null;
    actor_display_name: string | null;
  }>(
    `SELECT ra.activity_code, ra.title, ra.occurred_at, ra.recent_activity_id, ra.activity_payload,
            NULLIF(btrim(up.display_name), '') AS actor_display_name
     FROM projection.recent_activity ra
     LEFT JOIN core.user_profile up ON up.user_id = ra.user_id
     WHERE ra.scope_type = 'MOMENT'
       AND ra.scope_id = $1::uuid
       AND ra.domain_code = 'BUSINESS'
       AND EXISTS (
         SELECT 1 FROM business.business_moment_context gate
         WHERE gate.moment_id = ra.scope_id
           AND gate.company_id = $2::uuid
           AND gate.status = 'ACTIVE'
       )
       AND (
         $3::timestamptz IS NULL
         OR (ra.occurred_at, ra.recent_activity_id) < ($3::timestamptz, $4::uuid)
       )
     ORDER BY ra.occurred_at DESC, ra.recent_activity_id DESC
     LIMIT $5`,
    [momentId, scope.companyId, cursorOccurredAt, cursorId, safeLimit + 1]
  );
  const hasMore = rows.rows.length > safeLimit;
  const slice = rows.rows.slice(0, safeLimit);
  const items = slice.map((r) => ({
    activityCode: r.activity_code,
    title: r.title,
    occurredAt: r.occurred_at.toISOString(),
    activityPayload: r.activity_payload ?? {},
    actorDisplayName: r.actor_display_name,
  }));
  const last = slice[slice.length - 1];
  const nextCursor =
    hasMore && last ? `${last.occurred_at.toISOString()}|${last.recent_activity_id}` : null;
  return { items, nextCursor };
}

export async function getLife360(_client: PoolClient, userId: string): Promise<{ userId: string; circles: unknown[] }> {
  return { userId, circles: [] };
}

export async function getAvailableActions(
  _client: PoolClient,
  _ctx: RequestContext,
  scope: { momentId?: string; domain: 'PERSONAL' | 'GROUP' | 'BUSINESS' }
): Promise<{ actions: Array<{ actionCode: string; label: string }> }> {
  const common = [
    { actionCode: 'EXPENSE_CREATE', label: 'Expense' },
    { actionCode: 'TASK_CREATE', label: 'Task' },
    { actionCode: 'GOAL_CREATE', label: 'Goal' },
    { actionCode: 'POLL_CREATE', label: 'Poll' },
  ];
  if (scope.domain === 'GROUP') {
    return {
      actions: [
        ...common,
        { actionCode: 'PLANNING_ITEM_CREATE', label: 'Planning' },
        { actionCode: 'BOOKING_CREATE', label: 'Booking' },
        { actionCode: 'CONTRIBUTION_RECORD', label: 'Contribution' },
        { actionCode: 'GROUP_UPDATE_POST', label: 'Update' },
      ],
    };
  }
  if (scope.domain === 'BUSINESS') {
    return {
      actions: [
        ...common,
        { actionCode: 'INVOICE_CREATE', label: 'Invoice' },
        { actionCode: 'REVENUE_RECORD', label: 'Revenue' },
      ],
    };
  }
  return {
    actions: [
      { actionCode: 'EXPENSE_CREATE', label: 'Expense' },
      { actionCode: 'GOAL_CREATE', label: 'Goal' },
      { actionCode: 'MOVEMENT_RECORD', label: 'Transfer / Savings' },
      { actionCode: 'LIFE_OBSERVATION_RECORD', label: 'Life Ops Adjust' },
    ],
  };
}

export async function getMomentActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  cursor: string | undefined,
  limit: number
): Promise<
  CursorPage<{
    activityCode: string;
    title: string;
    occurredAt: string;
    activityPayload: Record<string, unknown>;
    actorDisplayName: string | null;
  }>
> {
  await assertGroupMember(client, ctx, momentId);

  const safeLimit = Math.min(Math.max(limit, 1), 50);
  let cursorOccurredAt: string | null = null;
  let cursorId: string | null = null;
  if (cursor) {
    const parts = cursor.split('|');
    if (parts.length === 2) {
      cursorOccurredAt = parts[0];
      cursorId = parts[1];
    }
  }

  const rows = await client.query<{
    activity_code: string;
    title: string;
    occurred_at: Date;
    recent_activity_id: string;
    activity_payload: Record<string, unknown> | null;
    actor_display_name: string | null;
  }>(
    // S9-G-OPT: moment-scoped activity (membership already asserted). Dedupes legacy
    // per-member fan-out rows by source_event_id so one event appears once.
    // Actor comes from domain_event (not recent_activity.user_id, which is fan-out recipient).
    `SELECT scoped.activity_code, scoped.title, scoped.occurred_at, scoped.recent_activity_id,
            scoped.activity_payload, up.display_name AS actor_display_name
     FROM (
       SELECT DISTINCT ON (source_event_id)
         activity_code, title, occurred_at, recent_activity_id, activity_payload, source_event_id
       FROM projection.recent_activity
       WHERE scope_type = 'MOMENT'
         AND scope_id = $1::uuid
         AND domain_code = 'GROUP'
       ORDER BY source_event_id, occurred_at DESC, recent_activity_id DESC
     ) scoped
     LEFT JOIN events.domain_event de ON de.domain_event_id = scoped.source_event_id
     LEFT JOIN core.user_profile up ON up.user_id = de.actor_user_id
     WHERE (
       $2::timestamptz IS NULL
       OR (scoped.occurred_at, scoped.recent_activity_id) < ($2::timestamptz, $3::uuid)
     )
     ORDER BY scoped.occurred_at DESC, scoped.recent_activity_id DESC
     LIMIT $4`,
    [momentId, cursorOccurredAt, cursorId, safeLimit + 1]
  );
  const hasMore = rows.rows.length > safeLimit;
  const slice = rows.rows.slice(0, safeLimit);
  const items = slice.map((r) => ({
    activityCode: r.activity_code,
    title: r.title,
    occurredAt: r.occurred_at.toISOString(),
    activityPayload: r.activity_payload ?? {},
    actorDisplayName: r.actor_display_name?.trim() || null,
  }));
  const last = slice[slice.length - 1];
  const nextCursor =
    hasMore && last ? `${last.occurred_at.toISOString()}|${last.recent_activity_id}` : null;
  return { items, nextCursor };
}
