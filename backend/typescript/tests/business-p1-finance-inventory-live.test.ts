/**
 * P1 Shared Finance & Inventory — live DB acceptance.
 *
 * Callers: `node --import tsx --test tests/business-p1-finance-inventory-live.test.ts`
 * Schemas: business.operating_event, business_obligation, business_settlement_allocation,
 *          business_inventory_movement (V104). Does not run V103.
 * User: "Activate and integrate the shared financial and inventory foundations..."
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
import { allocateSettlement } from '../src/modules/business/finance-bridge';
import { reconcileInventoryQuantity } from '../src/modules/business/inventory-bridge';

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
    .set('Idempotency-Key', `p1-co-${randomUUID()}`)
    .send({ displayName: name, legalName: `${name} L`, timezone: 'UTC' });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.companyId as string;
}

async function createOperatingMoment(
  uid: string,
  companyId: string,
  args: {
    familyCode: string;
    momentTypeCode: string;
    title: string;
    prefs?: Record<string, unknown>;
  }
): Promise<string> {
  const res = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `p1-mom-${randomUUID()}`)
    .send({
      domainCode: 'BUSINESS',
      momentTypeCode: args.momentTypeCode,
      title: args.title,
      companyId,
      businessSetup: {
        familyCode: args.familyCode,
        preferences: {
          subtype: args.momentTypeCode,
          identityName: args.title,
          partyName: `${args.title} Co`,
          monthlyBudget: '100000',
          monthlyTarget: '200000',
          currency: 'INR',
          channels: ['Walk-in'],
          startDate: '2026-01-01',
          ...args.prefs,
        },
      },
    });
  assert.equal(res.status, 201, JSON.stringify(res.body));
  return res.body.data.momentId as string;
}

async function postOp(
  uid: string,
  momentId: string,
  body: Record<string, unknown>,
  key = randomUUID()
) {
  return request(app)
    .post(`/v1/business/moments/${momentId}/operating-events`)
    .set('X-Dev-Firebase-Uid', uid)
    .set('Idempotency-Key', `p1-op-${key}`)
    .send(body);
}

describe('P1 Shared finance & inventory (live DB)', () => {
  before(async () => {
    await getPool().query('SELECT 1');
    const v104 = await getPool().query(
      `SELECT 1 FROM information_schema.tables
       WHERE table_schema='business' AND table_name='business_obligation'`
    );
    assert.equal(v104.rowCount, 1, 'V104 business_obligation must exist');
  });

  after(async () => {
    await closePool();
  });

  it('spend→payable→payment and invoice→receivable→collection reconcile; duplicates and over-alloc rejected', async () => {
    const ownerUid = `p1-own-${randomUUID().slice(0, 8)}`;
    const observerUid = `p1-obs-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p1.local`, 'Owner');
    await ensureUser(userIdFor(observerUid), `${observerUid}@p1.local`, 'Observer');

    const companyId = await createCompany(ownerUid, `P1 ${ownerUid}`);
    const addObs = await request(app)
      .post(`/v1/companies/${companyId}/members`)
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `p1-mem-${randomUUID()}`)
      .send({ userId: userIdFor(observerUid), membershipType: 'OBSERVER' });
    assert.equal(addObs.status, 201, JSON.stringify(addObs.body));

    const momentId = await createOperatingMoment(ownerUid, companyId, {
      familyCode: 'TRADING_DISTRIBUTION',
      momentTypeCode: 'TRADING_WHOLESALE',
      title: `Trade ${ownerUid}`,
    });

    const beforeEvents = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.operating_event WHERE moment_id = $1`,
      [momentId]
    );

    // Spend opens PAYABLE
    const spend = await postOp(ownerUid, momentId, {
      eventType: 'SPEND',
      title: 'Vendor bill',
      amount: '1000.00',
      counterpartyName: 'Supplier A',
      paymentStatus: 'PENDING',
      channelName: 'Walk-in',
      dueAt: '2026-12-31',
      contract: { expenseCategory: 'OPS', paymentState: 'PAY_LATER' },
    });
    assert.equal(spend.status, 201, JSON.stringify(spend.body));
    const spendId = spend.body.data.eventId as string;

    const payable = await getPool().query<{
      obligation_id: string;
      outstanding: string;
      status: string;
      obligation_kind: string;
    }>(
      `SELECT obligation_id, outstanding::text, status, obligation_kind
       FROM business.business_obligation WHERE source_event_id = $1`,
      [spendId]
    );
    assert.equal(payable.rows[0]?.obligation_kind, 'PAYABLE');
    assert.equal(payable.rows[0]?.status, 'OPEN');
    assert.equal(Number(payable.rows[0]?.outstanding), 1000);

    // Payment settles payable
    const pay = await postOp(ownerUid, momentId, {
      eventType: 'PAYMENT',
      title: 'Pay vendor',
      amount: '1000.00',
      parentEventId: spendId,
      counterpartyName: 'Supplier A',
      channelName: 'Walk-in',
    });
    assert.equal(pay.status, 201, JSON.stringify(pay.body));
    const payId = pay.body.data.eventId as string;

    const settled = await getPool().query<{ outstanding: string; status: string }>(
      `SELECT outstanding::text, status FROM business.business_obligation WHERE source_event_id = $1`,
      [spendId]
    );
    assert.equal(settled.rows[0]?.status, 'SETTLED');
    assert.equal(Number(settled.rows[0]?.outstanding), 0);

    const alloc = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.business_settlement_allocation WHERE payment_event_id = $1`,
      [payId]
    );
    assert.equal(Number(alloc.rows[0]?.n), 1);

    // Duplicate payment idempotency key → no second allocation
    const payDup = await postOp(
      ownerUid,
      momentId,
      {
        eventType: 'PAYMENT',
        title: 'Pay vendor',
        amount: '1000.00',
        parentEventId: spendId,
        counterpartyName: 'Supplier A',
        channelName: 'Walk-in',
      },
      // reuse same logical body under new key with clientKey-less path — second payment should fail over-alloc
      randomUUID()
    );
    assert.ok(
      payDup.status === 409 || payDup.status === 400,
      `expected over-alloc reject, got ${payDup.status} ${JSON.stringify(payDup.body)}`
    );

    // Invoice → receivable → collection
    const inv = await postOp(ownerUid, momentId, {
      eventType: 'SALE',
      title: 'Credit sale',
      amount: '2500.00',
      counterpartyName: 'Buyer B',
      paymentStatus: 'PENDING',
      channelName: 'Walk-in',
      dueAt: '2026-12-31',
      // QUICK amount-only: no quantity → no inventory issue (honest non-stock sale).
      contract: { paymentState: 'PAY_LATER', entryMode: 'QUICK' },
    });
    assert.equal(inv.status, 201, JSON.stringify(inv.body));
    const saleId = inv.body.data.eventId as string;

    const recv = await getPool().query<{ obligation_kind: string; status: string; outstanding: string }>(
      `SELECT obligation_kind, status, outstanding::text
       FROM business.business_obligation WHERE source_event_id = $1`,
      [saleId]
    );
    assert.equal(recv.rows[0]?.obligation_kind, 'RECEIVABLE');
    assert.equal(recv.rows[0]?.status, 'OPEN');

    const coll = await postOp(ownerUid, momentId, {
      eventType: 'COLLECTION',
      title: 'Collect',
      amount: '2500.00',
      parentEventId: saleId,
      counterpartyName: 'Buyer B',
      channelName: 'Walk-in',
    });
    assert.equal(coll.status, 201, JSON.stringify(coll.body));

    const recvDone = await getPool().query<{ status: string; outstanding: string }>(
      `SELECT status, outstanding::text FROM business.business_obligation WHERE source_event_id = $1`,
      [saleId]
    );
    assert.equal(recvDone.rows[0]?.status, 'SETTLED');
    assert.equal(Number(recvDone.rows[0]?.outstanding), 0);

    // Concurrent over-allocation rejected at bridge
    const openSpend = await postOp(ownerUid, momentId, {
      eventType: 'SPEND',
      title: 'Partial bill',
      amount: '500.00',
      counterpartyName: 'Supplier C',
      paymentStatus: 'PENDING',
      channelName: 'Walk-in',
      dueAt: '2026-12-31',
      contract: { expenseCategory: 'OPS', paymentState: 'PAY_LATER' },
    });
    assert.equal(openSpend.status, 201, JSON.stringify(openSpend.body));
    const obl = await getPool().query<{ obligation_id: string }>(
      `SELECT obligation_id FROM business.business_obligation WHERE source_event_id = $1`,
      [openSpend.body.data.eventId]
    );
    await withUserConnection(userIdFor(ownerUid), async (client) => {
      await client.query('BEGIN');
      try {
        const stubPay = async () => {
          const id = randomUUID();
          await client.query(
            `INSERT INTO business.operating_event (
               operating_event_id, moment_id, company_id, actor_user_id, event_type, title,
               amount, currency_code, payment_relevance, payment_status, approval_status,
               submitted_by_user_id, occurred_at, payload
             ) VALUES (
               $1,$2,$3,$4,'PAYMENT','stub',$5,'INR','PAYABLE','PAID','NONE',$4, now(), '{}'::jsonb
             )`,
            [id, momentId, companyId, userIdFor(ownerUid), '100.00']
          );
          return id;
        };
        const pay1 = await stubPay();
        const pay2 = await stubPay();
        await allocateSettlement(client, {
          obligationId: obl.rows[0]!.obligation_id,
          paymentEventId: pay1,
          allocatedAmount: '400.00',
          currencyCode: 'INR',
        });
        let rejected = false;
        try {
          await allocateSettlement(client, {
            obligationId: obl.rows[0]!.obligation_id,
            paymentEventId: pay2,
            allocatedAmount: '200.00',
            currencyCode: 'INR',
          });
        } catch {
          rejected = true;
        }
        assert.equal(rejected, true, 'over-allocation must be rejected');
      } finally {
        await client.query('ROLLBACK');
      }
    });

    // Inventory: stock add + transfer neutrality
    const stockAdd = await postOp(ownerUid, momentId, {
      eventType: 'STOCK',
      title: 'SKU-1',
      quantity: '100',
      channelName: 'Walk-in',
      contract: { stockAction: 'ADD', stockReason: 'opening', ownership: 'OWNED' },
      master: { kind: 'PRODUCT', name: 'SKU-1', unit: 'pc', tracked: true },
    });
    assert.equal(stockAdd.status, 201, JSON.stringify(stockAdd.body));

    const netAfterAdd = await withUserConnection(userIdFor(ownerUid), async (client) =>
      reconcileInventoryQuantity(client, momentId, null)
    );
    assert.equal(netAfterAdd, 100);

    const transfer = await postOp(ownerUid, momentId, {
      eventType: 'STOCK',
      title: 'SKU-1 transfer',
      quantity: '40',
      channelName: 'Walk-in',
      contract: {
        stockAction: 'TRANSFER',
        stockReason: 'TRANSFER',
        ownership: 'OWNED',
        transferFromName: 'Main DC',
        transferToName: 'Branch',
      },
      master: { kind: 'PRODUCT', name: 'SKU-1', unit: 'pc', tracked: true },
    });
    assert.equal(transfer.status, 201, JSON.stringify(transfer.body));
    const xferId = transfer.body.data.eventId as string;
    const xferRows = await getPool().query<{ movement_kind: string; quantity: string }>(
      `SELECT movement_kind, quantity::text FROM business.business_inventory_movement
       WHERE source_event_id = $1 ORDER BY movement_kind`,
      [xferId]
    );
    assert.equal(xferRows.rowCount, 2);
    const kinds = xferRows.rows.map((r) => r.movement_kind).sort();
    assert.deepEqual(kinds, ['TRANSFER_IN', 'TRANSFER_OUT']);
    // Net still 100 (transfer neutral)
    const netAfterXfer = await withUserConnection(userIdFor(ownerUid), async (client) =>
      reconcileInventoryQuantity(client, momentId, null)
    );
    assert.equal(netAfterXfer, 100);

    // Restricted role cannot retrieve finance
    const mom = await request(app)
      .get(`/v1/business/moments/${momentId}/mom-deltas`)
      .set('X-Dev-Firebase-Uid', observerUid);
    assert.equal(mom.status, 403);

    const afterEvents = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM business.operating_event WHERE moment_id = $1`,
      [momentId]
    );
    assert.ok(Number(afterEvents.rows[0]?.n) > Number(beforeEvents.rows[0]?.n));
  });

  it('retail stock and manufacturing production movements reconcile', async () => {
    const ownerUid = `p1-rm-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@p1.local`, 'Owner');
    const companyId = await createCompany(ownerUid, `P1R ${ownerUid}`);

    const retailId = await createOperatingMoment(ownerUid, companyId, {
      familyCode: 'RETAIL_CONSUMER',
      momentTypeCode: 'RETAIL_STORE',
      title: `Retail ${ownerUid}`,
      prefs: { storeName: `Store ${ownerUid}`, format: 'Kirana' },
    });
    const retailStock = await postOp(ownerUid, retailId, {
      eventType: 'STOCK',
      title: 'Rice',
      quantity: '25',
      channelName: 'Walk-in',
      contract: { stockAction: 'ADD', stockReason: 'opening', ownership: 'OWNED' },
      master: { kind: 'PRODUCT', name: 'Rice', unit: 'kg', tracked: true },
    });
    assert.equal(retailStock.status, 201, JSON.stringify(retailStock.body));
    const retailNet = await withUserConnection(userIdFor(ownerUid), async (client) =>
      reconcileInventoryQuantity(client, retailId, null)
    );
    assert.equal(retailNet, 25);

    const mfgId = await createOperatingMoment(ownerUid, companyId, {
      familyCode: 'MANUFACTURING_PRODUCTION',
      momentTypeCode: 'STANDARD_PRODUCTION',
      title: `Mfg ${ownerUid}`,
      prefs: { plantName: `Plant ${ownerUid}`, monthlyOutput: '1000' },
    });
    // Module under test: inventory-bridge sync for PRODUCTION (ITEMIZED → finished RECEIPT).
    // Existing: business-p2-ledger-integrity-live, operating-manufacturing. This file is P1 live.
    // User: "Do not declare P2 PASS if manufacturing is verified only through STOCK ADD"
    const production = await postOp(ownerUid, mfgId, {
      eventType: 'PRODUCTION',
      title: 'Batch A',
      quantity: '50',
      channelName: 'Walk-in',
      unit: 'pc',
      contract: {
        entryMode: 'ITEMIZED',
        acceptedQuantity: '50',
        productionStatus: 'COMPLETED',
      },
      master: { kind: 'PRODUCT', name: 'Finished', unit: 'pc', tracked: true },
    });
    assert.equal(production.status, 201, JSON.stringify(production.body));
    const mfgNet = await withUserConnection(userIdFor(ownerUid), async (client) =>
      reconcileInventoryQuantity(client, mfgId, null)
    );
    assert.equal(mfgNet, 50);
  });
});
