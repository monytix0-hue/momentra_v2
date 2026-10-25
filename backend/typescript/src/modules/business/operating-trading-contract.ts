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
  type OperatingContract,
  type RetailAttention,
  type RetailEvent,
  type RetailLine,
  type RetailWrite,
  type RetailWriteResult,
  type StockPosition,
} from './operating-retail-contract';
import { lineBaseQuantity, toBaseQuantity, type MasterMeta } from './operating-trading-lov';
import { computeGstLine, rollGstLines, scaleGstSnapshot, type GstSnapshot } from './operating-gst';

export const TRADING_SUBTYPES = ['TRADING_WHOLESALE', 'DISTRIBUTION_DEALERSHIP'] as const;

export type TradingEvent = RetailEvent;
export interface TradingGstContext {
  enabled: boolean;
  businessState: string;
  saleIncludesGst: boolean | null;
  purchaseIncludesGst: boolean | null;
  registration: string;
}

export type TradingWrite = RetailWrite & { channelTracking?: boolean; gst?: TradingGstContext | null };
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

function masterRecord(line: TradingLine, existing: TradingEvent[]) {
  if (line.masterId) {
    const byId = existing.find((row) => row.operatingEventId === line.masterId && row.master);
    if (byId?.master) return byId.master;
  }
  const name = line.itemName.trim().toLowerCase();
  if (!name) return null;
  return existing.find((row) => row.master && row.master.name.trim().toLowerCase() === name)?.master ?? null;
}

function partyState(name: string | null | undefined, existing: TradingEvent[]): string {
  const key = String(name ?? '').trim().toLowerCase();
  if (!key) return '';
  const party = existing.find((row) => {
    const kind = String(row.master?.kind ?? '').toUpperCase();
    return ['CUSTOMER', 'BUYER', 'SUPPLIER'].includes(kind) && row.master?.name.trim().toLowerCase() === key;
  });
  return String(party?.master?.deliveryState || party?.master?.billingState || party?.master?.businessState || '').trim();
}

