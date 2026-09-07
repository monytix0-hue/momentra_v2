/**
 * Master Expense analytical dimensions: one canonical expense → LO / Rel / Lifestyle contributions.
 * Future Building is never activated for ordinary Master Expense.
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { isLifestyleEligible } from './lifestyle-eligibility';
import { refreshRelationshipsBondAxes } from '../personal/relationships-precision';
import { refreshLifestylePulseAxes } from '../personal/lifestyle-precision';

export const SHARED_EXPERIENCE_CODES = [
  'SELF',
  'SPOUSE',
  'FAMILY',
  'FRIEND',
  'COLLEAGUE',
  'OTHER',
] as const;

export type SharedExperienceCode = (typeof SHARED_EXPERIENCE_CODES)[number];

export type DimensionCode = 'LIFE_OPERATIONS' | 'RELATIONSHIPS' | 'LIFESTYLE' | 'FUTURE_BUILDING';

const RELATIONSHIP_DISPLAY: Record<Exclude<SharedExperienceCode, 'SELF'>, string> = {
  SPOUSE: 'Spouse / Partner',
  FAMILY: 'Family',
  FRIEND: 'Friend',
  COLLEAGUE: 'Colleague',
  OTHER: 'Other',
};

export async function resolveSetupMomentId(
  client: PoolClient,
  userId: string,
  systemCode: 'LIFE_OPERATIONS' | 'RELATIONSHIPS' | 'LIFESTYLE' | 'FUTURE_BUILDING'
): Promise<string | null> {
  const row = await client.query<{ moment_id: string }>(
    `SELECT moment_id FROM personal.life_system_setup
     WHERE user_id = $1 AND system_code = $2 AND status = 'ACTIVE'
     ORDER BY created_at ASC
     LIMIT 1`,
    [userId, systemCode]
  );
  return row.rows[0]?.moment_id ?? null;
}

/** Prefer LIFE_OPERATIONS setup moment; fall back to path moment when setup missing. */
export async function resolveCanonicalExpenseMomentId(
  client: PoolClient,
  userId: string,
  pathMomentId: string
): Promise<string> {
  const lo = await resolveSetupMomentId(client, userId, 'LIFE_OPERATIONS');
  return lo ?? pathMomentId;
}

async function upsertContribution(
  client: PoolClient,
  expenseId: string,
  dimension: DimensionCode,
  status: 'ACTIVE' | 'INACTIVE',
  targetMomentId: string | null,
  linkedResourceType: 'NONE' | 'RELATIONSHIP_ACTIVITY' | 'LIFESTYLE_ACTIVITY',
  linkedResourceId: string | null
): Promise<void> {
  await client.query(
    `INSERT INTO finance.expense_dimension_contribution (
       expense_id, dimension_code, status, target_moment_id, linked_resource_type, linked_resource_id
     ) VALUES ($1,$2,$3,$4,$5,$6)
     ON CONFLICT (expense_id, dimension_code) DO UPDATE SET
       status = EXCLUDED.status,
       target_moment_id = EXCLUDED.target_moment_id,
       linked_resource_type = EXCLUDED.linked_resource_type,
       linked_resource_id = COALESCE(EXCLUDED.linked_resource_id, finance.expense_dimension_contribution.linked_resource_id),
       updated_at = now()`,
    [expenseId, dimension, status, targetMomentId, linkedResourceType, linkedResourceId]
  );
}

async function getContributionLink(
  client: PoolClient,
  expenseId: string,
  dimension: DimensionCode
): Promise<{
  linked_resource_id: string | null;
  linked_resource_type: string;
  status: string;
  target_moment_id: string | null;
} | null> {
  const row = await client.query<{
    linked_resource_id: string | null;
    linked_resource_type: string;
    status: string;
    target_moment_id: string | null;
  }>(
    `SELECT linked_resource_id, linked_resource_type, status, target_moment_id
     FROM finance.expense_dimension_contribution
     WHERE expense_id = $1 AND dimension_code = $2`,
    [expenseId, dimension]
  );
  return row.rows[0] ?? null;
}

