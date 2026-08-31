# S1 Shell Audit

**Date:** 2026-08-26  
**Scope:** Existing iOS + Android global app shell before S1 chrome/theme work  
**Figma root:** `TzLvwVwlPbeVB8ug1zB3GM` / node `169:487`  
**Chrome seeds:** TopBar `763:12896`, ContextSwitcher `763:12897`, BottomNav `501:6367`  
**Rule:** Prefer REUSE/REFACTOR. No production chrome edits before this document.

---

## Executive summary

Both platforms already have a working S0-backed shell: TopBar, ContextSwitcher, MomentSwitcher, BottomNav (Android custom; iOS TabView+chrome), bootstrap SWR, and `tabByContext`. Gaps vs S1 plan are architectural, not greenfield:

| Finding | Severity |
|---------|----------|
| ContextSwitcher always renders all 4 contexts | **BLOCK S1-D** |
| `supportedContexts` / `currentlySelectedContext` unused for selection | **BLOCK S1-D/K** |
| CompanySwitcher embedded in TopBar (not independent) | **S1-E** |
| Business Moments not filtered by `selectedCompany` | **BLOCK S1-E/F invariant** |
| Android `AppShellViewModel` default `MeRepository()` has no `BootstrapCache` | **BLOCK S1-K TTCS** |
| Avatar → sign-out (a11y says profile) | **S1-C** |
| Life360 TopBar action no-op / treated as Circle deferred only | **S1-C** (GlobalSurface) |
| No `ContextTheme.contextAccent` vs `MomentTheme.primary` split | **S1-B** |
| Personal QuickAdd owned inside shell host | **REFACTOR → launcher port** |
| Monolithic screen `when` (not ScreenResolver) | **S1-I** |
| iOS BottomNav is system `TabView`, not extracted custom component | **REFACTOR S1-G** |
| No shell-state invariant self-heal | **S1-I/M** |
| Figma MCP unavailable this session — use Phase 4 + code hex; record FIGMA_GAP where unverified | **S1-L** |

---

## Component classification

### Android

| Component | Path | Classification | Notes |
|-----------|------|----------------|-------|
| AppShell host | `ui/shell/AppShellScreen.kt` | **REFACTOR** | Split chrome vs product; Personal QA sheets leak |
| AppShellViewModel | `ui/shell/AppShellViewModel.kt` | **REFACTOR** | Add invariants, supportedContexts, company-scoped moments, cache-wired repo |
| MomentraTopBar | `ui/shell/components/MomentraTopBar.kt` | **REUSE** chrome; **REFACTOR** extract company + profile/Life360 wiring |
| BusinessCompanyChip | private in TopBar | **REFACTOR →** `CompanySwitcher` | Independent component |
| ContextSwitcher | `ui/shell/components/ContextSwitcher.kt` | **REFACTOR** | Accept `supportedContexts`; use `contextAccent` |
| MomentSwitcher | `ui/shell/components/MomentSwitcher.kt` | **REUSE**; **REFACTOR** MomentTheme.primary | Visibility policy centralize |
| ShellBottomNavigation | `ui/shell/components/ShellBottomNavigation.kt` | **REUSE** | Wire BottomSelected token; a11y pass |
| ShellTokens | `ui/theme/ShellTokens.kt` | **REFACTOR →** GlobalTheme + ContextTheme | Seed for S1-B |
| PersonalPulseFamily | `ui/shell/personal/PersonalPulseFamily.kt` | **REFACTOR** colors → MomentTheme table | Keep product copy in place |
| GroupSetupTheme | `ui/shell/empty/group/GroupSetupTheme.kt` | **REFACTOR** accents → MomentTheme | Product setup stays S3 |
| PersonalQuickAddHub | `ui/shell/personal/*` | **DEFER** product; **REFACTOR** mount via QuickAddLauncher | Do not rewrite forms |
| ShellDestinationContent | inside AppShellScreen | **REFACTOR →** ScreenResolver slots | Keep product bodies |
| Color.kt / Theme.kt overlap | `ui/theme/*` | **REMOVE_DUPLICATE** gradually | Prefer Shell/Global tokens |
| HomeScreen.kt | deprecated stub | **REMOVE_DUPLICATE** if unused | |
| MeRepository / BootstrapCache | `data/repository`, `data/local` | **REUSE**; fix VM wiring | |
| AppShellViewModelTest | `ui/shell/AppShellViewModelTest.kt` | **REUSE**; expand invariants | |

