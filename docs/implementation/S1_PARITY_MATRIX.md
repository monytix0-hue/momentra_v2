# S1 Parity Matrix

**Date:** 2026-08-26  
**Figma root:** `TzLvwVwlPbeVB8ug1zB3GM` / `169:487`  
**Statuses:** PASS | FAIL | BLOCKED_ENVIRONMENT | FIGMA_GAP | NOT_REQUIRED

| Component | Figma | iOS | Android | Functional | Visual |
|-----------|-------|-----|---------|------------|--------|
| TopBar | `763:12896` | PASS (source) | PASS | PASS | Android PASS / iOS BLOCKED_ENVIRONMENT |
| ContextSwitcher | `763:12897` | PASS (bootstrap-driven) | PASS (bootstrap-driven) | PASS | Android PASS / iOS BLOCKED_ENVIRONMENT |
| CompanySwitcher | top-row chip | PASS (extracted Menu) | PASS (`CompanySwitcher`) | PASS | Android PASS / iOS BLOCKED_ENVIRONMENT |
| MomentSwitcher | module switcher | PASS | PASS | PASS (MomentTheme.primary) | PARTIAL / FIGMA_GAP |
| BottomNav | `501:6367` | PASS (TabView+icons; custom parity deferred polish) | PASS (`ShellBottomNavigation`) | PASS | Android PASS / iOS BLOCKED_ENVIRONMENT |
| QuickAddLauncher | center + / create tab | PASS (shell port) | PASS (shell port) | PASS | NOT_REQUIRED product catalogs |
| Create Moment launcher | TopBar + | PASS | PASS | PASS | PASS |
| Profile entry | TopBar avatar | PASS (sheet stub) | PASS (sheet stub) | PASS | PASS |
| Life360 entry | TopBar radar | PASS (GlobalSurface stub) | PASS (GlobalSurface stub) | PASS | PASS |
| Theme ContextAccent | context tabs | PASS | PASS | PASS | FIGMA_GAP Circle vs Personal |
| Theme MomentPrimary | switcher/QA | PASS | PASS | PASS | FIGMA_GAP Future Building / Group Purchase families |
| GlobalSurface Life360 | TopBar | PASS | PASS | PASS | PASS |
| ScreenResolver / invariants | — | PASS (source) | PASS (unit) | PASS | NOT_REQUIRED |
| Bootstrap SWR / TTCS | — | PASS (source) | PASS | PASS | NOT_REQUIRED |

## Notes

- Figma MCP was unavailable during S1 execution; visual Android verification is compile + component structure against Phase 4 nodes; live screenshot diff remains a follow-up when MCP/device available.
- iOS Xcode/device runtime = **BLOCKED_ENVIRONMENT** on Windows.
- Product Personal/Group/Business screens intentionally unchanged (S2–S4).
