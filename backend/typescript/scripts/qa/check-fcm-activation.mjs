/**
 * Ops check for FCM activation (no secrets printed).
 * Usage: node scripts/qa/check-fcm-activation.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import pg from 'pg';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '../../../..');
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

const projectId = env.FIREBASE_PROJECT_ID || '';
const saRaw =
  env.FIREBASE_SERVICE_ACCOUNT_JSON || env.FIREBASE_CREDENTIALS_JSON || '';
const adcPath = path.join(
  process.env.APPDATA || process.env.HOME || '',
  'gcloud',
  'application_default_credentials.json'
);
const adcPresent = Boolean(
  process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    (adcPath && fs.existsSync(adcPath)) ||
    fs.existsSync(
      path.join(
        process.env.HOME || '',
        '.config',
        'gcloud',
        'application_default_credentials.json'
      )
    ) ||
    (process.env.APPDATA &&
      fs.existsSync(
        path.join(process.env.APPDATA, 'gcloud', 'application_default_credentials.json')
      ))
);

let saOk = false;
let saProject = null;
if (saRaw.trim()) {
  try {
    const parsed = JSON.parse(saRaw);
    saOk = parsed.type === 'service_account' && Boolean(parsed.private_key);
    saProject = parsed.project_id || null;
  } catch {
    saOk = false;
  }
}

const androidGs = path.join(root, 'apk/app/google-services.json');
const iosPlist = path.join(root, 'momentra/momentra/GoogleService-Info.plist');
let androidPkg = null;
let androidProject = null;
if (fs.existsSync(androidGs)) {
  const j = JSON.parse(fs.readFileSync(androidGs, 'utf8'));
  androidProject = j.project_info?.project_id ?? null;
  androidPkg = j.client?.[0]?.client_info?.android_client_info?.package_name ?? null;
}
let iosBundle = null;
let iosProject = null;
let iosGcm = false;
if (fs.existsSync(iosPlist)) {
  const t = fs.readFileSync(iosPlist, 'utf8');
  iosBundle = (t.match(/<key>BUNDLE_ID<\/key>\s*<string>([^<]+)<\/string>/) || [])[1] || null;
  iosProject = (t.match(/<key>PROJECT_ID<\/key>\s*<string>([^<]+)<\/string>/) || [])[1] || null;
  iosGcm = /<key>IS_GCM_ENABLED<\/key>\s*<true\/>/.test(t);
}

const report = {
  local_worker_would_enable_fcm:
    (saOk && (!saProject || saProject === 'momentra-v2')) || adcPresent,
  firebase_project_id_env: projectId || null,
  service_account_present: Boolean(saRaw.trim()),
  service_account_parses: saOk,
  service_account_project_id: saProject,
  application_default_credentials: adcPresent,
  android: {
    google_services_present: fs.existsSync(androidGs),
    project_id: androidProject,
    package_name: androidPkg,
    package_matches_app: androidPkg === 'com.example.momentra',
    project_matches: androidProject === 'momentra-v2',
  },
  ios: {
    plist_present: fs.existsSync(iosPlist),
    project_id: iosProject,
    bundle_id: iosBundle,
    bundle_matches_app: iosBundle === 'resolvingpoint.momentra',
    is_gcm_enabled: iosGcm,
    project_matches: iosProject === 'momentra-v2',
    note: 'APNs .p8 must be uploaded in Firebase Console (not verifiable here)',
  },
  devices: null,
  dispatch: null,
  inbox: null,
  blockers: [],
  next_actions: [],
};

if (!report.android.package_matches_app || !report.android.project_matches) {
  report.blockers.push('Android google-services.json mismatch');
}
if (!report.ios.bundle_matches_app || !report.ios.project_matches) {
  report.blockers.push('iOS GoogleService-Info.plist mismatch');
}
if (!saOk && !adcPresent) {
  report.blockers.push(
    'No Firebase credentials — set FIREBASE_SERVICE_ACCOUNT_JSON or run gcloud auth application-default login'
  );
  report.next_actions.push(
    'Org policy may block new SA keys; use existing firebase-adminsdk key on Dokploy, or ADC locally'
  );
} else if (saProject && saProject !== 'momentra-v2') {
  report.blockers.push(`Service account project_id is ${saProject}, expected momentra-v2`);
}
if (!saOk && adcPresent) {
  report.next_actions.push(
    'Local ADC OK for smoke. For Dokploy: paste existing momentra-v2 firebase-adminsdk JSON into FIREBASE_SERVICE_ACCOUNT_JSON and restart notification-worker'
  );
}

if (env.DATABASE_URL) {
  const client = new pg.Client({
    connectionString: env.DATABASE_URL,
    ssl: env.DATABASE_URL.includes('localhost')
      ? undefined
      : { rejectUnauthorized: false },
  });
  try {
    await client.connect();
    const devices = await client.query(`
      SELECT
        count(*)::int AS n,
        count(*) FILTER (
          WHERE push_token IS NOT NULL AND push_token <> '' AND revoked_at IS NULL
            AND length(push_token) >= 100
        )::int AS active_likely_fcm,
        count(*) FILTER (
          WHERE push_token IS NOT NULL AND push_token <> '' AND revoked_at IS NULL
            AND length(push_token) < 100
        )::int AS active_short_token,
        count(*) FILTER (WHERE revoked_at IS NULL)::int AS active
      FROM platform.user_device
    `);
    const byReason = await client.query(`
      SELECT coalesce(failure_reason, '(success_or_null)') AS failure_reason,
             count(*)::int AS n,
             sum(sent_count)::int AS sent_sum,
             max(sent_at) AS last_at
      FROM platform.notification_dispatch
      GROUP BY 1
      ORDER BY 2 DESC
      LIMIT 20
    `);
    const success = await client.query(
      `SELECT count(*)::int AS n FROM platform.notification_dispatch WHERE sent_count > 0`
    );
    const inbox = await client.query(
      `SELECT count(*)::int AS n, max(created_at) AS last_at FROM platform.user_notification`
    );
    const unconfigured = byReason.rows.find((r) => r.failure_reason === 'fcm_unconfigured');
    report.devices = devices.rows[0];
    report.dispatch = {
      by_reason: byReason.rows,
      any_success: success.rows[0].n,
      prod_worker_appears_configured: !unconfigured && byReason.rows.length > 0,
    };
    report.inbox = inbox.rows[0];

    if (devices.rows[0].active_likely_fcm === 0) {
      report.blockers.push(
        'No active likely-FCM tokens — sign in on device and grant notification permission'
      );
    }
    if (success.rows[0].n === 0 && byReason.rows.length > 0) {
      report.next_actions.push(
        'DB shows zero worker FCM successes yet — confirm Dokploy FIREBASE_SERVICE_ACCOUNT_JSON is the momentra-v2 firebase-adminsdk key, restart notification-worker, redeploy logging fixes'
      );
    }
  } catch (e) {
    report.blockers.push(`DB check failed: ${e.message}`);
  } finally {
    await client.end().catch(() => {});
  }
} else {
  report.blockers.push('DATABASE_URL missing — cannot verify platform.user_device');
}

report.next_actions.push(
  'Confirm APNs Authentication Key (.p8) in Firebase Console → Project settings → Cloud Messaging'
);

console.log(JSON.stringify(report, null, 2));
const hardBlockers = report.blockers.filter(
  (b) => !b.includes('Zero successful')
);
process.exit(report.local_worker_would_enable_fcm && hardBlockers.length === 0 ? 0 : 2);
