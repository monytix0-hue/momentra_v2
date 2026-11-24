# Business Setup — Brand Name Label, Live Hero Title, Logo Slot, Keyboard Dismiss

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On all nine Business Setup flows (Android `apk/` + iOS `momentra/momentra/`), the primary name field reads **"Brand name"**, the hero card icon is a working logo-upload control, the hero title tracks that field live, and the keyboard can always be dismissed.

**Architecture:** One spec-driven document (`BusinessSetupDocuments` + `BusinessSetupShell` + `BusinessSetupDocumentKit`) drives all nine subtypes on both platforms; the nine subtypes differ only in spec data. A second, legacy Android surface (`BusinessSetupWizardContent.kt` → `TradingSetupDocument.kt`) is still reachable from Create → Business and carries its own hardcoded labels, so it needs the same relabel. Items **live hero title** and **logo upload** are already implemented on the unmerged branch `feat/setup-logo-live-hero`; this plan lands that branch first, then adds the remaining label and keyboard work on top rather than rewriting them.

**Tech Stack:** Jetpack Compose (`BasicTextField`, `rememberLauncherForActivityResult`, `WindowInsets`/`imePadding`, `KeyboardActions`); SwiftUI (`TextField`, `PhotosPicker`/`PHPicker`, `.scrollDismissesKeyboard`, `.submitLabel`, `.toolbar(placement: .keyboard)`); existing `BusinessSetupHeroTitle` / `BusinessSliceRepository.uploadCompanyLogo` / `APIClient.uploadCompanyLogo`.

**Spec:** User request (2026-11-23) + screenshot `Screenshot_2026-11-23_at_13.40.51` (Trading setup, hero still reading "Northstar Trading Setup", plain `▣` circle, keyboard open with Save draft / Schedule later / Start this business beneath it). Supersedes the unexecuted remainder of [`2026-11-22-business-setup-logo-live-hero-title.md`](2026-11-22-business-setup-logo-live-hero-title.md), whose Tasks 1-3 already landed as six commits on `feat/setup-logo-live-hero`.

## Global Constraints

- Apply to all 9 type codes: `CLIENT_CAMPAIGN`, `OWN_BUSINESS_CAMPAIGN`, `TRADING_WHOLESALE`, `DISTRIBUTION_DEALERSHIP`, `RETAIL_STORE`, `RESTAURANT_FOOD`, `CONSUMER_SERVICE`, `STANDARD_PRODUCTION`, `MADE_TO_ORDER`.
- Android and iOS change in lockstep; `BusinessSetupDocuments.kt` and `BusinessSetupDocuments.swift` are declared mirror files ("Lockstep with iOS").
- **Rename labels only. Never rename a field `key`.** `MomentCreateViewModel.kt:113-117` filters saved preferences through `spec(...).sections` keys, and `BusinessSetupFieldModelTest.kt:467-471` asserts `defaultTitleKey` is present on every document. A key rename silently drops the saved value.
- Label rename scope (decided 2026-11-23): exactly the **5** fields currently labelled `Business name` → **`Brand name`**. `Campaign name`, `Store name`, and `Restaurant name` keep their labels.
- The two Marketing documents' second field `Promoted brand` → **`Promoted brand / product`** (disambiguates it from the new `Brand name`).
- Exact copy strings: `"Brand name"` and `"Promoted brand / product"`. No trailing punctuation, no localisation.
- **Do not touch validation error strings.** `OperatingSetupPayloadBuilder` emits `errors["brandName"] = "Promoted brand is required."` on both platforms, and both suites assert it verbatim (`OperatingSetupPayloadBuilderTests.swift:324`, `OperatingSetupPayloadBuilderTest.kt:333`). Renaming the *label* to `Promoted brand / product` must not tempt anyone to reword the *error*; update label only, leave the error string and its assertions alone.
- **`logoMediaId` must join `allowedKeys`, or it is silently dropped.** `SetupPreferenceFilter.filterToCatalogKeys` keeps only keys present in the spec, and `allowedKeys` is derived from spec field keys (`BusinessCompatibilityStubs.swift:111,126`). The branch does `catalog.allowedKeys.union(["logoMediaId"])` in `MomentCreateModel.swift`; verify that survives the merge on both platforms, because a silent drop means the logo never persists while everything still appears to work.
- Hero title source stays `spec.defaultTitleKey`; format is `BusinessSetupHeroTitle.format`. Do not derive the title from `companyName`.
- Logo persists as `logoMediaId` in setup preferences **and** `profileJson.logoMediaId` on the linked company.
- Do not change: sticky-bar CTAs, subtype chips, section count/numbering/order, or review-summary key lists.

