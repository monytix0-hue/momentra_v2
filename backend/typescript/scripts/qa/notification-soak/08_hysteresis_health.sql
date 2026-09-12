-- 08 hysteresis health: clear → re-emit buckets
WITH clears AS (
  SELECT
    dedupe_key,
    signal_name,
    explanation_code,
    cleared_at,
    last_emitted_at AS emit_before_clear
  FROM platform.derived_notification_signal
  WHERE cleared_at IS NOT NULL
    AND cleared_at >= now() - interval '30 days'
),
reemits AS (
  SELECT
    c.dedupe_key,
    c.explanation_code,
    c.signal_name,
    c.cleared_at,
    s.last_emitted_at AS reemit_at,
    extract(epoch FROM (s.last_emitted_at - c.cleared_at)) / 3600.0 AS hours_to_reemit
  FROM clears c
  JOIN platform.derived_notification_signal s
    ON s.dedupe_key = c.dedupe_key
   AND s.cleared_at IS NULL
   AND s.last_emitted_at > c.cleared_at
)
SELECT
  coalesce(explanation_code, signal_name) AS code,
  count(*) AS reemits,
  count(*) FILTER (WHERE hours_to_reemit < 1) AS reemit_lt_1h,
  count(*) FILTER (WHERE hours_to_reemit < 6) AS reemit_lt_6h,
  count(*) FILTER (WHERE hours_to_reemit < 24) AS reemit_lt_24h,
  percentile_cont(0.5) WITHIN GROUP (ORDER BY hours_to_reemit) AS p50_hours_to_reemit
FROM reemits
GROUP BY 1
ORDER BY reemit_lt_1h DESC, reemits DESC;

-- Open signal inventory + emit_count
SELECT
  signal_name,
  explanation_code,
  count(*) AS open_signals,
  avg(emit_count)::numeric(10,2) AS avg_emit_count,
  max(emit_count) AS max_emit_count
FROM platform.derived_notification_signal
WHERE cleared_at IS NULL
GROUP BY 1, 2
ORDER BY open_signals DESC;
