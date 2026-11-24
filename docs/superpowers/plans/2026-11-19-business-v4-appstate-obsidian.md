# Business V4 AppState (Obsidian Emerald) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Business empty / loading / error / offline / NoCompany / Hub / ViewAll app-state bodies from Figma Obsidian Emerald (`rknf` `2:67`) on Android + iOS, and wire them into AppShell in place of `BusinessV4Placeholder` and generic `EmptyPanel` for BUSINESS.

**Architecture:** Spec-driven kit (`BusinessAppStateSpecs` + shared atoms + screen bodies) under restored `business/appstate` packages. Bodies render under the **already-unified** Personal/Group/Business shell chrome (do not re-fork topbar or bottom nav). Accent is Emerald by default with vertical overrides (Trading emerald, Retail rose, RST flame, ViewAll cyan) via `BusinessAppStateAccent`.

**Tech Stack:** Jetpack Compose (Kotlin), SwiftUI (Swift), shared shell `ShellContentState` routing.

**Spec:** Figma [Business v3 · International Obsidian · Emerald Theme](https://www.figma.com/design/rknfN9EikSCdqmqXogR29o/momentra?node-id=2-67&m=dev) · file `rknfN9EikSCdqmqXogR29o` · section `2:67`. Companion history: [`docs/design/FIGMA_APPSTATE_DRIFT.md`](../../design/FIGMA_APPSTATE_DRIFT.md) (TzLvw kit; copy rules still apply), [`docs/superpowers/plans/2026-11-19-business-v4-contract-freeze.md`](2026-11-19-business-v4-contract-freeze.md).

## Global Constraints

- Figma SoT file key: `rknfN9EikSCdqmqXogR29o`; section node: `2:67`.
- Canvas: `#05070B`. Emerald accent: `#10B981`; gradient CTA: `#059669` → `#34D399`. Amber warning values: `#F5A624`. Error coral: `#FF5C38`. Muted body: `#94A4B8`.
- Missing figures never render as `0` — use `Not recorded` / `None yet` / `Quiet` / `Hidden until online` / omit row.
- AppState owns **content body only**; keep unified shell topbar / moment switcher / bottom nav from Personal·Group·Business chrome work.
- Primary CTA labels in specs are title case (`Retry`, `Record first sale`); buttons apply `.uppercase()` at render (matches Figma `RETRY`).
- Body copy uses typographic apostrophe `’` where Figma does.
- Platforms: Android + iOS parity for every shipped screen/phase in this plan.

## Review Focus

1. **BUSINESS Offline/Error still hit generic EmptyPanel** — expect Figma Offline/Error bodies with Retry, not `"You're offline"` shell panel.
2. **Missing figure shown as `0`** — expect `Not recorded` / amber tone, never numeric zero.
3. **Loading title bypass** — expect `Loading Pulse…` (ellipsis U+2026) from specs, not `Pulse · loading`.
4. **Hub Empty invents CTAs** — expect Figma Hub empty (tiles + footnote) without invented `Record first capture` unless the Obsidian Hub frame includes it (`2:613`).
5. **Chrome fork regression** — expect AppState body under shared chrome; no Business-only bottom-bar color fork reintroduced.

## File map

| Responsibility | Android | iOS |
|---|---|---|
| Tokens / accent | `apk/.../business/appstate/BusinessAppStateTokens.kt` | `momentra/.../Shell/BusinessAppState/BusinessAppStateTokens.swift` |
| Specs / copy | `.../BusinessAppStateSpecs.kt` | `.../BusinessAppStateSpecs.swift` |
| Kit atoms (hero, glass card, buttons, banner, skeleton) | `.../BusinessAppStateKit.kt` | `.../BusinessAppStateKit.swift` |
| Bodies | `.../BusinessAppStateContent.kt` | `.../BusinessAppStateContent.swift` |
| Shell wire | `AppShellScreen.kt` `ShellDestinationContent` | `AppShellView.swift` `destinationContent` |
| Empty mount | `empty/ContextEmptyExperience.kt` | `MomentEmptyStateView.swift` `businessEmpty` |
| Tests | `apk/.../test/.../business/appstate/BusinessAppStateSpecsTest.kt` | `momentraTests/BusinessAppStateSpecsTests.swift` |
| Assets | `apk/.../res/` + Compose vectors as needed | `Assets.xcassets/BusinessAppState/` after `download_assets` |

Key Figma frames (implement these first):

| Frame | Node |
|---|---|
| Pulse Empty (Trading) | `2:221` |
| Moments Empty (Distribution) | `2:319` |
| Life Empty (Services) | `2:417` |
| Memory Empty (Manufacturing) | `2:515` |
| Hub Quick Adds (Marketing) | `2:613` |
| NoCompany Setup (Retail) | `2:721` |
| Pulse Loading Skeleton | `2:822` |
| Pulse Error | `2:907` |
| Offline | `2:996` |
| Retail Empty (Rose) | `2:1093` |
| RST Food Empty (Flame) | `2:1190` |
| View All (Cyan) | `2:1287` |
| Toaster & Banner system | `2:1375` |

---

### Task 1: Tokens + enums (Android + iOS)

**Files:**
- Create: `apk/app/src/main/java/com/example/momentra/ui/shell/business/appstate/BusinessAppStateTokens.kt`
- Create: `momentra/momentra/Shell/BusinessAppState/BusinessAppStateTokens.swift`
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/appstate/BusinessAppStateSpecsTest.kt` (token constants section)
- Test: `momentra/momentraTests/BusinessAppStateSpecsTests.swift`

**Interfaces:**
- Produces: `BusinessAppStateScreen`, `BusinessAppStatePhase`, `BusinessAppStateAccent` (Emerald `#10B981`, Rose, Flame, Cyan), `ValueTone`, canvas/glass/CTA color constants matching Figma.

- [ ] **Step 1: Write failing tests asserting canvas `#05070B` and Emerald `#10B981`**

```kotlin
@Test fun canvasIsObsidian() {
    assertEquals(0xFF05070B, BusinessAppStateTokens.Canvas.value.toLong() and 0xFFFFFFFF)
}
@Test fun emeraldAccent() {
    assertEquals(0xFF10B981, BusinessAppStateAccent.Emerald.color.value.toLong() and 0xFFFFFFFF)
}
```

- [ ] **Step 2: Run tests — expect FAIL (types missing)**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests '*.BusinessAppStateSpecsTest' -q`

- [ ] **Step 3: Implement tokens/enums on Android + mirrored Swift**

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/business/appstate/BusinessAppStateTokens.kt \
  momentra/momentra/Shell/BusinessAppState/BusinessAppStateTokens.swift \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/appstate/BusinessAppStateSpecsTest.kt \
  momentra/momentraTests/BusinessAppStateSpecsTests.swift
git commit -m "feat(business): add Obsidian AppState tokens and accents"
```

---

### Task 2: Specs copy matrix (Pulse/Moments/Life/Memory/Hub/NoCompany/Offline × Empty/Loading/Error/Setup)

**Files:**
- Create: `.../BusinessAppStateSpecs.kt` / `.swift`
- Modify: tests above

**Interfaces:**
- Consumes: Task 1 enums/tokens
- Produces: `AppStateSpec`, `AppStateRow`, `fun spec(screen, phase, accent, now): AppStateSpec`, `hubTileLabels(vertical)`

Pinned Pulse Empty copy from `2:221`:
- title: `No pulse yet`
- body: `Nothing has been recorded for your business this period. Pulse fills in as sales, collections and stock land.`
- rows: `Sales & collections` → `Not recorded` (Warning); `Needs attention` → `Nothing open`; `GST position` → `Not recorded` (Warning)
- primary: `Record first sale`; secondary: `Open Quick adds`

Pinned Pulse Error (`2:907`):
- code chip: `PULSE_FETCH_FAILED`
- title: `Couldn't load Pulse` (typographic apostrophe in source → render as Figma)
- body: `We couldn't reach your business data. Check your connection and try again.`
- footnote: `Nothing was lost. Values stay hidden until they reload — never shown as 0.`
- primary: `Retry`; secondary: `Open Quick adds`

Pinned Offline (`2:996`):
- banner: `You're offline · showing last known`
- title: `Waiting for connection`
- body: `Last-known Pulse is shown read-only. Captures queue on this device and sync when you're back.`
- rows under `LAST KNOWN`: `Last synced` → `—`; `Queued captures` → `None`; `Live figures` → `Hidden until online` (Warning)
- primary: `Retry`

- [ ] **Step 1: Write failing tests for Pulse Empty / Error / Offline exact strings + no-zero rule**

```kotlin
@Test fun pulseEmptyCopyMatchesFigma() {
    val s = BusinessAppStateSpecs.spec(Pulse, Empty)
    assertEquals("No pulse yet", s.title)
    assertTrue(s.rows.none { it.value == "0" })
}
@Test fun pulseErrorCodeChip() {
    assertEquals("PULSE_FETCH_FAILED", BusinessAppStateSpecs.spec(Pulse, Error).errorCode)
}
```

- [ ] **Step 2: Run tests — expect FAIL**

- [ ] **Step 3: Implement `BusinessAppStateSpecs` on both platforms; pull remaining Empty titles/bodies from nodes `2:319`, `2:417`, `2:515`, `2:613`, `2:721` via `get_design_context` during implementation**

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(business): Obsidian AppState specs for empty error offline"
```

---

### Task 3: Kit atoms (hero badge, glass card, CTAs, offline banner, skeleton)

**Files:**
- Create: `BusinessAppStateKit.kt` / `.swift`
- Download hero ring / glow SVGs from Figma into project assets (no temporary MCP URLs at runtime)

**Interfaces:**
- Consumes: tokens, `AppStateSpec`
- Produces: `AppStateHeroBadge`, `AppStateGlassCard`, `AppStatePrimaryButton`, `AppStateSecondaryButton`, `AppStateOfflineBanner`, `AppStateErrorCodeChip`, `AppStateLoadingSkeleton`

- [ ] **Step 1: Write a Compose/SwiftUI screenshot-free unit/layout smoke that PrimaryButton uppercases `Retry` → `RETRY`**

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement atoms; download assets with Figma `download_assets` for glow/hero rings used by hero badge**

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(business): Obsidian AppState kit atoms"
```

---

### Task 4: Screen bodies

**Files:**
- Create: `BusinessAppStateContent.kt` / `.swift`
- Produces: `BusinessAppStateBody(spec, accent, onPrimary, onSecondary)`, `BusinessAppStateLoadingBody`, `BusinessAppStateHubBody`, `BusinessAppStateViewAllSheet` (from `2:1287`)

- [ ] **Step 1: Failing test — Hub Empty does not invent primary CTA unless present in `2:613` footnote-only layout**

- [ ] **Step 2: Implement bodies composing Task 3 atoms from `AppStateSpec`**

- [ ] **Step 3: Compile Android `./gradlew :app:compileDebugKotlin`; iOS build or rg import check**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(business): Obsidian AppState screen bodies"
```

---

### Task 5: Wire AppShell BUSINESS Loading / Empty / Error / Offline / Ready-empty

**Files:**
- Modify: `apk/.../AppShellScreen.kt` (`ShellDestinationContent`)
- Modify: `apk/.../empty/ContextEmptyExperience.kt`
- Modify: `momentra/.../Shell/AppShellView.swift`
- Modify: `momentra/.../Shell/MomentEmptyStateView.swift`
- Remove or stop using: `BusinessV4Placeholder` for these states (keep stub only if CREATE still needs a temporary body)

**Interfaces:**
- Consumes: `BusinessAppStateBody` / Loading / Hub
- Maps: `ShellContentState.Loading|Idle` → Loading body; `Empty` → Empty/NoCompany; `Error|Forbidden` → Error; `Offline` → Offline; CREATE empty Hub → Hub Empty

- [ ] **Step 1: Write failing instrumentation or unit mapping test: `destination + ShellContentState.Offline + BUSINESS` resolves Offline spec title `Waiting for connection`**

- [ ] **Step 2: Replace BUSINESS branches that currently show `BusinessV4Placeholder` / generic `EmptyPanel` with AppState bodies; wire Retry → `onRetry` / `reloadCurrentContext`; Open Quick adds → `selectBottomDestination(CREATE)`**

- [ ] **Step 3: Android compile + iOS import check; manual smoke Personal/Group unchanged**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(business): wire Obsidian AppState into AppShell"
```

---

### Task 6: Vertical accent variants + ViewAll + toaster/banner stubs

**Files:**
- Modify: `BusinessAppStateAccent` mapping from `selectedMomentTypeCode`
- Modify: bodies to tint hero/CTA from accent (Rose `2:1093`, Flame `2:1190`, Cyan `2:1287`)
- Create minimal toast/banner components from `2:1375` (success / offline queue / error code) mounted on existing `OperatingToastHost` placeholder or shell snackbar path

**Interfaces:**
- Produces: `BusinessAppStateAccent.forTypeCode(code: String?): BusinessAppStateAccent`
- Produces: `BusinessAppStateToast` / banner composables (success, offline queue, error) — visual only if toast host not yet rebuilt

- [ ] **Step 1: Test `forTypeCode("RETAIL_STORE")` → Rose; `"RESTAURANT_FOOD"` → Flame; default → Emerald**

- [ ] **Step 2: Implement mapping + ViewAll sheet empty from `2:1287`**

- [ ] **Step 3: Implement toaster/banner atoms from `2:1375` copy (`₹12,450 added…`, `Offline queue active (2 items)`, `Error CODE: SYNC_TIMEOUT_504 · Tap to retry`)**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(business): AppState vertical accents ViewAll and banners"
```

---

### Task 7: Gallery + parity checklist

**Files:**
- Create (debug): `apk/.../debug/.../AppStateGalleryActivity.kt` (optional, debug source set)
- Update: `docs/design/FIGMA_APPSTATE_DRIFT.md` header to note rknf `2:67` as V4 SoT (short addendum, not full rewrite)

- [ ] **Step 1: Smoke checklist doc under `docs/qa/BUSINESS_V4_APPSTATE_SMOKE.md` listing 12 frames × Android/iOS**

- [ ] **Step 2: Run unit tests both platforms; Android compile**

- [ ] **Step 3: Commit**

```bash
git commit -m "docs(qa): Business V4 Obsidian AppState smoke checklist"
```

---

## Self-review

1. **Spec coverage:** Empty (6) + Loading + Error + Offline + ViewAll + NoCompany + banners covered by Tasks 2–6; chrome intentionally out of scope.
2. **Steps:** Each step is one action with a checkable result; copy pinned from Figma nodes.
3. **Types:** `BusinessAppStateScreen` / `Phase` / `AppStateSpec` consistent across tasks.
4. **Review Focus:** Each failure mode has a test in Tasks 2, 4, or 5.
5. **Proportion:** Plan decides files, names, copy, and wiring — not full Compose/SwiftUI bodies.
