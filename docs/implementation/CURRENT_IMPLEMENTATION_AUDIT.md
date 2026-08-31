# Current Implementation Audit

**Date:** 2026-08-26  
**Scope:** Full repository readiness before S0 platform foundation (A–H)  
**Authoritative sources:** Figma `TzLvwVwlPbeVB8ug1zB3GM`, SQL V001–V040, OpenAPI `momentra-v1.yaml`, live tree  
**Supersedes:** root `CURRENT_IMPLEMENTATION_AUDIT.md` (2026-08-24 — stale)

---

## Executive summary

| Area | Verdict | Notes |
|------|---------|-------|
| Backend control plane | **EXISTING** | Express modular monolith; Firebase → UUIDv5; Governance; outbox writers |
| Redis / BullMQ / Sentry | **MISSING** | Required for S0-F/G/H |
| `GET /v1/me` | **PARTIAL** | Identity-only; shell fans out to moment/company lists |
| Workers | **ARCHITECTURE_CONFLICT** | Illegal outbox statuses vs V011 |
| iOS / Android shells | **EXISTING_PARTIAL** | Shell + Personal/Group product UI ahead of platform bootstrap |
| Schema | **V001–V040 present** | V030 must not execute; V031–V040 preserved |
| iOS device build | **BLOCKED_ENVIRONMENT** | Windows host; no Xcode |

**Do not rewrite working domain services.** S0 hardens platform only.

---

## 1. Backend architecture

```text
HTTP → correlation → request log → /health | /v1 (authMiddleware)
  → Firebase verifyIdToken | ALLOW_DEV_AUTH (non-prod)
  → UUIDv5 user_id + core.user_profile upsert
  → RequestContext (frozen)
  → Zod → Governance → runCommand / withTransaction
  → canonical + audit + domain_event + outbox
  → commandEnvelope | projectionEnvelope
```

| Piece | Path | Status |
|-------|------|--------|
| Entry | `backend/typescript/src/index.ts`, `app.ts` | EXISTS |
| Mounted router | `api/v1/router.ts` | Shell + S1 Personal + S2A invites |
| Unmounted product | `api/v1/router-product.ts` | Larger surface; not mounted |
| Platform | `platform/*` | auth, db, outbox, idempotency, observability, rate-limit noop |
| Realtime | `realtime/sse.ts` | In-process SSE; `?token=` query risk |
| Deploy | `backend/deploy/dokploy/docker-compose.yml` | API + workers + FastAPI; **no Redis** |

**Authorization path for mobile:** Node Governance (`modules/governance/resolver.ts`), not RLS. Pool does not `SET ROLE momentra_app` and does not set JWT claims for `security.current_user_id()`.

---

## 2. TypeScript modules

| Module | Path | Role |
|--------|------|------|
| ai | `modules/ai/` | Action proposal execute |
| business | `modules/business/` | Companies, locations, setup |
| collaboration | `modules/collaboration/` | Participants, invites, polls |
| device | `modules/device/` | `getMe`, device register/revoke |
| finance | `modules/finance/` | Expenses, movements |
| governance | `modules/governance/` | App-layer authorize |
| media | `modules/media/` | Upload intent; fake signed URL |
| moment | `modules/moment/` | Create/lifecycle, setup prefs |
| personal | `modules/personal/` | Observations, lifestyle, setup |
| projection | `modules/projection/` | Pulse/moments/life/memory/activity reads |
| telemetry | `modules/telemetry/` | Client event ingest + admin |
| work | `modules/work/` | Goals / milestones / tasks |

---

## 3. FastAPI

| Path | Status |
|------|--------|
| `backend/python/fastapi-ai/main.py` | Stub: `/health/live`, `/v1/inference` → `{status:"stub"}` |
| `backend/python/ai-context/` | Placeholder context builder |

**No mobile CRUD calls FastAPI.** Correct architecture; AI not implemented.

---

## 4. Firebase

| Surface | Status |
|---------|--------|
| Backend `firebase-admin` verifyIdToken | EXISTS |
| UUIDv5 identity + profile upsert | EXISTS |
| Android `google-services.json` | EXISTS |
| iOS `GoogleService-Info.plist` | EXISTS |
| Dev bypass `X-Dev-Firebase-Uid` | EXISTS (fail-closed in production) |
| FCM send | Stub only in notification worker |

---

## 5. PostgreSQL connection

| Item | Detail |
|------|--------|
| Client | `pg` Pool (`platform/database/pool.ts`) |
| URL | `DATABASE_URL` (pooler); migrations prefer `DATABASE_URL_DIRECT` |
| Prisma | Generator stub only; **not** used for migrate/runtime |
| Migrations | `scripts/migrate.ts` ← `frds/migrations/` + `MIGRATION_ORDER.txt` |

