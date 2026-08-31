import type { PoolClient } from 'pg';
import { z } from 'zod';
import Decimal from 'decimal.js';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox, recordCommandSideEffects } from '../../platform/events/outbox';
import { parseMoney } from './service';
import { config } from '../../platform/config';
import {
  assertActiveCompanyMember,
  assertCanApproveCompanyFinance,
  assertCompanyMomentAccess,
} from '../business/membership';

function isUniqueViolation(err: unknown, constraint?: string): boolean {
  if (!err || typeof err !== 'object') return false;
  const e = err as { code?: string; constraint?: string };
  if (e.code !== '23505') return false;
  if (!constraint) return true;
  return e.constraint === constraint;
}

const moneyString = z.string().regex(/^\d+(\.\d{1,4})?$/);

export const createBusinessExpenseSchema = z
  .object({
    amount: moneyString,
    currencyCode: z.string().length(3).toUpperCase(),
    description: z.string().max(500).optional(),
    merchantName: z.string().max(500).optional(),
    /** Canonical category; use PURCHASE for Figma "Purchase". */
    categoryCode: z.string().max(100).optional(),
    vendorId: z.string().uuid().optional(),
  })
  .strict();

export type CreateBusinessExpenseInput = z.infer<typeof createBusinessExpenseSchema>;

export const createBusinessRevenueSchema = z
  .object({
    amount: moneyString,
    currencyCode: z.string().length(3).toUpperCase(),
    description: z.string().max(500).optional(),
    categoryCode: z.string().max(100).optional(),
  })
  .strict();

export const createBusinessInvoiceSchema = z
  .object({
    invoiceNumber: z.string().min(1).max(100),
    invoiceDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    dueDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
    currencyCode: z.string().length(3).toUpperCase(),
    lines: z
      .array(
        z
          .object({
            description: z.string().min(1).max(500),
            quantity: moneyString,
            unitPrice: moneyString,
            taxAmount: moneyString.optional(),
          })
          .strict()
      )
      .min(1),
  })
  .strict();

export const decideApprovalSchema = z
  .object({
    decision: z.enum(['APPROVE', 'REJECT']),
    reason: z.string().max(1000).optional(),
  })
  .strict();

export const createVendorSchema = z
  .object({
    name: z.string().min(1).max(300),
    vendorType: z.string().max(100).optional(),
  })
  .strict();

function parseThreshold(raw: unknown): Decimal | null {
  if (raw == null) return null;
  const s = String(raw).replace(/[₹$,\s]/g, '');
  if (!/^\d+(\.\d{1,4})?$/.test(s)) return null;
  return new Decimal(s);
}

async function loadApprovalThreshold(
  client: PoolClient,
  companyId: string,
  momentId: string
): Promise<{ required: boolean; threshold: Decimal | null }> {
  const row = await client.query<{ preferences: Record<string, unknown> }>(
    `SELECT preferences FROM business.business_system_setup
     WHERE company_id = $1 AND moment_id = $2 AND status = 'ACTIVE'
     ORDER BY updated_at DESC LIMIT 1`,
    [companyId, momentId]
  );
  const prefs = row.rows[0]?.preferences ?? {};
  const spending = String(prefs.spendingApproval ?? prefs.approvalModel ?? '').toLowerCase();
  const required =
    spending.includes('required') ||
    spending.includes('threshold') ||
    spending.includes('manager');
  const threshold =
    parseThreshold(prefs.approvalThreshold) ?? parseThreshold(prefs.approvalAlarm);
  return { required: required && threshold != null, threshold };
}

/**
 * S9-H-OPT: membership + fail-closed policies + approval prefs in one RTT.
 */
