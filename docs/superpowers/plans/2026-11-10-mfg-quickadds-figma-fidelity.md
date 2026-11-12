# Manufacturing QuickAdds Bottom-Sheet Figma Fidelity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all **31** Manufacturing Figma frames (masters, forms, utilities, browsers, pickers, money/ops captures, production) on **both iOS SwiftUI and Android Compose** so chrome, section order, field labels, chips, Why copy, and CTAs match Figma while using the Manufacturing amber/zinc palette — then apply the **same sheet layout system** to Consumer Service with the ServicesTeal / SVC palette as a follow-on phase.

**Architecture:** Mirror the Retail Rose fidelity stack (`RetailSheetChrome` + dedicated masters / utilities / capture / POS modules) for Manufacturing with **vertical tokens** instead of Rose. Introduce `MfgSheetChrome` (Amber/Zinc primitives parallel to `Rose*` / `Teal*`). Route MFG hub tiles through a dedicated MFG sheet router rather than leaving money/ops on generic `OperatingCaptureSheet` forever. Reuse existing `ManufacturingCapture` / operating-event post paths and capability gates. Do **not** invent GST engines, park sync APIs, or print stacks. After MFG lands, clone or parameterize the same chrome pattern for SVC (`TealSheetChrome` already exists — deepen capture bodies to MFG-proven layout).

**Tech Stack:** SwiftUI (iOS), Jetpack Compose (Android/APK), Figma file `TzLvwVwlPbeVB8ug1zB3GM` as visual SoT. Tokens: `design/tokens/business.tokens.json` → `palettes.manufacturing` / `palettes.services`.

**Spec / audit baseline:** Figma inventory via MCP `get_metadata` on 2026-11-10 (session `2d3b3154-3a45-499d-80f8-5f80aec1c1bb`). Template: `docs/superpowers/plans/2026-11-10-retail-quickadds-figma-fidelity.md`. Related: `2026-11-10-service-flow-22-screens.md` (SVC screen list already scoped; this plan’s SVC phase is the **layout-system parity** follow-on after MFG).

**Demo brand in Figma:** Forge & Form · “Plan. Make. Fulfil.” — replace with live moment title/tagline in product; keep MFG copy semantics.

---

## 1. Figma inventory (all 31)

