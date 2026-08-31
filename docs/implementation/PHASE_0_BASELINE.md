# Phase 0 — Repository & Architecture Baseline

**Date:** 2026-08-20  
**Scope:** Inspect existing repository, establish development baseline, make only safe structural fixes. No product features.

---

## 1. Discovered Current State

### 1.1 Repository layout

```
g:\momentra_v2\
├── .cursor/                    # Cursor plans
├── .firebaserc                 # Firebase project: momentra-v2
├── .gitignore
├── apk/                        # Android (Kotlin + Jetpack Compose)
├── backend/
│   ├── .env, .env.example
│   ├── deploy/dokploy/docker-compose.yml
│   ├── packages/event-contracts/
│   ├── python/
│   │   ├── ai-context/context_builder.py
│   │   └── fastapi-ai/         # FastAPI stub
│   ├── typescript/             # Node sync control plane
│   └── workers/                # 6 standalone workers
├── docs/
│   ├── mapping/                # Figma ↔ backend ↔ APK mapping (Freeze v4)
│   └── implementation/         # This document
├── firebase.json, firestore.rules, firestore.indexes.json
├── frds/
│   ├── migrations/             # Canonical SQL V001–V034
│   ├── manifest/
│   └── phase1–phase6/        # Historical phase copies (V001–V030 only)
└── momentra/                   # iOS (Swift + SwiftUI)
```

**Git:** Not initialized at repository root (`fatal: not a git repository`). Version control baseline is unresolved.

---

### 1.2 Backend architecture

| Area | Path | Status |
|------|------|--------|
| API entry | `backend/typescript/src/index.ts` → `app.ts` | Express 5, mounts `/v1`, `/health/*`, SSE |
| Domain modules | `backend/typescript/src/modules/` | 11 modules: ai, business, collaboration, device, finance, governance, media, moment, personal, projection, work |
| Platform layer | `backend/typescript/src/platform/` | auth, config, database pool, errors, events/outbox, idempotency, observability, request-context, transaction |
| API layer | `backend/typescript/src/api/` | `middleware/` (auth, correlation), `v1/router.ts` (monolithic ~713 lines), `routes/` **empty** |
| Realtime | `backend/typescript/src/realtime/sse.ts` | SSE invalidation channel |
| Workers (runtime) | `backend/workers/*/` | outbox-dispatcher, projection-worker, notification-worker, scheduler, analytics-worker, memory-worker |
| Workers (placeholder) | `backend/typescript/src/workers/` | **Empty directory** |
| Python AI | `backend/python/fastapi-ai/main.py` | Stub: `/health/live`, `/v1/inference` |
| Event contracts | `backend/packages/event-contracts/` | Shared TS event types |
| Deployment | `backend/deploy/dokploy/docker-compose.yml` | API + 4 workers + FastAPI |

**Database access:** Raw `pg` pool (`platform/database/pool.ts`). Prisma schema exists (`prisma/schema.prisma`) for client generator only; **migrations are not Prisma Migrate** — applied via `scripts/migrate.ts` from `frds/migrations/`.

**Auth:** Firebase Admin JWT verification in `api/middleware/auth.ts`; dev bypass via `ALLOW_DEV_AUTH` / `X-Dev-Firebase-Uid`.

---

### 1.3 Backend entrypoints and dependencies

**Package:** `momentra-api` @ `backend/typescript/package.json`

| Script | Command |
|--------|---------|
| `dev` | `tsx watch src/index.ts` |
| `build` | `tsc` → `dist/index.js` |
| `start` | `node dist/index.js` |
| `test` | `tsx --test tests/**/*.test.ts` |
| `migrate:*` | Custom SQL migration runner |
| `worker:*` | Run each worker via tsx |
| `openapi:generate-kotlin` | PowerShell codegen for Android |

**Runtime deps:** express, pg, firebase-admin, zod, decimal.js, bcrypt, cors, uuid, ws, @prisma/client  
**Python deps:** fastapi, uvicorn (`backend/python/fastapi-ai/requirements.txt`)

---

### 1.4 Android project status

