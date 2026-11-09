/**
 * GSTR summaries derived from recorded GST snapshots.
 *
 * This is a read model, not a return. It walks the same posted `GstSnapshot`s that
 * `accumulateGstPosition` walks and buckets them the way GSTR-1 (outward supplies) and GSTR-3B
 * (net liability) group them, so an operator can see what a filing would contain.
 *
 * Nothing here is filed, signed, or transmitted. The `note` on every summary says so, because a
 * number that looks like a return is worse than no number at all.
 */

import type { GstSnapshot } from './operating-gst';
import { gstStateCodeOf } from './operating-gst-states';

/** The caveat printed next to every derived figure. */
export const GSTR_NOTE =
  'Derived from records saved in this Moment. This is not a GST return, is not filed with the ' +
  'government, and is not a verified amount payable.';

export interface GstrBucketLine {
  /** `B2B` when the counterparty has a GSTIN, `B2C` otherwise. */
  counterpartyType: 'B2B' | 'B2C';
  /** `INTRA` (CGST+SGST) or `INTER` (IGST). */
  jurisdiction: 'INTRA' | 'INTER' | 'UNKNOWN';
  /** Two-digit place-of-supply code, or `''` when the snapshot predates v12.0. */
  placeOfSupplyCode: string;
  /** The human place of supply for the row, when it was recorded. */
  placeOfSupply: string;
  invoiceCount: number;
  taxableValue: string;
  cgst: string;
  sgst: string;
  igst: string;
  totalTax: string;
  invoiceValue: string;
}

export interface Gstr1Summary {
  month: string;
  /** Outward supplies, grouped by counterparty type and jurisdiction. */
  outward: GstrBucketLine[];
  totals: {
    invoiceCount: number;
    taxableValue: string;
    cgst: string;
    sgst: string;
    igst: string;
    totalTax: string;
    invoiceValue: string;
  };
  note: string;
}

export interface Gstr3bSummary {
  month: string;
  /** 3.1(a) outward taxable supplies. */
  outwardTaxable: {
    taxableValue: string;
    cgst: string;
    sgst: string;
    igst: string;
    totalTax: string;
  };
  /** Eligible input tax credit from recorded purchases. */
  inputTaxCredit: {
    taxableValue: string;
    cgst: string;
    sgst: string;
    igst: string;
    totalTax: string;
  };
  /**
   * Output tax minus eligible ITC, per head. A negative head is a credit carried forward, which
   * is why it is not clamped to zero.
   */
  netLiability: {
    cgst: string;
    sgst: string;
    igst: string;
    total: string;
  };
  invoiceCount: number;
  note: string;
}

export interface GstrSourceRow {
  eventType: string;
  approvalStatus: string;
  paymentStatus: string;
  /** Whether the counterparty master carries a GSTIN. */
  counterpartyHasGstin?: boolean;
  returnDirection?: string | null;
  gst: GstSnapshot | null | undefined;
}

/** Rupees string → paise integer, matching `operating-gst.ts`. */
function readPaise(raw: string | null | undefined): number {
  const text = String(raw ?? '').trim();
  if (!text) return 0;
  const negative = text.startsWith('-');
  const [whole = '0', frac = ''] = text.replace('-', '').split('.');
  const head = Number(whole || '0') * 100 + Number((frac + '00').slice(0, 2));
  return negative ? -head : head;
}

/** Paise integer → the four-decimal money string the rest of the module uses. */
function moneyFromPaise(paise: number): string {
  const rounded = Math.round(paise);
  const negative = rounded < 0;
  const abs = Math.abs(rounded);
  const whole = Math.floor(abs / 100);
  const frac = String(abs % 100).padStart(2, '0');
  return `${negative ? '-' : ''}${whole}.${frac}00`;
}

/** Only posted, non-rejected, non-cancelled rows contribute — the same gate the position uses. */
function countsTowardReturn(row: GstrSourceRow): boolean {
  if (row.approvalStatus === 'PENDING' || row.approvalStatus === 'REJECTED') return false;
  if (row.paymentStatus === 'CANCELLED') return false;
  if (!row.gst || row.gst.taxStatus === 'NOT_CONFIGURED') return false;
  return true;
}

