import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

const OBS_TYPES = ['RECOVERY', 'MOOD', 'RHYTHM', 'WELLBEING'] as const;
const LIFESTYLE_CONTEXTS = ['EXPERIENCE', 'WELLBEING', 'DISCOVERY', 'CREATION', 'LIFESTYLE'] as const;
const FUTURE_KINDS = ['OPPORTUNITY', 'PIVOT', 'LEARNING', 'PROGRESS', 'MILESTONE'] as const;

export const observationSchema = z
  .object({
    observationType: z.enum(OBS_TYPES),
    numericValue: z.number().optional(),
    textValue: z.string().max(2000).optional(),
    note: z.string().max(2000).optional(),
    observedAt: z.string().datetime().optional(),
    // Typed detail fields (V044) — optional until clients send them.
    activityTypeCode: z
      .enum(['REST', 'SLEEP', 'EXERCISE', 'MEDITATION', 'SOCIAL', 'MUSIC', 'OTHER'])
      .optional(),
    durationMinutes: z.number().int().min(0).optional(),
    energyBeforePct: z.number().min(0).max(100).optional(),
    energyAfterPct: z.number().min(0).max(100).optional(),
    feelingStateCode: z.enum(['GREAT', 'CALM', 'NEUTRAL', 'LOW', 'STRESSED', 'OTHER']).optional(),
    moodDrivers: z.array(z.string().regex(/^[A-Z][A-Z0-9_]*$/)).max(12).optional(),
  })
  .strict()
  .refine((b) => b.numericValue != null || b.textValue != null || b.note != null || b.feelingStateCode != null || b.activityTypeCode != null, {
    message: 'At least one of numericValue, textValue, note, feelingStateCode, or activityTypeCode is required.',
  });

export const futureItemSchema = z
  .object({
    kind: z.enum(FUTURE_KINDS),
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    targetDate: z.string().date().optional(),
    progressValue: z.number().optional(),
    unitCode: z.string().max(50).optional(),
  })
  .strict();

export const lifestyleActivitySchema = z
  .object({
    lifestyleContext: z.enum(LIFESTYLE_CONTEXTS),
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    occurredAt: z.string().datetime().optional(),
    wellbeingRating: z.number().min(0).max(10).optional(),
  })
  .strict();

export const updateLifestyleActivitySchema = z
  .object({
    title: z.string().min(1).max(500).optional(),
    description: z.string().max(5000).nullable().optional(),
    wellbeingRating: z.number().min(0).max(10).nullable().optional(),
  })
  .strict()
  .refine((b) => b.title != null || b.description !== undefined || b.wellbeingRating !== undefined, {
    message: 'At least one field is required.',
  });

export const relationshipActivitySchema = z
  .object({
    activityKind: z.enum(['CONNECTION', 'SUPPORT', 'SHARED_EXPERIENCE', 'INVESTMENT', 'INTERACTION']),
    displayName: z.string().min(1).max(200),
    note: z.string().max(2000).optional(),
    occurredAt: z.string().datetime().optional(),
  })
  .strict();

async function assertPersonalMoment(client: PoolClient, ctx: RequestContext, momentId: string): Promise<void> {
  const row = await client.query(
    `SELECT 1 FROM personal.personal_moment_context WHERE moment_id = $1 AND user_id = $2`,
    [momentId, ctx.userId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
}

export async function recordObservation(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof observationSchema>
): Promise<{ observationId: string; momentId: string; observationType: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const inserted = await client.query<{ life_operation_observation_id: string }>(
    `INSERT INTO personal.life_operation_observation (
       moment_id, user_id, observation_type, observed_at, numeric_value, text_value, note
     ) VALUES ($1, $2, $3, COALESCE($4::timestamptz, now()), $5, $6, $7)
     RETURNING life_operation_observation_id`,
    [
      momentId,
      ctx.userId,
      body.observationType,
      body.observedAt ?? null,
      body.numericValue ?? null,
      body.textValue ?? null,
      body.note ?? null,
    ]
  );
  const observationId = inserted.rows[0]!.life_operation_observation_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifeObservationRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_OBSERVATION',
    aggregateId: observationId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { observationId, momentId, observationType: body.observationType },
  });

  const activityTitle = observationActivityTitle(body);
  const activityCode = `${body.observationType}_RECORDED`;
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
      activityCode,
      activityTitle,
      JSON.stringify({
        observationId,
        observationType: body.observationType,
        numericValue: body.numericValue ?? null,
        textValue: body.textValue ?? null,
        note: body.note ?? null,
      }),
    ]
  );

  await bumpPersonalPulseAfterObservation(client, ctx.userId, domainEventId, body);

  // Attach typed Recovery/Mood details when present (V044 tables).
  try {
    const { attachObservationDetails } = await import('./life-ops-precision');
    await attachObservationDetails(client, observationId, body.observationType, {
      activityTypeCode: body.activityTypeCode,
      durationMinutes: body.durationMinutes,
      energyBeforePct: body.energyBeforePct,
      energyAfterPct: body.energyAfterPct,
      feelingStateCode: body.feelingStateCode,
      moodDrivers: body.moodDrivers,
    });
  } catch {
    // Tables may not be applied yet in older DBs — observation row still valid.
  }

  return { observationId, momentId, observationType: body.observationType };
}

