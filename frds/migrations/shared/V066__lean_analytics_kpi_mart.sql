BEGIN;

CREATE TABLE IF NOT EXISTS analytics_core.kpi_registry (
  kpi_code VARCHAR(80) PRIMARY KEY,
  kpi_name VARCHAR(150) NOT NULL,
  category VARCHAR(50) NOT NULL,
  priority VARCHAR(5) NOT NULL,
  audience VARCHAR(20) NOT NULL,
  formula_description TEXT NOT NULL,
  numerator_definition TEXT NULL,
  denominator_definition TEXT NULL,
  observation_window VARCHAR(100) NULL,
  eligible_population_rule TEXT NULL,
  formula_version SMALLINT NOT NULL DEFAULT 1,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS analytics_mart.kpi_period (
  period_type VARCHAR(20) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  kpi_code VARCHAR(80) NOT NULL REFERENCES analytics_core.kpi_registry(kpi_code),
  numerator NUMERIC(20,4) NULL,
  denominator NUMERIC(20,4) NULL,
  kpi_value NUMERIC(20,4) NULL,
  sample_size BIGINT NULL,
  moment_domain VARCHAR(20) NULL,
  moment_category VARCHAR(100) NULL,
  moment_type VARCHAR(100) NULL,
  acquisition_source VARCHAR(100) NULL,
  formula_version SMALLINT NOT NULL DEFAULT 1,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  kpi_period_id BIGSERIAL UNIQUE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_kpi_period_slice
  ON analytics_mart.kpi_period (
    kpi_code, period_type, period_start, period_end, formula_version,
    COALESCE(moment_domain, ''),
    COALESCE(moment_category, ''),
    COALESCE(moment_type, ''),
    COALESCE(acquisition_source, '')
  );

INSERT INTO analytics_core.kpi_registry
  (kpi_code, kpi_name, category, priority, audience, formula_description, numerator_definition, denominator_definition, observation_window, eligible_population_rule, formula_version, is_active)
VALUES
  ('KPI_001_NEW_REGISTERED_USERS', 'New Registered Users', 'Acquisition', 'P0', 'Both', 'Count(distinct user_id with registered_at in period)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_002_USER_GROWTH_RATE', 'User Growth Rate', 'Acquisition', 'P0', 'VC', '(Current new users - Previous new users) / Previous new users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_003_ACQUISITION_SOURCE_MIX', 'Acquisition Source Mix', 'Acquisition', 'P0', 'Both', 'New users by source / all new users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_004_INVITE_SOURCED_USERS', 'Invite-Sourced Users', 'Acquisition', 'P0', 'Both', 'Count(users with invite_sourced_flag=true)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_005_ACTIVATED_ACQUISITION_RATE', 'Activated Acquisition Rate', 'Acquisition', 'P0', 'Both', 'Activated eligible new users / eligible new registered users', NULL, NULL, '14 days', NULL, 1, TRUE),
  ('KPI_006_USER_ACTIVATION_RATE', 'User Activation Rate', 'Activation', 'P0', 'Both', 'Activated users / eligible registered users', NULL, NULL, '14 days', NULL, 1, TRUE),
  ('KPI_007_MEDIAN_TIME_TO_FIRST_MOMENT', 'Median Time to First Moment', 'Activation', 'P0', 'Product', 'median(first_moment_created_at - registered_at)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_008_MEDIAN_TIME_TO_FIRST_MEANINGFUL_ACTION', 'Median Time to First Meaningful Action', 'Activation', 'P0', 'Product', 'median(first_meaningful_action_at - registered_at)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_009_MOMENT_CREATION_COMPLETION_RATE', 'Moment Creation Completion Rate', 'Activation', 'P0', 'Product', 'created creation_flow_id / started creation_flow_id', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_010_MOMENT_CREATION_DROP_OFF', 'Moment Creation Drop-Off', 'Activation', 'P0', 'Product', '1 - step_n_flows / step_n_minus_1_flows', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_011_JOIN_ACTIVATION_RATE', 'Join Activation Rate', 'Activation', 'P1', 'Both', 'activated joined invitees / eligible joined invitees', NULL, NULL, '7 days after join', NULL, 1, TRUE),
  ('KPI_012_MOMENTS_CREATED', 'Moments Created', 'Moment Adoption', 'P0', 'Both', 'count(distinct moment_id created)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM', 'Weekly Active Moments (WAM)', 'Moment Adoption', 'P0', 'Both', 'count(distinct meaningfully active moment_id)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM', 'Monthly Active Moments (MAM)', 'Moment Adoption', 'P0', 'Both', 'count(distinct meaningfully active moment_id)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_015_MOMENTS_PER_ACTIVE_USER', 'Moments per Active User', 'Moment Adoption', 'P0', 'Both', 'active moment-user relationships / meaningfully active users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_016_MOMENT_ACTIVATION_RATE', 'Moment Activation Rate', 'Moment Adoption', 'P0', 'Both', 'activated eligible Moments / eligible created Moments', NULL, NULL, '7 days after creation', NULL, 1, TRUE),
  ('KPI_017_MOMENT_COMPLETION_RATE', 'Moment Completion Rate', 'Moment Adoption', 'P0', 'Both', 'completed mature activated / mature activated', NULL, NULL, '30 days after activation', NULL, 1, TRUE),
  ('KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT', 'Meaningful Actions per Active Moment', 'Moment Adoption', 'P0', 'Both', 'qualifying activity events / active Moments', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_019_MEANINGFUL_ACTIONS_PER_ACTIVE_USER', 'Meaningful Actions per Active User', 'Moment Adoption', 'P1', 'Product', 'qualifying actions / meaningfully active users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT', 'Average Participants per Group Moment', 'Moment Adoption', 'P0', 'Both', 'active participants / Group Moments', 'active participants', 'Group Moments', '', NULL, 1, TRUE),
  ('KPI_021_MOMENT_REVISIT_RATE', 'Moment Revisit Rate', 'Moment Adoption', 'P1', 'Product', 'revisited Moments / eligible viewed Moments', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_022_MOMENT_TYPE__CATEGORY_MIX', 'Moment Type / Category Mix', 'Moment Adoption', 'P0', 'Both', 'created Moments of segment / all created Moments', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_023_D7_MEANINGFUL_RETENTION', 'D7 Meaningful Retention', 'Retention', 'P0', 'Both', 'D7-D9 retained / eligible activated cohort', NULL, NULL, 'D7-D9', NULL, 1, TRUE),
  ('KPI_024_D30_MEANINGFUL_RETENTION', 'D30 Meaningful Retention', 'Retention', 'P0', 'Both', 'D30-D36 retained / eligible activated cohort', NULL, NULL, 'D30-D36', NULL, 1, TRUE),
  ('KPI_025_SECOND_MOMENT_RATE', 'Second Moment Rate', 'Repeat', 'P0', 'Both', '2nd Moment creators / mature first-time creator cohort', NULL, NULL, '60 days', NULL, 1, TRUE),
  ('KPI_026_REPEAT_MOMENT_CREATOR_RATE', 'Repeat Moment Creator Rate', 'Repeat', 'P0', 'Both', 'creators with >=2 Moments / eligible creators', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_027_THIRD_MOMENT_CREATOR_RATE', 'Third+ Moment Creator Rate', 'Repeat', 'P1', 'VC', '3+ creators / mature first-time creator cohort', NULL, NULL, '90 days', NULL, 1, TRUE),
  ('KPI_028_CROSS_CATEGORY_ADOPTION_RATE', 'Cross-Category Adoption Rate', 'Repeat', 'P1', 'Both', 'repeat creators across >=2 categories / repeat creators', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_029_MOMENT_ABANDONMENT_RATE', 'Moment Abandonment Rate', 'Repeat', 'P1', 'Product', 'abandoned / eligible created Moments', NULL, NULL, '14 days', NULL, 1, TRUE),
  ('KPI_030_INVITATIONS_PER_GROUP_MOMENT', 'Invitations per Group Moment', 'Virality', 'P0', 'Both', 'distinct invite_id / eligible Group Moments', 'distinct invite_id', 'eligible Group Moments', '', NULL, 1, TRUE),
  ('KPI_031_INVITE_OPEN_RATE', 'Invite Open Rate', 'Virality', 'P1', 'Product', 'unique invite opens / valid invites issued', 'unique invite opens', 'valid invites issued', '', NULL, 1, TRUE),
  ('KPI_032_INVITE__JOIN_CONVERSION', 'Invite → Join Conversion', 'Virality', 'P0', 'Both', 'joined invite IDs / valid invites issued', 'joined invite IDs', 'valid invites issued', '14 days', NULL, 1, TRUE),
  ('KPI_033_INVITED_USER_ACTIVATION_RATE', 'Invited User Activation Rate', 'Virality', 'P0', 'Both', 'activated invited users / eligible joined invited users', 'activated invited users', 'eligible joined invited users', '7 days', NULL, 1, TRUE),
  ('KPI_034_PARTICIPANT__CREATOR_CONVERSION', 'Participant → Creator Conversion', 'Virality', 'P0', 'Both', 'converted participants / mature eligible participants', 'converted participants', 'mature eligible participants', '60 days', NULL, 1, TRUE),
  ('KPI_035_VIRAL_COEFFICIENT', 'Viral Coefficient', 'Virality', 'P1', 'VC', 'invitees per creator × join rate × participant→creator rate', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_036_QUICK_ADD_COMPLETION_RATE', 'Quick Add Completion Rate', 'Product Experience', 'P0', 'Product', 'completed quick-add flows / started quick-add flows', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_037_FEATURE_ADOPTION_RATE', 'Feature Adoption Rate', 'Product Experience', 'P0', 'Product', 'feature users / eligible active users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_038_CRITICAL_FLOW_ABANDONMENT_RATE', 'Critical Flow Abandonment Rate', 'Product Experience', 'P0', 'Product', 'abandoned flows / started flows', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_039_MEDIAN_SESSION_DURATION', 'Median Session Duration', 'Product Experience', 'P1', 'Product', 'median(last_activity - session_start)', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_040_SESSIONS_PER_ACTIVE_USER', 'Sessions per Active User', 'Product Experience', 'P1', 'Product', 'sessions / active users', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_041_CRITICAL_OPERATION_FAILURE_RATE', 'Critical Operation Failure Rate', 'Product Experience', 'P0', 'Product', 'failures / operation attempts', NULL, NULL, '', NULL, 1, TRUE),
  ('KPI_042_CRASH_FREE_SESSION_RATE', 'Crash-Free Session Rate', 'Product Experience', 'P0', 'Product', 'non-crash sessions / total sessions', NULL, NULL, '', NULL, 1, TRUE)
ON CONFLICT (kpi_code) DO UPDATE SET
  kpi_name = EXCLUDED.kpi_name,
  formula_description = EXCLUDED.formula_description,
  formula_version = EXCLUDED.formula_version,
  is_active = TRUE;

GRANT SELECT ON analytics_core.kpi_registry TO momentra_app, momentra_analytics_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_mart.kpi_period TO momentra_app, momentra_analytics_worker;
GRANT SELECT ON analytics_mart.kpi_period TO analytics_read;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA analytics_mart TO momentra_app, momentra_analytics_worker;

COMMIT;
