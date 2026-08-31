/**
 * Business Operations precision — CL-25..CL-29 writers over V005 SQL + V051 improvement.
 */
import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { assertGovernanceAllowed } from '../governance/resolver';
import { assertCompanyMomentAccess } from './membership';

async function bumpRecentActivity(
  client: PoolClient,
  ctx: RequestContext,
  opts: {
    momentId: string;
    companyId: string;
    domainEventId: string;
    activityCode: string;
    title: string;
    payload: Record<string, unknown>;
  }
): Promise<void> {
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'BUSINESS', 'MOMENT', $3, $4, $5, now(), $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      opts.domainEventId,
      opts.momentId,
      opts.activityCode,
      opts.title,
      JSON.stringify({ companyId: opts.companyId, ...opts.payload }),
    ]
  );
}

export const updateVendorSchema = z
  .object({
    name: z.string().min(1).max(300).optional(),
    vendorType: z.string().max(100).optional(),
    status: z.enum(['ACTIVE', 'INACTIVE', 'BLOCKED', 'ARCHIVED']).optional(),
    note: z.string().max(2000).optional(),
  })
  .strict()
  .refine((b) => b.name != null || b.vendorType != null || b.status != null || b.note != null, {
    message: 'At least one vendor field is required.',
  });

export const createVendorContractSchema = z
  .object({
    contractName: z.string().min(1).max(300),
    contractReference: z.string().max(200).optional(),
    startDate: z.string().date().optional(),
    endDate: z.string().date().optional(),
    contractValue: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).optional(),
  })
  .strict();

export const createSlaDefinitionSchema = z
  .object({
    name: z.string().min(1).max(200),
    metricCode: z.string().regex(/^[A-Z][A-Z0-9_]*$/),
    targetValue: z.number().optional(),
    comparator: z.enum(['LT', 'LTE', 'EQ', 'GTE', 'GT']),
    unitCode: z.string().max(40).optional(),
    measurementPeriod: z.string().max(80).optional(),
    vendorContractId: z.string().uuid().optional(),
  })
  .strict();

export const createSlaCheckSchema = z
  .object({
    observedAt: z.string().datetime().optional(),
    observedValue: z.number().optional(),
    result: z.enum(['PASS', 'FAIL', 'UNKNOWN']),
    evidence: z.record(z.string(), z.unknown()).optional(),
    note: z.string().max(2000).optional(),
  })
  .strict();

export const createIssueSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
    vendorId: z.string().uuid().optional(),
  })
  .strict();

export const createRiskSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    likelihood: z.enum(['LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH']).optional(),
    impact: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
    mitigationText: z.string().max(5000).optional(),
  })
  .strict();

export const createImprovementSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    categoryCode: z.string().max(80).optional(),
    impactEstimate: z.string().max(500).optional(),
  })
  .strict();

export const createBusinessUpdateSchema = z
  .object({
    title: z.string().max(300).optional(),
    body: z.string().min(1).max(8000),
  })
  .strict();

export const createApprovalRequestSchema = z
  .object({
    title: z.string().min(1).max(500),
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).optional(),
    note: z.string().max(2000).optional(),
  })
  .strict();

export async function updateVendor(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  vendorId: string,
  body: z.infer<typeof updateVendorSchema>
): Promise<{ vendorId: string; companyId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'VENDOR_MANAGE',
    resourceType: 'VENDOR',
    companyId,
  });
  const contactPatch =
    body.note != null
      ? await client.query(
          `UPDATE business.vendor
           SET name = COALESCE($3, name),
               vendor_type = COALESCE($4, vendor_type),
               status = COALESCE($5, status),
               contact_details = contact_details || jsonb_build_object('note', $6::text),
               version = version + 1,
               updated_at = now()
           WHERE vendor_id = $1 AND company_id = $2
           RETURNING vendor_id`,
          [vendorId, companyId, body.name ?? null, body.vendorType ?? null, body.status ?? null, body.note]
        )
      : await client.query(
          `UPDATE business.vendor
           SET name = COALESCE($3, name),
               vendor_type = COALESCE($4, vendor_type),
               status = COALESCE($5, status),
               version = version + 1,
               updated_at = now()
           WHERE vendor_id = $1 AND company_id = $2
           RETURNING vendor_id`,
          [vendorId, companyId, body.name ?? null, body.vendorType ?? null, body.status ?? null]
        );
  if (!contactPatch.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Vendor not found.', 404);
  }
  return { vendorId, companyId };
}

