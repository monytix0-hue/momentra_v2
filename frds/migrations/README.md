# FRDS migrations (Supabase Postgres)

Files are grouped for browsing:

| Bucket | Path | Use |
|--------|------|-----|
| personal | `personal/` | Personal domain schema / RLS / UI gaps |
| group | `group/` | Collaboration / trip / shared-living |
| business | `business/` | Company / ops / runway / team ops |
| shared | `shared/` | Core platform, finance kernel, seeds, analytics, mixed files |

**Apply order** is still `../manifest/MIGRATION_ORDER.txt` (basenames only).  
Runner: `backend/typescript/scripts/migrate.ts` (resolves basename under these buckets).  
Ledger: `public.momentra_migration_ledger` keys on basename — moving files does not re-apply.

```bash
cd backend/typescript
npx tsx scripts/migrate.ts --check-order   # CI guard
npx tsx scripts/migrate.ts --install-only  # apply pending
```
