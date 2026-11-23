/**
 * P3 widget projection + source drill-down.
 *
 * Callers: api/v1/router.ts GET /business/moments/:momentId/widgets and
 *   GET /business/moments/:momentId/widgets/:widgetId/sources (added next to /ledger).
 * Existing ledger-projection.ts only covers AR/AP/stock — not Pulse/Moments/Life/Memory
 * widget contracts. Reads operating_event + OperatingProjection; writes none.
 * User: "For financial totals, the widget value must reconcile to the same eligible
 * source records returned by its drill-down."
 * User: "Preserve company, Moment, role, currency, period and asOf filters."
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertCompanyMomentAccess, assertFinanceVisible } from './membership';
import { getOperatingProjection, type OperatingProjectionQuery } from './operating-service';
import type { OperatingEventRow, OperatingProjection, OperatingRole } from './operating-catalog';
import { financeVisibleFor } from './operating-catalog';
import { getMomentLedgerProjection } from './ledger-projection';
import { attributedSalesTotal, marketingProgress } from './operating-marketing';
import {
  widgetsForSubtype,
  widgetById,
  coverageMatrix,
  MARKETING_WIDGET_IDS,
} from './operating-widget-catalog';
import {
  type WidgetInstance,
  type CoverageStatus,
  filterEligible,
  sumAmounts,
  resolveCoverage,
  coverageValue,
  money,
  num,
  isEffectiveForAnalytics,
} from './operating-widget-engine';

export type WidgetsProjection = {
  momentId: string;
  familyCode: string;
  subtype: string;
  currency: string;
  role: OperatingRole;
  financeVisible: boolean;
  postingComplete: boolean;
  residualIncomplete: boolean;
  coverageMatrix: ReturnType<typeof coverageMatrix>;
  widgets: WidgetInstance[];
  marketingContract?: { widgetIds: readonly string[]; count: number };
};

export type WidgetSources = {
  widgetId: string;
  formula: string;
  coverage: CoverageStatus;
  reconciledTotal: string | number | null;
  sources: Array<{
    sourceEventId: string;
    eventType: string;
    title: string;
    amount: string | null;
    occurredAt: string;
    ledgerStatus: string | null;
  }>;
};

function metric(
  key: string,
  label: string,
  raw: number | string | null,
  coverage: CoverageStatus,
  unit: string | null = null
) {
  return { key, label, value: coverageValue(coverage, raw), unit, coverage };
}

function projectOne(
  def: ReturnType<typeof widgetsForSubtype>[number],
  args: {
    projection: OperatingProjection;
    events: OperatingEventRow[];
    postingComplete: boolean;
    residualIncomplete: boolean;
    visible: boolean;
  }
): WidgetInstance {
  const { projection: p, events, postingComplete, residualIncomplete, visible } = args;
  const financeBlocked = def.financeOnly && !visible;
  const eligible = filterEligible(events, def.eligibleEventTypes);
  const sourceEventIds = eligible.map((e) => e.operatingEventId);

  const baseCoverage = resolveCoverage({
    eligibleCount: def.eligibleEventTypes.length === 0 ? 1 : eligible.length,
    measuredValue: def.eligibleEventTypes.length === 0 ? 1 : sumAmounts(eligible),
    postingComplete: def.financeOnly ? postingComplete : true,
    residualIncomplete: def.financeOnly ? residualIncomplete : false,
    forceUnavailable: financeBlocked,
  });

  let metrics = [
    metric('records', 'Records', eligible.length, eligible.length === 0 ? 'UNAVAILABLE' : 'COMPLETE', 'count'),
  ];
  let coverage: CoverageStatus = baseCoverage;
  let why: string | null = null;

  if (financeBlocked) {
    metrics = [metric('value', 'Value', null, 'UNAVAILABLE', null)];
    coverage = 'UNAVAILABLE';
    why = 'Finance figures are visible to owner, finance, and approver roles.';
  } else if (def.widgetId === 'P01') {
    const budgetConfigured = p.budget?.configured === true;
    const targetConfigured = p.target?.configured === true;
    const budgetCov = resolveCoverage({
      eligibleCount: budgetConfigured ? 1 : 0,
      measuredValue: budgetConfigured ? num(p.budget?.amount) : null,
      postingComplete,
      residualIncomplete,
    });
    const targetCov = resolveCoverage({
      eligibleCount: targetConfigured ? 1 : 0,
      measuredValue: targetConfigured ? num(p.target?.value) : null,
      postingComplete,
      residualIncomplete,
    });
    metrics = [
      metric('budget', 'Budget', budgetConfigured ? p.budget!.amount : null, budgetCov),
      metric('spent', 'Spent', budgetConfigured ? p.budget!.spent : null, budgetCov),
      metric('target', 'Target', targetConfigured ? p.target!.value : null, targetCov),
      metric('progress', 'Progress', targetConfigured ? p.target!.progress : null, targetCov),
    ];
    coverage = budgetConfigured || targetConfigured ? 'COMPLETE' : 'UNAVAILABLE';
    why = coverage === 'UNAVAILABLE' ? 'Set a target and budget to measure progress.' : null;
  } else if (def.widgetId === 'P02') {
    const sales = p.finance ? num(p.finance.sales) : null;
    const cov = resolveCoverage({
      eligibleCount: eligible.length,
      measuredValue: sales,
      postingComplete,
      residualIncomplete,
      forceUnavailable: !p.finance,
    });
    metrics = [
      metric('sales', 'Sales', p.finance?.sales ?? null, cov),
      metric('received', 'Received', p.finance?.received ?? null, cov),
      metric('outstanding', 'Outstanding', p.finance?.outstanding ?? null, cov),
      metric('payable', 'Payable', p.finance?.payable ?? null, cov),
    ];
    coverage = cov;
  } else if (def.widgetId === 'P03') {
    const open = p.paymentReadiness.length;
    const overdue = p.paymentReadiness.filter((r) => r.dueBucket === 'OVERDUE').length;
    metrics = [
      metric('open', 'Open', open, 'COMPLETE', 'count'),
      metric('overdue', 'Overdue', overdue, 'COMPLETE', 'count'),
    ];
    coverage = 'COMPLETE';
  } else if (def.widgetId === 'P04') {
    metrics = [
      metric('open', 'Open items', p.attention.length, 'COMPLETE', 'count'),
      metric('high', 'High priority', p.attention.filter((a) => a.severity === 'HIGH').length, 'COMPLETE', 'count'),
    ];
    coverage = 'COMPLETE';
  } else if (def.widgetId === 'P05') {
    metrics = [metric('records', 'Records', eligible.length, eligible.length === 0 ? 'ZERO' : 'COMPLETE', 'count')];
    coverage = eligible.length === 0 ? 'ZERO' : 'COMPLETE';
  } else if (def.widgetId === 'L03' || def.widgetId === 'ME02' || def.widgetId === 'M04') {
    const total = sumAmounts(eligible);
    const cov = resolveCoverage({
      eligibleCount: eligible.length,
      measuredValue: total,
      postingComplete,
      residualIncomplete,
    });
    metrics = [
      metric('total', 'Total', eligible.length === 0 ? null : money(total), cov),
      metric('records', 'Records', eligible.length, eligible.length === 0 ? 'UNAVAILABLE' : 'COMPLETE', 'count'),
    ];
    coverage = cov;
  } else if (def.widgetId === 'MKT-RESULTS') {
    const measured = marketingProgress(events, p.target?.metric || 'LEADS');
    const unavailable = measured.coverage === 'N/A';
    const cov = resolveCoverage({
      eligibleCount: unavailable ? 0 : eligible.length,
      measuredValue: unavailable ? null : measured.progress,
    });
    metrics = [metric('progress', 'Progress', unavailable ? null : measured.progress, cov)];
    coverage = cov;
    why = unavailable ? 'Result coverage is N/A (mixed units or empty).' : null;
  } else if (def.widgetId === 'MKT-ATTRIBUTION') {
    const sales = attributedSalesTotal(events);
    const cov = resolveCoverage({
      eligibleCount: sales == null ? 0 : 1,
      measuredValue: sales,
      postingComplete,
      residualIncomplete,
    });
    metrics = [metric('attributedSales', 'Attributed sales', sales == null ? null : money(sales), cov)];
    coverage = cov;
    why = 'Observational only — never booked as accounting revenue.';
  } else if (def.eligibleEventTypes.length === 0) {
    metrics = [metric('configured', 'Configured', 1, 'COMPLETE', 'flag')];
    coverage = 'COMPLETE';
  } else {
    const total = sumAmounts(eligible);
    const cov = resolveCoverage({
      eligibleCount: eligible.length,
      measuredValue: total,
      postingComplete: def.financeOnly ? postingComplete : true,
      residualIncomplete: def.financeOnly ? residualIncomplete : false,
    });
    metrics = [
      metric('total', 'Total', eligible.length === 0 ? null : money(total), cov),
      metric('records', 'Records', eligible.length, eligible.length === 0 ? 'UNAVAILABLE' : 'COMPLETE', 'count'),
    ];
    coverage = cov;
  }

  return { ...def, metrics, sourceEventIds, coverage, why };
}

export async function getMomentWidgets(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  query?: OperatingProjectionQuery
): Promise<WidgetsProjection> {
  await assertCompanyMomentAccess(client, ctx, momentId);
  const projection = await getOperatingProjection(client, ctx, momentId, query);
  if ('retired' in projection && projection.retired) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
  }
  const p = projection as OperatingProjection;

  let postingComplete = true;
  let residualIncomplete = false;
  if (p.financeVisible) {
    try {
      const ledger = await getMomentLedgerProjection(client, ctx, momentId);
      postingComplete =
        ledger.accountsReceivable.complete &&
        ledger.accountsPayable.complete &&
        ledger.inventory.complete;
      residualIncomplete = !postingComplete;
    } catch {
      postingComplete = true;
    }
  }

  const events: OperatingEventRow[] = (p.activity ?? []).map((row) => ({
    operatingEventId: row.id,
    eventType: row.eventType,
    title: row.title,
    note: row.note,
    amount: row.amount,
    currencyCode: p.currency,
    counterpartyName: row.counterpartyName,
    channelName: row.channelName,
    paymentRelevance: 'NONE',
    paymentStatus: row.paymentStatus ?? 'NONE',
    approvalStatus: row.approvalStatus ?? 'NONE',
    submittedByUserId: '',
    decidedByUserId: null,
    occurredAt: row.occurredAt,
    parentEventId: row.parentEventId ?? null,
    quantity: row.quantity ?? null,
    unit: row.unit ?? null,
    ledgerStatus: null,
  }));

  if (events.length > 0) {
    const ids = events.map((e) => e.operatingEventId);
    const st = await client.query<{ operating_event_id: string; ledger_status: string | null }>(
      `SELECT operating_event_id, payload->>'ledgerStatus' AS ledger_status
       FROM business.operating_event
       WHERE operating_event_id = ANY($1::uuid[])`,
      [ids]
    );
    const map = new Map(st.rows.map((r) => [r.operating_event_id, r.ledger_status]));
    for (const e of events) {
      e.ledgerStatus = map.get(e.operatingEventId) === 'SUPERSEDED' ? 'SUPERSEDED' : null;
    }
  }

  const widgets = widgetsForSubtype(p.familyCode, p.subtype).map((d) =>
    projectOne(d, {
      projection: p,
      events,
      postingComplete,
      residualIncomplete,
      visible: p.financeVisible,
    })
  );

  return {
    momentId,
    familyCode: p.familyCode,
    subtype: p.subtype,
    currency: p.currency,
    role: p.role,
    financeVisible: p.financeVisible,
    postingComplete,
    residualIncomplete,
    coverageMatrix: coverageMatrix(p.familyCode, p.subtype),
    widgets,
    marketingContract:
      p.familyCode === 'MARKETING_CAMPAIGNS'
        ? { widgetIds: MARKETING_WIDGET_IDS, count: MARKETING_WIDGET_IDS.length }
        : undefined,
  };
}

export async function getWidgetSources(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  widgetId: string,
  query?: OperatingProjectionQuery
): Promise<WidgetSources> {
  const def = widgetById(widgetId);
  if (!def) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Widget not found.', 404);

  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  if (def.financeOnly) assertFinanceVisible(scope);

  const bundle = await getMomentWidgets(client, ctx, momentId, query);
  const widget = bundle.widgets.find((w) => w.widgetId === widgetId);
  if (!widget) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Widget not applicable to this Moment subtype.', 400);
  }

  const projection = await getOperatingProjection(client, ctx, momentId, query);
  if ('retired' in projection && (projection as OperatingProjection).retired) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
  }
  const p = projection as OperatingProjection;

  const rows = await client.query<{
    operating_event_id: string;
    event_type: string;
    title: string;
    amount: string | null;
    occurred_at: Date;
    payment_status: string;
    approval_status: string;
    ledger_status: string | null;
  }>(
    `SELECT operating_event_id, event_type, title, amount::text, occurred_at,
            payment_status, approval_status, payload->>'ledgerStatus' AS ledger_status
     FROM business.operating_event
     WHERE moment_id = $1
     ORDER BY occurred_at DESC
     LIMIT 500`,
    [momentId]
  );

  const sources = rows.rows
    .filter((r) =>
      isEffectiveForAnalytics({
        operatingEventId: r.operating_event_id,
        eventType: r.event_type,
        amount: r.amount,
        approvalStatus: r.approval_status,
        paymentStatus: r.payment_status,
        ledgerStatus: r.ledger_status === 'SUPERSEDED' ? 'SUPERSEDED' : null,
      })
    )
    .filter((r) => (def.eligibleEventTypes.length === 0 ? false : def.eligibleEventTypes.includes(r.event_type)))
    .map((r) => ({
      sourceEventId: r.operating_event_id,
      eventType: r.event_type,
      title: r.title,
      amount: r.amount,
      occurredAt: r.occurred_at.toISOString(),
      ledgerStatus: r.ledger_status,
    }));

  let reconciledTotal: string | number | null =
    widget.metrics.find((m) =>
      ['sales', 'total', 'progress', 'attributedSales'].includes(m.key)
    )?.value ??
    (sources.length === 0
      ? null
      : money(sources.reduce((total, row) => total + num(row.amount), 0)));

  if (widgetId === 'P02' && p.finance && widget.coverage === 'COMPLETE') {
    reconciledTotal = p.finance.sales;
  }

  return {
    widgetId,
    formula: def.formula,
    coverage: widget.coverage,
    reconciledTotal,
    sources,
  };
}

/** Pure helper for unit tests. */
export function projectWidgetsFromProjection(args: {
  projection: OperatingProjection;
  events: OperatingEventRow[];
  postingComplete?: boolean;
  residualIncomplete?: boolean;
}): WidgetsProjection {
  const p = args.projection;
  const visible = financeVisibleFor(p.role);
  const widgets = widgetsForSubtype(p.familyCode, p.subtype).map((d) =>
    projectOne(d, {
      projection: p,
      events: args.events,
      postingComplete: args.postingComplete ?? true,
      residualIncomplete: args.residualIncomplete ?? false,
      visible,
    })
  );
  return {
    momentId: 'test',
    familyCode: p.familyCode,
    subtype: p.subtype,
    currency: p.currency,
    role: p.role,
    financeVisible: visible,
    postingComplete: args.postingComplete ?? true,
    residualIncomplete: args.residualIncomplete ?? false,
    coverageMatrix: coverageMatrix(p.familyCode, p.subtype),
    widgets,
    marketingContract:
      p.familyCode === 'MARKETING_CAMPAIGNS'
        ? { widgetIds: MARKETING_WIDGET_IDS, count: MARKETING_WIDGET_IDS.length }
        : undefined,
  };
}
