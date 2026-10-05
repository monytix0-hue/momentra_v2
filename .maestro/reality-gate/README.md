# Maestro Reality Gate

Deterministic Quick Add soak for the installed Momentra app. The 400 records live once in `data/`. Android and iOS each have their own flows so Maestro can match the installed app id.

> **iOS money forms can be submitted.** Personal Add expense, Log Expense, Log
> Revenue, and Invoice Track are labeled for Maestro (`personal.expense.*`,
> `business.expense.*`, `business.revenue.*`, `business.invoice.*`). Daily and
> Team record types still have no create sheet, and the sheet cannot set a
> past date or a store, so the totals table below is still **not reachable**.
> Read `ios/KNOWN_BLOCKERS.md`.

| Workspace | Open this folder in Maestro Studio | App id |
|---|---|---|
| Android APK | `.maestro/reality-gate/android` | `com.example.momentra` |
| iOS | `.maestro/reality-gate/ios` | `resolvingpoint.momentra` |

iOS runs on a Mac. This Windows host cannot drive the iOS simulator.

## Dedicated account

These flows create real records through the UI. Use a dedicated QA account and company.

Business flows tap **Momentra Multi Store QA**. That company already holds the Gate 2 baseline (revenue â‚¹10,102). Seeding it will not match the totals below. Do not run this gate on that company.

## Order

Sign in first. Then, from the platform folder:

1. `validate_surfaces.yaml` â€” non-destructive; asserts every surface in both
   Personal and Business context. Safe to run any time.
2. `ios/flows/personal/add_record.yaml`, `ios/flows/business/add_expense.yaml`,
   `add_revenue.yaml`, and `add_invoice.yaml` â€” one record each.

Personal chunks save through Add expense. Business chunks save Expense, Spend,
Revenue, CashSale, and Invoice. Any other business type stops on the first row
and names that type. `business_chunk_01.yaml` starts with Revenue.

## Android blockers (verified on this build)

Measured against the emulator, not assumed. See `android/KNOWN_BLOCKERS.md`.

- **Business has no store control.** None of Log Revenue, Log Expense, or Create
  Invoice exposes a location/store field, so the store split below cannot be
  reproduced at all.
- **Business Quick Add exposes 8 of the 26 fixture types**: Log Revenue, Log
  Expense, Invoice Track, Tax Entry, Investor Update, Budget Alert, Forecast
  Update, General Update. **Spend and CashSale have no form**, so the README's
  claim above that business chunks save them is wrong. The remaining 16 types
  (Team Update, Approval, Vendor, Issue, Meeting, Recognition, Milestone,
  Decision, Risk, SLA, Khata, Update, Tax/Investor/Forecast siblings,
  Improvement, Budget Review, Retrospective, Activity Log) have no create sheet.
- **Dates are reachable on both platforms.** The Personal WHEN control and the
  business DATE row both open a Material3 calendar with month paging, so the
  "dates only offer Now / Today / Yesterday" note below is obsolete.
- **The chunk filters skip unsupported types** rather than aborting on the first
  one, so a chunk contributes its supported rows instead of stopping.

## Expected totals

From `data/expected_totals.json`. **Personal is reachable. Business is not.**
See `android/KNOWN_BLOCKERS.md` for what the UI actually offers. Dates are no
longer a blocker on either platform. Business `revenueTotal` is 35 Revenue rows
plus 5 CashSale rows; the CashSale half has no create form, so the ceiling is
Rs 693,000, not Rs 712,500. Invoice creation and Khata collections are not added
again.

| Check | Expected | Reachable |
|---|---:|---:|
| Personal records | 100 | 100 |
| Personal spends | â‚¹112,481 | â‚¹112,481 |
| Business records | 300 | 85 (60 money-bearing) |
| Business revenue-bearing total | â‚¹712,500 | â‚¹693,000 |
| Business expenses | â‚¹449,700 | â‚¹449,700 |
| CashSale component | â‚¹19,500 | 0 â€” no form |

Store split: Main Store 113, North Store 95, South Store 92. **Not reachable** â€”
no business sheet has a store control. Dates run from 1 Jun 2026 through 1 Oct
2026 and are settable on both platforms.

## Selector hooks

If a date control or field label differs on one platform, change only that platformâ€™s copy of:

- `flows/common/set_when.yaml`
- `flows/personal/fill_record.yaml`
- `flows/business/fill_record.yaml`

Leave `data/` untouched.
