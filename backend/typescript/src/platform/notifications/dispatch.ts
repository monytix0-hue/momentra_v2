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

export type DomainEventRow = {
  domain_event_id: string;
  actor_user_id: string;
  event_name: string;
  scope_id: string | null;
  payload: Record<string, unknown> | null;
};

export type RecipientPrefs = {
  user_id: string;
  push_notifications_enabled: boolean;
  notification_categories: Record<string, boolean> | null;
  quiet_hours_start: string | null;
  quiet_hours_end: string | null;
  digest_enabled: boolean;
  timezone: string;
};

function asCategoryMap(raw: unknown): Record<string, boolean> {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
  const out: Record<string, boolean> = {};
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof v === 'boolean') out[k] = v;
  }
  return out;
}

export function categoryEnabled(
  cats: Record<string, boolean> | null | undefined,
  category: NotificationCategory
): boolean {
  if (!cats || Object.keys(cats).length === 0) return true;
  if (category === 'system') return true;
  return cats[category] !== false;
}

/** Quiet hours use profile local time (HH:MM[:SS]). Cross-midnight supported. */
export function inQuietHours(
  now: Date,
  timezone: string,
  start: string | null,
  end: string | null
): boolean {
  if (!start || !end) return false;
  try {
    const local = new Intl.DateTimeFormat('en-GB', {
      timeZone: timezone || 'UTC',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(now);
    const [hh, mm] = local.split(':').map((x) => parseInt(x, 10));
    const nowMins = hh * 60 + mm;
    const parse = (t: string) => {
      const parts = t.split(':').map((x) => parseInt(x, 10));
      return (parts[0] ?? 0) * 60 + (parts[1] ?? 0);
    };
    const s = parse(start);
    const e = parse(end);
    if (s === e) return false;
    if (s < e) return nowMins >= s && nowMins < e;
    return nowMins >= s || nowMins < e;
  } catch {
    return false;
  }
}

export function shouldDigest(
  prefs: Pick<RecipientPrefs, 'digest_enabled' | 'quiet_hours_start' | 'quiet_hours_end' | 'timezone'>,
  priority: NotificationPriority,
  now = new Date()
): boolean {
  if (priority === 'HIGH') return false;
  if (prefs.digest_enabled) return true;
  return inQuietHours(now, prefs.timezone, prefs.quiet_hours_start, prefs.quiet_hours_end);
}

export async function loadDomainEvent(pool: Pool, domainEventId: string): Promise<DomainEventRow | null> {
  const r = await pool.query<DomainEventRow>(
    `SELECT domain_event_id, actor_user_id, event_name, scope_id, payload
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

/**
 * Recipients:
 * - payload.targetUserIds / assigneeUserId / approverUserIds when present (targeted)
 * - else all other active moment participants with notify_on_changes
 */
export async function resolveRecipients(
  pool: Pool,
  ev: DomainEventRow
): Promise<RecipientPrefs[]> {
  const payload = ev.payload ?? {};
  const momentId =
    (typeof payload.momentId === 'string' ? payload.momentId : null) ?? ev.scope_id;

  const targeted: string[] = [];
  if (Array.isArray(payload.targetUserIds)) {
    for (const id of payload.targetUserIds) {
      if (typeof id === 'string') targeted.push(id);
    }
  }
  if (typeof payload.assigneeUserId === 'string') targeted.push(payload.assigneeUserId);
  if (Array.isArray(payload.approverUserIds)) {
    for (const id of payload.approverUserIds) {
      if (typeof id === 'string') targeted.push(id);
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
      if (row.assignee_user_id) targeted.push(row.assignee_user_id);
    }
  }

  // Pending approval requesters' peers with ADMIN/OWNER role when ApprovalRequested
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
    for (const row of admins.rows) targeted.push(row.user_id);
  }

  const uniqueTargets = [...new Set(targeted)].filter((id) => Boolean(id));

  if (uniqueTargets.length > 0) {
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
      [uniqueTargets]
    );
    return r.rows.map((row) => ({
      ...row,
      notification_categories: asCategoryMap(row.notification_categories),
    }));
  }

  if (!momentId) return [];

  const peers = await pool.query<RecipientPrefs>(
    `SELECT up.user_id,
            up.push_notifications_enabled,
            up.notification_categories,
            up.quiet_hours_start::text,
            up.quiet_hours_end::text,
            up.digest_enabled,
            coalesce(nullif(up.timezone, ''), 'UTC') AS timezone
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
  }
): Promise<string> {
  const r = await pool.query<{ user_notification_id: string }>(
    `INSERT INTO platform.user_notification (
       user_id, domain_event_id, event_name, category_code, priority_code,
       title, body, moment_id, deep_link, actor_user_id, actor_display_name,
       digest_pending, pushed_at
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
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
  // Also roll up to wildcard row
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

export {
  isPeerPushEvent,
  shouldSkipPushForPayload,
  notificationCopy,
  notificationCategory,
  notificationPriority,
  deepLinkForEvent,
  PEER_PUSH_EVENT_NAMES,
};
