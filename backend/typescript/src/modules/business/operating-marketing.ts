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

export interface MarketingFacts {
  reportingMode: 'INCREMENTAL' | 'CUMULATIVE' | null;
  metricCode: string | null;
  budgetCategory: string | null;
  paymentState: 'PAID' | 'PART_PAID' | 'PAY_LATER' | null;
  activityStatus: 'OPEN' | 'DONE' | null;
  attributedSales: string | null;
  revisionKind: 'BUDGET' | 'TARGET' | 'CATEGORY' | null;
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
    activityStatus: activity === 'OPEN' || activity === 'DONE' ? activity : null,
    attributedSales: rec.attributedSales == null || rec.attributedSales === '' ? null : String(rec.attributedSales),
    revisionKind: revision === 'BUDGET' || revision === 'TARGET' || revision === 'CATEGORY' ? revision : null,
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

export function seriesValue(rows: MarketingEvent[]): number {
  const ordered = [...rows].sort((a, b) => a.occurredAt.localeCompare(b.occurredAt));
  let incremental = 0;
  let cumulative: number | null = null;
  for (const row of ordered) {
    const mode = factsOf(row).reportingMode ?? 'INCREMENTAL';
    const value = measure(row);
    if (mode === 'CUMULATIVE') cumulative = value;
    else incremental += value;
  }
  return cumulative != null ? cumulative : incremental;
}

/** Compatible observations only. Empty coverage is N/A, never a fabricated zero result. */
export function marketingProgress(events: MarketingEvent[], targetMetric: string): { progress: number; coverage: string } {
  const metric = targetMetric.toUpperCase();
  const rows = events.filter((row) => {
    if (!effective(row)) return false;
    if (row.eventType !== 'RESULT' && row.eventType !== 'PROGRESS') return false;
    const code = metricOf(row);
    if (metric === 'REVENUE' || metric === 'CLIENT_BILLING') {
      return code !== 'LEADS' && code !== 'CONVERSIONS';
    }
    return code === metric;
  });
  if (rows.length === 0) return { progress: 0, coverage: 'N/A' };
  const units = new Set(rows.map((row) => (row.unit ?? '').trim().toLowerCase()).filter(Boolean));
  if (units.size > 1) return { progress: 0, coverage: 'N/A' };
  const groups = new Map<string, MarketingEvent[]>();
  for (const row of rows) {
    const key = `${metricOf(row)}|${(row.unit ?? '').trim().toLowerCase()}|${row.channelName ?? ''}`;
    groups.set(key, [...(groups.get(key) ?? []), row]);
  }
  let total = 0;
  for (const group of groups.values()) total += seriesValue(group);
  return { progress: total, coverage: `${rows.length} recorded` };
}

export function rollMarketingLedger(
  events: MarketingEvent[],
  seed: { budget: number; target: number }
): {
  budget: number;
  target: number;
  budgetConfigured: boolean;
  targetConfigured: boolean;
  spent: number;
  revisions: Array<{ id: string; title: string; occurredAt: string }>;
} {
  let budget = seed.budget;
  let target = seed.target;
  let budgetConfigured = budget > 0;
  let targetConfigured = target > 0;
  const revisions: Array<{ id: string; title: string; occurredAt: string }> = [];
  const ordered = [...events].filter(effective).sort((a, b) => a.occurredAt.localeCompare(b.occurredAt));
  for (const row of ordered) {
    const amount = num(row.amount);
    if (row.eventType === 'BUDGET' && amount > 0) {
      budget = amount;
      budgetConfigured = true;
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
    } else if (row.eventType === 'TARGET' && amount > 0) {
      target = amount;
      targetConfigured = true;
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
    } else if (row.eventType === 'REVISION') {
      const kind = factsOf(row).revisionKind ?? 'BUDGET';
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
      if (kind === 'TARGET' && amount > 0) {
        target = amount;
        targetConfigured = true;
      } else if (kind === 'BUDGET' && amount > 0) {
        budget = amount;
        budgetConfigured = true;
      }
    }
  }
  const spent = ordered.reduce((total, row) => {
    if (row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST') return total + num(row.amount);
    return total;
  }, 0);
  return { budget, target, budgetConfigured, targetConfigured, spent, revisions };
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

/** ATT-01–ATT-09 from the v3 Needs Attention contract. */
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
    if (row.eventType !== 'ACTIVITY' || !effective(row) || factsOf(row).activityStatus === 'DONE') continue;
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
  activityStatus: 'OPEN' | 'DONE' | null;
  attributedSales: string | null;
}

export function validateMarketingWrite(
  input: MarketingWrite,
  existing: MarketingEvent[]
): { ok: true } | { ok: false; message: string } {
  const client = input.subtype === 'CLIENT_CAMPAIGN';
  if ((input.eventType === 'INVOICE' || input.eventType === 'COLLECTION') && !client) {
    return { ok: false, message: 'Own-business campaigns do not book invoices or collections.' };
  }
  if (input.eventType === 'INVOICE' && !(num(input.amount) > 0)) {
    return { ok: false, message: 'Invoice amount is required.' };
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
    if (factsOf(parent).activityStatus === 'DONE') {
      return { ok: false, message: 'That activity is already done.' };
    }
    if (!(num(input.quantity) > 0) && !(num(input.amount) > 0)) {
      return { ok: false, message: 'Progress needs a quantity or amount.' };
    }
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
  }
  return { ok: true };
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
