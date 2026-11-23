# Figma → AppState drift report

## V4 SoT addendum (2026-11-19)

**Current source of truth for Business V4 AppState + BusinessTokens:** Figma file [`rknfN9EikSCdqmqXogR29o`](https://www.figma.com/design/rknfN9EikSCdqmqXogR29o/momentra?node-id=2-67&m=dev) · section node `2:67` (Obsidian Emerald).  
Palette pin: canvas `#05070B`, accent `#10B981`, CTA `#059669→#34D399`, amber `#F5A624`, coral `#FF5C38`, muted `#94A4B8`.  
AppState tokens **alias** generated `BusinessTokens` (from `design/tokens/business.tokens.json` v4).  
The historical TzLvw kit notes below remain useful for copy rules (never render missing figures as `0`) and variant coverage archaeology; do not treat TzLvw node IDs as the V4 SoT.



## V4 shell wire-up (2026-11-20)

Business AppShell no longer short-circuits to `BusinessV4Placeholder` /
`BusinessV4RebuildingView`. Live routing:

| State | Body |
|-------|------|
| Loading / Idle | `BusinessAppStateLoadingBody` via `BusinessAppStateHost` |
| Offline | Offline AppState body (`Waiting for connection`) |
| Error / Forbidden | Error AppState body + Retry |
| Empty + no company | NoCompany setup (Create → CREATE guided flow) |
| Empty/Setup + CREATE | `BusinessGuidedCreateView` / `BusinessGuidedCreateContent` (`2:1514`) |
| Empty Pulse/Moments/Life/Memory | Matching AppState empty bodies |

Historical call-site notes below that assume placeholders or `"Pulse · loading"`
bypass are **superseded** for BUSINESS once Host is mounted; Personal/Group
unchanged. Code Connect SoT file key is now `rknfN9EikSCdqmqXogR29o`
(`figma.config.json`); only published unmapped Continue (`2:32`) is mapped via
`Continue.figma.ts`.

---

**Legacy source of truth:** `frame-2348-29188.json` (Figma REST `/v1/files/…/nodes?ids=2348:29188`, ~88 MB, checked in at the repo root).
**File key:** `TzLvwVwlPbeVB8ug1zB3GM` · **Page:** `2348:29188` (`Business Redesign`) · **Section:** `2666:29489` (`App States · Shared (all Business verticals)`)
**Component set:** `AppState` = `2666:32397` — 17 variants.
**Extractor:** `python3 scripts/figma/dump-appstate-variants.py` (`--json`, `--set MomentSwitcher`).
**Date:** 2026-11-16

Nothing here was changed in app code. The report says what to fix; it does not fix it.

The kit's governing rule, from `BusinessAppStateSpecs.swift:99`: *a missing figure is never rendered as 0* — rows say `Not recorded` / `None yet` / `Quiet`, or the row is omitted. Every variant below holds that line.

## Variant map

Status legend: **Implemented** = Figma body copy renders verbatim on both platforms · **Copy-drift** = renders, but the string differs · **Missing** = no counterpart.

| Figma variant | Node ID | iOS | Android | Status |
|---|---|---|---|---|
| `Screen=Pulse, State=Empty` | `2666:29671` | `BusinessAppStateSpecs.swift` + `BusinessAppStateContent.swift` | `BusinessAppStateSpecs.kt` + `BusinessAppStateContent.kt` | Implemented |
| `Screen=Pulse, State=Loading` | `2666:29856` | `AppShellView.swift:1129-1130` (call site) | `AppShellScreen.kt:1641` (`businessLoadingLabel`, def at 1640) | Copy-drift (iOS) |
| `Screen=Pulse, State=Error` | `2666:30032` | `BusinessAppStateSpecs.swift` | `BusinessAppStateSpecs.kt` | Implemented |
| `Screen=Moments, State=Empty` | `2666:30215` | same | same | Implemented |
| `Screen=Moments, State=Loading` | `2666:30400` | `AppShellView.swift:1129-1130` | `AppShellScreen.kt:1642` | Copy-drift (iOS) |
| `Screen=Moments, State=Error` | `2666:30576` | `BusinessAppStateSpecs.swift:226` | `BusinessAppStateSpecs.kt:244` | Copy-drift (cosmetic) |
| `Screen=Life, State=Empty` | `2666:30759` | same | same | Implemented |
| `Screen=Life, State=Loading` | `2666:30944` | `AppShellView.swift:1129-1130` | `AppShellScreen.kt:1643` | Copy-drift (iOS) |
| `Screen=Life, State=Error` | `2666:31120` | same | same | Implemented |
| `Screen=Memory, State=Empty` | `2666:31303` | `BusinessAppStateSpecs.swift:289` | `BusinessAppStateSpecs.kt:311` | Copy-drift |
| `Screen=Memory, State=Loading` | `2666:31488` | `AppShellView.swift:1129-1130` | `AppShellScreen.kt:1644` | Copy-drift (iOS) |
| `Screen=Memory, State=Error` | `2666:31664` | same | same | Implemented |
| `Screen=Hub, State=Empty` | `2666:31852` | `BusinessAppStateSpecs.swift:331` (footnote) | `BusinessAppStateSpecs.kt:361` (footnote) | Copy-drift (Android) |
| `Screen=Hub, State=Error` | `2666:32028` | `BusinessAppStateSpecs.swift:337` | `BusinessAppStateSpecs.kt:368` | Copy-drift (cosmetic) |
| `Screen=ViewAll, State=Empty` | `2666:32064` | `BusinessAppStateContent.swift:288` | `BusinessAppStateContent.kt:381` | Implemented |
| `Screen=NoCompany, State=Setup` | `2666:32211` | `BusinessAppStateSpecs.swift:378` | `BusinessAppStateSpecs.kt:407` | Copy-drift (cosmetic) |
| `Screen=Offline, State=Error` | `2666:32395` | `BusinessAppStateSpecs.swift:106,390` | `BusinessAppStateSpecs.kt:422,433` | Copy-drift (cosmetic) |

**Variant coverage.** 17 of the 32 `Screen × State` combinations exist. Missing in Figma (15): `Setup` for Pulse/Moments/Life/Memory/Hub/ViewAll/Offline, `Loading` for Hub/ViewAll/NoCompany/Offline, `Error` for ViewAll/NoCompany, `Empty` for NoCompany/Offline. `BusinessAppStateSpecs.spec(_:_:)` still resolves all 32 at runtime (Pulse/Moments/Life/Memory `.setup` fold onto their `.empty` copy; Hub `.loading`/`.setup` fold onto `.empty`; ViewAll `.loading`/`.error` fold onto `.empty`), so those gaps are Figma coverage, not code drift. The Android debug gallery (`AppStateGalleryActivity.kt`) enumerates 21 pages: 14 AppState variants + 7 directory states — it is not a 1:1 mirror of the 17, and that is fine.

## Copy drift

**11 Figma strings do not render verbatim** (5 root causes), plus **1 code-only string** with no Figma counterpart. Everything else in the export matches once `Retry → RETRY` uppercasing and runtime formatting are accounted for.

| # | Drift | Figma strings | Platforms |
|---|-------|--------------|-----------|
| 1 | Memory body uppercases the month | 1 | iOS + Android |
| 2 | iOS loading title never reaches the Figma string | 4 | iOS |
| 3 | Android Hub footnote names no tile | 1 | Android |
| 4 | Android Hub ships a CTA pair the frame lacks | 0 (one invented code-only string) | Android |
| 5 | Straight apostrophes instead of `’` | 5 | iOS + Android |

### 1. Memory empty body uppercases the month — genuine, both platforms

| | |
|---|---|
| Figma (2666:31135) | `No closed activity for October 2026 yet. Memory writes itself as days close — nothing is estimated.` |
| iOS `BusinessAppStateSpecs.swift:289` | `"No closed activity for \(month) yet. …"` where `month = monthLabel(now)` |
| Android `BusinessAppStateSpecs.kt:311` | `"No closed activity for ${monthLabel(now)} yet. …"` |

`monthLabel` returns `MMMM yyyy` **`.uppercased()`** because Figma's *eyebrow* is `OCTOBER 2026` (2666:31137). The body reuses that same value, so the sentence renders `No closed activity for OCTOBER 2026 yet.` Figma's body is title case. **Genuine drift** — the fix is a second, non-uppercased formatter for the body sentence, not a change to `monthLabel` (which is correct for the eyebrow and is pinned by `BusinessAppStateSpecsTests.monthLabelMatchesFigmaCasing`).

The suggested `— nothing is e…` truncation is a **false positive**: the literal is complete and matches on both platforms.

### 2. iOS loading title never reaches the Figma string — genuine, iOS

| | |
|---|---|
| Figma (2666:29681, 30225, 30769, 31313) | `Loading Pulse…` · `Loading Moments…` · `Loading Life…` · `Loading Memory…` |
| iOS `AppShellView.swift:1129-1130` | `label: "\(model.bottomDestination.label) · loading"` → renders `Pulse · loading` |
| Android `AppShellScreen.kt:1641-1644` | `"Loading Pulse\u2026"` … `"Loading Memory\u2026"` — matches |

`BusinessAppStateSpecs.swift` carries the correct `title: "Loading Pulse…"` values, but the shell bypasses them and builds its own label from `BottomDestination.label`. That mapping returns `"Create"` for `.create` (`ShellModels.swift:30`), so the Hub loading page would read `Create · loading` while the spec's own title for that screen is `Loading Quick adds…` (Android already renders that form, `AppShellScreen.kt:1645`). **Genuine drift**, iOS only. Four existing variants are affected; the Hub case is the one that diverges furthest from Figma's naming.

### 3. Android Hub footnote points at a different affordance — genuine, Android

| | |
|---|---|
| Figma (2666:31699) | `Tap Sale to begin — the rest unlock as you go.` |
| iOS `BusinessAppStateSpecs.swift:331` | `"Tap Sale to begin — the rest unlock as you go."` ✓ |
| Android `BusinessAppStateSpecs.kt:361` | `"Tap a tile to begin — the rest unlock as you go."` ✗ |

**Genuine drift.** The design names the tile; "a tile" loses the instruction. One string.

### 4. Android adds a CTA pair the Hub frame does not have — genuine, Android

Figma `Screen=Hub, State=Empty` (2666:31852) contains only the hero, title, body, six tiles and the footnote — no button row. Android `BusinessAppStateSpecs.kt:359-360` sets:

```kotlin
primaryLabel = "Record first capture",
secondaryLabel = "Back to Pulse",
```

`BusinessAppStateHubBody` renders both under the grid (`BusinessAppStateContent.kt:315-320`). `Record first capture` is **invented** — it appears nowhere in the export (`rg -c "Record first capture" frame-2348-29188.json` → 0). `Back to Pulse` is a real Figma string, but only on Life/Memory/Hub-Error frames (2666:30606, 2666:31150, 2666:31874), never on Hub Empty. iOS `BusinessAppStateHubBody` renders only `spec.footnote` (`BusinessAppStateContent.swift:224-226`), so iOS matches Figma and Android does not. **Genuine drift** — extra UI, not just extra copy: one invented string plus a CTA pair the frame does not have.

### 5. Figma uses typographic apostrophes; both platforms use straight ones — genuine but cosmetic

The eyebrow/CTA path applies `.uppercased()` (iOS `BusinessAppStateKit.swift:205,327`; Android `BusinessAppStateKit.kt:221,339`), which leaves `’` and `'` distinguishable, so these do reach the screen differently.

| Figma string | Node | iOS | Android |
|---|---|---|---|
| `YOU’LL SET UP` | 2666:32080 | `YOU'LL SET UP` (`:378`) | `YOU'LL SET UP` (`:407`) |
| `You’re offline · showing last known` | 2666:32221 | `You're offline · …` (`:106`) | `You're offline · …` (`:433`) |
| `The moment is safe — we just couldn’t fetch its details right now.` | 2666:30417 | `couldn't` (`:226`) | `couldn't` (`:244`) |
| `Capture forms couldn’t load. Nothing you entered was lost.` | 2666:31869 | `couldn't` (`:337`) | `couldn't` (`:368`) |
| `Last-known Pulse is shown read-only. Captures queue on this device and sync when you’re back.` | 2666:32229 | `you're` (`:390`) | `you're` (`:422`) |

**Genuine, low impact.** The repo is inconsistent here already — 14 of 404 Swift files and 18 of 495 Kotlin files use `’` — so this is a house-style decision, not a bug. Five strings, one root cause.

### Confirmed false positives (do not "fix")

| Figma string | Why it is not drift |
|---|---|
| `RETRY`, `RECORD FIRST SALE`, `ADD FIRST ORDER`, `OPEN QUICK ADDS`, `PICK ANOTHER MONTH`, `CREATE BUSINESS` | Figma ships the pre-uppercased label. `AppStatePrimaryButton` applies `label.uppercased()` / `label.uppercase()` (iOS `BusinessAppStateKit.swift:327`, Android `BusinessAppStateKit.kt:339`). The specs carry `Retry`, `Record first sale`, … and render correctly. |
| `OCTOBER 2026`, `TODAY · 31 OCT` (eyebrows) | Derived at runtime from `monthLabel` / `todayLabel`, which are pinned to Figma casing by `BusinessAppStateSpecsTests.monthLabelMatchesFigmaCasing` and `todayLabelMatchesFigmaCasing`. |
| `Due soon`, `Needs Attention`, `All`, `Open`, `Closed` (ViewAll) | Live in `BusinessAppStateContent.swift:299,317` and `BusinessAppStateContent.kt:391,431` — i.e. the view layer, not the spec file the extractor diffed against. |
| `✕`, `✓`, `!`, `+`, `—` (badge glyphs) | Decorative. `—` is a real value in `Last synced` and is present on both platforms. |
| `Sale`, `Purchase`, `Collection`, `Payment`, `Expense`, `Stock` (Hub tiles) | `BusinessAppStateSpecs.hubTileLabels(vertical:)` returns exactly this set for every vertical except `.services` and `.marketing` (Figma "Hub label overrides"), which are not part of this component set. |
| `No business yet` / `Create one to begin` | Header-chrome `Selector Trigger` (2666:32141/32142), not state-body copy. Correctly excluded by the extractor. |
| `Nothing was lost. Values stay hidden until they reload — never shown as 0.` | Present verbatim on both platforms (`BusinessAppStateSpecs.swift:103`, `.kt:119`). |
| `No open items under “Open”. …` | Android writes it with `\u201C` / `\u201D` escapes (`BusinessAppStateSpecs.kt:392`) rather than literal curly quotes, so a byte-level diff flags it. The rendered glyph is identical. |

### Not copy drift, but noticed

- **NoCompany hero glyph.** Figma's badge is `+` (2666:32076); iOS uses `icon: .plus` (`:370`) but Android uses `AppStateIcon.Sparkle` (`:403`), and its comment claims *"Figma NoCompany Setup: sparkle in the hero core"* — the export says otherwise.
- **Hub loading.** Android renders `Loading Quick adds…` (`AppShellScreen.kt:1645`); Figma has no Hub loading variant to compare against.
- **MomentSwitcher (out of scope for the count above).** `State=LoadError` (2678:37790) uses `Couldn’t refresh` / `Couldn’t load moments` (text nodes 2678:37727 / 2678:37753); both platforms use `Couldn't` (`MomentSwitcherSpecs.swift:123,199`). Figma's `Last 30 days` chip (text 2678:37668) has no counterpart anywhere in `MomentSwitcherDirectoryView.swift` or the Android package — a real gap, not a casing quirk. `1 member · Trading` (2678:37443) is *not* drift: it is built at runtime by the `memberLabel` closure (`MomentSwitcherSpecs.swift:189-191`).

## How to verify in Figma

Every link opens the variant directly. Hyphens in `node-id` are the URL form of the colon ids above.

| Variant | Link |
|---|---|
| Component set `AppState` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-32397 |
| `Screen=Pulse, State=Loading` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-29856 |
| `Screen=Memory, State=Empty` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-31303 |
| `Screen=Hub, State=Empty` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-31852 |
| `Screen=Hub, State=Error` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-32028 |
| `Screen=Moments, State=Error` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-30576 |
| `Screen=NoCompany, State=Setup` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-32211 |
| `Screen=Offline, State=Error` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-32395 |
| `Screen=ViewAll, State=Empty` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2666-32064 |
| Component set `MomentSwitcher` | https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=2678-37791 |

Check a specific string by selecting the text layer in Figma and comparing its `characters` value, or by re-running the extractor — it prints the exact codepoints, so `’` vs `'` is visible without eyeballing glyphs:

```bash
python3 scripts/figma/dump-appstate-variants.py --json \
  | python3 -c "import json,sys; [print(v['name'], '|', s) for v in json.load(sys.stdin)['variants'] for s in v['strings'] if any(ord(c) > 127 for c in s)]"
```
