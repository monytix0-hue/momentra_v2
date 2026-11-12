# Marketing-Client Quick Add Figma Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. **PLAN ONLY until a follow-up asks to implement.**

**Goal:** Bring Marketing **Client Campaign** (`CLIENT_CAMPAIGN`) Quick Add bottom sheets to Figma layout + content parity on **APK + iOS**, mirroring the Trading / Distribution approach (dedicated palette section, mirrored copy catalog, capture chrome tokens, chip routing, masters/pickers phased).

**Architecture:** Keep Marketing hubs (`MarketingQuickAddHub` / `MarketingQuickAddHubView`) posting through existing capture hosts (`OperatingCaptureSheet` / `MarketingCaptureSheet` + `OperatingQuickSheet` with `variant=client`). Introduce **`OperatingCaptureChromeTokens.MarketingClient`** (indigo/violet — not Group lilac, not Own magenta) and a mirrored **`MarketingClientQuickAddCopy`** catalog (Kotlin + Swift). Wire Client-only gates for Invoice / Collection. Do **not** invent HOLD / TRANSFER / REPRINT write APIs (Figma shows them; product currently marks tiles unavailable).

**Tech Stack:** Kotlin Jetpack Compose (APK), SwiftUI (iOS), Figma `TzLvwVwlPbeVB8ug1zB3GM` as visual/copy SoT.

**Figma session:** `d2b82901-76df-4890-8b30-76d4848ba1cf` (2026-11-10). Demo brand: **Brightline** · “Plan. Launch. Measure.” — runtime uses `model.title` / subtype tagline.

**Related plans:**  
- Trading: `docs/superpowers/plans/2026-11-10-trading-quick-add-figma-parity.md`  
- Distribution: `docs/superpowers/plans/2026-11-10-distribution-quick-add-figma-parity.md`  
- Manufacturing (31 of the listed nodes): `docs/superpowers/plans/2026-11-10-mfg-quickadds-figma-fidelity.md`

---

## 1. Scope / non-goals (Client vs Own)

### Goals
- Layout + content parity for **Marketing-Client** Quick Add sheets: mode chips, context line, fields (M/O/C), amount-first where shown, More details, WHY panel, sticky CTA + draft meta.
- Dedicated **Client indigo/violet** capture palette (see §2) on APK + iOS.
- Mirrored string catalog so platforms cannot drift on context / WHY / CTA.
- Client-only Invoice + Collection chrome (“Client only · Hidden & server-rejected in Own Business mode”).
- Masters / HOLD / TRANSFER / REPRINT as phased follow-ons with honest capability gates.

### Non-goals
- **Own Business Campaign** (`OWN_BUSINESS_CAMPAIGN` / `MKT-OWN-*` frames, magenta `#D946EF`) — separate SoT; do not retoken Client to Own.
- **Manufacturing** frames incorrectly included in the 51-node paste (see §3) — owned by the MFG plan.
- **Service** vertical — user wording mentioned “service”; listed nodes are **not** SVC. No Service frames appear in this set.
- New backend event types for HOLD / TRANSFER / REPRINT (catalog has no campaign-hold / budget-transfer / invoice-reprint writes today).
- Pixel-perfect typography beyond audited chrome/copy/field-order gaps.
- Changing Pulse / Memory / setup wizards except where capture chrome tokens are shared.

### Client vs Own flag
| Surface | Client (`CLIENT_CAMPAIGN`) | Own (`OWN_BUSINESS_CAMPAIGN`) |
|---------|----------------------------|------------------------------|
| Accent | Indigo `#6366F1` → violet `#8B5CF6` | Magenta `#D946EF` |
| Invoice / Collection | Shown + postable | Hidden / server-rejected |
| Figma family | `MKT-QA-*` / `MKT-MASTER-*` (Brightline) | `MKT-OWN-QA-*` (Harbor & Loom) — **out of scope** |
| Hub | 10 Client QAs (8 shared + INV + COL) | 8 shared only |

---

## 2. Color palette (Marketing-Client) + token map

Pulled from MKT-QA-01 (`2390:30430`) design context + fill inventory across the 20 MKT frames (session 2026-11-10).

