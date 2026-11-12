# Retail Flow (23 Figma Screens) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring all 23 Figma Retail (`RTL-*`) capture / masters / POS / Hold-Transfer-Reprint screens to layout + copy + rose palette parity on both Android (APK) and iOS, with **full POS support** (Scan cart charge, Quick amount, Hold park/recall, stock transfer, reprint) end-to-end.

**Architecture:** Treat the existing Android retail kit (`RetailPosSheet`, `RetailCaptureSheets`, `RetailMasterForms`, `RetailUtilitySheets`, `RetailQuickAdds`) as the structural reference — same specialization pattern Service used against Restaurant. Do **not** rebuild from Trading capture. Lock Figma copy/Why/CTAs and rose chrome; close visual gaps; port any Android-ahead surfaces to iOS `Shell/BusinessActive/Retail/*`. Keep Pulse/Hub shell work aligned with `docs/superpowers/plans/2026-11-09-figma-operating-ui-parity.md`. Honour honesty rules in `docs/implementation/RETAIL_MISSING_APIS.md` for register/holds/receipt PDF gaps (UI present; no invented APIs).

**Tech Stack:** Android Jetpack Compose + `RetailRose`; iOS SwiftUI + `BusinessRetail*` / RetailRose assets / Plus Jakarta; shared operating projection + `POST …/operating-events` + masters/vendors/locations APIs; device-local durable hold store (`RetailHoldSession` / `RetailParkedSaleStore` — SharedPreferences / UserDefaults scoped by moment; not a register API).

**Spec:** Figma file `TzLvwVwlPbeVB8ug1zB3GM` (momentra), session inventory below. Related: `2026-11-09-figma-operating-ui-parity.md`, `2026-11-10-service-flow-22-screens.md` (process template), `docs/implementation/RETAIL_MISSING_APIS.md`.

## Global Constraints

- Match Figma **layout and copy verbatim** (titles, Why notes, CTAs, chip labels, footers, ALT badge).
- Palette is rose/maroon on slate: accent `#F43F5E`, deep canvas `#4C0519` → `#0B0A1F` → `#050414` — already in `RetailRose` / iOS `BusinessRetail*`; do not invent a new palette.
- Honesty rules: never invent metrics as `0`/`0%`; quick amount ≠ SKU movement; Order/PO = commitment only; Hold = draft only; Collection/Payment allocate only; GST posts on POS Charge / Purchase post, never on SO/PO save.
- Platform chrome: same content on iOS + Android; only system status/safe-area differ.
- Font: Plus Jakarta Sans on both platforms (already used).
- **Expense** hub tile exists in code but is **outside this 23-node batch** — keep tile; rose money-QA honesty only (no invented fiscal scope).
- No new markdown docs beyond this plan unless the user asks.

## Review Focus

1. **POS GST / stock boundary:** Scan cart Charge posts sale + tender atomically and moves tracked stock; Quick amount posts revenue with **zero** SKU movement; Hold never posts.
2. **Commitment vs post:** Sales Order / Purchase Order never create revenue, AP, or stock; Purchase / POS Charge do.
3. **Hold/Transfer/Reprint semantics:** Retail park-bill / stock-transfer / receipt-reprint — not trading stock-hold or restaurant KOT.
4. **Register fiction:** Prefer Figma chrome with location-derived labels over fake register/shift IDs; never invent bill numbers client-side for posted sales when API lacks allocation (see Missing APIs).
5. **iOS kit routing:** Retail must stay on retail-family host (`OperatingExperienceHost` → `RetailActiveView`), not Trading hub.

---

## 1. Screen inventory (all 23)

Platform: **both iOS + Android**. No separate Figma platform variants — one mobile frame each (402×~H). Demo entity: Harbor Mart.

### A. POS Sale (three modes of one sheet family)

