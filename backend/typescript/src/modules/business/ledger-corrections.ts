/**
 * P2.2 Append-only corrections for financially/inventory-posted operating events.
 * Original rows keep amounts/directions/source identities; reversal + correction events
 * reverse and repost effects atomically.
 *
 * Callers: operating-service.reviseOperatingEvent
 * User: "Replace destructive revisions with correction/reversal events linked to the original."
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import {
  syncFinancialEffectsFromOperatingEvent,
} from './finance-bridge';
import { recordInventoryMovement, syncInventoryFromOperatingEvent } from './inventory-bridge';

const OPPOSITE: Record<string, string> = {
  RECEIPT: 'ISSUE',
  ISSUE: 'RECEIPT',
  TRANSFER_IN: 'TRANSFER_OUT',
  TRANSFER_OUT: 'TRANSFER_IN',
  RETURN: 'ISSUE',
  ADJUSTMENT: 'ADJUSTMENT',
};

export async function eventHasLedgerEffects(client: PoolClient, eventId: string): Promise<boolean> {
  const [o, m] = await Promise.all([
    client.query(`SELECT 1 FROM business.business_obligation WHERE source_event_id = $1 LIMIT 1`, [eventId]),
    client.query(
      `SELECT 1 FROM business.business_inventory_movement WHERE source_event_id = $1 LIMIT 1`,
      [eventId]
    ),
  ]);
  return (o.rowCount ?? 0) > 0 || (m.rowCount ?? 0) > 0;
}

export function financialFieldsChanged(
  current: {
    amount: string | null;
    quantity: string | null;
    paymentStatus: string;
    parentEventId: string | null;
    eventType: string;
  },
  next: {
    amount: string | null;
    quantity: string | null;
    paymentStatus: string | null;
    parentEventId: string | null;
  }
): boolean {
  const amt = (v: string | null | undefined) => (v == null || v === '' ? null : Number(v).toFixed(4));
  if (amt(current.amount) !== amt(next.amount)) return true;
  if (amt(current.quantity) !== amt(next.quantity)) return true;
  if (next.paymentStatus != null && next.paymentStatus !== current.paymentStatus) return true;
  if (next.parentEventId !== undefined && next.parentEventId !== current.parentEventId) return true;
  return false;
}

async function reverseInventory(
  client: PoolClient,
  args: { companyId: string; momentId: string; originalEventId: string; reversalEventId: string }
): Promise<number> {
  const rows = await client.query<{
    movement_kind: string;
    quantity: string;
    unit: string | null;
    product_id: string | null;
    location_id: string | null;
    note: string | null;
  }>(
    `SELECT movement_kind, quantity::text, unit, product_id, location_id, note
     FROM business.business_inventory_movement
     WHERE source_event_id = $1`,
    [args.originalEventId]
  );
  let n = 0;
  for (const row of rows.rows) {
    const opposite = OPPOSITE[row.movement_kind] ?? 'ADJUSTMENT';
    await recordInventoryMovement(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      sourceEventId: args.reversalEventId,
      kind: opposite as 'RECEIPT' | 'ISSUE' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'ADJUSTMENT' | 'RETURN',
      quantity: row.quantity,
      unit: row.unit,
      productId: row.product_id,
      locationId: row.location_id,
      note: `Reversal of ${row.movement_kind}${row.note ? `: ${row.note}` : ''}`,
    });
    n += 1;
  }
  return n;
}

async function reverseFinance(
  client: PoolClient,
  args: { originalEventId: string }
): Promise<'VOIDED' | 'BLOCKED_SETTLED' | 'NONE'> {
  const obl = await client.query<{
    obligation_id: string;
    status: string;
    outstanding: string;
    amount: string;
  }>(
    `SELECT obligation_id, status, outstanding::text, amount::text
     FROM business.business_obligation
     WHERE source_event_id = $1
     FOR UPDATE`,
    [args.originalEventId]
  );
  const row = obl.rows[0];
  if (!row) return 'NONE';
  if (row.status === 'SETTLED' || row.status === 'PARTIAL') {
    const alloc = await client.query(
      `SELECT 1 FROM business.business_settlement_allocation WHERE obligation_id = $1 LIMIT 1`,
      [row.obligation_id]
    );
    if ((alloc.rowCount ?? 0) > 0) return 'BLOCKED_SETTLED';
  }
  await client.query(
    `UPDATE business.business_obligation
     SET status = 'VOID', outstanding = 0, updated_at = now(), version = version + 1
     WHERE obligation_id = $1`,
    [row.obligation_id]
  );
  return 'VOIDED';
}

/**
 * Append REVERSAL + CORRECTION events; leave original financial fields unchanged.
 * Returns the correction event id (API continues to expose a writable handle).
 */
