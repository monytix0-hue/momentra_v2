/**
 * Behaviour under test: P3 widget coverageMatrix + projectWidgetsFromProjection
 * for Trading → Distribution → Retail trio → MFG (shared engine + industry extras).
 * Modules: operating-widget-engine / catalog / service.
 * Existing tests: operating-trading-analytics.test.ts, operating-retail-analytics.test.ts
 * (board figures only — no formal Widget ID contracts).
 * User: "Implement and verify in this order: Trading, Distribution, Marketing,
 * Retail Store/Restaurant/Consumer Service, Standard + MTO Manufacturing."
 * User: "If eligible backfill residual is positive... metrics must be null/unavailable
 * rather than zero."
 */
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import { coverageMatrix, NINE_SUBTYPES, widgetsForSubtype } from '../src/modules/business/operating-widget-catalog';
import { projectWidgetsFromProjection } from '../src/modules/business/operating-widget-service';
import { resolveCoverage, isEffectiveForAnalytics } from '../src/modules/business/operating-widget-engine';

const FAMILIES: Record<string, string> = {
  TRADING_WHOLESALE: 'TRADING_DISTRIBUTION',
  DISTRIBUTION_DEALERSHIP: 'TRADING_DISTRIBUTION',
  CLIENT_CAMPAIGN: 'MARKETING_CAMPAIGNS',
  OWN_BUSINESS_CAMPAIGN: 'MARKETING_CAMPAIGNS',
  RETAIL_STORE: 'RETAIL_CONSUMER',
  RESTAURANT_FOOD: 'RETAIL_CONSUMER',
  CONSUMER_SERVICE: 'RETAIL_CONSUMER',
  STANDARD_PRODUCTION: 'MANUFACTURING_PRODUCTION',
  MADE_TO_ORDER: 'MANUFACTURING_PRODUCTION',
};

function prefs(family: string, subtype: string) {
  return normalizeOperatingPreferences(
    family,
    {
      subtype,
      identityName: subtype,
      partyName: `${subtype} Co`,
      monthlyBudget: '100000',
      monthlyTarget: '200000',
      targetMetric:
        family === 'MARKETING_CAMPAIGNS'
          ? 'LEADS'
          : family === 'MANUFACTURING_PRODUCTION'
            ? 'PRODUCTION_OUTPUT'
            : 'REVENUE',
      channels: ['Walk-in'],
      startDate: '2026-11-01',
      endDate: family === 'MARKETING_CAMPAIGNS' ? '2026-11-30' : '',
      ...(family === 'MANUFACTURING_PRODUCTION' ? { plantName: 'Plant', monthlyOutput: '1000' } : {}),
      ...(subtype === 'RETAIL_STORE' ? { storeName: 'Store', format: 'Kirana' } : {}),
    },
    { userId: 'u', displayName: 'Asha' }
  );
}

function event(id: string, eventType: string, fields: Partial<OperatingEventRow> = {}): OperatingEventRow {
  return {
    operatingEventId: id,
    eventType,
    title: fields.title ?? eventType,
    note: null,
    amount: null,
    currencyCode: 'INR',
    counterpartyName: null,
    channelName: 'Walk-in',
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: '2026-11-10T10:00:00.000Z',
    ...fields,
  };
}

describe('P3 shared coverage engine', () => {
  it('distinguishes UNAVAILABLE vs ZERO vs COMPLETE', () => {
    assert.equal(resolveCoverage({ eligibleCount: 0, measuredValue: 0 }), 'UNAVAILABLE');
    assert.equal(resolveCoverage({ eligibleCount: 1, measuredValue: 0 }), 'ZERO');
    assert.equal(resolveCoverage({ eligibleCount: 1, measuredValue: 10 }), 'COMPLETE');
    assert.equal(resolveCoverage({ eligibleCount: 1, measuredValue: 10, residualIncomplete: true }), 'UNAVAILABLE');
    assert.equal(resolveCoverage({ eligibleCount: 1, measuredValue: 10, postingComplete: false }), 'PARTIAL');
  });

  it('excludes SUPERSEDED and REVERSAL from analytics eligibility', () => {
    assert.equal(
      isEffectiveForAnalytics({
        operatingEventId: '1',
        eventType: 'SPEND',
        amount: '100',
        approvalStatus: 'NONE',
        paymentStatus: 'PENDING',
        ledgerStatus: 'SUPERSEDED',
      }),
      false
    );
    assert.equal(
      isEffectiveForAnalytics({
        operatingEventId: '2',
        eventType: 'REVERSAL',
        amount: '100',
        approvalStatus: 'NONE',
        paymentStatus: 'NONE',
      }),
      false
    );
  });
});

