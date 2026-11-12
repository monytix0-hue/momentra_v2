# Distribution Quick Add Figma Parity Implementation Plan

**Goal:** Bring Distribution Quick Adds (DST-QA-01…12 + HOLD/TRANSFER/REPRINT + masters) to Figma parity on APK + iOS, mirroring the Trading TRD-QA approach (catalog + capture chrome + chip routing).

**Figma:** `TzLvwVwlPbeVB8ug1zB3GM` · session inventory 2026-11-10.

## Color palette (from DST-QA-02 design context)

| Token | Hex / value | Use |
|-------|-------------|-----|
| Sheet surface | `#13203A` | Bottom sheet fill (not Trading `#151D2E`, not Group lilac) |
| Field / chip idle | `#1A2A4A` | Inputs, idle chips |
| Underlying / peek | `#0B1226` | Dimmed pulse under sheet |
| Accent | `#EA580C` | Selected chips, CTA, amount border, INR pill |
| Accent soft fill | `rgba(234,88,12,0.15)` | Currency pill bg |
| Amount border | `rgba(234,88,12,0.33)` | Amount-first hero stroke |
| Context / WHY cyan | `#38BDF8` | Context line + WHY label (sky; Trading uses `#06B6D4`) |
| WHY fill / border | `rgba(56,189,248,0.08)` / `0.30` | WHY panel |
| WHY body | `#CFEFF6` | WHY text |
| Label | `#8B97AD` | Field labels |
| Hint | `#6B7790` | Hints / sticky meta |
| Chip idle text | `#C9D2E3` | Unselected chip text |
| Value | `#F1F5F9` | Input values |
| Ink | `#FFFFFF` | Titles |

**Rule:** Distribution must **not** use Group lilac or Trading emerald.

## Frame inventory (27 listed nodes)

### Core DST-QA (implement first — shared with Trading layout)

| Code | Title | nodeId | Notes |
|------|-------|--------|-------|
| DST-QA-01 | Record Sale | `2376:30678` | Listed id `2376:30751` was home-indicator fragment |
| DST-QA-02 | Record Purchase | `2376:30752` | |
| DST-QA-03 | Record Collection | `2376:30821` | |
| DST-QA-04 | Record Payment | `2376:30875` | |
| DST-QA-05 | Record Expense | `2376:30928` | |
| DST-QA-06 | Stock Update | `2376:30991` | Transfer chip → TRANSFER |
| DST-QA-07 | Return / Credit Note | `2376:30336` | |
| DST-QA-08 | Budget / Target Revision | `2376:30400` | |
| DST-QA-09 | Post Update / Issue | `2376:30454` | Approval chip → APPROVAL |
| DST-QA-10 | Approval / Reimbursement | `2376:30505` | Reimburse → PAYMENT |
| DST-QA-11 | Order | `2376:30562` | Delivery chip → SHIPMENT |
| DST-QA-12 | Delivery | `2376:30619` | Hold DO → HOLD |

### Ops extensions

| Code | Title | nodeId |
|------|-------|--------|
| DST-QA-HOLD | Delivery order hold | `2559:29336` |
| DST-QA-TRANSFER | Stock transfer | `2559:29450` (listed `2559:29451` = underlying) |
| DST-QA-REPRINT | Reprint invoice / challan | `2559:29540` |

### Masters / forms (phase 2 — defer if needed)

| Frame | nodeId |
|-------|--------|
| Invite & QR | `2558:29336` |
| Principals & dealers | `2558:29420` |
| Catalogue & SKU | `2558:29478` |
| Warehouses & routes | `2558:29560` |
| New / Edit Principal or Dealer | `2558:29617` |
| Add / Edit Warehouse or Route | `2558:29708` |
| Create SKU | `2558:29787` |
| Create price list | `2558:29912` |

### Pickers / chrome (phase 3 — defer)

| Frame | nodeId |
|-------|--------|
| SKU browser | `2559:29630` |
| Date picker | `2560:29770` |
| Month picker | `2560:29894` |
| Date & time picker | `2560:29951` |
| Switch Moment | `2560:30025` |

## Copy catalog (core money/ops)

Brand prefix: `{Brand} Distribution · ` (blank → `"Distribution"`).

Context suffixes + WHY + CTAs match Trading Figma table (verified identical on DST-QA-01/02/07). HOLD/TRANSFER/REPRINT Dist-specific WHY from frames:

