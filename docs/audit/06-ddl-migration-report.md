# DDL / Migration Audit Report (Area 6)

Generated: 2026-09-01T09:43:12.471610+00:00

## Scope
- Migrations: `frds/migrations/` V001–V055
- Tables discovered: 200
- Register gaps: SP-005, PER-001, GRP-001, BUS-002, BUS-009, BUS-010, BUS-012

## Migration count
- Forward migration files: 55
- Manifest: `frds/manifest/MIGRATION_ORDER.txt`

## Fresh install test
- **Status:** PENDING_ENV — requires PostgreSQL/Supabase dev instance
- **Script:** `backend/typescript/scripts/migrate.ts`

## Upgrade path test
- **Status:** PENDING_ENV

## Audit result
| Check | Result |
|-------|--------|
| Migration order manifest | PASS |
| Forward migrations present | PASS (55 files) |
| Fresh install executed | TEST_GAP — SP-005 OPEN |
| Upgrade executed | TEST_GAP — SP-005 OPEN |