describe('P3 widget coverage — all nine subtypes', () => {
  for (const subtype of NINE_SUBTYPES) {
    it(`${subtype} exposes P01–P05, M01–M04, L01–L04, ME01–ME04`, () => {
      const family = FAMILIES[subtype]!;
      const matrix = coverageMatrix(family, subtype);
      const ids = new Set(matrix.map((w) => w.widgetId));
      for (const id of [
        'P01', 'P02', 'P03', 'P04', 'P05',
        'M01', 'M02', 'M03', 'M04',
        'L01', 'L02', 'L03', 'L04',
        'ME01', 'ME02', 'ME03', 'ME04',
      ]) {
        assert.ok(ids.has(id), `${subtype} missing ${id}`);
      }
      assert.ok(widgetsForSubtype(family, subtype).every((w) => w.formula && w.historicalRule));
    });
  }
});

describe('P3.1 Trading / Wholesale', () => {
  it('P02 sales reconcile to SALE sources; residual incomplete → null', () => {
    const family = 'TRADING_DISTRIBUTION';
    const subtype = 'TRADING_WHOLESALE';
    const events = [
      event('s1', 'SALE', { amount: '5000.0000', paymentStatus: 'COLLECTED', paymentRelevance: 'RECEIVABLE' }),
      event('s2', 'SALE', {
        amount: '1000.0000',
        paymentStatus: 'PENDING',
        paymentRelevance: 'RECEIVABLE',
        ledgerStatus: 'SUPERSEDED',
      }),
    ];
    const projection = projectOperatingMoment({
      familyCode: family,
      prefs: prefs(family, subtype),
      role: 'OWNER_ADMIN',
      events,
    });
    const complete = projectWidgetsFromProjection({ projection, events, postingComplete: true });
    const p02 = complete.widgets.find((w) => w.widgetId === 'P02')!;
    assert.equal(p02.metrics.find((m) => m.key === 'sales')?.value, '5000.0000');
    assert.ok(p02.sourceEventIds.includes('s1'));
    assert.ok(!p02.sourceEventIds.includes('s2'));

    const incomplete = projectWidgetsFromProjection({
      projection,
      events,
      postingComplete: false,
      residualIncomplete: true,
    });
    const sales = incomplete.widgets.find((w) => w.widgetId === 'P02')!.metrics.find((m) => m.key === 'sales');
    assert.equal(sales?.value, null);
    assert.equal(sales?.coverage, 'UNAVAILABLE');

    assert.ok(complete.coverageMatrix.some((w) => w.widgetId === 'TW-ORDERS'));
    assert.ok(complete.coverageMatrix.some((w) => w.widgetId === 'TW-STOCK'));
  });

  it('TEAM_MEMBER redacts finance widgets', () => {
    const projection = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_DISTRIBUTION', 'TRADING_WHOLESALE'),
      role: 'TEAM_MEMBER',
      events: [event('s1', 'SALE', { amount: '1000.0000', paymentStatus: 'COLLECTED', paymentRelevance: 'RECEIVABLE' })],
    });
    const bundle = projectWidgetsFromProjection({
      projection,
      events: [event('s1', 'SALE', { amount: '1000.0000', paymentStatus: 'COLLECTED' })],
    });
    const p02 = bundle.widgets.find((w) => w.widgetId === 'P02')!;
    assert.equal(p02.coverage, 'UNAVAILABLE');
    assert.equal(p02.metrics[0]?.value, null);
  });
});

