import { getStoryComposer, TRADING_STORY_CHAPTERS } from './profiles';
import type { StorySnapshot } from './snapshot';

export interface TradingEvent {
  operatingEventId: string;
  eventType: string;
  title: string;
  amount: string | null;
  quantity: string | null;
  counterpartyName: string | null;
  paymentStatus: string;
  approvalStatus: string;
  occurredAt: string;
  parentEventId: string | null;
  rejectedQuantity?: string | null;
  contract?: any;
  lines?: Array<{ itemName: string; quantity: string; unit: string | null; unitPrice: string }>;
}

export interface TradingStoryInput {
  momentId: string;
  title: string;
  subtype: string;
  currency: string;
  startAt: string | null;
  endAt: string | null;
  channels: string[];
  participants: string[];
  events: TradingEvent[];
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function live(row: TradingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function round(value: number): string {
  return String(Math.round(value));
}

function namedParty(value: string | null | undefined): boolean {
  const name = String(value ?? '').trim().toLowerCase();
  return name.length > 0 && !['walk-in', 'anonymous', 'guest', 'walk in'].includes(name);
}

/**
 * Eight wholesale pages matching Figma TRD-STORY-P1 through TRD-STORY-P8.
 * Orders and shipments stay commitments: only a posted sale is revenue.
 */
export function composeTradingStory(input: TradingStoryInput): StorySnapshot {
  const rows = input.events.filter(live);
  const sales = rows.filter((row) => row.eventType === 'SALE' || row.eventType === 'INVOICE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const credits = rows
    .filter((row) => row.eventType === 'RETURN' && row.parentEventId && saleIds.has(row.parentEventId))
    .reduce((total, row) => total + num(row.amount), 0);
  const gross = sales.reduce((total, row) => total + num(row.amount), 0);
  const net = Math.max(0, gross - credits);

  const collections = rows
    .filter((row) => row.eventType === 'COLLECTION')
    .reduce((total, row) => total + num(row.amount), 0);
  const purchases = rows.filter((row) => row.eventType === 'PURCHASE' || row.eventType === 'VENDOR_COST');
  const spent = purchases.reduce((total, row) => total + num(row.amount), 0);
  const payments = rows.filter((row) => row.eventType === 'PAYMENT').reduce((total, row) => total + num(row.amount), 0);

  const orders = rows.filter((row) => row.eventType === 'ORDER');
  const openOrders = orders.filter(
    (row) => row.contract?.orderStatus !== 'CANCELLED' && row.contract?.orderStatus !== 'COMPLETED'
  );
  const shipments = rows.filter((row) => row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY');
  const inTransit = shipments.filter(
    (row) => row.contract?.shipmentStatus !== 'DELIVERED' && row.contract?.shipmentStatus !== 'CANCELLED'
  );
  const stockMoves = rows.filter((row) => row.eventType === 'STOCK');

  const buyers = new Set<string>();
  const suppliers = new Set<string>();
  for (const row of rows) {
    if (!namedParty(row.counterpartyName)) continue;
    const name = String(row.counterpartyName).trim();
    if (row.eventType === 'SALE' || row.eventType === 'COLLECTION' || row.eventType === 'ORDER') buyers.add(name);
    if (row.eventType === 'PURCHASE' || row.eventType === 'PAYMENT') suppliers.add(name);
  }

  const timeline = rows.slice(0, 15).map((row) => ({
    at: row.occurredAt,
    label: row.title,
    detail: row.eventType,
  }));

  const composer = getStoryComposer('TRADING_DISTRIBUTION');
  const openToCollect = Math.max(0, net - collections);
  const stockQty = stockMoves.reduce((total, row) => total + num(row.quantity), 0);

  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: input.subtype,
      groupFamily: 'TRADING_DISTRIBUTION',
      familyProfile: 'TRADING_DISTRIBUTION',
      startAt: input.startAt,
      endAt: input.endAt,
      completedAt: null,
      currencyCode: input.currency || 'INR',
    },
    people: input.participants.map((name) => ({ userId: null, displayName: name, roleCode: 'PARTICIPANT' })),
    metrics: {
      sales: net > 0 ? `₹${round(net)}` : 'N/A',
      collections: collections > 0 ? `₹${round(collections)}` : 'N/A',
      stock: stockQty !== 0 ? round(stockQty) : 'N/A',
      buyers: buyers.size,
      invoices: sales.length,
      orders: openOrders.length,
    },
    timeline,
    money: {
      contributed: collections,
      spent,
      remaining: openToCollect,
      unsettled: Math.max(0, spent - payments),
      target: null,
      categories: [],
      contributors: [],
      payers: [],
      expenses: purchases.map((p) => ({
        description: p.title,
        category: p.contract?.expenseCategory || 'Purchase',
        payer: p.counterpartyName || 'Supplier',
        amount: num(p.amount),
        at: p.occurredAt,
      })),
    },
    places: [],
    decisions: orders.map((o) => ({ title: o.title, status: o.approvalStatus })),
    photos: [],
    memories: [],
    highlights: [
      {
        id: 'trd-moment',
        title: 'The Business Moment',
        detail: `${sales.length} sales, ${purchases.length} purchases and ${shipments.length} dispatches recorded.`,
      },
      {
        id: 'trd-motion',
        title: 'Source to Sale',
        detail: 'One item journey — bought, stocked, sold, and collected — as recorded.',
      },
      {
        id: 'trd-orders',
        title: 'Orders & Commitments',
        detail: `${openOrders.length} open orders and ${inTransit.length} dispatches in transit. Commitments are not revenue.`,
      },
      {
        id: 'trd-money',
        title: 'Money In & Out',
        detail: `Collected ₹${round(collections)} · Paid ₹${round(payments)} · Still to collect ₹${round(openToCollect)}.`,
      },
      {
        id: 'trd-stock',
        title: 'Stock & Purchases',
        detail: `Bought ₹${round(spent)} across ${purchases.length} purchases with ${stockMoves.length} stock moves.`,
      },
      {
        id: 'trd-network',
        title: 'Buyers, Suppliers & Channels',
        detail: `${buyers.size} buyers, ${suppliers.size} suppliers${input.channels.length > 0 ? `, channels ${input.channels.join(', ')}` : ''}.`,
      },
      {
        id: 'trd-attention',
        title: 'What Needs Attention',
        detail: 'Overdue receivables, unsettled payables, and dispatches still open.',
      },
      {
        id: 'trd-sofar',
        title: 'The Story So Far',
        detail: 'Some sales became cash. Some commitments carried forward.',
      },
    ],
    narrative: {
      opening: `${input.title} recorded ${sales.length} sales and ${shipments.length} dispatches during this period.`,
      insights: [
        `Net recorded sales: ₹${round(net)}`,
        `Collected from buyers: ₹${round(collections)}`,
        `Purchases from suppliers: ₹${round(spent)}`,
      ],
    },
    chapters: [...TRADING_STORY_CHAPTERS],
    display: composer,
  };
}
