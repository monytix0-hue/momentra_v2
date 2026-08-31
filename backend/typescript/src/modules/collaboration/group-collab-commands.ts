/**
 * GX2-C collaboration list + write helpers for Group moments.
 * Complements service.ts writers with membership checks, side-effects, and reads.
 */
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { recordCommandSideEffects } from '../../platform/events/outbox';
import { assertGroupMember } from './group-membership';
import * as collaborationService from './service';

export const planningItemSchema = z
  .object({
    title: z.string().min(1).max(500),
    dueAt: z.string().datetime().optional(),
  })
  .strict();

export const bookingSchema = z
  .object({
    title: z.string().min(1).max(500),
    bookedAt: z.string().datetime().optional(),
  })
  .strict();

export const updateSchema = z
  .object({
    message: z.string().min(1).max(5000),
  })
  .strict();

export const purchaseItemSchema = z
  .object({
    label: z.string().min(1).max(500),
    amount: z.string().optional(),
    customTypeLabel: z.string().max(200).optional(),
  })
  .strict();

export const residentSchema = z
  .object({
    name: z.string().min(1).max(200),
    roleCode: z.string().max(40).optional(),
  })
  .strict();

export const memorySchema = z
  .object({
    title: z.string().min(1).max(500),
    capturedAt: z.string().datetime().optional(),
  })
  .strict();

export async function createPlanningItemCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof planningItemSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createPlanningItem(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'PlanningItemCreated',
    domainCode: 'GROUP',
    aggregateType: 'PLANNING_ITEM',
    aggregateId: result.planningItemId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { planningItemId: result.planningItemId, momentId, title: body.title },
    auditActionCode: 'PLANNING_ITEM_CREATE',
    auditResourceType: 'PLANNING_ITEM',
    auditResourceId: result.planningItemId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_PLANNING_ITEM_CREATED',
      title: body.title,
      payload: { planningItemId: result.planningItemId },
    },
  });
  await client
    .query(
      `INSERT INTO projection.group_pulse (moment_id, participant_count, attention_count, task_open_count, projection_version, updated_at)
       VALUES ($1, 0, 0, 1, 1, now())
       ON CONFLICT (moment_id) DO UPDATE
         SET task_open_count = projection.group_pulse.task_open_count + 1,
             projection_version = projection.group_pulse.projection_version + 1,
             updated_at = now()`,
      [momentId]
    )
    .catch(() => undefined);
  await client
    .query(
      `INSERT INTO projection.group_life (moment_id, planning_payload, projection_version, updated_at)
       VALUES ($1, $2::jsonb, 1, now())
       ON CONFLICT (moment_id) DO UPDATE
         SET planning_payload = $2::jsonb,
             projection_version = projection.group_life.projection_version + 1,
             updated_at = now()`,
      [momentId, JSON.stringify({ lastPlanningItemId: result.planningItemId, title: body.title })]
    )
    .catch(() => undefined);
  return result;
}

export async function createBookingCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof bookingSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createBooking(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'BookingCreated',
    domainCode: 'GROUP',
    aggregateType: 'BOOKING',
    aggregateId: result.bookingId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { bookingId: result.bookingId, momentId, title: body.title },
    auditActionCode: 'BOOKING_CREATE',
    auditResourceType: 'BOOKING',
    auditResourceId: result.bookingId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_BOOKING_CREATED',
      title: body.title,
      payload: { bookingId: result.bookingId },
    },
  });
  return result;
}

