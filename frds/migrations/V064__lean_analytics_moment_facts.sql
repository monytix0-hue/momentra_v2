BEGIN;

CREATE TABLE IF NOT EXISTS analytics_core.moment_daily (
  activity_date DATE NOT NULL,
  moment_id UUID NOT NULL,
  moment_domain VARCHAR(20) NOT NULL,
  moment_category VARCHAR(100) NULL,
  moment_type VARCHAR(100) NULL,
  creator_user_id UUID NULL,
  meaningful_action_count INTEGER NOT NULL DEFAULT 0,
  unique_active_participants INTEGER NOT NULL DEFAULT 0,
  views INTEGER NOT NULL DEFAULT 0,
  invites_sent INTEGER NOT NULL DEFAULT 0,
  expenses_added INTEGER NOT NULL DEFAULT 0,
  expense_amount_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  contributions_recorded INTEGER NOT NULL DEFAULT 0,
  contribution_amount_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  active_flag BOOLEAN NOT NULL DEFAULT FALSE,
  meaningfully_active_flag BOOLEAN NOT NULL DEFAULT FALSE,
  activated_flag BOOLEAN NOT NULL DEFAULT FALSE,
  completed_flag BOOLEAN NOT NULL DEFAULT FALSE,
  cancelled_flag BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (activity_date, moment_id)
);

CREATE TABLE IF NOT EXISTS analytics_core.moment_lifecycle_fact (
  moment_id UUID PRIMARY KEY,
  creator_user_id UUID NOT NULL,
  moment_domain VARCHAR(20) NOT NULL,
  moment_category VARCHAR(100) NULL,
  moment_type VARCHAR(100) NULL,
  created_at TIMESTAMPTZ NOT NULL,
  activated_at TIMESTAMPTZ NULL,
  completed_at TIMESTAMPTZ NULL,
  cancelled_at TIMESTAMPTZ NULL,
  reopened_at TIMESTAMPTZ NULL,
  first_meaningful_action_at TIMESTAMPTZ NULL,
  last_meaningful_action_at TIMESTAMPTZ NULL,
  time_to_activation_seconds BIGINT NULL,
  time_to_completion_seconds BIGINT NULL,
  participant_peak_count INTEGER NOT NULL DEFAULT 0,
  is_activated BOOLEAN NOT NULL DEFAULT FALSE,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
  current_analytics_state VARCHAR(30) NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS analytics_core.participant_fact (
  moment_id UUID NOT NULL,
  user_id UUID NOT NULL,
  invite_id UUID NULL,
  invited_at TIMESTAMPTZ NULL,
  invite_opened_at TIMESTAMPTZ NULL,
  joined_at TIMESTAMPTZ NULL,
  exited_at TIMESTAMPTZ NULL,
  participant_role VARCHAR(50) NULL,
  was_existing_user BOOLEAN NULL,
  first_meaningful_action_at TIMESTAMPTZ NULL,
  participant_activated_at TIMESTAMPTZ NULL,
  later_became_creator_at TIMESTAMPTZ NULL,
  is_active_participant BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (moment_id, user_id)
);

GRANT SELECT, INSERT, UPDATE ON analytics_core.moment_daily, analytics_core.moment_lifecycle_fact, analytics_core.participant_fact
  TO momentra_app, momentra_analytics_worker;

COMMIT;
