# Phase 8 — Expense Create Figma Mapping

Figma file: [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra)

| Figma node/frame | Context | Field/action | Android | iOS | API | Status |
|------------------|---------|--------------|---------|-----|-----|--------|
| `1035:7757` Personal expense FAB | Personal Pulse / Moments / Life / Memory | ₹+ floating action button → Master Expense | `PersonalExpenseFab` in `AppShellScreen` | `PersonalExpenseFab` in `AppShellView` | — | MATCHED |
| `453:9376` Master Expense | Personal (Life Ops / Lifestyle / Relationships) | Full premium create form | `PersonalMasterExpenseSheet` + `PersonalMasterExpenseTheme` | same | `POST .../expenses` | MATCHED (premium visual parity) |
| `453:9376` | Personal | Purpose / title | purpose field | purpose field | `merchantName` / `description` | MATCHED |
| `453:9376` | Personal | Amount hero | amount field | amount field | `amount` | MATCHED |
| `453:9376` | Personal | 8-category grid (2×4, 72px tiles) | `PersonalExpenseCategoryCatalog` grid | same | `categoryCode` | MATCHED (static catalog) |
| `453:9376` | Personal | Paid From row card + Change | row card → `PersonalAccountPickerSheet` | same | `financialAccountId` | MATCHED |
| `353:21778` Add Account | Life Ops money flows | Type chips, name, balance (UI), currency, default toggle (UI) | `PersonalAddAccountSheet` | same | `POST /financial-accounts` | MATCHED (balance/default UI-only v1) |
| `353:21863` Account Picker | Life Ops money flows | Empty + populated account list, Create Account CTA | `PersonalAccountPickerSheet` | same | `GET /financial-accounts` | MATCHED |
| `417:8759` | Personal | Account chevron row | picker via `PersonalAccountSelectRow` | same | `financialAccountId` on PATCH | MATCHED |
| `453:9376` | Personal | When row card + Change | formatted label + picker menu | same | `effectiveAt` optional | PARTIAL — encoded in description until wired |
| `453:9376` | Personal | More details (emoji feelings / shared / relationship cards / reasoning) | accordion card → `description` suffix | same | `description` | MATCHED |
| `453:9376` | Personal | Notes 0/200 counter | notes field with counter | same | `description` prefix | MATCHED |
| `453:9376` | Personal | Cancel + Confirm Expense ✓ footer | solid `#7C5CFC` confirm | same | `createExpense` | MATCHED |
| `453:9376` | Personal | Privacy lock footer | lock line | same | — | MATCHED (UI-only) |
| `1006:8434` Activity Timeline | Personal Pulse → View All | Search + filters + stats + rich rows | `PersonalRecentActivityScreen` | `PersonalRecentActivityFlow` | `GET /v1/personal/activity` | MATCHED |
| `417:8759` Edit Transaction | Timeline expense row edit | Full edit form | `PersonalEditTransactionSheet` | `PersonalEditTransactionSheet` | `PATCH .../expenses/:id` | MATCHED |
| `417:8759` | Personal | Income segment | disabled + honest label | disabled | — | API_GAP (expense-only) |
| `417:8759` | Personal | Recurring toggle | UI only / Coming soon | same | — | API_GAP |
| `417:8759` | Personal | Delete | disabled / Coming soon | same | — | API_GAP (no DELETE route) |
| (inferred) Category picker | Edit Transaction chevrons | Searchable category → subcategory | `PersonalCategoryPickerSheet` | `PersonalCategoryPickerSheet` | static catalog | MATCHED |
| `417:8863` Upload Attachment | Edit Transaction attachments | Take Photo / Library / Files + drop zone | `PersonalUploadAttachmentSheet` | `PersonalUploadAttachmentSheet` | — | API_GAP — local preview only |
| `482:18697` Money Quick Add — Income | Life Ops Create hub | Tabbed sheet (green accent) | `PersonalMoneyQuickAddSheet` INCOME | same | `POST .../income` | MATCHED |
| `520:29924` Money Quick Add — Transfer | Life Ops Create hub | FROM/TO, note, transfer type (blue) | `PersonalMoneyQuickAddSheet` TRANSFER | same | `POST .../movements` TRANSFER | MATCHED |
| `520:30019` Money Quick Add — Savings | Life Ops Create hub | Goal cards, frequency (teal) | `PersonalMoneyQuickAddSheet` SAVINGS | same | `POST .../movements` SAVINGS_DEPOSIT | MATCHED |
| `453:9376` Master Expense | Pulse / Moments "+ Add Expense" + shell FAB | Full premium create form | `PersonalMasterExpenseSheet` | same | `POST .../expenses` | MATCHED |
| `1035:7757` Personal expense FAB | Personal + active Moment on Pulse/Moments/Life/Memory | Master Expense entry | `personal_expense_fab` testTag | `personal_expense_fab` accessibility id | — | MATCHED |
| Bottom Create hub | Personal Life Ops | Income tile → Money Quick Add | `AppShellScreen` `moneyQa=INCOME` | `AppShellView` `.income` | — | MATCHED |

## Test / Maestro ids

- `personal_expense_fab` — shell FAB on Personal tabs
- `master_expense_confirm` — Confirm Expense ✓ button
- `master_expense_clear_all` — Clear All pill

## API gaps (explicit — not faked)

- Attachments upload/storage
- Delete expense
- Income transaction type — **resolved** via `POST .../income` and Life Ops Income tab
- Recurring transactions
- Payment method field
- Subcategory as first-class DB column (encoded in `categoryCode` / description)
- Opening balance persistence on account create (UI shown; not sent v1)
- Default account flag in DB (toggle UI-only v1)
- `updateExpense` lacks `effectiveAt` — date read-only on edit

## Status legend

- MATCHED — implemented against contract + Figma layout
- PARTIAL — contracted field with simplified UX
- API_GAP — UI present but backend not available
- DEFERRED — out of scope this phase
