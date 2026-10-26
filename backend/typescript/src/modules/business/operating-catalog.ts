/**
 * Four operating Business Moments. Family config drives setup, Quick Add, and
 * which Pulse figures a role may see.
 */

import {
  explicitAccepted,
  isManufacturingSubtype,
  manufacturingMeasure,
  manufacturingStock,
  type ManufacturingEvent,
} from './operating-manufacturing';
import {
  appointmentSummary,
  budgetAttention,
  customersNetworkDetail,
  isRetailSubtype,
  moneyMovingDetail,
  outcomeCoverage,
  postedSalesSummary,
  retailAttention,
  salesCoverage,
  serviceFlowSummary,
  stockPositions,
  type OperatingContract,
} from './operating-retail-contract';
import {
  buyersChannelsCoverage,
  consignmentStockHint,
  currentBusinessDetail,
  fulfilmentCoverage,
  isTradingSubtype,
  moneyMovingDetail as tradingMoneyMoving,
  ordersCommitmentsSummary,
  ordersCoverage,
  peopleNetworkDetail,
  stockCoverage,
  tradingAttention,
  tradingDueBucket,
  tradingStockPositions,
  obligationRemaining as tradingObligationRemaining,
} from './operating-trading-contract';
import { accumulateGstPosition, type GstPosition } from './operating-gst';
import {
  channelCoverage,
  isMarketingSubtype,
  activityClosed,
  marketingAttention,
  marketingCpl,
  marketingProgress,
  rollMarketingLedger,
  MARKETING_METRICS,
} from './operating-marketing';

export const OPERATING_FAMILY_CODES = [
  'MARKETING_CAMPAIGNS',
  'TRADING_DISTRIBUTION',
  'RETAIL_CONSUMER',
  'MANUFACTURING_PRODUCTION',
] as const;

export type OperatingFamilyCode = (typeof OPERATING_FAMILY_CODES)[number];

export const LEGACY_BUSINESS_FAMILY_CODES = [
  'TEAM_OPERATIONS',
  'BUSINESS_RUNWAY',
  'BUSINESS_OPERATIONS',
] as const;

export type OperatingRole =
  | 'OWNER_ADMIN'
  | 'OPERATIONS_LEAD'
  | 'FINANCE'
  | 'APPROVER'
  | 'CONTRIBUTOR'
  | 'TEAM_MEMBER'
  | 'OBSERVER';

export const OPERATING_ROLES: Array<{ code: OperatingRole; label: string }> = [
  { code: 'OWNER_ADMIN', label: 'Owner / Admin' },
  { code: 'OPERATIONS_LEAD', label: 'Operations Lead' },
  { code: 'FINANCE', label: 'Finance' },
  { code: 'APPROVER', label: 'Approver' },
  { code: 'CONTRIBUTOR', label: 'Contributor' },
  { code: 'TEAM_MEMBER', label: 'Team Member' },
  { code: 'OBSERVER', label: 'Observer' },
];

export type QuickAddCode =
  | 'SPEND'
  | 'RESULT'
  | 'PAYMENT'
  | 'COLLECTION'
  | 'BUDGET'
  | 'TARGET'
  | 'UPDATE'
  | 'ISSUE'
  | 'APPROVAL'
  | 'DELIVERABLE'
  | 'VENDOR_COST'
  | 'COMPLETE'
  | 'SALE'
  | 'PURCHASE'
  | 'STOCK'
  | 'ORDER'
  | 'SHIPMENT'
  | 'SERVICE'
  | 'APPOINTMENT'
  | 'PRODUCTION'
  | 'CUSTOMER_ORDER'
  | 'DELIVERY'
  | 'RETURN'
  | 'ACTIVITY'
  | 'PROGRESS'
  | 'INVOICE'
  | 'REVISION';

export type PaymentRelevance = 'NONE' | 'PAYABLE' | 'RECEIVABLE' | 'REIMBURSEMENT' | 'SETTLEMENT';

export interface QuickAddDef {
  code: QuickAddCode;
  label: string;
  consumesBudget: boolean;
  countsTarget: boolean;
  paymentRelevance: PaymentRelevance;
  financeOnly: boolean;
  needsAmount: boolean;
}

const QA = {
  SPEND: qa('SPEND', 'Add Spend', true, false, 'PAYABLE', false, true),
  RESULT: qa('RESULT', 'Add Result', false, true, 'NONE', false, true),
  PAYMENT: qa('PAYMENT', 'Add Payment', false, false, 'PAYABLE', true, true),
  COLLECTION: qa('COLLECTION', 'Add Collection', false, true, 'RECEIVABLE', true, true),
  BUDGET: qa('BUDGET', 'Revise Budget', false, false, 'NONE', true, true),
  TARGET: qa('TARGET', 'Revise Target', false, false, 'NONE', true, true),
  UPDATE: qa('UPDATE', 'Add Update', false, false, 'NONE', false, false),
  ISSUE: qa('ISSUE', 'Add Issue', false, false, 'NONE', false, false),
  APPROVAL: qa('APPROVAL', 'Request Approval', false, false, 'NONE', false, false),
  DELIVERABLE: qa('DELIVERABLE', 'Add Deliverable', false, false, 'NONE', false, false),
  VENDOR_COST: qa('VENDOR_COST', 'Add Vendor Cost', true, false, 'PAYABLE', false, true),
  COMPLETE: qa('COMPLETE', 'Mark Complete', false, false, 'NONE', false, false),
  SALE: qa('SALE', 'Record Sale', false, true, 'RECEIVABLE', false, true),
  PURCHASE: qa('PURCHASE', 'Record Purchase', true, false, 'PAYABLE', false, true),
  STOCK: qa('STOCK', 'Stock Update', false, false, 'NONE', false, false),
  ORDER: qa('ORDER', 'Order Update', false, false, 'NONE', false, false),
  SHIPMENT: qa('SHIPMENT', 'Shipment Update', false, false, 'NONE', false, false),
  SERVICE: qa('SERVICE', 'Record Service', false, true, 'RECEIVABLE', false, true),
  APPOINTMENT: qa('APPOINTMENT', 'Appointment / Job', false, false, 'NONE', false, false),
  PRODUCTION: qa('PRODUCTION', 'Record Production', false, true, 'NONE', false, true),
  CUSTOMER_ORDER: qa('CUSTOMER_ORDER', 'Customer Order', false, false, 'NONE', false, true),
  DELIVERY: qa('DELIVERY', 'Record Delivery', false, false, 'NONE', false, false),
  RETURN: qa('RETURN', 'Return / Credit Note', false, false, 'NONE', false, true),
  ACTIVITY: qa('ACTIVITY', 'Add Activity', false, false, 'NONE', false, false),
  PROGRESS: qa('PROGRESS', 'Update Progress', false, true, 'NONE', false, false),
  INVOICE: qa('INVOICE', 'Record Invoice', false, false, 'RECEIVABLE', true, true),
  REVISION: qa('REVISION', 'Budget / Target Revision', false, false, 'NONE', true, true),
} satisfies Record<QuickAddCode, QuickAddDef>;

function qa(
  code: QuickAddCode,
  label: string,
  consumesBudget: boolean,
  countsTarget: boolean,
  paymentRelevance: PaymentRelevance,
  financeOnly: boolean,
  needsAmount: boolean
): QuickAddDef {
  return { code, label, consumesBudget, countsTarget, paymentRelevance, financeOnly, needsAmount };
}

export interface OperatingSubtype {
  code: string;
  label: string;
  partyLabel: string;
  quickAdds: QuickAddCode[];
  targetMetrics: string[];
  requiresChannel: boolean;
}

export interface OperatingFamily {
  familyCode: OperatingFamilyCode;
  title: string;
  subtitle: string;
  figmaNodeId: string;
  activateLabel: string;
  subtypes: OperatingSubtype[];
}

const MARKETING_ADDS: QuickAddCode[] = [
  'ACTIVITY', 'PROGRESS', 'SPEND', 'PAYMENT', 'REVISION', 'APPROVAL', 'UPDATE', 'ISSUE', 'RESULT',
];

export const OPERATING_FAMILIES: OperatingFamily[] = [
  {
    familyCode: 'MARKETING_CAMPAIGNS',
    title: 'Marketing & Campaigns',
    subtitle: 'Run a client campaign or a campaign for your own business.',
    figmaNodeId: '2170:32721',
    activateLabel: 'Activate campaign',
    subtypes: [
      {
        code: 'CLIENT_CAMPAIGN',
        label: 'Client Campaign',
        partyLabel: 'Client',
        quickAdds: [...MARKETING_ADDS, 'INVOICE', 'COLLECTION'],
        targetMetrics: [...MARKETING_METRICS],
        requiresChannel: false,
      },
      {
        code: 'OWN_BUSINESS_CAMPAIGN',
        label: 'Own Business Campaign',
        partyLabel: 'Brand',
        quickAdds: [...MARKETING_ADDS],
        targetMetrics: MARKETING_METRICS.filter((metric) => metric !== 'CLIENT_BILLING'),
        requiresChannel: false,
      },
    ],
  },
  {
    familyCode: 'TRADING_DISTRIBUTION',
    title: 'Trading & Distribution',
    subtitle: 'Source, move, and collect across wholesale or distribution.',
    figmaNodeId: '2160:21022',
    activateLabel: 'Activate trading moment',
    subtypes: [
      {
        code: 'TRADING_WHOLESALE',
        label: 'Trading / Wholesale',
        partyLabel: 'Business',
        quickAdds: ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'BUDGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK', 'ORDER', 'SHIPMENT', 'RETURN'],
        targetMetrics: ['REVENUE', 'ORDERS', 'CUSTOM_COUNT'],
        requiresChannel: false,
      },
      {
        code: 'DISTRIBUTION_DEALERSHIP',
        label: 'Distribution / Dealership',
        partyLabel: 'Principal',
        quickAdds: ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'BUDGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK', 'ORDER', 'SHIPMENT', 'DELIVERY', 'RETURN'],
        targetMetrics: ['REVENUE', 'ORDERS', 'CUSTOM_COUNT'],
        requiresChannel: false,
      },
    ],
  },
  {
    familyCode: 'RETAIL_CONSUMER',
    title: 'Retail & Consumer',
    subtitle: 'Sell through a store, a food outlet, or a consumer service.',
    figmaNodeId: '2166:32567',
    activateLabel: 'Activate retail moment',
    subtypes: [
      {
        code: 'RETAIL_STORE',
        label: 'Retail Store',
        partyLabel: 'Store',
        quickAdds: ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'BUDGET', 'TARGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK', 'ORDER'],
        targetMetrics: ['REVENUE', 'ORDERS', 'CUSTOM_COUNT'],
        requiresChannel: true,
      },
      {
        code: 'RESTAURANT_FOOD',
        label: 'Restaurant / Food',
        partyLabel: 'Outlet',
        quickAdds: ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'BUDGET', 'TARGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK', 'ORDER'],
        targetMetrics: ['REVENUE', 'ORDERS', 'CUSTOM_COUNT'],
        requiresChannel: true,
      },
      {
        code: 'CONSUMER_SERVICE',
        label: 'Consumer Service',
        partyLabel: 'Service',
        quickAdds: ['SERVICE', 'APPOINTMENT', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'BUDGET', 'TARGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK'],
        targetMetrics: ['REVENUE', 'ORDERS', 'CUSTOM_COUNT'],
        requiresChannel: true,
      },
    ],
  },
  {
    familyCode: 'MANUFACTURING_PRODUCTION',
    title: 'Manufacturing & Production',
    subtitle: 'Produce to stock or make to a customer order.',
    figmaNodeId: '2177:25239',
    activateLabel: 'Activate production moment',
    subtypes: [
      {
        code: 'STANDARD_PRODUCTION',
        label: 'Standard Production',
        partyLabel: 'Plant',
        quickAdds: ['PRODUCTION', 'PURCHASE', 'SPEND', 'SALE', 'CUSTOMER_ORDER', 'DELIVERY', 'PAYMENT', 'COLLECTION', 'BUDGET', 'TARGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK'],
        targetMetrics: ['REVENUE', 'PRODUCTION_OUTPUT', 'DELIVERY_COUNT', 'ORDERS'],
        requiresChannel: false,
      },
      {
        code: 'MADE_TO_ORDER',
        label: 'Made-to-Order',
        partyLabel: 'Workshop',
        quickAdds: ['PRODUCTION', 'PURCHASE', 'SPEND', 'SALE', 'CUSTOMER_ORDER', 'DELIVERY', 'PAYMENT', 'COLLECTION', 'BUDGET', 'TARGET', 'UPDATE', 'ISSUE', 'APPROVAL', 'STOCK'],
        targetMetrics: ['REVENUE', 'PRODUCTION_OUTPUT', 'DELIVERY_COUNT', 'ORDERS'],
        requiresChannel: false,
      },
    ],
  },
];

