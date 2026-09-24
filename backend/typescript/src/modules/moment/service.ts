import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed, resolveCapabilityForMomentType, assertMomentLifecycleLeader } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';
import {
  businessSetupBlockSchema,
  groupSetupBlockSchema,
  groupSetupUpdateBlockSchema,
  personalSetupBlockSchema,
  validateAndMergeBusinessPreferences,
  validateAndMergePersonalPreferences,
  normalizeGroupSetupBudgets,
} from './setup-preferences';
import { seedGroupBudget } from '../finance/group-budget';
import {
  insertBusinessFamilyContext,
  insertBusinessSetupRow,
  insertPersonalSetupRow,
} from './setup-persistence';
import { bindInviteToMoment } from '../collaboration/invite-service';
import { emitLeanBusinessEvent } from '../analytics/lean-events';

const participantRoleSchema = z.enum([
  'ORGANIZER',
  'CO_ORGANIZER',
  'PARTICIPANT',
  'RESIDENT',
  'CONTRIBUTOR',
  'OBSERVER',
]);

export const createMomentParticipantSchema = z
  .object({
    userId: z.string().uuid().optional(),
    displayName: z.string().min(1).max(200).optional(),
    roleCode: participantRoleSchema.default('PARTICIPANT'),
    email: z.string().max(200).nullish(),
    phone: z.string().max(40).nullish(),
  })
  .strict()
  .superRefine((row, ctx) => {
    if (!row.userId && !row.displayName) {
      ctx.addIssue({
        code: 'custom',
        message: 'userId or displayName is required.',
        path: ['displayName'],
      });
    }
  });

export const createMomentSchema = z
  .object({
    domainCode: z.enum(['PERSONAL', 'GROUP', 'BUSINESS']).default('PERSONAL'),
    momentTypeCode: z.string().min(1),
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    startAt: z.string().datetime().optional(),
    endAt: z.string().datetime().optional(),
    timezone: z.string().default('UTC'),
    customTypeLabel: z.string().max(500).optional(),
    companyId: z.string().uuid().optional(),
    teamId: z.string().uuid().optional(),
    participants: z.array(createMomentParticipantSchema).max(50).optional(),
    inviteCode: z.string().min(8).max(64).nullish(),
    expectedVersion: z.number().int().positive().optional(),
    /** DRAFT = save setup without activating; ACTIVE = default when omitted. */
    status: z.enum(['DRAFT', 'ACTIVE']).optional(),
    personalSetup: personalSetupBlockSchema.optional(),
    businessSetup: businessSetupBlockSchema.optional(),
    groupSetup: groupSetupBlockSchema.optional(),
  })
  .strict()
  .superRefine((body, ctx) => {
    if (body.domainCode === 'BUSINESS' && !body.companyId) {
      ctx.addIssue({ code: 'custom', message: 'companyId is required for BUSINESS moments.', path: ['companyId'] });
    }
    if (body.personalSetup && body.domainCode !== 'PERSONAL') {
      ctx.addIssue({ code: 'custom', message: 'personalSetup is only valid for PERSONAL moments.', path: ['personalSetup'] });
    }
    if (body.businessSetup && body.domainCode !== 'BUSINESS') {
      ctx.addIssue({ code: 'custom', message: 'businessSetup is only valid for BUSINESS moments.', path: ['businessSetup'] });
    }
    if (body.groupSetup && body.domainCode !== 'GROUP') {
      ctx.addIssue({ code: 'custom', message: 'groupSetup is only valid for GROUP moments.', path: ['groupSetup'] });
    }
    const setupBlocks = [body.personalSetup, body.businessSetup, body.groupSetup].filter(Boolean);
    if (setupBlocks.length > 1) {
      ctx.addIssue({ code: 'custom', message: 'Only one setup block may be sent.', path: ['groupSetup'] });
    }
    if (body.startAt && body.endAt && body.endAt < body.startAt) {
      ctx.addIssue({ code: 'custom', message: 'endAt must be greater than or equal to startAt.', path: ['endAt'] });
    }
    if (body.inviteCode && body.domainCode !== 'GROUP') {
      ctx.addIssue({ code: 'custom', message: 'inviteCode is only valid for GROUP moments.', path: ['inviteCode'] });
    }
  });

export const updateMomentSchema = z
  .object({
    title: z.string().min(1).max(500).optional(),
    description: z.string().max(5000).optional(),
    startAt: z.string().datetime().optional(),
    endAt: z.string().datetime().optional(),
    timezone: z.string().optional(),
    customTypeLabel: z.string().max(500).optional(),
    expectedVersion: z.number().int().positive(),
    groupSetup: groupSetupUpdateBlockSchema.optional(),
  })
  .strict()
  .superRefine((body, ctx) => {
    if (body.startAt && body.endAt && body.endAt < body.startAt) {
      ctx.addIssue({ code: 'custom', message: 'endAt must be greater than or equal to startAt.', path: ['endAt'] });
    }
  });

export type CreateMomentInput = z.infer<typeof createMomentSchema>;
export type UpdateMomentInput = z.infer<typeof updateMomentSchema>;
type GroupSetupUpdateInput = NonNullable<UpdateMomentInput['groupSetup']>;

export interface MomentResult {
  momentId: string;
  domainCode: string;
  title: string;
  status: string;
  version: number;
  momentTypeCode?: string;
  setupId?: string;
}

const PERSONAL_SYSTEM_LABELS: Record<string, string> = {
  LIFE_OPERATIONS: 'Life Operations',
  FUTURE_BUILDING: 'Future Building',
  LIFESTYLE: 'Lifestyle',
  RELATIONSHIPS: 'Relationships',
};

/** One ACTIVE Personal moment per life-system category (LIFE_OPERATIONS, etc.). */
export async function assertNoActivePersonalSystem(
  client: PoolClient,
  userId: string,
  systemCategoryCode: string
): Promise<void> {
  const existing = await client.query<{ moment_id: string }>(
    `SELECT m.moment_id
     FROM core.moment m
     JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     WHERE pmc.user_id = $1
       AND m.domain_code = 'PERSONAL'
       AND mc.code = $2
       AND m.status = 'ACTIVE'
     LIMIT 1`,
    [userId, systemCategoryCode]
  );
  if (existing.rows[0]) {
    const label = PERSONAL_SYSTEM_LABELS[systemCategoryCode] ?? systemCategoryCode;
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      `You already have an active ${label} moment.`,
      409
    );
  }
}

