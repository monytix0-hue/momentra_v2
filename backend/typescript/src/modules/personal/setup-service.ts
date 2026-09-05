import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import * as momentService from '../moment/service';

export const PERSONAL_SETUP_SYSTEM_CODES = [
  'LIFE_OPERATIONS',
  'FUTURE_BUILDING',
  'LIFESTYLE',
  'RELATIONSHIPS',
] as const;

export type PersonalSetupSystemCode = (typeof PERSONAL_SETUP_SYSTEM_CODES)[number];

export interface PersonalSetupCatalogItem {
  systemCode: PersonalSetupSystemCode;
  title: string;
  subtitle: string;
  figmaNodeId: string;
  defaultMomentTypeCode: string;
  activateLabel: string;
  defaultTitle: string;
  defaultPreferences: Record<string, unknown>;
}

/** Figma-aligned Personal Create setups (scroll content nodes). */
export const PERSONAL_SETUP_CATALOG: PersonalSetupCatalogItem[] = [
  {
    systemCode: 'LIFE_OPERATIONS',
    title: 'Life Operations Setup',
    subtitle: 'Align your rhythm and what restores you.',
    figmaNodeId: '353:6809',
    defaultMomentTypeCode: 'LIFE_RHYTHM',
    activateLabel: 'Activate Life Operations →',
    defaultTitle: 'My life operations rhythm',
    defaultPreferences: {
      lifeFocus: 'Daily balance',
      currentRhythm: 'Busy but manageable',
      primaryNeed: 'More breathing room',
      healthEnergy: 'Selected',
      timeBalance: 'Selected',
      shapesFocus: 'Daily balance',
      shapesRhythm: 'Busy but manageable',
      mainPressure: 'Too many commitments',
      recoveryWindow: 'Weekends',
      checkInRhythm: 'Weekly',
      helpfulSupport: 'Clear routines',
      recoveryStyle: 'Quiet time',
      habit: 'Morning routine',
      habit2: '',
      currentEnergy: 'Steady',
      reflectWeekly: true,
      stressCheckIn: 'Enabled',
      recoveryCheckIn: 'Enabled',
      reviewCadence: 'Every week',
      profile: 'STRUCTURE SEEKER',
    },
  },
  {
    systemCode: 'FUTURE_BUILDING',
    title: 'Future Building Setup',
    subtitle: 'Define the direction, values and momentum for your next chapter.',
    figmaNodeId: '353:6905',
    defaultMomentTypeCode: 'FUTURE_GOAL',
    activateLabel: 'Activate Future Building →',
    defaultTitle: 'My future building',
    defaultPreferences: {
      building: 'Career growth',
      today: 'Making progress',
      primaryValue: 'Freedom',
      valueGrowth: 'Selected',
      valueSecurity: 'Selected',
      futureFeel: 'Hopeful',
      focusHorizon: '12 months',
      progressRhythm: 'Weekly',
      mainFriction: 'Lack of time',
      supportStyle: 'Daily progress',
      momentumDriver: 'Daily progress',
      habit2: '',
      remindWeekly: true,
      learningCheckIn: 'Enabled',
      focusTimeCheckIn: 'Enabled',
      reviewCadence: 'Every week',
      profile: 'Future Builder',
    },
  },
  {
    systemCode: 'LIFESTYLE',
    title: 'Lifestyle Setup',
    subtitle: 'Shape the habits, spaces and priorities that make everyday life yours.',
    figmaNodeId: '353:7075',
    defaultMomentTypeCode: 'LIFESTYLE',
    activateLabel: 'Activate My Lifestyle →',
    defaultTitle: 'My intentional lifestyle',
    defaultPreferences: {
      vision: 'Balanced & energized',
      current: 'Good, with room to grow',
      primaryPriority: 'Health & energy',
      workLifeBalance: 'Selected',
      homeEnvironment: 'Selected',
      healthEnergy: 'Strong and consistent',
      socialRhythm: 'A few times a week',
      homeRhythm: 'Calm & organized',
      topPriority: 'Health & energy',
      neglectedArea: 'Rest & recovery',
      habit: 'Movement routine',
      habit2: '',
      desiredFeeling: 'Balanced',
      remindWeekly: true,
      energyCheckIn: 'Enabled',
      balanceCheckIn: 'Enabled',
      reviewCadence: 'Every week',
      profile: 'Lifestyle Curator',
    },
  },
  {
    systemCode: 'RELATIONSHIPS',
    title: 'Relationships Setup',
    subtitle: 'Clarify where you want to invest, repair and create meaningful connection.',
    figmaNodeId: '353:7217',
    defaultMomentTypeCode: 'RELATIONSHIP_CONNECTION',
    activateLabel: 'Activate My Relationships →',
    defaultTitle: 'My relationships',
    defaultPreferences: {
      relationshipFocus: 'Deeper connection',
      current: 'Connected, but busy',
      primaryCircle: 'Family',
      partnerFamily: 'Selected',
      friendsCommunity: 'Selected',
      timeTogether: 'Meaningful moments',
      reachOutRhythm: 'Every week',
      communicationStyle: 'Thoughtful check-ins',
      strongestConnection: 'Family',
      needsInvestment: 'Friends',
      ritual: 'Weekly check-in',
      habit2: '',
      desiredFeeling: 'Close & supported',
      remindWeekly: true,
      connectionCheckIn: 'Enabled',
      reachOutReminder: 'Enabled',
      reviewCadence: 'Every week',
      profile: 'Connection Builder',
    },
  },
];

