import type { Pool, PoolClient } from 'pg';
import {
  deepLinkForEvent,
  isPeerPushEvent,
  notificationCategory,
  notificationCopy,
  notificationPriority,
  shouldSkipPushForPayload,
  type NotificationCategory,
  type NotificationPriority,
  PEER_PUSH_EVENT_NAMES,
} from './allowlist';
import {
  asCategoryMap,
  categoryEnabled,
  inQuietHours,
  shouldDigest,
  type RecipientPrefs,
} from './recipient-prefs';

export type { RecipientPrefs };
export { categoryEnabled, inQuietHours, shouldDigest };

export type DomainEventRow = {
  domain_event_id: string;
  actor_user_id: string;
  event_name: string;
  scope_id: string | null;
  payload: Record<string, unknown> | null;
  occurred_at?: Date | string | null;
  recorded_at?: Date | string | null;
};

export async function loadDomainEvent(pool: Pool, domainEventId: string): Promise<DomainEventRow | null> {
  const r = await pool.query<DomainEventRow>(
    `SELECT domain_event_id, actor_user_id, event_name, scope_id, payload,
            occurred_at, recorded_at
     FROM events.domain_event WHERE domain_event_id = $1`,
    [domainEventId]
  );
  return r.rows[0] ?? null;
}

export async function alreadySucceeded(pool: Pool, domainEventId: string): Promise<boolean> {
  const r = await pool.query(
    `SELECT 1 FROM events.event_consumer_state
     WHERE consumer_code = 'NOTIFICATION_WORKER'
       AND domain_event_id = $1
       AND status = 'SUCCEEDED'
     LIMIT 1`,
    [domainEventId]
  );
  return (r.rowCount ?? 0) > 0;
}

export async function markSucceeded(pool: Pool, domainEventId: string): Promise<void> {
  await pool.query(
    `INSERT INTO events.event_consumer_state (
       consumer_code, domain_event_id, status, started_at, completed_at, attempt_count
     ) VALUES ('NOTIFICATION_WORKER', $1, 'SUCCEEDED', now(), now(), 1)
     ON CONFLICT (consumer_code, domain_event_id) DO UPDATE SET
       status = 'SUCCEEDED',
       completed_at = now(),
       attempt_count = events.event_consumer_state.attempt_count + 1,
       updated_at = now()`,
    [domainEventId]
  );
}

export async function resolveActorDisplayName(pool: Pool, actorUserId: string): Promise<string | null> {
  const r = await pool.query<{ display_name: string | null }>(
    `SELECT display_name FROM core.user_profile WHERE user_id = $1`,
    [actorUserId]
  );
  return r.rows[0]?.display_name ?? null;
}

async function loadPrefsForUserIds(pool: Pool, userIds: string[], momentId: string | null): Promise<RecipientPrefs[]> {
  if (userIds.length === 0) return [];
  if (momentId) {
    const r = await pool.query<RecipientPrefs & { notification_cadence: string | null; notify_on_changes: boolean }>(
      `SELECT up.user_id,
              up.push_notifications_enabled,
              up.notification_categories,
              up.quiet_hours_start::text,
              up.quiet_hours_end::text,
              up.digest_enabled,
              coalesce(nullif(up.timezone, ''), 'UTC') AS timezone,
              coalesce(mp.notification_cadence, 'ALL') AS notification_cadence,
              coalesce(mp.notify_on_changes, true) AS notify_on_changes
       FROM core.user_profile up
       LEFT JOIN collaboration.moment_participant mp
         ON mp.moment_id = $2 AND mp.user_id = up.user_id AND mp.status = 'ACTIVE'
       WHERE up.user_id = ANY($1::uuid[])
         AND up.status = 'ACTIVE'
         AND up.push_notifications_enabled = true`,
      [userIds, momentId]
    );
    return r.rows.map((row) => ({
      ...row,
      notification_categories: asCategoryMap(row.notification_categories),
    }));
  }
  const r = await pool.query<RecipientPrefs>(
    `SELECT up.user_id,
            up.push_notifications_enabled,
            up.notification_categories,
            up.quiet_hours_start::text,
            up.quiet_hours_end::text,
            up.digest_enabled,
            coalesce(nullif(up.timezone, ''), 'UTC') AS timezone
     FROM core.user_profile up
     WHERE up.user_id = ANY($1::uuid[])
       AND up.status = 'ACTIVE'
       AND up.push_notifications_enabled = true`,
    [userIds]
  );
  return r.rows.map((row) => ({
    ...row,
    notification_categories: asCategoryMap(row.notification_categories),
    notification_cadence: 'ALL',
    notify_on_changes: true,
  }));
}

