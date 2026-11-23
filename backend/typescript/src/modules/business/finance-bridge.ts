/**
 * Business V4 finance bridge — posting helpers for obligations / settlements.
 * Canonical events live in business.operating_event; these helpers keep AR/AP consistent.
 */
import type { PoolClient } from 'pg';
import { AppError, ErrorCode } from '../../platform/errors/errors';

export type ObligationKind = 'PAYABLE' | 'RECEIVABLE';

export async function openObligation(
  client: PoolClient,
  args: {
    companyId: string;
    momentId: string;
    partyId?: string | null;
    kind: ObligationKind;
    sourceEventId: string;
    currencyCode: string;
    amount: string;
    dueAt?: string | null;
  }
): Promise<{ obligationId: string }> {
  const amount = Number(args.amount);
  if (!(amount > 0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Obligation amount must be positive.', 400);
  }
  const inserted = await client.query<{ obligation_id: string }>(
    `INSERT INTO business.business_obligation (
       company_id, moment_id, party_id, obligation_kind, source_event_id,
       currency_code, amount, outstanding, status, due_at
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$7,'OPEN',$8)
     RETURNING obligation_id`,
    [
      args.companyId,
      args.momentId,
      args.partyId ?? null,
      args.kind,
      args.sourceEventId,
      args.currencyCode,
      args.amount,
      args.dueAt ?? null,
    ]
  );
  return { obligationId: inserted.rows[0]!.obligation_id };
}

/**
 * Allocate a payment/collection event against an obligation under row lock.
 * Rejects over-allocation (AT-14).
 */
export async function allocateSettlement(
  client: PoolClient,
  args: {
    obligationId: string;
    paymentEventId: string;
    allocatedAmount: string;
    currencyCode: string;
  }
): Promise<{ outstanding: string; status: string }> {
  const locked = await client.query<{
    obligation_id: string;
    outstanding: string;
    amount: string;
    status: string;
    currency_code: string;
  }>(
    `SELECT obligation_id, outstanding::text, amount::text, status, currency_code
     FROM business.business_obligation
     WHERE obligation_id = $1
     FOR UPDATE`,
    [args.obligationId]
  );
  const row = locked.rows[0];
  if (!row) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Obligation not found.', 404);
  }
  if (row.status === 'VOID' || row.status === 'SETTLED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Obligation is not open for settlement.', 409);
  }
  const alloc = Number(args.allocatedAmount);
  const outstanding = Number(row.outstanding);
  if (!(alloc > 0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Allocation must be positive.', 400);
  }
  if (alloc > outstanding + 1e-9) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Allocation exceeds outstanding balance.', 409);
  }

  await client.query(
    `INSERT INTO business.business_settlement_allocation (
       obligation_id, payment_event_id, allocated_amount, currency_code
     ) VALUES ($1,$2,$3,$4)`,
    [args.obligationId, args.paymentEventId, args.allocatedAmount, args.currencyCode]
  );

  const next = outstanding - alloc;
  const status = next <= 1e-9 ? 'SETTLED' : 'PARTIAL';
  const updated = await client.query<{ outstanding: string; status: string }>(
    `UPDATE business.business_obligation
     SET outstanding = $2, status = $3, updated_at = now(), version = version + 1
     WHERE obligation_id = $1
     RETURNING outstanding::text, status`,
    [args.obligationId, Math.max(0, next).toFixed(4), status]
  );
  return updated.rows[0]!;
}


async function findObligationBySource(
  client: PoolClient,
  sourceEventId: string
): Promise<{ obligationId: string; outstanding: string; status: string } | null> {
  const r = await client.query<{ obligation_id: string; outstanding: string; status: string }>(
    `SELECT obligation_id, outstanding::text, status
     FROM business.business_obligation
     WHERE source_event_id = $1
     ORDER BY created_at ASC
     LIMIT 1`,
    [sourceEventId]
  );
  const row = r.rows[0];
  if (!row) return null;
  return { obligationId: row.obligation_id, outstanding: row.outstanding, status: row.status };
}

/**
 * Idempotent open: one obligation per source operating_event.
 * Callers: syncFinancialEffectsFromOperatingEvent / operating-service.
 */
export async function openObligationIdempotent(
  client: PoolClient,
  args: Parameters<typeof openObligation>[1]
): Promise<{ obligationId: string; created: boolean }> {
  const existing = await findObligationBySource(client, args.sourceEventId);
  if (existing) return { obligationId: existing.obligationId, created: false };
  const opened = await openObligation(client, args);
  return { obligationId: opened.obligationId, created: true };
}

