import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  isPeerPushEvent,
  notificationCopy,
  shouldSkipPushForPayload,
} from '../src/platform/notifications/allowlist';

describe('notification allowlist', () => {
  it('treats group expenses as peer push', () => {
    assert.equal(isPeerPushEvent('GroupExpenseRecorded'), true);
    assert.equal(isPeerPushEvent('DeviceRegistered'), false);
    assert.equal(isPeerPushEvent('ProfileUpdated'), false);
  });

  it('skips GroupUpdatePosted when notifyMembers is false', () => {
    assert.equal(shouldSkipPushForPayload('GroupUpdatePosted', { notifyMembers: false }), true);
    assert.equal(shouldSkipPushForPayload('GroupUpdatePosted', { notifyMembers: true }), false);
    assert.equal(shouldSkipPushForPayload('GroupExpenseRecorded', { notifyMembers: false }), false);
  });

  it('returns copy for allowlisted events', () => {
    assert.equal(notificationCopy('PollCreated').title, 'New poll');
    assert.equal(notificationCopy('GroupUpdatePosted').title, 'New update');
  });
});
