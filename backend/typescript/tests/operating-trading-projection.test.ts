/**
 * Trading / Distribution projection contract: structured board metrics, the 18
 * view-all sections, HOLD and TRANSFER semantics, and the story profile wiring.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import {
  tradingStockPositions,
  validateTradingWrite,
  type TradingEvent,
  type TradingWrite,
} from '../src/modules/business/operating-trading-contract';
import {
  DISTRIBUTION_STORY_CHAPTERS,
  TRADING_STORY_CHAPTERS,
  getStoryComposer,
  resolveStoryFamilyProfile,
} from '../src/modules/story/profiles';

function prefs(subtype: string) {
  return normalizeOperatingPreferences(
    'TRADING_DISTRIBUTION',
    {
      subtype,
      identityName: subtype,
      partyName: subtype,
      monthlyBudget: '500000',
      monthlyTarget: '1000000',
      targetMetric: 'REVENUE',
      channels: ['Wholesale'],
      startDate: '2026-10-01',
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
    channelName: 'Wholesale',
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: '2026-10-10T10:00:00.000Z',
    ...fields,
  };
}

function asTrading(events: OperatingEventRow[]): TradingEvent[] {
  return events.map((row) => ({
    operatingEventId: row.operatingEventId,
    eventType: row.eventType,
    title: row.title,
    amount: row.amount,
    paymentStatus: row.paymentStatus,
    approvalStatus: row.approvalStatus,
    occurredAt: row.occurredAt,
    parentEventId: row.parentEventId ?? null,
    quantity: row.quantity ?? null,
    unit: row.unit ?? null,
    counterpartyName: row.counterpartyName ?? null,
    dueAt: row.dueAt ?? null,
    lines: row.lines ?? [],
    contract: row.contract ?? null,
    note: row.note,
    master: row.master ?? null,
  }));
}

function write(partial: Partial<TradingWrite> & Pick<TradingWrite, 'eventType' | 'title'>): TradingWrite {
  return { amount: null, ...partial };
}

const AS_OF = new Date('2026-10-18T00:00:00.000Z');

/** The board sections both verticals render, in Figma order. */
const BOARD_SECTIONS = [
  'M1',
  'M2',
  'M3',
  'M4',
  'orders',
  'stock',
  'network',
  'L1',
  'L2',
  'L3',
  'L4',
  'L5',
  'transit',
  'onhand',
  'ME1',
  'ME2',
  'ME3',
  'ME4',
  'chains',
  'fulfilment',
];

/** The six Pulse surfaces, which carry their own tile groups. */
const PULSE_SECTIONS = ['TARGET_BUDGET', 'FINANCE', 'PAYMENT_READINESS', 'ATTENTION', 'ACTIVITY', 'GST'];

function busyEvents(): OperatingEventRow[] {
  return [
    event('buy', 'PURCHASE', {
      amount: '40000.0000',
      counterpartyName: 'Mill Works',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      dueAt: '2026-10-20',
      lines: [{ itemName: 'Cup', quantity: '100', unit: 'pc', unitPrice: '400', baseQuantity: '100.0000' }],
      contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
    }),
    event('sale', 'SALE', {
      amount: '60000.0000',
      counterpartyName: 'South Dealer',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'PENDING',
      dueAt: '2026-10-01',
      contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
    }),
    event('collect', 'COLLECTION', {
      amount: '25000.0000',
      counterpartyName: 'South Dealer',
      parentEventId: 'sale',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'COLLECTED',
    }),
    event('order', 'ORDER', {
      title: 'Dealer order',
      quantity: '50',
      counterpartyName: 'South Dealer',
      dueAt: '2026-10-12',
      contract: { orderStatus: 'PLACED', orderSide: 'SALES' },
    }),
    event('ship', 'SHIPMENT', {
      quantity: '20',
      parentEventId: 'order',
      contract: { shipmentStatus: 'IN_TRANSIT' },
    }),
    event('open', 'STOCK', {
      title: 'Cup',
      quantity: '100',
      unit: 'pc',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '100', ownership: 'OWNED' },
    }),
    event('ret', 'RETURN', {
      amount: '2000.0000',
      parentEventId: 'sale',
      contract: { returnDirection: 'CUSTOMER', goodsOutcome: 'NOT_RETURNED', returnReason: 'Breakage' },
    }),
  ];
}

