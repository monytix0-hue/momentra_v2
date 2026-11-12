# Trading Quick Add Figma Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close Trading Quick Add (TRD-QA-01…12) Figma parity gaps on APK + iOS so context/WHY copy, amount-first chrome, More details, chip routing, and QA-09/10 fields match Figma `TzLvwVwlPbeVB8ug1zB3GM`.

**Architecture:** Keep existing capture hosts (`OperatingCaptureSheet` / `OperatingQuickSheet`). Introduce a **shared string catalog** (mirrored Kotlin + Swift constants — same literals) for context lines, WHY bodies, sticky meta, and CTAs. Port APK chrome patterns (`AmountBlock`, `MoreDetails`, `quickAddHint` navigation) onto iOS; fix the few remaining APK copy/CTA gaps. No event-type / API enum changes.

**Tech Stack:** Kotlin Jetpack Compose (APK), SwiftUI (iOS), Figma TRD-QA frames as copy SoT.

**Spec / audit baseline:** Agent [Trading QA audit](7a88857e-0f51-46d1-aeb1-e1a15eb962da) — APK ~74%, iOS ~58%, overall ~66%. Palette already OK (`#151D2E` / `#10B981` / `#06B6D4`).

**Figma nodes (fileKey `TzLvwVwlPbeVB8ug1zB3GM`):**

| QA | Mode | nodeId |
|----|------|--------|
| 01 Sale | SALE | `2566:31418` |
| 02 Purchase | PURCHASE | `2566:31496` |
| 03 Collection | COLLECTION | `2566:31565` |
| 04 Payment | PAYMENT | `2566:31619` |
| 05 Expense | SPEND | `2566:31672` |
| 06 Stock | STOCK | `2566:31735` |
| 07 Return | RETURN | `2566:31060` |
| 08 Budget | BUDGET | `2566:31129` |
| 09 Update/Issue | UPDATE / ISSUE | `2566:31183` |
| 10 Approval | APPROVAL | `2566:31236` |
| 11 Order | ORDER | `2566:31298` |
| 12 Delivery | SHIPMENT | `2566:31357` |

## Global Constraints

- **Trading-only** — do not change Marketing / Retail / Restaurant / Distribution / Service capture chrome or copy unless a shared helper is used with a trading guard.
- **Enums / event types unchanged** — SALE, PURCHASE, COLLECTION, PAYMENT, SPEND, STOCK, RETURN, BUDGET, UPDATE, ISSUE, APPROVAL, ORDER, SHIPMENT, HOLD, TRANSFER, REPRINT stay as today.
- **Brand prefix** — context lines are `{BusinessTitle} Trading · …` (Figma uses `Northstar Trading · …`; runtime uses `model.title` / `model.title` with fallback `"Trading"`).
- **Copy must match the table below character-for-character** (aside from brand prefix).
- **No backend / SQL / API contract changes** for this plan (hub may *display* all 12 tiles client-side; capture still posts existing event types).
- **CF-05 Order** — Customer/Supplier order stay ORDER; Delivery chip navigates to SHIPMENT (do not invent a new ORDER subtype).
- Palette tokens already land in `OperatingCaptureChromeTokens.Trading` (APK) and trading theme (iOS) — do not re-theme.

## Review Focus

1. **Chip navigation leaves sheet** — selecting Return/Reprint/Transfer/Delivery/Approval/Hold DO/Reimburse must open the target sheet (not stay on a placeholder body).
2. **Conditional QA-01 fields** — Customer / Received now / Due date only when Part paid / Pay later (not always visible).
3. **QA-09 Approval chip** — must open APPROVAL sheet; must not post UPDATE with “Approval” mode.
4. **QA-10 CTA** — primary button is always `Confirm action` on trading; Approve/Reject remain mode chips only.
5. **Purchase context “only”** — `stock only when received` (missing word today on both platforms).

---

## 1. Goals / non-goals

