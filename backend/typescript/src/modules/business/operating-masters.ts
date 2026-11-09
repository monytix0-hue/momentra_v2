/**
 * Stable master data for operating Business Moments.
 *
 * A master is company-scoped (`moment_id` null) or moment-scoped, and is the only
 * place a product, party, location, or route gets an identity. Events reference it
 * through `payload.lines[].masterId`; the operating projection reads masters from
 * here first and only falls back to legacy event-derived masters so moments created
 * before V099 keep loading.
 *
 * Prices on a master are the standing rate. Effective-dated buyer rates live on
 * `business.operating_price_list` + `..._line`.
 */

import type { PoolClient } from 'pg';
import { z } from 'zod';
import { gstStateName } from './operating-gst-states';
import { HSN_FORMAT_MESSAGE, isValidHsnCode } from './operating-hsn';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';

export const OPERATING_MASTER_KINDS = [
  'SKU',
  'PRODUCT',
  'MATERIAL',
  'CONSUMABLE',
  'SUPPLIER',
  'BUYER',
  'CUSTOMER',
  'PRINCIPAL',
  'DEALER',
  'CHANNEL',
  'LOCATION',
  'WAREHOUSE',
  'ROUTE',
  'EXPENSE_CATEGORY',
  'UOM',
  'BATCH',
  'PRICE_LIST',
  'SERVICE',
  'STAFF',
  'STATION',
  'METRIC',
  'BUDGET_CATEGORY',
  'PARTY',
  'VENDOR',
  'MENU_DISH',
] as const;

export type OperatingMasterKind = (typeof OPERATING_MASTER_KINDS)[number];

export const OPERATING_MASTER_STATUSES = ['ACTIVE', 'INACTIVE', 'ARCHIVED'] as const;
export type OperatingMasterStatus = (typeof OPERATING_MASTER_STATUSES)[number];

const priceSchema = z.string().regex(/^\d+(\.\d{1,4})?$/, 'Use a number with up to 4 decimals.');
const percentSchema = z.string().regex(/^\d{1,3}(\.\d{1,2})?$/, 'Use a percentage with up to 2 decimals.');
/**
 * A state may arrive as a full name or as the two-digit GST code. A known spelling is stored
 * canonically so every jurisdiction compare sees one form.
 *
 * An unknown value is kept trimmed rather than rejected: master rows written before the state
 * registry existed can hold arbitrary text, and a PATCH echoes the row back. Rejecting here would
 * make an unrelated edit to such a row fail.
 */
const stateSchema = z
  .string()
  .max(80)
  .transform((value) => gstStateName(value) || value.trim());
/** HSN is 2, 4, 6 or 8 digits; blank is allowed until the operator knows it. */
const hsnSchema = z
  .string()
  .max(16)
  .refine((value) => isValidHsnCode(value), { message: HSN_FORMAT_MESSAGE });

const masterFields = {
  kind: z.enum(OPERATING_MASTER_KINDS),
  name: z.string().min(1).max(160),
  code: z.string().max(60).nullish(),
  momentId: z.string().uuid().nullish(),
  unit: z.string().max(24).nullish(),
  dimension: z.enum(['count', 'mass', 'volume', 'length', 'area']).nullish(),
  conversionToBase: priceSchema.nullish(),
  tracked: z.boolean().nullish(),
  barcode: z.string().max(80).nullish(),
  sellPrice: priceSchema.nullish(),
  mrp: priceSchema.nullish(),
  purchasePrice: priceSchema.nullish(),
  hsn: hsnSchema.nullish(),
  gstApplicable: z.enum(['YES', 'NO', 'NOT_SURE']).nullish(),
  gstClassification: z.enum(['TAXABLE', 'EXEMPT', 'NIL', 'NON_GST']).nullish(),
  gstRate: percentSchema.nullish(),
  priceIncludesGst: z.enum(['YES', 'NO']).nullish(),
  salePriceIncludesGst: z.enum(['YES', 'NO']).nullish(),
  purchasePriceIncludesGst: z.enum(['YES', 'NO']).nullish(),
  gstEffectiveFrom: z.string().max(10).nullish(),
  gstin: z.string().max(20).nullish(),
  businessState: stateSchema.nullish(),
  billingState: stateSchema.nullish(),
  deliveryState: stateSchema.nullish(),
  parentMasterId: z.string().uuid().nullish(),
  payload: z.record(z.string(), z.unknown()).optional(),
} as const;