/**
 * Smart recipients:
 * - Explicit targetUserIds / assignee / approver lists when present
 * - Event-specific defaults (settlement payer/payee, expense splittees, organizers on invite redeem)
 * - else peers with notify_on_changes (cadence != MUTED)
 * Always excludes actor unless self-reminder targets include them.
 */
export async function resolveRecipients(
  pool: Pool,
  ev: DomainEventRow
): Promise<RecipientPrefs[]> {
  const payload = ev.payload ?? {};
  const momentId =
    (typeof payload.momentId === 'string' ? payload.momentId : null) ?? ev.scope_id;

  const targeted: string[] = [];
  const pushUnique = (id: string | null | undefined) => {
    if (id && !targeted.includes(id)) targeted.push(id);
  };

  if (Array.isArray(payload.targetUserIds)) {
    for (const id of payload.targetUserIds) {
      if (typeof id === 'string') pushUnique(id);
    }
  }
  if (typeof payload.assigneeUserId === 'string') pushUnique(payload.assigneeUserId);
  if (Array.isArray(payload.assigneeUserIds)) {
    for (const id of payload.assigneeUserIds) {
      if (typeof id === 'string') pushUnique(id);
    }
  }
  if (Array.isArray(payload.approverUserIds)) {
    for (const id of payload.approverUserIds) {
      if (typeof id === 'string') pushUnique(id);
    }
  }
  if (typeof payload.payerUserId === 'string') pushUnique(payload.payerUserId);
  if (typeof payload.payeeUserId === 'string') pushUnique(payload.payeeUserId);
  if (typeof payload.paidByUserId === 'string') pushUnique(payload.paidByUserId);
  if (payload.sharesByUserId && typeof payload.sharesByUserId === 'object') {
    for (const id of Object.keys(payload.sharesByUserId as Record<string, unknown>)) {
      pushUnique(id);
    }
  }

  // Active assignments for TaskCreated
  if (ev.event_name === 'TaskCreated' && typeof payload.taskId === 'string') {
    const assigns = await pool.query<{ assignee_user_id: string | null }>(
      `SELECT assignee_user_id FROM work.assignment
       WHERE task_id = $1 AND status = 'ACTIVE' AND assignee_user_id IS NOT NULL`,
      [payload.taskId]
    );
    for (const row of assigns.rows) {
      if (row.assignee_user_id) pushUnique(row.assignee_user_id);
    }
  }

  // ApprovalRequested → organizers when no explicit approvers
  if (ev.event_name === 'ApprovalRequested' && momentId && targeted.length === 0) {
    const admins = await pool.query<{ user_id: string }>(
      `SELECT mp.user_id
       FROM collaboration.moment_participant mp
       WHERE mp.moment_id = $1
         AND mp.status = 'ACTIVE'
         AND mp.user_id IS NOT NULL
         AND mp.user_id <> $2
         AND mp.participant_role IN ('ORGANIZER','CO_ORGANIZER')`,
      [momentId, ev.actor_user_id]
    );
    for (const row of admins.rows) pushUnique(row.user_id);
  }

  // GroupInviteRedeemed → organizers/admins
  if (ev.event_name === 'GroupInviteRedeemed' && momentId && targeted.length === 0) {
    const admins = await pool.query<{ user_id: string }>(
      `SELECT mp.user_id
       FROM collaboration.moment_participant mp
       WHERE mp.moment_id = $1
         AND mp.status = 'ACTIVE'
         AND mp.user_id IS NOT NULL
         AND mp.user_id <> $2
         AND mp.participant_role IN ('ORGANIZER','CO_ORGANIZER','ADMIN')`,
      [momentId, ev.actor_user_id]
    );
    for (const row of admins.rows) pushUnique(row.user_id);
  }

  // BusinessIssueCreated → organizers when no targets
  if (ev.event_name === 'BusinessIssueCreated' && momentId && targeted.length === 0) {
    const admins = await pool.query<{ user_id: string }>(
      `SELECT mp.user_id
       FROM collaboration.moment_participant mp
       WHERE mp.moment_id = $1
         AND mp.status = 'ACTIVE'
         AND mp.user_id IS NOT NULL
         AND mp.user_id <> $2
         AND mp.participant_role IN ('ORGANIZER','CO_ORGANIZER','ADMIN')`,
      [momentId, ev.actor_user_id]
    );
    for (const row of admins.rows) pushUnique(row.user_id);
  }

  const selfReminder =
    payload.derivedSignal === true ||
    ev.event_name === 'WeeklyReminder' ||
    ev.event_name === 'DailyPersonalReminder' ||
    ev.event_name === 'TaskDueReminder' ||
    ev.event_name === 'BillReminder' ||
    ev.event_name === 'ChoreReminder' ||
    ev.event_name === 'ExpenseReminder' ||
    ev.event_name === 'PhotoReminder' ||
    ev.event_name === 'DigestReady' ||
    ev.event_name === 'MomentDigestReady' ||
    ev.event_name === 'TripBudgetThresholdReached' ||
    ev.event_name === 'UserBalanceChangedMeaningfully' ||
    ev.event_name === 'SettlementSuggested' ||
    ev.event_name === 'GroupNearlySettled' ||
    ev.event_name === 'PollNeedsYourVote' ||
    ev.event_name === 'AssignedTaskDueSoon' ||
    ev.event_name === 'ApprovalsAccumulating' ||
    ev.event_name === 'ApprovalAging' ||
    ev.event_name === 'InvoiceDueSoon' ||
    ev.event_name === 'InvoiceOverdue' ||
    ev.event_name === 'ExpenseThresholdExceeded' ||
    ev.event_name === 'RunwayChangedMeaningfully' ||
    ev.event_name === 'BudgetThresholdReached' ||
    ev.event_name === 'BillDueSoon' ||
    ev.event_name === 'GoalMilestoneReached' ||
    ev.event_name === 'GoalAtRisk' ||
    ev.event_name === 'RecurringExpenseExpected';

  let uniqueTargets = [...new Set(targeted)].filter(Boolean);
  if (!selfReminder) {
    uniqueTargets = uniqueTargets.filter((id) => id !== ev.actor_user_id);
  }

  if (uniqueTargets.length > 0) {
    return loadPrefsForUserIds(pool, uniqueTargets, momentId);
  }

  if (!momentId) return [];

  // Default peer fan-out (cadence-aware mute via notify_on_changes)
  const peers = await pool.query<RecipientPrefs & { notification_cadence: string; notify_on_changes: boolean }>(
    `SELECT up.user_id,
            up.push_notifications_enabled,
            up.notification_categories,
            up.quiet_hours_start::text,
            up.quiet_hours_end::text,
            up.digest_enabled,
            coalesce(nullif(up.timezone, ''), 'UTC') AS timezone,
            coalesce(mp.notification_cadence, 'ALL') AS notification_cadence,
            mp.notify_on_changes
     FROM collaboration.moment_participant mp
     JOIN core.user_profile up ON up.user_id = mp.user_id
     WHERE mp.moment_id = $1
       AND mp.status = 'ACTIVE'
       AND mp.user_id IS NOT NULL
       AND mp.user_id <> $2
       AND mp.notify_on_changes = true
       AND up.push_notifications_enabled = true
       AND up.status = 'ACTIVE'`,
    [momentId, ev.actor_user_id]
  );
  return peers.rows.map((row) => ({
    ...row,
    notification_categories: asCategoryMap(row.notification_categories),
  }));
}

