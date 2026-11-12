# Made-to-Order (Job Work) QuickAdds Bottom-Sheet Figma Fidelity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all **29** Made-to-Order (job work) Figma frames (masters, forms, journey captures, money/ops, Hold/Transfer/Reprint) on **both iOS SwiftUI and Android Compose** so chrome, section order, field labels, chips, Why copy, and CTAs match Figma while using the **MTO soft-amber / zinc** vertical palette — without inventing GST engines, print stacks, or park/hold backends.

**Architecture:** Mirror the Retail Rose + Manufacturing Amber fidelity stack for MTO. Introduce `MtoSheetChrome` (soft-amber primitives parallel to `Rose*` / `Mfg*` / `Teal*`). Route MTO hub tiles through dedicated MTO sheets instead of leaving bodies on generic `OperatingCaptureSheet` forever. Reuse existing `MtoCapture` / `ManufacturingEventDraft` posting and capability gates. Keep MFG Amber WIP untouched.

**Tech Stack:** SwiftUI (iOS), Jetpack Compose (Android/APK), Figma file `TzLvwVwlPbeVB8ug1zB3GM` as visual SoT. Tokens: `design/tokens/business.tokens.json` → `palettes.manufacturing` + MTO soft-amber overrides already in `OperatingV12Theme.madeToOrder()` / `BusinessActiveTheme.MadeToOrder`.

**Spec / audit baseline:** Figma inventory via MCP `create_session` + `get_metadata` on 2026-11-11 (session `58f58d28-72f2-475e-b6d1-9147a42f5cf7`). Template: `docs/superpowers/plans/2026-11-10-mfg-quickadds-figma-fidelity.md` and Retail Rose plan. Do **not** break uncommitted MFG Amber WIP.

**Demo brand in Figma:** Forge & Form · “Quote. Confirm. Make. Invoice.” — replace with live moment title/tagline in product; keep MTO copy semantics (quote ≠ revenue; invoice owns AR/GST).

**Gate:** Plan only until product replies **ok** (same MFG approve-before-execute pattern).

---

## 1. Figma inventory (all 29)