## Review Focus

- Editing **Company name** alone does not change the hero title; editing **Brand name** does. A user who only fills the legal company name will see the placeholder hero — expected, and the reason the two fields sit in the same panel; verify the placeholder is the Figma `heroTitle`, never `" Setup"` or blank.
- A remote draft prefill arriving *after* the user starts typing must not overwrite the name they typed. Both shells load prefill asynchronously and today overwrite `values` wholesale.
- The sticky CTA row must stay reachable while the keyboard is open, and there must always be a way to dismiss the keyboard without scrolling (a `Done` action on the field itself).
- The label rename must not shift the `reviewSummary` key list or the section `01`…`06` numbering that `BusinessAppState` and the smoke checklist depend on.
- Reopening a draft saved before this change still shows the saved name under the new `Brand name` label (label is presentational; the stored key is untouched).

---

## Baseline: what already exists

Confirmed 2026-11-23 by reading the working tree and `feat/setup-logo-live-hero` (25 files, +985/−34, **not merged**):

| Item | State on `HEAD` / working tree | State on `feat/setup-logo-live-hero` |
|---|---|---|
| Live hero title | Broken — `BusinessSetupShell.kt:188` and `BusinessSetupShell.swift:111` pass the static `spec.heroTitle` | Done — `BusinessSetupHeroTitle.format` + `heroTitleLive` |
| Logo upload on hero circle | Missing — static `▣` glyph (`BusinessSetupDocumentKit.kt:96-104`, `.swift:35-37`) | Done — tap target, preview, upload, `logoMediaId` |
| `Business name` → `Brand name` | 5 spec fields per platform still say `Business name` | **Not done** (5 per platform remain) |
| `Promoted brand` disambiguation | Still `Promoted brand` | **Not done** |
| Keyboard dismissal | **Not done on either platform** — zero `imePadding` / `ImeAction` in `ui/shell/business/`; iOS spec shell has no `.scrollDismissesKeyboard` | **Not done** |
| iOS build | **Red** — working tree deletes 9 `BusinessEmpty/*.swift` files but keeps `BusinessSetupWizardView.swift`, which still references the deleted `BusinessSetupDocState`, `SetupDocField`, `BusinessSetupDocTheme`, `SetupDocSectionGap`; an untracked `BusinessPlaceholder/BusinessV4PlaceholderViews.swift` defines a **duplicate** `BusinessSetupWizardView` | Branches from the same broken base |

---

## File map