async function prepareBusinessExpenseGate(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  companyId: string;
  businessFamily: string;
  membershipType: string;
  membershipId: string;
  required: boolean;
  threshold: Decimal | null;
}> {
  const row = await client.query<{
    company_id: string;
    business_family: string;
    membership_id: string;
    membership_type: string;
    policy_n: string;
    preferences: Record<string, unknown> | null;
  }>(
    `SELECT bmc.company_id, bmc.business_family,
            cm.company_membership_id AS membership_id, cm.membership_type,
            (SELECT COUNT(*)::text FROM governance.policy p
             JOIN governance.policy_version pv ON pv.policy_id = p.policy_id
             WHERE p.status = 'ACTIVE' AND pv.status = 'ACTIVE') AS policy_n,
            (SELECT preferences FROM business.business_system_setup bss
             WHERE bss.company_id = bmc.company_id AND bss.moment_id = bmc.moment_id
               AND bss.status = 'ACTIVE'
             ORDER BY bss.updated_at DESC LIMIT 1) AS preferences
     FROM business.business_moment_context bmc
     JOIN core.moment m ON m.moment_id = bmc.moment_id AND m.domain_code = 'BUSINESS'
     JOIN business.company_membership cm
       ON cm.company_id = bmc.company_id AND cm.user_id = $2 AND cm.status = 'ACTIVE'
     WHERE bmc.moment_id = $1 AND bmc.status = 'ACTIVE'`,
    [momentId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Not an active company member for this business moment.',
      403
    );
  }
  const r = row.rows[0];
  if (!config.governanceFailOpen && parseInt(r.policy_n ?? '0', 10) === 0) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Governance policies are not active for EXPENSE_CREATE.',
      403
    );
  }
  const prefs = r.preferences ?? {};
  const spending = String(prefs.spendingApproval ?? prefs.approvalModel ?? '').toLowerCase();
  const required =
    spending.includes('required') ||
    spending.includes('threshold') ||
    spending.includes('manager');
  const threshold =
    parseThreshold(prefs.approvalThreshold) ?? parseThreshold(prefs.approvalAlarm);
  return {
    companyId: r.company_id,
    businessFamily: r.business_family,
    membershipType: r.membership_type,
    membershipId: r.membership_id,
    required: required && threshold != null,
    threshold,
  };
}

async function upsertBusinessFinanceSnapshot(
  client: PoolClient,
  companyId: string,
  currencyCode: string,
  deltas: { expense?: Decimal; revenue?: Decimal; invoiceOutstanding?: Decimal }
): Promise<void> {
  const expense = deltas.expense ?? new Decimal(0);
  const revenue = deltas.revenue ?? new Decimal(0);
  const invoice = deltas.invoiceOutstanding ?? new Decimal(0);
  await client.query(
    `INSERT INTO projection.business_finance_snapshot (
       company_id, currency_code, expense_total, budget_total, revenue_total,
       invoice_outstanding_total, cash_balance_total, snapshot_payload, projection_version, updated_at
     ) VALUES ($1, $2, $3, 0, $4, $5, 0,
       jsonb_build_object('expenseCount', CASE WHEN $3::numeric > 0 THEN 1 ELSE 0 END), 1, now())
     ON CONFLICT (company_id, currency_code) DO UPDATE SET
       expense_total = projection.business_finance_snapshot.expense_total + EXCLUDED.expense_total,
       revenue_total = projection.business_finance_snapshot.revenue_total + EXCLUDED.revenue_total,
       invoice_outstanding_total =
         projection.business_finance_snapshot.invoice_outstanding_total + EXCLUDED.invoice_outstanding_total,
       snapshot_payload = COALESCE(projection.business_finance_snapshot.snapshot_payload, '{}'::jsonb)
         || jsonb_build_object(
              'expenseCount',
              COALESCE((projection.business_finance_snapshot.snapshot_payload->>'expenseCount')::int, 0)
                + CASE WHEN $3::numeric > 0 THEN 1 ELSE 0 END
            ),
       projection_version = projection.business_finance_snapshot.projection_version + 1,
       updated_at = now()`,
    [companyId, currencyCode, expense.toFixed(4), revenue.toFixed(4), invoice.toFixed(4)]
  );
}

async function bumpBusinessPulse(
  client: PoolClient,
  companyId: string,
  attentionDelta: number
): Promise<void> {
  await client.query(
    `INSERT INTO projection.business_pulse (
       company_id, active_moment_count, attention_count, open_issue_count, open_risk_count,
       widget_payload, projection_version, updated_at
     ) VALUES ($1, 0, GREATEST($2, 0), 0, 0, '{}'::jsonb, 1, now())
     ON CONFLICT (company_id) DO UPDATE SET
       attention_count = GREATEST(projection.business_pulse.attention_count + $2, 0),
       projection_version = projection.business_pulse.projection_version + 1,
       updated_at = now()`,
    [companyId, attentionDelta]
  );
}

