import { describe, expect, it } from 'vitest';
import {
  attentionCaptureSchema,
  lifeOpsAdjustSchema,
  lifeOpsProfileSchema,
  observationDetailSchema,
} from '../src/modules/personal/life-ops-precision';
import { observationSchema } from '../src/modules/personal/service';
import { createExpenseSchema } from '../src/modules/finance/service';

describe('Life Ops precision contracts', () => {
  it('accepts attention capture payload', () => {
    const parsed = attentionCaptureSchema.parse({
      categoryCode: 'WORK',
      intensityCode: 'MODERATE',
      timeBlockCode: 'MORNING',
      energyRemaining: 3,
    });
    expect(parsed.categoryCode).toBe('WORK');
  });

  it('accepts life ops adjust with rhythm action', () => {
    const parsed = lifeOpsAdjustSchema.parse({
      rhythmActionCode: 'REDUCE_LOAD',
      signalDirectionCode: 'DECREASE_PRESSURE',
      reason: 'Need recovery',
    });
    expect(parsed.rhythmActionCode).toBe('REDUCE_LOAD');
  });

  it('rejects empty adjust', () => {
    expect(() => lifeOpsAdjustSchema.parse({})).toThrow();
  });

  it('accepts profile upsert with priorities', () => {
    const parsed = lifeOpsProfileSchema.parse({
      lifeFocusCode: 'BALANCE',
      priorities: [{ priorityCode: 'HEALTH', weightPct: 80 }],
      anchors: [{ displayName: 'Morning walk', anchorCode: 'WALK' }],
    });
    expect(parsed.priorities?.[0]?.priorityCode).toBe('HEALTH');
  });

  it('observation schema accepts mood detail fields', () => {
    const parsed = observationSchema.parse({
      observationType: 'MOOD',
      numericValue: 8,
      feelingStateCode: 'CALM',
      moodDrivers: ['WORK', 'REST'],
    });
    expect(parsed.feelingStateCode).toBe('CALM');
  });

  it('observation detail schema validates recovery activity', () => {
    const parsed = observationDetailSchema.parse({
      activityTypeCode: 'SLEEP',
      durationMinutes: 30,
      energyAfterPct: 70,
    });
    expect(parsed.activityTypeCode).toBe('SLEEP');
  });

  it('create expense accepts tags and planning class', () => {
    const parsed = createExpenseSchema.parse({
      amount: '120.50',
      currencyCode: 'INR',
      categoryCode: 'FOOD',
      subcategoryCode: 'GROCERIES',
      planningClassCode: 'ESSENTIAL',
      tags: ['weekly', 'home'],
      note: 'Market run',
    });
    expect(parsed.tags).toEqual(['weekly', 'home']);
  });
});