| # | Node | Figma name | UI title | Purpose | POS |
|---|------|------------|----------|---------|-----|
| 1 | `2438:29655` | RTL-QA-01-ALT · POS Sale · Hold recall | POS Sale | Find parked bill; Recall → Scan cart | **Core** |
| 2 | `2439:29655` | RTL-QA-01-ALT · POS Sale · Quick amount | POS Sale | Amount-first keypad; revenue only | **Core** |
| 3 | `2421:29655` | RTL-QA-01-ALT · POS Sale · Scan cart | POS Sale | Cart + tender + Charge · Post & print | **Core** |

Mode chips (all three): `Scan cart` · `Hold recall` · `Quick amount`. Header chip: `ALT`. Context: `Register 2 · Shift open · Andheri · Harbor Mart`.

### B. Money / goods QA captures

| # | Node | Figma name | UI title | CTA | POS |
|---|------|------------|----------|-----|-----|
| 4 | `2399:30130` | RTL-QA-02 · Record Purchase | Record Purchase | Post purchase · ₹… | Adjacent (GRN / AP) |
| 5 | `2399:30194` | RTL-QA-03 · Record Collection | Record Collection | Post collection · ₹… | Tender settle AR |
| 6 | `2399:30253` | RTL-QA-04 · Record Payment | Record Payment | Post payment · ₹… | Tender settle AP |

### C. Orders + ops QA

| # | Node | Figma name | UI title | CTA | POS |
|---|------|------------|----------|-----|-----|
| 7 | `2443:29784` | RTL-QA-07 · Purchase Order | Purchase Order | Save purchase order · ₹… | Pre-POS stock path |
| 8 | `2399:30418` | RTL-QA-07 · Sales Order | Sales Order | Save sales order · ₹… | Pre-POS commitment |
| 9 | `2399:30359` | RTL-QA-06 · Stock Update | Stock Update | Post stock update | Inventory |
| 10 | `2399:30482` | RTL-QA-08 · Status / Assignment | Status / Assignment | Update status | Fulfil before charge |
| 11 | `2399:30539` | RTL-QA-09 · Budget / Target Revision | Budget / Target Revision | Save revision · ₹… | Non-POS |
| 12 | `2399:30597` | RTL-QA-10 · Issue / Approval / Update | Issue / Approval / Update | Post issue | Register variance audit |

### D. Masters (list + invite)

| # | Node | Figma name | UI title | Purpose | POS |
|---|------|------------|----------|---------|-----|
| 13 | `2506:29417` | RTL-MASTER · Suppliers | Suppliers | Vendor cards, + New supplier | PO / Purchase |
| 14 | `2506:29333` | RTL-MASTER · Invite & QR | Invite team · Harbor Mart | Contacts, roles, invite link, QR | Staffing |
| 15 | `2506:29475` | RTL-MASTER · Catalogue | Catalogue | Category chips, SKU rows, + New product | Scan cart |
| 16 | `2506:29555` | RTL-MASTER · Locations | Locations | Store/Warehouse/Counter rows | Transfer / stock |

### E. Master create/edit forms

| # | Node | Figma name | UI title | CTA | POS |
|---|------|------------|----------|-----|-----|
| 17 | `2506:29612` | RTL-FORM · New / Edit Supplier | New supplier | Save supplier | Masters |
| 18 | `2506:29703` | RTL-FORM · Add / Edit Location | Add location | Save location | Masters |
| 19 | `2506:29782` | RTL-MASTER · Create Product / SKU | Create product / SKU | Save product | Scan cart catalogue |
| 20 | `2506:29907` | RTL-MASTER · Create Stock Item | Create stock item | Save stock item | PO browser |

### F. Utility HTR tickets

| # | Node | Figma name | UI title | CTA | POS |
|---|------|------------|----------|-----|-----|
| 21 | `2508:29333` | RTL-QA-HOLD · Park bill · Register 2 | Hold / Park bill | Park bill · HOLD-22 | **Core** |
| 22 | `2508:29447` | RTL-QA-TRANSFER · Stock transfer | Stock transfer | Confirm transfer · 32 units | **Core** |
| 23 | `2508:29537` | RTL-QA-REPRINT · Reprint receipt | Reprint receipt | Reprint · duplicate copy | **Core** |

