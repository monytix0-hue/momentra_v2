import type { Pool } from 'pg';
import type { NotificationPriority } from './allowlist';

export type DeliveryRoute = 'immediate' | 'digest' | 'suppress';

const DEFAULT_WINDOW_MS = parseInt(process.env.NOTIFICATION_RATE_WINDOW_MS ?? '900000', 10); // 15m
const DEFAULT_NORMAL_LIMIT = parseInt(process.env.NOTIFICATION_RATE_NORMAL_LIMIT ?? '3', 10);

/**
 * Lightweight fatigue control (no ML):
 * HIGH → immediate
 * NORMAL → immediate under threshold per moment, else digest
 * LOW → digest (inbox + digest) unless forceImmediate
 */
export async function applyRatePolicy(
  pool: Pool,
  input: {
    userId: string;
    momentId: string | null;
    priority: NotificationPriority;
    forceImmediate?: boolean;
    now?: Date;
  }
): Promise<DeliveryRoute> {
  if (input.forceImmediate) return 'immediate';
  if (input.priority === 'HIGH') return 'immediate';
  if (input.priority === 'LOW') return 'digest';

  // NORMAL
  if (!input.momentId) return 'immediate';

  const windowMs = DEFAULT_WINDOW_MS;
  const limit = DEFAULT_NORMAL_LIMIT;
  const since = new Date((input.now ?? new Date()).getTime() - windowMs);

  const r = await pool.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM platform.user_notification
     WHERE user_id = $1
       AND moment_id = $2
       AND pushed_at IS NOT NULL
       AND pushed_at >= $3::timestamptz
       AND priority_code = 'NORMAL'`,
    [input.userId, input.momentId, since.toISOString()]
  );
  const count = parseInt(r.rows[0]?.n ?? '0', 10);
  return count >= limit ? 'digest' : 'immediate';
}
