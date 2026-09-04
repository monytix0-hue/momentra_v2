import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { config } from '../../platform/config';

export interface GovernanceInput {
  actionCode: string;
  resourceType: string;
  resourceId?: string;
  momentId?: string;
  companyId?: string;
  /** Target user for Personal-scope checks (must equal actor for Personal). */
  ownerUserId?: string;
}

export type GovernanceDecision = { allowed: true } | { allowed: false; reason: string };

const FAIL_CLOSED_ACTIONS = new Set(['EXPENSE_CREATE', 'SETTLEMENT_RECORD']);

/**
 * Authorize an action against canonical backend state.
 * Client-supplied userId/companyId/groupId are never trusted as authority —
 * they are request scope; membership is resolved from PostgreSQL.
 */
export async function authorize(
  client: PoolClient,
  ctx: RequestContext,
  input: GovernanceInput
): Promise<GovernanceDecision> {
  if (FAIL_CLOSED_ACTIONS.has(input.actionCode) && !config.governanceFailOpen) {
    const policyCount = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM governance.policy p
       JOIN governance.policy_version pv ON pv.policy_id = p.policy_id
       WHERE p.status = 'ACTIVE' AND pv.status = 'ACTIVE'`
    );
    if (parseInt(policyCount.rows[0]?.n ?? '0', 10) === 0) {
      return { allowed: false, reason: `Governance policies are not active for ${input.actionCode}.` };
    }
  }

  if (input.ownerUserId && input.ownerUserId !== ctx.userId) {
    return { allowed: false, reason: 'Cannot act on another user Personal context.' };
  }

  if (input.momentId) {
    const access = await client.query<{
      ok: boolean;
      domain_code: string | null;
      moment_type_id: string | null;
    }>(
      `SELECT
         EXISTS (
           SELECT 1 FROM core.moment m
           LEFT JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
           LEFT JOIN collaboration.moment_participant mp
             ON mp.moment_id = m.moment_id AND mp.user_id = $1 AND mp.status = 'ACTIVE'
           LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
           LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
           LEFT JOIN business.company_membership cm
             ON cm.company_id = bmc.company_id AND cm.user_id = $1 AND cm.status = 'ACTIVE'
           WHERE m.moment_id = $2
             AND (
               (m.domain_code = 'PERSONAL' AND (pmc.user_id = $1 OR m.created_by_user_id = $1))
               OR (m.domain_code = 'GROUP' AND (gmc.organizer_user_id = $1 OR mp.participant_id IS NOT NULL))
               OR (m.domain_code = 'BUSINESS' AND cm.company_membership_id IS NOT NULL)
               OR m.created_by_user_id = $1
             )
         ) AS ok,
         (SELECT domain_code FROM core.moment WHERE moment_id = $2) AS domain_code,
         (SELECT moment_type_id FROM core.moment WHERE moment_id = $2) AS moment_type_id`,
      [ctx.userId, input.momentId]
    );
    if (!access.rows[0]?.ok) {
      return { allowed: false, reason: 'Not permitted for this moment scope.' };
    }

    // V019 contract: catalog capabilities must be mapped on the moment type.
    const catalog = await client.query<{ capability_id: string }>(
      `SELECT capability_id FROM core.capability
       WHERE code = $1 AND status = 'ACTIVE'
       LIMIT 1`,
      [input.actionCode]
    );
    if (catalog.rows[0] && access.rows[0].moment_type_id) {
      const mapped = await resolveCapabilityForMomentType(
        client,
        access.rows[0].moment_type_id,
        input.actionCode
      );
      if (!mapped) {
        return { allowed: false, reason: 'Capability not enabled for this moment type.' };
      }
    }
  }

  if (input.companyId) {
    const membership = await client.query<{ ok: boolean }>(
      `SELECT EXISTS (
         SELECT 1 FROM business.company_membership cm
         WHERE cm.company_id = $2 AND cm.user_id = $1 AND cm.status = 'ACTIVE'
       ) AS ok`,
      [ctx.userId, input.companyId]
    );
    if (!membership.rows[0]?.ok) {
      return { allowed: false, reason: 'Not an active company member.' };
    }
  }

  return { allowed: true };
}

export async function assertGovernanceAllowed(
  client: PoolClient,
  ctx: RequestContext,
  input: GovernanceInput
): Promise<void> {
  const decision = await authorize(client, ctx, input);
  if (!decision.allowed) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, decision.reason, 403);
  }
}

/**
 * Fail-closed policy presence check only.
 * Use after a stronger membership/scope assert already proved access (avoids
 * re-running redundant moment/company EXISTS queries).
 */
export async function assertFailClosedPolicies(
  client: PoolClient,
  actionCode: string
): Promise<void> {
  if (!FAIL_CLOSED_ACTIONS.has(actionCode) || config.governanceFailOpen) {
    return;
  }
  const policyCount = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM governance.policy p
     JOIN governance.policy_version pv ON pv.policy_id = p.policy_id
     WHERE p.status = 'ACTIVE' AND pv.status = 'ACTIVE'`
  );
  if (parseInt(policyCount.rows[0]?.n ?? '0', 10) === 0) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      `Governance policies are not active for ${actionCode}.`,
      403
    );
  }
}

/** Personal context: actor may only operate as themselves. */
export async function assertPersonalOwner(
  client: PoolClient,
  ctx: RequestContext,
  ownerUserId: string
): Promise<void> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PERSONAL_ACCESS',
    resourceType: 'USER',
    ownerUserId,
  });
}

