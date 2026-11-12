# Retail QuickAdds Bottom-Sheet Figma Fidelity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close Figma fidelity gaps for all 20 retail QuickAdds / master bottom-sheets on **both iOS and Android** so chrome, section order, field labels, chips, and cards match Figma while preserving intentional gates (no fake GST math, no PDF/thermal print stack, park session-only, reprint read-only).

**Architecture:** Keep the existing retail Rose sheet stack (`RetailSheetChrome` + masters / POS / utilities / capture). Expand each sheet’s body to Figma section parity using existing chrome primitives (`RoseSectionCard`, `RoseLinesCard`, `RoseTotalsCard`, `RoseScanField`, `RoseAmountHero`, `RoseStepper`, `RosePickerRow`, `RoseModeChip`, `RoseLockedChip`, sticky footer). Do **not** invent backend APIs. Persist only fields the existing event/master drafts already accept; extra Figma fields stay UI-local or map into existing draft/metadata bags. Both platforms ship the same field set in the same phase.

**Tech Stack:** SwiftUI (iOS), Jetpack Compose (Android/APK), Figma file `TzLvwVwlPbeVB8ug1zB3GM` as visual SoT.

**Spec / audit baseline:** Agent [Retail QuickAdds audit](64384673-9e99-455f-8185-6714fd62ea56) — all 20 **WIRED** + **PARTIAL**; none MISSING/WRONG WIRE on the retail hub path. Field inventories below were re-checked via Figma MCP `get_metadata` on 2026-11-10 (session `d946b000-9adb-4828-b171-26b6c9d80444`).

**Figma nodes (correct mapping):**

| # | Sheet | nodeId | Frame name |
|---|-------|--------|------------|
| 1 | Supplier | `2506:29612` | RTL-FORM · New / Edit Supplier |
| 2 | Location | `2506:29703` | RTL-FORM · Add / Edit Location |
| 3 | Product | `2506:29782` | RTL-MASTER · Create Product / SKU |
| 4 | Stock item | `2506:29907` | RTL-MASTER · Create Stock Item |
| 5 | Hold/Park | `2508:29333` | RTL-QA-HOLD · Park bill · Register 2 |
| 6 | Transfer | `2508:29447` | (Stock transfer) |
| 7 | Reprint | `2508:29537` | (Reprint) |
| 8 | POS Hold recall | `2438:29655` | (POS mode) |
| 9 | POS Quick amount | `2439:29655` | RTL-QA-01-ALT · POS Sale · Quick amount |
| 10 | POS Scan cart | `2421:29655` | RTL-QA-01-ALT · POS Sale |
| 11 | Purchase | `2399:30130` | RTL-QA-02 · Record Purchase |
| 12 | Collection | `2399:30194` | RTL-QA-03 · Record Collection |
| 13 | Payment | `2399:30253` | RTL-QA-04 · Record Payment |
| 14 | Expense | `2399:30306` | RTL-QA-05 · Record Expense |
| 15 | Purchase order | `2443:29784` | RTL-QA-07 · Purchase Order |
| 16 | Sales order | `2399:30418` | RTL-QA-07 · Sales Order |
| 17 | Stock update | `2399:30359` | (Stock update) |
| 18 | Status | `2399:30482` | (Status) |
| 19 | Budget | `2399:30539` | (Budget) |
| 20 | Issue | `2399:30597` | (Issue) |

## Global Constraints

- **Retail-only** — these 20 nodes; do not change Trading / Restaurant / Service / Marketing Quick Adds.
- **Both platforms in parity** — every task that changes a sheet must land iOS + Android in the same task (or a paired A/B task that cannot merge until both pass).
- **Reuse Rose chrome** — extend `RetailSheetChrome.swift` / `RetailSheetChrome.kt`; do not fork a second visual system.
- **Reuse event posting + capability gating** — existing `RetailEventDraft` / capture post paths / `requires` locks stay authoritative.
- **Do NOT invent:** fake GST computation, PDF/thermal print stack, park sync APIs, split-tender settlement engine.
  - GST / tax rows → keep **Unavailable** (or label-only “Est.” display from master rate chips without posting computed tax) where that was intentional.
  - Bill/print / Receipt print / Slip to → chrome + picker UI; action may stay no-op / locked / session preference only.
  - Park/hold → **session-only** (no new park backend).
  - Reprint → **read-only** pick + preview chrome; never re-post.
  - Split tender → show chip; keep locked / Unavailable unless an existing path already supports it.
