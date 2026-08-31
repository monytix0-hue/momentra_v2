import { randomBytes } from 'crypto';
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';

export const mintInviteSchema = z
  .object({
    title: z.string().min(1).max(500),
    momentTypeCode: z.string().min(1).max(80),
    /** When set, bind the invite to an existing trip so redeem joins this moment. */
    momentId: z.string().uuid().optional(),
  })
  .strict();

export type MintInviteInput = z.infer<typeof mintInviteSchema>;

export interface InviteResult {
  inviteId: string;
  inviteCode: string;
  invitePath: string;
  inviteUrl: string;
  status: string;
  title: string;
  momentTypeCode: string;
  momentId: string | null;
}

export interface RedeemInviteResult {
  inviteId: string;
  inviteCode: string;
  status: string;
  momentId: string | null;
  participantId: string | null;
  alreadyMember: boolean;
}

const DISPLAY_ORIGIN = 'https://momentra.app';
const CODE_ALPHABET = 'abcdefghjkmnpqrstuvwxyz23456789';

function toInviteViews(row: {
  invite_id: string;
  invite_code: string;
  status: string;
  title_snapshot: string;
  moment_type_code: string;
  moment_id: string | null;
}): InviteResult {
  const hostPath = `momentra.app/j/${row.invite_code}`;
  return {
    inviteId: row.invite_id,
    inviteCode: row.invite_code,
    invitePath: hostPath,
    inviteUrl: `${DISPLAY_ORIGIN}/j/${row.invite_code}`,
    status: row.status,
    title: row.title_snapshot,
    momentTypeCode: row.moment_type_code,
    momentId: row.moment_id,
  };
}

async function allocateCode(client: PoolClient): Promise<string> {
  for (let i = 0; i < 8; i++) {
    const bytes = randomBytes(8);
    let code = '';
    for (let n = 0; n < 8; n++) {
      code += CODE_ALPHABET[bytes[n] % CODE_ALPHABET.length];
    }
    const exists = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM collaboration.moment_invite WHERE invite_code = $1`,
      [code]
    );
    if (parseInt(exists.rows[0]?.n ?? '0', 10) === 0) return code;
  }
  throw new AppError(ErrorCode.VERSION_CONFLICT, 'Could not allocate a unique invite code.', 409);
}

async function loadGroupType(
  client: PoolClient,
  momentTypeCode: string
): Promise<{ momentTypeId: string; groupFamily: string }> {
  const momentType = await client.query<{ moment_type_id: string; category_code: string }>(
    `SELECT mt.moment_type_id, mc.code AS category_code
     FROM core.moment_type mt
     JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     WHERE mt.domain_code = 'GROUP' AND mt.code = $1 AND mt.status = 'ACTIVE'`,
    [momentTypeCode]
  );
  if (!momentType.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown group moment type: ${momentTypeCode}`, 400);
  }
  return {
    momentTypeId: momentType.rows[0].moment_type_id,
    groupFamily: momentType.rows[0].category_code,
  };
}

export async function mintInvite(
  client: PoolClient,
  ctx: RequestContext,
  body: MintInviteInput
): Promise<InviteResult> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MOMENT_CREATE', resourceType: 'MOMENT' });
  const type = await loadGroupType(client, body.momentTypeCode);
  const code = await allocateCode(client);

  let boundMomentId: string | null = null;
  let status: 'PENDING' | 'ACTIVE' = 'PENDING';
  if (body.momentId) {
    await assertGovernanceAllowed(client, ctx, {
      actionCode: 'PARTICIPANT_MANAGE',
      resourceType: 'PARTICIPANT',
      momentId: body.momentId,
    });
    const moment = await client.query<{ moment_id: string; title: string; type_code: string }>(
      `SELECT m.moment_id, m.title, mt.code AS type_code
       FROM core.moment m
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       WHERE m.moment_id = $1`,
      [body.momentId]
    );
    if (!moment.rows[0]) {
      throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
    }
    if (moment.rows[0].type_code !== body.momentTypeCode) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invite type does not match this moment.', 400);
    }
    boundMomentId = body.momentId;
    status = 'ACTIVE';
  }

  const inserted = await client.query<{
    invite_id: string;
    invite_code: string;
    status: string;
    title_snapshot: string;
    moment_type_code: string;
    moment_id: string | null;
  }>(
    `INSERT INTO collaboration.moment_invite (
       invite_code, created_by_user_id, moment_type_code, group_family, title_snapshot, status, moment_id
     ) VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING invite_id, invite_code, status, title_snapshot, moment_type_code, moment_id`,
    [
      code,
      ctx.userId,
      body.momentTypeCode,
      type.groupFamily,
      body.title.trim(),
      status,
      boundMomentId,
    ]
  );

  const result = toInviteViews(inserted.rows[0]!);
  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'GroupInviteMinted',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT_INVITE',
    aggregateId: result.inviteId,
    payload: {
      inviteCode: result.inviteCode,
      momentTypeCode: body.momentTypeCode,
      momentId: boundMomentId,
    },
  });
  return result;
}

export async function getInviteByCode(
  client: PoolClient,
  ctx: RequestContext,
  code: string
): Promise<InviteResult> {
  await expireIfNeeded(client, code);
  const row = await client.query<{
    invite_id: string;
    invite_code: string;
    status: string;
    title_snapshot: string;
    moment_type_code: string;
    moment_id: string | null;
  }>(
    `SELECT invite_id, invite_code, status, title_snapshot, moment_type_code, moment_id
     FROM collaboration.moment_invite
     WHERE invite_code = $1`,
    [code]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Invite not found.', 404);
  }
  void ctx;
  return toInviteViews(row.rows[0]);
}