Utility mode chip trio on all three: `Hold` · `Transfer` · `Reprint`.

**Flow order (dependency):** Tokens/chrome → POS Scan/Quick/Recall (1–3) → Park/Transfer/Reprint (21–23) → Purchase/Collection/Payment (4–6) → SO/PO + Stock/Status (7–10) → Masters lists (13–16) → Master forms (17–20) → Budget/Issue (11–12) → Hub wiring + iOS parity → Visual QA.

**POS end-to-end paths (both platforms):**

1. **Counter charge:** Hub Sale → Scan cart → add SKUs → tender → `Charge ₹… · Post & print` → SALE event with lines.
2. **Quick till:** Hub Sale → Quick amount → keypad → tender → `Charge ₹… · Post` → SALE amount-only.
3. **Park & recall:** Scan cart → Hold bill / Hub Hold → Park → later Hold recall → Recall HOLD-n → Scan cart → Charge.
4. **Transfer:** Hub Transfer → from/to locations → Confirm transfer (STOCK semantics; no sale).
5. **Reprint:** Hub Reprint → pick posted sale → Reprint · duplicate copy (client re-render; no new SALE).

---

## 2. Color palette / design tokens

`get_variable_defs` returned empty for POS nodes; hex from design screenshots + existing `RetailRose` / `BusinessRetailAccent` (`#F43F5E`).

| Role | Hex | Android (`RetailRose`) | iOS (`BusinessTokens` / Assets) |
|------|-----|------------------------|----------------------------------|
| Canvas top / wash | `#4C0519` | `screenTop` | `BusinessRetailCanvasTop` |
| Canvas mid | `#0B0A1F` | `screenMid` | `BusinessRetailCanvas` / Deep family |
| Canvas bottom | `#050414` | `screenBottom` | `BusinessRetailCanvasDeep` |
| Nested card | `#2A1A1F` | `card` | `BusinessRetailInset` |
| Sheet / tile / chip | `#1E293B` | `cardSlate`, `tile`, `chip` | `BusinessRetailSurface` |
| Field / header | `#0F172A` | `field` | surface deep |
| Accent rose | `#F43F5E` | `rose` | `BusinessRetailAccent` |
| Rose border | `#E11D48` | `roseBorder` | `BusinessRetailAccentDeep` |
| Rose soft / muted | `#FFE4E6` / `#FECDD3` | `roseSoft`, `roseMuted` | `BusinessRetailAccentSoft` |
| CTA / hero | `#F43F5E` | `rose` (solid or soft gradient) | `BusinessRetailHeroStart` → `HeroEnd` |
| Ink on rose CTA | `#04110C` | `inkOnRose` | `BusinessRetailOnAccent` |
| Amber / Why label / Add line | `#FBBF24` | `amber` | signal warning |
| Why body cream | `#FEF3C7` (if present) | add `whyBody` if missing | map or soft amber |
| Tender selected | `#22C55E` | `positive` | SignalPositive |
| Negative | `#EF4444` | `negative` | signal danger |
| Cyan scan badge | `#06B6D4` | `cyan` | existing |
| Primary / body / muted text | `#F8FAFC` / `#CBD5E1` / `#94A3B8` | `textPrimary` / `textBody` / `textMuted` | slate family |

**Token work:** Diff Figma hex vs `RetailRoseTokens.kt` + iOS colorsets; add only missing (`whyBody`, chip inactive border if needed). Do not rename existing keys.

**Typography:** Plus Jakarta — Bold 22–28 titles, Medium 11–14 fields, Regular 10–11 footers / Why.

**Radii (from Figma / RetailRose):** sheet top ~28, amount/cart cards 16, fields 12, pills 999, CTA 16, grab 2×40.

---

## 3. Shared components to build / reuse