async function ensureRelationshipConnection(
  client: PoolClient,
  ctx: RequestContext,
  shared: Exclude<SharedExperienceCode, 'SELF'>,
  label: string | null
): Promise<string> {
  const displayName =
    shared === 'OTHER' && label?.trim() ? label.trim() : RELATIONSHIP_DISPLAY[shared];
  const existing = await client.query<{ relationship_connection_id: string }>(
    `SELECT relationship_connection_id FROM personal.relationship_connection
     WHERE user_id = $1 AND lower(display_name) = lower($2) AND status = 'ACTIVE'
     LIMIT 1`,
    [ctx.userId, displayName]
  );
  if (existing.rows[0]) return existing.rows[0].relationship_connection_id;

  const party = await client.query<{ external_party_id: string }>(
    `INSERT INTO core.external_party (party_type, display_name, status)
     VALUES ('PERSON', $1, 'ACTIVE')
     RETURNING external_party_id`,
    [displayName]
  );
  const conn = await client.query<{ relationship_connection_id: string }>(
    `INSERT INTO personal.relationship_connection (
       user_id, external_party_id, display_name, relationship_type, status
     ) VALUES ($1, $2, $3, $4, 'ACTIVE')
     RETURNING relationship_connection_id`,
    [ctx.userId, party.rows[0]!.external_party_id, displayName, shared]
  );
  return conn.rows[0]!.relationship_connection_id;
}

async function mirrorResourceLink(
  client: PoolClient,
  expenseId: string,
  resourceType: 'RELATIONSHIP_ACTIVITY' | 'LIFESTYLE_ACTIVITY',
  resourceId: string
): Promise<void> {
  try {
    await client.query(
      `INSERT INTO finance.expense_resource_link (expense_id, resource_type, resource_id, relation_type)
       VALUES ($1, $2, $3, 'RELATED')
       ON CONFLICT (expense_id, resource_type, resource_id, relation_type) DO NOTHING`,
      [expenseId, resourceType, resourceId]
    );
  } catch {
    // Pre-migration environments may lack extended resource types.
  }
}

/** Mark Rel/Lifestyle Moments feed rows VOIDED so View Activity hides them. */
export async function voidRecentActivityByActivityId(
  client: PoolClient,
  userId: string,
  momentId: string,
  activityId: string
): Promise<void> {
  await client.query(
    `UPDATE projection.recent_activity SET
       activity_payload = COALESCE(activity_payload, '{}'::jsonb) || '{"status":"VOIDED"}'::jsonb,
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2::uuid AND activity_payload->>'activityId' = $3`,
    [userId, momentId, activityId]
  );
}

/**
 * Ensure Master Expense–derived activities appear on Rel/Lifestyle Moments feeds
 * (GET /v1/personal/activity?momentId=…).
 */
async function upsertMasterExpenseRecentActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  activityCode: string,
  title: string,
  activityId: string,
  expenseId: string,
  aggregateType: 'RELATIONSHIP_ACTIVITY' | 'LIFESTYLE_ACTIVITY'
): Promise<void> {
  const payload = {
    activityId,
    expenseId,
    source: 'MASTER_EXPENSE',
    status: 'POSTED',
  };
  const updated = await client.query(
    `UPDATE projection.recent_activity SET
       title = $4,
       activity_code = $5,
       activity_payload = $6::jsonb,
       occurred_at = now(),
       projection_version = projection_version + 1
     WHERE user_id = $1 AND scope_id = $2::uuid AND activity_payload->>'activityId' = $3
     RETURNING recent_activity_id`,
    [ctx.userId, momentId, activityId, title, activityCode, JSON.stringify(payload)]
  );
  if (updated.rowCount && updated.rowCount > 0) return;

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MasterExpenseDimensionActivityVisible',
    domainCode: 'PERSONAL',
    aggregateType,
    aggregateId: activityId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { activityId, expenseId, activityCode },
  });
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1,$2,'PERSONAL','MOMENT',$3::uuid,$4,$5,now(),$6::jsonb,1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [ctx.userId, domainEventId, momentId, activityCode, title, JSON.stringify(payload)]
  );
}