| # | nodeId | Frame name | Sheet type | Maps to existing code? |
|---|--------|------------|------------|------------------------|
| 1 | `2545:29335` | MFG-MASTER · Invite & QR | Masters list | **Partial** — Android/iOS Operating masters Invite; needs MFG chrome + Forge roles (Owner/Manager/Supervisor/Storekeeper/Operator) |
| 2 | `2545:29419` | MFG-MASTER · Vendors & buyers | Masters list | **Partial** — `OperatingMasters` vendors; MFG copy Vendors & buyers + Preferred/Approved badges |
| 3 | `2545:29477` | MFG-MASTER · Products & BOM | Masters list | **Partial** — Parts browser / catalogue; needs BOM rows + Raw/WIP/Finished/Consumables/Spares chips |
| 4 | `2545:29557` | MFG-MASTER · Lines & stores | Masters list | **Partial** — Locations/stations; Line/Station/Store capacity rows (Busy/Open) |
| 5 | `2545:29614` | MFG-FORM · New / Edit Vendor | Master form | **Thin / shared** — Retail Supplier form is closest; MFG supplies chips (Steel/Raw/Finished/Spares/Consumables/Job work) |
| 6 | `2545:29705` | MFG-FORM · Add / Edit Line or Store | Master form | **Thin / shared** — Location form; Type Store\|Line\|Station; Job-card printer chips |
| 7 | `2545:29784` | MFG-MASTER · Create Part / SKU | Master form | **Partial** — Parts master; sell price hero, HSN, GST chips, drawing/revision, batch/heat |
| 8 | `2545:29909` | MFG-MASTER · Create BOM / WO item | Master form | **Missing dedicated** — BOM master (routing chips, scrap %, backflush vs manual issue) |
| 9 | `2547:29335` | MFG-QA-HOLD · Work order hold | Utility | **Partial** — HOLD via `OperatingCaptureSheet`; needs WO hold slip chrome (no stock/GST) |
| 10 | `2547:29449` | MFG-QA-TRANSFER · Stock / WIP transfer | Utility | **Partial** — TRANSFER capture; needs slip + same-GSTIN valuation note |
| 11 | `2547:29539` | MFG-QA-REPRINT · Reprint invoice / job card | Utility | **Partial** — REPRINT; read-only duplicate; never new sale |
| 12 | `2547:29629` | MFG-SHEET · Parts browser (SO / WO) | Browser | **Partial** — `OperatingPartsBrowser` / Catalogue; Add into SO/WO |
| 13 | `2547:29687` | MFG-SHEET · Materials browser (PO) | Browser | **Partial / thin** — materials + on-hand for PO; + New BOM CTA |
| 14 | `2549:29336` | MFG-PICKER · Date · Forge & Form | Picker | **Shared / latent** — date modal; theme with MFG accent |
| 15 | `2549:29460` | MFG-PICKER · Month · Forge & Form | Picker | **Shared / latent** — month/budget period |
| 16 | `2549:29517` | MFG-PICKER · Date & time · Forge & Form | Picker | **Shared / latent** — dispatch/due slots |
| 17 | `2549:29591` | MFG-SHEET · Switch Moment | Shell sheet | **Exists** — moment switcher; ensure MFG Current badge when active |
| 18 | `2396:30095` | MFG-QA-01 · Record Sale | Money capture | **Wired shared** — SALE via Operating capture; amount-first Quick amount \| Itemized \| Return \| Reprint |
| 19 | `2396:30166` | MFG-QA-02 · Record Purchase | Money capture | **Wired shared** — PURCHASE; Materials \| Consumables |
| 20 | `2396:30237` | MFG-QA-03 · Record Collection | Money capture | **Wired shared** — COLLECTION; AR allocate |
| 21 | `2396:30297` | MFG-QA-04 · Record Payment | Money capture | **Wired shared** — PAYMENT; AP settle |
| 22 | `2396:30357` | MFG-QA-05 · Record Expense | Money capture | **Wired shared** — SPEND; Type/Payee/Job link/Period |
| 23 | `2396:30417` | MFG-QA-06 · Stock Update | Ops capture | **Wired shared** — STOCK; Set/Add/Reduce/Transfer |
| 24 | `2396:30483` | MFG-QA-07 · Return / Credit Note | Money capture | **Wired shared** — RETURN; Customer \| Supplier return |
| 25 | `2396:30548` | MFG-QA-08 · Budget / Target Revision | Ops capture | **Wired shared** — BUDGET/TARGET; Sales basis \| Accepted units |
| 26 | `2396:30613` | MFG-QA-09 · Post Update / Issue | Ops capture | **Wired shared** — UPDATE/ISSUE |
| 27 | `2396:30679` | MFG-QA-10 · Approval / Reimbursement | Ops capture | **Wired shared** — APPROVAL/REIMBURSE |
| 28 | `2396:30737` | MFG-QA-11 · Order | Commitment | **Wired shared** — ORDER; SO \| PO \| Work order \| Delivery chips; commitment only |
| 29 | `2396:30804` | MFG-QA-12 · Delivery | Ops capture | **Wired shared** — DELIVERY; Incoming \| Outgoing |
| 30 | `2396:30862` | MFG-QA-13 · Record Production | Production | **Wired shared** — PRODUCTION; Batch/MTO/Rework/Hold WO; accepted≠rejected |
| 31 | `2396:30932` | MFG-QA-14 · Rework / Scrap / Job Work | Production | **Wired shared** — REWORK; distinct events, no silent overwrite |

**Counts by type:** Masters list 4 · Master forms 4 · Utilities 3 · Browsers 2 · Pickers 3 · Shell 1 · Money/ops/production captures 14 = **31**.

---

## Global Constraints

- **Manufacturing-first** — these 31 nodes are the MFG ship gate. SVC is Phase S follow-on (same layouts, teal palette); do not block MFG on SVC completion.
- **Both platforms in parity** — every sheet task lands iOS + Android in the same task (or paired A/B that cannot merge until both pass).
- **Vertical tokens, not Rose** — MFG uses Manufacturing amber/zinc; never hard-code RetailRose / ServicesTeal into MFG sheets.
- **Reuse event posting + capability gating** — `ManufacturingCapture` / `ManufacturingEventDraft` / operating-events / `requires` locks stay authoritative.
- **Do NOT invent:** fake GST computation, PDF/thermal print stack, park/hold sync backend, job-work challan engine, BOM explosion server if missing.
  - GST / tax rows → **Unavailable** or label-only “Est.” from master rate chips; Figma Why copy repeatedly: GST posts on Sale invoice / GRN only — never on SO/WO/PO/BOM save.
  - Hold WO → **no stock, no cost, no GST**; resume same WO.
  - Transfer → stock/WIP move at cost; **no revenue**; same-GSTIN note; job work out needs challan (chrome + locked if API absent).
  - Reprint → **read-only** re-render; never new SALE / AR.
  - Order (SO/PO/WO) → **commitment only**.
  - Production → only **accepted qty** becomes sellable; rejected/rework distinct.
