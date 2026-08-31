# S2 Personal Audit

**Date:** 2026-08-26  
**Scope:** Personal vertical slice before S2-B…L production edits  
**Figma root:** `TzLvwVwlPbeVB8ug1zB3GM` / node `169:487`  
**Theme authority:** [`MOMENTRA_THEME_MATRIX.md`](../design/MOMENTRA_THEME_MATRIX.md)  
**Guardrails:** G1–G10 (dual router, Relationships data, Life section-level, Memory honesty, no blind clone, canonical Quick Add, multi-Moment isolation, Future color from Figma, create = POST moments + personalSetup, critical-path no unresolved API_GAP)  
**Rule:** Prefer REUSE/REFINE/COMPLETE. No Personal production feature edits before this document and `S2_PERSONAL_SCREEN_MATRIX.md` exist.

---

## Executive summary

Personal product UI is largely present on Android (`apk/app/.../ui/shell/`) and iOS (`momentra/momentra/Shell/`). Live backend Personal slice covers create, expense, observations, future-items, lifestyle-activities, pulse, activity, and setups catalog. Gaps that block a complete S2 vertical slice:

| Finding | Classification | Guardrail |
|---------|----------------|-----------|
| `router-product.ts` unmounted; life/memory/relationship-activities only there | **API_GAP** until mount under G1 | G1 |
| Dual semantic fork if both routers stay live | Must deprecate/remove duplicates | G1 |
| Relationships Pulse bond scores hardcoded on clients | **REFACTOR** + verify API fields | G2 |
| Relationships activity falls back to demo rows | **REFACTOR** — remove fake | G2/G10 |
| `GET /personal/life` figma-seeded; route unmounted; UI unmounted | Section-level classify — not whole-tab PASS | G3 |
| `GET /personal/memory` → `{items:[]}`; route unmounted | Honest empty or **API_GAP** | G4 |
| Lifestyle + Relationships Moments/Memory active UIs missing | **MISSING** — reuse structure, Figma content | G5 |
| No shared `PersonalActionRegistry`; hubs hard-code tiles | **REFACTOR** → V019 → registry → form → API | G6 |
| Relationships Quick Add UI-only (no write API wired) | **MISSING** write mount + client | G6/G10 |
| Future Pulse hero purple vs MomentThemes `#10B981` | **THEME_MATRIX_STALE** or **SCREEN_STALE** | G8 |
| Create uses `POST /v1/moments` + `personalSetup` | **PASS** — activate **NOT_REQUIRED** for create | G9 |
| Transfer / Savings / Reflect disabled | **DEFERRED** / **API_GAP** | — |
| `PersonalLifeActive*` exists but not composed | **MISSING** wire | G3 |
| Multi-Moment isolation not gated end-to-end | **MISSING** test gate | G7 |

---

## Backend classification

| Surface | Path / symbol | Status | Notes |
|---------|---------------|--------|-------|
| Live router mount | `api/v1/router.ts` via `app.ts` | **PASS** | Sole runtime router |
| Product router | `api/v1/router-product.ts` | **REFACTOR** (G1) | Not mounted; contains life/memory/activate/relationship-activities/memories + older duplicates |
| `GET /personal/moments` | `projectionService.listPersonalMoments` | **REUSE** | |
| `GET /personal/setups` | `personalSetupService` | **REUSE** | Catalog + user setups |
| `POST /personal/setups/:systemCode/activate` | `activatePersonalSetup` | **NOT_REQUIRED** for create (G9) | Alternate create path; keep unmounted unless separate product action |
| `POST /v1/moments` + `personalSetup` | `momentService.createMoment` | **PASS** | Primary create |
| `PERSONAL_SETUP_CATALOG` | `modules/personal/setup-service.ts` | **REUSE** | 4 families; Figma seeds `353:6809/6905/7075/7217` |
| `GET /personal/pulse` | `projectionService.getPersonalPulse` | **REUSE** | Real `projection.personal_pulse`; `?momentId` overlays spend |
| Relationships pulse bump | `recordRelationshipActivity` | **API_GAP** (G2) | Service writes DB; **no pulse projection bump** |
| `GET /personal/activity` | `projectionService.getPersonalActivity` | **REUSE** | Keyset live |
| Expense create/patch | `financeService` | **REUSE** | Critical path |
| Observations / future-items / lifestyle | `personalService` | **REUSE** | Live |
| `POST …/relationship-activities` | product router only | **MISSING** mount (G1) | Service exists |
| `POST …/memories` | product router only | **DEFERRED** / **API_GAP** | |
| `GET /personal/life` | `getPersonalLife` / `figmaSeededLife` | **API_GAP** sections (G3) | Seeded; only `activeAreaCount` live |
| `GET /personal/memory` | `getPersonalMemory` | **EMPTY_SUPPORTED** or **API_GAP** (G4) | `{items:[]}` |
| `GET /personal/attention` | stub | **DEFERRED** | |
| V019 capabilities | `V019__capability_seed.sql` | **REUSE** | Quick Add codes (see below) |
| OpenAPI marks product routes IMPLEMENTED | `build-openapi.ts` | **REFACTOR** | Align with live mount |