const OUTWARD_TYPES = new Set(['SALE', 'SERVICE', 'INVOICE']);
const INWARD_TYPES = new Set(['PURCHASE', 'SPEND']);

/**
 * GSTR-1 style outward supply summary for one month (`YYYY-MM`).
 *
 * A RETURN whose direction is the customer's is an outward credit note and reduces the bucket;
 * a supplier-direction return reduces input credit instead and is handled by `gstr3b`.
 */
export function buildGstr1(rows: GstrSourceRow[], month: string): Gstr1Summary {
  const buckets = new Map<string, GstrBucketLine>();
  let invoiceCount = 0;
  let taxable = 0;
  let cgst = 0;
  let sgst = 0;
  let igst = 0;
  let totalTax = 0;
  let invoiceValue = 0;

  for (const row of rows) {
    if (!countsTowardReturn(row)) continue;
    const isOutward = OUTWARD_TYPES.has(row.eventType) ||
      (row.eventType === 'RETURN' && row.returnDirection !== 'SUPPLIER');
    if (!isOutward) continue;

    // A return against a sale is a credit note: it subtracts from the outward bucket.
    const sign = row.eventType === 'RETURN' ? -1 : 1;
    const gst = row.gst!;
    const jurisdiction: GstrBucketLine['jurisdiction'] =
      gst.taxJurisdiction === 'INTRA' || gst.taxJurisdiction === 'INTER' ? gst.taxJurisdiction : 'UNKNOWN';
    const counterpartyType: 'B2B' | 'B2C' = row.counterpartyHasGstin ? 'B2B' : 'B2C';
    const code = gst.placeOfSupplyCode || gstStateCodeOf(gst.placeOfSupply);
    const key = `${counterpartyType}|${jurisdiction}|${code}`;

    const bucket = buckets.get(key) ?? {
      counterpartyType,
      jurisdiction,
      placeOfSupplyCode: code,
      placeOfSupply: gst.placeOfSupply || '',
      invoiceCount: 0,
      taxableValue: '0.0000',
      cgst: '0.0000',
      sgst: '0.0000',
      igst: '0.0000',
      totalTax: '0.0000',
      invoiceValue: '0.0000',
    };

    const bTaxable = readPaise(bucket.taxableValue) + sign * readPaise(gst.taxableAmount);
    const bCgst = readPaise(bucket.cgst) + sign * readPaise(gst.cgstAmount);
    const bSgst = readPaise(bucket.sgst) + sign * readPaise(gst.sgstAmount);
    const bIgst = readPaise(bucket.igst) + sign * readPaise(gst.igstAmount);
    const bTax = readPaise(bucket.totalTax) + sign * readPaise(gst.gstTotal);
    const bInvoice = readPaise(bucket.invoiceValue) + sign * readPaise(gst.invoiceTotal);

    bucket.invoiceCount += sign;
    bucket.taxableValue = moneyFromPaise(bTaxable);
    bucket.cgst = moneyFromPaise(bCgst);
    bucket.sgst = moneyFromPaise(bSgst);
    bucket.igst = moneyFromPaise(bIgst);
    bucket.totalTax = moneyFromPaise(bTax);
    bucket.invoiceValue = moneyFromPaise(bInvoice);
    buckets.set(key, bucket);

    invoiceCount += sign;
    taxable += sign * readPaise(gst.taxableAmount);
    cgst += sign * readPaise(gst.cgstAmount);
    sgst += sign * readPaise(gst.sgstAmount);
    igst += sign * readPaise(gst.igstAmount);
    totalTax += sign * readPaise(gst.gstTotal);
    invoiceValue += sign * readPaise(gst.invoiceTotal);
  }

  return {
    month,
    outward: [...buckets.values()].sort((a, b) => {
      if (a.counterpartyType !== b.counterpartyType) return a.counterpartyType === 'B2B' ? -1 : 1;
      if (a.placeOfSupplyCode !== b.placeOfSupplyCode) {
        return a.placeOfSupplyCode.localeCompare(b.placeOfSupplyCode);
      }
      return a.jurisdiction.localeCompare(b.jurisdiction);
    }),
    totals: {
      invoiceCount,
      taxableValue: moneyFromPaise(taxable),
      cgst: moneyFromPaise(cgst),
      sgst: moneyFromPaise(sgst),
      igst: moneyFromPaise(igst),
      totalTax: moneyFromPaise(totalTax),
      invoiceValue: moneyFromPaise(invoiceValue),
    },
    note: GSTR_NOTE,
  };
}

