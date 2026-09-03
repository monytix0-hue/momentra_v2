import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertFailClosedPolicies, assertGovernanceAllowed } from '../governance/resolver';
import { recordCommandSideEffects } from '../../platform/events/outbox';
import { z } from 'zod';
import Decimal from 'decimal.js';
import { parseMoney } from './service';
import { assertGroupMember, assertParticipantsOnMoment } from '../collaboration/group-membership';

const moneyString = z.string().regex(/^\d+(\.\d{1,4})?$/);
const percentString = z.string().regex(/^\d+(\.\d{1,6})?$/);

export const createGroupExpenseSchema = z
  .object({
    amount: moneyString,
    currencyCode: z.string().length(3).toUpperCase(),
    description: z.string().max(500).optional(),
    paidByParticipantId: z.string().uuid(),
    splitStrategy: z.enum(['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'POOLED']),
    splitInputs: z
      .array(
        z
          .object({
            participantId: z.string().uuid(),
            amount: moneyString.optional(),
            percent: percentString.optional(),
            shares: z.number().positive().optional(),
          })
          .strict()
      )
      .default([]),
  })
  .strict()
  .superRefine((val, ctx) => {
    if (val.splitStrategy !== 'POOLED' && val.splitInputs.length < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'splitInputs requires at least one participant for non-POOLED splits.',
        path: ['splitInputs'],
      });
    }
  });

export type CreateGroupExpenseInput = z.infer<typeof createGroupExpenseSchema>;

export const createSettlementSchema = z
  .object({
    payerParticipantId: z.string().uuid(),
    payeeParticipantId: z.string().uuid(),
    amount: moneyString,
    currencyCode: z.string().length(3).toUpperCase(),
    obligationIds: z.array(z.string().uuid()).optional(),
  })
  .strict();

export type CreateSettlementInput = z.infer<typeof createSettlementSchema>;

export interface GroupExpenseResult {
  expenseId: string;
  momentId: string;
  amount: string;
  currencyCode: string;
  status: string;
  version: number;
  paidByParticipantId: string;
  splitStrategy: string;
  shares: Array<{
    expenseShareId: string;
    participantId: string;
    shareAmount: string;
    sharePercent: string | null;
  }>;
  obligations: Array<{
    obligationId: string;
    participantId: string;
    originalAmount: string;
  }>;
}

export interface ComputedShare {
  participantId: string;
  shareAmount: Decimal;
  sharePercent: Decimal | null;
}

function rejectNegativeMoney(label: string, value: Decimal): void {
  if (value.lt(0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `${label} must not be negative.`, 400);
  }
  if (value.eq(0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `${label} must be greater than zero.`, 400);
  }
}

/** Distribute remainder (amount - sum of floored shares) to first participant by id. */
function applyRemainderToFirst(shares: ComputedShare[], total: Decimal): ComputedShare[] {
  const sorted = [...shares].sort((a, b) => a.participantId.localeCompare(b.participantId));
  const allocated = sorted.reduce((acc, s) => acc.plus(s.shareAmount), new Decimal(0));
  const remainder = total.minus(allocated);
  if (remainder.eq(0)) return sorted;
  sorted[0] = {
    ...sorted[0],
    shareAmount: sorted[0].shareAmount.plus(remainder),
  };
  return sorted;
}

/** Largest-remainder for percentage; ties broken by participantId ascending. */
function applyLargestRemainder(
  provisional: Array<{ participantId: string; exact: Decimal; floored: Decimal; fraction: Decimal; sharePercent: Decimal }>,
  total: Decimal
): ComputedShare[] {
  const flooredSum = provisional.reduce((acc, p) => acc.plus(p.floored), new Decimal(0));
  let unitsLeft = total.minus(flooredSum).times(10000).toDecimalPlaces(0, Decimal.ROUND_HALF_UP).toNumber();
  const ranked = [...provisional].sort((a, b) => {
    const frac = b.fraction.cmp(a.fraction);
    if (frac !== 0) return frac;
    return a.participantId.localeCompare(b.participantId);
  });
  const bump = new Set<string>();
  for (const row of ranked) {
    if (unitsLeft <= 0) break;
    bump.add(row.participantId);
    unitsLeft -= 1;
  }
  return provisional
    .map((p) => ({
      participantId: p.participantId,
      shareAmount: bump.has(p.participantId) ? p.floored.plus('0.0001') : p.floored,
      sharePercent: p.sharePercent,
    }))
    .sort((a, b) => a.participantId.localeCompare(b.participantId));
}

