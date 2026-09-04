import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';

export type LeanMomentDomain = 'personal' | 'group' | 'business';

export interface LeanBusinessEventInput {
  eventName: string;
  eventId?: string;
  occurredAt?: Date | string;
  userId?: string | null;
  anonymousId?: string | null;
  sessionId?: string | null;
  momentId?: string | null;
  momentDomain?: LeanMomentDomain | null;
  momentCategory?: string | null;
  momentType?: string | null;
  actorRole?: string | null;
  platform?: 'ios' | 'android' | 'web' | null;
  appVersion?: string | null;
  sourceScreen?: string | null;
  correlationId?: string | null;
  properties?: Record<string, unknown>;
  ingestionSource?: string;
}

const MEANINGFUL = new Set([
  'moment_created',
  'participant_invited',
  'participant_joined',
  'moment_activity_completed',
  'expense_added',
  'contribution_recorded',
  'split_created',
  'memory_created',
  'moment_completed',
]);

/**
 * Non-blocking Lean product analytics write.
 * Business transaction must already succeed; analytics failure is swallowed.
 */
export async function emitLeanBusinessEvent(
  client: PoolClient,
  ctx: RequestContext | null,
  input: LeanBusinessEventInput
): Promise<void> {
  try {
    const eventId = input.eventId ?? randomUUID();
    const occurredAt = input.occurredAt ? new Date(input.occurredAt) : new Date();
    const userId = input.userId ?? ctx?.userId ?? null;
    const correlationId = input.correlationId ?? ctx?.correlationId ?? null;
    const properties = input.properties ?? {};

    await client.query(
      `INSERT INTO analytics_raw.events (
         event_id, event_name, event_version, occurred_at, received_at,
         user_id, anonymous_id, session_id, moment_id, moment_domain,
         moment_category, moment_type, actor_role, platform, app_version,
         source_screen, correlation_id, properties, ingestion_source, is_valid
       ) VALUES (
         $1,$2,1,$3,now(),
         $4,$5,$6,$7,$8,
         $9,$10,$11,$12,$13,
         $14,$15,$16::jsonb,$17,TRUE
       )
       ON CONFLICT (event_id) DO NOTHING`,
      [
        eventId,
        input.eventName,
        occurredAt.toISOString(),
        userId,
        input.anonymousId ?? null,
        input.sessionId ?? null,
        input.momentId ?? null,
        input.momentDomain ?? null,
        input.momentCategory ?? null,
        input.momentType ?? null,
        input.actorRole ?? null,
        input.platform ?? null,
        input.appVersion ?? null,
        input.sourceScreen ?? null,
        correlationId,
        JSON.stringify(properties),
        input.ingestionSource ?? 'backend',
      ]
    );

    await applyCoreFactUpserts(client, {
      ...input,
      eventId,
      occurredAt,
      userId,
      properties,
    });
  } catch (err) {
    console.warn('[lean-analytics] emit failed (non-blocking):', err);
  }
}

