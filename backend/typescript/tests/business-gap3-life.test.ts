/**
 * Gap 3 — Business Life is one company row with independently owned family slices.
 * A family refresh must not replace another family's payload or company runway.
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
import {
  refreshBusinessLifeProjection,
  refreshBusinessPulseProjection,
} from '../src/modules/business/business-projection';

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
    [userId, email, 'Gap3 Life']
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g3-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createMoment(
  uid: string,
  companyId: string,
  momentTypeCode: Family,
  title: string,
  preferences: Record<string, unknown>
): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g3-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode,
      title,
      companyId,
      businessSetup: { familyCode: momentTypeCode, preferences },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function postExpense(uid: string, momentId: string, description: string, amount = '10000.00'): Promise<void> {
  const res = await request(app)
    .post(`/v1/moments/${momentId}/business-expenses`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g3-exp-${randomUUID()}`)
    .send({ amount, currencyCode: 'INR', description, categoryCode: 'PURCHASE' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

async function createVendor(uid: string, companyId: string, name: string): Promise<void> {
  const res = await request(app)
    .post(`/v1/companies/${companyId}/vendors`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `g3-ven-${randomUUID()}`)
    .send({ name });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

async function refresh(companyId: string, momentId: string, family: Family): Promise<void> {
  const client = await getPool().connect();
  try {
    await client.query('BEGIN');
    await refreshBusinessPulseProjection(client, companyId, momentId);
    await refreshBusinessLifeProjection(client, companyId, momentId, family);
    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

type StoredLife = {
  runway: Record<string, unknown>;
  daily: Record<string, unknown>;
  team: Record<string, unknown>;
  vendor: Record<string, unknown>;
  runwayMonths: string | null;
  health: string | null;
};

async function readStored(companyId: string): Promise<StoredLife> {
  const life = await getPool().query<{
    runway_payload: Record<string, unknown>;
    business_operations_payload: Record<string, unknown>;
    team_operations_payload: Record<string, unknown>;
    vendor_operations_payload: Record<string, unknown>;
  }>(
    `SELECT runway_payload, business_operations_payload, team_operations_payload, vendor_operations_payload
     FROM projection.business_life WHERE company_id = $1`,
    [companyId]
  );
  const pulse = await getPool().query<{ runway_months: string | null; financial_health_score: string | null }>(
    `SELECT runway_months::text, financial_health_score::text
     FROM projection.business_pulse WHERE company_id = $1`,
    [companyId]
  );
  const row = life.rows[0];
  assert.ok(row, 'business_life row missing');
  return {
    runway: row.runway_payload,
    daily: row.business_operations_payload,
    team: row.team_operations_payload,
    vendor: row.vendor_operations_payload,
    runwayMonths: pulse.rows[0]?.runway_months ?? null,
    health: pulse.rows[0]?.financial_health_score ?? null,
  };
}

async function getLife(uid: string, momentId: string): Promise<Record<string, unknown>> {
  const res = await request(app)
    .get(`/v1/business/moments/${momentId}/life`)
    .set('X-Dev-Firebase-Uid', uid);
  assert.equal(res.status, 200, JSON.stringify(res.body));
  return res.body.data as Record<string, unknown>;
}

function lifeCore(data: Record<string, unknown>): Record<string, unknown> {
  const payload = data.payload as Record<string, unknown>;
  const kpis = payload.kpis as Record<string, unknown>;
  return {
    runwayPayload: payload.runwayPayload,
    businessOperationsPayload: payload.businessOperationsPayload,
    teamOperationsPayload: payload.teamOperationsPayload,
    vendorOperationsPayload: payload.vendorOperationsPayload,
    runwayMonths: kpis.runwayMonths,
    financialHealthScore: kpis.financialHealthScore,
    signals: payload.signals,
    modules: payload.modules,
  };
}

describe('Gap 3 Business Life projection integrity', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('family refresh preserves the other slices, and load order does not change the result', async () => {
    const uidA = `g3-a-${randomUUID().slice(0, 8)}`;
    const uidB = `g3-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@g3.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@g3.local`);

    async function build(uid: string, name: string) {
      const companyId = await createCompany(uid, name);
      const moneyId = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', `${name} Money`, {
        availableCash: '50000',
        monthlySpending: '10000',
        warningThreshold: '6 months',
        businessStage: 'Scaling',
      });
      const dailyId = await createMoment(uid, companyId, 'BUSINESS_OPERATIONS', `${name} Daily`, {
        monthlyBudget: '80000',
        monitoringStyle: 'Proactive',
      });
      const teamId = await createMoment(uid, companyId, 'TEAM_OPERATIONS', `${name} Team`, {
        reviewCycle: 'Weekly',
      });
      await getPool().query(
        `UPDATE business.business_system_setup
         SET preferences = preferences || '{"teamSize":"4","reviewCadence":"Weekly"}'::jsonb
         WHERE moment_id = $1`,
        [teamId]
      );
      await createVendor(uid, companyId, `${name} Vendor`);
      await postExpense(uid, moneyId, `${name} expense`);
      return { companyId, moneyId, dailyId, teamId };
    }

    const a = await build(uidA, `OrderA ${uidA}`);
    const b = await build(uidB, `OrderB ${uidB}`);

    const forward: Array<[string, Family]> = [
      [a.moneyId, 'BUSINESS_RUNWAY'],
      [a.dailyId, 'BUSINESS_OPERATIONS'],
      [a.teamId, 'TEAM_OPERATIONS'],
    ];
    for (const [momentId, family] of forward) {
      await refresh(a.companyId, momentId, family);
    }
    const reverse: Array<[string, Family]> = [
      [b.teamId, 'TEAM_OPERATIONS'],
      [b.dailyId, 'BUSINESS_OPERATIONS'],
      [b.moneyId, 'BUSINESS_RUNWAY'],
    ];
    for (const [momentId, family] of reverse) {
      await refresh(b.companyId, momentId, family);
    }

    const storedA = await readStored(a.companyId);
    const storedB = await readStored(b.companyId);
    assert.equal(storedA.runway.availableCash, '50000');
    assert.equal(storedA.daily.monthlyBudget, '80000');
    assert.equal(storedA.team.memberCapacity, '4');
    assert.equal(storedA.vendor.activeVendorCount, 1);
    assert.deepEqual(storedA.runway, storedB.runway);
    assert.deepEqual(storedA.daily, storedB.daily);
    assert.deepEqual(storedA.team, storedB.team);
    assert.deepEqual(storedA.vendor, storedB.vendor);
    assert.equal(storedA.runwayMonths, storedB.runwayMonths);
    assert.equal(storedA.health, storedB.health);
    assert.ok(storedA.runwayMonths != null, 'money refresh should record runway months');

    const beforeDaily = await readStored(a.companyId);
    await refresh(a.companyId, a.dailyId, 'BUSINESS_OPERATIONS');
    const afterDaily = await readStored(a.companyId);
    assert.deepEqual(afterDaily.runway, beforeDaily.runway);
    assert.deepEqual(afterDaily.team, beforeDaily.team);
    assert.equal(afterDaily.runwayMonths, beforeDaily.runwayMonths);
    assert.equal(afterDaily.health, beforeDaily.health);
    assert.deepEqual(afterDaily.vendor, beforeDaily.vendor);

    await refresh(a.companyId, a.teamId, 'TEAM_OPERATIONS');
    const afterTeam = await readStored(a.companyId);
    assert.deepEqual(afterTeam.runway, beforeDaily.runway);
    assert.deepEqual(afterTeam.daily, afterDaily.daily);
    assert.equal(afterTeam.runwayMonths, beforeDaily.runwayMonths);
    assert.equal(afterTeam.health, beforeDaily.health);
    assert.deepEqual(afterTeam.vendor, beforeDaily.vendor);

    await refresh(a.companyId, a.moneyId, 'BUSINESS_RUNWAY');
    const afterMoney = await readStored(a.companyId);
    assert.deepEqual(afterMoney.daily, afterDaily.daily);
    assert.deepEqual(afterMoney.team, afterTeam.team);
    assert.deepEqual(afterMoney.vendor, beforeDaily.vendor);
    assert.equal(afterMoney.runway.availableCash, '50000');

    const viaMoney = lifeCore(await getLife(uidA, a.moneyId));
    const viaDaily = lifeCore(await getLife(uidA, a.dailyId));
    assert.deepEqual(viaDaily, viaMoney);
  });

  it('one company does not leak into another', async () => {
    const uidA = `g3-iso-a-${randomUUID().slice(0, 8)}`;
    const uidB = `g3-iso-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@g3.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@g3.local`);
    const cash = `73421`;
    const issueTitle = `Gap3 only A ${randomUUID().slice(0, 8)}`;
    const expenseTitle = `Gap3 cash A ${randomUUID().slice(0, 8)}`;

    const companyA = await createCompany(uidA, `IsoA ${uidA}`);
    const moneyA = await createMoment(uidA, companyA, 'BUSINESS_RUNWAY', 'Iso Money', {
      availableCash: cash,
      warningThreshold: '6 months',
    });
    await postExpense(uidA, moneyA, expenseTitle, '15.00');
    const issue = await request(app)
      .post(`/v1/moments/${moneyA}/issues`)
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `g3-iss-${randomUUID()}`)
      .send({ title: issueTitle, severity: 'HIGH' });
    assert.equal(issue.status, 201, JSON.stringify(issue.body));

    const companyB = await createCompany(uidB, `IsoB ${uidB}`);
    const moneyB = await createMoment(uidB, companyB, 'BUSINESS_RUNWAY', 'Iso B Money', {
      availableCash: '10',
      warningThreshold: '6 months',
    });
    const lifeB = await getLife(uidB, moneyB);
    const encoded = JSON.stringify(lifeB);
    assert.equal(encoded.includes(cash), false);
    assert.equal(encoded.includes(issueTitle), false);
    assert.equal(encoded.includes(expenseTitle), false);
    assert.equal((lifeB.companyId as string) === companyA, false);
  });

  it('uses the earliest active family moment, then the lower moment id', async () => {
    const uid = `g3-can-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@g3.local`);
    const companyId = await createCompany(uid, `Canon ${uid}`);
    const firstId = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Money first', {
      availableCash: '40000',
      warningThreshold: '3 months',
    });
    const secondId = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Money second', {
      availableCash: '40000',
      warningThreshold: '3 months',
    });
    const dailyId = await createMoment(uid, companyId, 'BUSINESS_OPERATIONS', 'Daily lens', {
      monthlyBudget: '1000',
    });

    const lowerId = firstId < secondId ? firstId : secondId;
    const higherId = firstId < secondId ? secondId : firstId;
    await getPool().query(
      `UPDATE business.business_moment_context
       SET created_at = '2020-01-01T00:00:00Z'
       WHERE moment_id = ANY($1::uuid[])`,
      [[lowerId, higherId]]
    );
    await getPool().query(
      `UPDATE business.business_system_setup
       SET preferences = preferences || '{"availableCash":"40000","warningThreshold":"9 months"}'::jsonb
       WHERE moment_id = $1`,
      [lowerId]
    );
    await getPool().query(
      `UPDATE business.business_system_setup
       SET preferences = preferences || '{"availableCash":"40000","warningThreshold":"24 months"}'::jsonb
       WHERE moment_id = $1`,
      [higherId]
    );
    await postExpense(uid, lowerId, 'Canonical burn', '10000.00');
    await postExpense(uid, higherId, 'Other burn', '10000.00');

    await refresh(companyId, lowerId, 'BUSINESS_RUNWAY');
    await refresh(companyId, higherId, 'BUSINESS_RUNWAY');

    const viaHigher = lifeCore(await getLife(uid, higherId));
    const viaLower = lifeCore(await getLife(uid, lowerId));
    const viaDaily = lifeCore(await getLife(uid, dailyId));
    assert.deepEqual(viaHigher, viaLower);
    assert.deepEqual(viaDaily, viaLower);

    const signals = viaDaily.signals as Array<{ title: string; signalType: string }>;
    const runwaySignal = signals.find((s) => s.signalType === 'runway');
    assert.ok(runwaySignal, JSON.stringify(signals));
    assert.match(runwaySignal.title, /9 month/);
    assert.equal(runwaySignal.title.includes('24 month'), false);
  });

  it('keeps a real zero and leaves an unrefreshed family empty', async () => {
    const uid = `g3-zero-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@g3.local`);
    const companyId = await createCompany(uid, `Zero ${uid}`);
    const moneyId = await createMoment(uid, companyId, 'BUSINESS_RUNWAY', 'Zero money', {
      availableCash: '0',
      warningThreshold: '6 months',
    });
    await getPool().query(
      `UPDATE business.business_system_setup
       SET preferences = (preferences - 'monthlyRevenue') || '{"availableCash":"0"}'::jsonb
       WHERE moment_id = $1`,
      [moneyId]
    );
    await refresh(companyId, moneyId, 'BUSINESS_RUNWAY');

    const stored = await readStored(companyId);
    assert.equal(stored.runway.availableCash, '0');
    assert.equal(stored.runway.monthlyRevenue, null);
    assert.deepEqual(stored.team, {});
    assert.deepEqual(stored.daily, {});

    const life = await getLife(uid, moneyId);
    const payload = life.payload as Record<string, unknown>;
    const sections = payload.sections as Record<string, string>;
    assert.equal(sections.teamOperations, 'EMPTY_SUPPORTED');
    assert.equal(sections.businessOperations, 'EMPTY_SUPPORTED');
    assert.equal((payload.runwayPayload as Record<string, unknown>).availableCash, '0');
    assert.equal((payload.teamOperationsPayload as Record<string, unknown>).memberCapacity, undefined);
  });
});
