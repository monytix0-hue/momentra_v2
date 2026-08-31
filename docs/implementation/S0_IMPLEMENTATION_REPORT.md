# S0 Platform Foundation — Implementation Report

**Date:** 2026-08-26  
**Scope:** S0-A through S0-H only  
**Verdict:** **PASS** (declared S0 scope)  
**Next:** **STOP** — do not begin S1

Authoritative plan: sequential A→H. This report is the acceptance gate before any S1 work.

---

## Executive summary

| Letter | Work | Result |
|--------|------|--------|
| S0-A | Audit + identity freeze + V030 migrate block | **PASS** |
| S0-B | Auth correctness (no Firebase UID as Momentra userId) | **PASS** |
| S0-C | Expand `GET /v1/me` shell bootstrap (no Redis required) | **PASS** |
| S0-D | Native SWR bootstrap cache (iOS + Android) | **PASS** |
| S0-E | Networking parity (correlation, timeouts, GET retry, SSE Bearer) | **PASS** |
| S0-F | Redis optional infrastructure; `/health/ready` PG-only | **PASS** |
| S0-G | Outbox → BullMQ with legal V011 statuses | **PASS** |
| S0-H | Observability, timings, tests, this report | **PASS** |

Unresolved **FAIL** in declared S0 scope: **none**.

---

## Acceptance gate

| Criterion | Result | Evidence |
|-----------|--------|----------|
| One `GET /v1/me` paints shell inventory | **PASS** | `modules/device/bootstrap.ts`; OpenAPI `MeResponse`; runtime-parity test |
| Cached bootstrap paints shell offline (SWR) | **PASS** | Android `BootstrapCache` + `MeRepository`; iOS `ShellMeGateway` / UserDefaults cache |
| Firebase identity never leaks into Momentra `userId` | **PASS** | `MomentraIdentityCache` (iOS); Android identity cache; offline restore uses UUID or stays restoring |
| iOS / Android bootstrap/auth/error parity (Apple iOS-only) | **PASS** | Shared rules; Apple Sign-In remains iOS-only |
| No extra startup Moment/company list after bootstrap | **PASS** | `AppShellViewModel` / `AppShellModel` use `getBootstrap()` inventory |
| Redis failure does not break core commands | **PASS** | Redis client fail-open; rate limiter fail-open; ready check is PG-only |
| Outbox can reach `PUBLISHED` and enqueue BullMQ | **PASS** | `backend/workers/outbox-dispatcher`; `outbox-bullmq.test.ts`; enqueue best-effort |
| No mobile CRUD touches FastAPI | **PASS** | FastAPI remains stub; clients call Node `/v1` only |
| V030 cannot execute | **PASS** | `scripts/migrate.ts` `isV030Blocked` |
| V031–V040 remain preserved | **PASS** | Files on disk; migrate still runs non-V030 forward pack |
| Report has no unresolved FAIL in S0 scope | **PASS** | This document |

---

## Phase notes

### S0-A — Audit + identity freeze

- Wrote [`docs/implementation/CURRENT_IMPLEMENTATION_AUDIT.md`](CURRENT_IMPLEMENTATION_AUDIT.md)
- Wrote [`docs/platform/IDENTITY_ARCHITECTURE.md`](../platform/IDENTITY_ARCHITECTURE.md)
- Root `CURRENT_IMPLEMENTATION_AUDIT.md` is a pointer (supersedes 2026-08-24 stale audit)
- Identity freeze: `firebase:<projectId>:<uid>` → UUIDv5 → `core.user_profile` → `RequestContext`
- Freeze statements recorded: `auth.uid()` ≠ Firebase UID; mobile never talks to PG; Node Governance is mobile authz; V024–V029 preserved; `SET ROLE momentra_app` deferred; V031–V040 preserved
- Migrate tooling always skips V030

### S0-B — Authentication correctness

- iOS offline restore: never assign Firebase UID to Momentra `userId`
- Android: same rule via cached Momentra UUID / restoring without fake id
- Bearer injection + one-shot 401 refresh retained; logout clears current-user caches
- Android OkHttp: Authorization / sensitive headers redacted
- Production fail-closed retained; **no** `SET ROLE`

### S0-C — `GET /v1/me` bootstrap

