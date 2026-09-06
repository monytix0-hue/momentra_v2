BEGIN;

-- Pack V040 validation + dashboard views, plus Group Lean KPI compute views (20, 30–35).

CREATE OR REPLACE VIEW analytics_core.v_event_validation_summary AS
SELECT
  date_trunc('day', received_at)::date AS received_date,
  COUNT(*) AS events_received,
  COUNT(*) FILTER (WHERE is_valid) AS events_valid,
  COUNT(*) FILTER (WHERE NOT is_valid) AS events_invalid
FROM analytics_raw.events
GROUP BY 1;

CREATE OR REPLACE VIEW analytics_core.v_invalid_kpi_rates AS
SELECT *
FROM analytics_mart.kpi_period
WHERE denominator IS NOT NULL
  AND (kpi_value < 0 OR kpi_value > 100 OR numerator > denominator);

CREATE OR REPLACE VIEW analytics_core.v_lifecycle_anomalies AS
SELECT *
FROM analytics_core.moment_lifecycle_fact
WHERE (activated_at IS NOT NULL AND activated_at < created_at)
   OR (completed_at IS NOT NULL AND completed_at < created_at)
   OR (completed_at IS NOT NULL AND activated_at IS NOT NULL AND completed_at < activated_at);

CREATE OR REPLACE VIEW analytics_mart.v_founder_latest AS
SELECT DISTINCT ON (kpi_code)
  kpi_code, period_type, period_start, period_end, numerator, denominator, kpi_value, sample_size, formula_version
FROM analytics_mart.kpi_period
WHERE moment_domain IS NULL AND acquisition_source IS NULL
ORDER BY kpi_code, period_start DESC, calculated_at DESC;

CREATE OR REPLACE VIEW analytics_mart.v_wam_12_weeks AS
SELECT period_start, kpi_value AS wam, numerator, denominator
FROM analytics_mart.kpi_period
WHERE kpi_code LIKE 'KPI_013_%' AND period_type = 'week'
ORDER BY period_start DESC
LIMIT 12;

CREATE OR REPLACE VIEW analytics_mart.v_second_moment_cohorts AS
SELECT cohort_month, first_time_creators, second_moment_eligible, second_moment_creators, second_moment_rate
FROM analytics_mart.moment_repeat_cohort
ORDER BY cohort_month;

-- KPI_020: Average Participants per Group Moment
CREATE OR REPLACE VIEW analytics_mart.v_kpi_020_avg_participants_per_group_moment AS
SELECT
  AVG(cnt)::numeric AS kpi_value,
  SUM(cnt)::numeric AS numerator,
  COUNT(*)::numeric AS denominator,
  COUNT(*)::bigint AS sample_size
FROM (
  SELECT pf.moment_id, COUNT(*) AS cnt
  FROM analytics_core.participant_fact pf
  JOIN analytics_core.moment_lifecycle_fact mf USING (moment_id)
  WHERE mf.moment_domain = 'group'
    AND pf.is_active_participant
  GROUP BY pf.moment_id
) x;

-- KPI_030: Invitations per Group Moment (all-time; period filter applied by refresh job)
CREATE OR REPLACE VIEW analytics_mart.v_kpi_030_invitations_per_group_moment AS
SELECT
  COUNT(DISTINCT (properties->>'invite_id'))::numeric
    / NULLIF(COUNT(DISTINCT moment_id), 0) AS kpi_value,
  COUNT(DISTINCT (properties->>'invite_id'))::numeric AS numerator,
  COUNT(DISTINCT moment_id)::numeric AS denominator,
  COUNT(DISTINCT moment_id)::bigint AS sample_size
FROM analytics_raw.events
WHERE event_name = 'participant_invited'
  AND moment_domain = 'group'
  AND is_valid;

-- KPI_031: Invite Open Rate
CREATE OR REPLACE VIEW analytics_mart.v_kpi_031_invite_open_rate AS
WITH i AS (
  SELECT DISTINCT properties->>'invite_id' AS id
  FROM analytics_raw.events
  WHERE event_name = 'participant_invited' AND is_valid AND properties->>'invite_id' IS NOT NULL
),
o AS (
  SELECT DISTINCT properties->>'invite_id' AS id
  FROM analytics_raw.events
  WHERE event_name = 'invite_opened' AND is_valid AND properties->>'invite_id' IS NOT NULL
)
SELECT
  (SELECT COUNT(*) FROM o)::numeric * 100.0 / NULLIF((SELECT COUNT(*) FROM i), 0) AS kpi_value,
  (SELECT COUNT(*) FROM o)::numeric AS numerator,
  (SELECT COUNT(*) FROM i)::numeric AS denominator,
  (SELECT COUNT(*) FROM i)::bigint AS sample_size;

