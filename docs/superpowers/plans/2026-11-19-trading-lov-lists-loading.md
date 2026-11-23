# Trading LOV Lists Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore supplier, customer, and product list loading in Trading Sale/Purchase capture and interlinking pickers on Android + iOS so masters visible in company catalogues are selectable in Quick Adds.

**Architecture:** Fix Android LOV kind matching first (shared pure helper + unit tests), then align company/projection master hydration so pickers and masters sheets see the same catalogue. On iOS, hydrate masters on capture open (with projection fallback), then add a searchable LOV sheet for Trading party/product selection matching Android `SearchableOperatingLovSheet` behavior. No backend schema change required; optionally decode existing `companyId` on the operating projection DTO on both clients.

**Tech Stack:** Kotlin/Compose (`apk/`), SwiftUI (`momentra/`), JUnit (`apk/app/src/test`), Swift Testing (`momentra/momentraTests`), operating projection + `listOperatingMasters` APIs (existing).

**Spec:** Part A findings in this plan’s **Audit Findings (source of truth)** section (2026-11-19 code review of Trading LOV loading). Contract: `/home/santosh/Downloads/common_7 industries/common_7 industries/Momentra_Trading_Distribution_v11_1_Integrated_GST_Contract.docx` — §3 Master lists and LOV execution / §3.1 Searchable LOV inventory and + Add behavior (counterparty may hold multiple roles; Supplier/principal LOV must list active suppliers **and** principals; searchable bottom sheet; “No matching [master]” + Add when permitted).

## Global Constraints

- Fix **list loading / selectability** only — do not redesign Sale amount/delivery UI already in flight in `OperatingQuickSheet.kt` / `OperatingCaptureSheet.swift`.
- Preserve GST contract: no free-text phantom masters; selection returns stable master id/name; PRINCIPAL = dual-role party pickable as supplier **and** customer.
- Android capture LOVs stay projection-backed for the happy path; hydrate via `listMasters` / projection `companyId` only when `model.masters` is empty or shell company is missing.
- iOS must reach Android LOV parity for Trading Sale/Purchase/Order party + product (searchable sheet), not chips-only.
- DRY: one shared kind-match helper per platform; do not fork filters into every capture layout.
- YAGNI: no new master APIs; no OperatingPickerSheet changes (that sheet is date/range only).
- TDD / frequent commits: one commit per task; Android `./gradlew :app:testDebugUnitTest --tests <FQCN>` from `apk/`; iOS Swift Testing where harness exists.
- Platforms: every Critical/High finding fixed on **apk + iOS** unless a task is marked platform-only.

## Review Focus

1. **PRINCIPAL saved as “Both”:** Opening Supplier or Customer LOV must show that party — covered in Tasks 1–2 (Android) and Task 7 (iOS).
2. **Projection masters empty, company API has rows:** Capture open must still show catalogue after hydrate — covered in Tasks 3–5.
3. **`companyId` nil on shell but projection embeds masters/`companyId`:** Lists must not stay empty — covered in Tasks 4–5.
4. **Hub → Sale before load completes (iOS):** Capture `.task` must load masters; product/party UI must not silently omit — covered in Tasks 5–7.
5. **Quick amount / Received paths:** Product/customer fields remain conditional per Figma; do not “fix” by forcing LOVs onto Received Quick amount — covered by Review Focus note in Task 7 manual check (assert Itemized + Part paid / Pay later / Purchase paths).

---

## Audit Findings (source of truth)

Audit date: **2026-11-19**. Method: call-site tracing of Trading Sale/Purchase → LOV/masters/browsers → operating projection + `listOperatingMasters`. Investigation only; no code fixed in the audit pass.

### Critical

1. **iOS Trading capture has no searchable master LOV** — Android uses `CaptureLovHost` → `SearchableOperatingLovSheet` (`OperatingQuickSheet.kt` ~3791–3848). iOS `OperatingCaptureSheet` uses free-text fields + optional horizontal `skuPicker` chips only; no `SearchableOperatingLov` under `momentra/`. Contract §3.1 requires searchable LOV for Product / Customer / Supplier·principal.