export function computeGroupShares(
  strategy: CreateGroupExpenseInput['splitStrategy'],
  amount: Decimal,
  splitInputs: CreateGroupExpenseInput['splitInputs']
): ComputedShare[] {
  if (strategy === 'POOLED') {
    // Household pool: record spend only — no per-member shares / IOUs.
    return [];
  }

  const ids = splitInputs.map((s) => s.participantId);
  if (new Set(ids).size !== ids.length) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Duplicate participantId in splitInputs.', 400);
  }

  if (strategy === 'EQUAL') {
    const n = splitInputs.length;
    const base = amount.div(n).toDecimalPlaces(4, Decimal.ROUND_DOWN);
    const shares: ComputedShare[] = splitInputs.map((s) => ({
      participantId: s.participantId,
      shareAmount: base,
      sharePercent: null,
    }));
    return applyRemainderToFirst(shares, amount);
  }

  if (strategy === 'PERCENTAGE') {
    let percentSum = new Decimal(0);
    const provisional = splitInputs.map((s) => {
      if (s.percent == null) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, 'percent is required for PERCENTAGE splits.', 400);
      }
      const percent = new Decimal(s.percent);
      rejectNegativeMoney('percent', percent);
      percentSum = percentSum.plus(percent);
      const exact = amount.times(percent).div(100);
      const floored = exact.toDecimalPlaces(4, Decimal.ROUND_DOWN);
      return {
        participantId: s.participantId,
        exact,
        floored,
        fraction: exact.minus(floored),
        sharePercent: percent,
      };
    });
    if (!percentSum.eq(100)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'PERCENTAGE split percents must sum to 100.', 400);
    }
    return applyLargestRemainder(provisional, amount);
  }

  if (strategy === 'EXACT') {
    let sum = new Decimal(0);
    const shares: ComputedShare[] = splitInputs.map((s) => {
      if (s.amount == null) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, 'amount is required for EXACT splits.', 400);
      }
      const shareAmount = new Decimal(s.amount);
      rejectNegativeMoney('share amount', shareAmount);
      sum = sum.plus(shareAmount);
      return {
        participantId: s.participantId,
        shareAmount: shareAmount.toDecimalPlaces(4, Decimal.ROUND_HALF_UP),
        sharePercent: null,
      };
    });
    if (!sum.eq(amount)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'EXACT split amounts must equal expense amount.', 400);
    }
    return shares.sort((a, b) => a.participantId.localeCompare(b.participantId));
  }

  // SHARES
  let weightSum = new Decimal(0);
  for (const s of splitInputs) {
    if (s.shares == null || s.shares <= 0) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'shares must be a positive number for SHARES splits.', 400);
    }
    weightSum = weightSum.plus(s.shares);
  }
  const provisional = splitInputs.map((s) => {
    const weight = new Decimal(s.shares!);
    const exact = amount.times(weight).div(weightSum);
    const floored = exact.toDecimalPlaces(4, Decimal.ROUND_DOWN);
    return {
      participantId: s.participantId,
      exact,
      floored,
      fraction: exact.minus(floored),
      sharePercent: weight.div(weightSum).times(100),
    };
  });
  return applyLargestRemainder(provisional, amount);
}

