/**
 * Dry-validate S9-QA-E..I artifacts without a physical device.
 * Exit 0 = artifacts ready; device execution still PENDING.
 */
import { existsSync, readdirSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import path from 'path';

type Check = { id: string; ok: boolean; detail: string };

function countYaml(dir: string): number {
  if (!existsSync(dir)) return 0;
  return readdirSync(dir).filter((f) => f.endsWith('.yaml') || f.endsWith('.yml')).length;
}

function parseCsvLen(file: string): number {
  if (!existsSync(file)) return 0;
  return readFileSync(file, 'utf8').split(/\r?\n/).filter(Boolean).length - 1;
}

function pilotCoverage(csvPath: string): { ok: boolean; detail: string } {
  if (!existsSync(csvPath)) return { ok: false, detail: 'missing pilot csv' };
  const text = readFileSync(csvPath, 'utf8');
  const lines = text.split(/\r?\n/).filter(Boolean).slice(1);
  const modes = new Set(lines.map((l) => l.split(',')[2]));
  const splits = new Set(
    lines
      .map((l) => {
        // Split_Method is column index 15 in joined pilot
        const cols: string[] = [];
        let cur = '';
        let q = false;
        for (let i = 0; i < l.length; i++) {
          const ch = l[i];
          if (ch === '"') {
            if (q && l[i + 1] === '"') {
              cur += '"';
              i++;
            } else q = !q;
          } else if (ch === ',' && !q) {
            cols.push(cur);
            cur = '';
          } else cur += ch;
        }
        cols.push(cur);
        return cols[15] || '';
      })
      .filter(Boolean)
  );
  const needModes = ['Personal', 'Group', 'Business'];
  const needSplits = ['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES'];
  const missingModes = needModes.filter((m) => ![...modes].some((x) => x === m));
  const missingSplits = needSplits.filter((s) => !splits.has(s));
  const ok = missingModes.length === 0 && missingSplits.length === 0 && lines.length >= 150;
  return {
    ok,
    detail: `rows=${lines.length} modes=${[...modes].join('/')} splits=${[...splits].join('/')} missingModes=${missingModes.join('|') || '-'} missingSplits=${missingSplits.join('|') || '-'}`,
  };
}

function main() {
  const repoRoot = path.resolve(__dirname, '../../../..');
  const checks: Check[] = [];

  for (const platform of ['android', 'ios'] as const) {
    const pilot = pilotCoverage(
      path.join(repoRoot, '.maestro', 'data', 'pilot', `${platform}_pilot_150.csv`)
    );
    checks.push({ id: `S9-QA-E-${platform}-pilot-csv`, ok: pilot.ok, detail: pilot.detail });
    checks.push({
      id: `S9-QA-E-${platform}-pilot-yaml`,
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'pilot')) >= 3,
      detail: `yaml=${countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'pilot'))}`,
    });
    checks.push({
      id: `S9-QA-F-${platform}-personal`,
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'personal')) >= 20,
      detail: `shards=${countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'personal'))} csv=${parseCsvLen(path.join(repoRoot, '.maestro', 'data', platform, 'personal.csv'))}`,
    });
    checks.push({
      id: `S9-QA-G-${platform}-group`,
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'group')) >= 20,
      detail: `shards=${countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'group'))} csv=${parseCsvLen(path.join(repoRoot, '.maestro', 'data', platform, 'group.csv'))}`,
    });
    checks.push({
      id: `S9-QA-H-${platform}-business`,
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'business')) >= 20,
      detail: `shards=${countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'business'))} csv=${parseCsvLen(path.join(repoRoot, '.maestro', 'data', platform, 'business.csv'))}`,
    });
    checks.push({
      id: `S9-QA-I-${platform}-stress`,
      ok: countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'stress')) === 70,
      detail: `shards=${countYaml(path.join(repoRoot, '.maestro', 'input', platform, 'stress'))}`,
    });
  }

  checks.push({
    id: 'S9-QA-J-report',
    ok: existsSync(path.join(repoRoot, 'docs', 'qa', 'CERTIFICATION_REPORT.md')),
    detail: 'CERTIFICATION_REPORT.md',
  });
  checks.push({
    id: 'S9-QA-J-offline-math',
    ok: existsSync(path.join(repoRoot, 'docs', 'qa', 'reconciliation', 'offline-math-report.json')),
    detail: 'run qa:reconcile-ledger-math first if missing',
  });

  const allOk = checks.every((c) => c.ok);
  const out = {
    generatedAt: new Date().toISOString(),
    allArtifactsReady: allOk,
    deviceExecution: {
      android: 'PENDING_DEVICE — adb devices empty on this host',
      ios: 'BLOCKED_ENVIRONMENT — requires macOS',
      hardGate: 'No F/G/H/I device runs until pilot PASS',
    },
    checks,
  };
  const outDir = path.join(repoRoot, '.maestro', 'reports');
  mkdirSync(outDir, { recursive: true });
  writeFileSync(path.join(outDir, 'wave-artifact-validation.json'), JSON.stringify(out, null, 2));
  console.log(JSON.stringify(out, null, 2));
  if (!allOk) process.exitCode = 1;
}

main();
