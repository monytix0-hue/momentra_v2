BEGIN;

CREATE SCHEMA analytics;
COMMENT ON SCHEMA analytics IS 'Momentra deterministic analytics: versioned metric definitions, calculations, observations, current values, attention and deterministic insights.';

CREATE TABLE analytics.metric_definition (
    metric_definition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    description TEXT,
    domain_code TEXT,
    output_type TEXT NOT NULL,
    unit_code TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_metric_definition__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_metric_definition__domain CHECK (domain_code IS NULL OR domain_code IN ('PERSONAL','GROUP','BUSINESS','CROSS_DOMAIN')),
    CONSTRAINT ck_metric_definition__output CHECK (output_type IN ('NUMBER','PERCENT','BOOLEAN','TEXT','CATEGORY','JSON')),
    CONSTRAINT ck_metric_definition__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE analytics.metric_version (
    metric_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    formula_type TEXT NOT NULL,
    formula_definition JSONB NOT NULL,
    null_behavior TEXT NOT NULL DEFAULT 'NO_RESULT',
    time_window_definition JSONB NOT NULL DEFAULT '{}'::jsonb,
    minimum_evidence_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_metric_version__definition FOREIGN KEY (metric_definition_id) REFERENCES analytics.metric_definition(metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT uq_metric_version__number UNIQUE (metric_definition_id, version_number),
    CONSTRAINT uq_metric_version__id_definition UNIQUE (metric_version_id, metric_definition_id),
    CONSTRAINT ck_metric_version__number CHECK (version_number > 0),
    CONSTRAINT ck_metric_version__formula_type CHECK (formula_type IN ('EXPRESSION','SQL_TEMPLATE','RULE_SET','AGGREGATION','COMPOSITE')),
    CONSTRAINT ck_metric_version__null_behavior CHECK (null_behavior IN ('NO_RESULT','ZERO','IGNORE','ERROR')),
    CONSTRAINT ck_metric_version__evidence CHECK (minimum_evidence_count >= 0),
    CONSTRAINT ck_metric_version__status CHECK (status IN ('DRAFT','ACTIVE','RETIRED')),
    CONSTRAINT ck_metric_version__time CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX uq_metric_version__active
    ON analytics.metric_version (metric_definition_id)
    WHERE status = 'ACTIVE';

CREATE TABLE analytics.metric_input_definition (
    metric_input_definition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_version_id UUID NOT NULL,
    input_code TEXT NOT NULL,
    source_type TEXT NOT NULL,
    source_reference TEXT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT true,
    data_type TEXT NOT NULL,
    aggregation_rule TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_metric_input_definition__version FOREIGN KEY (metric_version_id) REFERENCES analytics.metric_version(metric_version_id) ON DELETE RESTRICT,
    CONSTRAINT uq_metric_input_definition__code UNIQUE (metric_version_id, input_code),
    CONSTRAINT ck_metric_input_definition__code CHECK (input_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_metric_input_definition__source CHECK (source_type IN ('TABLE_COLUMN','EVENT','METRIC','CONSTANT','SERVICE')),
    CONSTRAINT ck_metric_input_definition__type CHECK (data_type IN ('NUMBER','BOOLEAN','TEXT','TIMESTAMP','DATE','JSON'))
);

CREATE TABLE analytics.metric_dependency (
    metric_dependency_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID NOT NULL,
    depends_on_metric_definition_id UUID NOT NULL,
    dependency_type TEXT NOT NULL DEFAULT 'VALUE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_metric_dependency__metric FOREIGN KEY (metric_definition_id) REFERENCES analytics.metric_definition(metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT fk_metric_dependency__depends_on FOREIGN KEY (depends_on_metric_definition_id) REFERENCES analytics.metric_definition(metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT uq_metric_dependency__pair UNIQUE (metric_definition_id, depends_on_metric_definition_id),
    CONSTRAINT ck_metric_dependency__self CHECK (metric_definition_id <> depends_on_metric_definition_id),
    CONSTRAINT ck_metric_dependency__type CHECK (dependency_type IN ('VALUE','STATE','THRESHOLD'))
);

CREATE TABLE analytics.threshold_definition (
    threshold_definition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_version_id UUID NOT NULL,
    code TEXT NOT NULL,
    comparator TEXT NOT NULL,
    lower_value NUMERIC(24,8),
    upper_value NUMERIC(24,8),
    category_value TEXT,
    severity TEXT NOT NULL,
    creates_attention BOOLEAN NOT NULL DEFAULT false,
    creates_deterministic_insight BOOLEAN NOT NULL DEFAULT false,
    message_template TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_threshold_definition__version FOREIGN KEY (metric_version_id) REFERENCES analytics.metric_version(metric_version_id) ON DELETE RESTRICT,
    CONSTRAINT uq_threshold_definition__code UNIQUE (metric_version_id, code),
    CONSTRAINT ck_threshold_definition__code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_threshold_definition__comparator CHECK (comparator IN ('LT','LTE','EQ','GTE','GT','BETWEEN','CATEGORY_EQ')),
    CONSTRAINT ck_threshold_definition__severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_threshold_definition__sort CHECK (sort_order >= 0),
    CONSTRAINT ck_threshold_definition__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE analytics.calculation_run (
    calculation_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID NOT NULL,
    metric_version_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    trigger_type TEXT NOT NULL,
    trigger_event_id UUID,
    status TEXT NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    input_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_code TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_calculation_run__metric_version FOREIGN KEY (metric_version_id, metric_definition_id) REFERENCES analytics.metric_version(metric_version_id, metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT ck_calculation_run__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_calculation_run__trigger CHECK (trigger_type IN ('EVENT','SCHEDULE','MANUAL','BACKFILL','DEPENDENCY')),
    CONSTRAINT ck_calculation_run__status CHECK (status IN ('RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    CONSTRAINT ck_calculation_run__completed CHECK (status NOT IN ('SUCCEEDED','FAILED','CANCELLED') OR completed_at IS NOT NULL)
);
COMMENT ON COLUMN analytics.calculation_run.trigger_event_id IS 'Late FK to events.domain_event is added in V015 after the events schema exists.';

CREATE TABLE analytics.metric_observation (
    metric_observation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID NOT NULL,
    metric_version_id UUID NOT NULL,
    calculation_run_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    numeric_value NUMERIC(24,8),
    boolean_value BOOLEAN,
    text_value TEXT,
    category_value TEXT,
    json_value JSONB,
    evidence_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_metric_observation__metric_version FOREIGN KEY (metric_version_id, metric_definition_id) REFERENCES analytics.metric_version(metric_version_id, metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT fk_metric_observation__run FOREIGN KEY (calculation_run_id) REFERENCES analytics.calculation_run(calculation_run_id) ON DELETE RESTRICT,
    CONSTRAINT uq_metric_observation__id_scope UNIQUE (metric_observation_id, scope_type, scope_id),
    CONSTRAINT ck_metric_observation__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_metric_observation__one_value CHECK (((numeric_value IS NOT NULL)::int + (boolean_value IS NOT NULL)::int + (text_value IS NOT NULL)::int + (category_value IS NOT NULL)::int + (json_value IS NOT NULL)::int) = 1),
    CONSTRAINT ck_metric_observation__evidence CHECK (evidence_count >= 0)
);

CREATE TABLE analytics.metric_current (
    metric_current_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID NOT NULL,
    metric_version_id UUID NOT NULL,
    metric_observation_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    numeric_value NUMERIC(24,8),
    boolean_value BOOLEAN,
    text_value TEXT,
    category_value TEXT,
    json_value JSONB,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_metric_current__metric_version FOREIGN KEY (metric_version_id, metric_definition_id) REFERENCES analytics.metric_version(metric_version_id, metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT fk_metric_current__observation_scope FOREIGN KEY (metric_observation_id, scope_type, scope_id) REFERENCES analytics.metric_observation(metric_observation_id, scope_type, scope_id) ON DELETE RESTRICT,
    CONSTRAINT uq_metric_current__scope UNIQUE (metric_definition_id, scope_type, scope_id),
    CONSTRAINT ck_metric_current__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_metric_current__one_value CHECK (((numeric_value IS NOT NULL)::int + (boolean_value IS NOT NULL)::int + (text_value IS NOT NULL)::int + (category_value IS NOT NULL)::int + (json_value IS NOT NULL)::int) = 1)
);

CREATE TABLE analytics.attention_item (
    attention_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID,
    metric_observation_id UUID,
    threshold_definition_id UUID,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    attention_code TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT,
    severity TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    dedupe_key TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_attention_item__metric FOREIGN KEY (metric_definition_id) REFERENCES analytics.metric_definition(metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attention_item__observation FOREIGN KEY (metric_observation_id) REFERENCES analytics.metric_observation(metric_observation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attention_item__threshold FOREIGN KEY (threshold_definition_id) REFERENCES analytics.threshold_definition(threshold_definition_id) ON DELETE RESTRICT,
    CONSTRAINT ck_attention_item__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_attention_item__code CHECK (attention_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_attention_item__severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_attention_item__status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','DISMISSED')),
    CONSTRAINT ck_attention_item__resolved CHECK (status <> 'RESOLVED' OR resolved_at IS NOT NULL),
    CONSTRAINT ck_attention_item__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_attention_item__active_dedupe
    ON analytics.attention_item (scope_type, scope_id, dedupe_key)
    WHERE status IN ('OPEN','ACKNOWLEDGED');

CREATE TABLE analytics.deterministic_insight (
    deterministic_insight_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_definition_id UUID,
    metric_observation_id UUID,
    threshold_definition_id UUID,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    insight_code TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'INFO',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_deterministic_insight__metric FOREIGN KEY (metric_definition_id) REFERENCES analytics.metric_definition(metric_definition_id) ON DELETE RESTRICT,
    CONSTRAINT fk_deterministic_insight__observation FOREIGN KEY (metric_observation_id) REFERENCES analytics.metric_observation(metric_observation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_deterministic_insight__threshold FOREIGN KEY (threshold_definition_id) REFERENCES analytics.threshold_definition(threshold_definition_id) ON DELETE RESTRICT,
    CONSTRAINT ck_deterministic_insight__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY','PARTICIPANT','TEAM','RESOURCE')),
    CONSTRAINT ck_deterministic_insight__code CHECK (insight_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_deterministic_insight__severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_deterministic_insight__status CHECK (status IN ('ACTIVE','DISMISSED','EXPIRED','SUPERSEDED')),
    CONSTRAINT ck_deterministic_insight__expiry CHECK (expires_at IS NULL OR expires_at >= generated_at)
);

CREATE INDEX ix_metric_input_definition__version ON analytics.metric_input_definition (metric_version_id, input_code);
CREATE INDEX ix_metric_dependency__metric ON analytics.metric_dependency (metric_definition_id, depends_on_metric_definition_id);
CREATE INDEX ix_threshold_definition__version_status ON analytics.threshold_definition (metric_version_id, status, sort_order);
CREATE INDEX ix_calculation_run__metric_scope_time ON analytics.calculation_run (metric_definition_id, scope_type, scope_id, started_at DESC);
CREATE INDEX ix_calculation_run__status_time ON analytics.calculation_run (status, started_at DESC);
CREATE INDEX ix_metric_observation__metric_scope_time ON analytics.metric_observation (metric_definition_id, scope_type, scope_id, observed_at DESC);
CREATE INDEX ix_metric_current__scope ON analytics.metric_current (scope_type, scope_id, metric_definition_id);
CREATE INDEX ix_attention_item__scope_status_severity ON analytics.attention_item (scope_type, scope_id, status, severity, opened_at DESC);
CREATE INDEX ix_deterministic_insight__scope_status_time ON analytics.deterministic_insight (scope_type, scope_id, status, generated_at DESC);

COMMIT;
