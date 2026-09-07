import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { computeOverallWellbeing } from '../src/modules/personal/personal-wellbeing';

describe('computeOverallWellbeing', () => {
  it('is order-independent across family inputs', () => {
    const loOnly = computeOverallWellbeing({
      recoveryScore: 70,
      rhythmScore: 80,
      widgetPayload: {},
    });
    assert.equal(loOnly, 75);

    const withRel = computeOverallWellbeing({
      recoveryScore: 70,
      rhythmScore: 80,
      widgetPayload: { bondIndex: 83 },
    });
    // equal weight of LO 75 and Rel 83
    assert.equal(withRel, Math.round((75 + 83) / 2));

    const withFutureLast = computeOverallWellbeing({
      recoveryScore: 70,
      rhythmScore: 80,
      widgetPayload: {
        bondIndex: 83,
        visionScore: 50,
        growthScore: 60,
        momentumScore: 55,
        disciplineScore: 59,
      },
    });
    const futureAvg = Math.round((50 + 60 + 55 + 59) / 4);
    assert.equal(withFutureLast, Math.round((75 + futureAvg + 83) / 3));

    // Same inputs different object construction → same result
    const again = computeOverallWellbeing({
      recoveryScore: 70,
      rhythmScore: 80,
      widgetPayload: {
        disciplineScore: 59,
        bondIndex: 83,
        growthScore: 60,
        visionScore: 50,
        momentumScore: 55,
      },
    });
    assert.equal(again, withFutureLast);
  });

  it('applies LO adjust bias only to LO contribution', () => {
    const base = computeOverallWellbeing({
      recoveryScore: 60,
      rhythmScore: 60,
      widgetPayload: { bondIndex: 80 },
    });
    const biased = computeOverallWellbeing({
      recoveryScore: 60,
      rhythmScore: 60,
      widgetPayload: { bondIndex: 80, lifeOpsAdjustBias: 5 },
    });
    // LO becomes 65; avg(65, 80) = 73 vs avg(60, 80) = 70
    assert.equal(base, 70);
    assert.equal(biased, 73);
  });

  it('ignores missing families', () => {
    assert.equal(
      computeOverallWellbeing({
        recoveryScore: null,
        rhythmScore: null,
        widgetPayload: { vitalityScore: 88 },
      }),
      88
    );
    assert.equal(
      computeOverallWellbeing({
        recoveryScore: null,
        rhythmScore: null,
        widgetPayload: {},
      }),
      null
    );
  });

  it('does not use recovery/rhythm as Future stand-ins', () => {
    // Only LO columns set — Future payload empty → LO-only score
    assert.equal(
      computeOverallWellbeing({
        recoveryScore: 40,
        rhythmScore: 40,
        widgetPayload: {},
      }),
      40
    );
  });
});
