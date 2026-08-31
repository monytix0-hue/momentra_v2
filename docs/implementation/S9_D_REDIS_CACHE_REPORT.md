# S9-D — Redis / Cache Hardening

**Status:** PASS  
**Authorized block:** S9-D + S9-E (this report covers D only)  
**V030:** NOT RUN  
**Acceptance:** Canonical writes succeed based on PostgreSQL. Redis failures may delay derived state only — never corrupt or block commands.

---

## Key contract (authorization scope + invalidation owner)

Source of truth: `backend/typescript/src/platform/redis/key-contract.ts`

| Prefix | Authorization scope | TTL | Invalidation owner | Reader |
|--------|---------------------|-----|--------------------|--------|
| `analytics:insight:` | `userId` + `scopeType` + `scopeId` (momentId \| companyId \| userId) | 300s | analytics-worker (`SETEX` overwrite on compute) | `listInsightsForScope` (STALE when Redis hit + PG empty) |
| `rl:` | `userId` (else IP) + time window bucket | window + TTL | rate-limiter (TTL expiry) | rate-limiter |

Rules enforced:

- Every cache key includes the authorizing subject and scope ids that bound the data.
- No key may cross `userId` / company / moment boundaries.
- Redis remains optional acceleration — never a dependency for canonical commands.
- Finance / membership / outbox rows remain Postgres-only.
- No new `/v1/me`, pulse, or finance-list caches in this pass (S9-B already fixed those without Redis).

`scopeType` (`MOMENT` \| `COMPANY` \| `USER`) implies domain context; an extra PERSONAL/GROUP/BUSINESS segment was **not** added — mapping is documented as acceptable.

---

## What shipped

| Item | Detail |
|------|--------|
| D1 Inventory | `REDIS_KEY_CONTRACT` + `analyticsInsightCacheKey` / `rateLimitBucketKey` |
| D2 Instrumentation | Structured `cache_hit` / `cache_miss` / `cache_set` / `cache_stale_served` (prefix only, no PII payloads) in `platform/redis/client.ts` |
| D3 Invalidation | Insight refresh continues to overwrite via `SETEX`; STALE path logged; `invalidateProjectionKeys` left unwired (no projection cache keys) |
| D4 Soft-fail | Redis rate-limiter errors set `softFail`; `CompositeRateLimiter` falls through to `MemoryRateLimiter` (enforcement preserved when Redis is flaky) |
| Readiness | `/health/ready` remains Postgres-only |

---

## Verification

`npx tsx --test tests/s9-d-redis-cache.test.ts` — **PASS** (suite within joint D/E run: **13/13**)

| Case | Result |
|------|--------|
| Key contract documents scope + invalidation owner | PASS |
| Analytics / rate-limit key builders | PASS |
| Redis soft-fail → memory rate limit still enforces | PASS |
| `GET /v1/me` with Redis disabled | **200** |
| `POST` group expense with Redis disabled | **201** (canonical write) |

---

## Explicit non-goals (honored)

- No Redis cache for `/v1/me`, pulse, or finance lists  
- No schema redesign  
- Business write RTT chase deferred (carry-forward P1)

---

## Carry-forward

| Item | Status |
|------|--------|
| Business write p95 ~920 ms | **P1** — not in S9-D scope |
| Next | S9-F + S9-J (after D/E reports) — **not started** |

**STOP** after S9-E report. No F/J/L–P. No V030.
