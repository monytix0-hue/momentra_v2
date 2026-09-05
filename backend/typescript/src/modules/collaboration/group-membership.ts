import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { recordCommandSideEffects } from '../../platform/events/outbox';
import { emitLeanBusinessEvent, loadMomentTaxonomy } from '../analytics/lean-events';

export interface GroupMemberInfo {
  participantId: string;
  role: string;
}

export interface GroupParticipantRow {
  participantId: string;
  userId: string | null;
  roleCode: string;
  roleLabel: string;
  status: string;
  displayName: string | null;
  isGuest: boolean;
}

function roleLabelFor(roleCode: string, isGuest: boolean): string {
  if (isGuest) return 'Guest';
  switch (roleCode) {
    case 'ORGANIZER':
      return 'Organizer';
    case 'CO_ORGANIZER':
      return 'Co-organizer';
    case 'RESIDENT':
      return 'Resident';
    case 'CONTRIBUTOR':
      return 'Contributor';
    case 'OBSERVER':
      return 'Viewer';
    default:
      return 'Member';
  }
}

/** Assert caller is an ACTIVE participant on a GROUP moment. */
export async function assertGroupMember(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<GroupMemberInfo> {
  const row = await client.query<{
    participant_id: string;
    participant_role: string;
  }>(
    `SELECT mp.participant_id, mp.participant_role
     FROM collaboration.moment_participant mp
     JOIN core.moment m ON m.moment_id = mp.moment_id
     JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     WHERE mp.moment_id = $1
       AND mp.user_id = $2
       AND mp.status = 'ACTIVE'
       AND m.domain_code = 'GROUP'`,
    [momentId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active member of this group moment.', 403);
  }
  return {
    participantId: row.rows[0].participant_id,
    role: row.rows[0].participant_role,
  };
}

/** List participants for a GROUP moment when caller is an active member. */
export async function listGroupParticipants(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ momentId: string; participants: GroupParticipantRow[] }> {
  await assertGroupMember(client, ctx, momentId);

  const rows = await client.query<{
    participant_id: string;
    user_id: string | null;
    external_party_id: string | null;
    participant_role: string;
    status: string;
    metadata: { displayName?: string } | null;
    display_name: string | null;
  }>(
    `SELECT mp.participant_id, mp.user_id, mp.external_party_id, mp.participant_role, mp.status, mp.metadata,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName') AS display_name
     FROM collaboration.moment_participant mp
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE mp.moment_id = $1
     ORDER BY mp.participant_role, mp.created_at, mp.participant_id`,
    [momentId]
  );

  return {
    momentId,
    participants: rows.rows.map((r) => {
      const isGuest = r.external_party_id != null;
      return {
        participantId: r.participant_id,
        userId: r.user_id,
        roleCode: r.participant_role,
        roleLabel: roleLabelFor(r.participant_role, isGuest),
        status: r.status,
        displayName: r.display_name ?? r.metadata?.displayName ?? null,
        isGuest,
      };
    }),
  };
}

/** Ensure participantIds exist on moment with ACTIVE or INVITED status (same-moment only). */
export async function assertParticipantsOnMoment(
  client: PoolClient,
  momentId: string,
  participantIds: string[]
): Promise<void> {
  const unique = [...new Set(participantIds)];
  const rows = await client.query<{ participant_id: string }>(
    `SELECT participant_id
     FROM collaboration.moment_participant
     WHERE moment_id = $1
       AND participant_id = ANY($2::uuid[])
       AND status IN ('ACTIVE', 'INVITED')`,
    [momentId, unique]
  );
  if (rows.rows.length !== unique.length) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'All participants must be ACTIVE or INVITED members of the same moment.',
      400
    );
  }
}

const LEADER_ROLES = new Set(['ORGANIZER', 'CO_ORGANIZER']);

export const leaveGroupMomentSchema = z
  .object({
    transferUserId: z.string().uuid().optional(),
  })
  .strict();

export type LeaveGroupMomentInput = z.infer<typeof leaveGroupMomentSchema>;

/**
 * Self-leave a Group moment. Leaders must transfer ORGANIZER to another ACTIVE user first.
 */
