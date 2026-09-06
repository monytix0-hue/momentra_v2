BEGIN;

CREATE INDEX IF NOT EXISTS idx_lean_events_occurred ON analytics_raw.events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_lean_events_user_time ON analytics_raw.events (user_id, occurred_at) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lean_events_moment_time ON analytics_raw.events (moment_id, occurred_at) WHERE moment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lean_events_name_time ON analytics_raw.events (event_name, occurred_at);
CREATE INDEX IF NOT EXISTS idx_lean_events_session_time ON analytics_raw.events (session_id, occurred_at) WHERE session_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lean_events_domain_time ON analytics_raw.events (moment_domain, occurred_at) WHERE moment_domain IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lean_user_daily_user_date ON analytics_core.user_daily (user_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_lean_moment_daily_moment_date ON analytics_core.moment_daily (moment_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_lean_moment_daily_active_date ON analytics_core.moment_daily (activity_date, meaningfully_active_flag);
CREATE INDEX IF NOT EXISTS idx_lean_user_lifecycle_registered ON analytics_core.user_lifecycle_fact (registered_at);
CREATE INDEX IF NOT EXISTS idx_lean_moment_lifecycle_created ON analytics_core.moment_lifecycle_fact (created_at);
CREATE INDEX IF NOT EXISTS idx_lean_moment_lifecycle_domain ON analytics_core.moment_lifecycle_fact (moment_domain, created_at);
CREATE INDEX IF NOT EXISTS idx_lean_participant_joined ON analytics_core.participant_fact (joined_at) WHERE joined_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lean_kpi_period_lookup ON analytics_mart.kpi_period (kpi_code, period_type, period_start DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_lean_one_moment_created_event
  ON analytics_raw.events (moment_id)
  WHERE event_name = 'moment_created' AND is_valid = TRUE AND moment_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lean_one_moment_activated_event
  ON analytics_raw.events (moment_id)
  WHERE event_name = 'moment_activated' AND is_valid = TRUE AND moment_id IS NOT NULL;

COMMIT;
