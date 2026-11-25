/**
 * Canonical Trading/Distribution metrics M01–M18.
 *
 * Pulse, Moments, Life, Memory, and Business Story must consume these same
 * calculations (Moment Story v3 contract §04 / hard reconciliation).
 * Missing ≠ zero: omit unsupported values rather than inventing 0.
 */

import type { OperatingEventRow } from './operating-catalog';
import { tradingStockPositions } from './operating-trading-contract';

export type TradingMetricId =
  | 'M01'
  | 'M02'
  | 'M03'
  | 'M04'
  | 'M05'
  | 'M06'
  | 'M07'
  | 'M08'
  | 'M09'
  | 'M10'
  | 'M11'
  | 'M12'
  | 'M13'
  | 'M14'
  | 'M15'
  | 'M16'
  | 'M17'
  | 'M18';

export interface TradingMetricValue {
  id: TradingMetricId;
  label: string;
  /** Numeric when known; null when not recorded / not computable. */
  value: number | null;
  unit: 'INR' | 'count' | 'qty' | 'ratio' | 'pct';
  basis?: string;
  sourceIds: string[];
  missing: boolean;
}

export interface TradingFactsBundle {
  asOf: string;
  currency: string;
  mode: 'trading' | 'distribution';
  metrics: Record<TradingMetricId, TradingMetricValue>;
  /** Effective posted events used for money math. */
  liveEventIds: string[];
  warnings: string[];
}

function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function effective(row: OperatingEventRow): boolean {
  if (row.ledgerStatus === 'SUPERSEDED') return false;
  if (row.eventType === 'REVERSAL') return false;
  return (
    row.approvalStatus !== 'PENDING' &&
    row.approvalStatus !== 'REJECTED' &&
    row.paymentStatus !== 'CANCELLED'
  );
}

function obligationOpen(
  row: OperatingEventRow,
  events: OperatingEventRow[],
  childType: 'COLLECTION' | 'PAYMENT'
): number {
  const settled = events
    .filter(
      (child) =>
        effective(child) &&
        child.parentEventId === row.operatingEventId &&
        child.eventType === childType
    )
    .reduce((total, child) => total + num(child.amount), 0);
  return Math.max(0, num(row.amount) - settled);
}

function metric(
  id: TradingMetricId,
  label: string,
  value: number | null,
  unit: TradingMetricValue['unit'],
  sourceIds: string[],
  basis?: string
): TradingMetricValue {
  return {
    id,
    label,
    value,
    unit,
    basis,
    sourceIds,
    missing: value == null,
  };
}

export interface ComputeTradingFactsArgs {
  events: OperatingEventRow[];
  currency?: string;
  asOf?: Date;
  mode?: 'trading' | 'distribution';
  /** Period target amount when configured (for M12). */
  periodTarget?: number | null;
  financeVisible?: boolean;
}

/**
 * Shared fact service for Trading Wholesale + Distribution Dealership.
 * Orders never create revenue; invoices never invent stock.
 */
