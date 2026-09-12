import type { Pool } from 'pg';
import { importanceFromScore, scoreActionability } from './actionability';
import { clearDerivedSignal } from './signal-store';
import type { DerivedNotificationSignal } from './types';

const BUDGET_THRESHOLDS = [50, 80, 100] as const;
/** Clear hysteresis when utilization falls this far below the band. */
const BUDGET_RESET_GAP = 5;
const BALANCE_MEANINGFUL_ABS = 100;

function num(v: unknown): number {
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : 0;
}

function moneyStr(n: number): string {
  return n.toFixed(2);
}

/**
 * Group signals from canonical finance / poll / task projections only.
 */
export async function evaluateGroupSignals(pool: Pool): Promise<DerivedNotificationSignal[]> {
  const out: DerivedNotificationSignal[] = [];
  const now = new Date();

  // --- Trip / group budget thresholds ---
  const budgets = await pool.query<{
    moment_id: string;
    currency_code: string;
    expense_total: string;
    budget_total: string;
    title: string | null;
    user_id: string;
  }>(
    `SELECT gfs.moment_id, gfs.currency_code, gfs.expense_total::text, gfs.budget_total::text,
            m.title, mp.user_id
     FROM projection.group_finance_snapshot gfs
     JOIN core.moment m ON m.moment_id = gfs.moment_id AND m.status = 'ACTIVE'
     JOIN collaboration.moment_participant mp
       ON mp.moment_id = gfs.moment_id AND mp.status = 'ACTIVE' AND mp.user_id IS NOT NULL
     WHERE gfs.budget_total > 0
     LIMIT 500`
  );

  for (const row of budgets.rows) {
    const expense = num(row.expense_total);
    const budget = num(row.budget_total);
    if (budget <= 0) continue;
    const pct = (expense / budget) * 100;
    const title = row.title ?? 'Trip';

    for (const threshold of BUDGET_THRESHOLDS) {
      const dedupeKey = `GROUP:${row.moment_id}:USER:${row.user_id}:TRIP_BUDGET_${threshold}`;
      if (pct < threshold - BUDGET_RESET_GAP) {
        await clearDerivedSignal(pool, dedupeKey);
        continue;
      }
      if (pct < threshold) continue;

      const explanationCode =
        threshold === 100
          ? 'BUDGET_100_PERCENT'
          : threshold === 80
            ? 'BUDGET_80_PERCENT'
            : 'BUDGET_50_PERCENT';
      const actionabilityScore = scoreActionability({
        importance: threshold >= 80 ? 'HIGH' : 'NORMAL',
        hasClearNextAction: true,
        amountImpact: expense,
        thresholdProximity: Math.max(0, (pct - threshold) / 20),
      });
      const importance = importanceFromScore(threshold >= 80 ? 'HIGH' : 'NORMAL', actionabilityScore);

      out.push({
        signalName: 'TripBudgetThresholdReached',
        userId: row.user_id,
        contextType: 'GROUP',
        contextId: row.moment_id,
        momentId: row.moment_id,
        category: 'finance',
        importance,
        facts: {
          momentTitle: title,
          currencyCode: row.currency_code,
          expenseTotal: moneyStr(expense),
          budgetTotal: moneyStr(budget),
          utilizationPercent: Math.round(pct),
          thresholdPercent: threshold,
          body: `This trip has used ${Math.round(pct)}% of its planned budget.`,
        },
        dedupeKey,
        explanationCode,
        observedAt: now,
        actionabilityScore,
        hysteresisState: { threshold, utilizationPercent: Math.round(pct) },
      });
    }
  }

  // --- User balance / settlement suggested / nearly settled ---
  const positions = await pool.query<{
    moment_id: string;
    currency_code: string;
    net_position: string;
    payable_total: string;
    receivable_total: string;
    outstanding_total: string | null;
    title: string | null;
    user_id: string;
  }>(
    `SELECT gfp.moment_id, gfp.currency_code, gfp.net_position::text,
            gfp.payable_total::text, gfp.receivable_total::text,
            gfs.outstanding_total::text, m.title, mp.user_id
     FROM projection.group_finance_position gfp
     JOIN collaboration.moment_participant mp
       ON mp.moment_id = gfp.moment_id
      AND mp.participant_id = gfp.participant_id
      AND mp.status = 'ACTIVE'
      AND mp.user_id IS NOT NULL
     JOIN core.moment m ON m.moment_id = gfp.moment_id AND m.status = 'ACTIVE'
     LEFT JOIN projection.group_finance_snapshot gfs
       ON gfs.moment_id = gfp.moment_id AND gfs.currency_code = gfp.currency_code
     WHERE ABS(gfp.net_position) >= $1
        OR (gfs.outstanding_total IS NOT NULL AND gfs.outstanding_total > 0 AND gfs.outstanding_total < $1 * 5)
     LIMIT 800`,
    [BALANCE_MEANINGFUL_ABS]
  );

  // --- User balance / settlement: one signal per user+moment (max |net| across currencies) ---
  // IMPORTANT: never clear from a near-zero secondary currency while another currency is still meaningful —
  // that caused minute-level re-emit spam (clear → reclaim → new domain event).
  type PosAgg = {
    moment_id: string;
    user_id: string;
    title: string;
    currency_code: string;
    net: number;
    absNet: number;
    outstanding: number;
  };
  const byUserMoment = new Map<string, PosAgg>();
  for (const row of positions.rows) {
    const net = num(row.net_position);
    const absNet = Math.abs(net);
    const key = `${row.moment_id}:${row.user_id}`;
    const prev = byUserMoment.get(key);
    if (!prev || absNet > prev.absNet) {
      byUserMoment.set(key, {
        moment_id: row.moment_id,
        user_id: row.user_id,
        title: row.title ?? 'Group',
        currency_code: row.currency_code,
        net,
        absNet,
        outstanding: num(row.outstanding_total),
      });
    } else if (prev && num(row.outstanding_total) > prev.outstanding) {
      prev.outstanding = num(row.outstanding_total);
    }
  }

  for (const agg of byUserMoment.values()) {
    const shareKey = `GROUP:${agg.moment_id}:USER:${agg.user_id}:BALANCE_MEANINGFUL`;
    const settleKey = `GROUP:${agg.moment_id}:USER:${agg.user_id}:SETTLEMENT_SUGGESTED`;
    const title = agg.title;

    if (agg.absNet >= BALANCE_MEANINGFUL_ABS) {
      const balanceScore = scoreActionability({
        importance: 'NORMAL',
        hasClearNextAction: true,
        amountImpact: agg.absNet,
      });
      out.push({
        signalName: 'UserBalanceChangedMeaningfully',
        userId: agg.user_id,
        contextType: 'GROUP',
        momentId: agg.moment_id,
        category: 'finance',
        importance: importanceFromScore('NORMAL', balanceScore),
        facts: {
          momentTitle: title,
          currencyCode: agg.currency_code,
          netPosition: moneyStr(agg.net),
          body:
            agg.net < 0
              ? `Your ${title} share has reached ${moneyLabel(agg.absNet, agg.currency_code)}.`
              : `You're owed ${moneyLabel(agg.absNet, agg.currency_code)} in ${title}.`,
        },
        dedupeKey: shareKey,
        explanationCode: 'BALANCE_CHANGED_MEANINGFUL',
        observedAt: now,
        actionabilityScore: balanceScore,
        hysteresisState: { lastAbsNet: agg.absNet, currencyCode: agg.currency_code },
      });

      const settleScore = scoreActionability({
        importance: agg.absNet >= 500 ? 'HIGH' : 'NORMAL',
        hasClearNextAction: true,
        amountImpact: agg.absNet,
      });
      out.push({
        signalName: 'SettlementSuggested',
        userId: agg.user_id,
        contextType: 'GROUP',
        momentId: agg.moment_id,
        category: 'finance',
        importance: importanceFromScore(agg.absNet >= 500 ? 'HIGH' : 'NORMAL', settleScore),
        facts: {
          momentTitle: title,
          currencyCode: agg.currency_code,
          settleAmount: moneyStr(agg.absNet),
          direction: agg.net < 0 ? 'YOU_OWE' : 'YOU_ARE_OWED',
          body:
            agg.net < 0
              ? `One ${moneyLabel(agg.absNet, agg.currency_code)} settlement would clear your balance.`
              : `A ${moneyLabel(agg.absNet, agg.currency_code)} settlement would clear what’s owed to you.`,
        },
        dedupeKey: settleKey,
        explanationCode: 'BALANCE_SETTLEABLE',
        observedAt: now,
        actionabilityScore: settleScore,
        hysteresisState: { settleAmount: agg.absNet, currencyCode: agg.currency_code },
      });
    }

    if (
      agg.outstanding > 0 &&
      agg.outstanding < BALANCE_MEANINGFUL_ABS * 5 &&
      agg.absNet > 0 &&
      agg.absNet < BALANCE_MEANINGFUL_ABS
    ) {
      const nearlyKey = `GROUP:${agg.moment_id}:USER:${agg.user_id}:NEARLY_SETTLED`;
      const actionabilityScore = scoreActionability({
        importance: 'LOW',
        hasClearNextAction: true,
        amountImpact: agg.outstanding,
      });
      out.push({
        signalName: 'GroupNearlySettled',
        userId: agg.user_id,
        contextType: 'GROUP',
        momentId: agg.moment_id,
        category: 'finance',
        importance: 'LOW',
        facts: {
          momentTitle: title,
          currencyCode: agg.currency_code,
          outstandingTotal: moneyStr(agg.outstanding),
          body: `${title} is nearly settled — only ${moneyLabel(agg.outstanding, agg.currency_code)} left.`,
        },
        dedupeKey: nearlyKey,
        explanationCode: 'GROUP_NEARLY_SETTLED',
        observedAt: now,
        actionabilityScore,
      });
    }
  }

  // --- Polls needing vote ---
  const polls = await pool.query<{
    poll_id: string;
    moment_id: string;
    question: string;
    title: string | null;
    user_id: string;
  }>(
    `SELECT p.poll_id, p.moment_id, p.question, m.title, mp.user_id
     FROM shared.poll p
     JOIN core.moment m ON m.moment_id = p.moment_id AND m.status = 'ACTIVE'
     JOIN collaboration.moment_participant mp
       ON mp.moment_id = p.moment_id AND mp.status = 'ACTIVE' AND mp.user_id IS NOT NULL
     WHERE p.status = 'OPEN'
       AND p.domain_code = 'GROUP'
       AND NOT EXISTS (
         SELECT 1 FROM shared.poll_vote v
         WHERE v.poll_id = p.poll_id AND v.voter_user_id = mp.user_id
       )
     LIMIT 400`
  );

  for (const row of polls.rows) {
    const actionabilityScore = scoreActionability({
      importance: 'NORMAL',
      hasClearNextAction: true,
    });
    out.push({
      signalName: 'PollNeedsYourVote',
      userId: row.user_id,
      contextType: 'GROUP',
      momentId: row.moment_id,
      category: 'tasks',
      importance: 'NORMAL',
      facts: {
        momentTitle: row.title,
        pollId: row.poll_id,
        question: row.question,
        body: row.question
          ? `Your vote is needed: “${row.question.slice(0, 80)}”.`
          : 'A poll needs your vote.',
      },
      dedupeKey: `GROUP:${row.moment_id}:USER:${row.user_id}:POLL:${row.poll_id}:VOTE`,
      explanationCode: 'POLL_NEEDS_VOTE',
      observedAt: now,
      actionabilityScore,
    });
  }

  // --- Assigned task due soon (complement to TaskDueReminder; distinct copy + dedupe) ---
  const tasks = await pool.query<{
    task_id: string;
    moment_id: string;
    title: string;
    due_at: Date;
    assignee_user_id: string;
    moment_title: string | null;
  }>(
    `SELECT t.task_id, t.moment_id, t.title, t.due_at,
            a.assignee_user_id, m.title AS moment_title
     FROM work.task t
     JOIN work.assignment a ON a.task_id = t.task_id AND a.status = 'ACTIVE' AND a.assignee_user_id IS NOT NULL
     JOIN core.moment m ON m.moment_id = t.moment_id AND m.domain_code = 'GROUP' AND m.status = 'ACTIVE'
     WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'BLOCKED')
       AND t.due_at IS NOT NULL
       AND t.due_at <= now() + interval '36 hours'
       AND t.due_at >= now() - interval '6 hours'
     LIMIT 300`
  );

  for (const row of tasks.rows) {
    const day = row.due_at.toISOString().slice(0, 10);
    const actionabilityScore = scoreActionability({
      importance: 'HIGH',
      hasClearNextAction: true,
      ageHours: Math.max(0, (now.getTime() - row.due_at.getTime()) / 3_600_000),
    });
    out.push({
      signalName: 'AssignedTaskDueSoon',
      userId: row.assignee_user_id,
      contextType: 'GROUP',
      momentId: row.moment_id,
      category: 'tasks',
      importance: 'HIGH',
      facts: {
        momentTitle: row.moment_title,
        taskId: row.task_id,
        taskTitle: row.title,
        dueAt: row.due_at.toISOString(),
        body: row.title
          ? `“${row.title}” is due soon.`
          : 'An assigned task is due soon.',
      },
      dedupeKey: `GROUP:${row.moment_id}:USER:${row.assignee_user_id}:TASK:${row.task_id}:DUE:${day}`,
      explanationCode: 'TASK_DUE_SOON',
      observedAt: now,
      actionabilityScore,
    });
  }

  await clearSettledBalanceSignals(pool);
  return out;
}

async function clearSettledBalanceSignals(pool: Pool): Promise<void> {
  // Clear only when the user has no remaining meaningful position in ANY currency for that moment.
  await pool.query(
    `UPDATE platform.derived_notification_signal dns
     SET cleared_at = now()
     WHERE dns.cleared_at IS NULL
       AND dns.signal_name IN ('UserBalanceChangedMeaningfully', 'SettlementSuggested')
       AND dns.moment_id IS NOT NULL
       AND NOT EXISTS (
         SELECT 1
         FROM projection.group_finance_position gfp
         JOIN collaboration.moment_participant mp
           ON mp.moment_id = gfp.moment_id
          AND mp.participant_id = gfp.participant_id
          AND mp.user_id = dns.user_id
         WHERE gfp.moment_id = dns.moment_id
           AND ABS(gfp.net_position) >= $1
       )`,
    [BALANCE_MEANINGFUL_ABS * 0.4]
  );
}

function moneyLabel(amount: number, currency: string): string {
  const formatted = amount.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  if ((currency ?? '').toUpperCase() === 'INR' || !currency) return `₹${formatted}`;
  return `${currency} ${formatted}`;
}
