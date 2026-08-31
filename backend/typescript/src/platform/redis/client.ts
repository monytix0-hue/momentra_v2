import Redis from 'ioredis';
import { config } from '../config';
import { cacheKeyPrefix } from './key-contract';

let redis: Redis | null = null;
let redisDisabled = false;

/** Returns Redis client or null when REDIS_URL unset / connection failed. Core commands must not require Redis. */
export function getRedis(): Redis | null {
  if (redisDisabled) return null;
  const url = process.env.REDIS_URL?.trim();
  if (!url) return null;
  if (!redis) {
    redis = new Redis(url, {
      maxRetriesPerRequest: 1,
      enableReadyCheck: true,
      lazyConnect: true,
    });
    redis.on('error', (err) => {
      console.log(
        JSON.stringify({
          level: 'warn',
          msg: 'redis_error',
          err: String(err),
        })
      );
    });
  }
  return redis;
}

export async function withRedisTiming<T>(
  op: string,
  fn: (client: Redis) => Promise<T>
): Promise<T | null> {
  const client = getRedis();
  if (!client) return null;
  const started = Date.now();
  try {
    if (client.status !== 'ready') {
      await client.connect().catch(() => undefined);
    }
    const result = await fn(client);
    console.log(
      JSON.stringify({
        level: 'debug',
        msg: 'redis_op',
        op,
        durationMs: Date.now() - started,
      })
    );
    return result;
  } catch (e) {
    console.log(
      JSON.stringify({
        level: 'warn',
        msg: 'redis_op_failed',
        op,
        durationMs: Date.now() - started,
        err: String(e),
      })
    );
    return null;
  }
}

function logCacheEvent(
  event: 'cache_hit' | 'cache_miss' | 'cache_stale_served' | 'cache_set',
  key: string,
  extra?: Record<string, unknown>
): void {
  console.log(
    JSON.stringify({
      level: 'info',
      msg: event,
      keyPrefix: cacheKeyPrefix(key),
      ...extra,
    })
  );
}

/** Invalidate-only projection cache keys — never cache canonical truth as source. */
export async function invalidateProjectionKeys(keys: string[]): Promise<void> {
  if (!keys.length) return;
  await withRedisTiming('invalidate', async (client) => {
    if (keys.length === 1) await client.del(keys[0]!);
    else await client.del(...keys);
  });
}

export async function cacheGetJson<T>(key: string): Promise<T | null> {
  const raw = await withRedisTiming('get', (c) => c.get(key));
  if (raw == null) {
    logCacheEvent('cache_miss', key);
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as T;
    logCacheEvent('cache_hit', key);
    return parsed;
  } catch {
    logCacheEvent('cache_miss', key, { reason: 'json_parse' });
    return null;
  }
}

/** Log STALE insight served from Redis when PG had no rows. */
export function logCacheStaleServed(key: string): void {
  logCacheEvent('cache_stale_served', key);
}

export async function cacheSetJson(key: string, value: unknown, ttlSec: number): Promise<void> {
  const ok = await withRedisTiming('setex', (c) => c.setex(key, ttlSec, JSON.stringify(value)));
  if (ok !== null) {
    logCacheEvent('cache_set', key, { ttlSec });
  }
}

export function redisConfigured(): boolean {
  return Boolean(process.env.REDIS_URL?.trim()) && !redisDisabled;
}

/** Test helper — force Redis off without mutating env permanently. */
export function disableRedisForTests(): void {
  redisDisabled = true;
  redis = null;
}

/** Test helper — re-enable Redis client construction. */
export function enableRedisForTests(): void {
  redisDisabled = false;
  redis = null;
}

export async function closeRedis(): Promise<void> {
  if (redis) {
    await redis.quit().catch(() => undefined);
    redis = null;
  }
}

void config;
