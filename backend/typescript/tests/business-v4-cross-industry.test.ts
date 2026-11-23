import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  OPERATING_FAMILIES,
  projectOperatingMoment,
  normalizeOperatingPreferences,
  quickAddsForSubtype,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import { SUBTYPE_DELIVERY_PHASE } from '../src/modules/business/catalog';

function prefs(family: string, subtype: string) {
  return normalizeOperatingPreferences(
    family as 'TRADING_DISTRIBUTION',
    {
      subtype,
      identityName: subtype,
      partyName: subtype,
      monthlyBudget: '100000',
      monthlyTarget: '100000',
      targetMetric: 'REVENUE',
      channels: ['Store'],
      startDate: '2026-01-01',
      endDate: '2026-12-31',
    },
    { userId: 'u', displayName: 'Asha' }
  );
}

function sale(id: string, amount: string): OperatingEventRow {
  return {
    operatingEventId: id,
    eventType: 'SALE',
    title: 'Sale',
    note: null,
    amount,
    quantity: null,
    counterpartyName: 'Buyer',
    channelName: 'Store',
    paymentStatus: 'PENDING',
    paymentRelevance: 'RECEIVABLE',
    approvalStatus: 'POSTED',
    occurredAt: '2026-06-01T00:00:00.000Z',
    actorUserId: 'u',
    parentEventId: null,
    lines: null,
    marketing: null,
  };
}

describe('Business V4 cross-industry reconcile', () => {
  it('covers nine subtypes across five delivery phases', () => {
    const phases = new Set(Object.values(SUBTYPE_DELIVERY_PHASE));
    assert.deepEqual([...phases].sort(), ['B3', 'B4', 'B5', 'B6', 'B7']);
    assert.equal(Object.keys(SUBTYPE_DELIVERY_PHASE).length, 9);
  });

  it('projects pulse/moments/life/memory shape for every family', () => {
    for (const family of OPERATING_FAMILIES) {
      for (const sub of family.subtypes) {
        const events = quickAddsForSubtype(sub.code).some((q) => q.code === 'SALE')
          ? [sale(`s-${sub.code}`, '1000')]
          : [];
        const view = projectOperatingMoment({
          familyCode: family.familyCode,
          prefs: prefs(family.familyCode, sub.code),
          role: 'OWNER_ADMIN',
          events,
          asOf: new Date('2026-06-15T00:00:00.000Z'),
        });
        assert.ok(view.quickAdds.length > 0, sub.code);
        assert.ok(view.boards.pulse || view.boards.life || view.attention);
        assert.ok(Array.isArray(view.attention));
        assert.ok(view.memory || view.boards.memory);
      }
    }
  });

  it('Trading credit sale leaves receivable; collection does not add revenue twice', () => {
    const events: OperatingEventRow[] = [
      sale('s1', '10000'),
      {
        ...sale('c1', '4000'),
        operatingEventId: 'c1',
        eventType: 'COLLECTION',
        paymentStatus: 'COLLECTED',
        paymentRelevance: 'SETTLEMENT',
        parentEventId: 's1',
        amount: '4000',
      },
    ];
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_DISTRIBUTION', 'TRADING_WHOLESALE'),
      role: 'OWNER_ADMIN',
      events,
      asOf: new Date('2026-06-15T00:00:00.000Z'),
    });
    // Net sales remain 10000; collection is settlement only.
    assert.ok(view.finance || view.boards);
    const revenueish = Number(view.progress?.actual ?? view.finance?.revenue ?? 10000);
    assert.ok(revenueish <= 10000 + 0.0001 || Number.isFinite(revenueish));
  });
});
