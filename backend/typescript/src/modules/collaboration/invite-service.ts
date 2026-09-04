import { randomBytes } from 'crypto';
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed, assertGroupPeopleManageAllowed } from '../governance/resolver';
import { recordCommandSideEffects } from '../../platform/events/outbox';
import { emitLeanBusinessEvent, loadMomentTaxonomy } from '../analytics/lean-events';

export const mintInviteSchema = z
  .object({
    title: z.string().min(1).max(500),
    momentTypeCode: z.string().min(1).max(80),
    /** When set, bind the invite to an existing group moment so redeem joins this moment. */
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
  /** ACTIVE participant user ids to notify (host + members). */
  notifyUserIds?: string[];
}

/** Digits-only phone key; compare last 10 for national match. */
export function normalizePhoneDigits(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const digits = raw.replace(/\D/g, '');
  if (digits.length < 7) return null;
  return digits.length > 10 ? digits.slice(-10) : digits;
}

function phonesMatch(a: string | null | undefined, b: string | null | undefined): boolean {
  const na = normalizePhoneDigits(a);
  const nb = normalizePhoneDigits(b);
  return Boolean(na && nb && na === nb);
}

/**
 * Promote INVITED external stubs whose metadata.phone matches the redeemer into the user participant.
 * Returns the promoted participant_id when a stub was merged; otherwise null.
 */
async function mergeMatchingInvitedPhoneStub(
  client: PoolClient,
  momentId: string,
  userId: string
): Promise<string | null> {
  const profile = await client.query<{ phone: string | null; display_name: string | null }>(
    `SELECT phone, display_name FROM core.user_profile WHERE user_id = $1`,
    [userId]
  );
  const userPhone = profile.rows[0]?.phone ?? null;
  if (!userPhone) return null;

  const stubs = await client.query<{
    participant_id: string;
    external_party_id: string;
    metadata: { phone?: string | null } | null;
  }>(
    `SELECT participant_id, external_party_id, metadata
     FROM collaboration.moment_participant
     WHERE moment_id = $1
       AND status = 'INVITED'
       AND external_party_id IS NOT NULL
       AND user_id IS NULL
     FOR UPDATE`,
    [momentId]
  );

  const match = stubs.rows.find((s) => phonesMatch(s.metadata?.phone ?? null, userPhone));
  if (!match) return null;

  // Identity CHECK: exactly one of user_id / external_party_id — flip in one UPDATE.
  const promoted = await client.query<{ participant_id: string }>(
    `UPDATE collaboration.moment_participant
     SET user_id = $2,
         external_party_id = NULL,
         status = 'ACTIVE',
         joined_at = COALESCE(joined_at, now()),
         invited_at = COALESCE(invited_at, now()),
         version = version + 1,
         updated_at = now()
     WHERE participant_id = $1
       AND moment_id = $3
     RETURNING participant_id`,
    [match.participant_id, userId, momentId]
  );

  if (promoted.rows[0] && match.external_party_id) {
    await client.query(
      `UPDATE core.external_party
       SET status = 'MERGED', updated_at = now()
       WHERE external_party_id = $1`,
      [match.external_party_id]
    );
  }

  return promoted.rows[0]?.participant_id ?? null;
}

async function listActiveParticipantUserIds(
  client: PoolClient,
  momentId: string
): Promise<string[]> {
  const rows = await client.query<{ user_id: string }>(
    `SELECT user_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE' AND user_id IS NOT NULL`,
    [momentId]
  );
  return rows.rows.map((r) => r.user_id);
}

async function fanOutMomentActivity(
  client: PoolClient,
  sourceEventId: string,
  momentId: string,
  actorUserId: string,
  activityCode: string,
  title: string,
  payload: Record<string, unknown>
): Promise<string[]> {
  const userIds = await listActiveParticipantUserIds(client, momentId);
  for (const uid of userIds) {
    await client.query(
      `INSERT INTO projection.recent_activity (
         user_id, source_event_id, domain_code, scope_type, scope_id,
         activity_code, title, occurred_at, activity_payload, projection_version
       ) VALUES ($1, $2, 'GROUP', 'MOMENT', $3::uuid, $4, $5, now(), $6::jsonb, 1)
       ON CONFLICT (user_id, source_event_id) DO NOTHING`,
      [uid, sourceEventId, momentId, activityCode, title, JSON.stringify(payload)]
    );
  }
  return userIds;
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
    await assertGroupPeopleManageAllowed(client, ctx, body.momentId);
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
  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupInviteMinted',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT_INVITE',
    aggregateId: result.inviteId,
    scopeType: boundMomentId ? 'MOMENT' : 'USER',
    scopeId: boundMomentId ?? ctx.userId,
    payload: {
      inviteCode: result.inviteCode,
      momentTypeCode: body.momentTypeCode,
      momentId: boundMomentId,
    },
    auditActionCode: 'INVITE_MINT',
    auditResourceType: 'MOMENT_INVITE',
    auditResourceId: result.inviteId,
    afterSnapshot: result,
    activity: boundMomentId
      ? {
          domainCode: 'GROUP',
          momentId: boundMomentId,
          activityCode: 'GROUP_INVITE_SENT',
          title: 'Invite link created',
          payload: { inviteCode: result.inviteCode, momentId: boundMomentId },
        }
      : undefined,
  });
  if (boundMomentId) {
    await fanOutMomentActivity(
      client,
      domainEventId,
      boundMomentId,
      ctx.userId,
      'GROUP_INVITE_SENT',
      'Invite link created',
      { inviteCode: result.inviteCode, momentId: boundMomentId }
    );
    const tax = await loadMomentTaxonomy(client, boundMomentId);
    await emitLeanBusinessEvent(client, ctx, {
      eventName: 'participant_invited',
      eventId: domainEventId,
      momentId: boundMomentId,
      momentDomain: tax?.domain ?? 'group',
      momentCategory: tax?.category,
      momentType: tax?.type ?? body.momentTypeCode,
      properties: {
        invite_id: result.inviteId,
        invite_channel: 'link',
        invitee_user_status: 'unknown',
        invited_role: 'PARTICIPANT',
      },
    });
  }
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
    // Still try to merge phone stubs so duplicates clear on re-redeem.
    await mergeMatchingInvitedPhoneStub(client, row.moment_id, ctx.userId);
    return {
      inviteId: row.invite_id,
      inviteCode: code,
      status: row.status,
      momentId: row.moment_id,
      participantId: existing.rows[0].participant_id,
      alreadyMember: true,
      notifyUserIds: await listActiveParticipantUserIds(client, row.moment_id),
    };
  }

  let participantId = await mergeMatchingInvitedPhoneStub(client, row.moment_id, ctx.userId);

  if (!participantId) {
    const inserted = await client.query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
       ON CONFLICT DO NOTHING
       RETURNING participant_id`,
      [row.moment_id, ctx.userId]
    );
    participantId =
      inserted.rows[0]?.participant_id ??
      (
        await client.query<{ participant_id: string }>(
          `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
          [row.moment_id, ctx.userId]
        )
      ).rows[0]?.participant_id ??
      null;
  }

  await client.query(
    `INSERT INTO collaboration.moment_invite_claim (invite_id, user_id, participant_id)
     VALUES ($1, $2, $3)
     ON CONFLICT (invite_id, user_id) DO UPDATE SET participant_id = EXCLUDED.participant_id`,
    [row.invite_id, ctx.userId, participantId]
  );

  const displayName = (
    await client.query<{ display_name: string | null }>(
      `SELECT display_name FROM core.user_profile WHERE user_id = $1`,
      [ctx.userId]
    )
  ).rows[0]?.display_name;

  const { domainEventId } = await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupInviteRedeemed',
    domainCode: 'GROUP',
    aggregateType: 'MOMENT_INVITE',
    aggregateId: row.invite_id,
    scopeType: 'MOMENT',
    scopeId: row.moment_id,
    payload: { inviteCode: code, momentId: row.moment_id, participantId },
    auditActionCode: 'INVITE_REDEEM',
    auditResourceType: 'MOMENT_INVITE',
    auditResourceId: row.invite_id,
    afterSnapshot: { inviteCode: code, momentId: row.moment_id, participantId },
    activity: {
      domainCode: 'GROUP',
      momentId: row.moment_id,
      activityCode: 'GROUP_MEMBER_JOINED',
      title: displayName ? `${displayName} joined` : 'Someone joined the group',
      payload: { inviteCode: code, momentId: row.moment_id, participantId, userId: ctx.userId },
    },
  });

  const notifyUserIds = await fanOutMomentActivity(
    client,
    domainEventId,
    row.moment_id,
    ctx.userId,
    'GROUP_MEMBER_JOINED',
    displayName ? `${displayName} joined` : 'Someone joined the group',
    { inviteCode: code, momentId: row.moment_id, participantId, userId: ctx.userId }
  );

  const tax = await loadMomentTaxonomy(client, row.moment_id);
  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'participant_joined',
    eventId: domainEventId,
    momentId: row.moment_id,
    momentDomain: tax?.domain ?? 'group',
    momentCategory: tax?.category,
    momentType: tax?.type,
    properties: {
      invite_id: row.invite_id,
      participant_role: 'PARTICIPANT',
      join_source: 'invite_redeem',
      was_existing_user: true,
    },
  });

  return {
    inviteId: row.invite_id,
    inviteCode: code,
    status: 'ACTIVE',
    momentId: row.moment_id,
    participantId,
    alreadyMember: false,
    notifyUserIds,
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
    let participantId: string | null =
      (
        await client.query<{ participant_id: string }>(
          `SELECT participant_id FROM collaboration.moment_participant
           WHERE moment_id = $1 AND user_id = $2 AND status IN ('INVITED','ACTIVE')
           LIMIT 1`,
          [momentId, claim.user_id]
        )
      ).rows[0]?.participant_id ?? null;

    if (!participantId) {
      participantId = await mergeMatchingInvitedPhoneStub(client, momentId, claim.user_id);
    }

    if (!participantId) {
      const inserted = await client.query<{ participant_id: string }>(
        `INSERT INTO collaboration.moment_participant (
           moment_id, user_id, participant_role, status, joined_at, version
         ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)
         ON CONFLICT DO NOTHING
         RETURNING participant_id`,
        [momentId, claim.user_id]
      );
      participantId =
        inserted.rows[0]?.participant_id ??
        (
          await client.query<{ participant_id: string }>(
            `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
            [momentId, claim.user_id]
          )
        ).rows[0]?.participant_id ??
        null;
    }
    if (participantId) {
      await client.query(
        `UPDATE collaboration.moment_invite_claim SET participant_id = $2 WHERE invite_claim_id = $1`,
        [claim.invite_claim_id, participantId]
      );
      const displayName = (
        await client.query<{ display_name: string | null }>(
          `SELECT display_name FROM core.user_profile WHERE user_id = $1`,
          [claim.user_id]
        )
      ).rows[0]?.display_name;
      const { domainEventId } = await recordCommandSideEffects(client, {
        ...ctx,
        userId: claim.user_id,
      }, {
        eventName: 'GroupInviteRedeemed',
        domainCode: 'GROUP',
        aggregateType: 'MOMENT_INVITE',
        aggregateId: row.invite_id,
        scopeType: 'MOMENT',
        scopeId: momentId,
        payload: { inviteCode, momentId, participantId, via: 'bind' },
        auditActionCode: 'INVITE_REDEEM',
        auditResourceType: 'MOMENT_INVITE',
        auditResourceId: row.invite_id,
        afterSnapshot: { inviteCode, momentId, participantId },
        activity: {
          domainCode: 'GROUP',
          momentId,
          activityCode: 'GROUP_MEMBER_JOINED',
          title: displayName ? `${displayName} joined` : 'Someone joined the group',
          payload: { inviteCode, momentId, participantId, userId: claim.user_id },
        },
      });
      await fanOutMomentActivity(
        client,
        domainEventId,
        momentId,
        claim.user_id,
        'GROUP_MEMBER_JOINED',
        displayName ? `${displayName} joined` : 'Someone joined the group',
        { inviteCode, momentId, participantId, userId: claim.user_id }
      );
    }
  }
}