| Item | Value |
|------|-------|
| Module | Single `:app` |
| Package | `com.example.momentra` |
| SDK | min 24, compile/target 36 |
| UI | Compose + Material3 |
| Flow | Splash → Onboarding → Login → Home (placeholder) |
| Firebase | `google-services.json` present; project `momentra-v2` |
| Auth | Email, phone, Google (Credential Manager) |
| API layer | Retrofit `ApiService.kt` + hand-maintained DTOs aligned to OpenAPI |
| Realtime | `SseClient.kt` scaffolded |
| Build | **SUCCESS** after Phase 0 fix (see §3) |

---

### 1.5 iOS project status

| Item | Value |
|------|-------|
| Project | `momentra/momentra.xcodeproj` |
| Bundle ID | `resolvingpoint.momentra` |
| Entry | `momentraApp.swift` (@main) |
| Flow | Splash → Onboarding → Login → `HomePlaceholderView` |
| Firebase | SPM Firebase 12.18.0 + GoogleSignIn 8.0.0; `GoogleService-Info.plist` present |
| API | `API/APIClient.swift` — bootstrap `/v1/me` with Firebase ID token |
| Tests | Unit + UI test targets present |
| Build | **Not verified** — host is Windows; Xcode build requires macOS |
| Structure | Valid: 3 targets, SPM resolved, entitlements for Apple Sign-In |

**Note:** `ContentView.swift` is unused Xcode boilerplate.

---

### 1.6 Firebase integration status

| Platform | Config | Services wired |
|----------|--------|----------------|
| Root | `.firebaserc`, `firebase.json`, `firestore.rules` | Auth providers (email, Google), Firestore rules |
| Backend | `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` | firebase-admin JWT verify |
| Android | `apk/app/google-services.json` | Auth, Analytics, Firestore dep (unused in UI) |
| iOS | `momentra/GoogleService-Info.plist` | Auth, Analytics, Google Sign-In, APNS for phone auth |

**Project ID:** `momentra-v2` (consistent across platforms).

**Gap:** Android package `com.example.momentra` vs iOS bundle `resolvingpoint.momentra` — different application IDs under same Firebase project (intentional for now, but not production-final naming).

---

### 1.7 SQL migrations (V001–V034)

**Canonical location:** `frds/migrations/` — **all 34 files present**.

| Range | Files | Purpose |
|-------|-------|---------|
| V001–V017 | Schema DDL | Extensions, core, domains, events, projection, indexes |
| V018–V023 | Seeds | Taxonomy, capability wire codes, governance, consent, analytics, policy |
| V024–V029 | RLS + grants | Row-level security per domain |
| V030 | Validation | Production validation gate |
| V031–V034 | Forward pack | Company location labels, poll kernel, forward RLS/grants, forward validation |

**Migration runner:** `backend/typescript/scripts/migrate.ts` reads `frds/migrations/` per `frds/manifest/MIGRATION_ORDER.txt`.

**Manifest drift:** `frds/manifest/manifest.json` tracks V001–V030 only (status `PRE_RC_BLOCKED`); V031–V034 not in manifest but present on disk and supported by migrate script forward mode.

**Phase copies:** `frds/phase1`–`phase6` duplicate V001–V030; V031–V034 exist only in canonical `migrations/`.

---

### 1.8 OpenAPI / API contract status

| File | Version | Role |
|------|---------|------|
| `backend/typescript/openapi/openapi.yaml` | 3.1.0 | **Authoritative** full `/v1` contract (Mapping Freeze v4) |
| `backend/typescript/openapi/v1.yaml` | 3.0.3 | Partial/stub subset |

**Implementation:** Routes implemented in monolithic `backend/typescript/src/api/v1/router.ts`.

**Mobile clients:**
- Android: Hand-maintained Retrofit in `apk/.../data/api/` (not yet codegen-driven in CI)
- iOS: Minimal `APIClient.swift` (bootstrap `/v1/me` only)
- Codegen script exists: `scripts/generate-kotlin.ps1` → not run in Phase 0

**Envelope pattern:** CommandEnvelope / ProjectionEnvelope / ErrorEnvelope — consistent across OpenAPI, backend, and Android DTOs.

---

### 1.9 Environment variable requirements

Loaded from `backend/typescript/.env` and `backend/.env` (latter wins on conflict).

