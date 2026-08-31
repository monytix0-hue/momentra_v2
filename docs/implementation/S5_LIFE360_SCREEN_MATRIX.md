# S5 Life360 Screen Matrix

**Date:** 2026-08-26  
**Figma:** [`1075:7637`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7637) — Life 360: Coming Soon  
**Statuses:** PASS | REUSE | REFACTOR | MISSING | LOCAL_ONLY | DEFERRED | SKIP | NOT_CONNECTED | OUT_OF_SCOPE | NOT_REQUIRED | BLOCKED_ENVIRONMENT

---

## Global entry

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| TopBar Life360 action | shell / `763:12896` | `MomentraTopBar` | `ShellChrome` | — | **REUSE** |
| Open / close overlay | — | `AppShellViewModel.openLife360` | `AppShellModel.openLife360` | — | **REUSE** |
| Profile mutual exclusion | — | same | same | — | **REUSE** |
| Shell selection preserved | — | context/company/moment/tab | same | — | **PASS** target |

---

## Coming Soon surface (`1075:7637` content)

| Section | Figma | Android | iOS | API | Status |
|---------|-------|---------|-----|-----|--------|
| Status bar | frame chrome | OS | OS | — | **NOT_REQUIRED** in overlay |
| TopBar + Context switcher | frame chrome | existing shell under overlay | same | — | **REUSE** (do not duplicate) |
| Coming Soon badge | hero | `Life360GlobalSurface` | `Life360ComingSoonView` | — | **LOCAL_ONLY** |
| Title + subtitle | hero | same | same | — | **LOCAL_ONLY** |
| Illustration | viz | Compose canvas / static | SwiftUI canvas / static | — | **LOCAL_ONLY** |
| Life Map card | development | same | same | — | **LOCAL_ONLY** |
| Feature grid ×4 | preview | same | same | — | **LOCAL_ONLY** (labels only) |
| Progress 65% | progress | same | same | — | **LOCAL_ONLY** decorative |
| Notify Me When Ready | CTA | local Snackbar/Toast ack | local alert/ack | **no notify API** | **LOCAL_ONLY** |
| Info card | footer | same | same | — | **LOCAL_ONLY** |
| Dismiss / Close | — | sheet dismiss | sheet dismiss | — | Instant; always works |

---

## Client `/life360` (hard lock)

| Concern | Android | iOS | Status |
|---------|---------|-----|--------|
| S5 Coming Soon UI fetch | **none** | **none** | **NOT_CONNECTED** / **OUT_OF_SCOPE** |
| `MeRepository.hasLife360` / gateway | must not network for S5 | same | neutralize → local true / unused |
| `ApiService.getLife360` / `APIClient.getLife360` | may remain generated | same | **OUT_OF_SCOPE** for S5 UI |
| Projection table read | — | — | **OUT_OF_SCOPE** |

---

## Future projection (S5-D docs only)

| Concern | Status |
|---------|--------|
| Align clients to live GET for UI | **OUT_OF_SCOPE** S5 |
| Empty-honest `{}` / `circles: []` | Documented in audit |
| OpenAPI / backend change for Coming Soon | **NOT_REQUIRED** |

---

## Populated product (S5-E)

| Concern | Status |
|---------|--------|
| Live metrics / AI / populated Life360 | **SKIP** / **DEFERRED** |

---

## Loading / error / offline (S5-F)

| Concern | Status |
|---------|--------|
| Network loading for Life360 data | **N/A** — no fetch |
| Error / offline for Life360 data | **N/A** |
| Open latency | Instant local surface |
| Dismiss while “loading” | Always works (no loading) |

---

## Privacy / isolation (S5-G)

| Concern | Status |
|---------|--------|
| Overlay mutates context/company/moment | Must **not** |
| Aggregation AuthZ | **DEFERRED** (API later) |

---

## Theme (S5-H)

| Token | Figma / matrix | Status |
|-------|----------------|--------|
| `GlobalSurfaceTheme.life360` surface `#0C0F15` | matrix + Figma topbar | **PASS** target |
| Coming Soon gold `#F2CA50` / `#FFAB40` | `1075:7637` | **REFINE** tokens on surface |
| Card `#161B26`, text `#E5E0EE` / `#C9C4D8` | Figma | **PASS** target |

---

## Cache (S5-I)

| Concern | Status |
|---------|--------|
| Life360 API client cache | **N/A** — no API |
| Open/dismiss latency sample | Optional / document only |