export const createOperatingMasterSchema = z.object(masterFields).strict();

export const updateOperatingMasterSchema = z
  .object({
    ...masterFields,
    kind: masterFields.kind.optional(),
    name: masterFields.name.optional(),
    status: z.enum(OPERATING_MASTER_STATUSES).optional(),
    expectedVersion: z.number().int().positive(),
  })
  .strict();

export const archiveOperatingMasterSchema = z
  .object({ expectedVersion: z.number().int().positive() })
  .strict();

const priceListLineSchema = z
  .object({
    masterId: z.string().uuid(),
    price: priceSchema,
    schemePct: percentSchema.nullish(),
  })
  .strict();

export const createPriceListSchema = z
  .object({
    name: z.string().min(1).max(160),
    momentId: z.string().uuid().nullish(),
    channel: z.string().max(120).nullish(),
    priceUnit: z.string().max(24).nullish(),
    marginPct: percentSchema.nullish(),
    schemePct: percentSchema.nullish(),
    gstRate: percentSchema.nullish(),
    validFrom: z.string().max(10),
    validTo: z.string().max(10).nullish(),
    warehouseMasterId: z.string().uuid().nullish(),
    minOrder: priceSchema.nullish(),
    lines: z.array(priceListLineSchema).max(200).optional(),
    payload: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

export const updatePriceListSchema = z
  .object({
    name: z.string().min(1).max(160).optional(),
    channel: z.string().max(120).nullish(),
    priceUnit: z.string().max(24).nullish(),
    marginPct: percentSchema.nullish(),
    schemePct: percentSchema.nullish(),
    gstRate: percentSchema.nullish(),
    validFrom: z.string().max(10).optional(),
    validTo: z.string().max(10).nullish(),
    warehouseMasterId: z.string().uuid().nullish(),
    minOrder: priceSchema.nullish(),
    status: z.enum(OPERATING_MASTER_STATUSES).optional(),
    lines: z.array(priceListLineSchema).max(200).optional(),
    expectedVersion: z.number().int().positive(),
  })
  .strict();

export type CreateOperatingMasterInput = z.infer<typeof createOperatingMasterSchema>;
export type UpdateOperatingMasterInput = z.infer<typeof updateOperatingMasterSchema>;
export type CreatePriceListInput = z.infer<typeof createPriceListSchema>;
export type UpdatePriceListInput = z.infer<typeof updatePriceListSchema>;

export interface OperatingMasterRecord {
  id: string;
  companyId: string;
  momentId: string | null;
  kind: string;
  name: string;
  code: string | null;
  unit: string | null;
  dimension: string | null;
  conversionToBase: string | null;
  tracked: boolean | null;
  barcode: string | null;
  sellPrice: string | null;
  mrp: string | null;
  purchasePrice: string | null;
  hsn: string | null;
  gstApplicable: string | null;
  gstClassification: string | null;
  gstRate: string | null;
  priceIncludesGst: string | null;
  salePriceIncludesGst: string | null;
  purchasePriceIncludesGst: string | null;
  gstEffectiveFrom: string | null;
  gstin: string | null;
  businessState: string | null;
  billingState: string | null;
  deliveryState: string | null;
  parentMasterId: string | null;
  status: string;
  payload: Record<string, unknown>;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OperatingPriceListLineRecord {
  masterId: string;
  price: string;
  schemePct: string | null;
}

export interface OperatingPriceListRecord {
  id: string;
  companyId: string;
  momentId: string | null;
  name: string;
  channel: string | null;
  priceUnit: string | null;
  marginPct: string | null;
  schemePct: string | null;
  gstRate: string | null;
  validFrom: string;
  validTo: string | null;
  warehouseMasterId: string | null;
  minOrder: string | null;
  status: string;
  payload: Record<string, unknown>;
  version: number;
  lines: OperatingPriceListLineRecord[];
  createdAt: string;
  updatedAt: string;
}

interface MasterRow {
  master_id: string;
  company_id: string;
  moment_id: string | null;
  kind: string;
  name: string;
  code: string | null;
  unit: string | null;
  dimension: string | null;
  conversion_to_base: string | null;
  tracked: boolean | null;
  barcode: string | null;
  sell_price: string | null;
  mrp: string | null;
  purchase_price: string | null;
  hsn: string | null;
  gst_applicable: string | null;
  gst_classification: string | null;
  gst_rate: string | null;
  price_includes_gst: string | null;
  sale_price_includes_gst: string | null;
  purchase_price_includes_gst: string | null;
  gst_effective_from: string | null;
  gstin: string | null;
  business_state: string | null;
  billing_state: string | null;
  delivery_state: string | null;
  parent_master_id: string | null;
  status: string;
  payload: Record<string, unknown> | null;
  version: string | number;
  created_at: Date;
  updated_at: Date;
}

const MASTER_COLUMNS = `master_id, company_id, moment_id, kind, name, code, unit, dimension,
  conversion_to_base::text, tracked, barcode, sell_price::text, mrp::text, purchase_price::text,
  hsn, gst_applicable, gst_classification, gst_rate::text, price_includes_gst,
  sale_price_includes_gst, purchase_price_includes_gst,
  to_char(gst_effective_from, 'YYYY-MM-DD') AS gst_effective_from,
  gstin, business_state, billing_state, delivery_state, parent_master_id, status, payload,
  version, created_at, updated_at`;

function isoDay(value: string | null | undefined): string | null {
  const text = String(value ?? '').trim();
  if (!text) return null;
  return text.slice(0, 10);
}

function masterRecord(row: MasterRow): OperatingMasterRecord {
  return {
    id: row.master_id,
    companyId: row.company_id,
    momentId: row.moment_id,
    kind: row.kind,
    name: row.name,
    code: row.code,
    unit: row.unit,
    dimension: row.dimension,
    conversionToBase: row.conversion_to_base,
    tracked: row.tracked,
    barcode: row.barcode,
    sellPrice: row.sell_price,
    mrp: row.mrp,
    purchasePrice: row.purchase_price,
    hsn: row.hsn,
    gstApplicable: row.gst_applicable,
    gstClassification: row.gst_classification,
    gstRate: row.gst_rate,
    priceIncludesGst: row.price_includes_gst,
    salePriceIncludesGst: row.sale_price_includes_gst,
    purchasePriceIncludesGst: row.purchase_price_includes_gst,
    gstEffectiveFrom: row.gst_effective_from,
    gstin: row.gstin,
    businessState: row.business_state,
    billingState: row.billing_state,
    deliveryState: row.delivery_state,
    parentMasterId: row.parent_master_id,
    status: row.status,
    payload: row.payload ?? {},
    version: Number(row.version),
    createdAt: row.created_at.toISOString(),
    updatedAt: row.updated_at.toISOString(),
  };
}

function isUniqueViolation(error: unknown): boolean {
  return Boolean(error && typeof error === 'object' && (error as { code?: string }).code === '23505');
}

function rethrowMasterWrite(error: unknown): never {
  if (isUniqueViolation(error)) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'A master with this name already exists. Edit that one instead of adding a second.',
      409
    );
  }
  throw error;
}

/**
 * Whether V099 has been applied. A failed query inside an open transaction poisons the
 * whole transaction (`current transaction is aborted`), so the presence check happens
 * first — `to_regclass` never throws, and a cache keeps it off the hot path.
 */
let masterTablePresence: { ready: boolean; checkedAt: number } | null = null;

export async function operatingMasterTablesReady(client: PoolClient): Promise<boolean> {
  const cached = masterTablePresence;
  if (cached && Date.now() - cached.checkedAt < 60_000) return cached.ready;
  const probe = await client.query<{ ready: boolean }>(
    "SELECT to_regclass('business.operating_master') IS NOT NULL AS ready"
  );
  const ready = probe.rows[0]?.ready === true;
  masterTablePresence = { ready, checkedAt: Date.now() };
  return ready;
}

/** Test-only: forget the probe so a migration applied mid-run is seen immediately. */
export function resetOperatingMasterTableProbe(): void {
  masterTablePresence = null;
}

/**
 * Every master for a company, with moment-scoped rows included. The projection
 * filters by moment when it builds the pickable list.
 */
export async function listMastersForCompany(
  client: PoolClient,
  companyId: string
): Promise<OperatingMasterRecord[]> {
  // A caller in a write transaction must never have this abort the transaction, and an
  // install that predates V099 must keep rendering its event-derived masters.
  if (!(await operatingMasterTablesReady(client))) return [];
  const rows = await client.query<MasterRow>(
    `SELECT ${MASTER_COLUMNS}
     FROM business.operating_master
     WHERE company_id = $1 AND status <> 'ARCHIVED'
     ORDER BY kind, lower(name)`,
    [companyId]
  );
  return rows.rows.map(masterRecord);
}

export async function listOperatingMasters(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  filters: { kind?: string; search?: string; momentId?: string; includeArchived?: boolean } = {}
): Promise<{ items: OperatingMasterRecord[] }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_MASTER_READ',
    resourceType: 'OPERATING_MASTER',
    companyId,
  });
  const conditions = ['company_id = $1'];
  const values: unknown[] = [companyId];
  if (!filters.includeArchived) conditions.push(`status <> 'ARCHIVED'`);
  if (filters.kind) {
    values.push(filters.kind.toUpperCase());
    conditions.push(`kind = $${values.length}`);
  }
  if (filters.search && filters.search.trim()) {
    values.push(`%${filters.search.trim().toLowerCase()}%`);
    conditions.push(`(lower(name) LIKE $${values.length} OR lower(coalesce(code, '')) LIKE $${values.length})`);
  }
  if (filters.momentId) {
    values.push(filters.momentId);
    conditions.push(`(moment_id IS NULL OR moment_id = $${values.length})`);
  }
  const rows = await client.query<MasterRow>(
    `SELECT ${MASTER_COLUMNS}
     FROM business.operating_master
     WHERE ${conditions.join(' AND ')}
     ORDER BY kind, lower(name)
     LIMIT 500`,
    values
  );
  return { items: rows.rows.map(masterRecord) };
}

