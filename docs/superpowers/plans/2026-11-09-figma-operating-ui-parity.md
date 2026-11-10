# Figma Operating UI Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the Momentra business-operating UI gaps vs Figma so iOS retail-family hubs/POS/KOT match Android, and Made-to-Order becomes a first-class vertical (hub + QAs) on both platforms—without regressing Trading/Distribution.

**Architecture:** Keep Android retail/restaurant/service kits as the reference implementation. On iOS, stop routing retail-family through the Trading/Distribution `OperatingActiveView` + `OperatingHubScreen` path; introduce vertical hosts analogous to Android’s `OperatingMomentScreens` subtype switch. Treat MTO as its own kit (not a Manufacturing copy overlay). Defer story chrome, toasts, and browsers until hubs and primary QAs ship.

**Tech Stack:** Android Jetpack Compose (Kotlin), iOS SwiftUI (Swift), shared operating projection / `POST …/operating-events` contract.

**Spec:** Figma [Business Redesign](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2348-29188&m=dev) (`2348:29188`). Audit blockers/high items listed below.

## Global Constraints

- Never invent missing metrics as `0` / `0%` — render `Unavailable` (or Figma empty copy).
- Trading & Distribution: regression-only unless a shared kit change forces a touch.
- Android Retail / Restaurant / Service active tabs + hubs (incl. POS/KOT) are **reference**, not greenfield redesign.
- Setup v6, nav accents, masters, view-all footers, story config entry points: do not re-plan as greenfield.
- Table / covers / KOT number (restaurant) and similar non-contract fields stay locked/`Unavailable` until backend fields exist (`docs/implementation/RESTAURANT_MISSING_APIS.md`).
- Prefer porting Android tile order, labels, and sheet modes over inventing new iOS layouts.
- MTO hub tiles and QAs must match Figma `MTO-HUB` / `MTO-QA · *` — not MFG tile relabels.

## Review Focus

1. **Wrong hub kit:** Retail/Restaurant/Service on iOS must not show Trading 9+5 grid or cyan trading hero once Phase 1–3 land.
2. **MTO ≠ MFG:** `MADE_TO_ORDER` must not open Manufacturing Production/Sale/Job-order grid after Phase 4.
3. **Honesty:** Missing covers/tables/KOT ids and missing projection fields stay `Unavailable`, never fabricated.
4. **Regression:** Trading/Distribution Pulse/Hub/Moments/Life/Memory must still match pre-change behaviour.
5. **Dead path:** `ProductionActive` must not be re-wired as the live MFG/MTO route (Phase 9 cleanup only).

## Code baseline (skim findings)

| Surface | Android today | iOS today |
| --- | --- | --- |
| Retail / Restaurant / Service routing | Dedicated kits via `OperatingMomentScreens.kt` (`RetailStoreActive`, `RestaurantActive`, `ConsumerServiceActive`) | `isRetailFamily` exists in `OperatingCatalog.swift` but host always uses `OperatingActiveView` + trading-shaped `OperatingHubScreen` |
| Restaurant POS / KOT | `RestaurantPosSheet.kt`, `RestaurantKotSheet.kt`; hub tiles Sale→POS, Status→KOT | Missing |
| MTO hub | `ManufacturingActive` + MFG hub with minor MTO copy (`OperatingV12Hub.kt`) | Same pattern via `OperatingHubScreen` manufacturing branch |
| MFG Rework/Scrap | Scrap field buried in production capture; no first-class `MFG-QA-14` | Same |
| `ProductionActive.kt` | Unreferenced dead path | n/a |

### Figma anchors (primary)

| Vertical | Hub | Signature QAs / chrome |
| --- | --- | --- |
| Retail | `RTL-HUB-v5` `2399:29887` | Pulse/Moments/Life/Memory `2399:29655` / `30656` / `30816` / `30979` |
| Restaurant | `RST-HUB-v5` `2400:29892` | POS `2446:29655`+, KOT `2449:29655`+ |
| Service | `SVC-HUB-v5` `2401:29887` | Finalize Charge + Booking tiles |
| Manufacturing | `MFG-HUB-v3` `2396:29892` | `MFG-QA-14 · Rework / Scrap / Job Work` `2396:30932` |
| MTO | `MTO-HUB` `2686:30690` | Quote / Confirm / Open job / … / Quote revise / Job status; 13× `MTO-TOAST · *` |

