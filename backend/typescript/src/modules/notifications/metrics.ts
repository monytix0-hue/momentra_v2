import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';

export type DeliveryMetrics = {
  today: {
    attempted: number;
    sent: number;
    failed: number;
    revokedTokens: number;
    digestBatched: number;
    inbox: number;
  };
  byEvent: Array<{
    eventName: string;
    attempted: number;
    sent: number;
    failed: number;
  }>;
  inboxUnread: number;
};

export async function getDeliveryMetrics(
  client: PoolClient,
  ctx: RequestContext
): Promise<DeliveryMetrics> {
  const today = await client.query<{
    attempted_count: string;
    sent_count: string;
    failed_count: string;
    revoked_token_count: string;
    digest_batched_count: string;
    inbox_count: string;
  }>(
    `SELECT attempted_count::text, sent_count::text, failed_count::text,
            revoked_token_count::text, digest_batched_count::text, inbox_count::text
     FROM platform.notification_delivery_stats
     WHERE stat_day = CURRENT_DATE AND event_name = '*'`,
    []
  );
  const byEvent = await client.query<{
    event_name: string;
    attempted_count: string;
    sent_count: string;
    failed_count: string;
  }>(
    `SELECT event_name, attempted_count::text, sent_count::text, failed_count::text
     FROM platform.notification_delivery_stats
     WHERE stat_day = CURRENT_DATE AND event_name <> '*'
     ORDER BY sent_count DESC
     LIMIT 25`
  );
  const unread = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM platform.user_notification
     WHERE user_id = $1 AND read_at IS NULL`,
    [ctx.userId]
  );
  const t = today.rows[0];
  return {
    today: {
      attempted: parseInt(t?.attempted_count ?? '0', 10),
      sent: parseInt(t?.sent_count ?? '0', 10),
      failed: parseInt(t?.failed_count ?? '0', 10),
      revokedTokens: parseInt(t?.revoked_token_count ?? '0', 10),
      digestBatched: parseInt(t?.digest_batched_count ?? '0', 10),
      inbox: parseInt(t?.inbox_count ?? '0', 10),
    },
    byEvent: byEvent.rows.map((r) => ({
      eventName: r.event_name,
      attempted: parseInt(r.attempted_count, 10),
      sent: parseInt(r.sent_count, 10),
      failed: parseInt(r.failed_count, 10),
    })),
    inboxUnread: parseInt(unread.rows[0]?.n ?? '0', 10),
  };
}
