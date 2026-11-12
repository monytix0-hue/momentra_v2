# Marketing-Own Palette Correction Plan (Pulse SoT)

> **IMPLEMENTED (2026-11-11)** — Own Business palette locked from Figma Pulse `2582:29187`. Client indigo path untouched.  
> Amends / supersedes the palette claims in `2026-11-11-marketing-own-quick-add-figma-parity.md` §2 for **Pulse + shared Own chrome**. Capture QA sheet chrome already matched Figma; this plan retokened **Own Business surfaces** so none silently look like Client indigo.

**Goal:** Lock the **real Own Business palette** from Figma Pulse `2582:29187`, prove **Own ≠ Client indigo**, then retoken APK + iOS Own surfaces (Pulse, Quick Adds, masters, pickers, hub, setup) to that SoT.

**Figma:** `TzLvwVwlPbeVB8ug1zB3GM`  
**Pulse SoT:** `2582:29187` (`MKT-OWN-PULSE-v12 · Harbor & Loom`)  
**QA cross-check:** `2583:29187` (Add Activity) · Invite `2584:29990`  
**Session:** `4e8fa2ac-7f39-4b3e-bf90-2ba461f9fed0` (2026-11-11)

---

## 0. Verdict (explicit)

| Question | Answer | Figma paint evidence |
|----------|--------|----------------------|
| Is Own **indigo** `#6366F1`? | **No.** That is **Client** only. | Client accent lives on MKT-PULSE / MKT-QA; Own never fills chips/CTA/tab with `#6366F1`. |
| Is Own **magenta**? | **Yes.** Primary accent is **magenta / fuchsia `#D946EF`**. | Pulse active chip `bg-[#d946ef]`; hero `rgb(217,70,239)→rgb(245,158,11)`; tab label `text-[#d946ef]`; QA CTA `from-[#d946ef] to-[#f59e0b]`; FAB `bg-[#d946ef]`. |
| Secondary? | **Amber `#F59E0B`** (not violet). | Hero end; chip border on selected; section codes; More details / WHY; owner subtitle. |
| Soft / link? | **`#E879F9`** | “View all ›”; masters tagline / section labels; Invite WHY label. |
| Why it can *feel* indigo | Pulse **canvas gradient start** is **`#1E1B4B`** (deep indigo wash — same hex as Client `marketing.canvasTop`). | Root: `from-[#1e1b4b] via/to-[#0f172a]`. This is a **shared dark wash**, not the Own brand accent. |

**One-liner:** Own Business = **magenta `#D946EF` → amber `#F59E0B`** on a slate/plum dark canvas; Client = **indigo `#6366F1` → violet `#8B5CF6`**. Do not conflate the shared `#1E1B4B` wash with Client’s accent.

---

## 1. Correct palette table (Pulse SoT + QA)

### 1A. Brand / Pulse (`2582:29187`)

| Token | Hex / value | Figma use |
|-------|-------------|-----------|
| Canvas mid / base | `#0F172A` | Root gradient via/to; topbar; navbar |
| Canvas top wash | `#1E1B4B` | Root `from-[#1e1b4b]` (indigo-deep wash) |
| Canvas deep (optional) | `#150618` / plum near-black | Token deep; hero-icon fill uses `#18081C` @ 0.9 |
| Card / section surface | `#2A1030` | Absolute card fill under AN-P0x sections |
| Inset / metric chip / list row | `#18081C` | Metric chips, list rows, target hero inset |
| Card stroke / glow | `rgba(217,70,239,0.28)` + magenta shadow | Section card border |
| Accent (primary) | `#D946EF` | Active context chip fill; progress labels; Alert soft chip text; tab active; FAB |
| Accent border (selected chip) | `#F59E0B` | Own Business Campaign chip stroke |
| Accent soft / secondary | `#F59E0B` | Owner line; AN-P0x codes; Unavailable / Behind pace |
| Soft magenta (links) | `#E879F9` | View all › |
| Hero gradient | `#D946EF` → `#F59E0B` (~150°) | Hero banner |
| Hero eyebrow | `#F5D0FE` | “FESTIVE… OWN BUSINESS” |
| Hero body | `#FAE8FF` | Hero stats line |
| Ink / titles | `#F8FAFC` | Headings, values |
| Muted | `#94A3B8` | Idle chip text, captions |
| Hint / idle tab | `#64748B` | Inactive nav labels |
| Idle chip fill / border | `#334155` / `#475569` | Non-selected context chips |
| Danger | `#F43F5E` / `#EF4444` | Alert chip; Overdue |
| Positive | green paints on Finance / Done (keep existing signal greens) | Paid / Done |

