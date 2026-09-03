import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';
import Decimal from 'decimal.js';
import {
  derivePaymentMethodFromAccountType,
  getAccountType,
  paymentMethodCodeSchema,
  resolveUserAccount,
} from './financial-account';
import { listExpenseAttachments } from './expense-attachments';

/** Personal Expense.Create — matches OpenAPI ExpenseCreateRequest (splits write deferred). */
export const createExpenseSchema = z
  .object({
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    currencyCode: z.string().length(3).toUpperCase(),
    description: z.string().max(500).optional(),
    merchantName: z.string().max(500).optional(),
    categoryCode: z.string().max(100).optional(),
    subcategoryCode: z.string().max(100).optional(),
    financialAccountId: z.string().uuid().optional(),
    paymentMethodCode: paymentMethodCodeSchema.optional(),
    planningClassCode: z.enum(['ESSENTIAL', 'PLANNED', 'UNPLANNED']).optional(),
    note: z.string().max(2000).optional(),
    tags: z.array(z.string().min(1).max(80)).max(20).optional(),
    effectiveAt: z
      .string()
      .refine((s) => !Number.isNaN(Date.parse(s)), { message: 'Invalid ISO datetime' })
      .nullish(),
    recurringScheduleId: z.string().uuid().optional(),
  })
  .strict();

export type CreateExpenseInput = z.infer<typeof createExpenseSchema>;

export const updateExpenseSchema = z
  .object({
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).toUpperCase().optional(),
    description: z.string().max(500).nullable().optional(),
    merchantName: z.string().max(500).nullable().optional(),
    categoryCode: z.string().max(100).nullable().optional(),
    subcategoryCode: z.string().max(100).nullable().optional(),
    financialAccountId: z.string().uuid().nullable().optional(),
    paymentMethodCode: paymentMethodCodeSchema.nullable().optional(),
    // Mobile offset ISO / Gson null — Zod .datetime() is Z-only by default.
    effectiveAt: z
      .string()
      .refine((s) => !Number.isNaN(Date.parse(s)), { message: 'Invalid ISO datetime' })
      .nullish(),
    recurringScheduleId: z.string().uuid().nullable().optional(),
    transactionType: z.never().optional(),
  })
  .strict()
  .refine(
    (b) =>
      b.amount != null ||
      b.currencyCode != null ||
      b.description !== undefined ||
      b.merchantName !== undefined ||
      b.categoryCode !== undefined ||
      b.subcategoryCode !== undefined ||
      b.financialAccountId !== undefined ||
      b.paymentMethodCode !== undefined ||
      b.effectiveAt != null ||
      b.recurringScheduleId !== undefined,
    { message: 'At least one field is required.' }
  );

export type UpdateExpenseInput = z.infer<typeof updateExpenseSchema>;

export interface ExpenseResult {
  expenseId: string;
  momentId: string;
  amount: string;
  currencyCode: string;
  status: string;
  version: number;
}

export interface ExpenseDetail extends ExpenseResult {
  description: string | null;
  merchantName: string | null;
  categoryCode: string | null;
  subcategoryCode: string | null;
  financialAccountId: string | null;
  paymentMethodCode: string | null;
  effectiveAt: string;
  recurringScheduleId: string | null;
  attachmentIds: string[];
}

export function parseMoney(amount: string): Decimal {
  const d = new Decimal(amount);
  if (d.lte(0)) throw new AppError(ErrorCode.VALIDATION_FAILED, 'Amount must be positive.', 400);
  return d;
}

function buildActivityPayload(row: {
  expenseId: string;
  amount: string;
  currencyCode: string;
  categoryCode: string | null;
  subcategoryCode?: string | null;
  description?: string | null;
  financialAccountId?: string | null;
  paymentMethodCode?: string | null;
  effectiveAt?: string;
  status: string;
}): Record<string, unknown> {
  return {
    expenseId: row.expenseId,
    amount: row.amount,
    currencyCode: row.currencyCode,
    categoryCode: row.categoryCode,
    subcategoryCode: row.subcategoryCode ?? null,
    description: row.description ?? null,
    financialAccountId: row.financialAccountId ?? null,
    paymentMethodCode: row.paymentMethodCode ?? null,
    effectiveAt: row.effectiveAt ?? null,
    status: row.status,
  };
}

