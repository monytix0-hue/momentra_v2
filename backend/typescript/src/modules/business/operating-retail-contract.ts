/**
 * Retail, restaurant, and consumer-service rules on the existing operating event.
 * Orders and bookings stay commitments. Only a posted sale or finalized charge is revenue.
 */

export const RETAIL_SUBTYPES = ['RETAIL_STORE', 'RESTAURANT_FOOD', 'CONSUMER_SERVICE'] as const;

export const EXPENSE_CATEGORIES = [
  'Rent/lease',
  'Utilities',
  'Electricity',
  'Water',
  'Repairs',
  'Cleaning',
  'Security',
  'Wages/salaries',
  'Contract labour',
  'Staff welfare',
  'Training',
  'Travel',
  'Advertising',
  'Promotions',
  'Commissions',
  'Packaging',
  'Delivery/shipping',
  'Marketplace/platform fees',
  'Office supplies',
  'Consumables',
  'Kitchen supplies',
  'Disposables',
  'Laundry',
  'Uniforms',
  'Software/subscriptions',
  'Telecom/internet',
  'POS/payment gateway fees',
  'Equipment maintenance',
  'Professional fees',
  'Licences',
  'Insurance',
  'Bank charges',
  'Taxes/levies',
  'Cleaning materials',
  'Service equipment maintenance',
  'Field travel',
  'Outsourced service',
  'Gas/fuel',
  'Kitchen equipment',
  'Food wastage',
  'Hygiene/pest control',
  'Aggregator fees',
  'Miscellaneous',
] as const;

const STOCK_REASONS = ['opening', 'stocktake', 'damage', 'waste', 'return', 'correction'] as const;
const DUE_SOON_DAYS = 3;

export interface OperatingAllocation {
  sourceId: string;
  amount: string;
}

export interface OperatingContract {
  entryMode?: 'QUICK' | 'ITEMIZED' | null;
  paymentState?: 'RECEIVED' | 'PART_PAID' | 'PAY_LATER' | 'PAID' | null;
  receiptState?: 'RECEIVED' | 'PART_RECEIVED' | 'AWAITING' | null;
  receivedAmount?: string | null;
  quantityReceived?: string | null;
  stockAction?: 'SET' | 'ADD' | 'REDUCE' | null;
  stockReason?: (typeof STOCK_REASONS)[number] | null;
  expenseCategory?: string | null;
  appointmentStatus?: 'BOOKED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW' | null;
  appointmentAction?: 'RESCHEDULE' | 'CANCEL' | 'NO_SHOW' | 'START' | 'COMPLETE' | null;
  orderStatus?: 'PLACED' | 'PREPARING' | 'COMPLETED' | 'CANCELLED' | null;
  clientKey?: string | null;
  allocations?: OperatingAllocation[];
  reorderPoint?: string | null;
  priorTimes?: string[];
  startedAt?: string | null;
  completedAt?: string | null;
  endsAt?: string | null;
  declaredQuantity?: string | null;
  staffName?: string | null;
  followUp?: boolean | null;
  estimatedAmount?: string | null;
  ownership?: 'OWNED' | 'CONSIGNMENT' | null;
  shipmentStatus?: 'PLACED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED' | null;
  promisedAt?: string | null;
}

export interface RetailLine {
  itemName: string;
  quantity?: string | null;
  unit?: string | null;
  unitPrice?: string | null;
}

export interface RetailEvent {
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
  dueAt?: string | null;
  lines?: RetailLine[];
  contract?: OperatingContract | null;
  note?: string | null;
}

export interface RetailWrite {
  eventType: string;
  title: string;
  amount: string | null;
  counterpartyName?: string | null;
  channelName?: string | null;
  occurredAt?: string | null;
  parentEventId?: string | null;
  quantity?: string | null;
  dueAt?: string | null;
  lines?: RetailLine[] | null;
  contract?: OperatingContract | null;
  revisingId?: string | null;
}

export interface RetailAttention {
  id: string;
  title: string;
  kind: string;
  severity: string;
  amount: string | null;
  dueAt: string | null;
  counterpartyName: string | null;
  sourceEventId: string | null;
  cta: string | null;
}

