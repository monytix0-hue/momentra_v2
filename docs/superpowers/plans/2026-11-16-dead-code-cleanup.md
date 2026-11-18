# Dead Code & Obsolete File Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove high-confidence dead code and obsolete non-product files across the Momentra monorepo without breaking Android, iOS, backend, or admin builds.

**Architecture:** Cleanup proceeds in risk-ordered waves: (1) tracked junk / local regenerable dumps with zero runtime impact, (2) isolated backend modules and unused packages, (3) Android orphaned packages and large unused UI kits (preserving live color tokens), (4) iOS orphaned screens/repos and archive shells, (5) drawable/asset batches tied to deleted call sites, (6) final cross-platform verification. Each wave uses a fail-first “absence check” (ripgrep/assert path gone) before delete, then build/typecheck/test.

**Tech Stack:** Kotlin/Compose (apk), SwiftUI (momentra), TypeScript/Node (backend), Vite (admin-dashboard), Gradle, Xcode, npm/tsx, ripgrep for absence proofs.

**Spec:** No prior product spec. This plan is driven by the **Audit Findings** section below (repo audit 2026-11-16). Treat that section as the authoritative removal inventory; do not expand scope into refactors or “maybe unused” API types without human confirmation.

## Global Constraints

- Delete only **high-confidence** items listed in Audit Findings §A unless a task explicitly marks medium-confidence and the human has approved the “Needs human confirmation” set.
- Never delete Manifest-registered, Compose-called, or router-mounted code; never delete `ApiService` / `APIClient` hand clients; never delete widget receivers or `network_security_config`.
- When removing UI kits that still export live tokens (`OpsColors`, `RunwayColors`), **extract tokens first** into a small kept file, then delete the unused composables.
- Preserve `TeamOpsSheetChrome` / live TeamOps fields; do not delete the whole `teamops/` tree.
- Preserve OpenAPI generation pipelines (`openapi:generate*`) even if runtime clients are unused—flag `openapi-generated` / `MomentraAPI` for confirmation, not silent deletion.
- Do not commit secrets, `.env`, keystores, or regenerate Figma dumps into git.
- Prefer small commits per task; do not squash unrelated platforms into one commit.
- Absence checks must fail before delete and pass after delete.

## Review Focus

1. **False-positive dynamic use:** Android Manifest XML, Glance widgets, Gradle `keep` rules, iOS synchronized folders, and backend dynamic `import()` — expect “no Kotlin import” to still be live if Manifest/XML references it.
2. **Color-token survivors:** After Ops/Runway kit deletion, `AppShellScreen` must still compile against relocated `OpsColors` / `RunwayColors`.
3. **Cross-platform empty-state parity docs:** Removing `BusinessCreateEmpty*` / `BusinessNoMomentEmptyCopy` may desync QA CSVs that still name those files — update or note doc drift in the same task.
4. **Dependabot / root npm:** Removing `@momentra/event-contracts` and/or root `pdfkit` requires matching edits to `.github/dependabot.yml` and `package-lock.json`.
5. **Prisma / AI stubs:** Empty Prisma scaffold and `ai-context` are unused today; removing them must not assume Prisma migration is planned this sprint (YAGNI: delete scaffold, don’t replace with another ORM).

---

## Audit Findings

Audit date: **2026-11-16**. Method: entry-point tracing, ripgrep symbol/import graphs, Manifest/Gradle/Xcode project inspection, `package.json` script wiring, Compose/deploy references. Confidence: **high** = no references outside defining file / regenerable junk; **medium** = unused but may be intentional scaffolding or pipeline; **low** = template/noise.

### A. High-confidence removals (execute in tasks)

#### A1. Tracked / root obsolete files

| Path | Why it looks dead |
|------|-------------------|
| `apk/compile-out.txt`, `apk/compile-out2.txt`, `apk/compile-out3.txt` | Tracked local Gradle compile logs; not consumed by build. |
| `Untitled (300 x 300 px).png` | Root orphan image; not referenced by app/web/docs. |
| `android_trading_distribution_exploration_plan.md` | Root exploration note; superseded by `docs/qa/trading-device-walkthrough-2026-11-12/` and superpowers plans. |
| `Momentra_Personal_Functional_Design_v1_2.docx` | Root design dump; not linked from docs index; product design lives under `docs/` / Figma. |
| Root `package.json` + `package-lock.json` + root `node_modules` (pdfkit only) | Duplicate of `backend/typescript` pdfkit usage (`modules/story/pdf.ts`); Dependabot still scans `/`. |

#### A2. Local regenerable / ignored junk (delete from disk; already gitignored)

