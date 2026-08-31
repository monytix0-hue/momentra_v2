/**
 * Typed projection invalidation hints — matches OpenAPI ProjectionHint.
 * Public codes only; never DB object names.
 */
export const PROJECTION_CODES = [
  'personal.pulse',
  'personal.moments',
  'personal.life',
  'personal.memory',
  'personal.activity',
  'group.moments',
  'group.pulse',
  'business.moments',
  'business.pulse',
  'business.finance',
] as const;

export type ProjectionCode = (typeof PROJECTION_CODES)[number];
export type ProjectionHintAction = 'invalidate' | 'refresh';

export interface ProjectionHint {
  projection: ProjectionCode;
  action: ProjectionHintAction;
}

/** Map legacy runtime string codes to stable public projection codes. */
const LEGACY_MAP: Record<string, ProjectionCode> = {
  PERSONAL_MOMENTS: 'personal.moments',
  PERSONAL_PULSE: 'personal.pulse',
  PERSONAL_LIFE: 'personal.life',
  PERSONAL_MEMORY: 'personal.memory',
  PERSONAL_ACTIVITY: 'personal.activity',
  GROUP_MOMENTS: 'group.moments',
  GROUP_PULSE: 'group.pulse',
  BUSINESS_MOMENTS: 'business.moments',
  BUSINESS_PULSE: 'business.pulse',
  BUSINESS_FINANCE: 'business.finance',
};

export function projectionHint(
  projection: ProjectionCode,
  action: ProjectionHintAction = 'invalidate'
): ProjectionHint {
  return { projection, action };
}

export function toProjectionHints(
  codes: Array<ProjectionCode | string>,
  action: ProjectionHintAction = 'invalidate'
): ProjectionHint[] {
  const out: ProjectionHint[] = [];
  const seen = new Set<string>();
  for (const code of codes) {
    const mapped =
      (PROJECTION_CODES as readonly string[]).includes(code)
        ? (code as ProjectionCode)
        : LEGACY_MAP[code];
    if (!mapped || seen.has(mapped)) continue;
    seen.add(mapped);
    out.push({ projection: mapped, action });
  }
  return out;
}
