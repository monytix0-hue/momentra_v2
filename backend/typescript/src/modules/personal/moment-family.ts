import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';

/** Unified Personal family codes (client Life chips / journey). */
export type PersonalFamilyCode =
  | 'LIFE_OPERATIONS'
  | 'FUTURE_BUILDING'
  | 'LIFESTYLE'
  | 'RELATIONSHIPS';

export function personalFamilyFromMomentTypeCode(momentTypeCode: string | null | undefined): PersonalFamilyCode {
  const code = (momentTypeCode ?? '').toUpperCase();
  if (code.startsWith('LIFE_') || code === 'LIFE_OPERATIONS' || code === 'LIFE_RHYTHM') {
    return 'LIFE_OPERATIONS';
  }
  if (code.startsWith('FUTURE_') || code === 'FUTURE_BUILDING') {
    return 'FUTURE_BUILDING';
  }
  if (code.startsWith('LIFESTYLE')) {
    return 'LIFESTYLE';
  }
  if (code.startsWith('RELATIONSHIP_') || code === 'RELATIONSHIPS') {
    return 'RELATIONSHIPS';
  }
  return 'LIFE_OPERATIONS';
}

export function personalFamilyLabel(family: PersonalFamilyCode): string {
  switch (family) {
    case 'LIFE_OPERATIONS':
      return 'Everyday';
    case 'FUTURE_BUILDING':
      return 'Future';
    case 'LIFESTYLE':
      return 'Lifestyle';
    case 'RELATIONSHIPS':
      return 'People';
  }
}

async function loadPersonalMomentType(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<string | null> {
  const row = await client.query<{ moment_type_code: string }>(
    `SELECT mt.code AS moment_type_code
     FROM core.moment m
     JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     WHERE m.moment_id = $1 AND pmc.user_id = $2 AND m.domain_code = 'PERSONAL'`,
    [momentId, ctx.userId]
  );
  return row.rows[0]?.moment_type_code ?? null;
}

/** Ownership + Everyday / Life Ops type — mood, recovery, Everyday spend home. */
export async function assertLifeOpsMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<void> {
  const typeCode = await loadPersonalMomentType(client, ctx, momentId);
  if (!typeCode) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
  if (personalFamilyFromMomentTypeCode(typeCode) !== 'LIFE_OPERATIONS') {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Everyday actions require an Everyday (Life Operations) moment. Set up Everyday first.',
      400
    );
  }
}

export async function assertPersonalFamilyMoment(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expected: PersonalFamilyCode,
  label: string
): Promise<void> {
  const typeCode = await loadPersonalMomentType(client, ctx, momentId);
  if (!typeCode) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
  if (personalFamilyFromMomentTypeCode(typeCode) !== expected) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      `${label} actions require a ${label} moment.`,
      400
    );
  }
}
