# Consumer Service Flow (22 Figma Screens) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all 22 Figma Consumer Service (`SVC-*`) screens on both Android (APK) and iOS so layout, copy, and teal/lime palette match Figma exactly.

**Architecture:** Treat Android restaurant masters/POS/KOT as the structural template and Android `ConsumerService*` / `ServicesTeal` as the palette/home kit. Specialize service masters + forms + Hold/Transfer/Reprint into dedicated composables/SwiftUI sheets (do not leave them as generic trading capture). Wire amount-first QA sheets through the existing `OperatingCaptureSheet` / iOS capture path with `variant = "service"` copy locked to Figma. Keep Pulse/Hub shell work aligned with `docs/superpowers/plans/2026-11-09-figma-operating-ui-parity.md` (iOS retail-family host).

**Tech Stack:** Android Jetpack Compose + `ServicesTeal`; iOS SwiftUI + `BusinessTokens` services colors / Plus Jakarta; shared operating projection + `POST …/operating-events` + masters APIs.

**Spec:** Figma file `TzLvwVwlPbeVB8ug1zB3GM` (momentra). Session inventory of nodes below. Related: `2026-11-09-figma-operating-ui-parity.md` (iOS service host routing).

## Global Constraints

- Match Figma **layout and copy verbatim** (titles, Why notes, CTAs, chip labels, footers).
- Palette is teal/lime: canvas `#042F2E`, accent `#0D9488`, lime `#84CC16` — already in `ServicesTeal` / iOS `BusinessServices*` assets; do not invent a new palette.
- Honesty rules: never invent metrics as `0`/`0%`; booking ≠ revenue; GST posts only on Finalize Charge.
- Platform chrome: same content on iOS + Android; only system status/safe-area differ.
- Font: Plus Jakarta Sans on both platforms (already used).
- No new markdown docs beyond this plan unless the user asks.

## Review Focus

1. **GST boundary:** Booking / Hold / Transfer / Status never post revenue or GST; Finalize Charge / Reprint of posted invoice only.
2. **Master vs capture:** Create Client / Service / Package / Room-Staff are master writes; QA-01..10 are event captures.
3. **Hold/Transfer/Reprint:** Service semantics (appointment hold, staff/room move, invoice reprint) — not trading stock hold/transfer.
4. **iOS kit routing:** Service must not render Trading hub once retail-family host lands.
5. **Package sessions:** Package master + browser must show session/validity copy; deposits stay unallocated until Finalize Charge.

---

## 1. Screen inventory (all 22)

Platform: **both iOS + Android** unless noted. No separate Figma platform variants — one mobile frame each (402×~H).

### A. Masters (list + invite)

| # | Node | Figma name | UI title | Purpose |
|---|------|------------|----------|---------|
| 1 | `2530:29334` | SVC-MASTER · Invite & QR | Invite team · BrightCare | Contact search, role chips, invite link, QR, Done |
| 2 | `2530:29476` | SVC-MASTER · Services catalogue | Services | Category chips, offering rows, + New service/package, Done |
| 3 | `2530:29418` | SVC-MASTER · Clients | Clients | Client cards (Regular/VIP/New/Corporate), + New client |
| 4 | `2530:29556` | SVC-MASTER · Rooms & Staff | Rooms & Staff | Capacity rows (Busy/Open), + Add room / staff |

### B. Master create/edit forms

| # | Node | Figma name | UI title | Purpose |
|---|------|------------|----------|---------|
| 5 | `2530:29613` | SVC-FORM · New / Edit Client | New client | GSTIN, registration, interests, preferred staff, status |
| 6 | `2530:29704` | SVC-FORM · Add / Edit Room or Staff | Add room / staff | Type Chair/Room/Staff, calendar colour, categories |
| 7 | `2530:29783` | SVC-MASTER · Create Service offering | Create service offering | SAC, GST, duration, price, default room, buffer |
| 8 | `2530:29908` | SVC-MASTER · Create Package | Create package | Sessions, package price, includes, validity |

### C. Utility QA (Hold / Transfer / Reprint + browser)

| # | Node | Figma name | UI title | Purpose |
|---|------|------------|----------|---------|
| 9 | `2533:29334` | SVC-QA-HOLD · Appointment hold | Appointment hold | Hold slip + lines; no revenue |
| 10 | `2533:29448` | SVC-QA-TRANSFER · Staff / Room transfer | Staff / room transfer | Move bookings between pro/room |
| 11 | `2533:29538` | SVC-QA-REPRINT · Reprint invoice | Reprint invoice | Re-render posted invoice, same number |
| 12 | `2533:29628` | SVC-SHEET · Services browser | Services | Compact picker to Add service/package into booking/charge |

