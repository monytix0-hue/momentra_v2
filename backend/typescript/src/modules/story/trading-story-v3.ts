/**
 * Trading / Distribution Business Story v3 — options, preview pages P1–P8,
 * immutable save watermark. Metrics come only from computeTradingFacts (M01–M18).
 */

import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import type { OperatingEventRow } from '../business/operating-catalog';
import { computeTradingFacts, type TradingFactsBundle, type TradingMetricId } from '../business/trading-facts';

export type StoryPeriodPreset =
  | 'THIS_WEEK'
  | 'THIS_MONTH'
  | 'LAST_MONTH'
  | 'QUARTER'
  | 'YEAR'
  | 'SINCE_BEGINNING'
  | 'CUSTOM';

export interface StoryOptions {
  momentId: string;
  mode: 'trading' | 'distribution';
  timezone: string;
  currency: string;
  allowedPeriods: StoryPeriodPreset[];
  gstAvailable: boolean;
  permissions: { canGenerate: boolean; canExport: boolean; canShare: boolean };
}

export interface StoryPreviewRequest {
  startDate: string;
  endDate: string;
  detail?: 'SUMMARY' | 'DETAILED';
  includeGst?: boolean;
  includeRegister?: boolean;
  idempotencyKey?: string;
}

export interface StoryPageViewModel {
  pageId: 'P1' | 'P2' | 'P3' | 'P4' | 'P5' | 'P6' | 'P7' | 'P8';
  title: string;
  hero: { metricId: TradingMetricId | null; value: number | null; label: string; unit: string };
  cards: Array<{ label: string; value: string; metricId?: TradingMetricId }>;
  narrative: string[];
  sourceRefs: string[];
  state: 'ready' | 'empty' | 'partial';
  chart: null;
}

export interface StoryPreviewPayload {
  previewId: string;
  jobId: string;
  status: 'ready' | 'failed' | 'conflict';
  momentId: string;
  mode: 'trading' | 'distribution';
  period: { start: string; end: string; timezone: string; cutoff: string };
  currency: string;
  detail: 'SUMMARY' | 'DETAILED';
  includeGst: boolean;
  pages: StoryPageViewModel[];
  metrics: TradingFactsBundle['metrics'];
  coverage: Array<{ metricId: string; missing: boolean }>;
  warnings: string[];
  businessName: string;
}

type JobRecord = {
  jobId: string;
  previewId: string;
  momentId: string;
  status: 'queued' | 'running' | 'ready' | 'failed' | 'conflict';
  progress: number;
  warnings: string[];
  preview: StoryPreviewPayload | null;
  createdAt: string;
};

type SavedStory = {
  storyId: string;
  version: number;
  preview: StoryPreviewPayload;
  savedAt: string;
  watermark: string;
};

const jobs = new Map<string, JobRecord>();
const previews = new Map<string, StoryPreviewPayload>();
const savedByMoment = new Map<string, SavedStory[]>();

function inPeriod(iso: string, start: string, end: string): boolean {
  const day = iso.slice(0, 10);
  return day >= start.slice(0, 10) && day <= end.slice(0, 10);
}

function fmt(value: number | null, currency: string, missingLabel = 'Not recorded'): string {
  if (value == null) return missingLabel;
  return `₹${Number(value.toFixed(2)).toLocaleString('en-IN', { maximumFractionDigits: 2 })} ${currency}`.trim();
}

function page(
  pageId: StoryPageViewModel['pageId'],
  title: string,
  hero: StoryPageViewModel['hero'],
  cards: StoryPageViewModel['cards'],
  narrative: string[],
  sourceRefs: string[],
  state: StoryPageViewModel['state']
): StoryPageViewModel {
  return { pageId, title, hero, cards, narrative, sourceRefs, state, chart: null };
}

