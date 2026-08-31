import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { z } from 'zod';

/** Relationships precision (PX-3) — PER-RE widgets; bond axes from activity_type counts only. */

const ACTIVITY_KINDS = ['CONNECTION', 'SUPPORT', 'SHARED_EXPERIENCE', 'INVESTMENT', 'INTERACTION'] as const;

const ACTIVITY_TYPE_MAP: Record<(typeof ACTIVITY_KINDS)[number], string> = {
  CONNECTION: 'INTERACTION',
  INTERACTION: 'INTERACTION',
  SUPPORT: 'SUPPORT',
  SHARED_EXPERIENCE: 'SHARED_EXPERIENCE',
  INVESTMENT: 'RELATIONSHIP_INVESTMENT',
};

async function assertPersonalMoment(client: PoolClient, ctx: RequestContext, momentId: string): Promise<void> {
  const row = await client.query(
    `SELECT 1 FROM personal.personal_moment_context WHERE moment_id = $1 AND user_id = $2`,
    [momentId, ctx.userId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Personal moment not found.', 404);
  }
}

function clampScore(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function scoreFromCount(count: number): number | null {
  if (count <= 0) return null;
  return clampScore(40 + count * 12);
}

export const relationshipsProfileSchema = z
  .object({
    bondFocusCode: z.string().max(80).optional(),
    connectionStyleCode: z.string().max(80).optional(),
    carePriorityCode: z.string().max(80).optional(),
    presenceRhythmCode: z.string().max(80).optional(),
    supportPreferenceCode: z.string().max(80).optional(),
    investmentStanceCode: z.string().max(80).optional(),
  })
  .strict();

export const relationshipActivityPrecisionSchema = z
  .object({
    activityKind: z.enum(ACTIVITY_KINDS),
    displayName: z.string().min(1).max(200),
    note: z.string().max(2000).optional(),
    occurredAt: z.string().datetime().optional(),
    relationshipType: z.string().max(80).optional(),
    investmentValue: z.number().optional(),
    unitCode: z.string().max(50).optional(),
  })
  .strict();

export async function upsertRelationshipsProfile(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof relationshipsProfileSchema>
): Promise<{ momentId: string; updatedAt: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  await client.query(
    `INSERT INTO personal.relationships_profile (
       moment_id, user_id, bond_focus_code, connection_style_code, care_priority_code,
       presence_rhythm_code, support_preference_code, investment_stance_code
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
     ON CONFLICT (moment_id) DO UPDATE SET
       bond_focus_code = COALESCE(EXCLUDED.bond_focus_code, personal.relationships_profile.bond_focus_code),
       connection_style_code = COALESCE(EXCLUDED.connection_style_code, personal.relationships_profile.connection_style_code),
       care_priority_code = COALESCE(EXCLUDED.care_priority_code, personal.relationships_profile.care_priority_code),
       presence_rhythm_code = COALESCE(EXCLUDED.presence_rhythm_code, personal.relationships_profile.presence_rhythm_code),
       support_preference_code = COALESCE(EXCLUDED.support_preference_code, personal.relationships_profile.support_preference_code),
       investment_stance_code = COALESCE(EXCLUDED.investment_stance_code, personal.relationships_profile.investment_stance_code),
       version = personal.relationships_profile.version + 1,
       updated_at = now()`,
    [
      momentId,
      ctx.userId,
      body.bondFocusCode ?? null,
      body.connectionStyleCode ?? null,
      body.carePriorityCode ?? null,
      body.presenceRhythmCode ?? null,
      body.supportPreferenceCode ?? null,
      body.investmentStanceCode ?? null,
    ]
  );
  await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RelationshipsProfileUpserted',
    domainCode: 'PERSONAL',
    aggregateType: 'RELATIONSHIPS_PROFILE',
    aggregateId: momentId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { momentId, ...body },
  });
  return { momentId, updatedAt: new Date().toISOString() };
}

async function ensureExternalConnection(
  client: PoolClient,
  ctx: RequestContext,
  displayName: string,
  relationshipType: string | null
): Promise<string> {
  const existing = await client.query<{ relationship_connection_id: string }>(
    `SELECT relationship_connection_id FROM personal.relationship_connection
     WHERE user_id = $1 AND lower(display_name) = lower($2) AND status = 'ACTIVE'
     LIMIT 1`,
    [ctx.userId, displayName]
  );
  if (existing.rows[0]) return existing.rows[0].relationship_connection_id;

  const party = await client.query<{ external_party_id: string }>(
    `INSERT INTO core.external_party (party_type, display_name, status)
     VALUES ('PERSON', $1, 'ACTIVE')
     RETURNING external_party_id`,
    [displayName]
  );
  const conn = await client.query<{ relationship_connection_id: string }>(
    `INSERT INTO personal.relationship_connection (
       user_id, external_party_id, display_name, relationship_type, status
     ) VALUES ($1, $2, $3, COALESCE($4, 'PERSON'), 'ACTIVE')
     RETURNING relationship_connection_id`,
    [ctx.userId, party.rows[0]!.external_party_id, displayName, relationshipType]
  );
  return conn.rows[0]!.relationship_connection_id;
}

