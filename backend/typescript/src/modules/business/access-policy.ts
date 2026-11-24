/**
 * Business access policy: company membership vs Moment membership vs Moment grants,
 * plus server-side finance visibility for every projection path (including legacy RUNWAY).
 *
 * Policy (company-bound Moments):
 * - COMPANY_WIDE_ROLE (OWNER|ADMIN): company-wide Moment access; no Moment grant required.
 * - COMPANY_OPERATING_GRANT (membership.operating_role set): company-wide operational grant.
 * - COMPANY_OPEN (default momentAccess): any active company member.
 * - RESTRICTED momentAccess: MEMBER/CONTRACTOR/OBSERVER need Moment membership
 *   (moment_participant) or an explicit Moment access grant (setup preferences.participants).
 *
 * Company-less Moments: creator or active moment_participant only.
 *
 * Finance visibility is independent of access: OWNER_ADMIN / FINANCE / APPROVER (and matching
 * company membership / operating_role / participant role) see amounts; others get redacted
 * payloads and finance list endpoints are denied.
 *
 * Callers: membership.ts (re-export), projection/service.ts, business-reads.ts, operating-service.ts.
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import {
  financeVisibleFor,
  type OperatingRole,
  OPERATING_ROLES,
} from './operating-catalog';

export type BusinessAccessPath =
  | 'COMPANY_WIDE_ROLE'
  | 'COMPANY_OPERATING_GRANT'
  | 'MOMENT_MEMBERSHIP'
  | 'MOMENT_ACCESS_GRANT'
  | 'COMPANY_OPEN'
  | 'COMPANY_LESS'
  | 'CREATOR_DRAFT';

export type MomentAccessMode = 'COMPANY' | 'RESTRICTED';

export interface BusinessMomentScope {
  momentId: string;
  companyId: string | null;
  businessFamily: string;
  membershipType: string;
  membershipId: string | null;
  /** Invite/membership operating_role when set (company-wide grant). */
  operatingRole: string | null;
  accessPath: BusinessAccessPath;
  momentAccess: MomentAccessMode;
  viewerRole: OperatingRole;
  financeVisible: boolean;
}

const COMPANY_WIDE = new Set(['OWNER', 'ADMIN']);
const OPERATING_ROLE_CODES = new Set(OPERATING_ROLES.map((r) => r.code));

export function asOperatingRole(raw: string | null | undefined): OperatingRole | null {
  if (!raw) return null;
  const code = raw.toUpperCase() as OperatingRole;
  return OPERATING_ROLE_CODES.has(code) ? code : null;
}

/**
 * Resolve the viewer role used for finance visibility and Quick Add gates.
 * Order: setup participant role → OWNER/ADMIN → membership.operating_role → OBSERVER → TEAM_MEMBER.
 */
export function resolveBusinessViewerRole(args: {
  userId: string;
  membershipType: string;
  operatingRole?: string | null;
  participants?: Array<{ userId: string; role: string }>;
}): OperatingRole {
  const match = (args.participants ?? []).find((p) => p.userId === args.userId);
  if (match) {
    return asOperatingRole(match.role) ?? 'TEAM_MEMBER';
  }
  const membership = (args.membershipType ?? 'MEMBER').toUpperCase();
  if (COMPANY_WIDE.has(membership)) return 'OWNER_ADMIN';
  const fromGrant = asOperatingRole(args.operatingRole);
  if (fromGrant) return fromGrant;
  if (membership === 'OBSERVER') return 'OBSERVER';
  return 'TEAM_MEMBER';
}

export function momentAccessModeOf(preferences: Record<string, unknown> | null | undefined): MomentAccessMode {
  const raw = String(preferences?.momentAccess ?? preferences?.accessMode ?? 'COMPANY').toUpperCase();
  return raw === 'RESTRICTED' ? 'RESTRICTED' : 'COMPANY';
}

function participantGrantMatch(
  preferences: Record<string, unknown> | null | undefined,
  userId: string
): boolean {
  const list = preferences?.participants;
  if (!Array.isArray(list)) return false;
  return list.some((p) => {
    if (!p || typeof p !== 'object') return false;
    const rec = p as Record<string, unknown>;
    return String(rec.userId ?? '') === userId;
  });
}

function participantsFromPrefs(
  preferences: Record<string, unknown> | null | undefined
): Array<{ userId: string; role: string }> {
  const list = preferences?.participants;
  if (!Array.isArray(list)) return [];
  return list.flatMap((p) => {
    if (!p || typeof p !== 'object') return [];
    const rec = p as Record<string, unknown>;
    const userId = String(rec.userId ?? '');
    if (!userId) return [];
    return [{ userId, role: String(rec.role ?? 'TEAM_MEMBER') }];
  });
}

/**
 * Non-throwing evaluation of canonical Business Moment access.
 * Used by authorize() and assertCompanyMomentAccess so every path shares one policy.
 */
