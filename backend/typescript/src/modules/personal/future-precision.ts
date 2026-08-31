import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

/** Future Building precision (PX-1) — PER-FU widgets over V003 Future tables + V046 profile. */

async function assertPersonalMoment(client: PoolClient, ctx: RequestContext, momentId: string): Promise<void> {
  const row = await client.query(
    `SELECT 1 FROM personal.personal_moment_context WHERE moment_id = $1 AND user_id = $2`,
    [momentId, ctx.userId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
}

function clampScore(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function scoreFromCount(count: number): number | null {
  if (count <= 0) return null;
  return clampScore(40 + count * 12);
}

export const futureProfileSchema = z
  .object({
    buildingFocusCode: z.string().max(80).optional(),
    focusHorizonCode: z.string().max(80).optional(),
    progressRhythmCode: z.string().max(80).optional(),
    primaryValueCode: z.string().max(80).optional(),
    mainFrictionCode: z.string().max(80).optional(),
    momentumDriverCode: z.string().max(80).optional(),
    supportStyleCode: z.string().max(80).optional(),
    futureFeelCode: z.string().max(80).optional(),
  })
  .strict();

export const futureItemPrecisionSchema = z
  .object({
    kind: z.enum(['OPPORTUNITY', 'PIVOT', 'LEARNING', 'PROGRESS', 'MILESTONE']),
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    targetDate: z.string().date().optional(),
    progressValue: z.number().optional(),
    unitCode: z.string().max(50).optional(),
    opportunityType: z.string().max(80).optional(),
    pivotReason: z.string().max(2000).optional(),
    providerName: z.string().max(200).optional(),
    progressType: z.enum(['GOAL', 'MILESTONE', 'GENERAL']).optional(),
  })
  .strict();

export async function upsertFutureProfile(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof futureProfileSchema>
): Promise<{ momentId: string; updatedAt: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  await client.query(
    `INSERT INTO personal.future_building_profile (
       moment_id, user_id, building_focus_code, focus_horizon_code, progress_rhythm_code,
       primary_value_code, main_friction_code, momentum_driver_code, support_style_code, future_feel_code
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
     ON CONFLICT (moment_id) DO UPDATE SET
       building_focus_code = COALESCE(EXCLUDED.building_focus_code, personal.future_building_profile.building_focus_code),
       focus_horizon_code = COALESCE(EXCLUDED.focus_horizon_code, personal.future_building_profile.focus_horizon_code),
       progress_rhythm_code = COALESCE(EXCLUDED.progress_rhythm_code, personal.future_building_profile.progress_rhythm_code),
       primary_value_code = COALESCE(EXCLUDED.primary_value_code, personal.future_building_profile.primary_value_code),
       main_friction_code = COALESCE(EXCLUDED.main_friction_code, personal.future_building_profile.main_friction_code),
       momentum_driver_code = COALESCE(EXCLUDED.momentum_driver_code, personal.future_building_profile.momentum_driver_code),
       support_style_code = COALESCE(EXCLUDED.support_style_code, personal.future_building_profile.support_style_code),
       future_feel_code = COALESCE(EXCLUDED.future_feel_code, personal.future_building_profile.future_feel_code),
       version = personal.future_building_profile.version + 1,
       updated_at = now()`,
    [
      momentId,
      ctx.userId,
      body.buildingFocusCode ?? null,
      body.focusHorizonCode ?? null,
      body.progressRhythmCode ?? null,
      body.primaryValueCode ?? null,
      body.mainFrictionCode ?? null,
      body.momentumDriverCode ?? null,
      body.supportStyleCode ?? null,
      body.futureFeelCode ?? null,
    ]
  );
  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'FutureBuildingProfileUpserted',
    domainCode: 'PERSONAL',
    aggregateType: 'FUTURE_BUILDING_PROFILE',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { momentId, ...body },
  });
  return { momentId, updatedAt: new Date().toISOString() };
}

/** Enrich create path used by router when precision fields are present; falls back to thin insert. */
export async function createFutureItemPrecision(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof futureItemPrecisionSchema>
): Promise<{ itemId: string; kind: string; title: string }> {
  await assertPersonalMoment(client, ctx, momentId);

  let itemId: string;
  if (body.kind === 'OPPORTUNITY') {
    const r = await client.query<{ future_opportunity_id: string }>(
      `INSERT INTO personal.future_opportunity (moment_id, user_id, title, description, opportunity_type, target_date)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING future_opportunity_id`,
      [
        momentId,
        ctx.userId,
        body.title,
        body.description ?? null,
        body.opportunityType ?? null,
        body.targetDate ?? null,
      ]
    );
    itemId = r.rows[0]!.future_opportunity_id;
  } else if (body.kind === 'PIVOT') {
    const r = await client.query<{ future_pivot_id: string }>(
      `INSERT INTO personal.future_pivot (moment_id, user_id, title, description, pivot_reason, effective_date)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING future_pivot_id`,
      [
        momentId,
        ctx.userId,
        body.title,
        body.description ?? null,
        body.pivotReason ?? null,
        body.targetDate ?? null,
      ]
    );
    itemId = r.rows[0]!.future_pivot_id;
  } else if (body.kind === 'LEARNING') {
    const r = await client.query<{ future_learning_activity_id: string }>(
      `INSERT INTO personal.future_learning_activity (moment_id, user_id, title, description, provider_name, target_date)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING future_learning_activity_id`,
      [
        momentId,
        ctx.userId,
        body.title,
        body.description ?? null,
        body.providerName ?? null,
        body.targetDate ?? null,
      ]
    );
    itemId = r.rows[0]!.future_learning_activity_id;
  } else {
    const progressType =
      body.progressType ?? (body.kind === 'MILESTONE' ? 'MILESTONE' : 'GENERAL');
    const r = await client.query<{ future_progress_observation_id: string }>(
      `INSERT INTO personal.future_progress_observation (
         moment_id, user_id, progress_type, progress_value, unit_code, note
       ) VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING future_progress_observation_id`,
      [
        momentId,
        ctx.userId,
        progressType,
        body.progressValue ?? null,
        body.unitCode ?? null,
        body.description ?? body.title,
      ]
    );
    itemId = r.rows[0]!.future_progress_observation_id;
  }

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'FutureItemRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'FUTURE_ITEM',
    aggregateId: itemId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { itemId, momentId, kind: body.kind, title: body.title },
  });

  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'PERSONAL', 'MOMENT', $3, $4, $5, now(), $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      `${body.kind}_RECORDED`,
      body.title,
      JSON.stringify({ itemId, kind: body.kind, title: body.title }),
    ]
  );

  await refreshFuturePulseAxes(client, ctx.userId, momentId, domainEventId);
  return { itemId, kind: body.kind, title: body.title };
}

