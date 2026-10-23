import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { composeRetailStory } from '../src/modules/story/retail-story';
import { renderBookletHtml } from '../src/modules/story/render';
import type { RetailEvent } from '../src/modules/business/operating-retail-contract';

function row(partial: Partial<RetailEvent> & Pick<RetailEvent, 'eventType' | 'title'>): RetailEvent {
  return {
    operatingEventId: partial.operatingEventId ?? partial.title,
    amount: null,
    paymentStatus: 'NONE',
    approvalStatus: 'NONE',
    occurredAt: '2026-10-15T00:00:00.000Z',
    ...partial,
  };
}

describe('retail food consumer story', () => {
  it('ST01 ST03 ST25 eight pages and missing facts stay unrecorded', () => {
    const story = composeRetailStory({
      momentId: 'm',
      title: 'Corner shop',
      subtype: 'RETAIL_STORE',
      currency: 'INR',
      startAt: '2026-10-01',
      endAt: '2026-10-31',
      channels: [],
      participants: [],
      events: [],
    });
    assert.deepEqual(story.chapters, [
      'rf-moment',
      'rf-motion',
      'rf-revenue',
      'rf-money',
      'rf-operations',
      'rf-people',
      'rf-attention',
      'rf-sofar',
    ]);
    assert.equal(story.metrics.revenue, 'not recorded');
    assert.equal(story.highlights?.find((card) => card.id === 'rf-people')?.detail, 'People not recorded.');
    const html = renderBookletHtml(story);
    assert.equal(html.toLowerCase().includes('profit'), false);
    assert.equal(html.toLowerCase().includes('units sold'), false);
  });

  it('ST06 ST07 ST08 retail net 15000 and receivable 6000', () => {
    const story = composeRetailStory({
      momentId: 'shop',
      title: 'Shop',
      subtype: 'RETAIL_STORE',
      currency: 'INR',
      startAt: null,
      endAt: null,
      channels: ['Store'],
      participants: [],
      events: [
        row({ operatingEventId: 'a', eventType: 'SALE', title: 'Sale A', amount: '10000', paymentStatus: 'PENDING', counterpartyName: 'Meera', contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' } }),
        row({ operatingEventId: 'b', eventType: 'SALE', title: 'Sale B', amount: '6000', paymentStatus: 'PENDING', counterpartyName: 'Meera', contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' } }),
        row({ operatingEventId: 'c', eventType: 'RETURN', title: 'Credit', amount: '1000', parentEventId: 'a', contract: { returnDirection: 'CUSTOMER', goodsOutcome: 'NOT_RETURNED' } }),
        row({ operatingEventId: 'd', eventType: 'COLLECTION', title: 'Receipt', amount: '9000', parentEventId: 'a', paymentStatus: 'COLLECTED' }),
      ],
    });
    assert.equal(story.metrics.revenue, '15000');
    assert.equal(story.metrics.receivable, '6000');
    assert.equal(story.metrics.cash, '9000');
    assert.equal(story.metrics.sales, '2');
  });

  it('ST04 food order is not revenue until billed', () => {
    const story = composeRetailStory({
      momentId: 'food',
      title: 'Kitchen',
      subtype: 'RESTAURANT_FOOD',
      currency: 'INR',
      startAt: null,
      endAt: null,
      channels: ['Dine-in'],
      participants: [],
      events: [
        row({ eventType: 'ORDER', title: 'Open', contract: { orderStatus: 'PLACED', estimatedAmount: '900' } }),
        row({ operatingEventId: 'done', eventType: 'ORDER', title: 'Served', contract: { orderStatus: 'COMPLETED', estimatedAmount: '800' } }),
        row({ eventType: 'ORDER', title: 'Served again', contract: { orderStatus: 'COMPLETED' } }),
        row({ operatingEventId: 'bill', eventType: 'SALE', title: 'Bill', amount: '2400', parentEventId: 'done', paymentStatus: 'PENDING', counterpartyName: 'Guest', contract: { entryMode: 'QUICK', paymentState: 'PART_PAID' } }),
        row({ eventType: 'COLLECTION', title: 'Part', amount: '1000', parentEventId: 'bill', paymentStatus: 'COLLECTED' }),
      ],
    });
    assert.equal(story.metrics.revenue, '2400');
    assert.equal(story.metrics.receivable, '1400');
    assert.equal(story.metrics.sales, '1');
  });

  it('ST10 ST14 service charge pending is not revenue', () => {
    const story = composeRetailStory({
      momentId: 'svc',
      title: 'Studio',
      subtype: 'CONSUMER_SERVICE',
      currency: 'INR',
      startAt: null,
      endAt: null,
      channels: ['Walk-in'],
      participants: [],
      events: [
        row({ operatingEventId: 'b1', eventType: 'APPOINTMENT', title: 'Done unpaid', contract: { appointmentStatus: 'COMPLETED' } }),
        row({ operatingEventId: 'b2', eventType: 'APPOINTMENT', title: 'Done billed', contract: { appointmentStatus: 'COMPLETED' } }),
        row({ eventType: 'APPOINTMENT', title: 'Cancelled', contract: { appointmentStatus: 'CANCELLED' } }),
        row({ eventType: 'APPOINTMENT', title: 'Upcoming', contract: { appointmentStatus: 'BOOKED' } }),
        row({ operatingEventId: 'charge', eventType: 'SERVICE', title: 'Charge', amount: '3000', parentEventId: 'b2', paymentStatus: 'PENDING', counterpartyName: 'Guest', contract: { entryMode: 'QUICK', paymentState: 'PAY_LATER' } }),
        row({ eventType: 'COLLECTION', title: 'Part', amount: '1200', parentEventId: 'charge', paymentStatus: 'COLLECTED' }),
      ],
    });
    assert.equal(story.metrics.revenue, '3000');
    assert.equal(story.metrics.receivable, '1800');
    assert.equal(story.metrics.chargePending, '1');
    assert.equal(story.highlights?.find((card) => card.id === 'rf-attention')?.detail.includes('completed'), true);
    assert.equal(JSON.stringify(story).toLowerCase().includes('completed means charged'), false);
  });
});