| # | nodeId | Frame name | Sheet type | Maps to existing code? |
|---|--------|------------|------------|------------------------|
| 1 | `2686:31133` | MTO-QA · Create Quotation | Journey / commitment | **Partial** — `MtoCapture.QUOTATION` → `OperatingCaptureSheet`; needs amount-first + Quotation\|Revise\|Accept\|Cancel chips |
| 2 | `2686:31201` | MTO-QA · Accept / Confirm Order | Journey / commitment | **Partial** — `CONFIRM`; commitment-only hero |
| 3 | `2686:31269` | MTO-QA · Open Production Job | Job ops | **Partial** — `OPEN_JOB`; From order\|Job\|BOM\|Hold chips; reserve ≠ consume |
| 4 | `2686:31348` | MTO-QA · Record Production · against job | Production | **Partial** — `PRODUCTION`; Against job\|Batch\|Rework\|WIP move; accepted≠rejected |
| 5 | `2686:31427` | MTO-QA · Invoice against order | Money / sale | **Partial** — `INVOICE`; From order\|New sale\|Return\|Reprint; only INV owns revenue/GST |
| 6 | `2692:29187` | MTO-QA · Collection / Advance received | Money | **Partial** — `COLLECTION`; advance = liability until INV |
| 7 | `2692:29252` | MTO-QA · Payment to supplier | Money | **Partial** — `PAYMENT`; settle AP only |
| 8 | `2692:29317` | MTO-QA · Delivery / dispatch against job | Ops | **Missing tile/enum** — Figma present; **no** `MtoCapture.DELIVERY` / hub tile today (D11) |
| 9 | `2692:29380` | MTO-QA · Purchase job materials | Money | **Partial** — `MATERIALS`; Job materials\|General stock; job required |
| 10 | `2692:29450` | MTO-QA · Record Expense · job link | Money | **Partial** — `EXPENSE`; job-linked cost |
| 11 | `2696:29187` | MTO-MASTER · Invite & QR | Masters list | **Partial** — masters Invite; roles Owner/Manager/Job lead/Estimator/Operator |
| 12 | `2696:29271` | MTO-MASTER · Customers & suppliers | Masters list | **Partial** — Principals; Customer/Supplier/Job work badges |
| 13 | `2696:29331` | MTO-MASTER · Products & BOM | Masters list | **Partial** — Catalogue/parts; Raw/WIP/Finished/Consumables/Spares |
| 14 | `2696:29413` | MTO-MASTER · Lines & stores | Masters list | **Partial** — Locations; Line/Station/Store + Active/Planned |
| 15 | `2696:29470` | MTO-FORM · New / Edit Customer or Supplier | Master form | **Thin / shared** — party form; Role Customer\|Supplier\|Job work\|… |
| 16 | `2696:29561` | MTO-FORM · Add / Edit Line or Store | Master form | **Thin / shared** — Store\|Line\|Station; Job-card printer chips |
| 17 | `2696:29640` | MTO-MASTER · Create Part / SKU | Master form | **Partial** — parts master; sell price hero; GST label-only |
| 18 | `2696:29765` | MTO-MASTER · Create BOM / job item | Master form | **Missing dedicated** — Job BOM (Cut/Machine/Plate/QC/Pack/Dispatch) |
| 19 | `2698:37725` | MTO-QA · Return / Credit note | Money | **Partial** — `RETURN`; restock to Job FG |
| 20 | `2698:37795` | MTO-QA · Rework / Scrap / Job work | Production | **Partial** — `REWORK`; distinct events |
| 21 | `2698:37860` | MTO-QA · Stock update (job-linked) | Ops | **Partial** — `STOCK`; Set/Add/Reduce/Transfer; J-07 tagged |
| 22 | `2698:37927` | MTO-QA · Job hold | Utility | **Partial** — `HOLD` enum exists; **not** on hub tiles; HTR cross-nav |
| 23 | `2698:38041` | MTO-QA · WIP transfer | Utility | **Partial** — `WIP`/`TRANSFER`; no revenue |
| 24 | `2698:38132` | MTO-QA · Reprint invoice / job card | Utility | **Partial** — `REPRINT`; metadata root is peek — sheet body `2698:38140`; same INV no. |
| 25 | `2698:38221` | MTO-QA · Approval / Reimbursement | Ops | **Partial** — approval path / `APPROVAL` |
| 26 | `2698:38284` | MTO-QA · Post update / Issue | Ops | **Partial** — `UPDATE` / `ISSUE` |
| 27 | `2698:38351` | MTO-QA · Budget / Target revision | Ops | **Partial** — `BUDGET`; Invoiced basis \| Confirmed orders |
| 28 | `2698:38415` | MTO-QA · Quote revise / expire | Journey | **Partial** — `QUOTE_REVISE`; versioned quote ≠ revenue |
| 29 | `2698:38483` | MTO-QA · Job status change | Job ops | **Partial** — `JOB_STATUS`; Active\|Pending\|Blocked\|On hold; no stock/cost |

**Counts by type:** Journey/commitment 4 (Quote, Confirm, Revise, Open job) · Production 2 · Money 6 · Ops 4 · Utilities HTR 3 · Masters lists 4 · Master forms 4 · Job status 1 · Delivery 1 = **29**.

**Not in this 29 (defer):** dedicated Date/Month/DateTime picker frames and Switch Moment — theme shared pickers with MTO soft amber in polish (same as MFG Phase 6 light pass). Parts/Materials browsers already opened from hub masters (`Catalogue` / `Locations`); deepen chrome under masters phase if density drifts.

---

## Global Constraints