function observationActivityTitle(body: z.infer<typeof observationSchema>): string {
  const label = body.textValue?.trim() || body.note?.trim();
  if (label) return label;
  switch (body.observationType) {
    case 'RECOVERY':
      return body.numericValue != null ? `Recovery · ${body.numericValue}` : 'Recovery logged';
    case 'MOOD':
      return 'Mood check-in';
    case 'RHYTHM':
      return 'Attention set';
    case 'WELLBEING':
      return 'Rhythm adjusted';
    default:
      return 'Observation logged';
  }
}

function clampScore(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function scoreFromNumeric(numericValue: number | undefined): number {
  if (numericValue == null || Number.isNaN(numericValue)) return 70;
  // Accept 0–10 sliders or 0–100 scores from clients.
  if (numericValue <= 10) return clampScore(numericValue * 10);
  return clampScore(numericValue);
}

async function bumpPersonalPulseAfterObservation(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  body: z.infer<typeof observationSchema>
): Promise<void> {
  const existing = await client.query<{
    attention_count: number;
    recovery_score: string | null;
    mood_state: string | null;
    rhythm_score: string | null;
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );

  let attentionCount = existing.rows[0]?.attention_count ?? 0;
  let recoveryScore = existing.rows[0]?.recovery_score != null ? Number(existing.rows[0].recovery_score) : null;
  let moodState = existing.rows[0]?.mood_state ?? null;
  let rhythmScore = existing.rows[0]?.rhythm_score != null ? Number(existing.rows[0].rhythm_score) : null;
  let wellbeingScore = existing.rows[0]?.wellbeing_score != null ? Number(existing.rows[0].wellbeing_score) : null;
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };

  switch (body.observationType) {
    case 'RECOVERY': {
      recoveryScore = scoreFromNumeric(body.numericValue);
      payload.lastRecoveryAt = new Date().toISOString();
      break;
    }
    case 'MOOD': {
      moodState = (body.textValue ?? body.note ?? 'Logged').trim().slice(0, 100);
      payload.lastMoodAt = new Date().toISOString();
      break;
    }
    case 'RHYTHM': {
      attentionCount += 1;
      rhythmScore = scoreFromNumeric(body.numericValue);
      payload.lastAttentionAt = new Date().toISOString();
      if (body.textValue) payload.lastAttentionTarget = body.textValue;
      break;
    }
    case 'WELLBEING': {
      wellbeingScore = scoreFromNumeric(body.numericValue);
      payload.lastAdjustAt = new Date().toISOString();
      break;
    }
  }

  // Blend recovery + rhythm when either changes; keep explicit WELLBEING if set alone.
  if (body.observationType === 'RECOVERY' || body.observationType === 'RHYTHM') {
    const parts: number[] = [];
    if (recoveryScore != null) parts.push(recoveryScore);
    if (rhythmScore != null) parts.push(rhythmScore);
    if (parts.length > 0) {
      wellbeingScore = clampScore(parts.reduce((a, b) => a + b, 0) / parts.length);
    }
  }

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         attention_count = $2,
         recovery_score = $3,
         mood_state = $4,
         rhythm_score = $5,
         wellbeing_score = $6,
         widget_payload = $7::jsonb,
         source_event_id = $8,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, 1)`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  }
}

export async function createFutureItem(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof futureItemSchema>
): Promise<{ itemId: string; kind: string; title: string }> {
  await assertPersonalMoment(client, ctx, momentId);

  let itemId: string;
  if (body.kind === 'OPPORTUNITY') {
    const r = await client.query<{ future_opportunity_id: string }>(
      `INSERT INTO personal.future_opportunity (moment_id, user_id, title, description, target_date)
       VALUES ($1, $2, $3, $4, $5) RETURNING future_opportunity_id`,
      [momentId, ctx.userId, body.title, body.description ?? null, body.targetDate ?? null]
    );
    itemId = r.rows[0]!.future_opportunity_id;
  } else if (body.kind === 'PIVOT') {
    const r = await client.query<{ future_pivot_id: string }>(
      `INSERT INTO personal.future_pivot (moment_id, user_id, title, description, effective_date)
       VALUES ($1, $2, $3, $4, $5) RETURNING future_pivot_id`,
      [momentId, ctx.userId, body.title, body.description ?? null, body.targetDate ?? null]
    );
    itemId = r.rows[0]!.future_pivot_id;
  } else if (body.kind === 'LEARNING') {
    const r = await client.query<{ future_learning_activity_id: string }>(
      `INSERT INTO personal.future_learning_activity (moment_id, user_id, title, description, target_date)
       VALUES ($1, $2, $3, $4, $5) RETURNING future_learning_activity_id`,
      [momentId, ctx.userId, body.title, body.description ?? null, body.targetDate ?? null]
    );
    itemId = r.rows[0]!.future_learning_activity_id;
  } else {
    // PROGRESS or MILESTONE → future_progress_observation
    const progressType = body.kind === 'MILESTONE' ? 'MILESTONE' : 'GENERAL';
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

  const activityCode = `${body.kind}_RECORDED`;
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
      activityCode,
      body.title,
      JSON.stringify({
        itemId,
        kind: body.kind,
        title: body.title,
        description: body.description ?? null,
        progressValue: body.progressValue ?? null,
      }),
    ]
  );

  await bumpPersonalPulseAfterFutureItem(client, ctx.userId, domainEventId, body);

  return { itemId, kind: body.kind, title: body.title };
}

async function bumpPersonalPulseAfterFutureItem(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  body: z.infer<typeof futureItemSchema>
): Promise<void> {
  const existing = await client.query<{
    attention_count: number;
    recovery_score: string | null;
    mood_state: string | null;
    rhythm_score: string | null;
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );

  let attentionCount = existing.rows[0]?.attention_count ?? 0;
  let recoveryScore = existing.rows[0]?.recovery_score != null ? Number(existing.rows[0].recovery_score) : null;
  let moodState = existing.rows[0]?.mood_state ?? null;
  let rhythmScore = existing.rows[0]?.rhythm_score != null ? Number(existing.rows[0].rhythm_score) : null;
  let wellbeingScore = existing.rows[0]?.wellbeing_score != null ? Number(existing.rows[0].wellbeing_score) : null;
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };

  const bump = body.progressValue != null ? scoreFromNumeric(body.progressValue) : 72;

  switch (body.kind) {
    case 'MILESTONE':
    case 'PROGRESS':
      // Vision ← wellbeing_score; Momentum ← rhythm_score
      wellbeingScore = bump;
      rhythmScore = clampScore(((rhythmScore ?? 60) + bump) / 2);
      payload.lastMilestoneAt = new Date().toISOString();
      payload.lastFutureKind = body.kind;
      break;
    case 'LEARNING':
    case 'OPPORTUNITY':
      // Growth ← recovery_score; Discipline via attention
      recoveryScore = bump;
      attentionCount += 1;
      payload.lastLearningAt = new Date().toISOString();
      payload.lastFutureKind = body.kind;
      break;
    case 'PIVOT':
      // Momentum ← rhythm_score
      rhythmScore = bump;
      payload.lastPivotAt = new Date().toISOString();
      payload.lastFutureKind = body.kind;
      break;
  }

  // Blended Future Score into wellbeing when axes exist
  const parts: number[] = [];
  if (wellbeingScore != null) parts.push(wellbeingScore);
  if (recoveryScore != null) parts.push(recoveryScore);
  if (rhythmScore != null) parts.push(rhythmScore);
  if (attentionCount > 0) parts.push(clampScore(40 + attentionCount * 8));
  if (parts.length > 0) {
    wellbeingScore = clampScore(parts.reduce((a, b) => a + b, 0) / parts.length);
  }

  // Store Future axis aliases in widget for clients that prefer explicit keys
  if (wellbeingScore != null) payload.visionScore = wellbeingScore;
  if (recoveryScore != null) payload.growthScore = recoveryScore;
  if (rhythmScore != null) payload.momentumScore = rhythmScore;
  if (attentionCount > 0) payload.disciplineScore = clampScore(40 + attentionCount * 8);

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         attention_count = $2,
         recovery_score = $3,
         mood_state = $4,
         rhythm_score = $5,
         wellbeing_score = $6,
         widget_payload = $7::jsonb,
         source_event_id = $8,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, 1)`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  }
}