| Token | Hex / value | Use in Figma |
|-------|-------------|--------------|
| Canvas / underlying | `#0F172A` | Dimmed pulse behind sheet |
| Sheet surface | `#1E293B` | Bottom-sheet fill |
| Field / chip idle | `#334155` | Inputs, idle chips, close btn |
| Field / chip border | `#475569` | Idle chip + field stroke |
| Accent (primary) | `#6366F1` | Selected chip fill, CTA start, sheet glow |
| Accent soft / secondary | `#8B5CF6` | Pulse tagline on QA, More details, WHY label, CTA end |
| Accent soft (masters pulse) | `#818CF8` | Masters “Plan. Launch. Measure.” tagline |
| Accent soft fill | `rgba(99,102,241,0.12–0.15)` | Soft chip / pill fills |
| Amount / sheet glow border | `rgba(99,102,241,0.40)` | Sheet top border / amount hero stroke |
| CTA gradient | `#6366F1` → `#8B5CF6` | Sticky primary button |
| WHY fill | `rgba(139,92,246,0.12)` | WHY panel bg |
| WHY border | `rgba(139,92,246,0.40)` | WHY + More details stroke |
| WHY body | `#C4B5FD` | WHY copy |
| Label / muted | `#94A3B8` | Field labels, context, sticky meta |
| Hint / placeholder | `#64748B` | Empty optional placeholders |
| Value / ink | `#F8FAFC` | Titles + filled values |
| Grab | `#64748B` | Handle |
| Positive / danger | `#22C55E` / `#EF4444` | Status badges only |

**Rule:** Marketing-Client must **not** use Group lilac, Trading emerald, Dist orange, MFG amber, Own magenta, or Retail rose for capture chrome.

### Map → existing code (match / gap)

| Figma token | APK today | iOS today | Gap |
|-------------|-----------|-----------|-----|
| Accent `#6366F1` | `OperatingV12Theme.marketingClient()` / `CampBrand` / `MarketingClientSetupDocTheme.accent` | `BusinessOperatingVertical.clientCampaign` accent | **Match** (pulse/hub) |
| Hero gradient → `#8B5CF6` | `heroEnd` on marketingClient | theme hero | **Match** |
| Soft `#818CF8` | `marketingAccentSoft` / setup | partial | Soft OK for masters |
| Sheet `#1E293B` | Pulse cards use CampCard; **capture often falls through `OperatingCaptureChromeTokens.Group` lilac** | `OperatingTheme.captureSheet` defaults to `canvas` for non-TRD/DST | **Gap — need MarketingClient chrome** |
| Field `#334155` | CampField-ish on invite; capture Shared lilac | `captureField` default `inset` | **Gap** |
| WHY violet panel | Generic / outdated WHY strings | `MarketingCaptureMode.whyNote` ≠ Figma | **Gap — copy + chrome** |
| CTA indigo→violet | Hub tiles yes; capture CTA often solid accent | Solid accent | **Gap — gradient CTA on capture** |

**Implement:** `OperatingCaptureChromeTokens.MarketingClient` (APK) + extend `OperatingTheme` for `.clientCampaign` capture sheet/field/cyan(=violet WHY)/whyBody. Own stays magenta when `OWN_BUSINESS` (separate token object later).

---

## 3. Full inventory of 51 listed nodes

### Critical finding — MFG + MKT mix (not Service)

The 51 pasted node IDs are **not** all Marketing-Client. Inventory via `use_figma` (2026-11-10):

| Family | Count | Prefix | Action for this plan |
|--------|-------|--------|----------------------|
| **Manufacturing** | **31** | `MFG-*` (2545 / 2547 / 2549 / 2396) | **Out of scope** — already planned in `2026-11-10-mfg-quickadds-figma-fidelity.md` |
| **Marketing-Client** | **20** | `MKT-*` (2569 / 2573 / 2390) | **In scope SoT** |
| Service | 0 | — | None present |

User wording also said “service”; **no SVC frames** appear in this list. Default SoT for *this* plan = the **20 MKT nodes** below. MFG nodes are listed only for provenance / rejection.

### 3A. Out of scope — Manufacturing (31) → MFG plan

