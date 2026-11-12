import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { resolveExplicitCess, resolveLineDiscounts, stampPostedGst } from '../src/modules/business/operating-gst.ts';
import { parseOperatingContract, validateRetailWrite } from '../src/modules/business/operating-retail-contract.ts';

const gst = {
  enabled: true,
  saleIncludesGst: true as boolean | null,
  purchaseIncludesGst: true as boolean | null,
  businessState: 'Maharashtra',
  businessStateCode: '27',
  registration: 'REGULAR',
};

describe('POS discount + split tender + explicit cess', () => {
  it('distributes header discount across itemized lines', () => {
    const resolved = resolveLineDiscounts({
      lineGrosses: [100, 100],
      lineDiscounts: [0, 0],
      headerDiscount: 20,
    });
    assert.equal(resolved.ok, true);
    if (!resolved.ok) return;
    assert.equal(resolved.discounts[0], 10);
    assert.equal(resolved.discounts[1], 10);
  });

  it('rejects discount above gross', () => {
    const resolved = resolveLineDiscounts({
      lineGrosses: [50],
      lineDiscounts: [0],
      headerDiscount: 60,
    });
    assert.equal(resolved.ok, false);
  });

  it('computes explicit cess from rate on taxable', () => {
    const cess = resolveExplicitCess({ taxable: 1000, cessRate: '5' });
    assert.equal(cess.ok, true);
    if (!cess.ok) return;
    assert.equal(cess.cessPaise, 5000);
    assert.equal(cess.rateStored, '5.0000');
  });

  it('prefers explicit cess amount over rate', () => {
    const cess = resolveExplicitCess({ taxable: 1000, cessAmount: '12.5', cessRate: '5' });
    assert.equal(cess.ok, true);
    if (!cess.ok) return;
    assert.equal(cess.cessPaise, 1250);
    assert.equal(cess.rateStored, '');
  });

  it('stamps inclusive GST with document discount and cess amount', () => {
    const contract = {
      taxMode: 'INCLUSIVE' as const,
      gstRate: '5',
      discount: '50',
      cessAmount: '10',
      taxJurisdiction: 'INTRA' as const,
      placeOfSupply: 'Maharashtra',
    };
    const stamped = stampPostedGst({
      gst,
      side: 'SALE',
      entered: 1050,
      title: 'Quick',
      lines: null,
      contract,
    });
    assert.equal(stamped.ok, true);
    if (!stamped.ok) return;
    assert.equal(contract.gst?.discount, '50.0000');
    assert.equal(contract.gst?.cessAmount, '10.0000');
    assert.equal(stamped.amount, Number(contract.gst?.invoiceTotal));
    assert.ok(stamped.amount < 1050);
  });

  it('accepts split tender summing to received amount', () => {
    const result = validateRetailWrite(
      {
        eventType: 'SALE',
        title: 'Split',
        amount: '1000',
        channelName: 'Counter',
        contract: {
          entryMode: 'QUICK',
          paymentState: 'RECEIVED',
          payments: [
            { method: 'UPI', amount: '600' },
            { method: 'Cash', amount: '400' },
          ],
        },
      },
      []
    );
    assert.equal(result.ok, true);
    if (!result.ok) return;
    assert.equal(result.contract.payments?.length, 2);
    assert.equal(result.contract.payments?.[0]?.method, 'UPI');
    assert.equal(result.amount, '1000.0000');
  });

  it('rejects split tender that does not sum to payable', () => {
    const result = validateRetailWrite(
      {
        eventType: 'SALE',
        title: 'Bad split',
        amount: '1000',
        channelName: 'Counter',
        contract: {
          entryMode: 'QUICK',
          paymentState: 'RECEIVED',
          payments: [
            { method: 'UPI', amount: '600' },
            { method: 'Cash', amount: '300' },
          ],
        },
      },
      []
    );
    assert.equal(result.ok, false);
  });

  it('rejects tender instruments on PAY_LATER', () => {
    const result = validateRetailWrite(
      {
        eventType: 'SALE',
        title: 'AR',
        amount: '500',
        channelName: 'Cloud',
        counterpartyName: 'Zomato',
        contract: {
          entryMode: 'QUICK',
          paymentState: 'PAY_LATER',
          payments: [{ method: 'UPI', amount: '500' }],
        },
      },
      []
    );
    assert.equal(result.ok, false);
  });

  it('applies discount without GST tracking', () => {
    const result = validateRetailWrite(
      {
        eventType: 'SALE',
        title: 'Plain discount',
        amount: '1000',
        channelName: 'Counter',
        contract: {
          entryMode: 'QUICK',
          paymentState: 'RECEIVED',
          discount: '100',
          payments: [{ method: 'Cash', amount: '900' }],
        },
      },
      []
    );
    assert.equal(result.ok, true);
    if (!result.ok) return;
    assert.equal(result.amount, '900.0000');
    assert.equal(result.contract.receivedAmount, '900.0000');
  });

  it('parses payments and cess on the operating contract', () => {
    const parsed = parseOperatingContract({
      entryMode: 'QUICK',
      paymentState: 'RECEIVED',
      discount: '25',
      cessAmount: '5',
      payments: [
        { method: 'Card', amount: '80' },
        { method: 'UPI', amount: '20' },
      ],
    });
    assert.ok(parsed);
    assert.equal(parsed?.discount, '25');
    assert.equal(parsed?.cessAmount, '5');
    assert.equal(parsed?.payments?.length, 2);
  });

  it('distributes discount on itemized SALE with GST', () => {
    const result = validateRetailWrite(
      {
        eventType: 'SALE',
        title: 'Itemized discount',
        amount: null,
        channelName: 'Counter',
        gst,
        contract: {
          entryMode: 'ITEMIZED',
          paymentState: 'RECEIVED',
          taxMode: 'EXCLUSIVE',
          gstRate: '5',
          discount: '100',
          taxJurisdiction: 'INTRA',
          placeOfSupply: 'Maharashtra',
          payments: [{ method: 'UPI', amount: '945' }],
        },
        lines: [
          { itemName: 'A', quantity: '1', unit: 'pc', unitPrice: '500', gstRate: '5' },
          { itemName: 'B', quantity: '1', unit: 'pc', unitPrice: '500', gstRate: '5' },
        ],
      },
      []
    );
    assert.equal(result.ok, true);
    if (!result.ok) return;
    // taxable 900 @ 5% = 45 → invoice 945
    assert.equal(result.amount, '945.0000');
    assert.equal(result.contract.gst?.discount, '100.0000');
  });
});
