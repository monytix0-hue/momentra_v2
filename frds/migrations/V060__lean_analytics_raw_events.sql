BEGIN;

CREATE TABLE IF NOT EXISTS analytics_raw.events (
  event_id UUID PRIMARY KEY,
  event_name VARCHAR(100) NOT NULL,
  event_version SMALLINT NOT NULL DEFAULT 1,
  occurred_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id UUID NULL,
  anonymous_id UUID NULL,
  session_id UUID NULL,
  moment_id UUID NULL,
  moment_domain VARCHAR(20) NULL CHECK (moment_domain IS NULL OR moment_domain IN ('personal','group','business')),
  moment_category VARCHAR(100) NULL,
  moment_type VARCHAR(100) NULL,
  actor_role VARCHAR(50) NULL,
  platform VARCHAR(20) NULL CHECK (platform IS NULL OR platform IN ('ios','android','web')),
  app_version VARCHAR(30) NULL,
  source_screen VARCHAR(100) NULL,
  correlation_id UUID NULL,
  properties JSONB NOT NULL DEFAULT '{}'::jsonb,
  ingestion_source VARCHAR(30) NOT NULL,
  is_valid BOOLEAN NOT NULL DEFAULT TRUE,
  validation_error_code VARCHAR(100) NULL
);

CREATE TABLE IF NOT EXISTS analytics_raw.event_quarantine (
  quarantine_id BIGSERIAL PRIMARY KEY,
  event_id UUID NULL,
  event_name VARCHAR(100) NULL,
  payload JSONB NOT NULL,
  failure_reason VARCHAR(200) NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ NULL
);

GRANT SELECT, INSERT ON analytics_raw.events TO momentra_app, momentra_analytics_worker;
GRANT SELECT, UPDATE ON analytics_raw.events TO momentra_analytics_worker;
GRANT SELECT, INSERT ON analytics_raw.event_quarantine TO momentra_app, momentra_analytics_worker;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA analytics_raw TO momentra_app, momentra_analytics_worker;

COMMIT;