| File | Responsibility |
|---|---|
| Delete: `momentra/momentra/Shell/BusinessEmpty/BusinessSetupWizardView.swift` (tracked) | Removes the stale six-section wizard + duplicate `BusinessSetupWizardView` type; `BusinessV4PlaceholderViews.swift` already provides the replacement over `BusinessSetupShell` |
| Modify: `apk/.../setup/BusinessSetupDocuments.kt` | 5 `Brand name` labels; 2 `Promoted brand / product` labels |
| Modify: `momentra/.../BusinessSetup/BusinessSetupDocuments.swift` | Same 7 edits |
| Modify: `apk/.../empty/business/TradingSetupDocument.kt` | Legacy surface's hardcoded `"Business name"` (line 614) |
| Modify: `apk/.../setup/BusinessSetupShell.kt` | `imePadding`, scroll dismissal, prefill-does-not-clobber guard |
| Modify: `momentra/.../BusinessSetup/BusinessSetupShell.swift` | `.scrollDismissesKeyboard`, focus state, prefill guard |
| Modify: `apk/.../setup/BusinessSetupDocumentKit.kt` | `ImeAction.Done` + `keyboardActions` on `Field`, sticky bar IME awareness |
| Modify: `momentra/.../BusinessSetup/BusinessSetupDocumentKit.swift` | `.submitLabel(.done)` on `FieldRow`; keyboard `Done` toolbar |
| Create: `apk/.../setup/BusinessSetupFieldInputPolicy.kt` | Pure single-line IME-action policy (unit-testable) |
| Create: `momentra/.../BusinessSetup/BusinessSetupFieldInputPolicy.swift` | Same |
| Modify: `apk/.../setup/BusinessSetupFieldModelTest.kt` | Label guards for all 9 |
| Modify: `momentra/momentraTests/BusinessSetupFieldModelTests.swift` | Same |
| Modify: `docs/qa/BUSINESS_V4_APPSTATE_SMOKE.md` | Device checks for label, title, logo, keyboard |

---

### Task 1: Restore a buildable iOS baseline and land `feat/setup-logo-live-hero`

The branch already implements issues #2 and #3; nothing else can be validated on iOS until the tree builds and that branch is in.

**Files:**
- Delete: `momentra/momentra/Shell/BusinessEmpty/BusinessSetupWizardView.swift`
- Keep (untracked, must be `git add`ed): `momentra/momentra/Shell/BusinessPlaceholder/BusinessV4PlaceholderViews.swift`, `momentra/momentra/Shell/BusinessSetup/{BusinessSetupCatalog,BusinessSetupDateFormatting,BusinessSetupInviteSection,BusinessSetupTheme}.swift`

**Interfaces:**
- Consumes: nothing
- Produces: a tree where `BusinessSetupHeroTitle.format(name:placeholder:)` and `HeroCard(… logoBitmap/logoImage:logoUrl:logoUploading:onLogoClick:)` exist on both platforms, and `BusinessSetupWizardView` is defined exactly once

- [ ] **Step 1: Merge the branch**

The merge is **not** conflict-free, and one conflict is delete/modify rather than textual:

```bash
git -C /Volumes/coding/momentra_v2 merge --no-ff feat/setup-logo-live-hero
```

Verified 2026-11-23 with `git merge-tree --write-tree HEAD feat/setup-logo-live-hero` → **exits 0, zero conflicts**. The merge is clean *against committed HEAD*; it only fails today because the working tree is dirty (279 deletions, 129 modifications, 96 untracked) and `git merge` refuses to start. So the order is forced: **commit the dirty tree first, then merge.** Do not `git stash` — a stash on top of 279 deletions is far harder to recover than a commit.

Once committed, one delete/modify collision appears:
`momentra/momentra/Shell/BusinessEmpty/BusinessSetupDocSpec.swift` is **deleted** by that commit but **modified** on the branch (its only branch change is `+1` line adding `logoMediaId` to `preferenceKeys`). Resolution: **keep the deletion** (`git rm` it). The branch's `+1` line is dead weight — `preferenceKeys` has **zero** remaining references in the working tree, because the spec shell stores a flat `[String: String]` instead of a `BusinessSetupDocState`.

Do **not** resolve it by keeping the file: re-adding it resurrects `BusinessSetupDocState`, which nothing declares anymore, and the target is folder-synchronised so it would re-enter the build and break it.

Note the merge also carries branch edits to `BusinessSetupShell.swift`, `BusinessSetupDocumentKit.swift`, `MomentCreateModel.swift`, and `OperatingSetupPayloadBuilder.swift` on both platforms — the same files Task 2-6 edit. Land the merge *first*, on its own, so later label/keyboard diffs are readable.

Expected after resolution: no remaining conflicts, `git status` clean apart from the pre-existing untracked `BusinessPlaceholder/` files.

