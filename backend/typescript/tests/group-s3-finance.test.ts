/**
 * S3 Group finance + membership foundations.
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
import Decimal from 'decimal.js';

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
    [userId, email, 'S3 Group Finance']
  );
}

async function createGroupMomentWithTwoMembers(
  organizerUid: string,
  memberUid: string
): Promise<{ momentId: string; organizerParticipantId: string; memberParticipantId: string }> {
  const mint = await request(app)
    .post('/v1/group/invites')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `s3-mint-${randomUUID()}`)
    .send({ title: 'S3 Finance Trip', momentTypeCode: 'TRIP' });
  assert.equal(mint.status, 201, JSON.stringify(mint.body));

  const created = await request(app)
    .post('/v1/moments')
    .set('X-Dev-Firebase-Uid', organizerUid)
    .set('Idempotency-Key', `s3-moment-${randomUUID()}`)
    .send({
      domainCode: 'GROUP',
      momentTypeCode: 'TRIP',
      title: 'S3 Finance Trip',
      inviteCode: mint.body.data.inviteCode,
    });
  assert.equal(created.status, 201, JSON.stringify(created.body));
  const momentId = created.body.data.momentId as string;

  const redeem = await request(app)
    .post(`/v1/group/invites/${mint.body.data.inviteCode}/redeem`)
    .set('X-Dev-Firebase-Uid', memberUid)
    .set('Idempotency-Key', `s3-redeem-${randomUUID()}`)
    .send({});
  assert.equal(redeem.status, 200, JSON.stringify(redeem.body));
  assert.equal(redeem.body.data.momentId, momentId);

  const parts = await getPool().query<{ participant_id: string; user_id: string }>(
    `SELECT participant_id, user_id FROM collaboration.moment_participant
     WHERE moment_id = $1 AND status = 'ACTIVE'`,
    [momentId]
  );
  const organizerParticipantId = parts.rows.find((r) => r.user_id === userIdFor(organizerUid))!.participant_id;
  const memberParticipantId = parts.rows.find((r) => r.user_id === userIdFor(memberUid))!.participant_id;
  return { momentId, organizerParticipantId, memberParticipantId };
}

describe('S3 Group finance foundations', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('equal-split expense creates shares summing to amount and obligations for non-payer', async () => {
    const orgUid = `s3-org-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-mem-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createGroupMomentWithTwoMembers(
      orgUid,
      memUid
    );

    const amount = '100.00';
    const res = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `s3-exp-${randomUUID()}`)
      .send({
        amount,
        currencyCode: 'INR',
        description: 'Dinner',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
      });

    assert.equal(res.status, 201, JSON.stringify(res.body));
    assert.ok(res.body.data.expenseId);
    assert.equal(res.body.data.shares.length, 2);
    const shareSum = res.body.data.shares.reduce(
      (acc: Decimal, s: { shareAmount: string }) => acc.plus(s.shareAmount),
      new Decimal(0)
    );
    assert.equal(shareSum.toFixed(4), new Decimal(amount).toFixed(4));
    assert.equal(res.body.data.obligations.length, 1);
    assert.equal(res.body.data.obligations[0].participantId, memberParticipantId);
    assert.equal(res.body.data.obligations[0].originalAmount, '50.0000');

    const finance = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(finance.status, 200, JSON.stringify(finance.body));
    assert.equal(finance.body.data.payload.dataQuality, 'OK');
    assert.ok(finance.body.data.payload.positions.length >= 2);
  });

  it('rejects cross-moment participant in split', async () => {
    const orgUid = `s3-x-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-xm-${randomUUID().slice(0, 8)}`;
    const otherUid = `s3-xo-${randomUUID().slice(0, 8)}`;
    const otherMemUid = `s3-xom-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    await ensureUser(userIdFor(otherUid), `${otherUid}@s3.local`);
    await ensureUser(userIdFor(otherMemUid), `${otherMemUid}@s3.local`);

    const a = await createGroupMomentWithTwoMembers(orgUid, memUid);
    const b = await createGroupMomentWithTwoMembers(otherUid, otherMemUid);

    const res = await request(app)
      .post(`/v1/moments/${a.momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `s3-cross-${randomUUID()}`)
      .send({
        amount: '40.00',
        currencyCode: 'INR',
        paidByParticipantId: a.organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: a.organizerParticipantId }, { participantId: b.organizerParticipantId }],
      });
    assert.equal(res.status, 400, JSON.stringify(res.body));
  });

  it('non-member cannot read finance', async () => {
    const orgUid = `s3-nm-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-nmm-${randomUUID().slice(0, 8)}`;
    const strangerUid = `s3-str-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    await ensureUser(userIdFor(strangerUid), `${strangerUid}@s3.local`);
    const { momentId } = await createGroupMomentWithTwoMembers(orgUid, memUid);

    const finance = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', strangerUid);
    assert.equal(finance.status, 403, JSON.stringify(finance.body));
  });

  it('idempotency key replay returns same expense', async () => {
    const orgUid = `s3-id-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-idm-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createGroupMomentWithTwoMembers(
      orgUid,
      memUid
    );
    const key = `s3-idem-${randomUUID()}`;
    const body = {
      amount: '30.00',
      currencyCode: 'INR',
      paidByParticipantId: organizerParticipantId,
      splitStrategy: 'EQUAL' as const,
      splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
    };

    const first = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.equal(first.status, 201, JSON.stringify(first.body));

    const second = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.equal(second.status, 201, JSON.stringify(second.body));
    assert.equal(second.body.data.expenseId, first.body.data.expenseId);
  });

  it('isolation: U2 cannot access moment B expense/finance', async () => {
    const u1 = `s3-u1-${randomUUID().slice(0, 8)}`;
    const u1b = `s3-u1b-${randomUUID().slice(0, 8)}`;
    const u2 = `s3-u2-${randomUUID().slice(0, 8)}`;
    const u2b = `s3-u2b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(u1), `${u1}@s3.local`);
    await ensureUser(userIdFor(u1b), `${u1b}@s3.local`);
    await ensureUser(userIdFor(u2), `${u2}@s3.local`);
    await ensureUser(userIdFor(u2b), `${u2b}@s3.local`);

    const momentA = await createGroupMomentWithTwoMembers(u1, u1b);
    const momentB = await createGroupMomentWithTwoMembers(u2, u2b);

    const exp = await request(app)
      .post(`/v1/moments/${momentB.momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', u2)
      .set('Idempotency-Key', `s3-iso-${randomUUID()}`)
      .send({
        amount: '20.00',
        currencyCode: 'INR',
        paidByParticipantId: momentB.organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [
          { participantId: momentB.organizerParticipantId },
          { participantId: momentB.memberParticipantId },
        ],
      });
    assert.equal(exp.status, 201, JSON.stringify(exp.body));

    const deniedFinance = await request(app)
      .get(`/v1/group/moments/${momentB.momentId}/finance`)
      .set('X-Dev-Firebase-Uid', u1);
    assert.equal(deniedFinance.status, 403);

    const deniedWrite = await request(app)
      .post(`/v1/moments/${momentB.momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', u1)
      .set('Idempotency-Key', `s3-iso-w-${randomUUID()}`)
      .send({
        amount: '5.00',
        currencyCode: 'INR',
        paidByParticipantId: momentB.organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: momentB.organizerParticipantId }],
      });
    assert.ok(deniedWrite.status === 403 || deniedWrite.status === 400, JSON.stringify(deniedWrite.body));

    // Sanity: U1 can still read own moment
    const ok = await request(app)
      .get(`/v1/group/moments/${momentA.momentId}/finance`)
      .set('X-Dev-Firebase-Uid', u1);
    assert.equal(ok.status, 200);
  });

  it('settlement lifecycle: outstanding → write → audit/event/outbox/activity/projection', async () => {
    const orgUid = `s3-set-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-setm-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createGroupMomentWithTwoMembers(
      orgUid,
      memUid
    );

    const expense = await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `s3-set-exp-${randomUUID()}`)
      .send({
        amount: '100.00',
        currencyCode: 'INR',
        description: 'Dinner before settle',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
      });
    assert.equal(expense.status, 201, JSON.stringify(expense.body));

    const financeBefore = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(financeBefore.status, 200, JSON.stringify(financeBefore.body));
    const outstandingBefore = new Decimal(
      financeBefore.body.data.payload.totals?.[0]?.outstandingTotal ?? '0'
    );
    assert.ok(outstandingBefore.gt(0), `expected outstanding > 0, got ${outstandingBefore}`);

    const settleAmount = '50.00';
    const settle = await request(app)
      .post(`/v1/moments/${momentId}/settlements`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `s3-settle-${randomUUID()}`)
      .send({
        payerParticipantId: memberParticipantId,
        payeeParticipantId: organizerParticipantId,
        amount: settleAmount,
        currencyCode: 'INR',
      });
    assert.equal(settle.status, 201, JSON.stringify(settle.body));
    const settlementId = settle.body.data.settlementId as string;
    assert.ok(settlementId);
    assert.ok(Array.isArray(settle.body.projectionHints), 'projection hints required');

    const row = await getPool().query<{ n: string; amount: string }>(
      `SELECT COUNT(*)::text AS n, MAX(amount::text) AS amount
       FROM finance.settlement WHERE settlement_id = $1 AND moment_id = $2 AND status = 'POSTED'`,
      [settlementId, momentId]
    );
    assert.equal(row.rows[0].n, '1');
    assert.equal(new Decimal(row.rows[0].amount).toFixed(4), new Decimal(settleAmount).toFixed(4));

    const snap = await getPool().query<{ outstanding_total: string }>(
      `SELECT outstanding_total::text FROM projection.group_finance_snapshot
       WHERE moment_id = $1 AND currency_code = 'INR'`,
      [momentId]
    );
    const outstandingAfterSnap = new Decimal(snap.rows[0].outstanding_total);
    assert.equal(
      outstandingAfterSnap.toFixed(4),
      outstandingBefore.minus(settleAmount).toFixed(4)
    );

    const audit = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM audit.audit_record
       WHERE action_code = 'SETTLEMENT_RECORD' AND resource_id = $1`,
      [settlementId]
    );
    assert.ok(parseInt(audit.rows[0].n, 10) >= 1);

    const domain = await getPool().query<{ domain_event_id: string }>(
      `SELECT domain_event_id FROM events.domain_event
       WHERE event_name = 'SettlementRecorded' AND aggregate_id = $1`,
      [settlementId]
    );
    assert.equal(domain.rowCount, 1);
    const domainEventId = domain.rows[0].domain_event_id;

    const outbox = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM events.outbox_event WHERE domain_event_id = $1`,
      [domainEventId]
    );
    assert.ok(parseInt(outbox.rows[0].n, 10) >= 1);

    const activity = await request(app)
      .get(`/v1/group/moments/${momentId}/activity`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(activity.status, 200, JSON.stringify(activity.body));
    const items = activity.body.data?.items ?? [];
    const settleActivity = items.find(
      (i: { activityCode?: string }) => i.activityCode === 'GROUP_SETTLEMENT_RECORDED'
    );
    assert.ok(settleActivity, JSON.stringify(activity.body));

    const financeAfter = await request(app)
      .get(`/v1/group/moments/${momentId}/finance`)
      .set('X-Dev-Firebase-Uid', orgUid);
    assert.equal(financeAfter.status, 200, JSON.stringify(financeAfter.body));
    const outstandingAfter = new Decimal(
      financeAfter.body.data.payload.totals?.[0]?.outstandingTotal ?? '0'
    );
    assert.equal(outstandingAfter.toFixed(4), outstandingBefore.minus(settleAmount).toFixed(4));
  });

  it('settlement idempotency key replay yields exactly one settlement effect', async () => {
    const orgUid = `s3-sid-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-sidm-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    const { momentId, organizerParticipantId, memberParticipantId } = await createGroupMomentWithTwoMembers(
      orgUid,
      memUid
    );

    await request(app)
      .post(`/v1/moments/${momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', `s3-sid-exp-${randomUUID()}`)
      .send({
        amount: '80.00',
        currencyCode: 'INR',
        paidByParticipantId: organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: organizerParticipantId }, { participantId: memberParticipantId }],
      })
      .expect(201);

    const key = `s3-settle-idem-${randomUUID()}`;
    const body = {
      payerParticipantId: memberParticipantId,
      payeeParticipantId: organizerParticipantId,
      amount: '40.00',
      currencyCode: 'INR',
    };

    const first = await request(app)
      .post(`/v1/moments/${momentId}/settlements`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.equal(first.status, 201, JSON.stringify(first.body));

    const second = await request(app)
      .post(`/v1/moments/${momentId}/settlements`)
      .set('X-Dev-Firebase-Uid', orgUid)
      .set('Idempotency-Key', key)
      .send(body);
    assert.equal(second.status, 201, JSON.stringify(second.body));
    assert.equal(second.body.data.settlementId, first.body.data.settlementId);

    const count = await getPool().query<{ n: string }>(
      `SELECT COUNT(*)::text AS n FROM finance.settlement WHERE moment_id = $1`,
      [momentId]
    );
    assert.equal(count.rows[0].n, '1');
  });

  it('rejects Moment A participant settlement posted against Moment B (no finance mutation)', async () => {
    const orgUid = `s3-sx-${randomUUID().slice(0, 8)}`;
    const memUid = `s3-sxm-${randomUUID().slice(0, 8)}`;
    const otherUid = `s3-sxo-${randomUUID().slice(0, 8)}`;
    const otherMemUid = `s3-sxom-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(orgUid), `${orgUid}@s3.local`);
    await ensureUser(userIdFor(memUid), `${memUid}@s3.local`);
    await ensureUser(userIdFor(otherUid), `${otherUid}@s3.local`);
    await ensureUser(userIdFor(otherMemUid), `${otherMemUid}@s3.local`);

    const a = await createGroupMomentWithTwoMembers(orgUid, memUid);
    const b = await createGroupMomentWithTwoMembers(otherUid, otherMemUid);

    await request(app)
      .post(`/v1/moments/${b.momentId}/group-expenses`)
      .set('X-Dev-Firebase-Uid', otherUid)
      .set('Idempotency-Key', `s3-sx-exp-${randomUUID()}`)
      .send({
        amount: '60.00',
        currencyCode: 'INR',
        paidByParticipantId: b.organizerParticipantId,
        splitStrategy: 'EQUAL',
        splitInputs: [{ participantId: b.organizerParticipantId }, { participantId: b.memberParticipantId }],
      })
      .expect(201);

    const before = await getPool().query<{ outstanding_total: string }>(
      `SELECT outstanding_total::text FROM projection.group_finance_snapshot
       WHERE moment_id = $1 AND currency_code = 'INR'`,
      [b.momentId]
    );
    const outstandingBefore = before.rows[0].outstanding_total;

    const res = await request(app)
      .post(`/v1/moments/${b.momentId}/settlements`)
      .set('X-Dev-Firebase-Uid', otherUid)
      .set('Idempotency-Key', `s3-sx-settle-${randomUUID()}`)
      .send({
        payerParticipantId: a.memberParticipantId,
        payeeParticipantId: b.organizerParticipantId,
        amount: '10.00',
        currencyCode: 'INR',
      });
    assert.ok(res.status === 400 || res.status === 403, JSON.stringify(res.body));

    const after = await getPool().query<{ outstanding_total: string; n: string }>(
      `SELECT outstanding_total::text,
              (SELECT COUNT(*)::text FROM finance.settlement WHERE moment_id = $1) AS n
       FROM projection.group_finance_snapshot
       WHERE moment_id = $1 AND currency_code = 'INR'`,
      [b.momentId]
    );
    assert.equal(after.rows[0].outstanding_total, outstandingBefore);
    assert.equal(after.rows[0].n, '0');
  });
});
