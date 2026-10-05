# Known blockers on the iOS Reality Gate

Measured against app id `resolvingpoint.momentra`. The three money forms that
the gate needs can now be filled by accessibility id and submitted:

| Form | Amount id | Submit id |
|---|---|---|
| Personal Add expense | `personal.expense.amount` | `personal.expense.submit` |
| Log Expense | `business.expense.amount` | `business.expense.submit` |
| Log Revenue | `business.revenue.amount` | `business.revenue.submit` |
| Invoice Track | `business.invoice.amount` | `business.invoice.submit` |

Invoice also needs `business.invoice.number` and `business.invoice.line`. Quantity
stays at 1. After a save, the flow taps `business.invoice.submit_done`.

`tapOn(text)` is still a full-string match. `AMOUNT` does not match `Amount`.
Prefer `id:`.

---

## What still cannot be created

| Seed needs | Installed build |
|---|---|
| 4 personal families and 20 personal types | one expense sheet; every personal row is saved as an expense |
| personal dates 2026-06-01 → 2026-10-01 | `WHEN` offers only Now / Today / Yesterday |
| 26 business types | Expense, Revenue, CashSale, and Invoice |
| store split Main / North / South | no location control |
| status Approved, Pending, Paid, … | no status control |

Daily and Team types (`Activity Log`, `Meeting`, `Milestone`, and the rest)
stop the business chunk on the first such row. The failure text names the type.

## Totals

`data/expected_totals.json` (100 personal / 300 business, the revenue and
expense sums, the store split) is still not reachable. Money rows can be saved.
They land on today, with no store, and non-money types are not created.
