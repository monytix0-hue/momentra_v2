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
    created_at: Date;
  }>(
    `SELECT life_system_setup_id, system_code, title, moment_id, status, preferences, created_at
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
    createdAt: r.created_at.toISOString(),
  }));
}
