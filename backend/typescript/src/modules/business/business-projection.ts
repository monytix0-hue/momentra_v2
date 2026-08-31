/**
 * B2-B: Business projection writers — pulse, finance snapshot enrichments, life, memory, moments.
 * Setup preferences feed runway/health/category breakdown; no invented demo KPIs.
 */
import type { PoolClient } from 'pg';
import { loadOpsPulseExtras } from './operations-precision';

type SetupPrefs = Record<string, unknown>;

async function loadSetupPrefs(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<SetupPrefs> {
  const row = await client.query<{ preferences: SetupPrefs }>(
    `SELECT preferences FROM business.business_system_setup
     WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
     ORDER BY updated_at DESC LIMIT 1`,
    [companyId, momentId]
  );
  return row.rows[0]?.preferences ?? {};
}

function parseMoneyish(v: unknown): number {
  if (v == null) return 0;
  const s = String(v).replace(/[₹,\s]/g, '');
  const n = parseFloat(s);
  return Number.isFinite(n) ? n : 0;
}

async function computeCategoryBreakdown(
  client: PoolClient,
  companyId: string,
  momentId: string | null
): Promise<Array<{ label: string; amount: string; pct: number }>> {
  const rows = await client.query<{ category_code: string; amt: string }>(
    `SELECT COALESCE(NULLIF(TRIM(e.category_code), ''), 'OTHER') AS category_code,
            SUM(e.amount)::text AS amt
     FROM finance.expense e
     JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
     WHERE bec.company_id = $1
       AND ($2::uuid IS NULL OR bec.moment_id = $2)
       AND e.status IN ('POSTED', 'DRAFT')
     GROUP BY 1
     ORDER BY SUM(e.amount) DESC
     LIMIT 12`,
    [companyId, momentId]
  );
  const total = rows.rows.reduce((s, r) => s + parseFloat(r.amt || '0'), 0);
  return rows.rows.map((r) => ({
    label: r.category_code,
    amount: r.amt,
    pct: total > 0 ? Math.round((parseFloat(r.amt) / total) * 100) : 0,
  }));
}

async function computeRunwayMonths(
  client: PoolClient,
  companyId: string,
  momentId: string,
  prefs: SetupPrefs
): Promise<number | null> {
  const burnRow = await client.query<{ total: string }>(
    `SELECT COALESCE(SUM(e.amount), 0)::text AS total
     FROM finance.expense e
     JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
     WHERE bec.company_id = $1 AND bec.moment_id = $2
       AND e.status = 'POSTED'
       AND e.effective_at >= now() - interval '30 days'`,
    [companyId, momentId]
  );
  const burn = parseFloat(burnRow.rows[0]?.total ?? '0');
  if (burn <= 0) return null;

  const cash =
    parseMoneyish(prefs.availableCash) ||
    parseMoneyish(prefs.operatingCashBuffer) ||
    parseMoneyish(prefs.cashBalance);
  if (cash <= 0) return null;

  return Math.round((cash / burn) * 10) / 10;
}

export function computeHealthScore(
  runwayMonths: number | null,
  prefs: SetupPrefs,
  expenseTotal: number,
  revenueTotal: number
): number | null {
  if (runwayMonths != null) {
    const target = parseMoneyish(prefs.warningThreshold) || 6;
    const targetMonths = typeof prefs.warningThreshold === 'string' && prefs.warningThreshold.includes('month')
      ? parseFloat(prefs.warningThreshold) || target
      : target;
    const runwayScore = Math.min(100, Math.round((runwayMonths / Math.max(targetMonths, 1)) * 100));
    return Math.max(0, Math.min(100, runwayScore));
  }
  if (expenseTotal > 0 || revenueTotal > 0) {
    const ratio = revenueTotal / Math.max(expenseTotal, 1);
    return Math.max(0, Math.min(100, Math.round(ratio * 50)));
  }
  return null;
}

/** Team module score from capacity heuristic (matches getCapacity). */
export async function computeTeamScore(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<number | null> {
  const members = await client
    .query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.company_membership
       WHERE company_id = $1 AND status = 'ACTIVE'`,
      [companyId]
    )
    .catch(() => ({ rows: [{ n: '0' }] }));
  const openIssues = await client
    .query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.issue
       WHERE company_id = $1 AND moment_id = $2 AND status IN ('OPEN','IN_PROGRESS','BLOCKED')`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [{ n: '0' }] }));
  const memberCount = parseInt(members.rows[0]?.n ?? '0', 10);
  const issueCount = parseInt(openIssues.rows[0]?.n ?? '0', 10);
  if (memberCount === 0) return null;
  const maxIssuesPerMember = 5;
  const load = issueCount / (memberCount * maxIssuesPerMember);
  return Math.max(0, Math.round((1 - load) * 100));
}

