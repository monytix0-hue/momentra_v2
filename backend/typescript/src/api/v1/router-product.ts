/**
 * DEPRECATED / NON-RUNTIME (S2 G1).
 * Not mounted by app.ts. Live authority is api/v1/router.ts.
 * Do not re-export this router into the live app.
 *
 * Promoted to live router.ts (do not treat as gap):
 * - PATCH /moments/:momentId, POST archive/cancel
 * - POST /moments/:momentId/goals|milestones|tasks
 * - GET /moments/:momentId/activity
 * - POST /personal/setups/:systemCode/activate
 * - GET /personal/attention
 * - POST /ai/action-proposals/:actionProposalId/execute
 * - personal/life, personal/memory, relationship-activities, and prior S0/S1 Personal duplicates
 *
 * Remaining drafts here are stale duplicates or stubs until explicitly remounted.
 */
import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import { runCommand } from '../../platform/transaction/run-command';
import { AppError, ErrorCode, commandEnvelope, projectionEnvelope } from '../../platform/errors/errors';
import { authMiddleware, requireIdempotencyKey } from '../middleware/auth';
import { toProjectionHints } from '../../platform/projections/hints';
import { projectionCodesForDomain } from '../../modules/moment/projection-routing';
import { publishProjectionUpdated } from '../../realtime/sse';
import * as momentService from '../../modules/moment/service';
import * as workService from '../../modules/work/service';
import * as financeService from '../../modules/finance/service';
import * as collaborationService from '../../modules/collaboration/service';
import * as inviteService from '../../modules/collaboration/invite-service';
import * as businessService from '../../modules/business/service';
import * as projectionService from '../../modules/projection/service';
import * as mediaService from '../../modules/media/service';
import * as deviceService from '../../modules/device/service';
import * as personalService from '../../modules/personal/service';
import * as personalSetupService from '../../modules/personal/setup-service';
import * as businessSetupService from '../../modules/business/setup-service';
import * as movementService from '../../modules/finance/movement';
import { executeActionProposal } from '../../modules/ai/action-proposal.command';
import { telemetryRouter } from './telemetry-router';

export const v1Router = Router();
v1Router.use('/telemetry', telemetryRouter);
v1Router.use(authMiddleware);

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

function parseBody<T>(schema: z.ZodSchema<T>, body: unknown): T {
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, parsed.error.message, 400);
  }
  return parsed.data;
}

function param(value: string | string[]): string {
  return Array.isArray(value) ? value[0] : value;
}

function parseVersion(body: unknown): number {
  const v = (body as { expectedVersion?: number })?.expectedVersion;
  if (!v || v < 1) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'expectedVersion is required.', 400);
  }
  return v;
}