export async function createGroupExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: CreateGroupExpenseInput
): Promise<GroupExpenseResult> {
  const amount = parseMoney(body.amount);

  await assertFailClosedPolicies(client, 'EXPENSE_CREATE');
  await assertGroupMember(client, ctx, momentId);

  const participantIds = [
    body.paidByParticipantId,
    ...body.splitInputs.map((s) => s.participantId),
  ];
  await assertParticipantsOnMoment(client, momentId, participantIds);

  const computed = computeGroupShares(body.splitStrategy, amount, body.splitInputs);
  if (body.splitStrategy !== 'POOLED') {
    const shareSum = computed.reduce((acc, s) => acc.plus(s.shareAmount), new Decimal(0));
    if (!shareSum.eq(amount)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Computed shares must equal expense amount.', 400);
    }
    for (const s of computed) {
      if (s.shareAmount.lt(0)) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, 'Share amounts must not be negative.', 400);
      }
    }
  }

  const expenseInsert = await client.query<{ expense_id: string; version: string }>(
    `INSERT INTO finance.expense (
       moment_id, domain_code, created_by_user_id, description,
       amount, currency_code, effective_at, status, posted_at, version
     ) VALUES ($1, 'GROUP', $2, $3, $4, $5, now(), 'POSTED', now(), 1)
     RETURNING expense_id, version`,
    [momentId, ctx.userId, body.description ?? null, amount.toFixed(4), body.currencyCode]
  );
  const expenseId = expenseInsert.rows[0].expense_id;

  await client.query(
    `INSERT INTO finance.group_expense_context (
       expense_id, moment_id, paid_by_participant_id, split_strategy
     ) VALUES ($1, $2, $3, $4)`,
    [expenseId, momentId, body.paidByParticipantId, body.splitStrategy]
  );

  let shareRows: GroupExpenseResult['shares'] = [];
  let obligationRows: GroupExpenseResult['obligations'] = [];

  if (body.splitStrategy !== 'POOLED' && computed.length > 0) {
    const shareInsert = await client.query<{
      expense_share_id: string;
      participant_id: string;
      share_amount: string;
      share_percent: string | null;
    }>(
      `INSERT INTO finance.expense_share (
         expense_id, moment_id, participant_id, share_amount, share_percent, status
       )
       SELECT $1::uuid, $2::uuid, x.participant_id, x.share_amount::numeric,
              NULLIF(x.share_percent, '')::numeric, 'ALLOCATED'
       FROM UNNEST($3::uuid[], $4::text[], $5::text[]) AS x(participant_id, share_amount, share_percent)
       RETURNING expense_share_id, participant_id, share_amount::text, share_percent::text`,
      [
        expenseId,
        momentId,
        computed.map((s) => s.participantId),
        computed.map((s) => s.shareAmount.toFixed(4)),
        computed.map((s) => (s.sharePercent != null ? s.sharePercent.toFixed(6) : '')),
      ]
    );

    shareRows = shareInsert.rows.map((r) => ({
      expenseShareId: r.expense_share_id,
      participantId: r.participant_id,
      shareAmount: r.share_amount,
      sharePercent: r.share_percent,
    }));

    const obligationInsert = await client.query<{
      participant_obligation_id: string;
      participant_id: string;
      original_amount: string;
    }>(
      `INSERT INTO finance.participant_obligation (
         moment_id, participant_id, source_type, source_id, currency_code,
         original_amount, settled_amount, status, version
       )
       SELECT $1::uuid, es.participant_id, 'EXPENSE_SHARE', es.expense_share_id, $2,
              es.share_amount, 0, 'OPEN', 1
       FROM finance.expense_share es
       WHERE es.expense_id = $3::uuid
         AND es.participant_id <> $4::uuid
         AND es.share_amount > 0
       RETURNING participant_obligation_id, participant_id, original_amount::text`,
      [momentId, body.currencyCode, expenseId, body.paidByParticipantId]
    );
    obligationRows = obligationInsert.rows.map((r) => ({
      obligationId: r.participant_obligation_id,
      participantId: r.participant_id,
      originalAmount: r.original_amount,
    }));
  }

  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupExpenseRecorded',
    domainCode: 'GROUP',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      expenseId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      paidByParticipantId: body.paidByParticipantId,
      splitStrategy: body.splitStrategy,
      shareCount: shareRows.length,
    },
    auditActionCode: 'EXPENSE_CREATE',
    auditResourceType: 'EXPENSE',
    auditResourceId: expenseId,
    afterSnapshot: {
      expenseId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      status: 'POSTED',
      version: parseInt(expenseInsert.rows[0].version, 10),
      paidByParticipantId: body.paidByParticipantId,
      splitStrategy: body.splitStrategy,
      shares: shareRows,
      obligations: obligationRows,
    },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode:
        body.splitStrategy === 'POOLED' ? 'GROUP_POOLED_EXPENSE_RECORDED' : 'GROUP_EXPENSE_RECORDED',
      title:
        body.splitStrategy === 'POOLED'
          ? body.description ?? 'Household pooled spend'
          : body.description ?? 'Group expense',
      payload: {
        expenseId,
        amount: amount.toFixed(4),
        currencyCode: body.currencyCode,
        paidByParticipantId: body.paidByParticipantId,
        splitStrategy: body.splitStrategy,
      },
    },
  });

  const result: GroupExpenseResult = {
    expenseId,
    momentId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
    version: parseInt(expenseInsert.rows[0].version, 10),
    paidByParticipantId: body.paidByParticipantId,
    splitStrategy: body.splitStrategy,
    shares: shareRows,
    obligations: obligationRows,
  };

  await upsertGroupFinanceProjection(
    client,
    momentId,
    body.currencyCode,
    body.paidByParticipantId,
    amount,
    computed,
    domainEventId,
    body.splitStrategy === 'POOLED',
    1
  );

  return result;
}