export interface StockPosition {
  item: string;
  unit: string | null;
  onHand: number;
  reorderPoint: number | null;
}

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

function effective(row: RetailEvent): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function namedParty(value: string | null | undefined): boolean {
  const name = String(value ?? '').trim().toLowerCase();
  return name.length > 0 && !['walk-in', 'anonymous', 'guest', 'walk in'].includes(name);
}

function itemKey(value: string): string {
  return value.trim().toLowerCase();
}

export function isRetailSubtype(subtype: string | null | undefined): boolean {
  return (RETAIL_SUBTYPES as readonly string[]).includes(String(subtype ?? '').toUpperCase());
}

export function parseOperatingContract(raw: unknown): OperatingContract | null {
  if (!raw || typeof raw !== 'object') return null;
  const rec = raw as Record<string, unknown>;
  const allocations = Array.isArray(rec.allocations)
    ? rec.allocations.flatMap((row) => {
        if (!row || typeof row !== 'object') return [];
        const part = row as Record<string, unknown>;
        const sourceId = String(part.sourceId ?? '').trim();
        const amount = String(part.amount ?? '').trim();
        if (!sourceId || !amount) return [];
        return [{ sourceId, amount }];
      })
    : [];
  const contract: OperatingContract = {
    entryMode: rec.entryMode === 'QUICK' || rec.entryMode === 'ITEMIZED' ? rec.entryMode : null,
    paymentState:
      rec.paymentState === 'RECEIVED' || rec.paymentState === 'PART_PAID' || rec.paymentState === 'PAY_LATER' || rec.paymentState === 'PAID'
        ? rec.paymentState
        : null,
    receiptState:
      rec.receiptState === 'RECEIVED' || rec.receiptState === 'PART_RECEIVED' || rec.receiptState === 'AWAITING'
        ? rec.receiptState
        : null,
    receivedAmount: rec.receivedAmount == null ? null : String(rec.receivedAmount),
    quantityReceived: rec.quantityReceived == null ? null : String(rec.quantityReceived),
    stockAction: rec.stockAction === 'SET' || rec.stockAction === 'ADD' || rec.stockAction === 'REDUCE' ? rec.stockAction : null,
    stockReason: STOCK_REASONS.includes(rec.stockReason as (typeof STOCK_REASONS)[number])
      ? (rec.stockReason as (typeof STOCK_REASONS)[number])
      : null,
    expenseCategory: rec.expenseCategory == null ? null : String(rec.expenseCategory),
    appointmentStatus:
      rec.appointmentStatus === 'BOOKED' ||
      rec.appointmentStatus === 'IN_PROGRESS' ||
      rec.appointmentStatus === 'COMPLETED' ||
      rec.appointmentStatus === 'CANCELLED' ||
      rec.appointmentStatus === 'NO_SHOW'
        ? rec.appointmentStatus
        : null,
    appointmentAction:
      rec.appointmentAction === 'RESCHEDULE' ||
      rec.appointmentAction === 'CANCEL' ||
      rec.appointmentAction === 'NO_SHOW' ||
      rec.appointmentAction === 'START' ||
      rec.appointmentAction === 'COMPLETE'
        ? rec.appointmentAction
        : null,
    orderStatus:
      rec.orderStatus === 'PLACED' || rec.orderStatus === 'PREPARING' || rec.orderStatus === 'COMPLETED' || rec.orderStatus === 'CANCELLED'
        ? rec.orderStatus
        : null,
    clientKey: rec.clientKey == null ? null : String(rec.clientKey),
    allocations,
    reorderPoint: rec.reorderPoint == null ? null : String(rec.reorderPoint),
    priorTimes: Array.isArray(rec.priorTimes) ? rec.priorTimes.map((item) => String(item)) : [],
    startedAt: rec.startedAt == null ? null : String(rec.startedAt),
    completedAt: rec.completedAt == null ? null : String(rec.completedAt),
    endsAt: rec.endsAt == null ? null : String(rec.endsAt),
    declaredQuantity: rec.declaredQuantity == null ? null : String(rec.declaredQuantity),
    staffName: rec.staffName == null ? null : String(rec.staffName),
    followUp: rec.followUp === true,
    estimatedAmount: rec.estimatedAmount == null ? null : String(rec.estimatedAmount),
    ownership: rec.ownership === 'OWNED' || rec.ownership === 'CONSIGNMENT' ? rec.ownership : null,
    shipmentStatus:
      rec.shipmentStatus === 'PLACED' ||
      rec.shipmentStatus === 'IN_TRANSIT' ||
      rec.shipmentStatus === 'DELIVERED' ||
      rec.shipmentStatus === 'CANCELLED'
        ? rec.shipmentStatus
        : null,
    promisedAt: rec.promisedAt == null ? null : String(rec.promisedAt),
  };
  return contract;
}

