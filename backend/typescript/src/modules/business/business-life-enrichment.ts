/**
 * Company Business Life enrichment — module scores, typed signals, trends.
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import {
  getCapacity,
  getMomDeltas,
} from '../business/business-closure-reads';
import { computeHealthScore, computeTeamScore, computeOpsScore } from '../business/business-projection';
import { loadOpsPulseExtras } from '../business/operations-precision';

export type LifeSignal = {
  signalId: string;
  signalType: string;
  title: string;
  family: string;
  statusLabel: string;
  severity?: string;
  metricValue?: number | string | null;
};

export type LifeTrendPoint = {
  month: string;
  financialHealthScore: number | null;
  teamScore: number | null;
  runwayScore: number | null;
  opsScore: number | null;
};

function mapFamily(raw: string | null | undefined): string {
  const f = (raw ?? '').toUpperCase();
  if (f.includes('TEAM')) return 'TEAM_OPS';
  if (f.includes('RUNWAY')) return 'RUNWAY';
  if (f.includes('OPERATIONS')) return 'OPERATIONS';
  return 'OPERATIONS';
}

function severityToSignal(severity: string): string {
  const s = severity.toUpperCase();
  if (s === 'CRITICAL' || s === 'HIGH') return 'Action';
  if (s === 'MEDIUM') return 'Watch';
  return 'Healthy';
}

function signalRank(statusLabel: string): number {
  if (statusLabel === 'Action') return 0;
  if (statusLabel === 'Watch') return 1;
  return 2;
}

function parseMoneyish(v: unknown): number {
  if (v == null) return 0;
  const s = String(v).replace(/[₹,\s]/g, '');
  const n = parseFloat(s);
  return Number.isFinite(n) ? n : 0;
}

export const LIFE_MONEY_FAMILY = 'BUSINESS_RUNWAY';
export const LIFE_DAILY_FAMILY = 'BUSINESS_OPERATIONS';
export const LIFE_TEAM_FAMILY = 'TEAM_OPERATIONS';

/** Earliest active moment of a family, then the lower moment id. Not the caller moment. */
export async function resolveCanonicalFamilyMoment(
  client: PoolClient,
  companyId: string,
  businessFamily: string
): Promise<string | null> {
  const row = await client.query<{ moment_id: string }>(
    `SELECT moment_id
     FROM business.business_moment_context
     WHERE company_id = $1 AND status = 'ACTIVE' AND business_family = $2
     ORDER BY created_at ASC, moment_id ASC
     LIMIT 1`,
    [companyId, businessFamily]
  );
  return row.rows[0]?.moment_id ?? null;
}