async function assertPersonalMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<void> {
  const moment = await client.query(
    `SELECT 1 FROM core.moment m
     JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
     WHERE m.moment_id = $1 AND pmc.user_id = $2 AND m.domain_code = 'PERSONAL'`,
    [momentId, ctx.userId]
  );
  if (!moment.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
}

async function loadOwnedExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<{
  expense_id: string;
  amount: string;
  currency_code: string;
  description: string | null;
  merchant_name: string | null;
  category_code: string | null;
  subcategory_code: string | null;
  financial_account_id: string | null;
  payment_method_code: string | null;
  effective_at: Date;
  recurring_schedule_id: string | null;
  version: string;
  status: string;
}> {
  const existing = await client.query(
    `SELECT e.expense_id, e.amount::text AS amount, e.currency_code, e.description, e.merchant_name,
            e.category_code, e.subcategory_code, e.financial_account_id, e.payment_method_code,
            e.effective_at, e.recurring_schedule_id, e.version::text AS version, e.status
     FROM finance.expense e
     JOIN finance.personal_expense_context pec ON pec.expense_id = e.expense_id
     WHERE e.expense_id = $1 AND e.moment_id = $2 AND pec.user_id = $3`,
    [expenseId, momentId, ctx.userId]
  );
  if (!existing.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Expense not found.', 404);
  }
  return existing.rows[0] as never;
}

export async function getExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<ExpenseDetail> {
  const row = await loadOwnedExpense(client, ctx, momentId, expenseId);
  const attachments = await listExpenseAttachments(client, ctx, momentId, expenseId);
  let paymentMethod = row.payment_method_code;
  if (!paymentMethod && row.financial_account_id) {
    const accountType = await getAccountType(client, row.financial_account_id);
    if (accountType) paymentMethod = derivePaymentMethodFromAccountType(accountType);
  }
  return {
    expenseId: row.expense_id,
    momentId,
    amount: row.amount,
    currencyCode: row.currency_code,
    status: row.status,
    version: parseInt(row.version, 10),
    description: row.description,
    merchantName: row.merchant_name,
    categoryCode: row.category_code,
    subcategoryCode: row.subcategory_code,
    financialAccountId: row.financial_account_id,
    paymentMethodCode: paymentMethod,
    effectiveAt: row.effective_at.toISOString(),
    recurringScheduleId: row.recurring_schedule_id,
    attachmentIds: attachments.map((a) => a.uploadId),
  };
}

export async function createExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: CreateExpenseInput
): Promise<ExpenseResult> {
  const amount = parseMoney(body.amount);

  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'EXPENSE_CREATE',
    resourceType: 'EXPENSE',
    momentId,
  });
  await assertPersonalMoment(client, ctx, momentId);

  const accountId = await resolveUserAccount(client, ctx, body.currencyCode, body.financialAccountId ?? null);
  const accountType = await getAccountType(client, accountId);
  const paymentMethod =
    body.paymentMethodCode ?? (accountType ? derivePaymentMethodFromAccountType(accountType) : 'OTHER');
  const effectiveAt = body.effectiveAt ?? new Date().toISOString();

  const expenseInsert = await client.query<{ expense_id: string; version: string }>(
    `INSERT INTO finance.expense (
       moment_id, domain_code, financial_account_id, created_by_user_id, merchant_name, description,
       category_code, subcategory_code, payment_method_code, amount, currency_code, effective_at,
       recurring_schedule_id, status, posted_at, version
     ) VALUES ($1, 'PERSONAL', $2, $3, $4, $5, $6, $7, $8, $9, $10, COALESCE($11::timestamptz, now()),
               $12, 'POSTED', now(), 1)
     RETURNING expense_id, version`,
    [
      momentId,
      accountId,
      ctx.userId,
      body.merchantName ?? null,
      body.description ?? null,
      body.categoryCode ?? null,
      body.subcategoryCode ?? null,
      paymentMethod,
      amount.toFixed(4),
      body.currencyCode,
      body.effectiveAt ?? null,
      body.recurringScheduleId ?? null,
    ]
  );
  const expenseId = expenseInsert.rows[0].expense_id;

  // V045 columns — best-effort until migration applied everywhere.
  if (body.planningClassCode != null || body.note != null) {
    try {
      await client.query(
        `UPDATE finance.expense
         SET planning_class_code = COALESCE($2, planning_class_code),
             note = COALESCE($3, note),
             updated_at = now()
         WHERE expense_id = $1`,
        [expenseId, body.planningClassCode ?? null, body.note ?? null]
      );
    } catch {
      // Columns absent pre-V045
    }
  }

  if (body.tags?.length) {
    try {
      const { replaceExpenseTags } = await import('../personal/life-ops-precision');
      await replaceExpenseTags(client, expenseId, body.tags);
    } catch {
      // expense_tag table may be absent pre-V045
    }
  }

  await client.query(
    `INSERT INTO finance.personal_expense_context (expense_id, moment_id, user_id)
     VALUES ($1, $2, $3)`,
    [expenseId, momentId, ctx.userId]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ExpenseRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      expenseId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      userId: ctx.userId,
    },
  });

  const result: ExpenseResult = {
    expenseId,
    momentId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
    version: parseInt(expenseInsert.rows[0].version, 10),
  };

  await insertAudit(client, ctx, 'EXPENSE_CREATE', 'EXPENSE', expenseId, domainEventId, result as unknown as Record<string, unknown>);

  const title = body.description ?? body.merchantName ?? 'Expense';
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'PERSONAL', 'MOMENT', $3, 'EXPENSE_RECORDED', $4, $5::timestamptz, $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      title,
      effectiveAt,
      JSON.stringify(
        buildActivityPayload({
          expenseId,
          amount: amount.toFixed(4),
          currencyCode: body.currencyCode,
          categoryCode: body.categoryCode ?? null,
          subcategoryCode: body.subcategoryCode ?? null,
          description: body.description ?? null,
          financialAccountId: accountId,
          paymentMethodCode: paymentMethod,
          effectiveAt,
          status: 'POSTED',
        })
      ),
    ]
  );

  await bumpPersonalPulseAfterExpense(client, ctx.userId, domainEventId, body.currencyCode, amount.toFixed(4));

  try {
    const { refreshPersonalFinanceSnapshot } = await import('../personal/life-ops-precision');
    await refreshPersonalFinanceSnapshot(client, ctx.userId, momentId);
  } catch {
    // Snapshot writer optional until V045 applied
  }

  return result;
}