async function applyCoreFactUpserts(
  client: PoolClient,
  input: LeanBusinessEventInput & {
    eventId: string;
    occurredAt: Date;
    userId: string | null;
    properties: Record<string, unknown>;
  }
): Promise<void> {
  const { eventName, momentId, momentDomain, momentType, momentCategory, userId, occurredAt, properties } =
    input;
  if (!userId && !momentId) return;

  if (eventName === 'moment_created' && momentId && userId && momentDomain) {
    await client.query(
      `INSERT INTO analytics_core.moment_lifecycle_fact (
         moment_id, creator_user_id, moment_domain, moment_category, moment_type,
         created_at, activated_at, is_activated, participant_peak_count, current_analytics_state
       ) VALUES ($1,$2,$3,$4,$5,$6,$6,TRUE,1,'ACTIVE')
       ON CONFLICT (moment_id) DO NOTHING`,
      [momentId, userId, momentDomain, momentCategory ?? null, momentType ?? null, occurredAt.toISOString()]
    );
    await client.query(
      `INSERT INTO analytics_core.participant_fact (
         moment_id, user_id, joined_at, participant_role, is_active_participant
       ) VALUES ($1,$2,$3,'ORGANIZER',TRUE)
       ON CONFLICT (moment_id, user_id) DO UPDATE SET
         is_active_participant = TRUE,
         joined_at = COALESCE(analytics_core.participant_fact.joined_at, EXCLUDED.joined_at)`,
      [momentId, userId, occurredAt.toISOString()]
    );
    await upsertUserLifecycleOnCreate(client, userId, occurredAt);
  }

  if (eventName === 'participant_invited' && momentId && userId) {
    const inviteId = typeof properties.invite_id === 'string' ? properties.invite_id : null;
    await client.query(
      `INSERT INTO analytics_core.moment_daily (
         activity_date, moment_id, moment_domain, moment_type, creator_user_id,
         invites_sent, meaningful_action_count, active_flag, meaningfully_active_flag
       ) VALUES (($1::timestamptz AT TIME ZONE 'Asia/Kolkata')::date, $2, COALESCE($3,'group'), $4, $5, 1, 1, TRUE, TRUE)
       ON CONFLICT (activity_date, moment_id) DO UPDATE SET
         invites_sent = analytics_core.moment_daily.invites_sent + 1,
         meaningful_action_count = analytics_core.moment_daily.meaningful_action_count + 1,
         meaningfully_active_flag = TRUE,
         updated_at = now()`,
      [occurredAt.toISOString(), momentId, momentDomain, momentType ?? null, userId]
    );
    if (inviteId) {
      // Invitee user may be unknown at mint time — track inviter activity only.
      void inviteId;
    }
  }

  if (eventName === 'participant_joined' && momentId && userId) {
    const inviteId = typeof properties.invite_id === 'string' ? properties.invite_id : null;
    const role = typeof properties.participant_role === 'string' ? properties.participant_role : 'PARTICIPANT';
    await client.query(
      `INSERT INTO analytics_core.participant_fact (
         moment_id, user_id, invite_id, joined_at, participant_role, is_active_participant, was_existing_user
       ) VALUES ($1,$2,$3::uuid,$4,$5,TRUE,TRUE)
       ON CONFLICT (moment_id, user_id) DO UPDATE SET
         invite_id = COALESCE(EXCLUDED.invite_id, analytics_core.participant_fact.invite_id),
         joined_at = COALESCE(analytics_core.participant_fact.joined_at, EXCLUDED.joined_at),
         is_active_participant = TRUE`,
      [momentId, userId, inviteId, occurredAt.toISOString(), role]
    );
    await client.query(
      `UPDATE analytics_core.moment_lifecycle_fact
       SET participant_peak_count = GREATEST(participant_peak_count, (
             SELECT COUNT(*)::int FROM analytics_core.participant_fact
             WHERE moment_id = $1 AND is_active_participant
           )),
           updated_at = now()
       WHERE moment_id = $1`,
      [momentId]
    );
    await client.query(
      `INSERT INTO analytics_core.user_lifecycle_fact (user_id, registered_at, first_moment_joined_at, total_moments_joined)
       VALUES ($1, $2, $2, 1)
       ON CONFLICT (user_id) DO UPDATE SET
         first_moment_joined_at = COALESCE(analytics_core.user_lifecycle_fact.first_moment_joined_at, EXCLUDED.first_moment_joined_at),
         total_moments_joined = analytics_core.user_lifecycle_fact.total_moments_joined + 1,
         updated_at = now()`,
      [userId, occurredAt.toISOString()]
    );
  }

  if (eventName === 'invite_opened') {
    const inviteId = typeof properties.invite_id === 'string' ? properties.invite_id : null;
    if (inviteId && userId && momentId) {
      await client.query(
        `UPDATE analytics_core.participant_fact
         SET invite_opened_at = COALESCE(invite_opened_at, $3::timestamptz)
         WHERE moment_id = $1 AND user_id = $2`,
        [momentId, userId, occurredAt.toISOString()]
      );
    }
  }

  if (eventName === 'expense_added' && momentId && userId) {
    const amount = Number(properties.amount ?? 0);
    await client.query(
      `INSERT INTO analytics_core.moment_daily (
         activity_date, moment_id, moment_domain, moment_type, creator_user_id,
         expenses_added, expense_amount_total, meaningful_action_count,
         active_flag, meaningfully_active_flag
       ) VALUES (($1::timestamptz AT TIME ZONE 'Asia/Kolkata')::date, $2, COALESCE($3,'group'), $4, $5,
                 1, $6, 1, TRUE, TRUE)
       ON CONFLICT (activity_date, moment_id) DO UPDATE SET
         expenses_added = analytics_core.moment_daily.expenses_added + 1,
         expense_amount_total = analytics_core.moment_daily.expense_amount_total + EXCLUDED.expense_amount_total,
         meaningful_action_count = analytics_core.moment_daily.meaningful_action_count + 1,
         meaningfully_active_flag = TRUE,
         updated_at = now()`,
      [occurredAt.toISOString(), momentId, momentDomain, momentType ?? null, userId, Number.isFinite(amount) ? amount : 0]
    );
    await markParticipantMeaningful(client, momentId, userId, occurredAt);
  }

  if (MEANINGFUL.has(eventName) && userId) {
    await client.query(
      `INSERT INTO analytics_core.user_daily (
         activity_date, user_id, meaningful_action_count, active_flag, meaningfully_active_flag,
         moments_created, moments_joined, invites_sent, expenses_added, moments_completed
       ) VALUES (
         ($1::timestamptz AT TIME ZONE 'Asia/Kolkata')::date, $2, 1, TRUE, TRUE,
         CASE WHEN $3='moment_created' THEN 1 ELSE 0 END,
         CASE WHEN $3='participant_joined' THEN 1 ELSE 0 END,
         CASE WHEN $3='participant_invited' THEN 1 ELSE 0 END,
         CASE WHEN $3='expense_added' THEN 1 ELSE 0 END,
         CASE WHEN $3='moment_completed' THEN 1 ELSE 0 END
       )
       ON CONFLICT (activity_date, user_id) DO UPDATE SET
         meaningful_action_count = analytics_core.user_daily.meaningful_action_count + 1,
         moments_created = analytics_core.user_daily.moments_created + EXCLUDED.moments_created,
         moments_joined = analytics_core.user_daily.moments_joined + EXCLUDED.moments_joined,
         invites_sent = analytics_core.user_daily.invites_sent + EXCLUDED.invites_sent,
         expenses_added = analytics_core.user_daily.expenses_added + EXCLUDED.expenses_added,
         moments_completed = analytics_core.user_daily.moments_completed + EXCLUDED.moments_completed,
         meaningfully_active_flag = TRUE,
         active_flag = TRUE,
         updated_at = now()`,
      [occurredAt.toISOString(), userId, eventName]
    );
  }
}