### Goals
- Bring Trading Quick Add APK + iOS to Figma parity for **context lines, WHY bodies, amount-first hero, More details accordion, mode-chip routing, QA-09 fields, QA-10 CTA, sticky footer meta**.
- Raise estimated parity from ~66% toward ~90%+ on the audited checklist (chrome + copy + routing).
- Single source-of-truth string catalog so APK/iOS cannot drift again on context/WHY.

### Non-goals
- Rewriting capture save/API payloads, GST masters, or continuity LOVs.
- Pixel-perfect spacing/typography beyond the audited gaps.
- HOLD / TRANSFER / REPRINT frame redesign (only ensure chips route into existing sheets).
- Distribution / other verticals’ Quick Adds.
- Changing hub icons, Pulse chrome, or Memory story.
- Fixing Figma typo `Supplier / supplier` upstream (keep label as Figma unless design corrects it).

---

## 2. Exact Figma context + WHY string table

Pulled via Figma MCP `use_figma` on 2026-11-10 from TRD-QA-01…12. **Implement these strings.** Brand prefix shown as `{Brand}` (= `model.title` blank → `"Trading"`; Figma sample uses `Northstar`).

| QA | Mode | Context line (suffix after `{Brand} Trading · `) | WHY body | Primary CTA | Sticky footer meta (example) |
|----|------|--------------------------------------------------|----------|-------------|------------------------------|
| 01 | SALE | `Sale ≠ receipt · stock only on dispatch` | `Mode chips re-render the form — item fields hidden in Quick amount. Part paid posts one sale + linked partial receipt + remaining AR. Itemized total is read-only sum of lines.` | `Save sale` | `Part paid · ₹90,000 stays in AR` (compute remaining) |
| 02 | PURCHASE | `Purchase ≠ payment · stock only when received` | `Itemized + received-now adds stock; receive-later leaves goods pending for Delivery → Incoming. Payable remains until Payment allocates.` | `Save purchase` | `₹… payable · goods received now` |
| 03 | COLLECTION | `Never posts a new sale · allocates to open AR` | `Single-source default; multi-allocation only when enabled. Allocations must equal collection amount — no over-allocation.` | `Save collection` | `Allocated ₹… of ₹…` |
| 04 | PAYMENT | `Never posts a new purchase · allocates to open AP` | `Only unpaid/part-paid posted purchase/expense sources. Sum of allocations must equal payment.` | `Save payment` | `Allocated ₹… of ₹…` |
| 05 | SPEND | `Expense ≠ payment · payable if unpaid` | `Paid posts expense + linked disbursement once. Pay later requires payee and due date when policy demands.` | `Save expense` | `Paid · posts expense + disbursement` |
| 06 | STOCK | `Reconciliation/adjustment — not a purchase` | `Set current reconciles to a target qty. Never invents a purchase or sale. Amount-only sales do not move stock.` | `Save stock update` | `Prior 184 → +16 → 200 pc` |
| 07 | RETURN | `Reversal only · stock changes need explicit goods movement` | `Credit cannot exceed eligible source. Stock changes only when goods movement is explicit — not inferred from credit amount.` | `Save return` | `Credit ₹… · +N pc to stock` |
| 08 | BUDGET | `Versioned plan · never mutates past actuals` | `Preserves prior version. Pulse Target & Budget uses the effective version for the selected period.` | `Save revision` | `v3 replaces v2 · prior kept` |
| 09 | UPDATE/ISSUE | `Operational only — never money or stock by inference` | `Issue fields appear only for Issue mode. Creates Needs Attention + Activity entries, not financial facts.` | `Post` | `Adds to Needs Attention` |
| 10 | APPROVAL | `Approval ≠ disbursement · maker-checker aware` | `Only role-authorized actions shown. Self-approval blocked when maker-checker enabled. Reimburse posts payment separately from approval.` | `Confirm action` | `Reimburse posts a separate payment` / decision meta |
| 11 | ORDER | `Commitment only — no sale, purchase, stock, or money` | `Save creates commitment. Record Sale / Record Purchase select eligible orders and prefill unexecuted lines. Direct sale without order remains allowed.` | `Save order` | `Commitment ₹… · nothing posted` |
| 12 | SHIPMENT | `Physical movement only — not a new sale or purchase` | `Outgoing reduces undelivered sale qty + stock once. Incoming reduces unreceived purchase qty + increases stock. Money settlement stays on Collection/Payment.` | `Save delivery` | `−60 pc stock · money stays on Collection` |

