/**
 * Business V4 inventory bridge — shared movement ledger for Trading, Distribution,
 * Retail, and Manufacturing. Canonical source remains business.operating_event.
 *
 * Callers: operating-service.recordOperatingEvent (after INSERT) and approval paths.
 * Schema: business.business_inventory_movement (V104). No duplicate stock ledger.
 * User: "Connect Trading and Distribution stock movements to the shared inventory tables."
 */
import type { PoolClient } from 'pg';
import { AppError, ErrorCode } from '../../platform/errors/errors';

export type InventoryMovementKind =
  | 'RECEIPT'
  | 'ISSUE'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'ADJUSTMENT'
  | 'RETURN';

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function asUuid(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  return UUID_RE.test(value) ? value : null;
}

function qtyOf(raw: string | null | undefined): number {
  if (raw == null || raw === '') return 0;
  const n = Number(raw);
  return Number.isFinite(n) ? n : 0;
}

async function movementExists(
  client: PoolClient,
  sourceEventId: string,
  kind: InventoryMovementKind,
  productId: string | null,
  note: string | null
): Promise<boolean> {
  const r = await client.query(
    `SELECT 1 FROM business.business_inventory_movement
     WHERE source_event_id = $1
       AND movement_kind = $2
       AND COALESCE(product_id::text, '') = COALESCE($3::text, '')
       AND COALESCE(note, '') = COALESCE($4, '')
     LIMIT 1`,
    [sourceEventId, kind, productId, note]
  );
  return (r.rowCount ?? 0) > 0;
}

export async function recordInventoryMovement(
  client: PoolClient,
  args: {
    companyId: string;
    momentId: string;
    sourceEventId: string;
    kind: InventoryMovementKind;
    quantity: string;
    unit?: string | null;
    productId?: string | null;
    locationId?: string | null;
    note?: string | null;
    occurredAt?: string | null;
  }
): Promise<{ inventoryMovementId: string } | null> {
  const q = Number(args.quantity);
  if (!Number.isFinite(q) || q === 0) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Inventory quantity must be non-zero.', 400);
  }
  if (await movementExists(client, args.sourceEventId, args.kind, args.productId ?? null, args.note ?? null)) {
    return null;
  }
  const inserted = await client.query<{ inventory_movement_id: string }>(
    `INSERT INTO business.business_inventory_movement (
       company_id, moment_id, product_id, location_id, source_event_id,
       movement_kind, quantity, unit, note, occurred_at
     ) VALUES (
       $1,$2,$3,$4,$5,$6,$7,$8,$9, COALESCE($10::timestamptz, now())
     )
     RETURNING inventory_movement_id`,
    [
      args.companyId,
      args.momentId,
      args.productId ?? null,
      args.locationId ?? null,
      args.sourceEventId,
      args.kind,
      Math.abs(q).toFixed(4),
      args.unit ?? null,
      args.note ?? null,
      args.occurredAt ?? null,
    ]
  );
  return { inventoryMovementId: inserted.rows[0]!.inventory_movement_id };
}

/**
 * Derive inventory movements from a posted operating event.
 * Transfer posts paired TRANSFER_OUT + TRANSFER_IN (net zero).
 */
