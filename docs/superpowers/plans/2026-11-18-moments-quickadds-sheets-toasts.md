# Moments QuickAdds / Sheets / Toasts Wiring Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the audited missing/broken Personal and Business QuickAdd hubs, sheets, and success toasts so CREATE/Save paths actually open and confirm — without redesigning working operating hubs.

**Architecture:** One sequenced repair program ordered by audit priority. First introduce a **shared operating toast host** mounted at `OperatingMomentPage` / `OperatingExperienceHost` (all kits), then mount Personal QuickAdd hub tiles onto CREATE (calling existing `openLifeOpsQa` / `openMoneyQa`), then mount `BusinessGapQuickAddSheet`, then scope/clear `OperatingChromeState` on tab/moment change, then honesty fixes (Marketing Hold/Transfer, Restaurant Reprint, Transfer TO / Savings stubs). Prefer wiring existing sheets over inventing APIs.

**Tech Stack:** Kotlin/Compose (`apk/`), SwiftUI (`momentra/`), JUnit (`apk/app/src/test`), Swift XCTest (`momentra/momentraTests`), TypeScript backend catalog (`backend/typescript/src/modules/business/operating-catalog.ts`) read-only for MFG assertions.

**Spec:** Audit findings in this plan’s **Audit Findings (source of truth)** section (user audit of Business Moments, Pulse, Moments, Life, Memory, QuickAdds, Hub, bottom sheets, toasters vs docs + Figma, 2026-11-18). Supporting contracts (QA names/copy only): Manufacturing MTO v3.1, Marketing Campaigns v5.1, Retail Food Consumer v5, Trading Distribution v11.1 (paths under `/home/santosh/Desktop` / `/home/santosh/Downloads` as named in the audit request).

## Global Constraints

- Plan and implement **missing/broken wiring only** — do not redesign working operating hubs (Retail/Restaurant/Trading/Dist/MFG/MTO/Marketing cores that already open captures).
- Prefer a **shared operating toast host** at Operating experience host / page; do not leave Retail/Restaurant calling `MtoToastHost` with overlay only on MTO.
- Personal CREATE: mount real QuickAdd hub; tiles → `openLifeOpsQa` / `openMoneyQa`; **single expense entry = Master Expense** (`PersonalMasterExpenseSheet` via `MoneyQuickAddKind.MASTER_EXPENSE`), not ExperienceHost local expense on CREATE.
- Business GAP: wire hub → mount `BusinessGapQuickAddSheet` (do not delete the API); Wedding collab body reuse is honesty/document or neutralize ownership — do not invent a new collab subsystem this sprint.
- Chrome state: **clear sheet-open / hint / browser selection on tab or moment change** (smaller correct fix); do not rewrite all consumers to Context locals unless clear-on-change proves insufficient.
- Marketing HOLD/TRANSFER/REPRINT: intentional non-post — make UX honest, **do not invent write APIs**.
- Retail POS local hold / missing APIs: honesty/docs only, not full POS backend.
- Future / Lifestyle / Relationships stub families: hide tiles or keep stubs; **do not invent sheets**.
- Figma node ID remaps: note only; do not remake designs.
- DRY / YAGNI / TDD / frequent commits: one commit per task; reuse existing toast/copy symbols (`MtoToastSpec`, `RetailCaptureCopy.SAVED_TOAST`, `RestaurantCaptureCopy.SAVED_TOAST`, `MfgSavedToast`, `RoseSavedToast`, `EmberSavedToast`, `habitRewardMessage`).
- Android tests: `./gradlew :app:testDebugUnitTest --tests <FQCN>` from `apk/`. iOS: XCTest where harness exists; otherwise exact manual checklists as the test step.
- Platforms: Android + iOS parity for every P0/P1 user-visible fix unless a task explicitly marks Android-only or iOS-only.

## Review Focus

1. **CREATE with no Life Ops moment:** Tapping Spend/Mood must open setup (`PersonalSetupSystem.LIFE_OPERATIONS`) via existing `openLifeOpsQa`/`openMoneyQa`, not crash or no-op — covered in Task 4.
2. **Operating toast after leaving CREATE:** Saving on Retail CREATE then switching to Pulse must still show (or safely dismiss) toast without leaking MTO-only overlay — covered in Tasks 1–2.
3. **Chrome leak across tabs:** Opening a capture hint on CREATE then switching to Memory must not auto-open that sheet on return — covered in Task 10.
4. **Restaurant Reprint must not post:** CTA dismisses only; activity list gains no new `UPDATE` row — covered in Task 17.
5. **Savings stub goalId:** Never send non-UUID stub ids (`"house"`); either create a real goal then pass UUID, or keep `goalId=null` with honest “local label” copy — covered in Task 11.

---

## Audit Findings (source of truth)

Audit date: **2026-11-18**. Method: call-site tracing of QuickAdd hubs, sheet mounts, toast hosts vs docs/Figma. Repair only what is missing/broken.

### P0 / Critical

1. **Personal LifeOps/Money QuickAdds unreachable** — CREATE is expense-only `PersonalExperienceHost` (`QuickAddsHub`); iOS `PersonalQuickAddHubView` never mounted; Android has no hub equivalent; FAB gated `if (false && …)`; `openLifeOpsQa`/`openMoneyQa` exist in shell but CREATE content never calls them (`onLifeOpsQuickAdd`/`onMoneyQuickAdd` orphaned on `ShellDestinationContent`).
2. **Android Business GAP QuickAdd never mounted** — `BusinessGapQuickAddSheet` defined in `BusinessQuickAddSheets.kt`; `businessGapQa` is set; sheet never composed (only EXPENSE/SPEND short-circuit in `LaunchedEffect`).
3. **Retail/Restaurant success toasts orphaned** — call `MtoToastHost.show` but `MtoToastOverlay` only on MTO hub (`MadeToOrderQuickAdds.kt`) / iOS `kit == .madeToOrder`. Silent saves.

