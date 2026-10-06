# Launch decisions (Momentra)

Updated: 2026-10-05 (Asia/Calcutta)

## Data deletion policy (locked)

- **No hard-wipe of user, media, or domain data.** Operators and product must not DROP/TRUNCATE/destroy production user data for â€œcleanupâ€ or account-deletion automation.
- **Account deletion = soft-delete only** (`DELETE /v1/me` marks the account deleted / disables Firebase sign-in). Historical moments, media, and related records are **retained**.
- Shared group/business history may remain visible to other participants after a member soft-deletes.
- Access / correction / retention questions: `resolvingpoint@gmail.com`.

## Legal documents

- Privacy Policy and Terms of Service in-repo drafts: `web/legal/privacy.html`, `web/legal/terms.html`.
- Canonical public URLs (apps + invite footer): `https://momentra.tech/privacy`, `https://momentra.tech/terms`.
- **Status:** DRAFT â€” pending lawyer review. Not legal advice.
- Contact / entity email for policies: `resolvingpoint@gmail.com`.

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
- **Still locked:** no hard-wipe / no user-data deletes; `DELETE /v1/me` soft-delete unchanged.



## Sprint S3 scope (age, disclaimer, honesty)

- Age gate (13+) on consent gate for Android + iOS; Continue disabled until confirmed; local flag `momentra_age_gate_accepted_13plus`.
- Consent gate copy lists purposes briefly (account/moments, analytics, AI, memory) aligned with Privacy Policy; `/v1/me/consents` API unchanged.
- Finance disclaimer on Business Money/Runway Pulse and Personal Master Expense: "Not financial, legal, or tax advice."
- Delete-account copy clarifies soft-delete only (devices revoked; moment/domain history may be retained; no hard wipe).
- **Still locked:** no hard-wipe / no user-data deletes; `DELETE /v1/me` soft-delete unchanged; no export endpoint in S3.
- Live policy pages on momentra.tech remain source of truth; in-repo drafts stay DRAFT.

## Sprint S4 scope (ops polish)

- Dependabot: `.github/dependabot.yml` (npm: `backend/typescript`, `admin-dashboard`, `backend/packages/event-contracts`, root; gradle: `apk/`; github-actions). **SPM (iOS):** not covered by Dependabot — update Xcode packages manually.
- Backups ops checklist: `docs/security/BACKUPS_CHECKLIST.md` (Supabase encryption at rest, PITR, restore drill). Not automated.
- Subprocessors register: `docs/legal/SUBPROCESSORS.md` (Firebase, Google, Apple, FCM, Supabase, optional Sentry).
- Invite polish: `web/invite` favicon.svg, Open Graph tags on `join.html`, robots.txt + sitemap.xml. Privacy/Terms links remain `https://momentra.tech/privacy` and `/terms`.
- **Sentry DSN:** still an ops responsibility — set only in production when ready; not required for this sprint to pass.
- **Still locked:** no hard-wipe / no user-data deletes; soft-delete unchanged.