async function expireIfNeeded(client: PoolClient, code: string): Promise<void> {
  await client.query(
    `UPDATE collaboration.moment_invite
     SET status = 'EXPIRED', updated_at = now(), version = version + 1
     WHERE invite_code = $1 AND status = 'PENDING' AND expires_at < now()`,
    [code]
  );
}

export async function redeemInvite(
  client: PoolClient,
  ctx: RequestContext,
  code: string
): Promise<RedeemInviteResult> {
  await expireIfNeeded(client, code);
  const invite = await client.query<{
    invite_id: string;
    status: string;
    created_by_user_id: string;
    moment_id: string | null;
  }>(
    `SELECT invite_id, status, created_by_user_id, moment_id
     FROM collaboration.moment_invite
     WHERE invite_code = $1
     FOR UPDATE`,
    [code]
  );
  if (!invite.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Invite not found.', 404);
  }
  const row = invite.rows[0];
  if (row.status === 'REVOKED' || row.status === 'EXPIRED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'This invite is no longer valid.', 410);
  }
  if (row.created_by_user_id === ctx.userId) {
    return {
      inviteId: row.invite_id,
      inviteCode: code,
      status: row.status,
      momentId: row.moment_id,
      participantId: null,
      alreadyMember: true,
    };
  }

  if (row.status === 'PENDING' || !row.moment_id) {
    await client.query(
      `INSERT INTO collaboration.moment_invite_claim (invite_id, user_id)
       VALUES ($1, $2)
       ON CONFLICT (invite_id, user_id) DO NOTHING`,
      [row.invite_id, ctx.userId]
    );
    return {
      inviteId: row.invite_id,
      inviteCode: code,
      status: 'PENDING',
      momentId: null,
      participantId: null,
      alreadyMember: false,
    };
  }

  const existing = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status IN ('INVITED','ACTIVE')
     LIMIT 1`,
    [row.moment_id, ctx.userId]
  );
  if (existing.rows[0]) {
    return {
      inviteId: row.invite_id,
      inviteCode: code,
      status: row.status,
      momentId: row.moment_id,
      participantId: existing.rows[0].participant_id,
      alreadyMember: true,
    };
  }

  const inserted = await client.query<{ participant_id: string }>(
    `INSERT INTO collaboration.moment_participant (
       moment_id, user_id, participant_role, status, joined_at, version
     ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
     ON CONFLICT DO NOTHING
     RETURNING participant_id`,
    [row.moment_id, ctx.userId]
  );
  const participantId =
    inserted.rows[0]?.participant_id ??
    (
      await client.query<{ participant_id: string }>(
        `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
        [row.moment_id, ctx.userId]
      )
    ).rows[0]?.participant_id ??
    null;

  await client.query(
    `INSERT INTO collaboration.moment_invite_claim (invite_id, user_id, participant_id)
     VALUES ($1, $2, $3)
     ON CONFLICT (invite_id, user_id) DO UPDATE SET participant_id = EXCLUDED.participant_id`,
    [row.invite_id, ctx.userId, participantId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'GroupInviteRedeemed',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT_INVITE',
    aggregateId: row.invite_id,
    scopeType: 'MOMENT',
    scopeId: row.moment_id,
    payload: { inviteCode: code, momentId: row.moment_id, participantId },
  });

  return {
    inviteId: row.invite_id,
    inviteCode: code,
    status: 'ACTIVE',
    momentId: row.moment_id,
    participantId,
    alreadyMember: false,
  };
}

export async function bindInviteToMoment(
  client: PoolClient,
  ctx: RequestContext,
  inviteCode: string,
  momentId: string,
  momentTypeCode: string,
  title: string
): Promise<void> {
  const invite = await client.query<{
    invite_id: string;
    status: string;
    created_by_user_id: string;
    moment_type_code: string;
    moment_id: string | null;
  }>(
    `SELECT invite_id, status, created_by_user_id, moment_type_code, moment_id
     FROM collaboration.moment_invite
     WHERE invite_code = $1
     FOR UPDATE`,
    [inviteCode]
  );
  if (!invite.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invite code was not found.', 400);
  }
  const row = invite.rows[0];
  if (row.created_by_user_id !== ctx.userId) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Invite does not belong to this user.', 403);
  }
  if (row.moment_type_code !== momentTypeCode) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invite type does not match this experience.', 400);
  }
  if (row.status === 'REVOKED' || row.status === 'EXPIRED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invite is no longer valid.', 400);
  }
  if (row.moment_id && row.moment_id !== momentId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invite is already bound to another moment.', 409);
  }

  await client.query(
    `UPDATE collaboration.moment_invite
     SET moment_id = $2,
         status = 'ACTIVE',
         title_snapshot = $3,
         updated_at = now(),
         version = version + 1
     WHERE invite_id = $1`,
    [row.invite_id, momentId, title]
  );

  const claims = await client.query<{ user_id: string; invite_claim_id: string }>(
    `SELECT user_id, invite_claim_id FROM collaboration.moment_invite_claim WHERE invite_id = $1`,
    [row.invite_id]
  );
  for (const claim of claims.rows) {
    if (claim.user_id === ctx.userId) continue;
    const inserted = await client.query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
       ON CONFLICT DO NOTHING
       RETURNING participant_id`,
      [momentId, claim.user_id]
    );
    const participantId =
      inserted.rows[0]?.participant_id ??
      (
        await client.query<{ participant_id: string }>(
          `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
          [momentId, claim.user_id]
        )
      ).rows[0]?.participant_id ??
      null;
    if (participantId) {
      await client.query(
        `UPDATE collaboration.moment_invite_claim SET participant_id = $2 WHERE invite_claim_id = $1`,
        [claim.invite_claim_id, participantId]
      );
    }
  }
}
