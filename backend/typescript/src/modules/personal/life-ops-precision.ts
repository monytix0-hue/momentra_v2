import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

/** Life Ops precision writers + RP-01..05 reads (pack V042–V045 / UI-frozen PER-LO). */

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

// ─── Schemas ─────────────────────────────────────────────────────────────────

export const lifeOpsProfileSchema = z
  .object({
    lifeFocusCode: z.string().max(80).optional(),
    dailyBalanceCode: z.string().max(80).optional(),
    currentRhythmCode: z.string().max(80).optional(),
    currentStateCode: z.string().max(80).optional(),
    primaryNeedCode: z.string().max(80).optional(),
    mainPressureCode: z.string().max(80).optional(),
    recoveryWindowCode: z.string().max(80).optional(),
    checkinCadenceCode: z.string().max(80).optional(),
    helpfulSupportCode: z.string().max(80).optional(),
    recoveryStyleCode: z.string().max(80).optional(),
    currentEnergyCode: z.string().max(80).optional(),
    wellbeingReminderCadenceCode: z.string().max(80).optional(),
    stressCheckinEnabled: z.boolean().optional(),
    recoveryCheckinEnabled: z.boolean().optional(),
    reviewCadenceCode: z.string().max(80).optional(),
    priorities: z
      .array(
        z.object({
          priorityCode: z.string().regex(/^[A-Z][A-Z0-9_]*$/),
          weightPct: z.number().min(0).max(100).optional(),
          selected: z.boolean().optional(),
        })
      )
      .max(20)
      .optional(),
    anchors: z
      .array(
        z.object({
          anchorCode: z.string().regex(/^[A-Z][A-Z0-9_]*$/).optional(),
          displayName: z.string().min(1).max(200),
          selected: z.boolean().optional(),
        })
      )
      .max(20)
      .optional(),
  })
  .strict();

export const attentionCaptureSchema = z
  .object({
    categoryCode: z.string().regex(/^[A-Z][A-Z0-9_]*$/),
    intensityCode: z.enum(['LIGHT', 'MODERATE', 'HEAVY']),
    timeBlockCode: z.enum(['MORNING', 'AFTERNOON', 'EVENING', 'NIGHT']),
    energyRemaining: z.number().int().min(0).max(5).optional(),
    observedAt: z.string().datetime().optional(),
    note: z.string().max(2000).optional(),
  })
  .strict();

export const lifeOpsAdjustSchema = z
  .object({
    rhythmActionCode: z.enum(['REDUCE_LOAD', 'INCREASE_INTENSITY', 'PAUSE', 'RESET']).optional(),
    signalDirectionCode: z.enum(['DECREASE_PRESSURE', 'MAINTAIN', 'INCREASE_PRESSURE']).optional(),
    reason: z.string().max(2000).optional(),
    occurredAt: z.string().datetime().optional(),
    priorityWeights: z
      .array(
        z.object({
          priorityCode: z.string().regex(/^[A-Z][A-Z0-9_]*$/),
          weightPct: z.number().min(0).max(100),
        })
      )
      .max(20)
      .optional(),
  })
  .strict()
  .refine(
    (b) =>
      b.rhythmActionCode != null ||
      b.signalDirectionCode != null ||
      (b.reason != null && b.reason.trim().length > 0) ||
      (b.priorityWeights != null && b.priorityWeights.length > 0),
    { message: 'At least one adjust field is required.' }
  );

export const observationDetailSchema = z
  .object({
    // Recovery detail (when observationType=RECOVERY)
    activityTypeCode: z
      .enum(['REST', 'SLEEP', 'EXERCISE', 'MEDITATION', 'SOCIAL', 'MUSIC', 'OTHER'])
      .optional(),
    durationMinutes: z.number().int().min(0).optional(),
    energyBeforePct: z.number().min(0).max(100).optional(),
    energyAfterPct: z.number().min(0).max(100).optional(),
    // Mood detail (when observationType=MOOD)
    feelingStateCode: z.enum(['GREAT', 'CALM', 'NEUTRAL', 'LOW', 'STRESSED', 'OTHER']).optional(),
    moodDrivers: z.array(z.string().regex(/^[A-Z][A-Z0-9_]*$/)).max(12).optional(),
  })
  .strict();