### P1 / High

4. Trading/Distribution no success toast after save (`sheet = null` only).
5. `OperatingChromeState` process-global — sheets/hints leak across Pulse/Moments/Life/Memory/CREATE.
6. Personal Transfer TO unused (`toId` never sent); Savings `goalId` always null; stub goals (`house`/`travel`/`education`).
7. Dual Personal expense paths — ExperienceHost local expense vs unreachable Master Expense.
8. Business `REVENUE`/`INVOICE` QuickAdd kinds no-op (`Unit`); finance sheets side-door only.
9. Business GAP sheets reuse Wedding collab bodies (`WeddingPollSheetBody`, etc.).
10. Android MFG catalog vs backend `operating-catalog.ts` — **verify** RETURN/ORDER (hub already lists them in `MfgHubCatalog`; assert, fix only if regression).

### P2 / Medium

11. Habit/toast gaps on Personal shell saves; ExperienceHost uses Saved screen.
12. Orphaned `ShellDestinationContent` params (`onLifeOpsQuickAdd`/`onMoneyQuickAdd`) — wire or delete.
13. Marketing Own Hold/Transfer hub tiles hard-`unavailable = true` vs chrome chip paths that open chrome-only sheets.
14. Restaurant Reprint posts `UPDATE` (should dismiss-only like Trading REPRINT at `OperatingQuickSheet` ~3923).
15. MFG Moments widget labels diverge from contract — **YAGNI** unless a one-line fix.
16. iOS Hub-as-sheet vs Android CREATE tab — **document known divergence**; fix only if required for toast/deep-link parity.

### Out of scope (honesty-only)

- Marketing HOLD/TRANSFER/REPRINT intentional non-post — UX honesty, no new APIs.
- Retail POS local hold / missing APIs — honesty/docs.
- Future/Lifestyle/Relationships stub families — hide or keep stubs; no new sheets.
- Figma node ID remaps — note only.

---

## File map (decomposition)

| Unit | Responsibility |
|------|----------------|
| `apk/.../business/shared/OperatingToast.kt` (new) + iOS `OperatingToast.swift` (new or rename wrapper) | Shared toast host + overlay for all operating kits; keep `MtoToastHost` as typealias/delegate during migration |
| `OperatingMomentScreens.kt` / `OperatingExperienceHost.swift` | Mount shared overlay for every kit; clear chrome on tab/moment |
| `OperatingChromeState.kt` (+ iOS equivalent if any) | `clearTransientUi()` for hints/sheets |
| `PersonalQuickAddHub.kt` (new) + existing `PersonalQuickAddHubView.swift` | CREATE hub tiles → LifeOps/Money callbacks |
| `PersonalExperienceHost.kt` / `.swift` | CREATE uses hub, not expense-only; drop dual expense on CREATE |
| `AppShellScreen.kt` / `AppShellView.swift` | Pass callbacks; mount `BusinessGapQuickAddSheet`; keep sheet hosts |
| `BusinessQuickAddSheets.kt` | GAP mount target; REVENUE/INVOICE redirect bodies already exist |
| `TradingQuickAdds.kt`, `DistributionQuickAdds.kt` | Call shared toast on save success |
| `RetailQuickAdds.kt`, `RestaurantCaptureSheets.kt`, `RestaurantUtilitySheets.kt` | Toast + Reprint dismiss-only |
| `MarketingQuickAdds.kt` | Own Hold/Transfer hub honesty |
| `PersonalMoneyQuickAddSheets.kt` (+ iOS) | Transfer TO honesty; Savings goalId |

---

### Task 1: Shared operating toast host (Android)

**Files:**
- Create: `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingToast.kt`
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/mto/MtoToast.kt` (delegate `MtoToastHost` → shared host; keep `MtoToastSpec` / `MtoToastKind` / `MtoToastOverlay` as thin aliases or move types into `OperatingToast.kt` and re-export)
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingToastHostTest.kt`

**Interfaces:**
- Consumes: existing `MtoToastSpec(kind, title, body, actionLabel, accent, action)`
- Produces: `object OperatingToastHost { var current: MtoToastSpec?; var onAction: ((MtoToastAction) -> Unit)?; fun show(spec: MtoToastSpec); fun show(kind: MtoToastKind, entity: String = "this job", businessLabel: String = "Make to order"); fun dismiss() }` and `@Composable fun OperatingToastOverlay(modifier: Modifier = Modifier)`

- [x] **Step 1: Write the failing test**

```kotlin
class OperatingToastHostTest {
    @Test
    fun show_setsCurrentSpec() {
        OperatingToastHost.dismiss()
        OperatingToastHost.show(
            MtoToastSpec(
                kind = MtoToastKind.Saved,
                title = "✓ Saved",
                body = "test",
                actionLabel = "Done",
                accent = Color(0xFF10B981),
            ),
        )
        assertEquals("✓ Saved", OperatingToastHost.current?.title)
        OperatingToastHost.dismiss()
        assertNull(OperatingToastHost.current)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.OperatingToastHostTest.show_setsCurrentSpec`
