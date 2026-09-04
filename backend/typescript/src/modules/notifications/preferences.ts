import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { assertGroupMember } from '../collaboration/group-membership';

export const patchGlobalNotificationPrefsSchema = z
  .object({
    pushNotificationsEnabled: z.boolean(),
  })
  .strict();

export type PatchGlobalNotificationPrefsInput = z.infer<typeof patchGlobalNotificationPrefsSchema>;

export const patchMomentNotificationPrefsSchema = z
  .object({
    notifyOnChanges: z.boolean(),
  })
  .strict();

export type PatchMomentNotificationPrefsInput = z.infer<typeof patchMomentNotificationPrefsSchema>;

export async function getGlobalNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext
): Promise<{ pushNotificationsEnabled: boolean }> {
  const r = await client.query<{ push_notifications_enabled: boolean }>(
    `SELECT push_notifications_enabled FROM core.user_profile WHERE user_id = $1`,
    [ctx.userId]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found.', 404);
  }
  return { pushNotificationsEnabled: r.rows[0]!.push_notifications_enabled };
}

export async function patchGlobalNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  body: PatchGlobalNotificationPrefsInput
): Promise<{ pushNotificationsEnabled: boolean }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PROFILE_UPDATE',
    resourceType: 'USER',
    ownerUserId: ctx.userId,
  });
  const r = await client.query<{ push_notifications_enabled: boolean }>(
    `UPDATE core.user_profile
     SET push_notifications_enabled = $2, updated_at = now(), version = version + 1
     WHERE user_id = $1 AND status = 'ACTIVE'
     RETURNING push_notifications_enabled`,
    [ctx.userId, body.pushNotificationsEnabled]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found or not active.', 404);
  }
  return { pushNotificationsEnabled: r.rows[0]!.push_notifications_enabled };
}

export async function getMomentNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ momentId: string; notifyOnChanges: boolean }> {
  await assertGroupMember(client, ctx, momentId);
  const r = await client.query<{ notify_on_changes: boolean }>(
    `SELECT notify_on_changes FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'
     LIMIT 1`,
    [momentId, ctx.userId]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Active participant not found.', 404);
  }
  return { momentId, notifyOnChanges: r.rows[0]!.notify_on_changes };
}

export async function patchMomentNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: PatchMomentNotificationPrefsInput
): Promise<{ momentId: string; notifyOnChanges: boolean }> {
  await assertGroupMember(client, ctx, momentId);
  const r = await client.query<{ notify_on_changes: boolean }>(
    `UPDATE collaboration.moment_participant
     SET notify_on_changes = $3, updated_at = now(), version = version + 1
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE'
     RETURNING notify_on_changes`,
    [momentId, ctx.userId, body.notifyOnChanges]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Active participant not found.', 404);
  }
  return { momentId, notifyOnChanges: r.rows[0]!.notify_on_changes };
}
