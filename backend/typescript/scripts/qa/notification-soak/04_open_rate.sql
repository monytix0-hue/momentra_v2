-- 04 open rate + time-to-read by explanation_code
SELECT
  coalesce(n.explanation_code, n.event_name) AS code,
  count(*) AS delivered,
  count(*) FILTER (WHERE n.read_at IS NOT NULL) AS opened,
  round(
    100.0 * count(*) FILTER (WHERE n.read_at IS NOT NULL) / nullif(count(*), 0),
    1
  ) AS open_rate_pct,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY extract(epoch FROM (n.read_at - n.created_at)) / 3600.0
  ) FILTER (WHERE n.read_at IS NOT NULL) AS p50_hours_to_read
FROM platform.user_notification n
WHERE n.created_at >= now() - interval '14 days'
GROUP BY 1
ORDER BY delivered DESC;