export async function leaveGroupMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: LeaveGroupMomentInput
): Promise<{ momentId: string; status: 'LEFT'; transferredToUserId: string | null }> {
  const me = await assertGroupMember(client, ctx, momentId);

  const ctxRow = await client.query<{ organizer_user_id: string }>(
    `SELECT organizer_user_id FROM collaboration.group_moment_context WHERE moment_id = $1`,
    [momentId]
  );
  if (!ctxRow.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Group moment context not found.', 404);
  }

  const isLeader =
    LEADER_ROLES.has(me.role) || ctxRow.rows[0].organizer_user_id === ctx.userId;

  const others = await client.query<{ user_id: string; participant_role: string }>(
    `SELECT user_id, participant_role
     FROM collaboration.moment_participant
     WHERE moment_id = $1
       AND status = 'ACTIVE'
       AND user_id IS NOT NULL
       AND user_id <> $2`,
    [momentId, ctx.userId]
  );

  let transferredToUserId: string | null = null;

  if (isLeader) {
    if (others.rows.length === 0) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Invite another member and transfer organizer before leaving, or delete the moment.',
        400
      );
    }
    if (!body.transferUserId) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Organizers must select another member to become organizer before leaving.',
        400
      );
    }
    const successor = others.rows.find((r) => r.user_id === body.transferUserId);
    if (!successor) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'transferUserId must be another ACTIVE participant on this moment.',
        400
      );
    }
    await client.query(
      `UPDATE collaboration.moment_participant
       SET participant_role = 'ORGANIZER', updated_at = now(), version = version + 1
       WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [momentId, body.transferUserId]
    );
    await client.query(
      `UPDATE collaboration.group_moment_context
       SET organizer_user_id = $2, updated_at = now(), version = version + 1
       WHERE moment_id = $1`,
      [momentId, body.transferUserId]
    );
    transferredToUserId = body.transferUserId;
  }

  await client.query(
    `UPDATE collaboration.moment_participant
     SET status = 'LEFT',
         participant_role = CASE
           WHEN participant_role IN ('ORGANIZER', 'CO_ORGANIZER') THEN 'PARTICIPANT'
           ELSE participant_role
         END,
         updated_at = now(),
         version = version + 1
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [momentId, ctx.userId]
  );

  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupParticipantLeft',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      momentId,
      userId: ctx.userId,
      transferredToUserId,
    },
    auditActionCode: 'PARTICIPANT_LEAVE',
    auditResourceType: 'PARTICIPANT',
    auditResourceId: me.participantId,
    afterSnapshot: { momentId, status: 'LEFT', transferredToUserId },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_MEMBER_LEFT',
      title: transferredToUserId ? 'Organizer transferred and left' : 'Member left',
      payload: { userId: ctx.userId, transferredToUserId },
    },
  });

  const tax = await loadMomentTaxonomy(client, momentId);
  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'participant_exited',
    eventId: domainEventId,
    momentId,
    momentDomain: tax?.domain ?? 'group',
    momentCategory: tax?.category,
    momentType: tax?.type,
    properties: {
      exit_type: 'left',
      initiated_by_role: 'self',
      transfer_user_id: transferredToUserId,
    },
  });

  return { momentId, status: 'LEFT', transferredToUserId };
}

const MANAGEABLE_ROLES = [
  'ORGANIZER',
  'CO_ORGANIZER',
  'PARTICIPANT',
  'RESIDENT',
  'CONTRIBUTOR',
  'OBSERVER',
] as const;

export const updateGroupParticipantRoleSchema = z
  .object({
    roleCode: z.enum(MANAGEABLE_ROLES),
  })
  .strict();

export type UpdateGroupParticipantRoleInput = z.infer<typeof updateGroupParticipantRoleSchema>;

export function assertCallerIsOrganizer(me: GroupMemberInfo): void {
  if (!LEADER_ROLES.has(me.role)) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Only organizers can manage members.', 403);
  }
}

async function countActiveOrganizers(client: PoolClient, momentId: string): Promise<number> {
  const rows = await client.query<{ c: string }>(
    `SELECT COUNT(*)::text AS c
     FROM collaboration.moment_participant
     WHERE moment_id = $1
       AND status = 'ACTIVE'
       AND participant_role IN ('ORGANIZER', 'CO_ORGANIZER')`,
    [momentId]
  );
  return Number(rows.rows[0]?.c ?? 0);
}

