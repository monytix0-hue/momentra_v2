# S7 Global / Settings / Security — Implementation Report

**Date:** 2026-08-26  
**Verdict:** S7 GLOBAL / SETTINGS / SECURITY — **PASS**  
**Next:** S8 AI + ANALYTICS — **NOT STARTED**

```text
S0–S6 PASS / CLOSED
S7 GLOBAL / SETTINGS / SECURITY — COMPLETE (A→O)
STOP — do not start S8
```

---

## Execution

| Step | Outcome |
|------|---------|
| S7-A Audit + matrix | `S7_GLOBAL_AUDIT.md`, `S7_GLOBAL_SCREEN_MATRIX.md` |
| S7-B Auth | Forgot/reset password both platforms; Apple iOS-only; session restore REUSE |
| S7-C Onboarding | Consent gates Android+iOS; `onboarding_seen` / consent ack persistence |
| S7-D Profile | Account hubs; `PATCH /v1/me` |
| S7-E App Lock | PIN Keystore/Keychain; biometrics; auto-lock; hide balances presentation |
| S7-F Prefs | Currency/language/appearance **DEFERRED** (FIGMA_GAP / no prefs JSONB) |
| S7-G Notifications | OS permission request; `POST /me/devices`; send worker **STUB** |
| S7-H Consent | `GET/POST grant/withdraw` + hub toggles vs catalogue |
| S7-I Devices | List + revoke; logout-all **DEFERRED** |
| S7-J Lifecycle | Logout isolation harden; soft-delete + best-effort Firebase `deleteUser` |
| S7-K Deep links | Android group cold-start → redeem; Business invite **DEFERRED** |
| S7-L Legal | Placeholder About/Privacy/Terms (FIGMA_GAP) |
| S7-M Theme | Auth/lock/consent on brand/shell tokens |
| S7-N Isolation | User A→B shell unit proofs; PIN never networked |
| S7-O Tests + docs | Backend `s7-account.test.ts`; Android security + shell tests; parity + this report |

---

## Architecture locks honored

```text
Firebase Auth → Momentra Bearer session → Local App Lock (PIN never to API)
```

- Hide balances = presentation only (not AuthZ)  
- Soft-delete `user_profile.status=DELETED`; no invented hard cascade wipe  
- Logout-all / Business invite invention / push send worker / S8 — out of scope  

---

## Tests

| Suite | Result |
|-------|--------|
| Backend `tests/s7-account.test.ts` | PASS (prefs / soft-delete / devices / consents) |
| Android `AppLockSecurityTest` + `accountSwitchIsolation` | compile + unit |
| iOS `accountSwitchIsolation` / `balanceMaskPresentationOnly` | source in `ShellModelTests.swift` |

---

## Docs

- `S7_GLOBAL_AUDIT.md`
- `S7_GLOBAL_SCREEN_MATRIX.md`
- `S7_GLOBAL_PARITY_MATRIX.md`
- This report

**STOP — S8 NOT STARTED.**
