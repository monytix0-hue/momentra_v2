/**
 * The India States & UTs registry.
 *
 * This is the table every GST decision reads, so the tests pin its shape (37 rows, unique
 * two-digit codes), its normalization (name, code, abbreviation, casing, legacy spellings), and
 * the retired code that must not resolve.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  GST_STATES,
  gstStateCodeOf,
  gstStateName,
  normalizeGstState,
  resolveJurisdiction,
} from '../src/modules/business/operating-gst-states';

describe('GST state registry', () => {
  it('has 37 entries: 28 states, 8 union territories, and Other Territory', () => {
    assert.equal(GST_STATES.length, 37);
    assert.equal(GST_STATES.filter((s) => s.type === 'STATE').length, 28);
    assert.equal(GST_STATES.filter((s) => s.type === 'UT').length, 9);
  });

  it('has unique two-digit codes', () => {
    const codes = GST_STATES.map((s) => s.code);
    assert.equal(new Set(codes).size, codes.length);
    for (const code of codes) {
      assert.match(code, /^\d{2}$/, `${code} must be a two-digit code`);
    }
  });

  it('carries the official code for the states most trading setups register in', () => {
    const expected: Record<string, string> = {
      Maharashtra: '27',
      Karnataka: '29',
      Delhi: '07',
      Gujarat: '24',
      'Tamil Nadu': '33',
      Telangana: '36',
      'West Bengal': '19',
      Kerala: '32',
      'Uttar Pradesh': '09',
      Haryana: '06',
      Rajasthan: '08',
      Punjab: '03',
      Goa: '30',
      Ladakh: '38',
    };
    for (const [name, code] of Object.entries(expected)) {
      assert.equal(gstStateCodeOf(name), code, `${name} should be ${code}`);
    }
  });

  it('resolves a full name in any casing and with surrounding whitespace', () => {
    assert.equal(normalizeGstState('Maharashtra')?.code, '27');
    assert.equal(normalizeGstState('  maharashtra  ')?.code, '27');
    assert.equal(normalizeGstState('MAHARASHTRA')?.code, '27');
    assert.equal(normalizeGstState('Tamil  Nadu')?.code, '33');
  });

  it('resolves the two-digit code and returns the canonical name back', () => {
    assert.equal(normalizeGstState('27')?.name, 'Maharashtra');
    assert.equal(normalizeGstState('07')?.name, 'Delhi');
    assert.equal(gstStateName('29'), 'Karnataka');
  });

  it('resolves the postal abbreviations operators typed into the old free-text field', () => {
    assert.equal(normalizeGstState('MH')?.code, '27');
    assert.equal(normalizeGstState('KA')?.code, '29');
    assert.equal(normalizeGstState('TN')?.code, '33');
    // Former spellings.
    assert.equal(normalizeGstState('OR')?.code, '21');
    assert.equal(normalizeGstState('UK')?.code, '05');
    assert.equal(normalizeGstState('CG')?.code, '22');
    assert.equal(normalizeGstState('J&K')?.code, '01');
  });

  it('resolves the legacy names and the city names that leaked in', () => {
    assert.equal(normalizeGstState('Orissa')?.code, '21');
    assert.equal(normalizeGstState('Pondicherry')?.code, '34');
    assert.equal(normalizeGstState('Uttaranchal')?.code, '05');
    assert.equal(normalizeGstState('Bombay')?.code, '27');
    assert.equal(normalizeGstState('Bangalore')?.code, '29');
    assert.equal(normalizeGstState('Calcutta')?.code, '19');
  });

  it('maps the merged Daman & Diu spellings onto 26', () => {
    assert.equal(normalizeGstState('Daman')?.code, '26');
    assert.equal(normalizeGstState('Diu')?.code, '26');
    assert.equal(normalizeGstState('Daman and Diu')?.code, '26');
    assert.equal(normalizeGstState('Dadra and Nagar Haveli')?.code, '26');
  });

  it('does not revive the retired code 25 as its own jurisdiction', () => {
    // 25 was Daman & Diu before the merger. It resolves forward, and never to code 25.
    assert.notEqual(normalizeGstState('25')?.code, '25');
    assert.equal(normalizeGstState('25')?.code, '26');
    assert.equal(gstStateCodeOf('25'), '26');
  });

  it('returns nothing for an unknown or empty value', () => {
    assert.equal(normalizeGstState('Narnia'), null);
    assert.equal(normalizeGstState(''), null);
    assert.equal(normalizeGstState(null), null);
    assert.equal(normalizeGstState(undefined), null);
    assert.equal(gstStateCodeOf('Narnia'), '');
    assert.equal(gstStateName('Narnia'), '');
  });
});

describe('jurisdiction resolution', () => {
  it('treats a code and the matching name as intra-state', () => {
    assert.equal(resolveJurisdiction('Maharashtra', 'MH'), 'INTRA');
    assert.equal(resolveJurisdiction('27', 'Maharashtra'), 'INTRA');
    assert.equal(resolveJurisdiction('27', '27'), 'INTRA');
  });

  it('treats two different states as inter-state', () => {
    assert.equal(resolveJurisdiction('Maharashtra', 'Karnataka'), 'INTER');
    assert.equal(resolveJurisdiction('27', '29'), 'INTER');
  });

  it('falls back to a name compare when one side is unknown, but only when both are written', () => {
    // A master written before the registry existed, identical on both sides.
    assert.equal(resolveJurisdiction('Somewhereshire', 'Somewhereshire'), 'INTRA');
    assert.equal(resolveJurisdiction('Somewhereshire', 'Elsewhere'), 'INTER');
    // Nothing to compare.
    assert.equal(resolveJurisdiction('Maharashtra', ''), null);
    assert.equal(resolveJurisdiction('', ''), null);
  });
});