- **Latent risk:** generic `OperatingQuickAddHubView` / shared `OperatingCaptureSheet` remains for other verticals. MFG production path should prefer dedicated MFG modules once Phase 0–1 land; leave shared path intact for Trading/Distribution.

## Review Focus

1. **Fake GST** — never post computed CGST/SGST/IGST from invented rates; Unavailable / Est. label only.
2. **Hold / Transfer / Reprint gates** — Hold no stock/cost; Transfer no sale; Reprint no new invoice number / revenue.
3. **Production honesty** — accepted vs rejected/scrap never silently overwrite FG stock.
4. **Cross-platform drift** — same fields, chips, titles, CTAs on iOS and Android after each phase.
5. **Create/Edit mode** — Edit existing must hydrate or lock Unavailable; empty Edit that creates duplicates is a bug.

---

## 2. MFG color palette / token mapping

### Source of truth

| Layer | Path |
|-------|------|
| Design tokens JSON | `design/tokens/business.tokens.json` → `palettes.manufacturing` |
| Android (today) | Local `Mfg*` colors in `ManufacturingActive.kt` + `OperatingV12Theme.manufacturing()` |
| Android (target) | New `ManufacturingAmberTokens.kt` `object ManufacturingAmber` (or `MfgTokens`) paralleling `RetailRose` / `ServicesTeal` |
| iOS (today) | `BusinessTokens.Colors.manufacturing*` + xcassets `BusinessManufacturing*` |
| iOS (target) | `MfgSheetChrome` / `AmberSheetChrome` primitives consuming `BusinessTokens` manufacturing colors |

### Token table (use these exact values)

| Token | Hex | Use |
|-------|-----|-----|
| `canvas` / `MfgBg` | `#18181B` | Screen / sheet canvas end |
| `canvasTop` | `#3F3F46` | Pulse gradient start / notice border family |
| `canvasDeep` | `#09090B` | Deep scrim |
| `surface` / `MfgCard` | `#27272A` | Cards / rows |
| `inset` / `MfgField` | `#18181B` | Inset fields |
| `accent` / `MfgBrand` | `#F59E0B` | Accent, grabber, selected chips, CTA |
| `accentSoft` / `MfgSoft` | `#FBBF24` | Soft accent / secondary highlight |
| `accentDeep` | `#B45309` | Hero end / pressed |
| `onAccent` | `#1C1917` | Ink on amber CTA |
| `heroStart` → `heroEnd` | `#F59E0B` → `#B45309` | Hub hero gradient |
| Primary ink | `#FFFFFF` / zinc-50 | Titles |
| Body / secondary | `#A1A1AA` | Subtitles, meta |

**Chrome primitive prefix (target):** `MfgGrabber`, `MfgSheetHeader`, `MfgModeChip`, `MfgAmountHero`, `MfgSectionCard`, `MfgLinesCard`, `MfgTotalsCard`, `MfgPickerRow`, `MfgUnavailableGstRow`, `MfgStickyFooter`, `MfgWhyNote`, `MfgSavedToast` — same API shape as Rose/Teal.

### SVC palette (follow-on — already in repo)

| Token | Hex |
|-------|-----|
| `ServicesTeal.teal` | `#0D9488` |
| `ServicesTeal.lime` | `#84CC16` |
| canvas mid / deep | `#042F2E` / `#022C22` |
| card | `#134E4A` |

iOS: `TealSheetChrome` already exists under `Service/ServiceSheetChrome.swift`. Android: `ServicesTealTokens.kt` + partial Teal chrome in `ServiceStoreKit.kt`.

---

## Open product decisions (D1…)