export async function getOperatingMaster(
  client: PoolClient,
  companyId: string,
  masterId: string
): Promise<OperatingMasterRecord | null> {
  const rows = await client.query<MasterRow>(
    `SELECT ${MASTER_COLUMNS}
     FROM business.operating_master
     WHERE company_id = $1 AND master_id = $2`,
    [companyId, masterId]
  );
  return rows.rows[0] ? masterRecord(rows.rows[0]) : null;
}

/**
 * Resolve a batch of master references in one round trip. Used by the trading
 * contract so a line can read its unit, GST class, and standing price without a
 * query per line.
 */
export async function resolveMasterRefs(
  client: PoolClient,
  companyId: string,
  ids: string[]
): Promise<Map<string, OperatingMasterRecord>> {
  const wanted = [...new Set(ids.filter((id) => typeof id === 'string' && id.trim().length > 0))];
  if (wanted.length === 0) return new Map();
  // Called inside the write transaction, so a missing table must read as "no refs"
  // rather than abort the post.
  if (!(await operatingMasterTablesReady(client))) return new Map();
  const rows = await client.query<MasterRow>(
    `SELECT ${MASTER_COLUMNS}
     FROM business.operating_master
     WHERE company_id = $1 AND master_id = ANY($2::uuid[])`,
    [companyId, wanted]
  );
  return new Map(rows.rows.map((row) => [row.master_id, masterRecord(row)]));
}

