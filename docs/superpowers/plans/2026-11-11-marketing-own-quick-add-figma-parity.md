# Marketing-Own Quick Add Figma Parity Implementation Plan

> **For agentic workers:** Implement APK + iOS Own Business capture parity. Client indigo stays untouched.

**Goal:** Bring Marketing **Own Business Campaign** (`OWN_BUSINESS_CAMPAIGN`) Quick Add bottom sheets to Figma layout + content parity on **APK + iOS**, mirroring Client / Trading / Distribution (palette, mirrored copy catalog, capture chrome, chip routing, masters).

**Architecture:** Reuse Client field layouts where Figma matches; retoken with **Own magenta → amber**. Gate **Invoice / Collection / Reprint** off Own (no Own Figma frames). Do not invent HOLD / TRANSFER write APIs.

**Tech Stack:** Kotlin Compose (APK), SwiftUI (iOS), Figma `TzLvwVwlPbeVB8ug1zB3GM`.

**Figma session:** `2c5d8ba0-e778-49ec-935f-03cdd3cab70a` (2026-11-11). Demo brand: **Harbor & Loom** · “Plan. Launch. Measure.”

**Related:** Client `2026-11-10-marketing-client-quick-add-figma-parity.md`, Trading / Distribution / MFG plans same folder.

---

## 1. Scope / non-goals

### Goals
- Own QA sheets 01–04, 07–10 (Activity, Progress, Spend, Payment, Budget, Approval, Update, Result).
- Magenta capture palette (§2) on APK + iOS.
- `MarketingOwnQuickAddCopy` mirrored catalog.
- Masters (Vendors & creators, Services & rate card, Channels & teams, Invite) under Own chrome.
- Chip routing: Hold / Payment / Transfer / Approval (chrome-only for Hold/Transfer).
- Keep Invoice + Collection **hidden** on Own hub (server-reject stays).

### Non-goals
- Marketing-Client indigo frames (`2569` / `2573` / `2390`) — do not retoken.
- Manufacturing (`2545` / `2547` / `2549` / `2396`) — MFG plan.
- Invoice / Collection / Reprint Own surfaces (absent from Own SoT).
- New HOLD / TRANSFER write APIs.

### Own vs Client
| Surface | Client | Own |
|---------|--------|-----|
| Accent | Indigo `#6366F1` → violet `#8B5CF6` | Magenta `#D946EF` → amber `#F59E0B` |
| Invoice / Collection / Reprint | Shown | Hidden |
| Figma | `MKT-QA-*` Brightline | `MKT-OWN-QA-*` Harbor & Loom |
| Hub | 10 QAs | 8 shared only |

---

## 2. Color palette (Marketing-Own)

> **Palette SoT upgrade (2026-11-11):** Capture QA table below remains valid. Full Pulse + chrome retoken (incl. canvasTop `#1E1B4B`, Own ≠ Client indigo proof, file touch list) lives in **`2026-11-11-marketing-own-palette-correction.md`**.  
> **Verdict:** Own accent is **magenta `#D946EF`** (not indigo `#6366F1`, not violet). Secondary **amber `#F59E0B`**. Pulse canvas wash shares Client’s deep `#1E1B4B` but brand chrome is magenta.

Pulled from `2583:29187` (Activity) + Spend CTA/amount (`2583:29342`); Pulse cross-check `2582:29187`.