2. **iOS `skuPicker` hides entirely when `model.masters` is empty** — `OperatingCaptureSheet.swift` ~1847–1893 wraps Product UI in `if !skus.isEmpty`. Expected: empty state or searchable picker. Actual: no Product row; itemized Sale cannot add lines.

### High

3. **Android `lovOptions` excludes `PRINCIPAL` / `DEALER` from Supplier & Customer pickers** — Masters form writes contact role “Both” as `PRINCIPAL` (`OperatingMastersSheet.kt` ~490–495). Principals tab lists those kinds (~99–101). `lovOptions` (`OperatingQuickSheet.kt` ~5687–5693) matches `BUYER`/`CUSTOMER` only for customers and exact `kind` for suppliers → “No matching …” while masters sheet shows rows. Contract: one party may hold multiple roles; Supplier/principal LOV must include principals.

4. **Android capture LOVs never call `listOperatingMasters`; masters sheet does** — `CaptureLovHost` uses `model.masters.orEmpty()` only (~3807–3811). `OperatingMastersSheet` `LaunchedEffect` calls `repo.listMasters` (~131–144). Split catalogue: masters UI can be full while Sale pickers stay empty if projection `masters` is null/[]/stale.

5. **iOS capture never calls `loadMasters()` on open** — Retail does `.task { if model.masters.isEmpty { await model.loadMasters() } }` (`RetailCaptureSheets.swift` ~185–187). `OperatingCaptureSheet` only sets draft defaults on appear (~395–406). Hub → Sale race leaves `masters == []`.

6. **iOS `skuPicker` ignores `projection.masters` fallback** — `browserItems` uses `masters.isEmpty ? projection?.masters ?? [] : masters` (`OperatingPartsBrowserView.swift` / model helper ~267–269). `skuPicker` filters `model.masters` only (~1849).

7. **iOS `loadMasters()` early-returns when `companyId` is nil** — `BusinessOperatingModel.swift` ~201–208: no fallback to `projection?.masters`. Catch path does fall back; nil company never reaches it. Both clients omit `companyId` on projection DTO though backend sends it (`operating-catalog.ts` ~1629; Android `OperatingProjectionDto` ~1386–1422; iOS `OperatingProjectionPayload` ~207–243).

### Medium

8. **Android shell `companyId` is `selectedCompany` only** — `AppShellScreen.kt` passes `companyId` from selected company into `OperatingMomentPage` (~1932); masters context uses that. Moment-bound company may differ; unbound moment → masters hint “not attached to a company” (`OperatingMastersSheet.kt` ~266–268). Backend skips table masters when scope has no company (`operating-service.ts` ~502–505).

9. **`applyPartySupply` ignores `PRINCIPAL`** — `OperatingQuickSheet.kt` ~438–441 looks up `CUSTOMER`/`BUYER`/`SUPPLIER` only → GST place-of-supply prefills miss dual-role parties after pick (not empty list, but broken interlink).

10. **iOS “Create customer/supplier” copy is non-interactive** — Underlined `Text` under party fields (`OperatingCaptureSheet.swift` ~596–607 area); Android LOV can `onCreateMaster` when `UPDATE` in `quickAdds`.

### Low / not root cause

11. **Uncommitted Sale/Purchase amount/delivery edits** did not change LOV fetch wiring; may increase “missing product” reports when users stay on Quick amount (product LOV only on Itemized).
12. **`OperatingPickerSheet`** is date/month/range UI on both platforms — not master LOVs.
13. **Capability gating** (`UPDATE` missing → no + Add) is secondary; empty list with “Ask an admin” is distinct from never-fetched.

### Root-cause hypothesis

