# Momentra iOS — Device QA & Figma Parity Report

**Date:** 2026-08-27  
**Device:** Santosh’s iPhone 14 Plus (`00008110-00016CAA2E29401E`)  
**Bundle:** `resolvingpoint.momentra`  
**Figma:** [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra) (`TzLvwVwlPbeVB8ug1zB3GM`)

> **Canvas copy:** Cursor also keeps a live canvas at  
> `~/.cursor/projects/Volumes-coding-momentra-v2/canvases/ios-device-qa-figma-report.canvas.tsx`  
> (not under the git repo root — that path is intentional for Cursor Canvases).

---

## Test method limits

Full tap-through of every screen on the physical phone is **not** available from the agent (no UI automation / device screenshot pipeline). This report combines:

1. Successful device **build + install + launch**
2. Complete **code/route inventory**
3. Project Figma mapping docs + MCP design samples
4. Known API / Coming Soon gaps

**Repo policy:** Do not mark Figma visual **MATCHED** without side-by-side Figma-node screenshot vs device screenshot (`PHASE_5_EMPTY_STATE_FIGMA_MAPPING.md`).

---

## 1. Device / runtime readiness

| Check | Result | Notes |
|-------|--------|-------|
| xcodebuild iphoneos | **Working** | `BUILD SUCCEEDED` for connected iPhone 14 Plus |
| Install + launch | **Working** | `devicectl` install + process launch succeeded |
| API base URL | **Partial** | Info.plist → `https://veggie-handmade-splashed.ngrok-free.dev/` (must be live for bootstrap) |
| Auth providers | **Working** | Email / phone / Apple / Google coded; enable in Firebase Console |
| Contacts 2759 | **Partial** | Off-main via `GroupContactsLoader`; re-verify on Add People after clean install |
| Interactive UI crawl | **Untested UI** | Needs manual or XCUITest pass |

---

## 2. What’s working (wired & shippable UI)

### Auth & shell
- Splash → Onboarding carousel → Consent → Login
- App Lock PIN / biometrics gate
- 4 contexts × 5 tabs (Pulse / Moments / Quickadds / Life / Memory)
- Top bar: wordmark, + New Moment, Life360 sheet, Account
- Offline / error / forbidden panels

### Personal
- Empty: Pulse / Moments / Life / Memory (Figma-composed bodies)
- Create chooser → 4 long-form setups → `POST /v1/moments`
- Active Pulse / Moments / Memory by family (Life Ops, Future, Lifestyle, Relationships)
- Quick Add hub + expense / recovery / mood / family sheets
- Life active reads `GET /v1/personal/life` (may be seeded)

### Group
- Empty Pulse/Moments/Life/Memory + QR join
- Create: Experience / Purchase / Living setups
- Add People sheet (contacts + invite/QR)
- Active Pulse/Moments + expense & contribution sheets
- Invite mint API wired; redeem via QR/deep link path

### Business
- Empty Pulse/Moments/Life/Memory
- Company 4-step setup when no company
- Create Moment: Team Ops / Runway / Business Ops wizards
- Active Pulse/Moments + Quick Add sheet (expense/revenue/invoice)
- Company chip in top bar when selected

---

## 3. Partial / not working / Coming Soon

| Area | Status | Detail |
|------|--------|--------|
| Circle context | Coming soon | `CircleComingSoonView` (Figma `1075:7556`) — no Circle API |
| Life360 top-bar | Coming soon | `Life360ComingSoonView` (`1075:7637`) — no `/life360` |
| Group Goal / Community create | Coming soon | Tiles disabled in type grid |
| Group settlements | Not working | Tile deferred; API_GAP — finance SQL ready, write API incomplete |
| Group/Business Memory write | Partial | Honest empty until write path |
| Business Memory chooser | Coming soon | Figma `658:9573` DEFERRED |
| Business Project/Event/Vendor Ops | Coming soon | Create categories badged Coming Soon |
| Personal Transfer / Savings / Reflect | Coming soon | Quick Add tiles disabled |
| AI Insights cards | Coming soon | Across personal Moments/Memory/Pulse |
| Account hub prefs / legal | Partial | FIGMA_GAP: currency/language/appearance deferred; Privacy/Terms placeholder |
| Activity delete | Coming soon | Edit flows exist; delete not shipped |
| Life scores quality | Partial | May be `FIGMA_SEEDED` — not production metrics |
| Orphan screens | Not working | `ExpenseCreateView`, `BusinessCreateEmptyView`, `BusinessSetupScrollView`, `CompanySettingsView` not shell-routed |

---

## 4. iOS ↔ Figma parity