- **Both platforms in parity** — every sheet task lands iOS + Android in the same task (or paired A/B that cannot merge until both pass).
- **Vertical tokens, not Rose/Teal** — MTO uses manufacturing zinc canvas + **soft amber** hero (`#FBBF24` → `#F59E0B`); never hard-code RetailRose / ServicesTeal into MTO sheets. Prefer shared zinc with MFG; do not fork a second zinc palette.
- **Do not break MFG Amber WIP** — leave `MfgSheetChrome` / MFG capture files alone unless a shared primitive extract is explicitly chosen (D1→B). Prefer clone-and-adapt `Mto*` files.
- **Reuse event posting + capability gating** — `MtoCapture` / `draft.toMto()` / `ManufacturingEventDraft` / `requires` locks stay authoritative.
- **Do NOT invent:** fake GST computation, PDF/thermal print SDK, park/hold sync backend, job-work challan engine, BOM explosion server if missing.
  - Quotation / Confirm / Open job / Quote revise → **no revenue, AR, or GST**. Tax on quote is preview only.
  - Advance collection → **liability held**; applies to INV later; never revenue.
  - Invoice → **only** posted INV owns revenue, AR, output GST.
  - Hold job → **no stock, no cost, no GST**; resume same job.
  - Transfer → stock/WIP move at cost; **no revenue**; same-GSTIN note.
  - Reprint → **read-only** re-render; never new SALE / AR.
  - Production → only **accepted qty** becomes sellable; rejected/rework distinct.
  - Dispatch → moves accepted units only; never re-books INV revenue.
- **Hub already MTO-shaped** — `MadeToOrderQuickAddHub` / `MadeToOrderHubScreen` (Core·9 + Support·5 + Utilities·10). Prefer deepening this hub over reintroducing MFG plant V12 grid.

## Review Focus

1. **Fake GST** — never post computed CGST/SGST/IGST from invented rates; Unavailable / preview / Est. label only; GST posts on INV/GRN only.
2. **Quote / order / advance honesty** — quote≠revenue; confirm=commitment; advance=liability; only INV owns AR.
3. **Hold / Transfer / Reprint gates** — Hold no stock/cost; Transfer no sale; Reprint same invoice number / no new revenue.
4. **Production honesty** — accepted vs rejected/scrap never silently overwrite FG / job remaining.
5. **Cross-platform drift** — same fields, chips, titles, CTAs on iOS and Android after each phase.

---

## 2. MTO color palette / token mapping

### Source of truth

| Layer | Path |
|-------|------|
| Design tokens JSON | `design/tokens/business.tokens.json` → `palettes.manufacturing` (no separate `madeToOrder` key today) |
| Android (today) | `MtoBrand = Color(0xFFF59E0B)` in `MadeToOrderActive.kt`; `OperatingV12Theme.madeToOrder()` soft hero |
| Android (theme) | `BusinessActiveTheme.MadeToOrder` — accent `#FBBF24`, solid `#F59E0B`, light `#FDE68A` |
| Android (target) | New `MadeToOrderAmberTokens.kt` `object MadeToOrderAmber` (or `MtoTokens`) paralleling `ManufacturingAmber` / `RetailRose` |
| iOS (today) | Hub uses `theme.accent` from MadeToOrder kit |
| iOS (target) | `MtoSheetChrome` primitives consuming soft-amber + manufacturing zinc |

### Token table (use these exact values)

| Token | Hex | Use |
|-------|-----|-----|
| `canvas` / zinc | `#18181B` | Screen / sheet canvas (shared with MFG) |
| `canvasTop` | `#3F3F46` | Gradient start / notice border family |
| `canvasDeep` | `#09090B` | Deep scrim |
| `surface` | `#27272A` | Cards / rows |
| `inset` | `#18181B` | Inset fields |
| `accent` / `MtoBrand` | `#F59E0B` | Accent, grabber, selected chips, CTA (solid) |
| `accentSoft` | `#FBBF24` | Soft amber lead / hero start / secondary highlight |
| `accentLight` | `#FDE68A` | Soft fill chips |
| `accentDeep` | `#B45309` | Pressed / MFG-shared deep (optional) |
| `onAccent` | `#1C1917` | Ink on amber CTA |
| `heroStart` → `heroEnd` | `#FBBF24` → `#F59E0B` | Hub / sheet hero (soft → solid) |
| Primary ink | `#FFFFFF` / zinc-50 / `#F8F9FD` | Titles |
| Body / secondary | `#A1A1AA` / `#A8A29E` | Subtitles, meta |

**Chrome primitive prefix (target):** `MtoGrabber`, `MtoSheetHeader`, `MtoModeChip`, `MtoAmountHero`, `MtoSectionCard`, `MtoLinesCard`, `MtoTotalsCard`, `MtoPickerRow`, `MtoUnavailableGstRow`, `MtoStickyFooter`, `MtoWhyNote`, `MtoSavedToast` — same API shape as Rose/Mfg.