async function upsertRelationshipContribution(
  client: PoolClient,
  ctx: RequestContext,
  expenseId: string,
  shared: SharedExperienceCode,
  label: string | null,
  merchantName: string | null,
  amount: string,
  effectiveAt: string,
  sourceEventId: string
): Promise<void> {
  const relMomentId = await resolveSetupMomentId(client, ctx.userId, 'RELATIONSHIPS');
  const existing = await getContributionLink(client, expenseId, 'RELATIONSHIPS');

  if (shared !== 'SELF' && !relMomentId) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Activate Relationships setup before logging a shared-experience expense.',
      400
    );
  }

  if (shared === 'SELF') {
    if (existing?.linked_resource_id && existing.linked_resource_type === 'RELATIONSHIP_ACTIVITY') {
      await client.query(
        `UPDATE personal.relationship_activity SET status = 'VOIDED', updated_at = now()
         WHERE relationship_activity_id = $1 AND user_id = $2`,
        [existing.linked_resource_id, ctx.userId]
      );
      if (relMomentId) {
        await voidRecentActivityByActivityId(
          client,
          ctx.userId,
          relMomentId,
          existing.linked_resource_id
        );
        await refreshRelationshipsBondAxes(client, ctx.userId, relMomentId, sourceEventId);
      }
    }
    await upsertContribution(
      client,
      expenseId,
      'RELATIONSHIPS',
      'INACTIVE',
      relMomentId,
      existing?.linked_resource_type === 'RELATIONSHIP_ACTIVITY' ? 'RELATIONSHIP_ACTIVITY' : 'NONE',
      existing?.linked_resource_id ?? null
    );
    return;
  }

  const connectionId = await ensureRelationshipConnection(client, ctx, shared, label);
  const title = merchantName?.trim() || RELATIONSHIP_DISPLAY[shared];
  let activityId = existing?.linked_resource_id ?? null;

  if (activityId && existing?.linked_resource_type === 'RELATIONSHIP_ACTIVITY') {
    await client.query(
      `UPDATE personal.relationship_activity SET
         moment_id = $2,
         relationship_connection_id = $3,
         activity_type = 'SHARED_EXPERIENCE',
         occurred_at = $4::timestamptz,
         title = $5,
         note = $6,
         investment_value = $7::numeric,
         status = 'ACTIVE',
         updated_at = now()
       WHERE relationship_activity_id = $1 AND user_id = $8`,
      [
        activityId,
        relMomentId,
        connectionId,
        effectiveAt,
        title,
        `Master Expense shared experience: ${shared}`,
        amount,
        ctx.userId,
      ]
    );
  } else {
    const inserted = await client.query<{ relationship_activity_id: string }>(
      `INSERT INTO personal.relationship_activity (
         moment_id, user_id, relationship_connection_id, activity_type, occurred_at,
         title, note, investment_value, status
       ) VALUES ($1,$2,$3,'SHARED_EXPERIENCE',$4::timestamptz,$5,$6,$7::numeric,'ACTIVE')
       RETURNING relationship_activity_id`,
      [
        relMomentId,
        ctx.userId,
        connectionId,
        effectiveAt,
        title,
        `Master Expense shared experience: ${shared}`,
        amount,
      ]
    );
    activityId = inserted.rows[0]!.relationship_activity_id;
  }

  await upsertContribution(
    client,
    expenseId,
    'RELATIONSHIPS',
    'ACTIVE',
    relMomentId,
    'RELATIONSHIP_ACTIVITY',
    activityId
  );
  await mirrorResourceLink(client, expenseId, 'RELATIONSHIP_ACTIVITY', activityId!);
  await upsertMasterExpenseRecentActivity(
    client,
    ctx,
    relMomentId!,
    'RELATIONSHIP_SHARED_EXPERIENCE',
    title,
    activityId!,
    expenseId,
    'RELATIONSHIP_ACTIVITY'
  );
  await refreshRelationshipsBondAxes(client, ctx.userId, relMomentId!, sourceEventId);
}