| ID | Decision | Options | Default if product silent | Blocks |
|----|----------|---------|---------------------------|--------|
| **D1** | MFG chrome strategy | **A)** Dedicated `MfgSheetChrome` clone of Rose API. **B)** Parameterized `VerticalSheetChrome(tokens:)` shared by Rose/Teal/Amber. | **A** for ship speed (match Retail pattern); extract B later if churn hurts | Phase 0 |
| **D2** | Hub structure vs V12 | **A)** Keep `OperatingV12HubScreen` plant tiles, only swap sheet bodies to MFG chrome. **B)** Migrate hub to `RetailFamilyHubScreen`-style Core·9 + Support·5 + Utilities like Retail/Service. | **A** first (content fidelity); optional B polish | Phase 1 hub wiring |
| **D3** | Sale Itemized vs Quick amount | **A)** Mode chips on one Record Sale sheet (Figma). **B)** Separate POS module like Retail Scan cart. | **A** — MFG Figma is amount-first + Itemized chip, not full POS cart | Phase 5 |
| **D4** | GST rows | Unavailable vs decorative Est. from HSN chip | **Unavailable** for posted math; Est. OK on Order/BOM totals if labeled “on invoice/GRN” | Phases 3–6 |
| **D5** | Print / job-card / invoice print | Chrome-only vs SDK | Chrome-only + locked secondary | Utilities + Sale |
| **D6** | Create BOM API | Full BOM lines + scrap % vs master stub with UI-local components | Prefer existing parts/BOM write if present; else Save BOM posts supported fields only + UI-local component list | Phase 2 |
| **D7** | Hold WO persistence | Session-only vs server HOLD event | Prefer existing HOLD/`ORDER` event if already posted; never invent stock moves | Phase 3 |
| **D8** | SVC follow-on timing | Same PR train after MFG Phase 6 vs separate milestone | Separate milestone after MFG acceptance | Phase S |
| **D9** | Switch Moment / pickers in MFG plan | Include themed polish vs defer to shell parity plan | Include light token pass in Phase 0; deep calendar polish can defer | Phase 0 / 7 |
| **D10** | Master Create/Edit | Full hydrate vs Edit locked Unavailable | Prefer hydrate if update API exists; else lock Edit | Phase 2 |

Mark each chosen option in the PR description when executing.

---

## File map (anchors → targets)

### Android — today

- `apk/.../manufacturing/ManufacturingActive.kt` — shell + local `Mfg*` colors
- `apk/.../manufacturing/ManufacturingQuickAdds.kt` — hub + `ManufacturingCapture` → `OperatingCaptureSheet`
- `apk/.../shared/OperatingV12Hub.kt` / `OperatingV12Kit.kt` — plant hub
- `apk/.../shared/OperatingQuickSheet.kt` — shared capture
- `apk/.../shared/OperatingMastersSheet.kt` — masters
- `apk/.../service/*` — SVC reference (hub + HTR + masters already closer to Retail)

### Android — create / expand

- `apk/.../theme/ManufacturingAmberTokens.kt` — `object ManufacturingAmber`
- `apk/.../manufacturing/MfgSheetChrome.kt` — shared primitives
- `apk/.../manufacturing/MfgMasterForms.kt` — Vendor, Line/Store, Part/SKU, BOM
- `apk/.../manufacturing/MfgMastersSheet.kt` — list sheets (Invite, Vendors, Products & BOM, Lines & stores)
- `apk/.../manufacturing/MfgUtilitySheets.kt` — Hold / Transfer / Reprint
- `apk/.../manufacturing/MfgBrowsers.kt` — Parts + Materials browsers
- `apk/.../manufacturing/MfgCaptureSheets.kt` — QA-01…14 bodies
- `apk/.../manufacturing/MfgPickers.kt` — Date / Month / DateTime themed (or shared picker + amber tokens)
- Modify: `ManufacturingQuickAdds.kt` — router: utilities → `MfgUtilitySheets`; masters → MFG masters; else `MfgCaptureSheets`

### iOS — today

- `OperatingExperienceHost.swift` — MFG kit → `OperatingActiveView` + `OperatingHubScreen`
- `OperatingV12HubScreen.swift` / `OperatingV12Kit.swift` — plant tiles
- `Sheets/OperatingCaptureSheet.swift`, `OperatingMastersView.swift`, `OperatingPartsBrowserView.swift`
- Service reference: `Service/ServiceSheetChrome.swift` (`TealSheetChrome`), `ServiceHoldTransferReprint.swift`, `ServiceMasterForms.swift`

### iOS — create / expand

- `Shell/BusinessActive/Manufacturing/MfgSheetChrome.swift`
- `…/Manufacturing/MfgMasterForms.swift`
- `…/Manufacturing/MfgMastersViews.swift`
- `…/Manufacturing/MfgUtilitySheets.swift`
- `…/Manufacturing/MfgBrowsers.swift`
- `…/Manufacturing/MfgCaptureSheets.swift`
- `…/Manufacturing/MfgKit.swift` — hub host / tile → sheet router (or extend OperatingExperienceHost)
- Modify: `OperatingExperienceHost.swift` — MFG HOLD/TRANSFER/REPRINT → MFG utilities; Catalogue → Parts browser; capture → MFG capture modules