### iOS

| Component | Path | Classification | Notes |
|-----------|------|----------------|-------|
| AppShellView | `Shell/AppShellView.swift` | **REFACTOR** | God view; avatar=signOut; TabView |
| AppShellModel | `Shell/AppShellModel.swift` | **REFACTOR** | Same invariant/bootstrap gaps as Android |
| MomentraTopBar | `Shell/Components/ShellChrome.swift` | **REUSE**; **REFACTOR** company extract + actions | |
| companyChip | private in ShellChrome | **REFACTOR →** CompanySwitcher | |
| ContextSwitcherView | ShellChrome | **REFACTOR** supportedContexts | |
| MomentSwitcherView | ShellChrome | **REUSE**; MomentTheme | |
| Bottom nav | `TabView` in AppShellView | **REFACTOR** toward Figma custom bar parity | Phase 4 claimed ShellBottomNavigationView MATCHED — current code uses TabView |
| CompactShellChrome | ShellChrome | **REUSE** | |
| Brand/Setup/PersonalEmpty tokens | scattered | **REFACTOR →** ShellTokens.swift + MomentTheme | |
| PersonalQuickAddHubView | PersonalEmpty | **DEFER** product; launcher port | |
| ShellMeGateway / BootstrapCacheStore | Shell + Domain | **REUSE** | |
| ShellModelTests | momentraTests | **REUSE**; expand | |

---

## Theme / color findings

### Context accents (today — often re-hardcoded)

| Context | Hex | Sources |
|---------|-----|---------|
| PERSONAL | `#7C5CFC` | ShellTokens, ContextSwitcher, empty accents |
| GROUP | `#E8621A` | ShellTokens, brand ember |
| BUSINESS | `#818CF8` | ShellTokens |
| CIRCLE | `#7C5CFC` | ShellTokens (=Personal) — verify Figma |
| LIFE360 | *not modeled* | Radar button only — must be GlobalSurfaceTheme |

### Personal Moment accents (code; verify Figma in S1-B)

| Family | Primary (code) | Notes |
|--------|----------------|-------|
| Life Operations | `#7C5CFC` | Matches product decision |
| Relationships | `#E91E63` | Matches product decision |
| Future Building | `#8B5CF6` / hero `#6C4EF2` | Product decision said `#10B981` — **FIGMA VERIFY** before overwrite |
| Lifestyle | teal `#0EA5A4` path in theme | Product decision: use Figma canonical |

### Group type accents (GroupSetupTheme)

Trip `#E8744F`, Wedding `#EC4899`, Party/House `#3B82F6`, Outing `#14B8A6`. Shared Purchase / Living families need Figma matrix fill in S1-B.

### Business Moment colors

Not centralized in shell tokens — **FIGMA_GAP** until matrix filled from design.

**Anti-pattern:** No single `currentColor`, but components mix context accent and moment accent interchangeably — S1-B must split APIs.

---

## State / bootstrap findings

### Present

- SWR: cache paint → network `/v1/me` → merge
- `tabByContext` on both platforms (good — keep as only tab memory)
- Moment lists from bootstrap slices (no shell list fan-out for inventory)
- Company list from bootstrap

### Missing / wrong