| nodeId | Name | Type |
|--------|------|------|
| `2545:29335` | MFG-MASTER · Invite & QR | master |
| `2545:29419` | MFG-MASTER · Vendors & buyers | master |
| `2545:29477` | MFG-MASTER · Products & BOM | master |
| `2545:29557` | MFG-MASTER · Lines & stores | master |
| `2545:29614` | MFG-FORM · New / Edit Vendor | form |
| `2545:29705` | MFG-FORM · Add / Edit Line or Store | form |
| `2545:29784` | MFG-MASTER · Create Part / SKU | form |
| `2545:29909` | MFG-MASTER · Create BOM / WO item | form |
| `2547:29335` | MFG-QA-HOLD · Work order hold | ops |
| `2547:29449` | MFG-QA-TRANSFER · Stock / WIP transfer | ops |
| `2547:29539` | MFG-QA-REPRINT · Reprint invoice / job card | ops |
| `2547:29629` | MFG-SHEET · Parts browser (SO / WO) | browser |
| `2547:29687` | MFG-SHEET · Materials browser (PO) | browser |
| `2549:29336` | MFG-PICKER · Date | picker |
| `2549:29460` | MFG-PICKER · Month | picker |
| `2549:29517` | MFG-PICKER · Date & time | picker |
| `2549:29591` | MFG-SHEET · Switch Moment | switch |
| `2396:30095`…`2396:30932` | MFG-QA-01…14 (Sale→Rework) | capture |

### 3B. In scope — Marketing-Client (20 listed)

#### Capture sheets (core)

| Code | Title | nodeId | Context (Figma) | Mode chips | Key fields | WHY | CTA | Type |
|------|-------|--------|-----------------|------------|------------|-----|-----|------|
| MKT-QA-01 | Add Activity | `2390:30430` | `{Campaign} · Client Campaign · {period}` | Creative · Publish · Booking · Event · **Hold** | Activity name (M), type (M), Owner (M), Due (M), Milestone (O), Channel (C) | Activity is a plan item — no money. Spend/results later. | Save activity | capture |
| MKT-QA-02 | Update Progress | `2390:30504` | Pick an open activity… | Incremental · Cumulative | Progress % (M), Activity (M), As of (M), Status (M), Note (O) | Incremental vs cumulative explicit — never double-count. | Save progress | capture |
| MKT-QA-03 | Record Spend | `2390:30569` | `{Campaign} · Budget vN · used %` | Incurred · Accrued · Reimbursable · **Payment** | Amount (M), Vendor (M), Activity (M), Spend date (M), Channel (C), Due (C) | Spend ≠ payment. Creates payable; cash leaves on Payment. | Save spend | capture |
| MKT-QA-04 | Record Payment | `2390:30642` | Settle open payable · AP … | Partial · Full | Amount (M), Open payable (M), Paid on (M), Method (M), Reference (O) | Payment only settles open payable — never new spend / budget use. | Save payment | capture |
| MKT-QA-05 | Record Invoice | `2390:30707` | Client Campaign · {Client} | **Client only** gate · Retainer · Deliverable · Pass-through · Reprint | Amount (M), Client (M), Deliverable (M), Invoice no. (M), Due (M), Tax (C) | Invoice ≠ collection. Never implies cash. | Save invoice | capture (client-only) |
| MKT-QA-06 | Record Collection | `2390:30784` | Against open invoice · AR … | **Client only** gate · Partial · Full | Amount (M), Open invoice (M), Received on (M), Method (M), TDS (C) | Collection reduces AR — never duplicate revenue. | Save collection | capture (client-only) |
| MKT-QA-07 | Budget / Target Revision | `2390:30853` | Current: Budget vN · Target … | Budget · Target · **Transfer** | New budget/target (M), Effective from (M), Reason (M), Approver (C) | Revisions versioned + effective-dated. Past achievement keeps denominator. | Save revision vN | capture |
| MKT-QA-09 | Team Update / Issue | `2390:30972` | `{Campaign} · visible to team` | Update · Issue · Risk · **Approval** | Title (M), Activity (C), Severity (C), Assignee (O), Due (O) | Issues/risks raise Pulse alerts; plain updates do not. | Post update | capture |
| MKT-QA-10 | Campaign Result | `2390:31038` | Record a measured outcome | Leads · Reach · Clicks · Orders · Sales | Value (M), Metric (M), Channel (M), Period (M), Coverage (M), Attributed sales (O) | Empty ≠ zero. ROAS ≠ profit; attributed sales ≠ booked revenue. | Save result | capture |

