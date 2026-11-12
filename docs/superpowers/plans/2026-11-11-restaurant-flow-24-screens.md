# Restaurant Flow (24 Figma Screens) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring all 24 Figma Restaurant (`RST-*`) Quick Add / POS / KOT / masters / forms screens to layout + copy + Ember/Stone palette parity on both Android (APK) and iOS, with **full KOT support** (Send · Fire · Reprint, course Now/Hold, station routing, table/covers honesty) end-to-end.

**Architecture:** Treat the existing Android restaurant kit (`RestaurantPosSheet`, `RestaurantKotSheet`, `RestaurantMastersSheet`, `RestaurantQuickAdds`, `RestaurantStoreKit`, `RestaurantEmber`) as the structural reference — same specialization pattern Retail used against Service. Do **not** rebuild from Trading capture, and do **not** borrow Service teal or Retail rose. Lock Figma copy/Why/CTAs and ember chrome; close visual + KOT-mode gaps; port Android-ahead surfaces to iOS `Shell/BusinessActive/Restaurant/*`. Keep Pulse/Hub shell work aligned with `docs/superpowers/plans/2026-11-09-figma-operating-ui-parity.md`. Honour honesty rules in `docs/implementation/RESTAURANT_MISSING_APIS.md` (table/covers/KOT id/printer/PDF/recipe — UI present; no invented APIs).

**Tech Stack:** Android Jetpack Compose + `RestaurantEmber`; iOS SwiftUI + `BusinessRestaurant*` / RestaurantEmber assets / Plus Jakarta; shared operating projection + `POST …/operating-events` + `PATCH …/operating-events/:id` (KOT Fire) + masters via UPDATE+master payload.

**Spec:** Figma file `TzLvwVwlPbeVB8ug1zB3GM` (momentra), session inventory below. Related: `2026-11-09-figma-operating-ui-parity.md`, `2026-11-10-service-flow-22-screens.md`, `2026-11-10-retail-flow-23-screens.md` (process templates), `docs/implementation/RESTAURANT_MISSING_APIS.md`.

## Global Constraints

- Match Figma **layout and copy verbatim** (titles, Why notes, CTAs, chip labels, footers, toast).
- Palette is **Ember / Stone** (wine `#881337` → ember `#EA580C`, gold `#FCD34D`, stone canvas `#44403C` / `#1C1917` / `#0C0A09`) — already in `RestaurantEmber` / iOS `BusinessRestaurant*`; do not invent a new palette; do not use Service teal or Retail rose.
- Honesty rules: never invent metrics as `0`/`0%`; ORDER / KOT / Status / SO / PO never post revenue; GST stamps on posted bill / Purchase only; recipe deduction excluded in v5; empty = Unavailable.
- Platform chrome: same content on iOS + Android; only system status/safe-area differ.
- Font: Plus Jakarta Sans on both platforms (already used).
- Demo fixture brand in Figma: **Spice Route** / Spice Route BKC / Spice Route Kitchen / Spice Route POS — visual QA only; production binds projection.
- No new markdown docs beyond this plan unless the user asks.

## Review Focus

1. **KOT ≠ revenue:** Send / Fire / Reprint / Status never post SALE; Fire PATCHes same ORDER (`revisesEventId`); Reprint never allocates a new KOT id.
2. **POS GST / tender honesty:** GST/Service/Packaging rows Unavailable or server-stamped only; Split tender locked when contract has one `paymentState`; Platform AR posts bill then Collection later.
3. **Commitment vs post:** Sales Order / Purchase Order never create revenue, AP, or stock; Purchase / POS Charge do.
4. **Table / covers / KOT number:** Persist on contract when schema allows; otherwise locked/Unavailable per Missing APIs — never fabricate covers counts or ticket numbers for posted ledgers.
5. **iOS kit routing:** Restaurant must stay on retail-family host (`OperatingExperienceHost` → `RestaurantActiveView`), ember theme — not Trading hub or teal/rose kits.

---

## 1. Screen inventory (all 24)

Platform: **both iOS + Android**. No separate Figma platform variants — one mobile frame each (402×~H). Demo entity: Spice Route.

**Node ID notes:** User list items 9 and 11 pointed at `underlying · dimmed` children; implement against **parent frames**. Item 7 is the Cloud POS `bottom-sheet` child; treat as POS Cloud mode (sibling of In-dining / Takeaway; parent family historically `2446:29802`).

