import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';
import {
  obligationRemaining,
  stockPositions,
  validateRetailWrite,
  type RetailEvent,
  type RetailWrite,
} from '../src/modules/business/operating-retail-contract';

function prefs(subtype: string, budget = '', target = '') {
  return normalizeOperatingPreferences(
    'RETAIL_CONSUMER',
    {
      subtype,
      identityName: subtype,
      partyName: subtype,
      monthlyBudget: budget,
      monthlyTarget: target,
      targetMetric: 'REVENUE',
      channels: subtype === 'CONSUMER_SERVICE' ? ['Walk-in'] : ['Store'],
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
    channelName: 'Store',
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: '2026-10-10T10:00:00.000Z',
    ...fields,
  };
}

function asRetail(events: OperatingEventRow[]): RetailEvent[] {
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
  }));
}

function write(partial: Partial<RetailWrite> & Pick<RetailWrite, 'eventType' | 'title'>): RetailWrite {
  return {
    amount: null,
    ...partial,
  };
}

describe('retail functional analytics RF / ALL', () => {
  it('ALL-02: no target configured does not invent achievement', () => {
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE'),
      role: 'FINANCE',
      events: [event('sale', 'SALE', { amount: '2400.0000', paymentStatus: 'COLLECTED', paymentRelevance: 'RECEIVABLE' })],
    });
    assert.equal(view.target?.configured, false);
    assert.equal(view.budget?.configured, false);
    assert.equal(view.finance?.sales, '2400.0000');
  });

  it('RF-01: quick amount sale received posts sales and receipt without stock move', () => {
    const checked = validateRetailWrite(
      write({
        eventType: 'SALE',
        title: 'Walk-in',
        amount: '2400',
        channelName: 'Store',
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
      }),
      []
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    assert.equal(checked.paymentStatus, 'COLLECTED');
    assert.equal(checked.amount, '2400.0000');
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE', '10000', '30000'),
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
    assert.equal(view.finance?.outstanding, '0.0000');
    assert.equal(stockPositions(asRetail(view.activity.map((row) => event(row.id, row.eventType, {
      amount: row.amount,
      paymentStatus: row.paymentStatus ?? 'NONE',
      approvalStatus: row.approvalStatus ?? 'NONE',
      contract: checked.contract,
    })))).length, 0);
  });

  it('RF-02: itemized sale does not move stock until a stock event', () => {
    const opening = event('open', 'STOCK', {
      title: 'SKU-A',
      quantity: '10',
      unit: 'pcs',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '10' },
    });
    const saleWrite = validateRetailWrite(
      write({
        eventType: 'SALE',
        title: 'Itemized',
        channelName: 'Store',
        lines: [{ itemName: 'SKU-A', quantity: '2', unit: 'pcs', unitPrice: '18000' }],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
      }),
      asRetail([opening])
    );
    assert.equal(saleWrite.ok, true);
    if (!saleWrite.ok) return;
    assert.equal(saleWrite.amount, '36000.0000');
    const sale = event('sale', 'SALE', {
      amount: saleWrite.amount,
      paymentStatus: saleWrite.paymentStatus,
      paymentRelevance: 'RECEIVABLE',
      lines: [{ itemName: 'SKU-A', quantity: '2', unit: 'pcs', unitPrice: '18000' }],
      contract: saleWrite.contract,
    });
    const positions = stockPositions(asRetail([opening, sale]));
    assert.equal(positions.find((row) => row.item === 'SKU-A')?.onHand, 10);
  });

  it('RF-03/RF-04: partial collection reduces receivable without changing sales', () => {
    const sale = event('sale', 'SALE', {
      amount: '144000.0000',
      counterpartyName: 'Customer B',
      paymentRelevance: 'RECEIVABLE',
      paymentStatus: 'PENDING',
      dueAt: '2026-10-15',
      contract: { entryMode: 'QUICK', paymentState: 'PART_PAID', receivedAmount: '60000.0000' },
    });
    const initial = event('init', 'COLLECTION', {
      amount: '60000.0000',
      parentEventId: 'sale',
      paymentStatus: 'COLLECTED',
      paymentRelevance: 'RECEIVABLE',
      counterpartyName: 'Customer B',
      contract: { allocations: [{ sourceId: 'sale', amount: '60000.0000' }] },
    });
    const afterPartial = event('collect', 'COLLECTION', {
      amount: '15000.0000',
      parentEventId: 'sale',
      paymentStatus: 'COLLECTED',
      paymentRelevance: 'RECEIVABLE',
      counterpartyName: 'Customer B',
      occurredAt: '2026-10-18T00:00:00.000Z',
      contract: { allocations: [{ sourceId: 'sale', amount: '15000.0000' }] },
    });
    const open = obligationRemaining(asRetail([sale, initial, afterPartial])[0]!, asRetail([sale, initial, afterPartial]), 'COLLECTION');
    assert.equal(open, 69000);
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE', '10000', '300000'),
      role: 'FINANCE',
      events: [sale, initial, afterPartial],
      asOf: new Date('2026-10-18T12:00:00.000Z'),
    });
    assert.equal(view.finance?.sales, '144000.0000');
    assert.equal(view.finance?.received, '75000.0000');
    assert.equal(view.finance?.outstanding, '69000.0000');
    const overdue = view.attention.find((item) => item.kind === 'OVERDUE_RECEIVABLE');
    assert.ok(overdue);
    assert.equal(overdue?.cta, 'collect');
    assert.equal(overdue?.amount, '69000.0000');
  });

  it('RF-06: set current stock reconciles delta, does not add declared again', () => {
    const system = event('open', 'STOCK', {
      title: 'Rice',
      quantity: '100',
      unit: 'kg',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '100' },
    });
    const set = event('count', 'STOCK', {
      title: 'Rice',
      quantity: '200',
      unit: 'kg',
      occurredAt: '2026-10-11T00:00:00.000Z',
      contract: { stockAction: 'SET', stockReason: 'stocktake', declaredQuantity: '200' },
    });
    assert.equal(stockPositions(asRetail([system, set]))[0]?.onHand, 200);
  });

  it('RF-07: partial goods receipt increases stock by received qty only', () => {
    const purchase = event('buy', 'PURCHASE', {
      amount: '1000.0000',
      counterpartyName: 'Supplier A',
      paymentRelevance: 'PAYABLE',
      paymentStatus: 'PENDING',
      lines: [{ itemName: 'Oil', quantity: '100', unit: 'L', unitPrice: '10' }],
      contract: { receiptState: 'PART_RECEIVED', quantityReceived: '40', paymentState: 'PAY_LATER' },
    });
    assert.equal(stockPositions(asRetail([purchase]))[0]?.onHand, 40);
  });

  it('RF-09/RF-10: restaurant order is not sales until billed once', () => {
    const order = event('ord', 'ORDER', {
      channelName: 'Dine-in',
      contract: { orderStatus: 'PLACED', estimatedAmount: '3000' },
    });
    const before = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RESTAURANT_FOOD', '5000', '20000'),
      role: 'FINANCE',
      events: [order],
    });
    assert.equal(before.finance?.sales, '0.0000');
    const bill = validateRetailWrite(
      write({
        eventType: 'SALE',
        title: 'Bill',
        amount: '3000',
        channelName: 'Dine-in',
        parentEventId: 'ord',
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
      }),
      asRetail([order])
    );
    assert.equal(bill.ok, true);
    if (!bill.ok) return;
    const billed = event('bill', 'SALE', {
      amount: bill.amount,
      paymentStatus: bill.paymentStatus,
      paymentRelevance: 'RECEIVABLE',
      parentEventId: 'ord',
      channelName: 'Dine-in',
      contract: bill.contract,
    });
    const after = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RESTAURANT_FOOD', '5000', '20000'),
      role: 'FINANCE',
      events: [order, billed],
    });
    assert.equal(after.finance?.sales, '3000.0000');
    assert.equal(after.finance?.received, '3000.0000');
    const duplicate = validateRetailWrite(
      write({
        eventType: 'SALE',
        title: 'Bill again',
        amount: '3000',
        channelName: 'Dine-in',
        parentEventId: 'ord',
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
      }),
      asRetail([order, billed])
    );
    assert.equal(duplicate.ok, false);
  });

  it('RF-11: waste stock reduce does not create purchase', () => {
    const open = event('open', 'STOCK', {
      title: 'Tomato',
      quantity: '5',
      unit: 'kg',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '5' },
    });
    const waste = event('waste', 'STOCK', {
      title: 'Tomato',
      quantity: '2',
      unit: 'kg',
      occurredAt: '2026-10-12T00:00:00.000Z',
      contract: { stockAction: 'REDUCE', stockReason: 'waste' },
    });
    assert.equal(stockPositions(asRetail([open, waste]))[0]?.onHand, 3);
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RESTAURANT_FOOD', '5000', '20000'),
      role: 'FINANCE',
      events: [open, waste],
    });
    assert.equal(view.finance?.eligibleCost, '0.0000');
  });

  it('ALL-01: partial itemized sales show product mix coverage', () => {
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE', '10000', '50000'),
      role: 'FINANCE',
      events: [
        event('q1', 'SALE', {
          amount: '100',
          paymentStatus: 'COLLECTED',
          paymentRelevance: 'RECEIVABLE',
          contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
        }),
        event('i1', 'SALE', {
          amount: '200',
          paymentStatus: 'COLLECTED',
          paymentRelevance: 'RECEIVABLE',
          lines: [{ itemName: 'A', quantity: '1', unit: 'pcs', unitPrice: '200' }],
          contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
        }),
      ],
    });
    const sales = view.boards.moments.find((section) => section.code === 'sales');
    assert.match(String(sales?.coverage ?? ''), /1 of 2/);
  });

  it('ALL-05: team member does not see finance amounts', () => {
    const events = [
      event('sale', 'SALE', {
        amount: '500',
        paymentStatus: 'PENDING',
        paymentRelevance: 'RECEIVABLE',
        counterpartyName: 'Customer B',
        dueAt: '2026-10-01',
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
      }),
    ];
    const staff = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE', '1000', '5000'),
      role: 'TEAM_MEMBER',
      events,
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    assert.equal(staff.finance, null);
    assert.equal(staff.activity[0]?.amount, null);
  });

  it('ALL-07: undated open balance is not overdue attention', () => {
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE', '1000', '5000'),
      role: 'FINANCE',
      events: [
        event('sale', 'SALE', {
          amount: '900',
          paymentStatus: 'PENDING',
          paymentRelevance: 'RECEIVABLE',
          counterpartyName: 'Customer B',
          dueAt: null,
          contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
        }),
      ],
      asOf: new Date('2026-10-18T00:00:00.000Z'),
    });
    assert.equal(view.attention.some((item) => item.kind === 'OVERDUE_RECEIVABLE'), false);
    assert.equal(view.attention.some((item) => item.kind === 'UNDATED'), false);
    const open = view.boards.moments.find((section) => section.code === 'M4');
    assert.ok((open?.rows.length ?? 0) > 0);
  });
});