async function loadActiveParticipant(
  client: PoolClient,
  momentId: string,
  participantId: string
): Promise<{
  participantId: string;
  userId: string | null;
  roleCode: string;
}> {
  const row = await client.query<{
    participant_id: string;
    user_id: string | null;
    participant_role: string;
  }>(
    `SELECT participant_id, user_id, participant_role
     FROM collaboration.moment_participant
     WHERE moment_id = $1 AND participant_id = $2 AND status = 'ACTIVE'`,
    [momentId, participantId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Active participant not found.', 404);
  }
  return {
    participantId: row.rows[0].participant_id,
    userId: row.rows[0].user_id,
    roleCode: row.rows[0].participant_role,
  };
}

async function loadRemovableParticipant(
  client: PoolClient,
  momentId: string,
  participantId: string
): Promise<{
  participantId: string;
  userId: string | null;
  roleCode: string;
  status: string;
}> {
  const row = await client.query<{
    participant_id: string;
    user_id: string | null;
    participant_role: string;
    status: string;
  }>(
    `SELECT participant_id, user_id, participant_role, status
     FROM collaboration.moment_participant
     WHERE moment_id = $1 AND participant_id = $2 AND status IN ('ACTIVE', 'INVITED')`,
    [momentId, participantId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Participant not found.', 404);
  }
  return {
    participantId: row.rows[0].participant_id,
    userId: row.rows[0].user_id,
    roleCode: row.rows[0].participant_role,
    status: row.rows[0].status,
  };
}

/**
 * Organizer updates another (or self) ACTIVE participant's role.
 * Cannot demote the sole remaining organizer.
 */
export async function updateGroupParticipantRole(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  participantId: string,
  body: UpdateGroupParticipantRoleInput
): Promise<{ momentId: string; participantId: string; roleCode: string }> {
  const me = await assertGroupMember(client, ctx, momentId);
  assertCallerIsOrganizer(me);

  const target = await loadActiveParticipant(client, momentId, participantId);
  const nextRole = body.roleCode;
  const wasLeader = LEADER_ROLES.has(target.roleCode);
  const willBeLeader = LEADER_ROLES.has(nextRole);

  if (wasLeader && !willBeLeader) {
    const organizers = await countActiveOrganizers(client, momentId);
    if (organizers <= 1) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Cannot demote the only organizer. Promote someone else first.',
        400
      );
    }
  }

  await client.query(
    `UPDATE collaboration.moment_participant
     SET participant_role = $3, updated_at = now(), version = version + 1
     WHERE moment_id = $1 AND participant_id = $2 AND status = 'ACTIVE'`,
    [momentId, participantId, nextRole]
  );

  if (nextRole === 'ORGANIZER' && target.userId) {
    await client.query(
      `UPDATE collaboration.group_moment_context
       SET organizer_user_id = $2, updated_at = now(), version = version + 1
       WHERE moment_id = $1`,
      [momentId, target.userId]
    );
  }

  await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupParticipantRoleUpdated',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      momentId,
      participantId,
      previousRole: target.roleCode,
      roleCode: nextRole,
      actorUserId: ctx.userId,
    },
    auditActionCode: 'PARTICIPANT_ROLE_UPDATE',
    auditResourceType: 'PARTICIPANT',
    auditResourceId: participantId,
    afterSnapshot: { momentId, participantId, roleCode: nextRole },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_MEMBER_ROLE_CHANGED',
      title: 'Member role updated',
      payload: { participantId, roleCode: nextRole },
    },
  });

  return { momentId, participantId, roleCode: nextRole };
}

/**
 * Organizer removes another ACTIVE or INVITED participant (status REMOVED).
 * Cannot remove self or sole organizer.
 */
export async function removeGroupParticipant(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  participantId: string
): Promise<{ momentId: string; participantId: string; status: 'REMOVED' }> {
  const me = await assertGroupMember(client, ctx, momentId);
  assertCallerIsOrganizer(me);

  if (me.participantId === participantId) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Use leave to exit the group yourself.',
      400
    );
  }

  const target = await loadRemovableParticipant(client, momentId, participantId);
  if (LEADER_ROLES.has(target.roleCode) && target.status === 'ACTIVE') {
    const organizers = await countActiveOrganizers(client, momentId);
    if (organizers <= 1) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Cannot remove the only organizer.',
        400
      );
    }
  }

  await client.query(
    `UPDATE collaboration.moment_participant
     SET status = 'REMOVED',
         participant_role = CASE
           WHEN participant_role IN ('ORGANIZER', 'CO_ORGANIZER') THEN 'PARTICIPANT'
           ELSE participant_role
         END,
         updated_at = now(),
         version = version + 1
     WHERE moment_id = $1 AND participant_id = $2 AND status IN ('ACTIVE', 'INVITED')`,
    [momentId, participantId]
  );

  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupParticipantRemoved',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      momentId,
      participantId,
      removedUserId: target.userId,
      actorUserId: ctx.userId,
    },
    auditActionCode: 'PARTICIPANT_REMOVE',
    auditResourceType: 'PARTICIPANT',
    auditResourceId: participantId,
    afterSnapshot: { momentId, participantId, status: 'REMOVED' },
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_MEMBER_REMOVED',
      title: 'Member removed',
      payload: { participantId, removedUserId: target.userId },
    },
  });

  const tax = await loadMomentTaxonomy(client, momentId);
  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'participant_exited',
    eventId: domainEventId,
    momentId,
    momentDomain: tax?.domain ?? 'group',
    momentCategory: tax?.category,
    momentType: tax?.type,
    properties: {
      exit_type: 'removed',
      initiated_by_role: 'organizer',
      removed_participant_id: participantId,
    },
  });

  return { momentId, participantId, status: 'REMOVED' };
}
