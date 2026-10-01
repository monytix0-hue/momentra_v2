/**
 * Gap 4 — Business Memory is one company row with independently owned family slices.
 * A family refresh must not replace another family's memories.
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
import { refreshBusinessMemoryProjection } from '../src/modules/business/business-projection';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

type Family = 'BUSINESS_RUNWAY' | 'BUSINESS_OPERATIONS' | 'TEAM_OPERATIONS';

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, 'Gap4 Memory']
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g4-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createMoment(uid: string, companyId: string, momentTypeCode: Family, title: string): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g4-mom-${randomUUID()}`)
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

async function postMemory(uid: string, momentId: string, title: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/business/moments/${momentId}/memories`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g4-mem-${randomUUID()}`)
    .send({ title });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

async function refreshMemory(companyId: string, momentId: string): Promise<void> {
  const client = await getPool().connect();
  try {
    await client.query('BEGIN');
    await refreshBusinessMemoryProjection(client, companyId, momentId);
    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

type MemoryItem = { title: string; businessFamily: string };

async function getMemory(uid: string, momentId: string): Promise<MemoryItem[]> {
  const res = await request(app)
    .get(`/v1/business/moments/${momentId}/memory`)
    .set('X-Dev-Firebase-Uid', uid);
  assert.equal(res.status, 200, JSON.stringify(res.body));
  return (res.body.data.payload.items ?? []) as MemoryItem[];
}

describe('Gap 4 Business Memory projection integrity', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('keeps each family slice and returns the same company set from any moment', async () => {
    const uid = `g4-a-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@g4.local`);
    const companyId = await createCompany(uid, `MemA ${uid}`);
    const moneyId = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Money');
    const dailyId = await createMoment(uid, companyId, 'BUSINESS_OPERATIONS', 'Daily');
    const teamId = await createMoment(uid, companyId, 'TEAM_OPERATIONS', 'Team');
    const moneyTitle = `Money learn ${uid}`;
    const dailyTitle = `Daily learn ${uid}`;
    const teamTitle = `Team learn ${uid}`;

    await postMemory(uid, moneyId, moneyTitle);
    await postMemory(uid, dailyId, dailyTitle);
    await postMemory(uid, teamId, teamTitle);
    await refreshMemory(companyId, moneyId);

    const viaMoney = await getMemory(uid, moneyId);
    const viaDaily = await getMemory(uid, dailyId);
    assert.deepEqual(
      viaMoney.map((item) => item.title),
      viaDaily.map((item) => item.title)
    );
    assert.deepEqual(
      viaMoney.map((item) => item.businessFamily),
      ['BUSINESS_RUNWAY', 'BUSINESS_OPERATIONS', 'TEAM_OPERATIONS']
    );
    assert.deepEqual(
      viaMoney.map((item) => item.title),
      [moneyTitle, dailyTitle, teamTitle]
    );

    const uidB = `g4-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidB), `${uidB}@g4.local`);
    const companyB = await createCompany(uidB, `MemB ${uidB}`);
    const teamB = await createMoment(uidB, companyB, 'TEAM_OPERATIONS', 'Team B');
    const dailyB = await createMoment(uidB, companyB, 'BUSINESS_OPERATIONS', 'Daily B');
    const moneyB = await createMoment(uidB, companyB, 'BUSINESS_RUNWAY', 'Money B');
    const moneyBTitle = `B money ${uidB}`;
    const dailyBTitle = `B daily ${uidB}`;
    const teamBTitle = `B team ${uidB}`;
    await postMemory(uidB, teamB, teamBTitle);
    await postMemory(uidB, dailyB, dailyBTitle);
    await postMemory(uidB, moneyB, moneyBTitle);

    const viaTeamB = await getMemory(uidB, teamB);
    assert.deepEqual(
      viaTeamB.map((item) => item.businessFamily),
      ['BUSINESS_RUNWAY', 'BUSINESS_OPERATIONS', 'TEAM_OPERATIONS']
    );
    const encoded = JSON.stringify(viaTeamB);
    assert.equal(encoded.includes(moneyTitle), false);
    assert.equal(encoded.includes(dailyTitle), false);
    assert.equal(encoded.includes(teamTitle), false);
  });

  it('an empty Money refresh does not clear Daily, and both Money moments contribute', async () => {
    const uid = `g4-m-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@g4.local`);
    const companyId = await createCompany(uid, `MemM ${uid}`);
    const moneyA = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Money A');
    const moneyB = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Money B');
    const dailyId = await createMoment(uid, companyId, 'BUSINESS_OPERATIONS', 'Daily');
    const first = `First money ${uid}`;
    const second = `Second money ${uid}`;
    const daily = `Daily stays ${uid}`;
    await postMemory(uid, moneyA, first);
    await postMemory(uid, moneyB, second);
    await postMemory(uid, dailyId, daily);
    await refreshMemory(companyId, moneyA);

    const items = await getMemory(uid, dailyId);
    const titles = items.map((item) => item.title);
    assert.equal(titles.includes(first), true);
    assert.equal(titles.includes(second), true);
    assert.equal(titles.includes(daily), true);
    assert.deepEqual(
      items.filter((item) => item.businessFamily === 'BUSINESS_RUNWAY').map((item) => item.title).sort(),
      [first, second].sort()
    );

    const viaOtherMoney = await getMemory(uid, moneyB);
    assert.deepEqual(
      viaOtherMoney.map((item) => item.title),
      items.map((item) => item.title)
    );
  });
});
