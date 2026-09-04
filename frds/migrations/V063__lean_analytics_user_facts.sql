BEGIN;

CREATE TABLE IF NOT EXISTS analytics_core.user_daily (
  activity_date DATE NOT NULL,
  user_id UUID NOT NULL,
  session_count INTEGER NOT NULL DEFAULT 0,
  screen_view_count INTEGER NOT NULL DEFAULT 0,
  moments_created INTEGER NOT NULL DEFAULT 0,
  moments_joined INTEGER NOT NULL DEFAULT 0,
  moments_viewed INTEGER NOT NULL DEFAULT 0,
  meaningful_action_count INTEGER NOT NULL DEFAULT 0,
  expenses_added INTEGER NOT NULL DEFAULT 0,
  contributions_recorded INTEGER NOT NULL DEFAULT 0,
  invites_sent INTEGER NOT NULL DEFAULT 0,
  moments_completed INTEGER NOT NULL DEFAULT 0,
  active_flag BOOLEAN NOT NULL DEFAULT FALSE,
  meaningfully_active_flag BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (activity_date, user_id)
);

CREATE TABLE IF NOT EXISTS analytics_core.user_lifecycle_fact (
  user_id UUID PRIMARY KEY,
  registered_at TIMESTAMPTZ NOT NULL,
  onboarding_completed_at TIMESTAMPTZ NULL,
  acquisition_source VARCHAR(100) NULL,
  acquisition_medium VARCHAR(100) NULL,
  campaign_id VARCHAR(100) NULL,
  invite_sourced_flag BOOLEAN NOT NULL DEFAULT FALSE,
  first_moment_created_at TIMESTAMPTZ NULL,
  first_moment_joined_at TIMESTAMPTZ NULL,
  first_meaningful_action_at TIMESTAMPTZ NULL,
  activated_at TIMESTAMPTZ NULL,
  first_completed_moment_at TIMESTAMPTZ NULL,
  second_moment_created_at TIMESTAMPTZ NULL,
  third_moment_created_at TIMESTAMPTZ NULL,
  total_moments_created INTEGER NOT NULL DEFAULT 0,
  total_moments_joined INTEGER NOT NULL DEFAULT 0,
  total_moments_completed INTEGER NOT NULL DEFAULT 0,
  distinct_moment_categories INTEGER NOT NULL DEFAULT 0,
  repeat_creator_flag BOOLEAN NOT NULL DEFAULT FALSE,
  cross_category_flag BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE ON analytics_core.user_daily, analytics_core.user_lifecycle_fact
  TO momentra_app, momentra_analytics_worker;

COMMIT;