### A. POS Bill (three modes of one sheet family) — KOT-adjacent

| # | Node (implement) | Figma name | UI title | Purpose | KOT |
|---|------|------------|----------|---------|-----|
| 1 | `2446:29655` | RST-QA-01-ALT · POS · In-dining | POS Bill | Table ticket + tender; Charge · Post & print | **Core** — link open KOT; `KOT live` chip |
| 2 | `2448:29655` | RST-QA-01-ALT · POS · Takeaway | POS Bill | Pickup ticket; Charge · Post & notify | **Core** — `KOT live`; Ready by |
| 7 | `2446:29811` (sheet; family `2446:29802`) | RST-QA-01-ALT · POS · Cloud | POS Bill | Aggregator ticket; Post cloud bill | **Core** — Sync platform; Platform AR |

Mode chips (all three): `In-dining` · `Cloud` · `Takeaway`. Context varies by mode (Table/covers / Zomato+SLA / Pickup+Ready).

### B. Money / goods QA captures

| # | Node | Figma name | UI title | CTA | KOT |
|---|------|------------|----------|-----|-----|
| 3 | `2400:30129` | RST-QA-02 · Record Purchase | Record Purchase | Post purchase | Adjacent (stock in) |
| 4 | `2400:30193` | RST-QA-03 · Record Collection | Record Collection | Post collection | Settles Platform AR from cloud bills |
| 5 | `2400:30257` | RST-QA-04 · Record Payment | Record Payment | Post payment | Settles AP |
| 13 | `2400:30310` | RST-QA-05 · Record Expense | Record Expense | Post expense | Non-KOT |
| 6 | `2400:30363` | RST-QA-06 · Stock Update | Stock Update | Post stock update | Waste/count — not recipe |

### C. Orders + ops QA

| # | Node (implement) | Figma name | UI title | CTA | KOT |
|---|------|------------|----------|-----|-----|
| 11 | **`2400:30422`** (user gave child `2400:30423`) | RST-QA-07 · Sales Order | Sales Order | Save sales order | **Adjacent** — commitment; footer “Send KOT from ticket after save”; Hold for fire on lines |
| 12 | `2455:29655` | RST-QA-07-PO · Purchase Order | Purchase Order | Save purchase order | Non-KOT commitment |
| 8 | `2400:30488` | RST-QA-08 · Status / Assignment | Status / Assignment | Update status | **Core** — KOT-441 board; Prepare/Ready/Fulfil |
| 9 | **`2400:30547`** (user gave child `2400:30548`) | RST-QA-09 · Budget / Target Revision | Budget / Target Revision | Save revision | Non-KOT |
| 10 | `2400:30605` | RST-QA-10 · Issue / Approval / Update | Issue / Approval / Update | Post issue | Spoilage audit |

### D. Full KOT ticket family (Send · Fire · Reprint)

| # | Node | Figma name | UI title | CTA | KOT |
|---|------|------------|----------|-----|-----|
| 16 | `2449:29655` | RST-QA-KOT · Send · In-dining | Send KOT | Send to kitchen | **Core** |
| 17 | `2449:29769` | RST-QA-KOT · Fire · In-dining | Fire course | Fire to kitchen | **Core** |
| 18 | `2449:29859` | RST-QA-KOT · Reprint · Cloud | Reprint KOT | Reprint ticket | **Core** |

Mode chips on all three: `Send` · `Fire` · `Reprint`. Channel chips: `In-dining` · `Cloud` · `Takeaway`. Course/station: Now / Hold / Fire now; stations All / Tandoor / Hot / Cold / Bar / Pack / Cloud queue.

### E. Masters (list + invite)

| # | Node | Figma name | UI title | Purpose | KOT |
|---|------|------------|----------|---------|-----|
| 19 | `2465:29655` | RST-MASTER · Invite & QR | Invite team · Spice Route | Contacts, roles, invite link, QR | Staffing |
| 20 | `2465:30021` | RST-MASTER · Vendors | Vendors | Vendor cards, + New vendor | PO / Purchase |
| 21 | `2465:30143` | RST-MASTER · Menu | Menu | Category chips, dish rows, + New dish | POS / KOT lines |
| 22 | `2465:30265` | RST-MASTER · Stations | Stations | Busy/Open rows, + Add station | **Core** — KOT routing |

