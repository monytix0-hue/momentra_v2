import { DISTRIBUTION_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';

export interface DistributionEvent {
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

export interface DistributionStoryInput {
  momentId: string;
  title: string;
  subtype: string;
  currency: string;
  startAt: string | null;
  endAt: string | null;
  channels: string[];
  participants: string[];
  events: DistributionEvent[];
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function live(row: DistributionEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function round(value: number): string {
  return String(Math.round(value));
}

/** Eight distribution pages matching Figma DST-STORY-P1 through DST-STORY-P8. */
export function composeDistributionStory(input: DistributionStoryInput): StorySnapshot {
  const rows = input.events.filter(live);
  const sales = rows.filter((row) => row.eventType === 'SALE' || row.eventType === 'INVOICE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const credits = rows
    .filter((row) => row.eventType === 'RETURN' && row.parentEventId && saleIds.has(row.parentEventId))
    .reduce((total, row) => total + num(row.amount), 0);
  const gross = sales.reduce((total, row) => total + num(row.amount), 0);
  const net = Math.max(0, gross - credits);

  const collections = rows.filter((row) => row.eventType === 'COLLECTION').reduce((total, row) => total + num(row.amount), 0);
  const purchases = rows.filter((row) => row.eventType === 'PURCHASE' || row.eventType === 'VENDOR_COST');
  const spent = purchases.reduce((total, row) => total + num(row.amount), 0);
  const payments = rows.filter((row) => row.eventType === 'PAYMENT').reduce((total, row) => total + num(row.amount), 0);

  const orders = rows.filter((row) => row.eventType === 'ORDER' || row.eventType === 'CUSTOMER_ORDER');
  const deliveries = rows.filter((row) => row.eventType === 'DELIVERY' || row.eventType === 'SHIPMENT');

  const timeline = rows.slice(0, 15).map((row) => ({
    at: row.occurredAt,
    label: row.title,
    detail: row.eventType,
  }));

  const composer = getStoryComposer('TRADING_DISTRIBUTION');
  const countInvoices = sales.length;
  const countPurchases = purchases.length;
  const countDispatches = deliveries.length;

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
      invoices: countInvoices,
      purchases: countPurchases,
      dispatches: countDispatches,
    },
    timeline,
    money: {
      contributed: collections,
      spent,
      remaining: Math.max(0, net - collections),
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
      { id: 'dst-moment', title: 'The Business Moment', detail: `${countInvoices} dealer invoices, ${countPurchases} brand purchases and ${countDispatches} route dispatches recorded.` },
      { id: 'dst-motion', title: 'From Brand to Dealer', detail: 'One dealer order journey through the network, as recorded.' },
      { id: 'dst-orders', title: 'The Sales & Order Story', detail: `Net sales ₹${round(net)} after scheme credits. Orders are commitments, not revenue.` },
      { id: 'dst-money', title: 'Money In & Out', detail: `Collections ₹${round(collections)} collected from dealers · ₹${round(payments)} paid to brands.` },
      { id: 'dst-stock', title: 'Purchases, Goods & Stock', detail: `Bought ₹${round(spent)} from suppliers across ${countPurchases} purchases.` },
      { id: 'dst-network', title: 'Dealers, Brands & Routes', detail: `${input.participants.length} team members and recorded counterparties across active routes.` },
      { id: 'dst-attention', title: 'What Needs Attention', detail: 'Overdue receivables and pending dispatches requiring follow-up.' },
      { id: 'dst-sofar', title: 'The Story So Far', detail: 'Some activity became cash. Some commitments carried forward.' },
    ],
    narrative: {
      opening: `${input.title} recorded ${countInvoices} dealer invoices and ${countDispatches} dispatches during this period.`,
      insights: [
        `Net recorded sales: ₹${round(net)}`,
        `Collections from dealers: ₹${round(collections)}`,
        `Brand purchases: ₹${round(spent)}`,
      ],
    },
    chapters: [...DISTRIBUTION_STORY_CHAPTERS],
    display: composer,
  };
}
