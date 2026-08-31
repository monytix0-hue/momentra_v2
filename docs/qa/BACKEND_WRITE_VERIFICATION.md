# Backend Write Verification

**Tool:** `npm run qa:verify -- --run-id <RUN> --correlation-id qa-… --expect <kind>`

**Schema:** verifies `finance.*` / personal tables, `audit.audit_record`, **`events.domain_event`** (not platform.domain_event), `events.outbox_event`, `projection.recent_activity`.

## Checks per write

| Check | Status when missing |
|-------|---------------------|
| Canonical record | FAIL |
| Audit | FAIL if correlation present |
| Domain event | FAIL |
| Outbox | FAIL if event exists |
| Projection / Activity | FAIL if moment/note present |
| Expected scope | FAIL on moment mismatch |
| No duplicate | FAIL if count ≠ 1 |
| No cross-Moment write | FAIL if sibling hit |
| Group split sum | FAIL if shares ≠ amount |

## Evidence

Machine JSON lands in `.maestro/reports/<RUN_ID>/backend/`.

**Current certification:** PENDING — no Master Cert run has closed backend proof for all Moments.

Correlation: non-production accepts `qa-[a-z0-9-]{8,64}` plus UUID. Header `X-Maestro-Run-Id` is logged on request lifecycle.
