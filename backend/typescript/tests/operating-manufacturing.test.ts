import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  manufacturingMeasure,
  manufacturingStock,
  validateManufacturingWrite,
  type ManufacturingEvent,
  type ManufacturingWrite,
} from '../src/modules/business/operating-manufacturing';
import {
  normalizeOperatingPreferences,
  projectOperatingMoment,
  type OperatingEventRow,
} from '../src/modules/business/operating-catalog';

const when = '2026-10-04T00:00:00.000Z';

function ok(input: ManufacturingWrite, existing: ManufacturingEvent[] = []) {
  const checked = validateManufacturingWrite(input, existing);
  assert.equal(checked.ok, true);
  if (!checked.ok) throw new Error('unreachable');
  return checked;
}

function row(id: string, eventType: string, fields: Partial<OperatingEventRow> = {}): OperatingEventRow {
  return {
    operatingEventId: id,
    eventType,
    title: fields.title ?? eventType,
    note: null,
    amount: null,
    currencyCode: 'INR',
    counterpartyName: null,
    channelName: null,
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: when,
    ...fields,
  };
}

function view(events: OperatingEventRow[], metric = 'REVENUE') {
  const prefs = normalizeOperatingPreferences(
    'MANUFACTURING_PRODUCTION',
    {
      subtype: 'MADE_TO_ORDER',
      identityName: 'Workshop',
      partyName: 'Workshop',
      monthlyBudget: '100000',
      monthlyTarget: '100000',
      targetMetric: metric,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
    },
    { userId: 'u', displayName: 'Asha' }
  );
  return projectOperatingMoment({ familyCode: 'MANUFACTURING_PRODUCTION', prefs, role: 'OWNER_ADMIN', events });
}

