import { randomUUID } from 'crypto';
import type { Pool } from 'pg';
import { insertDomainEventAndOutbox } from '../../events/outbox';
import type { RequestContext } from '../../request-context/context';
import { claimDerivedSignal } from './signal-store';
import type { DerivedNotificationSignal, SignalEmitResult } from './types';

function systemCtx(userId: string): RequestContext {
  return {
    firebaseUid: `signal:${userId}`,
    firebaseProjectId: process.env.FIREBASE_PROJECT_ID ?? 'momentra',
    userId,
    correlationId: randomUUID(),
    roles: [],
    permissions: [],
  };
}

/**
 * Claim (dedupe/hysteresis) then emit a domain event into the Wave 1 pipeline.
 * Does not bypass decision / rate / prefs / delivery.
 */
export async function emitDerivedSignal(
  pool: Pool,
  signal: DerivedNotificationSignal
): Promise<boolean> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const claimed = await claimDerivedSignal(client, signal);
    if (!claimed) {
      await client.query('ROLLBACK');
      return false;
    }

    const ctx = systemCtx(signal.userId);
    const domainCode =
      signal.contextType === 'BUSINESS'
        ? 'BUSINESS'
        : signal.contextType === 'GROUP'
          ? 'GROUP'
          : 'PERSONAL';
    const scopeType = signal.momentId
      ? 'MOMENT'
      : signal.companyId
        ? 'COMPANY'
        : 'USER';
    const scopeId = signal.momentId ?? signal.companyId ?? signal.userId;

    await insertDomainEventAndOutbox(client, ctx, {
      eventName: signal.signalName,
      domainCode,
      aggregateType: 'DERIVED_SIGNAL',
      aggregateId: randomUUID(),
      scopeType,
      scopeId,
      payload: {
        ...signal.facts,
        derivedSignal: true,
        explanationCode: signal.explanationCode,
        importance: signal.importance,
        actionabilityScore: signal.actionabilityScore,
        dedupeKey: signal.dedupeKey,
        momentId: signal.momentId,
        companyId: signal.companyId,
        contextId: signal.contextId,
        contextType: signal.contextType,
        category: signal.category,
        targetUserIds: [signal.userId],
        observedAt: signal.observedAt.toISOString(),
        validUntil: signal.validUntil?.toISOString(),
      },
    });
    await client.query('COMMIT');
    return true;
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

export async function emitDerivedSignals(
  pool: Pool,
  signals: DerivedNotificationSignal[]
): Promise<SignalEmitResult> {
  let emitted = 0;
  let suppressed = 0;
  for (const s of signals) {
    const ok = await emitDerivedSignal(pool, s);
    if (ok) emitted += 1;
    else suppressed += 1;
  }
  return { attempted: signals.length, emitted, suppressed };
}