**HOLD / TRANSFER / REPRINT** (chip destinations; keep existing short context if already present):

| Mode | Context suffix (current → keep unless Figma adds frames later) |
|------|----------------------------------------------------------------|
| HOLD | `Freeze open order or dispatch` |
| TRANSFER | `Move qty between locations` |
| REPRINT | `Read only · never posts` |

**More details control label (all money/ops sheets):** `More details · notes, attachments, tags`

**Today’s wrong context suffixes (both platforms — replace):**

| Mode | Current (wrong) |
|------|-----------------|
| COLLECTION | `Collection allocates to sale AR` |
| PAYMENT | `Payment allocates to payable` |
| SPEND | `Expense ± disbursement` |
| STOCK | `Reconcile qty · no invented buy/sell` |
| RETURN | `Credit vs source · explicit goods` |
| BUDGET | `Versioned plan amount` |
| UPDATE/ISSUE | `Ops signal only` |
| APPROVAL | `Decide / pay separately` |
| ORDER | `Commitment only` |
| SHIPMENT | `Goods movement · money on Collection` |
| PURCHASE | `… stock when received` (missing **only**) |

---

## 3. Shared copy / source-of-truth approach

**Decision: mirrored catalog, not a runtime shared module.**

APK and iOS do not share a package. Introduce **identical string tables** on both sides and treat APK literals (already ≈ Figma for WHY) as the reference when editing:

| Platform | New / primary file | Responsibility |
|----------|--------------------|----------------|
| APK | `apk/.../shared/TradingQuickAddCopy.kt` | `object TradingQuickAddCopy` — `context(mode, brand)`, `why(mode)`, `cta(mode)`, `moreDetailsLabel`, sticky meta builders |
| iOS | `momentra/.../Shell/BusinessActive/TradingQuickAddCopy.swift` | `enum TradingQuickAddCopy` — same keys/strings |

**Rules:**
1. `tradingContextLine` / `afterSaveNote` / trading CTA branches **must call the catalog** — no inline Figma sentences left in sheet bodies.
2. When a string changes in Figma, update **both** catalog files in the same PR.
3. Optional later: JSON under `shared/` — **out of scope** (YAGNI); mirrored constants are enough for 12 sheets.
4. Unit-test the catalog (APK JVM test + iOS XCTest or string snapshot) that each mode’s context/WHY equals the table above (brand-agnostic suffix asserts).

**Wire-up:**
- APK: replace `tradingContextLine()` in `OperatingQuickSheet.kt` (~L4527) and inline `TradingWhyPanel("…")` bodies with `TradingQuickAddCopy.*`.
- iOS: replace `tradingContextLine` / `afterSaveNote` in `OperatingCaptureSheet.swift` (~L88, ~L1048) with catalog.

---

## 4. Workstreams (P0 → P1 → P2) + file touch list

### File map (real paths)

