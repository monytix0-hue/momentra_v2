import express from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { config } from './platform/config';
import { correlationMiddleware } from './api/middleware/correlation';
import { requestLogMiddleware } from './platform/observability/logging';
import { errorHandler } from './platform/errors/errors';
import { checkDatabaseReady, closePool, prewarmPool } from './platform/database/pool';
import { v1Router } from './api/v1/router';
import { adminRouter } from './api/admin/router';
import { attachSseRoutes } from './realtime/sse';

export function createApp(): express.Express {
  const app = express();
  const corsOrigins = config.isProduction
    ? config.corsOrigins
    : [...new Set([...config.corsOrigins, ...config.admin.corsOrigins])];

  app.disable('x-powered-by');
  app.use(
    cors({
      origin: config.isProduction && corsOrigins.includes('*') ? false : corsOrigins,
      credentials: true,
    })
  );
  app.use(express.json({ limit: '1mb' }));
  app.use(correlationMiddleware);
  app.use(requestLogMiddleware);

  // Liveness: process up only — no external dependency checks.
  app.get('/health/live', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // Readiness: PostgreSQL required. AI/Redis/workers must not gate core commands.
  app.get('/health/ready', async (_req, res) => {
    const dbOk = await checkDatabaseReady();
    if (!dbOk) {
      res.status(503).json({ status: 'degraded' });
      return;
    }
    res.json({ status: 'ok' });
  });

  app.use('/admin/api', adminRouter);
  app.use('/v1', v1Router);
  attachSseRoutes(app);

  app.use(errorHandler);
  return app;
}

export function startServer(): ReturnType<typeof createServer> {
  const app = createApp();
  const server = createServer(app);

  server.listen(config.port, '0.0.0.0', () => {
    console.log(
      JSON.stringify({
        level: 'info',
        msg: 'momentra-api listening',
        host: '0.0.0.0',
        port: config.port,
        nodeEnv: config.nodeEnv,
        schemaRelease: config.schemaRelease,
        dbPoolMax: config.database.poolMax,
        dbPoolMin: config.database.poolMin,
      })
    );
    // S9-C: pay TLS/pooler handshake at boot (modest min), not on first /v1/me.
    void prewarmPool(config.database.poolMin).catch((err) => {
      console.log(
        JSON.stringify({
          level: 'warn',
          msg: 'pg_pool_prewarm_failed',
          err: String(err),
        })
      );
    });
  });

  const shutdown = async () => {
    console.log(JSON.stringify({ level: 'info', msg: 'graceful shutdown' }));
    server.close();
    await closePool();
    process.exit(0);
  };
  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);

  return server;
}