describe('structured board metrics for both verticals', () => {
  for (const subtype of ['TRADING_WHOLESALE', 'DISTRIBUTION_DEALERSHIP']) {
    it(`${subtype}: every view-all surface carries tiles and a status badge`, () => {
      const view = projectOperatingMoment({
        familyCode: 'TRADING_DISTRIBUTION',
        prefs: prefs(subtype),
        role: 'OWNER_ADMIN',
        events: busyEvents(),
        asOf: AS_OF,
      });
      const byCode = new Map(
        [...view.boards.moments, ...view.boards.life, ...view.boards.memory].map((section) => [section.code, section])
      );
      for (const code of BOARD_SECTIONS) {
        const section = byCode.get(code);
        assert.ok(section, `${subtype} is missing the ${code} board`);
        assert.ok(
          Array.isArray(section.metrics) && section.metrics.length >= 2,
          `${subtype} ${code} should expose at least two tiles`
        );
        for (const tile of section.metrics ?? []) {
          assert.ok(tile.key && tile.label, `${subtype} ${code} tile needs a key and label`);
          assert.ok(['NORMAL', 'WARNING', 'POSITIVE'].includes(tile.tone), `bad tone on ${code}/${tile.key}`);
          assert.equal(typeof tile.value, 'string');
        }
        assert.ok(section.why === null || typeof section.why === 'string', `${code} why must be string|null`);
      }
    });

    it(`${subtype}: pulse metrics cover the six Pulse surfaces`, () => {
      const view = projectOperatingMoment({
        familyCode: 'TRADING_DISTRIBUTION',
        prefs: prefs(subtype),
        role: 'OWNER_ADMIN',
        events: busyEvents(),
        asOf: AS_OF,
      });
      const codes = (view.pulseMetrics ?? []).map((group) => group.code);
      assert.deepEqual(codes, ['TARGET_BUDGET', 'FINANCE', 'PAYMENT_READINESS', 'ATTENTION', 'ACTIVITY', 'GST']);
      for (const group of view.pulseMetrics ?? []) {
        assert.ok(group.metrics.length >= 3, `${group.code} needs at least three tiles`);
      }
    });
  }

  it('reports a missing measurement as Not recorded rather than zero', () => {
    const unconfigured = { ...prefs('TRADING_WHOLESALE'), monthlyBudget: '', monthlyTarget: '' };
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: unconfigured,
      role: 'OWNER_ADMIN',
      events: [],
      asOf: AS_OF,
    });
    const stock = view.boards.moments.find((section) => section.code === 'stock');
    assert.equal(stock?.metrics?.find((tile) => tile.key === 'onHand')?.value, 'Not recorded');
    const target = (view.pulseMetrics ?? []).find((group) => group.code === 'TARGET_BUDGET');
    assert.equal(target?.metrics.find((tile) => tile.key === 'target')?.value, 'Not recorded');
    assert.equal(target?.metrics.find((tile) => tile.key === 'progress')?.value, 'Not recorded');
    assert.match(String(target?.why ?? ''), /target and budget/i);
  });

  it('badges rows with the Figma status chips', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE'),
      role: 'OWNER_ADMIN',
      events: [
        ...busyEvents(),
        event('held', 'ORDER', {
          title: 'Held order',
          contract: { orderStatus: 'PLACED', orderSide: 'SALES', holdStatus: 'HELD', holdReason: 'Credit review' },
        }),
      ],
      asOf: AS_OF,
    });
    const rows = [...view.boards.moments, ...view.boards.life, ...view.boards.memory].flatMap((s) => s.rows);
    const badges = new Set(rows.map((row) => row.badge).filter(Boolean));
    assert.ok(badges.has('Open'), `expected an Open badge, saw ${[...badges].join(', ')}`);
    assert.ok(badges.has('Transit'), `expected a Transit badge, saw ${[...badges].join(', ')}`);
    assert.ok(badges.has('Overdue') || badges.has('Part'), `expected Overdue/Part, saw ${[...badges].join(', ')}`);
    assert.ok(badges.has('On hold'), `expected an On hold badge, saw ${[...badges].join(', ')}`);
  });

  it('hides every money tile from a team member but keeps counts', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE'),
      role: 'TEAM_MEMBER',
      events: busyEvents(),
      asOf: AS_OF,
    });
    assert.equal(view.finance, null);
    const finance = (view.pulseMetrics ?? []).find((group) => group.code === 'FINANCE');
    assert.ok(finance?.metrics.every((tile) => tile.value === 'Hidden'));
    const orders = view.boards.moments.find((section) => section.code === 'orders');
    assert.equal(
      orders?.metrics?.some((tile) => tile.key === 'netSales' && tile.value !== 'Hidden'),
      false
    );
  });

  it('computes delivered % and average pay lag from recorded events', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('DISTRIBUTION_DEALERSHIP'),
      role: 'OWNER_ADMIN',
      events: [
        ...busyEvents(),
        event('ship-done', 'SHIPMENT', {
          quantity: '30',
          parentEventId: 'order',
          occurredAt: '2026-10-15T10:00:00.000Z',
          contract: { shipmentStatus: 'DELIVERED' },
        }),
        event('pay', 'PAYMENT', {
          amount: '40000.0000',
          counterpartyName: 'Mill Works',
          parentEventId: 'buy',
          paymentRelevance: 'PAYABLE',
          paymentStatus: 'PAID',
          occurredAt: '2026-10-16T10:00:00.000Z',
        }),
      ],
      asOf: AS_OF,
    });
    const fulfilment = view.boards.memory.find((section) => section.code === 'fulfilment');
    assert.equal(fulfilment?.metrics?.find((tile) => tile.key === 'delivered')?.value, '50%');
    const lag = fulfilment?.metrics?.find((tile) => tile.key === 'payLag');
    assert.equal(lag?.unit, 'days');
    assert.ok(Number(lag?.value) > 0);
  });
});

