-- Business V4 Phase 1B tear-down.
-- Destructive: drops the business schema and shared FKs/tables that depend on it.
-- Apply only on confirmed V4 / dedicated Dev DBs after backup.
-- Do NOT delete prior business migration files; this is a forward migration only.

BEGIN;

-- ---------------------------------------------------------------------------
-- 1) Drop every FK from non-business schemas that references business.*
-- ---------------------------------------------------------------------------

DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    SELECT c.conname, n.nspname AS sch, t.relname AS tbl
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    JOIN pg_class ft ON ft.oid = c.confrelid
    JOIN pg_namespace fn ON fn.oid = ft.relnamespace
    WHERE c.contype = 'f'
      AND fn.nspname = 'business'
      AND n.nspname <> 'business'
  LOOP
    EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I', r.sch, r.tbl, r.conname);
  END LOOP;
END $$;

-- Explicit known names (safe if DO already removed them)
ALTER TABLE IF EXISTS finance.financial_account
  DROP CONSTRAINT IF EXISTS fk_financial_account__owner_company;
ALTER TABLE IF EXISTS finance.budget
  DROP CONSTRAINT IF EXISTS fk_budget__company;
ALTER TABLE IF EXISTS finance.revenue
  DROP CONSTRAINT IF EXISTS fk_revenue__company;
ALTER TABLE IF EXISTS finance.revenue
  DROP CONSTRAINT IF EXISTS fk_revenue__business_moment;
ALTER TABLE IF EXISTS finance.invoice
  DROP CONSTRAINT IF EXISTS fk_invoice__company;
ALTER TABLE IF EXISTS finance.invoice
  DROP CONSTRAINT IF EXISTS fk_invoice__business_moment;
ALTER TABLE IF EXISTS shared.poll
  DROP CONSTRAINT IF EXISTS fk_shared_poll__company;

-- ---------------------------------------------------------------------------
-- 2) Drop projection tables that are Business-specific
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS projection.business_finance_snapshot CASCADE;
DROP TABLE IF EXISTS projection.business_memory CASCADE;
DROP TABLE IF EXISTS projection.business_life CASCADE;
DROP TABLE IF EXISTS projection.business_moments CASCADE;
DROP TABLE IF EXISTS projection.business_pulse CASCADE;
DROP TABLE IF EXISTS projection.user_company_access CASCADE;

-- ---------------------------------------------------------------------------
-- 3) Drop entire business schema (company, operating, khata, closure, GST helpers)
-- ---------------------------------------------------------------------------

DROP SCHEMA IF EXISTS business CASCADE;

COMMIT;
