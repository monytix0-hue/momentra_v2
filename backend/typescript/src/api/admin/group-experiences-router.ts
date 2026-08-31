import { Router } from 'express';
import { getPool } from '../../platform/database/pool';

export const adminGroupExperiencesRouter = Router();

adminGroupExperiencesRouter.get('/', async (_req, res, next) => {
  try {
    const pool = getPool();
    const result = await pool.query<{
      moment_id: string;
      title: string;
      status: string;
      moment_type_code: string;
      experience_kind: string | null;
      start_at: string | null;
      end_at: string | null;
      organizer_user_id: string;
      organizer_name: string | null;
      participant_count: string;
      created_at: string;
    }>(
      `SELECT m.moment_id,
              m.title,
              m.status,
              mt.code AS moment_type_code,
              sec.experience_kind,
              COALESCE(sec.start_at, m.start_at) AS start_at,
              COALESCE(sec.end_at, m.end_at) AS end_at,
              gmc.organizer_user_id,
              up.display_name AS organizer_name,
              (
                SELECT COUNT(*)::text
                FROM collaboration.moment_participant mp
                WHERE mp.moment_id = m.moment_id
                  AND mp.status IN ('INVITED', 'ACTIVE')
              ) AS participant_count,
              m.created_at
       FROM core.moment m
       JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       LEFT JOIN collaboration.shared_experience_context sec ON sec.moment_id = m.moment_id
       LEFT JOIN core.user_profile up ON up.user_id = gmc.organizer_user_id
       WHERE gmc.group_family = 'SHARED_EXPERIENCE'
       ORDER BY m.created_at DESC
       LIMIT 200`
    );

    res.json({
      data: {
        items: result.rows.map((row) => ({
          momentId: row.moment_id,
          title: row.title,
          status: row.status,
          momentTypeCode: row.moment_type_code,
          experienceKind: row.experience_kind,
          startAt: row.start_at,
          endAt: row.end_at,
          organizerUserId: row.organizer_user_id,
          organizerName: row.organizer_name,
          participantCount: Number(row.participant_count),
          createdAt: row.created_at,
        })),
      },
    });
  } catch (err) {
    next(err);
  }
});

adminGroupExperiencesRouter.get('/:momentId', async (req, res, next) => {
  try {
    const pool = getPool();
    const momentId = String(req.params.momentId);
    const moment = await pool.query<{
      moment_id: string;
      title: string;
      description: string | null;
      status: string;
      moment_type_code: string;
      experience_kind: string | null;
      destination_text: string | null;
      venue_text: string | null;
      start_at: string | null;
      end_at: string | null;
      timezone: string;
      organizer_user_id: string;
      organizer_name: string | null;
      created_at: string;
      updated_at: string;
    }>(
      `SELECT m.moment_id,
              m.title,
              m.description,
              m.status,
              mt.code AS moment_type_code,
              sec.experience_kind,
              sec.destination_text,
              sec.venue_text,
              COALESCE(sec.start_at, m.start_at) AS start_at,
              COALESCE(sec.end_at, m.end_at) AS end_at,
              m.timezone,
              gmc.organizer_user_id,
              up.display_name AS organizer_name,
              m.created_at,
              m.updated_at
       FROM core.moment m
       JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
       JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
       LEFT JOIN collaboration.shared_experience_context sec ON sec.moment_id = m.moment_id
       LEFT JOIN core.user_profile up ON up.user_id = gmc.organizer_user_id
       WHERE m.moment_id = $1
         AND gmc.group_family = 'SHARED_EXPERIENCE'`,
      [momentId]
    );

    if (!moment.rows[0]) {
      res.status(404).json({ code: 'NOT_FOUND', message: 'Group experience not found.' });
      return;
    }

    const participants = await pool.query<{
      participant_id: string;
      participant_role: string;
      status: string;
      user_id: string | null;
      user_name: string | null;
      external_party_id: string | null;
      external_name: string | null;
      metadata: Record<string, unknown>;
    }>(
      `SELECT mp.participant_id,
              mp.participant_role,
              mp.status,
              mp.user_id,
              up.display_name AS user_name,
              mp.external_party_id,
              ep.display_name AS external_name,
              mp.metadata
       FROM collaboration.moment_participant mp
       LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
       LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
       WHERE mp.moment_id = $1
       ORDER BY mp.created_at ASC`,
      [momentId]
    );

    const row = moment.rows[0];
    res.json({
      data: {
        momentId: row.moment_id,
        title: row.title,
        description: row.description,
        status: row.status,
        momentTypeCode: row.moment_type_code,
        experienceKind: row.experience_kind,
        destinationText: row.destination_text,
        venueText: row.venue_text,
        startAt: row.start_at,
        endAt: row.end_at,
        timezone: row.timezone,
        organizerUserId: row.organizer_user_id,
        organizerName: row.organizer_name,
        createdAt: row.created_at,
        updatedAt: row.updated_at,
        participants: participants.rows.map((p) => ({
          participantId: p.participant_id,
          roleCode: p.participant_role,
          status: p.status,
          userId: p.user_id,
          displayName:
            p.user_name ??
            p.external_name ??
            (typeof p.metadata?.displayName === 'string' ? p.metadata.displayName : null),
        })),
      },
    });
  } catch (err) {
    next(err);
  }
});
