import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';

export const patchMeSchema = z
  .object({
    displayName: z.string().trim().min(1).max(120).optional(),
    timezone: z.string().trim().min(1).max(64).optional(),
    locale: z.string().trim().min(2).max(32).optional(),
  })
  .strict()
  .refine((b) => b.displayName !== undefined || b.timezone !== undefined || b.locale !== undefined, {
    message: 'At least one field required',
  });

export type PatchMeInput = z.infer<typeof patchMeSchema>;

export const grantConsentSchema = z
  .object({
    purposeCode: z.string().min(1).max(80),
    scopeType: z.enum(['USER', 'GLOBAL']).default('USER'),
  })
  .strict();

export type GrantConsentInput = z.infer<typeof grantConsentSchema>;

export const withdrawConsentSchema = z
  .object({
    purposeCode: z.string().min(1).max(80),
    scopeType: z.enum(['USER', 'GLOBAL']).default('USER'),
  })
  .strict();

export type WithdrawConsentInput = z.infer<typeof withdrawConsentSchema>;

export async function patchMe(
  client: PoolClient,
  ctx: RequestContext,
  body: PatchMeInput,
): Promise<{ userId: string; displayName: string | null; timezone: string; locale: string | null }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PROFILE_UPDATE',
    resourceType: 'USER',
    ownerUserId: ctx.userId,
  });

  const updated = await client.query<{
    display_name: string | null;
    timezone: string;
    locale: string | null;
  }>(
    `UPDATE core.user_profile SET
       display_name = COALESCE($2, display_name),
       timezone = COALESCE($3, timezone),
       locale = COALESCE($4, locale),
       updated_at = now(),
       version = version + 1
     WHERE user_id = $1 AND status = 'ACTIVE'
     RETURNING display_name, timezone, locale`,
    [ctx.userId, body.displayName ?? null, body.timezone ?? null, body.locale ?? null],
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found or not active.', 404);
  }
  const row = updated.rows[0];
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ProfileUpdated',
    domainCode: 'PLATFORM',
    aggregateType: 'USER',
    aggregateId: ctx.userId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: {
      displayName: body.displayName ?? undefined,
      timezone: body.timezone ?? undefined,
      locale: body.locale ?? undefined,
    },
  });
  await insertAudit(client, ctx, 'PROFILE_UPDATE', 'USER', ctx.userId, domainEventId, {
    fields: Object.keys(body),
  });
  return {
    userId: ctx.userId,
    displayName: row.display_name,
    timezone: row.timezone,
    locale: row.locale,
  };
}

export type ErasureBlocker = {
  kind: 'GROUP_ORGANIZER_TRANSFER_REQUIRED' | 'COMPANY_OWNER_TRANSFER_REQUIRED';
  id: string;
  title: string;
  message: string;
};

export type ErasurePreflight = {
  canErase: boolean;
  blockers: ErasureBlocker[];
  personalMomentsToDelete: Array<{ momentId: string; title: string }>;
  groupMembershipsToLeave: Array<{ momentId: string; title: string; role: string }>;
  companyMembershipsToLeave: Array<{ companyId: string; name: string; membershipType: string }>;
  soleMemberGroupsToDelete: Array<{ momentId: string; title: string }>;
};

