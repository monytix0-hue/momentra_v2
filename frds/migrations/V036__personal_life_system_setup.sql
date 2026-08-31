BEGIN;

-- Guided Personal Create setups (Figma 353:6809 / 353:6905 / 353:7075 / 353:7217).
CREATE TABLE personal.life_system_setup (
    life_system_setup_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    system_code TEXT NOT NULL,
    title TEXT NOT NULL,
    preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_life_system_setup__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id),
    CONSTRAINT fk_life_system_setup__moment
        FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id),
    CONSTRAINT fk_life_system_setup__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id),
    CONSTRAINT ck_life_system_setup__system CHECK (
        system_code IN ('LIFE_OPERATIONS', 'FUTURE_BUILDING', 'LIFESTYLE', 'RELATIONSHIPS')
    ),
    CONSTRAINT ck_life_system_setup__status CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'ARCHIVED')),
    CONSTRAINT ck_life_system_setup__version CHECK (version > 0)
);

CREATE INDEX ix_life_system_setup__user_system_time
    ON personal.life_system_setup (user_id, system_code, created_at DESC);
CREATE INDEX ix_life_system_setup__moment
    ON personal.life_system_setup (moment_id);

ALTER TABLE personal.life_system_setup ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_life_system_setup__select_owner ON personal.life_system_setup
FOR SELECT USING (
    (user_id = security.current_user_id())
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_life_system_setup__backend_write ON personal.life_system_setup
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT, INSERT, UPDATE, DELETE ON personal.life_system_setup TO momentra_app;
GRANT SELECT ON personal.life_system_setup TO momentra_analytics_worker, momentra_projection_worker;

COMMENT ON TABLE personal.life_system_setup IS
  'Activated Personal Create life-system setups (Life Ops / Future / Lifestyle / Relationships) with preference snapshot.';

COMMIT;
