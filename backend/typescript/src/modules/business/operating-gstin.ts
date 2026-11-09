/**
 * GSTIN validation.
 *
 * A GSTIN is 15 characters:
 *
 *   `27` `ABCDE` `4321` `T` `1` `Z` `5`
 *    |     |      |     |    |   |   |
 *    |     |      |     |    |   |   +-- checksum digit (base-36)
 *    |     |      |     |    |   +------ 'Z' by default
 *    |     |      |     |    +---------- entity number for that PAN within the state
 *    |     |      |     +--------------- first letter of the PAN holder's name
 *    |     |      +--------------------- 4th–7th digits of the PAN
 *    |     +---------------------------- first 5 letters of the PAN
 *    +---------------------------------- GST state code of the registering state
 *
 * The checksum is a base-36 weighted sum: for the first 14 characters, take the value
 * (`0-9` → 0–9, `A-Z` → 10–35), multiply by a weight that alternates 1, 2, 1, 2, …, divide by 36,
 * and take the quotient plus the remainder. The check digit is `(36 - sum % 36) % 36` rendered in
 * base 36. A single transposed pair of characters breaks it, which is what makes it worth doing.
 */

import { normalizeGstState } from './operating-gst-states';

export const GSTIN_LENGTH = 15;

/** The shape check, before the checksum. Lowercase is rejected — a GSTIN is uppercase. */
export const GSTIN_PATTERN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;

export type GstinErrorCode =
  | 'LENGTH'
  | 'FORMAT'
  | 'CHECKSUM'
  | 'STATE_MISMATCH'
  | 'UNKNOWN_STATE';

export interface GstinFailure {
  ok: false;
  code: GstinErrorCode;
  message: string;
}

export interface GstinSuccess {
  ok: true;
  gstin: string;
  /** The state code the GSTIN's first two characters declare. */
  stateCode: string;
  /** The canonical state name, or `''` for `97 Other Territory` when not yet resolved. */
  stateName: string;
}

export type GstinResult = GstinSuccess | GstinFailure;

const BASE36 = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';

/** `0-9` → 0–9, `A-Z` → 10–35; `-1` for anything else. */
function base36Value(char: string): number {
  return BASE36.indexOf(char);
}

/** Compute the 15th checksum character for the first 14 characters of a GSTIN. */
export function gstinChecksumChar(first14: string): string | null {
  if (first14.length !== GSTIN_LENGTH - 1) return null;
  let sum = 0;
  for (let i = 0; i < first14.length; i += 1) {
    const value = base36Value(first14[i]!);
    if (value < 0) return null;
    const weight = i % 2 === 0 ? 1 : 2;
    const product = value * weight;
    sum += Math.floor(product / 36) + (product % 36);
  }
  return BASE36[(36 - (sum % 36)) % 36]!;
}

/**
 * Validate a GSTIN, optionally against the state the business says it is registered in.
 *
 * `stateCode` is compared with the GSTIN's first two characters when supplied — an operator who
 * types a Karnataka GSTIN under a Maharashtra registration has a real mismatch the invoice will
 * eventually expose, so activation rejects it rather than silently stamping the wrong place of
 * supply on every sale.
 */
export function validateGstin(raw: unknown, stateCode?: string | null): GstinResult {
  const gstin = String(raw ?? '').trim().toUpperCase();

  if (!gstin) {
    return { ok: false, code: 'LENGTH', message: 'Enter a 15-character GSTIN.' };
  }
  if (gstin.length !== GSTIN_LENGTH) {
    return {
      ok: false,
      code: 'LENGTH',
      message: `A GSTIN is 15 characters; this one is ${gstin.length}.`,
    };
  }
  if (!GSTIN_PATTERN.test(gstin)) {
    return {
      ok: false,
      code: 'FORMAT',
      message: 'This is not a valid GSTIN format. It reads like 27ABCDE4321T1Z5.',
    };
  }

  const expected = gstinChecksumChar(gstin.slice(0, GSTIN_LENGTH - 1));
  if (expected == null || expected !== gstin[GSTIN_LENGTH - 1]) {
    return {
      ok: false,
      code: 'CHECKSUM',
      message: 'The GSTIN checksum does not match. Re-check the number for a typo.',
    };
  }

  const declaredStateCode = gstin.slice(0, 2);
  const declaredState = normalizeGstState(declaredStateCode);
  if (!declaredState) {
    return {
      ok: false,
      code: 'UNKNOWN_STATE',
      message: `The GSTIN starts with ${declaredStateCode}, which is not an Indian GST state code.`,
    };
  }

  const expectedCode = String(stateCode ?? '').trim() ? normalizeGstState(stateCode)?.code ?? '' : '';
  if (expectedCode && expectedCode !== declaredState.code) {
    const expectedName = normalizeGstState(stateCode)?.name ?? stateCode;
    return {
      ok: false,
      code: 'STATE_MISMATCH',
      message:
        `The GSTIN starts with ${declaredState.code} (${declaredState.name}) but the business ` +
        `is registered in ${expectedCode} (${expectedName}).`,
    };
  }

  return { ok: true, gstin, stateCode: declaredState.code, stateName: declaredState.name };
}

/**
 * The GSTIN to store for a lenient (draft) write: uppercased and trimmed, with no validation.
 * A draft is mid-document, so a half-typed GSTIN must survive a save rather than block it.
 */
export function draftGstin(raw: unknown): string {
  return String(raw ?? '').trim().toUpperCase().slice(0, GSTIN_LENGTH);
}
