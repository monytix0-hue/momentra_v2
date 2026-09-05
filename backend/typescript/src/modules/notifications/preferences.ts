import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { assertGroupMember } from '../collaboration/group-membership';

const categoryPrefsSchema = z
  .object({
    finance: z.boolean().optional(),
    tasks: z.boolean().optional(),
    social: z.boolean().optional(),
    invites: z.boolean().optional(),
    approvals: z.boolean().optional(),
    reminders: z.boolean().optional(),
  })
  .strict();

const timeHHMM = z
  .string()
  .regex(/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/, 'Expected HH:MM or HH:MM:SS')
  .nullable()
  .optional();

export const patchGlobalNotificationPrefsSchema = z
  .object({
    pushNotificationsEnabled: z.boolean().optional(),
    categories: categoryPrefsSchema.optional(),
    quietHoursStart: timeHHMM,
    quietHoursEnd: timeHHMM,
    digestEnabled: z.boolean().optional(),
  })
  .strict()
  .refine(
    (b) =>
      b.pushNotificationsEnabled !== undefined ||
      b.categories !== undefined ||
      b.quietHoursStart !== undefined ||
      b.quietHoursEnd !== undefined ||
      b.digestEnabled !== undefined,
    { message: 'At least one preference field is required.' }
  );

export type PatchGlobalNotificationPrefsInput = z.infer<typeof patchGlobalNotificationPrefsSchema>;

export const patchMomentNotificationPrefsSchema = z
  .object({
    notifyOnChanges: z.boolean().optional(),
    reminderPreferences: z
      .object({
        billReminders: z.boolean().optional(),
        choreReminders: z.boolean().optional(),
        expenseReminders: z.boolean().optional(),
        photoReminders: z.boolean().optional(),
        paymentReminders: z.boolean().optional(),
      })
      .strict()
      .optional(),
  })
  .strict()
  .refine((b) => b.notifyOnChanges !== undefined || b.reminderPreferences !== undefined, {
    message: 'At least one preference field is required.',
  });

export type PatchMomentNotificationPrefsInput = z.infer<typeof patchMomentNotificationPrefsSchema>;

export type GlobalNotificationPrefs = {
  pushNotificationsEnabled: boolean;
  categories: {
    finance: boolean;
    tasks: boolean;
    social: boolean;
    invites: boolean;
    approvals: boolean;
    reminders: boolean;
  };
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  digestEnabled: boolean;
};

const DEFAULT_CATEGORIES = {
  finance: true,
  tasks: true,
  social: true,
  invites: true,
  approvals: true,
  reminders: true,
};

function mergeCategories(raw: unknown): GlobalNotificationPrefs['categories'] {
  const base = { ...DEFAULT_CATEGORIES };
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return base;
  for (const key of Object.keys(DEFAULT_CATEGORIES) as Array<keyof typeof DEFAULT_CATEGORIES>) {
    const v = (raw as Record<string, unknown>)[key];
    if (typeof v === 'boolean') base[key] = v;
  }
  return base;
}

export async function getGlobalNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext
): Promise<GlobalNotificationPrefs> {
  const r = await client.query<{
    push_notifications_enabled: boolean;
    notification_categories: unknown;
    quiet_hours_start: string | null;
    quiet_hours_end: string | null;
    digest_enabled: boolean;
  }>(
    `SELECT push_notifications_enabled,
            notification_categories,
            quiet_hours_start::text,
            quiet_hours_end::text,
            digest_enabled
     FROM core.user_profile WHERE user_id = $1`,
    [ctx.userId]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found.', 404);
  }
  const row = r.rows[0]!;
  return {
    pushNotificationsEnabled: row.push_notifications_enabled,
    categories: mergeCategories(row.notification_categories),
    quietHoursStart: row.quiet_hours_start ? row.quiet_hours_start.slice(0, 8) : null,
    quietHoursEnd: row.quiet_hours_end ? row.quiet_hours_end.slice(0, 8) : null,
    digestEnabled: row.digest_enabled,
  };
}

export async function patchGlobalNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  body: PatchGlobalNotificationPrefsInput
): Promise<GlobalNotificationPrefs> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PROFILE_UPDATE',
    resourceType: 'USER',
    ownerUserId: ctx.userId,
  });
  const current = await getGlobalNotificationPrefs(client, ctx);
  const nextCategories = body.categories
    ? { ...current.categories, ...body.categories }
    : current.categories;
  const r = await client.query<{
    push_notifications_enabled: boolean;
    notification_categories: unknown;
    quiet_hours_start: string | null;
    quiet_hours_end: string | null;
    digest_enabled: boolean;
  }>(
    `UPDATE core.user_profile
     SET push_notifications_enabled = COALESCE($2, push_notifications_enabled),
         notification_categories = $3::jsonb,
         quiet_hours_start = CASE WHEN $4::boolean THEN $5::time ELSE quiet_hours_start END,
         quiet_hours_end = CASE WHEN $6::boolean THEN $7::time ELSE quiet_hours_end END,
         digest_enabled = COALESCE($8, digest_enabled),
         updated_at = now(),
         version = version + 1
     WHERE user_id = $1 AND status = 'ACTIVE'
     RETURNING push_notifications_enabled,
               notification_categories,
               quiet_hours_start::text,
               quiet_hours_end::text,
               digest_enabled`,
    [
      ctx.userId,
      body.pushNotificationsEnabled ?? null,
      JSON.stringify(nextCategories),
      body.quietHoursStart !== undefined,
      body.quietHoursStart ?? null,
      body.quietHoursEnd !== undefined,
      body.quietHoursEnd ?? null,
      body.digestEnabled ?? null,
    ]
  );
  if (!r.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Profile not found or not active.', 404);
  }
  const row = r.rows[0]!;
  return {
    pushNotificationsEnabled: row.push_notifications_enabled,
    categories: mergeCategories(row.notification_categories),
    quietHoursStart: row.quiet_hours_start ? row.quiet_hours_start.slice(0, 8) : null,
    quietHoursEnd: row.quiet_hours_end ? row.quiet_hours_end.slice(0, 8) : null,
    digestEnabled: row.digest_enabled,
  };
}

export type MomentNotificationPrefs = {
  momentId: string;
  notifyOnChanges: boolean;
  reminderPreferences: Record<string, boolean>;
};

function asBoolMap(raw: unknown): Record<string, boolean> {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
  const out: Record<string, boolean> = {};
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof v === 'boolean') out[k] = v;
  }
  return out;
}

export async function getMomentNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<MomentNotificationPrefs> {
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
  const rem = await client.query<{ reminder_preferences: unknown }>(
    `SELECT reminder_preferences FROM collaboration.group_moment_context WHERE moment_id = $1`,
    [momentId]
  );
  return {
    momentId,
    notifyOnChanges: r.rows[0]!.notify_on_changes,
    reminderPreferences: asBoolMap(rem.rows[0]?.reminder_preferences),
  };
}

export async function patchMomentNotificationPrefs(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: PatchMomentNotificationPrefsInput
): Promise<MomentNotificationPrefs> {
  await assertGroupMember(client, ctx, momentId);
  if (body.notifyOnChanges !== undefined) {
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
  }
  if (body.reminderPreferences) {
    await client.query(
      `UPDATE collaboration.group_moment_context
       SET reminder_preferences = coalesce(reminder_preferences, '{}'::jsonb) || $2::jsonb,
           updated_at = now(),
           version = version + 1
       WHERE moment_id = $1`,
      [momentId, JSON.stringify(body.reminderPreferences)]
    );
  }
  return getMomentNotificationPrefs(client, ctx, momentId);
}
