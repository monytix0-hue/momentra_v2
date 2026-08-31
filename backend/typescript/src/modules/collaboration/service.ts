import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

export const participantSchema = z
  .object({
    userId: z.string().uuid().optional(),
    displayName: z.string().max(200).optional(),
    email: z.string().max(200).nullish(),
    phone: z.string().max(40).nullish(),
    roleCode: z
      .enum(['ORGANIZER', 'CO_ORGANIZER', 'PARTICIPANT', 'RESIDENT', 'CONTRIBUTOR', 'OBSERVER'])
      .default('PARTICIPANT'),
  })
  .strict();

export const pollSchema = z
  .object({
    question: z.string().min(1).max(1000),
    options: z.array(z.string().min(1).max(500)).min(2).max(20),
    closesAt: z.string().datetime().optional(),
    pollType: z.enum(['SINGLE_CHOICE', 'MULTI_CHOICE', 'YES_NO']).optional(),
  })
  .strict();

async function assertMomentAccess(client: PoolClient, ctx: RequestContext, momentId: string): Promise<{
  domainCode: 'GROUP' | 'BUSINESS' | 'PERSONAL';
  companyId: string | null;
}> {
  const row = await client.query<{ domain_code: string; company_id: string | null }>(
    `SELECT m.domain_code,
            bmc.company_id
     FROM core.moment m
     LEFT JOIN business.business_moment_context bmc ON bmc.moment_id = m.moment_id
     LEFT JOIN personal.personal_moment_context pmc ON pmc.moment_id = m.moment_id AND pmc.user_id = $2
     LEFT JOIN collaboration.moment_participant mp ON mp.moment_id = m.moment_id AND mp.user_id = $2
     LEFT JOIN business.company_membership cm
       ON cm.company_id = bmc.company_id AND cm.user_id = $2 AND cm.status = 'ACTIVE'
     WHERE m.moment_id = $1
       AND (
         pmc.user_id IS NOT NULL
         OR mp.participant_id IS NOT NULL
         OR m.created_by_user_id = $2
         OR (m.domain_code = 'BUSINESS' AND cm.company_membership_id IS NOT NULL)
       )`,
    [momentId, ctx.userId]
  );
  if (!row.rows[0]) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Not permitted for this moment.', 403);
  }
  return {
    domainCode: row.rows[0].domain_code as 'GROUP' | 'BUSINESS' | 'PERSONAL',
    companyId: row.rows[0].company_id,
  };
}

export async function addParticipant(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof participantSchema>
): Promise<{ participantId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'PARTICIPANT_MANAGE', resourceType: 'PARTICIPANT', momentId });

  if (body.userId || !body.displayName) {
    const targetUserId = body.userId ?? ctx.userId;
    const inserted = await client.query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, $3, 'ACTIVE', now(), 1)
       ON CONFLICT DO NOTHING
       RETURNING participant_id`,
      [momentId, targetUserId, body.roleCode]
    );
    if (!inserted.rows[0]) {
      const existing = await client.query<{ participant_id: string }>(
        `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
        [momentId, targetUserId]
      );
      return { participantId: existing.rows[0]!.participant_id, momentId };
    }
    return { participantId: inserted.rows[0].participant_id, momentId };
  }

  const party = await client.query<{ external_party_id: string }>(
    `INSERT INTO core.external_party (party_type, display_name, status)
     VALUES ('PERSON', $1, 'ACTIVE')
     RETURNING external_party_id`,
    [body.displayName]
  );
  const inserted = await client.query<{ participant_id: string }>(
    `INSERT INTO collaboration.moment_participant (
       moment_id, external_party_id, participant_role, status, invited_at, version, metadata
     ) VALUES ($1, $2, $3, 'INVITED', now(), 1, $4::jsonb)
     RETURNING participant_id`,
    [
      momentId,
      party.rows[0]!.external_party_id,
      body.roleCode,
      JSON.stringify({
        displayName: body.displayName,
        email: body.email ?? null,
        phone: body.phone ?? null,
      }),
    ]
  );
  return { participantId: inserted.rows[0]!.participant_id, momentId };
}

export async function createPoll(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof pollSchema>
): Promise<{ pollId: string; momentId: string; question: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'POLL_CREATE', resourceType: 'POLL', momentId });
  const scope = await assertMomentAccess(client, ctx, momentId);
  const domainCode = scope.domainCode === 'BUSINESS' ? 'BUSINESS' : 'GROUP';

  const pollType = body.pollType ?? 'SINGLE_CHOICE';
  const pollInsert = await client.query<{ poll_id: string }>(
    `INSERT INTO shared.poll (
       moment_id, domain_code, company_id, question, poll_type, closes_at, status, created_by_user_id
     ) VALUES ($1, $2, $3, $4, $5, $6::timestamptz, 'OPEN', $7)
     RETURNING poll_id`,
    [momentId, domainCode, scope.companyId, body.question, pollType, body.closesAt ?? null, ctx.userId]
  );
  const pollId = pollInsert.rows[0]!.poll_id;

  for (let i = 0; i < body.options.length; i++) {
    await client.query(
      `INSERT INTO shared.poll_option (poll_id, option_text, sort_order) VALUES ($1, $2, $3)`,
      [pollId, body.options[i], i]
    );
  }

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PollCreated',
    domainCode,
    aggregateType: 'POLL',
    aggregateId: pollId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { pollId, momentId, question: body.question },
  });

  return { pollId, momentId, question: body.question };
}

