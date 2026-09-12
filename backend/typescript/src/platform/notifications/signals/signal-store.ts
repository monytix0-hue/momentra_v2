import type { Pool, PoolClient } from 'pg';
import type { DerivedNotificationSignal } from './types';

/**
 * Claim a derived signal for emit (dedupe). Returns false if already open/active.
 * For threshold hysteresis: pass resetWhen to clear prior claim when state resets below band.
 */
export async function claimDerivedSignal(
  client: Pool | PoolClient,
  signal: DerivedNotificationSignal,
  opts?: { allowRetriggerAfterClear?: boolean }
): Promise<boolean> {
  const r = await client.query(
    `INSERT INTO platform.derived_notification_signal (
       dedupe_key, signal_name, user_id, moment_id, explanation_code, importance,
       facts, actionability_score, hysteresis_state
     ) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, $9::jsonb)
     ON CONFLICT (dedupe_key) DO UPDATE SET
       last_emitted_at = CASE
         WHEN platform.derived_notification_signal.cleared_at IS NOT NULL THEN now()
         ELSE platform.derived_notification_signal.last_emitted_at
       END,
       emit_count = CASE
         WHEN platform.derived_notification_signal.cleared_at IS NOT NULL
           THEN platform.derived_notification_signal.emit_count + 1
         ELSE platform.derived_notification_signal.emit_count
       END,
       facts = CASE
         WHEN platform.derived_notification_signal.cleared_at IS NOT NULL THEN EXCLUDED.facts
         ELSE platform.derived_notification_signal.facts
       END,
       hysteresis_state = CASE
         WHEN platform.derived_notification_signal.cleared_at IS NOT NULL THEN EXCLUDED.hysteresis_state
         ELSE platform.derived_notification_signal.hysteresis_state
       END,
       cleared_at = CASE
         WHEN platform.derived_notification_signal.cleared_at IS NOT NULL THEN NULL
         ELSE platform.derived_notification_signal.cleared_at
       END,
       explanation_code = EXCLUDED.explanation_code,
       importance = EXCLUDED.importance,
       actionability_score = EXCLUDED.actionability_score
     WHERE platform.derived_notification_signal.cleared_at IS NOT NULL
     RETURNING dedupe_key`,
    [
      signal.dedupeKey,
      signal.signalName,
      signal.userId,
      signal.momentId ?? null,
      signal.explanationCode,
      signal.importance,
      JSON.stringify(signal.facts),
      signal.actionabilityScore,
      JSON.stringify(signal.hysteresisState ?? {}),
    ]
  );
  if ((r.rowCount ?? 0) > 0) return true;
  if (opts?.allowRetriggerAfterClear) {
    // no-op; insert path already handles clear → re-emit
  }
  return false;
}

/** Mark a signal cleared so hysteresis can re-fire after reset (e.g. budget drops below 80). */
export async function clearDerivedSignal(
  client: Pool | PoolClient,
  dedupeKey: string
): Promise<void> {
  await client.query(
    `UPDATE platform.derived_notification_signal
     SET cleared_at = now()
     WHERE dedupe_key = $1 AND cleared_at IS NULL`,
    [dedupeKey]
  );
}

export async function isSignalActive(
  client: Pool | PoolClient,
  dedupeKey: string
): Promise<boolean> {
  const r = await client.query(
    `SELECT 1 FROM platform.derived_notification_signal
     WHERE dedupe_key = $1 AND cleared_at IS NULL
     LIMIT 1`,
    [dedupeKey]
  );
  return (r.rowCount ?? 0) > 0;
}

export async function getHysteresisState(
  client: Pool | PoolClient,
  dedupeKey: string
): Promise<Record<string, unknown> | null> {
  const r = await client.query<{ hysteresis_state: Record<string, unknown>; cleared_at: Date | null }>(
    `SELECT hysteresis_state, cleared_at FROM platform.derived_notification_signal WHERE dedupe_key = $1`,
    [dedupeKey]
  );
  if (!r.rows[0] || r.rows[0].cleared_at) return null;
  return r.rows[0].hysteresis_state ?? {};
}
