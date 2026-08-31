# Phase 8 — Production Expense Create

## Status

**COMPLETE** (Personal E2E gate) with documented **API_GAP / DEFERRED** for Group/Business splits, Paid From, category catalogs, and Expense.Update.

## Canonical ownership

Finance owns `Expense` (and `Movement`, not implemented this phase). There is **one** Finance expense engine:

- Route: `POST /v1/moments/{momentId}/expenses`
- Service: `backend/typescript/src/modules/finance/service.ts` → `createExpense`
- No `/v1/finance/expenses` alias (Phase 2 NOT_REQUIRED)

## Personal Expense transaction

Inside `runCommand` + DB transaction:

1. Governance `EXPENSE_CREATE`
2. Resolve PERSONAL moment ownership
3. Insert `finance.expense`
4. Insert `finance.personal_expense_context`
5. Domain event `ExpenseRecorded` + outbox
6. Audit `EXPENSE_CREATE`
7. O(1) `projection.recent_activity` row
8. Response includes typed `projectionHints`: `personal.activity`, `personal.pulse`, `personal.memory`

No AI / FCM / external I/O inside the transaction.

## Money

- OpenAPI: decimal **string** `^[0-9]+(\.[0-9]{1,4})?$` + `currencyCode` (3-letter)
- Backend: `decimal.js` via `parseMoney` → `toFixed(4)`
- Clients: `ExpenseMoney` helpers — never Double/Float canonical state

## Personal form (Android + iOS)

Amount-first compact UI:

- Amount + currency
- More details: merchant, note (`description`)
- No Paid From (API_GAP)
- No category picker (no catalog API; optional `categoryCode` not exposed as fake taxonomy)
- No splits UI

Entry: Personal context + active Moment + Create tab → Expense form. Requires `selectedMomentId`.

## Idempotency

- Header `Idempotency-Key` required
- Client stores key per draft `expense:{momentId}:{draftId}` until success
- Retry reuses same key; “Add another” generates a new draft id / key

## Scoped refresh

After create: **do not** `reloadCurrentContext()`. Consume `projectionHints` and optionally refresh `GET /v1/personal/activity`.

Authoritative Activity comes from the read API (written during create into `projection.recent_activity`). No client-fabricated timeline append as truth.

## Group / Business

Same Finance engine tables exist (`group_expense_context`, `business_expense_context`, `expense_share`). Create path remains **PERSONAL-only**. Splits OpenAPI fields exist but Zod `.strict()` rejects them — **API_GAP** for write.

## Tests

| Suite | Result |
|-------|--------|
| `tests/expense-create.test.ts` | 8/8 PASS |
| Android `ExpenseMoneyTest` | PASS |
| iOS `ExpenseMoneyTests` | sources added (run on macOS/Xcode) |

## Files changed (high level)

- Backend: finance service Zod, router hints, OpenAPI bind, expense-create tests
- Android: ExpenseCreate* stack, shell wiring, Activity DTO/API, money tests
- iOS: APIClient createExpense, ExpenseCreate* stack, shell wiring, money tests
- Docs: this file + figma mapping + contract matrix + status

## Gaps

See `PHASE_8_FINANCE_CONTRACT_MATRIX.md`.

## Non-goals (STOP)

Movement, Budget, Settlement, Vendor.Create, Goal/Milestone/Task, Phase 9 Work Kernel.