describe('HOLD semantics', () => {
  it('a held order must state why and cannot be completed', () => {
    const noReason = validateTradingWrite(
      write({
        eventType: 'ORDER',
        title: 'Order',
        quantity: '10',
        channelName: 'Wholesale',
        contract: { orderStatus: 'PLACED', holdStatus: 'HELD' },
      }),
      []
    );
    assert.equal(noReason.ok, false);

    const completed = validateTradingWrite(
      write({
        eventType: 'ORDER',
        title: 'Order',
        quantity: '10',
        channelName: 'Wholesale',
        contract: { orderStatus: 'COMPLETED', holdStatus: 'HELD', holdReason: 'Credit review' },
      }),
      []
    );
    assert.equal(completed.ok, false);

    const held = validateTradingWrite(
      write({
        eventType: 'ORDER',
        title: 'Order',
        quantity: '10',
        channelName: 'Wholesale',
        contract: { orderStatus: 'PLACED', holdStatus: 'HELD', holdReason: 'Credit review' },
      }),
      []
    );
    assert.equal(held.ok, true);
    if (!held.ok) return;
    assert.equal(held.contract.holdStatus, 'HELD');
    assert.equal(held.contract.holdReason, 'Credit review');
  });

  it('a held dispatch cannot be delivered', () => {
    const parent = event('order', 'ORDER', {
      quantity: '10',
      contract: { orderStatus: 'PLACED', orderSide: 'SALES' },
    });
    const blocked = validateTradingWrite(
      write({
        eventType: 'SHIPMENT',
        title: 'Dispatch',
        quantity: '10',
        parentEventId: 'order',
        contract: { shipmentStatus: 'DELIVERED', holdStatus: 'HELD', holdReason: 'Payment pending' },
      }),
      asTrading([parent])
    );
    assert.equal(blocked.ok, false);

    const released = validateTradingWrite(
      write({
        eventType: 'SHIPMENT',
        title: 'Dispatch',
        quantity: '10',
        parentEventId: 'order',
        contract: { shipmentStatus: 'DELIVERED', holdStatus: 'RELEASED', holdReason: 'Payment cleared' },
      }),
      asTrading([parent])
    );
    assert.equal(released.ok, true);
  });
});

