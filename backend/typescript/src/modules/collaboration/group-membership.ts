import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';

export interface GroupMemberInfo {
  participantId: string;
  role: string;
}

export interface GroupParticipantRow {
  participantId: string;
  userId: string | null;
  roleCode: string;
  status: string;
  displayName: string | null;
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
    participant_role: string;
    status: string;
    metadata: { displayName?: string } | null;
    display_name: string | null;
  }>(
    `SELECT mp.participant_id, mp.user_id, mp.participant_role, mp.status, mp.metadata,
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
    participants: rows.rows.map((r) => ({
      participantId: r.participant_id,
      userId: r.user_id,
      roleCode: r.participant_role,
      status: r.status,
      displayName: r.display_name ?? r.metadata?.displayName ?? null,
    })),
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
