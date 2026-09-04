import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGroupMember } from '../collaboration/group-membership';
import { listMediaForMemories } from '../memory/memory-attachments';
import { listMetricsForScope, PHASE7_PULSE_METRIC_CODES } from '../analytics/engine';
import {
  assembleTypedSignals,
  computeLifeModuleScores,
  formatScore,
  loadLifeTrendSeries,
  mapFamily,
} from '../business/business-life-enrichment';

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
    `SELECT moment_id, title, status, moment_type_code, display_rank
     FROM projection.personal_moments
     WHERE user_id = $1 AND ($2::int IS NULL OR display_rank > $2)
     ORDER BY display_rank ASC LIMIT $3`,
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

/** Personal Life Health dashboard — Figma `1047:7689` / body `1047:7707`. User-scoped, cross-moment. */
export interface PersonalLifeDto {
  userId: string;
  activeAreaCount: number;
  /** REAL after Personal join — honest empties / area counts; no invented Figma scores. */
  dataQuality: 'FIGMA_SEEDED' | 'REAL';
  sectionQuality: Record<string, LifeSectionQuality>;
  score: number;
  scoreMax: number;
  statusLabel: string;
  trendLabel: string;
  insight: string;
  areaScores: { code: string; label: string; score: number; color: string }[];
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
    items: { icon: string; title: string; when: string; value: string; tone: 'up' | 'down' | 'neutral' }[];
  };
  aiInsights: { title: string; lead: string; body: string };
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
  activeAreaCount: 'REAL_DATA',
};

/** Honest Life shell — no invented Figma scores. */
function honestEmptyLife(userId: string, activeAreaCount: number): PersonalLifeDto {
  return {
    userId,
    activeAreaCount,
    dataQuality: 'REAL',
    sectionQuality: { ...LIFE_HONEST_EMPTY_SECTION_QUALITY },
    score: 0,
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
    aiInsights: {
      title: 'Insights',
      lead: 'No insights yet',
      body: 'Insights appear after analytics refresh when consent is granted. Core Momentra works without them.',
    },
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
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

  const areaLabel: Record<string, { label: string; color: string }> = {
    LIFE_OPERATIONS: { label: 'Life Ops', color: '#3B82F6' },
    FUTURE_BUILDING: { label: 'Future', color: '#10B981' },
    LIFESTYLE: { label: 'Lifestyle', color: '#F59E0B' },
    RELATIONSHIPS: { label: 'Relations', color: '#E12A9E' },
  };
  const areaScores = areas.rows.slice(0, 4).map((r) => {
    const meta = areaLabel[r.system_code] ?? { label: r.system_code, color: '#8C8C9E' };
    return { code: r.system_code, label: meta.label, score: 0, color: meta.color };
  });

  const journeyRows = await client
    .query<{ title: string; occurred_at: Date; activity_code: string }>(
      `SELECT title, occurred_at, activity_code
       FROM projection.recent_activity
       WHERE user_id = $1
         AND COALESCE(activity_payload->>'status', 'POSTED') <> 'VOIDED'
       ORDER BY occurred_at DESC
       LIMIT 8`,
      [userId]
    )
    .catch(() => ({ rows: [] as Array<{ title: string; occurred_at: Date; activity_code: string }> }));

  const journeyItems = journeyRows.rows.map((r) => ({
    icon: '•',
    title: r.title,
    when: r.occurred_at.toISOString(),
    value: r.activity_code,
    tone: 'neutral' as const,
  }));

  const sectionQuality: Record<string, LifeSectionQuality> = {
    ...LIFE_HONEST_EMPTY_SECTION_QUALITY,
    activeAreaCount: 'REAL_DATA',
    areaScores: areaScores.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
    journey: journeyItems.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
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
      journey: { title: 'Life Journey', subtitle: journeyItems.length ? 'Recent activity' : 'Key shifts', items: journeyItems },
      sectionQuality,
      insight: row.title,
      aiInsights: { title: 'Insights', lead: row.title, body: row.body ?? row.title },
      updatedAt: row.generated_at.toISOString(),
    };
  }

  return {
    ...base,
    areaScores,
    journey: { title: 'Life Journey', subtitle: journeyItems.length ? 'Recent activity' : 'Key shifts', items: journeyItems },
    sectionQuality,
  };
}

