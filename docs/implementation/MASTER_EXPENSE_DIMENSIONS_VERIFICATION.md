# Master Expense dimensional routing — verification report

## A. Root cause
Master Expense posted solely to shell `selectedMomentId`. Shared Experience was prose in `description`. No Relationships/Lifestyle precision feed. Classification was selection, not rules.

## B. Architecture implemented
```
Master Expense UI (structured sharedExperienceCode)
  → POST/PATCH/void /v1/moments/:momentId/expenses
  → finance.expense (one row; moment_id = LIFE_OPERATIONS setup when present)
  → finance.expense_dimension_contribution (LO / Rel / Lifestyle / FB)
  → relationship_activity (non-SELF) + lifestyle_activity (eligible category)
  → refreshRelationshipsBondAxes / refreshLifestylePulseAxes
  → spendByCurrency + personal_finance_snapshot (canonical amount once)
```

## C. Mobile UI — SELF added
- Android: `PersonalMasterExpenseTheme.sharedExperienceOptions` includes Self; default `SELF` in `PersonalMasterExpenseSheet` + Edit Transaction chips.
- iOS: same in `PersonalMasterExpenseTheme` / `PersonalMasterExpenseSheet` / `PersonalEditTransactionSheet`.

## D. API
- `sharedExperienceCode`, `sharedExperienceLabel` on create/update Zod schemas and expense detail.

## E. Database (V076)
- `finance.expense.shared_experience_code` (+ label)
- `finance.expense_dimension_contribution`
- `expense_resource_link` types: RELATIONSHIP_ACTIVITY, LIFESTYLE_ACTIVITY

## F. Lifestyle eligibility
See `lifestyle-eligibility.ts` — FOOD dining subs YES; GROCERIES NO; CAFE/ENTERTAINMENT YES; BILLS/HOUSING/TRANSPORT/HEALTH/SHOPPING/OTHER NO.

## G. Relationships rule
`shared_experience_code != SELF` → ACTIVE RELATIONSHIPS contribution + SHARED_EXPERIENCE activity on Relationships setup moment.

## H. Future Building exclusion
Ordinary Master Expense always INACTIVE FUTURE_BUILDING (no investment category path).

## I. Key files
| File | Change |
|------|--------|
| frds/migrations/personal/V076__… | Schema |
| finance/expense-dimensions.ts | Sync orchestration |
| finance/lifestyle-eligibility.ts | Category map |
| finance/service.ts | Canonical LO moment + sync hooks |
| PersonalMasterExpenseSheet.kt/.swift | SELF UI + subcategory |
| PersonalEditTransactionSheet | Round-trip shared code |
| tests/expense-dimensions.test.ts | T1–T10 + four-dinner |

## J. Tests
`npx tsx --test tests/expense-dimensions.test.ts` → **9/9 pass** (eligibility + T1–T10 + four-dinner).

## K–L. Four-dinner / double-count
Four POSTED expenses sum to **9000**; contribution rows never summed for money; posting via Future moment still lands on LO with FB INACTIVE.

## M. Limitations
- Rel/Lifestyle activities skipped if those setups are missing (contribution INACTIVE).
- HEALTH/SHOPPING/TRANSPORT treated Lifestyle NO (ambiguous).
- OpenAPI generated clients may lag until `openapi:generate` is re-run (hand DTOs updated).
