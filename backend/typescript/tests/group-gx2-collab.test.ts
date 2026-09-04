/**
 * GX2-C Group collaboration APIs: planning, booking, poll, update, purchase, resident, memory.
 * Also proves membership isolation, audit, event, and outbox.
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

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, 'GX2 Collab']
  );
}

async function createGroupMomentWithTwoMembers(
  organizerUid: string,
  memberUid: string,
  momentTypeCode = 'TRIP'
): Promise<{ momentId: string }> {
  await ensureUser(userIdFor(organizerUid), `${organizerUid}@test.local`);
  await ensureUser(userIdFor(memberUid), `${memberUid}@test.local`);

  const mint = await request(app)
    .post('/v1/group/invites')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `gx2-mint-${randomUUID()}`)
    .send({ title: `GX2 ${momentTypeCode}`, momentTypeCode });
  assert.equal(mint.status, 201, JSON.stringify(mint.body));

  const created = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `gx2-moment-${randomUUID()}`)
    .send({
      domainCode: 'GROUP',
      momentTypeCode,
      title: `GX2 ${momentTypeCode}`,
      inviteCode: mint.body.data.inviteCode,
    });
  assert.equal(created.status, 201, JSON.stringify(created.body));
  const momentId = created.body.data.momentId as string;

  const redeem = await request(app)
    .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
    .set('X-Dev-Firebase-Uid', memberUid)
    .set('Idempotency-Key', `gx2-redeem-${randomUUID()}`)
    .send({});
  assert.equal(redeem.status, 200, JSON.stringify(redeem.body));
  return { momentId };
}

describe('GX2-C Group collaboration', () => {
  const organizerUid = `gx2-org-${randomUUID()}`;
  const memberUid = `gx2-mem-${randomUUID()}`;
  const outsiderUid = `gx2-out-${randomUUID()}`;

  before(async () => {
    await getPool().query('SELECT 1');
    await ensureUser(userIdFor(organizerUid), `${organizerUid}@test.local`);
    await ensureUser(userIdFor(memberUid), `${memberUid}@test.local`);
    await ensureUser(userIdFor(outsiderUid), `${outsiderUid}@test.local`);
  });

  after(async () => {
    await closePool();
  });

  it('creates planning/booking/poll/update/memory and lists them; outsider is denied', async () => {
    const { momentId } = await createGroupMomentWithTwoMembers(organizerUid, memberUid);

    const plan = await request(app)
      .post(`/v1/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gx2-plan-${randomUUID()}`)
      .send({ title: 'Day 1 — Airport pickup' });
    assert.equal(plan.status, 201, JSON.stringify(plan.body));
    assert.ok(plan.body.data.planningItemId);

    const booking = await request(app)
      .post(`/v1/moments/${momentId}/bookings`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `gx2-book-${randomUUID()}`)
      .send({ title: 'Hotel Nova' });
    assert.equal(booking.status, 201, JSON.stringify(booking.body));

    const poll = await request(app)
      .post(`/v1/moments/${momentId}/polls`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gx2-poll-${randomUUID()}`)
      .send({ question: 'Dinner venue?', options: ['Italian', 'Thai'] });
    assert.equal(poll.status, 201, JSON.stringify(poll.body));
    const pollId = poll.body.data.pollId as string;

    const pollDetail = await request(app)
      .get(`/v1/polls/${pollId}`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(pollDetail.status, 200, JSON.stringify(pollDetail.body));
    assert.equal(pollDetail.body.data.status, 'OPEN');
    assert.equal(pollDetail.body.data.options.length, 2);
    assert.equal(pollDetail.body.data.canClose, false);
    assert.ok(pollDetail.body.data.createdByUserId);

    const pollDetailOrg = await request(app)
      .get(`/v1/polls/${pollId}`)
      .set('X-Dev-Firebase-Uid', organizerUid);
    assert.equal(pollDetailOrg.status, 200, JSON.stringify(pollDetailOrg.body));
    assert.equal(pollDetailOrg.body.data.canClose, true);

    const optionId = pollDetail.body.data.options[0].pollOptionId as string;

    const vote = await request(app)
      .post(`/v1/polls/${pollId}/votes`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `gx2-vote-${randomUUID()}`)
      .send({ pollOptionId: optionId });
    assert.equal(vote.status, 201, JSON.stringify(vote.body));

    const voteRow = await getPool().query(
      `SELECT 1 FROM shared.poll_vote WHERE poll_id = $1 AND voter_user_id = $2`,
      [pollId, userIdFor(memberUid)]
    );
    assert.equal(voteRow.rows.length, 1);

    const memberCloseDenied = await request(app)
      .post(`/v1/polls/${pollId}/close`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `gx2-close-deny-${randomUUID()}`)
      .send({});
    assert.equal(memberCloseDenied.status, 403, JSON.stringify(memberCloseDenied.body));

    const close = await request(app)
      .post(`/v1/polls/${pollId}/close`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gx2-close-${randomUUID()}`)
      .send({});
    assert.equal(close.status, 200, JSON.stringify(close.body));
    assert.equal(close.body.data.status, 'CLOSED');

    const postCloseVote = await request(app)
      .post(`/v1/polls/${pollId}/votes`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gx2-vote2-${randomUUID()}`)
      .send({ pollOptionId: optionId });
    assert.equal(postCloseVote.status, 409, JSON.stringify(postCloseVote.body));

    const update = await request(app)
      .post(`/v1/moments/${momentId}/updates`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `gx2-upd-${randomUUID()}`)
      .send({ message: 'Landing at 4pm' });
    assert.equal(update.status, 201, JSON.stringify(update.body));

    const memory = await request(app)
      .post(`/v1/moments/${momentId}/memories`)
      .set('X-Dev-Firebase-Uid', organizerUid)
      .set('Idempotency-Key', `gx2-mem-${randomUUID()}`)
      .send({ title: 'Sunset at the pier' });
    assert.equal(memory.status, 201, JSON.stringify(memory.body));

    const plans = await request(app)
      .get(`/v1/group/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(plans.status, 200, JSON.stringify(plans.body));
    assert.ok(plans.body.data.items.length >= 1);
    assert.ok(plans.body.data.openCount >= 1);

    const life = await request(app)
      .get(`/v1/group/moments/${momentId}/life`)
      .set('X-Dev-Firebase-Uid', organizerUid);
    assert.equal(life.status, 200, JSON.stringify(life.body));
    assert.notEqual(life.body.data.payload?.sections?.planning, 'API_GAP');
    assert.ok((life.body.data.payload?.planningItems?.length ?? 0) >= 1);
    assert.equal(life.body.data.payload?.metricVersion, 'life-v1-provisional');
    assert.ok(life.body.data.payload?.domains);
    assert.ok(life.body.data.payload?.domains?.experience?.score != null);
    assert.ok(life.body.data.payload?.domains?.community?.score != null);
    assert.ok(life.body.data.payload?.health?.score != null);
    assert.ok(life.body.data.payload?.counts?.planningCount >= 1);
    assert.ok(Array.isArray(life.body.data.payload?.drivers));
    assert.ok(Array.isArray(life.body.data.payload?.activity));

    const memFacet = await request(app)
      .get(`/v1/group/moments/${momentId}/memory`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(memFacet.status, 200, JSON.stringify(memFacet.body));
    assert.ok((memFacet.body.data.payload?.memoryCount ?? 0) >= 1);

    const pulse = await request(app)
      .get(`/v1/group/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', organizerUid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    assert.ok((pulse.body.data.payload?.openTaskCount ?? 0) >= 1);

    const denied = await request(app)
      .get(`/v1/group/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', outsiderUid);
    assert.equal(denied.status, 403);

    const deniedWrite = await request(app)
      .post(`/v1/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', outsiderUid)
      .set('Idempotency-Key', `gx2-deny-${randomUUID()}`)
      .send({ title: 'Should fail' });
    assert.equal(deniedWrite.status, 403);
  });

  it('allows poll creator (non-organizer) to close their own poll', async () => {
    const orgUid = `gx2-pc-org-${randomUUID()}`;
    const memUid = `gx2-pc-mem-${randomUUID()}`;
    const { momentId } = await createGroupMomentWithTwoMembers(orgUid, memUid);

    const poll = await request(app)
      .post(`/v1/moments/${momentId}/polls`)
      .set('X-Dev-Firebase-Uid', memUid)
      .set('Idempotency-Key', `gx2-pc-poll-${randomUUID()}`)
      .send({ question: 'Lunch spot?', options: ['Cafe', 'Park'] });
    assert.equal(poll.status, 201, JSON.stringify(poll.body));
    const pollId = poll.body.data.pollId as string;

    const asCreator = await request(app)
      .get(`/v1/polls/${pollId}`)
      .set('X-Dev-Firebase-Uid', memUid);
    assert.equal(asCreator.status, 200, JSON.stringify(asCreator.body));
    assert.equal(asCreator.body.data.canClose, true);
    assert.equal(asCreator.body.data.createdByUserId, userIdFor(memUid));

    const close = await request(app)
      .post(`/v1/polls/${pollId}/close`)
      .set('X-Dev-Firebase-Uid', memUid)
      .set('Idempotency-Key', `gx2-pc-close-${randomUUID()}`)
      .send({});
    assert.equal(close.status, 200, JSON.stringify(close.body));
    assert.equal(close.body.data.status, 'CLOSED');
  });

  it('GET /life returns null domain scores when empty, populated when seeded', async () => {
    const emptyOrg = `gx2-life-e-${randomUUID()}`;
    const emptyMem = `gx2-life-m-${randomUUID()}`;
    const { momentId: emptyId } = await createGroupMomentWithTwoMembers(emptyOrg, emptyMem, 'TRIP');

    const emptyLife = await request(app)
      .get(`/v1/group/moments/${emptyId}/life`)
      .set('X-Dev-Firebase-Uid', emptyOrg);
    assert.equal(emptyLife.status, 200, JSON.stringify(emptyLife.body));
    const emptyPayload = emptyLife.body.data.payload;
    assert.equal(emptyPayload?.metricVersion, 'life-v1-provisional');
    // Participant-only signal may populate community; experience/purchase/living/goal stay null without collab.
    assert.equal(emptyPayload?.domains?.experience?.score ?? null, null);
    assert.equal(emptyPayload?.domains?.purchase?.score ?? null, null);
    assert.equal(emptyPayload?.domains?.living?.score ?? null, null);
    assert.equal(emptyPayload?.domains?.goal?.score ?? null, null);
    assert.equal(emptyPayload?.planningItems?.length ?? 0, 0);

    await request(app)
      .post(`/v1/moments/${emptyId}/planning-items`)
      .set('X-Dev-Firebase-Uid', emptyOrg)
      .set('Idempotency-Key', `gx2-life-plan-${randomUUID()}`)
      .send({ title: 'Life seed plan' })
      .expect(201);

    await request(app)
      .post(`/v1/moments/${emptyId}/updates`)
      .set('X-Dev-Firebase-Uid', emptyMem)
      .set('Idempotency-Key', `gx2-life-upd-${randomUUID()}`)
      .send({ message: 'Life seed update' })
      .expect(201);

    const seeded = await request(app)
      .get(`/v1/group/moments/${emptyId}/life`)
      .set('X-Dev-Firebase-Uid', emptyOrg);
    assert.equal(seeded.status, 200, JSON.stringify(seeded.body));
    const seededPayload = seeded.body.data.payload;
    assert.ok(seededPayload?.domains?.experience?.score != null);
    assert.ok(seededPayload?.domains?.goal?.score != null);
    assert.ok(seededPayload?.domains?.community?.score != null);
    assert.ok(seededPayload?.health?.score != null);
    assert.ok((seededPayload?.counts?.planningCount ?? 0) >= 1);
    assert.ok((seededPayload?.counts?.updateCount ?? 0) >= 1);
  });

  it('supports purchase items (Gift Pool) and residents (Flatmates) with isolation', async () => {
    const giftOrg = `gx2-gift-org-${randomUUID()}`;
    const giftMem = `gx2-gift-mem-${randomUUID()}`;
    const { momentId: giftId } = await createGroupMomentWithTwoMembers(giftOrg, giftMem, 'GIFT_POOL');

    const purchase = await request(app)
      .post(`/v1/moments/${giftId}/purchase-items`)
      .set('X-Dev-Firebase-Uid', giftOrg)
      .set('Idempotency-Key', `gx2-pi-${randomUUID()}`)
      .send({ label: 'Coffee machine', amount: '12000' });
    assert.equal(purchase.status, 201, JSON.stringify(purchase.body));

    const listed = await request(app)
      .get(`/v1/group/moments/${giftId}/purchase-items`)
      .set('X-Dev-Firebase-Uid', giftMem);
    assert.equal(listed.status, 200, JSON.stringify(listed.body));
    assert.ok(listed.body.data.items.length >= 1);

    const delivery = await request(app)
      .post(`/v1/moments/${giftId}/delivery-handovers`)
      .set('X-Dev-Firebase-Uid', giftOrg)
      .set('Idempotency-Key', `gx2-dh-${randomUUID()}`)
      .send({
        recipientName: 'Recipient',
        handoverType: 'DELIVERY',
        scheduledAt: '2026-03-21',
        address: 'Mom house, Sector 15',
        note: 'Gift wrap requested',
      });
    assert.equal(delivery.status, 201, JSON.stringify(delivery.body));

    const deliveries = await request(app)
      .get(`/v1/group/moments/${giftId}/delivery-handovers`)
      .set('X-Dev-Firebase-Uid', giftMem);
    assert.equal(deliveries.status, 200, JSON.stringify(deliveries.body));
    assert.ok(deliveries.body.data.items.length >= 1);

    const ownership = await request(app)
      .post(`/v1/moments/${giftId}/ownership-records`)
      .set('X-Dev-Firebase-Uid', giftOrg)
      .set('Idempotency-Key', `gx2-own-${randomUUID()}`)
      .send({
        assetLabel: 'Coffee machine',
        fromOwnerName: 'Group pool',
        ownershipShare: 1,
        effectiveAt: '2026-03-22',
      });
    assert.equal(ownership.status, 201, JSON.stringify(ownership.body));

    const ownershipList = await request(app)
      .get(`/v1/group/moments/${giftId}/ownership-records`)
      .set('X-Dev-Firebase-Uid', giftMem);
    assert.equal(ownershipList.status, 200, JSON.stringify(ownershipList.body));
    assert.ok(ownershipList.body.data.items.length >= 1);

    const flatOrg = `gx2-fo-${randomUUID()}`;
    const flatMem = `gx2-fm-${randomUUID()}`;
    const { momentId: livingId } = await createGroupMomentWithTwoMembers(flatOrg, flatMem, 'FLATMATES');

    const resident = await request(app)
      .post(`/v1/moments/${livingId}/residents`)
      .set('X-Dev-Firebase-Uid', flatOrg)
      .set('Idempotency-Key', `gx2-res-${randomUUID()}`)
      .send({ name: 'Alex', roleCode: 'RESIDENT' });
    assert.equal(resident.status, 201, JSON.stringify(resident.body));

    const residents = await request(app)
      .get(`/v1/group/moments/${livingId}/residents`)
      .set('X-Dev-Firebase-Uid', flatMem);
    assert.equal(residents.status, 200, JSON.stringify(residents.body));
    assert.ok(residents.body.data.items.length >= 1);

    const iso = await request(app)
      .get(`/v1/group/moments/${livingId}/residents`)
      .set('X-Dev-Firebase-Uid', giftMem);
    assert.equal(iso.status, 403);
  });

  it('JOIN golden path: Trip / Gift Pool / Flatmates create → SQL → GET list → life/pulse', async () => {
    // Trip — planning + booking + life/pulse
    const tripOrg = `join-trip-o-${randomUUID()}`;
    const tripMem = `join-trip-m-${randomUUID()}`;
    const { momentId: tripId } = await createGroupMomentWithTwoMembers(tripOrg, tripMem, 'TRIP');

    const plan = await request(app)
      .post(`/v1/moments/${tripId}/planning-items`)
      .set('X-Dev-Firebase-Uid', tripOrg)
      .set('Idempotency-Key', `join-plan-${randomUUID()}`)
      .send({ title: 'Join Trip Plan' });
    assert.equal(plan.status, 201, JSON.stringify(plan.body));
    const planningItemId = plan.body.data.planningItemId as string;

    const sqlPlan = await getPool().query(
      `SELECT 1 FROM collaboration.planning_item WHERE planning_item_id = $1 AND moment_id = $2`,
      [planningItemId, tripId]
    );
    assert.equal(sqlPlan.rows.length, 1);

    const planList = await request(app)
      .get(`/v1/group/moments/${tripId}/planning-items`)
      .set('X-Dev-Firebase-Uid', tripMem);
    assert.equal(planList.status, 200);
    assert.ok(planList.body.data.items.some((i: { planningItemId: string }) => i.planningItemId === planningItemId));

    const tripLife = await request(app)
      .get(`/v1/group/moments/${tripId}/life`)
      .set('X-Dev-Firebase-Uid', tripOrg);
    assert.equal(tripLife.status, 200);
    assert.ok((tripLife.body.data.payload?.planningItems?.length ?? 0) >= 1);

    const tripPulse = await request(app)
      .get(`/v1/group/moments/${tripId}/pulse`)
      .set('X-Dev-Firebase-Uid', tripOrg);
    assert.equal(tripPulse.status, 200);
    assert.ok((tripPulse.body.data.payload?.openTaskCount ?? 0) >= 1);

    // Gift Pool — purchase item
    const giftOrg = `join-gift-o-${randomUUID()}`;
    const giftMem = `join-gift-m-${randomUUID()}`;
    const { momentId: giftId } = await createGroupMomentWithTwoMembers(giftOrg, giftMem, 'GIFT_POOL');
    const purchase = await request(app)
      .post(`/v1/moments/${giftId}/purchase-items`)
      .set('X-Dev-Firebase-Uid', giftOrg)
      .set('Idempotency-Key', `join-pi-${randomUUID()}`)
      .send({ label: 'Join Gift', amount: '500' });
    assert.equal(purchase.status, 201, JSON.stringify(purchase.body));
    const purchaseItemId = purchase.body.data.purchaseItemId as string;
    const sqlPi = await getPool().query(
      `SELECT 1 FROM collaboration.purchase_item WHERE purchase_item_id = $1 AND moment_id = $2`,
      [purchaseItemId, giftId]
    );
    assert.equal(sqlPi.rows.length, 1);
    const piList = await request(app)
      .get(`/v1/group/moments/${giftId}/purchase-items`)
      .set('X-Dev-Firebase-Uid', giftMem);
    assert.equal(piList.status, 200);
    assert.ok(piList.body.data.items.some((i: { purchaseItemId: string }) => i.purchaseItemId === purchaseItemId));

    // Flatmates — resident
    const flatOrg = `join-flat-o-${randomUUID()}`;
    const flatMem = `join-flat-m-${randomUUID()}`;
    const { momentId: flatId } = await createGroupMomentWithTwoMembers(flatOrg, flatMem, 'FLATMATES');
    const resident = await request(app)
      .post(`/v1/moments/${flatId}/residents`)
      .set('X-Dev-Firebase-Uid', flatOrg)
      .set('Idempotency-Key', `join-res-${randomUUID()}`)
      .send({ name: 'Join Resident', roleCode: 'RESIDENT' });
    assert.equal(resident.status, 201, JSON.stringify(resident.body));
    const residentId = resident.body.data.residentId as string;
    const sqlRes = await getPool().query(
      `SELECT 1 FROM collaboration.resident WHERE resident_id = $1 AND moment_id = $2`,
      [residentId, flatId]
    );
    assert.equal(sqlRes.rows.length, 1);
    const resList = await request(app)
      .get(`/v1/group/moments/${flatId}/residents`)
      .set('X-Dev-Firebase-Uid', flatMem);
    assert.equal(resList.status, 200);
    assert.ok(resList.body.data.items.some((i: { residentId: string }) => i.residentId === residentId));
  });

  it('writes audit + domain event + outbox for planning create', async () => {
    const org = `gx2-a-org-${randomUUID()}`;
    const mem = `gx2-a-mem-${randomUUID()}`;
    const { momentId } = await createGroupMomentWithTwoMembers(org, mem);

    const plan = await request(app)
      .post(`/v1/moments/${momentId}/planning-items`)
      .set('X-Dev-Firebase-Uid', org)
      .set('Idempotency-Key', `gx2-audit-${randomUUID()}`)
      .send({ title: 'Audit trail item' });
    assert.equal(plan.status, 201, JSON.stringify(plan.body));
    const planningItemId = plan.body.data.planningItemId as string;

    const event = await getPool().query(
      `SELECT domain_event_id FROM events.domain_event
       WHERE aggregate_id = $1::uuid AND event_name = 'PlanningItemCreated'
       LIMIT 1`,
      [planningItemId]
    );
    assert.equal(event.rows.length, 1);

    const outbox = await getPool().query(
      `SELECT 1 FROM events.outbox_event oe
       JOIN events.domain_event de ON de.domain_event_id = oe.domain_event_id
       WHERE de.aggregate_id = $1::uuid
       LIMIT 1`,
      [planningItemId]
    );
    assert.equal(outbox.rows.length, 1);

    const audit = await getPool().query(
      `SELECT 1 FROM audit.audit_record
       WHERE resource_id = $1::uuid AND action_code = 'PLANNING_ITEM_CREATE'
       LIMIT 1`,
      [planningItemId]
    );
    assert.equal(audit.rows.length, 1);
  });
});