- [ ] **Step 2: Resolve the duplicate `BusinessSetupWizardView`**

`git grep -n "struct BusinessSetupWizardView"` must report exactly one hit after deleting the `BusinessEmpty` copy — the one in `BusinessPlaceholder/BusinessV4PlaceholderViews.swift:57`, whose `init` already accepts `initialTypeCode`, `editingMomentStatus`, and `ownerName`, satisfying the call sites in `Components/EditMomentSetupHost.swift:96` and `Components/MomentScreen.swift:76`.

```bash
git rm momentra/momentra/Shell/BusinessEmpty/BusinessSetupWizardView.swift
```

- [ ] **Step 3: Prove no dangling references remain**

Run: `grep -rn "BusinessSetupDocState\|BusinessSetupDocTheme\|SetupDocSectionGap\|SetupDocField" momentra/momentra --include=*.swift`
Expected: no matches.

- [ ] **Step 4: Build iOS**

Run: `xcodebuild -project momentra/momentra.xcodeproj -scheme momentra -destination 'generic/platform=iOS Simulator' build`
Expected: `BUILD SUCCEEDED`. The project uses `PBXFileSystemSynchronizedRootGroup`, so added/removed files need no `project.pbxproj` edits.

**Known pre-existing failure — expect exactly this one, and fix it in this direction.**
Baseline build (verified 2026-11-23, and present at `HEAD`, so it is not caused by this plan) fails with a single error:

```
momentra/momentra/Shell/AppShellView.swift:1298:37: error: cannot convert value of type 'UInt64' to expected argument type 'Int'
```

`AppShellModel.swift:25` declares `businessTabRefreshToken` as `UInt64` (incremented with `&+=` at `:857`). The two `BusinessOperating` consumers declare it as `Int`:
`OperatingExperienceHost.swift:9` and `OperatingMomentHubView.swift:146`.

The correct direction is to **widen the two consumers to `UInt64`**, not to cast at the call site and not to change the model:

```swift
// OperatingExperienceHost.swift:9
let refreshToken: UInt64
// OperatingMomentHubView.swift:146
let refreshToken: UInt64
```

Rationale: every other refresh-token consumer in the app is already `UInt64` — `Personal/Life/PersonalLifeActiveView.swift:5`, `GroupActive/Shared/GroupPulseActiveView.swift:5`, `GroupActive/Wedding/Pulse/WeddingPulseActiveView.swift:5`, and ~27 more. These two files are the outliers. `refreshToken` is only ever interpolated into a `.task(id:)` string (`OperatingMomentHubView.swift:301`), so widening is behaviour-preserving. Do not "fix" it with `Int(...)` — that traps on overflow and diverges from the house convention.

- [ ] **Step 5: Run the existing unit suites**

Run: `xcodebuild test -project momentra/momentra.xcodeproj -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:momentraTests/BusinessSetupFieldModelTests -only-testing:momentraTests/BusinessSetupHeroTitleTests -only-testing:momentraTests/OperatingSetupPayloadBuilderTests`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A momentra/momentra/Shell/BusinessEmpty momentra/momentra/Shell/BusinessPlaceholder
git commit -m "refactor(setup): finish the spec-shell migration on iOS and land logo + live title"
```

---

### Task 2: Rename the five `Business name` fields to `Brand name`

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/setup/BusinessSetupDocuments.kt:420, 524, 853, 959, 1061`
- Modify: `momentra/momentra/Shell/BusinessSetup/BusinessSetupDocuments.swift:351, 442, 729, 823, 914`
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/empty/business/TradingSetupDocument.kt:614`
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/setup/BusinessSetupFieldModelTest.kt`
- Test: `momentra/momentraTests/BusinessSetupFieldModelTests.swift`

**Interfaces:**
- Consumes: `BusinessSetupDocumentSpec.sections[].fields[]` with `.key` and `.label`
- Produces: no signature change; `defaultTitleKey` values are unchanged (`businessName`, `serviceName`)

- [ ] **Step 1: Write the failing Android test**

