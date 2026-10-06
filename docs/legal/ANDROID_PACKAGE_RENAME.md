# Android applicationId rename checklist

Updated: 2026-10-06 (Asia/Calcutta)

## Code change (done in repo)

- **`applicationId`**: `com.example.momentra` -> **`resolvingpoint.momentra`** (`apk/app/build.gradle.kts`).
- **`namespace`**: left as `com.example.momentra` so existing `com.example.momentra.R` / `BuildConfig` imports keep compiling. That is fine for Play; only `applicationId` is the store identity.
- Broadcast action: `resolvingpoint.momentra.PERSONAL_EVENING_NUDGE`.
- `web/invite/.well-known/assetlinks.json` lists **both** `resolvingpoint.momentra` and legacy `com.example.momentra` (same debug cert SHA-256), so App Links work for both while old installs exist.

## Current state of `apk/app/google-services.json` (read this)

**Installed 2026-10-06:** official Firebase download is now in `apk/app/google-services.json` (both Android clients present).

| Package | `mobilesdk_app_id` | Android OAuth (`client_type: 1` + cert hash) |
|---------|--------------------|-----------------------------------------------|
| `com.example.momentra` (legacy) | `...:android:1e5714d69fcbc5be88291b` | Yes (SHA-1 `3e38ea66...`, `fa1981ca...`) |
| `resolvingpoint.momentra` (current) | `...:android:d0bed8d293b07c9488291b` | **No** — only web `client_type: 3` |

**SHA fingerprints still required in Firebase** so Android OAuth clients (`client_type: 1` with `certificate_hash`) appear for `resolvingpoint.momentra`. Until you add debug + release (+ Play signing) SHA-1/SHA-256 on that app in Firebase and re-download this file:

- Builds compile and install under `resolvingpoint.momentra`.
- **Google Sign-In will likely fail** (`DEVELOPER_ERROR` / code 10).
- **Phone Auth** may fail Play Integrity / SafetyNet app verification.
- Do **not** hand-edit the JSON. After adding SHAs, download again from Firebase and replace this file.

## Firebase / Google Cloud steps (you must do)

1. Firebase Console -> project **`momentra-v2`** -> Project settings -> *Your apps*: confirm Android app **`resolvingpoint.momentra`** exists (done if this JSON is present). Keep legacy `com.example.momentra` registered.
2. On **`resolvingpoint.momentra`**, add SHA fingerprints (this is what creates Android OAuth clients):
   - Debug: `keytool -list -v -keystore apk/keystore/momentra-debug.jks -alias momentra-debug` (SHA-1 + SHA-256).
   - Release / upload keystore SHA-1 + SHA-256.
   - After first Play upload: Play Console -> *App integrity* -> **App signing key** SHA-1 + SHA-256.
3. Download a fresh **`google-services.json`** and replace `apk/app/google-services.json`. Confirm `resolvingpoint.momentra` now has `oauth_client` entries with `client_type: 1` and `certificate_hash`.
4. Google Cloud Console -> APIs & Services -> Credentials: confirm an **Android OAuth client** exists for `resolvingpoint.momentra` + each SHA-1 (Firebase usually creates it in step 2). If the API key has Android restrictions, add `resolvingpoint.momentra` + SHA-1.
5. Firebase -> Authentication -> Settings -> Authorized domains: unchanged (domains, not packages). Phone auth: nothing extra beyond SHA-256 in step 2.
6. `assetlinks.json`: append the **release / Play app-signing** SHA-256 (no colons) to the `resolvingpoint.momentra` entry and redeploy momentra.tech. Live site currently still serves only the legacy `com.example.momentra` entry.
7. Play Console: create / ship the app as **`resolvingpoint.momentra`** (never publish `com.example.*`; Play rejects it).
8. Devices: uninstall old debug builds first. Android treats the new id as a **different app** (no local data migration; server data is untouched).

## Done when

- [x] New Firebase Android app `resolvingpoint.momentra` exists (JSON has both clients).
- [ ] Debug + release (+ Play signing) SHA-1/SHA-256 added on that app so Android OAuth clients appear.
- [ ] `google-services.json` re-downloaded after SHAs (type-1 clients for `resolvingpoint.momentra`), committed.
- [ ] Google Sign-In, Phone OTP, and an FCM test push work on a fresh install of the new package.
- [ ] momentra.tech serves the updated `assetlinks.json`; `https://momentra.tech/j/<code>` opens the app.

## Repo leftovers (non-blocking)

- QA/Maestro generators (`backend/typescript/scripts/qa/generate-cert-flows.ts`, `check-fcm-activation.mjs`) still use `appId: com.example.momentra`. Update when you run Maestro against the renamed build.
- `apk/app/google-services.json.example` still shows the old package (template only).

## Safety

- This rename does **not** delete user data in Momentra backends; Firebase users and UIDs are per project, not per app, so accounts keep working once Auth is configured.
- Existing installs on `com.example.momentra` will not auto-migrate; treat as a new app id.
