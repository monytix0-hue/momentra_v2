-- Momentra V046 - Personal Future / Lifestyle / Relationships precision profiles
-- Requires V001-V045. Canonical ownership: Personal. No V003 table rewrite.

BEGIN;

DO $$
BEGIN
  IF to_regclass('personal.personal_moment_context') IS NULL THEN
    RAISE EXCEPTION 'V046 requires personal.personal_moment_context';
  END IF;
  IF to_regprocedure('platform.set_updated_at()') IS NULL THEN
    RAISE EXCEPTION 'V046 requires platform.set_updated_at() from V016';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS personal.future_building_profile (
  moment_id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  building_focus_code TEXT,
  focus_horizon_code TEXT,
  progress_rhythm_code TEXT,
  primary_value_code TEXT,
  main_friction_code TEXT,
  momentum_driver_code TEXT,
  support_style_code TEXT,
  future_feel_code TEXT,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_future_building_profile__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_future_building_profile__version CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS personal.lifestyle_profile (
  moment_id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  lifestyle_focus_code TEXT,
  energy_style_code TEXT,
  exploration_bias_code TEXT,
  wellbeing_priority_code TEXT,
  rhythm_code TEXT,
  joy_driver_code TEXT,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_lifestyle_profile__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_lifestyle_profile__version CHECK (version > 0)
);

CREATE TABLE IF NOT EXISTS personal.relationships_profile (
  moment_id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  bond_focus_code TEXT,
  connection_style_code TEXT,
  care_priority_code TEXT,
  presence_rhythm_code TEXT,
  support_preference_code TEXT,
  investment_stance_code TEXT,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_relationships_profile__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_relationships_profile__version CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS ix_future_building_profile__user_updated
  ON personal.future_building_profile(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_lifestyle_profile__user_updated
  ON personal.lifestyle_profile(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_relationships_profile__user_updated
  ON personal.relationships_profile(user_id, updated_at DESC);

DROP TRIGGER IF EXISTS trg_future_building_profile__set_updated_at ON personal.future_building_profile;
CREATE TRIGGER trg_future_building_profile__set_updated_at
BEFORE UPDATE ON personal.future_building_profile
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

DROP TRIGGER IF EXISTS trg_lifestyle_profile__set_updated_at ON personal.lifestyle_profile;
CREATE TRIGGER trg_lifestyle_profile__set_updated_at
BEFORE UPDATE ON personal.lifestyle_profile
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

DROP TRIGGER IF EXISTS trg_relationships_profile__set_updated_at ON personal.relationships_profile;
CREATE TRIGGER trg_relationships_profile__set_updated_at
BEFORE UPDATE ON personal.relationships_profile
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

ALTER TABLE personal.future_building_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.lifestyle_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.relationships_profile ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_future_building_profile__owner ON personal.future_building_profile;
CREATE POLICY rls_future_building_profile__owner
ON personal.future_building_profile FOR SELECT
USING (user_id = security.current_user_id() OR security.is_backend_app() OR security.is_projection_worker());
DROP POLICY IF EXISTS rls_future_building_profile__backend_write ON personal.future_building_profile;
CREATE POLICY rls_future_building_profile__backend_write
ON personal.future_building_profile FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

DROP POLICY IF EXISTS rls_lifestyle_profile__owner ON personal.lifestyle_profile;
CREATE POLICY rls_lifestyle_profile__owner
ON personal.lifestyle_profile FOR SELECT
USING (user_id = security.current_user_id() OR security.is_backend_app() OR security.is_projection_worker());
DROP POLICY IF EXISTS rls_lifestyle_profile__backend_write ON personal.lifestyle_profile;
CREATE POLICY rls_lifestyle_profile__backend_write
ON personal.lifestyle_profile FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

DROP POLICY IF EXISTS rls_relationships_profile__owner ON personal.relationships_profile;
CREATE POLICY rls_relationships_profile__owner
ON personal.relationships_profile FOR SELECT
USING (user_id = security.current_user_id() OR security.is_backend_app() OR security.is_projection_worker());
DROP POLICY IF EXISTS rls_relationships_profile__backend_write ON personal.relationships_profile;
CREATE POLICY rls_relationships_profile__backend_write
ON personal.relationships_profile FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT, INSERT, UPDATE, DELETE ON personal.future_building_profile, personal.lifestyle_profile, personal.relationships_profile TO momentra_app;
GRANT SELECT ON personal.future_building_profile, personal.lifestyle_profile, personal.relationships_profile TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

COMMENT ON TABLE personal.future_building_profile IS 'PX-1 Future Building precision profile (setup codes).';
COMMENT ON TABLE personal.lifestyle_profile IS 'PX-2 Lifestyle precision profile (setup codes).';
COMMENT ON TABLE personal.relationships_profile IS 'PX-3 Relationships precision profile (setup codes).';

COMMIT;
