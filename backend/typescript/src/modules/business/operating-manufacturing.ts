/**
 * Standard production and made-to-order rules on the existing operating event.
 * An order is not a sale. Recorded output is not accepted stock. A receipt is not a purchase twice.
 */

import type { OperatingContract } from './operating-retail-contract';
import { stampPostedGst, type GstPostingContext } from './operating-gst';

export const MANUFACTURING_SUBTYPES = ['STANDARD_PRODUCTION', 'MADE_TO_ORDER'] as const;

export interface ManufacturingLine {
  itemName: string;
  quantity?: string | null;
  unit?: string | null;
  unitPrice?: string | null;
}

export interface ManufacturingEvent {
  operatingEventId: string;
  eventType: string;
  title: string;
  amount: string | null;
  paymentStatus: string;
  approvalStatus: string;
  occurredAt: string;
  parentEventId?: string | null;
  quantity?: string | null;
  unit?: string | null;
  counterpartyName?: string | null;
  lines?: ManufacturingLine[] | null;
  contract?: OperatingContract | null;
  rejectedQuantity?: string | null;
  dueAt?: string | null;
}

export interface ManufacturingWrite {
  eventType: string;
  title: string;
  amount: string | null;
  counterpartyName?: string | null;
  channelName?: string | null;
  occurredAt?: string | null;
  parentEventId?: string | null;
  quantity?: string | null;
  dueAt?: string | null;
  lines?: ManufacturingLine[] | null;
  contract?: OperatingContract | null;
  unit?: string | null;
  rejectedQuantity?: string | null;
  gst?: GstPostingContext | null;
  subtype?: string | null;
}

export interface ManufacturingWriteResult {
  ok: true;
  paymentStatus: string;
  amount: string | null;
  parentEventId: string | null;
  quantity: string | null;
  contract: OperatingContract;
  settleChild: { type: 'COLLECTION' | 'PAYMENT'; amount: string } | null;
}

export interface ManufacturingStock {
  item: string;
  unit: string | null;
  kind: 'MATERIAL' | 'FINISHED';
  onHand: number;
}

function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function money(value: number): string {
  return value.toFixed(4);
}

function fail(message: string): { ok: false; message: string } {
  return { ok: false, message };
}

function named(value: string | null | undefined): boolean {
  return String(value ?? '').trim().length > 0;
}

