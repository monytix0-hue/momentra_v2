/**
 * Notification pipeline smoke checks (unit + contract).
 * Run: npx tsx --test tests/notification-pipeline.test.ts
 *
 * Live E2E (manual / CI with Redis+FCM):
 * 1. REDIS_URL + FIREBASE_* set; run outbox-dispatcher + notification-worker + scheduler
 * 2. Register device via POST /v1/me/devices with FCM token
 * 3. Create group expense with notify peers → expect notification_dispatch + inbox row
 * 4. PATCH prefs digestEnabled=true → next peer event inbox-only until digest flush
 * 5. Scheduler tick emits TaskDueReminder / WeeklyReminder when due
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { PEER_PUSH_EVENT_NAMES, notificationPriority } from '../src/platform/notifications/allowlist';
import { enqueueNotificationJob, NOTIFICATION_QUEUE_NAME } from '../src/platform/queue/notification-queue';

describe('notification pipeline contract', () => {
  it('allowlists reminder + digest events for scheduler path', () => {
    for (const name of [
      'WeeklyReminder',
      'TaskDueReminder',
      'BillReminder',
      'DigestReady',
      'ExpenseRecorded',
    ]) {
      assert.equal(PEER_PUSH_EVENT_NAMES.has(name), true, name);
    }
  });

  it('exposes a single prioritized BullMQ queue name', () => {
    assert.equal(NOTIFICATION_QUEUE_NAME, 'momentra-notifications');
    assert.equal(notificationPriority('ApprovalRequested'), 'HIGH');
  });

  it('enqueueNotificationJob no-ops without REDIS_URL', async () => {
    const prev = process.env.REDIS_URL;
    delete process.env.REDIS_URL;
    const ok = await enqueueNotificationJob({
      outboxEventId: '00000000-0000-0000-0000-000000000001',
      domainEventId: '00000000-0000-0000-0000-000000000002',
      topicCode: 'TASKCREATED',
      eventName: 'TaskCreated',
    });
    assert.equal(ok, false);
    if (prev !== undefined) process.env.REDIS_URL = prev;
  });
});
