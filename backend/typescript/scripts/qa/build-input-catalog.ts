/**
 * S9-QA-A — Build canonical Quick Add capability catalog.
 *
 * This is NOT the 3,300 scenario ledger (S9-QA-B). It freezes what can be tested:
 * tile + fields + route + calculation type + projections + platform support + status.
 *
 * Sources:
 *   - Action Registries / QuickAddKind hubs (embedded hub truth)
 *   - Existing cert catalog classifications
 *   - accessibility-ids.md Maestro IDs
 *
 * Output: .maestro/input-catalog/catalog.json
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/build-input-catalog.ts
 */
import { writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

type Context = 'PERSONAL' | 'GROUP' | 'BUSINESS';
type Status = 'ACTIVE' | 'CAPABILITY_GAP' | 'LOCAL_ONLY' | 'DEFERRED' | 'BROKEN';
type CalculationType =
  | 'none'
  | 'personal_balance'
  | 'group_split'
  | 'group_contribution'
  | 'group_settlement'
  | 'business_finance'
  | 'business_ops'
  | 'approval';

interface FieldDef {
  id: string;
  required: boolean;
  role: 'amount' | 'note' | 'selector' | 'date' | 'split' | 'text' | 'submit' | 'other';
}

interface CatalogItem {
  id: string;
  context: Context;
  family: string;
  momentId: string;
  momentLabel: string;
  momentTypeCode: string;
  quickAdd: string;
  hubLabelAndroid: string;
  hubLabelIos: string;
  tileMaestroId: string | null;
  screenId: string | null;
  requiredFields: FieldDef[];
  optionalFields: FieldDef[];
  backendRoute: string | null;
  dbWrite: string | null;
  calculationType: CalculationType;
  expectedProjections: string[];
  androidSupported: boolean;
  iosSupported: boolean;
  status: Status;
  notes: string;
}

interface MomentMeta {
  id: string;
  context: Context;
  family: string;
  label: string;
  momentTypeCode: string;
}

const MOMENTS: MomentMeta[] = [
  { id: 'P1', context: 'PERSONAL', family: 'LIFE_OPERATIONS', label: 'Life Operations', momentTypeCode: 'LIFE_RHYTHM' },
  { id: 'P2', context: 'PERSONAL', family: 'FUTURE_BUILDING', label: 'Future Building', momentTypeCode: 'FUTURE_GOAL' },
  { id: 'P3', context: 'PERSONAL', family: 'LIFESTYLE', label: 'Lifestyle', momentTypeCode: 'LIFESTYLE' },
  { id: 'P4', context: 'PERSONAL', family: 'RELATIONSHIPS', label: 'Relationships', momentTypeCode: 'RELATIONSHIP_CONNECTION' },
  { id: 'G01', context: 'GROUP', family: 'SHARED_EXPERIENCE', label: 'Trip', momentTypeCode: 'TRIP' },
  { id: 'G02', context: 'GROUP', family: 'SHARED_EXPERIENCE', label: 'Wedding', momentTypeCode: 'WEDDING' },
  { id: 'G03', context: 'GROUP', family: 'SHARED_EXPERIENCE', label: 'House Party', momentTypeCode: 'HOUSE_PARTY' },
  { id: 'G04', context: 'GROUP', family: 'SHARED_EXPERIENCE', label: 'Office Outing', momentTypeCode: 'OFFICE_OUTING' },
  { id: 'G05', context: 'GROUP', family: 'SHARED_PURCHASE', label: 'Gift Pool', momentTypeCode: 'GIFT_POOL' },
  { id: 'G06', context: 'GROUP', family: 'SHARED_PURCHASE', label: 'Group Purchase', momentTypeCode: 'GROUP_PURCHASE' },
  { id: 'G07', context: 'GROUP', family: 'SHARED_PURCHASE', label: 'Shared Asset', momentTypeCode: 'SHARED_ASSET' },
  { id: 'G08', context: 'GROUP', family: 'SHARED_PURCHASE', label: 'Custom Purchase', momentTypeCode: 'COMMUNITY_PURCHASE' },
  { id: 'G09', context: 'GROUP', family: 'SHARED_LIVING', label: 'Flatmates', momentTypeCode: 'FLATMATES' },
  { id: 'G10', context: 'GROUP', family: 'SHARED_LIVING', label: 'Family Household', momentTypeCode: 'FAMILY_HOUSEHOLD' },
  { id: 'G11', context: 'GROUP', family: 'SHARED_LIVING', label: 'Co-living', momentTypeCode: 'CO_LIVING' },
  { id: 'G12', context: 'GROUP', family: 'SHARED_LIVING', label: 'Custom Living', momentTypeCode: 'COMMUNITY_LIVING' },
  { id: 'B01', context: 'BUSINESS', family: 'TEAM_OPERATIONS', label: 'Team Operations', momentTypeCode: 'TEAM_OPERATIONS' },
  { id: 'B02', context: 'BUSINESS', family: 'BUSINESS_RUNWAY', label: 'Business Runway', momentTypeCode: 'BUSINESS_RUNWAY' },
  { id: 'B03', context: 'BUSINESS', family: 'BUSINESS_OPERATIONS', label: 'Business Operations', momentTypeCode: 'BUSINESS_OPERATIONS' },
];

function f(id: string, required: boolean, role: FieldDef['role']): FieldDef {
  return { id, required, role };
}

function item(partial: Omit<CatalogItem, 'id'> & { id?: string }): CatalogItem {
  const id = partial.id ?? `${partial.momentId}:${partial.quickAdd}`;
  return { ...partial, id };
}

function personalTiles(): CatalogItem[] {
  const p1 = MOMENTS.find((m) => m.id === 'P1')!;
  const p2 = MOMENTS.find((m) => m.id === 'P2')!;
  const p3 = MOMENTS.find((m) => m.id === 'P3')!;
  const p4 = MOMENTS.find((m) => m.id === 'P4')!;
  const projections = ['pulse', 'activity', 'personal_finance'];

  const lifeOps: CatalogItem[] = [
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Income',
      hubLabelAndroid: 'Income',
      hubLabelIos: 'Expense',
      tileMaestroId: 'qa.tile.income',
      screenId: 'PER-LO-S14',
      requiredFields: [
        f('personal.income.amount', true, 'amount'),
        f('personal.income.submit', true, 'submit'),
      ],
      optionalFields: [
        f('personal.income.category', false, 'selector'),
        f('personal.income.account', false, 'selector'),
        f('personal.income.note', false, 'note'),
      ],
      backendRoute: 'POST /v1/moments/:id/income',
      dbWrite: 'finance.income',
      calculationType: 'personal_balance',
      expectedProjections: projections,
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'Label drift: Android Income / iOS Expense registry; both money writes',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Recovery',
      hubLabelAndroid: 'Recovery',
      hubLabelIos: 'Recovery',
      tileMaestroId: 'qa.tile.recovery',
      screenId: null,
      requiredFields: [
        f('personal.lifeops.recovery.note', true, 'note'),
        f('personal.lifeops.recovery.submit', true, 'submit'),
      ],
      optionalFields: [],
      backendRoute: 'POST /v1/moments/:id/observations',
      dbWrite: 'personal.life_operation_observation + recovery_observation_detail',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'LIFE_OBSERVATION_RECORD / typed Recovery',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Mood',
      hubLabelAndroid: 'Mood',
      hubLabelIos: 'Mood',
      tileMaestroId: 'qa.tile.mood',
      screenId: null,
      requiredFields: [
        f('personal.lifeops.mood.note', true, 'note'),
        f('personal.lifeops.mood.submit', true, 'submit'),
      ],
      optionalFields: [],
      backendRoute: 'POST /v1/moments/:id/observations',
      dbWrite: 'personal.life_operation_observation + mood_observation_detail',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: '',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Attention',
      hubLabelAndroid: 'Attention',
      hubLabelIos: 'Attention',
      tileMaestroId: 'qa.tile.attention',
      screenId: null,
      requiredFields: [
        f('personal.lifeops.attention.note', true, 'note'),
        f('personal.lifeops.attention.submit', true, 'submit'),
      ],
      optionalFields: [],
      backendRoute: 'POST /v1/moments/:id/attention-captures',
      dbWrite: 'personal.attention_capture',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'ATTENTION_CAPTURE precision path',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Transfer',
      hubLabelAndroid: 'Transfer',
      hubLabelIos: 'Transfer',
      tileMaestroId: 'qa.tile.transfer',
      screenId: 'PER-LO-S15',
      requiredFields: [
        f('personal.money.transfer.amount', true, 'amount'),
        f('personal.money.transfer.submit', true, 'submit'),
      ],
      optionalFields: [f('personal.money.transfer.note', false, 'note')],
      backendRoute: 'POST /v1/moments/:id/movements',
      dbWrite: 'finance.movement',
      calculationType: 'personal_balance',
      expectedProjections: projections,
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'MOVEMENT_RECORD',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Savings',
      hubLabelAndroid: 'Savings',
      hubLabelIos: 'Savings',
      tileMaestroId: 'qa.tile.savings',
      screenId: null,
      requiredFields: [
        f('personal.money.savings.amount', true, 'amount'),
        f('personal.money.savings.submit', true, 'submit'),
      ],
      optionalFields: [f('personal.money.savings.account', false, 'selector')],
      backendRoute: 'POST /v1/moments/:id/movements',
      dbWrite: 'finance.movement',
      calculationType: 'personal_balance',
      expectedProjections: projections,
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: '',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Adjust',
      hubLabelAndroid: 'Adjust',
      hubLabelIos: 'Adjust',
      tileMaestroId: 'qa.tile.adjust',
      screenId: null,
      requiredFields: [
        f('personal.lifeops.adjust.note', true, 'note'),
        f('personal.lifeops.adjust.submit', true, 'submit'),
      ],
      optionalFields: [],
      backendRoute: 'POST /v1/moments/:id/life-ops-adjustments',
      dbWrite: 'personal.life_ops_adjustment',
      calculationType: 'none',
      expectedProjections: ['pulse', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'LIFE_OPS_ADJUST',
    }),
    item({
      context: 'PERSONAL',
      family: p1.family,
      momentId: p1.id,
      momentLabel: p1.label,
      momentTypeCode: p1.momentTypeCode,
      quickAdd: 'Reflect',
      hubLabelAndroid: 'Reflect',
      hubLabelIos: 'Reflect',
      tileMaestroId: 'qa.tile.reflect',
      screenId: null,
      requiredFields: [],
      optionalFields: [],
      backendRoute: null,
      dbWrite: null,
      calculationType: 'none',
      expectedProjections: [],
      androidSupported: false,
      iosSupported: false,
      status: 'DEFERRED',
      notes: 'Tile present but tappable=false on both platforms',
    }),
  ];

  const futureKinds = [
    ['Milestone', 'MILESTONE_CREATE', 'qa.tile.milestone'],
    ['Opportunity', 'OPPORTUNITY_CREATE', 'qa.tile.opportunity'],
    ['Pivot', 'PIVOT_RECORD', 'qa.tile.pivot'],
    ['Progress', 'PROGRESS_RECORD', 'qa.tile.progress'],
    ['Learning', 'LEARNING_ACTIVITY_CREATE', 'qa.tile.learning'],
  ] as const;

  const future: CatalogItem[] = futureKinds.map(([label, , tileId]) =>
    item({
      context: 'PERSONAL',
      family: p2.family,
      momentId: p2.id,
      momentLabel: p2.label,
      momentTypeCode: p2.momentTypeCode,
      quickAdd: label,
      hubLabelAndroid: label,
      hubLabelIos: label,
      tileMaestroId: tileId,
      screenId: null,
      requiredFields: [f('personal.future.title', true, 'text'), f('personal.future.submit', true, 'submit')],
      optionalFields: [f('personal.future.note', false, 'note'), f('personal.future.date', false, 'date')],
      backendRoute: 'POST /v1/moments/:id/future-items',
      dbWrite: 'personal.future_item',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: 'Field Maestro IDs to be wired per sheet in S9-QA-D if missing',
    })
  );

  const lifestyleKinds = [
    ['Experience', 'qa.tile.experience'],
    ['Wellbeing', 'qa.tile.wellbeing'],
    ['Discovery', 'qa.tile.discovery'],
    ['Create', 'qa.tile.expression'],
    ['Adjust', 'qa.tile.adjust'],
  ] as const;

  const lifestyle: CatalogItem[] = lifestyleKinds.map(([label, tileId]) =>
    item({
      context: 'PERSONAL',
      family: p3.family,
      momentId: p3.id,
      momentLabel: p3.label,
      momentTypeCode: p3.momentTypeCode,
      quickAdd: label === 'Create' ? 'Expression' : label,
      hubLabelAndroid: label,
      hubLabelIos: label,
      tileMaestroId: tileId,
      screenId: null,
      requiredFields: [f('personal.lifestyle.title', true, 'text'), f('personal.lifestyle.submit', true, 'submit')],
      optionalFields: [f('personal.lifestyle.note', false, 'note')],
      backendRoute: 'POST /v1/moments/:id/lifestyle-activities',
      dbWrite: 'personal.lifestyle_activity',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: label === 'Create' ? 'Hub label Create; catalog quickAdd Expression' : '',
    })
  );

  const relKinds = [
    ['Connection', 'qa.tile.connection'],
    ['Support', 'qa.tile.support'],
    ['Shared Exp', 'qa.tile.shared_exp'],
    ['Investment', 'qa.tile.investment'],
    ['Adjust', 'qa.tile.adjust'],
  ] as const;

  const relationships: CatalogItem[] = relKinds.map(([label, tileId]) =>
    item({
      context: 'PERSONAL',
      family: p4.family,
      momentId: p4.id,
      momentLabel: p4.label,
      momentTypeCode: p4.momentTypeCode,
      quickAdd: label,
      hubLabelAndroid: label,
      hubLabelIos: label,
      tileMaestroId: tileId,
      screenId: null,
      requiredFields: [
        f('personal.relationship.display_name', true, 'text'),
        f('personal.relationship.submit', true, 'submit'),
      ],
      optionalFields: [f('personal.relationship.note', false, 'note')],
      backendRoute: 'POST /v1/moments/:id/relationship-activities',
      dbWrite: 'personal.relationship_activity',
      calculationType: 'none',
      expectedProjections: ['pulse', 'activity', 'life'],
      androidSupported: true,
      iosSupported: true,
      status: 'ACTIVE',
      notes: '',
    })
  );

  return [...lifeOps, ...future, ...lifestyle, ...relationships];
}

