import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import {
  assertGovernanceAllowed,
  assertGroupPeopleManageAllowed,
  assertPollCloseAllowed,
  canClosePoll,
} from '../governance/resolver';
import { assertCallerIsOrganizer, assertGroupMember } from './group-membership';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { emitLeanBusinessEvent, loadMomentTaxonomy } from '../analytics/lean-events';
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
    // Offset ISO / Gson null — see clientIsoDatetime notes in group-collab-commands.
    closesAt: z
      .string()
      .refine((s) => !Number.isNaN(Date.parse(s)), { message: 'Invalid ISO datetime' })
      .nullish(),
    pollType: z.enum(['SINGLE_CHOICE', 'MULTI_CHOICE', 'YES_NO']).nullish(),
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
  const isGuest = Boolean(body.displayName?.trim()) && !body.userId;

  if (isGuest) {
    const me = await assertGroupMember(client, ctx, momentId);
    assertCallerIsOrganizer(me);

    const displayName = body.displayName!.trim();
    const party = await client.query<{ external_party_id: string }>(
      `INSERT INTO core.external_party (party_type, display_name, status)
       VALUES ('PERSON', $1, 'ACTIVE')
       RETURNING external_party_id`,
      [displayName]
    );
    // ACTIVE so guests appear in expense paid-by / split pickers.
    const inserted = await client.query<{ participant_id: string }>(
      `INSERT INTO collaboration.moment_participant (
         moment_id, external_party_id, participant_role, status, invited_at, joined_at, version, metadata
       ) VALUES ($1, $2, 'OBSERVER', 'ACTIVE', now(), now(), 1, $3::jsonb)
       RETURNING participant_id`,
      [
        momentId,
        party.rows[0]!.external_party_id,
        JSON.stringify({
          displayName,
          email: body.email ?? null,
          phone: body.phone ?? null,
          guest: true,
        }),
      ]
    );
    const tax = await loadMomentTaxonomy(client, momentId);
    await emitLeanBusinessEvent(client, ctx, {
      eventName: 'participant_invited',
      momentId,
      momentDomain: tax?.domain ?? 'group',
      momentCategory: tax?.category,
      momentType: tax?.type,
      properties: {
        invite_id: inserted.rows[0]!.participant_id,
        invite_channel: 'in_app',
        invitee_user_status: 'external',
        invited_role: 'OBSERVER',
        is_guest: true,
      },
    });
    return { participantId: inserted.rows[0]!.participant_id, momentId };
  }

  await assertGroupPeopleManageAllowed(client, ctx, momentId);

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
  const tax = await loadMomentTaxonomy(client, momentId);
  await emitLeanBusinessEvent(client, ctx, {
    eventName: 'participant_joined',
    momentId,
    momentDomain: tax?.domain ?? 'group',
    momentCategory: tax?.category,
    momentType: tax?.type,
    userId: targetUserId,
    properties: {
      participant_role: body.roleCode,
      join_source: 'add_participant',
      was_existing_user: true,
    },
  });
  return { participantId: inserted.rows[0].participant_id, momentId };
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

export const votePollSchema = z
  .object({
    pollOptionId: z.string().uuid(),
  })
  .strict();

