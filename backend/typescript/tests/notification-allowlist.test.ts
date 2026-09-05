import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  bullmqPriority,
  deepLinkForEvent,
  isPeerPushEvent,
  notificationCategory,
  notificationCopy,
  notificationPriority,
  shouldSkipPushForPayload,
} from '../src/platform/notifications/allowlist';
import { categoryEnabled, inQuietHours, shouldDigest } from '../src/platform/notifications/dispatch';

describe('notification allowlist', () => {
  it('treats group and personal expenses as peer push', () => {
    assert.equal(isPeerPushEvent('GroupExpenseRecorded'), true);
    assert.equal(isPeerPushEvent('ExpenseRecorded'), true);
    assert.equal(isPeerPushEvent('DeviceRegistered'), false);
    assert.equal(isPeerPushEvent('ProfileUpdated'), false);
  });

  it('skips GroupUpdatePosted when notifyMembers is false', () => {
    assert.equal(shouldSkipPushForPayload('GroupUpdatePosted', { notifyMembers: false }), true);
    assert.equal(shouldSkipPushForPayload('GroupUpdatePosted', { notifyMembers: true }), false);
    assert.equal(shouldSkipPushForPayload('GroupExpenseRecorded', { notifyMembers: false }), false);
  });

  it('returns richer copy with actor and title', () => {
    const copy = notificationCopy('PollCreated', {
      actorDisplayName: 'Sam',
      title: 'Dinner spot?',
    });
    assert.equal(copy.title, 'New poll');
    assert.match(copy.body, /Sam/);
    assert.match(copy.body, /Dinner spot/);
  });

  it('maps categories and priorities', () => {
    assert.equal(notificationCategory('SettlementRecorded'), 'finance');
    assert.equal(notificationCategory('ApprovalRequested'), 'approvals');
    assert.equal(notificationPriority('ApprovalRequested'), 'HIGH');
    assert.equal(notificationPriority('PollVoted'), 'LOW');
    assert.equal(bullmqPriority('HIGH'), 1);
    assert.ok(bullmqPriority('LOW') > bullmqPriority('NORMAL'));
  });

  it('builds deep links', () => {
    assert.equal(
      deepLinkForEvent('TaskCreated', { momentId: '11111111-1111-1111-1111-111111111111' }),
      'momentra://moment/11111111-1111-1111-1111-111111111111?category=tasks&event=TaskCreated'
    );
    assert.equal(deepLinkForEvent('DigestReady', {}), 'momentra://inbox');
  });
});

describe('notification digest / quiet hours', () => {
  it('respects category toggles', () => {
    assert.equal(categoryEnabled({ finance: false }, 'finance'), false);
    assert.equal(categoryEnabled({ finance: false }, 'tasks'), true);
    assert.equal(categoryEnabled(null, 'finance'), true);
  });

  it('detects quiet hours across midnight', () => {
    // 23:30 UTC — construct a known Date and use UTC timezone
    const late = new Date('2026-01-01T23:30:00Z');
    assert.equal(inQuietHours(late, 'UTC', '22:00', '07:00'), true);
    const noon = new Date('2026-01-01T12:00:00Z');
    assert.equal(inQuietHours(noon, 'UTC', '22:00', '07:00'), false);
  });

  it('never digests HIGH priority', () => {
    assert.equal(
      shouldDigest(
        {
          digest_enabled: true,
          quiet_hours_start: '22:00',
          quiet_hours_end: '07:00',
          timezone: 'UTC',
        },
        'HIGH',
        new Date('2026-01-01T23:30:00Z')
      ),
      false
    );
    assert.equal(
      shouldDigest(
        {
          digest_enabled: true,
          quiet_hours_start: null,
          quiet_hours_end: null,
          timezone: 'UTC',
        },
        'NORMAL',
        new Date()
      ),
      true
    );
  });
});