function shareAgainst(row: RetailEvent, parentId: string): number {
  const parts = row.contract?.allocations ?? [];
  if (parts.length > 0) {
    return parts.reduce((total, part) => (part.sourceId === parentId ? total + num(part.amount) : total), 0);
  }
  return row.parentEventId === parentId ? num(row.amount) : 0;
}

export function obligationRemaining(row: RetailEvent, events: RetailEvent[], childType: 'COLLECTION' | 'PAYMENT'): number {
  if (!effective(row) || ['COLLECTED', 'PAID', 'SETTLED', 'CANCELLED'].includes(row.paymentStatus)) return 0;
  const used = events.reduce((total, child) => {
    if (child.eventType !== childType || !effective(child)) return total;
    return total + shareAgainst(child, row.operatingEventId);
  }, 0);
  return Math.max(0, num(row.amount) - used);
}

function lineTotal(lines: RetailLine[] | null | undefined): number {
  return (lines ?? []).reduce((total, line) => total + num(line.quantity ?? 1) * num(line.unitPrice), 0);
}

function fail(message: string): { ok: false; message: string } {
  return { ok: false, message };
}

export interface RetailWriteResult {
  ok: true;
  paymentStatus: string;
  amount: string | null;
  parentEventId: string | null;
  quantity: string | null;
  contract: OperatingContract;
  settleChild: { type: 'COLLECTION' | 'PAYMENT'; amount: string } | null;
}

export function validateRetailWrite(input: RetailWrite, existing: RetailEvent[]): RetailWriteResult | { ok: false; message: string } {
  const contract: OperatingContract = { ...(input.contract ?? {}) };
  const current = input.revisingId ? existing.find((row) => row.operatingEventId === input.revisingId) ?? null : null;
  if (input.eventType === 'APPOINTMENT' && input.revisingId) {
    return validateAppointmentRevision(input, current, contract);
  }
  if (input.eventType === 'SALE' || input.eventType === 'SERVICE') return validateSale(input, existing, contract);
  if (input.eventType === 'PURCHASE') return validatePurchase(input, contract);
  if (input.eventType === 'SPEND') return validateExpense(input, contract);
  if (input.eventType === 'COLLECTION' || input.eventType === 'PAYMENT') return validateSettlement(input, existing, contract);
  if (input.eventType === 'STOCK') return validateStock(input, contract);
  if (input.eventType === 'ORDER') return validateOrder(input, contract);
  if (input.eventType === 'APPOINTMENT') return validateBooking(input, contract);
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

function validateSale(input: RetailWrite, existing: RetailEvent[], contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
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
  if (received < amount && !namedParty(input.counterpartyName)) return fail('A named customer is required when a balance remains.');
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent) return fail('Choose a record on this moment.');
    const allowed = input.eventType === 'SERVICE' ? ['APPOINTMENT'] : ['ORDER', 'APPOINTMENT'];
    if (!allowed.includes(parent.eventType)) return fail('That record cannot be linked here.');
    const billed = existing.some(
      (row) =>
        row.parentEventId === parent.operatingEventId &&
        (row.eventType === 'SALE' || row.eventType === 'SERVICE') &&
        effective(row)
    );
    if (billed) return fail('This already has a posted charge.');
  }
  contract.receivedAmount = money(received);
  const paymentStatus = state === 'RECEIVED' ? 'COLLECTED' : 'PENDING';
  return {
    ok: true,
    paymentStatus,
    amount: money(amount),
    parentEventId: input.parentEventId ?? null,
    quantity: mode === 'ITEMIZED' ? money(lines.reduce((total, line) => total + num(line.quantity), 0)) : null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'COLLECTION', amount: money(received) } : null,
  };
}

