/**
 * Live /v1 router (authoritative). Mounted by app.ts.
 * S2 Personal: life, memory, relationship-activities live here.
 * router-product.ts is NON-RUNTIME / deprecated for any path also defined here — do not dual-mount.
 */
import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import { runCommand } from '../../platform/transaction/run-command';
import { AppError, ErrorCode, commandEnvelope, projectionEnvelope } from '../../platform/errors/errors';
import { authMiddleware, requireIdempotencyKey } from '../middleware/auth';
import { rateLimitMiddleware } from '../../platform/rate-limit/interface';
import { toProjectionHints } from '../../platform/projections/hints';
import { projectionCodesForDomain } from '../../modules/moment/projection-routing';
import { publishProjectionUpdated } from '../../realtime/sse';
import * as businessService from '../../modules/business/service';
import * as businessMembership from '../../modules/business/membership';
import * as businessSetupService from '../../modules/business/setup-service';
import * as businessFinanceService from '../../modules/finance/business-finance';
import * as businessOpsPrecision from '../../modules/business/operations-precision';
import * as businessClosureWriters from '../../modules/business/business-closure-writers';
import * as businessClosureReads from '../../modules/business/business-closure-reads';
import * as businessReads from '../../modules/business/business-reads';
import * as businessMemoryCommands from '../../modules/business/business-memory-commands';
import * as projectionService from '../../modules/projection/service';
import * as deviceService from '../../modules/device/service';
import * as accountService from '../../modules/account/service';
import * as notificationPrefs from '../../modules/notifications/preferences';
import * as momentService from '../../modules/moment/service';
import * as workService from '../../modules/work/service';
import * as financeService from '../../modules/finance/service';
import * as financialAccountService from '../../modules/finance/financial-account';
import * as expenseAttachmentService from '../../modules/finance/expense-attachments';
import * as memoryAttachmentService from '../../modules/memory/memory-attachments';
import * as personalIncomeService from '../../modules/finance/personal-income';
import * as movementService from '../../modules/finance/movement';
import * as recurringScheduleService from '../../modules/finance/recurring-schedule';
import * as mediaService from '../../modules/media/service';
import * as personalService from '../../modules/personal/service';
import * as personalSetupService from '../../modules/personal/setup-service';
import * as lifeOpsPrecision from '../../modules/personal/life-ops-precision';
import * as futurePrecision from '../../modules/personal/future-precision';
import * as lifestylePrecision from '../../modules/personal/lifestyle-precision';
import * as relationshipsPrecision from '../../modules/personal/relationships-precision';
import * as inviteService from '../../modules/collaboration/invite-service';
import * as companyInviteService from '../../modules/business/company-invite-service';
import * as collaborationService from '../../modules/collaboration/service';
import * as groupMembership from '../../modules/collaboration/group-membership';
import * as groupExpenseService from '../../modules/finance/group-expense';
import * as groupBudgetService from '../../modules/finance/group-budget';
import * as groupCollab from '../../modules/collaboration/group-collab-commands';
import * as analyticsEngine from '../../modules/analytics/engine';
import { resolveCapabilityForMomentType } from '../../modules/governance/resolver';
import { executeActionProposal } from '../../modules/ai/action-proposal.command';
import { telemetryRouter } from './telemetry-router';
import { leanAnalyticsRouter } from './lean-analytics-router';