| Mode | Dist WHY (Figma) |
|------|------------------|
| HOLD | Hold pauses a delivery order. No dispatch, no stock move and no GST; resume continues the same DO. |
| TRANSFER | Transfer moves stock between warehouses at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN (inter-state branch transfer needs an e-way bill). |
| REPRINT | Reprint re-renders the posted invoice with the same number and GST split (IGST for inter-state dealers). No new sale, no stock or AR change. |

More details: `More details · notes, attachments, tags`

## Architecture (mirror Trading)

1. `OperatingCaptureChromeTokens.Distribution` — orange/slate tokens above.
2. `DistributionQuickAddCopy` (APK + iOS) — mirrored catalog.
3. `variant=distribution` selects Dist chrome; layout gates use `opsQa = trading || distribution`.
4. Chip routing for Dist same as Trading (`quickAddHint` / `onRouteToMode`).
5. API enums unchanged.
6. Do not alter Marketing / Retail / Restaurant / Service / Trading emerald.

## Implementation order

1. Plan (this doc) + palette lock  
2. Chrome tokens + copy catalogs + unit tests  
3. APK: chrome provider, opsQa layout, Dist copy, chip routing  
4. iOS: theme Dist capture tokens, Dist copy, reuse tradingBody under `isOpsQaCapture`  
5. Compile APK (Temurin 21)  
6. List deferred masters/pickers  

## Phase 2 — masters / pickers / Switch Moment (2026-11-10 follow-up)

**Palette for chrome sheets (not pulse canvas):** sheet `#13203A`, field `#24365A`, border `#2A3D63`, accent `#EA580C`.

| Frame | nodeId | Status |
|-------|--------|--------|
| Invite & QR | `2558:29336` | Dist roles (Route lead / Warehouse / Salesman / Manager) + QA sheet palette |
| Principals & dealers | `2558:29420` | Dist titles / WHY / `+ New principal / dealer` / `Save party` |
| Catalogue & SKU | `2558:29478` | Dist titles / CTAs |
| Warehouses & routes | `2558:29560` | Dist titles / CTAs |
| New/Edit Principal or Dealer | `2558:29617` | Dist form heading + Save party |
| Add/Edit Warehouse or Route | `2558:29708` | Dist form heading + Save CTA |
| Create SKU | `2558:29787` | Dist form heading + Save SKU |
| Create price list | `2558:29912` | Dist titles / CTAs |
| HOLD / TRANSFER / REPRINT | `2559:29336`, `2559:29450`, `2559:29540` | Shipped in phase 1 (listed `2559:29451` = underlying fragment) |
| SKU browser | `2559:29630` | Dist Catalogue copy + category chips |
| Date / month / datetime pickers | `2560:29770`, `29894`, `29951` | Inherit Dist capture chrome (no Group lilac) |
| Switch Moment | `2560:30025` | Dist title/subtitle + QA sheet palette |

## Status (2026-11-10)

**Shipped**

- Palette locked (Dist orange `#EA580C` / sheet `#13203A` / sky WHY `#38BDF8`)
- `DistributionQuickAddCopy` APK + iOS + unit tests
- `DistributionMasterCopy` APK + iOS (masters / SKU browser / Switch Moment)
- `OperatingCaptureChromeTokens.Distribution` + `isOpsQa()`; iOS `isDistributionCapture` / `isOpsQaCapture`
- Hub `variant=distribution`; chip routing + Dist WHY/CTA/context on DST-QA-01…12 + HOLD/TRANSFER/REPRINT
- Dist QA sheet tokens wired into chrome SideEffect (`DistQaSheet` / `DistQaField` / `DistQaBorder`)
- Capture date/month/datetime pickers themed from active capture chrome
- SKU browser Dist labels; Switch Moment Dist copy
- Masters Dist titles/notes/CTAs/invite roles on APK + iOS

**Remaining gaps (minor)**

| Area | Notes |
|------|-------|
| Pixel-perfect master form field order | Core fields present; Dist Figma has extra category/pack chips not yet as separate UI controls |
| Shell MomentSwitcher (iOS global) | Dist-specific subtitle lives on APK `OperatingSwitchMomentSheet`; iOS shell switcher is shared |
| Listed `2559:29451` | Underlying/dimmed fragment of TRANSFER — not a separate sheet |
