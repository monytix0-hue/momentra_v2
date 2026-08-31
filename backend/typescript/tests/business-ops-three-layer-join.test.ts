/**
 * Business Operations three-layer join proof — writers → SQL → pulse/activity.
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
    [userId, email, 'Ops Join']
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `ops-co-${randomUUID()}`)
    .send({
      displayName: name,
      legalName: `${name} Legal`,
      timezone: 'UTC',
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createOpsMoment(uid: string, companyId: string): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `ops-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'BUSINESS_OPERATIONS',
      title: 'Ops Join Moment',
      companyId,
      businessSetup: {
        familyCode: 'BUSINESS_OPERATIONS',
        preferences: {
          coreOps: 'Growth & Product',
          scope: 'Company-wide',
          model: 'Centralized',
          cadence: 'Monthly',
          monthlyBudget: '₹35,00,000',
          allocationMethod: 'Category-based',
          monitoringStyle: 'Proactive',
          approvalModel: 'not required',
          approvalAlarm: '₹5,00,000',
        },
      },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

describe('Business Ops three-layer join proof', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('V019/V051 Ops capability maps on BUSINESS_OPERATIONS types', async () => {
    const required = ['EXPENSE_CREATE', 'ISSUE_CREATE', 'SLA_MANAGE', 'VENDOR_MANAGE'];
    const rows = await getPool().query<{ code: string; n: string }>(
      `SELECT c.code, COUNT(*)::text AS n
       FROM core.capability c
       JOIN core.moment_type_capability mtc ON mtc.capability_id = c.capability_id AND mtc.status = 'ACTIVE'
       JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id
       WHERE mt.domain_code = 'BUSINESS' AND mt.status = 'ACTIVE' AND c.status = 'ACTIVE'
         AND (mt.code ILIKE '%OPERATIONS%' OR mt.code = 'BUSINESS_OPERATIONS')
         AND c.code = ANY($1::text[])
       GROUP BY c.code`,
      [required]
    );
    for (const code of required) {
      const row = rows.rows.find((r) => r.code === code);
      assert.ok(row && Number(row.n) > 0, `missing capability map: ${code}`);
    }
  });

  it('spend + vendor + issue + SLA → SQL + ops pulse extras + activity', async () => {
    const uid = `ops-join-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@ops.local`);
    const companyId = await createCompany(uid, `OpsCo ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const spend = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-spend-${randomUUID()}`)
      .send({
        amount: '120.00',
        currencyCode: 'INR',
        description: 'Cloud bill',
        categoryCode: 'PURCHASE',
      });
    assert.equal(spend.status, 201, JSON.stringify(spend.body));

    const vendor = await request(app)
      .post(`/v1/companies/${companyId}/vendors`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-vendor-${randomUUID()}`)
      .send({ name: 'Acme Cloud', vendorType: 'SaaS' });
    assert.equal(vendor.status, 201, JSON.stringify(vendor.body));
    const vendorId = vendor.body.data.vendorId as string;

    const patch = await request(app)
      .patch(`/v1/companies/${companyId}/vendors/${vendorId}`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-vpatch-${randomUUID()}`)
      .send({ note: 'Primary infra vendor' });
    assert.equal(patch.status, 200, JSON.stringify(patch.body));

    const contract = await request(app)
      .post(`/v1/companies/${companyId}/vendors/${vendorId}/contracts`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-contract-${randomUUID()}`)
      .send({ contractName: 'Annual SaaS', contractValue: '12000', currencyCode: 'INR' });
    assert.equal(contract.status, 201, JSON.stringify(contract.body));

    const slaDef = await request(app)
      .post(`/v1/companies/${companyId}/vendors/${vendorId}/sla-definitions`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-sla-${randomUUID()}`)
      .send({
        name: 'Uptime',
        metricCode: 'UPTIME_PCT',
        targetValue: 99.9,
        comparator: 'GTE',
        unitCode: 'PCT',
      });
    assert.equal(slaDef.status, 201, JSON.stringify(slaDef.body));
    const slaDefinitionId = slaDef.body.data.slaDefinitionId as string;

    const slaCheck = await request(app)
      .post(`/v1/companies/${companyId}/sla-definitions/${slaDefinitionId}/checks`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-slac-${randomUUID()}`)
      .send({ result: 'PASS', observedValue: 99.95 });
    assert.equal(slaCheck.status, 201, JSON.stringify(slaCheck.body));

    const issue = await request(app)
      .post(`/v1/moments/${momentId}/issues`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-issue-${randomUUID()}`)
      .send({ title: 'Latency spike', severity: 'HIGH', description: 'p99 over SLO' });
    assert.equal(issue.status, 201, JSON.stringify(issue.body));

    const improvement = await request(app)
      .post(`/v1/moments/${momentId}/improvements`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-imp-${randomUUID()}`)
      .send({ title: 'Cache warm path', categoryCode: 'PERF' });
    assert.equal(improvement.status, 201, JSON.stringify(improvement.body));

    const update = await request(app)
      .post(`/v1/moments/${momentId}/business-updates`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-upd-${randomUUID()}`)
      .send({ title: 'Weekly ops', body: 'Vendors stable; watching latency.' });
    assert.equal(update.status, 201, JSON.stringify(update.body));

    const approval = await request(app)
      .post(`/v1/moments/${momentId}/approval-requests`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `ops-apr-${randomUUID()}`)
      .send({ title: 'Extra seats', amount: '500.00', currencyCode: 'INR' });
    assert.equal(approval.status, 201, JSON.stringify(approval.body));

    const issueSql = await getPool().query(
      `SELECT COUNT(*)::int AS n FROM business.issue WHERE moment_id = $1 AND title = 'Latency spike'`,
      [momentId]
    );
    assert.equal(issueSql.rows[0].n, 1);

    const impSql = await getPool().query(
      `SELECT COUNT(*)::int AS n FROM business.operational_improvement WHERE moment_id = $1`,
      [momentId]
    );
    assert.equal(impSql.rows[0].n, 1);

    const pulse = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    const ops = pulse.body.data.payload?.operations;
    assert.ok(ops, 'operations extras missing');
    assert.equal(ops.sectionQuality?.operationsIntelligence, 'DEFERRED');
    assert.ok(Number(ops.activeVendorCount) >= 1);
    assert.ok(ops.needsAttention?.length >= 1);
    assert.equal(ops.slaCompliancePct, 100);

    const activity = await request(app)
      .get(`/v1/business/moments/${momentId}/activity`)
      .set('X-Dev-Firebase-Uid', uid)
      .query({ limit: 30 });
    assert.equal(activity.status, 200, JSON.stringify(activity.body));
    const items = activity.body.data.items as { title: string; activityCode: string }[];
    assert.ok(
      items.some((i) => i.activityCode === 'ISSUE_REPORTED' || i.title.includes('Latency')),
      JSON.stringify(items)
    );
  });
});
