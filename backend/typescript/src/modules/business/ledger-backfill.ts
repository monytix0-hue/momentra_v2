/**
 * P2.1 Historical backfill of V104 financial/inventory effects.
 * Reuses planFinancialEffects / planInventoryMovements + live sync writers.
 * Callers: scripts/business-v104-backfill.ts, business-p2-*-live.test.ts
 */
import type { PoolClient } from 'pg';
import {
  planFinancialEffects,
  syncFinancialEffectsFromOperatingEvent,
  type PlannedFinancialEffect,
} from './finance-bridge';
import {
  planInventoryMovements,
  syncInventoryFromOperatingEvent,
  type PlannedInventoryMovement,
} from './inventory-bridge';

export type BackfillEventRow = {
  operating_event_id: string;
  moment_id: string;
  company_id: string | null;
  event_type: string;
  title: string;
  amount: string | null;
  currency_code: string | null;
  payment_status: string;
  approval_status: string;
  parent_event_id: string | null;
  quantity: string | null;
  payload: Record<string, unknown> | null;
  occurred_at: Date;
};

export type BackfillEventPlan = {
  eventId: string;
  momentId: string;
  companyId: string | null;
  eventType: string;
  currencyCode: string;
  financial: PlannedFinancialEffect[];
  inventory: PlannedInventoryMovement[];
  ambiguous: boolean;
  skipReasons: string[];
};

export type BackfillReport = {
  mode: 'DRY_RUN' | 'APPLY';
  scanned: number;
  expectedObligations: number;
  expectedAllocations: number;
  expectedMovements: number;
  createdObligations: number;
  createdAllocations: number;
  createdMovements: number;
  /** Expected plans that already had a posting before this apply (not failures). */
  alreadyPresentObligations: number;
  alreadyPresentAllocations: number;
  alreadyPresentMovements: number;
  skipped: number;
  ambiguous: number;
  byCompany: Record<string, { obligations: number; allocations: number; movements: number }>;
  byMoment: Record<string, { obligations: number; allocations: number; movements: number }>;
  ambiguousEvents: Array<{ eventId: string; reason: string }>;
  exclusionPolicy: string;
  /**
   * expected − created − alreadyPresent should be 0 when every eligible plan is posted.
   * Non-zero means eligible source coverage is still incomplete (preserve as null in projections).
   */
  gapNote: string;
};

const EXCLUSION =
  'Events without company_id are excluded from financial/inventory postings. ' +
  'Ambiguous PAYMENT/COLLECTION without parentEventId or allocations are reported and not invented. ' +
  'Approval PENDING/REJECTED events are skipped. Non-financial event types are skipped.';

export async function loadBackfillCandidates(
  client: PoolClient,
  opts?: { momentId?: string; companyId?: string; limit?: number }
): Promise<BackfillEventRow[]> {
  const r = await client.query<BackfillEventRow>(
    `SELECT operating_event_id, moment_id, company_id, event_type, title,
            amount::text, currency_code, payment_status, approval_status,
            parent_event_id, quantity::text, payload, occurred_at
     FROM business.operating_event
     WHERE ($1::uuid IS NULL OR moment_id = $1::uuid)
       AND ($2::uuid IS NULL OR company_id = $2::uuid)
     ORDER BY occurred_at ASC, created_at ASC
     LIMIT $3`,
    [opts?.momentId ?? null, opts?.companyId ?? null, opts?.limit ?? 100000]
  );
  return r.rows;
}

export function planBackfillEvent(row: BackfillEventRow): BackfillEventPlan {
  const currency = (row.currency_code || 'INR').toUpperCase();
  const payload = row.payload ?? {};
  const dueAt = typeof payload.dueAt === 'string' ? payload.dueAt : null;
  const allocations =
    payload.contract && typeof payload.contract === 'object'
      ? ((payload.contract as Record<string, unknown>).allocations as
          | Array<{ sourceId: string; amount: string }>
          | undefined) ?? null
      : null;

  const skipReasons: string[] = [];
  if (!row.company_id) skipReasons.push('missing_company_id');

  const financial = planFinancialEffects({
    eventId: row.operating_event_id,
    eventType: row.event_type,
    amount: row.amount,
    currencyCode: currency,
    paymentStatus: row.payment_status,
    approvalStatus: row.approval_status,
    parentEventId: row.parent_event_id,
    dueAt,
    allocations,
    requireSettlementTarget: true,
  });
  const inventory = planInventoryMovements({
    eventId: row.operating_event_id,
    eventType: row.event_type,
    quantity: row.quantity,
    unit: typeof payload.unit === 'string' ? payload.unit : null,
    payload,
    title: row.title,
  });

  const ambiguous =
    financial.some((f) => f.kind === 'ALLOCATE' && f.ambiguous) ||
    inventory.some((m) => m.ambiguous === true);

  return {
    eventId: row.operating_event_id,
    momentId: row.moment_id,
    companyId: row.company_id,
    eventType: row.event_type,
    currencyCode: currency,
    financial,
    inventory,
    ambiguous,
    skipReasons,
  };
}

