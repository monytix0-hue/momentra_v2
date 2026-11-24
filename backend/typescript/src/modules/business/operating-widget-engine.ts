/**
 * P3 shared analytics engine — coverage states + eligible-source filtering.
 *
 * Callers: operating-widget-catalog.ts, operating-widget-service.ts,
 *   tests/business-p3-*.test.ts (projectWidgets / getWidgetSources).
 * No existing file serves COMPLETE|PARTIAL|UNAVAILABLE|ZERO for operating widgets
 * (analytics/engine.ts is S8 personal metrics; ledger-projection is AR/AP only).
 * Reads: in-memory OperatingEventRow / V104 posting completeness flags. Writes: none.
 * User: "Distinguish COMPLETE, PARTIAL, UNAVAILABLE and genuine zero states."
 * User: "If eligible backfill residual is positive or historical coverage is incomplete,
 *   affected metrics must be null/unavailable rather than zero."
 */

export type CoverageStatus = 'COMPLETE' | 'PARTIAL' | 'UNAVAILABLE' | 'ZERO';

export type WidgetScreen = 'PULSE' | 'MOMENTS' | 'LIFE' | 'MEMORY';

export type WidgetMetricValue = {
  key: string;
  label: string;
  /** Null when coverage is UNAVAILABLE / PARTIAL for financial totals. */
  value: string | number | null;
  unit: string | null;
  coverage: CoverageStatus;
};

export type WidgetDefinition = {
  widgetId: string;
  screen: WidgetScreen;
  title: string;
  familyCodes: string[];
  subtypes: string[] | '*';
  eligibleEventTypes: string[];
  /** When true, TEAM_MEMBER / OBSERVER / CONTRIBUTOR see redacted values. */
  financeOnly: boolean;
  formula: string;
  periodPolicy: 'AS_OF_RANGE' | 'CAMPAIGN_WINDOW' | 'ALL_TIME';
  currencyPolicy: 'MOMENT_CURRENCY' | 'NA';
  historicalRule: string;
  /**
   * Figma section code for clients (e.g. AN-P01). Evaluation keeps `widgetId` (P01).
   * Set for marketing presentation; omit when the industry has no AN-* frame.
   */
  displayCode?: string | null;
};

export type WidgetInstance = WidgetDefinition & {
  metrics: WidgetMetricValue[];
  sourceEventIds: string[];
  coverage: CoverageStatus;
  why: string | null;
};

export type EligibleEventLike = {
  operatingEventId: string;
  eventType: string;
  amount: string | null;
  approvalStatus: string;
  paymentStatus: string;
  ledgerStatus?: 'SUPERSEDED' | null;
  parentEventId?: string | null;
};

/** Effective for finance/analytics: exclude pending/rejected/cancelled + SUPERSEDED + REVERSAL. */
export function isEffectiveForAnalytics(row: EligibleEventLike): boolean {
  if (row.ledgerStatus === 'SUPERSEDED') return false;
  if (row.eventType === 'REVERSAL') return false;
  return (
    row.approvalStatus !== 'PENDING' &&
    row.approvalStatus !== 'REJECTED' &&
    row.paymentStatus !== 'CANCELLED'
  );
}

export function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

export function money(value: number): string {
  return value.toFixed(2);
}

/**
 * Resolve coverage for a metric given eligible sources and optional posting completeness.
 * - no eligible sources → UNAVAILABLE (not zero)
 * - residual incomplete / incomplete posting coverage → PARTIAL or UNAVAILABLE
 * - eligible sources, all zero amounts → ZERO (genuine zero)
 * - otherwise COMPLETE
 */
export function resolveCoverage(args: {
  eligibleCount: number;
  measuredValue: number | null;
  postingComplete?: boolean | null;
  residualIncomplete?: boolean;
  forceUnavailable?: boolean;
}): CoverageStatus {
  if (args.forceUnavailable) return 'UNAVAILABLE';
  if (args.residualIncomplete) return 'UNAVAILABLE';
  if (args.postingComplete === false) return 'PARTIAL';
  if (args.eligibleCount <= 0) return 'UNAVAILABLE';
  if (args.measuredValue == null) return 'UNAVAILABLE';
  if (args.measuredValue === 0) return 'ZERO';
  return 'COMPLETE';
}

/** Display value: UNAVAILABLE/PARTIAL financial metrics stay null; ZERO renders 0. */
export function coverageValue(
  coverage: CoverageStatus,
  raw: number | string | null,
  opts?: { allowPartialNumber?: boolean }
): string | number | null {
  if (coverage === 'UNAVAILABLE') return null;
  if (coverage === 'PARTIAL' && !opts?.allowPartialNumber) return null;
  if (raw == null) return null;
  return raw;
}

export function filterEligible(
  events: EligibleEventLike[],
  types: string[]
): EligibleEventLike[] {
  const set = new Set(types.map((t) => t.toUpperCase()));
  return events.filter((e) => isEffectiveForAnalytics(e) && set.has(e.eventType.toUpperCase()));
}

export function sumAmounts(events: EligibleEventLike[]): number {
  return events.reduce((total, row) => total + num(row.amount), 0);
}
