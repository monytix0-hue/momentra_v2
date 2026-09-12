import type { PoolClient } from 'pg';

export type SoakOverview = {
  windowDays: number;
  policyVersions: string[];
  outcomeMix: Array<{
    explanationCode: string;
    policyVersion: string;
    outcome: string;
    count: number;
  }>;
  volumeByCategory: Array<{
    categoryBucket: string;
    p50PerUserDay: number | null;
    p90PerUserDay: number | null;
    avgPerUserDay: number | null;
  }>;
  openRates: Array<{
    code: string;
    delivered: number;
    opened: number;
    openRatePct: number | null;
  }>;
  productivity: {
    notificationsDelivered: number;
    notificationsRead: number;
    usefulActions: number;
    usefulActionsPerNotification: number | null;
    usefulActionsPerActiveUserDay: number | null;
  };
  digestByPolicy: Array<{
    policyVersion: string;
    immediateDecisions: number;
    digestDecisions: number;
    immediatePushes: number;
  }>;
  hysteresis: Array<{
    code: string;
    reemits: number;
    reemitLt1h: number;
    reemitLt6h: number;
    reemitLt24h: number;
  }>;
  suppressionReasons: Array<{
    reason: string;
    count: number;
  }>;
};

function num(v: string | number | null | undefined): number | null {
  if (v == null) return null;
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : null;
}

