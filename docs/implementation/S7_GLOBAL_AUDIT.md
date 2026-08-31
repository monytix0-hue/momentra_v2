# S7 Global / Settings / Security Audit

**Date:** 2026-08-26  
**Scope:** Account-level auth, security, preferences, privacy, devices, lifecycle, deep links  
**Figma file:** `TzLvwVwlPbeVB8ug1zB3GM` (shell root `169:487`)  
**Rule:** Prefer REUSE/REFINE. No invented hard account wipe. PIN = LOCAL_ONLY. Discover delete semantics from SQL.

---

## Executive summary (post-implementation)

| Area | Classification |
|------|----------------|
| Firebase login/signup/Google/phone; iOS Apple | **REUSE** / **PASS** |
| Forgot / reset password | **PASS** (Firebase client) |
| Session restore + offline identity | **REUSE** |
| Product onboarding carousel | **REUSE** |
| Consent / legal gates in onboarding | **PASS** (ack gate + hub grant APIs) |
| Profile → account hub | **PASS** |
| `PATCH /me` prefs / display name | **PASS** |
| PIN / biometrics / auto-lock | **PASS** / **LOCAL_ONLY** |
| Hide balances | **PASS** / **LOCAL_ONLY** (presentation) |
| Currency / language / appearance prefs | **FIGMA_GAP** / **DEFERRED** |
| Devices register/list/revoke | **PASS**; logout-all **DEFERRED** |
| Push send worker | **STUB** / **DEFERRED** |
| Consent catalogue + grant/withdraw APIs | **PASS** |
| Soft-delete `user_profile.status=DELETED` | **PASS** |
| Hard delete cascade | **SCHEMA_GAP** — documented |
| Group invite deep links | **PASS** (Android cold-start wired) |
| Business invite links | **API_GAP** / **DEFERRED** |
| Help / About / Legal | **FIGMA_GAP** — placeholder |

See `S7_GLOBAL_PARITY_MATRIX.md` + `S7_IMPLEMENTATION_REPORT.md` for final status.

---

## Executive summary (audit baseline — superseded)

| Area | Classification |
|------|----------------|
| Firebase login/signup/Google/phone; iOS Apple | **REUSE** |
| Forgot / reset password | **MISSING** → Firebase client |
| Session restore + offline identity | **REUSE** |
| Product onboarding carousel | **REUSE** |
| Consent / legal gates in onboarding | **MISSING** / **FIGMA_GAP** |
| Profile sheet (name/email/sign-out) | **REFACTOR** → account hub |
| `PATCH /me` prefs / display name | **API_GAP** |
| PIN / biometrics / auto-lock | **MISSING** / **LOCAL_ONLY** |
| Hide balances | **MISSING** / **LOCAL_ONLY** (presentation) |
| Currency / language / appearance prefs | **FIGMA_GAP** / **DEFERRED** (no user prefs JSONB) |
| `POST/DELETE /me/devices` | **REUSE**; list **API_GAP** |
| Push send worker | **STUB** / **DEFERRED** |
| Consent SQL catalogue (V008/V021) | **REUSE** schema; APIs **API_GAP** |
| Soft-delete `user_profile.status=DELETED` | **SCHEMA** live; command **MISSING** |
| Hard delete cascade | **SCHEMA_GAP** — many `ON DELETE RESTRICT` |
| Logout-all / session inventory | **DEFERRED** (no `user_session`) |
| Group invite deep links | Runtime **REUSE**; Android cold-start **REFACTOR** |
| Business invite links | **API_GAP** / **DEFERRED** |
| Help / About / Legal | **FIGMA_GAP** — minimal static only |
| Dedicated global Settings Figma chapter | **FIGMA_GAP** (company/moments settings only in file) |

---

## Security architecture (locked)

```text
Firebase Authentication
  → Momentra session (Bearer → GET /me)
  → Local App Lock (PIN / biometric)  ← never networked
  → Unlock app
```

Hide balances = presentation preference, not AuthZ.

---

## Delete-account discovery

| Fact | Detail |
|------|--------|
| Status enum | `ACTIVE \| INACTIVE \| SUSPENDED \| DELETED` on `core.user_profile` (V002) |
| Soft-delete path | `UPDATE status='DELETED'` — **ship this** |
| Hard wipe | Blocked by RESTRICT FKs (e.g. consent → user); **do not invent** |
| Devices | `ON DELETE CASCADE` from user — soft-delete does not remove rows; revoke `revoked_at` instead |
| Firebase | Client `deleteUser()` when reauth allows; document failure as residual identity |
| Retention | Domain rows remain; document **SCHEMA_GAP** for anonymization |

---

## Consent catalogue (seeded; no API yet)

Purpose codes: `PERSONAL_ANALYTICS`, `CROSS_DOMAIN_LIFE360`, `AI_INSIGHT_GENERATION`, `AI_RECOMMENDATION_GENERATION`, `AI_ACTION_ASSISTANCE`, `MEMORY_PATTERN_ANALYSIS`, `GROUP_DATA_SHARING`, `BUSINESS_ANALYTICS`.

Tables: `governance.consent_purpose`, `governance.consent` (ACTIVE/WITHDRAWN/…).

---

## Devices

`platform.user_device`: register/upsert + revoke live. No GET list → add for S7-I.

---

## Invites

| Path | Status |
|------|--------|
| `POST/GET/POST …/group/invites` | Live |
| Android intent → PendingJoinInvite | Unwired |
| Business invite URL | **DEFERRED** |

---

## Preferences model

`user_profile` has `timezone`, `locale`, `display_name` — **no preferences JSONB**.  
Currency/appearance/hide-balances: **LOCAL_ONLY** on clients unless later migration (out of inventing columns in S7 beyond PATCH of existing fields).

---

## Out of scope

S8 AI product, Circle/Life360 API connect, Business invite invention, multi-session logout-all, push send production, V030, hard wipe.
