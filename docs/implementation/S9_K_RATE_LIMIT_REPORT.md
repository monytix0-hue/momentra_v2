# S9-K — Rate Limiting (P0)

**Status:** PASS  
**Authorized after:** S9-B proven  
**Finding closed:** PERF_FINDING-003  
**V030:** untouched

---

## What shipped

| Item | Detail |
|------|--------|
| Mount | `rateLimitMiddleware` on live `v1Router` **after** `authMiddleware` |
| Key | `requestContext.userId` (fallback IP) |
| Redis | Preferred when `REDIS_URL` set |
| No Redis | **In-memory** limiter (still enforces; not Noop) |
| Break-glass | `RATE_LIMIT_DISABLED=1` → Noop |
| Defaults | `RATE_LIMIT_WINDOW_SEC=60`, `RATE_LIMIT_MAX=120` |
| 429 | `ErrorCode.RATE_LIMITED` + `Retry-After` header |

---

## Verification

`tests/s9-k-rate-limit.test.ts` — **2/2 PASS**

- 429 after per-user max exceeded  
- Limits isolated across users  

`tests/runtime-parity.test.ts` — still **PASS** with limiter mounted.

---

## Notes

- Redis errors remain **fail-open** on the Redis path (availability); absence of Redis uses memory enforcement.
- Telemetry sub-router is mounted before the shared auth/limit chain (pre-existing). Follow-up if abuse on `/v1/telemetry` is observed (not required to close S9-K).
