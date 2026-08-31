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

/** Deterministic fixture emails (override via .env.maestro.local). */
export const QA_FIXTURE_ALIASES = [
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

export type QaFixtureAlias = (typeof QA_FIXTURE_ALIASES)[number];

export function defaultQaEmail(alias: QaFixtureAlias): string {
  const slug = alias.replace(/^QA_/, '').toLowerCase().replace(/_/g, '.');
  return `qa.${slug}@test.com`;
}

export function defaultQaPassword(): string {
  return process.env.QA_DEFAULT_PASSWORD || 'MaestroQa123';
}
