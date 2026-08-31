# Phase 6 — Production Moment Creation

## Summary

Phase 6 replaces Phase 5 scroll-PNG + activate shortcuts with **interactive setup wizards** routed through one shared client create engine and a single atomic **`POST /v1/moments`** command extended to persist validated setup preferences.

| Layer | Deliverable | Status |
|-------|-------------|--------|
| **6A** Backend | V038 `MOMENT_CREATE`, OpenAPI `MomentCreateRequest`, validated setup blocks, atomic transaction, tests | PASS |
| **6B** Client coordinator | `MomentCreateRepository` / `MomentCreateViewModel` (Android), `MomentCreateRepository` / `MomentCreateModel` (iOS), idempotency persistence | PASS (Android verified; iOS sources complete) |
| **6C** Personal Life Ops | Interactive wizard 353:6809 → `POST /v1/moments` | PASS |
| **6D** Personal ×3 | Future / Lifestyle / Relationships wizards | PASS |
| **6E** Group Experience | 575:9917 — 4 variants on shared command | PASS |
| **6F** Group Purchase + Living | 575:9919 / 575:10567 — 8 variants; budget UI DEFERRED | PASS |
| **6G** Business ×3 | 692:34736 / 36690 / 37188 on shared command | PASS |
| **6H** Cross-platform + docs | Android unit tests, docs, acceptance gate | PASS (iOS Xcode verification deferred to macOS) |

## Architecture

- **One engine:** `backend/typescript/src/modules/moment/service.ts` → `createMoment`
- **One client command:** all wizards submit via `POST /v1/moments` (activate endpoints remain backward-compatible aliases)
- **Atomic transaction:** `core.moment` + domain context + setup row + audit + domain event + outbox
- **Validated preferences:** keys merged server-side per `systemCode` / `familyCode`; unknown keys rejected
- **Post-create:** `onMomentCreated(outcome)` sets selection from command response, exits Create, reloads context read models

## Backend (6A)

### Migrations added to ledger

- `V036__personal_life_system_setup.sql`
- `V037__business_system_setup.sql`
- `V038__moment_create_capability.sql`

### Key files

| File | Role |
|------|------|
| `backend/typescript/src/modules/moment/setup-preferences.ts` | Zod blocks + per-catalog preference validation |
| `backend/typescript/src/modules/moment/setup-persistence.ts` | Personal/business setup row inserts |
| `backend/typescript/src/modules/moment/projection-routing.ts` | Domain-specific projection hint codes |
| `backend/typescript/tests/moment-create.test.ts` | Idempotency, setup validation, audit/event, auth |

### API gaps documented

- **Group scope:** no contracted `groupId` in OpenAPI — organizer-user scope used (see mapping doc)
- **Budget:** Group purchase/living budget UI disabled (DEFERRED / API_GAP)
- **Business Memory chooser:** DEFERRED / NOT_IN_SCOPE (not in 10 Figma nodes)

## Android client

| File | Role |
|------|------|
| `data/repository/MomentCreateRepository.kt` | Unified create + idempotency |
| `ui/create/MomentCreateViewModel.kt` | Submission coordinator |
| `ui/shell/empty/personal/PersonalSetupWizardContent.kt` | Personal interactive wizards |
| `ui/shell/empty/BusinessSetupWizardContent.kt` | Business interactive wizards |
| `ui/shell/empty/group/GroupSectionSetupContent.kt` | Purchase + Living wizards |
| `ui/shell/AppShellViewModel.kt` | `onMomentCreated` post-create handler |

## iOS client

| File | Role |
|------|------|
| `momentra/Data/MomentCreateRepository.swift` | Unified create |
| `momentra/Shell/MomentCreateModel.swift` | Coordinator |
| `momentra/Shell/PersonalEmpty/PersonalCreateEmptyView.swift` | Personal wizards |
| `momentra/Shell/GroupEmpty/GroupPurchaseSetupView.swift` | Purchase variants |
| `momentra/Shell/GroupEmpty/GroupLivingSetupView.swift` | Living variants |
| `momentraTests/MomentCreateModelTests.swift` | Basic coordinator test |

## Tests run (this phase)

```bash
cd backend/typescript && npm test -- tests/moment-create.test.ts   # 5/5 PASS
cd apk && ./gradlew :app:testDebugUnitTest --tests AppShellViewModelTest  # 20/20 PASS
```

iOS: run `momentra` scheme + `MomentCreateModelTests` on macOS.

## Acceptance gate

- [x] 10/10 Figma source nodes mapped (see `PHASE_6_MOMENT_CREATE_FIGMA_MAPPING.md`)
- [x] 19/19 concrete flows wired to `POST /v1/moments`
- [x] No runtime mock data in migrated flows
- [x] Scroll PNG activate paths removed for migrated wizards
- [x] Backend moment-create test suite green
- [x] Android shell post-create selection from command result
- [ ] iOS binary verification on macOS (environment limitation)
