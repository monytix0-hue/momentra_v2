# Momentra Database Release Pack — Phase 11.9

Release state: **PRE-RC / NOT FOR PRODUCTION EXECUTION**

This package is the authoritative V001–V030 PostgreSQL migration distribution produced from Phases 11.1–11.7. Static integration has passed. Phase 11.8 clean PostgreSQL + Development Supabase engine execution was explicitly deferred and remains mandatory before Production RC approval.

## What is included

- `migrations/` — V001 through V030 in frozen execution order.
- `manifest/` — checksums, release status, blockers, migration order, seed review register.
- `reports/` — Phase 11.7 static integration evidence.
- `runbook/` — dry-run, staging, production, backout and verification procedures.
- `scripts/` — local checksum and ordered-execution helpers. These scripts do not contain credentials.

## Hard release rule

Do not execute this pack against production until all blockers in `manifest/RELEASE_BLOCKERS.md` are closed and Phase 11.8 has passed on clean PostgreSQL and Development Supabase using these exact checksums.

## Frozen architecture protections

The pack preserves:

- one canonical Work kernel;
- one canonical Cross-Domain Finance kernel;
- Personal Master Expense represented by `finance.expense` + `finance.personal_expense_context`;
- Personal user isolation;
- Group Moment/Participant isolation;
- Business Company isolation;
- five Lifestyle contexts: EXPERIENCE, WELLBEING, DISCOVERY, CREATION, LIFESTYLE;
- Analytics-owned Attention;
- governed non-canonical AI;
- transactional Events/Outbox;
- rebuildable read projections;
- RLS + Governance layered security.

## Next mandatory action

Run Phase 11.8. If engine execution reveals a defect, do not edit an already approved/applied migration in-place. Before the first shared-engine application, a corrected baseline may be regenerated and checksummed; after application to a shared environment, corrections must use a new forward migration.
