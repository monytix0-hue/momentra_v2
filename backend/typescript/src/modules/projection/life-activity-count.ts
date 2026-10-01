/**
 * Category-inferred Lifestyle mirrors of a Master Expense are not a second family.
 * Shared-experience rows (a different activity code) stay countable.
 */
export const INFERRED_LIFESTYLE_MIRROR_SOURCE = 'MASTER_EXPENSE';
export const INFERRED_LIFESTYLE_MIRROR_CODE = 'LIFESTYLE_EXPERIENCE';

export function isInferredLifestyleExpenseMirror(
  activityCode: string,
  source: string | null | undefined,
): boolean {
  return source === INFERRED_LIFESTYLE_MIRROR_SOURCE && activityCode === INFERRED_LIFESTYLE_MIRROR_CODE;
}

/** SQL predicate: keep the row in Life activity counts and highlights. */
export function lifeActivityCountExclusionSql(alias?: string): string {
  const payload = alias ? `${alias}.activity_payload` : 'activity_payload';
  const code = alias ? `${alias}.activity_code` : 'activity_code';
  return `NOT (
    COALESCE(${payload}->>'source', '') = '${INFERRED_LIFESTYLE_MIRROR_SOURCE}'
    AND ${code} = '${INFERRED_LIFESTYLE_MIRROR_CODE}'
  )`;
}

export type LifeActivityCountFamily =
  | 'LIFE_OPERATIONS'
  | 'FUTURE_BUILDING'
  | 'LIFESTYLE'
  | 'RELATIONSHIPS';

export function tallyLifeActivityFamilies(
  rows: Array<{ activityCode: string; source: string | null; family: LifeActivityCountFamily }>,
): Record<LifeActivityCountFamily, number> {
  const counts: Record<LifeActivityCountFamily, number> = {
    LIFE_OPERATIONS: 0,
    FUTURE_BUILDING: 0,
    LIFESTYLE: 0,
    RELATIONSHIPS: 0,
  };
  for (const row of rows) {
    if (isInferredLifestyleExpenseMirror(row.activityCode, row.source)) continue;
    counts[row.family] += 1;
  }
  return counts;
}