function live(row: ManufacturingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

export function isManufacturingSubtype(subtype: string | null | undefined): boolean {
  return (MANUFACTURING_SUBTYPES as readonly string[]).includes(String(subtype ?? '').toUpperCase());
}

export function recordedOutput(row: ManufacturingEvent): number {
  if (row.quantity != null && row.quantity !== '') return num(row.quantity);
  return num(row.amount);
}

/** Finished stock and the accepted-units target use only an explicit accepted quantity. */
export function explicitAccepted(row: ManufacturingEvent): number | null {
  const raw = row.contract?.acceptedQuantity;
  if (raw == null || String(raw).trim() === '') return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

function share(row: ManufacturingEvent, parentId: string): number {
  const parts = row.contract?.allocations ?? [];
  if (parts.length > 0) {
    return parts.reduce((total, part) => (part.sourceId === parentId ? total + num(part.amount) : total), 0);
  }
  return row.parentEventId === parentId ? num(row.amount) : 0;
}

function remaining(row: ManufacturingEvent, events: ManufacturingEvent[], childType: 'COLLECTION' | 'PAYMENT'): number {
  if (!live(row) || ['COLLECTED', 'PAID', 'SETTLED', 'CANCELLED'].includes(row.paymentStatus)) return 0;
  const used = events.reduce((total, child) => {
    if (child.eventType !== childType || !live(child)) return total;
    return total + share(child, row.operatingEventId);
  }, 0);
  const credited = events.reduce((total, child) => {
    if (child.eventType !== 'RETURN' || !live(child) || child.parentEventId !== row.operatingEventId) return total;
    return total + num(child.amount);
  }, 0);
  return Math.max(0, num(row.amount) - used - credited);
}

export function validateManufacturingWrite(
  input: ManufacturingWrite,
  existing: ManufacturingEvent[]
): ManufacturingWriteResult | { ok: false; message: string } {
  const contract: OperatingContract = { ...(input.contract ?? {}) };
  if (input.eventType === 'CUSTOMER_ORDER') return validateOrder(input, contract);
  if (input.eventType === 'PURCHASE') return validatePurchase(input, existing, contract);
  if (input.eventType === 'PRODUCTION') return validateProduction(input, existing, contract);
  if (input.eventType === 'SALE') return validateSale(input, existing, contract);
  if (input.eventType === 'DELIVERY') return validateDelivery(input, existing, contract);
  if (input.eventType === 'COLLECTION') return validateCollection(input, existing, contract);
  if (input.eventType === 'PAYMENT') return validatePayment(input, existing, contract);
  if (input.eventType === 'SPEND') return validateSpend(input, contract);
  if (input.eventType === 'STOCK') return validateStock(input, contract);
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: input.amount,
    parentEventId: input.parentEventId ?? null,
    quantity: input.quantity ?? null,
    contract,
    settleChild: null,
  };
}

function postedValue(row: ManufacturingEvent): number {
  const taxable = row.contract?.gst?.taxableAmount;
  if (taxable != null && taxable !== '') return num(taxable);
  return num(row.amount);
}

function withPostedGst(
  input: ManufacturingWrite,
  contract: OperatingContract,
  side: 'SALE' | 'PURCHASE',
  entered: number
): { ok: true; amount: number } | { ok: false; message: string } {
  return stampPostedGst({
    gst: input.gst,
    side,
    entered,
    title: input.title,
    lines: (input.lines ?? []).map((line) => ({ ...line, gstRate: contract.gstRate, taxMode: contract.taxMode === 'NOT_CONFIGURED' ? null : contract.taxMode })),
    partyState: contract.placeOfSupply,
    contract,
  });
}

function validateOrder(input: ManufacturingWrite, contract: OperatingContract): ManufacturingWriteResult | { ok: false; message: string } {
  if (input.subtype === 'STANDARD_PRODUCTION' && contract.quotationStatus) {
    return fail('Standard production does not use a quotation.');
  }
  if (!named(input.counterpartyName)) return fail('Buyer is required.');
  if (contract.quotationStatus && contract.quotationStatus !== 'ACCEPTED') {
    if (input.amount && !(num(input.amount) > 0)) return fail('Order value must be a positive amount.');
    if (input.amount) contract.estimatedAmount = money(num(input.amount));
    return {
      ok: true,
      paymentStatus: 'NONE',
      amount: input.amount ? money(num(input.amount)) : null,
      parentEventId: null,
      quantity: input.quantity ?? null,
      contract,
      settleChild: null,
    };
  }
  if (contract.quotationStatus === 'ACCEPTED') contract.orderStatus = contract.orderStatus ?? 'PLACED';
  if (!input.occurredAt) return fail('Order date is required.');
  if (input.amount && !(num(input.amount) > 0)) return fail('Order value must be a positive amount.');
  if (input.amount) contract.estimatedAmount = money(num(input.amount));
  contract.orderStatus = contract.orderStatus ?? 'PLACED';
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: input.amount ? money(num(input.amount)) : null,
    parentEventId: null,
    quantity: input.quantity ?? null,
    contract,
    settleChild: null,
  };
}