export async function getPersonalMemory(
  client: PoolClient,
  userId: string
): Promise<{
  userId: string;
  items: Array<{ memoryId: string; title: string | null; occurredAt: string | null; momentId: string | null }>;
  memoryCount: number;
}> {
  const rows = await client
    .query<{ memory_id: string; title: string | null; occurred_at: Date | null; moment_id: string | null }>(
      `SELECT m.memory_id, m.title, m.occurred_at, m.moment_id
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
    .catch(() => ({ rows: [] as Array<{ memory_id: string; title: string | null; occurred_at: Date | null; moment_id: string | null }> }));

  const items = rows.rows.map((r) => ({
    memoryId: r.memory_id,
    title: r.title,
    occurredAt: r.occurred_at?.toISOString() ?? null,
    momentId: r.moment_id,
  }));
  return { userId, items, memoryCount: items.length };
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
  limit: number
): Promise<CursorPage<{ momentId: string; title: string; status: string; groupFamily: string; momentTypeCode: string }>> {
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
    group_family: string;
    moment_type_code: string;
    updated_at: Date;
  }>(
    `SELECT m.moment_id, m.title, m.status, gmc.group_family, mt.code AS moment_type_code, m.updated_at
     FROM collaboration.group_moment_context gmc
     JOIN core.moment m ON m.moment_id = gmc.moment_id
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     JOIN collaboration.moment_participant mp ON mp.moment_id = m.moment_id AND mp.user_id = $1
     WHERE mp.status = 'ACTIVE' AND m.status = 'ACTIVE' ${cursorClause}
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
          hasFinanceSignal || contributionTotal > 0,
          contributionTotal > 0 || expenseTotal > 0
            ? 50 + Math.min(20, Math.round((contributionTotal / Math.max(expenseTotal, 1)) * 30))
            : 45,
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
    }>(
      `SELECT memory_id, title, occurred_at FROM memory.memory
       WHERE moment_id = $1 AND status = 'ACTIVE'
       ORDER BY COALESCE(occurred_at, created_at) DESC
       LIMIT 100`,
      [momentId]
    );
    const mediaByMemory = await listMediaForMemories(
      client,
      memories.rows.map((r) => r.memory_id)
    );
    const items = memories.rows.map((r) => {
      const media = mediaByMemory.get(r.memory_id) ?? [];
      return {
        memoryId: r.memory_id,
        title: r.title,
        occurredAt: r.occurred_at?.toISOString() ?? null,
        media,
        mediaCount: media.length,
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
                   'participantId', participant_id,
                   'currencyCode', currency_code,
                   'paidTotal', paid_total::text,
                   'allocatedTotal', allocated_total::text,
                   'contributionTotal', contribution_total::text,
                   'payableTotal', payable_total::text,
                   'receivableTotal', receivable_total::text,
                   'settledTotal', settled_total::text,
                   'netPosition', net_position::text
                 ) AS p
                 FROM projection.group_finance_position
                 WHERE moment_id = $1
                 ORDER BY ABS(net_position) DESC, currency_code, participant_id
                 LIMIT $3
               ) x
              ) AS positions,
              (SELECT COALESCE(jsonb_agg(v), '[]'::jsonb)
               FROM (
                 SELECT jsonb_build_object(
                   'participantId', participant_id,
                   'currencyCode', currency_code,
                   'paidTotal', paid_total::text,
                   'allocatedTotal', allocated_total::text,
                   'contributionTotal', contribution_total::text,
                   'payableTotal', payable_total::text,
                   'receivableTotal', receivable_total::text,
                   'settledTotal', settled_total::text,
                   'netPosition', net_position::text
                 ) AS v
                 FROM projection.group_finance_position
                 WHERE moment_id = $1 AND participant_id = mp.participant_id
                 ORDER BY currency_code
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
    const financePayload = {
      dataQuality: empty ? ('EMPTY' as const) : ('OK' as const),
      expenseCount: empty ? 0 : expenseCount,
      totals: empty ? [] : totals,
      positions: empty ? [] : mappedPositions,
      viewerPosition: empty ? null : (viewerRows[0] ?? null),
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
     WHERE cm.status = 'ACTIVE' AND m.status = 'ACTIVE' ${cursorClause}
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
      momentId,
      scope.companyId,
      pulse,
      teamPayload,
      runwayPayload,
      opsPayload
    );

    const signals = await assembleTypedSignals(
      client,
      ctx,
      momentId,
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
    let items = (payload.items as Array<Record<string, unknown>> | undefined) ?? [];
    if (items.length === 0) {
      const live = await client.query<{ memory_id: string; title: string; summary: string | null }>(
        `SELECT m.memory_id, m.title, m.summary
         FROM memory.memory m
         WHERE m.moment_id = $1 AND m.status = 'ACTIVE'
         ORDER BY COALESCE(m.occurred_at, m.created_at) DESC
         LIMIT 50`,
        [momentId]
      );
      items = live.rows.map((r) => ({
        memoryId: r.memory_id,
        title: r.title,
        body: r.summary,
      }));
    }
    return {
      momentId,
      facet,
      title,
      companyId: scope.companyId,
      businessFamily: scope.businessFamily,
      status: count > 0 || items.length > 0 ? 'OK' : 'EMPTY',
      payload: {
        dataQuality: count > 0 || items.length > 0 ? 'OK' : 'EMPTY',
        memoryCount: count || items.length,
        items,
        recentMemoryPayload: payload,
        patternCount: payload.patternCount ?? 0,
        learningCount: payload.learningCount ?? count,
        riskCount: payload.riskCount ?? 0,
        successCount: payload.successCount ?? 0,
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
            (SELECT COALESCE(jsonb_agg(a), '[]'::jsonb)
             FROM (
               SELECT jsonb_build_object(
                 'activityCode', activity_code,
                 'title', title,
                 'occurredAt', to_char(occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                 'activityPayload', COALESCE(activity_payload, '{}'::jsonb)
               ) AS a
               FROM projection.recent_activity
               WHERE user_id = $2
                 AND scope_type = 'MOMENT'
                 AND scope_id = $1::uuid
                 AND domain_code = 'BUSINESS'
               ORDER BY occurred_at DESC, recent_activity_id DESC
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
  CursorPage<{ activityCode: string; title: string; occurredAt: string; activityPayload: Record<string, unknown> }>
> {
  const { assertCompanyMomentAccess } = await import('../business/membership');
  await assertCompanyMomentAccess(client, ctx, momentId);

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
       AND scope_type = 'MOMENT'
       AND scope_id = $2::uuid
       AND domain_code = 'BUSINESS'
       AND (
         $3::timestamptz IS NULL
         OR (occurred_at, recent_activity_id) < ($3::timestamptz, $4::uuid)
       )
     ORDER BY occurred_at DESC, recent_activity_id DESC
     LIMIT $5`,
    [ctx.userId, momentId, cursorOccurredAt, cursorId, safeLimit + 1]
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
  CursorPage<{ activityCode: string; title: string; occurredAt: string; activityPayload: Record<string, unknown> }>
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
  }>(
    // S9-G-OPT: moment-scoped activity (membership already asserted). Dedupes legacy
    // per-member fan-out rows by source_event_id so one event appears once.
    `SELECT activity_code, title, occurred_at, recent_activity_id, activity_payload
     FROM (
       SELECT DISTINCT ON (source_event_id)
         activity_code, title, occurred_at, recent_activity_id, activity_payload, source_event_id
       FROM projection.recent_activity
       WHERE scope_type = 'MOMENT'
         AND scope_id = $1::uuid
       ORDER BY source_event_id, occurred_at DESC, recent_activity_id DESC
     ) scoped
     WHERE (
       $2::timestamptz IS NULL
       OR (occurred_at, recent_activity_id) < ($2::timestamptz, $3::uuid)
     )
     ORDER BY occurred_at DESC, recent_activity_id DESC
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
  }));
  const last = slice[slice.length - 1];
  const nextCursor =
    hasMore && last ? `${last.occurred_at.toISOString()}|${last.recent_activity_id}` : null;
  return { items, nextCursor };
}
