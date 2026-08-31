# S9-QH Navigation Matrix

**Gate:** S9-QH Quick Add Hub + Screen Linking  
**Generated:** 2026-08-29 (QH-W Settle wiring)

## Launcher → Hub → Sheet → Refresh

| Link | Android | iOS | Status |
|------|---------|-----|--------|
| `bottom.quickadd` → hub | `ShellBottomNavigation` → CREATE tab | TabView `.create` | PASS |
| Personal hub | `PersonalQuickAddHub` on CREATE | `PersonalQuickAddHubView` on CREATE | PASS |
| Group hub | `GroupQuickAddHub` on CREATE (active moment) | `GroupQuickAddHubView` on CREATE | PASS |
| Business hub | `BusinessQuickAddHub` on CREATE (active moment + company) | `BusinessQuickAddHub` on CREATE + Pulse modal | PASS |
| Hub tile → expense sheet | AppShell sheet state | `.sheet` modifiers | PASS |
| Hub tile → group participants | `GroupParticipantsSheet` | `GroupParticipantsSheet` | PASS |
| Hub tile → Settle | **`GroupSettlementSheet`** (shared G01–G12 + Wedding) | same | **PASS** |
| Hub tile → business revenue/invoice | `BusinessRevenueSheet` / `BusinessInvoiceSheet` | same | PASS |
| Hub tile → business members | `BusinessMembersSheet` (companyId) | `BusinessMembersSheet` | PASS |
| Finance CTA → Settle | `GroupFinanceDetailFlow.onSettle` | `GroupFinanceDetailView.onSettle` | PASS |
| Cancel → hub / exit create | `onCreateBack` / `exitCreateDestination` | same | PASS |
| Submit → scoped refresh | `refreshVisibleGroupTab()` (finance / Pulse / Activity) | same | PASS |
| Moment switch → hub theme | `momentTypeCode` → `MomentThemes` | same | PASS |
| Company switch → business context | `selectedCompany` preserved | same | PASS |
| Capabilities → hub gating | `state.capabilities` passed | `model.capabilities` passed | PASS |

## Anti-patterns resolved

- Business CREATE no longer falls through to empty experience when moment active
- Group `qa.tile.people` no longer routes to create-moment flow
- Revenue/Invoice disabled on non-Runway business moments (CAPABILITY_GAP, not fake submit)
- Settlement **IMPLEMENTED** — V047 mapped; Android/iOS registries enable `SETTLEMENT`; one shared sheet for all Group subtypes

## Sheet stacking

Single sheet host at AppShell level; hub dismisses before opening finance sheets on iOS modal path. Android uses sibling sheet overlays with independent open flags. Settle opens from hub tile or Finance CTA; post-submit calls `refreshVisibleGroupTab()` only (scoped).
