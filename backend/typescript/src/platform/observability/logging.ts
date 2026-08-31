import type { Request, Response, NextFunction } from 'express';
import { getCorrelationId, getMaestroRunId } from '../observability/correlation';
import { config } from '../config';

/** Structured request logging — never logs Authorization or tokens. */
export function requestLogMiddleware(req: Request, res: Response, next: NextFunction): void {
  const started = Date.now();
  const maestroRunId = getMaestroRunId(req);
  if (maestroRunId && config.logLevel !== 'silent') {
    console.log(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        level: 'info',
        event: 'request_received',
        correlationId: getCorrelationId(req),
        maestroRunId,
        route: req.path,
        method: req.method,
      })
    );
  }
  res.on('finish', () => {
    if (config.logLevel === 'silent') return;
    const line = {
      timestamp: new Date().toISOString(),
      level: res.statusCode >= 500 ? 'error' : res.statusCode >= 400 ? 'warn' : 'info',
      event: maestroRunId ? 'response_sent' : undefined,
      requestId: req.correlationId,
      correlationId: getCorrelationId(req),
      maestroRunId: maestroRunId || undefined,
      route: req.route?.path ?? req.path,
      method: req.method,
      status: res.statusCode,
      durationMs: Date.now() - started,
      canonicalUserId: req.requestContext?.userId,
    };
    console.log(JSON.stringify(line));
  });
  next();
}