/** Soft-delete tombstone + revoke devices (legacy path; prefer eraseMe). */
export async function softDeleteMe(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{ userId: string; status: string }> {
  return eraseMe(client, ctx);
}

export async function erasurePreflight(
  client: PoolClient,
  ctx: RequestContext,
): Promise<ErasurePreflight> {
  const blockers: ErasureBlocker[] = [];

  const personalMoments = await client.query<{ moment_id: string; title: string }>(
    `SELECT m.moment_id, m.title
     FROM core.moment m
     INNER JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
     WHERE pmc.user_id = $1 AND m.status <> 'DELETED'
     ORDER BY m.title`,
    [ctx.userId],
  );

  const groupMemberships = await client.query<{
    moment_id: string;
    title: string;
    participant_role: string;
    organizer_user_id: string | null;
    other_active: string;
  }>(
    `SELECT m.moment_id, m.title, mp.participant_role, gmc.organizer_user_id,
            (SELECT COUNT(*)::text FROM collaboration.moment_participant o
             WHERE o.moment_id = m.moment_id AND o.status = 'ACTIVE' AND o.user_id <> $1) AS other_active
     FROM collaboration.moment_participant mp
     INNER JOIN core.moment m ON m.moment_id = mp.moment_id
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     WHERE mp.user_id = $1 AND mp.status = 'ACTIVE' AND m.status <> 'DELETED'
     ORDER BY m.title`,
    [ctx.userId],
  );

  const soleMemberGroupsToDelete: Array<{ momentId: string; title: string }> = [];
  const groupMembershipsToLeave: Array<{ momentId: string; title: string; role: string }> = [];

  for (const g of groupMemberships.rows) {
    const otherCount = Number(g.other_active || '0');
    const isLeader =
      g.participant_role === 'ORGANIZER' ||
      g.participant_role === 'CO_ORGANIZER' ||
      g.organizer_user_id === ctx.userId;
    if (isLeader && otherCount > 0) {
      blockers.push({
        kind: 'GROUP_ORGANIZER_TRANSFER_REQUIRED',
        id: g.moment_id,
        title: g.title,
        message:
          'Transfer organizer to another member (or remove them) before deleting your account.',
      });
    } else if (isLeader && otherCount === 0) {
      soleMemberGroupsToDelete.push({ momentId: g.moment_id, title: g.title });
    } else {
      groupMembershipsToLeave.push({
        momentId: g.moment_id,
        title: g.title,
        role: g.participant_role,
      });
    }
  }

  const companies = await client.query<{
    company_id: string;
    name: string;
    membership_type: string;
    other_active: string;
  }>(
    `SELECT c.company_id, c.display_name AS name, cm.membership_type,
            (SELECT COUNT(*)::text FROM business.company_membership o
             WHERE o.company_id = c.company_id AND o.status = 'ACTIVE' AND o.user_id <> $1) AS other_active
     FROM business.company_membership cm
     INNER JOIN business.company c ON c.company_id = cm.company_id
     WHERE cm.user_id = $1 AND cm.status = 'ACTIVE'
     ORDER BY c.display_name`,
    [ctx.userId],
  );

  const companyMembershipsToLeave: Array<{
    companyId: string;
    name: string;
    membershipType: string;
  }> = [];

  for (const c of companies.rows) {
    const otherCount = Number(c.other_active || '0');
    const isLeader = c.membership_type === 'OWNER' || c.membership_type === 'ADMIN';
    if (isLeader && otherCount > 0) {
      blockers.push({
        kind: 'COMPANY_OWNER_TRANSFER_REQUIRED',
        id: c.company_id,
        title: c.name,
        message:
          'Transfer ownership/admin to another member before deleting your account.',
      });
    } else {
      companyMembershipsToLeave.push({
        companyId: c.company_id,
        name: c.name,
        membershipType: c.membership_type,
      });
    }
  }

  return {
    canErase: blockers.length === 0,
    blockers,
    personalMomentsToDelete: personalMoments.rows.map((m) => ({
      momentId: m.moment_id,
      title: m.title,
    })),
    groupMembershipsToLeave,
    companyMembershipsToLeave,
    soleMemberGroupsToDelete,
  };
}

/**
 * Principal-owned erasure (DPDP s.12 / s.8(7)):
 * leave shared graphs, delete personal-only data, anonymize profile tombstone.
 * Does not DROP shared tables or other users' data.
 */
export async function eraseMe(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{
  userId: string;
  status: string;
  erased: {
    personalMoments: number;
    soleMemberGroups: number;
    groupsLeft: number;
    companiesLeft: number;
    consentsWithdrawn: number;
    userMediaExpired: number;
    notificationsCleared: number;
  };
}> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ACCOUNT_SOFT_DELETE',
    resourceType: 'USER',
    ownerUserId: ctx.userId,
  });

  const preflight = await erasurePreflight(client, ctx);
  if (!preflight.canErase) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Resolve organizer/ownership transfers before account erasure.',
      400,
      { blockers: preflight.blockers },
    );
  }

  let personalMoments = 0;
  let soleMemberGroups = 0;
  let groupsLeft = 0;
  let companiesLeft = 0;
  let consentsWithdrawn = 0;
  let userMediaExpired = 0;
  let notificationsCleared = 0;

  for (const m of preflight.personalMomentsToDelete) {
    await client.query(
      `UPDATE core.moment SET status = 'DELETED', version = version + 1, updated_at = now()
       WHERE moment_id = $1 AND status <> 'DELETED'`,
      [m.momentId],
    );
    await client.query(`DELETE FROM projection.personal_moments WHERE moment_id = $1`, [m.momentId]);
    personalMoments += 1;
  }

  for (const m of preflight.soleMemberGroupsToDelete) {
    await client.query(
      `UPDATE core.moment SET status = 'DELETED', version = version + 1, updated_at = now()
       WHERE moment_id = $1 AND status <> 'DELETED'`,
      [m.momentId],
    );
    await client.query(
      `UPDATE collaboration.moment_participant
       SET status = 'LEFT', updated_at = now(), version = version + 1
       WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [m.momentId, ctx.userId],
    );
    soleMemberGroups += 1;
  }

  for (const g of preflight.groupMembershipsToLeave) {
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
      [g.momentId, ctx.userId],
    );
    groupsLeft += 1;
  }

  for (const c of preflight.companyMembershipsToLeave) {
    await client.query(
      `UPDATE business.company_membership
       SET status = 'LEFT', updated_at = now(), version = version + 1
       WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [c.companyId, ctx.userId],
    );
    companiesLeft += 1;
  }

  const withdrawn = await client.query(
    `UPDATE governance.consent
     SET status = 'WITHDRAWN', withdrawn_at = now(), updated_at = now()
     WHERE subject_user_id = $1 AND status = 'ACTIVE'`,
    [ctx.userId],
  );
  consentsWithdrawn = withdrawn.rowCount ?? 0;

  const media = await client.query<{ media_upload_id: string; bucket: string; object_key: string }>(
    `UPDATE platform.media_upload
     SET status = 'EXPIRED'
     WHERE user_id = $1 AND scope_type = 'USER' AND status IN ('PENDING', 'COMPLETED')
     RETURNING media_upload_id, bucket, object_key`,
    [ctx.userId],
  );
  userMediaExpired = media.rowCount ?? 0;
  // Best-effort storage delete — never fail erasure if storage is unavailable.
  if (media.rows.length > 0) {
    try {
      const { createClient } = await import('@supabase/supabase-js');
      const url = process.env.SUPABASE_URL?.trim();
      const key = process.env.SUPABASE_SECRET_KEY?.trim();
      if (url && key) {
        const supabase = createClient(url, key, {
          auth: { persistSession: false, autoRefreshToken: false },
        });
        const byBucket = new Map<string, string[]>();
        for (const row of media.rows) {
          const list = byBucket.get(row.bucket) ?? [];
          list.push(row.object_key);
          byBucket.set(row.bucket, list);
        }
        for (const [bucket, keys] of byBucket) {
          await supabase.storage.from(bucket).remove(keys);
        }
      }
    } catch {
      // ignore storage failures
    }
  }

  const cleared = await client.query(
    `DELETE FROM platform.user_notification WHERE user_id = $1`,
    [ctx.userId],
  );
  notificationsCleared = cleared.rowCount ?? 0;

  await client.query(
    `UPDATE platform.user_device
     SET revoked_at = COALESCE(revoked_at, now()),
         push_token = NULL,
         updated_at = now()
     WHERE user_id = $1`,
    [ctx.userId],
  );

  const tombstoneEmail = `erased-${ctx.userId.replace(/-/g, '').slice(0, 16)}@deleted.momentra.local`;
  const updated = await client.query(
    `UPDATE core.user_profile SET
       status = 'DELETED',
       display_name = NULL,
       email = $2,
       push_notifications_enabled = false,
       updated_at = now(),
       version = version + 1
     WHERE user_id = $1 AND status <> 'DELETED'`,
    [ctx.userId, tombstoneEmail],
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found or already deleted.', 404);
  }

  const erased = {
    personalMoments,
    soleMemberGroups,
    groupsLeft,
    companiesLeft,
    consentsWithdrawn,
    userMediaExpired,
    notificationsCleared,
  };

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'AccountErased',
    domainCode: 'PLATFORM',
    aggregateType: 'USER',
    aggregateId: ctx.userId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: { status: 'DELETED', erased },
  });
  await insertAudit(client, ctx, 'ACCOUNT_ERASE', 'USER', ctx.userId, domainEventId, erased);

  return { userId: ctx.userId, status: 'DELETED', erased };
}