export function isOperatingFamily(code: string | null | undefined): code is OperatingFamilyCode {
  return OPERATING_FAMILY_CODES.includes(String(code ?? '').toUpperCase() as OperatingFamilyCode);
}

export function isLegacyBusinessFamily(code: string | null | undefined): boolean {
  const c = String(code ?? '').toUpperCase();
  return (LEGACY_BUSINESS_FAMILY_CODES as readonly string[]).includes(c);
}

export function isOperatingSubtype(code: string | null | undefined): boolean {
  const c = String(code ?? '').toUpperCase();
  return OPERATING_FAMILIES.some((f) => f.subtypes.some((s) => s.code === c));
}

export function familyForSubtype(subtype: string): OperatingFamily | undefined {
  const c = subtype.toUpperCase();
  return OPERATING_FAMILIES.find((f) => f.subtypes.some((s) => s.code === c));
}

export function subtypeDef(familyCode: string, subtype: string): OperatingSubtype | undefined {
  const family = OPERATING_FAMILIES.find((f) => f.familyCode === familyCode.toUpperCase());
  return family?.subtypes.find((s) => s.code === subtype.toUpperCase());
}

export function quickAddDef(code: string): QuickAddDef | undefined {
  return QA[code.toUpperCase() as QuickAddCode];
}

export function quickAddsForSubtype(subtype: string): QuickAddDef[] {
  const family = familyForSubtype(subtype);
  const sub = family?.subtypes.find((s) => s.code === subtype.toUpperCase());
  if (!sub) return [];
  return sub.quickAdds.map((code) => QA[code]);
}

const FINANCE_VISIBLE = new Set<OperatingRole>(['OWNER_ADMIN', 'FINANCE', 'APPROVER']);
const APPROVER_ROLES = new Set<OperatingRole>(['OWNER_ADMIN', 'FINANCE', 'APPROVER']);
const AUTO_POST = new Set<OperatingRole>(['OWNER_ADMIN', 'FINANCE', 'OPERATIONS_LEAD']);

export function financeVisibleFor(role: OperatingRole): boolean {
  return FINANCE_VISIBLE.has(role);
}

export function canApprove(role: OperatingRole): boolean {
  return APPROVER_ROLES.has(role);
}

export function canSubmit(role: OperatingRole, def: QuickAddDef): boolean {
  if (role === 'OBSERVER') return false;
  if (def.financeOnly && !FINANCE_VISIBLE.has(role) && role !== 'OPERATIONS_LEAD') return false;
  if (role === 'TEAM_MEMBER' && (def.consumesBudget || def.financeOnly || def.paymentRelevance !== 'NONE')) {
    return false;
  }
  return true;
}

/** Controlled money actions from a non-posting role wait for a different approver. */
export function needsMakerChecker(role: OperatingRole, def: QuickAddDef): boolean {
  if (def.code === 'APPROVAL' || def.code === 'BUDGET' || def.code === 'TARGET' || def.code === 'REVISION' || def.code === 'INVOICE' || def.code === 'PAYMENT' || def.code === 'COLLECTION') {
    return !AUTO_POST.has(role);
  }
  if (def.consumesBudget || def.paymentRelevance === 'RECEIVABLE') {
    return role === 'CONTRIBUTOR' || role === 'TEAM_MEMBER';
  }
  return false;
}

export interface OperatingParticipant {
  displayName: string;
  role: OperatingRole;
  userId?: string;
}

export interface OperatingCounterparty {
  name: string;
  kind: string;
}

export type ActivationPolicy = 'NONE' | 'APPROVAL_REQUIRED';

export interface OperatingPreferences {
  subtype: string;
  identityName: string;
  partyName: string;
  description: string;
  currency: string;
  startDate: string;
  endDate: string;
  location: string;
  activationPolicy: ActivationPolicy;
  participants: OperatingParticipant[];
  counterparties: OperatingCounterparty[];
  channels: string[];
  tradeReach: string[];
  monthlyBudget: string;
  monthlyTarget: string;
  targetMetric: string;
  gstTracking: string;
  gstRegistration: string;
  gstState: string;
  gstin: string;
  salePriceIncludesGst: string;
  purchasePriceIncludesGst: string;
  gstEffectiveFrom: string;
  clientFee: string;
  offering: string;
  creativeApprovalRequired: string;
  reportingCadence: string;
  conversionNumerator: string;
  conversionDenominator: string;
  conversionDefinitionVersion: string;
  campaignStatus: string;
}

function asRole(value: unknown): OperatingRole | undefined {
  const code = String(value ?? '').toUpperCase() as OperatingRole;
  return OPERATING_ROLES.some((r) => r.code === code) ? code : undefined;
}

function positiveAmount(value: unknown): string | undefined {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) return value.toFixed(4);
  const raw = String(value ?? '').replace(/[₹,\s]/g, '');
  if (!/^\d+(\.\d{1,4})?$/.test(raw)) return undefined;
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) return undefined;
  return n.toFixed(4);
}

export function normalizeOperatingPreferences(
  familyCode: OperatingFamilyCode,
  input: Record<string, unknown> | undefined,
  creator: { userId: string; displayName: string }
): OperatingPreferences {
  const prefs = input ?? {};
  const subtype = String(prefs.subtype ?? '').toUpperCase();
  const sub = subtypeDef(familyCode, subtype);
  if (!sub) {
    throw new Error(`Unknown subtype for ${familyCode}: ${subtype || '(missing)'}`);
  }
  const identityName = String(prefs.identityName ?? prefs.title ?? '').trim();
  const partyName = String(prefs.partyName ?? '').trim();
  if (!identityName) throw new Error('Moment name is required.');
  if (!partyName) throw new Error(`${sub.partyLabel} name is required.`);
  const monthlyBudget = positiveAmount(prefs.monthlyBudget) ?? '';
  const monthlyTarget = positiveAmount(prefs.monthlyTarget) ?? '';
  if (familyCode === 'MARKETING_CAMPAIGNS') {
    if (!monthlyBudget) throw new Error('Monthly budget must be a positive amount.');
  } else if (familyCode !== 'RETAIL_CONSUMER') {
    if (!monthlyBudget) throw new Error('Monthly budget must be a positive amount.');
    if (!monthlyTarget) throw new Error('Monthly target must be a positive amount.');
  }
  const targetMetric = String(prefs.targetMetric ?? sub.targetMetrics[0]).toUpperCase();
  if (!sub.targetMetrics.includes(targetMetric)) {
    throw new Error(`Target measure must be one of ${sub.targetMetrics.join(', ')}.`);
  }
  const channels = Array.isArray(prefs.channels)
    ? prefs.channels.map((c) => String(c).trim()).filter(Boolean)
    : [];
  const reachAllowed = new Set(['Local', 'Domestic', 'International']);
  const tradeReach = Array.isArray(prefs.tradeReach)
    ? [...new Set(prefs.tradeReach.map((item) => String(item).trim()).filter((item) => reachAllowed.has(item)))]
    : [];
  if (sub.requiresChannel && channels.length === 0) {
    throw new Error('Select at least one channel.');
  }
  const startDate = String(prefs.startDate ?? '').slice(0, 10);
  const endDate = String(prefs.endDate ?? '').slice(0, 10);
  if (familyCode === 'MARKETING_CAMPAIGNS') {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) {
      throw new Error('Campaign start date is required.');
    }
    if (endDate && (!/^\d{4}-\d{2}-\d{2}$/.test(endDate) || endDate < startDate)) {
      throw new Error('Campaign end date must be on or after the start date.');
    }
  }
  const participants: OperatingParticipant[] = [];
  if (Array.isArray(prefs.participants)) {
    for (const row of prefs.participants) {
      if (!row || typeof row !== 'object') continue;
      const rec = row as Record<string, unknown>;
      const role = asRole(rec.role);
      const displayName = String(rec.displayName ?? '').trim();
      if (!role || !displayName) continue;
      participants.push({
        displayName,
        role,
        userId: typeof rec.userId === 'string' ? rec.userId : undefined,
      });
    }
  }
  if (!participants.some((p) => p.userId === creator.userId)) {
    participants.unshift({
      displayName: creator.displayName || 'Owner',
      role: 'OWNER_ADMIN',
      userId: creator.userId,
    });
  }
  const counterparties: OperatingCounterparty[] = [];
  if (Array.isArray(prefs.counterparties)) {
    for (const row of prefs.counterparties) {
      if (!row || typeof row !== 'object') continue;
      const rec = row as Record<string, unknown>;
      const name = String(rec.name ?? '').trim();
      if (!name) continue;
      counterparties.push({ name, kind: String(rec.kind ?? 'OTHER').trim() || 'OTHER' });
    }
  }
  return {
    subtype,
    identityName,
    partyName,
    description: String(prefs.description ?? '').trim(),
    currency: String(prefs.currency ?? 'INR').toUpperCase(),
    startDate: familyCode === 'MARKETING_CAMPAIGNS' ? startDate : String(prefs.startDate ?? ''),
    endDate: familyCode === 'MARKETING_CAMPAIGNS' ? endDate : String(prefs.endDate ?? ''),
    location: String(prefs.location ?? '').trim(),
    activationPolicy: String(prefs.activationPolicy ?? '').toUpperCase() === 'APPROVAL_REQUIRED'
      ? 'APPROVAL_REQUIRED'
      : 'NONE',
    participants,
    counterparties,
    channels,
    tradeReach,
    monthlyBudget,
    monthlyTarget,
    targetMetric,
    ...normalizeGstPreferences(familyCode, prefs),
    ...normalizeMarketingPreferences(familyCode, subtype, prefs),
  };
}

function normalizeMarketingPreferences(
  familyCode: OperatingFamilyCode,
  subtype: string,
  prefs: Record<string, unknown>
): Pick<
  OperatingPreferences,
  | 'clientFee'
  | 'offering'
  | 'creativeApprovalRequired'
  | 'reportingCadence'
  | 'conversionNumerator'
  | 'conversionDenominator'
  | 'conversionDefinitionVersion'
  | 'campaignStatus'
