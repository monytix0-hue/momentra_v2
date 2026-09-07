/**
 * Overall Personal Pulse wellbeing — equal-weight blend of family contributions.
 * Family refreshes must not last-writer-wins overwrite wellbeing_score.
 */
import type { PoolClient } from 'pg';

export function clampScore(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value)));
}

export function averageNullable(parts: Array<number | null | undefined>): number | null {
  const nums = parts.filter((n): n is number => n != null && Number.isFinite(n));
  if (!nums.length) return null;
  return clampScore(nums.reduce((a, b) => a + b, 0) / nums.length);
}

function num(v: unknown): number | null {
  if (v == null) return null;
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : null;
}

export type PulseWellbeingState = {
  recoveryScore: number | null;
  rhythmScore: number | null;
  widgetPayload: Record<string, unknown>;
};

/**
 * Family representatives:
 * - LO: avg(recovery, rhythm, lifeOpsWellbeingScore) then + lifeOpsAdjustBias
 * - Future: avg(vision/growth/momentum/discipline)
 * - Lifestyle: vitalityScore or avg(joy/fulfillment/exploration)
 * - Relationships: bondIndex or avg(trust/care/support/presence)
 *
 * Overall = equal-weight average of families with at least one input.
 */
export function computeOverallWellbeing(state: PulseWellbeingState): number | null {
  const p = state.widgetPayload ?? {};

  const loBase = averageNullable([
    state.recoveryScore,
    state.rhythmScore,
    num(p.lifeOpsWellbeingScore),
  ]);
  const adjustBias = num(p.lifeOpsAdjustBias) ?? 0;
  let loScore: number | null = loBase;
  if (loScore != null && adjustBias !== 0) {
    loScore = clampScore(loScore + adjustBias);
  } else if (loScore == null && adjustBias !== 0) {
    loScore = clampScore(60 + adjustBias);
  }

  const futureScore = averageNullable([
    num(p.visionScore),
    num(p.growthScore),
    num(p.momentumScore),
    num(p.disciplineScore),
  ]);

  const vitality = num(p.vitalityScore);
  const lifestyleScore =
    vitality != null
      ? vitality
      : averageNullable([num(p.joyScore), num(p.fulfillmentScore), num(p.explorationScore)]);

  const bond = num(p.bondIndex);
  const relationshipsScore =
    bond != null
      ? bond
      : averageNullable([
          num(p.trustScore),
          num(p.careScore),
          num(p.supportScore),
          num(p.presenceScore),
        ]);

  return averageNullable([loScore, futureScore, lifestyleScore, relationshipsScore]);
}

/** Recompute and persist overall wellbeing_score from current pulse row state. */
export async function recomputeOverallWellbeing(
  client: PoolClient,
  userId: string,
  sourceEventId?: string | null
): Promise<number | null> {
  const row = await client.query<{
    recovery_score: string | null;
    rhythm_score: string | null;
    widget_payload: Record<string, unknown> | null;
  }>(
    `SELECT recovery_score, rhythm_score, widget_payload
     FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );
  if (!row.rows[0]) return null;

  const r = row.rows[0];
  const wellbeing = computeOverallWellbeing({
    recoveryScore: r.recovery_score != null ? Number(r.recovery_score) : null,
    rhythmScore: r.rhythm_score != null ? Number(r.rhythm_score) : null,
    widgetPayload: r.widget_payload ?? {},
  });

  if (sourceEventId) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         wellbeing_score = $2,
         source_event_id = $3,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, wellbeing, sourceEventId]
    );
  } else {
    await client.query(
      `UPDATE projection.personal_pulse SET
         wellbeing_score = $2,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, wellbeing]
    );
  }
  return wellbeing;
}
