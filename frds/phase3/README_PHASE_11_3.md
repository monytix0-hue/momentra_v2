# Momentra Phase 11.3 — Actual SQL V011–V017

This pack compiles the frozen Phase 10.3 design into executable PostgreSQL migrations.

## Files
- V011__events.sql — Domain Event, transactional Outbox, consumer state, delivery attempts, dead letters
- V012__audit_platform.sql — Audit + idempotency + distributed locks + jobs + checkpoints
- V013__ai.sql — governed AI context, inference, insights, recommendations, action proposals, provenance
- V014__projection.sql — rebuildable Personal/Group/Business read models, Finance snapshots, Available Actions, Life360
- V015__integration_constraints.sql — deferred cross-schema event FKs
- V016__technical_functions_triggers.sql — technical updated_at trigger only
- V017__indexes_finalize.sql — final cross-domain hot-path indexes

## Frozen implementation choices
- Domain Event and Outbox are explicit transactional application writes; there is no generic outbox trigger.
- Event delivery is designed for at-least-once processing; consumers deduplicate by (consumer_code, domain_event_id).
- Audit is separate from Domain Event.
- AI remains non-canonical and Action Proposal execution must return to the ordinary governed domain command path.
- Projection tables are disposable/rebuildable and never own canonical business truth.
- business_moments uses RESTRICT for the composite (team_id, company_id) FK, avoiding the Phase 9.13 composite SET NULL defect.
- Projection source-event references are added late in V015 after events exists.
- V016 only maintains updated_at; it deliberately does not implement domain workflows, analytics, memory, AI, projection, authorization, or outbox publication.

## Execution order
Run only after V001–V010, then execute V011 through V017 in order.

## Validation status
Static object/reference checks are performed as part of Phase 11.3 packaging. Full PostgreSQL/Supabase execution is reserved for Phase 11.8.
