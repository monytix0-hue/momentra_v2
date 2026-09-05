import { randomUUID } from 'crypto';
import type { Pool } from 'pg';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import type { RequestContext } from '../../platform/request-context/context';

function systemCtx(userId: string): RequestContext {
  return {
    firebaseUid: `scheduler:${userId}`,
    firebaseProjectId: process.env.FIREBASE_PROJECT_ID ?? 'momentra',
    userId,
    correlationId: randomUUID(),
    roles: [],
    permissions: [],
  };
}

async function claimReminder(
  pool: Pool,
  reminderKey: string,
  userId: string,
  eventName: string,
  momentId: string | null,
  payload: Record<string, unknown>
): Promise<boolean> {
  const r = await pool.query(
    `INSERT INTO platform.reminder_dispatch (reminder_key, user_id, event_name, moment_id, payload)
     VALUES ($1, $2, $3, $4, $5::jsonb)
     ON CONFLICT (reminder_key) DO NOTHING
     RETURNING reminder_key`,
    [reminderKey, userId, eventName, momentId, JSON.stringify(payload)]
  );
  return (r.rowCount ?? 0) > 0;
}

async function emitReminder(
  pool: Pool,
  userId: string,
  eventName: string,
  momentId: string | null,
  payload: Record<string, unknown>
): Promise<void> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ctx = systemCtx(userId);
    await insertDomainEventAndOutbox(client, ctx, {
      eventName,
      domainCode: momentId ? 'GROUP' : 'PERSONAL',
      aggregateType: 'REMINDER',
      aggregateId: randomUUID(),
      scopeType: momentId ? 'MOMENT' : 'USER',
      scopeId: momentId ?? userId,
      payload: { ...payload, momentId: momentId ?? undefined, targetUserIds: [userId] },
    });
    await client.query('COMMIT');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

