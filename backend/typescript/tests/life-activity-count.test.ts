import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  lifeActivityCountExclusionSql,
  tallyLifeActivityFamilies,
} from '../src/modules/projection/life-activity-count';

describe('Life activity family counts', () => {
  it('counts a FOOD expense once on Everyday and not on the inferred Lifestyle mirror', () => {
    const counts = tallyLifeActivityFamilies([
      { activityCode: 'EXPENSE_RECORDED', source: null, family: 'LIFE_OPERATIONS' },
      { activityCode: 'LIFESTYLE_EXPERIENCE', source: 'MASTER_EXPENSE', family: 'LIFESTYLE' },
    ]);
    assert.equal(counts.LIFE_OPERATIONS, 1);
    assert.equal(counts.LIFESTYLE, 0);
  });

  it('still counts an explicit shared-experience row', () => {
    const counts = tallyLifeActivityFamilies([
      { activityCode: 'EXPENSE_RECORDED', source: null, family: 'LIFE_OPERATIONS' },
      { activityCode: 'RELATIONSHIP_SHARED_EXPERIENCE', source: 'MASTER_EXPENSE', family: 'RELATIONSHIPS' },
    ]);
    assert.equal(counts.LIFE_OPERATIONS, 1);
    assert.equal(counts.RELATIONSHIPS, 1);
  });

  it('puts the same skip into the Life SQL', () => {
    const sql = lifeActivityCountExclusionSql('ra');
    assert.match(sql, /MASTER_EXPENSE/);
    assert.match(sql, /LIFESTYLE_EXPERIENCE/);
    assert.match(sql, /ra\.activity_payload/);
  });
});
