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

/** Earliest local hour a daily nudge may be sent. */
const DAILY_REMINDER_LOCAL_HOUR = 8;

/** Users handled per tick. Claimed users drop out, so the next tick sees the rest. */
const DAILY_REMINDER_BATCH = 500;

/**
 * Server DailyPersonalReminder is disabled.
 * Unconditional morning nudges stacked with the opt-in local evening check-in
 * and group digests. Prefer the Account "Evening check-in" local reminder.
 * Kept as a no-op so scheduler ticks / tests stay stable.
 */
export async function dispatchDailyPersonalReminders(
  _pool: Pool,
  _now = new Date()
): Promise<number> {
  void _pool;
  void _now;
  void DAILY_REMINDER_LOCAL_HOUR;
  void DAILY_REMINDER_BATCH;
  return 0;
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

/** Pref flags that map to the same event (payment + bill → one BillReminder/day). */
const GROUP_PREF_TO_EVENT: Array<{ prefs: string[]; event: GroupReminderKind }> = [
  { prefs: ['choreReminders'], event: 'ChoreReminder' },
  { prefs: ['billReminders', 'paymentReminders'], event: 'BillReminder' },
  { prefs: ['expenseReminders'], event: 'ExpenseReminder' },
  { prefs: ['photoReminders'], event: 'PhotoReminder' },
];

function prefEnabled(prefs: Record<string, unknown>, keys: string[]): boolean {
  return keys.some((key) => {
    const enabled = prefs[key];
    return (
      enabled === true ||
      (typeof enabled === 'string' && enabled.toLowerCase() === 'enabled')
    );
  });
}

type ActionableHit = { count: number; sampleTitle: string | null; body: string };

/** Open chores = open planning items categorized as chores, or open work.tasks. */
async function findOpenChores(pool: Pool, momentId: string): Promise<ActionableHit | null> {
  const planning = await pool.query<{ title: string; n: string }>(
    `SELECT title, COUNT(*) OVER()::text AS n
     FROM collaboration.planning_item
     WHERE moment_id = $1
       AND status IN ('OPEN', 'IN_PROGRESS')
       AND (
         category_code ILIKE '%chore%'
         OR lower(coalesce(category_code, '')) IN ('chores', 'chore')
         OR title ILIKE '%chore%'
       )
     ORDER BY due_at NULLS LAST, updated_at DESC
     LIMIT 1`,
    [momentId]
  );
  if (planning.rows[0]) {
    const count = parseInt(planning.rows[0].n, 10);
    return {
      count,
      sampleTitle: planning.rows[0].title,
      body:
        count === 1
          ? `“${planning.rows[0].title}” still needs doing.`
          : `${count} open chores — start with “${planning.rows[0].title}”.`,
    };
  }
  const tasks = await pool.query<{ title: string; n: string }>(
    `SELECT title, COUNT(*) OVER()::text AS n
     FROM work.task
     WHERE moment_id = $1
       AND status = 'OPEN'
     ORDER BY due_at NULLS LAST, updated_at DESC
     LIMIT 1`,
    [momentId]
  );
  if (tasks.rows[0]) {
    const count = parseInt(tasks.rows[0].n, 10);
    return {
      count,
      sampleTitle: tasks.rows[0].title,
      body:
        count === 1
          ? `“${tasks.rows[0].title}” still needs doing.`
          : `${count} open tasks — start with “${tasks.rows[0].title}”.`,
    };
  }
  return null;
}

/** Bills = open planning items in Bills category, or OPEN participant obligations. */
async function findOpenBills(pool: Pool, momentId: string): Promise<ActionableHit | null> {
  const planning = await pool.query<{ title: string; n: string }>(
    `SELECT title, COUNT(*) OVER()::text AS n
     FROM collaboration.planning_item
     WHERE moment_id = $1
       AND status IN ('OPEN', 'IN_PROGRESS')
       AND (
         category_code ILIKE '%bill%'
         OR lower(coalesce(category_code, '')) IN ('bills', 'bill', 'payments')
       )
     ORDER BY due_at NULLS LAST, updated_at DESC
     LIMIT 1`,
    [momentId]
  );
  if (planning.rows[0]) {
    const count = parseInt(planning.rows[0].n, 10);
    return {
      count,
      sampleTitle: planning.rows[0].title,
      body:
        count === 1
          ? `“${planning.rows[0].title}” still needs attention.`
          : `${count} open bills — start with “${planning.rows[0].title}”.`,
    };
  }
  const oblig = await pool.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM finance.participant_obligation
     WHERE moment_id = $1
       AND status IN ('OPEN', 'PARTIALLY_SETTLED')
       AND (original_amount - settled_amount) > 0`,
    [momentId]
  );
  const count = parseInt(oblig.rows[0]?.n ?? '0', 10);
  if (count <= 0) return null;
  return {
    count,
    sampleTitle: null,
    body: count === 1 ? 'One balance still needs settling.' : `${count} open balances need settling.`,
  };
}

/** Expense nudge only when unsettled OPEN obligations exist. */
async function findUnsettledExpenses(pool: Pool, momentId: string): Promise<ActionableHit | null> {
  const r = await pool.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM finance.participant_obligation
     WHERE moment_id = $1
       AND status IN ('OPEN', 'PARTIALLY_SETTLED')
       AND (original_amount - settled_amount) > 0`,
    [momentId]
  );
  const count = parseInt(r.rows[0]?.n ?? '0', 10);
  if (count <= 0) return null;
  return {
    count,
    sampleTitle: null,
    body:
      count === 1
        ? 'One shared expense still needs settling.'
        : `${count} shared balances still need settling.`,
  };
}