// --- Bootstrap ---
// Bootstrap only — do not load Pulse/Moments/Finance/projections here.
v1Router.get('/me', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => deviceService.getMe(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/**
 * Phase 3 transactional proof command: device registration.
 * Uses idempotency + governance + audit + domain event + outbox in one transaction.
 */
v1Router.post('/me/devices', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(deviceService.registerDeviceSchema, req.body);
    const result = await runCommand({
      operationCode: 'DEVICE_REGISTER',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'DEVICE',
      execute: async (client, b) => {
        const r = await deviceService.registerDevice(client, ctx, b as deviceService.RegisterDeviceInput);
        return { result: r, resourceId: r.userDeviceId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/me/devices/:deviceId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => deviceService.revokeDevice(client, ctx, param(req.params.deviceId)));
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Moments ---
v1Router.post('/moments', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(momentService.createMomentSchema, req.body);
    const result = await runCommand({
      operationCode: 'MOMENT_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MOMENT',
      execute: async (client, b) => {
        const r = await momentService.createMoment(client, ctx, b as momentService.CreateMomentInput);
        return { result: r, resourceId: r.momentId };
      },
    });
    const hints = projectionCodesForDomain(result.domainCode);
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints(hints),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => momentService.getMoment(client, ctx, param(req.params.momentId)));
    if (!data) {
      throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
    }
    res.json(commandEnvelope(data, ctx.correlationId, { resourceVersion: data.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(momentService.updateMomentSchema, req.body);
    const result = await runCommand({
      operationCode: 'MOMENT_UPDATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MOMENT',
      execute: async (client, b) => {
        const r = await momentService.updateMoment(client, ctx, param(req.params.momentId), b as momentService.UpdateMomentInput);
        return { result: r, resourceId: r.momentId };
      },
    });
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/archive', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const expectedVersion = parseVersion(req.body);
    const result = await withDb((client) =>
      momentService.archiveMoment(client, ctx, param(req.params.momentId), expectedVersion)
    );
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/cancel', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const expectedVersion = parseVersion(req.body);
    const result = await withDb((client) =>
      momentService.cancelMoment(client, ctx, param(req.params.momentId), expectedVersion)
    );
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const expectedVersion = parseVersion(req.body);
    const result = await withDb((client) =>
      momentService.deleteMoment(client, ctx, param(req.params.momentId), expectedVersion)
    );
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/expenses', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(financeService.createExpenseSchema, req.body);
    const result = await runCommand({
      operationCode: 'EXPENSE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'EXPENSE',
      execute: async (client, b) => {
        const r = await financeService.createExpense(client, ctx, param(req.params.momentId), b as z.infer<typeof financeService.createExpenseSchema>);
        return { result: r, resourceId: r.expenseId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/tasks', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(workService.createTaskSchema, req.body);
    const result = await runCommand({
      operationCode: 'TASK_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'TASK',
      execute: async (client, b) => {
        const r = await workService.createTask(client, ctx, param(req.params.momentId), b as z.infer<typeof workService.createTaskSchema>);
        return { result: r, resourceId: r.taskId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/goals', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(workService.createGoalSchema, req.body);
    const result = await runCommand({
      operationCode: 'GOAL_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'GOAL',
      execute: async (client, b) => {
        const r = await workService.createGoal(client, ctx, param(req.params.momentId), b as z.infer<typeof workService.createGoalSchema>);
        return { result: r, resourceId: r.goalId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/milestones', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(workService.createMilestoneSchema, req.body);
    const result = await runCommand({
      operationCode: 'MILESTONE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MILESTONE',
      execute: async (client, b) => {
        const r = await workService.createMilestone(client, ctx, param(req.params.momentId), b as z.infer<typeof workService.createMilestoneSchema>);
        return { result: r, resourceId: r.milestoneId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/movements', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(movementService.createMovementSchema, req.body);
    const result = await runCommand({
      operationCode: 'MOVEMENT_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MOVEMENT',
      execute: async (client, b) => {
        const r = await movementService.recordMovement(client, ctx, param(req.params.momentId), b as z.infer<typeof movementService.createMovementSchema>);
        return { result: r, resourceId: r.movementId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['PERSONAL_PULSE'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/observations', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalService.observationSchema, req.body);
    const result = await withDb((client) => personalService.recordObservation(client, ctx, param(req.params.momentId), body));
    publishProjectionUpdated(ctx.userId, ['PERSONAL_PULSE', 'PERSONAL_LIFE'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/future-items', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalService.futureItemSchema, req.body);
    const result = await withDb((client) => personalService.createFutureItem(client, ctx, param(req.params.momentId), body));
    publishProjectionUpdated(ctx.userId, ['PERSONAL_LIFE'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/lifestyle-activities', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalService.lifestyleActivitySchema, req.body);
    const result = await withDb((client) => personalService.createLifestyleActivity(client, ctx, param(req.params.momentId), body));
    publishProjectionUpdated(ctx.userId, ['PERSONAL_LIFE'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/relationship-activities', requireIdempotencyKey, async (_req, res) => {
  // PROMOTED TO LIVE router.ts (S2 G1) — do not re-implement here.
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

/* REMOVED duplicate live Personal write handlers — authority is router.ts */

v1Router.post('/moments/:momentId/polls', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/planning-items', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/bookings', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/contributions', requireIdempotencyKey, async (_req, res) => {
  // PROMOTED TO LIVE router.ts (S3 Group finance) — do not re-implement here.
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/updates', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/group/invites', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(inviteService.mintInviteSchema, req.body);
    const result = await runCommand({
      operationCode: 'GROUP_INVITE_MINT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MOMENT_INVITE',
      execute: async (client, b) => {
        const r = await inviteService.mintInvite(client, ctx, b as inviteService.MintInviteInput);
        return { result: r, resourceId: r.inviteId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/invites/:code', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => inviteService.getInviteByCode(client, ctx, param(req.params.code)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: data.status }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/group/invites/:code/redeem', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const code = param(req.params.code);
    const result = await runCommand({
      operationCode: 'GROUP_INVITE_REDEEM',
      idempotencyKey: req.idempotencyKey!,
      body: { code },
      ctx,
      resourceType: 'MOMENT_INVITE',
      execute: async (client) => {
        const r = await inviteService.redeemInvite(client, ctx, code);
        return { result: r, resourceId: r.inviteId };
      },
    });
    if (result.momentId) {
      publishProjectionUpdated(ctx.userId, ['GROUP_MOMENTS'], ctx.correlationId);
    }
    res.status(200).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/participants', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(collaborationService.participantSchema, req.body);
    const result = await withDb((client) => collaborationService.addParticipant(client, ctx, param(req.params.momentId), body));
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/purchase-items', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/residents', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.post('/moments/:momentId/memories', requireIdempotencyKey, async (_req, res) => {
  res.status(501).json({
    error: {
      code: 'ROUTE_PROMOTED',
      message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
    },
  });
});

v1Router.get('/moments/:momentId/activity', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) =>
      projectionService.getMomentActivity(client, ctx, param(req.params.momentId), cursor, limit)
    );
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

// --- Personal projections ---
v1Router.get('/personal/setups', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const catalog = personalSetupService.getSetupCatalog().map((s) => ({
      systemCode: s.systemCode,
      title: s.title,
      subtitle: s.subtitle,
      figmaNodeId: s.figmaNodeId,
      defaultMomentTypeCode: s.defaultMomentTypeCode,
      activateLabel: s.activateLabel,
      defaultTitle: s.defaultTitle,
    }));
    const mine = await withDb((client) => personalSetupService.listUserSetups(client, ctx, 20));
    res.json(projectionEnvelope({ catalog, items: mine }, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/personal/setups/:systemCode/activate', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalSetupService.activateSetupSchema, req.body ?? {});
    const systemCode = param(req.params.systemCode).toUpperCase();
    const result = await runCommand({
      operationCode: 'PERSONAL_SETUP_ACTIVATE',
      idempotencyKey: req.idempotencyKey!,
      body: { ...body, systemCode },
      ctx,
      resourceType: 'LIFE_SYSTEM_SETUP',
      execute: async (client, b) => {
        const input = b as personalSetupService.ActivateSetupInput & { systemCode: string };
        const r = await personalSetupService.activatePersonalSetup(client, ctx, input.systemCode, input);
        return { result: r, resourceId: r.setupId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['PERSONAL_MOMENTS', 'PERSONAL_PULSE', 'PERSONAL_LIFE'], ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints(['PERSONAL_MOMENTS', 'PERSONAL_PULSE', 'PERSONAL_LIFE']),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/pulse', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getPersonalPulse(client, ctx.userId));
    res.json(projectionEnvelope(data, ctx.correlationId, { projectionVersion: data.projectionVersion, updatedAt: data.updatedAt, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/moments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) => projectionService.listPersonalMoments(client, ctx.userId, cursor, limit));
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/life', async (_req, res) => {
  // PROMOTED TO LIVE router.ts (S2 G1).
  res.status(501).json({
    error: { code: 'ROUTE_PROMOTED', message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.' },
  });
});

v1Router.get('/personal/memory', async (_req, res) => {
  // PROMOTED TO LIVE router.ts (S2 G1).
  res.status(501).json({
    error: { code: 'ROUTE_PROMOTED', message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.' },
  });
});

v1Router.get('/personal/attention', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getPersonalAttention(client, ctx.userId));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/activity', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) => projectionService.getPersonalActivity(client, ctx.userId, undefined, cursor, limit));
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

// --- Group ---
v1Router.get('/group/moments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) => projectionService.listGroupMoments(client, ctx, cursor, limit));
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

for (const facet of ['pulse', 'life', 'memory', 'finance', 'actions'] as const) {
  v1Router.get(`/group/moments/:momentId/${facet}`, async (_req, res) => {
    // PROMOTED TO LIVE router.ts (S3 Group finance/pulse) — do not re-implement here.
    res.status(501).json({
      error: {
        code: 'ROUTE_PROMOTED',
        message: 'Use live api/v1/router.ts — this product router is NON-RUNTIME.',
      },
    });
  });
}

// --- Business ---
v1Router.get('/business/moments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) => projectionService.listBusinessMoments(client, ctx, cursor, limit));
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

for (const facet of ['pulse', 'life', 'memory', 'finance', 'actions'] as const) {
  v1Router.get(`/business/moments/:momentId/${facet}`, async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const data = await withDb((client) =>
        projectionService.getBusinessMomentProjection(client, ctx, param(req.params.momentId), facet)
      );
      res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
    } catch (e) {
      next(e);
    }
  });
}

v1Router.get('/business/setups', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const catalog = businessSetupService.getSetupCatalog().map((s) => ({
      familyCode: s.familyCode,
      title: s.title,
      subtitle: s.subtitle,
      figmaNodeId: s.figmaNodeId,
      defaultMomentTypeCode: s.defaultMomentTypeCode,
      activateLabel: s.activateLabel,
      defaultTitle: s.defaultTitle,
    }));
    const mine = await withDb((client) => businessSetupService.listUserSetups(client, ctx, 20));
    res.json(projectionEnvelope({ catalog, items: mine }, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/business/setups/:familyCode/activate', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessSetupService.activateBusinessSetupSchema, req.body ?? {});
    const familyCode = param(req.params.familyCode).toUpperCase();
    const result = await runCommand({
      operationCode: 'BUSINESS_SETUP_ACTIVATE',
      idempotencyKey: req.idempotencyKey!,
      body: { ...body, familyCode },
      ctx,
      resourceType: 'BUSINESS_SYSTEM_SETUP',
      execute: async (client, b) => {
        const input = b as businessSetupService.ActivateBusinessSetupInput & { familyCode: string };
        const r = await businessSetupService.activateBusinessSetup(client, ctx, input.familyCode, input);
        return { result: r, resourceId: r.setupId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['BUSINESS_MOMENTS'], ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints(['BUSINESS_MOMENTS']),
      })
    );
  } catch (e) {
    next(e);
  }
});

// --- Companies ---
v1Router.get('/companies', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => businessService.listCompanies(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.createCompanySchema, req.body);
    const result = await withDb((client) => businessService.createCompany(client, ctx, body));
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/companies/:companyId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => businessService.getCompany(client, ctx, param(req.params.companyId)));
    if (!data) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Company not found.', 404);
    res.json(commandEnvelope(data, ctx.correlationId, { resourceVersion: data.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/companies/:companyId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.updateCompanySchema, req.body);
    const result = await withDb((client) => businessService.updateCompany(client, ctx, param(req.params.companyId), body));
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/companies/:companyId/locations', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => businessService.listLocations(client, ctx, param(req.params.companyId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/locations', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.createLocationSchema, req.body);
    const result = await withDb((client) => businessService.createLocation(client, ctx, param(req.params.companyId), body));
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/companies/:companyId/locations/:locationId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.updateLocationSchema, req.body);
    const result = await withDb((client) =>
      businessService.updateLocation(client, ctx, param(req.params.companyId), param(req.params.locationId), body)
    );
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/companies/:companyId/teams', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => businessService.listTeams(client, ctx, param(req.params.companyId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/teams', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.createTeamSchema, req.body);
    const result = await withDb((client) => businessService.createTeam(client, ctx, param(req.params.companyId), body));
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Media ---
v1Router.post('/media/uploads', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(mediaService.uploadIntentSchema, req.body);
    const result = await withDb((client) => mediaService.createUploadIntent(client, ctx, body));
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/media/uploads/:uploadId/complete', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(mediaService.uploadCompleteSchema, req.body);
    const result = await withDb((client) => mediaService.completeUpload(client, ctx, param(req.params.uploadId), body));
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Life360 ---
v1Router.get('/life360', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getLife360(client, ctx.userId));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

// --- AI (proposals re-enter Node command path) ---
v1Router.post('/ai/action-proposals/:actionProposalId/execute', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const result = await runCommand({
      operationCode: 'AI_ACTION_EXECUTE',
      idempotencyKey: req.idempotencyKey!,
      body: { actionProposalId: param(req.params.actionProposalId) },
      ctx,
      resourceType: 'ACTION_PROPOSAL',
      execute: async (client, body) => {
        const r = await executeActionProposal(client, ctx, body.actionProposalId, req.idempotencyKey!);
        return { result: r, resourceId: body.actionProposalId };
      },
    });
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});