---

## 6. Migrations present

Canonical: `frds/migrations/` **V001–V040**.

| Range | Purpose |
|-------|---------|
| V001–V017 | Schema DDL |
| V018–V023 | Seeds |
| V024–V029 | RLS + grants |
| **V030** | Production validation gate — **must not execute in normal/feature development** |
| V031–V040 | Forward pack (locations, polls, telemetry, setups, invites) — **preserve** |

Phase copies under `frds/Phase1`…`phase6` duplicate V001–V030 history.

---

## 7. Redis

**MISSING.** `NoopRateLimiter` only (`platform/rate-limit/interface.ts`). No `ioredis` / Redis service in Dokploy compose.

---

## 8. BullMQ / workers

**BullMQ MISSING.** Workers are PG poll loops:

| Worker | Behavior |
|--------|----------|
| outbox-dispatcher | `PENDING` → writes illegal `DISPATCHED` / `dispatched_at` |
| projection-worker | claims `DISPATCHED` → writes illegal `PROCESSED` / `processed_at` |
| notification / analytics / memory / scheduler | Stubs |

**V011 CHECK:** `PENDING|PROCESSING|PUBLISHED|FAILED|DEAD_LETTER|CANCELLED` + `published_at`.  
**ARCHITECTURE_CONFLICT** — fix workers; do not modify V011.

Outbox **writers** in command TX are correct (`platform/events/outbox.ts`).

---

## 9. OpenAPI

| Artifact | Status |
|----------|--------|
| `openapi/momentra-v1.yaml` (+ schemas) | Authoritative |
| `MeResponse` | Identity-only (`userId`, email, displayName, firebaseUid, roles) |
| Codegen | Kotlin + Swift scripts; hand clients primary on mobile |
| Coverage | Mounted router ⊂ full contract |

---

## 10. Android architecture

| Area | Status |
|------|--------|
| Package | `com.example.momentra` (placeholder applicationId) |
| UI | Compose + Material3; `AppShellScreen` / `AppShellViewModel` |
| Auth | Email, Google (OAuthProvider), Phone; **no Apple** |
| API | Retrofit `ApiClient` / `ApiService`; 30s timeouts |
| Token | Bearer + OkHttp Authenticator one-shot refresh |
| Local | SharedPreferences only (onboarding, telemetry, selected moment, idempotency) |
| SWR bootstrap cache | **MISSING** |
| Client `X-Correlation-Id` | **MISSING** |
| Sentry / Crashlytics | **MISSING** |
| SSE | `SseClient.kt` exists, **unwired** |
| Cleartext | `usesCleartextTraffic=true` |
| Debug logging | OkHttp BODY in DEBUG — token leak risk |
| Offline restore bug | On network failure sets `userId = Firebase uid` |

---

## 11. iOS architecture

| Area | Status |
|------|--------|
| Bundle | `resolvingpoint.momentra` |
| UI | SwiftUI; `AppShellView` / `AppShellModel` |
| Auth | Email, Google, **Apple**, Phone |
| API | Hand `APIClient.swift`; 8s/12s timeouts |
| Token | Bearer + force refresh on 401 |
| Local | UserDefaults (onboarding, invite, idempotency, telemetry) |
| SWR bootstrap cache | **MISSING** |
| Client `X-Correlation-Id` | **MISSING** |
| Sentry | **MISSING** (Firebase Analytics + backend telemetry exist) |
| ATS | `NSAllowsArbitraryLoads=true` |
| Offline restore bug | On network failure sets `userId = Firebase uid` |
| Build | **BLOCKED_ENVIRONMENT** on Windows |

---

## 12. Implemented screens (high level)

**Both platforms (approx parity):** Splash, onboarding, login, app shell (contexts, bottom nav, switchers), Personal empty + setup + active Pulse/Moments/Life/Memory + quick adds + expense, Group empty + setup families + join/invite, Business empty + company setup + moment chooser.

**Deferred / incomplete:** Circle CRUD, Life360 populated, Business Memory/active product content, AI Insights, Activity delete, account balances in expense.

---

## 13. Incomplete screens / placeholders

- Circle: deferred / Life360 read placeholder  
- Business Memory: “Coming Soon”  
- AI sections: “Coming Soon”  
- Expense Paid From: Android `AccountPlaceholder` (no fake balances)  

---

## 14. Mock / hard-coded / temporary (production paths)