| Variable | Required | Purpose |
|----------|----------|---------|
| `DATABASE_URL` | **Yes** | PostgreSQL pooler (Supabase) |
| `DATABASE_URL_DIRECT` | No | Direct 5432 for migrations (falls back to `DATABASE_URL`) |
| `PORT` | No | API port (default 3000) |
| `NODE_ENV` | No | Environment label |
| `FIREBASE_PROJECT_ID` | No* | Firebase project (*required for prod auth) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | No* | Service account JSON string |
| `MOMENTRA_UUID_NAMESPACE` / `MOMENTRA_IDENTITY_NAMESPACE` | No | UUIDv5 identity namespace |
| `CORS_ORIGINS` | No | Comma-separated origins |
| `SCHEMA_RELEASE` | No | Schema version label (default `V001-V029`; examples updated to `V001-V034`) |
| `ALLOW_DEV_AUTH` | No | Dev auth bypass |
| `GOVERNANCE_FAIL_OPEN` | No | Governance resolver behavior |
| `DB_POOL_MAX`, `DB_POOL_IDLE_MS`, `DB_STATEMENT_TIMEOUT_MS` | No | Pool tuning |
| `MEDIA_BUCKET` | No | GCS bucket for media uploads |
| `FCM_SERVER_KEY` | No | Push notifications (worker) |
| `OUTBOX_POLL_MS`, `OUTBOX_BATCH_SIZE` | No | Outbox worker tuning |
| `PROJECTION_POLL_MS`, `PROJECTION_BATCH_SIZE` | No | Projection worker tuning |
| `NOTIFICATION_POLL_MS` | No | Notification worker tuning |
| `SCHEDULER_INTERVAL_MS` | No | Scheduler tuning |
| `REPAIR_LEGACY_SCHEMAS` | No | Migration repair flag |

**Android (`local.properties`):** `API_BASE_URL`, `GOOGLE_WEB_CLIENT_ID`  
**iOS:** `MOMENTRA_API_BASE_URL` env override in scheme

---

## 2. Gap Analysis vs Target Architecture

| Rule / Target | Current state | Gap severity |
|---------------|---------------|--------------|
| PostgreSQL canonical source of truth | Migrations V001–V034 in `frds/migrations/`; pg pool in backend | Low — aligned |
| Node owns sync commands + auth orchestration | Express `/v1` router + modules + platform | Low — structure exists |
| FastAPI for async AI/analytics | Stub only at `python/fastapi-ai/` | Medium — no LLM, no worker integration |
| Projection tables for UI reads | `modules/projection/service.ts` + projection-worker | Low — scaffolded |
| OpenAPI `/v1` transport contract | Full spec exists; router monolithic | Low–Medium — no route-per-file split |
| Figma owns UI labels | `docs/mapping/` exists | Low — mapping docs present |
| V018/V019 wire codes | Seed migrations present | Low — not verified applied to DB |
| Work owns Goal/Milestone/Task | `modules/work/service.ts` | Not verified — Phase 1+ |
| Finance owns Expense/Movement kernels | `modules/finance/` | Not verified — Phase 1+ |
| Realtime/FCM invalidates; clients refetch | SSE + notification-worker scaffold | Medium — end-to-end not verified |
| Never trust client userId/companyId | Auth middleware derives context | Low — pattern in place |
| No Prisma Migrate | Custom migrate.ts; Prisma schema minimal | Low — aligned (Prisma present but not used for migrate) |
| No external I/O in DB transactions | Not audited in Phase 0 | Unknown — Phase 1 audit |
| `backend/typescript/modules/platform/api` layout | Present; `api/routes/` and `src/workers/` empty | Low — placeholders |
| `backend/workers/` separate from typescript | Workers at sibling path importing platform code | Low — intentional split |
| Git version control | **No git repo** | **High blocker** |
| Mobile production package IDs | `com.example.momentra` / `resolvingpoint.momentra` | Medium — pre-production naming |
| iOS OpenAPI client parity | Bootstrap only | Medium — Android ahead |
| Dokploy Docker CMD | Was wrong path | **Fixed in Phase 0** |
| manifest.json vs V034 | Manifest stops at V030 | Low — update in Phase 1 |
| Root README | Missing | Low |

---

## 3. Changes Made (Phase 0 only)

