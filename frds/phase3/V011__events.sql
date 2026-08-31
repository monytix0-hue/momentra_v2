BEGIN;

CREATE SCHEMA events;
COMMENT ON SCHEMA events IS 'Momentra domain events, transactional outbox, consumer state, delivery attempts and dead letters.';

CREATE TABLE events.domain_event (
    domain_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_name TEXT NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    domain_code TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_version BIGINT,
    scope_type TEXT,
    scope_id UUID,
    actor_user_id UUID,
    correlation_id UUID NOT NULL,
    causation_id UUID,
    idempotency_key TEXT,
    dedupe_key TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    payload_schema_version INTEGER NOT NULL DEFAULT 1,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata_schema_version INTEGER NOT NULL DEFAULT 1,
    trace_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_domain_event__actor FOREIGN KEY (actor_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_domain_event__causation FOREIGN KEY (causation_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL,
    CONSTRAINT ck_domain_event__name CHECK (event_name ~ '^[A-Z][A-Za-z0-9]+$'),
    CONSTRAINT ck_domain_event__event_version CHECK (event_version > 0),
    CONSTRAINT ck_domain_event__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS','SHARED','PLATFORM')),
    CONSTRAINT ck_domain_event__aggregate_version CHECK (aggregate_version IS NULL OR aggregate_version > 0),
    CONSTRAINT ck_domain_event__scope CHECK (scope_type IS NULL OR scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE','GLOBAL')),
    CONSTRAINT ck_domain_event__scope_id CHECK ((scope_type IS NULL AND scope_id IS NULL) OR (scope_type='GLOBAL' AND scope_id IS NULL) OR (scope_type IS NOT NULL AND scope_type<>'GLOBAL' AND scope_id IS NOT NULL)),
    CONSTRAINT ck_domain_event__payload_schema CHECK (payload_schema_version > 0),
    CONSTRAINT ck_domain_event__metadata_schema CHECK (metadata_schema_version > 0)
);

CREATE UNIQUE INDEX uq_domain_event__dedupe
    ON events.domain_event (dedupe_key)
    WHERE dedupe_key IS NOT NULL;

CREATE TABLE events.outbox_event (
    outbox_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_event_id UUID NOT NULL,
    topic_code TEXT NOT NULL,
    partition_key TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_by TEXT,
    locked_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 10,
    last_error_code TEXT,
    last_error_message TEXT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_outbox_event__event FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE RESTRICT,
    CONSTRAINT uq_outbox_event__event_topic UNIQUE (domain_event_id, topic_code),
    CONSTRAINT ck_outbox_event__topic CHECK (topic_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_outbox_event__status CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED','DEAD_LETTER','CANCELLED')),
    CONSTRAINT ck_outbox_event__attempts CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts),
    CONSTRAINT ck_outbox_event__processing_lock CHECK (status <> 'PROCESSING' OR (locked_by IS NOT NULL AND locked_at IS NOT NULL)),
    CONSTRAINT ck_outbox_event__published CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);

CREATE TABLE events.event_consumer_state (
    event_consumer_state_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_code TEXT NOT NULL,
    domain_event_id UUID NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error_code TEXT,
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_consumer_state__event FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE RESTRICT,
    CONSTRAINT uq_event_consumer_state__consumer_event UNIQUE (consumer_code, domain_event_id),
    CONSTRAINT ck_event_consumer_state__consumer CHECK (consumer_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_event_consumer_state__status CHECK (status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED','SKIPPED','DEAD_LETTER')),
    CONSTRAINT ck_event_consumer_state__attempts CHECK (attempt_count >= 0),
    CONSTRAINT ck_event_consumer_state__completed CHECK (status NOT IN ('SUCCEEDED','SKIPPED','DEAD_LETTER') OR completed_at IS NOT NULL)
);

CREATE TABLE events.event_delivery_attempt (
    event_delivery_attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_event_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    outcome TEXT NOT NULL DEFAULT 'STARTED',
    response_code TEXT,
    error_code TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_delivery_attempt__outbox FOREIGN KEY (outbox_event_id) REFERENCES events.outbox_event(outbox_event_id) ON DELETE RESTRICT,
    CONSTRAINT uq_event_delivery_attempt__number UNIQUE (outbox_event_id, attempt_number),
    CONSTRAINT ck_event_delivery_attempt__number CHECK (attempt_number > 0),
    CONSTRAINT ck_event_delivery_attempt__outcome CHECK (outcome IN ('STARTED','SUCCEEDED','FAILED','TIMEOUT','CANCELLED')),
    CONSTRAINT ck_event_delivery_attempt__completed CHECK (outcome='STARTED' OR completed_at IS NOT NULL)
);

CREATE TABLE events.dead_letter_event (
    dead_letter_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_event_id UUID NOT NULL,
    domain_event_id UUID NOT NULL,
    consumer_code TEXT,
    reason_code TEXT,
    reason_message TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN',
    replay_count INTEGER NOT NULL DEFAULT 0,
    first_failed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_replayed_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_dead_letter_event__outbox FOREIGN KEY (outbox_event_id) REFERENCES events.outbox_event(outbox_event_id) ON DELETE RESTRICT,
    CONSTRAINT fk_dead_letter_event__event FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE RESTRICT,
    CONSTRAINT uq_dead_letter_event__outbox_consumer UNIQUE (outbox_event_id, consumer_code),
    CONSTRAINT ck_dead_letter_event__status CHECK (status IN ('OPEN','REPLAYING','RESOLVED','DISCARDED')),
    CONSTRAINT ck_dead_letter_event__replay_count CHECK (replay_count >= 0),
    CONSTRAINT ck_dead_letter_event__resolved CHECK (status NOT IN ('RESOLVED','DISCARDED') OR resolved_at IS NOT NULL)
);

CREATE INDEX ix_domain_event__aggregate_time ON events.domain_event (aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX ix_domain_event__correlation_time ON events.domain_event (correlation_id, occurred_at);
CREATE INDEX ix_domain_event__scope_time ON events.domain_event (scope_type, scope_id, occurred_at DESC) WHERE scope_type IS NOT NULL;
CREATE INDEX ix_outbox_event__pending ON events.outbox_event (available_at, created_at) WHERE status IN ('PENDING','FAILED');
CREATE INDEX ix_outbox_event__processing_lock ON events.outbox_event (locked_at) WHERE status='PROCESSING';
CREATE INDEX ix_event_consumer_state__consumer_status ON events.event_consumer_state (consumer_code, status, updated_at);
CREATE INDEX ix_dead_letter_event__status_time ON events.dead_letter_event (status, created_at DESC);

COMMIT;
