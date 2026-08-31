# Phase 6 — Figma Parity Checklist

Per-node visual parity evidence for the 10 Figma setup wizard nodes (Android + iOS).

## Shared primitives

| Primitive | Android | iOS | Status |
|-----------|---------|-----|--------|
| SetupWizardScaffold | `ui/setup/SetupWizardScaffold.kt` | `Shell/Setup/SetupWizardScaffold.swift` | PASS |
| SetupWizardHeader | `ui/setup/SetupWizardHeader.kt` | `Shell/Setup/SetupWizardHeader.swift` | PASS |
| SetupStickyFooter | `ui/setup/SetupStickyFooter.kt` | `Shell/Setup/SetupStickyFooter.swift` | PASS |
| SetupChipGrid | `ui/setup/SetupChipGrid.kt` | `Shell/Setup/SetupChipGrid.swift` | PASS |
| SetupPreviewCard | `ui/setup/SetupPreviewCard.kt` | `Shell/Setup/SetupPreviewCard.swift` | PASS |
| GroupSetupStepper | `group/GroupSetupComponents.kt` | `Shell/Setup/GroupSetupStepper.swift` | PASS |
| SetupPreferenceFilter | `ui/setup/SetupPreferenceFilter.kt` | `Shell/Setup/SetupPreferenceFilter.swift` | PASS |

## Node parity

| Node | Screen | Android | iOS | Visual parity | Create flow |
|------|--------|---------|-----|---------------|-------------|
| 353:6809 | Life Operations | `PersonalLifeOpsSetupContent` | `PersonalLifeOpsSetupView` | FAIL — full Figma fields restored; awaiting screenshot compare | `POST /v1/moments` + expanded prefs |
| 353:6905 | Future Building | `PersonalFutureSetupContent` | `PersonalFutureSetupView` | FAIL — full Figma fields restored; awaiting screenshot compare | same |
| 353:7075 | Lifestyle | `PersonalLifestyleSetupContent` | `PersonalLifestyleSetupView` | FAIL — full Figma fields restored; awaiting screenshot compare | same |
| 353:7217 | Relationships | `PersonalRelationshipsSetupContent` | `PersonalRelationshipsSetupView` | FAIL — full Figma fields restored; awaiting screenshot compare | same |
| 575:9917 | Group Experience | `GroupExperienceSetupContent` | `GroupExperienceSetupView` | PASS — dynamic stepper, type cards, Member role, review CTA | same |
| 575:9919 | Group Purchase | `GroupPurchaseSetupContent` | `GroupPurchaseSetupView` | PASS — 4-step scroll, local budget note | same |
| 575:10567 | Group Living | `GroupLivingSetupContent` | `GroupLivingSetupView` | PASS — 4-step scroll, household budget local | same |
| 692:34736 | Team Operations | `BusinessSetupWizardContent` | `BusinessSetupWizardView` | PASS — sectioned form, sticky footer | same + `companyId` |
| 692:36690 | Business Runway | same | same | PASS — cash/revenue sections, runway preview | same |
| 692:37188 | Business Operations | same | same | PASS — budget/approval fields, monitoring preview | same |

## Budget / API-gap policy

| Area | UI | API payload |
|------|-----|-------------|
| Group Purchase budget | Interactive text field + inline note | **Excluded** (local device only) |
| Group Living household budget | Interactive text field + inline note | **Excluded** |
| Business budget fields | Editable in wizard | **Included** when key exists in `BUSINESS_SETUP_CATALOG.defaultPreferences` |
| Personal preview / intelligence | Client-derived cards | **Excluded** (`profile` sent as catalog default only) |

Note shown on group budget fields: *"Saved on this device — syncs when budget API ships"*

## Build verification

| Platform | Command | Result |
|----------|---------|--------|
| Android | `./gradlew :app:assembleDebug` | PASS (2026-08-22) |
| iOS | Xcode scheme on macOS | Pending macOS verification |

## Deviations logged

- Company onboarding (695:4455) — out of scope
- Business Memory chooser tab — out of scope
- Moment Switcher hidden on setup screens per shell rules — matches Figma
