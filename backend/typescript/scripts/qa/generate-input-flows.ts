/**
 * S9-QA-D — Generate Maestro input flows from joined ledger CSVs.
 *
 * Reads:
 *   .maestro/data/{android,ios}/joined_3500.csv
 *   .maestro/data/pilot/{android,ios}_pilot_150.csv
 *
 * Writes:
 *   .maestro/input/{android,ios}/shared/*.yaml
 *   .maestro/input/{android,ios}/pilot/*.yaml
 *   .maestro/input/{android,ios}/{personal,group,business}/shard_NN.yaml
 *   .maestro/input/{android,ios}/stress/interleaved_shards/*.yaml
 *   .maestro/input/MANIFEST.json
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/generate-input-flows.ts
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

type Platform = 'android' | 'ios';
type Row = Record<string, string>;

const SHARD_SIZE = 50;
const APP_IDS: Record<Platform, string> = {
  android: 'com.example.momentra',
  ios: 'resolvingpoint.momentra',
};

function ensureDir(p: string) {
  if (!existsSync(p)) mkdirSync(p, { recursive: true });
}

function parseCsv(text: string): Row[] {
  const lines: string[] = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '"') {
      if (inQuotes && text[i + 1] === '"') {
        cur += '"';
        i++;
      } else inQuotes = !inQuotes;
    } else if ((ch === '\n' || ch === '\r') && !inQuotes) {
      if (ch === '\r' && text[i + 1] === '\n') i++;
      lines.push(cur);
      cur = '';
    } else cur += ch;
  }
  if (cur.length) lines.push(cur);
  if (!lines.length) return [];
  const headers = splitCsvLine(lines[0]);
  return lines.slice(1).filter(Boolean).map((line) => {
    const cols = splitCsvLine(line);
    const row: Row = {};
    headers.forEach((h, idx) => {
      row[h] = cols[idx] ?? '';
    });
    return row;
  });
}

function splitCsvLine(line: string): string[] {
  const out: string[] = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else inQuotes = !inQuotes;
    } else if (ch === ',' && !inQuotes) {
      out.push(cur);
      cur = '';
    } else cur += ch;
  }
  out.push(cur);
  return out;
}

function yamlEscape(s: string): string {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

/** Load .maestro/.env.maestro.local so generated flows work in Studio without -e. */
function loadMaestroEnv(repoRoot: string): Record<string, string> {
  const out: Record<string, string> = {};
  const file = path.join(repoRoot, '.maestro', '.env.maestro.local');
  if (!existsSync(file)) return out;
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith('#')) continue;
    const eq = t.indexOf('=');
    if (eq <= 0) continue;
    out[t.slice(0, eq).trim()] = t.slice(eq + 1).trim().replace(/^['"]|['"]$/g, '');
  }
  return out;
}

let MAESTRO_ENV: Record<string, string> = {};

function emailEnvKeys(platform: Platform, mode: string): { email: string; password: string } {
  const prefix = platform === 'android' ? 'QA_APK' : 'QA_IOS';
  if (mode === 'Personal') return { email: `${prefix}_PERSONAL_EMAIL`, password: `${prefix}_PERSONAL_PASSWORD` };
  if (mode === 'Group') return { email: `${prefix}_GROUP_OWNER_EMAIL`, password: `${prefix}_GROUP_OWNER_PASSWORD` };
  return { email: `${prefix}_BUSINESS_OWNER_EMAIL`, password: `${prefix}_BUSINESS_OWNER_PASSWORD` };
}