export async function createMoment(
  client: PoolClient,
  ctx: RequestContext,
  body: CreateMomentInput
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_CREATE', resourceType: 'MOMENT' });

  const momentType = await client.query<{ moment_type_id: string; category_code: string }>(
    `SELECT mt.moment_type_id, mc.code AS category_code
     FROM core.moment_type mt
     JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     WHERE mt.domain_code = $1 AND mt.code = $2 AND mt.status = 'ACTIVE'`,
    [body.domainCode, body.momentTypeCode]
  );
  if (!momentType.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown moment type: ${body.momentTypeCode}`, 400);
  }

  const momentTypeId = momentType.rows[0].moment_type_id;
  const familyCode = momentType.rows[0].category_code;
  const allowed = await resolveCapabilityForMomentType(client, momentTypeId, 'MOMENT_CREATE');
  if (!allowed && process.env.GOVERNANCE_FAIL_OPEN !== '1') {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Capability not enabled for moment type.', 403);
  }

  const momentStatus = body.status === 'DRAFT' ? 'DRAFT' : 'ACTIVE';

  if (body.domainCode === 'PERSONAL' && momentStatus === 'ACTIVE') {
    await assertNoActivePersonalSystem(client, ctx.userId, familyCode);
  }

  const momentInsert = await client.query<{ moment_id: string; version: string }>(
    `INSERT INTO core.moment (
       domain_code, moment_type_id, created_by_user_id, title, description,
       status, start_at, end_at, timezone, custom_type_label, version
     ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 1)
     RETURNING moment_id, version`,
    [
      body.domainCode,
      momentTypeId,
      ctx.userId,
      body.title,
      body.description ?? null,
      momentStatus,
      body.startAt ?? null,
      body.endAt ?? null,
      body.timezone,
      body.customTypeLabel ?? null,
    ]
  );
  const momentId = momentInsert.rows[0].moment_id;

  if (body.domainCode === 'PERSONAL') {
    await client.query(
      `INSERT INTO personal.personal_moment_context (moment_id, user_id, status, version)
       VALUES ($1, $2, $3, 1)`,
      [momentId, ctx.userId, momentStatus]
    );
  } else if (body.domainCode === 'GROUP') {
    await client.query(
      `INSERT INTO collaboration.group_moment_context (
         moment_id, group_family, organizer_user_id, status, version, reminder_preferences
       ) VALUES ($1, $2, $3, $4, 1, $5::jsonb)`,
      [
        momentId,
        familyCode,
        ctx.userId,
        momentStatus,
        JSON.stringify(body.groupSetup?.reminderPreferences ?? {}),
      ]
    );
    await client.query(
      `INSERT INTO collaboration.moment_participant (moment_id, user_id, participant_role, status, joined_at)
       VALUES ($1, $2, 'ORGANIZER', 'ACTIVE', now())`,
      [momentId, ctx.userId]
    );
    if (familyCode === 'SHARED_EXPERIENCE') {
      const places = body.groupSetup?.places ?? [];
      const destinationText =
        places.length > 0
          ? places.length === 1
            ? places[0]!.label
            : `${places.length} places`
          : (body.groupSetup?.destinationText ?? body.customTypeLabel ?? body.title);
      const placeStart = places[0]?.startAt ?? body.startAt ?? null;
      const placeEnd = places.length
        ? places[places.length - 1]?.endAt ?? places[0]?.endAt ?? body.endAt ?? null
        : (body.endAt ?? null);
      await client.query(
        `INSERT INTO collaboration.shared_experience_context (
           moment_id, experience_kind, destination_text, start_at, end_at, status,
           multi_currency_enabled, split_style, primary_goal, setup_preferences
         ) VALUES ($1, $2, $3, $4::timestamptz, $5::timestamptz, $6, $7, $8, $9, $10::jsonb)
         ON CONFLICT (moment_id) DO UPDATE SET
           experience_kind = EXCLUDED.experience_kind,
           destination_text = EXCLUDED.destination_text,
           start_at = EXCLUDED.start_at,
           end_at = EXCLUDED.end_at,
           status = EXCLUDED.status,
           multi_currency_enabled = EXCLUDED.multi_currency_enabled,
           split_style = EXCLUDED.split_style,
           primary_goal = EXCLUDED.primary_goal,
           setup_preferences = EXCLUDED.setup_preferences,
           updated_at = now()`,
        [
          momentId,
          body.momentTypeCode,
          destinationText,
          placeStart,
          placeEnd,
          momentStatus,
          body.groupSetup?.multiCurrencyEnabled ?? false,
          body.groupSetup?.splitStyle ?? null,
          body.groupSetup?.primaryGoal ?? null,
          JSON.stringify(body.groupSetup?.setupPreferences ?? {}),
        ]
      );
      if (places.length > 0) {
        let order = 0;
        for (const place of places) {
          await client.query(
            `INSERT INTO collaboration.shared_experience_place (
               moment_id, sort_order, label, start_at, end_at
             ) VALUES ($1, $2, $3, $4::timestamptz, $5::timestamptz)`,
            [momentId, order++, place.label, place.startAt ?? null, place.endAt ?? null]
          );
        }
      }
    } else if (familyCode === 'SHARED_PURCHASE') {
      await client.query(
        `INSERT INTO collaboration.shared_purchase_context (moment_id, purchase_purpose, status)
         VALUES ($1, $2, 'ACTIVE')
         ON CONFLICT (moment_id) DO NOTHING`,
        [momentId, body.customTypeLabel ?? body.title]
      );
    } else if (familyCode === 'SHARED_LIVING') {
      await client.query(
        `INSERT INTO collaboration.shared_living_context (moment_id, property_name, status)
         VALUES ($1, $2, 'ACTIVE')
         ON CONFLICT (moment_id) DO NOTHING`,
        [momentId, body.customTypeLabel ?? body.title]
      );
    }
  } else if (body.domainCode === 'BUSINESS') {
    const membership = await client.query(
      `SELECT 1 FROM business.company_membership
       WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [body.companyId, ctx.userId]
    );
    if (!membership.rowCount) {
      throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Active company membership required.', 403);
    }
    await client.query(
      `INSERT INTO business.business_moment_context (moment_id, company_id, business_family, team_id, status, version)
       VALUES ($1, $2, $3, $4, $5, 1)`,
      [momentId, body.companyId, familyCode, body.teamId ?? null, momentStatus]
    );
  }

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentCreated',
    domainCode: body.domainCode,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId, momentTypeCode: body.momentTypeCode, title: body.title, userId: ctx.userId },
  });

  const result: MomentResult = {
    momentId,
    domainCode: body.domainCode,
    title: body.title,
    status: momentStatus,
    version: parseInt(momentInsert.rows[0].version, 10),
    momentTypeCode: body.momentTypeCode,
  };

  await insertAudit(client, ctx, 'MOMENT_CREATE', 'MOMENT', momentId, domainEventId, result as unknown as Record<string, unknown>);

  const leanDomain =
    body.domainCode === 'GROUP' ? 'group' : body.domainCode === 'BUSINESS' ? 'business' : 'personal';
  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'moment_created',
    eventId: domainEventId,
    momentId,
    momentDomain: leanDomain,
    momentCategory: familyCode,
    momentType: body.momentTypeCode,
    properties: {
      creation_method: 'api',
      has_budget: Boolean(
        body.groupSetup?.budgetAmount || (body.groupSetup?.budgets && body.groupSetup.budgets.length > 0)
      ),
      has_timeline: Boolean(body.startAt || body.endAt),
      is_draft: momentStatus === 'DRAFT',
      initial_participant_count: (body.participants?.length ?? 0) + (body.domainCode === 'GROUP' ? 1 : 0),
    },
  });

  if (body.domainCode === 'PERSONAL' && momentStatus === 'ACTIVE') {
    await client.query(
      `INSERT INTO projection.personal_moments (
         user_id, moment_id, temporal_bucket, display_rank, status, title,
         moment_type_code, start_at, end_at, card_payload, projection_version
       ) VALUES ($1, $2, 'ACTIVE', 0, 'ACTIVE', $3, $4, $5, $6, '{}'::jsonb, 1)
       ON CONFLICT (user_id, moment_id) DO NOTHING`,
      [ctx.userId, momentId, body.title, body.momentTypeCode, body.startAt ?? null, body.endAt ?? null]
    );
    await client.query(
      `INSERT INTO projection.personal_pulse (user_id, active_moment_count, projection_version)
       VALUES ($1, 1, 1)
       ON CONFLICT (user_id) DO UPDATE SET
         active_moment_count = projection.personal_pulse.active_moment_count + 1,
         projection_version = projection.personal_pulse.projection_version + 1,
         updated_at = now()`,
      [ctx.userId]
    );
  }

  if (body.domainCode === 'GROUP' && body.participants?.length) {
    for (const participant of body.participants) {
      if (participant.userId && participant.userId === ctx.userId) {
        continue; // organizer already inserted
      }
      if (participant.roleCode === 'ORGANIZER') {
        continue;
      }
      if (participant.userId) {
        await client.query(
          `INSERT INTO collaboration.moment_participant (
             moment_id, user_id, participant_role, status, invited_at, version
           ) VALUES ($1, $2, $3, 'INVITED', now(), 1)
           ON CONFLICT DO NOTHING`,
          [momentId, participant.userId, participant.roleCode]
        );
        continue;
      }
      if (participant.displayName) {
        const party = await client.query<{ external_party_id: string }>(
          `INSERT INTO core.external_party (party_type, display_name, status)
           VALUES ('PERSON', $1, 'ACTIVE')
           RETURNING external_party_id`,
          [participant.displayName]
        );
        await client.query(
          `INSERT INTO collaboration.moment_participant (
             moment_id, external_party_id, participant_role, status, invited_at, version, metadata
           ) VALUES ($1, $2, $3, 'INVITED', now(), 1, $4::jsonb)`,
          [
            momentId,
            party.rows[0]!.external_party_id,
            participant.roleCode,
            JSON.stringify({
              displayName: participant.displayName,
              email: participant.email ?? null,
              phone: participant.phone ?? null,
            }),
          ]
        );
      }
    }
  }

  if (body.domainCode === 'GROUP' && body.inviteCode && momentStatus === 'ACTIVE') {
    await bindInviteToMoment(client, ctx, body.inviteCode, momentId, body.momentTypeCode, body.title);
  }

  if (body.personalSetup) {
    const preferences = validateAndMergePersonalPreferences(
      body.personalSetup.systemCode,
      body.personalSetup.preferences as Record<string, unknown> | undefined
    );
    result.setupId = await insertPersonalSetupRow(
      client,
      ctx,
      momentId,
      body.personalSetup.systemCode,
      body.title,
      preferences,
      momentStatus
    );
  } else if (body.businessSetup && body.companyId) {
    const preferences = validateAndMergeBusinessPreferences(
      body.businessSetup.familyCode,
      body.businessSetup.preferences as Record<string, unknown> | undefined
    );
    result.setupId = await insertBusinessSetupRow(
      client,
      ctx,
      momentId,
      body.companyId,
      body.businessSetup.familyCode,
      body.title,
      preferences,
      momentStatus
    );
  }

  if (body.domainCode === 'GROUP' && body.groupSetup) {
    const budgets = normalizeGroupSetupBudgets(body.groupSetup);
    for (const b of budgets) {
      await seedGroupBudget(client, ctx, momentId, b.amount, b.currencyCode);
    }
  }

  return result;
}

