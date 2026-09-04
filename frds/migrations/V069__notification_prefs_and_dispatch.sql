BEGIN;

-- Global push preference on user profile
ALTER TABLE core.user_profile
  ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN core.user_profile.push_notifications_enabled IS
  'When false, FCM peer pushes are suppressed for this user.';

-- Moment-level receive preference on participant
ALTER TABLE collaboration.moment_participant
  ADD COLUMN IF NOT EXISTS notify_on_changes BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN collaboration.moment_participant.notify_on_changes IS
  'When false, suppress moment-scoped peer push for this participant.';

-- Durable per-recipient dispatch log (replaces runtime DDL with event-only PK)
DROP TABLE IF EXISTS platform.notification_dispatch;

CREATE TABLE platform.notification_dispatch (
    domain_event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    event_name TEXT NOT NULL,
    sent_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_notification_dispatch PRIMARY KEY (domain_event_id, user_id),
    CONSTRAINT fk_notification_dispatch__event
        FOREIGN KEY (domain_event_id)
        REFERENCES events.domain_event(domain_event_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_dispatch__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_notification_dispatch__sent_count CHECK (sent_count >= 0)
);

CREATE INDEX ix_notification_dispatch__user_sent
  ON platform.notification_dispatch (user_id, sent_at DESC);

GRANT SELECT, INSERT, UPDATE, DELETE ON platform.notification_dispatch TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.notification_dispatch TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON events.event_consumer_state TO momentra_app;

COMMIT;