function applyPostedGst(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract,
  side: 'SALE' | 'PURCHASE',
  entered: number,
  lines: TradingLine[]
): { ok: true; amount: number } | { ok: false; message: string } {
  if (!input.gst?.enabled) {
    if (input.gst) contract.taxStatus = 'NOT_CONFIGURED';
    return { ok: true, amount: entered };
  }
  const preference = side === 'SALE' ? input.gst.saleIncludesGst : input.gst.purchaseIncludesGst;
  const businessState = input.gst.businessState.trim().toLowerCase();
  const supplyState = (contract.placeOfSupply || partyState(input.counterpartyName, existing)).trim();
  let jurisdiction = contract.taxJurisdiction ?? null;
  if (!jurisdiction && businessState && supplyState) {
    jurisdiction = supplyState.toLowerCase() === businessState ? 'INTRA' : 'INTER';
  }
  if (!jurisdiction) return fail('Confirm whether this supply is inside your state or outside it.');
  contract.taxJurisdiction = jurisdiction;
  contract.placeOfSupply = supplyState || contract.placeOfSupply || input.gst.businessState;

  const pricedLines = lines.filter((line) => num(line.unitPrice) > 0 && num(line.quantity) > 0);
  const sources = pricedLines.length > 0
    ? pricedLines.map((line) => ({ line, master: masterRecord(line, existing) }))
    : [{ line: { itemName: input.title, quantity: '1', unitPrice: String(entered), unit: null }, master: null }];

  const gstLines = [];
  for (const source of sources) {
    const master = source.master;
    const applicable = String(master?.gstApplicable ?? '').toUpperCase();
    const classification = String(master?.gstClassification ?? '').toUpperCase();
    const exempt = applicable === 'NO' || ['EXEMPT', 'NIL', 'NON_GST'].includes(classification);
    const rate = exempt ? 0 : num(source.line.gstRate || contract.gstRate || master?.gstRate);
    if (!exempt && !(rate > 0)) return fail('Choose a GST rate before saving. A missing rate is not 0%.');
    const sideOverride = side === 'SALE' ? master?.salePriceIncludesGst : master?.purchasePriceIncludesGst;
    const override = String(sideOverride || master?.priceIncludesGst || '').toUpperCase();
    const lineMode = source.line.taxMode;
    const inclusive = lineMode === 'INCLUSIVE'
      ? true
      : lineMode === 'EXCLUSIVE'
        ? false
        : contract.taxMode === 'INCLUSIVE'
          ? true
          : contract.taxMode === 'EXCLUSIVE'
            ? false
            : override === 'YES'
              ? true
              : override === 'NO'
                ? false
                : preference;
    if (inclusive == null) return fail('Choose whether the price includes GST.');
    const lineDiscount = num(source.line.discount);
    gstLines.push({
      quantity: num(source.line.quantity) || 1,
      unitPrice: num(source.line.unitPrice),
      discount: lineDiscount > 0 ? lineDiscount : pricedLines.length > 0 ? 0 : num(contract.discount),
      ratePercent: rate,
      inclusive,
    });
  }
  const place = contract.placeOfSupply ?? '';
  const registration = String(input.gst.registration ?? '');
  const snapshot: GstSnapshot = gstLines.length === 1
    ? computeGstLine(gstLines[0]!, jurisdiction, place, registration)
    : rollGstLines(gstLines, jurisdiction, place, registration);
  if (pricedLines.length <= 1 && num(contract.discount) > 0 && gstLines.length === 1) {
    // discount already applied on the single quick line
  }
  contract.gst = snapshot;
  contract.taxStatus = 'CALCULATED';
  contract.taxMode = snapshot.priceIncludesGst ? 'INCLUSIVE' : 'EXCLUSIVE';
  contract.taxAmount = snapshot.gstTotal;
  return { ok: true, amount: num(snapshot.invoiceTotal) };
}

function lineTotal(lines: TradingLine[] | null | undefined): number {
  return (lines ?? []).reduce((total, line) => total + num(line.quantity ?? 1) * num(line.unitPrice), 0);
}

function productForLine(line: TradingLine, existing: TradingEvent[]): MasterMeta | null {
  if (line.masterId) {
    const byId = existing.find((row) => row.operatingEventId === line.masterId && row.master?.kind === 'PRODUCT');
    if (byId?.master) {
      return {
        kind: byId.master.kind,
        name: byId.master.name,
        unit: byId.master.unit,
        dimension: (byId.master.dimension as MasterMeta['dimension']) ?? null,
        conversionToBase: byId.master.conversionToBase,
      };
    }
  }
  const name = line.itemName.trim().toLowerCase();
  if (!name) return null;
  const byName = existing.find(
    (row) => row.master?.kind === 'PRODUCT' && row.master.name.trim().toLowerCase() === name
  );
  if (!byName?.master) return null;
  return {
    kind: byName.master.kind,
    name: byName.master.name,
    unit: byName.master.unit,
    dimension: (byName.master.dimension as MasterMeta['dimension']) ?? null,
    conversionToBase: byName.master.conversionToBase,
  };
}

