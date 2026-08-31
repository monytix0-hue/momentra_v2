# Momentra V001–V030 Execution Runbook

## 1. Preflight

- Confirm target environment explicitly: local / development / QA / staging / production.
- Confirm database host/project and region.
- Verify backup/restore procedure before any staging/production execution.
- Verify migration file checksums with `scripts/verify_checksums.sh`.
- Verify no unexpected migration history exists.
- Confirm the application is not pointed at a partially migrated environment.

## 2. Execute in exact order

Run V001 through V030 exactly as listed in `manifest/MIGRATION_ORDER.txt`.

Recommended checkpoints:

- V001–V017: structural database.
- V018–V023: architecture-owned seeds.
- V024–V029: RLS, security, grants/revokes.
- V030: production validation.

Stop immediately on any failed migration. Do not skip forward.

## 3. Expected V030 status before seed blockers are closed

The current PRE-RC seed state is intentionally expected to fail production readiness because Metric Versions and Policy Versions remain DRAFT. Do not weaken V030 to make the package appear ready.

## 4. Development Supabase checks

Validate all of the following with authenticated test identities and service/worker identities:

- `core.user_profile.user_id` equals the effective Supabase `auth.uid()` identity.
- Personal user isolation.
- Group same-Moment active Participant isolation.
- Business active Company Membership isolation.
- No Group/Business entitlement leaks Personal data.
- Approved projection reads work.
- Authenticated direct canonical mutations remain denied.
- Internal schemas remain unavailable to ordinary clients.
- `security.*` helpers do not recurse or expose data.
- Worker roles have only their intended privileges.
- AI worker cannot mutate Personal/Collaboration/Business/Work/Finance canonical state.
- Projection worker can update projections but not canonical domains.
- Service-role/backend execution reauthorizes user scope because privileged Supabase access may bypass RLS.

## 5. Behavioral gates

After engine installation, run the Phase 10.7 suite for negative/RLS paths, Phase 10.8 for concurrency/idempotency and Phase 10.9 for projection rebuild/performance.

## 6. Production execution gate

Production execution is permitted only when:

- Phase 11.8 passes with the exact checksums.
- All P0 blockers are closed.
- V030 reports no P0/P1 release blockers required by the release policy.
- staging rehearsal passes;
- backup/restore is verified;
- app/worker versions are pinned to the database release manifest.

## 7. Failure handling

Before migration commit: allow PostgreSQL transaction rollback.

After an applied migration: do not edit migration history. Use a reviewed forward migration.

If only a projection is defective: disable/rebuild the projection rather than restoring canonical data.

If Analytics formula is wrong: version/activate a corrected Metric Version and recompute affected projections.

If AI is defective: disable AI independently; canonical domains must remain functional.

If canonical financial/security integrity is compromised: freeze affected traffic and evaluate forward repair versus point-in-time restore.