| Token | Hex / value | Use |
|-------|-------------|-----|
| Canvas | `#0F172A` | Dimmed pulse |
| Canvas top wash (Pulse) | `#1E1B4B` | Pulse root gradient start (not card surface) |
| Sheet | `#1E293B` | Bottom sheet |
| Pulse card surface | `#2A1030` | Pulse section cards (not capture sheet) |
| Field / chip idle | `#334155` | Inputs, idle chips |
| Chip / field border | `#475569` | Idle stroke |
| Accent | `#D946EF` | Selected chip, sheet glow, CTA start |
| Accent soft (secondary) | `#F59E0B` | Chip stroke, More details, WHY label, CTA end |
| Soft magenta (masters) | `#E879F9` | Masters tagline / links |
| Accent fill soft | `rgba(217,70,239,0.12–0.15)` | Soft pills |
| Sheet / amount glow | `#D946EF` @ 0.40 / solid | Sheet stroke; amount hero |
| CTA gradient | `#D946EF` → `#F59E0B` | Sticky primary |
| WHY fill | `#F59E0B` @ 0.12 | WHY panel |
| WHY border | `#F59E0B` @ 0.40 | WHY + More details |
| WHY body | `#FCD34D` | WHY copy |
| Label / muted | `#94A3B8` | Labels, context, sticky meta |
| Hint | `#64748B` | Placeholders |
| Value / ink | `#F8FAFC` | Titles + values |
| Masters WHY body | `#FAE8FF` | Master sheets only |

**Rule:** Own must not use Client indigo/violet, Group lilac, Trading emerald, Dist orange, or MFG-only amber as primary accent (amber is Own secondary only).

---

## 3. Inventory of 70 pasted nodes

| Family | Count | Prefix / parent | Action |
|--------|-------|-----------------|--------|
| **Manufacturing** | 31 | `MFG-*` (2545/2547/2549/2396) | **Out of scope** |
| **Marketing-Client** | 20 | `MKT-*` under `client` (2569/2573/2390) | **Out of scope** (Client plan) |
| **Marketing-Own** | **19** | `MKT-OWN-*` under `own business` (2583/2584) | **In scope SoT** |

### 3A. In scope — Own (19)

#### Capture (`2583:*`)
| Code | Title | nodeId | Chips | CTA |
|------|-------|--------|-------|-----|
| MKT-OWN-QA-01 | Add Activity | `2583:29187` | Creative · Publish · Booking · Event · Hold | Save activity |
| MKT-OWN-QA-02 | Update Progress | `2583:29268` | Incremental · Cumulative | Save progress |
| MKT-OWN-QA-03 | Record Spend | `2583:29342` | Incurred · Accrued · Reimbursable · Payment | Save spend |
| MKT-OWN-QA-04 | Record Payment | `2583:29424` | Partial · Full | Save payment |
| MKT-OWN-QA-07 | Budget / Target Revision | `2583:29498` | Budget · Target · Transfer | Save revision vN |
| MKT-OWN-QA-08 | Approval / Decision | `2583:29569` | Approve · Reject · Changes | Save decision |
| MKT-OWN-QA-09 | Team Update / Issue | `2583:29638` | Update · Issue · Risk · Approval | Post update |
| MKT-OWN-QA-10 | Campaign Result | `2583:29710` | Leads · Reach · Clicks · Orders · Sales | Save result |

**Absent on Own SoT:** Invoice, Collection, Reprint.

#### Masters / pickers / sheets (`2584:*`)
| Frame | nodeId |
|-------|--------|
| Month picker | `2584:29188` |
| View all · Target & Budget / Needs Attention / Payment Readiness / Finance / Activity | `2584:29250`…`29594` |
| Switch Moment | `2584:29680` |
| Vendors & creators | `2584:29768` |
| Services & rate card | `2584:29833` |
| Channels & teams | `2584:29924` |
| Invite & QR | `2584:29990` |

### 3B. Out of scope (from paste)
- All `MFG-*` (31) and Client `MKT-*` (20) — listed in Client/MFG plans; do not implement under Own.

---

## 4. Implementation sequence

1. **Catalog** — `MarketingOwnQuickAddCopy` APK + iOS + unit tests (Own Business Campaign context; shared WHY/CTA with Client for overlapping modes). ✅
2. **Chrome** — `OperatingCaptureChromeTokens.MarketingOwn`; iOS `OperatingTheme` `.ownBusinessCampaign` capture sheet/field/WHY amber. ✅
3. **Wire Own layouts** — Reuse Client field registries for shared modes; select Own copy + magenta chrome when `variant=own` / `.ownBusinessCampaign`. ✅
4. **Gates** — Invoice/Collection/Reprint never offered on Own hub; Collection capture stays Client-gated. ✅
5. **Chip routing** — Own: Hold → chrome-only, Payment, Transfer → chrome-only, Approval; no Reprint. ✅
6. **Masters** — Own titles (Vendors & creators) + magenta invite palette. ✅
7. **Verify** — Compile APK; copy unit tests. ✅

