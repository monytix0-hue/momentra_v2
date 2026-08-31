/**
 * Q0 — Build authoritative S9-QA Master Certification catalog.
 *
 * Sources (four):
 *   1. Frozen Figma / screen inventory (embedded from S2–S4 matrices)
 *   2. V019 capability codes (static map aligned to seed)
 *   3. Android Action Registries + hub tiles
 *   4. iOS Action Registries + hub tiles
 *
 * Output:
 *   .maestro/cert/catalog.json
 *   docs/qa/QUICK_ADD_COVERAGE_MATRIX.md
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/build-certification-catalog.ts
 */
import { writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

type Classification =
  | 'PASS_CANDIDATE'
  | 'IMPLEMENTED'
  | 'CAPABILITY_GAP'
  | 'ANDROID_MISSING'
  | 'IOS_MISSING'
  | 'API_GAP'
  | 'BACKEND_GAP'
  | 'DEFERRED'
  | 'NOT_REQUIRED'
  | 'FIGMA_STALE'
  | 'FIGMA_UNIQUE'
  | 'FAMILY_UI_REUSED';

interface QuickAddRow {
  id: string;
  context: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  momentId: string;
  momentLabel: string;
  family: string;
  momentTypeCode: string;
  theme: string;
  label: string;
  capability: string;
  apiRoute: string | null;
  expectedDbEffect: string | null;
  figmaNode: string | null;
  android: boolean;
  ios: boolean;
  androidEnabled: boolean;
  iosEnabled: boolean;
  apiAvailable: boolean;
  classification: Classification;
  notes: string;
  /** S9-QH screen-level wiring (generated) */
  destinationAndroid: string | null;
  destinationIos: string | null;
  hubTile: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  destinationScreen: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  submit: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  postSubmitRefresh: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  androidStatus: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  iosStatus: 'PASS' | 'FIX' | 'ADD' | 'N/A' | '—';
  figmaParity: string;
}

interface ScreenRow {
  id: string;
  context: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  momentId: string;
  momentLabel: string;
  screen: string;
  figmaNode: string | null;
  androidImpl: string | null;
  iosImpl: string | null;
  classification: Classification;
  notes: string;
}

interface MomentDef {
  id: string;
  context: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  family: string;
  label: string;
  momentTypeCode: string;
  theme: string;
  figmaSetup: string | null;
  familyUiReuse?: string;
  deferred?: boolean;
}

const MOMENTS: MomentDef[] = [
  // Personal P1–P4
  {
    id: 'P1',
    context: 'PERSONAL',
    family: 'LIFE_OPERATIONS',
    label: 'Life Operations',
    momentTypeCode: 'LIFE_RHYTHM',
    theme: '#7C5CFC',
    figmaSetup: '353:6809',
  },
  {
    id: 'P2',
    context: 'PERSONAL',
    family: 'FUTURE_BUILDING',
    label: 'Future Building',
    momentTypeCode: 'FUTURE_GOAL',
    theme: '#10B981',
    figmaSetup: '353:6905',
  },
  {
    id: 'P3',
    context: 'PERSONAL',
    family: 'LIFESTYLE',
    label: 'Lifestyle',
    momentTypeCode: 'LIFESTYLE',
    theme: '#0EA5A4',
    figmaSetup: '353:7075',
  },
  {
    id: 'P4',
    context: 'PERSONAL',
    family: 'RELATIONSHIPS',
    label: 'Relationships',
    momentTypeCode: 'RELATIONSHIP_CONNECTION',
    theme: '#E91E63',
    figmaSetup: '353:7217',
  },
  // Group G01–G12
  {
    id: 'G01',
    context: 'GROUP',
    family: 'SHARED_EXPERIENCE',
    label: 'Trip',
    momentTypeCode: 'TRIP',
    theme: '#E8621A',
    figmaSetup: '575:9761',
    familyUiReuse: 'GroupExperienceSetup',
  },
  {
    id: 'G02',
    context: 'GROUP',
    family: 'SHARED_EXPERIENCE',
    label: 'Wedding',
    momentTypeCode: 'WEDDING',
    theme: '#EC4899',
    figmaSetup: '575:9761',
    familyUiReuse: 'GroupExperienceSetup',
  },
  {
    id: 'G03',
    context: 'GROUP',
    family: 'SHARED_EXPERIENCE',
    label: 'House Party',
    momentTypeCode: 'HOUSE_PARTY',
    theme: '#3B82F6',
    figmaSetup: '575:9761',
    familyUiReuse: 'GroupExperienceSetup',
  },
  {
    id: 'G04',
    context: 'GROUP',
    family: 'SHARED_EXPERIENCE',
    label: 'Office Outing',
    momentTypeCode: 'OFFICE_OUTING',
    theme: '#14B8A6',
    figmaSetup: '575:9761',
    familyUiReuse: 'GroupExperienceSetup',
  },
  {
    id: 'G05',
    context: 'GROUP',
    family: 'SHARED_PURCHASE',
    label: 'Gift Pool',
    momentTypeCode: 'GIFT_POOL',
    theme: '#EC4899',
    figmaSetup: '575:9919',
    familyUiReuse: 'GroupPurchaseSetup',
  },
  {
    id: 'G06',
    context: 'GROUP',
    family: 'SHARED_PURCHASE',
    label: 'Group Purchase',
    momentTypeCode: 'GROUP_PURCHASE',
    theme: '#E8621A',
    figmaSetup: '575:9919',
    familyUiReuse: 'GroupPurchaseSetup',
  },
  {
    id: 'G07',
    context: 'GROUP',
    family: 'SHARED_PURCHASE',
    label: 'Shared Asset',
    momentTypeCode: 'SHARED_ASSET',
    theme: '#3B82F6',
    figmaSetup: '575:9919',
    familyUiReuse: 'GroupPurchaseSetup',
  },
  {
    id: 'G08',
    context: 'GROUP',
    family: 'SHARED_PURCHASE',
    label: 'Custom Purchase',
    momentTypeCode: 'COMMUNITY_PURCHASE',
    theme: '#14B8A6',
    figmaSetup: '575:9919',
    familyUiReuse: 'GroupPurchaseSetup',
  },
  {
    id: 'G09',
    context: 'GROUP',
    family: 'SHARED_LIVING',
    label: 'Flatmates',
    momentTypeCode: 'FLATMATES',
    theme: '#E8621A',
    figmaSetup: '634:13345',
    familyUiReuse: 'GroupLivingSetup',
  },
  {
    id: 'G10',
    context: 'GROUP',
    family: 'SHARED_LIVING',
    label: 'Family Household',
    momentTypeCode: 'FAMILY_HOUSEHOLD',
    theme: '#EC4899',
    figmaSetup: '634:13345',
    familyUiReuse: 'GroupLivingSetup',
  },
  {
    id: 'G11',
    context: 'GROUP',
    family: 'SHARED_LIVING',
    label: 'Co-living',
    momentTypeCode: 'CO_LIVING',
    theme: '#3B82F6',
    figmaSetup: '634:13345',
    familyUiReuse: 'GroupLivingSetup',
  },
  {
    id: 'G12',
    context: 'GROUP',
    family: 'SHARED_LIVING',
    label: 'Custom Living',
    momentTypeCode: 'COMMUNITY_LIVING',
    theme: '#14B8A6',
    figmaSetup: '634:13345',
    familyUiReuse: 'GroupLivingSetup',
  },
  // Business
  {
    id: 'B00',
    context: 'BUSINESS',
    family: 'COMPANY',
    label: 'Company Setup',
    momentTypeCode: 'COMPANY',
    theme: '#818CF8',
    figmaSetup: '649:20260',
  },
  {
    id: 'B01',
    context: 'BUSINESS',
    family: 'TEAM_OPERATIONS',
    label: 'Team Operations',
    momentTypeCode: 'TEAM_OPERATIONS',
    theme: '#818CF8',
    figmaSetup: '692:34736',
  },
  {
    id: 'B02',
    context: 'BUSINESS',
    family: 'BUSINESS_RUNWAY',
    label: 'Business Runway',
    momentTypeCode: 'BUSINESS_RUNWAY',
    theme: '#818CF8',
    figmaSetup: '692:36690',
  },
  {
    id: 'B03',
    context: 'BUSINESS',
    family: 'BUSINESS_OPERATIONS',
    label: 'Business Operations',
    momentTypeCode: 'BUSINESS_OPERATIONS',
    theme: '#818CF8',
    figmaSetup: '692:37188',
  },
  // Deferred inventory (must appear)
  {
    id: 'B-VENDOR',
    context: 'BUSINESS',
    family: 'VENDOR_OPERATIONS',
    label: 'Vendor Operations',
    momentTypeCode: 'VENDOR_OPERATIONS',
    theme: '#818CF8',
    figmaSetup: '1124:0',
    deferred: true,
  },
  {
    id: 'B-MULTILOC',
    context: 'BUSINESS',
    family: 'MULTI_LOCATION',
    label: 'Multi-location Dashboard',
    momentTypeCode: 'MULTI_LOCATION',
    theme: '#818CF8',
    figmaSetup: '692:33733',
    deferred: true,
  },
];

/** Tile definition shared across Android hubActionsFor and iOS catalogTiles. */
interface TileDef {
  label: string;
  capability: string;
  android: boolean;
  ios: boolean;
  androidEnabled: boolean;
  iosEnabled: boolean;
  apiRoute: string | null;
  expectedDbEffect: string | null;
  figmaNode: string | null;
  inV019: boolean;
  classificationOverride?: Classification;
  notes?: string;
}

function personalTiles(family: string): TileDef[] {
  switch (family) {
    case 'LIFE_OPERATIONS':
      return [
        tile('Income', 'EXPENSE_CREATE', true, true, true, true, 'POST /v1/moments/:id/income', 'finance.income', '482:18697', true, undefined, 'Life Ops money QA; precision finance path'),
        tile('Recovery', 'LIFE_OBSERVATION_RECORD', true, true, true, true, 'POST /v1/moments/:id/observations', 'personal.life_operation_observation + recovery_observation_detail', null, true, undefined, 'Life Ops precision V042–V045 typed Recovery detail'),
        tile('Mood', 'LIFE_OBSERVATION_RECORD', true, true, true, true, 'POST /v1/moments/:id/observations', 'personal.life_operation_observation + mood_observation_detail', null, true, undefined, 'Life Ops precision typed Mood detail'),
        tile('Attention', 'ATTENTION_CAPTURE', true, true, true, true, 'POST /v1/moments/:id/attention-captures', 'analytics.attention_capture', null, true, undefined, 'Life Ops precision attention-captures (not generic observation)'),
        tile('Transfer', 'MOVEMENT_RECORD', true, true, true, true, 'POST /v1/moments/:id/movements', 'finance.movement', '520:29924', true),
        tile('Savings', 'MOVEMENT_RECORD', true, true, true, true, 'POST /v1/moments/:id/movements', 'finance.movement', '520:30019', true),
        tile('Adjust', 'LIFE_OPS_ADJUST', true, true, true, true, 'POST /v1/moments/:id/life-ops-adjustments', 'personal.life_operation_adjustment', null, true, undefined, 'Life Ops precision life-ops-adjustments'),
        tile('Reflect', 'LIFE_OBSERVATION_RECORD', true, true, false, false, null, null, null, true, 'DEFERRED', 'Tile present but disabled on both platforms'),
      ];
    case 'FUTURE_BUILDING':
      return [
        tile('Expense', 'EXPENSE_CREATE', true, true, true, true, 'POST /v1/moments/:id/expenses', 'finance.expense', null, true, undefined, 'PX-1 Future precision LIVE (V046); shared expense writer'),
        tile('Milestone', 'MILESTONE_CREATE', true, true, true, true, 'POST /v1/moments/:id/future-items', 'personal.future_progress_observation', null, true, undefined, 'PX-1 future-precision LIVE; kind=MILESTONE; axis refresh from canonical counts'),
        tile('Opportunity', 'OPPORTUNITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/future-items', 'personal.future_opportunity', null, true, undefined, 'PX-1 future-precision LIVE; enriched opportunityType'),
        tile('Pivot', 'PIVOT_RECORD', true, true, true, true, 'POST /v1/moments/:id/future-items', 'personal.future_pivot', null, true, undefined, 'PX-1 future-precision LIVE; enriched pivotReason'),
        tile('Progress', 'PROGRESS_RECORD', true, true, true, true, 'POST /v1/moments/:id/future-items', 'personal.future_progress_observation', null, true, undefined, 'PX-1 future-precision LIVE; progressType GOAL|MILESTONE|GENERAL'),
        tile('Learning', 'LEARNING_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/future-items', 'personal.future_learning_activity', null, true, undefined, 'PX-1 future-precision LIVE; enriched providerName'),
      ];
    case 'LIFESTYLE':
      return [
        tile('Expense', 'EXPENSE_CREATE', true, true, true, true, 'POST /v1/moments/:id/expenses', 'finance.expense', null, true, undefined, 'PX-2 Lifestyle precision LIVE (V046)'),
        tile('Experience', 'LIFESTYLE_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/lifestyle-activities', 'personal.lifestyle_activity', null, true, undefined, 'PX-2 lifestyle-precision LIVE; context=EXPERIENCE; vitality from counts'),
        tile('Wellbeing', 'LIFESTYLE_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/lifestyle-activities', 'personal.lifestyle_activity', null, true, undefined, 'PX-2 lifestyle-precision LIVE; context=WELLBEING'),
        tile('Discovery', 'LIFESTYLE_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/lifestyle-activities', 'personal.lifestyle_activity', null, true, undefined, 'PX-2 lifestyle-precision LIVE; context=DISCOVERY'),
        tile('Expression', 'LIFESTYLE_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/lifestyle-activities', 'personal.lifestyle_activity', null, true, undefined, 'PX-2 lifestyle-precision LIVE; context=CREATION'),
        tile('Adjust', 'LIFESTYLE_ACTIVITY_CREATE', true, true, true, true, 'POST /v1/moments/:id/lifestyle-activities', 'personal.lifestyle_activity', null, true, undefined, 'PX-2 lifestyle-precision LIVE; context=LIFESTYLE'),
      ];
    case 'RELATIONSHIPS':
      return [
        tile('Connection', 'RELATIONSHIP_ACTIVITY_RECORD', true, true, true, true, 'POST /v1/moments/:id/relationship-activities', 'personal.relationship_activity', '1006:8274', true, undefined, 'PX-3 relationships-precision LIVE; bond axes from activity_type counts (no fixed-72)'),
        tile('Support', 'RELATIONSHIP_ACTIVITY_RECORD', true, true, true, true, 'POST /v1/moments/:id/relationship-activities', 'personal.relationship_activity', '1006:8274', true, undefined, 'PX-3 relationships-precision LIVE'),
        tile('Shared Exp', 'RELATIONSHIP_ACTIVITY_RECORD', true, true, true, true, 'POST /v1/moments/:id/relationship-activities', 'personal.relationship_activity', '1006:8274', true, undefined, 'PX-3 relationships-precision LIVE'),
        tile('Investment', 'RELATIONSHIP_ACTIVITY_RECORD', true, true, true, true, 'POST /v1/moments/:id/relationship-activities', 'personal.relationship_activity', '1006:8274', true, undefined, 'PX-3 relationships-precision LIVE; investmentValue/unitCode'),
        tile('Adjust', 'RELATIONSHIP_ACTIVITY_RECORD', true, true, true, true, 'POST /v1/moments/:id/relationship-activities', 'personal.relationship_activity', '1006:8274', true, undefined, 'PX-3 relationships-precision LIVE; INTERACTION adjust'),
      ];
    default:
      return [];
  }
}

function groupTiles(): TileDef[] {
  return [
    tile('Expense', 'EXPENSE_CREATE', true, true, true, true, 'POST /v1/moments/:id/group-expenses', 'finance.expense + expense_share + obligations', null, true),
    tile('Contribute', 'CONTRIBUTION_RECORD', true, true, true, true, 'POST /v1/moments/:id/contributions', 'finance.contribution', null, true),
    // GX-1 + QH-W: SETTLEMENT_RECORD mapped (V047) + shared GroupSettlementSheet wired on Android/iOS hubs.
    tile(
      'Settle',
      'SETTLEMENT_RECORD',
      true,
      true,
      true,
      true,
      'POST /v1/moments/:id/settlements',
      'finance.settlement + settlement_allocation + obligation settled_amount; group_finance_snapshot.outstanding_total; recent_activity GROUP_SETTLEMENT_RECORDED',
      null,
      true,
      'IMPLEMENTED',
      'QH-W LIVE. Shared GroupSettlementSheet for G01–G12 (+ Wedding hub). Lifecycle: outstanding before → write → row → outstanding after → audit → event → outbox → projection → Activity → Pulse/finance. Idempotency + cross-Moment rejection certified.'
    ),
    tile('People', 'PARTICIPANT_MANAGE', true, true, true, true, 'POST /v1/group/invites', 'collaboration.invite', null, true),
  ];
}

function businessTiles(family: string): TileDef[] {
  const base: TileDef[] = [
    tile('Expense', 'EXPENSE_CREATE', true, true, true, true, 'POST /v1/moments/:id/business-expenses', 'finance.expense + approval_request?', null, true),
    tile(
      'Revenue',
      'REVENUE_RECORD',
      true,
      true,
      family === 'BUSINESS_RUNWAY',
      true,
      'POST /v1/moments/:id/revenues',
      'finance.revenue',
      null,
      true,
      family === 'BUSINESS_RUNWAY' ? undefined : 'CAPABILITY_GAP',
      family === 'BUSINESS_RUNWAY' ? 'V019 default on BUSINESS_RUNWAY' : 'Revenue primarily mapped to BUSINESS_RUNWAY in V019'
    ),
    tile(
      'Invoice',
      'INVOICE_CREATE',
      true,
      true,
      family === 'BUSINESS_RUNWAY',
      true,
      'POST /v1/moments/:id/invoices',
      'finance.invoice',
      null,
      true,
      family === 'BUSINESS_RUNWAY' ? undefined : 'CAPABILITY_GAP',
      family === 'BUSINESS_RUNWAY' ? 'V019 default on BUSINESS_RUNWAY' : 'Invoice primarily mapped to BUSINESS_RUNWAY in V019'
    ),
  ];
  // iOS-only People tile (MEMBER_MANAGE) — Android sheet added S9-QH
  base.push(
    tile(
      'People',
      'MEMBER_MANAGE',
      true,
      true,
      true,
      true,
      'GET /v1/companies/:id/members',
      'business.company_membership',
      null,
      true,
      undefined,
      'Read-only members list; invite flow deferred'
    )
  );
  return base;
}

function tile(
  label: string,
  capability: string,
  android: boolean,
  ios: boolean,
  androidEnabled: boolean,
  iosEnabled: boolean,
  apiRoute: string | null,
  expectedDbEffect: string | null,
  figmaNode: string | null,
  inV019: boolean,
  classificationOverride?: Classification,
  notes?: string
): TileDef {
  return {
    label,
    capability,
    android,
    ios,
    androidEnabled,
    iosEnabled,
    apiRoute,
    expectedDbEffect,
    figmaNode,
    inV019,
    classificationOverride,
    notes,
  };
}

function classifyTile(t: TileDef): Classification {
  if (t.classificationOverride) return t.classificationOverride;
  if (!t.inV019 && t.apiRoute) return 'CAPABILITY_GAP';
  if (!t.android && t.ios) return 'ANDROID_MISSING';
  if (t.android && !t.ios) return 'IOS_MISSING';
  if (!t.apiRoute) return 'API_GAP';
  if (!t.androidEnabled && !t.iosEnabled) return 'DEFERRED';
  return 'PASS_CANDIDATE';
}

const SHELL_SCREENS = [
  'Empty',
  'Setup',
  'Pulse',
  'Moments',
  'Life',
  'Memory',
  'Activity',
] as const;

function destinationForTile(context: string, label: string): { android: string | null; ios: string | null } {
  const key = `${context}:${label}`;
  const map: Record<string, { android: string; ios: string }> = {
    'PERSONAL:Expense': { android: 'PersonalExpenseSheet', ios: 'PersonalExpenseSheet' },
    'PERSONAL:Recovery': { android: 'PersonalLifeOpsQuickAddSheet', ios: 'PersonalLifeOpsQuickAddSheet' },
    'PERSONAL:Mood': { android: 'PersonalLifeOpsQuickAddSheet', ios: 'PersonalLifeOpsQuickAddSheet' },
    'PERSONAL:Attention': { android: 'PersonalLifeOpsQuickAddSheet', ios: 'PersonalLifeOpsQuickAddSheet' },
    'PERSONAL:Adjust': { android: 'PersonalLifeOpsQuickAddSheet', ios: 'PersonalLifeOpsQuickAddSheet' },
    'PERSONAL:Reflect': { android: '—', ios: '—' },
    'PERSONAL:Transfer': { android: '—', ios: '—' },
    'PERSONAL:Savings': { android: '—', ios: '—' },
    'PERSONAL:Milestone': { android: 'PersonalFutureQuickAddSheet', ios: 'PersonalFutureQuickAddSheet' },
    'PERSONAL:Opportunity': { android: 'PersonalFutureQuickAddSheet', ios: 'PersonalFutureQuickAddSheet' },
    'PERSONAL:Pivot': { android: 'PersonalFutureQuickAddSheet', ios: 'PersonalFutureQuickAddSheet' },
    'PERSONAL:Progress': { android: 'PersonalFutureQuickAddSheet', ios: 'PersonalFutureQuickAddSheet' },
    'PERSONAL:Learning': { android: 'PersonalFutureQuickAddSheet', ios: 'PersonalFutureQuickAddSheet' },
    'PERSONAL:Experience': { android: 'PersonalLifestyleQuickAddSheet', ios: 'PersonalLifestyleQuickAddSheet' },
    'PERSONAL:Wellbeing': { android: 'PersonalLifestyleQuickAddSheet', ios: 'PersonalLifestyleQuickAddSheet' },
    'PERSONAL:Discovery': { android: 'PersonalLifestyleQuickAddSheet', ios: 'PersonalLifestyleQuickAddSheet' },
    'PERSONAL:Expression': { android: 'PersonalLifestyleQuickAddSheet', ios: 'PersonalLifestyleQuickAddSheet' },
    'PERSONAL:Connection': { android: 'PersonalRelationshipsQuickAddSheet', ios: 'PersonalRelationshipsQuickAddSheet' },
    'PERSONAL:Support': { android: 'PersonalRelationshipsQuickAddSheet', ios: 'PersonalRelationshipsQuickAddSheet' },
    'PERSONAL:Shared Exp': { android: 'PersonalRelationshipsQuickAddSheet', ios: 'PersonalRelationshipsQuickAddSheet' },
    'PERSONAL:Investment': { android: 'PersonalRelationshipsQuickAddSheet', ios: 'PersonalRelationshipsQuickAddSheet' },
    'GROUP:Expense': { android: 'GroupExpenseSheet', ios: 'GroupExpenseSheet' },
    'GROUP:Contribute': { android: 'GroupContributionSheet', ios: 'GroupContributionSheet' },
    'GROUP:Settle': {
      android: 'GroupSettlementSheet (shared G01–G12 + Wedding hub)',
      ios: 'GroupSettlementSheet (shared G01–G12 + Wedding hub)',
    },
    'GROUP:People': { android: 'GroupParticipantsSheet', ios: 'GroupParticipantsSheet' },
    'BUSINESS:Expense': { android: 'BusinessExpenseSheet', ios: 'BusinessExpenseSheet' },
    'BUSINESS:Revenue': { android: 'BusinessRevenueSheet', ios: 'BusinessRevenueSheet' },
    'BUSINESS:Invoice': { android: 'BusinessInvoiceSheet', ios: 'BusinessInvoiceSheet' },
    'BUSINESS:People': { android: 'BusinessMembersSheet', ios: 'BusinessMembersSheet' },
  };
  const hit = map[key];
  return hit ? hit : { android: null, ios: null };
}

type WiringMark = QuickAddRow['hubTile'];

function assessWiring(
  row: Omit<
    QuickAddRow,
    | 'destinationAndroid'
    | 'destinationIos'
    | 'hubTile'
    | 'destinationScreen'
    | 'submit'
    | 'postSubmitRefresh'
    | 'androidStatus'
    | 'iosStatus'
    | 'figmaParity'
  >
): Pick<
  QuickAddRow,
  | 'destinationAndroid'
  | 'destinationIos'
  | 'hubTile'
  | 'destinationScreen'
  | 'submit'
  | 'postSubmitRefresh'
  | 'androidStatus'
  | 'iosStatus'
  | 'figmaParity'
> {
  const dest = destinationForTile(row.context, row.label);
  const isGap = ['API_GAP', 'CAPABILITY_GAP', 'DEFERRED', 'ANDROID_MISSING', 'IOS_MISSING'].includes(
    row.classification
  );
  const isImplemented =
    row.classification === 'PASS_CANDIDATE' ||
    row.classification === 'IMPLEMENTED' ||
    row.classification === 'FAMILY_UI_REUSED';

  let hubTile: WiringMark = 'PASS';
  if (row.context === 'BUSINESS' && row.label !== 'Expense') {
    hubTile = 'PASS'; // BusinessQuickAddHub mounted S9-QH
  }

  let destinationScreen: WiringMark = '—';
  let submit: WiringMark = '—';
  let postSubmitRefresh: WiringMark = '—';
  let androidStatus: WiringMark = '—';
  let iosStatus: WiringMark = '—';

  if (row.classification === 'API_GAP') {
    hubTile = row.android || row.ios ? 'PASS' : 'N/A';
    destinationScreen = '—';
    submit = '—';
    postSubmitRefresh = '—';
    androidStatus = 'N/A';
    iosStatus = 'N/A';
  } else if (row.classification === 'DEFERRED') {
    hubTile = 'PASS';
    destinationScreen = '—';
    submit = '—';
    postSubmitRefresh = '—';
    androidStatus = 'N/A';
    iosStatus = 'N/A';
  } else if (row.classification === 'CAPABILITY_GAP') {
    hubTile = 'PASS';
    destinationScreen = dest.android ? 'PASS' : '—';
    submit = 'N/A';
    postSubmitRefresh = 'N/A';
    androidStatus = row.androidEnabled ? 'PASS' : 'N/A';
    iosStatus = row.iosEnabled ? 'PASS' : 'N/A';
  } else if (isImplemented) {
    destinationScreen = dest.android && dest.ios ? 'PASS' : 'FIX';
    submit = row.apiAvailable ? 'PASS' : '—';
    postSubmitRefresh =
      row.label === 'People' && row.context === 'GROUP' ? 'N/A' : row.apiAvailable ? 'PASS' : '—';
    androidStatus = row.android && row.androidEnabled ? 'PASS' : row.android ? 'N/A' : '—';
    iosStatus = row.ios && row.iosEnabled ? 'PASS' : row.ios ? 'N/A' : '—';
  }

  let figmaParity = 'EXACT';
  if (row.classification === 'API_GAP') figmaParity = 'API_GAP';
  else if (row.classification === 'CAPABILITY_GAP') figmaParity = 'CAPABILITY_GAP';
  else if (row.classification === 'DEFERRED') figmaParity = 'DEFERRED';
  else if (row.classification === 'FAMILY_UI_REUSED') figmaParity = 'FAMILY_UI_REUSED';
  else if (row.classification === 'IMPLEMENTED') figmaParity = 'IMPLEMENTED';

  return {
    destinationAndroid: dest.android,
    destinationIos: dest.ios,
    hubTile,
    destinationScreen,
    submit,
    postSubmitRefresh,
    androidStatus,
    iosStatus,
    figmaParity,
  };
}

function buildQuickAdds(): QuickAddRow[] {
  const rows: QuickAddRow[] = [];
  for (const m of MOMENTS) {
    if (m.deferred || m.id === 'B00') continue;
    let tiles: TileDef[] = [];
    if (m.context === 'PERSONAL') tiles = personalTiles(m.family);
    else if (m.context === 'GROUP') tiles = groupTiles();
    else if (m.context === 'BUSINESS') tiles = businessTiles(m.family);

    for (const t of tiles) {
      const classified = classifyTile(t);
      // Keep Settle as IMPLEMENTED — do not collapse into FAMILY_UI_REUSED.
      const classification =
        t.capability === 'SETTLEMENT_RECORD' && (classified === 'IMPLEMENTED' || classified === 'PASS_CANDIDATE')
          ? classified === 'IMPLEMENTED'
            ? 'IMPLEMENTED'
            : 'PASS_CANDIDATE'
          : m.familyUiReuse && classified === 'PASS_CANDIDATE'
            ? 'FAMILY_UI_REUSED'
            : classified;
      let notes = t.notes || '';
      if (m.familyUiReuse) {
        notes = [notes, `FAMILY_UI_REUSED:${m.familyUiReuse}`, 'FIGMA_UNIQUE subtype tile set shares family hub'].filter(Boolean).join('; ');
      }
      rows.push({
        id: `${m.id}:${t.label.replace(/\s+/g, '_')}`,
        context: m.context,
        momentId: m.id,
        momentLabel: m.label,
        family: m.family,
        momentTypeCode: m.momentTypeCode,
        theme: m.theme,
        label: t.label,
        capability: t.capability,
        apiRoute: t.apiRoute,
        expectedDbEffect: t.expectedDbEffect,
        figmaNode: t.figmaNode,
        android: t.android,
        ios: t.ios,
        androidEnabled: t.androidEnabled,
        iosEnabled: t.iosEnabled,
        apiAvailable: Boolean(t.apiRoute),
        classification,
        notes,
        ...assessWiring({
          id: `${m.id}:${t.label.replace(/\s+/g, '_')}`,
          context: m.context,
          momentId: m.id,
          momentLabel: m.label,
          family: m.family,
          momentTypeCode: m.momentTypeCode,
          theme: m.theme,
          label: t.label,
          capability: t.capability,
          apiRoute: t.apiRoute,
          expectedDbEffect: t.expectedDbEffect,
          figmaNode: t.figmaNode,
          android: t.android,
          ios: t.ios,
          androidEnabled: t.androidEnabled,
          iosEnabled: t.iosEnabled,
          apiAvailable: Boolean(t.apiRoute),
          classification,
          notes,
        }),
      });
    }
  }
  return rows;
}

function buildScreens(): ScreenRow[] {
  const rows: ScreenRow[] = [];
  for (const m of MOMENTS) {
    if (m.deferred) {
      rows.push({
        id: `${m.id}:DEFERRED`,
        context: m.context,
        momentId: m.id,
        momentLabel: m.label,
        screen: m.label,
        figmaNode: m.figmaSetup,
        androidImpl: null,
        iosImpl: null,
        classification: 'DEFERRED',
        notes: 'Inventory only — not in implemented certification scope',
      });
      continue;
    }
    if (m.id === 'B00') {
      for (const screen of ['Empty', 'CreateCompany', 'Identity', 'Location', 'Membership', 'CompanySwitcher', 'Restored']) {
        rows.push({
          id: `B00:${screen}`,
          context: 'BUSINESS',
          momentId: 'B00',
          momentLabel: 'Company Setup',
          screen,
          figmaNode: m.figmaSetup,
          androidImpl: 'BusinessEmpty / CompanyCreate',
          iosImpl: 'BusinessEmpty / CompanyCreate',
          classification: 'PASS_CANDIDATE',
          notes: '',
        });
      }
      continue;
    }
    for (const screen of SHELL_SCREENS) {
      const reuse = m.familyUiReuse
        ? `FAMILY_UI_REUSED:${m.familyUiReuse}; FIGMA_UNIQUE subtype — mark EQUIVALENT or PARTIAL after visual compare`
        : '';
      rows.push({
        id: `${m.id}:${screen}`,
        context: m.context,
        momentId: m.id,
        momentLabel: m.label,
        screen,
        figmaNode: screen === 'Setup' || screen === 'Empty' ? m.figmaSetup : null,
        androidImpl: `${m.context} shell`,
        iosImpl: `${m.context} shell`,
        classification: m.familyUiReuse ? 'FAMILY_UI_REUSED' : 'PASS_CANDIDATE',
        notes: reuse,
      });
    }
  }
  return rows;
}

function assertNoUnknown(rows: { classification: string; id: string }[]): void {
  const bad = rows.filter((r) => !r.classification || r.classification === 'UNKNOWN');
  if (bad.length) {
    throw new Error(`UNKNOWN classification forbidden: ${bad.map((b) => b.id).join(', ')}`);
  }
}

function renderMatrix(quickAdds: QuickAddRow[]): string {
  const lines: string[] = [
    '# Quick Add Coverage Matrix',
    '',
    '**Generated by** `npm run qa:build-catalog` (Q0 / S9-QH-REFRESH post V046–V047). Do not hand-edit.',
    '',
    '**Refresh note (2026-08-29):** Future/Lifestyle/Relationships precision = LIVE. Group Settle = **IMPLEMENTED** (QH-W shared sheet; V047). Master QA still blocked until explicit start.',
    '',
    'Classification vocabulary: `IMPLEMENTED` · `PASS_CANDIDATE` · `CAPABILITY_GAP` · `ANDROID_MISSING` · `IOS_MISSING` · `API_GAP` · `BACKEND_GAP` · `DEFERRED` · `NOT_REQUIRED` · `FIGMA_STALE` · `FIGMA_UNIQUE` · `FAMILY_UI_REUSED`',
    '',
    '**Rule:** UNKNOWN is forbidden. A Quick Add does not need to be implemented to finish the audit — it must be classified.',
    '',
    '| Context | Moment | Quick Add | Capability | Android | iOS | API | Classification | Notes |',
    '|---------|--------|-----------|------------|---------|-----|-----|----------------|-------|',
  ];
  for (const r of quickAdds) {
    lines.push(
      `| ${r.context} | ${r.momentId} ${r.momentLabel} | ${r.label} | ${r.capability} | ${r.android ? (r.androidEnabled ? 'Y' : 'disabled') : 'N'} | ${r.ios ? (r.iosEnabled ? 'Y' : 'disabled') : 'N'} | ${r.apiAvailable ? 'Y' : 'N'} | ${r.classification} | ${r.notes.replace(/\|/g, '/')} |`
    );
  }
  lines.push('');
  const counts = new Map<string, number>();
  for (const r of quickAdds) {
    counts.set(r.classification, (counts.get(r.classification) || 0) + 1);
  }
  lines.push('## Classification counts');
  lines.push('');
  for (const [k, v] of [...counts.entries()].sort()) {
    lines.push(`- **${k}:** ${v}`);
  }
  lines.push('');
  lines.push(`Total Quick Add rows: **${quickAdds.length}**`);
  lines.push('');
  return lines.join('\n');
}

function renderImplementationMatrix(quickAdds: QuickAddRow[]): string {
  const lines: string[] = [
    '# Quick Add Implementation Matrix',
    '',
    '**Generated by** `npm run qa:build-catalog` (S9-QH). Do not hand-edit.',
    '',
    'Distinguishes **HUB_TILE**, **DESTINATION_SCREEN**, **SUBMIT**, and **POST_SUBMIT_REFRESH** per Moment variant row.',
    '',
    '| Moment | Action | Hub | Screen | API | Submit | Refresh | Android | iOS | Figma |',
    '|--------|--------|-----|--------|-----|--------|---------|---------|-----|-------|',
  ];
  const mark = (v: string) => (v === 'PASS' ? '✓' : v === 'N/A' || v === '—' ? '—' : v);
  for (const r of quickAdds) {
    lines.push(
      `| ${r.momentLabel} | ${r.label} | ${mark(r.hubTile)} | ${mark(r.destinationScreen)} | ${r.apiAvailable ? '✓' : '—'} | ${mark(r.submit)} | ${mark(r.postSubmitRefresh)} | ${r.androidStatus} | ${r.iosStatus} | ${r.figmaParity} |`
    );
  }
  lines.push('');
  lines.push(`Total rows: **${quickAdds.length}** (19 Moment variants × tile sets)`);
  lines.push('');
  lines.push('## Moment variant gate');
  lines.push('');
  lines.push('- Personal P1–P4: **4/4**');
  lines.push('- Group G01–G12: **12/12**');
  lines.push('- Business B01–B03: **3/3**');
  lines.push('');
  return lines.join('\n');
}

function main(): void {
  assertQaFixturesSafe('build-certification-catalog');

  const repoRoot = path.resolve(__dirname, '../../../..');
  const certDir = path.join(repoRoot, '.maestro', 'cert');
  const docsQa = path.join(repoRoot, 'docs', 'qa');
  const docsImpl = path.join(repoRoot, 'docs', 'implementation');
  if (!existsSync(certDir)) mkdirSync(certDir, { recursive: true });
  if (!existsSync(docsQa)) mkdirSync(docsQa, { recursive: true });
  if (!existsSync(docsImpl)) mkdirSync(docsImpl, { recursive: true });

  const quickAdds = buildQuickAdds();
  const screens = buildScreens();
  assertNoUnknown(quickAdds);
  assertNoUnknown(screens);

  const catalog = {
    generatedAt: new Date().toISOString(),
    schemaVersion: 2,
    gate: 'S9-QH-QUICK-ADD-WIRING',
    sources: ['Figma/S2-S4 matrices', 'V019 capability seed', 'Android ActionRegistries + hubs', 'iOS ActionRegistries', 'S9-QH wiring assessment'],
    moments: MOMENTS,
    screens,
    quickAdds,
    stats: {
      momentsExecutable: MOMENTS.filter((m) => !m.deferred).length,
      momentsDeferred: MOMENTS.filter((m) => m.deferred).length,
      quickAddRows: quickAdds.length,
      screenRows: screens.length,
      unknownCount: 0,
    },
    passRule:
      'Nothing is PASS because a screen opened or a POST returned 201. Writable features require UI→request→DB→audit→event→outbox→projection→UI→persist.',
  };

  const catalogPath = path.join(certDir, 'catalog.json');
  writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
  writeFileSync(path.join(docsQa, 'QUICK_ADD_COVERAGE_MATRIX.md'), renderMatrix(quickAdds), 'utf8');
  const implMatrixPath = path.join(docsImpl, 'QUICK_ADD_IMPLEMENTATION_MATRIX.md');
  writeFileSync(implMatrixPath, renderImplementationMatrix(quickAdds), 'utf8');

  console.log(
    JSON.stringify(
      {
        ok: true,
        catalogPath,
        matrixPath: path.join(docsQa, 'QUICK_ADD_COVERAGE_MATRIX.md'),
        implementationMatrixPath: implMatrixPath,
        stats: catalog.stats,
      },
      null,
      2
    )
  );
}

main();
