/**
 * Trading wholesale and distribution dealership rules on the operating event.
 * Orders and shipments stay commitments. Only a posted sale is revenue.
 * Consignment is never owned on-hand stock.
 */

import {
  budgetAttention,
  moneyMovingDetail,
  obligationRemaining,
  parseOperatingContract,
  salesCoverage,
  stockPositions as ownedStockPositions,
  type OperatingContract,
  type RetailAttention,
  type RetailEvent,
  type RetailLine,
  type RetailWrite,
  type RetailWriteResult,
  type StockPosition,
} from './operating-retail-contract';

export const TRADING_SUBTYPES = ['TRADING_WHOLESALE', 'DISTRIBUTION_DEALERSHIP'] as const;

export type TradingEvent = RetailEvent;
export type TradingWrite = RetailWrite;
export type TradingAttention = RetailAttention;
export type TradingLine = RetailLine;

const DUE_SOON_DAYS = 3;

function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function money(value: number): string {
  return value.toFixed(4);
}

function day(value: string | null | undefined): string {
  return String(value ?? '').slice(0, 10);
}

function effective(row: TradingEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function namedParty(value: string | null | undefined): boolean {
  const name = String(value ?? '').trim().toLowerCase();
  return name.length > 0 && !['walk-in', 'anonymous', 'guest', 'walk in'].includes(name);
}

function itemKey(value: string): string {
  return value.trim().toLowerCase();
}

function fail(message: string): { ok: false; message: string } {
  return { ok: false, message };
}

function lineTotal(lines: TradingLine[] | null | undefined): number {
  return (lines ?? []).reduce((total, line) => total + num(line.quantity ?? 1) * num(line.unitPrice), 0);
}

function addDays(dayText: string, days: number): string {
  const date = new Date(`${dayText}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function trim(value: number): string {
  return Number(value.toFixed(2)).toString();
}

const SEVERITY_RANK: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

function sortAttention(cards: TradingAttention[]): TradingAttention[] {
  const kindRank = (kind: string): number => {
    if (kind.startsWith('OVERDUE')) return 0;
    if (kind.startsWith('DUE')) return 1;
    if (kind === 'OVERDUE_SHIPMENT' || kind === 'UNFULFILLED') return 2;
    if (kind === 'LOW_STOCK' || kind === 'BUDGET_WARNING' || kind === 'BUDGET_OVERRUN') return 3;
    if (kind === 'APPROVAL') return 4;
    return 5;
  };
  return [...cards].sort((a, b) => {
    const sev = (SEVERITY_RANK[a.severity ?? ''] ?? 9) - (SEVERITY_RANK[b.severity ?? ''] ?? 9);
    if (sev !== 0) return sev;
    return kindRank(a.kind) - kindRank(b.kind);
  });
}

export function isTradingSubtype(subtype: string | null | undefined): boolean {
  return (TRADING_SUBTYPES as readonly string[]).includes(String(subtype ?? '').toUpperCase());
}

export { parseOperatingContract, moneyMovingDetail, salesCoverage, budgetAttention, obligationRemaining };

export function validateTradingWrite(
  input: TradingWrite,
  existing: TradingEvent[]
): RetailWriteResult | { ok: false; message: string } {
  const contract: OperatingContract = { ...(input.contract ?? {}) };
  if (input.eventType === 'SALE') return validateSale(input, existing, contract);
  if (input.eventType === 'PURCHASE') return validatePurchase(input, contract);
  if (input.eventType === 'SPEND') return validateExpense(input, contract);
  if (input.eventType === 'COLLECTION' || input.eventType === 'PAYMENT') return validateSettlement(input, existing, contract);
  if (input.eventType === 'STOCK') return validateStock(input, contract);
  if (input.eventType === 'ORDER') return validateOrder(input, contract);
  if (input.eventType === 'SHIPMENT' || input.eventType === 'DELIVERY') return validateShipment(input, existing, contract);
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

function validateSale(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  const mode = contract.entryMode;
  if (mode !== 'QUICK' && mode !== 'ITEMIZED') return fail('Choose quick amount or itemized.');
  if (!String(input.channelName ?? '').trim()) return fail('Sales channel is required.');
  const lines = input.lines ?? [];
  let amount = num(input.amount);
  if (mode === 'ITEMIZED') {
    if (lines.length === 0) return fail('Add at least one item.');
    for (const line of lines) {
      if (!line.itemName.trim() || !(num(line.quantity) > 0) || !String(line.unit ?? '').trim() || !(num(line.unitPrice) > 0)) {
        return fail('Each item needs a name, quantity, unit, and price.');
      }
    }
    amount = lineTotal(lines);
  }
  if (!(amount > 0)) return fail('Amount is required.');
  const state = contract.paymentState;
  if (state !== 'RECEIVED' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose received, part paid, or pay later.');
  const received = state === 'RECEIVED' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (state === 'PART_PAID' && !(received > 0 && received < amount)) return fail('Part paid must be more than zero and less than the total.');
  if (received < amount && !namedParty(input.counterpartyName)) return fail('A named buyer is required when a balance remains.');
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent) return fail('Choose a record on this moment.');
    if (!['ORDER', 'SHIPMENT', 'DELIVERY'].includes(parent.eventType)) return fail('That record cannot be linked here.');
    const billed = existing.some(
      (row) => row.parentEventId === parent.operatingEventId && row.eventType === 'SALE' && effective(row)
    );
    if (billed) return fail('This already has a posted sale.');
  }
  contract.receivedAmount = money(received);
  return {
    ok: true,
    paymentStatus: state === 'RECEIVED' ? 'COLLECTED' : 'PENDING',
    amount: money(amount),
    parentEventId: input.parentEventId ?? null,
    quantity: mode === 'ITEMIZED' ? money(lines.reduce((total, line) => total + num(line.quantity), 0)) : null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'COLLECTION', amount: money(received) } : null,
  };
}

function validatePurchase(input: TradingWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!namedParty(input.counterpartyName)) return fail('Supplier is required.');
  const lines = input.lines ?? [];
  const amount = lines.length > 0 && lineTotal(lines) > 0 ? lineTotal(lines) : num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  if (contract.receiptState !== 'RECEIVED' && contract.receiptState !== 'PART_RECEIVED' && contract.receiptState !== 'AWAITING') {
    return fail('Choose whether goods were received.');
  }
  if (contract.receiptState === 'PART_RECEIVED' && !(num(contract.quantityReceived) > 0)) return fail('Quantity received is required.');
  if (contract.ownership !== 'OWNED' && contract.ownership !== 'CONSIGNMENT') contract.ownership = 'OWNED';
  const state = contract.paymentState === 'PAID' || contract.paymentState === 'RECEIVED' ? 'PAID' : contract.paymentState;
  if (state !== 'PAID' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose paid, part paid, or pay later.');
  const paid = state === 'PAID' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (paid - amount > 0.00005) return fail('Paid amount cannot exceed the total.');
  if (state === 'PART_PAID' && !(paid > 0 && paid < amount)) return fail('Part paid must be more than zero and less than the total.');
  contract.paymentState = state;
  contract.receivedAmount = money(paid);
  return {
    ok: true,
    paymentStatus: state === 'PAID' ? 'PAID' : 'PENDING',
    amount: money(amount),
    parentEventId: null,
    quantity: contract.receiptState === 'AWAITING' ? '0.0000' : contract.quantityReceived ?? input.quantity ?? null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'PAYMENT', amount: money(paid) } : null,
  };
}

function validateExpense(input: TradingWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  const category = String(contract.expenseCategory ?? '').trim();
  if (!category) return fail('Expense category is required.');
  const amount = num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  const state = contract.paymentState === 'RECEIVED' ? 'PAID' : contract.paymentState;
  if (state !== 'PAID' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose paid, part paid, or pay later.');
  const paid = state === 'PAID' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (state !== 'PAID' && !namedParty(input.counterpartyName)) return fail('Payee is required when the expense is not fully paid.');
  if (state === 'PART_PAID' && !(paid > 0 && paid < amount)) return fail('Part paid must be more than zero and less than the total.');
  contract.expenseCategory = category;
  contract.paymentState = state;
  contract.receivedAmount = money(paid);
  return {
    ok: true,
    paymentStatus: state === 'PAID' ? 'PAID' : 'PENDING',
    amount: money(amount),
    parentEventId: null,
    quantity: null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'PAYMENT', amount: money(paid) } : null,
  };
}

function validateSettlement(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  const amount = num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  const childType = input.eventType === 'COLLECTION' ? 'COLLECTION' : 'PAYMENT';
  const parts = contract.allocations?.length
    ? contract.allocations
    : input.parentEventId
      ? [{ sourceId: input.parentEventId, amount: money(amount) }]
      : [];
  if (parts.length === 0) return fail('Choose the open record this settles.');
  const allocated = parts.reduce((total, part) => total + num(part.amount), 0);
  if (Math.abs(allocated - amount) > 0.00005) return fail('Allocations must add up to the amount.');
  const seen = new Set<string>();
  for (const part of parts) {
    if (seen.has(part.sourceId)) return fail('Each open record can be selected once.');
    seen.add(part.sourceId);
    const source = existing.find((row) => row.operatingEventId === part.sourceId);
    if (!source || !effective(source)) return fail('Choose an open posted record.');
    const allowed = childType === 'COLLECTION' ? ['SALE'] : ['PURCHASE', 'SPEND', 'VENDOR_COST'];
    if (!allowed.includes(source.eventType)) return fail('That record cannot be settled here.');
    const open = obligationRemaining(source, existing, childType);
    if (num(part.amount) - open > 0.00005) return fail('That is more than the amount still open.');
  }
  contract.allocations = parts.map((part) => ({ sourceId: part.sourceId, amount: money(num(part.amount)) }));
  return {
    ok: true,
    paymentStatus: childType === 'COLLECTION' ? 'COLLECTED' : 'PAID',
    amount: money(amount),
    parentEventId: parts[0]!.sourceId,
    quantity: null,
    contract,
    settleChild: null,
  };
}

function validateStock(input: TradingWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!input.title.trim()) return fail('Choose an item.');
  if (contract.stockAction !== 'SET' && contract.stockAction !== 'ADD' && contract.stockAction !== 'REDUCE') {
    return fail('Choose set, add, or reduce.');
  }
  if (!(num(input.quantity) >= 0) || (contract.stockAction !== 'SET' && !(num(input.quantity) > 0))) {
    return fail('Quantity is required.');
  }
  if (!contract.stockReason) return fail('Choose a reason.');
  if (contract.ownership !== 'OWNED' && contract.ownership !== 'CONSIGNMENT') contract.ownership = 'OWNED';
  if (contract.stockAction === 'SET') contract.declaredQuantity = money(num(input.quantity));
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: money(num(input.quantity)),
    contract,
    settleChild: null,
  };
}

function validateOrder(input: TradingWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!String(input.channelName ?? '').trim() && !namedParty(input.counterpartyName)) {
    return fail('Buyer or channel is required.');
  }
  if (!contract.orderStatus) contract.orderStatus = 'PLACED';
  if (!['PLACED', 'PREPARING', 'COMPLETED', 'CANCELLED'].includes(contract.orderStatus)) return fail('Choose an order status.');
  // Order amount is a commitment estimate — never revenue until billed as SALE.
  if (input.amount) contract.estimatedAmount = input.amount;
  if (input.dueAt) contract.promisedAt = input.dueAt;
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: input.quantity ?? null,
    contract,
    settleChild: null,
  };
}

function validateShipment(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  if (!(num(input.quantity) > 0)) return fail('Quantity is required.');
  if (!contract.shipmentStatus) {
    contract.shipmentStatus = input.eventType === 'DELIVERY' ? 'DELIVERED' : 'IN_TRANSIT';
  }
  if (!['PLACED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'].includes(contract.shipmentStatus)) {
    return fail('Choose a shipment status.');
  }
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent || parent.eventType !== 'ORDER') return fail('Link shipment to an order on this moment.');
  }
  if (input.dueAt) contract.promisedAt = input.dueAt;
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: input.parentEventId ?? null,
    quantity: money(num(input.quantity)),
    contract,
    settleChild: null,
  };
}

/** Owned on-hand only — consignment receipts never enter owned stock. */
export function tradingStockPositions(events: TradingEvent[]): StockPosition[] {
  const filtered = events.filter((row) => {
    if (!effective(row)) return false;
    if (row.contract?.ownership === 'CONSIGNMENT') return false;
    return true;
  });
  return ownedStockPositions(filtered);
}

export function ordersCoverage(events: TradingEvent[]): string | null {
  const sales = events.filter((row) => row.eventType === 'SALE' && effective(row));
  if (sales.length === 0) return null;
  const itemized = sales.filter((row) => row.contract?.entryMode === 'ITEMIZED').length;
  if (itemized === 0) return 'Line coverage not recorded';
  if (itemized === sales.length) return null;
  return `Line coverage ${itemized} of ${sales.length} posted sales`;
}

export function fulfilmentCoverage(events: TradingEvent[]): string | null {
  const orders = events.filter(
    (row) => row.eventType === 'ORDER' && effective(row) && row.contract?.orderStatus !== 'CANCELLED'
  );
  if (orders.length === 0) return null;
  let fulfilled = 0;
  for (const order of orders) {
    const ordered = num(order.quantity);
    if (!(ordered > 0)) continue;
    const shipped = events
      .filter(
        (row) =>
          row.parentEventId === order.operatingEventId &&
          (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
          effective(row) &&
          row.contract?.shipmentStatus !== 'CANCELLED'
      )
      .reduce((total, row) => total + num(row.quantity), 0);
    if (shipped + 0.00005 >= ordered) fulfilled += 1;
  }
  if (fulfilled === orders.length) return null;
  return `Fulfilment covers ${fulfilled} of ${orders.length} open orders`;
}

export function ordersCommitmentsSummary(events: TradingEvent[], visible: boolean, currency: string): string {
  const rows = events.filter((row) => row.approvalStatus !== 'REJECTED');
  const sales = rows.filter((row) => row.eventType === 'SALE' && effective(row));
  const openOrders = rows.filter(
    (row) =>
      row.eventType === 'ORDER' &&
      effective(row) &&
      row.contract?.orderStatus !== 'CANCELLED' &&
      row.contract?.orderStatus !== 'COMPLETED'
  );
  const openShipments = rows.filter(
    (row) =>
      (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
      effective(row) &&
      row.contract?.shipmentStatus !== 'DELIVERED' &&
      row.contract?.shipmentStatus !== 'CANCELLED'
  );
  const total = sales.reduce((sum, row) => sum + num(row.amount), 0);
  const moneyText = visible ? ` · ${money(total)} ${currency}` : '';
  return `Posted sales ${sales.length}${moneyText} · Open orders ${openOrders.length} · In transit ${openShipments.length}`;
}

export function peopleNetworkDetail(events: TradingEvent[], network: string[]): string {
  const buyers = new Set<string>();
  const suppliers = new Set<string>();
  for (const row of events) {
    if (!effective(row) || !namedParty(row.counterpartyName)) continue;
    const name = String(row.counterpartyName).trim();
    if (row.eventType === 'SALE' || row.eventType === 'COLLECTION' || row.eventType === 'ORDER') buyers.add(name);
    if (row.eventType === 'PURCHASE' || row.eventType === 'PAYMENT') suppliers.add(name);
  }
  const networkText = network.length > 0 ? ` · Listed ${network.join(', ')}` : '';
  return `Buyers ${buyers.size} · Suppliers ${suppliers.size}${networkText}`;
}

function unfulfilledShipmentQty(order: TradingEvent, events: TradingEvent[]): number {
  const ordered = num(order.quantity);
  if (!(ordered > 0)) return 0;
  const moved = events
    .filter(
      (row) =>
        row.parentEventId === order.operatingEventId &&
        (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
        effective(row) &&
        row.contract?.shipmentStatus !== 'CANCELLED'
    )
    .reduce((total, row) => total + num(row.quantity), 0);
  return Math.max(0, ordered - moved);
}

export function tradingAttention(
  events: TradingEvent[],
  visible: boolean,
  canApprove: boolean,
  asOf: Date
): TradingAttention[] {
  const today = asOf.toISOString().slice(0, 10);
  const soon = addDays(today, DUE_SOON_DAYS);
  const cards: TradingAttention[] = [];
  const push = (card: TradingAttention) => cards.push(card);

  for (const row of events) {
    if (row.approvalStatus === 'PENDING' && canApprove) {
      push({
        id: row.operatingEventId,
        title: row.title,
        kind: 'APPROVAL',
        severity: 'MEDIUM',
        amount: visible ? row.amount : null,
        dueAt: row.dueAt ?? null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'approval',
      });
      continue;
    }
    if (row.eventType === 'ISSUE' && row.approvalStatus !== 'REJECTED' && row.approvalStatus !== 'PENDING') {
      push({
        id: row.operatingEventId,
        title: row.title,
        kind: 'ISSUE',
        severity: 'LOW',
        amount: null,
        dueAt: row.dueAt ?? null,
        counterpartyName: null,
        sourceEventId: row.operatingEventId,
        cta: null,
      });
    }
  }

  if (visible) {
    for (const row of events) {
      if (!effective(row)) continue;
      const receivable = row.eventType === 'SALE';
      const payable = row.eventType === 'PURCHASE' || row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST';
      if (!receivable && !payable) continue;
      const open = obligationRemaining(row, events, receivable ? 'COLLECTION' : 'PAYMENT');
      if (!(open > 0)) continue;
      const due = day(row.dueAt);
      const party = row.counterpartyName || 'Unnamed';
      const amount = money(open);
      // Undated open balances stay on Open Commitments, not Needs Attention (NA-T09).
      if (!due) continue;
      if (due < today) {
        push({
          id: `open-${row.operatingEventId}`,
          title: receivable ? `${party} is overdue` : `${party} payment is overdue`,
          kind: receivable ? 'OVERDUE_RECEIVABLE' : 'OVERDUE_PAYABLE',
          severity: 'HIGH',
          amount,
          dueAt: due,
          counterpartyName: row.counterpartyName ?? null,
          sourceEventId: row.operatingEventId,
          cta: receivable ? 'collect' : 'pay',
        });
      } else if (due <= soon) {
        push({
          id: `open-${row.operatingEventId}`,
          title: receivable ? `${party} is due soon` : `${party} payment is due soon`,
          kind: receivable ? 'DUE_RECEIVABLE' : 'DUE_PAYABLE',
          severity: 'MEDIUM',
          amount,
          dueAt: due,
          counterpartyName: row.counterpartyName ?? null,
          sourceEventId: row.operatingEventId,
          cta: receivable ? 'collect' : 'pay',
        });
      }
    }
  }

  for (const order of events) {
    if (order.eventType !== 'ORDER' || !effective(order)) continue;
    if (order.contract?.orderStatus === 'CANCELLED' || order.contract?.orderStatus === 'COMPLETED') continue;
    const left = unfulfilledShipmentQty(order, events);
    if (!(left > 0)) continue;
    const promised = day(order.contract?.promisedAt ?? order.dueAt);
    if (!promised || promised >= today) continue;
    const party = order.counterpartyName || order.title;
    push({
      id: `ship-${order.operatingEventId}`,
      title: `${party} delivery is overdue · ${trim(left)} unfulfilled`,
      kind: 'OVERDUE_SHIPMENT',
      severity: 'HIGH',
      amount: null,
      dueAt: promised,
      counterpartyName: order.counterpartyName ?? null,
      sourceEventId: order.operatingEventId,
      cta: 'shipment',
    });
  }

  for (const position of tradingStockPositions(events)) {
    if (position.reorderPoint == null || position.onHand > position.reorderPoint) continue;
    const unit = position.unit ? ` ${position.unit}` : '';
    push({
      id: `stock-${itemKey(position.item)}`,
      title: `${position.item} is low: ${trim(position.onHand)}${unit}`,
      kind: 'LOW_STOCK',
      severity: 'MEDIUM',
      amount: null,
      dueAt: null,
      counterpartyName: null,
      sourceEventId: null,
      cta: 'stock',
    });
  }

  return sortAttention(cards);
}
