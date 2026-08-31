/**
 * Company Business Life facet — enriched company-scoped dashboard payload.
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
    [userId, email, 'Company Life']
  );
}

async function createCompany(uid: string, name: string): Promise<string> {
  const res = await request(app)
    .post('/v1/companies')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `life-co-${randomUUID()}`)
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
    .set('Idempotency-Key', `life-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'BUSINESS_OPERATIONS',
      title: 'Company Life Ops',
      companyId,
      businessSetup: {
        familyCode: 'BUSINESS_OPERATIONS',
        preferences: {
          coreOps: 'Growth',
          scope: 'Company-wide',
          model: 'Centralized',
          cadence: 'Monthly',
          monthlyBudget: '100000',
          monitoringStyle: 'Proactive',
        },
      },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

describe('Company Business Life facet', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('GET /life returns company kpis, typed signals, journey, trends, module scores', async () => {
    const uid = `life-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@life.local`);
    const companyId = await createCompany(uid, `LifeCo ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const issue = await request(app)
      .post(`/v1/moments/${momentId}/issues`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `life-issue-${randomUUID()}`)
      .send({ title: 'Invoice overdue', severity: 'HIGH', description: '30+ days' });
    assert.equal(issue.status, 201, JSON.stringify(issue.body));

    const life = await request(app)
      .get(`/v1/business/moments/${momentId}/life`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(life.status, 200, JSON.stringify(life.body));

    const payload = life.body.data.payload;
    assert.ok(payload, 'payload missing');
    assert.ok(payload.kpis, 'kpis missing');
    assert.ok(typeof payload.kpis.activeModuleCount === 'number');
    assert.ok(payload.kpis.activeModuleCount >= 1);
    assert.ok(payload.trends, 'trends missing');
    assert.ok(Array.isArray(payload.trends.series), 'trends.series missing');
    assert.notEqual(payload.sections?.healthTrends, 'DEFERRED');
    assert.notEqual(payload.sections?.vendorOperations, 'DEFERRED');
    assert.ok(Array.isArray(payload.signals));
    assert.ok(
      payload.signals.some(
        (s: { title: string; statusLabel: string; signalType?: string }) =>
          s.title.includes('Invoice') && s.statusLabel === 'Action' && s.signalType === 'issue'
      ),
      JSON.stringify(payload.signals)
    );
    assert.ok(Array.isArray(payload.journey));
    assert.ok(
      payload.journey.some((j: { familyCode: string }) => j.familyCode === 'BUSINESS_OPERATIONS'),
      JSON.stringify(payload.journey)
    );
    assert.ok(payload.modules?.businessOperations?.active === true);
    assert.ok(payload.modules?.vendorOperations != null, 'vendorOperations module missing');
    assert.ok(payload.vendorOperationsPayload != null, 'vendorOperationsPayload missing');
  });

  it('POST /share-link returns token and expiry', async () => {
    const uid = `life-share-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@life.local`);
    const companyId = await createCompany(uid, `LifeShare ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const share = await request(app)
      .post(`/v1/business/moments/${momentId}/share-link`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `life-share-${randomUUID()}`)
      .send({});
    assert.equal(share.status, 201, JSON.stringify(share.body));
    assert.ok(share.body.data.shareToken, JSON.stringify(share.body));
    assert.ok(share.body.data.expiresAt, JSON.stringify(share.body));
    assert.ok(String(share.body.data.shareUrl).includes('token='), JSON.stringify(share.body));
  });

  it('GET /weekly-report accepts period query', async () => {
    const uid = `life-report-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@life.local`);
    const companyId = await createCompany(uid, `LifeReport ${uid}`);
    const momentId = await createOpsMoment(uid, companyId);

    const report = await request(app)
      .get(`/v1/business/moments/${momentId}/weekly-report?period=7d`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(report.status, 200, JSON.stringify(report.body));
    assert.equal(report.body.data.period, '7d');
    assert.ok(Array.isArray(report.body.data.sections));
  });
});
