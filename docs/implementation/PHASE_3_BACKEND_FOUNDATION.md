# Phase 3 — Node Backend Platform Foundation

**Status:** COMPLETE  
**Date:** 2026-08-20

## 1. Starting state

Before Phase 3 the repository already had a substantial Express/TypeScript control plane:

- Firebase Admin verification + UUIDv5 identity
- `RequestContext`, correlation middleware, `runCommand` + idempotency store
- Governance resolver (moment/company scoped)
- Audit / domain event / outbox writers
- `/v1` router with ~58 routes
- OpenAPI freeze from Phase 2 with known mismatch: `projectionHints` as `string[]`

Phase 3 hardened and aligned this platform; it did **not** implement all 61 contracted APIs as product features.

## 2. Runtime stack

| Component | Choice |
|-----------|--------|
| Node | v24.15.0 (local) |
| Framework | Express 5 |
| Database | `pg` Pool + parameterized SQL |
| Validation | Zod |
| Firebase | `firebase-admin` ID token verify |
| Logging | Structured JSON stdout |
| Tests | Node.js `node:test` + supertest |
| OpenAPI | `momentra-v1.yaml` + swagger-parser |

## 3. Request flow

```text
HTTP
 → correlationMiddleware (X-Correlation-Id UUID or server UUID)
 → requestLogMiddleware (no tokens logged)
 → /health/* (no auth) | /v1/* (authMiddleware)
 → Firebase verifyIdToken | FAIL-CLOSED | ALLOW_DEV_AUTH (non-prod only)
 → provisionUserProfile (deterministic UUIDv5)
 → RequestContext (frozen)
 → Zod validation
 → Governance.authorize / assertGovernanceAllowed
 → withTransaction / runCommand
 → canonical write + audit + domain_event + outbox
 → COMMIT
 → commandEnvelope | projectionEnvelope (typed projectionHints)
```

Python/FastAPI, Redis, Celery, and FCM are **not** on this path.

## 4. Identity model

```text
Firebase UID + projectId
  → provider key: firebase:<projectId>:<uid>
  → canonical user_id = UUIDv5(namespace, provider key)
  → core.user_profile upsert (ON CONFLICT user_id)
```

Same Firebase identity always resolves to the same Momentra user. Concurrent resolution does not create duplicates.

## 5. Governance

Public API: `authorize(client, ctx, input)` → `{ allowed }` or deny reason.

Supported scopes proven in tests:

| Context | Rule |
|---------|------|
| Personal | `ownerUserId` must equal actor `userId` |
| Group | GROUP moment organizer or ACTIVE participant |
| Business | ACTIVE `company_membership` for requested `companyId` |

Client-supplied IDs are request **scope** only. Authority comes from PostgreSQL. RLS remains enabled (Phase 1); Node Governance does not replace it.

## 6. Transactions

`withTransaction` / `runCommand`:

- BEGIN → work → COMMIT
- On error: ROLLBACK (canonical + audit + event + outbox)
- No external network calls inside the transaction (Firebase verify happens before)

## 7. Idempotency

Persisted in `platform.idempotency_record` (not process memory).

- Advisory `pg_advisory_xact_lock` + `FOR UPDATE` for replica-safe races
- Same key + same payload → replay
- Same key + different payload → `409 IDEMPOTENCY_CONFLICT`

## 8. OCC

Mutable moment commands use `expectedVersion`. Stale version → `409 VERSION_CONFLICT` with `{ resourceId, expectedVersion, currentVersion }`.

## 9. Audit / event / outbox

Writers in `platform/events/outbox.ts`. Device registration (Phase 3 proof) writes all three in the same transaction as the device row.

## 10. projectionHints alignment

| Before | After |
|--------|-------|
| `string[]` e.g. `['PERSONAL_MOMENTS']` | `{ projection: 'personal.moments', action: 'invalidate' }[]` |

Helper: `platform/projections/hints.ts` (`toProjectionHints`).

## 11. `/v1/me`

Bootstrap only: `userId`, `email`, `displayName`, `firebaseUid`, `roles`.

Explicitly **excluded**: Pulse, Moments lists, Finance, Life, Memory, AI, workers.

Response uses `projectionEnvelope` (`data` + `correlationId` + `status`).

## 12. Health

| Endpoint | Semantics |
|----------|-----------|
| `GET /health/live` | `{ status: "ok" }` — process alive |
| `GET /health/ready` | `{ status: "ok" }` if PostgreSQL reachable; else `503 { status: "degraded" }` |

Does **not** depend on Redis, Celery, FCM, or AI.

## 13. Transactional proof command

**`POST /v1/me/devices`** (with `Idempotency-Key`)

Why safe:

- Canonical table already exists (`platform.user_device`)
- Scoped to authenticated user only
- No Moment/Expense/Group product surface
- Exercises auth → governance → validation → idempotency → tx → audit → event → outbox → envelope

## 14. Tests

```text
npm test → 36/36 PASS
```

Includes:

- Production fail-closed config
- Auth 401 / dev identity
- Identity determinism + concurrency
- Personal / Group / Business governance isolation
- Device idempotency retry + conflict + race
- Transactional rollback
- OCC VERSION_CONFLICT details
- Health + `/v1/me` + runtime envelope parity

## 15. Deferred contract APIs (unchanged)

| Item | Status |
|------|--------|
| Settlement | GAP |
| Budget | GAP |
| Vendor | GAP |
| Circle CRUD | DEFERRED (`GET /life360` only) |
| Poll vote/close | CONTRACT_ONLY |

No V035. No invented product behavior.

## 16. Files changed (primary)

- `src/platform/config.ts` — typed config + production fail-closed
- `src/platform/errors/errors.ts` — typed `projectionHints`, `RATE_LIMITED`
- `src/platform/projections/hints.ts` — **new**
- `src/platform/idempotency/store.ts` — advisory lock race hardening
- `src/platform/observability/correlation.ts` — UUID validation
- `src/platform/observability/logging.ts` — **new**
- `src/platform/rate-limit/interface.ts` — **new** (noop; Redis deferred)
- `src/modules/governance/resolver.ts` — `authorize` + Personal/Group/Business
- `src/modules/device/service.ts` — transactional proof + audit/event/outbox
- `src/modules/moment/service.ts` — VERSION_CONFLICT details
- `src/api/middleware/auth.ts` — production-safe dev auth
- `src/api/v1/router.ts` — `/me` envelope, device `runCommand`, typed hints
- `src/app.ts` — health OpenAPI parity, logging, CORS harden
- `tests/platform-foundation.test.ts` — **new**
- `tests/runtime-parity.test.ts` — **new**
- `scripts/route-coverage.ts` — **new**
- OpenAPI rebuild (device idempotency + RATE_LIMITED)
- Generated clients regenerated
- `docs/implementation/PHASE_3_*.md`
- `docs/implementation/IMPLEMENTATION_STATUS.md`

## 17. Known blockers

**none**

## 18. Android / iOS

Generated clients regenerated from bundled OpenAPI. Android `assembleDebug` remains PASS with generated output outside `app/src` (`apk/openapi-generated/`). iOS generated under `momentra/momentra/API/Generated/` (compile on macOS/Xcode).
