# Gap 1 — Company-authorized Business activity

Date: 2026-09-30.

Classification: **PASS WITH GAPS**

Same-company cross-user reads and company isolation passed. Refreshing Business Moments returns the authorized company event stream for that selected moment, not only the current actor’s activity. That closes the semantic blocker for B2. Cross-user realtime does not block B2. The other open items below are frozen as separate work. This gap did not start B2 or Gap 2.

## 1. Before / after

Before: `assertCompanyMomentAccess` already required an active company membership and a moment in that company, then both Business activity reads discarded every row whose `user_id` was not the caller. Pulse Recent was titled “Your recent activity” because that feed was the caller’s rows. `actorDisplayName` existed on the clients and was left empty by the Business query.

After: membership still authorizes the read. The query returns Business rows for that moment. `user_id` on those rows is the actor, and the response includes `actorDisplayName` when `core.user_profile.display_name` is present. Pulse Recent is titled “Recent activity”. The card model accepts another member’s row with or without a name. No new table, index, or Moments layout.

## 2. Files changed

Backend:

- `backend/typescript/src/modules/projection/service.ts` — `getBusinessMomentActivity` and the activity subquery inside `getBusinessPulseOrFinance`
- `backend/typescript/tests/business-gap1-activity.test.ts` (new)

Android:

- `apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/BusinessPulsePresentation.kt`
- `apk/app/src/test/java/com/example/momentra/ui/shell/business/shared/BusinessPulsePresentationTest.kt`

iOS (not compiled on this host):

- `momentra/momentra/Shell/BusinessActive/Shared/BusinessPulsePresentation.swift`
- `momentra/momentra/Shell/BusinessActive/Shared/BusinessPulseScreen.swift`
- `momentra/momentraTests/BusinessActionRegistryTests.swift`

## 3. Authorization and scope

Order of checks is unchanged: authenticate, then `assertCompanyMomentAccess` (active `company_membership`, moment belongs to that company, otherwise the existing 403 “Not an active company member for this business moment.”), then the query.

`projection.recent_activity` still has no `company_id` or `moment_id`. The moment boundary is `scope_type = 'MOMENT'` and `scope_id = moment id`. Company is the active `business.business_moment_context` row for that moment and the caller’s company. There is no `user_id = caller` predicate.

The activity route caps at 50. The pulse bundle caps at 5. Both keep `occurred_at DESC, recent_activity_id DESC`.

## 4. Actor metadata

Business writes already store the actor in `recent_activity.user_id`. That is not the Group fan-out recipient. The read left-joins `core.user_profile` on that id and returns `NULLIF(btrim(display_name), '')` as `actorDisplayName`. A missing or blank name does not drop the row, and the server does not invent one.

`BusinessMomentCardModel` now carries optional `actorDisplayName`. Pulse still renders title, time, and amount. It does not add an actor line.

## 5. Ordering and pagination

Cursor shape is unchanged: `occurred_at|recent_activity_id`, compared as `(occurred_at, recent_activity_id) < cursor`. The Gap 1 test pages with `limit=1` across rows from two users, including two rows that share `occurred_at`, and matches `ORDER BY occurred_at DESC, recent_activity_id DESC`. The pulse bundle of at most five rows follows that same order.

## 6. Pulse

The only copy change is the Recent heading, from “Your recent activity” to “Recent activity”, on Android and iOS. Recent still shows up to three cards from the pulse activity bundle. Another member’s item maps to `BusinessMomentCardModel` when `actorDisplayName` is set and when it is blank.

The Team fact label “Your updates” was left as it was. Its count is the activity list on the pulse payload, which is now this moment’s rows rather than only the caller’s.

## 7. Moments, Life, and Memory

Moments already call `getActivity(momentId)`. They pick up the company feed with no layout change.

Life’s activity SQL was not edited. It joins every active moment in the company, does not filter `user_id`, and returns eight rows. That feed is company-wide across moments. The activity endpoint and the pulse bundle are one selected moment.

Memory projection writes were not touched.

## 8. Left unchanged

Personal activity still filters `user_id = caller` and `domain_code = 'PERSONAL'`. Group activity still drops the caller filter for `domain_code = 'GROUP'` and still treats `user_id` as a fan-out recipient. SSE `publishProjectionUpdated` still delivers only to the acting user (`REALTIME_GAP`). Finance totals are still not location-scoped. Observer write denial was not widened. B0.5 was not implemented.

## 9. Tests

`business-gap1-activity.test.ts` — 7 passed, 0 failed:

- Member B’s expense is visible to Owner A on that Money moment, and A’s expense is visible to B
- Money titles do not appear on Daily or Team; Daily and Team keep their own rows
- Company B does not see Company A, and Company A does not see Company B
- A non-member receives 403 “Not an active company member…”
- An observer can read and receives 403 on `POST .../business-expenses`
- `actorDisplayName` is “Member B” when A reads B’s row; a null profile name does not drop the row
- Several rows from two users, including a shared timestamp, match `occurred_at DESC, recent_activity_id DESC` through cursor pages of 1, and the pulse bundle follows that order

Also re-run, 0 failed:

- `business-s4-finance.test.ts` — 4 passed
- `business-company-life.test.ts` — 3 passed
- `business-ops-three-layer-join.test.ts` — 2 passed

Android `compileDebugKotlin` succeeded. `BusinessActionRegistryTest` 12 passed. `BusinessPulsePresentationTest` 9 passed, including `recentHeadingAndOtherActorCards`.

iOS `recentTitleIsCompanyActivity` and `otherMemberActivityMapsWithOrWithoutActorName` were written and were not run on Windows.

## 10. Runtime verification

Not run. No device pass of Pulse Recent, the Moments timeline, or a second signed-in member refreshing after a save.

## 11. Remaining gaps

These stay open and are frozen as separate work. None of them blocks the Moments stream unless a later check shows a B0.5 item changing activity itself.

- **Cross-user realtime** — later correctness and UX improvement. `REALTIME_GAP`: events created by another member may require refresh before appearing. SSE still delivers only to the acting user. Refresh already returns the authorized company feed.
- **Location-scoped totals** — separate backend and product decision. Finance and cash totals stay company-wide.
- **Device cash smoke** — runtime gate for B1. Not run in this gap.
- **Remaining B0.5 projection fixes** — still to be completed, and they do not block the Moments stream unless they affect activity itself. Company-level pulse, life, and memory rows can still be overwritten by another moment. The memory read can still serve another moment’s items from the company blob. The observer deny list is still incomplete outside the expense path exercised here.

## 12. B2 readiness

YES — Business activity is now a company-authorized, moment-scoped event stream.

B2 is unblocked. Do not do Gap 2 (cross-user realtime) first. B2 was not started in this gap.

B2 contract: company + selected Business moment → authorized event feed → `BusinessMomentCardModel` → shared Moments stream, with actor metadata when the profile has a name.

B2 acceptance: Moments represents what happened in the selected Business moment for the company, subject to authorized membership and current refresh state.

`REALTIME_GAP`: events created by another member may require refresh before appearing. B2 must not claim live cross-user updates.

Life remains a company-wide stream across that company’s moments. The activity endpoint and Pulse Recent are the selected moment only.