Expected: FAIL (class/symbol not found) or FAIL assert if stub empty

- [x] **Step 3: Implement `OperatingToastHost` + `OperatingToastOverlay` in `OperatingToast.kt`; make `MtoToastHost.show`/`dismiss`/`current`/`onAction` forward to `OperatingToastHost`**

Move or share the overlay UI from `MtoToast.kt` so one composition path exists. Keep package imports for Retail/Restaurant (`MtoToastHost`) compiling via delegation.

- [x] **Step 4: Run test to verify it passes**

Run: same command as Step 2  
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingToast.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/mto/MtoToast.kt \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingToastHostTest.kt
git commit -m "$(cat <<'EOF'
feat(operating): add shared OperatingToastHost with MtoToastHost delegate

EOF
)"
```

---

### Task 2: Mount shared toast overlay for all operating kits (Android + iOS)

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingMomentScreens.kt` (`OperatingMomentPage`)
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/mto/MadeToOrderQuickAdds.kt` (remove duplicate `MtoToastOverlay()` if page-level mounts it — keep one overlay)
- Modify: `momentra/momentra/Shell/BusinessActive/OperatingExperienceHost.swift` (today: `if kit == .madeToOrder { MtoToastOverlay() }`)
- Create or modify: `momentra/momentra/Shell/BusinessActive/OperatingToast.swift` (or wrap `MtoToast.swift` the same way as Android)
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingToastMountContractTest.kt` (string/source contract: `OperatingMomentPage` contains `OperatingToastOverlay` or `MtoToastOverlay` once; MTO hub does not double-mount — or manual checklist below if source-contract tests are too brittle)

**Interfaces:**
- Consumes: `OperatingToastHost` / `OperatingToastOverlay` from Task 1; iOS `MtoToastHost.shared` / `MtoToastOverlay`
- Produces: Overlay visible for Retail, Restaurant, Trading, Distribution, MFG, MTO, Marketing, Service when any kit’s CREATE host is shown

- [x] **Step 1: Write the failing test (Android source contract)**

```kotlin
class OperatingToastMountContractTest {
    @Test
    fun operatingMomentPage_mentionsToastOverlay() {
        val src = File("../main/java/com/example/momentra/ui/shell/business/shared/OperatingMomentScreens.kt").readText()
        assertTrue(src.contains("OperatingToastOverlay") || src.contains("MtoToastOverlay"))
    }
}
```

Place test under `apk/app/src/test/...` and resolve path relative to test resources, **or** use classpath `ClassLoader` reading from known module path. If pathing is awkward, use the manual checklist as the failing “test” before implement.

- [x] **Step 2: Run test / checklist to verify it fails**

Run unit test **or** manual: open Retail CREATE → save a spend → confirm no toast today (baseline fail).

- [x] **Step 3: Mount overlay once in `OperatingMomentPage` Box (Android) and in `OperatingExperienceHost` for **all** kits (iOS: remove `kit == .madeToOrder` guard). Dedupe MTO hub overlay.**

Wire `OperatingToastHost.onAction` / `MtoToastHost.shared.onAction` at the same host level (Pulse navigation) for all kits, not only MTO.

- [x] **Step 4: Verify pass**

Manual checklist (both platforms):
1. Retail CREATE → save → toast title equals `RetailCaptureCopy.SAVED_TOAST` (or “✓ Saved…” copy already used).
2. Restaurant CREATE → save → toast visible (`RestaurantCaptureCopy.SAVED_TOAST` / Ember path).
3. MTO CREATE → save → still one toast (no double).

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingMomentScreens.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/mto/MadeToOrderQuickAdds.kt \
  momentra/momentra/Shell/BusinessActive/OperatingExperienceHost.swift \
  momentra/momentra/Shell/BusinessActive/OperatingToast.swift \
  momentra/momentra/Shell/BusinessActive/MtoToast.swift
git commit -m "$(cat <<'EOF'
fix(operating): mount shared toast overlay for all kits

EOF
)"
```

---

### Task 3: Point Retail/Restaurant toast calls through shared host (no silent path)

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/retail/RetailQuickAdds.kt` (`showSavedToast` / `showPermissionToast`)
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/retail/RetailCaptureSheets.kt`, `RetailUtilitySheets.kt` (any `MtoToastHost.show`)
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/restaurant/RestaurantCaptureSheets.kt` (`captureSavedToast` / `MtoToastHost.show`)
- iOS equivalents under `momentra/momentra/Shell/BusinessActive/Retail/` and `Restaurant/` that call `MtoToastHost.shared.show`
- Test: reuse `OperatingToastHostTest` + manual checklist from Task 2

**Interfaces:**
- Consumes: `OperatingToastHost.show` (or delegated `MtoToastHost.show` after Task 1)
- Produces: unchanged call sites that now render because overlay is mounted

- [x] **Step 1: Write failing assertion**

```kotlin
@Test
fun retailSavedToast_usesSharedHost() {
    OperatingToastHost.dismiss()
    // simulate RetailQuickAdds.showSavedToast body
    OperatingToastHost.show(
        MtoToastSpec(
            kind = MtoToastKind.Saved,
            title = "✓ Saved · sheet closes and returns back", // use exact RetailCaptureCopy.SAVED_TOAST constant
            body = "x",
            actionLabel = "Done",
            accent = Color(0xFF10B981),
        ),
    )
    assertNotNull(OperatingToastHost.current)
}
```

Use the real `RetailCaptureCopy.SAVED_TOAST` string constant in the test.

