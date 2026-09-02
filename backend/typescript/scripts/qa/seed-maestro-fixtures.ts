/**
 * Seed deterministic Maestro QA prerequisites (no finance transactions under test).
 *
 * Guards: NODE_ENV != production && QA_FIXTURES_ENABLED=true
 * Forces ALLOW_DEV_AUTH=1 for X-Dev-Firebase-Uid seeding (after dotenv may clear it).
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/seed-maestro-fixtures.ts
 */
import { readFileSync, existsSync, writeFileSync } from 'fs';
import path from 'path';
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../../src/app';
import { config } from '../../src/platform/config';
import { closePool, getPool, prewarmPool } from '../../src/platform/database/pool';
import { clearKnownUserProfiles } from '../../src/platform/auth';
import { firebaseUserId } from '../../src/platform/auth/uuid';
import {
  assertQaFixturesSafe,
  QA_FIXTURE_ALIASES,
  defaultQaEmail,
  defaultQaPassword,
  type QaFixtureAlias,
  type QaPlatformFixtureAlias,
} from './qa-env-guard';

// Must run after config dotenv (override:true can clear parent env).
process.env.ALLOW_DEV_AUTH = '1';
process.env.RATE_LIMIT_DISABLED = process.env.RATE_LIMIT_DISABLED || '1';
(config as { allowDevAuth: boolean }).allowDevAuth = true;

type Identity = {
  alias: QaFixtureAlias;
  email: string;
  password: string;
  firebaseUid: string;
  userId: string;
};