---

## Phase 0 — Inventory & routing contracts

1. **Phase name + goal:** Lock vertical routing contracts so later phases can swap kits without ad-hoc subtype ifs. Goal: iOS host can select retail-family vs operating-v12 vs marketing vs (future) MTO kits; MTO is named as its own vertical path (not only `isManufacturing`).
2. **Platforms:** both (iOS host contracts primary; Android document + any shared enum alignment).
3. **Verticals in scope:** Retail, Restaurant, Service, Manufacturing, Made-to-Order (routing only); Trading/Distribution unchanged.
4. **Concrete deliverables:**
   - iOS: `OperatingExperienceHost.swift`, `OperatingCatalog.swift` (`isRetailFamily`, `forSubtype`, consider `isMadeToOrder` / kit enum), `BusinessOperatingModel.swift`.
   - Android: inventory note against `OperatingMomentScreens.kt` subtype switch; confirm MTO still shares `ManufacturingActive` (to be split in Phase 4).
   - Short routing matrix checked into this plan’s “Recommended first PR” notes or a sibling checklist in PR description (no new architecture doc unless needed).
5. **Dependencies:** none.
6. **Acceptance criteria:**
   - Documented matrix: subtype → kit (Trading V12 / Dist V12 / MFG / MTO / Marketing / Retail / Restaurant / Service).
   - iOS compile with host switch scaffolding (stubs OK) that does **not** yet change visible retail UI (or only no-op branches).
   - `isRetailFamily` / MTO flags are the only gates subsequent phases use.
7. **Out of scope / Unavailable:** No new hub UI, POS/KOT, or MTO QA screens. Do not delete `ProductionActive` yet.
8. **Rough effort:** S

### Phase 0 tasks

- [ ] **Task 0.1:** Add `OperatingKit` (or equivalent) mapping in iOS `BusinessOperatingVertical` / host.
- [ ] **Task 0.2:** Wire `OperatingExperienceHost` to branch on kit with stub views that currently forward to existing `OperatingActiveView` (behaviour-preserving).
- [ ] **Task 0.3:** Confirm Android subtype matrix matches Figma vertical list; note MTO share-with-MFG as Phase 4 debt.
- [ ] **Task 0.4:** Commit routing-only change.

---

## Phase 1 — iOS Retail hub + capture shells

1. **Phase name + goal:** Replace iOS Retail Trading-grid hub with rose Retail hub/captures ported from Android `retail/*`.
2. **Platforms:** iOS (Android retail = reference only / regression smoke).
3. **Verticals in scope:** `RETAIL_STORE`.
4. **Concrete deliverables:**
   - New iOS kit under e.g. `Shell/BusinessActive/Retail/` (mirroring Android): hub, pulse/moments/life/memory shells or wrappers, capture sheet modes for RTL core/support/utilities.
   - Touch: `OperatingExperienceHost.swift`, possibly `OperatingActiveView.swift` (stop using for retail), tokens if rose canvas not fully wired.
   - Android reference: `RetailStoreActive.kt`, `RetailQuickAdds.kt`, `RetailStoreKit.kt`, `RetailFormat.kt`.
   - Figma: `RTL-HUB-v5` `2399:29887`, tab frames `2399:29655` / `30656` / `30816` / `30979`.
5. **Dependencies:** Phase 0 routing.
6. **Acceptance criteria:**
   - Retail Quick Add shows rose hero + Retail tile set (Sale / Purchase / … utilities), not Trading cyan hub.
   - Pulse/Moments/Life/Memory use retail IA/copy (Android parity), `Unavailable` honesty preserved.
   - Trading/Distribution hubs unchanged.