async function upsertLifestyleContribution(
  client: PoolClient,
  ctx: RequestContext,
  expenseId: string,
  categoryCode: string | null,
  subcategoryCode: string | null,
  merchantName: string | null,
  description: string | null,
  effectiveAt: string,
  sourceEventId: string
): Promise<void> {
  const lsMomentId = await resolveSetupMomentId(client, ctx.userId, 'LIFESTYLE');
  const eligible = isLifestyleEligible(categoryCode, subcategoryCode);
  const existing = await getContributionLink(client, expenseId, 'LIFESTYLE');

  if (eligible && !lsMomentId) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Activate Lifestyle setup before logging a lifestyle-eligible expense.',
      400
    );
  }

  if (!eligible) {
    if (existing?.linked_resource_id && existing.linked_resource_type === 'LIFESTYLE_ACTIVITY') {
      await client.query(
        `UPDATE personal.lifestyle_activity SET status = 'CANCELLED', updated_at = now()
         WHERE lifestyle_activity_id = $1 AND user_id = $2`,
        [existing.linked_resource_id, ctx.userId]
      );
      if (lsMomentId) {
        await voidRecentActivityByActivityId(
          client,
          ctx.userId,
          lsMomentId,
          existing.linked_resource_id
        );
        await refreshLifestylePulseAxes(client, ctx.userId, lsMomentId, sourceEventId);
      }
    }
    await upsertContribution(
      client,
      expenseId,
      'LIFESTYLE',
      'INACTIVE',
      lsMomentId,
      existing?.linked_resource_type === 'LIFESTYLE_ACTIVITY' ? 'LIFESTYLE_ACTIVITY' : 'NONE',
      existing?.linked_resource_id ?? null
    );
    return;
  }

  const title = merchantName?.trim() || 'Lifestyle experience';
  let activityId = existing?.linked_resource_id ?? null;

  if (activityId && existing?.linked_resource_type === 'LIFESTYLE_ACTIVITY') {
    await client.query(
      `UPDATE personal.lifestyle_activity SET
         moment_id = $2,
         lifestyle_context = 'EXPERIENCE',
         title = $3,
         description = $4,
         occurred_at = $5::timestamptz,
         status = 'ACTIVE',
         updated_at = now()
       WHERE lifestyle_activity_id = $1 AND user_id = $6`,
      [activityId, lsMomentId, title, description, effectiveAt, ctx.userId]
    );
  } else {
    const inserted = await client.query<{ lifestyle_activity_id: string }>(
      `INSERT INTO personal.lifestyle_activity (
         moment_id, user_id, lifestyle_context, title, description, occurred_at, status
       ) VALUES ($1,$2,'EXPERIENCE',$3,$4,$5::timestamptz,'ACTIVE')
       RETURNING lifestyle_activity_id`,
      [lsMomentId, ctx.userId, title, description, effectiveAt]
    );
    activityId = inserted.rows[0]!.lifestyle_activity_id;
  }

  await upsertContribution(
    client,
    expenseId,
    'LIFESTYLE',
    'ACTIVE',
    lsMomentId,
    'LIFESTYLE_ACTIVITY',
    activityId
  );
  await mirrorResourceLink(client, expenseId, 'LIFESTYLE_ACTIVITY', activityId!);
  await upsertMasterExpenseRecentActivity(
    client,
    ctx,
    lsMomentId!,
    'LIFESTYLE_EXPERIENCE',
    title,
    activityId!,
    expenseId,
    'LIFESTYLE_ACTIVITY'
  );
  await refreshLifestylePulseAxes(client, ctx.userId, lsMomentId!, sourceEventId);
}

