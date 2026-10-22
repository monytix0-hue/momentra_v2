import { MANUFACTURING_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';
import {
  explicitAccepted,
  manufacturingStock,
  recordedOutput,
  type ManufacturingEvent,
} from '../business/operating-manufacturing';

export interface ManufacturingStoryInput {
  momentId: string;
  title: string;
  subtype: string;
  currency: string;
  startAt: string | null;
  endAt: string | null;
  channels: string[];
  participants: string[];
  events: ManufacturingEvent[];
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function live(row: ManufacturingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function round(value: number): string {
  return String(Math.round(value));
}

/** Eight manufacturing pages. Missing facts stay “not recorded”. */
export function composeManufacturingStory(input: ManufacturingStoryInput): StorySnapshot {
  const made = input.subtype === 'MADE_TO_ORDER';
  const rows = input.events.filter(live);
  const sales = rows.filter((row) => row.eventType === 'SALE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const credits = rows
    .filter((row) => row.eventType === 'RETURN' && row.parentEventId && saleIds.has(row.parentEventId))
    .reduce((total, row) => total + num(row.amount), 0);
  const gross = sales.reduce((total, row) => total + num(row.amount), 0);
  const net = Math.max(0, gross - credits);
  const collected = rows.filter((row) => row.eventType === 'COLLECTION').reduce((total, row) => total + num(row.amount), 0);
  const paidIn = sales
    .filter((row) => row.paymentStatus === 'COLLECTED')
    .filter((row) => !rows.some((child) => child.parentEventId === row.operatingEventId && child.eventType === 'COLLECTION'))
    .reduce((total, row) => total + num(row.amount), 0);
  const receipts = collected + paidIn;
  const receivable = Math.max(0, net - receipts);
  const orders = rows.filter((row) => row.eventType === 'CUSTOMER_ORDER');
  const pricedOrders = orders.filter((row) => row.amount != null);
  const orderValue = pricedOrders.reduce((total, row) => total + num(row.amount), 0);
  const production = rows.filter((row) => row.eventType === 'PRODUCTION');
  const recorded = production.reduce((total, row) => total + recordedOutput(row), 0);
  const acceptedRows = production.filter((row) => explicitAccepted(row) != null);
  const accepted = acceptedRows.reduce((total, row) => total + (explicitAccepted(row) ?? 0), 0);
  const rejectedKnown = production.filter((row) => row.rejectedQuantity != null && row.rejectedQuantity !== '');
  const rejected = rejectedKnown.reduce((total, row) => total + num(row.rejectedQuantity), 0);
  const sameOutputUnit = new Set(acceptedRows.map((row) => String(row.unit ?? '').trim().toLowerCase())).size <= 1;
  const yieldKnown = acceptedRows.length > 0 && rejectedKnown.length > 0 && sameOutputUnit && accepted + rejected > 0;
  const yieldPct = yieldKnown ? ((accepted / (accepted + rejected)) * 100).toFixed(2) : null;
  const deliveries = rows.filter((row) => row.eventType === 'DELIVERY');
  const itemizedDeliveries = deliveries.filter((row) => row.contract?.entryMode === 'ITEMIZED' && row.quantity != null);
  const delivered = itemizedDeliveries.reduce((total, row) => total + num(row.quantity), 0);
  const statusOnly = deliveries.filter((row) => row.contract?.entryMode !== 'ITEMIZED' || row.quantity == null);
  const mixedDelivery = new Set(itemizedDeliveries.map((row) => String(row.unit ?? '').trim().toLowerCase())).size > 1;
  const oneOrder = orders.length === 1 ? orders[0] : null;
  const pending =
    oneOrder && oneOrder.quantity && itemizedDeliveries.length && !mixedDelivery
      ? Math.max(0, num(oneOrder.quantity) - delivered)
      : null;
  const fulfilmentPct =
    pending != null && num(oneOrder?.quantity) > 0 ? ((delivered / num(oneOrder?.quantity)) * 100).toFixed(0) : null;
  const stock = manufacturingStock(input.events);
  const finished = stock.filter((row) => row.kind === 'FINISHED');
  const materials = stock.filter((row) => row.kind === 'MATERIAL');
  const undatedOpen = rows.filter(
    (row) =>
      (row.eventType === 'SALE' || row.eventType === 'PURCHASE') &&
      row.paymentStatus === 'PENDING' &&
      !row.dueAt
  );
  const people = [
    ...input.participants,
    ...rows.map((row) => row.counterpartyName?.trim() ?? '').filter(Boolean),
  ];
  const uniquePeople = [...new Set(people)];
  const salesKnown = sales.length > 0;
  const outputDetail = production.length
    ? `Recorded output ${round(recorded)}. ${acceptedRows.length ? `Accepted ${round(accepted)}.` : 'Acceptance not recorded.'} ${yieldPct ? `Yield ${yieldPct}%.` : 'Yield not recorded.'} ${finished.length ? `Finished stock ${finished.map((row) => `${row.item} ${round(row.onHand)}`).join(', ')}.` : 'Finished stock was not increased.'}`
    : 'Output not recorded.';
  const moneyDetail = salesKnown
    ? `Net sales ${round(net)}. Receipts ${round(receipts)}. Open to collect ${round(receivable)}. Orders are not sales. No GST is recorded.`
    : `Net sales not recorded. ${pricedOrders.length ? `Order commitment ${round(orderValue)} is not a receivable.` : 'Orders not recorded.'}`;
  const materialsDetail = made
    ? `Order fulfilment. ${itemizedDeliveries.length ? `Delivered ${mixedDelivery ? 'not recorded' : round(delivered)}.` : 'Delivered quantity not recorded.'} ${pending != null ? `Pending ${round(pending)}. ${fulfilmentPct}% of that order.` : ''} ${statusOnly.length ? `${statusOnly.length} status-only deliveries have no quantity.` : ''} ${materials.length ? `Materials on hand from receipts.` : 'Material stock not recorded.'}`
    : `Materials and finished goods. ${materials.length ? `${materials.length} material lines from receipts.` : 'Material receipt not recorded.'} ${finished.length ? 'Finished stock follows accepted quantity.' : 'Finished stock not recorded.'}`;
  const attention = [
    undatedOpen.length ? `${undatedOpen.length} open balances have no due date and are not overdue.` : '',
    mixedDelivery || !sameOutputUnit ? 'Unlike units were not added.' : '',
    orders.length && !salesKnown ? 'An order is still open and is not a sale.' : '',
  ].filter(Boolean);
  const composer = getStoryComposer('MANUFACTURING_MADE_TO_ORDER');
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: input.subtype,
      groupFamily: 'MANUFACTURING_PRODUCTION',
      familyProfile: 'MANUFACTURING_MADE_TO_ORDER',
      startAt: input.startAt,
      endAt: input.endAt,
      completedAt: null,
      currencyCode: input.currency || 'INR',
    },
    people: uniquePeople.map((name) => ({ userId: null, displayName: name, roleCode: 'PARTICIPANT' })),
    metrics: {
      sales: salesKnown ? round(net) : 'not recorded',
      orders: orders.length ? String(orders.length) : 'not recorded',
      accepted: acceptedRows.length && sameOutputUnit ? round(accepted) : 'not recorded',
      recorded: production.length ? round(recorded) : 'not recorded',
      receivable: salesKnown ? round(receivable) : 'not recorded',
      receipts: receipts > 0 ? round(receipts) : 'not recorded',
      orderValue: pricedOrders.length ? round(orderValue) : 'not recorded',
      delivered: itemizedDeliveries.length && !mixedDelivery ? round(delivered) : 'not recorded',
      yield: yieldPct ?? 'not recorded',
    },
    timeline: input.events.slice(0, 12).map((row) => ({ at: row.occurredAt, label: row.title, detail: row.eventType })),
    money: {
      contributed: receipts,
      spent: rows.filter((row) => row.eventType === 'SPEND' || row.eventType === 'PURCHASE').reduce((total, row) => total + num(row.amount), 0),
      remaining: 0,
      unsettled: receivable,
      target: null,
      categories: [],
      contributors: [],
      payers: [],
      expenses: [],
    },
    places: [],
    decisions: [],
    memories: [],
    photos: [],
    highlights: [
      { id: 'mf-moment', title: 'The Manufacturing Moment', detail: `${input.title}. ${made ? 'Made-to-order.' : 'Standard production.'} ${input.channels.length ? input.channels.join(', ') : 'Channel not recorded.'}` },
      { id: 'mf-motion', title: 'The Period in Motion', detail: input.events.length ? `${input.events.length} posted events.` : 'Activity not recorded.' },
      {
        id: 'mf-output',
        title: 'The Production and Order Story',
        detail: made
          ? `Orders come first. ${orders.length ? `${orders.length} orders.` : 'Orders not recorded.'} ${outputDetail}`
          : `Production comes first. ${outputDetail}`,
      },
      { id: 'mf-money', title: 'The Money Story', detail: moneyDetail },
      {
        id: 'mf-materials',
        title: 'The Materials and Fulfilment Story',
        detail: materialsDetail,
      },
      { id: 'mf-people', title: 'People Behind Production', detail: uniquePeople.length ? uniquePeople.join(', ') : 'People not recorded.' },
      { id: 'mf-attention', title: 'What Still Needs Attention', detail: attention.length ? attention.join(' ') : 'Nothing recorded is waiting.' },
      { id: 'mf-sofar', title: 'The Story So Far', detail: 'This chapter records what was posted. It does not estimate profit, yield, or stock that was not recorded.' },
    ],
    narrative: {
      opening: `${input.title}. A recorded output is not accepted stock. An order is not a sale.`,
      insights: ['No GST amount is stored.'],
    },
    chapters: MANUFACTURING_STORY_CHAPTERS,
    display: composer,
  };
}
