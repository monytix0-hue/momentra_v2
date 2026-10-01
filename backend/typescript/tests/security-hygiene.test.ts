import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { redactForLog } from '../src/platform/observability/sensitive-redaction';
import { lockScreenPush } from '../src/platform/notifications/lock-screen';
import { SIGNED_DOWNLOAD_TTL_SEC } from '../src/modules/media/service';

describe('security hygiene', () => {
  it('redacts notes, memory, mood, amounts, tokens, and signed URLs', () => {
    const sample = {
      route: '/v1/moments/x/expenses/y',
      note: 'private dinner note',
      description: 'rent for March',
      memoryText: 'the day we moved',
      mood: 'anxious and tired',
      amount: '1240.00',
      token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig',
      downloadUrl: 'https://example.supabase.co/storage/v1/object/sign/momentra-media/a?token=abc',
      nested: { feeling: 'lonely', safe: 'ok' },
    };
    const redacted = redactForLog(sample) as Record<string, unknown>;
    const serialized = JSON.stringify(redacted);
    assert.equal(redacted.route, '/v1/moments/x/expenses/y');
    assert.equal(redacted.note, '[redacted]');
    assert.equal(redacted.amount, '[redacted]');
    assert.equal(redacted.downloadUrl, '[redacted]');
    assert.doesNotMatch(serialized, /private dinner note/);
    assert.doesNotMatch(serialized, /rent for March/);
    assert.doesNotMatch(serialized, /the day we moved/);
    assert.doesNotMatch(serialized, /anxious/);
    assert.doesNotMatch(serialized, /1240/);
    assert.doesNotMatch(serialized, /eyJhbGci/);
    assert.doesNotMatch(serialized, /supabase\.co\/storage/);
    assert.doesNotMatch(serialized, /lonely/);
    assert.equal((redacted.nested as { safe: string }).safe, 'ok');
  });

  it('keeps money and mood off the lock-screen push', () => {
    const money = lockScreenPush('Goa Trip · Dinner', 'Your share is ₹310');
    assert.equal(money.title, 'Momentra');
    assert.equal(money.body, 'You have an update in Momentra.');
    assert.doesNotMatch(`${money.title} ${money.body}`, /₹|310|Dinner/);

    const mood = lockScreenPush('Today', 'You logged an anxious mood');
    assert.equal(mood.body, 'You have an update in Momentra.');
    assert.doesNotMatch(mood.body, /anxious/);

    const plain = lockScreenPush('Trip update', 'Sam added a plan.');
    assert.equal(plain.title, 'Trip update');
    assert.equal(plain.body, 'Sam added a plan.');
  });

  it('uses a short signed download TTL and not a permanent URL', () => {
    assert.ok(SIGNED_DOWNLOAD_TTL_SEC <= 900);
    assert.ok(SIGNED_DOWNLOAD_TTL_SEC >= 60);
  });
});
