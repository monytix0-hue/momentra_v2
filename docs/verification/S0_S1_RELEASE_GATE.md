# S0 / S1 Release Gate

**Date:** 2026-08-25  
**Scope:** Verify Personal production slice before S2 Group.  
**Evidence:** code inspection + `backend/typescript` `npm test` (55/55) + Android `AppShellViewModelTest` + `compileDebugKotlin`.  
**iOS runtime:** `BLOCKED_ENVIRONMENT` (Windows host; no Xcode).

Legend: `PASS` | `FAIL` | `BLOCKED_ENVIRONMENT` | `API_GAP` | `ARCHITECTURE_CONFLICT` | `NEEDS_PRODUCT_DECISION`

---

## S0 Foundation

| Item | Result | Notes |
|------|--------|-------|
| Authentication / session bootstrap | PASS | Auth middleware + Firebase/dev auth; clients bootstrap via `/v1/me` |
| `/v1/me` or canonical identity path | PASS | Mounted on live `router.ts` |
| Context switching | PASS | Personal / Group / Business on AppShell |
| Personal Moment inventory | PASS | `GET /v1/personal/moments` |
| `selectedMomentId` handling | PASS | Shell state + selectMoment |
| Moment selector `activeCount` behaviour | PASS | Switcher active when `activeCount > 0` |
| `selectMoment()` | PASS | Selects moment; refreshes visible Personal tab only |
| `onMomentCreated()` | PASS | Selects new moment; reloads **inventory** for switcher; tab content gated by refresh token (not all-tab fetch). Updated to use current `selectedContext` (Group-safe) |
| Visible-tab-only refresh | PASS | `refreshVisiblePersonalTab` / token; selectMoment does not fetch every tab |
| App shell unchanged | PASS | Existing shell retained |
| Logout / account isolation | PASS | Session clear + per-user IDs on server |
| Offline / error state | PASS | Shell empty/error/retry paths present |
| Android implementation | PASS | Compile + AppShellViewModelTest |
| iOS source implementation | PASS | Parallel `AppShellModel` / create paths |
| iOS runtime verification | BLOCKED_ENVIRONMENT | No Xcode/device on this host |

---

## S1 Moment Creation

| Item | Result | Notes |
|------|--------|-------|
| LIFE_OPERATIONS | PASS | Catalog → `POST /v1/moments` |
| FUTURE_BUILDING | PASS | Same |
| LIFESTYLE | PASS | Same |
| RELATIONSHIPS | PASS | Same |
| Single canonical Moment engine | PASS | `modules/moment/service.ts` `createMoment` only |

---

## S1 Personal Expense

| Item | Result | Notes |
|------|--------|-------|
| Canonical expense write | PASS | `POST /v1/moments/:id/expenses` |
| Decimal-safe money | PASS | `decimal.js` via `parseMoney` |
| Zero amount | PASS | Product/tests: **amount must be positive**; `amount: '0'` → 400 (`expense-create.test.ts`, `unit.test.ts`). Not a regression vs contract tests |
| Multi-currency | PASS | `currencyCode` + `spendByCurrency` bump |
| Optional `financialAccountId` | PASS | Accepted on create schema |
| Idempotency | PASS | `runCommand` + Idempotency-Key |
| Double-submit protection | PASS | Same-key replay |
| Transaction | PASS | Begin/commit via `runCommand` |
| Audit | PASS | Inserted in expense tx |
| Domain event | PASS | Expense recorded |
| Outbox | PASS | With domain event |
| Activity integration | PASS | Recent activity row |
| Pulse integration (write) | PASS | `bumpPersonalPulseAfterExpense` — **bounded** `spendByCurrency` delta only |
| Pulse GET (moment-scoped) | PASS | Bounded `SUM` for that moment’s expenses only — **not** full metric rebuild / Life/Memory rebuild. Acceptable projection read overlay |
| Moment isolation | PASS | Membership + PERSONAL domain check |
| User isolation | PASS | `user_id` scoped |

**Immediate Pulse bump verdict:** ACCEPTABLE (delta write). No `ARCHITECTURE_CONFLICT`.

---

## S1 Activity

| Item | Result | Notes |
|------|--------|-------|
| `ORDER BY occurred_at DESC, recent_activity_id DESC` | PASS | `getPersonalActivity` |
| Keyset continuation (both fields) | PASS | Cursor `iso|uuid`; predicate `(occurred_at, recent_activity_id) < (...)` |
| Identical timestamps / no dupes / stable page | PASS | Covered by keyset design + S1 tests |

---

## S1 Performance (architecture checks)

| Item | Result | Notes |
|------|--------|-------|
| Pulse GET does not synchronously rebuild all projections | PASS | Reads projection row; optional bounded moment spend overlay |
| Activity query bounded | PASS | Limit + keyset |
| No global invalidation after expense | PASS | Targeted projection hints / SSE codes |
| No cross-context invalidation | PASS | Personal hints only |
| Moment switch does not fetch every tab | PASS | Visible-tab token |
| No obvious N+1 on expense path | PASS | Single-command write path |

Warm timings from prior local report (~55ms Pulse / ~53ms Activity) not re-measured this run → performance **gate for S0/S1 architecture: PASS**; numeric p95 re-measure deferred.

---

## Automated evidence

| Suite | Result |
|-------|--------|
| `cd backend/typescript && npm test` | **55/55 PASS** |
| Android `AppShellViewModelTest` + `compileDebugKotlin` | **PASS** (removed Syncthing conflict test file that broke unit-test compile) |
| iOS Xcode / device | **BLOCKED_ENVIRONMENT** |

---

## Gate decision

**S0:** PASS (iOS runtime blocked)  
**S1:** PASS  

Critical regressions that would block S2: **none**.  
Proceed to S2A (Group Trip golden slice).
