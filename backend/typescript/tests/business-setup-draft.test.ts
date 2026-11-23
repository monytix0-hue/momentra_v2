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
  OperatingSetupFieldError,
  applyOperatingSetupAliases,
  normalizeOperatingPreferences,
  type OperatingFamilyCode,
} from '../src/modules/business/operating-catalog';
import { AppError, ErrorCode } from '../src/platform/errors/errors';
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
    // Identity alone aliases into partyName, so the monthly plan is what blocks activation.
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          { subtype: 'TRADING_WHOLESALE', identityName: 'Northstar' },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.match(err.message, /budget must be a positive amount/i);
        assert.ok(err.fields.monthlyBudget);
        return true;
      }
    );
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
        channels: ['Retail buyers', 'Wholesale'],
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
    assert.deepEqual(stored.channels, ['Retail buyers', 'Wholesale']);
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

  it('maps retail Figma aliases into closed prefs and persists a full draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'RETAIL_STORE',
      storeName: 'Harbor Mart',
      companyName: 'Harbor Mart Pvt',
      locality: 'Andheri West',
      channels: ['Counter', 'Online'],
      planMonth: '2026-10',
      monthlyBudget: '₹8,40,000',
      monthlyTarget: '₹12,00,000',
      targetMetric: 'Revenue',
      salePriceIncludesGst: 'GST is added separately',
      format: 'Kirana',
    });
    assert.equal(aliased.identityName, 'Harbor Mart');
    assert.equal(aliased.partyName, 'Harbor Mart Pvt');
    assert.equal(aliased.location, 'Andheri West');
    assert.equal(aliased.startDate, '2026-10-01');
    assert.equal(aliased.salePriceIncludesGst, 'NO');
    // Retail must not treat format chips as the company/party name.
    assert.notEqual(aliased.partyName, 'Kirana');

    const draft = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RETAIL_STORE',
        storeName: 'Harbor Mart',
        companyName: 'Harbor Mart Pvt',
        currency: 'INR',
        location: 'Andheri West',
        channels: ['Counter'],
        monthlyBudget: '840000',
        monthlyTarget: '1200000',
        targetMetric: 'Revenue',
        gstTracking: 'YES',
        gstType: 'Regular',
        gstState: 'Maharashtra',
        gstin: '27AAGCB7383J1Z5',
        salePriceIncludesGst: 'GST is added separately',
        description: 'Neighborhood mart',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'RETAIL_STORE');
    assert.equal(draft.identityName, 'Harbor Mart');
    assert.equal(draft.partyName, 'Harbor Mart Pvt');
    assert.deepEqual(draft.channels, ['Counter']);
    assert.equal(draft.targetMetric, 'REVENUE');
    assert.equal(draft.gstRegistration, 'REGULAR');
    assert.equal(draft.salePriceIncludesGst, 'NO');
  });

  it('maps service monthlyChargeTarget alias and accepts a consumer-service draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'CONSUMER_SERVICE',
      serviceName: 'BrightCare',
      companyName: 'BrightCare',
      channels: ['Studio', 'Home'],
      monthlyBudget: '₹3,20,000',
      monthlyChargeTarget: '₹6,50,000',
      targetMetric: 'Revenue',
    });
    assert.equal(aliased.identityName, 'BrightCare');
    assert.equal(aliased.partyName, 'BrightCare');
    assert.equal(aliased.monthlyTarget, '₹6,50,000');

    const draft = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'CONSUMER_SERVICE',
        serviceName: 'BrightCare',
        companyName: 'BrightCare',
        channels: ['Studio'],
        monthlyBudget: '320000',
        monthlyChargeTarget: '650000',
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'CONSUMER_SERVICE');
    assert.equal(draft.identityName, 'BrightCare');
    assert.deepEqual(draft.channels, ['Studio']);
    assert.equal(draft.monthlyTarget, '650000.0000');
  });

  it('maps trading Figma aliases and accepts a trading draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'TRADING_WHOLESALE',
      businessName: 'Northstar Trading',
      companyName: 'Northstar Trading',
      channels: ['Retail buyers', 'Wholesale'],
      monthlyBudget: '₹60,00,000',
      monthlySalesTarget: '₹90,00,000',
      targetMetric: 'Revenue',
    });
    assert.equal(aliased.identityName, 'Northstar Trading');
    assert.equal(aliased.partyName, 'Northstar Trading');
    assert.equal(aliased.monthlyTarget, '₹90,00,000');

    const draft = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      {
        subtype: 'TRADING_WHOLESALE',
        businessName: 'Northstar Trading',
        companyName: 'Northstar Trading',
        channels: ['Retail buyers'],
        monthlyBudget: '6000000',
        monthlySalesTarget: '9000000',
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'TRADING_WHOLESALE');
    assert.equal(draft.identityName, 'Northstar Trading');
    assert.deepEqual(draft.channels, ['Retail buyers']);
    assert.equal(draft.monthlyTarget, '9000000.0000');
  });

  it('maps distribution Figma aliases and accepts a distribution draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'DISTRIBUTION_DEALERSHIP',
      businessName: 'Metro Distribution',
      companyName: 'Metro Distribution',
      channels: ['Retail dealers', 'Van sales'],
      monthlyBudget: '₹60,00,000',
      monthlySalesTarget: '₹90,00,000',
      targetMetric: 'Revenue',
    });
    assert.equal(aliased.identityName, 'Metro Distribution');
    assert.equal(aliased.partyName, 'Metro Distribution');
    assert.equal(aliased.monthlyTarget, '₹90,00,000');

    const draft = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      {
        subtype: 'DISTRIBUTION_DEALERSHIP',
        businessName: 'Metro Distribution',
        companyName: 'Metro Distribution',
        channels: ['Retail dealers'],
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'DISTRIBUTION_DEALERSHIP');
    assert.equal(draft.identityName, 'Metro Distribution');
    assert.deepEqual(draft.channels, ['Retail dealers']);
  });

  it('distribution activate rejects missing budget with companyName/businessName field keys', () => {
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          { subtype: 'DISTRIBUTION_DEALERSHIP' },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.equal(err.fields.businessName, 'Moment name is required.');
        return true;
      }
    );

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'TRADING_DISTRIBUTION',
          {
            subtype: 'DISTRIBUTION_DEALERSHIP',
            businessName: 'Metro Distribution',
            companyName: 'Metro Distribution',
          },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.equal(err.fields.monthlyBudget, 'Monthly budget must be a positive amount.');
        return true;
      }
    );
  });

  it('maps restaurant Figma aliases and accepts a restaurant draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'RESTAURANT_FOOD',
      restaurantName: 'Spice Route Kitchen',
      companyName: 'Spice Route Kitchen',
      channels: ['Dine-in', 'Swiggy'],
      monthlyBudget: '₹5,20,000',
      monthlySalesTarget: '₹6,50,000',
      targetMetric: 'Revenue',
    });
    assert.equal(aliased.outletName, 'Spice Route Kitchen');
    assert.equal(aliased.identityName, 'Spice Route Kitchen');
    assert.equal(aliased.partyName, 'Spice Route Kitchen');
    assert.equal(aliased.monthlyTarget, '₹6,50,000');

    const draft = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RESTAURANT_FOOD',
        outletName: 'Spice Route Kitchen',
        companyName: 'Spice Route Kitchen',
        channels: ['Dine-in'],
        monthlyBudget: '520000',
        monthlySalesTarget: '650000',
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'RESTAURANT_FOOD');
    assert.equal(draft.identityName, 'Spice Route Kitchen');
    assert.deepEqual(draft.channels, ['Dine-in']);
    assert.equal(draft.monthlyTarget, '650000.0000');
  });

  it('activates retail with Counter channel and rejects missing channels with field keys', () => {
    const active = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RETAIL_STORE',
        storeName: 'Harbor Mart',
        companyName: 'Harbor Mart',
        channels: ['Counter'],
        currency: 'INR',
      },
      creator
    );
    assert.equal(active.identityName, 'Harbor Mart');
    assert.deepEqual(active.channels, ['Counter']);

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'RETAIL_CONSUMER',
          {
            subtype: 'RETAIL_STORE',
            storeName: 'Harbor Mart',
            companyName: 'Harbor Mart',
          },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.match(err.message, /channel/i);
        assert.equal(err.fields.channels, 'Select at least one channel.');
        return true;
      }
    );

    try {
      validateAndMergeBusinessPreferences(
        'RETAIL_CONSUMER',
        {
          subtype: 'RETAIL_STORE',
          storeName: 'Harbor Mart',
          companyName: 'Harbor Mart',
        },
        creator
      );
      assert.fail('expected validation failure');
    } catch (err) {
      assert.ok(err instanceof AppError);
      assert.equal(err.code, ErrorCode.VALIDATION_FAILED);
      const details = err.details as { fields?: Record<string, string> };
      assert.equal(details.fields?.channels, 'Select at least one channel.');
    }
  });

  it('maps own marketing Figma aliases and accepts an own-campaign draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'OWN_BUSINESS_CAMPAIGN',
      campaignName: 'Harbor Spring',
      brandName: 'Harbor & Loom',
      channels: ['Paid social', 'Search'],
      ownMarketingBudget: '₹2,00,000',
      primaryTarget: '₹5,00,000',
      targetMetric: 'Leads',
      roles: ['Campaign Lead'],
      teamMembers: ['Priya'],
    });
    assert.equal(aliased.identityName, 'Harbor Spring');
    assert.equal(aliased.partyName, 'Harbor & Loom');
    assert.equal(aliased.marketingBudget, '₹2,00,000');
    assert.equal(aliased.conversionGoal, '₹5,00,000');
    assert.equal(aliased.monthlyBudget, '₹2,00,000');
    assert.equal(aliased.monthlyTarget, '₹5,00,000');
    assert.equal(aliased.targetMetric, 'LEADS');
    const participants = aliased.participants as Array<{ displayName: string; role: string }>;
    assert.equal(participants[0]?.displayName, 'Priya');
    assert.equal(participants[0]?.role, 'OPERATIONS_LEAD');

    const draft = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'OWN_BUSINESS_CAMPAIGN',
        campaignName: 'Harbor Spring',
        brandName: 'Harbor & Loom',
        channels: ['Paid social'],
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'OWN_BUSINESS_CAMPAIGN');
    assert.equal(draft.identityName, 'Harbor Spring');
    assert.equal(draft.partyName, 'Harbor & Loom');
    assert.deepEqual(draft.channels, ['Paid social']);
  });

  it('maps client campaign Figma aliases and accepts a client draft', () => {
    const aliased = applyOperatingSetupAliases({
      subtype: 'CLIENT_CAMPAIGN',
      campaignName: 'Brightline Q2',
      clientName: 'Northstar Residences',
      companyName: 'Brightline Creative',
      channels: ['Paid social', 'Search'],
      campaignBudget: '₹6,00,000',
      campaignPurpose: 'Leads',
      targetMetric: 'Leads',
      clientFee: '₹1,50,000',
      clientApprovedMediaBudget: '₹4,00,000',
      agencyOperatingBudget: '₹50,000',
    });
    assert.equal(aliased.identityName, 'Brightline Q2');
    assert.equal(aliased.partyName, 'Northstar Residences');
    assert.equal(aliased.monthlyBudget, '₹6,00,000');
    assert.equal(aliased.offering, 'Leads');
    assert.equal(aliased.targetMetric, 'LEADS');

    const draft = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'CLIENT_CAMPAIGN',
        campaignName: 'Brightline Q2',
        clientName: 'Northstar Residences',
        channels: ['Paid social'],
        currency: 'INR',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'CLIENT_CAMPAIGN');
    assert.equal(draft.identityName, 'Brightline Q2');
    assert.equal(draft.partyName, 'Northstar Residences');
    assert.deepEqual(draft.channels, ['Paid social']);
  });

  it('client campaign activate rejects with campaignName/clientName/monthlyBudget/startDate field keys', () => {
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'MARKETING_CAMPAIGNS',
          { subtype: 'CLIENT_CAMPAIGN' },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.equal(err.fields.campaignName, 'Moment name is required.');
        return true;
      }
    );

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'MARKETING_CAMPAIGNS',
          {
            subtype: 'CLIENT_CAMPAIGN',
            campaignName: 'Brightline Q2',
            companyName: 'Brightline Creative',
            monthlyBudget: '200000',
            startDate: '2026-10-12',
          },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.match(err.fields.clientName ?? '', /client/i);
        return true;
      }
    );

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'MARKETING_CAMPAIGNS',
          {
            subtype: 'CLIENT_CAMPAIGN',
            campaignName: 'Brightline Q2',
            clientName: 'Northstar Residences',
            startDate: '2026-10-12',
          },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.equal(err.fields.monthlyBudget, 'Monthly budget must be a positive amount.');
        return true;
      }
    );

    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'MARKETING_CAMPAIGNS',
          {
            subtype: 'CLIENT_CAMPAIGN',
            campaignName: 'Brightline Q2',
            clientName: 'Northstar Residences',
            monthlyBudget: '200000',
          },
          creator
        ),
      (err: unknown) => {
        assert.ok(err instanceof OperatingSetupFieldError);
        assert.equal(err.fields.startDate, 'Campaign start date is required.');
        return true;
      }
    );

    const active = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'CLIENT_CAMPAIGN',
        campaignName: 'Brightline Q2',
        clientName: 'Northstar Residences',
        monthlyBudget: '200000',
        clientFee: '150000',
        clientApprovedMediaBudget: '400000',
        agencyOperatingBudget: '50000',
        campaignPurpose: 'Leads',
        targetMetric: 'Leads',
        startDate: '2026-10-12',
        channels: ['Paid social'],
      },
      creator
    );
    assert.equal(active.identityName, 'Brightline Q2');
    assert.equal(active.partyName, 'Northstar Residences');
    assert.equal(active.clientFee, '150000.0000');
    assert.equal(active.clientApprovedMediaBudget, '400000.0000');
    assert.equal(active.agencyOperatingBudget, '50000.0000');
    assert.equal(active.offering, 'Leads');
    assert.equal(active.targetMetric, 'LEADS');
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

  it('stores Job-order (MADE_TO_ORDER) quote policy on draft and activate', () => {
    const draft = normalizeOperatingPreferences(
      'MANUFACTURING_PRODUCTION',
      {
        subtype: 'MADE_TO_ORDER',
        identityName: 'Forge & Form',
        quoteValidity: '15 days · expired quotes revise, never invoice',
        specificationTemplate: 'Drawing · material · tolerance · finish',
        orderApproval: 'No approval required',
        jobOnConfirm: 'Open job manually after confirm',
        productsBomNote: 'Skip for now · never forced',
        targetMetric: 'Invoiced value (or accepted job units)',
        priceIncludesGst: 'Prices include GST',
      },
      creator,
      { draft: true }
    );
    assert.equal(draft.subtype, 'MADE_TO_ORDER');
    assert.equal(draft.quoteValidity, '15 days · expired quotes revise, never invoice');
    assert.equal(draft.specificationTemplate, 'Drawing · material · tolerance · finish');
    assert.equal(draft.orderApproval, 'No approval required');
    assert.equal(draft.jobOnConfirm, 'NO');
    assert.equal(draft.productsBomNote, 'Skip for now · never forced');
    assert.equal(draft.targetMetric, 'REVENUE');
    assert.equal(draft.salePriceIncludesGst, 'YES');
    assert.equal(draft.planMonth, '');

    const active = normalizeOperatingPreferences(
      'MANUFACTURING_PRODUCTION',
      {
        subtype: 'MADE_TO_ORDER',
        identityName: 'Forge & Form',
        partyName: 'Forge & Form Pvt Ltd',
        monthlyBudget: '900000',
        monthlyTarget: '1800000',
        channels: ['Direct B2B', 'Job work'],
        targetMetric: 'Production output',
        quoteValidity: '30 days · expired quotes revise, never invoice',
        jobOnConfirm: 'Accepted order opens a job with target qty',
        gstTracking: 'NOT_NOW',
      },
      creator
    );
    assert.equal(active.targetMetric, 'PRODUCTION_OUTPUT');
    assert.equal(active.quoteValidity, '30 days · expired quotes revise, never invoice');
    assert.equal(active.jobOnConfirm, 'YES');
  });

});
