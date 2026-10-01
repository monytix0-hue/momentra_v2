/**
 * Gap 1 — Business activity is company-authorized and moment-scoped.
 * Membership authorizes the read. The actor id does not filter the feed.
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
    .set('Idempotency-Key', `g1-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createMoment(
  uid: string,
  companyId: string,
  momentTypeCode: 'BUSINESS_RUNWAY' | 'BUSINESS_OPERATIONS' | 'TEAM_OPERATIONS',
  title: string
): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g1-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode,
      title,
      companyId,
      businessSetup: { familyCode: momentTypeCode, preferences: {} },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function addMember(ownerUid: string, companyId: string, userId: string, membershipType: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/companies/${companyId}/members`)
    .set('X-Dev-Firebase-Uid', ownerUid)
    .set('Idempotency-Key', `g1-add-${randomUUID()}`)
    .send({ userId, membershipType });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

async function postExpense(uid: string, momentId: string, description: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/moments/${momentId}/business-expenses`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g1-exp-${randomUUID()}`)
    .send({ amount: '15.00', currencyCode: 'INR', description, categoryCode: 'PURCHASE' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

type ActivityItem = {
  title: string;
  activityCode: string;
  occurredAt: string;
  actorDisplayName: string | null;
};

async function listActivity(uid: string, momentId: string, limit = 50): Promise<ActivityItem[]> {
  const collected: ActivityItem[] = [];
  let cursor: string | undefined;
  for (let page = 0; page < 8; page += 1) {
    const res = await request(app)
      .get(`/v1/business/moments/${momentId}/activity`)
      .set('X-Dev-Firebase-Uid', uid)
      .query({ limit, ...(cursor ? { cursor } : {}) });
    assert.equal(res.status, 200, JSON.stringify(res.body));
    const items = (res.body.data.items ?? []) as ActivityItem[];
    collected.push(...items);
    const next = (res.body.data.nextCursor ?? res.body.nextCursor) as string | null;
    if (!next || items.length === 0) break;
    cursor = next;
  }
  return collected;
}

function titlesMatching(items: ActivityItem[], wanted: string[]): string[] {
  const set = new Set(wanted);
  return items.map((item) => item.title).filter((title) => set.has(title));
}

describe('Gap 1 company-authorized Business activity', () => {
  const ownerUid = `g1-own-${randomUUID().slice(0, 8)}`;
  const memberUid = `g1-mem-${randomUUID().slice(0, 8)}`;
  const namelessUid = `g1-noname-${randomUUID().slice(0, 8)}`;
  const observerUid = `g1-obs-${randomUUID().slice(0, 8)}`;
  const outsiderUid = `g1-out-${randomUUID().slice(0, 8)}`;
  const otherOwnerUid = `g1-b-${randomUUID().slice(0, 8)}`;

  const memberExpense = `B money ${randomUUID().slice(0, 8)}`;
  const ownerExpense = `A money ${randomUUID().slice(0, 8)}`;
  const namelessExpense = `Nameless ${randomUUID().slice(0, 8)}`;
  const tieNewerId = `Tie later id ${randomUUID().slice(0, 8)}`;
  const tieOlderId = `Tie earlier id ${randomUUID().slice(0, 8)}`;
  const dailyTitle = `Daily only ${randomUUID().slice(0, 8)}`;
  const teamTitle = `Team only ${randomUUID().slice(0, 8)}`;
  const foreignTitle = `Foreign ${randomUUID().slice(0, 8)}`;
  const moneyTitles = [memberExpense, ownerExpense, namelessExpense, tieNewerId, tieOlderId];

  let moneyId = '';
  let dailyId = '';
  let teamId = '';
  let foreignMoneyId = '';

  before(async () => {
    await getPool().query('SELECT 1');
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@g1.local`, 'Owner A');
    await ensureUser(userIdFor(memberUid), `${memberUid}@g1.local`, 'Member B');
    await ensureUser(userIdFor(namelessUid), `${namelessUid}@g1.local`, null);
    await ensureUser(userIdFor(observerUid), `${observerUid}@g1.local`, 'Observer');
    await ensureUser(userIdFor(outsiderUid), `${outsiderUid}@g1.local`, 'Outsider');
    await ensureUser(userIdFor(otherOwnerUid), `${otherOwnerUid}@g1.local`, 'Owner B');

    const companyId = await createCompany(ownerUid, `Gap1 ${ownerUid}`);
    moneyId = await createMoment(ownerUid, companyId, 'BUSINESS_RUNWAY', 'Gap1 Money');
    dailyId = await createMoment(ownerUid, companyId, 'BUSINESS_OPERATIONS', 'Gap1 Daily');
    teamId = await createMoment(ownerUid, companyId, 'TEAM_OPERATIONS', 'Gap1 Team');

    await addMember(ownerUid, companyId, userIdFor(memberUid), 'MEMBER');
    await addMember(ownerUid, companyId, userIdFor(namelessUid), 'MEMBER');
    await addMember(ownerUid, companyId, userIdFor(observerUid), 'OBSERVER');

    await postExpense(memberUid, moneyId, memberExpense);
    await postExpense(ownerUid, moneyId, ownerExpense);
    await postExpense(namelessUid, moneyId, namelessExpense);
    await postExpense(memberUid, moneyId, tieOlderId);
    await postExpense(ownerUid, moneyId, tieNewerId);

    const issue = await request(app)
      .post(`/v1/moments/${dailyId}/issues`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `g1-iss-${randomUUID()}`)
      .send({ title: dailyTitle, severity: 'LOW' });
    assert.equal(issue.status, 201, JSON.stringify(issue.body));

    const update = await request(app)
      .post(`/v1/moments/${teamId}/business-updates`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `g1-upd-${randomUUID()}`)
      .send({ title: teamTitle, body: 'Shift note' });
    assert.equal(update.status, 201, JSON.stringify(update.body));

    const otherCompany = await createCompany(otherOwnerUid, `Gap1 B ${otherOwnerUid}`);
    foreignMoneyId = await createMoment(otherOwnerUid, otherCompany, 'BUSINESS_RUNWAY', 'Gap1 Foreign');
    await postExpense(otherOwnerUid, foreignMoneyId, foreignTitle);

    await getPool().query(
      `UPDATE projection.recent_activity SET occurred_at = $3::timestamptz
       WHERE scope_id = $1::uuid AND domain_code = 'BUSINESS' AND title = $2`,
      [moneyId, memberExpense, '2099-03-03T00:00:00Z']
    );
    await getPool().query(
      `UPDATE projection.recent_activity SET occurred_at = $3::timestamptz
       WHERE scope_id = $1::uuid AND domain_code = 'BUSINESS' AND title = $2`,
      [moneyId, ownerExpense, '2099-03-02T00:00:00Z']
    );
    await getPool().query(
      `UPDATE projection.recent_activity SET occurred_at = $3::timestamptz
       WHERE scope_id = $1::uuid AND domain_code = 'BUSINESS' AND title = $2`,
      [moneyId, namelessExpense, '2099-03-01T00:00:00Z']
    );
    await getPool().query(
      `UPDATE projection.recent_activity
       SET occurred_at = '2099-04-01T00:00:00Z'::timestamptz
       WHERE scope_id = $1::uuid AND domain_code = 'BUSINESS' AND title = ANY($2::text[])`,
      [moneyId, [tieNewerId, tieOlderId]]
    );
  });

  after(async () => {
    await closePool();
  });

  it('shows the other member’s row to the owner and the owner’s row to the member', async () => {
    const asOwner = await listActivity(ownerUid, moneyId);
    const asMember = await listActivity(memberUid, moneyId);
    assert.ok(asOwner.some((item) => item.title === memberExpense && item.activityCode === 'BUSINESS_EXPENSE'));
    assert.ok(asMember.some((item) => item.title === ownerExpense && item.activityCode === 'BUSINESS_EXPENSE'));
  });

  it('keeps Money rows off Daily and Team', async () => {
    const daily = titlesMatching(await listActivity(ownerUid, dailyId), [...moneyTitles, dailyTitle, teamTitle]);
    const team = titlesMatching(await listActivity(ownerUid, teamId), [...moneyTitles, dailyTitle, teamTitle]);
    assert.deepEqual(daily, [dailyTitle]);
    assert.deepEqual(team, [teamTitle]);
  });

  it('does not show Company A rows to Company B', async () => {
    const foreign = await listActivity(otherOwnerUid, foreignMoneyId);
    assert.ok(foreign.some((item) => item.title === foreignTitle));
    for (const title of [...moneyTitles, dailyTitle, teamTitle]) {
      assert.equal(foreign.some((item) => item.title === title), false, title);
    }
    const local = await listActivity(ownerUid, moneyId);
    assert.equal(local.some((item) => item.title === foreignTitle), false);
  });

  it('returns the existing 403 to a non-member', async () => {
    const res = await request(app)
      .get(`/v1/business/moments/${moneyId}/activity`)
      .set('X-Dev-Firebase-Uid', outsiderUid);
    assert.equal(res.status, 403, JSON.stringify(res.body));
    assert.match(String(res.body.message ?? ''), /Not an active company member/);
  });

  it('lets an observer read and still blocks an expense write', async () => {
    const items = await listActivity(observerUid, moneyId);
    assert.ok(items.some((item) => item.title === memberExpense));
    const write = await request(app)
      .post(`/v1/moments/${moneyId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', observerUid)
      .set('Idempotency-Key', `g1-obs-exp-${randomUUID()}`)
      .send({ amount: '9.00', currencyCode: 'INR', description: 'Observer should fail' });
    assert.equal(write.status, 403, JSON.stringify(write.body));
  });

  it('returns the actor profile name and keeps a row when the name is missing', async () => {
    const items = await listActivity(ownerUid, moneyId);
    const named = items.find((item) => item.title === memberExpense);
    const unnamed = items.find((item) => item.title === namelessExpense);
    assert.ok(named);
    assert.equal(named.actorDisplayName, 'Member B');
    assert.ok(unnamed);
    assert.equal(unnamed.actorDisplayName, null);
  });

  it('orders rows by occurred_at then recent_activity_id, including the pulse bundle', async () => {
    const expected = await getPool().query<{ title: string }>(
      `SELECT title
       FROM projection.recent_activity
       WHERE scope_id = $1::uuid AND domain_code = 'BUSINESS' AND title = ANY($2::text[])
       ORDER BY occurred_at DESC, recent_activity_id DESC`,
      [moneyId, moneyTitles]
    );
    const expectedTitles = expected.rows.map((row) => row.title);
    assert.equal(expectedTitles.length, moneyTitles.length);
    assert.deepEqual(titlesMatching(await listActivity(ownerUid, moneyId, 1), moneyTitles), expectedTitles);

    const pulse = await request(app)
      .get(`/v1/business/moments/${moneyId}/pulse`)
      .set('X-Dev-Firebase-Uid', ownerUid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    const bundled = (pulse.body.data.payload.activity ?? []) as ActivityItem[];
    assert.ok(bundled.length <= 5);
    const bundledKnown = titlesMatching(bundled, moneyTitles);
    assert.deepEqual(bundledKnown, expectedTitles.slice(0, bundledKnown.length));
    const bundledNamed = bundled.find((item) => item.title === memberExpense);
    assert.ok(bundledNamed);
    assert.equal(bundledNamed.actorDisplayName, 'Member B');
  });
});
