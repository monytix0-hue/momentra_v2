/**
 * P3 widget-to-source contracts for all nine Business Moment subtypes.
 *
 * Callers: operating-widget-service.ts (widgetsForSubtype, widgetById, coverageMatrix),
 *   tests/business-p3-widget-analytics.test.ts, tests/business-p3-marketing-at.test.ts.
 * Not a duplicate of operating-board-metrics (prose tiles) — this is the formal
 * Widget ID / formula / drill-down contract layer required by P3.
 * Reads/writes: pure catalog constants (no DB).
 * User: "Every widget must define Widget ID, eligible event types, formula,
 * period/currency, role visibility, coverage rules, drill-down, assertions."
 * User: "Do not replace industry-specific widgets with generic financial summaries."
 */
import type { WidgetDefinition } from './operating-widget-engine';

const ALL_TRADING = ['TRADING_WHOLESALE', 'DISTRIBUTION_DEALERSHIP'] as const;
const ALL_RETAIL = ['RETAIL_STORE', 'RESTAURANT_FOOD', 'CONSUMER_SERVICE'] as const;
const ALL_MFG = ['STANDARD_PRODUCTION', 'MADE_TO_ORDER'] as const;
const ALL_MKT = ['CLIENT_CAMPAIGN', 'OWN_BUSINESS_CAMPAIGN'] as const;
const ALL_NINE = [...ALL_TRADING, ...ALL_RETAIL, ...ALL_MFG, ...ALL_MKT];

function def(
  partial: Omit<WidgetDefinition, 'periodPolicy' | 'currencyPolicy' | 'historicalRule'> &
    Partial<Pick<WidgetDefinition, 'periodPolicy' | 'currencyPolicy' | 'historicalRule'>>
): WidgetDefinition {
  return {
    periodPolicy: partial.periodPolicy ?? 'AS_OF_RANGE',
    currencyPolicy: partial.currencyPolicy ?? 'MOMENT_CURRENCY',
    historicalRule:
      partial.historicalRule ??
      'Null/unavailable when eligible posting coverage is incomplete or backfill residual > 0; genuine zero only when eligible sources exist and sum to 0.',
    ...partial,
  };
}

const PULSE: WidgetDefinition[] = [
  def({
    widgetId: 'P01',
    screen: 'PULSE',
    title: 'Target & Budget',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['BUDGET', 'TARGET', 'REVISION', 'SALE', 'SERVICE', 'INVOICE', 'SPEND', 'PURCHASE', 'RESULT', 'PROGRESS', 'PRODUCTION'],
    financeOnly: true,
    formula: 'Effective budget/target from prefs + REVISION(effectiveFrom≤asOf); progress from industry target basis',
  }),
  def({
    widgetId: 'P02',
    screen: 'PULSE',
    title: 'Finance',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'SERVICE', 'INVOICE', 'COLLECTION', 'PAYMENT', 'SPEND', 'PURCHASE', 'VENDOR_COST', 'RETURN'],
    financeOnly: true,
    formula: 'Net posted sales − credits; received=collections; AR/AP from open obligations; exclude SUPERSEDED/REVERSAL',
  }),
  def({
    widgetId: 'P03',
    screen: 'PULSE',
    title: 'Payment Readiness',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'SERVICE', 'INVOICE', 'SPEND', 'PURCHASE', 'VENDOR_COST', 'CUSTOMER_ORDER'],
    financeOnly: true,
    formula: 'Open PENDING/READY payables & receivables with due buckets',
  }),
  def({
    widgetId: 'P04',
    screen: 'PULSE',
    title: 'Attention',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['ACTIVITY', 'APPROVAL', 'ISSUE', 'SPEND', 'SALE', 'INVOICE', 'PURCHASE'],
    financeOnly: false,
    formula: 'Industry attention cards (ATT-*); counts of high/open/approvals/overdue',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'P05',
    screen: 'PULSE',
    title: 'Activity',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'PURCHASE', 'STOCK', 'PRODUCTION', 'ACTIVITY', 'RESULT', 'PROGRESS', 'SERVICE', 'ORDER'],
    financeOnly: false,
    formula: 'Count of effective operating events in period by type',
    currencyPolicy: 'NA',
  }),
];