export async function updateExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string,
  body: UpdateExpenseInput
): Promise<ExpenseResult> {
  if (body.transactionType !== undefined) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'transactionType is not supported on expense PATCH.', 400);
  }

  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'EXPENSE_CREATE',
    resourceType: 'EXPENSE',
    momentId,
  });

  const row = await loadOwnedExpense(client, ctx, momentId, expenseId);
  if (row.status !== 'POSTED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only posted expenses can be updated.', 400);
  }

  const nextCurrency = body.currencyCode ?? row.currency_code;
  const nextAmount = body.amount != null ? parseMoney(body.amount).toFixed(4) : row.amount;
  const nextDescription = body.description !== undefined ? body.description : row.description;
  const nextMerchant = body.merchantName !== undefined ? body.merchantName : row.merchant_name;
  const nextCategory = body.categoryCode !== undefined ? body.categoryCode : row.category_code;
  const nextSubcategory = body.subcategoryCode !== undefined ? body.subcategoryCode : row.subcategory_code;
  const nextEffectiveAt = body.effectiveAt ?? row.effective_at.toISOString();

  let nextAccountId = row.financial_account_id;
  if (body.financialAccountId !== undefined) {
    nextAccountId = body.financialAccountId
      ? await resolveUserAccount(client, ctx, nextCurrency, body.financialAccountId)
      : await resolveUserAccount(client, ctx, nextCurrency, null);
  } else if (!nextAccountId) {
    nextAccountId = await resolveUserAccount(client, ctx, nextCurrency, null);
  }

  let nextPaymentMethod = body.paymentMethodCode !== undefined ? body.paymentMethodCode : row.payment_method_code;
  if (!nextPaymentMethod && nextAccountId) {
    const accountType = await getAccountType(client, nextAccountId);
    if (accountType) nextPaymentMethod = derivePaymentMethodFromAccountType(accountType);
  }

  const nextRecurring =
    body.recurringScheduleId !== undefined ? body.recurringScheduleId : row.recurring_schedule_id;

  const updated = await client.query<{ expense_id: string; version: string }>(
    `UPDATE finance.expense SET
       amount = $2,
       currency_code = $3,
       description = $4,
       merchant_name = $5,
       category_code = $6,
       subcategory_code = $7,
       financial_account_id = $8,
       payment_method_code = $9,
       effective_at = $10::timestamptz,
       recurring_schedule_id = $11,
       version = version + 1,
       updated_at = now()
     WHERE expense_id = $1
     RETURNING expense_id, version`,
    [
      expenseId,
      nextAmount,
      nextCurrency,
      nextDescription,
      nextMerchant,
      nextCategory,
      nextSubcategory,
      nextAccountId,
      nextPaymentMethod,
      nextEffectiveAt,
      nextRecurring,
    ]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ExpenseUpdated',
    domainCode: 'PERSONAL',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { expenseId, momentId, amount: nextAmount },
  });
  await insertAudit(client, ctx, 'EXPENSE_UPDATE', 'EXPENSE', expenseId, domainEventId, { expenseId });

  const title = nextDescription ?? nextMerchant ?? 'Expense';
  await client.query(
    `UPDATE projection.recent_activity SET
       title = $3,
       occurred_at = $4::timestamptz,
       activity_payload = $5::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2
       AND activity_payload->>'expenseId' = $6`,
    [
      ctx.userId,
      momentId,
      title,
      nextEffectiveAt,
      JSON.stringify(
        buildActivityPayload({
          expenseId,
          amount: nextAmount,
          currencyCode: nextCurrency,
          categoryCode: nextCategory,
          subcategoryCode: nextSubcategory,
          description: nextDescription,
          financialAccountId: nextAccountId,
          paymentMethodCode: nextPaymentMethod,
          effectiveAt: nextEffectiveAt,
          status: 'POSTED',
        })
      ),
      expenseId,
    ]
  );

  if (body.amount != null || body.currencyCode != null) {
    await adjustPersonalPulseSpend(
      client,
      ctx.userId,
      domainEventId,
      row.currency_code,
      row.amount,
      nextCurrency,
      nextAmount
    );
  }

  return {
    expenseId,
    momentId,
    amount: nextAmount,
    currencyCode: nextCurrency,
    status: row.status,
    version: parseInt(updated.rows[0]!.version, 10),
  };
}

