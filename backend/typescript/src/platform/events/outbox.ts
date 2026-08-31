import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';

export interface DomainEventInput {
  eventName: string;
  domainCode: string;
  aggregateType: string;
  aggregateId: string;
  scopeType?: string;
  scopeId?: string;
  payload: Record<string, unknown>;
  idempotencyKey?: string;
}

export interface CommandSideEffectsInput extends DomainEventInput {
  auditActionCode: string;
  auditResourceType: string;
  auditResourceId: string;
  afterSnapshot: unknown;
  /** When set, also inserts one moment-scoped recent_activity row. */
  activity?: {
    domainCode: string;
    momentId: string;
    activityCode: string;
    title: string;
    payload: Record<string, unknown>;
  };
}

/** Event + outbox in one RTT (CTE). */
export async function insertDomainEventAndOutbox(
  client: PoolClient,
  ctx: RequestContext,
  input: DomainEventInput
): Promise<{ domainEventId: string; outboxEventId: string }> {
  const correlationId = ctx.correlationId || randomUUID();
  const result = await client.query<{ domain_event_id: string; outbox_event_id: string }>(
    `WITH ev AS (
       INSERT INTO events.domain_event (
         event_name, domain_code, aggregate_type, aggregate_id,
         scope_type, scope_id, actor_user_id, correlation_id,
         idempotency_key, payload
       ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10::jsonb)
       RETURNING domain_event_id
     ),
     ob AS (
       INSERT INTO events.outbox_event (domain_event_id, topic_code, partition_key)
       SELECT domain_event_id, $11, $12 FROM ev
       RETURNING outbox_event_id, domain_event_id
     )
     SELECT domain_event_id, outbox_event_id FROM ob`,
    [
      input.eventName,
      input.domainCode,
      input.aggregateType,
      input.aggregateId,
      input.scopeType ?? 'USER',
      input.scopeId ?? ctx.userId,
      ctx.userId,
      correlationId,
      input.idempotencyKey ?? null,
      JSON.stringify(input.payload),
      input.eventName.toUpperCase(),
      ctx.userId,
    ]
  );

  return {
    domainEventId: result.rows[0]!.domain_event_id,
    outboxEventId: result.rows[0]!.outbox_event_id,
  };
}

/**
 * Event + outbox + audit (+ optional activity) in one RTT.
 * Preserves transaction semantics — all statements run on the caller's client.
 */
export async function recordCommandSideEffects(
  client: PoolClient,
  ctx: RequestContext,
  input: CommandSideEffectsInput
): Promise<{ domainEventId: string; outboxEventId: string }> {
  const correlationId = ctx.correlationId || randomUUID();
  const hasActivity = Boolean(input.activity);
  const result = await client.query<{ domain_event_id: string; outbox_event_id: string }>(
    `WITH ev AS (
       INSERT INTO events.domain_event (
         event_name, domain_code, aggregate_type, aggregate_id,
         scope_type, scope_id, actor_user_id, correlation_id,
         idempotency_key, payload
       ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10::jsonb)
       RETURNING domain_event_id
     ),
     ob AS (
       INSERT INTO events.outbox_event (domain_event_id, topic_code, partition_key)
       SELECT domain_event_id, $11, $12 FROM ev
       RETURNING outbox_event_id, domain_event_id
     ),
     aud AS (
       INSERT INTO audit.audit_record (
         actor_type, actor_user_id, action_code, resource_type, resource_id,
         scope_type, scope_id, domain_event_id, correlation_id, outcome,
         after_snapshot, snapshot_schema_version
       )
       SELECT 'USER', $7, $13, $14, $15, 'USER', $7, domain_event_id, $8, 'SUCCEEDED', $16::jsonb, 1
       FROM ev
     ),
     act AS (
       INSERT INTO projection.recent_activity (
         user_id, source_event_id, domain_code, scope_type, scope_id,
         activity_code, title, occurred_at, activity_payload, projection_version
       )
       SELECT $7, domain_event_id, $17, 'MOMENT', $18::uuid, $19, $20, now(), $21::jsonb, 1
       FROM ev
       WHERE $22::boolean
       ON CONFLICT (user_id, source_event_id) DO NOTHING
     )
     SELECT domain_event_id, outbox_event_id FROM ob`,
    [
      input.eventName,
      input.domainCode,
      input.aggregateType,
      input.aggregateId,
      input.scopeType ?? 'USER',
      input.scopeId ?? ctx.userId,
      ctx.userId,
      correlationId,
      input.idempotencyKey ?? null,
      JSON.stringify(input.payload),
      input.eventName.toUpperCase(),
      ctx.userId,
      input.auditActionCode,
      input.auditResourceType,
      input.auditResourceId,
      JSON.stringify(input.afterSnapshot),
      input.activity?.domainCode ?? 'GROUP',
      input.activity?.momentId ?? ctx.userId,
      input.activity?.activityCode ?? '',
      input.activity?.title ?? '',
      JSON.stringify(input.activity?.payload ?? {}),
      hasActivity,
    ]
  );

  return {
    domainEventId: result.rows[0]!.domain_event_id,
    outboxEventId: result.rows[0]!.outbox_event_id,
  };
}

export async function insertAudit(
  client: PoolClient,
  ctx: RequestContext,
  actionCode: string,
  resourceType: string,
  resourceId: string,
  domainEventId: string,
  afterSnapshot: unknown
): Promise<void> {
  await client.query(
    `INSERT INTO audit.audit_record (
       actor_type, actor_user_id, action_code, resource_type, resource_id,
       scope_type, scope_id, domain_event_id, correlation_id, outcome,
       after_snapshot, snapshot_schema_version
     ) VALUES ('USER', $1, $2, $3, $4, 'USER', $1, $5, $6, 'SUCCEEDED', $7::jsonb, 1)`,
    [
      ctx.userId,
      actionCode,
      resourceType,
      resourceId,
      domainEventId,
      ctx.correlationId,
      JSON.stringify(afterSnapshot),
    ]
  );
}
