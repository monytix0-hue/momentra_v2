/**
 * B2-G: Business memory create — company-scoped auth + projection refresh.
 */
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { recordCommandSideEffects } from '../../platform/events/outbox';
import { createMemory } from '../collaboration/service';
import { assertCompanyMomentAccess } from './membership';
import { refreshBusinessMemoryProjection, refreshBusinessProjectionsAfterWrite } from './business-projection';

export const createBusinessMemorySchema = z
  .object({
    title: z.string().min(1).max(500),
    body: z.string().max(5000).optional(),
    capturedAt: z.string().datetime().optional(),
  })
  .strict();

export async function createBusinessMemory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBusinessMemorySchema>
): Promise<{ memoryId: string; momentId: string; companyId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const result = await createMemory(client, ctx, momentId, {
    title: body.title,
    capturedAt: body.capturedAt,
  });

  if (body.body) {
    await client.query(`UPDATE memory.memory SET summary = $2 WHERE memory_id = $1`, [
      result.memoryId,
      body.body,
    ]);
  }

  await recordCommandSideEffects(client, ctx, {
    eventName: 'BusinessMemoryCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'MEMORY',
    aggregateId: result.memoryId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { memoryId: result.memoryId, momentId, title: body.title },
    auditActionCode: 'MEMORY_CREATE',
    auditResourceType: 'MEMORY',
    auditResourceId: result.memoryId,
    afterSnapshot: result,
    activity: {
      domainCode: 'BUSINESS',
      momentId,
      activityCode: 'BUSINESS_MEMORY_CREATED',
      title: body.title,
      payload: { memoryId: result.memoryId },
    },
  });

  await refreshBusinessMemoryProjection(client, scope.companyId, momentId);
  await refreshBusinessProjectionsAfterWrite(
    client,
    scope.companyId,
    momentId,
    scope.businessFamily
  );

  return { ...result, companyId: scope.companyId };
}