### Shared tokens

- `design/tokens/business.tokens.json` — already has manufacturing; regenerate BusinessTokens if adding sheet-specific keys (prefer reuse existing)

---

## Phase 0 — Tokens + MfgSheetChrome (prerequisite)

**Goal:** Amber token object + chrome primitives exist on both platforms so later phases do not invent one-off widgets.

### Task 0.1: ManufacturingAmber tokens

**Files:** Create `ManufacturingAmberTokens.kt`; confirm iOS `BusinessTokens.Colors.manufacturing*` coverage; optional regenerate from JSON.

- [ ] **Step 1:** Add Android `object ManufacturingAmber` with hex table above (+ typography helpers if Teal/Rose pattern has them).
- [ ] **Step 2:** Point `ManufacturingActive` / V12 manufacturing theme reads at shared tokens (no behavior change).
- [ ] **Step 3:** Compile APK theme module + iOS Design target.
- [ ] **Step 4:** Commit `chore(mfg): ManufacturingAmber tokens for QuickAdds fidelity`

### Task 0.2: MfgSheetChrome primitives

**Files:** `MfgSheetChrome.kt` / `.swift`

- [ ] **Step 1:** Port Rose primitive set renamed to `Mfg*` (grabber, header, mode chips, amount hero, section/lines/totals cards, picker row, more details, why note, sticky footer, locked chip, Unavailable GST row, saved toast).
- [ ] **Step 2:** Wire colors exclusively from ManufacturingAmber / BusinessTokens manufacturing.
- [ ] **Step 3:** Smoke a placeholder sheet compiling on both platforms.
- [ ] **Step 4:** Commit `chore(mfg): MfgSheetChrome primitives`

**Acceptance:** Later phases import chrome only; no one-off amber hex in capture files.

---

## Phase 1 — Wire MFG hub router (both platforms)

**Goal:** Hub tiles open dedicated MFG sheets (even if bodies are still thin wrappers), not only generic Operating capture forever.

### Task 1.1: Android router

**Files:** `ManufacturingQuickAdds.kt`

- [ ] **Step 1:** Split presentation: HOLD/TRANSFER/REPRINT → `MfgUtilitySheets`; Invite/Vendors/Parts/Lines → masters; PRODUCTION/REWORK/SALE/… → `MfgCaptureSheets` (may initially wrap existing OperatingCaptureSheet with MFG chrome shell).
- [ ] **Step 2:** Keep `ManufacturingCapture` enum + draft `toManufacturing()` posting.
- [ ] **Step 3:** Role-lock tiles stay visible with “Role locked”.
- [ ] **Step 4:** Commit `feat(mfg): QuickAdds hub routes dedicated MFG sheets`

### Task 1.2: iOS router

**Files:** `OperatingExperienceHost.swift` + new `MfgKit.swift` (or Manufacturing hub host)

- [ ] **Step 1:** Mirror Android routing for manufacturing kit.
- [ ] **Step 2:** Catalogue → Parts browser; Materials entry from PO context.
- [ ] **Step 3:** Commit `feat(mfg): iOS host routes MFG utilities/captures`

**Acceptance:** From MFG QuickAdds, every Figma-mapped tile opens an amber-chrome sheet (content may still be partial until later phases).

---

## Phase 2 — Masters lists + forms

**Priority:** Vendor form → Part/SKU → Line/Store → BOM → list sheets (Invite, Vendors, Products & BOM, Lines & stores).

### Field inventories (Figma)

**Vendor `2545:29614` — “New vendor”**
- Create | Edit existing
- Vendor name; Legal/trade name; GSTIN (+ helper)
- GST registration: Regular | Composition | Unregistered
- Phone; Email optional
- Supplies chips: Steel | Raw | Finished | Spares | Consumables | Job work
- Payment terms; Address
- Status: Active | Preferred | Inactive
- Why + CTA `Save vendor`

**Line/Store `2545:29705` — “Add line / store”**
- Create | Edit; Name; Type: Store | Line | Station
- Job-card printer: Shop printer 1 | Label printer | Tablet only (D5 UI-only)
- Display order; Holds stock chips (Raw/WIP/Finished/Consumables/Spares)
- Line supervisor optional; Status Active | Paused
- CTA `Save line / store`