export async function createLifestyleActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof lifestyleActivitySchema>
): Promise<{ activityId: string; lifestyleContext: string; title: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const r = await client.query<{ lifestyle_activity_id: string }>(
    `INSERT INTO personal.lifestyle_activity (
       moment_id, user_id, lifestyle_context, title, description, occurred_at, wellbeing_rating
     ) VALUES ($1, $2, $3, $4, $5, COALESCE($6::timestamptz, now()), $7)
     RETURNING lifestyle_activity_id`,
    [
      momentId,
      ctx.userId,
      body.lifestyleContext,
      body.title,
      body.description ?? null,
      body.occurredAt ?? null,
      body.wellbeingRating ?? null,
    ]
  );
  const activityId = r.rows[0]!.lifestyle_activity_id;

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifestyleActivityRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFESTYLE_ACTIVITY',
    aggregateId: activityId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      activityId,
      momentId,
      lifestyleContext: body.lifestyleContext,
      title: body.title,
    },
  });

  const activityCode = `${body.lifestyleContext}_RECORDED`;
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
      activityCode,
      body.title,
      JSON.stringify({
        activityId,
        lifestyleContext: body.lifestyleContext,
        title: body.title,
        description: body.description ?? null,
        wellbeingRating: body.wellbeingRating ?? null,
      }),
    ]
  );

  await bumpPersonalPulseAfterLifestyleActivity(client, ctx.userId, domainEventId, body);

  return {
    activityId,
    lifestyleContext: body.lifestyleContext,
    title: body.title,
  };
}

