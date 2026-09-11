/**
 * Send a one-off FCM smoke message to a stored device token.
 * Requires FIREBASE_SERVICE_ACCOUNT_JSON in backend/.env (or env).
 * Never prints the full token.
 *
 * Usage:
 *   node scripts/qa/fcm-smoke-send.mjs
 *   node scripts/qa/fcm-smoke-send.mjs --user <uuid>
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import pg from 'pg';
import { initializeApp, cert, applicationDefault, getApps } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendRoot = path.resolve(__dirname, '../../..');

function loadEnv(file) {
  if (!fs.existsSync(file)) return {};
  const out = {};
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const i = line.indexOf('=');
    let v = line.slice(i + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1);
    }
    out[line.slice(0, i).trim()] = v;
  }
  return out;
}

const env = {
  ...loadEnv(path.join(backendRoot, '.env')),
  ...loadEnv(path.join(backendRoot, 'typescript', '.env')),
  ...process.env,
};

const saRaw = env.FIREBASE_SERVICE_ACCOUNT_JSON || env.FIREBASE_CREDENTIALS_JSON;
if (getApps().length === 0) {
  if (saRaw?.trim()) {
    const cred = JSON.parse(saRaw);
    initializeApp({
      credential: cert(cred),
      projectId: env.FIREBASE_PROJECT_ID || cred.project_id || 'momentra-v2',
    });
  } else {
    // Fallback: gcloud auth application-default login (org policy may block new SA keys).
    initializeApp({
      credential: applicationDefault(),
      projectId: env.FIREBASE_PROJECT_ID || 'momentra-v2',
    });
  }
}

const userArgIdx = process.argv.indexOf('--user');
const userFilter = userArgIdx >= 0 ? process.argv[userArgIdx + 1] : null;

const client = new pg.Client({
  connectionString: env.DATABASE_URL,
  ssl: env.DATABASE_URL?.includes('localhost')
    ? undefined
    : { rejectUnauthorized: false },
});
await client.connect();

const device = await client.query(
  `
  SELECT user_id, platform, push_token, device_id
  FROM platform.user_device
  WHERE revoked_at IS NULL
    AND length(coalesce(push_token, '')) >= 100
    AND ($1::uuid IS NULL OR user_id = $1::uuid)
  ORDER BY last_seen_at DESC NULLS LAST, created_at DESC NULLS LAST
  LIMIT 1
`,
  [userFilter]
);

if (!device.rows[0]) {
  console.error(JSON.stringify({ ok: false, detail: 'No device with likely-FCM token found' }));
  await client.end();
  process.exit(3);
}

const row = device.rows[0];
try {
  const messageId = await getMessaging().send({
    token: row.push_token,
    notification: {
      title: 'Momentra FCM smoke',
      body: 'Push delivery verified.',
    },
    data: {
      eventName: 'FcmSmokeTest',
      deepLink: 'momentra://inbox',
    },
    android: {
      priority: 'high',
      notification: { channelId: 'momentra_updates' },
    },
  });
  console.log(
    JSON.stringify({
      ok: true,
      outcome: 'fcm_send_success',
      messageId,
      userId: row.user_id,
      platform: row.platform,
      tokenFingerprint: row.push_token.slice(0, 8),
    })
  );
} catch (e) {
  const msg = String(e);
  const invalid =
    /not-registered|invalid-registration-token|registration-token-not-registered/i.test(
      msg
    );
  console.error(
    JSON.stringify({
      ok: false,
      outcome: invalid ? 'fcm_token_invalid' : 'fcm_send_failed',
      userId: row.user_id,
      platform: row.platform,
      tokenFingerprint: row.push_token.slice(0, 8),
      error: msg,
    })
  );
  process.exit(1);
} finally {
  await client.end();
}
