import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { composeManufacturingStory } from '../src/modules/story/manufacturing-story';
import type { ManufacturingEvent } from '../src/modules/business/operating-manufacturing';

const when = '2026-10-04T00:00:00.000Z';

function row(id: string, eventType: string, fields: Partial<ManufacturingEvent> = {}): ManufacturingEvent {
  return {
    operatingEventId: id,
    eventType,
    title: eventType,
    amount: null,
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    occurredAt: when,
    ...fields,
  };
}

function story(subtype: string, events: ManufacturingEvent[]) {
  return composeManufacturingStory({
    momentId: 'm',
    title: 'Plant',
    subtype,
    currency: 'INR',
    startAt: null,
    endAt: null,
    channels: [],
    participants: [],
    events,
  });
}

function page(snapshot: ReturnType<typeof story>, id: string): string {
  return snapshot.highlights.find((item) => item.id === id)?.detail ?? '';
}

describe('manufacturing story fixtures', () => {
  it('S1 quick output records units and does not claim acceptance, yield, or finished stock', () => {
    const snap = story('STANDARD_PRODUCTION', [row('made', 'PRODUCTION', { quantity: '120', unit: 'pcs', contract: { entryMode: 'QUICK' } })]);
    const output = page(snap, 'mf-output');
    assert.match(output, /Recorded output 120/);
    assert.match(output, /Acceptance not recorded/);
    assert.match(output, /Yield not recorded/);
    assert.match(output, /Finished stock was not increased/);
  });

  it('S2 itemized acceptance and rejection publish yield on one basis', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('made', 'PRODUCTION', {
        title: 'Bracket',
        quantity: '105',
        unit: 'pcs',
        rejectedQuantity: '5',
        contract: { entryMode: 'ITEMIZED', acceptedQuantity: '100' },
      }),
    ]);
    assert.equal(snap.metrics.accepted, '100');
    assert.equal(snap.metrics.yield, '95.24');
  });

  it('M1 an order is a commitment and not sales or a receivable', () => {
    const snap = story('MADE_TO_ORDER', [row('order', 'CUSTOMER_ORDER', { amount: '50000', quantity: '10', counterpartyName: 'Buyer' })]);
    assert.equal(snap.metrics.sales, 'not recorded');
    assert.equal(snap.metrics.orderValue, '50000');
    assert.equal(snap.metrics.receivable, 'not recorded');
    assert.match(page(snap, 'mf-money'), /not a receivable/);
  });

  it('M2 an itemized delivery reports delivered, pending, and that line only', () => {
    const snap = story('MADE_TO_ORDER', [
      row('order', 'CUSTOMER_ORDER', { quantity: '50', unit: 'pcs' }),
      row('drop', 'DELIVERY', { quantity: '30', unit: 'pcs', parentEventId: 'order', contract: { entryMode: 'ITEMIZED', deliveryStatus: 'DELIVERED' } }),
    ]);
    const materials = page(snap, 'mf-materials');
    assert.match(materials, /Delivered 30/);
    assert.match(materials, /Pending 20/);
    assert.match(materials, /60%/);
  });

  it('F1 receipts and a later collection leave the open sale', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('sale', 'SALE', { amount: '30000', paymentStatus: 'PENDING' }),
      row('part', 'COLLECTION', { amount: '10000', parentEventId: 'sale', paymentStatus: 'COLLECTED' }),
      row('later', 'COLLECTION', { amount: '5000', parentEventId: 'sale', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(snap.metrics.sales, '30000');
    assert.equal(snap.metrics.receipts, '15000');
    assert.equal(snap.metrics.receivable, '15000');
  });

  it('F2 a partial receipt stocks only what arrived', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('buy', 'PURCHASE', {
        amount: '20000',
        title: 'Resin',
        lines: [{ itemName: 'Resin', quantity: '20', unit: 'kg', unitPrice: '1000' }],
        contract: { entryMode: 'ITEMIZED', receiptState: 'PART_RECEIVED', quantityReceived: '10' },
      }),
    ]);
    assert.match(page(snap, 'mf-materials'), /1 material lines from receipts/);
  });

  it('F3 a credit reduces net sales and does not restock', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('sale', 'SALE', { amount: '15000', paymentStatus: 'PENDING' }),
      row('credit', 'RETURN', { amount: '3000', parentEventId: 'sale', contract: { returnDirection: 'CUSTOMER', goodsOutcome: 'NO_STOCK' } }),
    ]);
    assert.equal(snap.metrics.sales, '12000');
    assert.equal(snap.metrics.receivable, '12000');
    assert.match(page(snap, 'mf-materials'), /Finished stock not recorded/);
  });

  it('ST01 and ST02 use standard and made-to-order page copy', () => {
    const standard = story('STANDARD_PRODUCTION', []);
    const made = story('MADE_TO_ORDER', []);
    assert.match(page(standard, 'mf-moment'), /Standard production/);
    assert.match(page(standard, 'mf-output'), /Production comes first/);
    assert.match(page(made, 'mf-moment'), /Made-to-order/);
    assert.match(page(made, 'mf-output'), /Orders come first/);
    assert.match(page(made, 'mf-materials'), /Order fulfilment/);
  });

  it('ST07 a status-only delivery has no quantity', () => {
    const snap = story('MADE_TO_ORDER', [
      row('order', 'CUSTOMER_ORDER', { quantity: '30' }),
      row('drop', 'DELIVERY', { parentEventId: 'order', contract: { entryMode: 'QUICK', deliveryStatus: 'DISPATCHED' } }),
    ]);
    assert.equal(snap.metrics.delivered, 'not recorded');
    assert.match(page(snap, 'mf-materials'), /status-only/);
  });

  it('ST08 a quick purchase does not create stock', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('buy', 'PURCHASE', { amount: '20000', contract: { entryMode: 'QUICK', receiptState: 'AWAITING' } }),
    ]);
    assert.match(page(snap, 'mf-materials'), /Material receipt not recorded/);
  });

  it('ST18 an undated open balance is not overdue', () => {
    const snap = story('STANDARD_PRODUCTION', [row('sale', 'SALE', { amount: '1000', paymentStatus: 'PENDING' })]);
    assert.match(page(snap, 'mf-attention'), /not overdue/);
  });

  it('ST19 unlike units are not totaled', () => {
    const snap = story('STANDARD_PRODUCTION', [
      row('a', 'PRODUCTION', { unit: 'kg', quantity: '10', contract: { entryMode: 'ITEMIZED', acceptedQuantity: '10' } }),
      row('b', 'PRODUCTION', { unit: 'pcs', quantity: '4', contract: { entryMode: 'ITEMIZED', acceptedQuantity: '4' } }),
    ]);
    assert.equal(snap.metrics.accepted, 'not recorded');
    assert.equal(snap.metrics.yield, 'not recorded');
    assert.match(page(snap, 'mf-attention'), /Unlike units were not added/);
  });

  it('ST21 no GST field means no GST amount', () => {
    const snap = story('STANDARD_PRODUCTION', [row('sale', 'SALE', { amount: '1000', paymentStatus: 'COLLECTED' })]);
    assert.match(page(snap, 'mf-money'), /No GST is recorded/);
    assert.equal(snap.narrative.insights.some((line) => /GST/.test(line)), true);
  });
});