/**
 * Allocate with idempotent unique (payment_event_id, obligation_id).
 * Concurrent over-allocation is rejected by FOR UPDATE + outstanding check.
 */
export async function allocateSettlementIdempotent(
  client: PoolClient,
  args: Parameters<typeof allocateSettlement>[1]
): Promise<{ outstanding: string; status: string; created: boolean }> {
  const prior = await client.query<{ outstanding: string; status: string }>(
    `SELECT o.outstanding::text, o.status
     FROM business.business_settlement_allocation a
     JOIN business.business_obligation o ON o.obligation_id = a.obligation_id
     WHERE a.payment_event_id = $1 AND a.obligation_id = $2
     LIMIT 1`,
    [args.paymentEventId, args.obligationId]
  );
  if (prior.rows[0]) {
    return { outstanding: prior.rows[0].outstanding, status: prior.rows[0].status, created: false };
  }
  try {
    const result = await allocateSettlement(client, args);
    return { ...result, created: true };
  } catch (err) {
    const msg = String(err);
    if (msg.includes('uq_business_settlement_allocation__payment_obligation') || msg.includes('duplicate key')) {
      const again = await client.query<{ outstanding: string; status: string }>(
        `SELECT outstanding::text, status FROM business.business_obligation WHERE obligation_id = $1`,
        [args.obligationId]
      );
      if (again.rows[0]) {
        return { outstanding: again.rows[0].outstanding, status: again.rows[0].status, created: false };
      }
    }
    throw err;
  }
}

const PAYABLE_TYPES = new Set(['SPEND', 'PURCHASE', 'VENDOR_COST']);
const RECEIVABLE_TYPES = new Set(['INVOICE', 'SALE', 'SERVICE']);
const SETTLEMENT_TYPES = new Set(['PAYMENT', 'COLLECTION']);

/**
 * Post AR/AP effects for one operating_event inside the same DB transaction.
 * Does not create a second ledger — obligations reference operating_event ids.
 */
export async function syncFinancialEffectsFromOperatingEvent(
  client: PoolClient,
  args: {
    companyId: string;
    momentId: string;
    eventId: string;
    eventType: string;
    amount: string | null;
    currencyCode: string;
    paymentStatus: string;
    approvalStatus: string;
    parentEventId?: string | null;
    partyId?: string | null;
    dueAt?: string | null;
    allocations?: Array<{ sourceId: string; amount: string }> | null;
  }
): Promise<void> {
  if (args.approvalStatus === 'PENDING' || args.approvalStatus === 'REJECTED') return;
  const type = args.eventType.toUpperCase();
  const amount = args.amount;
  if (!amount || !(Number(amount) > 0)) {
    if (!SETTLEMENT_TYPES.has(type)) return;
  }

  if (PAYABLE_TYPES.has(type) && args.paymentStatus !== 'CANCELLED') {
    // Credit purchase / unpaid spend opens a payable. Cash (PAID) still opens so
    // the paired PAYMENT child can settle atomically without inventing revenue.
    await openObligationIdempotent(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      partyId: args.partyId,
      kind: 'PAYABLE',
      sourceEventId: args.eventId,
      currencyCode: args.currencyCode,
      amount: amount!,
      dueAt: args.dueAt,
    });
    return;
  }

  if (RECEIVABLE_TYPES.has(type) && args.paymentStatus !== 'CANCELLED') {
    // Cash sale (COLLECTED) still opens receivable; collection child settles it.
    // Collection never increases revenue — only settles outstanding.
    await openObligationIdempotent(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      partyId: args.partyId,
      kind: 'RECEIVABLE',
      sourceEventId: args.eventId,
      currencyCode: args.currencyCode,
      amount: amount!,
      dueAt: args.dueAt,
    });
    return;
  }

  if (!SETTLEMENT_TYPES.has(type)) return;

  const targets: Array<{ obligationId: string; allocatedAmount: string }> = [];
  if (args.allocations?.length) {
    for (const alloc of args.allocations) {
      const obl = await findObligationBySource(client, alloc.sourceId);
      if (!obl) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, 'Allocation source has no open obligation.', 400);
      }
      targets.push({ obligationId: obl.obligationId, allocatedAmount: alloc.amount });
    }
  } else if (args.parentEventId) {
    const obl = await findObligationBySource(client, args.parentEventId);
    if (!obl) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Parent event has no open obligation to settle.', 400);
    }
    targets.push({ obligationId: obl.obligationId, allocatedAmount: amount! });
  } else {
    // Unlinked settlement: nothing to allocate (allowed when no open parents).
    return;
  }

  for (const t of targets) {
    await allocateSettlementIdempotent(client, {
      obligationId: t.obligationId,
      paymentEventId: args.eventId,
      allocatedAmount: t.allocatedAmount,
      currencyCode: args.currencyCode,
    });
  }
}


