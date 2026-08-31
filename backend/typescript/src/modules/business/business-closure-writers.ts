/**
 * Business Deployment Closure — Wave 1 writers.
 * Follows operations-precision.ts patterns: assertCompanyMomentAccess,
 * assertGovernanceAllowed, INSERT, event/outbox, recent_activity, projections.
 */
import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { assertGovernanceAllowed } from '../governance/resolver';
import { assertCompanyMomentAccess } from './membership';

/* ------------------------------------------------------------------ */
/*  Inline recent_activity bump (same pattern as ops-precision)       */
/* ------------------------------------------------------------------ */
async function bumpRecentActivity(
  client: PoolClient,
  ctx: RequestContext,
  opts: {
    momentId: string;
    companyId: string;
    domainEventId: string;
    activityCode: string;
    title: string;
    payload: Record<string, unknown>;
  }
): Promise<void> {
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1, $2, 'BUSINESS', 'MOMENT', $3, $4, $5, now(), $6::jsonb, 1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      opts.domainEventId,
      opts.momentId,
      opts.activityCode,
      opts.title,
      JSON.stringify({ companyId: opts.companyId, ...opts.payload }),
    ]
  );
}

/* ------------------------------------------------------------------ */
/*  Schemas                                                           */
/* ------------------------------------------------------------------ */
export const createTaxObligationSchema = z
  .object({
    title: z.string().min(1).max(500),
    taxType: z.string().max(100).optional(),
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).optional(),
    dueDate: z.string().date().optional(),
    notes: z.string().max(5000).optional(),
  })
  .strict();

export const createForecastScenarioSchema = z
  .object({
    name: z.string().min(1).max(300),
    horizonMonths: z.number().int().min(1).max(120).optional(),
    assumptions: z.string().max(5000).optional(),
    lines: z
      .array(
        z.object({
          lineLabel: z.string().min(1).max(300),
          amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
          currencyCode: z.string().length(3).optional(),
          periodLabel: z.string().max(100).optional(),
        }).strict()
      )
      .max(200)
      .optional(),
  })
  .strict();

export const createInvestorUpdateSchema = z
  .object({
    updateType: z.string().max(100).optional(),
    subject: z.string().min(1).max(500),
    keyMetrics: z.string().max(5000).optional(),
    runwayStatus: z.string().max(1000).optional(),
    highlights: z.string().max(5000).optional(),
    nextSteps: z.string().max(5000).optional(),
  })
  .strict();

export const createBudgetAlertSchema = z
  .object({
    title: z.string().min(1).max(500),
    metricLabel: z.string().max(200).optional(),
    thresholdValue: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    currencyCode: z.string().length(3).optional(),
    severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
    note: z.string().max(5000).optional(),
  })
  .strict();

export const createBudgetReviewSchema = z
  .object({
    period: z.enum(['WEEKLY', 'MONTHLY', 'QUARTERLY', 'OTHER']).optional(),
    summary: z.string().min(1).max(5000),
    outcome: z.string().max(5000).optional(),
  })
  .strict();

export const createDecisionSchema = z
  .object({
    title: z.string().min(1).max(500),
    decisionText: z.string().min(1).max(5000),
    rationale: z.string().max(5000).optional(),
  })
  .strict();

export const createMeetingRecordSchema = z
  .object({
    title: z.string().min(1).max(500),
    meetingAt: z.string().datetime().optional(),
    attendeesText: z.string().max(5000).optional(),
    notes: z.string().max(8000).optional(),
    decisionsText: z.string().max(5000).optional(),
  })
  .strict();

export const createRecognitionSchema = z
  .object({
    recipientName: z.string().min(1).max(300),
    recognitionType: z.string().max(100).optional(),
    whyText: z.string().min(1).max(3000),
  })
  .strict();

export const createRetrospectiveSchema = z
  .object({
    wentWell: z.string().max(5000).optional(),
    improveNext: z.string().max(5000).optional(),
  })
  .strict()
  .refine((b) => b.wentWell != null || b.improveNext != null, {
    message: 'At least one of wentWell or improveNext is required.',
  });

export const createActivityLogEntrySchema = z
  .object({
    title: z.string().min(1).max(500),
    ownerLabel: z.string().max(200).optional(),
    categoryCode: z.string().max(80).optional(),
  })
  .strict();