export async function getPollById(
  client: PoolClient,
  ctx: RequestContext,
  pollId: string
): Promise<{
  pollId: string;
  momentId: string;
  question: string;
  status: string;
  pollType: string;
  closesAt: string | null;
  createdAt: string;
  createdByUserId: string;
  canClose: boolean;
  options: Array<{
    pollOptionId: string;
    text: string;
    sortOrder: number;
    voteCount: number;
    votedByMe: boolean;
  }>;
}> {
  const poll = await client.query<{
    poll_id: string;
    moment_id: string;
    question: string;
    status: string;
    poll_type: string;
    closes_at: Date | null;
    created_at: Date;
    created_by_user_id: string;
  }>(
    `SELECT poll_id, moment_id, question, status, poll_type, closes_at, created_at, created_by_user_id
     FROM shared.poll WHERE poll_id = $1`,
    [pollId]
  );
  if (!poll.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Poll not found', 404);
  }
  const row = poll.rows[0];
  await assertMomentAccess(client, ctx, row.moment_id);

  const opts = await client.query<{
    poll_option_id: string;
    option_text: string;
    sort_order: number;
  }>(
    `SELECT poll_option_id, option_text, sort_order
     FROM shared.poll_option WHERE poll_id = $1 ORDER BY sort_order ASC`,
    [pollId]
  );

  const voteCounts = await client.query<{ poll_option_id: string; n: string }>(
    `SELECT poll_option_id, COUNT(*)::text AS n
     FROM shared.poll_vote WHERE poll_id = $1 GROUP BY poll_option_id`,
    [pollId]
  );
  const countMap = new Map(voteCounts.rows.map((v) => [v.poll_option_id, Number(v.n)]));

  const myVotes = await client.query<{ poll_option_id: string }>(
    `SELECT poll_option_id FROM shared.poll_vote WHERE poll_id = $1 AND voter_user_id = $2`,
    [pollId, ctx.userId]
  );
  const mySet = new Set(myVotes.rows.map((v) => v.poll_option_id));

  const allowedToClose = await canClosePoll(client, ctx, row.moment_id, row.created_by_user_id);

  return {
    pollId: row.poll_id,
    momentId: row.moment_id,
    question: row.question,
    status: row.status,
    pollType: row.poll_type,
    closesAt: row.closes_at?.toISOString() ?? null,
    createdAt: row.created_at.toISOString(),
    createdByUserId: row.created_by_user_id,
    canClose: row.status === 'OPEN' && allowedToClose,
    options: opts.rows.map((o) => ({
      pollOptionId: o.poll_option_id,
      text: o.option_text,
      sortOrder: o.sort_order,
      voteCount: countMap.get(o.poll_option_id) ?? 0,
      votedByMe: mySet.has(o.poll_option_id),
    })),
  };
}

export async function votePoll(
  client: PoolClient,
  ctx: RequestContext,
  pollId: string,
  body: z.infer<typeof votePollSchema>
): Promise<{ pollId: string; pollOptionId: string; momentId: string }> {
  const poll = await client.query<{ moment_id: string; status: string; closes_at: Date | null }>(
    `SELECT moment_id, status, closes_at FROM shared.poll WHERE poll_id = $1`,
    [pollId]
  );
  if (!poll.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Poll not found', 404);
  }
  const { moment_id: momentId, status, closes_at: closesAt } = poll.rows[0];
  await assertGovernanceAllowed(client, ctx, { actionCode: 'POLL_VOTE', resourceType: 'POLL', momentId });
  await assertMomentAccess(client, ctx, momentId);

  if (status !== 'OPEN') {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Poll is closed', 409);
  }
  if (closesAt && closesAt.getTime() <= Date.now()) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Poll voting period has ended', 409);
  }

  const option = await client.query<{ poll_option_id: string }>(
    `SELECT poll_option_id FROM shared.poll_option WHERE poll_option_id = $1 AND poll_id = $2`,
    [body.pollOptionId, pollId]
  );
  if (!option.rows[0]) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Invalid poll option', 400);
  }

  await client.query(
    `INSERT INTO shared.poll_vote (poll_id, poll_option_id, moment_id, voter_user_id)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (poll_option_id, voter_user_id) DO NOTHING`,
    [pollId, body.pollOptionId, momentId, ctx.userId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PollVoted',
    domainCode: 'GROUP',
    aggregateType: 'POLL',
    aggregateId: pollId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { pollId, pollOptionId: body.pollOptionId, momentId },
  });

  return { pollId, pollOptionId: body.pollOptionId, momentId };
}