// ─── Writers ─────────────────────────────────────────────────────────────────

export async function upsertLifeOpsProfile(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof lifeOpsProfileSchema>
): Promise<{ momentId: string; version: number }> {
  await assertPersonalMoment(client, ctx, momentId);

  const upserted = await client.query<{ version: string }>(
    `INSERT INTO personal.life_operation_profile (
       moment_id, user_id,
       life_focus_code, daily_balance_code, current_rhythm_code, current_state_code,
       primary_need_code, main_pressure_code, recovery_window_code, checkin_cadence_code,
       helpful_support_code, recovery_style_code, current_energy_code,
       wellbeing_reminder_cadence_code, stress_checkin_enabled, recovery_checkin_enabled,
       review_cadence_code, version
     ) VALUES (
       $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,1
     )
     ON CONFLICT (moment_id) DO UPDATE SET
       life_focus_code = COALESCE(EXCLUDED.life_focus_code, personal.life_operation_profile.life_focus_code),
       daily_balance_code = COALESCE(EXCLUDED.daily_balance_code, personal.life_operation_profile.daily_balance_code),
       current_rhythm_code = COALESCE(EXCLUDED.current_rhythm_code, personal.life_operation_profile.current_rhythm_code),
       current_state_code = COALESCE(EXCLUDED.current_state_code, personal.life_operation_profile.current_state_code),
       primary_need_code = COALESCE(EXCLUDED.primary_need_code, personal.life_operation_profile.primary_need_code),
       main_pressure_code = COALESCE(EXCLUDED.main_pressure_code, personal.life_operation_profile.main_pressure_code),
       recovery_window_code = COALESCE(EXCLUDED.recovery_window_code, personal.life_operation_profile.recovery_window_code),
       checkin_cadence_code = COALESCE(EXCLUDED.checkin_cadence_code, personal.life_operation_profile.checkin_cadence_code),
       helpful_support_code = COALESCE(EXCLUDED.helpful_support_code, personal.life_operation_profile.helpful_support_code),
       recovery_style_code = COALESCE(EXCLUDED.recovery_style_code, personal.life_operation_profile.recovery_style_code),
       current_energy_code = COALESCE(EXCLUDED.current_energy_code, personal.life_operation_profile.current_energy_code),
       wellbeing_reminder_cadence_code = COALESCE(EXCLUDED.wellbeing_reminder_cadence_code, personal.life_operation_profile.wellbeing_reminder_cadence_code),
       stress_checkin_enabled = COALESCE(EXCLUDED.stress_checkin_enabled, personal.life_operation_profile.stress_checkin_enabled),
       recovery_checkin_enabled = COALESCE(EXCLUDED.recovery_checkin_enabled, personal.life_operation_profile.recovery_checkin_enabled),
       review_cadence_code = COALESCE(EXCLUDED.review_cadence_code, personal.life_operation_profile.review_cadence_code),
       version = personal.life_operation_profile.version + 1,
       updated_at = now()
     RETURNING version`,
    [
      momentId,
      ctx.userId,
      body.lifeFocusCode ?? null,
      body.dailyBalanceCode ?? null,
      body.currentRhythmCode ?? null,
      body.currentStateCode ?? null,
      body.primaryNeedCode ?? null,
      body.mainPressureCode ?? null,
      body.recoveryWindowCode ?? null,
      body.checkinCadenceCode ?? null,
      body.helpfulSupportCode ?? null,
      body.recoveryStyleCode ?? null,
      body.currentEnergyCode ?? null,
      body.wellbeingReminderCadenceCode ?? null,
      body.stressCheckinEnabled ?? null,
      body.recoveryCheckinEnabled ?? null,
      body.reviewCadenceCode ?? null,
    ]
  );

  if (body.priorities?.length) {
    for (const p of body.priorities) {
      await client.query(
        `UPDATE personal.life_operation_priority
         SET effective_to = now(), status = 'INACTIVE', updated_at = now()
         WHERE moment_id = $1 AND priority_code = $2 AND status = 'ACTIVE' AND effective_to IS NULL`,
        [momentId, p.priorityCode]
      );
      await client.query(
        `INSERT INTO personal.life_operation_priority (
           moment_id, user_id, priority_code, weight_pct, selected, status
         ) VALUES ($1,$2,$3,$4,COALESCE($5,TRUE),'ACTIVE')`,
        [momentId, ctx.userId, p.priorityCode, p.weightPct ?? null, p.selected ?? true]
      );
    }
  }

  if (body.anchors?.length) {
    for (const a of body.anchors) {
      await client.query(
        `INSERT INTO personal.life_operation_anchor (
           moment_id, user_id, anchor_code, display_name, selected, status
         ) VALUES ($1,$2,$3,$4,COALESCE($5,TRUE),'ACTIVE')
         ON CONFLICT (moment_id, display_name) DO UPDATE SET
           anchor_code = COALESCE(EXCLUDED.anchor_code, personal.life_operation_anchor.anchor_code),
           selected = EXCLUDED.selected,
           status = 'ACTIVE',
           updated_at = now()`,
        [momentId, ctx.userId, a.anchorCode ?? null, a.displayName, a.selected ?? true]
      );
    }
  }

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifeOpsProfileUpserted',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_OPS_PROFILE',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { momentId },
  });

  return { momentId, version: parseInt(upserted.rows[0]!.version, 10) };
}