### Pulse field coverage (Relationships — G2)

Live pulse DTO is **user-scoped** (not family-partitioned): `recoveryScore`, `moodState`, `rhythmScore`, `wellbeingScore`, `attentionCount`, `widgetPayload`, spend overlays via `momentId`.

Family writers bump aliases in `widgetPayload` for Future/Lifestyle. **Relationships `recordRelationshipActivity` does not bump pulse.** Clients currently invent Trust/Care/Support/Presence scores → must become **API_GAP** until projection exists, not on-device fake scores.

### Life sections (G3 — provisional)

| Section (seed DTO) | Provisional status |
|--------------------|--------------------|
| score / statusLabel / trendLabel / insight | **API_GAP** (seed) |
| areaScores (LIFE_OPS/FUTURE/LIFESTYLE/RELATIONS) | **API_GAP** (seed); count hint from ACTIVE setups only |
| drift / leverage / balance | **API_GAP** (seed) |
| emotionalTrend / dominantEmotion / happyDrivers | **API_GAP** (seed) |
| journey / aiInsights | **API_GAP** (seed) |
| Empty Life (no Moments) | **EMPTY_SUPPORTED** via empty experience |

### V019 Quick Add–relevant codes (G6)

| Code | Family |
|------|--------|
| `EXPENSE_CREATE` | All personal |
| `LIFE_OBSERVATION_RECORD` | Life Ops |
| `GOAL_CREATE` / `MILESTONE_CREATE` / `PROGRESS_RECORD` | Future |
| `OPPORTUNITY_CREATE` / `PIVOT_RECORD` / `LEARNING_ACTIVITY_CREATE` | Future |
| `LIFESTYLE_ACTIVITY_CREATE` | Lifestyle |
| `RELATIONSHIP_ACTIVITY_RECORD` | Relationships |
| `TASK_CREATE` / `MEMORY_CREATE` | Life Ops (+) |
| `MOVEMENT_RECORD` | Transfer/Savings (backend exists; client deferred) |

---

## Android classification

Root: `apk/app/src/main/java/com/example/momentra/`

| Surface | Path | Status | Notes |
|---------|------|--------|-------|
| Empty Pulse/Moments/Memory/Create | `ui/shell/empty/personal/*` | **REUSE** | Shell-wired |
| Empty Life | `ui/shell/PersonalLifeEmptyContent.kt` | **REUSE** | |
| Create chooser | `PersonalCreateEmptyContent.kt` | **REUSE** | |
| Setup wizard ×4 | `Personal*SetupContent.kt` + catalog | **REUSE** | All call `createPersonalMoment` |
| Pulse LifeOps/Future/Lifestyle | `PersonalPulseActiveContent.kt` | **REUSE** | Real API |
| Pulse Relationships | `PersonalRelationshipsPulseActiveContent.kt` | **REFACTOR** (G2) | Hardcoded bond scores; demo activity |
| Moments LifeOps/Future | `Personal*MomentsActiveContent.kt` | **REUSE** | |
| Moments Lifestyle/Relationships | — | **MISSING** (G5) | Shell stub |
| Memory LifeOps/Future | `Personal*MemoryActiveContent.kt` | **REUSE** structure | Likely pulse/activity-derived; confirm vs Memory API |
| Memory Lifestyle/Relationships | — | **MISSING** (G5) | |
| Life active | `PersonalLifeActiveContent.kt` | **MISSING** wire | Imported, never composed |
| Activity sheets | `PersonalRecentActivity*`, Relationships activity | **REFACTOR** | Remove demo fallback |
| Quick Add hub | `PersonalQuickAddHub.kt` | **REFACTOR** (G6) | Hard-coded family grids |
| Expense sheet | `PersonalExpenseSheet.kt` | **REUSE** | Expense live; Income/Transfer/Savings UI-only |
| LifeOps/Future/Lifestyle QA writes | `Personal*QuickAddSheets.kt` | **REUSE** | |
| Relationships QA | `PersonalRelationshipsQuickAddSheet` | **MISSING** API | UI dismiss only |
| Transfer/Savings/Reflect | hub tiles | **DEFERRED** | `enabled=false` |
| `PersonalPulseFamily` Future hex | purple `#6C4EF2` | **SCREEN_STALE** candidate (G8) | vs MomentThemes `#10B981` |
| `MomentThemes.personal` Future | emerald | Align after Figma node capture | |
| PersonalActionRegistry | — | **MISSING** (G6) | |

