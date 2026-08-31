# Momentra Phase 11.2 — Actual SQL V006–V010

This pack contains the executable baseline PostgreSQL migrations for:

- V006 — Shared Work
- V007 — Cross-Domain Finance
- V008 — Governance
- V009 — Analytics & Deterministic Intelligence
- V010 — Memory / Pattern / Learning / Evidence / Playbook

## Required predecessor migrations

Run V001–V005 from Phase 11.1 first. These files rely on the exact composite keys established there, especially:

- `core.moment(moment_id, domain_code)`
- `personal.personal_moment_context(moment_id, user_id)`
- `collaboration.moment_participant(participant_id, moment_id)`
- `business.business_moment_context(moment_id, company_id)`
- `business.company_membership(company_membership_id, company_id)`
- `business.team(team_id, company_id)`
- `business.vendor(vendor_id, company_id)`
- `business.vendor_contract(vendor_contract_id, company_id, vendor_id)`

## Phase 9.13 / Phase 10 decisions embedded

- One shared Work kernel; no Personal/Group/Business task duplication.
- One Finance Expense kernel with Personal, Group and Business context tables.
- No `master_expense` table.
- Group financial participants are constrained to the same Moment using composite FKs.
- Business Expense Vendor/Contract references remain Company-isolated.
- Financial Account ownership has real User/Company FK anchors.
- Governance uses canonical `PARTICIPANT` scope vocabulary.
- Approval ROLE/USER/SYSTEM step anchors are mutually exclusive.
- Attention lives in `analytics.attention_item`, not Personal.
- Metric formulas are structural/versioned here; actual product formulas and thresholds arrive in V022 seeds.
- `analytics.calculation_run.trigger_event_id` intentionally has no FK yet; V015 adds it after Events exists.
- Memory is cross-domain and evidence-backed; polymorphic evidence sources remain service-validated.

## Important service-level invariants intentionally not implemented as DB triggers

The following require transactional/service validation and are tested later:

- task dependency cycles
- expense-share total equals posted Expense amount where full allocation is required
- settlement allocation cannot exceed outstanding obligation
- invoice payments cannot exceed invoice outstanding amount
- role-assignment polymorphic scope existence/identity
- metric dependency cycles
- learning supersession cycles
- evidence polymorphic source existence

## Execution

Run in version order after V005:

```text
V006__work.sql
V007__finance.sql
V008__governance.sql
V009__analytics.sql
V010__memory.sql
```

Each file uses a transaction and is intended to fail rather than silently mask unexpected schema drift.

## Engine-execution status

The files are statically inspected in this environment. Full PostgreSQL/Supabase engine execution remains part of Phase 11.8.