| Path | Why |
|------|-----|
| `figma_data.json`, `figma_design_data.json`, `figma_frame_data.json`, `frame-2348-29188.json` (~84MB each) | `.gitignore` “Design export dumps”; regenerable from Figma. |
| `.tmp/`, `.figma_tmp/`, `.figma-screens/`, `.figma-shots/`, `graphify-out/` | Scratch/export dirs; ignored or untracked. |
| `.dump*`, `.tmp_*`, `test.png`, `settings.json ` (trailing space) | Explicitly gitignored ad-hoc scratch. |
| Untracked root PNGs `02-services.png` … `06-room-form.png` | Ad-hoc Figma/QA screenshots at repo root. |
| `firebase-debug.log` | Ignored debug log. |
| `OpenAPI/.build/` at repo root | Stale SPM cache only (no `Package.swift`); canonical is `momentra/OpenAPI/`. |
| Empty shell `momentra.xcodeproj/` at **repo root** (xcuserdata only, no `project.pbxproj`) | Canonical project is `momentra/momentra.xcodeproj`. |
| `momentra/_archive/sync-conflicts-20261024/*.swift` (8 Syncthing conflict copies) | Outside synced app folder; zero references; live hubs use different names. |

#### A3. Backend

| Path | Why |
|------|-----|
| `backend/typescript/src/modules/finance/transaction-types.ts` | Zero imports in `src/`, tests, workers, scripts. |
| `backend/typescript/src/modules/story/artifacts.ts` | `MOMENT_STORY_ARTIFACT_TYPES` never imported; `story/service.ts` hardcodes type strings. |
| `stubMomentCommand` in `backend/typescript/src/modules/moment/service.ts` | Exported function; zero call sites repo-wide. |
| `backend/python/ai-context/context_builder.py` | No Docker COPY, no imports from `fastapi-ai` or TS; placeholder only. |
| `backend/packages/event-contracts/` | No runtime imports; not a dependency of `backend/typescript`; only Dependabot + docs mention it. |
| `backend/typescript/prisma/schema.prisma` + `@prisma/client` / `prisma` deps | Schema has no models; no `PrismaClient` usage; migrations use `frds/` + `scripts/migrate.ts`. |
| `backend/typescript/scripts/s0-perf-sample.ts`, `s9-*-measure.ts`, `s9-opt-remeasure.ts`, `s9-opt-remeasure-latest.json` | Abandoned S9 perf campaign; not in npm scripts. |
| `tools/design-pipeline/sample.mjs` | Not in `package.json` scripts or README; one-off sharp sampler. |

#### A4. Android (`apk/`) — orphaned code

| Path | Why |
|------|-----|
| `.../data/security/EnterpriseSecurityGuard.kt` | Zero usages. |
| `.../data/security/SecureClipboard.kt` | Zero usages. |
| `.../data/security/SecurityIntegrityCheck.kt` | Zero usages. |
| `.../ui/security/BiometricActionPrompt.kt` | Zero usages; app lock uses `AppLockStore` / `AppLockGate`. |
| `.../ui/security/SecureTouchModifier.kt` | No-op helper; never imported. |
| `.../ui/shared/HapticFeedbackHelper.kt` | `rememberHapticFeedback` only defined, never called. |
| `.../data/local/MomentraSyncWorker.kt` | Never enqueued via WorkManager; sync uses `OfflineOutbox`. |
| `.../ui/expense/ExpenseCreateContent.kt`, `ExpenseCreateViewModel.kt` | No navigation/host; personal expense UX elsewhere. |
| `.../data/repository/ExpenseCreateRepository.kt` | Only used by dead ViewModel. |
| `.../data/repository/PersonalActivityRepository.kt` | Only defined; activity served by `PersonalSliceRepository`. |
| `.../data/create/IdempotencyKeyStore.kt` | Only used by dead `ExpenseCreateRepository`. |
| `.../data/operation/IdempotencyStore.kt` | Zero usages. |
| `.../data/api/OpenApiGeneratedBridge.kt` | App uses hand-written `ApiService`; bridge unused at runtime. |
| `.../navigation/Routes.kt` | `Routes.HOME` never referenced. |
| `.../ui/shell/components/ShellLaunchers.kt` | Launchers defined only; never called. |
| `.../ui/shell/empty/business/BusinessCreateEmptyContent.kt` | Superseded by `ContextEmptyExperience` / `BusinessAppStateBody`. |
| `.../ui/shell/empty/business/BusinessNoMomentEmptyCopy.kt` | Enum never referenced on Android. |
| `.../ui/shell/empty/business/CompanyFlowSheets.kt`, `CompanyJoinConfirmSheet.kt`, `TradingHowItWorksSheet.kt` | Sheets never invoked. |
| `.../ui/shell/empty/group/GroupAddPeopleSheet.kt`, `GroupSetupFields.kt` | Never used. |
| `.../ui/shell/personal/life/PersonalLifeActiveContent.kt` | Never referenced; personal UI via `PersonalExperienceHost`. |
| Setup wizard scaffold set: `SetupWizardScaffold.kt`, `SetupWizardHeader.kt`, `SetupStickyFooter.kt`, `SetupPreviewCard.kt`, `SetupPrefField.kt`, `SetupChipGrid.kt` | No callers; other `ui/setup/*` helpers remain live. |
| `.../ops/components/OpsUiComponents.kt`, `OpsSheetChrome.kt` (except **keep/move `OpsColors`**) | External use is only `OpsColors` from `AppShellScreen`. |
| `.../runway/components/RunwayUiComponents.kt`, `RunwaySheetChrome.kt` (except **keep/move `RunwayColors`**) | External use is only `RunwayColors` from `AppShellScreen`. |

