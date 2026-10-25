/**
 * Trading and Distribution GST snapshots. Historical invoices keep the snapshot
 * written at posting time. Later master or setup changes do not rewrite it.
 */

export const GST_CALCULATION_VERSION = 'v11.1';

export interface GstLineSnapshot {
  enteredUnitPrice: string;
  quantity: string;
  discount: string;
  priceIncludesGst: boolean;
  gstRate: string;
  taxableAmount: string;
  cgstAmount: string;
  sgstAmount: string;
  igstAmount: string;
  gstTotal: string;
  lineTotal: string;
  placeOfSupply: string;
}

export interface GstSnapshot {
  priceIncludesGst: boolean;
  enteredUnitPrice: string;
  quantity: string;
  discount: string;
  taxableAmount: string;
  gstRate: string;
  cgstAmount: string;
  sgstAmount: string;
  igstAmount: string;
  gstTotal: string;
  invoiceTotal: string;
  taxJurisdiction: 'INTRA' | 'INTER' | null;
  placeOfSupply: string;
  calculationVersion: string;
  taxStatus: 'CALCULATED' | 'NOT_CONFIGURED';
  sellerRegistration: string;
  gstLines: GstLineSnapshot[];
}

export interface GstPositionSource {
  id: string;
  title: string;
  eventType: string;
  occurredAt: string;
  taxableAmount: string;
  gstTotal: string;
  direction: 'OUTPUT' | 'INPUT' | 'ADJUSTMENT';
}

export interface GstPosition {
  outputGst: string;
  inputGst: string;
  difference: string;
  cgstSales: string;
  sgstSales: string;
  igstSales: string;
  cgstPurchases: string;
  sgstPurchases: string;
  igstPurchases: string;
  adjustmentGst: string;
  asOfMonth: string;
  sources: GstPositionSource[];
}

export interface GstLineInput {
  quantity: number;
  unitPrice: number;
  discount?: number;
  ratePercent: number;
  inclusive: boolean;
}

function rupeesToPaise(value: number): number {
  if (!Number.isFinite(value)) return 0;
  const negative = value < 0;
  const paise = Math.floor(Math.abs(value) * 100 + 0.5 + 1e-8);
  return negative ? -paise : paise;
}

function moneyFromPaise(paise: number): string {
  const rounded = Math.round(paise);
  const negative = rounded < 0;
  const abs = Math.abs(rounded);
  const whole = Math.floor(abs / 100);
  const frac = String(abs % 100).padStart(2, '0');
  return `${negative ? '-' : ''}${whole}.${frac}00`;
}

function readPaise(raw: string): number {
  const text = raw.trim();
  if (!text) return 0;
  const negative = text.startsWith('-');
  const [whole, frac = ''] = text.replace('-', '').split('.');
  const head = Number(whole || '0') * 100 + Number((frac + '00').slice(0, 2));
  return negative ? -head : head;
}

function mulDivHalfUp(value: number, numerator: number, denominator: number): number {
  if (!denominator) return 0;
  const negative = value < 0;
  const product = Math.abs(Math.round(value)) * numerator;
  const result = Math.floor((product + Math.floor(denominator / 2)) / denominator);
  return negative ? -result : result;
}

function money(value: number): string {
  return moneyFromPaise(rupeesToPaise(value));
}

function splitHeads(gstPaise: number, jurisdiction: 'INTRA' | 'INTER'): { cgst: number; sgst: number; igst: number } {
  if (jurisdiction === 'INTER') return { cgst: 0, sgst: 0, igst: gstPaise };
  const cgst = mulDivHalfUp(gstPaise, 1, 2);
  return { cgst, sgst: gstPaise - cgst, igst: 0 };
}