export async function recordAttentionCapture(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof attentionCaptureSchema>
): Promise<{ attentionCaptureId: string; momentId: string }> {
  await assertPersonalMoment(client, ctx, momentId);

  const inserted = await client.query<{ attention_capture_id: string }>(
    `INSERT INTO analytics.attention_capture (
       moment_id, user_id, category_code, intensity_code, time_block_code,
       energy_remaining, observed_at, note
     ) VALUES ($1,$2,$3,$4,$5,$6,COALESCE($7::timestamptz, now()),$8)
     RETURNING attention_capture_id`,
    [
      momentId,
      ctx.userId,
      body.categoryCode,
      body.intensityCode,
      body.timeBlockCode,
      body.energyRemaining ?? null,
      body.observedAt ?? null,
      body.note ?? null,
    ]
  );
  const attentionCaptureId = inserted.rows[0]!.attention_capture_id;

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'AttentionCaptured',
    domainCode: 'PERSONAL',
    aggregateType: 'ATTENTION_CAPTURE',
    aggregateId: attentionCaptureId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      attentionCaptureId,
      momentId,
      categoryCode: body.categoryCode,
      intensityCode: body.intensityCode,
    },
  });

  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1,$2,'PERSONAL','MOMENT',$3,'ATTENTION_CAPTURED',$4,now(),$5::jsonb,1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      `Attention · ${body.categoryCode}`,
      JSON.stringify({
        attentionCaptureId,
        categoryCode: body.categoryCode,
        intensityCode: body.intensityCode,
        timeBlockCode: body.timeBlockCode,
        energyRemaining: body.energyRemaining ?? null,
      }),
    ]
  );

  // Bump pulse attention without wiping other widgetPayload keys.
  const existing = await client.query<{
    attention_count: number;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT attention_count, widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [ctx.userId]
  );
  const attentionCount = (existing.rows[0]?.attention_count ?? 0) + 1;
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  payload.lastAttentionAt = new Date().toISOString();
  payload.lastAttentionTarget = body.categoryCode;
  payload.lastAttentionIntensity = body.intensityCode;

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse
       SET attention_count = $2,
           widget_payload = $3::jsonb,
           source_event_id = $4,
           projection_version = projection_version + 1,
           updated_at = now()
       WHERE user_id = $1`,
      [ctx.userId, attentionCount, JSON.stringify(payload), domainEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, active_moment_count, widget_payload, source_event_id, projection_version
       ) VALUES ($1,$2,1,$3::jsonb,$4,1)`,
      [ctx.userId, attentionCount, JSON.stringify(payload), domainEventId]
    );
  }

  return { attentionCaptureId, momentId };
}

