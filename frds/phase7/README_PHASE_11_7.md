# Momentra Phase 11.7 Integrated Migration Pack

This folder is the authoritative combined V001–V030 SQL pack assembled from Phase 11.1 through 11.6.

## Status

- Static integration: PASS
- PostgreSQL/Supabase engine execution: NOT YET RUN (Phase 11.8)
- Production RC: BLOCKED

## Why RC is blocked

See `manifest/RELEASE_BLOCKERS.md`. In particular, Analytics metric versions and Governance policy versions intentionally remain DRAFT because exact production formulas/thresholds/rule bodies were not previously recoverable and were not fabricated.

## Important correction

The earlier Phase 11.6 QA summary said V001–V014 contained 155 tables. The authoritative recount is 156. This is a reporting-only correction; no table is missing.

## Execution

Execute only in numeric migration order. Verify `manifest/SHA256SUMS.txt` before Phase 11.8.