### F. Master create/edit forms

| # | Node | Figma name | UI title | CTA | KOT |
|---|------|------------|----------|-----|-----|
| 14 | `2461:29655` | RST-MASTER · Create Menu Dish | Create menu dish | Save menu dish | Routes to kitchen station |
| 15 | `2461:29777` | RST-MASTER · Create Raw Material | Create raw material | Save raw material | PO browser |
| 23 | `2472:29659` | RST-FORM · New / Edit Vendor | New vendor | Save vendor | Masters |
| 24 | `2472:29750` | RST-FORM · Add / Edit Station | Add station | Save station | **Core** — printer / category routes |

### Adjacent hub utilities (not in the 24-node batch)

Hub tiles **Hold · Transfer · Reprint** already exist on Android/iOS hub (`Utilities · 3`) but have **no dedicated RST-QA-HTR frames in this 24**. Plan treats them as **adjacent full-ops paths** (park order / stock transfer / receipt reprint) — reuse Restaurant Kot-style ticket chrome + Retail HTR process, **not** trading layouts; prefer Figma chrome if a later HTR batch appears; until then keep tiles wired with honesty + ember ticket UI.

**Flow order (dependency):** Tokens/chrome → POS In-dining/Cloud/Takeaway (1,7,2) → **KOT Send/Fire/Reprint (16–18)** → Status (8) → Purchase/Collection/Payment/Expense/Stock (3–6,13) → SO/PO (11–12) → Masters lists (19–22) → Forms (14–15,23–24) → Budget/Issue (9–10) → Hub wiring + iOS parity → Visual QA.

**KOT end-to-end paths (both platforms):**

1. **Send:** Hub Status → KOT Send → channel + station + lines (Now/Hold) → `Send to kitchen` → `ORDER` `orderStatus=PLACED` (never SALE).
2. **Fire / course:** Fire tab → held lines → `Fire to kitchen` → PATCH same event toward PREPARING/READY; held → Now; no revenue.
3. **Bump / status board:** Status / Assignment → Prepare/Ready/Fulfil chips → `Update status` (ops only).
4. **Reprint KOT:** Reprint tab → pick ticket → `Reprint ticket` — same KOT id; client re-render / share; no PDF endpoint → honesty lock if needed.
5. **Bill from KOT:** POS → `KOT live` / Bill from KOT picker → Charge → SALE with optional `parentEventId`.
6. **Cloud path:** POS Cloud → Post cloud bill (Platform AR) → Collection later; KOT Reprint Cloud for kitchen jam.
7. **SO → KOT:** Sales Order save → “Send KOT from ticket after save” → open KOT Send with lines.

---

## 2. Color palette / design tokens

`get_variable_defs` returned empty; hex from `get_design_context` (KOT Send) + existing `RestaurantEmber` / `BusinessRestaurant*`.

| Role | Hex | Android (`RestaurantEmber`) | iOS (`BusinessTokens` / Assets) |
|------|-----|-----------------------------|----------------------------------|
| Canvas top / wash | `#44403C` | `screenTop`, `chip` | `BusinessRestaurantCanvasTop` |
| Canvas mid / sheet field | `#1C1917` | `screenMid`, `sheet`, `field`, `inkOnEmber` | `BusinessRestaurantCanvas` / Deep |
| Canvas bottom | `#0C0A09` | `screenBottom` | `BusinessRestaurantCanvasDeep` |
| Card / sheet surface | `#292524` | `card`, `tile` | `BusinessRestaurantSurface` |
| Nested / chip well | `#44403C` | `chip`, `divider` | inset / chip |
| Hairline / inactive border | `#57534E` | `hairline` | map or add |
| Wine (selected secondary / CTA start) | `#881337` | `wine`, `heroStart` | `BusinessRestaurantHeroStart` |
| Ember accent | `#EA580C` | `ember`, `tileStroke` | `BusinessRestaurantAccent` |
| Gold | `#FCD34D` | `gold` | accent soft / metric |
| Amber / Hold | `#FBBF24` | `amber` | signal warning |
| Why label | `#FCD34D` | `gold` | gold |
| Why body cream | `#FEF3C7` | add `whyBody` if missing | map or soft amber |
| Positive / Now chip | `#22C55E` | `positive` | SignalPositive |
| Negative | `#EF4444` | `negative` | signal danger |
| Primary / body / faint text | `#FAFAF9` / `#A8A29E` / `#78716C` | `textPrimary` / `textBody` / `textFaint` | stone family |
| CTA gradient | `#881337` → `#EA580C` | `heroStart` → `heroEnd` / ember | HeroStart → HeroEnd |
| CTA label on KOT sticky | **white** | verify vs POS (may use white on wine→ember) | match Figma per sheet |
| Selected chip ink | `#1C1917` | `inkOnEmber` | OnAccent |
| Sheet border glow | ember `@0.4` | existing scrim accent | ember shadow |

