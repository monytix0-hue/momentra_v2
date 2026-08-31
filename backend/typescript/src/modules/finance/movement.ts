import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';
import { parseMoney } from './service';
import { resolveUserAccount } from './financial-account';

export const createMovementSchema = z
  .object({
    movementType: z.enum(['TRANSFER', 'SAVINGS_DEPOSIT']),
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    currencyCode: z.string().length(3).toUpperCase(),
    accountId: z.string().uuid().optional(),
    goalId: z.string().uuid().optional(),
    description: z.string().max(2000).optional(),
    effectiveAt: z.string().datetime().optional(),
  })
  .strict();

export async function recordMovement(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createMovementSchema>
): Promise<{ movementId: string; momentId: string; amount: string; movementType: string }> {
  const amount = parseMoney(body.amount);
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOVEMENT_RECORD', resourceType: 'MOVEMENT', momentId });

  const accountId = await resolveUserAccount(client, ctx, body.currencyCode, body.accountId);
  const inserted = await client.query<{ financial_movement_id: string }>(
    `INSERT INTO finance.financial_movement (
       financial_account_id, movement_type, direction, amount, currency_code, effective_at, status, version
     ) VALUES ($1, 'TRANSFER', 'CREDIT', $2, $3, COALESCE($4::timestamptz, now()), 'POSTED', 1)
     RETURNING financial_movement_id`,
    [accountId, amount.toFixed(4), body.currencyCode, body.effectiveAt ?? null]
  );
  const movementId = inserted.rows[0]!.financial_movement_id;

  if (body.goalId) {
    await client.query(
      `INSERT INTO finance.financial_movement_link (
         financial_movement_id, resource_type, resource_id, relation_type
       ) VALUES ($1, 'GOAL', $2, 'SOURCE_OF')`,
      [movementId, body.goalId]
    );
  }

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MovementRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'FINANCIAL_MOVEMENT',
    aggregateId: movementId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      movementId,
      momentId,
      amount: amount.toFixed(4),
      movementType: body.movementType,
      goalId: body.goalId,
    },
  });

  return {
    movementId,
    momentId,
    amount: amount.toFixed(4),
    movementType: body.movementType,
  };
}
