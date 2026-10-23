import { MARKETING_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';
import { marketingProgress, type MarketingEvent } from '../business/operating-marketing';

export interface MarketingStoryInput {
  momentId: string;
  title: string;
  subtype: string;
  currency: string;
  startAt: string | null;
  endAt: string | null;
  targetMetric: string;
  budget?: number | null;
  target?: number | null;
  channels: string[];
  participants: string[];
  events: MarketingEvent[];
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function shown(value: number, known: boolean): string {
  return known ? String(Math.round(value)) : 'N/A';
}

function effectiveResult(row: MarketingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

/** Linked result quantity over eligible result quantity. Missing channel is unattributed, not zero. */
function resultCoverage(events: MarketingEvent[], targetMetric: string): string {
  const metric = targetMetric.toUpperCase();
  const rows = events.filter((row) => {
    if (!effectiveResult(row)) return false;
    if (row.eventType !== 'RESULT' && row.eventType !== 'PROGRESS') return false;
    const code = (row.marketing?.metricCode || row.metric || '').toUpperCase();
    if (metric === 'REVENUE' || metric === 'CLIENT_BILLING') return code !== 'LEADS' && code !== 'CONVERSIONS';
    return code === metric;
  });
  if (rows.length === 0) return 'N/A';
  const value = (row: MarketingEvent) => num(row.quantity ?? row.amount);
  const total = rows.reduce((sum, row) => sum + value(row), 0);
  if (!(total > 0)) return 'N/A';
  const linked = rows.filter((row) => row.channelName).reduce((sum, row) => sum + value(row), 0);
  return `${Math.round(linked)} / ${Math.round(total)}`;
}

/** Eight campaign pages. Missing facts stay N/A. Own campaigns never claim booked revenue. */
export function composeMarketingStory(input: MarketingStoryInput): StorySnapshot {
  const client = input.subtype === 'CLIENT_CAMPAIGN';
  const measured = marketingProgress(input.events, input.targetMetric || 'LEADS');
  const spendRows = input.events.filter((row) => row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST');
  const spent = spendRows.reduce((total, row) => total + num(row.amount), 0);
  const paid = input.events
    .filter((row) => row.eventType === 'PAYMENT')
    .reduce((total, row) => total + num(row.amount), 0);
  const invoiced = client
    ? input.events.filter((row) => row.eventType === 'INVOICE' || row.eventType === 'SALE').reduce((total, row) => total + num(row.amount), 0)
    : 0;
  const collected = client
    ? input.events.filter((row) => row.eventType === 'COLLECTION').reduce((total, row) => total + num(row.amount), 0)
    : 0;
  const coverage = resultCoverage(input.events, input.targetMetric || 'LEADS');
  const budget = Number(input.budget ?? 0);
  const target = Number(input.target ?? 0);
  const utilization = budget > 0 && spendRows.length ? `${Math.round((spent / budget) * 100)}%` : 'N/A';
  const attainment = target > 0 && measured.coverage !== 'N/A' ? `${Math.round((measured.progress / target) * 100)}%` : 'N/A';
  const open = input.events.filter((row) => row.approvalStatus === 'PENDING' || (row.eventType === 'ACTIVITY' && row.marketing?.activityStatus !== 'DONE')).length;
  const resultsLabel = measured.coverage === 'N/A' ? 'N/A' : shown(measured.progress, true);
  const timeline = input.events.slice(0, 12).map((row) => ({
    at: row.occurredAt,
    label: row.title,
    detail: row.eventType,
  }));
  const categories = new Map<string, number>();
  for (const row of spendRows) {
    const name = row.marketing?.budgetCategory || 'OTHER';
    categories.set(name, (categories.get(name) ?? 0) + num(row.amount));
  }
  const composer = getStoryComposer('MARKETING_CAMPAIGN');
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: input.subtype,
      groupFamily: 'MARKETING_CAMPAIGNS',
      familyProfile: 'MARKETING_CAMPAIGN',
      startAt: input.startAt,
      endAt: input.endAt,
      completedAt: null,
      currencyCode: input.currency || 'INR',
    },
    people: input.participants.map((name) => ({ userId: null, displayName: name, roleCode: 'PARTICIPANT' })),
    metrics: {
      results: resultsLabel,
      spent: spendRows.length ? Math.round(spent) : 'N/A',
      paid: paid > 0 ? Math.round(paid) : 'N/A',
      channels: coverage,
      budgetUse: utilization,
      attainment,
      open: String(open),
      invoices: client ? Math.round(invoiced) : 'N/A',
    },
    timeline,
    money: {
      contributed: client ? collected : 0,
      spent,
      remaining: Math.max(0, spent - paid),
      unsettled: client ? Math.max(0, invoiced - collected) : 0,
      target: target > 0 ? target : null,
      categories: [...categories.entries()].map(([name, amount]) => ({ name, amount })),
      contributors: [],
      payers: [],
      expenses: spendRows.map((row) => ({
        description: row.title,
        category: row.marketing?.budgetCategory || 'OTHER',
        payer: row.counterpartyName || 'Campaign',
        amount: num(row.amount),
        at: row.occurredAt,
      })),
    },
    places: [],
    decisions: input.events
      .filter((row) => row.eventType === 'APPROVAL' || row.eventType === 'ISSUE')
      .map((row) => ({ title: row.title, status: row.approvalStatus })),
    memories: [],
    photos: [],
    highlights: [
      { id: 'mc-moment', title: 'Campaign Moment', detail: measured.coverage === 'N/A' ? `${input.title}. Campaign created; no activity recorded for this period.` : `${input.title}. ${resultsLabel} ${input.targetMetric} was recorded.` },
      { id: 'mc-motion', title: 'In Motion', detail: timeline.length ? `${timeline.length} posted events.` : 'No recorded activity.' },
      { id: 'mc-results', title: 'Results', detail: measured.coverage === 'N/A' ? 'No compatible results yet.' : target > 0 ? `${resultsLabel} ${input.targetMetric}. Target ${target}. Attainment ${attainment}.` : `${resultsLabel} ${input.targetMetric}.` },
      { id: 'mc-money', title: 'Money', detail: client ? `Incurred ${Math.round(spent)}. Paid ${Math.round(paid)}. Invoices stay separate from results.` : `Incurred ${spendRows.length ? Math.round(spent) : 'not recorded'}. Paid ${paid > 0 ? Math.round(paid) : 'not recorded'}. Attributed sales are not booked revenue.` },
      { id: 'mc-channels', title: 'Channels', detail: coverage === 'N/A' ? 'No channel-linked results.' : `Channel coverage ${coverage}.` },
      { id: 'mc-people', title: 'People', detail: input.participants.length ? input.participants.join(', ') : 'No people recorded.' },
      { id: 'mc-attention', title: 'Attention', detail: open ? `At period end, ${open} recorded items remained open.` : 'No open items recorded.' },
      { id: 'mc-sofar', title: 'Story So Far', detail: 'This chapter records what happened. The next moment builds on it.' },
    ],
    narrative: {
      opening: client
        ? `${input.title} is a client campaign. Results and invoices are kept apart.`
        : `${input.title} is an own-business campaign. Results are not booked revenue.`,
      insights: measured.coverage === 'N/A' ? ['Results are not available until a compatible series is posted.'] : [],
    },
    chapters: MARKETING_STORY_CHAPTERS,
    display: composer,
  };
}
