/**
 * The canonical India States & Union Territories registry, keyed by GST state code.
 *
 * A GSTIN begins with the two-digit code of the state that issued the registration, and GST
 * splits a supply into CGST+SGST (intra-state) or IGST (inter-state) by comparing the supplier's
 * state with the place of supply. Both of those are code comparisons — comparing the operator's
 * typed state name against a master's typed state name is what let `"MH"` and `"Maharashtra"`
 * read as two different states and charge IGST on an intra-state sale.
 *
 * This table is therefore the single source of truth on the server. The Android and iOS clients
 * carry an offline copy so the picker opens without a round-trip; `tests/gst-states.test.ts`
 * reads both files and fails if any of the three copies drift.
 */

export type GstStateType = 'STATE' | 'UT';

export interface GstState {
  /** Two-digit GST state code, zero-padded (`'01'` … `'38'`, plus `'97'`). */
  code: string;
  /** The name printed in the picker and stored on the preference. */
  name: string;
  type: GstStateType;
}

/**
 * 28 states and 8 union territories, in GST code order, plus `97 Other Territory`.
 *
 * Code 25 (Daman & Diu) was merged into 26 (Dadra & Nagar Haveli and Daman & Diu) when the two
 * territories were combined, so it is deliberately absent — `normalizeGstState('25')` resolves to
 * nothing rather than resurrecting a retired jurisdiction.
 */
export const GST_STATES: ReadonlyArray<GstState> = [
  { code: '01', name: 'Jammu & Kashmir', type: 'UT' },
  { code: '02', name: 'Himachal Pradesh', type: 'STATE' },
  { code: '03', name: 'Punjab', type: 'STATE' },
  { code: '04', name: 'Chandigarh', type: 'UT' },
  { code: '05', name: 'Uttarakhand', type: 'STATE' },
  { code: '06', name: 'Haryana', type: 'STATE' },
  { code: '07', name: 'Delhi', type: 'UT' },
  { code: '08', name: 'Rajasthan', type: 'STATE' },
  { code: '09', name: 'Uttar Pradesh', type: 'STATE' },
  { code: '10', name: 'Bihar', type: 'STATE' },
  { code: '11', name: 'Sikkim', type: 'STATE' },
  { code: '12', name: 'Arunachal Pradesh', type: 'STATE' },
  { code: '13', name: 'Nagaland', type: 'STATE' },
  { code: '14', name: 'Manipur', type: 'STATE' },
  { code: '15', name: 'Mizoram', type: 'STATE' },
  { code: '16', name: 'Tripura', type: 'STATE' },
  { code: '17', name: 'Meghalaya', type: 'STATE' },
  { code: '18', name: 'Assam', type: 'STATE' },
  { code: '19', name: 'West Bengal', type: 'STATE' },
  { code: '20', name: 'Jharkhand', type: 'STATE' },
  { code: '21', name: 'Odisha', type: 'STATE' },
  { code: '22', name: 'Chhattisgarh', type: 'STATE' },
  { code: '23', name: 'Madhya Pradesh', type: 'STATE' },
  { code: '24', name: 'Gujarat', type: 'STATE' },
  { code: '26', name: 'Dadra & Nagar Haveli and Daman & Diu', type: 'UT' },
  { code: '27', name: 'Maharashtra', type: 'STATE' },
  { code: '29', name: 'Karnataka', type: 'STATE' },
  { code: '30', name: 'Goa', type: 'STATE' },
  { code: '31', name: 'Lakshadweep', type: 'UT' },
  { code: '32', name: 'Kerala', type: 'STATE' },
  { code: '33', name: 'Tamil Nadu', type: 'STATE' },
  { code: '34', name: 'Puducherry', type: 'UT' },
  { code: '35', name: 'Andaman & Nicobar Islands', type: 'UT' },
  { code: '36', name: 'Telangana', type: 'STATE' },
  { code: '37', name: 'Andhra Pradesh', type: 'STATE' },
  { code: '38', name: 'Ladakh', type: 'UT' },
  { code: '97', name: 'Other Territory', type: 'UT' },
];

/**
 * Free-text spellings that appear in older saved rows, typed invoices and master records.
 *
 * Keys are the normalized (lowercased, whitespace-collapsed, punctuation-stripped) spelling; the
 * value is the GST state code. `'25'` is intentionally mapped to `26` because Daman & Diu ceased
 * to be its own code when the two territories merged.
 */
