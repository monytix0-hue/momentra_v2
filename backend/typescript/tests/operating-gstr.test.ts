/**
 * GSTR-1 / GSTR-3B read summaries.
 *
 * These are derived from recorded GST snapshots, so the tests pin the derivation rules that
 * matter: which rows contribute, how a B2B/B2C split is decided, that a customer credit note
 * subtracts from outward supply while a supplier return subtracts from input credit, and that a
 * negative net head is reported as a credit rather than clamped to zero.
 *
 * Nothing here files anything. Every summary must carry the "not a return" caveat.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { buildGstr1, buildGstr3b, buildGstrSummary, GSTR_NOTE, type GstrSourceRow } from '../src/modules/business/operating-gstr';
import type { GstSnapshot } from '../src/modules/business/operating-gst';

function snapshot(over: Partial<GstSnapshot> = {}): GstSnapshot {
  const taxableAmount = over.taxableAmount ?? '1000.0000';
  const cgstAmount = over.cgstAmount ?? '90.0000';
  const sgstAmount = over.sgstAmount ?? '90.0000';
  const igstAmount = over.igstAmount ?? '0.0000';
  const gstTotal = over.gstTotal ?? '180.0000';
  const invoiceTotal = over.invoiceTotal ?? '1180.0000';
  return {
    priceIncludesGst: false,
    enteredUnitPrice: taxableAmount,
    quantity: '1.0000',
    discount: '0.0000',
    taxableAmount,
    gstRate: '18.0000',
    cgstAmount,
    sgstAmount,
    igstAmount,
    gstTotal,
    invoiceTotal,
    taxJurisdiction: 'INTRA',
    placeOfSupply: 'Maharashtra',
    placeOfSupplyCode: '27',
    calculationVersion: 'v12.0',
    taxStatus: 'CALCULATED',
    sellerRegistration: 'REGULAR',
    gstLines: [],
    ...over,
  };
}

function row(over: Partial<GstrSourceRow> = {}): GstrSourceRow {
  return {
    eventType: 'SALE',
    approvalStatus: 'NONE',
    paymentStatus: 'PENDING',
    counterpartyHasGstin: true,
    returnDirection: null,
    gst: snapshot(),
    ...over,
  };
}

describe('GSTR-1 outward supply summary', () => {
  it('splits B2B from B2C and intra from inter into separate buckets', () => {
    const summary = buildGstr1(
      [
        row({ counterpartyHasGstin: true }),
        row({
          counterpartyHasGstin: false,
          gst: snapshot({ taxJurisdiction: 'INTER', cgstAmount: '0.0000', sgstAmount: '0.0000', igstAmount: '180.0000' }),
        }),
        row({
          counterpartyHasGstin: false,
          gst: snapshot({ placeOfSupply: 'Karnataka', placeOfSupplyCode: '29' }),
        }),
      ],
      '2026-10'
    );

    assert.equal(summary.month, '2026-10');
    // One bucket per (counterparty type, jurisdiction, place of supply) triple.
    assert.equal(summary.outward.length, 3);
    assert.equal(summary.totals.invoiceCount, 3);
    assert.equal(summary.totals.taxableValue, '3000.0000');
    assert.equal(summary.totals.cgst, '180.0000');
    assert.equal(summary.totals.sgst, '180.0000');
    assert.equal(summary.totals.igst, '180.0000');
    assert.equal(summary.totals.totalTax, '540.0000');
    assert.equal(summary.totals.invoiceValue, '3540.0000');

    // B2B sorts before B2C.
    assert.equal(summary.outward[0].counterpartyType, 'B2B');
    assert.equal(summary.outward[0].jurisdiction, 'INTRA');
    assert.equal(summary.outward[0].placeOfSupplyCode, '27');
    assert.equal(summary.outward[0].invoiceCount, 1);

    const b2c = summary.outward[1];
    assert.equal(b2c.counterpartyType, 'B2C');
    assert.equal(b2c.invoiceCount, 1);
    assert.equal(b2c.placeOfSupplyCode, '27');
    assert.equal(b2c.taxableValue, '1000.0000');

    // Same counterparty type and jurisdiction, different place of supply: a separate bucket.
    const b2cKarnataka = summary.outward[2];
    assert.equal(b2cKarnataka.counterpartyType, 'B2C');
    assert.equal(b2cKarnataka.placeOfSupplyCode, '29');
    assert.equal(b2cKarnataka.jurisdiction, 'INTRA');

    const inter = buildGstr1(
      [row({ gst: snapshot({ taxJurisdiction: 'INTER', cgstAmount: '0.0000', sgstAmount: '0.0000', igstAmount: '180.0000' }) })],
      '2026-10'
    );
    assert.equal(inter.outward[0].jurisdiction, 'INTER');
    assert.equal(inter.outward[0].igst, '180.0000');
    assert.equal(inter.outward[0].cgst, '0.0000');
  });

  it('subtracts a customer credit note from outward supply', () => {
    const summary = buildGstr1(
      [
        row(),
        row({ eventType: 'RETURN', returnDirection: 'CUSTOMER', gst: snapshot({ taxableAmount: '400.0000', cgstAmount: '36.0000', sgstAmount: '36.0000', gstTotal: '72.0000', invoiceTotal: '472.0000' }) }),
      ],
      '2026-10'
    );
    assert.equal(summary.totals.invoiceCount, 0);
    assert.equal(summary.totals.taxableValue, '600.0000');
    assert.equal(summary.totals.cgst, '54.0000');
    assert.equal(summary.totals.totalTax, '108.0000');
    assert.equal(summary.outward[0].invoiceCount, 0);
  });

  it('ignores a supplier-direction return, which is an input-credit adjustment', () => {
    const summary = buildGstr1([row({ eventType: 'RETURN', returnDirection: 'SUPPLIER' })], '2026-10');
    assert.equal(summary.outward.length, 0);
    assert.equal(summary.totals.invoiceCount, 0);
    assert.equal(summary.totals.taxableValue, '0.0000');
  });

  it('excludes pending, rejected, cancelled, and not-configured rows', () => {
    const summary = buildGstr1(
      [
        row({ approvalStatus: 'PENDING' }),
        row({ approvalStatus: 'REJECTED' }),
        row({ paymentStatus: 'CANCELLED' }),
        row({ gst: null }),
        row({ gst: snapshot({ taxStatus: 'NOT_CONFIGURED', cgstAmount: '0.0000', sgstAmount: '0.0000', gstTotal: '0.0000' }) }),
        row({ eventType: 'PAYMENT' }),
        row(),
      ],
      '2026-10'
    );
    assert.equal(summary.totals.invoiceCount, 1);
    assert.equal(summary.totals.taxableValue, '1000.0000');
  });

  it('treats a snapshot without a stored code as UNKNOWN jurisdiction and derives the code from the name', () => {
    const summary = buildGstr1(
      [row({ gst: snapshot({ taxJurisdiction: null, placeOfSupply: 'Karnataka', placeOfSupplyCode: undefined }) })],
      '2026-10'
    );
    assert.equal(summary.outward[0].jurisdiction, 'UNKNOWN');
    assert.equal(summary.outward[0].placeOfSupplyCode, '29');
  });

  it('always carries the not-a-return caveat', () => {
    const summary = buildGstr1([row()], '2026-10');
    assert.equal(summary.note, GSTR_NOTE);
    assert.match(summary.note, /not a GST return/i);
  });
});

describe('GSTR-3B net liability summary', () => {
  it('nets input credit from purchases against output tax', () => {
    const summary = buildGstr3b(
      [
        row(),
        row({
          eventType: 'PURCHASE',
          gst: snapshot({ taxableAmount: '500.0000', cgstAmount: '45.0000', sgstAmount: '45.0000', gstTotal: '90.0000', invoiceTotal: '590.0000' }),
        }),
      ],
      '2026-10'
    );
    assert.equal(summary.outwardTaxable.taxableValue, '1000.0000');
    assert.equal(summary.outwardTaxable.totalTax, '180.0000');
    assert.equal(summary.inputTaxCredit.taxableValue, '500.0000');
    assert.equal(summary.inputTaxCredit.totalTax, '90.0000');
    assert.equal(summary.netLiability.cgst, '45.0000');
    assert.equal(summary.netLiability.sgst, '45.0000');
    assert.equal(summary.netLiability.igst, '0.0000');
    assert.equal(summary.netLiability.total, '90.0000');
    assert.equal(summary.invoiceCount, 1);
  });

  it('reports a negative head as credit carried forward rather than clamping it to zero', () => {
    const summary = buildGstr3b(
      [
        row({ gst: snapshot({ taxableAmount: '100.0000', cgstAmount: '0.0000', sgstAmount: '0.0000', igstAmount: '18.0000', gstTotal: '18.0000', invoiceTotal: '118.0000', taxJurisdiction: 'INTER' }) }),
        row({ eventType: 'PURCHASE', gst: snapshot({ cgstAmount: '90.0000', sgstAmount: '90.0000', gstTotal: '180.0000' }) }),
      ],
      '2026-10'
    );
    assert.equal(summary.netLiability.cgst, '-90.0000');
    assert.equal(summary.netLiability.sgst, '-90.0000');
    assert.equal(summary.netLiability.igst, '18.0000');
    assert.equal(summary.netLiability.total, '-162.0000');
  });

  it('reduces input credit on a supplier return and output tax on a customer return', () => {
    const summary = buildGstr3b(
      [
        row(),
        row({ eventType: 'PURCHASE', gst: snapshot({ cgstAmount: '9.0000', sgstAmount: '9.0000', gstTotal: '18.0000' }) }),
        row({ eventType: 'RETURN', returnDirection: 'SUPPLIER', gst: snapshot({ cgstAmount: '9.0000', sgstAmount: '9.0000', gstTotal: '18.0000' }) }),
        row({ eventType: 'RETURN', returnDirection: 'CUSTOMER', gst: snapshot({ cgstAmount: '9.0000', sgstAmount: '9.0000', gstTotal: '18.0000' }) }),
      ],
      '2026-10'
    );
    // Output 180 - 18 = 162; ITC 18 - 18 = 0.
    assert.equal(summary.outwardTaxable.totalTax, '162.0000');
    assert.equal(summary.inputTaxCredit.totalTax, '0.0000');
    assert.equal(summary.netLiability.total, '162.0000');
  });

  it('returns zeroed heads and no invoices for a month with no records', () => {
    const summary = buildGstr3b([], '2026-09');
    assert.equal(summary.month, '2026-09');
    assert.equal(summary.invoiceCount, 0);
    assert.equal(summary.netLiability.total, '0.0000');
    assert.equal(summary.note, GSTR_NOTE);
  });
});

describe('buildGstrSummary', () => {
  it('returns both summaries and the shared caveat for one month', () => {
    const summary = buildGstrSummary([row()], '2026-10');
    assert.equal(summary.note, GSTR_NOTE);
    assert.equal(summary.gstr1.month, '2026-10');
    assert.equal(summary.gstr3b.month, '2026-10');
    assert.equal(summary.gstr1.totals.totalTax, summary.gstr3b.outwardTaxable.totalTax);
  });
});