export const v1Router = Router();
v1Router.use('/telemetry', telemetryRouter);
v1Router.use('/analytics/lean', leanAnalyticsRouter);
v1Router.use(authMiddleware);
/** S9-K: per-user when authenticated; fail-open without Redis. */
v1Router.use(
  rateLimitMiddleware((req) => req.requestContext?.userId ?? req.ip ?? 'unknown')
);

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
v1Router.get('/me', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    // S9-B: bootstrap opens its own pool checkouts for true parallel reads.
    const data = await deviceService.getMeBootstrap(null, ctx);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/me', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(accountService.patchMeSchema, req.body);
    const data = await withDb((client) => accountService.patchMe(client, ctx, body));
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/me', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => accountService.softDeleteMe(client, ctx));
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/me/devices', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => deviceService.listDevices(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/me/consents', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => accountService.listConsents(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/me/consents/grant', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(accountService.grantConsentSchema, req.body);
    const result = await runCommand({
      operationCode: 'CONSENT_GRANT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'CONSENT',
      execute: async (client, b) => {
        const r = await accountService.grantConsent(client, ctx, b as accountService.GrantConsentInput);
        return { result: r, resourceId: r.consentId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/me/consents/withdraw', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(accountService.withdrawConsentSchema, req.body);
    const result = await runCommand({
      operationCode: 'CONSENT_WITHDRAW',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'CONSENT',
      execute: async (client, b) => {
        const r = await accountService.withdrawConsent(client, ctx, b as accountService.WithdrawConsentInput);
        return { result: r, resourceId: ctx.userId };
      },
    });
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

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

v1Router.get('/me/notification-preferences', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => notificationPrefs.getGlobalNotificationPrefs(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/me/notification-preferences', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(notificationPrefs.patchGlobalNotificationPrefsSchema, req.body);
    const data = await withDb((client) => notificationPrefs.patchGlobalNotificationPrefs(client, ctx, body));
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/notification-preferences', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const momentId = param(req.params.momentId);
    const data = await withDb((client) =>
      notificationPrefs.getMomentNotificationPrefs(client, ctx, momentId)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/notification-preferences', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const momentId = param(req.params.momentId);
    const body = parseBody(notificationPrefs.patchMomentNotificationPrefsSchema, req.body);
    const data = await withDb((client) =>
      notificationPrefs.patchMomentNotificationPrefs(client, ctx, momentId, body)
    );
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Shell list reads (Phase 5 empty / inactive moment experience) ---
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

// --- Business shell (company selector + setup flow) ---
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
    const result = await withDb((client) =>
      businessService.createLocation(client, ctx, param(req.params.companyId), body)
    );
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/companies/:companyId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => businessService.getCompany(client, ctx, param(req.params.companyId)));
    if (!data) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Company not found.', 404);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/companies/:companyId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.updateCompanySchema, req.body);
    const result = await withDb((client) =>
      businessService.updateCompany(client, ctx, param(req.params.companyId), body)
    );
    res.json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/companies/:companyId/locations/:locationId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessService.updateLocationSchema, req.body);
    const result = await withDb((client) =>
      businessService.updateLocation(
        client,
        ctx,
        param(req.params.companyId),
        param(req.params.locationId),
        body
      )
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
    const result = await withDb((client) =>
      businessService.createTeam(client, ctx, param(req.params.companyId), body)
    );
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/companies/:companyId/members', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessMembership.listCompanyMembers(client, ctx, param(req.params.companyId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/leave', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessMembership.leaveCompanySchema, req.body ?? {});
    const result = await runCommand({
      operationCode: 'COMPANY_MEMBER_LEAVE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'COMPANY_MEMBERSHIP',
      execute: async (client, b) => {
        const r = await businessMembership.leaveCompany(
          client,
          ctx,
          param(req.params.companyId),
          b as z.infer<typeof businessMembership.leaveCompanySchema>
        );
        return { result: r, resourceId: r.companyId };
      },
    });
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/members', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessMembership.addCompanyMemberSchema, req.body);
    const result = await runCommand({
      operationCode: 'COMPANY_MEMBER_ADD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'COMPANY_MEMBERSHIP',
      execute: async (client, b) => {
        const r = await businessMembership.addCompanyMember(
          client,
          ctx,
          param(req.params.companyId),
          b as z.infer<typeof businessMembership.addCompanyMemberSchema>
        );
        return { result: r, resourceId: r.membershipId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/vendors', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessFinanceService.createVendorSchema, req.body);
    const result = await runCommand({
      operationCode: 'VENDOR_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'VENDOR',
      execute: async (client, b) => {
        const r = await businessFinanceService.createVendor(
          client,
          ctx,
          param(req.params.companyId),
          b as z.infer<typeof businessFinanceService.createVendorSchema>
        );
        return { result: r, resourceId: r.vendorId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/companies/:companyId/vendors/:vendorId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.updateVendorSchema, req.body);
    const result = await runCommand({
      operationCode: 'VENDOR_UPDATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'VENDOR',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.updateVendor(
          client,
          ctx,
          param(req.params.companyId),
          param(req.params.vendorId),
          b as z.infer<typeof businessOpsPrecision.updateVendorSchema>
        );
        return { result: r, resourceId: r.vendorId };
      },
    });
    res.status(200).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/vendors/:vendorId/contracts', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createVendorContractSchema, req.body);
    const result = await runCommand({
      operationCode: 'VENDOR_CONTRACT_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'VENDOR_CONTRACT',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createVendorContract(
          client,
          ctx,
          param(req.params.companyId),
          param(req.params.vendorId),
          b as z.infer<typeof businessOpsPrecision.createVendorContractSchema>
        );
        return { result: r, resourceId: r.vendorContractId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/companies/:companyId/vendors/:vendorId/sla-definitions', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createSlaDefinitionSchema, req.body);
    const result = await runCommand({
      operationCode: 'SLA_DEFINITION_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'SLA',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createSlaDefinition(
          client,
          ctx,
          param(req.params.companyId),
          param(req.params.vendorId),
          b as z.infer<typeof businessOpsPrecision.createSlaDefinitionSchema>
        );
        return { result: r, resourceId: r.slaDefinitionId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.post(
  '/companies/:companyId/sla-definitions/:slaDefinitionId/checks',
  requireIdempotencyKey,
  async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const body = parseBody(businessOpsPrecision.createSlaCheckSchema, req.body);
      const result = await runCommand({
        operationCode: 'SLA_CHECK_CREATE',
        idempotencyKey: req.idempotencyKey!,
        body,
        ctx,
        resourceType: 'SLA_CHECK',
        execute: async (client, b) => {
          const r = await businessOpsPrecision.recordSlaCheck(
            client,
            ctx,
            param(req.params.companyId),
            param(req.params.slaDefinitionId),
            b as z.infer<typeof businessOpsPrecision.createSlaCheckSchema>
          );
          return { result: r, resourceId: r.slaCheckId };
        },
      });
      const hints = ['business.pulse', 'business.moments'] as const;
      publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
      res.status(201).json(
        commandEnvelope(result, ctx.correlationId, {
          projectionHints: toProjectionHints([...hints], 'refresh'),
        })
      );
    } catch (e) {
      next(e);
    }
  }
);

v1Router.post('/moments/:momentId/issues', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createIssueSchema, req.body);
    const result = await runCommand({
      operationCode: 'ISSUE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'ISSUE',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createIssue(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessOpsPrecision.createIssueSchema>
        );
        return { result: r, resourceId: r.issueId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/risks', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createRiskSchema, req.body);
    const result = await runCommand({
      operationCode: 'RISK_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RISK',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createRisk(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessOpsPrecision.createRiskSchema>
        );
        return { result: r, resourceId: r.riskId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/improvements', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createImprovementSchema, req.body);
    const result = await runCommand({
      operationCode: 'IMPROVEMENT_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'OPERATIONAL_IMPROVEMENT',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createImprovement(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessOpsPrecision.createImprovementSchema>
        );
        return { result: r, resourceId: r.improvementId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/business-updates', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createBusinessUpdateSchema, req.body);
    const result = await runCommand({
      operationCode: 'BUSINESS_UPDATE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'BUSINESS_UPDATE',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createBusinessUpdate(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessOpsPrecision.createBusinessUpdateSchema>
        );
        return { result: r, resourceId: r.updateId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/approval-requests', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessOpsPrecision.createApprovalRequestSchema, req.body);
    const result = await runCommand({
      operationCode: 'APPROVAL_REQUEST_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'APPROVAL_REQUEST',
      execute: async (client, b) => {
        const r = await businessOpsPrecision.createApprovalRequest(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessOpsPrecision.createApprovalRequestSchema>
        );
        return { result: r, resourceId: r.approvalRequestId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

// --- Business Deployment Closure: Wave 1 write routes ---
v1Router.post('/moments/:momentId/tax-obligations', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createTaxObligationSchema, req.body);
    const result = await runCommand({
      operationCode: 'TAX_OBLIGATION_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'TAX_OBLIGATION',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createTaxObligation(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createTaxObligationSchema>
        );
        return { result: r, resourceId: r.taxObligationId };
      },
    });
    const hints = ['business.pulse', 'business.moments', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/forecast-scenarios', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createForecastScenarioSchema, req.body);
    const result = await runCommand({
      operationCode: 'FORECAST_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'FORECAST',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createForecastScenario(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createForecastScenarioSchema>
        );
        return { result: r, resourceId: r.forecastScenarioId };
      },
    });
    const hints = ['business.pulse', 'business.moments', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/investor-updates', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createInvestorUpdateSchema, req.body);
    const result = await runCommand({
      operationCode: 'INVESTOR_UPDATE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'INVESTOR_UPDATE',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createInvestorUpdate(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createInvestorUpdateSchema>
        );
        return { result: r, resourceId: r.investorUpdateId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/budget-alerts', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createBudgetAlertSchema, req.body);
    const result = await runCommand({
      operationCode: 'BUDGET_ALERT_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'BUDGET_ALERT',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createBudgetAlert(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createBudgetAlertSchema>
        );
        return { result: r, resourceId: r.budgetAlertId };
      },
    });
    const hints = ['business.pulse', 'business.moments', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/business-reviews', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createBudgetReviewSchema, req.body);
    const result = await runCommand({
      operationCode: 'REVIEW_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'REVIEW',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createBudgetReview(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createBudgetReviewSchema>
        );
        return { result: r, resourceId: r.businessReviewId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/decisions', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createDecisionSchema, req.body);
    const result = await runCommand({
      operationCode: 'DECISION_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'DECISION',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createDecision(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createDecisionSchema>
        );
        return { result: r, resourceId: r.decisionId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/meeting-records', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createMeetingRecordSchema, req.body);
    const result = await runCommand({
      operationCode: 'MEETING_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MEETING',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createMeetingRecord(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createMeetingRecordSchema>
        );
        return { result: r, resourceId: r.meetingRecordId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/recognitions', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createRecognitionSchema, req.body);
    const result = await runCommand({
      operationCode: 'RECOGNITION_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RECOGNITION',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createRecognition(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createRecognitionSchema>
        );
        return { result: r, resourceId: r.recognitionId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/retrospectives', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createRetrospectiveSchema, req.body);
    const result = await runCommand({
      operationCode: 'RETRO_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RETROSPECTIVE',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createRetrospective(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createRetrospectiveSchema>
        );
        return { result: r, resourceId: r.retrospectiveId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/activity-log-entries', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureWriters.createActivityLogEntrySchema, req.body);
    const result = await runCommand({
      operationCode: 'ACTIVITY_LOG_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'ACTIVITY_LOG',
      execute: async (client, b) => {
        const r = await businessClosureWriters.createActivityLogEntry(
          client, ctx, param(req.params.momentId),
          b as z.infer<typeof businessClosureWriters.createActivityLogEntrySchema>
        );
        return { result: r, resourceId: r.activityLogEntryId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

// --- Business Deployment Closure: Wave 3 read routes ---
v1Router.get('/business/moments/:momentId/capacity', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.getCapacity(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.get('/business/moments/:momentId/workload', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.getWorkload(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.get('/business/moments/:momentId/mom-deltas', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.getMomDeltas(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.get('/business/moments/:momentId/progress-snapshot', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.getProgressSnapshot(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.get('/business/moments/:momentId/roster', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.getRoster(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.get('/business/moments/:momentId/weekly-report', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const periodRaw = String(req.query.period ?? '7d');
    const period = periodRaw === '30d' ? '30d' : '7d';
    const data = await withDb((client) =>
      businessClosureReads.getWeeklyReport(client, ctx, param(req.params.momentId), period)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) { next(e); }
});

v1Router.post('/moments/:momentId/issues/:issueId/evidence', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessClosureReads.addIssueEvidenceSchema, req.body);
    const result = await runCommand({
      operationCode: 'ISSUE_EVIDENCE_ADD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'ISSUE',
      execute: async (client, b) => {
        const r = await businessClosureReads.addIssueEvidence(
          client, ctx, param(req.params.momentId), param(req.params.issueId),
          b as z.infer<typeof businessClosureReads.addIssueEvidenceSchema>
        );
        return { result: r, resourceId: r.issueId };
      },
    });
    const hints = ['business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') })
    );
  } catch (e) { next(e); }
});

v1Router.post('/business/moments/:momentId/share-link', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessClosureReads.createShareLink(client, ctx, param(req.params.momentId))
    );
    res.status(201).json(commandEnvelope(data, ctx.correlationId));
  } catch (e) { next(e); }
});

v1Router.get('/business/setups', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb(async (client) => {
      const catalog = businessSetupService.getSetupCatalog();
      const mine = await businessSetupService.listUserSetups(client, ctx);
      return { catalog, mine };
    });
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/business/setups/:familyCode/activate', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const familyCode = param(req.params.familyCode);
    const body = parseBody(businessSetupService.activateBusinessSetupSchema, req.body);
    const result = await runCommand({
      operationCode: 'BUSINESS_SETUP_ACTIVATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MOMENT',
      execute: async (client, b) => {
        const r = await businessSetupService.activateBusinessSetup(
          client,
          ctx,
          familyCode,
          b as z.infer<typeof businessSetupService.activateBusinessSetupSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    const hints = ['business.moments', 'business.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
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
      const status = typeof data.status === 'string' ? data.status : 'OK';
      res.json(projectionEnvelope(data, ctx.correlationId, { status }));
    } catch (e) {
      next(e);
    }
  });
}

v1Router.get('/business/moments/:momentId/activity', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) =>
      projectionService.getBusinessMomentActivity(client, ctx, param(req.params.momentId), cursor, limit)
    );
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** B2-B: Structured business moments timeline + KPIs */
v1Router.get('/business/moments/:momentId/moments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const limit = parseInt(String(req.query.limit ?? '50'), 10);
    const data = await withDb((client) =>
      businessReads.listBusinessMomentTimeline(client, ctx, param(req.params.momentId), limit)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

const businessListRoutes: Array<{
  path: string;
  handler: (client: import('pg').PoolClient, ctx: import('../../platform/request-context/context').RequestContext, momentId: string) => Promise<unknown>;
}> = [
  { path: 'expenses', handler: businessReads.listBusinessExpenses },
  { path: 'revenues', handler: businessReads.listBusinessRevenues },
  { path: 'invoices', handler: businessReads.listBusinessInvoices },
  { path: 'issues', handler: businessReads.listBusinessIssues },
  { path: 'improvements', handler: businessReads.listBusinessImprovements },
  { path: 'updates', handler: businessReads.listBusinessUpdates },
  { path: 'approvals', handler: businessReads.listPendingApprovals },
  { path: 'memories', handler: businessReads.listBusinessMemories },
];

for (const route of businessListRoutes) {
  v1Router.get(`/business/moments/:momentId/${route.path}`, async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const data = await withDb((client) =>
        route.handler(client, ctx, param(req.params.momentId))
      );
      res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
    } catch (e) {
      next(e);
    }
  });
}

v1Router.get('/companies/:companyId/vendors', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      businessReads.listCompanyVendors(client, ctx, param(req.params.companyId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/business/moments/:momentId/memories', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessMemoryCommands.createBusinessMemorySchema, req.body);
    const result = await runCommand({
      operationCode: 'BUSINESS_MEMORY_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MEMORY',
      execute: async (client, b) => {
        const r = await businessMemoryCommands.createBusinessMemory(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessMemoryCommands.createBusinessMemorySchema>
        );
        return { result: r, resourceId: r.memoryId };
      },
    });
    const hints = ['business.memory', 'business.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/business-expenses', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessFinanceService.createBusinessExpenseSchema, req.body);
    const result = await runCommand({
      operationCode: 'BUSINESS_EXPENSE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'EXPENSE',
      execute: async (client, b) => {
        const r = await businessFinanceService.createBusinessExpense(
          client,
          ctx,
          param(req.params.momentId),
          b as businessFinanceService.CreateBusinessExpenseInput
        );
        return { result: r, resourceId: r.expenseId };
      },
    });
    const hints = ['business.pulse', 'business.finance', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/revenues', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessFinanceService.createBusinessRevenueSchema, req.body);
    const result = await runCommand({
      operationCode: 'REVENUE_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'REVENUE',
      execute: async (client, b) => {
        const r = await businessFinanceService.createBusinessRevenue(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessFinanceService.createBusinessRevenueSchema>
        );
        return { result: r, resourceId: r.revenueId };
      },
    });
    const hints = ['business.pulse', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/invoices', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessFinanceService.createBusinessInvoiceSchema, req.body);
    const result = await runCommand({
      operationCode: 'INVOICE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'INVOICE',
      execute: async (client, b) => {
        const r = await businessFinanceService.createBusinessInvoice(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof businessFinanceService.createBusinessInvoiceSchema>
        );
        return { result: r, resourceId: r.invoiceId };
      },
    });
    const hints = ['business.pulse', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/approvals/:approvalRequestId/decide', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(businessFinanceService.decideApprovalSchema, req.body);
    const result = await runCommand({
      operationCode: 'APPROVAL_DECIDE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'APPROVAL_REQUEST',
      execute: async (client, b) => {
        const r = await businessFinanceService.decideBusinessApproval(
          client,
          ctx,
          param(req.params.approvalRequestId),
          b as z.infer<typeof businessFinanceService.decideApprovalSchema>
        );
        return { result: r, resourceId: r.approvalRequestId };
      },
    });
    const hints = ['business.pulse', 'business.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

// --- S1 Personal slice (moment create, expense, pulse, activity, setups) ---
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
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
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
        const r = await momentService.updateMoment(
          client,
          ctx,
          param(req.params.momentId),
          b as momentService.UpdateMomentInput
        );
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
        const r = await workService.createGoal(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof workService.createGoalSchema>
        );
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
        const r = await workService.createMilestone(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof workService.createMilestoneSchema>
        );
        return { result: r, resourceId: r.milestoneId };
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
        const r = await workService.createTask(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof workService.createTaskSchema>
        );
        return { result: r, resourceId: r.taskId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { resourceVersion: result.version }));
  } catch (e) {
    next(e);
  }
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
        const r = await financeService.createExpense(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof financeService.createExpenseSchema>
        );
        return { result: r, resourceId: r.expenseId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/observations', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalService.observationSchema, req.body);
    const result = await runCommand({
      operationCode: 'OBSERVATION_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIFE_OBSERVATION',
      execute: async (client, b) => {
        const r = await personalService.recordObservation(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof personalService.observationSchema>
        );
        return { result: r, resourceId: r.observationId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/future-items', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(futurePrecision.futureItemPrecisionSchema, req.body);
    const result = await runCommand({
      operationCode: 'FUTURE_ITEM_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'FUTURE_ITEM',
      execute: async (client, b) => {
        const r = await futurePrecision.createFutureItemPrecision(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof futurePrecision.futureItemPrecisionSchema>
        );
        return { result: r, resourceId: r.itemId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/lifestyle-activities', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(lifestylePrecision.lifestyleActivityPrecisionSchema, req.body);
    const result = await runCommand({
      operationCode: 'LIFESTYLE_ACTIVITY_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIFESTYLE_ACTIVITY',
      execute: async (client, b) => {
        const r = await lifestylePrecision.createLifestyleActivityPrecision(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof lifestylePrecision.lifestyleActivityPrecisionSchema>
        );
        return { result: r, resourceId: r.activityId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/lifestyle-activities/:activityId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalService.updateLifestyleActivitySchema, req.body);
    const data = await withDb((client) =>
      personalService.updateLifestyleActivity(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.activityId),
        body
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      projectionEnvelope(data, ctx.correlationId, {
        status: 'OK',
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId/lifestyle-activities/:activityId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      personalService.voidLifestyleActivity(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.activityId)
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/expenses/:expenseId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      financeService.getExpense(client, ctx, param(req.params.momentId), param(req.params.expenseId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/expenses/:expenseId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(financeService.updateExpenseSchema, req.body);
    const data = await withDb((client) =>
      financeService.updateExpense(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.expenseId),
        body
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      projectionEnvelope(data, ctx.correlationId, {
        status: 'OK',
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId/expenses/:expenseId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      financeService.voidExpense(client, ctx, param(req.params.momentId), param(req.params.expenseId))
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/expenses/:expenseId/attachments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      expenseAttachmentService.listExpenseAttachments(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.expenseId)
      )
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/expenses/:expenseId/attachments', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(expenseAttachmentService.attachExpenseMediaSchema, req.body);
    const data = await withDb((client) =>
      expenseAttachmentService.attachExpenseMedia(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.expenseId),
        body.uploadId
      )
    );
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId/expenses/:expenseId/attachments/:uploadId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    await withDb((client) =>
      expenseAttachmentService.detachExpenseMedia(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.expenseId),
        param(req.params.uploadId)
      )
    );
    res.status(204).send();
  } catch (e) {
    next(e);
  }
});

v1Router.get('/financial-accounts', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => financialAccountService.listUserFinancialAccounts(client, ctx));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/financial-accounts', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(financialAccountService.createFinancialAccountSchema, req.body);
    const data = await withDb((client) => financialAccountService.createUserFinancialAccount(client, ctx, body));
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/media/uploads', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(mediaService.uploadIntentSchema, req.body);
    const data = await withDb((client) => mediaService.createUploadIntent(client, ctx, body));
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/media/uploads/:uploadId/complete', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(mediaService.uploadCompleteSchema, req.body);
    const data = await withDb((client) =>
      mediaService.completeUpload(client, ctx, param(req.params.uploadId), body)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/income', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalIncomeService.createPersonalIncomeSchema, req.body);
    const result = await runCommand({
      operationCode: 'INCOME_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'FINANCIAL_MOVEMENT',
      execute: async (client, b) => {
        const r = await personalIncomeService.createPersonalIncome(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof personalIncomeService.createPersonalIncomeSchema>
        );
        return { result: r, resourceId: r.incomeId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/income/:incomeId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      personalIncomeService.getPersonalIncome(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.incomeId)
      )
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/income/:incomeId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(personalIncomeService.updatePersonalIncomeSchema, req.body);
    const data = await withDb((client) =>
      personalIncomeService.updatePersonalIncome(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.incomeId),
        body
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId/income/:incomeId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      personalIncomeService.voidPersonalIncome(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.incomeId)
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
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
        const r = await movementService.recordMovement(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof movementService.createMovementSchema>
        );
        return { result: r, resourceId: r.movementId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['PERSONAL_PULSE'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/recurring-schedules', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      recurringScheduleService.listRecurringSchedules(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/recurring-schedules', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(recurringScheduleService.createRecurringScheduleSchema, req.body);
    const data = await withDb((client) =>
      recurringScheduleService.createRecurringSchedule(client, ctx, param(req.params.momentId), body)
    );
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/recurring-schedules/:scheduleId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(recurringScheduleService.updateRecurringScheduleSchema, req.body);
    const data = await withDb((client) =>
      recurringScheduleService.updateRecurringSchedule(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.scheduleId),
        body
      )
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/recurring-schedules/:scheduleId/generate', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      recurringScheduleService.generateRecurringInstance(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.scheduleId),
        req.idempotencyKey!
      )
    );
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

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

v1Router.get('/personal/attention', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getPersonalAttention(client, ctx.userId));
    const status = !data.items?.length ? 'EMPTY' : 'OK';
    res.json(projectionEnvelope(data, ctx.correlationId, { status }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/pulse', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const momentId = typeof req.query.momentId === 'string' ? req.query.momentId : undefined;
    const data = await withDb((client) => projectionService.getPersonalPulse(client, ctx.userId, momentId));
    const status = data.projectionVersion === 0 ? 'EMPTY' : 'OK';
    res.json(
      projectionEnvelope(data, ctx.correlationId, {
        projectionVersion: data.projectionVersion,
        updatedAt: data.updatedAt,
        status,
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/activity', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const momentId = typeof req.query.momentId === 'string' ? req.query.momentId : undefined;
    const cursor = req.query.cursor as string | undefined;
    const limit = parseInt(String(req.query.limit ?? '20'), 10);
    const page = await withDb((client) =>
      projectionService.getPersonalActivity(client, ctx.userId, momentId, cursor, limit)
    );
    res.json(projectionEnvelope(page, ctx.correlationId, { nextCursor: page.nextCursor, status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/life', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getPersonalLife(client, ctx.userId));
    // Section honesty: DTO may include figma-seeded fields — clients must classify per section (S2 G3).
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/memory', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getPersonalMemory(client, ctx.userId));
    // Honest empty projection — clients must not fabricate Memory items (S2 G4).
    const status = !data.items?.length ? 'EMPTY' : 'OK';
    res.json(projectionEnvelope(data, ctx.correlationId, { status }));
  } catch (e) {
    next(e);
  }
});

/** Life Ops precision — RP-01 runtime summary */
v1Router.get('/personal/moments/:momentId/runtime-summary', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifeOpsPrecision.getRuntimeSummary(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** RP-02 mood history */
v1Router.get('/personal/moments/:momentId/mood-history', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const days = parseInt(String(req.query.days ?? '7'), 10);
    const data = await withDb((client) =>
      lifeOpsPrecision.getMoodHistory(client, ctx, param(req.params.momentId), days)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** RP-03 adjustment insight */
v1Router.get('/personal/moments/:momentId/adjustment-insight', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifeOpsPrecision.getAdjustmentInsight(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** RP-04 activity summary */
v1Router.get('/personal/moments/:momentId/activity-summary', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const period = String(req.query.period ?? 'MONTH').toUpperCase() === 'WEEK' ? 'WEEK' : 'MONTH';
    const data = await withDb((client) =>
      lifeOpsPrecision.getActivitySummary(client, ctx, param(req.params.momentId), period)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** RP-05 money journey */
v1Router.get('/personal/moments/:momentId/money-journey', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const months = parseInt(String(req.query.periodMonths ?? '6'), 10);
    const data = await withDb((client) =>
      lifeOpsPrecision.getMoneyJourney(client, ctx, param(req.params.momentId), months)
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

/** Expense category catalogue (V045) */
v1Router.get('/finance/expense-categories', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => lifeOpsPrecision.listExpenseCategories(client));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

// ─── PX family precision reads / profiles ───────────────────────────────────

v1Router.get('/personal/moments/:momentId/future-runtime-summary', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      futurePrecision.getFutureRuntimeSummary(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/future-axis-snapshot', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      futurePrecision.getFutureAxisSnapshot(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/future-inventory', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      futurePrecision.getFutureInventory(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/future-journey', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      futurePrecision.getFutureJourney(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.patch('/personal/moments/:momentId/future-profile', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(futurePrecision.futureProfileSchema, req.body);
    const result = await runCommand({
      operationCode: 'FUTURE_PROFILE_UPSERT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'FUTURE_BUILDING_PROFILE',
      execute: async (client, b) => {
        const r = await futurePrecision.upsertFutureProfile(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof futurePrecision.futureProfileSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    res.status(200).json(commandEnvelope(result, ctx.correlationId, {}));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/moments/:momentId/lifestyle-runtime-summary', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifestylePrecision.getLifestyleRuntimeSummary(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/lifestyle-vitality-snapshot', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifestylePrecision.getLifestyleVitalitySnapshot(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/lifestyle-inventory', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifestylePrecision.getLifestyleInventory(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/lifestyle-journey', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      lifestylePrecision.getLifestyleJourney(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.patch('/personal/moments/:momentId/lifestyle-profile', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(lifestylePrecision.lifestyleProfileSchema, req.body);
    const result = await runCommand({
      operationCode: 'LIFESTYLE_PROFILE_UPSERT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIFESTYLE_PROFILE',
      execute: async (client, b) => {
        const r = await lifestylePrecision.upsertLifestyleProfile(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof lifestylePrecision.lifestyleProfileSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    res.status(200).json(commandEnvelope(result, ctx.correlationId, {}));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/personal/moments/:momentId/relationships-runtime-summary', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      relationshipsPrecision.getRelationshipsRuntimeSummary(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/relationships-bond-snapshot', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      relationshipsPrecision.getRelationshipsBondSnapshot(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/relationships-connections', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      relationshipsPrecision.getRelationshipsConnections(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.get('/personal/moments/:momentId/relationships-journey', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      relationshipsPrecision.getRelationshipsJourney(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
v1Router.patch('/personal/moments/:momentId/relationships-profile', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(relationshipsPrecision.relationshipsProfileSchema, req.body);
    const result = await runCommand({
      operationCode: 'RELATIONSHIPS_PROFILE_UPSERT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RELATIONSHIPS_PROFILE',
      execute: async (client, b) => {
        const r = await relationshipsPrecision.upsertRelationshipsProfile(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof relationshipsPrecision.relationshipsProfileSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    res.status(200).json(commandEnvelope(result, ctx.correlationId, {}));
  } catch (e) {
    next(e);
  }
});

/** Life Ops profile upsert */
v1Router.patch('/personal/moments/:momentId/life-ops-profile', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(lifeOpsPrecision.lifeOpsProfileSchema, req.body);
    const result = await runCommand({
      operationCode: 'LIFE_OPS_PROFILE_UPSERT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIFE_OPS_PROFILE',
      execute: async (client, b) => {
        const r = await lifeOpsPrecision.upsertLifeOpsProfile(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof lifeOpsPrecision.lifeOpsProfileSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    res.status(200).json(commandEnvelope(result, ctx.correlationId, {}));
  } catch (e) {
    next(e);
  }
});

/** Attention capture (V043) */
v1Router.post('/moments/:momentId/attention-captures', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(lifeOpsPrecision.attentionCaptureSchema, req.body);
    const result = await runCommand({
      operationCode: 'ATTENTION_CAPTURE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'ATTENTION_CAPTURE',
      execute: async (client, b) => {
        const r = await lifeOpsPrecision.recordAttentionCapture(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof lifeOpsPrecision.attentionCaptureSchema>
        );
        return { result: r, resourceId: r.attentionCaptureId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

/** Life Ops adjust (V044) */
v1Router.post('/moments/:momentId/life-ops-adjustments', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(lifeOpsPrecision.lifeOpsAdjustSchema, req.body);
    const result = await runCommand({
      operationCode: 'LIFE_OPS_ADJUST',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIFE_OPS_ADJUSTMENT',
      execute: async (client, b) => {
        const r = await lifeOpsPrecision.recordLifeOpsAdjust(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof lifeOpsPrecision.lifeOpsAdjustSchema>
        );
        return { result: r, resourceId: r.adjustmentId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/relationship-activities', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(relationshipsPrecision.relationshipActivityPrecisionSchema, req.body);
    const result = await runCommand({
      operationCode: 'RELATIONSHIP_ACTIVITY_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RELATIONSHIP_ACTIVITY',
      execute: async (client, b) => {
        const r = await relationshipsPrecision.recordRelationshipActivityPrecision(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof relationshipsPrecision.relationshipActivityPrecisionSchema>
        );
        return { result: r, resourceId: r.activityId };
      },
    });
    const hints = ['personal.activity', 'personal.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

// --- S3 Group membership + finance foundations ---
v1Router.get('/group/moments/:momentId/participants', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      groupMembership.listGroupParticipants(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/group/moments/:momentId/leave', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupMembership.leaveGroupMomentSchema, req.body ?? {});
    const result = await runCommand({
      operationCode: 'GROUP_PARTICIPANT_LEAVE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'PARTICIPANT',
      execute: async (client, b) => {
        const r = await groupMembership.leaveGroupMoment(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupMembership.leaveGroupMomentSchema>
        );
        return { result: r, resourceId: r.momentId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['GROUP_PARTICIPANTS', 'GROUP_ACTIVITY', 'GROUP_PULSE'], ctx.correlationId);
    res.json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.patch(
  '/group/moments/:momentId/participants/:participantId',
  requireIdempotencyKey,
  async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const body = parseBody(groupMembership.updateGroupParticipantRoleSchema, req.body ?? {});
      const result = await runCommand({
        operationCode: 'GROUP_PARTICIPANT_ROLE_UPDATE',
        idempotencyKey: req.idempotencyKey!,
        body,
        ctx,
        resourceType: 'PARTICIPANT',
        execute: async (client, b) => {
          const r = await groupMembership.updateGroupParticipantRole(
            client,
            ctx,
            param(req.params.momentId),
            param(req.params.participantId),
            b as z.infer<typeof groupMembership.updateGroupParticipantRoleSchema>
          );
          return { result: r, resourceId: r.participantId };
        },
      });
      publishProjectionUpdated(ctx.userId, ['GROUP_PARTICIPANTS', 'GROUP_ACTIVITY', 'GROUP_PULSE'], ctx.correlationId);
      res.json(commandEnvelope(result, ctx.correlationId));
    } catch (e) {
      next(e);
    }
  }
);

v1Router.post(
  '/group/moments/:momentId/participants/:participantId/remove',
  requireIdempotencyKey,
  async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const result = await runCommand({
        operationCode: 'GROUP_PARTICIPANT_REMOVE',
        idempotencyKey: req.idempotencyKey!,
        body: {},
        ctx,
        resourceType: 'PARTICIPANT',
        execute: async (client) => {
          const r = await groupMembership.removeGroupParticipant(
            client,
            ctx,
            param(req.params.momentId),
            param(req.params.participantId)
          );
          return { result: r, resourceId: r.participantId };
        },
      });
      publishProjectionUpdated(ctx.userId, ['GROUP_PARTICIPANTS', 'GROUP_ACTIVITY', 'GROUP_PULSE'], ctx.correlationId);
      res.json(commandEnvelope(result, ctx.correlationId));
    } catch (e) {
      next(e);
    }
  }
);

v1Router.post('/moments/:momentId/participants', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(collaborationService.participantSchema, req.body);
    const result = await runCommand({
      operationCode: 'PARTICIPANT_ADD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'PARTICIPANT',
      execute: async (client, b) => {
        const r = await collaborationService.addParticipant(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof collaborationService.participantSchema>
        );
        return { result: r, resourceId: r.participantId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['GROUP_PARTICIPANTS', 'GROUP_ACTIVITY'], ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

for (const facet of ['pulse', 'life', 'memory', 'finance'] as const) {
  v1Router.get(`/group/moments/:momentId/${facet}`, async (req, res, next) => {
    try {
      const ctx = req.requestContext!;
      const data = await withDb((client) =>
        projectionService.getGroupMomentProjection(client, ctx, param(req.params.momentId), facet)
      );
      const status = typeof data.status === 'string' ? data.status : 'OK';
      res.json(projectionEnvelope(data, ctx.correlationId, { status }));
    } catch (e) {
      next(e);
    }
  });
}

v1Router.get('/group/moments/:momentId/activity', async (req, res, next) => {
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

v1Router.patch('/group/moments/:momentId/budget', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupBudgetService.patchGroupMomentBudgetSchema, req.body);
    const result = await runCommand({
      operationCode: 'GROUP_BUDGET_PATCH',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'BUDGET',
      execute: async (client, b) => {
        const r = await groupBudgetService.patchGroupMomentBudget(
          client,
          ctx,
          param(req.params.momentId),
          b as groupBudgetService.PatchGroupMomentBudgetInput
        );
        return { result: r, resourceId: r.budgetId };
      },
    });
    const hints = ['group.pulse', 'group.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/group-expenses', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupExpenseService.createGroupExpenseSchema, req.body);
    const result = await runCommand({
      operationCode: 'GROUP_EXPENSE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'EXPENSE',
      execute: async (client, b) => {
        const r = await groupExpenseService.createGroupExpense(
          client,
          ctx,
          param(req.params.momentId),
          b as groupExpenseService.CreateGroupExpenseInput
        );
        return { result: r, resourceId: r.expenseId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/group-expenses/:expenseId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const pool = getPool();
    const client = await pool.connect();
    try {
      const data = await groupExpenseService.getGroupExpense(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.expenseId)
      );
      res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
    } finally {
      client.release();
    }
  } catch (e) {
    next(e);
  }
});

v1Router.patch('/moments/:momentId/group-expenses/:expenseId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupExpenseService.updateGroupExpenseSchema, req.body);
    const result = await runCommand({
      operationCode: 'GROUP_EXPENSE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'EXPENSE',
      execute: async (client, b) => {
        const r = await groupExpenseService.updateGroupExpense(
          client,
          ctx,
          param(req.params.momentId),
          param(req.params.expenseId),
          b as groupExpenseService.UpdateGroupExpenseInput
        );
        return { result: r, resourceId: r.expenseId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.delete('/moments/:momentId/group-expenses/:expenseId', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const result = await runCommand({
      operationCode: 'GROUP_EXPENSE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body: {},
      ctx,
      resourceType: 'EXPENSE',
      execute: async (client) => {
        const r = await groupExpenseService.voidGroupExpense(
          client,
          ctx,
          param(req.params.momentId),
          param(req.params.expenseId)
        );
        return { result: r, resourceId: r.expenseId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.finance'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.json(
      commandEnvelope(result, ctx.correlationId, {
        resourceVersion: result.version,
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/contributions', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(collaborationService.contributionSchema, req.body);
    const result = await runCommand({
      operationCode: 'CONTRIBUTION_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'CONTRIBUTION',
      execute: async (client, b) => {
        const r = await collaborationService.recordContribution(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof collaborationService.contributionSchema>
        );
        return { result: r, resourceId: r.contributionId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- GX2-C Group collaboration commands + reads ---
v1Router.get('/group/moments/:momentId/planning-items', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listPlanningItems(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/planning-items', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.planningItemSchema, req.body);
    const result = await runCommand({
      operationCode: 'PLANNING_ITEM_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'PLANNING_ITEM',
      execute: async (client, b) => {
        const r = await groupCollab.createPlanningItemCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.planningItemSchema>
        );
        return { result: r, resourceId: r.planningItemId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.life', 'group.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/bookings', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listBookings(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/bookings', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.bookingSchema, req.body);
    const result = await runCommand({
      operationCode: 'BOOKING_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'BOOKING',
      execute: async (client, b) => {
        const r = await groupCollab.createBookingCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.bookingSchema>
        );
        return { result: r, resourceId: r.bookingId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/polls', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listPolls(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/polls', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(collaborationService.pollSchema, req.body);
    const result = await runCommand({
      operationCode: 'POLL_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'POLL',
      execute: async (client, b) => {
        const r = await groupCollab.createPollCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof collaborationService.pollSchema>
        );
        return { result: r, resourceId: r.pollId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'business.pulse', 'business.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/polls/:pollId', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.getPollCommand(client, ctx, param(req.params.pollId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/polls/:pollId/votes', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(collaborationService.votePollSchema, req.body);
    const result = await runCommand({
      operationCode: 'POLL_VOTE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'POLL',
      execute: async (client, b) => {
        const r = await groupCollab.votePollCommand(
          client,
          ctx,
          param(req.params.pollId),
          b as z.infer<typeof collaborationService.votePollSchema>
        );
        return { result: r, resourceId: r.pollId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'business.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/polls/:pollId/close', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const result = await runCommand({
      operationCode: 'POLL_CLOSE',
      idempotencyKey: req.idempotencyKey!,
      body: {},
      ctx,
      resourceType: 'POLL',
      execute: async (client) => {
        const r = await groupCollab.closePollCommand(client, ctx, param(req.params.pollId));
        return { result: r, resourceId: r.pollId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'business.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(200).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/updates', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listUpdates(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/updates', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.updateSchema, req.body);
    const result = await runCommand({
      operationCode: 'UPDATE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'UPDATE',
      execute: async (client, b) => {
        const r = await groupCollab.postUpdateCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.updateSchema>
        );
        return { result: r, resourceId: r.updateId };
      },
    });
    const hints = ['group.activity', 'group.pulse', 'group.moments'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/purchase-items', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listPurchaseItems(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/purchase-items', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.purchaseItemSchema, req.body);
    const result = await runCommand({
      operationCode: 'PURCHASE_ITEM_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'PURCHASE_ITEM',
      execute: async (client, b) => {
        const r = await groupCollab.addPurchaseItemCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.purchaseItemSchema>
        );
        return { result: r, resourceId: r.purchaseItemId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/delivery-handovers', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listDeliveryHandovers(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/delivery-handovers', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.deliveryHandoverSchema, req.body);
    const result = await runCommand({
      operationCode: 'DELIVERY_HANDOVER_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'DELIVERY_HANDOVER',
      execute: async (client, b) => {
        const r = await groupCollab.addDeliveryHandoverCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.deliveryHandoverSchema>
        );
        return { result: r, resourceId: r.deliveryHandoverId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/ownership-records', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listOwnershipRecords(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/ownership-records', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.ownershipRecordSchema, req.body);
    const result = await runCommand({
      operationCode: 'OWNERSHIP_RECORD_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'OWNERSHIP_RECORD',
      execute: async (client, b) => {
        const r = await groupCollab.addOwnershipRecordCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.ownershipRecordSchema>
        );
        return { result: r, resourceId: r.ownershipRecordId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/residents', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listResidents(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/residents', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.residentSchema, req.body);
    const result = await runCommand({
      operationCode: 'RESIDENT_MANAGE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'RESIDENT',
      execute: async (client, b) => {
        const r = await groupCollab.addResidentCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.residentSchema>
        );
        return { result: r, resourceId: r.residentId };
      },
    });
    const hints = ['group.activity', 'group.life'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/memories', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listMemories(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/memories', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.memorySchema, req.body);
    const result = await runCommand({
      operationCode: 'MEMORY_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MEMORY',
      execute: async (client, b) => {
        const r = await groupCollab.createMemoryCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.memorySchema>
        );
        return { result: r, resourceId: r.memoryId };
      },
    });
    const hints = ['group.activity', 'group.memory'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/moments/:momentId/memories/:memoryId/media', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      memoryAttachmentService.listMemoryAttachments(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.memoryId)
      )
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/memories/:memoryId/media', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(memoryAttachmentService.attachMemoryMediaSchema, req.body);
    const data = await withDb((client) =>
      memoryAttachmentService.attachMemoryMedia(
        client,
        ctx,
        param(req.params.momentId),
        param(req.params.memoryId),
        body.uploadId
      )
    );
    publishProjectionUpdated(ctx.userId, ['GROUP_MEMORY'], ctx.correlationId);
    res.status(201).json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/vendors', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listVendors(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/vendors', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.vendorSchema, req.body);
    const result = await runCommand({
      operationCode: 'GROUP_VENDOR_MANAGE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'GROUP_VENDOR',
      execute: async (client, b) => {
        const r = await groupCollab.createGroupVendorCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.vendorSchema>
        );
        return { result: r, resourceId: r.groupVendorId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/attendance', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listAttendance(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/attendance', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.attendanceSchema, req.body);
    const result = await runCommand({
      operationCode: 'ATTENDANCE_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'ATTENDANCE',
      execute: async (client, b) => {
        const r = await groupCollab.recordAttendanceCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.attendanceSchema>
        );
        return { result: r, resourceId: r.attendanceId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/living-rules', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listLivingRules(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/living-rules', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.livingRuleSchema, req.body);
    const result = await runCommand({
      operationCode: 'RULE_MANAGE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'LIVING_RULE',
      execute: async (client, b) => {
        const r = await groupCollab.createLivingRuleCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.livingRuleSchema>
        );
        return { result: r, resourceId: r.livingRuleId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/shared-assets', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => groupCollab.listSharedAssets(client, ctx, param(req.params.momentId)));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/shared-assets', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.sharedAssetSchema, req.body);
    const result = await runCommand({
      operationCode: 'ASSET_MANAGE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'SHARED_ASSET',
      execute: async (client, b) => {
        const r = await groupCollab.createSharedAssetCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.sharedAssetSchema>
        );
        return { result: r, resourceId: r.sharedAssetId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/group/moments/:momentId/maintenance-records', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      groupCollab.listMaintenanceRecords(client, ctx, param(req.params.momentId))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/maintenance-records', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(groupCollab.maintenanceSchema, req.body);
    const result = await runCommand({
      operationCode: 'MAINTENANCE_CREATE',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'MAINTENANCE_RECORD',
      execute: async (client, b) => {
        const r = await groupCollab.createMaintenanceRecordCommand(
          client,
          ctx,
          param(req.params.momentId),
          b as z.infer<typeof groupCollab.maintenanceSchema>
        );
        return { result: r, resourceId: r.maintenanceRecordId };
      },
    });
    const hints = ['group.activity', 'group.pulse'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(commandEnvelope(result, ctx.correlationId, { projectionHints: toProjectionHints([...hints], 'refresh') }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/moments/:momentId/settlements', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const momentId = param(req.params.momentId);
    // SETTLEMENT_RECORD mapped for expense-capable GROUP types via V047 (GX-1).
    const capabilityOk = await withDb(async (client) => {
      await groupMembership.assertGroupMember(client, ctx, momentId);
      const mt = await client.query<{ moment_type_id: string }>(
        `SELECT moment_type_id FROM core.moment WHERE moment_id = $1`,
        [momentId]
      );
      if (!mt.rows[0]) {
        throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
      }
      return resolveCapabilityForMomentType(client, mt.rows[0].moment_type_id, 'SETTLEMENT_RECORD');
    });
    if (!capabilityOk) {
      res.status(501).json({
        error: {
          code: 'API_GAP',
          message:
            'SETTLEMENT_RECORD is not mapped on moment_type_capability for this moment type. Settlement write deferred.',
          correlationId: ctx.correlationId,
        },
      });
      return;
    }
    const body = parseBody(groupExpenseService.createSettlementSchema, req.body);
    const result = await runCommand({
      operationCode: 'SETTLEMENT_RECORD',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'SETTLEMENT',
      execute: async (client, b) => {
        const r = await groupExpenseService.createSettlement(
          client,
          ctx,
          momentId,
          b as groupExpenseService.CreateSettlementInput
        );
        return { result: r, resourceId: r.settlementId };
      },
    });
    const hints = ['group.pulse', 'group.finance', 'group.activity'] as const;
    publishProjectionUpdated(ctx.userId, hints.map((h) => h.toUpperCase().replace('.', '_')), ctx.correlationId);
    res.status(201).json(
      commandEnvelope(result, ctx.correlationId, {
        projectionHints: toProjectionHints([...hints], 'refresh'),
      })
    );
  } catch (e) {
    next(e);
  }
});

// --- S2A Group invites (mint / preview / redeem) ---
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
      const hints = ['GROUP_MOMENTS', 'GROUP_ACTIVITY', 'GROUP_PARTICIPANTS'];
      const targets = new Set<string>([ctx.userId, ...(result.notifyUserIds ?? [])]);
      for (const uid of targets) {
        publishProjectionUpdated(uid, hints, ctx.correlationId);
      }
    }
    res.status(200).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Company invites (mint / preview / redeem) ---
v1Router.post('/company/invites', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = parseBody(companyInviteService.mintCompanyInviteSchema, req.body);
    const result = await runCommand({
      operationCode: 'COMPANY_INVITE_MINT',
      idempotencyKey: req.idempotencyKey!,
      body,
      ctx,
      resourceType: 'COMPANY_INVITE',
      execute: async (client, b) => {
        const r = await companyInviteService.mintCompanyInvite(
          client,
          ctx,
          b as companyInviteService.MintCompanyInviteInput
        );
        return { result: r, resourceId: r.inviteId };
      },
    });
    res.status(201).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

v1Router.get('/company/invites/:code', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) =>
      companyInviteService.getCompanyInviteByCode(client, ctx, param(req.params.code))
    );
    res.json(projectionEnvelope(data, ctx.correlationId, { status: data.status }));
  } catch (e) {
    next(e);
  }
});

v1Router.post('/company/invites/:code/redeem', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const code = param(req.params.code);
    const result = await runCommand({
      operationCode: 'COMPANY_INVITE_REDEEM',
      idempotencyKey: req.idempotencyKey!,
      body: { code },
      ctx,
      resourceType: 'COMPANY_INVITE',
      execute: async (client) => {
        const r = await companyInviteService.redeemCompanyInvite(client, ctx, code);
        return { result: r, resourceId: r.inviteId };
      },
    });
    publishProjectionUpdated(ctx.userId, ['BUSINESS_MOMENTS'], ctx.correlationId);
    res.status(200).json(commandEnvelope(result, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});

// --- Analytics (S8) ---
v1Router.get('/analytics/metrics', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const scopeType = String(req.query.scopeType ?? 'USER');
    const scopeId = String(req.query.scopeId ?? ctx.userId);
    const data = await withDb((client) => analyticsEngine.listMetricsForScope(client, scopeType, scopeId));
    res.json(
      projectionEnvelope(
        { items: data, meta: { contractVersion: '1' } },
        ctx.correlationId,
        { status: 'OK' },
      ),
    );
  } catch (e) {
    next(e);
  }
});

v1Router.get('/analytics/insights', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const scopeType = String(req.query.scopeType ?? 'USER');
    const scopeId = String(req.query.scopeId ?? ctx.userId);
    const data = await withDb((client) =>
      analyticsEngine.listInsightsForScope(client, scopeType, scopeId, ctx.userId),
    );
    const status = data.length ? 'OK' : 'EMPTY';
    res.json(
      projectionEnvelope(
        {
          items: data,
          meta: {
            contractVersion: '1',
            status: data.length ? data[0]?.status ?? 'EMPTY' : 'EMPTY',
            computedAt: data[0]?.computedAt ?? null,
            dataThrough: data[0]?.dataThrough ?? null,
            version: data[0]?.version ?? null,
          },
        },
        ctx.correlationId,
        { status },
      ),
    );
  } catch (e) {
    next(e);
  }
});

v1Router.post('/analytics/refresh', requireIdempotencyKey, async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const body = z
      .object({
        context: z.enum(['PERSONAL', 'GROUP', 'BUSINESS']),
        companyId: z.string().uuid().optional().nullable(),
        momentId: z.string().uuid().optional().nullable(),
      })
      .parse(req.body ?? {});
    const result = await withDb((client) =>
      analyticsEngine.runAnalyticsJob(client, {
        userId: ctx.userId,
        context: body.context,
        companyId: body.companyId,
        momentId: body.momentId,
        correlationId: ctx.correlationId,
        triggerEventId: null,
      }),
    );
    res.status(202).json(commandEnvelope(result, ctx.correlationId));
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

// --- Circle (deferred read) ---
v1Router.get('/life360', async (req, res, next) => {
  try {
    const ctx = req.requestContext!;
    const data = await withDb((client) => projectionService.getLife360(client, ctx.userId));
    res.json(projectionEnvelope(data, ctx.correlationId, { status: 'OK' }));
  } catch (e) {
    next(e);
  }
});