export async function listConsents(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{
  purposes: Array<{
    code: string;
    displayName: string;
    description: string | null;
    status: string;
    granted: boolean;
    consentId: string | null;
    grantedAt: string | null;
  }>;
}> {
  const rows = await client.query<{
    code: string;
    display_name: string;
    description: string | null;
    purpose_status: string;
    consent_id: string | null;
    granted_at: Date | null;
    consent_status: string | null;
  }>(
    `SELECT p.code, p.display_name, p.description, p.status AS purpose_status,
            c.consent_id, c.granted_at, c.status AS consent_status
     FROM governance.consent_purpose p
     LEFT JOIN governance.consent c
       ON c.consent_purpose_id = p.consent_purpose_id
      AND c.subject_user_id = $1
      AND c.scope_type = 'USER'
      AND c.scope_id = $1
      AND c.status = 'ACTIVE'
     WHERE p.status = 'ACTIVE'
     ORDER BY p.code`,
    [ctx.userId],
  );
  return {
    purposes: rows.rows.map((r) => ({
      code: r.code,
      displayName: r.display_name,
      description: r.description,
      status: r.purpose_status,
      granted: r.consent_status === 'ACTIVE',
      consentId: r.consent_id,
      grantedAt: r.granted_at ? r.granted_at.toISOString() : null,
    })),
  };
}

export async function grantConsent(
  client: PoolClient,
  ctx: RequestContext,
  body: GrantConsentInput,
): Promise<{ consentId: string; purposeCode: string; status: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'CONSENT_GRANT',
    resourceType: 'CONSENT',
    ownerUserId: ctx.userId,
  });

  const purpose = await client.query<{ consent_purpose_id: string }>(
    `SELECT consent_purpose_id FROM governance.consent_purpose
     WHERE code = $1 AND status = 'ACTIVE'`,
    [body.purposeCode],
  );
  if (!purpose.rowCount) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Unknown consent purpose.', 400);
  }
  const purposeId = purpose.rows[0].consent_purpose_id;
  const scopeType = body.scopeType;
  const scopeId = scopeType === 'GLOBAL' ? null : ctx.userId;

  await client.query(
    `UPDATE governance.consent SET status = 'SUPERSEDED', updated_at = now()
     WHERE subject_user_id = $1 AND consent_purpose_id = $2 AND scope_type = $3
       AND COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid)
         = COALESCE($4::uuid, '00000000-0000-0000-0000-000000000000'::uuid)
       AND status = 'ACTIVE'`,
    [ctx.userId, purposeId, scopeType, scopeId],
  );

  const inserted = await client.query<{ consent_id: string }>(
    `INSERT INTO governance.consent (
       subject_user_id, consent_purpose_id, scope_type, scope_id, status, granted_at, source, version
     ) VALUES ($1, $2, $3, $4, 'ACTIVE', now(), 'USER', 1)
     RETURNING consent_id`,
    [ctx.userId, purposeId, scopeType, scopeId],
  );
  const consentId = inserted.rows[0].consent_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ConsentGranted',
    domainCode: 'PLATFORM',
    aggregateType: 'CONSENT',
    aggregateId: consentId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: { purposeCode: body.purposeCode },
  });
  await insertAudit(client, ctx, 'CONSENT_GRANT', 'CONSENT', consentId, domainEventId, {
    purposeCode: body.purposeCode,
  });
  return { consentId, purposeCode: body.purposeCode, status: 'ACTIVE' };
}

