BEGIN;

-- Expense extended fields for transaction CRUD (S9-QH)
ALTER TABLE finance.expense
    ADD COLUMN IF NOT EXISTS subcategory_code TEXT,
    ADD COLUMN IF NOT EXISTS payment_method_code TEXT,
    ADD COLUMN IF NOT EXISTS recurring_schedule_id UUID;

ALTER TABLE finance.expense
    DROP CONSTRAINT IF EXISTS ck_expense__payment_method;

ALTER TABLE finance.expense
    ADD CONSTRAINT ck_expense__payment_method CHECK (
        payment_method_code IS NULL
        OR payment_method_code IN ('CASH','CARD','UPI','BANK_TRANSFER','WALLET','OTHER')
    );

-- Allow MEDIA attachments on expenses
ALTER TABLE finance.expense_resource_link
    DROP CONSTRAINT IF EXISTS ck_expense_resource_link__resource_type;

ALTER TABLE finance.expense_resource_link
    ADD CONSTRAINT ck_expense_resource_link__resource_type CHECK (
        resource_type IN ('BOOKING','PURCHASE_ITEM','TASK','VENDOR','INVOICE','ASSET','MEDIA','OTHER')
    );

-- Recurring schedule templates
CREATE TABLE IF NOT EXISTS finance.recurring_schedule (
    recurring_schedule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    resource_kind TEXT NOT NULL DEFAULT 'EXPENSE',
    template_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    frequency TEXT NOT NULL,
    interval_count INTEGER NOT NULL DEFAULT 1,
    start_date DATE NOT NULL,
    end_date DATE,
    next_run_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_recurring_schedule__user FOREIGN KEY (owner_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_schedule__moment FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
    CONSTRAINT ck_recurring_schedule__kind CHECK (resource_kind IN ('EXPENSE','INCOME')),
    CONSTRAINT ck_recurring_schedule__frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
    CONSTRAINT ck_recurring_schedule__interval CHECK (interval_count > 0),
    CONSTRAINT ck_recurring_schedule__status CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','VOIDED')),
    CONSTRAINT ck_recurring_schedule__version CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS finance.recurring_schedule_link (
    recurring_schedule_link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recurring_schedule_id UUID NOT NULL,
    expense_id UUID,
    financial_movement_id UUID,
    occurrence_date DATE NOT NULL,
    idempotency_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_recurring_schedule_link__schedule FOREIGN KEY (recurring_schedule_id) REFERENCES finance.recurring_schedule(recurring_schedule_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_schedule_link__expense FOREIGN KEY (expense_id) REFERENCES finance.expense(expense_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_schedule_link__movement FOREIGN KEY (financial_movement_id) REFERENCES finance.financial_movement(financial_movement_id) ON DELETE RESTRICT,
    CONSTRAINT uq_recurring_schedule_link__idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_recurring_schedule_link__target CHECK (
        (expense_id IS NOT NULL AND financial_movement_id IS NULL)
        OR (expense_id IS NULL AND financial_movement_id IS NOT NULL)
    )
);

ALTER TABLE finance.expense
    DROP CONSTRAINT IF EXISTS fk_expense__recurring_schedule;

ALTER TABLE finance.expense
    ADD CONSTRAINT fk_expense__recurring_schedule FOREIGN KEY (recurring_schedule_id)
        REFERENCES finance.recurring_schedule(recurring_schedule_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_recurring_schedule__owner_moment ON finance.recurring_schedule (owner_user_id, moment_id, status);
CREATE INDEX IF NOT EXISTS ix_recurring_schedule__next_run ON finance.recurring_schedule (next_run_at) WHERE status = 'ACTIVE';

COMMIT;
