# RFC v3.1 Functional Audit — Manufacturing / Made-to-Order

**Source:** `Momentra_Manufacturing_MTO_v3_1_Roles_Permissions_Integrated.docx`  
**Date:** 2026-11-18  
**Scope:** Functional / roles honesty only — no layout or design changes.

**Subtypes:** `STANDARD_PRODUCTION` (Own/Standard) · `MADE_TO_ORDER` (MTO) · family `MANUFACTURING_PRODUCTION`.

## Verdict

Core posting boundaries hold on the backend:

- Production ≠ sale; accepted-only finished goods; QUICK clears accepted
- Quote/order ≠ revenue/AR; SALE owns revenue; DELIVERY physical-only
- STANDARD rejects quotation; Hold / non-STOCK transfer refused server-side
- Coarse `canSubmit` / `financeVisibleFor` / maker-checker via shared operating roles

**P0 honesty failures** were client fabricated Hold/Reprint rows and a misleading Open Job “Reserve material” UPDATE — not chrome structure.

**§17 plant roles** (Sales / Procurement / Inventory / Production / QC / Auditor) remain deferred per contract freeze; this pass does not add those role codes.

## Gaps remediated in this pass

| ID | Gap | Fix |
|---|---|---|
| P0-A1 | Hold invents CRC/M8/Paint rows when masters empty | Empty masters → empty / Unavailable (APK + iOS MFG/MTO) |
| P0-A2 | MTO Reprint invents Harbor flange / CRC posted lines | Bind to live SALE activity only; empty when none |
| P0-A3 | Open Job “Reserve material” posts UPDATE as if reserved | Default No reserve; Reserve chip blocks post (ledger Unavailable) |
| P0-A4 | Master BOM forms seed fake CRC / M8 components | Start with empty component list |
| P1-B1 | MTO PRODUCTION without confirmed job allowed | `validateProduction` requires confirmed CUSTOMER_ORDER parent (RP-15) |
| P1-B2 | Invite blurbs hide API role codes | Access copy states `Posts as FINANCE|…` |
| P1-B3 | Missing RP-named acceptance cases | RP-02 / 08 / 11 / 15 (+ Hold refuse) on manufacturing tests |

## Explicitly out of scope

- New §17.1 plant role templates or Moment/job scoped grants
- First-class reservation / material_consumption / quarantine / job-work custody events
- Location-paired TRANSFER stock ledger redesign
- QC provisional → sellable release workflow as separate approval path
- Cost/margin widgets; Pulse shared ring 0% cross-family change
- Hub/sheet/color/typography redesign

## Evidence

- Backend: `operating-manufacturing.ts`, `operating-manufacturing.test.ts` (40 pass incl. RFC v3.1 RP block)
- Clients: MFG/MTO utility, capture, masters (APK + iOS) — Hold/Reprint honesty, Open Job reserve gate, invite API-code copy
- Verify: manufacturing tests 40/40; `:app:compileDebugKotlin` BUILD SUCCESSFUL