export async function voidExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<ExpenseResult> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'EXPENSE_CREATE',
    resourceType: 'EXPENSE',
    momentId,
  });

  const row = await loadOwnedExpense(client, ctx, momentId, expenseId);
  if (row.status === 'VOIDED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Expense already voided.', 400);
  }

  await client.query(
    `UPDATE finance.expense SET status = 'VOIDED', reversed_at = now(), version = version + 1, updated_at = now()
     WHERE expense_id = $1`,
    [expenseId]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ExpenseVoided',
    domainCode: 'PERSONAL',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { expenseId, momentId },
  });
  await insertAudit(client, ctx, 'EXPENSE_VOID', 'EXPENSE', expenseId, domainEventId, { expenseId });

  await client.query(
    `UPDATE projection.recent_activity SET
       activity_payload = COALESCE(activity_payload, '{}'::jsonb) || '{"status":"VOIDED"}'::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2 AND activity_payload->>'expenseId' = $3`,
    [ctx.userId, momentId, expenseId]
  );

  await reversePersonalPulseSpend(client, ctx.userId, domainEventId, row.currency_code, row.amount);

  return {
    expenseId,
    momentId,
    amount: row.amount,
    currencyCode: row.currency_code,
    status: 'VOIDED',
    version: parseInt(row.version, 10) + 1,
  };
}

async function bumpPersonalPulseAfterExpense(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  currencyCode: string,
  amountStr: string
): Promise<void> {
  const existing = await client.query<{ widget_payload: Record<string, unknown> | null }>(
    `SELECT widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  const spendByCurrency = { ...((payload.spendByCurrency as Record<string, string>) ?? {}) };
  const prev = spendByCurrency[currencyCode] ?? '0';
  spendByCurrency[currencyCode] = new Decimal(prev).plus(amountStr).toFixed(4);
  payload.spendByCurrency = spendByCurrency;
  payload.lastExpenseAt = new Date().toISOString();

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         widget_payload = $2::jsonb,
         source_event_id = $3,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, JSON.stringify(payload), sourceEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (user_id, widget_payload, source_event_id, projection_version)
       VALUES ($1, $2::jsonb, $3, 1)`,
      [userId, JSON.stringify(payload), sourceEventId]
    );
  }
}

async function adjustPersonalPulseSpend(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  oldCurrency: string,
  oldAmount: string,
  newCurrency: string,
  newAmount: string
): Promise<void> {
  await reversePersonalPulseSpend(client, userId, sourceEventId, oldCurrency, oldAmount);
  await bumpPersonalPulseAfterExpense(client, userId, sourceEventId, newCurrency, newAmount);
}

async function reversePersonalPulseSpend(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  currencyCode: string,
  amountStr: string
): Promise<void> {
  const existing = await client.query<{ widget_payload: Record<string, unknown> | null }>(
    `SELECT widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`,
    [userId]
  );
  if (!existing.rows[0]) return;
  const payload = { ...(existing.rows[0].widget_payload ?? {}) };
  const spendByCurrency = { ...((payload.spendByCurrency as Record<string, string>) ?? {}) };
  spendByCurrency[currencyCode] = new Decimal(spendByCurrency[currencyCode] ?? '0').minus(amountStr).toFixed(4);
  payload.spendByCurrency = spendByCurrency;
  await client.query(
    `UPDATE projection.personal_pulse SET widget_payload = $2::jsonb, source_event_id = $3,
       projection_version = projection_version + 1, updated_at = now() WHERE user_id = $1`,
    [userId, JSON.stringify(payload), sourceEventId]
  );
}
