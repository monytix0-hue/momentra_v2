# DPDP erasure + breach intimation runbook (ops)

**Not legal advice.** Aligns with DPDP Act ss.8(6)–8(7) and s.12(3).

Updated: 2026-11-13 (Asia/Calcutta)

## Self-serve erasure (product)

Users delete via Account → Delete account:

1. Client calls `GET /v1/me/erasure-preflight`.
2. If blockers (organizer/owner with other members), user must transfer first.
3. Client calls `DELETE /v1/me` (`eraseMe`) then Firebase user delete; clears local age/consent prefs, PIN, retail parks.

Ops should prefer this path. Do not invent SQL wipe jobs.

## Email intake (residual / disputes)

1. **Channel:** `hello@momentra.app` (or Account → Privacy / Grievance email).
2. **Verify identity** against account email.
3. Confirm `eraseMe` already ran; if not, guide user to in-app delete.
4. Explain shared-graph retention for other participants.
5. **Must not:** DROP/TRUNCATE shared tables; delete another user’s personal data.

## Grievance SLA (internal target until Board prescribes)

- Acknowledge within **72 hours**.
- Aim to resolve within **14 days**.
- Exhaust this channel before Board complaint.

## Breach intimation checklist (s.8(6))

1. **Contain** — revoke keys, rotate credentials, isolate affected systems (see [`docs/security/MOMENTRA_SECURITY_POSITION.md`](../security/MOMENTRA_SECURITY_POSITION.md)).
2. **Assess** — data categories, approximate affected principals, CIA impact.
3. **Notify ops lead** immediately.
4. **Intimate** affected Data Principals and the Data Protection Board in the **form and manner as may be prescribed** (update when rules publish templates).
5. **Document** timeline, scope, and remediation; do not public-post unverified claims.

## Escalation

Counsel / founder for Board notices, exceptional wipe requests, SDF notification (s.10), or child-data incidents.
