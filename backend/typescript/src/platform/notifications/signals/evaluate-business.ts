import type { Pool } from 'pg';
import { importanceFromScore, scoreActionability } from './actionability';
import { clearDerivedSignal } from './signal-store';
import type { DerivedNotificationSignal } from './types';

function num(v: unknown): number {
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : 0;
}

function moneyLabel(amount: number, currency: string): string {
  const formatted = amount.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  if ((currency ?? '').toUpperCase() === 'INR' || !currency) return `₹${formatted}`;
  return `${currency} ${formatted}`;
}

const RUNWAY_DELTA_MONTHS = 0.5;
const EXPENSE_THRESHOLDS = [80, 100] as const;

/**
 * Business signals from governance + finance projections (no independent recalculation).
 */
export async function evaluateBusinessSignals(pool: Pool): Promise<DerivedNotificationSignal[]> {
  const out: DerivedNotificationSignal[] = [];
  const now = new Date();

  // --- Approvals accumulating / aging for pending USER steps ---
  const approvals = await pool.query<{
    approver_user_id: string;
    company_id: string | null;
    company_name: string | null;
    pending_count: string;
    oldest_age_hours: string;
    total_amount: string | null;
  }>(
    `SELECT s.approver_user_id,
            CASE WHEN ar.scope_type = 'COMPANY' THEN ar.scope_id ELSE NULL END AS company_id,
            c.display_name AS company_name,
            COUNT(*)::text AS pending_count,
            MAX(EXTRACT(EPOCH FROM (now() - ar.requested_at)) / 3600)::text AS oldest_age_hours,
            NULL::text AS total_amount
     FROM governance.approval_step s
     JOIN governance.approval_request ar ON ar.approval_request_id = s.approval_request_id
     LEFT JOIN business.company c
       ON ar.scope_type = 'COMPANY' AND c.company_id = ar.scope_id
     WHERE s.status IN ('PENDING', 'IN_PROGRESS')
       AND ar.status IN ('PENDING', 'IN_REVIEW')
       AND s.approver_user_id IS NOT NULL
     GROUP BY s.approver_user_id,
              CASE WHEN ar.scope_type = 'COMPANY' THEN ar.scope_id ELSE NULL END,
              c.display_name
     HAVING COUNT(*) >= 1
     LIMIT 300`
  );

  for (const row of approvals.rows) {
    const count = Math.floor(num(row.pending_count));
    const ageHours = num(row.oldest_age_hours);
    const companyName = row.company_name ?? 'your company';
    const companyId = row.company_id ?? undefined;

    if (count >= 3) {
      const dedupeKey = `BUSINESS:${companyId ?? 'GLOBAL'}:USER:${row.approver_user_id}:APPROVALS_ACCUM`;
      const actionabilityScore = scoreActionability({
        importance: 'HIGH',
        hasClearNextAction: true,
        ageHours,
      });
      out.push({
        signalName: 'ApprovalsAccumulating',
        userId: row.approver_user_id,
        contextType: 'BUSINESS',
        contextId: companyId,
        companyId,
        category: 'approvals',
        importance: importanceFromScore('HIGH', actionabilityScore),
        facts: {
          companyName,
          pendingCount: count,
          body: `${count} approvals are waiting in ${companyName}.`,
        },
        dedupeKey,
        explanationCode: 'APPROVALS_ACCUMULATING',
        observedAt: now,
        actionabilityScore,
        hysteresisState: { pendingCount: count },
      });
    } else {
      await clearDerivedSignal(
        pool,
        `BUSINESS:${companyId ?? 'GLOBAL'}:USER:${row.approver_user_id}:APPROVALS_ACCUM`
      );
    }

    if (ageHours >= 48) {
      const dedupeKey = `BUSINESS:${companyId ?? 'GLOBAL'}:USER:${row.approver_user_id}:APPROVAL_AGE_48H`;
      const actionabilityScore = scoreActionability({
        importance: 'HIGH',
        hasClearNextAction: true,
        ageHours,
      });
      out.push({
        signalName: 'ApprovalAging',
        userId: row.approver_user_id,
        contextType: 'BUSINESS',
        contextId: companyId,
        companyId,
        category: 'approvals',
        importance: 'HIGH',
        facts: {
          companyName,
          oldestAgeHours: Math.round(ageHours),
          pendingCount: count,
          body: `An approval in ${companyName} has been waiting ${Math.round(ageHours)}h.`,
        },
        dedupeKey,
        explanationCode: 'APPROVAL_AGE_48H',
        observedAt: now,
        actionabilityScore,
      });
    } else if (ageHours < 24) {
      await clearDerivedSignal(
        pool,
        `BUSINESS:${companyId ?? 'GLOBAL'}:USER:${row.approver_user_id}:APPROVAL_AGE_48H`
      );
    }
  }

  // --- Invoices due soon / overdue ---
  const invoices = await pool.query<{
    invoice_id: string;
    company_id: string;
    company_name: string;
    invoice_number: string;
    due_date: Date;
    status: string;
    total_amount: string;
    paid_amount: string;
    currency_code: string;
    user_id: string;
  }>(
    `SELECT i.invoice_id, i.company_id, c.display_name AS company_name, i.invoice_number,
            i.due_date, i.status, i.total_amount::text, i.paid_amount::text, i.currency_code,
            cm.user_id
     FROM finance.invoice i
     JOIN business.company c ON c.company_id = i.company_id
     JOIN business.company_membership cm
       ON cm.company_id = i.company_id AND cm.status = 'ACTIVE'
       AND cm.membership_type IN ('OWNER', 'ADMIN')
     WHERE i.status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
       AND i.due_date IS NOT NULL
       AND i.due_date <= (CURRENT_DATE + INTERVAL '3 days')
     LIMIT 400`
  );

  for (const row of invoices.rows) {
    const due = new Date(row.due_date);
    const daysUntil =
      (Date.UTC(due.getFullYear(), due.getMonth(), due.getDate()) -
        Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())) /
      86_400_000;
    const remaining = Math.max(0, num(row.total_amount) - num(row.paid_amount));
    const overdue = daysUntil < 0 || row.status === 'OVERDUE';

    if (overdue) {
      const actionabilityScore = scoreActionability({
        importance: 'HIGH',
        hasClearNextAction: true,
        amountImpact: remaining,
        ageHours: Math.abs(daysUntil) * 24,
      });
      out.push({
        signalName: 'InvoiceOverdue',
        userId: row.user_id,
        contextType: 'BUSINESS',
        companyId: row.company_id,
        category: 'finance',
        importance: 'HIGH',
        facts: {
          companyName: row.company_name,
          invoiceNumber: row.invoice_number,
          currencyCode: row.currency_code,
          remainingAmount: remaining.toFixed(2),
          body: `Invoice ${row.invoice_number} is overdue (${moneyLabel(remaining, row.currency_code)}).`,
        },
        dedupeKey: `BUSINESS:${row.company_id}:USER:${row.user_id}:INVOICE:${row.invoice_id}:OVERDUE`,
        explanationCode: 'INVOICE_OVERDUE',
        observedAt: now,
        actionabilityScore,
      });
    } else if (daysUntil <= 1) {
      const actionabilityScore = scoreActionability({
        importance: 'HIGH',
        hasClearNextAction: true,
        amountImpact: remaining,
      });
      out.push({
        signalName: 'InvoiceDueSoon',
        userId: row.user_id,
        contextType: 'BUSINESS',
        companyId: row.company_id,
        category: 'finance',
        importance: 'HIGH',
        facts: {
          companyName: row.company_name,
          invoiceNumber: row.invoice_number,
          currencyCode: row.currency_code,
          remainingAmount: remaining.toFixed(2),
          body: `Invoice ${row.invoice_number} is due within 24h.`,
        },
        dedupeKey: `BUSINESS:${row.company_id}:USER:${row.user_id}:INVOICE:${row.invoice_id}:DUE_24H`,
        explanationCode: 'INVOICE_DUE_24H',
        observedAt: now,
        actionabilityScore,
      });
    }
  }

  // --- Expense threshold from business finance snapshot ---
  const expenses = await pool.query<{
    company_id: string;
    company_name: string;
    currency_code: string;
    expense_total: string;
    budget_total: string;
    user_id: string;
  }>(
    `SELECT bfs.company_id, c.display_name AS company_name, bfs.currency_code,
            bfs.expense_total::text, bfs.budget_total::text, cm.user_id
     FROM projection.business_finance_snapshot bfs
     JOIN business.company c ON c.company_id = bfs.company_id
     JOIN business.company_membership cm
       ON cm.company_id = bfs.company_id AND cm.status = 'ACTIVE'
       AND cm.membership_type IN ('OWNER', 'ADMIN')
     WHERE bfs.budget_total > 0
     LIMIT 300`
  );

  for (const row of expenses.rows) {
    const expense = num(row.expense_total);
    const budget = num(row.budget_total);
    if (budget <= 0) continue;
    const pct = (expense / budget) * 100;
    for (const threshold of EXPENSE_THRESHOLDS) {
      const dedupeKey = `BUSINESS:${row.company_id}:USER:${row.user_id}:EXPENSE_${threshold}`;
      if (pct < threshold - 5) {
        await clearDerivedSignal(pool, dedupeKey);
        continue;
      }
      if (pct < threshold) continue;
      const actionabilityScore = scoreActionability({
        importance: threshold >= 100 ? 'HIGH' : 'NORMAL',
        hasClearNextAction: true,
        amountImpact: expense,
      });
      out.push({
        signalName: 'ExpenseThresholdExceeded',
        userId: row.user_id,
        contextType: 'BUSINESS',
        companyId: row.company_id,
        category: 'finance',
        importance: importanceFromScore(threshold >= 100 ? 'HIGH' : 'NORMAL', actionabilityScore),
        facts: {
          companyName: row.company_name,
          currencyCode: row.currency_code,
          utilizationPercent: Math.round(pct),
          thresholdPercent: threshold,
          body: `${row.company_name} has used ${Math.round(pct)}% of its expense budget.`,
        },
        dedupeKey,
        explanationCode: threshold >= 100 ? 'EXPENSE_100_PERCENT' : 'EXPENSE_80_PERCENT',
        observedAt: now,
        actionabilityScore,
        hysteresisState: { threshold, utilizationPercent: Math.round(pct) },
      });
    }
  }

  // --- Runway changed meaningfully (from projection.business_pulse only) ---
  const runway = await pool.query<{
    company_id: string;
    company_name: string;
    runway_months: string | null;
    user_id: string;
  }>(
    `SELECT bp.company_id, c.display_name AS company_name, bp.runway_months::text, cm.user_id
     FROM projection.business_pulse bp
     JOIN business.company c ON c.company_id = bp.company_id
     JOIN business.company_membership cm
       ON cm.company_id = bp.company_id AND cm.status = 'ACTIVE'
       AND cm.membership_type IN ('OWNER', 'ADMIN')
     WHERE bp.runway_months IS NOT NULL
     LIMIT 200`
  );

  for (const row of runway.rows) {
    const months = num(row.runway_months);
    const band = Math.floor(months / RUNWAY_DELTA_MONTHS);
    const dedupeKey = `BUSINESS:${row.company_id}:USER:${row.user_id}:RUNWAY_BAND_${band}`;
    // Clear neighboring bands so crossing back can re-fire later
    await clearDerivedSignal(
      pool,
      `BUSINESS:${row.company_id}:USER:${row.user_id}:RUNWAY_BAND_${band + 1}`
    );
    await clearDerivedSignal(
      pool,
      `BUSINESS:${row.company_id}:USER:${row.user_id}:RUNWAY_BAND_${band - 1}`
    );

    const actionabilityScore = scoreActionability({
      importance: months < 3 ? 'HIGH' : 'NORMAL',
      hasClearNextAction: months < 6,
    });
    out.push({
      signalName: 'RunwayChangedMeaningfully',
      userId: row.user_id,
      contextType: 'BUSINESS',
      companyId: row.company_id,
      category: 'finance',
      importance: importanceFromScore(months < 3 ? 'HIGH' : 'NORMAL', actionabilityScore),
      facts: {
        companyName: row.company_name,
        runwayMonths: Math.round(months * 10) / 10,
        body: `${row.company_name} runway is now ${Math.round(months * 10) / 10} months.`,
      },
      dedupeKey,
      explanationCode: 'RUNWAY_CHANGED',
      observedAt: now,
      actionabilityScore,
      hysteresisState: { runwayMonths: months, band },
    });
  }

  return out;
}