/** Photo nudge only when an active moment has no recent memories (7 days). */
async function findPhotoGap(pool: Pool, momentId: string): Promise<ActionableHit | null> {
  const moment = await pool.query<{ status: string }>(
    `SELECT status FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  if (moment.rows[0]?.status !== 'ACTIVE') return null;

  const recent = await pool.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM memory.memory
     WHERE moment_id = $1
       AND status = 'ACTIVE'
       AND created_at > now() - interval '7 days'`,
    [momentId]
  );
  if (parseInt(recent.rows[0]?.n ?? '0', 10) > 0) return null;
  return {
    count: 0,
    sampleTitle: null,
    body: 'No recent photos or memories — add one while it’s fresh.',
  };
}

async function actionableForKind(
  pool: Pool,
  momentId: string,
  event: GroupReminderKind
): Promise<ActionableHit | null> {
  switch (event) {
    case 'ChoreReminder':
      return findOpenChores(pool, momentId);
    case 'BillReminder':
      return findOpenBills(pool, momentId);
    case 'ExpenseReminder':
      return findUnsettledExpenses(pool, momentId);
    case 'PhotoReminder':
      return findPhotoGap(pool, momentId);
    default:
      return null;
  }
}

/**
 * Honor group reminder_preferences — one nudge per moment/kind/day,
 * only when there is actionable open work (no empty “may need attention” spam).
 */
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
      if (!prefEnabled(row.reminder_preferences, mapping.prefs)) continue;

      const hit = await actionableForKind(pool, row.moment_id, mapping.event);
      if (!hit) continue;

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
          title: hit.sampleTitle ?? undefined,
          body: hit.body,
          openCount: hit.count,
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
  daily: number;
  tasks: number;
  group: number;
  momentCompletions?: number;
  digests: number;
  signals?: {
    groupEmitted: number;
    businessEmitted: number;
    personalEmitted: number;
    momentDigestsEmitted: number;
  };
};

/**
 * Auto-complete ACTIVE group moments whose end_at has passed.
 * Generates Moment Story for each completion.
 */
export async function dispatchExpiredMomentCompletions(pool: Pool): Promise<number> {
  const due = await pool.query<{
    moment_id: string;
    version: string;
    organizer_user_id: string | null;
    title: string;
  }>(
    `SELECT m.moment_id, m.version::text,
            COALESCE(gmc.organizer_user_id, m.created_by_user_id) AS organizer_user_id,
            m.title
     FROM core.moment m
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     WHERE m.status = 'ACTIVE'
       AND m.domain_code = 'GROUP'
       AND m.end_at IS NOT NULL
       AND m.end_at <= now()
     ORDER BY m.end_at ASC
     LIMIT 25`
  );

  let completed = 0;
  const { completeMoment } = await import('../moment/service');
  for (const row of due.rows) {
    const key = `moment-complete:${row.moment_id}`;
    const actor = row.organizer_user_id;
    if (!actor) continue;
    if (!(await claimReminder(pool, key, actor, 'MomentCompleted', row.moment_id, { title: row.title }))) {
      continue;
    }
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const ctx = systemCtx(actor);
      await completeMoment(client, ctx, row.moment_id, null, { auto: true });
      await client.query('COMMIT');
      completed += 1;
    } catch (e) {
      await client.query('ROLLBACK');
      console.log(
        JSON.stringify({
          worker: 'scheduler',
          action: 'moment_auto_complete_failed',
          momentId: row.moment_id,
          error: e instanceof Error ? e.message : String(e),
        })
      );
    } finally {
      client.release();
    }
  }
  return completed;
}

export async function runReminderTick(pool: Pool): Promise<SchedulerTickResult> {
  const weekly = await dispatchWeeklyReminders(pool);
  const daily = await dispatchDailyPersonalReminders(pool);
  const tasks = await dispatchOverdueTaskReminders(pool);
  const group = await dispatchGroupSetupReminders(pool);
  const momentCompletions = await dispatchExpiredMomentCompletions(pool);
  // Moment-scoped digests run inside signal tick before global digest flush
  const { runDerivedSignalTick } = await import('../../platform/notifications/signals');
  const signalResult = await runDerivedSignalTick(pool);
  const digests = await flushDigests(pool);
  return {
    weekly,
    daily,
    tasks,
    group,
    momentCompletions,
    digests,
    signals: {
      groupEmitted: signalResult.group.emitted,
      businessEmitted: signalResult.business.emitted,
      personalEmitted: signalResult.personal.emitted,
      momentDigestsEmitted: signalResult.momentDigests.emitted,
    },
  };
}