**Differentiation vs MFG:** Same zinc canvas; MTO soft amber leads (`#FBBF24`) vs MFG solid amber hero (`#F59E0B` → `#B45309`). Motion line: “Quote. Confirm. Make. Invoice.”

---

## Open product decisions (D1…)

| ID | Decision | Options | Default if product silent | Blocks |
|----|----------|---------|---------------------------|--------|
| **D1** | MTO chrome strategy | **A)** Dedicated `MtoSheetChrome` clone of Rose/Mfg API. **B)** Parameterized `VerticalSheetChrome(tokens:)`. | **A** for ship speed; extract B later | Phase 0 |
| **D2** | Hub structure | **A)** Keep Core·9 + Support·5 + Utilities·10; only swap sheet bodies. **B)** Restructure tiles to match every Figma frame 1:1. | **A** — add missing Delivery/Hold entry points without redesigning hub IA | Phase 1 |
| **D3** | Delivery capture | **A)** Add `MtoCapture.DELIVERY` + utility/core tile. **B)** Open only from Invoice/Order context. | **A** — Figma frame is a first-class QA | Phase 1 / 5 |
| **D4** | GST rows | Unavailable vs decorative Est./preview | **Unavailable** for posted math; quote GST preview + INV Est. OK if labeled | Phases 2–5 |
| **D5** | Print / job-card / invoice reprint | Chrome-only vs SDK | Chrome-only + locked secondary | Utilities |
| **D6** | Create BOM API | Full BOM lines + scrap % vs master stub | Prefer existing parts/BOM write if present; else Save BOM posts supported fields only + UI-local components | Phase 2 |
| **D7** | Hold job persistence | Session-only vs server HOLD event | Prefer existing `HOLD`/`ORDER` event if already posted; never invent stock moves | Phase 3 |
| **D8** | Job Hold vs Job Status · On hold | **A)** Separate sheets (Figma). **B)** Collapse into Job Status. | **A** — Job hold is HTR slip; Job status is status event | Phase 3 / 5 |
| **D9** | Create/Edit masters | Full hydrate vs Edit locked Unavailable | Prefer hydrate if update API exists; else lock Edit | Phase 2 |
| **D10** | Soft amber vs MFG amber in shared pickers | MTO soft tokens vs reuse MFG amber pickers | Soft amber when opened from MTO; shared component + token param | Phase 6 |
| **D11** | Collection title | “Collection / advance” (Figma) vs “Money collection” (code) | Match Figma title + Hold advance CTA when against uninvoiced SO | Phase 4 |

Mark each chosen option in the PR description when executing.

---

## File map (anchors → targets)

### Android — today

- `apk/.../mto/MadeToOrderActive.kt` — `MtoBrand`, shell
- `apk/.../mto/MadeToOrderQuickAdds.kt` — hub + `MtoCapture` → `OperatingCaptureSheet`
- `apk/.../mto/MtoToast.kt` — toast catalog
- Shared: `OperatingCaptureSheet`, `OperatingMastersSheet`, parts browser via `OperatingChromeState`
- MFG reference (do not break): `manufacturing/MfgSheetChrome.kt`, `MfgCaptureSheets.kt`, …

### Android — create / expand

- `apk/.../theme/MadeToOrderAmberTokens.kt` — `object MadeToOrderAmber` (or under `mto/`)
- `apk/.../mto/MtoSheetChrome.kt` — shared primitives
- `apk/.../mto/MtoMasterForms.kt` — Party, Line/Store, Part/SKU, BOM/job item
- `apk/.../mto/MtoMastersSheet.kt` — Invite, Customers & suppliers, Products & BOM, Lines & stores
- `apk/.../mto/MtoUtilitySheets.kt` — Job hold / WIP transfer / Reprint
- `apk/.../mto/MtoCaptureSheets.kt` — journey + money + ops bodies
- Modify: `MadeToOrderQuickAdds.kt` — router: HTR → utilities; masters → MTO masters; else `MtoCaptureSheets`; add Delivery (+ Hold tile if D2/D8)

### iOS — today