```kotlin
@Test
fun noDocumentLabelsItsPrimaryFieldBusinessName() {
    val all = BusinessSetupFamily.entries.flatMap { it.chips }
    all.forEach { chip ->
        val labels = BusinessSetupDocuments.spec(chip.typeCode).sections
            .flatMap { it.fields }
            .map { it.label }
        assertFalse("${chip.typeCode} still says Business name", labels.contains("Business name"))
    }
}

@Test
fun theFiveSharedNameFieldsAreLabelledBrandName() {
    listOf(
        "TRADING_WHOLESALE",
        "DISTRIBUTION_DEALERSHIP",
        "CONSUMER_SERVICE",
        "STANDARD_PRODUCTION",
        "MADE_TO_ORDER",
    ).forEach { code ->
        val spec = BusinessSetupDocuments.spec(code)
        val primary = spec.sections.flatMap { it.fields }
            .first { it.key == spec.defaultTitleKey }
        assertEquals("Brand name", primary.label)
    }
}

@Test
fun brandNameRelabelKeepsTheStoredKey() {
    assertEquals("businessName", BusinessSetupDocuments.spec("TRADING_WHOLESALE").defaultTitleKey)
    assertEquals("serviceName", BusinessSetupDocuments.spec("CONSUMER_SERVICE").defaultTitleKey)
}
```

- [ ] **Step 2: Run it — expect FAIL**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests "com.example.momentra.ui.shell.business.setup.BusinessSetupFieldModelTest"`
Expected: FAIL on `noDocumentLabelsItsPrimaryFieldBusinessName`.

- [ ] **Step 3: Change the five labels on Android** — replace the second argument at each anchor, leaving every `key` and `placeholder` untouched:

`text("businessName", "Business name", "Northstar Trading")` → `text("businessName", "Brand name", "Northstar Trading")` (and the same one-argument swap at the `Metro Distribution`, `BrightCare`, and both `Forge & Form` call sites; `serviceName` keeps its key).

- [ ] **Step 4: Change the legacy document's label** — `TradingSetupDocument.kt:614`, `label = "Business name"` → `label = "Brand name"`. It stays reachable through `BusinessSetupWizardContent.kt:369`.

- [ ] **Step 5: Mirror the test on iOS and run it red, then green**

```swift
@Test func noDocumentLabelsItsPrimaryFieldBusinessName() {
    let codes = ["CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN", "TRADING_WHOLESALE",
                 "DISTRIBUTION_DEALERSHIP", "RETAIL_STORE", "RESTAURANT_FOOD",
                 "CONSUMER_SERVICE", "STANDARD_PRODUCTION", "MADE_TO_ORDER"]
    for code in codes {
        let labels = BusinessSetupDocuments.spec(for: code).sections.flatMap(\.fields).map(\.label)
        #expect(!labels.contains("Business name"), "\(code) still says Business name")
    }
}
```

Apply the identical five label edits in `BusinessSetupDocuments.swift`, then run Steps 1-4's verification on iOS.

- [ ] **Step 6: Run both suites — expect PASS**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests "*BusinessSetupFieldModelTest"` and the iOS `-only-testing:momentraTests/BusinessSetupFieldModelTests` command.

- [ ] **Step 7: Commit**

```bash
git commit -am "feat(setup): label the shared name field Brand name across all nine setups"
```

---

### Task 3: Disambiguate the Marketing second field

**Files:**
- Modify: `apk/.../setup/BusinessSetupDocuments.kt:188, 302`
- Modify: `momentra/.../BusinessSetup/BusinessSetupDocuments.swift:150, 243`
- Test: both field-model suites

**Interfaces:**
- Consumes: existing labels; keys `promotedBrand` and `brandName` stay as they are — `OperatingSetupPayloadBuilder` reads both (`firstNonEmpty(prefs["partyName"], prefs["brandName"])`)
- Produces: no signature change

- [ ] **Step 1: Write the failing test (Android, mirrored on iOS)**

