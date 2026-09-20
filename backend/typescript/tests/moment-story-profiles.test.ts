import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  getStoryComposer,
  resolveStoryFamilyProfile,
} from '../src/modules/story/profiles';

describe('moment story family profiles', () => {
  it('maps HOUSE_PARTY', () => {
    const p = resolveStoryFamilyProfile({ momentTypeCode: 'HOUSE_PARTY', groupFamily: 'SHARED_EXPERIENCE' });
    assert.equal(p, 'HOUSE_PARTY');
    const c = getStoryComposer(p);
    assert.equal(c.displayLabel, 'House Party Story');
    assert.ok(c.moneyCategoriesHint.includes('Drinks'));
  });

  it('maps SHARED_PURCHASE types', () => {
    assert.equal(
      resolveStoryFamilyProfile({ momentTypeCode: 'GROUP_PURCHASE', groupFamily: 'SHARED_PURCHASE' }),
      'SHARED_PURCHASE'
    );
    const c = getStoryComposer('SHARED_PURCHASE');
    assert.match(c.shareBlurb('Camera Pool', { raised: '₹45k' }), /Camera Pool/);
  });

  it('maps SHARED_LIVING types', () => {
    assert.equal(
      resolveStoryFamilyProfile({ momentTypeCode: 'FLATMATES', groupFamily: 'SHARED_LIVING' }),
      'SHARED_LIVING'
    );
    const c = getStoryComposer('SHARED_LIVING');
    assert.equal(c.omitPlaces, true);
    assert.ok(c.metricKeys.some((m) => m.key === 'months'));
  });
});