export async function createOperatingMaster(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: CreateOperatingMasterInput
): Promise<OperatingMasterRecord> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_MASTER_CREATE',
    resourceType: 'OPERATING_MASTER',
    companyId,
  });
  try {
    const inserted = await client.query<MasterRow>(
      `INSERT INTO business.operating_master (
         company_id, moment_id, kind, name, code, unit, dimension, conversion_to_base,
         tracked, barcode, sell_price, mrp, purchase_price, hsn, gst_applicable,
         gst_classification, gst_rate, price_includes_gst, sale_price_includes_gst,
         purchase_price_includes_gst, gst_effective_from, gstin, business_state,
         billing_state, delivery_state, parent_master_id, payload, status, version
       ) VALUES (
         $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,
         $21::date,$22,$23,$24,$25,$26,$27::jsonb,'ACTIVE',1
       )
       RETURNING ${MASTER_COLUMNS}`,
      [
        companyId,
        body.momentId ?? null,
        body.kind,
        body.name.trim(),
        body.code ?? null,
        body.unit ?? null,
        body.dimension ?? null,
        body.conversionToBase ?? null,
        body.tracked ?? null,
        body.barcode ?? null,
        body.sellPrice ?? null,
        body.mrp ?? null,
        body.purchasePrice ?? null,
        body.hsn ?? null,
        body.gstApplicable ?? null,
        body.gstClassification ?? null,
        body.gstRate ?? null,
        body.priceIncludesGst ?? null,
        body.salePriceIncludesGst ?? null,
        body.purchasePriceIncludesGst ?? null,
        isoDay(body.gstEffectiveFrom),
        body.gstin ?? null,
        body.businessState ?? null,
        body.billingState ?? null,
        body.deliveryState ?? null,
        body.parentMasterId ?? null,
        JSON.stringify(body.payload ?? {}),
      ]
    );
    return masterRecord(inserted.rows[0]!);
  } catch (error) {
    rethrowMasterWrite(error);
  }
}

