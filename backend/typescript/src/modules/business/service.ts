import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

export const createCompanySchema = z
  .object({
    displayName: z.string().min(1).max(300),
    legalName: z.string().min(1).max(500),
    timezone: z.string().default('UTC'),
    companyType: z.string().max(100).optional(),
    taxIdentifier: z.string().max(100).optional(),
    profileJson: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

export const updateCompanySchema = z
  .object({
    displayName: z.string().min(1).max(300).optional(),
    legalName: z.string().min(1).max(500).optional(),
    timezone: z.string().optional(),
    expectedVersion: z.number().int().positive(),
  })
  .strict();

export const createLocationSchema = z
  .object({
    name: z.string().min(1).max(300),
    addressText: z.string().max(1000).optional(),
    timezone: z.string().optional(),
  })
  .strict();

export const updateLocationSchema = z
  .object({
    name: z.string().min(1).max(300).optional(),
    addressText: z.string().max(1000).optional(),
    timezone: z.string().optional(),
    status: z.enum(['ACTIVE', 'INACTIVE']).optional(),
    expectedVersion: z.number().int().positive(),
  })
  .strict();

export const createTeamSchema = z
  .object({
    name: z.string().min(1).max(300),
    description: z.string().max(2000).optional(),
  })
  .strict();

export async function createCompany(
  client: PoolClient,
  ctx: RequestContext,
  body: z.infer<typeof createCompanySchema>
): Promise<{ companyId: string; displayName: string; version: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'COMPANY_CREATE', resourceType: 'COMPANY' });
  const inserted = await client.query<{ company_id: string; version: string }>(
    `INSERT INTO business.company (
       legal_name, display_name, timezone, company_type, tax_identifier, profile_json, created_by_user_id, status, version
     )
     VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7, 'ACTIVE', 1)
     RETURNING company_id, version`,
    [
      body.legalName,
      body.displayName,
      body.timezone,
      body.companyType ?? null,
      body.taxIdentifier ?? null,
      JSON.stringify(body.profileJson ?? {}),
      ctx.userId,
    ]
  );
  const companyId = inserted.rows[0]!.company_id;
  await client.query(
    `INSERT INTO business.company_membership (company_id, user_id, membership_type, status, joined_at, version)
     VALUES ($1, $2, 'OWNER', 'ACTIVE', now(), 1)`,
    [companyId, ctx.userId]
  );
  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'CompanyCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'COMPANY',
    aggregateId: companyId,
    scopeType: 'COMPANY',
    scopeId: companyId,
    payload: { companyId, displayName: body.displayName },
  });
  return {
    companyId,
    displayName: body.displayName,
    version: parseInt(inserted.rows[0]!.version, 10),
  };
}

export async function getCompany(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<{ companyId: string; displayName: string; legalName: string; version: number } | null> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'COMPANY_READ', resourceType: 'COMPANY', companyId });
  const row = await client.query<{ company_id: string; display_name: string; legal_name: string; version: string }>(
    `SELECT company_id, display_name, legal_name, version FROM business.company WHERE company_id = $1`,
    [companyId]
  );
  if (!row.rows[0]) return null;
  const r = row.rows[0];
  return {
    companyId: r.company_id,
    displayName: r.display_name,
    legalName: r.legal_name,
    version: parseInt(r.version, 10),
  };
}

export async function listCompanies(
  client: PoolClient,
  ctx: RequestContext
): Promise<{ items: Array<{ companyId: string; displayName: string }> }> {
  const rows = await client.query<{ company_id: string; display_name: string }>(
    `SELECT c.company_id, c.display_name
     FROM business.company c
     JOIN business.company_membership cm ON cm.company_id = c.company_id
     WHERE cm.user_id = $1 AND cm.status = 'ACTIVE'
     ORDER BY c.display_name`,
    [ctx.userId]
  );
  return {
    items: rows.rows.map((r) => ({ companyId: r.company_id, displayName: r.display_name })),
  };
}