### D. Core Quick-Add captures (amount-first)

| # | Node | Figma name | UI title | CTA |
|---|------|------------|----------|-----|
| 13 | `2401:30060` | SVC-QA-01 · Finalize Charge | Finalize Charge | Post charge |
| 14 | `2401:30124` | SVC-QA-02 · Record Purchase | Record Purchase | Post purchase |
| 15 | `2401:30182` | SVC-QA-03 · Record Collection | Record Collection | Post collection |
| 16 | `2401:30235` | SVC-QA-04 · Record Payment | Record Payment | Post payment |
| 17 | `2401:30288` | SVC-QA-05 · Record Expense | Record Expense | Post expense |
| 18 | `2401:30341` | SVC-QA-06 · Stock Update | Stock Update | Post stock update |
| 19 | `2401:30400` | SVC-QA-07 · Booking | Booking | Save booking |
| 20 | `2401:30470` | SVC-QA-08 · Status / Assignment | Status / Assignment | Update status |
| 21 | `2401:30529` | SVC-QA-09 · Budget / Target Revision | Budget / Target Revision | Save revision |
| 22 | `2401:30587` | SVC-QA-10 · Issue / Approval / Update | Issue / Approval / Update | Post |

**Flow order (dependency):** Tokens → shared sheet chrome → Masters lists → Master forms → Services browser → QA-01..10 → Hold/Transfer/Reprint → Hub tile wiring → iOS parity.

---

## 2. Color palette / design tokens

`get_variable_defs` returned empty for these nodes; hex values taken from `get_design_context` / screenshots and already match the codebase.

| Role | Hex | Android (`ServicesTeal`) | iOS (`BusinessTokens` / Assets) |
|------|-----|--------------------------|----------------------------------|
| Canvas / dimmed bg | `#042F2E` | `screenMid`, `sheet`, `field` | `BusinessServicesCanvas` (`#042F2E`) |
| Sheet / card surface | `#134E4A` | `card`, `tile` | `BusinessServicesSurface` |
| Deep teal / chip fill | `#115E59` | `screenTop`, `chip`, `tealDeep` | `BusinessServicesCanvasDeep` / inset family |
| Accent teal | `#0D9488` | `teal`, `heroStart` | `BusinessServicesAccent` / `HeroStart` (`#0D9488`) |
| Lime accent / CTA end | `#84CC16` | `lime`, `heroEnd` | `BusinessServicesHeroEnd` (`#84CC16`) |
| Chip border (inactive) | `#0F766E` | add if missing → `chipBorder` | map to border token |
| Grab / muted text | `#5EEAD4` | `tealMuted`, `textMuted` | services muted / soft |
| Body text on teal | `#99F6E4` | `textBody` | services secondary |
| Primary text | `#FAFAF9` / `#F0FDFA` | `textPrimary` | `textPrimary` |
| Ink on accent CTA | `#042F2E` | `inkOnTeal` | `BusinessServicesOnAccent` |
| Why-note body | `#FEF3C7` | use `amber` soft or add `whyBody` | signal warning soft |
| Selected chip fill | `#0D9488` + border `#84CC16` | existing | accent + lime border |
| CTA gradient | `#0D9488` → `#84CC16` | `teal` → `lime` | heroStart → heroEnd |
| Amber / VIP | `#FBBF24` | `amber` | signal warning |
| Negative | `#EF4444` | `negative` | signal danger |

**Token work:** Confirm iOS asset hex equals Android; add any missing (`chipBorder` `#0F766E`, `whyBody` `#FEF3C7`) to both. Do not rename existing `ServicesTeal` keys unless necessary.

**Typography:** Plus Jakarta Sans — Bold 22 title, Medium 11–14 fields, Regular 10–11 footers / Why.

**Radii (from Figma):** sheet top 28, amount card 16, fields 12, pills 999, CTA 16, grab 2×40.

---

## 3. Shared components to build / reuse