/** Group moment: active participant or organizer. */
export async function assertGroupMomentAccess(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<void> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'GROUP_ACCESS',
    resourceType: 'MOMENT',
    momentId,
  });
}

/** Business company: active membership required. */
export async function assertCompanyMember(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<void> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_ACCESS',
    resourceType: 'COMPANY',
    companyId,
  });
}

export async function resolveCapabilityForMomentType(
  client: PoolClient,
  momentTypeId: string,
  actionCode: string
): Promise<boolean> {
  const r = await client.query<{ ok: boolean }>(
    `SELECT EXISTS (
       SELECT 1 FROM core.moment_type_capability mtc
       JOIN core.capability c ON c.capability_id = mtc.capability_id
       WHERE mtc.moment_type_id = $1 AND c.code = $2 AND mtc.status = 'ACTIVE'
     ) AS ok`,
    [momentTypeId, actionCode]
  );
  return r.rows[0]?.ok === true;
}

/**
 * Invite link mint + manual participant add for GROUP moments.
 * Shared Living types use RESIDENT_MANAGE in the catalog; Trip/Experience use PARTICIPANT_MANAGE.
 * Accept either when the moment belongs to SHARED_LIVING.
 */
export async function assertGroupPeopleManageAllowed(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<void> {
  const familyRow = await client.query<{ category_code: string }>(
    `SELECT mc.code AS category_code
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  const family = familyRow.rows[0]?.category_code;
  if (family === 'SHARED_LIVING') {
    const resident = await authorize(client, ctx, {
      actionCode: 'RESIDENT_MANAGE',
      resourceType: 'RESIDENT',
      momentId,
    });
    if (resident.allowed) return;
    const participant = await authorize(client, ctx, {
      actionCode: 'PARTICIPANT_MANAGE',
      resourceType: 'PARTICIPANT',
      momentId,
    });
    if (participant.allowed) return;
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      participant.reason ?? resident.reason ?? 'Not permitted for this moment scope.',
      403
    );
  }
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PARTICIPANT_MANAGE',
    resourceType: 'PARTICIPANT',
    momentId,
  });
}

const GROUP_LEADER_ROLES = new Set(['ORGANIZER', 'CO_ORGANIZER']);
const COMPANY_LEADER_TYPES = new Set(['OWNER', 'ADMIN']);

async function loadMomentLeadership(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  domain_code: string;
  organizer_user_id: string | null;
  participant_role: string | null;
  membership_type: string | null;
} | null> {
  const row = await client.query<{
    domain_code: string;
    organizer_user_id: string | null;
    participant_role: string | null;
    membership_type: string | null;
  }>(
    `SELECT m.domain_code,
            gmc.organizer_user_id,
            mp.participant_role,
            cm.membership_type
     FROM core.moment m
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     LEFT JOIN collaboration.moment_participant mp
       ON mp.moment_id = m.moment_id AND mp.user_id = $2 AND mp.status = 'ACTIVE'
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
     LEFT JOIN business.company_membership cm
       ON cm.company_id = bmc.company_id AND cm.user_id = $2 AND cm.status = 'ACTIVE'
     WHERE m.moment_id = $1`,
    [momentId, ctx.userId]
  );
  return row.rows[0] ?? null;
}

function isMomentLeader(r: {
  domain_code: string;
  organizer_user_id: string | null;
  participant_role: string | null;
  membership_type: string | null;
}, userId: string): boolean {
  if (r.domain_code === 'PERSONAL') return true;
  if (r.domain_code === 'GROUP') {
    const isOrganizerUser = r.organizer_user_id === userId;
    const isLeaderRole = r.participant_role != null && GROUP_LEADER_ROLES.has(r.participant_role);
    return isOrganizerUser || isLeaderRole;
  }
  if (r.domain_code === 'BUSINESS') {
    return r.membership_type != null && COMPANY_LEADER_TYPES.has(r.membership_type);
  }
  return false;
}

/**
 * Group/Business moment lifecycle (archive/cancel/delete) is leader-only.
 * Personal remains any owner with moment access (authorize already scoped).
 */
export async function assertMomentLifecycleLeader(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<void> {
  const r = await loadMomentLeadership(client, ctx, momentId);
  if (!r) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Moment not found.', 404);
  }
  if (r.domain_code === 'PERSONAL') {
    return;
  }
  if (r.domain_code === 'GROUP') {
    if (isMomentLeader(r, ctx.userId)) return;
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Only organizers can archive, cancel, or delete this group moment.',
      403
    );
  }
  if (r.domain_code === 'BUSINESS') {
    if (isMomentLeader(r, ctx.userId)) return;
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Only company owners or admins can archive, cancel, or delete this business moment.',
      403
    );
  }
  throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not permitted for this moment scope.', 403);
}

/** True if caller may close a poll (creator, group organizer, or company admin). */
export async function canClosePoll(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  createdByUserId: string
): Promise<boolean> {
  if (createdByUserId === ctx.userId) return true;
  const r = await loadMomentLeadership(client, ctx, momentId);
  if (!r) return false;
  return isMomentLeader(r, ctx.userId);
}

/** Poll close is restricted to the poll creator, group organizers, or company admins. */
export async function assertPollCloseAllowed(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  createdByUserId: string
): Promise<void> {
  if (await canClosePoll(client, ctx, momentId, createdByUserId)) return;
  throw new AppError(
    ErrorCode.GOVERNANCE_DENIED,
    'Only the poll creator, organizers, or admins can close this poll.',
    403
  );
}
