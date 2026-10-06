# Subprocessors / third-party processors (Momentra)

Informational register for Privacy Policy alignment. Not a substitute for legal review.
Updated: 2026-10-06 (Asia/Calcutta)

| Processor | Purpose | Typical data categories | Notes |
|-----------|---------|-------------------------|-------|
| **Firebase (Google)** | Auth (email/Google/Apple/phone where enabled), Crashlytics, Analytics, App Check (if enabled) | Account identifiers, device tokens, crash/analytics events | Primary identity provider |
| **Google** | Sign-In, Play services / FCM | Profile basics from OAuth consent; push tokens | Android + web where used |
| **Apple** | Sign in with Apple | Apple user id, relay email if chosen | iOS (and Android if Apple Sign-In enabled later) |
| **FCM (Firebase Cloud Messaging)** | Push notifications | Device FCM tokens, notification payloads | Android/iOS push |
| **Supabase** | Postgres database, storage (media), optional realtime | Moments, media blobs/metadata, app domain data | Encryption at rest; see `docs/security/BACKUPS_CHECKLIST.md` |
| **Sentry** (optional) | Error monitoring | Stack traces, device/context; may include PII if misconfigured | Ops must set DSN only when intended; scrub before enable |
| **Hosting (Vercel / Firebase Hosting / marketing CDN)** | Invite links, marketing site (`momentra.tech`) | IP, logs, cookies if any | Invite: `web/invite`; legal pages on momentra.tech |

## Not listed as billing processors yet

No Stripe / IAP processor until billing ships (`LAUNCH_DECISIONS.md`).

## Change control

When adding a vendor that processes personal data, update this file and the public Privacy Policy on momentra.tech.
