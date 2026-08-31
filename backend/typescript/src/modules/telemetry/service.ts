import { z } from 'zod';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';

const userSnapshotSchema = z
  .object({
    userName: z.string().max(200).optional(),
    userEmail: z.string().max(200).optional(),
    userPhone: z.string().max(50).optional(),
    userAge: z.string().max(20).optional(),
    userSex: z.string().max(20).optional(),
    hasPhoto: z.boolean().optional(),
    photoUrl: z.string().max(500).optional(),
    authProviders: z.string().max(200).optional(),
  })
  .strict()
  .optional();

/** Accepts Z or numeric-offset ISO strings; Zod .datetime() rejects +00:00 from some clients. */
const clientDatetime = z.string().refine((s) => !Number.isNaN(Date.parse(s)), {
  message: 'Invalid client datetime',
});

export const ingestTelemetrySchema = z
  .object({
    sessionId: z.string().uuid(),
    anonymousId: z.string().uuid(),
    platform: z.enum(['android', 'ios', 'web']),
    appVersion: z.string().max(50).optional(),
    deviceModel: z.string().max(120).optional(),
    sessionStartedAt: clientDatetime.optional(),
    sessionEndedAt: clientDatetime.optional(),
    userSnapshot: userSnapshotSchema,
    events: z
      .array(
        z
          .object({
            eventName: z.string().min(1).max(64),
            screenName: z.string().max(120).optional(),
            widgetName: z.string().max(120).optional(),
            clientOccurredAt: clientDatetime,
            properties: z.record(z.string(), z.union([z.string(), z.number(), z.boolean()])).optional(),
          })
          .strict()
      )
      .min(1)
      .max(200),
  })
  .strict();

export type IngestTelemetryInput = z.infer<typeof ingestTelemetrySchema>;

export async function ingestTelemetry(
  client: PoolClient,
  ctx: RequestContext | null,
  input: IngestTelemetryInput
): Promise<{ accepted: number; sessionId: string }> {
  const userId = ctx?.userId ?? null;
  const snapshot = input.userSnapshot ?? {};

  await client.query(
    `INSERT INTO analytics.client_session (
       client_session_id, anonymous_id, user_id, platform, app_version, device_model,
       started_at, ended_at, user_snapshot, updated_at
     ) VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7::timestamptz, now()), $8::timestamptz, $9::jsonb, now())
     ON CONFLICT (client_session_id) DO UPDATE SET
       user_id = COALESCE(EXCLUDED.user_id, analytics.client_session.user_id),
       ended_at = COALESCE(EXCLUDED.ended_at, analytics.client_session.ended_at),
       user_snapshot = CASE
         WHEN EXCLUDED.user_snapshot <> '{}'::jsonb THEN EXCLUDED.user_snapshot
         ELSE analytics.client_session.user_snapshot
       END,
       updated_at = now()`,
    [
      input.sessionId,
      input.anonymousId,
      userId,
      input.platform,
      input.appVersion ?? null,
      input.deviceModel ?? null,
      input.sessionStartedAt ?? null,
      input.sessionEndedAt ?? null,
      JSON.stringify(snapshot),
    ]
  );

  if (input.events.length > 0) {
    const rows = input.events.map((ev) => ({
      event_name: ev.eventName,
      screen_name: ev.screenName ?? null,
      widget_name: ev.widgetName ?? null,
      properties: ev.properties ?? {},
      client_occurred_at: ev.clientOccurredAt,
    }));
    await client.query(
      `INSERT INTO analytics.client_event (
         client_session_id, anonymous_id, user_id, event_name, screen_name, widget_name,
         properties, client_occurred_at
       )
       SELECT $1, $2, $3, r.event_name, r.screen_name, r.widget_name, r.properties, r.client_occurred_at::timestamptz
       FROM jsonb_to_recordset($4::jsonb) AS r(
         event_name text,
         screen_name text,
         widget_name text,
         properties jsonb,
         client_occurred_at text
       )`,
      [input.sessionId, input.anonymousId, userId, JSON.stringify(rows)]
    );
  }

  return { accepted: input.events.length, sessionId: input.sessionId };
}

export async function listRecentTelemetry(
  client: PoolClient,
  userId: string,
  limit: number
): Promise<{ items: Array<Record<string, unknown>> }> {
  const capped = Math.min(Math.max(limit, 1), 100);
  const result = await client.query(
    `SELECT
       ce.client_event_id,
       ce.event_name,
       ce.screen_name,
       ce.widget_name,
       ce.properties,
       ce.client_occurred_at,
       cs.platform,
       cs.user_snapshot
     FROM analytics.client_event ce
     JOIN analytics.client_session cs ON cs.client_session_id = ce.client_session_id
     WHERE ce.user_id = $1
     ORDER BY ce.client_occurred_at DESC
     LIMIT $2`,
    [userId, capped]
  );
  return { items: result.rows };
}
