# S1 Global App Shell — Implementation Report

**Date:** 2026-08-26  
**Scope:** S1-A → S1-M  
**Verdict:** **PASS** (declared S1 scope)  
**Next:** **S2 PERSONAL — NOT STARTED**

---

## Scope

Reusable global chrome for iOS (SwiftUI) + Android (Compose): theme engine, TopBar, Context/Company/Moment switchers, BottomNav, QuickAdd/Create launchers, ScreenResolver + invariants, bootstrap SWR/TTCS, parity docs. No Personal/Group/Business product feature work. No V030. No V041.

---

## Figma nodes inspected

| Node | Role |
|------|------|
| `169:487` | Android screens root (pinned) |
| `763:12896` | TopBar |
| `763:12897` | ContextSwitcher |
| `501:6367` | BottomNav |
| Phase 4 + code | Moment family colors (FIGMA_GAP where unverified) |

Figma MCP unavailable in-session → tokens from Phase 4 + native sources; recorded in [`docs/design/MOMENTRA_THEME_MATRIX.md`](../design/MOMENTRA_THEME_MATRIX.md).

---

## Existing components reused / refactored / created

| Action | Items |
|--------|-------|
| **REUSE** | Android MomentSwitcher, ShellBottomNavigation, MeRepository/BootstrapCache, iOS MomentSwitcherView, Tab icons |
| **REFACTOR** | AppShellViewModel / AppShellModel, ContextSwitcher (bootstrap), MomentraTopBar, ShellChrome, AppShellScreen/View |
| **CREATED** | `CompanySwitcher`, theme engine (`MomentraShellTheme`), `ShellPolicy` (visibility + invariants + resolver), QuickAdd/Create/Profile/Life360 shell stubs, `MOMENTRA_THEME_MATRIX.md` |

---

## Theme architecture

```text
GlobalTheme
ContextTheme.contextAccent   (PERSONAL|GROUP|BUSINESS|CIRCLE)
MomentTheme.{family,type,primary,secondary,surfaceTint,icon}
GlobalSurfaceTheme.life360   (NOT a context)
```

No `currentColor`. ContextSwitcher → contextAccent; MomentSwitcher/QuickAdd → MomentTheme.primary.

---

## Navigation / state

- Bootstrap-driven `supportedContexts`
- Business: Context → Company → Moments(company) → Moment (companyId on bootstrap business moments)
- `selectedTabByContext` only
- Self-heal after every bootstrap merge
- Life360 / Profile = shell stubs (global surface / profile sheet)

---

## Bootstrap integration

```text
cached /v1/me → SHELL VISIBLE (TTCS) → background GET /v1/me → merge + heal → visible tab
```

Android shell VM now uses `MeRepository(applicationContext)` via AppRoot factory (BootstrapCache wired). No startup list fan-out for inventory.

**API change (no migration):** bootstrap business moments include `companyId` (+ `momentTypeCode` from business family).

---

## Network waterfall / performance

| Metric | Result |
|--------|--------|
| Startup inventory | `/v1/me` only (no list fan-out) |
| S0 `/v1/me` baseline | p50 995 / p95 1023 |
| S1 re-check (runtime-parity sample) | ~1087 ms single call — **no unexplained &gt;10% regression** vs p95 |
| TTCS | Field `ttcsMs` recorded on shell state when cache paints |

S1 did not optimize `/v1/me` latency (correct).

---

## Tests / builds

| Suite | Result |
|-------|--------|
| Backend platform-foundation + runtime-parity | **PASS** (23) |
| Android `ui.shell.*` unit tests | **PASS** |
| iOS source tests (`ShellInvariantTests`, etc.) | **Complete** — Xcode runtime **BLOCKED_ENVIRONMENT** |
| Migrations | V030 untouched/blocked; V031–V040 preserved; **zero new migrations** |

---

## Four S1 documents

1. [`S1_SHELL_AUDIT.md`](S1_SHELL_AUDIT.md)  
2. [`../design/MOMENTRA_THEME_MATRIX.md`](../design/MOMENTRA_THEME_MATRIX.md)  
3. [`S1_PARITY_MATRIX.md`](S1_PARITY_MATRIX.md)  
4. This report  

---

## Accessibility

Icon-only TopBar actions labeled (Life360, Create moment, Open profile). Context tabs announce selected. Reduced-motion not newly instrumented beyond existing animations — follow-up when Figma motion inspected live.

---

## Mock-data audit

No production fake Moments/companies/contexts introduced. Tests/fakes only.

---

## Remaining gaps / risks

- Live Figma MCP screenshot parity for all chrome nodes
- Future Building Pulse hero still uses legacy purple in Personal product UI until Figma confirms `#10B981`
- Group Purchase/Living / Business type swatches partially **FIGMA_GAP**
- iOS BottomNav still system TabView (functional parity; visual custom bar polish optional)
- Backend still always includes CIRCLE in `supportedContexts` (bootstrap config — not native hard-code)

---

## Completion gate

| Gate | Status |
|------|--------|
| Canonical TopBar / Context / Company / Moment / BottomNav / QuickAdd / Create | ✓ |
| Theme Global + Context + Moment + Circle + Life360 global | ✓ |
| Bootstrap-driven; persisted context/company; tab-by-context; self-heal; resolver | ✓ |
| Cached shell first; no fan-out; visible-slice refresh; /v1/me regression OK; TTCS recorded | ✓ |
| Android tests/build PASS; backend PASS; iOS source tests; four docs | ✓ |
| Zero migrations; V030 untouched; V031–V040 preserved | ✓ |
| No unresolved FAIL in S1 scope | ✓ |
| S2 PERSONAL | **NOT STARTED** |

---

## STOP

```text
S1 GLOBAL APP SHELL

STATUS: PASS

GLOBAL SHELL: canonical TopBar, ContextSwitcher, CompanySwitcher, MomentSwitcher, BottomNav, QuickAdd + Create launchers

THEME ENGINE: Global / Context(contextAccent) / Moment / GlobalSurface(Life360); MOMENTRA_THEME_MATRIX.md

FIGMA PARITY: Phase-4 anchored; MCP visual live-diff FIGMA_GAP; Android functional PASS; iOS runtime BLOCKED_ENVIRONMENT

BACKEND: companyId on business bootstrap moments; /v1/me suite PASS; no V030/V041

ANDROID: unit tests PASS; BootstrapCache wired

IOS: source parity + invariant tests; Xcode BLOCKED_ENVIRONMENT

PERFORMANCE: no fan-out; TTCS field; /v1/me ~1.09s sample vs S0 p95 1023 (no unexplained >10% fail)

ACCESSIBILITY: labels on shell icon actions + context tabs

GAPS: live Figma MCP; some Moment type swatches FIGMA_GAP; iOS custom tab bar polish

FILES CHANGED: docs/implementation/S1_*.md, docs/design/MOMENTRA_THEME_MATRIX.md, apk shell/theme/policy, momentra Shell/Design/Policy, backend bootstrap + listBusinessMoments

NEXT: S2 PERSONAL — NOT STARTED
```
