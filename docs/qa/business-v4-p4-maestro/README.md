# P4 Maestro — Business V4 operating hub

<!-- Supersedes: ad-hoc trading/restaurant walkthroughs for P4 acceptance evidence.
     Linked from: docs/qa/BUSINESS_V4_P4_MOBILE_GATE.md.
     User: "Use Maestro where supported..." + "Exit gate: P4 passes only when all nine
     Moment types have working Android and iOS journeys against the real V4 backend." -->

## Prerequisites

- Live V4 API (not mocks).
- Seeded Maestro user: `cd backend/typescript && npm run qa:ensure-maestro-user`
- Android emulator or device with debug APK installed.
- Existing setup flows under `docs/qa/trading-device-walkthrough-2026-11-12/maestro/` for company + Trading moment creation.

## Flows in this folder

| Flow | Purpose |
| ---- | ------- |
| `01-operating-hub-pulse.yaml` | Open Business Ready Pulse; assert operating hub (not “Rebuilding”). |
| `02-widget-drilldown.yaml` | Tap a widget; assert Sources panel with coverage label. |
| `03-quick-add-save.yaml` | CREATE tab → typed Quick Add → save → assert Saved confirmation. |
| `04-create-moment-no-selection.yaml` | Create New Moment without selected Moment (not Quick Add). |
| `industry-matrix.md` | Nine-industry PASS/FAIL + evidence paths. |
| `../BUSINESS_V4_P4_QUICK_ADD_INVENTORY.md` | Server Quick Add + required field inventory. |

## Nine-industry matrix

Re-run `01`–`03` after selecting each subtype Moment. Record PASS/FAIL in
`industry-matrix.md` and `BUSINESS_V4_P4_MOBILE_GATE.md`.
User: "All nine industry journeys pass on Android and iOS."
User: "Do not mark P4 PASS based only on compilation, mocks or test scaffolds."

```bash
maestro test docs/qa/business-v4-p4-maestro/
```