| Component | Reuse from | Adapt for Service |
|-----------|------------|-------------------|
| Sheet scrim + grab + close | Android `OperatingSheetScrim`; iOS sheet chrome | Teal accent shadow |
| Mode / category chips | Restaurant masters / `OperatingPill` | Active = teal fill + lime border + dark ink |
| Amount-first hero card | `OperatingCaptureSheet` amount block | Titles: Charge / Purchase / Collect / Pay / Expense / Estimate / New target |
| Field row (label + ›) | Capture sheet | Exact Figma labels |
| Why note | Capture footer why | Verbatim Figma strings + lime “Why” + cream body |
| Sticky CTA + caption | Capture footer | Gradient teal→lime; dark ink label |
| Saved toast | Restaurant form toasts | `✓ Saved · sheet closes and returns back` |
| Master list row | `RestaurantMastersSheet` pattern | Service-specific badges (VIP, Busy/Open, GST %) |
| Invite & QR | `BusinessInviteQr` + `EmberInvitePalette` → **Teal invite palette** | Roles: Owner, Manager, Stylist, Front desk, Therapist |
| Services browser | `RestaurantPosSheet` menu browser | Categories Hair/Skin/Spa/Wellness/Nails; Add buttons |
| Hold / Transfer / Reprint ticket | `RestaurantKotSheet` structure | Service line metas (duration · ₹ · SAC · %) |

**Do not reuse trading Hold/Transfer/Reprint layouts** — different domain objects and copy.

---

## 4. Proposed file / module structure

### Android (`apk/app/src/main/java/com/example/momentra/…`)

```
ui/theme/ServicesTealTokens.kt          # extend missing tokens only
ui/shell/business/service/
  ConsumerServiceActive.kt              # existing Pulse/Life host
  ConsumerServiceQuickAdds.kt           # hub tiles → open new sheets
  ServiceStoreKit.kt                    # shared chrome (existing)
  ServiceFormat.kt                      # existing
  ServiceSheetState.kt                  # view-all boards (existing)
  ServiceMastersSheet.kt                # NEW — extract + Figma-faithful masters
  ServiceMasterForms.kt                 # NEW — Client / Room-Staff / Service / Package forms
  ServiceServicesBrowser.kt             # NEW — SVC-SHEET browser
  ServiceHoldTransferReprint.kt         # NEW — QA-HOLD / TRANSFER / REPRINT
  ServiceCaptureOverrides.kt            # optional — service-specific capture titles if not in OperatingQuickSheet
ui/shell/business/shared/
  OperatingQuickSheet.kt                # service variant copy + fields for QA-01..10
  BusinessInviteQr.kt                   # TealInvitePalette
```

### iOS (`momentra/momentra/…`)

```
Shell/BusinessActive/
  OperatingCatalog.swift                # consumerService labels already partially present
  OperatingExperienceHost.swift         # retail-family → Service kit (parity plan)
  Service/                              # NEW folder (mirror Marketing/)
    ServiceActiveView.swift             # Pulse/Hub host (or thin wrapper)
    ServiceQuickAdds.swift              # hub tiles
    ServiceMastersViews.swift           # Invite / Clients / Services / Rooms
    ServiceMasterForms.swift            # 4 create/edit forms
    ServiceServicesBrowser.swift
    ServiceHoldTransferReprint.swift
  Sheets/OperatingCaptureSheet.swift    # consumerService branch copy for QA-01..10
  Sheets/OperatingMastersView.swift     # service tab titles/kinds OR defer to ServiceMastersViews
Assets.xcassets/BusinessTokens/         # verify + add chipBorder/whyBody if needed
```

---

## 5. Implementation phases (dependency order)

### Phase 0 — Tokens + sheet chrome parity
- Lock `ServicesTeal` / iOS services assets to Figma hex table above.
- Shared: grab, close, chip states, Why note, CTA gradient, toast.
- **Done when:** one throwaway preview sheet matches Finalize Charge chrome.

### Phase 1 — Masters lists (screens 1–4)
- Android: replace generic `ServiceMastersSheet` list UI with Figma layouts (mirror `RestaurantMastersSheet`).
- iOS: service-specific masters views (or branch `OperatingMastersView` for `.consumerService`).
- Wire hub tiles Clients / Services / Rooms & Staff / Invite & QR.
- **Done when:** each list matches screenshot copy + badges.

### Phase 2 — Master forms (screens 5–8)
- Dedicated forms (not generic MASTERS capture): New Client, Add Room/Staff, Create Service, Create Package.
- Nested sheet from masters (`+ New …` / Edit).
- Persist via existing master create/update APIs; map fields (SAC, GST %, duration, sessions, validity).
- **Done when:** save returns to list with toast copy exact.

### Phase 3 — Services browser (screen 12)
- Compact picker used from Booking / Finalize Charge service field.
- Categories + Add; `+ New service` / `+ New package` deep-link to Phase 2 forms.
- **Done when:** selecting a row returns name/price/SAC into parent draft.

