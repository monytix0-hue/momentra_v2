# Production Go / No-Go Checklist

## NO-GO if any item below is false

- [ ] Exact release checksum set passed clean PostgreSQL dry run.
- [ ] Exact release checksum set passed Development Supabase dry run.
- [ ] Analytics Metric Versions/formulas/thresholds approved and ACTIVE as intended.
- [ ] Governance Policy Versions/rules approved and ACTIVE as intended.
- [ ] Group Quick Add subtype matrix reconciled.
- [ ] Role-Permission matrix reconciled.
- [ ] V030 P0 = 0.
- [ ] Required V030 P1 = 0.
- [ ] Personal isolation tests pass.
- [ ] Group isolation tests pass.
- [ ] Business isolation tests pass.
- [ ] Finance cross-scope/integrity tests pass.
- [ ] Idempotency/concurrency tests pass.
- [ ] Outbox multi-worker tests pass.
- [ ] Projection rebuild passes.
- [ ] Hostinger/Dokploy → Supabase performance gate passes.
- [ ] Backup and restore rehearsal passes.
- [ ] App + worker builds are pinned to this DB release.

If any P0 security, financial or canonical-integrity issue exists: **NO-GO**.
