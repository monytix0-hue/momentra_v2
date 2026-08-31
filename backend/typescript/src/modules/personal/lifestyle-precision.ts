import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

/** Lifestyle precision (PX-2) — PER-LS widgets over lifestyle_activity + V046 profile. */

const LIFESTYLE_CONTEXTS = ['EXPERIENCE', 'WELLBEING', 'DISCOVERY', 'CREATION', 'LIFESTYLE'] as const;

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

export const lifestyleProfileSchema = z
  .object({
    lifestyleFocusCode: z.string().max(80).optional(),
    energyStyleCode: z.string().max(80).optional(),
    explorationBiasCode: z.string().max(80).optional(),
    wellbeingPriorityCode: z.string().max(80).optional(),
    rhythmCode: z.string().max(80).optional(),
    joyDriverCode: z.string().max(80).optional(),
  })
  .strict();

export const lifestyleActivityPrecisionSchema = z
  .object({
    lifestyleContext: z.enum(LIFESTYLE_CONTEXTS),
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    occurredAt: z.string().datetime().optional(),
    wellbeingRating: z.number().min(0).max(10).optional(),
    locationText: z.string().max(500).optional(),
    startAt: z.string().datetime().optional(),
    endAt: z.string().datetime().optional(),
  })
  .strict();

export async function upsertLifestyleProfile(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof lifestyleProfileSchema>
): Promise<{ momentId: string; updatedAt: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  await client.query(
    `INSERT INTO personal.lifestyle_profile (
       moment_id, user_id, lifestyle_focus_code, energy_style_code, exploration_bias_code,
       wellbeing_priority_code, rhythm_code, joy_driver_code
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
     ON CONFLICT (moment_id) DO UPDATE SET
       lifestyle_focus_code = COALESCE(EXCLUDED.lifestyle_focus_code, personal.lifestyle_profile.lifestyle_focus_code),
       energy_style_code = COALESCE(EXCLUDED.energy_style_code, personal.lifestyle_profile.energy_style_code),
       exploration_bias_code = COALESCE(EXCLUDED.exploration_bias_code, personal.lifestyle_profile.exploration_bias_code),
       wellbeing_priority_code = COALESCE(EXCLUDED.wellbeing_priority_code, personal.lifestyle_profile.wellbeing_priority_code),
       rhythm_code = COALESCE(EXCLUDED.rhythm_code, personal.lifestyle_profile.rhythm_code),
       joy_driver_code = COALESCE(EXCLUDED.joy_driver_code, personal.lifestyle_profile.joy_driver_code),
       version = personal.lifestyle_profile.version + 1,
       updated_at = now()`,
    [
      momentId,
      ctx.userId,
      body.lifestyleFocusCode ?? null,
      body.energyStyleCode ?? null,
      body.explorationBiasCode ?? null,
      body.wellbeingPriorityCode ?? null,
      body.rhythmCode ?? null,
      body.joyDriverCode ?? null,
    ]
  );
  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifestyleProfileUpserted',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFESTYLE_PROFILE',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { momentId, ...body },
  });
  return { momentId, updatedAt: new Date().toISOString() };
}

export async function createLifestyleActivityPrecision(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof lifestyleActivityPrecisionSchema>
): Promise<{ activityId: string; lifestyleContext: string; title: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const inserted = await client.query<{ lifestyle_activity_id: string }>(
    `INSERT INTO personal.lifestyle_activity (
       moment_id, user_id, lifestyle_context, title, description, occurred_at,
       start_at, end_at, location_text, wellbeing_rating, status
     ) VALUES ($1,$2,$3,$4,$5,COALESCE($6::timestamptz, now()),$7,$8,$9,$10,'ACTIVE')
     RETURNING lifestyle_activity_id`,
    [
      momentId,
      ctx.userId,
      body.lifestyleContext,
      body.title,
      body.description ?? null,
      body.occurredAt ?? null,
      body.startAt ?? null,
      body.endAt ?? null,
      body.locationText ?? null,
      body.wellbeingRating ?? null,
    ]
  );
  const activityId = inserted.rows[0]!.lifestyle_activity_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifestyleActivityRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFESTYLE_ACTIVITY',
    aggregateId: activityId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { activityId, momentId, lifestyleContext: body.lifestyleContext, title: body.title },
  });
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1,$2,'PERSONAL','MOMENT',$3,'LIFESTYLE_ACTIVITY_RECORDED',$4,now(),$5::jsonb,1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      body.title,
      JSON.stringify({ activityId, lifestyleContext: body.lifestyleContext }),
    ]
  );
  await refreshLifestylePulseAxes(client, ctx.userId, momentId, domainEventId);
  return { activityId, lifestyleContext: body.lifestyleContext, title: body.title };
}