describe('manufacturing ledger gates', () => {
  it('01 quick purchase pending counts once toward budget and payables and does not stock', () => {
    const checked = ok({
      eventType: 'PURCHASE',
      title: 'Steel',
      amount: '20000',
      counterpartyName: 'Mill',
      occurredAt: when,
      contract: { entryMode: 'QUICK', receiptState: 'AWAITING', paymentState: 'PAY_LATER' },
    });
    const projected = view([
      row('buy', 'PURCHASE', {
        amount: checked.amount,
        paymentStatus: checked.paymentStatus,
        paymentRelevance: 'PAYABLE',
        counterpartyName: 'Mill',
        contract: checked.contract,
      }),
    ]);
    assert.equal(projected.finance?.eligibleCost, '20000.0000');
    assert.equal(projected.continuity.payable, '20000.0000');
    assert.equal(manufacturingStock([{ ...asEvent('buy', 'PURCHASE'), contract: checked.contract, lines: [] }]).length, 0);
  });

  it('02 a partial receipt stocks only the received quantity', () => {
    const checked = ok({
      eventType: 'PURCHASE',
      title: 'Resin',
      amount: '10000',
      counterpartyName: 'Mill',
      occurredAt: when,
      lines: [{ itemName: 'Resin', quantity: '100', unit: 'kg', unitPrice: '100' }],
      contract: { entryMode: 'ITEMIZED', receiptState: 'PART_RECEIVED', quantityReceived: '60', paymentState: 'PAY_LATER' },
    });
    const stock = manufacturingStock([
      {
        ...asEvent('buy', 'PURCHASE'),
        lines: [{ itemName: 'Resin', quantity: '100', unit: 'kg', unitPrice: '100' }],
        contract: checked.contract,
        unit: 'kg',
      },
    ]);
    assert.equal(stock[0]?.onHand, 60);
    assert.equal(checked.amount, '10000.0000');
  });

  it('03 a payment reduces the payable and is not a second purchase', () => {
    const purchase = {
      ...asEvent('buy', 'PURCHASE'),
      amount: '20000.0000',
      paymentStatus: 'PENDING',
      counterpartyName: 'Mill',
    };
    const checked = ok(
      { eventType: 'PAYMENT', title: 'Pay mill', amount: '5000', parentEventId: 'buy', counterpartyName: 'Mill' },
      [purchase]
    );
    const projected = view([
      row('buy', 'PURCHASE', { amount: '20000.0000', paymentStatus: 'PENDING', paymentRelevance: 'PAYABLE' }),
      row('pay', 'PAYMENT', { amount: checked.amount, parentEventId: 'buy', paymentStatus: 'PAID' }),
    ]);
    assert.equal(projected.continuity.payable, '15000.0000');
    assert.equal(projected.finance?.eligibleCost, '20000.0000');
  });

  it('04 quick output is recorded and does not create finished stock', () => {
    const checked = ok({
      eventType: 'PRODUCTION',
      title: 'Batch',
      amount: '40',
      contract: { entryMode: 'QUICK' },
    });
    assert.equal(checked.quantity, '40.0000');
    assert.equal(checked.contract.acceptedQuantity ?? null, null);
    const stock = manufacturingStock([{ ...asEvent('made', 'PRODUCTION'), quantity: checked.quantity, contract: checked.contract }]);
    assert.equal(stock.length, 0);
    const projected = view([row('made', 'PRODUCTION', { quantity: '40', amount: null })], 'PRODUCTION_OUTPUT');
    assert.equal(projected.target?.progress, '0.0000');
    assert.equal(projected.target?.coverage, 'not recorded');
    assert.equal(projected.continuity.produced, '40.0000');
  });

  it('05 a detailed batch stocks only accepted quantity', () => {
    const checked = ok({
      eventType: 'PRODUCTION',
      title: 'Bracket',
      quantity: '50',
      unit: 'pcs',
      rejectedQuantity: '5',
      contract: { entryMode: 'ITEMIZED', acceptedQuantity: '45', productionStatus: 'COMPLETED' },
    });
    const stock = manufacturingStock([
      { ...asEvent('made', 'PRODUCTION'), title: 'Bracket', quantity: '50', unit: 'pcs', contract: checked.contract },
    ]);
    assert.equal(stock.find((item) => item.kind === 'FINISHED')?.onHand, 45);
    const projected = view(
      [row('made', 'PRODUCTION', { title: 'Bracket', quantity: '50', unit: 'pcs', rejectedQuantity: '5', contract: checked.contract })],
      'PRODUCTION_OUTPUT'
    );
    assert.equal(projected.continuity.accepted, '45.0000');
    assert.equal(projected.target?.progress, '45.0000');
  });

  it('06 a priced order is a commitment and not revenue, cash, stock, or a receivable', () => {
    const checked = ok({
      eventType: 'CUSTOMER_ORDER',
      title: 'Order 30',
      amount: '30000',
      quantity: '30',
      counterpartyName: 'Buyer',
      occurredAt: when,
    });
    assert.equal(checked.paymentStatus, 'NONE');
    const projected = view([
      row('order', 'CUSTOMER_ORDER', {
        amount: checked.amount,
        quantity: '30',
        counterpartyName: 'Buyer',
        contract: checked.contract,
      }),
    ]);
    assert.equal(projected.finance?.sales, '0.0000');
    assert.equal(projected.continuity.receivable, '0.0000');
    assert.equal(projected.finance?.received, '0.0000');
  });

  it('07 an itemized delivery reduces the open quantity and does not post a sale', () => {
    const order = { ...asEvent('order', 'CUSTOMER_ORDER'), quantity: '30', amount: '30000.0000' };
    const checked = ok(
      {
        eventType: 'DELIVERY',
        title: 'Dispatch',
        quantity: '12',
        parentEventId: 'order',
        contract: { entryMode: 'ITEMIZED', deliveryStatus: 'DELIVERED' },
      },
      [order]
    );
    const projected = view([
      row('order', 'CUSTOMER_ORDER', { quantity: '30', amount: '30000' }),
      row('drop', 'DELIVERY', { quantity: checked.quantity, parentEventId: 'order', contract: checked.contract }),
    ]);
    assert.equal(projected.continuity.pendingDelivery, '18.0000');
    assert.equal(projected.finance?.sales, '0.0000');
  });

  it('08 a linked sale on credit posts revenue and a receivable once', () => {
    const order = { ...asEvent('order', 'CUSTOMER_ORDER'), quantity: '30' };
    const checked = ok(
      {
        eventType: 'SALE',
        title: 'Invoice',
        amount: '12000',
        counterpartyName: 'Buyer',
        parentEventId: 'order',
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' },
      },
      [order]
    );
    const projected = view([
      row('order', 'CUSTOMER_ORDER', { quantity: '30', amount: '30000' }),
      row('sale', 'SALE', { amount: checked.amount, parentEventId: 'order', paymentStatus: 'PENDING', paymentRelevance: 'RECEIVABLE' }),
    ]);
    assert.equal(projected.finance?.sales, '12000.0000');
    assert.equal(projected.continuity.receivable, '12000.0000');
    assert.equal(projected.continuity.pendingDelivery, '30.0000');
  });

  it('09 a collection reduces the receivable and does not change sales', () => {
    const sale = { ...asEvent('sale', 'SALE'), amount: '12000.0000', paymentStatus: 'PENDING' };
    const checked = ok(
      { eventType: 'COLLECTION', title: 'Receipt', amount: '4000', counterpartyName: 'Buyer', parentEventId: 'sale' },
      [sale]
    );
    const projected = view([
      row('sale', 'SALE', { amount: '12000', paymentStatus: 'PENDING', paymentRelevance: 'RECEIVABLE' }),
      row('cash', 'COLLECTION', { amount: checked.amount, parentEventId: 'sale', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(projected.continuity.receivable, '8000.0000');
    assert.equal(projected.finance?.sales, '12000.0000');
    assert.equal(projected.finance?.received, '4000.0000');
  });

  it('10 a quick received sale posts revenue and cash without a sku', () => {
    const checked = ok({
      eventType: 'SALE',
      title: 'Counter sale',
      amount: '3000',
      contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
    });
    assert.equal(checked.quantity, null);
    const projected = view([row('sale', 'SALE', { amount: checked.amount, paymentStatus: 'COLLECTED' })]);
    assert.equal(projected.finance?.sales, '3000.0000');
    assert.equal(projected.finance?.received, '3000.0000');
    assert.equal(manufacturingStock([]).length, 0);
  });

  it('11 a paid expense counts once', () => {
    const checked = ok({
      eventType: 'SPEND',
      title: 'Power',
      amount: '2000',
      contract: { paymentState: 'PAID' },
    });
    const projected = view([row('cost', 'SPEND', { amount: checked.amount, paymentStatus: 'PAID', paymentRelevance: 'PAYABLE' })]);
    assert.equal(projected.finance?.eligibleCost, '2000.0000');
    assert.equal(projected.finance?.cashPaid, '2000.0000');
    assert.equal(projected.continuity.payable, '0.0000');
  });

  it('12 a credit note reduces net sales and receivables and does not restock', () => {
    const projected = view([
      row('sale', 'SALE', { amount: '15000', paymentStatus: 'PENDING', paymentRelevance: 'RECEIVABLE' }),
      row('credit', 'RETURN', { amount: '1000', parentEventId: 'sale', contract: { returnDirection: 'CUSTOMER', goodsOutcome: 'NO_STOCK' } }),
    ]);
    assert.equal(projected.finance?.sales, '14000.0000');
    assert.equal(projected.continuity.receivable, '14000.0000');
    assert.equal(manufacturingStock([asEvent('credit', 'RETURN')]).length, 0);
  });

  it('16 an unpriced order keeps quantity and has no monetary value', () => {
    const checked = ok({
      eventType: 'CUSTOMER_ORDER',
      title: 'Open qty',
      quantity: '30',
      counterpartyName: 'Buyer',
      occurredAt: when,
    });
    assert.equal(checked.amount, null);
    assert.equal(checked.quantity, '30');
  });

  it('22 unlike units are not added', () => {
    const measured = manufacturingMeasure(
      [
        { ...asEvent('a', 'PRODUCTION'), unit: 'kg', quantity: '10', contract: { entryMode: 'ITEMIZED', acceptedQuantity: '10' } },
        { ...asEvent('b', 'PRODUCTION'), unit: 'pcs', quantity: '4', contract: { entryMode: 'ITEMIZED', acceptedQuantity: '4' } },
      ],
      'PRODUCTION_OUTPUT'
    );
    assert.equal(measured.progress, 0);
    assert.equal(measured.coverage, 'N/A');
  });

  it('24 damage reduces on-hand and is not an expense or a sale', () => {
    const opening = ok({
      eventType: 'STOCK',
      title: 'Resin',
      quantity: '20',
      unit: 'kg',
      contract: { stockAction: 'SET', stockReason: 'opening' },
    });
    const damage = ok({
      eventType: 'STOCK',
      title: 'Resin',
      quantity: '3',
      unit: 'kg',
      contract: { stockAction: 'REDUCE', stockReason: 'damage' },
    });
    const stock = manufacturingStock([
      { ...asEvent('open', 'STOCK'), title: 'Resin', unit: 'kg', quantity: '20', contract: opening.contract },
      { ...asEvent('dmg', 'STOCK'), title: 'Resin', unit: 'kg', quantity: '3', contract: damage.contract },
    ]);
    assert.equal(stock[0]?.onHand, 17);
    const projected = view([
      row('open', 'STOCK', { title: 'Resin', quantity: '20', contract: opening.contract }),
      row('dmg', 'STOCK', { title: 'Resin', quantity: '3', contract: damage.contract }),
    ]);
    assert.equal(projected.finance?.sales, '0.0000');
    assert.equal(projected.finance?.eligibleCost, '0.0000');
  });

  it('25 a cash sale without a buyer is still revenue', () => {
    const checked = ok({
      eventType: 'SALE',
      title: 'Cash sale',
      amount: '500',
      contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
    });
    assert.equal(checked.paymentStatus, 'COLLECTED');
    assert.equal(checked.amount, '500.0000');
  });

  it('a status-only delivery does not invent a quantity', () => {
    const order = { ...asEvent('order', 'CUSTOMER_ORDER'), quantity: '30' };
    const checked = ok(
      {
        eventType: 'DELIVERY',
        title: 'Status',
        quantity: '12',
        parentEventId: 'order',
        contract: { entryMode: 'QUICK', deliveryStatus: 'DISPATCHED' },
      },
      [order]
    );
    assert.equal(checked.quantity, null);
  });

  it('an itemized delivery cannot exceed the open order', () => {
    const order = { ...asEvent('order', 'CUSTOMER_ORDER'), quantity: '30' };
    const checked = validateManufacturingWrite(
      {
        eventType: 'DELIVERY',
        title: 'Too much',
        quantity: '31',
        parentEventId: 'order',
        contract: { entryMode: 'ITEMIZED', deliveryStatus: 'DELIVERED' },
      },
      [order]
    );
    assert.equal(checked.ok, false);
  });
});

function asEvent(id: string, eventType: string): ManufacturingEvent {
  return {
    operatingEventId: id,
    eventType,
    title: eventType,
    amount: null,
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    occurredAt: when,
  };
}