export async function createPlanningItem(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; dueAt?: string }
): Promise<{ planningItemId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'PLANNING_ITEM_CREATE', resourceType: 'PLANNING_ITEM', momentId });
  const r = await client.query<{ planning_item_id: string }>(
    `INSERT INTO collaboration.planning_item (moment_id, title, due_at, status)
     VALUES ($1, $2, $3::timestamptz, 'OPEN')
     RETURNING planning_item_id`,
    [momentId, body.title, body.dueAt ?? null]
  );
  return { planningItemId: r.rows[0]!.planning_item_id, momentId };
}

export async function createBooking(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; bookedAt?: string }
): Promise<{ bookingId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'BOOKING_CREATE', resourceType: 'BOOKING', momentId });
  const r = await client.query<{ booking_id: string }>(
    `INSERT INTO collaboration.booking (moment_id, booking_type, provider_name, booked_at, status, version)
     VALUES ($1, 'OTHER', $2, $3::timestamptz, 'PLANNED', 1)
     RETURNING booking_id`,
    [momentId, body.title, body.bookedAt ?? null]
  );
  return { bookingId: r.rows[0]!.booking_id, momentId };
}

export const contributionSchema = z
  .object({
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    currencyCode: z.string().length(3).toUpperCase(),
    label: z.string().max(200).optional(),
  })
  .strict();

export async function recordContribution(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof contributionSchema>
): Promise<{ contributionId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'CONTRIBUTION_RECORD', resourceType: 'CONTRIBUTION', momentId });
  const participant = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
    [momentId, ctx.userId]
  );
  if (!participant.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Active participant required to record contribution.', 400);
  }
  const r = await client.query<{ contribution_id: string }>(
    `INSERT INTO finance.contribution (moment_id, participant_id, amount, currency_code, status, version)
     VALUES ($1, $2, $3, $4, 'RECORDED', 1)
     RETURNING contribution_id`,
    [momentId, participant.rows[0].participant_id, body.amount, body.currencyCode]
  );
  return { contributionId: r.rows[0]!.contribution_id, momentId };
}

export async function postUpdate(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { message: string }
): Promise<{ updateId: string; momentId: string; authorUserId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'UPDATE_CREATE', resourceType: 'UPDATE', momentId });
  const participant = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
    [momentId, ctx.userId]
  );
  const r = await client.query<{ group_update_id: string }>(
    `INSERT INTO collaboration.group_update (moment_id, participant_id, body, status)
     VALUES ($1, $2, $3, 'PUBLISHED')
     RETURNING group_update_id`,
    [momentId, participant.rows[0]?.participant_id ?? null, body.message]
  );
  return { updateId: r.rows[0]!.group_update_id, momentId, authorUserId: ctx.userId };
}

export async function addPurchaseItem(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { label: string; amount?: string; customTypeLabel?: string }
): Promise<{ purchaseItemId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'PURCHASE_ITEM_CREATE', resourceType: 'PURCHASE_ITEM', momentId });
  const r = await client.query<{ purchase_item_id: string }>(
    `INSERT INTO collaboration.purchase_item (moment_id, title, target_amount, status, version)
     VALUES ($1, $2, $3, 'PLANNED', 1)
     RETURNING purchase_item_id`,
    [momentId, body.customTypeLabel ? `${body.label} (${body.customTypeLabel})` : body.label, body.amount ?? null]
  );
  return { purchaseItemId: r.rows[0]!.purchase_item_id, momentId };
}

export async function addResident(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { name: string; roleCode?: string }
): Promise<{ residentId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'RESIDENT_MANAGE', resourceType: 'RESIDENT', momentId });
  const participant = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
    [momentId, ctx.userId]
  );
  if (!participant.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Active participant required to add resident.', 400);
  }
  const r = await client.query<{ resident_id: string }>(
    `INSERT INTO collaboration.resident (moment_id, participant_id, resident_role, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     RETURNING resident_id`,
    [momentId, participant.rows[0].participant_id, body.roleCode ?? 'RESIDENT']
  );
  return { residentId: r.rows[0]!.resident_id, momentId };
}