### 1B. Capture Quick Adds (`2583:29187` Activity — matches prior plan §2)

| Token | Hex / value | Use |
|-------|-------------|-----|
| Dimmed canvas | `#0F172A` | Underlying Pulse peek |
| Sheet | `#1E293B` | Bottom sheet fill |
| Sheet glow border | `rgba(217,70,239,0.40)` | Sheet stroke |
| Field / idle chip | `#334155` | Inputs, idle chips, close |
| Field / chip border | `#475569` | Idle stroke |
| Accent | `#D946EF` | Selected chip, CTA start |
| Accent soft | `#F59E0B` | Chip stroke, More details, WHY label, CTA end |
| WHY fill / border / body | amber `@0.12` / `@0.40` / `#FCD34D` | WHY panel |
| Label / hint / ink | `#94A3B8` / `#64748B` / `#F8FAFC` | Chrome text |

### 1C. Masters Invite (`2584:29990`) — soft-magenta family

| Token | Hex / value | Use |
|-------|-------------|-----|
| Tagline / section / WHY label | `#E879F9` | Soft magenta (not amber) |
| WHY fill / border / body | magenta `@0.12` / `@0.40` / `#FAE8FF` | Masters WHY (not QA amber WHY) |
| Selected role chip | `#D946EF` fill + `#E879F9` border | Role selection |
| CTA | `#334155` → `#D946EF` | Done (masters; not magenta→amber QA CTA) |

---

## 2. Diff vs current Own tokens

### 2A. Source of truth file (`design/tokens/business.tokens.json` → `marketingOwn`)

| Token | Today | Pulse SoT | Gap |
|-------|-------|-----------|-----|
| `canvas` | `#0F172A` | `#0F172A` | Match |
| `canvasTop` | ~~`#2A1030`~~ → **`#1E1B4B`** | `#1E1B4B` | **Fixed** |
| `canvasDeep` | `#150618` | deep plum OK | Keep |
| `surface` | `#2A1030` | `#2A1030` | Match |
| `inset` | `#18081C` | `#18081C` | Match |
| `accent` | `#D946EF` | `#D946EF` | Match |
| `accentSoft` | `#F59E0B` | `#F59E0B` | Match |
| `accentSoftLink` | **`#E879F9`** (added) | `#E879F9` | **Added** |
| `accentDeep` | `#A21CAF` | (derived) | Keep |
| `heroStart/End` | `#D946EF` / `#F59E0B` | same | Match |

### 2B–2C. Runtime gaps — closed in §4 checklist below.

---

## 3. Diff vs Client indigo (Own ≠ Client)

| Role | Client | Own |
|------|--------|-----|
| Accent | `#6366F1` | `#D946EF` |
| Accent soft / CTA end | `#8B5CF6` | `#F59E0B` |
| Soft link / masters | `#818CF8` / violet WHY `#C4B5FD` | `#E879F9` / masters WHY `#FAE8FF` |
| Hero | indigo → violet | magenta → amber |
| Card surface | `#1E1B4B` | `#2A1030` plum |
| Canvas wash | `#1E1B4B` → `#0F172A` | **Same wash hex** `#1E1B4B` → `#0F172A` (do not treat as “Own = indigo”) |
| Capture sheet | `#1E293B` + indigo glow | `#1E293B` + **magenta** glow |
| Selected chip | indigo fill | magenta fill + **amber** border |

**Acceptance gate:** On Own, zero UI chrome may use `#6366F1` or `#8B5CF6` as accent/CTA/selected chip (Client-only). Shared slate neutrals (`#0F172A`, `#1E293B`, `#334155`, `#475569`) are allowed.

---

## 4. File touch list (retoken ALL Own Business surfaces)

