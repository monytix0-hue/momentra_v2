-- Momentra V044 (renumbered from pack V033 v2) - Life Operations typed observation details and adjustments
-- Requires V001-V043 baseline; pack V033 renumbered to V044.
-- Enforces Recovery/Mood detail type consistency relationally.

BEGIN;

DO $$
BEGIN
  IF to_regclass('personal.life_operation_observation') IS NULL THEN
    RAISE EXCEPTION 'V044 requires personal.life_operation_observation';
  END IF;
  IF to_regclass('personal.personal_moment_context') IS NULL THEN
    RAISE EXCEPTION 'V044 requires personal.personal_moment_context';
  END IF;
  IF to_regprocedure('platform.set_updated_at()') IS NULL THEN
    RAISE EXCEPTION 'V044 requires platform.set_updated_at() from V016';
  END IF;
END $$;

-- Required target key for typed composite FKs below.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_life_operation_observation__id_type'
  ) THEN
    ALTER TABLE personal.life_operation_observation
      ADD CONSTRAINT uq_life_operation_observation__id_type
      UNIQUE (life_operation_observation_id, observation_type);
  END IF;
END $$;

CREATE TABLE personal.recovery_observation_detail (
  life_operation_observation_id UUID PRIMARY KEY,
  observation_type TEXT NOT NULL DEFAULT 'RECOVERY',
  activity_type_code TEXT NOT NULL,
  duration_minutes INTEGER,
  energy_before_pct NUMERIC(5,2),
  energy_after_pct NUMERIC(5,2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_recovery_observation_detail__typed_observation
    FOREIGN KEY (life_operation_observation_id, observation_type)
    REFERENCES personal.life_operation_observation(life_operation_observation_id, observation_type)
    ON DELETE CASCADE,
  CONSTRAINT ck_recovery_observation_detail__type CHECK (observation_type = 'RECOVERY'),
  CONSTRAINT ck_recovery_observation_detail__activity_type CHECK (activity_type_code IN ('REST','SLEEP','EXERCISE','MEDITATION','SOCIAL','MUSIC','OTHER')),
  CONSTRAINT ck_recovery_observation_detail__duration CHECK (duration_minutes IS NULL OR duration_minutes >= 0),
  CONSTRAINT ck_recovery_observation_detail__energy_before CHECK (energy_before_pct IS NULL OR energy_before_pct BETWEEN 0 AND 100),
  CONSTRAINT ck_recovery_observation_detail__energy_after CHECK (energy_after_pct IS NULL OR energy_after_pct BETWEEN 0 AND 100)
);

CREATE TABLE personal.mood_observation_detail (
  life_operation_observation_id UUID PRIMARY KEY,
  observation_type TEXT NOT NULL DEFAULT 'MOOD',
  feeling_state_code TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_mood_observation_detail__typed_observation
    FOREIGN KEY (life_operation_observation_id, observation_type)
    REFERENCES personal.life_operation_observation(life_operation_observation_id, observation_type)
    ON DELETE CASCADE,
  CONSTRAINT ck_mood_observation_detail__type CHECK (observation_type = 'MOOD'),
  CONSTRAINT ck_mood_observation_detail__feeling_state CHECK (feeling_state_code IN ('GREAT','CALM','NEUTRAL','LOW','STRESSED','OTHER'))
);

CREATE TABLE personal.mood_observation_driver (
  mood_observation_driver_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  life_operation_observation_id UUID NOT NULL,
  driver_code TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_mood_observation_driver__mood_detail
    FOREIGN KEY (life_operation_observation_id)
    REFERENCES personal.mood_observation_detail(life_operation_observation_id)
    ON DELETE CASCADE,
  CONSTRAINT uq_mood_observation_driver__observation_driver UNIQUE (life_operation_observation_id, driver_code),
  CONSTRAINT ck_mood_observation_driver__code CHECK (driver_code ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE TABLE personal.life_operation_adjustment (
  life_operation_adjustment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  user_id UUID NOT NULL,
  rhythm_action_code TEXT,
  signal_direction_code TEXT,
  reason TEXT,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_life_operation_adjustment__personal_context
    FOREIGN KEY (moment_id, user_id)
    REFERENCES personal.personal_moment_context(moment_id, user_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_life_operation_adjustment__rhythm_action CHECK (rhythm_action_code IS NULL OR rhythm_action_code IN ('REDUCE_LOAD','INCREASE_INTENSITY','PAUSE','RESET')),
  CONSTRAINT ck_life_operation_adjustment__signal_direction CHECK (signal_direction_code IS NULL OR signal_direction_code IN ('DECREASE_PRESSURE','MAINTAIN','INCREASE_PRESSURE')),
  CONSTRAINT ck_life_operation_adjustment__status CHECK (status IN ('ACTIVE','VOIDED')),
  CONSTRAINT ck_life_operation_adjustment__meaningful CHECK (rhythm_action_code IS NOT NULL OR signal_direction_code IS NOT NULL OR length(trim(coalesce(reason,''))) > 0)
);

CREATE INDEX ix_recovery_observation_detail__activity
  ON personal.recovery_observation_detail(activity_type_code);
CREATE INDEX ix_mood_observation_detail__feeling
  ON personal.mood_observation_detail(feeling_state_code);
CREATE INDEX ix_mood_observation_driver__driver
  ON personal.mood_observation_driver(driver_code, life_operation_observation_id);
CREATE INDEX ix_life_operation_adjustment__user_time
  ON personal.life_operation_adjustment(user_id, occurred_at DESC)
  WHERE status = 'ACTIVE';
CREATE INDEX ix_life_operation_adjustment__moment_time
  ON personal.life_operation_adjustment(moment_id, occurred_at DESC)
  WHERE status = 'ACTIVE';

CREATE TRIGGER trg_recovery_observation_detail__set_updated_at
BEFORE UPDATE ON personal.recovery_observation_detail
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
CREATE TRIGGER trg_mood_observation_detail__set_updated_at
BEFORE UPDATE ON personal.mood_observation_detail
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();
CREATE TRIGGER trg_life_operation_adjustment__set_updated_at
BEFORE UPDATE ON personal.life_operation_adjustment
FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at();

ALTER TABLE personal.recovery_observation_detail ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.mood_observation_detail ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.mood_observation_driver ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal.life_operation_adjustment ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_recovery_observation_detail__select_owner
ON personal.recovery_observation_detail FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM personal.life_operation_observation o
    WHERE o.life_operation_observation_id = recovery_observation_detail.life_operation_observation_id
      AND o.observation_type = 'RECOVERY'
      AND (
        o.user_id = security.current_user_id()
        OR security.is_backend_app()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
  )
);
CREATE POLICY rls_recovery_observation_detail__backend_write
ON personal.recovery_observation_detail FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_mood_observation_detail__select_owner
ON personal.mood_observation_detail FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM personal.life_operation_observation o
    WHERE o.life_operation_observation_id = mood_observation_detail.life_operation_observation_id
      AND o.observation_type = 'MOOD'
      AND (
        o.user_id = security.current_user_id()
        OR security.is_backend_app()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
  )
);
CREATE POLICY rls_mood_observation_detail__backend_write
ON personal.mood_observation_detail FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_mood_observation_driver__select_owner
ON personal.mood_observation_driver FOR SELECT
USING (
  EXISTS (
    SELECT 1
    FROM personal.mood_observation_detail md
    JOIN personal.life_operation_observation o
      ON o.life_operation_observation_id = md.life_operation_observation_id
     AND o.observation_type = md.observation_type
    WHERE md.life_operation_observation_id = mood_observation_driver.life_operation_observation_id
      AND (
        o.user_id = security.current_user_id()
        OR security.is_backend_app()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
  )
);
CREATE POLICY rls_mood_observation_driver__backend_write
ON personal.mood_observation_driver FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_life_operation_adjustment__select_owner
ON personal.life_operation_adjustment FOR SELECT
USING (
  user_id = security.current_user_id()
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_life_operation_adjustment__backend_write
ON personal.life_operation_adjustment FOR ALL
USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

REVOKE ALL ON personal.recovery_observation_detail, personal.mood_observation_detail, personal.mood_observation_driver, personal.life_operation_adjustment FROM PUBLIC;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN EXECUTE 'REVOKE ALL ON personal.recovery_observation_detail, personal.mood_observation_detail, personal.mood_observation_driver, personal.life_operation_adjustment FROM anon'; END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN EXECUTE 'REVOKE ALL ON personal.recovery_observation_detail, personal.mood_observation_detail, personal.mood_observation_driver, personal.life_operation_adjustment FROM authenticated'; END IF;
END $$;
GRANT SELECT, INSERT, UPDATE, DELETE ON personal.recovery_observation_detail, personal.mood_observation_detail, personal.mood_observation_driver, personal.life_operation_adjustment TO momentra_app;
GRANT SELECT ON personal.recovery_observation_detail, personal.mood_observation_detail, personal.mood_observation_driver, personal.life_operation_adjustment TO momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

COMMENT ON TABLE personal.recovery_observation_detail IS 'Typed Recovery attributes; composite FK guarantees RECOVERY parent type.';
COMMENT ON TABLE personal.mood_observation_detail IS 'Typed Mood attributes; composite FK guarantees MOOD parent type.';
COMMENT ON TABLE personal.mood_observation_driver IS 'Multi-valued drivers; existence requires a canonical Mood detail row.';
COMMENT ON TABLE personal.life_operation_adjustment IS 'Canonical user adjustment to Life Operations rhythm/signal direction.';

COMMIT;
