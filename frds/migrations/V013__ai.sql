BEGIN;

CREATE SCHEMA ai;
COMMENT ON SCHEMA ai IS 'Momentra governed non-canonical AI context, inference, insights, recommendations, action proposals and provenance.';

CREATE TABLE ai.context_session (
    context_session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_user_id UUID NOT NULL,
    requested_by_user_id UUID,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    domain_code TEXT,
    purpose_code TEXT NOT NULL,
    context_type TEXT NOT NULL,
    consent_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    governance_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'OPEN',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    CONSTRAINT fk_context_session__subject FOREIGN KEY (subject_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_context_session__requested_by FOREIGN KEY (requested_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_context_session__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_context_session__domain CHECK (domain_code IS NULL OR domain_code IN ('PERSONAL','GROUP','BUSINESS','CROSS_DOMAIN')),
    CONSTRAINT ck_context_session__purpose CHECK (purpose_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_context_session__type CHECK (context_type IN ('INSIGHT','RECOMMENDATION','ACTION_ASSISTANCE','MEMORY_INTERPRETATION','OTHER')),
    CONSTRAINT ck_context_session__status CHECK (status IN ('OPEN','COMPLETED','EXPIRED','REVOKED','FAILED')),
    CONSTRAINT ck_context_session__expiry CHECK (expires_at IS NULL OR expires_at >= created_at),
    CONSTRAINT ck_context_session__closed CHECK (status NOT IN ('COMPLETED','EXPIRED','REVOKED','FAILED') OR closed_at IS NOT NULL)
);

CREATE TABLE ai.context_item (
    context_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_session_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID,
    data_category_code TEXT NOT NULL,
    sensitivity_level TEXT NOT NULL,
    content JSONB NOT NULL,
    content_hash TEXT,
    included_reason TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_context_item__session FOREIGN KEY (context_session_id) REFERENCES ai.context_session(context_session_id) ON DELETE CASCADE,
    CONSTRAINT ck_context_item__source CHECK (source_type IN ('MOMENT','WORK','FINANCE','ANALYTICS','MEMORY','BUSINESS','COLLABORATION','PERSONAL','PROJECTION','OTHER')),
    CONSTRAINT ck_context_item__category CHECK (data_category_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_context_item__sensitivity CHECK (sensitivity_level IN ('INTERNAL','CONFIDENTIAL','HIGHLY_CONFIDENTIAL','RESTRICTED')),
    CONSTRAINT ck_context_item__expiry CHECK (expires_at IS NULL OR expires_at >= created_at)
);

CREATE TABLE ai.inference_run (
    inference_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_session_id UUID NOT NULL,
    provider_code TEXT NOT NULL,
    model_code TEXT NOT NULL,
    model_version TEXT,
    prompt_template_code TEXT,
    prompt_template_version INTEGER,
    status TEXT NOT NULL DEFAULT 'RUNNING',
    input_token_count INTEGER,
    output_token_count INTEGER,
    duration_ms INTEGER,
    estimated_cost NUMERIC(19,6),
    response_hash TEXT,
    error_code TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_inference_run__session FOREIGN KEY (context_session_id) REFERENCES ai.context_session(context_session_id) ON DELETE RESTRICT,
    CONSTRAINT ck_inference_run__status CHECK (status IN ('RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    CONSTRAINT ck_inference_run__tokens CHECK ((input_token_count IS NULL OR input_token_count >= 0) AND (output_token_count IS NULL OR output_token_count >= 0)),
    CONSTRAINT ck_inference_run__duration CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT ck_inference_run__cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    CONSTRAINT ck_inference_run__completed CHECK (status='RUNNING' OR completed_at IS NOT NULL)
);

CREATE TABLE ai.ai_insight (
    ai_insight_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inference_run_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    domain_code TEXT,
    insight_code TEXT,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    confidence_score NUMERIC(6,5),
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    dismissed_at TIMESTAMPTZ,
    CONSTRAINT fk_ai_insight__run FOREIGN KEY (inference_run_id) REFERENCES ai.inference_run(inference_run_id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_insight__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_ai_insight__domain CHECK (domain_code IS NULL OR domain_code IN ('PERSONAL','GROUP','BUSINESS','CROSS_DOMAIN')),
    CONSTRAINT ck_ai_insight__confidence CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)),
    CONSTRAINT ck_ai_insight__status CHECK (status IN ('ACTIVE','DISMISSED','EXPIRED','INVALIDATED','SUPERSEDED')),
    CONSTRAINT ck_ai_insight__dismissed CHECK (status <> 'DISMISSED' OR dismissed_at IS NOT NULL)
);

CREATE TABLE ai.recommendation (
    recommendation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inference_run_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    recommendation_code TEXT,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'MEDIUM',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    dismissed_at TIMESTAMPTZ,
    CONSTRAINT fk_recommendation__run FOREIGN KEY (inference_run_id) REFERENCES ai.inference_run(inference_run_id) ON DELETE RESTRICT,
    CONSTRAINT ck_recommendation__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_recommendation__priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_recommendation__status CHECK (status IN ('ACTIVE','ACCEPTED','DISMISSED','EXPIRED','INVALIDATED','SUPERSEDED')),
    CONSTRAINT ck_recommendation__dismissed CHECK (status <> 'DISMISSED' OR dismissed_at IS NOT NULL)
);

CREATE TABLE ai.action_proposal (
    action_proposal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inference_run_id UUID NOT NULL,
    recommendation_id UUID,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    action_code TEXT NOT NULL,
    target_resource_type TEXT,
    target_resource_id UUID,
    target_resource_version BIGINT,
    requires_user_confirmation BOOLEAN NOT NULL DEFAULT true,
    confirmed_by_user_id UUID,
    confirmed_at TIMESTAMPTZ,
    requires_governance_approval BOOLEAN NOT NULL DEFAULT false,
    governance_approval_request_id UUID,
    status TEXT NOT NULL DEFAULT 'PROPOSED',
    idempotency_key TEXT,
    executed_resource_type TEXT,
    executed_resource_id UUID,
    executed_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_action_proposal__run FOREIGN KEY (inference_run_id) REFERENCES ai.inference_run(inference_run_id) ON DELETE RESTRICT,
    CONSTRAINT fk_action_proposal__recommendation FOREIGN KEY (recommendation_id) REFERENCES ai.recommendation(recommendation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_action_proposal__confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_action_proposal__approval FOREIGN KEY (governance_approval_request_id) REFERENCES governance.approval_request(approval_request_id) ON DELETE RESTRICT,
    CONSTRAINT ck_action_proposal__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_action_proposal__action CHECK (action_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_action_proposal__target_version CHECK (target_resource_version IS NULL OR target_resource_version > 0),
    CONSTRAINT ck_action_proposal__confirmation CHECK (NOT requires_user_confirmation OR status IN ('PROPOSED','REJECTED','EXPIRED','INVALIDATED') OR (confirmed_by_user_id IS NOT NULL AND confirmed_at IS NOT NULL)),
    CONSTRAINT ck_action_proposal__approval_required CHECK (NOT requires_governance_approval OR status IN ('PROPOSED','CONFIRMED','REJECTED','EXPIRED','INVALIDATED') OR governance_approval_request_id IS NOT NULL),
    CONSTRAINT ck_action_proposal__status CHECK (status IN ('PROPOSED','CONFIRMED','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','REJECTED','EXPIRED','INVALIDATED','FAILED')),
    CONSTRAINT ck_action_proposal__executed CHECK (status <> 'EXECUTED' OR executed_at IS NOT NULL),
    CONSTRAINT ck_action_proposal__rejected CHECK (status <> 'REJECTED' OR rejected_at IS NOT NULL),
    CONSTRAINT ck_action_proposal__terminal_exclusive CHECK (NOT (executed_at IS NOT NULL AND rejected_at IS NOT NULL)),
    CONSTRAINT ck_action_proposal__version CHECK (version > 0),
    CONSTRAINT ck_action_proposal__expiry CHECK (expires_at IS NULL OR expires_at >= created_at)
);

CREATE UNIQUE INDEX uq_action_proposal__idempotency
    ON ai.action_proposal (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE ai.action_proposal_parameter (
    action_proposal_parameter_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action_proposal_id UUID NOT NULL,
    parameter_name TEXT NOT NULL,
    parameter_type TEXT NOT NULL,
    string_value TEXT,
    numeric_value NUMERIC(24,8),
    boolean_value BOOLEAN,
    uuid_value UUID,
    json_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_action_proposal_parameter__proposal FOREIGN KEY (action_proposal_id) REFERENCES ai.action_proposal(action_proposal_id) ON DELETE CASCADE,
    CONSTRAINT uq_action_proposal_parameter__name UNIQUE (action_proposal_id, parameter_name),
    CONSTRAINT ck_action_proposal_parameter__name CHECK (parameter_name ~ '^[A-Za-z][A-Za-z0-9_]*$'),
    CONSTRAINT ck_action_proposal_parameter__type CHECK (parameter_type IN ('STRING','NUMBER','BOOLEAN','UUID','JSON')),
    CONSTRAINT ck_action_proposal_parameter__one_value CHECK (((string_value IS NOT NULL)::int + (numeric_value IS NOT NULL)::int + (boolean_value IS NOT NULL)::int + (uuid_value IS NOT NULL)::int + (json_value IS NOT NULL)::int) = 1)
);

CREATE TABLE ai.provenance (
    provenance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    output_type TEXT NOT NULL,
    output_id UUID NOT NULL,
    context_item_id UUID,
    source_type TEXT NOT NULL,
    source_id UUID,
    evidence_role TEXT NOT NULL DEFAULT 'SUPPORTING_EVIDENCE',
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_provenance__context_item FOREIGN KEY (context_item_id) REFERENCES ai.context_item(context_item_id) ON DELETE RESTRICT,
    CONSTRAINT uq_provenance__source UNIQUE (output_type, output_id, source_type, source_id, evidence_role),
    CONSTRAINT ck_provenance__output CHECK (output_type IN ('INSIGHT','RECOMMENDATION','ACTION_PROPOSAL')),
    CONSTRAINT ck_provenance__source CHECK (source_type IN ('CONTEXT_ITEM','MOMENT','WORK','FINANCE','ANALYTICS','MEMORY','EVENT','POLICY','OTHER')),
    CONSTRAINT ck_provenance__role CHECK (evidence_role IN ('PRIMARY_EVIDENCE','SUPPORTING_EVIDENCE','CONTRADICTING_EVIDENCE','CONSTRAINT','CONTEXT'))
);

CREATE INDEX ix_context_session__subject_scope_time ON ai.context_session (subject_user_id, scope_type, scope_id, created_at DESC);
CREATE INDEX ix_context_item__session_category ON ai.context_item (context_session_id, data_category_code, created_at);
CREATE INDEX ix_inference_run__session_status_time ON ai.inference_run (context_session_id, status, started_at DESC);
CREATE INDEX ix_ai_insight__scope_status_time ON ai.ai_insight (scope_type, scope_id, status, created_at DESC);
CREATE INDEX ix_recommendation__scope_status_time ON ai.recommendation (scope_type, scope_id, status, created_at DESC);
CREATE INDEX ix_action_proposal__scope_status_time ON ai.action_proposal (scope_type, scope_id, status, created_at DESC);
CREATE INDEX ix_action_proposal__approval ON ai.action_proposal (governance_approval_request_id) WHERE governance_approval_request_id IS NOT NULL;
CREATE INDEX ix_provenance__output ON ai.provenance (output_type, output_id, created_at);

COMMIT;