export async function evaluateCompanyMomentAccess(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<BusinessMomentScope | null> {
  const row = await client.query<{
    company_id: string | null;
    business_family: string;
    membership_id: string | null;
    membership_type: string | null;
    operating_role: string | null;
    participant_id: string | null;
    created_by_user_id: string;
    bmc_status: string;
    preferences: Record<string, unknown> | null;
  }>(
    `SELECT bmc.company_id, bmc.business_family, bmc.status AS bmc_status,
            cm.company_membership_id AS membership_id, cm.membership_type,
            cm.operating_role,
            mp.participant_id,
            m.created_by_user_id,
            (
              SELECT bss.preferences
              FROM business.business_system_setup bss
              WHERE bss.moment_id = bmc.moment_id AND bss.status = 'ACTIVE'
              ORDER BY bss.created_at DESC
              LIMIT 1
            ) AS preferences
     FROM business.business_moment_context bmc
     JOIN core.moment m ON m.moment_id = bmc.moment_id AND m.domain_code = 'BUSINESS'
     LEFT JOIN business.company_membership cm
       ON cm.company_id = bmc.company_id AND cm.user_id = $2 AND cm.status = 'ACTIVE'
     LEFT JOIN collaboration.moment_participant mp
       ON mp.moment_id = bmc.moment_id AND mp.user_id = $2 AND mp.status = 'ACTIVE'
     WHERE bmc.moment_id = $1
       AND (
         bmc.status = 'ACTIVE'
         OR (bmc.status = 'DRAFT' AND m.created_by_user_id = $2)
       )`,
    [momentId, ctx.userId]
  );

  if (!row.rows[0]) {
    return null;
  }

  const r = row.rows[0];
  const prefs = r.preferences ?? {};
  const momentAccess = momentAccessModeOf(prefs);
  const membershipType = (r.membership_type ?? 'ORGANIZER').toUpperCase();
  const operatingRole = r.operating_role;
  const isCreator = r.created_by_user_id === ctx.userId;
  const hasMembership = r.membership_id != null;
  const hasMomentMembership = r.participant_id != null;
  const hasMomentGrant = participantGrantMatch(prefs, ctx.userId);

  let accessPath: BusinessAccessPath | null = null;

  if (r.bmc_status === 'DRAFT' && isCreator) {
    accessPath = 'CREATOR_DRAFT';
  } else if (r.company_id == null) {
    if (hasMomentMembership || isCreator) {
      accessPath = 'COMPANY_LESS';
    }
  } else if (!hasMembership) {
    accessPath = null;
  } else if (COMPANY_WIDE.has(membershipType)) {
    accessPath = 'COMPANY_WIDE_ROLE';
  } else if (asOperatingRole(operatingRole)) {
    accessPath = 'COMPANY_OPERATING_GRANT';
  } else if (momentAccess === 'COMPANY') {
    accessPath = 'COMPANY_OPEN';
  } else if (hasMomentMembership) {
    accessPath = 'MOMENT_MEMBERSHIP';
  } else if (hasMomentGrant) {
    accessPath = 'MOMENT_ACCESS_GRANT';
  }

  if (!accessPath) {
    return null;
  }

  const viewerRole = resolveBusinessViewerRole({
    userId: ctx.userId,
    membershipType,
    operatingRole,
    participants: participantsFromPrefs(prefs),
  });

  return {
    momentId,
    companyId: r.company_id,
    businessFamily: r.business_family,
    membershipType,
    membershipId: r.membership_id,
    operatingRole,
    accessPath,
    momentAccess,
    viewerRole,
    financeVisible: financeVisibleFor(viewerRole),
  };
}

/**
 * Canonical Business Moment access. Authorization runs before any projection/finance read.
 */
export async function assertCompanyMomentAccess(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<BusinessMomentScope> {
  const scope = await evaluateCompanyMomentAccess(client, ctx, momentId);
  if (!scope) {
    // Company-bound Moments keep the company-membership error the API contract documents.
    const bound = await client.query<{ company_id: string | null }>(
      `SELECT company_id FROM business.business_moment_context WHERE moment_id = $1`,
      [momentId]
    );
    const isCompanyBound = bound.rows[0]?.company_id != null;
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      isCompanyBound ? 'Not an active company member.' : 'Not a member of this business moment.',
      403
    );
  }
  return scope;
}

/** Company-bound writers: active company membership on the moment's company. */
export async function assertActiveCompanyMemberForMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<BusinessMomentScope & { companyId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  if (!scope.companyId || !scope.membershipId) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Active company membership is required for this action.',
      403
    );
  }
  return scope as BusinessMomentScope & { companyId: string };
}

