-- V077: Grants + RLS for finance.expense_dimension_contribution (V076 table).

BEGIN;

DO $$
BEGIN
  IF to_regclass('finance.expense_dimension_contribution') IS NULL THEN
    RAISE EXCEPTION 'V077 requires finance.expense_dimension_contribution (apply V076 first)';
  END IF;
END $$;

ALTER TABLE finance.expense_dimension_contribution ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_expense_dimension_contribution__select_scope
  ON finance.expense_dimension_contribution;
CREATE POLICY rls_expense_dimension_contribution__select_scope
ON finance.expense_dimension_contribution FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM finance.expense e
    WHERE e.expense_id = expense_dimension_contribution.expense_id
      AND (
        security.can_access_moment(e.moment_id)
        OR security.is_backend_app()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
  )
);

DROP POLICY IF EXISTS rls_expense_dimension_contribution__backend_write
  ON finance.expense_dimension_contribution;
CREATE POLICY rls_expense_dimension_contribution__backend_write
ON finance.expense_dimension_contribution FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

REVOKE ALL ON finance.expense_dimension_contribution FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    EXECUTE 'REVOKE ALL ON finance.expense_dimension_contribution FROM anon';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    EXECUTE 'REVOKE ALL ON finance.expense_dimension_contribution FROM authenticated';
  END IF;
END $$;

GRANT SELECT, INSERT, UPDATE, DELETE ON finance.expense_dimension_contribution TO momentra_app;
GRANT SELECT ON finance.expense_dimension_contribution
  TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

COMMENT ON TABLE finance.expense_dimension_contribution IS
  'Analytical dimension associations for one canonical finance.expense (LO / Rel / Lifestyle / FB). Never multiplies spend.';

COMMIT;
