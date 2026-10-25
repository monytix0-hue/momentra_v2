import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { insertAudit, insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { assertCompanyMomentAccess } from './membership';
import {
  canApprove,
  canSubmit,
  familyForSubtype,
  isLegacyBusinessFamily,
  isOperatingFamily,
  isOperatingSubtype,
  needsMakerChecker,
  normalizeOperatingPreferences,
  projectOperatingMoment,
  quickAddDef,
  resolveRole,
  validateOperatingAllocation,
  type OperatingEventRow,
  type OperatingPreferences,
  type OperatingProjection,
  type OperatingRole,
} from './operating-catalog';
import {
  isRetailSubtype,
  parseOperatingContract,
  validateRetailWrite,
  type OperatingContract,
} from './operating-retail-contract';
import { isTradingSubtype, validateTradingWrite } from './operating-trading-contract';
import { uomDimension } from './operating-trading-lov';
import { isMarketingSubtype, observationDelta, parseMarketingFacts, validateMarketingWrite } from './operating-marketing';
import { isManufacturingSubtype, validateManufacturingWrite } from './operating-manufacturing';

const amountSchema = z.string().regex(/^\d+(\.\d{1,4})?$/);

export const recordOperatingEventSchema = z
  .object({
    eventType: z.string().min(1).max(40),
    title: z.string().min(1).max(240),
    note: z.string().max(2000).optional(),
    amount: amountSchema.optional(),
    counterpartyName: z.string().max(200).optional(),
    channelName: z.string().max(120).optional(),
    paymentStatus: z.enum(['NONE', 'PENDING', 'READY', 'PAID', 'COLLECTED']).optional(),
    occurredAt: z.string().datetime({ offset: true }).nullish(),
    parentEventId: z.string().uuid().nullish(),
    quantity: amountSchema.nullish(),
    unit: z.string().max(24).nullish(),
    dueAt: z.string().max(40).nullish(),
    lines: z
      .array(
        z.object({
          itemName: z.string().min(1).max(160),
          quantity: amountSchema.nullish(),
          unit: z.string().max(24).nullish(),
          unitPrice: amountSchema.nullish(),
          masterId: z.string().max(80).nullish(),
          baseQuantity: amountSchema.nullish(),
          discount: amountSchema.nullish(),
          taxMode: z.enum(['INCLUSIVE', 'EXCLUSIVE']).nullish(),
          gstRate: amountSchema.nullish(),
        })
      )
      .max(12)
      .nullish(),
    master: z
      .object({
        kind: z.string().min(1).max(40),
        name: z.string().min(1).max(160),
        unit: z.string().max(24).nullish(),
        dimension: z.enum(['count', 'mass', 'volume', 'length', 'area']).nullish(),
        conversionToBase: amountSchema.nullish(),
        tracked: z.boolean().nullish(),
        barcode: z.string().max(80).nullish(),
        gstApplicable: z.enum(['YES', 'NO', 'NOT_SURE']).nullish(),
        gstClassification: z.enum(['TAXABLE', 'EXEMPT', 'NIL', 'NON_GST']).nullish(),
        gstRate: amountSchema.nullish(),
        priceIncludesGst: z.enum(['YES', 'NO']).nullish(),
        salePriceIncludesGst: z.enum(['YES', 'NO']).nullish(),
        purchasePriceIncludesGst: z.enum(['YES', 'NO']).nullish(),
        gstEffectiveFrom: z.string().max(10).nullish(),
        hsn: z.string().max(16).nullish(),
        gstRegistration: z.enum(['REGISTERED', 'UNREGISTERED', 'NOT_SURE']).nullish(),
        businessState: z.string().max(80).nullish(),
        billingState: z.string().max(80).nullish(),
        deliveryState: z.string().max(80).nullish(),
        gstin: z.string().max(20).nullish(),
      })
      .nullish(),
    metric: z.enum(['LEADS', 'CONVERSIONS']).optional(),
    reportingMode: z.enum(['INCREMENTAL', 'CUMULATIVE']).optional(),
    metricCode: z.string().max(40).optional(),
    budgetCategory: z.string().max(80).optional(),
    paymentState: z.enum(['PAID', 'PART_PAID', 'PAY_LATER']).optional(),
    activityStatus: z.enum(['OPEN', 'DONE', 'IN_PROGRESS', 'BLOCKED', 'POSTPONED', 'COMPLETED', 'CANCELLED']).optional(),
    attributedSales: amountSchema.optional(),
    revisionKind: z.enum(['BUDGET', 'TARGET', 'CATEGORY']).optional(),
    effectiveAt: z.string().max(40).optional(),
    costBasis: z.enum(['AGENCY', 'REIMBURSABLE']).optional(),
    overlapDeduped: z.boolean().optional(),
    resultSource: z.string().max(160).optional(),
    periodEnd: z.string().max(40).optional(),
    creativeVersion: z.string().max(40).optional(),
    effectiveFrom: z.string().max(40).optional(),
    evidenceUrl: z.string().max(500).optional(),
    plannedStart: z.string().max(40).optional(),
    blockedById: z.string().max(80).optional(),
    bookingReference: z.string().max(80).optional(),
    deliverableType: z.string().max(80).optional(),
    repeatRule: z.string().max(80).optional(),
    idempotencyKey: z.string().max(80).optional(),
    rejectedQuantity: amountSchema.optional(),
    contract: z
      .object({
        entryMode: z.enum(['QUICK', 'ITEMIZED']).nullish(),
        paymentState: z.enum(['RECEIVED', 'PART_PAID', 'PAY_LATER', 'PAID']).nullish(),
        receiptState: z.enum(['RECEIVED', 'PART_RECEIVED', 'AWAITING']).nullish(),
        receivedAmount: amountSchema.nullish(),
        quantityReceived: amountSchema.nullish(),
        stockAction: z.enum(['SET', 'ADD', 'REDUCE']).nullish(),
        stockReason: z.enum(['opening', 'stocktake', 'damage', 'waste', 'return', 'correction']).nullish(),
        expenseCategory: z.string().max(80).nullish(),
        appointmentStatus: z.enum(['BOOKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW']).nullish(),
        appointmentAction: z.enum(['RESCHEDULE', 'CANCEL', 'NO_SHOW', 'START', 'COMPLETE']).nullish(),
        orderStatus: z.enum(['PLACED', 'PREPARING', 'COMPLETED', 'CANCELLED']).nullish(),
        orderSide: z.enum(['SALES', 'PURCHASE']).nullish(),
        goodsStatus: z.enum(['DELIVERED', 'PARTIAL', 'LATER']).nullish(),
        clientKey: z.string().max(80).nullish(),
        allocations: z.array(z.object({ sourceId: z.string().min(1).max(80), amount: amountSchema })).max(12).nullish(),
        reorderPoint: amountSchema.nullish(),
        priorTimes: z.array(z.string().max(40)).max(20).nullish(),
        startedAt: z.string().max(40).nullish(),
        completedAt: z.string().max(40).nullish(),
        endsAt: z.string().max(40).nullish(),
        declaredQuantity: amountSchema.nullish(),
        staffName: z.string().max(120).nullish(),
        followUp: z.boolean().nullish(),
        estimatedAmount: amountSchema.nullish(),
        ownership: z.enum(['OWNED', 'CONSIGNMENT']).nullish(),
        shipmentStatus: z.enum(['PLACED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED']).nullish(),
        promisedAt: z.string().max(40).nullish(),
        taxMode: z.enum(['INCLUSIVE', 'EXCLUSIVE', 'NOT_CONFIGURED']).nullish(),
        taxAmount: amountSchema.nullish(),
        gstRate: amountSchema.nullish(),
        discount: amountSchema.nullish(),
        taxJurisdiction: z.enum(['INTRA', 'INTER']).nullish(),
        placeOfSupply: z.string().max(80).nullish(),
        originalCurrency: z.string().max(8).nullish(),
        fxRate: amountSchema.nullish(),
        convertedAmount: amountSchema.nullish(),
        returnDirection: z.enum(['CUSTOMER', 'SUPPLIER']).nullish(),
        goodsOutcome: z.enum(['TO_STOCK', 'NOT_RETURNED', 'PENDING_INSPECTION']).nullish(),
        returnReason: z.string().max(80).nullish(),
        acceptedQuantity: amountSchema.nullish(),
        productionStatus: z.enum(['PLANNED', 'IN_PROGRESS', 'COMPLETED']).nullish(),
        deliveryStatus: z.enum(['PLANNED', 'DISPATCHED', 'DELIVERED', 'ACCEPTED', 'RETURNED']).nullish(),
      })
      .strict()
      .optional(),
  })
  .strict();

export const decideOperatingEventSchema = z
  .object({
    decision: z.enum(['APPROVED', 'REJECTED']),
    reason: z.string().max(500).optional(),
  })
  .strict();

export type RecordOperatingEventInput = z.infer<typeof recordOperatingEventSchema>;
export type DecideOperatingEventInput = z.infer<typeof decideOperatingEventSchema>;

interface SetupRow {
  family_code: string;
  title: string;
  preferences: OperatingPreferences;
  company_id: string;
}

async function loadSetup(client: PoolClient, momentId: string): Promise<SetupRow | null> {
  const row = await client.query<SetupRow>(
    `SELECT family_code, title, preferences, company_id
     FROM business.business_system_setup
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY updated_at DESC
     LIMIT 1`,
    [momentId]
  );
  return row.rows[0] ?? null;
}

async function loadEvents(client: PoolClient, momentId: string): Promise<OperatingEventRow[]> {
  const rows = await client.query<{
    operating_event_id: string;
    event_type: string;
    title: string;
    note: string | null;
    amount: string | null;
    currency_code: string | null;
    counterparty_name: string | null;
    channel_name: string | null;
    payment_relevance: string;
    payment_status: string;
    approval_status: string;
    submitted_by_user_id: string;
    decided_by_user_id: string | null;
    occurred_at: Date;
    parent_event_id: string | null;
    quantity: string | null;
    payload: Record<string, unknown> | null;
  }>(
    `SELECT operating_event_id, event_type, title, note, amount::text, currency_code,
            counterparty_name, channel_name, payment_relevance, payment_status, approval_status,
            submitted_by_user_id, decided_by_user_id, occurred_at, parent_event_id, quantity::text,
            payload
     FROM business.operating_event
     WHERE moment_id = $1
     ORDER BY occurred_at DESC
     LIMIT 200`,
    [momentId]
  );
  return rows.rows.map((r) => {
    const payload = r.payload ?? {};
    const metric = payload.metric === 'LEADS' || payload.metric === 'CONVERSIONS' ? payload.metric : null;
    const rejected = payload.rejectedQuantity;
    const lines = Array.isArray(payload.lines)
      ? payload.lines.flatMap((line) => {
          if (!line || typeof line !== 'object') return [];
          const rec = line as Record<string, unknown>;
          const itemName = String(rec.itemName ?? '').trim();
          if (!itemName) return [];
          return [{
            itemName,
            quantity: rec.quantity == null ? null : String(rec.quantity),
            unit: rec.unit == null ? null : String(rec.unit),
            unitPrice: rec.unitPrice == null ? null : String(rec.unitPrice),
            masterId: rec.masterId == null ? null : String(rec.masterId),
            baseQuantity: rec.baseQuantity == null ? null : String(rec.baseQuantity),
            discount: rec.discount == null ? null : String(rec.discount),
            taxMode: rec.taxMode === 'INCLUSIVE' || rec.taxMode === 'EXCLUSIVE' ? rec.taxMode : null,
            gstRate: rec.gstRate == null ? null : String(rec.gstRate),
          }];
        })
      : [];
    const masterRaw = payload.master;
    const masterRec = masterRaw && typeof masterRaw === 'object' ? (masterRaw as Record<string, unknown>) : null;
    const master = masterRec
      ? {
          kind: String(masterRec.kind ?? ''),
          name: String(masterRec.name ?? ''),
          unit: masterRec.unit == null ? null : String(masterRec.unit),
          dimension: masterRec.dimension == null ? null : String(masterRec.dimension),
          conversionToBase: masterRec.conversionToBase == null ? null : String(masterRec.conversionToBase),
          tracked: masterRec.tracked === true ? true : masterRec.tracked === false ? false : null,
          barcode: masterRec.barcode == null ? null : String(masterRec.barcode),
          gstApplicable: masterRec.gstApplicable == null ? null : String(masterRec.gstApplicable),
          gstClassification: masterRec.gstClassification == null ? null : String(masterRec.gstClassification),
          gstRate: masterRec.gstRate == null ? null : String(masterRec.gstRate),
          priceIncludesGst: masterRec.priceIncludesGst == null ? null : String(masterRec.priceIncludesGst),
          salePriceIncludesGst: masterRec.salePriceIncludesGst == null ? null : String(masterRec.salePriceIncludesGst),
          purchasePriceIncludesGst: masterRec.purchasePriceIncludesGst == null ? null : String(masterRec.purchasePriceIncludesGst),
          gstEffectiveFrom: masterRec.gstEffectiveFrom == null ? null : String(masterRec.gstEffectiveFrom),
          hsn: masterRec.hsn == null ? null : String(masterRec.hsn),
          gstRegistration: masterRec.gstRegistration == null ? null : String(masterRec.gstRegistration),
          businessState: masterRec.businessState == null ? null : String(masterRec.businessState),
          billingState: masterRec.billingState == null ? null : String(masterRec.billingState),
          deliveryState: masterRec.deliveryState == null ? null : String(masterRec.deliveryState),
          gstin: masterRec.gstin == null ? null : String(masterRec.gstin),
        }
      : null;
    return {
      operatingEventId: r.operating_event_id,
      eventType: r.event_type,
      title: r.title,
      note: r.note,
      amount: r.amount,
      currencyCode: r.currency_code,
      counterpartyName: r.counterparty_name,
      channelName: r.channel_name,
      paymentRelevance: r.payment_relevance,
      paymentStatus: r.payment_status,
      approvalStatus: r.approval_status,
      submittedByUserId: r.submitted_by_user_id,
      decidedByUserId: r.decided_by_user_id,
      occurredAt: r.occurred_at.toISOString(),
      parentEventId: r.parent_event_id,
      quantity: r.quantity,
      unit: payload.unit == null ? lines[0]?.unit ?? null : String(payload.unit),
      lines,
      dueAt: payload.dueAt == null ? null : String(payload.dueAt),
      master: master && master.kind && master.name ? master : null,
      contract: parseOperatingContract(payload.contract),
      metric,
      marketing: parseMarketingFacts(payload, metric),
      rejectedQuantity: rejected == null || rejected === '' ? null : String(rejected),
    };
  });
}

function prefsOf(raw: OperatingPreferences | Record<string, unknown>): OperatingPreferences {
  return raw as OperatingPreferences;
}

function tradingGstContext(prefs: OperatingPreferences) {
  const yesNo = (value: string | undefined): boolean | null => (value === 'YES' ? true : value === 'NO' ? false : null);
  return {
    enabled: prefs.gstTracking === 'YES' && prefs.gstRegistration === 'REGULAR',
    businessState: prefs.gstState ?? '',
    saleIncludesGst: yesNo(prefs.salePriceIncludesGst),
    purchaseIncludesGst: yesNo(prefs.purchasePriceIncludesGst),
    registration: prefs.gstRegistration ?? '',
  };
}

export async function viewerRole(
  client: PoolClient,
  userId: string,
  _momentId: string,
  companyId: string | null,
  prefs: OperatingPreferences,
  knownMembership?: string | null
): Promise<OperatingRole> {
  if (!companyId) {
    return resolveRole(prefs.participants ?? [], userId, 'MEMBER');
  }
  if (knownMembership) {
    return resolveRole(prefs.participants ?? [], userId, knownMembership);
  }
  const membership = await client.query<{ membership_type: string }>(
    `SELECT membership_type FROM business.company_membership
     WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
    [companyId, userId]
  );
  return resolveRole(prefs.participants ?? [], userId, membership.rows[0]?.membership_type ?? 'MEMBER');
}

export async function getOperatingProjection(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<OperatingProjection | { retired: true; familyCode: string; title: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  if (isLegacyBusinessFamily(scope.businessFamily) || !isOperatingFamily(scope.businessFamily)) {
    const title = await client.query<{ title: string }>(
      `SELECT title FROM core.moment WHERE moment_id = $1`,
      [momentId]
    );
    return {
      retired: true,
      familyCode: scope.businessFamily,
      title: title.rows[0]?.title ?? 'Business moment',
    };
  }
  const setup = await loadSetup(client, momentId);
  const prefs = setup ? prefsOf(setup.preferences) : null;
  if (!prefs?.subtype) {
    return { retired: true, familyCode: scope.businessFamily, title: 'Business moment' };
  }
  const role = await viewerRole(client, ctx.userId, momentId, scope.companyId, prefs, scope.membershipType);
  const events = await loadEvents(client, momentId);
  return projectOperatingMoment({
    familyCode: scope.businessFamily,
    prefs,
    role,
    events,
  });
}

export async function recordOperatingEvent(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: RecordOperatingEventInput,
  idempotencyKey?: string
): Promise<{ eventId: string; approvalStatus: string; projection: OperatingProjection }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  if (!isOperatingFamily(scope.businessFamily)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Quick Add is only available on an operating moment.', 400);
  }
  const setup = await loadSetup(client, momentId);
  if (!setup) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating setup was not found.', 400);
  }
  const prefs = prefsOf(setup.preferences);
  const postedType = body.eventType.toUpperCase() === 'SALE' && prefs.subtype === 'CLIENT_CAMPAIGN' ? 'INVOICE' : body.eventType;
  const def = quickAddDef(postedType);
  const allowed = def && (
    familyForSubtype(prefs.subtype)?.subtypes.find((s) => s.code === prefs.subtype)?.quickAdds.includes(def.code)
    || (prefs.subtype === 'CLIENT_CAMPAIGN' && body.eventType.toUpperCase() === 'SALE')
  );
  if (!def || !allowed) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `This moment does not include ${body.eventType}.`, 400);
  }
  const role = await viewerRole(client, ctx.userId, momentId, scope.companyId, prefs, scope.membershipType);
  if (!canSubmit(role, def)) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Your role cannot add this.', 403);
  }
  const quantityCoversAmount =
    Boolean(body.quantity) &&
    (def.code === 'RESULT' ||
      def.code === 'PROGRESS' ||
      def.code === 'TARGET' ||
      def.code === 'ORDER' ||
      def.code === 'SHIPMENT' ||
      def.code === 'DELIVERY' ||
      def.code === 'PRODUCTION');
  if (def.needsAmount && !body.amount && !quantityCoversAmount && !(body.lines?.length) && def.code !== 'UPDATE') {
    if (def.consumesBudget || def.countsTarget || def.code === 'BUDGET' || def.code === 'TARGET' || def.code === 'PAYMENT' || def.code === 'COLLECTION') {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Amount is required.', 400);
    }
  }
  const checker = needsMakerChecker(role, def);
  const approvalStatus = checker || def.code === 'APPROVAL' ? 'PENDING' : 'NONE';
  let paymentStatus: 'NONE' | 'PENDING' | 'READY' | 'PAID' | 'COLLECTED' = body.paymentStatus ?? 'NONE';
  if (def.paymentRelevance !== 'NONE' && paymentStatus === 'NONE') {
    paymentStatus = 'PENDING';
  }
  if (def.paymentRelevance === 'NONE') paymentStatus = 'NONE';
  const lineTotal = (body.lines ?? []).reduce(
    (total, line) => total + Number(line.quantity ?? 1) * Number(line.unitPrice ?? 0),
    0
  );
  let amount = body.amount ?? (lineTotal > 0 ? lineTotal.toFixed(4) : null);
  const existing = await loadEvents(client, momentId);
  const idempotency = (body.idempotencyKey ?? idempotencyKey ?? '').trim();
  if (idempotency && isMarketingSubtype(prefs.subtype)) {
    const prior = existing.find((row) => row.marketing?.idempotencyKey === idempotency);
    if (prior) {
      const projection = await getOperatingProjection(client, ctx, momentId);
      if ('retired' in projection && projection.retired) {
        throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
      }
      return { eventId: prior.operatingEventId, approvalStatus: prior.approvalStatus, projection: projection as OperatingProjection };
    }
  }
  const retailGoverned =
    isRetailSubtype(prefs.subtype) &&
    ['SALE', 'SERVICE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'STOCK', 'ORDER', 'APPOINTMENT'].includes(def.code);
  const tradingGoverned =
    isTradingSubtype(prefs.subtype) &&
    ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'STOCK', 'ORDER', 'SHIPMENT', 'DELIVERY', 'RETURN'].includes(
      def.code
    );
  const manufacturingGoverned =
    isManufacturingSubtype(prefs.subtype) &&
    ['PRODUCTION', 'PURCHASE', 'SPEND', 'SALE', 'CUSTOMER_ORDER', 'DELIVERY', 'PAYMENT', 'COLLECTION', 'STOCK'].includes(def.code);
  let retailContract: OperatingContract | null = null;
  let settleChild: { type: 'COLLECTION' | 'PAYMENT'; amount: string } | null = null;
  let parentEventId = body.parentEventId ?? null;
  let quantity = body.quantity ?? null;
  if (retailGoverned || tradingGoverned || manufacturingGoverned) {
    const writeContract = parseOperatingContract(body.contract) ?? undefined;
    const checked = retailGoverned
      ? validateRetailWrite(
          {
            eventType: def.code,
            title: body.title,
            amount,
            counterpartyName: body.counterpartyName,
            channelName: body.channelName,
            occurredAt: body.occurredAt,
            parentEventId,
            quantity: body.quantity,
            dueAt: body.dueAt,
            lines: body.lines,
            contract: writeContract,
          },
          existing
        )
      : tradingGoverned
        ? validateTradingWrite(
          {
            eventType: def.code,
            title: body.title,
            amount,
            counterpartyName: body.counterpartyName,
            channelName: body.channelName,
            occurredAt: body.occurredAt,
            parentEventId,
            quantity: body.quantity,
            dueAt: body.dueAt,
            lines: body.lines,
            contract: writeContract,
            channelTracking: (prefs.channels?.length ?? 0) > 0,
            gst: tradingGstContext(prefs),
          },
          existing
        )
        : validateManufacturingWrite(
          {
            eventType: def.code,
            title: body.title,
            amount,
            counterpartyName: body.counterpartyName,
            channelName: body.channelName,
            occurredAt: body.occurredAt,
            parentEventId,
            quantity: body.quantity,
            dueAt: body.dueAt,
            lines: body.lines,
            contract: writeContract,
            unit: body.unit,
            rejectedQuantity: body.rejectedQuantity,
          },
          existing
        );
    if (!checked.ok) throw new AppError(ErrorCode.VALIDATION_FAILED, checked.message, 400);
    paymentStatus = checked.paymentStatus as typeof paymentStatus;
    amount = checked.amount;
    parentEventId = checked.parentEventId;
    quantity = checked.quantity;
    retailContract = checked.contract;
    settleChild = checked.settleChild;
    if (retailContract.clientKey) {
      const prior = await client.query<{ operating_event_id: string; approval_status: string }>(
        `SELECT operating_event_id, approval_status
         FROM business.operating_event
         WHERE moment_id = $1 AND payload->>'clientKey' = $2
         LIMIT 1`,
        [momentId, retailContract.clientKey]
      );
      const found = prior.rows[0];
      if (found) {
        const projection = await getOperatingProjection(client, ctx, momentId);
        if ('retired' in projection && projection.retired) {
          throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
        }
        return { eventId: found.operating_event_id, approvalStatus: found.approval_status, projection: projection as OperatingProjection };
      }
    }
  } else {
    const parent = parentEventId ? existing.find((row) => row.operatingEventId === parentEventId) ?? null : null;
    if (parentEventId && !parent) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Choose a record on this moment.', 400);
    }
    const wanted = def.code === 'PAYMENT' || def.code === 'COLLECTION'
      ? (def.code === 'COLLECTION' ? ['SALE', 'SERVICE', 'CUSTOMER_ORDER', 'INVOICE'] : ['PURCHASE', 'SPEND', 'VENDOR_COST'])
      : [];
    const openParent = existing.some(
      (row) =>
        wanted.includes(row.eventType) &&
        row.approvalStatus !== 'REJECTED' &&
        row.paymentStatus !== 'PAID' &&
        row.paymentStatus !== 'COLLECTED' &&
        row.paymentStatus !== 'SETTLED' &&
        row.paymentStatus !== 'CANCELLED'
    );
    const allocation = validateOperatingAllocation({
      eventType: def.code,
      parent,
      siblings: parent ? existing.filter((row) => row.parentEventId === parent.operatingEventId) : [],
      amount,
      quantity: body.quantity ?? null,
      allowUnlinked: !openParent,
    });
    if (!allocation.ok) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, allocation.message, 400);
    }
    if (isMarketingSubtype(prefs.subtype)) {
      const checked = validateMarketingWrite(
        {
          eventType: def.code === 'INVOICE' && body.eventType.toUpperCase() === 'SALE' ? 'INVOICE' : def.code,
          subtype: prefs.subtype,
          amount,
          quantity: body.quantity ?? null,
          unit: body.unit ?? null,
          parentEventId,
          metricCode: body.metricCode ?? body.metric ?? null,
          reportingMode: body.reportingMode ?? null,
          revisionKind: body.revisionKind ?? null,
          activityStatus: body.activityStatus ?? null,
          attributedSales: body.attributedSales ?? null,
          costBasis: body.costBasis ?? null,
          creativeVersion: body.creativeVersion ?? null,
          idempotencyKey: idempotency || null,
        },
        existing
      );
      if (!checked.ok) throw new AppError(ErrorCode.VALIDATION_FAILED, checked.message, 400);
    }
    const lineQuantity = body.lines?.reduce((total, line) => total + Number(line.quantity ?? 0), 0) ?? 0;
    quantity = body.quantity ?? (lineQuantity > 0 ? lineQuantity.toFixed(4) : null);
  }
  const payload: Record<string, unknown> = {};
  if (body.metric) payload.metric = body.metric;
  if (body.metricCode) payload.metricCode = body.metricCode;
  if (body.reportingMode) payload.reportingMode = body.reportingMode;
  if (body.budgetCategory) payload.budgetCategory = body.budgetCategory;
  if (body.paymentState) payload.paymentState = body.paymentState;
  if (body.activityStatus) payload.activityStatus = body.activityStatus;
  if (body.attributedSales) payload.attributedSales = body.attributedSales;
  if (body.revisionKind) payload.revisionKind = body.revisionKind;
  if (body.effectiveAt) payload.effectiveAt = body.effectiveAt;
  if (body.effectiveFrom || body.effectiveAt) payload.effectiveFrom = (body.effectiveFrom ?? body.effectiveAt ?? '').slice(0, 10);
  if (body.costBasis) payload.costBasis = body.costBasis;
  if (body.overlapDeduped) payload.overlapDeduped = true;
  if (body.resultSource) payload.resultSource = body.resultSource;
  if (body.periodEnd) payload.periodEnd = body.periodEnd.slice(0, 10);
  if (body.creativeVersion) payload.creativeVersion = body.creativeVersion;
  if (body.evidenceUrl) payload.evidenceUrl = body.evidenceUrl;
  if (body.plannedStart) payload.plannedStart = body.plannedStart;
  if (body.blockedById) payload.blockedById = body.blockedById;
  if (body.bookingReference) payload.bookingReference = body.bookingReference;
  if (body.deliverableType) payload.deliverableType = body.deliverableType;
  if (body.repeatRule) payload.repeatRule = body.repeatRule;
  if (idempotency) payload.idempotencyKey = idempotency;
  if (isMarketingSubtype(prefs.subtype) && (def.code === 'RESULT' || def.code === 'PROGRESS') && (body.reportingMode ?? 'INCREMENTAL') === 'CUMULATIVE') {
    const delta = observationDelta(existing, {
      operatingEventId: 'next',
      eventType: def.code,
      title: body.title,
      amount,
      quantity,
      unit: body.unit ?? null,
      channelName: body.channelName ?? null,
      paymentStatus,
      approvalStatus,
      occurredAt: body.occurredAt ?? new Date().toISOString(),
      metric: body.metric ?? null,
      marketing: {
        reportingMode: 'CUMULATIVE',
        metricCode: body.metricCode ?? body.metric ?? null,
        budgetCategory: null,
        paymentState: null,
        activityStatus: null,
        attributedSales: null,
        revisionKind: null,
        resultSource: body.resultSource ?? null,
      },
    });
    payload.normalizedDelta = delta.toFixed(4);
  }
  if (body.rejectedQuantity) payload.rejectedQuantity = body.rejectedQuantity;
  if (body.lines?.length) payload.lines = body.lines;
  if (body.unit) payload.unit = body.unit;
  if (body.dueAt) payload.dueAt = body.dueAt;
  if (body.master) {
    const dimension = body.master.dimension ?? (body.master.unit ? uomDimension(body.master.unit) : null);
    payload.master = dimension ? { ...body.master, dimension } : body.master;
  }
  if (retailContract) {
    payload.contract = retailContract;
    if (retailContract.clientKey) payload.clientKey = retailContract.clientKey;
  }
  const title = body.master?.name || body.title;
  const inserted = await client.query<{ operating_event_id: string }>(
    `INSERT INTO business.operating_event (
       moment_id, company_id, actor_user_id, event_type, title, note, amount, currency_code,
       counterparty_name, channel_name, payment_relevance, payment_status, approval_status,
       submitted_by_user_id, occurred_at, parent_event_id, quantity, payload
     ) VALUES (
       $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$3,
       COALESCE($14::timestamptz, now()), $15, $16, $17::jsonb
     )
     RETURNING operating_event_id`,
    [
      momentId,
      scope.companyId,
      ctx.userId,
      def.code,
      title,
      body.note ?? null,
      amount,
      prefs.currency,
      body.counterpartyName ?? null,
      body.channelName ?? null,
      def.paymentRelevance,
      paymentStatus,
      approvalStatus,
      body.occurredAt ?? null,
      parentEventId,
      quantity,
      JSON.stringify(payload),
    ]
  );
  const eventId = inserted.rows[0]!.operating_event_id;
  const companyId = scope.companyId;
  if (companyId) {
    if (settleChild && approvalStatus !== 'PENDING') {
      await ensureSettlement(client, ctx, momentId, companyId, prefs.currency, {
        eventId,
        eventType: def.code,
        title,
        amount: settleChild.amount,
        paymentStatus: settleChild.type === 'COLLECTION' ? 'COLLECTED' : 'PAID',
        approvalStatus,
        counterpartyName: body.counterpartyName ?? null,
        occurredAt: body.occurredAt ?? null,
      });
    }
    await ensureSettlement(client, ctx, momentId, companyId, prefs.currency, {
      eventId,
      eventType: def.code,
      title,
      amount,
      paymentStatus,
      approvalStatus,
      counterpartyName: body.counterpartyName ?? null,
      occurredAt: body.occurredAt ?? null,
    });
  }
  if (approvalStatus !== 'PENDING' && (def.code === 'BUDGET' || def.code === 'TARGET') && body.amount) {
    await applyPlanRevision(client, momentId, def.code, body.amount);
  }
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'OperatingEventRecorded',
    domainCode: 'BUSINESS',
    aggregateType: 'OPERATING_EVENT',
    aggregateId: eventId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { eventId, eventType: def.code, momentId, approvalStatus },
  });
  await insertAudit(client, ctx, 'OPERATING_EVENT_RECORD', 'OPERATING_EVENT', eventId, domainEventId, {
    momentId,
    eventType: def.code,
    approvalStatus,
  });
  const projection = await getOperatingProjection(client, ctx, momentId);
  if ('retired' in projection && projection.retired) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
  }
  return { eventId, approvalStatus, projection: projection as OperatingProjection };
}

async function applyPlanRevision(
  client: PoolClient,
  momentId: string,
  code: 'BUDGET' | 'TARGET',
  amount: string
): Promise<void> {
  const key = code === 'BUDGET' ? 'monthlyBudget' : 'monthlyTarget';
  await client.query(
    `UPDATE business.business_system_setup
     SET preferences = jsonb_set(preferences, $2::text[], to_jsonb($3::text), true),
         version = version + 1,
         updated_at = now()
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId, `{${key}}`, amount]
  );
}

