# Identity Architecture — Firebase → Momentra

**Status:** FROZEN for S0+  
**Date:** 2026-08-26  
**Related:** [CURRENT_IMPLEMENTATION_AUDIT.md](../implementation/CURRENT_IMPLEMENTATION_AUDIT.md)

---

## Canonical identity chain

```text
Firebase ID Token (mobile)
        ↓
TypeScript authMiddleware verifies signature (firebase-admin)
        ↓
decoded.uid + decoded.aud (projectId)
        ↓
provider key = "firebase:<projectId>:<uid>"
        ↓
user_id = UUIDv5(MOMENTRA_IDENTITY_NAMESPACE, provider key)
        ↓
UPSERT core.user_profile (user_id, email, display_name, …)
        ↓
RequestContext { firebaseUid, firebaseProjectId, userId, email, displayName, correlationId, … }
        ↓
Application services + Governance (Momentra user_id only)
        ↓
PostgreSQL canonical rows keyed by Momentra UUID
```

**Implementation references:**

- `backend/typescript/src/platform/auth/uuid.ts` — `firebaseUserId(projectId, uid)`
- `backend/typescript/src/platform/auth/index.ts` — `resolveIdentityFromToken`, `provisionUserProfile`
- `backend/typescript/src/api/middleware/auth.ts` — Bearer / dev bypass → `RequestContext`
- Namespace env: `MOMENTRA_UUID_NAMESPACE` or `MOMENTRA_IDENTITY_NAMESPACE`

Same Firebase project + UID always resolves to the same Momentra `user_id`. Concurrent first-login upserts must not create duplicates (`ON CONFLICT (user_id)`).

---

## Freeze statements

1. **`auth.uid()` is not the Firebase UID.**  
   Supabase `auth.uid()` (when present) returns a UUID from Supabase Auth. Firebase UIDs are opaque strings and are **never** written as `core.user_profile.user_id`.

2. **Mobile never talks to PostgreSQL.**  
   iOS and Android use Firebase Authentication for session + TypeScript `/v1` for all product data. No PostgREST / JDBC / direct Supabase table access for Moments, Finance, or profiles.

3. **Node Governance is the current mobile authorization path.**  
   Personal ownership, Group participation, and Business membership are enforced in TypeScript (`modules/governance/resolver.ts`) using Momentra `user_id` from `RequestContext`. Client-supplied IDs are scope only.

4. **Preserve V024–V029.**  
   Do not rewrite historical RLS migrations. Policies that call `security.current_user_id()` or `security.is_backend_app()` remain as shipped. Silent “fixes” to equate Firebase UID with `auth.uid()` are forbidden.

5. **`SET ROLE momentra_app` is deferred hardening, not S0.**  
   Ideal production connections: LOGIN role that inherits `momentra_app`, so `security.is_backend_app()` is true without BYPASSRLS. S0 does **not** introduce `SET ROLE` or JWT claim bridging. Document only until credentials/grants are proven.

6. **V031–V040 remain preserved.**  
   Forward migrations are part of the live development schema. New additive DDL only after V040 if a proven gap exists. **V030 must never execute** during normal feature development (final production validation gate only, under explicit ops control — tooling skips it).

---

## SQL vs runtime (parallel models)

| Layer | Assumption | Momentra runtime |
|-------|------------|------------------|
| V002 comment | `user_id` equals Supabase `auth.uid()` | `user_id` = UUIDv5(Firebase provider key) |
| V024 `security.current_user_id()` | Prefer `auth.uid()`, else JWT `sub` as UUID | Node does **not** set `auth.uid()` or `request.jwt.*` |
| V025–V028 RLS | Owner = `current_user_id()` OR backend/worker roles | API writes rely on DB role privilege + app Governance |
| V029 | `momentra_app` NOLOGIN group role | Pool user should eventually be a LOGIN member of `momentra_app` |

These models only collide if a **direct** Supabase/PostgREST client is enabled with a JWT whose `sub` is not the Momentra UUIDv5. That path is **out of scope** for mobile.

---

## RequestContext contract

```typescript
interface RequestContext {
  firebaseUid: string;
  firebaseProjectId: string;
  userId: string;          // Momentra UUIDv5 — NEVER Firebase UID
  email?: string;
  displayName?: string;
  momentId?: string;
  participantId?: string;
  companyId?: string;
  correlationId: string;
  roles: string[];         // populated by bootstrap/governance when available
  permissions: string[];
}
```

**Client rules:**

- Never send Momentra `userId` as an authority claim; server derives it from the token.
- On offline restore, never substitute Firebase UID for `userId`. Use last cached Momentra bootstrap identity, or remain restoring/offline without a fake id.
- `firebaseUid` may be stored separately for telemetry/debug; it is not a primary key in PostgreSQL.

---

## Dev auth

Non-production only (`ALLOW_DEV_AUTH` and/or missing `FIREBASE_PROJECT_ID`, never in `NODE_ENV=production`):

- Header `X-Dev-Firebase-Uid` → same UUIDv5 path with project `momentra-dev` (or configured project id).

Production fail-closed: `ALLOW_DEV_AUTH` forbidden; `FIREBASE_PROJECT_ID` and `DATABASE_URL` required.

---

## What S0 must not do

- Do not change V024–V029 SQL.
- Do not execute V030.
- Do not map Firebase UID → `core.user_profile.user_id` as raw string.
- Do not enable mobile → PostgreSQL.
- Do not introduce `SET ROLE` in S0-B.
- Do not invent a second identity table for Firebase linkage in S0 (deterministic UUIDv5 is sufficient).

---

## Later hardening (post-S0)

1. Provision LOGIN role granted `momentra_app`; connect API as that role.  
2. Optionally `SET LOCAL` request settings carrying Momentra `user_id` for defense-in-depth if PostgREST-style access is ever added.  
3. Custom Firebase claims or Admin custom token mapping only if a Supabase Auth bridge is explicitly required.  
4. Production RLS redesign as its own program — not silent feature work.
