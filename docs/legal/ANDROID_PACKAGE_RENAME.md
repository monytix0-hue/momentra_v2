# Android applicationId rename checklist

Updated: 2026-10-06 (Asia/Calcutta)

## Code change (done in repo)

- **`applicationId`**: `com.example.momentra` -> **`resolvingpoint.momentra`** (`apk/app/build.gradle.kts`).
- **`namespace`**: left as `com.example.momentra` so existing `com.example.momentra.R` / `BuildConfig` imports keep compiling. That is fine for Play; only `applicationId` is the store identity.
- Broadcast action: `resolvingpoint.momentra.PERSONAL_EVENING_NUDGE`.
- `web/invite/.well-known/assetlinks.json` lists **both** `resolvingpoint.momentra` and legacy `com.example.momentra` (same debug cert SHA-256), so App Links work for both while old installs exist.

## Current state of `apk/app/google-services.json` (read this)

In S2 the checked-in file was **hand-edited**: `package_name` was changed to `resolvingpoint.momentra` so the Google Services Gradle plugin accepts the build. Everything else still belongs to the **old** Firebase Android app (`com.example.momentra`):

- `mobilesdk_app_id` `1:315259659778:android:1e5714d69fcbc5be88291b` = the old app registration.
- The two `client_type: 1` Android OAuth clients (SHA-1 `3e38ea66...` and `fa1981ca...`) were registered in Google Cloud for `com.example.momentra`. Editing the JSON does not change them on Google's side.

What that means until the console steps are done:

- Builds compile and install.
- **Google Sign-In will likely fail** (`DEVELOPER_ERROR` / code 10), because no OAuth Android client exists for `resolvingpoint.momentra` + your SHA-1.
- **Phone Auth** may fail Play Integrity / SafetyNet app verification for the new package.
- **FCM / Analytics / Crashlytics** may be rejected or misattributed if the API key has Android app restrictions.

Do **not** hand-edit the JSON further. Replace it with the file Firebase generates (step 3).

## Firebase / Google Cloud steps (you must do)

1. Firebase Console -> project **`momentra-v2`** -> Project settings -> *Your apps* -> **Add app -> Android**, package name **`resolvingpoint.momentra`**. Keep the old `com.example.momentra` app registered (do not delete it).
2. In that new app, add SHA fingerprints:
   - Debug: `keytool -list -v -keystore apk/keystore/momentra-debug.jks -alias momentra-debug` (SHA-1 + SHA-256).
   - Release / upload keystore SHA-1 + SHA-256.
   - After first Play upload: Play Console -> *App integrity* -> **App signing key** SHA-1 + SHA-256 (add these too).
3. Download the new **`google-services.json`** and replace `apk/app/google-services.json` as-is. Confirm it has a new `mobilesdk_app_id` and an OAuth `client_type: 1` entry for `resolvingpoint.momentra`.
4. Google Cloud Console -> APIs & Services -> Credentials: confirm an **Android OAuth client** exists for `resolvingpoint.momentra` + each SHA-1 (Firebase usually creates it in step 2). If the API key has Android restrictions, add `resolvingpoint.momentra` + SHA-1.
5. Firebase -> Authentication -> Settings -> Authorized domains: unchanged (domains, not packages). Phone auth: nothing extra beyond SHA-256 in step 2.
6. `assetlinks.json`: append the **release / Play app-signing** SHA-256 (no colons) to the `resolvingpoint.momentra` entry and redeploy momentra.tech. Live site currently still serves only the legacy `com.example.momentra` entry.
7. Play Console: create / ship the app as **`resolvingpoint.momentra`** (never publish `com.example.*`; Play rejects it).
8. Devices: uninstall old debug builds first. Android treats the new id as a **different app** (no local data migration; server data is untouched).

## Done when

- [ ] New Firebase Android app `resolvingpoint.momentra` exists with debug + release (+ Play signing) SHA-1/SHA-256.
- [ ] `apk/app/google-services.json` is the downloaded file (new `mobilesdk_app_id`), committed.
- [ ] Google Sign-In, Phone OTP, and an FCM test push work on a fresh install of the new package.
- [ ] momentra.tech serves the updated `assetlinks.json`; `https://momentra.tech/j/<code>` opens the app.

## Repo leftovers (non-blocking)

- QA/Maestro generators (`backend/typescript/scripts/qa/generate-cert-flows.ts`, `check-fcm-activation.mjs`) still use `appId: com.example.momentra`. Update when you run Maestro against the renamed build.
- `apk/app/google-services.json.example` still shows the old package (template only).

## Safety

- This rename does **not** delete user data in Momentra backends; Firebase users and UIDs are per project, not per app, so accounts keep working once Auth is configured.
- Existing installs on `com.example.momentra` will not auto-migrate; treat as a new app id.