export async function decideOperatingEvent(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  eventId: string,
  body: DecideOperatingEventInput
): Promise<{ eventId: string; approvalStatus: string }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const setup = await loadSetup(client, momentId);
  if (!setup || !isOperatingFamily(scope.businessFamily)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating setup was not found.', 400);
  }
  const prefs = prefsOf(setup.preferences);
  const role = await viewerRole(client, ctx.userId, momentId, scope.companyId, prefs, scope.membershipType);
  if (!canApprove(role)) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Your role cannot approve this.', 403);
  }
  const existing = await client.query<{
    submitted_by_user_id: string;
    approval_status: string;
    event_type: string;
    amount: string | null;
  }>(
    `SELECT submitted_by_user_id, approval_status, event_type, amount::text
     FROM business.operating_event
     WHERE operating_event_id = $1 AND moment_id = $2`,
    [eventId, momentId]
  );
  const row = existing.rows[0];
  if (!row) throw new AppError(ErrorCode.VALIDATION_FAILED, 'Event was not found.', 404);
  if (row.approval_status !== 'PENDING') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'This event is not waiting for approval.', 400);
  }
  if (row.submitted_by_user_id === ctx.userId) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'You cannot approve an action you submitted.', 403);
  }
  await client.query(
    `UPDATE business.operating_event
     SET approval_status = $2, decided_by_user_id = $3, version = version + 1
     WHERE operating_event_id = $1`,
    [eventId, body.decision, ctx.userId]
  );
  if (body.decision === 'APPROVED' && (row.event_type === 'BUDGET' || row.event_type === 'TARGET') && row.amount) {
    await applyPlanRevision(client, momentId, row.event_type, row.amount);
  }
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'OperatingEventDecided',
    domainCode: 'BUSINESS',
    aggregateType: 'OPERATING_EVENT',
    aggregateId: eventId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { eventId, decision: body.decision, momentId },
  });
  await insertAudit(client, ctx, 'OPERATING_EVENT_DECIDE', 'OPERATING_EVENT', eventId, domainEventId, {
    decision: body.decision,
    reason: body.reason ?? null,
  });
  return { eventId, approvalStatus: body.decision };
}

