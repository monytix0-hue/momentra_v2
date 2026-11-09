/**
 * Structured board figures for the Trading and Distribution view-all surfaces.
 *
 * Before this module every board reported a single prose `detail` sentence, so a
 * client could only render tiles by parsing English. Each section now carries
 * `metrics` (label/value/unit/tone) plus a `why` footnote, and every row carries a
 * `badge`. Figures that cannot be derived from recorded events are reported as
 * `Not recorded` instead of `0`, because a missing measurement and a measured zero
 * mean different things to an operator.
 */

import type {
  OperatingBoardMetric,
  OperatingBoardRow,
  OperatingBoardSection,
  OperatingEventRow,
  OperatingPreferences,
  OperatingRole,
} from './operating-catalog';
import {
  fulfilmentCoverage,
  buyersChannelsCoverage,
  ordersCoverage,
  stockCoverage,
  tradingStockPositions,
  consignmentStockHint,
} from './operating-trading-contract';

const NOT_RECORDED = 'Not recorded';

interface TradingFacts {
  events: OperatingEventRow[];
  live: OperatingEventRow[];
  visible: boolean;
  currency: string;
  asOf: Date;
  sales: OperatingEventRow[];
  purchases: OperatingEventRow[];
  collections: OperatingEventRow[];
  payments: OperatingEventRow[];
  orders: OperatingEventRow[];
  openOrders: OperatingEventRow[];
  shipments: OperatingEventRow[];
  inTransit: OperatingEventRow[];
  delivered: OperatingEventRow[];
  returns: OperatingEventRow[];
  stockMoves: OperatingEventRow[];
  netSales: number;
  grossSales: number;
  credits: number;
  collected: number;
  spent: number;
  paid: number;
  openToCollect: number;
  openToPay: number;
  buyers: string[];
  suppliers: string[];
  channels: string[];
  repeatBuyers: number;
  topSupplier: string | null;
  deliveredPct: number | null;
  receivedPct: number | null;
  avgPayLagDays: number | null;
  insufficientData: boolean;
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function effective(row: OperatingEventRow): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function namedParty(value: string | null | undefined): boolean {
  const name = String(value ?? '').trim().toLowerCase();
  return name.length > 0 && !['walk-in', 'anonymous', 'guest', 'walk in'].includes(name);
}

function money(value: number): string {
  return Number(value.toFixed(2)).toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

function daysBetween(from: string, to: string): number | null {
  const start = Date.parse(`${from.slice(0, 10)}T00:00:00.000Z`);
  const end = Date.parse(`${to.slice(0, 10)}T00:00:00.000Z`);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return null;
  return Math.max(0, Math.round((end - start) / 86_400_000));
}

function metric(
  key: string,
  label: string,
  value: string | number,
  unit: string | null = null,
  tone: OperatingBoardMetric['tone'] = 'NORMAL'
): OperatingBoardMetric {
  return { key, label, value: String(value), unit, tone };
}

/** Open obligation on a posted sale/purchase after settlements and returns. */
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

function buildFacts(
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  asOf: Date
): TradingFacts {
  const live = events.filter(effective);
  const sales = live.filter((row) => row.eventType === 'SALE' || row.eventType === 'INVOICE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const purchases = live.filter((row) => row.eventType === 'PURCHASE' || row.eventType === 'VENDOR_COST');
  const collections = live.filter((row) => row.eventType === 'COLLECTION');
  const payments = live.filter((row) => row.eventType === 'PAYMENT');
  const orders = live.filter((row) => row.eventType === 'ORDER' || row.eventType === 'CUSTOMER_ORDER');
  const openOrders = orders.filter(
    (row) => row.contract?.orderStatus !== 'CANCELLED' && row.contract?.orderStatus !== 'COMPLETED'
  );
  const shipments = live.filter((row) => row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY');
  const delivered = shipments.filter((row) => row.contract?.shipmentStatus === 'DELIVERED');
  const inTransit = shipments.filter(
    (row) => row.contract?.shipmentStatus !== 'DELIVERED' && row.contract?.shipmentStatus !== 'CANCELLED'
  );
  const returns = live.filter((row) => row.eventType === 'RETURN');
  const stockMoves = live.filter((row) => row.eventType === 'STOCK');

  const credits = returns
    .filter((row) => row.parentEventId && saleIds.has(row.parentEventId))
    .reduce((total, row) => total + num(row.amount), 0);
  const grossSales = sales.reduce((total, row) => total + num(row.amount), 0);

  const buyerBook = new Map<string, number>();
  const supplierBook = new Map<string, number>();
  const channelSet = new Set<string>();
  for (const row of live) {
    if (row.channelName?.trim()) channelSet.add(row.channelName.trim());
    if (!namedParty(row.counterpartyName)) continue;
    const name = String(row.counterpartyName).trim();
    if (row.eventType === 'SALE' || row.eventType === 'ORDER' || row.eventType === 'COLLECTION') {
      buyerBook.set(name, (buyerBook.get(name) ?? 0) + 1);
    }
    if (row.eventType === 'PURCHASE' || row.eventType === 'PAYMENT' || row.eventType === 'VENDOR_COST') {
      supplierBook.set(name, (supplierBook.get(name) ?? 0) + 1);
    }
  }

  const receivedOrders = live.filter(
    (row) => row.eventType === 'PURCHASE' && row.contract?.receiptState && row.contract.receiptState !== 'AWAITING'
  );

  // Pay lag: days between a promised due date and the final settlement date.
  const lags: number[] = [];
  for (const row of [...sales, ...purchases]) {
    const due = String(row.dueAt ?? row.contract?.promisedAt ?? '').slice(0, 10);
    if (!due) continue;
    const children = live.filter(
      (child) =>
        child.parentEventId === row.operatingEventId &&
        (child.eventType === 'COLLECTION' || child.eventType === 'PAYMENT')
    );
    if (children.length === 0) continue;
    const last = children
      .map((child) => child.occurredAt)
      .sort((a, b) => a.localeCompare(b))
      .at(-1);
    if (!last) continue;
    const lag = daysBetween(due, last);
    if (lag != null) lags.push(lag);
  }

  const openToCollect = sales.reduce(
    (total, row) => total + obligationOpen(row, events, 'COLLECTION'),
    0
  );
  const openToPay = purchases.reduce((total, row) => total + obligationOpen(row, events, 'PAYMENT'), 0);

  const topSupplier = [...supplierBook.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null;
  const deliveredPct = shipments.length > 0 ? Math.round((delivered.length / shipments.length) * 100) : null;
  const receivedPct =
    purchases.length > 0 ? Math.round((receivedOrders.length / purchases.length) * 100) : null;

  return {
    events,
    live,
    visible,
    currency,
    asOf,
    sales,
    purchases,
    collections,
    payments,
    orders,
    openOrders,
    shipments,
    inTransit,
    delivered,
    returns,
    stockMoves,
    netSales: Math.max(0, grossSales - credits),
    grossSales,
    credits,
    collected: collections.reduce((total, row) => total + num(row.amount), 0),
    spent: purchases.reduce((total, row) => total + num(row.amount), 0),
    paid: payments.reduce((total, row) => total + num(row.amount), 0),
    openToCollect,
    openToPay,
    buyers: [...buyerBook.keys()],
    suppliers: [...supplierBook.keys()],
    channels: [...channelSet],
    repeatBuyers: [...buyerBook.values()].filter((count) => count > 1).length,
    topSupplier,
    deliveredPct,
    receivedPct,
    avgPayLagDays:
      lags.length > 0 ? Math.round((lags.reduce((sum, lag) => sum + lag, 0) / lags.length) * 10) / 10 : null,
    insufficientData: sales.length === 0 && purchases.length === 0 && orders.length === 0,
  };
}

function moneyValue(facts: TradingFacts, value: number): string {
  return facts.visible ? `${money(value)} ${facts.currency}` : 'Hidden';
}

/** `why` is the Figma footnote: it explains a gap, never restates the tiles. */
function whyFor(code: string, facts: TradingFacts): string | null {
  switch (code) {
    case 'orders':
    case 'M4':
      return (
        ordersCoverage(facts.events) ??
        (facts.openOrders.length > 0
          ? 'Orders are commitments. Only a posted sale becomes revenue.'
          : null)
      );
    case 'stock':
      return stockCoverage(facts.events) ?? consignmentStockHint(facts.events);
    case 'network':
    case 'L2':
      return buyersChannelsCoverage(facts.events);
    case 'fulfilment':
      return fulfilmentCoverage(facts.events);
    case 'ME2':
    case 'L3':
      return facts.collected === 0 && facts.paid === 0
        ? 'No settlement recorded yet, so cash movement is not measured.'
        : null;
    default:
      return null;
  }
}

function metricsFor(code: string, facts: TradingFacts): OperatingBoardMetric[] {
  const unitCount = 'count';
  switch (code) {
    case 'orders': {
      return [
        metric('sales', 'Posted sales', facts.sales.length, unitCount),
        metric('openOrders', 'Open orders', facts.openOrders.length, unitCount, facts.openOrders.length > 0 ? 'WARNING' : 'NORMAL'),
        metric('inTransit', 'In transit', facts.inTransit.length, unitCount),
        metric(
          'delivered',
          'Delivered',
          facts.deliveredPct == null ? NOT_RECORDED : `${facts.deliveredPct}%`,
          null,
          facts.deliveredPct != null && facts.deliveredPct >= 80 ? 'POSITIVE' : 'NORMAL'
        ),
        metric('netSales', 'Net sales', moneyValue(facts, facts.netSales)),
      ];
    }
    case 'stock': {
      const positions = tradingStockPositions(facts.events);
      const onHand = positions.reduce((total, position) => total + position.onHand, 0);
      const low = positions.filter(
        (position) => position.reorderPoint != null && position.onHand <= position.reorderPoint
      ).length;
      return [
        metric('skus', 'Products tracked', positions.length, unitCount),
        metric('onHand', 'On hand', positions.length > 0 ? Number(onHand.toFixed(2)) : NOT_RECORDED, positions[0]?.unit ?? null),
        metric('low', 'Low stock', low, unitCount, low > 0 ? 'WARNING' : 'NORMAL'),
        metric('stockMoves', 'Stock moves', facts.stockMoves.length, unitCount),
        metric('purchases', 'Purchases', facts.purchases.length, unitCount),
      ];
    }
    case 'network': {
      return [
        metric('buyers', 'Buyers', facts.buyers.length, unitCount),
        metric('suppliers', 'Suppliers', facts.suppliers.length, unitCount),
        metric('channels', 'Channels', facts.channels.length, unitCount),
        metric('repeat', 'Repeat buyers', facts.repeatBuyers, unitCount, facts.repeatBuyers > 0 ? 'POSITIVE' : 'NORMAL'),
      ];
    }
    case 'M4': {
      return [
        metric('toCollect', 'To collect', moneyValue(facts, facts.openToCollect), null, facts.openToCollect > 0 ? 'WARNING' : 'POSITIVE'),
        metric('toPay', 'To pay', moneyValue(facts, facts.openToPay), null, facts.openToPay > 0 ? 'WARNING' : 'POSITIVE'),
        metric('openOrders', 'Open orders', facts.openOrders.length, unitCount),
        metric('inTransit', 'In transit', facts.inTransit.length, unitCount),
      ];
    }
    case 'L1': {
      return [
        metric('sales', 'Sales', facts.sales.length, unitCount),
        metric('purchases', 'Purchases', facts.purchases.length, unitCount),
        metric('stockMoves', 'Stock moves', facts.stockMoves.length, unitCount),
        metric('issues', 'Issues', facts.live.filter((row) => row.eventType === 'ISSUE').length, unitCount),
      ];
    }
    case 'L2': {
      return [
        metric('channels', 'Channels', facts.channels.length, unitCount),
        metric('buyers', 'Named buyers', facts.buyers.length, unitCount),
        metric('sales', 'Attributed sales', moneyValue(facts, facts.netSales)),
      ];
    }
    case 'L3': {
      return [
        metric('collected', 'Collected', moneyValue(facts, facts.collected), null, facts.collected > 0 ? 'POSITIVE' : 'NORMAL'),
        metric('paid', 'Paid', moneyValue(facts, facts.paid)),
        metric(
          'net',
          'Net movement',
          moneyValue(facts, facts.collected - facts.paid),
          null,
          facts.collected - facts.paid >= 0 ? 'POSITIVE' : 'WARNING'
        ),
        metric(
          'payLag',
          'Avg pay lag',
          facts.avgPayLagDays == null ? NOT_RECORDED : facts.avgPayLagDays,
          facts.avgPayLagDays == null ? null : 'days',
          facts.avgPayLagDays != null && facts.avgPayLagDays > 7 ? 'WARNING' : 'NORMAL'
        ),
      ];
    }
    case 'L4': {
      return [
        metric('stockMoves', 'Stock moves', facts.stockMoves.length, unitCount),
        metric('issues', 'Issues', facts.live.filter((row) => row.eventType === 'ISSUE').length, unitCount),
        metric('updates', 'Updates', facts.live.filter((row) => row.eventType === 'UPDATE').length, unitCount),
      ];
    }
    case 'L5':
    case 'ME3': {
      const today = facts.asOf.toISOString().slice(0, 10);
      const todayCount = facts.live.filter((row) => row.occurredAt.slice(0, 10) === today).length;
      return [
        metric('records', 'Records', facts.live.length, unitCount),
        metric('today', 'Today', todayCount, unitCount),
        metric('people', 'People', new Set(facts.live.map((row) => row.submittedByUserId)).size, unitCount),
      ];
    }
    case 'ME1': {
      const months = new Set(
        facts.live
          .map((row) => row.occurredAt.slice(0, 7))
          .filter((month) => /^\d{4}-\d{2}$/.test(month))
      );
      return [
        metric('months', 'Months recorded', months.size, unitCount),
        metric('records', 'Records', facts.live.length, unitCount),
        metric('netSales', 'Net sales', moneyValue(facts, facts.netSales)),
      ];
    }
    case 'ME2': {
      return [
        metric('collected', 'Collected', moneyValue(facts, facts.collected)),
        metric('paid', 'Paid', moneyValue(facts, facts.paid)),
        metric('returns', 'Returns', facts.returns.length, unitCount, facts.returns.length > 0 ? 'WARNING' : 'NORMAL'),
        metric('credits', 'Credits', moneyValue(facts, facts.credits)),
      ];
    }
    case 'ME4': {
      const pending = facts.events.filter((row) => row.approvalStatus === 'PENDING').length;
      return [
        metric('issues', 'Issues', facts.live.filter((row) => row.eventType === 'ISSUE').length, unitCount),
        metric('approvals', 'Waiting approval', pending, unitCount, pending > 0 ? 'WARNING' : 'POSITIVE'),
      ];
    }
    case 'fulfilment': {
      return [
        metric(
          'delivered',
          'Delivered',
          facts.deliveredPct == null ? NOT_RECORDED : `${facts.deliveredPct}%`,
          null,
          facts.deliveredPct != null && facts.deliveredPct >= 80 ? 'POSITIVE' : 'NORMAL'
        ),
        metric(
          'received',
          'Received',
          facts.receivedPct == null ? NOT_RECORDED : `${facts.receivedPct}%`,
          null,
          facts.receivedPct != null && facts.receivedPct >= 80 ? 'POSITIVE' : 'NORMAL'
        ),
        metric('returns', 'Returns', facts.returns.length, unitCount, facts.returns.length > 0 ? 'WARNING' : 'NORMAL'),
        metric(
          'payLag',
          'Avg pay lag',
          facts.avgPayLagDays == null ? NOT_RECORDED : facts.avgPayLagDays,
          facts.avgPayLagDays == null ? null : 'days'
        ),
      ];
    }
    case 'transit': {
      return [
        metric('inTransit', 'In transit', facts.inTransit.length, unitCount),
        metric('delivered', 'Delivered', facts.delivered.length, unitCount),
      ];
    }
    case 'onhand': {
      const positions = tradingStockPositions(facts.events);
      return [
        metric('items', 'Items', positions.length, unitCount),
        metric(
          'onHand',
          'On hand',
          positions.length > 0 ? Number(positions.reduce((sum, p) => sum + p.onHand, 0).toFixed(2)) : NOT_RECORDED,
          positions[0]?.unit ?? null
        ),
      ];
    }
    default: {
      return [
        metric('records', 'Records', facts.live.length, unitCount),
        metric(
          'toCollect',
          'To collect',
          moneyValue(facts, facts.openToCollect),
          null,
          facts.openToCollect > 0 ? 'WARNING' : 'NORMAL'
        ),
      ];
    }
  }
}

function badgeFor(row: OperatingBoardRow, event: OperatingEventRow | undefined): string | null {
  if (!event) {
    if (row.approvalStatus === 'PENDING') return 'Approval';
    if (row.paymentStatus === 'COLLECTED' || row.paymentStatus === 'PAID') return 'Settled';
    return null;
  }
  const contract = event.contract ?? null;
  if (event.approvalStatus === 'PENDING') return 'Approval';
  if (contract?.holdStatus === 'HELD') return 'On hold';
  if (event.eventType === 'ORDER' || event.eventType === 'CUSTOMER_ORDER') {
    if (contract?.orderStatus === 'CANCELLED') return 'Cancelled';
    if (contract?.orderStatus === 'COMPLETED') return 'Done';
    return 'Open';
  }
  if (event.eventType === 'SHIPMENT' || event.eventType === 'DELIVERY') {
    if (contract?.shipmentStatus === 'DELIVERED') return 'Delivered';
    if (contract?.shipmentStatus === 'CANCELLED') return 'Cancelled';
    return 'Transit';
  }
  if (contract?.goodsStatus === 'PARTIAL') return 'Part';
  if (event.eventType === 'RETURN') return 'Return';
  if (row.paymentStatus === 'COLLECTED' || row.paymentStatus === 'PAID' || row.paymentStatus === 'SETTLED') {
    return 'Settled';
  }
  if (event.paymentRelevance === 'RECEIVABLE' || event.paymentRelevance === 'PAYABLE') {
    const due = String(event.dueAt ?? '').slice(0, 10);
    if (due && due < new Date().toISOString().slice(0, 10) && row.amount) return 'Overdue';
    if (row.amount) return 'Open';
  }
  return null;
}

/** Fill `metrics`, `why`, and row `badge`/`subtitle` on every trading board. */
export function applyTradingBoardMetrics(
  boards: { moments: OperatingBoardSection[]; life: OperatingBoardSection[]; memory: OperatingBoardSection[] },
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  asOf: Date
): void {
  const facts = buildFacts(events, visible, currency, asOf);
  const byId = new Map(events.map((row) => [row.operatingEventId, row]));
  const sections = [...boards.moments, ...boards.life, ...boards.memory];
  for (const section of sections) {
    section.metrics = metricsFor(section.code, facts);
    section.why = whyFor(section.code, facts);
    for (const row of section.rows) {
      const event = byId.get(row.id);
      row.badge = badgeFor(row, event);
      row.subtitle = row.counterpartyName ?? row.channelName ?? row.note ?? null;
    }
  }
  // A section with no tiles is a client bug waiting to happen; keep two facts minimum.
  for (const section of sections) {
    if (!section.metrics || section.metrics.length === 0) {
      section.metrics = [
        metric('records', 'Records', section.rows.length, 'count'),
        metric('status', 'Status', section.rows.length > 0 ? 'Recorded' : NOT_RECORDED),
      ];
    }
  }
}

export interface OperatingPulseMetricGroup {
  code: 'TARGET_BUDGET' | 'FINANCE' | 'PAYMENT_READINESS' | 'ATTENTION' | 'ACTIVITY' | 'GST';
  metrics: OperatingBoardMetric[];
  why: string | null;
}

/**
 * The six Pulse surfaces are projection figures rather than board sections, so they
 * get the same tile shape through one extra additive field.
 */
export function tradingPulseMetrics(args: {
  events: OperatingEventRow[];
  visible: boolean;
  currency: string;
  asOf: Date;
  prefs: OperatingPreferences;
  role: OperatingRole;
  projection: {
    budget: { amount: string; spent: string; remaining: string; configured: boolean } | null;
    target: { metric: string; value: string; progress: string; configured: boolean } | null;
    finance: { sales: string; received: string; outstanding: string; payable: string; eligibleCost: string; cashPaid: string } | null;
    attention: Array<{ severity?: string | null }>;
    paymentReadiness: Array<{ relevance: string; status: string; amount: string; dueBucket?: string | null }>;
    activity: Array<{ eventType: string }>;
  };
}): OperatingPulseMetricGroup[] {
  const { prefs, projection } = args;
  const facts = buildFacts(args.events, args.visible, args.currency, args.asOf);
  const overdue = projection.paymentReadiness.filter((row) => row.dueBucket === 'OVERDUE').length;
  const dueSoon = projection.paymentReadiness.filter((row) => row.dueBucket === 'DUE_SOON').length;
  const high = projection.attention.filter((row) => row.severity === 'HIGH').length;
  const pendingApprovals = args.events.filter((row) => row.approvalStatus === 'PENDING').length;
  const gstRows = facts.live.filter((row) => row.contract?.gst);

  return [
    {
      code: 'TARGET_BUDGET',
      metrics: [
        metric(
          'target',
          'Target',
          projection.target?.configured ? moneyValue(facts, num(projection.target.value)) : NOT_RECORDED
        ),
        metric(
          'progress',
          'Achieved',
          projection.target?.configured ? moneyValue(facts, num(projection.target.progress)) : NOT_RECORDED,
          null,
          projection.target?.configured && num(projection.target.progress) >= num(projection.target.value)
            ? 'POSITIVE'
            : 'NORMAL'
        ),
        metric(
          'budget',
          'Budget',
          projection.budget?.configured ? moneyValue(facts, num(projection.budget.amount)) : NOT_RECORDED
        ),
        metric(
          'remaining',
          'Budget left',
          projection.budget?.configured ? moneyValue(facts, num(projection.budget.remaining)) : NOT_RECORDED,
          null,
          projection.budget?.configured && num(projection.budget.remaining) < 0 ? 'WARNING' : 'NORMAL'
        ),
      ],
      why: projection.target?.configured || projection.budget?.configured
        ? null
        : 'Set a target and budget to measure progress against them.',
    },
    {
      code: 'FINANCE',
      metrics: [
        metric('sales', 'Sales', projection.finance ? moneyValue(facts, num(projection.finance.sales)) : 'Hidden'),
        metric('received', 'Received', projection.finance ? moneyValue(facts, num(projection.finance.received)) : 'Hidden'),
        metric(
          'outstanding',
          'Outstanding',
          projection.finance ? moneyValue(facts, num(projection.finance.outstanding)) : 'Hidden',
          null,
          projection.finance && num(projection.finance.outstanding) > 0 ? 'WARNING' : 'NORMAL'
        ),
        metric('payable', 'Payable', projection.finance ? moneyValue(facts, num(projection.finance.payable)) : 'Hidden'),
      ],
      why: projection.finance ? null : 'Finance figures are visible to owner, finance, and approver roles.',
    },
    {
      code: 'PAYMENT_READINESS',
      metrics: [
        metric('overdue', 'Overdue', overdue, 'count', overdue > 0 ? 'WARNING' : 'POSITIVE'),
        metric('dueSoon', 'Due soon', dueSoon, 'count', dueSoon > 0 ? 'WARNING' : 'NORMAL'),
        metric('open', 'Open', projection.paymentReadiness.length, 'count'),
        metric('toCollect', 'To collect', moneyValue(facts, facts.openToCollect)),
      ],
      why: projection.paymentReadiness.length === 0 ? 'Nothing is awaiting settlement.' : null,
    },
    {
      code: 'ATTENTION',
      metrics: [
        metric('high', 'High priority', high, 'count', high > 0 ? 'WARNING' : 'POSITIVE'),
        metric('open', 'Open items', projection.attention.length, 'count'),
        metric('approvals', 'Waiting approval', pendingApprovals, 'count', pendingApprovals > 0 ? 'WARNING' : 'NORMAL'),
        metric('overdue', 'Overdue', overdue, 'count', overdue > 0 ? 'WARNING' : 'NORMAL'),
      ],
      why: projection.attention.length === 0 ? 'Nothing needs attention right now.' : null,
    },
    {
      code: 'ACTIVITY',
      metrics: [
        metric('records', 'Records', projection.activity.length, 'count'),
        metric('sales', 'Sales', facts.sales.length, 'count'),
        metric('purchases', 'Purchases', facts.purchases.length, 'count'),
        metric('stockMoves', 'Stock moves', facts.stockMoves.length, 'count'),
      ],
      why: null,
    },
    {
      code: 'GST',
      metrics: [
        metric('invoices', 'Taxed records', gstRows.length, 'count'),
        metric(
          'tax',
          'Tax',
          args.visible
            ? `${money(
                gstRows.reduce((total, row) => total + num(row.contract?.gst?.gstTotal ?? row.contract?.taxAmount), 0)
              )} ${args.currency}`
            : 'Hidden'
        ),
        metric('registration', 'Registration', prefs.gstRegistration || NOT_RECORDED),
        metric('state', 'Business state', prefs.gstState || NOT_RECORDED),
      ],
      why: prefs.gstTracking === 'YES'
        ? null
        : 'GST tracking is off for this moment, so tax is not computed on new records.',
    },
  ];
}