async function insertMomentActivity(
  client: PoolClient,
  ctx: RequestContext,
  domainEventId: string,
  momentId: string,
  activityCode: string,
  title: string,
  payload: Record<string, unknown>
): Promise<void> {
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'BUSINESS', 'MOMENT', $3::uuid, $4, $5, now(), $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [ctx.userId, domainEventId, momentId, activityCode, title, JSON.stringify(payload)]
  );
}

export async function createBusinessExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: CreateBusinessExpenseInput
): Promise<{
  expenseId: string;
  momentId: string;
  companyId: string;
  amount: string;
  currencyCode: string;
  categoryCode: string | null;
  status: string;
  approvalRequestId: string | null;
  version: number;
}> {
  const amount = parseMoney(body.amount);
  if (amount.lte(0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Amount must be positive.', 400);
  }

  const scope = await prepareBusinessExpenseGate(client, ctx, momentId);

  if (body.vendorId) {
    const vendor = await client.query<{ ok: boolean }>(
      `SELECT EXISTS (
         SELECT 1 FROM business.vendor
         WHERE vendor_id = $1 AND company_id = $2 AND status = 'ACTIVE'
       ) AS ok`,
      [body.vendorId, scope.companyId]
    );
    if (!vendor.rows[0]?.ok) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Vendor not found for this company.', 400);
    }
  }

  const needsApproval = scope.required && scope.threshold != null && amount.gte(scope.threshold);
  const status = needsApproval ? 'DRAFT' : 'POSTED';
  const threshold = scope.threshold;

  const expenseInsert = await client.query<{ expense_id: string; version: string }>(
    needsApproval
      ? `WITH e AS (
           INSERT INTO finance.expense (
             moment_id, domain_code, created_by_user_id, merchant_name, description, category_code,
             amount, currency_code, effective_at, status, posted_at, version
           ) VALUES ($1, 'BUSINESS', $2, $3, $4, $5, $6, $7, now(), $8, $9, 1)
           RETURNING expense_id, version
         ),
         ctx AS (
           INSERT INTO finance.business_expense_context (
             expense_id, moment_id, domain_code, company_id, vendor_id
           )
           SELECT expense_id, $1::uuid, 'BUSINESS', $10::uuid, $11::uuid FROM e
         )
         SELECT expense_id, version FROM e`
      : `WITH e AS (
           INSERT INTO finance.expense (
             moment_id, domain_code, created_by_user_id, merchant_name, description, category_code,
             amount, currency_code, effective_at, status, posted_at, version
           ) VALUES ($1, 'BUSINESS', $2, $3, $4, $5, $6, $7, now(), $8, $9, 1)
           RETURNING expense_id, version
         ),
         ctx AS (
           INSERT INTO finance.business_expense_context (
             expense_id, moment_id, domain_code, company_id, vendor_id
           )
           SELECT expense_id, $1::uuid, 'BUSINESS', $10::uuid, $11::uuid FROM e
         ),
         snap AS (
           INSERT INTO projection.business_finance_snapshot (
             company_id, currency_code, expense_total, budget_total, revenue_total,
             invoice_outstanding_total, cash_balance_total, snapshot_payload, projection_version, updated_at
           ) VALUES ($10::uuid, $7, $6::numeric, 0, 0, 0, 0,
             jsonb_build_object('expenseCount', 1), 1, now())
           ON CONFLICT (company_id, currency_code) DO UPDATE SET
             expense_total = projection.business_finance_snapshot.expense_total + EXCLUDED.expense_total,
             snapshot_payload = COALESCE(projection.business_finance_snapshot.snapshot_payload, '{}'::jsonb)
               || jsonb_build_object(
                    'expenseCount',
                    COALESCE((projection.business_finance_snapshot.snapshot_payload->>'expenseCount')::int, 0) + 1
                  ),
             projection_version = projection.business_finance_snapshot.projection_version + 1,
             updated_at = now()
         )
         SELECT expense_id, version FROM e`,
    [
      momentId,
      ctx.userId,
      body.merchantName ?? null,
      body.description ?? null,
      body.categoryCode ?? null,
      amount.toFixed(4),
      body.currencyCode,
      status,
      needsApproval ? null : new Date(),
      scope.companyId,
      body.vendorId ?? null,
    ]
  );
  const expenseId = expenseInsert.rows[0]!.expense_id;

  let approvalRequestId: string | null = null;
  if (needsApproval) {
    const ar = await client.query<{ approval_request_id: string }>(
      `WITH ar AS (
         INSERT INTO governance.approval_request (
           requested_by_user_id, scope_type, scope_id, resource_type, resource_id,
           action_code, status, context, version
         ) VALUES ($1, 'COMPANY', $2::uuid, 'EXPENSE', $3::uuid, 'EXPENSE_CREATE', 'PENDING', $4::jsonb, 1)
         RETURNING approval_request_id
       ),
       step AS (
         INSERT INTO governance.approval_step (
           approval_request_id, step_number, step_type, minimum_approvals, status
         )
         SELECT approval_request_id, 1, 'SYSTEM', 1, 'PENDING' FROM ar
       ),
       pulse AS (
         INSERT INTO projection.business_pulse (
           company_id, active_moment_count, attention_count, open_issue_count, open_risk_count,
           widget_payload, projection_version, updated_at
         ) VALUES ($2::uuid, 0, 1, 0, 0, '{}'::jsonb, 1, now())
         ON CONFLICT (company_id) DO UPDATE SET
           attention_count = GREATEST(projection.business_pulse.attention_count + 1, 0),
           projection_version = projection.business_pulse.projection_version + 1,
           updated_at = now()
       )
       SELECT approval_request_id FROM ar`,
      [
        ctx.userId,
        scope.companyId,
        expenseId,
        JSON.stringify({
          momentId,
          companyId: scope.companyId,
          amount: amount.toFixed(4),
          currencyCode: body.currencyCode,
          threshold: threshold!.toFixed(4),
        }),
      ]
    );
    approvalRequestId = ar.rows[0]!.approval_request_id;
  }

  const result = {
    expenseId,
    momentId,
    companyId: scope.companyId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    categoryCode: body.categoryCode ?? null,
    status,
    approvalRequestId,
    version: parseInt(expenseInsert.rows[0]!.version, 10),
  };

  await recordCommandSideEffects(client, ctx, {
    eventName: needsApproval ? 'BusinessExpenseSubmitted' : 'BusinessExpensePosted',
    domainCode: 'BUSINESS',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: result as unknown as Record<string, unknown>,
    auditActionCode: 'EXPENSE_CREATE',
    auditResourceType: 'EXPENSE',
    auditResourceId: expenseId,
    afterSnapshot: result,
    activity: {
      domainCode: 'BUSINESS',
      momentId,
      activityCode: 'BUSINESS_EXPENSE',
      title: body.description ?? 'Expense',
      payload: result as unknown as Record<string, unknown>,
    },
  });

  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    '../business/business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: expenseId,
    eventType: 'EXPENSE',
    title: body.description ?? 'Expense',
    category: body.categoryCode ?? 'Spend',
    description: body.description,
    occurredAt: new Date().toISOString(),
    payload: { amount: amount.toFixed(4), currencyCode: body.currencyCode },
  });

  return result;
}