export async function recordLifeOpsAdjust(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof lifeOpsAdjustSchema>
): Promise<{ adjustmentId: string; momentId: string }> {
  await assertPersonalMoment(client, ctx, momentId);

  const inserted = await client.query<{ life_operation_adjustment_id: string }>(
    `INSERT INTO personal.life_operation_adjustment (
       moment_id, user_id, rhythm_action_code, signal_direction_code, reason, occurred_at
     ) VALUES ($1,$2,$3,$4,$5,COALESCE($6::timestamptz, now()))
     RETURNING life_operation_adjustment_id`,
    [
      momentId,
      ctx.userId,
      body.rhythmActionCode ?? null,
      body.signalDirectionCode ?? null,
      body.reason ?? null,
      body.occurredAt ?? null,
    ]
  );
  const adjustmentId = inserted.rows[0]!.life_operation_adjustment_id;

  if (body.priorityWeights?.length) {
    for (const p of body.priorityWeights) {
      await client.query(
        `UPDATE personal.life_operation_priority
         SET effective_to = now(), status = 'INACTIVE', updated_at = now()
         WHERE moment_id = $1 AND priority_code = $2 AND status = 'ACTIVE' AND effective_to IS NULL`,
        [momentId, p.priorityCode]
      );
      await client.query(
        `INSERT INTO personal.life_operation_priority (
           moment_id, user_id, priority_code, weight_pct, selected, status
         ) VALUES ($1,$2,$3,$4,TRUE,'ACTIVE')`,
        [momentId, ctx.userId, p.priorityCode, p.weightPct]
      );
    }
  }

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'LifeOpsAdjusted',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_OPS_ADJUSTMENT',
    aggregateId: adjustmentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { adjustmentId, momentId, rhythmActionCode: body.rhythmActionCode ?? null },
  });

  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1,$2,'PERSONAL','MOMENT',$3,'LIFE_OPS_ADJUSTED',$4,now(),$5::jsonb,1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      body.rhythmActionCode ? `Adjust · ${body.rhythmActionCode}` : 'Rhythm adjusted',
      JSON.stringify({
        adjustmentId,
        rhythmActionCode: body.rhythmActionCode ?? null,
        signalDirectionCode: body.signalDirectionCode ?? null,
      }),
    ]
  );

  // Soft wellbeing bump for adjust.
  const existing = await client.query<{
    wellbeing_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT wellbeing_score, widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [ctx.userId]
  );
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  payload.lastAdjustAt = new Date().toISOString();
  if (body.rhythmActionCode) payload.lastRhythmAction = body.rhythmActionCode;
  if (body.signalDirectionCode) payload.lastSignalDirection = body.signalDirectionCode;
  const wellbeing =
    body.signalDirectionCode === 'DECREASE_PRESSURE'
      ? clampScore((existing.rows[0]?.wellbeing_score != null ? Number(existing.rows[0].wellbeing_score) : 60) + 5)
      : body.signalDirectionCode === 'INCREASE_PRESSURE'
        ? clampScore((existing.rows[0]?.wellbeing_score != null ? Number(existing.rows[0].wellbeing_score) : 60) - 5)
        : existing.rows[0]?.wellbeing_score != null
          ? Number(existing.rows[0].wellbeing_score)
          : 65;

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse
       SET wellbeing_score = $2,
           widget_payload = $3::jsonb,
           source_event_id = $4,
           projection_version = projection_version + 1,
           updated_at = now()
       WHERE user_id = $1`,
      [ctx.userId, wellbeing, JSON.stringify(payload), domainEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, active_moment_count, wellbeing_score, widget_payload, source_event_id, projection_version
       ) VALUES ($1,0,1,$2,$3::jsonb,$4,1)`,
      [ctx.userId, wellbeing, JSON.stringify(payload), domainEventId]
    );
  }

  return { adjustmentId, momentId };
}