function groupExpenseFields(): { required: FieldDef[]; optional: FieldDef[] } {
  return {
    required: [
      f('group.expense.amount', true, 'amount'),
      f('group.expense.split', true, 'split'),
      f('group.expense.submit', true, 'submit'),
    ],
    optional: [
      f('group.expense.note', false, 'note'),
      f('group.expense.payer', false, 'selector'),
      f('group.expense.split.equal', false, 'split'),
      f('group.expense.split.percentage', false, 'split'),
      f('group.expense.split.exact', false, 'split'),
      f('group.expense.split.shares', false, 'split'),
    ],
  };
}

function groupTiles(): CatalogItem[] {
  const out: CatalogItem[] = [];
  const expenseFields = groupExpenseFields();

  for (const m of MOMENTS.filter((x) => x.context === 'GROUP')) {
    out.push(
      item({
        context: 'GROUP',
        family: m.family,
        momentId: m.id,
        momentLabel: m.label,
        momentTypeCode: m.momentTypeCode,
        quickAdd: 'Expense',
        hubLabelAndroid: 'Expense',
        hubLabelIos: 'Expense',
        tileMaestroId: 'qa.tile.expense',
        screenId: null,
        requiredFields: expenseFields.required,
        optionalFields: expenseFields.optional,
        backendRoute: 'POST /v1/moments/:id/group-expenses',
        dbWrite: 'finance.expense + finance.expense_split + finance.expense_share',
        calculationType: 'group_split',
        expectedProjections: ['pulse', 'activity', 'group_finance_snapshot'],
        androidSupported: true,
        iosSupported: true,
        status: 'ACTIVE',
        notes: 'Split strategies EQUAL|PERCENTAGE|EXACT|SHARES; platform datasets diverge in S9-QA-B',
      })
    );

    const hasContribution =
      m.family !== 'SHARED_LIVING' || m.id === 'G09' || m.id === 'G11' || m.family === 'SHARED_EXPERIENCE' || m.family === 'SHARED_PURCHASE';

    if (hasContribution) {
      out.push(
        item({
          context: 'GROUP',
          family: m.family,
          momentId: m.id,
          momentLabel: m.label,
          momentTypeCode: m.momentTypeCode,
          quickAdd: 'Contribute',
          hubLabelAndroid: 'Contribution',
          hubLabelIos: 'Contribution',
          tileMaestroId: 'qa.tile.contribute',
          screenId: null,
          requiredFields: [
            f('group.contribute.amount', true, 'amount'),
            f('group.contribute.submit', true, 'submit'),
          ],
          optionalFields: [f('group.contribute.note', false, 'note')],
          backendRoute: 'POST /v1/moments/:id/contributions',
          dbWrite: 'finance.contribution',
          calculationType: 'group_contribution',
          expectedProjections: ['pulse', 'activity', 'group_finance_snapshot'],
          androidSupported: true,
          iosSupported: true,
          status: m.id === 'G10' || m.id === 'G12' ? 'CAPABILITY_GAP' : 'ACTIVE',
          notes:
            m.id === 'G10' || m.id === 'G12'
              ? 'Contribution not on Living hub for Family/Custom'
              : '',
        })
      );
    }

    out.push(
      item({
        context: 'GROUP',
        family: m.family,
        momentId: m.id,
        momentLabel: m.label,
        momentTypeCode: m.momentTypeCode,
        quickAdd: 'Settle',
        hubLabelAndroid: 'Settle',
        hubLabelIos: 'Settle',
        tileMaestroId: 'qa.tile.settle',
        screenId: null,
        requiredFields: [
          f('group.settle.amount', true, 'amount'),
          f('group.settle.submit', true, 'submit'),
        ],
        optionalFields: [f('group.settle.note', false, 'note')],
        backendRoute: 'POST /v1/moments/:id/settlements',
        dbWrite: 'finance.settlement',
        calculationType: 'group_settlement',
        expectedProjections: ['pulse', 'activity', 'group_finance_snapshot'],
        androidSupported: m.id !== 'G02',
        iosSupported: true,
        status: m.id === 'G02' ? 'BROKEN' : 'ACTIVE',
        notes:
          m.id === 'G02'
            ? 'Android Wedding hub missing Settle tile (iOS has it) — classify BROKEN until hub parity'
            : 'V047 settlements LIVE',
      })
    );

    out.push(
      item({
        context: 'GROUP',
        family: m.family,
        momentId: m.id,
        momentLabel: m.label,
        momentTypeCode: m.momentTypeCode,
        quickAdd: 'People',
        hubLabelAndroid: m.id === 'G01' ? 'Invite' : 'Participant',
        hubLabelIos: m.id === 'G01' ? 'Invite' : 'Participant',
        tileMaestroId: 'qa.tile.people',
        screenId: null,
        requiredFields: [],
        optionalFields: [f('group.invite.create', false, 'other')],
        backendRoute: 'POST /v1/group/invites',
        dbWrite: 'collaboration.moment_participant',
        calculationType: 'none',
        expectedProjections: ['activity'],
        androidSupported: true,
        iosSupported: true,
        status: 'ACTIVE',
        notes: 'Invite/participant manage; full invite redeem covered in isolation class',
      })
    );

    // Living rule deferred
    if (m.family === 'SHARED_LIVING') {
      out.push(
        item({
          context: 'GROUP',
          family: m.family,
          momentId: m.id,
          momentLabel: m.label,
          momentTypeCode: m.momentTypeCode,
          quickAdd: 'Rule',
          hubLabelAndroid: 'House Rule',
          hubLabelIos: 'Rule',
          tileMaestroId: null,
          screenId: null,
          requiredFields: [],
          optionalFields: [],
          backendRoute: null,
          dbWrite: null,
          calculationType: 'none',
          expectedProjections: [],
          androidSupported: false,
          iosSupported: false,
          status: 'DEFERRED',
          notes: 'isLive=false; label drift House Rule vs Rule',
        })
      );
    }
  }

  return out;
}

