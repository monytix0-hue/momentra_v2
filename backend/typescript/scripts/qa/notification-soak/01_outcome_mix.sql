-- Attribution windows (per family) used by useful-action proxy queries.
-- settlement/balance: 72h | poll: 48h | task: 72h | approval: 48h | bill: 48h | goal/budget: 7d

-- 01 outcome mix by explanation_code + policy_version
SELECT
  coalesce(explanation_code, '(none)') AS explanation_code,
  policy_version,
  outcome,
  count(*) AS n
FROM platform.notification_decision_audit
WHERE decided_at >= now() - interval '14 days'
GROUP BY 1, 2, 3
ORDER BY 1, 2, 3;
