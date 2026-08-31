import { randomUUID } from 'crypto';
import type { Request, Response, NextFunction } from 'express';

export const CORRELATION_HEADER = 'x-correlation-id';
/** Maestro / cert run token — never required in production. */
export const MAESTRO_RUN_HEADER = 'x-maestro-run-id';

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/** Human-readable QA tokens, e.g. qa-20260827-personal-lifeops-expense-001 */
const QA_TOKEN_RE = /^qa-[a-z0-9-]{8,64}$/i;

function allowQaCorrelationTokens(): boolean {
  return (process.env.NODE_ENV || 'development').toLowerCase() !== 'production';
}

export function normalizeCorrelationId(incoming: string | undefined): string {
  const trimmed = incoming?.trim();
  if (!trimmed || trimmed.length > 80) {
    return randomUUID();
  }
  if (UUID_RE.test(trimmed)) {
    return trimmed;
  }
  // Non-production only: accept Maestro qa-* correlation tokens for cert tracing.
  if (allowQaCorrelationTokens() && QA_TOKEN_RE.test(trimmed)) {
    return trimmed.toLowerCase();
  }
  return randomUUID();
}

export function getCorrelationId(req: Request): string {
  return req.correlationId ?? randomUUID();
}

export function getMaestroRunId(req: Request): string | undefined {
  const raw = req.header(MAESTRO_RUN_HEADER)?.trim();
  if (!raw || raw.length > 64) return undefined;
  return raw;
}

export function correlationMiddleware(req: Request, res: Response, next: NextFunction): void {
  const correlationId = normalizeCorrelationId(req.header(CORRELATION_HEADER));
  req.correlationId = correlationId;
  const maestroRunId = getMaestroRunId(req);
  if (maestroRunId) {
    (req as Request & { maestroRunId?: string }).maestroRunId = maestroRunId;
  }
  res.setHeader(CORRELATION_HEADER, correlationId);
  if (maestroRunId) {
    res.setHeader(MAESTRO_RUN_HEADER, maestroRunId);
  }
  next();
}