export async function claimRecipient(
  pool: Pool,
  domainEventId: string,
  userId: string,
  eventName: string,
  category: NotificationCategory,
  priority: NotificationPriority
): Promise<boolean> {
  const r = await pool.query(
    `INSERT INTO platform.notification_dispatch (
       domain_event_id, user_id, event_name, sent_count, category_code, priority_code
     ) VALUES ($1, $2, $3, 0, $4, $5)
     ON CONFLICT (domain_event_id, user_id) DO NOTHING
     RETURNING user_id`,
    [domainEventId, userId, eventName, category, priority]
  );
  return (r.rowCount ?? 0) > 0;
}

export async function releaseClaim(pool: Pool, domainEventId: string, userId: string): Promise<void> {
  await pool.query(
    `DELETE FROM platform.notification_dispatch
     WHERE domain_event_id = $1 AND user_id = $2 AND sent_count = 0`,
    [domainEventId, userId]
  );
}

export async function markSent(
  pool: Pool,
  domainEventId: string,
  userId: string,
  sentCount: number,
  failureReason?: string | null
): Promise<void> {
  await pool.query(
    `UPDATE platform.notification_dispatch
     SET sent_count = $3,
         sent_at = now(),
         failure_reason = $4,
         delivery_channel = CASE WHEN $3 > 0 THEN 'PUSH' ELSE coalesce(delivery_channel, 'NONE') END
     WHERE domain_event_id = $1 AND user_id = $2`,
    [domainEventId, userId, sentCount, failureReason ?? null]
  );
}

