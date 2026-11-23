import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';

export interface CompanyMemberInfo {
  companyId: string;
  membershipId: string;
  membershipType: string;
  userId: string;
}

/** Re-export canonical access policy (callers: projection, business-reads, operating, finance). */
export type {
  BusinessMomentScope,
  BusinessAccessPath,
  MomentAccessMode,
} from './access-policy';
export {
  assertCompanyMomentAccess,
  evaluateCompanyMomentAccess,
  assertActiveCompanyMemberForMoment,
  assertFinanceVisible,
  assertCompanyFinanceVisible,
  redactActivityItem,
  redactLegacyFinanceProjection,
  resolveBusinessViewerRole,
  BUSINESS_MOMENT_LIST_ACCESS_SQL,
} from './access-policy';
import type { BusinessMomentScope } from './access-policy';

/** Khata, finance, and company records stay on a company. A moment with no company cannot use them. */
export function requireCompanyBoundMoment(
  scope: BusinessMomentScope
): asserts scope is BusinessMomentScope & { companyId: string } {
  if (!scope.companyId) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'This record stays with a company.',
      409
    );
  }
}

export async function assertActiveCompanyMember(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<CompanyMemberInfo> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_ACCESS',
    resourceType: 'COMPANY',
    companyId,
  });
  const row = await client.query<{
    company_membership_id: string;
    membership_type: string;
  }>(
    `SELECT company_membership_id, membership_type
     FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active company member.', 403);
  }
  return {
    companyId,
    membershipId: row.rows[0].company_membership_id,
    membershipType: row.rows[0].membership_type,
    userId: ctx.userId,
  };
}

/** Company observers can read. They cannot post expenses, revenue, or invoices. */
export function assertNotCompanyObserver(membershipType: string): void {
  if (membershipType.toUpperCase() === 'OBSERVER') {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Observers can view but not create or edit.',
      403
    );
  }
}

/** Approve rights via membership_type mapped through governance action — not native enum alone. */
export async function assertCanApproveCompanyFinance(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<CompanyMemberInfo> {
  const member = await assertActiveCompanyMember(client, ctx, companyId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'COMPANY',
    companyId,
  });
  if (member.membershipType !== 'OWNER' && member.membershipType !== 'ADMIN') {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Approval requires OWNER or ADMIN membership with COMPANY_UPDATE capability.',
      403
    );
  }
  return member;
}

export async function listCompanyMembers(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<{
  companyId: string;
  members: Array<{
    membershipId: string;
    userId: string;
    membershipType: string;
    status: string;
    displayName: string | null;
  }>;
}> {
  await assertActiveCompanyMember(client, ctx, companyId);
  const rows = await client.query<{
    company_membership_id: string;
    user_id: string;
    membership_type: string;
    status: string;
    display_name: string | null;
  }>(
    `SELECT cm.company_membership_id, cm.user_id, cm.membership_type, cm.status, up.display_name
     FROM business.company_membership cm
     LEFT JOIN core.user_profile up ON up.user_id = cm.user_id
     WHERE cm.company_id = $1
     ORDER BY cm.membership_type, cm.joined_at NULLS LAST, cm.created_at`,
    [companyId]
  );
  return {
    companyId,
    members: rows.rows.map((r) => ({
      membershipId: r.company_membership_id,
      userId: r.user_id,
      membershipType: r.membership_type,
      status: r.status,
      displayName: r.display_name,
    })),
  };
}

export const addCompanyMemberSchema = z
  .object({
    userId: z.string().uuid(),
    membershipType: z.enum(['ADMIN', 'MEMBER', 'CONTRACTOR', 'OBSERVER']).default('MEMBER'),
  })
  .strict();

export async function addCompanyMember(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof addCompanyMemberSchema>
): Promise<{ membershipId: string; companyId: string; userId: string; membershipType: string; status: string }> {
  const actor = await assertActiveCompanyMember(client, ctx, companyId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'COMPANY',
    companyId,
  });
  if (actor.membershipType !== 'OWNER' && actor.membershipType !== 'ADMIN') {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Only OWNER or ADMIN may add members.', 403);
  }

  const target = await client.query<{ user_id: string }>(
    `SELECT user_id FROM core.user_profile WHERE user_id = $1 AND status = 'ACTIVE'`,
    [body.userId]
  );
  if (!target.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Target user not found.', 404);
  }

  const existing = await client.query<{ company_membership_id: string; status: string }>(
    `SELECT company_membership_id, status FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2
       AND status IN ('INVITED', 'ACTIVE')`,
    [companyId, body.userId]
  );
  if (existing.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'User already has open membership on this company.', 409);
  }

  const inserted = await client.query<{ company_membership_id: string }>(
    `INSERT INTO business.company_membership (
       company_id, user_id, membership_type, status, joined_at, version
     ) VALUES ($1, $2, $3, 'ACTIVE', now(), 1)
     RETURNING company_membership_id`,
    [companyId, body.userId, body.membershipType]
  );
  const membershipId = inserted.rows[0]!.company_membership_id;

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyMemberAdded',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY',
    aggregateId: companyId,
    scopeType: 'COMPANY',
    scopeId: companyId,
    payload: { companyId, membershipId, userId: body.userId, membershipType: body.membershipType },
  });

  return {
    membershipId,
    companyId,
    userId: body.userId,
    membershipType: body.membershipType,
    status: 'ACTIVE',
  };
}

export const leaveCompanySchema = z
  .object({
    transferUserId: z.string().uuid().optional(),
  })
  .strict();

export type LeaveCompanyInput = z.infer<typeof leaveCompanySchema>;

/**
 * Self-leave a company. OWNER/ADMIN must transfer leadership to another ACTIVE member first.
 */
export async function leaveCompany(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: LeaveCompanyInput
): Promise<{ companyId: string; status: 'LEFT'; transferredToUserId: string | null }> {
  const me = await assertActiveCompanyMember(client, ctx, companyId);
  const isLeader = me.membershipType === 'OWNER' || me.membershipType === 'ADMIN';

  const others = await client.query<{ user_id: string; membership_type: string }>(
    `SELECT user_id, membership_type
     FROM business.company_membership
     WHERE company_id = $1 AND status = 'ACTIVE' AND user_id <> $2`,
    [companyId, ctx.userId]
  );

  let transferredToUserId: string | null = null;

  if (isLeader) {
    if (others.rows.length === 0) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Invite another member and transfer admin before leaving, or keep the company.',
        400
      );
    }
    if (!body.transferUserId) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'Owners and admins must select another member before leaving.',
        400
      );
    }
    const successor = others.rows.find((r) => r.user_id === body.transferUserId);
    if (!successor) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        'transferUserId must be another ACTIVE company member.',
        400
      );
    }
    const promoteTo =
      me.membershipType === 'OWNER'
        ? 'OWNER'
        : successor.membership_type === 'OWNER'
          ? 'OWNER'
          : 'ADMIN';
    await client.query(
      `UPDATE business.company_membership
       SET membership_type = $3, updated_at = now(), version = version + 1
       WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [companyId, body.transferUserId, promoteTo]
    );
    if (me.membershipType === 'OWNER' && promoteTo === 'OWNER') {
      // Demote other owners to ADMIN so there is a single primary owner after transfer.
      await client.query(
        `UPDATE business.company_membership
         SET membership_type = 'ADMIN', updated_at = now(), version = version + 1
         WHERE company_id = $1
           AND user_id <> $2
           AND user_id <> $3
           AND membership_type = 'OWNER'
           AND status = 'ACTIVE'`,
        [companyId, body.transferUserId, ctx.userId]
      );
    }
    transferredToUserId = body.transferUserId;
  }

  await client.query(
    `UPDATE business.company_membership
     SET status = 'LEFT', updated_at = now(), version = version + 1
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, ctx.userId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyMemberLeft',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY',
    aggregateId: companyId,
    scopeType: 'COMPANY',
    scopeId: companyId,
    payload: { companyId, userId: ctx.userId, transferredToUserId },
  });

  return { companyId, status: 'LEFT', transferredToUserId };
}

export const transferCompanyOwnershipSchema = z
  .object({
    transferUserId: z.string().uuid(),
  })
  .strict();

export type TransferCompanyOwnershipInput = z.infer<typeof transferCompanyOwnershipSchema>;

/**
 * OWNER transfers primary ownership to another ACTIVE member and stays as ADMIN.
 */
export async function transferCompanyOwnership(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: TransferCompanyOwnershipInput
): Promise<{ companyId: string; ownerUserId: string; previousOwnerUserId: string }> {
  const me = await assertActiveCompanyMember(client, ctx, companyId);
  if (me.membershipType !== 'OWNER') {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Only the company owner can transfer ownership.',
      403
    );
  }
  if (body.transferUserId === ctx.userId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Pick another member to become owner.', 400);
  }

  const target = await client.query<{ user_id: string; membership_type: string }>(
    `SELECT user_id, membership_type
     FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, body.transferUserId]
  );
  if (!target.rows[0]) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'transferUserId must be another ACTIVE company member.',
      400
    );
  }

  await client.query(
    `UPDATE business.company_membership
     SET membership_type = 'OWNER', updated_at = now(), version = version + 1
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, body.transferUserId]
  );
  await client.query(
    `UPDATE business.company_membership
     SET membership_type = 'ADMIN', updated_at = now(), version = version + 1
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, ctx.userId]
  );
  // Keep a single primary owner.
  await client.query(
    `UPDATE business.company_membership
     SET membership_type = 'ADMIN', updated_at = now(), version = version + 1
     WHERE company_id = $1
       AND user_id <> $2
       AND user_id <> $3
       AND membership_type = 'OWNER'
       AND status = 'ACTIVE'`,
    [companyId, body.transferUserId, ctx.userId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyOwnershipTransferred',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY',
    aggregateId: companyId,
    scopeType: 'COMPANY',
    scopeId: companyId,
    payload: {
      companyId,
      previousOwnerUserId: ctx.userId,
      ownerUserId: body.transferUserId,
    },
  });

  return {
    companyId,
    ownerUserId: body.transferUserId,
    previousOwnerUserId: ctx.userId,
  };
}