export async function attachObservationDetails(
  client: PoolClient,
  observationId: string,
  observationType: string,
  detail: z.infer<typeof observationDetailSchema>
): Promise<void> {
  if (observationType === 'RECOVERY' && detail.activityTypeCode) {
    await client.query(
      `INSERT INTO personal.recovery_observation_detail (
         life_operation_observation_id, observation_type, activity_type_code,
         duration_minutes, energy_before_pct, energy_after_pct
       ) VALUES ($1,'RECOVERY',$2,$3,$4,$5)
       ON CONFLICT (life_operation_observation_id) DO UPDATE SET
         activity_type_code = EXCLUDED.activity_type_code,
         duration_minutes = EXCLUDED.duration_minutes,
         energy_before_pct = EXCLUDED.energy_before_pct,
         energy_after_pct = EXCLUDED.energy_after_pct,
         updated_at = now()`,
      [
        observationId,
        detail.activityTypeCode,
        detail.durationMinutes ?? null,
        detail.energyBeforePct ?? null,
        detail.energyAfterPct ?? null,
      ]
    );
  }

  if (observationType === 'MOOD' && detail.feelingStateCode) {
    await client.query(
      `INSERT INTO personal.mood_observation_detail (
         life_operation_observation_id, observation_type, feeling_state_code
       ) VALUES ($1,'MOOD',$2)
       ON CONFLICT (life_operation_observation_id) DO UPDATE SET
         feeling_state_code = EXCLUDED.feeling_state_code,
         updated_at = now()`,
      [observationId, detail.feelingStateCode]
    );
    if (detail.moodDrivers?.length) {
      for (const driver of detail.moodDrivers) {
        await client.query(
          `INSERT INTO personal.mood_observation_driver (life_operation_observation_id, driver_code)
           VALUES ($1,$2)
           ON CONFLICT (life_operation_observation_id, driver_code) DO NOTHING`,
          [observationId, driver]
        );
      }
    }
  }
}

// ─── RP-01..05 reads ─────────────────────────────────────────────────────────

