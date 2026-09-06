-- Momentra V054 - Business Deployment Closure Wave 1
-- New tables in finance/business schemas; capability inserts + moment_type_capability maps.

BEGIN;

-- ================================================================
-- finance.tax_obligation
-- ================================================================
CREATE TABLE IF NOT EXISTS finance.tax_obligation (
    tax_obligation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    title TEXT NOT NULL,
    tax_type TEXT,
    amount NUMERIC(19,4),
    currency_code CHAR(3),
    due_date DATE,
    status TEXT NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tax_obligation__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_tax_obligation__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_tax_obligation__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_tax_obligation__status
        CHECK (status IN ('OPEN','FILED','PAID','OVERDUE','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_tax_obligation__company_moment
    ON finance.tax_obligation (company_id, moment_id, created_at DESC);

-- ================================================================
-- finance.forecast_scenario
-- ================================================================
CREATE TABLE IF NOT EXISTS finance.forecast_scenario (
    forecast_scenario_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    name TEXT NOT NULL,
    horizon_months INT,
    assumptions TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_forecast_scenario__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_scenario__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_scenario__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_forecast_scenario__status
        CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS ix_forecast_scenario__company_moment
    ON finance.forecast_scenario (company_id, moment_id, created_at DESC);

-- ================================================================
-- finance.forecast_line
-- ================================================================
CREATE TABLE IF NOT EXISTS finance.forecast_line (
    forecast_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forecast_scenario_id UUID NOT NULL,
    company_id UUID NOT NULL,
    moment_id UUID,
    line_label TEXT NOT NULL,
    amount NUMERIC(19,4),
    currency_code CHAR(3),
    period_label TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_forecast_line__scenario
        FOREIGN KEY (forecast_scenario_id)
        REFERENCES finance.forecast_scenario(forecast_scenario_id) ON DELETE CASCADE,
    CONSTRAINT fk_forecast_line__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_line__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_forecast_line__scenario
    ON finance.forecast_line (forecast_scenario_id, sort_order);

-- ================================================================
-- business.investor_update
-- ================================================================
CREATE TABLE IF NOT EXISTS business.investor_update (
    investor_update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    update_type TEXT,
    subject TEXT NOT NULL,
    key_metrics TEXT,
    runway_status TEXT,
    highlights TEXT,
    next_steps TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    author_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_investor_update__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_investor_update__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_investor_update__author
        FOREIGN KEY (author_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_investor_update__status
        CHECK (status IN ('DRAFT','SENT','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS ix_investor_update__company_moment
    ON business.investor_update (company_id, moment_id, created_at DESC);

-- ================================================================
-- business.budget_alert
-- ================================================================
CREATE TABLE IF NOT EXISTS business.budget_alert (
    budget_alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    title TEXT NOT NULL,
    metric_label TEXT,
    threshold_value NUMERIC(19,4),
    currency_code CHAR(3),
    severity TEXT NOT NULL DEFAULT 'MEDIUM',
    note TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget_alert__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_alert__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_alert__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_budget_alert__severity
        CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_budget_alert__status
        CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS ix_budget_alert__company_moment
    ON business.budget_alert (company_id, moment_id, created_at DESC);

-- ================================================================
-- business.recognition
-- ================================================================
CREATE TABLE IF NOT EXISTS business.recognition (
    recognition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    recipient_name TEXT NOT NULL,
    recognition_type TEXT,
    why_text TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PUBLISHED',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_recognition__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recognition__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recognition__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_recognition__company_moment
    ON business.recognition (company_id, moment_id, created_at DESC);

-- ================================================================
-- business.meeting_record
-- ================================================================
CREATE TABLE IF NOT EXISTS business.meeting_record (
    meeting_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    title TEXT NOT NULL,
    meeting_at TIMESTAMPTZ,
    attendees_text TEXT,
    notes TEXT,
    decisions_text TEXT,
    status TEXT NOT NULL DEFAULT 'LOGGED',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_meeting_record__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_meeting_record__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_meeting_record__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_meeting_record__company_moment
    ON business.meeting_record (company_id, moment_id, created_at DESC);

-- ================================================================
-- business.retrospective
-- ================================================================
CREATE TABLE IF NOT EXISTS business.retrospective (
    retrospective_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    went_well TEXT,
    improve_next TEXT,
    status TEXT NOT NULL DEFAULT 'LOGGED',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_retrospective__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_retrospective__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_retrospective__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_retrospective__company_moment
    ON business.retrospective (company_id, moment_id, created_at DESC);

-- ================================================================
-- business.activity_log_entry
-- ================================================================
CREATE TABLE IF NOT EXISTS business.activity_log_entry (
    activity_log_entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    title TEXT NOT NULL,
    owner_label TEXT,
    category_code TEXT,
    status TEXT NOT NULL DEFAULT 'LOGGED',
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_activity_log_entry__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_log_entry__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_log_entry__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_activity_log_entry__company_moment
    ON business.activity_log_entry (company_id, moment_id, created_at DESC);


-- ================================================================
-- NEW CAPABILITIES
-- ================================================================
INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:TAX_OBLIGATION_CREATE')::uuid, 'TAX_OBLIGATION_CREATE', 'Create Tax Obligation', 'Create Tax Obligation capability.', 'FINANCE', 'TAX_OBLIGATION', 'CREATE', 'SENSITIVE', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'TAX_OBLIGATION_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:FORECAST_CREATE')::uuid, 'FORECAST_CREATE', 'Create Forecast', 'Create Forecast Scenario capability.', 'FINANCE', 'FORECAST', 'CREATE', 'SENSITIVE', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'FORECAST_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:INVESTOR_UPDATE_CREATE')::uuid, 'INVESTOR_UPDATE_CREATE', 'Create Investor Update', 'Create Investor Update capability.', 'BUSINESS', 'INVESTOR_UPDATE', 'CREATE', 'SENSITIVE', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'INVESTOR_UPDATE_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:BUDGET_ALERT_CREATE')::uuid, 'BUDGET_ALERT_CREATE', 'Create Budget Alert', 'Create Budget Alert capability.', 'FINANCE', 'BUDGET_ALERT', 'CREATE', 'SENSITIVE', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'BUDGET_ALERT_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:RECOGNITION_CREATE')::uuid, 'RECOGNITION_CREATE', 'Create Recognition', 'Create Recognition capability.', 'BUSINESS', 'RECOGNITION', 'CREATE', 'STANDARD', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'RECOGNITION_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:MEETING_CREATE')::uuid, 'MEETING_CREATE', 'Create Meeting Record', 'Create Meeting Record capability.', 'BUSINESS', 'MEETING', 'CREATE', 'STANDARD', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'MEETING_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:RETRO_CREATE')::uuid, 'RETRO_CREATE', 'Create Retrospective', 'Create Retrospective capability.', 'BUSINESS', 'RETROSPECTIVE', 'CREATE', 'STANDARD', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'RETRO_CREATE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
SELECT md5('momentra:capability:ACTIVITY_LOG_CREATE')::uuid, 'ACTIVITY_LOG_CREATE', 'Create Activity Log', 'Create Activity Log Entry capability.', 'BUSINESS', 'ACTIVITY_LOG', 'CREATE', 'STANDARD', false, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'ACTIVITY_LOG_CREATE');

-- ================================================================
-- NEW GOVERNANCE PERMISSIONS (matching capability codes)
-- ================================================================
INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:TAX_OBLIGATION_CREATE')::uuid, 'TAX_OBLIGATION_CREATE', 'Create Tax Obligation', 'Create Tax Obligation permission.', 'TAX_OBLIGATION', 'CREATE', 'SENSITIVE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'TAX_OBLIGATION_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:FORECAST_CREATE')::uuid, 'FORECAST_CREATE', 'Create Forecast', 'Create Forecast permission.', 'FORECAST', 'CREATE', 'SENSITIVE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'FORECAST_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:INVESTOR_UPDATE_CREATE')::uuid, 'INVESTOR_UPDATE_CREATE', 'Create Investor Update', 'Create Investor Update permission.', 'INVESTOR_UPDATE', 'CREATE', 'SENSITIVE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'INVESTOR_UPDATE_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:BUDGET_ALERT_CREATE')::uuid, 'BUDGET_ALERT_CREATE', 'Create Budget Alert', 'Create Budget Alert permission.', 'BUDGET_ALERT', 'CREATE', 'SENSITIVE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'BUDGET_ALERT_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:RECOGNITION_CREATE')::uuid, 'RECOGNITION_CREATE', 'Create Recognition', 'Create Recognition permission.', 'RECOGNITION', 'CREATE', 'STANDARD', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'RECOGNITION_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:MEETING_CREATE')::uuid, 'MEETING_CREATE', 'Create Meeting Record', 'Create Meeting Record permission.', 'MEETING', 'CREATE', 'STANDARD', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'MEETING_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:RETRO_CREATE')::uuid, 'RETRO_CREATE', 'Create Retrospective', 'Create Retrospective permission.', 'RETROSPECTIVE', 'CREATE', 'STANDARD', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'RETRO_CREATE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
SELECT md5('momentra:permission:ACTIVITY_LOG_CREATE')::uuid, 'ACTIVITY_LOG_CREATE', 'Create Activity Log', 'Create Activity Log Entry permission.', 'ACTIVITY_LOG', 'CREATE', 'STANDARD', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM governance.permission WHERE code = 'ACTIVITY_LOG_CREATE');

-- ================================================================
-- MAP capabilities onto ALL ACTIVE BUSINESS moment types
-- Covers both new + existing codes that may be missing on some types.
-- ================================================================
INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mt.moment_type_id::text || ':' || c.code)::uuid,
  mt.moment_type_id,
  c.capability_id,
  true,
  30,
  'ACTIVE'
FROM core.moment_type mt
CROSS JOIN core.capability c
WHERE mt.domain_code = 'BUSINESS'
  AND mt.status = 'ACTIVE'
  AND c.code IN (
    'GOAL_CREATE', 'MILESTONE_CREATE', 'RISK_CREATE', 'DECISION_RECORD', 'REVIEW_CREATE', 'POLL_CREATE',
    'TAX_OBLIGATION_CREATE', 'FORECAST_CREATE', 'INVESTOR_UPDATE_CREATE', 'BUDGET_ALERT_CREATE',
    'RECOGNITION_CREATE', 'MEETING_CREATE', 'RETRO_CREATE', 'ACTIVITY_LOG_CREATE'
  )
  AND c.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM core.moment_type_capability x
    WHERE x.moment_type_id = mt.moment_type_id AND x.capability_id = c.capability_id
  );

-- ================================================================
-- MAP ISSUE_CREATE, RISK_CREATE onto TEAM_OPERATIONS types if missing
-- ================================================================
INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mt.moment_type_id::text || ':' || c.code)::uuid,
  mt.moment_type_id,
  c.capability_id,
  true,
  30,
  'ACTIVE'
FROM core.moment_type mt
CROSS JOIN core.capability c
WHERE mt.domain_code = 'BUSINESS'
  AND mt.status = 'ACTIVE'
  AND mt.code ILIKE '%TEAM_OPERATIONS%'
  AND c.code IN ('ISSUE_CREATE', 'RISK_CREATE')
  AND c.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM core.moment_type_capability x
    WHERE x.moment_type_id = mt.moment_type_id AND x.capability_id = c.capability_id
  );

COMMENT ON TABLE finance.tax_obligation IS 'Business tax obligation tracking (V054 Deployment Closure).';
COMMENT ON TABLE finance.forecast_scenario IS 'Business forecast scenario header (V054 Deployment Closure).';
COMMENT ON TABLE finance.forecast_line IS 'Line items within a forecast scenario (V054 Deployment Closure).';
COMMENT ON TABLE business.investor_update IS 'Investor update records (V054 Deployment Closure).';
COMMENT ON TABLE business.budget_alert IS 'Budget threshold alert tracking (V054 Deployment Closure).';
COMMENT ON TABLE business.recognition IS 'Team recognition / kudos records (V054 Deployment Closure).';
COMMENT ON TABLE business.meeting_record IS 'Meeting notes and decisions log (V054 Deployment Closure).';
COMMENT ON TABLE business.retrospective IS 'Sprint/cycle retrospective records (V054 Deployment Closure).';
COMMENT ON TABLE business.activity_log_entry IS 'General activity log entries (V054 Deployment Closure).';

COMMIT;
