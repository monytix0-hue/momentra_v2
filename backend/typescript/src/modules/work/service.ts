import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

export const createGoalSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    targetAt: z.string().datetime().optional(),
    expectedVersion: z.number().int().positive().optional(),
  })
  .strict();

export const createMilestoneSchema = z
  .object({
    /** Optional — when omitted, a default Team Ops goal is ensured for the moment. */
    goalId: z.string().uuid().optional(),
    title: z.string().min(1).max(500),
    targetAt: z.string().datetime().optional(),
    status: z.enum(['PLANNED', 'ACTIVE', 'BLOCKED', 'COMPLETED', 'CANCELLED', 'ARCHIVED']).optional(),
    expectedVersion: z.number().int().positive().optional(),
  })
  .strict();

export const createTaskSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    goalId: z.string().uuid().optional(),
    milestoneId: z.string().uuid().optional(),
    dueAt: z.string().datetime().optional(),
    expectedVersion: z.number().int().positive().optional(),
  })
  .strict();

async function momentDomain(
  client: PoolClient,
  momentId: string
): Promise<'PERSONAL' | 'GROUP' | 'BUSINESS'> {
  const r = await client.query<{ domain_code: string }>(
    `SELECT domain_code FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  return (r.rows[0]?.domain_code ?? 'PERSONAL') as 'PERSONAL' | 'GROUP' | 'BUSINESS';
}

export async function createGoal(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createGoalSchema>
): Promise<{ goalId: string; momentId: string; title: string; version: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'GOAL_CREATE', resourceType: 'GOAL', momentId });
  const domainCode = await momentDomain(client, momentId);
  const inserted = await client.query<{ goal_id: string; version: string }>(
    `INSERT INTO work.goal (moment_id, domain_code, title, description, owner_user_id, target_date, status, version)
     VALUES ($1, $2, $3, $4, $5, $6::date, 'ACTIVE', 1)
     RETURNING goal_id, version`,
    [
      momentId,
      domainCode,
      body.title,
      body.description ?? null,
      ctx.userId,
      body.targetAt ? body.targetAt.slice(0, 10) : null,
    ]
  );
  const goalId = inserted.rows[0]!.goal_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'GoalCreated',
    domainCode,
    aggregateType: 'GOAL',
    aggregateId: goalId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { goalId, momentId, title: body.title },
  });
  const result = {
    goalId,
    momentId,
    title: body.title,
    version: parseInt(inserted.rows[0]!.version, 10),
  };
  await insertAudit(client, ctx, 'GOAL_CREATE', 'GOAL', goalId, domainEventId, result);
  return result;
}

async function ensureDefaultGoal(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<string> {
  const existing = await client.query<{ goal_id: string }>(
    `SELECT goal_id FROM work.goal
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY created_at ASC
     LIMIT 1`,
    [momentId]
  );
  if (existing.rows[0]) return existing.rows[0].goal_id;
  const created = await createGoal(client, ctx, momentId, {
    title: 'Team milestones',
  });
  return created.goalId;
}

export async function createMilestone(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createMilestoneSchema>
): Promise<{ milestoneId: string; goalId: string; title: string; version: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MILESTONE_CREATE', resourceType: 'MILESTONE', momentId });
  const goalId = body.goalId ?? (await ensureDefaultGoal(client, ctx, momentId));
  const status = body.status ?? 'PLANNED';
  const inserted = await client.query<{ milestone_id: string; version: string }>(
    `INSERT INTO work.milestone (goal_id, moment_id, title, target_date, status, completed_at, version)
     VALUES ($1, $2, $3, $4::date, $5, CASE WHEN $5 = 'COMPLETED' THEN now() ELSE NULL END, 1)
     RETURNING milestone_id, version`,
    [goalId, momentId, body.title, body.targetAt ? body.targetAt.slice(0, 10) : null, status]
  );
  const milestoneId = inserted.rows[0]!.milestone_id;
  const domainCode = await momentDomain(client, momentId);
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MilestoneCreated',
    domainCode,
    aggregateType: 'MILESTONE',
    aggregateId: milestoneId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { milestoneId, goalId, title: body.title, status },
  });
  const result = {
    milestoneId,
    goalId,
    title: body.title,
    version: parseInt(inserted.rows[0]!.version, 10),
  };
  await insertAudit(client, ctx, 'MILESTONE_CREATE', 'MILESTONE', milestoneId, domainEventId, result);

  const domain = domainCode;
  if (domain === 'BUSINESS') {
    const bmc = await client.query<{ company_id: string; business_family: string }>(
      `SELECT company_id, business_family FROM business.business_moment_context WHERE moment_id = $1`,
      [momentId]
    );
    if (bmc.rows[0]) {
      const { refreshBusinessProjectionsAfterWrite } = await import('../business/business-projection');
      await refreshBusinessProjectionsAfterWrite(client, bmc.rows[0].company_id, momentId, bmc.rows[0].business_family);
    }
  }

  return result;
}

export async function createTask(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createTaskSchema>
): Promise<{ taskId: string; momentId: string; title: string; version: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'TASK_CREATE', resourceType: 'TASK', momentId });
  const domainCode = await momentDomain(client, momentId);
  const inserted = await client.query<{ task_id: string; version: string }>(
    `INSERT INTO work.task (
       moment_id, domain_code, goal_id, milestone_id, title, description,
       due_at, status, created_by_user_id, version
     ) VALUES ($1, $2, $3, $4, $5, $6, $7::timestamptz, 'OPEN', $8, 1)
     RETURNING task_id, version`,
    [
      momentId,
      domainCode,
      body.goalId ?? null,
      body.milestoneId ?? null,
      body.title,
      body.description ?? null,
      body.dueAt ?? null,
      ctx.userId,
    ]
  );
  const taskId = inserted.rows[0]!.task_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'TaskCreated',
    domainCode,
    aggregateType: 'TASK',
    aggregateId: taskId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { taskId, momentId, title: body.title },
  });
  const result = {
    taskId,
    momentId,
    title: body.title,
    version: parseInt(inserted.rows[0]!.version, 10),
  };
  await insertAudit(client, ctx, 'TASK_CREATE', 'TASK', taskId, domainEventId, result);
  return result;
}