export async function reviseOperatingEvent(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  eventId: string,
  body: RecordOperatingEventInput
): Promise<{ eventId: string; projection: OperatingProjection }> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  const setup = await loadSetup(client, momentId);
  if (!setup || !isOperatingFamily(scope.businessFamily)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating setup was not found.', 400);
  }
  const prefs = prefsOf(setup.preferences);
  const role = await viewerRole(client, ctx.userId, momentId, scope.companyId, prefs, scope.membershipType);
  const existing = await loadEvents(client, momentId);
  const current = existing.find((row) => row.operatingEventId === eventId);
  if (!current) throw new AppError(ErrorCode.VALIDATION_FAILED, 'Event was not found.', 404);
  const def = quickAddDef(current.eventType);
  if (!def || !canSubmit(role, def)) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Your role cannot edit this.', 403);
  }
  const payload: Record<string, unknown> = {};
  if (body.lines?.length) payload.lines = body.lines;
  if (body.unit) payload.unit = body.unit;
  if (body.dueAt) payload.dueAt = body.dueAt;
  if (body.metric) payload.metric = body.metric;
  if (body.metricCode) payload.metricCode = body.metricCode;
  if (body.reportingMode) payload.reportingMode = body.reportingMode;
  if (body.revisionKind) payload.revisionKind = body.revisionKind;
  if (body.activityStatus) payload.activityStatus = body.activityStatus;
  let revisedAmount = body.amount ?? null;
  let revisedStatus: 'NONE' | 'PENDING' | 'READY' | 'PAID' | 'COLLECTED' | null = body.paymentStatus ?? null;
  let revisedQuantity = body.quantity ?? null;
  if (
    isRetailSubtype(prefs.subtype) &&
    (body.contract || current.contract) &&
    ['SALE', 'SERVICE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'STOCK', 'ORDER', 'APPOINTMENT'].includes(current.eventType)
  ) {
    const checked = validateRetailWrite(
      {
        eventType: current.eventType,
        title: body.title,
        amount: body.amount ?? current.amount,
        counterpartyName: body.counterpartyName ?? current.counterpartyName,
        channelName: body.channelName ?? current.channelName,
        occurredAt: body.occurredAt ?? current.occurredAt,
        parentEventId: body.parentEventId ?? current.parentEventId,
        quantity: body.quantity ?? current.quantity,
        dueAt: body.dueAt ?? current.dueAt,
        lines: body.lines ?? current.lines,
        contract: parseOperatingContract(body.contract) ?? current.contract ?? undefined,
        revisingId: eventId,
      },
      existing
    );
    if (!checked.ok) throw new AppError(ErrorCode.VALIDATION_FAILED, checked.message, 400);
    revisedAmount = checked.amount;
    revisedStatus = checked.paymentStatus as typeof revisedStatus;
    revisedQuantity = checked.quantity;
    payload.contract = checked.contract;
  } else if (
    isTradingSubtype(prefs.subtype) &&
    (body.contract || current.contract) &&
    ['SALE', 'PURCHASE', 'SPEND', 'PAYMENT', 'COLLECTION', 'STOCK', 'ORDER', 'SHIPMENT', 'DELIVERY', 'RETURN'].includes(
      current.eventType
    )
  ) {
    const checked = validateTradingWrite(
      {
        eventType: current.eventType,
        title: body.title,
        amount: body.amount ?? current.amount,
        counterpartyName: body.counterpartyName ?? current.counterpartyName,
        channelName: body.channelName ?? current.channelName,
        occurredAt: body.occurredAt ?? current.occurredAt,
        parentEventId: body.parentEventId ?? current.parentEventId,
        quantity: body.quantity ?? current.quantity,
        dueAt: body.dueAt ?? current.dueAt,
        lines: body.lines ?? current.lines,
        contract: parseOperatingContract(body.contract) ?? current.contract ?? undefined,
        revisingId: eventId,
        channelTracking: (prefs.channels?.length ?? 0) > 0,
        gst: tradingGstContext(prefs),
      },
      existing
    );
    if (!checked.ok) throw new AppError(ErrorCode.VALIDATION_FAILED, checked.message, 400);
    revisedAmount = checked.amount;
    revisedStatus = checked.paymentStatus as typeof revisedStatus;
    revisedQuantity = checked.quantity;
    payload.contract = checked.contract;
  } else if (
    isManufacturingSubtype(prefs.subtype) &&
    ['PRODUCTION', 'PURCHASE', 'SPEND', 'SALE', 'CUSTOMER_ORDER', 'DELIVERY', 'PAYMENT', 'COLLECTION', 'STOCK'].includes(current.eventType)
  ) {
    const checked = validateManufacturingWrite(
      {
        eventType: current.eventType,
        title: body.title,
        amount: body.amount ?? current.amount,
        counterpartyName: body.counterpartyName ?? current.counterpartyName,
        channelName: body.channelName ?? current.channelName,
        occurredAt: body.occurredAt ?? current.occurredAt,
        parentEventId: body.parentEventId ?? current.parentEventId,
        quantity: body.quantity ?? current.quantity,
        dueAt: body.dueAt ?? current.dueAt,
        lines: body.lines ?? current.lines,
        contract: parseOperatingContract(body.contract) ?? current.contract ?? undefined,
        unit: body.unit ?? current.unit,
        rejectedQuantity: body.rejectedQuantity ?? current.rejectedQuantity,
      },
      existing.filter((row) => row.operatingEventId !== eventId)
    );
    if (!checked.ok) throw new AppError(ErrorCode.VALIDATION_FAILED, checked.message, 400);
    revisedAmount = checked.amount;
    revisedStatus = checked.paymentStatus as typeof revisedStatus;
    revisedQuantity = checked.quantity;
    payload.contract = checked.contract;
    if (body.rejectedQuantity) payload.rejectedQuantity = body.rejectedQuantity;
  }
  const lineQuantity = body.lines?.reduce((total, line) => total + Number(line.quantity ?? 0), 0) ?? 0;
  await client.query(
    `UPDATE business.operating_event
     SET title = $3,
         note = $4,
         amount = $5,
         counterparty_name = $6,
         channel_name = $7,
         payment_status = COALESCE($8, payment_status),
         quantity = $9,
         payload = COALESCE(payload, '{}'::jsonb) || $10::jsonb,
         occurred_at = COALESCE($11::timestamptz, occurred_at),
         parent_event_id = COALESCE($12, parent_event_id),
         version = version + 1
     WHERE operating_event_id = $1 AND moment_id = $2`,
    [
      eventId,
      momentId,
      body.title,
      body.note ?? null,
      revisedAmount,
      body.counterpartyName ?? null,
      body.channelName ?? null,
      revisedStatus,
      revisedQuantity ?? (lineQuantity > 0 ? lineQuantity.toFixed(4) : null),
      JSON.stringify(payload),
      body.occurredAt ?? null,
      body.parentEventId ?? null,
    ]
  );
  const { domainEventId } = await insertDomainEventAndOutbox(client, ctx, {
    eventName: 'OperatingEventRevised',
    domainCode: 'BUSINESS',
    aggregateType: 'OPERATING_EVENT',
    aggregateId: eventId,
    scopeType: 'MOMENT',
    scopeId: momentId,
    payload: { eventId, momentId },
  });
  await insertAudit(client, ctx, 'OPERATING_EVENT_REVISE', 'OPERATING_EVENT', eventId, domainEventId, {
    momentId,
    eventType: current.eventType,
  });
  if (scope.companyId) {
    await ensureSettlement(client, ctx, momentId, scope.companyId, prefs.currency, {
      eventId,
      eventType: current.eventType,
      title: body.title,
      amount: revisedAmount ?? current.amount,
      paymentStatus: revisedStatus ?? current.paymentStatus,
      approvalStatus: current.approvalStatus,
      counterpartyName: body.counterpartyName ?? current.counterpartyName,
      occurredAt: body.occurredAt ?? current.occurredAt,
    });
  }
  const projection = await getOperatingProjection(client, ctx, momentId);
  if ('retired' in projection && projection.retired) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Operating projection unavailable.', 400);
  }
  return { eventId, projection: projection as OperatingProjection };
}