export async function createVendorContract(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  vendorId: string,
  body: z.infer<typeof createVendorContractSchema>
): Promise<{ vendorContractId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'VENDOR_MANAGE',
    resourceType: 'VENDOR_CONTRACT',
    companyId,
  });
  const r = await client.query<{ vendor_contract_id: string }>(
    `INSERT INTO business.vendor_contract (
       company_id, vendor_id, contract_name, contract_reference,
       start_date, end_date, contract_value, currency_code, status, version
     ) VALUES ($1,$2,$3,$4,$5::date,$6::date,$7::numeric,$8,'ACTIVE',1)
     RETURNING vendor_contract_id`,
    [
      companyId,
      vendorId,
      body.contractName,
      body.contractReference ?? null,
      body.startDate ?? null,
      body.endDate ?? null,
      body.contractValue ?? null,
      body.currencyCode ?? null,
    ]
  );
  return { vendorContractId: r.rows[0]!.vendor_contract_id };
}

export async function createSlaDefinition(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  vendorId: string,
  body: z.infer<typeof createSlaDefinitionSchema>
): Promise<{ slaDefinitionId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'SLA_MANAGE',
    resourceType: 'SLA',
    companyId,
  });
  const r = await client.query<{ sla_definition_id: string }>(
    `INSERT INTO business.sla_definition (
       company_id, vendor_id, vendor_contract_id, name, metric_code,
       target_value, comparator, unit_code, measurement_period, status, version
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,'ACTIVE',1)
     RETURNING sla_definition_id`,
    [
      companyId,
      vendorId,
      body.vendorContractId ?? null,
      body.name,
      body.metricCode,
      body.targetValue ?? null,
      body.comparator,
      body.unitCode ?? null,
      body.measurementPeriod ?? null,
    ]
  );
  return { slaDefinitionId: r.rows[0]!.sla_definition_id };
}

export async function recordSlaCheck(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  slaDefinitionId: string,
  body: z.infer<typeof createSlaCheckSchema>
): Promise<{ slaCheckId: string; vendorId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'SLA_MANAGE',
    resourceType: 'SLA_CHECK',
    companyId,
  });
  const def = await client.query<{ vendor_id: string }>(
    `SELECT vendor_id FROM business.sla_definition
     WHERE sla_definition_id = $1 AND company_id = $2 AND status = 'ACTIVE'`,
    [slaDefinitionId, companyId]
  );
  if (!def.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'SLA definition not found.', 404);
  }
  const vendorId = def.rows[0].vendor_id;
  const r = await client.query<{ sla_check_id: string }>(
    `INSERT INTO business.sla_check (
       sla_definition_id, company_id, vendor_id, observed_at, observed_value, result, evidence
     ) VALUES ($1,$2,$3,COALESCE($4::timestamptz, now()),$5,$6,$7::jsonb)
     RETURNING sla_check_id`,
    [
      slaDefinitionId,
      companyId,
      vendorId,
      body.observedAt ?? null,
      body.observedValue ?? null,
      body.result,
      JSON.stringify({ ...(body.evidence ?? {}), note: body.note ?? null }),
    ]
  );
  return { slaCheckId: r.rows[0]!.sla_check_id, vendorId };
}

export async function createIssue(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createIssueSchema>
): Promise<{ issueId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ISSUE_CREATE',
    resourceType: 'ISSUE',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ issue_id: string }>(
    `INSERT INTO business.issue (
       company_id, moment_id, vendor_id, title, description, severity, status, version
     ) VALUES ($1,$2,$3,$4,$5,$6,'OPEN',1)
     RETURNING issue_id`,
    [
      scope.companyId,
      momentId,
      body.vendorId ?? null,
      body.title,
      body.description ?? null,
      body.severity ?? 'MEDIUM',
    ]
  );
  const issueId = r.rows[0]!.issue_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessIssueCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'ISSUE',
    aggregateId: issueId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { issueId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId,
    companyId: scope.companyId,
    domainEventId,
    activityCode: 'ISSUE_REPORTED',
    title: body.title,
    payload: { issueId, severity: body.severity ?? 'MEDIUM' },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    './business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: issueId,
    eventType: 'ISSUE',
    title: body.title,
    category: 'Issues',
    description: body.description ?? body.severity ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { issueId, momentId };
}