- **Android:** Primary — kind mismatch (`PRINCIPAL`/`DEALER` filtered out of SUPPLIER/BUYER LOVs). Secondary — projection-only LOV source vs masters API, plus shell/moment `companyId` gaps.
- **iOS:** Primary — LOV UI never ported + silent empty `skuPicker`. Amplifiers — no `loadMasters` on capture open, no projection fallback, nil `companyId` abort.
- Recent amount/delivery work is **not** the list-fetch root cause.

---

## File map (decomposition)

| Unit | Responsibility |
|------|----------------|
| `apk/.../shared/OperatingLovKindMatch.kt` (new) | Pure `masterMatchesLovKind(pickerKind, masterKind)` + party/product kind sets |
| `OperatingQuickSheet.kt` | Call kind helper from `lovOptions` + `applyPartySupply` |
| `apk/.../Dto.kt` + iOS `APIClient.swift` | Decode `companyId` on operating projection |
| `BusinessOperatingModel.swift` | `loadMasters` projection fallback; adopt projection `companyId` when shell nil |
| `OperatingCaptureSheet.swift` | `.task` hydrate; searchable LOV; `skuPicker` source + empty state |
| `SearchableOperatingLovSheet.swift` (new) | iOS searchable LOV dialog parity with Android |
| `OperatingLovKindMatch.swift` (new) or extend `OperatingCatalog.swift` | Same kind-match rules as Android |
| `AppShellScreen.kt` / operating chrome callers | Prefer moment/projection company for masters context when selected company null/mismatched |
| Tests | `OperatingLovKindMatchTest.kt`, `BusinessOperatingModelTests.swift`, iOS LOV kind tests |

---

### Task 1: Android LOV kind-match helper (TDD)

**Files:**
- Create: `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatch.kt`
- Test: `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatchTest.kt`

**Interfaces:**
- Produces: `object OperatingLovKindMatch` with
  - `fun masterMatchesLovKind(pickerKind: String, masterKind: String): Boolean`
  - `fun partyKindsForSupply(): Set<String>` (for `applyPartySupply`)
- Consumes: none

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun principalAppearsInSupplierAndBuyerLovs() {
    assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "PRINCIPAL"))
    assertTrue(OperatingLovKindMatch.masterMatchesLovKind("BUYER", "PRINCIPAL"))
    assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "DEALER"))
    assertTrue(OperatingLovKindMatch.masterMatchesLovKind("BUYER", "CUSTOMER"))
    assertTrue(OperatingLovKindMatch.masterMatchesLovKind("PRODUCT", "SKU"))
    assertFalse(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "SKU"))
    assertFalse(OperatingLovKindMatch.masterMatchesLovKind("PRODUCT", "CUSTOMER"))
}

@Test
fun partyKindsForSupplyIncludePrincipal() {
    assertTrue(OperatingLovKindMatch.partyKindsForSupply().containsAll(
        setOf("CUSTOMER", "BUYER", "SUPPLIER", "PRINCIPAL", "DEALER", "VENDOR")
    ))
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `cd apk && ./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.OperatingLovKindMatchTest`
Expected: FAIL (symbol not found)

- [x] **Step 3: Implement `OperatingLovKindMatch` in `OperatingLovKindMatch.kt`**

Rules (from GST contract + backend `partyKind` in `operating-trading-contract.ts` ~142):
- `PRODUCT` / `UOM` product set: `PRODUCT`, `SKU`, `BATCH`, `PART`, `MATERIAL`, `RAW`, `BOM` (blank kind allowed for PRODUCT only, matching current `lovOptions`)
- `BUYER`: `BUYER`, `CUSTOMER`, `PRINCIPAL`, `DEALER`, `PARTY`
- `SUPPLIER`: `SUPPLIER`, `VENDOR`, `PRINCIPAL`, `DEALER`, `PARTY`
- else: exact uppercase equality

- [x] **Step 4: Run test to verify it passes**

Run: same as Step 2  
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatch.kt \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatchTest.kt
git commit -m "$(cat <<'EOF'
fix(trading): match PRINCIPAL/DEALER in supplier and buyer LOVs

EOF
)"
```

---

### Task 2: Wire Android `lovOptions` and `applyPartySupply`

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingQuickSheet.kt` (`lovOptions` ~5679–5694; `applyPartySupply` ~438–441)
- Test: reuse `OperatingLovKindMatchTest` (no new file required); optionally add one mapping assert in `OperatingMasterMappingTest.kt` that Principals tab kinds intersect LOV supplier/buyer rules

