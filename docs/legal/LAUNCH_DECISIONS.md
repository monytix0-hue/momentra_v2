# Launch decisions (Momentra)

Updated: 2026-10-05 (Asia/Calcutta)

## Data deletion policy (locked)

- **No hard-wipe of user, media, or domain data.** Operators and product must not DROP/TRUNCATE/destroy production user data for “cleanup” or account-deletion automation.
- **Account deletion = soft-delete only** (`DELETE /v1/me` marks the account deleted / disables Firebase sign-in). Historical moments, media, and related records are **retained**.
- Shared group/business history may remain visible to other participants after a member soft-deletes.
- Access / correction / retention questions: `resolvingpoint@gmail.com`.

## Legal documents

- Privacy Policy and Terms of Service in-repo drafts: `web/legal/privacy.html`, `web/legal/terms.html`.
- Canonical public URLs (apps + invite footer): `https://momentra.tech/privacy`, `https://momentra.tech/terms`.
- **Status:** DRAFT — pending lawyer review. Not legal advice.
- Contact / entity email for policies: `resolvingpoint@gmail.com`.

## Sprint S1 scope (this decision set)

- Wire Privacy / Terms links in Android + iOS Account hub and consent gate.
- Invite `join.html` footer links.
- In-repo draft policy pages + third-party font notices.
- **Out of scope for S1:** ATS changes, security headers, AI key fail-closed, Android `applicationId` rename, age gate, data export endpoint, hard-wipe jobs, any database destructive changes.

## Billing

- No in-app purchases / subscriptions in current scope → refund policy deferred until billing ships.

## Hosting note

- Publish or reverse-proxy `web/legal/*.html` (or equivalent CMS pages) to `momentra.tech/privacy` and `momentra.tech/terms` so the in-app links resolve.
