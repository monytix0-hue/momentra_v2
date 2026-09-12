-- 02 latency chain (event → decision → push → read)
SELECT
  coalesce(a.explanation_code, a.event_name) AS code,
  count(*) AS decisions,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY extract(epoch FROM (a.decided_at - a.event_occurred_at))
  ) FILTER (WHERE a.event_occurred_at IS NOT NULL) AS p50_event_to_decision_s,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY extract(epoch FROM (n.pushed_at - a.decided_at))
  ) FILTER (WHERE n.pushed_at IS NOT NULL) AS p50_decision_to_push_s,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY extract(epoch FROM (n.read_at - n.pushed_at))
  ) FILTER (WHERE n.read_at IS NOT NULL AND n.pushed_at IS NOT NULL) AS p50_push_to_read_s
FROM platform.notification_decision_audit a
LEFT JOIN platform.user_notification n ON n.user_notification_id = a.user_notification_id
WHERE a.decided_at >= now() - interval '14 days'
  AND a.outcome <> 'SUPPRESS'
GROUP BY 1
ORDER BY decisions DESC;
