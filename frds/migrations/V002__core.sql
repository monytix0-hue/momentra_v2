BEGIN;

CREATE SCHEMA core;
COMMENT ON SCHEMA core IS 'Momentra shared identity, Moment Kernel, taxonomy and capability registry.';

CREATE TABLE core.user_profile (
    user_id UUID PRIMARY KEY,
    display_name TEXT,
    email TEXT,
    timezone TEXT NOT NULL DEFAULT 'UTC',
    locale TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_profile__status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','DELETED')),
    CONSTRAINT ck_user_profile__version CHECK (version > 0)
);
COMMENT ON COLUMN core.user_profile.user_id IS 'Must equal the authenticated Supabase auth.uid() for the corresponding user; no hard FK to auth.users is used in the portable baseline.';

CREATE UNIQUE INDEX uq_user_profile__email_ci
    ON core.user_profile (lower(email))
    WHERE email IS NOT NULL;

CREATE TABLE core.moment_category (
    moment_category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_code TEXT NOT NULL,
    code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_moment_category__domain_code UNIQUE (domain_code, code),
    CONSTRAINT ck_moment_category__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_moment_category__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_moment_category__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED')),
    CONSTRAINT ck_moment_category__sort_order CHECK (sort_order >= 0)
);

CREATE TABLE core.moment_type (
    moment_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_category_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment_type__category
        FOREIGN KEY (moment_category_id)
        REFERENCES core.moment_category(moment_category_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_moment_type__domain_code UNIQUE (domain_code, code),
    CONSTRAINT uq_moment_type__id_domain UNIQUE (moment_type_id, domain_code),
    CONSTRAINT ck_moment_type__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_moment_type__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_moment_type__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED')),
    CONSTRAINT ck_moment_type__sort_order CHECK (sort_order >= 0)
);

CREATE TABLE core.capability (
    capability_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    owning_service TEXT NOT NULL,
    resource_type TEXT,
    action_type TEXT NOT NULL,
    sensitivity_level TEXT NOT NULL DEFAULT 'STANDARD',
    approval_eligible BOOLEAN NOT NULL DEFAULT false,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_capability__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_capability__service CHECK (owning_service IN ('CORE','PERSONAL','COLLABORATION','BUSINESS','WORK','FINANCE','GOVERNANCE','ANALYTICS','MEMORY','AI')),
    CONSTRAINT ck_capability__action_type CHECK (action_type IN ('CREATE','READ','UPDATE','DELETE','MANAGE','EXECUTE','APPROVE','RECORD','VIEW','OTHER')),
    CONSTRAINT ck_capability__sensitivity CHECK (sensitivity_level IN ('STANDARD','SENSITIVE','HIGHLY_SENSITIVE')),
    CONSTRAINT ck_capability__status CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED'))
);

CREATE TABLE core.moment_type_capability (
    moment_type_capability_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_type_id UUID NOT NULL,
    capability_id UUID NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment_type_capability__moment_type
        FOREIGN KEY (moment_type_id)
        REFERENCES core.moment_type(moment_type_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment_type_capability__capability
        FOREIGN KEY (capability_id)
        REFERENCES core.capability(capability_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_moment_type_capability__pair UNIQUE (moment_type_id, capability_id),
    CONSTRAINT ck_moment_type_capability__sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_moment_type_capability__status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE core.external_party (
    external_party_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_type TEXT NOT NULL,
    display_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_external_party__party_type CHECK (party_type IN ('PERSON','ORGANIZATION','OTHER')),
    CONSTRAINT ck_external_party__status CHECK (status IN ('ACTIVE','INACTIVE','MERGED'))
);

CREATE TABLE core.moment (
    moment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_code TEXT NOT NULL,
    moment_type_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    visibility TEXT NOT NULL DEFAULT 'PRIVATE',
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    timezone TEXT NOT NULL DEFAULT 'UTC',
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment__moment_type_domain
        FOREIGN KEY (moment_type_id, domain_code)
        REFERENCES core.moment_type(moment_type_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment__created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_moment__id_domain UNIQUE (moment_id, domain_code),
    CONSTRAINT ck_moment__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_moment__status CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_moment__visibility CHECK (visibility IN ('PRIVATE','PARTICIPANTS','COMPANY')),
    CONSTRAINT ck_moment__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_moment__version CHECK (version > 0),
    CONSTRAINT ck_moment__completed_at CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT ck_moment__cancelled_at CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
    CONSTRAINT ck_moment__archived_at CHECK (status <> 'ARCHIVED' OR archived_at IS NOT NULL)
);

CREATE INDEX ix_moment__domain_status_start
    ON core.moment (domain_code, status, start_at DESC);
CREATE INDEX ix_moment__type_status
    ON core.moment (moment_type_id, status);
CREATE INDEX ix_moment__created_by_status
    ON core.moment (created_by_user_id, status, updated_at DESC);
CREATE INDEX ix_moment_type__category_status
    ON core.moment_type (moment_category_id, status, sort_order);
CREATE INDEX ix_moment_type_capability__moment_type
    ON core.moment_type_capability (moment_type_id, status, sort_order);
CREATE INDEX ix_moment_type_capability__capability
    ON core.moment_type_capability (capability_id, status);

COMMIT;
