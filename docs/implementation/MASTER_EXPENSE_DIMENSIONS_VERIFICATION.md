# Master Expense dimensional routing — verification report

## A. Relationships activity delete — root cause

View Activity showed a trash/delete control, but Android/iOS only filtered **local list state**. There was no `DELETE` relationship-activity API (unlike Lifestyle). Refresh restored rows. Android item ids were often `occurredAt+title` without `activityId`.

## B. Delete architecture

```
View Activity → DELETE /v1/moments/:momentId/relationship-activities/:activityId
  → voidRelationshipActivity (status VOIDED)
  → recent_activity payload status VOIDED
  → refreshRelationshipsBondAxes + recomputeOverallWellbeing
  → GET activity / Pulse / Life Rel chip
```

Master-Expense-derived activities (ACTIVE `expense_dimension_contribution` link) return **409** — edit/void the expense instead.

## C. Master Expense fan-out — root cause (runtime “not appearing”)

Analytical Rel/Lifestyle rows were created and axes refreshed, but Master Expense did **not** write `projection.recent_activity` on Rel/Lifestyle moments. Moments View Activity reads that feed → empty. Silent INACTIVE when setups missing also violated §1E.

**Fix:** upsert Rel/Lifestyle `recent_activity` with `source: MASTER_EXPENSE`; error if non-SELF without Rel setup or Lifestyle-eligible without Lifestyle setup.

## D. Architecture (unchanged financial model)

```
Master Expense UI (sharedExperienceCode)
  → POST/PATCH/void /v1/moments/:momentId/expenses
  → finance.expense (one row; LO home)
  → expense_dimension_contribution (LO / Rel / Lifestyle / FB)
  → relationship_activity / lifestyle_activity
  → recent_activity on Rel/Lifestyle moments (feed visibility)
  → bond / lifestyle axes + wellbeing blend
```

## E. Files changed (this pass)

| File | Change |
|------|--------|
| `finance/expense-dimensions.ts` | recent_activity upsert/void; missing-setup errors |
| `personal/relationships-precision.ts` | `voidRelationshipActivity` |
| `api/v1/router.ts` + `openapi/momentra-v1.yaml` | DELETE relationship-activities |
| APK/iOS Rel activity sheets + API | stable activityId; call DELETE; hide ME-derived delete |
| `tests/master-expense-rel-fix.test.ts` | R1–R10 coverage |

## F. API

- `DELETE /v1/moments/{momentId}/relationship-activities/{activityId}` → `{ activityId, title, status }`

## G. Database

No new migrations. Uses existing `VOIDED` status + contribution links.

## H–I. Controlled tests (`master-expense-rel-fix.test.ts`)

| ID | Result |
|----|--------|
| R6 FRIEND dining + Rel/LS feeds | PASS |
| R7 SELF dining | PASS |
| R8 SELF utility | PASS |
| R9 FRIEND→SELF | PASS |
| R10 void expense | PASS |
| Missing Rel setup → 400 | PASS |
| R1–R5 delete / idempotent / ownership / ME 409 | PASS |

## J. Remaining limitations

- Rel/Lifestyle Moments list only shows rows with `activityPayload.activityId` (intentional for deletable identity).
- HEALTH/SHOPPING/TRANSPORT/OTHER still Lifestyle NO by design.
- Edit Transaction category encoding on mobile can still affect Lifestyle eligibility on PATCH (pre-existing).
- Bundled OpenAPI may lag until regenerate.