export async function createRisk(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createRiskSchema>
): Promise<{ riskId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'RISK_CREATE',
    resourceType: 'RISK',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ risk_id: string }>(
    `INSERT INTO business.risk (
       company_id, moment_id, title, description, likelihood, impact, mitigation_text, status, version
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,'OPEN',1)
     RETURNING risk_id`,
    [
      scope.companyId,
      momentId,
      body.title,
      body.description ?? null,
      body.likelihood ?? 'MEDIUM',
      body.impact ?? 'MEDIUM',
      body.mitigationText ?? null,
    ]
  );
  const riskId = r.rows[0]!.risk_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessRiskCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'RISK',
    aggregateId: riskId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { riskId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId,
    companyId: scope.companyId,
    domainEventId,
    activityCode: 'RISK_FLAGGED',
    title: body.title,
    payload: {
      riskId,
      likelihood: body.likelihood ?? 'MEDIUM',
      impact: body.impact ?? 'MEDIUM',
    },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    './business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: riskId,
    eventType: 'RISK',
    title: body.title,
    category: 'Risks',
    description: body.description ?? body.impact ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { riskId, momentId };
}

export async function createImprovement(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createImprovementSchema>
): Promise<{ improvementId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ISSUE_CREATE',
    resourceType: 'OPERATIONAL_IMPROVEMENT',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ operational_improvement_id: string }>(
    `INSERT INTO business.operational_improvement (
       company_id, moment_id, title, description, category_code, impact_estimate,
       status, created_by_user_id
     ) VALUES ($1,$2,$3,$4,$5,$6,'LOGGED',$7)
     RETURNING operational_improvement_id`,
    [
      scope.companyId,
      momentId,
      body.title,
      body.description ?? null,
      body.categoryCode ?? null,
      body.impactEstimate ?? null,
      ctx.userId,
    ]
  );
  const improvementId = r.rows[0]!.operational_improvement_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'OperationalImprovementLogged',
    domainCode: 'BUSINESS',
    aggregateType: 'OPERATIONAL_IMPROVEMENT',
    aggregateId: improvementId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { improvementId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId,
    companyId: scope.companyId,
    domainEventId,
    activityCode: 'IMPROVEMENT_LOGGED',
    title: body.title,
    payload: { improvementId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    './business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: improvementId,
    eventType: 'IMPROVEMENT',
    title: body.title,
    category: 'Updates',
    description: body.description ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { improvementId, momentId };
}

export async function createBusinessUpdate(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBusinessUpdateSchema>
): Promise<{ updateId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const r = await client.query<{ business_update_id: string }>(
    `INSERT INTO business.business_update (
       company_id, moment_id, author_user_id, title, body, status
     ) VALUES ($1,$2,$3,$4,$5,'PUBLISHED')
     RETURNING business_update_id`,
    [scope.companyId, momentId, ctx.userId, body.title ?? null, body.body]
  );
  const updateId = r.rows[0]!.business_update_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessUpdatePublished',
    domainCode: 'BUSINESS',
    aggregateType: 'BUSINESS_UPDATE',
    aggregateId: updateId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { updateId, momentId, title: body.title ?? null },
  });
  await bumpRecentActivity(client, ctx, {
    momentId,
    companyId: scope.companyId,
    domainEventId,
    activityCode: 'BUSINESS_UPDATE',
    title: body.title ?? body.body.slice(0, 80),
    payload: { updateId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    './business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: updateId,
    eventType: 'UPDATE',
    title: body.title ?? body.body.slice(0, 80),
    category: 'Updates',
    description: body.body,
    occurredAt: new Date().toISOString(),
  });
  return { updateId, momentId };
}

