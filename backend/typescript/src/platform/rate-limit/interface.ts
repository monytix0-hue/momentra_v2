import type { Request, Response, NextFunction } from 'express';
import { AppError, ErrorCode } from '../errors/errors';
import { getRedis } from '../redis/client';
import { rateLimitBucketKey } from '../redis/key-contract';

/**
 * Rate-limit interface.
 * Prefers Redis when available; otherwise in-process memory (still enforces).
 * Redis soft-fail falls through to memory (S9-D) — core commands stay available with enforcement.
 * Set RATE_LIMIT_DISABLED=1 only for deliberate fail-open (tests / break-glass).
 */
export interface RateLimitResult {
  allowed: boolean;
  retryAfterSec?: number;
  /** When true, CompositeRateLimiter must use memory fallback. */
  softFail?: boolean;
}

export interface RateLimiter {
  check(key: string): Promise<RateLimitResult>;
}

export class NoopRateLimiter implements RateLimiter {
  async check(): Promise<RateLimitResult> {
    return { allowed: true };
  }
}

function envInt(name: string, fallback: number): number {
  const raw = process.env[name]?.trim();
  if (!raw) return fallback;
  const n = parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

export class MemoryRateLimiter implements RateLimiter {
  private readonly buckets = new Map<string, { count: number; resetAt: number }>();

  constructor(
    private readonly windowSec = envInt('RATE_LIMIT_WINDOW_SEC', 60),
    private readonly maxRequests = envInt('RATE_LIMIT_MAX', 120)
  ) {}

  async check(key: string): Promise<RateLimitResult> {
    const now = Date.now();
    const windowMs = this.windowSec * 1000;
    let bucket = this.buckets.get(key);
    if (!bucket || bucket.resetAt <= now) {
      bucket = { count: 0, resetAt: now + windowMs };
      this.buckets.set(key, bucket);
    }
    bucket.count += 1;
    if (bucket.count > this.maxRequests) {
      return {
        allowed: false,
        retryAfterSec: Math.max(1, Math.ceil((bucket.resetAt - now) / 1000)),
      };
    }
    return { allowed: true };
  }
}

export class RedisRateLimiter implements RateLimiter {
  constructor(
    private readonly windowSec = envInt('RATE_LIMIT_WINDOW_SEC', 60),
    private readonly maxRequests = envInt('RATE_LIMIT_MAX', 120)
  ) {}

  async check(key: string): Promise<RateLimitResult> {
    const redis = getRedis();
    if (!redis) {
      return { allowed: true, softFail: true };
    }
    try {
      if (redis.status !== 'ready') {
        await redis.connect().catch(() => undefined);
      }
      const bucket = rateLimitBucketKey(key, this.windowSec);
      const count = await redis.incr(bucket);
      if (count === 1) {
        await redis.expire(bucket, this.windowSec + 1);
      }
      if (count > this.maxRequests) {
        return { allowed: false, retryAfterSec: this.windowSec };
      }
      return { allowed: true };
    } catch {
      return { allowed: true, softFail: true };
    }
  }
}

/** Redis when healthy; memory when Redis absent or soft-fails. */
export class CompositeRateLimiter implements RateLimiter {
  constructor(
    private readonly primary: RateLimiter,
    private readonly fallback: RateLimiter
  ) {}

  async check(key: string): Promise<RateLimitResult> {
    const redis = getRedis();
    if (!redis) {
      return this.fallback.check(key);
    }
    const result = await this.primary.check(key);
    if (result.softFail) {
      return this.fallback.check(key);
    }
    return result;
  }
}

function buildDefaultLimiter(): RateLimiter {
  if (
    process.env.RATE_LIMIT_DISABLED === '1' ||
    process.env.RATE_LIMIT_DISABLED === 'true'
  ) {
    return new NoopRateLimiter();
  }
  return new CompositeRateLimiter(new RedisRateLimiter(), new MemoryRateLimiter());
}

let limiter: RateLimiter = buildDefaultLimiter();

export function setRateLimiter(next: RateLimiter): void {
  limiter = next;
}

export function resetRateLimiterFromEnv(): void {
  limiter = buildDefaultLimiter();
}

export function rateLimitMiddleware(keyFn: (req: Request) => string = (req) => req.ip ?? 'unknown') {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const result = await limiter.check(keyFn(req));
      if (!result.allowed) {
        if (result.retryAfterSec) {
          res.setHeader('Retry-After', String(result.retryAfterSec));
        }
        next(
          new AppError(ErrorCode.RATE_LIMITED, 'Rate limit exceeded.', 429, {
            retryAfterSec: result.retryAfterSec,
          })
        );
        return;
      }
      next();
    } catch (e) {
      next(e);
    }
  };
}
