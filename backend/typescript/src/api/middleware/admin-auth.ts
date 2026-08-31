import type { Request, Response, NextFunction } from 'express';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { config } from '../../platform/config';

const ADMIN_KEY_HEADER = 'x-admin-key';

/** Protects /admin/api routes — separate from mobile Firebase auth. */
export function adminAuthMiddleware(req: Request, _res: Response, next: NextFunction): void {
  if (!config.admin.apiKey) {
    next(new AppError(ErrorCode.UNAUTHORIZED, 'Admin API is not configured (set ADMIN_API_KEY).', 503));
    return;
  }
  const key = req.header(ADMIN_KEY_HEADER)?.trim();
  if (!key || key !== config.admin.apiKey) {
    next(new AppError(ErrorCode.UNAUTHORIZED, 'Invalid admin key.', 401));
    return;
  }
  next();
}