function isoWeekKey(d = new Date()): string {
  const tmp = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
  const day = tmp.getUTCDay() || 7;
  tmp.setUTCDate(tmp.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((tmp.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
  return `${tmp.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
}

/** Personal setup prefs with remindWeekly / reflectWeekly. */
export async function dispatchWeeklyReminders(pool: Pool): Promise<number> {
  const week = isoWeekKey();
  const rows = await pool.query<{
    user_id: string;
    moment_id: string;
    preferences: Record<string, unknown>;
    system_code: string;
  }>(
    `SELECT DISTINCT ON (lss.user_id, lss.system_code)
            lss.user_id, lss.moment_id, lss.preferences, lss.system_code
     FROM personal.life_system_setup lss
     JOIN core.user_profile up ON up.user_id = lss.user_id
     WHERE lss.status = 'ACTIVE'
       AND up.status = 'ACTIVE'
       AND up.push_notifications_enabled = true
       AND (
         (lss.preferences->>'remindWeekly')::boolean = true
         OR (lss.preferences->>'reflectWeekly')::boolean = true
       )
     ORDER BY lss.user_id, lss.system_code, lss.created_at DESC
     LIMIT 200`
  );

  let n = 0;
  for (const row of rows.rows) {
    const key = `weekly:${row.user_id}:${row.system_code}:${week}`;
    const body =
      row.system_code === 'LIFE_OPERATIONS'
        ? 'Reflect on this week’s life ops.'
        : `Weekly reminder for ${row.system_code.replace(/_/g, ' ').toLowerCase()}.`;
    const payload = {
      momentId: row.moment_id,
      systemCode: row.system_code,
      body,
      targetUserIds: [row.user_id],
    };
    if (!(await claimReminder(pool, key, row.user_id, 'WeeklyReminder', row.moment_id, payload))) {
      continue;
    }
    await emitReminder(pool, row.user_id, 'WeeklyReminder', row.moment_id, payload);
    n += 1;
  }
  return n;
}

/** Tasks due in the next 24h (OPEN). */
export async function dispatchOverdueTaskReminders(pool: Pool): Promise<number> {
  const rows = await pool.query<{
    task_id: string;
    moment_id: string;
    title: string;
    created_by_user_id: string;
    assignee_user_id: string | null;
    due_at: Date;
  }>(
    `SELECT t.task_id, t.moment_id, t.title, t.created_by_user_id, t.due_at,
            (
              SELECT a.assignee_user_id FROM work.assignment a
              WHERE a.task_id = t.task_id AND a.status = 'ACTIVE' AND a.assignee_user_id IS NOT NULL
              ORDER BY a.created_at DESC LIMIT 1
            ) AS assignee_user_id
     FROM work.task t
     WHERE t.status = 'OPEN'
       AND t.due_at IS NOT NULL
       AND t.due_at <= now() + interval '24 hours'
       AND t.due_at >= now() - interval '1 hour'
     ORDER BY t.due_at ASC
     LIMIT 100`
  );

  let n = 0;
  for (const row of rows.rows) {
    const userId = row.assignee_user_id ?? row.created_by_user_id;
    const day = row.due_at.toISOString().slice(0, 10);
    const key = `task-due:${row.task_id}:${day}`;
    const payload = {
      taskId: row.task_id,
      momentId: row.moment_id,
      title: row.title,
      targetUserIds: [userId],
      assigneeUserId: userId,
    };
    if (!(await claimReminder(pool, key, userId, 'TaskDueReminder', row.moment_id, payload))) {
      continue;
    }
    await emitReminder(pool, userId, 'TaskDueReminder', row.moment_id, payload);
    n += 1;
  }
  return n;
}

type GroupReminderKind = 'BillReminder' | 'ChoreReminder' | 'ExpenseReminder' | 'PhotoReminder';

const GROUP_PREF_TO_EVENT: Array<{ pref: string; event: GroupReminderKind }> = [
  { pref: 'billReminders', event: 'BillReminder' },
  { pref: 'choreReminders', event: 'ChoreReminder' },
  { pref: 'expenseReminders', event: 'ExpenseReminder' },
  { pref: 'photoReminders', event: 'PhotoReminder' },
  { pref: 'paymentReminders', event: 'BillReminder' },
];

/** Honor group reminder_preferences — one nudge per moment/kind/day. */
export async function dispatchGroupSetupReminders(pool: Pool): Promise<number> {
  const day = new Date().toISOString().slice(0, 10);
  const rows = await pool.query<{
    moment_id: string;
    organizer_user_id: string;
    reminder_preferences: Record<string, unknown>;
  }>(
    `SELECT moment_id, organizer_user_id, reminder_preferences
     FROM collaboration.group_moment_context
     WHERE status = 'ACTIVE'
       AND reminder_preferences IS NOT NULL
       AND reminder_preferences <> '{}'::jsonb
     LIMIT 200`
  );

  let n = 0;
  for (const row of rows.rows) {
    for (const mapping of GROUP_PREF_TO_EVENT) {
      const enabled = row.reminder_preferences[mapping.pref];
      // Accept boolean true or string "Enabled"
      const on =
        enabled === true ||
        (typeof enabled === 'string' && enabled.toLowerCase() === 'enabled');
      if (!on) continue;

      const members = await pool.query<{ user_id: string }>(
        `SELECT mp.user_id
         FROM collaboration.moment_participant mp
         JOIN core.user_profile up ON up.user_id = mp.user_id
         WHERE mp.moment_id = $1
           AND mp.status = 'ACTIVE'
           AND mp.user_id IS NOT NULL
           AND mp.notify_on_changes = true
           AND up.push_notifications_enabled = true
           AND up.status = 'ACTIVE'`,
        [row.moment_id]
      );

      for (const m of members.rows) {
        const key = `group:${mapping.event}:${row.moment_id}:${m.user_id}:${day}`;
        const payload = {
          momentId: row.moment_id,
          targetUserIds: [m.user_id],
        };
        if (!(await claimReminder(pool, key, m.user_id, mapping.event, row.moment_id, payload))) {
          continue;
        }
        await emitReminder(pool, m.user_id, mapping.event, row.moment_id, payload);
        n += 1;
      }
    }
  }
  return n;
}

/** Flush digest-batched inbox items after quiet hours. */
export async function flushDigests(pool: Pool): Promise<number> {
  const pending = await pool.query<{
    user_id: string;
    n: string;
    quiet_hours_start: string | null;
    quiet_hours_end: string | null;
    digest_enabled: boolean;
    timezone: string;
  }>(
    `SELECT un.user_id,
            COUNT(*)::text AS n,
            up.quiet_hours_start::text,
            up.quiet_hours_end::text,
            up.digest_enabled,
            coalesce(nullif(up.timezone, ''), 'UTC') AS timezone
     FROM platform.user_notification un
     JOIN core.user_profile up ON up.user_id = un.user_id
     WHERE un.digest_pending = true
       AND un.pushed_at IS NULL
       AND up.push_notifications_enabled = true
     GROUP BY un.user_id, up.quiet_hours_start, up.quiet_hours_end, up.digest_enabled, up.timezone
     HAVING COUNT(*) > 0
     LIMIT 100`
  );

  const { inQuietHours } = await import('../../platform/notifications/dispatch');
  let n = 0;
  for (const row of pending.rows) {
    const stillQuiet = inQuietHours(
      new Date(),
      row.timezone,
      row.quiet_hours_start,
      row.quiet_hours_end
    );
    // If digest_enabled forever, flush once/day outside quiet hours; if only quiet hours, flush when quiet ends
    if (stillQuiet) continue;

    const day = new Date().toISOString().slice(0, 10);
    const key = `digest:${row.user_id}:${day}`;
    const count = parseInt(row.n, 10);
    const payload = { count, targetUserIds: [row.user_id] };
    if (!(await claimReminder(pool, key, row.user_id, 'DigestReady', null, payload))) {
      continue;
    }
    await emitReminder(pool, row.user_id, 'DigestReady', null, payload);
    await pool.query(
      `UPDATE platform.user_notification
       SET digest_pending = false, pushed_at = coalesce(pushed_at, now())
       WHERE user_id = $1 AND digest_pending = true AND pushed_at IS NULL`,
      [row.user_id]
    );
    n += 1;
  }
  return n;
}

export type SchedulerTickResult = {
  weekly: number;
  tasks: number;
  group: number;
  digests: number;
};

export async function runReminderTick(pool: Pool): Promise<SchedulerTickResult> {
  const weekly = await dispatchWeeklyReminders(pool);
  const tasks = await dispatchOverdueTaskReminders(pool);
  const group = await dispatchGroupSetupReminders(pool);
  const digests = await flushDigests(pool);
  return { weekly, tasks, group, digests };
}
