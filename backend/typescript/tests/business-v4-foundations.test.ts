import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { budgetUsePercent, assertNoDoubleRevenue } from '../src/modules/business/finance-bridge';
import { SUBTYPE_DELIVERY_PHASE, OPERATING_FAMILIES } from '../src/modules/business/catalog';
import { quickAddsForSubtype, isOperatingSubtype } from '../src/modules/business/operating-catalog';

describe('Business V4 foundations', () => {
  it('maps all nine subtypes to delivery phases', () => {
    const codes = Object.keys(SUBTYPE_DELIVERY_PHASE);
    assert.equal(codes.length, 9);
    assert.equal(SUBTYPE_DELIVERY_PHASE.TRADING_WHOLESALE, 'B3');
    assert.equal(SUBTYPE_DELIVERY_PHASE.DISTRIBUTION_DEALERSHIP, 'B4');
    assert.equal(SUBTYPE_DELIVERY_PHASE.CLIENT_CAMPAIGN, 'B5');
    assert.equal(SUBTYPE_DELIVERY_PHASE.OWN_BUSINESS_CAMPAIGN, 'B5');
  });

  it('exposes four operating families in catalog', () => {
    assert.equal(OPERATING_FAMILIES.length, 4);
    for (const f of OPERATING_FAMILIES) {
      for (const s of f.subtypes) {
        assert.equal(isOperatingSubtype(s.code), true);
        assert.ok(quickAddsForSubtype(s.code).length > 0);
      }
    }
  });

  it('AT-04 budget use 350000/550000 = 63.64%', () => {
    assert.equal(budgetUsePercent(350_000, 550_000), 63.64);
  });

  it('collection does not increase revenue', () => {
    const r = assertNoDoubleRevenue({ postedRevenue: 650_000, collectionAmount: 400_000 });
    assert.equal(r.revenue, 650_000);
    assert.equal(r.cash, 400_000);
  });

  it('missing budget denominator is unavailable', () => {
    assert.equal(budgetUsePercent(100, 0), null);
  });

  it('Client has invoice+collection; Own does not', () => {
    const client = new Set(quickAddsForSubtype('CLIENT_CAMPAIGN').map((q) => q.code));
    const own = new Set(quickAddsForSubtype('OWN_BUSINESS_CAMPAIGN').map((q) => q.code));
    assert.equal(client.has('INVOICE'), true);
    assert.equal(client.has('COLLECTION'), true);
    assert.equal(own.has('INVOICE'), false);
    assert.equal(own.has('COLLECTION'), false);
    assert.equal(client.size, 10); // 8 shared + INVOICE + COLLECTION
    assert.equal(own.size, 8);
  });
});
