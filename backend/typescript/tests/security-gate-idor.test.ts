/**
 * Negative isolation and IDOR/BOLA: one identifier swap must not return a protected body.
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
const SECRET = 'idor-secret-note-9f3a';

function userIdFor(uid: string): string {
  return firebaseUserId(projectId, uid);
}

async function ensureUser(userId: string, email: string): Promise<void> {
  await getPool().query(
    `INSERT INTO core.user_profile (user_id, email, display_name, status)
     VALUES ($1, $2, 'IDOR', 'ACTIVE')
     ON CONFLICT (user_id) DO NOTHING`,
    [userId, email]
  );
}

function rejected(status: number): boolean {
  return status === 403 || status === 404;
}

function noSecret(body: unknown): void {
  const text = JSON.stringify(body);
  assert.equal(text.includes(SECRET), false);
  assert.equal(text.includes('signedUrl'), false);
  assert.equal(text.includes('downloadUrl'), false);
}

describe('security gate IDOR', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('rejects a swapped expense, moment, mood, company, memory, attachment, and participant id', async () => {
    const uidA = `idor-a-${randomUUID().slice(0, 8)}`;
    const uidB = `idor-b-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(uidA), `${uidA}@idor.local`);
    await ensureUser(userIdFor(uidB), `${uidB}@idor.local`);

    const types = await getPool().query<{ code: string }>(
      `SELECT code FROM core.moment_type WHERE domain_code = 'PERSONAL' AND status = 'ACTIVE' LIMIT 1`
    );
    assert.ok(types.rows[0], 'Need PERSONAL moment type');
    const typeCode = types.rows[0].code;

    async function personalMoment(uid: string): Promise<string> {
      const created = await request(app)
        .post('/v1/moments')
        .set('X-Dev-Firebase-Uid', uid)
        .set('Idempotency-Key', `idor-m-${randomUUID()}`)
        .send({ domainCode: 'PERSONAL', momentTypeCode: typeCode, title: 'Private' });
      assert.equal(created.status, 201, JSON.stringify(created.body));
      return created.body.data.momentId as string;
    }

    const momentA = await personalMoment(uidA);
    const momentB = await personalMoment(uidB);

    const expenseA = await request(app)
      .post(`/v1/moments/${momentA}/expenses`)
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `idor-e-${randomUUID()}`)
      .send({ amount: '42.00', currencyCode: 'INR', description: SECRET });
    assert.equal(expenseA.status, 201, JSON.stringify(expenseA.body));
    const expenseId = expenseA.body.data.expenseId as string;

    const expenseB = await request(app)
      .post(`/v1/moments/${momentB}/expenses`)
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `idor-eb-${randomUUID()}`)
      .send({ amount: '7.00', currencyCode: 'INR', description: 'owner-b' });
    assert.equal(expenseB.status, 201, JSON.stringify(expenseB.body));
    const expenseIdB = expenseB.body.data.expenseId as string;

    const swappedExpense = await request(app)
      .get(`/v1/moments/${momentB}/expenses/${expenseId}`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(swappedExpense.status), `expenseId swap ${swappedExpense.status}`);
    noSecret(swappedExpense.body);

    const swappedMoment = await request(app)
      .get(`/v1/moments/${momentA}/expenses/${expenseIdB}`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(swappedMoment.status), `momentId swap ${swappedMoment.status}`);
    noSecret(swappedMoment.body);

    const mood = await request(app)
      .get(`/v1/personal/moments/${momentA}/mood-history`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(mood.status), `mood momentId ${mood.status}`);
    noSecret(mood.body);

    const upload = await request(app)
      .post('/v1/media/uploads')
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `idor-up-${randomUUID()}`)
      .send({
        contentType: 'image/jpeg',
        byteSize: 1024,
        scopeType: 'MOMENT',
        scopeId: momentA,
      });
    assert.ok(rejected(upload.status), `attachment scope ${upload.status}`);
    noSecret(upload.body);

    const attachment = await request(app)
      .delete(`/v1/moments/${momentA}/expenses/${expenseId}/attachments/${randomUUID()}`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(attachment.status), `attachmentId ${attachment.status}`);
    noSecret(attachment.body);

    const companyA = await request(app)
      .post('/v1/companies')
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `idor-co-${randomUUID()}`)
      .send({ displayName: 'Secret Co', legalName: 'Secret Co Legal', timezone: 'UTC' });
    assert.equal(companyA.status, 201, JSON.stringify(companyA.body));
    const companyId = companyA.body.data.companyId as string;
    const companyRead = await request(app)
      .get(`/v1/companies/${companyId}`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(companyRead.status), `companyId ${companyRead.status}`);
    noSecret(companyRead.body);
    assert.equal(JSON.stringify(companyRead.body).includes('Secret Co'), false);

    const mint = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `idor-inv-${randomUUID()}`)
      .send({ title: 'IDOR trip', momentTypeCode: 'TRIP' });
    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    const group = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `idor-g-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: SECRET,
        inviteCode: mint.body.data.inviteCode,
      });
    assert.equal(group.status, 201, JSON.stringify(group.body));
    const groupMomentId = group.body.data.momentId as string;

    const memory = await request(app)
      .post(`/v1/moments/${groupMomentId}/memories`)
      .set('X-Dev-Firebase-Uid', uidA)
      .set('Idempotency-Key', `idor-mem-${randomUUID()}`)
      .send({ title: SECRET });
    assert.equal(memory.status, 201, JSON.stringify(memory.body));
    const memoryId = (memory.body.data?.memoryId ?? memory.body.data?.result?.memoryId) as string;
    assert.ok(memoryId, JSON.stringify(memory.body));

    const memorySwap = await request(app)
      .get(`/v1/moments/${momentB}/memories/${memoryId}/media`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(memorySwap.status), `memoryId ${memorySwap.status}`);
    noSecret(memorySwap.body);

    const outsiderMemory = await request(app)
      .get(`/v1/moments/${groupMomentId}/memories/${memoryId}/media`)
      .set('X-Dev-Firebase-Uid', uidB);
    assert.ok(rejected(outsiderMemory.status), `non-member memory ${outsiderMemory.status}`);
    noSecret(outsiderMemory.body);

    const participants = await request(app)
      .get(`/v1/group/moments/${groupMomentId}/participants`)
      .set('X-Dev-Firebase-Uid', uidA);
    assert.equal(participants.status, 200, JSON.stringify(participants.body));
    const participantId = participants.body.data.participants[0].participantId as string;

    const mintB = await request(app)
      .post('/v1/group/invites')
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `idor-invb-${randomUUID()}`)
      .send({ title: 'IDOR trip B', momentTypeCode: 'TRIP' });
    assert.equal(mintB.status, 201, JSON.stringify(mintB.body));
    const groupB = await request(app)
      .post('/v1/moments')
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `idor-gb-${randomUUID()}`)
      .send({
        domainCode: 'GROUP',
        momentTypeCode: 'TRIP',
        title: 'Caller group',
        inviteCode: mintB.body.data.inviteCode,
      });
    assert.equal(groupB.status, 201, JSON.stringify(groupB.body));

    const participantSwap = await request(app)
      .patch(`/v1/group/moments/${groupB.body.data.momentId}/participants/${participantId}`)
      .set('X-Dev-Firebase-Uid', uidB)
      .set('Idempotency-Key', `idor-role-${randomUUID()}`)
      .send({ roleCode: 'PARTICIPANT' });
    assert.ok(rejected(participantSwap.status), `participantId ${participantSwap.status}`);
    noSecret(participantSwap.body);
  });
});
