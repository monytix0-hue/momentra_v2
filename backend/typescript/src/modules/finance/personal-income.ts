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
import { parseMoney } from './service';

export const createPersonalIncomeSchema = z
  .object({
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    currencyCode: z.string().length(3).toUpperCase(),
    description: z.string().max(500).optional(),
    merchantName: z.string().max(500).optional(),
    categoryCode: z.string().max(100).optional(),
    financialAccountId: z.string().uuid().optional(),
    paymentMethodCode: paymentMethodCodeSchema.optional(),
    effectiveAt: z.string().datetime().optional(),
  })
  .strict();

export const updatePersonalIncomeSchema = z
  .object({
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).toUpperCase().optional(),
    description: z.string().max(500).nullable().optional(),
    merchantName: z.string().max(500).nullable().optional(),
    categoryCode: z.string().max(100).nullable().optional(),
    financialAccountId: z.string().uuid().nullable().optional(),
    paymentMethodCode: paymentMethodCodeSchema.nullable().optional(),
    effectiveAt: z.string().datetime().optional(),
  })
  .strict()
  .refine(
    (b) => Object.values(b).some((v) => v !== undefined),
    { message: 'At least one field is required.' }
  );

export interface PersonalIncomeResult {
  incomeId: string;
  momentId: string;
  amount: string;
  currencyCode: string;
  status: string;
  version: number;
}

export interface PersonalIncomeDetail extends PersonalIncomeResult {
  description: string | null;
  merchantName: string | null;
  categoryCode: string | null;
  financialAccountId: string;
  paymentMethodCode: string | null;
  effectiveAt: string;
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

export async function createPersonalIncome(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createPersonalIncomeSchema>
): Promise<PersonalIncomeResult> {
  const amount = parseMoney(body.amount);
  await assertGovernanceAllowed(client, ctx, { actionCode: 'EXPENSE_CREATE', resourceType: 'EXPENSE', momentId });
  await assertPersonalMoment(client, ctx, momentId);

  const accountId = await resolveUserAccount(client, ctx, body.currencyCode, body.financialAccountId ?? null);
  const accountType = await getAccountType(client, accountId);
  const paymentMethod =
    body.paymentMethodCode ?? (accountType ? derivePaymentMethodFromAccountType(accountType) : 'OTHER');

  const inserted = await client.query<{ financial_movement_id: string; version: string }>(
    `INSERT INTO finance.financial_movement (
       financial_account_id, movement_type, direction, amount, currency_code, effective_at, status, version,
       source_type, source_id
     ) VALUES ($1, 'REVENUE', 'CREDIT', $2, $3, COALESCE($4::timestamptz, now()), 'POSTED', 1, 'PERSONAL_INCOME', $5::uuid)
     RETURNING financial_movement_id, version`,
    [accountId, amount.toFixed(4), body.currencyCode, body.effectiveAt ?? null, momentId]
  );
  const incomeId = inserted.rows[0]!.financial_movement_id;

  await client.query(
    `INSERT INTO finance.financial_movement_link (financial_movement_id, resource_type, resource_id, relation_type)
     VALUES ($1, 'MOMENT', $2, 'SOURCE_OF')`,
    [incomeId, momentId]
  );

  const title = body.description ?? body.merchantName ?? 'Income';
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'IncomeRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'FINANCIAL_MOVEMENT',
    aggregateId: incomeId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      incomeId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      categoryCode: body.categoryCode ?? null,
      paymentMethodCode: paymentMethod,
    },
  });

  await insertAudit(client, ctx, 'INCOME_CREATE', 'FINANCIAL_MOVEMENT', incomeId, domainEventId, {
    incomeId,
    momentId,
    amount: amount.toFixed(4),
  });

  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'PERSONAL', 'MOMENT', $3, 'INCOME_RECORDED', $4, COALESCE($5::timestamptz, now()), $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      title,
      body.effectiveAt ?? null,
      JSON.stringify({
        incomeId,
        amount: amount.toFixed(4),
        currencyCode: body.currencyCode,
        categoryCode: body.categoryCode ?? null,
        paymentMethodCode: paymentMethod,
        status: 'POSTED',
      }),
    ]
  );

  await bumpPersonalPulseIncome(client, ctx.userId, domainEventId, body.currencyCode, amount.toFixed(4));

  return {
    incomeId,
    momentId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
    version: parseInt(inserted.rows[0]!.version, 10),
  };
}

