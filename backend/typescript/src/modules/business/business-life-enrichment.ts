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
  momentId: string,
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

  const capacity = await getCapacity(client, ctx, momentId);
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

  const pulse = await client.query<{ runway_months: string | null }>(
    `SELECT runway_months::text FROM projection.business_pulse WHERE company_id = $1`,
    [companyId]
  );
  const runwayMonths = pulse.rows[0]?.runway_months != null ? parseFloat(pulse.rows[0].runway_months) : null;
  const prefsRow = await client.query<{ preferences: Record<string, unknown> }>(
    `SELECT preferences FROM business.business_system_setup
     WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
     ORDER BY updated_at DESC LIMIT 1`,
    [companyId, momentId]
  );
  const prefs = prefsRow.rows[0]?.preferences ?? {};
  const warningThreshold = parseMoneyish(prefs.warningThreshold) || 6;
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

  const ops = await loadOpsPulseExtras(client, companyId, momentId);
  const budgetRaw = prefs.monthlyBudget ?? prefs.monthlySpending;
  const budgetNum = budgetRaw != null ? parseMoneyish(budgetRaw) : 0;
  const spendNum = parseFloat(ops.monthlySpend ?? '0');
  if (budgetNum > 0 && spendNum > budgetNum) {
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
  momentId: string,
  companyId: string,
  pulse: {
    runway_months: string | null;
    financial_health_score: string | null;
  } | undefined,
  teamPayload: Record<string, unknown>,
  runwayPayload: Record<string, unknown>,
  opsPayload: Record<string, unknown>
): Promise<{
  teamScore: number | null;
  runwayScore: number | null;
  opsScore: number | null;
  vendorScore: number | null;
  capacityPct: number | null;
  revenueMomPct: number | null;
  expenseMomPct: number | null;
}> {
  const [capacity, mom, ops] = await Promise.all([
    getCapacity(client, ctx, momentId),
    getMomDeltas(client, ctx, momentId),
    loadOpsPulseExtras(client, companyId, momentId),
  ]);

  const fin = await client.query<{ expense_total: string; revenue_total: string }>(
    `SELECT expense_total::text, revenue_total::text
     FROM projection.business_finance_snapshot WHERE company_id = $1 LIMIT 1`,
    [companyId]
  );
  const prefsRow = await client.query<{ preferences: Record<string, unknown> }>(
    `SELECT preferences FROM business.business_system_setup
     WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
     ORDER BY updated_at DESC LIMIT 1`,
    [companyId, momentId]
  );
  const prefs = prefsRow.rows[0]?.preferences ?? {};
  const expenseTotal = parseFloat(fin.rows[0]?.expense_total ?? '0');
  const revenueTotal = parseFloat(fin.rows[0]?.revenue_total ?? '0');
  const runwayMonths = pulse?.runway_months != null ? parseFloat(pulse.runway_months) : null;

  const teamScore = await computeTeamScore(client, companyId, momentId);
  const runwayScore =
    pulse?.financial_health_score != null
      ? Math.round(parseFloat(pulse.financial_health_score))
      : computeHealthScore(runwayMonths, prefs, expenseTotal, revenueTotal);
  const opsScore = computeOpsScore(ops.slaCompliancePct, ops.monthlySpend, prefs);
  const vendorScore =
    ops.slaCompliancePct != null && ops.activeVendorCount > 0
      ? Math.round((ops.slaCompliancePct + Math.min(100, ops.activeVendorCount * 10)) / 2)
      : ops.activeVendorCount > 0
        ? Math.min(100, ops.activeVendorCount * 15)
        : null;

  if (capacity.capacityPct != null && teamPayload) {
    teamPayload.capacityPct = capacity.capacityPct;
  }
  if (runwayPayload) {
    if (mom.revenueMomPct != null) runwayPayload.revenueMomPct = mom.revenueMomPct;
    if (mom.expenseMomPct != null) runwayPayload.expenseMomPct = mom.expenseMomPct;
  }
  void opsPayload;

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
