-- Momentra V043 (renumbered from pack V032) - Analytics-owned Attention canonical input capture
-- Requires V001-V042 baseline; pack V032 renumbered to V043.
-- Canonical input lives in analytics; analytics.attention_item remains derived deterministic output.

BEGIN;

DO $$
BEGIN
  IF to_regclass('personal.personal_moment_context') IS NULL THEN
    RAISE EXCEPTION 'V043 requires personal.personal_moment_context';
  END IF;
  IF to_regclass('analytics.attention_item') IS NULL THEN
    RAISE EXCEPTION 'V043 requires analytics.attention_item baseline';
  END IF;
  IF to_regprocedure('platform.set_updated_at()') IS NULL THEN
    RAISE EXCEPTION 'V043 requires platform.set_updated_at() from V016';
  END IF;
END $$;

CREATE TABLE analytics.attention_capture (
  attention_capture_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  user_id UUID NOT NULL,
  category_code TEXT NOT NULL,
  intensity_code TEXT NOT NULL,
  time_block_code TEXT NOT NULL,
  energy_remaining SMALLINT,
  observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  note TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_attention_capture__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_attention_capture__category CHECK (category_code ~ '^[A-Z][A-Z0-9_]*$'),
  CONSTRAINT ck_attention_capture__intensity CHECK (intensity_code IN ('LIGHT','MODERATE','HEAVY')),
  CONSTRAINT ck_attention_capture__time_block CHECK (time_block_code IN ('MORNING','AFTERNOON','EVENING','NIGHT')),
  CONSTRAINT ck_attention_capture__energy CHECK (energy_remaining IS NULL OR energy_remaining BETWEEN 0 AND 5),
  CONSTRAINT ck_attention_capture__status CHECK (status IN ('ACTIVE','VOIDED'))
);

CREATE INDEX ix_attention_capture__user_time
  ON analytics.attention_capture(user_id, observed_at DESC);
CREATE INDEX ix_attention_capture__moment_time
  ON analytics.attention_capture(moment_id, observed_at DESC);
CREATE INDEX ix_attention_capture__user_category_time
  ON analytics.attention_capture(user_id, category_code, observed_at DESC)
  WHERE status = 'ACTIVE';

CREATE TRIGGER trg_attention_capture__set_updated_at
BEFORE UPDATE ON analytics.attention_capture
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

ALTER TABLE analytics.attention_capture ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_attention_capture__select_owner
ON analytics.attention_capture FOR SELECT
USING (
  user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);

CREATE POLICY rls_attention_capture__backend_write
ON analytics.attention_capture FOR ALL
USING (security.is_backend_app())
WITH CHECK (security.is_backend_app());

REVOKE ALL ON analytics.attention_capture FROM PUBLIC;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN EXECUTE 'REVOKE ALL ON analytics.attention_capture FROM anon'; END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN EXECUTE 'REVOKE ALL ON analytics.attention_capture FROM authenticated'; END IF;
END $$;
GRANT SELECT, INSERT, UPDATE, DELETE ON analytics.attention_capture TO momentra_app;
GRANT SELECT ON analytics.attention_capture TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

COMMENT ON TABLE analytics.attention_capture IS 'Canonical user-entered Attention capture. Analytics derives attention_item and metrics asynchronously; no business trigger is attached.';

COMMIT;
