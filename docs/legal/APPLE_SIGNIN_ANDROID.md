# Sign in with Apple on Android — checklist

Updated: 2026-10-06 (Asia/Calcutta) · Sprint S5

## Status

- **Client code: done, gated OFF by default.**
  - `FirebaseAuthRepository.signInWithApple(activity)` uses Firebase `OAuthProvider("apple.com")` with
    `startActivityForSignInWithProvider` (Custom Tab web flow), scopes `email` + `name`, and resumes a
    `pendingAuthResult` if the activity was recreated mid-flow.
  - `AuthViewModel.signInWithApple(activity)` mirrors Google (analytics `trackAuthResult("apple", …)`,
    then the same backend bootstrap as every other provider).
  - `LoginScreen` shows **Continue with Apple** (`testTag("login.apple")`, analytics `login/btn_apple`)
    **only when** `BuildConfig.APPLE_SIGNIN_ENABLED == true`.
  - Flag source: `apk/local.properties` → `APPLE_SIGNIN_ENABLED=true` (defaults to `false`).
- **Why gated:** the Apple provider is used today by iOS native Sign in with Apple, which only needs the
  App ID / bundle. The Android web flow additionally needs a **Services ID + private key** registered on the
  Firebase Apple provider. That console setup could not be verified from the repo, so the button stays
  hidden to avoid shipping a broken sign-in.

## Console steps (owner: you, ~20 min)

1. **Apple Developer → Identifiers → Services IDs** → create e.g. `resolvingpoint.momentra.signin`.
   - Enable *Sign in with Apple*, primary App ID = the iOS app (`resolvingpoint.momentra`).
   - Domains: `<firebase-project-id>.firebaseapp.com`
   - Return URL: `https://<firebase-project-id>.firebaseapp.com/__/auth/handler`
2. **Apple Developer → Keys** → create a key with *Sign in with Apple* enabled → download the `.p8`
   (one-time download). Note the **Key ID** and your **Team ID**.
3. **Firebase console → Authentication → Sign-in method → Apple**
   - Enabled (already on for iOS).
   - *Services ID* = step 1 identifier; *Apple team ID*; *Key ID*; paste the `.p8` private key.
4. **Apple Developer → Services ID → Configure → Email relay**: register the Firebase sender domain if you
   want to reach users who chose *Hide My Email* (`noreply@<project>.firebaseapp.com`).
5. Make sure the Android app `resolvingpoint.momentra` is registered in Firebase with SHA-1/SHA-256
   (see `ANDROID_PACKAGE_RENAME.md`) and `google-services.json` is refreshed.
6. Set `APPLE_SIGNIN_ENABLED=true` in `apk/local.properties` (or CI gradle property), rebuild, test:
   - new Apple user → lands in consent/onboarding → backend bootstrap creates the Momentra user;
   - existing iOS Apple user signs in on Android → **same Firebase UID** (same Apple ID + same Team).
7. Flip the flag on in release config once verified.

## Notes / risks

- Apple only returns name/email on the **first** authorization; Firebase stores it. Nothing is deleted
  when a user re-authorizes.
- If the same email exists with another provider, Firebase may return
  `account-exists-with-different-credential` — surfaced via `AuthErrorMapper`; account linking is a
  follow-up, not in S5.
- **No user data is deleted** by any part of this flow.
