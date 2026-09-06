BEGIN;

-- Guided Business Create family setups (Team Ops / Runway / Ops).
CREATE TABLE business.business_system_setup (
    business_system_setup_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    family_code TEXT NOT NULL,
    title TEXT NOT NULL,
    preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_system_setup__company
        FOREIGN KEY (company_id) REFERENCES business.company(company_id),
    CONSTRAINT fk_business_system_setup__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id),
    CONSTRAINT fk_business_system_setup__moment
        FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id),
    CONSTRAINT fk_business_system_setup__moment_context
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id),
    CONSTRAINT ck_business_system_setup__family CHECK (
        family_code IN ('TEAM_OPERATIONS', 'BUSINESS_RUNWAY', 'BUSINESS_OPERATIONS')
    ),
    CONSTRAINT ck_business_system_setup__status CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'ARCHIVED')),
    CONSTRAINT ck_business_system_setup__version CHECK (version > 0)
);

CREATE INDEX ix_business_system_setup__company_family_time
    ON business.business_system_setup (company_id, family_code, created_at DESC);
CREATE INDEX ix_business_system_setup__user_time
    ON business.business_system_setup (user_id, created_at DESC);
CREATE INDEX ix_business_system_setup__moment
    ON business.business_system_setup (moment_id);

ALTER TABLE business.business_system_setup ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_business_system_setup__select_member ON business.business_system_setup
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_business_system_setup__backend_write ON business.business_system_setup
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT, INSERT, UPDATE, DELETE ON business.business_system_setup TO momentra_app;
GRANT SELECT ON business.business_system_setup TO momentra_analytics_worker, momentra_projection_worker;

COMMENT ON TABLE business.business_system_setup IS
  'Activated Business Create family setups (Team Operations / Business Runway / Business Operations).';

-- Optional company profile extras from Company Setup wizard.
ALTER TABLE business.company
    ADD COLUMN IF NOT EXISTS profile_json JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMIT;
