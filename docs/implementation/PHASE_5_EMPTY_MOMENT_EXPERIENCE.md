# Phase 5 — Empty & Inactive Moment Experience

## Scope

Phases 0–4 complete. This phase delivers the experience when the user has **no currently active Moment**, without implementing Moment.Create.

Platforms: Android (Kotlin/Compose), iOS (Swift/SwiftUI). Data from existing `/v1` list reads only. **Runtime mock data = 0.**

## State resolver

Presentation enum `MomentExperienceKind` (Android + iOS):

| Kind | Meaning |
|------|---------|
| `LOADING` | Request in flight |
| `ERROR` | Network/API failure (never shown as empty) |
| `FIRST_MOMENT` | List succeeded with zero Moments |
| `ACTIVE` | ≥1 `ACTIVE` or `DRAFT` |
| `BETWEEN_MOMENTS` | History exists, none active |
| `PAUSED_ONLY` | Only `PAUSED` (backend may not emit yet) |

Derived from real list payloads via `resolveMomentExperience`. Not a backend state machine.

`ACTIVE`/`DRAFT` count as active. Recent history uses up to 5 non-active, non-`ARCHIVED` items.

## New-user behavior

`FIRST_MOMENT` → context-specific empty copy + CTA routing to Create tab / top-bar `+`. No auto-launch wizard. No fabricated Moment cards.

## Returning-user behavior

`BETWEEN_MOMENTS` / `PAUSED_ONLY` → calm “between moments” copy + real recent history rows when present.

## Personal

| Tab | First | Between |
|-----|-------|---------|
| Pulse | Moments will come alive here | All quiet + history |
| Moments | Make space for what matters | Between moments + history |
| Create | Shell entry only (no wizard) | same |
| Life | Existing educational Life empty | Story continues + history |
| Memory | Nothing to look back on yet | Memories wait quietly + history |

Moment Switcher never shown on Personal.

## Group

Distinct together/shared copy. History: “Recent moments together”. Switcher only when `activeMomentCount > 0`.

## Business

Hierarchy: Company → Moment.

- No company → Phase 4 setup empty (no Create Business Moment CTA).
- Company + no active → Business Moment empty (+ history when present).
- Company selector remains.

## Moment Switcher rules

Hidden when `activeMomentCount == 0`, while loading, or on Personal/Circle. Never “Select Moment ▼” with nothing to switch.

## History

Source: `GET /v1/{personal\|group\|business}/moments?limit=20`, display ≤5 recent non-active non-archived. No fake rows.

## CTA routing

| CTA | Route | API | Status |
|-----|-------|-----|--------|
| Create / Start Moment (all contexts) | Bottom Create tab + top-bar `+` | — | Shell entry only |
| Full Moment.Create wizard | — | create command | **API_GAP** / deferred Phase 6+ |
| Resume paused | — | resume command | **API_GAP** (no resume endpoint) |
| See how Moments work | — | — | Deferred (no destination) |
| View all past moments | — | pagination | Deferred |

## Loading / error / 403

| State | UI |
|-------|-----|
| Pending | Spinner — never flash empty |
| Success empty / between | Empty experience |
| Network/API error | “We couldn't load your moments” + Retry |
| 403 | Phase 4 Forbidden — not empty |

## Android

- `domain/MomentExperience.kt`
- `MeGateway` list methods
- `AppShellViewModel` loads moments per context
- `ui/shell/empty/MomentEmptyState.kt` + `ContextEmptyExperience.kt`
- Unit tests: `MomentExperienceTest`, `AppShellViewModelTest`

## iOS

- `Shell/MomentExperience.swift`
- `APIClient` list helpers + `ShellMeGatewaying`
- `AppShellModel` mirrors Android resolver
- `MomentEmptyStateView` + `ContextEmptyExperienceView`
- Tests in `ShellModelTests.swift` (require Xcode/macOS)

## Tests

Android: **25/25** unit tests passed on this host.

iOS: implemented; **not executed** on Windows (no Xcode).

## Figma parity

See `PHASE_5_EMPTY_STATE_FIGMA_MAPPING.md`. Personal Pulse / Moments / Create / Life / Memory empty chapters are implemented from Figma (layout + committed assets). Group/Business remain calm empty + history (full marketing chapters deferred).

## Gaps

**API_GAP**

- Full Moment.Create wizard / expense / check-in log APIs (Create Quick Start buttons are visual-only)
- Resume paused Moment command
- Dedicated “view all past” screen
- PAUSED status may be absent from DB today

**ASSET_GAP / VISUAL_ASSET_GAP**

- Personal empties (Pulse, Moments, Create, Life, Memory): Figma exports committed; no open VISUAL_ASSET_GAP for those five frames
- Group/Business chapter hero illustrations still largely typography + chrome (deferred)

## Files changed (primary)

Android: `MomentExperience.kt`, `MeRepository.kt`, `AppShellViewModel.kt`, `AppShellScreen.kt`, `empty/personal/*`, `empty/ContextEmptyExperience.kt`, tests.

iOS: `MomentExperience.swift`, `MomentEmptyStateView.swift`, `Shell/PersonalEmpty/*`, `AppShellModel.swift`, `AppShellView.swift`, `ShellMeGateway.swift`, `APIClient.swift`, `ShellModelTests.swift`.

Docs: Phase 5 reports under `docs/implementation/`.
