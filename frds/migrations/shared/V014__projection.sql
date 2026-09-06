BEGIN;

CREATE SCHEMA projection;
COMMENT ON SCHEMA projection IS 'Momentra disposable, rebuildable read models for Pulse, Moments, Life, Memory, Finance, actions and Life360.';

CREATE TABLE projection.moment_summary (
    moment_id UUID PRIMARY KEY,
    domain_code TEXT NOT NULL,
    moment_type_id UUID NOT NULL,
    moment_type_code TEXT NOT NULL,
    title TEXT NOT NULL,
    status TEXT NOT NULL,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    primary_scope_type TEXT NOT NULL,
    primary_scope_id UUID NOT NULL,
    summary_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment_summary__moment FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_summary__moment_type FOREIGN KEY (moment_type_id) REFERENCES core.moment_type(moment_type_id) ON DELETE RESTRICT,
    CONSTRAINT ck_moment_summary__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_moment_summary__scope CHECK (primary_scope_type IN ('USER','MOMENT','COMPANY')),
    CONSTRAINT ck_moment_summary__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.moment_summary.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.personal_pulse (
    user_id UUID PRIMARY KEY,
    attention_count INTEGER NOT NULL DEFAULT 0,
    recovery_score NUMERIC(24,8),
    mood_state TEXT,
    rhythm_score NUMERIC(24,8),
    wellbeing_score NUMERIC(24,8),
    active_moment_count INTEGER NOT NULL DEFAULT 0,
    latest_ai_insight_id UUID,
    widget_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_pulse__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_pulse__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_personal_pulse__counts CHECK (attention_count >= 0 AND active_moment_count >= 0),
    CONSTRAINT ck_personal_pulse__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.personal_pulse.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.personal_moments (
    personal_moments_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    temporal_bucket TEXT NOT NULL,
    display_rank INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,
    title TEXT NOT NULL,
    moment_type_code TEXT NOT NULL,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    card_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_moments__context FOREIGN KEY (moment_id, user_id) REFERENCES personal.personal_moment_context(moment_id, user_id) ON DELETE CASCADE,
    CONSTRAINT uq_personal_moments__user_moment UNIQUE (user_id, moment_id),
    CONSTRAINT ck_personal_moments__bucket CHECK (temporal_bucket IN ('ACTIVE','UPCOMING','COMPLETED','ARCHIVED')),
    CONSTRAINT ck_personal_moments__rank CHECK (display_rank >= 0),
    CONSTRAINT ck_personal_moments__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.personal_moments.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.personal_life (
    user_id UUID PRIMARY KEY,
    life_operations_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    future_building_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    lifestyle_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    relationships_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_life__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_life__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_personal_life__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.personal_life.lifestyle_payload IS 'Projection contract must preserve EXPERIENCE, WELLBEING, DISCOVERY, CREATION and LIFESTYLE contexts.';
COMMENT ON COLUMN projection.personal_life.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.personal_memory (
    user_id UUID PRIMARY KEY,
    memory_strength_score NUMERIC(24,8),
    memory_count INTEGER NOT NULL DEFAULT 0,
    pattern_count INTEGER NOT NULL DEFAULT 0,
    learning_count INTEGER NOT NULL DEFAULT 0,
    recent_memory_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_memory__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_memory__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_personal_memory__counts CHECK (memory_count >= 0 AND pattern_count >= 0 AND learning_count >= 0),
    CONSTRAINT ck_personal_memory__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.personal_memory.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.personal_finance_snapshot (
    user_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    expense_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    contribution_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    payable_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    receivable_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    snapshot_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, currency_code),
    CONSTRAINT fk_personal_finance_snapshot__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT ck_personal_finance_snapshot__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_personal_finance_snapshot__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.personal_finance_snapshot.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_pulse (
    moment_id UUID PRIMARY KEY,
    participant_count INTEGER NOT NULL DEFAULT 0,
    attention_count INTEGER NOT NULL DEFAULT 0,
    task_open_count INTEGER NOT NULL DEFAULT 0,
    budget_utilization NUMERIC(24,8),
    contribution_completion NUMERIC(24,8),
    latest_ai_insight_id UUID,
    widget_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_pulse__context FOREIGN KEY (moment_id) REFERENCES collaboration.group_moment_context(moment_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_pulse__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_group_pulse__counts CHECK (participant_count >= 0 AND attention_count >= 0 AND task_open_count >= 0),
    CONSTRAINT ck_group_pulse__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_pulse.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_moments (
    group_moments_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    temporal_bucket TEXT NOT NULL,
    display_rank INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,
    title TEXT NOT NULL,
    moment_type_code TEXT NOT NULL,
    card_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_moments__participant FOREIGN KEY (participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_moments__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_group_moments__user_moment UNIQUE (user_id, moment_id),
    CONSTRAINT ck_group_moments__bucket CHECK (temporal_bucket IN ('ACTIVE','UPCOMING','COMPLETED','ARCHIVED')),
    CONSTRAINT ck_group_moments__rank CHECK (display_rank >= 0),
    CONSTRAINT ck_group_moments__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_moments.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_life (
    moment_id UUID PRIMARY KEY,
    planning_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    participation_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    operations_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    finance_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_life__context FOREIGN KEY (moment_id) REFERENCES collaboration.group_moment_context(moment_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_life__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_group_life__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_life.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_memory (
    moment_id UUID PRIMARY KEY,
    memory_count INTEGER NOT NULL DEFAULT 0,
    pattern_count INTEGER NOT NULL DEFAULT 0,
    learning_count INTEGER NOT NULL DEFAULT 0,
    memory_strength_score NUMERIC(24,8),
    recent_memory_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_memory__context FOREIGN KEY (moment_id) REFERENCES collaboration.group_moment_context(moment_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_memory__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_group_memory__counts CHECK (memory_count >= 0 AND pattern_count >= 0 AND learning_count >= 0),
    CONSTRAINT ck_group_memory__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_memory.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_finance_snapshot (
    moment_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    expense_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    contribution_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    settled_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    outstanding_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    snapshot_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (moment_id, currency_code),
    CONSTRAINT fk_group_finance_snapshot__context FOREIGN KEY (moment_id) REFERENCES collaboration.group_moment_context(moment_id) ON DELETE CASCADE,
    CONSTRAINT ck_group_finance_snapshot__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_group_finance_snapshot__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_finance_snapshot.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.group_finance_position (
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    paid_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    allocated_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    contribution_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    payable_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    receivable_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    settled_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    net_position NUMERIC(19,4) NOT NULL DEFAULT 0,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (moment_id, participant_id, currency_code),
    CONSTRAINT fk_group_finance_position__participant FOREIGN KEY (participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE CASCADE,
    CONSTRAINT ck_group_finance_position__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_group_finance_position__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.group_finance_position.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.business_pulse (
    company_id UUID PRIMARY KEY,
    active_moment_count INTEGER NOT NULL DEFAULT 0,
    attention_count INTEGER NOT NULL DEFAULT 0,
    open_issue_count INTEGER NOT NULL DEFAULT 0,
    open_risk_count INTEGER NOT NULL DEFAULT 0,
    runway_months NUMERIC(24,8),
    financial_health_score NUMERIC(24,8),
    operations_health_score NUMERIC(24,8),
    latest_ai_insight_id UUID,
    widget_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_pulse__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE CASCADE,
    CONSTRAINT fk_business_pulse__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_business_pulse__counts CHECK (active_moment_count >= 0 AND attention_count >= 0 AND open_issue_count >= 0 AND open_risk_count >= 0),
    CONSTRAINT ck_business_pulse__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.business_pulse.runway_months IS 'Must be sourced from analytics.metric_current; Projection does not recalculate runway.';
COMMENT ON COLUMN projection.business_pulse.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.business_moments (
    business_moments_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    team_id UUID,
    temporal_bucket TEXT NOT NULL,
    display_rank INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,
    title TEXT NOT NULL,
    moment_type_code TEXT NOT NULL,
    card_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_moments__context FOREIGN KEY (moment_id, company_id) REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE CASCADE,
    CONSTRAINT fk_business_moments__team_company FOREIGN KEY (team_id, company_id) REFERENCES business.team(team_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT uq_business_moments__company_moment UNIQUE (company_id, moment_id),
    CONSTRAINT ck_business_moments__bucket CHECK (temporal_bucket IN ('ACTIVE','UPCOMING','COMPLETED','ARCHIVED')),
    CONSTRAINT ck_business_moments__rank CHECK (display_rank >= 0),
    CONSTRAINT ck_business_moments__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.business_moments.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.business_life (
    company_id UUID PRIMARY KEY,
    team_operations_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    runway_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    business_operations_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    vendor_operations_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_life__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE CASCADE,
    CONSTRAINT fk_business_life__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_business_life__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.business_life.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.business_memory (
    company_id UUID PRIMARY KEY,
    memory_count INTEGER NOT NULL DEFAULT 0,
    pattern_count INTEGER NOT NULL DEFAULT 0,
    learning_count INTEGER NOT NULL DEFAULT 0,
    playbook_count INTEGER NOT NULL DEFAULT 0,
    memory_strength_score NUMERIC(24,8),
    recent_memory_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    latest_ai_insight_id UUID,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_memory__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE CASCADE,
    CONSTRAINT fk_business_memory__ai_insight FOREIGN KEY (latest_ai_insight_id) REFERENCES ai.ai_insight(ai_insight_id) ON DELETE SET NULL,
    CONSTRAINT ck_business_memory__counts CHECK (memory_count >= 0 AND pattern_count >= 0 AND learning_count >= 0 AND playbook_count >= 0),
    CONSTRAINT ck_business_memory__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.business_memory.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.business_finance_snapshot (
    company_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    expense_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    revenue_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    invoice_outstanding_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    cash_balance_total NUMERIC(19,4) NOT NULL DEFAULT 0,
    snapshot_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, currency_code),
    CONSTRAINT fk_business_finance_snapshot__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE CASCADE,
    CONSTRAINT ck_business_finance_snapshot__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_business_finance_snapshot__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.business_finance_snapshot.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.available_action (
    available_action_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id UUID NOT NULL,
    capability_id UUID NOT NULL,
    capability_code TEXT NOT NULL,
    action_code TEXT NOT NULL,
    availability_status TEXT NOT NULL DEFAULT 'AVAILABLE',
    reason_code TEXT,
    requires_confirmation BOOLEAN NOT NULL DEFAULT false,
    requires_approval BOOLEAN NOT NULL DEFAULT false,
    source_version BIGINT,
    source_event_id UUID,
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_available_action__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_available_action__capability FOREIGN KEY (capability_id) REFERENCES core.capability(capability_id) ON DELETE RESTRICT,
    CONSTRAINT uq_available_action__user_resource_action UNIQUE (user_id, resource_type, resource_id, action_code),
    CONSTRAINT ck_available_action__status CHECK (availability_status IN ('AVAILABLE','DISABLED','HIDDEN')),
    CONSTRAINT ck_available_action__source_version CHECK (source_version IS NULL OR source_version > 0),
    CONSTRAINT ck_available_action__projection_version CHECK (projection_version > 0),
    CONSTRAINT ck_available_action__expiry CHECK (expires_at IS NULL OR expires_at >= evaluated_at)
);
COMMENT ON COLUMN projection.available_action.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.pending_approval_summary (
    pending_approval_summary_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    approval_request_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID,
    resource_type TEXT NOT NULL,
    resource_id UUID NOT NULL,
    action_code TEXT NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    summary_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pending_approval_summary__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pending_approval_summary__request FOREIGN KEY (approval_request_id) REFERENCES governance.approval_request(approval_request_id) ON DELETE CASCADE,
    CONSTRAINT uq_pending_approval_summary__user_request UNIQUE (user_id, approval_request_id),
    CONSTRAINT ck_pending_approval_summary__projection_version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.pending_approval_summary.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.attention_summary (
    attention_summary_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    attention_item_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    severity TEXT NOT NULL,
    title TEXT NOT NULL,
    status TEXT NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    summary_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_attention_summary__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_attention_summary__attention FOREIGN KEY (attention_item_id) REFERENCES analytics.attention_item(attention_item_id) ON DELETE CASCADE,
    CONSTRAINT uq_attention_summary__user_attention UNIQUE (user_id, attention_item_id),
    CONSTRAINT ck_attention_summary__severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_attention_summary__projection_version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.attention_summary.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.life360 (
    user_id UUID PRIMARY KEY,
    personal_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    group_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    business_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    attention_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    recent_activity_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_life360__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT ck_life360__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.life360.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.user_company_access (
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    membership_id UUID NOT NULL,
    membership_status TEXT NOT NULL,
    role_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID,
    projection_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, company_id),
    CONSTRAINT fk_user_company_access__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_company_access__membership FOREIGN KEY (membership_id, company_id) REFERENCES business.company_membership(company_membership_id, company_id) ON DELETE CASCADE,
    CONSTRAINT ck_user_company_access__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.user_company_access.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.recent_activity (
    recent_activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    activity_code TEXT NOT NULL,
    title TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    activity_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    projection_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_recent_activity__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_recent_activity__user_event UNIQUE (user_id, source_event_id),
    CONSTRAINT ck_recent_activity__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS','SHARED','PLATFORM')),
    CONSTRAINT ck_recent_activity__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_recent_activity__version CHECK (projection_version > 0)
);
COMMENT ON COLUMN projection.recent_activity.source_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE projection.projection_state (
    projection_code TEXT NOT NULL,
    partition_key TEXT NOT NULL DEFAULT 'GLOBAL',
    status TEXT NOT NULL DEFAULT 'READY',
    last_event_id UUID,
    last_event_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    lag_seconds INTEGER NOT NULL DEFAULT 0,
    error_code TEXT,
    error_message TEXT,
    rebuild_started_at TIMESTAMPTZ,
    rebuild_completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (projection_code, partition_key),
    CONSTRAINT ck_projection_state__code CHECK (projection_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_projection_state__status CHECK (status IN ('READY','STALE','FAILED','REBUILDING','DISABLED')),
    CONSTRAINT ck_projection_state__lag CHECK (lag_seconds >= 0),
    CONSTRAINT ck_projection_state__rebuild CHECK (rebuild_completed_at IS NULL OR rebuild_started_at IS NOT NULL)
);
COMMENT ON COLUMN projection.projection_state.last_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE INDEX ix_personal_moments__user_bucket_rank ON projection.personal_moments (user_id, temporal_bucket, display_rank, updated_at DESC);
CREATE INDEX ix_group_moments__user_bucket_rank ON projection.group_moments (user_id, temporal_bucket, display_rank, updated_at DESC);
CREATE INDEX ix_business_moments__company_bucket_rank ON projection.business_moments (company_id, temporal_bucket, display_rank, updated_at DESC);
CREATE INDEX ix_available_action__user_resource ON projection.available_action (user_id, resource_type, resource_id, availability_status, evaluated_at DESC);
CREATE INDEX ix_pending_approval_summary__user_time ON projection.pending_approval_summary (user_id, requested_at DESC);
CREATE INDEX ix_attention_summary__user_status_severity ON projection.attention_summary (user_id, status, severity, opened_at DESC);
CREATE INDEX ix_recent_activity__user_time ON projection.recent_activity (user_id, occurred_at DESC);
CREATE INDEX ix_projection_state__status_lag ON projection.projection_state (status, lag_seconds DESC, updated_at DESC);

COMMIT;