export async function recordRelationshipActivityPrecision(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof relationshipActivityPrecisionSchema>
): Promise<{ activityId: string; connectionId: string; activityKind: string }> {
  await assertPersonalMoment(client, ctx, momentId);
  const connectionId = await ensureExternalConnection(
    client,
    ctx,
    body.displayName,
    body.relationshipType ?? null
  );

  const activityType = ACTIVITY_TYPE_MAP[body.activityKind];
  const inserted = await client.query<{ relationship_activity_id: string }>(
    `INSERT INTO personal.relationship_activity (
       moment_id, user_id, relationship_connection_id, activity_type, occurred_at,
       title, note, investment_value, unit_code, status
     ) VALUES ($1,$2,$3,$4,COALESCE($5::timestamptz, now()),$6,$7,$8,$9,'ACTIVE')
     RETURNING relationship_activity_id`,
    [
      momentId,
      ctx.userId,
      connectionId,
      activityType,
      body.occurredAt ?? null,
      body.displayName,
      body.note ?? null,
      body.investmentValue ?? null,
      body.unitCode ?? null,
    ]
  );
  const activityId = inserted.rows[0]!.relationship_activity_id;
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'RelationshipActivityRecorded',
    domainCode: 'PERSONAL',
    aggregateType: 'RELATIONSHIP_ACTIVITY',
    aggregateId: activityId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { activityId, momentId, activityKind: body.activityKind, connectionId },
  });
  await client.query(
    `INSERT INTO projection.recent_activity (
       user_id, source_event_id, domain_code, scope_type, scope_id,
       activity_code, title, occurred_at, activity_payload, projection_version
     ) VALUES ($1,$2,'PERSONAL','MOMENT',$3,$4,$5,now(),$6::jsonb,1)
     ON CONFLICT (user_id, source_event_id) DO NOTHING`,
    [
      ctx.userId,
      domainEventId,
      momentId,
      `RELATIONSHIP_${body.activityKind}`,
      body.displayName,
      JSON.stringify({ activityId, activityKind: body.activityKind }),
    ]
  );
  await refreshRelationshipsBondAxes(client, ctx.userId, momentId, domainEventId);
  return { activityId, connectionId, activityKind: body.activityKind };
}

async function computeBondAxes(client: PoolClient, userId: string, momentId: string) {
  const row = await client.query<{
    interaction: string;
    support: string;
    shared: string;
    investment: string;
  }>(
    `SELECT
       COUNT(*) FILTER (WHERE activity_type='INTERACTION')::text AS interaction,
       COUNT(*) FILTER (WHERE activity_type='SUPPORT')::text AS support,
       COUNT(*) FILTER (WHERE activity_type='SHARED_EXPERIENCE')::text AS shared,
       COUNT(*) FILTER (WHERE activity_type='RELATIONSHIP_INVESTMENT')::text AS investment
     FROM personal.relationship_activity
     WHERE moment_id=$1 AND user_id=$2 AND status='ACTIVE'`,
    [momentId, userId]
  );
  const c = row.rows[0]!;
  const interaction = parseInt(c.interaction, 10);
  const support = parseInt(c.support, 10);
  const shared = parseInt(c.shared, 10);
  const investment = parseInt(c.investment, 10);
  const trustScore = scoreFromCount(interaction);
  const careScore = scoreFromCount(support);
  const supportScore = scoreFromCount(support + investment);
  const presenceScore = scoreFromCount(shared);
  const parts = [trustScore, careScore, supportScore, presenceScore].filter((n): n is number => n != null);
  const bondIndex =
    parts.length > 0 ? clampScore(parts.reduce((a, b) => a + b, 0) / parts.length) : null;
  return {
    trustScore,
    careScore,
    supportScore,
    presenceScore,
    bondIndex,
    counts: { interaction, support, shared, investment },
  };
}