function lineSnapshot(line: GstLineInput, jurisdiction: 'INTRA' | 'INTER', placeOfSupply: string): GstLineSnapshot {
  const quantity = line.quantity > 0 ? line.quantity : 1;
  const quantityPaise = rupeesToPaise(quantity);
  const pricePaise = rupeesToPaise(line.unitPrice);
  const entered = mulDivHalfUp(pricePaise, quantityPaise, 100);
  const discount = Math.min(Math.max(0, rupeesToPaise(line.discount ?? 0)), Math.max(0, entered));
  const rateHundredths = Math.max(0, rupeesToPaise(line.ratePercent));
  let taxable: number;
  let gst: number;
  let invoice: number;
  if (line.inclusive) {
    const gross = Math.max(0, entered - discount);
    taxable = rateHundredths > 0 ? mulDivHalfUp(gross, 10000, 10000 + rateHundredths) : gross;
    gst = gross - taxable;
    invoice = gross;
  } else {
    taxable = Math.max(0, entered - discount);
    gst = rateHundredths > 0 ? mulDivHalfUp(taxable, rateHundredths, 10000) : 0;
    invoice = taxable + gst;
  }
  const heads = splitHeads(gst, jurisdiction);
  return {
    enteredUnitPrice: moneyFromPaise(pricePaise),
    quantity: money(quantity),
    discount: moneyFromPaise(discount),
    priceIncludesGst: line.inclusive,
    gstRate: money(line.ratePercent),
    taxableAmount: moneyFromPaise(taxable),
    cgstAmount: moneyFromPaise(heads.cgst),
    sgstAmount: moneyFromPaise(heads.sgst),
    igstAmount: moneyFromPaise(heads.igst),
    gstTotal: moneyFromPaise(gst),
    lineTotal: moneyFromPaise(invoice),
    placeOfSupply,
  };
}

function headerFromLines(
  lines: GstLineSnapshot[],
  jurisdiction: 'INTRA' | 'INTER',
  placeOfSupply: string,
  sellerRegistration: string
): GstSnapshot {
  const sum = (pick: (row: GstLineSnapshot) => string) => lines.reduce((total, row) => total + readPaise(pick(row)), 0);
  const first = lines[0];
  const mixedRate = lines.some((row) => row.gstRate !== first?.gstRate);
  const mixedPrice = lines.some((row) => row.priceIncludesGst !== first?.priceIncludesGst);
  return {
    priceIncludesGst: mixedPrice ? false : Boolean(first?.priceIncludesGst),
    enteredUnitPrice: lines.length === 1 ? first?.enteredUnitPrice ?? money(0) : '',
    quantity: moneyFromPaise(sum((row) => row.quantity)),
    discount: moneyFromPaise(sum((row) => row.discount)),
    taxableAmount: moneyFromPaise(sum((row) => row.taxableAmount)),
    gstRate: mixedRate ? '' : first?.gstRate ?? money(0),
    cgstAmount: moneyFromPaise(sum((row) => row.cgstAmount)),
    sgstAmount: moneyFromPaise(sum((row) => row.sgstAmount)),
    igstAmount: moneyFromPaise(sum((row) => row.igstAmount)),
    gstTotal: moneyFromPaise(sum((row) => row.gstTotal)),
    invoiceTotal: moneyFromPaise(sum((row) => row.lineTotal)),
    taxJurisdiction: jurisdiction,
    placeOfSupply,
    calculationVersion: GST_CALCULATION_VERSION,
    taxStatus: 'CALCULATED',
    sellerRegistration,
    gstLines: lines,
  };
}

export function computeGstLine(
  line: GstLineInput,
  jurisdiction: 'INTRA' | 'INTER',
  placeOfSupply: string,
  sellerRegistration = ''
): GstSnapshot {
  return headerFromLines([lineSnapshot(line, jurisdiction, placeOfSupply)], jurisdiction, placeOfSupply, sellerRegistration);
}

export function rollGstLines(
  lines: GstLineInput[],
  jurisdiction: 'INTRA' | 'INTER',
  placeOfSupply: string,
  sellerRegistration = ''
): GstSnapshot {
  return headerFromLines(
    lines.map((line) => lineSnapshot(line, jurisdiction, placeOfSupply)),
    jurisdiction,
    placeOfSupply,
    sellerRegistration
  );
}

function scaleComponent(raw: string, creditPaise: number, invoicePaise: number): number {
  return mulDivHalfUp(readPaise(raw), creditPaise, invoicePaise);
}

