# S7 Global Screen Matrix

**Date:** 2026-08-26  
**Figma:** file `TzLvwVwlPbeVB8ug1zB3GM` — no dedicated global Settings chapter (**FIGMA_GAP**); company `702:9524` / moments settings out of S7 scope.  
**Statuses:** PASS | REUSE | REFACTOR | MISSING | API_GAP | SCHEMA_GAP | FIGMA_GAP | DEFERRED | NOT_REQUIRED | LOCAL_ONLY

---

## Auth (S7-B)

| Screen | Android | iOS | API | Status |
|--------|---------|-----|-----|--------|
| Login / Signup | `LoginScreen` | `LoginView` | Firebase | **REUSE** |
| Google | live | live | Firebase | **REUSE** |
| Apple | — | live | Firebase | iOS **REUSE**; Android **DEFERRED** |
| Phone | live | live | Firebase | **REUSE** |
| Forgot password | — | — | Firebase reset email | **MISSING** |
| Session restore | `AuthViewModel` | same | `GET /me` | **REUSE** |
| Auth errors | `AuthErrorMapper` | same | — | **REFACTOR** harden |

---

## Onboarding (S7-C)

| Screen | Status |
|--------|--------|
| Product carousel | **REUSE** |
| Consent / permissions gate | **MISSING** / **FIGMA_GAP** → minimal consent step |
| Completion persistence | **REUSE** `onboarding_seen` |

---

## Profile / Account (S7-D)

| Screen | Status |
|--------|--------|
| Profile stub sheet | **REFACTOR** → Account hub |
| Display name edit | **API_GAP** → `PATCH /me` |
| Timezone / locale | read via `/me`; write **API_GAP** → PATCH |
| Password change | Firebase client **NOT_REQUIRED** backend |

---

## App Security (S7-E)

| Feature | Status |
|---------|--------|
| PIN setup/change/remove | **LOCAL_ONLY** |
| Biometrics | **LOCAL_ONLY** |
| Auto-lock / resume / timeout | **LOCAL_ONLY** |
| Hide balances | **LOCAL_ONLY** presentation |

---

## Preferences (S7-F)

| Feature | Status |
|---------|--------|
| Currency / language / appearance | **FIGMA_GAP** / **DEFERRED** |
| Timezone / locale | **REFACTOR** via PATCH |

---

## Notifications (S7-G)

| Feature | Status |
|---------|--------|
| OS permission + token → `POST /me/devices` | **MISSING** wire |
| Push send | **STUB** / **DEFERRED** |
| Prefs UI | **LOCAL_ONLY** / minimal |

---

## Privacy / Consent (S7-H)

| Feature | Status |
|---------|--------|
| Consent list/grant/withdraw | **API_GAP** → live routes |
| Privacy UI | **FIGMA_GAP** shell UI |

---

## Devices / Sessions (S7-I)

| Feature | Status |
|---------|--------|
| Register / revoke | **REUSE** |
| List devices | **API_GAP** → `GET /me/devices` |
| Logout-all sessions | **DEFERRED** |

---

## Account lifecycle (S7-J)

| Feature | Status |
|---------|--------|
| Logout + cache clear | **REFACTOR** harden user-scoped wipe |
| Soft-delete account | **MISSING** → `DELETE /me` soft |
| Hard wipe | **SCHEMA_GAP** |

---

## Deep links (S7-K)

| Feature | Status |
|---------|--------|
| Group invite iOS | **REUSE** |
| Group invite Android cold-start | **REFACTOR** |
| Business invite | **DEFERRED** |
| Push routing | **DEFERRED** (send stub) |

---

## Help / Legal (S7-L)

| Feature | Status |
|---------|--------|
| About / Privacy / Terms | **FIGMA_GAP** — static placeholder links |

---

## Theme (S7-M)

| Feature | Status |
|---------|--------|
| Settings chrome vs shell tokens | **REFACTOR** align |