export async function refreshRelationshipsBondAxes(
  client: PoolClient,
  userId: string,
  momentId: string,
  sourceEventId: string
): Promise<void> {
  const axes = await computeBondAxes(client, userId, momentId);
  const existing = await client.query<{
    attention_count: number;
    widget_payload: Record<string, unknown> | null;
  }>(`SELECT attention_count, widget_payload FROM projection.personal_pulse WHERE user_id = $1 FOR UPDATE`, [
    userId,
  ]);
  const payload = { ...(existing.rows[0]?.widget_payload ?? {}) };
  if (axes.trustScore != null) payload.trustScore = axes.trustScore;
  if (axes.careScore != null) payload.careScore = axes.careScore;
  if (axes.supportScore != null) payload.supportScore = axes.supportScore;
  if (axes.presenceScore != null) payload.presenceScore = axes.presenceScore;
  if (axes.bondIndex != null) payload.bondIndex = axes.bondIndex;
  payload.relationshipsAxisSource = 'PRECISION_V046';
  payload.lastRelationshipRefreshAt = new Date().toISOString();

  const attention = (existing.rows[0]?.attention_count ?? 0) + 1;

  if (existing.rows[0]) {
    await client.query(
      `UPDATE projection.personal_pulse SET
         attention_count = $2,
         wellbeing_score = COALESCE($3, wellbeing_score),
         widget_payload = $4::jsonb,
         source_event_id = $5,
         projection_version = projection_version + 1,
         updated_at = now()
       WHERE user_id = $1`,
      [userId, attention, axes.bondIndex, JSON.stringify(payload), sourceEventId]
    );
  } else {
    await client.query(
      `INSERT INTO projection.personal_pulse (
         user_id, attention_count, recovery_score, mood_state, rhythm_score, wellbeing_score,
         widget_payload, source_event_id, projection_version
       ) VALUES ($1, $2, NULL, NULL, NULL, $3, $4::jsonb, $5, 1)`,
      [userId, attention, axes.bondIndex, JSON.stringify(payload), sourceEventId]
    );
  }
}

export async function getRelationshipsRuntimeSummary(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const conn = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM personal.relationship_connection WHERE user_id=$1 AND status='ACTIVE'`,
    [ctx.userId]
  );
  const act = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM personal.relationship_activity
     WHERE moment_id=$1 AND user_id=$2 AND status='ACTIVE'`,
    [momentId, ctx.userId]
  );
  return {
    momentId,
    familyCode: 'RELATIONSHIPS',
    connectionCount: parseInt(conn.rows[0]?.n ?? '0', 10),
    activityCount: parseInt(act.rows[0]?.n ?? '0', 10),
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getRelationshipsBondSnapshot(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const axes = await computeBondAxes(client, ctx.userId, momentId);
  return {
    momentId,
    familyCode: 'RELATIONSHIPS',
    trustScore: axes.trustScore,
    careScore: axes.careScore,
    supportScore: axes.supportScore,
    presenceScore: axes.presenceScore,
    bondIndex: axes.bondIndex,
    source: 'CANONICAL_COUNTS',
    counts: axes.counts,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getRelationshipsConnections(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const rows = await client.query(
    `SELECT c.relationship_connection_id AS id, c.display_name, c.relationship_type, c.status,
            c.updated_at,
            (SELECT COUNT(*)::int FROM personal.relationship_activity a
              WHERE a.relationship_connection_id = c.relationship_connection_id
                AND a.moment_id = $2 AND a.status = 'ACTIVE') AS activity_count
     FROM personal.relationship_connection c
     WHERE c.user_id = $1 AND c.status = 'ACTIVE'
     ORDER BY c.updated_at DESC
     LIMIT 50`,
    [ctx.userId, momentId]
  );
  return {
    momentId,
    familyCode: 'RELATIONSHIPS',
    connections: rows.rows,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}

export async function getRelationshipsJourney(client: PoolClient, ctx: RequestContext, momentId: string) {
  await assertPersonalMoment(client, ctx, momentId);
  const rows = await client.query(
    `SELECT a.relationship_activity_id AS id, a.activity_type, a.title, a.note, a.occurred_at,
            a.investment_value, a.unit_code, c.display_name
     FROM personal.relationship_activity a
     JOIN personal.relationship_connection c
       ON c.relationship_connection_id = a.relationship_connection_id
     WHERE a.moment_id=$1 AND a.user_id=$2 AND a.status='ACTIVE'
     ORDER BY a.occurred_at DESC
     LIMIT 40`,
    [momentId, ctx.userId]
  );
  return {
    momentId,
    familyCode: 'RELATIONSHIPS',
    events: rows.rows,
    projectionVersion: 1,
    updatedAt: new Date().toISOString(),
  };
}