function validatePurchase(input: ManufacturingWrite, existing: ManufacturingEvent[], contract: OperatingContract): ManufacturingWriteResult | { ok: false; message: string } {
  const parent = input.parentEventId ? existing.find((row) => row.operatingEventId === input.parentEventId) ?? null : null;
  if (parent?.eventType === 'PURCHASE') {
    const qty = num(contract.quantityReceived ?? input.quantity);
    if (!(qty > 0)) return fail('Quantity received is required.');
    contract.receiptState = 'PART_RECEIVED';
    contract.quantityReceived = money(qty);
    return {
      ok: true,
      paymentStatus: 'NONE',
      amount: null,
      parentEventId: parent.operatingEventId,
      quantity: money(qty),
      contract,
      settleChild: null,
    };
  }
  if (!named(input.counterpartyName)) return fail('Supplier is required.');
  const lines = input.lines ?? [];
  let amount = lines.length > 0 && lineTotal(lines) > 0 ? lineTotal(lines) : num(input.amount);
  const stamped = withPostedGst(input, contract, 'PURCHASE', amount);
  if (!stamped.ok) return stamped;
  amount = stamped.amount;
  if (!(amount > 0)) return fail('Amount is required.');
  if (contract.receiptState !== 'RECEIVED' && contract.receiptState !== 'PART_RECEIVED' && contract.receiptState !== 'AWAITING') {
    return fail('Choose whether materials were received.');
  }
  if (contract.receiptState === 'PART_RECEIVED' && !(num(contract.quantityReceived) > 0)) return fail('Quantity received is required.');
  const state = contract.paymentState === 'RECEIVED' ? 'PAID' : contract.paymentState;
  if (state !== 'PAID' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose paid, part paid, or pay later.');
  const paid = state === 'PAID' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (state === 'PART_PAID' && !(paid > 0 && paid < amount)) return fail('Part paid must be more than zero and less than the total.');
  contract.paymentState = state;
  contract.receivedAmount = money(paid);
  return {
    ok: true,
    paymentStatus: state === 'PAID' ? 'PAID' : 'PENDING',
    amount: money(amount),
    parentEventId: null,
    quantity: contract.receiptState === 'AWAITING' ? null : contract.quantityReceived ?? input.quantity ?? null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'PAYMENT', amount: money(paid) } : null,
  };
}

function validateProduction(input: ManufacturingWrite, existing: ManufacturingEvent[], contract: OperatingContract): ManufacturingWriteResult | { ok: false; message: string } {
  const output = input.quantity != null && input.quantity !== '' ? num(input.quantity) : num(input.amount);
  if (!(output > 0)) return fail('Output quantity is required.');
  const detailed = contract.entryMode === 'ITEMIZED';
  if (!detailed) {
    contract.entryMode = 'QUICK';
    contract.acceptedQuantity = null;
    return {
      ok: true,
      paymentStatus: 'NONE',
      amount: null,
      parentEventId: input.parentEventId ?? null,
      quantity: money(output),
      contract,
      settleChild: null,
    };
  }
  if (!named(input.title)) return fail('Finished product is required for a detailed batch.');
  const accepted = contract.acceptedQuantity == null || contract.acceptedQuantity === '' ? null : num(contract.acceptedQuantity);
  const rejected = input.rejectedQuantity == null || input.rejectedQuantity === '' ? null : num(input.rejectedQuantity);
  if (accepted != null && rejected != null && accepted + rejected - output > 0.00005) {
    return fail('Accepted and rejected quantities cannot exceed completed output.');
  }
  if (accepted != null && (accepted < 0 || accepted > output)) return fail('Accepted quantity cannot exceed completed output.');
  const scrap = contract.scrapQuantity == null || contract.scrapQuantity === '' ? 0 : num(contract.scrapQuantity);
  if (scrap < 0) return fail('Scrap quantity cannot be negative.');
  const parentRun = input.parentEventId ? existing.find((row) => row.operatingEventId === input.parentEventId) ?? null : null;
  if (parentRun?.eventType === 'PRODUCTION') {
    const rejected = num(parentRun.rejectedQuantity);
    const used = existing.reduce((total, row) => {
      if (row.eventType !== 'PRODUCTION' || row.parentEventId !== parentRun.operatingEventId || !live(row)) return total;
      return total + num(row.contract?.acceptedQuantity) + num(row.contract?.scrapQuantity);
    }, 0);
    if (rejected > 0 && (accepted ?? 0) + scrap + used - rejected > 0.00005) {
      return fail('Rework cannot exceed the rejected quantity.');
    }
  }
  if (accepted != null) contract.acceptedQuantity = money(accepted);
  if (scrap > 0) contract.scrapQuantity = money(scrap);
  contract.productionStatus = contract.productionStatus ?? 'COMPLETED';
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: input.parentEventId ?? null,
    quantity: money(output),
    contract,
    settleChild: null,
  };
}

