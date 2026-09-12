import type { Pool } from 'pg';
import { emitDerivedSignals } from './emit-signal';
import { evaluateBusinessSignals } from './evaluate-business';
import { evaluateGroupSignals } from './evaluate-group';
import { evaluatePersonalSignals } from './evaluate-personal';
import type { SignalEmitResult } from './types';

export type SignalTickResult = {
  group: SignalEmitResult;
  business: SignalEmitResult;
  personal: SignalEmitResult;
  momentDigests: SignalEmitResult;
};

/**
 * Evaluate all derived signals from canonical projections, then emit through Wave 1.
 */
export async function runDerivedSignalTick(pool: Pool): Promise<SignalTickResult> {
  const [groupSignals, businessSignals, personalSignals] = await Promise.all([
    evaluateGroupSignals(pool),
    evaluateBusinessSignals(pool),
    evaluatePersonalSignals(pool),
  ]);

  const group = await emitDerivedSignals(pool, groupSignals);
  const business = await emitDerivedSignals(pool, businessSignals);
  const personal = await emitDerivedSignals(pool, personalSignals);
  const momentDigests = await emitMomentScopedDigests(pool);

  return { group, business, personal, momentDigests };
}

/**
 * Moment-scoped smart digests: when a user has multiple digest-pending inbox rows
 * for the same moment, emit one MomentDigestReady instead of only a global DigestReady.
 */
async function emitMomentScopedDigests(pool: Pool): Promise<SignalEmitResult> {
  const { emitDerivedSignal } = await import('./emit-signal');
  const { scoreActionability } = await import('./actionability');

  const rows = await pool.query<{
    user_id: string;
    moment_id: string;
    moment_title: string | null;
    domain_code: string;
    n: string;
  }>(
    `SELECT un.user_id, un.moment_id, m.title AS moment_title, m.domain_code, COUNT(*)::text AS n
     FROM platform.user_notification un
     JOIN core.moment m ON m.moment_id = un.moment_id
     WHERE un.digest_pending = true
       AND un.pushed_at IS NULL
       AND un.moment_id IS NOT NULL
     GROUP BY un.user_id, un.moment_id, m.title, m.domain_code
     HAVING COUNT(*) >= 2
     LIMIT 100`
  );

  const now = new Date();
  let emitted = 0;
  let suppressed = 0;
  for (const row of rows.rows) {
    const count = Math.floor(Number(row.n));
    const day = now.toISOString().slice(0, 10);
    const title = row.moment_title ?? 'your moment';
    const actionabilityScore = scoreActionability({
      importance: 'NORMAL',
      hasClearNextAction: true,
    });
    const contextType =
      row.domain_code === 'BUSINESS'
        ? 'BUSINESS'
        : row.domain_code === 'PERSONAL'
          ? 'PERSONAL'
          : 'GROUP';
    const ok = await emitDerivedSignal(pool, {
      signalName: 'MomentDigestReady',
      userId: row.user_id,
      contextType,
      momentId: row.moment_id,
      category: 'system',
      importance: 'NORMAL',
      facts: {
        momentTitle: title,
        count,
        body: `${count} updates in ${title} while you were away.`,
      },
      dedupeKey: `MOMENT:${row.moment_id}:USER:${row.user_id}:DIGEST:${day}`,
      explanationCode: 'MOMENT_DIGEST',
      observedAt: now,
      actionabilityScore,
    });
    if (ok) {
      emitted += 1;
      await pool.query(
        `UPDATE platform.user_notification
         SET digest_pending = false, pushed_at = coalesce(pushed_at, now())
         WHERE user_id = $1 AND moment_id = $2
           AND digest_pending = true AND pushed_at IS NULL`,
        [row.user_id, row.moment_id]
      );
    } else {
      suppressed += 1;
    }
  }
  return { attempted: rows.rows.length, emitted, suppressed };
}

export type { DerivedNotificationSignal } from './types';
export { scoreActionability, importanceFromScore } from './actionability';