| Component | Reuse from | Adapt for Retail Figma |
|-----------|------------|------------------------|
| Sheet scrim + grab + close | `OperatingSheetScrim` / iOS retail chrome | Rose accent shadow |
| Mode / category / tender chips | `RetailSheetChrome` / restaurant pills | Active = rose fill; tender selected = green |
| Amount-first hero + keypad | `RetailPosSheet` Quick amount | Exact ALT copy + INR amber |
| Cart / receive / PO / SO lines + steppers | Existing retail capture | SKU · HSN · GST meta rows |
| Scan field + Scan pill | POS / Purchase / Stock | Barcode placeholder copy |
| Why note | Capture footer | Verbatim Figma + amber “Why” |
| Sticky CTA + caption | Capture footer | Rose fill; dark ink label |
| Saved toast | Master forms | `✓ Saved · sheet closes and returns back` |
| Master list row | `RetailMastersSheet` | Preferred / Busy/Open / GST % badges |
| Invite & QR | `BusinessInviteQr` + `RoseInvitePalette` | Roles: Owner, Manager, Cashier, Stock, Floor (map schema roles per Missing APIs) |
| Catalogue / stock browser | Embedded in SO/PO sheets | Categories Staples/Snacks/…; Add + New product/stock |
| Hold / Transfer / Reprint ticket | `RetailUtilitySheets` (Kot-style) | Retail line metas (HSN · ₹ · %) |
| In-session hold store | `RetailHoldSession` / `RetailParkedSaleStore` | Wire Park ↔ Hold recall |

**Do not reuse trading Hold/Transfer/Reprint layouts** — different domain objects and copy.

**Code already present (baseline, not greenfield):**

- Android: `RetailPosSheet.kt`, `RetailCaptureSheets.kt`, `RetailMasterForms.kt`, `RetailUtilitySheets.kt`, `RetailQuickAdds.kt`, `RetailStoreActive.kt`, `RetailRoseTokens.kt`
- iOS: `Shell/BusinessActive/Retail/{RetailPosSheet,RetailCaptureSheets,RetailMasterForms,RetailUtilitySheets,RetailKit,RetailSheetChrome}.swift` + `RetailFamily/*`

---

## 4. Proposed file / module structure

### Android (`apk/app/src/main/java/com/example/momentra/…`)

```
ui/theme/RetailRoseTokens.kt              # extend missing tokens only
ui/shell/business/retail/
  RetailStoreActive.kt                    # Pulse/Life host (existing)
  RetailQuickAdds.kt                      # hub tiles → sheets (existing)
  RetailStoreKit.kt / RetailFormat.kt      # chrome helpers (existing)
  RetailSheetChrome.kt                    # shared sheet atoms (existing / tighten)
  RetailPosSheet.kt                       # Scan / Quick / Hold recall (parity pass)
  RetailCaptureSheets.kt                  # Purchase…Issue + SO/PO (parity pass)
  RetailMasterForms.kt                    # Supplier/Location/Product/Stock (parity pass)
  RetailUtilitySheets.kt                  # Park / Transfer / Reprint (parity pass)
  RetailStoryConfigSheet.kt               # out of 23 — leave alone
ui/shell/business/shared/
  BusinessInviteQr.kt                     # RoseInvitePalette roles
  OperatingSheetScrim.kt
```

### iOS (`momentra/momentra/…`)

```
Shell/BusinessActive/
  OperatingExperienceHost.swift           # retail kit routing (existing)
  Retail/
    RetailKit.swift                       # theme binding
    RetailPosSheet.swift                  # parity with Android POS
    RetailCaptureSheets.swift             # QA-02..10 / SO / PO
    RetailMasterForms.swift
    RetailUtilitySheets.swift             # HTR + parked store
    RetailSheetChrome.swift
  RetailFamily/                           # Pulse/Hub shells
Assets.xcassets/BusinessTokens/BusinessRetail*.colorset
Assets.xcassets/RetailRose/               # tile icons already present
```

---

## 5. Implementation phases (dependency order)

