/**
 * P0.1/P0.2 live DB authorization hardening.
 *
 * Behaviour: company OWNER/ADMIN company-wide access; RESTRICTED Moments require
 * Moment membership/grant; finance redacted for TEAM_MEMBER/OBSERVER; 401 without
 * Bearer when ALLOW_DEV_AUTH is off.
 * Modules: access-policy.ts, projection/service.ts, business-reads.ts, auth middleware.
 * Existing: business-p0-authz-live.test.ts, business-s4-finance.test.ts.
 * User: "Define and enforce the distinction between company membership, Moment
 * membership, and explicit Moment access grants... Enforce finance visibility
 * server-side... Verify unauthenticated requests using production-equivalent
 * authentication settings..."
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

async function ensureUser(userId: string, email: string, displayName: string): Promise<void> {
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
    .set('Idempotency-Key', `p01-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} Legal`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createRunway(
  uid: string,
  companyId: string,
  prefs: Record<string, unknown> = {}
): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `p01-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'BUSINESS_RUNWAY',
      title: 'P01 Runway',
      companyId,
      businessSetup: {
        familyCode: 'BUSINESS_RUNWAY',
        preferences: {
          businessStage: 'Scaling',
          goalHorizon: '18-months goal',
          multiCurrency: false,
          availableCash: '100000',
          monthlySpending: '10000',
          revenueStage: 'Growing',
          monthlyRevenue: '8000',
          revenueModel: 'Recurring',
          warningThreshold: '6 months',
          fundingSource: 'Bootstrapped',
          ...prefs,
        },
      },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function addMember(
  ownerUid: string,
  companyId: string,
  memberUserId: string,
  membershipType: string
): Promise<void> {
  const res = await request(app)
    .post(`/v1/companies/${companyId}/members`)
    .set('X-Dev-Firebase-Uid', ownerUid)
    .set('Idempotency-Key', `p01-mem-${randomUUID()}`)
    .send({ userId: memberUserId, membershipType });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

describe('P0.1/P0.2 Business authz hardening (live DB)', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('enforces company-wide vs Moment grant vs stranger/cross-company; redacts finance for restricted roles', async () => {
    const ownerUid = `p01-own-${randomUUID().slice(0, 8)}`;
    const memberUid = `p01-mem-${randomUUID().slice(0, 8)}`;
    const observerUid = `p01-obs-${randomUUID().slice(0, 8)}`;
    const strangerUid = `p01-str-${randomUUID().slice(0, 8)}`;
    const otherUid = `p01-oco-${randomUUID().slice(0, 8)}`;

    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p01.local`, 'Owner');
    await ensureUser(userIdFor(memberUid), `${memberUid}@p01.local`, 'Member');
    await ensureUser(userIdFor(observerUid), `${observerUid}@p01.local`, 'Observer');
    await ensureUser(userIdFor(strangerUid), `${strangerUid}@p01.local`, 'Stranger');
    await ensureUser(userIdFor(otherUid), `${otherUid}@p01.local`, 'Other');

    const companyId = await createCompany(ownerUid, `P01 ${ownerUid}`);
    await addMember(ownerUid, companyId, userIdFor(memberUid), 'MEMBER');
    await addMember(ownerUid, companyId, userIdFor(observerUid), 'OBSERVER');

    const openMomentId = await createRunway(ownerUid, companyId, { momentAccess: 'COMPANY' });
    const openPulse = await request(app)
      .get(`/v1/business/moments/${openMomentId}/pulse`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(openPulse.status, 200, JSON.stringify(openPulse.body));

    const restrictedId = await createRunway(ownerUid, companyId, { momentAccess: 'RESTRICTED' });
    const exp = await request(app)
      .post(`/v1/moments/${restrictedId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p01-exp-${randomUUID()}`)
      .send({
        amount: '40.00',
        currencyCode: 'USD',
        description: 'Restricted spend',
        categoryCode: 'PURCHASE',
      });
    assert.equal(exp.status, 201, JSON.stringify(exp.body));

    const ownerPulse = await request(app)
      .get(`/v1/business/moments/${restrictedId}/pulse`)
      .set('X-Dev-Firebase-Uid', ownerUid);
    assert.equal(ownerPulse.status, 200);
    assert.equal(ownerPulse.body.data.financeVisible, true);
    assert.ok(ownerPulse.body.data.payload?.finance?.totals?.length > 0);

    const deniedMember = await request(app)
      .get(`/v1/business/moments/${restrictedId}/pulse`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(deniedMember.status, 403, 'MEMBER without Moment grant must be denied on RESTRICTED');

    await getPool().query(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)`,
      [restrictedId, userIdFor(memberUid)]
    );

    const grantedMember = await request(app)
      .get(`/v1/business/moments/${restrictedId}/pulse`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(grantedMember.status, 200, JSON.stringify(grantedMember.body));
    assert.equal(grantedMember.body.data.financeVisible, false);
    assert.deepEqual(grantedMember.body.data.payload?.finance?.totals ?? [], []);
    assert.equal(grantedMember.body.data.payload?.finance?.dataQuality, 'RESTRICTED');
    assert.equal(grantedMember.body.data.payload?.runwayMonths, null);

    const grantedFinance = await request(app)
      .get(`/v1/business/moments/${restrictedId}/finance`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(grantedFinance.status, 200);
    assert.deepEqual(grantedFinance.body.data.payload?.totals ?? [], []);

    const expenseList = await request(app)
      .get(`/v1/business/moments/${restrictedId}/expenses`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(expenseList.status, 403, 'restricted role must not list expenses');

    const obsPulse = await request(app)
      .get(`/v1/business/moments/${openMomentId}/pulse`)
      .set('X-Dev-Firebase-Uid', observerUid);
    assert.equal(obsPulse.status, 200);
    assert.equal(obsPulse.body.data.financeVisible, false);

    for (const facet of ['pulse', 'life', 'memory', 'finance', 'activity'] as const) {
      const s = await request(app)
        .get(`/v1/business/moments/${restrictedId}/${facet}`)
        .set('X-Dev-Firebase-Uid', strangerUid);
      assert.equal(s.status, 403, `stranger ${facet}`);
    }

    await createCompany(otherUid, `Other ${otherUid}`);
    const cross = await request(app)
      .get(`/v1/business/moments/${restrictedId}/pulse`)
      .set('X-Dev-Firebase-Uid', otherUid);
    assert.equal(cross.status, 403);
  });

  it('returns 401 without credentials under production-equivalent auth', async () => {
    const prevAllow = process.env.ALLOW_DEV_AUTH;
    const prevConfigAllow = config.allowDevAuth;
    process.env.ALLOW_DEV_AUTH = '0';
    Object.assign(config, { allowDevAuth: false });

    try {
      const prodApp = createApp();
      const res = await request(prodApp).get(`/v1/business/moments/${randomUUID()}/pulse`);
      assert.equal(res.status, 401, JSON.stringify(res.body));
    } finally {
      if (prevAllow === undefined) delete process.env.ALLOW_DEV_AUTH;
      else process.env.ALLOW_DEV_AUTH = prevAllow;
      Object.assign(config, { allowDevAuth: prevConfigAllow });
    }
  });
});