/** Resolved login credentials (never leave ${VAR} unresolved → "undefined" in the app). */
function loginCredentials(
  platform: Platform,
  mode: string
): { emailKey: string; passwordKey: string; email: string; password: string } {
  const keys = emailEnvKeys(platform, mode);
  const slug =
    platform === 'android'
      ? mode === 'Personal'
        ? 'apk.personal'
        : mode === 'Group'
          ? 'apk.group.owner'
          : 'apk.business.owner'
      : mode === 'Personal'
        ? 'ios.personal'
        : mode === 'Group'
          ? 'ios.group.owner'
          : 'ios.business.owner';
  const email =
    MAESTRO_ENV[keys.email] ||
    process.env[keys.email] ||
    `qa.${slug}@test.com`;
  const password =
    MAESTRO_ENV[keys.password] ||
    process.env[keys.password] ||
    'MaestroQa123';
  return { emailKey: keys.email, passwordKey: keys.password, email, password };
}

function flowEnvHeader(platform: Platform, mode: string): string {
  const c = loginCredentials(platform, mode);
  return `env:
  ${c.emailKey}: "${yamlEscape(c.email)}"
  ${c.passwordKey}: "${yamlEscape(c.password)}"
`;
}

function contextId(mode: string): string {
  if (mode === 'Personal') return 'context.personal';
  if (mode === 'Group') return 'context.group';
  return 'context.business';
}

function loginBlock(platform: Platform, mode: string): string {
  // Product onboarding Skip → cinematic Skip (same id). Must tap Skip twice; see smoke_onboarding_to_login.yaml.
  return `- launchApp:
    clearState: true
- extendedWaitUntil:
    visible:
      id: "onboarding.skip"
    timeout: 30000
    optional: true
- runFlow:
    when:
      visible:
        id: "onboarding.skip"
    commands:
      - tapOn:
          id: "onboarding.skip"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible: "Skip"
    commands:
      - tapOn: "Skip"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible: "Skip"
    commands:
      - tapOn: "Skip"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible:
        id: "onboarding.skip"
    commands:
      - tapOn:
          id: "onboarding.skip"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible: "Step Inside"
    commands:
      - tapOn: "Step Inside"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible:
        id: "consent.continue"
    commands:
      - tapOn:
          id: "consent.continue"
      - waitForAnimationToEnd
- runFlow:
    when:
      visible: "Continue"
    commands:
      - tapOn: "Continue"
      - waitForAnimationToEnd
- extendedWaitUntil:
    visible:
      id: "login.email"
    timeout: 60000
- tapOn:
    id: "login.email"
- eraseText
- inputText: "${yamlEscape(loginCredentials(platform, mode).email)}"
- tapOn:
    id: "login.password"
- eraseText
- inputText: "${yamlEscape(loginCredentials(platform, mode).password)}"
- tapOn:
    id: "login.submit"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000`;
}

function setupSubmitSteps(submitId: string, activateText: string, momentId: string, opts?: { stickyFooter?: boolean }): string {
  // Match visible CTA via regex (trailing → glyph breaks exact UTF matching).
  const activateRegex = `${activateText}.*`;
  const scroll = opts?.stickyFooter
    ? ''
    : `- scrollUntilVisible:
    element: "${yamlEscape(activateRegex)}"
    direction: DOWN
    timeout: 90000
    visibilityPercentage: 40
    centerElement: true
`;
  return `${scroll}- tapOn: "${yamlEscape(activateRegex)}"
- runFlow:
    when:
      visible:
        id: "${submitId}"
    commands:
      - tapOn:
          id: "${submitId}"
- waitForAnimationToEnd
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 120000
# moment=${momentId}`;
}

const GROUP_FAMILY_UI: Record<string, { card: string; setupHint: string }> = {
  'Shared Experience': {
    card: 'Trips, weddings, celebrations, outings and events.',
    setupHint: 'Set up Shared Experience',
  },
  'Shared Purchase': {
    card: 'Plan, fund and track something together.',
    setupHint: 'Set up Shared Purchase',
  },
  'Shared Living': {
    card: 'Coordinate a home, routine or shared space.',
    setupHint: 'Set up Shared Living',
  },
};

