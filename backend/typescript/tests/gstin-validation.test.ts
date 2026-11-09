/**
 * GSTIN format, checksum, and state-prefix validation.
 *
 * The checksum is the only part that catches a typo the format check cannot: a 15-character
 * string of the right shape with one digit transposed. These tests use GSTINs whose check digit
 * is known to be correct rather than synthesizing them, so the algorithm is pinned to reality.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  GSTIN_PATTERN,
  draftGstin,
  gstinChecksumChar,
  validateGstin,
} from '../src/modules/business/operating-gstin';

/** A GSTIN with a correct check digit: 27 Maharashtra, 29 Karnataka. */
const VALID_MAHARASHTRA = '27AAPFU0939F1ZV';
const VALID_KARNATAKA = '29AAGCB7383J1Z4';

describe('GSTIN checksum', () => {
  it('computes the check digit for a known-valid GSTIN', () => {
    assert.equal(gstinChecksumChar(VALID_MAHARASHTRA.slice(0, 14)), 'V');
    assert.equal(gstinChecksumChar(VALID_KARNATAKA.slice(0, 14)), '4');
  });

  it('returns null for an input that is not 14 base-36 characters', () => {
    assert.equal(gstinChecksumChar('SHORT'), null);
    assert.equal(gstinChecksumChar('27AAPFU0939F1Z!'), null);
  });
});

describe('validateGstin', () => {
  it('accepts a correctly checksummed GSTIN and reports its state', () => {
    const result = validateGstin(VALID_MAHARASHTRA);
    assert.equal(result.ok, true);
    assert.equal(result.ok && result.stateCode, '27');
    assert.equal(result.ok && result.stateName, 'Maharashtra');
  });

  it('accepts lowercase input by uppercasing it first', () => {
    const result = validateGstin('27aapfu0939f1zv');
    assert.equal(result.ok, true);
    assert.equal(result.ok && result.gstin, VALID_MAHARASHTRA);
  });

  it('rejects the wrong length with LENGTH', () => {
    const result = validateGstin('27AAPFU0939F1Z');
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'LENGTH');
  });

  it('rejects an empty value', () => {
    const result = validateGstin('');
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'LENGTH');
  });

  it('rejects a shape failure with FORMAT', () => {
    // Entity number has a leading zero, which the pattern forbids.
    const result = validateGstin('27AAPFU0939F0ZV');
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'FORMAT');
  });

  it('rejects a flipped check digit with CHECKSUM', () => {
    const flipped = VALID_MAHARASHTRA.slice(0, 14) + 'A';
    const result = validateGstin(flipped);
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'CHECKSUM');
  });

  it('rejects a transposed pair the format check would let through', () => {
    // Swap the PAN's 4th and 5th digits. Same shape, different checksum.
    const transposed = '27AAPFU9039F1ZV';
    const result = validateGstin(transposed);
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'CHECKSUM');
  });

  it('reports STATE_MISMATCH when the prefix disagrees with the given state', () => {
    const result = validateGstin(VALID_KARNATAKA, '27');
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'STATE_MISMATCH');
    // The message names both states so the operator can see what to fix.
    assert.match(!result.ok ? result.message : '', /29.*Karnataka.*27.*Maharashtra/);
  });

  it('accepts when the prefix matches the given state name or code', () => {
    assert.equal(validateGstin(VALID_KARNATAKA, '29').ok, true);
    assert.equal(validateGstin(VALID_KARNATAKA, 'Karnataka').ok, true);
    assert.equal(validateGstin(VALID_KARNATAKA, 'KA').ok, true);
  });

  it('reports UNKNOWN_STATE for a prefix that is not an Indian GST code', () => {
    // 99 is not in the registry. Build a checksum-correct value for it.
    const body = '99AAPFU0939F1Z';
    const check = gstinChecksumChar(body);
    assert.ok(check);
    const result = validateGstin(body + check);
    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.code, 'UNKNOWN_STATE');
  });
});

describe('GSTIN_PATTERN', () => {
  it('accepts a well-formed GSTIN and rejects lowercase', () => {
    assert.equal(GSTIN_PATTERN.test(VALID_MAHARASHTRA), true);
    assert.equal(GSTIN_PATTERN.test(VALID_MAHARASHTRA.toLowerCase()), false);
  });
});

describe('draftGstin', () => {
  it('uppercases, trims, and truncates without validating', () => {
    assert.equal(draftGstin('  27aapfu0939f1zv  '), VALID_MAHARASHTRA);
    // A half-typed value survives a draft save.
    assert.equal(draftGstin('27aapf'), '27AAPF');
    assert.equal(draftGstin(null), '');
  });
});
