import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { selectPushDevices } from '../src/platform/notifications/device-targets';
import { dispatchDailyPersonalReminders } from '../src/modules/notifications/reminders';

describe('selectPushDevices', () => {
  it('dedupes identical tokens and keeps one per platform (newest wins)', () => {
    const selected = selectPushDevices([
      {
        push_token: 'tok-a',
        platform: 'ANDROID',
        last_seen_at: '2026-01-01T00:00:00Z',
      },
      {
        push_token: 'tok-a',
        platform: 'ANDROID',
        last_seen_at: '2026-11-01T00:00:00Z',
      },
      {
        push_token: 'tok-b',
        platform: 'ANDROID',
        last_seen_at: '2026-10-01T00:00:00Z',
      },
      {
        push_token: 'tok-ios',
        platform: 'IOS',
        last_seen_at: '2026-11-01T00:00:00Z',
      },
      {
        push_token: 'tok-ios-old',
        platform: 'IOS',
        last_seen_at: '2026-01-01T00:00:00Z',
      },
    ]);
    assert.equal(selected.length, 2);
    const android = selected.find((d) => d.platform === 'ANDROID');
    const ios = selected.find((d) => d.platform === 'IOS');
    // Newest ANDROID among unique tokens is tok-a (Nov) over tok-b (Oct)
    assert.equal(android?.push_token, 'tok-a');
    assert.equal(ios?.push_token, 'tok-ios');
  });

  it('ignores blank tokens', () => {
    assert.deepEqual(
      selectPushDevices([{ push_token: '  ', platform: 'ANDROID', last_seen_at: null }]),
      []
    );
  });
});

describe('dispatchDailyPersonalReminders', () => {
  it('is a no-op (server daily personal disabled)', async () => {
    const n = await dispatchDailyPersonalReminders({} as never);
    assert.equal(n, 0);
  });
});
