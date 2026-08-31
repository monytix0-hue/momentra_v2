import type { Request, Response, NextFunction } from 'express';
import { getCorrelationId } from '../observability/correlation';
import type { ProjectionHint } from '../projections/hints';

export enum ErrorCode {
  VALIDATION_FAILED = 'VALIDATION_FAILED',
  GOVERNANCE_DENIED = 'GOVERNANCE_DENIED',
  RESOURCE_NOT_FOUND = 'RESOURCE_NOT_FOUND',
  VERSION_CONFLICT = 'VERSION_CONFLICT',
  IDEMPOTENCY_CONFLICT = 'IDEMPOTENCY_CONFLICT',
  INVOICE_NUMBER_CONFLICT = 'INVOICE_NUMBER_CONFLICT',
  APPROVAL_REQUIRED = 'APPROVAL_REQUIRED',
  CONSENT_REQUIRED = 'CONSENT_REQUIRED',
  SETTLEMENT_EXCEEDS_OUTSTANDING = 'SETTLEMENT_EXCEEDS_OUTSTANDING',
  INFRASTRUCTURE_UNAVAILABLE = 'INFRASTRUCTURE_UNAVAILABLE',
  UNAUTHORIZED = 'UNAUTHORIZED',
  RATE_LIMITED = 'RATE_LIMITED',
}

/**
 * Phase 2 OpenAPI error codes are authoritative.
 * Phase 3 prompt aliases map as follows (do not invent parallel codes):
 * VALIDATION_ERROR → VALIDATION_FAILED
 * UNAUTHENTICATED → UNAUTHORIZED
 * FORBIDDEN → GOVERNANCE_DENIED
 * NOT_FOUND → RESOURCE_NOT_FOUND
 * BUSINESS_RULE_VIOLATION → GOVERNANCE_DENIED / domain-specific codes
 * INTERNAL_ERROR → INFRASTRUCTURE_UNAVAILABLE
 */

export class AppError extends Error {
  constructor(
    public readonly code: ErrorCode,
    message: string,
    public readonly httpStatus: number = 400,
    public readonly details?: unknown
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export interface CommandEnvelope<T> {
  data: T;
  resourceVersion?: number;
  correlationId: string;
  projectionHints?: ProjectionHint[];
}

export interface ProjectionEnvelope<T> {
  data: T;
  projectionVersion?: number;
  updatedAt?: string;
  status?: string;
  nextCursor?: string | null;
  correlationId: string;
}

export interface ErrorEnvelope {
  code: string;
  message: string;
  correlationId: string;
  details?: unknown;
}

export function commandEnvelope<T>(
  data: T,
  correlationId: string,
  opts?: { resourceVersion?: number; projectionHints?: ProjectionHint[] }
): CommandEnvelope<T> {
  return {
    data,
    correlationId,
    resourceVersion: opts?.resourceVersion,
    projectionHints: opts?.projectionHints,
  };
}

export function projectionEnvelope<T>(
  data: T,
  correlationId: string,
  opts?: { projectionVersion?: number; updatedAt?: string; status?: string; nextCursor?: string | null }
): ProjectionEnvelope<T> {
  return {
    data,
    correlationId,
    projectionVersion: opts?.projectionVersion,
    updatedAt: opts?.updatedAt,
    status: opts?.status,
    nextCursor: opts?.nextCursor,
  };
}

export function errorEnvelope(
  code: string,
  message: string,
  correlationId: string,
  details?: unknown
): ErrorEnvelope {
  return { code, message, correlationId, details };
}

export function errorHandler(err: unknown, req: Request, res: Response, _next: NextFunction): void {
  const correlationId = getCorrelationId(req);

  if (err instanceof AppError) {
    if (err.code === ErrorCode.RATE_LIMITED) {
      const details = err.details as { retryAfterSec?: number } | undefined;
      if (details?.retryAfterSec && !res.getHeader('Retry-After')) {
        res.setHeader('Retry-After', String(details.retryAfterSec));
      }
    }
    res.status(err.httpStatus).json(errorEnvelope(err.code, err.message, correlationId, err.details));
    return;
  }

  console.error(
    JSON.stringify({
      level: 'error',
      correlationId,
      route: req.path,
      method: req.method,
      errorCode: ErrorCode.INFRASTRUCTURE_UNAVAILABLE,
      err: String(err),
    })
  );
  res
    .status(500)
    .json(errorEnvelope(ErrorCode.INFRASTRUCTURE_UNAVAILABLE, 'An unexpected error occurred.', correlationId));
}
