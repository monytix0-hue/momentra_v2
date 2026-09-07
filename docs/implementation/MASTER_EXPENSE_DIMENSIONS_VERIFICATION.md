# Master Expense dimensional routing — verification report

## A. Root cause
Master Expense originally posted solely to shell `selectedMomentId`. Shared Experience lived as prose in `description`. There was no Relationships/Lifestyle precision feed. Classification was shell selection, not independent dimensional rules.

## B. Architecture implemented
```
Master Expense UI (structured sharedExperienceCode, default SELF)
  → POST/PATCH/void /v1/moments/:momentId/expenses
  → finance.expense (one row; moment_id = LIFE_OPERATIONS setup when present)
  → finance.expense_dimension_contribution (LO / Rel / Lifestyle / FB)
  → relationship_activity (non-SELF; investment_value mirrored from amount)
    + lifestyle_activity (eligible category)
  → refreshRelationshipsBondAxes / refreshLifestylePulseAxes
  → spendByCurrency + personal_finance_snapshot on create/update/void
     (canonical amount once — never summed across contributions)
```

Gap-closure hardening (this pass):
- Dimensional sync is **mandatory** (no silent try/catch swallow).
- Finance snapshot refreshes on **update and void**, not only create.
- V077 grants + RLS on `expense_dimension_contribution`.
- Expense GET returns `contributions[]`.
- Rel activity stores `investment_value` from the canonical expense amount.

## C. Mobile UI — SELF
- Android: `PersonalMasterExpenseTheme.sharedExperienceOptions` includes Self; default `SELF` in create + edit.
- iOS: same. Codes: `SELF`, `SPOUSE` (label Spouse / Partner), `FAMILY`, `FRIEND`, `COLLEAGUE`, `OTHER`.
- DTOs accept optional `contributions` on expense detail (no form UI required).

## D. API / data model
- Create/update: `sharedExperienceCode`, `sharedExperienceLabel`.
- GET expense: same fields + `contributions[]` (`dimensionCode`, `status`, `targetMomentId`, `linkedResourceType`, `linkedResourceId`).
- OpenAPI: `ExpenseCreateRequest`, `ExpenseUpdateRequest`, `ExpenseDetail`, `ExpenseDimensionContribution` in `schemas/common.yaml`; paths wired in `momentra-v1.yaml`.

## E. Database
| Migration | Purpose |
|-----------|---------|
| V076 | `shared_experience_*` on expense; `expense_dimension_contribution`; resource link types |
| V077 | Grants + RLS for `expense_dimension_contribution` |

## F. Lifestyle eligibility mapping
From `lifestyleEligibilityCatalog()` / `lifestyle-eligibility.ts`:

| Category | Subcategory | Lifestyle? | Rule |
|----------|-------------|------------|------|
| FOOD | DINING_OUT / FOOD_DINING / TAKEAWAY / COFFEE / CELEBRATIONS | YES | dining subcategory |
| FOOD | GROCERIES | NO | groceries exclusion |
| FOOD | (null) | YES | FOOD default without GROCERIES |
| CAFE | — | YES | category allowlist |
| ENTERTAINMENT | — | YES | category allowlist |
| HEALTH / SHOPPING / TRANSPORT / OTHER | — | NO | ambiguous → NO |
| HOUSING / BILLS | — | NO | ops spend |

## G. Relationship routing
`shared_experience_code != SELF` **and** Relationships setup moment exists → ACTIVE RELATIONSHIPS + `SHARED_EXPERIENCE` activity (`investment_value` = expense amount).  
`SELF` → RELATIONSHIPS INACTIVE (prior activity voided).

## H. Future Building exclusion
Ordinary Master Expense always sets FUTURE_BUILDING **INACTIVE** (no investment/keyword path).

## I. Key files changed (gap closure)
| File | Change | Reason |
|------|--------|--------|
| `finance/service.ts` | Mandatory sync; snapshot on update/void; GET contributions | RTF §19–§22 |
| `finance/expense-dimensions.ts` | Require V076; Rel `investment_value`; list contributions | Sync + Rel mirror |
| `V077__…grants_rls.sql` + `MIGRATION_ORDER.txt` | Grants/RLS | Ops hardening |
| `openapi/schemas/common.yaml`, `momentra-v1.yaml` | sharedExperience + contributions | Contract sync |
| `Dto.kt` / `APIClient.swift` | contributions on expense detail | Client decode |

## J. Tests
`npx tsx --test tests/expense-dimensions.test.ts` → **9/9 pass** (eligibility + T1–T10 + four-dinner).

| Test | Result |
|------|--------|
| Lifestyle eligibility mapping | PASS |
| T1 Self dinner | PASS |
| T2 Self groceries | PASS |
| T3–T5 Spouse/Family/Friend dinners | PASS |
| T6–T7 SELF↔FRIEND edit | PASS |
| T8 Dinner→Bills category | PASS |
| T9 idempotent retry | PASS |
| T10 void | PASS |
| Four-dinner total 9000 | PASS |

## K–L. Four-dinner / double-count
Four POSTED expenses sum to **9000**. Contribution rows are associations only — financial aggregation uses `finance.expense` once via `spendByCurrency` / snapshots.

| Expense | LO | Relationships | Lifestyle | FB |
|---------|----|---------------|-----------|-----|
| Family ₹3000 | YES | YES | YES | NO |
| Friends ₹2000 | YES | YES | YES | NO |
| Wife ₹1500 | YES | YES | YES | NO |
| Self ₹2500 | YES | NO | YES | NO |

## M. Remaining limitations
- Rel/Lifestyle activities skipped (contribution INACTIVE) if those life-system setups are missing.
- HEALTH/SHOPPING/TRANSPORT/OTHER treated Lifestyle NO (ambiguous by design).
- Bundled OpenAPI (`momentra-v1.bundled.yaml`) may lag until regenerate; source `common.yaml` / `momentra-v1.yaml` updated.
- LO feed is spend + `EXPENSE_RECORDED` + LO contribution (no separate `life_operation_observation` from Master Expense).