**Part/SKU `2545:29784` — “Create part / SKU”**
- Create | Edit; context chips (Forge & Form ERP / Parts master / GST-aware)
- Part name; Category Raw/WIP/Finished/Consumables/Spares; Part type Finished/Raw/WIP
- Sell price hero; Standard cost; GST on part 18%/5%/Exempt; HSN/SAC
- Default store chips; Unit nos/kg/set; Drawing/revision; Track batch/heat; Active|Inactive
- CTA `Save part` — GST note: posts on invoice only

**BOM `2545:29909` — “Create BOM”**
- Finished part; Routing Press/Weld/Paint/QC/Pack/Dispatch
- Output UOM; Rolled-up cost hero; GST on FG chips (informational)
- HSN (FG); Issue from; Batch size; Default line
- Backflush on accept | Manual issue; Active|Inactive
- CTA `Save BOM` — stock moves only on issue/accepted production (D6)

**Lists:** Invite & QR (roles Owner/Manager/Supervisor/Storekeeper/Operator); Vendors & buyers; Products & BOM (+ New part / + New BOM); Lines & stores (+ Add line / store).

### Task 2.1–2.4: Forms then lists

- [ ] **Step 1:** Implement Vendor + Line/Store + Part forms to inventory (both platforms); Create/Edit per D10.
- [ ] **Step 2:** Implement BOM form per D6.
- [ ] **Step 3:** List sheets with search, badges, + New CTAs opening forms.
- [ ] **Step 4:** Invite & QR MFG roles + copy; join ≠ approval ≠ payment.
- [ ] **Step 5:** Device check; commits per form: `feat(mfg): <name> master Figma field parity`

**Phase 2 acceptance:** Section order matches inventories; saves use existing master write paths; no GST posted on master save.

---

## Phase 3 — Utilities (Hold / Transfer / Reprint) + browsers

### Hold `2547:29335`
- Cross-nav Hold | Transfer | Reprint; line chips; context chips
- Hold slip hero (WOH-id, WO title, component count)
- Lines with Raw/WIP/Consumables + Held chips
- Meta: Notify; Held by; Linked SO
- CTA `Hold WO · WOH-…` — no stock/cost (D7)

### Transfer `2547:29449`
- Transfer slip; from → to; valuation at standard cost; Sale impact None
- CTA `Confirm transfer · N items`

### Reprint `2547:29539`
- Tax invoice / job card hero; Posted lines; Original print; Reprint reason; Payment meta
- CTA `Reprint · duplicate copy` — same number (D5 chrome)

### Browsers
- Parts (`2547:29629`): search/scan, category chips, Add, + New part/SKU
- Materials (`2547:29687`): on-hand meta, + New BOM

### Task 3.1–3.3

- [ ] **Step 1:** `MfgUtilitySheets` Hold/Transfer/Reprint with cross-nav.
- [ ] **Step 2:** Enforce gates in Review Focus.
- [ ] **Step 3:** Parts + Materials browsers density; commit `feat(mfg): Hold/Transfer/Reprint + browsers Figma chrome`

**Acceptance:** Hold never posts stock; Reprint never posts SALE; browsers can Add into parent draft.

---

## Phase 4 — Money captures (Sale → Purchase → Collection → Payment → Expense → Return)

Reuse amount-first pattern from Figma QA-01…05, 07. Map GST rows per D4.

| Sheet | Modes / key fields | CTA |
|-------|--------------------|-----|
| Sale | Quick amount \| Itemized \| Return \| Reprint; Customer; Sale date; Channel; Payment state; GST | Post sale |
| Purchase | Materials \| Consumables; Supplier; Receive now; Payment state; Link job | Post purchase |
| Collection | Customer; Source invoice; Method; Reference; Received on | Post collection |
| Payment | Payee; Source; Method; Reference; Paid on | Post payment |
| Expense | Type; Payee; Job link; Payment state; Period | Post expense |
| Return | Customer return \| Supplier return; Party; Source; Restock; Reason; Credit note date | Post credit note |

### Task 4.1–4.6

- [ ] **Step 1:** One task per sheet, both platforms, Why copy verbatim from Figma.
- [ ] **Step 2:** Collection/Payment must allocate only — never create new SALE/PURCHASE.
- [ ] **Step 3:** Commits `feat(mfg): <Sheet> Figma field parity`

**Acceptance:** Draft preserved on picker open (footer copy); GST honesty; posts via existing event types.

---

## Phase 5 — Ops / commitment / production (Stock, Budget, Issue, Approval, Order, Delivery, Production, Rework)