function validatePurchase(input: RetailWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!namedParty(input.counterpartyName)) return fail('Supplier is required.');
  const lines = input.lines ?? [];
  const amount = lines.length > 0 && lineTotal(lines) > 0 ? lineTotal(lines) : num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  if (contract.receiptState !== 'RECEIVED' && contract.receiptState !== 'PART_RECEIVED' && contract.receiptState !== 'AWAITING') {
    return fail('Choose whether goods were received.');
  }
  if (contract.receiptState === 'PART_RECEIVED' && !(num(contract.quantityReceived) > 0)) return fail('Quantity received is required.');
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

function validateExpense(input: RetailWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
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
  input: RetailWrite,
  existing: RetailEvent[],
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
    const allowed = childType === 'COLLECTION' ? ['SALE', 'SERVICE'] : ['PURCHASE', 'SPEND', 'VENDOR_COST'];
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

function validateStock(input: RetailWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!input.title.trim()) return fail('Choose an item.');
  if (contract.stockAction !== 'SET' && contract.stockAction !== 'ADD' && contract.stockAction !== 'REDUCE') {
    return fail('Choose set, add, or reduce.');
  }
  if (!(num(input.quantity) >= 0) || (contract.stockAction !== 'SET' && !(num(input.quantity) > 0))) {
    return fail('Quantity is required.');
  }
  if (!contract.stockReason) return fail('Choose a reason.');
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

function validateOrder(input: RetailWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!String(input.channelName ?? '').trim()) return fail('Order channel is required.');
  if (!contract.orderStatus) contract.orderStatus = 'PLACED';
  if (!['PLACED', 'PREPARING', 'COMPLETED', 'CANCELLED'].includes(contract.orderStatus)) return fail('Choose an order status.');
  if (input.amount) contract.estimatedAmount = input.amount;
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

function validateBooking(input: RetailWrite, contract: OperatingContract): RetailWriteResult | { ok: false; message: string } {
  if (!input.title.trim()) return fail('Service is required.');
  if (!input.occurredAt) return fail('Appointment time is required.');
  contract.appointmentStatus = 'BOOKED';
  if (input.amount) contract.estimatedAmount = input.amount;
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: null,
    contract,
    settleChild: null,
  };
}

function validateAppointmentRevision(
  input: RetailWrite,
  current: RetailEvent | null,
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  if (!current || current.eventType !== 'APPOINTMENT') return fail('Choose the booking to update.');
  const prior = current.contract?.appointmentStatus ?? 'BOOKED';
  const action = contract.appointmentAction;
  if (!action) return fail('Choose what changed on this booking.');
  if (['CANCELLED', 'NO_SHOW', 'COMPLETED'].includes(prior)) return fail('This booking is already closed.');
  let status = prior;
  if (action === 'RESCHEDULE') {
    if (prior !== 'BOOKED') return fail('Only a booked appointment can be rescheduled.');
    if (!input.occurredAt) return fail('A new appointment time is required.');
    status = 'BOOKED';
    contract.priorTimes = [...(current.contract?.priorTimes ?? []), day(current.occurredAt) || current.occurredAt];
  } else if (action === 'CANCEL') status = 'CANCELLED';
  else if (action === 'NO_SHOW') status = 'NO_SHOW';
  else if (action === 'START') {
    if (prior !== 'BOOKED') return fail('Start a booked appointment.');
    status = 'IN_PROGRESS';
    contract.startedAt = input.occurredAt || new Date().toISOString();
  } else if (action === 'COMPLETE') {
    if (prior !== 'BOOKED' && prior !== 'IN_PROGRESS') return fail('Complete a booked or in-progress appointment.');
    status = 'COMPLETED';
    contract.completedAt = input.occurredAt || new Date().toISOString();
    contract.startedAt = current.contract?.startedAt ?? null;
  }
  contract.appointmentStatus = status as OperatingContract['appointmentStatus'];
  contract.staffName = contract.staffName ?? current.contract?.staffName ?? null;
  contract.endsAt = contract.endsAt ?? current.contract?.endsAt ?? null;
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: null,
    contract,
    settleChild: null,
  };
}

