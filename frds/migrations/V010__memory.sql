BEGIN;

CREATE SCHEMA memory;
COMMENT ON SCHEMA memory IS 'Momentra cross-domain durable Memory: evidence, patterns, learning and versioned playbooks.';

CREATE TABLE memory.memory (
    memory_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    domain_code TEXT,
    moment_id UUID,
    title TEXT NOT NULL,
    summary TEXT,
    memory_type TEXT NOT NULL DEFAULT 'GENERAL',
    significance_level TEXT NOT NULL DEFAULT 'MEDIUM',
    visibility_level TEXT NOT NULL DEFAULT 'SCOPE',
    occurred_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source_type TEXT NOT NULL DEFAULT 'USER',
    created_by_user_id UUID,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_memory__moment FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_memory__created_by FOREIGN KEY (created_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_memory__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_memory__domain CHECK (domain_code IS NULL OR domain_code IN ('PERSONAL','GROUP','BUSINESS','CROSS_DOMAIN')),
    CONSTRAINT ck_memory__moment_scope CHECK (scope_type <> 'MOMENT' OR (moment_id IS NOT NULL AND scope_id = moment_id)),
    CONSTRAINT ck_memory__type CHECK (memory_type IN ('GENERAL','EXPERIENCE','MILESTONE','DECISION','FINANCIAL','RELATIONSHIP','BUSINESS','LEARNING','OTHER')),
    CONSTRAINT ck_memory__significance CHECK (significance_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_memory__visibility CHECK (visibility_level IN ('PRIVATE','SCOPE','RESTRICTED')),
    CONSTRAINT ck_memory__status CHECK (status IN ('ACTIVE','ARCHIVED','INVALIDATED','DELETED')),
    CONSTRAINT ck_memory__source CHECK (source_type IN ('USER','SYSTEM','ANALYTICS','AI_ACCEPTED','IMPORT')),
    CONSTRAINT ck_memory__version CHECK (version > 0)
);

CREATE TABLE memory.memory_evidence (
    memory_evidence_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID NOT NULL,
    evidence_role TEXT NOT NULL DEFAULT 'SUPPORTING',
    evidence_snapshot JSONB,
    snapshot_schema_version INTEGER,
    observed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_memory_evidence__memory FOREIGN KEY (memory_id) REFERENCES memory.memory(memory_id) ON DELETE RESTRICT,
    CONSTRAINT uq_memory_evidence__source UNIQUE (memory_id, source_type, source_id, evidence_role),
    CONSTRAINT ck_memory_evidence__source CHECK (source_type IN ('MOMENT','EXPENSE','TASK','DECISION','METRIC_OBSERVATION','EVENT','LIFE_OBSERVATION','RELATIONSHIP_ACTIVITY','BOOKING','OTHER')),
    CONSTRAINT ck_memory_evidence__role CHECK (evidence_role IN ('PRIMARY','SUPPORTING','CONTRADICTING','CONTEXT')),
    CONSTRAINT ck_memory_evidence__schema CHECK (snapshot_schema_version IS NULL OR snapshot_schema_version > 0)
);

CREATE TABLE memory.pattern (
    pattern_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    pattern_code TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    pattern_type TEXT NOT NULL,
    confidence NUMERIC(7,6),
    first_detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_pattern__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_pattern__code CHECK (pattern_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_pattern__type CHECK (pattern_type IN ('BEHAVIOR','FINANCIAL','RELATIONSHIP','WORK','BUSINESS','LIFE','CROSS_DOMAIN','OTHER')),
    CONSTRAINT ck_pattern__confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT ck_pattern__time CHECK (last_detected_at >= first_detected_at),
    CONSTRAINT ck_pattern__status CHECK (status IN ('ACTIVE','CONFIRMED','DISMISSED','SUPERSEDED','ARCHIVED')),
    CONSTRAINT ck_pattern__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_pattern__active_scope_code
    ON memory.pattern (scope_type, scope_id, pattern_code)
    WHERE status IN ('ACTIVE','CONFIRMED');

CREATE TABLE memory.pattern_occurrence (
    pattern_occurrence_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pattern_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    significance NUMERIC(7,6),
    evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pattern_occurrence__pattern FOREIGN KEY (pattern_id) REFERENCES memory.pattern(pattern_id) ON DELETE RESTRICT,
    CONSTRAINT uq_pattern_occurrence__source UNIQUE (pattern_id, source_type, source_id),
    CONSTRAINT ck_pattern_occurrence__source CHECK (source_type IN ('MEMORY','MOMENT','EXPENSE','TASK','METRIC_OBSERVATION','EVENT','OTHER')),
    CONSTRAINT ck_pattern_occurrence__significance CHECK (significance IS NULL OR (significance >= 0 AND significance <= 1))
);

CREATE TABLE memory.learning (
    learning_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    title TEXT NOT NULL,
    learning_text TEXT NOT NULL,
    learning_type TEXT NOT NULL DEFAULT 'GENERAL',
    source_pattern_id UUID,
    supersedes_learning_id UUID,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source_type TEXT NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_learning__pattern FOREIGN KEY (source_pattern_id) REFERENCES memory.pattern(pattern_id) ON DELETE RESTRICT,
    CONSTRAINT fk_learning__supersedes FOREIGN KEY (supersedes_learning_id) REFERENCES memory.learning(learning_id) ON DELETE RESTRICT,
    CONSTRAINT ck_learning__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_learning__type CHECK (learning_type IN ('GENERAL','BEHAVIOR','FINANCIAL','RELATIONSHIP','WORK','BUSINESS','LIFE','OTHER')),
    CONSTRAINT ck_learning__self CHECK (supersedes_learning_id IS NULL OR supersedes_learning_id <> learning_id),
    CONSTRAINT ck_learning__status CHECK (status IN ('ACTIVE','SUPERSEDED','INVALIDATED','ARCHIVED')),
    CONSTRAINT ck_learning__source CHECK (source_type IN ('USER','SYSTEM','ANALYTICS','AI_ACCEPTED')),
    CONSTRAINT ck_learning__version CHECK (version > 0)
);

CREATE TABLE memory.learning_evidence (
    learning_evidence_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    learning_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID NOT NULL,
    evidence_role TEXT NOT NULL DEFAULT 'SUPPORTING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_learning_evidence__learning FOREIGN KEY (learning_id) REFERENCES memory.learning(learning_id) ON DELETE RESTRICT,
    CONSTRAINT uq_learning_evidence__source UNIQUE (learning_id, source_type, source_id, evidence_role),
    CONSTRAINT ck_learning_evidence__source CHECK (source_type IN ('MEMORY','PATTERN','PATTERN_OCCURRENCE','DECISION','METRIC_OBSERVATION','EVENT','OTHER')),
    CONSTRAINT ck_learning_evidence__role CHECK (evidence_role IN ('PRIMARY','SUPPORTING','CONTRADICTING','CONTEXT'))
);

CREATE TABLE memory.playbook (
    playbook_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    code TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    playbook_type TEXT NOT NULL DEFAULT 'GENERAL',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_playbook__scope_code UNIQUE (scope_type, scope_id, code),
    CONSTRAINT ck_playbook__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_playbook__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_playbook__type CHECK (playbook_type IN ('GENERAL','PERSONAL','GROUP','BUSINESS','OPERATIONS','FINANCE','RELATIONSHIP','OTHER')),
    CONSTRAINT ck_playbook__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED'))
);

CREATE TABLE memory.playbook_version (
    playbook_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playbook_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    content JSONB NOT NULL,
    summary TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_playbook_version__playbook FOREIGN KEY (playbook_id) REFERENCES memory.playbook(playbook_id) ON DELETE RESTRICT,
    CONSTRAINT uq_playbook_version__number UNIQUE (playbook_id, version_number),
    CONSTRAINT ck_playbook_version__number CHECK (version_number > 0),
    CONSTRAINT ck_playbook_version__status CHECK (status IN ('DRAFT','ACTIVE','RETIRED')),
    CONSTRAINT ck_playbook_version__time CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX uq_playbook_version__active
    ON memory.playbook_version (playbook_id)
    WHERE status = 'ACTIVE';

CREATE TABLE memory.playbook_evidence (
    playbook_evidence_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playbook_version_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID NOT NULL,
    evidence_role TEXT NOT NULL DEFAULT 'SUPPORTING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_playbook_evidence__version FOREIGN KEY (playbook_version_id) REFERENCES memory.playbook_version(playbook_version_id) ON DELETE RESTRICT,
    CONSTRAINT uq_playbook_evidence__source UNIQUE (playbook_version_id, source_type, source_id, evidence_role),
    CONSTRAINT ck_playbook_evidence__source CHECK (source_type IN ('MEMORY','PATTERN','LEARNING','DECISION','METRIC_OBSERVATION','EVENT','OTHER')),
    CONSTRAINT ck_playbook_evidence__role CHECK (evidence_role IN ('PRIMARY','SUPPORTING','CONTRADICTING','CONTEXT'))
);

CREATE INDEX ix_memory__scope_status_time ON memory.memory (scope_type, scope_id, status, (COALESCE(occurred_at, created_at)) DESC);
CREATE INDEX ix_memory__moment_time ON memory.memory (moment_id, created_at DESC) WHERE moment_id IS NOT NULL;
CREATE INDEX ix_memory_evidence__source ON memory.memory_evidence (source_type, source_id, memory_id);
CREATE INDEX ix_pattern__scope_status_time ON memory.pattern (scope_type, scope_id, status, last_detected_at DESC);
CREATE INDEX ix_pattern_occurrence__pattern_time ON memory.pattern_occurrence (pattern_id, occurred_at DESC);
CREATE INDEX ix_learning__scope_status ON memory.learning (scope_type, scope_id, status, updated_at DESC);
CREATE INDEX ix_learning_evidence__source ON memory.learning_evidence (source_type, source_id, learning_id);
CREATE INDEX ix_playbook__scope_status ON memory.playbook (scope_type, scope_id, status, updated_at DESC);
CREATE INDEX ix_playbook_version__playbook_status ON memory.playbook_version (playbook_id, status, version_number DESC);
CREATE INDEX ix_playbook_evidence__source ON memory.playbook_evidence (source_type, source_id, playbook_version_id);

COMMIT;