- [x] **Step 2: Run test — fail if host not wired**

- [x] **Step 3: Ensure Retail/Restaurant call `MtoToastHost.show` / `OperatingToastHost.show` (no local-only `RoseSavedToast`/`EmberSavedToast` that disappears when sheet closes unless that toast is also hosted at page level). Prefer shared host for capture-success paths that currently call `MtoToastHost`.**

- [x] **Step 4: Pass + manual Retail/Restaurant save on device/emulator**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(retail,restaurant): ensure capture success uses shared toast host

EOF
)"
```

---

### Task 4: Personal CREATE QuickAdd hub (Android) — mount + wire LifeOps/Money

**Files:**
- Create: `apk/app/src/main/java/com/example/momentra/ui/shell/personal/shared/PersonalQuickAddHub.kt`
- Create: `apk/app/src/main/java/com/example/momentra/ui/shell/personal/shared/PersonalActionRegistry.kt` (port Life Ops tile set from iOS `PersonalActionRegistry.swift` — Life Ops tiles only for this task: Spend, Mood, Recovery, Income, Attention, Transfer, Savings, Adjust; Future/Lifestyle/Relationships stay stubs/hidden per YAGNI)
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/personal/experience/PersonalExperienceHost.kt` — on `BottomDestination.CREATE`, compose hub instead of expense-only `QuickAddsHub`; add params `onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Unit`, `onMoneyQuickAdd: (MoneyQuickAddKind) -> Unit`
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt` — pass `onLifeOpsQuickAdd` / `onMoneyQuickAdd` into `PersonalExperienceHost` (from existing `openLifeOpsQa` / `openMoneyQa`)
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/personal/shared/PersonalActionRegistryTest.kt`

**Interfaces:**
- Consumes: `LifeOpsQuickAddKind` (`RECOVERY`, `MOOD`, `ATTENTION`, `ADJUST`), `MoneyQuickAddKind` (`MASTER_EXPENSE`, `INCOME`, `TRANSFER`, `SAVINGS`), `openLifeOpsQa`/`openMoneyQa` signatures in `AppShellScreen.kt`
- Produces: `@Composable fun PersonalQuickAddHub(hasActiveMoment: Boolean, momentTypeCode: String?, onSpend: () -> Unit, onIncome: () -> Unit, onRecovery: () -> Unit, onMood: () -> Unit, onAttention: () -> Unit, onAdjust: () -> Unit, onTransfer: () -> Unit, onSavings: () -> Unit, …)`  
  Tile → callback map (match iOS labels): Spend/Expense→`onSpend`→`MoneyQuickAddKind.MASTER_EXPENSE`; Income→`INCOME`; Transfer→`TRANSFER`; Savings→`SAVINGS`; Mood→`MOOD`; Recovery→`RECOVERY`; Attention→`ATTENTION`; Adjust→`ADJUST`

- [x] **Step 1: Write the failing test**

```kotlin
class PersonalActionRegistryTest {
    @Test
    fun lifeOpsCatalog_includesSpendMoodRecoveryIncomeTransferSavings() {
        val labels = PersonalActionRegistry.catalogTiles(PersonalPulseFamily.LIFE_OPERATIONS).map { it.label }
        assertTrue(labels.containsAll(listOf("Spend", "Mood", "Recovery", "Income", "Transfer", "Savings")))
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.personal.shared.PersonalActionRegistryTest`

- [x] **Step 3: Implement registry + hub; replace CREATE `QuickAddsHub` with `PersonalQuickAddHub` wired to shell openers; leave Future/Lifestyle/Relationships tiles non-tappable or omitted**

Decision (locked): **Expense on CREATE opens Master Expense only** (`onMoneyQuickAdd(MoneyQuickAddKind.MASTER_EXPENSE)`), not `openExpense()` local ExperienceHost sheet.

- [x] **Step 4: Run unit test PASS + manual CREATE checklist**

Manual:
1. Personal → CREATE → see Spend/Mood/Recovery/Income/Transfer/Savings tiles.
2. Spend → `PersonalMasterExpenseSheet` opens.
3. Mood → `PersonalLifeOpsQuickAddSheet(MOOD)` opens.
4. Transfer → `PersonalMoneyQuickAddSheet(TRANSFER)` opens.
5. No Life Ops moment → setup prompt (existing `personalSetupSystem`).

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(personal): mount QuickAdd hub on CREATE and wire LifeOps/Money sheets

EOF
)"
```

---

### Task 5: Personal CREATE QuickAdd hub (iOS) — mount existing `PersonalQuickAddHubView`

**Files:**
- Modify: `momentra/momentra/Shell/Personal/Experience/PersonalExperienceHost.swift` — CREATE branch mounts `PersonalQuickAddHubView` with callbacks
- Modify: `momentra/momentra/Shell/AppShellView.swift` — pass `openLifeOpsQa` / `openMoneyQa` into host (or bind hub sheets at shell the same as Android)
- Test: `momentra/momentraTests/PersonalActionRegistryTests.swift` (extend if needed) + manual checklist

**Interfaces:**
- Consumes: existing `PersonalQuickAddHubView` callbacks (`onSpend`, `onIncome`, `onRecovery`, `onMood`, `onAttention`, `onAdjust`, `onTransfer`, `onSavings`)
- Produces: CREATE no longer expense-only ExperienceHost flow for primary tiles

- [x] **Step 1: Write/extend failing test** — assert Life Ops catalog labels include Spend/Income/Transfer (existing `PersonalActionRegistryTests` if present; else manual fail baseline).

- [x] **Step 2: Confirm fail** — CREATE today does not show hub (baseline).

- [x] **Step 3: Mount `PersonalQuickAddHubView` on `.create`; map callbacks to `openMoneyQa` / `openLifeOpsQa`; stub families remain non-invented (disable or `onSetupMissing`).**

- [x] **Step 4: Manual checklist same as Task 4 (iOS Simulator)**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(ios/personal): mount PersonalQuickAddHubView on CREATE

EOF
)"
```

