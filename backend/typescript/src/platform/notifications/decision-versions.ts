/** Soak-phase decision engine versions — bump policy_version on every threshold/cadence calibration. */
export const NOTIFICATION_DECISION_VERSION = 'v2.1';
/** Bumped when balance clear/re-emit spam fix shipped (multi-currency false clear). */
export const NOTIFICATION_POLICY_VERSION = '2026-09-soak-02';

export type DecisionOutcome = 'IMMEDIATE' | 'DEFER_TO_DIGEST' | 'SUPPRESS';

export type SuppressionReason =
  | 'actor'
  | 'uninvolved'
  | 'muted'
  | 'category_disabled'
  | 'quiet_hours'
  | 'rate_policy'
  | 'digest_cadence'
  | 'duplicate'
  | 'already_seen'
  | 'stale'
  | 'invalid_recipient';

export type DeliveryRoute = 'immediate' | 'digest' | 'suppress';

export function outcomeToRoute(outcome: DecisionOutcome): DeliveryRoute {
  switch (outcome) {
    case 'IMMEDIATE':
      return 'immediate';
    case 'DEFER_TO_DIGEST':
      return 'digest';
    case 'SUPPRESS':
      return 'suppress';
  }
}

export function routeToOutcome(route: DeliveryRoute): DecisionOutcome {
  switch (route) {
    case 'immediate':
      return 'IMMEDIATE';
    case 'digest':
      return 'DEFER_TO_DIGEST';
    case 'suppress':
      return 'SUPPRESS';
  }
}