- `Shell/BusinessActive/MadeToOrder/MadeToOrderHubScreen.swift`
- `MtoToast.swift`
- `OperatingExperienceHost.swift` — MTO kit → hub + shared capture/masters
- MFG reference: `Manufacturing/MfgSheetChrome.swift`, …

### iOS — create / expand

- `Shell/BusinessActive/MadeToOrder/MtoSheetChrome.swift`
- `…/MadeToOrder/MtoMasterForms.swift`
- `…/MadeToOrder/MtoMastersViews.swift`
- `…/MadeToOrder/MtoUtilitySheets.swift`
- `…/MadeToOrder/MtoCaptureSheets.swift`
- `…/MadeToOrder/MtoKit.swift` — tile → sheet router (or extend host)
- Modify: `OperatingExperienceHost.swift` / hub — MTO HOLD/TRANSFER/REPRINT → MTO utilities; captures → MTO modules

### Shared tokens

- Prefer reuse `palettes.manufacturing` + document soft-amber overrides in theme; optional `palettes.madeToOrder` only if product wants token JSON parity (non-blocking).

---

## Phase 0 — Tokens + MtoSheetChrome (prerequisite)

**Goal:** Soft-amber token object + chrome primitives exist on both platforms so later phases do not invent one-off widgets.

### Task 0.1: MadeToOrderAmber tokens

**Files:** Create `MadeToOrderAmberTokens.kt`; confirm iOS theme accent coverage.

- [ ] **Step 1:** Add Android `object MadeToOrderAmber` with hex table above (+ typography helpers if Rose/Mfg pattern has them).
- [ ] **Step 2:** Point `MadeToOrderActive` / `madeToOrder()` theme reads at shared tokens (no behavior change).
- [ ] **Step 3:** Compile APK theme module + iOS Design target.
- [ ] **Step 4:** Commit `chore(mto): MadeToOrderAmber tokens for QuickAdds fidelity`

### Task 0.2: MtoSheetChrome primitives

**Files:** `MtoSheetChrome.kt` / `.swift`

- [ ] **Step 1:** Port Rose/Mfg primitive set renamed to `Mto*` (grabber, header, mode chips, amount hero, section/lines/totals cards, picker row, more details, why note, sticky footer, locked chip, Unavailable GST row, saved toast).
- [ ] **Step 2:** Wire colors exclusively from MadeToOrderAmber / theme soft amber + zinc.
- [ ] **Step 3:** Smoke a placeholder sheet compiling on both platforms.
- [ ] **Step 4:** Commit `chore(mto): MtoSheetChrome primitives`

**Acceptance:** Later phases import chrome only; no one-off amber hex in capture files. MFG files unchanged.

---

## Phase 1 — Wire MTO hub router (both platforms)

**Goal:** Hub tiles open dedicated MTO sheets (even if bodies are still thin wrappers), not only generic Operating capture forever.

### Task 1.1: Android router

**Files:** `MadeToOrderQuickAdds.kt`

- [ ] **Step 1:** Split presentation: HOLD/TRANSFER/REPRINT → `MtoUtilitySheets`; Invite/Customers/Parts/Lines → masters; journey/money/ops → `MtoCaptureSheets` (may initially wrap existing capture with MTO chrome shell).
- [ ] **Step 2:** Keep `MtoCapture` enum + `draft.toMto()` posting; add `DELIVERY` per D3.
- [ ] **Step 3:** Ensure Hold reachable (tile or HTR cross-nav from WIP/Reprint) per D8.
- [ ] **Step 4:** Role-lock tiles stay visible with “Role locked” / Return “Unavailable”.
- [ ] **Step 5:** Commit `feat(mto): QuickAdds hub routes dedicated MTO sheets`

### Task 1.2: iOS router

**Files:** `MadeToOrderHubScreen.swift` + `MtoKit.swift` / `OperatingExperienceHost.swift`

- [ ] **Step 1:** Mirror Android routing for MTO kit.
- [ ] **Step 2:** Catalogue → Products & BOM / parts; Locations → Lines & stores / materials as today.
- [ ] **Step 3:** Commit `feat(mto): iOS host routes MTO utilities/captures`

**Acceptance:** From MTO QuickAdds, every Figma-mapped tile opens a soft-amber-chrome sheet (content may still be partial until later phases).

---