export function scaleGstSnapshot(snapshot: GstSnapshot, credit: number): GstSnapshot {
  const invoicePaise = readPaise(snapshot.invoiceTotal);
  const creditPaise = rupeesToPaise(credit);
  const scale = (raw: string) => scaleComponent(raw, creditPaise, invoicePaise);
  const lines = (snapshot.gstLines ?? []).map((line) => {
    const cgst = scale(line.cgstAmount);
    const sgst = scale(line.sgstAmount);
    const igst = scale(line.igstAmount);
    const gst = cgst + sgst + igst;
    const taxable = scale(line.taxableAmount);
    return {
      ...line,
      discount: moneyFromPaise(scale(line.discount)),
      taxableAmount: moneyFromPaise(taxable),
      cgstAmount: moneyFromPaise(cgst),
      sgstAmount: moneyFromPaise(sgst),
      igstAmount: moneyFromPaise(igst),
      gstTotal: moneyFromPaise(gst),
      lineTotal: moneyFromPaise(taxable + gst),
    };
  });
  const cgst = scale(snapshot.cgstAmount);
  const sgst = scale(snapshot.sgstAmount);
  const igst = scale(snapshot.igstAmount);
  const gst = cgst + sgst + igst;
  const taxable = scale(snapshot.taxableAmount);
  return {
    ...snapshot,
    discount: moneyFromPaise(scale(snapshot.discount)),
    taxableAmount: moneyFromPaise(taxable),
    cgstAmount: moneyFromPaise(cgst),
    sgstAmount: moneyFromPaise(sgst),
    igstAmount: moneyFromPaise(igst),
    gstTotal: moneyFromPaise(gst),
    invoiceTotal: moneyFromPaise(taxable + gst),
    calculationVersion: GST_CALCULATION_VERSION,
    taxStatus: snapshot.taxStatus === 'NOT_CONFIGURED' ? 'NOT_CONFIGURED' : 'CALCULATED',
    sellerRegistration: snapshot.sellerRegistration ?? '',
    gstLines: lines,
  };
}

export function parseGstSnapshot(raw: unknown): GstSnapshot | null {
  if (!raw || typeof raw !== 'object') return null;
  const rec = raw as Record<string, unknown>;
  if (rec.invoiceTotal == null || rec.gstTotal == null || rec.taxableAmount == null) return null;
  const jurisdiction = rec.taxJurisdiction === 'INTRA' || rec.taxJurisdiction === 'INTER' ? rec.taxJurisdiction : null;
  return {
    priceIncludesGst: rec.priceIncludesGst === true,
    enteredUnitPrice: String(rec.enteredUnitPrice ?? '0.0000'),
    quantity: String(rec.quantity ?? '1.0000'),
    discount: String(rec.discount ?? '0.0000'),
    taxableAmount: String(rec.taxableAmount),
    gstRate: String(rec.gstRate ?? ''),
    cgstAmount: String(rec.cgstAmount ?? '0.0000'),
    sgstAmount: String(rec.sgstAmount ?? '0.0000'),
    igstAmount: String(rec.igstAmount ?? '0.0000'),
    gstTotal: String(rec.gstTotal),
    invoiceTotal: String(rec.invoiceTotal),
    taxJurisdiction: jurisdiction,
    placeOfSupply: String(rec.placeOfSupply ?? ''),
    calculationVersion: String(rec.calculationVersion ?? GST_CALCULATION_VERSION),
    taxStatus: rec.taxStatus === 'NOT_CONFIGURED' ? 'NOT_CONFIGURED' : 'CALCULATED',
    sellerRegistration: String(rec.sellerRegistration ?? ''),
    gstLines: Array.isArray(rec.gstLines)
      ? rec.gstLines.flatMap((line) => {
          if (!line || typeof line !== 'object') return [];
          const row = line as Record<string, unknown>;
          return [{
            enteredUnitPrice: String(row.enteredUnitPrice ?? '0.0000'),
            quantity: String(row.quantity ?? '1.0000'),
            discount: String(row.discount ?? '0.0000'),
            priceIncludesGst: row.priceIncludesGst === true,
            gstRate: String(row.gstRate ?? ''),
            taxableAmount: String(row.taxableAmount ?? '0.0000'),
            cgstAmount: String(row.cgstAmount ?? '0.0000'),
            sgstAmount: String(row.sgstAmount ?? '0.0000'),
            igstAmount: String(row.igstAmount ?? '0.0000'),
            gstTotal: String(row.gstTotal ?? '0.0000'),
            lineTotal: String(row.lineTotal ?? row.invoiceTotal ?? '0.0000'),
            placeOfSupply: String(row.placeOfSupply ?? ''),
          }];
        })
      : [],
  };
}

