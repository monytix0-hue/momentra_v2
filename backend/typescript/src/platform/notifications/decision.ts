import type { Pool } from 'pg';
import {
  notificationCategory,
  notificationCopy,
  notificationPriority,
  type NotificationCategory,
  type NotificationPriority,
} from './allowlist';
import { applyRatePolicy, type DeliveryRoute } from './rate-policy';
import { categoryEnabled, inQuietHours, type RecipientPrefs } from './recipient-prefs';
import { threadKeyFor } from './thread-key';
import {
  buildRecipientFinanceCtx,
  cadenceFromNotifyFlag,
  inferRelationship,
  type NotificationCadence,
  type RecipientContext,
  type RecipientRelationship,
} from './decision-types';
import {
  outcomeToRoute,
  type DecisionOutcome,
  type SuppressionReason,
} from './decision-versions';

export type { NotificationCadence, RecipientContext, RecipientRelationship, DeliveryRoute };
export {
  cadenceFromNotifyFlag,
  mapCadenceToNotifyOnChanges,
  moneyLabel,
  inferRelationship,
} from './decision-types';
export {
  NOTIFICATION_DECISION_VERSION,
  NOTIFICATION_POLICY_VERSION,
  type DecisionOutcome,
  type SuppressionReason,
} from './decision-versions';

export type NotificationDecision = {
  /** Internal semantic: IMMEDIATE | DEFER_TO_DIGEST | SUPPRESS */
  outcome: DecisionOutcome;
  /** Surface route for delivery code */
  route: DeliveryRoute;
  suppressionReason: SuppressionReason | null;
  category: NotificationCategory;
  priority: NotificationPriority;
  title: string;
  body: string;
  threadKey: string;
  replacePrior: boolean;
  relationship: RecipientRelationship;
};

function suppress(
  reason: SuppressionReason,
  partial: {
    category: NotificationCategory;
    priority: NotificationPriority;
    relationship: RecipientRelationship;
    threadKey: string;
    title?: string;
    body?: string;
  }
): NotificationDecision {
  return {
    outcome: 'SUPPRESS',
    route: 'suppress',
    suppressionReason: reason,
    category: partial.category,
    priority: partial.priority,
    title: partial.title ?? '',
    body: partial.body ?? '',
    threadKey: partial.threadKey,
    replacePrior: false,
    relationship: partial.relationship,
  };
}

/**
 * Per-recipient decision: always returns an auditable result (never bare null).
 * Prefs + cadence + relationship + quiet hours + rate policy → IMMEDIATE | DEFER_TO_DIGEST | SUPPRESS.
 */
export async function decideNotificationForRecipient(
  pool: Pool,
  input: {
    eventName: string;
    actorUserId: string;
    payload: Record<string, unknown>;
    prefs: RecipientPrefs;
    momentId: string | null;
    companyId?: string | null;
    domainCode?: string | null;
  }
): Promise<NotificationDecision> {
  const category = notificationCategory(input.eventName);
  const priority = notificationPriority(input.eventName, input.payload);
  const cadence = cadenceFromNotifyFlag(
    input.prefs.notify_on_changes !== false,
    input.prefs.notification_cadence
  );

  const relationship = inferRelationship(
    input.eventName,
    input.prefs.user_id,
    input.actorUserId,
    input.payload
  );

  const threadKey = threadKeyFor({
    domainCode:
      input.domainCode ??
      (typeof input.payload.domainCode === 'string' ? input.payload.domainCode : null),
    momentId: input.momentId,
    companyId:
      input.companyId ??
      (typeof input.payload.companyId === 'string' ? input.payload.companyId : null),
    eventName: input.eventName,
  });

  const base = { category, priority, relationship, threadKey };

  if (relationship === 'actor') {
    return suppress('actor', base);
  }
  if (relationship === 'uninvolved') {
    return suppress('uninvolved', base);
  }
  if (cadence === 'MUTED') {
    return suppress('muted', base);
  }
  if (!categoryEnabled(input.prefs.notification_categories, category)) {
    return suppress('category_disabled', base);
  }

  const finance = buildRecipientFinanceCtx(input.prefs.user_id, relationship, input.payload);
  const ctx: RecipientContext = {
    userId: input.prefs.user_id,
    relationship,
    cadence,
    ...finance,
  };
  const copy = notificationCopy(input.eventName, input.payload, ctx);

  let outcome: DecisionOutcome = 'IMMEDIATE';
  let suppressionReason: SuppressionReason | null = null;

  if (cadence === 'IMPORTANT' && priority !== 'HIGH') {
    outcome = 'DEFER_TO_DIGEST';
    suppressionReason = 'digest_cadence';
  } else if (cadence === 'DIGEST_ONLY' && priority !== 'HIGH') {
    outcome = 'DEFER_TO_DIGEST';
    suppressionReason = 'digest_cadence';
  } else if (priority !== 'HIGH' && input.prefs.digest_enabled) {
    outcome = 'DEFER_TO_DIGEST';
    suppressionReason = 'digest_cadence';
  } else if (
    priority !== 'HIGH' &&
    inQuietHours(
      new Date(),
      input.prefs.timezone,
      input.prefs.quiet_hours_start,
      input.prefs.quiet_hours_end
    )
  ) {
    outcome = 'DEFER_TO_DIGEST';
    suppressionReason = 'quiet_hours';
  } else {
    const rateRoute = await applyRatePolicy(pool, {
      userId: input.prefs.user_id,
      momentId: input.momentId,
      priority,
      forceImmediate: priority === 'HIGH',
    });
    if (rateRoute === 'digest') {
      outcome = 'DEFER_TO_DIGEST';
      suppressionReason = 'rate_policy';
    } else if (rateRoute === 'suppress') {
      outcome = 'SUPPRESS';
      suppressionReason = 'rate_policy';
    } else {
      outcome = 'IMMEDIATE';
      suppressionReason = null;
    }
  }

  return {
    outcome,
    route: outcomeToRoute(outcome),
    suppressionReason,
    category,
    priority,
    title: copy.title,
    body: copy.body,
    threadKey,
    replacePrior: input.eventName === 'DigestReady' || input.eventName === 'MomentDigestReady',
    relationship,
  };
}
