# Launch decisions (Momentra)

Updated: 2026-11-13 (Asia/Calcutta)

## DPDP Act (engineering track)

- Gap audit: [DPDP_GAP_AUDIT.md](./DPDP_GAP_AUDIT.md) (maps to [DPDP Act, 2023 MeitY PDF](https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf)).
- What shipped: [DPDP_COMPLIANCE_NOTE.md](./DPDP_COMPLIANCE_NOTE.md).
- Ops erasure + breach stub: [DPDP_ERASURE_RUNBOOK.md](./DPDP_ERASURE_RUNBOOK.md).
- **Age gate:** product requires **18+** (`momentra_age_gate_accepted_18plus`).
- **Account deletion:** principal-owned erasure via `DELETE /v1/me` (`eraseMe`) + preflight; shared graphs retained for others.
- **Still locked:** no DROP/TRUNCATE of shared tables or other users’ data; no aggressive hard wipe of Group/Business content.
- **Counsel:** Privacy/Terms remain DRAFT until lawyer review; do not market “fully compliant” without counsel.

## Data deletion policy (locked)

- **No DROP/TRUNCATE** of production shared graphs or other principals’ data for “cleanup.”
- **Account deletion = principal-owned erasure** (`DELETE /v1/me`): anonymize profile tombstone, revoke devices, erase personal-only moments / USER media / consents / notifications; leave shared Group/Business membership content for others (organizer/owner transfer required when others remain).
- Access / correction / residual erasure / grievance: `hello@momentra.app` (matches live momentra.tech Privacy/Terms).

## Legal documents

- Privacy Policy and Terms of Service in-repo drafts: `web/legal/privacy.html`, `web/legal/terms.html`.
- Canonical public URLs (apps + invite footer): `https://momentra.tech/privacy`, `https://momentra.tech/terms`.
- **Status:** DRAFT â€” pending lawyer review. Not legal advice.
- Public contact for policies and in-app support copy: `hello@momentra.app`.
- `resolvingpoint@gmail.com` stays only as the Firebase / Google OAuth consent-screen support email (`firebase.json` `supportEmail`, Firebase console). Change it there only in the console if you ever want to; it is not user-facing support.

## Sprint S1 scope (this decision set)

- Wire Privacy / Terms links in Android + iOS Account hub and consent gate.
- Invite `join.html` footer links.
- In-repo draft policy pages + third-party font notices.
- **Out of scope for S1:** ATS changes, security headers, AI key fail-closed, Android `applicationId` rename, age gate, data export endpoint, hard-wipe jobs, any database destructive changes.

## Billing

- No in-app purchases / subscriptions in current scope â†’ refund policy deferred until billing ships.

## Hosting note

- Publish or reverse-proxy `web/legal/*.html` (or equivalent CMS pages) to `momentra.tech/privacy` and `momentra.tech/terms` so the in-app links resolve.

## Sprint S2 scope (security hardening)

- iOS ATS: removed `NSAllowsArbitraryLoads` (HTTPS-only / default ATS).
- FastAPI AI: fail-closed when `MOMENTRA_AI_INTERNAL_KEY` empty under production/staging (`MOMENTRA_ENV`/`NODE_ENV`); compose sets `MOMENTRA_ENV=production` for `fastapi-ai`.
- Express API: `helmet` security headers (CSP, HSTS, X-Frame-Options DENY, nosniff, Referrer-Policy).
- Android `applicationId` -> `resolvingpoint.momentra` (namespace kept); see `docs/legal/ANDROID_PACKAGE_RENAME.md` for Firebase console steps.
- Admin dashboard: document `VITE_ADMIN_API_KEY` client-bundle risk; ignore env key in production builds.
- iOS `PrivacyInfo.xcprivacy` (UserDefaults CA92.1).
- **Superseded (DPDP full track):** principal-owned `eraseMe` + 18+ gate; shared DROP still forbidden.

## Sprint S3 scope (age, disclaimer, honesty)