async function computeLifestyleAxes(client: PoolClient, userId: string, momentId: string) {
  const row = await client.query<{
    experience: string;
    wellbeing: string;
    discovery: string;
    creation: string;
    lifestyle: string;
    avg_rating: string | null;
  }>(
    `SELECT
       COUNT(*) FILTER (WHERE lifestyle_context='EXPERIENCE')::text AS experience,
       COUNT(*) FILTER (WHERE lifestyle_context='WELLBEING')::text AS wellbeing,
       COUNT(*) FILTER (WHERE lifestyle_context='DISCOVERY')::text AS discovery,
       COUNT(*) FILTER (WHERE lifestyle_context='CREATION')::text AS creation,
       COUNT(*) FILTER (WHERE lifestyle_context='LIFESTYLE')::text AS lifestyle,
       AVG(wellbeing_rating)::text AS avg_rating
     FROM personal.lifestyle_activity
     WHERE moment_id=$1 AND user_id=$2 AND status IN ('ACTIVE','COMPLETED')`,
    [momentId, userId]
  );
  const c = row.rows[0]!;
  const experience = parseInt(c.experience, 10);
  const wellbeing = parseInt(c.wellbeing, 10);
  const discovery = parseInt(c.discovery, 10);
  const creation = parseInt(c.creation, 10);
  const lifestyle = parseInt(c.lifestyle, 10);
  const avgRating = c.avg_rating != null ? Number(c.avg_rating) : null;
  const joy = scoreFromCount(experience);
  let fulfillment = scoreFromCount(wellbeing);
  if (fulfillment != null && avgRating != null) {
    fulfillment = clampScore((fulfillment + avgRating * 10) / 2);
  }
  const exploration = scoreFromCount(discovery + creation);
  const vitalityParts = [joy, fulfillment, exploration, scoreFromCount(lifestyle)].filter(
    (n): n is number => n != null
  );
  const vitality =
    vitalityParts.length > 0
      ? clampScore(vitalityParts.reduce((a, b) => a + b, 0) / vitalityParts.length)
      : null;
  return {
    joyScore: joy,
    fulfillmentScore: fulfillment,
    vitalityScore: vitality,
    explorationScore: exploration,
    counts: { experience, wellbeing, discovery, creation, lifestyle },
    avgWellbeingRating: avgRating,
  };
}

export async function refreshLifestylePulseAxes(
  client: PoolClient,
  userId: string,
  momentId: string,
  sourceEventId: string
): Promise<void> {
  const axes = await computeLifestyleAxes(client, userId, momentId);
  const existing = await client.query<{ widget_payload: Record<string, unknown> | null }>(
    `SELECT widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  if (axes.joyScore != null) payload.joyScore = axes.joyScore;
  if (axes.fulfillmentScore != null) payload.fulfillmentScore = axes.fulfillmentScore;
  if (axes.vitalityScore != null) payload.vitalityScore = axes.vitalityScore;
  if (axes.explorationScore != null) payload.explorationScore = axes.explorationScore;
  payload.lifestyleAxisSource = 'PRECISION_V046';
  payload.lastLifestyleRefreshAt = new Date().toISOString();

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         wellbeing_score = COALESCE($2, wellbeing_score),
         widget_payload = $3::jsonb,
         source_event_id = $4,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, axes.vitalityScore, JSON.stringify(payload), sourceEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, 0, NULL, NULL, NULL, $2, $3::jsonb, $4, 1)`,
      [userId, axes.vitalityScore, JSON.stringify(payload), sourceEventId]
    );
  }
}

export async function getLifestyleRuntimeSummary(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const axes = await computeLifestyleAxes(client, ctx.userId, momentId);
  const total =
    axes.counts.experience +
    axes.counts.wellbeing +
    axes.counts.discovery +
    axes.counts.creation +
    axes.counts.lifestyle;
  return {
    momentId,
    familyCode: 'LIFESTYLE',
    activityCount: total,
    countsByContext: axes.counts,
    avgWellbeingRating: axes.avgWellbeingRating,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getLifestyleVitalitySnapshot(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const axes = await computeLifestyleAxes(client, ctx.userId, momentId);
  return {
    momentId,
    familyCode: 'LIFESTYLE',
    joyScore: axes.joyScore,
    fulfillmentScore: axes.fulfillmentScore,
    vitalityScore: axes.vitalityScore,
    explorationScore: axes.explorationScore,
    source: 'CANONICAL_COUNTS',
    counts: axes.counts,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getLifestyleInventory(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const rows = await client.query(
    `SELECT lifestyle_activity_id AS id, lifestyle_context, title, description, occurred_at,
            location_text, wellbeing_rating, status, created_at
     FROM personal.lifestyle_activity
     WHERE moment_id=$1 AND user_id=$2
     ORDER BY occurred_at DESC NULLS LAST, created_at DESC
     LIMIT 80`,
    [momentId, ctx.userId]
  );
  return {
    momentId,
    familyCode: 'LIFESTYLE',
    items: rows.rows,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getLifestyleJourney(client: PoolClient, ctx: RequestContext, momentId: string) {
  const inv = await getLifestyleInventory(client, ctx, momentId);
  return {
    momentId,
    familyCode: 'LIFESTYLE',
    events: inv.items.slice(0, 40).map((i) => ({
      occurredAt: i.occurred_at ?? i.created_at,
      context: i.lifestyle_context,
      title: i.title,
      activityId: i.id,
      wellbeingRating: i.wellbeing_rating,
    })),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}
