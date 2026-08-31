# S2 Personal Screen Matrix

**Date:** 2026-08-26  
**Figma file:** `TzLvwVwlPbeVB8ug1zB3GM`  
**Statuses:** `PASS` | `REUSE` | `REFACTOR` | `MISSING` | `REAL_DATA` | `EMPTY_SUPPORTED` | `API_GAP` | `SCHEMA_GAP` | `FIGMA_GAP` | `THEME_MATRIX_STALE` | `SCREEN_STALE` | `DEFERRED` | `NOT_REQUIRED` | `BLOCKED_ENVIRONMENT`  
**Rule:** No silent scope drop. Life rows are **section-level** (G3). Memory honesty (G4).

---

## Legend

| Column | Meaning |
|--------|---------|
| Figma | Node id from setup catalog / Phase docs / theme matrix (FIGMA_GAP if unverified live) |
| Android | Path under `apk/app/.../com/example/momentra/` |
| iOS | Path under `momentra/momentra/` |
| API | Live = mounted on `router.ts`; Product = `router-product.ts` only |
| SQL | Primary tables / migrations |

---

## Empty experience

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Personal empty host | Phase 5 | `ui/shell/empty/ContextEmptyExperience.kt` | `Shell/MomentEmptyStateView.swift` | bootstrap inventory | — | **REUSE** |
| Pulse empty | Phase 5 / Pulse empty | `empty/personal/PersonalPulseEmptyContent.kt` | `PersonalEmpty/PersonalPulseEmptyView.swift` | none (empty) | — | **REUSE**; decorative preview ≠ metrics |
| Moments empty | Phase 5 | `PersonalMomentsEmptyContent.kt` | `PersonalMomentsEmptyView.swift` | none | — | **REUSE** |
| Memory empty | Phase 5 | `PersonalMemoryEmptyContent.kt` | `PersonalMemoryEmptyView.swift` | none | — | **REUSE** |
| Life empty | Phase 5 | `PersonalLifeEmptyContent.kt` | `PersonalLifeEmptyView.swift` | none | — | **REUSE** / **EMPTY_SUPPORTED** |
| Create chooser | setup chooser | `PersonalCreateEmptyContent.kt` | `PersonalCreateEmptyView.swift` | `GET /personal/setups` optional | `personal.life_system_setup` | **REUSE** |

---

## Setup flows (4 families)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Setup wizard host | — | `PersonalSetupWizardContent.kt` | `PersonalSetupWizardView.swift` | — | — | **REUSE** |
| Catalog defaults | `353:6809` etc | `PersonalSetupCatalog.kt` | `PersonalSetupCatalog.swift` | `GET /personal/setups` | V catalog | **REUSE** |
| Life Operations setup | `353:6809` | `PersonalLifeOpsSetupContent.kt` | `PersonalLifeOpsSetupView.swift` | `POST /v1/moments` + `personalSetup` | `core.moment`, `personal.life_system_setup` | **REUSE** |
| Future Building setup | `353:6905` | `PersonalFutureSetupContent.kt` | `PersonalFutureSetupView.swift` | same | same | **REUSE** |
| Lifestyle setup | `353:7075` | `PersonalLifestyleSetupContent.kt` | `PersonalLifestyleSetupView.swift` | same | same | **REUSE** |
| Relationships setup | `353:7217` | `PersonalRelationshipsSetupContent.kt` | `PersonalRelationshipsSetupView.swift` | same | same | **REUSE** |
| Activate alternate | — | — | — | `POST /personal/setups/:code/activate` (product) | same | **NOT_REQUIRED** (G9) |

---

## Pulse (populated)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Pulse Life Ops | Pulse Life Ops | `personal/PersonalPulseActiveContent.kt` | `PersonalPulseActiveView.swift` | Live `GET /personal/pulse?momentId` | `projection.personal_pulse` | **REUSE** / **REAL_DATA** |
| Pulse Future | Pulse Future | same | same | same | same | **REUSE**; theme **SCREEN_STALE** vs matrix `#10B981` (G8) |
| Pulse Lifestyle | Pulse Lifestyle | same | same | same | same | **REUSE** / **REAL_DATA** |
| Pulse Relationships | Rel Pulse | `PersonalRelationshipsPulseActiveContent.kt` | `PersonalRelationshipsPulseActiveView.swift` | Live pulse + activity | pulse + `projection.recent_activity` | **REFACTOR**; bond scores **API_GAP** (no on-device fake) (G2) |

---