function businessTiles(): CatalogItem[] {
  const out: CatalogItem[] = [];
  const b01 = MOMENTS.find((m) => m.id === 'B01')!;
  const b02 = MOMENTS.find((m) => m.id === 'B02')!;
  const b03 = MOMENTS.find((m) => m.id === 'B03')!;

  const teamOps = [
    ['Team Update', 'POST /v1/moments/:id/business-updates', 'business.business_update'],
    ['Decision', 'POST /v1/moments/:id/decisions', 'business.decision'],
    ['Blocker', 'POST /v1/moments/:id/issues', 'business.issue'],
    ['Meeting', 'POST /v1/moments/:id/meeting-records', 'business.meeting_record'],
    ['Recognition', 'POST /v1/moments/:id/recognitions', 'business.recognition'],
    ['Approval', 'POST /v1/moments/:id/approval-requests', 'governance.approval_request'],
    ['Milestone', 'POST /v1/moments/:id/milestones', 'work.milestone'],
    ['Retrospective', 'POST /v1/moments/:id/retrospectives', 'business.retrospective'],
    ['Risk', 'POST /v1/moments/:id/risks', 'business.risk'],
    ['Activity Log', 'POST /v1/moments/:id/activity-log-entries', 'business.activity_log_entry'],
    ['Poll', 'POST /v1/moments/:id/polls', 'governance.poll'],
    ['Memory', 'POST /v1/moments/:id/business-memory', 'memory.memory'],
  ] as const;

  for (const [label, route, db] of teamOps) {
    out.push(
      item({
        context: 'BUSINESS',
        family: b01.family,
        momentId: b01.id,
        momentLabel: b01.label,
        momentTypeCode: b01.momentTypeCode,
        quickAdd: label,
        hubLabelAndroid: label,
        hubLabelIos: label,
        tileMaestroId: null,
        screenId: 'B-BT06',
        requiredFields: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
        optionalFields: [f('business.ops.note', false, 'note')],
        backendRoute: route,
        dbWrite: db,
        calculationType: label === 'Approval' ? 'approval' : 'business_ops',
        expectedProjections: ['pulse', 'activity', 'business_memory'].filter(Boolean),
        androidSupported: true,
        iosSupported: true,
        status: label === 'Approval' ? 'ACTIVE' : 'ACTIVE',
        notes: 'Team Ops Action Center; per-tile Maestro IDs incomplete — wire in S9-QA-D',
      })
    );
  }

  // Legacy finance tiles on B01 — capability gap
  for (const qa of ['Revenue', 'Invoice'] as const) {
    out.push(
      item({
        context: 'BUSINESS',
        family: b01.family,
        momentId: b01.id,
        momentLabel: b01.label,
        momentTypeCode: b01.momentTypeCode,
        quickAdd: qa,
        hubLabelAndroid: qa,
        hubLabelIos: qa,
        tileMaestroId: qa === 'Revenue' ? 'qa.tile.revenue' : 'qa.tile.invoice',
        screenId: null,
        requiredFields: [],
        optionalFields: [],
        backendRoute: qa === 'Revenue' ? 'POST /v1/moments/:id/revenues' : 'POST /v1/moments/:id/invoices',
        dbWrite: qa === 'Revenue' ? 'finance.revenue' : 'finance.invoice',
        calculationType: 'business_finance',
        expectedProjections: [],
        androidSupported: false,
        iosSupported: false,
        status: 'CAPABILITY_GAP',
        notes: 'V019 maps Revenue/Invoice to BUSINESS_RUNWAY only; not on B01 hub',
      })
    );
  }

  const runway = [
    {
      quickAdd: 'Revenue',
      tile: 'qa.tile.revenue',
      route: 'POST /v1/moments/:id/revenues',
      db: 'finance.revenue',
      required: [f('business.revenue.amount', true, 'amount'), f('business.revenue.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Expense',
      tile: 'qa.tile.expense',
      route: 'POST /v1/moments/:id/business-expenses',
      db: 'finance.expense',
      required: [
        f('business.expense.amount', true, 'amount'),
        f('business.expense.submit', true, 'submit'),
      ],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Invoice',
      tile: 'qa.tile.invoice',
      route: 'POST /v1/moments/:id/invoices',
      db: 'finance.invoice',
      required: [
        f('business.invoice.customer', true, 'text'),
        f('business.invoice.amount', true, 'amount'),
        f('business.invoice.submit', true, 'submit'),
      ],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Tax Entry',
      tile: null,
      route: 'POST /v1/moments/:id/tax-obligations',
      db: 'finance.tax_obligation',
      required: [f('business.tax.amount', true, 'amount'), f('business.tax.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Investor Update',
      tile: null,
      route: 'POST /v1/moments/:id/business-updates',
      db: 'business.business_update',
      required: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Budget Alert',
      tile: null,
      route: 'POST /v1/moments/:id/business-updates',
      db: 'business.business_update',
      required: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Forecast',
      tile: null,
      route: 'POST /v1/moments/:id/forecasts',
      db: 'business.forecast',
      required: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'General Update',
      tile: null,
      route: 'POST /v1/moments/:id/business-updates',
      db: 'business.business_update',
      required: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
    {
      quickAdd: 'Memory',
      tile: null,
      route: 'POST /v1/moments/:id/business-memory',
      db: 'memory.memory',
      required: [f('business.ops.title', true, 'text'), f('business.ops.submit', true, 'submit')],
      status: 'ACTIVE' as Status,
    },
  ];

  for (const r of runway) {
    out.push(
      item({
        context: 'BUSINESS',
        family: b02.family,
        momentId: b02.id,
        momentLabel: b02.label,
        momentTypeCode: b02.momentTypeCode,
        quickAdd: r.quickAdd,
        hubLabelAndroid: r.quickAdd === 'Revenue' ? 'Log Revenue' : r.quickAdd === 'Expense' ? 'Log Expense' : r.quickAdd,
        hubLabelIos: r.quickAdd === 'Revenue' ? 'Log Revenue' : r.quickAdd === 'Expense' ? 'Log Expense' : r.quickAdd,
        tileMaestroId: r.tile,
        screenId: null,
        requiredFields: r.required,
        optionalFields: [],
        backendRoute: r.route,
        dbWrite: r.db,
        calculationType:
          r.quickAdd === 'Revenue' || r.quickAdd === 'Expense' || r.quickAdd === 'Invoice'
            ? 'business_finance'
            : 'business_ops',
        expectedProjections: ['pulse', 'activity', 'business_finance'],
        androidSupported: true,
        iosSupported: true,
        status: r.status,
        notes: '',
      })
    );
  }

  const ops = [
    ['Spend Entry', 'POST /v1/moments/:id/business-expenses', 'finance.expense', 'business_finance'],
    ['Vendor', 'POST /v1/moments/:id/vendors', 'business.vendor', 'business_ops'],
    ['Approval', 'POST /v1/moments/:id/approval-requests', 'governance.approval_request', 'approval'],
    ['Issue', 'POST /v1/moments/:id/issues', 'business.issue', 'business_ops'],
    ['Improvement', 'POST /v1/moments/:id/issues', 'business.issue', 'business_ops'],
    ['Budget Review', 'POST /v1/moments/:id/business-updates', 'business.business_update', 'business_ops'],
    ['SLA', 'POST /v1/moments/:id/slas', 'business.sla', 'business_ops'],
    ['General Update', 'POST /v1/moments/:id/business-updates', 'business.business_update', 'business_ops'],
    ['Memory', 'POST /v1/moments/:id/business-memory', 'memory.memory', 'business_ops'],
  ] as const;

  for (const [label, route, db, calc] of ops) {
    out.push(
      item({
        context: 'BUSINESS',
        family: b03.family,
        momentId: b03.id,
        momentLabel: b03.label,
        momentTypeCode: b03.momentTypeCode,
        quickAdd: label,
        hubLabelAndroid: label.startsWith('Spend') ? 'Log Spend Entry' : label,
        hubLabelIos: label.startsWith('Spend') ? 'Log Spend Entry' : label,
        tileMaestroId: null,
        screenId: null,
        requiredFields: [
          label === 'Spend Entry'
            ? f('business.expense.amount', true, 'amount')
            : f('business.ops.title', true, 'text'),
          f('business.ops.submit', true, 'submit'),
        ],
        optionalFields: [],
        backendRoute: route,
        dbWrite: db,
        calculationType: calc as CalculationType,
        expectedProjections: ['pulse', 'activity'],
        androidSupported: true,
        iosSupported: true,
        status: 'ACTIVE',
        notes: 'Ops hub; Revenue/Invoice intentionally absent (Runway-only)',
      })
    );
  }

  for (const qa of ['Revenue', 'Invoice'] as const) {
    out.push(
      item({
        context: 'BUSINESS',
        family: b03.family,
        momentId: b03.id,
        momentLabel: b03.label,
        momentTypeCode: b03.momentTypeCode,
        quickAdd: qa,
        hubLabelAndroid: qa,
        hubLabelIos: qa,
        tileMaestroId: qa === 'Revenue' ? 'qa.tile.revenue' : 'qa.tile.invoice',
        screenId: null,
        requiredFields: [],
        optionalFields: [],
        backendRoute: qa === 'Revenue' ? 'POST /v1/moments/:id/revenues' : 'POST /v1/moments/:id/invoices',
        dbWrite: qa === 'Revenue' ? 'finance.revenue' : 'finance.invoice',
        calculationType: 'business_finance',
        expectedProjections: [],
        androidSupported: false,
        iosSupported: false,
        status: 'CAPABILITY_GAP',
        notes: 'Not on B03 hub; Runway-gated',
      })
    );
  }

  return out;
}

function summarize(items: CatalogItem[]) {
  const byStatus: Record<string, number> = {};
  const byContext: Record<string, number> = {};
  for (const i of items) {
    byStatus[i.status] = (byStatus[i.status] ?? 0) + 1;
    byContext[i.context] = (byContext[i.context] ?? 0) + 1;
  }
  return { byStatus, byContext, total: items.length };
}

function main() {
  assertQaFixturesSafe('build-input-catalog');

  const items = [...personalTiles(), ...groupTiles(), ...businessTiles()];
  const summary = summarize(items);

  const outDir = path.resolve(__dirname, '../../../../.maestro/input-catalog');
  if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });

  const catalog = {
    schemaVersion: 1,
    gate: 'S9-QA-A',
    generatedAt: new Date().toISOString(),
    description:
      'Canonical Quick Add capability catalog. Scenarios (1100×3) live in Excel (S9-QA-B), not here.',
    verificationChain: [
      'ui_tile',
      'writable_fields',
      'api_persistence',
      'expected_calculation',
      'post_write_surfaces',
    ],
    statusTaxonomy: ['ACTIVE', 'CAPABILITY_GAP', 'LOCAL_ONLY', 'DEFERRED', 'BROKEN'],
    moments: MOMENTS,
    summary,
    labelDrift: [
      {
        id: 'P1:Income',
        android: 'Income',
        ios: 'Expense',
        note: 'Both map to money write; prefer qa.tile.income / text guards',
      },
      {
        id: 'P3:Expression',
        android: 'Create',
        ios: 'Create',
        note: 'Matrix Expression; hub Create; Maestro qa.tile.expression',
      },
      {
        id: 'Living:Rule',
        android: 'House Rule',
        ios: 'Rule',
        note: 'DEFERRED both platforms',
      },
      {
        id: 'G02:Settle',
        android: 'missing from Wedding hub',
        ios: 'present',
        note: 'BROKEN on Android until hub parity',
      },
    ],
    foundationBlockersFixed: [
      'MQA-A-001: ProductOnboardingScreen Skip now has testTag onboarding.skip',
      'iOS appId: resolvingpoint.momentra aligned in ios/02_personal flows',
      'Group split Maestro IDs: group.expense.split.{equal|percentage|exact|shares}',
      'Business Runway revenue/invoice amount+submit IDs wired',
    ],
    quickAdds: items,
  };

  const outPath = path.join(outDir, 'catalog.json');
  writeFileSync(outPath, JSON.stringify(catalog, null, 2) + '\n', 'utf8');

  const mdPath = path.join(outDir, 'README.md');
  writeFileSync(
    mdPath,
    `# S9-QA-A Input Catalog

Generated: \`${catalog.generatedAt}\`

## Role

- **This catalog** = capabilities (what can be tested)
- **Excel (S9-QA-B)** = scenarios + expected math (1,100 × 3)
- **Platform CSVs** = exports for Maestro
- **qa:verify** = backend truth

## Summary

| Context | Count |
|---------|------:|
| PERSONAL | ${summary.byContext.PERSONAL ?? 0} |
| GROUP | ${summary.byContext.GROUP ?? 0} |
| BUSINESS | ${summary.byContext.BUSINESS ?? 0} |
| **Total** | **${summary.total}** |

### By status

${Object.entries(summary.byStatus)
  .map(([k, v]) => `- **${k}:** ${v}`)
  .join('\n')}

## Rebuild

\`\`\`powershell
cd backend\\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:build-input-catalog
\`\`\`

Do not hand-edit \`catalog.json\`. Fix sources and regenerate.
`,
    'utf8'
  );

  console.log(`[build-input-catalog] wrote ${outPath}`);
  console.log(`[build-input-catalog] ${summary.total} capabilities`, summary.byStatus);
}

main();