---

### Task 6: Single Personal expense path on CREATE + disable dead FAB

**Files:**
- Modify: `apk/.../personal/experience/PersonalExperienceHost.kt` — remove CREATE-only local `openExpense()` entry; area shortcuts on CREATE either open Master Expense with prefill **or** are removed from CREATE hub (YAGNI: prefer open Master Expense without ExperienceHost area flow)
- Modify: `apk/.../AppShellScreen.kt` — delete or keep `if (false && …) PersonalExpenseFab` **deleted** (dead code), not re-enabled (hub replaces FAB)
- Modify: iOS ExperienceHost CREATE similarly
- Test: manual + optional unit asserting CREATE host signature requires money callback

**Interfaces:**
- Consumes: Task 4/5 hub `onSpend` → `MASTER_EXPENSE`
- Produces: One expense sheet on CREATE: `PersonalMasterExpenseSheet`

- [x] **Step 1: Failing checklist** — CREATE still has “+ Add expense” ExperienceHub card (baseline fail after Task 4 if not removed).

- [x] **Step 2: Confirm fail**

- [x] **Step 3: Remove CREATE `QuickAddsHub` expense card path; remove `false &&` FAB block entirely from `AppShellScreen.kt`**

- [x] **Step 4: Manual — CREATE Spend opens Master Expense only; Pulse/Life may keep ExperienceHost browse (do not redesign)**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(personal): single Master Expense path on CREATE; remove dead FAB

EOF
)"
```

---

### Task 7: Mount Android `BusinessGapQuickAddSheet`

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt` — where `businessGapQa` is set (~809–830, ~1224–1233), compose:

```kotlin
BusinessGapQuickAddSheet(
    theme = /* BusinessActiveTheme for selected type */,
    kind = kind,
    visible = businessGapQa != null,
    momentId = state.selectedMomentId,
    onDismiss = { businessGapQa = null },
    onSaved = { businessGapQa = null; shellViewModel.refreshVisibleBusinessTab() },
    onExpense = { /* existing expense open */ },
    onRevenue = { businessRevenueSheetOpen = true },
    onInvoice = { businessInvoiceSheetOpen = true },
)
```

