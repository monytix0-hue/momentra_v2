/**
 * S9-QA-J — Generate / update certification report from run metadata + join report.
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/generate-certification-report.ts \
 *     --platform android --class pilot --run-id 20260902120000 --exit 0
 */
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

function main() {
  assertQaFixturesSafe('generate-certification-report');
  const repoRoot = path.resolve(__dirname, '../../../..');
  const platform = arg('--platform') || 'android';
  const cls = arg('--class') || 'unknown';
  const runId = arg('--run-id') || '';
  const exitCode = Number(arg('--exit') || '1');

  const joinReportPath = path.join(repoRoot, '.maestro', 'data', 'join-report.json');
  const manifestPath = path.join(repoRoot, '.maestro', 'input', 'MANIFEST.json');
  const join = existsSync(joinReportPath)
    ? JSON.parse(readFileSync(joinReportPath, 'utf8'))
    : null;
  const manifest = existsSync(manifestPath)
    ? JSON.parse(readFileSync(manifestPath, 'utf8'))
    : null;

  const status =
    exitCode === 0
      ? 'PASS'
      : exitCode === 2 && platform === 'ios'
        ? 'BLOCKED_ENVIRONMENT'
        : 'FAIL';

  const outDir = path.join(repoRoot, 'docs', 'qa');
  mkdirSync(outDir, { recursive: true });
  const reportPath = path.join(outDir, 'CERTIFICATION_REPORT.md');

  let existing = existsSync(reportPath) ? readFileSync(reportPath, 'utf8') : '';
  if (!existing.includes('# Momentra S9-QA Certification Report')) {
    existing = `# Momentra S9-QA Certification Report

Living report for Maestro 3,500 certification. Updated by \`qa:generate-certification-report\`.

## Gates

| Gate | Criterion | Status |
|------|-----------|--------|
| UI | ACTIVE Quick Adds open, accept input, submit | PENDING |
| Data | Backend records exist exactly once | PENDING |
| Math | Balances / splits reconcile with Excel | PENDING |
| Projection | Pulse / Activity / finance match | PENDING |
| Isolation | No cross-account leakage | PENDING |
| Idempotency | No duplicate money movements | PENDING |
| Performance | No regression 100 → 3500 | PENDING |
| Parity | Same business rule despite platform inputs | PENDING |
| Recovery | Relaunch retains state | PENDING |

## Hard gate (S9-QA-E)

Pilot (150/platform) must PASS before F/G/H/I scale runs.

## Run log

| When (UTC) | Platform | Class | Run ID | Exit | Status |
|------------|----------|-------|--------|------|--------|
`;
  }

  const when = new Date().toISOString();
  const line = `| ${when} | ${platform} | ${cls} | ${runId} | ${exitCode} | ${status} |\n`;
  if (!existing.includes('## Run log')) {
    existing += `\n## Run log\n\n| When (UTC) | Platform | Class | Run ID | Exit | Status |\n|------------|----------|-------|--------|------|--------|\n`;
  }
  // Append before trailing sections if present
  const marker = '## Join summary';
  if (existing.includes(marker)) {
    existing = existing.replace(marker, line + '\n' + marker);
  } else {
    existing += line;
  }

  const joinSummary = `
## Join summary

${
  join
    ? `- Android: ${JSON.stringify(join.android?.statusCounts || {})}
- iOS: ${JSON.stringify(join.ios?.statusCounts || {})}
- Pilot rows/platform: ${join.android?.pilot ?? 'n/a'}`
    : '_Run qa:sync-ledger-data_'
}

## Flow manifest

${
  manifest
    ? `- Generated: ${manifest.generatedAt}
- Android writable: ${manifest.android?.counts?.joinedWritable}
- iOS writable: ${manifest.ios?.counts?.joinedWritable}`
    : '_Run qa:generate-input-flows_'
}

## Ledger

- APK: \`docs/qa/ledgers/Momentra_APK_3500_Certification_Ledger.xlsx\`
- iOS: \`docs/qa/ledgers/Momentra_IOS_3500_Certification_Ledger.xlsx\`
- Reconciliation sheet: fill Actual_Results after \`qa:verify\` batches

## Cross-device sync (S9-QA-J)

Separate from platform isolation. Use shared Group/Business workspace with one APK + one iOS device after per-platform PASS.

## Defects

See \`docs/qa/MAESTRO_DEFECT_REGISTER.md\`.
`;

  // Refresh join summary section
  if (existing.includes('## Join summary')) {
    existing = existing.split('## Join summary')[0] + joinSummary;
  } else {
    existing += '\n' + joinSummary;
  }

  writeFileSync(reportPath, existing, 'utf8');

  const jsonOut = path.join(repoRoot, '.maestro', 'reports', `cert_run_${platform}_${cls}_${runId || 'na'}.json`);
  mkdirSync(path.dirname(jsonOut), { recursive: true });
  writeFileSync(
    jsonOut,
    JSON.stringify({ when, platform, class: cls, runId, exitCode, status }, null, 2) + '\n',
    'utf8'
  );

  console.log(`[generate-certification-report] ${status} → ${reportPath}`);
}

main();
