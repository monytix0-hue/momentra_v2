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

const GOAL_MILESTONES = [25, 50, 75, 100] as const;
const BUDGET_THRESHOLDS = [80, 100] as const;

/**
 * Personal signals from work.goal + personal_finance_snapshot + recurring_schedule.
 */
export async function evaluatePersonalSignals(pool: Pool): Promise<DerivedNotificationSignal[]> {
  const out: DerivedNotificationSignal[] = [];
  const now = new Date();

  // --- Goal milestones / at risk ---
  const goals = await pool.query<{
    goal_id: string;
    moment_id: string;
    title: string;
    progress_percent: string;
    target_date: Date | null;
    owner_user_id: string;
  }>(
    `SELECT g.goal_id, g.moment_id, g.title, g.progress_percent::text, g.target_date, g.owner_user_id
     FROM work.goal g
     WHERE g.domain_code = 'PERSONAL'
       AND g.status = 'ACTIVE'
       AND g.owner_user_id IS NOT NULL
     LIMIT 500`
  );

  for (const row of goals.rows) {
    const progress = num(row.progress_percent);
    for (const milestone of GOAL_MILESTONES) {
      const dedupeKey = `PERSONAL:USER:${row.owner_user_id}:GOAL:${row.goal_id}:M${milestone}`;
      if (progress < milestone - 5) {
        await clearDerivedSignal(pool, dedupeKey);
        continue;
      }
      if (progress < milestone) continue;

      const actionabilityScore = scoreActionability({
        importance: milestone >= 75 ? 'HIGH' : 'NORMAL',
        hasClearNextAction: false,
        thresholdProximity: Math.max(0, (progress - milestone) / 25),
      });
      out.push({
        signalName: 'GoalMilestoneReached',
        userId: row.owner_user_id,
        contextType: 'PERSONAL',
        momentId: row.moment_id,
        category: 'tasks',
        importance: importanceFromScore(milestone >= 75 ? 'HIGH' : 'NORMAL', actionabilityScore),
        facts: {
          goalTitle: row.title,
          progressPercent: Math.round(progress),
          milestonePercent: milestone,
          body: `Your ${row.title} is now ${Math.round(progress)}% complete.`,
        },
        dedupeKey,
        explanationCode: `GOAL_${milestone}_PERCENT`,
        observedAt: now,
        actionabilityScore,
        hysteresisState: { milestone, progressPercent: Math.round(progress) },
      });
    }

    if (row.target_date) {
      const daysLeft =
        (Date.UTC(
          row.target_date.getFullYear(),
          row.target_date.getMonth(),
          row.target_date.getDate()
        ) -
          Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())) /
        86_400_000;
      const atRisk = daysLeft <= 14 && daysLeft >= 0 && progress < 50;
      const riskKey = `PERSONAL:USER:${row.owner_user_id}:GOAL:${row.goal_id}:AT_RISK`;
      if (!atRisk) {
        await clearDerivedSignal(pool, riskKey);
      } else {
        const actionabilityScore = scoreActionability({
          importance: 'HIGH',
          hasClearNextAction: true,
          thresholdProximity: progress / 100,
        });
        out.push({
          signalName: 'GoalAtRisk',
          userId: row.owner_user_id,
          contextType: 'PERSONAL',
          momentId: row.moment_id,
          category: 'tasks',
          importance: 'HIGH',
          facts: {
            goalTitle: row.title,
            progressPercent: Math.round(progress),
            daysLeft: Math.round(daysLeft),
            body: `${row.title} is at risk — ${Math.round(progress)}% with ${Math.round(daysLeft)} days left.`,
          },
          dedupeKey: riskKey,
          explanationCode: 'GOAL_AT_RISK',
          observedAt: now,
          actionabilityScore,
        });
      }
    }
  }

  // --- Personal budget threshold from projection ---
  const budgets = await pool.query<{
    user_id: string;
    currency_code: string;
    expense_total: string;
    budget_total: string;
  }>(
    `SELECT user_id, currency_code, expense_total::text, budget_total::text
     FROM projection.personal_finance_snapshot
     WHERE budget_total > 0
     LIMIT 400`
  );

  for (const row of budgets.rows) {
    const expense = num(row.expense_total);
    const budget = num(row.budget_total);
    if (budget <= 0) continue;
    const pct = (expense / budget) * 100;
    for (const threshold of BUDGET_THRESHOLDS) {
      const dedupeKey = `PERSONAL:USER:${row.user_id}:BUDGET_${threshold}`;
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
        signalName: 'BudgetThresholdReached',
        userId: row.user_id,
        contextType: 'PERSONAL',
        category: 'finance',
        importance: importanceFromScore(threshold >= 100 ? 'HIGH' : 'NORMAL', actionabilityScore),
        facts: {
          currencyCode: row.currency_code,
          utilizationPercent: Math.round(pct),
          thresholdPercent: threshold,
          expenseTotal: expense.toFixed(2),
          budgetTotal: budget.toFixed(2),
          body: `You've used ${Math.round(pct)}% of your personal budget.`,
        },
        dedupeKey,
        explanationCode: threshold >= 100 ? 'PERSONAL_BUDGET_100' : 'PERSONAL_BUDGET_80',
        observedAt: now,
        actionabilityScore,
        hysteresisState: { threshold, utilizationPercent: Math.round(pct) },
      });
    }
  }

  // --- Bill / recurring expense expected ---
  const bills = await pool.query<{
    recurring_schedule_id: string;
    owner_user_id: string;
    moment_id: string;
    next_run_at: Date;
    template_payload: Record<string, unknown>;
  }>(
    `SELECT recurring_schedule_id, owner_user_id, moment_id, next_run_at, template_payload
     FROM finance.recurring_schedule
     WHERE status = 'ACTIVE'
       AND resource_kind = 'EXPENSE'
       AND next_run_at IS NOT NULL
       AND next_run_at <= now() + interval '36 hours'
       AND next_run_at >= now() - interval '6 hours'
     LIMIT 300`
  );

  for (const row of bills.rows) {
    const day = row.next_run_at.toISOString().slice(0, 10);
    const title =
      typeof row.template_payload?.title === 'string'
        ? row.template_payload.title
        : typeof row.template_payload?.name === 'string'
          ? row.template_payload.name
          : 'Recurring expense';
    const amountRaw = row.template_payload?.amount ?? row.template_payload?.amountMinor;
    const amount = num(amountRaw);
    const currency =
      typeof row.template_payload?.currencyCode === 'string'
        ? row.template_payload.currencyCode
        : 'INR';

    const actionabilityScore = scoreActionability({
      importance: 'NORMAL',
      hasClearNextAction: true,
      amountImpact: amount > 0 ? amount : null,
    });
    out.push({
      signalName: 'BillDueSoon',
      userId: row.owner_user_id,
      contextType: 'PERSONAL',
      momentId: row.moment_id,
      category: 'reminders',
      importance: 'NORMAL',
      facts: {
        scheduleId: row.recurring_schedule_id,
        title,
        currencyCode: currency,
        amount: amount > 0 ? amount.toFixed(2) : null,
        body:
          amount > 0
            ? `${title} (${moneyLabel(amount, currency)}) is expected soon.`
            : `${title} is expected soon.`,
      },
      dedupeKey: `PERSONAL:USER:${row.owner_user_id}:BILL:${row.recurring_schedule_id}:${day}`,
      explanationCode: 'BILL_DUE_SOON',
      observedAt: now,
      actionabilityScore,
    });

    out.push({
      signalName: 'RecurringExpenseExpected',
      userId: row.owner_user_id,
      contextType: 'PERSONAL',
      momentId: row.moment_id,
      category: 'finance',
      importance: 'LOW',
      facts: {
        scheduleId: row.recurring_schedule_id,
        title,
        body: `Expected recurring expense: ${title}.`,
      },
      dedupeKey: `PERSONAL:USER:${row.owner_user_id}:RECURRING:${row.recurring_schedule_id}:${day}`,
      explanationCode: 'RECURRING_EXPENSE_EXPECTED',
      observedAt: now,
      actionabilityScore: scoreActionability({
        importance: 'LOW',
        hasClearNextAction: false,
      }),
    });
  }

  return out;
}