export function composeTradingStoryPages(args: {
  bundle: TradingFactsBundle;
  businessName: string;
  periodLabel: string;
  mode: 'trading' | 'distribution';
  includeGst: boolean;
  events: OperatingEventRow[];
}): StoryPageViewModel[] {
  const m = args.bundle.metrics;
  const net = m.M03.value;
  const collections = m.M04.value;
  const ar = m.M05.value;
  const purchases = m.M06.value;
  const activity = m.M18.value;
  const saleCount = args.events.filter(
    (e) => e.eventType === 'SALE' || e.eventType === 'INVOICE'
  ).length;

  const p1State: StoryPageViewModel['state'] =
    net == null && (activity == null || activity === 0) ? 'empty' : net == null ? 'partial' : 'ready';

  const dist = args.mode === 'distribution';
  const motionEmphasis = dist
    ? 'Supplier procurement → receipt → channel deliveries → settlement.'
    : 'Customer orders → sales → dispatch → collection.';
  const salesNarrative = dist
    ? net == null
      ? 'No net sales to bridge in this period.'
      : `Net sales reached ${fmt(net, args.bundle.currency)} across multi-buyer / channel execution.${
          m.M12.value != null ? ` That is ${m.M12.value}% of the configured target.` : ''
        }`
    : net == null
      ? 'No net sales to bridge in this period.'
      : `Net sales reached ${fmt(net, args.bundle.currency)}.${
          m.M12.value != null ? ` That is ${m.M12.value}% of the configured target.` : ''
        }`;
  const moneyNarrative = dist
    ? `${fmt(collections, args.bundle.currency)} was collected from dealers; ${fmt(
        m.M08.value,
        args.bundle.currency
      )} paid to suppliers; ${fmt(ar, args.bundle.currency)} remained to collect as of cutoff.`
    : `${fmt(collections, args.bundle.currency)} was received during the period; ${fmt(
        ar,
        args.bundle.currency
      )} remained to collect as of cutoff.`;
  const goodsNarrative = dist
    ? purchases == null
      ? 'No purchases recorded in this period.'
      : `${fmt(purchases, args.bundle.currency)} in supplier purchases; inbound receipts and outward channel deliveries tracked separately.`
    : purchases == null
      ? 'No purchases recorded in this period.'
      : `${fmt(purchases, args.bundle.currency)} in purchases was recorded.`;
  const peopleNarrative =
    m.M16.value == null
      ? dist
        ? 'Named principals, dealers or channels were not attributed for coverage.'
        : 'Named counterparties were not attributed for coverage.'
      : dist
        ? `${m.M16.value}% of sales carry named dealer / channel attribution.`
        : `${m.M16.value}% of sales carry named customer attribution.`;

  return [
    page(
      'P1',
      'The Business Moment',
      { metricId: 'M03', value: net, label: 'Net sales', unit: 'INR' },
      [
        { label: 'Sales', value: String(saleCount), metricId: 'M18' },
        { label: 'Activity', value: activity == null ? 'Not recorded' : String(activity), metricId: 'M18' },
        { label: 'Period', value: args.periodLabel },
      ],
      [
        net == null
          ? `No recorded sales in this period${activity ? `; ${activity} verified operational events still count.` : '.'}`
          : `During ${args.periodLabel}, ${args.businessName} recorded ${fmt(net, args.bundle.currency)} in net sales across ${saleCount} sales.`,
      ],
      m.M03.sourceIds,
      p1State
    ),
    page(
      'P2',
      'The Business in Motion',
      { metricId: 'M18', value: activity, label: 'Events', unit: 'count' },
      // Figma P2 glass beats: label = short date, value = event title (chronological).
      (() => {
        const beats = [...args.events]
          .filter((e) => Boolean(e.occurredAt) && Boolean(e.title))
          .sort((a, b) => a.occurredAt.localeCompare(b.occurredAt))
          .slice(0, 8)
          .map((e) => {
            const day = e.occurredAt.slice(0, 10);
            const d = new Date(`${day}T12:00:00Z`);
            const short =
              Number.isNaN(d.getTime())
                ? day.slice(5)
                : d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', timeZone: 'UTC' });
            return { label: short, value: e.title };
          });
        if (beats.length > 0) return beats;
        return [
          { label: 'Net sales', value: fmt(net, args.bundle.currency), metricId: 'M03' as const },
          { label: 'Purchases', value: fmt(purchases, args.bundle.currency), metricId: 'M06' as const },
          { label: 'Collections', value: fmt(collections, args.bundle.currency), metricId: 'M04' as const },
        ];
      })(),
      [motionEmphasis, `${activity ?? 0} posted events tell this chapter in effective-date order.`],
      args.bundle.liveEventIds.slice(0, 40),
      activity == null || activity === 0 ? 'empty' : 'ready'
    ),
    page(
      'P3',
      'The Sales & Order Story',
      { metricId: 'M03', value: net, label: 'Net sales', unit: 'INR' },
      [
        { label: 'Gross', value: fmt(m.M01.value, args.bundle.currency), metricId: 'M01' },
        { label: 'Credits', value: fmt(m.M02.value, args.bundle.currency), metricId: 'M02' },
        {
          label: 'Target',
          value: m.M12.value == null ? 'Not configured' : `${m.M12.value}%`,
          metricId: 'M12',
        },
        {
          label: 'Order remaining',
          value: m.M13.value == null ? 'Not recorded' : String(m.M13.value),
          metricId: 'M13',
        },
      ],
      [salesNarrative],
      [...m.M01.sourceIds, ...m.M13.sourceIds],
      net == null ? 'empty' : 'ready'
    ),
    page(
      'P4',
      'The Money Story',
      { metricId: 'M04', value: collections, label: dist ? 'Dealer collections' : 'Collections', unit: 'INR' },
      [
        { label: 'Receivables', value: fmt(ar, args.bundle.currency), metricId: 'M05' },
        { label: dist ? 'Supplier payables' : 'Payables', value: fmt(m.M09.value, args.bundle.currency), metricId: 'M09' },
        { label: dist ? 'Supplier payments' : 'Payments', value: fmt(m.M08.value, args.bundle.currency), metricId: 'M08' },
        { label: 'Cash movement', value: fmt(m.M10.value, args.bundle.currency), metricId: 'M10' },
        ...(args.includeGst
          ? [{ label: 'GST snapshot', value: fmt(m.M15.value, args.bundle.currency), metricId: 'M15' as const }]
          : []),
      ],
      [moneyNarrative],
      [...m.M04.sourceIds, ...m.M05.sourceIds, ...m.M08.sourceIds],
      collections == null && ar == null ? 'empty' : 'ready'
    ),
    page(
      'P5',
      'Purchases, Goods & Stock',
      { metricId: 'M06', value: purchases, label: dist ? 'Supplier purchases' : 'Purchases', unit: 'INR' },
      [
        {
          label: 'Stock on hand',
          value: m.M11.value == null ? 'Not recorded' : String(m.M11.value),
          metricId: 'M11',
        },
        {
          label: dist ? 'Inbound / outbound gap' : 'Fulfilment gap',
          value: m.M14.value == null ? 'Not recorded' : String(m.M14.value),
          metricId: 'M14',
        },
      ],
      [goodsNarrative],
      [...m.M06.sourceIds, ...m.M11.sourceIds],
      purchases == null && m.M11.value == null ? 'empty' : 'ready'
    ),
    page(
      'P6',
      dist ? 'Principals, Dealers & Channels' : 'People & Channels',
      { metricId: 'M16', value: m.M16.value, label: 'Coverage', unit: 'pct' },
      [
        { label: dist ? 'Dealer AR' : 'Receivables', value: fmt(ar, args.bundle.currency), metricId: 'M05' },
        { label: dist ? 'Supplier AP' : 'Payables', value: fmt(m.M09.value, args.bundle.currency), metricId: 'M09' },
      ],
      [peopleNarrative],
      m.M16.sourceIds,
      m.M16.value == null ? 'partial' : 'ready'
    ),
    page(
      'P7',
      'What Needs Attention',
      { metricId: 'M17', value: m.M17.value, label: 'Overdue', unit: 'INR' },
      [
        { label: 'AR open', value: fmt(ar, args.bundle.currency), metricId: 'M05' },
        { label: 'AP open', value: fmt(m.M09.value, args.bundle.currency), metricId: 'M09' },
        {
          label: dist ? 'Unexecuted orders' : 'Order remaining',
          value: m.M13.value == null ? 'Not recorded' : String(m.M13.value),
          metricId: 'M13',
        },
        {
          label: dist ? 'Goods awaiting move' : 'Fulfilment gap',
          value: m.M14.value == null ? 'Not recorded' : String(m.M14.value),
          metricId: 'M14',
        },
      ],
      [
        `As of cutoff, ${fmt(ar, args.bundle.currency)} remained to collect and ${fmt(
          m.M09.value,
          args.bundle.currency
        )} remained to pay.`,
      ],
      [...m.M17.sourceIds, ...m.M05.sourceIds],
      'ready'
    ),
    page(
      'P8',
      'The Story So Far',
      { metricId: 'M03', value: net, label: 'Net sales', unit: 'INR' },
      [
        { label: 'Collections', value: fmt(collections, args.bundle.currency), metricId: 'M04' },
        { label: 'Receivables', value: fmt(ar, args.bundle.currency), metricId: 'M05' },
      ],
      [
        'Some activity became cash. Some commitments carried forward. This chapter ends here; the business continues.',
      ],
      [...m.M03.sourceIds, ...m.M04.sourceIds, ...m.M05.sourceIds],
      net == null ? 'partial' : 'ready'
    ),
  ];
}

