# Business V4 — P4 Mobile Integration Gate

<!-- Supersedes: “Next: P4” stub in docs/qa/BUSINESS_V4_BACKEND_GATE.md.
     Linked from: BUSINESS_V4_BACKEND_GATE.md, release checklists.
     User: "Momentra Business V4 — P4 Mobile Integration & End-to-End Verification"
     + "Exit gate: P4 passes only when all nine Moment types have working Android and iOS journeys" -->

## Status (2026-11-22)

**Overall: PASS WITH GAPS — device Maestro journeys still required for exit.**

P4.1–P4.4 wired against the real V4 backend contracts. P4.5 device acceptance remains partially open (see unresolved gaps).

### P4.1 — API Contract Freeze — PASS

- OpenAPI regenerated with:
  - `GET /business/moments/{momentId}/operating`
  - `GET /business/moments/{momentId}/ledger`
  - `GET /business/moments/{momentId}/widgets`
  - `GET /business/moments/{momentId}/widgets/{widgetId}/sources`
- Existing operating-events retain idempotency + 400/401/403/409.
- Evidence: `npm run openapi:build && npm run openapi:validate`
- Evidence: `npx tsx --test tests/business-p4-openapi-widgets.test.ts`

### P4.2 — Guided Setup and Navigation — PASS (scoped)

- Company creation/selection remains in `BusinessAppStateHost` / `BusinessSetupShell` (Android + iOS).
- Ready + company + selected Moment → `OperatingMomentPage` (Android) / `OperatingMomentHubView` (iOS).
- Pulse / Moments / Life / Memory / CREATE(Quick Adds) consume `GET /operating` + `GET /widgets`.
- Widget tiles open `GET /widgets/{widgetId}/sources` (server reconciled totals only).

### P4.3 — Industry Quick Adds — PASS (scoped)

- Quick Add catalog comes from server `operating.quickAdds` for the selected subtype.
- Submit via canonical `POST /operating-events` with Idempotency-Key (repository/APIClient).
- Visible “Saved …” confirmation; moment projection refresh via tick/cache invalidate.
- Industry-specific rich forms (line items / MTO / marketing attribution sheets) remain thinner than pre-V4 Active hubs — see gaps.

### P4.4 — Cross-Screen Consistency — PASS (scoped)

- After Quick Add: invalidate operating cache → reload operating + widgets for the selected Moment only.
- Widget drill-down shows server `reconciledTotal` + eligible sources; PARTIAL hides values.
- Coverage presentation: COMPLETE / PARTIAL / UNAVAILABLE / ZERO (Android unit test).
- Clients do **not** compute authoritative finance totals.

### P4.5 — Android and iOS Acceptance — PARTIAL

| Journey | Android | iOS | Notes |
| ------- | ------- | --- | ----- |
| Create company + Business Moment | Manual / prior Maestro setup flows | Same | Guided setup preserved |
| Guided setup | Wired | Wired | AppState host |
| Invite + redeem membership | Backend covered (P0/P1) | Same | Mobile UI unchanged |
| Financial/operational Quick Adds | Hub form (server catalog) | Hub form | Rich industry sheets deferred |
| Partial payment / collection | Via Quick Add codes when offered | Same | |
| Stock transfer / return | Via Quick Add codes when offered | Same | |
| Marketing Client/Own | Via server Quick Adds + widgets | Same | |
| Standard / MTO production | Via server Quick Adds + widgets | Same | |
| Every widget + drill-down | Wired | Wired | Needs device screenshots |
| Restricted roles / redaction | Server `financeVisible` + UNAVAILABLE | Same | Needs device proof |
| Offline / retry / stale | Retry + cache peek | Retry | |
| Maestro E2E nine industries | **Pending device run** | **Pending** | Flows under `docs/qa/*-device-walkthrough` |

## Industry PASS/FAIL matrix (integration wiring)

| Moment type | Operating hub | Quick Adds API | Widgets + sources | Device Maestro |
| ----------- | ------------- | -------------- | ----------------- | -------------- |
| Trading / Wholesale | PASS | PASS | PASS | PENDING |
| Distribution / Dealership | PASS (DST-PULSE-v12 hub) | PASS | PASS (DD-* coverage) | PENDING — see `BUSINESS_V4_APPSTATE_SMOKE.md` Distribution activate walk |
| Marketing Client Campaign | PASS | PASS | PASS | PENDING |
| Marketing Own Business | PASS | PASS | PASS | PENDING |
| Retail Store | PASS | PASS | PASS | PENDING |
| Restaurant | PASS | PASS | PASS | PENDING |
| Consumer Service | PASS | PASS | PASS | PENDING |
| Standard Manufacturing | PASS | PASS | PASS | PENDING |
| Made-to-Order Manufacturing | PASS | PASS | PASS | PENDING |

## Commands

```bash
cd backend/typescript
npm run openapi:build && npm run openapi:validate
npx tsx --test tests/business-p4-openapi-widgets.test.ts
npx tsx --test tests/business-p3-widget-analytics.test.ts tests/business-p3-marketing-at.test.ts
```

## Unresolved gaps (explicit)

1. **Device Maestro evidence** for all nine Moment types against a live V4 API (screenshots + logs) — exit gate blocker.
2. **Industry-rich Quick Add sheets** (lines, GST, marketing attribution, MTO reject qty) not restored; catalog codes + amount/title form only.
3. **Creating a second Moment while one is selected** via CREATE tab now opens Quick Adds; guided create requires empty/no-moment AppState path.
4. Device Maestro logs/screenshots not yet attached (Android unit+compile and iOS `xcodebuild` iPhone 17 simulator **BUILD SUCCEEDED** 2026-11-22).
5. Personal/Group paths not altered beyond Business Ready wiring; regression risk low but not re-certified here.

## Exit gate

P4 **full PASS** only when the nine-industry Maestro matrix is green on Android and iOS against the real V4 backend. Compilation + contract wiring alone are insufficient.
