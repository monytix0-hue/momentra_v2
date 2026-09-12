-- 09 importance / actionability vs outcomes (over-promotion check)
SELECT
  a.importance,
  CASE
    WHEN a.actionability_score IS NULL THEN 'unknown'
    WHEN a.actionability_score < 45 THEN 'low'
    WHEN a.actionability_score < 75 THEN 'mid'
    ELSE 'high'
  END AS actionability_band,
  a.outcome,
  count(*) AS n,
  count(*) FILTER (WHERE n.read_at IS NOT NULL) AS opened
FROM platform.notification_decision_audit a
LEFT JOIN platform.user_notification n ON n.user_notification_id = a.user_notification_id
WHERE a.decided_at >= now() - interval '14 days'
GROUP BY 1, 2, 3
ORDER BY 1, 2, 3;
