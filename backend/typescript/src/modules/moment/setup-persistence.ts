import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import type { BusinessSetupFamilyCode } from '../business/setup-service';
import { BUSINESS_SETUP_CATALOG } from '../business/setup-service';
import type { PersonalSetupSystemCode } from '../personal/setup-service';
import { PERSONAL_SETUP_CATALOG } from '../personal/setup-service';

export async function insertPersonalSetupRow(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  systemCode: PersonalSetupSystemCode,
  title: string,
  preferences: Record<string, unknown>,
  status: 'DRAFT' | 'ACTIVE' = 'ACTIVE'
): Promise<string> {
  const inserted = await client.query<{ life_system_setup_id: string }>(
    `INSERT INTO personal.life_system_setup (
       user_id, moment_id, system_code, title, preferences, status, version
     ) VALUES ($1, $2, $3, $4, $5::jsonb, $6, 1)
     RETURNING life_system_setup_id`,
    [ctx.userId, momentId, systemCode, title, JSON.stringify(preferences), status]
  );
  const setupId = inserted.rows[0]!.life_system_setup_id;
  if (status === 'DRAFT') {
    return setupId;
  }
  const catalog = PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === systemCode)!;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PersonalSetupActivated',
    domainCode: 'PERSONAL',
    aggregateType: 'LIFE_SYSTEM_SETUP',
    aggregateId: setupId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { setupId, systemCode, momentId, title },
  });
  await insertAudit(client, ctx, 'PERSONAL_SETUP_ACTIVATE', 'LIFE_SYSTEM_SETUP', setupId, domainEventId, {
    systemCode,
    momentId,
    title,
    momentTypeCode: catalog.defaultMomentTypeCode,
  });
  return setupId;
}

export async function insertBusinessFamilyContext(
  client: PoolClient,
  momentId: string,
  familyCode: string,
  title: string
): Promise<void> {
  if (familyCode === 'TEAM_OPERATIONS') {
    await client.query(
      `INSERT INTO business.team_operations_context (moment_id, business_family, objective_text, status)
       VALUES ($1, 'TEAM_OPERATIONS', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  } else if (familyCode === 'BUSINESS_RUNWAY') {
    await client.query(
      `INSERT INTO business.business_runway_context (moment_id, business_family, scenario_name, status)
       VALUES ($1, 'BUSINESS_RUNWAY', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  } else if (familyCode === 'BUSINESS_OPERATIONS') {
    await client.query(
      `INSERT INTO business.business_operations_context (moment_id, business_family, objective_text, status)
       VALUES ($1, 'BUSINESS_OPERATIONS', $2, 'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, title]
    );
  }
}

export async function insertBusinessSetupRow(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  companyId: string | null,
  familyCode: BusinessSetupFamilyCode | 'TEAM_OPERATIONS' | 'BUSINESS_RUNWAY' | 'BUSINESS_OPERATIONS',
  title: string,
  preferences: Record<string, unknown>,
  status: 'DRAFT' | 'ACTIVE' = 'ACTIVE'
): Promise<string> {
  if (status === 'ACTIVE') {
    await insertBusinessFamilyContext(client, momentId, familyCode, title);
  }
  const inserted = await client.query<{ business_system_setup_id: string }>(
    `INSERT INTO business.business_system_setup (
       company_id, user_id, moment_id, family_code, title, preferences, status, version
     ) VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7, 1)
     RETURNING business_system_setup_id`,
    [companyId, ctx.userId, momentId, familyCode, title, JSON.stringify(preferences), status]
  );
  const setupId = inserted.rows[0]!.business_system_setup_id;
  if (status === 'DRAFT') {
    return setupId;
  }
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessSetupActivated',
    domainCode: 'BUSINESS',
    aggregateType: 'BUSINESS_SYSTEM_SETUP',
    aggregateId: setupId,
    scopeType: companyId ? 'COMPANY' : 'MOMENT',
    scopeId: companyId ?? momentId,
    payload: { setupId, familyCode, momentId, title, companyId },
  });
  await insertAudit(client, ctx, 'BUSINESS_SETUP_ACTIVATE', 'BUSINESS_SYSTEM_SETUP', setupId, domainEventId, {
    familyCode,
    momentId,
    title,
    companyId,
  });
  return setupId;
}

/**
 * Rewrite a DRAFT business setup in place — the setup document's Section 01–05 edits after the
 * draft moment already exists. Only a DRAFT row is writable: once a setup is ACTIVE its
 * preferences move through the operating events (`BUDGET` / `TARGET` revisions), not a rewrite.
 */
export async function updateBusinessSetupDraftRow(
  client: PoolClient,
  momentId: string,
  title: string,
  preferences: Record<string, unknown>
): Promise<string | null> {
  const updated = await client.query<{ business_system_setup_id: string }>(
    `UPDATE business.business_system_setup
     SET title = $2, preferences = $3::jsonb, updated_at = now(), version = version + 1
     WHERE moment_id = $1 AND status = 'DRAFT'
     RETURNING business_system_setup_id`,
    [momentId, title, JSON.stringify(preferences)]
  );
  return updated.rows[0]?.business_system_setup_id ?? null;
}

/**
 * Keep `core.moment.moment_type_id` aligned with `preferences.subtype` when the setup document's
 * kind tiles flip mid-draft (Client ↔ Own, Trading ↔ Distribution). Without this, activate and
 * shell chrome keep the type chosen on create.
 */
export async function syncBusinessMomentTypeFromSubtype(
  client: PoolClient,
  momentId: string,
  subtypeCode: string
): Promise<string | null> {
  const code = String(subtypeCode ?? '')
    .trim()
    .toUpperCase();
  if (!code) return null;
  const typeRow = await client.query<{ moment_type_id: string; code: string }>(
    `SELECT moment_type_id, code
     FROM core.moment_type
     WHERE domain_code = 'BUSINESS' AND code = $1 AND status = 'ACTIVE'
     LIMIT 1`,
    [code]
  );
  const next = typeRow.rows[0];
  if (!next) return null;
  await client.query(
    `UPDATE core.moment
     SET moment_type_id = $2, updated_at = now()
     WHERE moment_id = $1
       AND moment_type_id IS DISTINCT FROM $2`,
    [momentId, next.moment_type_id]
  );
  return next.code;
}

export function catalogSubtitleForPersonal(systemCode: PersonalSetupSystemCode): string {
  return PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === systemCode)?.subtitle ?? '';
}

export function catalogSubtitleForBusiness(familyCode: BusinessSetupFamilyCode): string {
  return BUSINESS_SETUP_CATALOG.find((s) => s.familyCode === familyCode)?.subtitle ?? '';
}