function validateSale(
  input: ManufacturingWrite,
  existing: ManufacturingEvent[],
  contract: OperatingContract
): ManufacturingWriteResult | { ok: false; message: string } {
  const lines = input.lines ?? [];
  const itemized = contract.entryMode === 'ITEMIZED';
  let amount = itemized && lineTotal(lines) > 0 ? lineTotal(lines) : num(input.amount);
  const stamped = withPostedGst(input, contract, 'SALE', amount);
  if (!stamped.ok) return stamped;
  amount = stamped.amount;
  if (!(amount > 0)) return fail('Sale amount is required.');
  const state = contract.paymentState;
  if (state !== 'RECEIVED' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose received, part paid, or pay later.');
  const received = state === 'RECEIVED' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (state === 'PART_PAID' && !(received > 0 && received < amount)) return fail('Part paid must be more than zero and less than the total.');
  if (received < amount && !named(input.counterpartyName)) return fail('A named buyer is required when a balance remains.');
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent || parent.eventType !== 'CUSTOMER_ORDER') return fail('Link the sale to the customer order.');
    if (parent.contract?.quotationStatus && parent.contract.quotationStatus !== 'ACCEPTED') {
      return fail('Invoice a confirmed order.');
    }
    const cap = num(parent.contract?.estimatedAmount || parent.amount);
    if (cap > 0) {
      const used = existing.reduce((total, row) => {
        if (row.eventType !== 'SALE' || row.parentEventId !== parent.operatingEventId || !live(row)) return total;
        return total + num(row.contract?.gst?.invoiceTotal || row.amount);
      }, 0);
      if (used + amount - cap > 0.00005) return fail('Invoices cannot exceed the confirmed order.');
    }
  }
  contract.entryMode = itemized ? 'ITEMIZED' : 'QUICK';
  contract.receivedAmount = money(received);
  return {
    ok: true,
    paymentStatus: state === 'RECEIVED' ? 'COLLECTED' : 'PENDING',
    amount: money(amount),
    parentEventId: input.parentEventId ?? null,
    quantity: itemized
      ? money(lines.reduce((total, line) => total + num(line.quantity), 0))
      : input.quantity
        ? money(num(input.quantity))
        : null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'COLLECTION', amount: money(received) } : null,
  };
}

function validateDelivery(
  input: ManufacturingWrite,
  existing: ManufacturingEvent[],
  contract: OperatingContract
): ManufacturingWriteResult | { ok: false; message: string } {
  const parent = input.parentEventId ? existing.find((row) => row.operatingEventId === input.parentEventId) ?? null : null;
  if (!parent || (parent.eventType !== 'CUSTOMER_ORDER' && parent.eventType !== 'SALE')) {
    return fail('Choose the customer order or sale being fulfilled.');
  }
  if (!contract.deliveryStatus) return fail('Choose a delivery status.');
  const itemized = contract.entryMode === 'ITEMIZED';
  if (!itemized) {
    contract.entryMode = 'QUICK';
    return {
      ok: true,
      paymentStatus: 'NONE',
      amount: null,
      parentEventId: parent.operatingEventId,
      quantity: null,
      contract,
      settleChild: null,
    };
  }
  const qty = num(input.quantity);
  if (!(qty > 0)) return fail('Delivered quantity is required.');
  const already = existing.reduce((total, row) => {
    if (row.eventType !== 'DELIVERY' || row.parentEventId !== parent.operatingEventId || !live(row)) return total;
    if (row.contract?.entryMode !== 'ITEMIZED') return total;
    return total + num(row.quantity);
  }, 0);
  const ordered = num(parent.quantity);
  if (ordered > 0 && already + qty - ordered > 0.00005) return fail('Delivered quantity cannot exceed the open order.');
  contract.entryMode = 'ITEMIZED';
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: parent.operatingEventId,
    quantity: money(qty),
    contract,
    settleChild: null,
  };
}