describe('P3.2 Distribution / Dealership', () => {
  it('exposes dealership-specific order/stock widgets', () => {
    const projection = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_DISTRIBUTION', 'DISTRIBUTION_DEALERSHIP'),
      role: 'FINANCE',
      events: [
        event('o1', 'ORDER', { quantity: '10', paymentStatus: 'NONE' }),
        event('sale', 'SALE', { amount: '2000.0000', paymentStatus: 'PENDING', paymentRelevance: 'RECEIVABLE' }),
      ],
    });
    const bundle = projectWidgetsFromProjection({
      projection,
      events: [
        event('o1', 'ORDER', { quantity: '10' }),
        event('sale', 'SALE', { amount: '2000.0000', paymentStatus: 'PENDING' }),
      ],
    });
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'DD-ORDERS'));
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'DD-STOCK'));
    assert.equal(
      bundle.widgets.find((w) => w.widgetId === 'P02')?.metrics.find((m) => m.key === 'sales')?.value,
      '2000.0000'
    );
  });
});

describe('P3.4 Retail Store / Restaurant / Consumer Service', () => {
  it('Retail Store has RT-SALES and RT-STOCK; order is not revenue', () => {
    const events = [
      event('ord', 'ORDER', { quantity: '5', amount: '500.0000' }),
      event('sale', 'SALE', { amount: '300.0000', paymentStatus: 'COLLECTED', paymentRelevance: 'RECEIVABLE' }),
    ];
    const projection = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_CONSUMER', 'RETAIL_STORE'),
      role: 'OWNER_ADMIN',
      events,
    });
    const bundle = projectWidgetsFromProjection({ projection, events });
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'RT-SALES'));
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'RT-STOCK'));
    assert.equal(
      bundle.widgets.find((w) => w.widgetId === 'P02')?.metrics.find((m) => m.key === 'sales')?.value,
      '300.0000'
    );
  });

  it('Restaurant Food shares retail sales/stock widgets', () => {
    const matrix = coverageMatrix('RETAIL_CONSUMER', 'RESTAURANT_FOOD');
    assert.ok(matrix.some((w) => w.widgetId === 'RT-SALES'));
  });

  it('Consumer Service exposes services + appointments, not retail stock', () => {
    const matrix = coverageMatrix('RETAIL_CONSUMER', 'CONSUMER_SERVICE');
    assert.ok(matrix.some((w) => w.widgetId === 'CS-SERVICES'));
    assert.ok(matrix.some((w) => w.widgetId === 'CS-APPOINTMENTS'));
    assert.ok(!matrix.some((w) => w.widgetId === 'RT-STOCK'));
  });
});

describe('P3.5 Standard + MTO Manufacturing', () => {
  it('Standard Production exposes MFG orders/production/stock widgets', () => {
    const events = [
      event('co', 'CUSTOMER_ORDER', { quantity: '20', amount: '0' }),
      event('prod', 'PRODUCTION', { quantity: '10', unit: 'pcs' }),
    ];
    const projection = projectOperatingMoment({
      familyCode: 'MANUFACTURING_PRODUCTION',
      prefs: prefs('MANUFACTURING_PRODUCTION', 'STANDARD_PRODUCTION'),
      role: 'OWNER_ADMIN',
      events,
    });
    const bundle = projectWidgetsFromProjection({ projection, events });
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'MFG-ORDERS'));
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'MFG-PRODUCTION'));
    assert.ok(bundle.widgets.some((w) => w.widgetId === 'MFG-STOCK'));
    // No SALE/INVOICE sources → UNAVAILABLE (not a fabricated zero). Customer order is not revenue.
    const sales = bundle.widgets.find((w) => w.widgetId === 'P02')?.metrics.find((m) => m.key === 'sales');
    assert.equal(sales?.value, null);
    assert.equal(sales?.coverage, 'UNAVAILABLE');
  });

  it('Made-to-Order shares manufacturing widget set', () => {
    const matrix = coverageMatrix('MANUFACTURING_PRODUCTION', 'MADE_TO_ORDER');
    assert.ok(matrix.some((w) => w.widgetId === 'MFG-PRODUCTION'));
  });
});