export async function createPollCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof collaborationService.pollSchema>
) {
  const domain = await client.query<{ domain_code: string }>(
    `SELECT domain_code FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  const domainCode = domain.rows[0]?.domain_code ?? 'GROUP';
  if (domainCode === 'BUSINESS') {
    const { assertCompanyMomentAccess } = await import('../business/membership');
    await assertCompanyMomentAccess(client, ctx, momentId);
  } else {
    await assertGroupMember(client, ctx, momentId);
  }
  const result = await collaborationService.createPoll(client, ctx, momentId, body);
  const activityDomain = domainCode === 'BUSINESS' ? 'BUSINESS' : 'GROUP';
  const activityCode = domainCode === 'BUSINESS' ? 'BUSINESS_POLL_CREATED' : 'GROUP_POLL_CREATED';
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     )
     SELECT $1, de.domain_event_id, $6, 'MOMENT', $2::uuid,
            $7, $3, now(), $4::jsonb, 1
     FROM events.domain_event de
     WHERE de.aggregate_id = $5::uuid AND de.event_name = 'PollCreated'
     ORDER BY de.occurred_at DESC
     LIMIT 1
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      momentId,
      body.question,
      JSON.stringify({ pollId: result.pollId }),
      result.pollId,
      activityDomain,
      activityCode,
    ]
  ).catch(() => undefined);
  return result;
}

export async function postUpdateCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof updateSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.postUpdate(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupUpdatePosted',
    domainCode: 'GROUP',
    aggregateType: 'UPDATE',
    aggregateId: result.updateId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { updateId: result.updateId, momentId },
    auditActionCode: 'UPDATE_CREATE',
    auditResourceType: 'UPDATE',
    auditResourceId: result.updateId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_UPDATE_POSTED',
      title: body.message.slice(0, 120),
      payload: { updateId: result.updateId },
    },
  });
  return result;
}

export async function addPurchaseItemCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof purchaseItemSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.addPurchaseItem(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'PurchaseItemAdded',
    domainCode: 'GROUP',
    aggregateType: 'PURCHASE_ITEM',
    aggregateId: result.purchaseItemId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { purchaseItemId: result.purchaseItemId, momentId, label: body.label },
    auditActionCode: 'PURCHASE_ITEM_CREATE',
    auditResourceType: 'PURCHASE_ITEM',
    auditResourceId: result.purchaseItemId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_PURCHASE_ITEM_ADDED',
      title: body.label,
      payload: { purchaseItemId: result.purchaseItemId },
    },
  });
  return result;
}

export async function addResidentCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof residentSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.addResident(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'ResidentAdded',
    domainCode: 'GROUP',
    aggregateType: 'RESIDENT',
    aggregateId: result.residentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { residentId: result.residentId, momentId, name: body.name },
    auditActionCode: 'RESIDENT_MANAGE',
    auditResourceType: 'RESIDENT',
    auditResourceId: result.residentId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_RESIDENT_ADDED',
      title: body.name,
      payload: { residentId: result.residentId },
    },
  });
  return result;
}

export async function createMemoryCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof memorySchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createMemory(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'MemoryCreated',
    domainCode: 'GROUP',
    aggregateType: 'MEMORY',
    aggregateId: result.memoryId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { memoryId: result.memoryId, momentId, title: body.title },
    auditActionCode: 'MEMORY_CREATE',
    auditResourceType: 'MEMORY',
    auditResourceId: result.memoryId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_MEMORY_CREATED',
      title: body.title,
      payload: { memoryId: result.memoryId },
    },
  });
  // Keep memory facet non-empty: upsert a thin projection row if table supports it.
  await client
    .query(
      `INSERT INTO projection.group_memory (
         moment_id, memory_count, recent_memory_payload, projection_version, updated_at
       ) VALUES ($1, 1, $2::jsonb, 1, now())
       ON CONFLICT (moment_id) DO UPDATE
         SET memory_count = projection.group_memory.memory_count + 1,
             recent_memory_payload = $2::jsonb,
             projection_version = projection.group_memory.projection_version + 1,
             updated_at = now()`,
      [momentId, JSON.stringify({ lastMemoryId: result.memoryId, title: body.title })]
    )
    .catch(() => undefined);
  return result;
}

export async function listPlanningItems(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    planning_item_id: string;
    title: string;
    due_at: Date | null;
    status: string;
    created_at: Date;
  }>(
    `SELECT planning_item_id, title, due_at, status, created_at
     FROM collaboration.planning_item
     WHERE moment_id = $1
     ORDER BY COALESCE(due_at, created_at) ASC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      planningItemId: r.planning_item_id,
      title: r.title,
      dueAt: r.due_at?.toISOString() ?? null,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
    openCount: rows.rows.filter((r) => r.status === 'OPEN' || r.status === 'IN_PROGRESS').length,
  };
}