## Phase 2 — Masters lists + forms

**Priority:** Party form → Part/SKU → Line/Store → BOM → list sheets (Invite, Customers & suppliers, Products & BOM, Lines & stores).

### Field inventories (Figma)

**Party `2696:29470` — “New customer / supplier”**
- Create | Edit existing
- Party name; Legal/trade name; GSTIN (+ state helper)
- GST registration: Regular | Composition | Unregistered
- Phone; Email optional
- Role chips: Customer | Supplier | Job work | Spares | Consumables (visible Figma text; ignore leftover restaurant component names)
- Terms; Address
- Status: Active | Preferred | Inactive
- Why + CTA `Save party`

**Line/Store `2696:29561` — “Add line / store”**
- Create | Edit; Name; Type: Store | Line | Station
- Job-card printer: Shop printer 1 | Label printer | Tablet only (D5 UI-only)
- Display order; Holds job stock: Job material | WIP | Job FG | Consumables | Spares
- Line supervisor optional; Status Active | Paused
- CTA `Save line / store`

**Part/SKU `2696:29640` — “Create part / SKU”**
- Create | Edit; context chips (Forge & Form MTO / Parts master / GST-aware)
- Part name; Category Raw/WIP/Finished/Consumables/Spares; Part type Finished/Raw/WIP
- Sell price hero; Standard cost; GST on part 18%/5%/Exempt; HSN/SAC
- Default store chips (Job FG bay / Job material store / WIP rack / QC bay); Unit nos/kg/set
- Drawing/revision; Track batch/heat; Active|Inactive
- CTA `Save part` — GST posts on INV only

**BOM `2696:29765` — “Create BOM / job item”**
- Finished part; Routing Cut/Machine/Plate (out)/QC/Pack/Dispatch
- Output UOM; Rolled-up cost hero; GST on FG chips (informational)
- HSN (FG); Issue from; Batch size; Default line
- Backflush on accept | Manual issue; Active|Inactive
- CTA `Save BOM` — stock moves only on issue/accepted production (D6)

**Lists:** Invite & QR (roles Owner/Manager/Job lead/Estimator/Operator — join ≠ quote approval ≠ payment); Customers & suppliers; Products & BOM (+ New part / + New BOM); Lines & stores (+ Add line / store).

### Task 2.1–2.4: Forms then lists

- [ ] **Step 1:** Implement Party + Line/Store + Part forms to inventory (both platforms); Create/Edit per D9.
- [ ] **Step 2:** Implement BOM / job item form per D6.
- [ ] **Step 3:** List sheets with search, badges, + New CTAs opening forms.
- [ ] **Step 4:** Invite & QR MTO roles + copy.
- [ ] **Step 5:** Device check; commits per form: `feat(mto): <name> master Figma field parity`

**Phase 2 acceptance:** Section order matches inventories; saves use existing master write paths; no GST posted on master save.

---

## Phase 3 — Utilities (Job hold / WIP transfer / Reprint)

### Job hold `2698:37927`
- Cross-nav Hold | Transfer | Reprint; line chips; context chips
- Job hold slip hero (JH-id, job title, component count)
- Lines with Raw/WIP/Consumables + Held chips
- Meta: Notify; Held by; Linked order
- CTA `Hold job · J-…` — no stock/cost (D7)

### WIP transfer `2698:38041`
- Transfer slip; from → to; valuation at standard cost; Sale impact None; Same GSTIN
- CTA `Confirm transfer · N items`

### Reprint `2698:38132` / body `2698:38140`
- Tax invoice / job card hero; Posted lines; Original print; Reprint reason; Payment meta
- CTA `Reprint · duplicate copy` — same number (D5 chrome)

### Task 3.1–3.2

- [ ] **Step 1:** `MtoUtilitySheets` Hold/Transfer/Reprint with cross-nav.
- [ ] **Step 2:** Enforce gates in Review Focus; commit `feat(mto): Hold/Transfer/Reprint Figma chrome`

**Acceptance:** Hold never posts stock; Reprint never posts SALE; Transfer never posts revenue.

---

## Phase 4 — Journey + money captures

