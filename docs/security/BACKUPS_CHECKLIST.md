# Supabase / production backups checklist (ops)

Manual ops checklist — not automated. Run before major releases and at least quarterly.
**Hard rule:** never wipe or hard-delete production user data for cleanup. Soft-delete (`DELETE /v1/me`) stays soft; retained history must remain recoverable from backups.

## Encryption at rest

- [ ] Confirm Supabase project uses encryption at rest (default on hosted Supabase).
- [ ] Confirm storage buckets for media use encrypted storage / private buckets + signed URLs.
- [ ] Confirm Firebase Auth / Firestore (if used) encryption-at-rest defaults are accepted for your threat model.

## Point-in-time recovery (PITR)

- [ ] In Supabase Dashboard → Database → Backups: note whether **PITR** is enabled (Pro+).
- [ ] Record retention window (e.g. 7 days) and who can restore.
- [ ] If PITR is off: enable scheduled daily backups and document retention days.
- [ ] Document RPO/RTO targets for Momentra (example: RPO ≤ 24h, RTO ≤ 4h — adjust to your SLA).

## Restore drill (do this once, then annually)

1. Pick a non-production restore target (new Supabase project or staging).
2. Restore a recent backup / PITR snapshot into that target.
3. Verify: auth users exist (or Firebase remains source of truth), sample moments/media metadata readable, app can connect with staging secrets.
4. Record date, operator, backup ID/time, pass/fail in this table:

| Date (Asia/Calcutta) | Operator | Backup / PITR point | Target | Result | Notes |
|----------------------|----------|---------------------|--------|--------|-------|
| _TBD_                |          |                     |        |        | First drill pending |

## Access & secrets

- [ ] Dashboard access limited to operators who need restore.
- [ ] Service role / DB passwords rotated if leaked; not committed to git.
- [ ] Dokploy / compose env backups do **not** replace database PITR.

## Out of scope for this checklist

- Automated `pg_dump` cron in-repo
- Hard-wipe jobs, TRUNCATE of user tables, or changing soft-delete behavior