export async function listBookings(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    booking_id: string;
    provider_name: string | null;
    booked_at: Date | null;
    start_at: Date | null;
    end_at: Date | null;
    status: string;
  }>(
    `SELECT booking_id, provider_name, booked_at, start_at, end_at, status
     FROM collaboration.booking
     WHERE moment_id = $1
     ORDER BY COALESCE(start_at, booked_at, created_at) ASC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      bookingId: r.booking_id,
      title: r.provider_name,
      bookedAt: r.booked_at?.toISOString() ?? null,
      startAt: r.start_at?.toISOString() ?? null,
      endAt: r.end_at?.toISOString() ?? null,
      status: r.status,
    })),
  };
}

export async function listUpdates(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    group_update_id: string;
    body: string;
    status: string;
    created_at: Date;
    participant_id: string | null;
  }>(
    `SELECT group_update_id, body, status, created_at, participant_id
     FROM collaboration.group_update
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 100`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      updateId: r.group_update_id,
      message: r.body,
      status: r.status,
      createdAt: r.created_at.toISOString(),
      participantId: r.participant_id,
    })),
  };
}

export async function listPolls(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const polls = await client.query<{
    poll_id: string;
    question: string;
    status: string;
    closes_at: Date | null;
    created_at: Date;
  }>(
    `SELECT poll_id, question, status, closes_at, created_at
     FROM shared.poll
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 50`,
    [momentId]
  );
  const items = [];
  for (const p of polls.rows) {
    const opts = await client.query<{ poll_option_id: string; option_text: string; sort_order: number }>(
      `SELECT poll_option_id, option_text, sort_order FROM shared.poll_option
       WHERE poll_id = $1 ORDER BY sort_order ASC`,
      [p.poll_id]
    );
    items.push({
      pollId: p.poll_id,
      question: p.question,
      status: p.status,
      closesAt: p.closes_at?.toISOString() ?? null,
      createdAt: p.created_at.toISOString(),
      options: opts.rows.map((o) => ({
        pollOptionId: o.poll_option_id,
        text: o.option_text,
        sortOrder: o.sort_order,
      })),
    });
  }
  return { momentId, items };
}

export async function listPurchaseItems(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    purchase_item_id: string;
    title: string;
    target_amount: string | null;
    status: string;
  }>(
    `SELECT purchase_item_id, title, target_amount::text, status
     FROM collaboration.purchase_item
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      purchaseItemId: r.purchase_item_id,
      label: r.title,
      amount: r.target_amount,
      status: r.status,
    })),
  };
}