export async function syncInventoryFromOperatingEvent(
  client: PoolClient,
  args: {
    companyId: string;
    momentId: string;
    eventId: string;
    eventType: string;
    quantity: string | null;
    unit?: string | null;
    occurredAt?: string | null;
    payload?: Record<string, unknown> | null;
  }
): Promise<void> {
  const contract =
    args.payload?.contract && typeof args.payload.contract === 'object'
      ? (args.payload.contract as Record<string, unknown>)
      : {};
  const productId =
    asUuid(args.payload?.masterId) ??
    asUuid((args.payload?.master as Record<string, unknown> | undefined)?.masterId) ??
    null;
  const master = args.payload?.master as Record<string, unknown> | undefined;
  const unit =
    args.unit ?? (typeof master?.unit === 'string' ? String(master.unit) : null);
  const qty = qtyOf(args.quantity);
  const type = args.eventType.toUpperCase();

  if (type === 'STOCK') {
    const action = String(contract.stockAction ?? '').toUpperCase();
    if (action === 'TRANSFER') {
      if (!(qty > 0)) return;
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'TRANSFER_OUT',
        quantity: qty.toFixed(4),
        unit,
        productId,
        locationId: asUuid(contract.transferFromMasterId),
        note: 'Stock transfer out',
        occurredAt: args.occurredAt,
      });
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'TRANSFER_IN',
        quantity: qty.toFixed(4),
        unit,
        productId,
        locationId: asUuid(contract.transferToMasterId),
        note: 'Stock transfer in',
        occurredAt: args.occurredAt,
      });
      return;
    }
    if (action === 'ADD' && qty > 0) {
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'RECEIPT',
        quantity: qty.toFixed(4),
        unit,
        productId,
        note: 'Stock add',
        occurredAt: args.occurredAt,
      });
      return;
    }
    if (action === 'REDUCE' && qty > 0) {
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'ISSUE',
        quantity: qty.toFixed(4),
        unit,
        productId,
        note: 'Stock reduce',
        occurredAt: args.occurredAt,
      });
      return;
    }
    if (action === 'SET' && qty !== 0) {
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'ADJUSTMENT',
        quantity: Math.abs(qty).toFixed(4),
        unit,
        productId,
        note: `Stock set to ${Math.abs(qty).toFixed(4)}`,
        occurredAt: args.occurredAt,
      });
    }
    return;
  }

  if (type === 'PURCHASE') {
    const receiptState = String(contract.receiptState ?? '').toUpperCase();
    const received = qtyOf(
      typeof contract.quantityReceived === 'string' ? contract.quantityReceived : args.quantity
    );
    if (receiptState === 'AWAITING' || !(received > 0)) return;
    await recordInventoryMovement(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      sourceEventId: args.eventId,
      kind: 'RECEIPT',
      quantity: received.toFixed(4),
      unit,
      productId,
      note: 'Purchase receipt',
      occurredAt: args.occurredAt,
    });
    return;
  }

  if (type === 'PRODUCTION') {
    // QUICK output is recorded work only — never invents finished stock or revenue.
    const entryMode = String(contract.entryMode ?? 'QUICK').toUpperCase();
    if (entryMode !== 'ITEMIZED') return;
    const acceptedRaw =
      contract.acceptedQuantity != null && String(contract.acceptedQuantity) !== ''
        ? qtyOf(String(contract.acceptedQuantity))
        : qty;
    if (acceptedRaw > 0) {
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'RECEIPT',
        quantity: acceptedRaw.toFixed(4),
        unit,
        productId,
        note: 'Finished goods from production',
        occurredAt: args.occurredAt,
      });
    }
    const bom = Array.isArray(contract.bomLines) ? contract.bomLines : [];
    for (const line of bom) {
      if (!line || typeof line !== 'object') continue;
      const rec = line as Record<string, unknown>;
      const bq = qtyOf(typeof rec.quantity === 'string' || typeof rec.quantity === 'number' ? String(rec.quantity) : null);
      if (!(bq > 0)) continue;
      const materialId = asUuid(rec.masterId) ?? asUuid(rec.productId);
      const materialUnit = typeof rec.unit === 'string' ? rec.unit : unit;
      await recordInventoryMovement(client, {
        companyId: args.companyId,
        momentId: args.momentId,
        sourceEventId: args.eventId,
        kind: 'ISSUE',
        quantity: bq.toFixed(4),
        unit: materialUnit,
        productId: materialId,
        note: `BOM consume ${String(rec.itemName ?? 'material')}`,
        occurredAt: args.occurredAt,
      });
    }
    return;
  }

  if (type === 'SALE' || type === 'SHIPMENT' || type === 'DELIVERY') {
    if (!(qty > 0)) return;
    await recordInventoryMovement(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      sourceEventId: args.eventId,
      kind: 'ISSUE',
      quantity: qty.toFixed(4),
      unit,
      productId,
      note: `${type} issue`,
      occurredAt: args.occurredAt,
    });
    return;
  }

  if (type === 'RETURN') {
    const outcome = String(contract.goodsOutcome ?? '').toUpperCase();
    if (outcome !== 'TO_STOCK' || !(qty > 0)) return;
    await recordInventoryMovement(client, {
      companyId: args.companyId,
      momentId: args.momentId,
      sourceEventId: args.eventId,
      kind: 'RETURN',
      quantity: qty.toFixed(4),
      unit,
      productId,
      note: 'Return to stock',
      occurredAt: args.occurredAt,
    });
  }
}

/** Net quantity for a product on a moment. Null when no movements exist (not zero). */
export async function reconcileInventoryQuantity(
  client: PoolClient,
  momentId: string,
  productId: string | null
): Promise<number | null> {
  const exists = await client.query(
    `SELECT 1 FROM business.business_inventory_movement
     WHERE moment_id = $1 AND ($2::uuid IS NULL OR product_id = $2::uuid)
     LIMIT 1`,
    [momentId, productId]
  );
  if ((exists.rowCount ?? 0) === 0) return null;

  const r = await client.query<{ net: string }>(
    `SELECT COALESCE(SUM(
       CASE movement_kind
         WHEN 'RECEIPT' THEN quantity
         WHEN 'TRANSFER_IN' THEN quantity
         WHEN 'RETURN' THEN quantity
         WHEN 'ADJUSTMENT' THEN quantity
         WHEN 'ISSUE' THEN -quantity
         WHEN 'TRANSFER_OUT' THEN -quantity
         ELSE 0
       END
     ), 0)::text AS net
     FROM business.business_inventory_movement
     WHERE moment_id = $1
       AND ($2::uuid IS NULL OR product_id = $2::uuid)`,
    [momentId, productId]
  );
  return Number(r.rows[0]?.net ?? 0);
}


export type PlannedInventoryMovement = {
  kind: InventoryMovementKind;
  quantity: string;
  unit: string | null;
  productId: string | null;
  locationId: string | null;
  note: string;
  ambiguous?: boolean;
  reason?: string;
};