**APK**
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingQuickSheet.kt` — capture bodies, `tradingContextLine`, `TradingWhyPanel`, `MoreDetails`, `AmountBlock`, `actionLabel`, chip → `OperatingChromeState.quickAddHint`
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingCaptureChrome.kt` — tokens (read-only unless danger/meta polish)
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingChromeState.kt` — `quickAddHint` routing
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingV12Hub.kt` — hub tiles / subtitles
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/trading/TradingQuickAdds.kt` — hub host / sheet enum titles
- **Create:** `.../shared/TradingQuickAddCopy.kt`
- **Test:** `apk/app/src/test/.../TradingQuickAddCopyTest.kt`

**iOS**
- `momentra/momentra/Shell/BusinessActive/Sheets/OperatingCaptureSheet.swift` — `tradingBody`, context, WHY, sticky meta, CTA, chips
- `momentra/momentra/Shell/BusinessActive/Sheets/OperatingQuickAddHubView.swift` — tile list from API (+ guarantee 12)
- `momentra/momentra/Shell/BusinessActive/OperatingCatalog.swift` — `OperatingCaptureLayout` labels / `forMode`
- `momentra/momentra/Shell/BusinessActive/OperatingComponents.swift` — `OperatingWhyFootnote` (may add amount hero / more-details helpers here)
- `momentra/momentra/Shell/BusinessActive/OperatingExperienceHost.swift` — only if sheet routing needs host-level reopen
- **Create:** `.../BusinessActive/TradingQuickAddCopy.swift` (+ amount hero / more-details views if not in Components)
- **Test:** add XCTest for copy catalog if test target exists; otherwise snapshot asserts in plan verify steps

---

### Workstream A — P0 (correctness)

#### Task A1: Catalog + context lines (both)

**Files:** Create `TradingQuickAddCopy.kt` / `.swift`; Modify `OperatingQuickSheet.kt` `tradingContextLine`; Modify `OperatingCaptureSheet.swift` `tradingContextLine`

- [ ] **Step 1:** Add catalog with all 12 context suffixes + HOLD/TRANSFER/REPRINT from §2.
- [ ] **Step 2:** Wire both platforms’ `tradingContextLine` to `TradingQuickAddCopy.context(mode, brand)`.
- [ ] **Step 3:** APK test: assert PURCHASE contains `stock only when received`; COLLECTION equals Figma suffix; etc.
- [ ] **Step 4:** Commit `fix(trading-qa): align TRD context lines with Figma catalog`

#### Task A2: APK QA-10 CTA → Confirm action

**Files:** Modify `OperatingQuickSheet.kt` `actionLabel` (~L3242) trading `APPROVAL` branch → always `"Confirm action"` (chips stay Approve/Reject/Reimburse)

- [ ] **Step 1:** Change trading APPROVAL CTA only (`if (trading) "Confirm action"`); leave non-trading Approve/Reject.
- [ ] **Step 2:** Manual verify Approval sheet footer shows Confirm action for Approve and Reject modes.
- [ ] **Step 3:** Commit `fix(trading-qa): use Confirm action CTA on TRD-QA-10`

#### Task A3: iOS amount-first hero

**Files:** Create helper (e.g. `TradingAmountHero` in `OperatingComponents.swift` or CaptureSheet); Modify `tradingBody` for sale/purchase/collection/payment/spend/budget/return (+ qty hero for stock/shipment)

**Mirror APK `AmountBlock`:** 40pt bold value, `INR ₹` emerald pill, field `#1E293B`, border emerald @ 33%, radius ~20.

- [ ] **Step 1:** Implement `TradingAmountHero(label:currency:text:hint:)` matching APK tokens.
- [ ] **Step 2:** Replace plain amount/qty `field(...)` on money/qty sheets listed above.
- [ ] **Step 3:** Commit `feat(ios-trading-qa): add amount-first hero matching Figma`

#### Task A4: iOS More details accordion

**Files:** `OperatingCaptureSheet.swift` (+ small dashed control view)

- [ ] **Step 1:** Add dashed border control labeled `More details · notes, attachments, tags`; expand to note/attachments/tags placeholders (parity with APK `MoreDetails` — note required minimum).
- [ ] **Step 2:** Remove always-visible `Note (optional)` from trading sheets that should hide notes behind accordion (01–08, 11–12; 09–10 as Figma).
- [ ] **Step 3:** Commit `feat(ios-trading-qa): dashed More details accordion`

#### Task A5: iOS mode-chip navigation (mirror APK `quickAddHint`)

**Files:** `OperatingCaptureSheet.swift`; possibly hub/host sheet binding

