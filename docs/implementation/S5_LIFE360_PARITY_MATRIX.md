# S5 Life360 Parity Matrix

**Date:** 2026-08-26  
**Figma:** [`1075:7637`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7637)  
**Rule:** Coming Soon only — no client `/life360` calls.

| Surface | Android | iOS | Match |
|---------|---------|-----|-------|
| TopBar entry | `MomentraTopBar` life360 | `ShellChrome` Open Life360 | **PASS** |
| Overlay open/close | `openLife360` + ModalBottomSheet | `openLife360` + sheet | **PASS** |
| Profile mutual exclusion | VM | Model | **PASS** |
| Shell selection preserved | unit test | unit test | **PASS** |
| Coming Soon hero + copy | `Life360ComingSoon.kt` | `Life360ComingSoonView.swift` | **PASS** |
| Feature preview labels | static ×4 | static ×4 | **PASS** |
| Progress 65% | decorative LOCAL_ONLY | decorative LOCAL_ONLY | **PASS** |
| Notify CTA | Snackbar LOCAL_ONLY | Alert LOCAL_ONLY | **PASS** |
| GET `/life360` from UI | none | none | **PASS** |
| Theme gold / surface | `GlobalSurfaceTheme.life360` | same tokens | **PASS** |
| Populated product | SKIP | SKIP | **N/A** |
| API cache | N/A | N/A | **N/A** |

**iOS environment:** UI wired; device/simulator compile may be **BLOCKED_ENVIRONMENT** on CI without macOS — source parity still required.