export function stockPositions(events: RetailEvent[]): StockPosition[] {
  const ordered = events
    .map((row, index) => ({ row, index }))
    .filter(({ row }) => effective(row))
    .sort((a, b) => a.row.occurredAt.localeCompare(b.row.occurredAt) || a.index - b.index);
  const positions = new Map<string, StockPosition>();
  const touch = (name: string, unit: string | null) => {
    const key = itemKey(name);
    const current = positions.get(key) ?? { item: name.trim(), unit, onHand: 0, reorderPoint: null };
    if (unit && !current.unit) current.unit = unit;
    positions.set(key, current);
    return current;
  };
  for (const { row } of ordered) {
    const reorder = row.contract?.reorderPoint;
    if (row.eventType === 'STOCK' && row.contract?.stockAction) {
      const position = touch(row.title, row.unit ?? null);
      if (reorder != null) position.reorderPoint = num(reorder);
      const qty = num(row.contract.declaredQuantity ?? row.quantity);
      if (row.contract.stockAction === 'SET') position.onHand = qty;
      else if (row.contract.stockAction === 'ADD') position.onHand += num(row.quantity);
      else position.onHand -= num(row.quantity);
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
        touch(line.itemName, line.unit ?? null).onHand += take;
        left -= take;
      }
      continue;
    }
    if ((row.eventType === 'SALE' || row.eventType === 'SERVICE') && row.contract?.entryMode === 'ITEMIZED') {
      for (const line of row.lines ?? []) {
        touch(line.itemName, line.unit ?? null).onHand -= num(line.quantity);
      }
    }
  }
  return [...positions.values()];
}