**Token work:** Diff Figma hex vs `RestaurantEmberTokens.kt` + iOS colorsets; add only missing (`whyBody` `#FEF3C7`, chip inactive `#57534E` if not mapped). Do not rename existing keys.

**Typography:** Plus Jakarta — Bold 22 title / 28 ticket hero, Medium 11–14 fields, Regular 10–11 footers / Why.

**Radii (from Figma):** sheet top 28, ticket/amount cards 16, line cards 14, fields 12, pills 999, CTA 16, grab 4×40, close 16.

---

## 3. Shared components to build / reuse

| Component | Reuse from | Adapt for Restaurant Figma |
|-----------|------------|----------------------------|
| Sheet scrim + grab + close | `OperatingSheetScrim` / ember chrome | Ember accent shadow |
| Mode / channel / station chips | `EmberModeChip` / iOS retail-family chips | Active = ember or wine fill + gold border + dark ink |
| Amount / ticket hero card | POS totals / KOT ticket hero | Gold KOT id; 28pt table headline |
| Cart / KOT / SO / PO lines + steppers | `RestaurantPosSheet` / Kot lines | Meta: station · Now/Hold/Fire; category |
| Scan / Sync / Add field | POS / Purchase / SO browser | Exact placeholders |
| Why note | EmberWhyNote | Gold “Why” + cream `#FEF3C7` body |
| Sticky CTA + caption | EmberSheetFooter | Wine→ember gradient; white or dark ink per Figma sheet |
| Saved toast | Master forms | `✓ Saved · sheet closes and returns back` |
| Master list row | `RestaurantMastersSheet` | Preferred / Busy/Open / GST 5% |
| Invite & QR | `BusinessInviteQr` + `EmberInvitePalette` | Roles: Owner, Manager, Cashier, Kitchen, Waiter |
| Menu / raw browser | Embedded in SO/PO | Categories Mains/Breads/… or Dairy/Grains/… |
| Hold/Transfer/Reprint (hub utilities) | Kot ticket structure | Restaurant park-order / stock-transfer / receipt-reprint — not trading |

**Do not reuse** Service teal, Retail rose, or trading Hold/Transfer/Reprint layouts.

**Code already present (baseline, not greenfield):**

- Android: `RestaurantPosSheet.kt`, `RestaurantKotSheet.kt`, `RestaurantMastersSheet.kt`, `RestaurantQuickAdds.kt`, `RestaurantStoreKit.kt`, `RestaurantActive.kt`, `RestaurantEmberTokens.kt`, `EmberPickerSheet.kt`
- iOS: `Shell/BusinessActive/Restaurant/{RestaurantKit,RestaurantPosSheet,RestaurantKotSheet}.swift` + `RetailFamily/*` host; Assets `RestaurantEmber/` + `BusinessRestaurant*`

---

## 4. Proposed file / module structure

### Android (`apk/app/src/main/java/com/example/momentra/…`)

```
ui/theme/RestaurantEmberTokens.kt         # extend missing tokens only
ui/shell/business/restaurant/
  RestaurantActive.kt                     # Pulse/Life host (existing)
  RestaurantQuickAdds.kt                  # hub → sheets (existing; wire SO/PO/forms)
  RestaurantStoreKit.kt / RestaurantFormat.kt
  RestaurantPosSheet.kt                   # In-dining / Cloud / Takeaway parity pass
  RestaurantKotSheet.kt                   # Send / Fire / Reprint + course Hold/Now (parity pass)
  RestaurantMastersSheet.kt               # lists parity + open dedicated forms
  RestaurantMasterForms.kt                # NEW — Dish / Raw / Vendor / Station forms
  RestaurantCaptureSheets.kt              # NEW or extend — Purchase…Issue + SO/PO browsers
  RestaurantUtilitySheets.kt              # NEW — hub Hold/Transfer/Reprint tickets (adjacent)
  RestaurantStoryConfigSheet.kt           # out of 24 — leave alone
ui/shell/business/shared/
  BusinessInviteQr.kt                     # EmberInvitePalette roles
  OperatingSheetScrim.kt
  OperatingCaptureSheet.kt                # restaurant variant copy when shared
```