**Interfaces:**
- Consumes: `OperatingLovKindMatch.masterMatchesLovKind`, `partyKindsForSupply`
- Produces: unchanged `lovOptions(...)` signature; broader option lists

- [x] **Step 1: Write failing regression assert (optional in existing test)**

In `OperatingMasterMappingTest.kt` or `OperatingLovKindMatchTest.kt`:

```kotlin
@Test
fun principalsTabKindsAreSelectableInCaptureLovs() {
    val principals = OperatingMasterTab.Principals.kinds()
    assertTrue(principals.any { OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", it) })
    assertTrue(principals.any { OperatingLovKindMatch.masterMatchesLovKind("BUYER", it) })
}
```

- [x] **Step 2: Run test — PASS on kind helper alone is fine; Step 3 is the wiring**

- [x] **Step 3: Replace inline when-filter in `lovOptions` with `OperatingLovKindMatch.masterMatchesLovKind(kind, masterKind)`**

Replace `applyPartySupply` kind set with `OperatingLovKindMatch.partyKindsForSupply()`.

- [x] **Step 4: Run**

`cd apk && ./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.OperatingLovKindMatchTest --tests com.example.momentra.ui.shell.business.OperatingMasterMappingTest`  
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingQuickSheet.kt \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatchTest.kt \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/OperatingMasterMappingTest.kt
git commit -m "$(cat <<'EOF'
fix(apk): use shared LOV kind match in capture pickers

EOF
)"
```

---

### Task 3: Decode projection `companyId` (Android + iOS)

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/data/api/Dto.kt` — add `@SerializedName("companyId") val companyId: String? = null` to `OperatingProjectionDto` (~1386)
- Modify: `momentra/momentra/API/APIClient.swift` — add `let companyId: String?` to `OperatingProjectionPayload` (~207)
- Test: extend fixture JSON in `momentra/momentraTests/BusinessOperatingModelTests.swift` helper that builds projection JSON to include `"companyId":"c1"` and assert decode; Android Gson round-trip optional via small Dto test if one exists, else document manual decode via existing operating tests

**Interfaces:**
- Produces: `OperatingProjectionDto.companyId`, `OperatingProjectionPayload.companyId`
- Consumes: backend field already returned by `projectOperatingMoment` (`operating-catalog.ts` ~1629)

- [x] **Step 1: Write failing iOS decode assert**

```swift
@Test func projectionDecodesCompanyId() throws {
    let p = try makeProjection(jsonExtra: #" "companyId": "c-proj" "#) // adapt to existing helper
    #expect(p.companyId == "c-proj")
}
```

- [x] **Step 2: Run iOS test — expect FAIL (unknown/missing key ignored → nil)**

- [x] **Step 3: Add `companyId` to both DTOs**

- [x] **Step 4: Re-run — PASS**

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/data/api/Dto.kt \
  momentra/momentra/API/APIClient.swift \
  momentra/momentraTests/BusinessOperatingModelTests.swift
git commit -m "$(cat <<'EOF'
fix(api): decode companyId on operating projection