#### A5. iOS (`momentra/`) — orphaned code

| Path | Why |
|------|-----|
| `momentra/momentra/ContentView.swift` | Xcode Hello-World template; app entry uses `AppShellView` / onboarding. |
| `momentra/momentra/Onboarding/QuickTourView.swift`, `TourView.swift`, `TourSteps.swift`, `TourGuide.swift`, `TourStep.swift` | Tour cluster only self-references; never presented from app shell. |
| `momentra/momentra/Shell/BusinessEmpty/BusinessCreateEmptyView.swift` | Zero call sites; create flow uses `BusinessCreateMomentView`. |
| `momentra/momentra/Shell/BusinessEmpty/BusinessNoMomentEmptyCopy.swift` | Enum never referenced. |
| `momentra/momentra/Shell/ExpenseCreateView.swift`, `ExpenseCreateModel.swift` | No navigation; `ExpenseCreateRepository` stays (used by `PersonalMasterExpenseSheet`). |
| `momentra/momentra/Data/FinancialAccountCreateRepository.swift` | Gateway+impl only in-file. |
| `momentra/momentra/Data/FutureItemCreateRepository.swift` | Same. |
| `momentra/momentra/Data/LifestyleActivityCreateRepository.swift` | Same. |
| `momentra/momentra/Data/RelationshipActivityCreateRepository.swift` | Same. |
| `momentra/momentra/OpenAPI/OpenAPISyncBridge.swift` | Sole `import MomentraAPI` in app sources; no callers. |
| `momentra/momentra/Assets.xcassets/BusinessEmpty/business_empty_*.imageset` | Only referenced from dead `BusinessCreateEmptyView`. |

### B. Medium-confidence — Needs human confirmation (do not delete in Tasks 1–8 without explicit OK)

| Path / item | Why flagged | Risk if deleted wrongly |
|-------------|-------------|-------------------------|
| `apk/openapi-generated/` Gradle module | Compiled but no `generated.apis.*` imports; still contract/CI/audit surface (SUPP-009). | Breaks audit scripts / future migration. |
| `momentra/OpenAPI` SPM package link | Runtime uses `APIClient.swift`; package still linked in Xcode. | Build graph / generation workflow break. |
| `backend/typescript/scripts/audit/run-deployment-audit.ts` | npm `audit:deployment` runs Python twin only. | Loss of alternate auditor if scripts diverge. |
| S9 scripts vs keeping `s9-opt-remeasure-latest.json` as historical baseline | Campaign abandoned. | Lose perf regression evidence. |
| `memory-worker` stub | Deployed intentional no-op. | Ops/Compose expectations. |
| `operating-einvoice.ts` runtime exports (`eInvoiceProvider`, etc.) | Types used; runtime unused. | Premature removal of e-invoice seam. |
| `PersonalAssets.kt` indirection | Map unused; many `ic_pers_*` still used directly. | Asset pipeline confusion. |
| ~116 unreferenced drawable basenames | Heuristic; some may be dynamic/taxonomy keys. | Missing icons in future screens. |
| `BusinessTokens` iOS colorsets vs generated Swift tokens | Likely redundant with `BusinessTokens.generated.swift`. | Dark-mode / Figma export pipeline. |
| Template tests `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`, iOS UITest templates | Noise only. | Harmless; low value. |
| Duplicate compose / twin `.env.example` | Drift risk, not dead. | Deploy breakage. |
| Root Dependabot entry after removing root package.json | Must update together. | Dependabot noise/errors. |

### C. Explicitly NOT dead (do not touch)

- Widget stack: `MomentraWidgets.kt`, receivers, `*_widget_info.xml`, shortcuts, `network_security_config.xml`.
- `GestureModifiers.kt`, `OperatingAmountFormat.kt`, `AppLock*` path.
- `TeamOpsSheetChrome` + khata/expense sheet usage.
- Hand-written `ApiService` / iOS `APIClient`.
- `fastapi-ai` service + all six Compose workers (including stub memory-worker).
- `admin-dashboard` and `web/` invite/legal surfaces.
- `BusinessCompatibilityStubs.swift` (defines live `BusinessSetupKind`).
- Live `ui/setup/*` helpers other than the wizard scaffold set listed in §A4.

### D. Stale doc references (fix in cleanup commits when touching related code)

- Docs/audit scripts still mention removed `router-product.ts`.
- QA CSVs may still name `BusinessCreateEmptyView` / Android empty content.
- `docs/legal/LAUNCH_DECISIONS.md` Dependabot bullet lists `event-contracts` and root npm.

---

## File map (before tasks)

### Create

- `scripts/qa/assert-paths-absent.sh` — shared absence checker used by tasks (exit 1 if any listed path still exists).
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/ops/OpsColors.kt` — extracted from `OpsUiComponents.kt` (only if colors currently live inside that file).
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/runway/RunwayColors.kt` — extracted from `RunwayUiComponents.kt` (same rule).

### Modify

