BEGIN;

CREATE TABLE IF NOT EXISTS analytics_core.identity_map (
  anonymous_id UUID NOT NULL,
  user_id UUID NOT NULL,
  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  link_source VARCHAR(50) NOT NULL,
  PRIMARY KEY (anonymous_id, user_id)
);

CREATE TABLE IF NOT EXISTS analytics_core.session_fact (
  session_id UUID PRIMARY KEY,
  user_id UUID NULL,
  anonymous_id UUID NULL,
  session_started_at TIMESTAMPTZ NOT NULL,
  session_last_activity_at TIMESTAMPTZ NOT NULL,
  session_duration_seconds BIGINT NOT NULL DEFAULT 0,
  platform VARCHAR(20) NULL,
  app_version VARCHAR(30) NULL,
  entry_source VARCHAR(50) NULL,
  screen_view_count INTEGER NOT NULL DEFAULT 0,
  meaningful_action_count INTEGER NOT NULL DEFAULT 0,
  moment_count_touched INTEGER NOT NULL DEFAULT 0,
  had_crash BOOLEAN NOT NULL DEFAULT FALSE,
  had_critical_failure BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE ON analytics_core.identity_map, analytics_core.session_fact
  TO momentra_app, momentra_analytics_worker;

COMMIT;