## Moments tab (populated)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Moments Life Ops | Moments Life Ops | `PersonalLifeOpsMomentsActiveContent.kt` | `PersonalLifeOpsMomentsActiveView.swift` | pulse/activity/moments | projection + moment | **REUSE** |
| Moments Future | Moments Future | `PersonalFutureMomentsActiveContent.kt` | `PersonalFutureMomentsActiveView.swift` | same | same | **REUSE** |
| Moments Lifestyle | Moments Lifestyle | — | — | same | same | **MISSING** (G5) |
| Moments Relationships | Moments Rel | — | — | same | same | **MISSING** (G5) |

---

## Life tab — section-level (G3)

Whole-tab PASS is **forbidden** while seed remains. Wire `PersonalLifeActive*` + mount `GET /personal/life` under G1, then reclassify.

| Section | Figma | Android | iOS | API | SQL | Status |
|---------|-------|---------|-----|-----|-----|--------|
| Life tab host (empty) | Life empty | `PersonalLifeEmptyContent.kt` | `PersonalLifeEmptyView.swift` | — | — | **EMPTY_SUPPORTED** |
| Life tab host (active) | Life active `1047:7689` / `1047:7707` (seed cite) | `PersonalLifeActiveContent.kt` **unmounted** | `PersonalLifeActiveView.swift` **unmounted** | Product `GET /personal/life` | `personal.life_system_setup` + seed | **MISSING** wire + **API_GAP** seed |
| score / statusLabel / trend / insight | Life hero | (in Life active) | (in Life active) | product life | seed | **API_GAP** |
| areaScores (4 areas) | Life areas | same | same | product life | seed + ACTIVE count | **API_GAP** |
| drift | Life drift | same | same | product life | seed | **API_GAP** |
| leverage | Life leverage | same | same | product life | seed | **API_GAP** |
| balance | Life balance | same | same | product life | seed | **API_GAP** |
| emotionalTrend / dominantEmotion / happyDrivers | Life emotion | same | same | product life | seed | **API_GAP** |
| journey | Life journey | same | same | product life | seed | **API_GAP** |
| aiInsights | Life AI | same | same | product life | seed | **API_GAP** |

---

## Memory tab (G4)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Memory empty | Memory empty | `PersonalMemoryEmptyContent.kt` | `PersonalMemoryEmptyView.swift` | — | — | **EMPTY_SUPPORTED** |
| Memory Life Ops active | Memory Life Ops | `PersonalLifeOpsMemoryActiveContent.kt` | `PersonalLifeOpsMemoryActiveView.swift` | Often pulse/activity-derived; product `GET /personal/memory` stub | `projection` / stub | **REUSE** UI; data honesty TBD in S2-G |
| Memory Future active | Memory Future | `PersonalFutureMemoryActiveContent.kt` | `PersonalFutureMemoryActiveView.swift` | same | same | **REUSE** UI |
| Memory Lifestyle active | Memory Lifestyle | — | — | same | same | **MISSING** (G5) |
| Memory Relationships active | Memory Rel | — | — | same | same | **MISSING** (G5) |
| Memory list projection | Memory list | — | — | Product `GET /personal/memory` → `{items:[]}` | — | **EMPTY_SUPPORTED** if S2 scope = empty Memory surface; else **API_GAP** (no fabricate) |
| Memory write | — | — | — | Product `POST …/memories` | collaboration | **DEFERRED** |

---

## Activity

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Recent activity (LifeOps-style) | Activity | `PersonalRecentActivityScreen.kt` / flow | `PersonalRecentActivitySheets.swift` | Live `GET /personal/activity?momentId` | `projection.recent_activity` | **REUSE** |
| Relationships activity | Rel Activity | `PersonalRelationshipsActivitySheets.kt` | `PersonalRelationshipsActivitySheets.swift` | Live activity | same | **REFACTOR** — remove `relationshipsDemoActivities` fallback |
| Activity pagination / cursor | — | same | same | keyset `(occurred_at, recent_activity_id)` | same | **REUSE**; Moment-scoped (G7) |

---

## Quick Adds (G6)