export async function syncExpenseAnalyticalContributions(
  client: PoolClient,
  ctx: RequestContext,
  expenseId: string,
  opts?: { deactivateAll?: boolean; sourceEventId?: string }
): Promise<void> {
  const table = await client.query<{ ok: boolean }>(
    `SELECT to_regclass('finance.expense_dimension_contribution') IS NOT NULL AS ok`
  );
  if (!table.rows[0]?.ok) {
    throw new AppError(
      ErrorCode.INFRASTRUCTURE_UNAVAILABLE,
      'finance.expense_dimension_contribution is required (apply V076).',
      500
    );
  }

  const expense = await client.query<{
    moment_id: string;
    status: string;
    merchant_name: string | null;
    description: string | null;
    category_code: string | null;
    subcategory_code: string | null;
    shared_experience_code: string;
    shared_experience_label: string | null;
    amount: string;
    effective_at: Date;
  }>(
    `SELECT moment_id, status, merchant_name, description, category_code, subcategory_code,
            COALESCE(shared_experience_code, 'SELF') AS shared_experience_code,
            shared_experience_label, amount::text AS amount, effective_at
     FROM finance.expense WHERE expense_id = $1`,
    [expenseId]
  );
  const row = expense.rows[0];
  if (!row) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Expense not found.', 404);
  }

  let sourceEventId = opts?.sourceEventId;
  if (!sourceEventId) {
    const ev = await insertDomainEventAndOutbox(client, ctx, {
      eventName: 'ExpenseDimensionsSynced',
      domainCode: 'PERSONAL',
      aggregateType: 'EXPENSE',
      aggregateId: expenseId,
      scopeType: 'MOMENT',
      scopeId: row.moment_id,
      payload: { expenseId },
    });
    sourceEventId = ev.domainEventId;
  }

  if (opts?.deactivateAll || row.status === 'VOIDED' || row.status === 'DRAFT') {
    for (const dim of ['LIFE_OPERATIONS', 'RELATIONSHIPS', 'LIFESTYLE', 'FUTURE_BUILDING'] as DimensionCode[]) {
      const existing = await getContributionLink(client, expenseId, dim);
      if (existing?.linked_resource_id && existing.linked_resource_type === 'RELATIONSHIP_ACTIVITY') {
        await client.query(
          `UPDATE personal.relationship_activity SET status = 'VOIDED', updated_at = now()
           WHERE relationship_activity_id = $1`,
          [existing.linked_resource_id]
        );
        const relM =
          existing.target_moment_id ??
          (await resolveSetupMomentId(client, ctx.userId, 'RELATIONSHIPS'));
        if (relM) {
          await voidRecentActivityByActivityId(client, ctx.userId, relM, existing.linked_resource_id);
        }
      }
      if (existing?.linked_resource_id && existing.linked_resource_type === 'LIFESTYLE_ACTIVITY') {
        await client.query(
          `UPDATE personal.lifestyle_activity SET status = 'CANCELLED', updated_at = now()
           WHERE lifestyle_activity_id = $1`,
          [existing.linked_resource_id]
        );
        const lsM =
          existing.target_moment_id ??
          (await resolveSetupMomentId(client, ctx.userId, 'LIFESTYLE'));
        if (lsM) {
          await voidRecentActivityByActivityId(client, ctx.userId, lsM, existing.linked_resource_id);
        }
      }
      await upsertContribution(
        client,
        expenseId,
        dim,
        'INACTIVE',
        existing
          ? await resolveSetupMomentId(
              client,
              ctx.userId,
              dim === 'LIFE_OPERATIONS'
                ? 'LIFE_OPERATIONS'
                : dim === 'RELATIONSHIPS'
                  ? 'RELATIONSHIPS'
                  : dim === 'LIFESTYLE'
                    ? 'LIFESTYLE'
                    : 'FUTURE_BUILDING'
            )
          : null,
        existing?.linked_resource_type === 'RELATIONSHIP_ACTIVITY' ||
          existing?.linked_resource_type === 'LIFESTYLE_ACTIVITY'
          ? (existing.linked_resource_type as 'RELATIONSHIP_ACTIVITY' | 'LIFESTYLE_ACTIVITY')
          : 'NONE',
        existing?.linked_resource_id ?? null
      );
    }
    const relM = await resolveSetupMomentId(client, ctx.userId, 'RELATIONSHIPS');
    const lsM = await resolveSetupMomentId(client, ctx.userId, 'LIFESTYLE');
    if (relM) await refreshRelationshipsBondAxes(client, ctx.userId, relM, sourceEventId);
    if (lsM) await refreshLifestylePulseAxes(client, ctx.userId, lsM, sourceEventId);
    return;
  }

  const shared = (SHARED_EXPERIENCE_CODES.includes(row.shared_experience_code as SharedExperienceCode)
    ? row.shared_experience_code
    : 'SELF') as SharedExperienceCode;
  const effectiveAt = row.effective_at.toISOString();

  // LIFE_OPERATIONS — always ACTIVE for ordinary posted expenses (financial home).
  await upsertContribution(
    client,
    expenseId,
    'LIFE_OPERATIONS',
    'ACTIVE',
    row.moment_id,
    'NONE',
    null
  );

  // FUTURE_BUILDING — never for ordinary Master Expense.
  await upsertContribution(client, expenseId, 'FUTURE_BUILDING', 'INACTIVE', null, 'NONE', null);

  await upsertRelationshipContribution(
    client,
    ctx,
    expenseId,
    shared,
    row.shared_experience_label,
    row.merchant_name,
    row.amount,
    effectiveAt,
    sourceEventId
  );

  await upsertLifestyleContribution(
    client,
    ctx,
    expenseId,
    row.category_code,
    row.subcategory_code,
    row.merchant_name,
    row.description,
    effectiveAt,
    sourceEventId
  );
}

