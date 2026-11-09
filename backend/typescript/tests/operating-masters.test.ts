/**
 * Operating master data — schema contract, uniqueness surface, archive guard,
 * and `line.masterId` resolution against the table instead of a free-text name.
 *
 * These run without a database: they pin the request/response contract and the
 * resolution rules the clients depend on. The DB-backed CRUD round trips live in
 * the `:*db` suite.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  OPERATING_MASTER_KINDS,
  OPERATING_MASTER_STATUSES,
  archiveOperatingMasterSchema,
  createOperatingMasterSchema,
  createPriceListSchema,
  updateOperatingMasterSchema,
  updatePriceListSchema,
} from '../src/modules/business/operating-masters';
import {
  validateTradingWrite,
  type TradingEvent,
  type TradingMasterRefs,
  type TradingWrite,
} from '../src/modules/business/operating-trading-contract';

function write(partial: Partial<TradingWrite> & Pick<TradingWrite, 'eventType' | 'title'>): TradingWrite {
  return { amount: null, ...partial };
}

function refs(entries: Array<[string, Record<string, unknown>]>): TradingMasterRefs {
  return new Map(
    entries.map(([id, ref]) => [
      id,
      {
        id,
        kind: String(ref.kind ?? 'SKU'),
        name: String(ref.name ?? id),
        ...ref,
      } as never,
    ])
  );
}

const noEvents: TradingEvent[] = [];

describe('operating master contract', () => {
  it('accepts every declared kind and rejects an unknown one', () => {
    for (const kind of OPERATING_MASTER_KINDS) {
      const parsed = createOperatingMasterSchema.safeParse({ kind, name: `Row ${kind}` });
      assert.equal(parsed.success, true, `kind ${kind} should be accepted`);
    }
    const bad = createOperatingMasterSchema.safeParse({ kind: 'NOT_A_KIND', name: 'Nope' });
    assert.equal(bad.success, false);
  });

  it('requires a name and rejects unknown fields so a typo cannot silently drop data', () => {
    assert.equal(createOperatingMasterSchema.safeParse({ kind: 'SKU', name: '' }).success, false);
    assert.equal(createOperatingMasterSchema.safeParse({ kind: 'SKU' }).success, false);
    assert.equal(
      createOperatingMasterSchema.safeParse({ kind: 'SKU', name: 'Cup', colour: 'red' }).success,
      false
    );
  });

  it('keeps prices at four decimals and GST percentages at two', () => {
    const ok = createOperatingMasterSchema.safeParse({
      kind: 'SKU',
      name: 'Cup',
      sellPrice: '240.0000',
      mrp: '260.5',
      purchasePrice: '180.1234',
      gstRate: '12.5',
    });
    assert.equal(ok.success, true);
    assert.equal(
      createOperatingMasterSchema.safeParse({ kind: 'SKU', name: 'Cup', sellPrice: '240.00001' }).success,
      false
    );
    assert.equal(
      createOperatingMasterSchema.safeParse({ kind: 'SKU', name: 'Cup', gstRate: '12.555' }).success,
      false
    );
  });

  it('carries the identity fields a dealer, warehouse, or SKU needs', () => {
    const parsed = createOperatingMasterSchema.parse({
      kind: 'DEALER',
      name: 'South Dealer',
      code: 'DL-01',
      gstin: '29ABCDE1234F1Z5',
      billingState: 'Karnataka',
      deliveryState: 'Karnataka',
      parentMasterId: '11111111-1111-4111-8111-111111111111',
    });
    assert.equal(parsed.gstin, '29ABCDE1234F1Z5');
    assert.equal(parsed.parentMasterId, '11111111-1111-4111-8111-111111111111');
  });

  it('requires an expected version to update or archive (optimistic concurrency)', () => {
    assert.equal(
      updateOperatingMasterSchema.safeParse({ name: 'Renamed', expectedVersion: 2 }).success,
      true
    );
    assert.equal(updateOperatingMasterSchema.safeParse({ name: 'Renamed' }).success, false);
    assert.equal(archiveOperatingMasterSchema.safeParse({}).success, false);
    assert.equal(archiveOperatingMasterSchema.safeParse({ expectedVersion: 1 }).success, true);
    assert.equal(OPERATING_MASTER_STATUSES.includes('ARCHIVED'), true);
  });

  it('an effective-dated price list needs a start date and priced lines', () => {
    const parsed = createPriceListSchema.parse({
      name: 'Dealer April',
      validFrom: '2026-04-01',
      validTo: '2026-04-30',
      channel: 'Dealer',
      gstRate: '12',
      lines: [
        { masterId: '11111111-1111-4111-8111-111111111111', price: '210.0000', schemePct: '2.5' },
      ],
    });
    assert.equal(parsed.lines?.length, 1);
    assert.equal(createPriceListSchema.safeParse({ name: 'No dates' }).success, false);
    assert.equal(
      createPriceListSchema.safeParse({
        name: 'Bad line',
        validFrom: '2026-04-01',
        lines: [{ price: '210' }],
      }).success,
      false
    );
  });

  it('a price list update still needs expectedVersion', () => {
    assert.equal(updatePriceListSchema.safeParse({ name: 'x', expectedVersion: 3 }).success, true);
    assert.equal(updatePriceListSchema.safeParse({ name: 'x' }).success, false);
  });
});

describe('line.masterId resolution against the master table', () => {
  it('converts a pack quantity using the table SKU conversion', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Dozen cups',
        channelName: 'Wholesale',
        channelTracking: true,
        lines: [
          { itemName: 'Cup', quantity: '2', unit: 'pack', unitPrice: '120', masterId: 'sku-cup' },
        ],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
        masterRefs: refs([
          ['sku-cup', { kind: 'SKU', name: 'Cup', unit: 'pc', dimension: 'count', conversionToBase: '12' }],
        ]),
      }),
      noEvents
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    assert.equal(checked.quantity, '24.0000');
  });

  it('rejects an incompatible unit against the table SKU', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'By weight',
        channelName: 'Wholesale',
        channelTracking: true,
        lines: [
          { itemName: 'Cup', quantity: '2', unit: 'kg', unitPrice: '10', masterId: 'sku-cup' },
        ],
        contract: { entryMode: 'ITEMIZED', paymentState: 'RECEIVED' },
        masterRefs: refs([
          ['sku-cup', { kind: 'SKU', name: 'Cup', unit: 'pc', dimension: 'count' }],
        ]),
      }),
      noEvents
    );
    assert.equal(checked.ok, false);
  });

  it('rejects a masterId that resolves to nothing rather than trusting a free-text name', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Unknown',
        quantity: '2',
        lines: [{ itemName: 'Unknown', quantity: '2', unit: 'pc', masterId: 'missing' }],
        contract: { stockAction: 'ADD', stockReason: 'stocktake', ownership: 'OWNED' },
        masterRefs: refs([
          ['sku-cup', { kind: 'SKU', name: 'Cup', unit: 'pc', dimension: 'count' }],
        ]),
      }),
      noEvents
    );
    assert.equal(checked.ok, false);
  });

  it('uses the buyer master state to decide intra vs inter state GST', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'SALE',
        title: 'Out of state',
        amount: '1000',
        channelName: 'Wholesale',
        channelTracking: true,
        counterpartyName: 'South Dealer',
        dueAt: '2026-11-30',
        gst: { enabled: true, businessState: 'Karnataka', saleIncludesGst: false, purchaseIncludesGst: false },
        contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER', taxMode: 'EXCLUSIVE', gstRate: '12' },
        masterRefs: refs([
          ['dealer-south', { kind: 'DEALER', name: 'South Dealer', deliveryState: 'Tamil Nadu' }],
        ]),
      }),
      noEvents
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    assert.equal(checked.contract.taxJurisdiction, 'INTER');
    assert.equal(checked.contract.gst?.igstAmount, '120.0000');
    assert.equal(checked.contract.gst?.cgstAmount, '0.0000');
  });

  it('falls back to the event-derived master when the table has no row', () => {
    const legacy: TradingEvent[] = [
      {
        operatingEventId: 'legacy-prod',
        eventType: 'UPDATE',
        title: 'Oil',
        amount: null,
        paymentStatus: 'NONE',
        approvalStatus: 'NONE',
        occurredAt: '2026-10-01T00:00:00.000Z',
        master: { kind: 'PRODUCT', name: 'Oil', unit: 'litre' },
      },
    ];
    const checked = validateTradingWrite(
      write({
        eventType: 'STOCK',
        title: 'Oil',
        quantity: '2',
        lines: [{ itemName: 'Oil', quantity: '2', unit: 'L', masterId: 'legacy-prod' }],
        contract: { stockAction: 'ADD', stockReason: 'opening', ownership: 'OWNED' },
      }),
      legacy
    );
    assert.equal(checked.ok, true);
  });

  it('treats a table SKU as a product for pack conversion', () => {
    const checked = validateTradingWrite(
      write({
        eventType: 'PURCHASE',
        title: 'Buy cartons',
        counterpartyName: 'Mill',
        dueAt: '2026-11-30',
        quantity: '3',
        lines: [
          { itemName: 'Cup', quantity: '3', unit: 'carton', unitPrice: '900', masterId: 'sku-cup' },
        ],
        contract: {
          entryMode: 'ITEMIZED',
          receiptState: 'RECEIVED',
          quantityReceived: '72',
          paymentState: 'PAY_LATER',
          ownership: 'OWNED',
        },
        masterRefs: refs([
          ['sku-cup', { kind: 'SKU', name: 'Cup', unit: 'pc', dimension: 'count', conversionToBase: '24' }],
        ]),
      }),
      noEvents
    );
    assert.equal(checked.ok, true);
    if (!checked.ok) return;
    assert.equal(checked.quantity, '72');
  });
});
