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
    [userId, email, 'Company Invite Test']
  );
}

describe('Company invites (Group parity)', () => {
  before(async () => {
    await getPool().query('SELECT 1');
    const table = await getPool().query<{ ok: boolean }>(
      `SELECT EXISTS (
         SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'business' AND table_name = 'company_invite'
       ) AS ok`
    );
    if (!table.rows[0]?.ok) {
      throw new Error('Run business migrations including V052__company_invite.sql before company invite tests.');
    }
  });

  after(async () => {
    await closePool();
  });

  it('mints invite with /j/ URL and role, redeems idempotently', async () => {
    const ownerUid = `co-own-${randomUUID().slice(0, 8)}`;
    const joinerUid = `co-join-${randomUUID().slice(0, 8)}`;
    await ensureUser(userIdFor(ownerUid), `${ownerUid}@invite.local`);
    await ensureUser(userIdFor(joinerUid), `${joinerUid}@invite.local`);

    const company = await request(app)
      .post('/v1/companies')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `co-${randomUUID()}`)
      .send({ displayName: `Invite Co ${randomUUID().slice(0, 6)}`, legalName: 'Invite Co Legal' });

    if (company.status === 501) return;
    assert.equal(company.status, 201, JSON.stringify(company.body));
    const companyId = company.body.data.companyId ?? company.body.data.company_id;
    assert.ok(companyId);

    const mint = await request(app)
      .post('/v1/company/invites')
      .set('X-Dev-Firebase-Uid', ownerUid)
      .set('Idempotency-Key', `mint-${randomUUID()}`)
      .send({ companyId, membershipType: 'MEMBER', operatingRole: 'FINANCE' });

    assert.equal(mint.status, 201, JSON.stringify(mint.body));
    assert.match(mint.body.data.inviteCode, /^[a-hj-np-z2-9]{8}$/);
    assert.equal(mint.body.data.invitePath, `momentra.tech/j/${mint.body.data.inviteCode}`);
    assert.equal(mint.body.data.inviteUrl, `https://momentra.tech/j/${mint.body.data.inviteCode}`);
    assert.equal(mint.body.data.operatingRole, 'FINANCE');

    const preview = await request(app)
      .get(`/v1/company/invites/${mint.body.data.inviteCode}`)
      .set('X-Dev-Firebase-Uid', joinerUid);
    assert.equal(preview.status, 200, JSON.stringify(preview.body));

    const redeem = await request(app)
      .post(`/v1/company/invites/${mint.body.data.inviteCode}/redeem`)
      .set('X-Dev-Firebase-Uid', joinerUid)
      .set('Idempotency-Key', `redeem-${randomUUID()}`)
      .send({});
    assert.ok([200, 201].includes(redeem.status), JSON.stringify(redeem.body));
    assert.equal(redeem.body.data.alreadyMember, false);

    const redeem2 = await request(app)
      .post(`/v1/company/invites/${mint.body.data.inviteCode}/redeem`)
      .set('X-Dev-Firebase-Uid', joinerUid)
      .set('Idempotency-Key', `redeem2-${randomUUID()}`)
      .send({});
    assert.ok([200, 201].includes(redeem2.status), JSON.stringify(redeem2.body));
    assert.equal(redeem2.body.data.alreadyMember, true);
  });
});