export async function updateCompany(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof updateCompanySchema>
): Promise<{ companyId: string; displayName: string; version: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'COMPANY_UPDATE', resourceType: 'COMPANY', companyId });
  const updated = await client.query<{ company_id: string; display_name: string; version: string }>(
    `UPDATE business.company SET
       display_name = COALESCE($3, display_name),
       legal_name = COALESCE($4, legal_name),
       timezone = COALESCE($5, timezone),
       version = version + 1,
       updated_at = now()
     WHERE company_id = $1 AND version = $2
     RETURNING company_id, display_name, version`,
    [companyId, body.expectedVersion, body.displayName ?? null, body.legalName ?? null, body.timezone ?? null]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Company version conflict.', 409);
  }
  const r = updated.rows[0];
  return { companyId: r.company_id, displayName: r.display_name, version: parseInt(r.version, 10) };
}

export async function createLocation(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof createLocationSchema>
): Promise<{ locationId: string; companyId: string; name: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'LOCATION_CREATE', resourceType: 'LOCATION', companyId });
  const inserted = await client.query<{ company_location_id: string }>(
    `INSERT INTO business.company_location (company_id, name, address_text, timezone, status, version)
     VALUES ($1, $2, $3, $4, 'ACTIVE', 1)
     RETURNING company_location_id`,
    [companyId, body.name, body.addressText ?? null, body.timezone ?? null]
  );
  return {
    locationId: inserted.rows[0]!.company_location_id,
    companyId,
    name: body.name,
  };
}

export async function updateLocation(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  locationId: string,
  body: z.infer<typeof updateLocationSchema>
): Promise<{ locationId: string; companyId: string; name: string; status: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'LOCATION_UPDATE', resourceType: 'LOCATION', companyId });
  const updated = await client.query<{ company_location_id: string; name: string; status: string }>(
    `UPDATE business.company_location SET
       name = COALESCE($4, name),
       address_text = COALESCE($5, address_text),
       timezone = COALESCE($6, timezone),
       status = COALESCE($7, status),
       version = version + 1,
       updated_at = now()
     WHERE company_location_id = $1 AND company_id = $2 AND version = $3
     RETURNING company_location_id, name, status`,
    [
      locationId,
      companyId,
      body.expectedVersion,
      body.name ?? null,
      body.addressText ?? null,
      body.timezone ?? null,
      body.status ?? null,
    ]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.VERSION_CONFLICT, 'Location version conflict.', 409);
  }
  const r = updated.rows[0];
  return { locationId: r.company_location_id, companyId, name: r.name, status: r.status };
}

export async function listLocations(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<{ items: Array<{ locationId: string; name: string; status: string }> }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'LOCATION_READ', resourceType: 'LOCATION', companyId });
  const rows = await client.query<{ company_location_id: string; name: string; status: string }>(
    `SELECT company_location_id, name, status
     FROM business.company_location
     WHERE company_id = $1 AND status = 'ACTIVE'
     ORDER BY name`,
    [companyId]
  );
  return {
    items: rows.rows.map((r) => ({
      locationId: r.company_location_id,
      name: r.name,
      status: r.status,
    })),
  };
}

export async function createTeam(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof createTeamSchema>
): Promise<{ teamId: string; companyId: string; name: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'TEAM_CREATE', resourceType: 'TEAM', companyId });
  const inserted = await client.query<{ team_id: string }>(
    `INSERT INTO business.team (company_id, name, description, status, version)
     VALUES ($1, $2, $3, 'ACTIVE', 1)
     RETURNING team_id`,
    [companyId, body.name, body.description ?? null]
  );
  return { teamId: inserted.rows[0]!.team_id, companyId, name: body.name };
}

export async function listTeams(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string
): Promise<{ items: Array<{ teamId: string; name: string }> }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'TEAM_READ', resourceType: 'TEAM', companyId });
  const rows = await client.query<{ team_id: string; name: string }>(
    `SELECT team_id, name FROM business.team WHERE company_id = $1 AND status = 'ACTIVE' ORDER BY name`,
    [companyId]
  );
  return { items: rows.rows.map((r) => ({ teamId: r.team_id, name: r.name })) };
}