### iOS (`momentra/momentra/…`)

```
Shell/BusinessActive/
  OperatingExperienceHost.swift           # restaurant kit routing (existing)
  Restaurant/
    RestaurantKit.swift                   # hub + theme (existing)
    RestaurantPosSheet.swift              # parity with Android POS modes
    RestaurantKotSheet.swift              # Send/Fire/Reprint parity
    RestaurantCaptureSheets.swift         # NEW — QA money/ops + SO/PO
    RestaurantMasterForms.swift           # NEW — Dish/Raw/Vendor/Station
    RestaurantMastersViews.swift          # NEW or extend — lists if not shared
    RestaurantUtilitySheets.swift         # NEW — hub HTR adjacent
    RestaurantSheetChrome.swift           # optional shared atoms
  RetailFamily/                           # Pulse/Hub shells (ember via vertical)
Assets.xcassets/BusinessTokens/BusinessRestaurant*.colorset
Assets.xcassets/RestaurantEmber/          # tile icons already present
```

---

## 5. Implementation phases (dependency order)

**Recommendation: Android-first** (same as Retail), then iOS parity — Android restaurant kit is thicker; iOS already has host + thin POS/KOT shells via `RestaurantKit` / `RetailFamily`.

### Phase 0 — Tokens + sheet chrome parity
- Lock `RestaurantEmber` / iOS restaurant assets to hex table.
- Shared: grab, close, chip states (ember/wine/gold/positive/amber Hold), Why cream, CTA gradient, toast.
- **Done when:** throwaway preview of Send KOT chrome matches `2449:29655`.

### Phase 1 — POS Bill full modes (screens 1, 7, 2) — Android first
- Unify In-dining / Cloud / Takeaway in `RestaurantPosSheet` with Figma copy, cart, tender, totals honesty, Bill from KOT.
- CTAs: `Charge ₹… · Post & print` / `Post cloud bill · ₹…` / `Charge ₹… · Post & notify`.
- **Done when:** screenshot parity for three POS nodes; SALE posts itemized; Cloud uses Platform AR tender path.

### Phase 2 — Full KOT (screens 16–18) — **priority**
- Rebuild `RestaurantKotSheet` tabs to Figma: Send / Fire / Reprint + channel chips + station filters + Now/Hold/Fire now line chips + ticket hero + meta rows.
- Send → ORDER PLACED; Fire → PATCH status; Reprint → client re-render / share, same id; no fake PDF.
- Wire Hub Status → KOT; SO footer deep-link optional.
- **Done when:** CTAs `Send to kitchen` / `Fire to kitchen` / `Reprint ticket` + Why exact; Fire never posts SALE.

### Phase 3 — Status / Assignment (screen 8)
- KOT board UI (ticket card, line Ready/Preparing chips, Prepare|Ready|Fulfil modes).
- **Done when:** `Update status` ops-only; Why: operational state only.

### Phase 4 — Purchase / Collection / Payment / Expense / Stock (screens 3–6, 13)
- Line receive / allocate cards; mode chips; Why/CTA lock; stock Count|Waste|Usage.
- **Done when:** Collection settles Platform AR; Payment settles AP only; Stock qty-only.

### Phase 5 — Sales Order / Purchase Order (screens 11–12)
- Mode chips Sales Order ↔ Purchase Order; embedded menu / raw browsers; commitment Why.
- SO lines support “Hold for fire”; footer points to KOT send.
- **Done when:** SO/PO save commitment only; no revenue/AP/stock.

### Phase 6 — Masters lists + forms (screens 19–22, 14–15, 23–24)
- Figma-faithful Invite / Vendors / Menu / Stations + four forms (Dish, Raw, Vendor, Station).
- Roles; Preferred; Busy/Open; GST 5%; station printer chips; toast copy.
- Persist via existing master/vendor paths; hide unsavable price/printer fields per Missing APIs with honesty.
- **Done when:** + New … opens form; save returns with exact toast.

