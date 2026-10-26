/**
 * Marketing Campaigns v3 ledger rules.
 * Client campaigns invoice and collect. Own-business campaigns record results
 * and attribution only — attributed sales are never booked revenue.
 */

/** Contract §3.1 channel names. Offline channels are included; impressions are never required. */
export const MARKETING_CHANNEL_LOV = [
  'Search Ads',
  'Paid Social',
  'Display / Video Ads',
  'Organic Social',
  'SEO / Content',
  'Email',
  'SMS / Messaging',
  'Cold Calling',
  'Field Sales / Visits',
  'Newspaper / Magazine',
  'Flyers / Brochures',
  'Radio / Podcast',
  'Billboard / Transit',
  'Exhibition / Roadshow',
  'Influencer',
  'Affiliate / Referral',
  'Custom Channel',
] as const;

/** Contract §3.2 budget categories. */
export const MARKETING_BUDGET_CATEGORIES = [
  'DIGITAL_ADS',
  'SOCIAL_CONTENT',
  'INFLUENCER',
  'EMAIL_DIRECT',
  'PRINT',
  'RADIO_AUDIO',
  'OUTDOOR',
  'EVENTS',
  'OUTREACH',
  'CREATIVE',
  'RESEARCH',
  'OTHER',
] as const;

export const MARKETING_METRICS = [
  'LEADS',
  'CONVERSIONS',
  'REVENUE',
  'ORDERS',
  'REACH',
  'ENGAGEMENT',
  'CUSTOM_COUNT',
  'CLIENT_BILLING',
] as const;

const COUNT_METRICS = new Set(['LEADS', 'CONVERSIONS', 'ORDERS', 'REACH', 'ENGAGEMENT', 'CUSTOM_COUNT']);

export function isMarketingSubtype(subtype: string | null | undefined): boolean {
  const code = String(subtype ?? '').toUpperCase();
  return code === 'CLIENT_CAMPAIGN' || code === 'OWN_BUSINESS_CAMPAIGN';
}

export type MarketingActivityStatus =
  | 'OPEN'
  | 'DONE'
  | 'IN_PROGRESS'
  | 'BLOCKED'
  | 'POSTPONED'
  | 'COMPLETED'
  | 'CANCELLED';

export interface MarketingFacts {
  reportingMode: 'INCREMENTAL' | 'CUMULATIVE' | null;
  metricCode: string | null;
  budgetCategory: string | null;
  paymentState: 'PAID' | 'PART_PAID' | 'PAY_LATER' | null;
  activityStatus: MarketingActivityStatus | null;
  attributedSales: string | null;
  revisionKind: 'BUDGET' | 'TARGET' | 'CATEGORY' | null;
  costBasis?: 'AGENCY' | 'REIMBURSABLE' | null;
  normalizedDelta?: string | null;
  overlapDeduped?: boolean;
  resultSource?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  supersedesEventId?: string | null;
  creativeVersion?: string | null;
  effectiveFrom?: string | null;
  evidenceUrl?: string | null;
  idempotencyKey?: string | null;
}

export interface MarketingEvent {
  operatingEventId: string;
  eventType: string;
  title: string;
  amount: string | null;
  quantity?: string | null;
  unit?: string | null;
  channelName?: string | null;
  counterpartyName?: string | null;
  paymentStatus: string;
  approvalStatus: string;
  occurredAt: string;
  parentEventId?: string | null;
  dueAt?: string | null;
  metric?: string | null;
  marketing?: MarketingFacts | null;
}

function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function money(value: number): string {
  return value.toFixed(4);
}