### Deferrals / hard blockers
- **View-all Pulse sheets** (`2584:29250`…`29594`) — ✅ Own magenta glow / soft-link / Own chip strokes (APK + iOS); ✅ sticky period/status density + slate sheet + metric row chrome matched to `2584:29250`.
- **Month picker / Switch Moment** Own-specific chrome polish — shared operating pickers; retoken follow-up.
- **HOLD / TRANSFER write APIs** — chrome-only preview only; hub tiles remain `unavailable`; no inventing event types.
- **Invoice / Collection / Reprint** — absent from Own Figma SoT; intentionally gated off.

---

## 5. Acceptance

1. Each Own QA sheet shows Figma title, Own Business Campaign context, chips, WHY, CTA, magenta→amber chrome.
2. Client indigo unchanged.
3. Own never shows Invoice/Collection/Reprint tiles or Client-only rose gate.
4. Mirrored catalogs + APK unit tests pass.
5. Non-Own paste nodes documented as out of scope.

---

## Appendix — WHY / CTA (Own = Client for shared modes)

| QA | WHY | CTA |
|----|-----|-----|
| 01 | An activity is a plan item — it carries no money. Spend and results are recorded against it later. | Save activity |
| 02 | Incremental vs cumulative is explicit so updates are never double-counted. | Save progress |
| 03 | Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment. | Save spend |
| 04 | A payment only settles an open payable — it never creates new spend or changes budget use. | Save payment |
| 07 | Revisions are versioned and effective-dated. Past achievement keeps its original denominator. | Save revision v2 |
| 08 | Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action. | Save decision |
| 09 | Issues and risks raise alerts on Pulse; plain updates do not. | Post update |
| 10 | Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue. | Save result |

Sticky meta: `Draft preserved if you close or open a picker`  
More details: `More details` · `Notes · Attachment · Tags`

---

## Deferrals / gaps

| Item | Status | Notes |
|------|--------|-------|
| View-all pulse sheets (`2584:29250`…`29594`) | Closed | Own sticky chrome density (period vs status chips, 20sp title, slate sheet, metric rows) + magenta glow (APK + iOS) |
| Switch Moment (`2584:29680`) | Closed | Own `storyAccentSoft` amber wired for Switch/Month story chrome |
| Month picker (`2584:29188`) Own retoken | Closed | Capture chrome + storyAccentSoft Own amber |
| Masters list sheets (Vendors / Services / Channels) | Closed | Hub titles + Invite `#FAE8FF` WHY; list masters still share capture field chrome |
| HOLD / TRANSFER write APIs | Deferred / chrome-only | Hub tiles unavailable; chip routes open preview sheets that never post |
| Invoice / Collection / Reprint on Own | Intentionally absent | No Own Figma frames; hub + server reject stay |
| Pixel-perfect typography / spacing | Deferred | Layout + copy + palette parity first |

## Compile / verify checklist

- [x] Plan + Color palette (§2)
- [x] `MarketingOwnQuickAddCopy` APK + iOS + unit tests
- [x] `OperatingCaptureChromeTokens.MarketingOwn` + iOS `.ownBusinessCampaign` capture tokens
- [x] Own hub excludes Invoice/Collection/Reprint; chip routing Hold/Payment/Transfer/Approval
- [x] APK compile + Own copy unit tests (Temurin 21 — BUILD SUCCESSFUL; Own 8/8 + Client 7/7)
- [x] iOS copy/chrome wired (filesystem-synced Xcode group); full Xcode device build not run here