export async function getGroupExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<GroupExpenseResult & { description: string | null }> {
  await assertGroupMember(client, ctx, momentId);

  const row = await client.query<{
    expense_id: string;
    moment_id: string;
    amount: string;
    currency_code: string;
    status: string;
    version: string;
    description: string | null;
    paid_by_participant_id: string;
    split_strategy: string;
  }>(
    `SELECT e.expense_id, e.moment_id, e.amount::text, e.currency_code, e.status, e.version::text,
            e.description, g.paid_by_participant_id, g.split_strategy
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g ON g.expense_id = e.expense_id
     WHERE e.expense_id = $1::uuid AND e.moment_id = $2::uuid AND e.domain_code = 'GROUP'`,
    [expenseId, momentId]
  );
  if (row.rows.length === 0) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Group expense not found.', 404);
  }
  const e = row.rows[0];

  const shares = await client.query<{
    expense_share_id: string;
    participant_id: string;
    share_amount: string;
    share_percent: string | null;
  }>(
    `SELECT expense_share_id, participant_id, share_amount::text, share_percent::text
     FROM finance.expense_share
     WHERE expense_id = $1::uuid AND status <> 'VOIDED'
     ORDER BY participant_id`,
    [expenseId]
  );

  const obligations = await client.query<{
    participant_obligation_id: string;
    participant_id: string;
    original_amount: string;
  }>(
    `SELECT participant_obligation_id, participant_id, original_amount::text
     FROM finance.participant_obligation
     WHERE source_type = 'EXPENSE_SHARE'
       AND source_id IN (SELECT expense_share_id FROM finance.expense_share WHERE expense_id = $1::uuid)
       AND status <> 'VOIDED'`,
    [expenseId]
  );

  return {
    expenseId: e.expense_id,
    momentId: e.moment_id,
    amount: e.amount,
    currencyCode: e.currency_code,
    status: e.status,
    version: parseInt(e.version, 10),
    description: e.description,
    paidByParticipantId: e.paid_by_participant_id,
    splitStrategy: e.split_strategy,
    shares: shares.rows.map((r) => ({
      expenseShareId: r.expense_share_id,
      participantId: r.participant_id,
      shareAmount: r.share_amount,
      sharePercent: r.share_percent,
    })),
    obligations: obligations.rows.map((r) => ({
      obligationId: r.participant_obligation_id,
      participantId: r.participant_id,
      originalAmount: r.original_amount,
    })),
  };
}

export const updateGroupExpenseSchema = createGroupExpenseSchema;

export type UpdateGroupExpenseInput = CreateGroupExpenseInput;

