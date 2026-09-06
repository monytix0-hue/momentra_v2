BEGIN;

CREATE SCHEMA audit;
CREATE SCHEMA platform;
COMMENT ON SCHEMA audit IS 'Momentra immutable-oriented action audit history.';
COMMENT ON SCHEMA platform IS 'Momentra technical execution state: idempotency, jobs, distributed locks and checkpoints.';

CREATE TABLE audit.audit_record (
    audit_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type TEXT NOT NULL,
    actor_user_id UUID,
    action_code TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id UUID,
    scope_type TEXT,
    scope_id UUID,
    domain_event_id UUID,
    correlation_id UUID,
    outcome TEXT NOT NULL,
    reason TEXT,
    before_snapshot JSONB,
    after_snapshot JSONB,
    snapshot_schema_version INTEGER,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_record__actor FOREIGN KEY (actor_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_audit_record__actor_type CHECK (actor_type IN ('USER','SYSTEM','AI_WORKFLOW','ADMIN')),
    CONSTRAINT ck_audit_record__actor_user CHECK (actor_type NOT IN ('USER','ADMIN') OR actor_user_id IS NOT NULL),
    CONSTRAINT ck_audit_record__scope CHECK (scope_type IS NULL OR scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE','GLOBAL')),
    CONSTRAINT ck_audit_record__scope_id CHECK ((scope_type IS NULL AND scope_id IS NULL) OR (scope_type='GLOBAL' AND scope_id IS NULL) OR (scope_type IS NOT NULL AND scope_type<>'GLOBAL' AND scope_id IS NOT NULL)),
    CONSTRAINT ck_audit_record__outcome CHECK (outcome IN ('SUCCEEDED','FAILED','DENIED','CONFLICT','NO_OP')),
    CONSTRAINT ck_audit_record__snapshot_schema CHECK (snapshot_schema_version IS NULL OR snapshot_schema_version > 0),
    CONSTRAINT ck_audit_record__snapshot_version_required CHECK ((before_snapshot IS NULL AND after_snapshot IS NULL) OR snapshot_schema_version IS NOT NULL)
);
COMMENT ON COLUMN audit.audit_record.domain_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE TABLE platform.idempotency_record (
    idempotency_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_code TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PROCESSING',
    resource_type TEXT,
    resource_id UUID,
    response_code INTEGER,
    response_payload JSONB,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    locked_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_record__operation_key UNIQUE (operation_code, idempotency_key),
    CONSTRAINT ck_idempotency_record__operation CHECK (operation_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_idempotency_record__status CHECK (status IN ('PROCESSING','SUCCEEDED','FAILED','EXPIRED')),
    CONSTRAINT ck_idempotency_record__completed CHECK (status NOT IN ('SUCCEEDED','FAILED') OR completed_at IS NOT NULL),
    CONSTRAINT ck_idempotency_record__expiry CHECK (expires_at IS NULL OR expires_at >= started_at)
);

CREATE TABLE platform.distributed_lock (
    lock_key TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    heartbeat_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_distributed_lock__key CHECK (length(lock_key) > 0),
    CONSTRAINT ck_distributed_lock__expiry CHECK (expires_at > acquired_at)
);

CREATE TABLE platform.job_execution (
    job_execution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_code TEXT NOT NULL,
    job_type TEXT NOT NULL,
    scope_key TEXT,
    status TEXT NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    processed_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    error_code TEXT,
    error_message TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_job_execution__code CHECK (job_code ~ '^[A-Z][A-Z0-9_.-]*$'),
    CONSTRAINT ck_job_execution__type CHECK (job_type IN ('OUTBOX','ANALYTICS','MEMORY','AI','PROJECTION','ARCHIVAL','CLEANUP','MIGRATION','BACKFILL','OTHER')),
    CONSTRAINT ck_job_execution__status CHECK (status IN ('RUNNING','SUCCEEDED','FAILED','CANCELLED','PARTIAL')),
    CONSTRAINT ck_job_execution__completed CHECK ((status='RUNNING' AND completed_at IS NULL) OR (status<>'RUNNING' AND completed_at IS NOT NULL)),
    CONSTRAINT ck_job_execution__counts CHECK (processed_count >= 0 AND success_count >= 0 AND failure_count >= 0 AND success_count + failure_count <= processed_count)
);

CREATE TABLE platform.processing_checkpoint (
    processing_checkpoint_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processor_code TEXT NOT NULL,
    partition_key TEXT NOT NULL DEFAULT 'DEFAULT',
    checkpoint_event_id UUID,
    checkpoint_value TEXT,
    checkpoint_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_processing_checkpoint__processor_partition UNIQUE (processor_code, partition_key),
    CONSTRAINT ck_processing_checkpoint__processor CHECK (processor_code ~ '^[A-Z][A-Z0-9_.-]*$')
);
COMMENT ON COLUMN platform.processing_checkpoint.checkpoint_event_id IS 'Late FK to events.domain_event is added in V015.';

CREATE INDEX ix_audit_record__resource_time ON audit.audit_record (resource_type, resource_id, occurred_at DESC) WHERE resource_id IS NOT NULL;
CREATE INDEX ix_audit_record__actor_time ON audit.audit_record (actor_user_id, occurred_at DESC) WHERE actor_user_id IS NOT NULL;
CREATE INDEX ix_audit_record__correlation ON audit.audit_record (correlation_id, occurred_at) WHERE correlation_id IS NOT NULL;
CREATE INDEX ix_idempotency_record__status_time ON platform.idempotency_record (status, updated_at);
CREATE INDEX ix_idempotency_record__resource ON platform.idempotency_record (resource_type, resource_id) WHERE resource_id IS NOT NULL;
CREATE INDEX ix_job_execution__code_status_time ON platform.job_execution (job_code, status, started_at DESC);

COMMIT;