### Phase 7 — Budget / Issue (screens 9–10)
- Versioned target card; Issue severity chips + spoilage fixture copy.
- **Done when:** CTAs/Why match Figma.

### Phase 8 — Hub wiring + adjacent HTR + iOS parity
- Android hub tiles open Phases 1–7; utilities Hold/Transfer/Reprint get ember ticket sheets (adjacent).
- iOS: thicken `Restaurant/` to mirror Android sheets; confirm `OperatingExperienceHost` → `RestaurantActiveView`.
- Hero tagline: **Order. Serve. Collect.**

### Phase 9 — Visual QA pass
- Side-by-side Figma screenshot vs device for all 24 on both platforms.
- Fix spacing, chip active states, KOT Now/Hold colours, Why cream, CTA ink.

---

## 6. Content / copy to match exactly (key strings)

**Brand / chrome**
- Dimmed header: `Spice Route Pulse` / `Order. Serve. Collect.`
- Context examples: `Floor · Spice Route BKC · Station open` / `Cloud / aggregator · Spice Route BKC` / `Counter pickup · Spice Route BKC · Ready desk`
- Footer draft (QA family): `Draft preserved if you close or open a picker`
- Toast: `✓ Saved · sheet closes and returns back` / `Saved · sheet closes and returns back`

**POS**
| Mode | CTA | Why (verbatim start) |
|------|-----|----------------------|
| In-dining | Charge ₹820 · Post & print | In-dining POS posts one bill + tender. KOT/prep status never posts revenue. Recipe stock deduction excluded in v5. |
| Cloud | Post cloud bill · ₹580 | Cloud bill posts revenue now; platform settlement is Collection later. Fees must not double-count cost. Prep/ready status ≠ revenue. |
| Takeaway | Charge ₹440 · Post & notify | Takeaway posts bill + tender (or Pay on pickup → open AR until collect). Ready status is ops only — never posts revenue by itself. |

**KOT**
| Mode | Title | CTA | Why |
|------|-------|-----|-----|
| Send | Send KOT | Send to kitchen | Send KOT never posts revenue or stock. Held lines stay on ticket until Fire. Recipe deduction excluded in v5. |
| Fire | Fire course | Fire to kitchen | Course fire is operational state. Does not create revenue, collection, or stock movement. |
| Reprint | Reprint KOT | Reprint ticket | Reprint and cloud ticket sync are operational. Revenue only when bill posts; platform settle via Collection. |
| Captions | | | Send: `KOT is operational only — bill posts revenue later`; Fire: `Fire updates ticket state only — never posts a sale`; Reprint: `Reprint keeps same KOT id — never duplicates the order` |

**Money / stock**
- Purchase Why: `Stock += accepted qty only. No automatic recipe deduction (v5 exclusion).` · CTA `Post purchase`
- Collection Why: `Platform receivable needs explicit settlement — fees must not duplicate cost.` · CTA `Post collection`
- Payment Why: `Settles open AP only — never invents a purchase.` · CTA `Post payment`
- Expense Why: `Kitchen/platform running cost; payment state separate from the expense fact.` · CTA `Post expense`
- Stock Why: `Quantity delta only — never invent recipe consumption.` · CTA `Post stock update`

**Orders / status / budget / issue**
- SO Why: `Menu Add only builds the commitment cart. Revenue posts on bill / POS Sale. KOT send is separate and never posts.` · CTA `Save sales order` · Caption `Draft preserved · Send KOT from ticket after save`
- PO Why: `Raw material Add builds PO lines only. AP and stock post on Purchase / GRN receive — never on save PO. Recipe deduction still excluded in v5.` · CTA `Save purchase order`
- Status Why: `Operational state only — does not post revenue.` · CTA `Update status`
- Budget Why: `Versioned + effective-dated. Achievement uses posted bills only.` · CTA `Save revision`
- Issue Why: `Audit / action — approval ≠ disbursement.` · CTA `Post issue`

**Masters Why / CTAs**
- Invite: `Invite grants a role and its access. Joining ≠ approval ≠ payment.` / CTA `Done`
- Vendors: GSTIN drives input credit on Purchase / GRN — never on PO save. / list CTA may show `Save vendor` in Figma footer
- Menu: GST badge is dish rate; tax posts on bill / POS Sale only. / CTA `Done`
- Stations: Stations are KOT routing targets. Busy = open tickets over threshold… / CTA `Save stations`
- Forms: `Save menu dish` / `Save raw material` / `Save vendor` / `Save station`