/** Company-scoped finance visibility (khata, company exports) without a Moment id. */
export async function assertCompanyFinanceVisible(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<void> {
  const row = await client.query<{ membership_type: string; operating_role: string | null }>(
    `SELECT membership_type, operating_role
     FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not an active company member.', 403);
  }
  const viewerRole = resolveBusinessViewerRole({
    userId: ctx.userId,
    membershipType: row.rows[0].membership_type,
    operatingRole: row.rows[0].operating_role,
  });
  if (!financeVisibleFor(viewerRole)) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Finance data is not available for this role.',
      403
    );
  }
}

/** Deny finance list/export/alternate endpoints when the role cannot see finance. */
export function assertFinanceVisible(scope: BusinessMomentScope): void {
  if (!scope.financeVisible) {
    throw new AppError(
      ErrorCode.GOVERNANCE_DENIED,
      'Finance data is not available for this role.',
      403
    );
  }
}

const FINANCE_ACTIVITY = /EXPENSE|REVENUE|INVOICE|PAYMENT|COLLECTION|BUDGET|FINANCE|SPEND/i;
const FINANCE_KEYS = new Set([
  'amount',
  'currencyCode',
  'currency_code',
  'expenseId',
  'expense_id',
  'revenueId',
  'revenue_id',
  'invoiceId',
  'invoice_id',
  'operatingEventId',
  'sourceId',
  'source_id',
  'totalAmount',
  'subtotalAmount',
  'taxAmount',
  'expenseTotal',
  'revenueTotal',
  'invoiceOutstandingTotal',
]);

/** Strip amounts and source IDs from activity payloads for restricted roles. */
export function redactActivityItem<T extends { activityCode: string; activityPayload?: Record<string, unknown> }>(
  item: T,
  financeVisible: boolean
): T {
  if (financeVisible) return item;
  const payload = { ...(item.activityPayload ?? {}) };
  for (const key of Object.keys(payload)) {
    if (FINANCE_KEYS.has(key) || /amount|total|price|cost|balance/i.test(key)) {
      delete payload[key];
    }
  }
  if (FINANCE_ACTIVITY.test(item.activityCode)) {
    return { ...item, activityPayload: {} };
  }
  return { ...item, activityPayload: payload };
}

/** Legacy RUNWAY / company finance projection redaction. */
export function redactLegacyFinanceProjection(
  result: Record<string, unknown>,
  financeVisible: boolean
): Record<string, unknown> {
  if (financeVisible) {
    return { ...result, financeVisible: true };
  }

  const facet = result.facet;
  const base: Record<string, unknown> = {
    momentId: result.momentId,
    facet,
    title: result.title,
    companyId: result.companyId,
    businessFamily: result.businessFamily,
    financeVisible: false,
    status: result.status === 'EMPTY' ? 'EMPTY' : 'OK',
  };

  if (facet === 'finance') {
    return {
      ...base,
      payload: {
        dataQuality: 'RESTRICTED',
        financeVisible: false,
        totals: [],
        snapshotPayload: {},
      },
    };
  }

  const payload = (result.payload ?? {}) as Record<string, unknown>;
  return {
    ...base,
    payload: {
      dataQuality: payload.dataQuality ?? 'OK',
      financeVisible: false,
      attentionCount: payload.attentionCount ?? 0,
      activeMomentCount: payload.activeMomentCount ?? 0,
      runwayMonths: null,
      financialHealthScore: null,
      widgetPayload: {},
      finance: {
        dataQuality: 'RESTRICTED',
        financeVisible: false,
        totals: [],
        snapshotPayload: {},
      },
      activity: Array.isArray(payload.activity)
        ? (payload.activity as Array<{ activityCode: string; activityPayload?: Record<string, unknown> }>).map(
            (a) => redactActivityItem(a, false)
          )
        : [],
    },
  };
}

/** SQL predicate: caller may list this Business moment. Aliases: bmc, m, cm, mp. Param $1 = userId. */
export const BUSINESS_MOMENT_LIST_ACCESS_SQL = `
  (
    bmc.company_id IS NULL
    AND (mp.participant_id IS NOT NULL OR m.created_by_user_id = $1)
  )
  OR (
    bmc.company_id IS NOT NULL
    AND cm.company_membership_id IS NOT NULL
    AND (
      UPPER(cm.membership_type) IN ('OWNER', 'ADMIN')
      OR cm.operating_role IS NOT NULL
      OR COALESCE(
           (
             SELECT UPPER(COALESCE(bss.preferences->>'momentAccess', bss.preferences->>'accessMode', 'COMPANY'))
             FROM business.business_system_setup bss
             WHERE bss.moment_id = bmc.moment_id AND bss.status = 'ACTIVE'
             ORDER BY bss.created_at DESC
             LIMIT 1
           ),
           'COMPANY'
         ) <> 'RESTRICTED'
      OR mp.participant_id IS NOT NULL
      OR EXISTS (
           SELECT 1
           FROM business.business_system_setup bss
           WHERE bss.moment_id = bmc.moment_id
             AND bss.status = 'ACTIVE'
             AND bss.preferences->'participants' @> jsonb_build_array(jsonb_build_object('userId', $1::text))
         )
    )
  )
`;
