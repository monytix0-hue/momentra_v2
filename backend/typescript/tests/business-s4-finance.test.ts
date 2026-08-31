/**
 * S4 Business finance + membership + isolation foundations.
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
    .set('Idempotency-Key', `s4-co-${randomUUID()}`)
    .send({
      displayName: name,
      legalName: `${name} Legal`,
      timezone: 'UTC',
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createRunwayMoment(
  uid: string,
  companyId: string,
  prefs?: Record<string, unknown>
): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `s4-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: 'BUSINESS_RUNWAY',
      title: 'Series A Runway',
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

describe('S4 Business foundations', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('posts business expense and updates finance projection', async () => {
    const uid = `s4-u1-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@s4.local`, 'S4 Owner');
    const companyId = await createCompany(uid, `Co ${uid}`);
    const momentId = await createRunwayMoment(uid, companyId);

    const res = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s4-exp-${randomUUID()}`)
      .send({
        amount: '250.00',
        currencyCode: 'INR',
        description: 'AWS',
        categoryCode: 'PURCHASE',
      });
    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.equal(res.body.data.status, 'POSTED');
    assert.equal(res.body.data.categoryCode, 'PURCHASE');
    assert.equal(res.body.data.approvalRequestId, null);

    const snap = await getPool().query<{ expense_total: string }>(
      `SELECT expense_total::text FROM projection.business_finance_snapshot
       WHERE company_id = $1 AND currency_code = 'INR'`,
      [companyId]
    );
    assert.ok(snap.rows[0]);
    assert.equal(Number(snap.rows[0].expense_total), 250);

    const pulse = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(pulse.status, 200, JSON.stringify(pulse.body));
    assert.ok(pulse.body.data.payload?.finance);
  });

  it('threshold expense creates DRAFT + PENDING approval; MEMBER cannot approve; OWNER can', async () => {
    const ownerUid = `s4-own-${randomUUID().slice(0, 8)}`;
    const memberUid = `s4-mem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@s4.local`, 'Owner');
    await ensureUser(userIdFor(memberUid), `${memberUid}@s4.local`, 'Member');

    const companyId = await createCompany(ownerUid, `Thresh ${ownerUid}`);
    const momentId = await createRunwayMoment(ownerUid, companyId);

    // Patch setup prefs for threshold via SQL (team ops style keys on runway also accepted in JSONB)
    await getPool().query(
      `UPDATE business.business_system_setup
       SET preferences = preferences || $2::jsonb
       WHERE company_id = $1 AND moment_id = $3 AND status = 'ACTIVE'`,
      [
        companyId,
        JSON.stringify({
          spendingApproval: 'Required',
          approvalThreshold: '1000',
        }),
        momentId,
      ]
    );

    const add = await request(app)
      .post(`/v1/companies/${companyId}/members`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `s4-add-${randomUUID()}`)
      .send({ userId: userIdFor(memberUid), membershipType: 'MEMBER' });
    assert.equal(add.status, 201, JSON.stringify(add.body));

    const submit = await request(app)
      .post(`/v1/moments/${momentId}/business-expenses`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `s4-thr-${randomUUID()}`)
      .send({
        amount: '5000.00',
        currencyCode: 'INR',
        description: 'Large purchase',
        categoryCode: 'PURCHASE',
      });
    assert.equal(submit.status, 201, JSON.stringify(submit.body));
    assert.equal(submit.body.data.status, 'DRAFT');
    assert.ok(submit.body.data.approvalRequestId);
    const approvalId = submit.body.data.approvalRequestId as string;

    const denied = await request(app)
      .post(`/v1/approvals/${approvalId}/decide`)
      .set('X-Dev-Firebase-Uid', memberUid)
      .set('Idempotency-Key', `s4-deny-${randomUUID()}`)
      .send({ decision: 'APPROVE' });
    assert.equal(denied.status, 403, JSON.stringify(denied.body));

    const approved = await request(app)
      .post(`/v1/approvals/${approvalId}/decide`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `s4-ok-${randomUUID()}`)
      .send({ decision: 'APPROVE' });
    assert.equal(approved.status, 200, JSON.stringify(approved.body));
    assert.equal(approved.body.data.expenseStatus, 'POSTED');

    const exp = await getPool().query<{ status: string }>(
      `SELECT status FROM finance.expense WHERE expense_id = $1`,
      [submit.body.data.expenseId]
    );
    assert.equal(exp.rows[0]?.status, 'POSTED');
  });

  it('revenue + invoice server totals; C1/C2 isolation', async () => {
    const uid = `s4-iso-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uid), `${uid}@s4.local`, 'Iso');
    const c1 = await createCompany(uid, `C1 ${uid}`);
    const c2 = await createCompany(uid, `C2 ${uid}`);
    const m1 = await createRunwayMoment(uid, c1);
    const m2 = await createRunwayMoment(uid, c2);

    await request(app)
      .post(`/v1/moments/${m1}/business-expenses`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s4-c1e-${randomUUID()}`)
      .send({ amount: '10.00', currencyCode: 'USD', description: 'C1 only' });

    const rev = await request(app)
      .post(`/v1/moments/${m1}/revenues`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s4-rev-${randomUUID()}`)
      .send({ amount: '99.00', currencyCode: 'USD', description: 'Sale' });
    assert.equal(rev.status, 201, JSON.stringify(rev.body));

    const inv = await request(app)
      .post(`/v1/moments/${m1}/invoices`)
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `s4-inv-${randomUUID()}`)
      .send({
        invoiceNumber: `INV-${randomUUID().slice(0, 6)}`,
        invoiceDate: '2026-08-26',
        currencyCode: 'USD',
        lines: [
          { description: 'Item', quantity: '2', unitPrice: '10.00', taxAmount: '1.00' },
          { description: 'Item2', quantity: '1', unitPrice: '5.00' },
        ],
      });
    assert.equal(inv.status, 201, JSON.stringify(inv.body));
    assert.equal(inv.body.data.subtotalAmount, '25.0000');
    assert.equal(inv.body.data.taxAmount, '1.0000');
    assert.equal(inv.body.data.totalAmount, '26.0000');

    const c2Pulse = await request(app)
      .get(`/v1/business/moments/${m2}/finance`)
      .set('X-Dev-Firebase-Uid', uid);
    assert.equal(c2Pulse.status, 200);
    const totals = c2Pulse.body.data.payload?.totals ?? [];
    const usd = totals.find((t: { currencyCode: string }) => t.currencyCode === 'USD');
    assert.ok(!usd || Number(usd.expenseTotal) === 0, 'C2 must not see C1 expenses');

    const c1Fin = await request(app)
      .get(`/v1/business/moments/${m1}/finance`)
      .set('X-Dev-Firebase-Uid', uid);
    const c1Totals = c1Fin.body.data.payload?.totals ?? [];
    const c1Usd = c1Totals.find((t: { currencyCode: string }) => t.currencyCode === 'USD');
    assert.ok(c1Usd);
    assert.equal(Number(c1Usd.expenseTotal), 10);
  });

  it('non-member cannot read business moment', async () => {
    const ownerUid = `s4-own2-${randomUUID().slice(0, 8)}`;
    const strangerUid = `s4-str-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@s4.local`, 'Owner2');
    await ensureUser(userIdFor(strangerUid), `${strangerUid}@s4.local`, 'Stranger');
    const companyId = await createCompany(ownerUid, `Priv ${ownerUid}`);
    const momentId = await createRunwayMoment(ownerUid, companyId);

    const res = await request(app)
      .get(`/v1/business/moments/${momentId}/pulse`)
      .set('X-Dev-Firebase-Uid', strangerUid);
    assert.equal(res.status, 403);
  });
});
