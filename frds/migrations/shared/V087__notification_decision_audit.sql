BEGIN;

-- Wave soak: immutable per-recipient decision telemetry (append-only; never update with opens/actions)
CREATE TABLE IF NOT EXISTS platform.notification_decision_audit (
    decision_audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    domain_event_id UUID,
    user_notification_id UUID,
    event_name TEXT NOT NULL,
    moment_id UUID,
    dedupe_key TEXT,
    explanation_code TEXT,
    signal_name TEXT,
    context_type TEXT,
    outcome TEXT NOT NULL,
    route TEXT NOT NULL,
    suppression_reason TEXT,
    importance TEXT NOT NULL DEFAULT 'NORMAL',
    actionability_score NUMERIC(6,2),
    category_code TEXT NOT NULL,
    relationship TEXT,
    thread_key TEXT,
    decision_version TEXT NOT NULL,
    policy_version TEXT NOT NULL,
    event_occurred_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_decision_audit__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_decision_audit__event
        FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_decision_audit__inbox
        FOREIGN KEY (user_notification_id) REFERENCES platform.user_notification(user_notification_id) ON DELETE SET NULL,
    CONSTRAINT ck_notification_decision_audit__outcome
        CHECK (outcome IN ('IMMEDIATE', 'DEFER_TO_DIGEST', 'SUPPRESS')),
    CONSTRAINT ck_notification_decision_audit__route
        CHECK (route IN ('immediate', 'digest', 'suppress')),
    CONSTRAINT ck_notification_decision_audit__importance
        CHECK (importance IN ('LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT ck_notification_decision_audit__reason
        CHECK (
          suppression_reason IS NULL
          OR suppression_reason IN (
            'actor',
            'uninvolved',
            'muted',
            'category_disabled',
            'quiet_hours',
            'rate_policy',
            'digest_cadence',
            'duplicate',
            'already_seen',
            'stale',
            'invalid_recipient'
          )
        ),
    CONSTRAINT ck_notification_decision_audit__context
        CHECK (context_type IS NULL OR context_type IN ('PERSONAL', 'GROUP', 'BUSINESS'))
);

CREATE INDEX IF NOT EXISTS ix_notification_decision_audit__decided
  ON platform.notification_decision_audit (decided_at DESC);

CREATE INDEX IF NOT EXISTS ix_notification_decision_audit__code_outcome
  ON platform.notification_decision_audit (explanation_code, outcome, decided_at DESC);

CREATE INDEX IF NOT EXISTS ix_notification_decision_audit__policy
  ON platform.notification_decision_audit (policy_version, decided_at DESC);

CREATE INDEX IF NOT EXISTS ix_notification_decision_audit__user_day
  ON platform.notification_decision_audit (user_id, decided_at DESC);

CREATE INDEX IF NOT EXISTS ix_notification_decision_audit__event_user
  ON platform.notification_decision_audit (domain_event_id, user_id);

-- Denormalize calibration fields onto delivered for simple soak joins
ALTER TABLE platform.user_notification
  ADD COLUMN IF NOT EXISTS explanation_code TEXT,
  ADD COLUMN IF NOT EXISTS dedupe_key TEXT,
  ADD COLUMN IF NOT EXISTS actionability_score NUMERIC(6,2),
  ADD COLUMN IF NOT EXISTS decision_route TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_user_notification__decision_route'
  ) THEN
    ALTER TABLE platform.user_notification
      ADD CONSTRAINT ck_user_notification__decision_route
      CHECK (decision_route IS NULL OR decision_route IN ('immediate', 'digest', 'suppress'));
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_user_notification__explanation
  ON platform.user_notification (explanation_code, created_at DESC)
  WHERE explanation_code IS NOT NULL;

GRANT SELECT, INSERT ON platform.notification_decision_audit TO momentra_app;
GRANT SELECT, INSERT ON platform.notification_decision_audit TO momentra_projection_worker;
-- Append-only: no UPDATE/DELETE grants for app roles beyond cascade cleanup via FKs

COMMIT;
