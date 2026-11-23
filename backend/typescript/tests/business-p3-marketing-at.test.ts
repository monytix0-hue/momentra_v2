/**
 * Behaviour: Marketing v5.1 AT-01..AT-25 (Quick Adds, attribution, widgets, coverage).
 * Modules under test: operating-catalog, operating-marketing, operating-widget-*.
 * Existing: operating-moments.test.ts (marketing v3/v4.2/v5 — not numbered AT suite).
 * User: "Automate AT-01 through AT-25."
 * User: "Client Campaign: 10 Quick Adds. Own Business Campaign: 8 Quick Adds."
 * User: "Enforce observational attributed sales versus accounting revenue."
 */
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  quickAddsForSubtype,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import {
  attributedSalesTotal,
  marketingProgress,
  seriesValue,
  validateMarketingWrite,
} from '../src/modules/business/operating-marketing';
import { budgetUsePercent, assertNoDoubleRevenue } from '../src/modules/business/finance-bridge';
import { MARKETING_WIDGET_IDS, coverageMatrix } from '../src/modules/business/operating-widget-catalog';
import { projectWidgetsFromProjection } from '../src/modules/business/operating-widget-service';

const blank = {
  reportingMode: null as 'INCREMENTAL' | 'CUMULATIVE' | null,
  metricCode: null as string | null,
  budgetCategory: null,
  paymentState: null,
  activityStatus: null,
  attributedSales: null as string | null,
  revisionKind: null as 'BUDGET' | 'TARGET' | 'CATEGORY' | null,
  effectiveFrom: null as string | null,
};

function prefs(subtype: 'CLIENT_CAMPAIGN' | 'OWN_BUSINESS_CAMPAIGN', extra: Record<string, unknown> = {}) {
  return normalizeOperatingPreferences(
    'MARKETING_CAMPAIGNS',
    {
      subtype,
      identityName: subtype,
      partyName: 'Northstar',
      monthlyBudget: '500000',
      monthlyTarget: '300',
      targetMetric: 'LEADS',
      channels: ['Search Ads'],
      startDate: '2026-11-01',
      endDate: '2026-11-30',
      ...extra,
    },
    { userId: 'u', displayName: 'Asha' }
  );
}

function posted(id: string, eventType: string, fields: Partial<OperatingEventRow> = {}): OperatingEventRow {
  return {
    operatingEventId: id,
    eventType,
    title: fields.title ?? eventType,
    note: null,
    amount: null,
    currencyCode: 'INR',
    counterpartyName: null,
    channelName: 'Search Ads',
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: '2026-11-10T10:00:00.000Z',
    ...fields,
  };
}