export async function listResidents(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    resident_id: string;
    participant_id: string;
    resident_role: string | null;
    status: string;
  }>(
    `SELECT resident_id, participant_id, resident_role, status
     FROM collaboration.resident
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY created_at ASC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      residentId: r.resident_id,
      participantId: r.participant_id,
      roleCode: r.resident_role,
      status: r.status,
    })),
  };
}

export async function listMemories(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    memory_id: string;
    title: string | null;
    occurred_at: Date | null;
    status: string;
  }>(
    `SELECT memory_id, title, occurred_at, status
     FROM memory.memory
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY COALESCE(occurred_at, created_at) DESC
     LIMIT 100`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      memoryId: r.memory_id,
      title: r.title,
      occurredAt: r.occurred_at?.toISOString() ?? null,
      status: r.status,
    })),
    memoryCount: rows.rows.length,
  };
}

export const vendorSchema = z
  .object({
    vendorName: z.string().min(1).max(500),
    vendorType: z.string().max(100).optional(),
    phone: z.string().max(40).optional(),
    email: z.string().max(200).optional(),
    notes: z.string().max(2000).optional(),
    quotedPrice: z.string().max(40).optional(),
    statusLabel: z.string().max(100).optional(),
  })
  .strict();

export const attendanceSchema = z
  .object({
    participantId: z.string().uuid(),
    attendanceStatus: z.enum(['UNKNOWN', 'EXPECTED', 'CONFIRMED', 'ATTENDED', 'ABSENT']),
    note: z.string().max(2000).optional(),
  })
  .strict();

export const livingRuleSchema = z
  .object({
    title: z.string().min(1).max(500),
    ruleText: z.string().min(1).max(10000),
  })
  .strict();

export const sharedAssetSchema = z
  .object({
    title: z.string().min(1).max(500),
    assetType: z.string().max(100).optional(),
    conditionCode: z.enum(['NEW', 'GOOD', 'FAIR', 'POOR', 'OUT_OF_SERVICE']).optional(),
  })
  .strict();

export const maintenanceSchema = z
  .object({
    title: z.string().min(1).max(500),
    description: z.string().max(5000).optional(),
    sharedAssetId: z.string().uuid().optional(),
  })
  .strict();

function packVendorContactDetails(body: z.infer<typeof vendorSchema>): Record<string, unknown> {
  const contact: Record<string, unknown> = {};
  if (body.phone !== undefined) contact.phone = body.phone;
  if (body.email !== undefined) contact.email = body.email;
  if (body.notes !== undefined) contact.notes = body.notes;
  if (body.quotedPrice !== undefined) contact.quotedPrice = body.quotedPrice;
  if (body.statusLabel !== undefined) contact.statusLabel = body.statusLabel;
  return contact;
}

export async function createGroupVendorCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof vendorSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createGroupVendor(client, ctx, momentId, {
    vendorName: body.vendorName,
    vendorType: body.vendorType,
    contactDetails: packVendorContactDetails(body),
  });
  await recordCommandSideEffects(client, ctx, {
    eventName: 'GroupVendorCreated',
    domainCode: 'GROUP',
    aggregateType: 'GROUP_VENDOR',
    aggregateId: result.groupVendorId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { groupVendorId: result.groupVendorId, momentId, vendorName: body.vendorName },
    auditActionCode: 'GROUP_VENDOR_MANAGE',
    auditResourceType: 'GROUP_VENDOR',
    auditResourceId: result.groupVendorId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_VENDOR_CREATED',
      title: body.vendorName,
      payload: { groupVendorId: result.groupVendorId },
    },
  });
  return result;
}

export async function recordAttendanceCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof attendanceSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.recordAttendance(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'AttendanceRecorded',
    domainCode: 'GROUP',
    aggregateType: 'ATTENDANCE',
    aggregateId: result.attendanceId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: {
      attendanceId: result.attendanceId,
      momentId,
      participantId: body.participantId,
      attendanceStatus: body.attendanceStatus,
    },
    auditActionCode: 'ATTENDANCE_RECORD',
    auditResourceType: 'ATTENDANCE',
    auditResourceId: result.attendanceId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_ATTENDANCE_RECORDED',
      title: body.attendanceStatus,
      payload: { attendanceId: result.attendanceId, participantId: body.participantId },
    },
  });
  return result;
}

export async function createLivingRuleCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof livingRuleSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createLivingRule(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'LivingRuleCreated',
    domainCode: 'GROUP',
    aggregateType: 'LIVING_RULE',
    aggregateId: result.livingRuleId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { livingRuleId: result.livingRuleId, momentId, title: body.title },
    auditActionCode: 'RULE_MANAGE',
    auditResourceType: 'LIVING_RULE',
    auditResourceId: result.livingRuleId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_LIVING_RULE_CREATED',
      title: body.title,
      payload: { livingRuleId: result.livingRuleId },
    },
  });
  return result;
}

export async function createSharedAssetCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof sharedAssetSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createSharedAsset(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'SharedAssetCreated',
    domainCode: 'GROUP',
    aggregateType: 'SHARED_ASSET',
    aggregateId: result.sharedAssetId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { sharedAssetId: result.sharedAssetId, momentId, title: body.title },
    auditActionCode: 'ASSET_MANAGE',
    auditResourceType: 'SHARED_ASSET',
    auditResourceId: result.sharedAssetId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_SHARED_ASSET_CREATED',
      title: body.title,
      payload: { sharedAssetId: result.sharedAssetId },
    },
  });
  return result;
}

export async function createMaintenanceRecordCommand(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof maintenanceSchema>
) {
  await assertGroupMember(client, ctx, momentId);
  const result = await collaborationService.createMaintenanceRecord(client, ctx, momentId, body);
  await recordCommandSideEffects(client, ctx, {
    eventName: 'MaintenanceRecordCreated',
    domainCode: 'GROUP',
    aggregateType: 'MAINTENANCE_RECORD',
    aggregateId: result.maintenanceRecordId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { maintenanceRecordId: result.maintenanceRecordId, momentId, title: body.title },
    auditActionCode: 'MAINTENANCE_CREATE',
    auditResourceType: 'MAINTENANCE_RECORD',
    auditResourceId: result.maintenanceRecordId,
    afterSnapshot: result,
    activity: {
      domainCode: 'GROUP',
      momentId,
      activityCode: 'GROUP_MAINTENANCE_CREATED',
      title: body.title,
      payload: { maintenanceRecordId: result.maintenanceRecordId },
    },
  });
  return result;
}

export async function listVendors(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    group_vendor_id: string;
    vendor_name: string;
    vendor_type: string | null;
    contact_details: Record<string, unknown>;
    status: string;
    created_at: Date;
  }>(
    `SELECT group_vendor_id, vendor_name, vendor_type, contact_details, status, created_at
     FROM collaboration.group_vendor
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      groupVendorId: r.group_vendor_id,
      vendorName: r.vendor_name,
      vendorType: r.vendor_type,
      contactDetails: r.contact_details,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listAttendance(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    attendance_id: string;
    participant_id: string;
    attendance_status: string;
    note: string | null;
    checked_at: Date | null;
    updated_at: Date;
  }>(
    `SELECT attendance_id, participant_id, attendance_status, note, checked_at, updated_at
     FROM collaboration.attendance
     WHERE moment_id = $1
     ORDER BY updated_at DESC
     LIMIT 500`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      attendanceId: r.attendance_id,
      participantId: r.participant_id,
      attendanceStatus: r.attendance_status,
      note: r.note,
      checkedAt: r.checked_at?.toISOString() ?? null,
      updatedAt: r.updated_at.toISOString(),
    })),
  };
}

