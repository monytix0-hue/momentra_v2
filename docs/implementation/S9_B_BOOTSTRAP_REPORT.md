# S9-B — Startup / Bootstrap Optimization

**Status:** PASS  
**Target:** `GET /v1/me` warm p95 ≤ **400 ms** (same harness as S9-A)  
**Captured:** 2026-08-26T16:45:31Z  
**Harness:** `backend/typescript/scripts/s9-b-remeasure.ts`  
**V030:** untouched

---

## Before / after

| Metric | S9-A (before) | S9-B (after) | Delta |
|--------|---------------|--------------|-------|
| Warm `/v1/me` p50 | 790.51 ms | **115.20 ms** | −85% |
| Warm `/v1/me` p95 | 835.55 ms | **115.82 ms** | −86% (≤400 ✓) |
| Warm `/v1/me` p99 | 847.92 ms | **116.15 ms** | −86% |
| Cold `/v1/me` | 1427.06 ms | **1350.17 ms** | −5% (see residual) |
| Handler DB RTT layers (empty) | 6 sequential | **1 parallel wave** | |
| Query count (empty inventory) | 6 (+ provision upsert) | **5 parallel** (capabilities skipped) | |
| Provision warm path | UPSERT every req ~111 ms | **cached skip ~0 ms** (hit rate 1.0) | |
| Peak pool `totalCount` in bootstrap | 1 client | **5** (max 10) | |
| Payload size | ~649 B | ~646 B | unchanged |
| `/v1/me` response cache | none | **none** (correctness-first) | |
| Inventory fan-out | none | **none** | |

**Gate:** warm p95 **115.82 ≤ 400 → PASS**

---

## What changed

1. **`ensureUserProfile`** (`platform/auth`) — SELECT once if unknown; UPSERT only when missing; **in-process known-user TTL** skips DB on warm auth. Recovery path still provisions when row absent (`getMeBootstrap` re-reads after provision).
2. **Parallel bootstrap** — independent reads use **separate pool checkouts** + `Promise.all` (not one `PoolClient`, which would serialize).
3. **Coalesce** — capabilities JOIN skipped when personal/group/business inventories are empty (baseline shell caps only).
4. **No `/v1/me` response cache** — deferred; warm already at DB RTT floor.

---

## Residual (cold)

Cold remains ~**1.3 s**. Warm `/health/ready` ≈ **115 ms** ≈ DB floor; warm `/v1/me` matches that floor. Cold is dominated by **first-request work** (profile create + opening multiple PG connections into an empty pool), not by the sequential query stack. Further cold gains need connection warm-up / pool pre-connect (S9-C), not more `/v1/me` redesign.

With inventory present, expect **~2 RTT layers** (parallel inventory + capabilities) ≈ ~230 ms in this environment — still under 400 ms.

---

## Secondary checks

| Check | Result |
|-------|--------|
| No startup inventory fan-out | ✓ shell inventory only |
| Auth still required | ✓ |
| Profile recovery if missing | ✓ `getMeBootstrap` provisions + re-reads |
| Cache correctness | ✓ no stale `/me` body cache; provision skip is existence-only |

---

## Next

**S9-K** — mount/verify rate limiting (P0), authorized after B proven.