export async function createApprovalRequest(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createApprovalRequestSchema>
): Promise<{ approvalRequestId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'EXPENSE_CREATE',
    resourceType: 'APPROVAL_REQUEST',
    companyId: scope.companyId,
    momentId,
  });
  const resourceId = randomUUID();
  const r = await client.query<{ approval_request_id: string }>(
    `WITH ar AS (
       INSERT INTO governance.approval_request (
         requested_by_user_id, scope_type, scope_id, resource_type, resource_id,
         action_code, status, context, version
       ) VALUES ($1, 'MOMENT', $2::uuid, 'BUSINESS_OPS', $3::uuid, 'APPROVAL_REQUEST', 'PENDING', $4::jsonb, 1)
       RETURNING approval_request_id
     ),
     step AS (
       INSERT INTO governance.approval_step (
         approval_request_id, step_number, step_type, minimum_approvals, status
       )
       SELECT approval_request_id, 1, 'SYSTEM', 1, 'PENDING' FROM ar
     )
     SELECT approval_request_id FROM ar`,
    [
      ctx.userId,
      momentId,
      resourceId,
      JSON.stringify({
        title: body.title,
        amount: body.amount ?? null,
        currencyCode: body.currencyCode ?? null,
        note: body.note ?? null,
        source: 'OPS_QUICK_ADD',
        companyId: scope.companyId,
      }),
    ]
  );
  const approvalRequestId = r.rows[0]!.approval_request_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ApprovalRequested',
    domainCode: 'BUSINESS',
    aggregateType: 'APPROVAL_REQUEST',
    aggregateId: approvalRequestId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { approvalRequestId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId,
    companyId: scope.companyId,
    domainEventId,
    activityCode: 'APPROVAL_REQUESTED',
    title: body.title,
    payload: { approvalRequestId },
  });
  return { approvalRequestId, momentId };
}