function loadDotEnvFile(filePath: string): void {
  if (!existsSync(filePath)) return;
  for (const line of readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq <= 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
}

function resolveWebApiKey(): string {
  const fromEnv = (process.env.FIREBASE_WEB_API_KEY || '').trim();
  if (fromEnv) return fromEnv;
  const gs = path.resolve(__dirname, '../../../../apk/app/google-services.json');
  const json = JSON.parse(readFileSync(gs, 'utf8')) as {
    client?: Array<{ api_key?: Array<{ current_key?: string }> }>;
  };
  const key = json.client?.[0]?.api_key?.[0]?.current_key;
  if (!key) throw new Error('FIREBASE_WEB_API_KEY missing');
  return key;
}

async function identityToolkit(
  apiKey: string,
  pathSuffix: string,
  body: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const url = `https://identitytoolkit.googleapis.com/v1/accounts:${pathSuffix}?key=${apiKey}`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const json = (await res.json()) as Record<string, unknown> & {
    error?: { message?: string };
  };
  if (!res.ok) {
    throw new Error(json.error?.message || `Identity Toolkit ${pathSuffix} failed (${res.status})`);
  }
  return json;
}

async function ensureFirebaseUser(
  apiKey: string,
  email: string,
  password: string,
  displayName: string
): Promise<string> {
  try {
    const signedIn = await identityToolkit(apiKey, 'signInWithPassword', {
      email,
      password,
      returnSecureToken: true,
    });
    return String(signedIn.localId);
  } catch (e) {
    const msg = String((e as Error)?.message || e);
    if (!/EMAIL_NOT_FOUND|INVALID_LOGIN_CREDENTIALS|INVALID_PASSWORD/i.test(msg)) throw e;
  }
  try {
    const signedUp = await identityToolkit(apiKey, 'signUp', {
      email,
      password,
      returnSecureToken: true,
      displayName,
    });
    return String(signedUp.localId);
  } catch (e) {
    const msg = String((e as Error)?.message || e);
    if (/EMAIL_EXISTS/i.test(msg)) {
      throw new Error(
        `Firebase user ${email} exists but password does not match. Update .env.maestro.local or reset password.`
      );
    }
    throw e;
  }
}

function authHeaders(firebaseUid: string): Record<string, string> {
  return {
    'X-Dev-Firebase-Uid': firebaseUid,
    'X-Firebase-Project-Id': config.firebase.projectId || 'momentra-v2',
  };
}

async function post(
  app: ReturnType<typeof createApp>,
  pathName: string,
  headers: Record<string, string>,
  body: unknown
) {
  const res = await request(app)
    .post(pathName)
    .set({ ...headers, 'Idempotency-Key': randomUUID() })
    .send(body);
  if (res.status >= 400) {
    throw new Error(`POST ${pathName} → ${res.status} ${JSON.stringify(res.body)}`);
  }
  return res.body;
}

async function firstType(domain: string): Promise<string> {
  const rows = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = $1 AND status = 'ACTIVE' ORDER BY code LIMIT 1`,
    [domain]
  );
  if (!rows.rows[0]) throw new Error(`No moment_type for ${domain}`);
  return rows.rows[0].code;
}

async function main(): Promise<void> {
  const repoRoot = path.resolve(__dirname, '../../../..');
  const envLocal = path.join(repoRoot, '.maestro', '.env.maestro.local');
  loadDotEnvFile(envLocal);
  assertQaFixturesSafe('seed-maestro-fixtures');
  process.env.ALLOW_DEV_AUTH = '1';
  (config as { allowDevAuth: boolean }).allowDevAuth = true;

  clearKnownUserProfiles();
  await prewarmPool();

  const apiKey = resolveWebApiKey();
  const projectId = config.firebase.projectId || 'momentra-v2';
  const defaultPassword = defaultQaPassword();
  const app = createApp();

  const identities = new Map<QaFixtureAlias, Identity>();
  const envUpdates: string[] = [];

  for (const alias of QA_FIXTURE_ALIASES) {
    const email = (process.env[`${alias}_EMAIL`] || defaultQaEmail(alias)).trim().toLowerCase();
    const password = (process.env[`${alias}_PASSWORD`] || defaultPassword).trim();
    const firebaseUid = await ensureFirebaseUser(apiKey, email, password, alias);
    const userId = firebaseUserId(projectId, firebaseUid);

    // Warm profile via /v1/me
    const me = await request(app).get('/v1/me').set(authHeaders(firebaseUid));
    if (me.status >= 400) {
      throw new Error(`/v1/me for ${alias} → ${me.status} ${JSON.stringify(me.body)}`);
    }

    identities.set(alias, { alias, email, password, firebaseUid, userId });
    envUpdates.push(`${alias}_EMAIL=${email}`);
    envUpdates.push(`${alias}_PASSWORD=${password}`);
  }

  const personalType = await firstType('PERSONAL');
  const groupType = await firstType('GROUP');
  // Prefer TRIP if present
  const trip = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'GROUP' AND code = 'TRIP' AND status = 'ACTIVE'`
  );
  const groupMomentType = trip.rows[0]?.code || groupType;
  const bizTypeRow = await getPool().query<{ code: string }>(
    `SELECT code FROM core.moment_type WHERE domain_code = 'BUSINESS' AND code = 'TEAM_OPERATIONS' AND status = 'ACTIVE'
     UNION ALL
     SELECT code FROM core.moment_type WHERE domain_code = 'BUSINESS' AND status = 'ACTIVE' LIMIT 1`
  );
  const businessType = bizTypeRow.rows[0]?.code;
  if (!businessType) throw new Error('No BUSINESS moment type');

  const personal = identities.get('QA_PERSONAL')!;
  const groupOwner = identities.get('QA_GROUP_OWNER')!;
  const groupMember = identities.get('QA_GROUP_MEMBER')!;
  const businessOwner = identities.get('QA_BUSINESS_OWNER')!;
  const businessMember = identities.get('QA_BUSINESS_MEMBER')!;
  const multi = identities.get('QA_MULTI_CONTEXT')!;

  // --- QA_PERSONAL: multiple personal moments (no expenses) ---
  const personalMoments: string[] = [];
  for (const title of ['QA Personal Alpha', 'QA Personal Beta', 'QA Personal Gamma']) {
    const body = await post(app, '/v1/moments', authHeaders(personal.firebaseUid), {
      domainCode: 'PERSONAL',
      momentTypeCode: personalType,
      title,
    });
    personalMoments.push(body?.data?.momentId as string);
  }

  // --- Group A: owner creates moment bound to invite; member redeems; no expenses ---
  const mintA = await post(app, '/v1/group/invites', authHeaders(groupOwner.firebaseUid), {
    title: 'QA Group A',
    momentTypeCode: groupMomentType,
  });
  const inviteCodeA = (mintA?.data?.inviteCode || mintA?.data?.code) as string;
  if (!inviteCodeA) throw new Error('Group A invite missing code');

  const groupAMoment = await post(app, '/v1/moments', authHeaders(groupOwner.firebaseUid), {
    domainCode: 'GROUP',
    momentTypeCode: groupMomentType,
    title: 'QA Group A',
    inviteCode: inviteCodeA,
  });
  const resolvedGroupA = groupAMoment?.data?.momentId as string;
  if (!resolvedGroupA) throw new Error('Group A moment missing');

  await post(app, `/v1/group/invites/${inviteCodeA}/redeem`, authHeaders(groupMember.firebaseUid), {});

  // Group B owned by outsider (for isolation denial)
  const groupOutsider = identities.get('QA_GROUP_OUTSIDER')!;
  const mintB = await post(app, '/v1/group/invites', authHeaders(groupOutsider.firebaseUid), {
    title: 'QA Group B',
    momentTypeCode: groupMomentType,
  });
  const inviteCodeB = (mintB?.data?.inviteCode || mintB?.data?.code) as string;
  await post(app, '/v1/moments', authHeaders(groupOutsider.firebaseUid), {
    domainCode: 'GROUP',
    momentTypeCode: groupMomentType,
    title: 'QA Group B',
    inviteCode: inviteCodeB,
  });

  // --- Business: Company A owner + member; Company B outsider ---
  const companyA = await post(app, '/v1/companies', authHeaders(businessOwner.firebaseUid), {
    displayName: 'QA Company A',
    legalName: 'QA Company A Legal',
  });
  const companyAId = companyA?.data?.companyId as string;
  await post(app, `/v1/companies/${companyAId}/members`, authHeaders(businessOwner.firebaseUid), {
    userId: businessMember.userId,
    membershipType: 'MEMBER',
  });
  const bizMomentA = await post(app, '/v1/moments', authHeaders(businessOwner.firebaseUid), {
    domainCode: 'BUSINESS',
    companyId: companyAId,
    momentTypeCode: businessType,
    title: 'QA Company A Ops',
  });

  const businessOutsider = identities.get('QA_BUSINESS_OUTSIDER')!;
  const companyB = await post(app, '/v1/companies', authHeaders(businessOutsider.firebaseUid), {
    displayName: 'QA Company B',
    legalName: 'QA Company B Legal',
  });
  const companyBId = companyB?.data?.companyId as string;
  await post(app, '/v1/moments', authHeaders(businessOutsider.firebaseUid), {
    domainCode: 'BUSINESS',
    companyId: companyBId,
    momentTypeCode: businessType,
    title: 'QA Company B Ops',
  });

  // --- QA_MULTI_CONTEXT: personal + group + 2 companies ---
  await post(app, '/v1/moments', authHeaders(multi.firebaseUid), {
    domainCode: 'PERSONAL',
    momentTypeCode: personalType,
    title: 'QA Multi Personal',
  });
  const multiMint = await post(app, '/v1/group/invites', authHeaders(multi.firebaseUid), {
    title: 'QA Multi Group',
    momentTypeCode: groupMomentType,
  });
  const multiInviteCode = (multiMint?.data?.inviteCode || multiMint?.data?.code) as string;
  await post(app, '/v1/moments', authHeaders(multi.firebaseUid), {
    domainCode: 'GROUP',
    momentTypeCode: groupMomentType,
    title: 'QA Multi Group',
    inviteCode: multiInviteCode,
  });
  const multiCo1 = await post(app, '/v1/companies', authHeaders(multi.firebaseUid), {
    displayName: 'QA Multi Co 1',
    legalName: 'QA Multi Co 1 Legal',
  });
  const multiCo2 = await post(app, '/v1/companies', authHeaders(multi.firebaseUid), {
    displayName: 'QA Multi Co 2',
    legalName: 'QA Multi Co 2 Legal',
  });
  await post(app, '/v1/moments', authHeaders(multi.firebaseUid), {
    domainCode: 'BUSINESS',
    companyId: multiCo1?.data?.companyId,
    momentTypeCode: businessType,
    title: 'QA Multi Ops 1',
  });
  await post(app, '/v1/moments', authHeaders(multi.firebaseUid), {
    domainCode: 'BUSINESS',
    companyId: multiCo2?.data?.companyId,
    momentTypeCode: businessType,
    title: 'QA Multi Ops 2',
  });

  // Smoke default → multi-context (rich shell) unless user already set MAESTRO_*
  if (!process.env.MAESTRO_EMAIL) {
    envUpdates.push(`MAESTRO_EMAIL=${multi.email}`);
    envUpdates.push(`MAESTRO_PASSWORD=${multi.password}`);
  }
  if (!process.env.MAESTRO_RUN_ID) {
    envUpdates.push(`MAESTRO_RUN_ID=${new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)}`);
  }
  envUpdates.push(`QA_APP_LOCK_PIN=${process.env.QA_APP_LOCK_PIN || '135790'}`);
  envUpdates.push(`QA_GROUP_A_INVITE_CODE=${inviteCodeA}`);
  if (inviteCodeB) envUpdates.push(`QA_GROUP_B_INVITE_CODE=${inviteCodeB}`);
  envUpdates.push(`QA_GROUP_A_MOMENT_ID=${resolvedGroupA}`);
  envUpdates.push(`QA_COMPANY_A_ID=${companyAId}`);
  envUpdates.push(`QA_COMPANY_B_ID=${companyBId}`);
  envUpdates.push(`QA_BUSINESS_A_MOMENT_ID=${bizMomentA?.data?.momentId || ''}`);
  envUpdates.push(`QA_PERSONAL_MOMENT_ID=${personalMoments[0] || ''}`);
  if (multiInviteCode) {
    envUpdates.push(`QA_MULTI_GROUP_INVITE_CODE=${multiInviteCode}`);
  }

  // --- S9-QA-C: platform-isolated accounts (empty inventory; Maestro creates moments) ---
  const platformSummary: Record<string, unknown> = {};
  for (const platform of ['android', 'ios'] as const) {
    const prefix = platform === 'android' ? 'APK' : 'IOS';
    const tag = platform === 'android' ? 'APK' : 'IOS';
    const personalAlias = `QA_${prefix}_PERSONAL` as QaPlatformFixtureAlias;
    const ownerAlias = `QA_${prefix}_GROUP_OWNER` as QaPlatformFixtureAlias;
    const memberAlias = `QA_${prefix}_GROUP_MEMBER` as QaPlatformFixtureAlias;
    const outsiderAlias = `QA_${prefix}_GROUP_OUTSIDER` as QaPlatformFixtureAlias;
    const bizOwnerAlias = `QA_${prefix}_BUSINESS_OWNER` as QaPlatformFixtureAlias;
    const bizMemberAlias = `QA_${prefix}_BUSINESS_MEMBER` as QaPlatformFixtureAlias;
    const bizOutAlias = `QA_${prefix}_BUSINESS_OUTSIDER` as QaPlatformFixtureAlias;

    const pPersonal = identities.get(personalAlias)!;
    const pOwner = identities.get(ownerAlias)!;
    const pMember = identities.get(memberAlias)!;
    const pOut = identities.get(outsiderAlias)!;
    const pBizOwner = identities.get(bizOwnerAlias)!;
    const pBizMember = identities.get(bizMemberAlias)!;
    const pBizOut = identities.get(bizOutAlias)!;

    // Platform accounts intentionally start empty for Personal (Maestro creates P1–P4).
    // Group: mint invite + 3-member-capable group with NO expenses.
    const mint = await post(app, '/v1/group/invites', authHeaders(pOwner.firebaseUid), {
      title: `QA ${tag} Group A`,
      momentTypeCode: groupMomentType,
    });
    const inviteCode = (mint?.data?.inviteCode || mint?.data?.code) as string;
    const groupMoment = await post(app, '/v1/moments', authHeaders(pOwner.firebaseUid), {
      domainCode: 'GROUP',
      momentTypeCode: groupMomentType,
      title: `QA ${tag} Group A`,
      inviteCode,
    });
    const groupMomentId = groupMoment?.data?.momentId as string;
    await post(app, `/v1/group/invites/${inviteCode}/redeem`, authHeaders(pMember.firebaseUid), {});

    const mintOut = await post(app, '/v1/group/invites', authHeaders(pOut.firebaseUid), {
      title: `QA ${tag} Group B`,
      momentTypeCode: groupMomentType,
    });
    const inviteOut = (mintOut?.data?.inviteCode || mintOut?.data?.code) as string;
    await post(app, '/v1/moments', authHeaders(pOut.firebaseUid), {
      domainCode: 'GROUP',
      momentTypeCode: groupMomentType,
      title: `QA ${tag} Group B`,
      inviteCode: inviteOut,
    });

    const company = await post(app, '/v1/companies', authHeaders(pBizOwner.firebaseUid), {
      displayName: `QA ${tag} Company A`,
      legalName: `QA ${tag} Company A Legal`,
    });
    const companyId = company?.data?.companyId as string;
    await post(app, `/v1/companies/${companyId}/members`, authHeaders(pBizOwner.firebaseUid), {
      userId: pBizMember.userId,
      membershipType: 'MEMBER',
    });
    // No business moments seeded — Maestro creates B01–B03 during certification.

    const companyOut = await post(app, '/v1/companies', authHeaders(pBizOut.firebaseUid), {
      displayName: `QA ${tag} Company B`,
      legalName: `QA ${tag} Company B Legal`,
    });

    envUpdates.push(`QA_${prefix}_GROUP_A_INVITE_CODE=${inviteCode}`);
    envUpdates.push(`QA_${prefix}_GROUP_A_MOMENT_ID=${groupMomentId || ''}`);
    envUpdates.push(`QA_${prefix}_GROUP_B_INVITE_CODE=${inviteOut || ''}`);
    envUpdates.push(`QA_${prefix}_COMPANY_A_ID=${companyId || ''}`);
    envUpdates.push(`QA_${prefix}_COMPANY_B_ID=${companyOut?.data?.companyId || ''}`);

    // Warm personal profile only (no moments) — Maestro creates P1–P4 during cert.
    void pPersonal;

    platformSummary[platform] = {
      personalEmail: pPersonal.email,
      groupA: { momentId: groupMomentId, inviteCode },
      groupB: { inviteCode: inviteOut },
      companyAId: companyId,
      companyBId: companyOut?.data?.companyId,
    };
  }

  // Merge into .env.maestro.local without wiping unknown keys
  const existing = existsSync(envLocal) ? readFileSync(envLocal, 'utf8') : '';
  const map = new Map<string, string>();
  for (const line of existing.split(/\r?\n/)) {
    if (!line.trim() || line.trim().startsWith('#')) continue;
    const eq = line.indexOf('=');
    if (eq > 0) map.set(line.slice(0, eq).trim(), line.slice(eq + 1).trim());
  }
  for (const line of envUpdates) {
    const eq = line.indexOf('=');
    map.set(line.slice(0, eq), line.slice(eq + 1));
  }
  const header = `# Generated/updated by seed-maestro-fixtures.ts — do not commit secrets.\n`;
  const body = [...map.entries()].map(([k, v]) => `${k}=${v}`).join('\n') + '\n';
  writeFileSync(envLocal, header + body, 'utf8');

  const summary = {
    ok: true,
    identities: Object.fromEntries(
      [...identities.entries()].map(([alias, id]) => [
        alias,
        { email: id.email, firebaseUid: id.firebaseUid, userId: id.userId },
      ])
    ),
    personalMoments,
    groupA: { momentId: resolvedGroupA, inviteCode: inviteCodeA },
    groupB: { inviteCode: inviteCodeB },
    companyAId,
    companyBId,
    businessMomentA: bizMomentA?.data?.momentId,
    platformIsolated: platformSummary,
    envLocalUpdated: envLocal,
  };
  console.log(JSON.stringify(summary, null, 2));

  const outJson = path.join(repoRoot, '.maestro', 'reports', 'qa_fixtures_last.json');
  try {
    writeFileSync(outJson, JSON.stringify(summary, null, 2));
  } catch {
    /* reports dir may not exist */
  }
}

main()
  .catch((e) => {
    console.error('[seed-maestro-fixtures] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