| Chip | From mode | Target mode / sheet |
|------|-----------|---------------------|
| Return / Reprint | SALE | RETURN / REPRINT |
| Transfer | STOCK | TRANSFER |
| Delivery | ORDER | SHIPMENT |
| Approval | UPDATE | APPROVAL |
| Hold DO | SHIPMENT | HOLD |
| Reimburse | APPROVAL | PAYMENT |

APK reference: `OperatingQuickSheet.kt` ~L529–558 and sale chips ~L757–777 (`OperatingChromeState.quickAddHint`).

- [ ] **Step 1:** Add `onRouteToMode: (String) -> Void` (or dismiss + set hub sheet) so chip selection closes current capture and opens target.
- [ ] **Step 2:** Wire SALE chips to include `Return`/`Reprint`; wire STOCK/ORDER/UPDATE/SHIPMENT/APPROVAL as table.
- [ ] **Step 3:** On navigate, do **not** save; clear draft of source sheet.
- [ ] **Step 4:** Commit `feat(ios-trading-qa): chip routing parity with APK quickAddHint`

---

### Workstream B — P1 (field & copy fidelity)

#### Task B1: iOS WHY bodies from catalog

**Files:** `TradingQuickAddCopy.swift`; `OperatingCaptureSheet.afterSaveNote`

- [ ] Replace paraphrases with exact §2 WHY strings (APK `TradingWhyPanel` already matches — copy those literals into catalog).
- [ ] Commit `fix(ios-trading-qa): exact Figma WHY copy for TRD-QA-02…12`

#### Task B2: QA-09 fields both platforms

**Files:** APK Update/Issue capture (~L1904); iOS `case .update` (~L365)

Figma fields:
- Description (M)
- Linked source (O) + Search affordance
- Owner (C · Issue)
- Due date (C · assigned)
- Mode chips: Update · Issue · **Approval** (navigates away — Task A5)

- [ ] Rename iOS `What changed` → `Description (M)`; add Linked source / Owner / Due date with Issue-only conditionals.
- [ ] Align APK labels to Figma (Description, Linked source, Owner, Due date); ensure WHY uses Issue-mode sentence from §2.
- [ ] Commit `fix(trading-qa): TRD-QA-09 field labels and Issue conditionals`

#### Task B3: QA-01 iOS field order + conditionals + Return/Reprint chips

**Files:** `OperatingCaptureSheet.swift` `case .sale`

Order per Figma: chips → amount hero → Sale date → Payment state → conditional Customer (part paid) / Received now / Due date → Channel → More details → WHY → sticky → Save sale.

- [ ] Match APK conditional visibility (`Customer (C · part paid)`, etc.).
- [ ] Add Return/Reprint chips (routing from A5).
- [ ] Commit `fix(ios-trading-qa): TRD-QA-01 field order and conditionals`

#### Task B4: Sticky footer meta closer to Figma

**Files:** APK footer meta helpers; iOS `tradingStickyMeta`

- [ ] Prefer computed examples from §2 (AR remaining, Prior→delta, delivery `−N pc…`) when inputs available; graceful fallbacks OK.
- [ ] Commit `fix(trading-qa): sticky footer meta closer to Figma`

---

### Workstream C — P2 (polish / hub)

#### Task C1: iOS hub guarantee all 12 tiles

**Files:** `OperatingQuickAddHubView.swift`

- [ ] When `theme.isTradingCapture`, union API `quickAdds` with `OperatingCaptureLayout.allCases` codes so Approval/Return/etc. always appear (disabled/locked if role/API omits — prefer show + open sheet over hide).
- [ ] Commit `fix(ios-trading-qa): always show twelve TRD hub tiles`

#### Task C2: Decide-by + Linked source affordance

**Files:** Approval + Update captures both platforms

- [ ] Add `Decide by (C)` (datetime) on QA-10 when feasible without new API (local/display + note if server ignores).
- [ ] Linked source on QA-09: search/LOV affordance mirroring Collection’s eligible source pattern where possible.
- [ ] Commit `feat(trading-qa): Decide-by and Linked source affordances`