async function upsertUserLifecycleOnCreate(
  client: PoolClient,
  userId: string,
  occurredAt: Date
): Promise<void> {
  await client.query(
    `INSERT INTO analytics_core.user_lifecycle_fact (
       user_id, registered_at, first_moment_created_at, first_meaningful_action_at,
       activated_at, total_moments_created
     ) VALUES ($1, $2, $2, $2, $2, 1)
     ON CONFLICT (user_id) DO UPDATE SET
       first_moment_created_at = COALESCE(analytics_core.user_lifecycle_fact.first_moment_created_at, EXCLUDED.first_moment_created_at),
       second_moment_created_at = CASE
         WHEN analytics_core.user_lifecycle_fact.first_moment_created_at IS NOT NULL
          AND analytics_core.user_lifecycle_fact.second_moment_created_at IS NULL
          AND analytics_core.user_lifecycle_fact.total_moments_created = 1
         THEN EXCLUDED.first_moment_created_at
         ELSE analytics_core.user_lifecycle_fact.second_moment_created_at
       END,
       third_moment_created_at = CASE
         WHEN analytics_core.user_lifecycle_fact.total_moments_created = 2
         THEN EXCLUDED.first_moment_created_at
         ELSE analytics_core.user_lifecycle_fact.third_moment_created_at
       END,
       first_meaningful_action_at = COALESCE(analytics_core.user_lifecycle_fact.first_meaningful_action_at, EXCLUDED.first_meaningful_action_at),
       activated_at = COALESCE(analytics_core.user_lifecycle_fact.activated_at, EXCLUDED.activated_at),
       total_moments_created = analytics_core.user_lifecycle_fact.total_moments_created + 1,
       repeat_creator_flag = (analytics_core.user_lifecycle_fact.total_moments_created + 1) >= 2,
       updated_at = now()`,
    [userId, occurredAt.toISOString()]
  );
}

async function markParticipantMeaningful(
  client: PoolClient,
  momentId: string,
  userId: string,
  occurredAt: Date
): Promise<void> {
  await client.query(
    `UPDATE analytics_core.participant_fact
     SET first_meaningful_action_at = COALESCE(first_meaningful_action_at, $3::timestamptz),
         participant_activated_at = COALESCE(participant_activated_at, $3::timestamptz)
     WHERE moment_id = $1 AND user_id = $2`,
    [momentId, userId, occurredAt.toISOString()]
  );
}

export async function loadMomentTaxonomy(
  client: PoolClient,
  momentId: string
): Promise<{ domain: LeanMomentDomain; category: string | null; type: string | null } | null> {
  const r = await client.query<{ domain_code: string; category_code: string | null; type_code: string | null }>(
    `SELECT lower(m.domain_code) AS domain_code, mc.code AS category_code, mt.code AS type_code
     FROM core.moment m
     JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     LEFT JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  const row = r.rows[0];
  if (!row) return null;
  const domain = row.domain_code as LeanMomentDomain;
  if (domain !== 'personal' && domain !== 'group' && domain !== 'business') return null;
  return { domain, category: row.category_code, type: row.type_code };
}