const MOMENTS: WidgetDefinition[] = [
  def({
    widgetId: 'M01',
    screen: 'MOMENTS',
    title: 'Moment Identity',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: [],
    financeOnly: false,
    formula: 'Setup identity/party/description (prefs)',
    currencyPolicy: 'NA',
    periodPolicy: 'ALL_TIME',
  }),
  def({
    widgetId: 'M02',
    screen: 'MOMENTS',
    title: 'People',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: [],
    financeOnly: false,
    formula: 'Setup participants',
    currencyPolicy: 'NA',
    periodPolicy: 'ALL_TIME',
  }),
  def({
    widgetId: 'M03',
    screen: 'MOMENTS',
    title: 'Network & Channels',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'PURCHASE', 'ACTIVITY', 'RESULT'],
    financeOnly: false,
    formula: 'Counterparties + channels from prefs and events',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'M04',
    screen: 'MOMENTS',
    title: 'Open Commitments',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['ORDER', 'SHIPMENT', 'CUSTOMER_ORDER', 'ACTIVITY', 'SALE', 'SPEND', 'PURCHASE', 'APPOINTMENT'],
    financeOnly: true,
    formula: 'Open orders/activities/AR/AP commitments (industry-specific)',
  }),
];

const LIFE: WidgetDefinition[] = [
  def({
    widgetId: 'L01',
    screen: 'LIFE',
    title: 'Current Business',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: [],
    financeOnly: false,
    formula: 'Identity snapshot for the active period',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'L02',
    screen: 'LIFE',
    title: 'Channels / Orders',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'ORDER', 'CUSTOMER_ORDER', 'ACTIVITY', 'RESULT'],
    financeOnly: false,
    formula: 'Channel activity or open orders (industry-specific)',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'L03',
    screen: 'LIFE',
    title: 'Money Moving',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'SERVICE', 'PAYMENT', 'COLLECTION', 'SPEND', 'VENDOR_COST', 'PURCHASE', 'INVOICE'],
    financeOnly: true,
    formula: 'Period money-moving events (exclude SUPERSEDED/REVERSAL)',
  }),
  def({
    widgetId: 'L04',
    screen: 'LIFE',
    title: 'Operations / Execution',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['ISSUE', 'STOCK', 'UPDATE', 'PRODUCTION', 'DELIVERY', 'ACTIVITY', 'PROGRESS', 'RESULT', 'SERVICE', 'APPOINTMENT'],
    financeOnly: false,
    formula: 'Execution events for the industry',
    currencyPolicy: 'NA',
  }),
];

const MEMORY: WidgetDefinition[] = [
  def({
    widgetId: 'ME01',
    screen: 'MEMORY',
    title: 'Monthly History',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'INVOICE', 'SPEND', 'PURCHASE', 'BUDGET', 'TARGET', 'REVISION', 'RESULT'],
    financeOnly: true,
    formula: 'Month buckets of net sales/costs from effective events',
    periodPolicy: 'ALL_TIME',
  }),
  def({
    widgetId: 'ME02',
    screen: 'MEMORY',
    title: 'Money History',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'SERVICE', 'PURCHASE', 'BUDGET', 'TARGET', 'SPEND', 'PAYMENT', 'COLLECTION', 'VENDOR_COST', 'INVOICE'],
    financeOnly: true,
    formula: 'Historical money events (effective only)',
    periodPolicy: 'ALL_TIME',
  }),
  def({
    widgetId: 'ME03',
    screen: 'MEMORY',
    title: 'People & Patterns',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['SALE', 'ACTIVITY', 'UPDATE', 'ISSUE', 'PAYMENT', 'COLLECTION'],
    financeOnly: false,
    formula: 'Actor × event history',
    currencyPolicy: 'NA',
    periodPolicy: 'ALL_TIME',
  }),
  def({
    widgetId: 'ME04',
    screen: 'MEMORY',
    title: 'Patterns & Decisions',
    familyCodes: ['TRADING_DISTRIBUTION', 'RETAIL_CONSUMER', 'MANUFACTURING_PRODUCTION', 'MARKETING_CAMPAIGNS'],
    subtypes: '*',
    eligibleEventTypes: ['ISSUE', 'APPROVAL', 'REVISION'],
    financeOnly: false,
    formula: 'Issues, approvals, revisions',
    currencyPolicy: 'NA',
    periodPolicy: 'ALL_TIME',
  }),
];

