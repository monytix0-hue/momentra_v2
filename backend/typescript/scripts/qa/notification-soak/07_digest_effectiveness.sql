-- 07 digest effectiveness: immediate vs digest share + open/action by route
-- Compare within the same policy_version; for before/after, run twice filtered by policy_version.
SELECT
  a.policy_version,
  a.outcome,
  count(*) AS decisions,
  count(n.user_notification_id) AS inbox_rows,
  count(*) FILTER (WHERE n.read_at IS NOT NULL) AS opened,
  round(
    100.0 * count(*) FILTER (WHERE n.read_at IS NOT NULL)
    / nullif(count(n.user_notification_id), 0),
    1
  ) AS open_rate_pct,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY extract(epoch FROM (n.read_at - n.created_at)) / 3600.0
  ) FILTER (WHERE n.read_at IS NOT NULL) AS p50_hours_to_read
FROM platform.notification_decision_audit a
LEFT JOIN platform.user_notification n ON n.user_notification_id = a.user_notification_id
WHERE a.decided_at >= now() - interval '14 days'
  AND a.outcome IN ('IMMEDIATE', 'DEFER_TO_DIGEST')
GROUP BY 1, 2
ORDER BY 1, 2;

-- Push volume comparison helper (immediate pushes vs digest batch markers):
SELECT
  a.policy_version,
  count(*) FILTER (WHERE a.outcome = 'IMMEDIATE') AS immediate_decisions,
  count(*) FILTER (WHERE a.outcome = 'DEFER_TO_DIGEST') AS digest_decisions,
  count(*) FILTER (
    WHERE a.outcome = 'IMMEDIATE' AND nd.sent_count > 0
  ) AS immediate_pushes,
  count(*) FILTER (
    WHERE a.outcome = 'DEFER_TO_DIGEST'
  ) AS deferred_to_digest
FROM platform.notification_decision_audit a
LEFT JOIN platform.notification_dispatch nd
  ON nd.domain_event_id = a.domain_event_id AND nd.user_id = a.user_id
WHERE a.decided_at >= now() - interval '14 days'
GROUP BY 1
ORDER BY 1;
