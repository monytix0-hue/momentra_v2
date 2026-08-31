# S7 Global Parity Matrix

**Date:** 2026-08-26  
**Scope:** Auth, onboarding/consent, account hub, local App Lock, prefs, devices, privacy, lifecycle, deep links, legal, theme  
**Figma:** `TzLvwVwlPbeVB8ug1zB3GM` — no dedicated Settings chapter → hub UI is **FIGMA_GAP** shell-consistent

| Surface | Android | iOS | Backend | Notes |
|---------|---------|-----|---------|-------|
| Email/password login+register | PASS | PASS | Firebase | REUSE |
| Google sign-in | PASS | PASS | Firebase | REUSE |
| Apple sign-in | N/A (honest omit) | PASS | Firebase | iOS-only |
| Forgot / reset password | PASS | PASS | Firebase `sendPasswordResetEmail` | LOCAL client |
| Session restore + offline identity | PASS | PASS | GET `/me` | REUSE |
| Product onboarding | PASS | PASS | — | REUSE `onboarding_seen` |
| Consent gate (pre-login) | PASS | PASS | — | Ack-only; grant in hub |
| Account hub profile + PATCH `/me` | PASS | PASS | PASS | displayName/timezone/locale |
| Soft-delete DELETE `/me` | PASS | PASS | PASS | status=DELETED; Firebase deleteUser best-effort |
| Local PIN App Lock | PASS | PASS | N/A | LOCAL_ONLY — never networked |
| Biometrics unlock | PASS | PASS | N/A | BiometricPrompt / LocalAuthentication |
| Auto-lock on background | PASS | PASS | N/A | Idle timeout local |
| Hide balances (presentation) | PASS | PASS | N/A | Pulse finance surfaces |
| Currency / language / appearance | DEFERRED | DEFERRED | SCHEMA_GAP | FIGMA_GAP documented in hub |
| Device register POST `/me/devices` | PASS | PASS | PASS | On auth + hub |
| Device list GET `/me/devices` | PASS | PASS | PASS | |
| Device revoke DELETE | PASS | PASS | PASS | |
| Logout-all sessions | DEFERRED | DEFERRED | SCHEMA_GAP | No `user_session` |
| Consent list/grant/withdraw | PASS | PASS | PASS | Catalogue V021 |
| Group invite deep-link cold-start | PASS | PASS | PASS | Android `PendingJoinInvite` |
| Business invite deep-link | DEFERRED | DEFERRED | API_GAP | Not invented |
| Help / About / Legal | FIGMA_GAP | FIGMA_GAP | — | Placeholder copy |
| Theme shell tokens | PASS | PASS | — | Auth/lock use brand tokens |
| User A→B isolation | PASS (unit) | PASS (unit) | PASS (API smoke) | Shell clear + prefs wipe |

**Classification legend:** PASS | REUSE | DEFERRED | FIGMA_GAP | API_GAP | SCHEMA_GAP | LOCAL_ONLY | N/A