export async function createMemory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; capturedAt?: string }
): Promise<{ memoryId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'MEMORY_CREATE', resourceType: 'MEMORY', momentId });
  const r = await client.query<{ memory_id: string }>(
    `INSERT INTO memory.memory (
       scope_type, scope_id, domain_code, moment_id, title, occurred_at, status, created_by_user_id, version
     ) VALUES ('MOMENT', $1, (SELECT domain_code FROM core.moment WHERE moment_id = $1), $1, $2, COALESCE($3::timestamptz, now()), 'ACTIVE', $4, 1)
     RETURNING memory_id`,
    [momentId, body.title, body.capturedAt ?? null, ctx.userId]
  );
  return { memoryId: r.rows[0]!.memory_id, momentId };
}

export async function createGroupVendor(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    vendorName: string;
    vendorType?: string;
    contactDetails?: Record<string, unknown>;
  }
): Promise<{ groupVendorId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'GROUP_VENDOR_MANAGE',
    resourceType: 'GROUP_VENDOR',
    momentId,
  });
  const r = await client.query<{ group_vendor_id: string }>(
    `INSERT INTO collaboration.group_vendor (
       moment_id, vendor_name, vendor_type, contact_details, status, version
     ) VALUES ($1, $2, $3, $4::jsonb, 'ACTIVE', 1)
     RETURNING group_vendor_id`,
    [
      momentId,
      body.vendorName,
      body.vendorType ?? null,
      JSON.stringify(body.contactDetails ?? {}),
    ]
  );
  return { groupVendorId: r.rows[0]!.group_vendor_id, momentId };
}

export async function recordAttendance(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    participantId: string;
    attendanceStatus: 'UNKNOWN' | 'EXPECTED' | 'CONFIRMED' | 'ATTENDED' | 'ABSENT';
    note?: string;
  }
): Promise<{ attendanceId: string; momentId: string; participantId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ATTENDANCE_RECORD',
    resourceType: 'ATTENDANCE',
    momentId,
  });
  const r = await client.query<{ attendance_id: string }>(
    `INSERT INTO collaboration.attendance (
       moment_id, participant_id, attendance_status, note, checked_at, updated_at
     ) VALUES ($1, $2, $3, $4, now(), now())
     ON CONFLICT (moment_id, participant_id) DO UPDATE
       SET attendance_status = EXCLUDED.attendance_status,
           note = EXCLUDED.note,
           checked_at = now(),
           updated_at = now()
     RETURNING attendance_id`,
    [momentId, body.participantId, body.attendanceStatus, body.note ?? null]
  );
  return {
    attendanceId: r.rows[0]!.attendance_id,
    momentId,
    participantId: body.participantId,
  };
}

export async function createLivingRule(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; ruleText: string }
): Promise<{ livingRuleId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'RULE_MANAGE',
    resourceType: 'LIVING_RULE',
    momentId,
  });
  const r = await client.query<{ living_rule_id: string }>(
    `INSERT INTO collaboration.living_rule (moment_id, title, rule_text, status, version)
     VALUES ($1, $2, $3, 'ACTIVE', 1)
     RETURNING living_rule_id`,
    [momentId, body.title, body.ruleText]
  );
  return { livingRuleId: r.rows[0]!.living_rule_id, momentId };
}

export async function createSharedAsset(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    title: string;
    assetType?: string;
    conditionCode?: 'NEW' | 'GOOD' | 'FAIR' | 'POOR' | 'OUT_OF_SERVICE';
  }
): Promise<{ sharedAssetId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ASSET_MANAGE',
    resourceType: 'SHARED_ASSET',
    momentId,
  });
  const r = await client.query<{ shared_asset_id: string }>(
    `INSERT INTO collaboration.shared_asset (
       moment_id, title, asset_type, condition_code, status, version
     ) VALUES ($1, $2, $3, $4, 'ACTIVE', 1)
     RETURNING shared_asset_id`,
    [momentId, body.title, body.assetType ?? null, body.conditionCode ?? null]
  );
  return { sharedAssetId: r.rows[0]!.shared_asset_id, momentId };
}

export async function createMaintenanceRecord(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; description?: string; sharedAssetId?: string }
): Promise<{ maintenanceRecordId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'MAINTENANCE_CREATE',
    resourceType: 'MAINTENANCE_RECORD',
    momentId,
  });
  const r = await client.query<{ maintenance_record_id: string }>(
    `INSERT INTO collaboration.maintenance_record (
       moment_id, shared_asset_id, title, description, status
     ) VALUES ($1, $2, $3, $4, 'OPEN')
     RETURNING maintenance_record_id`,
    [momentId, body.sharedAssetId ?? null, body.title, body.description ?? null]
  );
  return { maintenanceRecordId: r.rows[0]!.maintenance_record_id, momentId };
}
