BEGIN;

CREATE TABLE IF NOT EXISTS analytics_mart.user_retention_cohort (
  cohort_date DATE NOT NULL,
  cohort_type VARCHAR(30) NOT NULL,
  cohort_users INTEGER NOT NULL,
  d7_eligible INTEGER NOT NULL DEFAULT 0,
  d7_retained INTEGER NOT NULL DEFAULT 0,
  d7_rate NUMERIC(7,3) NULL,
  d30_eligible INTEGER NOT NULL DEFAULT 0,
  d30_retained INTEGER NOT NULL DEFAULT 0,
  d30_rate NUMERIC(7,3) NULL,
  formula_version SMALLINT NOT NULL DEFAULT 1,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (cohort_date, cohort_type, formula_version)
);

CREATE TABLE IF NOT EXISTS analytics_mart.moment_repeat_cohort (
  cohort_month DATE NOT NULL,
  first_time_creators INTEGER NOT NULL,
  second_moment_eligible INTEGER NOT NULL DEFAULT 0,
  second_moment_creators INTEGER NOT NULL DEFAULT 0,
  second_moment_rate NUMERIC(7,3) NULL,
  third_moment_eligible INTEGER NOT NULL DEFAULT 0,
  third_plus_creators INTEGER NOT NULL DEFAULT 0,
  third_plus_rate NUMERIC(7,3) NULL,
  cross_category_creators INTEGER NOT NULL DEFAULT 0,
  cross_category_rate NUMERIC(7,3) NULL,
  formula_version SMALLINT NOT NULL DEFAULT 1,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (cohort_month, formula_version)
);

CREATE TABLE IF NOT EXISTS analytics_mart.viral_funnel (
  period_type VARCHAR(20) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  invites_issued BIGINT NOT NULL DEFAULT 0,
  invites_opened BIGINT NOT NULL DEFAULT 0,
  joins BIGINT NOT NULL DEFAULT 0,
  invited_activated BIGINT NOT NULL DEFAULT 0,
  participant_to_creator BIGINT NOT NULL DEFAULT 0,
  formula_version SMALLINT NOT NULL DEFAULT 1,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (period_type, period_start, formula_version)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_mart.user_retention_cohort, analytics_mart.moment_repeat_cohort, analytics_mart.viral_funnel
  TO momentra_app, momentra_analytics_worker;
GRANT SELECT ON analytics_mart.user_retention_cohort, analytics_mart.moment_repeat_cohort, analytics_mart.viral_funnel
  TO analytics_read;

COMMIT;
