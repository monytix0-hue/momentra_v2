BEGIN;

-- Phase 13 Lean Analytics V1 schemas (pack V031 renumbered to V059).
CREATE SCHEMA IF NOT EXISTS analytics_raw;
CREATE SCHEMA IF NOT EXISTS analytics_core;
CREATE SCHEMA IF NOT EXISTS analytics_mart;

COMMENT ON SCHEMA analytics_raw IS 'Phase 13 Lean product analytics — immutable-ish event ingestion.';
COMMENT ON SCHEMA analytics_core IS 'Phase 13 Lean product analytics — validated facts and registries.';
COMMENT ON SCHEMA analytics_mart IS 'Phase 13 Lean product analytics — dashboard KPI aggregates.';

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_ingest') THEN CREATE ROLE analytics_ingest NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_transform') THEN CREATE ROLE analytics_transform NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_read') THEN CREATE ROLE analytics_read NOLOGIN; END IF;
EXCEPTION WHEN insufficient_privilege THEN
  RAISE NOTICE 'Role creation skipped: insufficient privilege';
END $$;

REVOKE ALL ON SCHEMA analytics_raw, analytics_core, analytics_mart FROM PUBLIC;

GRANT USAGE ON SCHEMA analytics_raw, analytics_core, analytics_mart TO momentra_app, momentra_analytics_worker;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_ingest') THEN
    EXECUTE 'GRANT USAGE ON SCHEMA analytics_raw TO analytics_ingest';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_transform') THEN
    EXECUTE 'GRANT USAGE ON SCHEMA analytics_raw, analytics_core, analytics_mart TO analytics_transform';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='analytics_read') THEN
    EXECUTE 'GRANT USAGE ON SCHEMA analytics_core, analytics_mart TO analytics_read';
  END IF;
END $$;

COMMIT;