async function loadMomentContext(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  title: string;
  subtype: string;
  mode: 'trading' | 'distribution';
  currency: string;
  timezone: string;
  events: OperatingEventRow[];
  gstAvailable: boolean;
}> {
  const moment = await client.query<{
    title: string;
    moment_type_code: string | null;
    currency_code: string | null;
  }>(
    `SELECT m.title, mt.code AS moment_type_code, bmc.currency_code
     FROM core.moment m
     LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  const row = moment.rows[0];
  if (!row) {
    throw new AppError(ErrorCode.NOT_FOUND, 'Moment not found', 404);
  }
  const access = await client.query(
    `SELECT 1 FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE' LIMIT 1`,
    [momentId, ctx.userId]
  );
  if (access.rows.length === 0) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not a moment participant', 403);
  }

  const subtype = (row.moment_type_code ?? '').toUpperCase();
  const mode: 'trading' | 'distribution' =
    subtype === 'DISTRIBUTION_DEALERSHIP' ? 'distribution' : 'trading';

  const eventsQ = await client.query<{
    operating_event_id: string;
    event_type: string;
    title: string;
    note: string | null;
    amount: string | null;
    currency_code: string | null;
    quantity: string | null;
    payment_status: string;
    approval_status: string;
    payment_relevance: string;
    submitted_by_user_id: string;
    decided_by_user_id: string | null;
    occurred_at: Date;
    parent_event_id: string | null;
    counterparty_name: string | null;
    channel_name: string | null;
    payload: Record<string, unknown> | null;
  }>(
    `SELECT operating_event_id, event_type, title, note, amount::text, currency_code, quantity::text,
            payment_status, approval_status, payment_relevance, submitted_by_user_id, decided_by_user_id,
            occurred_at, parent_event_id, counterparty_name, channel_name, payload
     FROM business.operating_event
     WHERE moment_id = $1
     ORDER BY occurred_at ASC
     LIMIT 500`,
    [momentId]
  );

  const events: OperatingEventRow[] = eventsQ.rows.map((r) => {
    const payload = r.payload ?? {};
    const contract = (payload.contract as OperatingEventRow['contract']) ?? null;
    const dueRaw = payload.dueAt ?? payload.promisedAt;
    return {
      operatingEventId: r.operating_event_id,
      eventType: r.event_type,
      title: r.title,
      note: r.note,
      amount: r.amount,
      quantity: r.quantity,
      paymentStatus: r.payment_status,
      approvalStatus: r.approval_status,
      paymentRelevance: r.payment_relevance,
      submittedByUserId: r.submitted_by_user_id,
      decidedByUserId: r.decided_by_user_id,
      occurredAt: r.occurred_at.toISOString(),
      parentEventId: r.parent_event_id,
      counterpartyName: r.counterparty_name,
      channelName: r.channel_name,
      currencyCode: r.currency_code ?? row.currency_code ?? 'INR',
      dueAt: dueRaw == null ? null : String(dueRaw),
      contract,
      ledgerStatus: payload.ledgerStatus === 'SUPERSEDED' ? 'SUPERSEDED' : null,
    };
  });

  const setup = await client.query<{ preferences: unknown }>(
    `SELECT preferences FROM business.business_system_setup WHERE moment_id = $1 LIMIT 1`,
    [momentId]
  );
  const prefs = (setup.rows[0]?.preferences ?? {}) as Record<string, unknown>;
  const gstAvailable = Boolean(prefs.gstin || prefs.gstRegistered || prefs.gstEnabled);

  return {
    title: row.title,
    subtype,
    mode,
    currency: row.currency_code ?? 'INR',
    timezone: 'Asia/Kolkata',
    events,
    gstAvailable,
  };
}

