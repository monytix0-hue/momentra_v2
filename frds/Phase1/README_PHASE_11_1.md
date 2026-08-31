# Momentra Phase 11.1 — Executable PostgreSQL V001–V005

Execution order:
1. `V001__extensions.sql`
2. `V002__core.sql`
3. `V003__personal.sql`
4. `V004__collaboration.sql`
5. `V005__business.sql`

## Frozen implementation assumptions
- `core.user_profile.user_id` is the application identity and is expected to equal Supabase `auth.uid()` for that user. The portable baseline deliberately does not hard-FK to `auth.users`.
- PostgreSQL UUIDs use `gen_random_uuid()` via `pgcrypto`.
- Canonical product domains in this batch are `PERSONAL`, `GROUP`, and `BUSINESS`.
- Personal Attention is not stored in `personal`; it remains Analytics-owned in later migrations.
- Personal Goal/Milestone/Task are not duplicated here; shared Work arrives in `V006`.
- Expense/Budget/Contribution are not duplicated in Personal, Collaboration, or Business; Finance arrives in `V007`.
- Memory is not duplicated in these domain schemas; Memory arrives in `V010`.
- Lifestyle physically permits all five frozen contexts: `EXPERIENCE`, `WELLBEING`, `DISCOVERY`, `CREATION`, `LIFESTYLE`.
- Group subtype context tables use a fixed `group_family` discriminator plus composite FK to prove family correctness.
- Business subtype context tables use a fixed `business_family` discriminator plus composite FK to prove family correctness.
- Company-scoped references use composite keys where practical to prevent cross-company links.

## Phase 9.13 corrections included
- `core.moment(moment_id, domain_code)` unique identity.
- `personal.personal_moment_context(moment_id, user_id)` unique identity.
- `collaboration.moment_participant(participant_id, moment_id)` unique identity.
- `business.business_moment_context(moment_id, company_id)` unique identity.
- `business.team(team_id, company_id)` unique identity.
- `business.vendor(vendor_id, company_id)` unique identity.
- `business.vendor_contract(vendor_contract_id, company_id, vendor_id)` unique identity.
- Contract/SLA Vendor consistency protections.
- No composite tenant FK uses `ON DELETE SET NULL`.

## Validation status
A static syntax-shape/dependency checklist was run in the generation environment. A PostgreSQL server/client is not installed in that environment, so these files have **not yet been executed against a live PostgreSQL engine**. The clean-database PostgreSQL/Supabase compile-and-run validation remains part of Phase 11.8.
