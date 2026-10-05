# Android known blockers

Verified by driving `emulator-5556` with Maestro CLI 2.11.0 against
`com.example.momentra`. Every number below was read off the live UI, not assumed
from the seed data.

## What the Business surface actually offers

Business Quick Add lists exactly eight record types:

| Quick Add action | Fixture type |
|---|---|
| Log Revenue | Revenue |
| Log Expense | Expense |
| Invoice Track | Invoice |
| Tax Entry | Tax Entry |
| Investor Update | Investor Update |
| Budget Alert | Budget Alert |
| Forecast Update | Forecast Update |
| General Update | General Update |

There is no family grid and no generic "Create Moment" record form. The moment
directory's create path opens a per-family **setup wizard** ("Activate Money &
Cash Flow"), not a record form, so it cannot substitute for the missing types.

Searching Quick Add for `cash` or `spend` returns "No actions match this
search." The following 18 fixture types have no create form of any kind:

Spend, CashSale, Vendor, Issue, Approval, Team Update, Update, Improvement,
Budget Review, SLA, Decision, Meeting, Recognition, Milestone, Risk, Khata,
Retrospective, Activity Log, Tax Entry*.

\* Tax Entry, Investor Update, Forecast Update, Budget Alert and General Update do
exist as Quick Add actions but carry no amount in the checked totals.

## Consequence for the expected totals

| Check | Expected | Reachable | Why |
|---|---:|---:|---|
| Business records | 300 | 85 | 8 of 26 types exist |
| Business revenue-bearing | ₹712,500 | ₹693,000 | CashSale ₹19,500 has no form |
| Business expenses | ₹449,700 | ₹449,700 | Log Expense works (one at a time) |
| Store split 113/95/92 | ✓ | impossible | no store control on any sheet |

Per-record these figures are reachable and verified. In bulk they are not — see
the open blocker below.

## Dates are no longer a blocker

Both platforms open a Material3 calendar:

- Personal WHEN control → dropdown (Now/Today/Yesterday/**Custom**), then the
  calendar.
- Business DATE row (Log Revenue, Add Expense) → the calendar directly.
- Create Invoice has separate ISSUE DATE and DUE DATE rows.

Month paging works via the `Change to previous month` control. The picker loop in
`flows/common/set_date_picker.yaml` pages back until the target month header is
actually on screen rather than trusting a tap count, because the grid animation
swallows a tap and lands one month short.

## Amount entry bug (fixed in the app)

The business amount fields prepended a decorative `"₹ "` **into** the editable
text of a `BasicTextField(value: String)`, which has no selection to adjust. The
caret was therefore two characters left of where the user was typing on every
keystroke, and `toLongOrNull()` then discarded the resulting leading zeros.
Digit-at-a-time input was silently scrambled:

| typed | stored (before fix) |
|---|---|
| 1111 | 1111 |
| 1234 | 3421 |
| 1000 | 10 |
| 2500 | 502 |
| 9800 | 809 |

Bulk text injection (a single `adb shell input text`) was unaffected, which is
why this only ever showed up under automation.

Fixed in three copy-pasted locations by moving the rupee sign out of the
editable value and pinning the selection to the end of the reformatted text, and
by grouping digits without a `Long` round-trip:

- `ui/shell/business/runway/components/RunwaySheetChrome.kt` (Log Revenue)
- `ui/shell/business/ops/components/OpsSheetChrome.kt` (Add Expense)
- `ui/shell/business/teamops/components/TeamOpsSheetChrome.kt`

Personal amount fields were never affected: `PersonalMoneyQuickAddSheets.kt`
renders the ₹ as a sibling `Text` and does not reformat.

After the fix, every type was verified **individually** with an exact amount and
an exact past date:

| Probe | Result |
|---|---|
| Log Revenue ₹9,800 dated Jun 1 2026 | Revenue 0 → 9,800.0000 exact |
| Add Expense ₹45,500 dated Jun 18 2026 | Outflow 5,115 → 50,615 exact (QA company) |
| Add Expense ₹45,500 dated Jun 18 2026 | Outflow 325 → 45,825 exact (Gate QA company) |

## Open blocker: bulk runs are not reliable

Individual probes are exact, but the same `add_record.yaml` driven in a
back-to-back `repeat` loop loses most records. Measured on a clean
`Momentra Gate QA` company after seeding chunks 01–06 (60 records):

| Metric | Expected | Actual | Delivered |
|---|---:|---:|---:|
| Revenue | 702,800 | 477,900 | 468,100 of 693,000 seed |
| Outflow | 449,700 | 325 | 325 of 449,700 |

Chunks 01–05 reported **no Maestro failures** — every command completed — yet
almost no expense landed. The records that did save carried scrambled amounts
(₹25, ₹24 where 6,800 / 12,000 were typed).

So the failure is silent and state-dependent, not an assertion error. Running the
identical flow as a single-record probe reproduces the correct amount every
time. The leading hypothesis is that `tapOn: id: bottom.quickadd` fires before the
previous sheet has finished dismissing, so the next sheet opens with stale
geometry and the fixed-percentage field taps land on the wrong rows.

Before trusting any bulk number, re-run the target chunk as single-record probes
or add an explicit wait for the previous sheet to close and re-verify the total.

## Known remaining instability

`Create Invoice` (Invoice Track) is not reliable to drive:

- The sheet restores a previous draft, so field contents and positions differ
  between runs.
- Field rows shift when the IME opens; absolute percentage taps land on the
  wrong row.
- ISSUE DATE and DUE DATE render the same `Mon D, YYYY` shape.
- `hideKeyboard` fails on this sheet ("app uses a custom input"), so BACK is
  used instead.

Invoice rows are excluded from the chunk filter for now. They add no revenue or
expense, so they do not affect any money total.