#### Task C3: Soft polish

- [ ] iOS Due date label/color uses danger `#EF4444` when required/overdue styling applies.
- [ ] Hub subtitle consistency: prefer `TRD-QA-0n` (iOS) or document APK `QA-0n · …` as acceptable variant — pick one and align.
- [ ] Commit `chore(trading-qa): due-date danger + hub subtitle consistency`

---

## 5. Per-sheet checklist (01–12)

Use after each platform change. Mark APK / iOS separately.

| QA | Context | WHY | Amount/Qty hero | More details | Mode chips + routing | Fields | Sticky meta | CTA |
|----|---------|-----|-----------------|--------------|----------------------|--------|-------------|-----|
| 01 Sale | ☐☐ | ☐☐ | ☐ iOS | ☐ iOS | Return/Reprint → sheets ☐ iOS | Order + conditionals ☐ iOS | ☐☐ | Save sale ☐☐ |
| 02 Purchase | ☐☐ (+ only) | ☐ iOS | ☐ iOS | ☐ iOS | Quick cost/Itemized ☐☐ | Supplier/Goods ☐☐ | ☐☐ | Save purchase ☐☐ |
| 03 Collection | ☐☐ | ☐ iOS | ☐ iOS | ☐ iOS | — | Eligible sale ☐☐ | ☐☐ | Save collection ☐☐ |
| 04 Payment | ☐☐ | ☐ iOS | ☐ iOS | ☐ iOS | — | Eligible payable ☐☐ | ☐☐ | Save payment ☐☐ |
| 05 Expense | ☐☐ | ☐ iOS | ☐ iOS | ☐ iOS | Quick/Detailed ☐☐ | Payee conditional ☐☐ | ☐☐ | Save expense ☐☐ |
| 06 Stock | ☐☐ | ☐ iOS | ☐ iOS qty | ☐ iOS | Transfer → TRANSFER ☐ iOS | Item/UOM/Reason ☐☐ | Prior→delta ☐☐ | Save stock update ☐☐ |
| 07 Return | ☐☐ | ☐ iOS | ☐ iOS | ☐ iOS | Customer/Supplier ☐☐ | Goods movement ☐☐ | ☐☐ | Save return ☐☐ |
| 08 Budget | ☐☐ | ☐ iOS | ☐ iOS | ☐ iOS | Budget/Target ☐☐ | Period/Effective ☐☐ | vN replaces ☐☐ | Save revision ☐☐ |
| 09 Update | ☐☐ | ☐☐ | — | ☐☐ | Approval → QA-10 ☐☐ | Description/Linked/Owner/Due ☐☐ | Needs Attention ☐☐ | Post ☐☐ |
| 10 Approval | ☐☐ | ☐ iOS | Amount if Reimburse ☐☐ | ☐☐ | Reimburse → Payment ☐ iOS | Decide by (P2) ☐☐ | ☐☐ | **Confirm action** ☐ APK |
| 11 Order | ☐☐ | ☐ iOS | — | ☐ iOS | Delivery → SHIPMENT ☐ iOS | Lines/Status ☐☐ | Commitment ☐☐ | Save order ☐☐ |
| 12 Delivery | ☐☐ | ☐ iOS | ☐ iOS qty | ☐ iOS | Hold DO → HOLD ☐ iOS | Source/Prefill ☐☐ | −N pc… ☐☐ | Save delivery ☐☐ |

☐☐ = APK then iOS.

---

## 6. Acceptance criteria + verify steps

