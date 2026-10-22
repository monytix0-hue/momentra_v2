import { RETAIL_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';
import type { RetailEvent } from '../business/operating-retail-contract';
import { stockPositions } from '../business/operating-retail-contract';

export interface RetailStoryInput {
  momentId: string;
  title: string;
  subtype: string;
  currency: string;
  startAt: string | null;
  endAt: string | null;
  channels: string[];
  participants: string[];
  events: RetailEvent[];
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function live(row: RetailEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function text(value: number, known: boolean): string {
  return known ? String(Math.round(value)) : 'not recorded';
}

/** Eight retail, food, and service pages. Missing facts stay “not recorded”. */
export function composeRetailStory(input: RetailStoryInput): StorySnapshot {
  const rows = input.events.filter(live);
  const sales = rows.filter((row) => row.eventType === 'SALE' || row.eventType === 'SERVICE');
  const saleIds = new Set(sales.map((row) => row.operatingEventId));
  const credits = rows
    .filter(
      (row) =>
        row.eventType === 'RETURN' &&
        row.parentEventId != null &&
        saleIds.has(row.parentEventId) &&
        (row.contract?.returnDirection == null || row.contract.returnDirection === 'CUSTOMER')
    )
    .reduce((total, row) => total + num(row.amount), 0);
  const gross = sales.reduce((total, row) => total + num(row.amount), 0);
  const net = Math.max(0, gross - credits);
  const collected = rows
    .filter((row) => row.eventType === 'COLLECTION')
    .reduce((total, row) => total + num(row.amount), 0);
  const paidIn = sales
    .filter((row) => row.paymentStatus === 'COLLECTED')
    .filter((row) => !rows.some((child) => child.parentEventId === row.operatingEventId && child.eventType === 'COLLECTION'))
    .reduce((total, row) => total + num(row.amount), 0);
  const cash = collected + paidIn;
  const receivable = Math.max(0, net - cash);
  const spend = rows.filter((row) => row.eventType === 'SPEND' || row.eventType === 'PURCHASE').reduce((total, row) => total + num(row.amount), 0);
  const paidOut = rows.filter((row) => row.eventType === 'PAYMENT').reduce((total, row) => total + num(row.amount), 0);
  const orders = rows.filter((row) => row.eventType === 'ORDER');
  const openOrders = orders.filter((row) => row.contract?.orderStatus !== 'CANCELLED' && row.contract?.orderStatus !== 'COMPLETED');
  const bookings = rows.filter((row) => row.eventType === 'APPOINTMENT');
  const chargePending = bookings.filter(
    (row) =>
      row.contract?.appointmentStatus === 'COMPLETED' &&
      !rows.some((child) => child.parentEventId === row.operatingEventId && (child.eventType === 'SALE' || child.eventType === 'SERVICE'))
  ).length;
  const stock = stockPositions(input.events);
  const channelNames = new Set(input.channels.map((name) => name.trim().toLowerCase()));
  const people = [
    ...input.participants,
    ...rows
      .map((row) => row.counterpartyName?.trim() ?? '')
      .filter((name) => name && !channelNames.has(name.toLowerCase())),
  ];
  const uniquePeople = [...new Set(people)];
  const revenueKnown = sales.length > 0;
  const composer = getStoryComposer('RETAIL_FOOD_CONSUMER');
  const revenueDetail = revenueKnown
    ? `Net posted revenue ${Math.round(net)}. ${sales.length} posted charges. Orders and bookings are not included.`
    : 'Revenue not recorded.';
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: input.subtype,
      groupFamily: 'RETAIL_CONSUMER',
      familyProfile: 'RETAIL_FOOD_CONSUMER',
      startAt: input.startAt,
      endAt: input.endAt,
      completedAt: null,
      currencyCode: input.currency || 'INR',
    },
    people: uniquePeople.map((name) => ({ userId: null, displayName: name, roleCode: 'PARTICIPANT' })),
    metrics: {
      revenue: text(net, revenueKnown),
      receivable: revenueKnown ? String(Math.round(receivable)) : 'not recorded',
      cash: cash > 0 ? String(Math.round(cash)) : 'not recorded',
      sales: revenueKnown ? String(sales.length) : 'not recorded',
      openOrders: orders.length ? String(openOrders.length) : 'not recorded',
      chargePending: bookings.length ? String(chargePending) : 'not recorded',
    },
    timeline: input.events.slice(0, 12).map((row) => ({ at: row.occurredAt, label: row.title, detail: row.eventType })),
    money: {
      contributed: cash,
      spent: spend,
      remaining: Math.max(0, spend - paidOut),
      unsettled: receivable,
      target: null,
      categories: [],
      contributors: [],
      payers: [],
      expenses: rows
        .filter((row) => row.eventType === 'SPEND' || row.eventType === 'PURCHASE')
        .map((row) => ({
          description: row.title,
          category: row.contract?.expenseCategory || row.eventType,
          payer: row.counterpartyName || 'not recorded',
          amount: num(row.amount),
          at: row.occurredAt,
        })),
    },
    places: [],
    decisions: rows
      .filter((row) => row.eventType === 'APPROVAL' || row.eventType === 'ISSUE')
      .map((row) => ({ title: row.title, status: row.approvalStatus })),
    memories: [],
    photos: [],
    highlights: [
      { id: 'rf-moment', title: 'Business Moment', detail: `${input.title}. ${input.channels.length ? input.channels.join(', ') : 'Channel not recorded.'}` },
      { id: 'rf-motion', title: 'In Motion', detail: input.events.length ? `${input.events.length} posted events.` : 'Activity not recorded.' },
      { id: 'rf-revenue', title: 'Revenue', detail: revenueDetail },
      { id: 'rf-money', title: 'Money', detail: revenueKnown ? `Cash received ${Math.round(cash)}. Open to collect ${Math.round(receivable)}. A channel name is not a customer.` : 'Money not recorded.' },
      { id: 'rf-operations', title: 'Operations', detail: stock.length ? `${stock.length} stock lines from movements.` : orders.length || bookings.length ? 'Commitments recorded. Stock not recorded.' : 'Operations not recorded.' },
      { id: 'rf-people', title: 'People', detail: uniquePeople.length ? uniquePeople.join(', ') : 'People not recorded.' },
      { id: 'rf-attention', title: 'Attention', detail: chargePending ? `${chargePending} completed bookings still need a charge.` : 'No charge is waiting on a completion.' },
      { id: 'rf-sofar', title: 'Story So Far', detail: 'This chapter records what was posted. It does not estimate what was not recorded.' },
    ],
    narrative: {
      opening: `${input.title}. Posted charges are the only revenue. Completing an order or a booking does not post a charge.`,
      insights: ['A quick amount has no stock quantity.'],
    },
    chapters: RETAIL_STORY_CHAPTERS,
    display: composer,
  };
}