7. **Out of scope / Unavailable:** Restaurant/Service kits; retail POS ALT frames beyond what Android already ships; inventing SKU movement beyond projection.
8. **Rough effort:** L

### Phase 1 tasks

- [ ] **Task 1.1:** Port Retail hub tile catalogue + hero CTA from Android `RetailQuickAdds.kt`.
- [ ] **Task 1.2:** Port or adapt Retail Pulse/Moments/Life/Memory from Android `RetailStoreActive.kt` into SwiftUI kit.
- [ ] **Task 1.3:** Route `RETAIL_STORE` in `OperatingExperienceHost` to the new kit.
- [ ] **Task 1.4:** Visual/smoke vs Figma RTL-HUB + Android device; commit.

---

## Phase 2 — iOS Restaurant hub + POS + KOT

1. **Phase name + goal:** Ship ember Restaurant hub and POS/KOT capture flows on iOS (blocker).
2. **Platforms:** iOS (Android restaurant = reference).
3. **Verticals in scope:** `RESTAURANT_FOOD`.
4. **Concrete deliverables:**
   - iOS: `Shell/BusinessActive/Restaurant/` — hub, active tabs, `Pos` + `Kot` sheets.
   - Android reference: `RestaurantActive.kt`, `RestaurantQuickAdds.kt` (Sale→Pos, Status→Kot), `RestaurantPosSheet.kt`, `RestaurantKotSheet.kt`, `RestaurantStoreKit.kt`.
   - Figma: `RST-HUB-v5` `2400:29892`; POS `2446:29655` / `29802` / `2448:29655`; KOT `2449:29655` / `29769` / `29859`.
5. **Dependencies:** Phase 0; Phase 1 patterns (retail-family host + sheet router) strongly preferred.
6. **Acceptance criteria:**
   - Restaurant hub is ember, not trading grid; Sale opens POS; Status opens KOT.
   - Table / covers / KOT no. locked or Unavailable per Android + missing-API doc.
   - Posted bill path uses existing operating-events body (no fabricated kitchen ledger).
7. **Out of scope / Unavailable:** Real KOT printer binding, covers/tables backend fields, purchase-order deep ALT unless Android already has it wired.
8. **Rough effort:** L

### Phase 2 tasks

- [ ] **Task 2.1:** Port Restaurant hub tile map (core/support/utilities) + ember chrome.
- [ ] **Task 2.2:** Implement POS sheet modes (In-dining / Cloud / Takeaway) matching Android field honesty.
- [ ] **Task 2.3:** Implement KOT Send / Fire / Reprint shells.
- [ ] **Task 2.4:** Route `RESTAURANT_FOOD` in host; smoke vs Figma + Android; commit.

---

## Phase 3 — iOS Service hub + Finalize Charge / Booking

1. **Phase name + goal:** Ship teal Service hub with Finalize Charge + Booking as first-class tiles/captures.
2. **Platforms:** iOS (Android service = reference).
3. **Verticals in scope:** `CONSUMER_SERVICE`.
4. **Concrete deliverables:**
   - iOS: `Shell/BusinessActive/Service/` — hub + active tabs + charge/booking captures.
   - Android reference: `ConsumerServiceActive.kt`, `ConsumerServiceQuickAdds.kt` (`Finalize Charge` / `Booking`), `ServiceStoreKit.kt`.
   - Figma: `SVC-HUB-v5` `2401:29887` (+ service pulse/moments/life/memory siblings in same section).
5. **Dependencies:** Phase 0; Phase 1–2 host/sheet patterns.
6. **Acceptance criteria:**
   - Service hub teal; hero CTA Finalize Charge; Booking tile distinct from charge (booking ≠ revenue).
   - Active tabs match Android service IA thickness (closes “iOS thinner than Android” for this vertical).
7. **Out of scope / Unavailable:** Inventing room/shift metrics without projection fields; Trading regression work.
8. **Rough effort:** M

### Phase 3 tasks

