/**
 * Unit invariants for Group expense position math (no DB).
 * Catches payer net = share (wrong) vs paid − allocated (correct).
 */
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import Decimal from 'decimal.js';
import {
  computeExpensePositionDeltas,
  computeGroupShares,
} from '../src/modules/finance/group-expense';

const A = '11111111-1111-1111-1111-111111111111';
const B = '22222222-2222-2222-2222-222222222222';
const C = '33333333-3333-3333-3333-333333333333';

function assertNetsSumToZero(
  deltas: ReturnType<typeof computeExpensePositionDeltas>
): void {
  const sum = deltas.reduce((acc, d) => acc.plus(d.netDelta), new Decimal(0));
  assert.ok(sum.eq(0), `sum(nets) must be 0, got ${sum.toFixed(4)}`);
}

describe('Group expense position invariants', () => {
  it('3-person EQUAL: payer net = paid − allocated; nets sum to 0', () => {
    const amount = new Decimal('100');
    const shares = computeGroupShares('EQUAL', amount, [
      { participantId: A },
      { participantId: B },
      { participantId: C },
    ]);
    const deltas = computeExpensePositionDeltas(A, amount, shares);
    const payer = deltas.find((d) => d.participantId === A)!;
    assert.ok(
      payer.netDelta.eq(payer.paidDelta.minus(payer.allocatedDelta)),
      `payer net ${payer.netDelta} != paid-allocated`
    );
    // ~66.6667 owed to payer (100 − 33.3334 after remainder)
    assert.ok(payer.netDelta.gt(new Decimal('66')), `expected ~66.67 got ${payer.netDelta}`);
    assert.ok(!payer.netDelta.eq(payer.allocatedDelta), 'payer net must not equal share alone');
    assertNetsSumToZero(deltas);
  });

  it('PERCENTAGE 70/30: payer net = 30 when paid 100', () => {
    const amount = new Decimal('100');
    const shares = computeGroupShares('PERCENTAGE', amount, [
      { participantId: A, percent: '70' },
      { participantId: B, percent: '30' },
    ]);
    const deltas = computeExpensePositionDeltas(A, amount, shares);
    const payer = deltas.find((d) => d.participantId === A)!;
    const other = deltas.find((d) => d.participantId === B)!;
    assert.ok(payer.netDelta.eq(new Decimal('30')), `payer net ${payer.netDelta}`);
    assert.ok(other.netDelta.eq(new Decimal('-30')), `other net ${other.netDelta}`);
    assert.ok(payer.netDelta.eq(payer.paidDelta.minus(payer.allocatedDelta)));
    assertNetsSumToZero(deltas);
  });

  it('EXACT custom 60/40: payer net = 40 when paid 100', () => {
    const amount = new Decimal('100');
    const shares = computeGroupShares('EXACT', amount, [
      { participantId: A, amount: '60' },
      { participantId: B, amount: '40' },
    ]);
    const deltas = computeExpensePositionDeltas(A, amount, shares);
    const payer = deltas.find((d) => d.participantId === A)!;
    assert.ok(payer.netDelta.eq(new Decimal('40')), `payer net ${payer.netDelta}`);
    assert.ok(payer.netDelta.eq(payer.paidDelta.minus(payer.allocatedDelta)));
    assertNetsSumToZero(deltas);
  });

  it('2-person EQUAL still nets to ±50', () => {
    const amount = new Decimal('100');
    const shares = computeGroupShares('EQUAL', amount, [
      { participantId: A },
      { participantId: B },
    ]);
    const deltas = computeExpensePositionDeltas(A, amount, shares);
    const payer = deltas.find((d) => d.participantId === A)!;
    assert.ok(payer.netDelta.eq(new Decimal('50')));
    assertNetsSumToZero(deltas);
  });
});