- **Do not wire restaurant KOT semantics** into retail Hold (Figma reuses some restaurant layer names; retail copy is Harbor Mart / SKU lines).
- **Latent risk (follow-up, not this plan’s ship gate):** generic `OperatingQuickAddHubView` → `OperatingCaptureSheet` is a non-Rose path. Retail production uses `OperatingExperienceHost` / `RetailQuickAdds`. Track as follow-up; do not expand scope unless product asks.

## Review Focus

1. **Fake GST** — a sheet must never post computed CGST/SGST from invented rates; Unavailable / estimate-label only.
2. **Park/reprint gates** — Hold must not post revenue/stock; Reprint must not create a new SALE.
3. **Cross-platform drift** — same fields, chips, titles, CTA copy on iOS and Android after each phase.
4. **Create/Edit mode** — Edit existing must load a picker + populate fields or be explicitly gated; empty Edit that silently creates is a bug.
5. **Order hub decision** — do not silently add a second hub tile or remove ORDER mode chips without resolving Decision D1.

---

## Open product decisions (resolve before or during the named phase)

| ID | Decision | Options | Default if product silent | Blocks |
|----|----------|---------|---------------------------|--------|
| **D1** | Hub Order tile vs split PO/SO | **A)** Keep one Order tile + Sales/Purchase mode chips (matches current code + Figma mode chips on both frames). **B)** Split hub into Purchase Order + Sales Order tiles that open the same sheet preselected. | **A** (combined) — document as intentional; still deepen PO/SO bodies to Figma | Phase 7 hub wiring only |
| **D2** | Master Create / Edit existing | **A)** Full Edit: pick existing master → hydrate → update via existing createMaster/update path if available. **B)** Create-only UI with Edit chip locked “Unavailable”. | Prefer **A** if update API exists; else **B** with locked chip | Phase 1 |
| **D3** | Quick amount keypad | **A)** On-sheet numeric keypad (Figma). **B)** System keyboard + amount hero only. | **A** (Figma) | Phase 3 |
| **D4** | GST rows on money sheets | Keep **Unavailable** vs show decorative “Est.” from selected rate chip with no post. | **Unavailable** for posted values; decorative Est. OK on PO/SO totals if labeled “Est. … on GRN/bill” | Phases 2–3, 7 |
| **D5** | Print / Bill / Receipt rows | Chrome-only vs deep-link to future print stack. | Chrome-only + locked secondary action | Phases 3–4, 6, 8 |
| **D6** | Location “Receipt printer” chips | UI preference stored in draft metadata vs omit. | UI-only metadata; no printer SDK | Phase 1 Location |
| **D7** | Expense Type/Period pickers | Free-text vs chip LOV from Figma samples. | Chip LOV + optional free-text fallback | Phase 5 |

Mark each decision’s chosen option in the PR description when executing.

---

## File map (anchors)

### Android
- `apk/.../retail/RetailSheetChrome.kt` — shared primitives
- `apk/.../retail/RetailMasterForms.kt` — Supplier / Location / Product / Stock item
- `apk/.../retail/RetailPosSheet.kt` — Scan cart / Hold recall / Quick amount
- `apk/.../retail/RetailUtilitySheets.kt` — Hold/Park, Transfer, Reprint
- `apk/.../retail/RetailCaptureSheets.kt` — Purchase, Collection, Payment, Expense, Order, Stock, Status, Budget, Issue
- `apk/.../retail/RetailQuickAdds.kt` — hub tiles + sheet router