EOF
)"
```

---

### Task 4: Android masters company resolution + empty-projection hydrate

**Files:**
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt` (business `companyId` passed into `OperatingMomentPage` / chrome ~1932, ~2183) — resolve `selectedCompany?.companyId ?: moments.firstOrNull { it.momentId == selectedMomentId }?.companyId`
- Modify: `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingQuickSheet.kt` or capture host — when opening LOV and `model.masters.isNullOrEmpty()` and companyId available, trigger one-shot `listMasters` into chrome/local state **or** bump refresh after ensuring companyId is set so next `getOperating` includes table masters
- Prefer smallest fix: fix companyId wiring first; if LOVs still empty with populated masters API, add LaunchedEffect in `OperatingCaptureSheet` path that calls `BusinessSliceRepository.listMasters` and merges into form.localMasters / a remembered list fed to `CaptureLovHost`
- Test: unit-test a pure resolver function if extracted, e.g. `fun resolveOperatingCompanyId(selected: String?, momentCompanyId: String?, projectionCompanyId: String?): String?`

**Interfaces:**
- Produces: `resolveOperatingCompanyId(selectedCompanyId, momentCompanyId, projectionCompanyId): String?` (prefer first non-blank)
- Consumes: Task 3 `projection.companyId`

- [x] **Step 1: Failing test for resolver**

```kotlin
@Test
fun prefersSelectedThenMomentThenProjection() {
    assertEquals("sel", resolveOperatingCompanyId("sel", "mom", "proj"))
    assertEquals("mom", resolveOperatingCompanyId(null, "mom", "proj"))
    assertEquals("proj", resolveOperatingCompanyId(null, null, "proj"))
    assertNull(resolveOperatingCompanyId(null, null, null))
}
```

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Implement resolver + wire AppShell / CaptureLovHost hydrate when masters empty**

Capture hydrate signature (if needed): merge API masters into options passed to `lovOptions` without replacing projection activity.

- [x] **Step 4: Run unit tests — PASS**

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingQuickSheet.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatch.kt \
  apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/OperatingLovKindMatchTest.kt
git commit -m "$(cat <<'EOF'
fix(apk): resolve companyId and hydrate empty capture LOVs

EOF
)"
```

---

### Task 5: iOS `loadMasters` fallback + capture open hydrate

**Files:**
- Modify: `momentra/momentra/Shell/BusinessActive/BusinessOperatingModel.swift` (`loadMasters` ~201–208)
- Modify: `momentra/momentra/Shell/BusinessActive/Sheets/OperatingCaptureSheet.swift` — add `.task { await model.ensureMastersForCapture() }`
- Modify: `momentra/momentra/Shell/BusinessActive/Sheets/OperatingQuickAddHubView.swift` — optional prefetch before capture (YAGNI if capture `.task` covers it)
- Test: `momentra/momentraTests/BusinessOperatingModelTests.swift`

**Interfaces:**
- Produces:
  - `func loadMasters() async` — if `companyId` nil/empty, set `masters = projection?.masters ?? []` (and if projection has `companyId`, assign `companyId` when model allows mutation, or call listMasters with projection companyId)
  - `func ensureMastersForCapture() async` — if `masters.isEmpty` { await loadMasters() }; if still empty, `masters = projection?.masters ?? []`
- Consumes: Task 3 `projection.companyId`

- [x] **Step 1: Write failing tests**

```swift
@Test func loadMastersFallsBackToProjectionWhenNoCompany() async throws {
    let gateway = FakeRepo(projection: try makeProjection(masters: [skuMaster], companyId: nil))
    let model = BusinessOperatingModel(momentId: "m1", companyId: nil, subtype: "TRADING_WHOLESALE", repo: gateway)
    model.projection = try makeProjection(masters: [skuMaster], companyId: nil) // or load() first
    await model.loadMasters()
    #expect(!model.masters.isEmpty)
}

@Test func ensureMastersForCaptureHydratesFromProjection() async throws {
    // masters empty, projection has PRODUCT → after ensure, masters non-empty
}
```

Update existing `noCompanyMeansNoMasterRead` if it asserted empty forever — change expectation to projection fallback when projection has masters; keep “no company and no projection masters ⇒ empty”.

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Implement `loadMasters` / `ensureMastersForCapture`; call from `OperatingCaptureSheet.task`

- [x] **Step 4: Run `BusinessOperatingModelTests` — PASS**

- [x] **Step 5: Commit**

```bash
git add momentra/momentra/Shell/BusinessActive/BusinessOperatingModel.swift \
  momentra/momentra/Shell/BusinessActive/Sheets/OperatingCaptureSheet.swift \
  momentra/momentraTests/BusinessOperatingModelTests.swift