### Phase 0 — Tokens + sheet chrome parity
- Lock `RetailRose` / iOS retail assets to hex table.
- Shared: grab, close, chip states (rose / green tender), Why note, CTA, toast, ALT badge.
- **Done when:** throwaway preview of Scan cart chrome matches `2421:29655`.

### Phase 1 — POS Sale full modes (screens 1–3) — Android first
- Unify three modes in `RetailPosSheet` with Figma copy, keypad, cart steppers, tender UPI/Cash/Card/Split, Charge CTAs.
- Hold recall list + `Recall HOLD-n → Scan cart` must not post.
- **Done when:** screenshot parity for nodes `2421`, `2438`, `2439`; Charge posts SALE; Quick amount has empty `lines`.

### Phase 2 — Hold / Transfer / Reprint (screens 21–23)
- Ticket UIs with Hold|Transfer|Reprint chips; register channel chips; Why/CTA exact.
- Park writes `RetailHoldSession`; Transfer posts STOCK (or honest Unavailable if from/to not in schema — prefer dual STOCK notes only if product already does; otherwise keep Figma UI + document gap).
- Reprint re-renders posted SALE; never new revenue.
- **Done when:** CTAs `Park bill · HOLD-22` / `Confirm transfer · 32 units` / `Reprint · duplicate copy`.

### Phase 3 — Purchase / Collection / Payment (screens 4–6)
- Line receive + allocate-against cards; tender chips; Why/CTA lock.
- **Done when:** Purchase opens AP path; Collection/Payment never create new sale/purchase cost.

### Phase 4 — SO / PO / Stock / Status (screens 7–10)
- Embedded catalogue/stock browsers; Sales order ↔ Purchase order mode chips; commitment Why notes.
- Stock Update: Set current / Add / Reduce + Scan SKU.
- Status: Dispatch / Fulfil / Pick — ops only.
- **Done when:** SO/PO save as ORDER commitment; Status does not post SALE/STOCK.

### Phase 5 — Masters lists + forms (screens 13–20)
- Figma-faithful Suppliers / Invite / Catalogue / Locations lists + four forms.
- Roles chips; Preferred badge; Busy/Open; GST %; toast copy.
- Persist via existing master/vendor/location paths; hide unsavable fields per Missing APIs.
- **Done when:** + New … opens form; save returns with exact toast.

### Phase 6 — Budget / Issue (screens 11–12)
- Versioned target card; Issue severity chips + register variance fixture copy.
- **Done when:** CTAs/Why match Figma.

### Phase 7 — Hub wiring + iOS parity
- Android hub tiles open Phases 1–6 (Sale → POS; Hold/Transfer/Reprint utilities; masters).
- iOS: mirror every sheet under `Retail/`; confirm `OperatingExperienceHost` retail kit.
- Hero tagline: **Stock. Sell. Settle.**

### Phase 8 — Visual QA pass
- Side-by-side Figma screenshot vs device for all 23 on both platforms.
- Fix spacing, chip active states, keypad, Why cream, CTA ink.

#### Phase 8 completion (2026-11-10)

Fresh audit of all 23 Figma nodes (`TzLvwVwlPbeVB8ug1zB3GM`) against Android + iOS retail kits. Both platforms reviewed.

**Fixed (high/medium):**
- Android `RoseStickyFooter`: QA/POS/HTR CTAs → white ink + slate `#CBD5E1` caveat (was dark `inkOnRose` + amber); Supplier form keeps dark ink via `formStyle`.
- iOS Issue CTAs → `Post approval` / `Post update` (was `Submit …`).
- iOS hub Phase 7 polish: Order caption `Optional customer`; Hold `Park bill` gated on `SALE` (was `ORDER`); Transfer/Reprint captions aligned to Android; utility trailing `Hold · move · reprint`.
- iOS Supplier form sticky footer `formStyle` dark ink to match Figma `2506:29612`.
- Android Supplier form caveat → `Supplier saved · available in Purchase Order`.

**QA matrix (screen → status):**