/** Chip strip labels in group setup wizards (short labels, not ledger names). */
const GROUP_MOMENT_CHIP: Record<string, string> = {
  Trip: 'Trip',
  Wedding: 'Wedding',
  'House Party': 'Party',
  'Office Outing': 'Office',
  'Gift Pool': 'Gift',
  Purchase: 'Purchase',
  Assets: 'Asset',
  'Custom Purchase': 'Custom',
  Flatmates: 'Flatmates',
  'Family Household': 'Family',
  'Shared Living': 'Co-living',
  'Custom Living': 'Custom',
};

const GROUP_ACTIVATE_TEXT: Record<string, string> = {
  'Shared Experience': 'Activate Shared Experience',
  'Shared Purchase': 'Activate Purchase',
  'Shared Living': 'Activate Living',
};

const PERSONAL_ACTIVATE_TEXT: Record<string, string> = {
  'Life Operations': 'Activate Life Operations',
  'Future Building': 'Activate Future Building',
  Lifestyle: 'Activate My Lifestyle',
  Relationships: 'Activate My Relationships',
};

const BUSINESS_SETUP_ID: Record<string, string> = {
  'Team Operations': 'business.setup.team_operations',
  'Business Runway': 'business.setup.business_runway',
  'Business Operations': 'business.setup.business_operations',
};

const BUSINESS_ACTIVATE_TEXT: Record<string, string> = {
  'Team Operations': 'Activate Team Operations',
  'Business Runway': 'Activate Business Runway',
  'Business Operations': 'Activate Business Operations',
};

function ensureMomentSteps(row: Row): string {
  const label = row.Moment;
  const mode = row.Mode;
  const momentId = row.catalog_moment_id;
  if (mode === 'Group') {
    const family = GROUP_FAMILY_UI[row.Family] || GROUP_FAMILY_UI['Shared Experience'];
    const chip = GROUP_MOMENT_CHIP[label];
    const activate = GROUP_ACTIVATE_TEXT[row.Family] || 'Activate Shared Experience';
    const chipTap = chip
      ? `- runFlow:
    when:
      visible: "${yamlEscape(chip)}"
    commands:
      - tapOn: "${yamlEscape(chip)}"
      - waitForAnimationToEnd`
      : '';
    return `- tapOn:
    id: "topbar.new_moment"
- extendedWaitUntil:
    visible: "${yamlEscape(family.card)}"
    timeout: 30000
- tapOn: "${yamlEscape(family.card)}"
- waitForAnimationToEnd
- extendedWaitUntil:
    visible: "${yamlEscape(family.setupHint)}"
    timeout: 30000
${chipTap}
${setupSubmitSteps('group.setup.submit', activate, momentId)}`;
  }
  if (mode === 'Business') {
    const setupId = BUSINESS_SETUP_ID[label] || 'business.setup.team_operations';
    const activate = BUSINESS_ACTIVATE_TEXT[label] || 'Activate Team Operations';
    return `- tapOn:
    id: "topbar.new_moment"
- tapOn:
    id: "${setupId}"
- waitForAnimationToEnd
${setupSubmitSteps('business.setup.submit', activate, momentId, { stickyFooter: true })}`;
  }
  const activate = PERSONAL_ACTIVATE_TEXT[label] || 'Activate Life Operations';
  return `- tapOn:
    id: "topbar.new_moment"
- runFlow:
    when:
      visible: "${yamlEscape(label)}"
    commands:
      - tapOn: "${yamlEscape(label)}"
      - waitForAnimationToEnd
${setupSubmitSteps('personal.setup.submit', activate, momentId)}`;
}

