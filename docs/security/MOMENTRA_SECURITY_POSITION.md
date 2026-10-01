# Momentra security position

Status of this gate: **PASS CANDIDATE**. It becomes **PASS** only after the runtime checks at the end of this document succeed on Android, iOS, and the API. Code inspection does not close the gate.

## Trust model

Momentra is a **server-trusted** application. It is not a zero-knowledge application.

```
Client → TLS/HTTPS → Authenticated API → authorization and scope enforcement → encrypted infrastructure → PostgreSQL
```

Pulse, Life, Memory, notifications, group settlements, and business workflows require the backend to interpret authorized plaintext.

**Promise:** Momentra protects data from unauthorized access.

**Not the promise:** Momentra cannot read your data.

## End-to-end encryption

**E2EE status: `NOT_REQUIRED`.**

Row-level or client-held end-to-end encryption is architecturally incompatible with the current projections. Those projections compute Pulse, Life, Memory, search, shared Moments, settlements, and notifications from authorized plaintext on the server.

A future Private Vault would be a separate contract. Its contents would be excluded from Pulse, Life, Memory intelligence, search, shared Moments, and notifications. That vault is out of scope for this gate. This decision is closed.

## Encryption at rest

Encryption at rest is required at the infrastructure layer: database, object storage, and backups. That is not row-level E2EE and it does not replace authorization.

Launch checklist (infrastructure settings, not application code):

- Supabase/PostgreSQL encryption at rest enabled for the production project.
- The media bucket stays private. Objects are readable only through short-lived signed URLs issued after a scope check.
- Backups are encrypted and access to backup restores is restricted to named operators.
- Production database credentials, the Supabase dashboard, and log access are not shared accounts.

## Balance masking

Hiding balances in the client is shoulder-surfing privacy. It does not protect API responses, the database, screenshots, or a stolen session. Personal money surfaces on Android and iOS apply the same local mask when the user turns it on. Treat that as presentation, not a security control.

## Authorization

The client never authorizes itself. Every object read or write enforces owner, membership, or company role on the server. A caller who changes only `momentId`, `expenseId`, `memoryId`, `attachmentId`, `companyId`, or `participantId` on an otherwise valid request is rejected with 403 or 404 and no protected body.

Signed media URLs are issued only after the same scope check as the parent moment, memory, or company. Download URLs expire (`SIGNED_DOWNLOAD_TTL_SEC`, 15 minutes). The bucket is private. Permanent public URLs are not stored in projections.

## Device and session

PIN verification uses SHA-256 stored in the platform secret store (iOS Keychain, Android Keystore). An iOS PIN saved with the previous non-cryptographic verifier must be set again. Biometrics and auto-lock are local. Logout revokes the current device registration on the API and clears the local session, including the in-memory app-lock unlock. Firebase ID tokens are verified with revocation checking enabled, so a revoked credential fails the next authenticated request.

Account recovery is Firebase / identity-provider recovery only. There is no user-held content key. Losing a phone does not, by itself, destroy financial history or memories.

## Sensitive data hygiene

Production request logs do not record authorization headers. The log redaction helper strips notes, memory text, mood text, amounts, tokens, and signed URLs before a structured field is written. Push copy that would show money, mood, tokens, or signed URLs on the lock screen is replaced with a generic Momentra update.

## Audit

High-value mutations already emit attributable `auditActionCode` records for expenses, settlements, participant role changes, participant removal, and device registration. Device revoke emits `DEVICE_REVOKE`. There is no separate audit product UI.

## Operational access

Production database access, the Supabase dashboard, application logs, and backups are restricted to named operators. Privileged access is attributable to a person.

Support does not have a “view all personal content” console. This gate does not build one. Support answers use the user’s own session or an explicitly scoped, attributable break-glass path defined by operations, not a standing view-everything tool.

## Recovery

Firebase / provider account recovery is the only recovery model. Do not introduce user-held content keys for Pulse, Life, Memory, or money.

## What this change verified

Ran against the API test database and unit tests:

- Log redaction strips notes, memory text, mood text, amounts, tokens, and signed URLs (`tests/security-hygiene.test.ts`).
- Lock-screen push copy for money and mood is generic (same test).
- Signed download TTL is 900 seconds.
- IDOR/BOLA matrix returns 403 or 404 with no protected body for a swapped `expenseId`, `momentId`, mood history `momentId`, media upload scope, expense `attachmentId`, `companyId`, `memoryId`, and `participantId` (`tests/security-gate-idor.test.ts`).

## Runtime verification still required for PASS

Automated checks above do not replace device and production checks.

These checks are still required on devices and a live API before the gate is **PASS**:

- Cross-user, non-member, and wrong-company requests return 403 or 404 with no protected body, including a single-field swap of `momentId`, `expenseId`, `memoryId`, `attachmentId`, `companyId`, and `participantId`.
- PIN, biometric unlock, and auto-lock on background on Android and iOS.
- Logout removes the device the API still trusts, and a revoked Firebase token is rejected on the next request.
- A sample expense, mood, and memory payload does not appear in production logs or crash reports.
- Money and mood push notifications on the lock screen are generic.
- A signed media URL stops working after the short TTL, and no permanent public URL is stored.
- The listed high-value mutations have an audit row with an action code attributable to the actor.
- Production encryption-at-rest and privileged-access settings match the checklist above.

Until those checks pass:

**MOMENTRA PRODUCTION SECURITY GATE — PASS CANDIDATE**