export function accumulateGstPosition(rows: Array<{
  id: string;
  title: string;
  eventType: string;
  occurredAt: string;
  approvalStatus: string;
  paymentStatus: string;
  gst: GstSnapshot | null | undefined;
  returnDirection?: string | null;
}>, month: string): GstPosition {
  const position = emptyGstPosition();
  position.asOfMonth = month;
  let output = 0;
  let input = 0;
  let cgstSales = 0;
  let sgstSales = 0;
  let igstSales = 0;
  let cgstPurchases = 0;
  let sgstPurchases = 0;
  let igstPurchases = 0;
  let adjustment = 0;
  for (const row of rows) {
    if (month && row.occurredAt.slice(0, 7) !== month) continue;
    if (row.approvalStatus === 'PENDING' || row.approvalStatus === 'REJECTED' || row.paymentStatus === 'CANCELLED') continue;
    const gst = row.gst;
    if (!gst || gst.taxStatus === 'NOT_CONFIGURED') continue;
    if (row.eventType !== 'SALE' && row.eventType !== 'PURCHASE' && row.eventType !== 'RETURN') continue;
    const total = Number(gst.gstTotal);
    let direction: GstPositionSource['direction'] = 'OUTPUT';
    if (row.eventType === 'SALE') {
      output += total;
      cgstSales += Number(gst.cgstAmount);
      sgstSales += Number(gst.sgstAmount);
      igstSales += Number(gst.igstAmount);
      direction = 'OUTPUT';
    } else if (row.eventType === 'PURCHASE') {
      input += total;
      cgstPurchases += Number(gst.cgstAmount);
      sgstPurchases += Number(gst.sgstAmount);
      igstPurchases += Number(gst.igstAmount);
      direction = 'INPUT';
    } else if (row.returnDirection === 'SUPPLIER') {
      input -= total;
      cgstPurchases -= Number(gst.cgstAmount);
      sgstPurchases -= Number(gst.sgstAmount);
      igstPurchases -= Number(gst.igstAmount);
      adjustment += total;
      direction = 'ADJUSTMENT';
    } else {
      output -= total;
      cgstSales -= Number(gst.cgstAmount);
      sgstSales -= Number(gst.sgstAmount);
      igstSales -= Number(gst.igstAmount);
      adjustment += total;
      direction = 'ADJUSTMENT';
    }
    position.sources.push({
      id: row.id,
      title: row.title,
      eventType: row.eventType,
      occurredAt: row.occurredAt,
      taxableAmount: gst.taxableAmount,
      gstTotal: gst.gstTotal,
      direction,
    });
  }
  position.outputGst = money(output);
  position.inputGst = money(input);
  position.difference = money(output - input);
  position.cgstSales = money(cgstSales);
  position.sgstSales = money(sgstSales);
  position.igstSales = money(igstSales);
  position.cgstPurchases = money(cgstPurchases);
  position.sgstPurchases = money(sgstPurchases);
  position.igstPurchases = money(igstPurchases);
  position.adjustmentGst = money(adjustment);
  return position;
}

export function emptyGstPosition(): GstPosition {
  const zero = '0.0000';
  return {
    outputGst: zero,
    inputGst: zero,
    difference: zero,
    cgstSales: zero,
    sgstSales: zero,
    igstSales: zero,
    cgstPurchases: zero,
    sgstPurchases: zero,
    igstPurchases: zero,
    adjustmentGst: zero,
    asOfMonth: '',
    sources: [],
  };
}