---

## iOS classification

Root: `momentra/momentra/`

| Surface | Path | Status | Notes |
|---------|------|--------|-------|
| Empty + Create + setups ×4 | `Shell/PersonalEmpty/*` | **REUSE** | Parity with Android |
| Pulse active (+ Relationships) | `PersonalPulseActiveView.swift`, `PersonalRelationshipsPulseActiveView.swift` | **REUSE** / **REFACTOR** | Same G2 risks if mock scores present |
| Moments/Memory LifeOps+Future | `Personal*Moments/Memory*View.swift` | **REUSE** | |
| Moments/Memory Lifestyle/Relationships | — | **MISSING** (G5) | Placeholder fallthrough |
| Life active | `PersonalLifeActiveView.swift` | **MISSING** wire | |
| Quick Add hub | `PersonalQuickAddHubView.swift` | **REFACTOR** (G6) | Hard-coded; Transfer/Savings/Reflect disabled |
| Expense / family sheets | `PersonalExpenseSheet.swift`, `*QuickAddSheets.swift` | **REUSE** / Relationships **MISSING** write | |
| Theme Future | `PersonalPulseFamily.swift` vs `MomentraShellTheme.swift` | G8 same as Android | |
| Xcode runtime | Windows host | **BLOCKED_ENVIRONMENT** | Source-equivalent required |

---

## Theme / Future color (G8)

| Source | Future primary | Status |
|--------|----------------|--------|
| `MOMENTRA_THEME_MATRIX.md` | `#10B981` / `#34D399` | Documented authority |
| `MomentThemes.personal` (Android/iOS) | `#10B981` | Matches matrix |
| `PersonalPulseFamily.FUTURE_BUILDING` | `#6C4EF2` / `#8B5CF6` | Conflicts with matrix |
| Setup catalog seed | verify Figma `353:6905` | Capture in S2-J |

**S2-J rule:** Inspect Figma node → classify `THEME_MATRIX_STALE` or `SCREEN_STALE` → update **only** the stale side. Do not average.

---

## Dual-router plan (G1)

1. Promote required product handlers onto live `router.ts` only as S2 needs them (`GET life`, `GET memory`, `POST relationship-activities`; activate only if G9 requires — currently **NOT_REQUIRED**).
2. Remove duplicate path definitions from `router-product.ts` **or** mark file header `DEPRECATED / NON-RUNTIME` and strip promoted routes so no endpoint is defined twice.
3. Fix OpenAPI to match live mounts.
4. Never ship two live definitions for the same path.

---

## Critical vs secondary (G10)

**Must clear by S2 end (no unresolved critical API_GAP):**

- Expense create
- Moment create (`POST /moments` + personalSetup)
- Moment switch + isolation (G7)
- Pulse (honest data; Relationships missing fields = documented API_GAP only if UI does not fake scores)
- Activity (no demo fallback)

**May remain classified if honest:**

- Life seeded sections → per-section `API_GAP` / `DEFERRED`
- Memory empty stub → `EMPTY_SUPPORTED` if product accepts empty Memory surface for S2
- Transfer / Savings / Reflect / movements
- Memory write / attention

---

## Explicit E2E acceptance journey (S2-L)

```text
Fresh Personal user
→ Empty
→ Create Life Operations Moment
→ Setup submit
→ selected automatically
→ Pulse loads
→ Quick Add expense
→ Pulse/Activity reflect it
→ create Future Building Moment
→ switch Moment
→ correct theme/data/capabilities
→ switch back
→ original state intact
→ Life
→ Memory
→ Activity pagination
→ logout/login
→ cached/bootstrap state restored correctly
```

Android runnable; iOS source-equivalent until Mac/Xcode.

---

## Non-goals (reconfirmed)

S3–S9, V030, inventing Life/Memory algorithms, fake submissions, Personal chrome duplicating S1 shell, blind pixel clones, dual live routes.
