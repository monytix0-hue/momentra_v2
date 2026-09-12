-- 03 per-user daily volume p50/p90 split by category (finance vs social/tasks)
WITH daily AS (
  SELECT
    a.user_id,
    date_trunc('day', a.decided_at) AS day,
    CASE
      WHEN a.category_code = 'finance' THEN 'finance'
      WHEN a.category_code IN ('social', 'tasks') THEN 'social_tasks'
      ELSE 'other'
    END AS cat_bucket,
    count(*) FILTER (WHERE a.outcome <> 'SUPPRESS') AS delivered
  FROM platform.notification_decision_audit a
  WHERE a.decided_at >= now() - interval '14 days'
  GROUP BY 1, 2, 3
)
SELECT
  cat_bucket,
  percentile_cont(0.5) WITHIN GROUP (ORDER BY delivered) AS p50_per_user_day,
  percentile_cont(0.9) WITHIN GROUP (ORDER BY delivered) AS p90_per_user_day,
  avg(delivered)::numeric(10,2) AS avg_per_user_day
FROM daily
GROUP BY cat_bucket
ORDER BY cat_bucket;
