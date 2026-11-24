# Nine-industry Maestro evidence matrix

Supersedes: PENDING rows in `docs/qa/BUSINESS_V4_P4_MOBILE_GATE.md` industry matrix.
Linked from: `docs/qa/BUSINESS_V4_P4_MOBILE_GATE.md`, `docs/qa/business-v4-p4-maestro/README.md`.
User instruction: "Run Maestro journeys against the real V4 backend for: [all nine types]."
User instruction: "Do not mark P4 PASS based only on compilation, mocks or test scaffolds."

## How to run

Prerequisites: live V4 API, seeded Maestro user, debug app installed, Moment of each
subtype already activated (or use trading/restaurant walkthrough create flows).

```bash
maestro test docs/qa/business-v4-p4-maestro/01-operating-hub-pulse.yaml
maestro test docs/qa/business-v4-p4-maestro/02-widget-drilldown.yaml
maestro test docs/qa/business-v4-p4-maestro/03-quick-add-save.yaml
maestro test docs/qa/business-v4-p4-maestro/04-create-moment-no-selection.yaml
```

Per industry: select/activate that subtype Moment, then re-run 01–03. Capture
screenshots under `docs/qa/business-v4-p4-maestro/evidence/<platform>/<subtype>/`.

## Matrix (fill after device runs)

| # | Moment type | Android hub | Android QA write | Android widget | iOS hub | iOS QA write | iOS widget | Notes |
| - | ----------- | ----------- | ---------------- | -------------- | ------- | ------------ | ---------- | ----- |
| 1 | Trading / Wholesale | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | |
| 2 | Distribution / Dealership | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | |
| 3 | Client Campaign | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | Invoice/collection OK |
| 4 | Own Business Campaign | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | No INVOICE/COLLECTION |
| 5 | Retail Store | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | Channel required |
| 6 | Restaurant | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | Table/covers |
| 7 | Consumer Service | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | SERVICE/APPOINTMENT |
| 8 | Standard Manufacturing | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PRODUCTION |
| 9 | Made-to-Order Manufacturing | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | Parent + reject qty |

## Per-journey checks

For each PASS cell, confirm: create/setup/activation → correct Quick Add catalog →
typed write Saved → Pulse/Moments/Life/Memory refresh → widget source reconciliation →
invalid input errors → permission denied when applicable → retry uses same Idempotency-Key.