export async function withdrawConsent(
  client: PoolClient,
  ctx: RequestContext,
  body: WithdrawConsentInput,
): Promise<{ purposeCode: string; status: string; consentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'CONSENT_WITHDRAW',
    resourceType: 'CONSENT',
    ownerUserId: ctx.userId,
  });

  const purpose = await client.query<{ consent_purpose_id: string }>(
    `SELECT consent_purpose_id FROM governance.consent_purpose WHERE code = $1`,
    [body.purposeCode],
  );
  if (!purpose.rowCount) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Unknown consent purpose.', 400);
  }
  const purposeId = purpose.rows[0].consent_purpose_id;
  const scopeType = body.scopeType;
  const scopeId = scopeType === 'GLOBAL' ? null : ctx.userId;

  const updated = await client.query<{ consent_id: string }>(
    `UPDATE governance.consent SET status = 'WITHDRAWN', withdrawn_at = now(), updated_at = now()
     WHERE subject_user_id = $1 AND consent_purpose_id = $2 AND scope_type = $3
       AND COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid)
         = COALESCE($4::uuid, '00000000-0000-0000-0000-000000000000'::uuid)
       AND status = 'ACTIVE'
     RETURNING consent_id`,
    [ctx.userId, purposeId, scopeType, scopeId],
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Active consent not found.', 404);
  }
  const consentId = updated.rows[0].consent_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ConsentWithdrawn',
    domainCode: 'PLATFORM',
    aggregateType: 'CONSENT',
    aggregateId: consentId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: { purposeCode: body.purposeCode },
  });
  await insertAudit(client, ctx, 'CONSENT_WITHDRAW', 'CONSENT', consentId, domainEventId, {
    purposeCode: body.purposeCode,
  });
  return { purposeCode: body.purposeCode, status: 'WITHDRAWN', consentId };
}