#### Not in the 51-list but required Client SoT

| Code | Title | nodeId | Notes |
|------|-------|--------|-------|
| **MKT-QA-08** | Approval / Decision | `2390:30912` | **Missing from paste** but exists beside QA-09. Chips Approve · Reject · Changes. WHY: Approval ≠ disbursement; no self-approve. CTA: Save decision. **Include in implementation.** |

#### Ops extensions (listed; UI-only until API exists)

| Code | Title | nodeId | WHY (Figma) | CTA | Code status |
|------|-------|--------|-------------|-----|-------------|
| MKT-QA-HOLD | Campaign hold | `2573:29329` | Hold pauses campaign. No spend, billing, GST; resume continues. | Hold campaign · CH-… | Hub tile `unavailable=true` |
| MKT-QA-TRANSFER | Budget transfer | `2573:29443` | Moves planned budget between channels; no revenue/AP/GST. | Confirm transfer · ₹… | Hub tile unavailable |
| MKT-QA-REPRINT | Reprint invoice | `2573:29533` | Same invoice no. + GST split; no new sale/AR. | Reprint · duplicate copy | Hub tile unavailable (client) |

Chip destinations from capture: Activity **Hold** → HOLD sheet; Spend **Payment** → PAYMENT; Budget **Transfer** → TRANSFER; Update **Approval** → APPROVAL; Invoice **Reprint** → REPRINT.

#### Masters / forms (listed)

| Frame | nodeId | Type |
|-------|--------|------|
| Invite & QR | `2569:29329` | master (roles: Owner / Manager / Account lead / Media buyer / Creative) |
| Clients & vendors | `2569:29413` | master list |
| Services & rate card | `2569:29471` | master list |
| Channels & teams | `2569:29553` | master list |
| New / Edit Client or Vendor | `2569:29610` | form |
| Add / Edit Channel or Team | `2569:29701` | form |
| Create service | `2569:29780` | form |
| Create rate card | `2569:29905` | form |

#### Missing from Client list (exist for Own / shared pattern)

Client-specific **Date / Month / DateTime pickers** and **Switch Moment** were not in the 51 paste (MFG pickers were). Prefer theming shared operating pickers with MarketingClient chrome (same as Dist phase 3). Own pickers live under `MKT-OWN-PICKER-*` — out of scope.

---

## 4. Shared chrome components

Reuse / extend (do not fork a second sheet system):

| Chrome piece | Behavior (Figma) | APK anchor | iOS anchor |
|--------------|------------------|------------|------------|
| Sheet shell | `#1E293B`, top radius ~28, indigo glow border | `OperatingCaptureChrome` + scrim | `MarketingCaptureSheet` / capture sheet |
| Grab + header + close | 40×4 grab; 22pt title; 32 close | Shared capture header | Same |
| Context line | 12pt `#94A3B8` under title | `quickAddHint` / context | Context `Text` |
| Mode chips | Selected `#6366F1` + `#8B5CF6` border; idle `#334155`/`#475569` | Chip row | `OperatingPill` / chips |
| Client-only gate | Chip “Client only” + helper text | Show when Invoice/Collection | Same |
| Amount-first hero | Large ₹ value, INR pill, indigo border | Port `AmountBlock` under MKT chrome | Port Trading amount hero |
| Field rows | Label (M/O/C) + `#334155` row + › | Capture fields | field helpers |
| More details | Dashed/violet border; “Notes · Attachment · Tags” | `MoreDetails` | Add accordion (parity Trading) |
| WHY panel | Violet fill/border; “Why” + `#C4B5FD` body | `TradingWhyPanel`-style with MKT tokens | `OperatingWhyFootnote` retoken |
| Sticky CTA | Gradient indigo→violet; draft meta under | Footer | Footer |
| Chip routing | Hold/Payment/Transfer/Approval/Reprint open target sheet | `quickAddHint` / reopen | `onRouteToMode` |