export async function updateOperatingMaster(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  masterId: string,
  body: UpdateOperatingMasterInput
): Promise<OperatingMasterRecord> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_MASTER_UPDATE',
    resourceType: 'OPERATING_MASTER',
    resourceId: masterId,
    companyId,
  });
  try {
    const updated = await client.query<MasterRow>(
      `UPDATE business.operating_master SET
         moment_id = CASE WHEN $4::uuid IS NULL THEN moment_id ELSE $4::uuid END,
         kind = COALESCE($5, kind),
         name = COALESCE($6, name),
         code = COALESCE($7, code),
         unit = COALESCE($8, unit),
         dimension = COALESCE($9, dimension),
         conversion_to_base = COALESCE($10, conversion_to_base),
         tracked = COALESCE($11, tracked),
         barcode = COALESCE($12, barcode),
         sell_price = COALESCE($13, sell_price),
         mrp = COALESCE($14, mrp),
         purchase_price = COALESCE($15, purchase_price),
         hsn = COALESCE($16, hsn),
         gst_applicable = COALESCE($17, gst_applicable),
         gst_classification = COALESCE($18, gst_classification),
         gst_rate = COALESCE($19, gst_rate),
         price_includes_gst = COALESCE($20, price_includes_gst),
         sale_price_includes_gst = COALESCE($21, sale_price_includes_gst),
         purchase_price_includes_gst = COALESCE($22, purchase_price_includes_gst),
         gst_effective_from = COALESCE($23::date, gst_effective_from),
         gstin = COALESCE($24, gstin),
         business_state = COALESCE($25, business_state),
         billing_state = COALESCE($26, billing_state),
         delivery_state = COALESCE($27, delivery_state),
         parent_master_id = COALESCE($28::uuid, parent_master_id),
         status = COALESCE($29, status),
         payload = COALESCE($30::jsonb, payload),
         version = version + 1,
         updated_at = now()
       WHERE company_id = $1 AND master_id = $2 AND version = $3
       RETURNING ${MASTER_COLUMNS}`,
      [
        companyId,
        masterId,
        body.expectedVersion,
        body.momentId ?? null,
        body.kind ?? null,
        body.name?.trim() ?? null,
        body.code ?? null,
        body.unit ?? null,
        body.dimension ?? null,
        body.conversionToBase ?? null,
        body.tracked ?? null,
        body.barcode ?? null,
        body.sellPrice ?? null,
        body.mrp ?? null,
        body.purchasePrice ?? null,
        body.hsn ?? null,
        body.gstApplicable ?? null,
        body.gstClassification ?? null,
        body.gstRate ?? null,
        body.priceIncludesGst ?? null,
        body.salePriceIncludesGst ?? null,
        body.purchasePriceIncludesGst ?? null,
        isoDay(body.gstEffectiveFrom),
        body.gstin ?? null,
        body.businessState ?? null,
        body.billingState ?? null,
        body.deliveryState ?? null,
        body.parentMasterId ?? null,
        body.status ?? null,
        body.payload === undefined ? null : JSON.stringify(body.payload),
      ]
    );
    if (!updated.rows[0]) {
      throw new AppError(
        ErrorCode.VERSION_CONFLICT,
        'This master changed since you opened it. Reload and try again.',
        409
      );
    }
    return masterRecord(updated.rows[0]);
  } catch (error) {
    rethrowMasterWrite(error);
  }
}