export async function activateMomentDraft(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<MomentResult> {
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const row = await client.query<{
    moment_id: string;
    domain_code: string;
    title: string;
    status: string;
    version: string;
    moment_type_code: string;
    category_code: string;
    company_id: string | null;
  }>(
    `SELECT m.moment_id, m.domain_code, m.title, m.status, m.version::text,
            mt.code AS moment_type_code, mc.code AS category_code,
            bmc.company_id
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  const m = row.rows[0];
  if (m.status === 'ACTIVE') {
    return {
      momentId: m.moment_id,
      domainCode: m.domain_code,
      title: m.title,
      status: 'ACTIVE',
      version: parseInt(m.version, 10),
      momentTypeCode: m.moment_type_code,
    };
  }
  if (m.status !== 'DRAFT') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only DRAFT moments can be activated.', 409);
  }

  if (m.domain_code === 'PERSONAL') {
    await assertNoActivePersonalSystem(client, ctx.userId, m.category_code);
  }

  await client.query(
    `UPDATE core.moment SET status = 'ACTIVE', updated_at = now(), version = version + 1 WHERE moment_id = $1`,
    [momentId]
  );

  if (m.domain_code === 'GROUP') {
    await client.query(
      `UPDATE collaboration.group_moment_context SET status = 'ACTIVE', updated_at = now(), version = version + 1
       WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `UPDATE collaboration.shared_experience_context SET status = 'ACTIVE', updated_at = now()
       WHERE moment_id = $1`,
      [momentId]
    );
  } else if (m.domain_code === 'PERSONAL') {
    await client.query(
      `UPDATE personal.personal_moment_context SET status = 'ACTIVE', updated_at = now(), version = version + 1
       WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `UPDATE personal.life_system_setup SET status = 'ACTIVE', updated_at = now(), version = version + 1
       WHERE moment_id = $1 AND status = 'DRAFT'`,
      [momentId]
    );
    await client.query(
      `INSERT INTO projection.personal_moments (
         user_id, moment_id, temporal_bucket, display_rank, status, title,
         moment_type_code, start_at, end_at, card_payload, projection_version
       )
       SELECT pmc.user_id, m.moment_id, 'ACTIVE', 0, 'ACTIVE', m.title,
              mt.code, m.start_at, m.end_at, '{}'::jsonb, 1
       FROM core.moment m
       JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE m.moment_id = $1
       ON CONFLICT (user_id, moment_id) DO UPDATE SET status = 'ACTIVE', title = EXCLUDED.title`,
      [momentId]
    );
    await client.query(
      `INSERT INTO projection.personal_pulse (user_id, active_moment_count, projection_version)
       VALUES ($1, 1, 1)
       ON CONFLICT (user_id) DO UPDATE SET
         active_moment_count = projection.personal_pulse.active_moment_count + 1,
         projection_version = projection.personal_pulse.projection_version + 1,
         updated_at = now()`,
      [ctx.userId]
    );
  } else if (m.domain_code === 'BUSINESS') {
    await client.query(
      `UPDATE business.business_moment_context SET status = 'ACTIVE', updated_at = now(), version = version + 1
       WHERE moment_id = $1`,
      [momentId]
    );
    const setup = await client.query<{ family_code: string; title: string; company_id: string }>(
      `SELECT family_code, title, company_id FROM business.business_system_setup
       WHERE moment_id = $1 AND status = 'DRAFT' LIMIT 1`,
      [momentId]
    );
    if (setup.rows[0]) {
      await insertBusinessFamilyContext(
        client,
        momentId,
        setup.rows[0].family_code as 'TEAM_OPERATIONS' | 'BUSINESS_RUNWAY' | 'BUSINESS_OPERATIONS',
        setup.rows[0].title
      );
      await client.query(
        `UPDATE business.business_system_setup SET status = 'ACTIVE', updated_at = now(), version = version + 1
         WHERE moment_id = $1 AND status = 'DRAFT'`,
        [momentId]
      );
    }
  }

  const ver = await client.query<{ version: string }>(
    `SELECT version::text FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentActivated',
    domainCode: m.domain_code,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId, fromStatus: 'DRAFT' },
  });
  await insertAudit(client, ctx, 'MOMENT_ACTIVATE', 'MOMENT', momentId, domainEventId, {
    momentId,
  });

  return {
    momentId,
    domainCode: m.domain_code,
    title: m.title,
    status: 'ACTIVE',
    version: parseInt(ver.rows[0]!.version, 10),
    momentTypeCode: m.moment_type_code,
  };
}

/** @deprecated Use activateMomentDraft */
export const activateGroupMomentDraft = activateMomentDraft;