### Tokens / codegen
- [x] `design/tokens/business.tokens.json` — fix `marketingOwn.canvasTop` → `#1E1B4B`; add `accentSoftLink` `#E879F9`; cite `2582:29187`
- [x] Regenerate `momentra/momentra/Design/Generated/BusinessTokens.generated.swift` (`npm run business-tokens`)
- [x] Kotlin generator only emits domain accents (already `MARKETING_OWNAccent`); runtime Own surfaces hand-synced in themes

### APK — Pulse / hub / active
- [x] `OperatingV12Kit.kt` — `marketingOwn()` canvas `#0F172A`; added `surface` / `canvasTop` / `inset` / `softLink`
- [x] `BusinessActiveTheme.kt` — `MarketingOwn` `bg`/`card`/`border` Own plum + magenta tint; hero magenta→amber
- [x] `MarketingActive.kt` — Own canvas gradient, plum cards, amber selected-chip border, soft-link View all; Own uses `theme.accent` (not CampBrand)
- [x] Hub / FAB / tab — Own accent via `BusinessActiveTheme.MarketingOwn` / `OperatingV12Theme.marketingOwn` (`#D946EF`)

### APK — Quick Adds / capture / masters
- [x] `OperatingCaptureChrome.kt` — verified `MarketingOwn` magenta→amber QA (unchanged)
- [x] `MarketingQuickAdds.kt` — Own Invite soft `#E879F9` + plum card surfaces
- [x] `OperatingQuickSheet.kt` — already routes Own → `MarketingOwn` chrome
- [x] `OperatingMomentScreens.kt` — Own path now uses `OperatingCaptureChromeTokens.MarketingOwn` (was CampBrand indigo)
- [x] Masters / Invite — soft magenta on Own Invite palette
- [x] Month picker / Switch Moment Own accent polish — `storyAccentSoft` Own amber / Client violet

### APK — Setup
- [x] `MarketingSetupDocumentSpec.kt` — Own accent `#D946EF` + soft link `#E879F9` already present

### iOS — Pulse / hub / active
- [x] `BusinessTokens.generated.swift` (via regen) — `marketingOwnCanvasTop` `#1E1B4B`, `marketingOwnAccentSoftLink` `#E879F9`
- [x] `OperatingCatalog.swift` — `canvasTop` / `softLink`; Own motion line `Plan. Launch. Measure.`
- [x] `MarketingActiveView.swift` — canvas gradient, plum pulse/board cards, soft-link View all
- [x] `OperatingComponents.swift` — `canvasTop` / `softLink` / `pulseCardFill`; capture Own unchanged

### iOS — Quick Adds / masters
- [x] Capture Own chrome already magenta→amber (verified)
- [x] Invite / masters — Own uses Invite block with soft-magenta selected stroke
- [x] `BusinessSetupDocTheme.swift` — Own accent already magenta; Client left alone

### Docs
- [x] Prior QA plan §2 already points here + canvasTop correction note
- [x] This file = implementation checklist (status updated)

### Explicit non-touches
- Client (`MarketingClient`, `#6366F1` / `#8B5CF6`) — **untouched**
- Trading / Dist / MFG / Retail palettes
- Inventing new Own accent (SoT remains magenta)

---

## 5. Implementation sequence

1. **Token SoT** — ✅ `canvasTop` + `accentSoftLink`; Swift regen
2. **APK theme roots** — ✅ `marketingOwn` + `BusinessActiveTheme.MarketingOwn`
3. **Pulse surfaces** — ✅ APK + iOS plum cards, soft links, gradient
4. **Capture audit** — ✅ Own QA chrome still magenta→amber
5. **Masters / Invite** — ✅ soft-magenta Own Invite + `#FAE8FF` WHY panel
6. **Hub / FAB / tabs** — ✅ via theme accent
7. **Grep gate** — ✅ Own paths do not hardcode Client indigo accents (legacy `CampBrand` constant remains Client-only / deprecated)
8. **Month / Switch Moment** — ✅ `storyAccentSoft` = Own amber / Client violet
9. **Visual check** — device screenshots vs `2582:29187` / `2583:29187` / `2584:29990` still recommended

---

## 6. Acceptance criteria