/**
 * Archive instead of delete: posted events and price-list lines still point here,
 * and a master that vanished would break every historical document.
 */
export async function archiveOperatingMaster(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  masterId: string,
  expectedVersion: number
): Promise<OperatingMasterRecord> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_MASTER_ARCHIVE',
    resourceType: 'OPERATING_MASTER',
    resourceId: masterId,
    companyId,
  });
  const updated = await client.query<MasterRow>(
    `UPDATE business.operating_master
     SET status = 'ARCHIVED', version = version + 1, updated_at = now()
     WHERE company_id = $1 AND master_id = $2 AND version = $3
     RETURNING ${MASTER_COLUMNS}`,
    [companyId, masterId, expectedVersion]
  );
  if (!updated.rows[0]) {
    throw new AppError(
      ErrorCode.VERSION_CONFLICT,
      'This master changed since you opened it. Reload and try again.',
      409
    );
  }
  return masterRecord(updated.rows[0]);
}

interface PriceListRow {
  price_list_id: string;
  company_id: string;
  moment_id: string | null;
  name: string;
  channel: string | null;
  price_unit: string | null;
  margin_pct: string | null;
  scheme_pct: string | null;
  gst_rate: string | null;
  valid_from: string;
  valid_to: string | null;
  warehouse_master_id: string | null;
  min_order: string | null;
  status: string;
  payload: Record<string, unknown> | null;
  version: string | number;
  created_at: Date;
  updated_at: Date;
}

const PRICE_LIST_COLUMNS = `price_list_id, company_id, moment_id, name, channel, price_unit,
  margin_pct::text, scheme_pct::text, gst_rate::text,
  to_char(valid_from, 'YYYY-MM-DD') AS valid_from,
  to_char(valid_to, 'YYYY-MM-DD') AS valid_to,
  warehouse_master_id, min_order::text, status, payload, version, created_at, updated_at`;

function priceListRecord(
  row: PriceListRow,
  lines: OperatingPriceListLineRecord[]
): OperatingPriceListRecord {
  return {
    id: row.price_list_id,
    companyId: row.company_id,
    momentId: row.moment_id,
    name: row.name,
    channel: row.channel,
    priceUnit: row.price_unit,
    marginPct: row.margin_pct,
    schemePct: row.scheme_pct,
    gstRate: row.gst_rate,
    validFrom: row.valid_from,
    validTo: row.valid_to,
    warehouseMasterId: row.warehouse_master_id,
    minOrder: row.min_order,
    status: row.status,
    payload: row.payload ?? {},
    version: Number(row.version),
    lines,
    createdAt: row.created_at.toISOString(),
    updatedAt: row.updated_at.toISOString(),
  };
}

async function loadPriceListLines(
  client: PoolClient,
  priceListIds: string[]
): Promise<Map<string, OperatingPriceListLineRecord[]>> {
  const map = new Map<string, OperatingPriceListLineRecord[]>();
  if (priceListIds.length === 0) return map;
  const rows = await client.query<{
    price_list_id: string;
    master_id: string;
    price: string;
    scheme_pct: string | null;
  }>(
    `SELECT price_list_id, master_id, price::text, scheme_pct::text
     FROM business.operating_price_list_line
     WHERE price_list_id = ANY($1::uuid[])
     ORDER BY price DESC`,
    [priceListIds]
  );
  for (const row of rows.rows) {
    const list = map.get(row.price_list_id) ?? [];
    list.push({ masterId: row.master_id, price: row.price, schemePct: row.scheme_pct });
    map.set(row.price_list_id, list);
  }
  return map;
}