function bump(
  map: Record<string, { obligations: number; allocations: number; movements: number }>,
  key: string,
  field: 'obligations' | 'allocations' | 'movements',
  n = 1
) {
  const cur = map[key] ?? { obligations: 0, allocations: 0, movements: 0 };
  cur[field] += n;
  map[key] = cur;
}

export async function runLedgerBackfill(
  client: PoolClient,
  opts: {
    mode: 'DRY_RUN' | 'APPLY';
    momentId?: string;
    companyId?: string;
    limit?: number;
    actorLabel?: string;
  }
): Promise<BackfillReport> {
  const rows = await loadBackfillCandidates(client, opts);
  const report: BackfillReport = {
    mode: opts.mode,
    scanned: rows.length,
    expectedObligations: 0,
    expectedAllocations: 0,
    expectedMovements: 0,
    createdObligations: 0,
    createdAllocations: 0,
    createdMovements: 0,
    alreadyPresentObligations: 0,
    alreadyPresentAllocations: 0,
    alreadyPresentMovements: 0,
    skipped: 0,
    ambiguous: 0,
    byCompany: {},
    byMoment: {},
    ambiguousEvents: [],
    exclusionPolicy: EXCLUSION,
    gapNote:
      'expected − created − alreadyPresent = residual incomplete coverage. ' +
      'Ambiguous settlements are excluded (not invented). Missing coverage must stay null in projections, not zero.',
  };

  const runIns = await client.query<{ backfill_run_id: string }>(
    `INSERT INTO business.ledger_backfill_run (mode, actor_label)
     VALUES ($1, $2) RETURNING backfill_run_id`,
    [opts.mode, opts.actorLabel ?? 'backfill']
  );
  const runId = runIns.rows[0]!.backfill_run_id;

  for (const row of rows) {
    const plan = planBackfillEvent(row);
    if (plan.skipReasons.length || !plan.companyId) {
      report.skipped += 1;
      continue;
    }
    if (plan.ambiguous) {
      report.ambiguous += 1;
      for (const f of plan.financial) {
        if (f.kind === 'ALLOCATE' && f.ambiguous) {
          report.ambiguousEvents.push({ eventId: plan.eventId, reason: f.reason ?? 'ambiguous' });
        }
      }
      // Do not invent allocations for ambiguous settlements.
      const safeFinancial = plan.financial.filter((f) => !(f.kind === 'ALLOCATE' && f.ambiguous));
      plan.financial = safeFinancial;
    }

    for (const f of plan.financial) {
      if (f.kind === 'OPEN_OBLIGATION') {
        report.expectedObligations += 1;
        bump(report.byCompany, plan.companyId!, 'obligations');
        bump(report.byMoment, plan.momentId, 'obligations');
      } else if (f.kind === 'ALLOCATE') {
        report.expectedAllocations += 1;
        bump(report.byCompany, plan.companyId!, 'allocations');
        bump(report.byMoment, plan.momentId, 'allocations');
      } else if (f.kind === 'SKIP') {
        report.skipped += 1;
      }
    }
    for (const m of plan.inventory) {
      if (m.ambiguous) continue;
      report.expectedMovements += 1;
      bump(report.byCompany, plan.companyId!, 'movements');
      bump(report.byMoment, plan.momentId, 'movements');
    }

    if (opts.mode !== 'APPLY') continue;

    const beforeObl = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_obligation WHERE source_event_id = $1`,
      [plan.eventId]
    );
    // Callers: tests/business-p2-ledger-integrity-live.test.ts, scripts/business-v104-backfill.ts.
    // Reads/writes business.business_settlement_allocation (payment_event_id only — avoids parent-side double count).
    // User: "Ensure repeated backfills create no duplicate effects."
    const beforeAlloc = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_settlement_allocation
       WHERE payment_event_id = $1`,
      [plan.eventId]
    );
    const beforeMov = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_inventory_movement WHERE source_event_id = $1`,
      [plan.eventId]
    );

    const payload = row.payload ?? {};
    const dueAt = typeof payload.dueAt === 'string' ? payload.dueAt : null;
    const allocations =
      payload.contract && typeof payload.contract === 'object'
        ? ((payload.contract as Record<string, unknown>).allocations as
            | Array<{ sourceId: string; amount: string }>
            | undefined) ?? null
        : null;

    try {
      await syncFinancialEffectsFromOperatingEvent(client, {
        companyId: plan.companyId!,
        momentId: plan.momentId,
        eventId: plan.eventId,
        eventType: plan.eventType,
        amount: row.amount,
        currencyCode: plan.currencyCode,
        paymentStatus: row.payment_status,
        approvalStatus: row.approval_status,
        parentEventId: row.parent_event_id,
        dueAt,
        allocations,
      });
    } catch (err) {
      const msg = String(err);
      if (msg.includes('no open obligation') || msg.includes('Allocation source')) {
        report.ambiguous += 1;
        report.ambiguousEvents.push({ eventId: plan.eventId, reason: 'allocation_target_missing' });
      } else {
        throw err;
      }
    }

    await syncInventoryFromOperatingEvent(client, {
      companyId: plan.companyId!,
      momentId: plan.momentId,
      eventId: plan.eventId,
      eventType: plan.eventType,
      quantity: row.quantity,
      unit: typeof payload.unit === 'string' ? payload.unit : null,
      occurredAt: row.occurred_at.toISOString(),
      payload,
    });

    const afterObl = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_obligation WHERE source_event_id = $1`,
      [plan.eventId]
    );
    const afterAlloc = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_settlement_allocation
       WHERE payment_event_id = $1`,
      [plan.eventId]
    );
    const afterMov = await client.query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_inventory_movement WHERE source_event_id = $1`,
      [plan.eventId]
    );

    const beforeOblN = Number(beforeObl.rows[0]?.n ?? 0);
    const beforeAllocN = Number(beforeAlloc.rows[0]?.n ?? 0);
    const beforeMovN = Number(beforeMov.rows[0]?.n ?? 0);
    const afterOblN = Number(afterObl.rows[0]?.n ?? 0);
    const afterAllocN = Number(afterAlloc.rows[0]?.n ?? 0);
    const afterMovN = Number(afterMov.rows[0]?.n ?? 0);

    // alreadyPresent: planned effect existed before this event's sync (live P1 writes or prior apply).
    if (plan.financial.some((f) => f.kind === 'OPEN_OBLIGATION') && beforeOblN > 0) {
      report.alreadyPresentObligations += 1;
    }
    if (plan.financial.some((f) => f.kind === 'ALLOCATE') && beforeAllocN > 0) {
      report.alreadyPresentAllocations += 1;
    }
    if (plan.inventory.some((m) => !m.ambiguous) && beforeMovN > 0) {
      report.alreadyPresentMovements += plan.inventory.filter((m) => !m.ambiguous).length;
      // Cap alreadyPresent to planned count when prior movements exceed plan (correction reversals etc.).
      // Recounted below via created delta; clamp after loop via Math.min in report finalize.
    }

    report.createdObligations += Math.max(0, afterOblN - beforeOblN);
    report.createdAllocations += Math.max(0, afterAllocN - beforeAllocN);
    report.createdMovements += Math.max(0, afterMovN - beforeMovN);
  }

  // Clamp alreadyPresent movements to expected (prior reversals can inflate beforeMov).
  report.alreadyPresentMovements = Math.min(report.alreadyPresentMovements, report.expectedMovements);

  await client.query(
    `UPDATE business.ledger_backfill_run
     SET completed_at = now(),
         expected_obligations = $2,
         expected_allocations = $3,
         expected_movements = $4,
         created_obligations = $5,
         created_allocations = $6,
         created_movements = $7,
         skipped_events = $8,
         ambiguous_events = $9,
         report = $10::jsonb
     WHERE backfill_run_id = $1`,
    [
      runId,
      report.expectedObligations,
      report.expectedAllocations,
      report.expectedMovements,
      report.createdObligations,
      report.createdAllocations,
      report.createdMovements,
      report.skipped,
      report.ambiguous,
      JSON.stringify({
        exclusionPolicy: report.exclusionPolicy,
        gapNote: report.gapNote,
        ambiguousEvents: report.ambiguousEvents.slice(0, 200),
        byCompany: report.byCompany,
        byMoment: report.byMoment,
        scanned: report.scanned,
        alreadyPresent: {
          obligations: report.alreadyPresentObligations,
          allocations: report.alreadyPresentAllocations,
          movements: report.alreadyPresentMovements,
        },
        residual: {
          obligations: Math.max(
            0,
            report.expectedObligations - report.createdObligations - report.alreadyPresentObligations
          ),
          allocations: Math.max(
            0,
            report.expectedAllocations - report.createdAllocations - report.alreadyPresentAllocations
          ),
          movements: Math.max(
            0,
            report.expectedMovements - report.createdMovements - report.alreadyPresentMovements
          ),
        },
      }),
    ]
  );

  return report;
}