export async function updateLifestyleActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  activityId: string,
  body: z.infer<typeof updateLifestyleActivitySchema>
): Promise<{ activityId: string; lifestyleContext: string; title: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const existing = await client.query<{
    lifestyle_activity_id: string;
    lifestyle_context: string;
    title: string;
    description: string | null;
    wellbeing_rating: string | null;
  }>(
    `SELECT lifestyle_activity_id, lifestyle_context, title, description, wellbeing_rating
     FROM personal.lifestyle_activity
     WHERE lifestyle_activity_id = $1 AND moment_id = $2 AND user_id = $3`,
    [activityId, momentId, ctx.userId]
  );
  if (!existing.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Lifestyle activity not found.', 404);
  }

  const nextTitle = body.title ?? existing.rows[0].title;
  const nextDescription =
    body.description !== undefined ? body.description : existing.rows[0].description;
  const nextRating =
    body.wellbeingRating !== undefined
      ? body.wellbeingRating
      : existing.rows[0].wellbeing_rating != null
        ? Number(existing.rows[0].wellbeing_rating)
        : null;

  await client.query(
    `UPDATE personal.lifestyle_activity SET
       title = $2,
       description = $3,
       wellbeing_rating = $4,
       updated_at = now()
     WHERE lifestyle_activity_id = $1`,
    [activityId, nextTitle, nextDescription, nextRating]
  );

  await client.query(
    `UPDATE projection.recent_activity SET
       title = $3,
       activity_payload = COALESCE(activity_payload, '{}'::jsonb) || $4::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2
       AND activity_payload->>'activityId' = $5`,
    [
      ctx.userId,
      momentId,
      nextTitle,
      JSON.stringify({
        activityId,
        title: nextTitle,
        description: nextDescription,
        wellbeingRating: nextRating,
      }),
      activityId,
    ]
  );

  return {
    activityId,
    lifestyleContext: existing.rows[0].lifestyle_context,
    title: nextTitle,
  };
}