const GST_STATE_ALIASES: Readonly<Record<string, string>> = {
  // Two-letter postal abbreviations operators type into a free-text field.
  ap: '37',
  ar: '12',
  as: '18',
  br: '10',
  cg: '22',
  ch: '04',
  dd: '26',
  dl: '07',
  dn: '26',
  ga: '30',
  gj: '24',
  hp: '02',
  hr: '06',
  jh: '20',
  jk: '01',
  'j k': '01',
  ka: '29',
  kl: '32',
  la: '38',
  ld: '31',
  mh: '27',
  ml: '17',
  mn: '14',
  mp: '23',
  mz: '15',
  nl: '13',
  or: '21',
  pb: '03',
  py: '34',
  rj: '08',
  sk: '11',
  tg: '36',
  tn: '33',
  tr: '16',
  ts: '36',
  uk: '05',
  up: '09',
  wb: '19',
  // City names that leaked into the field while it was free text.
  bangalore: '29',
  bengaluru: '29',
  bombay: '27',
  calcutta: '19',
  chennai: '33',
  cochin: '32',
  ernakulam: '32',
  gurgaon: '06',
  gurugram: '06',
  hyderabad: '36',
  kolkata: '19',
  mumbai: '27',
  mysore: '29',
  mysuru: '29',
  ncr: '07',
  noida: '09',
  pune: '27',
  trivandrum: '32',
  visakhapatnam: '37',
  vizag: '37',
  // Merged / renamed territories and the spellings the old placeholder invited.
  daman: '26',
  diu: '26',
  'daman and diu': '26',
  'dadra and nagar haveli': '26',
  'nagar haveli': '26',
  'andaman and nicobar islands': '35',
  andaman: '35',
  nicobar: '35',
  'andaman nicobar': '35',
  'jammu and kashmir': '01',
  jammu: '01',
  kashmir: '01',
  orissa: '21',
  pondicherry: '34',
  uttaranchal: '05',
  'new delhi': '07',
  nct: '07',
  'nct of delhi': '07',
  // The retired Daman & Diu code.
  '25': '26',
};

/** Lowercase, strip punctuation to spaces, and collapse runs of whitespace. */
function normalizeKey(raw: string): string {
  return raw
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
    .replace(/\s+/g, ' ');
}

const BY_CODE = new Map<string, GstState>(GST_STATES.map((s) => [s.code, s]));
const BY_KEY = new Map<string, GstState>(
  GST_STATES.map((s) => [normalizeKey(s.name), s])
);

/**
 * Resolve any spelling of an Indian state to its canonical record.
 *
 * Accepts the full name in any casing, the two-digit code, a two-letter postal abbreviation, and
 * the legacy names / city names listed in `GST_STATE_ALIASES`. Returns `null` when nothing
 * matches, so a caller can decide whether an unresolved state is a draft or a rejection.
 */
export function normalizeGstState(raw: unknown): GstState | null {
  const text = String(raw ?? '').trim();
  if (!text) return null;

  const key = normalizeKey(text);
  if (!key) return null;

  // A bare two-digit code is the most common saved form; check it before the name index.
  if (/^\d{2}$/.test(text)) {
    return BY_CODE.get(text) ?? (GST_STATE_ALIASES[text] ? BY_CODE.get(GST_STATE_ALIASES[text]!) ?? null : null);
  }

  const byName = BY_KEY.get(key);
  if (byName) return byName;

  const aliasCode = GST_STATE_ALIASES[key];
  if (aliasCode) return BY_CODE.get(aliasCode) ?? null;

  // A bare code with leading/trailing noise, e.g. `"07 "` after the trim above.
  if (/^\d{1,2}$/.test(key)) {
    const padded = key.padStart(2, '0');
    return BY_CODE.get(padded) ?? null;
  }

  return null;
}

/** The canonical full name for any spelling, or `''` when it cannot be resolved. */
export function gstStateName(raw: unknown): string {
  return normalizeGstState(raw)?.name ?? '';
}

/** The two-digit GST code for any spelling, or `''` when it cannot be resolved. */
export function gstStateCodeOf(raw: unknown): string {
  return normalizeGstState(raw)?.code ?? '';
}

/**
 * `INTRA` when both sides resolve to the same GST state code, `INTER` when both resolve and
 * differ, and `null` when either side cannot be resolved.
 *
 * When a side is a name the table does not know (a free-text master state from before the
 * registry existed), this falls back to a case-insensitive name compare so an existing record
 * whose two sides are written identically still resolves inside the state instead of silently
 * becoming inter-state.
 */
export function resolveJurisdiction(
  businessState: unknown,
  supplyState: unknown
): 'INTRA' | 'INTER' | null {
  const business = normalizeGstState(businessState);
  const supply = normalizeGstState(supplyState);
  if (business && supply) return business.code === supply.code ? 'INTRA' : 'INTER';

  const left = String(businessState ?? '').trim().toLowerCase();
  const right = String(supplyState ?? '').trim().toLowerCase();
  if (left && right) return left === right ? 'INTRA' : 'INTER';
  return null;
}
