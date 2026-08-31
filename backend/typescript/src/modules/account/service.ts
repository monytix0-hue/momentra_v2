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

/** Soft-delete: status=DELETED. Does not hard-wipe RESTRICT domain graphs. */
export async function softDeleteMe(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{ userId: string; status: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ACCOUNT_SOFT_DELETE',
    resourceType: 'USER',
    ownerUserId: ctx.userId,
  });

  const updated = await client.query(
    `UPDATE core.user_profile SET status = 'DELETED', updated_at = now(), version = version + 1
     WHERE user_id = $1 AND status <> 'DELETED'`,
    [ctx.userId],
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found.', 404);
  }

  await client.query(
    `UPDATE platform.user_device SET revoked_at = now(), updated_at = now()
     WHERE user_id = $1 AND revoked_at IS NULL`,
    [ctx.userId],
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'AccountSoftDeleted',
    domainCode: 'PLATFORM',
    aggregateType: 'USER',
    aggregateId: ctx.userId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: { status: 'DELETED' },
  });
  await insertAudit(client, ctx, 'ACCOUNT_SOFT_DELETE', 'USER', ctx.userId, domainEventId, {});

  return { userId: ctx.userId, status: 'DELETED' };
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
): Promise<{ purposeCode: string; status: string }> {
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

  const updated = await client.query(
    `UPDATE governance.consent SET status = 'WITHDRAWN', withdrawn_at = now(), updated_at = now()
     WHERE subject_user_id = $1 AND consent_purpose_id = $2 AND scope_type = $3
       AND COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid)
         = COALESCE($4::uuid, '00000000-0000-0000-0000-000000000000'::uuid)
       AND status = 'ACTIVE'`,
    [ctx.userId, purposeId, scopeType, scopeId],
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Active consent not found.', 404);
  }
  return { purposeCode: body.purposeCode, status: 'WITHDRAWN' };
}