export async function appendCorrectionForOperatingEvent(
  client: PoolClient,
  ctx: RequestContext,
  args: {
    companyId: string;
    momentId: string;
    currencyCode: string;
    original: {
      eventId: string;
      eventType: string;
      title: string;
      amount: string | null;
      quantity: string | null;
      paymentStatus: string;
      approvalStatus: string;
      parentEventId: string | null;
      counterpartyName: string | null;
      channelName: string | null;
      payload: Record<string, unknown> | null;
      occurredAt: string;
    };
    correction: {
      title: string;
      note?: string | null;
      amount: string | null;
      quantity: string | null;
      paymentStatus: string;
      parentEventId: string | null;
      counterpartyName: string | null;
      channelName: string | null;
      dueAt?: string | null;
      payload: Record<string, unknown>;
      occurredAt?: string | null;
    };
  }
): Promise<{ reversalEventId: string; correctionEventId: string }> {
  const fin = await reverseFinance(client, { originalEventId: args.original.eventId });
  if (fin === 'BLOCKED_SETTLED') {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Settled financial events cannot be rewritten. Post an adjusting event instead.',
      409
    );
  }

  const reversalPayload = {
    reversesEventId: args.original.eventId,
    correctionKind: 'REVERSAL',
  };
  const rev = await client.query<{ operating_event_id: string }>(
    `INSERT INTO business.operating_event (
       moment_id, company_id, actor_user_id, event_type, title, note, amount, currency_code,
       counterparty_name, channel_name, payment_relevance, payment_status, approval_status,
       submitted_by_user_id, occurred_at, parent_event_id, quantity, payload
     ) VALUES (
       $1,$2,$3,'REVERSAL',$4,$5,$6,$7,$8,$9,'NONE','NONE','NONE',$3, now(), $10, $11, $12::jsonb
     ) RETURNING operating_event_id`,
    [
      args.momentId,
      args.companyId,
      ctx.userId,
      `Reversal · ${args.original.title}`,
      'Append-only reversal of posted ledger effects.',
      args.original.amount,
      args.currencyCode,
      args.original.counterpartyName,
      args.original.channelName,
      args.original.eventId,
      args.original.quantity,
      JSON.stringify(reversalPayload),
    ]
  );
  const reversalEventId = rev.rows[0]!.operating_event_id;
  await reverseInventory(client, {
    companyId: args.companyId,
    momentId: args.momentId,
    originalEventId: args.original.eventId,
    reversalEventId,
  });

  const correctionPayload = {
    ...args.correction.payload,
    correctsEventId: args.original.eventId,
    reversalEventId,
    correctionKind: 'CORRECTION',
  };
  const corr = await client.query<{ operating_event_id: string }>(
    `INSERT INTO business.operating_event (
       moment_id, company_id, actor_user_id, event_type, title, note, amount, currency_code,
       counterparty_name, channel_name, payment_relevance, payment_status, approval_status,
       submitted_by_user_id, occurred_at, parent_event_id, quantity, payload
     ) VALUES (
       $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,'NONE',$3,
       COALESCE($13::timestamptz, now()), $14, $15, $16::jsonb
     ) RETURNING operating_event_id`,
    [
      args.momentId,
      args.companyId,
      ctx.userId,
      args.original.eventType,
      args.correction.title,
      args.correction.note ?? 'Append-only correction of posted event.',
      args.correction.amount,
      args.currencyCode,
      args.correction.counterpartyName,
      args.correction.channelName,
      args.original.eventType === 'PAYMENT' || args.original.eventType === 'SPEND' || args.original.eventType === 'PURCHASE'
        ? 'PAYABLE'
        : args.original.eventType === 'COLLECTION' ||
            args.original.eventType === 'SALE' ||
            args.original.eventType === 'INVOICE' ||
            args.original.eventType === 'SERVICE'
          ? 'RECEIVABLE'
          : 'NONE',
      args.correction.paymentStatus,
      args.correction.occurredAt ?? null,
      args.correction.parentEventId ?? args.original.eventId,
      args.correction.quantity,
      JSON.stringify(correctionPayload),
    ]
  );
  const correctionEventId = corr.rows[0]!.operating_event_id;

  await syncFinancialEffectsFromOperatingEvent(client, {
    companyId: args.companyId,
    momentId: args.momentId,
    eventId: correctionEventId,
    eventType: args.original.eventType,
    amount: args.correction.amount,
    currencyCode: args.currencyCode,
    paymentStatus: args.correction.paymentStatus,
    approvalStatus: 'NONE',
    parentEventId: args.correction.parentEventId,
    dueAt: args.correction.dueAt,
  });
  await syncInventoryFromOperatingEvent(client, {
    companyId: args.companyId,
    momentId: args.momentId,
    eventId: correctionEventId,
    eventType: args.original.eventType,
    quantity: args.correction.quantity,
    occurredAt: args.correction.occurredAt ?? null,
    payload: correctionPayload,
  });

  // Mark original as superseded without mutating financial columns.
  await client.query(
    `UPDATE business.operating_event
     SET payload = COALESCE(payload, '{}'::jsonb) || $2::jsonb,
         version = version + 1
     WHERE operating_event_id = $1`,
    [
      args.original.eventId,
      JSON.stringify({
        correctedByEventId: correctionEventId,
        reversedByEventId: reversalEventId,
        ledgerStatus: 'SUPERSEDED',
      }),
    ]
  );

  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'OperatingEventCorrected',
    domainCode: 'BUSINESS',
    aggregateType: 'OPERATING_EVENT',
    aggregateId: correctionEventId,
    scopeType: 'MOMENT',
    scopeId: args.momentId,
    payload: {
      originalEventId: args.original.eventId,
      reversalEventId,
      correctionEventId,
      momentId: args.momentId,
    },
  });
  await insertAudit(client, ctx, 'OPERATING_EVENT_CORRECT', 'OPERATING_EVENT', correctionEventId, domainEventId, {
    originalEventId: args.original.eventId,
    reversalEventId,
  });

  return { reversalEventId, correctionEventId };
}
