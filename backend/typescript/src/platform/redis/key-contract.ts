/**
 * S9-D Redis key contract — authorization scope + invalidation owner.
 * Canonical commands must not depend on Redis.
 */

export type CacheKeyContract = {
  prefix: string;
  /** What bounds authorization for this key (must not cross tenants). */
  authorizationScope: string;
  ttlSec: number | 'window';
  /** Who owns writes / invalidation of this key. */
  invalidationOwner: string;
  reader: string;
  examplePattern: string;
};

/** Authoritative inventory of Redis JSON / counter keys used by the API. */
export const REDIS_KEY_CONTRACT: readonly CacheKeyContract[] = [
  {
    prefix: 'analytics:insight:',
    authorizationScope: 'userId + scopeType + scopeId (momentId | companyId | userId)',
    ttlSec: 300,
    invalidationOwner: 'analytics-worker (SETEX overwrite on compute)',
    reader: 'listInsightsForScope (STALE fallback when PG empty)',
    examplePattern: 'analytics:insight:{userId}:{scopeType}:{scopeId}',
  },
  {
    prefix: 'rl:',
    authorizationScope: 'userId (else IP) + time window bucket',
    ttlSec: 'window',
    invalidationOwner: 'rate-limiter (TTL expiry)',
    reader: 'rate-limiter',
    examplePattern: 'rl:{subject}:{windowBucket}',
  },
] as const;

export const ANALYTICS_INSIGHT_TTL_SEC = 300;

/** Build analytics insight cache key — never omit userId or scope ids. */
export function analyticsInsightCacheKey(
  userId: string,
  scopeType: string,
  scopeId: string
): string {
  if (!userId || !scopeType || !scopeId) {
    throw new Error('analyticsInsightCacheKey requires userId, scopeType, and scopeId');
  }
  return `analytics:insight:${userId}:${scopeType}:${scopeId}`;
}

/** Rate-limit bucket key (subject already scoped by caller to userId or IP). */
export function rateLimitBucketKey(subject: string, windowSec: number, nowMs = Date.now()): string {
  const bucket = Math.floor(nowMs / (windowSec * 1000));
  return `rl:${subject}:${bucket}`;
}

/** Prefix-only for logs (no PII payloads). */
export function cacheKeyPrefix(key: string): string {
  if (key.startsWith('analytics:insight:')) return 'analytics:insight:';
  if (key.startsWith('rl:')) return 'rl:';
  const i = key.indexOf(':');
  return i > 0 ? key.slice(0, i + 1) : key;
}

export function assertKeyMatchesContract(key: string): boolean {
  return REDIS_KEY_CONTRACT.some((c) => key.startsWith(c.prefix));
}
