import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';
import { createExpense, createExpenseSchema } from './service';
import { createPersonalIncome, createPersonalIncomeSchema } from './personal-income';

export const createRecurringScheduleSchema = z
  .object({
    resourceKind: z.enum(['EXPENSE', 'INCOME']),
    templatePayload: z.record(z.unknown()),
    frequency: z.enum(['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']),
    intervalCount: z.number().int().positive().default(1),
    startDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    endDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  })
  .strict();

export const updateRecurringScheduleSchema = z
  .object({
    status: z.enum(['ACTIVE', 'PAUSED', 'COMPLETED', 'VOIDED']).optional(),
    endDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable().optional(),
    templatePayload: z.record(z.unknown()).optional(),
  })
  .strict()
  .refine((b) => Object.values(b).some((v) => v !== undefined), { message: 'At least one field is required.' });

export interface RecurringScheduleDto {
  recurringScheduleId: string;
  momentId: string;
  resourceKind: string;
  templatePayload: Record<string, unknown>;
  frequency: string;
  intervalCount: number;
  startDate: string;
  endDate: string | null;
  nextRunAt: string | null;
  status: string;
  version: number;
}

function computeNextRun(from: Date, frequency: string, interval: number): Date {
  const next = new Date(from);
  switch (frequency) {
    case 'DAILY':
      next.setUTCDate(next.getUTCDate() + interval);
      break;
    case 'WEEKLY':
      next.setUTCDate(next.getUTCDate() + interval * 7);
      break;
    case 'MONTHLY':
      next.setUTCMonth(next.getUTCMonth() + interval);
      break;
    case 'YEARLY':
      next.setUTCFullYear(next.getUTCFullYear() + interval);
      break;
  }
  return next;
}

export async function createRecurringSchedule(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createRecurringScheduleSchema>
): Promise<RecurringScheduleDto> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'EXPENSE_CREATE', resourceType: 'EXPENSE', momentId });
  const start = new Date(`${body.startDate}T00:00:00.000Z`);
  const nextRun = computeNextRun(start, body.frequency, body.intervalCount ?? 1);

  const inserted = await client.query<{
    recurring_schedule_id: string;
    version: string;
    next_run_at: Date | null;
  }>(
    `INSERT INTO finance.recurring_schedule (
       owner_user_id, moment_id, resource_kind, template_payload, frequency, interval_count,
       start_date, end_date, next_run_at, status
     ) VALUES ($1, $2, $3, $4::jsonb, $5, $6, $7::date, $8::date, $9, 'ACTIVE')
     RETURNING recurring_schedule_id, version, next_run_at`,
    [
      ctx.userId,
      momentId,
      body.resourceKind,
      JSON.stringify(body.templatePayload),
      body.frequency,
      body.intervalCount ?? 1,
      body.startDate,
      body.endDate ?? null,
      nextRun.toISOString(),
    ]
  );
  const row = inserted.rows[0]!;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RecurringScheduleCreated',
    domainCode: 'PERSONAL',
    aggregateType: 'RECURRING_SCHEDULE',
    aggregateId: row.recurring_schedule_id,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { recurringScheduleId: row.recurring_schedule_id },
  });
  await insertAudit(client, ctx, 'RECURRING_SCHEDULE_CREATE', 'RECURRING_SCHEDULE', row.recurring_schedule_id, domainEventId, {});

  return {
    recurringScheduleId: row.recurring_schedule_id,
    momentId,
    resourceKind: body.resourceKind,
    templatePayload: body.templatePayload,
    frequency: body.frequency,
    intervalCount: body.intervalCount ?? 1,
    startDate: body.startDate,
    endDate: body.endDate ?? null,
    nextRunAt: row.next_run_at?.toISOString() ?? null,
    status: 'ACTIVE',
    version: parseInt(row.version, 10),
  };
}

