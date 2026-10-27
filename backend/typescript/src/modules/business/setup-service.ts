import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import * as momentService from '../moment/service';
import {
  OPERATING_FAMILIES,
  type OperatingFamilyCode,
} from './operating-catalog';
import {
  assertOperatingActivate,
  insertReportingCadenceActivity,
  momentTypeForActivate,
  normalizeOperatingPreferences,
} from './operating-service';

export const BUSINESS_SETUP_FAMILY_CODES = [
  'MARKETING_CAMPAIGNS',
  'TRADING_DISTRIBUTION',
  'RETAIL_CONSUMER',
  'MANUFACTURING_PRODUCTION',
] as const;

export type BusinessSetupFamilyCode = (typeof BUSINESS_SETUP_FAMILY_CODES)[number];

export interface BusinessSetupCatalogItem {
  familyCode: BusinessSetupFamilyCode;
  title: string;
  subtitle: string;
  figmaNodeId: string;
  defaultMomentTypeCode: string;
  activateLabel: string;
  defaultTitle: string;
  defaultPreferences: Record<string, unknown>;
}

export const BUSINESS_SETUP_CATALOG: BusinessSetupCatalogItem[] = OPERATING_FAMILIES.map((family) => ({
  familyCode: family.familyCode,
  title: family.title,
  subtitle: family.subtitle,
  figmaNodeId: family.figmaNodeId,
  defaultMomentTypeCode: family.subtypes[0]!.code,
  activateLabel: family.activateLabel,
  defaultTitle: family.title,
  defaultPreferences: {
    subtype: family.subtypes[0]!.code,
    identityName: '',
    partyName: '',
    description: '',
    currency: 'INR',
    startDate: '',
    participants: [],
    counterparties: [],
    channels: [],
    monthlyBudget: '',
    monthlyTarget: '',
    targetMetric: family.subtypes[0]!.targetMetrics[0],
    subtypes: family.subtypes.map((s) => ({
      code: s.code,
      label: s.label,
      partyLabel: s.partyLabel,
      targetMetrics: s.targetMetrics,
      requiresChannel: s.requiresChannel,
      quickAdds: s.quickAdds,
    })),
  },
}));

export const activateBusinessSetupSchema = z
  .object({
    companyId: z.string().uuid(),
    title: z.string().min(1).max(500).optional(),
    momentTypeCode: z.string().min(1).max(100).optional(),
    preferences: z.record(z.string(), z.unknown()).optional(),
    timezone: z.string().default('UTC'),
  })
  .strict();

export type ActivateBusinessSetupInput = z.infer<typeof activateBusinessSetupSchema>;

export function getSetupCatalog(): BusinessSetupCatalogItem[] {
  return BUSINESS_SETUP_CATALOG;
}

export function getSetupByCode(code: string): BusinessSetupCatalogItem | undefined {
  return BUSINESS_SETUP_CATALOG.find((s) => s.familyCode === code);
}

export async function activateBusinessSetup(
  client: PoolClient,
  ctx: RequestContext,
  familyCode: string,
  body: ActivateBusinessSetupInput
): Promise<{
  setupId: string;
  familyCode: string;
  momentId: string;
  momentTypeCode: string;
  title: string;
  status: string;
  version: number;
}> {
  assertOperatingActivate(familyCode);
  const catalog = getSetupByCode(familyCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown business setup: ${familyCode}`, 400);
  }

  let normalized;
  try {
    normalized = normalizeOperatingPreferences(familyCode as OperatingFamilyCode, body.preferences, {
      userId: ctx.userId,
      displayName: 'Owner',
    });
  } catch (err) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      err instanceof Error ? err.message : 'Invalid operating setup.',
      400
    );
  }

  const membership = await client.query(
    `SELECT 1 FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [body.companyId, ctx.userId]
  );
  if (!membership.rowCount) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Active company membership required.', 403);
  }

  const momentTypeCode = momentTypeForActivate(
    catalog.familyCode,
    body.momentTypeCode ?? normalized.subtype,
    normalized.subtype
  );
  const title = body.title ?? normalized.identityName;
  const preferences = normalized as unknown as Record<string, unknown>;

  const moment = await momentService.createMoment(client, ctx, {
    domainCode: 'BUSINESS',
    momentTypeCode,
    title,
    description: catalog.subtitle,
    timezone: body.timezone ?? 'UTC',
    companyId: body.companyId,
  });

  const inserted = await client.query<{ business_system_setup_id: string }>(
    `INSERT INTO business.business_system_setup (
       company_id, user_id, moment_id, family_code, title, preferences, status, version
     ) VALUES ($1, $2, $3, $4, $5, $6::jsonb, 'ACTIVE', 1)
     RETURNING business_system_setup_id`,
    [body.companyId, ctx.userId, moment.momentId, catalog.familyCode, title, JSON.stringify(preferences)]
  );
  const setupId = inserted.rows[0]!.business_system_setup_id;
  if (catalog.familyCode === 'MARKETING_CAMPAIGNS') {
    await insertReportingCadenceActivity(client, ctx, moment.momentId, body.companyId, normalized);
  }

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessSetupActivated',
    domainCode: 'BUSINESS',
    aggregateType: 'BUSINESS_SYSTEM_SETUP',
    aggregateId: setupId,
    scopeType: 'COMPANY',
    scopeId: body.companyId,
    payload: {
      setupId,
      familyCode: catalog.familyCode,
      momentId: moment.momentId,
      momentTypeCode,
      title,
      companyId: body.companyId,
    },
  });
  await insertAudit(
    client,
    ctx,
    'BUSINESS_SETUP_ACTIVATE',
    'BUSINESS_SYSTEM_SETUP',
    setupId,
    domainEventId,
    {
      familyCode: catalog.familyCode,
      momentId: moment.momentId,
      momentTypeCode,
      title,
      companyId: body.companyId,
    }
  );

  const { refreshBusinessProjectionsAfterWrite } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(
    client,
    body.companyId,
    moment.momentId,
    catalog.familyCode
  );

  return {
    setupId,
    familyCode: catalog.familyCode,
    momentId: moment.momentId,
    momentTypeCode,
    title,
    status: moment.status,
    version: moment.version,
  };
}

export async function listUserSetups(
  client: PoolClient,
  ctx: RequestContext,
  limit = 20
): Promise<
  Array<{
    setupId: string;
    familyCode: string;
    title: string;
    momentId: string;
    companyId: string;
    status: string;
    preferences: Record<string, unknown>;
    createdAt: string;
  }>
> {
  const rows = await client.query<{
    business_system_setup_id: string;
    family_code: string;
    title: string;
    moment_id: string;
    company_id: string;
    status: string;
    preferences: Record<string, unknown>;
    created_at: Date;
  }>(
    `SELECT business_system_setup_id, family_code, title, moment_id, company_id, status, preferences, created_at
     FROM business.business_system_setup
     WHERE user_id = $1
     ORDER BY created_at DESC
     LIMIT $2`,
    [ctx.userId, limit]
  );
  return rows.rows.map((r) => ({
    setupId: r.business_system_setup_id,
    familyCode: r.family_code,
    title: r.title,
    momentId: r.moment_id,
    companyId: r.company_id,
    status: r.status,
    preferences: r.preferences ?? {},
    createdAt: r.created_at.toISOString(),
  }));
}