/* ------------------------------------------------------------------ */
/*  Writers                                                           */
/* ------------------------------------------------------------------ */

export async function createTaxObligation(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createTaxObligationSchema>
): Promise<{ taxObligationId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'TAX_OBLIGATION_CREATE',
    resourceType: 'TAX_OBLIGATION',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ tax_obligation_id: string }>(
    `INSERT INTO finance.tax_obligation (
       company_id, moment_id, title, tax_type, amount, currency_code, due_date, notes,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,$5::numeric,$6,$7::date,$8,'OPEN',$9,1)
     RETURNING tax_obligation_id`,
    [
      scope.companyId, momentId, body.title, body.taxType ?? null,
      body.amount ?? null, body.currencyCode ?? null, body.dueDate ?? null,
      body.notes ?? null, ctx.userId,
    ]
  );
  const taxObligationId = r.rows[0]!.tax_obligation_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'TaxObligationCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'TAX_OBLIGATION',
    aggregateId: taxObligationId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { taxObligationId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'TAX_OBLIGATION_CREATED', title: body.title,
    payload: { taxObligationId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: taxObligationId, eventType: 'TAX_OBLIGATION', title: body.title,
    category: 'Finance', description: body.taxType ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { taxObligationId, momentId };
}

export async function createForecastScenario(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createForecastScenarioSchema>
): Promise<{ forecastScenarioId: string; momentId: string; lineCount: number }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'FORECAST_CREATE',
    resourceType: 'FORECAST',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ forecast_scenario_id: string }>(
    `INSERT INTO finance.forecast_scenario (
       company_id, moment_id, name, horizon_months, assumptions,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,$5,'DRAFT',$6,1)
     RETURNING forecast_scenario_id`,
    [
      scope.companyId, momentId, body.name,
      body.horizonMonths ?? null, body.assumptions ?? null, ctx.userId,
    ]
  );
  const forecastScenarioId = r.rows[0]!.forecast_scenario_id;
  const lines = body.lines ?? [];
  for (let i = 0; i < lines.length; i++) {
    const ln = lines[i]!;
    await client.query(
      `INSERT INTO finance.forecast_line (
         forecast_scenario_id, company_id, moment_id, line_label, amount, currency_code, period_label, sort_order, version
       ) VALUES ($1,$2,$3,$4,$5::numeric,$6,$7,$8,1)`,
      [
        forecastScenarioId, scope.companyId, momentId, ln.lineLabel,
        ln.amount, ln.currencyCode ?? null, ln.periodLabel ?? null, i,
      ]
    );
  }
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ForecastScenarioCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'FORECAST',
    aggregateId: forecastScenarioId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { forecastScenarioId, momentId, name: body.name, lineCount: lines.length },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'FORECAST_CREATED', title: body.name,
    payload: { forecastScenarioId, lineCount: lines.length },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: forecastScenarioId, eventType: 'FORECAST', title: body.name,
    category: 'Finance', description: body.assumptions ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { forecastScenarioId, momentId, lineCount: lines.length };
}

export async function createInvestorUpdate(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createInvestorUpdateSchema>
): Promise<{ investorUpdateId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'INVESTOR_UPDATE_CREATE',
    resourceType: 'INVESTOR_UPDATE',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ investor_update_id: string }>(
    `INSERT INTO business.investor_update (
       company_id, moment_id, update_type, subject, key_metrics, runway_status,
       highlights, next_steps, status, author_user_id, version
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,'DRAFT',$9,1)
     RETURNING investor_update_id`,
    [
      scope.companyId, momentId, body.updateType ?? null, body.subject,
      body.keyMetrics ?? null, body.runwayStatus ?? null,
      body.highlights ?? null, body.nextSteps ?? null, ctx.userId,
    ]
  );
  const investorUpdateId = r.rows[0]!.investor_update_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'InvestorUpdateCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'INVESTOR_UPDATE',
    aggregateId: investorUpdateId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { investorUpdateId, momentId, subject: body.subject },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'INVESTOR_UPDATE_CREATED', title: body.subject,
    payload: { investorUpdateId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: investorUpdateId, eventType: 'INVESTOR_UPDATE', title: body.subject,
    category: 'Updates', description: body.highlights ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { investorUpdateId, momentId };
}

