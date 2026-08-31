import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import Decimal from 'decimal.js';
import { parseMoney } from '../src/modules/finance/service';
import { hashRequest } from '../src/platform/idempotency/store';

describe('finance.parseMoney', () => {
  it('accepts decimal strings without float drift', () => {
    const amount = parseMoney('12.5000');
    assert.equal(amount.toFixed(4), '12.5000');
    assert.equal(new Decimal('0.1').plus('0.2').toFixed(1), '0.3');
  });

  it('rejects non-positive amounts', () => {
    assert.throws(() => parseMoney('0'), /positive/);
  });
});

describe('idempotency.hashRequest', () => {
  it('is stable for same body', () => {
    const body = { a: 1, b: 'x' };
    assert.equal(hashRequest(body), hashRequest(body));
  });

  it('changes when body changes', () => {
    assert.notEqual(hashRequest({ a: 1 }), hashRequest({ a: 2 }));
  });
});