- [ ] **Task 3.1:** Port Service hub tiles + hero.
- [ ] **Task 3.2:** Port Finalize Charge + Booking capture modes.
- [ ] **Task 3.3:** Port Service Pulse/Moments/Life/Memory kit; route subtype; commit.

---

## Phase 4 — MTO hub + QAs (both platforms)

1. **Phase name + goal:** Make Made-to-Order a first-class vertical with its own hub and quote/job QAs on Android and iOS (blocker).
2. **Platforms:** both.
3. **Verticals in scope:** `MADE_TO_ORDER` only (Manufacturing plant stays on MFG kit).
4. **Concrete deliverables:**
   - **Hub** matching `MTO-HUB · Quick Adds` `2686:30690` — not MFG Production/Sale grid.
   - **Primary QAs** (minimum blocker set + adjacent Figma QAs in same PR series as needed):
     - `MTO-QA · Create Quotation` `2686:31133`
     - `MTO-QA · Accept / Confirm Order` `2686:31201`
     - `MTO-QA · Open Production Job` `2686:31269`
     - `MTO-QA · Quote revise / expire` `2698:38415`
     - `MTO-QA · Job status change` `2698:38483`
     - Plus production-against-job / invoice-against-order as required for hub tile targets (`2686:31348`, `2686:31427`).
   - Android: new `mto/` package (or dedicated composables) branched from `OperatingMomentScreens.kt` instead of `ManufacturingActive` for `MADE_TO_ORDER`.
   - iOS: new `MadeToOrder/` kit; host kit enum routes `.madeToOrder` away from manufacturing hub.
   - Capture mode codes/events must align with backend operating-events (map honestly; if an event type is missing, sheet may save as closest allowed type **only** when product already documented—otherwise mark Unavailable/disabled with note, do not invent revenue).
5. **Dependencies:** Phase 0 routing. Prefer after Phase 1–3 so iOS host patterns exist, but Android MTO can start in parallel once Phase 0 lands.
6. **Acceptance criteria:**
   - Selecting MTO never shows MFG hub tiles (Production / Customer order plant copy).
   - Hub exposes Quote / Confirm Order / Open job / Quote revise / Job status (labels per Figma).
   - Quote & order ≠ revenue copy/rules visible; invoice posts AR only on invoice QA.
   - Both platforms show the same hub tile set order.
7. **Out of scope / Unavailable:** Full MTO toast system (Phase 8); story chapter polish (Phase 7); Parts/Materials browsers (Phase 9); MFG Rework/Scrap plant QA (Phase 6)—except fields that appear on MTO production QA already in Figma.
8. **Rough effort:** L

### Phase 4 tasks

- [ ] **Task 4.1:** Define MTO hub tile catalogue + capture mode enum (shared naming Android/iOS).
- [ ] **Task 4.2:** Android — branch `MADE_TO_ORDER` off `ManufacturingActive`; implement hub + blocker QAs.
- [ ] **Task 4.3:** iOS — implement MTO kit + host routing.
- [ ] **Task 4.4:** Cross-platform smoke vs Figma `MTO-HUB` + five blocker QAs; commit.

---

## Phase 5 — Manufacturing / MTO Moments · Life · Memory IA/copy

1. **Phase name + goal:** Replace trading-shaped Moments/Life/Memory titles and board chrome for MFG + MTO with Figma plant/job IA.
2. **Platforms:** both.
3. **Verticals in scope:** `STANDARD_PRODUCTION`, `MADE_TO_ORDER`.
4. **Concrete deliverables:**
   - Android: `ManufacturingActive.kt` Moments/Life/Memory sections; MTO kit screens (`MTO-MOMENTS` `2686:30122`, `MTO-LIFE` `2686:30403`, `MTO-MEMORY` `2686:30690` area / `2692:29515`); sheet title overrides.
   - iOS: `OperatingCatalog.swift` manufacturing/MTO sheet overrides; MTO/MFG active screens copy; `OperatingV12Screens.swift` titles.
   - Figma view-alls: `MTO-SHEET · View all · …` family `2694:*`; MFG equivalents in MFG section.