### iOS
- `momentra/.../Retail/RetailSheetChrome.swift`
- `momentra/.../Retail/RetailMasterForms.swift`
- `momentra/.../Retail/RetailPosSheet.swift`
- `momentra/.../Retail/RetailUtilitySheets.swift`
- `momentra/.../Retail/RetailCaptureSheets.swift`
- `momentra/.../Retail/RetailKit.swift` — hub tiles
- `momentra/.../OperatingExperienceHost.swift` — SALE → POS; HOLD/TRANSFER/REPRINT → utility; else capture; masters → master form
- `momentra/.../Sheets/OperatingMastersView.swift` — masters entry

### Follow-up risk (out of phase ship gate)
- `OperatingQuickAddHubView.swift` + `OperatingCaptureSheet.swift` — non-Rose latent path

---

## Phase 0 — Shared chrome gaps (prerequisite)

**Goal:** Any missing primitives needed by later phases exist once on both platforms.

**Files:** `RetailSheetChrome.swift`, `RetailSheetChrome.kt`

### Task 0.1: Audit chrome primitives vs Figma needs

- [ ] **Step 1:** Confirm both platforms already have grabber, header, mode chips, scan field, lines card, totals card, amount hero, stepper, picker row, more details, why box, sticky footer, locked chip.
- [ ] **Step 2:** Add only missing shared pieces (likely: numeric keypad grid for Quick amount; Create/Edit chip row helper; “Unavailable” GST row helper if not centralized).
- [ ] **Step 3:** Compile Android + iOS sheet modules.
- [ ] **Step 4:** Commit `chore(retail): shared Rose chrome for QuickAdds fidelity`

**Acceptance:** Later phases do not invent one-off keypad/totals widgets outside chrome.

---

## Phase 1 — Masters field parity (Supplier → Product → Location → Stock item)

**Priority order inside phase:** Supplier → Product → Location → Stock item (audit: Supplier/Product are largest deltas; Location next; Stock item last among masters).

**Files:** `RetailMasterForms.swift`, `RetailMasterForms.kt` (+ chrome if needed)

### Figma field inventory (do not invent extras)

**Supplier `2506:29612` — “New supplier”**
- mode: Create | Edit existing
- Supplier name; Legal / trade name; GSTIN (+ helper text)
- GST registration chips: Regular | Composition | Unregistered
- Phone; Email (optional)
- Category chips: FMCG | Staples | Personal care | Home care | Beverages | Packaging
- Payment terms; Address
- Status: Active | Preferred | Inactive
- Why note + CTA “Save supplier”

**Product `2506:29782` — “Create product / SKU”**
- Create | Edit existing; context chips (POS / Catalogue master / GST-aware)
- Product name; Category chips (Staples, Snacks, Personal care, Beverages, Home care)
- Variants: Size / pack | Single | Colour set
- Sell price hero; MRP
- GST on product chips: 5% | 18% | Exempt (+ note that GST posts on bill, not save)
- HSN / SAC; Default location chips; Unit chips (pcs/kg/pack)
- Barcode / EAN; Track batch / expiry row; Active | Inactive
- CTA “Save product”

**Location `2506:29703` — “Add location”**
- Create | Edit existing
- Location name; Type: Store | Warehouse | Counter
- Receipt printer chips: Counter printer 1 | Label printer | Screen only
- Display order; Stocks categories chips; Location lead (optional)
- Status: Active | Paused
- CTA “Save location”

**Stock item `2506:29907` — “Create stock item”**
- Create | Edit existing; context chips
- Item name; Category chips (Packaging/Consumables/Cleaning/Stationery/Display/Spares — use Figma labels; treat “Sparesaging” typo as Packaging)
- Unit of measure chips; Purchase rate hero; GST on purchase chips (0/5/12/18%)
- HSN code; Default location picker; Par / reorder qty; Preferred supplier picker
- Track stock | Non-tracked; Active | Inactive
- CTA “Save stock item”

### Task 1.1: Supplier master fidelity (both platforms)

**Interfaces:**
- Consumes: existing `createMaster` / master draft path
- Produces: Supplier form matching Figma sections; posts only supported master fields