export async function listPriceLists(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  filters: { momentId?: string; includeArchived?: boolean } = {}
): Promise<{ items: OperatingPriceListRecord[] }> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_PRICE_LIST_READ',
    resourceType: 'OPERATING_PRICE_LIST',
    companyId,
  });
  const conditions = ['company_id = $1'];
  const values: unknown[] = [companyId];
  if (!filters.includeArchived) conditions.push(`status <> 'ARCHIVED'`);
  if (filters.momentId) {
    values.push(filters.momentId);
    conditions.push(`(moment_id IS NULL OR moment_id = $${values.length})`);
  }
  const rows = await client.query<PriceListRow>(
    `SELECT ${PRICE_LIST_COLUMNS}
     FROM business.operating_price_list
     WHERE ${conditions.join(' AND ')}
     ORDER BY valid_from DESC, lower(name)
     LIMIT 200`,
    values
  );
  const lines = await loadPriceListLines(
    client,
    rows.rows.map((row) => row.price_list_id)
  );
  return {
    items: rows.rows.map((row) => priceListRecord(row, lines.get(row.price_list_id) ?? [])),
  };
}

export async function createPriceList(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: CreatePriceListInput
): Promise<OperatingPriceListRecord> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_PRICE_LIST_CREATE',
    resourceType: 'OPERATING_PRICE_LIST',
    companyId,
  });
  const saved = await client.query<{ price_list_id: string }>(
    `INSERT INTO business.operating_price_list (
       company_id, moment_id, name, channel, price_unit, margin_pct, scheme_pct, gst_rate,
       valid_from, valid_to, warehouse_master_id, min_order, status, payload, version
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9::date,$10::date,$11,$12,'ACTIVE',$13::jsonb,1)
     RETURNING price_list_id`,
    [
      companyId,
      body.momentId ?? null,
      body.name.trim(),
      body.channel ?? null,
      body.priceUnit ?? null,
      body.marginPct ?? null,
      body.schemePct ?? null,
      body.gstRate ?? null,
      isoDay(body.validFrom),
      isoDay(body.validTo),
      body.warehouseMasterId ?? null,
      body.minOrder ?? null,
      JSON.stringify(body.payload ?? {}),
    ]
  );
  const priceListId = saved.rows[0]!.price_list_id;
  await replacePriceListLines(client, companyId, priceListId, body.lines ?? []);
  const created = await loadPriceList(client, companyId, priceListId);
  if (!created) {
    throw new AppError(ErrorCode.INFRASTRUCTURE_UNAVAILABLE, 'Price list was not saved.', 500);
  }
  return created;
}

export async function updatePriceList(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  priceListId: string,
  body: UpdatePriceListInput
): Promise<OperatingPriceListRecord> {
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'OPERATING_PRICE_LIST_UPDATE',
    resourceType: 'OPERATING_PRICE_LIST',
    resourceId: priceListId,
    companyId,
  });
  const updated = await client.query<{ price_list_id: string }>(
    `UPDATE business.operating_price_list SET
       name = COALESCE($4, name),
       channel = COALESCE($5, channel),
       price_unit = COALESCE($6, price_unit),
       margin_pct = COALESCE($7, margin_pct),
       scheme_pct = COALESCE($8, scheme_pct),
       gst_rate = COALESCE($9, gst_rate),
       valid_from = COALESCE($10::date, valid_from),
       valid_to = COALESCE($11::date, valid_to),
       warehouse_master_id = COALESCE($12::uuid, warehouse_master_id),
       min_order = COALESCE($13, min_order),
       status = COALESCE($14, status),
       version = version + 1,
       updated_at = now()
     WHERE company_id = $1 AND price_list_id = $2 AND version = $3
     RETURNING price_list_id`,
    [
      companyId,
      priceListId,
      body.expectedVersion,
      body.name?.trim() ?? null,
      body.channel ?? null,
      body.priceUnit ?? null,
      body.marginPct ?? null,
      body.schemePct ?? null,
      body.gstRate ?? null,
      isoDay(body.validFrom),
      isoDay(body.validTo),
      body.warehouseMasterId ?? null,
      body.minOrder ?? null,
      body.status ?? null,
    ]
  );
  if (!updated.rows[0]) {
    throw new AppError(
      ErrorCode.VERSION_CONFLICT,
      'This price list changed since you opened it. Reload and try again.',
      409
    );
  }
  if (body.lines) await replacePriceListLines(client, companyId, priceListId, body.lines);
  const saved = await loadPriceList(client, companyId, priceListId);
  if (!saved) {
    throw new AppError(ErrorCode.INFRASTRUCTURE_UNAVAILABLE, 'Price list was not saved.', 500);
  }
  return saved;
}