export async function discardMomentDraft(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ momentId: string; discarded: boolean }> {
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const row = await client.query<{ status: string; domain_code: string }>(
    `SELECT status, domain_code FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  if (row.rows[0].status !== 'DRAFT') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only DRAFT moments can be discarded.', 409);
  }
  const domain = row.rows[0].domain_code;

  await client.query(
    `UPDATE core.moment SET status = 'DELETED', updated_at = now(), version = version + 1 WHERE moment_id = $1`,
    [momentId]
  );

  if (domain === 'GROUP') {
    await client.query(
      `UPDATE collaboration.group_moment_context SET status = 'CANCELLED', updated_at = now() WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `UPDATE collaboration.shared_experience_context SET status = 'CANCELLED', updated_at = now() WHERE moment_id = $1`,
      [momentId]
    );
  } else if (domain === 'PERSONAL') {
    await client.query(
      `UPDATE personal.personal_moment_context SET status = 'CANCELLED', updated_at = now() WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `UPDATE personal.life_system_setup SET status = 'ARCHIVED', updated_at = now() WHERE moment_id = $1 AND status = 'DRAFT'`,
      [momentId]
    );
  } else if (domain === 'BUSINESS') {
    await client.query(
      `UPDATE business.business_moment_context SET status = 'CANCELLED', updated_at = now() WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `UPDATE business.business_system_setup SET status = 'ARCHIVED', updated_at = now() WHERE moment_id = $1 AND status = 'DRAFT'`,
      [momentId]
    );
  }

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentDraftDiscarded',
    domainCode: domain,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId },
  });
  await insertAudit(client, ctx, 'MOMENT_DISCARD_DRAFT', 'MOMENT', momentId, domainEventId, {
    momentId,
  });

  return { momentId, discarded: true };
}

/** @deprecated Use discardMomentDraft */
export const discardGroupMomentDraft = discardMomentDraft;