export async function createBudgetAlert(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBudgetAlertSchema>
): Promise<{ budgetAlertId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'BUDGET_ALERT_CREATE',
    resourceType: 'BUDGET_ALERT',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ budget_alert_id: string }>(
    `INSERT INTO business.budget_alert (
       company_id, moment_id, title, metric_label, threshold_value, currency_code,
       severity, note, status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,$5::numeric,$6,$7,$8,'OPEN',$9,1)
     RETURNING budget_alert_id`,
    [
      scope.companyId, momentId, body.title, body.metricLabel ?? null,
      body.thresholdValue ?? null, body.currencyCode ?? null,
      body.severity ?? 'MEDIUM', body.note ?? null, ctx.userId,
    ]
  );
  const budgetAlertId = r.rows[0]!.budget_alert_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BudgetAlertCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'BUDGET_ALERT',
    aggregateId: budgetAlertId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { budgetAlertId, momentId, title: body.title, severity: body.severity ?? 'MEDIUM' },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'BUDGET_ALERT_CREATED', title: body.title,
    payload: { budgetAlertId, severity: body.severity ?? 'MEDIUM' },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: budgetAlertId, eventType: 'BUDGET_ALERT', title: body.title,
    category: 'Finance', description: body.note ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { budgetAlertId, momentId };
}

export async function createBudgetReview(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createBudgetReviewSchema>
): Promise<{ businessReviewId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'REVIEW_CREATE',
    resourceType: 'REVIEW',
    companyId: scope.companyId,
    momentId,
  });
  const reviewType = body.period ?? 'OTHER';
  const r = await client.query<{ business_review_id: string }>(
    `INSERT INTO business.business_review (
       company_id, moment_id, review_type, review_date, summary, outcome,
       status, created_by_user_id
     ) VALUES ($1,$2,$3,CURRENT_DATE,$4,$5,'COMPLETED',$6)
     RETURNING business_review_id`,
    [
      scope.companyId, momentId, reviewType,
      body.summary, body.outcome ?? null, ctx.userId,
    ]
  );
  const businessReviewId = r.rows[0]!.business_review_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'BusinessReviewCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'BUSINESS_REVIEW',
    aggregateId: businessReviewId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { businessReviewId, momentId, reviewType },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'BUSINESS_REVIEW_CREATED', title: `${reviewType} review`,
    payload: { businessReviewId, reviewType },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: businessReviewId, eventType: 'REVIEW', title: `${reviewType} review`,
    category: 'Reviews', description: body.summary,
    occurredAt: new Date().toISOString(),
  });
  return { businessReviewId, momentId };
}

export async function createDecision(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createDecisionSchema>
): Promise<{ decisionId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'DECISION_RECORD',
    resourceType: 'DECISION',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ decision_id: string }>(
    `INSERT INTO business.decision (
       company_id, moment_id, title, decision_text, rationale,
       decided_by_user_id, decided_at, status, version
     ) VALUES ($1,$2,$3,$4,$5,$6,now(),'ACTIVE',1)
     RETURNING decision_id`,
    [
      scope.companyId, momentId, body.title, body.decisionText,
      body.rationale ?? null, ctx.userId,
    ]
  );
  const decisionId = r.rows[0]!.decision_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'DecisionRecorded',
    domainCode: 'BUSINESS',
    aggregateType: 'DECISION',
    aggregateId: decisionId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { decisionId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'DECISION_RECORDED', title: body.title,
    payload: { decisionId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: decisionId, eventType: 'DECISION', title: body.title,
    category: 'Decisions', description: body.decisionText,
    occurredAt: new Date().toISOString(),
  });
  return { decisionId, momentId };
}