const INDUSTRY_EXTRA: WidgetDefinition[] = [
  def({
    widgetId: 'TW-ORDERS',
    screen: 'MOMENTS',
    title: 'Orders & Commitments',
    familyCodes: ['TRADING_DISTRIBUTION'],
    subtypes: ['TRADING_WHOLESALE'],
    eligibleEventTypes: ['ORDER', 'CUSTOMER_ORDER', 'SHIPMENT', 'DELIVERY'],
    financeOnly: false,
    formula: 'Open/dispatched/delivered order quantities',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'TW-STOCK',
    screen: 'MOMENTS',
    title: 'Stock & Products',
    familyCodes: ['TRADING_DISTRIBUTION'],
    subtypes: ['TRADING_WHOLESALE'],
    eligibleEventTypes: ['STOCK', 'PURCHASE', 'SALE'],
    financeOnly: false,
    formula: 'On-hand from stock + purchase receipts − sale issues; null when no movements',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'DD-ORDERS',
    screen: 'MOMENTS',
    title: 'Orders & Fulfilment',
    familyCodes: ['TRADING_DISTRIBUTION'],
    subtypes: ['DISTRIBUTION_DEALERSHIP'],
    eligibleEventTypes: ['ORDER', 'SHIPMENT', 'DELIVERY'],
    financeOnly: false,
    formula: 'Dealership fulfilment pipeline',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'DD-STOCK',
    screen: 'MOMENTS',
    title: 'Stock & Consignment',
    familyCodes: ['TRADING_DISTRIBUTION'],
    subtypes: ['DISTRIBUTION_DEALERSHIP'],
    eligibleEventTypes: ['STOCK', 'PURCHASE', 'SALE'],
    financeOnly: false,
    formula: 'Owned vs consignment positions when recorded',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'RT-SALES',
    screen: 'MOMENTS',
    title: 'Sales & Orders',
    familyCodes: ['RETAIL_CONSUMER'],
    subtypes: ['RETAIL_STORE', 'RESTAURANT_FOOD'],
    eligibleEventTypes: ['SALE', 'ORDER'],
    financeOnly: true,
    formula: 'Posted retail/restaurant sales (commitments excluded until SALE)',
  }),
  def({
    widgetId: 'RT-STOCK',
    screen: 'MOMENTS',
    title: 'Stock & Supplies',
    familyCodes: ['RETAIL_CONSUMER'],
    subtypes: ['RETAIL_STORE', 'RESTAURANT_FOOD'],
    eligibleEventTypes: ['STOCK', 'PURCHASE'],
    financeOnly: false,
    formula: 'Explicit stock + received purchases only',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'CS-SERVICES',
    screen: 'MOMENTS',
    title: 'Services',
    familyCodes: ['RETAIL_CONSUMER'],
    subtypes: ['CONSUMER_SERVICE'],
    eligibleEventTypes: ['SERVICE', 'SALE'],
    financeOnly: true,
    formula: 'Finalized service charges only',
  }),
  def({
    widgetId: 'CS-APPOINTMENTS',
    screen: 'MOMENTS',
    title: 'Appointments / Jobs',
    familyCodes: ['RETAIL_CONSUMER'],
    subtypes: ['CONSUMER_SERVICE'],
    eligibleEventTypes: ['APPOINTMENT'],
    financeOnly: false,
    formula: 'Bookings are commitments until SERVICE/SALE',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'MFG-ORDERS',
    screen: 'MOMENTS',
    title: 'Orders',
    familyCodes: ['MANUFACTURING_PRODUCTION'],
    subtypes: [...ALL_MFG],
    eligibleEventTypes: ['CUSTOMER_ORDER', 'ORDER'],
    financeOnly: false,
    formula: 'Customer orders are not revenue/AR/stock',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'MFG-PRODUCTION',
    screen: 'MOMENTS',
    title: 'Production',
    familyCodes: ['MANUFACTURING_PRODUCTION'],
    subtypes: [...ALL_MFG],
    eligibleEventTypes: ['PRODUCTION'],
    financeOnly: false,
    formula: 'QUICK=recorded work; ITEMIZED accepted qty stocks finished goods',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'MFG-STOCK',
    screen: 'MOMENTS',
    title: 'Stock',
    familyCodes: ['MANUFACTURING_PRODUCTION'],
    subtypes: [...ALL_MFG],
    eligibleEventTypes: ['STOCK', 'PURCHASE', 'PRODUCTION'],
    financeOnly: false,
    formula: 'Materials + finished from receipts/issues; null when no movements',
    currencyPolicy: 'NA',
  }),
  def({
    widgetId: 'MKT-RESULTS',
    screen: 'MOMENTS',
    title: 'Campaign Results',
    familyCodes: ['MARKETING_CAMPAIGNS'],
    subtypes: [...ALL_MKT],
    eligibleEventTypes: ['RESULT', 'PROGRESS'],
    financeOnly: false,
    formula: 'Incremental sum / cumulative delta; mixed units → UNAVAILABLE',
    currencyPolicy: 'NA',
    periodPolicy: 'CAMPAIGN_WINDOW',
  }),
  def({
    widgetId: 'MKT-ATTRIBUTION',
    screen: 'MOMENTS',
    title: 'Attributed Sales (observational)',
    familyCodes: ['MARKETING_CAMPAIGNS'],
    subtypes: ['OWN_BUSINESS_CAMPAIGN'],
    eligibleEventTypes: ['RESULT', 'PROGRESS'],
    financeOnly: true,
    formula: 'Sum attributedSales — never booked as accounting revenue',
    periodPolicy: 'CAMPAIGN_WINDOW',
  }),
];