/** Pure planner shared by live writes and historical backfill dry-runs. */
export function planInventoryMovements(args: {
  eventId: string;
  eventType: string;
  quantity: string | null;
  unit?: string | null;
  payload?: Record<string, unknown> | null;
  title?: string | null;
}): PlannedInventoryMovement[] {
  const contract =
    args.payload?.contract && typeof args.payload.contract === 'object'
      ? (args.payload.contract as Record<string, unknown>)
      : {};
  const productId =
    asUuid(args.payload?.masterId) ??
    asUuid((args.payload?.master as Record<string, unknown> | undefined)?.masterId) ??
    null;
  const master = args.payload?.master as Record<string, unknown> | undefined;
  const unit =
    args.unit ?? (typeof master?.unit === 'string' ? String(master.unit) : null);
  const qty = qtyOf(args.quantity);
  const type = args.eventType.toUpperCase();
  const out: PlannedInventoryMovement[] = [];

  if (type === 'STOCK') {
    const action = String(contract.stockAction ?? '').toUpperCase();
    if (action === 'TRANSFER') {
      if (!(qty > 0)) return [{ kind: 'ADJUSTMENT', quantity: '0', unit, productId, locationId: null, note: '', ambiguous: true, reason: 'transfer_missing_qty' }];
      out.push({
        kind: 'TRANSFER_OUT',
        quantity: qty.toFixed(4),
        unit,
        productId,
        locationId: asUuid(contract.transferFromMasterId),
        note: 'Stock transfer out',
      });
      out.push({
        kind: 'TRANSFER_IN',
        quantity: qty.toFixed(4),
        unit,
        productId,
        locationId: asUuid(contract.transferToMasterId),
        note: 'Stock transfer in',
      });
      return out;
    }
    if (action === 'ADD' && qty > 0) {
      out.push({ kind: 'RECEIPT', quantity: qty.toFixed(4), unit, productId, locationId: null, note: 'Stock add' });
      return out;
    }
    if (action === 'REDUCE' && qty > 0) {
      out.push({ kind: 'ISSUE', quantity: qty.toFixed(4), unit, productId, locationId: null, note: 'Stock reduce' });
      return out;
    }
    if (action === 'SET' && qty !== 0) {
      out.push({
        kind: 'ADJUSTMENT',
        quantity: Math.abs(qty).toFixed(4),
        unit,
        productId,
        locationId: null,
        note: `Stock set to ${Math.abs(qty).toFixed(4)}`,
      });
      return out;
    }
    return [];
  }

  if (type === 'PURCHASE') {
    const receiptState = String(contract.receiptState ?? '').toUpperCase();
    const received = qtyOf(
      typeof contract.quantityReceived === 'string' ? contract.quantityReceived : args.quantity
    );
    if (receiptState === 'AWAITING' || !(received > 0)) return [];
    out.push({ kind: 'RECEIPT', quantity: received.toFixed(4), unit, productId, locationId: null, note: 'Purchase receipt' });
    return out;
  }

  if (type === 'PRODUCTION') {
    const entryMode = String(contract.entryMode ?? 'QUICK').toUpperCase();
    if (entryMode !== 'ITEMIZED') return [];
    const acceptedRaw =
      contract.acceptedQuantity != null && String(contract.acceptedQuantity) !== ''
        ? qtyOf(String(contract.acceptedQuantity))
        : qty;
    if (acceptedRaw > 0) {
      out.push({
        kind: 'RECEIPT',
        quantity: acceptedRaw.toFixed(4),
        unit,
        productId,
        locationId: null,
        note: 'Finished goods from production',
      });
    }
    const bom = Array.isArray(contract.bomLines) ? contract.bomLines : [];
    for (const line of bom) {
      if (!line || typeof line !== 'object') continue;
      const rec = line as Record<string, unknown>;
      const bq = qtyOf(typeof rec.quantity === 'string' || typeof rec.quantity === 'number' ? String(rec.quantity) : null);
      if (!(bq > 0)) continue;
      out.push({
        kind: 'ISSUE',
        quantity: bq.toFixed(4),
        unit: typeof rec.unit === 'string' ? rec.unit : unit,
        productId: asUuid(rec.masterId) ?? asUuid(rec.productId),
        locationId: null,
        note: `BOM consume ${String(rec.itemName ?? 'material')}`,
      });
    }
    return out;
  }

  if (type === 'SALE' || type === 'SHIPMENT' || type === 'DELIVERY') {
    if (!(qty > 0)) return [];
    out.push({ kind: 'ISSUE', quantity: qty.toFixed(4), unit, productId, locationId: null, note: `${type} issue` });
    return out;
  }

  if (type === 'RETURN') {
    const outcome = String(contract.goodsOutcome ?? '').toUpperCase();
    if (outcome !== 'TO_STOCK' || !(qty > 0)) return [];
    out.push({ kind: 'RETURN', quantity: qty.toFixed(4), unit, productId, locationId: null, note: 'Return to stock' });
    return out;
  }

  return [];
}