/** Principal-scoped access package (DPDP s.11 summary). No other users' private data; no media binaries. */
export async function exportMe(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{
  exportedAt: string;
  profile: {
    userId: string;
    email: string | null;
    displayName: string | null;
    timezone: string | null;
    locale: string | null;
    status: string;
  };
  devices: Array<{
    deviceId: string;
    platform: string;
    appVersion: string | null;
    lastSeenAt: string | null;
    revoked: boolean;
  }>;
  consents: Array<{
    code: string;
    displayName: string;
    description: string | null;
    granted: boolean;
    consentId: string | null;
    grantedAt: string | null;
  }>;
  processors: Array<{ name: string; purpose: string }>;
  notes: string[];
}> {
  const profile = await client.query<{
    email: string | null;
    display_name: string | null;
    timezone: string | null;
    locale: string | null;
    status: string;
  }>(
    `SELECT email, display_name, timezone, locale, status
     FROM core.user_profile WHERE user_id = $1`,
    [ctx.userId],
  );
  if (!profile.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found.', 404);
  }
  const p = profile.rows[0];
  const devices = await client.query<{
    device_id: string;
    platform: string;
    app_version: string | null;
    last_seen_at: Date | null;
    revoked_at: Date | null;
  }>(
    `SELECT device_id, platform, app_version, last_seen_at, revoked_at
     FROM platform.user_device
     WHERE user_id = $1
     ORDER BY COALESCE(last_seen_at, created_at) DESC`,
    [ctx.userId],
  );
  const consents = await listConsents(client, ctx);

  return {
    exportedAt: new Date().toISOString(),
    profile: {
      userId: ctx.userId,
      email: p.email,
      displayName: p.display_name,
      timezone: p.timezone,
      locale: p.locale,
      status: p.status,
    },
    devices: devices.rows.map((d) => ({
      deviceId: d.device_id,
      platform: d.platform,
      appVersion: d.app_version,
      lastSeenAt: d.last_seen_at ? d.last_seen_at.toISOString() : null,
      revoked: d.revoked_at != null,
    })),
    consents: consents.purposes.map((c) => ({
      code: c.code,
      displayName: c.displayName,
      description: c.description,
      granted: c.granted,
      consentId: c.consentId,
      grantedAt: c.grantedAt,
    })),
    processors: [
      { name: 'Firebase (Google)', purpose: 'Authentication, messaging, optional analytics/crash' },
      { name: 'Google', purpose: 'Sign-In / Play services / FCM where used' },
      { name: 'Apple', purpose: 'Sign in with Apple where used' },
      { name: 'Supabase / Postgres', purpose: 'Application database and media storage' },
      { name: 'Sentry (optional)', purpose: 'Error monitoring when enabled' },
      { name: 'Hosting / CDN', purpose: 'Invite links and marketing site' },
    ],
    notes: [
      'Principal-scoped summary only — does not include other participants’ private data or media binaries.',
      'Account deletion performs principal-owned erasure: personal data removed/anonymized; shared Group/Business graphs remain for other members.',
      'Privacy / grievance: hello@momentra.app',
    ],
  };
}
