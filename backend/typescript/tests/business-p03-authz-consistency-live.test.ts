/**
 * P0.3 Authorization Consistency Gate — live DB.
 *
 * Behaviour: authorize() delegates to access-policy; RESTRICTED Moments block
 * unauthorized company MEMBER writes (expense/issue/governance); finance hidden
 * on alternate endpoints; invite roles match V102 CHECK without escalation;
 * denied writes leave no domain_event/outbox side effects.
 * Modules: access-policy, governance/resolver, business-finance, invites.
 * Existing: business-p01-authz-hardening-live.test.ts, company-invite.test.ts.
 * User: "Ensure every Business API and command uses one authoritative
 * company/Moment access policy... Reconcile invitation role persistence with
 * the V102 database CHECK... Unauthorized requests produce no events..."
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
import { withUserConnection } from '../src/platform/database/pool';
import { authorize } from '../src/modules/governance/resolver';
import { evaluateCompanyMomentAccess } from '../src/modules/business/access-policy';

const app = createApp();
const projectId = config.firebase.projectId || 'momentra-dev';

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string, displayName: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, $3, 'ACTIVE') ON CONFLICT (user_id) DO NOTHING`,
    [userId, email, displayName]
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `p03-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} L`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createRestrictedRunway(uid: string, companyId: string): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `p03-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'BUSINESS_RUNWAY',
      title: 'P03 Restricted',
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
          momentAccess: 'RESTRICTED',
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
    .set('Idempotency-Key', `p03-mem-${randomUUID()}`)
    .send({ userId: memberUserId, membershipType });
  assert.equal(res.status, 201, JSON.stringify(res.body));
}

async function countSideEffects(momentId: string): Promise<{
  events: number;
  outbox: number;
  expenses: number;
  issues: number;
}> {
  const [events, outbox, expenses, issues] = await Promise.all([
    getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.domain_event
       WHERE scope_id = $1::uuid
          OR (payload->>'momentId') = $1::text
          OR aggregate_id = $1::uuid`,
      [momentId]
    ),
    getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n
       FROM events.outbox_event o
       JOIN events.domain_event d ON d.domain_event_id = o.domain_event_id
       WHERE d.scope_id = $1::uuid
          OR (d.payload->>'momentId') = $1::text
          OR d.aggregate_id = $1::uuid`,
      [momentId]
    ),
    getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.business_expense_context WHERE moment_id = $1::uuid`,
      [momentId]
    ),
    getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.issue WHERE moment_id = $1::uuid`,
      [momentId]
    ),
  ]);
  return {
    events: Number(events.rows[0]?.n ?? 0),
    outbox: Number(outbox.rows[0]?.n ?? 0),
    expenses: Number(expenses.rows[0]?.n ?? 0),
    issues: Number(issues.rows[0]?.n ?? 0),
  };
}

describe('P0.3 Business authz consistency (live DB)', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('MEMBER cannot write RESTRICTED Moment; authorize matches evaluate; no side effects on deny', async () => {
    const ownerUid = `p03-own-${randomUUID().slice(0, 8)}`;
    const memberUid = `p03-mem-${randomUUID().slice(0, 8)}`;
    const observerUid = `p03-obs-${randomUUID().slice(0, 8)}`;
    const otherUid = `p03-oco-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p03.local`, 'Owner');
    await ensureUser(userIdFor(memberUid), `${memberUid}@p03.local`, 'Member');
    await ensureUser(userIdFor(observerUid), `${observerUid}@p03.local`, 'Observer');
    await ensureUser(userIdFor(otherUid), `${otherUid}@p03.local`, 'Other');

    const companyId = await createCompany(ownerUid, `P03 ${ownerUid}`);
    await addMember(ownerUid, companyId, userIdFor(memberUid), 'MEMBER');
    await addMember(ownerUid, companyId, userIdFor(observerUid), 'OBSERVER');
    const momentId = await createRestrictedRunway(ownerUid, companyId);

    const before = await countSideEffects(momentId);

    const expenseDeny = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `p03-exp-${randomUUID()}`)
      .send({ amount: '5.00', currencyCode: 'USD', description: 'Denied', categoryCode: 'PURCHASE' });
    assert.equal(expenseDeny.status, 403, JSON.stringify(expenseDeny.body));

    const issueDeny = await request(app)
      .post(`/v1/moments/${momentId}/issues`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `p03-iss-${randomUUID()}`)
      .send({ title: 'Denied issue', severity: 'LOW' });
    assert.equal(issueDeny.status, 403, JSON.stringify(issueDeny.body));

    const afterDeny = await countSideEffects(momentId);
    assert.deepEqual(afterDeny, before, 'denied writes must not create events/postings');

    // authorize() agrees with evaluateCompanyMomentAccess
    await withUserConnection(userIdFor(memberUid), async (client) => {
      const ctx = {
        userId: userIdFor(memberUid),
        firebaseUid: memberUid,
        firebaseProjectId: projectId,
        correlationId: 'p03-authz',
        roles: [] as string[],
        permissions: [] as string[],
      };
      const scope = await evaluateCompanyMomentAccess(client, ctx, momentId);
      assert.equal(scope, null);
      const decision = await authorize(client, ctx, {
        actionCode: 'ISSUE_CREATE',
        resourceType: 'ISSUE',
        momentId,
        companyId,
      });
      assert.equal(decision.allowed, false);
    });

    // OWNER can write
    const ownerExp = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p03-oexp-${randomUUID()}`)
      .send({ amount: '9.00', currencyCode: 'USD', description: 'Owner ok', categoryCode: 'PURCHASE' });
    assert.equal(ownerExp.status, 201, JSON.stringify(ownerExp.body));

    // Grant Moment membership → MEMBER can post non-finance issue; still no finance list
    await getPool().query(
      `INSERT INTO collaboration.moment_participant (
         moment_id, user_id, participant_role, status, joined_at, version
       ) VALUES ($1, $2, 'PARTICIPANT', 'ACTIVE', now(), 1)`,
      [momentId, userIdFor(memberUid)]
    );

    const issueOk = await request(app)
      .post(`/v1/moments/${momentId}/issues`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `p03-issok-${randomUUID()}`)
      .send({ title: 'Granted issue', severity: 'LOW' });
    assert.equal(issueOk.status, 201, JSON.stringify(issueOk.body));

    const momDeny = await request(app)
      .get(`/v1/business/moments/${momentId}/mom-deltas`)
      .set('X-Dev-Firebase-Uid', memberUid);
    assert.equal(momDeny.status, 403, 'TEAM_MEMBER must not see MoM finance deltas');

    const expList = await request(app)
      .get(`/v1/business/moments/${momentId}/expenses`)
      .set('X-Dev-Firebase-Uid', observerUid);
    assert.equal(expList.status, 403);

    // Cross-company write denied, no new events for foreign actor path
    await createCompany(otherUid, `Other ${otherUid}`);
    const beforeCross = await countSideEffects(momentId);
    const cross = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', otherUid)
      .set('Idempotency-Key', `p03-x-${randomUUID()}`)
      .send({ amount: '1.00', currencyCode: 'USD', description: 'x', categoryCode: 'PURCHASE' });
    assert.equal(cross.status, 403);
    const afterCross = await countSideEffects(momentId);
    assert.deepEqual(afterCross, beforeCross);
  });

  it('invite rejects non-V102 operatingRole; FINANCE redeem persists without escalation', async () => {
    const ownerUid = `p03-inv-${randomUUID().slice(0, 8)}`;
    const joinerUid = `p03-join-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p03.local`, 'Owner');
    await ensureUser(userIdFor(joinerUid), `${joinerUid}@p03.local`, 'Joiner');
    const companyId = await createCompany(ownerUid, `Inv ${ownerUid}`);

    const bad = await request(app)
      .post('/v1/company/invites')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p03-bad-${randomUUID()}`)
      .send({ companyId, membershipType: 'MEMBER', operatingRole: 'OWNER_ADMIN' });
    assert.ok(bad.status === 400 || bad.status === 422, `expected validation reject, got ${bad.status}`);

    const mint = await request(app)
      .post('/v1/company/invites')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p03-good-${randomUUID()}`)
      .send({ companyId, membershipType: 'MEMBER', operatingRole: 'FINANCE' });
    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    assert.equal(mint.body.data.operatingRole, 'FINANCE');
    const code = mint.body.data.inviteCode as string;

    const redeem = await request(app)
      .post(`/v1/company/invites/${code}/redeem`)
      .set('X-Dev-Firebase-Uid', joinerUid)
      .set('Idempotency-Key', `p03-red-${randomUUID()}`)
      .send({});
    assert.ok([200, 201].includes(redeem.status), JSON.stringify(redeem.body));
    assert.equal(redeem.body.data.membershipType, 'MEMBER');
    assert.equal(redeem.body.data.operatingRole, 'FINANCE');

    const row = await getPool().query<{ membership_type: string; operating_role: string | null }>(
      `SELECT membership_type, operating_role FROM business.company_membership
       WHERE company_id = $1 AND user_id = $2 AND status = 'ACTIVE'`,
      [companyId, userIdFor(joinerUid)]
    );
    assert.equal(row.rows[0]?.membership_type, 'MEMBER');
    assert.equal(row.rows[0]?.operating_role, 'FINANCE');
    assert.notEqual(row.rows[0]?.membership_type, 'OWNER');
    assert.notEqual(row.rows[0]?.membership_type, 'ADMIN');
  });
});
