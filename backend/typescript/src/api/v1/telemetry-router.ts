import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import { commandEnvelope } from '../../platform/errors/errors';
import { optionalAuthMiddleware } from '../middleware/auth';
import * as telemetryService from '../../modules/telemetry/service';

export const telemetryRouter = Router();
telemetryRouter.use(optionalAuthMiddleware);

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

telemetryRouter.post('/events', async (req, res, next) => {
  try {
    const parsed = telemetryService.ingestTelemetrySchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ code: 'VALIDATION_FAILED', message: parsed.error.message });
      return;
    }
    const ctx = req.requestContext ?? null;
    const data = await withDb((client) => telemetryService.ingestTelemetry(client, ctx, parsed.data));
    res.status(202).json(
      commandEnvelope(data, req.correlationId ?? 'anonymous-telemetry')
    );
  } catch (e) {
    next(e);
  }
});

telemetryRouter.get('/events', async (req, res, next) => {
  try {
    const ctx = req.requestContext;
    if (!ctx?.userId) {
      res.status(401).json({ code: 'UNAUTHORIZED', message: 'Sign in to query telemetry.' });
      return;
    }
    const limit = z.coerce.number().int().min(1).max(100).catch(50).parse(req.query.limit);
    const data = await withDb((client) => telemetryService.listRecentTelemetry(client, ctx.userId, limit));
    res.json(commandEnvelope(data, ctx.correlationId));
  } catch (e) {
    next(e);
  }
});
