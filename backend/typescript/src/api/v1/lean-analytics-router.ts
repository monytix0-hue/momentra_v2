import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import { commandEnvelope } from '../../platform/errors/errors';
import { optionalAuthMiddleware, authMiddleware } from '../middleware/auth';
import { emitLeanBusinessEvent } from '../../modules/analytics/lean-events';
import { refreshGroupLeanKpis } from '../../modules/analytics/group-lean-kpis';

export const leanAnalyticsRouter = Router();

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

const leanClientEventSchema = z
  .object({
    eventName: z.enum(['invite_opened', 'moment_viewed', 'session_started', 'screen_viewed', 'quick_add_started']),
    eventId: z.string().uuid().optional(),
    occurredAt: z.string().datetime().optional(),
    anonymousId: z.string().uuid().optional(),
    sessionId: z.string().uuid().optional(),
    momentId: z.string().uuid().optional(),
    momentDomain: z.enum(['personal', 'group', 'business']).optional(),
    momentCategory: z.string().max(100).optional(),
    momentType: z.string().max(100).optional(),
    sourceScreen: z.string().max(100).optional(),
    correlationId: z.string().uuid().optional(),
    platform: z.enum(['ios', 'android', 'web']).optional(),
    appVersion: z.string().max(30).optional(),
    properties: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

leanAnalyticsRouter.post('/events', optionalAuthMiddleware, async (req, res, next) => {
  try {
    const parsed = leanClientEventSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ code: 'VALIDATION_FAILED', message: parsed.error.message });
      return;
    }
    const body = parsed.data;
    if (body.eventName === 'invite_opened') {
      const inviteId = body.properties?.invite_id;
      if (typeof inviteId !== 'string' || !inviteId) {
        res.status(400).json({ code: 'VALIDATION_FAILED', message: 'invite_opened requires properties.invite_id' });
        return;
      }
    }
    await withDb(async (client) => {
      await client.query('BEGIN');
      try {
        await emitLeanBusinessEvent(client, req.requestContext ?? null, {
          eventName: body.eventName,
          eventId: body.eventId,
          occurredAt: body.occurredAt,
          anonymousId: body.anonymousId,
          sessionId: body.sessionId,
          momentId: body.momentId,
          momentDomain: body.momentDomain,
          momentCategory: body.momentCategory,
          momentType: body.momentType,
          sourceScreen: body.sourceScreen,
          correlationId: body.correlationId,
          platform: body.platform,
          appVersion: body.appVersion,
          properties: body.properties,
          ingestionSource: 'client',
        });
        await client.query('COMMIT');
      } catch (e) {
        await client.query('ROLLBACK');
        throw e;
      }
    });
    res.status(202).json(
      commandEnvelope({ accepted: true }, req.correlationId ?? req.requestContext?.correlationId ?? 'lean')
    );
  } catch (e) {
    next(e);
  }
});

leanAnalyticsRouter.post('/group-kpis/refresh', authMiddleware, async (req, res, next) => {
  try {
    const rows = await withDb((client) => refreshGroupLeanKpis(client));
    res.json(commandEnvelope({ kpis: rows }, req.requestContext!.correlationId));
  } catch (e) {
    next(e);
  }
});

leanAnalyticsRouter.get('/group-kpis', authMiddleware, async (req, res, next) => {
  try {
    const data = await withDb(async (client) => {
      const r = await client.query<{
        kpi_code: string;
        numerator: string | null;
        denominator: string | null;
        kpi_value: string | null;
        sample_size: string | null;
        period_start: string;
        calculated_at: string;
      }>(
        `SELECT DISTINCT ON (kpi_code)
           kpi_code, numerator::text, denominator::text, kpi_value::text, sample_size::text,
           period_start::text, calculated_at::text
         FROM analytics_mart.kpi_period
         WHERE moment_domain = 'group'
           AND kpi_code IN (
             'KPI_020_AVERAGE_PARTICIPANTS_PER_GROUP_MOMENT',
             'KPI_030_INVITATIONS_PER_GROUP_MOMENT',
             'KPI_031_INVITE_OPEN_RATE',
             'KPI_032_INVITE__JOIN_CONVERSION',
             'KPI_033_INVITED_USER_ACTIVATION_RATE',
             'KPI_034_PARTICIPANT__CREATOR_CONVERSION',
             'KPI_035_VIRAL_COEFFICIENT'
           )
         ORDER BY kpi_code, period_start DESC, calculated_at DESC`
      );
      return r.rows.map((row) => ({
        kpiCode: row.kpi_code,
        numerator: row.numerator != null ? Number(row.numerator) : null,
        denominator: row.denominator != null ? Number(row.denominator) : null,
        kpiValue: row.kpi_value != null ? Number(row.kpi_value) : null,
        sampleSize: row.sample_size != null ? Number(row.sample_size) : null,
        periodStart: row.period_start,
        calculatedAt: row.calculated_at,
      }));
    });
    res.json(commandEnvelope({ kpis: data }, req.requestContext!.correlationId));
  } catch (e) {
    next(e);
  }
});