export async function createMeetingRecord(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createMeetingRecordSchema>
): Promise<{ meetingRecordId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'MEETING_CREATE',
    resourceType: 'MEETING',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ meeting_record_id: string }>(
    `INSERT INTO business.meeting_record (
       company_id, moment_id, title, meeting_at, attendees_text, notes, decisions_text,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4::timestamptz,$5,$6,$7,'LOGGED',$8,1)
     RETURNING meeting_record_id`,
    [
      scope.companyId, momentId, body.title, body.meetingAt ?? null,
      body.attendeesText ?? null, body.notes ?? null, body.decisionsText ?? null,
      ctx.userId,
    ]
  );
  const meetingRecordId = r.rows[0]!.meeting_record_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'MeetingRecordCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'MEETING',
    aggregateId: meetingRecordId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { meetingRecordId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'MEETING_LOGGED', title: body.title,
    payload: { meetingRecordId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: meetingRecordId, eventType: 'MEETING', title: body.title,
    category: 'Meetings', description: body.notes ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { meetingRecordId, momentId };
}

export async function createRecognition(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createRecognitionSchema>
): Promise<{ recognitionId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'RECOGNITION_CREATE',
    resourceType: 'RECOGNITION',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ recognition_id: string }>(
    `INSERT INTO business.recognition (
       company_id, moment_id, recipient_name, recognition_type, why_text,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,$5,'PUBLISHED',$6,1)
     RETURNING recognition_id`,
    [
      scope.companyId, momentId, body.recipientName,
      body.recognitionType ?? null, body.whyText, ctx.userId,
    ]
  );
  const recognitionId = r.rows[0]!.recognition_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RecognitionCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'RECOGNITION',
    aggregateId: recognitionId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { recognitionId, momentId, recipientName: body.recipientName },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'RECOGNITION_GIVEN', title: `Kudos: ${body.recipientName}`,
    payload: { recognitionId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: recognitionId, eventType: 'RECOGNITION', title: `Kudos: ${body.recipientName}`,
    category: 'Team', description: body.whyText,
    occurredAt: new Date().toISOString(),
  });
  return { recognitionId, momentId };
}

export async function createRetrospective(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createRetrospectiveSchema>
): Promise<{ retrospectiveId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'RETRO_CREATE',
    resourceType: 'RETROSPECTIVE',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ retrospective_id: string }>(
    `INSERT INTO business.retrospective (
       company_id, moment_id, went_well, improve_next,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,'LOGGED',$5,1)
     RETURNING retrospective_id`,
    [
      scope.companyId, momentId,
      body.wentWell ?? null, body.improveNext ?? null, ctx.userId,
    ]
  );
  const retrospectiveId = r.rows[0]!.retrospective_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RetrospectiveCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'RETROSPECTIVE',
    aggregateId: retrospectiveId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { retrospectiveId, momentId },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'RETRO_LOGGED', title: 'Retrospective',
    payload: { retrospectiveId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: retrospectiveId, eventType: 'RETROSPECTIVE', title: 'Retrospective',
    category: 'Reviews', description: body.wentWell ?? body.improveNext ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { retrospectiveId, momentId };
}

export async function createActivityLogEntry(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createActivityLogEntrySchema>
): Promise<{ activityLogEntryId: string; momentId: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'ACTIVITY_LOG_CREATE',
    resourceType: 'ACTIVITY_LOG',
    companyId: scope.companyId,
    momentId,
  });
  const r = await client.query<{ activity_log_entry_id: string }>(
    `INSERT INTO business.activity_log_entry (
       company_id, moment_id, title, owner_label, category_code,
       status, created_by_user_id, version
     ) VALUES ($1,$2,$3,$4,$5,'LOGGED',$6,1)
     RETURNING activity_log_entry_id`,
    [
      scope.companyId, momentId, body.title,
      body.ownerLabel ?? null, body.categoryCode ?? null, ctx.userId,
    ]
  );
  const activityLogEntryId = r.rows[0]!.activity_log_entry_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'ActivityLogEntryCreated',
    domainCode: 'BUSINESS',
    aggregateType: 'ACTIVITY_LOG',
    aggregateId: activityLogEntryId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { activityLogEntryId, momentId, title: body.title },
  });
  await bumpRecentActivity(client, ctx, {
    momentId, companyId: scope.companyId, domainEventId,
    activityCode: 'ACTIVITY_LOGGED', title: body.title,
    payload: { activityLogEntryId },
  });
  const { refreshBusinessProjectionsAfterWrite, appendBusinessMomentEvent } = await import('./business-projection');
  await refreshBusinessProjectionsAfterWrite(client, scope.companyId, momentId, scope.businessFamily);
  await appendBusinessMomentEvent(client, scope.companyId, momentId, {
    eventId: activityLogEntryId, eventType: 'ACTIVITY_LOG', title: body.title,
    category: 'Updates', description: body.categoryCode ?? undefined,
    occurredAt: new Date().toISOString(),
  });
  return { activityLogEntryId, momentId };
}