export async function insertInboxRow(
  pool: Pool,
  input: {
    userId: string;
    domainEventId: string;
    eventName: string;
    category: NotificationCategory;
    priority: NotificationPriority;
    title: string;
    body: string;
    momentId: string | null;
    deepLink: string | null;
    actorUserId: string | null;
    actorDisplayName: string | null;
    digestPending: boolean;
    pushedAt: Date | null;
    explanationCode?: string | null;
    dedupeKey?: string | null;
    actionabilityScore?: number | null;
    decisionRoute?: string | null;
  }
): Promise<string> {
  const r = await pool.query<{ user_notification_id: string }>(
    `INSERT INTO platform.user_notification (
       user_id, domain_event_id, event_name, category_code, priority_code,
       title, body, moment_id, deep_link, actor_user_id, actor_display_name,
       digest_pending, pushed_at,
       explanation_code, dedupe_key, actionability_score, decision_route
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17)
     RETURNING user_notification_id`,
    [
      input.userId,
      input.domainEventId,
      input.eventName,
      input.category,
      input.priority,
      input.title,
      input.body,
      input.momentId,
      input.deepLink,
      input.actorUserId,
      input.actorDisplayName,
      input.digestPending,
      input.pushedAt,
      input.explanationCode ?? null,
      input.dedupeKey ?? null,
      input.actionabilityScore ?? null,
      input.decisionRoute ?? null,
    ]
  );
  return r.rows[0]!.user_notification_id;
}

