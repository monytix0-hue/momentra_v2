# Momentra Phase 11.4 — V018–V023 Seed Pack

This directory contains the executable seed migrations for the frozen Momentra PostgreSQL baseline.

## Files

- `V018__taxonomy_seed.sql` — Personal, Group and Business taxonomy / Moment Types
- `V019__capability_seed.sql` — Capability catalogue + Moment-Type capability mappings
- `V020__governance_seed.sql` — Permission, system-role and role-permission seeds
- `V021__consent_catalogue_seed.sql` — Consent Purpose and Data Category seeds
- `V022__analytics_metric_seed.sql` — Metric definitions, DRAFT formula versions, input contracts and dependencies
- `V023__policy_seed.sql` — Governance policy identities and DRAFT default-deny versions
- `SEED_REVIEW_REQUIRED.md` — explicit business-semantics gaps that were not silently invented

## Deterministic IDs

All architecture-owned seed UUIDs are explicit UUIDv5 values and are stable across local/dev/QA/staging/production. Runtime user data continues to use `gen_random_uuid()`.

## Lifestyle regression

The exact Personal lifestyle activity contexts remain physically frozen in V003 as `EXPERIENCE`, `WELLBEING`, `DISCOVERY`, `CREATION`, and `LIFESTYLE`. V018 uses namespaced Moment Type codes where necessary to avoid the V002 `(domain_code, code)` uniqueness collision with Life Operations Wellbeing.

## Production activation caveat

V022 metric versions and V023 policy versions intentionally remain `DRAFT` where exact formulas/thresholds/rule bodies were not available as frozen source values. This is deliberate fail-closed behavior, not an omission masked as production logic.