- [ ] **Step 1:** Replace thin Identity-only form with Figma section order and labels above.
- [ ] **Step 2:** Implement Create/Edit per D2 (hydrate or lock Edit).
- [ ] **Step 3:** Map GST registration / category / status chips into draft metadata or typed fields already accepted; do not invent GST verification network calls (helper text can be static/local).
- [ ] **Step 4:** Align titles: “New supplier” / CTA “Save supplier”; Why copy from Figma.
- [ ] **Step 5:** Device check (see Verification) on iOS + Android.
- [ ] **Step 6:** Commit `feat(retail): Supplier master Figma field parity`

### Task 1.2: Product master fidelity

- [ ] **Step 1:** Separate Product from Stock-item toggle if Android still folds them; Product sheet must match Product Figma (Stock item is Task 1.4).
- [ ] **Step 2:** Add sell-price hero, MRP, GST chips (rate selection only — no tax posting), HSN, location/unit/barcode/batch/status.
- [ ] **Step 3:** Parity check both platforms; commit `feat(retail): Product master Figma field parity`

### Task 1.3: Location master fidelity

- [ ] **Step 1:** Add Type / Receipt printer / Display order / Stocks categories / Lead / Status per Figma.
- [ ] **Step 2:** Printer chips per D6 (UI-only).
- [ ] **Step 3:** Parity + commit `feat(retail): Location master Figma field parity`

### Task 1.4: Stock item master fidelity

- [ ] **Step 1:** Dedicated Stock item body (not a thin catalogue clone).
- [ ] **Step 2:** Purchase rate hero, GST-on-purchase chips, HSN, location, par, preferred supplier, track chips.
- [ ] **Step 3:** Ensure hub/masters entry can open Stock item without requiring Product mode toggle as the only path (Android).
- [ ] **Step 4:** Parity + commit `feat(retail): Stock item master Figma field parity`

**Phase 1 acceptance (all four):**
- Visual section order matches Figma inventories above.
- Save still uses existing master write path.
- Edit existing follows D2 without creating ghost duplicates.
- iOS ↔ Android field lists identical.

---

## Phase 2 — Purchase receive-lines + scan affordance

**Files:** `RetailCaptureSheets.swift` / `.kt` (PURCHASE body)

### Figma inventory `2399:30130` — “Record Purchase”
- Goods | Supplies mode chips
- Scan supplier invoice / SKU (scan field + Scan button)
- Receive lines card (SKU · name · qty · rate · HSN · line total; + Add line · From PO · Favourites)
- Totals: Goods; Input GST · CGST+SGST (**Unavailable** or Est. per D4); Freight; Purchase total + Pay later / AP open
- Tender: Pay later | Pay now | Part
- Pickers: Supplier; Location; Linked PO
- More details; Why; CTA `Post purchase · ₹…`

### Task 2.1: Purchase chrome + multi-line receive

- [ ] **Step 1:** Add Goods/Supplies chips, scan affordance (camera/scanner hook optional; field must exist).
- [ ] **Step 2:** Upgrade to multi-line receive card with stepper + line meta (HSN from master if known).
- [ ] **Step 3:** Totals card + Pay later/now/part; Linked PO picker (optional).
- [ ] **Step 4:** Keep GST row Unavailable/Est. per D4 — never invent tax posting.
- [ ] **Step 5:** Post still uses existing PURCHASE event path; commit `feat(retail): Purchase receive-lines Figma parity`

**Acceptance:** Can add ≥2 lines, see totals, choose Pay later, post once; GST not fabricated.

---

## Phase 3 — POS Scan cart chrome, then Quick amount keypad

**Files:** `RetailPosSheet.swift` / `.kt`

### Figma Scan cart `2421:29655` — “POS Sale”
- Mode chips: Scan cart | Hold recall | Quick amount
- Scan or search SKU + Scan button
- Cart card with steppers, HSN meta, + Add line · Recent · Favourites
- Totals: Subtotal; Discount; CGST/SGST row (**Unavailable**/Est.); Payable now + Tender
- Tender: UPI | Cash | Card | Split (Split locked OK)
- Customer picker; Bill / print picker (D5); More details
- CTA `Charge ₹… · Post & print` (print may be chrome-only)

