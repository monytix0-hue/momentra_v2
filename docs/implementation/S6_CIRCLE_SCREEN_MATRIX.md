# S6 Circle Screen Matrix

**Date:** 2026-08-26  
**Figma:** [`1075:7556`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7556) — Circle: Coming Soon  
**Statuses:** PASS | REUSE | REFACTOR | MISSING | LOCAL_ONLY | SKIP | NOT_CONNECTED | OUT_OF_SCOPE | NOT_REQUIRED | DEFERRED

---

## Context entry

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| ContextSwitcher Circle tab | frame chrome | `ContextSwitcher` | `ContextSwitcherView` | bootstrap | **REUSE** |
| CIRCLE in supportedContexts | — | shell | shell | `GET /me` bootstrap | **REUSE** |
| Moment switcher | — | hidden | hidden | — | **REUSE** policy |
| Hard Deferred placeholder | — | VM Deferred | Model deferred | — | **REFACTOR** → Empty/Coming Soon |

---

## Coming Soon surface (`1075:7556`)

| Section | Figma | Android | iOS | API | Status |
|---------|-------|---------|-----|-----|--------|
| Hero illustration | network rings | `CircleComingSoonContent` | `CircleComingSoonView` | — | **LOCAL_ONLY** |
| Coming Soon badge | pink pill | same | same | — | **LOCAL_ONLY** |
| Title + subtitle | hero text | same | same | — | **LOCAL_ONLY** |
| Feature grid 2×2 | preview | same | same | — | **LOCAL_ONLY** |
| Development card | copy | same | same | — | **LOCAL_ONLY** |
| Progress 45% | bar | same | same | — | **LOCAL_ONLY** decorative |
| Notify Me When Ready | CTA | Snackbar LOCAL_ONLY | Alert LOCAL_ONLY | **no notify API** | **LOCAL_ONLY** |
| Info card | footer | same | same | — | **LOCAL_ONLY** |

---

## Shell states (S6-C)

| State | When | UI | Status |
|-------|------|-----|--------|
| Loading | bootstrap Loading + CIRCLE | shell loading container | **REUSE** |
| Error / Offline / Forbidden | bootstrap failure | shell error/offline + Retry | **REUSE** |
| Ready / Empty + CIRCLE | bootstrap ok | Coming Soon body | **REFACTOR** |
| Circle-data fetch loading | — | — | **N/A** (no fetch) |

---

## Client API (hard lock)

| Concern | Status |
|---------|--------|
| Circle CRUD | **OUT_OF_SCOPE** |
| `GET /life360` from Circle UI | **NOT_CONNECTED** |
| `projection.life360` read | **OUT_OF_SCOPE** |
| Generated CircleApi / hasLife360 for Circle UI | unused |

---

## Capability (S6-D)

| Concern | Status |
|---------|--------|
| Bootstrap always includes CIRCLE | **PASS** target |
| Circle capability seed | **NOT_REQUIRED** — do not invent |
| Future Life360/Circle contract | Documented in audit |

---

## Populated (S6-E)

| Concern | Status |
|---------|--------|
| Populated Circle / live network map | **SKIP** |

---

## Theme (S6-F)

| Token | Target | Status |
|-------|--------|--------|
| CIRCLE context accent | `#E86BA3` / selected `#FC6A8B` | **REFACTOR** from purple |
| Coming Soon pink gradient | `#E86BA3` → `#FF6B8A` | **PASS** target |

---

## Isolation / cache (S6-G)

| Concern | Status |
|---------|--------|
| No P/G/B moment leakage on CIRCLE | **PASS** target |
| Circle API cache | **N/A** |
| Logout clears shell | **REUSE** |