5. **Dependencies:** Phase 4 for MTO screens; MFG copy can start after Phase 0 but should land with or just after Phase 4 so MTO boards are not half trading / half job.
6. **Acceptance criteria:**
   - MTO Moments/Life use job/customer language (e.g. Current Jobs, Orders/Jobs, Job Operations)—not Trading “Sales & Orders” clones.
   - MFG boards use plant language (not wholesale trading nouns).
   - Empty states stay honest (empty ≠ zero).
7. **Out of scope / Unavailable:** Hub tile work (done in 4); story Saved/Share (Phase 7).
8. **Rough effort:** M

### Phase 5 tasks

- [ ] **Task 5.1:** Audit current Moments/Life/Memory strings on both platforms vs Figma; produce replace list.
- [ ] **Task 5.2:** Apply MFG copy/IA.
- [ ] **Task 5.3:** Apply MTO copy/IA + view-all titles; commit.

---

## Phase 6 — MFG Rework/Scrap + remaining QA gaps

1. **Phase name + goal:** Promote Rework/Scrap/Job Work to a first-class MFG QA and close remaining MFG/MTO QA gaps not covered in Phase 4.
2. **Platforms:** both.
3. **Verticals in scope:** `STANDARD_PRODUCTION` (primary); MTO only for parity QAs still missing (`MTO-QA · Rework / Scrap / Job work` `2698:37795`, hold/transfer/reprint/approval/update/budget as needed).
4. **Concrete deliverables:**
   - First-class sheet: Figma `MFG-QA-14 · Rework / Scrap / Job Work` `2396:30932` (modes Rework / Scrap / Job Work).
   - Android today: scrap fields live inside production capture (`OperatingQuickSheet.kt`) — extract/promote.
   - Hub tile on MFG hub if Figma hub includes it; else entry from production utility row—match Figma hub.
   - Remaining MTO utility QAs not shipped in Phase 4 (return, stock update job-linked, job hold, WIP transfer, reprint, etc.).
5. **Dependencies:** Phase 4 (MTO kit exists); Phase 5 optional but preferred for consistent empty copy.
6. **Acceptance criteria:**
   - Operator can open Rework/Scrap/Job Work as its own QA (not only a scrap qty field on production).
   - Scrap does not become finished stock (existing honesty rule preserved).
   - MFG hub no longer “hides” rework/scrap as a secondary-only control.
7. **Out of scope / Unavailable:** Inventing job-work subledger fields absent from API; FEFO browsers (Phase 9).
8. **Rough effort:** M

### Phase 6 tasks

- [ ] **Task 6.1:** Implement MFG-QA-14 sheet Android + iOS; wire hub/utility entry.
- [ ] **Task 6.2:** Land remaining MTO utility QAs per Figma list still open.
- [ ] **Task 6.3:** Regression on MFG production QA; commit.

---

## Phase 7 — Story chapter / Saved / Share / STORY-STATE polish

1. **Phase name + goal:** Complete story composition chrome (chapters, Saved, Share, STORY-STATE empty/error) for verticals that still trail Figma—especially MFG/MTO and retail-family where iOS was thin.
2. **Platforms:** both.
3. **Verticals in scope:** MFG, MTO, Retail, Restaurant, Service (Trading/Distribution only if shared story kit changes force it—regression).
4. **Concrete deliverables:**
   - Chapter labels: MTO `MTO-STORY-P1…P8` (`2706:37994`…); existing `MTO_STORY_PAGES` on Android vs Figma job nouns.
   - Saved / Share: `MTO-STORY-SAVED` / `MTO-STORY-SHARE`; retail-family `RST-STORY-*`, `RTL-STORY-*`, service equivalents.
   - STORY-STATE patterns from Trading reference `TRD-STORY-STATE · *` (`2657:29510`…) applied per vertical: Memory empty, Config no activity, Generating failed, Conflict, Unauthorized, Partial preview, Loading skeleton.
   - Files: Android `OperatingStoryConfigSheet.kt`, `*StoryConfigSheet.kt`, `OperatingMomentScreens.kt` page lists; iOS story sheets under `BusinessActive`.