function addDays(dayText: string, days: number): string {
  const date = new Date(`${dayText}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function billed(parentId: string, events: RetailEvent[]): boolean {
  return events.some(
    (row) => row.parentEventId === parentId && (row.eventType === 'SALE' || row.eventType === 'SERVICE') && effective(row)
  );
}

export function retailAttention(events: RetailEvent[], visible: boolean, canApprove: boolean, asOf: Date): RetailAttention[] {
  const today = asOf.toISOString().slice(0, 10);
  const soon = addDays(today, DUE_SOON_DAYS);
  const cards: RetailAttention[] = [];
  const push = (card: RetailAttention) => cards.push(card);
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
      const receivable = row.eventType === 'SALE' || row.eventType === 'SERVICE';
      const payable = row.eventType === 'PURCHASE' || row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST';
      if (!receivable && !payable) continue;
      const open = obligationRemaining(row, events, receivable ? 'COLLECTION' : 'PAYMENT');
      if (!(open > 0)) continue;
      const due = day(row.dueAt);
      const party = row.counterpartyName || 'Unnamed';
      const amount = money(open);
      // Open undated balances belong in Open Commitments, not Needs Attention (ALL-07).
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
  for (const position of stockPositions(events)) {
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
  const active = events.filter((row) => row.eventType === 'APPOINTMENT' && effective(row) && row.contract?.appointmentStatus);
  for (const row of active) {
    const status = row.contract?.appointmentStatus;
    const when = row.occurredAt;
    const startDay = day(when);
    if ((status === 'BOOKED' || status === 'IN_PROGRESS') && startDay && startDay < today) {
      push({
        id: `booking-${row.operatingEventId}`,
        title: `${row.title} needs a status update`,
        kind: 'BOOKING_OVERDUE',
        severity: 'HIGH',
        amount: null,
        dueAt: startDay,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'booking',
      });
    } else if (status === 'BOOKED' && when > asOf.toISOString() && Date.parse(when) - asOf.getTime() <= 24 * 60 * 60 * 1000) {
      push({
        id: `upcoming-${row.operatingEventId}`,
        title: `${row.title} is coming up`,
        kind: 'UPCOMING',
        severity: 'LOW',
        amount: null,
        dueAt: startDay,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'booking',
      });
      if (!String(row.contract?.staffName ?? '').trim()) {
        push({
          id: `staff-${row.operatingEventId}`,
          title: `${row.title} needs a person assigned`,
          kind: 'UNASSIGNED',
          severity: 'MEDIUM',
          amount: null,
          dueAt: startDay,
          counterpartyName: null,
          sourceEventId: row.operatingEventId,
          cta: 'booking',
        });
      }
    }
    if (status === 'COMPLETED' && !billed(row.operatingEventId, events)) {
      push({
        id: `charge-${row.operatingEventId}`,
        title: `${row.title} is completed and still needs a charge`,
        kind: 'CHARGE_PENDING',
        severity: 'HIGH',
        amount: null,
        dueAt: null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: visible ? 'charge' : null,
      });
    }
    if (status === 'NO_SHOW' && row.contract?.followUp) {
      push({
        id: `noshow-${row.operatingEventId}`,
        title: `Follow up on missed ${row.title}`,
        kind: 'NO_SHOW',
        severity: 'LOW',
        amount: null,
        dueAt: null,
        counterpartyName: row.counterpartyName ?? null,
        sourceEventId: row.operatingEventId,
        cta: 'booking',
      });
    }
    if (status === 'IN_PROGRESS' && row.contract?.endsAt && row.contract.endsAt < asOf.toISOString()) {
      push({
        id: `stuck-${row.operatingEventId}`,
        title: `${row.title} is still in progress`,
        kind: 'STUCK',
        severity: 'MEDIUM',
        amount: null,
        dueAt: null,
        counterpartyName: null,
        sourceEventId: row.operatingEventId,
        cta: 'booking',
      });
    }
  }
  const staffed = active.filter((row) => row.contract?.staffName && row.contract.appointmentStatus !== 'CANCELLED' && row.contract.appointmentStatus !== 'NO_SHOW' && row.contract.appointmentStatus !== 'COMPLETED');
  for (let i = 0; i < staffed.length; i += 1) {
    for (let j = i + 1; j < staffed.length; j += 1) {
      const a = staffed[i]!;
      const b = staffed[j]!;
      if (a.contract?.staffName !== b.contract?.staffName) continue;
      if (!overlaps(a, b)) continue;
      push({
        id: `conflict-${a.operatingEventId}`,
        title: `${a.contract?.staffName} has overlapping appointments`,
        kind: 'CONFLICT',
        severity: 'HIGH',
        amount: null,
        dueAt: null,
        counterpartyName: null,
        sourceEventId: a.operatingEventId,
        cta: 'booking',
      });
      break;
    }
  }
  return sortAttention(cards);
}

const SEVERITY_RANK: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

function sortAttention(cards: RetailAttention[]): RetailAttention[] {
  const kindRank = (kind: string): number => {
    if (kind.startsWith('OVERDUE')) return 0;
    if (kind.startsWith('DUE')) return 1;
    if (kind === 'CONFLICT' || kind === 'CHARGE_PENDING' || kind === 'BOOKING_OVERDUE' || kind === 'STUCK') return 2;
    if (kind === 'LOW_STOCK' || kind === 'UNASSIGNED' || kind === 'BUDGET_WARNING' || kind === 'BUDGET_OVERRUN') return 3;
    if (kind === 'APPROVAL') return 4;
    return 5;
  };
  return [...cards].sort((a, b) => {
    const sev = (SEVERITY_RANK[a.severity ?? ''] ?? 9) - (SEVERITY_RANK[b.severity ?? ''] ?? 9);
    if (sev !== 0) return sev;
    return kindRank(a.kind) - kindRank(b.kind);
  });
}

function overlaps(a: RetailEvent, b: RetailEvent): boolean {
  const startA = Date.parse(a.occurredAt);
  const startB = Date.parse(b.occurredAt);
  if (!Number.isFinite(startA) || !Number.isFinite(startB)) return false;
  const endA = Date.parse(a.contract?.endsAt || '') || startA;
  const endB = Date.parse(b.contract?.endsAt || '') || startB;
  return startA < endB && startB < endA;
}

function trim(value: number): string {
  return Number(value.toFixed(2)).toString();
}

export function salesCoverage(events: RetailEvent[]): string | null {
  const sales = events.filter((row) => (row.eventType === 'SALE' || row.eventType === 'SERVICE') && effective(row));
  if (sales.length === 0) return null;
  const itemized = sales.filter((row) => row.contract?.entryMode === 'ITEMIZED').length;
  if (itemized === 0) return 'Product mix not recorded';
  if (itemized === sales.length) return null;
  return `Product mix covers ${itemized} of ${sales.length} posted sales`;
}

export function postedSalesSummary(events: RetailEvent[], visible: boolean, currency: string): string {
  const rows = events.filter((row) => row.approvalStatus !== 'REJECTED');
  const sales = rows.filter((row) => (row.eventType === 'SALE' || row.eventType === 'SERVICE') && effective(row));
  const openOrders = rows.filter((row) => row.eventType === 'ORDER' && row.contract?.orderStatus !== 'CANCELLED' && row.contract?.orderStatus !== 'COMPLETED');
  const total = sales.reduce((sum, row) => sum + num(row.amount), 0);
  const moneyText = visible ? ` · ${money(total)} ${currency}` : '';
  return `Posted sales ${sales.length}${moneyText} · Open orders ${openOrders.length}`;
}

export function appointmentSummary(events: RetailEvent[]): string | null {
  const rows = events.filter((row) => row.eventType === 'APPOINTMENT' && row.approvalStatus !== 'REJECTED' && row.contract?.appointmentStatus);
  if (rows.length === 0) return null;
  const count = (status: string) => rows.filter((row) => row.contract?.appointmentStatus === status).length;
  return `Booked ${count('BOOKED')} · In progress ${count('IN_PROGRESS')} · Completed ${count('COMPLETED')} · Cancelled ${count('CANCELLED')} · No-show ${count('NO_SHOW')}`;
}

export function outcomeCoverage(events: RetailEvent[], asOf: Date): string | null {
  const today = asOf.toISOString().slice(0, 10);
  // Eligible = scheduled time has passed; reschedule keeps one identity (current occurredAt).
  const rows = events.filter(
    (row) =>
      row.eventType === 'APPOINTMENT' &&
      effective(row) &&
      row.contract?.appointmentStatus &&
      day(row.occurredAt) &&
      day(row.occurredAt)! <= today
  );
  if (rows.length === 0) return null;
  const known = rows.filter((row) => ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(row.contract?.appointmentStatus ?? ''));
  const unresolved = rows.length - known.length;
  if (known.length === 0 || known.length < Math.ceil(rows.length * 0.5)) {
    return unresolved > 0
      ? `Insufficient recorded outcomes · ${known.length} of ${rows.length} known`
      : 'Insufficient recorded outcomes';
  }
  const completed = known.filter((row) => row.contract?.appointmentStatus === 'COMPLETED').length;
  const missed = known.filter((row) => row.contract?.appointmentStatus === 'NO_SHOW').length;
  const coverage =
    unresolved > 0
      ? ` · Coverage ${known.length} of ${rows.length}`
      : '';
  return `Completion ${completed} of ${known.length} · No-show ${missed} of ${known.length}${coverage}`;
}

/** Service Flow counts for Consumer Services Life (Booked → … → Finalized). */
export function serviceFlowSummary(events: RetailEvent[]): string {
  const appts = events.filter((row) => row.eventType === 'APPOINTMENT' && effective(row) && row.contract?.appointmentStatus);
  const count = (status: string) => appts.filter((row) => row.contract?.appointmentStatus === status).length;
  const completed = appts.filter((row) => row.contract?.appointmentStatus === 'COMPLETED');
  const chargePending = completed.filter(
    (row) =>
      !events.some(
        (child) =>
          child.parentEventId === row.operatingEventId &&
          (child.eventType === 'SERVICE' || child.eventType === 'SALE') &&
          effective(child)
      )
  ).length;
  const finalized = events.filter(
    (row) => (row.eventType === 'SERVICE' || row.eventType === 'SALE') && effective(row) && row.parentEventId
  ).length;
  return `Booked ${count('BOOKED')} · In progress ${count('IN_PROGRESS')} · Completed ${count('COMPLETED')} · Charge pending ${chargePending} · Finalized ${finalized}`;
}

export function customersNetworkDetail(events: RetailEvent[], channels: string[]): string {
  const named = new Set<string>();
  let walkIns = 0;
  for (const row of events) {
    if (!effective(row)) continue;
    if (!(row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'APPOINTMENT' || row.eventType === 'COLLECTION')) {
      continue;
    }
    if (namedParty(row.counterpartyName)) named.add(String(row.counterpartyName).trim());
    else if (row.counterpartyName || row.eventType === 'SALE' || row.eventType === 'SERVICE') walkIns += 1;
  }
  const channelText = channels.length > 0 ? ` · Channels ${channels.join(', ')}` : '';
  return `Named customers ${named.size} · Walk-ins ${walkIns}${channelText}`;
}

export function moneyMovingDetail(events: RetailEvent[], visible: boolean, currency: string, asOf: Date): string | null {
  if (!visible) return null;
  const today = asOf.toISOString().slice(0, 10);
  const month = today.slice(0, 7);
  let todayIn = 0;
  let monthIn = 0;
  let todayOut = 0;
  let monthOut = 0;
  for (const row of events) {
    if (!effective(row)) continue;
    const stamp = day(row.occurredAt);
    const inflow =
      row.eventType === 'COLLECTION' && row.paymentStatus === 'COLLECTED'
        ? num(row.amount)
        : (row.eventType === 'SALE' || row.eventType === 'SERVICE') && row.paymentStatus === 'COLLECTED' && !events.some((child) => child.parentEventId === row.operatingEventId && child.eventType === 'COLLECTION' && effective(child))
          ? num(row.amount)
          : 0;
    const outflow =
      row.eventType === 'PAYMENT' && row.paymentStatus === 'PAID'
        ? num(row.amount)
        : (row.eventType === 'PURCHASE' || row.eventType === 'SPEND') && row.paymentStatus === 'PAID' && !events.some((child) => child.parentEventId === row.operatingEventId && child.eventType === 'PAYMENT' && effective(child))
          ? num(row.amount)
          : 0;
    if (stamp === today) {
      todayIn += inflow;
      todayOut += outflow;
    }
    if (stamp.slice(0, 7) === month) {
      monthIn += inflow;
      monthOut += outflow;
    }
  }
  return `Today received ${money(todayIn)} ${currency} · Month received ${money(monthIn)} ${currency} · Today paid ${money(todayOut)} ${currency} · Month paid ${money(monthOut)} ${currency}`;
}

export function budgetAttention(spent: number, budget: number, configured: boolean, visible: boolean): RetailAttention[] {
  if (!visible || !configured || !(budget > 0)) return [];
  if (spent > budget) {
    return [{
      id: 'budget-over',
      title: 'Budget is over the current plan',
      kind: 'BUDGET_OVERRUN',
      severity: 'HIGH',
      amount: money(spent - budget),
      dueAt: null,
      counterpartyName: null,
      sourceEventId: null,
      cta: 'budget',
    }];
  }
  if (spent / budget >= 0.8) {
    return [{
      id: 'budget-threshold',
      title: 'Budget is above 80% of the current plan',
      kind: 'BUDGET_WARNING',
      severity: 'MEDIUM',
      amount: money(budget - spent),
      dueAt: null,
      counterpartyName: null,
      sourceEventId: null,
      cta: 'budget',
    }];
  }
  return [];
}