export async function bumpDeliveryStats(
  pool: Pool,
  eventName: string,
  delta: {
    attempted?: number;
    sent?: number;
    failed?: number;
    revokedToken?: number;
    digestBatched?: number;
    inbox?: number;
  }
): Promise<void> {
  await pool.query(
    `INSERT INTO platform.notification_delivery_stats (
       stat_day, event_name, attempted_count, sent_count, failed_count,
       revoked_token_count, digest_batched_count, inbox_count
     ) VALUES (
       CURRENT_DATE, $1, $2, $3, $4, $5, $6, $7
     )
     ON CONFLICT (stat_day, event_name) DO UPDATE SET
       attempted_count = platform.notification_delivery_stats.attempted_count + EXCLUDED.attempted_count,
       sent_count = platform.notification_delivery_stats.sent_count + EXCLUDED.sent_count,
       failed_count = platform.notification_delivery_stats.failed_count + EXCLUDED.failed_count,
       revoked_token_count = platform.notification_delivery_stats.revoked_token_count + EXCLUDED.revoked_token_count,
       digest_batched_count = platform.notification_delivery_stats.digest_batched_count + EXCLUDED.digest_batched_count,
       inbox_count = platform.notification_delivery_stats.inbox_count + EXCLUDED.inbox_count,
       updated_at = now()`,
    [
      eventName,
      delta.attempted ?? 0,
      delta.sent ?? 0,
      delta.failed ?? 0,
      delta.revokedToken ?? 0,
      delta.digestBatched ?? 0,
      delta.inbox ?? 0,
    ]
  );
  await pool.query(
    `INSERT INTO platform.notification_delivery_stats (
       stat_day, event_name, attempted_count, sent_count, failed_count,
       revoked_token_count, digest_batched_count, inbox_count
     ) VALUES (
       CURRENT_DATE, '*', $1, $2, $3, $4, $5, $6
     )
     ON CONFLICT (stat_day, event_name) DO UPDATE SET
       attempted_count = platform.notification_delivery_stats.attempted_count + EXCLUDED.attempted_count,
       sent_count = platform.notification_delivery_stats.sent_count + EXCLUDED.sent_count,
       failed_count = platform.notification_delivery_stats.failed_count + EXCLUDED.failed_count,
       revoked_token_count = platform.notification_delivery_stats.revoked_token_count + EXCLUDED.revoked_token_count,
       digest_batched_count = platform.notification_delivery_stats.digest_batched_count + EXCLUDED.digest_batched_count,
       inbox_count = platform.notification_delivery_stats.inbox_count + EXCLUDED.inbox_count,
       updated_at = now()`,
    [
      delta.attempted ?? 0,
      delta.sent ?? 0,
      delta.failed ?? 0,
      delta.revokedToken ?? 0,
      delta.digestBatched ?? 0,
      delta.inbox ?? 0,
    ]
  );
}

export function enrichPayload(
  ev: DomainEventRow,
  actorDisplayName: string | null
): Record<string, unknown> {
  const base = { ...(ev.payload ?? {}) };
  if (actorDisplayName && !base.actorDisplayName) base.actorDisplayName = actorDisplayName;
  if (!base.momentId && ev.scope_id) base.momentId = ev.scope_id;
  return base;
}

/** Resolve participant_id → user_id for a moment (canonical finance enrichment). */
export async function mapParticipantUserIds(
  client: Pool | PoolClient,
  momentId: string,
  participantIds: string[]
): Promise<Map<string, string>> {
  const map = new Map<string, string>();
  if (participantIds.length === 0) return map;
  const r = await client.query<{ participant_id: string; user_id: string }>(
    `SELECT participant_id, user_id
     FROM collaboration.moment_participant
     WHERE moment_id = $1
       AND participant_id = ANY($2::uuid[])
       AND user_id IS NOT NULL`,
    [momentId, participantIds]
  );
  for (const row of r.rows) map.set(row.participant_id, row.user_id);
  return map;
}

export async function loadMomentTitle(
  client: Pool | PoolClient,
  momentId: string
): Promise<string | null> {
  const r = await client.query<{ title: string | null }>(
    `SELECT title FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  return r.rows[0]?.title ?? null;
}

export {
  isPeerPushEvent,
  shouldSkipPushForPayload,
  notificationCopy,
  notificationCategory,
  notificationPriority,
  deepLinkForEvent,
  PEER_PUSH_EVENT_NAMES,
};
