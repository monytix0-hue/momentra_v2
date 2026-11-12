/**
 * The six-section setup document writes into a DRAFT moment, then activates it.
 *
 * Two things have to hold for that to be safe:
 *  - a draft is *incomplete by definition*, so its preferences normalise leniently (no
 *    "budget must be positive" rejections while the operator is still typing);
 *  - activation stays strict, so a half-filled document can never become ACTIVE;
 *  - every key the document emits is one the operating contract accepts.
 *
 * These run without a database: they pin the rules the clients depend on.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  OPERATING_FAMILIES,
  normalizeOperatingPreferences,
  type OperatingFamilyCode,
} from '../src/modules/business/operating-catalog';
import { updateBusinessSetupDraftSchema } from '../src/modules/moment/service';
import { validateAndMergeBusinessPreferences } from '../src/modules/moment/setup-preferences';

const creator = { userId: '11111111-1111-1111-1111-111111111111', displayName: 'Owner' };

/** Every preference key the setup document emits, per the plan's contract. */
const DOCUMENT_KEYS = [
  'subtype',
  'identityName',
  'partyName',
  'description',
  'currency',
  'startDate',
  'endDate',
  'location',
  'gstTracking',
  'gstRegistration',
  'gstState',
  'gstStateCode',
  'gstin',
  'salePriceIncludesGst',
  'purchasePriceIncludesGst',
  'gstEffectiveFrom',
  'channels',
  'monthlyBudget',
  'monthlyTarget',
  'targetMetric',
  'participants',
  'counterparties',
];