/** Ops module score from SLA compliance and budget adherence. */
export function computeOpsScore(
  slaCompliancePct: number | null,
  monthlySpend: string | null,
  prefs: SetupPrefs
): number | null {
  const parts: number[] = [];
  if (slaCompliancePct != null) parts.push(slaCompliancePct);
  const budgetRaw = prefs.monthlyBudget ?? prefs.monthlySpending;
  const budgetNum = budgetRaw != null ? parseMoneyish(budgetRaw) : 0;
  const spendNum = parseFloat(monthlySpend ?? '0');
  if (budgetNum > 0 && spendNum >= 0) {
    const adherence = spendNum <= budgetNum ? 100 : Math.max(0, 100 - Math.round(((spendNum - budgetNum) / budgetNum) * 100));
    parts.push(adherence);
  }
  if (parts.length === 0) return slaCompliancePct;
  return Math.round(parts.reduce((a, b) => a + b, 0) / parts.length);
}

async function upsertPulseHistory(
  client: PoolClient,
  companyId: string,
  financialHealthScore: number | null,
  teamScore: number | null,
  runwayScore: number | null,
  opsScore: number | null
): Promise<void> {
  await client.query('SAVEPOINT sp_business_pulse_history');
  try {
    await client.query(
      `INSERT INTO projection.business_pulse_history (
         company_id, period_month, financial_health_score, team_score, runway_score, ops_score,
         projection_version, updated_at
       ) VALUES ($1, date_trunc('month', now())::date, $2, $3, $4, $5, 1, now())
       ON CONFLICT (company_id, period_month) DO UPDATE SET
         financial_health_score = EXCLUDED.financial_health_score,
         team_score = EXCLUDED.team_score,
         runway_score = EXCLUDED.runway_score,
         ops_score = EXCLUDED.ops_score,
         projection_version = projection.business_pulse_history.projection_version + 1,
         updated_at = now()`,
      [companyId, financialHealthScore, teamScore, runwayScore, opsScore]
    );
    await client.query('RELEASE SAVEPOINT sp_business_pulse_history');
  } catch {
    await client.query('ROLLBACK TO SAVEPOINT sp_business_pulse_history');
  }
}

/** Refresh business_pulse + finance snapshot category payload for a company/moment. */
export async function refreshBusinessPulseProjection(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<void> {
  const prefs = await loadSetupPrefs(client, companyId, momentId);
  const categories = await computeCategoryBreakdown(client, companyId, momentId);
  const runwayMonths = await computeRunwayMonths(client, companyId, momentId, prefs);

  const fin = await client.query<{ expense_total: string; revenue_total: string }>(
    `SELECT expense_total::text, revenue_total::text
     FROM projection.business_finance_snapshot
     WHERE company_id = $1
     ORDER BY expense_total DESC NULLS LAST
     LIMIT 1`,
    [companyId]
  );
  const expenseTotal = parseFloat(fin.rows[0]?.expense_total ?? '0');
  const revenueTotal = parseFloat(fin.rows[0]?.revenue_total ?? '0');
  const healthScore = computeHealthScore(runwayMonths, prefs, expenseTotal, revenueTotal);
  const teamScore = await computeTeamScore(client, companyId, momentId);
  const opsExtras = await loadOpsPulseExtras(client, companyId, momentId);
  const opsScore = computeOpsScore(opsExtras.slaCompliancePct, opsExtras.monthlySpend, prefs);
  const runwayScore = healthScore;

  await upsertPulseHistory(client, companyId, healthScore, teamScore, runwayScore, opsScore);

  const issueCount = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM business.issue
     WHERE company_id = $1 AND moment_id = $2 AND status IN ('OPEN','IN_PROGRESS','BLOCKED')`,
    [companyId, momentId]
  );
  const openIssues = parseInt(issueCount.rows[0]?.n ?? '0', 10);

  const momentCount = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM business.business_moment_context
     WHERE company_id = $1`,
    [companyId]
  );
  const activeMoments = parseInt(momentCount.rows[0]?.n ?? '0', 10);

  const widgetPayload = {
    categoryBreakdown: categories,
    monthlyBudget: prefs.monthlyBudget ?? prefs.monthlySpending ?? null,
    targetRunway: prefs.warningThreshold ?? prefs.goalHorizon ?? null,
    setupFamily: prefs.businessStage ?? null,
    expenseTotal: expenseTotal > 0 ? expenseTotal.toFixed(2) : null,
    revenueTotal: revenueTotal > 0 ? revenueTotal.toFixed(2) : null,
  };

  await client.query(
    `INSERT INTO projection.business_pulse (
       company_id, active_moment_count, attention_count, open_issue_count, open_risk_count,
       runway_months, financial_health_score, widget_payload, projection_version, updated_at
     ) VALUES ($1, $2, COALESCE((SELECT attention_count FROM projection.business_pulse WHERE company_id = $1), 0),
               $3, 0, $4, $5, $6::jsonb, 1, now())
     ON CONFLICT (company_id) DO UPDATE SET
       active_moment_count = EXCLUDED.active_moment_count,
       open_issue_count = EXCLUDED.open_issue_count,
       runway_months = COALESCE(EXCLUDED.runway_months, projection.business_pulse.runway_months),
       financial_health_score = COALESCE(EXCLUDED.financial_health_score, projection.business_pulse.financial_health_score),
       widget_payload = EXCLUDED.widget_payload,
       projection_version = projection.business_pulse.projection_version + 1,
       updated_at = now()`,
    [
      companyId,
      activeMoments,
      openIssues,
      runwayMonths,
      healthScore,
      JSON.stringify(widgetPayload),
    ]
  );

  if (categories.length > 0) {
    await client.query(
      `UPDATE projection.business_finance_snapshot
       SET snapshot_payload = COALESCE(snapshot_payload, '{}'::jsonb)
         || jsonb_build_object('categoryBreakdown', $2::jsonb),
           projection_version = projection_version + 1,
           updated_at = now()
       WHERE company_id = $1 AND currency_code = COALESCE(
         (SELECT currency_code FROM projection.business_finance_snapshot WHERE company_id = $1 LIMIT 1),
         'INR'
       )`,
      [companyId, JSON.stringify(categories)]
    );
  }
}

