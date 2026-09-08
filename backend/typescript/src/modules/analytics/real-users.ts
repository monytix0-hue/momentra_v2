/**
 * Shared definition of a "real user" for Founder / VC / Product KPIs.
 *
 * Analytics facts are keyed by `user_id`, and every authenticated request
 * provisions a `core.user_profile` row — including the dev-auth bypass and the
 * seeded accounts that QA/perf scripts create. Counting those rows inflates
 * headline user KPIs, so KPIs count only accounts that belong to a person:
 *
 * - Seed/test accounts use non-routable domains (`*.local`, `*.test`,
 *   `example.*`, `test.*`) — excluded.
 * - Firebase identities without an email get `<uuid>@users.momentra.local`
 *   auto-filled. Those are real people (phone or provider sign-in) whenever the
 *   profile carries a display name or phone — kept.
 */
const REAL_USER_PREDICATE = `
  up.status = 'ACTIVE'
  AND CASE
        WHEN COALESCE(up.email, '') ILIKE '%@users.momentra.local'
          THEN up.display_name IS NOT NULL OR up.phone IS NOT NULL
        ELSE
          COALESCE(up.email, '') <> ''
          AND up.email NOT ILIKE '%.local'
          AND up.email NOT ILIKE '%.test'
          AND up.email NOT ILIKE '%.invalid'
          AND up.email NOT ILIKE '%.localhost'
          AND up.email NOT ILIKE '%@example.%'
          AND up.email NOT ILIKE '%@test.%'
      END
`;

/**
 * SQL predicate restricting a `user_id` expression to a real account.
 * Uses a correlated EXISTS so it can be appended to any WHERE clause without
 * changing row cardinality. The `up` alias is scoped to the subquery.
 */
export function realUserExists(userIdExpr: string): string {
  return `EXISTS (
    SELECT 1 FROM core.user_profile up
    WHERE up.user_id = ${userIdExpr}
      AND ${REAL_USER_PREDICATE}
  )`;
}

/** Same rule, applied to a `core.user_profile up` row already in scope. */
export const REAL_USER_PROFILE_FILTER = REAL_USER_PREDICATE;

/**
 * Human-readable name for a joined `core.user_profile`. Falls back to the email
 * local part, except for auto-filled placeholder addresses whose local part is
 * just the user UUID. NULL when nothing readable exists.
 */
export function userDisplayNameSql(alias: string): string {
  return `COALESCE(
    NULLIF(${alias}.display_name, ''),
    CASE
      WHEN COALESCE(${alias}.email, '') ILIKE '%@users.momentra.local'
        OR COALESCE(${alias}.email, '') ILIKE '%@dev.momentra.local'
        THEN NULL
      ELSE NULLIF(split_part(${alias}.email, '@', 1), '')
    END
  )`;
}

/** Email for display — placeholder addresses are hidden. */
export function userEmailSql(alias: string): string {
  return `CASE
    WHEN COALESCE(${alias}.email, '') ILIKE '%@users.momentra.local'
      OR COALESCE(${alias}.email, '') ILIKE '%@dev.momentra.local'
      THEN NULL
    ELSE NULLIF(${alias}.email, '')
  END`;
}

/**
 * Moment activity is attributed to a real person via the moment creator.
 * `analytics_core.moment_daily.creator_user_id` records whoever first touched
 * the moment that day, so the lifecycle fact is preferred when present.
 * Pair the join with the filter, keeping the `md` / `mlf` aliases.
 */
export const REAL_CREATOR_MOMENT_DAILY_JOIN =
  'LEFT JOIN analytics_core.moment_lifecycle_fact mlf ON mlf.moment_id = md.moment_id';

export const REAL_CREATOR_MOMENT_DAILY_FILTER = realUserExists(
  'COALESCE(mlf.creator_user_id, md.creator_user_id)'
);