| Screen / action | Cap code (V019) | Android | iOS | API | Status |
|-----------------|-----------------|---------|-----|-----|--------|
| Capability → registry | V019 → `PersonalActionRegistry` | **MISSING** | **MISSING** | bootstrap caps | **REFACTOR** (G6) |
| Quick Add hub | family grids | `PersonalQuickAddHub.kt` | `PersonalQuickAddHubView.swift` | caps | **REFACTOR** |
| Expense | `EXPENSE_CREATE` | `PersonalExpenseSheet.kt` | `PersonalExpenseSheet.swift` | Live `POST …/expenses` | **REUSE** (critical) |
| Income / Transfer / Savings chips | `MOVEMENT_RECORD` etc | Expense sheet UI | same | movements product | **DEFERRED** — no fake submit |
| Hub Transfer / Savings / Reflect | — | disabled tiles | disabled | — | **DEFERRED** |
| Life Ops observations | `LIFE_OBSERVATION_RECORD` | `PersonalLifeOpsQuickAddSheets.kt` | `PersonalLifeOpsQuickAddSheets.swift` | Live `POST …/observations` | **REUSE** |
| Future items | Future caps | `PersonalFutureQuickAddSheets.kt` | family sheets | Live `POST …/future-items` | **REUSE** |
| Lifestyle activities | `LIFESTYLE_ACTIVITY_CREATE` | `PersonalFamilyQuickAddSheets.kt` | same | Live `POST …/lifestyle-activities` | **REUSE** |
| Relationships activities | `RELATIONSHIP_ACTIVITY_RECORD` | Relationships QA sheet | same | Product `POST …/relationship-activities` | **MISSING** mount + wire |

---

## Theme / chrome (S1 owned — S2 consumes)

| Item | Figma | Android | iOS | Status |
|------|-------|---------|-----|--------|
| Context PERSONAL accent | `763:12897` | `MomentraShellTheme` | `MomentraShellTheme.swift` | **PASS** (S1) |
| MomentTheme Life Ops | `353:8893` | `#7C5CFC` | same | **PASS** |
| MomentTheme Future | matrix `#10B981` | MomentThemes emerald | same | Matrix vs Pulse hero conflict → G8 |
| Pulse family Future hero | Future Pulse | `PersonalPulseFamily` purple | same | **SCREEN_STALE** or matrix stale after Figma capture |
| MomentTheme Lifestyle | `505:12365` | `#0EA5A4` | same | **PASS** / verify |
| MomentTheme Relationships | `505:11793` | `#E91E63` | same | **PASS** / verify |
| Shell TopBar / BottomNav / switchers | S1 nodes | shell components | shell | **PASS** — do not duplicate Personal chrome |

---

## Backend route matrix (G1)

| Method | Path | Live | Product | S2 action |
|--------|------|------|---------|-----------|
| GET | `/personal/moments` | Y | Y (dup) | Keep live; strip/deprecate product dup |
| GET | `/personal/setups` | Y | Y | Keep live |
| GET | `/personal/pulse` | Y (+momentId) | Y (older) | Keep live; deprecate product |
| GET | `/personal/activity` | Y | Y (arity risk) | Keep live |
| POST | `/v1/moments` | Y | Y | Keep live |
| POST | `/moments/:id/expenses` | Y | Y | Keep live |
| POST | `/moments/:id/observations` | Y | Y | Keep live |
| POST | `/moments/:id/future-items` | Y | Y | Keep live |
| POST | `/moments/:id/lifestyle-activities` | Y | Y | Keep live |
| GET | `/personal/life` | N | Y | **Mount** on live; section-honest |
| GET | `/personal/memory` | N | Y | **Mount** on live; empty-honest |
| GET | `/personal/attention` | N | Y | **DEFERRED** unless needed |
| POST | `/personal/setups/:code/activate` | N | Y | **NOT_REQUIRED** (G9) |
| POST | `/moments/:id/relationship-activities` | N | Y | **Mount** on live |
| POST | `/moments/:id/memories` | N | Y | **DEFERRED** |
| POST | `/moments/:id/movements` | N | Y | **DEFERRED** |

---

## Multi-Moment isolation checklist (G7)

| Concern | Status |
|---------|--------|
| Selection + MomentTheme color | S1 — verify in S2-C/L |
| Quick Add capability set | **MISSING** registry gate |
| Pulse data scoped / correct | Partial (`momentId` spend overlay); verify |
| Activity cursor state | Must be Moment-scoped |
| Cached data | No cross-Moment bleed |
| Form draft state | Must clear/scoped on switch |
| Optimistic updates | Moment-scoped |
| Projection invalidation | Hints → correct Moment slices |

---

## Platform runtime

| Platform | Build/tests | Device | Status |
|----------|-------------|--------|--------|
| Backend | suite | local | Track through S2-L |
| Android | compile + unit | preferred E2E host | Track through S2-L |
| iOS | source-equivalent | Mac/Xcode | **BLOCKED_ENVIRONMENT** on Windows — not FAIL |

---

## S2-A exit criteria

- [x] This matrix exists
- [x] `S2_PERSONAL_AUDIT.md` exists
- [x] No Personal production feature edits before both existed (enforced)
- [x] S2-B→L executed; see `S2_IMPLEMENTATION_REPORT.md` + `S2_PERSONAL_PARITY_MATRIX.md`
- **STOP** before S3
