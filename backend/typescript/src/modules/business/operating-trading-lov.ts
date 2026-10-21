/**
 * Trading LOV seeds, UOM dimensions, and base-quantity conversion helpers (v6 Part 2).
 */

export type UomDimension = 'count' | 'mass' | 'volume' | 'length' | 'area';

export interface SeedUom {
  code: string;
  label: string;
  dimension: UomDimension;
  /** Multiplier to base unit within the same dimension (1 for base). */
  toBase: number;
}

/** Doc §3.1 seed units — pack/box/carton are count family but conversion is product-specific. */
export const SEED_UOMS: SeedUom[] = [
  { code: 'pc', label: 'piece (pc)', dimension: 'count', toBase: 1 },
  { code: 'pair', label: 'pair', dimension: 'count', toBase: 2 },
  { code: 'set', label: 'set', dimension: 'count', toBase: 1 },
  { code: 'pack', label: 'pack', dimension: 'count', toBase: 0 },
  { code: 'box', label: 'box', dimension: 'count', toBase: 0 },
  { code: 'carton', label: 'carton', dimension: 'count', toBase: 0 },
  { code: 'dozen', label: 'dozen (12 pc)', dimension: 'count', toBase: 12 },
  { code: 'kg', label: 'kg', dimension: 'mass', toBase: 1 },
  { code: 'g', label: 'g', dimension: 'mass', toBase: 0.001 },
  { code: 'tonne', label: 'tonne', dimension: 'mass', toBase: 1000 },
  { code: 'L', label: 'litre (L)', dimension: 'volume', toBase: 1 },
  { code: 'mL', label: 'mL', dimension: 'volume', toBase: 0.001 },
  { code: 'm', label: 'metre (m)', dimension: 'length', toBase: 1 },
  { code: 'cm', label: 'cm', dimension: 'length', toBase: 0.01 },
  { code: 'foot', label: 'foot', dimension: 'length', toBase: 0.3048 },
  { code: 'sq ft', label: 'sq ft', dimension: 'area', toBase: 1 },
  { code: 'sq m', label: 'sq m', dimension: 'area', toBase: 10.7639 },
];

export const TRADING_MASTER_KINDS = [
  'PRODUCT',
  'BUYER',
  'CUSTOMER',
  'SUPPLIER',
  'CHANNEL',
  'EXPENSE_CATEGORY',
  'UOM',
  'LOCATION',
] as const;

export type TradingMasterKind = (typeof TRADING_MASTER_KINDS)[number];

export interface MasterMeta {
  kind: string;
  name: string;
  unit?: string | null;
  dimension?: UomDimension | null;
  conversionToBase?: string | number | null;
  tracked?: boolean | null;
  barcode?: string | null;
}

function num(value: string | number | null | undefined): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

export function seedUomByCode(code: string | null | undefined): SeedUom | null {
  const key = String(code ?? '').trim().toLowerCase();
  if (!key) return null;
  return SEED_UOMS.find((row) => row.code.toLowerCase() === key || row.label.toLowerCase() === key) ?? null;
}

export function uomDimension(code: string | null | undefined): UomDimension | null {
  return seedUomByCode(code)?.dimension ?? null;
}

/**
 * Convert qty in `unit` to product base units.
 * Pack/box/carton require product conversionToBase (>0).
 */
export function toBaseQuantity(
  quantity: string | number | null | undefined,
  unit: string | null | undefined,
  product: MasterMeta | null | undefined
): { ok: true; baseQty: number } | { ok: false; message: string } {
  const qty = num(quantity);
  if (!(qty > 0)) return { ok: false, message: 'Quantity is required.' };
  const seed = seedUomByCode(unit);
  const productDim = product?.dimension ?? (product?.unit ? uomDimension(product.unit) : null);
  if (seed && productDim && seed.dimension !== productDim) {
    return { ok: false, message: 'Unit is not compatible with this product.' };
  }
  const needsProductConversion = seed && seed.toBase === 0;
  if (needsProductConversion) {
    const factor = num(product?.conversionToBase);
    if (!(factor > 0)) {
      return { ok: false, message: 'Set a product conversion for pack, box, or carton.' };
    }
    return { ok: true, baseQty: qty * factor };
  }
  if (seed) return { ok: true, baseQty: qty * seed.toBase };
  // Unknown free-text unit: treat as base 1:1 when no product dimension conflict.
  return { ok: true, baseQty: qty };
}

export function lineBaseQuantity(
  line: { quantity?: string | null; unit?: string | null; baseQuantity?: string | null },
  product: MasterMeta | null | undefined
): number {
  if (line.baseQuantity != null && num(line.baseQuantity) > 0) return num(line.baseQuantity);
  const converted = toBaseQuantity(line.quantity, line.unit, product);
  return converted.ok ? converted.baseQty : num(line.quantity);
}
