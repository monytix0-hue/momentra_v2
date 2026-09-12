-- 05 useful-action proxies by family (configurable windows)
-- Settlement / balance: 72h
WITH settlement AS (
  SELECT
    a.decision_audit_id,
    a.explanation_code,
    a.user_id,
    a.moment_id,
    a.decided_at,
    EXISTS (
      SELECT 1 FROM events.domain_event de
      WHERE de.event_name = 'SettlementRecorded'
        AND de.actor_user_id = a.user_id
        AND (a.moment_id IS NULL OR de.scope_id = a.moment_id)
        AND de.occurred_at BETWEEN a.decided_at AND a.decided_at + interval '72 hours'
    ) OR EXISTS (
      SELECT 1 FROM platform.derived_notification_signal s
      WHERE s.dedupe_key = a.dedupe_key
        AND s.cleared_at IS NOT NULL
        AND s.cleared_at BETWEEN a.decided_at AND a.decided_at + interval '72 hours'
    ) AS acted
  FROM platform.notification_decision_audit a
  WHERE a.outcome <> 'SUPPRESS'
    AND a.explanation_code IN ('BALANCE_SETTLEABLE', 'BALANCE_CHANGED_MEANINGFUL')
    AND a.decided_at >= now() - interval '14 days'
),
-- Poll: 48h
poll AS (
  SELECT
    a.decision_audit_id,
    a.explanation_code,
    EXISTS (
      SELECT 1 FROM shared.poll_vote v
      WHERE v.voter_user_id = a.user_id
        AND a.dedupe_key LIKE '%POLL:' || v.poll_id::text || '%'
        AND v.created_at BETWEEN a.decided_at AND a.decided_at + interval '48 hours'
    ) AS acted
  FROM platform.notification_decision_audit a
  WHERE a.outcome <> 'SUPPRESS'
    AND a.explanation_code = 'POLL_NEEDS_VOTE'
    AND a.decided_at >= now() - interval '14 days'
),
-- Approvals: 48h (signal clear)
approval AS (
  SELECT
    a.decision_audit_id,
    a.explanation_code,
    EXISTS (
      SELECT 1 FROM platform.derived_notification_signal s
      WHERE s.dedupe_key = a.dedupe_key
        AND s.cleared_at IS NOT NULL
        AND s.cleared_at BETWEEN a.decided_at AND a.decided_at + interval '48 hours'
    ) AS acted
  FROM platform.notification_decision_audit a
  WHERE a.outcome <> 'SUPPRESS'
    AND a.explanation_code IN ('APPROVALS_ACCUMULATING', 'APPROVAL_AGE_48H')
    AND a.decided_at >= now() - interval '14 days'
),
-- Goal/budget: 7d
goal AS (
  SELECT
    a.decision_audit_id,
    a.explanation_code,
    EXISTS (
      SELECT 1 FROM platform.derived_notification_signal s
      WHERE s.dedupe_key = a.dedupe_key
        AND s.cleared_at IS NOT NULL
        AND s.cleared_at BETWEEN a.decided_at AND a.decided_at + interval '7 days'
    ) AS acted
  FROM platform.notification_decision_audit a
  WHERE a.outcome <> 'SUPPRESS'
    AND (
      a.explanation_code LIKE 'GOAL_%'
      OR a.explanation_code LIKE 'BUDGET_%'
      OR a.explanation_code LIKE 'PERSONAL_BUDGET_%'
    )
    AND a.decided_at >= now() - interval '14 days'
),
all_proxies AS (
  SELECT explanation_code, acted FROM settlement
  UNION ALL SELECT explanation_code, acted FROM poll
  UNION ALL SELECT explanation_code, acted FROM approval
  UNION ALL SELECT explanation_code, acted FROM goal
)
SELECT
  explanation_code,
  count(*) AS delivered,
  count(*) FILTER (WHERE acted) AS useful_actions,
  round(100.0 * count(*) FILTER (WHERE acted) / nullif(count(*), 0), 1) AS useful_action_rate_pct
FROM all_proxies
GROUP BY 1
ORDER BY delivered DESC;