| Sheet | Modes / key fields | CTA / gate |
|-------|--------------------|------------|
| Stock Update | Set current \| Add \| Reduce \| Transfer; Product; Location; Qty; Reason; Custody | Post stock update — explicit movement only |
| Budget / Target | Sales basis \| Accepted units; New target hero; Period; Previous; Reason; Units target | Save revision — versioned + effective-dated |
| Post Update / Issue | Update \| Issue \| Risk \| Approval; Title; Severity; Owner; Link; Due | Post update |
| Approval / Reimburse | Approval \| Reimbursement; Action; Requester; Approver; Policy; Decide by | Submit for approval — approval ≠ disbursement |
| Order | Sales order \| Purchase order \| Work order \| Delivery; Commitment only hero | Save order — no revenue/stock/GST |
| Delivery | Incoming receipt \| Outgoing dispatch; Direction; Source; Qty remaining; Confirm; Dispatch on | Post delivery — no duplicate invoice revenue |
| Production | Batch/run \| MTO job \| Rework \| Hold WO; Finished product; Batch; Accepted/Rejected qty; Scrap; Materials; Produced on | Post production — accepted only sellable |
| Rework / Scrap / Job Work | Rework \| Scrap \| Job work out; Action; Source batch; Qty; Vendor; Expected return | Post action — distinct events |

### Task 5.1–5.8

- [ ] **Step 1:** Implement each body with MFG chrome; Order commitment copy locked.
- [ ] **Step 2:** Production accepted/rejected split; Rework never overwrites accepted FG silently.
- [ ] **Step 3:** Commits per sheet or batched `feat(mfg): production+ops Figma parity` if small.

**Acceptance:** Gates in Why notes are behavioral, not decorative.

---

## Phase 6 — Pickers + Switch Moment polish + hub copy

### Task 6.1: Date / Month / DateTime pickers

- [ ] **Step 1:** Theme shared pickers with ManufacturingAmber (or `MfgPickers`) matching Figma layouts (quick chips, calendar grid, time slots).
- [ ] **Step 2:** Wire Sale date / Period / Dispatch on / Decide by / Produced on / Expected return to these pickers.
- [ ] **Step 3:** Commit `feat(mfg): amber date/month/time pickers`

### Task 6.2: Switch Moment

- [ ] **Step 1:** Ensure MFG row shows **Current**; + New Business moment intact.
- [ ] **Step 2:** Commit if changes needed; else note N/A.

### Task 6.3: Hub eyebrow / hero copy

- [ ] **Step 1:** Align QuickAdds hero with plant positioning (Plan. Make. Fulfil. / live tagline) without breaking V12 tile set (D2).
- [ ] **Step 2:** Commit `feat(mfg): QuickAdds hub copy polish`

**Acceptance:** Opening a date field from any capture shows amber-themed picker; moment switcher marks MFG current.

---

## Phase 7 — Final MFG parity + latent path note

- [ ] **Step 1:** Walk all 31 frames against device screenshots (iOS + Android); checklist below.
- [ ] **Step 2:** Fix residual chrome drift only (no new APIs).
- [ ] **Step 3:** Document latent `OperatingCaptureSheet` MFG fallback if any tile still hits it — follow-up, not ship blocker if router covers hub path.
- [ ] **Step 4:** Commit `chore(mfg): QuickAdds Figma fidelity parity pass`

---

## Phase S — Consumer Service follow-on (same layouts, SVC palette)

**Goal:** Apply the **same sheet layout system** proven on MFG/Retail to Consumer Service money/ops captures, using ServicesTeal / `TealSheetChrome`. Masters + HTR already largely exist (`2026-11-10-service-flow-22-screens.md`); this phase closes capture body fidelity and any remaining chrome gaps.

### What to clone vs share

| Piece | Clone | Share / parameterize |
|-------|-------|----------------------|
| Sheet chrome primitives | Prefer keep `Teal*` (already) | Optionally extract `VerticalSheetChrome` if D1→B later |
| Amount-first capture bodies | **Clone layout structure** from `MfgCaptureSheets` / Rose capture → `ServiceCaptureLayouts` deepened | Event post paths already service-variant |
| Hold/Transfer/Reprint | Keep service semantics (appointment/staff) — **do not** copy MFG WO/stock wording | Cross-nav + slip hero pattern from MFG/Retail utilities |
| Masters forms | Already SVC-specific | Chrome tokens only |
| Hub | `ServiceQuickAddHub` / `RetailFamilyHubScreen` | Tile → sheet router patterns |
| Pickers | Theme with teal | Same calendar/time component |

### SVC file list (touch)