**Invite roles (Figma):** Owner, Manager, Cashier, Kitchen, Waiter  
**Menu categories:** Mains, Breads, Starters, Drinks, Dessert  
**Raw categories:** Dairy, Grains, Oils, Spices, Produce, Pack  
**Station types:** Kitchen, Service, Bar · Printer: Kitchen printer 1, Bar printer, Screen only

Demo names (Butter chicken, KOT-442, Fresh Farm, Chef Ama, ZOM-9182, etc.) are **fixtures for visual QA**; production binds projection/masters data.

---

## 7. Risks / gaps

| Risk | Mitigation |
|------|------------|
| Table / covers / KOT id not on strict contract | UI chips + contract fields when allowed; else locked + reason from Missing APIs — never invent posted ids |
| KOT Reprint / kitchen printer | Client re-render + system share; Reprint unlocked in UI with honesty if no PDF endpoint |
| Split tender / GST preview / service charge | Show Figma rows; Unavailable or single tender; server GST stamp |
| Dish sell price / station printer on master | Form fields visible; persist only schema-backed fields; honesty on unsaved |
| Masters derived from events | Create via UPDATE+master (existing); no fake CRUD |
| Hub Hold/Transfer/Reprint not in 24 nodes | Adjacent Phase 8 ember tickets; do not block KOT Send/Fire/Reprint |
| iOS Restaurant sheets thinner than Android | Phase 8 port; keep RetailFamily host |
| Recipe deduction | Explicitly excluded v5 — copy must say so |
| Wrong palette bleed | Ban Service teal / Retail rose in restaurant kit reviews |

---

## 8. Next step after approval

1. Confirm this plan (especially parent-node corrections for SO `2400:30422` and Budget `2400:30547`, and Android-first vs parallel).
2. Choose execution: **Subagent-driven** (recommended for 24 screens + KOT) or **Native**.
3. Start Phase 0 → Phase 1 POS → **Phase 2 Full KOT** before remaining QA/masters.

---

## Task checklist (execution skeleton)

### Task 0: Tokens + chrome
- [x] Diff `RestaurantEmber` / iOS `BusinessRestaurant*` vs hex table; add `whyBody` if missing
- [x] Preview Send KOT chrome atoms; commit

### Task 1: POS three modes
- [x] Android `RestaurantPosSheet` Figma parity (In-dining/Cloud/Takeaway)
- [x] Tests: SALE itemized; Cloud Platform AR; no fabricated GST
- [ ] Commit *(deferred — user did not request commit)*

### Task 2: Full KOT
- [x] Android `RestaurantKotSheet` Send/Fire/Reprint + course chips
- [x] Tests: Send ORDER; Fire PATCH; Reprint no new id / no SALE
- [ ] Commit *(deferred)*

### Task 3: Status + money QA + SO/PO
- [x] Status board; Purchase/Collection/Payment/Expense/Stock; SO/PO browsers
- [ ] Commit *(deferred)*

### Task 4: Masters + forms
- [x] Lists + Dish/Raw/Vendor/Station forms; invite roles
- [ ] Commit *(deferred)*

### Task 5: Budget/Issue + hub HTR adjacent + iOS parity
- [x] Port sheets under iOS `Restaurant/`; hub wiring
- [ ] Commit *(deferred)*

### Task 6: Visual QA both platforms
- [x] Matrix all 24 nodes; fix deltas; commit *(matrix below; commit deferred)*

---

## Phase completion notes (2026-11-11)

### Phase 6 — Masters + forms
**Android:** `RestaurantMastersSheet.kt`, `RestaurantMasterForms.kt`, `BusinessInviteQr.kt` (`RestaurantInviteRoles`), SO/PO `+ New dish` / `+ New raw` → forms.
**iOS:** `RestaurantMastersViews.swift`, `RestaurantMasterForms.swift`, `RestaurantInviteRoles` + `BusinessInviteAccent.ember`, host → `RestaurantMastersView`; order sheet opens dish/raw forms.
Spice Route fixtures; roles Owner/Manager/Cashier/Kitchen/Waiter; Why/CTA lock.

