# iOS Native Layout Audit — Summary

**Date:** 2026-09-01  
**Register:** [`IOS_NATIVE_LAYOUT_AUDIT.csv`](IOS_NATIVE_LAYOUT_AUDIT.csv)  
**Regenerator:** `npx tsx scripts/qa/build-ios-native-layout-audit.ts`  
**Capture:** [`scripts/qa/capture-native-layout-screenshot.sh`](../scripts/qa/capture-native-layout-screenshot.sh)

## Scope

Layout structure only — **keep** custom nav icons (`NavPulse`, `NavMoments`, `ShellPlus`), hero artwork, and brand tokens. APK defines **what sections exist**; iOS HIG defines **containers** (`List`, `Form`, `safeAreaInset`, `NavigationStack`).

## Score counts (`native_layout_score` 0–2)

| Score | Count | Meaning |
|-------|-------|---------|
| 0 | 0 | ScrollView+VStack; magic padding; no native containers |
| 1 | 59 | Partial native (NavStack or List without sticky CTA) |
| 2 | 19 | Form/List + safeAreaInset or reference pattern |

**Total screens:** 78

## Wave checklist

| Wave | Screens | Focus |
|------|---------|-------|
| A | 6 | Shell chrome — TabView, top chrome, FAB inset |
| B | 6 | Auth/gates — Form, pinned CTAs |
| C | 13 | P1 body — QuickAdd hubs, pulse dashboards, PersonalLife |
| D | 53 | Remaining PARTIAL backlog |

## Top 20 remediation queue

| screen_id | wave | native | content | remediation |
|-----------|------|--------|---------|-------------|
| business_memory | C | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_moments | C | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_life | C | 1 | 0 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| personal_create | C | 1 | 0 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| personal_moments | C | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| group_pulse | C | 1 | 0 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| personal_life | C | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| personal_life | C | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| unknown_life | D | 1 | 0 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_life | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_life | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_memory | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_moments | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_pulse | D | 1 | 1 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_memory | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_moments | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_pulse | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_memory | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_moments | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |
| business_pulse | D | 1 | 2 | Maintain NativeDashboardScaffold sections; verify sticky CTA |

## Reference implementations

- [`AccountHubView.swift`](../momentra/momentra/Account/AccountHubView.swift) — `NavigationStack` + `Form`
- [`GroupFinanceScreens.swift`](../momentra/momentra/Shell/GroupActive/GroupFinanceScreens.swift) — `safeAreaInset` sticky CTA

## Verification

```bash
./scripts/qa/capture-native-layout-screenshot.sh A app_shell ios after
cd momentra && xcodebuild -scheme momentra -destination 'generic/platform=iOS' build
```