| # | Screen | Android | iOS |
|---|--------|---------|-----|
| 1 | POS Hold recall `2438:29655` | pass | pass |
| 2 | POS Quick amount `2439:29655` | fixed (CTA ink) | pass |
| 3 | POS Scan cart `2421:29655` | fixed (CTA ink) | pass |
| 4 | Record Purchase `2399:30130` | fixed (CTA ink) | pass |
| 5 | Record Collection `2399:30194` | fixed (CTA ink) | pass |
| 6 | Record Payment `2399:30253` | fixed (CTA ink) | pass |
| 7 | Purchase Order `2443:29784` | fixed (CTA ink) | pass |
| 8 | Sales Order `2399:30418` | fixed (CTA ink) | pass |
| 9 | Stock Update `2399:30359` | fixed (CTA ink) | pass |
| 10 | Status / Assignment `2399:30482` | fixed (CTA ink) | pass |
| 11 | Budget / Target `2399:30539` | fixed (CTA ink) | pass |
| 12 | Issue / Approval `2399:30597` | fixed (CTA ink) | fixed (CTA copy) |
| 13 | Suppliers `2506:29417` | pass | pass |
| 14 | Invite & QR `2506:29333` | pass | pass |
| 15 | Catalogue `2506:29475` | pass | pass |
| 16 | Locations `2506:29555` | pass | pass |
| 17 | Supplier form `2506:29612` | fixed (caveat + formStyle) | fixed (formStyle) |
| 18 | Location form `2506:29703` | pass | pass |
| 19 | Product form `2506:29782` | pass | pass |
| 20 | Stock item form `2506:29907` | pass | pass |
| 21 | Park bill `2508:29333` | fixed (CTA ink) | pass |
| 22 | Stock transfer `2508:29447` | fixed (CTA ink) | pass |
| 23 | Reprint receipt `2508:29537` | fixed (CTA ink) | pass |
| Hub | Quick Adds tiles / Why / tagline | pass | fixed (tiles + Hold gate) |

**Deferred blockers (honesty / API — no invent):**
- Register / shift / fiscal bill-number APIs — chrome labels stay location-derived; no fake posted bill nos.
- ~~Cross-device Hold persistence — in-session park only~~ → **Resolved (device-local):** `RetailHoldSession` (SharedPreferences, company+moment) + `RetailParkedSaleStore` (UserDefaults) survive process death; HOLD-n sequential; honesty that parks are not cross-device until register API exists. Still no server holds API.
- Transfer from/to STOCK schema — UI matches Figma; note-only when master ids unavailable.
- Split tender multi-leg ledger — chip selectable; primary tender + note when payload cannot split.
- Camera barcode — Scan UX may fall back to manual SKU search.
- ~~Expense hub tile — outside 23-node batch; left alone~~ → **Resolved (kept):** rose money-QA chrome + “Running cost” caption / honesty Why; not removed.

**Open-blocker resolution note (2026-11-11 re-audit):**
1. **Hold persistence** — device-durable local store both platforms; clear on recall/charge; no invented fiscal bill nos.
2. **Reprint** — shareable duplicate text receipt via system share sheet from posted SALE fields; labeled client re-render / duplicate copy; no PDF allocation API invented.
3. **GST display** — estimate rows from master/line rates when rate+amount available; Unavailable when missing; Why still says GST posts only on Charge/Purchase with real tax config.
4. **Utilities · 3** — kept muted (Figma hub omits intentionally) for HTR discovery; HTR sheets retained.
5. **Expense** — kept; aligned to rose money capture honesty (running cost; payment separate).

---

## 6. Content / copy to match exactly (key strings)

**Brand / chrome**
- Dimmed header: `Harbor Mart Pulse` / `Stock. Sell. Settle.`
- POS context: `Register 2 · Shift open · Andheri · Harbor Mart`
- Footer draft (QA family): `Draft preserved if you close or open a picker`
- Toast: `✓ Saved · sheet closes and returns back`

