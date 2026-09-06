BEGIN;

CREATE SCHEMA governance;
COMMENT ON SCHEMA governance IS 'Momentra governance control plane: permissions, roles, assignments, consent, policy and approvals.';

CREATE TABLE governance.permission (
    permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    resource_type TEXT NOT NULL,
    action_type TEXT NOT NULL,
    sensitivity_level TEXT NOT NULL DEFAULT 'STANDARD',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_permission__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_permission__action CHECK (action_type IN ('CREATE','READ','UPDATE','DELETE','MANAGE','EXECUTE','APPROVE','RECORD','VIEW','OTHER')),
    CONSTRAINT ck_permission__sensitivity CHECK (sensitivity_level IN ('STANDARD','SENSITIVE','HIGHLY_SENSITIVE')),
    CONSTRAINT ck_permission__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED'))
);

CREATE TABLE governance.role (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    role_type TEXT NOT NULL DEFAULT 'SYSTEM',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_role__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_role__type CHECK (role_type IN ('SYSTEM','CUSTOM')),
    CONSTRAINT ck_role__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED'))
);

CREATE TABLE governance.role_permission (
    role_permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_role_permission__role FOREIGN KEY (role_id) REFERENCES governance.role(role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_permission__permission FOREIGN KEY (permission_id) REFERENCES governance.permission(permission_id) ON DELETE RESTRICT,
    CONSTRAINT uq_role_permission__pair UNIQUE (role_id, permission_id)
);

CREATE TABLE governance.role_assignment (
    role_assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID,
    starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    assigned_by_user_id UUID,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_role_assignment__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_assignment__role FOREIGN KEY (role_id) REFERENCES governance.role(role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_assignment__assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_role_assignment__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE','GLOBAL')),
    CONSTRAINT ck_role_assignment__scope_id CHECK ((scope_type='GLOBAL' AND scope_id IS NULL) OR (scope_type<>'GLOBAL' AND scope_id IS NOT NULL)),
    CONSTRAINT ck_role_assignment__time CHECK (ends_at IS NULL OR ends_at >= starts_at),
    CONSTRAINT ck_role_assignment__status CHECK (status IN ('ACTIVE','INACTIVE','REVOKED','EXPIRED')),
    CONSTRAINT ck_role_assignment__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_role_assignment__active_scope
    ON governance.role_assignment (user_id, role_id, scope_type, COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE status = 'ACTIVE';

CREATE TABLE governance.consent_purpose (
    consent_purpose_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_consent_purpose__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_consent_purpose__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE governance.data_category (
    data_category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    default_sensitivity_level TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_data_category__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_data_category__sensitivity CHECK (default_sensitivity_level IN ('INTERNAL','CONFIDENTIAL','HIGHLY_CONFIDENTIAL','RESTRICTED')),
    CONSTRAINT ck_data_category__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE governance.consent (
    consent_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_user_id UUID NOT NULL,
    consent_purpose_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    withdrawn_at TIMESTAMPTZ,
    source TEXT NOT NULL DEFAULT 'USER',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_consent__subject FOREIGN KEY (subject_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consent__purpose FOREIGN KEY (consent_purpose_id) REFERENCES governance.consent_purpose(consent_purpose_id) ON DELETE RESTRICT,
    CONSTRAINT ck_consent__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE','GLOBAL')),
    CONSTRAINT ck_consent__scope_id CHECK ((scope_type='GLOBAL' AND scope_id IS NULL) OR (scope_type<>'GLOBAL' AND scope_id IS NOT NULL)),
    CONSTRAINT ck_consent__status CHECK (status IN ('ACTIVE','WITHDRAWN','EXPIRED','SUPERSEDED')),
    CONSTRAINT ck_consent__source CHECK (source IN ('USER','ADMIN','MIGRATION','SYSTEM')),
    CONSTRAINT ck_consent__expiry CHECK (expires_at IS NULL OR expires_at >= granted_at),
    CONSTRAINT ck_consent__withdrawn CHECK (status <> 'WITHDRAWN' OR withdrawn_at IS NOT NULL),
    CONSTRAINT ck_consent__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_consent__active_scope
    ON governance.consent (subject_user_id, consent_purpose_id, scope_type, COALESCE(scope_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE status = 'ACTIVE';

CREATE TABLE governance.policy (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    policy_family TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_policy__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_policy__family CHECK (policy_family IN ('MOMENT','PARTICIPATION','BUSINESS','FINANCE','CONSENT','AI','MEMORY','APPROVAL','OTHER')),
    CONSTRAINT ck_policy__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE governance.policy_version (
    policy_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    definition JSONB NOT NULL,
    decision_precedence JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_policy_version__policy FOREIGN KEY (policy_id) REFERENCES governance.policy(policy_id) ON DELETE RESTRICT,
    CONSTRAINT fk_policy_version__created_by FOREIGN KEY (created_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_policy_version__number UNIQUE (policy_id, version_number),
    CONSTRAINT ck_policy_version__number CHECK (version_number > 0),
    CONSTRAINT ck_policy_version__status CHECK (status IN ('DRAFT','ACTIVE','RETIRED')),
    CONSTRAINT ck_policy_version__time CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX uq_policy_version__active
    ON governance.policy_version (policy_id)
    WHERE status = 'ACTIVE';

CREATE TABLE governance.approval_request (
    approval_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by_user_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID,
    resource_type TEXT NOT NULL,
    resource_id UUID NOT NULL,
    action_code TEXT NOT NULL,
    policy_id UUID,
    status TEXT NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    context JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_approval_request__requested_by FOREIGN KEY (requested_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_approval_request__policy FOREIGN KEY (policy_id) REFERENCES governance.policy(policy_id) ON DELETE RESTRICT,
    CONSTRAINT ck_approval_request__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE','GLOBAL')),
    CONSTRAINT ck_approval_request__scope_id CHECK ((scope_type='GLOBAL' AND scope_id IS NULL) OR (scope_type<>'GLOBAL' AND scope_id IS NOT NULL)),
    CONSTRAINT ck_approval_request__status CHECK (status IN ('PENDING','IN_REVIEW','APPROVED','REJECTED','CANCELLED','EXPIRED')),
    CONSTRAINT ck_approval_request__completed CHECK (status NOT IN ('APPROVED','REJECTED','CANCELLED','EXPIRED') OR completed_at IS NOT NULL),
    CONSTRAINT ck_approval_request__expiry CHECK (expires_at IS NULL OR expires_at >= requested_at),
    CONSTRAINT ck_approval_request__version CHECK (version > 0)
);

CREATE TABLE governance.approval_step (
    approval_step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL,
    step_number INTEGER NOT NULL,
    step_type TEXT NOT NULL,
    approver_role_id UUID,
    approver_user_id UUID,
    minimum_approvals INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_approval_step__request FOREIGN KEY (approval_request_id) REFERENCES governance.approval_request(approval_request_id) ON DELETE RESTRICT,
    CONSTRAINT fk_approval_step__role FOREIGN KEY (approver_role_id) REFERENCES governance.role(role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_approval_step__user FOREIGN KEY (approver_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_approval_step__request_number UNIQUE (approval_request_id, step_number),
    CONSTRAINT uq_approval_step__id_request UNIQUE (approval_step_id, approval_request_id),
    CONSTRAINT ck_approval_step__number CHECK (step_number > 0),
    CONSTRAINT ck_approval_step__type CHECK (step_type IN ('ROLE','USER','SYSTEM')),
    CONSTRAINT ck_approval_step__role_requirement CHECK (
        (step_type='ROLE' AND approver_role_id IS NOT NULL AND approver_user_id IS NULL)
        OR (step_type='USER' AND approver_role_id IS NULL AND approver_user_id IS NOT NULL)
        OR (step_type='SYSTEM' AND approver_role_id IS NULL AND approver_user_id IS NULL)
    ),
    CONSTRAINT ck_approval_step__minimum CHECK (minimum_approvals > 0),
    CONSTRAINT ck_approval_step__status CHECK (status IN ('PENDING','IN_PROGRESS','APPROVED','REJECTED','SKIPPED','CANCELLED')),
    CONSTRAINT ck_approval_step__completed CHECK (status NOT IN ('APPROVED','REJECTED','SKIPPED','CANCELLED') OR completed_at IS NOT NULL)
);

CREATE TABLE governance.approval_decision (
    approval_decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL,
    approval_step_id UUID NOT NULL,
    decided_by_user_id UUID,
    decision TEXT NOT NULL,
    reason TEXT,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_approval_decision__step_request FOREIGN KEY (approval_step_id, approval_request_id) REFERENCES governance.approval_step(approval_step_id, approval_request_id) ON DELETE RESTRICT,
    CONSTRAINT fk_approval_decision__decided_by FOREIGN KEY (decided_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_approval_decision__decision CHECK (decision IN ('APPROVE','REJECT','ABSTAIN','SYSTEM_APPROVE','SYSTEM_REJECT')),
    CONSTRAINT ck_approval_decision__actor CHECK (decision NOT IN ('APPROVE','REJECT','ABSTAIN') OR decided_by_user_id IS NOT NULL)
);

CREATE INDEX ix_role_permission__role ON governance.role_permission (role_id, permission_id);
CREATE INDEX ix_role_assignment__user_scope_status ON governance.role_assignment (user_id, scope_type, scope_id, status);
CREATE INDEX ix_role_assignment__scope_status ON governance.role_assignment (scope_type, scope_id, status);
CREATE INDEX ix_consent__subject_scope_status ON governance.consent (subject_user_id, scope_type, scope_id, status);
CREATE INDEX ix_policy_version__policy_status ON governance.policy_version (policy_id, status, version_number DESC);
CREATE INDEX ix_approval_request__resource_status ON governance.approval_request (resource_type, resource_id, status, requested_at DESC);
CREATE INDEX ix_approval_request__scope_status ON governance.approval_request (scope_type, scope_id, status, requested_at DESC);
CREATE INDEX ix_approval_step__request_status ON governance.approval_step (approval_request_id, status, step_number);
CREATE INDEX ix_approval_decision__request_time ON governance.approval_decision (approval_request_id, decided_at DESC);

COMMIT;
