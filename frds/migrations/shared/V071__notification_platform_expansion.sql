BEGIN;

-- Category + quiet-hours prefs on profile
ALTER TABLE core.user_profile
  ADD COLUMN IF NOT EXISTS notification_categories JSONB NOT NULL DEFAULT '{
    "finance": true,
    "tasks": true,
    "social": true,
    "invites": true,
    "approvals": true,
    "reminders": true
  }'::jsonb,
  ADD COLUMN IF NOT EXISTS quiet_hours_start TIME,
  ADD COLUMN IF NOT EXISTS quiet_hours_end TIME,
  ADD COLUMN IF NOT EXISTS digest_enabled BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN core.user_profile.notification_categories IS
  'Per-category push toggles: finance, tasks, social, invites, approvals, reminders.';
COMMENT ON COLUMN core.user_profile.quiet_hours_start IS
  'Local quiet-hours start (profile timezone). NULL = disabled.';
COMMENT ON COLUMN core.user_profile.quiet_hours_end IS
  'Local quiet-hours end (profile timezone). NULL = disabled.';
COMMENT ON COLUMN core.user_profile.digest_enabled IS
  'When true (or during quiet hours), non-urgent pushes batch into a digest.';

-- Group reminder prefs on group context
ALTER TABLE collaboration.group_moment_context
  ADD COLUMN IF NOT EXISTS reminder_preferences JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN collaboration.group_moment_context.reminder_preferences IS
  'Group reminder toggles e.g. billReminders, choreReminders, expenseReminders, photoReminders.';

-- Enrich dispatch log
ALTER TABLE platform.notification_dispatch
  ADD COLUMN IF NOT EXISTS category_code TEXT,
  ADD COLUMN IF NOT EXISTS priority_code TEXT NOT NULL DEFAULT 'NORMAL',
  ADD COLUMN IF NOT EXISTS delivery_channel TEXT NOT NULL DEFAULT 'PUSH',
  ADD COLUMN IF NOT EXISTS failure_reason TEXT;

-- In-app notification inbox
CREATE TABLE IF NOT EXISTS platform.user_notification (
    user_notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    domain_event_id UUID,
    event_name TEXT NOT NULL,
    category_code TEXT NOT NULL,
    priority_code TEXT NOT NULL DEFAULT 'NORMAL',
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    moment_id UUID,
    deep_link TEXT,
    actor_user_id UUID,
    actor_display_name TEXT,
    read_at TIMESTAMPTZ,
    pushed_at TIMESTAMPTZ,
    digest_pending BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_notification__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notification__event
        FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_notification__moment
        FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_notification__priority CHECK (priority_code IN ('HIGH', 'NORMAL', 'LOW')),
    CONSTRAINT ck_user_notification__category CHECK (
      category_code IN ('finance', 'tasks', 'social', 'invites', 'approvals', 'reminders', 'system')
    )
);

CREATE INDEX IF NOT EXISTS ix_user_notification__user_created
  ON platform.user_notification (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_user_notification__user_unread
  ON platform.user_notification (user_id, created_at DESC)
  WHERE read_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_user_notification__digest_pending
  ON platform.user_notification (user_id, created_at ASC)
  WHERE digest_pending = true AND pushed_at IS NULL;

-- Idempotent reminder sends
CREATE TABLE IF NOT EXISTS platform.reminder_dispatch (
    reminder_key TEXT PRIMARY KEY,
    user_id UUID NOT NULL,
    event_name TEXT NOT NULL,
    moment_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    dispatched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_reminder_dispatch__user
        FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_reminder_dispatch__user_time
  ON platform.reminder_dispatch (user_id, dispatched_at DESC);

-- Delivery metrics rollup (worker-maintained counters)
CREATE TABLE IF NOT EXISTS platform.notification_delivery_stats (
    stat_day DATE NOT NULL,
    event_name TEXT NOT NULL DEFAULT '*',
    attempted_count BIGINT NOT NULL DEFAULT 0,
    sent_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    revoked_token_count BIGINT NOT NULL DEFAULT 0,
    digest_batched_count BIGINT NOT NULL DEFAULT 0,
    inbox_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_notification_delivery_stats PRIMARY KEY (stat_day, event_name)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON platform.user_notification TO momentra_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON platform.reminder_dispatch TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.notification_delivery_stats TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.user_notification TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON platform.reminder_dispatch TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON platform.notification_delivery_stats TO momentra_projection_worker;

COMMIT;
