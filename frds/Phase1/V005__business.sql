BEGIN;

CREATE SCHEMA business;
COMMENT ON SCHEMA business IS 'Momentra Business domain: Company tenancy, memberships, teams, Business Moment contexts, vendors, SLA, issues, risks, decisions and reviews.';

CREATE TABLE business.company (
    company_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name TEXT NOT NULL,
    display_name TEXT NOT NULL,
    company_type TEXT,
    registration_number TEXT,
    tax_identifier TEXT,
    timezone TEXT NOT NULL DEFAULT 'UTC',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_company__created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_company__status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','ARCHIVED')),
    CONSTRAINT ck_company__version CHECK (version > 0)
);

CREATE TABLE business.company_membership (
    company_membership_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    membership_type TEXT NOT NULL DEFAULT 'MEMBER',
    status TEXT NOT NULL DEFAULT 'INVITED',
    joined_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_company_membership__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_company_membership__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_company_membership__id_company UNIQUE (company_membership_id, company_id),
    CONSTRAINT ck_company_membership__type CHECK (membership_type IN ('OWNER','ADMIN','MEMBER','CONTRACTOR','OBSERVER')),
    CONSTRAINT ck_company_membership__status CHECK (status IN ('INVITED','ACTIVE','INACTIVE','REVOKED','LEFT','DECLINED')),
    CONSTRAINT ck_company_membership__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_company_membership__company_user_open
    ON business.company_membership (company_id, user_id)
    WHERE status IN ('INVITED','ACTIVE');
CREATE INDEX ix_company_membership__user_company_status
    ON business.company_membership (user_id, company_id, status);

CREATE TABLE business.team (
    team_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_team__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_team__id_company UNIQUE (team_id, company_id),
    CONSTRAINT uq_team__company_name UNIQUE (company_id, name),
    CONSTRAINT ck_team__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT ck_team__version CHECK (version > 0)
);

CREATE TABLE business.team_membership (
    team_membership_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL,
    company_id UUID NOT NULL,
    company_membership_id UUID NOT NULL,
    team_role TEXT NOT NULL DEFAULT 'MEMBER',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_team_membership__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_team_membership__company_membership
        FOREIGN KEY (company_membership_id, company_id)
        REFERENCES business.company_membership(company_membership_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_team_membership__team_member UNIQUE (team_id, company_membership_id),
    CONSTRAINT ck_team_membership__role CHECK (team_role IN ('LEAD','MEMBER','OBSERVER')),
    CONSTRAINT ck_team_membership__status CHECK (status IN ('ACTIVE','INACTIVE','REMOVED'))
);

CREATE TABLE business.business_moment_context (
    moment_id UUID PRIMARY KEY,
    domain_code TEXT NOT NULL DEFAULT 'BUSINESS',
    company_id UUID NOT NULL,
    business_family TEXT NOT NULL,
    team_id UUID,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_moment_context__moment_domain
        FOREIGN KEY (moment_id, domain_code)
        REFERENCES core.moment(moment_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_moment_context__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_moment_context__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_business_moment_context__moment_company UNIQUE (moment_id, company_id),
    CONSTRAINT uq_business_moment_context__moment_family UNIQUE (moment_id, business_family),
    CONSTRAINT ck_business_moment_context__domain CHECK (domain_code = 'BUSINESS'),
    CONSTRAINT ck_business_moment_context__family CHECK (business_family IN ('TEAM_OPERATIONS','BUSINESS_RUNWAY','BUSINESS_OPERATIONS','EVENTS_OPERATIONS','VENDOR_OPERATIONS')),
    CONSTRAINT ck_business_moment_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_business_moment_context__version CHECK (version > 0)
);

CREATE TABLE business.team_operations_context (
    moment_id UUID PRIMARY KEY,
    business_family TEXT NOT NULL DEFAULT 'TEAM_OPERATIONS',
    operating_cycle TEXT,
    objective_text TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_team_operations_context__family
        FOREIGN KEY (moment_id, business_family)
        REFERENCES business.business_moment_context(moment_id, business_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_team_operations_context__family CHECK (business_family = 'TEAM_OPERATIONS'),
    CONSTRAINT ck_team_operations_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE business.business_runway_context (
    moment_id UUID PRIMARY KEY,
    business_family TEXT NOT NULL DEFAULT 'BUSINESS_RUNWAY',
    planning_horizon_start DATE,
    planning_horizon_end DATE,
    scenario_name TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_runway_context__family
        FOREIGN KEY (moment_id, business_family)
        REFERENCES business.business_moment_context(moment_id, business_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_business_runway_context__family CHECK (business_family = 'BUSINESS_RUNWAY'),
    CONSTRAINT ck_business_runway_context__horizon CHECK (planning_horizon_end IS NULL OR planning_horizon_start IS NULL OR planning_horizon_end >= planning_horizon_start),
    CONSTRAINT ck_business_runway_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE business.business_operations_context (
    moment_id UUID PRIMARY KEY,
    business_family TEXT NOT NULL DEFAULT 'BUSINESS_OPERATIONS',
    operation_area TEXT,
    objective_text TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_operations_context__family
        FOREIGN KEY (moment_id, business_family)
        REFERENCES business.business_moment_context(moment_id, business_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_business_operations_context__family CHECK (business_family = 'BUSINESS_OPERATIONS'),
    CONSTRAINT ck_business_operations_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE business.events_operations_context (
    moment_id UUID PRIMARY KEY,
    business_family TEXT NOT NULL DEFAULT 'EVENTS_OPERATIONS',
    event_name TEXT,
    venue_text TEXT,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_events_operations_context__family
        FOREIGN KEY (moment_id, business_family)
        REFERENCES business.business_moment_context(moment_id, business_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_events_operations_context__family CHECK (business_family = 'EVENTS_OPERATIONS'),
    CONSTRAINT ck_events_operations_context__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_events_operations_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE business.vendor_operations_context (
    moment_id UUID PRIMARY KEY,
    business_family TEXT NOT NULL DEFAULT 'VENDOR_OPERATIONS',
    objective_text TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_vendor_operations_context__family
        FOREIGN KEY (moment_id, business_family)
        REFERENCES business.business_moment_context(moment_id, business_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_vendor_operations_context__family CHECK (business_family = 'VENDOR_OPERATIONS'),
    CONSTRAINT ck_vendor_operations_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE business.vendor (
    vendor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    external_party_id UUID,
    name TEXT NOT NULL,
    vendor_type TEXT,
    contact_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_vendor__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_vendor__external_party
        FOREIGN KEY (external_party_id)
        REFERENCES core.external_party(external_party_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_vendor__id_company UNIQUE (vendor_id, company_id),
    CONSTRAINT ck_vendor__status CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED','ARCHIVED')),
    CONSTRAINT ck_vendor__version CHECK (version > 0)
);

CREATE TABLE business.vendor_contract (
    vendor_contract_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    contract_name TEXT NOT NULL,
    contract_reference TEXT,
    start_date DATE,
    end_date DATE,
    contract_value NUMERIC(19,4),
    currency_code CHAR(3),
    status TEXT NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_vendor_contract__vendor_company
        FOREIGN KEY (vendor_id, company_id)
        REFERENCES business.vendor(vendor_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_vendor_contract__id_company_vendor UNIQUE (vendor_contract_id, company_id, vendor_id),
    CONSTRAINT ck_vendor_contract__date CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_vendor_contract__value CHECK (contract_value IS NULL OR contract_value >= 0),
    CONSTRAINT ck_vendor_contract__currency CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_vendor_contract__status CHECK (status IN ('DRAFT','ACTIVE','EXPIRED','TERMINATED','ARCHIVED')),
    CONSTRAINT ck_vendor_contract__version CHECK (version > 0)
);

CREATE TABLE business.sla_definition (
    sla_definition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    vendor_contract_id UUID,
    name TEXT NOT NULL,
    metric_code TEXT NOT NULL,
    target_value NUMERIC(24,8),
    comparator TEXT NOT NULL,
    unit_code TEXT,
    measurement_period TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_sla_definition__vendor_company
        FOREIGN KEY (vendor_id, company_id)
        REFERENCES business.vendor(vendor_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_sla_definition__contract_vendor_company
        FOREIGN KEY (vendor_contract_id, company_id, vendor_id)
        REFERENCES business.vendor_contract(vendor_contract_id, company_id, vendor_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_sla_definition__id_company_vendor UNIQUE (sla_definition_id, company_id, vendor_id),
    CONSTRAINT ck_sla_definition__contract_requires_vendor CHECK (vendor_contract_id IS NULL OR vendor_id IS NOT NULL),
    CONSTRAINT ck_sla_definition__comparator CHECK (comparator IN ('LT','LTE','EQ','GTE','GT')),
    CONSTRAINT ck_sla_definition__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED')),
    CONSTRAINT ck_sla_definition__version CHECK (version > 0)
);

CREATE TABLE business.sla_check (
    sla_check_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sla_definition_id UUID NOT NULL,
    company_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    observed_value NUMERIC(24,8),
    result TEXT NOT NULL,
    evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_sla_check__definition_company_vendor
        FOREIGN KEY (sla_definition_id, company_id, vendor_id)
        REFERENCES business.sla_definition(sla_definition_id, company_id, vendor_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_sla_check__result CHECK (result IN ('PASS','FAIL','UNKNOWN'))
);

CREATE TABLE business.issue (
    issue_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    team_id UUID,
    vendor_id UUID,
    title TEXT NOT NULL,
    description TEXT,
    severity TEXT NOT NULL DEFAULT 'MEDIUM',
    status TEXT NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_issue__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_issue__business_moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_issue__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_issue__vendor_company
        FOREIGN KEY (vendor_id, company_id)
        REFERENCES business.vendor(vendor_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_issue__severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_issue__status CHECK (status IN ('OPEN','IN_PROGRESS','BLOCKED','RESOLVED','CLOSED','CANCELLED')),
    CONSTRAINT ck_issue__resolved CHECK (status NOT IN ('RESOLVED','CLOSED') OR resolved_at IS NOT NULL),
    CONSTRAINT ck_issue__version CHECK (version > 0)
);

CREATE TABLE business.risk (
    risk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    team_id UUID,
    vendor_id UUID,
    title TEXT NOT NULL,
    description TEXT,
    likelihood TEXT NOT NULL DEFAULT 'MEDIUM',
    impact TEXT NOT NULL DEFAULT 'MEDIUM',
    mitigation_text TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN',
    identified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_risk__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_risk__business_moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_risk__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_risk__vendor_company
        FOREIGN KEY (vendor_id, company_id)
        REFERENCES business.vendor(vendor_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_risk__likelihood CHECK (likelihood IN ('LOW','MEDIUM','HIGH','VERY_HIGH')),
    CONSTRAINT ck_risk__impact CHECK (impact IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_risk__status CHECK (status IN ('OPEN','MITIGATING','ACCEPTED','CLOSED','ARCHIVED')),
    CONSTRAINT ck_risk__closed CHECK (status <> 'CLOSED' OR closed_at IS NOT NULL),
    CONSTRAINT ck_risk__version CHECK (version > 0)
);

CREATE TABLE business.decision (
    decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    team_id UUID,
    title TEXT NOT NULL,
    decision_text TEXT NOT NULL,
    rationale TEXT,
    decided_by_user_id UUID NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_decision__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_decision__business_moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_decision__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_decision__decided_by
        FOREIGN KEY (decided_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_decision__status CHECK (status IN ('ACTIVE','SUPERSEDED','REVERSED','ARCHIVED')),
    CONSTRAINT ck_decision__version CHECK (version > 0)
);

CREATE TABLE business.business_update (
    business_update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    team_id UUID,
    author_user_id UUID NOT NULL,
    title TEXT,
    body TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_update__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_update__business_moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_update__team_company
        FOREIGN KEY (team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_update__author
        FOREIGN KEY (author_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_business_update__status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE TABLE business.business_review (
    business_review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    review_type TEXT NOT NULL,
    review_date DATE NOT NULL,
    summary TEXT,
    outcome TEXT,
    status TEXT NOT NULL DEFAULT 'COMPLETED',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_review__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_review__business_moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_business_review__created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_business_review__type CHECK (review_type IN ('WEEKLY','MONTHLY','QUARTERLY','MILESTONE','INCIDENT','OTHER')),
    CONSTRAINT ck_business_review__status CHECK (status IN ('DRAFT','COMPLETED','ARCHIVED'))
);

CREATE INDEX ix_company__status
    ON business.company (status, updated_at DESC);
CREATE INDEX ix_team__company_status
    ON business.team (company_id, status, updated_at DESC);
CREATE INDEX ix_team_membership__company_member_status
    ON business.team_membership (company_id, company_membership_id, status);
CREATE INDEX ix_business_moment_context__company_status
    ON business.business_moment_context (company_id, status, updated_at DESC);
CREATE INDEX ix_business_moment_context__company_family_status
    ON business.business_moment_context (company_id, business_family, status);
CREATE INDEX ix_vendor__company_status
    ON business.vendor (company_id, status, updated_at DESC);
CREATE INDEX ix_vendor_contract__company_vendor_status
    ON business.vendor_contract (company_id, vendor_id, status, end_date);
CREATE INDEX ix_sla_definition__company_vendor_status
    ON business.sla_definition (company_id, vendor_id, status);
CREATE INDEX ix_sla_check__company_vendor_time
    ON business.sla_check (company_id, vendor_id, observed_at DESC);
CREATE INDEX ix_issue__company_status_severity
    ON business.issue (company_id, status, severity, updated_at DESC);
CREATE INDEX ix_issue__moment_status
    ON business.issue (moment_id, status, updated_at DESC)
    WHERE moment_id IS NOT NULL;
CREATE INDEX ix_risk__company_status_impact
    ON business.risk (company_id, status, impact, updated_at DESC);
CREATE INDEX ix_decision__company_time
    ON business.decision (company_id, decided_at DESC);
CREATE INDEX ix_business_update__company_time
    ON business.business_update (company_id, created_at DESC);
CREATE INDEX ix_business_review__company_date
    ON business.business_review (company_id, review_date DESC);

COMMIT;