| File | Change | Rationale |
|------|--------|-----------|
| `backend/typescript/Dockerfile` | `CMD` → `node dist/index.js` | `tsconfig rootDir: src` outputs to `dist/index.js`, not `dist/src/index.js`; fixes Dokploy container start |
| `apk/.../AuthViewModel.kt` | Unwrap `SuccessEnvelope`: `envelope.data.displayName/email` | Kotlin compile failure — API returns envelope, not bare DTO |
| `backend/.env.example` | `SCHEMA_RELEASE=V001-V034` | Align env template with available migrations |
| `backend/typescript/.env.example` | `SCHEMA_RELEASE=V001-V034` | Same |
| `docs/implementation/PHASE_0_BASELINE.md` | Created | This document |

**No deletions. No product feature code. No mobile rewrites.**

---

## 4. Commands Run

```powershell
# Backend
Set-Location g:\momentra_v2\backend\typescript
npm install          # up to date, 439 packages
npm run build        # tsc — SUCCESS
npm test             # 4 tests, 0 failures

# Android
Set-Location g:\momentra_v2\apk
.\gradlew.bat assembleDebug --no-daemon   # FAILED (pre-fix)
.\gradlew.bat assembleDebug --no-daemon   # SUCCESS (post-fix)

# Git (attempted)
Set-Location g:\momentra_v2
git status           # not a git repository
```

**Not run (environment limitation):**
- iOS `xcodebuild` — requires macOS
- Database migration apply — requires live Supabase credentials
- FastAPI/Python server start — stub only; deferred
- Docker image build — deferred

---

## 5. Test / Build Results

| Target | Result | Notes |
|--------|--------|-------|
| Backend `npm run build` | **PASS** | TypeScript compiles cleanly |
| Backend `npm test` | **PASS** | 4 unit tests (parseMoney, hashRequest) |
| Android `assembleDebug` | **PASS** | After AuthViewModel envelope fix; deprecation warnings only |
| iOS structure | **VALID (unbuilt)** | Xcode project + SPM + Firebase wired; build not executed on Windows |
| Git diff review | **N/A** | No git repository initialized |

---

## 6. Unresolved Blockers

1. **No git repository** — Cannot track changes, create PRs, or enforce review workflow until `git init` / remote setup.
2. **Database not verified** — `DATABASE_URL` in `.env` not tested; migration apply status unknown on target Supabase instance.
3. **iOS build unverified** — Requires macOS CI or local Xcode to confirm compile.
4. **Firebase service account** — Backend prod auth needs `FIREBASE_SERVICE_ACCOUNT_JSON` populated.
5. **Package ID alignment** — Android `com.example.momentra` is placeholder; iOS uses `resolvingpoint.momentra`.
6. **manifest.json stale** — Does not include V031–V034 metadata.
7. **OpenAPI ↔ router parity** — Not audited route-by-route in Phase 0.
8. **Prisma presence** — Schema file exists; ensure Phase 1 does not introduce Prisma Migrate (architecture rule #12).

---

## 7. Proposed Minimal Changes for Phase 1+ (not executed)

- Initialize git + `.gitignore` at repo root (if not managed externally)
- Run `migrate:forward` against Supabase staging; confirm V034 validation passes
- Add macOS CI job for iOS `xcodebuild`
- Update `frds/manifest/manifest.json` to V034
- Split `router.ts` into `api/routes/` modules as endpoints stabilize
- Run `openapi:generate-kotlin` and decide codegen vs hand-maintained DTO policy
- Rename Android applicationId when production package finalized
- Expand iOS `APIClient` to OpenAPI parity
- Wire FastAPI to projection read-only context builder

---

## 8. Architecture Alignment Summary

The repository **already matches the target multi-runtime layout** for Phase 0:

```
backend/typescript/src/
├── api/          ✓ middleware + v1 router
├── modules/      ✓ 11 domain modules
├── platform/     ✓ cross-cutting infra
└── realtime/     ✓ SSE

backend/workers/  ✓ 6 workers (sibling to typescript)
backend/python/   ✓ FastAPI stub
frds/migrations/  ✓ V001–V034 complete
apk/              ✓ Compose app with auth shell + API scaffold
momentra/         ✓ SwiftUI app with auth shell + bootstrap API
```

Phase 0 establishes that the baseline is **production-oriented scaffolding** (real modules, real migrations, real OpenAPI, real Firebase) — not a prototype with mock data. Product domains (Moments, Expenses, Group, Business, AI) remain unimplemented at the UI layer and are deferred to Phase 1+.

**Phase 0 complete. Do not begin Phase 1.**