async function loadFamilyPrefs(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<Record<string, unknown>> {
  const prefsRow = await client.query<{ preferences: Record<string, unknown> }>(
    `SELECT preferences FROM business.business_system_setup
     WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
     ORDER BY updated_at DESC LIMIT 1`,
    [companyId, momentId]
  );
  return prefsRow.rows[0]?.preferences ?? {};
}

function warningThresholdMonths(prefs: Record<string, unknown>): number {
  if (prefs.warningThreshold == null || prefs.warningThreshold === '') return 6;
  return parseMoneyish(prefs.warningThreshold);
}

export async function loadLifeTrendSeries(
  client: PoolClient,
  companyId: string
): Promise<{ status: string; series: LifeTrendPoint[] }> {
  const rows = await client
    .query<{
      period_month: Date;
      financial_health_score: string | null;
      team_score: string | null;
      runway_score: string | null;
      ops_score: string | null;
    }>(
      `SELECT period_month,
              financial_health_score::text,
              team_score::text,
              runway_score::text,
              ops_score::text
       FROM projection.business_pulse_history
       WHERE company_id = $1
       ORDER BY period_month DESC
       LIMIT 6`,
      [companyId]
    )
    .catch(() => ({
      rows: [] as Array<{
        period_month: Date;
        financial_health_score: string | null;
        team_score: string | null;
        runway_score: string | null;
        ops_score: string | null;
      }>,
    }));

  const series = rows.rows
    .map((r) => ({
      month: r.period_month.toISOString().slice(0, 7),
      financialHealthScore: r.financial_health_score != null ? Math.round(parseFloat(r.financial_health_score)) : null,
      teamScore: r.team_score != null ? Math.round(parseFloat(r.team_score)) : null,
      runwayScore: r.runway_score != null ? Math.round(parseFloat(r.runway_score)) : null,
      opsScore: r.ops_score != null ? Math.round(parseFloat(r.ops_score)) : null,
    }))
    .reverse();

  return {
    status: series.length >= 2 ? 'OK' : series.length === 1 ? 'EMPTY_SUPPORTED' : 'EMPTY_SUPPORTED',
    series,
  };
}

export async function assembleTypedSignals(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  issueRows: Array<{
    issue_id: string;
    title: string;
    severity: string;
    business_family: string | null;
  }>
): Promise<LifeSignal[]> {
  const signals: LifeSignal[] = issueRows.map((r) => ({
    signalId: r.issue_id,
    signalType: 'issue',
    title: r.title,
    family: mapFamily(r.business_family),
    statusLabel: severityToSignal(r.severity),
    severity: r.severity,
  }));

  const [teamMomentId, moneyMomentId, dailyMomentId] = await Promise.all([
    resolveCanonicalFamilyMoment(client, companyId, LIFE_TEAM_FAMILY),
    resolveCanonicalFamilyMoment(client, companyId, LIFE_MONEY_FAMILY),
    resolveCanonicalFamilyMoment(client, companyId, LIFE_DAILY_FAMILY),
  ]);

  if (teamMomentId) {
    const capacity = await getCapacity(client, ctx, teamMomentId);
    if (capacity.capacityPct != null && capacity.capacityPct < 40) {
      signals.push({
        signalId: `capacity-${companyId}`,
        signalType: 'capacity',
        title: `Team capacity at ${capacity.capacityPct}%`,
        family: 'TEAM_OPS',
        statusLabel: capacity.capacityPct < 25 ? 'Action' : 'Watch',
        metricValue: capacity.capacityPct,
      });
    }
  }

  const pulse = await client.query<{ runway_months: string | null }>(
    `SELECT runway_months::text FROM projection.business_pulse WHERE company_id = $1`,
    [companyId]
  );
  const runwayMonths = pulse.rows[0]?.runway_months != null ? parseFloat(pulse.rows[0].runway_months) : null;
  if (moneyMomentId) {
    const prefs = await loadFamilyPrefs(client, companyId, moneyMomentId);
    const warningThreshold = warningThresholdMonths(prefs);
    if (runwayMonths != null && runwayMonths < warningThreshold) {
      signals.push({
        signalId: `runway-${companyId}`,
        signalType: 'runway',
        title: `Runway ${runwayMonths} months below ${warningThreshold} month target`,
        family: 'RUNWAY',
        statusLabel: runwayMonths < warningThreshold / 2 ? 'Action' : 'Watch',
        metricValue: runwayMonths,
      });
    }
  }

  if (dailyMomentId) {
    const ops = await loadOpsPulseExtras(client, companyId, dailyMomentId);
    const prefs = await loadFamilyPrefs(client, companyId, dailyMomentId);
    const budgetRaw = prefs.monthlyBudget ?? prefs.monthlySpending;
    const budgetNum = budgetRaw != null ? parseMoneyish(budgetRaw) : 0;
    const spendNum = ops.monthlySpend != null ? parseFloat(ops.monthlySpend) : null;
    if (budgetNum > 0 && spendNum != null && spendNum > budgetNum) {
      const pct = Math.round((spendNum / budgetNum) * 100);
      signals.push({
        signalId: `budget-${companyId}`,
        signalType: 'budget',
        title: `Monthly spend at ${pct}% of budget`,
        family: 'OPERATIONS',
        statusLabel: pct > 110 ? 'Action' : 'Watch',
        metricValue: pct,
      });
    }

    if (ops.slaCompliancePct != null && ops.slaCompliancePct < 90) {
      signals.push({
        signalId: `sla-${companyId}`,
        signalType: 'sla',
        title: `SLA compliance ${ops.slaCompliancePct}%`,
        family: 'OPERATIONS',
        statusLabel: ops.slaCompliancePct < 75 ? 'Action' : 'Watch',
        metricValue: ops.slaCompliancePct,
      });
    }
  }

  const overdue = await client
    .query<{ invoice_id: string; invoice_number: string; n: string }>(
      `SELECT invoice_id, invoice_number, total_amount::text AS n
       FROM finance.invoice
       WHERE company_id = $1
         AND status IN ('ISSUED','PARTIALLY_PAID','OVERDUE')
         AND due_date IS NOT NULL
         AND due_date < CURRENT_DATE
       ORDER BY due_date ASC
       LIMIT 3`,
      [companyId]
    )
    .catch(() => ({ rows: [] as Array<{ invoice_id: string; invoice_number: string; n: string }> }));

  for (const inv of overdue.rows) {
    signals.push({
      signalId: inv.invoice_id,
      signalType: 'invoice',
      title: `Overdue invoice ${inv.invoice_number}`,
      family: 'RUNWAY',
      statusLabel: 'Action',
      metricValue: inv.n,
    });
  }

  return signals
    .sort((a, b) => signalRank(a.statusLabel) - signalRank(b.statusLabel))
    .slice(0, 8);
}

export async function computeLifeModuleScores(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  pulse: {
    runway_months: string | null;
    financial_health_score: string | null;
  } | undefined
): Promise<{
  teamScore: number | null;
  runwayScore: number | null;
  opsScore: number | null;
  vendorScore: number | null;
  capacityPct: number | null;
  revenueMomPct: number | null;
  expenseMomPct: number | null;
}> {
  const [teamMomentId, moneyMomentId, dailyMomentId] = await Promise.all([
    resolveCanonicalFamilyMoment(client, companyId, LIFE_TEAM_FAMILY),
    resolveCanonicalFamilyMoment(client, companyId, LIFE_MONEY_FAMILY),
    resolveCanonicalFamilyMoment(client, companyId, LIFE_DAILY_FAMILY),
  ]);

  const [capacity, mom, ops, moneyPrefs] = await Promise.all([
    teamMomentId ? getCapacity(client, ctx, teamMomentId) : Promise.resolve({ capacityPct: null as number | null }),
    moneyMomentId
      ? getMomDeltas(client, ctx, moneyMomentId)
      : Promise.resolve({ revenueMomPct: null as number | null, expenseMomPct: null as number | null }),
    dailyMomentId ? loadOpsPulseExtras(client, companyId, dailyMomentId) : Promise.resolve(null),
    moneyMomentId ? loadFamilyPrefs(client, companyId, moneyMomentId) : Promise.resolve({}),
  ]);

  const fin = await client.query<{ expense_total: string; revenue_total: string }>(
    `SELECT expense_total::text, revenue_total::text
     FROM projection.business_finance_snapshot WHERE company_id = $1 LIMIT 1`,
    [companyId]
  );
  const dailyPrefs = dailyMomentId ? await loadFamilyPrefs(client, companyId, dailyMomentId) : {};
  const expenseTotal = parseFloat(fin.rows[0]?.expense_total ?? '0');
  const revenueTotal = parseFloat(fin.rows[0]?.revenue_total ?? '0');
  const runwayMonths = pulse?.runway_months != null ? parseFloat(pulse.runway_months) : null;

  const teamScore = teamMomentId ? await computeTeamScore(client, companyId, teamMomentId) : null;
  const runwayScore =
    pulse?.financial_health_score != null
      ? Math.round(parseFloat(pulse.financial_health_score))
      : moneyMomentId
        ? computeHealthScore(runwayMonths, moneyPrefs, expenseTotal, revenueTotal)
        : null;
  const opsScore = ops ? computeOpsScore(ops.slaCompliancePct, ops.monthlySpend, dailyPrefs) : null;
  const vendorScore =
    ops == null
      ? null
      : ops.slaCompliancePct != null && ops.activeVendorCount > 0
        ? Math.round((ops.slaCompliancePct + Math.min(100, ops.activeVendorCount * 10)) / 2)
        : ops.activeVendorCount > 0
          ? Math.min(100, ops.activeVendorCount * 15)
          : null;

  return {
    teamScore,
    runwayScore,
    opsScore,
    vendorScore,
    capacityPct: capacity.capacityPct,
    revenueMomPct: mom.revenueMomPct,
    expenseMomPct: mom.expenseMomPct,
  };
}

export function formatScore(score: number | null): string | null {
  return score != null ? String(score) : null;
}

export { mapFamily };
