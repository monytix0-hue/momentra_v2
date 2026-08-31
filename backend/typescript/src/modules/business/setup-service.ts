import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import * as momentService from '../moment/service';

export const BUSINESS_SETUP_FAMILY_CODES = [
  'TEAM_OPERATIONS',
  'BUSINESS_RUNWAY',
  'BUSINESS_OPERATIONS',
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

export const BUSINESS_SETUP_CATALOG: BusinessSetupCatalogItem[] = [
  {
    familyCode: 'TEAM_OPERATIONS',
    title: 'Set up Team Operations',
    subtitle: "Configure your team's operating system in one go.",
    figmaNodeId: '692:34736',
    defaultMomentTypeCode: 'TEAM_OPERATIONS',
    activateLabel: 'Activate Team Operations',
    defaultTitle: 'Team Operations',
    defaultPreferences: {
      teamName: 'Growth & Product',
      size: '11-25 people',
      workMode: 'Hybrid',
      country: 'India',
      currency: 'INR',
      timezone: 'IST (UTC+5:30)',
      language: 'English',
      financialYear: 'Apr - Mar',
      taxSystem: 'GST',
      coordination: 'Structured',
      reviewCycle: 'Weekly',
      monitoring: 'Balanced',
      spendingApproval: 'Required',
      approvalThreshold: '₹50,000',
    },
  },
  {
    familyCode: 'BUSINESS_RUNWAY',
    title: 'Set up Business Runway',
    subtitle: 'Configure your financial operating system on-the-go.',
    figmaNodeId: '692:36690',
    defaultMomentTypeCode: 'BUSINESS_RUNWAY',
    activateLabel: 'Activate Business Runway',
    defaultTitle: 'Business Runway',
    defaultPreferences: {
      businessStage: 'Scaling',
      goalHorizon: '18-months goal',
      multiCurrency: true,
      availableCash: '₹ 1,80,00,000',
      monthlySpending: '₹ 12,50,000',
      revenueStage: 'Growing',
      monthlyRevenue: '₹ 8,08,000',
      revenueModel: 'Recurring',
      warningThreshold: '6 months',
      fundingSource: 'Bootstrapped + revenue',
    },
  },
  {
    familyCode: 'BUSINESS_OPERATIONS',
    title: 'Set up Business Operations',
    subtitle: 'Configure operational capacity, monitoring and approvals.',
    figmaNodeId: '692:37188',
    defaultMomentTypeCode: 'BUSINESS_OPERATIONS',
    activateLabel: 'Activate Business Operations',
    defaultTitle: 'Business Operations',
    defaultPreferences: {
      coreOps: 'Growth & Product',
      scope: 'Company-wide',
      model: 'Centralized',
      cadence: 'Monthly',
      monthlyBudget: '₹35,00,000',
      allocationMethod: 'Category-based',
      monitoringStyle: 'Proactive',
      approvalModel: 'Threshold-based',
      approvalAlarm: '₹5,00,000',
    },
  },
];

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

async function insertFamilyContext(
  client: PoolClient,
  momentId: string,
  familyCode: BusinessSetupFamilyCode,
  title: string
): Promise<void> {
  if (familyCode === 'TEAM_OPERATIONS') {
    await client.query(
      `INSERT INTO business.team_operations_context (moment_id, business_family, objective_text, status)
       VALUES ($1, 'TEAM_OPERATIONS', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  } else if (familyCode === 'BUSINESS_RUNWAY') {
    await client.query(
      `INSERT INTO business.business_runway_context (moment_id, business_family, scenario_name, status)
       VALUES ($1, 'BUSINESS_RUNWAY', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  } else if (familyCode === 'BUSINESS_OPERATIONS') {
    await client.query(
      `INSERT INTO business.business_operations_context (moment_id, business_family, objective_text, status)
       VALUES ($1, 'BUSINESS_OPERATIONS', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  }
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
  const catalog = getSetupByCode(familyCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown business setup: ${familyCode}`, 400);
  }

  const membership = await client.query(
    `SELECT 1 FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [body.companyId, ctx.userId]
  );
  if (!membership.rowCount) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Active company membership required.', 403);
  }

  const momentTypeCode = body.momentTypeCode ?? catalog.defaultMomentTypeCode;
  const title = body.title ?? catalog.defaultTitle;
  const preferences = body.preferences ?? catalog.defaultPreferences;

  const moment = await momentService.createMoment(client, ctx, {
    domainCode: 'BUSINESS',
    momentTypeCode,
    title,
    description: catalog.subtitle,
    timezone: body.timezone ?? 'UTC',
    companyId: body.companyId,
  });

  await insertFamilyContext(client, moment.momentId, catalog.familyCode, title);

  const inserted = await client.query<{ business_system_setup_id: string }>(
    `INSERT INTO business.business_system_setup (
       company_id, user_id, moment_id, family_code, title, preferences, status, version
     ) VALUES ($1, $2, $3, $4, $5, $6::jsonb, 'ACTIVE', 1)
     RETURNING business_system_setup_id`,
    [body.companyId, ctx.userId, moment.momentId, catalog.familyCode, title, JSON.stringify(preferences)]
  );
  const setupId = inserted.rows[0]!.business_system_setup_id;

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