export async function decideBusinessApproval(
  client: PoolClient,
  ctx: RequestContext,
  approvalRequestId: string,
  body: z.infer<typeof decideApprovalSchema>
): Promise<{
  approvalRequestId: string;
  decision: string;
  expenseId: string;
  expenseStatus: string;
}> {
  const ar = await client.query<{
    approval_request_id: string;
    resource_type: string;
    resource_id: string;
    scope_id: string;
    status: string;
  }>(
    `SELECT approval_request_id, resource_type, resource_id, scope_id::text, status
     FROM governance.approval_request WHERE approval_request_id = $1`,
    [approvalRequestId]
  );
  if (!ar.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Approval request not found.', 404);
  }
  const reqRow = ar.rows[0];
  if (reqRow.status !== 'PENDING' && reqRow.status !== 'IN_REVIEW') {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Approval request is not pending.', 409);
  }
  if (reqRow.resource_type !== 'EXPENSE') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Unsupported approval resource.', 400);
  }

  const companyId = reqRow.scope_id;
  await assertCanApproveCompanyFinance(client, ctx, companyId);

  const step = await client.query<{ approval_step_id: string }>(
    `SELECT approval_step_id FROM governance.approval_step
     WHERE approval_request_id = $1 AND step_number = 1`,
    [approvalRequestId]
  );
  if (!step.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Approval step not found.', 404);
  }

  await client.query(
    `INSERT INTO governance.approval_decision (
       approval_request_id, approval_step_id, decided_by_user_id, decision, reason, decided_at
     ) VALUES ($1, $2, $3, $4, $5, now())`,
    [
      approvalRequestId,
      step.rows[0].approval_step_id,
      ctx.userId,
      body.decision,
      body.reason ?? null,
    ]
  );

  const finalStatus = body.decision === 'APPROVE' ? 'APPROVED' : 'REJECTED';
  await client.query(
    `UPDATE governance.approval_request
     SET status = $2, completed_at = now(), version = version + 1, updated_at = now()
     WHERE approval_request_id = $1`,
    [approvalRequestId, finalStatus]
  );
  await client.query(
    `UPDATE governance.approval_step
     SET status = $2, completed_at = now(), updated_at = now()
     WHERE approval_step_id = $1`,
    [step.rows[0].approval_step_id, finalStatus]
  );

  const expenseId = reqRow.resource_id;
  let expenseStatus = 'DRAFT';
  let momentIdForActivity: string | null = null;

  if (body.decision === 'APPROVE') {
    const posted = await client.query<{ amount: string; currency_code: string; moment_id: string }>(
      `UPDATE finance.expense
       SET status = 'POSTED', posted_at = now(), version = version + 1, updated_at = now()
       WHERE expense_id = $1 AND status = 'DRAFT'
       RETURNING amount::text, currency_code, moment_id`,
      [expenseId]
    );
    if (!posted.rows[0]) {
      throw new AppError(ErrorCode.VERSION_CONFLICT, 'Expense is not in DRAFT state.', 409);
    }
    expenseStatus = 'POSTED';
    momentIdForActivity = posted.rows[0].moment_id;
    await upsertBusinessFinanceSnapshot(
      client,
      companyId,
      posted.rows[0].currency_code,
      { expense: new Decimal(posted.rows[0].amount) }
    );
  } else {
    const voided = await client.query<{ moment_id: string }>(
      `UPDATE finance.expense
       SET status = 'VOIDED', version = version + 1, updated_at = now()
       WHERE expense_id = $1 AND status = 'DRAFT'
       RETURNING moment_id`,
      [expenseId]
    );
    expenseStatus = 'VOIDED';
    momentIdForActivity = voided.rows[0]?.moment_id ?? null;
  }

  await bumpBusinessPulse(client, companyId, -1);

  const result = {
    approvalRequestId,
    decision: body.decision,
    expenseId,
    expenseStatus,
  };
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: body.decision === 'APPROVE' ? 'BusinessExpenseApproved' : 'BusinessExpenseRejected',
    domainCode: 'BUSINESS',
    aggregateType: 'APPROVAL_REQUEST',
    aggregateId: approvalRequestId,
    scopeType: 'COMPANY',
    scopeId: companyId,
    payload: result,
  });
  await insertAudit(
    client,
    ctx,
    body.decision === 'APPROVE' ? 'APPROVAL_APPROVE' : 'APPROVAL_REJECT',
    'APPROVAL_REQUEST',
    approvalRequestId,
    domainEventId,
    result
  );
  if (momentIdForActivity) {
    await insertMomentActivity(
      client,
      ctx,
      domainEventId,
      momentIdForActivity,
      body.decision === 'APPROVE' ? 'BUSINESS_EXPENSE_APPROVED' : 'BUSINESS_EXPENSE_REJECTED',
      body.decision === 'APPROVE' ? 'Expense approved' : 'Expense rejected',
      result
    );
    const fam = await client.query<{ business_family: string }>(
      `SELECT business_family FROM business.business_moment_context WHERE moment_id = $1`,
      [momentIdForActivity]
    );
    const { refreshBusinessProjectionsAfterWrite } = await import('../business/business-projection');
    await refreshBusinessProjectionsAfterWrite(
      client,
      companyId,
      momentIdForActivity,
      fam.rows[0]?.business_family ?? 'BUSINESS_OPERATIONS'
    );
  }

  return result;
}

