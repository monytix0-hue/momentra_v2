-- Momentra V045 (renumbered from pack V034 v2) - Shared Finance closure for frozen Personal Life Ops UI
-- Requires V001-V044 baseline (repo already has V041 transaction CRUD columns).
-- Shared Finance remains the single canonical finance engine.
-- Idempotent where V041 already added subcategory_code / payment_method_code.

BEGIN;

DO $$
BEGIN
  IF to_regclass('finance.financial_account') IS NULL
     OR to_regclass('finance.expense') IS NULL
     OR to_regclass('finance.financial_movement') IS NULL THEN
    RAISE EXCEPTION 'V045 requires V007 Finance baseline';
  END IF;
  IF to_regclass('personal.personal_moment_context') IS NULL THEN
    RAISE EXCEPTION 'V045 requires personal.personal_moment_context';
  END IF;
  IF to_regclass('work.goal') IS NULL THEN
    RAISE EXCEPTION 'V045 requires work.goal';
  END IF;
  IF to_regprocedure('platform.set_updated_at()') IS NULL THEN
    RAISE EXCEPTION 'V045 requires platform.set_updated_at() from V016';
  END IF;
END $$;

-- Support relational FK proving that the configured default account belongs to the same owner scope.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_financial_account__scope_account'
  ) THEN
    ALTER TABLE finance.financial_account
      ADD CONSTRAINT uq_financial_account__scope_account
      UNIQUE (owner_scope_type, owner_scope_id, financial_account_id);
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS finance.financial_account_preference (
  owner_scope_type TEXT NOT NULL,
  owner_scope_id UUID NOT NULL,
  default_financial_account_id UUID NOT NULL,
  updated_by_user_id UUID,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT pk_financial_account_preference PRIMARY KEY (owner_scope_type, owner_scope_id),
  CONSTRAINT fk_financial_account_preference__default_account
    FOREIGN KEY (owner_scope_type, owner_scope_id, default_financial_account_id)
    REFERENCES finance.financial_account(owner_scope_type, owner_scope_id, financial_account_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_financial_account_preference__updated_by
    FOREIGN KEY (updated_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_financial_account_preference__scope CHECK (owner_scope_type IN ('USER','COMPANY'))
);

CREATE TABLE IF NOT EXISTS finance.expense_category (
  category_code TEXT PRIMARY KEY,
  label TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_expense_category__label CHECK (length(trim(label)) > 0),
  CONSTRAINT ck_expense_category__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED')),
  CONSTRAINT ck_expense_category__sort_order CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS finance.expense_subcategory (
  subcategory_code TEXT PRIMARY KEY,
  category_code TEXT NOT NULL,
  label TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_expense_subcategory__category
    FOREIGN KEY (category_code) REFERENCES finance.expense_category(category_code) ON DELETE RESTRICT,
  CONSTRAINT ck_expense_subcategory__label CHECK (length(trim(label)) > 0),
  CONSTRAINT ck_expense_subcategory__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED')),
  CONSTRAINT uq_expense_subcategory__category_code UNIQUE (category_code, subcategory_code),
  CONSTRAINT ck_expense_subcategory__sort_order CHECK (sort_order >= 0)
);

-- Preserve all existing category_code values before adding a FK.
INSERT INTO finance.expense_category(category_code, label)
SELECT DISTINCT e.category_code, e.category_code
FROM finance.expense e
WHERE e.category_code IS NOT NULL
ON CONFLICT (category_code) DO NOTHING;

-- Seed categories/subcategories visible in the frozen Personal Money flow.
INSERT INTO finance.expense_category(category_code, label, sort_order) VALUES
  ('FOOD','Food',10),
  ('TRANSPORT','Transport',20),
  ('HOUSING','Housing',30),
  ('HEALTH','Health',40),
  ('ENTERTAINMENT','Entertainment',50),
  ('SHOPPING','Shopping',60),
  ('CAFE','Cafe',70),
  ('BILLS','Bills',80),
  ('OTHER','Other',999)
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO finance.expense_subcategory(subcategory_code, category_code, label, sort_order) VALUES
  ('COFFEE','FOOD','Coffee',10),
  ('GROCERIES','FOOD','Groceries',20),
  ('DINING_OUT','FOOD','Dining Out',30),
  ('TAKEAWAY','FOOD','Takeaway',40),
  ('CELEBRATIONS','FOOD','Celebrations',50),
  ('FOOD_DINING','FOOD','Food & Dining',5),
  ('CAFE','CAFE','Cafe',10),
  ('TRANSPORT','TRANSPORT','Transport',10),
  ('SHOPPING','SHOPPING','Shopping',10),
  ('HEALTH','HEALTH','Health',10),
  ('ENTERTAINMENT','ENTERTAINMENT','Entertainment',10),
  ('BILLS','BILLS','Bills',10),
  ('OTHER','OTHER','Other',10)
ON CONFLICT (subcategory_code) DO NOTHING;

-- Repair legacy rows (e.g. V041 subcategory without category) before CHECKs/FKs.
UPDATE finance.expense e
SET category_code = s.category_code
FROM finance.expense_subcategory s
WHERE e.subcategory_code IS NOT NULL
  AND e.category_code IS NULL
  AND e.subcategory_code = s.subcategory_code;

UPDATE finance.expense e
SET category_code = s.category_code
FROM finance.expense_subcategory s
WHERE e.subcategory_code IS NOT NULL
  AND e.category_code IS NOT NULL
  AND e.subcategory_code = s.subcategory_code
  AND e.category_code IS DISTINCT FROM s.category_code;

UPDATE finance.expense
SET subcategory_code = NULL
WHERE subcategory_code IS NOT NULL
  AND category_code IS NULL;

INSERT INTO finance.expense_category(category_code, label)
SELECT DISTINCT e.category_code, e.category_code
FROM finance.expense e
WHERE e.category_code IS NOT NULL
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO finance.expense_subcategory(subcategory_code, category_code, label)
SELECT DISTINCT e.subcategory_code, e.category_code, e.subcategory_code
FROM finance.expense e
WHERE e.subcategory_code IS NOT NULL
  AND e.category_code IS NOT NULL
ON CONFLICT (subcategory_code) DO NOTHING;

-- Same-moment recurring Expense template FK target.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_expense__id_moment'
  ) THEN
    ALTER TABLE finance.expense
      ADD CONSTRAINT uq_expense__id_moment UNIQUE (expense_id, moment_id);
  END IF;
END $$;

ALTER TABLE finance.expense
  ADD COLUMN IF NOT EXISTS subcategory_code TEXT,
  ADD COLUMN IF NOT EXISTS planning_class_code TEXT,
  ADD COLUMN IF NOT EXISTS payment_method_code TEXT,
  ADD COLUMN IF NOT EXISTS note TEXT;

-- Align payment-method CHECK with pack (regex); replace V041 enum CHECK if present.
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS ck_expense__payment_method;
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS ck_expense__payment_method_code;
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS fk_expense__category;
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS fk_expense__category_subcategory;
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS ck_expense__subcategory_requires_category;
ALTER TABLE finance.expense DROP CONSTRAINT IF EXISTS ck_expense__planning_class;

ALTER TABLE finance.expense
  ADD CONSTRAINT fk_expense__category
    FOREIGN KEY (category_code) REFERENCES finance.expense_category(category_code) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_expense__category_subcategory
    FOREIGN KEY (category_code, subcategory_code) REFERENCES finance.expense_subcategory(category_code, subcategory_code) ON DELETE RESTRICT,
  ADD CONSTRAINT ck_expense__subcategory_requires_category
    CHECK (subcategory_code IS NULL OR category_code IS NOT NULL),
  ADD CONSTRAINT ck_expense__planning_class
    CHECK (planning_class_code IS NULL OR planning_class_code IN ('ESSENTIAL','PLANNED','UNPLANNED')),
  ADD CONSTRAINT ck_expense__payment_method_code
    CHECK (payment_method_code IS NULL OR payment_method_code ~ '^[A-Z][A-Z0-9_]*$');

CREATE TABLE IF NOT EXISTS finance.expense_tag (
  expense_tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  expense_id UUID NOT NULL,
  tag_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_expense_tag__expense
    FOREIGN KEY (expense_id) REFERENCES finance.expense(expense_id) ON DELETE CASCADE,
  CONSTRAINT ck_expense_tag__name CHECK (length(trim(tag_name)) BETWEEN 1 AND 80)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_expense_tag__expense_name_ci
  ON finance.expense_tag(expense_id, lower(trim(tag_name)));
CREATE INDEX IF NOT EXISTS ix_expense_tag__name_ci
  ON finance.expense_tag(lower(trim(tag_name)));

ALTER TABLE finance.financial_movement
  ADD COLUMN IF NOT EXISTS note TEXT;

CREATE TABLE IF NOT EXISTS finance.recurring_financial_instruction (
  recurring_financial_instruction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  owner_user_id UUID NOT NULL,
  instruction_type TEXT NOT NULL,
  from_account_id UUID,
  to_account_id UUID,
  source_expense_id UUID,
  savings_goal_id UUID,
  amount NUMERIC(19,4) NOT NULL,
  currency_code CHAR(3) NOT NULL,
  frequency_code TEXT NOT NULL,
  next_run_at TIMESTAMPTZ,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_recurring_financial_instruction__personal_context
    FOREIGN KEY (moment_id, owner_user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_recurring_financial_instruction__from_account_owner
    FOREIGN KEY (from_account_id, owner_user_id)
    REFERENCES finance.financial_account(financial_account_id, owner_user_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_recurring_financial_instruction__to_account_owner
    FOREIGN KEY (to_account_id, owner_user_id)
    REFERENCES finance.financial_account(financial_account_id, owner_user_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_recurring_financial_instruction__source_expense
    FOREIGN KEY (source_expense_id, moment_id)
    REFERENCES finance.expense(expense_id, moment_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_recurring_financial_instruction__savings_goal
    FOREIGN KEY (savings_goal_id, moment_id)
    REFERENCES work.goal(goal_id, moment_id)
    ON DELETE RESTRICT,
  CONSTRAINT ck_recurring_financial_instruction__type CHECK (instruction_type IN ('EXPENSE','TRANSFER','SAVINGS')),
  CONSTRAINT ck_recurring_financial_instruction__amount CHECK (amount > 0),
  CONSTRAINT ck_recurring_financial_instruction__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
  CONSTRAINT ck_recurring_financial_instruction__frequency CHECK (frequency_code ~ '^[A-Z][A-Z0-9_]*$'),
  CONSTRAINT ck_recurring_financial_instruction__status CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','CANCELLED')),
  CONSTRAINT ck_recurring_financial_instruction__version CHECK (version > 0),
  CONSTRAINT ck_recurring_financial_instruction__shape CHECK (
    (instruction_type = 'TRANSFER'
      AND from_account_id IS NOT NULL
      AND to_account_id IS NOT NULL
      AND from_account_id <> to_account_id
      AND source_expense_id IS NULL
      AND savings_goal_id IS NULL)
    OR
    (instruction_type = 'SAVINGS'
      AND from_account_id IS NOT NULL
      AND to_account_id IS NULL
      AND source_expense_id IS NULL
      AND savings_goal_id IS NOT NULL)
    OR
    (instruction_type = 'EXPENSE'
      AND from_account_id IS NOT NULL
      AND to_account_id IS NULL
      AND source_expense_id IS NOT NULL
      AND savings_goal_id IS NULL)
  )
);

CREATE INDEX IF NOT EXISTS ix_financial_account_preference__default_account
  ON finance.financial_account_preference(default_financial_account_id);
CREATE INDEX IF NOT EXISTS ix_expense_category__status_sort
  ON finance.expense_category(status, sort_order, category_code);
CREATE INDEX IF NOT EXISTS ix_expense_subcategory__category_status_sort
  ON finance.expense_subcategory(category_code, status, sort_order, subcategory_code);
CREATE INDEX IF NOT EXISTS ix_expense__category_subcategory_time
  ON finance.expense(category_code, subcategory_code, effective_at DESC)
  WHERE category_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_expense__planning_class_time
  ON finance.expense(planning_class_code, effective_at DESC)
  WHERE planning_class_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_expense__payment_method_time
  ON finance.expense(payment_method_code, effective_at DESC)
  WHERE payment_method_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_recurring_financial_instruction__owner_status_next
  ON finance.recurring_financial_instruction(owner_user_id, status, next_run_at);
CREATE INDEX IF NOT EXISTS ix_recurring_financial_instruction__moment_status
  ON finance.recurring_financial_instruction(moment_id, status, next_run_at);
CREATE INDEX IF NOT EXISTS ix_recurring_financial_instruction__source_expense
  ON finance.recurring_financial_instruction(source_expense_id)
  WHERE source_expense_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_recurring_financial_instruction__savings_goal
  ON finance.recurring_financial_instruction(savings_goal_id)
  WHERE savings_goal_id IS NOT NULL;

DROP TRIGGER IF EXISTS trg_financial_account_preference__set_updated_at ON finance.financial_account_preference;
CREATE TRIGGER trg_financial_account_preference__set_updated_at
BEFORE UPDATE ON finance.financial_account_preference
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
DROP TRIGGER IF EXISTS trg_expense_category__set_updated_at ON finance.expense_category;
CREATE TRIGGER trg_expense_category__set_updated_at
BEFORE UPDATE ON finance.expense_category
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
DROP TRIGGER IF EXISTS trg_expense_subcategory__set_updated_at ON finance.expense_subcategory;
CREATE TRIGGER trg_expense_subcategory__set_updated_at
BEFORE UPDATE ON finance.expense_subcategory
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
DROP TRIGGER IF EXISTS trg_recurring_financial_instruction__set_updated_at ON finance.recurring_financial_instruction;
CREATE TRIGGER trg_recurring_financial_instruction__set_updated_at
BEFORE UPDATE ON finance.recurring_financial_instruction
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

ALTER TABLE finance.financial_account_preference ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance.expense_category ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance.expense_subcategory ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance.expense_tag ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance.recurring_financial_instruction ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_financial_account_preference__select_scope ON finance.financial_account_preference;
CREATE POLICY rls_financial_account_preference__select_scope
ON finance.financial_account_preference FOR SELECT
USING (
  (owner_scope_type = 'USER' AND owner_scope_id = security.current_user_id())
  OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_scope_id))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_financial_account_preference__backend_write ON finance.financial_account_preference;
CREATE POLICY rls_financial_account_preference__backend_write
ON finance.financial_account_preference FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

DROP POLICY IF EXISTS rls_expense_category__select_catalogue ON finance.expense_category;
CREATE POLICY rls_expense_category__select_catalogue
ON finance.expense_category FOR SELECT
USING (status = 'ACTIVE' OR security.is_backend_app() OR security.is_projection_worker());
DROP POLICY IF EXISTS rls_expense_category__backend_write ON finance.expense_category;
CREATE POLICY rls_expense_category__backend_write
ON finance.expense_category FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());
DROP POLICY IF EXISTS rls_expense_subcategory__select_catalogue ON finance.expense_subcategory;
CREATE POLICY rls_expense_subcategory__select_catalogue
ON finance.expense_subcategory FOR SELECT
USING (status = 'ACTIVE' OR security.is_backend_app() OR security.is_projection_worker());
DROP POLICY IF EXISTS rls_expense_subcategory__backend_write ON finance.expense_subcategory;
CREATE POLICY rls_expense_subcategory__backend_write
ON finance.expense_subcategory FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

DROP POLICY IF EXISTS rls_expense_tag__select_scope ON finance.expense_tag;
CREATE POLICY rls_expense_tag__select_scope
ON finance.expense_tag FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM finance.expense e
    WHERE e.expense_id = expense_tag.expense_id
      AND (
        security.can_access_moment(e.moment_id)
        OR security.is_backend_app()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
  )
);
DROP POLICY IF EXISTS rls_expense_tag__backend_write ON finance.expense_tag;
CREATE POLICY rls_expense_tag__backend_write
ON finance.expense_tag FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

DROP POLICY IF EXISTS rls_recurring_financial_instruction__select_owner ON finance.recurring_financial_instruction;
CREATE POLICY rls_recurring_financial_instruction__select_owner
ON finance.recurring_financial_instruction FOR SELECT
USING (
  owner_user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_recurring_financial_instruction__backend_write ON finance.recurring_financial_instruction;
CREATE POLICY rls_recurring_financial_instruction__backend_write
ON finance.recurring_financial_instruction FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

REVOKE ALL ON finance.financial_account_preference, finance.expense_category, finance.expense_subcategory, finance.expense_tag, finance.recurring_financial_instruction FROM PUBLIC;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN EXECUTE 'REVOKE ALL ON finance.financial_account_preference, finance.expense_tag, finance.recurring_financial_instruction FROM anon'; END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN EXECUTE 'REVOKE ALL ON finance.financial_account_preference, finance.expense_tag, finance.recurring_financial_instruction FROM authenticated'; END IF;
END $$;
GRANT SELECT, INSERT, UPDATE, DELETE ON finance.financial_account_preference, finance.expense_category, finance.expense_subcategory, finance.expense_tag, finance.recurring_financial_instruction TO momentra_app;
GRANT SELECT ON finance.financial_account_preference, finance.expense_tag, finance.recurring_financial_instruction TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT SELECT ON finance.expense_category, finance.expense_subcategory TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN EXECUTE 'GRANT SELECT ON finance.expense_category, finance.expense_subcategory TO authenticated'; END IF;
END $$;

COMMENT ON TABLE finance.financial_account_preference IS 'Default financial account by canonical owner scope; composite FK guarantees selected account belongs to that scope.';
COMMENT ON TABLE finance.expense_category IS 'Shared Finance canonical expense category catalogue.';
COMMENT ON TABLE finance.expense_subcategory IS 'Shared Finance canonical expense subcategory catalogue.';
COMMENT ON TABLE finance.expense_tag IS 'Normalized user-entered expense tags used by Personal transaction edit flows.';
COMMENT ON TABLE finance.recurring_financial_instruction IS 'Canonical recurrence instruction for frozen Personal expense, transfer and savings flows; execution is application/worker-owned.';

COMMIT;