export async function listExpenseDimensionContributions(
  client: PoolClient,
  expenseId: string
): Promise<
  Array<{
    dimensionCode: string;
    status: string;
    targetMomentId: string | null;
    linkedResourceType: string;
    linkedResourceId: string | null;
  }>
> {
  const table = await client.query<{ ok: boolean }>(
    `SELECT to_regclass('finance.expense_dimension_contribution') IS NOT NULL AS ok`
  );
  if (!table.rows[0]?.ok) return [];

  const rows = await client.query<{
    dimension_code: string;
    status: string;
    target_moment_id: string | null;
    linked_resource_type: string;
    linked_resource_id: string | null;
  }>(
    `SELECT dimension_code, status, target_moment_id, linked_resource_type, linked_resource_id
     FROM finance.expense_dimension_contribution
     WHERE expense_id = $1
     ORDER BY dimension_code`,
    [expenseId]
  );
  return rows.rows.map((r) => ({
    dimensionCode: r.dimension_code,
    status: r.status,
    targetMomentId: r.target_moment_id,
    linkedResourceType: r.linked_resource_type,
    linkedResourceId: r.linked_resource_id,
  }));
}

/** @deprecated Use listExpenseDimensionContributions */
export async function listActiveContributions(
  client: PoolClient,
  expenseId: string
): Promise<Array<{ dimensionCode: string; status: string; targetMomentId: string | null }>> {
  const rows = await listExpenseDimensionContributions(client, expenseId);
  return rows.map(({ dimensionCode, status, targetMomentId }) => ({
    dimensionCode,
    status,
    targetMomentId,
  }));
}
