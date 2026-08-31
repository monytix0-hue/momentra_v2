# S9-F — Personal Performance Validation

**Status:** PASS (engineering targets; expense preferred target missed on geo RTT floor)  
**Authorized block:** S9-F + S9-J  
**Harness:** `backend/typescript/scripts/s9-fj-personal-measure.ts`  
**Raw JSON:** [`S9_F_PERSONAL_MEASURE.json`](./S9_F_PERSONAL_MEASURE.json)  
**V030:** NOT RUN  
**S9-B/C/G/H:** not reopened (no regression found that required reopening)

---

## Acceptance rule

User-visible ready time matters more than single-endpoint latency. Pulse tab content waits on **both** pulse and activity; sequential client fetch ≈ sum of RTTs.

---

## Engineering targets vs measured (warm / populated)

| Target | Goal | Measured | Meet? |
|--------|------|----------|-------|
| Personal Pulse warm p95 | ≤ 300 ms | **242 ms** | Yes |
| Activity first page p95 | ≤ 300 ms | **122 ms** | Yes |
| Activity page 2 p95 | — | **121 ms** | OK |
| Moment switch visible-ready (parallel tab fetch) p95 | ≤ 500 ms | **223 ms** | Yes |
| Moment switch sequential client proxy p95 | — | **336 ms** | — |
| Expense submit p95 | ≤ 1000 ms preferred | **1403 ms** | No (RTT floor) |
| Health ready p50 (floor) | — | **~106 ms** | — |
| Cached shell paint | immediate where available | Client BootstrapCache + `ttcsMs` (see S9-J) | Instrumented |

Expense miss is the same class of remote Postgres RTT stacking as Business write ~920 ms P1 — **not** chased further in this block; carry-forward with Business write P1.

---

## Empty / setup inventory

| Flow | Empty p95 | Populated notes |
|------|-----------|-----------------|
| `/v1/me` warm | ~240 ms | Inventory grows body; still healthy |
| `/v1/personal/pulse` | ~112 ms | Scoped `?momentId` ~242 ms (extra work) |
| `/v1/personal/activity` | ~113 ms | Page1 ~122 ms; page2 ~121 ms |
| `/v1/personal/life` | ~224 ms | Seeded ~3.3 KB payload |
| `/v1/personal/memory` | ~3 ms | Empty projection |
| `/v1/personal/moments` | ~113 ms | Lists seeded moments |
| `/v1/personal/setups` | ~119 ms | Catalog + mine |

Cold `/v1/me` (fresh user): ~670 ms (pool prewarmed) — consistent with S9-C residual.

---

## Waterfall / duplicate refresh findings

### Fixed in this block (Android Personal)

`PersonalPulseActiveContent` (and sibling Personal Moments/Memory/Pulse family screens) previously awaited `getPulse` then `getActivity` **sequentially**.

| Mode | p50 | p95 |
|------|-----|-----|
| Sequential (pre-fix client behavior) | **333 ms** | **335 ms** |
| Parallel (shipped) | **222 ms** | p50-stable; one outlier in sample |

**Moment switch** (destination moment only):

| Mode | p95 |
|------|-----|
| Sequential | 336 ms |
| Parallel | **223 ms** (~113 ms saved) |

iOS Personal Pulse already used `async let` (parallel) — parity preserved.

### Scoped refresh (no full-context reload)

- Tab / moment changes bump `personalTabRefreshToken` only → personal facet GETs.
- Unit test: `momentSwitchBumpsScopedRefreshWithoutClearingIdentity` — moment switch does **not** issue another `/v1/me` bootstrap.
- Expense / Quick Add saves continue to call `refreshVisiblePersonalTab()` (existing pattern).
- Full `/v1/me` reserved for bootstrap / inventory changes (`reloadCurrentContext` on moment create).

### Unnecessary refreshes (documented, not changed)

`selectBottomDestination` refreshes the visible tab on every tab change while ACTIVE. Acceptable for correctness; optional future debounce out of scope.

---

## Quick Add

| Aspect | Result |
|--------|--------|
| Open | Local `BottomDestination.CREATE` / sheet state — near-instant (no network) |
| Expense submit | API p95 ~1403 ms (preferred ≤1s missed; geo floor) |

---

## Explicit non-goals honored

- No new Redis caches for `/v1/me` / pulse / finance lists  
- No product scope expansion  
- No S9-B/C/G/H reopen  
- No V030  

---

## Carry-forward

| Item | Status |
|------|--------|
| Personal expense submit p95 ~1.4 s | Prefer ≤1 s — **P1** alongside Business write ~920 ms |
| Device-side visible-ready numbers | See **S9-J** (Android device absent this host; iOS BLOCKED_ENVIRONMENT) |

**STOP** after S9-J report. Do not start S9-L–P.
