# Business B1 — Pulse and Create

Date: 2026-09-30.

Classification: **PASS CANDIDATE — runtime verification pending**

Owner device smoke of Money, Daily, and Team was not run on this Windows host. Android debug Kotlin compiled, and the unit tests below passed. iOS sources were updated and were not compiled here. This is not a full PASS.

## 1. Before / after

Before: three Pulse screens each drew a health ring from `financialHealthScore` or SLA, with client labels such as “Strong & Growing”. Cash on Money Pulse appeared only after Life had been opened, otherwise the copy was “Cash balance not added”. Create was one flat tile grid. `BusinessTabDataCache.invalidateMoment` had no callers. Khata saves did not refresh Pulse.

After: one `BusinessPulseScreen` on each platform. The three family files only pass family and context into it. Hero and snapshot both render `displayedFacts()` from one `BusinessPulsePresentation`. The score is not read. Create groups the same kind lists into Primary and More. A successful Business write invalidates that moment’s cache, drops the in-flight pulse load, then bumps the refresh token and prefetches. Khata entry and cash-sale saves join that loop. Party-only creates do not.

Moments, Life, and Memory trees were not redesigned and were not deleted.

## 2. Files changed

Android:

- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/BusinessMomentFamilyConfig.kt` (new)
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/BusinessPulsePresentation.kt` (new)
- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/BusinessPulseScreen.kt` (new)
- `BusinessPulseActiveContent.kt`, `RunwayPulseActiveContent.kt`, `OpsPulseActiveContent.kt`, `TeamOpsPulseActiveContent.kt` (thin wrappers)
- `BusinessQuickAddHub.kt`, `BusinessTabLoad.kt`, `BusinessTabDataCache.kt`, `BusinessKhataFlow.kt`
- `AppShellScreen.kt`, `AppShellViewModel.kt`
- `BusinessActionRegistryTest.kt`, `BusinessPulsePresentationTest.kt`

iOS:

- `BusinessMomentFamilyConfig.swift`, `BusinessPulsePresentation.swift`, `BusinessPulseScreen.swift` (new)
- `RunwayPulseActiveView.swift`, `OpsPulseActiveView.swift`, `TeamOpsPulseActiveView.swift`, `BusinessPulseActiveView.swift` (thin wrappers)
- `BusinessQuickAddHub.swift`, `BusinessTabLoad.swift`, `BusinessTabDataCache.swift`, `BusinessKhataHomeView.swift`
- `AppShellView.swift`, `AppShellModel.swift`, `APIClient.swift` (typed roster, issue, and approval list bodies)
- `momentraTests/BusinessActionRegistryTests.swift` (new, not run)

## 3. Shared config

`BusinessMomentFamilyConfig` holds family, title, and primary/secondary `BusinessQuickAddKind` lists. Labels and capability rules stay on the existing kind helpers and `BusinessActionRegistry`. Pulse Today and Create both call `visibleActions`. If filtering leaves two primaries, both show two. A secondary kind is not promoted.

Small shop Team does not include Decision. Its primary list is Khata, Staff update, Ask approval.

## 4. API contracts reused

No new score API. No projection-key change.

- GET `.../pulse` for finance totals, operations extras, and bundled activity
- GET `.../life` only on the Money pulse load, in parallel with pulse, for `runwayPayload.availableCash` and so runway can be shown after that fetch
- GET `.../roster`, `.../approvals`, `.../issues` for Team (Daily also loads issues)
- Existing Quick Add POST sheets, unchanged
- Khata entry POST and cash-sale revenue POST, accounting unchanged

`financialHealthScore` is not displayed.

## 5. Provenance of visible metrics

| Metric | When shown | Scope line |
|---|---|---|
| Revenue, outflow, invoice outstanding | Finance `dataQuality` is not `EMPTY` or `EMPTY_SUPPORTED`. A payload `"0"` on an empty quality is omitted. A real `0` renders `0`. | Company totals, on the hero and the snapshot |
| Cash, runway | Money Pulse’s own `getLife` succeeded and the field is non-blank. If `getLife` fails, both are omitted and revenue, outflow, attention, and recent still render. | Company totals |
| Open issues, vendors, spend, SLA | That operations `sectionQuality` key is `REAL_DATA` | Not company finance |
| People | Roster list loaded. Failure omits the figure. A loaded empty roster is `0`. | Not labeled as team health |
| Approvals, open work | Those lists loaded | Moment-scoped lists |
| Your updates | Count of the activity list returned with pulse | Actor-scoped |
| Needs attention | Pending approvals, real invoice outstanding, ops `needsAttention` when that section is `REAL_DATA`, otherwise open issues | — |
| Recent | Up to three activity rows | Title is exactly `Your recent activity` |

See all on Recent opens the existing Moments tab. Attention has no See all, because there is no separate attention destination that is not a create sheet. Cards are not given a new detail route.

Finance and Life cash figures are company-wide. They are not labeled as a location or as this moment’s health.

## 6. Action registry

Empty capabilities still fail open to the default V019 codes. That behavior is unchanged and covered by tests. The server can still return 403. Hiding a tile is not authorization. No copy claims that viewers cannot record.

## 7. Cache invalidation

`refreshVisibleBusinessTab` on both clients now:

1. Invalidates `BusinessTabDataCache` for the selected moment and bumps a generation counter.
2. Drops that moment’s in-flight pulse task.
3. Then increments the refresh token and prefetches with the selected moment’s family, so Money prefetch includes `getLife`.

Personal and Group caches are not cleared. A pulse GET failure still fails the load. A Life GET failure does not.

Khata udhaar/payment and cash sale call this refresh after their own list reload. Adding a party does not.

## 8. Android / iOS parity

Both platforms share the same primary lists, the same `Unavailable` versus `Zero` rules, the same recent title, and the same company-totals line on hero and snapshot. Neither calculates a health label. iOS was not compiled on this host, so parity is source-level until an iOS build runs.

## 9. Tests

| Suite | Result |
|---|---|
| `business-s4-finance.test.ts`, `business-company-life.test.ts`, `business-ops-three-layer-join.test.ts` | 9 passed, 0 failed |
| `BusinessActionRegistryTest` (12) and `BusinessPulsePresentationTest` (8) | 12 registry tests passed after the Quick Add routing fix, including family primaries and moment switching. Presentation tests passed in the earlier B1 run |
| `momentraTests/BusinessActionRegistryTests.swift` | Written. Not run. No iOS toolchain on this host |

Presentation tests cover empty finance versus a real zero, shared displayed facts, team facts without a finance score, Life failure omitting cash and runway while keeping revenue, missing `sectionQuality`, empty attention copy only when the list loaded, and cache invalidation of one moment id.

## 10. Runtime verification

Not run. The owner loop (cold Money Pulse, revenue, expense, invoice, Khata, Daily issue, Team update and approval) still needs a device.

## 11. Unresolved B0 risks

Still open, and not claimed fixed:

- R1 company-level pulse, life, and memory rows can be overwritten by another moment
- R5 viewer and observer writes are not fully denied on the server
- Location is not applied to finance totals
- Activity and SSE stay actor-scoped, so other members do not see the save live
- Growing Memory placeholders
- Create Memory chooser remains a hardcoded empty list

## 12. B2 readiness

Do not start Moments, Life, or Memory redesign on these numbers until a targeted B0.5 pass checks R1, viewer authorization, cross-user refresh, multi-location finance, and cold-start cash on a device. Pulse can show company totals and actor activity honestly. It cannot yet promise per-moment health or per-location cash.

The three Pulse files remain as wrappers. The Moments and Memory trees are unchanged.

## Quick Add family routing

Quick Add was resolving a missing type code to Team, so Money, Daily, and Team could show one catalog. The hub now takes the selected moment’s type only. A blank type on that moment is not replaced by another moment’s code, and it does not fall through to Team.

`quickAddSpec` is the catalog. `BusinessActionRegistry` only answers whether a kind in that list is available. Empty capabilities still fail open to `DEFAULT_CODES`, then that set is intersected with the selected family’s ids. Primary sets:

- Growing Money: revenue, expense, invoice
- Small shop Money: Khata, revenue, expense
- Daily: spend, vendor, issue
- Growing Team: team update, approval, decision
- Small shop Team: Khata, team update, approval

The business moment list maps `businessFamily` into `momentTypeCode` when the list payload has no type code. Pulse, Moments, Life, Memory, and backend endpoints were not changed.

Android `BusinessActionRegistryTest` covers each family’s primary ids and a moment switch that does not keep the previous catalog. iOS `BusinessActionRegistryTests` has the same cases and was not run on this host.