function convertLine(line: TradingLine, existing: TradingEvent[]): { ok: true } | { ok: false; message: string } {
  const product = productForLine(line, existing);
  if (line.masterId && !product) return fail('Choose a product from the list.');
  const converted = toBaseQuantity(line.quantity, line.unit, product);
  if (!converted.ok) return fail(converted.message);
  line.baseQuantity = money(converted.baseQty);
  return { ok: true };
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
  if (input.eventType === 'PURCHASE') return validatePurchase(input, existing, contract);
  if (input.eventType === 'SPEND') return validateExpense(input, contract);
  if (input.eventType === 'COLLECTION' || input.eventType === 'PAYMENT') return validateSettlement(input, existing, contract);
  if (input.eventType === 'STOCK') return validateStock(input, existing, contract);
  if (input.eventType === 'ORDER') return validateOrder(input, existing, contract);
  if (input.eventType === 'SHIPMENT' || input.eventType === 'DELIVERY') return validateShipment(input, existing, contract);
  if (input.eventType === 'RETURN') return validateReturn(input, existing, contract);
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

function applyTaxAndFx(
  contract: OperatingContract,
  amount: number
): RetailWriteResult | { ok: false; message: string } | null {
  if (!contract.taxMode) contract.taxMode = 'NOT_CONFIGURED';
  if (contract.taxMode === 'NOT_CONFIGURED') {
    contract.taxAmount = null;
  } else if (contract.taxAmount != null && !(num(contract.taxAmount) >= 0)) {
    return fail('Tax amount cannot be negative.');
  }
  const original = String(contract.originalCurrency ?? '').trim().toUpperCase();
  const rate = num(contract.fxRate);
  const converted = num(contract.convertedAmount);
  if (original || rate > 0 || converted > 0) {
    if (!original) return fail('Original currency is required for FX conversion.');
    if (!(rate > 0)) return fail('FX rate is required to convert currency.');
    if (!(converted > 0)) {
      contract.convertedAmount = money(amount * rate);
    }
  } else {
    contract.originalCurrency = null;
    contract.fxRate = null;
    contract.convertedAmount = null;
  }
  return null;
}

function validateSale(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  const mode = contract.entryMode;
  if (mode !== 'QUICK' && mode !== 'ITEMIZED') return fail('Choose quick amount or itemized.');
  if (input.channelTracking && !String(input.channelName ?? '').trim()) {
    return fail('Sales channel is required.');
  }
  const lines = input.lines ?? [];
  let amount = num(input.amount);
  if (mode === 'ITEMIZED') {
    if (lines.length === 0) return fail('Add at least one item.');
    for (const line of lines) {
      if (!line.itemName.trim() || !(num(line.quantity) > 0) || !String(line.unit ?? '').trim() || !(num(line.unitPrice) > 0)) {
        return fail('Each item needs a name, quantity, unit, and price.');
      }
      const converted = convertLine(line, existing);
      if (!converted.ok) return converted;
    }
    amount = lineTotal(lines);
  }
  if (!(amount > 0)) return fail('Amount is required.');
  const commercial = amount;
  const priced = applyPostedGst(input, existing, contract, 'SALE', amount, lines);
  if (!priced.ok) return priced;
  amount = priced.amount;
  const taxFx = applyTaxAndFx(contract, amount);
  if (taxFx && !taxFx.ok) return taxFx;
  const state = contract.paymentState;
  if (state !== 'RECEIVED' && state !== 'PART_PAID' && state !== 'PAY_LATER') return fail('Choose received, part paid, or pay later.');
  const received = state === 'RECEIVED' ? amount : state === 'PAY_LATER' ? 0 : num(contract.receivedAmount);
  if (state === 'PART_PAID' && !(received > 0 && received < amount)) return fail('Part paid must be more than zero and less than the total.');
  if (received < amount && !namedParty(input.counterpartyName)) return fail('A named buyer is required when a balance remains.');
  // D1: due date required when a credit balance remains.
  if (received < amount && !day(input.dueAt)) return fail('Due date is required when a balance remains.');
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent || !effective(parent)) return fail('Choose a record on this moment.');
    if (parent.eventType === 'ORDER') {
      if (parent.contract?.orderSide === 'PURCHASE') return fail('A purchase order is billed with a purchase.');
      const cap = num(parent.contract?.estimatedAmount);
      if (cap > 0) {
        const used = postedAgainst(parent.operatingEventId, ['SALE'], existing);
        const headroom = cap - used;
        if (commercial - headroom > 0.00005 && amount - headroom > 0.00005) return fail('That is more than the order still open.');
      }
    } else if (!['SHIPMENT', 'DELIVERY'].includes(parent.eventType)) {
      return fail('That record cannot be linked here.');
    }
  }
  if (contract.goodsStatus !== 'DELIVERED' && contract.goodsStatus !== 'PARTIAL' && contract.goodsStatus !== 'LATER') {
    contract.goodsStatus = 'LATER';
  }
  contract.receivedAmount = money(received);
  return {
    ok: true,
    paymentStatus: state === 'RECEIVED' ? 'COLLECTED' : 'PENDING',
    amount: money(amount),
    parentEventId: input.parentEventId ?? null,
    quantity: mode === 'ITEMIZED'
      ? money(lines.reduce((total, line) => total + lineBaseQuantity(line, productForLine(line, existing)), 0))
      : null,
    contract,
    settleChild: state === 'PART_PAID' ? { type: 'COLLECTION', amount: money(received) } : null,
  };
}