**POS**
| Mode | CTA | Why (start) |
|------|-----|-------------|
| Scan cart | Charge ₹… · Post & print | POS posts one itemized sale + linked tender receipt atomically… |
| Quick amount | Charge ₹… · Post | Quick amount posts revenue + linked tender with zero SKU movement… |
| Hold recall | Recall HOLD-18 → Scan cart | Hold is draft cart JSON only — no revenue, no stock, no tender… |
| Caption (scan) | Idempotent post · draft kept on picker · original QA-01 untouched | |
| Caption (quick) | No stock move · draft kept on mode switch | |
| Caption (recall) | Does not post · Charge still required on Scan cart | |

**Purchase / Collection / Payment**
- Purchase Why: `Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost.`
- Collection Why: `Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue.`
- Payment Why: `Settles open AP only — never posts new purchase cost.`

**Orders / stock / status**
- PO Why: `Purchase order is commitment only — no AP, no stock, no GST posting…`
- SO Why: `Sales order is commitment only — revenue + stock on posted Sale / POS Charge…`
- Stock Why: `Quantity delta only — never invent unit cost or BOM. Damage uses Reduce + reason.`
- Status Why: `Operational state only — does not post revenue or stock by itself…`

**Masters Why / CTAs**
- Invite: `Invite grants a role and its access. Joining ≠ approval ≠ payment.` / CTA `Done`
- Suppliers: GSTIN drives input credit… / `Save supplier`
- Catalogue: GST badge is product rate from HSN — output GST on bill / POS Sale… / `Done`
- Locations: Locations are stock and sale points… / `Save locations`
- Forms: `Save supplier` / `Save location` / `Save product` / `Save stock item`

**Invite roles (Figma):** Owner, Manager, Cashier, Stock, Floor  
**Catalogue categories:** Staples, Snacks, Personal care, Beverages, Home care  
**HTR CTAs:** `Park bill · HOLD-22` / `Confirm transfer · 32 units` / `Reprint · duplicate copy`

Demo names (Harbor Mart, Basmati 5kg, Wholesale Foods, etc.) are **fixtures for visual QA**; production binds projection/masters data.

---

## 7. Risks / gaps

| Risk | Impact | Mitigation |
|------|--------|------------|
| No register/shift/holds/receipt PDF APIs (`RETAIL_MISSING_APIS.md`) | Cannot persist parks across devices or allocate fiscal bill numbers | Device-local durable holds (not cross-device); register strip from location/title; reprint via shareable client re-render; never invent bill numbers as posted facts |
| Transfer lacks from/to on `STOCK` schema | Inter-location transfer may degrade to single adjustment | UI matches Figma; post best-effort STOCK with note; track API ask; honesty in Why |
| Split tender / cash drawer | Figma Split chip | Allow chip selection; if payload cannot split, post primary tender + note; no fake multi-leg ledger |
| Invite roles vs invite schema | Figma Cashier/Stock/Floor vs FINANCE/APPROVER/… | Keep Figma labels in UI; map to schema on mint (existing Missing APIs behaviour) |
| Category / MRP / variant fields | Forms cannot persist all Figma fields | Capture only schema-backed fields; omit or Unavailable extras |
| Camera barcode | Scan button UX | Platform scanner or manual SKU search fallback — never block Charge |
| Nested sheets (masters → form → browser) | Back-stack bugs | One create sheet at a time; mirror restaurant nested scrim |
| iOS/Android drift | Parity failure | Android-first visual lock, then port strings/layout deltas |
| Expense not in 23 | Accidental scope creep | Leave Expense unless shared chrome forces a touch |

---

## 8. Recommended next step (after approval)

1. **Approve this plan** (confirm Android-first then iOS, same as Service).
2. **Start Phase 0 + Phase 1 on Android** (token lock + POS three-mode visual parity on `RetailPosSheet`).
3. For each screen, call Figma `get_design_context` + `get_screenshot` with `skillNames: figma-design-to-code` immediately before coding that screen.
4. Execution method: **Subagent-driven** (POS + GST boundaries + multi-sheet interdependence; independent review per phase reduces revenue/stock regressions).