function writeTxnSteps(row: Row): string {
  if (row.join_status.startsWith('SKIP')) {
    return `# SKIP ${row.Txn_ID} ${row.join_status}: ${row.join_notes}\n`;
  }

  const catalogQa = row.catalog_quick_add || row.Quick_Add;
  // Business Runway "Revenue" hub tile label is "Log Revenue"; ledger may still say Revenue.
  const tileLabel =
    row.Mode === 'Business' && catalogQa === 'Revenue'
      ? 'Log Revenue'
      : row.Mode === 'Business' && catalogQa === 'Expense'
        ? 'Log Expense'
        : row.Mode === 'Business' && catalogQa === 'Spend Entry'
          ? 'Log Spend Entry'
          : catalogQa;

  const tile = row.tile_maestro_id;
  const tileTap = tile
    ? `- runFlow:
    when:
      visible:
        id: "${tile}"
    commands:
      - tapOn:
          id: "${tile}"
- runFlow:
    when:
      visible: "${yamlEscape(tileLabel)}"
    commands:
      - tapOn: "${yamlEscape(tileLabel)}"`
    : `- runFlow:
    when:
      visible: "${yamlEscape(tileLabel)}"
    commands:
      - tapOn: "${yamlEscape(tileLabel)}"`;

  const amount = row.Amount || '137.41';
  const note = row.correlation_note || row.Description;
  // Joined CSV still maps some Runway Revenue rows to expense.* ids — prefer revenue.* when catalog says Revenue.
  let amountId = row.amount_maestro_id;
  let noteId = row.note_maestro_id;
  let submitId = row.submit_maestro_id;
  if (row.Mode === 'Business' && catalogQa === 'Revenue') {
    amountId = 'business.revenue.amount';
    noteId = '';
    submitId = 'business.revenue.submit';
  }
  const splitId = row.split_maestro_id;

  const fill: string[] = [];
  if (splitId) {
    fill.push(`- runFlow:
    when:
      visible:
        id: "${splitId}"
    commands:
      - tapOn:
          id: "${splitId}"`);
  }
  if (noteId) {
    fill.push(`- runFlow:
    when:
      visible:
        id: "${noteId}"
    commands:
      - tapOn:
          id: "${noteId}"
      - eraseText
      - inputText: "${yamlEscape(note)}"`);
  }
  if (amountId) {
    fill.push(`- runFlow:
    when:
      visible:
        id: "${amountId}"
    commands:
      - tapOn:
          id: "${amountId}"
      - eraseText
      - inputText: "${yamlEscape(amount)}"`);
  }
  if (submitId) {
    fill.push(`- runFlow:
    when:
      visible:
        id: "${submitId}"
    commands:
      - tapOn:
          id: "${submitId}"
      - extendedWaitUntil:
          notVisible:
            id: "${submitId}"
          timeout: 45000`);
  } else {
    fill.push(`- runFlow:
    when:
      visible: "Save"
    commands:
      - tapOn: "Save"
- runFlow:
    when:
      visible: "Submit"
    commands:
      - tapOn: "Submit"`);
  }

  return `# ${row.Txn_ID} ${row.Mode}/${row.Moment}/${row.Quick_Add} join=${row.join_status}
- tapOn:
    id: "bottom.quickadd"
${tileTap}
${fill.join('\n')}
# Never unconditional back — exits the app to launcher on Android.
- runFlow:
    when:
      notVisible:
        id: "bottom.pulse"
    commands:
      - back
- runFlow:
    when:
      notVisible:
        id: "bottom.pulse"
    commands:
      - launchApp:
          clearState: false
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 60000
- runFlow:
    when:
      visible:
        id: "bottom.activity"
    commands:
      - tapOn:
          id: "bottom.activity"
- runFlow:
    when:
      visible: "${yamlEscape(note.slice(0, 40))}"
    commands:
      - assertVisible: "${yamlEscape(note.slice(0, 40))}"
- tapOn:
    id: "bottom.pulse"
`;
}

