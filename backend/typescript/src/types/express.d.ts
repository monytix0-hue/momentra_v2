import type { RequestContext } from '../platform/request-context/context';

declare global {
  namespace Express {
    interface Request {
      correlationId?: string;
      requestContext?: RequestContext;
      idempotencyKey?: string;
    }
  }
}

export {};