export async function getGroupSetupPrefill(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  momentId: string;
  title: string;
  status: string;
  momentTypeCode: string;
  startAt: string | null;
  endAt: string | null;
  places: Array<{ placeId: string; label: string; startAt: string | null; endAt: string | null; sortOrder: number }>;
  budgets: Array<{ currencyCode: string; amount: string; isPrimary: boolean }>;
  multiCurrencyEnabled: boolean;
  splitStyle: string | null;
  primaryGoal: string | null;
  destinationText: string | null;
  reminderPreferences: Record<string, unknown>;
  setupPreferences: Record<string, unknown>;
  version: number;
}> {
  const member = await client.query(
    `SELECT 1 FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [momentId, ctx.userId]
  );
  if (!member.rowCount) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not a member of this moment.', 403);
  }
  const m = await client.query<{
    title: string;
    status: string;
    start_at: Date | null;
    end_at: Date | null;
    version: string;
    moment_type_code: string;
    destination_text: string | null;
    multi_currency_enabled: boolean | null;
    split_style: string | null;
    primary_goal: string | null;
    setup_preferences: Record<string, unknown> | null;
    reminder_preferences: Record<string, unknown> | null;
  }>(
    `SELECT m.title, m.status, m.start_at, m.end_at, m.version::text,
            mt.code AS moment_type_code,
            sec.destination_text,
            sec.multi_currency_enabled,
            sec.split_style,
            sec.primary_goal,
            sec.setup_preferences,
            gmc.reminder_preferences
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     LEFT JOIN collaboration.shared_experience_context sec ON sec.moment_id = m.moment_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  if (!m.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  const row = m.rows[0];
  const places = await client.query<{
    place_id: string;
    label: string;
    start_at: Date | null;
    end_at: Date | null;
    sort_order: number;
  }>(
    `SELECT place_id, label, start_at, end_at, sort_order
     FROM collaboration.shared_experience_place
     WHERE moment_id = $1
     ORDER BY sort_order ASC`,
    [momentId]
  );
  const budgets = await client.query<{ currency_code: string; amount: string }>(
    `SELECT currency_code, amount::text
     FROM finance.budget
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY created_at ASC`,
    [momentId]
  );

  return {
    momentId,
    title: row.title,
    status: row.status,
    momentTypeCode: row.moment_type_code,
    startAt: row.start_at?.toISOString() ?? null,
    endAt: row.end_at?.toISOString() ?? null,
    places: places.rows.map((p) => ({
      placeId: p.place_id,
      label: p.label,
      startAt: p.start_at?.toISOString() ?? null,
      endAt: p.end_at?.toISOString() ?? null,
      sortOrder: p.sort_order,
    })),
    budgets: budgets.rows.map((b, i) => ({
      currencyCode: b.currency_code,
      amount: b.amount,
      isPrimary: i === 0,
    })),
    multiCurrencyEnabled: row.multi_currency_enabled ?? false,
    splitStyle: row.split_style,
    primaryGoal: row.primary_goal,
    destinationText: row.destination_text,
    reminderPreferences: row.reminder_preferences ?? {},
    setupPreferences: row.setup_preferences ?? {},
    version: parseInt(row.version, 10),
  };
}

const SPLIT_STYLES = new Set(['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'POOLED']);

export const duplicateGroupMomentSchema = z
  .object({
    includeData: z.boolean().optional().default(false),
  })
  .strict();

export type DuplicateGroupMomentInput = z.infer<typeof duplicateGroupMomentSchema>;

/**
 * Duplicate a Group moment: copy setup + checklist structure into a new ACTIVE moment.
 * Caller becomes sole ORGANIZER. Optionally copy expenses, contributions, and memories.
 */
export async function duplicateGroupMoment(
  client: PoolClient,
  ctx: RequestContext,
  sourceMomentId: string,
  opts: DuplicateGroupMomentInput = { includeData: false }
): Promise<MomentResult> {
  const domainCheck = await client.query<{ domain_code: string }>(
    `SELECT domain_code FROM core.moment WHERE moment_id = $1`,
    [sourceMomentId]
  );
  if (!domainCheck.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  if (domainCheck.rows[0].domain_code !== 'GROUP') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only Group moments can be duplicated.', 400);
  }

  const prefill = await getGroupSetupPrefill(client, ctx, sourceMomentId);
  const baseTitle = prefill.title.trim() || 'Moment';
  const copySuffix = ' (Copy)';
  const title =
    baseTitle.length + copySuffix.length <= 500
      ? `${baseTitle}${copySuffix}`
      : `${baseTitle.slice(0, Math.max(1, 500 - copySuffix.length))}${copySuffix}`;

  const reminderRaw = prefill.reminderPreferences ?? {};
  const reminderPreferences = {
    billReminders: typeof reminderRaw.billReminders === 'boolean' ? reminderRaw.billReminders : undefined,
    choreReminders: typeof reminderRaw.choreReminders === 'boolean' ? reminderRaw.choreReminders : undefined,
    expenseReminders: typeof reminderRaw.expenseReminders === 'boolean' ? reminderRaw.expenseReminders : undefined,
    photoReminders: typeof reminderRaw.photoReminders === 'boolean' ? reminderRaw.photoReminders : undefined,
    paymentReminders: typeof reminderRaw.paymentReminders === 'boolean' ? reminderRaw.paymentReminders : undefined,
  };
  const hasReminderPref = Object.values(reminderPreferences).some((v) => v !== undefined);

  const budgets =
    prefill.budgets.length > 0
      ? prefill.budgets.map((b) => ({
          currencyCode: b.currencyCode,
          amount: b.amount,
          isPrimary: b.isPrimary,
        }))
      : [{ currencyCode: 'USD', amount: '0', isPrimary: true }];

  const splitStyle =
    prefill.splitStyle && SPLIT_STYLES.has(prefill.splitStyle)
      ? (prefill.splitStyle as 'EQUAL' | 'PERCENTAGE' | 'EXACT' | 'SHARES' | 'POOLED')
      : undefined;

  const createBody = createMomentSchema.parse({
    domainCode: 'GROUP',
    momentTypeCode: prefill.momentTypeCode,
    title,
    startAt: prefill.startAt ?? undefined,
    endAt: prefill.endAt ?? undefined,
    status: 'ACTIVE',
    groupSetup: {
      budgets,
      destinationText: prefill.destinationText ?? undefined,
      places: prefill.places.map((p) => ({
        label: p.label,
        startAt: p.startAt ?? undefined,
        endAt: p.endAt ?? undefined,
      })),
      multiCurrencyEnabled: prefill.multiCurrencyEnabled,
      splitStyle,
      primaryGoal: prefill.primaryGoal ?? undefined,
      reminderPreferences: hasReminderPref ? reminderPreferences : undefined,
      setupPreferences: prefill.setupPreferences,
    },
  });

  const created = await createMoment(client, ctx, createBody);

  await client.query(
    `INSERT INTO collaboration.planning_item (
       moment_id, title, description, due_at, status, category_code, location, priority_code
     )
     SELECT $1, title, description, NULL, 'OPEN', category_code, location, priority_code
     FROM collaboration.planning_item
     WHERE moment_id = $2
       AND status <> 'CANCELLED'`,
    [created.momentId, sourceMomentId]
  );

  if (opts.includeData) {
    await copyGroupMomentOperationalData(client, ctx, sourceMomentId, created.momentId);
  }

  return created;
}

/** Copy expenses, contributions, and memories onto a new moment; remap to the caller as sole member. */
async function copyGroupMomentOperationalData(
  client: PoolClient,
  ctx: RequestContext,
  sourceMomentId: string,
  targetMomentId: string
): Promise<void> {
  const organizer = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'
     LIMIT 1`,
    [targetMomentId, ctx.userId]
  );
  const organizerParticipantId = organizer.rows[0]?.participant_id;
  if (!organizerParticipantId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Organizer participant missing on duplicated moment.', 500);
  }

  // --- Expenses (non-voided): payer + 100% share to organizer; no settlements ---
  const sourceExpenses = await client.query<{
    expense_id: string;
    description: string | null;
    merchant_name: string | null;
    category_code: string | null;
    amount: string;
    currency_code: string;
    effective_at: Date;
    status: string;
    posted_at: Date | null;
    payment_method_code: string | null;
    split_strategy: string | null;
  }>(
    `SELECT e.expense_id, e.description, e.merchant_name, e.category_code,
            e.amount::text, e.currency_code, e.effective_at, e.status, e.posted_at,
            e.payment_method_code, g.split_strategy
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g ON g.expense_id = e.expense_id AND g.moment_id = e.moment_id
     WHERE e.moment_id = $1
       AND e.status IN ('DRAFT', 'POSTED')
     ORDER BY e.effective_at ASC, e.created_at ASC`,
    [sourceMomentId]
  );

  for (const exp of sourceExpenses.rows) {
    const inserted = await client.query<{ expense_id: string }>(
      `INSERT INTO finance.expense (
         moment_id, domain_code, created_by_user_id, merchant_name, description, category_code,
         amount, currency_code, effective_at, status, posted_at, payment_method_code, version
       ) VALUES (
         $1, 'GROUP', $2, $3, $4, $5, $6::numeric, $7, $8::timestamptz, $9,
         CASE WHEN $9 = 'POSTED' THEN COALESCE($10::timestamptz, now()) ELSE NULL END,
         $11, 1
       )
       RETURNING expense_id`,
      [
        targetMomentId,
        ctx.userId,
        exp.merchant_name,
        exp.description,
        exp.category_code,
        exp.amount,
        exp.currency_code,
        exp.effective_at.toISOString(),
        exp.status,
        exp.posted_at?.toISOString() ?? null,
        exp.payment_method_code,
      ]
    );
    const newExpenseId = inserted.rows[0]!.expense_id;
    const splitStrategy =
      exp.split_strategy && SPLIT_STYLES.has(exp.split_strategy) ? exp.split_strategy : 'EQUAL';
    await client.query(
      `INSERT INTO finance.group_expense_context (
         expense_id, moment_id, paid_by_participant_id, split_strategy
       ) VALUES ($1, $2, $3, $4)`,
      [newExpenseId, targetMomentId, organizerParticipantId, splitStrategy]
    );
    if (exp.status === 'POSTED' && splitStrategy !== 'POOLED') {
      await client.query(
        `INSERT INTO finance.expense_share (
           expense_id, moment_id, participant_id, share_amount, share_percent, status
         ) VALUES ($1, $2, $3, $4::numeric, 100, 'ALLOCATED')`,
        [newExpenseId, targetMomentId, organizerParticipantId, exp.amount]
      );
    }
  }

  // --- Contributions ---
  await client.query(
    `INSERT INTO finance.contribution (
       moment_id, participant_id, amount, currency_code, contributed_at, status, version,
       label, payment_method_code
     )
     SELECT $1, $2, amount, currency_code, contributed_at, status, 1, label, payment_method_code
     FROM finance.contribution
     WHERE moment_id = $3
       AND status IN ('RECORDED', 'PENDING')`,
    [targetMomentId, organizerParticipantId, sourceMomentId]
  );

  // --- Memories + evidence (reuse media upload ids; no blob re-upload) ---
  const sourceMemories = await client.query<{
    memory_id: string;
    title: string;
    summary: string | null;
    memory_type: string;
    significance_level: string;
    visibility_level: string;
    occurred_at: Date | null;
    status: string;
    source_type: string;
  }>(
    `SELECT memory_id, title, summary, memory_type, significance_level, visibility_level,
            occurred_at, status, source_type
     FROM memory.memory
     WHERE moment_id = $1
       AND status IN ('ACTIVE', 'ARCHIVED')
     ORDER BY COALESCE(occurred_at, created_at) ASC`,
    [sourceMomentId]
  );

  for (const mem of sourceMemories.rows) {
    const inserted = await client.query<{ memory_id: string }>(
      `INSERT INTO memory.memory (
         scope_type, scope_id, domain_code, moment_id, title, summary, memory_type,
         significance_level, visibility_level, occurred_at, status, source_type,
         created_by_user_id, version
       ) VALUES (
         'MOMENT', $1, 'GROUP', $1, $2, $3, $4, $5, $6, $7::timestamptz, $8, $9, $10, 1
       )
       RETURNING memory_id`,
      [
        targetMomentId,
        mem.title,
        mem.summary,
        mem.memory_type,
        mem.significance_level,
        mem.visibility_level,
        mem.occurred_at?.toISOString() ?? null,
        mem.status,
        mem.source_type,
        ctx.userId,
      ]
    );
    const newMemoryId = inserted.rows[0]!.memory_id;
    await client.query(
      `INSERT INTO memory.memory_evidence (
         memory_id, source_type, source_id, evidence_role, evidence_snapshot,
         snapshot_schema_version, observed_at
       )
       SELECT $1, source_type, source_id, evidence_role, evidence_snapshot,
              snapshot_schema_version, observed_at
       FROM memory.memory_evidence
       WHERE memory_id = $2
       ON CONFLICT DO NOTHING`,
      [newMemoryId, mem.memory_id]
    );
  }

  await rebuildGroupFinanceSnapshotAfterCopy(client, targetMomentId);
}

async function rebuildGroupFinanceSnapshotAfterCopy(
  client: PoolClient,
  momentId: string
): Promise<void> {
  const expenseAgg = await client.query<{
    currency_code: string;
    expense_total: string;
    expense_count: string;
  }>(
    `SELECT currency_code, COALESCE(SUM(amount), 0)::text AS expense_total, COUNT(*)::text AS expense_count
     FROM finance.expense
     WHERE moment_id = $1 AND status = 'POSTED'
     GROUP BY currency_code`,
    [momentId]
  );
  const contribAgg = await client.query<{
    currency_code: string;
    contribution_total: string;
  }>(
    `SELECT currency_code, COALESCE(SUM(amount), 0)::text AS contribution_total
     FROM finance.contribution
     WHERE moment_id = $1 AND status = 'RECORDED'
     GROUP BY currency_code`,
    [momentId]
  );
  const byCurrency = new Map<string, { expenseTotal: string; expenseCount: number; contributionTotal: string }>();
  for (const row of expenseAgg.rows) {
    byCurrency.set(row.currency_code, {
      expenseTotal: row.expense_total,
      expenseCount: Number(row.expense_count) || 0,
      contributionTotal: '0',
    });
  }
  for (const row of contribAgg.rows) {
    const cur = byCurrency.get(row.currency_code) ?? {
      expenseTotal: '0',
      expenseCount: 0,
      contributionTotal: '0',
    };
    cur.contributionTotal = row.contribution_total;
    byCurrency.set(row.currency_code, cur);
  }
  for (const [currencyCode, totals] of byCurrency) {
    await client.query(
      `INSERT INTO projection.group_finance_snapshot (
         moment_id, currency_code, expense_total, outstanding_total, contribution_total,
         snapshot_payload, projection_version
       ) VALUES ($1, $2, $3::numeric, 0, $4::numeric, $5::jsonb, 1)
       ON CONFLICT (moment_id, currency_code) DO UPDATE SET
         expense_total = EXCLUDED.expense_total,
         contribution_total = EXCLUDED.contribution_total,
         snapshot_payload = COALESCE(projection.group_finance_snapshot.snapshot_payload, '{}'::jsonb)
           || EXCLUDED.snapshot_payload,
         projection_version = projection.group_finance_snapshot.projection_version + 1,
         updated_at = now()`,
      [
        momentId,
        currencyCode,
        totals.expenseTotal,
        totals.contributionTotal,
        JSON.stringify({ expenseCount: totals.expenseCount }),
      ]
    );
  }
}

/** Prefill for Personal or Business setup resume (DRAFT or ACTIVE). */
export async function getDomainSetupPrefill(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  momentId: string;
  title: string;
  status: string;
  domainCode: string;
  momentTypeCode: string;
  systemCode: string | null;
  familyCode: string | null;
  preferences: Record<string, unknown>;
  companyId: string | null;
  version: number;
}> {
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const m = await client.query<{
    title: string;
    status: string;
    domain_code: string;
    moment_type_code: string;
    version: string;
  }>(
    `SELECT m.title, m.status, m.domain_code, mt.code AS moment_type_code, m.version::text
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  if (!m.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  const row = m.rows[0];
  let systemCode: string | null = null;
  let familyCode: string | null = null;
  let preferences: Record<string, unknown> = {};
  let companyId: string | null = null;

  if (row.domain_code === 'PERSONAL') {
    const setup = await client.query<{ system_code: string; preferences: Record<string, unknown> }>(
      `SELECT system_code, preferences
       FROM personal.life_system_setup
       WHERE moment_id = $1 AND user_id = $2
       ORDER BY created_at DESC LIMIT 1`,
      [momentId, ctx.userId]
    );
    systemCode = setup.rows[0]?.system_code ?? null;
    preferences = setup.rows[0]?.preferences ?? {};
  } else if (row.domain_code === 'BUSINESS') {
    const setup = await client.query<{
      family_code: string;
      preferences: Record<string, unknown>;
      company_id: string;
    }>(
      `SELECT family_code, preferences, company_id
       FROM business.business_system_setup
       WHERE moment_id = $1
       ORDER BY created_at DESC LIMIT 1`,
      [momentId]
    );
    familyCode = setup.rows[0]?.family_code ?? null;
    preferences = setup.rows[0]?.preferences ?? {};
    companyId = setup.rows[0]?.company_id ?? null;
    if (!companyId) {
      const bmc = await client.query<{ company_id: string }>(
        `SELECT company_id FROM business.business_moment_context WHERE moment_id = $1`,
        [momentId]
      );
      companyId = bmc.rows[0]?.company_id ?? null;
    }
  } else if (row.domain_code === 'GROUP') {
    const gmc = await client.query<{ group_family: string }>(
      `SELECT group_family FROM collaboration.group_moment_context WHERE moment_id = $1`,
      [momentId]
    );
    familyCode = gmc.rows[0]?.group_family ?? null;
  }

  return {
    momentId,
    title: row.title,
    status: row.status,
    domainCode: row.domain_code,
    momentTypeCode: row.moment_type_code,
    systemCode,
    familyCode,
    preferences,
    companyId,
    version: parseInt(row.version, 10),
  };
}

export async function updateMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: UpdateMomentInput
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_UPDATE', resourceType: 'MOMENT', momentId });

  let startAt = body.startAt ?? null;
  let endAt = body.endAt ?? null;
  if (body.groupSetup?.places && body.groupSetup.places.length > 0) {
    const places = body.groupSetup.places;
    if (!startAt) startAt = places[0]?.startAt ?? null;
    if (!endAt) {
      endAt = places[places.length - 1]?.endAt ?? places[0]?.endAt ?? null;
    }
  }

  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment SET
       title = COALESCE($3, title),
       description = COALESCE($4, description),
       start_at = COALESCE($5::timestamptz, start_at),
       end_at = COALESCE($6::timestamptz, end_at),
       timezone = COALESCE($7, timezone),
       custom_type_label = COALESCE($8, custom_type_label),
       version = version + 1,
       updated_at = now()
     WHERE moment_id = $1 AND version = $2
     RETURNING moment_id, domain_code, title, status, version`,
    [
      momentId,
      body.expectedVersion,
      body.title ?? null,
      body.description ?? null,
      startAt,
      endAt,
      body.timezone ?? null,
      body.customTypeLabel ?? null,
    ]
  );
  if (!updated.rows[0]) {
    const current = await client.query<{ version: string }>(
      `SELECT version::text FROM core.moment WHERE moment_id = $1`,
      [momentId]
    );
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409, {
      resourceId: momentId,
      expectedVersion: body.expectedVersion,
      currentVersion: current.rows[0] ? parseInt(current.rows[0].version, 10) : null,
    });
  }
  const row = updated.rows[0];

  if (body.groupSetup && row.domain_code === 'GROUP') {
    await upsertGroupSetupOnUpdate(client, {
      momentId,
      title: row.title,
      status: row.status,
      customTypeLabel: body.customTypeLabel ?? null,
      startAt,
      endAt,
      groupSetup: body.groupSetup,
    });
  }

  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
  };
}

async function upsertGroupSetupOnUpdate(
  client: PoolClient,
  args: {
    momentId: string;
    title: string;
    status: string;
    customTypeLabel: string | null;
    startAt: string | null;
    endAt: string | null;
    groupSetup: GroupSetupUpdateInput;
  }
): Promise<void> {
  const gmc = await client.query<{ group_family: string }>(
    `SELECT group_family FROM collaboration.group_moment_context WHERE moment_id = $1`,
    [args.momentId]
  );
  const familyCode = gmc.rows[0]?.group_family;
  if (!familyCode) return;

  const typeRow = await client.query<{ moment_type_code: string }>(
    `SELECT mt.code AS moment_type_code
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     WHERE m.moment_id = $1`,
    [args.momentId]
  );
  const momentTypeCode = typeRow.rows[0]?.moment_type_code ?? 'TRIP';

  if (args.groupSetup.reminderPreferences) {
    await client.query(
      `UPDATE collaboration.group_moment_context
       SET reminder_preferences = $2::jsonb, updated_at = now()
       WHERE moment_id = $1`,
      [args.momentId, JSON.stringify(args.groupSetup.reminderPreferences)]
    );
  }

  if (familyCode === 'SHARED_EXPERIENCE') {
    await upsertSharedExperienceSetup(client, {
      momentId: args.momentId,
      momentTypeCode,
      momentStatus: args.status,
      title: args.title,
      customTypeLabel: args.customTypeLabel,
      startAt: args.startAt,
      endAt: args.endAt,
      groupSetup: args.groupSetup,
      replacePlaces: args.groupSetup.places !== undefined,
    });
  } else if (familyCode === 'SHARED_PURCHASE') {
    await client.query(
      `INSERT INTO collaboration.shared_purchase_context (moment_id, purchase_purpose, status)
       VALUES ($1, $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO UPDATE SET
         purchase_purpose = EXCLUDED.purchase_purpose,
         updated_at = now()`,
      [args.momentId, args.customTypeLabel ?? args.title]
    );
  } else if (familyCode === 'SHARED_LIVING') {
    await client.query(
      `INSERT INTO collaboration.shared_living_context (moment_id, property_name, status)
       VALUES ($1, $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO UPDATE SET
         property_name = EXCLUDED.property_name,
         updated_at = now()`,
      [args.momentId, args.customTypeLabel ?? args.title]
    );
  }
}

async function upsertSharedExperienceSetup(
  client: PoolClient,
  args: {
    momentId: string;
    momentTypeCode: string;
    momentStatus: string;
    title: string;
    customTypeLabel: string | null;
    startAt: string | null;
    endAt: string | null;
    groupSetup: {
      places?: Array<{ label: string; startAt?: string | null; endAt?: string | null }>;
      destinationText?: string | null;
      multiCurrencyEnabled?: boolean;
      splitStyle?: string | null;
      primaryGoal?: string | null;
      setupPreferences?: Record<string, unknown>;
    };
    replacePlaces: boolean;
  }
): Promise<void> {
  const places = args.groupSetup.places ?? [];
  const destinationText =
    places.length > 0
      ? places.length === 1
        ? places[0]!.label
        : `${places.length} places`
      : (args.groupSetup.destinationText ?? args.customTypeLabel ?? args.title);
  const placeStart = places[0]?.startAt ?? args.startAt ?? null;
  const placeEnd = places.length
    ? places[places.length - 1]?.endAt ?? places[0]?.endAt ?? args.endAt ?? null
    : (args.endAt ?? null);

  await client.query(
    `INSERT INTO collaboration.shared_experience_context (
       moment_id, experience_kind, destination_text, start_at, end_at, status,
       multi_currency_enabled, split_style, primary_goal, setup_preferences
     ) VALUES ($1, $2, $3, $4::timestamptz, $5::timestamptz, $6, $7, $8, $9, $10::jsonb)
     ON CONFLICT (moment_id) DO UPDATE SET
       experience_kind = EXCLUDED.experience_kind,
       destination_text = EXCLUDED.destination_text,
       start_at = EXCLUDED.start_at,
       end_at = EXCLUDED.end_at,
       status = EXCLUDED.status,
       multi_currency_enabled = COALESCE(EXCLUDED.multi_currency_enabled, collaboration.shared_experience_context.multi_currency_enabled),
       split_style = COALESCE(EXCLUDED.split_style, collaboration.shared_experience_context.split_style),
       primary_goal = COALESCE(EXCLUDED.primary_goal, collaboration.shared_experience_context.primary_goal),
       setup_preferences = CASE
         WHEN EXCLUDED.setup_preferences = '{}'::jsonb THEN collaboration.shared_experience_context.setup_preferences
         ELSE EXCLUDED.setup_preferences
       END,
       updated_at = now()`,
    [
      args.momentId,
      args.momentTypeCode,
      destinationText,
      placeStart,
      placeEnd,
      args.momentStatus,
      args.groupSetup.multiCurrencyEnabled ?? false,
      args.groupSetup.splitStyle ?? null,
      args.groupSetup.primaryGoal ?? null,
      JSON.stringify(args.groupSetup.setupPreferences ?? {}),
    ]
  );

  if (args.replacePlaces) {
    await client.query(`DELETE FROM collaboration.shared_experience_place WHERE moment_id = $1`, [
      args.momentId,
    ]);
  }
  if (places.length > 0) {
    let order = 0;
    for (const place of places) {
      await client.query(
        `INSERT INTO collaboration.shared_experience_place (
           moment_id, sort_order, label, start_at, end_at
         ) VALUES ($1, $2, $3, $4::timestamptz, $5::timestamptz)`,
        [args.momentId, order++, place.label, place.startAt ?? null, place.endAt ?? null]
      );
    }
  }
}

export async function archiveMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expectedVersion: number
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_ARCHIVE', resourceType: 'MOMENT', momentId });
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment
     SET status = 'ARCHIVED', archived_at = COALESCE(archived_at, now()), version = version + 1, updated_at = now()
     WHERE moment_id = $1 AND version = $2 AND status IN ('ACTIVE', 'DRAFT', 'COMPLETED')
     RETURNING moment_id, domain_code, title, status, version`,
    [momentId, expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409);
  }
  const row = updated.rows[0];
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentArchived',
    domainCode: row.domain_code,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId, title: row.title, userId: ctx.userId },
  });
  await insertAudit(client, ctx, 'MOMENT_ARCHIVE', 'MOMENT', momentId, domainEventId, { momentId });
  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
  };
}

export async function cancelMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expectedVersion: number
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_CANCEL', resourceType: 'MOMENT', momentId });
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment
     SET status = 'CANCELLED', cancelled_at = COALESCE(cancelled_at, now()), version = version + 1, updated_at = now()
     WHERE moment_id = $1 AND version = $2 AND status IN ('ACTIVE', 'DRAFT')
     RETURNING moment_id, domain_code, title, status, version`,
    [momentId, expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409);
  }
  const row = updated.rows[0];
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentCancelled',
    domainCode: row.domain_code,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId, title: row.title, userId: ctx.userId },
  });
  await insertAudit(client, ctx, 'MOMENT_CANCEL', 'MOMENT', momentId, domainEventId, { momentId });
  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
  };
}

/**
 * Complete a moment (COMPLETED + completed_at) and queue Moment Story generation.
 * Used by Manage "Complete Chapter" and by end_at auto-complete.
 */
export async function completeMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expectedVersion: number | null,
  opts?: { auto?: boolean; skipStory?: boolean }
): Promise<MomentResult & { storyId?: string; completionId?: string }> {
  if (!opts?.auto) {
    await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_COMPLETE', resourceType: 'MOMENT', momentId });
    await assertMomentLifecycleLeader(client, ctx, momentId);
  }

  const existing = await client.query<{
    status: string;
    domain_code: string;
    title: string;
    version: string;
  }>(`SELECT status, domain_code, title, version::text FROM core.moment WHERE moment_id = $1`, [momentId]);
  const cur = existing.rows[0];
  if (!cur) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  if (cur.status === 'COMPLETED') {
    return {
      momentId,
      domainCode: cur.domain_code,
      title: cur.title,
      status: cur.status,
      version: parseInt(cur.version, 10),
    };
  }
  if (cur.status !== 'ACTIVE') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Only ACTIVE moments can be completed.', 400);
  }
  if (expectedVersion != null && parseInt(cur.version, 10) !== expectedVersion) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409);
  }

  const completionId = randomUUID();
  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment
     SET status = 'COMPLETED',
         completed_at = now(),
         version = version + 1,
         updated_at = now()
     WHERE moment_id = $1 AND status = 'ACTIVE'
     RETURNING moment_id, domain_code, title, status, version`,
    [momentId]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409);
  }
  const row = updated.rows[0];

  // Soft-close group context rows when present.
  await client.query(
    `UPDATE collaboration.group_moment_context SET status = 'COMPLETED', updated_at = now()
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  ).catch(() => undefined);
  await client.query(
    `UPDATE collaboration.shared_experience_context SET status = 'COMPLETED', updated_at = now()
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  ).catch(() => undefined);

  const { listOtherMemberUserIds } = await import('../collaboration/group-membership');
  const peers =
    row.domain_code === 'GROUP'
      ? await listOtherMemberUserIds(client, momentId, ctx.userId)
      : [];

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentCompleted',
    domainCode: row.domain_code,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: {
      momentId,
      title: row.title,
      completionId,
      userId: ctx.userId,
      auto: Boolean(opts?.auto),
      targetUserIds: peers,
    },
  });
  await insertAudit(client, ctx, 'MOMENT_COMPLETE', 'MOMENT', momentId, domainEventId, {
    momentId,
    completionId,
    auto: Boolean(opts?.auto),
  });

  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'moment_completed',
    eventId: domainEventId,
    momentId,
    momentDomain: row.domain_code === 'GROUP' ? 'group' : row.domain_code === 'BUSINESS' ? 'business' : 'personal',
    properties: { auto: Boolean(opts?.auto), completion_id: completionId },
  }).catch(() => undefined);

  let storyId: string | undefined;
  if (!opts?.skipStory && row.domain_code === 'GROUP') {
    const { queueMomentStoryGeneration, recordStoryGenerationFailure } = await import('../story/service');
    await client.query('SAVEPOINT moment_story');
    try {
      const story = await queueMomentStoryGeneration(client, ctx, {
        momentId,
        completionId,
        completedByUserId: ctx.userId,
      });
      storyId = story.storyId;
      await client.query('RELEASE SAVEPOINT moment_story');
    } catch (err) {
      try {
        await client.query('ROLLBACK TO SAVEPOINT moment_story');
      } catch {
        // Savepoint is already gone if the connection died.
      }
      const message = err instanceof Error ? err.message : 'Story generation failed';
      console.log(JSON.stringify({ level: 'warn', msg: 'moment_story_generation_failed', momentId, err: message }));
      await recordStoryGenerationFailure({
        momentId,
        completionId,
        completedByUserId: ctx.userId,
        message,
      }).catch((recordErr) => {
        console.log(JSON.stringify({ level: 'warn', msg: 'moment_story_failure_unrecorded', momentId, err: String(recordErr) }));
      });
    }
  }

  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
    storyId,
    completionId,
  };
}

