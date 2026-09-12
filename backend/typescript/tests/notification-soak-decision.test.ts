import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  decideNotificationForRecipient,
  NOTIFICATION_DECISION_VERSION,
  NOTIFICATION_POLICY_VERSION,
} from '../src/platform/notifications/decision';
import { outcomeToRoute, routeToOutcome } from '../src/platform/notifications/decision-versions';
import { signalFieldsFromPayload } from '../src/platform/notifications/decision-audit';

describe('soak decision outcomes', () => {
  it('maps outcome to surface route', () => {
    assert.equal(outcomeToRoute('IMMEDIATE'), 'immediate');
    assert.equal(outcomeToRoute('DEFER_TO_DIGEST'), 'digest');
    assert.equal(outcomeToRoute('SUPPRESS'), 'suppress');
    assert.equal(routeToOutcome('digest'), 'DEFER_TO_DIGEST');
  });

  it('exposes decision and policy versions for calibration', () => {
    assert.equal(NOTIFICATION_DECISION_VERSION, 'v2.1');
    assert.match(NOTIFICATION_POLICY_VERSION, /^2026-09-soak-/);
  });

  it('extracts signal calibration fields from derived payloads', () => {
    const fields = signalFieldsFromPayload('SettlementSuggested', {
      derivedSignal: true,
      explanationCode: 'BALANCE_SETTLEABLE',
      dedupeKey: 'GROUP:m1:USER:u1:SETTLEMENT_SUGGESTED',
      contextType: 'GROUP',
      actionabilityScore: 72,
    });
    assert.equal(fields.signalName, 'SettlementSuggested');
    assert.equal(fields.explanationCode, 'BALANCE_SETTLEABLE');
    assert.equal(fields.actionabilityScore, 72);
  });

  it('suppresses muted recipients with an auditable reason', async () => {
    const fakePool = {
      query: async () => ({ rows: [{ n: '0' }], rowCount: 1 }),
    } as never;
    const decision = await decideNotificationForRecipient(fakePool, {
      eventName: 'GroupExpenseRecorded',
      actorUserId: 'actor',
      payload: { paidByUserId: 'payer' },
      prefs: {
        user_id: 'ravi',
        push_notifications_enabled: true,
        notification_categories: {},
        quiet_hours_start: null,
        quiet_hours_end: null,
        digest_enabled: false,
        timezone: 'UTC',
        notify_on_changes: false,
        notification_cadence: 'MUTED',
      },
      momentId: 'm1',
    });
    assert.equal(decision.outcome, 'SUPPRESS');
    assert.equal(decision.route, 'suppress');
    assert.equal(decision.suppressionReason, 'muted');
  });

  it('suppresses category-disabled recipients', async () => {
    const fakePool = {
      query: async () => ({ rows: [{ n: '0' }], rowCount: 1 }),
    } as never;
    const decision = await decideNotificationForRecipient(fakePool, {
      eventName: 'GroupExpenseRecorded',
      actorUserId: 'actor',
      payload: { paidByUserId: 'payer' },
      prefs: {
        user_id: 'ravi',
        push_notifications_enabled: true,
        notification_categories: { finance: false },
        quiet_hours_start: null,
        quiet_hours_end: null,
        digest_enabled: false,
        timezone: 'UTC',
        notify_on_changes: true,
        notification_cadence: 'ALL',
      },
      momentId: 'm1',
    });
    assert.equal(decision.outcome, 'SUPPRESS');
    assert.equal(decision.suppressionReason, 'category_disabled');
  });

  it('defers digest-only cadence as DEFER_TO_DIGEST not SUPPRESS', async () => {
    const fakePool = {
      query: async () => ({ rows: [{ n: '0' }], rowCount: 1 }),
    } as never;
    const decision = await decideNotificationForRecipient(fakePool, {
      eventName: 'GroupExpenseRecorded',
      actorUserId: 'actor',
      payload: {
        paidByUserId: 'payer',
        sharesByUserId: { ravi: '100' },
        title: 'Dinner',
      },
      prefs: {
        user_id: 'ravi',
        push_notifications_enabled: true,
        notification_categories: {},
        quiet_hours_start: null,
        quiet_hours_end: null,
        digest_enabled: false,
        timezone: 'UTC',
        notify_on_changes: true,
        notification_cadence: 'DIGEST_ONLY',
      },
      momentId: 'm1',
    });
    assert.equal(decision.outcome, 'DEFER_TO_DIGEST');
    assert.equal(decision.route, 'digest');
    assert.equal(decision.suppressionReason, 'digest_cadence');
    assert.ok(decision.title.length > 0);
  });
});