async function loadPriceList(
  client: PoolClient,
  companyId: string,
  priceListId: string
): Promise<OperatingPriceListRecord | null> {
  const rows = await client.query<PriceListRow>(
    `SELECT ${PRICE_LIST_COLUMNS}
     FROM business.operating_price_list
     WHERE company_id = $1 AND price_list_id = $2`,
    [companyId, priceListId]
  );
  if (!rows.rows[0]) return null;
  const lines = await loadPriceListLines(client, [priceListId]);
  return priceListRecord(rows.rows[0], lines.get(priceListId) ?? []);
}

async function replacePriceListLines(
  client: PoolClient,
  companyId: string,
  priceListId: string,
  lines: Array<{ masterId: string; price: string; schemePct?: string | null }>
): Promise<void> {
  if (lines.length === 0) return;
  // A line may only reference a master this company owns.
  const owned = await client.query<{ master_id: string }>(
    `SELECT master_id FROM business.operating_master
     WHERE company_id = $1 AND master_id = ANY($2::uuid[])`,
    [companyId, lines.map((line) => line.masterId)]
  );
  const allowed = new Set(owned.rows.map((row) => row.master_id));
  const unknown = lines.find((line) => !allowed.has(line.masterId));
  if (unknown) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'A price-list line references a product this company does not have.',
      400
    );
  }
  await client.query(`DELETE FROM business.operating_price_list_line WHERE price_list_id = $1`, [
    priceListId,
  ]);
  const values: string[] = [];
  const params: unknown[] = [priceListId];
  for (const line of lines) {
    params.push(line.masterId, line.price, line.schemePct ?? null);
    const base = params.length;
    values.push(`($1, $${base - 2}::uuid, $${base - 1}, $${base})`);
  }
  await client.query(
    `INSERT INTO business.operating_price_list_line (price_list_id, master_id, price, scheme_pct)
     VALUES ${values.join(', ')}
     ON CONFLICT (price_list_id, master_id) DO UPDATE
       SET price = EXCLUDED.price, scheme_pct = EXCLUDED.scheme_pct, updated_at = now()`,
    params
  );
}

/** Rate that applies to one product on a date, preferring an active price list. */
export async function resolvePriceListRate(
  client: PoolClient,
  companyId: string,
  masterId: string,
  asOf: string
): Promise<{ price: string; schemePct: string | null; priceListId: string } | null> {
  const rows = await client.query<{
    price: string;
    scheme_pct: string | null;
    price_list_id: string;
  }>(
    `SELECT l.price::text AS price, l.scheme_pct::text AS scheme_pct, l.price_list_id
     FROM business.operating_price_list_line l
     JOIN business.operating_price_list pl ON pl.price_list_id = l.price_list_id
     WHERE pl.company_id = $1
       AND l.master_id = $2
       AND pl.status = 'ACTIVE'
       AND pl.valid_from <= $3::date
       AND (pl.valid_to IS NULL OR pl.valid_to >= $3::date)
     ORDER BY pl.valid_from DESC
     LIMIT 1`,
    [companyId, masterId, isoDay(asOf) ?? new Date().toISOString().slice(0, 10)]
  );
  const row = rows.rows[0];
  if (!row) return null;
  return { price: row.price, schemePct: row.scheme_pct, priceListId: row.price_list_id };
}