- `apk/.../AppShellScreen.kt` — import relocated `OpsColors` / `RunwayColors`.
- `backend/typescript/src/modules/moment/service.ts` — remove `stubMomentCommand`.
- `backend/typescript/package.json` (+ lockfile) — remove `prisma` / `@prisma/client` when scaffold deleted.
- `.github/dependabot.yml` — drop `event-contracts` and/or root npm entries when those packages go.
- `docs/legal/LAUNCH_DECISIONS.md` (Dependabot bullet) — sync with Dependabot reality.
- Any QA/audit doc that names deleted empty-state files (minimal path-only fix).

### Delete

- All paths in Audit Findings **§A** (high-confidence), in the task that owns them.
- Local §A2 dumps on executor machines (not necessarily committed deletions if never tracked).

### Do not create

- Replacement ORM, new security framework, or new OpenAPI client — YAGNI.

---

### Task 1: Absence-check helper + local regenerable junk

**Files:**
- Create: `scripts/qa/assert-paths-absent.sh`
- Delete (local/disk): §A2 paths that exist on the machine (`figma_*.json`, `frame-*.json`, `.tmp/`, `.figma_*`, `graphify-out/`, root ad-hoc PNGs, `OpenAPI/.build/`, empty root `momentra.xcodeproj/`, `momentra/_archive/sync-conflicts-20261024/`)

**Interfaces:**
- Consumes: none
- Produces: `assert-paths-absent.sh` accepting space-separated relative paths; exit 0 iff none exist; prints survivors on failure

- [x] **Step 1: Write failing absence check for a known existing dump**

```bash
# scripts/qa/assert-paths-absent.sh must exist after Step 3; for now create the script first with the contract below, then use a path that currently exists to prove FAIL.
printf '%s\n' '#!/usr/bin/env bash' 'set -euo pipefail' 'fail=0' 'for p in "$@"; do' '  if [[ -e "$p" ]]; then echo "STILL_PRESENT: $p"; fail=1; fi' 'done' 'exit $fail' > scripts/qa/assert-paths-absent.sh
chmod +x scripts/qa/assert-paths-absent.sh
```

- [x] **Step 2: Run against an existing junk path to verify FAIL**

Run: `bash scripts/qa/assert-paths-absent.sh figma_data.json` (or another §A2 path that exists)
Expected: FAIL / exit 1 with `STILL_PRESENT: ...`

- [x] **Step 3: Delete §A2 local regenerable junk (disk only)**

Remove existing §A2 directories/files. Skip missing paths. Do not `git add -f` ignored dumps.

- [x] **Step 4: Re-run absence check for deleted paths**

Run: `bash scripts/qa/assert-paths-absent.sh figma_data.json figma_design_data.json figma_frame_data.json frame-2348-29188.json OpenAPI/.build momentra.xcodeproj momentra/_archive/sync-conflicts-20261024`
Expected: exit 0 (or only report paths that were never present — treat as OK)

- [x] **Step 5: Commit helper only**

```bash
git add scripts/qa/assert-paths-absent.sh
git commit -m "$(cat <<'EOF'
chore: add path-absence helper for dead-code cleanup waves

EOF
)"
```

---

### Task 2: Remove tracked root/APK junk artifacts

**Files:**
- Delete: `apk/compile-out.txt`, `apk/compile-out2.txt`, `apk/compile-out3.txt`, `Untitled (300 x 300 px).png`, `android_trading_distribution_exploration_plan.md`, `Momentra_Personal_Functional_Design_v1_2.docx`
- Modify (optional hygiene): `.gitignore` — add `apk/compile-out*.txt` if not covered by `*.log`

**Interfaces:**
- Consumes: `scripts/qa/assert-paths-absent.sh`
- Produces: those paths absent from git tree

- [x] **Step 1: Write failing check that listed tracked junk still exists**

Run: `bash scripts/qa/assert-paths-absent.sh apk/compile-out.txt "Untitled (300 x 300 px).png" android_trading_distribution_exploration_plan.md Momentra_Personal_Functional_Design_v1_2.docx`
Expected: FAIL (paths present)

- [x] **Step 2: Delete the files and stop tracking**

```bash
git rm -f apk/compile-out.txt apk/compile-out2.txt apk/compile-out3.txt "Untitled (300 x 300 px).png" android_trading_distribution_exploration_plan.md Momentra_Personal_Functional_Design_v1_2.docx
```

- [x] **Step 3: Absence check passes**

Run: same command as Step 1
Expected: exit 0

- [x] **Step 4: Commit**

```bash
git add -u apk/ .gitignore
git commit -m "$(cat <<'EOF'
chore: remove tracked compile logs and obsolete root design dumps

EOF
)"
```

---

### Task 3: Drop unused root npm pdfkit duplicate

**Files:**
- Delete: root `package.json`, `package-lock.json` (after confirming backend owns pdfkit)
- Modify: `.github/dependabot.yml` — remove `directory: "/"` npm ecosystem entry
- Modify: `docs/legal/LAUNCH_DECISIONS.md` Dependabot bullet — drop “root”
- Disk: remove root `node_modules/` if present

**Interfaces:**
- Consumes: `backend/typescript/package.json` must still list `pdfkit` (used by `modules/story/pdf.ts`)
- Produces: no root npm package; Dependabot no longer scans `/` for npm