/** Reopen a completed moment back to ACTIVE; prior Story versions are preserved. */
export async function reopenMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expectedVersion: number,
  reason?: string
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_REOPEN', resourceType: 'MOMENT', momentId });
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment
     SET status = 'ACTIVE',
         completed_at = NULL,
         version = version + 1,
         updated_at = now()
     WHERE moment_id = $1 AND version = $2 AND status = 'COMPLETED'
     RETURNING moment_id, domain_code, title, status, version`,
    [momentId, expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict or not completed.', 409);
  }
  const row = updated.rows[0];
  await client.query(
    `UPDATE collaboration.group_moment_context SET status = 'ACTIVE', updated_at = now()
     WHERE moment_id = $1`,
    [momentId]
  ).catch(() => undefined);
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MomentReopened',
    domainCode: row.domain_code,
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    payload: { momentId, reason: reason ?? null, userId: ctx.userId },
  });
  await insertAudit(client, ctx, 'MOMENT_REOPEN', 'MOMENT', momentId, domainEventId, {
    momentId,
    reason: reason ?? null,
  });
  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
  };
}

/**
 * Completion review scan — warnings vs hard blockers (MR1).
 * Outstanding settlement is a warning, not a blocker.
 */
export async function completionReview(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  momentId: string;
  eligible: boolean;
  warnings: Array<{ domain: string; code: string; message: string }>;
  blockers: Array<{ domain: string; code: string; message: string }>;
  summary: Record<string, number>;
}> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'GROUP_ACCESS', resourceType: 'MOMENT', momentId });
  const moment = await client.query<{ status: string; title: string }>(
    `SELECT status, title FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  if (!moment.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  const warnings: Array<{ domain: string; code: string; message: string }> = [];
  const blockers: Array<{ domain: string; code: string; message: string }> = [];
  if (moment.rows[0].status === 'COMPLETED') {
    blockers.push({ domain: 'authorization', code: 'ALREADY_COMPLETED', message: 'Moment is already completed.' });
  } else if (moment.rows[0].status !== 'ACTIVE') {
    blockers.push({
      domain: 'authorization',
      code: 'NOT_ACTIVE',
      message: `Moment status is ${moment.rows[0].status}.`,
    });
  }

  const people = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  );
  const pendingInvites = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM collaboration.group_invite
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0' }] }));
  if (parseInt(pendingInvites.rows[0]?.n ?? '0', 10) > 0) {
    warnings.push({
      domain: 'people',
      code: 'PENDING_INVITES',
      message: 'There are still open invitations.',
    });
  }

  const openPlans = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM collaboration.planning_item
     WHERE moment_id = $1 AND status IN ('OPEN', 'IN_PROGRESS')`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0' }] }));
  if (parseInt(openPlans.rows[0]?.n ?? '0', 10) > 0) {
    warnings.push({
      domain: 'plans',
      code: 'OPEN_PLANS',
      message: 'Some plans are still open.',
    });
  }

  const expensesScan = await client.query<{ n: string; total: string }>(
    `SELECT COUNT(*)::text AS n, COALESCE(SUM(e.amount), 0)::text AS total
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g ON g.expense_id = e.expense_id
     WHERE e.moment_id = $1 AND e.status = 'POSTED'`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0', total: '0' }] }));

  const memories = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM memory.memory WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0' }] }));

  if (parseInt(memories.rows[0]?.n ?? '0', 10) === 0) {
    warnings.push({
      domain: 'memory',
      code: 'NO_MEMORIES',
      message: 'No memories or photos yet — Story will use a non-photo layout.',
    });
  }

  return {
    momentId,
    eligible: blockers.length === 0,
    warnings,
    blockers,
    summary: {
      people: parseInt(people.rows[0]?.n ?? '0', 10),
      openPlans: parseInt(openPlans.rows[0]?.n ?? '0', 10),
      expenses: parseInt(expensesScan.rows[0]?.n ?? '0', 10),
      memories: parseInt(memories.rows[0]?.n ?? '0', 10),
    },
  };
}

/** Permanent product delete — status=DELETED (analytics may remain; avoids RESTRICT FK wipe). */
export async function deleteMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expectedVersion: number
): Promise<MomentResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_DELETE', resourceType: 'MOMENT', momentId });
  await assertMomentLifecycleLeader(client, ctx, momentId);
  const existing = await client.query<{ status: string }>(
    `SELECT status FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  if (!existing.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  if (existing.rows[0].status === 'DELETED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Moment is already deleted.', 400);
  }
  const updated = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `UPDATE core.moment SET status = 'DELETED', version = version + 1, updated_at = now()
     WHERE moment_id = $1 AND version = $2
     RETURNING moment_id, domain_code, title, status, version`,
    [momentId, expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Moment version conflict.', 409);
  }
  const row = updated.rows[0];
  return {
    momentId: row.moment_id,
    domainCode: row.domain_code,
    title: row.title,
    status: row.status,
    version: parseInt(row.version, 10),
  };
}

export async function getMoment(client: PoolClient, ctx: RequestContext, momentId: string): Promise<MomentResult | null> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_READ', resourceType: 'MOMENT', momentId });
  const row = await client.query<{ moment_id: string; domain_code: string; title: string; status: string; version: string }>(
    `SELECT moment_id, domain_code, title, status, version FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  if (!row.rows[0]) return null;
  const r = row.rows[0];
  return {
    momentId: r.moment_id,
    domainCode: r.domain_code,
    title: r.title,
    status: r.status,
    version: parseInt(r.version, 10),
  };
}

export function stubMomentCommand(_momentId: string, command: string): { commandId: string; status: string } {
  return { commandId: randomUUID(), status: `${command}_ACCEPTED` };
}
