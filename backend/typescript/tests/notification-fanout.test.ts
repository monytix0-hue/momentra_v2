/**
 * Group peer fan-out + daily personal reminder scheduling.
 * Run: npx tsx --test tests/notification-fanout.test.ts
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { Pool } from 'pg';
import { resolveRecipients } from '../src/platform/notifications/dispatch';
import { dispatchDailyPersonalReminders } from '../src/modules/notifications/reminders';

type QueryCall = { sql: string; params: unknown[] };

/** Minimal Pool stand-in: canned rows per call, plus a transcript of the SQL issued. */
function stubPool(responses: Array<{ rows: unknown[]; rowCount?: number }>) {
  const calls: QueryCall[] = [];
  let i = 0;
  const query = async (sql: unknown, params?: unknown[]) => {
    calls.push({ sql: String(sql), params: params ?? [] });
    const next = responses[i] ?? { rows: [] };
    i += 1;
    return { rows: next.rows, rowCount: next.rowCount ?? next.rows.length };
  };
  const pool = {
    query,
    connect: async () => ({ query, release: () => undefined }),
  };
  return { pool: pool as unknown as Pool, calls };
}

const PROFILE_ROW = {
  user_id: 'b1000000-0000-0000-0000-000000000001',
  push_notifications_enabled: true,
  notification_categories: null,
  quiet_hours_start: null,
  quiet_hours_end: null,
  digest_enabled: false,
  timezone: 'UTC',
};

describe('group peer fan-out', () => {
  it('notifies the members stamped on the event, not the actor', async () => {
    const { pool, calls } = stubPool([{ rows: [PROFILE_ROW] }]);

    const recipients = await resolveRecipients(pool, {
      domain_event_id: 'e0000000-0000-0000-0000-000000000001',
      actor_user_id: 'a0000000-0000-0000-0000-000000000001',
      event_name: 'GroupExpenseRecorded',
      scope_id: 'm0000000-0000-0000-0000-000000000001',
      payload: {
        momentId: 'm0000000-0000-0000-0000-000000000001',
        targetUserIds: [PROFILE_ROW.user_id],
      },
    } as never);

    assert.equal(recipients.length, 1);
    assert.equal(recipients[0]!.user_id, PROFILE_ROW.user_id);
    // Targeted branch only — the actor-excluding participant scan is never reached.
    assert.equal(calls.length, 1);
    assert.match(calls[0]!.sql, /user_id = ANY/);
    assert.deepEqual(calls[0]!.params[0], [PROFILE_ROW.user_id]);
  });

  it('falls back to moment participants when no members are stamped', async () => {
    const { pool, calls } = stubPool([{ rows: [PROFILE_ROW] }]);

    await resolveRecipients(pool, {
      domain_event_id: 'e0000000-0000-0000-0000-000000000002',
      actor_user_id: 'a0000000-0000-0000-0000-000000000001',
      event_name: 'GroupExpenseRecorded',
      scope_id: 'm0000000-0000-0000-0000-000000000001',
      payload: { momentId: 'm0000000-0000-0000-0000-000000000001', targetUserIds: [] },
    } as never);

    assert.equal(calls.length, 1);
    assert.match(calls[0]!.sql, /moment_participant/);
    assert.match(calls[0]!.sql, /notify_on_changes/);
  });
});

describe('daily personal reminder', () => {
  const personalRow = {
    user_id: 'c0000000-0000-0000-0000-000000000001',
    moment_id: 'm0000000-0000-0000-0000-000000000009',
    local_day: '2026-03-02',
  };

  it('gates on 08:00 local time and skips users already reminded today', async () => {
    const { pool, calls } = stubPool([{ rows: [] }]);
    const now = new Date('2026-03-02T05:00:00Z');
    const sent = await dispatchDailyPersonalReminders(pool, now);

    assert.equal(sent, 0);
    assert.equal(calls.length, 1);
    const select = calls[0]!;
    // Both gates live in SQL so the per-tick batch advances instead of
    // re-selecting the same users forever.
    assert.match(select.sql, /extract\(hour FROM e\.local_now\) >= \$2/);
    assert.match(select.sql, /NOT EXISTS/);
    assert.match(select.sql, /reminder_dispatch/);
    assert.equal(select.params[0], now.toISOString());
    assert.equal(select.params[1], 8);
  });

  it('emits one targeted reminder per eligible user', async () => {
    const { pool, calls } = stubPool([
      { rows: [personalRow] },
      { rows: [{ reminder_key: 'claimed' }] }, // claimReminder wins the race
      { rows: [] }, // BEGIN
      { rows: [{ domain_event_id: 'd1', outbox_event_id: 'o1' }] },
      { rows: [] }, // COMMIT
    ]);
    const sent = await dispatchDailyPersonalReminders(pool, new Date('2026-03-02T05:00:00Z'));

    assert.equal(sent, 1);
    const claim = calls[1]!;
    assert.match(claim.sql, /reminder_dispatch/);
    assert.equal(claim.params[0], `daily-personal:${personalRow.user_id}:2026-03-02`);
    assert.equal(claim.params[2], 'DailyPersonalReminder');

    const event = calls.find((c) => c.sql.includes('domain_event'));
    assert.ok(event, 'expected a domain event insert');
    const payload = JSON.parse(String(event!.params[9]));
    assert.deepEqual(payload.targetUserIds, [personalRow.user_id]);
    assert.equal(payload.momentId, personalRow.moment_id);
  });

  it('does not resend when another scheduler already claimed the day', async () => {
    const { pool } = stubPool([
      { rows: [personalRow] },
      { rows: [], rowCount: 0 }, // ON CONFLICT DO NOTHING — already sent today
    ]);
    const sent = await dispatchDailyPersonalReminders(pool, new Date('2026-03-02T05:00:00Z'));

    assert.equal(sent, 0);
  });
});
