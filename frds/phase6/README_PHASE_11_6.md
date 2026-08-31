# Momentra Phase 11.6 — V030 Production Validation Pack

This directory contains the actual V030 validation SQL compiled against the generated V001–V029 Momentra baseline.

## Behavior
- Non-destructive: V030 creates only a session-local temporary validation table.
- Fail-closed: any failed P0 or P1 check raises an exception.
- Static/data-integrity checks cover structure, canonical domain boundaries, Work, Finance, Governance, Analytics, Memory, AI, Events/Outbox, Platform, Projections, RLS and grants.
- Behavioral multi-session RLS, concurrency/idempotency and performance/rebuild tests remain Phase 10.7–10.9 / Phase 11.8 responsibilities.

## Expected current result
The current Phase 11.4 seed pack intentionally leaves metric versions and policy versions in DRAFT because exact formulas, thresholds and policy rule bodies were not recoverable as frozen values. Therefore V030 is expected to FAIL closed on VAL-ANA-001 / VAL-GOV-005 / VAL-RC-001 / VAL-RC-002 until those semantics are approved and activated.

VAL-RC-003 records the exact historical per-Moment-Type Quick Add subset reconciliation as an external Phase 11.7 release-manifest gate. It does not hard-fail SQL because database state cannot infer prior human product-design intent. Phase 11.7 must still close it before release.

## Important
Do not weaken V030 merely to make deployment green. Close the underlying seed/security/integrity blocker and rerun the same validation.