### Figma Quick amount `2439:29655`
- Same mode chips; Sale amount hero (“No SKU lines · revenue only · stock unchanged”)
- Keypad 1–9 / . / 0 / ⌫ (D3)
- Totals GST/Discount/Payable; Tender chips; Customer; Channel; GST picker (selection UI; no fake compute)
- CTA `Charge ₹… · Post` — no stock move

### Task 3.1: Scan cart chrome

- [ ] **Step 1:** Align cart line density (SKU · name · unit price · HSN · stepper · line total).
- [ ] **Step 2:** Totals card + Bill/print row + Favourites affordance (can open existing picker).
- [ ] **Step 3:** CTA copy; GST Unavailable; Split locked.
- [ ] **Step 4:** Commit `feat(retail): POS Scan cart Figma chrome`

### Task 3.2: Hold recall density (node `2438:29655`)

- [ ] **Step 1:** Hold list meta chips density to match Figma (park id, register, line count); still session parks only.
- [ ] **Step 2:** Commit with 3.1 or `feat(retail): POS Hold recall chrome`

### Task 3.3: Quick amount keypad

- [ ] **Step 1:** Implement on-sheet keypad per D3; amount hero updates; still posts amount-only SALE (no SKU).
- [ ] **Step 2:** Channel + GST picker rows; GST values Unavailable/Est.
- [ ] **Step 3:** Commit `feat(retail): POS Quick amount keypad`

**Phase 3 acceptance:** Mode switching preserves draft where Figma says so; Quick amount never moves stock; Scan cart Charge posts itemized sale via existing path.

---

## Phase 4 — Collection + Payment totals/allocation cards

**Files:** capture sheets Collection / Payment bodies

### Collection `2399:30194` — “Record Collection”
- Allocate against card (open invoice line)
- Totals: Open AR; This receipt; Remaining; Collect now + Method
- Method: UPI | Cash | Card | Bank
- Customer; Reference; Receipt print (D5)
- CTA `Post collection · ₹…`

### Payment `2399:30253` — “Record Payment”
- Allocate against card
- Totals: Open AP; This payment; Remaining; Pay now + Method
- Method: Bank | UPI | Cash | Cheque
- Payee; Reference; Linked purchase
- CTA `Post payment · ₹…`

### Task 4.1: Collection fidelity

- [ ] **Step 1:** Title “Record Collection”; allocation card + three-row totals + Bank method.
- [ ] **Step 2:** Receipt print chrome (D5); keep AR allocation post path.
- [ ] **Step 3:** Commit `feat(retail): Collection totals Figma parity`

### Task 4.2: Payment fidelity

- [ ] **Step 1:** Allocate card density; Open AP totals; Bank/Cheque chips; Payee/Reference/Linked purchase rows.
- [ ] **Step 2:** Commit `feat(retail): Payment allocation Figma parity`

**Acceptance:** Allocation still equals receipt/payment amount; never creates new SALE/PURCHASE.

---

## Phase 5 — Expense payment-state + type/period

**Files:** Expense / SPEND body in capture sheets

### Figma `2399:30306` — “Record Expense”
- Mode: Store | Ops | Other
- Totals: Amount; GST (if any) Unavailable/0; Cash impact; Expense hero + Type
- Payment state: Paid now | Pay later
- Type picker; Payee; Period
- CTA `Post expense · ₹…`

### Task 5.1: Expense fidelity

- [ ] **Step 1:** Replace Android Running/Rent/Ops (or iOS equivalent) with Store/Ops/Other.
- [ ] **Step 2:** Add payment-state chips, totals card, Type/Payee/Period per D7.
- [ ] **Step 3:** Commit `feat(retail): Expense payment-state Figma parity`

**Acceptance:** Paid now vs Pay later reflected in draft/post metadata; no stock invented from expense.

---

## Phase 6 — Hold / Park chrome polish

**Files:** `RetailUtilitySheets.swift` / `.kt`

