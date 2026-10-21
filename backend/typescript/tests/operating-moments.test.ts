import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  canSubmit,
  financeVisibleFor,
  needsMakerChecker,
  normalizeOperatingPreferences,
  projectOperatingMoment,
  quickAddDef,
  validateOperatingAllocation,
  type OperatingEventRow,
  type OperatingFamilyCode,
} from '../src/modules/business/operating-catalog';
import { recordOperatingEventSchema } from '../src/modules/business/operating-service';

describe('operating moment catalog', () => {
  it('hides finance totals from a team member and shows them to finance', () => {
    assert.equal(financeVisibleFor('TEAM_MEMBER'), false);
    assert.equal(financeVisibleFor('FINANCE'), true);
    assert.equal(financeVisibleFor('OWNER_ADMIN'), true);
    const prefs = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'CLIENT_CAMPAIGN',
        identityName: 'Summer launch',
        partyName: 'Northwind',
        channels: ['Instagram'],
        monthlyBudget: '10000',
        monthlyTarget: '25',
        targetMetric: 'LEADS',
      },
      { userId: 'owner-1', displayName: 'Asha' }
    );
    const events: OperatingEventRow[] = [
      {
        operatingEventId: 'e1',
        eventType: 'SPEND',
        title: 'Creator fee',
        note: null,
        amount: '2500.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Instagram',
        paymentRelevance: 'PAYABLE',
        paymentStatus: 'PENDING',
        approvalStatus: 'NONE',
        submittedByUserId: 'owner-1',
        decidedByUserId: null,
        occurredAt: '2026-10-01T00:00:00.000Z',
      },
    ];
    const staff = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs,
      role: 'TEAM_MEMBER',
      events,
    });
    const finance = projectOperatingMoment({
      familyCode: 'MARKETING_CAMPAIGNS',
      prefs,
      role: 'FINANCE',
      events,
    });
    assert.equal(staff.budget, null);
    assert.equal(staff.paymentReadiness.length, 0);
    assert.equal(finance.budget?.spent, '2500.0000');
    assert.equal(finance.budget?.remaining, '7500.0000');
    assert.equal(finance.paymentReadiness.length, 1);
    assert.equal(staff.boards.life.find((section) => section.code === 'L3')?.rows[0]?.amount, null);
    assert.equal(finance.boards.life.find((section) => section.code === 'L3')?.rows[0]?.amount, '2500.0000');
    assert.equal(staff.boards.memory.find((section) => section.code === 'ME2')?.rows[0]?.amount, null);
    assert.equal(finance.boards.memory.find((section) => section.code === 'ME2')?.rows[0]?.amount, '2500.0000');
    assert.equal(staff.quickAdds.some((q) => q.code === 'PAYMENT'), false);
    assert.equal(finance.quickAdds.some((q) => q.code === 'PAYMENT'), true);
  });

  it('requires a different approver for a contributor spend', () => {
    const spend = quickAddDef('SPEND');
    assert.ok(spend);
    assert.equal(needsMakerChecker('CONTRIBUTOR', spend), true);
    assert.equal(needsMakerChecker('FINANCE', spend), false);
    assert.equal(canSubmit('OBSERVER', spend), false);
    assert.equal(canSubmit('CONTRIBUTOR', spend), true);
  });

  it('keeps a retail store location and activation policy', () => {
    const prefs = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RETAIL_STORE',
        identityName: 'Store',
        partyName: 'Store',
        monthlyBudget: '1200',
        monthlyTarget: '3000',
        targetMetric: 'REVENUE',
        location: 'Market road',
        activationPolicy: 'APPROVAL_REQUIRED',
        channels: ['Store'],
      },
      { userId: 'u', displayName: 'Asha' }
    );
    assert.equal(prefs.location, 'Market road');
    assert.equal(prefs.activationPolicy, 'APPROVAL_REQUIRED');
    assert.deepEqual(prefs.channels, ['Store']);
  });

  it('keeps restaurant and consumer service subtype, channels, location, and activation policy', () => {
    const restaurant = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RESTAURANT_FOOD',
        identityName: 'Kitchen',
        partyName: 'Kitchen',
        monthlyBudget: '1200',
        monthlyTarget: '3000',
        targetMetric: 'REVENUE',
        location: 'Market road',
        activationPolicy: 'APPROVAL_REQUIRED',
        channels: ['Dine-in', 'Zomato'],
      },
      { userId: 'u', displayName: 'Asha' }
    );
    assert.equal(restaurant.subtype, 'RESTAURANT_FOOD');
    assert.equal(restaurant.location, 'Market road');
    assert.equal(restaurant.activationPolicy, 'APPROVAL_REQUIRED');
    assert.deepEqual(restaurant.channels, ['Dine-in', 'Zomato']);

    const service = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'CONSUMER_SERVICE',
        identityName: 'Care',
        partyName: 'Care',
        monthlyBudget: '800',
        monthlyTarget: '2000',
        targetMetric: 'REVENUE',
        location: 'Lane 4',
        activationPolicy: 'NONE',
        channels: ['Walk-in', 'Home service'],
      },
      { userId: 'u', displayName: 'Asha' }
    );
    assert.equal(service.subtype, 'CONSUMER_SERVICE');
    assert.equal(service.location, 'Lane 4');
    assert.equal(service.activationPolicy, 'NONE');
    assert.deepEqual(service.channels, ['Walk-in', 'Home service']);
  });

  it('projects a retail sale channel and hides amounts from staff', () => {
    const prefs = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RETAIL_STORE',
        identityName: 'Store',
        partyName: 'Store',
        monthlyBudget: '10000',
        monthlyTarget: '30000',
        targetMetric: 'REVENUE',
        channels: ['Store'],
        startDate: '2026-10-01',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    const events: OperatingEventRow[] = [
      {
        operatingEventId: 'sale-1',
        eventType: 'SALE',
        title: 'Counter sale',
        note: null,
        amount: '1800.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Store',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-02T00:00:00.000Z',
      },
      {
        operatingEventId: 'issue-1',
        eventType: 'ISSUE',
        title: 'Shelf low',
        note: 'Front row',
        amount: null,
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: null,
        paymentRelevance: 'NONE',
        paymentStatus: 'NONE',
        approvalStatus: 'PENDING',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-03T00:00:00.000Z',
      },
    ];
    const finance = projectOperatingMoment({ familyCode: 'RETAIL_CONSUMER', prefs, role: 'FINANCE', events });
    const staff = projectOperatingMoment({ familyCode: 'RETAIL_CONSUMER', prefs, role: 'TEAM_MEMBER', events });
    const sale = finance.boards.life.find((section) => section.code === 'L3')?.rows[0];
    assert.equal(sale?.channelName, 'Store');
    assert.equal(sale?.amount, '1800.0000');
    assert.equal(finance.finance?.sales, '1800.0000');
    assert.equal(finance.finance?.outstanding, '1800.0000');
    assert.equal(finance.attention.some((item) => item.id === 'issue-1' && item.kind === 'APPROVAL'), true);
    assert.equal(staff.finance, null);
    assert.equal(staff.activity.find((row) => row.id === 'sale-1')?.amount, null);
    assert.equal(staff.boards.life.find((section) => section.code === 'L3')?.rows[0]?.amount, null);
  });

  it('counts a recorded service in sales and a collected service in received', () => {
    const prefs = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'CONSUMER_SERVICE',
        identityName: 'Care',
        partyName: 'Care',
        monthlyBudget: '8000',
        monthlyTarget: '20000',
        targetMetric: 'REVENUE',
        channels: ['Walk-in'],
        startDate: '2026-10-01',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    const events: OperatingEventRow[] = [
      {
        operatingEventId: 'svc-1',
        eventType: 'SERVICE',
        title: 'Home visit',
        note: null,
        amount: '900.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Walk-in',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'COLLECTED',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-02T00:00:00.000Z',
      },
      {
        operatingEventId: 'svc-2',
        eventType: 'SERVICE',
        title: 'Follow up',
        note: null,
        amount: '400.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Walk-in',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-03T00:00:00.000Z',
      },
    ];
    const finance = projectOperatingMoment({ familyCode: 'RETAIL_CONSUMER', prefs, role: 'FINANCE', events });
    assert.equal(finance.finance?.sales, '1300.0000');
    assert.equal(finance.finance?.received, '900.0000');
    assert.equal(finance.target?.progress, '1300.0000');
    assert.equal(finance.boards.life.find((section) => section.code === 'L4')?.rows.length, 2);
    assert.equal(finance.boards.moments.some((section) => section.code === 'purchases'), false);
  });

  it('allows a retail store without a budget or target', () => {
    const prefs = normalizeOperatingPreferences(
      'RETAIL_CONSUMER',
      {
        subtype: 'RETAIL_STORE',
        identityName: 'Store',
        partyName: 'Store',
        monthlyBudget: '0',
        monthlyTarget: '',
        targetMetric: 'REVENUE',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    assert.equal(prefs.monthlyBudget, '');
    assert.equal(prefs.monthlyTarget, '');
    const view = projectOperatingMoment({
      familyCode: 'RETAIL_CONSUMER',
      prefs,
      role: 'FINANCE',
      events: [],
    });
    assert.equal(view.target?.configured, false);
    assert.equal(view.budget?.configured, false);
  });

  it('keeps trade reach and shows a trading shipment on the dispatches board', () => {
    const prefs = normalizeOperatingPreferences(
      'TRADING_DISTRIBUTION',
      {
        subtype: 'TRADING_WHOLESALE',
        identityName: 'Trade',
        partyName: 'Trade',
        monthlyBudget: '5000',
        monthlyTarget: '12000',
        targetMetric: 'REVENUE',
        channels: ['Wholesale'],
        tradeReach: ['Domestic', 'Moon', 'Domestic'],
        counterparties: [{ name: 'Harbor', kind: 'BUYER' }],
      },
      { userId: 'u', displayName: 'Asha' }
    );
    assert.deepEqual(prefs.tradeReach, ['Domestic']);
    const events: OperatingEventRow[] = [
      {
        operatingEventId: 'ship-1',
        eventType: 'SHIPMENT',
        title: 'Dispatch',
        note: null,
        amount: null,
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Wholesale',
        paymentRelevance: 'NONE',
        paymentStatus: 'NONE',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-04T00:00:00.000Z',
      },
    ];
    const view = projectOperatingMoment({ familyCode: 'TRADING_DISTRIBUTION', prefs, role: 'OWNER_ADMIN', events });
    assert.equal(view.quickAdds.some((item) => item.code === 'SHIPMENT'), true);
    const dispatches = view.boards.moments.find((section) => section.code === 'M4');
    assert.equal(dispatches?.title, 'Open Commitments');
    assert.equal(dispatches?.rows.length, 1);
    assert.equal(dispatches?.rows[0]?.title, 'In transit · Dispatch');
  });

  it('counts only campaign results toward the marketing target and offers collection on an own-business campaign', () => {
    const prefs = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'CLIENT_CAMPAIGN',
        identityName: 'Launch',
        partyName: 'Client',
        channels: ['Email'],
        monthlyBudget: '8000',
        monthlyTarget: '1000',
        targetMetric: 'REVENUE',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    const events: OperatingEventRow[] = [
      {
        operatingEventId: 'result-1',
        eventType: 'RESULT',
        title: 'Accepted result',
        note: null,
        amount: '100.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Email',
        paymentRelevance: 'NONE',
        paymentStatus: 'NONE',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-04T00:00:00.000Z',
      },
      {
        operatingEventId: 'collect-1',
        eventType: 'COLLECTION',
        title: 'Cash received',
        note: null,
        amount: '50.0000',
        currencyCode: 'INR',
        counterpartyName: null,
        channelName: 'Email',
        paymentRelevance: 'COLLECT',
        paymentStatus: 'COLLECTED',
        approvalStatus: 'NONE',
        submittedByUserId: 'u',
        decidedByUserId: null,
        occurredAt: '2026-10-05T00:00:00.000Z',
      },
    ];
    const view = projectOperatingMoment({ familyCode: 'MARKETING_CAMPAIGNS', prefs, role: 'OWNER_ADMIN', events });
    assert.equal(view.target?.progress, '100.0000');
    assert.notEqual(view.finance.sales, '100.0000');
    const own = normalizeOperatingPreferences(
      'MARKETING_CAMPAIGNS',
      {
        subtype: 'OWN_BUSINESS_CAMPAIGN',
        identityName: 'Brand',
        partyName: 'Brand',
        channels: ['Instagram'],
        monthlyBudget: '8000',
        monthlyTarget: '1000',
        targetMetric: 'REVENUE',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    const ownView = projectOperatingMoment({ familyCode: 'MARKETING_CAMPAIGNS', prefs: own, role: 'OWNER_ADMIN', events: [] });
    assert.equal(ownView.quickAdds.some((item) => item.code === 'COLLECTION'), true);
  });

  it('counts manufacturing progress from the selected metric and keeps collections out of revenue', () => {
    const row = (id: string, eventType: string, amount: string): OperatingEventRow => ({
      operatingEventId: id,
      eventType,
      title: eventType,
      note: null,
      amount,
      currencyCode: 'INR',
      counterpartyName: null,
      channelName: null,
      paymentRelevance: 'NONE',
      paymentStatus: 'NONE',
      approvalStatus: 'NONE',
      submittedByUserId: 'u',
      decidedByUserId: null,
      occurredAt: '2026-10-04T00:00:00.000Z',
    });
    const base = {
      subtype: 'STANDARD_PRODUCTION',
      identityName: 'Factory',
      partyName: 'Plant',
      monthlyBudget: '8000',
      monthlyTarget: '1000',
      targetMetric: 'REVENUE',
    };
    const events = [row('sale', 'SALE', '100.0000'), row('collect', 'COLLECTION', '50.0000'), row('made', 'PRODUCTION', '40.0000')];
    const revenue = normalizeOperatingPreferences('MANUFACTURING_PRODUCTION', base, { userId: 'u', displayName: 'Asha' });
    const revenueView = projectOperatingMoment({ familyCode: 'MANUFACTURING_PRODUCTION', prefs: revenue, role: 'OWNER_ADMIN', events });
    assert.equal(revenueView.target?.progress, '100.0000');
    assert.equal(revenueView.finance.sales, '100.0000');
    const output = normalizeOperatingPreferences(
      'MANUFACTURING_PRODUCTION',
      { ...base, targetMetric: 'PRODUCTION_OUTPUT' },
      { userId: 'u', displayName: 'Asha' }
    );
    const outputView = projectOperatingMoment({ familyCode: 'MANUFACTURING_PRODUCTION', prefs: output, role: 'OWNER_ADMIN', events });
    assert.equal(outputView.target?.progress, '40.0000');
    assert.equal(outputView.finance.sales, '100.0000');
    assert.equal(revenueView.quickAdds.some((item) => item.code === 'CUSTOMER_ORDER'), true);
    assert.equal(revenueView.quickAdds.some((item) => item.code === 'STOCK'), true);
    const orders = normalizeOperatingPreferences(
      'MANUFACTURING_PRODUCTION',
      {
        subtype: 'MADE_TO_ORDER',
        identityName: 'Workshop',
        partyName: 'Workshop',
        monthlyBudget: '8000',
        monthlyTarget: '10',
        targetMetric: 'ORDERS',
      },
      { userId: 'u', displayName: 'Asha' }
    );
    const orderView = projectOperatingMoment({
      familyCode: 'MANUFACTURING_PRODUCTION',
      prefs: orders,
      role: 'OWNER_ADMIN',
      events: [row('order', 'CUSTOMER_ORDER', '250.0000')],
    });
    assert.equal(orderView.target?.progress, '1.0000');
    assert.equal(orderView.quickAdds.some((item) => item.code === 'STOCK'), true);
  });

  it('accepts a chosen event date, including a past date', () => {
    const backdated = recordOperatingEventSchema.parse({
      eventType: 'SALE',
      title: 'Record Sale',
      occurredAt: '2020-01-15T00:00:00+05:30',
    });
    assert.equal(backdated.occurredAt, '2020-01-15T00:00:00+05:30');
    const utc = recordOperatingEventSchema.parse({
      eventType: 'SALE',
      title: 'Record Sale',
      occurredAt: '2024-06-01T00:00:00Z',
    });
    assert.equal(utc.occurredAt, '2024-06-01T00:00:00Z');
    const omitted = recordOperatingEventSchema.parse({ eventType: 'SALE', title: 'Record Sale' });
    assert.equal(omitted.occurredAt, undefined);
    const cleared = recordOperatingEventSchema.parse({ eventType: 'SALE', title: 'Record Sale', occurredAt: null });
    assert.equal(cleared.occurredAt, null);
    assert.throws(() =>
      recordOperatingEventSchema.parse({ eventType: 'SALE', title: 'Record Sale', occurredAt: '15 Jan 2020' })
    );
  });

  it('rejects a client campaign without a channel', () => {
    assert.throws(
      () =>
        normalizeOperatingPreferences(
          'RETAIL_CONSUMER',
          {
            subtype: 'CLIENT_CAMPAIGN',
            identityName: 'Nope',
            partyName: 'Nope',
            monthlyBudget: '10',
            monthlyTarget: '10',
            targetMetric: 'REVENUE',
          },
          { userId: 'u', displayName: 'U' }
        ),
      /Unknown subtype/
    );
  });
});

function posted(id: string, eventType: string, fields: Partial<OperatingEventRow> = {}): OperatingEventRow {
  return {
    operatingEventId: id,
    eventType,
    title: eventType,
    note: null,
    amount: null,
    currencyCode: 'INR',
    counterpartyName: null,
    channelName: null,
    paymentRelevance: 'NONE',
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    submittedByUserId: 'u',
    decidedByUserId: null,
    occurredAt: '2026-10-04T00:00:00.000Z',
    ...fields,
  };
}

function momentView(
  family: OperatingFamilyCode,
  subtype: string,
  events: OperatingEventRow[],
  budget = '1000',
  target = '1000'
) {
  const prefs = normalizeOperatingPreferences(
    family,
    {
      subtype,
      identityName: subtype,
      partyName: subtype,
      monthlyBudget: budget,
      monthlyTarget: target,
      targetMetric: 'REVENUE',
      channels: ['Store'],
    },
    { userId: 'u', displayName: 'Asha' }
  );
  return projectOperatingMoment({ familyCode: family, prefs, role: 'OWNER_ADMIN', events });
}

describe('operating allocation continuity', () => {
  it('rejects a payment, collection, or delivery that overruns the parent', () => {
    const purchase = posted('buy', 'PURCHASE', { amount: '80000', paymentStatus: 'PENDING', paymentRelevance: 'PAYABLE' });
    assert.equal(
      validateOperatingAllocation({
        eventType: 'PAYMENT',
        parent: null,
        siblings: [],
        amount: '30000',
        quantity: null,
      }).ok,
      false
    );
    assert.equal(
      validateOperatingAllocation({
        eventType: 'PAYMENT',
        parent: purchase,
        siblings: [],
        amount: '90000',
        quantity: null,
      }).ok,
      false
    );
    const pending = posted('pay-pending', 'PAYMENT', {
      amount: '60000',
      parentEventId: 'buy',
      paymentStatus: 'PENDING',
      approvalStatus: 'PENDING',
    });
    assert.equal(
      validateOperatingAllocation({
        eventType: 'PAYMENT',
        parent: purchase,
        siblings: [pending],
        amount: '30000',
        quantity: null,
      }).ok,
      false
    );
    const rejected = posted('pay-rejected', 'PAYMENT', {
      amount: '60000',
      parentEventId: 'buy',
      approvalStatus: 'REJECTED',
    });
    assert.equal(
      validateOperatingAllocation({
        eventType: 'PAYMENT',
        parent: purchase,
        siblings: [rejected],
        amount: '30000',
        quantity: null,
      }).ok,
      true
    );
    const order = posted('order', 'ORDER', { quantity: '100' });
    assert.equal(
      validateOperatingAllocation({
        eventType: 'DELIVERY',
        parent: null,
        siblings: [],
        amount: null,
        quantity: '70',
      }).ok,
      false
    );
    const delivered = posted('drop', 'DELIVERY', { quantity: '70', parentEventId: 'order' });
    assert.equal(
      validateOperatingAllocation({
        eventType: 'DELIVERY',
        parent: order,
        siblings: [delivered],
        amount: null,
        quantity: '40',
      }).ok,
      false
    );
    assert.equal(
      validateOperatingAllocation({
        eventType: 'SHIPMENT',
        parent: order,
        siblings: [delivered],
        amount: null,
        quantity: '100',
      }).ok,
      true
    );
  });

  it('keeps a collected sale at zero receivable without a collection child', () => {
    const view = momentView('RETAIL_CONSUMER', 'RETAIL_STORE', [
      posted('sale', 'SALE', { amount: '45000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(view.finance?.outstanding, '0.0000');
    assert.equal(view.continuity.receivable, '0.0000');
    assert.equal(view.finance?.received, '45000.0000');
  });

  it('retail: purchase 80000, pay 30000, credit sale 45000, collect 35000', () => {
    const view = momentView('RETAIL_CONSUMER', 'RETAIL_STORE', [
      posted('buy', 'PURCHASE', { amount: '80000', paymentRelevance: 'PAYABLE', paymentStatus: 'PENDING' }),
      posted('pay', 'PAYMENT', { amount: '30000', parentEventId: 'buy', paymentRelevance: 'PAYABLE', paymentStatus: 'PAID' }),
      posted('sale', 'SALE', { amount: '45000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
      posted('collect', 'COLLECTION', {
        amount: '35000',
        parentEventId: 'sale',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'COLLECTED',
      }),
    ]);
    assert.equal(view.finance?.payable, '50000.0000');
    assert.equal(view.finance?.outstanding, '10000.0000');
    assert.equal(view.continuity.payable, '50000.0000');
    assert.equal(view.continuity.receivable, '10000.0000');
  });

  it('restaurant: credit sales 25000, collect 20000, ingredients 8000, pay 5000', () => {
    const view = momentView('RETAIL_CONSUMER', 'RESTAURANT_FOOD', [
      posted('sale', 'SALE', { amount: '25000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
      posted('collect', 'COLLECTION', {
        amount: '20000',
        parentEventId: 'sale',
        paymentStatus: 'COLLECTED',
      }),
      posted('buy', 'PURCHASE', { amount: '8000', paymentRelevance: 'PAYABLE', paymentStatus: 'PENDING' }),
      posted('pay', 'PAYMENT', { amount: '5000', parentEventId: 'buy', paymentStatus: 'PAID' }),
    ]);
    assert.equal(view.continuity.receivable, '5000.0000');
    assert.equal(view.continuity.payable, '3000.0000');
  });

  it('services: booked 15000, completed 10000, collect 6000', () => {
    const view = momentView('RETAIL_CONSUMER', 'CONSUMER_SERVICE', [
      posted('book', 'APPOINTMENT', { amount: '15000' }),
      posted('job', 'SERVICE', {
        amount: '10000',
        parentEventId: 'book',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
      }),
      posted('collect', 'COLLECTION', {
        amount: '6000',
        parentEventId: 'job',
        paymentStatus: 'COLLECTED',
      }),
    ]);
    assert.equal(view.continuity.openBookings, '5000.0000');
    assert.equal(view.continuity.receivable, '4000.0000');
  });

  it('trading: purchase 80000, pay 30000, sale 110000, collect 50000', () => {
    const view = momentView('TRADING_DISTRIBUTION', 'TRADING_WHOLESALE', [
      posted('buy', 'PURCHASE', { amount: '80000', paymentRelevance: 'PAYABLE', paymentStatus: 'PENDING' }),
      posted('pay', 'PAYMENT', { amount: '30000', parentEventId: 'buy', paymentStatus: 'PAID' }),
      posted('sale', 'SALE', { amount: '110000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
      posted('collect', 'COLLECTION', { amount: '50000', parentEventId: 'sale', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(view.continuity.payable, '50000.0000');
    assert.equal(view.continuity.receivable, '60000.0000');
    assert.equal(view.quickAdds.some((item) => item.code === 'DELIVERY'), false);
  });

  it('distribution: order 100, dispatch 100, deliver 70, invoice 100000, collect 40000', () => {
    const view = momentView('TRADING_DISTRIBUTION', 'DISTRIBUTION_DEALERSHIP', [
      posted('order', 'ORDER', { quantity: '100' }),
      posted('ship', 'SHIPMENT', { quantity: '100', parentEventId: 'order' }),
      posted('drop', 'DELIVERY', { quantity: '70', parentEventId: 'order' }),
      posted('invoice', 'SALE', { amount: '100000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
      posted('collect', 'COLLECTION', { amount: '40000', parentEventId: 'invoice', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(view.continuity.pendingDelivery, '30.0000');
    assert.equal(view.continuity.dispatched, '100.0000');
    assert.equal(view.continuity.receivable, '60000.0000');
    assert.equal(view.quickAdds.some((item) => item.code === 'DELIVERY'), true);
  });

  it('client campaign: budget 200000, approved 80000, spend 60000, invoice 100000, collect 40000', () => {
    const view = momentView(
      'MARKETING_CAMPAIGNS',
      'CLIENT_CAMPAIGN',
      [
        posted('ok', 'APPROVAL', { amount: '80000', approvalStatus: 'APPROVED' }),
        posted('spend', 'SPEND', { amount: '60000', paymentRelevance: 'PAYABLE', paymentStatus: 'PENDING' }),
        posted('invoice', 'SALE', { amount: '100000', paymentRelevance: 'RECEIVABLE', paymentStatus: 'PENDING' }),
        posted('collect', 'COLLECTION', { amount: '40000', parentEventId: 'invoice', paymentStatus: 'COLLECTED' }),
      ],
      '200000',
      '1000'
    );
    assert.equal(view.budget?.remaining, '140000.0000');
    assert.equal(view.continuity.approvedUnspent, '20000.0000');
    assert.equal(view.continuity.receivable, '60000.0000');
    assert.equal(view.quickAdds.some((item) => item.code === 'SALE'), true);
  });

  it('own campaign: budget 100000, spend 35000, 150 leads, 12 conversions', () => {
    const view = momentView(
      'MARKETING_CAMPAIGNS',
      'OWN_BUSINESS_CAMPAIGN',
      [
        posted('spend', 'SPEND', { amount: '35000' }),
        posted('leads', 'RESULT', { metric: 'LEADS', quantity: '150' }),
        posted('wins', 'RESULT', { metric: 'CONVERSIONS', quantity: '12' }),
      ],
      '100000',
      '1000'
    );
    assert.equal(view.budget?.remaining, '65000.0000');
    assert.equal(view.continuity.costPerLead, '233.33');
    assert.equal(view.continuity.conversionRate, '8.00');
  });

  it('standard production: plan 500, produce 350, reject 20, sell 200', () => {
    const view = momentView('MANUFACTURING_PRODUCTION', 'STANDARD_PRODUCTION', [
      posted('plan', 'TARGET', { quantity: '500' }),
      posted('made', 'PRODUCTION', { quantity: '350', rejectedQuantity: '20' }),
      posted('sold', 'SALE', { quantity: '200' }),
    ]);
    assert.equal(view.continuity.accepted, '330.0000');
    assert.equal(view.continuity.unsoldAccepted, '130.0000');
  });

  it('made to order: order 100 / 200000, produce 80, deliver 60, invoice 120000, collect 50000', () => {
    const view = momentView('MANUFACTURING_PRODUCTION', 'MADE_TO_ORDER', [
      posted('order', 'CUSTOMER_ORDER', {
        quantity: '100',
        amount: '200000',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
      }),
      posted('made', 'PRODUCTION', { quantity: '80', parentEventId: 'order' }),
      posted('drop', 'DELIVERY', { quantity: '60', parentEventId: 'order' }),
      posted('invoice', 'SALE', {
        amount: '120000',
        parentEventId: 'order',
        paymentRelevance: 'RECEIVABLE',
        paymentStatus: 'PENDING',
      }),
      posted('collect', 'COLLECTION', { amount: '50000', parentEventId: 'invoice', paymentStatus: 'COLLECTED' }),
    ]);
    assert.equal(view.continuity.pendingProduction, '20.0000');
    assert.equal(view.continuity.pendingDelivery, '40.0000');
    assert.equal(view.continuity.receivable, '70000.0000');
  });
});