async function bumpPersonalPulseAfterLifestyleActivity(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  body: z.infer<typeof lifestyleActivitySchema>
): Promise<void> {
  const existing = await client.query<{
    attention_count: number;
    recovery_score: string | null;
    mood_state: string | null;
    rhythm_score: string | null;
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );

  let attentionCount = existing.rows[0]?.attention_count ?? 0;
  let recoveryScore = existing.rows[0]?.recovery_score != null ? Number(existing.rows[0].recovery_score) : null;
  let moodState = existing.rows[0]?.mood_state ?? null;
  let rhythmScore = existing.rows[0]?.rhythm_score != null ? Number(existing.rows[0].rhythm_score) : null;
  let wellbeingScore = existing.rows[0]?.wellbeing_score != null ? Number(existing.rows[0].wellbeing_score) : null;
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };

  const bump =
    body.wellbeingRating != null ? scoreFromNumeric(body.wellbeingRating * 10) : 72;

  switch (body.lifestyleContext) {
    case 'EXPERIENCE':
      // Joy ← recovery_score; Fulfillment ← wellbeing
      recoveryScore = bump;
      wellbeingScore = clampScore(((wellbeingScore ?? 60) + bump) / 2);
      moodState = body.title.slice(0, 40);
      payload.lastExperienceAt = new Date().toISOString();
      break;
    case 'WELLBEING':
      // Joy + Vitality
      recoveryScore = bump;
      rhythmScore = clampScore(((rhythmScore ?? 60) + bump) / 2);
      payload.lastWellbeingAt = new Date().toISOString();
      break;
    case 'DISCOVERY':
    case 'CREATION':
      // Exploration via attention; Vitality bump
      attentionCount += 1;
      rhythmScore = bump;
      payload.lastExplorationAt = new Date().toISOString();
      break;
    case 'LIFESTYLE':
      // Adjust → Fulfillment / Vitality blend
      wellbeingScore = bump;
      rhythmScore = clampScore(((rhythmScore ?? 60) + bump) / 2);
      payload.lastLifestyleAdjustAt = new Date().toISOString();
      break;
  }

  payload.lastLifestyleContext = body.lifestyleContext;

  const parts: number[] = [];
  if (recoveryScore != null) parts.push(recoveryScore);
  if (wellbeingScore != null) parts.push(wellbeingScore);
  if (rhythmScore != null) parts.push(rhythmScore);
  if (attentionCount > 0) parts.push(clampScore(40 + attentionCount * 8));
  if (parts.length > 0) {
    wellbeingScore = clampScore(parts.reduce((a, b) => a + b, 0) / parts.length);
  }

  // Lifestyle axis aliases
  if (recoveryScore != null) payload.joyScore = recoveryScore;
  if (wellbeingScore != null) payload.fulfillmentScore = wellbeingScore;
  if (rhythmScore != null) payload.vitalityScore = rhythmScore;
  if (attentionCount > 0) payload.explorationScore = clampScore(40 + attentionCount * 8);

  const experienceCount = Number(payload.experienceCount ?? 0) + (body.lifestyleContext === 'EXPERIENCE' ? 1 : 0);
  if (body.lifestyleContext === 'EXPERIENCE') payload.experienceCount = experienceCount;

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         attention_count = $2,
         recovery_score = $3,
         mood_state = $4,
         rhythm_score = $5,
         wellbeing_score = $6,
         widget_payload = $7::jsonb,
         source_event_id = $8,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, 1)`,
      [
        userId,
        attentionCount,
        recoveryScore,
        moodState,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  }
}

const ACTIVITY_TYPE_MAP: Record<string, string> = {
  SUPPORT: 'SUPPORT',
  SHARED_EXPERIENCE: 'SHARED_EXPERIENCE',
  INVESTMENT: 'RELATIONSHIP_INVESTMENT',
  INTERACTION: 'INTERACTION',
  CONNECTION: 'INTERACTION',
};

async function ensureRelationshipConnection(
  client: PoolClient,
  ctx: RequestContext,
  displayName: string
): Promise<string> {
  const existing = await client.query<{ relationship_connection_id: string }>(
    `SELECT relationship_connection_id FROM personal.relationship_connection
     WHERE user_id = $1 AND lower(display_name) = lower($2) AND status = 'ACTIVE'
     LIMIT 1`,
    [ctx.userId, displayName]
  );
  if (existing.rows[0]) {
    return existing.rows[0].relationship_connection_id;
  }

  const party = await client.query<{ external_party_id: string }>(
    `INSERT INTO core.external_party (party_type, display_name, status)
     VALUES ('PERSON', $1, 'ACTIVE')
     RETURNING external_party_id`,
    [displayName]
  );
  const conn = await client.query<{ relationship_connection_id: string }>(
    `INSERT INTO personal.relationship_connection (
       user_id, external_party_id, display_name, relationship_type, status
     ) VALUES ($1, $2, $3, 'PERSON', 'ACTIVE')
     RETURNING relationship_connection_id`,
    [ctx.userId, party.rows[0]!.external_party_id, displayName]
  );
  return conn.rows[0]!.relationship_connection_id;
}

export async function recordRelationshipActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof relationshipActivitySchema>
): Promise<{ activityId: string; displayName: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const connectionId = await ensureRelationshipConnection(client, ctx, body.displayName);

  const activityType = ACTIVITY_TYPE_MAP[body.activityKind] ?? 'INTERACTION';
  const r = await client.query<{ relationship_activity_id: string }>(
    `INSERT INTO personal.relationship_activity (
       moment_id, user_id, relationship_connection_id, activity_type, title, note, occurred_at
     ) VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7::timestamptz, now()))
     RETURNING relationship_activity_id`,
    [
      momentId,
      ctx.userId,
      connectionId,
      activityType,
      body.displayName,
      body.note ?? null,
      body.occurredAt ?? null,
    ]
  );
  const activityId = r.rows[0]!.relationship_activity_id;

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RelationshipActivityRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'RELATIONSHIP_ACTIVITY',
    aggregateId: activityId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      activityId,
      momentId,
      activityKind: body.activityKind,
      displayName: body.displayName,
    },
  });

  const activityTitle =
    body.note?.trim() ||
    `${body.activityKind.replace(/_/g, ' ').toLowerCase()} · ${body.displayName}`;
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
      `RELATIONSHIP_${body.activityKind}`,
      activityTitle,
      JSON.stringify({
        activityId,
        activityKind: body.activityKind,
        displayName: body.displayName,
        note: body.note ?? null,
      }),
    ]
  );

  // Real projection bump — bond axis aliases from recorded activity kinds (widget_payload).
  await bumpPersonalPulseAfterRelationshipActivity(client, ctx.userId, domainEventId, body.activityKind);

  return { activityId, displayName: body.displayName };
}

async function bumpPersonalPulseAfterRelationshipActivity(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  activityKind: string
): Promise<void> {
  const existing = await client.query<{
    attention_count: number;
    recovery_score: string | null;
    mood_state: string | null;
    rhythm_score: string | null;
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1`,
    [userId]
  );
  const row = existing.rows[0];
  const payload: Record<string, unknown> = {
    ...(row?.widget_payload ?? {}),
    lastRelationshipActivityAt: new Date().toISOString(),
    lastRelationshipActivityKind: activityKind,
  };
  const bump = 72;
  let wellbeingScore =
    row?.wellbeing_score != null ? parseFloat(row.wellbeing_score) : null;
  const rhythmScore =
    row?.rhythm_score != null ? parseFloat(row.rhythm_score) : null;
  const prevAxis = (key: string): number => {
    const v = Number(payload[key]);
    return Number.isFinite(v) && v > 0 ? v : 60;
  };

  const kind = activityKind.toUpperCase();
  switch (kind) {
    case 'CONNECTION':
    case 'INTERACTION':
      payload.trustScore = clampScore((prevAxis('trustScore') + bump) / 2);
      break;
    case 'SUPPORT':
      payload.careScore = clampScore((prevAxis('careScore') + bump) / 2);
      payload.supportScore = clampScore((prevAxis('supportScore') + bump) / 2);
      break;
    case 'SHARED_EXPERIENCE':
      payload.presenceScore = clampScore((prevAxis('presenceScore') + bump) / 2);
      break;
    case 'INVESTMENT':
    case 'RELATIONSHIP_INVESTMENT':
      wellbeingScore = clampScore(((wellbeingScore ?? 60) + bump) / 2);
      break;
    default:
      break;
  }

  const bondParts: number[] = [];
  for (const key of ['trustScore', 'careScore', 'supportScore', 'presenceScore']) {
    const v = Number(payload[key]);
    if (Number.isFinite(v) && v > 0) bondParts.push(v);
  }
  if (wellbeingScore != null) bondParts.push(wellbeingScore);
  if (bondParts.length > 0) {
    payload.bondIndex = clampScore(
      bondParts.reduce((a, b) => a + b, 0) / bondParts.length
    );
    if (wellbeingScore == null) {
      wellbeingScore = payload.bondIndex as number;
    }
  }

  const attentionCount = (row?.attention_count ?? 0) + 1;
  if (row) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         attention_count = $2,
         rhythm_score = $3,
         wellbeing_score = $4,
         widget_payload = $5::jsonb,
         source_event_id = $6,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [
        userId,
        attentionCount,
        rhythmScore,
        wellbeingScore,
        JSON.stringify(payload),
        sourceEventId,
      ]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, $2, NULL, NULL, NULL, $3, $4::jsonb, $5, 1)`,
      [userId, attentionCount, wellbeingScore, JSON.stringify(payload), sourceEventId]
    );
  }
}