/**
 * GSTR-3B style net liability for one month.
 *
 * Output tax comes from outward supplies; eligible ITC comes from recorded purchases and
 * supplier-direction returns. The net per head is deliberately not clamped: a negative IGST head
 * is a credit the operator carries, and printing it as zero would hide that.
 */
export function buildGstr3b(rows: GstrSourceRow[], month: string): Gstr3bSummary {
  let outTaxable = 0;
  let outCgst = 0;
  let outSgst = 0;
  let outIgst = 0;
  let outTax = 0;

  let itcTaxable = 0;
  let itcCgst = 0;
  let itcSgst = 0;
  let itcIgst = 0;
  let itcTax = 0;

  let invoiceCount = 0;

  for (const row of rows) {
    if (!countsTowardReturn(row)) continue;
    const gst = row.gst!;

    const isOutward = OUTWARD_TYPES.has(row.eventType) ||
      (row.eventType === 'RETURN' && row.returnDirection !== 'SUPPLIER');
    const isInward = INWARD_TYPES.has(row.eventType) ||
      (row.eventType === 'RETURN' && row.returnDirection === 'SUPPLIER');

    if (isOutward) {
      const sign = row.eventType === 'RETURN' ? -1 : 1;
      outTaxable += sign * readPaise(gst.taxableAmount);
      outCgst += sign * readPaise(gst.cgstAmount);
      outSgst += sign * readPaise(gst.sgstAmount);
      outIgst += sign * readPaise(gst.igstAmount);
      outTax += sign * readPaise(gst.gstTotal);
      if (sign > 0) invoiceCount += 1;
    } else if (isInward) {
      const sign = row.eventType === 'RETURN' ? -1 : 1;
      itcTaxable += sign * readPaise(gst.taxableAmount);
      itcCgst += sign * readPaise(gst.cgstAmount);
      itcSgst += sign * readPaise(gst.sgstAmount);
      itcIgst += sign * readPaise(gst.igstAmount);
      itcTax += sign * readPaise(gst.gstTotal);
    }
  }

  const netCgst = outCgst - itcCgst;
  const netSgst = outSgst - itcSgst;
  const netIgst = outIgst - itcIgst;

  return {
    month,
    outwardTaxable: {
      taxableValue: moneyFromPaise(outTaxable),
      cgst: moneyFromPaise(outCgst),
      sgst: moneyFromPaise(outSgst),
      igst: moneyFromPaise(outIgst),
      totalTax: moneyFromPaise(outTax),
    },
    inputTaxCredit: {
      taxableValue: moneyFromPaise(itcTaxable),
      cgst: moneyFromPaise(itcCgst),
      sgst: moneyFromPaise(itcSgst),
      igst: moneyFromPaise(itcIgst),
      totalTax: moneyFromPaise(itcTax),
    },
    netLiability: {
      cgst: moneyFromPaise(netCgst),
      sgst: moneyFromPaise(netSgst),
      igst: moneyFromPaise(netIgst),
      total: moneyFromPaise(netCgst + netSgst + netIgst),
    },
    invoiceCount,
    note: GSTR_NOTE,
  };
}

export interface GstrSummary {
  gstr1: Gstr1Summary;
  gstr3b: Gstr3bSummary;
  note: string;
}

/** Both summaries in one payload, which is what the returns endpoint returns. */
export function buildGstrSummary(rows: GstrSourceRow[], month: string): GstrSummary {
  return {
    gstr1: buildGstr1(rows, month),
    gstr3b: buildGstr3b(rows, month),
    note: GSTR_NOTE,
  };
}