**Catalog:** Create `MarketingClientQuickAddCopy` (APK + iOS) with context suffixes, WHY, CTA, more-details label, sticky meta builders — same pattern as `TradingQuickAddCopy` / `DistributionQuickAddCopy`.

---

## 5. Per-sheet / file touch list (APK + iOS)

### APK
| File | Touch |
|------|-------|
| **Create** `…/marketing/MarketingClientQuickAddCopy.kt` | Context / WHY / CTA / more-details / sticky meta |
| **Create** test `…/MarketingClientQuickAddCopyTest.kt` | Assert Figma literals |
| `…/shared/OperatingCaptureChrome.kt` | Add `MarketingClient` tokens; extend `isOpsQa` **or** `isMarketingClientQa()` |
| `…/shared/OperatingQuickSheet.kt` | Client variant bodies, context, WHY, chips, amount hero, Client-only gate |
| `…/marketing/MarketingQuickAdds.kt` | Hub titles/captions; wire chrome; chip routing; keep HOLD/TRANSFER/REPRINT unavailable **or** open chrome-only sheets |
| `…/marketing/MarketingActive.kt` | Ensure accent/canvas stay Client indigo |
| `…/shared/OperatingV12Kit.kt` | Confirm `marketingClient()` matches palette table |
| Masters (phase 2) | Invite roles, Clients/vendors, Services/rate, Channels/teams forms under Client chrome |

### iOS
| File | Touch |
|------|-------|
| **Create** `…/Marketing/MarketingClientQuickAddCopy.swift` | Mirrored catalog |
| `…/OperatingComponents.swift` | `captureSheet` / `captureField` / WHY for `.clientCampaign`; amount hero + More details helpers |
| `…/Marketing/MarketingQuickAdds.swift` | Replace outdated `whyNote` / titles with catalog; field order parity; Client-only gate; mode chips |
| `…/Marketing/MarketingActiveView.swift` | Hub captions only if needed |
| `…/OperatingExperienceHost.swift` | Sheet routing if chip reopen needed |
| `…/Sheets/OperatingMastersView.swift` | Phase 2 Client master chrome/copy |
| `…/Sheets/OperatingCaptureSheet.swift` | Only if Marketing still routes through shared capture for some modes |

### Backend (read-only for this plan)
| File | Note |
|------|------|
| `operating-catalog` / `operating-marketing` | Client = 10 QAs; Own rejects INV/COL; no HOLD/TRANSFER/REPRINT codes — **do not invent** |

---

## 6. Implementation sequence

1. **Catalog** — Lock §3B copy table into `MarketingClientQuickAddCopy` (APK + iOS) + unit tests. Include QA-08 even though omitted from paste.
2. **Chrome tokens** — `MarketingClient` palette; wire `variant=client` / `.clientCampaign` capture providers; stop Group lilac on Client capture.
3. **Core capture sheets (QA-01…07, 09, 10 + 08)** — Context, chips, fields, amount-first, More details, WHY, CTA, Client-only gates, chip routing among existing modes.
4. **Ops chrome (HOLD / TRANSFER / REPRINT)** — Optional visual sheets matching Figma **without** posting new event types; keep hub honest (`unavailable`) unless product unblocks APIs.
5. **Masters / forms** — Invite roles + party/service/channel/rate forms under Client chrome (phase 2).
6. **Pickers / Switch** — Theme shared date/month/datetime + Switch Moment with Client indigo (phase 3).
7. **Verify** — Side-by-side Figma screenshots vs device for each in-scope frame; compile APK (Temurin 21) + iOS build.

Checkbox tracking for workers:

- [ ] Catalog + tests both platforms  
- [ ] MarketingClient chrome tokens wired  
- [ ] QA-01…10 (incl. 08) layout/content parity  
- [ ] Client-only Invoice/Collection gate UI  
- [ ] Chip routing (Hold/Payment/Transfer/Approval/Reprint)  
- [ ] Masters phase 2  
- [ ] Pickers phase 3  
- [ ] Acceptance checklist pass  

---

## 7. Acceptance criteria

