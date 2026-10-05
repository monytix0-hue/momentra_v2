# Production security gate evidence

Recorded: 2026-10-01

Git SHA: `a577bce` (dirty working tree; the debug Android build was installed from that tree)

API: `https://api.momentra.tech` (`GET /health/ready` 200, unauthenticated `GET /v1/me` 401, `X-Dev-Firebase-Uid` on `GET /v1/me` 401)

Firebase project: `momentra-v2`

Database host: `aws-1-ap-south-1.pooler.supabase.com:6543`

Storage host: `udbjpqpfqmllwbnrhaxi.supabase.co`

Android: Nothing A059, Android 16 (SDK 36), `com.example.momentra` versionName 2.0 / versionCode 2, debug install on this date. No iOS device or Xcode toolchain is available on this Windows host.

Overall gate: **PASS CANDIDATE**. At least one check is FAIL or BLOCKED_ENVIRONMENT, so [MOMENTRA_SECURITY_POSITION.md](MOMENTRA_SECURITY_POSITION.md) stays PASS CANDIDATE.

Screenshots of the signed-in shell were not copied into the repo.

## Android device

### PIN wrong, PIN correct, cold start — PASS

On the installed debug build, App Security saved a local PIN and reported "PIN saved locally". `am force-stop` then launch showed "App Locked" / "Enter your local PIN to continue."

- Wrong PIN `1111`: screen stayed "App Locked" and showed "Incorrect PIN".
- Correct PIN: the lock screen was replaced by the signed-in shell.

The verification PIN was removed afterward ("PIN removed") so the device was not left on that PIN.

### Biometric unlock — BLOCKED_ENVIRONMENT

With "Unlock with biometrics" on, a cold start showed the system prompt "Unlock Momentra" / "Confirm it's you" / "Touch the fingerprint sensor", with "Use PIN". Automation cannot supply a fingerprint. The successful biometric unlock was not completed. The switch was turned off before sign-out.

### Background / foreground auto-lock — FAIL

Auto-lock was set to 0 seconds. The label after leaving and reopening App Security read "Auto-lock after 0s in background (0 = immediate)."

Home was pressed until `topResumedActivity` was the Nothing launcher. Returning to Momentra showed the same in-app sheet, not "App Locked". A process kill does show the lock screen; a normal background/foreground transition did not.

### Balance mask — BLOCKED_ENVIRONMENT

Hide balances was turned on. Personal Pulse showed "Nothing logged yet today." Personal Life showed "Nothing logged this week yet" and "No activity to allocate this week." There was no money figure on those surfaces to compare with the mask. Sign-out clears the hide-balances preference; the prefs file after sign-out has no `hide_balances` entry.

### Logout and device registration — PASS

Sign out from Account left the screen on "Sign in" (`login.screen`). In the following minutes the database had one non-test `ANDROID` device with `revoked_at` set, and one non-test `DEVICE_REVOKE` audit row with outcome `SUCCEEDED`. The client was not left inside the signed-in shell.

## iOS device — BLOCKED_ENVIRONMENT

This host has no `xcrun` and no attached iOS device. PIN, biometric, auto-lock, balance mask, and on-device logout were not run on iOS. The SHA-256 Keychain path was not exercised on a phone.

## Firebase revocation — PASS

A disposable Firebase user was created in `momentra-v2`, exchanged for an ID token, then deleted at the end.

- `GET https://api.momentra.tech/v1/me` with that bearer token: 200
- `POST /v1/me/devices`: 201, status `ACTIVE`
- `DELETE /v1/me/devices/{deviceId}`: 200, status `REVOKED`
- `GET /v1/me/devices`: that device `revoked: true`
- `audit.audit_record` action `DEVICE_REVOKE`, outcome `SUCCEEDED`
- `revokeRefreshTokens` on that user only, then the same bearer token replayed: 401 `Invalid or expired token.`

## Production hygiene

### API process log — BLOCKED_ENVIRONMENT for the production log host

The IDOR run's structured log lines (route, method, status, duration, canonical user id) did not contain the canary expense description, amounts, or signed URLs. That output is from the local test process against this database, not from the production API host's log drain. No production log viewer was available.

### Crash telemetry — BLOCKED_ENVIRONMENT

Crashlytics `topIssues` for Android app `1:315259659778:android:1e5714d69fcbc5be88291b` returned 403: Application Default Credentials have no quota project for `firebasecrashlytics.googleapis.com`. `SENTRY_DSN` is unset in the local API env. Canaries were not searched in crash events.

### Lock-screen notifications — BLOCKED_ENVIRONMENT

`dumpsys notification` on the Android device had no Momentra notification. A money or mood push was not delivered during this run, so the lock-screen title and body were not read on the shade.

## Media

### Signed URL expiry — PASS

Buckets from the storage API:

- `momentra-attachments` public `false`
- `momentra-media` public `false`

A 1-pixel PNG was uploaded to `momentra-media` under `security-gate/`. `createSignedUrl` with 900 seconds returned a URL whose token expiry was 900 seconds ahead.

- GET while the URL was valid: 200
- GET of the same path with the query string removed: 400
- GET of the same signed URL after 910 seconds: 400

The test object was then removed.

### Attachment id substitution — PASS

Re-ran `backend/typescript/tests/security-gate-idor.test.ts` against this database. Authenticated caller, someone else's expense attachment id: `DELETE /moments/:momentId/expenses/:expenseId/attachments/:uploadId` returned 404. The test also asserts the body has no `downloadUrl` and no canary text. The suite passed.

## Infrastructure encryption and backups — BLOCKED_ENVIRONMENT

The storage API shows the media buckets are private. It does not return a disk-encryption flag for PostgreSQL, object storage, or backups, and no Supabase management token was available to read backup configuration. Encryption at rest is not marked PASS from the position document.

## What this does not change

No E2EE, Private Vault, or User Trust and Privacy work was added. The gate is not PASS.
