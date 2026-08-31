# Capability → API → Handler matrix

Action Center chips are executable only when mapped through the full chain.

| Capability | Figma chip | Route | Handler | Canonical | Event |
|---|---|---|---|---|---|
| MOMENT_CREATE | Create Moment | `POST /v1/moments` | MomentCreateHandler | `core.moment` | MomentCreated |
| MOMENT_UPDATE | Rename | `PATCH /v1/moments/:id` | MomentUpdateHandler | `core.moment` | MomentUpdated |
| EXPENSE_CREATE | Expense / Money | `POST /v1/moments/:id/expenses` | ExpenseCreateHandler | `finance.expense` | ExpenseRecorded |
| TASK_CREATE | Task | `POST /v1/moments/:id/tasks` | TaskCreateHandler | `work.task` | TaskCreated |
| GOAL_CREATE | Goal / Investment | `POST /v1/moments/:id/goals` | GoalCreateHandler | `work.goal` | GoalCreated |
| POLL_CREATE | Poll | `POST /v1/moments/:id/polls` | PollCreateHandler | `shared.poll` | PollCreated |
| PLANNING_ITEM_CREATE | Planning item | `POST /v1/moments/:id/planning-items` | PlanningItemCreateHandler | `collaboration.planning_item` | PlanningItemCreated |
| BOOKING_CREATE | Booking | `POST /v1/moments/:id/bookings` | BookingCreateHandler | `collaboration.booking` | BookingCreated |
| CONTRIBUTION_RECORD | Contribution | `POST /v1/moments/:id/contributions` | ContributionRecordHandler | `finance.contribution` | ContributionRecorded |
| SETTLEMENT_RECORD | Settlement | `POST /v1/moments/:id/settlements` | SettlementRecordHandler | `finance.settlement` | SettlementRecorded |
| MEMORY_CREATE | Memory | `POST /v1/moments/:id/memories` | MemoryCreateHandler | `memory.memory` | MemoryCreated |
| UPDATE_CREATE | Update | `POST /v1/moments/:id/updates` | UpdateCreateHandler | `collaboration.group_update` | UpdateCreated |
| PARTICIPANT_MANAGE | Add people | `POST /v1/moments/:id/participants` | ParticipantManageHandler | `collaboration.moment_participant` | ParticipantAdded |
| RESIDENT_MANAGE | Resident | `POST /v1/moments/:id/residents` | ResidentManageHandler | `collaboration.resident` | ResidentAdded |
| PURCHASE_ITEM_CREATE | Purchase item | `POST /v1/moments/:id/purchase-items` | PurchaseItemCreateHandler | `collaboration.purchase_item` | PurchaseItemCreated |
| BUDGET_MANAGE | Budget | `POST /v1/moments/:id/budgets` | BudgetManageHandler | `finance.budget` | BudgetCreated |
| VENDOR_MANAGE | Vendor | `POST /v1/companies/:id/vendors` | VendorManageHandler | `business.vendor` | VendorCreated |

Server re-authorizes every command even when `available_action` showed the chip.
