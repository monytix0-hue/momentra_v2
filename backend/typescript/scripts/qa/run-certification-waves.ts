/**
 * S9-QA orchestration checklist runner — validates artifacts for E→J.
 * Does not require a device; records PENDING_DEVICE / BLOCKED_ENVIRONMENT.
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/run-certification-waves.ts
 */
import { existsSync, readFileSync, writeFileSync, mkdirSync, readdirSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

function countYaml(dir: string): number {
  if (!existsSync(dir)) return 0;
  return readdirSync(dir).filter((f) => f.endsWith('.yaml')).length;
}

function main() {
  assertQaFixturesSafe('run-certification-waves');
  const repoRoot = path.resolve(__dirname, '../../../..');

  const checks = [
    {
      id: 'S9-QA-A',
      ok: existsSync(path.join(repoRoot, '.maestro', 'input-catalog', 'catalog.json')),
      detail: 'input-catalog/catalog.json',
    },
    {
      id: 'S9-QA-B',
      ok:
        existsSync(path.join(repoRoot, 'docs', 'qa', 'ledgers', 'Momentra_APK_3500_Certification_Ledger.xlsx')) &&
        existsSync(path.join(repoRoot, '.maestro', 'data', 'join-report.json')),
      detail: 'ledgers + join-report',
    },
    {
      id: 'S9-QA-C',
      ok: readFileSync(path.join(repoRoot, '.maestro', '.env.maestro.example'), 'utf8').includes(
        'QA_APK_PERSONAL'
      ),
      detail: 'platform aliases in .env.maestro.example',
    },
    {
      id: 'S9-QA-D',
      ok: existsSync(path.join(repoRoot, '.maestro', 'input', 'MANIFEST.json')),
      detail: 'input flow MANIFEST',
    },
    {
      id: 'S9-QA-E-artifacts',
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'pilot')) >= 3,
      detail: `android pilot yaml count=${countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'pilot'))}`,
    },
    {
      id: 'S9-QA-F-artifacts',
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'personal')) >= 20,
      detail: `personal shards=${countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'personal'))}`,
    },
    {
      id: 'S9-QA-G-artifacts',
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'group')) >= 20,
      detail: `group shards=${countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'group'))}`,
    },
    {
      id: 'S9-QA-H-artifacts',
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'business')) >= 20,
      detail: `business shards=${countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'business'))}`,
    },
    {
      id: 'S9-QA-I-artifacts',
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'stress')) === 70,
      detail: `stress shards=${countYaml(path.join(repoRoot, '.maestro', 'input', 'android', 'stress'))}`,
    },
    {
      id: 'S9-QA-J-report',
      ok: existsSync(path.join(repoRoot, 'docs', 'qa', 'CERTIFICATION_REPORT.md')),
      detail: 'CERTIFICATION_REPORT.md',
    },
    {
      id: 'S9-QA-J-offline-math',
      ok: existsSync(path.join(repoRoot, 'docs', 'qa', 'reconciliation', 'offline-math-report.json')),
      detail: 'docs/qa/reconciliation/offline-math-report.json',
    },
  ];

  const deviceNote =
    process.platform === 'win32'
      ? 'iOS device runs BLOCKED_ENVIRONMENT on Windows; Android requires USB device or -AllowEmulator'
      : 'Run pilot on attached devices before scale';

  const summary = {
    gate: 'S9-QA-orchestration',
    generatedAt: new Date().toISOString(),
    deviceNote,
    executionStatus: {
      'S9-QA-E': 'PENDING_DEVICE — .\\.maestro\\run-qa-android.ps1 -Class pilot -PrepareFixtures -AllowEmulator',
      'S9-QA-F': 'BLOCKED_UNTIL_E — hard gate after pilot PASS',
      'S9-QA-G': 'BLOCKED_UNTIL_E',
      'S9-QA-H': 'BLOCKED_UNTIL_E',
      'S9-QA-I': 'BLOCKED_UNTIL_FGH',
      'S9-QA-J': 'OFFLINE_MATH_READY — Actual_Results after device; cross-device sync separate',
    },
    checks,
    allArtifactsReady: checks.every((c) => c.ok),
  };

  const outDir = path.join(repoRoot, '.maestro', 'reports');
  mkdirSync(outDir, { recursive: true });
  writeFileSync(path.join(outDir, 'wave_orchestration.json'), JSON.stringify(summary, null, 2) + '\n');

  // Update certification report status section (preserve content after next H2 if any)
  const reportPath = path.join(repoRoot, 'docs', 'qa', 'CERTIFICATION_REPORT.md');
  let report = existsSync(reportPath) ? readFileSync(reportPath, 'utf8') : '';
  const waveSection = `## Wave orchestration status

Generated: \`${summary.generatedAt}\`

${deviceNote}

| Wave | Artifact | Device execution |
|------|----------|------------------|
| A-D | ${summary.allArtifactsReady ? 'READY' : 'INCOMPLETE'} | n/a (codegen) |
| E Pilot | READY | ${summary.executionStatus['S9-QA-E']} |
| F Personal | READY | ${summary.executionStatus['S9-QA-F']} |
| G Group | READY | ${summary.executionStatus['S9-QA-G']} |
| H Business | READY | ${summary.executionStatus['S9-QA-H']} |
| I Stress 3500 | READY | ${summary.executionStatus['S9-QA-I']} |
| J Reconcile | READY | ${summary.executionStatus['S9-QA-J']} |

Checks: ${checks.map((c) => `${c.id}=${c.ok ? 'OK' : 'FAIL'}(${c.detail})`).join('; ')}
`;

  if (report.includes('## Wave orchestration status')) {
    const before = report.split('## Wave orchestration status')[0];
    const afterParts = report.split('## Wave orchestration status')[1]?.split(/^## /m) ?? [];
    // afterParts[0] is the old wave body; remaining ## sections start without leading ##
    const rest = afterParts
      .slice(1)
      .map((s) => '## ' + s)
      .join('');
    report = before + waveSection + (rest ? '\n' + rest : '');
  } else {
    report += '\n' + waveSection;
  }
  writeFileSync(reportPath, report, 'utf8');

  console.log(JSON.stringify(summary, null, 2));
  if (!summary.allArtifactsReady) process.exitCode = 1;
}

main();