export async function getRuntimeSummary(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const row = await client.query<{
    entries_today_count: string;
    last_entry_at: Date | null;
  }>(
    `SELECT
       COUNT(*) FILTER (WHERE occurred_at::date = (now() AT TIME ZONE 'UTC')::date)::text AS entries_today_count,
       MAX(occurred_at) AS last_entry_at
     FROM projection.recent_activity
     WHERE user_id = $1 AND scope_id = $2::uuid AND domain_code = 'PERSONAL'`,
    [ctx.userId, momentId]
  );
  return {
    momentId,
    domainCode: 'PERSONAL',
    entriesTodayCount: parseInt(row.rows[0]?.entries_today_count ?? '0', 10),
    lastEntryAt: row.rows[0]?.last_entry_at?.toISOString() ?? null,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getMoodHistory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  days = 7
) {
  await assertPersonalMoment(client, ctx, momentId);
  const safeDays = Math.min(Math.max(days, 1), 31);
  const rows = await client.query<{
    day: Date;
    mood_score: string | null;
    feeling_state_code: string | null;
    daily_count: string;
  }>(
    `WITH days AS (
       SELECT generate_series(
         (now() AT TIME ZONE 'UTC')::date - ($3::int - 1),
         (now() AT TIME ZONE 'UTC')::date,
         '1 day'::interval
       )::date AS day
     ),
     mood AS (
       SELECT
         o.observed_at::date AS day,
         AVG(o.numeric_value) AS mood_score,
         (ARRAY_AGG(md.feeling_state_code ORDER BY o.observed_at DESC))[1] AS feeling_state_code,
         COUNT(*)::text AS daily_count
       FROM personal.life_operation_observation o
       LEFT JOIN personal.mood_observation_detail md
         ON md.life_operation_observation_id = o.life_operation_observation_id
       WHERE o.moment_id = $1 AND o.user_id = $2 AND o.observation_type = 'MOOD'
         AND o.status = 'ACTIVE'
         AND o.observed_at::date >= (now() AT TIME ZONE 'UTC')::date - ($3::int - 1)
       GROUP BY o.observed_at::date
     )
     SELECT d.day, m.mood_score::text, m.feeling_state_code, COALESCE(m.daily_count,'0') AS daily_count
     FROM days d
     LEFT JOIN mood m ON m.day = d.day
     ORDER BY d.day`,
    [momentId, ctx.userId, safeDays]
  );

  const scores = rows.rows
    .map((r) => (r.mood_score != null ? Number(r.mood_score) : null))
    .filter((n): n is number => n != null);
  const periodAverage = scores.length ? scores.reduce((a, b) => a + b, 0) / scores.length : null;
  const half = Math.floor(scores.length / 2);
  let trendDirection: 'UP' | 'DOWN' | 'FLAT' | 'INSUFFICIENT' = 'INSUFFICIENT';
  if (scores.length >= 4) {
    const early = scores.slice(0, half).reduce((a, b) => a + b, 0) / half;
    const late = scores.slice(half).reduce((a, b) => a + b, 0) / (scores.length - half);
    const delta = late - early;
    trendDirection = Math.abs(delta) < 0.5 ? 'FLAT' : delta > 0 ? 'UP' : 'DOWN';
  }

  return {
    momentId,
    days: safeDays,
    items: rows.rows.map((r) => ({
      date: r.day.toISOString().slice(0, 10),
      moodScore: r.mood_score,
      feelingStateCode: r.feeling_state_code,
      dailyCount: parseInt(r.daily_count, 10),
    })),
    periodAverage: periodAverage != null ? Number(periodAverage.toFixed(2)) : null,
    trendDirection,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getAdjustmentInsight(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const pulse = await client.query<{
    recovery_score: string | null;
    rhythm_score: string | null;
    attention_count: number;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT recovery_score, rhythm_score, attention_count, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1`,
    [ctx.userId]
  );
  const r = pulse.rows[0];
  const recovery = r?.recovery_score != null ? Number(r.recovery_score) : null;
  const rhythm = r?.rhythm_score != null ? Number(r.rhythm_score) : null;
  const attention = r?.attention_count ?? 0;
  const pressure = recovery != null ? clampScore(100 - recovery) : null;

  // Catalogue-driven thresholds (documented): recovery < 40 → REDUCE_LOAD; pressure > 70 → DECREASE_PRESSURE.
  let suggestedRhythmAction: string | null = null;
  let suggestedSignalDirection: string | null = null;
  const reasonCodes: string[] = [];
  if (recovery != null && recovery < 40) {
    suggestedRhythmAction = 'REDUCE_LOAD';
    reasonCodes.push('LOW_RECOVERY');
  } else if (recovery != null && recovery > 75 && rhythm != null && rhythm < 50) {
    suggestedRhythmAction = 'INCREASE_INTENSITY';
    reasonCodes.push('HIGH_RECOVERY_LOW_RHYTHM');
  }
  if (pressure != null && pressure > 70) {
    suggestedSignalDirection = 'DECREASE_PRESSURE';
    reasonCodes.push('HIGH_PRESSURE');
  } else if (pressure != null && pressure < 30) {
    suggestedSignalDirection = 'MAINTAIN';
    reasonCodes.push('STABLE_PRESSURE');
  }
  if (attention >= 5) reasonCodes.push('HIGH_ATTENTION_LOAD');

  const confidence =
    recovery == null && rhythm == null ? 0.2 : reasonCodes.length >= 2 ? 0.75 : reasonCodes.length === 1 ? 0.55 : 0.35;

  return {
    momentId,
    suggestedRhythmActionCode: suggestedRhythmAction,
    suggestedSignalDirectionCode: suggestedSignalDirection,
    reasonCodes,
    recoverySignal: recovery,
    pressureSignal: pressure,
    attentionSignal: attention,
    confidence,
    generatedAt: new Date().toISOString(),
    projectionVersion: 1,
  };
}

export async function getActivitySummary(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  period: 'WEEK' | 'MONTH' = 'MONTH'
) {
  await assertPersonalMoment(client, ctx, momentId);
  const days = period === 'WEEK' ? 7 : 30;
  const act = await client.query<{ total_logs: string; period_logs: string }>(
    `SELECT
       COUNT(*)::text AS total_logs,
       COUNT(*) FILTER (WHERE occurred_at >= now() - ($3 || ' days')::interval)::text AS period_logs
     FROM projection.recent_activity
     WHERE user_id = $1 AND scope_id = $2::uuid AND domain_code = 'PERSONAL'`,
    [ctx.userId, momentId, String(days)]
  );
  const fin = await client.query<{ expense_total: string | null; income_total: string | null }>(
    `SELECT
       (SELECT COALESCE(SUM(amount),0)::text FROM finance.expense
         WHERE moment_id = $1 AND created_by_user_id = $2 AND status IN ('POSTED','DRAFT')
           AND effective_at >= now() - ($3 || ' days')::interval) AS expense_total,
       (SELECT COALESCE(SUM(fm.amount),0)::text
         FROM finance.financial_movement fm
         WHERE fm.source_type = 'PERSONAL_INCOME'
           AND fm.source_id = $1::uuid
           AND fm.status = 'POSTED'
           AND fm.effective_at >= now() - ($3 || ' days')::interval
       ) AS income_total`,
    [momentId, ctx.userId, String(days)]
  );

  // Keep personal_finance_snapshot warm for RP-04/05.
  await refreshPersonalFinanceSnapshot(client, ctx.userId, momentId);

  const periodEnd = new Date();
  const periodStart = new Date(periodEnd.getTime() - days * 86400000);
  return {
    momentId,
    totalLogs: parseInt(act.rows[0]?.total_logs ?? '0', 10),
    periodLogs: parseInt(act.rows[0]?.period_logs ?? '0', 10),
    expenseTotal: fin.rows[0]?.expense_total ?? '0',
    incomeTotal: fin.rows[0]?.income_total ?? '0',
    periodStart: periodStart.toISOString(),
    periodEnd: periodEnd.toISOString(),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getMoneyJourney(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  periodMonths = 6
) {
  await assertPersonalMoment(client, ctx, momentId);
  const months = Math.min(Math.max(periodMonths, 1), 24);
  const rows = await client.query<{
    bucket_start: Date;
    expense_total: string;
    income_total: string;
    savings_total: string;
    transfer_total: string;
    currency_code: string;
  }>(
    `WITH months AS (
       SELECT date_trunc('month', d)::date AS bucket_start
       FROM generate_series(
         date_trunc('month', now()) - (($2::int - 1) || ' months')::interval,
         date_trunc('month', now()),
         '1 month'::interval
       ) AS d
     ),
     expenses AS (
       SELECT date_trunc('month', effective_at)::date AS bucket_start,
              currency_code,
              SUM(amount)::text AS expense_total
       FROM finance.expense
       WHERE moment_id = $1 AND created_by_user_id = $3 AND status IN ('POSTED','DRAFT')
         AND effective_at >= date_trunc('month', now()) - (($2::int - 1) || ' months')::interval
       GROUP BY 1, 2
     ),
     movements AS (
       SELECT date_trunc('month', effective_at)::date AS bucket_start,
              currency_code,
              SUM(amount) FILTER (WHERE source_type = 'PERSONAL_INCOME')::text AS income_total,
              SUM(amount) FILTER (WHERE movement_type = 'TRANSFER' AND source_type = 'SAVINGS_DEPOSIT')::text AS savings_total,
              SUM(amount) FILTER (WHERE movement_type = 'TRANSFER' AND COALESCE(source_type,'') <> 'SAVINGS_DEPOSIT')::text AS transfer_total
       FROM finance.financial_movement
       WHERE (
           (source_type = 'PERSONAL_INCOME' AND source_id = $1::uuid)
           OR financial_movement_id IN (
             SELECT l.financial_movement_id FROM finance.financial_movement_link l
             WHERE l.resource_type = 'MOMENT' AND l.resource_id = $1::uuid
           )
         )
         AND effective_at >= date_trunc('month', now()) - (($2::int - 1) || ' months')::interval
       GROUP BY 1, 2
     )
     SELECT m.bucket_start,
            COALESCE(e.expense_total,'0') AS expense_total,
            COALESCE(mv.income_total,'0') AS income_total,
            COALESCE(mv.savings_total,'0') AS savings_total,
            COALESCE(mv.transfer_total,'0') AS transfer_total,
            COALESCE(e.currency_code, mv.currency_code, 'INR') AS currency_code
     FROM months m
     LEFT JOIN expenses e ON e.bucket_start = m.bucket_start
     LEFT JOIN movements mv ON mv.bucket_start = m.bucket_start
     ORDER BY m.bucket_start`,
    [momentId, months, ctx.userId]
  );

  await refreshPersonalFinanceSnapshot(client, ctx.userId, momentId);

  return {
    momentId,
    periodMonths: months,
    buckets: rows.rows.map((r) => {
      const expense = Number(r.expense_total);
      const income = Number(r.income_total);
      const savings = Number(r.savings_total);
      const transfer = Number(r.transfer_total);
      return {
        bucketStart: r.bucket_start.toISOString().slice(0, 10),
        expenseTotal: r.expense_total,
        incomeTotal: r.income_total,
        savingsTotal: r.savings_total,
        transferTotal: r.transfer_total,
        netFlow: (income - expense - savings).toFixed(4),
        currencyCode: r.currency_code,
      };
    }),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function refreshPersonalFinanceSnapshot(
  client: PoolClient,
  userId: string,
  momentId: string
): Promise<void> {
  const totals = await client.query<{
    currency_code: string;
    expense_total: string;
    income_total: string;
  }>(
    `SELECT
       COALESCE(
         (SELECT currency_code FROM finance.expense WHERE moment_id = $1 AND created_by_user_id = $2 ORDER BY effective_at DESC LIMIT 1),
         'INR'
       ) AS currency_code,
       (SELECT COALESCE(SUM(amount),0)::text FROM finance.expense
         WHERE moment_id = $1 AND created_by_user_id = $2 AND status IN ('POSTED','DRAFT')) AS expense_total,
       (SELECT COALESCE(SUM(amount),0)::text FROM finance.financial_movement
         WHERE source_type = 'PERSONAL_INCOME' AND source_id = $1::uuid AND status = 'POSTED') AS income_total`,
    [momentId, userId]
  );
  const t = totals.rows[0];
  if (!t) return;

  await client.query(
    `INSERT INTO projection.personal_finance_snapshot (
       user_id, currency_code, expense_total, budget_total, contribution_total,
       payable_total, receivable_total, snapshot_payload, projection_version, updated_at
     ) VALUES ($1,$2,$3::numeric,0,0,0,0,$4::jsonb,1,now())
     ON CONFLICT (user_id, currency_code) DO UPDATE SET
       expense_total = EXCLUDED.expense_total,
       snapshot_payload = EXCLUDED.snapshot_payload,
       projection_version = projection.personal_finance_snapshot.projection_version + 1,
       updated_at = now()`,
    [
      userId,
      t.currency_code,
      t.expense_total,
      JSON.stringify({
        momentId,
        incomeTotal: t.income_total,
        expenseTotal: t.expense_total,
      }),
    ]
  );
}

export async function listExpenseCategories(client: PoolClient) {
  const cats = await client.query<{
    category_code: string;
    label: string;
    sort_order: number;
  }>(
    `SELECT category_code, label, sort_order FROM finance.expense_category
     WHERE status = 'ACTIVE' ORDER BY sort_order, category_code`
  );
  const subs = await client.query<{
    subcategory_code: string;
    category_code: string;
    label: string;
    sort_order: number;
  }>(
    `SELECT subcategory_code, category_code, label, sort_order FROM finance.expense_subcategory
     WHERE status = 'ACTIVE' ORDER BY sort_order, subcategory_code`
  );
  return {
    categories: cats.rows.map((c) => ({
      categoryCode: c.category_code,
      label: c.label,
      sortOrder: c.sort_order,
      subcategories: subs.rows
        .filter((s) => s.category_code === c.category_code)
        .map((s) => ({
          subcategoryCode: s.subcategory_code,
          label: s.label,
          sortOrder: s.sort_order,
        })),
    })),
  };
}

export async function replaceExpenseTags(
  client: PoolClient,
  expenseId: string,
  tags: string[]
): Promise<void> {
  await client.query(`DELETE FROM finance.expense_tag WHERE expense_id = $1`, [expenseId]);
  for (const raw of tags) {
    const tag = raw.trim();
    if (!tag) continue;
    await client.query(
      `INSERT INTO finance.expense_tag (expense_id, tag_name) VALUES ($1,$2)
       ON CONFLICT DO NOTHING`,
      [expenseId, tag.slice(0, 80)]
    );
  }
}
