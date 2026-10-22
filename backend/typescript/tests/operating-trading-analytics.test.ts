import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import {
  obligationRemaining,
  tradingStockPositions,
  validateTradingWrite,
  type TradingEvent,
  type TradingWrite,
} from '../src/modules/business/operating-trading-contract';

function prefs(subtype: string, budget = '', target = '') {
  return normalizeOperatingPreferences(
    'TRADING_DISTRIBUTION',
    {
      subtype,
      identityName: subtype,
      partyName: subtype,
      monthlyBudget: budget,
      monthlyTarget: target,
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
  return {
    amount: null,
    ...partial,
  };
}

describe('trading / distribution functional analytics NA-T / M0 / M2', () => {
  it('ALL-style: no target configured does not invent achievement', () => {
    const base = prefs('TRADING_WHOLESALE', '10000', '50000');
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: { ...base, monthlyBudget: '', monthlyTarget: '' },
      role: 'FINANCE',
      events: [event('sale', 'SALE', { amount: '5000.0000', paymentStatus: 'COLLECTED', paymentRelevance: 'RECEIVABLE' })],
    });
    assert.equal(view.target?.configured, false);
    assert.equal(view.budget?.configured, false);
    assert.equal(view.finance?.sales, '5000.0000');
  });

  it('staff role hides finance', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '1000', '5000'),
      role: 'TEAM_MEMBER',
      events: [
        event('sale', 'SALE', {
          amount: '900',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          dueAt: '2026-10-01',
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    assert.equal(view.finance, null);
    assert.equal(view.activity[0]?.amount, null);
  });

  it('NA-T01: overdue receivable is high attention with collect CTA', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: '12000.0000',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          dueAt: '2026-10-01',
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    const overdue = view.attention.find((item) => item.kind === 'OVERDUE_RECEIVABLE');
    assert.ok(overdue);
    assert.equal(overdue?.severity, 'HIGH');
    assert.equal(overdue?.cta, 'collect');
    assert.equal(overdue?.amount, '12000.0000');
    assert.equal(view.attention.some((item) => String(item.id).startsWith('open-') && item.kind === 'RECEIVABLE'), false);
  });

  it('NA-T02: due-soon payable is medium attention with pay CTA', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('DISTRIBUTION_DEALERSHIP', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('buy', 'PURCHASE', {
          amount: '8000.0000',
          paymentStatus: 'PENDING',
          paymentRelevance: 'PAYABLE',
          counterpartyName: 'Supplier B',
          dueAt: '2026-10-19',
          contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    const due = view.attention.find((item) => item.kind === 'DUE_PAYABLE');
    assert.ok(due);
    assert.equal(due?.severity, 'MEDIUM');
    assert.equal(due?.cta, 'pay');
  });

  it('NA-T03: overdue payable ranks above due-soon receivable', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('buy', 'PURCHASE', {
          amount: '5000.0000',
          paymentStatus: 'PENDING',
          paymentRelevance: 'PAYABLE',
          counterpartyName: 'Supplier B',
          dueAt: '2026-10-01',
          contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
        }),
        event('sale', 'SALE', {
          amount: '3000.0000',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          dueAt: '2026-10-19',
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    assert.equal(view.attention[0]?.kind, 'OVERDUE_PAYABLE');
    assert.ok(view.attention.some((item) => item.kind === 'DUE_RECEIVABLE'));
  });

  it('NA-T09: undated open balance is not overdue attention', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '1000', '5000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: '900',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          dueAt: null,
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    assert.equal(view.attention.some((item) => item.kind === 'OVERDUE_RECEIVABLE'), false);
    const open = view.boards.moments.find((section) => section.code === 'M4');
    assert.ok((open?.rows.length ?? 0) > 0);
  });

  it('NA-T04: overdue unfulfilled order qty raises shipment attention', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('DISTRIBUTION_DEALERSHIP', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('order', 'ORDER', {
          title: 'Dealer order',
          quantity: '100',
          counterpartyName: 'Dealer C',
          dueAt: '2026-10-01',
          contract: { orderStatus: 'PLACED', promisedAt: '2026-10-01' },
        }),
        event('ship', 'SHIPMENT', {
          quantity: '40',
          parentEventId: 'order',
          contract: { shipmentStatus: 'IN_TRANSIT' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    const card = view.attention.find((item) => item.kind === 'OVERDUE_SHIPMENT');
    assert.ok(card);
    assert.match(String(card?.title ?? ''), /60/);
    assert.equal(card?.cta, 'shipment');
  });

  it('M0: quick sale posts sales without stock move', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Cash sale',
        amount: '2400',
        channelName: 'Wholesale',
        channelTracking: true,
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
      }),
      []
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '30000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: checked.amount,
          paymentStatus: checked.paymentStatus,
          paymentRelevance: 'RECEIVABLE',
          contract: checked.contract,
        }),
      ],
    });
    assert.equal(view.finance?.sales, '2400.0000');
    assert.equal(view.finance?.received, '2400.0000');
    assert.equal(tradingStockPositions(asTrading([event('sale', 'SALE', { contract: checked.contract })])).length, 0);
  });

  it('TD-08: itemized sale does not deduct stock until delivered shipment', () => {
    const opening = event('open', 'STOCK', {
      title: 'SKU-T',
      quantity: '10',
      unit: 'pcs',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '10', ownership: 'OWNED' },
    });
    const saleWrite = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Itemized',
        channelName: 'Wholesale',
        channelTracking: true,
        lines: [{ itemName: 'SKU-T', quantity: '2', unit: 'pcs', unitPrice: '18000' }],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
      }),
      asTrading([opening])
    );
    assert.equal(saleWrite.ok, true);
    if (!saleWrite.ok) return;
    const sale = event('sale', 'SALE', {
      amount: saleWrite.amount,
      paymentStatus: saleWrite.paymentStatus,
      paymentRelevance: 'RECEIVABLE',
      lines: [{ itemName: 'SKU-T', quantity: '2', unit: 'pcs', unitPrice: '18000' }],
      contract: saleWrite.contract,
    });
    assert.equal(tradingStockPositions(asTrading([opening, sale])).find((row) => row.item === 'SKU-T')?.onHand, 10);
    const delivered = event('ship', 'SHIPMENT', {
      title: 'SKU-T',
      quantity: '2',
      unit: 'pcs',
      occurredAt: '2026-10-11T00:00:00.000Z',
      lines: [{ itemName: 'SKU-T', quantity: '2', unit: 'pcs', unitPrice: '0' }],
      contract: { shipmentStatus: 'DELIVERED' },
    });
    assert.equal(tradingStockPositions(asTrading([opening, sale, delivered])).find((row) => row.item === 'SKU-T')?.onHand, 8);
  });

  it('TD-V4-03: partial purchase receipt adds only received qty', () => {
    const buy = event('buy', 'PURCHASE', {
      amount: '2000.0000',
      counterpartyName: 'Mill',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Bags', quantity: '100', unit: 'pcs', unitPrice: '20' }],
      contract: { receiptState: 'PART_RECEIVED', quantityReceived: '40', paymentState: 'PAY_LATER', ownership: 'OWNED' },
      dueAt: '2026-10-20',
    });
    assert.equal(tradingStockPositions(asTrading([buy]))[0]?.onHand, 40);
  });

  it('credit sale without due date is rejected', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Credit',
        amount: '1000',
        channelName: 'Wholesale',
        channelTracking: true,
        counterpartyName: 'Buyer A',
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
      }),
      []
    );
    assert.equal(checked.ok, false);
  });

  it('payment readiness buckets undated as OPEN not overdue', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: '900',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          dueAt: null,
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
        event('late', 'SALE', {
          amount: '500',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer B',
          dueAt: '2026-10-01',
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
          occurredAt: '2026-10-11T00:00:00.000Z',
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    const undated = view.paymentReadiness.find((row) => row.id === 'sale');
    const overdue = view.paymentReadiness.find((row) => row.id === 'late');
    assert.equal(undated?.dueBucket, 'OPEN');
    assert.equal(overdue?.dueBucket, 'OVERDUE');
    assert.equal(view.paymentReadiness[0]?.dueBucket, 'OVERDUE');
  });

  it('opening + purchase received → on-hand; Set Current reconciles (not 300)', () => {
    const open = event('open', 'STOCK', {
      title: 'Bags',
      quantity: '100',
      unit: 'pcs',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '100', ownership: 'OWNED' },
    });
    const buy = event('buy', 'PURCHASE', {
      amount: '2000.0000',
      counterpartyName: 'Mill',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Bags', quantity: '100', unit: 'pcs', unitPrice: '20' }],
      contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
      occurredAt: '2026-10-11T00:00:00.000Z',
    });
    assert.equal(tradingStockPositions(asTrading([open, buy]))[0]?.onHand, 200);
    const set = event('count', 'STOCK', {
      title: 'Bags',
      quantity: '180',
      unit: 'pcs',
      occurredAt: '2026-10-12T00:00:00.000Z',
      contract: { stockAction: 'SET', stockReason: 'stocktake', declaredQuantity: '180', ownership: 'OWNED' },
    });
    assert.equal(tradingStockPositions(asTrading([open, buy, set]))[0]?.onHand, 180);
  });

  it('partial collection leaves sales unchanged', () => {
    const sale = event('sale', 'SALE', {
      amount: '100000.0000',
      counterpartyName: 'Buyer A',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'PENDING',
      contract: { entryMode: 'QUICK', paymentState: 'PART_PAID', receivedAmount: '40000.0000' },
    });
    const collect = event('collect', 'COLLECTION', {
      amount: '40000.0000',
      parentEventId: 'sale',
      paymentStatus: 'COLLECTED',
      paymentRelevance: 'RECEIVABLE',
      contract: { allocations: [{ sourceId: 'sale', amount: '40000.0000' }] },
    });
    assert.equal(obligationRemaining(asTrading([sale, collect])[0]!, asTrading([sale, collect]), 'COLLECTION'), 60000);
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '200000'),
      role: 'FINANCE',
      events: [sale, collect],
    });
    assert.equal(view.finance?.sales, '100000.0000');
    assert.equal(view.finance?.received, '40000.0000');
    assert.equal(view.finance?.outstanding, '60000.0000');
  });

  it('QA-07: finance.sales nets customer RETURN credit notes; catalog obligationRemaining credits RETURN', () => {
    const sale = event('sale', 'SALE', {
      amount: '1000.0000',
      counterpartyName: 'Buyer A',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'PENDING',
      contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
    });
    const credit = event('ret', 'RETURN', {
      amount: '250.0000',
      parentEventId: 'sale',
      title: 'Credit note',
      contract: {
        returnDirection: 'CUSTOMER',
        goodsOutcome: 'NOT_RETURNED',
        returnReason: 'Price adjustment',
      },
    });
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '200000'),
      role: 'FINANCE',
      events: [sale, credit],
    });
    assert.equal(view.finance?.sales, '750.0000');
    assert.equal(view.finance?.outstanding, '750.0000');
    assert.equal(obligationRemaining(asTrading([sale, credit])[0]!, asTrading([sale, credit]), 'COLLECTION'), 750);

    const supplierReturn = event('sret', 'RETURN', {
      amount: '100.0000',
      parentEventId: 'sale',
      title: 'Wrong direction',
      contract: {
        returnDirection: 'SUPPLIER',
        goodsOutcome: 'NOT_RETURNED',
        returnReason: 'Should not net sales',
      },
    });
    const ignoreSupplier = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '200000'),
      role: 'FINANCE',
      events: [sale, supplierReturn],
    });
    assert.equal(ignoreSupplier.finance?.sales, '1000.0000');
  });

  it('partial payment leaves purchase cost unchanged', () => {
    const buy = event('buy', 'PURCHASE', {
      amount: '80000.0000',
      counterpartyName: 'Supplier B',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      contract: { receiptState: 'AWAITING', paymentState: 'PART_PAID', receivedAmount: '30000.0000', ownership: 'OWNED' },
    });
    const pay = event('pay', 'PAYMENT', {
      amount: '30000.0000',
      parentEventId: 'buy',
      paymentStatus: 'PAID',
      paymentRelevance: 'PAYABLE',
      contract: { allocations: [{ sourceId: 'buy', amount: '30000.0000' }] },
    });
    assert.equal(obligationRemaining(asTrading([buy, pay])[0]!, asTrading([buy, pay]), 'PAYMENT'), 50000);
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '100000', '200000'),
      role: 'FINANCE',
      events: [buy, pay],
    });
    assert.equal(view.continuity.payable, '50000.0000');
    assert.equal(view.finance?.eligibleCost, '80000.0000');
  });

  it('consignment receipt is not owned stock', () => {
    const consignment = event('consign', 'PURCHASE', {
      amount: '5000.0000',
      counterpartyName: 'Consignor',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Demo SKU', quantity: '50', unit: 'pcs', unitPrice: '100' }],
      contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'CONSIGNMENT' },
    });
    assert.equal(tradingStockPositions(asTrading([consignment])).length, 0);
    const owned = event('owned', 'PURCHASE', {
      amount: '1000.0000',
      counterpartyName: 'Mill',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Demo SKU', quantity: '10', unit: 'pcs', unitPrice: '100' }],
      contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
      occurredAt: '2026-10-11T00:00:00.000Z',
    });
    assert.equal(tradingStockPositions(asTrading([consignment, owned]))[0]?.onHand, 10);
  });

  it('order amount is not sales until billed', () => {
    const orderWrite = validateTradingWrite(
      write({
        eventType: 'ORDER',
        title: 'Buyer order',
        amount: '50000',
        quantity: '100',
        channelName: 'Wholesale',
        counterpartyName: 'Buyer A',
        contract: { orderStatus: 'PLACED' },
      }),
      []
    );
    assert.equal(orderWrite.ok, true);
    if (!orderWrite.ok) return;
    assert.equal(orderWrite.amount, null);
    assert.equal(orderWrite.contract.estimatedAmount, '50000');
    const order = event('ord', 'ORDER', {
      quantity: '100',
      counterpartyName: 'Buyer A',
      contract: orderWrite.contract,
    });
    const before = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '100000'),
      role: 'FINANCE',
      events: [order],
    });
    assert.equal(before.finance?.sales, '0.0000');
    const bill = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Invoice',
        amount: '50000',
        channelName: 'Wholesale',
        channelTracking: true,
        parentEventId: 'ord',
        counterpartyName: 'Buyer A',
        dueAt: '2026-10-30',
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
      }),
      asTrading([order])
    );
    assert.equal(bill.ok, true);
    if (!bill.ok) return;
    const after = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '100000'),
      role: 'FINANCE',
      events: [
        order,
        event('inv', 'SALE', {
          amount: bill.amount,
          paymentStatus: bill.paymentStatus,
          paymentRelevance: 'RECEIVABLE',
          parentEventId: 'ord',
          counterpartyName: 'Buyer A',
          contract: bill.contract,
        }),
      ],
    });
    assert.equal(after.finance?.sales, '50000.0000');
  });

  it('rejects incompatible UOM and converts pack via product conversionToBase', () => {
    const product = event('prod', 'UPDATE', {
      title: 'Widget',
      master: { kind: 'PRODUCT', name: 'Widget', unit: 'pc', dimension: 'count', conversionToBase: '12' },
    });
    const bad = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Sale',
        channelName: 'Wholesale',
        channelTracking: true,
        lines: [{ itemName: 'Widget', quantity: '2', unit: 'kg', unitPrice: '10', masterId: 'prod' }],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
      }),
      asTrading([product])
    );
    assert.equal(bad.ok, false);

    const pack = validateTradingWrite(
      write({
        eventType: 'PURCHASE',
        title: 'Buy packs',
        counterpartyName: 'Mill',
        dueAt: '2026-10-30',
        lines: [{ itemName: 'Widget', quantity: '2', unit: 'pack', unitPrice: '120', masterId: 'prod' }],
        contract: { entryMode: 'ITEMIZED', receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
      }),
      asTrading([product])
    );
    assert.equal(pack.ok, true);
    if (!pack.ok) return;
    assert.equal(pack.contract.receiptState, 'RECEIVED');
    const lines = [
      { itemName: 'Widget', quantity: '2', unit: 'pack', unitPrice: '120', masterId: 'prod', baseQuantity: '24.0000' },
    ];
    const buy = event('buy', 'PURCHASE', {
      amount: pack.amount,
      counterpartyName: 'Mill',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines,
      contract: pack.contract,
    });
    assert.equal(tradingStockPositions(asTrading([buy]))[0]?.onHand, 24);
  });

  it('return to stock increases on-hand; not-returned leaves stock; over-return blocked', () => {
    const buy = event('buy', 'PURCHASE', {
      amount: '1000.0000',
      counterpartyName: 'Mill',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Bolt', quantity: '10', unit: 'pc', unitPrice: '100', baseQuantity: '10.0000' }],
      contract: { receiptState: 'RECEIVED', paymentState: 'PAY_LATER', ownership: 'OWNED' },
    });
    const sale = event('sale', 'SALE', {
      amount: '500.0000',
      counterpartyName: 'Buyer A',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'PENDING',
      dueAt: '2026-10-30',
      contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
    });
    const toStock = validateTradingWrite(
      write({
        eventType: 'RETURN',
        title: 'Customer return',
        amount: '100',
        quantity: '2',
        parentEventId: 'sale',
        contract: {
          returnDirection: 'CUSTOMER',
          goodsOutcome: 'TO_STOCK',
          returnReason: 'Damaged outer',
        },
        lines: [{ itemName: 'Bolt', quantity: '2', unit: 'pc', unitPrice: '50' }],
      }),
      asTrading([buy, sale])
    );
    assert.equal(toStock.ok, true);
    if (!toStock.ok) return;
    const returned = event('ret', 'RETURN', {
      amount: toStock.amount,
      quantity: toStock.quantity,
      parentEventId: 'sale',
      lines: [{ itemName: 'Bolt', quantity: '2', unit: 'pc', unitPrice: '50', baseQuantity: '2.0000' }],
      contract: toStock.contract,
      title: 'Bolt',
    });
    assert.equal(tradingStockPositions(asTrading([buy, sale, returned]))[0]?.onHand, 12);
    assert.equal(obligationRemaining(asTrading([buy, sale, returned])[1]!, asTrading([buy, sale, returned]), 'COLLECTION'), 400);

    const notReturned = validateTradingWrite(
      write({
        eventType: 'RETURN',
        title: 'Credit only',
        amount: '50',
        parentEventId: 'sale',
        contract: {
          returnDirection: 'CUSTOMER',
          goodsOutcome: 'NOT_RETURNED',
          returnReason: 'Price adjustment',
        },
      }),
      asTrading([buy, sale, returned])
    );
    assert.equal(notReturned.ok, true);
    if (!notReturned.ok) return;
    const creditOnly = event('ret2', 'RETURN', {
      amount: notReturned.amount,
      parentEventId: 'sale',
      contract: notReturned.contract,
    });
    assert.equal(tradingStockPositions(asTrading([buy, sale, returned, creditOnly]))[0]?.onHand, 12);

    const over = validateTradingWrite(
      write({
        eventType: 'RETURN',
        title: 'Too much',
        amount: '9999',
        parentEventId: 'sale',
        contract: {
          returnDirection: 'CUSTOMER',
          goodsOutcome: 'NOT_RETURNED',
          returnReason: 'Bad',
        },
      }),
      asTrading([buy, sale, returned, creditOnly])
    );
    assert.equal(over.ok, false);
  });

  it('tax NOT_CONFIGURED clears taxAmount; exclusive stores entered tax; FX missing rate blocked', () => {
    const unconfigured = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Sale',
        amount: '1000',
        channelName: 'Wholesale',
        channelTracking: true,
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED', taxMode: 'NOT_CONFIGURED', taxAmount: '180' },
      }),
      []
    );
    assert.equal(unconfigured.ok, true);
    if (!unconfigured.ok) return;
    assert.equal(unconfigured.contract.taxMode, 'NOT_CONFIGURED');
    assert.equal(unconfigured.contract.taxAmount, null);

    const exclusive = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Sale',
        amount: '1000',
        channelName: 'Wholesale',
        channelTracking: true,
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED', taxMode: 'EXCLUSIVE', taxAmount: '180' },
      }),
      []
    );
    assert.equal(exclusive.ok, true);
    if (!exclusive.ok) return;
    assert.equal(exclusive.contract.taxAmount, '180');

    const missingRate = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'FX sale',
        amount: '100',
        channelName: 'Wholesale',
        channelTracking: true,
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED', originalCurrency: 'USD' },
      }),
      []
    );
    assert.equal(missingRate.ok, false);

    const withRate = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'FX sale',
        amount: '100',
        channelName: 'Wholesale',
        channelTracking: true,
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED', originalCurrency: 'USD', fxRate: '83.5' },
      }),
      []
    );
    assert.equal(withRate.ok, true);
    if (!withRate.ok) return;
    assert.equal(withRate.contract.convertedAmount, '8350.0000');
  });

  it('itemized sale with LOV-bound product master converts dozen to base', () => {
    const product = event('prod', 'UPDATE', {
      title: 'Cup',
      master: { kind: 'PRODUCT', name: 'Cup', unit: 'pc', dimension: 'count' },
    });
    const sale = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Dozen cups',
        channelName: 'Wholesale',
        channelTracking: true,
        lines: [{ itemName: 'Cup', quantity: '1', unit: 'dozen', unitPrice: '240', masterId: 'prod' }],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
      }),
      asTrading([product])
    );
    assert.equal(sale.ok, true);
    if (!sale.ok) return;
    assert.equal(sale.quantity, '12.0000');
  });

  it('decorates locked trading board titles and primary M4 commitments', () => {
    const view = projectOperatingMoment({
      familyCode: 'TRADING_DISTRIBUTION',
      prefs: prefs('TRADING_WHOLESALE', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: '1000.0000',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Buyer A',
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
      ],
    });
    const orders = view.boards.moments.find((section) => section.code === 'orders');
    const stock = view.boards.moments.find((section) => section.code === 'stock');
    const network = view.boards.moments.find((section) => section.code === 'network');
    const m4 = view.boards.moments.find((section) => section.code === 'M4');
    const dup = view.boards.moments.find((section) => section.code === 'commitments');
    assert.equal(orders?.title, 'Orders & Commitments');
    assert.equal(stock?.title, 'Stock & Products');
    assert.equal(network?.title, 'People & Network');
    assert.equal(m4?.title, 'Open Commitments');
    assert.ok((m4?.rows.length ?? 0) > 0);
    assert.equal(dup?.rows.length, 0);
    assert.equal(view.boards.life.find((section) => section.code === 'L2')?.title, 'Buyers & Channels');
    assert.equal(view.boards.life.find((section) => section.code === 'L3')?.title, 'Money Moving');
  });
});