### Acceptance
1. Every trading sheet context line matches §2 (brand + suffix).
2. Every trading WHY body matches §2 exactly on both platforms.
3. iOS money/qty sheets show 40pt amount/qty hero with INR ₹ pill + emerald border.
4. iOS shows dashed `More details · notes, attachments, tags` accordion.
5. All chip navigations in Task A5 open the correct sheet without posting.
6. APK QA-10 primary CTA is `Confirm action`.
7. QA-09 shows Description / Linked source / Owner (Issue) / Due date (Issue); Approval chip leaves sheet.
8. iOS Trading hub lists all 12 layouts (+ HOLD/TRANSFER/REPRINT extras OK).
9. No changes to non-trading vertical capture copy/chrome.
10. Event type enums and API payloads unchanged.

### Verify steps
1. **Catalog unit test** — run APK `TradingQuickAddCopyTest` (all modes).
2. **APK manual** — Trading moment → Quick Add → open each of 01–12; screenshot context + WHY + CTA; for QA-10 confirm Confirm action; for Sale tap Return/Reprint and confirm navigation.
3. **iOS manual** — same matrix; confirm amount hero + More details; chip routing matrix A5.
4. **Regression** — open Marketing or Retail Quick Add once; confirm lilac/non-trading chrome untouched.
5. **CF-05** — Order → Delivery chip opens Delivery (SHIPMENT), not a broken placeholder.
6. **Hub** — iOS with projection missing APPROVAL still shows Approval tile.

---

## 7. Risks

| Risk | Mitigation |
|------|------------|
| **CF-05 Order** — Delivery chip vs ORDER document type confusion | Navigate to SHIPMENT sheet; do not set `documentType=Delivery` as save path (APK already returns early when Delivery selected). |
| **API hub `quickAdds`** — Approval/Return omitted → iOS hub incomplete | Client-side union with `OperatingCaptureLayout.allCases` for trading; keep API as unlock/role hint if needed. |
| **Enums unchanged** — temptation to add REIMBURSE event type | Reimburse continues to route to PAYMENT capture (existing). |
| **Decide-by without API** | P2 display-only or store in note/contract only if field exists; do not invent columns. |
| **Shared helper regressions** | Gate all new UI behind `theme.isTradingCapture` / `variant == trading`. |
| **Copy drift** | Catalog + dual-file PR rule; test pins suffixes. |
| **Linux host can’t compile iOS** | Author against APK patterns; human verifies Xcode locally. |

---

## 8. Suggested PR / commit split

| PR | Scope | Platforms |
|----|-------|-----------|
| **PR1** | Catalog + context lines + APK QA-10 CTA | APK + iOS copy wire |
| **PR2** | iOS chrome: amount hero + More details | iOS |
| **PR3** | iOS chip routing + QA-01 conditionals | iOS |
| **PR4** | WHY fidelity + QA-09 fields + sticky meta | APK + iOS |
| **PR5** | Hub guarantee 12 + Decide-by / Linked source + soft polish | APK + iOS |

Smaller commits inside PRs follow Task steps above. Prefer **PR1 before PR2–3** (catalog unlocks copy). PR2/PR3 can parallelize after PR1 merges.

---

## Execution notes for implementers

1. Do **not** rewrite `AmountBlock` on APK — already correct; port behavior to iOS.
2. APK WHY panels are the best in-repo Figma mirror today — lift into catalog rather than re-typing from screenshots.
3. Chip routing on iOS likely needs hub `@State sheet` ownership: CaptureSheet should call `onNavigate(mode)` provided by `OperatingQuickAddHubView` / `OperatingExperienceHost`.
4. Keep `TradingQuickAdds.kt` sheet titles as-is (already Figma-aligned).

---

## Self-review (plan)

| Spec / audit item | Task |
|-------------------|------|
| P0 context lines | A1 |
| P0 iOS amount hero | A3 |
| P0 iOS More details | A4 |
| P0 iOS chip nav | A5 |
| P0 APK QA-10 CTA | A2 |
| P1 iOS WHY | B1 |
| P1 QA-09 | B2 |
| P1 QA-01 iOS | B3 |
| P1 sticky meta | B4 |
| P2 hub 12 | C1 |
| P2 Decide-by / Linked source | C2 |
| P2 soft polish | C3 |
