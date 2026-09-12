import { Router } from 'express';
import { z } from 'zod';
import { getPool } from '../../platform/database/pool';
import { getNotificationSoakOverview } from '../../modules/notifications/soak-admin';

export const adminNotificationsRouter = Router();

async function withDb<T>(fn: (client: import('pg').PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

/** GET /admin/api/notifications/soak?windowDays=14 */
adminNotificationsRouter.get('/soak', async (req, res, next) => {
  try {
    const windowDays = z.coerce.number().int().min(1).max(90).catch(14).parse(req.query.windowDays);
    const data = await withDb((client) => getNotificationSoakOverview(client, windowDays));
    res.json({ data });
  } catch (e) {
    next(e);
  }
});