function validateCollection(
  input: ManufacturingWrite,
  existing: ManufacturingEvent[],
  contract: OperatingContract
): ManufacturingWriteResult | { ok: false; message: string } {
  const amount = num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  if (!named(input.counterpartyName)) return fail('Buyer is required.');
  const parent = input.parentEventId ? existing.find((row) => row.operatingEventId === input.parentEventId) ?? null : null;
  if (input.parentEventId && !parent) return fail('Choose a record on this moment.');
  if (parent?.eventType === 'COLLECTION') {
    contract.advanceApplied = true;
    return {
      ok: true,
      paymentStatus: 'COLLECTED',
      amount: money(amount),
      parentEventId: parent.operatingEventId,
      quantity: null,
      contract,
      settleChild: null,
    };
  }
  if (parent && parent.eventType !== 'SALE' && parent.eventType !== 'INVOICE' && parent.eventType !== 'CUSTOMER_ORDER') {
    return fail('A collection applies to a sale or stays an identifiable advance.');
  }
  if (parent && (parent.eventType === 'SALE' || parent.eventType === 'INVOICE')) {
    const open = remaining(parent, existing, 'COLLECTION');
    if (amount - open > 0.00005) return fail('Collection cannot exceed the open balance.');
  }
  return {
    ok: true,
    paymentStatus: 'COLLECTED',
    amount: money(amount),
    parentEventId: parent?.operatingEventId ?? null,
    quantity: null,
    contract,
    settleChild: null,
  };
}

function validatePayment(
  input: ManufacturingWrite,
  existing: ManufacturingEvent[],
  contract: OperatingContract
): ManufacturingWriteResult | { ok: false; message: string } {
  const amount = num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  const parent = input.parentEventId ? existing.find((row) => row.operatingEventId === input.parentEventId) ?? null : null;
  if (!parent || (parent.eventType !== 'PURCHASE' && parent.eventType !== 'SPEND')) return fail('Choose the purchase or expense being paid.');
  const open = remaining(parent, existing, 'PAYMENT');
  if (amount - open > 0.00005) return fail('Payment cannot exceed the open balance.');
  return {
    ok: true,
    paymentStatus: 'PAID',
    amount: money(amount),
    parentEventId: parent.operatingEventId,
    quantity: null,
    contract,
    settleChild: null,
  };
}

function validateSpend(input: ManufacturingWrite, contract: OperatingContract): ManufacturingWriteResult | { ok: false; message: string } {
  const stamped = withPostedGst(input, contract, 'PURCHASE', num(input.amount));
  if (!stamped.ok) return stamped;
  const amount = stamped.amount;
  if (!(amount > 0)) return fail('Amount is required.');
  const state = contract.paymentState === 'RECEIVED' ? 'PAID' : contract.paymentState;
  if (state !== 'PAID' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose paid, part paid, or pay later.');
  if (state !== 'PAID' && !named(input.counterpartyName)) return fail('Payee is required when the expense is not fully paid.');
  contract.paymentState = state;
  return {
    ok: true,
    paymentStatus: state === 'PAID' ? 'PAID' : 'PENDING',
    amount: money(amount),
    parentEventId: null,
    quantity: null,
    contract,
    settleChild: null,
  };
}

function validateStock(input: ManufacturingWrite, contract: OperatingContract): ManufacturingWriteResult | { ok: false; message: string } {
  if (!named(input.title)) return fail('Item is required.');
  if (contract.stockAction !== 'SET' && contract.stockAction !== 'ADD' && contract.stockAction !== 'REDUCE') {
    return fail('Choose set, add, or reduce.');
  }
  if (!contract.stockReason) return fail('A stock reason is required.');
  const qty = num(input.quantity);
  if (contract.stockAction !== 'SET' && !(qty > 0)) return fail('Quantity is required.');
  if (contract.stockAction === 'SET') contract.declaredQuantity = money(qty);
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: money(qty),
    contract,
    settleChild: null,
  };
}

function lineTotal(lines: ManufacturingLine[]): number {
  return lines.reduce((total, line) => total + num(line.quantity) * num(line.unitPrice), 0);
}