**Android:** `ConsumerServiceQuickAdds.kt`, `ServiceHoldTransferReprint.kt`, `ServiceMasterForms.kt`, `ServiceMastersSheet.kt`, `ServicesTealTokens.kt`, `ServiceStoreKit.kt`; add `ServiceCaptureSheets.kt` if money QA still only on `OperatingCaptureSheet`.

**iOS:** `ServiceQuickAdds.swift`, `ServiceSheetChrome.swift`, `ServiceCaptureLayouts.swift`, `ServiceHoldTransferReprint.swift`, `ServiceMasterForms.swift`, `OperatingExperienceHost.swift`.

### Phase S tasks

- [ ] **S.1:** Audit SVC QA-01…10 vs Figma nodes in service-flow plan; list field deltas like MFG Phase 4–5.
- [ ] **S.2:** Deepen Finalize Charge / Purchase / Collection / Payment / Expense / Stock / Booking / Status / Budget / Issue bodies to amount-first + picker rows + Why/CTA parity (teal chrome).
- [ ] **S.3:** Confirm HTR + masters still match SVC Figma; fix only drift.
- [ ] **S.4:** Both-platform verification checklist (teal tokens, booking≠revenue, GST on Finalize Charge only).
- [ ] **S.5:** Commits `feat(svc): capture sheets layout parity from MFG/Retail pattern`

**Out of Phase S:** Replacing MFG plant hub with service hub; inventing new service GST engine.

---

## Multiagent parallelization (execution)

```
Phase 0 (tokens+chrome) ──► Phase 1 (routers)
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
         Phase 2 masters  Phase 3 utilities  (pickers can start after 0)
              │               │
              └───────┬───────┘
                      ▼
              Phase 4 money captures ──► Phase 5 ops/production
                      │
                      ▼
              Phase 6 polish ──► Phase 7 parity
                      │
                      ▼
              Phase S (SVC) — after MFG acceptance (D8)
```

**Suggested agent split after Phase 0–1:**
- Agent A: Masters forms/lists (iOS+Android)
- Agent B: Utilities + browsers
- Agent C: Money captures Sale→Return
- Agent D: Ops/production Order→Rework
- Agent E (later): SVC Phase S

Do not parallelize chrome inventiveness — Agents C/D must consume Phase 0 primitives only.

---

## Verification checklist

### MFG (per platform)

- [ ] All 31 frames reachable from MFG QuickAdds / masters / pickers (Switch Moment from shell).
- [ ] Amber/zinc tokens only — no Rose/Teal bleed in MFG sheets.
- [ ] Titles, mode chips, Why notes, CTAs match Figma inventories.
- [ ] GST rows Unavailable/Est.; no fabricated tax posted.
- [ ] Hold WO: no stock/cost post; Transfer: no sale; Reprint: same invoice no.
- [ ] Order: commitment only; Production: accepted≠rejected; Rework: distinct event.
- [ ] Create/Edit masters: hydrate or locked Edit (D10).
- [ ] iOS and Android field lists identical for each sheet.
- [ ] Role-locked tiles visible, not removed.

### SVC follow-on

- [ ] Teal/lime tokens only on service sheets.
- [ ] Booking/Hold/Transfer/Status never post revenue/GST; charge/reprint honesty.
- [ ] Layout structure matches MFG/Retail amount-first pattern.

---

## Acceptance

1. **Layout + content** match Figma for all 31 MFG frames on iOS and Android.
2. **Tokens correct per vertical** — MFG amber/zinc; SVC teal/lime when Phase S ships.
3. **Hard gates** preserved (GST, hold, reprint, commitment, production honesty).
4. **Hub wiring** opens dedicated MFG sheets on the production QuickAdds path.
5. **Phase S plan** is executable without redesigning chrome from scratch.

---

## Execution notes

- Plan only until product approves; do not implement UI in the planning PR.
- Prefer subagent-driven-development: one sheet family per agent, reviewer gate between phases.
- When a Figma layer still uses restaurant leftovers (e.g. chip name “Personal care” on Parts browser), **prefer manufacturing visible text** in the same frame (Finished / Consumables / etc.) — treat restaurant names as leftover component names.
- Do not commit secrets; do not expand Trading/Distribution/Restaurant scope.

## Out of scope

- Pulse / Moments / Life / Memory board redesign (separate operating UI plans).
- Real thermal/PDF print SDK, e-invoice IRN, GST portal verification network calls.
- Full MRP/BOM explosion engine beyond fields existing APIs accept.
- Parameterized VerticalSheetChrome mega-refactor unless D1 chooses B mid-flight.