function effective(row: MarketingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function day(value: string | null | undefined): string {
  return String(value ?? '').slice(0, 10);
}

const ACTIVITY_STATUSES = new Set<MarketingActivityStatus>([
  'OPEN',
  'DONE',
  'IN_PROGRESS',
  'BLOCKED',
  'POSTPONED',
  'COMPLETED',
  'CANCELLED',
]);

export function activityClosed(status: string | null | undefined): boolean {
  return status === 'DONE' || status === 'COMPLETED' || status === 'CANCELLED';
}

function activityStatusOf(value: string): MarketingActivityStatus | null {
  return ACTIVITY_STATUSES.has(value as MarketingActivityStatus) ? (value as MarketingActivityStatus) : null;
}

export function parseMarketingFacts(raw: unknown, metric: string | null): MarketingFacts | null {
  if (!raw || typeof raw !== 'object') {
    if (!metric) return null;
    return {
      reportingMode: null,
      metricCode: metric,
      budgetCategory: null,
      paymentState: null,
      activityStatus: null,
      attributedSales: null,
      revisionKind: null,
    };
  }
  const rec = raw as Record<string, unknown>;
  const mode = String(rec.reportingMode ?? '').toUpperCase();
  const state = String(rec.paymentState ?? '').toUpperCase();
  const activity = String(rec.activityStatus ?? '').toUpperCase();
  const revision = String(rec.revisionKind ?? '').toUpperCase();
  const metricCode = String(rec.metricCode ?? metric ?? '').toUpperCase();
  return {
    reportingMode: mode === 'INCREMENTAL' || mode === 'CUMULATIVE' ? mode : null,
    metricCode: metricCode || null,
    budgetCategory: rec.budgetCategory == null ? null : String(rec.budgetCategory).toUpperCase(),
    paymentState: state === 'PAID' || state === 'PART_PAID' || state === 'PAY_LATER' ? state : null,
    activityStatus: activityStatusOf(activity),
    attributedSales: rec.attributedSales == null || rec.attributedSales === '' ? null : String(rec.attributedSales),
    revisionKind: revision === 'BUDGET' || revision === 'TARGET' || revision === 'CATEGORY' ? revision : null,
    costBasis: rec.costBasis === 'REIMBURSABLE' ? 'REIMBURSABLE' : rec.costBasis === 'AGENCY' ? 'AGENCY' : null,
    normalizedDelta: rec.normalizedDelta == null || rec.normalizedDelta === '' ? null : String(rec.normalizedDelta),
    overlapDeduped: rec.overlapDeduped === true,
    resultSource: rec.resultSource == null || rec.resultSource === '' ? null : String(rec.resultSource),
    periodStart: rec.periodStart == null ? null : String(rec.periodStart).slice(0, 10),
    periodEnd: rec.periodEnd == null ? null : String(rec.periodEnd).slice(0, 10),
    supersedesEventId: rec.supersedesEventId == null || rec.supersedesEventId === '' ? null : String(rec.supersedesEventId),
    creativeVersion: rec.creativeVersion == null || rec.creativeVersion === '' ? null : String(rec.creativeVersion),
    effectiveFrom: rec.effectiveFrom == null ? null : String(rec.effectiveFrom).slice(0, 10),
    evidenceUrl: rec.evidenceUrl == null || rec.evidenceUrl === '' ? null : String(rec.evidenceUrl),
    idempotencyKey: rec.idempotencyKey == null || rec.idempotencyKey === '' ? null : String(rec.idempotencyKey),
  };
}

function factsOf(row: MarketingEvent): MarketingFacts {
  return (
    row.marketing ?? {
      reportingMode: null,
      metricCode: row.metric ?? null,
      budgetCategory: null,
      paymentState: null,
      activityStatus: null,
      attributedSales: null,
      revisionKind: null,
    }
  );
}

function metricOf(row: MarketingEvent): string {
  return (factsOf(row).metricCode || row.metric || '').toUpperCase();
}

function measure(row: MarketingEvent): number {
  const metric = metricOf(row);
  if (COUNT_METRICS.has(metric) || (row.quantity != null && row.quantity !== '' && metric !== 'REVENUE' && metric !== 'CLIENT_BILLING')) {
    return num(row.quantity ?? row.amount);
  }
  return num(row.amount);
}

function supersededIds(rows: MarketingEvent[]): Set<string> {
  const ids = new Set<string>();
  for (const row of rows) {
    const id = factsOf(row).supersedesEventId;
    if (id && rows.some((item) => item.operatingEventId === id)) ids.add(id);
  }
  return ids;
}

function measuredProgress(row: MarketingEvent): boolean {
  if (row.eventType !== 'PROGRESS') return true;
  return num(row.quantity) > 0 || num(row.amount) > 0;
}

/** Sum of normalized deltas. A later cumulative snapshot contributes only the increase. */
export function seriesValue(rows: MarketingEvent[]): number {
  const ordered = [...rows].sort((a, b) => a.occurredAt.localeCompare(b.occurredAt) || a.operatingEventId.localeCompare(b.operatingEventId));
  const replaced = supersededIds(ordered);
  const seen = new Set<string>();
  let total = 0;
  let priorCumulative: number | null = null;
  for (const row of ordered) {
    if (replaced.has(row.operatingEventId)) continue;
    const facts = factsOf(row);
    const key = facts.idempotencyKey;
    if (key) {
      if (seen.has(key)) continue;
      seen.add(key);
    }
    if (facts.normalizedDelta != null && facts.normalizedDelta !== '') {
      total += num(facts.normalizedDelta);
      if ((facts.reportingMode ?? 'INCREMENTAL') === 'CUMULATIVE') priorCumulative = measure(row);
      continue;
    }
    const value = measure(row);
    if ((facts.reportingMode ?? 'INCREMENTAL') === 'CUMULATIVE') {
      const delta = priorCumulative == null ? value : value - priorCumulative;
      if (delta >= 0) total += delta;
      priorCumulative = value;
    } else {
      total += value;
    }
  }
  return total;
}

export function observationDelta(prior: MarketingEvent[], row: MarketingEvent): number {
  const facts = factsOf(row);
  if (facts.idempotencyKey && prior.some((item) => factsOf(item).idempotencyKey === facts.idempotencyKey)) return 0;
  if ((facts.reportingMode ?? 'INCREMENTAL') !== 'CUMULATIVE') return measure(row);
  const same = prior
    .filter((item) => effective(item) && (item.eventType === 'RESULT' || item.eventType === 'PROGRESS'))
    .filter((item) => metricOf(item) === metricOf(row))
    .filter((item) => (item.unit ?? '') === (row.unit ?? ''))
    .filter((item) => (item.channelName ?? '') === (row.channelName ?? ''))
    .filter((item) => (factsOf(item).resultSource ?? '') === (facts.resultSource ?? ''))
    .filter((item) => (factsOf(item).reportingMode ?? 'INCREMENTAL') === 'CUMULATIVE')
    .filter((item) => item.operatingEventId !== facts.supersedesEventId)
    .filter((item) => !supersededIds(prior).has(item.operatingEventId))
    .sort((a, b) => a.occurredAt.localeCompare(b.occurredAt));
  const previous = same.length ? measure(same[same.length - 1]!) : null;
  const value = measure(row);
  if (previous == null) return value;
  return value >= previous ? value - previous : 0;
}

/** Compatible observations only. Empty coverage is N/A, never a fabricated zero result. */
export function marketingProgress(events: MarketingEvent[], targetMetric: string): { progress: number; coverage: string } {
  const metric = targetMetric.toUpperCase();
  const rows = events.filter((row) => {
    if (!effective(row)) return false;
    if (row.eventType !== 'RESULT' && row.eventType !== 'PROGRESS') return false;
    if (!measuredProgress(row)) return false;
    const code = metricOf(row);
    if (metric === 'REVENUE' || metric === 'CLIENT_BILLING') {
      return code !== 'LEADS' && code !== 'CONVERSIONS';
    }
    return code === metric;
  });
  if (rows.length === 0) return { progress: 0, coverage: 'N/A' };
  const units = new Set(rows.map((row) => (row.unit ?? '').trim().toLowerCase()).filter(Boolean));
  if (units.size > 1) return { progress: 0, coverage: 'N/A' };
  if (metric === 'REACH') {
    const channels = new Set(rows.map((row) => row.channelName ?? ''));
    const deduped = rows.every((row) => factsOf(row).overlapDeduped === true);
    if (channels.size > 1 && !deduped) return { progress: 0, coverage: 'N/A' };
  }
  const groups = new Map<string, MarketingEvent[]>();
  for (const row of rows) {
    const key = `${metricOf(row)}|${(row.unit ?? '').trim().toLowerCase()}|${row.channelName ?? ''}`;
    groups.set(key, [...(groups.get(key) ?? []), row]);
  }
  let total = 0;
  for (const group of groups.values()) total += seriesValue(group);
  return { progress: total, coverage: `${rows.length} recorded` };
}

function inForce(row: MarketingEvent, asOf: string | null): boolean {
  if (!asOf) return true;
  const from = factsOf(row).effectiveFrom || day(row.occurredAt);
  return !from || from <= asOf;
}

export function rollMarketingLedger(
  events: MarketingEvent[],
  seed: { budget: number; target: number },
  asOf: string | null = null,
  primaryMetric = ''
): {
  budget: number;
  target: number;
  budgetConfigured: boolean;
  targetConfigured: boolean;
  spent: number;
  reimbursable: number;
  supportingTargets: Record<string, number>;
  revisions: Array<{ id: string; title: string; occurredAt: string }>;
} {
  let budget = seed.budget;
  let target = seed.target;
  let budgetConfigured = budget > 0;
  let targetConfigured = target > 0;
  const supportingTargets: Record<string, number> = {};
  const primary = primaryMetric.toUpperCase();
  const revisions: Array<{ id: string; title: string; occurredAt: string }> = [];
  const ordered = [...events].filter(effective).sort((a, b) => a.occurredAt.localeCompare(b.occurredAt));
  for (const row of ordered) {
    if ((row.eventType === 'BUDGET' || row.eventType === 'TARGET' || row.eventType === 'REVISION') && !inForce(row, asOf)) continue;
    const amount = num(row.amount);
    if (row.eventType === 'BUDGET' && amount > 0) {
      budget = amount;
      budgetConfigured = true;
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
    } else if (row.eventType === 'TARGET' && amount > 0) {
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
      const metric = metricOf(row);
      if (metric && primary && metric !== primary) supportingTargets[metric] = amount;
      else {
        target = amount;
        targetConfigured = true;
      }
    } else if (row.eventType === 'REVISION') {
      const kind = factsOf(row).revisionKind ?? 'BUDGET';
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
      if (kind === 'TARGET' && amount > 0) {
        const metric = metricOf(row);
        if (metric && primary && metric !== primary) supportingTargets[metric] = amount;
        else {
          target = amount;
          targetConfigured = true;
        }
      } else if (kind === 'BUDGET' && amount > 0) {
        budget = amount;
        budgetConfigured = true;
      }
    }
  }
  const spent = ordered.reduce((total, row) => {
    if (row.eventType !== 'SPEND' && row.eventType !== 'VENDOR_COST') return total;
    if (factsOf(row).costBasis === 'REIMBURSABLE') return total;
    return total + num(row.amount);
  }, 0);
  const reimbursable = ordered.reduce((total, row) => {
    if (row.eventType !== 'SPEND' && row.eventType !== 'VENDOR_COST') return total;
    if (factsOf(row).costBasis !== 'REIMBURSABLE') return total;
    return total + num(row.amount);
  }, 0);
  return { budget, target, budgetConfigured, targetConfigured, spent, reimbursable, supportingTargets, revisions };
}

function obligationRemaining(row: MarketingEvent, events: MarketingEvent[], childType: 'PAYMENT' | 'COLLECTION'): number {
  if (row.paymentStatus === 'PAID' || row.paymentStatus === 'COLLECTED' || row.paymentStatus === 'SETTLED') return 0;
  const settled = events
    .filter((child) => child.parentEventId === row.operatingEventId && child.eventType === childType && effective(child))
    .reduce((total, child) => total + num(child.amount), 0);
  return Math.max(0, num(row.amount) - settled);
}

function daysAfter(earlier: string, later: string): number {
  const start = Date.parse(`${earlier}T00:00:00Z`);
  const end = Date.parse(`${later}T00:00:00Z`);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return 0;
  return Math.round((end - start) / 86400000);
}

function currentActivityStatus(row: MarketingEvent, events: MarketingEvent[]): MarketingActivityStatus | null {
  const updates = events
    .filter(
      (item) =>
        item.eventType === 'PROGRESS' &&
        item.parentEventId === row.operatingEventId &&
        effective(item) &&
        factsOf(item).activityStatus
    )
    .sort((a, b) => a.occurredAt.localeCompare(b.occurredAt));
  const latest = updates.length ? factsOf(updates[updates.length - 1]!).activityStatus : null;
  return latest ?? factsOf(row).activityStatus;
}

/** ATT-01–ATT-09 from the v3 Needs Attention contract. ATT-10 and ATT-11 keep their shipped meanings. */
export function marketingAttention(args: {
  events: MarketingEvent[];
  subtype: string;
  visible: boolean;
  canApprove: boolean;
  asOf: Date;
  budget: number;
  spent: number;
  budgetConfigured: boolean;
  target: number;
  progress: number;
  targetConfigured: boolean;
  progressCoverage: string;
  startDate: string | null;
  endDate: string | null;
  campaignStatus?: string | null;
}): Array<{
  id: string;
  title: string;
  kind: string;
  severity: string;
  amount: string | null;
  dueAt: string | null;
  counterpartyName: string | null;
  sourceEventId: string | null;
  cta: string | null;
}> {
  const today = args.asOf.toISOString().slice(0, 10);
  const soon = new Date(args.asOf);
  soon.setUTCDate(soon.getUTCDate() + 3);
  const soonDay = soon.toISOString().slice(0, 10);
  const cards: Array<{
    id: string;
    title: string;
    kind: string;
    severity: string;
    amount: string | null;
    dueAt: string | null;
    counterpartyName: string | null;
    sourceEventId: string | null;
    cta: string | null;
  }> = [];
  const client = args.subtype === 'CLIENT_CAMPAIGN';
  for (const row of args.events) {
    const status = currentActivityStatus(row, args.events);
    if (row.eventType !== 'ACTIVITY' || !effective(row) || activityClosed(status)) continue;
    if (status === 'BLOCKED') {
      cards.push({
        id: `blocked-${row.operatingEventId}`,
        title: `Blocked · ${row.title}`,
        kind: 'ATT-10',
        severity: 'HIGH',
        amount: null,
        dueAt: day(row.dueAt) || null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'activity',
      });
    }
    if (client && row.title.toLowerCase().startsWith('client report') && !activityClosed(status)) {
      cards.push({
        id: `report-${row.operatingEventId}`,
        title: `Client report due · ${row.title}`,
        kind: 'ATT-11',
        severity: 'MEDIUM',
        amount: null,
        dueAt: day(row.dueAt) || null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'activity',
      });
    }
    const due = day(row.dueAt);
    if (!due) continue;
    if (due < today) {
      const late = daysAfter(due, today);
      cards.push({
        id: `activity-${row.operatingEventId}`,
        title: `Activity overdue · ${row.title} · ${late} days`,
        kind: 'ATT-01',
        severity: 'HIGH',
        amount: null,
        dueAt: due,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'activity',
      });
    } else if (due <= soonDay) {
      cards.push({
        id: `activity-${row.operatingEventId}`,
        title: `Due soon · ${row.title}`,
        kind: 'ATT-02',
        severity: 'MEDIUM',
        amount: null,
        dueAt: due,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'activity',
      });
    }
    if (!row.counterpartyName) {
      cards.push({
        id: `owner-${row.operatingEventId}`,
        title: 'Assign an owner',
        kind: 'ATT-09',
        severity: 'LOW',
        amount: null,
        dueAt: due,
        counterpartyName: null,
        sourceEventId: row.operatingEventId,
        cta: 'activity',
      });
    }
  }
  for (const row of args.events) {
    if (row.approvalStatus !== 'PENDING' || !args.canApprove) continue;
    if (!['SPEND', 'PAYMENT', 'VENDOR_COST', 'APPROVAL'].includes(row.eventType)) continue;
    const due = day(row.dueAt);
    cards.push({
      id: row.operatingEventId,
      title: row.title,
      kind: 'ATT-03',
      severity: due && due < today ? 'HIGH' : 'MEDIUM',
      amount: args.visible ? row.amount : null,
      dueAt: row.dueAt ?? null,
      counterpartyName: row.counterpartyName ?? null,
      sourceEventId: row.operatingEventId,
      cta: 'approval',
    });
  }
  if (args.visible) {
    for (const row of args.events) {
      if (!effective(row)) continue;
      const payable = row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST';
      const receivable = client && (row.eventType === 'INVOICE' || row.eventType === 'SALE');
      if (!payable && !receivable) continue;
      const open = obligationRemaining(row, args.events, payable ? 'PAYMENT' : 'COLLECTION');
      if (!(open > 0)) continue;
      const due = day(row.dueAt);
      if (!due || due >= today) continue;
      const party = row.counterpartyName || 'Unnamed';
      cards.push({
        id: `open-${row.operatingEventId}`,
        title: payable ? `Payment overdue · ${party} · ${money(open)}` : `Collection overdue · ${party} · ${money(open)}`,
        kind: payable ? 'ATT-04' : 'ATT-05',
        severity: 'HIGH',
        amount: money(open),
        dueAt: due,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: payable ? 'pay' : 'collect',
      });
    }
    if (args.budgetConfigured && args.budget > 0) {
      const ratio = args.spent / args.budget;
      if (ratio > 1) {
        cards.push({
          id: 'budget-over',
          title: `Over budget by ${money(args.spent - args.budget)}`,
          kind: 'ATT-06',
          severity: 'HIGH',
          amount: money(args.spent - args.budget),
          dueAt: null,
          counterpartyName: null,
          sourceEventId: null,
          cta: 'budget',
        });
      } else if (ratio >= 0.9) {
        cards.push({
          id: 'budget-warn',
          title: 'Budget nearly used',
          kind: 'ATT-07',
          severity: 'MEDIUM',
          amount: money(args.budget - args.spent),
          dueAt: null,
          counterpartyName: null,
          sourceEventId: null,
          cta: 'budget',
        });
      }
    }
  }
  const start = day(args.startDate);
  const end = day(args.endDate);
  if (
    args.targetConfigured &&
    args.target > 0 &&
    args.progressCoverage !== 'N/A' &&
    start &&
    end &&
    end > start
  ) {
    const span = daysAfter(start, end);
    const elapsed = daysAfter(start, today);
    if (span > 0 && elapsed / span >= 0.75 && args.progress / args.target < 0.5) {
      cards.push({
        id: 'behind-target',
        title: 'Campaign behind target',
        kind: 'ATT-08',
        severity: 'MEDIUM',
        amount: null,
        dueAt: null,
        counterpartyName: null,
        sourceEventId: null,
        cta: 'activity',
      });
    }
  }
  if ((args.campaignStatus ?? '').toUpperCase() === 'CLOSED') {
    for (const row of args.events) {
      if (!effective(row)) continue;
      const openActivity = row.eventType === 'ACTIVITY' && !activityClosed(currentActivityStatus(row, args.events));
      const pending = row.approvalStatus === 'PENDING';
      const payable = (row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST') && obligationRemaining(row, args.events, 'PAYMENT') > 0;
      const receivable = client && (row.eventType === 'INVOICE' || row.eventType === 'SALE') && obligationRemaining(row, args.events, 'COLLECTION') > 0;
      if (!openActivity && !pending && !payable && !receivable) continue;
      cards.push({
        id: `close-${row.operatingEventId}`,
        title: `Still open · ${row.title}`,
        kind: 'ATT-12',
        severity: 'HIGH',
        amount: args.visible && (payable || receivable) ? money(obligationRemaining(row, args.events, payable ? 'PAYMENT' : 'COLLECTION')) : null,
        dueAt: day(row.dueAt) || null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: payable ? 'pay' : receivable ? 'collect' : 'activity',
      });
    }
  }
  for (const row of args.events) {
    if (row.eventType !== 'RESULT' || !effective(row)) continue;
    const facts = factsOf(row);
    if (facts.resultSource && facts.periodEnd) continue;
    cards.push({
      id: `evidence-${row.operatingEventId}`,
      title: `Result evidence · ${row.title}`,
      kind: 'ATT-13',
      severity: 'MEDIUM',
      amount: null,
      dueAt: null,
      counterpartyName: null,
      sourceEventId: row.operatingEventId,
      cta: 'result',
    });
  }
  const rank: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
  return cards.sort((a, b) => (rank[a.severity] ?? 9) - (rank[b.severity] ?? 9) || a.kind.localeCompare(b.kind));
}

export interface MarketingWrite {
  eventType: string;
  subtype: string;
  amount: string | null;
  quantity: string | null;
  unit: string | null;
  parentEventId: string | null;
  metricCode: string | null;
  reportingMode: 'INCREMENTAL' | 'CUMULATIVE' | null;
  revisionKind: 'BUDGET' | 'TARGET' | 'CATEGORY' | null;
  activityStatus: MarketingActivityStatus | null;
  attributedSales: string | null;
  costBasis?: 'AGENCY' | 'REIMBURSABLE' | null;
  creativeVersion?: string | null;
  idempotencyKey?: string | null;
  counterpartyName?: string | null;
  campaignClient?: string | null;
  channelName?: string | null;
  resultSource?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  supersedesEventId?: string | null;
}

function sameCumulativeSeries(input: MarketingWrite, row: MarketingEvent): boolean {
  if (!effective(row) || (row.eventType !== 'RESULT' && row.eventType !== 'PROGRESS')) return false;
  if ((factsOf(row).reportingMode ?? 'INCREMENTAL') !== 'CUMULATIVE') return false;
  if ((input.reportingMode ?? 'INCREMENTAL') !== 'CUMULATIVE') return false;
  const metric = (input.metricCode ?? '').toUpperCase();
  if (metric && metricOf(row) !== metric) return false;
  if ((input.unit ?? '') !== (row.unit ?? '')) return false;
  if ((input.channelName ?? '') !== (row.channelName ?? '')) return false;
  if ((input.resultSource ?? '') !== (factsOf(row).resultSource ?? '')) return false;
  return true;
}

function overlappingResult(
  input: MarketingWrite,
  existing: MarketingEvent[]
): { ok: true } | { ok: false; message: string } {
  const start = (input.periodStart ?? '').slice(0, 10);
  const end = (input.periodEnd ?? '').slice(0, 10);
  if (!start || !end) return { ok: true };
  const clashes = existing.filter((row) => {
    if (!sameCumulativeSeries(input, row)) return false;
    const facts = factsOf(row);
    const otherStart = facts.periodStart ?? '';
    const otherEnd = facts.periodEnd ?? '';
    if (!otherStart || !otherEnd) return false;
    return start <= otherEnd && otherStart <= end;
  });
  if (clashes.some((row) => row.operatingEventId !== input.supersedesEventId)) {
    return { ok: false, message: 'That result overlaps a posted period. Correct the earlier snapshot.' };
  }
  return { ok: true };
}

export function validateMarketingWrite(
  input: MarketingWrite,
  existing: MarketingEvent[]
): { ok: true } | { ok: false; message: string } {
  const client = input.subtype === 'CLIENT_CAMPAIGN';
  if ((input.eventType === 'INVOICE' || input.eventType === 'COLLECTION') && !client) {
    return { ok: false, message: 'Own-business campaigns do not book invoices or collections.' };
  }
  if (!client && input.costBasis === 'REIMBURSABLE') {
    return { ok: false, message: 'Own-business campaigns do not record client-reimbursable spend.' };
  }
  if (input.idempotencyKey && existing.some((row) => factsOf(row).idempotencyKey === input.idempotencyKey)) {
    return { ok: true };
  }
  if (input.eventType === 'PROGRESS' && input.attributedSales) {
    return { ok: false, message: 'Progress does not post a result.' };
  }
  if (input.eventType === 'INVOICE' && !(num(input.amount) > 0)) {
    return { ok: false, message: 'Invoice amount is required.' };
  }
  if (input.eventType === 'INVOICE' && input.campaignClient && input.counterpartyName) {
    if (input.counterpartyName.trim().toLowerCase() !== input.campaignClient.trim().toLowerCase()) {
      return { ok: false, message: 'Invoice the campaign client.' };
    }
  }
  if (input.eventType === 'RESULT' && input.attributedSales && !client) {
    // Informational only. Never a reason to reject, and never a receivable.
  }
  if (input.eventType === 'REVISION') {
    if (!input.revisionKind) return { ok: false, message: 'Choose what this revision changes.' };
    if (input.revisionKind !== 'CATEGORY' && !(num(input.amount) > 0)) {
      return { ok: false, message: 'Revision amount is required.' };
    }
  }
  if (input.eventType === 'PROGRESS') {
    if (!input.parentEventId) return { ok: false, message: 'Progress belongs to an open activity.' };
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent || parent.eventType !== 'ACTIVITY' || !effective(parent)) {
      return { ok: false, message: 'Progress belongs to an open activity.' };
    }
    if (activityClosed(currentActivityStatus(parent, existing))) {
      return { ok: false, message: 'That activity is already done.' };
    }
    const statusOnly = !(num(input.quantity) > 0) && !(num(input.amount) > 0);
    if (statusOnly && !input.activityStatus) {
      return { ok: false, message: 'Progress needs a status or a measured result.' };
    }
    if (!statusOnly) {
    const overlap = overlappingResult(input, existing);
    if (!overlap.ok) return overlap;
    const prior = existing.filter(
      (row) =>
        row.eventType === 'PROGRESS' &&
        effective(row) &&
        metricOf(row) === (input.metricCode ?? '').toUpperCase() &&
        row.parentEventId === input.parentEventId
    );
    const units = new Set(prior.map((row) => (row.unit ?? '').trim().toLowerCase()).filter(Boolean));
    const nextUnit = (input.unit ?? '').trim().toLowerCase();
    if (nextUnit && units.size > 0 && !units.has(nextUnit)) {
      return { ok: false, message: 'Do not mix units in one progress series.' };
    }
    }
  }
  if (input.eventType === 'RESULT') {
    const overlap = overlappingResult(input, existing);
    if (!overlap.ok) return overlap;
  }
  if (input.eventType === 'PAYMENT' || input.eventType === 'COLLECTION') {
    if (!input.parentEventId) return { ok: false, message: 'Choose the open record this settles.' };
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    const wanted = input.eventType === 'PAYMENT' ? ['SPEND', 'VENDOR_COST'] : ['INVOICE', 'SALE'];
    if (!parent || !wanted.includes(parent.eventType) || !effective(parent)) {
      return { ok: false, message: 'Choose an open spend or invoice on this campaign.' };
    }
    const child = input.eventType === 'PAYMENT' ? 'PAYMENT' : 'COLLECTION';
    const open = obligationRemaining(parent, existing, child);
    if (num(input.amount) - open > 0.00005) {
      return { ok: false, message: 'That is more than the amount still open.' };
    }
    const party = (input.counterpartyName ?? '').trim();
    const parentParty = (parent.counterpartyName ?? '').trim();
    if (party && parentParty && party.toLowerCase() !== parentParty.toLowerCase()) {
      return { ok: false, message: 'Use the party already on that record.' };
    }
  }
  return { ok: true };
}

const FINANCIAL_EVENTS = new Set(['SPEND', 'VENDOR_COST', 'PAYMENT', 'INVOICE', 'COLLECTION', 'SALE']);

export function marketingModeLocked(events: MarketingEvent[]): boolean {
  return events.some((row) => effective(row) && FINANCIAL_EVENTS.has(row.eventType));
}

export function assertMarketingSubtypeStable(
  previous: string,
  next: string,
  events: MarketingEvent[]
): { ok: true } | { ok: false; message: string } {
  if (previous.toUpperCase() === next.toUpperCase()) return { ok: true };
  if (!marketingModeLocked(events)) return { ok: true };
  return { ok: false, message: 'The campaign mode cannot change after money has been posted.' };
}

export function creativeVersionApproved(events: MarketingEvent[], version: string): boolean {
  const wanted = version.trim();
  if (!wanted) return false;
  return events.some(
    (row) =>
      row.eventType === 'APPROVAL' &&
      row.approvalStatus === 'APPROVED' &&
      effective(row) &&
      factsOf(row).creativeVersion === wanted
  );
}

export function marketingCpl(events: MarketingEvent[]): string | null {
  const rows = events.filter(effective);
  const spendRows = rows.filter(
    (row) => (row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST') && factsOf(row).costBasis !== 'REIMBURSABLE'
  );
  const leadRows = rows.filter(
    (row) => (row.eventType === 'RESULT' || row.eventType === 'PROGRESS') && metricOf(row) === 'LEADS'
  );
  const channels = new Set([...spendRows, ...leadRows].map((row) => row.channelName ?? ''));
  if (channels.size > 1) return null;
  const measured = marketingProgress(events, 'LEADS');
  if (measured.coverage === 'N/A') return null;
  const leads = measured.progress;
  if (!(leads > 0)) return null;
  const spend = spendRows.reduce((total, row) => total + num(row.amount), 0);
  return (spend / leads).toFixed(2);
}

export function marketingAchievement(actual: number, target: number | null, covered: boolean): string | null {
  if (!covered || target == null || !(target > 0)) return null;
  return ((actual / target) * 100).toFixed(2);
}

export function attributedSalesTotal(events: MarketingEvent[]): number | null {
  const rows = events.filter(
    (row) => effective(row) && (row.eventType === 'RESULT' || row.eventType === 'PROGRESS') && factsOf(row).attributedSales != null
  );
  if (rows.length === 0) return null;
  const groups = new Map<string, MarketingEvent[]>();
  for (const row of rows) {
    const facts = factsOf(row);
    const key = `${row.channelName ?? ''}|${facts.resultSource ?? ''}`;
    const standIn: MarketingEvent = {
      ...row,
      amount: facts.attributedSales ?? null,
      quantity: null,
      marketing: { ...facts, metricCode: 'REVENUE' },
    };
    groups.set(key, [...(groups.get(key) ?? []), standIn]);
  }
  let total = 0;
  for (const group of groups.values()) total += seriesValue(group);
  return total;
}

export function marketingRoas(events: MarketingEvent[]): string | null {
  const rows = events.filter(effective);
  const spend = rows
    .filter((row) => (row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST') && factsOf(row).costBasis !== 'REIMBURSABLE')
    .reduce((total, row) => total + num(row.amount), 0);
  const sales = attributedSalesTotal(rows);
  if (!(spend > 0) || sales == null) return null;
  return (sales / spend).toFixed(2);
}

export function marketingMetricTarget(
  primaryMetric: string,
  primaryValue: number,
  primaryConfigured: boolean,
  supporting: Record<string, number>,
  metric: string
): number | null {
  const code = metric.toUpperCase();
  if (primaryMetric.toUpperCase() === code && primaryConfigured) return primaryValue;
  return supporting[code] ?? null;
}

/** Marketing has no tax ledger. A missing or unconfigured rate is never stored as 0%. */
export function marketingTaxSnapshot(args: {
  gstEnabled: boolean;
  amount: string | null;
  gstRate?: string | null;
}): { amount: string | null; gst: null } {
  const rate = args.gstRate == null || String(args.gstRate).trim() === '' ? null : Number(args.gstRate);
  if (!args.gstEnabled || rate == null || !(rate > 0)) return { amount: args.amount, gst: null };
  return { amount: args.amount, gst: null };
}

export function channelCoverage(channels: string[], events: MarketingEvent[]): string {
  if (channels.length === 0) return 'N/A';
  const used = new Set(
    events
      .filter((row) => effective(row) && row.channelName && ['RESULT', 'PROGRESS', 'ACTIVITY', 'SPEND'].includes(row.eventType))
      .map((row) => row.channelName as string)
  );
  const hit = channels.filter((channel) => used.has(channel)).length;
  return `${hit} / ${channels.length} channels`;
}