1. **Accent identity:** Own primary accent is **`#D946EF`** everywhere brand chrome appears; never Client indigo/violet. ✅
2. **Hero / CTA:** Own Pulse hero + QA sticky CTA are **`#D946EF` → `#F59E0B`**. ✅
3. **Cards:** Own Pulse section cards are **`#2A1030`** with magenta glow. ✅
4. **Canvas:** Own Pulse background gradient **`#1E1B4B` → `#0F172A`**; token `canvasTop` matches. ✅
5. **Capture sheets:** Own QA sheets stay slate `#1E293B` + magenta glow; selected chips magenta + amber border; WHY amber/`#FCD34D`. ✅
6. **Masters:** Invite soft chrome uses **`#E879F9`**; WHY body **`#FAE8FF`**. ✅
7. **Client unchanged:** Client accent remains `#6366F1` → `#8B5CF6`. ✅
8. **Grep / review:** Own path no longer hardcodes CampBrand indigo for story chrome; APK `:app:compileDebugKotlin` ✅

---

## 7. Relationship to prior Own QA plan

`2026-11-11-marketing-own-quick-add-figma-parity.md` correctly stated **magenta → amber** for capture and is largely **implemented**. This correction plan:

- Re-validated that claim from **Pulse** SoT (not only QA)
- Fixed **canvasTop** mis-token (`#2A1030` vs `#1E1B4B`)
- Closed **Pulse/hub inheritance gaps** where Own still inherited Client/TeamOps indigo-adjacent chrome
- Clarified for humans: **shared `#1E1B4B` wash ≠ Own accent**

**Do not re-implement QA copy/catalog** unless a surface still shows indigo.

---

## 8. Leftovers / deferred

| Item | Notes |
|------|-------|
| Month picker / Switch Moment Own accent polish | ✅ Closed — `storyAccentSoft` Own amber / Client violet |
| Invite masters WHY body `#FAE8FF` full panel | ✅ Closed — APK + iOS Own Invite WHY panel |
| Device visual QA | ✅ Token cross-check vs Figma screenshots (Pulse `2582:29187`, view-all `2584:29250`, Invite `2584:29990`) — see §9; full device/Xcode still blocked on Linux |
| Deprecated `CampBrand` / `CampAccent` constants | Still exist as Client indigo aliases; Own runtime paths use `theme.accent` / `campBrandFor` / capture chrome |
| iOS Xcode compile | Not run in this environment (APK Kotlin compile verified) |
| Kotlin generated domain palette | Generator still emits accent-only for Android; full Own palette lives in runtime themes + Swift tokens |
| View-all Pulse board sheets Own theming | ✅ Closed — APK `MarketingViewAllSheet` magenta glow + soft-link Close; iOS `OperatingViewAllSheet` Own chip amber/magenta strokes |
| Dynamic Spend/Payment context amounts | ✅ Closed — Payment interpolates compact AP; Budget appends pacing/amount when projection has figures; catalog fallback when missing |

---

## 9. Visual QA notes (Figma re-pull · Linux host)

Re-pulled Figma file `TzLvwVwlPbeVB8ug1zB3GM` screenshots and compared to code tokens (no device/Xcode on this Linux host).

| Frame | Token match | Residual pixel gaps |
|-------|-------------|---------------------|
| Pulse `2582:29187` | Magenta→amber hero `#D946EF`→`#F59E0B`, plum cards `#2A1030`, canvas wash `#1E1B4B`→`#0F172A`, soft-link `#E879F9` — **match runtime Own theme** | Section card glow radius/alpha may be softer than masters; sticky bottom FAB chrome not pixel-diffed |
| View-all `2584:29250` | ✅ Own sticky chrome density (period solid + soft-link stroke; status soft-fill + magenta stroke; 20sp bold title; slate `#1E293B` sheet; magenta grab; metric rows with 3×32 bar + 28 icon tile) — APK + iOS | Invite list density / QR panel spacing not device-verified |
| Invite `2584:29990` | Magenta WHY panel + `#FAE8FF` body already wired | Invite list density / QR panel spacing not device-verified |

**Closed (2026-11-11 follow-up):** Compact money `K` (APK `OperatingV12Money` + iOS); Own view-all sticky period/status density vs `2584:29250`.

**Blocked:** iOS Xcode / Simulator / physical device screenshots — Linux host has no `xcodebuild`.