export function computeTradingFacts(args: ComputeTradingFactsArgs): TradingFactsBundle {
  const asOf = args.asOf ?? new Date();
  const currency = args.currency ?? 'INR';
  const mode = args.mode ?? 'trading';
  const visible = args.financeVisible !== false;
  const live = args.events.filter(effective);
  const warnings: string[] = [];

  const sales = live.filter((row) => row.eventType === 'SALE' || row.eventType === 'INVOICE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const purchases = live.filter((row) => row.eventType === 'PURCHASE' || row.eventType === 'VENDOR_COST');
  const collections = live.filter((row) => row.eventType === 'COLLECTION');
  const payments = live.filter((row) => row.eventType === 'PAYMENT');
  const spends = live.filter((row) => row.eventType === 'SPEND');
  const orders = live.filter((row) => row.eventType === 'ORDER' || row.eventType === 'CUSTOMER_ORDER');
  const returns = live.filter((row) => row.eventType === 'RETURN');
  const shipments = live.filter((row) => row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY');

  const credits = returns
    .filter((row) => row.parentEventId && saleIds.has(row.parentEventId))
    .reduce((total, row) => total + num(row.amount), 0);
  const grossSales = sales.reduce((total, row) => total + num(row.amount), 0);
  const netSales = Math.max(0, grossSales - credits);
  const collected = collections.reduce((total, row) => total + num(row.amount), 0);
  const spentPurchases = purchases.reduce((total, row) => total + num(row.amount), 0);
  const operatingExpenses = spends.reduce((total, row) => total + num(row.amount), 0);
  const paid = payments.reduce((total, row) => total + num(row.amount), 0);
  const openToCollect = sales.reduce(
    (total, row) => total + obligationOpen(row, args.events, 'COLLECTION'),
    0
  );
  const openToPay = purchases.reduce(
    (total, row) => total + obligationOpen(row, args.events, 'PAYMENT'),
    0
  );

  const stockPositions = tradingStockPositions(live as never);
  const stockQty =
    stockPositions.length > 0
      ? stockPositions.reduce((total, row) => total + num(row.onHand), 0)
      : null;
  if (stockPositions.length === 0 && live.some((row) => row.eventType === 'STOCK')) {
    warnings.push('Stock moves recorded without computable on-hand position.');
  }

  const target = args.periodTarget != null && args.periodTarget > 0 ? args.periodTarget : null;
  const attainment =
    target != null && target > 0 ? Math.round((netSales / target) * 10000) / 100 : null;

  // Order execution remaining (ordered − invoiced) — quantity when present.
  let orderRemaining = 0;
  let orderHasQty = false;
  for (const order of orders) {
    const ordered = num(order.quantity);
    if (ordered <= 0) continue;
    orderHasQty = true;
    const invoiced = live
      .filter(
        (child) =>
          child.parentEventId === order.operatingEventId &&
          (child.eventType === 'SALE' || child.eventType === 'INVOICE')
      )
      .reduce((total, child) => total + num(child.quantity ?? child.amount), 0);
    orderRemaining += Math.max(0, ordered - invoiced);
  }

  let fulfilmentGap = 0;
  let fulfilmentHasQty = false;
  for (const sale of sales) {
    const invoicedQty = num(sale.quantity);
    if (invoicedQty <= 0) continue;
    fulfilmentHasQty = true;
    const moved = live
      .filter(
        (child) =>
          child.parentEventId === sale.operatingEventId &&
          (child.eventType === 'SHIPMENT' || child.eventType === 'DELIVERY')
      )
      .reduce((total, child) => total + num(child.quantity), 0);
    fulfilmentGap += Math.max(0, invoicedQty - moved);
  }

  const gstOutput = sales.reduce((total, row) => {
    const g = row.contract?.gst as { gstTotal?: string | number } | undefined;
    if (g?.gstTotal != null) return total + num(String(g.gstTotal));
    return total;
  }, 0);
  const gstPurchase = purchases.reduce((total, row) => {
    const g = row.contract?.gst as { gstTotal?: string | number } | undefined;
    if (g?.gstTotal != null) return total + num(String(g.gstTotal));
    return total;
  }, 0);
  const hasGst = live.some((row) => Boolean(row.contract?.gst));

  const attributedSales = sales.filter((row) => Boolean(row.counterpartyName?.trim())).length;
  const coverage =
    sales.length > 0 ? Math.round((attributedSales / sales.length) * 10000) / 100 : null;

  const overdue = [...sales, ...purchases].filter((row) => {
    const due = String(row.dueAt ?? row.contract?.promisedAt ?? '').slice(0, 10);
    if (!due) return false;
    const childType = row.eventType === 'SALE' || row.eventType === 'INVOICE' ? 'COLLECTION' : 'PAYMENT';
    const open = obligationOpen(row, args.events, childType);
    if (open <= 0) return false;
    return Date.parse(`${due}T00:00:00.000Z`) < asOf.getTime();
  });

  const hide = (value: number | null): number | null => (visible ? value : null);

  const metrics: Record<TradingMetricId, TradingMetricValue> = {
    M01: metric(
      'M01',
      'Gross taxable sales',
      hide(sales.length > 0 ? grossSales : null),
      'INR',
      sales.map((r) => r.operatingEventId),
      'tax_exclusive'
    ),
    M02: metric(
      'M02',
      'Sales credits',
      hide(credits > 0 || returns.length > 0 ? credits : sales.length > 0 ? 0 : null),
      'INR',
      returns.map((r) => r.operatingEventId)
    ),
    M03: metric(
      'M03',
      'Net sales',
      hide(sales.length > 0 ? netSales : null),
      'INR',
      sales.map((r) => r.operatingEventId),
      'tax_exclusive'
    ),
    M04: metric(
      'M04',
      'Collections',
      hide(collections.length > 0 ? collected : null),
      'INR',
      collections.map((r) => r.operatingEventId)
    ),
    M05: metric(
      'M05',
      'Customer receivables',
      hide(sales.length > 0 ? openToCollect : null),
      'INR',
      sales.map((r) => r.operatingEventId),
      'tax_inclusive_as_of'
    ),
    M06: metric(
      'M06',
      'Purchases',
      hide(purchases.length > 0 ? spentPurchases : null),
      'INR',
      purchases.map((r) => r.operatingEventId)
    ),
    M07: metric(
      'M07',
      'Operating expenses',
      hide(spends.length > 0 ? operatingExpenses : null),
      'INR',
      spends.map((r) => r.operatingEventId)
    ),
    M08: metric(
      'M08',
      'Supplier payments',
      hide(payments.length > 0 ? paid : null),
      'INR',
      payments.map((r) => r.operatingEventId)
    ),
    M09: metric(
      'M09',
      'Supplier payables',
      hide(purchases.length > 0 ? openToPay : null),
      'INR',
      purchases.map((r) => r.operatingEventId)
    ),
    M10: metric(
      'M10',
      'Net recorded cash movement',
      hide(collections.length + payments.length > 0 ? collected - paid : null),
      'INR',
      [...collections, ...payments].map((r) => r.operatingEventId)
    ),
    M11: metric(
      'M11',
      'Physical stock',
      stockQty,
      'qty',
      live.filter((r) => r.eventType === 'STOCK').map((r) => r.operatingEventId)
    ),
    M12: metric(
      'M12',
      'Target attainment',
      attainment,
      'pct',
      [],
      target != null ? 'period_target' : undefined
    ),
    M13: metric(
      'M13',
      'Order execution remaining',
      orderHasQty ? orderRemaining : null,
      'qty',
      orders.map((r) => r.operatingEventId)
    ),
    M14: metric(
      'M14',
      'Goods fulfilment gap',
      fulfilmentHasQty ? fulfilmentGap : null,
      'qty',
      [...sales, ...shipments].map((r) => r.operatingEventId)
    ),
    M15: metric(
      'M15',
      'GST snapshot',
      hasGst ? gstOutput - gstPurchase : null,
      'INR',
      live.filter((r) => Boolean(r.contract?.gst)).map((r) => r.operatingEventId),
      'indicative_difference'
    ),
    M16: metric(
      'M16',
      'Attribution coverage',
      coverage,
      'pct',
      sales.map((r) => r.operatingEventId)
    ),
    M17: metric(
      'M17',
      'Overdue open amount',
      overdue.length > 0
        ? overdue.reduce((total, row) => {
            const childType =
              row.eventType === 'SALE' || row.eventType === 'INVOICE' ? 'COLLECTION' : 'PAYMENT';
            return total + obligationOpen(row, args.events, childType);
          }, 0)
        : sales.length + purchases.length > 0
          ? 0
          : null,
      'INR',
      overdue.map((r) => r.operatingEventId)
    ),
    M18: metric(
      'M18',
      'Activity count',
      live.length > 0 ? live.length : null,
      'count',
      live.map((r) => r.operatingEventId)
    ),
  };

  return {
    asOf: asOf.toISOString(),
    currency,
    mode,
    metrics,
    liveEventIds: live.map((r) => r.operatingEventId),
    warnings,
  };
}

/** Convenience: net sales for pulse/story heroes (null when no sales). */
export function netSalesOrNull(bundle: TradingFactsBundle): number | null {
  return bundle.metrics.M03.value;
}
