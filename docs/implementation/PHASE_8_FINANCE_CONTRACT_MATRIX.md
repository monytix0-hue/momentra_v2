# Phase 8 — Finance Contract Matrix

| Capability | OpenAPI | Runtime | Status | Notes |
|------------|---------|---------|--------|-------|
| Expense.Create | `POST /v1/moments/{momentId}/expenses` | Implemented (PERSONAL) | **CONTRACTED + IMPLEMENTED** | Request body bound to `ExpenseCreateRequest` |
| Standalone `POST /v1/finance/expenses` | — | — | **NOT_REQUIRED** | Do not add alias |
| Expense.Read (list by id) | No dedicated GET expense | Activity projection | **PARTIAL** | Read-after-create via `GET /v1/personal/activity` + DB row |
| Expense.Update | — | — | **API_GAP** | No PATCH in inventory |
| Money amount | decimal string | decimal.js | **PASS** | Max 4 fractional digits |
| Multi-currency | `currencyCode` | stored | **PASS** | No FX conversion |
| Category | optional `categoryCode` string | stored free-form | **API_GAP** catalog | No GET categories; UI does not fake taxonomy |
| Subcategory | — | — | **API_GAP** | Not in schema |
| Paid From / account | — | DB `financial_account_id` unused | **API_GAP** | Do not fake Cash/Bank |
| Splits (EQUAL/EXACT/PERCENTAGE/SHARES) | `ExpenseSplitLine` in schema | Zod rejects `splits` | **API_GAP** write | Schema present; persistence DEFERRED |
| Group participants | participants APIs exist | expense create PERSONAL-only | **DEFERRED** | Same engine tables ready |
| Business company scope | — | PERSONAL-only create | **DEFERRED** | |
| Business location | company locations exist | not on expense create | **NOT_APPLICABLE** this phase | |
| Vendor | — | — | **API_GAP — VENDOR_COMMAND** | Do not implement Vendor.Create |
| Idempotency | required header | `runCommand` | **PASS** | |
| ProjectionHints | CommandEnvelope | activity/pulse/memory | **PASS** | |
| Audit / domain event / outbox | — | EXPENSE_CREATE / ExpenseRecorded | **PASS** | |
| Split rounding residual | documented Phase 2 gap | — | **FINANCE_ROUNDING_GAP** | Does not block Personal |

## Decision record

Phase 8 acceptance blocker = **Personal Expense E2E** only. Group/Business stay on the same Finance engine without inventing unsupported write paths.
