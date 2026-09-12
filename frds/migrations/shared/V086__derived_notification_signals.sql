BEGIN;

-- Wave 2: derived signal dedupe + hysteresis (threshold crossed once until reset)
CREATE TABLE IF NOT EXISTS platform.derived_notification_signal (
    dedupe_key TEXT PRIMARY KEY,
    signal_name TEXT NOT NULL,
    user_id UUID NOT NULL,
    moment_id UUID,
    explanation_code TEXT NOT NULL,
    importance TEXT NOT NULL DEFAULT 'NORMAL',
    facts JSONB NOT NULL DEFAULT '{}'::jsonb,
    actionability_score NUMERIC(6,2) NOT NULL DEFAULT 0,
    first_emitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_emitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    emit_count INT NOT NULL DEFAULT 1,
    hysteresis_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    cleared_at TIMESTAMPTZ,
    CONSTRAINT fk_derived_notification_signal__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT ck_derived_notification_signal__importance
        CHECK (importance IN ('LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT ck_derived_notification_signal__emit_count CHECK (emit_count > 0)
);

CREATE INDEX IF NOT EXISTS ix_derived_notification_signal__user_time
  ON platform.derived_notification_signal (user_id, last_emitted_at DESC);

CREATE INDEX IF NOT EXISTS ix_derived_notification_signal__signal_open
  ON platform.derived_notification_signal (signal_name, cleared_at)
  WHERE cleared_at IS NULL;

GRANT SELECT, INSERT, UPDATE, DELETE ON platform.derived_notification_signal TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.derived_notification_signal TO momentra_projection_worker;

COMMIT;
