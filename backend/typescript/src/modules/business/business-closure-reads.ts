/**
 * Business Deployment Closure — Wave 3 reads.
 * Honest projections: return real data where available, null + explanatory note otherwise.
 */
import type { PoolClient } from 'pg';
import { randomBytes } from 'crypto';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertCompanyMomentAccess } from './membership';

/* ------------------------------------------------------------------ */
/*  GET capacity                                                      */
/* ------------------------------------------------------------------ */
export async function getCapacity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ capacityPct: number | null; note: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const members = await client
    .query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.company_membership
       WHERE company_id = $1 AND status = 'ACTIVE'`,
      [scope.companyId]
    )
    .catch(() => ({ rows: [{ n: '0' }] }));

  const openIssues = await client
    .query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.issue
       WHERE company_id = $1 AND moment_id = $2 AND status IN ('OPEN','IN_PROGRESS','BLOCKED')`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [{ n: '0' }] }));

  const memberCount = parseInt(members.rows[0]?.n ?? '0', 10);
  const issueCount = parseInt(openIssues.rows[0]?.n ?? '0', 10);

  if (memberCount === 0) {
    return {
      capacityPct: null,
      note: 'Capacity % requires staffing model; not fabricated.',
    };
  }

  const maxIssuesPerMember = 5;
  const load = issueCount / (memberCount * maxIssuesPerMember);
  const capacityPct = Math.max(0, Math.round((1 - load) * 100));

  return {
    capacityPct,
    note: `Estimated from ${issueCount} open issues across ${memberCount} members (${maxIssuesPerMember} capacity/member heuristic).`,
  };
}