- [x] **Step 1: Failing assertion — root package.json still present and Dependabot still lists `/`**

```bash
test -f package.json && rg -q 'directory: "/"' .github/dependabot.yml
# Expected: both true (precondition). Then invent the post-delete check that should fail later:
bash -c 'test ! -f package.json' ; echo "expect fail while package.json exists"
```

Expected: `test ! -f package.json` fails while file exists.

- [x] **Step 2: Verify pdfkit lives in backend**

Run: `rg -n '"pdfkit"' backend/typescript/package.json`
Expected: match present

- [x] **Step 3: Remove root package files + Dependabot root npm + doc bullet; delete root node_modules**

- [x] **Step 4: Post-checks**

Run:
```bash
test ! -f package.json
! rg -n 'directory: "/"' .github/dependabot.yml | rg npm -B3
rg -n 'pdfkit' backend/typescript/package.json
```
Expected: root package gone; Dependabot root npm entry gone; backend pdfkit remains

- [x] **Step 5: Commit**

```bash
git add -u package.json package-lock.json .github/dependabot.yml docs/legal/LAUNCH_DECISIONS.md
git commit -m "$(cat <<'EOF'
chore: remove unused root pdfkit package and Dependabot root npm entry

EOF
)"
```

---

### Task 4: Backend dead modules + stub export

**Files:**
- Delete: `backend/typescript/src/modules/finance/transaction-types.ts`, `backend/typescript/src/modules/story/artifacts.ts`
- Modify: `backend/typescript/src/modules/moment/service.ts` — delete `stubMomentCommand` function only
- Test: `backend/typescript/tests/` via `npm test` (existing)

**Interfaces:**
- Consumes: none from prior tasks
- Produces: those modules/exports absent; story/finance services unchanged externally

- [x] **Step 1: Failing absence/import checks**

```bash
bash scripts/qa/assert-paths-absent.sh \
  backend/typescript/src/modules/finance/transaction-types.ts \
  backend/typescript/src/modules/story/artifacts.ts
# Expected: FAIL (files present)
rg -n 'stubMomentCommand' backend/typescript/src/modules/moment/service.ts
# Expected: match (still present)
```

- [x] **Step 2: Delete the two modules; remove `stubMomentCommand` from `service.ts`**

- [x] **Step 3: Absence + symbol checks**

```bash
bash scripts/qa/assert-paths-absent.sh \
  backend/typescript/src/modules/finance/transaction-types.ts \
  backend/typescript/src/modules/story/artifacts.ts
! rg -n 'stubMomentCommand' backend/typescript
```
Expected: exit 0 / no matches

- [x] **Step 4: Typecheck + unit tests**

```bash
cd backend/typescript && npm run build && npm test
```
Expected: `tsc` OK; tests PASS

- [x] **Step 5: Commit**

```bash
git add -u backend/typescript/src/modules/finance/transaction-types.ts \
  backend/typescript/src/modules/story/artifacts.ts \
  backend/typescript/src/modules/moment/service.ts
git commit -m "$(cat <<'EOF'
chore: remove unused finance/story modules and stubMomentCommand

EOF
)"
```

---

### Task 5: Backend unused package scaffolds (event-contracts, prisma, ai-context, S9 scripts, sample.mjs)

**Files:**
- Delete: `backend/packages/event-contracts/` (entire package)
- Delete: `backend/typescript/prisma/schema.prisma` (and empty `prisma/` dir)
- Delete: `backend/python/ai-context/` (entire dir)
- Delete: `backend/typescript/scripts/s0-perf-sample.ts`, `s9-a-baseline-measure.ts`, `s9-b-remeasure.ts`, `s9-c-cold-measure.ts`, `s9-c-remeasure.ts`, `s9-fj-personal-measure.ts`, `s9-gh-scale-measure.ts`, `s9-opt-remeasure.ts`, `s9-opt-remeasure-latest.json`
- Delete: `tools/design-pipeline/sample.mjs`
- Modify: `backend/typescript/package.json` + lockfile — remove `prisma` and `@prisma/client`
- Modify: `.github/dependabot.yml` — remove `event-contracts` npm directory
- Modify: `docs/legal/LAUNCH_DECISIONS.md` — drop event-contracts Dependabot mention

**Interfaces:**
- Consumes: Task 3 Dependabot editing pattern
- Produces: no unused Prisma/event-contracts/ai-context/S9 artifacts in tree

- [x] **Step 1: Failing absence check on package paths**

```bash
bash scripts/qa/assert-paths-absent.sh \
  backend/packages/event-contracts/package.json \
  backend/typescript/prisma/schema.prisma \
  backend/python/ai-context/context_builder.py \
  tools/design-pipeline/sample.mjs
```
Expected: FAIL

- [x] **Step 2: Delete packages/files; strip Prisma deps; update Dependabot + LAUNCH_DECISIONS**

- [x] **Step 3: Absence + lockfile sanity**

