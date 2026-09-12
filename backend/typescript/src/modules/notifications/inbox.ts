import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { threadKeyFor } from '../../platform/notifications/thread-key';

export const listInboxQuerySchema = z
  .object({
    limit: z.coerce.number().int().min(1).max(100).optional(),
    unreadOnly: z
      .union([z.boolean(), z.enum(['true', 'false'])])
      .optional()
      .transform((v) => (v === true || v === 'true' ? true : v === false || v === 'false' ? false : undefined)),
    cursor: z.string().datetime().optional(),
  })
  .strict();

export type ListInboxQuery = z.infer<typeof listInboxQuerySchema>;

export type InboxItem = {
  notificationId: string;
  eventName: string;
  categoryCode: string;
  priorityCode: string;
  title: string;
  body: string;
  momentId: string | null;
  momentTitle: string | null;
  threadKey: string;
  deepLink: string | null;
  actorDisplayName: string | null;
  readAt: string | null;
  createdAt: string;
};

export async function listInbox(
  client: PoolClient,
  ctx: RequestContext,
  query: ListInboxQuery
): Promise<{ items: InboxItem[]; unreadCount: number }> {
  const limit = query.limit ?? 30;
  const params: unknown[] = [ctx.userId];
  let where = `n.user_id = $1`;
  if (query.unreadOnly) {
    where += ` AND n.read_at IS NULL`;
  }
  if (query.cursor) {
    params.push(query.cursor);
    where += ` AND n.created_at < $${params.length}::timestamptz`;
  }
  params.push(limit);
  const rows = await client.query<{
    user_notification_id: string;
    event_name: string;
    category_code: string;
    priority_code: string;
    title: string;
    body: string;
    moment_id: string | null;
    moment_title: string | null;
    domain_code: string | null;
    company_id: string | null;
    deep_link: string | null;
    actor_display_name: string | null;
    read_at: Date | null;
    created_at: Date;
  }>(
    `SELECT n.user_notification_id, n.event_name, n.category_code, n.priority_code,
            n.title, n.body, n.moment_id, m.title AS moment_title, m.domain_code,
            bmc.company_id,
            n.deep_link, n.actor_display_name, n.read_at, n.created_at
     FROM platform.user_notification n
     LEFT JOIN core.moment m ON m.moment_id = n.moment_id
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = n.moment_id
     WHERE ${where}
     ORDER BY n.created_at DESC
     LIMIT $${params.length}`,
    params
  );
  const unread = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM platform.user_notification
     WHERE user_id = $1 AND read_at IS NULL`,
    [ctx.userId]
  );
  return {
    items: rows.rows.map((r) => ({
      notificationId: r.user_notification_id,
      eventName: r.event_name,
      categoryCode: r.category_code,
      priorityCode: r.priority_code,
      title: r.title,
      body: r.body,
      momentId: r.moment_id,
      momentTitle: r.moment_title,
      threadKey: threadKeyFor({
        domainCode: r.domain_code,
        momentId: r.moment_id,
        companyId: r.company_id,
        eventName: r.event_name,
      }),
      deepLink: r.deep_link,
      actorDisplayName: r.actor_display_name,
      readAt: r.read_at?.toISOString() ?? null,
      createdAt: r.created_at.toISOString(),
    })),
    unreadCount: parseInt(unread.rows[0]?.n ?? '0', 10),
  };
}

export const markReadSchema = z
  .object({
    notificationIds: z.array(z.string().uuid()).min(1).max(100).optional(),
    all: z.boolean().optional(),
  })
  .strict()
  .refine((b) => b.all === true || (b.notificationIds && b.notificationIds.length > 0), {
    message: 'Provide notificationIds or all=true.',
  });

export type MarkReadInput = z.infer<typeof markReadSchema>;

export async function markInboxRead(
  client: PoolClient,
  ctx: RequestContext,
  body: MarkReadInput
): Promise<{ updatedCount: number }> {
  if (body.all) {
    const r = await client.query(
      `UPDATE platform.user_notification
       SET read_at = now()
       WHERE user_id = $1 AND read_at IS NULL`,
      [ctx.userId]
    );
    return { updatedCount: r.rowCount ?? 0 };
  }
  const ids = body.notificationIds ?? [];
  const r = await client.query(
    `UPDATE platform.user_notification
     SET read_at = now()
     WHERE user_id = $1 AND user_notification_id = ANY($2::uuid[]) AND read_at IS NULL`,
    [ctx.userId, ids]
  );
  return { updatedCount: r.rowCount ?? 0 };
}

export async function getInboxItem(
  client: PoolClient,
  ctx: RequestContext,
  notificationId: string
): Promise<InboxItem> {
  const r = await client.query<{
    user_notification_id: string;
    event_name: string;
    category_code: string;
    priority_code: string;
    title: string;
    body: string;
    moment_id: string | null;
    moment_title: string | null;
    domain_code: string | null;
    company_id: string | null;
    deep_link: string | null;
    actor_display_name: string | null;
    read_at: Date | null;
    created_at: Date;
  }>(
    `SELECT n.user_notification_id, n.event_name, n.category_code, n.priority_code,
            n.title, n.body, n.moment_id, m.title AS moment_title, m.domain_code,
            bmc.company_id,
            n.deep_link, n.actor_display_name, n.read_at, n.created_at
     FROM platform.user_notification n
     LEFT JOIN core.moment m ON m.moment_id = n.moment_id
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = n.moment_id
     WHERE n.user_id = $1 AND n.user_notification_id = $2`,
    [ctx.userId, notificationId]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Notification not found.', 404);
  }
  const row = r.rows[0]!;
  return {
    notificationId: row.user_notification_id,
    eventName: row.event_name,
    categoryCode: row.category_code,
    priorityCode: row.priority_code,
    title: row.title,
    body: row.body,
    momentId: row.moment_id,
    momentTitle: row.moment_title,
    threadKey: threadKeyFor({
      domainCode: row.domain_code,
      momentId: row.moment_id,
      companyId: row.company_id,
      eventName: row.event_name,
    }),
    deepLink: row.deep_link,
    actorDisplayName: row.actor_display_name,
    readAt: row.read_at?.toISOString() ?? null,
    createdAt: row.created_at.toISOString(),
  };
}