| Figma node | Screen | iOS view | Parity |
|------------|--------|----------|--------|
| `353:320` / `353:317` | Personal Pulse empty | `PersonalPulseEmptyView` | FAIL — live body; await screenshot |
| `353:394` / `353:391` | Personal Moments empty | `PersonalMomentsEmptyView` | FAIL — await screenshot |
| `353:5783` / `353:5780` | Personal Life empty | `PersonalLifeEmptyView` | FAIL — await screenshot |
| `353:5878` / `353:5875` | Personal Memory empty | `PersonalMemoryEmptyView` | FAIL — await screenshot |
| `353:6809` | Life Ops setup | `PersonalLifeOpsSetupView` | FAIL — long-form restored; await screenshot |
| `353:6905` | Future setup | `PersonalFutureSetupView` | FAIL — await screenshot |
| `353:7075` | Lifestyle setup | `PersonalLifestyleSetupView` | FAIL — await screenshot |
| `353:7217` | Relationships setup | `PersonalRelationshipsSetupView` | FAIL — await screenshot |
| `575:8967` | Group Pulse empty | `GroupPulseEmptyView` | PARTIAL/FAIL — await screenshot |
| `575:8553` | Group Moments empty | `GroupMomentsEmptyView` | PARTIAL — await screenshot |
| `575:9917` | Group Experience setup | `GroupExperienceSetupView` | PASS (Phase 6 checklist) / still needs device compare |
| `575:9919` | Group Purchase setup | `GroupPurchaseSetupView` | PASS (Phase 6 checklist) |
| `575:10567` | Group Living setup | `GroupLivingSetupView` | PASS (Phase 6 checklist) |
| `657:9980` | Business Pulse empty | `BusinessPulseEmptyView` | Implemented; await screenshot |
| `695:4455` | Company setup | `CompanySetupFlowView` | MATCHED (Phase 5 note) |
| `692:34736+` | Business wizards | `BusinessSetupWizardView` | PASS (Phase 6 checklist) |
| `1075:7556` | Circle Coming Soon | `CircleComingSoonView` | Intentional CS |
| `1075:7637` | Life360 Coming Soon | `Life360ComingSoonView` | Intentional CS |

**Official visual MATCHED count:** **0** until device↔Figma screenshot compare.

MCP verified nodes exist for Personal Pulse empty `353:320`, Group Pulse empty `575:8967`, Business Pulse empty `657:9980`.

---

## 5. Context × tab matrix

| | Pulse | Moments | Quickadds | Life | Memory |
|--|-------|---------|-----------|------|--------|
| Personal empty | Working | Working | Working | Working | Working |
| Personal active | Partial | Partial | Partial | Partial | Partial |
| Group empty | Working | Working | Working | Working | Working |
| Group active | Partial | Working | Partial | Partial | Partial |
| Business empty | Working | Working | Working | Working | Working |
| Business active | Partial | Working | Working | Partial | Partial |
| Circle | Coming soon | Coming soon | Coming soon | Coming soon | Coming soon |

*Partial* = UI present with Coming Soon tiles, API_GAP banners, seeded metrics, or disabled actions.  
*Working* = primary flow usable end-to-end in code.

---

## 6. Manual checklist (on device)

Keep phone unlocked; confirm ngrok tunnel is up.

| # | Action | Pass if |
|---|--------|---------|
| 1 | Sign in (Apple or email) | Shell loads; no hang on splash |
| 2 | Personal → all 5 tabs empty | Marketing empties match Figma structure |
| 3 | Create Life Ops → Activate | Moment appears; Pulse becomes active |
| 4 | Quickadds → Expense | Expense posts; activity updates |
| 5 | Group → Create Experience | Setup → Add People → Activate |
| 6 | Scan/join QR (if second account) | Redeem succeeds |
| 7 | Business → Company setup | Company chip appears |
| 8 | Business Create Team Ops | Moment activates; Pulse finance |
| 9 | Circle + Life360 | Coming Soon only (expected) |
| 10 | Account hub | Save profile / PIN / sign out |

---

## 7. Verdict

| Area | Status |
|------|--------|
| Installable on device | **Working** |
| Core create + shell | **Working** |
| Finance depth (settle / biz approval) | **Not working** |
| Figma visual MATCHED | **Not working** (0 nodes) |

The complete app shell and main create paths are on-device ready. Circle/Life360 and several finance/AI surfaces are intentionally Coming Soon or API-gapped. Figma parity is structurally implemented for empties/setups but **0 nodes are officially MATCHED** until device captures are compared to Figma nodes.

### Related docs
- `docs/implementation/PHASE_5_EMPTY_STATE_FIGMA_MAPPING.md`
- `docs/implementation/PHASE_6_FIGMA_PARITY_CHECKLIST.md`
- `docs/implementation/S3_GROUP_AUDIT.md`
- `docs/implementation/S4_BUSINESS_AUDIT.md`