export async function updateGroupExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string,
  body: UpdateGroupExpenseInput
): Promise<GroupExpenseResult> {
  const amount = parseMoney(body.amount);

  await assertFailClosedPolicies(client, 'EXPENSE_CREATE');
  await assertGroupMember(client, ctx, momentId);

  const existing = await getGroupExpense(client, ctx, momentId, expenseId);
  if (existing.status === 'VOIDED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Cannot update a voided expense.', 400);
  }

  const settled = await client.query<{ c: string }>(
    `SELECT count(*)::text AS c
     FROM finance.participant_obligation po
     INNER JOIN finance.expense_share es ON es.expense_share_id = po.source_id
     WHERE es.expense_id = $1::uuid
       AND po.source_type = 'EXPENSE_SHARE'
       AND (po.settled_amount > 0 OR po.status NOT IN ('OPEN', 'VOIDED'))`,
    [expenseId]
  );
  if (parseInt(settled.rows[0]?.c ?? '0', 10) > 0) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Cannot edit expense after settlements have been applied.',
      409
    );
  }

  const participantIds = [
    body.paidByParticipantId,
    ...body.splitInputs.map((s) => s.participantId),
  ];
  await assertParticipantsOnMoment(client, momentId, participantIds);

  const computed = computeGroupShares(body.splitStrategy, amount, body.splitInputs);
  if (body.splitStrategy !== 'POOLED') {
    const shareSum = computed.reduce((acc, s) => acc.plus(s.shareAmount), new Decimal(0));
    if (!shareSum.eq(amount)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Computed shares must equal expense amount.', 400);
    }
  }

  // Reverse prior finance projection
  const oldAmount = new Decimal(existing.amount);
  const oldShares: ComputedShare[] = existing.shares.map((s) => ({
    participantId: s.participantId,
    shareAmount: new Decimal(s.shareAmount),
    sharePercent: s.sharePercent != null ? new Decimal(s.sharePercent) : null,
  }));
  await upsertGroupFinanceProjection(
    client,
    momentId,
    existing.currencyCode,
    existing.paidByParticipantId,
    oldAmount.neg(),
    oldShares.map((s) => ({ ...s, shareAmount: s.shareAmount.neg() })),
    ctx.correlationId,
    existing.splitStrategy === 'POOLED',
    0
  );

  await client.query(
    `DELETE FROM finance.participant_obligation
     WHERE source_type = 'EXPENSE_SHARE'
       AND source_id IN (SELECT expense_share_id FROM finance.expense_share WHERE expense_id = $1::uuid)`,
    [expenseId]
  );
  await client.query(`DELETE FROM finance.expense_share WHERE expense_id = $1::uuid`, [expenseId]);

  const updated = await client.query<{ version: string }>(
    `UPDATE finance.expense
     SET description = $3, amount = $4, currency_code = $5, version = version + 1, updated_at = now()
     WHERE expense_id = $1::uuid AND moment_id = $2::uuid
     RETURNING version::text`,
    [expenseId, momentId, body.description ?? null, amount.toFixed(4), body.currencyCode]
  );
  await client.query(
    `UPDATE finance.group_expense_context
     SET paid_by_participant_id = $2, split_strategy = $3
     WHERE expense_id = $1::uuid`,
    [expenseId, body.paidByParticipantId, body.splitStrategy]
  );

  let shareRows: GroupExpenseResult['shares'] = [];
  let obligationRows: GroupExpenseResult['obligations'] = [];

  if (body.splitStrategy !== 'POOLED' && computed.length > 0) {
    const shareInsert = await client.query<{
      expense_share_id: string;
      participant_id: string;
      share_amount: string;
      share_percent: string | null;
    }>(
      `INSERT INTO finance.expense_share (
         expense_id, moment_id, participant_id, share_amount, share_percent, status
       )
       SELECT $1::uuid, $2::uuid, x.participant_id, x.share_amount::numeric,
              NULLIF(x.share_percent, '')::numeric, 'ALLOCATED'
       FROM UNNEST($3::uuid[], $4::text[], $5::text[]) AS x(participant_id, share_amount, share_percent)
       RETURNING expense_share_id, participant_id, share_amount::text, share_percent::text`,
      [
        expenseId,
        momentId,
        computed.map((s) => s.participantId),
        computed.map((s) => s.shareAmount.toFixed(4)),
        computed.map((s) => (s.sharePercent != null ? s.sharePercent.toFixed(6) : '')),
      ]
    );
    shareRows = shareInsert.rows.map((r) => ({
      expenseShareId: r.expense_share_id,
      participantId: r.participant_id,
      shareAmount: r.share_amount,
      sharePercent: r.share_percent,
    }));

    const obligationInsert = await client.query<{
      participant_obligation_id: string;
      participant_id: string;
      original_amount: string;
    }>(
      `INSERT INTO finance.participant_obligation (
         moment_id, participant_id, source_type, source_id, currency_code,
         original_amount, settled_amount, status, version
       )
       SELECT $1::uuid, es.participant_id, 'EXPENSE_SHARE', es.expense_share_id, $2,
              es.share_amount, 0, 'OPEN', 1
       FROM finance.expense_share es
       WHERE es.expense_id = $3::uuid
         AND es.status = 'ALLOCATED'
         AND es.participant_id <> $4::uuid
         AND es.share_amount > 0
       RETURNING participant_obligation_id, participant_id, original_amount::text`,
      [momentId, body.currencyCode, expenseId, body.paidByParticipantId]
    );
    obligationRows = obligationInsert.rows.map((r) => ({
      obligationId: r.participant_obligation_id,
      participantId: r.participant_id,
      originalAmount: r.original_amount,
    }));
  }

  const version = parseInt(updated.rows[0].version, 10);
  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupExpenseUpdated',
    domainCode: 'GROUP',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      expenseId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      paidByParticipantId: body.paidByParticipantId,
      splitStrategy: body.splitStrategy,
    },
    auditActionCode: 'EXPENSE_UPDATE',
    auditResourceType: 'EXPENSE',
    auditResourceId: expenseId,
    afterSnapshot: {
      expenseId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      status: 'POSTED',
      version,
      paidByParticipantId: body.paidByParticipantId,
      splitStrategy: body.splitStrategy,
      shares: shareRows,
      obligations: obligationRows,
    },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode:
        body.splitStrategy === 'POOLED' ? 'GROUP_POOLED_EXPENSE_UPDATED' : 'GROUP_EXPENSE_UPDATED',
      title:
        body.splitStrategy === 'POOLED'
          ? body.description ?? 'Household pooled spend updated'
          : body.description ?? 'Group expense updated',
      payload: {
        expenseId,
        amount: amount.toFixed(4),
        currencyCode: body.currencyCode,
        paidByParticipantId: body.paidByParticipantId,
        splitStrategy: body.splitStrategy,
      },
    },
  });

  await upsertGroupFinanceProjection(
    client,
    momentId,
    body.currencyCode,
    body.paidByParticipantId,
    amount,
    computed,
    domainEventId,
    body.splitStrategy === 'POOLED',
    0
  );

  return {
    expenseId,
    momentId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
    version,
    paidByParticipantId: body.paidByParticipantId,
    splitStrategy: body.splitStrategy,
    shares: shareRows,
    obligations: obligationRows,
  };
}