-- KPI_032: Invite → Join Conversion (mature invites issued >= 14d ago)
CREATE OR REPLACE VIEW analytics_mart.v_kpi_032_invite_join_conversion AS
WITH i AS (
  SELECT DISTINCT properties->>'invite_id' AS id
  FROM analytics_raw.events
  WHERE event_name = 'participant_invited'
    AND is_valid
    AND occurred_at <= now() - INTERVAL '14 days'
    AND properties->>'invite_id' IS NOT NULL
),
j AS (
  SELECT DISTINCT properties->>'invite_id' AS id
  FROM analytics_raw.events
  WHERE event_name = 'participant_joined'
    AND is_valid
    AND properties->>'invite_id' IS NOT NULL
)
SELECT
  COUNT(j.id)::numeric * 100.0 / NULLIF(COUNT(i.id), 0) AS kpi_value,
  COUNT(j.id)::numeric AS numerator,
  COUNT(i.id)::numeric AS denominator,
  COUNT(i.id)::bigint AS sample_size
FROM i
LEFT JOIN j USING (id);

-- KPI_033: Invited User Activation Rate
CREATE OR REPLACE VIEW analytics_mart.v_kpi_033_invited_user_activation_rate AS
SELECT
  COUNT(*) FILTER (
    WHERE participant_activated_at IS NOT NULL
      AND participant_activated_at <= joined_at + INTERVAL '7 days'
  )::numeric * 100.0 / NULLIF(COUNT(*), 0) AS kpi_value,
  COUNT(*) FILTER (
    WHERE participant_activated_at IS NOT NULL
      AND participant_activated_at <= joined_at + INTERVAL '7 days'
  )::numeric AS numerator,
  COUNT(*)::numeric AS denominator,
  COUNT(*)::bigint AS sample_size
FROM analytics_core.participant_fact
WHERE joined_at IS NOT NULL
  AND joined_at <= now() - INTERVAL '7 days'
  AND invite_id IS NOT NULL;

-- KPI_034: Participant → Creator Conversion
CREATE OR REPLACE VIEW analytics_mart.v_kpi_034_participant_creator_conversion AS
WITH first_participation AS (
  SELECT user_id, MIN(joined_at) AS joined_at
  FROM analytics_core.participant_fact
  WHERE joined_at IS NOT NULL
  GROUP BY user_id
),
eligible AS (
  SELECT fp.user_id, fp.joined_at, u.first_moment_created_at
  FROM first_participation fp
  JOIN analytics_core.user_lifecycle_fact u USING (user_id)
  WHERE fp.joined_at <= now() - INTERVAL '60 days'
    AND (u.first_moment_created_at IS NULL OR u.first_moment_created_at > fp.joined_at)
)
SELECT
  COUNT(*) FILTER (
    WHERE first_moment_created_at IS NOT NULL
      AND first_moment_created_at <= joined_at + INTERVAL '60 days'
  )::numeric * 100.0 / NULLIF(COUNT(*), 0) AS kpi_value,
  COUNT(*) FILTER (
    WHERE first_moment_created_at IS NOT NULL
      AND first_moment_created_at <= joined_at + INTERVAL '60 days'
  )::numeric AS numerator,
  COUNT(*)::numeric AS denominator,
  COUNT(*)::bigint AS sample_size
FROM eligible;

-- KPI_035: Viral Coefficient = invitees/creator × join_rate × p2c_rate (components as fractions)
CREATE OR REPLACE VIEW analytics_mart.v_kpi_035_viral_coefficient AS
WITH creators AS (
  SELECT COUNT(DISTINCT creator_user_id)::numeric AS n
  FROM analytics_core.moment_lifecycle_fact
  WHERE moment_domain = 'group'
),
invitees AS (
  SELECT COUNT(DISTINCT properties->>'invite_id')::numeric AS n
  FROM analytics_raw.events
  WHERE event_name = 'participant_invited' AND moment_domain = 'group' AND is_valid
),
join_rate AS (
  SELECT COALESCE(kpi_value, 0) / 100.0 AS rate FROM analytics_mart.v_kpi_032_invite_join_conversion
),
p2c AS (
  SELECT COALESCE(kpi_value, 0) / 100.0 AS rate FROM analytics_mart.v_kpi_034_participant_creator_conversion
)
SELECT
  (SELECT n FROM invitees) / NULLIF((SELECT n FROM creators), 0)
    * (SELECT rate FROM join_rate)
    * (SELECT rate FROM p2c) AS kpi_value,
  (SELECT n FROM invitees) AS numerator,
  (SELECT n FROM creators) AS denominator,
  (SELECT n FROM creators)::bigint AS sample_size;

GRANT SELECT ON ALL TABLES IN SCHEMA analytics_core TO momentra_app, momentra_analytics_worker;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics_mart TO momentra_app, momentra_analytics_worker;

COMMIT;
