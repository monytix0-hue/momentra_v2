import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getAuth, type DecodedIdToken } from 'firebase-admin/auth';
import { config } from '../config';
import { getPool } from '../database/pool';
import { firebaseUserId } from './uuid';

function initFirebaseAdmin(): void {
  if (getApps().length > 0) return;
  if (config.firebase.credentialsJson) {
    const cred = JSON.parse(config.firebase.credentialsJson) as Record<string, string>;
    initializeApp({
      credential: cert(cred),
      projectId: config.firebase.projectId || cred.project_id,
    });
  } else if (config.firebase.projectId) {
    initializeApp({ projectId: config.firebase.projectId });
  }
}

export async function verifyFirebaseToken(bearerToken: string): Promise<DecodedIdToken> {
  initFirebaseAdmin();
  return getAuth().verifyIdToken(bearerToken);
}

/** In-process warm cache: skip DB after we know the profile row exists. */
const knownProfileUntil = new Map<string, number>();
const KNOWN_PROFILE_TTL_MS = 10 * 60 * 1000;

export type EnsureUserProfileResult = 'cached' | 'existed' | 'created';

/** Test / soft-delete helper — drop warm-path skip for a user. */
export function forgetKnownUserProfile(userId: string): void {
  knownProfileUntil.delete(userId);
}

/** Test helper — clear all warm-path profile skips. */
export function clearKnownUserProfiles(): void {
  knownProfileUntil.clear();
}

/**
 * First auth / recovery: ensure `core.user_profile` exists.
 * Warm path: in-process known-user skip (no DB).
 * Cold/unknown: single INSERT … ON CONFLICT DO NOTHING (1 RTT; no SELECT+UPSERT pair).
 */
export async function ensureUserProfile(
  userId: string,
  email?: string,
  displayName?: string,
  phone?: string | null
): Promise<EnsureUserProfileResult> {
  const now = Date.now();
  const until = knownProfileUntil.get(userId);
  if (until !== undefined && until > now) {
    if (phone) {
      await getPool().query(
        `UPDATE core.user_profile
         SET phone = COALESCE(phone, $2), updated_at = now()
         WHERE user_id = $1 AND (phone IS NULL OR phone = '')`,
        [userId, phone]
      );
    }
    return 'cached';
  }

  const safeEmail = email ?? `${userId}@users.momentra.local`;
  try {
    const inserted = await getPool().query<{ user_id: string }>(
      `INSERT INTO core.user_profile (user_id, email, display_name, phone, status)
       VALUES ($1, $2, $3, $4, 'ACTIVE')
       ON CONFLICT (user_id) DO NOTHING
       RETURNING user_id`,
      [userId, safeEmail, displayName ?? null, phone ?? null]
    );
    if (!inserted.rows[0] && phone) {
      await getPool().query(
        `UPDATE core.user_profile
         SET phone = COALESCE(phone, $2),
             display_name = COALESCE(display_name, $3),
             updated_at = now()
         WHERE user_id = $1`,
        [userId, phone, displayName ?? null]
      );
    }
    knownProfileUntil.set(userId, now + KNOWN_PROFILE_TTL_MS);
    return inserted.rows[0] ? 'created' : 'existed';
  } catch (e) {
    const msg = String(e);
    if (msg.includes('uq_user_profile__email_ci')) {
      await provisionUserProfile(userId, `${userId}@users.momentra.local`, displayName, phone);
      knownProfileUntil.set(userId, now + KNOWN_PROFILE_TTL_MS);
      return 'created';
    }
    throw e;
  }
}

export async function provisionUserProfile(
  userId: string,
  email?: string,
  displayName?: string,
  phone?: string | null
): Promise<void> {
  const safeEmail = email ?? `${userId}@users.momentra.local`;
  try {
    await getPool().query(
      `INSERT INTO core.user_profile (user_id, email, display_name, phone, status)
       VALUES ($1, $2, $3, $4, 'ACTIVE')
       ON CONFLICT (user_id) DO UPDATE SET
         display_name = COALESCE(EXCLUDED.display_name, core.user_profile.display_name),
         phone = COALESCE(EXCLUDED.phone, core.user_profile.phone),
         updated_at = now()`,
      [userId, safeEmail, displayName ?? null, phone ?? null]
    );
  } catch (e) {
    const msg = String(e);
    if (msg.includes('uq_user_profile__email_ci')) {
      await getPool().query(
        `INSERT INTO core.user_profile (user_id, email, display_name, phone, status)
         VALUES ($1, $2, $3, $4, 'ACTIVE')
         ON CONFLICT (user_id) DO UPDATE SET
           display_name = COALESCE(EXCLUDED.display_name, core.user_profile.display_name),
           phone = COALESCE(EXCLUDED.phone, core.user_profile.phone),
           updated_at = now()`,
        [userId, `${userId}@users.momentra.local`, displayName ?? null, phone ?? null]
      );
      return;
    }
    throw e;
  }
}

export function resolveIdentityFromToken(decoded: DecodedIdToken): {
  firebaseUid: string;
  firebaseProjectId: string;
  userId: string;
  email?: string;
  displayName?: string;
  phone?: string;
} {
  const firebaseProjectId = (decoded.aud as string) || config.firebase.projectId || 'unknown';
  const firebaseUid = decoded.uid;
  const phone =
    typeof (decoded as { phone_number?: string }).phone_number === 'string'
      ? (decoded as { phone_number?: string }).phone_number
      : undefined;
  return {
    firebaseUid,
    firebaseProjectId,
    userId: firebaseUserId(firebaseProjectId, firebaseUid),
    email: decoded.email,
    displayName: decoded.name,
    phone,
  };
}

export function resolveDevIdentity(devUid: string): {
  firebaseUid: string;
  firebaseProjectId: string;
  userId: string;
} {
  const firebaseProjectId = config.firebase.projectId || 'momentra-dev';
  return {
    firebaseUid: devUid,
    firebaseProjectId,
    userId: firebaseUserId(firebaseProjectId, devUid),
  };
}