describe('TRANSFER semantics', () => {
  it('moves stock between two locations without changing global on-hand', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Cup',
        quantity: '25',
        contract: {
          stockAction: 'TRANSFER',
          transferFromMasterId: 'wh-a',
          transferToMasterId: 'wh-b',
          transferFromName: 'Main DC',
          transferToName: 'Depot 2',
        },
      }),
      []
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    assert.equal(checked.contract.stockAction, 'TRANSFER');
    assert.equal(checked.contract.stockReason, 'TRANSFER');

    const positions = tradingStockPositions([
      {
        operatingEventId: 'open-a',
        eventType: 'STOCK',
        title: 'Cup',
        amount: null,
        paymentStatus: 'NONE',
        approvalStatus: 'NONE',
        occurredAt: '2026-10-01T00:00:00.000Z',
        quantity: '100',
        unit: 'pc',
        contract: {
          stockAction: 'SET',
          stockReason: 'opening',
          declaredQuantity: '100',
          ownership: 'OWNED',
          locationName: 'Main DC',
        },
      },
      {
        operatingEventId: 'move',
        eventType: 'STOCK',
        title: 'Cup',
        amount: null,
        paymentStatus: 'NONE',
        approvalStatus: 'NONE',
        occurredAt: '2026-10-05T00:00:00.000Z',
        quantity: '25',
        unit: 'pc',
        contract: {
          stockAction: 'TRANSFER',
          stockReason: 'TRANSFER',
          transferFromName: 'Main DC',
          transferToName: 'Depot 2',
        },
      },
    ]);
    const source = positions.find((row) => row.item.includes('Main DC'));
    const destination = positions.find((row) => row.item.includes('Depot 2'));
    assert.equal(source?.onHand, 75);
    assert.equal(destination?.onHand, 25);
    const globalTotal = positions.reduce((total, row) => total + row.onHand, 0);
    assert.equal(globalTotal, 100);
  });

  it('rejects a transfer without two distinct locations', () => {
    const missingTo = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Cup',
        quantity: '5',
        contract: { stockAction: 'TRANSFER', transferFromName: 'Main DC' },
      }),
      []
    );
    assert.equal(missingTo.ok, false);

    const sameLocation = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Cup',
        quantity: '5',
        contract: {
          stockAction: 'TRANSFER',
          transferFromMasterId: 'wh-a',
          transferToMasterId: 'wh-a',
        },
      }),
      []
    );
    assert.equal(sameLocation.ok, false);

    const noQuantity = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Cup',
        quantity: '0',
        contract: { stockAction: 'TRANSFER', transferFromName: 'A', transferToName: 'B' },
      }),
      []
    );
    assert.equal(noQuantity.ok, false);
  });
});

describe('story profile wiring', () => {
  it('resolves TRADING_DISTRIBUTION from the family and from the moment type', () => {
    assert.equal(resolveStoryFamilyProfile({ businessFamily: 'TRADING_DISTRIBUTION' }), 'TRADING_DISTRIBUTION');
    assert.equal(resolveStoryFamilyProfile({ momentTypeCode: 'TRADING_WHOLESALE' }), 'TRADING_DISTRIBUTION');
    assert.equal(resolveStoryFamilyProfile({ momentTypeCode: 'DISTRIBUTION_DEALERSHIP' }), 'TRADING_DISTRIBUTION');
  });

  it('registers a composer and the eight chapter ids for both verticals', () => {
    const composer = getStoryComposer('TRADING_DISTRIBUTION');
    assert.equal(composer.profile, 'TRADING_DISTRIBUTION');
    assert.ok(composer.metricKeys.length > 0);
    assert.equal(TRADING_STORY_CHAPTERS.length, 8);
    assert.equal(DISTRIBUTION_STORY_CHAPTERS.length, 8);
    assert.deepEqual(TRADING_STORY_CHAPTERS, [
      'trd-moment',
      'trd-motion',
      'trd-orders',
      'trd-money',
      'trd-stock',
      'trd-network',
      'trd-attention',
      'trd-sofar',
    ]);
    assert.deepEqual(
      DISTRIBUTION_STORY_CHAPTERS,
      [
        'dst-moment',
        'dst-motion',
        'dst-orders',
        'dst-money',
        'dst-stock',
        'dst-network',
        'dst-attention',
        'dst-sofar',
      ]
    );
  });
});