/* ------------------------------------------------------------------ */
/*  GET workload                                                      */
/* ------------------------------------------------------------------ */
export async function getWorkload(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ byDepartment: Array<{ name: string; count: number }>; note: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const rows = await client
    .query<{ severity: string; n: string }>(
      `SELECT severity, COUNT(*)::text AS n FROM business.issue
       WHERE company_id = $1 AND moment_id = $2 AND status IN ('OPEN','IN_PROGRESS','BLOCKED')
       GROUP BY severity ORDER BY
         CASE severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ severity: string; n: string }> }));

  if (rows.rows.length === 0) {
    return {
      byDepartment: [],
      note: 'No department model configured; workload data unavailable.',
    };
  }

  return {
    byDepartment: rows.rows.map((r) => ({
      name: r.severity,
      count: parseInt(r.n, 10),
    })),
    note: 'Grouped open issues by severity as proxy for department breakdown (no department column available).',
  };
}

/* ------------------------------------------------------------------ */
/*  GET mom-deltas (month-over-month)                                 */
/* ------------------------------------------------------------------ */
export async function getMomDeltas(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ revenueMomPct: number | null; expenseMomPct: number | null; note: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const revenueQ = await client
    .query<{ cur: string; prev: string }>(
      `SELECT
         COALESCE(SUM(CASE WHEN effective_at >= date_trunc('month', now()) THEN amount END),0)::text AS cur,
         COALESCE(SUM(CASE WHEN effective_at >= date_trunc('month', now()) - interval '1 month'
                            AND effective_at < date_trunc('month', now()) THEN amount END),0)::text AS prev
       FROM finance.revenue
       WHERE company_id = $1 AND moment_id = $2 AND status IN ('POSTED','DRAFT')`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [{ cur: '0', prev: '0' }] }));

  const expenseQ = await client
    .query<{ cur: string; prev: string }>(
      `SELECT
         COALESCE(SUM(CASE WHEN e.effective_at >= date_trunc('month', now()) THEN e.amount END),0)::text AS cur,
         COALESCE(SUM(CASE WHEN e.effective_at >= date_trunc('month', now()) - interval '1 month'
                            AND e.effective_at < date_trunc('month', now()) THEN e.amount END),0)::text AS prev
       FROM finance.expense e
       JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
       WHERE bec.company_id = $1 AND bec.moment_id = $2 AND e.status IN ('POSTED','DRAFT')`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [{ cur: '0', prev: '0' }] }));

  const revCur = parseFloat(revenueQ.rows[0]?.cur ?? '0');
  const revPrev = parseFloat(revenueQ.rows[0]?.prev ?? '0');
  const expCur = parseFloat(expenseQ.rows[0]?.cur ?? '0');
  const expPrev = parseFloat(expenseQ.rows[0]?.prev ?? '0');

  const revenueMomPct = revPrev > 0 ? Math.round(((revCur - revPrev) / revPrev) * 100) : null;
  const expenseMomPct = expPrev > 0 ? Math.round(((expCur - expPrev) / expPrev) * 100) : null;

  const parts: string[] = [];
  if (revenueMomPct == null) parts.push('revenue MoM unavailable (no prior month data)');
  if (expenseMomPct == null) parts.push('expense MoM unavailable (no prior month data)');

  return {
    revenueMomPct,
    expenseMomPct,
    note: parts.length ? parts.join('; ') : 'Computed from finance facts.',
  };
}

/* ------------------------------------------------------------------ */
/*  GET progress-snapshot                                             */
/* ------------------------------------------------------------------ */
export async function getProgressSnapshot(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ collections: unknown[]; health: string | null; note: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const forecasts = await client
    .query<{ forecast_scenario_id: string; name: string; status: string }>(
      `SELECT forecast_scenario_id, name, status FROM finance.forecast_scenario
       WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
       ORDER BY created_at DESC LIMIT 5`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ forecast_scenario_id: string; name: string; status: string }> }));

  const reviews = await client
    .query<{ business_review_id: string; title: string; review_type: string }>(
      `SELECT business_review_id, title, review_type FROM business.business_review
       WHERE company_id = $1 AND moment_id = $2 AND status = 'COMPLETED'
       ORDER BY review_date DESC LIMIT 5`,
      [scope.companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ business_review_id: string; title: string; review_type: string }> }));

  const collections = [
    ...forecasts.rows.map((f) => ({
      id: f.forecast_scenario_id,
      type: 'forecast',
      title: f.name,
      status: f.status,
    })),
    ...reviews.rows.map((r) => ({
      id: r.business_review_id,
      type: 'review',
      title: r.title,
      status: r.review_type,
    })),
  ];

  const health =
    collections.length > 0
      ? collections.length >= 3
        ? 'STRONG'
        : 'STABLE'
      : null;

  return {
    collections,
    health,
    note:
      collections.length > 0
        ? `${collections.length} forecast/review collection(s) found.`
        : 'No forecast scenarios or completed reviews yet.',
  };
}

/* ------------------------------------------------------------------ */
/*  GET roster                                                        */
/* ------------------------------------------------------------------ */
export async function getRoster(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  members: Array<{ userId: string; displayName: string; membershipType: string; status: string }>;
  note: string;
}> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    user_id: string;
    display_name: string;
    membership_type: string;
    status: string;
  }>(
    `SELECT cm.user_id, COALESCE(up.display_name, up.email, cm.user_id::text) AS display_name,
            cm.membership_type, cm.status
     FROM business.company_membership cm
     JOIN core.user_profile up ON up.user_id = cm.user_id
     WHERE cm.company_id = $1 AND cm.status IN ('ACTIVE','INVITED')
     ORDER BY cm.membership_type, up.display_name
     LIMIT 200`,
    [scope.companyId]
  );
  return {
    members: rows.rows.map((r) => ({
      userId: r.user_id,
      displayName: r.display_name,
      membershipType: r.membership_type,
      status: r.status,
    })),
    note: `${rows.rows.length} member(s) for company.`,
  };
}

/* ------------------------------------------------------------------ */
/*  GET weekly-report                                                 */
/* ------------------------------------------------------------------ */
export async function getWeeklyReport(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  period: '7d' | '30d' = '7d'
): Promise<{
  title: string;
  sections: Array<{ heading: string; items: string[] }>;
  generatedAt: string;
  period: string;
  note: string;
}> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const now = new Date();
  const days = period === '30d' ? 30 : 7;
  const since = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);

  const sections: Array<{ heading: string; items: string[] }> = [];

  const issues = await client
    .query<{ title: string; status: string }>(
      `SELECT title, status FROM business.issue
       WHERE company_id = $1 AND moment_id = $2 AND created_at >= $3
       ORDER BY created_at DESC LIMIT 10`,
      [scope.companyId, momentId, since]
    )
    .catch(() => ({ rows: [] as Array<{ title: string; status: string }> }));
  if (issues.rows.length) {
    sections.push({
      heading: 'New Issues',
      items: issues.rows.map((r) => `${r.title} [${r.status}]`),
    });
  }

  const decisions = await client
    .query<{ title: string }>(
      `SELECT title FROM business.decision
       WHERE company_id = $1 AND moment_id = $2 AND decided_at >= $3
       ORDER BY decided_at DESC LIMIT 10`,
      [scope.companyId, momentId, since]
    )
    .catch(() => ({ rows: [] as Array<{ title: string }> }));
  if (decisions.rows.length) {
    sections.push({
      heading: 'Decisions',
      items: decisions.rows.map((r) => r.title),
    });
  }

  const updates = await client
    .query<{ title: string | null; body: string }>(
      `SELECT title, body FROM business.business_update
       WHERE company_id = $1 AND moment_id = $2 AND created_at >= $3 AND status = 'PUBLISHED'
       ORDER BY created_at DESC LIMIT 10`,
      [scope.companyId, momentId, since]
    )
    .catch(() => ({ rows: [] as Array<{ title: string | null; body: string }> }));
  if (updates.rows.length) {
    sections.push({
      heading: 'Updates',
      items: updates.rows.map((r) => r.title ?? r.body.slice(0, 120)),
    });
  }

  const milestones = await client
    .query<{ title: string; status: string }>(
      `SELECT title, status FROM work.milestone
       WHERE moment_id = $1 AND updated_at >= $2
       ORDER BY updated_at DESC LIMIT 10`,
      [momentId, since]
    )
    .catch(() => ({ rows: [] as Array<{ title: string; status: string }> }));
  if (milestones.rows.length) {
    sections.push({
      heading: 'Milestones',
      items: milestones.rows.map((r) => `${r.title} [${r.status}]`),
    });
  }

  const spend = await client
    .query<{ total: string }>(
      `SELECT COALESCE(SUM(e.amount),0)::text AS total
       FROM finance.expense e
       JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
       WHERE bec.company_id = $1 AND bec.moment_id = $2
         AND e.status IN ('POSTED','DRAFT') AND e.effective_at >= $3`,
      [scope.companyId, momentId, since]
    )
    .catch(() => ({ rows: [{ total: '0' }] }));
  const spendTotal = parseFloat(spend.rows[0]?.total ?? '0');
  if (spendTotal > 0) {
    sections.push({
      heading: 'Spend',
      items: [`Total spend in period: ${spendTotal.toFixed(2)}`],
    });
  }

  return {
    title: `Weekly Report — ${now.toISOString().slice(0, 10)}`,
    sections,
    generatedAt: now.toISOString(),
    period,
    note: sections.length
      ? `Generated from activity in the past ${days} days.`
      : `No activity recorded in the past ${days} days.`,
  };
}

/* ------------------------------------------------------------------ */
/*  POST evidence on issue                                            */
/* ------------------------------------------------------------------ */
export const addIssueEvidenceSchema = z
  .object({
    note: z.string().max(5000).optional(),
    url: z.string().url().max(2000).optional(),
  })
  .strict()
  .refine((b) => b.note != null || b.url != null, {
    message: 'At least one of note or url is required.',
  });

export async function addIssueEvidence(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  issueId: string,
  body: z.infer<typeof addIssueEvidenceSchema>
): Promise<{ issueId: string; momentId: string; updated: boolean }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const evidenceLine = [body.note, body.url].filter(Boolean).join(' — ');
  const appendText = `\n\n[Evidence ${new Date().toISOString()}] ${evidenceLine}`;

  const updated = await client.query(
    `UPDATE business.issue
     SET description = COALESCE(description, '') || $3,
         version = version + 1,
         updated_at = now()
     WHERE issue_id = $1 AND company_id = $2`,
    [issueId, scope.companyId, appendText]
  );

  if (updated.rowCount === 0) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Issue not found.', 404);
  }

  return { issueId, momentId, updated: true };
}

/* ------------------------------------------------------------------ */
/*  POST share-link                                                   */
/* ------------------------------------------------------------------ */
export async function createShareLink(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ shareUrl: string; shareToken: string; expiresAt: string; note: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const token = randomBytes(24).toString('base64url');
  const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

  try {
    await client.query(
      `INSERT INTO business.share_link (
         company_id, moment_id, created_by_user_id, share_token, expires_at
       ) VALUES ($1, $2, $3, $4, $5)`,
      [scope.companyId, momentId, ctx.userId, token, expiresAt]
    );
    return {
      shareUrl: `/share/business/${momentId}?token=${token}`,
      shareToken: token,
      expiresAt: expiresAt.toISOString(),
      note: 'Share link valid for 7 days.',
    };
  } catch {
    return {
      shareUrl: `/share/business/${momentId}?token=${token}`,
      shareToken: token,
      expiresAt: expiresAt.toISOString(),
      note: 'Share link returned; persist after V055 migration is applied.',
    };
  }
}
