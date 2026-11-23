/**
 * P2.4 Projection reconciliation against V104 obligations / inventory.
 * Outstanding AR/AP and stock are null when source coverage is incomplete.
 *
 * Callers: GET /v1/business/moments/:momentId/ledger
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { assertCompanyMomentAccess, assertFinanceVisible } from './membership';

export type LedgerProjection = {
  momentId: string;
  companyId: string | null;
  financeVisible: boolean;
  accountsReceivable: {
    outstanding: string | null;
    obligationCount: number;
    complete: boolean;
    note: string;
  };
  accountsPayable: {
    outstanding: string | null;
    obligationCount: number;
    complete: boolean;
    note: string;
  };
  inventory: {
    netQuantity: string | null;
    movementCount: number;
    complete: boolean;
    note: string;
  };
  drilldown: Array<{
    sourceEventId: string;
    eventType: string;
    obligationKind: string | null;
    obligationStatus: string | null;
    obligationOutstanding: string | null;
    movementKinds: string[];
  }>;
};

export async function getMomentLedgerProjection(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<LedgerProjection> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  // Restricted roles cannot access financial drill-downs.
  assertFinanceVisible(scope);

  const events = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n FROM business.operating_event
     WHERE moment_id = $1
       AND event_type IN ('SPEND','PURCHASE','VENDOR_COST','SALE','SERVICE','INVOICE','PAYMENT','COLLECTION','STOCK','PRODUCTION','RETURN','SHIPMENT','DELIVERY')
       AND approval_status NOT IN ('PENDING','REJECTED')`,
    [momentId]
  );
  const eligibleEvents = Number(events.rows[0]?.n ?? 0);

  const ar = await client.query<{ outstanding: string | null; n: string }>(
    `SELECT COALESCE(SUM(outstanding),0)::text AS outstanding, COUNT(*)::text AS n
     FROM business.business_obligation
     WHERE moment_id = $1 AND obligation_kind = 'RECEIVABLE' AND status IN ('OPEN','PARTIAL','SETTLED')`,
    [momentId]
  );
  const ap = await client.query<{ outstanding: string | null; n: string }>(
    `SELECT COALESCE(SUM(outstanding),0)::text AS outstanding, COUNT(*)::text AS n
     FROM business.business_obligation
     WHERE moment_id = $1 AND obligation_kind = 'PAYABLE' AND status IN ('OPEN','PARTIAL','SETTLED')`,
    [momentId]
  );
  const mov = await client.query<{ net: string | null; n: string }>(
    `SELECT
       CASE WHEN COUNT(*) = 0 THEN NULL
            ELSE COALESCE(SUM(
              CASE movement_kind
                WHEN 'RECEIPT' THEN quantity
                WHEN 'TRANSFER_IN' THEN quantity
                WHEN 'RETURN' THEN quantity
                WHEN 'ADJUSTMENT' THEN quantity
                WHEN 'ISSUE' THEN -quantity
                WHEN 'TRANSFER_OUT' THEN -quantity
                ELSE 0
              END
            ), 0)::text
       END AS net,
       COUNT(*)::text AS n
     FROM business.business_inventory_movement
     WHERE moment_id = $1`,
    [momentId]
  );

  // Completeness: every eligible financial/inventory event should have at least one effect
  // OR be a known non-posting shape (e.g. QUICK production). We treat coverage as complete
  // when there are zero eligible events OR when effect rows exist for the moment.
  const uncovered = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM business.operating_event e
     WHERE e.moment_id = $1
       AND e.company_id IS NOT NULL
       AND e.approval_status NOT IN ('PENDING','REJECTED')
       AND e.event_type IN ('SPEND','PURCHASE','VENDOR_COST','SALE','SERVICE','INVOICE','PAYMENT','COLLECTION','STOCK','PRODUCTION','RETURN','SHIPMENT','DELIVERY')
       AND e.event_type NOT IN ('REVERSAL')
       AND NOT EXISTS (SELECT 1 FROM business.business_obligation o WHERE o.source_event_id = e.operating_event_id)
       AND NOT EXISTS (SELECT 1 FROM business.business_inventory_movement m WHERE m.source_event_id = e.operating_event_id)
       AND NOT (
         e.event_type = 'PRODUCTION'
         AND COALESCE(e.payload->'contract'->>'entryMode','QUICK') <> 'ITEMIZED'
       )
       AND NOT (
         e.event_type IN ('PAYMENT','COLLECTION')
         AND e.parent_event_id IS NULL
         AND COALESCE(e.payload->'contract'->'allocations', '[]'::jsonb) = '[]'::jsonb
       )`,
    [momentId]
  );
  const uncoveredN = Number(uncovered.rows[0]?.n ?? 0);
  const complete = uncoveredN === 0;

  const drill = await client.query<{
    operating_event_id: string;
    event_type: string;
    obligation_kind: string | null;
    status: string | null;
    outstanding: string | null;
    movement_kinds: string[] | null;
  }>(
    `SELECT e.operating_event_id, e.event_type,
            o.obligation_kind, o.status, o.outstanding::text,
            COALESCE(
              (SELECT array_agg(DISTINCT m.movement_kind)
               FROM business.business_inventory_movement m
               WHERE m.source_event_id = e.operating_event_id),
              ARRAY[]::text[]
            ) AS movement_kinds
     FROM business.operating_event e
     LEFT JOIN business.business_obligation o ON o.source_event_id = e.operating_event_id
     WHERE e.moment_id = $1
       AND (
         o.obligation_id IS NOT NULL
         OR EXISTS (SELECT 1 FROM business.business_inventory_movement m WHERE m.source_event_id = e.operating_event_id)
       )
     ORDER BY e.occurred_at DESC
     LIMIT 100`,
    [momentId]
  );

  const arCount = Number(ar.rows[0]?.n ?? 0);
  const apCount = Number(ap.rows[0]?.n ?? 0);
  const movCount = Number(mov.rows[0]?.n ?? 0);

  return {
    momentId,
    companyId: scope.companyId,
    financeVisible: scope.financeVisible,
    accountsReceivable: {
      outstanding: complete ? (ar.rows[0]?.outstanding ?? '0.0000') : null,
      obligationCount: arCount,
      complete,
      note: complete
        ? 'Outstanding AR from business_obligation.'
        : `Incomplete source coverage (${uncoveredN} eligible events without effects). AR unavailable.`,
    },
    accountsPayable: {
      outstanding: complete ? (ap.rows[0]?.outstanding ?? '0.0000') : null,
      obligationCount: apCount,
      complete,
      note: complete
        ? 'Outstanding AP from business_obligation.'
        : `Incomplete source coverage (${uncoveredN} eligible events without effects). AP unavailable.`,
    },
    inventory: {
      netQuantity: complete ? mov.rows[0]?.net ?? null : null,
      movementCount: movCount,
      complete,
      note: complete
        ? movCount > 0
          ? 'Net stock from business_inventory_movement.'
          : 'No inventory movements recorded.'
        : `Incomplete source coverage (${uncoveredN} eligible events without effects). Stock unavailable.`,
    },
    drilldown: drill.rows.map((r) => ({
      sourceEventId: r.operating_event_id,
      eventType: r.event_type,
      obligationKind: r.obligation_kind,
      obligationStatus: r.status,
      obligationOutstanding: r.outstanding,
      movementKinds: r.movement_kinds ?? [],
    })),
  };
}
