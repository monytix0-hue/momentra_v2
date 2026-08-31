import type { Request, Response, NextFunction } from 'express';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import {
  ensureUserProfile,
  resolveDevIdentity,
  resolveIdentityFromToken,
  verifyFirebaseToken,
} from '../../platform/auth';
import { config } from '../../platform/config';
import { getCorrelationId } from '../../platform/observability/correlation';

const DEV_UID_HEADER = 'x-dev-firebase-uid';

async function buildContext(
  req: Request,
  identity: { firebaseUid: string; firebaseProjectId: string; userId: string; email?: string; displayName?: string }
): Promise<RequestContext> {
  // Warm path: ensureUserProfile skips DB once the profile is known in-process.
  // First bootstrap / recovery still creates the row when missing.
  await ensureUserProfile(identity.userId, identity.email, identity.displayName);
  return Object.freeze({
    ...identity,
    correlationId: getCorrelationId(req),
    roles: [] as string[],
    permissions: [] as string[],
  });
}

function canUseDevAuth(): boolean {
  if (config.isProduction) return false;
  const allow =
    process.env.ALLOW_DEV_AUTH === '1' ||
    process.env.ALLOW_DEV_AUTH === 'true' ||
    config.allowDevAuth;
  return !config.firebase.projectId || allow;
}

/** Optional auth — attaches user context when Bearer token is valid; anonymous otherwise. */
export async function optionalAuthMiddleware(req: Request, _res: Response, next: NextFunction): Promise<void> {
  try {
    if (canUseDevAuth()) {
      const devUid = req.header(DEV_UID_HEADER)?.trim();
      if (devUid) {
        const devIdentity = resolveDevIdentity(devUid);
        req.requestContext = await buildContext(req, {
          ...devIdentity,
          email: `${devIdentity.userId}@dev.momentra.local`,
          displayName: 'Dev User',
        });
      }
      return next();
    }

    const header = req.header('authorization');
    if (!header?.startsWith('Bearer ')) {
      return next();
    }
    const decoded = await verifyFirebaseToken(header.slice(7));
    req.requestContext = await buildContext(req, resolveIdentityFromToken(decoded));
    next();
  } catch {
    next();
  }
}

/**
 * Firebase ID token verification (signature-checked via Admin SDK).
 * Dev bypass via X-Dev-Firebase-Uid only when not production and
 * FIREBASE_PROJECT_ID unset or ALLOW_DEV_AUTH=1.
 */
export async function authMiddleware(req: Request, _res: Response, next: NextFunction): Promise<void> {
  try {
    if (canUseDevAuth()) {
      const devUid = req.header(DEV_UID_HEADER)?.trim() || 'dev-local-user';
      const devIdentity = resolveDevIdentity(devUid);
      req.requestContext = await buildContext(req, {
        ...devIdentity,
        email: `${devIdentity.userId}@dev.momentra.local`,
        displayName: 'Dev User',
      });
      return next();
    }

    const header = req.header('authorization');
    if (!header?.startsWith('Bearer ')) {
      throw new AppError(ErrorCode.UNAUTHORIZED, 'Missing Bearer token.', 401);
    }
    const decoded = await verifyFirebaseToken(header.slice(7));
    req.requestContext = await buildContext(req, resolveIdentityFromToken(decoded));
    next();
  } catch (e) {
    if (e instanceof AppError) {
      next(e);
      return;
    }
    next(new AppError(ErrorCode.UNAUTHORIZED, 'Invalid or expired token.', 401));
  }
}

export function requireIdempotencyKey(req: Request, _res: Response, next: NextFunction): void {
  const key = req.header('idempotency-key');
  if (!key?.trim()) {
    next(new AppError(ErrorCode.VALIDATION_FAILED, 'Missing Idempotency-Key header.', 400));
    return;
  }
  if (key.trim().length < 8 || key.trim().length > 128) {
    next(new AppError(ErrorCode.VALIDATION_FAILED, 'Idempotency-Key length must be 8–128.', 400));
    return;
  }
  req.idempotencyKey = key.trim();
  next();
}
