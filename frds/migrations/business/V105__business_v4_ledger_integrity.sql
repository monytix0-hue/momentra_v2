-- Business V4 P2 ledger integrity: backfill audit + correction linkage.
-- Additive only. Does not drop business schema (never run V103 here).

BEGIN;

CREATE TABLE IF NOT EXISTS business.ledger_backfill_run (
    backfill_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mode TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ NULL,
    actor_label TEXT NULL,
    expected_obligations INT NOT NULL DEFAULT 0,
    expected_allocations INT NOT NULL DEFAULT 0,
    expected_movements INT NOT NULL DEFAULT 0,
    created_obligations INT NOT NULL DEFAULT 0,
    created_allocations INT NOT NULL DEFAULT 0,
    created_movements INT NOT NULL DEFAULT 0,
    skipped_events INT NOT NULL DEFAULT 0,
    ambiguous_events INT NOT NULL DEFAULT 0,
    report JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_ledger_backfill_run__mode CHECK (mode IN ('DRY_RUN', 'APPLY'))
);

CREATE INDEX IF NOT EXISTS ix_ledger_backfill_run__started
    ON business.ledger_backfill_run (started_at DESC);

COMMENT ON TABLE business.ledger_backfill_run IS
  'P2.1 audit of historical V104 effect backfills (dry-run and apply).';

ALTER TABLE business.ledger_backfill_run ENABLE ROW LEVEL SECURITY;
CREATE POLICY rls_ledger_backfill_run__backend ON business.ledger_backfill_run
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());
CREATE POLICY rls_ledger_backfill_run__select ON business.ledger_backfill_run
FOR SELECT USING (security.is_backend_app() OR security.is_projection_worker());

GRANT SELECT, INSERT, UPDATE ON business.ledger_backfill_run TO momentra_app;
GRANT SELECT ON business.ledger_backfill_run TO momentra_projection_worker;

COMMIT;