/** Honest Ops aggregations for pulse enrichment. */
export async function loadOpsPulseExtras(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<{
  monthlySpend: string | null;
  activeVendorCount: number;
  slaCompliancePct: number | null;
  openIssueCount: number;
  spendByCategory: Array<{ label: string; pct: number }>;
  needsAttention: Array<{ title: string; severity: string; issueId: string }>;
  sectionQuality: Record<string, string>;
  spendVsForecast: string | null;
  spendTrendPct: number | null;
  vendorTrendPct: number | null;
  slaTrendPct: number | null;
  categoryShares: Array<{ label: string; pct: number }>;
}> {
  const spend = await client
    .query<{ total: string }>(
      `SELECT COALESCE(SUM(e.amount),0)::text AS total
       FROM finance.expense e
       JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
       WHERE bec.company_id = $1 AND bec.moment_id = $2
         AND e.status IN ('POSTED','DRAFT')
         AND e.effective_at >= date_trunc('month', now())`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [{ total: '0' }] }));

  const vendors = await client
    .query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.vendor
       WHERE company_id = $1 AND status = 'ACTIVE'`,
      [companyId]
    )
    .catch(() => ({ rows: [{ n: '0' }] }));

  const sla = await client
    .query<{ pass: string; total: string }>(
      `SELECT
         COUNT(*) FILTER (WHERE result = 'PASS')::text AS pass,
         COUNT(*)::text AS total
       FROM business.sla_check
       WHERE company_id = $1 AND observed_at >= now() - interval '90 days'`,
      [companyId]
    )
    .catch(() => ({ rows: [{ pass: '0', total: '0' }] }));

  const issues = await client
    .query<{ issue_id: string; title: string; severity: string }>(
      `SELECT issue_id, title, severity FROM business.issue
       WHERE company_id = $1 AND moment_id = $2 AND status IN ('OPEN','IN_PROGRESS','BLOCKED')
       ORDER BY
         CASE severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END,
         opened_at DESC
       LIMIT 5`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ issue_id: string; title: string; severity: string }> }));

  const cats = await client
    .query<{ category_code: string; amt: string }>(
      `SELECT COALESCE(e.category_code,'OTHER') AS category_code, SUM(e.amount)::text AS amt
       FROM finance.expense e
       JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
       WHERE bec.company_id = $1 AND bec.moment_id = $2 AND e.status IN ('POSTED','DRAFT')
       GROUP BY 1 ORDER BY SUM(e.amount) DESC LIMIT 6`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ category_code: string; amt: string }> }));

  const catTotal = cats.rows.reduce((s, r) => s + parseFloat(r.amt || '0'), 0);
  const spendByCategory = cats.rows.map((r) => ({
    label: r.category_code,
    pct: catTotal > 0 ? Math.round((parseFloat(r.amt) / catTotal) * 100) : 0,
  }));

  const slaTotal = parseInt(sla.rows[0]?.total ?? '0', 10);
  const slaPass = parseInt(sla.rows[0]?.pass ?? '0', 10);
  const slaCompliancePct = slaTotal > 0 ? Math.round((slaPass / slaTotal) * 100) : null;
  const monthlySpend = spend.rows[0]?.total ?? '0';
  const activeVendorCount = parseInt(vendors.rows[0]?.n ?? '0', 10);

  const setupRow = await client
    .query<{ preferences: Record<string, unknown> }>(
      `SELECT preferences FROM business.business_system_setup
       WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
       ORDER BY updated_at DESC LIMIT 1`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [] as Array<{ preferences: Record<string, unknown> }> }));
  const prefs = setupRow.rows[0]?.preferences ?? {};
  const budgetRaw = prefs.monthlyBudget ?? prefs.monthlySpending;
  const budgetNum =
    budgetRaw != null ? parseFloat(String(budgetRaw).replace(/[₹,\s]/g, '')) || 0 : 0;
  const spendNum = parseFloat(monthlySpend) || 0;
  const spendVsForecast =
    budgetNum > 0 ? `${Math.round((spendNum / budgetNum) * 100)}%` : null;

  const prevMonthSpend = await client
    .query<{ total: string }>(
      `SELECT COALESCE(SUM(e.amount),0)::text AS total
       FROM finance.expense e
       JOIN finance.business_expense_context bec ON bec.expense_id = e.expense_id
       WHERE bec.company_id = $1 AND bec.moment_id = $2
         AND e.status IN ('POSTED','DRAFT')
         AND e.effective_at >= date_trunc('month', now()) - interval '1 month'
         AND e.effective_at < date_trunc('month', now())`,
      [companyId, momentId]
    )
    .catch(() => ({ rows: [{ total: '0' }] }));
  const prevSpend = parseFloat(prevMonthSpend.rows[0]?.total ?? '0');
  const spendTrendPct =
    prevSpend > 0 ? Math.round(((spendNum - prevSpend) / prevSpend) * 100) : null;

  const shareLabels = ['SAAS', 'OFFICE', 'MARKETING', 'PROFESSIONAL_SERVICES'];
  const categoryShares = shareLabels.map((label) => {
    const match = spendByCategory.find(
      (c) => c.label.toUpperCase().includes(label.split('_')[0]!) || c.label.toUpperCase() === label
    );
    return { label: label.replace('_', ' & '), pct: match?.pct ?? 0 };
  }).filter((s) => s.pct > 0);

  return {
    monthlySpend,
    activeVendorCount,
    slaCompliancePct,
    openIssueCount: issues.rows.length,
    spendByCategory,
    needsAttention: issues.rows.map((i) => ({
      title: i.title,
      severity: i.severity,
      issueId: i.issue_id,
    })),
    spendVsForecast,
    spendTrendPct,
    vendorTrendPct: null,
    slaTrendPct: null,
    categoryShares,
    sectionQuality: {
      monthlySpend: parseFloat(monthlySpend) > 0 ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      activeVendors: activeVendorCount > 0 ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      slaCompliance: slaCompliancePct != null ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      spendByCategory: spendByCategory.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      needsAttention: issues.rows.length ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      spendVsForecast: spendVsForecast != null ? 'REAL_DATA' : 'EMPTY_SUPPORTED',
      operationsIntelligence: 'DEFERRED',
    },
  };
}
