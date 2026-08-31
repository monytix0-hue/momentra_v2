import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

export const patchGroupMomentBudgetSchema = z
  .object({
    budgetAmount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    budgetCurrencyCode: z.string().length(3).regex(/^[A-Z]{3}$/),
  })
  .strict();

export type PatchGroupMomentBudgetInput = z.infer<typeof patchGroupMomentBudgetSchema>;

export interface GroupBudgetResult {
  momentId: string;
  budgetAmount: string;
  budgetCurrencyCode: string;
  budgetId: string;
}

async function assertActiveGroupMember(
  client: PoolClient,
  momentId: string,
  userId: string
): Promise<void> {
  const row = await client.query(
    `SELECT 1 FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [momentId, userId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active member of this group moment.', 403);
  }
}

export async function seedGroupBudget(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  budgetAmount: string,
  budgetCurrencyCode: string
): Promise<string> {
  const existing = await client.query<{ budget_id: string }>(
    `SELECT budget_id FROM finance.budget
     WHERE scope_type = 'MOMENT' AND moment_id = $1 AND currency_code = $2 AND status = 'ACTIVE'
     LIMIT 1`,
    [momentId, budgetCurrencyCode]
  );
  if (existing.rows[0]) {
    await client.query(
      `UPDATE finance.budget SET amount = $3::numeric, updated_at = now(), version = version + 1
       WHERE budget_id = $1 AND moment_id = $2`,
      [existing.rows[0].budget_id, momentId, budgetAmount]
    );
    await upsertGroupFinanceSnapshotBudget(client, momentId, budgetCurrencyCode, budgetAmount);
    return existing.rows[0].budget_id;
  }

  const inserted = await client.query<{ budget_id: string }>(
    `INSERT INTO finance.budget (
       scope_type, scope_id, moment_id, name, currency_code, amount, status, version
     ) VALUES ('MOMENT', $1, $1, 'Group budget', $2, $3::numeric, 'ACTIVE', 1)
     RETURNING budget_id`,
    [momentId, budgetCurrencyCode, budgetAmount]
  );
  const budgetId = inserted.rows[0]!.budget_id;
  await upsertGroupFinanceSnapshotBudget(client, momentId, budgetCurrencyCode, budgetAmount);

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'GroupBudgetSeeded',
    domainCode: 'GROUP',
    aggregateType: 'BUDGET',
    aggregateId: budgetId,
    payload: { momentId, budgetAmount, budgetCurrencyCode },
  });
  await insertAudit(client, ctx, 'GROUP_BUDGET_SEED', 'BUDGET', budgetId, domainEventId, {
    momentId,
    budgetAmount,
    budgetCurrencyCode,
  });

  return budgetId;
}

async function upsertGroupFinanceSnapshotBudget(
  client: PoolClient,
  momentId: string,
  currencyCode: string,
  budgetAmount: string
): Promise<void> {
  await client.query(
    `INSERT INTO projection.group_finance_snapshot (
       moment_id, currency_code, budget_total, projection_version
     ) VALUES ($1, $2, $3::numeric, 1)
     ON CONFLICT (moment_id, currency_code) DO UPDATE SET
       budget_total = EXCLUDED.budget_total,
       projection_version = projection.group_finance_snapshot.projection_version + 1,
       updated_at = now()`,
    [momentId, currencyCode, budgetAmount]
  );
}

export async function patchGroupMomentBudget(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: PatchGroupMomentBudgetInput
): Promise<GroupBudgetResult> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'MOMENT_UPDATE',
    resourceType: 'MOMENT',
    momentId,
  });
  await assertActiveGroupMember(client, momentId, ctx.userId);

  const moment = await client.query<{ domain_code: string }>(
    `SELECT domain_code FROM core.moment WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  );
  if (!moment.rows[0] || moment.rows[0].domain_code !== 'GROUP') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Budget patch is only valid for GROUP moments.', 400);
  }

  const budgetId = await seedGroupBudget(
    client,
    ctx,
    momentId,
    body.budgetAmount,
    body.budgetCurrencyCode
  );

  return {
    momentId,
    budgetAmount: body.budgetAmount,
    budgetCurrencyCode: body.budgetCurrencyCode,
    budgetId,
  };
}
