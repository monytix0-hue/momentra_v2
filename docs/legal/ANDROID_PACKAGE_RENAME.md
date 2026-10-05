# Android applicationId rename checklist

Updated: 2026-10-05 (Asia/Calcutta)

## Code change (done in repo)

- **`applicationId`**: `com.example.momentra` → **`resolvingpoint.momentra`** (`apk/app/build.gradle.kts`)
- **`namespace`**: left as `com.example.momentra` so existing `com.example.momentra.R` / `BuildConfig` imports keep compiling
- Broadcast action: `resolvingpoint.momentra.PERSONAL_EVENING_NUDGE`
- `apk/app/google-services.json` `package_name` fields updated to **`resolvingpoint.momentra`** (must match `applicationId` for the Google Services plugin)

## Firebase / Google Cloud (you must do)

Until these console steps are finished, Google Sign-In / Phone Auth / FCM may fail on Android builds with the new id.

1. Firebase Console → project `momentra-v2` → Add Android app with package name **`resolvingpoint.momentra`**.
2. Register the **debug** SHA-1 / SHA-256 from `apk/keystore/momentra-debug.jks` and your **release** keystore.
3. Download the new `google-services.json` and replace `apk/app/google-services.json` (the checked-in file has renamed `package_name` but still carries the old `mobilesdk_app_id` until you swap it).
4. Google Cloud Console → OAuth Android client: package `resolvingpoint.momentra` + matching SHA-1.
5. Play Console: ship under `resolvingpoint.momentra` (do not publish `com.example.*`).
6. Uninstall old debug builds before installing the renamed package (Android treats it as a different app).

## Safety

- This rename does **not** delete user data in Momentra backends.
- Existing installs on `com.example.momentra` will not auto-migrate; treat as a new app id.