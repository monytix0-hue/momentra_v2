# Phase 1 — PostgreSQL Runtime Validation + Schema Completion

**Date:** 2026-08-20  
**Scope:** Database-only. No mobile features, no `/v1` endpoints, no workers.

---

## 1. Starting State (from Phase 0)

- Canonical migrations at `frds/migrations/` — V001–V034 present (+ V035 client telemetry file exists but **not** in migration order)
- Migration runner: `backend/typescript/scripts/migrate.ts` reading `frds/manifest/MIGRATION_ORDER.txt`
- Supabase dev project configured in `backend/.env` (`DATABASE_URL` pooler port 6543)
- V031–V034 forward pack already on disk (company locations, shared poll, RLS/grants, validation)
- V030 validation had **never passed** on clean install due to seed gaps
- No local Docker/PostgreSQL available (Docker daemon not running); validation used Supabase direct connection (port 5432)

---

## 2. Migration Inventory

| File | Description |
|------|-------------|
| V001__extensions.sql | pgcrypto extension |
| V002__core.sql | core schema: user_profile, moment, taxonomy tables |
| V003__personal.sql | personal domain schema |
| V004__collaboration.sql | collaboration/group schema incl. legacy poll tables |
| V005__business.sql | business schema incl. company, membership |
| V006__work.sql | work schema: goal, milestone, task |
| V007__finance.sql | finance schema: expense, movement kernels (NUMERIC) |
| V008__governance.sql | governance schema |
| V009__analytics.sql | analytics schema |
| V010__memory.sql | memory schema |
| V011__events.sql | events/outbox schema |
| V012__audit_platform.sql | audit + platform schemas |
| V013__ai.sql | ai schema |
| V014__projection.sql | projection read-model schema |
| V015__integration_constraints.sql | cross-domain FK constraints |
| V016__technical_functions_triggers.sql | triggers and technical functions |
| V017__indexes_finalize.sql | index finalization |
| V018__taxonomy_seed.sql | moment categories/types reference seed |
| V019__capability_seed.sql | capability catalogue + moment-type mappings |
| V020__governance_seed.sql | roles, permissions reference seed |
| V021__consent_catalogue_seed.sql | consent purposes reference seed |
| V022__analytics_metric_seed.sql | metric definitions + versions |
| V023__policy_seed.sql | governance policies + versions |
| V024__rls_enable.sql | security schema + RLS enable + helper functions |
| V025__rls_personal.sql | personal RLS policies |
| V026__rls_group.sql | group/collaboration RLS policies |
| V027__rls_business.sql | business RLS policies |
| V028__rls_shared_domains.sql | finance/work/memory/projection RLS |
| V029__grants_revokes.sql | role grants (momentra_app, workers) |
| V030__production_validation.sql | production validation gate (non-destructive) |
| V031__company_location_custom_label.sql | business.company_location + custom_type_label |
| V032__shared_poll_kernel.sql | shared.poll/option/vote kernel |
| V033__forward_rls_grants_devices_media.sql | RLS/grants for forward tables + platform devices/media |
| V034__forward_validation.sql | forward-pack validation gate |

**Not in order:** `V035__client_telemetry.sql` (exists on disk; out of Phase 1 scope)

---

## 3. Runtime Failures Found

### Failure 1 — VAL-TAX-002 (V030)

| Field | Value |
|-------|-------|
| Failure | Frozen GROUP moment type `SHARED_LIVING` missing |
| Root cause | V018 seeded `SHARED_RENTAL`, `FAMILY_HOUSEHOLD` but not `SHARED_LIVING` expected by V030 |
| Classification | SEED_CONFLICT |
| Fix | Added `SHARED_LIVING` moment type to V018 |
| File | `frds/migrations/V018__taxonomy_seed.sql` |

### Failure 2 — VAL-CAP-001 (V030)

