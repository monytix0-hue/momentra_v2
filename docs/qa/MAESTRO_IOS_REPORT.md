# Maestro iOS Report (S9-QA-B)

**Status:** **IMPLEMENTED** — **EXECUTION = BLOCKED_ENVIRONMENT** (Windows host; needs Mac/Xcode Simulator or CI)  
**App id:** `resolvingpoint.momentra`  
**Harness:** `.maestro/ios/` + `run-qa-ios.sh`

## Environment

| Item | Value |
|------|-------|
| Host | Windows (this workspace) |
| Execution | **BLOCKED_ENVIRONMENT** |
| Suite | Present under `.maestro/ios/` |

## iOS-specific coverage (suite ready)

| Area | Flow / notes | Exec |
|------|----------------|------|
| Apple Sign In | `00_auth/ios_apple_signin.yaml` | BLOCKED_ENVIRONMENT |
| Face ID / Touch ID + PIN | `08_security/ios_biometric_and_pin.yaml` | BLOCKED_ENVIRONMENT |
| Keychain clear on launch | `clearKeychain: true` in launch | BLOCKED_ENVIRONMENT |
| Permission dialogs | Optional `when:` guards (expand on Mac) | BLOCKED_ENVIRONMENT |
| Background / foreground lock | Security flow | BLOCKED_ENVIRONMENT |
| Keyboard / sheets / safe area | Covered in shell + finance flows (visual on Mac) | BLOCKED_ENVIRONMENT |
| Deep-link cold start | Add under `03_group` on Mac | PENDING |

## Smoke / critical

Same behavioral contract as Android (`shared/test-data-contract.md`). Selectors use dotted IDs (`login.apple`, `bottom.pulse`, …).

## Gate

S9-QA-B may close as **PASS** on Mac/CI, or remain **BLOCKED_ENVIRONMENT** with suite complete until RC Mac execution — must not leave release-critical journeys unrun before production RC.
