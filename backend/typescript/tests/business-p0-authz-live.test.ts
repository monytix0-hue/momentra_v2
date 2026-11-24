/**
 * Live DB authorization evidence for Business facet recovery (P0).
 * Not mock-only: hits Postgres via the real Express app.
 *
 * Under test: getBusinessMomentProjection / getBusinessMomentActivity via
 * /v1/business/moments/:id/{pulse,life,memory,finance,activity}.
 * Related existing coverage: business-s4-finance.test.ts (stranger pulse only).
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

describe('P0 Business projection authz (live DB)', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('stranger gets 403 on pulse/life/memory/finance/activity; owner gets contracts; cross-company denied', async () => {
    const ownerUid = `p0-own-${randomUUID().slice(0, 8)}`;
    const strangerUid = `p0-str-${randomUUID().slice(0, 8)}`;
    const otherCoUid = `p0-oco-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p0.local`, 'Owner');
    await ensureUser(userIdFor(strangerUid), `${strangerUid}@p0.local`, 'Stranger');
    await ensureUser(userIdFor(otherCoUid), `${otherCoUid}@p0.local`, 'OtherCo');

    const co = await request(app)
      .post('/v1/companies')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p0-co-${randomUUID()}`)
      .send({
        displayName: `P0 ${ownerUid}`,
        legalName: 'P0 Legal',
        timezone: 'UTC',
      });
    assert.equal(co.status, 201, JSON.stringify(co.body));
    const companyId = co.body.data.companyId as string;

    const mom = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p0-mom-${randomUUID()}`)
      .send({
        domainCode: 'BUSINESS',
        momentTypeCode: 'BUSINESS_RUNWAY',
        title: 'P0 Authz Runway',
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
          },
        },
      });
    assert.equal(mom.status, 201, JSON.stringify(mom.body));
    const momentId = mom.body.data.momentId as string;

    const exp = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p0-exp-${randomUUID()}`)
      .send({
        amount: '12.00',
        currencyCode: 'USD',
        description: 'P0',
        categoryCode: 'PURCHASE',
      });
    assert.equal(exp.status, 201, JSON.stringify(exp.body));

    const facets = ['pulse', 'life', 'memory', 'finance'] as const;
    for (const facet of facets) {
      const owner = await request(app)
        .get(`/v1/business/moments/${momentId}/${facet}`)
        .set('X-Dev-Firebase-Uid', ownerUid);
      assert.equal(owner.status, 200, `${facet} owner ${JSON.stringify(owner.body)}`);
      assert.ok(owner.body.data?.payload != null, `${facet} owner payload`);
      if (facet === 'finance' || facet === 'pulse') {
        assert.notEqual(owner.body.data?.status, 'EMPTY');
      }

      const stranger = await request(app)
        .get(`/v1/business/moments/${momentId}/${facet}`)
        .set('X-Dev-Firebase-Uid', strangerUid);
      assert.equal(stranger.status, 403, `${facet} stranger`);

      const unauth = await request(app).get(`/v1/business/moments/${momentId}/${facet}`);
      assert.ok([401, 403].includes(unauth.status), `${facet} unauth ${unauth.status}`);
    }

    const actOwner = await request(app)
      .get(`/v1/business/moments/${momentId}/activity`)
      .set('X-Dev-Firebase-Uid', ownerUid);
    assert.equal(actOwner.status, 200, JSON.stringify(actOwner.body));

    const actStranger = await request(app)
      .get(`/v1/business/moments/${momentId}/activity`)
      .set('X-Dev-Firebase-Uid', strangerUid);
    assert.equal(actStranger.status, 403);

    const otherCo = await request(app)
      .post('/v1/companies')
      .set('X-Dev-Firebase-Uid', otherCoUid)
      .set('Idempotency-Key', `p0-oco-${randomUUID()}`)
      .send({
        displayName: `Other ${otherCoUid}`,
        legalName: 'Other Legal',
        timezone: 'UTC',
      });
    assert.equal(otherCo.status, 201);
    const cross = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', otherCoUid);
    assert.equal(cross.status, 403, 'cross-company must not read');

    const missing = await request(app)
      .get(`/v1/business/moments/${randomUUID()}/pulse`)
      .set('X-Dev-Firebase-Uid', ownerUid);
    assert.ok([403, 404].includes(missing.status), `missing moment ${missing.status}`);
  });
});
