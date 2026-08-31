/**
 * B2-B: Business read/list APIs — structured domain data for Pulse/Moments/Memory clients.
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { assertCompanyMomentAccess, assertActiveCompanyMember } from './membership';

export async function listBusinessExpenses(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    expense_id: string;
    amount: string;
    currency_code: string;
    category_code: string | null;
    description: string | null;
    status: string;
    effective_at: Date;
  }>(
    `SELECT e.expense_id, e.amount::text, e.currency_code, e.category_code, e.description, e.status, e.effective_at
     FROM finance.expense e
     JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
     WHERE bec.company_id = $1 AND bec.moment_id = $2
     ORDER BY e.effective_at DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      expenseId: r.expense_id,
      amount: r.amount,
      currencyCode: r.currency_code,
      categoryCode: r.category_code,
      description: r.description,
      status: r.status,
      effectiveAt: r.effective_at.toISOString(),
    })),
  };
}

export async function listBusinessRevenues(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    revenue_id: string;
    amount: string;
    currency_code: string;
    category_code: string | null;
    status: string;
    effective_at: Date;
  }>(
    `SELECT revenue_id, amount::text, currency_code, category_code, status, effective_at
     FROM finance.revenue
     WHERE company_id = $1 AND moment_id = $2
     ORDER BY effective_at DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      revenueId: r.revenue_id,
      amount: r.amount,
      currencyCode: r.currency_code,
      categoryCode: r.category_code,
      status: r.status,
      effectiveAt: r.effective_at.toISOString(),
    })),
  };
}

export async function listBusinessInvoices(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    invoice_id: string;
    invoice_number: string;
    total_amount: string;
    currency_code: string;
    status: string;
    invoice_date: Date;
  }>(
    `SELECT invoice_id, invoice_number, total_amount::text, currency_code, status, invoice_date
     FROM finance.invoice
     WHERE company_id = $1 AND moment_id = $2
     ORDER BY invoice_date DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      invoiceId: r.invoice_id,
      invoiceNumber: r.invoice_number,
      totalAmount: r.total_amount,
      currencyCode: r.currency_code,
      status: r.status,
      invoiceDate: r.invoice_date.toISOString().slice(0, 10),
    })),
  };
}

export async function listCompanyVendors(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  limit = 100
) {
  await assertActiveCompanyMember(client, ctx, companyId);
  const rows = await client.query<{
    vendor_id: string;
    name: string;
    vendor_type: string | null;
    status: string;
  }>(
    `SELECT vendor_id, name, vendor_type, status
     FROM business.vendor
     WHERE company_id = $1
     ORDER BY name ASC
     LIMIT $2`,
    [companyId, limit]
  );
  return {
    companyId,
    items: rows.rows.map((r) => ({
      vendorId: r.vendor_id,
      name: r.name,
      vendorType: r.vendor_type,
      status: r.status,
    })),
  };
}

export async function listBusinessIssues(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    issue_id: string;
    title: string;
    severity: string;
    status: string;
    opened_at: Date;
  }>(
    `SELECT issue_id, title, severity, status, opened_at
     FROM business.issue
     WHERE company_id = $1 AND moment_id = $2
     ORDER BY opened_at DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      issueId: r.issue_id,
      title: r.title,
      severity: r.severity,
      status: r.status,
      openedAt: r.opened_at.toISOString(),
    })),
  };
}

export async function listBusinessImprovements(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    improvement_id: string;
    title: string;
    status: string;
    created_at: Date;
  }>(
    `SELECT improvement_id, title, status, created_at
     FROM business.operational_improvement
     WHERE company_id = $1 AND moment_id = $2
     ORDER BY created_at DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      improvementId: r.improvement_id,
      title: r.title,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listBusinessUpdates(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    update_id: string;
    title: string;
    body: string | null;
    created_at: Date;
  }>(
    `SELECT business_update_id AS update_id, title, body, created_at
     FROM business.business_update
     WHERE company_id = $1 AND moment_id = $2
     ORDER BY created_at DESC
     LIMIT $3`,
    [scope.companyId, momentId, limit]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      updateId: r.update_id,
      title: r.title,
      body: r.body,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listPendingApprovals(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    approval_request_id: string;
    title: string | null;
    status: string;
    resource_type: string;
    created_at: Date;
  }>(
    `SELECT ar.approval_request_id,
            COALESCE(ar.context->>'title', ar.action_code) AS title,
            ar.status, ar.resource_type, ar.created_at
     FROM governance.approval_request ar
     WHERE ar.scope_id = $1::uuid AND ar.status IN ('PENDING','IN_REVIEW')
     ORDER BY ar.created_at DESC
     LIMIT $2`,
    [momentId, limit]
  );
  return {
    momentId,
    companyId: scope.companyId,
    items: rows.rows.map((r) => ({
      approvalRequestId: r.approval_request_id,
      title: r.title,
      status: r.status,
      resourceType: r.resource_type,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listBusinessMemories(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const rows = await client.query<{
    memory_id: string;
    title: string;
    summary: string | null;
    memory_type: string;
    occurred_at: Date | null;
  }>(
    `SELECT memory_id, title, summary, memory_type, occurred_at
     FROM memory.memory
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY COALESCE(occurred_at, created_at) DESC
     LIMIT $2`,
    [momentId, limit]
  );
  return {
    momentId,
    companyId: scope.companyId,
    items: rows.rows.map((r) => ({
      memoryId: r.memory_id,
      title: r.title,
      body: r.summary,
      memoryType: r.memory_type,
      occurredAt: r.occurred_at?.toISOString() ?? null,
    })),
  };
}

/** Structured timeline merging domain tables + projection events. */
export async function listBusinessMomentTimeline(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  limit = 50
) {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const [expenses, issues, improvements, updates, proj] = await Promise.all([
    listBusinessExpenses(client, ctx, momentId, 20),
    listBusinessIssues(client, ctx, momentId, 20),
    listBusinessImprovements(client, ctx, momentId, 20),
    listBusinessUpdates(client, ctx, momentId, 20),
    client.query<{ card_payload: Record<string, unknown> }>(
      `SELECT card_payload FROM projection.business_moments
       WHERE company_id = $1 AND moment_id = $2`,
      [scope.companyId, momentId]
    ),
  ]);

  type TimelineItem = {
    eventId: string;
    eventType: string;
    title: string;
    category: string;
    description?: string | null;
    occurredAt: string;
  };

  const items: TimelineItem[] = [];

  for (const e of expenses.items) {
    items.push({
      eventId: e.expenseId,
      eventType: 'EXPENSE',
      title: e.description ?? `Expense ${e.amount}`,
      category: e.categoryCode ?? 'Spend',
      description: e.description,
      occurredAt: e.effectiveAt,
    });
  }
  for (const i of issues.items) {
    items.push({
      eventId: i.issueId,
      eventType: 'ISSUE',
      title: i.title,
      category: 'Issues',
      description: i.severity,
      occurredAt: i.openedAt,
    });
  }
  for (const imp of improvements.items) {
    items.push({
      eventId: imp.improvementId,
      eventType: 'IMPROVEMENT',
      title: imp.title,
      category: 'Updates',
      occurredAt: imp.createdAt,
    });
  }
  for (const u of updates.items) {
    items.push({
      eventId: u.updateId,
      eventType: 'UPDATE',
      title: u.title,
      category: 'Updates',
      description: u.body,
      occurredAt: u.createdAt,
    });
  }

  const stored = (proj.rows[0]?.card_payload?.events ?? []) as TimelineItem[];
  for (const s of stored) {
    if (!items.some((i) => i.eventId === s.eventId)) items.push(s);
  }

  items.sort((a, b) => (a.occurredAt < b.occurredAt ? 1 : -1));

  const contracts = await client.query<{ n: string; high: string }>(
    `SELECT COUNT(*)::text AS n,
            COUNT(*) FILTER (WHERE status = 'ACTIVE')::text AS high
     FROM business.vendor_contract vc
     JOIN business.vendor v ON v.vendor_id = vc.vendor_id
     WHERE v.company_id = $1`,
    [scope.companyId]
  ).catch(() => ({ rows: [{ n: '0', high: '0' }] }));

  const highPriorityIssues = issues.items.filter(
    (i) => i.severity === 'CRITICAL' || i.severity === 'HIGH'
  ).length;

  return {
    momentId,
    companyId: scope.companyId,
    items: items.slice(0, limit),
    kpis: {
      spendEvents: expenses.items.length,
      issueCount: issues.items.length,
      highPriorityIssues,
      updateCount: updates.items.length,
      activeContracts: parseInt(contracts.rows[0]?.high ?? '0', 10),
      vendorCount: 0,
    },
  };
}