export async function voidGroupExpense(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<GroupExpenseResult> {
  await assertFailClosedPolicies(client, 'EXPENSE_CREATE');
  await assertGroupMember(client, ctx, momentId);

  const existing = await getGroupExpense(client, ctx, momentId, expenseId);
  if (existing.status === 'VOIDED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Expense is already voided.', 400);
  }

  const settled = await client.query<{ c: string }>(
    `SELECT count(*)::text AS c
     FROM finance.participant_obligation po
     INNER JOIN finance.expense_share es ON es.expense_share_id = po.source_id
     WHERE es.expense_id = $1::uuid
       AND po.source_type = 'EXPENSE_SHARE'
       AND po.settled_amount > 0`,
    [expenseId]
  );
  if (parseInt(settled.rows[0]?.c ?? '0', 10) > 0) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Cannot void expense after settlements have been applied.',
      409
    );
  }

  const oldAmount = new Decimal(existing.amount);
  const oldShares: ComputedShare[] = existing.shares.map((s) => ({
    participantId: s.participantId,
    shareAmount: new Decimal(s.shareAmount),
    sharePercent: s.sharePercent != null ? new Decimal(s.sharePercent) : null,
  }));
  await upsertGroupFinanceProjection(
    client,
    momentId,
    existing.currencyCode,
    existing.paidByParticipantId,
    oldAmount.neg(),
    oldShares.map((s) => ({ ...s, shareAmount: s.shareAmount.neg() })),
    ctx.correlationId,
    existing.splitStrategy === 'POOLED',
    -1
  );

  await client.query(
    `UPDATE finance.participant_obligation SET status = 'VOIDED', updated_at = now()
     WHERE source_type = 'EXPENSE_SHARE'
       AND source_id IN (SELECT expense_share_id FROM finance.expense_share WHERE expense_id = $1::uuid)`,
    [expenseId]
  );
  await client.query(
    `UPDATE finance.expense_share SET status = 'VOIDED', updated_at = now() WHERE expense_id = $1::uuid`,
    [expenseId]
  );
  const voided = await client.query<{ version: string }>(
    `UPDATE finance.expense
     SET status = 'VOIDED', reversed_at = now(), version = version + 1, updated_at = now()
     WHERE expense_id = $1::uuid AND moment_id = $2::uuid
     RETURNING version::text`,
    [expenseId, momentId]
  );

  await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupExpenseVoided',
    domainCode: 'GROUP',
    aggregateType: 'EXPENSE',
    aggregateId: expenseId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { expenseId, momentId },
    auditActionCode: 'EXPENSE_VOID',
    auditResourceType: 'EXPENSE',
    auditResourceId: expenseId,
    afterSnapshot: { expenseId, status: 'VOIDED' },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_EXPENSE_VOIDED',
      title: existing.description ?? 'Group expense voided',
      payload: { expenseId, status: 'VOIDED' },
    },
  });

  return {
    ...existing,
    status: 'VOIDED',
    version: parseInt(voided.rows[0].version, 10),
    shares: [],
    obligations: [],
  };
}