> {
  const empty = {
    clientFee: '',
    offering: '',
    creativeApprovalRequired: '',
    reportingCadence: '',
    conversionNumerator: '',
    conversionDenominator: '',
    conversionDefinitionVersion: '',
    campaignStatus: 'OPEN',
  };
  if (familyCode !== 'MARKETING_CAMPAIGNS') return empty;
  const client = subtype === 'CLIENT_CAMPAIGN';
  const cadence = String(prefs.reportingCadence ?? '').trim().toUpperCase();
  return {
    clientFee: client ? positiveAmount(prefs.clientFee) ?? '' : '',
    offering: String(prefs.offering ?? '').trim(),
    creativeApprovalRequired: String(prefs.creativeApprovalRequired ?? '').toUpperCase() === 'YES' ? 'YES' : '',
    reportingCadence: client && ['WEEKLY', 'MONTHLY', 'CUSTOM'].includes(cadence) ? cadence : '',
    conversionNumerator: String(prefs.conversionNumerator ?? '').trim(),
    conversionDenominator: String(prefs.conversionDenominator ?? '').trim(),
    conversionDefinitionVersion: String(prefs.conversionDefinitionVersion ?? '').trim(),
    campaignStatus: String(prefs.campaignStatus ?? '').toUpperCase() === 'CLOSED' ? 'CLOSED' : 'OPEN',
  };
}

function gstChoice(value: unknown, allowed: string[]): string {
  const code = String(value ?? '').trim().toUpperCase();
  return allowed.includes(code) ? code : '';
}

function normalizeGstPreferences(
  familyCode: OperatingFamilyCode,
  prefs: Record<string, unknown>
): Pick<
  OperatingPreferences,
  'gstTracking' | 'gstRegistration' | 'gstState' | 'gstin' | 'salePriceIncludesGst' | 'purchasePriceIncludesGst' | 'gstEffectiveFrom'
> {
  if (familyCode !== 'TRADING_DISTRIBUTION' && familyCode !== 'RETAIL_CONSUMER' && familyCode !== 'MANUFACTURING_PRODUCTION') {
    return {
      gstTracking: '',
      gstRegistration: '',
      gstState: '',
      gstin: '',
      salePriceIncludesGst: '',
      purchasePriceIncludesGst: '',
      gstEffectiveFrom: '',
    };
  }
  const effective = String(prefs.gstEffectiveFrom ?? '').trim();
  return {
    gstTracking: gstChoice(prefs.gstTracking, ['YES', 'NOT_NOW']),
    gstRegistration: gstChoice(prefs.gstRegistration, ['REGULAR', 'COMPOSITION', 'UNREGISTERED', 'NOT_SURE']),
    gstState: String(prefs.gstState ?? '').trim(),
    gstin: String(prefs.gstin ?? '').trim(),
    salePriceIncludesGst: gstChoice(prefs.salePriceIncludesGst, ['YES', 'NO']),
    purchasePriceIncludesGst: gstChoice(prefs.purchasePriceIncludesGst, ['YES', 'NO']),
    gstEffectiveFrom: /^\d{4}-\d{2}-\d{2}$/.test(effective) ? effective : '',
  };
}

export interface OperatingEventRow {
  operatingEventId: string;
  eventType: string;
  title: string;
  note: string | null;
  amount: string | null;
  currencyCode: string | null;
  counterpartyName: string | null;
  channelName: string | null;
  paymentRelevance: string;
  paymentStatus: string;
  approvalStatus: string;
  submittedByUserId: string;
  decidedByUserId: string | null;
  occurredAt: string;
  parentEventId?: string | null;
  quantity?: string | null;
  unit?: string | null;
  lines?: OperatingLine[];
  dueAt?: string | null;
  master?: OperatingMaster | null;
  metric?: 'LEADS' | 'CONVERSIONS' | null;
  rejectedQuantity?: string | null;
  contract?: OperatingContract | null;
  marketing?: import('./operating-marketing').MarketingFacts | null;
  idempotencyKey?: string | null;
}

export interface OperatingLine {
  itemName: string;
  quantity?: string | null;
  unit?: string | null;
  unitPrice?: string | null;
  masterId?: string | null;
  baseQuantity?: string | null;
}

export interface OperatingMaster {
  kind: string;
  name: string;
  unit?: string | null;
  dimension?: string | null;
  conversionToBase?: string | null;
  tracked?: boolean | null;
  barcode?: string | null;
  gstApplicable?: string | null;
  gstClassification?: string | null;
  gstRate?: string | null;
  priceIncludesGst?: string | null;
  salePriceIncludesGst?: string | null;
  purchasePriceIncludesGst?: string | null;
  gstEffectiveFrom?: string | null;
  hsn?: string | null;
  gstRegistration?: string | null;
  businessState?: string | null;
  billingState?: string | null;
  deliveryState?: string | null;
  gstin?: string | null;
}

export interface OperatingProjection {
  retired: boolean;
  familyCode: string;
  subtype: string;
  subtypeLabel: string;
  title: string;
  partyName: string;
  role: OperatingRole;
  financeVisible: boolean;
  currency: string;
  startDate: string;
  endDate: string;
  participants: string[];
  masters: Array<{
    id: string;
    kind: string;
    name: string;
    unit: string | null;
    dimension?: string | null;
    conversionToBase?: string | null;
    tracked?: boolean | null;
    barcode?: string | null;
    gstApplicable?: string | null;
    gstClassification?: string | null;
    gstRate?: string | null;
    priceIncludesGst?: string | null;
    salePriceIncludesGst?: string | null;
    purchasePriceIncludesGst?: string | null;
    gstEffectiveFrom?: string | null;
    hsn?: string | null;
    gstRegistration?: string | null;
    businessState?: string | null;
    billingState?: string | null;
    deliveryState?: string | null;
    gstin?: string | null;
  }>;
  location: string;
  budget: { amount: string; spent: string; remaining: string; configured: boolean } | null;
  target: { metric: string; value: string; progress: string; configured: boolean; coverage?: string | null } | null;
  finance: OperatingFinanceTotals | null;
  continuity: OperatingContinuity;
  attention: Array<{
    id: string;
    title: string;
    kind: string;
    severity?: string | null;
    amount?: string | null;
    dueAt?: string | null;
    counterpartyName?: string | null;
    sourceEventId?: string | null;
    cta?: string | null;
  }>;
  paymentReadiness: Array<{
    id: string;
    title: string;
    amount: string;
    relevance: string;
    status: string;
    counterpartyName: string | null;
    dueAt: string | null;
    dueBucket?: 'OVERDUE' | 'DUE_SOON' | 'OPEN' | null;
    occurredAt: string | null;
    total: string | null;
  }>;
  activity: Array<{
    id: string;
    title: string;
    eventType: string;
    occurredAt: string;
    amount: string | null;
    channelName: string | null;
    counterpartyName: string | null;
    paymentStatus: string | null;
    approvalStatus: string | null;
    note: string | null;
    quantity: string | null;
    unit: string | null;
    parentEventId: string | null;
    status: string | null;
    orderSide: string | null;
    estimatedAmount: string | null;
  }>;
  life: { channels: string[]; openCommitments: number; resultProgress: string };
  memory: { revisions: Array<{ id: string; title: string; occurredAt: string }> };
  boards: {
    moments: OperatingBoardSection[];
    life: OperatingBoardSection[];
    memory: OperatingBoardSection[];
  };
  quickAdds: Array<{ code: string; label: string; needsAmount: boolean }>;
  gstTracking: string;
  gstRegistration: string;
  gstState: string;
  gstEffectiveFrom: string;
  salePriceIncludesGst: string;
  purchasePriceIncludesGst: string;
  gstPosition: GstPosition | null;
  marketing: MarketingInsight | null;
}

export interface MarketingInsight {
  cpl: string | null;
  pacing: string | null;
  coverage: string;
  reimbursable: string;
  clientContribution: string | null;
  campaignStatus: string;
  clientFee: string;
  offering: string;
  creativeApprovalRequired: string;
  reportingCadence: string;
}

export interface OperatingBoardRow {
  id: string;
  title: string;
  eventType: string;
  occurredAt: string;
  amount: string | null;
  channelName: string | null;
  counterpartyName: string | null;
  paymentStatus: string | null;
  approvalStatus: string | null;
  note: string | null;
}

export interface OperatingFinanceTotals {
  sales: string;
  received: string;
  outstanding: string;
  payable: string;
  eligibleCost: string;
  cashPaid: string;
}

export interface OperatingContinuity {
  payable: string | null;
  receivable: string | null;
  ordered: string;
  dispatched: string;
  delivered: string;
  pendingDelivery: string;
  planned: string;
  produced: string;
  rejected: string;
  accepted: string;
  unsoldAccepted: string;
  pendingProduction: string;
  approved: string | null;
  approvedUnspent: string | null;
  openBookings: string | null;
  leads: string;
  conversions: string;
  costPerLead: string | null;
  conversionRate: string | null;
}

export interface OperatingBoardSection {
  code: string;
  title: string;
  detail: string;
  rows: OperatingBoardRow[];
  coverage?: string | null;
}

