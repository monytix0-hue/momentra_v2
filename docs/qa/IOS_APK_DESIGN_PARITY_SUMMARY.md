# iOS vs APK Design Parity Summary

**Date:** 2026-09-01  
**Authority:** APK UI (Jetpack Compose) — near-perfect reference  
**Matrix:** [`IOS_APK_DESIGN_PARITY_MATRIX.csv`](IOS_APK_DESIGN_PARITY_MATRIX.csv) (78 rows)  
**Token diff:** [`IOS_APK_THEME_TOKEN_DIFF.md`](IOS_APK_THEME_TOKEN_DIFF.md)  
**Regenerator:** `npx tsx scripts/qa/build-ios-apk-parity-matrix.ts`

## Executive counts

| Overall | Count |
|---------|-------|
| MATCH | 22 |
| PARTIAL | 55 |
| GAP | 1 |
| MISSING_IOS | 0 |

Scores are 0–2 per dimension (layout, colors, typography, spacing, icons, empty_state, hero, animation) from source comparison. Screenshot confirmation pending — see [`screenshots/ios-apk-parity/README.md`](screenshots/ios-apk-parity/README.md).

## Device verification (2026-09-01)

Verified on USB-connected physical devices:

| Platform | Repo | ID | Status |
|----------|------|-----|--------|
| Android | [`apk/`](../../apk/) | `com.example.momentra` | Installed + launched; bottom nav FAB matches APK reference |
| iOS | [`momentra/`](../../momentra/) | `resolvingpoint.momentra` | Built, installed, launched — **not** `MagnatePoint.Momentra` |

Screenshot paths in the matrix are optional manual capture targets — **not** Maestro test flows.

## P0 remediation (completed)
|------|-----|--------------|-------------|
| Bottom navigation | `ShellBottomNavigation.kt` — custom bar + 36dp accent FAB | `TabView` + SF Symbol `plus.circle.fill` | `ShellBottomNavigationView` in `ShellChrome.swift`; `AppShellView` uses custom bar |
| Create tab asset | `ic_shell_plus` in accent circle | Ignored `NavCreate.imageset` | `ShellPlus` in accent FAB (no label under FAB, matches APK) |

## P1 remediation (2026-09-01 — in progress)

| Screen | Change |
|--------|--------|
| Personal Create empty | Plus Jakarta typography, centered hero, `PersonalHistorySection`, token-aligned pink |
| Group Pulse generic | APK hero pills, trip quick tiles, settlement badge + orange balance card, participation fallback, activity glyphs, balance masking |
| Personal / Group Quick Add | `PersonalQaIcons` vector chrome + tile icons (replaces SF Symbols on personal hub) |
| Personal Pulse / Moments empty | Plus Jakarta typography throughout |
| Personal Life active | Plus Jakarta typography throughout |

## P1 gaps (remaining backlog)

Highest-impact PARTIAL/GAP screens from matrix:

| Screen | Overall | Notes |
|--------|---------|-------|
| Personal Create empty | GAP | Section overlap 25%; thin iOS layout |
| Personal Life active | PARTIAL | Largest line delta (976 vs 565 APK lines) |
| Group Pulse generic | PARTIAL | 8 APK composables vs 2 iOS section vars |
| Personal / Group / Business Quick Add hubs | PARTIAL | Hub hero + tile grid thinner on iOS; SF Symbols on group/personal hubs |
| Personal Pulse / Moments empties | PARTIAL | Educational hero + CTA polish |
| Wedding Pulse | MATCH (source) | Thinner iOS file but section parity OK — screenshot verify |
| Product Onboarding | PARTIAL | Illustration carousel thinner on iOS |
| App Lock / Consent | PARTIAL | Minimal screens; layout scores low due to size |

## Wave audit notes

### Wave 1 — Shell chrome

- **TopBar / ContextSwitcher / MomentSwitcher:** Structurally aligned (`ShellChrome.swift` ↔ APK components). Company chip embedded in TopBar on iOS vs standalone `CompanySwitcher.kt` — functional parity, PARTIAL on component extraction.
- **Bottom Nav:** Remediated — see P0 table.
- **App shell:** `AppShellView` no longer uses `UITabBarAppearance`; routing unchanged.

### Wave 2 — Auth / onboarding / account

- Login: MATCH.
- Cinematic onboarding: PARTIAL (particle scenes present; section structure differs).
- Product onboarding, consent, app lock: PARTIAL (thin iOS implementations).
- Account hub: PARTIAL (Form-based iOS vs APK sheet layout).

### Wave 3 — Personal

- Active tabs (Life Ops, Future, Lifestyle, Relationships): mostly MATCH/PARTIAL on source; Life tab PARTIAL.
- Empty states: PARTIAL — pulse/moments/create empties need hero parity with APK.
- Setup wizards: cross-check [`PHASE_6_FIGMA_PARITY_CHECKLIST.md`](../implementation/PHASE_6_FIGMA_PARITY_CHECKLIST.md) — Personal setups still await screenshot compare on both platforms.
- **Routing note:** iOS wires `PersonalLifestylePulseActiveView`; APK has `PersonalLifestylePulseActiveContent` but router uses generic pulse — align routing when polishing Lifestyle.

### Wave 4 — Group

- Themed families (Wedding, Experience, Purchase, Living): Runway-equivalent APK screens marked EQUIVALENT in Figma coverage; iOS siblings largely MATCH on source (Wedding/Experience/Purchase pulses).
- Generic `GroupPulseActiveView`: PARTIAL — priority polish.
- Living G09–G12: APK PARTIAL vs Figma; iOS should match APK not Figma.

### Wave 5 — Business

- Runway / Ops / Team Ops active tabs: MATCH on source structure.
- Generic `BusinessPulseActiveView` + Quick Add hub: PARTIAL.
- Company Life: delegates to `BusinessLifeActiveView` on both platforms — MATCH.

### Wave 6 — Global

- Circle + Life360 coming soon: PARTIAL (SF Symbol decorative elements on iOS).
- Manage-moment / finance modals: covered in matrix rows; mostly MATCH/PARTIAL.

## Theme tokens

Shell-level hex values align between `ShellTokens.kt` and `MomentraShellTheme.swift`. Per-family `*ActiveTheme` files are paired in [`IOS_APK_THEME_TOKEN_DIFF.md`](IOS_APK_THEME_TOKEN_DIFF.md).

## Next steps

1. Capture screenshot pairs on macOS + Android emulator into `docs/qa/screenshots/ios-apk-parity/`.
2. Burn down P1 matrix rows starting with Personal Create empty (GAP) and Group Pulse.
3. Re-run matrix script after each remediation wave.
