/**
 * Inject FIREBASE_SERVICE_ACCOUNT_JSON into backend/.env from a key file.
 * Usage: node scripts/qa/inject-firebase-sa.mjs C:\path\to\momentra-v2-adminsdk.json
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.resolve(__dirname, '../../../.env');
const keyPath = process.argv[2];

if (!keyPath) {
  console.error('Usage: node scripts/qa/inject-firebase-sa.mjs <service-account.json>');
  process.exit(2);
}

const raw = fs.readFileSync(keyPath, 'utf8');
const parsed = JSON.parse(raw);
if (parsed.type !== 'service_account' || !parsed.private_key) {
  console.error('Not a Firebase/Google service account JSON');
  process.exit(3);
}
if (parsed.project_id && parsed.project_id !== 'momentra-v2') {
  console.error(
    JSON.stringify({
      ok: false,
      detail: `Refusing project_id=${parsed.project_id}; expected momentra-v2`,
    })
  );
  process.exit(4);
}

const singleLine = JSON.stringify(parsed);
let envText = fs.existsSync(envPath) ? fs.readFileSync(envPath, 'utf8') : '';
if (/^FIREBASE_SERVICE_ACCOUNT_JSON=/m.test(envText)) {
  envText = envText.replace(
    /^FIREBASE_SERVICE_ACCOUNT_JSON=.*$/m,
    `FIREBASE_SERVICE_ACCOUNT_JSON=${singleLine}`
  );
} else {
  envText += `\nFIREBASE_SERVICE_ACCOUNT_JSON=${singleLine}\n`;
}
if (!/^FIREBASE_PROJECT_ID=/m.test(envText)) {
  envText += `FIREBASE_PROJECT_ID=momentra-v2\n`;
} else {
  envText = envText.replace(/^FIREBASE_PROJECT_ID=.*$/m, 'FIREBASE_PROJECT_ID=momentra-v2');
}
fs.writeFileSync(envPath, envText);
console.log(
  JSON.stringify({
    ok: true,
    envPath,
    project_id: parsed.project_id || 'momentra-v2',
    client_email: parsed.client_email || null,
  })
);