- Keep EXPENSE/SPEND short-circuit **or** let the sheet’s `RedirectLiveBody` handle it (prefer sheet redirect for one path).
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/BusinessGapQuickAddMountTest.kt` (source contract: `AppShellScreen.kt` contains `BusinessGapQuickAddSheet(`)

**Interfaces:**
- Consumes: `BusinessGapQuickAddSheet(theme, kind, visible, momentId, onDismiss, onSaved, onExpense, onRevenue, onInvoice)` from `BusinessQuickAddSheets.kt`
- Produces: Visible GAP sheet when `onBusinessQuickAdd` sets non-operating / runway / ops kinds

- [x] **Step 1: Write failing source-contract test** asserting `BusinessGapQuickAddSheet(` appears in `AppShellScreen.kt`

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Compose the sheet; clear `businessGapQa` on dismiss**

- [x] **Step 4: PASS + manual** — non-operating Business moment QuickAdd opens sheet (not silent)

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(business): mount BusinessGapQuickAddSheet when gap QA selected

EOF
)"
```

---

### Task 8: Business REVENUE / INVOICE QuickAdd kinds stop no-op

**Files:**
- Modify: `apk/.../AppShellScreen.kt` `onBusinessQuickAdd` branch — replace

```kotlin
kind == BusinessQuickAddKind.REVENUE || kind == BusinessQuickAddKind.INVOICE -> Unit
```

with `businessGapQa = kind` (sheet already has RedirectLiveBody → finance sheets) **or** directly `businessRevenueSheetOpen` / `businessInvoiceSheetOpen = true`

- Decision (locked): **set `businessGapQa = kind`** so redirect copy (“available from the finance sheet”) stays honest; Continue opens existing finance sheets.
- iOS: mirror if the same `Unit`/empty switch exists in `AppShellView.swift`
- Test: unit or source contract that REVENUE/INVOICE are not a bare `Unit` branch

**Interfaces:**
- Consumes: Task 7 sheet mount; `BusinessQuickAddKind.REVENUE` / `INVOICE`
- Produces: Tapping those kinds opens redirect sheet → finance sheet

- [x] **Step 1: Failing test/contract** — `onBusinessQuickAdd` must not contain `-> Unit` for REVENUE/INVOICE

- [x] **Step 2: Run — FAIL while no-op remains**

- [x] **Step 3: Wire to `businessGapQa = kind` (or direct finance open)**

- [x] **Step 4: Manual — REVENUE/INVOICE QuickAdd shows redirect or finance sheet**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(business): wire REVENUE and INVOICE QuickAdds to finance redirect

EOF
)"
```

---

### Task 9: Trading + Distribution success toasts

**Files:**
- Modify: `apk/.../business/trading/TradingQuickAdds.kt` — on `onRecord` success, call `OperatingToastHost.show(MtoToastKind.Saved, …)` (or vertical copy constant if present) before `sheet = null`
- Modify: `apk/.../business/distribution/DistributionQuickAdds.kt` — same
- Modify: iOS Trading/Distribution hubs if they also dismiss silently
- Test: extend `OperatingToastHostTest` with a helper `assertSavedToastShown` used after simulating success callback, **or** manual checklist

**Interfaces:**
- Consumes: `OperatingToastHost.show` from Task 1–2
- Produces: Visible toast after Trading/Dist capture save

- [x] **Step 1: Failing manual/unit** — Trading save shows no toast (baseline)

- [x] **Step 2: Confirm fail**

- [x] **Step 3: On success path call shared host; keep existing error Text**

- [x] **Step 4: Manual Trading + Distribution save → toast → sheet closes**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(trading,distribution): show shared success toast after capture save

EOF
)"
```

---

### Task 10: Scope `OperatingChromeState` — clear on tab/moment change

**Files:**
- Modify: `apk/.../business/shared/OperatingChromeState.kt` — add:

```kotlin
fun clearTransientUi() {
    editing = null
    settling = null
    charging = null
    quickAddHint = null
    previewStoryId = null
    storyConfigOpen = false
    mastersOpen = null
    partsBrowserOpen = null
    servicesBrowserOpen = null
    browserSelection = null
    captureOpen = false
    switchMomentOpen = false
    // keep momentId / record / onSwitchMoment / theme colors; period already cleared on momentId
}
```

- Modify: `apk/.../business/shared/OperatingMomentScreens.kt` — `LaunchedEffect(momentId, tab) { OperatingChromeState.clearTransientUi() }` (in addition to existing period clear on momentId)
- Modify: `apk/.../AppShellScreen.kt` if CREATE vs other tabs remount without changing momentId — call clear when `bottomDestination` changes while Business operating
- iOS: clear equivalent `@State` / shared chrome when destination changes in `OperatingExperienceHost`
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingChromeStateClearTest.kt`

**Interfaces:**
- Consumes: `OperatingChromeState` fields listed above
- Produces: `OperatingChromeState.clearTransientUi()`

- [x] **Step 1: Write failing test**

```kotlin
@Test
fun clearTransientUi_nullsHintsAndSheets() {
    OperatingChromeState.quickAddHint = "sale"
    OperatingChromeState.mastersOpen = "Catalogue"
    OperatingChromeState.browserSelection = /* minimal selection if constructible */
    OperatingChromeState.clearTransientUi()
    assertNull(OperatingChromeState.quickAddHint)
    assertNull(OperatingChromeState.mastersOpen)
}
```

- [x] **Step 2: Run — FAIL (method missing)**

- [x] **Step 3: Implement clear + call on momentId/tab change**

- [x] **Step 4: PASS + manual** — set hint via attention CTA, switch to Memory, return to CREATE → sheet does not auto-open

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(operating): clear chrome transient UI on moment and tab change

EOF
)"
```

---

### Task 11: Personal Transfer TO + Savings goalId (honesty within existing API)

**Files:**
- Modify: `apk/.../personal/lifeops/create/PersonalMoneyQuickAddSheets.kt`
- Modify: `momentra/.../PersonalMoneyQuickAddSheets.swift` (parity)
- Modify: `apk/.../data/repository/PersonalSliceRepository.kt` — add `suspend fun createGoal(momentId, title, …): Result<CreateGoalResultDto>` wrapping `ApiService.createGoal` if missing
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/personal/lifeops/create/PersonalMoneyMovementPayloadTest.kt` (pure function extracting description / goal wiring if you extract a small helper)

**Interfaces:**
- Consumes: `PersonalSliceRepository.createMovement(momentId, movementType, amount, currencyCode, accountId, goalId, description)`; `CreateGoalBody(title, description?, targetAt?, expectedVersion?)`
- Produces:
  - **Transfer:** require `toId != null && toId != fromId` to enable Save; pass `description` including `to=<accountName>`; API still has single `accountId` (from) — **do not invent `toAccountId`**
  - **Savings:** on save, `createGoal(title = goalName)` then `createMovement(..., goalId = result.goalId)` **or** if createGoal fails offline, keep `goalId=null` and description contains goal name (already) with UI caption “Goal link pending”

- [x] **Step 1: Write failing test for transfer description helper**

```kotlin
@Test
fun transferDescription_includesToAccount() {
    val desc = buildTransferDescription(note = "", transferType = "One-time", whenCode = "Now", toAccountName = "HDFC Savings")
    assertTrue(desc.contains("HDFC Savings"))
}
```

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Implement helper + wire Transfer/Savings submit paths; never send stub id `"house"` as goalId**

- [x] **Step 4: PASS + manual Transfer with two accounts; Savings creates/links goal when online**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(personal): wire Transfer TO into description and Savings goalId via createGoal

EOF
)"
```

---

### Task 12: Business GAP Wedding body ownership — honesty / neutralize

**Files:**
- Modify: `apk/.../business/shared/BusinessQuickAddSheets.kt` — for `POLL` / `MEMORY` / `TEAM_UPDATE` / `GENERAL_UPDATE`, either keep Wedding bodies with an explicit comment `// shared collab primitive — extract later` **and** ensure CTAs work after Task 7 mount, **or** replace bodies with `BusinessGapBody` (Coming soon) if Wedding POST against business momentId is unsafe
- Decision (locked): **If Wedding sheet POST uses group endpoints that fail on business moments, switch those kinds to `BusinessGapBody` (disabled Coming soon).** If they already no-op safely via GAP CTA, document and leave. Prefer safe honesty over shared-primitive extraction this sprint.
- Test: manual open POLL/MEMORY gap on business runway/ops moment — CTA must not corrupt group data

**Interfaces:**
- Consumes: Task 7 mount
- Produces: Honest GAP UX for collab kinds

- [x] **Step 1: Manual fail baseline** — open POLL gap after Task 7; note whether save hits group API

- [x] **Step 2: Record outcome in task notes**

- [x] **Step 3: Implement neutralize (`BusinessGapBody`) if unsafe; else add KDoc pointing to later extract task**

- [x] **Step 4: Re-verify**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(business): honest GAP collab sheets without unsafe Wedding POSTs

EOF
)"
```

---

### Task 13: MFG RETURN/ORDER catalog verify (fix only if regression)

**Files:**
- Test only first: `apk/app/src/test/java/com/example/momentra/ui/shell/business/manufacturing/MfgFidelitySmokeTest.kt` (already asserts RETURN in fulfilment + ORDER in capture enum)
- Cross-check: `backend/typescript/src/modules/business/operating-catalog.ts` STANDARD_PRODUCTION / MADE_TO_ORDER `quickAdds` includes `ORDER`, `RETURN`
- Modify only if hub filters them out incorrectly: `OperatingV12Hub.kt` / `MfgHubCatalog`

**Interfaces:**
- Consumes: existing `MfgHubCatalog`, `ManufacturingCapture`
- Produces: Confirmation green **or** minimal hub fix

- [x] **Step 1: Run** `./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.manufacturing.MfgFidelitySmokeTest`

- [x] **Step 2: If PASS, skip implement; if FAIL, fix catalog/hub**

- [x] **Step 3: Optional — assert `operating-catalog.ts` plant quickAdds contain RETURN/ORDER via existing backend test `operating-manufacturing.test.ts` (`catalog unlocks RETURN for plant subtypes`)

- [x] **Step 4: YAGNI skip Moments widget label divergence (audit §15) unless a one-line constant mismatch is obvious**

- [x] **Step 5: Commit only if code changed; else note “verified green” in PR description**

---

### Task 14: Personal habit / toast parity on shell saves (P2)

**Files:**
- Modify: `apk/.../AppShellScreen.kt` — ensure Income/Transfer/Savings `onSaved` sets `habitRewardMessage` (Income already partially does); LifeOps ATTENTION/ADJUST also set a short message if missing
- Modify: ExperienceHost Saved screen — leave as-is for non-CREATE expense edits (do not force toast redesign)
- Test: manual habit chip appears after Income save

**Interfaces:**
- Consumes: existing `habitRewardMessage` / `LaunchedEffect` dismiss in `AppShellScreen.kt`
- Produces: Consistent short “Saved · …” snack after Money/LifeOps shell sheets

- [x] **Step 1–5: Failing manual → set messages on missing kinds → verify → commit**

```bash
git commit -m "$(cat <<'EOF'
fix(personal): habit reward messages for Money and LifeOps shell saves

