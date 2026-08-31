import { randomBytes } from 'crypto';
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { assertActiveCompanyMember } from './membership';

export const mintCompanyInviteSchema = z
  .object({
    companyId: z.string().uuid(),
    membershipType: z.enum(['ADMIN', 'MEMBER', 'CONTRACTOR', 'OBSERVER']).default('MEMBER'),
  })
  .strict();

export type MintCompanyInviteInput = z.infer<typeof mintCompanyInviteSchema>;

export interface CompanyInviteResult {
  inviteId: string;
  inviteCode: string;
  invitePath: string;
  inviteUrl: string;
  status: string;
  title: string;
  companyId: string;
  membershipType: string;
  expiresAt: string;
}

export interface RedeemCompanyInviteResult {
  inviteId: string;
  inviteCode: string;
  status: string;
  companyId: string;
  membershipId: string | null;
  membershipType: string;
  alreadyMember: boolean;
}

const DISPLAY_ORIGIN = 'https://momentra.app';
const CODE_ALPHABET = 'abcdefghjkmnpqrstuvwxyz23456789';

function toInviteViews(row: {
  invite_id: string;
  invite_code: string;
  status: string;
  title_snapshot: string;
  company_id: string;
  membership_type: string;
  expires_at: Date | string;
}): CompanyInviteResult {
  const hostPath = `momentra.app/c/${row.invite_code}`;
  const expiresAt =
    row.expires_at instanceof Date ? row.expires_at.toISOString() : String(row.expires_at);
  return {
    inviteId: row.invite_id,
    inviteCode: row.invite_code,
    invitePath: hostPath,
    inviteUrl: `${DISPLAY_ORIGIN}/c/${row.invite_code}`,
    status: row.status,
    title: row.title_snapshot,
    companyId: row.company_id,
    membershipType: row.membership_type,
    expiresAt,
  };
}

