import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { composeMarketingStory } from '../src/modules/story/marketing-story';
import { renderBookletHtml } from '../src/modules/story/render';
import type { MarketingEvent } from '../src/modules/business/operating-marketing';

function row(partial: Partial<MarketingEvent> & Pick<MarketingEvent, 'eventType' | 'title'>): MarketingEvent {
  return {
    operatingEventId: partial.operatingEventId ?? partial.title,
    amount: null,
    paymentStatus: 'PAID',
    approvalStatus: 'APPROVED',
    occurredAt: '2026-10-15T00:00:00.000Z',
    ...partial,
  };
}

describe('marketing campaign story', () => {
  it('MS-01 keeps own attributed sales out of booked revenue and uses eight chapters', () => {
    const story = composeMarketingStory({
      momentId: 'm1',
      title: 'October brand',
      subtype: 'OWN_BUSINESS_CAMPAIGN',
      currency: 'INR',
      startAt: '2026-10-01',
      endAt: '2026-10-31',
      targetMetric: 'LEADS',
      channels: ['Instagram', 'Email'],
      participants: ['Asha'],
      events: [
        row({
          eventType: 'RESULT',
          title: 'Leads',
          quantity: '4',
          unit: 'count',
          channelName: 'Instagram',
          marketing: {
            reportingMode: 'INCREMENTAL',
            metricCode: 'LEADS',
            budgetCategory: null,
            paymentState: null,
            activityStatus: null,
            attributedSales: '9000',
            revisionKind: null,
          },
        }),
      ],
    });
    assert.equal(story.identity.familyProfile, 'MARKETING_CAMPAIGN');
    assert.equal(story.chapters.length, 8);
    assert.equal(story.metrics.results, '4');
    assert.equal(story.money.contributed, 0);
    assert.equal(story.metrics.invoices, 'N/A');
    assert.match(story.narrative.opening, /not booked revenue/);
  });

  it('MS-02 shows N/A when no compatible result exists', () => {
    const story = composeMarketingStory({
      momentId: 'm2',
      title: 'Quiet',
      subtype: 'CLIENT_CAMPAIGN',
      currency: 'INR',
      startAt: '2026-10-01',
      endAt: '2026-10-31',
      targetMetric: 'LEADS',
      channels: [],
      participants: [],
      events: [],
    });
    assert.equal(story.metrics.results, 'N/A');
    assert.equal(story.metrics.channels, 'N/A');
    assert.equal(story.highlights?.[2]?.detail, 'No compatible results yet.');
  });

  it('MS-03 keeps client invoices separate from results', () => {
    const story = composeMarketingStory({
      momentId: 'm3',
      title: 'Northwind',
      subtype: 'CLIENT_CAMPAIGN',
      currency: 'INR',
      startAt: '2026-10-01',
      endAt: '2026-10-31',
      targetMetric: 'LEADS',
      channels: ['Email'],
      participants: ['Riya'],
      events: [
        row({ eventType: 'INVOICE', title: 'October invoice', amount: '60000', counterpartyName: 'Northwind' }),
        row({ eventType: 'COLLECTION', title: 'Part collected', amount: '10000' }),
        row({ eventType: 'SPEND', title: 'Media', amount: '5000', marketing: { reportingMode: null, metricCode: null, budgetCategory: 'MEDIA', paymentState: 'PAY_LATER', activityStatus: null, attributedSales: null, revisionKind: null } }),
      ],
    });
    assert.equal(story.metrics.invoices, 60000);
    assert.equal(story.money.unsettled, 50000);
    assert.equal(story.money.spent, 5000);
    assert.equal(story.metrics.results, 'N/A');
    const html = renderBookletHtml(story);
    assert.match(html, /Money/);
    assert.match(html, /Invoices stay separate/);
  });

  it('MS-04 October own-campaign fixture keeps spend, payment, and leads apart', () => {
    const lead = (title: string, quantity: string, channel: string | null) =>
      row({
        eventType: 'RESULT',
        title,
        quantity,
        unit: 'leads',
        channelName: channel,
        marketing: {
          reportingMode: 'INCREMENTAL',
          metricCode: 'LEADS',
          budgetCategory: null,
          paymentState: null,
          activityStatus: null,
          attributedSales: null,
          revisionKind: null,
        },
      });
    const story = composeMarketingStory({
      momentId: 'oct',
      title: 'October launch',
      subtype: 'OWN_BUSINESS_CAMPAIGN',
      currency: 'INR',
      startAt: '2026-10-01',
      endAt: '2026-10-31',
      targetMetric: 'LEADS',
      budget: 60000,
      target: 200,
      channels: ['Paid Social', 'Influencer'],
      participants: [],
      events: [
        row({ eventType: 'SPEND', title: 'Meta ads', amount: '24000', channelName: 'Paid Social' }),
        row({ eventType: 'SPEND', title: 'Creator fee', amount: '12000', channelName: 'Influencer' }),
        row({ eventType: 'PAYMENT', title: 'Paid toward spend', amount: '20000' }),
        lead('Meta leads', '50', 'Paid Social'),
        lead('Creator leads', '20', 'Influencer'),
        lead('Unlinked leads', '10', null),
      ],
    });
    assert.equal(story.metrics.results, '80');
    assert.equal(story.metrics.spent, 36000);
    assert.equal(story.metrics.paid, 20000);
    assert.equal(story.metrics.attainment, '40%');
    assert.equal(story.metrics.budgetUse, '60%');
    assert.equal(story.metrics.channels, '70 / 80');
    assert.equal(story.metrics.invoices, 'N/A');
    assert.equal(story.money.contributed, 0);
  });
});