describe('consumer service analytics CS / CS-L', () => {
  it('CS-01: booking alone does not post revenue', () => {
    const booking = validateRetailWrite(
      write({
        eventType: 'APPOINTMENT',
        title: 'Haircut',
        occurredAt: '2026-10-20T10:30:00.000Z',
        amount: '500',
      }),
      []
    );
    assert.equal(booking.ok, true);
    if (!booking.ok) return;
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE', '2000', '10000'),
      role: 'FINANCE',
      events: [
        event('book', 'APPOINTMENT', {
          title: 'Haircut',
          amount: null,
          occurredAt: '2026-10-20T10:30:00.000Z',
          contract: booking.contract,
        }),
      ],
    });
    assert.equal(view.finance?.sales, '0.0000');
    assert.match(String(view.boards.life.find((s) => s.code === 'service-flow')?.detail ?? ''), /Booked 1/);
  });

  it('CS-L01..L05: reschedule keeps one booking; start/complete add no revenue', () => {
    const created = event('book', 'APPOINTMENT', {
      title: 'Facial',
      occurredAt: '2026-10-20T09:00:00.000Z',
      contract: { appointmentStatus: 'BOOKED' },
    });
    const reschedule = validateRetailWrite(
      write({
        eventType: 'APPOINTMENT',
        title: 'Facial',
        revisingId: 'book',
        occurredAt: '2026-10-21T11:00:00.000Z',
        contract: { appointmentAction: 'RESCHEDULE' },
      }),
      asRetail([created])
    );
    assert.equal(reschedule.ok, true);
    if (!reschedule.ok) return;
    assert.equal(reschedule.contract.appointmentStatus, 'BOOKED');
    assert.deepEqual(reschedule.contract.priorTimes, ['2026-10-20']);
    const started = validateRetailWrite(
      write({
        eventType: 'APPOINTMENT',
        title: 'Facial',
        revisingId: 'book',
        occurredAt: '2026-10-21T11:00:00.000Z',
        contract: { appointmentAction: 'START' },
      }),
      asRetail([
        {
          ...asRetail([created])[0]!,
          occurredAt: '2026-10-21T11:00:00.000Z',
          contract: reschedule.contract,
        },
      ])
    );
    assert.equal(started.ok, true);
    if (!started.ok) return;
    assert.equal(started.contract.appointmentStatus, 'IN_PROGRESS');
    const completed = validateRetailWrite(
      write({
        eventType: 'APPOINTMENT',
        title: 'Facial',
        revisingId: 'book',
        occurredAt: '2026-10-21T12:00:00.000Z',
        contract: { appointmentAction: 'COMPLETE' },
      }),
      asRetail([
        {
          ...asRetail([created])[0]!,
          occurredAt: '2026-10-21T11:00:00.000Z',
          contract: started.contract,
        },
      ])
    );
    assert.equal(completed.ok, true);
    if (!completed.ok) return;
    assert.equal(completed.contract.appointmentStatus, 'COMPLETED');
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE', '2000', '10000'),
      role: 'FINANCE',
      events: [
        event('book', 'APPOINTMENT', {
          title: 'Facial',
          occurredAt: '2026-10-21T11:00:00.000Z',
          contract: completed.contract,
        }),
      ],
    });
    assert.equal(view.finance?.sales, '0.0000');
    assert.match(String(view.boards.moments.find((s) => s.code === 'appointments')?.detail ?? ''), /Completed 1/);
  });

  it('CS-03 / CS-L06/L07: finalize charge then part-collect', () => {
    const booking = event('book', 'APPOINTMENT', {
      title: 'Massage',
      occurredAt: '2026-10-10T10:00:00.000Z',
      contract: { appointmentStatus: 'COMPLETED', completedAt: '2026-10-10T11:00:00.000Z' },
    });
    const charge = validateRetailWrite(
      write({
        eventType: 'SERVICE',
        title: 'Massage charge',
        amount: '2000',
        channelName: 'Walk-in',
        parentEventId: 'book',
        counterpartyName: 'Customer B',
        contract: { entryMode: 'QUICK', paymentState: 'PART_PAID', receivedAmount: '500' },
      }),
      asRetail([booking])
    );
    assert.equal(charge.ok, true);
    if (!charge.ok) return;
    const posted = event('charge', 'SERVICE', {
      amount: charge.amount,
      paymentStatus: charge.paymentStatus,
      paymentRelevance: 'RECEIVABLE',
      parentEventId: 'book',
      counterpartyName: 'Customer B',
      channelName: 'Walk-in',
      contract: charge.contract,
      dueAt: '2026-10-20',
    });
    const collectChild = event('col', 'COLLECTION', {
      amount: '500.0000',
      parentEventId: 'charge',
      paymentStatus: 'COLLECTED',
      paymentRelevance: 'RECEIVABLE',
      contract: { allocations: [{ sourceId: 'charge', amount: '500.0000' }] },
    });
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE', '2000', '10000'),
      role: 'FINANCE',
      events: [booking, posted, collectChild],
    });
    assert.equal(view.finance?.sales, '2000.0000');
    assert.equal(view.finance?.received, '500.0000');
    assert.equal(view.finance?.outstanding, '1500.0000');
    const secondCharge = validateRetailWrite(
      write({
        eventType: 'SERVICE',
        title: 'Massage charge again',
        amount: '2000',
        channelName: 'Walk-in',
        parentEventId: 'book',
        counterpartyName: 'Customer B',
        contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
      }),
      asRetail([booking, posted, collectChild])
    );
    assert.equal(secondCharge.ok, false);
  });

  it('CS-L09: collection beyond open balance is blocked', () => {
    const charge = event('charge', 'SERVICE', {
      amount: '100.0000',
      paymentStatus: 'PENDING',
      paymentRelevance: 'RECEIVABLE',
      counterpartyName: 'Customer B',
      channelName: 'Walk-in',
      contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
    });
    const over = validateRetailWrite(
      write({
        eventType: 'COLLECTION',
        title: 'Collect',
        amount: '150',
        counterpartyName: 'Customer B',
        parentEventId: 'charge',
        contract: { allocations: [{ sourceId: 'charge', amount: '150' }] },
      }),
      asRetail([charge])
    );
    assert.equal(over.ok, false);
  });

  it('CS service flow and customers board are present', () => {
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE', '2000', '10000'),
      role: 'FINANCE',
      events: [
        event('book', 'APPOINTMENT', {
          title: 'Cut',
          channelName: 'Walk-in',
          counterpartyName: 'Customer B',
          contract: { appointmentStatus: 'BOOKED' },
        }),
      ],
    });
    assert.ok(view.boards.life.some((section) => section.code === 'service-flow'));
    assert.ok(view.boards.moments.some((section) => section.code === 'customers'));
    assert.ok(view.boards.moments.some((section) => section.code === 'M4'));
  });

  it('RF-05 / RF-08: payment is not a second expense and a credit does not restock', () => {
    const spend = event('rent', 'SPEND', {
      amount: '5000.0000',
      paymentStatus: 'PENDING',
      paymentRelevance: 'PAYABLE',
      counterpartyName: 'Landlord',
      contract: { expenseCategory: 'Rent/lease', paymentState: 'PAY_LATER' },
    });
    const pay = event('pay', 'PAYMENT', {
      amount: '5000.0000',
      parentEventId: 'rent',
      paymentStatus: 'PAID',
      paymentRelevance: 'PAYABLE',
      counterpartyName: 'Landlord',
      contract: { allocations: [{ sourceId: 'rent', amount: '5000.0000' }] },
    });
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('RETAIL_STORE'),
      role: 'FINANCE',
      events: [spend, pay],
    });
    assert.equal(view.finance?.eligibleCost, '5000.0000');
    assert.equal(view.finance?.sales, '0.0000');
    const opening = event('open', 'STOCK', {
      title: 'SKU-A',
      quantity: '4',
      contract: { stockAction: 'SET', stockReason: 'opening', declaredQuantity: '4' },
    });
    const credit = event('credit', 'RETURN', {
      amount: '100.0000',
      parentEventId: 'sale',
      lines: [{ itemName: 'SKU-A', quantity: '1', unit: 'pcs', unitPrice: '100' }],
      contract: { returnDirection: 'CUSTOMER', goodsOutcome: 'NOT_RETURNED' },
    });
    assert.equal(stockPositions(asRetail([opening, credit]))[0]?.onHand, 4);
  });

  it('RF-06 awaiting receipt does not increase stock', () => {
    const purchase = event('buy', 'PURCHASE', {
      amount: '800.0000',
      counterpartyName: 'Mill',
      lines: [{ itemName: 'Flour', quantity: '20', unit: 'kg', unitPrice: '40' }],
      contract: { receiptState: 'AWAITING', paymentState: 'PAY_LATER' },
    });
    assert.equal(stockPositions(asRetail([purchase])).length, 0);
  });

  it('ALL-01 / ALL-03: channel and a named customer are required', () => {
    const noChannel = validateRetailWrite(
      write({ eventType: 'SALE', title: 'Sale', amount: '100', contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' } }),
      []
    );
    assert.equal(noChannel.ok, false);
    const unpaid = validateRetailWrite(
      write({
        eventType: 'SALE',
        title: 'Sale',
        amount: '100',
        channelName: 'Marketplace',
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
      }),
      []
    );
    assert.equal(unpaid.ok, false);
    assert.throws(() => prefs('RETAIL_STORE').channels && normalizeOperatingPreferences('RETAIL_CONSUMER', {
      subtype: 'RETAIL_STORE',
      identityName: 'Store',
      partyName: 'Store',
      channels: [],
      startDate: '2026-10-01',
    }, { userId: 'u', displayName: 'Asha' }));
  });

  it('ALL-05: quick amount never moves stock', () => {
    const sale = event('sale', 'SALE', {
      amount: '500.0000',
      paymentStatus: 'COLLECTED',
      contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
    });
    assert.equal(stockPositions(asRetail([sale])).length, 0);
  });

  it('RF order status revision keeps one order and posts no revenue', () => {
    const order = event('ord', 'ORDER', { channelName: 'Store', contract: { orderStatus: 'PLACED' } });
    const revised = validateRetailWrite(
      write({
        eventType: 'ORDER',
        title: 'Order',
        channelName: 'Store',
        revisingId: 'ord',
        contract: { orderStatus: 'COMPLETED' },
      }),
      asRetail([order])
    );
    assert.equal(revised.ok, true);
    if (!revised.ok) return;
    assert.equal(revised.amount, null);
    assert.equal(revised.contract.orderStatus, 'COMPLETED');
  });

  it('CS-02 / CS-05: completion is not revenue and a later collection does not increase it', () => {
    const booking = event('book', 'APPOINTMENT', {
      title: 'Cut',
      contract: { appointmentStatus: 'COMPLETED' },
    });
    const charge = event('charge', 'SERVICE', {
      amount: '3000.0000',
      paymentStatus: 'PENDING',
      paymentRelevance: 'RECEIVABLE',
      counterpartyName: 'Customer B',
      channelName: 'Walk-in',
      parentEventId: 'book',
      contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
    });
    const before = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE'),
      role: 'FINANCE',
      events: [booking],
    });
    assert.equal(before.finance?.sales, '0.0000');
    const collect = event('cash', 'COLLECTION', {
      amount: '1200.0000',
      parentEventId: 'charge',
      paymentStatus: 'COLLECTED',
      paymentRelevance: 'RECEIVABLE',
      counterpartyName: 'Customer B',
      contract: { allocations: [{ sourceId: 'charge', amount: '1200.0000' }] },
    });
    const after = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs: prefs('CONSUMER_SERVICE'),
      role: 'FINANCE',
      events: [booking, charge, collect],
    });
    assert.equal(after.finance?.sales, '3000.0000');
    assert.equal(after.finance?.received, '1200.0000');
  });
});
