/**
 * Ensure a Maestro/QA email+password user exists in Firebase Auth and
 * has a matching core.user_profile row in Postgres.
 *
 * Prefer Firebase Admin (FIREBASE_SERVICE_ACCOUNT_JSON). If unset, falls back
 * to Identity Toolkit REST with FIREBASE_WEB_API_KEY (client API key).
 *
 * Usage:
 *   npx tsx scripts/ensure-maestro-user.ts
 *   npx tsx scripts/ensure-maestro-user.ts --email a@b.com --password secret
 *   npm run qa:ensure-maestro-user
 *
 * Loads .maestro/.env.maestro.local for MAESTRO_EMAIL / MAESTRO_PASSWORD when unset.
 */
import { readFileSync, existsSync } from 'fs';
import path from 'path';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { config } from '../src/platform/config';
import { closePool } from '../src/platform/database/pool';
import { ensureUserProfile, provisionUserProfile } from '../src/platform/auth';
import { firebaseUserId } from '../src/platform/auth/uuid';

type Args = {
  email: string;
  password: string;
  displayName?: string;
  resetPassword: boolean;
};

function loadDotEnvFile(filePath: string): void {
  if (!existsSync(filePath)) return;
  const text = readFileSync(filePath, 'utf8');
  for (const line of text.split(/\r?\n/)) {
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

function parseArgs(argv: string[]): Partial<Args> {
  const out: Partial<Args> = { resetPassword: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--email') out.email = argv[++i];
    else if (a === '--password') out.password = argv[++i];
    else if (a === '--display-name') out.displayName = argv[++i];
    else if (a === '--reset-password') out.resetPassword = true;
  }
  return out;
}

function resolveWebApiKey(): string | undefined {
  const fromEnv = (process.env.FIREBASE_WEB_API_KEY || '').trim();
  if (fromEnv) return fromEnv;
  const gs = path.resolve(__dirname, '../../../apk/app/google-services.json');
  if (!existsSync(gs)) return undefined;
  try {
    const json = JSON.parse(readFileSync(gs, 'utf8')) as {
      client?: Array<{ api_key?: Array<{ current_key?: string }> }>;
    };
    return json.client?.[0]?.api_key?.[0]?.current_key;
  } catch {
    return undefined;
  }
}

function initAdmin(): boolean {
  if (getApps().length > 0) return true;
  if (config.firebase.credentialsJson) {
    const cred = JSON.parse(config.firebase.credentialsJson) as Record<string, string>;
    initializeApp({
      credential: cert(cred),
      projectId: config.firebase.projectId || cred.project_id,
    });
    return true;
  }
  if (config.firebase.projectId) {
    // May work with Application Default Credentials; otherwise REST fallback.
    initializeApp({ projectId: config.firebase.projectId });
    return true;
  }
  return false;
}

async function ensureViaAdmin(
  email: string,
  password: string,
  displayName: string | undefined,
  resetPassword: boolean
): Promise<{ uid: string; created: boolean; source: 'admin' }> {
  initAdmin();
  const auth = getAuth();
  try {
    const existing = await auth.getUserByEmail(email);
    if (resetPassword) {
      await auth.updateUser(existing.uid, {
        password,
        emailVerified: true,
        displayName: displayName ?? existing.displayName,
      });
    } else if (displayName && displayName !== existing.displayName) {
      await auth.updateUser(existing.uid, { displayName });
    }
    return { uid: existing.uid, created: false, source: 'admin' };
  } catch (e: unknown) {
    const code = (e as { code?: string })?.code;
    if (code !== 'auth/user-not-found') throw e;
  }
  const created = await auth.createUser({
    email,
    password,
    emailVerified: true,
    displayName: displayName ?? 'Maestro QA',
  });
  return { uid: created.uid, created: true, source: 'admin' };
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

async function ensureViaRest(
  email: string,
  password: string,
  displayName: string | undefined,
  resetPassword: boolean
): Promise<{ uid: string; created: boolean; source: 'rest' }> {
  const apiKey = resolveWebApiKey();
  if (!apiKey) {
    throw new Error(
      'No FIREBASE_SERVICE_ACCOUNT_JSON and no FIREBASE_WEB_API_KEY (or apk/app/google-services.json)'
    );
  }

  try {
    const signedIn = await identityToolkit(apiKey, 'signInWithPassword', {
      email,
      password,
      returnSecureToken: true,
    });
    const uid = String(signedIn.localId);
    if (resetPassword) {
      // Password already matches; nothing to do without Admin. Warn if mismatch path unused.
    }
    if (displayName) {
      // Profile display name is stored in Postgres; Firebase displayName optional via Admin only.
    }
    return { uid, created: false, source: 'rest' };
  } catch (e: unknown) {
    const msg = String((e as Error)?.message || e);
    if (!/EMAIL_NOT_FOUND|INVALID_LOGIN_CREDENTIALS|INVALID_PASSWORD/i.test(msg)) {
      throw e;
    }
  }

  // Wrong password for existing account — need Admin to reset.
  try {
    const signedUp = await identityToolkit(apiKey, 'signUp', {
      email,
      password,
      returnSecureToken: true,
      displayName: displayName ?? 'Maestro QA',
    });
    return { uid: String(signedUp.localId), created: true, source: 'rest' };
  } catch (e: unknown) {
    const msg = String((e as Error)?.message || e);
    if (/EMAIL_EXISTS/i.test(msg)) {
      throw new Error(
        `Firebase user ${email} exists but password does not match. ` +
          `Re-run with Admin credentials and --reset-password, or update .env.maestro.local.`
      );
    }
    throw e;
  }
}

async function main(): Promise<void> {
  const repoRoot = path.resolve(__dirname, '../../..');
  loadDotEnvFile(path.join(repoRoot, '.maestro', '.env.maestro.local'));

  const cli = parseArgs(process.argv.slice(2));
  const email = (cli.email || process.env.MAESTRO_EMAIL || '').trim();
  const password = (cli.password || process.env.MAESTRO_PASSWORD || '').trim();
  const displayName = cli.displayName || process.env.MAESTRO_DISPLAY_NAME || 'Maestro QA';
  const resetPassword = Boolean(cli.resetPassword);

  if (!email || !password) {
    throw new Error(
      'Need --email/--password or MAESTRO_EMAIL/MAESTRO_PASSWORD in .maestro/.env.maestro.local'
    );
  }
  if (password.length < 6) {
    throw new Error('Password must be at least 6 characters (Firebase requirement)');
  }

  const projectId = config.firebase.projectId || 'momentra-v2';
  let result: { uid: string; created: boolean; source: 'admin' | 'rest' };

  if (config.firebase.credentialsJson) {
    result = await ensureViaAdmin(email, password, displayName, resetPassword);
  } else {
    try {
      result = await ensureViaAdmin(email, password, displayName, resetPassword);
    } catch (adminErr) {
      console.warn(
        `[ensure-maestro-user] Admin path failed (${String(adminErr)}); trying Identity Toolkit REST…`
      );
      result = await ensureViaRest(email, password, displayName, resetPassword);
    }
  }

  const userId = firebaseUserId(projectId, result.uid);
  const profile = await ensureUserProfile(userId, email, displayName);
  if (profile === 'existed' || profile === 'cached') {
    await provisionUserProfile(userId, email, displayName);
  }

  console.log(
    JSON.stringify(
      {
        ok: true,
        email,
        firebaseUid: result.uid,
        projectId,
        userId,
        firebase: result.created ? 'created' : 'existed',
        profile,
        source: result.source,
        resetPassword,
      },
      null,
      2
    )
  );
}

main()
  .catch((e) => {
    console.error('[ensure-maestro-user] FAILED:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await closePool();
    } catch {
      /* ignore */
    }
  });
