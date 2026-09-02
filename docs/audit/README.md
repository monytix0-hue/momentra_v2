# Momentra Deployment Audit Artifacts

Authoritative gap register: [`Momentra_Master_Deployment_Gap_Register.xlsx`](Momentra_Master_Deployment_Gap_Register.xlsx)

## Regenerate evidence

```bash
cd backend/typescript
npm run audit:deployment      # 16-area evidence
npm run audit:supplemental    # supplemental gaps beyond 60 root rows
```

## Supplemental re-audit

- [`SUPPLEMENTAL_GAP_REPORT.md`](SUPPLEMENTAL_GAP_REPORT.md) — 25 additional drill-down gaps
- [`SUPPLEMENTAL_GAP_REGISTER.csv`](SUPPLEMENTAL_GAP_REGISTER.csv) — maps to parent register IDs
- [`SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv`](SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv) — routes with no mobile client

## 16-area deliverables

| Area | File | Register gaps |
|------|------|---------------|
| 1 Frozen UI/Widget | `01-frozen-ui-widget-register.csv` | GRP-004, BUS-014, PER-002 |
| 2 UI → API | `02-ui-api-mapping.csv` | PER-002, GRP-006/007, BUS-* |
| 3 OpenAPI/Backend | `03-openapi-backend-reconciliation.csv` | SP-001, GRP-001 |
| 4 iOS/Android parity | `04-ios-android-parity.csv` | SP-013 |
| 5 Canonical ownership | `05-canonical-ownership.csv` | SP-006, BUS-001..013 |
| 6 DDL/Migrations | `06-ddl-migration-report.md` | SP-005, PER-001, GRP-001 |
| 7 Table utilization | `07-table-utilization.csv` | SP-007 |
| 8 Projections | `08-projection-read-models.csv` | SP-009, PER-009, GRP-008, BUS-018 |
| 9 Events/Workers | `09-event-worker-trace.csv` | SP-008, BUS-022 |
| 10 Metrics | `10-metric-formula-register.csv` | SP-011, PER-008, GRP-002, BUS-006/019 |
| 11 Auth/RLS | `11-auth-rls-matrix.csv` | SP-003/004, GRP-005 |
| 12 State machines | `12-state-machine-rules.csv` | SP-012, GRP-001/007 |
| 13 Refresh/Realtime | `13-refresh-realtime.csv` | SP-010, BUS-022 |
| 14 E2E flows | `14-e2e-flow-evidence/` | SP-014, PER-010, GRP-009, BUS-023 |
| 15 NFR | `15-nonfunctional-report.md` | SP-015 |
| 16 Observability | `16-observability-release-gate.md` | SP-016 |

## Consolidated outputs

- `MASTER_GAP_REGISTER.csv` — register export
- `MASTER_GAP_REGISTER_RECONCILED.csv` — 60 gaps with audit evidence status
- `MASTER_GAP_REGISTER_SUMMARY.md` — portfolio summary (frozen)

## Status

**Audit coverage: 100%** — all 16 areas have evidence artifacts.  
**Runtime readiness:** remediation not started (Wave 0 pending).  
**Unknown gaps: 0**