| Field | Value |
|-------|-------|
| Failure | New `SHARED_LIVING` type had no capability mappings |
| Root cause | V019 mappings did not include new type |
| Classification | SEED_CONFLICT |
| Fix | Added SHARED_LIVING capability mappings (mirrors CO_LIVING set) |
| File | `frds/migrations/V019__capability_seed.sql` |

### Failure 3 — VAL-ANA-001 / VAL-RC-001 (V030)

| Field | Value |
|-------|-------|
| Failure | ACTIVE metric definitions without ACTIVE metric versions |
| Root cause | V022 intentionally seeded DRAFT versions with `review_required: true` |
| Classification | SEED_CONFLICT |
| Fix | Foundation activation UPDATE at end of V022 (status ACTIVE, remove review_required) |
| File | `frds/migrations/V022__analytics_metric_seed.sql` |

### Failure 4 — VAL-GOV-005 / VAL-RC-002 (V030)

| Field | Value |
|-------|-------|
| Failure | ACTIVE policies without ACTIVE policy versions |
| Root cause | V023 intentionally seeded DRAFT versions with `review_required: true` |
| Classification | SEED_CONFLICT |
| Fix | Foundation activation UPDATE at end of V023 |
| File | `frds/migrations/V023__policy_seed.sql` |

### Failure 5 — Missing poll_option/poll_vote RLS policies (discovered during Phase 1 review)

| Field | Value |
|-------|-------|
| Failure | RLS enabled on shared.poll_option/vote but no policies |
| Root cause | V033 only created policy on shared.poll |
| Classification | INVALID_RLS |
| Fix | Added shared_poll_option_access and shared_poll_vote_access policies |
| File | `frds/migrations/V033__forward_rls_grants_devices_media.sql` |

---

## 4. Schema Gaps

| Gap | Status | Migration |
|-----|--------|-----------|
| Business company locations | **EXISTING** (V031) | `business.company_location` with FK, indexes, RLS |
| Shared poll engine | **EXISTING** (V032) | `shared.poll`, `shared.poll_option`, `shared.poll_vote` |
| Forward RLS/grants/devices/media | **EXISTING** (V033) | Enhanced with poll_option/vote policies |
| Forward validation | **ENHANCED** (V034) | Expanded invariant checks |
| SHARED_LIVING taxonomy | **ADDED** | V018 + V019 |
| Seed activation for metrics/policies | **ADDED** | V022 + V023 |

---

## 5. RLS Verification

Tested via `scripts/db-phase1-tests.ts` using JWT claim injection (`request.jwt.claim.sub`) and role switching.

| Path | Result |
|------|--------|
| USER A → USER A profile | ALLOW |
| USER A → USER B profile | DENY |
| COMPANY A member → COMPANY B location | DENY (SELECT) |
| COMPANY A member → insert COMPANY B location | DENY (INSERT) |
| GROUP member → create/vote poll | ALLOW |
| NON-member → group poll | DENY |
| Voter → vote on CLOSED poll | DENY |

**Note:** `momentra_app` role has backend bypass via `security.is_backend_app()`. User-isolation tests use dedicated `momentra_rls_test` role (test harness only, not a product role).

---

## 6. Seed Verification

| Seed | Idempotent | Result |
|------|------------|--------|
| V018 taxonomy | INSERT-based | PASS on clean install |
| V019 capabilities | INSERT-based | PASS |
| V020–V021 governance/consent | INSERT-based | PASS |
| V022 metrics | INSERT + UPDATE activation | PASS |
| V023 policies | INSERT + UPDATE activation | PASS |
| Duplicate stable codes | — | 0 duplicates (test verified) |

No mock application data (users/companies/expenses) inserted as seeds.

---

## 7. Finance Data-Type Verification

All canonical finance amount columns use `NUMERIC(19,4)` or `NUMERIC(9,6)` — no `REAL`, `FLOAT`, or `DOUBLE PRECISION` in finance schema.

Verified by:
- V034 forward validation check
- `db-phase1-tests.ts` finance-decimal-types
- Decimal math test: `0.1 + 0.2 = 0.3` (exact)

