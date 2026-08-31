import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';
import { getMeBootstrap } from './bootstrap';

export { getMeBootstrap } from './bootstrap';
export type { MeBootstrap } from './bootstrap';

export const registerDeviceSchema = z
  .object({
    deviceId: z.string().min(1).max(200).optional(),
    platform: z.enum(['ANDROID', 'IOS', 'WEB']),
    pushToken: z.string().min(1).max(512).optional().nullable(),
    appVersion: z.string().max(50).optional(),
  })
  .strict();

export type RegisterDeviceInput = z.infer<typeof registerDeviceSchema>;

/**
 * @deprecated Prefer getMeBootstrap — kept for narrow identity checks in tests.
 */
export async function getMe(client: PoolClient, ctx: RequestContext): Promise<{
  userId: string;
  email?: string;
  displayName?: string;
  firebaseUid: string;
  roles: string[];
}> {
  const boot = await getMeBootstrap(client, ctx);
  return {
    userId: boot.userId,
    email: boot.email ?? undefined,
    displayName: boot.displayName ?? undefined,
    firebaseUid: boot.firebaseUid,
    roles: boot.roles,
  };
}

/**
 * Phase 3 transactional proof command.
 * Low-risk: registers push device for the authenticated user only.
 * Writes canonical row + audit + domain event + outbox in one transaction
 * when invoked via runCommand.
 */
export async function registerDevice(
  client: PoolClient,
  ctx: RequestContext,
  body: RegisterDeviceInput
): Promise<{ deviceId: string; userDeviceId: string; userId: string; platform: string; status: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'DEVICE_REGISTER',
    resourceType: 'DEVICE',
    ownerUserId: ctx.userId,
  });

  const deviceId = body.deviceId ?? randomUUID();
  const inserted = await client.query<{ user_device_id: string }>(
    `INSERT INTO platform.user_device (user_id, device_id, platform, push_token, app_version, last_seen_at)
     VALUES ($1, $2, $3, $4, $5, now())
     ON CONFLICT (user_id, device_id) DO UPDATE SET
       platform = EXCLUDED.platform,
       push_token = EXCLUDED.push_token,
       app_version = EXCLUDED.app_version,
       last_seen_at = now(),
       revoked_at = NULL,
       updated_at = now()
     RETURNING user_device_id`,
    [ctx.userId, deviceId, body.platform, body.pushToken ?? '', body.appVersion ?? null]
  );

  const userDeviceId = inserted.rows[0].user_device_id;
  const result = {
    deviceId,
    userDeviceId,
    userId: ctx.userId,
    platform: body.platform,
    status: 'ACTIVE' as const,
  };

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'DeviceRegistered',
    domainCode: 'PLATFORM',
    aggregateType: 'DEVICE',
    aggregateId: userDeviceId,
    scopeType: 'USER',
    scopeId: ctx.userId,
    payload: {
      deviceId,
      platform: body.platform,
      // pushToken intentionally omitted from event payload
    },
  });

  await insertAudit(client, ctx, 'DEVICE_REGISTER', 'DEVICE', userDeviceId, domainEventId, {
    deviceId,
    platform: body.platform,
    status: 'ACTIVE',
  });

  return result;
}

export async function revokeDevice(
  client: PoolClient,
  ctx: RequestContext,
  deviceId: string
): Promise<{ deviceId: string; userId: string; status: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'DEVICE_REVOKE',
    resourceType: 'DEVICE',
    ownerUserId: ctx.userId,
  });

  const updated = await client.query(
    `UPDATE platform.user_device SET revoked_at = now(), updated_at = now()
     WHERE user_id = $1 AND device_id = $2 AND revoked_at IS NULL`,
    [ctx.userId, deviceId]
  );
  if (!updated.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Device not found.', 404);
  }
  return { deviceId, userId: ctx.userId, status: 'REVOKED' };
}

export async function listDevices(
  client: PoolClient,
  ctx: RequestContext,
): Promise<{
  items: Array<{
    deviceId: string;
    userDeviceId: string;
    platform: string;
    appVersion: string | null;
    lastSeenAt: string | null;
    revoked: boolean;
  }>;
}> {
  const rows = await client.query<{
    device_id: string;
    user_device_id: string;
    platform: string;
    app_version: string | null;
    last_seen_at: Date | null;
    revoked_at: Date | null;
  }>(
    `SELECT device_id, user_device_id, platform, app_version, last_seen_at, revoked_at
     FROM platform.user_device
     WHERE user_id = $1
     ORDER BY COALESCE(last_seen_at, created_at) DESC`,
    [ctx.userId],
  );
  return {
    items: rows.rows.map((r) => ({
      deviceId: r.device_id,
      userDeviceId: r.user_device_id,
      platform: r.platform,
      appVersion: r.app_version,
      lastSeenAt: r.last_seen_at ? r.last_seen_at.toISOString() : null,
      revoked: r.revoked_at != null,
    })),
  };
}