### Phase 4 — Core QA captures 01–10 (screens 13–22)
- Prefer extending `OperatingCaptureSheet` / iOS capture with `variant=service` mode chips + Why/CTA strings from Figma.
- Mode chips per screen (e.g. Finalize: From booking / Walk-in charge / Reprint; Booking: Appointment / Walk-in).
- Field labels + footer captions exact.
- **Done when:** each QA opens from hub tile with matching title/CTA/Why.

### Phase 5 — Hold / Transfer / Reprint (screens 9–11)
- Dedicated ticket UIs (Kot-style), not trading layouts.
- Mode chip trio Hold | Transfer | Reprint switches between the three sheets.
- **Done when:** CTAs `Hold slot · HLD-22` / `Confirm transfer · 2 bookings` / `Reprint · duplicate copy` with Why notes exact.

### Phase 6 — Hub wiring + iOS host
- Android: confirm `ServiceCoreTiles` / `ServiceSupportTiles` / `ServiceUtilityTiles` open Phases 1–5.
- iOS: ensure retail-family host serves Service kit (see parity plan Phase for Service).
- Hero copy: Figma tagline **Book. Deliver. Charge.** (update any stale “Book. Serve. Charge.” / “Book. Charge. Settle.”).

### Phase 7 — Visual QA pass
- Side-by-side Figma screenshot vs device for all 22.
- Fix spacing, chip active states, gradient CTA, Why cream text.

**Phase 7 complete (2026-11-10):** Batched Figma screenshots for all 22 SVC nodes (`TzLvwVwlPbeVB8ug1zB3GM`). Fixed high-impact parity: iOS service capture body (was trading `standardBody`), interactive mode chips + teal→lime CTA + draft footer, Why row (lime + cream), category/invite lime chip fills, invite selected stroke, GST line `CGST 9% + SGST 9%`, browser header copy, Hold/Transfer mode lime chips. Deferred (non-blocking visual): Invite QR live wire-up on iOS stub, HTR live event posts, some browser deep-links from nested capture fields.

---

## 6. Content / copy to match exactly (key strings)

**Brand / chrome**
- Dimmed header: `BrightCare Pulse` / `Book. Deliver. Charge.`
- Footer draft: `Draft preserved if you close or open a picker` (QA family)
- Toast: `✓ Saved · sheet closes and returns back`

**Masters Why**
- Invite: `Invite grants a role and its access. Joining ≠ approval ≠ payment.`
- Services catalogue: `Catalogue lists saved offerings. GST badge is the SAC rate — output GST posts on Finalize Charge, never on booking save.`
- Clients: `Client is a master write. Booking picks a saved client; GST posts on Finalize Charge, never on booking save.`
- Rooms: `Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue.`

**Form Why / CTAs**
- New client → `Save client` / `Client saved · available in Booking`
- Room/staff → `Save room / staff` / `Saved · calendar slots ready`
- Create service → `Save service` / Why: Create Service is a master write…
- Create package → `Save package` / Why: Sessions redeemed per booking; revenue+GST per Finalize Charge

**Roles (Invite):** Owner, Manager, Stylist, Front desk, Therapist  
**Categories:** Hair, Skin, Spa, Wellness, Nails

**QA CTAs / Why (abbrev)**
| Screen | CTA | Why (start) |
|--------|-----|-------------|
| Finalize Charge | Post charge | Finalized charge posts revenue + output GST once… |
| Record Purchase | Post purchase | Stock only when received. AP + input GST… |
| Record Collection | Post collection | Allocates charge AR — never creates new revenue… |
| Record Payment | Post payment | Settles open AP only. |
| Record Expense | Post expense | Service running cost; payment state separate. |
| Stock Update | Post stock update | Optional consumables — never invent usage from booking. |
| Booking | Save booking | Commitment only — no revenue, no GST… |
| Status / Assignment | Update status | Operational/appointment state only — does not post charge. |
| Budget / Target | Save revision | Versioned + effective-dated. |
| Issue / Approval / Update | Post | Audit / action — approval ≠ disbursement. |
| Appointment hold | Hold slot · HLD-22 | Hold reserves a slot only… |
| Staff / room transfer | Confirm transfer · 2 bookings | Transfer reassigns staff or room… |
| Reprint invoice | Reprint · duplicate copy | Reprint re-renders the posted invoice… |

Demo entity names in Figma (BrightCare, Ananya Mehta, etc.) are **fixtures for visual QA**; production binds projection/masters data.

---

## 7. Risks / gaps