### Figma `2508:29333` — “Hold / Park bill”
- Cross-nav chips: Hold | Transfer | Reprint (route to existing sheets)
- Register chips; context chips
- Hold slip hero (id, customer, line count)
- Category station chips; parked line list with meta chips
- Meta: Slip to; Cashier; Linked bill (Not posted · draft)
- CTA `Park bill · HOLD-…` — session only

### Task 6.1: Hold park chrome

- [ ] **Step 1:** Add cross-nav + register chips + hold-slip hero + line list density.
- [ ] **Step 2:** Printer/cashier meta chrome; park remains session-only.
- [ ] **Step 3:** Commit `feat(retail): Hold park chrome Figma polish`

**Acceptance:** Park creates/recalls session hold; no revenue/stock/GST post; Transfer/Reprint chips navigate correctly.

---

## Phase 7 — Order PO/SO (decision + body depth)

**Files:** Order body in capture sheets; hub in `RetailQuickAdds.kt` / `RetailKit.swift`

### Figma PO `2443:29784` / SO `2399:30418`
- Both frames include Sales order | Purchase order mode chips (supports D1-A)
- PO: Stock items browser + PO lines + totals (Goods, Est. input GST, Freight, PO value) + Supplier / Expect by / Deliver to / Place of supply
- SO: Catalogue browser + SO lines + totals (Lines value, Est. GST on bill, Balance due) + Customer / Fulfil by / Ship·pickup / Place of supply
- Commitment-only Why + CTAs; no AP/revenue/stock on save

### Task 7.0: Record D1 decision

- [ ] **Step 1:** Confirm D1-A or D1-B with product (or apply default A).
- [ ] **Step 2:** If B: add hub tiles that open ORDER preselected; keep mode chips.
- [ ] **Step 3:** Document choice in PR body.

### Task 7.1: Deepen Order bodies

- [ ] **Step 1:** Add browser/add-line chrome (can reuse catalogue pickers), multi-line steppers, totals with Est. GST labels (D4).
- [ ] **Step 2:** Side-specific pickers (Supplier vs Customer; Expect by vs Fulfil by; Deliver to vs Ship/pickup).
- [ ] **Step 3:** Commit `feat(retail): Order PO/SO Figma body parity`

**Acceptance:** Save remains commitment-only ORDER event; GST estimate not posted as tax.

---

## Phase 8 — Status / Budget / Issue / Transfer / Reprint polish

**Files:** capture Status/Budget/Issue; utility Transfer/Reprint

### Task 8.1: Align chip labels + density to Figma
- Status: open-order pick + status chips + assignee (match Figma labels, not restaurant leftovers)
- Budget: metric chips + amount + revision/period chrome
- Issue: severity / assignee / attachment rows (attachments may be chrome-only)
- Transfer: From/To pickers + scan + multi-line cart density
- Reprint: richer read-only receipt preview chrome; still no re-post

- [ ] **Step 1:** Implement Status + Budget + Issue polish both platforms.
- [ ] **Step 2:** Implement Transfer + Reprint polish both platforms.
- [ ] **Step 3:** Commit `feat(retail): Status Budget Issue Transfer Reprint polish`

**Acceptance:** Behavior gates unchanged; chrome matches Figma section names; both platforms aligned.

---

## Phase 9 — Final parity pass + latent-path note

### Task 9.1: Cross-platform checklist run
- [ ] Walk all 20 sheets on one Android device + one iOS simulator/device using Verification below.
- [ ] Fix any remaining title/CTA/chip drifts found.
- [ ] Commit `fix(retail): QuickAdds Figma fidelity final pass` if needed.

### Task 9.2: Document latent OperatingCaptureSheet risk
- [ ] Add a short note in PR / follow-up issue: `OperatingQuickAddHubView` still presents non-Rose `OperatingCaptureSheet` — not live for retail hub via `OperatingExperienceHost`, but a footgun if that hub is reused for retail.

---

## Verification checklist (device steps)

Run after each phase for touched sheets; full run after Phase 9.

### Setup
1. Open a **Retail** business with Rose hub (`RetailQuickAdds` / retail `OperatingExperienceHost`).
2. Confirm capability tiles unlock expected sheets (locked tiles stay locked).