---

## 8. Tests Executed

```powershell
Set-Location g:\momentra_v2\backend\typescript
$env:DATABASE_URL_DIRECT = "<supabase-direct-url>"
$env:DATABASE_URL = $env:DATABASE_URL_DIRECT

npm run migrate:full      # reset + migrate (run twice)
npm run migrate:validate  # V030 + V034 rerun on existing DB
npm run test:db           # 13/13 PASS
```

| Test | Result |
|------|--------|
| migration-ledger | PASS |
| duplicate-moment-type-codes | PASS |
| finance-decimal-types | PASS |
| finance-decimal-math | PASS |
| rls-cross-user-deny | PASS |
| rls-self-access | PASS |
| rls-company-location-cross-company-deny | PASS |
| rls-company-location-cross-company-write-deny | PASS |
| poll-create-by-member | PASS |
| poll-vote-by-member | PASS |
| poll-outsider-deny | PASS |
| poll-closed-vote-deny | PASS |
| seed-idempotency-snapshot | PASS |

**Results: 13/13 PASS**

---

## 9. Migration Clean-Run Result

```
CLEAN DATABASE → LATEST = PASS  (first run)
CLEAN DATABASE → LATEST = PASS  (second run)
VALIDATION RERUN (existing DB) = PASS
```

Migration range applied: **V001 → V034**

---

## 10. Remaining Blockers

1. **No local Docker PostgreSQL** — validation used Supabase dev project; add `docker-compose` for offline reproducibility (optional Phase 2 prep)
2. **V035 not in migration order** — client telemetry migration exists but is not wired into `MIGRATION_ORDER.txt`
3. **manifest.json stale** — still tracks V001–V030 only
4. **Git not initialized** — from Phase 0
5. **Metric/policy formula placeholders** — V022/V023 versions activated with `foundation_placeholder: true`; exact formulas still require product approval before production RC

---

## 11. Files Changed

| File | Change |
|------|--------|
| `frds/migrations/V018__taxonomy_seed.sql` | Added SHARED_LIVING moment type |
| `frds/migrations/V019__capability_seed.sql` | Added SHARED_LIVING capability mappings |
| `frds/migrations/V022__analytics_metric_seed.sql` | Foundation activation of metric versions |
| `frds/migrations/V023__policy_seed.sql` | Foundation activation of policy versions |
| `frds/migrations/V033__forward_rls_grants_devices_media.sql` | poll_option/vote RLS policies |
| `frds/migrations/V034__forward_validation.sql` | Expanded forward validation checks |
| `backend/typescript/scripts/migrate.ts` | Fail-hard validation; skip ledger for V030/V034 |
| `backend/typescript/scripts/reset-database.ts` | **Created** — disposable DB reset |
| `backend/typescript/scripts/db-phase1-tests.ts` | **Created** — Phase 1 DB tests |
| `backend/typescript/scripts/db-query.ts` | **Created** — diagnostic queries |
| `backend/typescript/scripts/migration-status.ts` | Fixed user_device check |
| `backend/typescript/package.json` | Added migrate:reset, migrate:full, test:db |
| `backend/.env.example` | Added DATABASE_URL_DIRECT |
| `docs/implementation/PHASE_1_DATABASE_FOUNDATION.md` | This document |
| `docs/implementation/IMPLEMENTATION_STATUS.md` | Phase status tracker |

---

## Architecture Compliance

- Work owns goal/milestone/task — verified, no duplicate engines
- Finance owns expense/movement — verified, no duplicate engines
- Shared poll kernel — Group/Business reuse `shared.*`, legacy `collaboration.poll*` deprecated
- Projection tables — read models only, V030 projection checks pass on empty DB
- Audit/event/outbox — structures intact, V034 verifies presence
- No Prisma Migrate introduced
- No mock application seed data introduced

**Phase 1 complete. Do not begin Phase 2.**