async function upsertGroupFinanceProjection(
  client: PoolClient,
  momentId: string,
  currencyCode: string,
  paidByParticipantId: string,
  amount: Decimal,
  shares: ComputedShare[],
  sourceEventId: string,
  isPooled = false,
  expenseCountDelta = 1
): Promise<void> {
  const outstandingDelta = isPooled
    ? new Decimal(0)
    : shares
        .filter((s) => s.participantId !== paidByParticipantId)
        .reduce((acc, s) => acc.plus(s.shareAmount), new Decimal(0));

  // For reverse (negative amount), still mark pooled adjustment when isPooled.
  const applyPooledPayload = isPooled && !amount.eq(0);

  const payloadExtras = isPooled
    ? {
        expenseCount: expenseCountDelta,
        pooledExpenseTotal: amount.toFixed(4),
      }
    : { expenseCount: expenseCountDelta };

  await client.query(
    `WITH snap AS (
       INSERT INTO projection.group_finance_snapshot (
         moment_id, currency_code, expense_total, outstanding_total,
         snapshot_payload, source_event_id, projection_version
       ) VALUES ($1, $2, $3, $4, $5::jsonb, $6, 1)
       ON CONFLICT (moment_id, currency_code) DO UPDATE SET
         expense_total = projection.group_finance_snapshot.expense_total + EXCLUDED.expense_total,
         outstanding_total = projection.group_finance_snapshot.outstanding_total + EXCLUDED.outstanding_total,
         snapshot_payload = COALESCE(projection.group_finance_snapshot.snapshot_payload, '{}'::jsonb)
           || jsonb_build_object(
                'expenseCount',
                GREATEST(
                  0,
                  COALESCE((projection.group_finance_snapshot.snapshot_payload->>'expenseCount')::int, 0) + $14::int
                )
              )
           || CASE WHEN $13::boolean THEN jsonb_build_object(
                'pooledExpenseTotal',
                (
                  COALESCE((projection.group_finance_snapshot.snapshot_payload->>'pooledExpenseTotal')::numeric, 0)
                  + $3::numeric
                )::text
              ) ELSE '{}'::jsonb END,
         source_event_id = EXCLUDED.source_event_id,
         projection_version = projection.group_finance_snapshot.projection_version + 1,
         updated_at = now()
     )
     INSERT INTO projection.group_finance_position (
       moment_id, participant_id, currency_code,
       paid_total, allocated_total, payable_total, receivable_total, net_position,
       source_event_id, projection_version
     )
     SELECT $1::uuid, x.participant_id, $2,
            x.paid_delta::numeric, x.allocated_delta::numeric, x.payable_delta::numeric,
            x.receivable_delta::numeric, x.net_delta::numeric,
            $6::uuid, 1
     FROM UNNEST(
       $7::uuid[], $8::text[], $9::text[], $10::text[], $11::text[], $12::text[]
     ) AS x(participant_id, paid_delta, allocated_delta, payable_delta, receivable_delta, net_delta)
     ON CONFLICT (moment_id, participant_id, currency_code) DO UPDATE SET
       paid_total = projection.group_finance_position.paid_total + EXCLUDED.paid_total,
       allocated_total = projection.group_finance_position.allocated_total + EXCLUDED.allocated_total,
       payable_total = projection.group_finance_position.payable_total + EXCLUDED.payable_total,
       receivable_total = projection.group_finance_position.receivable_total + EXCLUDED.receivable_total,
       net_position = projection.group_finance_position.net_position + EXCLUDED.net_position,
       source_event_id = EXCLUDED.source_event_id,
       projection_version = projection.group_finance_position.projection_version + 1,
       updated_at = now()`,
    [
      momentId,
      currencyCode,
      amount.toFixed(4),
      outstandingDelta.toFixed(4),
      JSON.stringify(payloadExtras),
      sourceEventId,
      shares.map((s) => s.participantId),
      shares.map((s) => (s.participantId === paidByParticipantId ? amount : new Decimal(0)).toFixed(4)),
      shares.map((s) => s.shareAmount.toFixed(4)),
      shares.map((s) =>
        (s.participantId === paidByParticipantId ? new Decimal(0) : s.shareAmount).toFixed(4)
      ),
      shares.map((s) =>
        (s.participantId === paidByParticipantId
          ? amount.minus(s.shareAmount)
          : new Decimal(0)
        ).toFixed(4)
      ),
      shares.map((s) =>
        (s.participantId === paidByParticipantId
          ? amount.minus(
              shares
                .filter((x) => x.participantId !== paidByParticipantId)
                .reduce((acc, x) => acc.plus(x.shareAmount), new Decimal(0))
            )
          : s.shareAmount.neg()
        ).toFixed(4)
      ),
      applyPooledPayload,
      expenseCountDelta,
    ]
  );
}

/**
 * Settlement write path.
 * API_GAP until SETTLEMENT_RECORD is mapped on moment_type_capability —
 * router returns 501 when capability is absent. When mapped, this posts
 * settlement + allocations against the payer's OPEN obligations (debtor).
 */