export const activateSetupSchema = z
  .object({
    title: z.string().min(1).max(500).optional(),
    momentTypeCode: z.string().min(1).max(100).optional(),
    preferences: z.record(z.string(), z.unknown()).optional(),
    timezone: z.string().default('UTC'),
  })
  .strict();

export type ActivateSetupInput = z.infer<typeof activateSetupSchema>;

export function getSetupCatalog(): PersonalSetupCatalogItem[] {
  return PERSONAL_SETUP_CATALOG;
}

export function getSetupByCode(code: string): PersonalSetupCatalogItem | undefined {
  return PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === code);
}

export async function activatePersonalSetup(
  client: PoolClient,
  ctx: RequestContext,
  systemCode: string,
  body: ActivateSetupInput
): Promise<{
  setupId: string;
  systemCode: string;
  momentId: string;
  momentTypeCode: string;
  title: string;
  status: string;
  version: number;
}> {
  const catalog = getSetupByCode(systemCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown personal setup: ${systemCode}`, 400);
  }

  const momentTypeCode = body.momentTypeCode ?? catalog.defaultMomentTypeCode;
  const title = body.title ?? catalog.defaultTitle;
  const preferences = body.preferences ?? catalog.defaultPreferences;

  const moment = await momentService.createMoment(client, ctx, {
    domainCode: 'PERSONAL',
    momentTypeCode,
    title,
    description: catalog.subtitle,
    timezone: body.timezone ?? 'UTC',
  });

  const inserted = await client.query<{ life_system_setup_id: string }>(
    `INSERT INTO personal.life_system_setup (
       user_id, moment_id, system_code, title, preferences, status, version
     ) VALUES ($1, $2, $3, $4, $5::jsonb, 'ACTIVE', 1)
     RETURNING life_system_setup_id`,
    [ctx.userId, moment.momentId, catalog.systemCode, title, JSON.stringify(preferences)]
  );
  const setupId = inserted.rows[0]!.life_system_setup_id;

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PersonalSetupActivated',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_SYSTEM_SETUP',
    aggregateId: setupId,
    scopeType: 'MOMENT',
    scopeId: moment.momentId,
    payload: {
      setupId,
      systemCode: catalog.systemCode,
      momentId: moment.momentId,
      momentTypeCode,
      title,
    },
  });
  await insertAudit(
    client,
    ctx,
    'PERSONAL_SETUP_ACTIVATE',
    'LIFE_SYSTEM_SETUP',
    setupId,
    domainEventId,
    {
      systemCode: catalog.systemCode,
      momentId: moment.momentId,
      momentTypeCode,
      title,
    }
  );

  return {
    setupId,
    systemCode: catalog.systemCode,
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
    systemCode: string;
    title: string;
    momentId: string;
    status: string;
    preferences: Record<string, unknown>;
    version: number;
    createdAt: string;
  }>
> {
  const rows = await client.query<{
    life_system_setup_id: string;
    system_code: string;
    title: string;
    moment_id: string;
    status: string;
    preferences: Record<string, unknown>;
    version: number;
    created_at: Date;
  }>(
    `SELECT life_system_setup_id, system_code, title, moment_id, status, preferences, version, created_at
     FROM personal.life_system_setup
     WHERE user_id = $1
     ORDER BY created_at DESC
     LIMIT $2`,
    [ctx.userId, limit]
  );
  return rows.rows.map((r) => ({
    setupId: r.life_system_setup_id,
    systemCode: r.system_code,
    title: r.title,
    momentId: r.moment_id,
    status: r.status,
    preferences: r.preferences ?? {},
    version: r.version,
    createdAt: r.created_at.toISOString(),
  }));
}

export const patchSetupSchema = z
  .object({
    expectedVersion: z.number().int().positive(),
    title: z.string().min(1).max(500).optional(),
    preferences: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

export type PatchSetupInput = z.infer<typeof patchSetupSchema>;

export async function patchPersonalSetup(
  client: PoolClient,
  ctx: RequestContext,
  setupId: string,
  body: PatchSetupInput
): Promise<{
  setupId: string;
  systemCode: string;
  momentId: string;
  title: string;
  status: string;
  version: number;
  preferences: Record<string, unknown>;
}> {
  const existing = await client.query<{
    life_system_setup_id: string;
    system_code: string;
    moment_id: string;
    title: string;
    status: string;
    version: number;
    preferences: Record<string, unknown>;
    user_id: string;
  }>(
    `SELECT life_system_setup_id, system_code, moment_id, title, status, version, preferences, user_id
     FROM personal.life_system_setup
     WHERE life_system_setup_id = $1`,
    [setupId]
  );
  const row = existing.rows[0];
  if (!row || row.user_id !== ctx.userId) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal setup not found.', 404);
  }
  if (row.version !== body.expectedVersion) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Setup version conflict.', 409);
  }

  const systemCode = row.system_code as PersonalSetupSystemCode;
  const nextTitle = body.title ?? row.title;
  const catalog = getSetupByCode(systemCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown personal setup: ${systemCode}`, 400);
  }
  let nextPreferences: Record<string, unknown> = row.preferences ?? {};
  if (body.preferences !== undefined) {
    const allowed = new Set(Object.keys(catalog.defaultPreferences));
    const booleanKeys = new Set(['reflectWeekly', 'remindWeekly']);
    for (const key of Object.keys(body.preferences)) {
      if (!allowed.has(key)) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown preference key: ${key}`, 400);
      }
      const value = body.preferences[key];
      if (booleanKeys.has(key)) {
        if (typeof value !== 'boolean') {
          throw new AppError(ErrorCode.VALIDATION_FAILED, `preferences.${key} must be a boolean.`, 400);
        }
      } else if (typeof value !== 'string') {
        throw new AppError(ErrorCode.VALIDATION_FAILED, `preferences.${key} must be a string.`, 400);
      }
    }
    nextPreferences = { ...catalog.defaultPreferences, ...body.preferences };
  }

  const updated = await client.query<{
    life_system_setup_id: string;
    system_code: string;
    moment_id: string;
    title: string;
    status: string;
    version: number;
    preferences: Record<string, unknown>;
  }>(
    `UPDATE personal.life_system_setup
     SET title = $2,
         preferences = $3::jsonb,
         version = version + 1,
         updated_at = now()
     WHERE life_system_setup_id = $1 AND version = $4
     RETURNING life_system_setup_id, system_code, moment_id, title, status, version, preferences`,
    [setupId, nextTitle, JSON.stringify(nextPreferences), body.expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Setup version conflict.', 409);
  }

  const result = updated.rows[0];
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PersonalSetupUpdated',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_SYSTEM_SETUP',
    aggregateId: result.life_system_setup_id,
    scopeType: 'MOMENT',
    scopeId: result.moment_id,
    payload: {
      setupId: result.life_system_setup_id,
      systemCode: result.system_code,
      momentId: result.moment_id,
      title: result.title,
    },
  });
  await insertAudit(client, ctx, 'PERSONAL_SETUP_UPDATE', 'LIFE_SYSTEM_SETUP', result.life_system_setup_id, domainEventId, {
    systemCode: result.system_code,
    momentId: result.moment_id,
    title: result.title,
  });

  return {
    setupId: result.life_system_setup_id,
    systemCode: result.system_code,
    momentId: result.moment_id,
    title: result.title,
    status: result.status,
    version: result.version,
    preferences: result.preferences ?? {},
  };
}
