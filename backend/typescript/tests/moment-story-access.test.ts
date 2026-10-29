/**
 * Moment Story access — regression for the self-recursive story guard.
 *
 * Every story route authorizes through `assertStoryAccess`. Before the fix its
 * fallback branch called itself instead of the MOMENT governance check, so any
 * Group or Personal moment request recursed until the API process ran out of
 * heap and never answered. These tests drive the real route so that a runaway
 * guard fails as a timeout / process death rather than passing silently.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool, getPool } from '../src/platform/database/pool';
import { firebaseUserId } from '../src/platform/auth/uuid';
import { config } from '../src/platform/config';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

const STORY_STATUSES = ['READY', 'GENERATING', 'NOT_STARTED', 'FAILED'];

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string, displayName: string | null): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, displayName]
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `story-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createGroupMoment(
  organizerUid: string,
  memberUid: string
): Promise<string> {
  const mint = await request(app)
    .post('/v1/group/invites')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `story-mint-${randomUUID()}`)
    .send({ title: 'Story Access Trip', momentTypeCode: 'TRIP' });
  assert.equal(mint.status, 201, JSON.stringify(mint.body));

  const created = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `story-moment-${randomUUID()}`)
    .send({
      domainCode: 'GROUP',
      momentTypeCode: 'TRIP',
      title: 'Story Access Trip',
      inviteCode: mint.body.data.inviteCode,
    });
  assert.equal(created.status, 201, JSON.stringify(created.body));
  const momentId = created.body.data.momentId as string;

  const redeem = await request(app)
    .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
    .set('X-Dev-Firebase-Uid', memberUid)
    .set('Idempotency-Key', `story-redeem-${randomUUID()}`)
    .send({});
  assert.equal(redeem.status, 200, JSON.stringify(redeem.body));

  const completed = await request(app)
    .post(`/v1/moments/${momentId}/complete`)
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `story-complete-${randomUUID()}`)
    .send({ expectedVersion: created.body.data.version });
  assert.equal(completed.status, 200, JSON.stringify(completed.body));

  return momentId;
}

async function createOperatingBusinessMoment(uid: string, companyId: string): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `story-biz-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'RETAIL_STORE',
      title: 'Story Access Store',
      companyId,
      businessSetup: {
        familyCode: 'RETAIL_CONSUMER',
        preferences: {
          subtype: 'RETAIL_STORE',
          identityName: 'Story Access Store',
          partyName: 'Story Access Store',
          channels: ['Store'],
        },
      },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function storyStatus(uid: string, momentId: string): Promise<request.Response> {
  return request(app)
    .get(`/v1/moments/${momentId}/story-status`)
    .set('X-Dev-Firebase-Uid', uid)
    // A recursed guard never answers, so bound the wait and fail loudly.
    .timeout({ deadline: 10_000, response: 8_000 });
}

async function generateStory(uid: string, momentId: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/moments/${momentId}/stories`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `story-gen-${randomUUID()}`)
    .timeout({ deadline: 20_000, response: 18_000 });
  assert.equal(res.status, 200, JSON.stringify(res.body));
}

async function postRetailSale(uid: string, momentId: string, amount: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/business/moments/${momentId}/operating-events`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `story-sale-${randomUUID()}`)
    .send({
      eventType: 'SALE',
      title: `Counter sale ${amount}`,
      amount,
      channelName: 'Store',
      paymentStatus: 'COLLECTED',
      contract: { entryMode: 'QUICK', paymentState: 'RECEIVED' },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

describe('Moment Story access guard', () => {
  const organizerUid = `story-org-${randomUUID().slice(0, 8)}`;
  const memberUid = `story-mem-${randomUUID().slice(0, 8)}`;
  const outsiderUid = `story-out-${randomUUID().slice(0, 8)}`;
  const ownerUid = `story-own-${randomUUID().slice(0, 8)}`;

  let groupMomentId = '';
  let businessMomentId = '';

  before(async () => {
    await getPool().query('SELECT 1');
    await ensureUser(userIdFor(organizerUid), `${organizerUid}@story.local`, 'Story Organizer');
    await ensureUser(userIdFor(memberUid), `${memberUid}@story.local`, 'Story Member');
    await ensureUser(userIdFor(outsiderUid), `${outsiderUid}@story.local`, 'Story Outsider');
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@story.local`, 'Story Owner');

    groupMomentId = await createGroupMoment(organizerUid, memberUid);

    const companyId = await createCompany(ownerUid, `Story ${ownerUid}`);
    businessMomentId = await createOperatingBusinessMoment(ownerUid, companyId);
  });

  after(async () => {
    await closePool();
  });

  it('answers story-status for a completed group moment', async () => {
    const res = await storyStatus(organizerUid, groupMomentId);
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.ok(
      STORY_STATUSES.includes(res.body.data.status),
      `unexpected story status ${res.body.data.status}`
    );
  });

  it('answers story-status for another member of the same moment', async () => {
    const res = await storyStatus(memberUid, groupMomentId);
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.ok(STORY_STATUSES.includes(res.body.data.status));
  });

  it('serves the story payload when the status is READY', async () => {
    const status = await storyStatus(organizerUid, groupMomentId);
    assert.equal(status.status, 200, JSON.stringify(status.body));
    if (status.body.data.status !== 'READY') {
      // Generation is environment-dependent; the guard is still covered above.
      return;
    }
    const res = await request(app)
      .get(`/v1/moments/${groupMomentId}/story`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .timeout({ deadline: 10_000, response: 8_000 });
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.equal(res.body.data.storyId, status.body.data.storyId);
    assert.ok(Array.isArray(res.body.data.chapters));
  });

  it('denies story-status to a non-member', async () => {
    const res = await storyStatus(outsiderUid, groupMomentId);
    assert.equal(res.status, 403, JSON.stringify(res.body));
    assert.equal(res.body.code, 'GOVERNANCE_DENIED');
  });

  it('answers story-status for an operating business moment', async () => {
    const res = await storyStatus(ownerUid, businessMomentId);
    assert.equal(res.status, 200, JSON.stringify(res.body));
    assert.ok(STORY_STATUSES.includes(res.body.data.status));
  });

  it('denies story-status to a non-member of the business moment', async () => {
    const res = await storyStatus(outsiderUid, businessMomentId);
    assert.equal(res.status, 403, JSON.stringify(res.body));
    assert.equal(res.body.code, 'GOVERNANCE_DENIED');
  });

  it('keeps the recorded business story chapters and figures on read', async () => {
    await postRetailSale(ownerUid, businessMomentId, '12500');
    await generateStory(ownerUid, businessMomentId);

    const status = await storyStatus(ownerUid, businessMomentId);
    assert.equal(status.status, 200, JSON.stringify(status.body));
    assert.equal(status.body.data.status, 'READY', JSON.stringify(status.body));
    assert.equal(status.body.data.familyProfile, 'RETAIL_FOOD_CONSUMER');

    const res = await request(app)
      .get(`/v1/moments/${businessMomentId}/story`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .timeout({ deadline: 10_000, response: 8_000 });
    assert.equal(res.status, 200, JSON.stringify(res.body));

    const story = res.body.data;
    assert.equal(story.familyProfile, 'RETAIL_FOOD_CONSUMER');
    // The retail composer owns the page order for a business story.
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
    // Reading must not replace operating-ledger money with Group Finance zeroes,
    // and must not drop the highlight cards the business composer wrote.
    assert.equal(story.snapshot.money.spent, 0);
    assert.ok(
      Array.isArray(story.snapshot.highlights) && story.snapshot.highlights.length >= 8,
      `business story highlights were dropped: ${JSON.stringify(story.snapshot.highlights)}`
    );
    const revenueCard = story.snapshot.highlights.find(
      (card: { id: string }) => card.id === 'rf-revenue'
    );
    assert.ok(revenueCard, 'rf-revenue card missing from business story');
    assert.match(revenueCard.detail, /12500/);
    assert.equal(story.snapshot.metrics.revenue, '12500');
  });
});