Assembler: `backend/typescript/src/modules/device/bootstrap.ts`  
Payload includes: profile, supportedContexts, currentlySelectedContext, bounded activeMoments, companies/selectedCompany, permissions/capabilities, preferences, featureFlags.  
**Excluded:** Pulse / Life / Memory / Activity projections.

### S0-D — Native SWR

- Per-user bootstrap cache keyed by Momentra `userId`
- Paint last-good → refresh `/v1/me` → merge
- Logout clears **current user only**
- No offline Pulse/Life/Memory/Activity projection storage in S0

### S0-E — Networking parity

- `X-Correlation-Id` on requests
- Aligned timeouts (~20s)
- Bounded **GET-only** retry; POST retry still requires Idempotency-Key
- SSE: Bearer header only (removed `?token=` query)

### S0-F — Redis

- `ioredis` client optional via `REDIS_URL`
- `RedisRateLimiter` when Redis present, else Noop; fail-open
- Dokploy compose: Redis service + `REDIS_URL` for API/workers
- `/health/ready` remains **PostgreSQL-only**
- Idempotency source of truth remains `platform.idempotency_record`

### S0-G — BullMQ bridge

Legal path (V011 untouched):

```text
PENDING → PROCESSING (locked_by, locked_at)
  → enqueue BullMQ (outside claim TX; best-effort)
  → PUBLISHED (published_at)
  → projection-worker acknowledges via events.event_consumer_state
```

Workers live under `backend/workers/outbox-dispatcher` and `backend/workers/projection-worker`.  
Bounded processors only — no full projection rebuild in S0.

### S0-H — Observability + proof

- Structured JSON request logs with `requestId` / `durationMs`
- Node Sentry behind optional `SENTRY_DSN` (`platform/observability/sentry.ts`)
- Mobile: `SentryBootstrap` entry points no-op when DSN blank (iOS Info.plist / Android BuildConfig)
- Hygiene: no `*.sync-conflict-*` remaining
- iOS Xcode/device run: **BLOCKED_ENVIRONMENT** (Windows host)

#### Latency baseline (carry forward — not an S0 failure)

**Captured:** 2026-08-26 via `backend/typescript/scripts/s0-perf-sample.ts` (supertest + live DB).  
**Status:** Acceptable for S0 PASS; **too slow for final product experience**. Preserve and improve in later performance passes — do not lose this baseline.

| Endpoint | n | p50 | p95 | max |
|----------|---|-----|-----|-----|
| `GET /v1/me` | 20 | **995 ms** | **1023 ms** | 1540 ms |
| `POST /v1/me/devices` | 10 | **1320 ms** | **1354 ms** | 1354 ms |

Includes pool + remote Postgres round-trips. S0 required bootstrap **without** Redis; query/fan-in and cache tuning belong to later performance work, not S1 shell visuals unless explicitly scoped.

#### Tests run

| Suite | Result |
|-------|--------|
| Backend `npm test` (56 tests) | **PASS** (0 fail) |
| `tests/outbox-bullmq.test.ts` | **PASS** (included) |
| Android `AppShellViewModelTest` | **PASS** (BUILD SUCCESSFUL) |
| iOS XCTest / device | **BLOCKED_ENVIRONMENT** — source-level tests added (`IdentityCacheTests.swift`); not executed on this host |

---

## Explicitly deferred (not S0 — do not treat as FAIL)

- Pulse list Redis TTLs / projection rebuild algorithms
- Memory / analytics workers as product features
- FastAPI AI implementation
- Broad offline tab projections
- Shell visual redesign / S1 Figma parity
- Production RLS redesign including `SET ROLE momentra_app`
- Production mock-data cleanup (Relationships demo, Group demo contacts, business empty metrics) — documented in audit only
- Linking full mobile Sentry SDKs when DSN is set (bootstrap hooks present; SDK package install is ops follow-up)

---

## Artifacts

| Artifact | Path |
|----------|------|
| Audit | `docs/implementation/CURRENT_IMPLEMENTATION_AUDIT.md` |
| Identity freeze | `docs/platform/IDENTITY_ARCHITECTURE.md` |
| This report | `docs/implementation/S0_IMPLEMENTATION_REPORT.md` |
| Perf sampler | `backend/typescript/scripts/s0-perf-sample.ts` |

---

## STOP

S0 Platform Foundation is complete for declared scope. **Do not start S1** until product owners explicitly open the next phase.