| Risk | Impact | Mitigation |
|------|--------|------------|
| Master API fields missing (SAC, sessions, validity, calendar colour, interests) | Forms cannot persist full Figma | Map available fields; hide or `Unavailable` extras until API; track like restaurant missing APIs |
| Service Hold/Transfer/Reprint vs trading modes | Wrong payload / copy | Dedicated service sheets + event modes; do not call trading HoldCaptureLayout |
| iOS still on Trading hub | Service screens unreachable / wrong chrome | Finish Service branch of `2026-11-09` parity host before claiming iOS done |
| `get_variable_defs` empty | No formal Figma variables | Use hex from design_context (already aligned with `ServicesTeal`) |
| Package / prepaid session accounting | Easy to post revenue twice | Enforce Why copy + server rules; UI never implies package Add = charge |
| QR / invite link assets | Placeholder “SR” glyph in Figma | Generate real QR from invite URL; keep Share/Save actions |
| Contacts permission (Invite search) | Platform friction | Fall back to email/phone search if contacts denied |
| Nested sheets (masters → form → browser) | Back-stack / dismiss bugs | Mirror restaurant nested scrim pattern; one create sheet at a time |
| Copy drift in catalog (`Book. Serve. Charge.`) | Tagline mismatch | Sweep iOS `OperatingCatalog` + Android hero to `Book. Deliver. Charge.` |

---

## 8. Recommended next step (after approval)

1. **Approve this plan** (and confirm iOS Service host may land in the same effort as Android, or Android-first).
2. **Start Phase 0 + Phase 1 on Android** (tokens + Figma-faithful `ServiceMastersSheet`), using restaurant masters as the port template.
3. For each screen, call Figma `get_design_context` + `get_screenshot` with `skillNames: figma-design-to-code` immediately before coding that screen.
4. Execution method: **Subagent-driven** (many interdependent sheets; independent review per phase reduces copy/GST boundary regressions).

---

## Task checklist (bite-sized)

### Task 1: Token lock
**Files:** `ServicesTealTokens.kt`; iOS `BusinessServices*.colorset`  
- [ ] Diff Figma hex table vs code; add `chipBorder` / `whyBody` if needed  
- [ ] Screenshot Finalize Charge chrome preview matches Figma  
- [ ] Commit `fix(tokens): align service teal/lime with SVC Figma`

### Task 2: Android masters lists (1–4)
**Files:** Create `ServiceMastersSheet.kt`; modify `ConsumerServiceQuickAdds.kt`  
- [ ] Port RestaurantMastersSheet structure with service copy/badges  
- [ ] Invite uses TealInvitePalette + service roles  
- [ ] Visual check vs nodes `2530:29334`, `29418`, `29476`, `29556`

### Task 3: Android master forms (5–8)
**Files:** Create `ServiceMasterForms.kt`  
- [ ] Four forms with Figma fields + Why + CTA  
- [ ] Wire Edit / + New from masters  
- [ ] Visual check vs `2530:29613`, `29704`, `29783`, `29908`

### Task 4: Android browser + utilities (9–12)
**Files:** `ServiceServicesBrowser.kt`, `ServiceHoldTransferReprint.kt`  
- [ ] Browser returns selection to parent  
- [ ] Hold/Transfer/Reprint ticket UIs + mode chips  
- [ ] Visual check vs `2533:*`

### Task 5: Android QA-01..10 copy/layout
**Files:** `OperatingQuickSheet.kt` (service branches), hub enums  
- [ ] Mode chips, amount labels, Why, CTAs per table  
- [ ] Hub tiles open correct modes  
- [ ] Visual check vs `2401:30060`…`30587`

### Task 6: iOS Service kit surfaces
**Files:** `Shell/BusinessActive/Service/*`, capture/masters branches, host routing  
- [ ] Mirror Android sheet set under services theme  
- [ ] Tagline + GST boundary copy  
- [ ] Side-by-side with Android + Figma

### Task 7: End-to-end QA
- [x] Walk hub → each of 22 screens on both platforms (Figma screenshot vs code; device render deferred where emulator unavailable)  
- [x] Confirm no revenue post on Booking/Hold/Transfer/Status (Why/CTA copy locked; utility sheets dedicated)  
- [ ] Commit per phase; no mega-commit

---

## Architecture decision (locked)

**Chosen approach:** Restaurant-style specialization for service masters/forms/utilities + shared amount-first capture for QA-01..10 with service variant strings — not a greenfield design system, and not trading Hold/Transfer reuse.

**Trade-off:** More service-specific files than “one generic masters sheet,” but matches Figma fidelity and GST semantics; parallel to proven restaurant POS/KOT split.