1. Opening each Client QA sheet on APK **and** iOS shows Figma title, context semantics, mode chips, field labels (incl. M/O/C meaning), WHY text, and primary CTA from §3B (brand/runtime values substituted for Brightline samples).
2. Capture chrome uses indigo/violet palette in §2 — **no** lilac / emerald / orange / magenta / amber on Client capture.
3. Invoice + Collection show Client-only gate; Own campaign never offers them (existing server reject stays).
4. More details control present on money/ops sheets; notes not always-forced above fold.
5. Amount-first hero on Spend / Payment / Invoice / Collection / Budget (and Result value hero) matches Figma hierarchy.
6. Chip navigation opens the target sheet (or honest unavailable) — never silent no-op.
7. Mirrored catalogs: APK test + iOS assert WHY/CTA/context suffixes match.
8. MFG nodes from the paste are **not** implemented under Marketing-Client work.
9. HOLD/TRANSFER/REPRINT either remain unavailable tiles **or** open chrome-only sheets that do not invent API writes.

---

## 8. Risks

| Risk | Mitigation |
|------|------------|
| 51-node paste mixes MFG + MKT | This plan scopes 20 MKT (+ QA-08); MFG deferred to MFG plan |
| QA-08 missing from paste | Pull `2390:30912` as SoT; do not skip Approval |
| HOLD/TRANSFER/REPRINT in Figma but no API | Keep unavailable; optional chrome-only; document product decision |
| Client indigo vs Own magenta bleed | Guard every chrome provider on subtype; never share Own tokens |
| iOS `MarketingCaptureSheet` still generic forms | Rebuild field order/chips against §3B; don’t only recolor |
| APK Marketing capture still on Group lilac | Must add `MarketingClient` chrome before visual QA |
| Chip “Hold” on Activity vs unavailable hub tile | Route to chrome sheet or hide chip until API — pick one and match both platforms |
| “Service” wording confusion | Explicitly out of scope; no SVC nodes in set |

---

## 9. Suggested PR split

| PR | Scope |
|----|-------|
| **PR1** | `MarketingClientQuickAddCopy` APK + iOS + unit tests (catalog only) |
| **PR2** | `OperatingCaptureChromeTokens.MarketingClient` + iOS theme capture tokens; wire Client variant |
| **PR3** | Core capture bodies QA-01…10 (incl. 08): fields, amount hero, More details, WHY, CTAs, Client-only gates |
| **PR4** | Chip routing + Approval/Payment/Hold destinations; sticky meta polish |
| **PR5** | Masters / forms Client chrome + Invite roles (phase 2) |
| **PR6** | Pickers + Switch Moment Client theme (phase 3); optional HOLD/TRANSFER/REPRINT chrome-only |

---

## Appendix — Copy SoT shortcuts (implement these WHY / CTAs)

| QA | WHY | CTA |
|----|-----|-----|
| 01 | An activity is a plan item — it carries no money. Spend and results are recorded against it later. | Save activity |
| 02 | Incremental vs cumulative is explicit so updates are never double-counted. | Save progress |
| 03 | Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment. | Save spend |
| 04 | A payment only settles an open payable — it never creates new spend or changes budget use. | Save payment |
| 05 | Invoice ≠ collection. Raising an invoice never implies cash received. | Save invoice |
| 06 | Collection reduces AR on an existing invoice — it never creates duplicate revenue. | Save collection |
| 07 | Revisions are versioned and effective-dated. Past achievement keeps its original denominator. | Save revision v2 |
| 08 | Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action. | Save decision |
| 09 | Issues and risks raise alerts on Pulse; plain updates do not. | Post update |
| 10 | Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue. | Save result |
| HOLD | Hold pauses a campaign. No spend, no billing and no GST; resume continues the same campaign. | Hold campaign · … |
| TRANSFER | Transfer moves planned budget between channels: − from, + to on confirm. No revenue, no AP and no GST — spend posts only on vendor bill. | Confirm transfer · … |
| REPRINT | Reprint re-renders the posted invoice with the same number and GST split (IGST for out-of-state clients). No new sale, no AR change. | Reprint · duplicate copy |

More details label: `More details` + hint `Notes · Attachment · Tags`  
Sticky meta (most captures): `Draft preserved if you close or open a picker`