| Item | Platforms | Notes |
|------|-----------|-------|
| Relationships demo activities | Both | Fallback when API empty |
| Hardcoded bond scores 84/72/91 | Both | Relationships Pulse |
| Group Add People Figma demo contacts | Both | Runtime visible |
| Business empty decorative metrics | Both | Empty-state visuals |
| Media signed URL `storage.momentra.local` | Backend | Temporary |
| FastAPI inference stub | Backend | Temporary |
| Worker stubs | Backend | Temporary |
| Setup catalogs | Both | Static defaults (expected) |

**Test-only fakes:** `FakeMeGateway`, `FakeCreateGateway` — OK.

---

## 15. Duplicate logic / temporary implementations

- `router.ts` vs `router-product.ts`  
- `frds/migrations` vs `frds/phase*` copies  
- Syncthing `*.sync-conflict-*` files in src/tests/docs  
- Hand API clients vs OpenAPI generated (generated secondary)  
- Inline projection bumps in commands vs incomplete projection-worker  
- Stale `docs/implementation/IMPLEMENTATION_STATUS.md` vs live router  

---

## 16. TODO / FIXME

Almost no classic `TODO`/`FIXME` in hand-written app/backend code. Deferred work expressed as “Coming Soon” / “DEFERRED” / `deferred` comments / worker stubs. Generated OpenAPI docs contain codegen `TODO` noise.

---

## 17. Existing tests

| Suite | Status |
|-------|--------|
| Backend `tests/*.test.ts` | platform, runtime-parity, openapi, moment-create, expense, personal-s1, group-invite, unit |
| Android unit | AppShellViewModel, AuthPhase, AuthErrorMapper, ApiErrors, ExpenseMoney, MomentExperience, GroupJoinLink |
| iOS unit | MomentCreateModel, ExpenseMoney, ShellModel (+ scaffold) |
| Performance | Warm Pulse/Activity timings mentioned in release gate; no formal p50/p95 harness |
| iOS device / Xcode | **BLOCKED_ENVIRONMENT** |

---

## 18. Figma references

- **fileKey:** `TzLvwVwlPbeVB8ug1zB3GM`  
- **Root / shell:** node `169:487`  
- Mapping docs: `PHASE_4_FIGMA_SHELL_MAPPING.md`, `PHASE_5_EMPTY_STATE_FIGMA_MAPPING.md`, `S2_GROUP_FIGMA_API_MAP.md`  
- Theme seed: Life Operations / Personal selected `#7C5CFC`; Group `#E8621A`  

---

## 19. Mismatches with V001–V029

| Issue | Detail |
|-------|--------|
| Identity | V002 comment: `user_id = auth.uid()`; runtime uses Firebase → UUIDv5 |
| RLS helpers | `security.current_user_id()` prefers `auth.uid()` / JWT `sub`; Node never sets these |
| Outbox statuses | Workers violate V011 CHECK |
| Forward migrations | V031–V040 already in use; product depends on them |
| V030 | Validation gate; must not run during feature development |

---

## 20. iOS vs Android mismatches

| Area | Gap |
|------|-----|
| Apple Sign-In | iOS only |
| Google UX | GIDSignIn vs Firebase OAuthProvider |
| Timeouts | 8/12s vs 30s |
| Bottom nav | Native TabView vs custom Compose |
| SSE | Neither live |
| Package ID | `com.example.momentra` immature |
| Offline identity | **Both** leak Firebase UID into `userId` |

---

## 21. Security concerns

1. Firebase UID used as Momentra `userId` on offline restore (both platforms)  
2. Outbox worker illegal statuses  
3. RLS not driven by API connection; depends on DB role privilege  
4. `GOVERNANCE_FAIL_OPEN=1` in `.env.example`  
5. SSE `?token=` query string  
6. Android DEBUG body logging / cleartext HTTP / ATS arbitrary loads  
7. Admin static API key  
8. Unused Supabase secret env if present  
9. Syncthing conflict file risk  

---

## 22. Performance concerns

1. Startup fan-out: `/me` + moments + companies + Life360  
2. No SWR bootstrap cache  
3. No Redis  
4. Projection worker does not rebuild  
5. Short iOS timeouts vs Android  
6. OkHttp `runBlocking` in auth interceptor  
7. Continuous analytics tick timers  

---

## 23. S0 readiness (before A–H)

| Gate | Status |
|------|--------|
| Audit document | This file |
| Identity architecture doc | Required next |
| V030 skip in migrate tooling | Required |
| Auth offline userId fix | S0-B |
| Expanded `/v1/me` | S0-C |
| Native SWR | S0-D |
| Networking parity | S0-E |
| Redis | S0-F |
| BullMQ + legal outbox | S0-G |
| Observability + report | S0-H |

**Do not begin S1 until S0 report has no unresolved FAIL in declared scope.**