| Sheet | Modes / key fields | CTA / gate |
|-------|--------------------|------------|
| Create Quotation | Quotation\|Revise\|Accept\|Cancel; Quote value (ex tax); Customer; Status; Lines; Validity | Send quotation — no AR/GST |
| Accept / Confirm | Same mode row; Confirmed value; Accepted quote; Approval; Spec; Due | Confirm order — creates SO commitment |
| Quote revise / expire | Q-id\|Revise\|Extend\|Expire; versioned value | Send revision vN — quotes ≠ revenue |
| Invoice against order | From order\|New sale\|Return\|Reprint; Invoice gross; Order; Sale date; Channel; Payment; GST | Post invoice — owns revenue/AR/GST |
| Collection / advance | Advance on order / against INV; Customer; Against order; Method; Reference; Received on | Hold advance / Post collection |
| Payment | Pay now; Payee; Against bill; Method; Reference; Paid on | Post payment — settle AP only |
| Purchase job materials | Job materials\|General stock; amount; Supplier; Date; Receive; Payment; Job required | Post purchase — job cost vs stock |
| Expense | Expense; Type (Job work › …); Payee; Job link; Payment; Period | Post expense — job margin cost |
| Return / Credit note | Customer\|Supplier return; Credit amount; Party; Source INV; Restock; Reason; Date | Post credit note |

### Task 4.1–4.9

- [ ] **Step 1:** One task per sheet (or tight pairs), both platforms, Why copy verbatim from Figma.
- [ ] **Step 2:** Collection/Payment allocate only — never create new SALE/PURCHASE; advance never revenue (D11).
- [ ] **Step 3:** Commits `feat(mto): <Sheet> Figma field parity`

**Acceptance:** Draft preserved on picker open (footer copy); GST honesty; posts via existing event types.

---

## Phase 5 — Ops / job work (Open job, Production, Delivery, Stock, Rework, Approval, Update/Issue, Budget, Job status)

| Sheet | Modes / key fields | CTA / gate |
|-------|--------------------|------------|
| Open Production Job | From order\|Job\|BOM\|Hold; Confirmed order; Job ID; Target; Planned start; BOM rev; Reserve material; Due | Open job — no output/stock/sale |
| Record Production | Against job\|Batch\|Rework\|WIP move; Job; Target remaining; Accepted/Rejected; Rejected goes to; Materials; Produced on | Post production — accepted only sellable |
| Delivery / dispatch | Material in\|Dispatch to buyer; Job; Source SO/J/INV; Qty capped at accepted; Confirm; Dispatch on | Post dispatch — no re-book INV |
| Stock Update | Set\|Add\|Reduce\|Transfer; Product; Location (job-tagged); Qty; Reason; Custody | Post stock update — explicit only |
| Rework / Scrap / Job work | Rework\|Scrap\|Job work out; Action; Source job; Qty; Vendor; Expected return | Post action — distinct events |
| Approval / Reimburse | Approval\|Reimbursement; Action; Requester; Approver; Policy; Decide by | Submit — approval ≠ disbursement |
| Post Update / Issue | Update\|Issue\|Risk\|Approval; Title; Severity; Owner; Link; Due | Post update — blockers link J/SO |
| Budget / Target | Invoiced basis\|Confirmed orders; New target; Period; Previous; Reason; Jobs target | Save revision — versioned; quotes never count |
| Job status change | Active\|Pending\|Blocked\|On hold; Job; New status; Reason; Since; Owner; Customer impact; Review by | Set status — no stock/cost/sale |

### Task 5.1–5.9

- [ ] **Step 1:** Implement each body with MTO chrome; Open job / Confirm commitment copy locked.
- [ ] **Step 2:** Production accepted/rejected split; Rework never overwrites accepted FG silently; Dispatch capped at accepted.
- [ ] **Step 3:** Wire Delivery per D3; Job Hold remains utility (D8).
- [ ] **Step 4:** Commits per sheet or batched `feat(mto): job-ops Figma parity` if small.

**Acceptance:** Gates in Why notes are behavioral, not decorative.

---

## Phase 6 — Pickers + hub copy polish

### Task 6.1: Date / Month / DateTime pickers

- [ ] **Step 1:** Theme shared pickers with MadeToOrderAmber soft tokens (D10).
- [ ] **Step 2:** Wire Sale date / Period / Dispatch on / Decide by / Produced on / Expected return / Due / Validity.
- [ ] **Step 3:** Commit `feat(mto): soft-amber date/month/time pickers`