describe('business setup draft preferences', () => {
  it('accepts a half-filled document without forcing budget or target', () => {
    const prefs = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      { subtype: 'TRADING_WHOLESALE', identityName: 'Northstar' },
      creator,
      { draft: true }
    );
    assert.equal(prefs.subtype, 'TRADING_WHOLESALE');
    assert.equal(prefs.identityName, 'Northstar');
    assert.equal(prefs.monthlyBudget, '');
    assert.equal(prefs.monthlyTarget, '');
    // The creator is always the owner, even before the People section is touched.
    assert.equal(prefs.participants[0]?.role, 'OWNER_ADMIN');
    assert.equal(prefs.participants[0]?.userId, creator.userId);
  });

  it('stores an unanswered target measure as the subtype default rather than rejecting', () => {
    const prefs = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      { subtype: 'DISTRIBUTION_DEALERSHIP', targetMetric: 'NOT_A_METRIC' },
      creator,
      { draft: true }
    );
    assert.equal(prefs.targetMetric, 'REVENUE');
  });

  it('still rejects an incomplete setup at activation', () => {
    // Strict normalisation checks identity before the plan fields, so this trips the
    // party guard first — either way the document cannot activate half-filled.
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          { subtype: 'TRADING_WHOLESALE', identityName: 'Northstar' },
          creator
        ),
      /Business name is required/i
    );
    // With identity supplied, the missing monthly plan is what blocks activation.
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          { subtype: 'TRADING_WHOLESALE', identityName: 'Northstar', partyName: 'Northstar' },
          creator
        ),
      /budget must be a positive amount/i
    );
  });

  it('still rejects an unknown subtype at activation', () => {
    assert.throws(
      () => normalizeOperatingPreferences('TRADING_DISTRIBUTION', { subtype: 'NOPE' }, creator),
      /Unknown subtype/i
    );
  });

  it('keeps both verticals on the same family and accepts every emitted key', () => {
    const family = OPERATING_FAMILIES.find((f) => f.familyCode === 'TRADING_DISTRIBUTION');
    assert.ok(family, 'TRADING_DISTRIBUTION family must exist');
    assert.deepEqual(
      family!.subtypes.map((s) => s.code),
      ['TRADING_WHOLESALE', 'DISTRIBUTION_DEALERSHIP']
    );

    const stored = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      {
        subtype: 'TRADING_WHOLESALE',
        identityName: 'Northstar Trading',
        partyName: 'Northstar',
        description: 'Wholesale trade',
        currency: 'INR',
        startDate: '2026-11-01',
        endDate: '2027-03-31',
        location: 'Pune',
        gstTracking: 'YES',
        gstRegistration: 'REGULAR',
        gstState: 'MH',
        gstin: '27AAAAA0000A1Z5',
        salePriceIncludesGst: 'YES',
        purchasePriceIncludesGst: 'NO',
        gstEffectiveFrom: '2026-11-01',
        channels: ['Wholesale', 'Retail'],
        monthlyBudget: '250000',
        monthlyTarget: '500000',
        targetMetric: 'REVENUE',
        participants: [{ displayName: 'Asha', role: 'CONTRIBUTOR' }],
        counterparties: [{ name: 'Verma Traders', kind: 'DEALER' }],
      },
      creator,
      { draft: true }
    );

    for (const key of DOCUMENT_KEYS) {
      assert.ok(key in stored, `document emits ${key} but the contract does not store it`);
    }
    assert.deepEqual(stored.channels, ['Wholesale', 'Retail']);
    // The state is stored canonically with its code, so `MH` can never be compared as a name.
    assert.equal(stored.gstState, 'Maharashtra');
    assert.equal(stored.gstStateCode, '27');
    assert.equal(stored.gstRegistration, 'REGULAR');
    assert.equal(stored.purchasePriceIncludesGst, 'NO');
    assert.equal(stored.counterparties[0]?.name, 'Verma Traders');
  });

  it('keeps a bad GSTIN through a draft save but rejects it at activation', () => {
    // A draft is mid-document: a half-typed GSTIN must survive the save, not block it.
    const lenient = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      {
        subtype: 'TRADING_WHOLESALE',
        identityName: 'Northstar',
        partyName: 'Northstar',
        monthlyBudget: '250000',
        monthlyTarget: '500000',
        gstState: 'Maharashtra',
        gstin: '27aaaaa0000a1z5',
      },
      creator,
      { draft: true }
    );
    // Uppercased and trimmed, but not checksum-checked.
    assert.equal(lenient.gstin, '27AAAAA0000A1Z5');
    assert.equal(lenient.gstStateCode, '27');

    // Activation is strict: the check digit on this GSTIN is wrong.
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          {
            subtype: 'TRADING_WHOLESALE',
            identityName: 'Northstar',
            partyName: 'Northstar',
            monthlyBudget: '250000',
            monthlyTarget: '500000',
            gstState: 'Maharashtra',
            gstin: '27AAAAA0000A1Z5',
          },
          creator
        ),
      /checksum/i
    );
  });

  it('rejects a GSTIN issued by a different state at activation', () => {
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          {
            subtype: 'TRADING_WHOLESALE',
            identityName: 'Northstar',
            partyName: 'Northstar',
            monthlyBudget: '250000',
            monthlyTarget: '500000',
            // A genuine Karnataka GSTIN against a Maharashtra registration.
            gstState: 'Maharashtra',
            gstin: '29AAGCB7383J1Z4',
          },
          creator
        ),
      /29.*Karnataka.*27.*Maharashtra/i
    );
  });

  it('rejects an unrecognised state name at activation but keeps it on a draft', () => {
    const base = {
      subtype: 'TRADING_WHOLESALE',
      identityName: 'Northstar',
      partyName: 'Northstar',
      monthlyBudget: '250000',
      monthlyTarget: '500000',
      gstState: 'Atlantis',
    };
    const lenient = normalizeOperatingPreferences('TRADING_DISTRIBUTION', base, creator, { draft: true });
    assert.equal(lenient.gstState, 'Atlantis');
    assert.equal(lenient.gstStateCode, '');
    assert.throws(
      () => normalizeOperatingPreferences('TRADING_DISTRIBUTION', base, creator),
      /not an Indian state/i
    );
  });

  it('validates the draft patch body and nothing extra', () => {
    const parsed = updateBusinessSetupDraftSchema.parse({
      preferences: { subtype: 'TRADING_WHOLESALE' },
    });
    assert.deepEqual(parsed.preferences, { subtype: 'TRADING_WHOLESALE' });
    assert.throws(() => updateBusinessSetupDraftSchema.parse({ preferences: {}, momentId: 'x' }));
  });

  it('routes the draft flag through validateAndMergeBusinessPreferences', () => {
    const lenient = validateAndMergeBusinessPreferences(
      'TRADING_DISTRIBUTION' as OperatingFamilyCode,
      { subtype: 'DISTRIBUTION_DEALERSHIP' },
      creator,
      { draft: true }
    );
    assert.equal(lenient.subtype, 'DISTRIBUTION_DEALERSHIP');
    assert.throws(() =>
      validateAndMergeBusinessPreferences(
        'TRADING_DISTRIBUTION' as OperatingFamilyCode,
        { subtype: 'DISTRIBUTION_DEALERSHIP' },
        creator
      )
    );
  });

  it('accepts a marketing draft flip Client → Own and clears clientFee on Own activate', () => {
    const draftFlip = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'OWN_BUSINESS_CAMPAIGN',
        identityName: 'Spring launch',
        partyName: 'Northstar',
        monthlyBudget: '50000',
        startDate: '2026-11-01',
        clientFee: '15000',
        channels: ['Paid Social'],
      },
      creator,
      { draft: true }
    );
    assert.equal(draftFlip.subtype, 'OWN_BUSINESS_CAMPAIGN');
    assert.equal(draftFlip.clientFee, '');

    const client = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'CLIENT_CAMPAIGN',
        identityName: 'Brightline Q2',
        partyName: 'Brightline',
        monthlyBudget: '200000',
        startDate: '2026-11-01',
        clientFee: '15000',
        channels: ['Paid Social', 'Search Ads'],
      },
      creator
    );
    assert.equal(client.subtype, 'CLIENT_CAMPAIGN');
    assert.match(client.clientFee, /^15000(\.0+)?$/);
    assert.equal(client.partyName, 'Brightline');

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'MARKETING_CAMPAIGNS',
          {
            subtype: 'CLIENT_CAMPAIGN',
            identityName: 'Brightline Q2',
            partyName: '',
            monthlyBudget: '200000',
            startDate: '2026-11-01',
          },
          creator
        ),
      /Client/i
    );
  });
});
