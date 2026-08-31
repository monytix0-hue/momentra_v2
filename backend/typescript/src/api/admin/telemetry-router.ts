import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import * as adminTelemetry from '../../modules/telemetry/admin-service';

export const adminTelemetryRouter = Router();

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

adminTelemetryRouter.get('/overview', async (_req, res, next) => {
  try {
    const data = await withDb((client) => adminTelemetry.getTelemetryOverview(client));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/sessions', async (req, res, next) => {
  try {
    const limit = z.coerce.number().int().min(1).max(200).catch(50).parse(req.query.limit);
    const platform = z.enum(['android', 'ios', 'web']).optional().catch(undefined).parse(req.query.platform);
    const data = await withDb((client) => adminTelemetry.listTelemetrySessions(client, { limit, platform }));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/events', async (req, res, next) => {
  try {
    const limit = z.coerce.number().int().min(1).max(500).catch(100).parse(req.query.limit);
    const screenName = z.string().optional().parse(req.query.screenName ?? req.query.screen_name);
    const eventName = z.string().optional().parse(req.query.eventName ?? req.query.event_name);
    const userId = z.string().uuid().optional().parse(req.query.userId ?? req.query.user_id);
    const data = await withDb((client) =>
      adminTelemetry.listTelemetryEvents(client, { limit, screenName, eventName, userId })
    );
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/screen-time', async (_req, res, next) => {
  try {
    const data = await withDb((client) => adminTelemetry.getScreenTimeReport(client));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/stuck-points', async (_req, res, next) => {
  try {
    const data = await withDb((client) => adminTelemetry.getStuckPointsReport(client));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/users', async (req, res, next) => {
  try {
    const limit = z.coerce.number().int().min(1).max(200).catch(50).parse(req.query.limit);
    const data = await withDb((client) => adminTelemetry.listTelemetryUsers(client, limit));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/widgets', async (req, res, next) => {
  try {
    const limit = z.coerce.number().int().min(1).max(100).catch(30).parse(req.query.limit);
    const data = await withDb((client) => adminTelemetry.getWidgetInteractions(client, limit));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/personal-setups', async (_req, res, next) => {
  try {
    const data = await withDb((client) => adminTelemetry.getPersonalSetupReport(client));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});

adminTelemetryRouter.get('/business-setups', async (_req, res, next) => {
  try {
    const data = await withDb((client) => adminTelemetry.getBusinessSetupReport(client));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});