EOF
)"
```

---

### Task 15: Orphaned `ShellDestinationContent` params — wire or delete

**Files:**
- Modify: `apk/.../AppShellScreen.kt` `ShellDestinationContent` — after Tasks 4–6, `onLifeOpsQuickAdd`/`onMoneyQuickAdd` must be **used** when composing `PersonalExperienceHost`. If any other callbacks remain unused (grep inside function body), delete parameter + call-site args (YAGNI).
- Test: compile + `rg` that each remaining parameter identifier appears in the function body beyond the signature

**Interfaces:**
- Consumes: Task 4 wiring
- Produces: No unused QuickAdd lambdas on `ShellDestinationContent`

- [x] **Step 1: `rg` unused params — fail if `onLifeOpsQuickAdd` only appears in signature**

- [x] **Step 2: Confirm**

- [x] **Step 3: Wire or delete**

- [x] **Step 4: Compile**

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
refactor(shell): wire or remove orphaned ShellDestinationContent QuickAdd params

EOF
)"
```

---

### Task 16: Marketing Own Hold/Transfer hub honesty

**Files:**
- Modify: `apk/.../business/marketing/MarketingQuickAdds.kt` — Own hub tiles currently `unavailable = true` for Hold/Transfer (~890–891) while chrome chips open chrome-only sheets. Set `unavailable = false` for Own Hold/Transfer **or** change label to open the same chrome-only sheet path as chips (`MarketingChromeOnlySheet` / HOLD-TRANSFER preview that never posts).
- Decision (locked): **Enable tiles to open existing chrome-only preview** (same as chip path); CTA dismisses; copy already says “write API not available”.
- iOS Marketing hub parity if the same hard-Unavailable exists
- Test: `MarketingOwnQuickAddCopyTest.kt` extend with assertion that Hold/Transfer are not marked unavailable in Own hub tile builder (extract pure tile list if needed)

**Interfaces:**
- Consumes: existing chrome-only HOLD/TRANSFER sheets in `MarketingQuickAdds.kt`
- Produces: Hub tiles reachable; still non-posting

- [x] **Step 1: Failing test** — Own hub tile list Hold/Transfer `unavailable == false`

- [x] **Step 2: Run FAIL**

- [x] **Step 3: Flip flags / wire open sheet**

- [x] **Step 4: PASS + manual** — tile opens preview; confirm does not post activity

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(marketing): open Own Hold/Transfer chrome-only sheets from hub tiles