```kotlin
@Test
fun marketingPromotedBrandIsDistinctFromBrandName() {
    listOf("CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN").forEach { code ->
        val labels = BusinessSetupDocuments.spec(code).sections.flatMap { it.fields }.map { it.label }
        assertTrue("$code missing promoted-brand / product", labels.contains("Promoted brand / product"))
        assertFalse("$code still has the ambiguous label", labels.contains("Promoted brand"))
    }
}
```

- [ ] **Step 2: Run red**, then change the two `"Promoted brand"` labels per platform to `"Promoted brand / product"` (keys and placeholders untouched), then run green.

- [ ] **Step 3: Confirm the payload builder still resolves the party name**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests "*OperatingSetupPayloadBuilderTest"`
Expected: PASS — no key changed, so `prefs["brandName"]` and `prefs["promotedBrand"]` still resolve.

- [ ] **Step 4: Commit**

```bash
git commit -am "feat(setup): rename marketing promoted-brand field to avoid clashing with Brand name"
```

---

### Task 4: Make the hero title follow the Brand name field, and never let prefill clobber typing

The branch added `BusinessSetupHeroTitle` and wired `heroTitleLive`. This task pins the behaviour the user reported and removes the race that can still make the title look stuck.

**Files:**
- Modify: `apk/.../setup/BusinessSetupShell.kt` (the `LaunchedEffect(Unit)` prefill block)
- Modify: `momentra/.../BusinessSetup/BusinessSetupShell.swift` (mirror prefill block)
- Test: `apk/.../setup/BusinessSetupHeroTitleTest.kt`, `momentra/momentraTests/BusinessSetupHeroTitleTests.swift`

**Interfaces:**
- Consumes: `BusinessSetupHeroTitle.format(name: String?, placeholder: String) -> String`; `spec.defaultTitleKey`; `spec.heroTitle`
- Produces:
  - Kotlin: `object BusinessSetupPrefill { fun shouldApply(remote: String?, typed: String?, userEdited: Boolean): Boolean }`
  - Swift: `enum BusinessSetupPrefill { static func shouldApply(remote: String?, typed: String?, userEdited: Bool) -> Bool }`

- [ ] **Step 1: Write the failing prefill tests**

```kotlin
@Test
fun prefillDoesNotOverwriteWhatTheUserTyped() {
    assertFalse(BusinessSetupPrefill.shouldApply("Northstar Trading", "enterprises", userEdited = true))
}

@Test
fun prefillStillFillsAnUntouchedEmptyField() {
    assertTrue(BusinessSetupPrefill.shouldApply("Northstar Trading", "", userEdited = false))
}

@Test
fun prefillIsIgnoredWhenTheRemoteValueIsBlank() {
    assertFalse(BusinessSetupPrefill.shouldApply(null, "enterprises", userEdited = false))
    assertFalse(BusinessSetupPrefill.shouldApply("   ", "enterprises", userEdited = false))
}
```

- [ ] **Step 2: Run red**, implement the helper (`userEdited` wins; otherwise apply a non-blank trimmed remote value only when the local value is blank), then run green.

- [ ] **Step 3: Track edits in both shells** — add a `userEditedKeys` set; every `values[key] = …` coming from a control records the key; the prefill loop consults `BusinessSetupPrefill.shouldApply(...)` per key instead of assigning unconditionally.

- [ ] **Step 4: Assert the title the user reported**

```kotlin
@Test
fun tradingHeroTitleTracksTheBrandNameField() {
    assertEquals("enterprises Setup", BusinessSetupHeroTitle.format("enterprises", "Northstar Trading Setup"))
    assertEquals("Northstar Trading Setup", BusinessSetupHeroTitle.format(null, "Northstar Trading Setup"))
}
```

Plus a Swift mirror. This is the regression the screenshot shows: hero frozen at `Northstar Trading Setup` while `enterprises` was being typed.

- [ ] **Step 5: Run all four suites**, then commit

```bash
git commit -am "fix(setup): stop draft prefill from overwriting a typed brand name"
```

---

### Task 5: Android keyboard — pad for the IME and give every field a Done action

**Files:**
- Create: `apk/.../setup/BusinessSetupFieldInputPolicy.kt`
- Modify: `apk/.../setup/BusinessSetupDocumentKit.kt` (`Field` ~line 250-332; `StickyBar` ~line 908)
- Modify: `apk/.../setup/BusinessSetupShell.kt` (root `Box`/`Column`)
- Test: `apk/app/src/test/.../setup/BusinessSetupFieldInputPolicyTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:

