/**
 * Non-prod qa-* correlation token acceptance (Q0).
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { normalizeCorrelationId } from '../src/platform/observability/correlation';

describe('normalizeCorrelationId', () => {
  it('accepts UUID', () => {
    const id = '550e8400-e29b-41d4-a716-446655440000';
    assert.equal(normalizeCorrelationId(id), id);
  });

  it('accepts qa-* tokens when not production', () => {
    const prev = process.env.NODE_ENV;
    process.env.NODE_ENV = 'development';
    const token = 'qa-20260827-personal-lifeops-expense-001';
    assert.equal(normalizeCorrelationId(token), token);
    process.env.NODE_ENV = prev;
  });

  it('rejects qa-* tokens in production', () => {
    const prev = process.env.NODE_ENV;
    process.env.NODE_ENV = 'production';
    const out = normalizeCorrelationId('qa-20260827-personal-lifeops-expense-001');
    assert.notEqual(out, 'qa-20260827-personal-lifeops-expense-001');
    assert.match(out, /^[0-9a-f-]{36}$/i);
    process.env.NODE_ENV = prev;
  });
});