```bash
bash scripts/qa/assert-paths-absent.sh \
  backend/packages/event-contracts/package.json \
  backend/typescript/prisma/schema.prisma \
  backend/python/ai-context/context_builder.py \
  tools/design-pipeline/sample.mjs \
  backend/typescript/scripts/s9-opt-remeasure.ts
cd backend/typescript && npm install && ! rg -n '"prisma"|"@prisma/client"' package.json
```
Expected: paths gone; prisma deps absent

- [x] **Step 4: Build + test backend**

```bash
cd backend/typescript && npm run build && npm test
```
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add -u backend/packages/event-contracts backend/typescript backend/python/ai-context \
  tools/design-pipeline/sample.mjs .github/dependabot.yml docs/legal/LAUNCH_DECISIONS.md
git commit -m "$(cat <<'EOF'
chore: drop unused event-contracts, Prisma scaffold, ai-context, and S9 scripts

EOF
)"
```

---

### Task 6: Android — unused security/worker/helpers island

**Files:**
- Delete:
  - `apk/app/src/main/java/com/example/momentra/data/security/EnterpriseSecurityGuard.kt`
  - `apk/app/src/main/java/com/example/momentra/data/security/SecureClipboard.kt`
  - `apk/app/src/main/java/com/example/momentra/data/security/SecurityIntegrityCheck.kt`
  - `apk/app/src/main/java/com/example/momentra/ui/security/BiometricActionPrompt.kt`
  - `apk/app/src/main/java/com/example/momentra/ui/security/SecureTouchModifier.kt`
  - `apk/app/src/main/java/com/example/momentra/ui/shared/HapticFeedbackHelper.kt`
  - `apk/app/src/main/java/com/example/momentra/data/local/MomentraSyncWorker.kt`
- Modify (only if ProGuard/R8 keep rules reference `MomentraSyncWorker`): matching `proguard-rules.pro` lines

**Interfaces:**
- Consumes: none
- Produces: security stubs + sync worker absent; `AppLock*` untouched

- [x] **Step 1: Failing absence check**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/app/src/main/java/com/example/momentra/data/security/EnterpriseSecurityGuard.kt \
  apk/app/src/main/java/com/example/momentra/data/local/MomentraSyncWorker.kt \
  apk/app/src/main/java/com/example/momentra/ui/shared/HapticFeedbackHelper.kt
```
Expected: FAIL

- [x] **Step 2: Delete the seven files; scrub keep-rules if present**

- [x] **Step 3: Absence + symbol scan**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/app/src/main/java/com/example/momentra/data/security/EnterpriseSecurityGuard.kt \
  apk/app/src/main/java/com/example/momentra/data/security/SecureClipboard.kt \
  apk/app/src/main/java/com/example/momentra/data/security/SecurityIntegrityCheck.kt \
  apk/app/src/main/java/com/example/momentra/ui/security/BiometricActionPrompt.kt \
  apk/app/src/main/java/com/example/momentra/ui/security/SecureTouchModifier.kt \
  apk/app/src/main/java/com/example/momentra/ui/shared/HapticFeedbackHelper.kt \
  apk/app/src/main/java/com/example/momentra/data/local/MomentraSyncWorker.kt
! rg -n 'EnterpriseSecurityGuard|MomentraSyncWorker|rememberHapticFeedback' apk/app/src --glob '*.kt'
```
Expected: exit 0 / no matches

- [x] **Step 4: Compile Android app**

```bash
cd apk && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [x] **Step 5: Commit**

```bash
git add -u apk/app/src/main/java/com/example/momentra/data/security \
  apk/app/src/main/java/com/example/momentra/ui/security \
  apk/app/src/main/java/com/example/momentra/ui/shared/HapticFeedbackHelper.kt \
  apk/app/src/main/java/com/example/momentra/data/local/MomentraSyncWorker.kt
git commit -m "$(cat <<'EOF'
chore(android): remove unused security stubs and unwired sync worker

EOF
)"
```

---

### Task 7: Android — expense-create island + orphan repos/routes/launchers/empty sheets/setup scaffolds

**Files:**
- Delete (expense island):
  - `.../ui/expense/ExpenseCreateContent.kt`
  - `.../ui/expense/ExpenseCreateViewModel.kt`
  - `.../data/repository/ExpenseCreateRepository.kt`
  - `.../data/repository/PersonalActivityRepository.kt`
  - `.../data/create/IdempotencyKeyStore.kt`
  - `.../data/operation/IdempotencyStore.kt`
- Delete (orphans):
  - `.../data/api/OpenApiGeneratedBridge.kt`
  - `.../navigation/Routes.kt`
  - `.../ui/shell/components/ShellLaunchers.kt`
  - `.../ui/shell/empty/business/BusinessCreateEmptyContent.kt`
  - `.../ui/shell/empty/business/BusinessNoMomentEmptyCopy.kt`
  - `.../ui/shell/empty/business/CompanyFlowSheets.kt`
  - `.../ui/shell/empty/business/CompanyJoinConfirmSheet.kt`
  - `.../ui/shell/empty/business/TradingHowItWorksSheet.kt`
  - `.../ui/shell/empty/group/GroupAddPeopleSheet.kt`
  - `.../ui/shell/empty/group/GroupSetupFields.kt`
  - `.../ui/shell/personal/life/PersonalLifeActiveContent.kt`
  - `.../ui/setup/SetupWizardScaffold.kt`
  - `.../ui/setup/SetupWizardHeader.kt`
  - `.../ui/setup/SetupStickyFooter.kt`
  - `.../ui/setup/SetupPreviewCard.kt`
  - `.../ui/setup/SetupPrefField.kt`
  - `.../ui/setup/SetupChipGrid.kt`