async function allocateCode(client: PoolClient): Promise<string> {
  for (let i = 0; i < 8; i++) {
    const bytes = randomBytes(8);
    let code = '';
    for (let n = 0; n < 8; n++) {
      code += CODE_ALPHABET[bytes[n]! % CODE_ALPHABET.length];
    }
    const exists = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.company_invite WHERE invite_code = $1`,
      [code]
    );
    if (parseInt(exists.rows[0]?.n ?? '0', 10) === 0) return code;
  }
  throw new AppError(ErrorCode.VERSION_CONFLICT, 'Could not allocate a unique invite code.', 409);
}

async function expireIfNeeded(client: PoolClient, code: string): Promise<void> {
  await client.query(
    `UPDATE business.company_invite
     SET status = 'EXPIRED', updated_at = now(), version = version + 1
     WHERE invite_code = $1 AND status = 'ACTIVE' AND expires_at < now()`,
    [code]
  );
}

export async function mintCompanyInvite(
  client: PoolClient,
  ctx: RequestContext,
  body: MintCompanyInviteInput
): Promise<CompanyInviteResult> {
  const actor = await assertActiveCompanyMember(client, ctx, body.companyId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'COMPANY',
    companyId: body.companyId,
  });
  if (actor.membershipType !== 'OWNER' && actor.membershipType !== 'ADMIN') {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Only OWNER or ADMIN may mint company invites.', 403);
  }

  const company = await client.query<{ company_id: string; display_name: string; status: string }>(
    `SELECT company_id, display_name, status FROM business.company WHERE company_id = $1`,
    [body.companyId]
  );
  if (!company.rows[0] || company.rows[0].status !== 'ACTIVE') {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Company not found.', 404);
  }

  const code = await allocateCode(client);
  const inserted = await client.query<{
    invite_id: string;
    invite_code: string;
    status: string;
    title_snapshot: string;
    company_id: string;
    membership_type: string;
    expires_at: Date;
  }>(
    `INSERT INTO business.company_invite (
       invite_code, created_by_user_id, company_id, membership_type, title_snapshot, status, version
     ) VALUES ($1, $2, $3, $4, $5, 'ACTIVE', 1)
     RETURNING invite_id, invite_code, status, title_snapshot, company_id, membership_type, expires_at`,
    [code, ctx.userId, body.companyId, body.membershipType, company.rows[0].display_name]
  );
  const row = inserted.rows[0]!;

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyInviteMinted',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY_INVITE',
    aggregateId: row.invite_id,
    scopeType: 'COMPANY',
    scopeId: body.companyId,
    payload: {
      inviteCode: code,
      companyId: body.companyId,
      membershipType: body.membershipType,
    },
  });

  return toInviteViews(row);
}

export async function getCompanyInviteByCode(
  client: PoolClient,
  ctx: RequestContext,
  code: string
): Promise<CompanyInviteResult> {
  await expireIfNeeded(client, code);
  const invite = await client.query<{
    invite_id: string;
    invite_code: string;
    status: string;
    title_snapshot: string;
    company_id: string;
    membership_type: string;
    expires_at: Date;
  }>(
    `SELECT invite_id, invite_code, status, title_snapshot, company_id, membership_type, expires_at
     FROM business.company_invite
     WHERE invite_code = $1`,
    [code]
  );
  if (!invite.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Invite not found.', 404);
  }
  // Authenticated preview — no membership required to view invite metadata.
  void ctx;
  return toInviteViews(invite.rows[0]);
}

export async function redeemCompanyInvite(
  client: PoolClient,
  ctx: RequestContext,
  code: string
): Promise<RedeemCompanyInviteResult> {
  await expireIfNeeded(client, code);
  const invite = await client.query<{
    invite_id: string;
    status: string;
    created_by_user_id: string;
    company_id: string;
    membership_type: string;
  }>(
    `SELECT invite_id, status, created_by_user_id, company_id, membership_type
     FROM business.company_invite
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

  const existing = await client.query<{ company_membership_id: string }>(
    `SELECT company_membership_id FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status IN ('INVITED', 'ACTIVE')
     LIMIT 1`,
    [row.company_id, ctx.userId]
  );
  if (existing.rows[0] || row.created_by_user_id === ctx.userId) {
    const membershipId = existing.rows[0]?.company_membership_id ?? null;
    await client.query(
      `INSERT INTO business.company_invite_claim (invite_id, user_id, company_membership_id)
       VALUES ($1, $2, $3)
       ON CONFLICT (invite_id, user_id) DO UPDATE
         SET company_membership_id = COALESCE(EXCLUDED.company_membership_id, business.company_invite_claim.company_membership_id)`,
      [row.invite_id, ctx.userId, membershipId]
    );
    return {
      inviteId: row.invite_id,
      inviteCode: code,
      status: row.status,
      companyId: row.company_id,
      membershipId,
      membershipType: row.membership_type,
      alreadyMember: true,
    };
  }

  const inserted = await client.query<{ company_membership_id: string }>(
    `INSERT INTO business.company_membership (
       company_id, user_id, membership_type, status, joined_at, version
     ) VALUES ($1, $2, $3, 'ACTIVE', now(), 1)
     RETURNING company_membership_id`,
    [row.company_id, ctx.userId, row.membership_type]
  );
  const membershipId = inserted.rows[0]!.company_membership_id;

  await client.query(
    `INSERT INTO business.company_invite_claim (invite_id, user_id, company_membership_id)
     VALUES ($1, $2, $3)
     ON CONFLICT (invite_id, user_id) DO UPDATE SET company_membership_id = EXCLUDED.company_membership_id`,
    [row.invite_id, ctx.userId, membershipId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyInviteRedeemed',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY_INVITE',
    aggregateId: row.invite_id,
    scopeType: 'COMPANY',
    scopeId: row.company_id,
    payload: {
      inviteCode: code,
      companyId: row.company_id,
      membershipId,
      membershipType: row.membership_type,
    },
  });

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyMemberAdded',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY',
    aggregateId: row.company_id,
    scopeType: 'COMPANY',
    scopeId: row.company_id,
    payload: {
      companyId: row.company_id,
      membershipId,
      userId: ctx.userId,
      membershipType: row.membership_type,
      viaInvite: true,
    },
  });

  return {
    inviteId: row.invite_id,
    inviteCode: code,
    status: 'ACTIVE',
    companyId: row.company_id,
    membershipId,
    membershipType: row.membership_type,
    alreadyMember: false,
  };
}
