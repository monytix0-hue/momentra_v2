-- 06 productivity: delivered / read / useful actions / per notif / per active user-day
WITH delivered AS (
  SELECT
    date_trunc('day', decided_at) AS day,
    user_id,
    count(*) AS n
  FROM platform.notification_decision_audit
  WHERE outcome <> 'SUPPRESS'
    AND decided_at >= now() - interval '14 days'
  GROUP BY 1, 2
),
reads AS (
  SELECT
    date_trunc('day', created_at) AS day,
    user_id,
    count(*) FILTER (WHERE read_at IS NOT NULL) AS opened,
    count(*) AS inbox_rows
  FROM platform.user_notification
  WHERE created_at >= now() - interval '14 days'
  GROUP BY 1, 2
),
-- Weak useful-action proxy: signal clears in window (all families)
actions AS (
  SELECT
    date_trunc('day', s.cleared_at) AS day,
    s.user_id,
    count(*) AS useful
  FROM platform.derived_notification_signal s
  WHERE s.cleared_at IS NOT NULL
    AND s.cleared_at >= now() - interval '14 days'
  GROUP BY 1, 2
),
active_days AS (
  SELECT DISTINCT day, user_id FROM delivered
)
SELECT
  (SELECT count(*) FROM platform.notification_decision_audit
   WHERE outcome <> 'SUPPRESS' AND decided_at >= now() - interval '14 days') AS notifications_delivered,
  (SELECT count(*) FROM platform.user_notification
   WHERE read_at IS NOT NULL AND created_at >= now() - interval '14 days') AS notifications_read,
  (SELECT coalesce(sum(useful), 0) FROM actions) AS useful_actions,
  round(
    (SELECT coalesce(sum(useful), 0)::numeric FROM actions)
    / nullif((SELECT count(*) FROM platform.notification_decision_audit
              WHERE outcome <> 'SUPPRESS' AND decided_at >= now() - interval '14 days'), 0),
    3
  ) AS useful_actions_per_notification,
  round(
    (SELECT coalesce(sum(useful), 0)::numeric FROM actions)
    / nullif((SELECT count(*) FROM active_days), 0),
    3
  ) AS useful_actions_per_active_user_day;