export async function getPersonalIncome(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  incomeId: string
): Promise<PersonalIncomeDetail> {
  const row = await client.query<{
    financial_movement_id: string;
    amount: string;
    currency_code: string;
    effective_at: Date;
    status: string;
    version: string;
    financial_account_id: string;
  }>(
    `SELECT fm.financial_movement_id, fm.amount::text AS amount, fm.currency_code, fm.effective_at,
            fm.status, fm.version::text AS version, fm.financial_account_id
     FROM finance.financial_movement fm
     JOIN finance.financial_movement_link fml ON fml.financial_movement_id = fm.financial_movement_id
     WHERE fm.financial_movement_id = $1
       AND fm.movement_type = 'REVENUE' AND fm.direction = 'CREDIT'
       AND fml.resource_type = 'MOMENT' AND fml.resource_id = $2::uuid
       AND EXISTS (
         SELECT 1 FROM finance.financial_account fa
         WHERE fa.financial_account_id = fm.financial_account_id AND fa.owner_user_id = $3
       )`,
    [incomeId, momentId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Income not found.', 404);
  }
  const r = row.rows[0];
  return {
    incomeId: r.financial_movement_id,
    momentId,
    amount: r.amount,
    currencyCode: r.currency_code,
    status: r.status,
    version: parseInt(r.version, 10),
    description: null,
    merchantName: null,
    categoryCode: null,
    financialAccountId: r.financial_account_id,
    paymentMethodCode: null,
    effectiveAt: r.effective_at.toISOString(),
  };
}

export async function updatePersonalIncome(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  incomeId: string,
  body: z.infer<typeof updatePersonalIncomeSchema>
): Promise<PersonalIncomeResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'EXPENSE_CREATE', resourceType: 'EXPENSE', momentId });
  const existing = await getPersonalIncome(client, ctx, momentId, incomeId);
  if (existing.status !== 'POSTED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only posted income can be updated.', 400);
  }

  const nextCurrency = body.currencyCode ?? existing.currencyCode;
  const nextAmount = body.amount != null ? parseMoney(body.amount).toFixed(4) : existing.amount;
  let nextAccountId = existing.financialAccountId;
  if (body.financialAccountId !== undefined) {
    nextAccountId = body.financialAccountId
      ? await resolveUserAccount(client, ctx, nextCurrency, body.financialAccountId)
      : await resolveUserAccount(client, ctx, nextCurrency, null);
  }
  const nextEffectiveAt = body.effectiveAt ?? existing.effectiveAt;

  const updated = await client.query<{ version: string }>(
    `UPDATE finance.financial_movement SET
       amount = $2,
       currency_code = $3,
       financial_account_id = $4,
       effective_at = $5::timestamptz,
       version = version + 1,
       updated_at = now()
     WHERE financial_movement_id = $1
     RETURNING version`,
    [incomeId, nextAmount, nextCurrency, nextAccountId, nextEffectiveAt]
  );

  const title = body.description ?? body.merchantName ?? existing.description ?? 'Income';
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'IncomeUpdated',
    domainCode: 'PERSONAL',
    aggregateType: 'FINANCIAL_MOVEMENT',
    aggregateId: incomeId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { incomeId, momentId, amount: nextAmount },
  });
  await insertAudit(client, ctx, 'INCOME_UPDATE', 'FINANCIAL_MOVEMENT', incomeId, domainEventId, { incomeId });

  await client.query(
    `UPDATE projection.recent_activity SET
       title = $3,
       occurred_at = $4::timestamptz,
       activity_payload = COALESCE(activity_payload, '{}'::jsonb) || $5::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2 AND activity_payload->>'incomeId' = $6`,
    [
      ctx.userId,
      momentId,
      title,
      nextEffectiveAt,
      JSON.stringify({ incomeId, amount: nextAmount, currencyCode: nextCurrency, status: 'POSTED' }),
      incomeId,
    ]
  );

  if (body.amount != null) {
    await adjustPersonalPulseIncome(
      client,
      ctx.userId,
      domainEventId,
      existing.currencyCode,
      existing.amount,
      nextCurrency,
      nextAmount
    );
  }

  return {
    incomeId,
    momentId,
    amount: nextAmount,
    currencyCode: nextCurrency,
    status: existing.status,
    version: parseInt(updated.rows[0]!.version, 10),
  };
}

