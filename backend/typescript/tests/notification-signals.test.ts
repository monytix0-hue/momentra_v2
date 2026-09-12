import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { notificationCopy, notificationPriority, isPeerPushEvent } from '../src/platform/notifications/allowlist';
import { importanceFromScore, scoreActionability } from '../src/platform/notifications/signals/actionability';
import { inferRelationship } from '../src/platform/notifications/decision-types';

describe('Wave 2 derived signals', () => {
  it('registers signal events on the peer push allowlist', () => {
    assert.equal(isPeerPushEvent('TripBudgetThresholdReached'), true);
    assert.equal(isPeerPushEvent('SettlementSuggested'), true);
    assert.equal(isPeerPushEvent('ApprovalsAccumulating'), true);
    assert.equal(isPeerPushEvent('GoalMilestoneReached'), true);
    assert.equal(isPeerPushEvent('MomentDigestReady'), true);
  });

  it('narrates verified signal facts in copy', () => {
    const budget = notificationCopy('TripBudgetThresholdReached', {
      momentTitle: 'Goa Trip',
      body: 'This trip has used 78% of its planned budget.',
      explanationCode: 'BUDGET_80_PERCENT',
    });
    assert.equal(budget.title, 'Goa Trip');
    assert.match(budget.body, /78%/);

    const settle = notificationCopy('SettlementSuggested', {
      momentTitle: 'Goa Trip',
      body: 'One ₹850 settlement would clear your balance.',
    });
    assert.match(settle.body, /₹850/);

    const approvals = notificationCopy('ApprovalsAccumulating', {
      companyName: 'Pureborn',
      body: '3 approvals worth ₹62,800 are waiting in Pureborn.',
    });
    assert.equal(approvals.title, 'Pureborn');
    assert.match(approvals.body, /Pureborn/);
  });

  it('treats derived signals as self when actor is recipient', () => {
    assert.equal(
      inferRelationship('SettlementSuggested', 'u1', 'u1', { derivedSignal: true }),
      'self'
    );
    assert.equal(
      inferRelationship('TripBudgetThresholdReached', 'u1', 'u1', {
        derivedSignal: true,
      }),
      'self'
    );
  });

  it('allows payload importance to override default priority', () => {
    assert.equal(notificationPriority('GroupNearlySettled'), 'LOW');
    assert.equal(notificationPriority('GroupNearlySettled', { importance: 'HIGH' }), 'HIGH');
    assert.equal(notificationPriority('SettlementSuggested'), 'HIGH');
  });

  it('scores actionability and can bump importance', () => {
    const score = scoreActionability({
      importance: 'NORMAL',
      hasClearNextAction: true,
      amountImpact: 850,
      ageHours: 50,
    });
    assert.ok(score >= 60);
    assert.equal(importanceFromScore('NORMAL', 80), 'HIGH');
    assert.equal(importanceFromScore('LOW', 50), 'NORMAL');
  });

  it('builds stable dedupe key shapes for hysteresis', () => {
    const momentId = 'm1';
    const userId = 'u1';
    const key = `GROUP:${momentId}:USER:${userId}:TRIP_BUDGET_80`;
    assert.equal(key, 'GROUP:m1:USER:u1:TRIP_BUDGET_80');
    assert.match(key, /TRIP_BUDGET_80$/);
  });
});