/** Refresh business_life family payloads from setup + live aggregates. */
export async function refreshBusinessLifeProjection(
  client: PoolClient,
  companyId: string,
  momentId: string,
  businessFamily: string
): Promise<void> {
  const prefs = await loadSetupPrefs(client, companyId, momentId);
  const family = businessFamily.toUpperCase();

  const fin = await client.query<{ expense_total: string; revenue_total: string }>(
    `SELECT expense_total::text, revenue_total::text
     FROM projection.business_finance_snapshot WHERE company_id = $1 LIMIT 1`,
    [companyId]
  );

  const teamOpsPayload =
    family.includes('TEAM') ?
      {
        statusLabel: 'Team operations active',
        memberCapacity: prefs.teamSize ?? null,
        reviewCadence: prefs.reviewCadence ?? null,
        openItems: prefs.openItems ?? null,
      }
    : {};

  const runwayPayload =
    family.includes('RUNWAY') ?
      {
        statusLabel: prefs.businessStage ?? 'Runway tracking',
        availableCash: prefs.availableCash ?? null,
        monthlySpending: prefs.monthlySpending ?? null,
        monthlyRevenue: prefs.monthlyRevenue ?? null,
        expenseTotal: fin.rows[0]?.expense_total ?? null,
        revenueTotal: fin.rows[0]?.revenue_total ?? null,
      }
    : {};

  const opsPayload =
    family.includes('OPERATIONS') && !family.includes('TEAM') ?
      {
        statusLabel: prefs.operatingModel ?? 'Operations active',
        monthlyBudget: prefs.monthlyBudget ?? null,
        monitoringStyle: prefs.monitoringStyle ?? null,
        expenseTotal: fin.rows[0]?.expense_total ?? null,
      }
    : {};

  const opsExtras = await loadOpsPulseExtras(client, companyId, momentId);
  const vendorOpsPayload = {
    statusLabel:
      opsExtras.activeVendorCount > 0
        ? `${opsExtras.activeVendorCount} active vendor${opsExtras.activeVendorCount === 1 ? '' : 's'}`
        : 'Vendor operations',
    activeVendorCount: opsExtras.activeVendorCount,
    slaCompliancePct: opsExtras.slaCompliancePct,
    monthlySpend: opsExtras.monthlySpend,
    spendVsForecast: opsExtras.spendVsForecast,
    openIssueCount: opsExtras.openIssueCount,
  };
  const vendorActive = opsExtras.activeVendorCount > 0 || opsExtras.slaCompliancePct != null;

  await client.query(
    `INSERT INTO projection.business_life (
       company_id, team_operations_payload, runway_payload, business_operations_payload,
       vendor_operations_payload, projection_version, updated_at
     ) VALUES ($1, $2::jsonb, $3::jsonb, $4::jsonb, $5::jsonb, 1, now())
     ON CONFLICT (company_id) DO UPDATE SET
       team_operations_payload = CASE WHEN $6::boolean THEN EXCLUDED.team_operations_payload
         ELSE projection.business_life.team_operations_payload END,
       runway_payload = CASE WHEN $7::boolean THEN EXCLUDED.runway_payload
         ELSE projection.business_life.runway_payload END,
       business_operations_payload = CASE WHEN $8::boolean THEN EXCLUDED.business_operations_payload
         ELSE projection.business_life.business_operations_payload END,
       vendor_operations_payload = CASE WHEN $9::boolean OR $5::jsonb != '{}'::jsonb
         THEN EXCLUDED.vendor_operations_payload
         ELSE projection.business_life.vendor_operations_payload END,
       projection_version = projection.business_life.projection_version + 1,
       updated_at = now()`,
    [
      companyId,
      JSON.stringify(teamOpsPayload),
      JSON.stringify(runwayPayload),
      JSON.stringify(opsPayload),
      JSON.stringify(vendorOpsPayload),
      family.includes('TEAM'),
      family.includes('RUNWAY'),
      family.includes('OPERATIONS') && !family.includes('TEAM'),
      vendorActive,
    ]
  );
}