---

## Task checklist (bite-sized)

### Task 1: Token lock
**Files:** `RetailRoseTokens.kt`; iOS `BusinessRetail*.colorset`  
**Interfaces:** Consumes Figma hex table §2; Produces locked `RetailRose` / asset hex for all later tasks.  
- [ ] Diff Figma hex table vs code; add `whyBody` if needed  
- [ ] Screenshot Scan cart chrome preview matches Figma  
- [ ] Commit `fix(tokens): align retail rose palette with RTL Figma`

### Task 2: Android POS Sale modes (1–3)
**Files:** `RetailPosSheet.kt`, `RetailSheetChrome.kt`  
**Interfaces:** Consumes `RetailHoldSession`, `RetailEventDraft`; Produces Scan/Quick/Recall UI + SALE posts.  
- [ ] Mode chips, keypad, cart, tender, CTAs/Why exact  
- [ ] Quick amount posts with no lines; Scan cart with lines  
- [ ] Visual check vs `2421:29655`, `2438:29655`, `2439:29655`

### Task 3: Android HTR utilities (21–23)
**Files:** `RetailUtilitySheets.kt`  
**Interfaces:** Consumes/produces park session shared with Task 2; Transfer/Reprint navigation via `RetailUtilityNav`.  
- [ ] Park / Transfer / Reprint ticket UIs + mode chips  
- [ ] Why/CTA exact; Reprint does not post new SALE  
- [ ] Visual check vs `2508:29333`, `29447`, `29537`

### Task 4: Android money QAs (4–6)
**Files:** `RetailCaptureSheets.kt` (Purchase/Collection/Payment)  
- [ ] Layout + Why/CTA lock  
- [ ] Visual check vs `2399:30130`, `30194`, `30253`

### Task 5: Android SO/PO/Stock/Status (7–10)
**Files:** `RetailCaptureSheets.kt`  
- [ ] Browsers + commitment copy; Stock/Status modes  
- [ ] Visual check vs `2443:29784`, `2399:30418`, `30359`, `30482`

### Task 6: Android masters + forms (13–20)
**Files:** `RetailQuickAdds.kt` (masters sheet), `RetailMasterForms.kt`, `BusinessInviteQr.kt`  
- [ ] Lists + forms Figma-faithful; toast copy  
- [ ] Visual check vs `2506:*` nodes

### Task 7: Android Budget/Issue + hub wiring (11–12)
**Files:** `RetailCaptureSheets.kt`, `RetailQuickAdds.kt`  
- [ ] Copy lock; hub tiles open correct sheets  
- [ ] Visual check vs `2399:30539`, `30597`

### Task 8: iOS Retail kit parity
**Files:** `Shell/BusinessActive/Retail/*`, host routing  
**Interfaces:** Mirror Android sheet set under rose theme; same CTAs/Why.  
- [ ] POS + HTR + captures + masters  
- [ ] Side-by-side with Android + Figma

### Task 9: End-to-end POS + visual QA
- [x] Walk Hub → all 23 on both platforms (Figma screenshot + code copy/token audit; CTA ink + hub polish fixed)  
- [x] Prove paths: Charge itemized, Quick amount no stock, Park→Recall→Charge, Transfer no sale, Reprint no duplicate revenue (semantics retained; register/receipt APIs still deferred)  
- [ ] Commit per phase; no mega-commit

---

## Architecture decision (locked)

**Chosen approach:** Figma parity + full POS path hardening on the **existing** Retail kit (Android reference → iOS), specializing POS/masters/HTR the same way Service specialized against Restaurant — not a greenfield design system, and not Trading Hold/Transfer reuse.

**Trade-off:** More retail-specific sheet code than “one generic OperatingCaptureSheet,” but matches Figma fidelity and POS/GST/stock semantics; parallel to proven Service and Restaurant splits. Missing register/receipt APIs stay honest UI + session holds rather than fabricated ledgers.