export async function createBusinessRevenue(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBusinessRevenueSchema>
): Promise<{ revenueId: string; companyId: string; amount: string; currencyCode: string; status: string }> {
  const amount = parseMoney(body.amount);
  if (amount.lte(0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Amount must be positive.', 400);
  }
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  const inserted = await client.query<{ revenue_id: string }>(
    `WITH r AS (
       INSERT INTO finance.revenue (
         company_id, moment_id, description, category_code, amount, currency_code, effective_at, status, version
       ) VALUES ($1, $2, $3, $4, $5, $6, now(), 'POSTED', 1)
       RETURNING revenue_id
     ),
     snap AS (
       INSERT INTO projection.business_finance_snapshot (
         company_id, currency_code, expense_total, budget_total, revenue_total,
         invoice_outstanding_total, cash_balance_total, snapshot_payload, projection_version, updated_at
       ) VALUES ($1, $6, 0, 0, $5::numeric, 0, 0, '{}'::jsonb, 1, now())
       ON CONFLICT (company_id, currency_code) DO UPDATE SET
         revenue_total = projection.business_finance_snapshot.revenue_total + EXCLUDED.revenue_total,
         projection_version = projection.business_finance_snapshot.projection_version + 1,
         updated_at = now()
     )
     SELECT revenue_id FROM r`,
    [
      scope.companyId,
      momentId,
      body.description ?? null,
      body.categoryCode ?? null,
      amount.toFixed(4),
      body.currencyCode,
    ]
  );
  const revenueId = inserted.rows[0]!.revenue_id;
  const result = {
    revenueId,
    companyId: scope.companyId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
  };
  await recordCommandSideEffects(client, ctx, {
    eventName: 'BusinessRevenuePosted',
    domainCode: 'BUSINESS',
    aggregateType: 'REVENUE',
    aggregateId: revenueId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: result,
    auditActionCode: 'REVENUE_RECORD',
    auditResourceType: 'REVENUE',
    auditResourceId: revenueId,
    afterSnapshot: result,
    activity: {
      domainCode: 'BUSINESS',
      momentId,
      activityCode: 'BUSINESS_REVENUE',
      title: body.description ?? 'Revenue',
      payload: result,
    },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    '../business/business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: revenueId,
    eventType: 'REVENUE',
    title: body.description ?? 'Revenue',
    category: 'Revenue',
    occurredAt: new Date().toISOString(),
    payload: result,
  });
  return result;
}

export async function createBusinessInvoice(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBusinessInvoiceSchema>
): Promise<{
  invoiceId: string;
  companyId: string;
  invoiceNumber: string;
  subtotalAmount: string;
  taxAmount: string;
  totalAmount: string;
  status: string;
}> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);

  let subtotal = new Decimal(0);
  let taxTotal = new Decimal(0);
  const computedLines = body.lines.map((line, idx) => {
    const qty = parseMoney(line.quantity);
    const unit = parseMoney(line.unitPrice);
    const tax = line.taxAmount != null ? parseMoney(line.taxAmount) : new Decimal(0);
    if (qty.lte(0) || unit.lt(0) || tax.lt(0)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invalid invoice line amounts.', 400);
    }
    const lineNet = qty.times(unit).toDecimalPlaces(4, Decimal.ROUND_HALF_UP);
    const lineTotal = lineNet.plus(tax);
    subtotal = subtotal.plus(lineNet);
    taxTotal = taxTotal.plus(tax);
    return {
      lineNumber: idx + 1,
      description: line.description,
      quantity: qty.toFixed(4),
      unitPrice: unit.toFixed(4),
      taxAmount: tax.toFixed(4),
      lineTotal: lineTotal.toFixed(4),
    };
  });
  const total = subtotal.plus(taxTotal);

  let invoiceId: string;
  try {
    const inv = await client.query<{ invoice_id: string }>(
      `WITH inv AS (
         INSERT INTO finance.invoice (
           company_id, moment_id, invoice_number, invoice_date, due_date, currency_code,
           subtotal_amount, tax_amount, total_amount, paid_amount, status, version
         ) VALUES ($1, $2, $3, $4::date, $5::date, $6, $7, $8, $9, 0, 'ISSUED', 1)
         RETURNING invoice_id
       ),
       lines AS (
         INSERT INTO finance.invoice_line (
           invoice_id, line_number, description, quantity, unit_price, tax_amount, line_total
         )
         SELECT inv.invoice_id, x.line_number, x.description, x.quantity::numeric,
                x.unit_price::numeric, x.tax_amount::numeric, x.line_total::numeric
         FROM inv
         CROSS JOIN UNNEST(
           $10::int[], $11::text[], $12::text[], $13::text[], $14::text[], $15::text[]
         ) AS x(line_number, description, quantity, unit_price, tax_amount, line_total)
       )
       SELECT invoice_id FROM inv`,
      [
        scope.companyId,
        momentId,
        body.invoiceNumber,
        body.invoiceDate,
        body.dueDate ?? null,
        body.currencyCode,
        subtotal.toFixed(4),
        taxTotal.toFixed(4),
        total.toFixed(4),
        computedLines.map((l) => l.lineNumber),
        computedLines.map((l) => l.description),
        computedLines.map((l) => l.quantity),
        computedLines.map((l) => l.unitPrice),
        computedLines.map((l) => l.taxAmount),
        computedLines.map((l) => l.lineTotal),
      ]
    );
    invoiceId = inv.rows[0]!.invoice_id;
  } catch (err) {
    if (isUniqueViolation(err, 'uq_invoice__company_number')) {
      throw new AppError(
        ErrorCode.INVOICE_NUMBER_CONFLICT,
        'Invoice number already exists for this company.',
        409,
        { invoiceNumber: body.invoiceNumber, companyId: scope.companyId }
      );
    }
    throw err;
  }

  await upsertBusinessFinanceSnapshot(client, scope.companyId, body.currencyCode, {
    invoiceOutstanding: total,
  });
  const result = {
    invoiceId,
    companyId: scope.companyId,
    invoiceNumber: body.invoiceNumber,
    subtotalAmount: subtotal.toFixed(4),
    taxAmount: taxTotal.toFixed(4),
    totalAmount: total.toFixed(4),
    status: 'ISSUED',
  };
  await recordCommandSideEffects(client, ctx, {
    eventName: 'BusinessInvoiceIssued',
    domainCode: 'BUSINESS',
    aggregateType: 'INVOICE',
    aggregateId: invoiceId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: result,
    auditActionCode: 'INVOICE_CREATE',
    auditResourceType: 'INVOICE',
    auditResourceId: invoiceId,
    afterSnapshot: result,
    activity: {
      domainCode: 'BUSINESS',
      momentId,
      activityCode: 'BUSINESS_INVOICE',
      title: `Invoice ${body.invoiceNumber}`,
      payload: result,
    },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import(
    '../business/business-projection'
  );
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: invoiceId,
    eventType: 'INVOICE',
    title: `Invoice ${body.invoiceNumber}`,
    category: 'Revenue',
    occurredAt: new Date().toISOString(),
    payload: result,
  });
  return result;
}

export async function createVendor(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof createVendorSchema>
): Promise<{ vendorId: string; companyId: string; name: string }> {
  await assertActiveCompanyMember(client, ctx, companyId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'VENDOR',
    companyId,
  });

  const inserted = await client.query<{ vendor_id: string }>(
    `INSERT INTO business.vendor (
       company_id, name, vendor_type, contact_details, status, version
     ) VALUES ($1, $2, $3, '{}'::jsonb, 'ACTIVE', 1)
     RETURNING vendor_id`,
    [companyId, body.name, body.vendorType ?? null]
  );
  return { vendorId: inserted.rows[0]!.vendor_id, companyId, name: body.name };
}