/** Sync business_memory projection from memory.memory rows. */
export async function refreshBusinessMemoryProjection(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<void> {
  const rows = await client.query<{
    memory_id: string;
    title: string;
    summary: string | null;
    memory_type: string;
    occurred_at: Date | null;
  }>(
    `SELECT m.memory_id, m.title, m.summary, m.memory_type, m.occurred_at
     FROM memory.memory m
     JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
     WHERE bmc.company_id = $1 AND m.moment_id = $2 AND m.status = 'ACTIVE'
     ORDER BY COALESCE(m.occurred_at, m.created_at) DESC
     LIMIT 50`,
    [companyId, momentId]
  );

  const items = rows.rows.map((r) => ({
    memoryId: r.memory_id,
    title: r.title,
    body: r.summary,
    memoryType: r.memory_type,
    occurredAt: r.occurred_at?.toISOString() ?? null,
  }));

  const riskCount = items.filter((i) => {
    const hay = `${i.title} ${i.body ?? ''}`.toLowerCase();
    return hay.includes('risk') || hay.includes('issue') || hay.includes('incident');
  }).length;

  await client.query(
    `INSERT INTO projection.business_memory (
       company_id, memory_count, pattern_count, learning_count, playbook_count,
       recent_memory_payload, projection_version, updated_at
     ) VALUES ($1, $2, 0, $2, 0, $3::jsonb, 1, now())
     ON CONFLICT (company_id) DO UPDATE SET
       memory_count = EXCLUDED.memory_count,
       learning_count = EXCLUDED.learning_count,
       recent_memory_payload = EXCLUDED.recent_memory_payload,
       projection_version = projection.business_memory.projection_version + 1,
       updated_at = now()`,
    [companyId, items.length, JSON.stringify({ items, riskCount, successCount: items.length - riskCount })]
  );
}

/** Append a structured timeline event into business_moments.card_payload.events. */
export async function appendBusinessMomentEvent(
  client: PoolClient,
  companyId: string,
  momentId: string,
  event: {
    eventId: string;
    eventType: string;
    title: string;
    category: string;
    description?: string;
    contributors?: string[];
    occurredAt: string;
    payload?: Record<string, unknown>;
  }
): Promise<void> {
  const existing = await client.query<{ card_payload: Record<string, unknown>; moment_type_code: string }>(
    `SELECT card_payload, moment_type_code FROM projection.business_moments
     WHERE company_id = $1 AND moment_id = $2`,
    [companyId, momentId]
  );
  const prev = (existing.rows[0]?.card_payload ?? {}) as { events?: unknown[] };
  const events = Array.isArray(prev.events) ? [...prev.events] : [];
  events.unshift({
    eventId: event.eventId,
    eventType: event.eventType,
    title: event.title,
    category: event.category,
    description: event.description ?? null,
    contributors: event.contributors ?? [],
    occurredAt: event.occurredAt,
    ...event.payload,
  });
  const trimmed = events.slice(0, 100);
  const momentType = existing.rows[0]?.moment_type_code ?? 'BUSINESS_EVENT';

  await client.query(
    `INSERT INTO projection.business_moments (
       company_id, moment_id, temporal_bucket, display_rank, status, title,
       moment_type_code, card_payload, projection_version, updated_at
     ) VALUES ($1, $2, 'ACTIVE', 0, 'ACTIVE', $3, $4, $5::jsonb, 1, now())
     ON CONFLICT (company_id, moment_id) DO UPDATE SET
       title = EXCLUDED.title,
       card_payload = EXCLUDED.card_payload,
       projection_version = projection.business_moments.projection_version + 1,
       updated_at = now()`,
    [companyId, momentId, event.title, momentType, JSON.stringify({ events: trimmed })]
  );
}

/** Full refresh after a business write — pulse, life, memory, moments metadata. */
export async function refreshBusinessProjectionsAfterWrite(
  client: PoolClient,
  companyId: string,
  momentId: string,
  businessFamily: string
): Promise<void> {
  await refreshBusinessPulseProjection(client, companyId, momentId);
  await refreshBusinessLifeProjection(client, companyId, momentId, businessFamily);
  await refreshBusinessMemoryProjection(client, companyId, momentId);
}