### Task 6.2: Hub eyebrow / hero copy

- [ ] **Step 1:** Align QuickAdds hero with “Quote. Confirm. Make. Invoice.” / live tagline; keep Core·9 structure (D2).
- [ ] **Step 2:** Commit `feat(mto): QuickAdds hub copy polish` if needed.

**Acceptance:** Opening a date field from any capture shows soft-amber-themed picker.

---

## Phase 7 — Final MTO parity + latent path note

- [ ] **Step 1:** Walk all 29 frames against device screenshots (iOS + Android); checklist below.
- [ ] **Step 2:** Fix residual chrome drift only (no new APIs).
- [ ] **Step 3:** Document latent `OperatingCaptureSheet` MTO fallback if any tile still hits it — follow-up, not ship blocker if router covers hub path.
- [ ] **Step 4:** Confirm MFG Amber WIP untouched (`git status` manufacturing paths clean relative to this work).
- [ ] **Step 5:** Commit `chore(mto): QuickAdds Figma fidelity parity pass`

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
              Phase 4 journey+money ──► Phase 5 ops/job-work
                      │
                      ▼
              Phase 6 polish ──► Phase 7 parity
```

**Suggested agent split after Phase 0–1:**
- Agent A: Masters forms/lists (iOS+Android)
- Agent B: Utilities HTR
- Agent C: Journey + money (Quote → Expense/Return)
- Agent D: Ops/job-work (Open job → Job status + Delivery)

Do not parallelize chrome inventiveness — Agents C/D must consume Phase 0 primitives only. Do not assign agents to edit MFG modules.

---

## Verification checklist

### MTO (per platform)

- [ ] All 29 frames reachable from MTO QuickAdds / masters / HTR cross-nav.
- [ ] Soft-amber/zinc tokens only — no Rose/Teal bleed; MFG files not regressively edited.
- [ ] Titles, mode chips, Why notes, CTAs match Figma inventories.
- [ ] GST rows Unavailable/preview/Est.; no fabricated tax posted.
- [ ] Quote/Confirm/Open job/Revise: no revenue/AR; Advance: liability; Invoice: owns AR/GST.
- [ ] Hold job: no stock/cost; Transfer: no sale; Reprint: same invoice no.
- [ ] Production: accepted≠rejected; Dispatch ≤ accepted; Rework: distinct event.
- [ ] Create/Edit masters: hydrate or locked Edit (D9).
- [ ] iOS and Android field lists identical for each sheet.
- [ ] Role-locked tiles visible, not removed.
- [ ] Delivery reachable (D3).

---

## Acceptance

1. **Layout + content** match Figma for all 29 MTO frames on iOS and Android.
2. **Tokens correct per vertical** — MTO soft amber + manufacturing zinc.
3. **Hard gates** preserved (GST, quote/advance honesty, hold, reprint, production, dispatch).
4. **Hub wiring** opens dedicated MTO sheets on the production QuickAdds path.
5. **MFG Amber WIP** remains intact.

---

## Execution notes

- Plan only until product approves with **ok**; do not implement UI in the planning turn.
- Prefer subagent-driven-development: one sheet family per agent, reviewer gate between phases.
- When a Figma layer still uses restaurant leftovers (chip component names like “Cashier”, “Mains”, dish row names), **prefer manufacturing/MTO visible text** in the same frame (Job lead / Raw / Finished / Harbor flange set) — treat restaurant names as leftover component names.
- Node `2698:38132` metadata may resolve to the dimmed peek; implement against child sheet `2698:38140` / frame title **Reprint invoice / job card**.
- Do not commit secrets; do not expand Trading/Distribution/Restaurant/MFG scope in this train.

## Out of scope

- Pulse / Moments / Life / Memory board redesign.
- Real thermal/PDF print SDK, e-invoice IRN, GST portal verification network calls.
- Full MRP/BOM explosion engine beyond fields existing APIs accept.
- Parameterized VerticalSheetChrome mega-refactor unless D1 chooses B mid-flight.
- Completing MFG Amber fidelity (separate WIP).
