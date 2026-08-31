BEGIN;

-- Monthly health snapshots for Company Life trends (695:9782)
CREATE TABLE IF NOT EXISTS projection.business_pulse_history (
    company_id UUID NOT NULL,
    period_month DATE NOT NULL,
    financial_health_score NUMERIC(24,8),
    team_score NUMERIC(24,8),
    runway_score NUMERIC(24,8),
    ops_score NUMERIC(24,8),
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_business_pulse_history PRIMARY KEY (company_id, period_month),
    CONSTRAINT fk_business_pulse_history__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_business_pulse_history__version CHECK (projection_version > 0)
);

CREATE INDEX IF NOT EXISTS ix_business_pulse_history__company_month
    ON projection.business_pulse_history (company_id, period_month DESC);

-- Share links for Business Life / Pulse sharing
CREATE TABLE IF NOT EXISTS business.share_link (
    share_link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    share_token TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_share_link__token UNIQUE (share_token),
    CONSTRAINT fk_share_link__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_share_link__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_share_link__creator
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_share_link__company_moment
    ON business.share_link (company_id, moment_id, created_at DESC)
    WHERE revoked_at IS NULL;

ALTER TABLE projection.business_pulse_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.share_link ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_business_pulse_history__select_scope ON projection.business_pulse_history
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_projection_worker()
);

CREATE POLICY rls_business_pulse_history__worker_write ON projection.business_pulse_history
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker())
WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_share_link__select_member ON business.share_link
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
);

CREATE POLICY rls_share_link__insert_member ON business.share_link
FOR INSERT WITH CHECK (
    security.is_active_company_member(company_id)
    AND created_by_user_id = security.current_user_id()
);

CREATE POLICY rls_share_link__backend_write ON business.share_link
FOR ALL USING (security.is_backend_app())
WITH CHECK (security.is_backend_app());

GRANT SELECT ON projection.business_pulse_history TO momentra_app, momentra_projection_worker;
GRANT INSERT, UPDATE, DELETE ON projection.business_pulse_history TO momentra_app, momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON business.share_link TO momentra_app;

COMMENT ON TABLE projection.business_pulse_history IS 'Monthly company health snapshots for Business Life trends (V055).';
COMMENT ON TABLE business.share_link IS 'Signed share links for business moment dashboards (V055).';

COMMIT;
