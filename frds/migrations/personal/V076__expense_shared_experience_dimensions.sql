-- V076: Master Expense structured Shared Experience + analytical dimension contributions.
-- One canonical finance.expense; multi-dimension associations without duplicating spend.

BEGIN;

DO $$
BEGIN
  IF to_regclass('finance.expense') IS NULL THEN
    RAISE EXCEPTION 'V076 requires finance.expense';
  END IF;
END $$;

ALTER TABLE finance.expense
  ADD COLUMN IF NOT EXISTS shared_experience_code TEXT NOT NULL DEFAULT 'SELF',
  ADD COLUMN IF NOT EXISTS shared_experience_label TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_expense__shared_experience_code'
  ) THEN
    ALTER TABLE finance.expense
      ADD CONSTRAINT ck_expense__shared_experience_code
      CHECK (shared_experience_code IN ('SELF','SPOUSE','FAMILY','FRIEND','COLLEAGUE','OTHER'));
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS finance.expense_dimension_contribution (
  contribution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  expense_id UUID NOT NULL,
  dimension_code TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  target_moment_id UUID,
  linked_resource_type TEXT NOT NULL DEFAULT 'NONE',
  linked_resource_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_expense_dimension_contribution__expense
    FOREIGN KEY (expense_id) REFERENCES finance.expense(expense_id) ON DELETE RESTRICT,
  CONSTRAINT fk_expense_dimension_contribution__moment
    FOREIGN KEY (target_moment_id) REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
  CONSTRAINT uq_expense_dimension_contribution__expense_dim
    UNIQUE (expense_id, dimension_code),
  CONSTRAINT ck_expense_dimension_contribution__dimension
    CHECK (dimension_code IN ('LIFE_OPERATIONS','RELATIONSHIPS','LIFESTYLE','FUTURE_BUILDING')),
  CONSTRAINT ck_expense_dimension_contribution__status
    CHECK (status IN ('ACTIVE','INACTIVE')),
  CONSTRAINT ck_expense_dimension_contribution__linked_type
    CHECK (linked_resource_type IN ('NONE','RELATIONSHIP_ACTIVITY','LIFESTYLE_ACTIVITY'))
);

CREATE INDEX IF NOT EXISTS ix_expense_dimension_contribution__expense
  ON finance.expense_dimension_contribution (expense_id);

CREATE INDEX IF NOT EXISTS ix_expense_dimension_contribution__status
  ON finance.expense_dimension_contribution (status)
  WHERE status = 'ACTIVE';

-- Extend expense_resource_link resource types for activity mirrors.
ALTER TABLE finance.expense_resource_link
  DROP CONSTRAINT IF EXISTS ck_expense_resource_link__resource_type;

ALTER TABLE finance.expense_resource_link
  ADD CONSTRAINT ck_expense_resource_link__resource_type CHECK (
    resource_type IN (
      'BOOKING','PURCHASE_ITEM','TASK','VENDOR','INVOICE','ASSET','MEDIA','OTHER',
      'RELATIONSHIP_ACTIVITY','LIFESTYLE_ACTIVITY'
    )
  );

COMMIT;
