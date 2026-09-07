-- Classify KPI values so validation does not assume every kpi_value is a 0–100 rate.
-- Note: migrate runner wraps each file in BEGIN/COMMIT; do not nest transaction control here.
ALTER TABLE analytics_core.kpi_registry
  ADD COLUMN IF NOT EXISTS value_kind VARCHAR(30) NOT NULL DEFAULT 'percent_bounded';

ALTER TABLE analytics_core.kpi_registry
  DROP CONSTRAINT IF EXISTS kpi_registry_value_kind_check;

ALTER TABLE analytics_core.kpi_registry
  ADD CONSTRAINT kpi_registry_value_kind_check
  CHECK (value_kind IN (
    'percent_bounded',
    'percent_unbounded',
    'count',
    'ratio',
    'duration_seconds'
  ));

UPDATE analytics_core.kpi_registry SET value_kind = 'count'
WHERE kpi_code IN (
  'KPI_001_NEW_REGISTERED_USERS',
  'KPI_004_INVITE_SOURCED_USERS',
  'KPI_012_MOMENTS_CREATED',
  'KPI_013_WEEKLY_ACTIVE_MOMENTS_WAM',
  'KPI_014_MONTHLY_ACTIVE_MOMENTS_MAM'
);

UPDATE analytics_core.kpi_registry SET value_kind = 'percent_unbounded'
WHERE kpi_code = 'KPI_002_USER_GROWTH_RATE';

UPDATE analytics_core.kpi_registry SET value_kind = 'ratio'
WHERE kpi_code IN (
  'KPI_015_MOMENTS_PER_ACTIVE_USER',
  'KPI_018_MEANINGFUL_ACTIONS_PER_ACTIVE_MOMENT',
  'KPI_019_MEANINGFUL_ACTIONS_PER_ACTIVE_USER',
  'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT',
  'KPI_030_INVITATIONS_PER_GROUP_MOMENT',
  'KPI_035_VIRAL_COEFFICIENT',
  'KPI_040_SESSIONS_PER_ACTIVE_USER'
);

UPDATE analytics_core.kpi_registry SET value_kind = 'duration_seconds'
WHERE kpi_code IN (
  'KPI_007_MEDIAN_TIME_TO_FIRST_MOMENT',
  'KPI_008_MEDIAN_TIME_TO_FIRST_MEANINGFUL_ACTION',
  'KPI_039_MEDIAN_SESSION_DURATION'
);

UPDATE analytics_core.kpi_registry SET value_kind = 'percent_bounded'
WHERE kpi_code IN (
  'KPI_003_ACQUISITION_SOURCE_MIX',
  'KPI_005_ACTIVATED_ACQUISITION_RATE',
  'KPI_006_USER_ACTIVATION_RATE',
  'KPI_009_MOMENT_CREATION_COMPLETION_RATE',
  'KPI_010_MOMENT_CREATION_DROP_OFF',
  'KPI_011_JOIN_ACTIVATION_RATE',
  'KPI_016_MOMENT_ACTIVATION_RATE',
  'KPI_017_MOMENT_COMPLETION_RATE',
  'KPI_021_MOMENT_REVISIT_RATE',
  'KPI_022_MOMENT_TYPE__CATEGORY_MIX',
  'KPI_023_D7_MEANINGFUL_RETENTION',
  'KPI_024_D30_MEANINGFUL_RETENTION',
  'KPI_025_SECOND_MOMENT_RATE',
  'KPI_026_REPEAT_MOMENT_CREATOR_RATE',
  'KPI_027_THIRD_MOMENT_CREATOR_RATE',
  'KPI_028_CROSS_CATEGORY_ADOPTION_RATE',
  'KPI_029_MOMENT_ABANDONMENT_RATE',
  'KPI_031_INVITE_OPEN_RATE',
  'KPI_032_INVITE__JOIN_CONVERSION',
  'KPI_033_INVITED_USER_ACTIVATION_RATE',
  'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
  'KPI_036_QUICK_ADD_COMPLETION_RATE',
  'KPI_037_FEATURE_ADOPTION_RATE',
  'KPI_038_CRITICAL_FLOW_ABANDONMENT_RATE',
  'KPI_041_CRITICAL_OPERATION_FAILURE_RATE',
  'KPI_042_CRASH_FREE_SESSION_RATE'
);

CREATE OR REPLACE VIEW analytics_core.v_invalid_kpi_rates AS
SELECT k.*
FROM analytics_mart.kpi_period k
JOIN analytics_core.kpi_registry r ON r.kpi_code = k.kpi_code
WHERE k.denominator IS NOT NULL
  AND (
    (
      r.value_kind = 'percent_bounded'
      AND (k.kpi_value < 0 OR k.kpi_value > 100 OR k.numerator > k.denominator)
    )
    OR (
      r.value_kind IN ('count', 'ratio', 'duration_seconds')
      AND k.kpi_value < 0
    )
  );

GRANT SELECT ON analytics_core.kpi_registry TO momentra_app, momentra_analytics_worker;
