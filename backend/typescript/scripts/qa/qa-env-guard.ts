/**
 * Hard guard for Maestro QA fixture scripts — never touch production.
 */
export function assertQaFixturesSafe(scriptName: string): void {
  const nodeEnv = (process.env.NODE_ENV || 'development').toLowerCase();
  if (nodeEnv === 'production') {
    throw new Error(`[${scriptName}] Refused: NODE_ENV=production`);
  }

  const enabled =
    process.env.QA_FIXTURES_ENABLED === '1' || process.env.QA_FIXTURES_ENABLED === 'true';
  if (!enabled) {
    throw new Error(
      `[${scriptName}] Refused: set QA_FIXTURES_ENABLED=true (and NODE_ENV!=production)`
    );
  }

  const db = (process.env.DATABASE_URL || '').toLowerCase();
  const blockedHints = ['prod', 'production', 'rds.amazonaws.com', 'azure.com', 'cloudsql'];
  // Allow local compose / localhost even if URL contains unrelated substrings.
  const isLocal =
    db.includes('localhost') ||
    db.includes('127.0.0.1') ||
    db.includes('@postgres:') ||
    db.includes('host=localhost');
  if (db && !isLocal) {
    for (const hint of blockedHints) {
      if (db.includes(hint)) {
        throw new Error(
          `[${scriptName}] Refused: DATABASE_URL looks non-local (${hint}). QA fixtures are local-only.`
        );
      }
    }
  }
}

/** Legacy smoke/critical fixture aliases. */
export const QA_LEGACY_FIXTURE_ALIASES = [
  'QA_EMPTY',
  'QA_PERSONAL',
  'QA_GROUP_OWNER',
  'QA_GROUP_MEMBER',
  'QA_GROUP_OUTSIDER',
  'QA_BUSINESS_OWNER',
  'QA_BUSINESS_MEMBER',
  'QA_BUSINESS_OUTSIDER',
  'QA_MULTI_CONTEXT',
] as const;

/**
 * S9-QA-C — Platform-isolated certification accounts.
 * Android and iOS never share these during platform certification.
 */
export const QA_PLATFORM_FIXTURE_ALIASES = [
  'QA_APK_PERSONAL',
  'QA_APK_GROUP_OWNER',
  'QA_APK_GROUP_MEMBER',
  'QA_APK_GROUP_OUTSIDER',
  'QA_APK_BUSINESS_OWNER',
  'QA_APK_BUSINESS_MEMBER',
  'QA_APK_BUSINESS_OUTSIDER',
  'QA_IOS_PERSONAL',
  'QA_IOS_GROUP_OWNER',
  'QA_IOS_GROUP_MEMBER',
  'QA_IOS_GROUP_OUTSIDER',
  'QA_IOS_BUSINESS_OWNER',
  'QA_IOS_BUSINESS_MEMBER',
  'QA_IOS_BUSINESS_OUTSIDER',
] as const;

/** All fixture aliases used by reset/seed. */
export const QA_FIXTURE_ALIASES = [
  ...QA_LEGACY_FIXTURE_ALIASES,
  ...QA_PLATFORM_FIXTURE_ALIASES,
] as const;

export type QaFixtureAlias = (typeof QA_FIXTURE_ALIASES)[number];
export type QaPlatformFixtureAlias = (typeof QA_PLATFORM_FIXTURE_ALIASES)[number];

export function defaultQaEmail(alias: QaFixtureAlias): string {
  const slug = alias.replace(/^QA_/, '').toLowerCase().replace(/_/g, '.');
  return `qa.${slug}@test.com`;
}

export function defaultQaPassword(): string {
  return process.env.QA_DEFAULT_PASSWORD || 'MaestroQa123';
}

/** Map platform alias → seed profile. */
export function platformAliasProfile(alias: QaPlatformFixtureAlias): {
  platform: 'android' | 'ios';
  role: 'personal' | 'group_owner' | 'group_member' | 'group_outsider' | 'business_owner' | 'business_member' | 'business_outsider';
} {
  const platform = alias.startsWith('QA_APK_') ? 'android' : 'ios';
  const rest = alias.replace(/^QA_(APK|IOS)_/, '');
  const roleMap: Record<string, ReturnType<typeof platformAliasProfile>['role']> = {
    PERSONAL: 'personal',
    GROUP_OWNER: 'group_owner',
    GROUP_MEMBER: 'group_member',
    GROUP_OUTSIDER: 'group_outsider',
    BUSINESS_OWNER: 'business_owner',
    BUSINESS_MEMBER: 'business_member',
    BUSINESS_OUTSIDER: 'business_outsider',
  };
  const role = roleMap[rest];
  if (!role) throw new Error(`Unknown platform alias role: ${alias}`);
  return { platform, role };
}