export async function listRecurringSchedules(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<RecurringScheduleDto[]> {
  const rows = await client.query<{
    recurring_schedule_id: string;
    resource_kind: string;
    template_payload: Record<string, unknown>;
    frequency: string;
    interval_count: number;
    start_date: Date;
    end_date: Date | null;
    next_run_at: Date | null;
    status: string;
    version: string;
  }>(
    `SELECT recurring_schedule_id, resource_kind, template_payload, frequency, interval_count,
            start_date, end_date, next_run_at, status, version::text AS version
     FROM finance.recurring_schedule
     WHERE owner_user_id = $1 AND moment_id = $2 AND status <> 'VOIDED'
     ORDER BY created_at DESC`,
    [ctx.userId, momentId]
  );
  return rows.rows.map((r) => ({
    recurringScheduleId: r.recurring_schedule_id,
    momentId,
    resourceKind: r.resource_kind,
    templatePayload: r.template_payload,
    frequency: r.frequency,
    intervalCount: r.interval_count,
    startDate: r.start_date.toISOString().slice(0, 10),
    endDate: r.end_date ? r.end_date.toISOString().slice(0, 10) : null,
    nextRunAt: r.next_run_at?.toISOString() ?? null,
    status: r.status,
    version: parseInt(r.version, 10),
  }));
}

export async function updateRecurringSchedule(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  scheduleId: string,
  body: z.infer<typeof updateRecurringScheduleSchema>
): Promise<RecurringScheduleDto> {
  const existing = await client.query<{ version: string }>(
    `SELECT version::text AS version FROM finance.recurring_schedule
     WHERE recurring_schedule_id = $1 AND owner_user_id = $2 AND moment_id = $3`,
    [scheduleId, ctx.userId, momentId]
  );
  if (!existing.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Recurring schedule not found.', 404);
  }

  const updated = await client.query<{
    recurring_schedule_id: string;
    resource_kind: string;
    template_payload: Record<string, unknown>;
    frequency: string;
    interval_count: number;
    start_date: Date;
    end_date: Date | null;
    next_run_at: Date | null;
    status: string;
    version: string;
  }>(
    `UPDATE finance.recurring_schedule SET
       status = COALESCE($4, status),
       end_date = CASE WHEN $5 IS NULL THEN end_date ELSE $5::date END,
       template_payload = COALESCE($6::jsonb, template_payload),
       version = version + 1,
       updated_at = now()
     WHERE recurring_schedule_id = $1 AND owner_user_id = $2 AND moment_id = $3
     RETURNING recurring_schedule_id, resource_kind, template_payload, frequency, interval_count,
               start_date, end_date, next_run_at, status, version::text AS version`,
    [
      scheduleId,
      ctx.userId,
      momentId,
      body.status ?? null,
      body.endDate === undefined ? null : body.endDate,
      body.templatePayload ? JSON.stringify(body.templatePayload) : null,
    ]
  );
  const r = updated.rows[0]!;
  return {
    recurringScheduleId: r.recurring_schedule_id,
    momentId,
    resourceKind: r.resource_kind,
    templatePayload: r.template_payload,
    frequency: r.frequency,
    intervalCount: r.interval_count,
    startDate: r.start_date.toISOString().slice(0, 10),
    endDate: r.end_date ? r.end_date.toISOString().slice(0, 10) : null,
    nextRunAt: r.next_run_at?.toISOString() ?? null,
    status: r.status,
    version: parseInt(r.version, 10),
  };
}

export async function generateRecurringInstance(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  scheduleId: string,
  idempotencyKey: string
): Promise<{ expenseId?: string; incomeId?: string; occurrenceDate: string }> {
  const schedule = await client.query<{
    resource_kind: string;
    template_payload: Record<string, unknown>;
    frequency: string;
    interval_count: number;
    next_run_at: Date | null;
    end_date: Date | null;
    status: string;
  }>(
    `SELECT resource_kind, template_payload, frequency, interval_count, next_run_at, end_date, status
     FROM finance.recurring_schedule
     WHERE recurring_schedule_id = $1 AND owner_user_id = $2 AND moment_id = $3`,
    [scheduleId, ctx.userId, momentId]
  );
  if (!schedule.rows[0] || schedule.rows[0].status !== 'ACTIVE') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Schedule not active.', 400);
  }
  const s = schedule.rows[0];
  const occurrenceDate = (s.next_run_at ?? new Date()).toISOString().slice(0, 10);

  const existingLink = await client.query(
    `SELECT 1 FROM finance.recurring_schedule_link WHERE idempotency_key = $1`,
    [idempotencyKey]
  );
  if (existingLink.rowCount) {
    throw new AppError(ErrorCode.IDEMPOTENCY_CONFLICT, 'Occurrence already generated.', 409);
  }

  const payload = s.template_payload;
  let expenseId: string | undefined;
  let incomeId: string | undefined;

  if (s.resource_kind === 'EXPENSE') {
    const parsed = createExpenseSchema.parse(payload);
    const result = await createExpense(client, ctx, momentId, parsed);
    expenseId = result.expenseId;
    await client.query(
      `UPDATE finance.expense SET recurring_schedule_id = $2 WHERE expense_id = $1`,
      [expenseId, scheduleId]
    );
  } else {
    const parsed = createPersonalIncomeSchema.parse(payload);
    const result = await createPersonalIncome(client, ctx, momentId, parsed);
    incomeId = result.incomeId;
  }

  await client.query(
    `INSERT INTO finance.recurring_schedule_link (
       recurring_schedule_id, expense_id, financial_movement_id, occurrence_date, idempotency_key
     ) VALUES ($1, $2, $3, $4::date, $5)`,
    [scheduleId, expenseId ?? null, incomeId ?? null, occurrenceDate, idempotencyKey]
  );

  const nextRun = s.next_run_at
    ? computeNextRun(s.next_run_at, s.frequency, s.interval_count)
    : null;
  const completed =
    s.end_date && nextRun && nextRun.toISOString().slice(0, 10) > s.end_date.toISOString().slice(0, 10);

  await client.query(
    `UPDATE finance.recurring_schedule SET
       next_run_at = $2,
       status = CASE WHEN $3 THEN 'COMPLETED' ELSE status END,
       version = version + 1,
       updated_at = now()
     WHERE recurring_schedule_id = $1`,
    [scheduleId, completed ? null : nextRun?.toISOString() ?? null, completed]
  );

  return { expenseId, incomeId, occurrenceDate };
}