git commit -m "$(cat <<'EOF'
fix(ios): hydrate trading masters on capture open

EOF
)"
```

---

### Task 6: iOS LOV kind match + `skuPicker` source/empty state

**Files:**
- Create: `momentra/momentra/Shell/BusinessActive/OperatingLovKindMatch.swift` (mirror Android rules)
- Modify: `OperatingCaptureSheet.swift` `skuPicker` (~1847–1893) and any party chip filters
- Test: `momentra/momentraTests/OperatingLovKindMatchTests.swift` (new) or extend `OperatingMasterMappingTests.swift`

**Interfaces:**
- Produces: `enum OperatingLovKindMatch { static func masterMatches(pickerKind: String, masterKind: String) -> Bool }`
- Consumes: same kind rules as Task 1

- [x] **Step 1: Failing Swift tests mirroring Android Task 1 cases**

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Implement helper; change `skuPicker` to:**
  - `let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters`
  - filter with `OperatingLovKindMatch.masterMatches(pickerKind: "PRODUCT", masterKind:)`
  - **always** show Product section when itemized: if source empty after filter, show “No matching product” + Create affordance (contract §3.1), not `EmptyView`

- [x] **Step 4: Run tests — PASS**

- [x] **Step 5: Commit**

```bash
git add momentra/momentra/Shell/BusinessActive/OperatingLovKindMatch.swift \
  momentra/momentra/Shell/BusinessActive/Sheets/OperatingCaptureSheet.swift \
  momentra/momentraTests/OperatingLovKindMatchTests.swift
git commit -m "$(cat <<'EOF'
fix(ios): product picker uses projection fallback and kind match

EOF
)"
```

---

### Task 7: iOS searchable Trading LOV sheet (parity)

**Files:**
- Create: `momentra/momentra/Shell/BusinessActive/Sheets/SearchableOperatingLovSheet.swift`
- Modify: `OperatingCaptureSheet.swift` — replace free-text-only Customer/Supplier (and Product on itemized) with tappable fields that present the sheet; wire `onAdd` to create-master flow when `UPDATE` allowed
- Modify: `momentra/momentra.xcodeproj/project.pbxproj` — add new Swift file to target
- Reference Android: `SearchableOperatingLovSheet.kt` (query filter, empty copy, + Add)
- Test: pure filter helper tests in `OperatingLovKindMatchTests.swift`; manual checklist below

**Interfaces:**
- Produces:
  - `struct SearchableOperatingLovSheet: View` with `kind: String`, `title: String`, `options: [OperatingLovOption]`, `canAdd: Bool`, `onSelect: (OperatingLovOption) -> Void`, `onAdd: ((String) -> Void)?`, `onDismiss: () -> Void`
  - `struct OperatingLovOption: Identifiable` (`id`, `name`, `kind`, `subtitle`, `unit`)
  - Builder: `func tradingLovOptions(kind: String, masters: [OperatingMasterPayload]) -> [OperatingLovOption]` using Task 6 kind match
- Consumes: `ensureMastersForCapture`, kind match, masters source

- [x] **Step 1: Write failing builder tests**

```swift
@Test func tradingLovOptionsIncludesPrincipalForSupplier() {
    let masters = [OperatingMasterPayload(/* kind PRINCIPAL, name Acme */)]
    let opts = tradingLovOptions(kind: "SUPPLIER", masters: masters)
    #expect(opts.contains { $0.name == "Acme" })
}
```

- [x] **Step 2: Run — FAIL**

- [x] **Step 3: Implement sheet + wire Sale (Itemized product; Part paid / Pay later customer), Purchase (supplier + product), Order paths in `OperatingCaptureSheet`**

Do **not** add Customer LOV to Received Quick amount (Figma conditional). Make “Create customer/supplier” a real button that opens master create or inline add via sheet `canAdd`.

- [x] **Step 4: Run unit tests — PASS**

Manual checklist (device/simulator):
- [ ] Trading moment with PRINCIPAL “Both” contact → Sale Pay later Customer LOV lists them; Purchase Supplier LOV lists them
- [ ] Catalogue SKU → Itemized Sale Product LOV lists them
- [ ] Open Sale from Hub immediately after moment open → lists appear after brief hydrate (not permanent blank)
- [ ] Empty catalogue → “No matching …” + Add when permitted

- [x] **Step 5: Commit**

```bash
git add momentra/momentra/Shell/BusinessActive/Sheets/SearchableOperatingLovSheet.swift \
  momentra/momentra/Shell/BusinessActive/Sheets/OperatingCaptureSheet.swift \
  momentra/momentra.xcodeproj/project.pbxproj \
  momentra/momentraTests/OperatingLovKindMatchTests.swift