function buildFlow(
  platform: Platform,
  tag: string,
  rows: Row[],
  opts: { createMoments: boolean; name: string }
): string {
  const mode = rows[0]?.Mode || 'Personal';
  const header = `appId: ${APP_IDS[platform]}
tags: [${tag}, ${platform}, input, ${mode.toLowerCase()}]
${flowEnvHeader(platform, mode)}---
# Generated by generate-input-flows.ts — do not hand-edit
# rows=${rows.length} first=${rows[0]?.Txn_ID || ''} last=${rows[rows.length - 1]?.Txn_ID || ''}
# Login baked from .env.maestro.local so Maestro Studio works without -e
${loginBlock(platform, mode)}
- tapOn:
    id: "${contextId(mode)}"
`;

  const momentKeys = new Set<string>();
  const body: string[] = [];
  for (const row of rows) {
    const key = `${row.Mode}:${row.Moment}`;
    if (opts.createMoments && !momentKeys.has(key)) {
      momentKeys.add(key);
      body.push(ensureMomentSteps(row));
    }
    body.push(writeTxnSteps(row));
  }

  return `${header}${body.join('\n')}- takeScreenshot: ${opts.name}_done
`;
}

function chunk<T>(arr: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
  return out;
}

function writableRows(rows: Row[]): Row[] {
  return rows.filter((r) => !r.join_status.startsWith('SKIP'));
}

function generatePlatform(repoRoot: string, platform: Platform) {
  const dataRoot = path.join(repoRoot, '.maestro', 'data');
  const outRoot = path.join(repoRoot, '.maestro', 'input', platform);
  ensureDir(path.join(outRoot, 'shared'));
  ensureDir(path.join(outRoot, 'pilot'));
  ensureDir(path.join(outRoot, 'personal'));
  ensureDir(path.join(outRoot, 'group'));
  ensureDir(path.join(outRoot, 'business'));
  ensureDir(path.join(outRoot, 'stress'));

  const joined = parseCsv(readFileSync(path.join(dataRoot, platform, 'joined_3500.csv'), 'utf8'));
  const pilot = parseCsv(readFileSync(path.join(dataRoot, 'pilot', `${platform}_pilot_150.csv`), 'utf8'));

  writeFileSync(
    path.join(outRoot, 'shared', 'login_personal.yaml'),
    `appId: ${APP_IDS[platform]}
tags: [shared, ${platform}]
${flowEnvHeader(platform, 'Personal')}---
${loginBlock(platform, 'Personal')}
`,
    'utf8'
  );

  // Pilot: one flow per mode (50 rows each approx from interleaved B01-B03)
  const pilotByMode: Record<string, Row[]> = { Personal: [], Group: [], Business: [] };
  for (const r of writableRows(pilot)) {
    pilotByMode[r.Mode]?.push(r);
  }
  const pilotFiles: string[] = [];
  for (const mode of ['Personal', 'Group', 'Business'] as const) {
    const rows = pilotByMode[mode];
    if (!rows.length) continue;
    const name = `pilot_${mode.toLowerCase()}`;
    const rel = path.join('pilot', `${name}.yaml`);
    writeFileSync(
      path.join(outRoot, rel),
      buildFlow(platform, 'pilot', rows, { createMoments: true, name }),
      'utf8'
    );
    pilotFiles.push(rel.replace(/\\/g, '/'));
  }

  const shardFiles: Record<string, string[]> = { personal: [], group: [], business: [] };
  for (const mode of ['Personal', 'Group', 'Business'] as const) {
    const key = mode.toLowerCase() as 'personal' | 'group' | 'business';
    const rows = writableRows(joined.filter((r) => r.Mode === mode));
    const shards = chunk(rows, SHARD_SIZE);
    shards.forEach((shard, idx) => {
      const n = String(idx + 1).padStart(2, '0');
      const rel = path.join(key, `shard_${n}.yaml`);
      writeFileSync(
        path.join(outRoot, rel),
        buildFlow(platform, 'input', shard, {
          createMoments: idx === 0,
          name: `${key}_shard_${n}`,
        }),
        'utf8'
      );
      shardFiles[key].push(rel.replace(/\\/g, '/'));
    });
  }

  // Stress: interleaved P/G/B — one YAML per shard; switch context when Mode changes
  const stressRows = writableRows(joined);
  const stressShards = chunk(stressRows, SHARD_SIZE);
  const stressFiles: string[] = [];
  stressShards.forEach((shard, idx) => {
    const n = String(idx + 1).padStart(2, '0');
    const rel = path.join('stress', `interleaved_${n}.yaml`);
    const parts: string[] = [];
    parts.push(`appId: ${APP_IDS[platform]}
tags: [stress, ${platform}, input]
${flowEnvHeader(platform, shard[0]?.Mode || 'Personal')}---
# Stress shard ${n} — interleaved P/G/B (${shard.length} writable rows)
# Hard gate: pilot PASS required before running stress at scale
`);
    let lastMode = '';
    for (const row of shard) {
      if (row.Mode !== lastMode) {
        if (!lastMode) {
          parts.push(loginBlock(platform, row.Mode));
        } else {
          // Same session: switch context (re-login only if account changes)
          const prevCred = emailEnvKeys(platform, lastMode);
          const nextCred = emailEnvKeys(platform, row.Mode);
          if (prevCred.email !== nextCred.email) {
            parts.push(loginBlock(platform, row.Mode));
          }
        }
        parts.push(`- tapOn:
    id: "${contextId(row.Mode)}"
`);
        lastMode = row.Mode;
      }
      parts.push(writeTxnSteps(row));
    }
    parts.push(`- takeScreenshot: stress_${n}_done\n`);
    writeFileSync(path.join(outRoot, rel), parts.join('\n'), 'utf8');
    stressFiles.push(rel.replace(/\\/g, '/'));
  });

  return {
    platform,
    pilotFiles,
    shardFiles,
    stressFiles,
    counts: {
      pilotWritable: writableRows(pilot).length,
      joinedWritable: stressRows.length,
      personalShards: shardFiles.personal.length,
      groupShards: shardFiles.group.length,
      businessShards: shardFiles.business.length,
      stressShards: stressFiles.length,
    },
  };
}

