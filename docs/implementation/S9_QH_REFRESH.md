# S9-QH Refresh — Post PX/GX-1 (V046–V047)

**Date:** 2026-08-29  
**Gate:** QH-REFRESH (before QH wiring / before Master QA)  
**Prerequisite PASS:** PX Personal Precision · GX-1 Settlements · family contracts 9/9 · Android compile

## Why refresh

| Assumption (stale) | Current truth |
|--------------------|---------------|
| Future/Lifestyle/Relationships = heuristic | **LIVE** family precision modules + routes |
| Group Settle = `API_GAP` (501) | **SETTLEMENT_RECORD mapped (V047)**; `POST .../settlements` live |
| Settle CTA = gap messaging only | Finance CTA copy updated; **hub still client-deferred** until QH wiring |

Do **not** run Master QA against the old catalog.

## Refresh checklist

| # | Item | Status |
|---|------|--------|
| 1 | Rebuild certification catalog | DONE — `npm run qa:build-catalog` |
| 2 | Rebuild Quick Add coverage matrix | DONE — [`docs/qa/QUICK_ADD_COVERAGE_MATRIX.md`](../qa/QUICK_ADD_COVERAGE_MATRIX.md) |
| 3 | Reclassify Future/Lifestyle/Relationships | DONE — notes = PX-1/2/3 LIVE; writers point at precision routes |
| 4 | Reclassify Group Settle `API_GAP` → `PASS_CANDIDATE` | DONE — 12× Settle rows; client enablement = QH wiring |
| 5 | Verify Android/iOS destination mappings | DONE — Personal sheets EXACT; Settle → planned `GroupSettlementSheet` / Finance CTA (`FIX` until wired) |
| 6 | Verify precision + settlement routes mounted | DONE — see route proof below |
| 7 | Update S9-QH audit + implementation report | DONE — this doc + linked matrices |
| 8 | Execute QH wiring | **NEXT** — enable Settle hub/sheet; remove `SETTLEMENT_DEFERRED` |

## Route proof (live `router.ts`)

| Family | Route examples |
|--------|----------------|
| Future | `GET .../future-runtime-summary`, `.../future-axis-snapshot`, `.../future-inventory`, `.../future-journey`, `PATCH .../future-profile`; `POST .../future-items` → `future-precision` |
| Lifestyle | `GET .../lifestyle-runtime-summary`, `.../lifestyle-vitality-snapshot`, `.../lifestyle-inventory`, `.../lifestyle-journey`, `PATCH .../lifestyle-profile`; `POST .../lifestyle-activities` → `lifestyle-precision` |
| Relationships | `GET .../relationships-runtime-summary`, `.../relationships-bond-snapshot`, `.../relationships-connections`, `.../relationships-journey`, `PATCH .../relationships-profile`; `POST .../relationship-activities` → `relationships-precision` |
| Group Settle | `POST .../settlements` when `SETTLEMENT_RECORD` mapped (V047) |

## Settle lifecycle QA (required before Master QA close)

Not “CTA visible” — prove:

1. Outstanding before (finance snapshot / obligations)  
2. Settlement write (`POST .../settlements`)  
3. Outstanding after (decrement)  
4. Activity / Pulse / group.finance projection hints  
5. Audit + domain event + outbox  
6. Duplicate-submit / idempotency protection  
7. Cross-Moment rejection  

## Runtime note

Restart TypeScript `npm run dev` after V046/V047 + route mounts before any QH or Maestro run.

## Next

**QH wiring** (step 8): enable Group Settle on Android/iOS hubs + settlement sheet; then Maestro + settlement lifecycle evidence; only then Master QA.