export async function refreshFuturePulseAxes(
  client: PoolClient,
  userId: string,
  momentId: string,
  sourceEventId: string
): Promise<void> {
  const axes = await computeFutureAxes(client, userId, momentId);
  const existing = await client.query<{ widget_payload: Record<string, unknown> | null }>(
    `SELECT widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  if (axes.visionScore != null) payload.visionScore = axes.visionScore;
  if (axes.growthScore != null) payload.growthScore = axes.growthScore;
  if (axes.momentumScore != null) payload.momentumScore = axes.momentumScore;
  if (axes.disciplineScore != null) payload.disciplineScore = axes.disciplineScore;
  payload.futureAxisSource = 'PRECISION_V046';
  payload.lastFutureRefreshAt = new Date().toISOString();

  const wellbeing = averageNullable([axes.visionScore, axes.growthScore, axes.momentumScore, axes.disciplineScore]);

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         recovery_score = COALESCE($2, recovery_score),
         rhythm_score = COALESCE($3, rhythm_score),
         wellbeing_score = COALESCE($4, wellbeing_score),
         widget_payload = $5::jsonb,
         source_event_id = $6,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, axes.growthScore, axes.momentumScore, wellbeing, JSON.stringify(payload), sourceEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, 0, $2, NULL, $3, $4, $5::jsonb, $6, 1)`,
      [userId, axes.growthScore, axes.momentumScore, wellbeing, JSON.stringify(payload), sourceEventId]
    );
  }
}

function averageNullable(parts: Array<number | null>): number | null {
  const nums = parts.filter((n): n is number => n != null);
  if (!nums.length) return null;
  return clampScore(nums.reduce((a, b) => a + b, 0) / nums.length);
}

async function computeFutureAxes(client: PoolClient, userId: string, momentId: string) {
  const counts = await client.query<{
    milestones: string;
    progress: string;
    learning: string;
    opportunities: string;
    pivots: string;
    learning_active: string;
    opportunities_pursuing: string;
  }>(
    `SELECT
       (SELECT COUNT(*)::text FROM personal.future_progress_observation
         WHERE moment_id=$1 AND user_id=$2 AND progress_type='MILESTONE') AS milestones,
       (SELECT COUNT(*)::text FROM personal.future_progress_observation
         WHERE moment_id=$1 AND user_id=$2 AND progress_type IN ('GENERAL','GOAL','MILESTONE')) AS progress,
       (SELECT COUNT(*)::text FROM personal.future_learning_activity
         WHERE moment_id=$1 AND user_id=$2 AND status <> 'ARCHIVED') AS learning,
       (SELECT COUNT(*)::text FROM personal.future_opportunity
         WHERE moment_id=$1 AND user_id=$2 AND status NOT IN ('ARCHIVED','DECLINED')) AS opportunities,
       (SELECT COUNT(*)::text FROM personal.future_pivot
         WHERE moment_id=$1 AND user_id=$2 AND status <> 'ARCHIVED') AS pivots,
       (SELECT COUNT(*)::text FROM personal.future_learning_activity
         WHERE moment_id=$1 AND user_id=$2 AND status='IN_PROGRESS') AS learning_active,
       (SELECT COUNT(*)::text FROM personal.future_opportunity
         WHERE moment_id=$1 AND user_id=$2 AND status='PURSUING') AS opportunities_pursuing`,
    [momentId, userId]
  );
  const c = counts.rows[0]!;
  const milestones = parseInt(c.milestones, 10);
  const progress = parseInt(c.progress, 10);
  const learning = parseInt(c.learning, 10);
  const opportunities = parseInt(c.opportunities, 10);
  const pivots = parseInt(c.pivots, 10);
  const disciplineSrc = parseInt(c.learning_active, 10) + parseInt(c.opportunities_pursuing, 10);

  return {
    visionScore: scoreFromCount(milestones + Math.max(0, progress - milestones)),
    growthScore: scoreFromCount(learning + opportunities),
    momentumScore: scoreFromCount(pivots + progress),
    disciplineScore: scoreFromCount(disciplineSrc),
    counts: { milestones, progress, learning, opportunities, pivots },
  };
}

