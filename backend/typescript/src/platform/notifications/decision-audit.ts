import type { Pool, PoolClient } from 'pg';
import {
  NOTIFICATION_DECISION_VERSION,
  NOTIFICATION_POLICY_VERSION,
  type DecisionOutcome,
  type DeliveryRoute,
  type SuppressionReason,
} from './decision-versions';

export type DecisionAuditInsert = {
  userId: string;
  domainEventId: string | null;
  userNotificationId?: string | null;
  eventName: string;
  momentId?: string | null;
  dedupeKey?: string | null;
  explanationCode?: string | null;
  signalName?: string | null;
  contextType?: string | null;
  outcome: DecisionOutcome;
  route: DeliveryRoute;
  suppressionReason?: SuppressionReason | null;
  importance: string;
  actionabilityScore?: number | null;
  categoryCode: string;
  relationship?: string | null;
  threadKey?: string | null;
  decisionVersion?: string;
  policyVersion?: string;
  eventOccurredAt?: Date | null;
  decidedAt?: Date;
};

/** Append-only insert. Never update this row with read/action later. */
export async function insertDecisionAudit(
  client: Pool | PoolClient,
  row: DecisionAuditInsert
): Promise<string> {
  const r = await client.query<{ decision_audit_id: string }>(
    `INSERT INTO platform.notification_decision_audit (
       user_id, domain_event_id, user_notification_id, event_name, moment_id,
       dedupe_key, explanation_code, signal_name, context_type,
       outcome, route, suppression_reason, importance, actionability_score,
       category_code, relationship, thread_key,
       decision_version, policy_version, event_occurred_at, decided_at
     ) VALUES (
       $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21
     )
     RETURNING decision_audit_id`,
    [
      row.userId,
      row.domainEventId,
      row.userNotificationId ?? null,
      row.eventName,
      row.momentId ?? null,
      row.dedupeKey ?? null,
      row.explanationCode ?? null,
      row.signalName ?? null,
      row.contextType ?? null,
      row.outcome,
      row.route,
      row.suppressionReason ?? null,
      row.importance,
      row.actionabilityScore ?? null,
      row.categoryCode,
      row.relationship ?? null,
      row.threadKey ?? null,
      row.decisionVersion ?? NOTIFICATION_DECISION_VERSION,
      row.policyVersion ?? NOTIFICATION_POLICY_VERSION,
      row.eventOccurredAt ?? null,
      row.decidedAt ?? new Date(),
    ]
  );
  return r.rows[0]!.decision_audit_id;
}

export function signalFieldsFromPayload(
  eventName: string,
  payload: Record<string, unknown>
): {
  dedupeKey: string | null;
  explanationCode: string | null;
  signalName: string | null;
  contextType: string | null;
  actionabilityScore: number | null;
} {
  const dedupeKey = typeof payload.dedupeKey === 'string' ? payload.dedupeKey : null;
  const explanationCode =
    typeof payload.explanationCode === 'string' ? payload.explanationCode : null;
  const signalName =
    payload.derivedSignal === true
      ? eventName
      : typeof payload.signalName === 'string'
        ? payload.signalName
        : null;
  const contextType =
    typeof payload.contextType === 'string' ? payload.contextType : null;
  const scoreRaw = payload.actionabilityScore;
  const actionabilityScore =
    typeof scoreRaw === 'number' && Number.isFinite(scoreRaw)
      ? scoreRaw
      : typeof scoreRaw === 'string' && Number.isFinite(Number(scoreRaw))
        ? Number(scoreRaw)
        : null;
  return { dedupeKey, explanationCode, signalName, contextType, actionabilityScore };
}
