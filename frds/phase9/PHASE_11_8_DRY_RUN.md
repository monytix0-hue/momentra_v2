# Phase 11.8 Dry-Run Procedure — Pending

Phase 11.8 was explicitly deferred. This document defines the exact future execution gate.

## A. Clean PostgreSQL

1. Create an empty PostgreSQL database supported by the target Supabase PostgreSQL version.
2. Verify `pgcrypto` can be installed.
3. Run the exact V001–V030 files in checksum order.
4. Capture stdout/stderr and migration duration per file.
5. Confirm all V001–V029 migrations commit cleanly.
6. Record V030 findings; current DRAFT metrics/policies are expected release blockers until closed.
7. Run schema/catalog checks and compare to the Phase 11.7 manifest.

## B. Development Supabase

1. Use a disposable/approved Development Supabase project.
2. Run the same exact V001–V030 package.
3. Validate `auth.uid()` integration and test users.
4. Validate RLS with Personal, Group and Business personas.
5. Validate grants/revokes and worker memberships.
6. Validate `SECURITY DEFINER` helper behavior and controlled `search_path`.
7. Validate backend/service-role behavior separately from authenticated-client behavior.
8. Run V030 and the Phase 10.7 negative suite.

## C. Pass criteria

- No SQL parser/runtime error in V001–V029.
- No unexpected transaction or FK failure.
- No RLS recursion/security leak.
- No unexpected privilege escalation.
- No cross-user/cross-Moment/cross-Company exposure.
- V030 findings match intentional known blockers only, until blockers are closed.

Any baseline SQL correction discovered before first shared application requires regenerated checksums and a new Phase 11.9 package. After shared application, use a forward migration.