- Do **not** delete other `ui/setup/*` live helpers

**Interfaces:**
- Consumes: Task 6 compile baseline
- Produces: orphan Compose/repo files gone; personal expense via `PersonalExperienceHost` unchanged

- [x] **Step 1: Failing absence check on key island paths**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/app/src/main/java/com/example/momentra/ui/expense/ExpenseCreateContent.kt \
  apk/app/src/main/java/com/example/momentra/navigation/Routes.kt \
  apk/app/src/main/java/com/example/momentra/ui/setup/SetupWizardScaffold.kt
```
Expected: FAIL

- [x] **Step 2: Delete all Task 7 paths listed above**

- [x] **Step 3: Absence + compile**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/app/src/main/java/com/example/momentra/ui/expense/ExpenseCreateContent.kt \
  apk/app/src/main/java/com/example/momentra/data/repository/ExpenseCreateRepository.kt \
  apk/app/src/main/java/com/example/momentra/navigation/Routes.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/empty/business/BusinessCreateEmptyContent.kt \
  apk/app/src/main/java/com/example/momentra/ui/setup/SetupWizardScaffold.kt
cd apk && ./gradlew :app:compileDebugKotlin
```
Expected: absence OK; BUILD SUCCESSFUL

- [x] **Step 4: Commit**

```bash
git add -u apk/app/src/main/java/com/example/momentra
git commit -m "$(cat <<'EOF'
chore(android): remove orphan expense-create island and unused empty/setup scaffolds

EOF
)"
```

---

### Task 8: Android — extract Ops/Runway colors then delete unused UI kits

**Files:**
- Create (if colors currently live inside kit files): `.../ops/OpsColors.kt`, `.../runway/RunwayColors.kt`
- Modify: `apk/.../ui/shell/AppShellScreen.kt` — imports
- Delete: `OpsUiComponents.kt`, `OpsSheetChrome.kt`, `RunwayUiComponents.kt`, `RunwaySheetChrome.kt`
- Do **not** delete `teamops/` chrome used by khata/expense

**Interfaces:**
- Consumes: `OpsColors` / `RunwayColors` API as currently used by `AppShellScreen` (same names/types)
- Produces: kits gone; colors still importable

- [x] **Step 1: Capture current color usages (fail if AppShellScreen would break)**

```bash
rg -n 'OpsColors|RunwayColors' apk/app/src/main/java --glob '*.kt'
```
Expected: matches in `AppShellScreen.kt` and kit files

- [x] **Step 2: Extract color objects into standalone files; update imports in `AppShellScreen.kt`**

- [x] **Step 3: Delete the four kit files**

- [x] **Step 4: Absence + compile**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/ops/components/OpsUiComponents.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/ops/components/OpsSheetChrome.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/runway/components/RunwayUiComponents.kt \
  apk/app/src/main/java/com/example/momentra/ui/shell/business/runway/components/RunwaySheetChrome.kt
rg -n 'OpsColors|RunwayColors' apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt
cd apk && ./gradlew :app:compileDebugKotlin
```
Expected: kits absent; AppShellScreen still references colors; BUILD SUCCESSFUL

- [x] **Step 5: Commit**

```bash
git add apk/app/src/main/java/com/example/momentra/ui/shell
git commit -m "$(cat <<'EOF'
chore(android): keep Ops/Runway colors and delete unused UI component kits

EOF
)"
```

---

### Task 9: iOS — delete orphaned screens, tour cluster, create-repos, OpenAPI bridge, BusinessEmpty assets

**Files:**
- Delete Swift listed in §A5
- Delete `momentra/momentra/Assets.xcassets/BusinessEmpty/business_empty_*.imageset`
- Keep: `ExpenseCreateRepository`, `BusinessCompatibilityStubs.swift`, widget extension

**Interfaces:**
- Consumes: none
- Produces: orphan Swift/assets gone; synchronized folder still builds

- [x] **Step 1: Failing absence check**

```bash
bash scripts/qa/assert-paths-absent.sh \
  momentra/momentra/ContentView.swift \
  momentra/momentra/Onboarding/QuickTourView.swift \
  momentra/momentra/Shell/ExpenseCreateView.swift \
  momentra/momentra/OpenAPI/OpenAPISyncBridge.swift