export async function listLivingRules(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    living_rule_id: string;
    title: string;
    rule_text: string;
    status: string;
    created_at: Date;
  }>(
    `SELECT living_rule_id, title, rule_text, status, created_at
     FROM collaboration.living_rule
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      livingRuleId: r.living_rule_id,
      title: r.title,
      ruleText: r.rule_text,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listSharedAssets(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    shared_asset_id: string;
    title: string;
    asset_type: string | null;
    condition_code: string | null;
    status: string;
    created_at: Date;
  }>(
    `SELECT shared_asset_id, title, asset_type, condition_code, status, created_at
     FROM collaboration.shared_asset
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      sharedAssetId: r.shared_asset_id,
      title: r.title,
      assetType: r.asset_type,
      conditionCode: r.condition_code,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
  };
}

export async function listMaintenanceRecords(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertGroupMember(client, ctx, momentId);
  const rows = await client.query<{
    maintenance_record_id: string;
    shared_asset_id: string | null;
    title: string;
    description: string | null;
    status: string;
    created_at: Date;
  }>(
    `SELECT maintenance_record_id, shared_asset_id, title, description, status, created_at
     FROM collaboration.maintenance_record
     WHERE moment_id = $1
     ORDER BY created_at DESC
     LIMIT 200`,
    [momentId]
  );
  return {
    momentId,
    items: rows.rows.map((r) => ({
      maintenanceRecordId: r.maintenance_record_id,
      sharedAssetId: r.shared_asset_id,
      title: r.title,
      description: r.description,
      status: r.status,
      createdAt: r.created_at.toISOString(),
    })),
  };
}