### Per sheet
| Sheet | Steps | Pass criteria |
|-------|-------|---------------|
| Supplier | Masters → Supplier → fill Figma fields → Save | Sections match; master appears in Purchase picker |
| Location | Create location with Type + printer chips | Saved; usable as default location |
| Product | Create with sell hero + GST chip + HSN | Saved; appears in POS search |
| Stock item | Create with purchase rate + track chip | Saved; appears in PO browser |
| Purchase | Add 2 receive lines, Pay later, Post | PURCHASE posted; GST not invented |
| POS Scan | Add lines, Charge | SALE posted; Split locked; GST Unavailable |
| POS Quick amount | Keypad enter amount, Charge | Amount-only sale; stock unchanged |
| POS Hold recall | Park from Hold, recall into cart | Session park works |
| Collection | Allocate open AR, Post | AR reduces; no new sale |
| Payment | Allocate open AP, Post | AP reduces; no new purchase |
| Expense | Store + Paid now + Period, Post | Expense posted |
| Hold/Park | Park bill CTA | Session hold; no revenue |
| Transfer | From/To + qty | Posts existing transfer path |
| Reprint | Pick sale | Preview only; no new post |
| Order PO/SO | Add lines, Save | ORDER commitment only |
| Stock / Status / Budget / Issue | Happy-path fill + post | Labels match Figma; gates intact |

### Regression gates (must stay true)
- [ ] No computed fake GST posted
- [ ] No PDF/thermal print SDK introduced
- [ ] Park remains session-only
- [ ] Reprint never re-posts
- [ ] iOS and Android field parity for every finished phase

---

## Suggested multiagent parallelization

```
Phase 0 (chrome) ─────────────────────────────► must finish first
        │
        ├─► Phase 1 Masters
        │     sequential inside: Supplier → Product → Location → Stock item
        │     optional: Agent-A iOS Supplier ∥ Agent-B Android Supplier (same task contract)
        │
        ├─► Phase 2 Purchase          ⎤
        ├─► Phase 3 POS               ⎥ can run in parallel AFTER Phase 0
        ├─► Phase 4 Collection/Pay    ⎥ (different files; coordinate chrome only)
        ├─► Phase 5 Expense           ⎥
        └─► Phase 6 Hold park         ⎦
                │
                └─► Phase 7 Order (needs D1; after Purchase helpful for Linked PO UX)
                        │
                        └─► Phase 8 polish (Transfer/Reprint can start after Phase 6)
                                │
                                └─► Phase 9 final parity (single agent, both platforms)
```

**Parallel-safe pairs**
- Masters (Phase 1) ∥ POS (Phase 3) ∥ Collection/Payment (Phase 4) after Phase 0
- Expense (5) ∥ Hold (6) after Phase 0
- iOS agent ∥ Android agent **per sheet** if they share the field inventory table and merge only when both pass acceptance

**Must stay sequential**
- Phase 0 before everything
- Create/Edit (D2) decision before shipping master Edit UX
- D1 before hub tile changes in Phase 7
- Phase 9 after all feature phases
- Do not parallel-edit the same file without a single owner (`RetailSheetChrome`, `RetailCaptureSheets`)

---

## Execution notes for agents

1. Before coding a sheet, re-fetch Figma `get_metadata` (or screenshot) for that nodeId — do not invent fields.
2. Prefer extending Rose primitives over one-off layouts.
3. When a Figma row cannot post, keep the chrome and gate the action (locked chip / Unavailable / session-only).
4. Commit per task; do not batch unrelated sheets into one commit.
5. Do not touch Trading plan work (`2026-11-10-trading-quick-add-figma-parity.md`) or non-retail capture sheets.

---

## Out of scope

- Restaurant POS / KOT fidelity
- Building real GST engine, print stack, or park sync backend
- Pixel-perfect typography beyond Rose tokens already in retail
- Reworking Pulse / Memory / non-sheet hub chrome
- Fixing the latent `OperatingCaptureSheet` retail footgun (document only in Phase 9)