```kotlin
object BusinessSetupFieldInputPolicy {
    /** Every setup field is single-line, so the IME action both completes the entry and dismisses. */
    fun imeAction(): ImeAction = ImeAction.Done
    const val CLEAR_FOCUS_ON_SUBMIT: Boolean = true
}
```

- [ ] **Step 1: Write the failing policy test**

```kotlin
@Test
fun setupFieldsDismissTheKeyboardOnImeAction() {
    assertEquals(ImeAction.Done, BusinessSetupFieldInputPolicy.imeAction())
    assertTrue(BusinessSetupFieldInputPolicy.CLEAR_FOCUS_ON_SUBMIT)
}
```

- [ ] **Step 2: Run red**, implement, run green.

- [ ] **Step 3: Wire the fields** — on `BasicTextField` add

```kotlin
keyboardOptions = KeyboardOptions(
    capitalization = if (forceUppercase) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
    imeAction = BusinessSetupFieldInputPolicy.imeAction(),
),
keyboardActions = KeyboardActions(
    onDone = { if (BusinessSetupFieldInputPolicy.CLEAR_FOCUS_ON_SUBMIT) focusManager.clearFocus() },
),
```

resolving `focusManager` from `LocalFocusManager.current` at the top of the composable.

- [ ] **Step 4: Consume the IME inset on the scrolling container** — the host (`MomentraFullscreenDialog`) pads bottom by navigation bars only and runs with `decorFitsSystemWindows = false`, so nothing consumes the IME inset today. On the shell's root scrolling `Column`, add `.imeNestedScroll()` and `.imePadding()`; on the sticky bar wrapper keep it pinned above the IME by adding `.imePadding()` to `StickyBar`.

- [ ] **Step 5: Manual check on the emulator** — open Trading setup, tap Brand name, type; the hero title updates per keystroke; `Done` on the keyboard dismisses it; Save draft / Schedule later / Start this business remain visible and tappable while the keyboard is up.

- [ ] **Step 6: Commit**

```bash
git commit -am "fix(setup): dismiss and clear the keyboard, keep the sticky CTA above the IME"
```

---

### Task 6: iOS keyboard — scroll-to-dismiss, Done submit label, keyboard toolbar

**Files:**
- Create: `momentra/momentra/Shell/BusinessSetup/BusinessSetupFieldInputPolicy.swift`
- Modify: `momentra/momentra/Shell/BusinessSetup/BusinessSetupDocumentKit.swift` (`FieldRow`, line 166-206)
- Modify: `momentra/momentra/Shell/BusinessSetup/BusinessSetupShell.swift` (the `ScrollView` at line 81)
- Test: `momentra/momentraTests/BusinessSetupFieldInputPolicyTests.swift`

**Interfaces:**
- Consumes: nothing
- Produces:

```swift
enum BusinessSetupFieldInputPolicy {
    /// A single-line setup field commits on return and dismisses the keyboard.
    static let submitLabel: SubmitLabel = .done
    static let scrollDismissesKeyboard: ScrollDismissesKeyboardMode = .interactively
}
```

- [ ] **Step 1: Write the failing test**

```swift
@Test func setupFieldsSubmitWithDone() {
    #expect(BusinessSetupFieldInputPolicy.submitLabel == .done)
    #expect(BusinessSetupFieldInputPolicy.scrollDismissesKeyboard == .interactively)
}
```

- [ ] **Step 2: Run red**, implement, run green.