function main() {
  assertQaFixturesSafe('generate-input-flows');
  const repoRoot = path.resolve(__dirname, '../../../..');
  MAESTRO_ENV = loadMaestroEnv(repoRoot);
  const android = generatePlatform(repoRoot, 'android');
  const ios = generatePlatform(repoRoot, 'ios');

  const manifest = {
    gate: 'S9-QA-D',
    generatedAt: new Date().toISOString(),
    shardSize: SHARD_SIZE,
    hardGate: 'S9-QA-E pilot must PASS before running input/stress shards',
    android,
    ios,
  };
  const out = path.join(repoRoot, '.maestro', 'input', 'MANIFEST.json');
  writeFileSync(out, JSON.stringify(manifest, null, 2) + '\n', 'utf8');
  writeFileSync(
    path.join(repoRoot, '.maestro', 'input', 'README.md'),
    `# Maestro input flows (S9-QA-D)

Generated from joined ledger CSVs. Do not hand-edit YAML.

## Hard gate

**S9-QA-E** pilot (\`pilot/*.yaml\`) must PASS the five-element verification chain before running \`personal|group|business\` shards or \`stress/\`.

## Regenerate

\`\`\`powershell
cd backend\\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:sync-ledger-data
npm run qa:generate-input-flows
\`\`\`

## Run (Android)

\`\`\`powershell
.\\.maestro\\run-qa-android.ps1 -Class pilot -PrepareFixtures
.\\.maestro\\run-qa-android.ps1 -Class input -Context personal -Shard 1
.\\.maestro\\run-qa-android.ps1 -Class stress -Shard 1
\`\`\`

## Run (iOS — macOS only)

\`\`\`bash
./.maestro/run-qa-ios.sh pilot
./.maestro/run-qa-ios.sh input personal 1
\`\`\`
`,
    'utf8'
  );

  console.log('[generate-input-flows] android', android.counts);
  console.log('[generate-input-flows] ios', ios.counts);
  console.log(`[generate-input-flows] manifest → ${out}`);
}

main();
