import { describe, expect, it } from 'vitest';
import {
  personalFamilyFromMomentTypeCode,
  personalFamilyLabel,
} from '../src/modules/personal/moment-family';

describe('personalFamilyFromMomentTypeCode', () => {
  it('maps Life Ops / Everyday types', () => {
    expect(personalFamilyFromMomentTypeCode('LIFE_RHYTHM')).toBe('LIFE_OPERATIONS');
    expect(personalFamilyFromMomentTypeCode('LIFE_OPERATIONS')).toBe('LIFE_OPERATIONS');
    expect(personalFamilyFromMomentTypeCode('LIFE_FOCUS')).toBe('LIFE_OPERATIONS');
  });

  it('maps Future / Lifestyle / Relationships', () => {
    expect(personalFamilyFromMomentTypeCode('FUTURE_GOAL')).toBe('FUTURE_BUILDING');
    expect(personalFamilyFromMomentTypeCode('LIFESTYLE')).toBe('LIFESTYLE');
    expect(personalFamilyFromMomentTypeCode('RELATIONSHIP_CONNECTION')).toBe('RELATIONSHIPS');
  });

  it('labels match Unified Personal chrome', () => {
    expect(personalFamilyLabel('LIFE_OPERATIONS')).toBe('Everyday');
    expect(personalFamilyLabel('FUTURE_BUILDING')).toBe('Future');
    expect(personalFamilyLabel('LIFESTYLE')).toBe('Lifestyle');
    expect(personalFamilyLabel('RELATIONSHIPS')).toBe('People');
  });
});
