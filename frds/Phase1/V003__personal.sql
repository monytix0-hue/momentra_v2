BEGIN;

CREATE SCHEMA personal;
COMMENT ON SCHEMA personal IS 'Momentra Personal domain: personal Moment context, Life Operations, Future Building extensions, Lifestyle, and Relationships.';

CREATE TABLE personal.personal_moment_context (
    moment_id UUID PRIMARY KEY,
    domain_code TEXT NOT NULL DEFAULT 'PERSONAL',
    user_id UUID NOT NULL,
    personal_context_type TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_moment_context__moment_domain
        FOREIGN KEY (moment_id, domain_code)
        REFERENCES core.moment(moment_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_personal_moment_context__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_personal_moment_context__moment_user UNIQUE (moment_id, user_id),
    CONSTRAINT ck_personal_moment_context__domain CHECK (domain_code = 'PERSONAL'),
    CONSTRAINT ck_personal_moment_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_personal_moment_context__version CHECK (version > 0)
);

CREATE TABLE personal.life_operation_observation (
    life_operation_observation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    observation_type TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    numeric_value NUMERIC(12,4),
    text_value TEXT,
    unit_code TEXT,
    note TEXT,
    source_type TEXT NOT NULL DEFAULT 'USER',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_life_operation_observation__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_life_operation_observation__type CHECK (observation_type IN ('RECOVERY','MOOD','RHYTHM','WELLBEING')),
    CONSTRAINT ck_life_operation_observation__source CHECK (source_type IN ('USER','DEVICE','IMPORT','SYSTEM')),
    CONSTRAINT ck_life_operation_observation__status CHECK (status IN ('ACTIVE','CORRECTED','VOIDED')),
    CONSTRAINT ck_life_operation_observation__value CHECK (numeric_value IS NOT NULL OR text_value IS NOT NULL OR note IS NOT NULL)
);

CREATE TABLE personal.future_opportunity (
    future_opportunity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    opportunity_type TEXT,
    target_date DATE,
    status TEXT NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_future_opportunity__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_future_opportunity__status CHECK (status IN ('OPEN','PURSUING','REALIZED','DECLINED','ARCHIVED')),
    CONSTRAINT ck_future_opportunity__version CHECK (version > 0)
);

CREATE TABLE personal.future_pivot (
    future_pivot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    pivot_reason TEXT,
    effective_date DATE,
    status TEXT NOT NULL DEFAULT 'PROPOSED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_future_pivot__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_future_pivot__status CHECK (status IN ('PROPOSED','ACCEPTED','REJECTED','COMPLETED','ARCHIVED')),
    CONSTRAINT ck_future_pivot__version CHECK (version > 0)
);

CREATE TABLE personal.future_learning_activity (
    future_learning_activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    provider_name TEXT,
    target_date DATE,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_future_learning_activity__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_future_learning_activity__status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','DROPPED','ARCHIVED')),
    CONSTRAINT ck_future_learning_activity__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT ck_future_learning_activity__version CHECK (version > 0)
);

CREATE TABLE personal.future_progress_observation (
    future_progress_observation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    progress_type TEXT NOT NULL,
    progress_value NUMERIC(12,4),
    unit_code TEXT,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_future_progress_observation__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_future_progress_observation__type CHECK (progress_type IN ('GOAL','MILESTONE','GENERAL'))
);

CREATE TABLE personal.lifestyle_activity (
    lifestyle_activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    lifestyle_context TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    occurred_at TIMESTAMPTZ,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    location_text TEXT,
    wellbeing_rating NUMERIC(5,2),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_lifestyle_activity__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_lifestyle_activity__context CHECK (lifestyle_context IN ('EXPERIENCE','WELLBEING','DISCOVERY','CREATION','LIFESTYLE')),
    CONSTRAINT ck_lifestyle_activity__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_lifestyle_activity__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_lifestyle_activity__version CHECK (version > 0)
);

CREATE TABLE personal.relationship_connection (
    relationship_connection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    related_user_id UUID,
    external_party_id UUID,
    relationship_type TEXT,
    display_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    started_on DATE,
    ended_on DATE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_relationship_connection__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_relationship_connection__related_user
        FOREIGN KEY (related_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_relationship_connection__external_party
        FOREIGN KEY (external_party_id)
        REFERENCES core.external_party(external_party_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_relationship_connection__id_user UNIQUE (relationship_connection_id, user_id),
    CONSTRAINT ck_relationship_connection__target CHECK (((related_user_id IS NOT NULL)::int + (external_party_id IS NOT NULL)::int) = 1),
    CONSTRAINT ck_relationship_connection__self CHECK (related_user_id IS NULL OR related_user_id <> user_id),
    CONSTRAINT ck_relationship_connection__status CHECK (status IN ('ACTIVE','INACTIVE','ENDED','ARCHIVED')),
    CONSTRAINT ck_relationship_connection__date CHECK (ended_on IS NULL OR started_on IS NULL OR ended_on >= started_on),
    CONSTRAINT ck_relationship_connection__version CHECK (version > 0)
);

CREATE TABLE personal.relationship_activity (
    relationship_activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    relationship_connection_id UUID NOT NULL,
    activity_type TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    title TEXT,
    note TEXT,
    investment_value NUMERIC(12,4),
    unit_code TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_relationship_activity__personal_context
        FOREIGN KEY (moment_id, user_id)
        REFERENCES personal.personal_moment_context(moment_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_relationship_activity__connection_user
        FOREIGN KEY (relationship_connection_id, user_id)
        REFERENCES personal.relationship_connection(relationship_connection_id, user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_relationship_activity__type CHECK (activity_type IN ('INTERACTION','ACTIVITY','SUPPORT','SHARED_EXPERIENCE','RELATIONSHIP_INVESTMENT')),
    CONSTRAINT ck_relationship_activity__status CHECK (status IN ('ACTIVE','CORRECTED','VOIDED'))
);

CREATE INDEX ix_personal_moment_context__user_status
    ON personal.personal_moment_context (user_id, status, updated_at DESC);
CREATE INDEX ix_life_operation_observation__user_type_time
    ON personal.life_operation_observation (user_id, observation_type, observed_at DESC);
CREATE INDEX ix_life_operation_observation__moment_time
    ON personal.life_operation_observation (moment_id, observed_at DESC);
CREATE INDEX ix_future_opportunity__user_status
    ON personal.future_opportunity (user_id, status, target_date);
CREATE INDEX ix_future_pivot__user_status
    ON personal.future_pivot (user_id, status, effective_date);
CREATE INDEX ix_future_learning_activity__user_status
    ON personal.future_learning_activity (user_id, status, target_date);
CREATE INDEX ix_lifestyle_activity__user_context_time
    ON personal.lifestyle_activity (user_id, lifestyle_context, occurred_at DESC);
CREATE INDEX ix_lifestyle_activity__moment_context
    ON personal.lifestyle_activity (moment_id, lifestyle_context, updated_at DESC);
CREATE INDEX ix_relationship_connection__user_status
    ON personal.relationship_connection (user_id, status, updated_at DESC);
CREATE INDEX ix_relationship_activity__user_connection_time
    ON personal.relationship_activity (user_id, relationship_connection_id, occurred_at DESC);
CREATE INDEX ix_relationship_activity__moment_time
    ON personal.relationship_activity (moment_id, occurred_at DESC);

COMMIT;
