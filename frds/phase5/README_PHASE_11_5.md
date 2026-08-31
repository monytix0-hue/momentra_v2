# Momentra Phase 11.5 — V024–V029 RLS & Database Security

Generated against the actual V001–V023 baseline.

## Security posture
- `core.user_profile.user_id` is expected to equal Supabase `auth.uid()`; `security.current_user_id()` also supports clean PostgreSQL using JWT GUC fallbacks.
- Direct authenticated client access is intentionally read-only and limited to taxonomy/capability catalogue plus approved `projection.*` tables.
- Canonical writes are backend-mediated through the `momentra_app` group role.
- Personal reads resolve to the owning user. Group reads resolve to an ACTIVE participant in the same Moment. Business reads resolve to ACTIVE Company membership.
- Work, Finance, Memory and Projection policies resolve the canonical domain/scope rather than inventing new ownership.
- Internal group roles are NOLOGIN roles. Production login/service identities must be provisioned separately and granted membership in the appropriate group role.
- AI receives no direct mutation privilege on Personal/Collaboration/Business/Work/Finance. Action execution must return through the application/domain command layer.
- No `FORCE ROW LEVEL SECURITY` is used in this baseline; Phase 11.8 must validate service-role and custom-role behavior before considering FORCE RLS.

## Important deployment note
`CREATE ROLE` requires a migration identity with CREATEROLE. The Supabase migration/admin identity should be used for V029. If an environment prohibits role creation, provision the six NOLOGIN group roles through the platform first, then run V029 with the CREATE ROLE block removed in a new reviewed baseline before RC freeze.

## Group lifecycle decision
Direct Group read RLS uses `moment_participant.status = 'ACTIVE'`. INVITED, LEFT, REMOVED and DECLINED identities receive no active Group access. This follows the safe default frozen in Phase 10.5.