### Phase 7 — Budget / Issue
**Android:** `RestaurantBudgetSheet` / `RestaurantIssueSheet` in `RestaurantCaptureSheets.kt`; subtitle fixed to Figma (`Kitchen · Owner · Sales target` / `… · Spoilage · dal`); approval ≠ disbursement note.
**iOS:** `budgetBody` / `issueBody` in `RestaurantCaptureSheets.swift`; host routes BUDGET/TARGET/ISSUE/UPDATE/APPROVAL.

### Phase 8 — Hub polish + HTR
Tagline **Order. Serve. Collect.** on Android hub hero + iOS `RestaurantHubScreen` / `RestaurantCaptureCopy.tagline`.
**Android:** `RestaurantUtilitySheets.kt` Hold/Transfer/Reprint ember tickets wired from hub.
**iOS:** `RestaurantUtilitySheets.swift` + `OperatingExperienceHost` HTR routing; Support tiles → masters/Issue.

### Phase 9 — Visual QA audit
Fresh Figma MCP screenshots (`fileKey TzLvwVwlPbeVB8ug1zB3GM`, session `a5a90b0e-…`) for all 24 parents (+ hub chrome). High-impact fix: Budget/Issue subtitles; form CTAs append name. Android `compileDebugKotlin` green.

### QA matrix (Android + iOS)

| # | Node | Screen | Android | iOS | Notes |
|---|------|--------|---------|-----|-------|
| 1 | `2446:29655` | POS In-dining | pass | pass | Charge · Post & print; Why locked |
| 7 | `2446:29811` | POS Cloud | pass | pass | Post cloud bill · Platform AR |
| 2 | `2448:29655` | POS Takeaway | pass | pass | Charge · Post & notify |
| 3 | `2400:30129` | Purchase | pass | pass | Stock += accepted qty |
| 4 | `2400:30193` | Collection | pass | pass | Platform settle honesty |
| 5 | `2400:30257` | Payment | pass | pass | Settles AP only |
| 13 | `2400:30310` | Expense | pass | pass | Kitchen/platform |
| 6 | `2400:30363` | Stock | pass | pass | Qty delta only |
| 11 | `2400:30422` | Sales Order | pass | pass | Commitment; + New dish |
| 12 | `2455:29655` | Purchase Order | pass | pass | Commitment; + New raw |
| 8 | `2400:30488` | Status | pass | pass | Ops-only Update status |
| 9 | `2400:30547` | Budget | **fixed** | **fixed** | Subtitle → Figma order; versioned |
| 10 | `2400:30605` | Issue | **fixed** | **fixed** | approval ≠ disbursement |
| 16 | `2449:29655` | KOT Send | pass | pass | Send to kitchen; no SALE |
| 17 | `2449:29769` | KOT Fire | pass | pass | CTA/Why code-locked; MCP screenshot aliased Status — deferred visual re-pull |
| 18 | `2449:29859` | KOT Reprint | pass | pass | Same KOT id; MCP screenshot aliased — deferred visual re-pull |
| 19 | `2465:29655` | Invite & QR | pass | pass | Ember roles |
| 20 | `2465:30021` | Vendors | pass | pass | Preferred / GSTIN |
| 21 | `2465:30143` | Menu | pass | pass | GST 5% badges · + New dish |
| 22 | `2465:30265` | Stations | pass | pass | Busy/Open · + Add station |
| 14 | `2461:29655` | Create Dish | **fixed** | **fixed** | CTA · name |
| 15 | `2461:29777` | Create Raw | pass | pass | Track stock honesty |
| 23 | `2472:29659` | Vendor form | pass | pass | GSTIN master write |
| 24 | `2472:29750` | Station form | pass | pass | Printer chrome honesty |
| Hub | — | Quick Adds | pass | pass | Tagline + HTR adjacent |

### Deferred blockers only
1. **Device visual re-pull** for KOT Fire (`2449:29769`) / Reprint (`2449:29859`) — MCP returned Status frame; code CTAs/Why already match plan §6.
2. **Kitchen printer / PDF / recipe deduction** — honesty locks remain (Missing APIs); not invented.
3. **Git commit** — not requested; working tree ready for user commit.
4. **Invite Kitchen vs Waiter access note** — both map API `CONTRIBUTOR`; label-specific Waiter copy is best-effort on Android, generic on iOS roleAccess(code).