export async function getTradingStoryOptions(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<StoryOptions> {
  const meta = await loadMomentContext(client, ctx, momentId);
  return {
    momentId,
    mode: meta.mode,
    timezone: meta.timezone,
    currency: meta.currency,
    allowedPeriods: [
      'THIS_WEEK',
      'THIS_MONTH',
      'LAST_MONTH',
      'QUARTER',
      'YEAR',
      'SINCE_BEGINNING',
      'CUSTOM',
    ],
    gstAvailable: meta.gstAvailable,
    permissions: { canGenerate: true, canExport: true, canShare: true },
  };
}

export async function startTradingStoryPreview(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: StoryPreviewRequest
): Promise<{ jobId: string; status: string; previewId: string }> {
  const meta = await loadMomentContext(client, ctx, momentId);
  const start = body.startDate?.slice(0, 10);
  const end = body.endDate?.slice(0, 10);
  if (!start || !end || start > end) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Inclusive start_date/end_date required', 400);
  }

  const periodEvents = meta.events.filter((e) => inPeriod(e.occurredAt, start, end));
  const cutoff = new Date(`${end}T23:59:59.999Z`);
  const bundle = computeTradingFacts({
    events: periodEvents,
    currency: meta.currency,
    asOf: cutoff,
    mode: meta.mode,
    financeVisible: true,
  });

  const periodLabel = `${start} → ${end}`;
  const pages = composeTradingStoryPages({
    bundle,
    businessName: meta.title,
    periodLabel,
    mode: meta.mode,
    includeGst: Boolean(body.includeGst) && meta.gstAvailable,
    events: periodEvents,
  });

  const previewId = randomUUID();
  const jobId = randomUUID();
  const preview: StoryPreviewPayload = {
    previewId,
    jobId,
    status: 'ready',
    momentId,
    mode: meta.mode,
    period: { start, end, timezone: meta.timezone, cutoff: cutoff.toISOString() },
    currency: meta.currency,
    detail: body.detail === 'DETAILED' ? 'DETAILED' : 'SUMMARY',
    includeGst: Boolean(body.includeGst) && meta.gstAvailable,
    pages,
    metrics: bundle.metrics,
    coverage: Object.values(bundle.metrics).map((m) => ({ metricId: m.id, missing: m.missing })),
    warnings: bundle.warnings,
    businessName: meta.title,
  };

  previews.set(previewId, preview);
  jobs.set(jobId, {
    jobId,
    previewId,
    momentId,
    status: 'ready',
    progress: 100,
    warnings: bundle.warnings,
    preview,
    createdAt: new Date().toISOString(),
  });

  return { jobId, status: 'ready', previewId };
}