export async function closePoll(
  client: PoolClient,
  ctx: RequestContext,
  pollId: string
): Promise<{ pollId: string; momentId: string; status: string }> {
  const poll = await client.query<{ moment_id: string; status: string; created_by_user_id: string }>(
    `SELECT moment_id, status, created_by_user_id FROM shared.poll WHERE poll_id = $1`,
    [pollId]
  );
  if (!poll.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Poll not found', 404);
  }
  const { moment_id: momentId, status, created_by_user_id: createdByUserId } = poll.rows[0];
  await assertGovernanceAllowed(client, ctx, { actionCode: 'POLL_CLOSE', resourceType: 'POLL', momentId });
  await assertMomentAccess(client, ctx, momentId);

  if (status === 'CLOSED') {
    return { pollId, momentId, status: 'CLOSED' };
  }
  if (status !== 'OPEN') {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Poll cannot be closed', 409);
  }
  await assertPollCloseAllowed(client, ctx, momentId, createdByUserId);

  await client.query(
    `UPDATE shared.poll SET status = 'CLOSED', updated_at = now() WHERE poll_id = $1`,
    [pollId]
  );

  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'PollClosed',
    domainCode: 'GROUP',
    aggregateType: 'POLL',
    aggregateId: pollId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { pollId, momentId },
  });

  return { pollId, momentId, status: 'CLOSED' };
}

export async function createPlanningItem(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    title: string;
    dueAt?: string | null;
    categoryCode?: string | null;
    location?: string | null;
    priorityCode?: string | null;
    description?: string | null;
  }
): Promise<{ planningItemId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'PLANNING_ITEM_CREATE', resourceType: 'PLANNING_ITEM', momentId });
  const r = await client.query<{ planning_item_id: string }>(
    `INSERT INTO collaboration.planning_item (
       moment_id, title, description, due_at, status, category_code, location, priority_code
     )
     VALUES ($1, $2, $3, $4::timestamptz, 'OPEN', $5, $6, $7)
     RETURNING planning_item_id`,
    [
      momentId,
      body.title,
      body.description ?? null,
      body.dueAt ?? null,
      body.categoryCode ?? null,
      body.location ?? null,
      body.priorityCode ?? null,
    ]
  );
  return { planningItemId: r.rows[0]!.planning_item_id, momentId };
}