export async function voidPersonalIncome(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  incomeId: string
): Promise<PersonalIncomeResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'EXPENSE_CREATE', resourceType: 'EXPENSE', momentId });
  const existing = await getPersonalIncome(client, ctx, momentId, incomeId);
  if (existing.status === 'VOIDED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Income already voided.', 400);
  }

  await client.query(
    `UPDATE finance.financial_movement SET status = 'VOIDED', updated_at = now(), version = version + 1
     WHERE financial_movement_id = $1`,
    [incomeId]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'IncomeVoided',
    domainCode: 'PERSONAL',
    aggregateType: 'FINANCIAL_MOVEMENT',
    aggregateId: incomeId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { incomeId, momentId },
  });
  await insertAudit(client, ctx, 'INCOME_VOID', 'FINANCIAL_MOVEMENT', incomeId, domainEventId, { incomeId });

  await client.query(
    `UPDATE projection.recent_activity SET
       activity_payload = COALESCE(activity_payload, '{}'::jsonb) || '{"status":"VOIDED"}'::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2 AND activity_payload->>'incomeId' = $3`,
    [ctx.userId, momentId, incomeId]
  );

  await reversePersonalPulseIncome(client, ctx.userId, domainEventId, existing.currencyCode, existing.amount);

  return { ...existing, status: 'VOIDED', version: existing.version + 1 };
}

async function bumpPersonalPulseIncome(
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
  const incomeByCurrency = { ...((payload.incomeByCurrency as Record<string, string>) ?? {}) };
  incomeByCurrency[currencyCode] = new Decimal(incomeByCurrency[currencyCode] ?? '0').plus(amountStr).toFixed(4);
  payload.incomeByCurrency = incomeByCurrency;

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET widget_payload = $2::jsonb, source_event_id = $3,
         projection_version = projection_version + 1, updated_at = now() WHERE user_id = $1`,
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

async function adjustPersonalPulseIncome(
  client: PoolClient,
  userId: string,
  sourceEventId: string,
  oldCurrency: string,
  oldAmount: string,
  newCurrency: string,
  newAmount: string
): Promise<void> {
  await reversePersonalPulseIncome(client, userId, sourceEventId, oldCurrency, oldAmount);
  await bumpPersonalPulseIncome(client, userId, sourceEventId, newCurrency, newAmount);
}

async function reversePersonalPulseIncome(
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
  const incomeByCurrency = { ...((payload.incomeByCurrency as Record<string, string>) ?? {}) };
  incomeByCurrency[currencyCode] = new Decimal(incomeByCurrency[currencyCode] ?? '0').minus(amountStr).toFixed(4);
  payload.incomeByCurrency = incomeByCurrency;
  await client.query(
    `UPDATE projection.personal_pulse SET widget_payload = $2::jsonb, source_event_id = $3,
       projection_version = projection_version + 1, updated_at = now() WHERE user_id = $1`,
    [userId, JSON.stringify(payload), sourceEventId]
  );
}