async function ensureSettlement(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  companyId: string,
  currency: string,
  source: {
    eventId: string;
    eventType: string;
    title: string;
    amount: string | null;
    paymentStatus: string;
    approvalStatus: string;
    counterpartyName: string | null;
    occurredAt: string | null;
  }
): Promise<void> {
  if (source.approvalStatus === 'PENDING' || source.approvalStatus === 'REJECTED' || !source.amount) return;
  const childType =
    (source.eventType === 'SALE' || source.eventType === 'SERVICE') && source.paymentStatus === 'COLLECTED'
      ? 'COLLECTION'
      : (source.eventType === 'PURCHASE' || source.eventType === 'SPEND' || source.eventType === 'VENDOR_COST') &&
          source.paymentStatus === 'PAID'
        ? 'PAYMENT'
        : null;
  if (!childType) return;
  const found = await client.query(
    `SELECT 1 FROM business.operating_event
     WHERE moment_id = $1 AND parent_event_id = $2 AND event_type = $3
     LIMIT 1`,
    [momentId, source.eventId, childType]
  );
  if (found.rowCount) return;
  await client.query(
    `INSERT INTO business.operating_event (
       moment_id, company_id, actor_user_id, event_type, title, note, amount, currency_code,
       counterparty_name, channel_name, payment_relevance, payment_status, approval_status,
       submitted_by_user_id, occurred_at, parent_event_id, quantity, payload
     ) VALUES (
       $1,$2,$3,$4,$5,NULL,$6,$7,$8,NULL,$9,$10,'NONE',$3,
       COALESCE($11::timestamptz, now()), $12, NULL, '{}'::jsonb
     )`,
    [
      momentId,
      companyId,
      ctx.userId,
      childType,
      childType === 'COLLECTION' ? `Collected · ${source.title}` : `Paid · ${source.title}`,
      source.amount,
      currency,
      source.counterpartyName,
      childType === 'COLLECTION' ? 'RECEIVABLE' : 'PAYABLE',
      childType === 'COLLECTION' ? 'COLLECTED' : 'PAID',
      source.occurredAt,
      source.eventId,
    ]
  );
}