describe('Marketing v5.1 AT-01–AT-25', () => {
  it('AT-01 Client has 10 Quick Adds; Own has 8', () => {
    assert.equal(quickAddsForSubtype('CLIENT_CAMPAIGN').length, 10);
    assert.equal(quickAddsForSubtype('OWN_BUSINESS_CAMPAIGN').length, 8);
  });

  it('AT-02 Own rejects INVOICE and COLLECTION Quick Adds', () => {
    const own = new Set(quickAddsForSubtype('OWN_BUSINESS_CAMPAIGN').map((q) => q.code));
    assert.equal(own.has('INVOICE'), false);
    assert.equal(own.has('COLLECTION'), false);
  });

  it('AT-03 Own attributed sales are observational — not accounting revenue', () => {
    const events = [
      posted('r1', 'RESULT', {
        quantity: '10',
        marketing: { ...blank, reportingMode: 'INCREMENTAL', metricCode: 'ORDERS', attributedSales: '50000' },
      }),
      posted('spend', 'SPEND', { amount: '10000.0000', paymentStatus: 'PAID', paymentRelevance: 'PAYABLE' }),
    ];
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('OWN_BUSINESS_CAMPAIGN', { monthlyTarget: '180', targetMetric: 'ORDERS' }),
      role: 'OWNER_ADMIN',
      events,
    });
    assert.equal(view.finance?.sales, '0.0000');
    assert.equal(attributedSalesTotal(events), 50000);
    const bundle = projectWidgetsFromProjection({ projection: view, events });
    const attr = bundle.widgets.find((w) => w.widgetId === 'MKT-ATTRIBUTION');
    assert.ok(attr);
    assert.equal(attr!.metrics[0]?.value, '50000.0000');
  });

  it('AT-04 budget use 350000/550000 = 63.64%', () => {
    assert.equal(budgetUsePercent(350_000, 550_000), 63.64);
  });

  it('AT-05 collection does not increase revenue', () => {
    const r = assertNoDoubleRevenue({ postedRevenue: 650_000, collectionAmount: 400_000 });
    assert.equal(r.revenue, 650_000);
    assert.equal(r.cash, 400_000);
  });

  it('AT-06 incremental RESULT sums without double count', () => {
    const events = [
      posted('a', 'RESULT', {
        quantity: '170',
        unit: 'leads',
        marketing: { ...blank, reportingMode: 'INCREMENTAL', metricCode: 'LEADS' },
      }),
      posted('b', 'RESULT', {
        quantity: '30',
        unit: 'leads',
        occurredAt: '2026-11-12T10:00:00.000Z',
        marketing: { ...blank, reportingMode: 'INCREMENTAL', metricCode: 'LEADS' },
      }),
    ];
    const measured = marketingProgress(events, 'LEADS');
    assert.equal(measured.progress, 200);
    assert.notEqual(measured.coverage, 'N/A');
  });

  it('AT-07 cumulative RESULT contributes only the increase', () => {
    const events = [
      posted('a', 'RESULT', {
        quantity: '100',
        unit: 'leads',
        marketing: { ...blank, reportingMode: 'CUMULATIVE', metricCode: 'LEADS' },
      }),
      posted('b', 'RESULT', {
        quantity: '150',
        unit: 'leads',
        occurredAt: '2026-11-12T10:00:00.000Z',
        marketing: { ...blank, reportingMode: 'CUMULATIVE', metricCode: 'LEADS' },
      }),
    ];
    assert.equal(seriesValue(events), 150);
    assert.equal(marketingProgress(events, 'LEADS').progress, 150);
  });

  it('AT-08 mixed units → N/A coverage not fabricated zero as COMPLETE', () => {
    const events = [
      posted('a', 'RESULT', {
        quantity: '10',
        unit: 'leads',
        marketing: { ...blank, reportingMode: 'INCREMENTAL', metricCode: 'LEADS' },
      }),
      posted('b', 'RESULT', {
        quantity: '5',
        unit: 'orders',
        occurredAt: '2026-11-12T10:00:00.000Z',
        marketing: { ...blank, reportingMode: 'INCREMENTAL', metricCode: 'LEADS' },
      }),
    ];
    assert.equal(marketingProgress(events, 'LEADS').coverage, 'N/A');
  });

  it('AT-09 effective-dated budget revision applies', () => {
    const asOf = new Date('2026-11-24T00:00:00.000Z');
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'OWNER_ADMIN',
      asOf,
      events: [
        posted('rev', 'REVISION', {
          amount: '550000',
          occurredAt: '2026-11-23T00:00:00.000Z',
          marketing: { ...blank, revisionKind: 'BUDGET', effectiveFrom: '2026-11-23' },
        }),
        posted('spend', 'SPEND', {
          amount: '350000',
          paymentRelevance: 'PAYABLE',
          paymentStatus: 'PENDING',
          occurredAt: '2026-11-20T00:00:00.000Z',
        }),
      ],
    });
    assert.equal(view.budget?.amount, '550000.0000');
    assert.equal(view.budget?.spent, '350000.0000');
  });

  it('AT-10 effective-dated target revision applies', () => {
    const asOf = new Date('2026-11-24T00:00:00.000Z');
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'OWNER_ADMIN',
      asOf,
      events: [
        posted('rev', 'REVISION', {
          amount: '400',
          occurredAt: '2026-11-23T00:00:00.000Z',
          marketing: { ...blank, revisionKind: 'TARGET', effectiveFrom: '2026-11-23' },
        }),
      ],
    });
    assert.equal(view.target?.value, '400.0000');
  });

  it('AT-11 TEAM_MEMBER finance redaction on widgets', () => {
    const events = [
      posted('inv', 'INVOICE', { amount: '100000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
    ];
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'TEAM_MEMBER',
      events,
    });
    assert.equal(view.finance, null);
    const bundle = projectWidgetsFromProjection({ projection: view, events });
    assert.equal(bundle.widgets.find((w) => w.widgetId === 'P02')?.coverage, 'UNAVAILABLE');
  });

  it('AT-12 Own write rejects invoice', () => {
    const checked = validateMarketingWrite(
      {
        subtype: 'OWN_BUSINESS_CAMPAIGN',
        eventType: 'INVOICE',
        amount: '100',
        quantity: null,
        unit: null,
        parentEventId: null,
        metricCode: null,
        reportingMode: null,
        revisionKind: null,
        activityStatus: null,
        attributedSales: null,
        counterpartyName: 'Client',
        channelName: 'Search Ads',
      },
      []
    );
    assert.equal(checked.ok, false);
  });

  it('AT-13 overdue payable surfaces ATT-04', () => {
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN', { monthlyTarget: '' }),
      role: 'OWNER_ADMIN',
      asOf: new Date('2026-11-24T00:00:00.000Z'),
      events: [
        posted('spend', 'SPEND', {
          amount: '1000',
          paymentRelevance: 'PAYABLE',
          paymentStatus: 'PENDING',
          dueAt: '2026-11-01',
          counterpartyName: 'Press',
        }),
      ],
    });
    assert.equal(view.attention.some((item) => item.kind === 'ATT-04'), true);
  });

  it('AT-14 missing budget denominator is unavailable', () => {
    assert.equal(budgetUsePercent(100, 0), null);
  });

  it('AT-15…AT-19 Pulse P01–P05 present for Client and Own', () => {
    for (const subtype of ['CLIENT_CAMPAIGN', 'OWN_BUSINESS_CAMPAIGN'] as const) {
      const ids = new Set(coverageMatrix('MARKETING_CAMPAIGNS', subtype).map((w) => w.widgetId));
      for (const id of ['P01', 'P02', 'P03', 'P04', 'P05']) assert.ok(ids.has(id), `${subtype} ${id}`);
    }
  });

  it('AT-20 Moments M01–M04 present', () => {
    const ids = new Set(coverageMatrix('MARKETING_CAMPAIGNS', 'CLIENT_CAMPAIGN').map((w) => w.widgetId));
    for (const id of ['M01', 'M02', 'M03', 'M04']) assert.ok(ids.has(id));
  });

  it('AT-21 Life L01–L04 present', () => {
    const ids = new Set(coverageMatrix('MARKETING_CAMPAIGNS', 'CLIENT_CAMPAIGN').map((w) => w.widgetId));
    for (const id of ['L01', 'L02', 'L03', 'L04']) assert.ok(ids.has(id));
  });

  it('AT-22 Memory ME01–ME04 present', () => {
    const ids = new Set(coverageMatrix('MARKETING_CAMPAIGNS', 'CLIENT_CAMPAIGN').map((w) => w.widgetId));
    for (const id of ['ME01', 'ME02', 'ME03', 'ME04']) assert.ok(ids.has(id));
  });

  it('AT-23 seventeen-widget Marketing contract', () => {
    assert.equal(MARKETING_WIDGET_IDS.length, 17);
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'OWNER_ADMIN',
      events: [],
    });
    const bundle = projectWidgetsFromProjection({ projection: view, events: [] });
    assert.equal(bundle.marketingContract?.count, 17);
    for (const id of MARKETING_WIDGET_IDS) {
      assert.ok(bundle.widgets.some((w) => w.widgetId === id), `missing ${id}`);
    }
  });

  it('AT-24 SUPERSEDED invoice excluded from P02 sales', () => {
    const events = [
      posted('inv1', 'INVOICE', { amount: '400000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
      posted('inv2', 'INVOICE', {
        amount: '100000',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
        ledgerStatus: 'SUPERSEDED',
      }),
    ];
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'OWNER_ADMIN',
      events,
    });
    assert.equal(view.finance?.sales, '400000.0000');
    const bundle = projectWidgetsFromProjection({ projection: view, events });
    const p02 = bundle.widgets.find((w) => w.widgetId === 'P02')!;
    assert.ok(p02.sourceEventIds.includes('inv1'));
    assert.ok(!p02.sourceEventIds.includes('inv2'));
  });

  it('clientCampaignPostActivatePulseSeed', () => {
    const asOf = new Date('2026-11-15T00:00:00.000Z');
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN', { identityName: 'Brightline', reportingCadence: 'WEEKLY' }),
      role: 'OWNER_ADMIN',
      events: [],
      asOf,
    });
    assert.equal(view.budget?.configured, true);
    assert.equal(view.target?.configured, true);
    assert.equal(view.title, 'Brightline');
    assert.equal(view.partyName, 'Northstar');
    assert.deepEqual(view.life?.channels, ['Search Ads']);
    const targetBudget = (view.pulseMetrics ?? []).find((group) => group.code === 'TARGET_BUDGET');
    assert.ok(targetBudget);
    assert.match(String(targetBudget?.metrics.find((tile) => tile.key === 'budget')?.value ?? ''), /5,?00,?000|500000/);
    assert.match(String(targetBudget?.metrics.find((tile) => tile.key === 'target')?.value ?? ''), /^300/);
    const progress = String(targetBudget?.metrics.find((tile) => tile.key === 'progress')?.value ?? '');
    assert.match(progress, /^0(\s*(LEADS|INR))?$/);
    const activity = (view.pulseMetrics ?? []).find((group) => group.code === 'ACTIVITY');
    assert.equal(activity?.metrics.find((tile) => tile.key === 'records')?.value, '0');
    const codes = (view.quickAdds ?? []).map((row) => row.code);
    assert.equal(codes.length, 10);
    for (const required of ['INVOICE', 'COLLECTION', 'RESULT', 'SPEND']) {
      assert.ok(codes.includes(required), `missing quick add ${required}`);
    }
    const bundle = projectWidgetsFromProjection({ projection: view, events: [] });
    assert.equal(bundle.marketingContract?.count, 17);
    for (const id of MARKETING_WIDGET_IDS) {
      assert.ok(bundle.widgets.some((w) => w.widgetId === id), `missing widget ${id}`);
    }
    const results = bundle.widgets.find((w) => w.widgetId === 'MKT-RESULTS')!;
    assert.notEqual(results.coverage, 'UNAVAILABLE');
    assert.equal(results.metrics[0]?.coverage, 'ZERO');
    assert.equal(results.metrics[0]?.value, 0);
  });

  it('AT-25 incomplete historical coverage → UNAVAILABLE not zero', () => {
    const events = [
      posted('inv', 'INVOICE', { amount: '100000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
    ];
    const view = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs: prefs('CLIENT_CAMPAIGN'),
      role: 'OWNER_ADMIN',
      events,
    });
    const bundle = projectWidgetsFromProjection({
      projection: view,
      events,
      postingComplete: false,
      residualIncomplete: true,
    });
    const sales = bundle.widgets.find((w) => w.widgetId === 'P02')!.metrics.find((m) => m.key === 'sales');
    assert.equal(sales?.value, null);
    assert.equal(sales?.coverage, 'UNAVAILABLE');
  });
});