export async function createSettlement(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: CreateSettlementInput
): Promise<{
  settlementId: string;
  momentId: string;
  amount: string;
  currencyCode: string;
  status: string;
  allocations: Array<{ obligationId: string; amount: string }>;
}> {
  const amount = parseMoney(body.amount);

  if (body.payerParticipantId === body.payeeParticipantId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Payer and payee must be different.', 400);
  }

  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'SETTLEMENT_RECORD',
    resourceType: 'SETTLEMENT',
    momentId,
  });
  await assertGroupMember(client, ctx, momentId);
  await assertParticipantsOnMoment(client, momentId, [body.payerParticipantId, body.payeeParticipantId]);

  const settlementInsert = await client.query<{ settlement_id: string }>(
    `INSERT INTO finance.settlement (
       moment_id, payer_participant_id, payee_participant_id,
       amount, currency_code, settled_at, status, version
     ) VALUES ($1, $2, $3, $4, $5, now(), 'POSTED', 1)
     RETURNING settlement_id`,
    [momentId, body.payerParticipantId, body.payeeParticipantId, amount.toFixed(4), body.currencyCode]
  );
  const settlementId = settlementInsert.rows[0].settlement_id;

  // Debtor (payer) OPEN obligations in this currency; optional filter by obligationIds.
  const obligations = await client.query<{
    participant_obligation_id: string;
    original_amount: string;
    settled_amount: string;
  }>(
    `SELECT participant_obligation_id, original_amount::text, settled_amount::text
     FROM finance.participant_obligation
     WHERE moment_id = $1
       AND participant_id = $2
       AND currency_code = $3
       AND status IN ('OPEN', 'PARTIALLY_SETTLED')
       AND ($4::uuid[] IS NULL OR participant_obligation_id = ANY($4::uuid[]))
     ORDER BY created_at ASC, participant_obligation_id ASC
     FOR UPDATE`,
    [momentId, body.payerParticipantId, body.currencyCode, body.obligationIds ?? null]
  );

  let remaining = amount;
  const allocations: Array<{ obligationId: string; amount: string }> = [];

  for (const obl of obligations.rows) {
    if (remaining.lte(0)) break;
    const open = new Decimal(obl.original_amount).minus(obl.settled_amount);
    if (open.lte(0)) continue;
    const apply = Decimal.min(remaining, open);
    await client.query(
      `INSERT INTO finance.settlement_allocation (
         settlement_id, moment_id, participant_obligation_id, amount
       ) VALUES ($1, $2, $3, $4)`,
      [settlementId, momentId, obl.participant_obligation_id, apply.toFixed(4)]
    );
    const newSettled = new Decimal(obl.settled_amount).plus(apply);
    const fullySettled = newSettled.eq(obl.original_amount);
    await client.query(
      `UPDATE finance.participant_obligation SET
         settled_amount = $2,
         status = $3,
         version = version + 1,
         updated_at = now()
       WHERE participant_obligation_id = $1`,
      [obl.participant_obligation_id, newSettled.toFixed(4), fullySettled ? 'SETTLED' : 'PARTIALLY_SETTLED']
    );
    allocations.push({ obligationId: obl.participant_obligation_id, amount: apply.toFixed(4) });
    remaining = remaining.minus(apply);
  }

  if (remaining.gt(0)) {
    throw new AppError(
      ErrorCode.SETTLEMENT_EXCEEDS_OUTSTANDING,
      'Settlement amount exceeds open obligations for payer.',
      400
    );
  }

  const result = {
    settlementId,
    momentId,
    amount: amount.toFixed(4),
    currencyCode: body.currencyCode,
    status: 'POSTED',
    allocations,
  };

  await recordCommandSideEffects(client, ctx, {
    eventName: 'SettlementRecorded',
    domainCode: 'GROUP',
    aggregateType: 'SETTLEMENT',
    aggregateId: settlementId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      settlementId,
      momentId,
      amount: amount.toFixed(4),
      currencyCode: body.currencyCode,
      payerParticipantId: body.payerParticipantId,
      payeeParticipantId: body.payeeParticipantId,
    },
    auditActionCode: 'SETTLEMENT_RECORD',
    auditResourceType: 'SETTLEMENT',
    auditResourceId: settlementId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_SETTLEMENT_RECORDED',
      title: 'Settlement recorded',
      payload: {
        settlementId,
        amount: amount.toFixed(4),
        currencyCode: body.currencyCode,
        payerParticipantId: body.payerParticipantId,
        payeeParticipantId: body.payeeParticipantId,
      },
    },
  });

  await client.query(
    `UPDATE projection.group_finance_snapshot SET
       outstanding_total = GREATEST(0, outstanding_total - $2),
       projection_version = projection_version + 1,
       updated_at = now()
     WHERE moment_id = $1 AND currency_code = $3`,
    [momentId, amount.toFixed(4), body.currencyCode]
  );

  return result;
}
