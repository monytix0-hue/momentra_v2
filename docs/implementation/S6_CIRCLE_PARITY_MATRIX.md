# S6 Circle Parity Matrix

**Date:** 2026-08-26  
**Figma:** [`1075:7556`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7556)  
**Rule:** Coming Soon context only — no Circle CRUD / no client `/life360`.

| Surface | Android | iOS | Match |
|---------|---------|-----|-------|
| ContextSwitcher Circle | `ContextSwitcher` | `ContextSwitcherView` | **PASS** |
| Content state | `Empty` (not Deferred) | `.empty` | **PASS** |
| Coming Soon body | `CircleComingSoonContent` | `CircleComingSoonView` | **PASS** |
| Moment switcher hidden | policy | policy | **PASS** |
| Empty moments on CIRCLE | VM | Model | **PASS** |
| No P/G/B leakage | unit test | — | **PASS** |
| Loading/Error/Offline/Retry | shell containers | shell containers | **PASS** |
| Notify CTA | Snackbar LOCAL_ONLY | Alert LOCAL_ONLY | **PASS** |
| Progress 45% | decorative | decorative | **PASS** |
| GET `/life360` from Circle UI | none | none | **PASS** |
| CIRCLE theme pink | `ContextThemes` / `ShellTokens` | `ContextTheme` / `CircleComingSoonTheme` | **PASS** |
| Life360 overlay unchanged | S5 | S5 | **PASS** |
| Populated | SKIP | SKIP | **N/A** |

**iOS:** UI wired; CI may be **BLOCKED_ENVIRONMENT** without macOS.
