import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { notificationCopy } from '../src/platform/notifications/allowlist';
import {
  inferRelationship,
  moneyLabel,
  cadenceFromNotifyFlag,
} from '../src/platform/notifications/decision-types';
import { threadKeyFor, collapseKeyFor } from '../src/platform/notifications/thread-key';

describe('personalized notification copy', () => {
  it('builds consequence copy for splittee with canonical share', () => {
    const copy = notificationCopy(
      'GroupExpenseRecorded',
      {
        actorDisplayName: 'Sam',
        title: 'Dinner',
        amount: '1240',
        currencyCode: 'INR',
        momentTitle: 'Goa Trip',
        paidByUserId: 'payer',
        sharesByUserId: { ravi: '310' },
      },
      {
        userId: 'ravi',
        relationship: 'splittee',
        cadence: 'ALL',
        recipientShare: '310',
        currencyCode: 'INR',
        momentTitle: 'Goa Trip',
      }
    );
    assert.match(copy.title, /Goa Trip/);
    assert.match(copy.title, /Dinner/);
    assert.match(copy.body, /Your share/);
    assert.match(copy.body, /₹310/);
  });

  it('builds settlement copy for payee', () => {
    const copy = notificationCopy(
      'SettlementRecorded',
      {
        amount: '500',
        currencyCode: 'INR',
        momentTitle: 'Goa Trip',
        payerDisplayName: 'Santosh',
        payeeUserId: 'ravi',
        payerUserId: 'santosh',
      },
      {
        userId: 'ravi',
        relationship: 'payee',
        cadence: 'ALL',
        currencyCode: 'INR',
        momentTitle: 'Goa Trip',
      }
    );
    assert.equal(copy.title, 'Goa Trip · Settlement');
    assert.match(copy.body, /Santosh paid you/);
    assert.match(copy.body, /₹500/);
  });

  it('infers settlement relationships', () => {
    assert.equal(
      inferRelationship('SettlementRecorded', 'ravi', 'actor', {
        payerUserId: 'santosh',
        payeeUserId: 'ravi',
      }),
      'payee'
    );
    assert.equal(
      inferRelationship('SettlementRecorded', 'priya', 'actor', {
        payerUserId: 'santosh',
        payeeUserId: 'ravi',
      }),
      'uninvolved'
    );
    assert.equal(
      inferRelationship('SettlementRecorded', 'actor', 'actor', {
        payerUserId: 'santosh',
        payeeUserId: 'ravi',
      }),
      'actor'
    );
  });

  it('maps mute cadence from notify flag', () => {
    assert.equal(cadenceFromNotifyFlag(false, 'ALL'), 'MUTED');
    assert.equal(cadenceFromNotifyFlag(true, 'DIGEST_ONLY'), 'DIGEST_ONLY');
  });

  it('formats INR money labels', () => {
    assert.equal(moneyLabel('1240', 'INR'), '₹1,240');
  });

  it('builds thread keys and collapses only when replacePrior', () => {
    assert.equal(threadKeyFor({ domainCode: 'GROUP', momentId: 'm1' }), 'GROUP:m1');
    assert.equal(
      threadKeyFor({ domainCode: 'BUSINESS', momentId: 'm1', companyId: 'c1' }),
      'BUSINESS:c1:m1'
    );
    assert.equal(collapseKeyFor({ threadKey: 'GROUP:m1', replacePrior: false }), null);
    assert.equal(collapseKeyFor({ threadKey: 'GROUP:m1', replacePrior: true }), 'GROUP:m1');
  });
});