export async function getFutureRuntimeSummary(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const axes = await computeFutureAxes(client, ctx.userId, momentId);
  const last = await client.query<{ last_at: Date | null }>(
    `SELECT MAX(t) AS last_at FROM (
       SELECT created_at AS t FROM personal.future_opportunity WHERE moment_id=$1 AND user_id=$2
       UNION ALL SELECT created_at FROM personal.future_pivot WHERE moment_id=$1 AND user_id=$2
       UNION ALL SELECT created_at FROM personal.future_learning_activity WHERE moment_id=$1 AND user_id=$2
       UNION ALL SELECT observed_at FROM personal.future_progress_observation WHERE moment_id=$1 AND user_id=$2
     ) x`,
    [momentId, ctx.userId]
  );
  const today = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM projection.recent_activity
     WHERE user_id=$1 AND scope_id=$2::uuid AND domain_code='PERSONAL'
       AND activity_code LIKE '%_RECORDED'
       AND occurred_at::date = (now() AT TIME ZONE 'UTC')::date
       AND activity_code IN ('OPPORTUNITY_RECORDED','PIVOT_RECORDED','LEARNING_RECORDED','PROGRESS_RECORDED','MILESTONE_RECORDED')`,
    [ctx.userId, momentId]
  );
  return {
    momentId,
    familyCode: 'FUTURE_BUILDING',
    entriesTodayCount: parseInt(today.rows[0]?.n ?? '0', 10),
    openOpportunityCount: axes.counts.opportunities,
    openLearningCount: axes.counts.learning,
    lastItemAt: last.rows[0]?.last_at?.toISOString() ?? null,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getFutureAxisSnapshot(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const axes = await computeFutureAxes(client, ctx.userId, momentId);
  return {
    momentId,
    familyCode: 'FUTURE_BUILDING',
    visionScore: axes.visionScore,
    growthScore: axes.growthScore,
    momentumScore: axes.momentumScore,
    disciplineScore: axes.disciplineScore,
    source: 'CANONICAL_COUNTS',
    counts: axes.counts,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getFutureInventory(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const [opps, pivots, learning, progress] = await Promise.all([
    client.query(
      `SELECT future_opportunity_id AS id, title, status, opportunity_type AS subtype, target_date, created_at
       FROM personal.future_opportunity WHERE moment_id=$1 AND user_id=$2 ORDER BY created_at DESC LIMIT 50`,
      [momentId, ctx.userId]
    ),
    client.query(
      `SELECT future_pivot_id AS id, title, status, pivot_reason AS subtype, effective_date AS target_date, created_at
       FROM personal.future_pivot WHERE moment_id=$1 AND user_id=$2 ORDER BY created_at DESC LIMIT 50`,
      [momentId, ctx.userId]
    ),
    client.query(
      `SELECT future_learning_activity_id AS id, title, status, provider_name AS subtype, target_date, created_at
       FROM personal.future_learning_activity WHERE moment_id=$1 AND user_id=$2 ORDER BY created_at DESC LIMIT 50`,
      [momentId, ctx.userId]
    ),
    client.query(
      `SELECT future_progress_observation_id AS id, note AS title, progress_type AS status,
              unit_code AS subtype, NULL::date AS target_date, observed_at AS created_at
       FROM personal.future_progress_observation WHERE moment_id=$1 AND user_id=$2 ORDER BY observed_at DESC LIMIT 50`,
      [momentId, ctx.userId]
    ),
  ]);

  const map = (kind: string, rows: typeof opps.rows) =>
    rows.map((r) => ({
      kind,
      id: r.id,
      title: r.title,
      status: r.status,
      subtype: r.subtype,
      targetDate: r.target_date,
      createdAt: r.created_at,
    }));

  return {
    momentId,
    familyCode: 'FUTURE_BUILDING',
    items: [
      ...map('OPPORTUNITY', opps.rows),
      ...map('PIVOT', pivots.rows),
      ...map('LEARNING', learning.rows),
      ...map('PROGRESS', progress.rows),
    ].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt))),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getFutureJourney(client: PoolClient, ctx: RequestContext, momentId: string) {
  const inv = await getFutureInventory(client, ctx, momentId);
  return {
    momentId,
    familyCode: 'FUTURE_BUILDING',
    events: inv.items.slice(0, 40).map((i) => ({
      occurredAt: i.createdAt,
      kind: i.kind,
      title: i.title,
      itemId: i.id,
    })),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}