export const WIDGET_CATALOG: WidgetDefinition[] = [
  ...PULSE,
  ...MOMENTS,
  ...LIFE,
  ...MEMORY,
  ...INDUSTRY_EXTRA,
];

export const MARKETING_WIDGET_IDS = [
  'P01', 'P02', 'P03', 'P04', 'P05',
  'M01', 'M02', 'M03', 'M04',
  'L01', 'L02', 'L03', 'L04',
  'ME01', 'ME02', 'ME03', 'ME04',
] as const;

/** Board section codes use M1/L1/ME1; map to Figma AN-* frame labels. */
const MARKETING_BOARD_DISPLAY_CODES: Record<string, string> = {
  M1: 'AN-M01',
  M2: 'AN-M02',
  M3: 'AN-M03',
  M4: 'AN-M04',
  L1: 'AN-L01',
  L2: 'AN-L02',
  L3: 'AN-L03',
  L4: 'AN-L04',
  ME1: 'AN-ME01',
  ME2: 'AN-ME02',
  ME3: 'AN-ME03',
  ME4: 'AN-ME04',
};

/** Pulse metric group codes → Figma AN-P0x (Harbor visual order). */
const MARKETING_PULSE_GROUP_DISPLAY_CODES: Record<string, string> = {
  ATTENTION: 'AN-P01',
  TARGET_BUDGET: 'AN-P02',
  PAYMENT_READINESS: 'AN-P03',
  FINANCE: 'AN-P04',
  ACTIVITY: 'AN-P05',
};

export function marketingBoardDisplayCode(sectionCode: string): string | null {
  return MARKETING_BOARD_DISPLAY_CODES[sectionCode] ?? null;
}

export function marketingPulseGroupDisplayCode(groupCode: string): string | null {
  return MARKETING_PULSE_GROUP_DISPLAY_CODES[groupCode] ?? null;
}

export function widgetsForSubtype(familyCode: string, subtype: string): WidgetDefinition[] {
  return WIDGET_CATALOG.filter((w) => {
    if (!w.familyCodes.includes(familyCode)) return false;
    if (w.subtypes === '*') return true;
    return w.subtypes.includes(subtype);
  });
}

export function widgetById(widgetId: string): WidgetDefinition | undefined {
  return WIDGET_CATALOG.find((w) => w.widgetId === widgetId);
}

export function coverageMatrix(familyCode: string, subtype: string) {
  return widgetsForSubtype(familyCode, subtype).map((w) => ({
    widgetId: w.widgetId,
    screen: w.screen,
    title: w.title,
    eligibleEventTypes: w.eligibleEventTypes,
    financeOnly: w.financeOnly,
    formula: w.formula,
  }));
}

export const NINE_SUBTYPES = ALL_NINE;
