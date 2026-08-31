import { describe, expect, it } from 'vitest';
import {
  futureItemPrecisionSchema,
  futureProfileSchema,
} from '../src/modules/personal/future-precision';
import {
  lifestyleActivityPrecisionSchema,
  lifestyleProfileSchema,
} from '../src/modules/personal/lifestyle-precision';
import {
  relationshipActivityPrecisionSchema,
  relationshipsProfileSchema,
} from '../src/modules/personal/relationships-precision';
import { createSettlementSchema } from '../src/modules/finance/group-expense';

describe('Personal family precision contracts (PX)', () => {
  it('accepts future profile upsert', () => {
    const parsed = futureProfileSchema.parse({
      buildingFocusCode: 'CAREER',
      focusHorizonCode: 'TWELVE_MONTHS',
      primaryValueCode: 'FREEDOM',
    });
    expect(parsed.buildingFocusCode).toBe('CAREER');
  });

  it('accepts enriched future item with opportunity type', () => {
    const parsed = futureItemPrecisionSchema.parse({
      kind: 'OPPORTUNITY',
      title: 'Role change',
      opportunityType: 'CAREER',
      targetDate: '2026-12-01',
    });
    expect(parsed.opportunityType).toBe('CAREER');
  });

  it('accepts future progress with GOAL type', () => {
    const parsed = futureItemPrecisionSchema.parse({
      kind: 'PROGRESS',
      title: 'Ship MVP',
      progressType: 'GOAL',
      progressValue: 40,
      unitCode: 'PCT',
    });
    expect(parsed.progressType).toBe('GOAL');
  });

  it('accepts lifestyle activity with location', () => {
    const parsed = lifestyleActivityPrecisionSchema.parse({
      lifestyleContext: 'EXPERIENCE',
      title: 'Sunset walk',
      locationText: 'Marina',
      wellbeingRating: 8,
    });
    expect(parsed.locationText).toBe('Marina');
  });

  it('accepts lifestyle profile', () => {
    const parsed = lifestyleProfileSchema.parse({
      lifestyleFocusCode: 'JOY',
      explorationBiasCode: 'HIGH',
    });
    expect(parsed.lifestyleFocusCode).toBe('JOY');
  });

  it('accepts relationship activity with investment', () => {
    const parsed = relationshipActivityPrecisionSchema.parse({
      activityKind: 'INVESTMENT',
      displayName: 'Alex',
      investmentValue: 50,
      unitCode: 'INR',
      relationshipType: 'FRIEND',
    });
    expect(parsed.investmentValue).toBe(50);
  });

  it('accepts relationships profile', () => {
    const parsed = relationshipsProfileSchema.parse({
      bondFocusCode: 'TRUST',
      carePriorityCode: 'FAMILY',
    });
    expect(parsed.bondFocusCode).toBe('TRUST');
  });

  it('rejects empty future profile extras but allows empty object', () => {
    expect(futureProfileSchema.parse({})).toEqual({});
  });
});

describe('GX-1 settlement contract', () => {
  it('accepts settlement body', () => {
    const parsed = createSettlementSchema.parse({
      payerParticipantId: '11111111-1111-4111-8111-111111111111',
      payeeParticipantId: '22222222-2222-4222-8222-222222222222',
      amount: '100.00',
      currencyCode: 'INR',
    });
    expect(parsed.currencyCode).toBe('INR');
  });
});