5. **Dependencies:** Phases 1–5 (kits + IA exist so story nouns match boards).
6. **Acceptance criteria:**
   - Config → Generating → P1…P8 → Saved → Share flow matches Figma chrome per vertical.
   - Failure/empty STORY-STATE frames render (not silent fail / blank sheet).
7. **Out of scope / Unavailable:** Redesigning Trading story content that already matches; backend story page payload changes beyond existing compose API.
8. **Rough effort:** M

### Phase 7 tasks

- [ ] **Task 7.1:** Align chapter noun lists with Figma for MFG/MTO/retail-family.
- [ ] **Task 7.2:** Implement Saved + Share sheets where missing.
- [ ] **Task 7.3:** Wire STORY-STATE empty/error/loading; commit.

---

## Phase 8 — MTO toasts + Obsidian empty/error wiring

1. **Phase name + goal:** Ship the 13 MTO toast frames and wire Obsidian empty/error/offline patterns into MTO (and any retail-family gaps still on generic empty).
2. **Platforms:** both.
3. **Verticals in scope:** MTO primary; Obsidian shared kit as needed for Retail/Restaurant empty accents.
4. **Concrete deliverables:**
   - All `MTO-TOAST · *` frames:
     - Setup activated, Saved, Deleted, Quote sent, Order accepted, Invoice posted (`2712:29832`…`29887`)
     - Saved → Accept/Confirm Order, Saved → Back, Saved → Customers & suppliers, Saved → Lines & stores, Saved → Products & BOM (`2713:29928`…`30043`)
     - Paused, Finished (`2761:38075`, `2761:38100`)
   - Obsidian: empty/error/offline/loading from `2853:*` / `2856:*` (Retail rose empty, RST flame empty, Pulse error, Offline, etc.).
   - Android appstate kit already has emerald work—extend vertical accents rather than fork.
5. **Dependencies:** Phase 4 (actions that fire toasts); Phase 7 helpful for story-related toasts but not required for quote/order toasts.
6. **Acceptance criteria:**
   - Each of the 13 MTO toasts can be triggered from the corresponding success path (or demo harness if product agrees)—copy and CTA match Figma.
   - Obsidian empty/error used on MTO pulse/moments when projection empty/failed—not trading cyan placeholders.
7. **Out of scope / Unavailable:** Full marketing toast system; inventing toast actions without a save path.
8. **Rough effort:** M

### Phase 8 tasks

- [ ] **Task 8.1:** Implement shared toast component variants for MTO copy/CTAs.
- [ ] **Task 8.2:** Hook post-save navigation toasts (Saved → …).
- [ ] **Task 8.3:** Wire Obsidian empty/error for MTO (+ remaining retail-family if still generic); commit.

---

## Phase 9 — FEFO/SKU/Parts browsers + cleanup

1. **Phase name + goal:** Add remaining browsers (Parts/Materials/SKU/FEFO where Figma specifies) and remove dead `ProductionActive` path.
2. **Platforms:** both (cleanup Android-heavy).
3. **Verticals in scope:** MFG, MTO, plus shared SO/Delivery SKU browsers if still incomplete; Trading/Distribution only if browser shared component changes.
4. **Concrete deliverables:**
   - `MTO-SHEET · Parts browser (Quote / Job)` `2706:37762`
   - `MTO-SHEET · Materials browser (Job PO)` `2706:37822`
   - `MFG-SHEET · Parts browser (SO / WO)` `2547:29629`
   - Trading/Dist SKU browsers already in Figma (`TRD-SHEET · SKU browser`, `DST-SHEET · SKU browser`)—verify wiring; fix only if broken.
   - Delete or quarantine unreferenced `apk/.../production/ProductionActive.kt` + `ProductionQuickAdds.kt` after confirming no navigation entry.