- [ ] **Step 3: Wire the field** — in `FieldRow`, add `.submitLabel(BusinessSetupFieldInputPolicy.submitLabel)` and a local `@FocusState private var focused: Bool` driving `.focused($focused)`, plus `.toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { focused = false } } }`. Keeping the focus state local to `FieldRow` avoids threading it through every call site in `BusinessSetupShell`.

- [ ] **Step 4: Wire the scroll view** — on the `ScrollView` in `BusinessSetupShell.body`, add `.scrollDismissesKeyboard(BusinessSetupFieldInputPolicy.scrollDismissesKeyboard)`.

- [ ] **Step 5: Manual check on the simulator** — Trading setup, tap Brand name, type; hero updates live; the keyboard's `Done` bar dismisses it; dragging the document dismisses it; the sticky bar stays above the keyboard.

- [ ] **Step 6: Commit**

```bash
git commit -am "fix(setup): add Done and scroll dismissal so the iOS setup keyboard always closes"
```

---

### Task 7: Smoke checklist and cross-subtype verification

**Files:**
- Modify: `docs/qa/BUSINESS_V4_APPSTATE_SMOKE.md`

**Interfaces:**
- Consumes: Tasks 1-6
- Produces: checklist lines an engineer ticks on device

- [ ] **Step 1: Add the block**

```markdown
### Brand name label, live hero title, logo, keyboard (all 9 setups)
- [ ] Trading, Distribution, Consumer Service, Manufacturing, Job order show "Brand name" (not "Business name")
- [ ] Retail Store still says "Store name", Restaurant still says "Restaurant name", Marketing still says "Campaign name"
- [ ] Marketing's second field reads "Promoted brand / product"
- [ ] Typing in Brand name updates the hero to "{name} Setup" on every keystroke
- [ ] Clearing Brand name restores the Figma placeholder hero title
- [ ] Editing Company name alone leaves the hero title unchanged
- [ ] Tapping the hero circle opens the image picker; after upload the circle shows the logo
- [ ] Keyboard has a Done affordance and closes on scroll; sticky CTAs stay tappable while it is open
- [ ] Reopening a draft saved before this change still shows the saved name under "Brand name"
```

- [ ] **Step 2: Device pass on one subtype per family** — Trading, Retail Store, Manufacturing, Client campaign. Four samples prove the shared document inherits the change for all nine.

- [ ] **Step 3: Commit**

```bash
git commit -am "docs(qa): brand name, live title, logo and keyboard checks for the nine setups"
```

---

## Self-review

1. **Spec coverage:** (1) five `Business name` → `Brand name` on both platforms, plus the legacy Android surface → Task 2. (2) Trading hero not updating → Task 4, on top of the branch's `BusinessSetupHeroTitle`. (3) Logo upload at the hero circle → Task 1 lands the branch that implements it. (4) Keyboard never disappearing → Tasks 5-6. APK + iOS in every task.
2. **Step scan:** label edits name exact anchors and exact copy; the two pure helpers carry their assertions inline; the prefill guard states its three-case rule once; no Compose/SwiftUI body is transcribed.
3. **Type consistency:** `BusinessSetupHeroTitle.format(name:placeholder:)`, `BusinessSetupPrefill.shouldApply(remote:typed:userEdited:)`, `BusinessSetupFieldInputPolicy.imeAction()` / `.submitLabel` are defined once and referenced by those names later.
4. **Review Focus:** company-name-doesn't-retitle is pinned by the Task 4 test; the prefill race has three tests in Task 4; sticky-CTA/keyboard reachability is a Task 5 Step 5 and Task 6 Step 5 manual check plus a Task 7 line; section/review-summary stability is covered by the unchanged-key assertions in Task 2 Step 1 and Task 3 Step 3; reopening a pre-change draft is a Task 7 line because it depends on stored data this plan does not touch.
5. **Proportion:** the plan is shorter than the code it changes because the largest two items already exist on a branch — Task 1 lands them instead of re-specifying them.