```
Expected: FAIL

- [x] **Step 2: Delete §A5 Swift files + BusinessEmpty imagesets**

- [x] **Step 3: Symbol scan**

```bash
bash scripts/qa/assert-paths-absent.sh \
  momentra/momentra/ContentView.swift \
  momentra/momentra/Onboarding/QuickTourView.swift \
  momentra/momentra/Onboarding/TourView.swift \
  momentra/momentra/Onboarding/TourSteps.swift \
  momentra/momentra/Onboarding/TourGuide.swift \
  momentra/momentra/Onboarding/TourStep.swift \
  momentra/momentra/Shell/BusinessEmpty/BusinessCreateEmptyView.swift \
  momentra/momentra/Shell/BusinessEmpty/BusinessNoMomentEmptyCopy.swift \
  momentra/momentra/Shell/ExpenseCreateView.swift \
  momentra/momentra/Shell/ExpenseCreateModel.swift \
  momentra/momentra/Data/FinancialAccountCreateRepository.swift \
  momentra/momentra/Data/FutureItemCreateRepository.swift \
  momentra/momentra/Data/LifestyleActivityCreateRepository.swift \
  momentra/momentra/Data/RelationshipActivityCreateRepository.swift \
  momentra/momentra/OpenAPI/OpenAPISyncBridge.swift
! rg -n 'BusinessCreateEmptyView|QuickTourView|OpenAPISyncBridge|ExpenseCreateView\b' momentra/momentra --glob '*.swift'
```
Expected: paths/symbols gone

- [x] **Step 4: Build iOS (on macOS agent)**

```bash
cd momentra && xcodebuild -project momentra.xcodeproj -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16' -quiet build
```
Expected: BUILD SUCCEEDED  
If executor is Linux-only: skip compile, record “iOS build deferred to Mac”, but still commit deletions after Step 3 symbol scan.

- [x] **Step 5: Commit**

```bash
git add -u momentra/momentra
git commit -m "$(cat <<'EOF'
chore(ios): remove unused tour, empty-state, expense screen, and create-repo stubs

EOF
)"
```

---

### Task 10: Final verification + optional medium-confidence gate

**Files:**
- Modify: none required unless Step 3 finds broken docs
- Optional (human-approved only): items from Audit Findings §B

**Interfaces:**
- Consumes: all prior task outputs
- Produces: green verification log; open questions list for §B

- [x] **Step 1: Backend verify**

```bash
cd backend/typescript && npm run build && npm test
```
Expected: PASS

- [x] **Step 2: Android verify**

```bash
cd apk && ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL (unit tests may include remaining live tests; template tests OK if still present)

- [x] **Step 3: Repo-wide absence smoke for high-confidence set**

```bash
bash scripts/qa/assert-paths-absent.sh \
  apk/compile-out.txt \
  backend/packages/event-contracts/package.json \
  backend/typescript/prisma/schema.prisma \
  backend/python/ai-context/context_builder.py \
  backend/typescript/src/modules/finance/transaction-types.ts \
  apk/app/src/main/java/com/example/momentra/data/security/EnterpriseSecurityGuard.kt \
  apk/app/src/main/java/com/example/momentra/ui/expense/ExpenseCreateContent.kt \
  momentra/momentra/ContentView.swift \
  momentra/momentra/OpenAPI/OpenAPISyncBridge.swift
```
Expected: exit 0

- [x] **Step 4: Confirm live surfaces still exist**

```bash
test -f apk/app/src/main/java/com/example/momentra/ui/widget/MomentraWidgets.kt
test -f apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt
test -f momentra/momentra/API/APIClient.swift
rg -n 'OpsColors|RunwayColors' apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt
```
Expected: all succeed

- [x] **Step 5: Stop — ask human about §B medium-confidence items before any further deletes**

Do not delete `apk/openapi-generated`, SPM OpenAPI package, drawables batch, or `run-deployment-audit.ts` in this task.

- [x] **Step 6: Commit only if Step 3/4 required doc fixes; otherwise no empty commit**

---

## Self-review (plan vs audit)

1. **Spec coverage:** §A1–A5 each map to Tasks 1–9; §B gated in Task 10; §C protected by Review Focus + Task 10 Step 4.
2. **Step scan:** Each step is one action (fail check → delete → pass check → build → commit). No TBD bodies.
3. **Type consistency:** Color extraction keeps `OpsColors` / `RunwayColors` names used by `AppShellScreen`.
4. **Review Focus:** Dynamic/Manifest false positives, color survivors, Dependabot, Prisma/AI scaffolds, and doc drift each have an owning check (Tasks 3, 5, 8, 10).
5. **Proportion:** Inventory lives in Audit Findings; tasks name paths/commands without transcribing file bodies.
6. **DRY/YAGNI/commits:** Shared `assert-paths-absent.sh`; no replacement frameworks; one commit per task wave.

**Gaps accepted intentionally:** Unused drawable mass-delete and OpenAPI module removal deferred to human confirmation (§B) to avoid shipping broken icons or audit regressions.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-16-dead-code-cleanup.md`.

**Which execution approach would you prefer?**

- **Subagent-driven** — A fresh subagent implements each task and a fresh reviewer checks it before the next one starts, then a whole-branch review at the end. Most thorough; costs a fresh context per task and per review.
- **Native** — One session implements every task, then one fresh reviewer checks the whole branch. Cheapest and fastest; no independent review until the end.

**For this plan I recommend Subagent-driven**, because tasks span four platforms with high false-positive risk (Manifest/XML, token extraction, Dependabot lockfiles) where an independent reviewer per wave is cheaper than a mistaken mass delete. Does the plan capture what you want, and which approach should we use?