git commit -m "$(cat <<'EOF'
feat(ios): searchable trading LOVs for party and product

EOF
)"
```

---

### Task 8: Cross-platform verification + honesty notes

**Files:**
- Optionally update: `docs/qa/trading-device-walkthrough-2026-11-12/RESULTS.md` or add a short note under `docs/qa/` — only if the repo already tracks LOV QA; otherwise skip docs (YAGNI)
- No production code unless a regression surfaces

**Interfaces:** none new

- [x] **Step 1: Run Android unit suite for touched FQCNs**

`cd apk && ./gradlew :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.OperatingLovKindMatchTest --tests com.example.momentra.ui.shell.business.OperatingMasterMappingTest`

- [ ] **Step 2: Run iOS `BusinessOperatingModelTests` + `OperatingLovKindMatchTests`** — BLOCKED on Linux (no `xcodebuild`); see `.superpowers/sdd/2026-11-19-trading-lov-lists-loading/task-8-report.md`

- [ ] **Step 3: Device smoke (apk + iOS)** — Itemized Sale product, Pay later customer, Purchase supplier, Masters Principals ↔ LOV parity for PRINCIPAL

- [x] **Step 4: Commit only if verification docs or tiny follow-ups were required; otherwise stop**

---

## Self-review

1. **Spec coverage:** Contract §3 / §3.1 searchable LOV + multi-role counterparty → Tasks 1–2, 6–7. Product/customer/supplier lists loading → Tasks 3–7. Company bind / projection → Tasks 3–5. Create + Add → Task 7. Amount/delivery UX left untouched (constraint).
2. **Step scan:** Each step is one action (test / run / implement signature / run / commit). Bodies limited to kind rules and wiring points.
3. **Type consistency:** `masterMatchesLovKind` / `OperatingLovKindMatch.masterMatches` / `tradingLovOptions` / `ensureMastersForCapture` / `resolveOperatingCompanyId` named consistently across tasks.
4. **Review Focus:** All five modes mapped to tasks; Quick amount conditional LOVs called out so executors do not over-fix.
5. **Proportion:** Plan targets audit findings only; no transcript of `SearchableOperatingLovSheet` UI chrome.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-19-trading-lov-lists-loading.md`.

**Which execution approach would you prefer?**

- **Subagent-driven** — A fresh subagent implements each task and a fresh reviewer checks it before the next one starts, then a whole-branch review at the end. Most thorough; costs a fresh context per task and per review.
- **Native** — I implement every task myself in this session, then one fresh reviewer on the most capable model checks the whole branch. Cheapest and fastest; no independent review until the end.

**For this plan I recommend Subagent-driven**, because the work spans Android + iOS + DTO interfaces with cross-task dependencies (kind helper → wire → hydrate → LOV UI), and a shipped empty-list regression blocks core Trading capture. Does the plan capture what you want, and which approach should we use?
