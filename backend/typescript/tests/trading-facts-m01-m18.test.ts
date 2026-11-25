import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { computeTradingFacts } from '../src/modules/business/trading-facts';
import { composeTradingStoryPages } from '../src/modules/story/trading-story-v3';
import type { OperatingEventRow } from '../src/modules/business/operating-catalog';

function event(
  partial: Partial<OperatingEventRow> & Pick<OperatingEventRow, 'operatingEventId' | 'eventType' | 'title'>
): OperatingEventRow {
  return {
    note: null,
    amount: null,
    currencyCode: 'INR',
    counterpartyName: null,
    channelName: null,
    paymentRelevance: 'NONE',
    paymentStatus: 'OPEN',
    approvalStatus: 'APPROVED',
    submittedByUserId: 'u1',
    decidedByUserId: null,
    occurredAt: '2026-10-15T10:00:00.000Z',
    ...partial,
  };
}

describe('trading facts M01–M18', () => {
  it('T01-style: order is not revenue; sale posts net sales', () => {
    const events = [
      event({
        operatingEventId: 'o1',
        eventType: 'ORDER',
        title: 'SO-100',
        quantity: '100',
        amount: '10000',
        counterpartyName: 'Buyer A',
      }),
      event({
        operatingEventId: 's1',
        eventType: 'SALE',
        title: 'INV-40',
        amount: '4000',
        quantity: '40',
        parentEventId: 'o1',
        counterpartyName: 'Buyer A',
      }),
    ];
    const facts = computeTradingFacts({ events, currency: 'INR' });
    assert.equal(facts.metrics.M03.value, 4000);
    assert.equal(facts.metrics.M01.value, 4000);
    assert.ok(facts.metrics.M13.value == null || facts.metrics.M13.value >= 0);
  });

  it('T06-style: partial collection leaves receivables', () => {
    const events = [
      event({
        operatingEventId: 's1',
        eventType: 'SALE',
        title: 'INV-1000',
        amount: '1000',
        counterpartyName: 'Buyer A',
      }),
      event({
        operatingEventId: 'c1',
        eventType: 'COLLECTION',
        title: 'Receipt',
        amount: '400',
        parentEventId: 's1',
        counterpartyName: 'Buyer A',
      }),
    ];
    const facts = computeTradingFacts({ events });
    assert.equal(facts.metrics.M03.value, 1000);
    assert.equal(facts.metrics.M04.value, 400);
    assert.equal(facts.metrics.M05.value, 600);
  });

  it('missing sales yields null net sales (missing ≠ zero)', () => {
    const facts = computeTradingFacts({
      events: [
        event({
          operatingEventId: 'p1',
          eventType: 'PURCHASE',
          title: 'PO',
          amount: '500',
          counterpartyName: 'Supplier',
        }),
      ],
    });
    assert.equal(facts.metrics.M03.value, null);
    assert.equal(facts.metrics.M03.missing, true);
    assert.equal(facts.metrics.M06.value, 500);
  });

  it('story pages P1–P8 share M03 with facts', () => {
    const events = [
      event({
        operatingEventId: 's1',
        eventType: 'SALE',
        title: 'Sale',
        amount: '2500',
        counterpartyName: 'Buyer',
      }),
    ];
    const bundle = computeTradingFacts({ events, mode: 'trading' });
    const pages = composeTradingStoryPages({
      bundle,
      businessName: 'Northstar Trading',
      periodLabel: '2026-10',
      mode: 'trading',
      includeGst: false,
      events,
    });
    assert.equal(pages.length, 8);
    assert.equal(pages[0].pageId, 'P1');
    assert.equal(pages[0].hero.value, bundle.metrics.M03.value);
    assert.equal(pages[1].pageId, 'P2');
    // P2 cards are chronological event beats (label = short date, value = title).
    assert.ok(pages[1].cards.length >= 1);
    assert.equal(pages[1].cards[0].value, 'Sale');
    assert.equal(pages[3].pageId, 'P4');
    assert.equal(pages[7].pageId, 'P8');
  });

  it('T17: Trading vs Distribution same M03 totals; Dist labels differ', () => {
    const events = [
      event({
        operatingEventId: 's1',
        eventType: 'SALE',
        title: 'Dealer sale',
        amount: '2500',
        counterpartyName: 'Dealer A',
      }),
    ];
    const tradingFacts = computeTradingFacts({ events, mode: 'trading' });
    const distFacts = computeTradingFacts({ events, mode: 'distribution' });
    assert.equal(tradingFacts.metrics.M03.value, distFacts.metrics.M03.value);
    assert.equal(tradingFacts.metrics.M04.value, distFacts.metrics.M04.value);

    const tradingPages = composeTradingStoryPages({
      bundle: tradingFacts,
      businessName: 'Metro',
      periodLabel: '2026-10',
      mode: 'trading',
      includeGst: false,
      events,
    });
    const distPages = composeTradingStoryPages({
      bundle: distFacts,
      businessName: 'Metro',
      periodLabel: '2026-10',
      mode: 'distribution',
      includeGst: false,
      events,
    });
    assert.equal(tradingPages[0].hero.value, distPages[0].hero.value);
    assert.equal(tradingPages[3].hero.label, 'Collections');
    assert.equal(distPages[3].hero.label, 'Dealer collections');
    assert.match(distPages[1].narrative[0] ?? '', /Supplier procurement/);
    assert.match(tradingPages[1].narrative[0] ?? '', /Customer orders/);
  });
});

describe('tradingPulseMetrics uses M01–M18', () => {
  it('FINANCE sales is Not recorded when no sales exist', async () => {
    const { tradingPulseMetrics } = await import('../src/modules/business/operating-board-metrics');
    const groups = tradingPulseMetrics({
      events: [
        event({
          operatingEventId: 'o1',
          eventType: 'ORDER',
          title: 'Open order',
          amount: '5000',
          quantity: '50',
        }),
      ],
      visible: true,
      currency: 'INR',
      asOf: new Date('2026-10-20T00:00:00.000Z'),
      prefs: {
        subtype: 'TRADING_WHOLESALE',
        startDate: '2026-10-01',
        endDate: '2026-10-31',
        monthlyBudget: null,
        monthlyTarget: null,
        gstRegistration: null,
        gstState: null,
        gstTracking: 'NO',
      } as never,
      role: 'OWNER_ADMIN' as never,
      projection: {
        budget: null,
        target: null,
        finance: { sales: '0', received: '0', outstanding: '0', payable: '0', eligibleCost: '0', cashPaid: '0' },
        attention: [],
        paymentReadiness: [],
        activity: [],
      },
    });
    const finance = groups.find((g) => g.code === 'FINANCE');
    const sales = finance?.metrics.find((m) => m.key === 'sales');
    assert.equal(sales?.value, 'Not recorded');
  });
});