function validatePurchase(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  if (!namedParty(input.counterpartyName)) return fail('Supplier is required.');
  const lines = input.lines ?? [];
  let amount = lines.length > 0 && lineTotal(lines) > 0 ? lineTotal(lines) : num(input.amount);
  if (!(amount > 0)) return fail('Amount is required.');
  const commercial = amount;
  const priced = applyPostedGst(input, existing, contract, 'PURCHASE', amount, lines);
  if (!priced.ok) return priced;
  amount = priced.amount;
  // Quick cost never asserts physical receipt (v6 / D3).
  if (contract.entryMode === 'QUICK' || lines.length === 0) {
    contract.receiptState = 'AWAITING';
    contract.quantityReceived = null;
  } else if (!contract.receiptState) {
    contract.receiptState = 'RECEIVED';
  }
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
  if (paid < amount && !day(input.dueAt)) return fail('Due date is required when a balance remains.');
  const taxFx = applyTaxAndFx(contract, amount);
  if (taxFx && !taxFx.ok) return taxFx;
  if (input.lines?.length) {
    for (const line of input.lines) {
      const converted = convertLine(line, existing);
      if (!converted.ok) return converted;
    }
  }
  let parentEventId: string | null = null;
  if (input.parentEventId) {
    const parent = existing.find((row) => row.operatingEventId === input.parentEventId);
    if (!parent || !effective(parent) || parent.eventType !== 'ORDER' || parent.contract?.orderSide !== 'PURCHASE') {
      return fail('Choose a purchase order on this moment.');
    }
    const cap = num(parent.contract?.estimatedAmount);
    if (cap > 0) {
      const used = postedAgainst(parent.operatingEventId, ['PURCHASE'], existing);
      const headroom = cap - used;
      if (commercial - headroom > 0.00005 && amount - headroom > 0.00005) return fail('That is more than the order still open.');
    }
    parentEventId = parent.operatingEventId;
  }
  contract.paymentState = state;
  contract.receivedAmount = money(paid);
  return {
    ok: true,
    paymentStatus: state === 'PAID' ? 'PAID' : 'PENDING',
    amount: money(amount),
    parentEventId,
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
  if (paid < amount && !day(input.dueAt)) return fail('Due date is required when a balance remains.');
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

function postedAgainst(parentId: string, types: string[], existing: TradingEvent[]): number {
  return existing
    .filter((row) => effective(row) && row.parentEventId === parentId && types.includes(row.eventType))
    .reduce((total, row) => total + num(row.amount), 0);
}

function validateStock(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  if (!input.title.trim()) return fail('Choose an item.');
  if (contract.stockAction !== 'SET' && contract.stockAction !== 'ADD' && contract.stockAction !== 'REDUCE') {
    return fail('Choose set, add, or reduce.');
  }
  if (!(num(input.quantity) >= 0) || (contract.stockAction !== 'SET' && !(num(input.quantity) > 0))) {
    return fail('Quantity is required.');
  }
  if (!contract.stockReason) return fail('Choose a reason.');
  if (contract.ownership !== 'OWNED' && contract.ownership !== 'CONSIGNMENT') contract.ownership = 'OWNED';
  if (num(input.quantity) > 0) {
    for (const line of input.lines ?? []) {
      if (!(num(line.quantity) > 0) || !String(line.unit ?? '').trim()) continue;
      const converted = convertLine(line, existing);
      if (!converted.ok) return converted;
    }
  }
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

function validateOrder(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  if (!String(input.channelName ?? '').trim() && !namedParty(input.counterpartyName)) {
    return fail('Buyer or channel is required.');
  }
  if (!contract.orderStatus) contract.orderStatus = 'PLACED';
  if (!['PLACED', 'PREPARING', 'COMPLETED', 'CANCELLED'].includes(contract.orderStatus)) return fail('Choose an order status.');
  if (contract.orderSide !== 'SALES' && contract.orderSide !== 'PURCHASE') contract.orderSide = 'SALES';
  if (input.lines?.length) {
    for (const line of input.lines) {
      if (!line.itemName.trim() || !(num(line.quantity) > 0) || !String(line.unit ?? '').trim()) {
        return fail('Each order line needs a name, quantity, and unit.');
      }
      const converted = convertLine(line, existing);
      if (!converted.ok) return converted;
    }
  }
  // Order amount is a commitment estimate — never revenue until billed as SALE or PURCHASE.
  if (input.amount) contract.estimatedAmount = input.amount;
  if (input.gst?.enabled) {
    const savedGst = contract.gst ?? null;
    const savedTax = contract.taxAmount ?? null;
    const savedMode = contract.taxMode ?? null;
    const savedStatus = contract.taxStatus ?? null;
    const preview = applyPostedGst(input, existing, contract, contract.orderSide === 'PURCHASE' ? 'PURCHASE' : 'SALE', num(input.amount), input.lines ?? []);
    if (preview.ok && contract.gst) contract.gstEstimate = contract.gst;
    contract.gst = savedGst;
    contract.taxAmount = savedTax;
    contract.taxMode = savedMode;
    contract.taxStatus = savedStatus;
  }
  if (input.dueAt) contract.promisedAt = input.dueAt;
  const lineQty = (input.lines ?? []).reduce((total, line) => total + lineBaseQuantity(line, productForLine(line, existing)), 0);
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: null,
    parentEventId: null,
    quantity: lineQty > 0 ? money(lineQty) : input.quantity ?? null,
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
    if (!parent || !effective(parent) || !['ORDER', 'SALE', 'PURCHASE'].includes(parent.eventType)) {
      return fail('Link delivery to a posted sale, purchase, or order.');
    }
  } else if (input.eventType === 'DELIVERY') {
    return fail('Choose the posted sale or purchase.');
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

function validateReturn(
  input: TradingWrite,
  existing: TradingEvent[],
  contract: OperatingContract
): RetailWriteResult | { ok: false; message: string } {
  const direction = contract.returnDirection;
  if (direction !== 'CUSTOMER' && direction !== 'SUPPLIER') return fail('Choose customer return or supplier return.');
  if (!contract.goodsOutcome) return fail('Choose whether goods returned to stock.');
  if (
    contract.goodsOutcome !== 'TO_STOCK' &&
    contract.goodsOutcome !== 'NOT_RETURNED' &&
    contract.goodsOutcome !== 'PENDING_INSPECTION'
  ) {
    return fail('Choose a goods outcome.');
  }
  if (!String(contract.returnReason ?? '').trim()) return fail('Return reason is required.');
  const parentId = input.parentEventId;
  if (!parentId) return fail('Choose the source sale or purchase.');
  const source = existing.find((row) => row.operatingEventId === parentId);
  if (!source || !effective(source)) return fail('Choose a posted source on this moment.');
  if (direction === 'CUSTOMER' && source.eventType !== 'SALE') return fail('Customer returns link to a sale.');
  if (direction === 'SUPPLIER' && source.eventType !== 'PURCHASE') return fail('Supplier returns link to a purchase.');
  const amount = num(input.amount);
  if (!(amount > 0)) return fail('Credit amount is required.');
  const open = obligationRemaining(source, existing, direction === 'CUSTOMER' ? 'COLLECTION' : 'PAYMENT');
  // Remaining open obligation (after collections/payments and prior returns).
  const ceiling = Math.max(open, 0);
  if (!(ceiling > 0)) return fail('Nothing remains open on that source.');
  if (amount - ceiling > 0.00005) return fail('Return cannot exceed the remaining open amount.');
  if (source.contract?.gst) contract.gst = scaleGstSnapshot(source.contract.gst, amount);
  const qty = num(input.quantity);
  if (contract.goodsOutcome === 'TO_STOCK' && !(qty > 0) && !(input.lines?.length)) {
    return fail('Quantity is required when goods return to stock.');
  }
  if (input.lines?.length) {
    for (const line of input.lines) {
      const converted = convertLine(line, existing);
      if (!converted.ok) return converted;
    }
  }
  return {
    ok: true,
    paymentStatus: 'NONE',
    amount: money(amount),
    parentEventId: parentId,
    quantity: qty > 0 ? money(qty) : input.lines?.length
      ? money(input.lines.reduce((total, line) => total + lineBaseQuantity(line, productForLine(line, existing)), 0))
      : null,
    contract,
    settleChild: null,
  };
}

/**
 * Owned on-hand only. Sale never decrements stock (sale ≠ dispatch).
 * Stock moves from STOCK adjustments, received PURCHASE qty, delivered SHIPMENT/DELIVERY,
 * and RETURN with goodsOutcome TO_STOCK. Quantities use baseQuantity when present.
 * Consignment never enters owned on-hand.
 */
export function tradingStockPositions(events: TradingEvent[]): StockPosition[] {
  const ordered = events
    .map((row, index) => ({ row, index }))
    .filter(({ row }) => effective(row) && row.contract?.ownership !== 'CONSIGNMENT')
    .sort((a, b) => a.row.occurredAt.localeCompare(b.row.occurredAt) || a.index - b.index);
  const positions = new Map<string, StockPosition>();
  const touch = (name: string, unit: string | null) => {
    const key = itemKey(name);
    const current = positions.get(key) ?? { item: name.trim(), unit, onHand: 0, reorderPoint: null };
    if (unit && !current.unit) current.unit = unit;
    positions.set(key, current);
    return current;
  };
  const qtyOf = (line: RetailLine) => lineBaseQuantity(line, null);
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
      const received =
        row.contract.receiptState === 'PART_RECEIVED'
          ? num(row.contract.quantityReceived)
          : lines.reduce((total, line) => total + qtyOf(line), 0);
      let left = received;
      for (const line of lines) {
        const take =
          row.contract.receiptState === 'PART_RECEIVED' ? Math.min(qtyOf(line) || left, left) : qtyOf(line);
        if (!(take > 0)) continue;
        touch(line.itemName, line.unit ?? null).onHand += take;
        left -= take;
      }
      continue;
    }
    if (
      (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
      row.contract?.shipmentStatus === 'DELIVERED'
    ) {
      const parent = events.find((item) => item.operatingEventId === row.parentEventId);
      const incoming = parent?.eventType === 'PURCHASE';
      const alreadyReceived = incoming && parent?.contract?.receiptState && parent.contract.receiptState !== 'AWAITING';
      if (!alreadyReceived) {
        const sign = incoming ? 1 : -1;
        const lines = row.lines ?? [];
        if (lines.length > 0) {
          for (const line of lines) {
            touch(line.itemName, line.unit ?? null).onHand += sign * qtyOf(line);
          }
        } else if (row.title.trim() && num(row.quantity) > 0) {
          touch(row.title, row.unit ?? null).onHand += sign * num(row.quantity);
        }
      }
      continue;
    }
    if (row.eventType === 'RETURN' && row.contract?.goodsOutcome === 'TO_STOCK') {
      const lines = row.lines ?? [];
      if (lines.length > 0) {
        for (const line of lines) {
          touch(line.itemName, line.unit ?? null).onHand += qtyOf(line);
        }
      } else if (row.title.trim() && num(row.quantity) > 0) {
        touch(row.title, row.unit ?? null).onHand += num(row.quantity);
      }
    }
  }
  return [...positions.values()];
}

/** Consignment quantity held separately from owned on-hand (D6). */
export function consignmentStockHint(events: TradingEvent[]): string | null {
  const held = new Map<string, number>();
  for (const row of events) {
    if (!effective(row) || row.contract?.ownership !== 'CONSIGNMENT') continue;
    if (row.eventType === 'PURCHASE' && row.contract.receiptState && row.contract.receiptState !== 'AWAITING') {
      for (const line of row.lines ?? []) {
        const key = itemKey(line.itemName);
        held.set(key, (held.get(key) ?? 0) + num(line.quantity));
      }
    }
    if (row.eventType === 'STOCK' && row.contract.stockAction === 'ADD') {
      const key = itemKey(row.title);
      held.set(key, (held.get(key) ?? 0) + num(row.quantity));
    }
  }
  if (held.size === 0) return null;
  const total = [...held.values()].reduce((sum, qty) => sum + qty, 0);
  return `Consignment held ${trim(total)} (not owned)`;
}

export function ordersCoverage(events: TradingEvent[]): string | null {
  const sales = events.filter((row) => row.eventType === 'SALE' && effective(row));
  if (sales.length === 0) return null;
  const totalValue = sales.reduce((sum, row) => sum + num(row.amount), 0);
  const itemized = sales.filter((row) => row.contract?.entryMode === 'ITEMIZED');
  const itemizedValue = itemized.reduce((sum, row) => sum + num(row.amount), 0);
  if (itemized.length === 0) return 'Line coverage not recorded';
  if (itemized.length === sales.length) return null;
  const pct = totalValue > 0 ? Math.round((itemizedValue / totalValue) * 100) : Math.round((itemized.length / sales.length) * 100);
  return `Based on itemized sales: ${pct}% of sales value (${itemized.length} of ${sales.length})`;
}

export type TradingDueBucket = 'OVERDUE' | 'DUE_SOON' | 'OPEN';

export function tradingDueBucket(dueAt: string | null | undefined, asOf: Date): TradingDueBucket {
  const due = day(dueAt);
  if (!due) return 'OPEN';
  const today = asOf.toISOString().slice(0, 10);
  if (due < today) return 'OVERDUE';
  if (due <= addDays(today, DUE_SOON_DAYS)) return 'DUE_SOON';
  return 'OPEN';
}

export function stockCoverage(events: TradingEvent[]): string | null {
  const sales = events.filter((row) => row.eventType === 'SALE' && effective(row));
  if (sales.length === 0) return null;
  const itemized = sales.filter((row) => row.contract?.entryMode === 'ITEMIZED').length;
  if (itemized === 0) return 'SKU stock excludes amount-only sales';
  if (itemized < sales.length) {
    return `Stock tracks itemized lines only · ${itemized} of ${sales.length} sales`;
  }
  return null;
}

export function buyersChannelsCoverage(events: TradingEvent[]): string | null {
  const sales = events.filter((row) => row.eventType === 'SALE' && effective(row));
  if (sales.length === 0) return null;
  const withChannel = sales.filter((row) => String(row.channelName ?? '').trim()).length;
  const withBuyer = sales.filter((row) => namedParty(row.counterpartyName)).length;
  if (withChannel === sales.length && withBuyer === sales.length) return null;
  if (withChannel === 0 && withBuyer === 0) return 'Buyer/channel attribution unavailable';
  return `Attributed ${withBuyer} buyers · ${withChannel} channels of ${sales.length} sales`;
}

export function currentBusinessDetail(events: TradingEvent[], visible: boolean, currency: string): string | null {
  const rows = events.filter((row) => effective(row));
  const sales = rows.filter((row) => row.eventType === 'SALE').length;
  const purchases = rows.filter((row) => row.eventType === 'PURCHASE').length;
  const stock = rows.filter((row) => row.eventType === 'STOCK').length;
  const issues = rows.filter((row) => row.eventType === 'ISSUE').length;
  if (sales + purchases + stock + issues === 0) return null;
  const moneyText = visible
    ? ` · Sales ${money(rows.filter((row) => row.eventType === 'SALE').reduce((sum, row) => sum + num(row.amount), 0))} ${currency}`
    : '';
  return `Sales ${sales} · Purchases ${purchases} · Stock moves ${stock} · Issues ${issues}${moneyText}`;
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

  for (const order of events) {
    if (order.eventType !== 'ORDER' || !effective(order)) continue;
    if (order.contract?.orderStatus === 'CANCELLED' || order.contract?.orderStatus === 'COMPLETED') continue;
    if (cards.some((card) => card.sourceEventId === order.operatingEventId && card.kind === 'OVERDUE_SHIPMENT')) continue;
    const party = order.counterpartyName || order.title;
    push({
      id: `order-${order.operatingEventId}`,
      title: `${party} order is still open`,
      kind: 'PENDING_ORDER',
      severity: 'LOW',
      amount: null,
      dueAt: order.contract?.promisedAt ?? order.dueAt ?? null,
      counterpartyName: order.counterpartyName ?? null,
      sourceEventId: order.operatingEventId,
      cta: 'order',
    });
  }

  for (const row of events) {
    if (!effective(row)) continue;
    const openShipment =
      (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
      row.contract?.shipmentStatus !== 'DELIVERED' &&
      row.contract?.shipmentStatus !== 'CANCELLED';
    const openSaleGoods = row.eventType === 'SALE' && (row.contract?.goodsStatus === 'PARTIAL' || row.contract?.goodsStatus === 'LATER');
    const openPurchaseGoods =
      row.eventType === 'PURCHASE' &&
      (row.contract?.receiptState === 'AWAITING' || row.contract?.receiptState === 'PART_RECEIVED');
    if (!openShipment && !openSaleGoods && !openPurchaseGoods) continue;
    if (
      cards.some(
        (card) =>
          card.sourceEventId === row.operatingEventId &&
          (card.kind === 'PENDING_DELIVERY' || card.kind === 'OVERDUE_DELIVERY')
      )
    ) {
      continue;
    }
    const promised = day(row.contract?.promisedAt);
    const overdue = Boolean(promised && promised < today);
    const party = row.counterpartyName || row.title;
    const receipt = openPurchaseGoods;
    const goodsWord = receipt ? 'receipt' : 'delivery';
    push({
      id: `${overdue ? 'goods-due' : 'move'}-${row.operatingEventId}`,
      title: overdue
        ? `${party} ${goodsWord} is overdue`
        : openShipment && !openSaleGoods && !openPurchaseGoods
          ? `${party} delivery is not complete`
          : `${party} ${goodsWord} is still open`,
      kind: overdue ? 'OVERDUE_DELIVERY' : 'PENDING_DELIVERY',
      severity: overdue ? 'HIGH' : 'LOW',
      amount: null,
      dueAt: row.contract?.promisedAt ?? null,
      counterpartyName: row.counterpartyName ?? null,
      sourceEventId: row.operatingEventId,
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