export function assertOperatingActivate(familyCode: string): void {
  if (isLegacyBusinessFamily(familyCode)) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Team & Work, Money & Cash Flow, and Daily Business are retired. Create a Marketing, Trading, Retail, or Manufacturing moment.',
      400
    );
  }
  if (!isOperatingFamily(familyCode)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown business setup: ${familyCode}`, 400);
  }
}

export async function insertReportingCadenceActivity(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  companyId: string,
  prefs: OperatingPreferences
): Promise<void> {
  if (prefs.subtype !== 'CLIENT_CAMPAIGN' || !prefs.reportingCadence) return;
  const start = /^\d{4}-\d{2}-\d{2}$/.test(prefs.startDate) ? prefs.startDate : new Date().toISOString().slice(0, 10);
  const shift = prefs.reportingCadence === 'WEEKLY' ? 7 : prefs.reportingCadence === 'MONTHLY' ? 30 : 14;
  const due = new Date(`${start}T00:00:00Z`);
  due.setUTCDate(due.getUTCDate() + shift);
  const dueAt = due.toISOString().slice(0, 10);
  await client.query(
    `INSERT INTO business.operating_event (
       moment_id, company_id, actor_user_id, event_type, title, note, currency_code,
       payment_relevance, payment_status, approval_status, submitted_by_user_id, occurred_at, payload
     ) VALUES (
       $1,$2,$3,'ACTIVITY',$4,$5,$6,'NONE','NONE','NONE',$3, now(), $7::jsonb
     )`,
    [
      momentId,
      companyId,
      ctx.userId,
      'Client report',
      `${prefs.reportingCadence.toLowerCase()} reporting reminder. This does not submit a report.`,
      prefs.currency || 'INR',
      JSON.stringify({ activityStatus: 'OPEN', dueAt, reportingCadence: prefs.reportingCadence }),
    ]
  );
}

export function momentTypeForActivate(familyCode: string, requested: string | undefined, subtype: string): string {
  const code = (requested ?? subtype).toUpperCase();
  if (!isOperatingSubtype(code) || familyForSubtype(code)?.familyCode !== familyCode) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Subtype does not belong to this moment.', 400);
  }
  return code;
}

export { normalizeOperatingPreferences };