5. **Dependencies:** Phases 4–6 (QA sheets that open browsers).
6. **Acceptance criteria:**
   - Quote/Job/PO sheets can open Parts/Materials browsers matching Figma.
   - `ProductionActive` no longer present as a confusing alternate MFG UI (or explicitly marked `@Deprecated` and unreachable—prefer delete).
   - No Trading/Distribution visual regressions from shared browser component edits.
7. **Out of scope / Unavailable:** New inventory algorithms (FEFO math) beyond displaying projection/batch facts the API already returns.
8. **Rough effort:** M

### Phase 9 tasks

- [ ] **Task 9.1:** Implement/port Parts + Materials browsers for MTO/MFG.
- [ ] **Task 9.2:** Verify SKU browsers on trading-family; fix gaps only.
- [ ] **Task 9.3:** Remove `ProductionActive` dead path; final regression pass; commit.

---

## Recommended first PR (Phase 0 + Phase 1 only)

**Title:** `iOS: retail-family routing + Retail rose hub kit`

**Include**
- Phase 0: kit/routing enum + `OperatingExperienceHost` branch points (Retail live; Restaurant/Service/MTO still stub→legacy or Android-unchanged).
- Phase 1: full iOS Retail hub + Pulse/Moments/Life/Memory + capture shells ported from Android `retail/*`.

**Exclude**
- Restaurant POS/KOT, Service hub, MTO hub, story/toasts/browsers.

**PR test plan**
- [ ] `RETAIL_STORE` Quick Add matches `RTL-HUB-v5` tile order/labels (rose), not Trading hub.
- [ ] Retail Pulse/Moments/Life/Memory match Android retail IA; missing metrics show `Unavailable`.
- [ ] `TRADING_WHOLESALE` / `DISTRIBUTION_DEALERSHIP` hubs unchanged.
- [ ] Restaurant/Service still open (even if still trading-shaped) without crash—tracked for Phases 2–3.
- [ ] Android retail smoke: no unintentional diffs (reference-only PR).

**Why this cut:** Unblocks the highest user-visible iOS retail-family failure with Android as a known-good template, while establishing the host contracts every later vertical needs.

---

## Phase summary

| Phase | Goal | Platforms | Effort |
| --- | --- | --- | --- |
| 0 | Inventory & routing contracts (iOS kits + MTO path) | both | S |
| 1 | iOS Retail hub + capture shells (port Android) | iOS | L |
| 2 | iOS Restaurant hub + POS + KOT | iOS | L |
| 3 | iOS Service hub + Finalize Charge / Booking | iOS | M |
| 4 | MTO hub + blocker QAs | both | L |
| 5 | MFG/MTO Moments·Life·Memory IA/copy | both | M |
| 6 | MFG Rework/Scrap + remaining QAs | both | M |
| 7 | Story chapter / Saved / Share / STORY-STATE | both | M |
| 8 | MTO toasts (13) + Obsidian empty/error | both | M |
| 9 | Parts/SKU browsers + `ProductionActive` cleanup | both | M |

## Explicitly not re-planned (already solid)

- Trading & Distribution full parity (Android + iOS) — regression only.
- Android Retail / Restaurant / Service active tabs + hubs (incl. POS/KOT) — reference implementations.
- Setup v6, nav accents, masters, view-all footers, story config **entry points**.

---

## Self-review (spec coverage)

| Audit finding | Phase |
| --- | --- |
| iOS Retail/Restaurant/Service still Trading grid | 1–3 |
| iOS Restaurant POS + KOT missing | 2 |
| MTO hub + QAs still Manufacturing tiles | 4 |
| MFG/MTO Moments/Life trading-shaped | 5 |
| Story chapters / Saved / Share / STORY-STATE | 7 |
| MFG Rework/Scrap not first-class | 6 |
| iOS retail-family Pulse/Moments/Life thinner | 1–3 |
| MTO toast system (13 frames) | 8 |
| Browsers + ProductionActive dead path | 9 |