EOF
)"
```

---

### Task 17: Restaurant Reprint dismiss-only (no UPDATE post)

**Files:**
- Modify: `apk/.../business/restaurant/RestaurantUtilitySheets.kt` `RestaurantReprintReceiptSheet` — footer currently `onSubmit(RestaurantEventDraft(eventType = "UPDATE", …))`. Change to **dismiss-only** (call `onDismiss()` after local “reprinted” feedback optional via `OperatingToastHost` / Ember toast), matching Trading REPRINT in `OperatingQuickSheet` (`if (mode == "REPRINT") { onDismiss(); return }`).
- Modify: call sites in `RestaurantQuickAdds.kt` that pass `onSubmit` into reprint
- Test: unit extracting `fun restaurantReprintShouldPost(): Boolean = false` **or** source contract that Reprint footer does not call `onSubmit` with UPDATE; plus manual activity-count checklist

**Interfaces:**
- Consumes: Trading REPRINT dismiss pattern
- Produces: Reprint CTA never posts `UPDATE`

- [x] **Step 1: Write failing test/contract**

```kotlin
@Test
fun restaurantReprint_doesNotPostUpdate() {
    // After fix: footer path must not build eventType UPDATE for reprint
    assertFalse(RestaurantReprintPolicy.postsEvent)
}
```

Introduce `object RestaurantReprintPolicy { const val postsEvent = false }` used by the sheet.

- [x] **Step 2: FAIL while sheet still posts**

- [x] **Step 3: Implement dismiss-only + optional toast “Reprint sent · same bill id”**

- [x] **Step 4: PASS + manual** — activity list unchanged after Reprint

- [x] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
fix(restaurant): Reprint receipt dismiss-only; never posts UPDATE

EOF
)"
```

---

### Task 18: Document known divergences + out-of-scope honesty

**Files:**
- Create: `docs/qa/2026-11-18-quickadds-wiring-honesty-notes.md` (short)
- Contents must include:
  1. iOS Hub-as-sheet vs Android CREATE tab — known divergence; toast/deep-link parity handled via shared host + CREATE hub; no platform unification this program.
  2. Marketing HOLD/TRANSFER/REPRINT intentional non-post.
  3. Retail POS local hold / missing APIs — honesty only.
  4. Future/Lifestyle/Relationships stubs — hidden or non-tappable.
  5. Figma node ID remaps — not redone.
  6. Transfer API single-account limitation.
- Test: n/a (doc)

**Interfaces:**
- Consumes: decisions from Tasks 1–17
- Produces: Honesty notes for QA/reviewers

- [x] **Step 1: Draft doc with the six bullets above**

- [x] **Step 2: Self-review against Audit Findings out-of-scope list**

- [x] **Step 3: Commit**

```bash
git add docs/qa/2026-11-18-quickadds-wiring-honesty-notes.md
git commit -m "$(cat <<'EOF'
docs(qa): record QuickAdd wiring honesty and platform divergences

EOF
)"
```

---

## Self-review (plan vs audit)

| Audit item | Task(s) |
|------------|---------|
| P0 Personal QuickAdds unreachable | 4, 5, 6, 15 |
| P0 Business GAP never mounted | 7 |
| P0 Retail/Restaurant toasts orphaned | 1, 2, 3 |
| P1 Trading/Dist no toast | 9 (depends 1–2) |
| P1 OperatingChromeState leak | 10 |
| P1 Transfer TO / Savings goalId | 11 |
| P1 Dual expense paths | 6 |
| P1 REVENUE/INVOICE no-op | 8 |
| P1 GAP Wedding bodies | 12 |
| P1 MFG RETURN/ORDER | 13 (verify) |
| P2 Habit/toast Personal | 14 |
| P2 Orphaned shell params | 15 |
| P2 Marketing Hold/Transfer hub | 16 |
| P2 Restaurant Reprint posts UPDATE | 17 |
| P2 MFG Moments labels | 13 YAGNI skip |
| P2 iOS vs Android hub presentation | 18 document |
| Out-of-scope honesty | 18 (+ 16 non-post preserved) |

**Step scan:** Each step is one action (test → fail → implement → pass → commit). Bodies left to implementers except locked decisions (Master Expense, shared toast mount, clearTransientUi, Reprint dismiss-only, Marketing chrome-only).

**Type consistency:** `OperatingToastHost` / `MtoToastHost` delegate; `LifeOpsQuickAddKind` / `MoneyQuickAddKind` unchanged; `BusinessGapQuickAddSheet` signature unchanged; `clearTransientUi()` named consistently for Task 10 callers.

**Proportion:** Plan lists signatures, files, and assertions — not full Compose/SwiftUI implementations.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-18-moments-quickadds-sheets-toasts.md`. Please review the plan. Which execution approach would you prefer?

- **Subagent-driven** — A fresh subagent implements each task and a fresh reviewer checks it before the next one starts, then a whole-branch review at the end. Most thorough; costs a fresh context per task and per review.
- **Native** — I implement every task myself in this session, the way this harness runs work, then one fresh reviewer on the most capable model checks the whole branch. Cheapest and fastest; no independent review until the end. Runs well with a mid-tier session model, since the plan carries the design.

**For this plan I recommend Subagent-driven**, because the 18 tasks span independent Android/iOS surfaces with strict interface handoffs (toast host → overlays → hubs → chrome clear) where a shipped wiring miss silently breaks saves again. Does the plan capture what you want, and which approach should we use?