/** Admin soak rollups — mirrors scripts/qa/notification-soak/*.sql */
export async function getNotificationSoakOverview(
  client: PoolClient,
  windowDays = 14
): Promise<SoakOverview> {
  const days = Math.min(90, Math.max(1, windowDays));

  const policies = await client.query<{ policy_version: string }>(
    `SELECT DISTINCT policy_version
     FROM platform.notification_decision_audit
     WHERE decided_at >= now() - ($1::text || ' days')::interval
     ORDER BY 1`,
    [String(days)]
  );

  const outcomeMix = await client.query<{
    explanation_code: string;
    policy_version: string;
    outcome: string;
    n: string;
  }>(
    `SELECT coalesce(explanation_code, '(none)') AS explanation_code,
            policy_version, outcome, count(*)::text AS n
     FROM platform.notification_decision_audit
     WHERE decided_at >= now() - ($1::text || ' days')::interval
     GROUP BY 1, 2, 3
     ORDER BY n::int DESC
     LIMIT 200`,
    [String(days)]
  );

  const volume = await client.query<{
    cat_bucket: string;
    p50: string | null;
    p90: string | null;
    avg: string | null;
  }>(
    `WITH daily AS (
       SELECT a.user_id,
              date_trunc('day', a.decided_at) AS day,
              CASE
                WHEN a.category_code = 'finance' THEN 'finance'
                WHEN a.category_code IN ('social', 'tasks') THEN 'social_tasks'
                ELSE 'other'
              END AS cat_bucket,
              count(*) FILTER (WHERE a.outcome <> 'SUPPRESS') AS delivered
       FROM platform.notification_decision_audit a
       WHERE a.decided_at >= now() - ($1::text || ' days')::interval
       GROUP BY 1, 2, 3
     )
     SELECT cat_bucket,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY delivered)::text AS p50,
            percentile_cont(0.9) WITHIN GROUP (ORDER BY delivered)::text AS p90,
            avg(delivered)::text AS avg
     FROM daily
     GROUP BY cat_bucket
     ORDER BY cat_bucket`,
    [String(days)]
  );

  const opens = await client.query<{
    code: string;
    delivered: string;
    opened: string;
    open_rate_pct: string | null;
  }>(
    `SELECT coalesce(n.explanation_code, n.event_name) AS code,
            count(*)::text AS delivered,
            count(*) FILTER (WHERE n.read_at IS NOT NULL)::text AS opened,
            round(
              100.0 * count(*) FILTER (WHERE n.read_at IS NOT NULL) / nullif(count(*), 0),
              1
            )::text AS open_rate_pct
     FROM platform.user_notification n
     WHERE n.created_at >= now() - ($1::text || ' days')::interval
     GROUP BY 1
     ORDER BY delivered::int DESC
     LIMIT 50`,
    [String(days)]
  );

  const productivity = await client.query<{
    notifications_delivered: string;
    notifications_read: string;
    useful_actions: string;
    useful_actions_per_notification: string | null;
    useful_actions_per_active_user_day: string | null;
  }>(
    `WITH delivered AS (
       SELECT date_trunc('day', decided_at) AS day, user_id
       FROM platform.notification_decision_audit
       WHERE outcome <> 'SUPPRESS'
         AND decided_at >= now() - ($1::text || ' days')::interval
     ),
     actions AS (
       SELECT count(*)::text AS useful
       FROM platform.derived_notification_signal
       WHERE cleared_at IS NOT NULL
         AND cleared_at >= now() - ($1::text || ' days')::interval
     ),
     active_days AS (
       SELECT count(*)::text AS n FROM (SELECT DISTINCT day, user_id FROM delivered) d
     )
     SELECT
       (SELECT count(*)::text FROM platform.notification_decision_audit
        WHERE outcome <> 'SUPPRESS'
          AND decided_at >= now() - ($1::text || ' days')::interval) AS notifications_delivered,
       (SELECT count(*)::text FROM platform.user_notification
        WHERE read_at IS NOT NULL
          AND created_at >= now() - ($1::text || ' days')::interval) AS notifications_read,
       (SELECT useful FROM actions) AS useful_actions,
       round(
         (SELECT useful::numeric FROM actions)
         / nullif((SELECT count(*) FROM platform.notification_decision_audit
                   WHERE outcome <> 'SUPPRESS'
                     AND decided_at >= now() - ($1::text || ' days')::interval), 0),
         3
       )::text AS useful_actions_per_notification,
       round(
         (SELECT useful::numeric FROM actions)
         / nullif((SELECT n::numeric FROM active_days), 0),
         3
       )::text AS useful_actions_per_active_user_day`,
    [String(days)]
  );

  const digest = await client.query<{
    policy_version: string;
    immediate_decisions: string;
    digest_decisions: string;
    immediate_pushes: string;
  }>(
    `SELECT a.policy_version,
            count(*) FILTER (WHERE a.outcome = 'IMMEDIATE')::text AS immediate_decisions,
            count(*) FILTER (WHERE a.outcome = 'DEFER_TO_DIGEST')::text AS digest_decisions,
            count(*) FILTER (
              WHERE a.outcome = 'IMMEDIATE' AND nd.sent_count > 0
            )::text AS immediate_pushes
     FROM platform.notification_decision_audit a
     LEFT JOIN platform.notification_dispatch nd
       ON nd.domain_event_id = a.domain_event_id AND nd.user_id = a.user_id
     WHERE a.decided_at >= now() - ($1::text || ' days')::interval
     GROUP BY 1
     ORDER BY 1`,
    [String(days)]
  );

  const hysteresis = await client.query<{
    code: string;
    reemits: string;
    reemit_lt_1h: string;
    reemit_lt_6h: string;
    reemit_lt_24h: string;
  }>(
    `WITH clears AS (
       SELECT dedupe_key, signal_name, explanation_code, cleared_at
       FROM platform.derived_notification_signal
       WHERE cleared_at IS NOT NULL
         AND cleared_at >= now() - interval '30 days'
     ),
     reemits AS (
       SELECT coalesce(c.explanation_code, c.signal_name) AS code,
              extract(epoch FROM (s.last_emitted_at - c.cleared_at)) / 3600.0 AS hours_to_reemit
       FROM clears c
       JOIN platform.derived_notification_signal s
         ON s.dedupe_key = c.dedupe_key
        AND s.cleared_at IS NULL
        AND s.last_emitted_at > c.cleared_at
     )
     SELECT code,
            count(*)::text AS reemits,
            count(*) FILTER (WHERE hours_to_reemit < 1)::text AS reemit_lt_1h,
            count(*) FILTER (WHERE hours_to_reemit < 6)::text AS reemit_lt_6h,
            count(*) FILTER (WHERE hours_to_reemit < 24)::text AS reemit_lt_24h
     FROM reemits
     GROUP BY 1
     ORDER BY reemit_lt_1h::int DESC, reemits::int DESC
     LIMIT 50`
  );

  const suppress = await client.query<{ reason: string; n: string }>(
    `SELECT coalesce(suppression_reason, '(none)') AS reason, count(*)::text AS n
     FROM platform.notification_decision_audit
     WHERE outcome = 'SUPPRESS'
       AND decided_at >= now() - ($1::text || ' days')::interval
     GROUP BY 1
     ORDER BY n::int DESC`,
    [String(days)]
  );

  const p = productivity.rows[0];
  return {
    windowDays: days,
    policyVersions: policies.rows.map((r) => r.policy_version),
    outcomeMix: outcomeMix.rows.map((r) => ({
      explanationCode: r.explanation_code,
      policyVersion: r.policy_version,
      outcome: r.outcome,
      count: parseInt(r.n, 10),
    })),
    volumeByCategory: volume.rows.map((r) => ({
      categoryBucket: r.cat_bucket,
      p50PerUserDay: num(r.p50),
      p90PerUserDay: num(r.p90),
      avgPerUserDay: num(r.avg),
    })),
    openRates: opens.rows.map((r) => ({
      code: r.code,
      delivered: parseInt(r.delivered, 10),
      opened: parseInt(r.opened, 10),
      openRatePct: num(r.open_rate_pct),
    })),
    productivity: {
      notificationsDelivered: parseInt(p?.notifications_delivered ?? '0', 10),
      notificationsRead: parseInt(p?.notifications_read ?? '0', 10),
      usefulActions: parseInt(p?.useful_actions ?? '0', 10),
      usefulActionsPerNotification: num(p?.useful_actions_per_notification),
      usefulActionsPerActiveUserDay: num(p?.useful_actions_per_active_user_day),
    },
    digestByPolicy: digest.rows.map((r) => ({
      policyVersion: r.policy_version,
      immediateDecisions: parseInt(r.immediate_decisions, 10),
      digestDecisions: parseInt(r.digest_decisions, 10),
      immediatePushes: parseInt(r.immediate_pushes, 10),
    })),
    hysteresis: hysteresis.rows.map((r) => ({
      code: r.code,
      reemits: parseInt(r.reemits, 10),
      reemitLt1h: parseInt(r.reemit_lt_1h, 10),
      reemitLt6h: parseInt(r.reemit_lt_6h, 10),
      reemitLt24h: parseInt(r.reemit_lt_24h, 10),
    })),
    suppressionReasons: suppress.rows.map((r) => ({
      reason: r.reason,
      count: parseInt(r.n, 10),
    })),
  };
}
