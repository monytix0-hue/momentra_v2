# Business B2 — Unified Moments

Date: 2026-09-30.

Classification: **PASS CANDIDATE — runtime verification pending**

Money, Daily, and Team now share one Moments timeline. It reads the Gap 1 activity feed for the selected moment. No device pass was run on this Windows host. iOS sources and tests were written and were not compiled here.

## 1. Executive status

**PASS CANDIDATE — runtime verification pending**

The screen no longer mixes finance, a second timeline, progress, or month deltas into Moments. Cards come from activity rows. Filters are the codes those rows actually use. A late response from the previous moment cannot replace the selected feed.

## 2. Before/after architecture

Before: each family had its own Moments body. Money also loaded finance, `getMomentTimeline`, progress, and month deltas, and filtered by substring. Daily and Team did the same with their own chips, including Vendors and Milestones.

After:

```text
AppShell
  → selected Business moment
  → GET /v1/business/moments/:id/activity
  → BusinessMomentsPresentation
  → BusinessMomentCardModel
  → BusinessMomentsScreen
```

Family files remain and only pass the family into that screen. Pulse still uses its own five-row bundle. Life and Memory were not changed.

## 3. Files changed

Android:

- `BusinessMomentsPresentation.kt`, `BusinessMomentsScreen.kt` (new)
- `BusinessMomentCardModel` gained optional `subtitle` and `statusLabel`
- `ActivityPayloadDto` gained the business fields the mapper reads
- `RunwayMomentsActiveContent.kt`, `OpsMomentsActiveContent.kt`, `TeamOpsMomentsActiveContent.kt`, `BusinessMomentsActiveContent.kt` are thin wrappers
- `BusinessMomentsPresentationTest.kt` (new)

iOS, not compiled here:

- `BusinessMomentsPresentation.swift`, `BusinessMomentsScreen.swift` (new)
- `BusinessMomentCardModel` and `ActivityPayload` gained the same optional fields
- `RunwayMomentsActiveView.swift`, `OpsMomentsActiveView.swift`, `TeamOpsMomentsActiveView.swift`, `BusinessMomentsActiveView.swift` are thin wrappers
- `momentraTests/BusinessMomentsPresentationTests.swift` (new, not run)

No backend files changed.

## 4. Shared components introduced

`BusinessMomentsPresentation` maps one activity row to one card, chooses family chips, keeps server order, and inserts Today, Yesterday, or `d MMM` when the local date changes. `BusinessMomentsFeed` is the load gate: a page applies only when the moment id and generation still match. `BusinessMomentsScreen` is the shared body. `sourceActivityIds` is the payload id when one exists. Rows are not clustered.

## 5. Event feed contract

The feed is company-authorized and selected-moment scoped.

Moments calls `getActivity` with cursor pages of 20, up to the server cap of 50, and follows `nextCursor`. It does not build the timeline from Pulse, Life, Memory, the finance snapshot, or Quick Add history. The heading is the family title plus “Company activity”, not “Recent business activity”, because the cursor can walk the rows that were written.

## 6. Event-to-card provenance

| Visible field | Source |
| --- | --- |
| Title | Server `title` when present. Otherwise a short label from the code. |
| Amount | `amount` plus currency. Invoice uses `totalAmount`. INR and USD get a symbol. A missing currency leaves the number. |
| Subtitle | Expense `categoryCode` only. |
| Actor | `by {actorDisplayName}` when the trimmed name is non-empty. |
| Time | Local time from parsed `occurredAt`. |
| Status | Payload `status`, or issue `severity` when status is absent. |

Invoice payloads have no party name, so no party line is shown. A blank actor omits the actor line. Unknown codes such as a vendor update do not invent a vendor title or subtitle. Cards are not tappable. There is no invoice, issue, or approval detail destination in the shell.

## 7. Family filters

Chips exist only for codes the writers emit.

Money: All, Revenue (`BUSINESS_REVENUE`), Expenses (`BUSINESS_EXPENSE`), Invoices (`BUSINESS_INVOICE`). Tax, forecast, investor, budget, and review rows stay on All.

Daily: All, Spend (`BUSINESS_EXPENSE`), Issues (`ISSUE_REPORTED`), Updates (`BUSINESS_UPDATE`, `IMPROVEMENT_LOGGED`), Approvals (`APPROVAL_REQUESTED`, `APPROVAL_APPROVED`, `APPROVAL_REJECTED`).

Team: All, Updates (`BUSINESS_UPDATE`, `ACTIVITY_LOGGED`), Decisions (`DECISION_RECORDED`), Approvals (the same three codes). Meeting, recognition, retro, and risk stay on All.

No Vendor, Khata, SLA, or Milestone chip. Those writes do not insert `recent_activity`.

## 8. Empty/loading/error behavior

Loading is a spinner only when this moment has no list yet. “Nothing recorded yet” plus the family line appears only for a successful empty feed, with Add something opening the existing Quick Add. “No expenses yet” (and the same shape for the other chips) appears when the feed has rows and the filter does not. Show all returns to All. A failed load with no rows shows the error and Try again, not the empty copy. A failed refresh keeps the last good rows and shows the error above them.

## 9. Cache and switching behavior

Moment ids are UUIDs. The Pulse cache stays keyed by `momentId` and does not store Moments pages. Changing moment clears the visible list immediately and bumps a generation. A Money page that finishes after Daily was selected is ignored. The same rule covers Company A to Company B, because that switch is a different moment id. Same-moment refresh keeps the previous rows until the new first page arrives. A local Business write still invalidates Pulse and bumps `businessTabRefreshToken`, which reloads Moments. Personal and Group caches were not touched.

## 10. Android/iOS parity

Both platforms share the chip sets, card mapping, actor line, date groups, empty and error copy, generation gate, cursor load-more, and the existing Quick Add callback. Native layout differs. iOS was not compiled on this host.

## 11. Tests

Android `compileDebugKotlin` succeeded.

`BusinessMomentsPresentationTest` 9 passed, 0 failed: money mapping, daily issue and update with no invented vendor card, team update decision and approval, filters, equal-timestamp order, Today / Yesterday / 28 Sep, empty versus filter-empty versus error, moment and company switch with a stale page, and another member’s row after a refreshed list.

`BusinessPulsePresentationTest` 9 passed. `BusinessActionRegistryTest` 12 passed.

iOS `BusinessMomentsPresentationTests` has the same cases and was not run.

Backend, 0 failed, no B2 code changes:

- `business-gap1-activity.test.ts` — 7 passed
- `business-s4-finance.test.ts` — 4 passed
- `business-company-life.test.ts` — 3 passed
- `business-ops-three-layer-join.test.ts` — 2 passed

`business-b05-correctness.test.ts` is not in the repo. It was not created.

## 12. Known gaps

- `REALTIME_GAP`: events created by another member may require refresh before appearing.
- Location-scoped totals remain a separate product decision.
- B1 device cash smoke was not run.
- B0.5 projection issues remain: company pulse, life, and memory rows can still be overwritten by another moment, and the memory read can still serve another moment’s items from the company blob.
- Khata, vendor updates, SLA checks, and milestones do not emit activity, so they cannot appear on this timeline until those writers do.

## 13. B3 readiness

No. Business Life is not safe to redesign next as this moment’s health.

GET life still reads one company row. Its activity query joins every active moment in the company. Another moment’s refresh can overwrite that row. A Life screen that presents those fields as the selected moment would blur the company contract. Company-wide Life can be shown honestly only after that contract is explicit. B3 was not started.
