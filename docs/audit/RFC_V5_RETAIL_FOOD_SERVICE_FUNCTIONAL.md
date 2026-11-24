# RFC v5 Functional Audit — Retail / Food / Service

**Source:** `Momentra_Retail_Food_Consumer_Integrated_Functional_UI_Analytics_Contract_v5.docx`  
**Date:** 2026-11-18  
**Scope:** Functional / analytics honesty only — no layout or design changes.

**Subtypes:** `RETAIL_STORE` · `RESTAURANT_FOOD` · `CONSUMER_SERVICE` (family `RETAIL_CONSUMER`).

## Verdict

Core money boundaries hold on the backend and existing RF/CS/ALL tests:

- Order/booking ≠ revenue; sale/charge ≠ collection; purchase ≠ payment; stock ≠ expense
- Quick amount ≠ SKU movement; credit needs named customer; GST stamped once
- Food prep/ready ≠ revenue until bill; Service complete ≠ revenue until charge
- Unconfigured target/budget → Unavailable / `configured:false` (not invented 0%)

## Gaps remediated in this pass

| ID | Gap | Fix |
|---|---|---|
| P0-A1 | Food Hold posted invalid `orderStatus=HOLD` | Device-local park (mirror Retail) |
| P0-A2 | Service Hold / staff Transfer POSTed refused writes | Chrome-only dismiss / local only |
| P0-A3 | Android Food Transfer omitted from/to | Send `transferFrom*` / `transferTo*` |
| P1-B1 | Payment readiness `dueBucket` always null | Compute OVERDUE / DUE_SOON / OPEN |
| P1-B2 | Life Money Moving included SALE/PURCHASE | Cash-true row filter |
| P1-B3 | Finance DTO forced `"0.0000"` | Nullable / omit when unknown |
| P2 | Missing M/E2E/LIN acceptance cases | Extended `operating-retail-analytics` tests |

## Explicitly out of scope

Table maps, recipe deduction, cash drawer, server PDF, new widgets, Figma chrome renames.

## Evidence

- Backend: `operating-retail-contract.ts`, `operating-catalog.ts`, `operating-retail-analytics.test.ts` (41 pass incl. RFC v5 block)
- Clients: Restaurant / Service / Retail utility sheets (APK + iOS Hold/Transfer paths)
- Verify: trading projection 15/15; `:app:compileDebugKotlin` BUILD SUCCESSFUL