function num(value: string | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

function toManufacturing(row: OperatingEventRow): ManufacturingEvent {
  return {
    operatingEventId: row.operatingEventId,
    eventType: row.eventType,
    title: row.title,
    amount: row.amount,
    paymentStatus: row.paymentStatus,
    approvalStatus: row.approvalStatus,
    occurredAt: row.occurredAt,
    parentEventId: row.parentEventId,
    quantity: row.quantity,
    unit: row.unit,
    counterpartyName: row.counterpartyName,
    lines: row.lines,
    contract: row.contract,
    rejectedQuantity: row.rejectedQuantity,
  };
}

function money(value: number): string {
  return value.toFixed(4);
}

function effective(row: OperatingEventRow): boolean {
  return row.approvalStatus !== 'PENDING' && row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

const PAYABLE_TYPES = new Set(['PURCHASE', 'SPEND', 'VENDOR_COST']);
const SETTLED_STATUSES = new Set(['COLLECTED', 'PAID', 'SETTLED', 'CANCELLED']);
const REQUIRED_PARENT: Record<string, readonly string[]> = {
  PAYMENT: ['PURCHASE', 'SPEND', 'VENDOR_COST'],
  COLLECTION: ['SALE', 'SERVICE', 'CUSTOMER_ORDER', 'INVOICE'],
  SHIPMENT: ['ORDER', 'CUSTOMER_ORDER'],
  DELIVERY: ['ORDER', 'CUSTOMER_ORDER'],
};
const OPTIONAL_PARENT: Record<string, readonly string[]> = {
  SERVICE: ['APPOINTMENT'],
  PRODUCTION: ['CUSTOMER_ORDER', 'TARGET'],
  SALE: ['CUSTOMER_ORDER', 'ORDER', 'APPOINTMENT'],
};
const QUANTITY_CHILDREN = new Set(['SHIPMENT', 'DELIVERY', 'PRODUCTION']);

function settled(row: OperatingEventRow): boolean {
  return SETTLED_STATUSES.has(row.paymentStatus);
}

function countsTowardAllocation(row: OperatingEventRow): boolean {
  return row.approvalStatus !== 'REJECTED' && row.paymentStatus !== 'CANCELLED';
}

function childAmount(
  events: OperatingEventRow[],
  parentId: string,
  types: Set<string>,
  onlyEffective: boolean
): number {
  return events.reduce((total, row) => {
    if (!types.has(row.eventType)) return total;
    if (onlyEffective ? !effective(row) : !countsTowardAllocation(row)) return total;
    const parts = row.contract?.allocations ?? [];
    if (parts.length > 0) {
      return total + parts.reduce((sum, part) => (part.sourceId === parentId ? sum + num(part.amount) : sum), 0);
    }
    if (row.parentEventId !== parentId) return total;
    return total + num(row.amount);
  }, 0);
}

function childQuantity(events: OperatingEventRow[], parentId: string, types: Set<string>): number {
  return events.reduce((total, row) => {
    if (row.parentEventId !== parentId || !types.has(row.eventType) || !effective(row)) return total;
    return total + num(row.quantity);
  }, 0);
}

function obligationRemaining(row: OperatingEventRow, events: OperatingEventRow[], childTypes: Set<string>): number {
  if (!effective(row) || settled(row)) return 0;
  // Include RETURN so credit notes reduce open receivable/payable (QA-07).
  const types = new Set(childTypes);
  types.add('RETURN');
  return Math.max(0, num(row.amount) - childAmount(events, row.operatingEventId, types, true));
}

function producedUnits(row: OperatingEventRow): number {
  if (row.quantity != null && row.quantity !== '') return num(row.quantity);
  return num(row.amount);
}

function countedUnits(row: OperatingEventRow): number {
  if (row.quantity != null && row.quantity !== '') return num(row.quantity);
  return num(row.amount);
}

function ratio(value: number): string {
  return value.toFixed(2);
}

export function operatingContinuity(
  events: OperatingEventRow[],
  familyCode: string,
  visible: boolean
): OperatingContinuity {
  const rows = events.filter(effective);
  let payable = 0;
  let receivable = 0;
  for (const row of rows) {
    if (PAYABLE_TYPES.has(row.eventType)) {
      payable += obligationRemaining(row, events, new Set(['PAYMENT']));
    }
    if (row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'INVOICE') {
      receivable += obligationRemaining(row, events, new Set(['COLLECTION']));
    }
  }

  const orders = rows.filter((row) => row.eventType === 'ORDER' || row.eventType === 'CUSTOMER_ORDER');
  const ordered = orders.reduce((total, row) => total + num(row.quantity), 0);
  const dispatched = rows.filter((row) => row.eventType === 'SHIPMENT').reduce((total, row) => total + num(row.quantity), 0);
  const delivered = rows.filter((row) => row.eventType === 'DELIVERY').reduce((total, row) => total + num(row.quantity), 0);
  let pendingDelivery = 0;
  for (const order of orders) {
    pendingDelivery += Math.max(0, num(order.quantity) - childQuantity(events, order.operatingEventId, new Set(['DELIVERY'])));
  }

  let planned = 0;
  for (const row of rows) {
    if (row.eventType === 'TARGET' && row.quantity != null && row.quantity !== '') planned = num(row.quantity);
  }
  const produced = rows
    .filter((row) => row.eventType === 'PRODUCTION')
    .reduce((total, row) => total + producedUnits(row), 0);
  const rejected = rows
    .filter((row) => row.eventType === 'PRODUCTION')
    .reduce((total, row) => total + num(row.rejectedQuantity), 0);
  const accepted = rows
    .filter((row) => row.eventType === 'PRODUCTION')
    .reduce((total, row) => total + (explicitAccepted(toManufacturing(row)) ?? 0), 0);
  const sold = rows.filter((row) => row.eventType === 'SALE').reduce((total, row) => total + num(row.quantity), 0);
  const unsoldAccepted = Math.max(0, accepted - sold);

  let pendingProduction = 0;
  if (familyCode === 'MANUFACTURING_PRODUCTION') {
    if (orders.length > 0) {
      for (const order of orders) {
        const linked = events
          .filter((row) => row.parentEventId === order.operatingEventId && row.eventType === 'PRODUCTION' && effective(row))
          .reduce((total, row) => total + producedUnits(row), 0);
        pendingProduction += Math.max(0, num(order.quantity) - linked);
      }
    } else {
      pendingProduction = Math.max(0, planned - produced);
    }
  }

  const approved = rows.filter((row) => row.eventType === 'APPROVAL').reduce((total, row) => total + num(row.amount), 0);
  const spent = rows.reduce(
    (total, row) => total + (quickAddDef(row.eventType)?.consumesBudget ? num(row.amount) : 0),
    0
  );
  const appointments = rows.filter((row) => row.eventType === 'APPOINTMENT');
  let openBookings = 0;
  for (const appointment of appointments) {
    openBookings += Math.max(
      0,
      num(appointment.amount) - childAmount(events, appointment.operatingEventId, new Set(['SERVICE']), true)
    );
  }

  let leads = 0;
  let conversions = 0;
  for (const row of rows) {
    const code = (row.marketing?.metricCode || row.metric || '').toUpperCase();
    if (row.eventType !== 'RESULT' && row.eventType !== 'PROGRESS') continue;
    if (code === 'LEADS') leads += countedUnits(row);
    if (code === 'CONVERSIONS') conversions += countedUnits(row);
  }

  return {
    payable: visible ? money(payable) : null,
    receivable: visible ? money(receivable) : null,
    ordered: money(ordered),
    dispatched: money(dispatched),
    delivered: money(delivered),
    pendingDelivery: money(pendingDelivery),
    planned: money(planned),
    produced: money(produced),
    rejected: money(rejected),
    accepted: money(accepted),
    unsoldAccepted: money(unsoldAccepted),
    pendingProduction: money(pendingProduction),
    approved: visible ? money(approved) : null,
    approvedUnspent: visible ? money(Math.max(0, approved - spent)) : null,
    openBookings: visible ? money(openBookings) : null,
    leads: money(leads),
    conversions: money(conversions),
    costPerLead: visible && leads > 0 ? ratio(spent / leads) : null,
    conversionRate: leads > 0 ? ratio((conversions / leads) * 100) : null,
  };
}

export function validateOperatingAllocation(input: {
  eventType: string;
  parent: OperatingEventRow | null;
  siblings: OperatingEventRow[];
  amount: string | null;
  quantity: string | null;
  allowUnlinked?: boolean;
}): { ok: true } | { ok: false; message: string } {
  const required = REQUIRED_PARENT[input.eventType];
  const optional = OPTIONAL_PARENT[input.eventType];
  if (required && !input.parent) {
    if (input.allowUnlinked) return { ok: true };
    return { ok: false, message: 'Choose the record this settles.' };
  }
  if (!input.parent) return { ok: true };
  const allowed = required ?? optional;
  if (!allowed?.includes(input.parent.eventType)) {
    return { ok: false, message: 'That record cannot be linked here.' };
  }
  if (
    (input.parent.eventType === 'ORDER' || input.parent.eventType === 'APPOINTMENT') &&
    (input.eventType === 'SALE' || input.eventType === 'SERVICE')
  ) {
    return { ok: true };
  }
  if (input.parent.approvalStatus === 'REJECTED' || settled(input.parent)) {
    return { ok: false, message: 'That record is no longer open.' };
  }
  if (QUANTITY_CHILDREN.has(input.eventType)) {
    if (input.eventType !== 'PRODUCTION' && !(num(input.quantity) > 0)) {
      return { ok: false, message: 'Quantity is required.' };
    }
    if (input.eventType === 'PRODUCTION' && !(num(input.quantity) > 0)) return { ok: true };
    if (!(num(input.parent.quantity) > 0)) {
      return { ok: false, message: 'The order has no quantity.' };
    }
    const used = input.siblings
      .filter((row) => countsTowardAllocation(row) && row.eventType === input.eventType)
      .reduce((total, row) => total + num(row.quantity), 0);
    if (used + num(input.quantity) - num(input.parent.quantity) > 0.00005) {
      return { ok: false, message: 'That is more than the quantity still open.' };
    }
    return { ok: true };
  }
  if (!(num(input.amount) > 0)) {
    return { ok: false, message: 'Amount is required.' };
  }
  if (!(num(input.parent.amount) > 0)) {
    return { ok: false, message: 'That record has no amount.' };
  }
  const used = input.siblings
    .filter((row) => countsTowardAllocation(row) && row.eventType === input.eventType)
    .reduce((total, row) => total + num(row.amount), 0);
  if (used + num(input.amount) - num(input.parent.amount) > 0.00005) {
    return { ok: false, message: 'That is more than the amount still open.' };
  }
  return { ok: true };
}

function readinessAmount(row: OperatingEventRow, events: OperatingEventRow[]): number | null {
  if (PAYABLE_TYPES.has(row.eventType)) {
    return obligationRemaining(row, events, new Set(['PAYMENT']));
  }
  if (row.eventType === 'SALE' || row.eventType === 'SERVICE') {
    return obligationRemaining(row, events, new Set(['COLLECTION']));
  }
  if (row.eventType === 'CUSTOMER_ORDER') {
    const invoiced = events.some(
      (child) => child.parentEventId === row.operatingEventId && child.eventType === 'SALE' && effective(child)
    );
    if (invoiced) return null;
    return obligationRemaining(row, events, new Set(['COLLECTION']));
  }
  return num(row.amount);
}

export function projectOperatingMoment(args: {
  familyCode: string;
  prefs: OperatingPreferences;
  role: OperatingRole;
  events: OperatingEventRow[];
  asOf?: Date;
}): OperatingProjection {
  const sub = subtypeDef(args.familyCode, args.prefs.subtype);
  const visible = financeVisibleFor(args.role);
  const asOf = args.asOf ?? new Date();
  const pulseEvents = eventsInRange(args.events, args.prefs);
  let budget = num(args.prefs.monthlyBudget);
  let target = num(args.prefs.monthlyTarget);
  let budgetConfigured = budget > 0;
  let targetConfigured = target > 0;
  let spent = 0;
  let progress = 0;
  const revisions: OperatingProjection['memory']['revisions'] = [];
  for (const row of pulseEvents) {
    if (!effective(row)) continue;
    const def = quickAddDef(row.eventType);
    const amount = num(row.amount);
    if (row.eventType === 'BUDGET' && amount > 0) {
      budget = amount;
      budgetConfigured = true;
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
    } else if (row.eventType === 'TARGET' && amount > 0) {
      target = amount;
      targetConfigured = true;
      revisions.push({ id: row.operatingEventId, title: row.title, occurredAt: row.occurredAt });
    }
    if (def?.consumesBudget && args.familyCode !== 'MARKETING_CAMPAIGNS') spent += recognizedValue(row);
    if (args.familyCode === 'MARKETING_CAMPAIGNS') {
      // Rolled below so revisions, incremental, and cumulative series stay one ledger.
    } else if (args.familyCode === 'MANUFACTURING_PRODUCTION') {
      // Measured once below so unlike units are not added row by row.
    } else if (def?.countsTarget && row.eventType !== 'COLLECTION' && row.eventType !== 'PAYMENT') {
      progress += amount;
    }
  }
  let marketingCoverage = 'N/A';
  if (args.familyCode === 'MARKETING_CAMPAIGNS') {
    const rolled = rollMarketingLedger(
      pulseEvents,
      { budget: num(args.prefs.monthlyBudget), target: num(args.prefs.monthlyTarget) },
      asOf.toISOString().slice(0, 10)
    );
    budget = rolled.budget;
    target = rolled.target;
    budgetConfigured = rolled.budgetConfigured;
    targetConfigured = rolled.targetConfigured;
    spent = rolled.spent;
    revisions.splice(0, revisions.length, ...rolled.revisions);
    const measured = marketingProgress(pulseEvents, args.prefs.targetMetric);
    progress = measured.progress;
    marketingCoverage = measured.coverage;
  }
  // Pulse recorded sales is net posted sales. Gross countsTarget amounts ignore credit notes.
  if (args.familyCode === 'TRADING_DISTRIBUTION' && args.prefs.targetMetric.toUpperCase() === 'REVENUE') {
    progress = netPostedSales(pulseEvents);
  }
  let manufacturingCoverage: string | null = null;
  if (args.familyCode === 'MANUFACTURING_PRODUCTION') {
    const measured = manufacturingMeasure(pulseEvents.map(toManufacturing), args.prefs.targetMetric);
    progress = measured.progress;
    manufacturingCoverage = measured.coverage;
  }
  const legacyAttention = pulseEvents
    .filter((row) => row.eventType === 'ISSUE' || row.approvalStatus === 'PENDING')
    .filter((row) => row.approvalStatus !== 'REJECTED')
    .map((row) => ({
      id: row.operatingEventId,
      title: row.title,
      kind: row.approvalStatus === 'PENDING' ? 'APPROVAL' : 'ISSUE',
      severity: row.approvalStatus === 'PENDING' ? 'MEDIUM' : 'LOW',
      amount: null,
      dueAt: row.dueAt ?? null,
      counterpartyName: row.counterpartyName ?? null,
      sourceEventId: row.operatingEventId,
      cta: row.approvalStatus === 'PENDING' ? 'approval' : null,
    }));
  const attention = isRetailSubtype(args.prefs.subtype)
    ? sortRetailAttention([
        ...retailAttention(pulseEvents, visible, canApprove(args.role), asOf),
        ...budgetAttention(spent, budget, budgetConfigured, visible),
      ])
    : isTradingSubtype(args.prefs.subtype)
      ? sortRetailAttention([
          ...tradingAttention(pulseEvents, visible, canApprove(args.role), asOf),
          ...budgetAttention(spent, budget, budgetConfigured, visible),
        ])
      : isMarketingSubtype(args.prefs.subtype)
        ? marketingAttention({
            events: pulseEvents,
            subtype: args.prefs.subtype,
            visible,
            canApprove: canApprove(args.role),
            asOf,
            budget,
            spent,
            budgetConfigured,
            target,
            progress,
            targetConfigured,
            progressCoverage: marketingProgress(pulseEvents, args.prefs.targetMetric).coverage,
            startDate: args.prefs.startDate,
            endDate: args.prefs.endDate,
          })
        : legacyAttention;
  const tradingReady = isTradingSubtype(args.prefs.subtype);
  const paymentReadiness = visible
    ? pulseEvents
        .filter(
          (row) =>
            row.paymentRelevance !== 'NONE' &&
            (row.paymentStatus === 'PENDING' || row.paymentStatus === 'READY') &&
            row.approvalStatus !== 'REJECTED' &&
            row.approvalStatus !== 'PENDING'
        )
        .map((row) => {
          const remaining = readinessAmount(row, args.events);
          return remaining == null
            ? null
            : {
                id: row.operatingEventId,
                title: displayTitle(row),
                amount: money(remaining),
                relevance: row.paymentRelevance,
                status: row.paymentStatus,
                counterpartyName: row.counterpartyName,
                dueAt: row.dueAt ?? null,
                dueBucket: tradingReady ? tradingDueBucket(row.dueAt, asOf) : null,
                occurredAt: row.occurredAt ?? null,
                total: row.amount ?? null,
              };
        })
        .filter((row): row is NonNullable<typeof row> => row != null && num(row.amount) > 0)
        .sort((a, b) => {
          if (!tradingReady) return 0;
          const rank = { OVERDUE: 0, DUE_SOON: 1, OPEN: 2 } as const;
          return (rank[a.dueBucket ?? 'OPEN'] ?? 2) - (rank[b.dueBucket ?? 'OPEN'] ?? 2);
        })
    : [];
  const continuity = operatingContinuity(pulseEvents, args.familyCode, visible);
  const ownCampaign = args.prefs.subtype === 'OWN_BUSINESS_CAMPAIGN';
  const clientCampaign = args.prefs.subtype === 'CLIENT_CAMPAIGN';
  if (args.familyCode === 'MARKETING_CAMPAIGNS') {
    continuity.costPerLead = visible ? marketingCpl(pulseEvents) : null;
    if (ownCampaign) continuity.receivable = visible ? '0.0000' : null;
  }
  const visibleReadiness = ownCampaign
    ? paymentReadiness.filter((row) => row.relevance !== 'RECEIVABLE')
    : paymentReadiness;
  const finance = financeTotals(pulseEvents, visible, continuity);
  if (ownCampaign && finance) {
    finance.sales = '0.0000';
    finance.received = '0.0000';
    finance.outstanding = '0.0000';
  }
  if (args.familyCode === 'MARKETING_CAMPAIGNS' && finance) {
    finance.eligibleCost = money(spent);
  }
  const marketingInsight: MarketingInsight | null = args.familyCode === 'MARKETING_CAMPAIGNS'
    ? {
        cpl: visible ? marketingCpl(pulseEvents) : null,
        pacing: marketingPacing(args.prefs.startDate, args.prefs.endDate, asOf, progress, target, targetConfigured),
        coverage: marketingCoverage,
        reimbursable: money(
          pulseEvents
            .filter((row) => effective(row) && (row.eventType === 'SPEND' || row.eventType === 'VENDOR_COST') && row.marketing?.costBasis === 'REIMBURSABLE')
            .reduce((total, row) => total + num(row.amount), 0)
        ),
        clientContribution: clientCampaign && visible ? money(netPostedSales(pulseEvents) - spent) : null,
        campaignStatus: args.prefs.campaignStatus || 'OPEN',
        clientFee: args.prefs.clientFee,
        offering: args.prefs.offering,
        creativeApprovalRequired: args.prefs.creativeApprovalRequired,
        reportingCadence: args.prefs.reportingCadence,
      }
    : null;
  const quickAdds = (sub?.quickAdds ?? [])
    .map((code) => QA[code])
    .filter((def) => canSubmit(args.role, def))
    .map((def) => ({ code: def.code, label: def.label, needsAmount: def.needsAmount }));
  return {
    retired: false,
    familyCode: args.familyCode,
    subtype: args.prefs.subtype,
    subtypeLabel: sub?.label ?? args.prefs.subtype,
    title: args.prefs.identityName,
    partyName: args.prefs.partyName,
    role: args.role,
    financeVisible: visible,
    currency: args.prefs.currency,
    startDate: args.prefs.startDate,
    endDate: args.prefs.endDate,
    participants: args.prefs.participants.map((person) => person.displayName),
    masters: args.events
      .filter((row) => row.master && row.approvalStatus !== 'REJECTED')
      .map((row) => ({
        id: row.operatingEventId,
        kind: row.master!.kind,
        name: row.master!.name,
        unit: row.master!.unit ?? null,
        dimension: row.master!.dimension ?? null,
        conversionToBase: row.master!.conversionToBase ?? null,
        tracked: row.master!.tracked ?? null,
        barcode: row.master!.barcode ?? null,
        gstApplicable: row.master!.gstApplicable ?? null,
        gstClassification: row.master!.gstClassification ?? null,
        gstRate: row.master!.gstRate ?? null,
        priceIncludesGst: row.master!.priceIncludesGst ?? null,
        salePriceIncludesGst: row.master!.salePriceIncludesGst ?? null,
        purchasePriceIncludesGst: row.master!.purchasePriceIncludesGst ?? null,
        gstEffectiveFrom: row.master!.gstEffectiveFrom ?? null,
        hsn: row.master!.hsn ?? null,
        gstRegistration: row.master!.gstRegistration ?? null,
        businessState: row.master!.businessState ?? null,
        billingState: row.master!.billingState ?? null,
        deliveryState: row.master!.deliveryState ?? null,
        gstin: row.master!.gstin ?? null,
      })),
    location: args.prefs.location,
    finance,
    continuity,
    budget: visible
      ? { amount: money(budget), spent: money(spent), remaining: money(budget - spent), configured: budgetConfigured }
      : null,
    target: visible
      ? {
          metric: args.prefs.targetMetric,
          value: money(target),
          progress: money(progress),
          configured: targetConfigured,
          coverage: args.familyCode === 'MARKETING_CAMPAIGNS' ? marketingCoverage : manufacturingCoverage,
        }
      : null,
    attention: [
      ...attention,
      ...(isRetailSubtype(args.prefs.subtype) || isTradingSubtype(args.prefs.subtype) || isMarketingSubtype(args.prefs.subtype)
        ? []
        : paymentReadiness.map((row) => ({
            id: `open-${row.id}`,
            title: `${row.relevance === 'RECEIVABLE' ? 'To collect' : 'To pay'} · ${row.counterpartyName ? `${row.counterpartyName} · ` : ''}${row.title}`,
            kind: row.relevance === 'RECEIVABLE' ? 'RECEIVABLE' : 'PAYABLE',
          }))),
    ],
    paymentReadiness: visibleReadiness,
    activity: pulseEvents.map((row) => ({
      id: row.operatingEventId,
      title: displayTitle(row),
      eventType: row.eventType,
      occurredAt: row.occurredAt,
      amount: visible ? row.amount : null,
      channelName: row.channelName,
      counterpartyName: row.counterpartyName,
      paymentStatus: row.paymentStatus,
      approvalStatus: row.approvalStatus,
      note: row.note,
      quantity: row.quantity ?? null,
      unit: row.unit ?? row.lines?.[0]?.unit ?? null,
      parentEventId: row.parentEventId ?? null,
      status: row.contract?.appointmentStatus ?? row.contract?.orderStatus ?? null,
      orderSide: row.contract?.orderSide ?? null,
      estimatedAmount: row.eventType === 'ORDER' ? row.contract?.estimatedAmount ?? null : null,
    })),
    life: {
      channels: args.prefs.channels,
      openCommitments: pulseEvents.filter(
        (row) =>
          (row.eventType === 'ORDER' || row.eventType === 'CUSTOMER_ORDER' || row.eventType === 'DELIVERABLE' || row.eventType === 'ACTIVITY') &&
          row.approvalStatus !== 'REJECTED'
      ).length,
      resultProgress: visible ? money(progress) : '',
    },
    memory: { revisions },
    boards: operatingBoards({
      familyCode: args.familyCode,
      subtype: args.prefs.subtype,
      prefs: args.prefs,
      events: args.events,
      pulseEvents,
      visible,
      revisions,
      asOf,
    }),
    quickAdds,
    gstTracking: args.prefs.gstTracking,
    gstRegistration: args.prefs.gstRegistration,
    gstState: args.prefs.gstState,
    gstEffectiveFrom: args.prefs.gstEffectiveFrom,
    salePriceIncludesGst: args.prefs.salePriceIncludesGst,
    purchasePriceIncludesGst: args.prefs.purchasePriceIncludesGst,
    gstPosition:
      visible && args.prefs.gstTracking === 'YES'
        ? accumulateGstPosition(
            pulseEvents.map((row) => ({
              id: row.operatingEventId,
              title: row.title,
              eventType: row.eventType,
              occurredAt: row.occurredAt,
              approvalStatus: row.approvalStatus,
              paymentStatus: row.paymentStatus,
              gst: row.contract?.gst,
              returnDirection: row.contract?.returnDirection,
            })),
            asOf.toISOString().slice(0, 7)
          )
        : null,
    marketing: marketingInsight,
  };
}

function marketingPacing(
  startDate: string,
  endDate: string,
  asOf: Date,
  progress: number,
  target: number,
  targetConfigured: boolean
): string | null {
  const start = startDate.slice(0, 10);
  const end = endDate.slice(0, 10);
  if (!targetConfigured || !/^\d{4}-\d{2}-\d{2}$/.test(start) || !/^\d{4}-\d{2}-\d{2}$/.test(end) || end <= start) return null;
  const span = Math.round((Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86400000);
  if (!(span > 0)) return null;
  const today = asOf.toISOString().slice(0, 10);
  const elapsed = Math.max(0, Math.min(span, Math.round((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86400000)));
  const timePct = Math.round((elapsed / span) * 100);
  const resultPct = target > 0 ? Math.round((progress / target) * 100) : 0;
  return `Linear benchmark · ${timePct}% of time · ${resultPct}% of target`;
}

function emptyFacts(): Pick<OperatingBoardRow, 'channelName' | 'counterpartyName' | 'paymentStatus' | 'approvalStatus' | 'note'> {
  return {
    channelName: null,
    counterpartyName: null,
    paymentStatus: null,
    approvalStatus: null,
    note: null,
  };
}

/** Posted SALE, SERVICE, and INVOICE minus linked customer credit notes. */
function recognizedValue(row: OperatingEventRow): number {
  const taxable = row.contract?.gst?.taxableAmount;
  if (taxable != null && taxable !== '') return num(taxable);
  return num(row.amount);
}

function commercialAmount(row: OperatingEventRow): number {
  if (row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'INVOICE' || row.eventType === 'PURCHASE' || row.eventType === 'RETURN') {
    return recognizedValue(row);
  }
  return num(row.amount);
}

function netPostedSales(events: OperatingEventRow[]): number {
  const rows = events.filter(effective);
  const posted = rows.filter((row) => row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'INVOICE');
  const grossSales = posted.reduce((total, row) => total + recognizedValue(row), 0);
  const saleIds = new Set(posted.map((row) => row.operatingEventId));
  const returned = rows
    .filter(
      (row) =>
        row.eventType === 'RETURN' &&
        row.parentEventId != null &&
        saleIds.has(row.parentEventId) &&
        (row.contract?.returnDirection == null || row.contract.returnDirection === 'CUSTOMER')
    )
    .reduce((total, row) => total + recognizedValue(row), 0);
  return Math.max(0, grossSales - returned);
}

function financeTotals(
  events: OperatingEventRow[],
  visible: boolean,
  continuity: OperatingContinuity
): OperatingFinanceTotals | null {
  if (!visible) return null;
  const rows = events.filter(effective);
  return {
    sales: money(netPostedSales(events)),
    received: money(
      rows.reduce((total, row) => {
        if (row.eventType === 'COLLECTION' && row.paymentStatus === 'COLLECTED' && row.contract?.advanceApplied !== true) return total + num(row.amount);
        if ((row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'INVOICE') && row.paymentStatus === 'COLLECTED') {
          const linked = events.some(
            (child) => child.parentEventId === row.operatingEventId && child.eventType === 'COLLECTION' && effective(child)
          );
          if (!linked) return total + num(row.amount);
        }
        return total;
      }, 0)
    ),
    outstanding: continuity.receivable ?? money(0),
    payable: continuity.payable ?? money(0),
    eligibleCost: money(rows.filter((row) => quickAddDef(row.eventType)?.consumesBudget === true).reduce((total, row) => total + recognizedValue(row), 0)),
    cashPaid: money(
      rows.reduce((total, row) => {
        if (row.eventType === 'PAYMENT' && row.paymentStatus === 'PAID') return total + num(row.amount);
        if (PAYABLE_TYPES.has(row.eventType) && row.paymentStatus === 'PAID') {
          const linked = events.some(
            (child) => child.parentEventId === row.operatingEventId && child.eventType === 'PAYMENT' && effective(child)
          );
          if (!linked) return total + num(row.amount);
        }
        return total;
      }, 0)
    ),
  };
}

function displayTitle(row: OperatingEventRow): string {
  const lines = row.lines ?? [];
  if (lines.length > 0) {
    return lines
      .map((line) => {
        const qty = line.quantity ? ` × ${trimNumber(line.quantity)}` : '';
        const unit = line.unit ? ` ${line.unit}` : '';
        const price = line.unitPrice ? ` @ ${trimNumber(line.unitPrice)}` : '';
        return `${line.itemName}${qty}${unit}${price}`;
      })
      .join(', ');
  }
  if (row.quantity) {
    const unit = row.unit ? ` ${row.unit}` : '';
    return `${row.title} · ${trimNumber(row.quantity)}${unit}`;
  }
  return row.title;
}

function trimNumber(value: string): string {
  const n = Number(value);
  if (!Number.isFinite(n)) return value;
  return n.toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

function boardRow(row: OperatingEventRow, visible: boolean): OperatingBoardRow {
  return {
    id: row.operatingEventId,
    title: displayTitle(row),
    eventType: row.eventType,
    occurredAt: row.occurredAt,
    amount: visible ? row.amount : null,
    channelName: row.channelName,
    counterpartyName: row.counterpartyName,
    paymentStatus: row.paymentStatus,
    approvalStatus: row.approvalStatus,
    note: row.note,
  };
}

function eventSection(
  code: string,
  title: string,
  events: OperatingEventRow[],
  types: string[],
  visible: boolean,
  currency: string
): OperatingBoardSection {
  const matched = events.filter(
    (row) => types.includes(row.eventType) && row.approvalStatus !== 'REJECTED' && !row.master
  );
  const rows = matched.map((row) => boardRow(row, visible));
  let detail = 'Nothing recorded yet.';
  if (rows.length > 0) {
    detail = `${rows.length} recorded`;
    if (visible) {
      const total = matched.reduce((sum, row) => sum + commercialAmount(row), 0);
      if (total > 0) detail = `${detail} · ${money(total)} ${currency}`;
    }
  }
  return { code, title, detail, rows };
}

function textSection(code: string, title: string, lines: string[]): OperatingBoardSection {
  const rows = lines.filter((line) => line.trim().length > 0).map((line, index) => ({
    id: `${code}-${index}`,
    title: line,
    eventType: code,
    occurredAt: '',
    amount: null,
    ...emptyFacts(),
  }));
  return {
    code,
    title,
    detail: rows.length > 0 ? `${rows.length} recorded` : 'Nothing recorded yet.',
    rows,
  };
}

const MONEY_MOVING_TYPES = ['SALE', 'SERVICE', 'PAYMENT', 'COLLECTION', 'SPEND', 'VENDOR_COST', 'PURCHASE'];
const MONEY_HISTORY_TYPES = ['SALE', 'SERVICE', 'PURCHASE', 'BUDGET', 'TARGET', 'SPEND', 'PAYMENT', 'COLLECTION', 'VENDOR_COST'];

type WidgetLabels = {
  m1: string;
  m3: string;
  l1: string;
  l2: string;
  l4: string;
  me3: string;
};

function widgetLabels(familyCode: string): WidgetLabels {
  if (familyCode === 'MARKETING_CAMPAIGNS') {
    return {
      m1: 'Campaign Moment',
      m3: 'Network & Channels',
      l1: 'Current Campaign',
      l2: 'Channels',
      l4: 'Execution',
      me3: 'People & Activity History',
    };
  }
  if (familyCode === 'TRADING_DISTRIBUTION') {
    return {
      m1: 'Business Moment',
      m3: 'Trade Network',
      l1: 'Current Business',
      l2: 'Buyers & Channels',
      l4: 'People & Activity',
      me3: 'People & Patterns',
    };
  }
  if (familyCode === 'MANUFACTURING_PRODUCTION') {
    return {
      m1: 'My Manufacturing Business',
      m3: 'Business Network',
      l1: 'Current Production / Business',
      l2: 'Orders / Buyers',
      l4: 'Production Operations',
      me3: 'People & Activity',
    };
  }
  return {
    m1: 'Business Moment',
    m3: 'Business Network',
    l1: 'Current Business',
    l2: 'Channel Activity',
    l4: 'Operations',
    me3: 'People & Activity',
  };
}

function commitmentTypes(familyCode: string): string[] {
  if (familyCode === 'MARKETING_CAMPAIGNS') return ['ACTIVITY'];
  if (familyCode === 'MANUFACTURING_PRODUCTION') return ['ORDER', 'CUSTOMER_ORDER', 'SHIPMENT'];
  if (familyCode === 'TRADING_DISTRIBUTION') return ['ORDER', 'SHIPMENT'];
  return ['ORDER'];
}

function executionTypes(familyCode: string): string[] {
  if (familyCode === 'MARKETING_CAMPAIGNS') return ['ACTIVITY', 'PROGRESS', 'RESULT', 'UPDATE', 'ISSUE'];
  if (familyCode === 'MANUFACTURING_PRODUCTION') return ['ISSUE', 'STOCK', 'UPDATE', 'PRODUCTION', 'DELIVERY'];
  if (familyCode === 'RETAIL_CONSUMER') return ['ISSUE', 'STOCK', 'UPDATE', 'SERVICE', 'APPOINTMENT'];
  return ['ISSUE', 'STOCK', 'UPDATE'];
}

function eventsInRange(events: OperatingEventRow[], prefs: OperatingPreferences): OperatingEventRow[] {
  const end = prefs.endDate.slice(0, 10);
  if (!end) return events;
  const start = (prefs.startDate || '0000-01-01').slice(0, 10);
  return events.filter((row) => {
    const day = row.occurredAt.slice(0, 10);
    return day >= start && day <= end;
  });
}

function operatingBoards(args: {
  familyCode: string;
  subtype: string;
  prefs: OperatingPreferences;
  events: OperatingEventRow[];
  pulseEvents: OperatingEventRow[];
  visible: boolean;
  revisions: Array<{ id: string; title: string; occurredAt: string }>;
  asOf: Date;
}): OperatingProjection['boards'] {
  const { familyCode, prefs, events, pulseEvents, visible } = args;
  const labels = widgetLabels(familyCode);
  const currency = prefs.currency;
  const section = (code: string, title: string, types: string[]) =>
    eventSection(code, title, events, types, visible, currency);
  const names = prefs.participants.map((person) => person.displayName);
  const network = [...prefs.counterparties.map((party) => party.name), ...prefs.channels];
  const moments = [
    textSection('M1', labels.m1, [prefs.identityName, prefs.partyName, prefs.description]),
    textSection('M2', 'People', names),
    textSection('M3', labels.m3, network),
    section('M4', 'Open Commitments', commitmentTypes(familyCode)),
    ...familyMomentSections(familyCode, args.subtype, events, visible, currency, network),
  ];
  const lifeEvents = pulseEvents;
  const lifeSection = (code: string, title: string, types: string[]) =>
    eventSection(code, title, lifeEvents, types, visible, currency);
  const life = [
    textSection('L1', labels.l1, [prefs.identityName, prefs.partyName].filter(Boolean)),
    familyCode === 'MANUFACTURING_PRODUCTION'
      ? lifeSection('L2', labels.l2, ['CUSTOMER_ORDER', 'ORDER'])
      : channelSection(labels.l2, prefs.channels, lifeEvents, visible, currency, familyCode),
    lifeSection('L3', 'Money Moving', MONEY_MOVING_TYPES),
    lifeSection('L4', labels.l4, executionTypes(familyCode)),
    peopleToday(lifeEvents, prefs),
    ...familyLifeSections(familyCode, args.subtype, lifeEvents, visible, currency),
  ];
  const monthly = monthlyHistory(
    pulseEvents,
    visible,
    currency,
    familyCode === 'MARKETING_CAMPAIGNS'
      ? { budget: num(prefs.monthlyBudget), target: num(prefs.monthlyTarget) }
      : undefined
  );
  const revisionRows = args.revisions.map((row) => ({
    id: row.id,
    title: row.title,
    eventType: 'REVISION',
    occurredAt: row.occurredAt,
    amount: null,
    ...emptyFacts(),
  }));
  const historyRows = [...monthly, ...revisionRows];
  const memory = [
    {
      code: 'ME1',
      title: 'Monthly History',
      detail: historyRows.length > 0 ? `${historyRows.length} recorded` : 'Nothing recorded yet.',
      rows: historyRows,
    },
    eventSection('ME2', 'Money History', pulseEvents, MONEY_HISTORY_TYPES, visible, currency),
    peopleHistory(labels.me3, pulseEvents, prefs, visible),
    eventSection('ME4', 'Patterns & Decisions', pulseEvents, ['ISSUE', 'APPROVAL'], visible, currency),
    settlementChains(pulseEvents, visible),
    ...familyMemorySections(familyCode, pulseEvents, visible, currency),
  ];
  if (isRetailSubtype(args.subtype)) decorateRetailBoards(moments, life, memory, args.events, visible, currency, args.asOf);
  if (isTradingSubtype(args.subtype)) decorateTradingBoards(moments, life, memory, args.events, visible, currency, args.asOf, network);
  if (isMarketingSubtype(args.subtype)) decorateMarketingBoards(moments, life, memory, args.prefs, args.events);
  if (isManufacturingSubtype(args.subtype)) decorateManufacturingBoards(moments, args.events);
  return { moments, life, memory };
}

function decorateManufacturingBoards(moments: OperatingBoardSection[], events: OperatingEventRow[]): void {
  const stock = moments.find((section) => section.code === 'stock');
  const positions = manufacturingStock(events.map(toManufacturing));
  if (stock && positions.length > 0) {
    stock.detail = positions
      .map((position) => {
        const unit = position.unit ? ` ${position.unit}` : '';
        return `${position.item} · ${Number(position.onHand.toFixed(2))}${unit} ${position.kind === 'FINISHED' ? 'finished' : 'on hand'}`;
      })
      .join(' · ');
  }
  const production = moments.find((section) => section.code === 'production');
  if (production) {
    const rows = events.filter((row) => row.eventType === 'PRODUCTION' && effective(row));
    const accepted = rows.reduce((total, row) => total + (explicitAccepted(toManufacturing(row)) ?? 0), 0);
    const recorded = rows.reduce((total, row) => total + recordedFrom(row), 0);
    const acceptedKnown = rows.some((row) => explicitAccepted(toManufacturing(row)) != null);
    production.coverage = acceptedKnown ? null : 'Acceptance not recorded';
    production.detail = rows.length
      ? `Recorded output ${Number(recorded.toFixed(2))}${acceptedKnown ? ` · accepted ${Number(accepted.toFixed(2))}` : ''}`
      : production.detail;
  }
}

function recordedFrom(row: OperatingEventRow): number {
  if (row.quantity != null && row.quantity !== '') return num(row.quantity);
  return num(row.amount);
}

function decorateMarketingBoards(
  moments: OperatingBoardSection[],
  life: OperatingBoardSection[],
  memory: OperatingBoardSection[],
  prefs: OperatingPreferences,
  events: OperatingEventRow[]
): void {
  const measured = marketingProgress(events, prefs.targetMetric);
  const current = life.find((section) => section.code === 'L1');
  if (current) {
    current.coverage = measured.coverage;
    current.detail = measured.coverage === 'N/A'
      ? 'No compatible results yet.'
      : `${prefs.targetMetric} · ${measured.coverage}`;
  }
  const network = moments.find((section) => section.code === 'M3');
  if (network) network.coverage = channelCoverage(prefs.channels, events);
  const commitments = moments.find((section) => section.code === 'M4');
  if (commitments) {
    const open = events.filter(
      (row) => row.eventType === 'ACTIVITY' && row.approvalStatus !== 'REJECTED' && !activityClosed(row.marketing?.activityStatus)
    );
    if (open.length === 0 && commitments.rows.length === 0) commitments.detail = 'Nothing recorded yet.';
  }
  if (prefs.subtype === 'OWN_BUSINESS_CAMPAIGN') {
    const hide = new Set(['SALE', 'INVOICE', 'COLLECTION']);
    for (const section of [...life, ...memory]) {
      if (section.code !== 'L3' && section.code !== 'ME2') continue;
      section.rows = section.rows.filter((row) => !hide.has(row.eventType));
    }
  }
}

function decorateTradingBoards(
  moments: OperatingBoardSection[],
  life: OperatingBoardSection[],
  memory: OperatingBoardSection[],
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  asOf: Date,
  network: string[]
): void {
  const orders = moments.find((section) => section.code === 'orders');
  if (orders) {
    orders.detail = ordersCommitmentsSummary(events, visible, currency);
    orders.coverage = ordersCoverage(events);
  }
  const stock = moments.find((section) => section.code === 'stock');
  const positions = tradingStockPositions(events);
  const consignHint = consignmentStockHint(events);
  const skuCoverage = stockCoverage(events);
  if (stock) {
    if (positions.length > 0) {
      const owned = positions
        .map((position) => {
          const unit = position.unit ? ` ${position.unit}` : '';
          const low = position.reorderPoint != null && position.onHand <= position.reorderPoint ? ' · low' : '';
          return `${position.item} · ${Number(position.onHand.toFixed(2))}${unit} on hand${low}`;
        })
        .join(' · ');
      stock.detail = consignHint ? `${owned} · ${consignHint}` : owned;
    } else if (consignHint) {
      stock.detail = consignHint;
    }
    if (skuCoverage) stock.coverage = skuCoverage;
  }
  const networkBoard = moments.find((section) => section.code === 'network');
  if (networkBoard) networkBoard.detail = peopleNetworkDetail(events, network);
  const onhand = life.find((section) => section.code === 'onhand');
  if (onhand && positions.length > 0) {
    onhand.detail = positions
      .map((position) => {
        const unit = position.unit ? ` ${position.unit}` : '';
        return `${position.item} · ${Number(position.onHand.toFixed(2))}${unit}`;
      })
      .join(' · ');
  }
  const current = life.find((section) => section.code === 'L1');
  const liveDetail = currentBusinessDetail(events, visible, currency);
  if (current && liveDetail) current.detail = liveDetail;
  const buyers = life.find((section) => section.code === 'L2');
  if (buyers) {
    const channelCov = buyersChannelsCoverage(events);
    if (channelCov) buyers.coverage = channelCov;
  }
  const moving = life.find((section) => section.code === 'L3');
  const movingDetail = tradingMoneyMoving(events, visible, currency, asOf);
  if (moving) {
    // Cash-true rows only — sales/purchases are obligations, not money movement.
    moving.rows = events
      .filter(
        (row) =>
          effective(row) &&
          ((row.eventType === 'COLLECTION' && row.paymentStatus === 'COLLECTED') ||
            (row.eventType === 'PAYMENT' && row.paymentStatus === 'PAID') ||
            ((row.eventType === 'SALE' || row.eventType === 'PURCHASE' || row.eventType === 'SPEND') &&
              (row.paymentStatus === 'COLLECTED' || row.paymentStatus === 'PAID') &&
              !events.some(
                (child) =>
                  child.parentEventId === row.operatingEventId &&
                  (child.eventType === 'COLLECTION' || child.eventType === 'PAYMENT') &&
                  effective(child)
              )))
      )
      .map((row) => boardRow(row, visible));
    if (movingDetail) moving.detail = movingDetail;
  }
  const fulfilment = memory.find((section) => section.code === 'fulfilment');
  if (fulfilment) {
    const coverage = fulfilmentCoverage(events);
    if (coverage) fulfilment.coverage = coverage;
  }
  const ordersBoard = moments.find((section) => section.code === 'orders');
  if (ordersBoard && ordersBoard.coverage == null) {
    const cov = ordersCoverage(events);
    if (cov) ordersBoard.coverage = cov;
  }
  // Primary Open Commitments surface is M4 — clear the duplicate family board.
  const duplicate = moments.find((section) => section.code === 'commitments');
  if (duplicate) {
    duplicate.detail = 'See Open Commitments above.';
    duplicate.rows = [];
  }
  const commitments = moments.find((section) => section.code === 'M4');
  if (commitments) {
    const rows = events.flatMap((row) => {
      if (!effective(row)) return [];
      if (row.eventType === 'SALE') {
        const open = tradingObligationRemaining(row, events, 'COLLECTION');
        if (!(open > 0)) return [];
        return [{ ...boardRow(row, visible), title: `To collect · ${row.counterpartyName || row.title}`, amount: visible ? money(open) : null }];
      }
      if (PAYABLE_TYPES.has(row.eventType)) {
        const open = tradingObligationRemaining(row, events, 'PAYMENT');
        if (!(open > 0)) return [];
        return [{ ...boardRow(row, visible), title: `To pay · ${row.counterpartyName || row.title}`, amount: visible ? money(open) : null }];
      }
      if (row.eventType === 'ORDER' && row.contract?.orderStatus !== 'COMPLETED' && row.contract?.orderStatus !== 'CANCELLED') {
        return [{ ...boardRow(row, visible), title: `Open order · ${displayTitle(row)}` }];
      }
      if (
        (row.eventType === 'SHIPMENT' || row.eventType === 'DELIVERY') &&
        row.contract?.shipmentStatus !== 'DELIVERED' &&
        row.contract?.shipmentStatus !== 'CANCELLED'
      ) {
        return [{ ...boardRow(row, visible), title: `In transit · ${displayTitle(row)}` }];
      }
      return [];
    });
    commitments.detail = rows.length > 0 ? `${rows.length} open` : 'Nothing recorded yet.';
    commitments.rows = rows;
  }
}

function decorateRetailBoards(
  moments: OperatingBoardSection[],
  life: OperatingBoardSection[],
  memory: OperatingBoardSection[],
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  asOf: Date
): void {
  const sales = moments.find((section) => section.code === 'sales');
  if (sales && (events.some((row) => row.eventType === 'SALE' || row.eventType === 'SERVICE' || row.eventType === 'ORDER'))) {
    sales.detail = postedSalesSummary(events, visible, currency);
    sales.coverage = salesCoverage(events);
  }
  const stock = moments.find((section) => section.code === 'stock');
  const positions = stockPositions(events);
  if (stock && positions.length > 0) {
    stock.detail = positions
      .map((position) => {
        const unit = position.unit ? ` ${position.unit}` : '';
        const low = position.reorderPoint != null && position.onHand <= position.reorderPoint ? ' · low' : '';
        return `${position.item} · ${Number(position.onHand.toFixed(2))}${unit} on hand${low}`;
      })
      .join(' · ');
  }
  const appointments = moments.find((section) => section.code === 'appointments');
  const appointmentDetail = appointmentSummary(events);
  if (appointments && appointmentDetail) appointments.detail = appointmentDetail;
  const customers = moments.find((section) => section.code === 'customers');
  if (customers) {
    const channelNames = events
      .map((row) => row.channelName)
      .filter((name): name is string => Boolean(name && name.trim()));
    customers.detail = customersNetworkDetail(events, [...new Set(channelNames)]);
  }
  const services = moments.find((section) => section.code === 'services');
  if (services) {
    const charges = events.filter((row) => (row.eventType === 'SERVICE' || row.eventType === 'SALE') && effective(row));
    const pending = events.filter(
      (row) =>
        row.eventType === 'APPOINTMENT' &&
        effective(row) &&
        row.contract?.appointmentStatus === 'COMPLETED' &&
        !charges.some((charge) => charge.parentEventId === row.operatingEventId)
    ).length;
    const total = charges.reduce((sum, row) => sum + num(row.amount), 0);
    services.detail = visible
      ? `Finalized ${charges.length} · ${money(total)} ${currency} · Charge pending ${pending}`
      : `Finalized ${charges.length} · Charge pending ${pending}`;
    services.coverage = salesCoverage(events);
  }
  const moving = life.find((section) => section.code === 'L3');
  const movingDetail = moneyMovingDetail(events, visible, currency, asOf);
  if (moving && movingDetail) moving.detail = movingDetail;
  const flow = life.find((section) => section.code === 'service-flow');
  if (flow) flow.detail = serviceFlowSummary(events);
  const patterns = memory.find((section) => section.code === 'ME4');
  const outcomes = outcomeCoverage(events, asOf);
  if (patterns && outcomes) {
    patterns.coverage = outcomes;
    patterns.detail = outcomes;
  }
  const commitments = moments.find((section) => section.code === 'M4');
  if (commitments) {
    const rows = events.flatMap((row) => {
      if (!effective(row)) return [];
      if (row.eventType === 'SALE' || row.eventType === 'SERVICE') {
        const open = obligationRemaining(row, events, new Set(['COLLECTION']));
        if (!(open > 0)) return [];
        return [{ ...boardRow(row, visible), title: `To collect · ${row.counterpartyName || row.title}`, amount: visible ? money(open) : null }];
      }
      if (PAYABLE_TYPES.has(row.eventType)) {
        const open = obligationRemaining(row, events, new Set(['PAYMENT']));
        if (!(open > 0)) return [];
        return [{ ...boardRow(row, visible), title: `To pay · ${row.counterpartyName || row.title}`, amount: visible ? money(open) : null }];
      }
      if (row.eventType === 'ORDER' && row.contract?.orderStatus !== 'COMPLETED' && row.contract?.orderStatus !== 'CANCELLED') {
        return [{ ...boardRow(row, visible), title: `Open order · ${displayTitle(row)}` }];
      }
      if (row.eventType === 'APPOINTMENT' && (row.contract?.appointmentStatus === 'BOOKED' || row.contract?.appointmentStatus === 'IN_PROGRESS')) {
        return [{ ...boardRow(row, visible), title: `Open booking · ${row.title}` }];
      }
      return [];
    });
    commitments.detail = rows.length > 0 ? `${rows.length} open` : 'Nothing recorded yet.';
    commitments.rows = rows;
  }
}

function familyMomentSections(
  familyCode: string,
  subtype: string,
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  network: string[]
): OperatingBoardSection[] {
  const section = (code: string, title: string, types: string[]) =>
    eventSection(code, title, events, types, visible, currency);
  if (familyCode === 'RETAIL_CONSUMER' && subtype === 'CONSUMER_SERVICE') {
    return [
      section('services', 'Services', ['SERVICE', 'SALE']),
      section('appointments', 'Appointments / Jobs', ['APPOINTMENT']),
      section('sales', 'Sales', ['SALE']),
      section('updates', 'Updates', ['UPDATE']),
      section('issues', 'Issues', ['ISSUE']),
      section('expenses', 'Expenses', ['SPEND', 'PURCHASE']),
      section('payments', 'Payments & Collections', ['PAYMENT', 'COLLECTION']),
      textSection('customers', 'Customers & Network', network),
    ];
  }
  if (familyCode === 'RETAIL_CONSUMER') {
    return [
      section('sales', 'Sales & Orders', ['SALE', 'ORDER']),
      section('purchases', 'Purchases', ['PURCHASE']),
      section('updates', 'Updates', ['UPDATE']),
      section('stock', 'Stock & Supplies', ['STOCK']),
      section('issues', 'Issues', ['ISSUE']),
      section('expenses', 'Expenses', ['SPEND']),
      section('payments', 'Payments & Collections', ['PAYMENT', 'COLLECTION']),
      textSection('channels', 'People & Network', network),
    ];
  }
  if (familyCode === 'MANUFACTURING_PRODUCTION') {
    return [
      section('orders', 'Orders', ['CUSTOMER_ORDER', 'ORDER']),
      section('production', 'Production', ['PRODUCTION']),
      section('stock', 'Stock', ['STOCK', 'PURCHASE']),
      section('commitments', 'Open Commitments', ['CUSTOMER_ORDER', 'PURCHASE', 'SALE']),
    ];
  }
  if (familyCode === 'TRADING_DISTRIBUTION') {
    return [
      section('orders', 'Orders & Commitments', ['ORDER', 'CUSTOMER_ORDER', 'SHIPMENT']),
      section('stock', 'Stock & Products', ['STOCK', 'PURCHASE', 'SALE']),
      textSection('network', 'People & Network', network),
      section('commitments', 'Open Commitments', ['ORDER', 'PURCHASE', 'SALE', 'SHIPMENT']),
    ];
  }
  if (familyCode === 'MARKETING_CAMPAIGNS') {
    const sections = [
      section('results', 'Results', ['RESULT', 'PROGRESS']),
      section('spend', 'Spend', ['SPEND', 'VENDOR_COST']),
      section('activity', 'Activity', ['ACTIVITY', 'UPDATE', 'ISSUE']),
    ];
    if (subtype === 'CLIENT_CAMPAIGN') sections.push(section('invoices', 'Client Invoices', ['INVOICE', 'SALE']));
    return sections;
  }
  return [];
}

function familyLifeSections(
  familyCode: string,
  subtype: string,
  events: OperatingEventRow[],
  visible: boolean,
  currency: string
): OperatingBoardSection[] {
  if (familyCode === 'TRADING_DISTRIBUTION') {
    return [
      eventSection('transit', 'In Transit', events, ['SHIPMENT'], visible, currency),
      eventSection('onhand', 'On Hand', events, ['STOCK'], visible, currency),
    ];
  }
  if (subtype === 'CONSUMER_SERVICE') {
    const today = new Date().toISOString().slice(0, 10);
    const rows = events.filter(
      (row) => row.eventType === 'APPOINTMENT' && row.occurredAt.slice(0, 10) === today && row.approvalStatus !== 'REJECTED'
    );
    return [
      {
        code: 'schedule',
        title: "Today's Schedule",
        detail: rows.length > 0 ? `${rows.length} recorded` : 'Nothing recorded yet.',
        rows: rows.map((row) => boardRow(row, visible)),
      },
      {
        code: 'service-flow',
        title: 'Service Flow',
        detail: serviceFlowSummary(events),
        rows: [],
      },
    ];
  }
  return [];
}

function sortRetailAttention<T extends { severity?: string | null; kind: string }>(cards: T[]): T[] {
  const severityRank: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
  const kindRank = (kind: string): number => {
    if (kind.startsWith('OVERDUE')) return 0;
    if (kind.startsWith('DUE')) return 1;
    if (kind === 'CONFLICT' || kind === 'CHARGE_PENDING' || kind === 'BOOKING_OVERDUE' || kind === 'STUCK') return 2;
    if (kind === 'LOW_STOCK' || kind === 'UNASSIGNED' || kind === 'BUDGET_WARNING' || kind === 'BUDGET_OVERRUN') return 3;
    if (kind === 'APPROVAL') return 4;
    return 5;
  };
  return [...cards].sort((a, b) => {
    const sev = (severityRank[a.severity ?? ''] ?? 9) - (severityRank[b.severity ?? ''] ?? 9);
    if (sev !== 0) return sev;
    return kindRank(a.kind) - kindRank(b.kind);
  });
}

function settlementChains(events: OperatingEventRow[], visible: boolean): OperatingBoardSection {
  const rows = events
    .filter((row) => (row.eventType === 'COLLECTION' || row.eventType === 'PAYMENT') && row.approvalStatus !== 'REJECTED')
    .map((row) => {
      const parent = events.find((item) => item.operatingEventId === row.parentEventId);
      const verb = row.eventType === 'COLLECTION' ? 'Collected against' : 'Paid against';
      const base = boardRow(row, visible);
      return {
        ...base,
        title: parent ? `${verb} ${displayTitle(parent)}` : displayTitle(row),
      };
    });
  return {
    code: 'chains',
    title: 'Settlement History',
    detail: rows.length > 0 ? `${rows.length} recorded` : 'Nothing recorded yet.',
    rows,
  };
}

function familyMemorySections(
  familyCode: string,
  events: OperatingEventRow[],
  visible: boolean,
  currency: string
): OperatingBoardSection[] {
  if (familyCode !== 'TRADING_DISTRIBUTION' && familyCode !== 'MANUFACTURING_PRODUCTION') return [];
  return [eventSection('fulfilment', 'Fulfilment History', events, ['ORDER', 'SHIPMENT', 'DELIVERY', 'PRODUCTION'], visible, currency)];
}

function channelSection(
  title: string,
  channels: string[],
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  familyCode: string
): OperatingBoardSection {
  const sales = events.filter((row) => {
    if (!effective(row)) return false;
    if (familyCode === 'MARKETING_CAMPAIGNS') return row.eventType === 'RESULT' || row.eventType === 'PROGRESS';
    return row.eventType === 'SALE' || row.eventType === 'SERVICE';
  });
  const known = new Set(channels);
  for (const row of sales) if (row.channelName) known.add(row.channelName);
  const lines = [...known].map((channel) => {
    const matched = sales.filter((row) => row.channelName === channel);
    if (matched.length === 0) return channel;
    if (familyCode === 'MARKETING_CAMPAIGNS') {
      const metrics = [...new Set(matched.map((row) => row.metric || 'RESULT'))].join(', ');
      return `${channel} · ${metrics} · ${matched.length}`;
    }
    const total = matched.reduce((sum, row) => sum + commercialAmount(row), 0);
    return visible && total > 0 ? `${channel} · ${matched.length} · ${money(total)} ${currency}` : `${channel} · ${matched.length}`;
  });
  return textSection('L2', title, lines);
}

function peopleToday(events: OperatingEventRow[], prefs: OperatingPreferences): OperatingBoardSection {
  const today = new Date().toISOString().slice(0, 10);
  const lines = events
    .filter((row) => row.occurredAt.slice(0, 10) === today && row.approvalStatus !== 'REJECTED')
    .map((row) => `${actorName(row, prefs)} · ${displayTitle(row)}`);
  return textSection('L5', 'People Today', lines);
}

function peopleHistory(
  title: string,
  events: OperatingEventRow[],
  prefs: OperatingPreferences,
  visible: boolean
): OperatingBoardSection {
  const rows = events
    .filter((row) => row.approvalStatus !== 'REJECTED')
    .slice(0, 30)
    .map((row) => ({
      ...boardRow(row, visible),
      title: `${actorName(row, prefs)} · ${displayTitle(row)}`,
    }));
  return {
    code: 'ME3',
    title,
    detail: rows.length > 0 ? `${rows.length} recorded` : 'Nothing recorded yet.',
    rows,
  };
}

function actorName(row: OperatingEventRow, prefs: OperatingPreferences): string {
  const match = prefs.participants.find((person) => person.userId && person.userId === row.submittedByUserId);
  return match?.displayName || row.counterpartyName || 'Recorded';
}

function monthlyHistory(
  events: OperatingEventRow[],
  visible: boolean,
  currency: string,
  marketingPlan?: { budget: number; target: number }
): OperatingBoardRow[] {
  const groups = new Map<string, OperatingEventRow[]>();
  for (const row of events) {
    if (!effective(row)) continue;
    const month = row.occurredAt.slice(0, 7);
    if (!/^\d{4}-\d{2}$/.test(month)) continue;
    const list = groups.get(month) ?? [];
    list.push(row);
    groups.set(month, list);
  }
  const current = new Date().toISOString().slice(0, 7);
  return [...groups.entries()]
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([month, rows]) => {
      const sales = rows
        .filter((row) => row.eventType === 'SALE' || row.eventType === 'SERVICE')
        .reduce((sum, row) => sum + commercialAmount(row), 0);
      const costs = rows
        .filter((row) => quickAddDef(row.eventType)?.consumesBudget)
        .reduce((sum, row) => sum + commercialAmount(row), 0);
      const progress = month === current ? ' · Month in progress' : '';
      const asOfBudget = marketingPlan
        ? rollMarketingLedger(events, marketingPlan, `${month}-31`).budget
        : null;
      const budgetLine = visible && asOfBudget != null ? ` · Budget ${money(asOfBudget)} ${currency}` : '';
      const moneyLine = visible ? ` · Sales ${money(sales)} ${currency} · Costs ${money(costs)} ${currency}${budgetLine}` : '';
      return {
        id: `month-${month}`,
        title: `${month}${progress}${moneyLine}`,
        eventType: 'MONTH',
        occurredAt: `${month}-01T00:00:00.000Z`,
        amount: visible ? money(sales) : null,
        ...emptyFacts(),
      };
    });
}

export function resolveRole(
  participants: OperatingParticipant[],
  userId: string,
  membershipType: string
): OperatingRole {
  const match = participants.find((p) => p.userId === userId);
  if (match) return match.role;
  if (membershipType === 'OWNER' || membershipType === 'ADMIN') return 'OWNER_ADMIN';
  return 'TEAM_MEMBER';
}