1. **supportedContexts unused** — ContextSwitcher iterates `AppContext.entries`
2. **currentlySelectedContext unused** — defaults PERSONAL always
3. **Business moments** applied as full `boot.businessMoments` without company filter (`MomentSummary` may lack `companyId` — document gap; filter when field exists / use bootstrap shape)
4. **Android MeRepository()** without Context → **no cache** → TTCS broken for shell VM unless AppRoot injects Context ctor
5. **No invariant self-heal** after merge
6. **selectedMomentByContext** not persisted map (only single selectedMomentId) — context switch clears moment then re-picks; OK if intentional, but plan wants by-context memory — **REFACTOR**
7. Life360 capability not from bootstrap flags for TopBar visibility

### Network waterfall (current intent)

```text
Auth bootstrapMe (identity)
→ Shell bindIdentity
  → cachedBootstrap? paint
  → getBootstrap() again
```

Shell inventory does not call list* Moments/companies. Double `/v1/me` on cold start is soft (auth + shell).

---

## Navigation / resolver

Android: `ShellDestinationContent` huge `when` — Personal family-aware; Group/Business placeholders/empty trees.  
iOS: nested `@ViewBuilder` switches in `AppShellView` + `ContextEmptyExperienceView`.

**Classification:** **REFACTOR** to ScreenResolver with product slots; do not delete Personal/Group/Business bodies.

---

## Figma parity baseline (Phase 4)

| Node | Component | Phase 4 | Audit note |
|------|-----------|---------|------------|
| 763:12896 | TopBar | MATCHED | Wire Life360 + Profile stubs |
| chip | Company | MATCHED | Extract CompanySwitcher |
| 763:12897 | Context | MATCHED | Must become bootstrap-driven |
| Moment switcher | Moment | PARTIAL | Theme + visibility policy |
| 501:6367 | BottomNav | MATCHED Android; iOS claim vs TabView drift | Resolve in S1-G |

Figma MCP not available during this audit → live token re-verify deferred to S1-B/L with honest `FIGMA_GAP` where needed.

---

## Personal / Group / Business leakage

| Leak | Location | S1 action |
|------|----------|-----------|
| Personal QuickAdd sheets in shell host | AppShellScreen / AppShellView | Launcher port only |
| Personal family branching in resolver | ShellDestinationContent | Keep as slot; don't expand |
| Group create phase in shell | empty + host | Leave; don't build S3 |
| Business create company in empty | empty trees | Leave; CompanySwitcher chrome only |
| Circle = Deferred hard-coded | ViewModels | OK as content; availability must be bootstrap |

---

## Tests today

| Suite | Coverage | Gap |
|-------|----------|-----|
| Android AppShellViewModelTest | context, company, tab preserve, logout | invariants, supportedContexts, theme |
| Android MomentExperienceTest | experience resolve | — |
| iOS ShellModelTests | thin | invariants, bootstrap, theme |
| iOS IdentityCacheTests | auth identity | not shell |

---

## Recommended S1 work order (confirmed)

1. Theme engine + `MOMENTRA_THEME_MATRIX.md` (S1-B)  
2. TopBar + Profile/Life360 stubs (S1-C)  
3. ContextSwitcher bootstrap-strict (S1-D)  
4. CompanySwitcher + atomic Business Moment resolve (S1-E)  
5. MomentSwitcher + visibility policy (S1-F)  
6. BottomNav + selectedTabByContext only (S1-G)  
7. QuickAdd + Create launchers (S1-H)  
8. ScreenResolver + self-heal (S1-I)  
9. Global states (S1-J)  
10. Bootstrap/TTCS cache wiring (S1-K)  
11. Parity matrix (S1-L)  
12. Invariant tests + report (S1-M) → STOP

---

## Explicit non-changes in S1

Do not rewrite Personal/Group/Business product screens. Do not execute V030. Do not create V041 unless a genuine schema gap is documented (Business `companyId` on bootstrap moments may be an API field gap — prefer OpenAPI/bootstrap DTO check before migration).