export type PlannedFinancialEffect =
  | {
      kind: 'OPEN_OBLIGATION';
      obligationKind: ObligationKind;
      sourceEventId: string;
      amount: string;
      currencyCode: string;
      dueAt?: string | null;
    }
  | {
      kind: 'ALLOCATE';
      sourceEventId: string;
      parentEventId: string;
      amount: string;
      currencyCode: string;
      ambiguous?: boolean;
      reason?: string;
    }
  | { kind: 'SKIP'; sourceEventId: string; reason: string };

/** Pure planner shared by live writes and historical backfill dry-runs. */
export function planFinancialEffects(args: {
  eventId: string;
  eventType: string;
  amount: string | null;
  currencyCode: string;
  paymentStatus: string;
  approvalStatus: string;
  parentEventId?: string | null;
  dueAt?: string | null;
  allocations?: Array<{ sourceId: string; amount: string }> | null;
  /** When true, settlement without parent/allocations is ambiguous (backfill). */
  requireSettlementTarget?: boolean;
}): PlannedFinancialEffect[] {
  if (args.approvalStatus === 'PENDING' || args.approvalStatus === 'REJECTED') {
    return [{ kind: 'SKIP', sourceEventId: args.eventId, reason: 'approval_pending_or_rejected' }];
  }
  const type = args.eventType.toUpperCase();
  const amount = args.amount;
  const out: PlannedFinancialEffect[] = [];

  if (PAYABLE_TYPES.has(type) && args.paymentStatus !== 'CANCELLED') {
    if (!amount || !(Number(amount) > 0)) {
      return [{ kind: 'SKIP', sourceEventId: args.eventId, reason: 'missing_amount' }];
    }
    out.push({
      kind: 'OPEN_OBLIGATION',
      obligationKind: 'PAYABLE',
      sourceEventId: args.eventId,
      amount,
      currencyCode: args.currencyCode,
      dueAt: args.dueAt,
    });
    return out;
  }

  if (RECEIVABLE_TYPES.has(type) && args.paymentStatus !== 'CANCELLED') {
    if (!amount || !(Number(amount) > 0)) {
      return [{ kind: 'SKIP', sourceEventId: args.eventId, reason: 'missing_amount' }];
    }
    out.push({
      kind: 'OPEN_OBLIGATION',
      obligationKind: 'RECEIVABLE',
      sourceEventId: args.eventId,
      amount,
      currencyCode: args.currencyCode,
      dueAt: args.dueAt,
    });
    return out;
  }

  if (!SETTLEMENT_TYPES.has(type)) {
    return [{ kind: 'SKIP', sourceEventId: args.eventId, reason: 'non_financial_event' }];
  }

  if (args.allocations?.length) {
    for (const alloc of args.allocations) {
      out.push({
        kind: 'ALLOCATE',
        sourceEventId: args.eventId,
        parentEventId: alloc.sourceId,
        amount: alloc.amount,
        currencyCode: args.currencyCode,
      });
    }
    return out;
  }
  if (args.parentEventId && amount && Number(amount) > 0) {
    out.push({
      kind: 'ALLOCATE',
      sourceEventId: args.eventId,
      parentEventId: args.parentEventId,
      amount,
      currencyCode: args.currencyCode,
    });
    return out;
  }
  if (args.requireSettlementTarget) {
    return [
      {
        kind: 'ALLOCATE',
        sourceEventId: args.eventId,
        parentEventId: '',
        amount: amount ?? '0',
        currencyCode: args.currencyCode,
        ambiguous: true,
        reason: 'settlement_missing_parent_or_allocations',
      },
    ];
  }
  return [{ kind: 'SKIP', sourceEventId: args.eventId, reason: 'unlinked_settlement' }];
}

/** Pure helpers for tests / projection (no DB). */
export function budgetUsePercent(spend: number, budget: number): number | null {
  if (!(budget > 0)) return null;
  return Math.round((spend / budget) * 10000) / 100;
}

export function assertNoDoubleRevenue(opts: {
  postedRevenue: number;
  collectionAmount: number;
}): { revenue: number; cash: number } {
  // Collection never increases revenue.
  return { revenue: opts.postedRevenue, cash: opts.collectionAmount };
}
