# Business V4 — P4 Mobile Integration Gate

<!-- Supersedes: “Next: P4” stub in docs/qa/BUSINESS_V4_BACKEND_GATE.md.
     Linked from: BUSINESS_V4_BACKEND_GATE.md, release checklists.
     User: "Momentra Business V4 — P4 Mobile Integration & End-to-End Verification"
     + "Exit gate: P4 passes only when all nine Moment types have working Android and iOS journeys" -->

## Status (2026-11-23)

**Overall: PASS WITH GAPS — Workstreams A+B closed in code; Workstream C device Maestro still exit blocker.**

Supersedes: 2026-11-22 “PASS WITH GAPS” stub in this file.
Linked from: `BUSINESS_V4_BACKEND_GATE.md`, release checklists.
User: "P4 Completion: Create, Quick Adds and Device E2E"
User: "Do not mark P4 PASS based only on compilation, mocks or test scaffolds."

### Workstream A — Creation and navigation — PASS (code)

- `preferBusinessCreateFlow` separates Create New Moment from CREATE-tab Quick Add (Android + iOS).
- Empty / no-selected-Moment → guided setup host; activation selects Moment and navigates to Pulse.
- Cross-company/Moment nav clears prefer flag; hub only when Ready + company + selected Moment.

### Workstream B — Typed Quick Adds — PASS (code)

- Inventory: `docs/qa/BUSINESS_V4_P4_QUICK_ADD_INVENTORY.md`.
- Typed forms: `TypedOperatingQuickAdd.kt` / `.swift` for all nine subtypes (lines, GST fields,
  parties, eligible-source parents, location, campaign metrics, PRODUCTION reject qty / MTO parent).
- Own Campaign excludes INVOICE/COLLECTION; validation before POST; draft preserved on failure;
  Idempotency-Key; Saved only on server success; refresh selected Moment only; no client finance math.

### Workstream C — Device E2E — PENDING

- Maestro: `docs/qa/business-v4-p4-maestro/` (`01`–`04`, `industry-matrix.md`).
- No Android adb/emulator run in this session; nine-industry matrix remains PENDING.
- Builds/unit tests are **not** C PASS.

### P4.1 — API Contract Freeze — PASS

- OpenAPI: operating / ledger / widgets / sources + `recordOperatingEvent` idempotency.
- Evidence: `npm run openapi:build && npm run openapi:validate`; `tests/business-p4-openapi-widgets.test.ts`.

## Industry matrix

| Moment type | Hub + typed QA (code) | Device Maestro Android | Device Maestro iOS |
| ----------- | --------------------- | ---------------------- | ------------------ |
| Trading / Wholesale | PASS | PENDING | PENDING |
| Distribution / Dealership | PASS | PENDING | PENDING |
| Marketing Client Campaign | PASS | PENDING | PENDING |
| Marketing Own Business | PASS | PENDING | PENDING |
| Retail Store | PASS | PENDING | PENDING |
| Restaurant | PASS | PENDING | PENDING |
| Consumer Service | PASS | PENDING | PENDING |
| Standard Manufacturing | PASS | PENDING | PENDING |
| Made-to-Order Manufacturing | PASS | PENDING | PENDING |

## Exact files changed (completion wave)

- Android: `AppShellScreen.kt`, `TypedOperatingQuickAdd.kt`, `OperatingMomentScreens.kt`,
  `BusinessSliceRepository.kt`, `TypedOperatingQuickAddTest.kt`, `BusinessSetupDocumentKit.kt` (DatePicker API unblock)
- iOS: `AppShellView.swift`, `TypedOperatingQuickAdd.swift`, `OperatingMomentHubView.swift`
- Docs: this gate, `BUSINESS_V4_P4_QUICK_ADD_INVENTORY.md`, `business-v4-p4-maestro/*`

## API contracts

- `GET /business/moments/{momentId}/operating`
- `GET /business/moments/{momentId}/widgets` + `.../widgets/{widgetId}/sources`
- `POST /business/moments/{momentId}/operating-events` + `Idempotency-Key`

## Commands

```bash
cd backend/typescript && npm run openapi:build && npm run openapi:validate
npx tsx --test tests/business-p4-openapi-widgets.test.ts
cd ../../apk && ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests 'com.example.momentra.ui.shell.business.shared.TypedOperatingQuickAddTest'
cd ../momentra && xcodebuild -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 17' build
maestro test docs/qa/business-v4-p4-maestro/
```

## Unresolved gaps (exit blockers)

1. **Device Maestro evidence** for all nine types on Android and iOS vs live V4 (screenshots + logs).
2. No adb/emulator available here; Personal/Group not re-certified this wave.

## Exit gate

P4 **full PASS** only when the nine-industry Maestro matrix is green on Android and iOS against the real V4 backend.