- Originally shipped 13+ gate (`momentra_age_gate_accepted_13plus`); **superseded** by 18+ (`momentra_age_gate_accepted_18plus`).
- Consent gate copy lists purposes briefly (account/moments, analytics, AI, memory) aligned with Privacy Policy; `/v1/me/consents` API unchanged.
- Finance disclaimer on Business Money/Runway Pulse and Personal Master Expense: "Not financial, legal, or tax advice."
- Delete-account copy originally soft-delete; **superseded** by principal-owned erasure + preflight UX.
- Live policy pages on momentra.tech remain source of truth; in-repo drafts stay DRAFT until counsel.

## Sprint S4 scope (ops polish)

- Dependabot: `.github/dependabot.yml` (npm: `backend/typescript`, `admin-dashboard`; gradle: `apk/`; github-actions). **SPM (iOS):** not covered by Dependabot — update Xcode packages manually.
- Backups ops checklist: `docs/security/BACKUPS_CHECKLIST.md` (Supabase encryption at rest, PITR, restore drill). Not automated.
- Subprocessors register: `docs/legal/SUBPROCESSORS.md` (Firebase, Google, Apple, FCM, Supabase, optional Sentry).
- Invite polish: `web/invite` favicon.svg, Open Graph tags on `join.html`, robots.txt + sitemap.xml. Privacy/Terms links remain `https://momentra.tech/privacy` and `/terms`.
- **Sentry DSN:** still an ops responsibility — set only in production when ready; not required for this sprint to pass.
- **Superseded (DPDP full track):** soft-delete lock relaxed for principal-owned erasure only.

## Sprint S5 scope (parity, accessibility, Apple on Android)

- **Parity re-check** (`docs/audit/04b-ios-android-parity-S5-recheck.md`): the 2026-09-03 `04-ios-android-parity.csv` listed 95 `ANDROID_ONLY` routes; a fresh scan of `ApiService.kt` (237 routes) vs iOS call sites shows 94 of them are now called on iOS too (telemetry via `BackendTelemetry` direct request). The only one still missing on iOS, `DELETE /v1/moments/{id}`, is no longer called by Android either and **will not be added** (no-delete policy). Newer Android-only routes found: `POST /v1/moments/{id}/stories` -> **closed in S5** (iOS Moment Story viewer Generate / Retry; additive new version, never deletes prior stories); `GET /v1/me/notifications/metrics` -> declared on Android but unused in UI, deferred.
- **Accessibility pass (both platforms):** headings + live status on Account hub; labelled switches; Privacy/Terms links announce "opens in browser/Safari"; delete-confirm explains soft-delete; device Revoke names the device; consent gate age checkbox is a single labelled toggle target (Android) and Continue explains why it is disabled; finance disclaimers read as "Disclaimer: …" with `finance.disclaimer` test tag on both; decorative lock/bullet glyphs hidden; Personal expense Save draft / Confirm are announced as buttons; login error is a polite live region.
- **Sign in with Apple on Android:** client implemented (Firebase `OAuthProvider("apple.com")`) and wired on Login behind `BuildConfig.APPLE_SIGNIN_ENABLED` (default **false**) until the Apple Services ID + key are configured in Firebase. Checklist: `docs/legal/APPLE_SIGNIN_ANDROID.md`.
- **Still open (not S5):** phone auth on iOS; full TalkBack/VoiceOver audit of every shell screen; contrast audit of Business/money tokens; Apple/Google account linking.
- **Superseded (DPDP full track):** `DELETE /v1/me` is principal-owned erasure; shared graphs retained.

## DPDP full compliance track (2026-11-13)

- Age gate raised to **18+**; Privacy/Terms minimum age 18; product not directed at under-18.
- `GET /v1/me/erasure-preflight` + `DELETE /v1/me` (`eraseMe`): leave shared, erase personal, anonymize profile, revoke devices.
- Clients: Account delete preflight UI, confirm copy, local wipe after success.
- Grievance internal target: acknowledge 72h / resolve 14d until Board prescribes.
- **Still locked:** no DROP/TRUNCATE of shared tables or other users’ data; no “fully compliant” marketing until counsel clears DRAFT.
