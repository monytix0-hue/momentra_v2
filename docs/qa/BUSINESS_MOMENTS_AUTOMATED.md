# Business moments — what is automated vs manual

**Answer:** yes, we can automate the high-ROI wiring and honesty checks. We do **not** claim full E2E sheet certification from this harness.

## One command

```bash
./scripts/qa/run-business-moments-automated.sh
```

Optional: export `DATABASE_URL` (or keep it in `backend/.env`) so capability-map tests run. Android unit tests need JDK 21 (`JAVA_HOME` or `~/.gradle/jdks/eclipse_adoptium-21*`).

## Automated now

| Layer | What | Verticals |
| --- | --- | --- |
| Backend `operating-*.test.ts` | Projection, ledger gates, POS tender, GSTR, trading HOLD/TRANSFER, manufacturing/MTO boards | Retail / Restaurant / Service / Trading / Distribution / MFG / MTO (via family/subtype) |
| Backend `operating-moment-capabilities.test.ts` | Start + shared write caps on every operating Business subtype | All operating types (needs live DB) |
| Android `ui.shell.business.*` | Hub fidelity smokes (captureMode / sheet enum / HOLD·REPRINT honesty), TradingSheetState 18 surfaces, copy + KOT/status semantics | Retail · Restaurant · Service · Trading · Distribution · MFG · MTO |

Fidelity smokes clone `MtoFidelitySmokeTest`: enum codes openable, HOLD ≠ SALE/STOCK write, REPRINT is mode `REPRINT`.

## Still manual / device

| Gap | Why |
| --- | --- |
| Visual Figma parity | Layout, spacing, chrome — screenshot / eye |
| Toast / snackbar copy on device | Compose UI not exercised by JVM unit tests |
| Logcat-only faults | Runtime networking, printers, scanners |
| Full capture → POST → Pulse round-trip | Needs seeded moment + auth on device or Maestro |
| Maestro certification ledger | Flows / fixtures exist under `backend/typescript/scripts/qa/*maestro*`; cert status **PENDING** |

## Do not invent

GST calculators, printer PDFs, or fake GS1 keys are out of scope for this harness. Honesty rules stay: UI may show Unavailable; server stamps what exists.

## Related

- Maestro prep: `npm run qa:prepare-fixtures` / `qa:verify` under `backend/typescript`
- Device screenshots: `scripts/qa/capture-wave1-screenshot.sh`
