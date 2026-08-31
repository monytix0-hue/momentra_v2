-- Momentra V042 (renumbered from pack V031 v2) - Personal Life Operations setup/profile closure
-- Requires V001-V041 baseline; pack V031 renumbered to V042.
-- Canonical ownership: Personal.
-- Technical triggers only; business events/outbox remain application-owned.

BEGIN;

DO $$
BEGIN
  IF to_regclass('personal.personal_moment_context') IS NULL THEN
    RAISE EXCEPTION 'V042 requires personal.personal_moment_context';
  END IF;
  IF to_regprocedure('platform.set_updated_at()') IS NULL THEN
    RAISE EXCEPTION 'V042 requires platform.set_updated_at() from V016';
  END IF;
END $$;

CREATE TABLE personal.life_operation_profile (
  moment_id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  life_focus_code TEXT,
  daily_balance_code TEXT,
  current_rhythm_code TEXT,
  current_state_code TEXT,
  primary_need_code TEXT,
  main_pressure_code TEXT,
  recovery_window_code TEXT,
  checkin_cadence_code TEXT,
  helpful_support_code TEXT,
  recovery_style_code TEXT,
  current_energy_code TEXT,
  wellbeing_reminder_cadence_code TEXT,
  stress_checkin_enabled BOOLEAN,
  recovery_checkin_enabled BOOLEAN,
  review_cadence_code TEXT,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_life_operation_profile__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_life_operation_profile__version CHECK (version > 0)
);

CREATE TABLE personal.life_operation_priority (
  life_operation_priority_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  user_id UUID NOT NULL,
  priority_code TEXT NOT NULL,
  weight_pct NUMERIC(5,2),
  selected BOOLEAN NOT NULL DEFAULT TRUE,
  effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
  effective_to TIMESTAMPTZ,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_life_operation_priority__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_life_operation_priority__moment_code_from
    UNIQUE (moment_id, priority_code, effective_from),
  CONSTRAINT ck_life_operation_priority__code CHECK (priority_code ~ '^[A-Z][A-Z0-9_]*$'),
  CONSTRAINT ck_life_operation_priority__weight CHECK (weight_pct IS NULL OR weight_pct BETWEEN 0 AND 100),
  CONSTRAINT ck_life_operation_priority__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
  CONSTRAINT ck_life_operation_priority__effective_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE personal.life_operation_anchor (
  life_operation_anchor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  user_id UUID NOT NULL,
  anchor_code TEXT,
  display_name TEXT NOT NULL,
  selected BOOLEAN NOT NULL DEFAULT TRUE,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_life_operation_anchor__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_life_operation_anchor__moment_name UNIQUE (moment_id, display_name),
  CONSTRAINT ck_life_operation_anchor__code CHECK (anchor_code IS NULL OR anchor_code ~ '^[A-Z][A-Z0-9_]*$'),
  CONSTRAINT ck_life_operation_anchor__display_name CHECK (length(trim(display_name)) > 0),
  CONSTRAINT ck_life_operation_anchor__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED'))
);

CREATE INDEX ix_life_operation_profile__user_updated
  ON personal.life_operation_profile(user_id, updated_at DESC);
CREATE INDEX ix_life_operation_priority__moment_status
  ON personal.life_operation_priority(moment_id, status, selected, effective_from DESC);
CREATE INDEX ix_life_operation_priority__user_status
  ON personal.life_operation_priority(user_id, status, effective_from DESC);
-- At most one current ACTIVE row per priority within a Personal moment.
CREATE UNIQUE INDEX uq_life_operation_priority__current_active
  ON personal.life_operation_priority(moment_id, priority_code)
  WHERE status = 'ACTIVE' AND effective_to IS NULL;
CREATE INDEX ix_life_operation_anchor__moment_status
  ON personal.life_operation_anchor(moment_id, status, selected, updated_at DESC);
CREATE INDEX ix_life_operation_anchor__user_status
  ON personal.life_operation_anchor(user_id, status, updated_at DESC);

CREATE TRIGGER trg_life_operation_profile__set_updated_at
BEFORE UPDATE ON personal.life_operation_profile
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

CREATE TRIGGER trg_life_operation_priority__set_updated_at
BEFORE UPDATE ON personal.life_operation_priority
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

CREATE TRIGGER trg_life_operation_anchor__set_updated_at
BEFORE UPDATE ON personal.life_operation_anchor
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

ALTER TABLE personal.life_operation_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.life_operation_priority ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.life_operation_anchor ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_life_operation_profile__select_owner
ON personal.life_operation_profile FOR SELECT
USING (
  user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_life_operation_profile__backend_write
ON personal.life_operation_profile FOR ALL
USING (security.is_backend_app())
WITH CHECK (security.is_backend_app());

CREATE POLICY rls_life_operation_priority__select_owner
ON personal.life_operation_priority FOR SELECT
USING (
  user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_life_operation_priority__backend_write
ON personal.life_operation_priority FOR ALL
USING (security.is_backend_app())
WITH CHECK (security.is_backend_app());

CREATE POLICY rls_life_operation_anchor__select_owner
ON personal.life_operation_anchor FOR SELECT
USING (
  user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_life_operation_anchor__backend_write
ON personal.life_operation_anchor FOR ALL
USING (security.is_backend_app())
WITH CHECK (security.is_backend_app());

REVOKE ALL ON personal.life_operation_profile, personal.life_operation_priority, personal.life_operation_anchor FROM PUBLIC;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN EXECUTE 'REVOKE ALL ON personal.life_operation_profile, personal.life_operation_priority, personal.life_operation_anchor FROM anon'; END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN EXECUTE 'REVOKE ALL ON personal.life_operation_profile, personal.life_operation_priority, personal.life_operation_anchor FROM authenticated'; END IF;
END $$;
GRANT SELECT, INSERT, UPDATE, DELETE ON personal.life_operation_profile, personal.life_operation_priority, personal.life_operation_anchor TO momentra_app;
GRANT SELECT ON personal.life_operation_profile, personal.life_operation_priority, personal.life_operation_anchor TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

COMMENT ON TABLE personal.life_operation_profile IS 'Canonical Life Operations setup/profile for one Personal Moment and owner.';
COMMENT ON TABLE personal.life_operation_priority IS 'Versioned/effective Life Operations priority selections and weights.';
COMMENT ON TABLE personal.life_operation_anchor IS 'Canonical Life Operations energy/routine anchors configured by the owner.';

COMMIT;