export function manufacturingStock(events: ManufacturingEvent[]): ManufacturingStock[] {
  const positions = new Map<string, ManufacturingStock>();
  const touch = (item: string, unit: string | null, kind: ManufacturingStock['kind']) => {
    const key = `${kind}:${item.trim().toLowerCase()}:${unit ?? ''}`;
    const current = positions.get(key) ?? { item: item.trim(), unit, kind, onHand: 0 };
    positions.set(key, current);
    return current;
  };
  for (const row of events.filter(live)) {
    if (row.eventType === 'STOCK' && row.contract?.stockAction) {
      const position = touch(row.title, row.unit ?? null, 'MATERIAL');
      const qty = num(row.contract.declaredQuantity ?? row.quantity);
      if (row.contract.stockAction === 'SET') position.onHand = qty;
      else if (row.contract.stockAction === 'ADD') position.onHand += num(row.quantity);
      else position.onHand -= num(row.quantity);
      continue;
    }
    if (row.eventType === 'PURCHASE' && row.parentEventId && row.contract?.receiptState === 'PART_RECEIVED') {
      const source = events.find((parent) => parent.operatingEventId === row.parentEventId);
      const line = source?.lines?.[0];
      touch(line?.itemName || row.title, line?.unit ?? row.unit ?? null, 'MATERIAL').onHand += num(row.contract.quantityReceived ?? row.quantity);
      continue;
    }
    if (row.eventType === 'PURCHASE' && row.contract?.receiptState && row.contract.receiptState !== 'AWAITING') {
      const lines = row.lines ?? [];
      if (lines.length === 0) continue;
      const received = row.contract.receiptState === 'PART_RECEIVED' ? num(row.contract.quantityReceived) : lines.reduce((total, line) => total + num(line.quantity), 0);
      let left = received;
      for (const line of lines) {
        const take = row.contract.receiptState === 'PART_RECEIVED' ? Math.min(num(line.quantity) || left, left) : num(line.quantity);
        if (!(take > 0)) continue;
        touch(line.itemName, line.unit ?? null, 'MATERIAL').onHand += take;
        left -= take;
      }
    }
    if (row.eventType === 'PRODUCTION' && row.contract?.entryMode === 'ITEMIZED') {
      const accepted = explicitAccepted(row);
      if (accepted != null && accepted > 0 && named(row.title)) {
        touch(row.title, row.unit ?? null, 'FINISHED').onHand += accepted;
      }
    }
    if (row.eventType === 'RETURN' && row.contract?.goodsOutcome === 'TO_STOCK') {
      for (const line of row.lines ?? []) {
        touch(line.itemName, line.unit ?? null, 'FINISHED').onHand += num(line.quantity);
      }
    }
  }
  return [...positions.values()];
}

export interface ManufacturingMeasure {
  progress: number;
  coverage: string | null;
}

/** One target basis. Unlike units stay unadded and are reported as not recorded. */
export function manufacturingMeasure(events: ManufacturingEvent[], metric: string): ManufacturingMeasure {
  const rows = events.filter(live);
  if (metric === 'REVENUE') {
    const sales = rows.filter((row) => row.eventType === 'SALE');
    const ids = new Set(sales.map((row) => row.operatingEventId));
    const gross = sales.reduce((total, row) => total + postedValue(row), 0);
    const credits = rows
      .filter((row) => row.eventType === 'RETURN' && row.parentEventId && ids.has(row.parentEventId))
      .reduce((total, row) => total + postedValue(row), 0);
    return { progress: Math.max(0, gross - credits), coverage: sales.length ? null : 'not recorded' };
  }
  if (metric === 'ORDERS') {
    const orders = rows.filter((row) => row.eventType === 'CUSTOMER_ORDER' && row.contract?.orderStatus !== 'CANCELLED');
    return { progress: orders.length, coverage: orders.length ? null : 'not recorded' };
  }
  if (metric === 'PRODUCTION_OUTPUT') {
    return quantityBasis(
      rows.filter((row) => row.eventType === 'PRODUCTION' && explicitAccepted(row) != null),
      (row) => explicitAccepted(row) ?? 0
    );
  }
  if (metric === 'DELIVERY_COUNT') {
    return quantityBasis(
      rows.filter((row) => row.eventType === 'DELIVERY' && row.contract?.entryMode === 'ITEMIZED'),
      (row) => num(row.quantity)
    );
  }
  return { progress: 0, coverage: 'not recorded' };
}

function quantityBasis(rows: ManufacturingEvent[], value: (row: ManufacturingEvent) => number): ManufacturingMeasure {
  if (rows.length === 0) return { progress: 0, coverage: 'not recorded' };
  const units = new Set(rows.map((row) => String(row.unit ?? '').trim().toLowerCase()));
  if (units.size > 1) return { progress: 0, coverage: 'N/A' };
  return { progress: rows.reduce((total, row) => total + value(row), 0), coverage: null };
}