export function getTradingStoryJob(jobId: string): JobRecord {
  const job = jobs.get(jobId);
  if (!job) throw new AppError(ErrorCode.NOT_FOUND, 'Story job not found', 404);
  return job;
}

export function getTradingStoryPreview(previewId: string): StoryPreviewPayload {
  const preview = previews.get(previewId);
  if (!preview) throw new AppError(ErrorCode.NOT_FOUND, 'Story preview not found', 404);
  return preview;
}

export function saveTradingStoryPreview(previewId: string): {
  storyId: string;
  version: number;
  snapshotWatermark: string;
  savedAt: string;
} {
  const preview = getTradingStoryPreview(previewId);
  const prior = savedByMoment.get(preview.momentId) ?? [];
  const version = prior.length + 1;
  const storyId = randomUUID();
  const savedAt = new Date().toISOString();
  const watermark = `trd-v3:${preview.momentId}:${preview.period.cutoff}:v${version}`;
  const saved: SavedStory = { storyId, version, preview, savedAt, watermark };
  prior.push(saved);
  savedByMoment.set(preview.momentId, prior);
  return { storyId, version, snapshotWatermark: watermark, savedAt };
}

export function getSavedTradingStory(storyId: string): SavedStory {
  for (const list of savedByMoment.values()) {
    const hit = list.find((s) => s.storyId === storyId);
    if (hit) return hit;
  }
  throw new AppError(ErrorCode.NOT_FOUND, 'Story not found', 404);
}

export async function listSavedTradingStories(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<Array<{ storyId: string; version: number; savedAt: string; period: StoryPreviewPayload['period'] }>> {
  await loadMomentContext(client, ctx, momentId);
  return (savedByMoment.get(momentId) ?? []).map((s) => ({
    storyId: s.storyId,
    version: s.version,
    savedAt: s.savedAt,
    period: s.preview.period,
  }));
}