export async function createBooking(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: { title: string; bookedAt?: string | null }
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
  body: { message: string; notifyMembers?: boolean; urgencyCode?: string }
): Promise<{ updateId: string; momentId: string; authorUserId: string; notifyMembers: boolean; urgencyCode: string }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'UPDATE_CREATE', resourceType: 'UPDATE', momentId });
  const participant = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant WHERE moment_id = $1 AND user_id = $2 LIMIT 1`,
    [momentId, ctx.userId]
  );
  const urgency = body.urgencyCode === 'URGENT' ? 'URGENT' : 'NORMAL';
  const r = await client.query<{ group_update_id: string }>(
    `INSERT INTO collaboration.group_update (moment_id, participant_id, body, status, urgency_code)
     VALUES ($1, $2, $3, 'PUBLISHED', $4)
     RETURNING group_update_id`,
    [momentId, participant.rows[0]?.participant_id ?? null, body.message, urgency]
  );
  return {
    updateId: r.rows[0]!.group_update_id,
    momentId,
    authorUserId: ctx.userId,
    notifyMembers: body.notifyMembers !== false,
    urgencyCode: urgency,
  };
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
  body: { title: string; capturedAt?: string | null }
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

async function resolveParticipantByDisplayName(
  client: PoolClient,
  momentId: string,
  displayName?: string
): Promise<string | null> {
  const name = displayName?.trim();
  if (!name) return null;
  const r = await client.query<{ participant_id: string }>(
    `SELECT mp.participant_id
     FROM collaboration.moment_participant mp
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE mp.moment_id = $1 AND mp.status = 'ACTIVE'
       AND LOWER(TRIM(COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', ''))) = LOWER($2)
     LIMIT 1`,
    [momentId, name]
  );
  return r.rows[0]?.participant_id ?? null;
}

async function actorParticipantId(
  client: PoolClient,
  momentId: string,
  userId: string
): Promise<string | null> {
  const r = await client.query<{ participant_id: string }>(
    `SELECT participant_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND user_id = $2 AND status = 'ACTIVE' LIMIT 1`,
    [momentId, userId]
  );
  return r.rows[0]?.participant_id ?? null;
}

function normalizeScheduledAt(raw?: string): string | null {
  if (!raw?.trim()) return null;
  const t = raw.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(t)) {
    return `${t}T12:00:00.000Z`;
  }
  return t;
}

export async function createDeliveryHandover(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    recipientName?: string;
    handoverType?: 'DELIVERY' | 'HANDOVER' | 'PICKUP';
    scheduledAt?: string;
    address?: string;
    note?: string;
    purchaseItemId?: string;
  }
): Promise<{ deliveryHandoverId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PURCHASE_ITEM_CREATE',
    resourceType: 'DELIVERY_HANDOVER',
    momentId,
  });
  const participantId = await resolveParticipantByDisplayName(client, momentId, body.recipientName);
  const noteParts = [
    body.recipientName ? `Recipient: ${body.recipientName}` : null,
    body.address ? `Address: ${body.address}` : null,
    body.note?.trim() || null,
  ].filter(Boolean);
  const r = await client.query<{ delivery_handover_id: string }>(
    `INSERT INTO collaboration.delivery_handover (
       moment_id, purchase_item_id, participant_id, handover_type, scheduled_at, status, note
     ) VALUES ($1, $2, $3, $4, $5::timestamptz, 'PLANNED', $6)
     RETURNING delivery_handover_id`,
    [
      momentId,
      body.purchaseItemId ?? null,
      participantId,
      body.handoverType ?? 'DELIVERY',
      normalizeScheduledAt(body.scheduledAt),
      noteParts.length ? noteParts.join('\n') : null,
    ]
  );
  return { deliveryHandoverId: r.rows[0]!.delivery_handover_id, momentId };
}

export async function createOwnershipRecord(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: {
    purchaseItemId?: string;
    toParticipantName?: string;
    fromOwnerName?: string;
    ownershipShare?: number;
    ownershipNote?: string;
    effectiveAt?: string;
  }
): Promise<{ ownershipRecordId: string; momentId: string }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'PURCHASE_ITEM_CREATE',
    resourceType: 'OWNERSHIP_RECORD',
    momentId,
  });
  let participantId = await resolveParticipantByDisplayName(client, momentId, body.toParticipantName);
  if (!participantId) {
    participantId = await actorParticipantId(client, momentId, ctx.userId);
  }
  if (!participantId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Could not resolve owner participant for transfer.', 400);
  }
  const noteParts = [
    body.fromOwnerName ? `From: ${body.fromOwnerName}` : null,
    body.toParticipantName ? `To: ${body.toParticipantName}` : null,
    body.effectiveAt ? `Effective: ${body.effectiveAt}` : null,
    body.ownershipNote?.trim() || null,
  ].filter(Boolean);
  const r = await client.query<{ ownership_record_id: string }>(
    `INSERT INTO collaboration.ownership_record (
       moment_id, purchase_item_id, participant_id, ownership_share, ownership_note, status
     ) VALUES ($1, $2, $3, $4, $5, 'ACTIVE')
     RETURNING ownership_record_id`,
    [
      momentId,
      body.purchaseItemId ?? null,
      participantId,
      body.ownershipShare ?? null,
      noteParts.length ? noteParts.join('\n') : null,
    ]
  );
  return { ownershipRecordId: r.rows[0]!.ownership_record_id, momentId };
}
