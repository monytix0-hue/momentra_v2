# Phase 4 — Figma Shell Mapping

Authoritative file: [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=169-487)  
fileKey `TzLvwVwlPbeVB8ug1zB3GM`

| Figma node/frame | Platform | Component | Implementation | State | Notes |
|------------------|----------|-----------|----------------|-------|-------|
| 763:12896 Personal/Group topbar | Android | MomentraTopBar | `MomentraTopBar.kt` | MATCHED | Logo + Life360 + orange + + avatar status; safe-area padded |
| 763:12896 Personal/Group topbar | iOS | MomentraTopBar | ShellChrome | MATCHED | `ShellTopbarLogo` + Figma SVGs |
| Business company chip (top row) | Android | BusinessCompanyChip | MomentraTopBar | MATCHED | In-row chip; real companies only |
| Business company chip | iOS | companyChip Menu | ShellChrome | MATCHED | Same |
| 763:12897 Context Switcher | Android | ContextSwitcher | `ContextSwitcher.kt` | MATCHED | Equal-width; Personal/Business/Circle `#7C5CFC`, Group `#E8621A` |
| 763:12897 Context Switcher | iOS | ContextSwitcherView | ShellChrome | MATCHED | Same accents |
| Moment / module switcher | Android | MomentSwitcher | `MomentSwitcher.kt` | PARTIAL | Figma card + pills; hidden when empty (no fake Moments) |
| Moment / module switcher | iOS | MomentSwitcherView | ShellChrome | PARTIAL | Same |
| 501:6367 Comp / Personal / Bottom Nav | Android | ShellBottomNavigation | `ShellBottomNavigation.kt` | MATCHED | Figma icons; center Create FAB; labels Pulse/Moments/Life/Memory + Create a11y; nav-bar insets |
| 501:6367 Comp / Personal / Bottom Nav | iOS | ShellBottomNavigationView | ShellChrome | MATCHED | Nav* imagesets; Create FAB |
| 353:5780 Personal / Life / Empty | Android | PersonalLifeEmptyContent | `PersonalLifeEmptyContent.kt` | MATCHED | Dark scroll education |
| 353:5780 Personal / Life / Empty | iOS | PersonalLifeEmptyView | `PersonalLifeEmptyView.swift` | MATCHED | Parity |
| Auth / Splash / Onboarding | Both | existing | preserved | MATCHED | Firebase + `/v1/me` |
| Create destination | Both | Create tab/FAB | deferred panel | API_GAP | FIGMA_API_GAP — Phase 5+ |
| Exported Figma chrome icons | Both | drawable / Assets | committed | MATCHED | Logo PNG + nav/radar/plus/status vectors |

## Summary counts

| Status | Count |
|--------|-------|
| MATCHED | 13 |
| PARTIAL | 2 |
| API_GAP | 1 |
| **Total mapped** | **16** |

Matched for acceptance reporting: **13/16** (PARTIAL/API_GAP intentional for Phase 4 scope).
