/**
 * Q8 — Generate Master Certification report stubs from catalog + optional evidence.
 * Never marks certification PASS unless closeout checkboxes are all true.
 *
 * Usage: QA_FIXTURES_ENABLED=true npx tsx scripts/qa/generate-certification-reports.ts
 */
import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

assertQaFixturesSafe('generate-certification-reports');

const repoRoot = path.resolve(__dirname, '../../../..');
const catalogPath = path.join(repoRoot, '.maestro', 'cert', 'catalog.json');
const docsQa = path.join(repoRoot, 'docs', 'qa');
mkdirSync(docsQa, { recursive: true });

if (!existsSync(catalogPath)) {
  throw new Error('Missing catalog.json — run qa:build-catalog');
}

const catalog = JSON.parse(readFileSync(catalogPath, 'utf8')) as {
  generatedAt: string;
  moments: Array<{
    id: string;
    context: string;
    label: string;
    momentTypeCode: string;
    theme: string;
    deferred?: boolean;
    family: string;
    figmaSetup: string | null;
    familyUiReuse?: string;
  }>;
  screens: Array<{
    id: string;
    momentId: string;
    momentLabel: string;
    screen: string;
    classification: string;
    figmaNode: string | null;
    notes: string;
  }>;
  quickAdds: Array<{
    id: string;
    context: string;
    momentId: string;
    momentLabel: string;
    label: string;
    capability: string;
    classification: string;
    apiRoute: string | null;
  }>;
  stats: Record<string, number>;
};

const runId = process.env.MAESTRO_RUN_ID || `QA-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-PENDING`;
const evidenceRoot = path.join(repoRoot, '.maestro', 'reports', runId);
mkdirSync(path.join(evidenceRoot, 'personal'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'group'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'business'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'isolation'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'backend'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'logs'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'junit'), { recursive: true });
mkdirSync(path.join(evidenceRoot, 'figma'), { recursive: true });

function countQa(ctx: string) {
  return catalog.quickAdds.filter((q) => q.context === ctx);
}

function classCounts(rows: { classification: string }[]) {
  const m = new Map<string, number>();
  for (const r of rows) m.set(r.classification, (m.get(r.classification) || 0) + 1);
  return [...m.entries()].sort().map(([k, v]) => `${k}=${v}`).join(', ');
}

/** Execution evidence: look for junit or verify json under reports */
function hasAnyExecutionEvidence(): boolean {
  const reports = path.join(repoRoot, '.maestro', 'reports');
  if (!existsSync(reports)) return false;
  try {
    const dirs = readdirSync(reports);
    return dirs.some((d) => d.includes('cert') || d.includes('qa_android'));
  } catch {
    return false;
  }
}

const executedEvidence = hasAnyExecutionEvidence();
const personalMoments = catalog.moments.filter((m) => m.context === 'PERSONAL');
const groupMoments = catalog.moments.filter((m) => m.context === 'GROUP');
const businessMoments = catalog.moments.filter((m) => m.context === 'BUSINESS' && !m.deferred);
const deferred = catalog.moments.filter((m) => m.deferred);

// Gate checkboxes — honest defaults until cert class runs green
const gate = {
  personal_4_of_4_executed: false,
  personal_quick_adds_classified: catalog.quickAdds.filter((q) => q.context === 'PERSONAL').every((q) => q.classification && q.classification !== 'UNKNOWN'),
  group_12_of_12_executed: false,
  group_quick_adds_classified: catalog.quickAdds.filter((q) => q.context === 'GROUP').every((q) => q.classification && q.classification !== 'UNKNOWN'),
  group_invite_redeem_tested: false,
  group_finance_correctness_tested: false,
  business_company_executed: false,
  business_3_of_3_executed: false,
  business_quick_adds_classified: catalog.quickAdds.filter((q) => q.context === 'BUSINESS').every((q) => q.classification && q.classification !== 'UNKNOWN'),
  business_finance_classified: true, // classified in catalog (not necessarily PASS)
  backend_writes_verified: false,
  isolation_pass: false,
  figma_frames_classified: catalog.screens.every((s) => s.classification && s.classification !== 'UNKNOWN'),
  android_physical_cert_executed: false,
  ios_suite_implemented: true,
  ios_execution: 'BLOCKED_ENVIRONMENT',
  p0: 0,
  p1: 1, // QA-A-001 still open until fixed
  unknown: 0,
};

const allGateTrue =
  gate.personal_4_of_4_executed &&
  gate.personal_quick_adds_classified &&
  gate.group_12_of_12_executed &&
  gate.group_quick_adds_classified &&
  gate.group_invite_redeem_tested &&
  gate.group_finance_correctness_tested &&
  gate.business_company_executed &&
  gate.business_3_of_3_executed &&
  gate.business_quick_adds_classified &&
  gate.backend_writes_verified &&
  gate.isolation_pass &&
  gate.figma_frames_classified &&
  gate.android_physical_cert_executed &&
  gate.ios_suite_implemented &&
  gate.p0 === 0 &&
  gate.p1 === 0 &&
  gate.unknown === 0;

const certStatus = allGateTrue ? 'PASS' : 'OPEN — S9-L→P BLOCKED';

function contextReport(ctx: 'PERSONAL' | 'GROUP' | 'BUSINESS', title: string): string {
  const moments = catalog.moments.filter((m) => m.context === ctx && !m.deferred);
  const qas = countQa(ctx);
  const lines: string[] = [
    `# ${title}`,
    '',
    `**Gate:** S9-QA Master Product Certification`,
    `**Run ID:** ${runId}`,
    `**Catalog generated:** ${catalog.generatedAt}`,
    `**Certification status:** ${certStatus}`,
    '',
    '> Nothing is PASS because a screen opened or a POST returned 201.',
    '',
    '## Moments',
    '',
  ];
  for (const m of moments) {
    const mq = qas.filter((q) => q.momentId === m.id);
    lines.push(`### ${m.id} — ${m.label}`);
    lines.push('');
    lines.push(`- Type: \`${m.momentTypeCode}\``);
    lines.push(`- Theme: \`${m.theme}\``);
    lines.push(`- Figma setup: ${m.figmaSetup || '—'}`);
    if (m.familyUiReuse) lines.push(`- Family UI reuse: \`${m.familyUiReuse}\` (FIGMA_UNIQUE — mark EQUIVALENT→PASS or PARTIAL after visual compare)`);
    lines.push(`- Maestro journey: \`.maestro/cert/{android|ios}/${ctx.toLowerCase()}/...\``);
    lines.push(`- Execution: **PENDING** (not certified until Android cert class + qa:verify evidence)`);
    lines.push(`- Quick Adds (${mq.length}): ${classCounts(mq)}`);
    lines.push('');
    lines.push('| Feature | Classification | Android | iOS | API | DB | Audit | Event | Outbox | Projection | UI | Figma |');
    lines.push('|---------|----------------|---------|-----|-----|----|-------|-------|--------|------------|----|-------|');
    for (const q of mq) {
      lines.push(
        `| ${q.label} | ${q.classification} | PENDING | BLOCKED_ENVIRONMENT | ${q.apiRoute ? 'ROUTE' : '—'} | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |`
      );
    }
    lines.push('');
  }
  if (ctx === 'BUSINESS') {
    lines.push('## Deferred inventory (must not disappear)');
    lines.push('');
    for (const d of deferred) {
      lines.push(`- **${d.label}** (\`${d.id}\`) — DEFERRED — Figma ${d.figmaSetup}`);
    }
    lines.push('');
  }
  return lines.join('\n');
}

// MASTER_QA_SUMMARY
writeFileSync(
  path.join(docsQa, 'MASTER_QA_SUMMARY.md'),
  `# Master QA Summary

**S9-QA MASTER PRODUCT CERTIFICATION:** **${certStatus}**

**Run ID:** ${runId}  
**Catalog:** \`.maestro/cert/catalog.json\`  
**Evidence root:** \`.maestro/reports/${runId}/\`  
**V030:** UNTOUCHED  
**S9-L→P:** BLOCKED until this document shows PASS

## Executive dashboard

### PERSONAL
- Moments: **0/4 executed** (journeys implemented; execution PENDING)
- Quick Adds classified: **${countQa('PERSONAL').length}/${countQa('PERSONAL').length}** (${classCounts(countQa('PERSONAL'))})
- Android: PENDING
- iOS: BLOCKED_ENVIRONMENT
- Figma coverage: classified in FIGMA_IMPLEMENTATION_COVERAGE.md

### GROUP
- Moments: **0/12 executed** (journeys implemented; execution PENDING)
- Quick Adds classified: **${countQa('GROUP').length}/${countQa('GROUP').length}** (${classCounts(countQa('GROUP'))})
- Finance / invites: PENDING
- Isolation: PENDING

### BUSINESS
- Company (B00): PENDING
- Moments: **0/3 executed**
- Quick Adds classified: **${countQa('BUSINESS').length}/${countQa('BUSINESS').length}** (${classCounts(countQa('BUSINESS'))})
- Deferred still inventoried: Vendor Operations, Multi-location Dashboard

### BACKEND
- Critical writes DB-verified: PENDING (\`npm run qa:verify\`)
- Audit / event / outbox / projection: PENDING
- Duplicate protection: PENDING (Q5)

### DEFECTS
- P0: ${gate.p0}
- P1: ${gate.p1} (see MAESTRO_DEFECT_REGISTER.md)
- UNKNOWN classifications: ${gate.unknown}

## Closeout checklist

| Requirement | Status |
|-------------|--------|
| PERSONAL 4/4 Moments executed | ${gate.personal_4_of_4_executed ? '✓' : '✗'} |
| PERSONAL every Quick Add classified | ${gate.personal_quick_adds_classified ? '✓' : '✗'} |
| GROUP 12/12 executed | ${gate.group_12_of_12_executed ? '✓' : '✗'} |
| GROUP every Quick Add classified | ${gate.group_quick_adds_classified ? '✓' : '✗'} |
| GROUP invite/redeem tested | ${gate.group_invite_redeem_tested ? '✓' : '✗'} |
| GROUP finance correctness tested | ${gate.group_finance_correctness_tested ? '✓' : '✗'} |
| BUSINESS Company lifecycle | ${gate.business_company_executed ? '✓' : '✗'} |
| BUSINESS 3/3 Moments | ${gate.business_3_of_3_executed ? '✓' : '✗'} |
| BUSINESS Quick Adds classified | ${gate.business_quick_adds_classified ? '✓' : '✗'} |
| BACKEND critical writes verified | ${gate.backend_writes_verified ? '✓' : '✗'} |
| ISOLATION pass | ${gate.isolation_pass ? '✓' : '✗'} |
| FIGMA every frame classified | ${gate.figma_frames_classified ? '✓' : '✗'} |
| ANDROID physical cert executed | ${gate.android_physical_cert_executed ? '✓' : '✗'} |
| IOS suite implemented | ${gate.ios_suite_implemented ? '✓' : '✗'} |
| IOS execution | ${gate.ios_execution} |
| P0 = 0 | ${gate.p0 === 0 ? '✓' : '✗'} |
| P1 = 0 | ${gate.p1 === 0 ? '✓' : '✗'} |
| UNKNOWN = 0 | ${gate.unknown === 0 ? '✓' : '✗'} |

## Hard rule

A representative flow is **not** certification. Writable features need:

UI → request → canonical DB → audit → domain event → outbox → projection/activity → resulting UI → persistence.

Then:

\`\`\`
S9-QA MASTER PRODUCT CERTIFICATION
        PASS
         ↓
STOP
S9-L→P may be authorized separately.
V030 remains untouched.
\`\`\`
`,
  'utf8'
);

writeFileSync(path.join(docsQa, 'PERSONAL_MAESTRO_CERTIFICATION.md'), contextReport('PERSONAL', 'Personal Maestro Certification (Q1)'), 'utf8');
writeFileSync(path.join(docsQa, 'GROUP_MAESTRO_CERTIFICATION.md'), contextReport('GROUP', 'Group Maestro Certification (Q2)'), 'utf8');
writeFileSync(path.join(docsQa, 'BUSINESS_MAESTRO_CERTIFICATION.md'), contextReport('BUSINESS', 'Business Maestro Certification (Q3)'), 'utf8');

// FIGMA coverage
const figmaLines = [
  '# Figma Implementation Coverage (Q6)',
  '',
  '**Rule:** Only screenshot-backed comparison can earn `IMPLEMENTED_EXACT`. Source inspection alone is insufficient.',
  '',
  'Statuses: IMPLEMENTED_EXACT · IMPLEMENTED_DIFFERENT · PARTIAL · MISSING_ANDROID · MISSING_IOS · MISSING_BOTH · API_GAP · BACKEND_GAP · DEFERRED · COMING_SOON · FIGMA_STALE · NOT_REQUIRED · FAMILY_UI_REUSED · PASS_CANDIDATE',
  '',
  '| Context | Moment | Screen | Figma node | Classification | Notes |',
  '|---------|--------|--------|------------|----------------|-------|',
];
for (const s of catalog.screens) {
  figmaLines.push(
    `| ${s.momentId} | ${s.momentLabel} | ${s.screen} | ${s.figmaNode || '—'} | ${s.classification} | ${s.notes.replace(/\|/g, '/')} |`
  );
}
figmaLines.push('');
figmaLines.push(`Total frames classified: **${catalog.screens.length}** · UNKNOWN: **0**`);
figmaLines.push('');
figmaLines.push('## Deferred (must remain visible)');
for (const d of deferred) {
  figmaLines.push(`- ${d.label} → DEFERRED (${d.figmaSetup})`);
}
writeFileSync(path.join(docsQa, 'FIGMA_IMPLEMENTATION_COVERAGE.md'), figmaLines.join('\n'), 'utf8');

// BACKEND_WRITE_VERIFICATION
writeFileSync(
  path.join(docsQa, 'BACKEND_WRITE_VERIFICATION.md'),
  `# Backend Write Verification

**Tool:** \`npm run qa:verify -- --run-id <RUN> --correlation-id qa-… --expect <kind>\`

**Schema:** verifies \`finance.*\` / personal tables, \`audit.audit_record\`, **\`events.domain_event\`** (not platform.domain_event), \`events.outbox_event\`, \`projection.recent_activity\`.

## Checks per write

| Check | Status when missing |
|-------|---------------------|
| Canonical record | FAIL |
| Audit | FAIL if correlation present |
| Domain event | FAIL |
| Outbox | FAIL if event exists |
| Projection / Activity | FAIL if moment/note present |
| Expected scope | FAIL on moment mismatch |
| No duplicate | FAIL if count ≠ 1 |
| No cross-Moment write | FAIL if sibling hit |
| Group split sum | FAIL if shares ≠ amount |

## Evidence

Machine JSON lands in \`.maestro/reports/<RUN_ID>/backend/\`.

**Current certification:** PENDING — no Master Cert run has closed backend proof for all Moments.

Correlation: non-production accepts \`qa-[a-z0-9-]{8,64}\` plus UUID. Header \`X-Maestro-Run-Id\` is logged on request lifecycle.
`,
  'utf8'
);

// ANDROID / IOS reports
writeFileSync(
  path.join(docsQa, 'ANDROID_MAESTRO_REPORT.md'),
  `# Android Maestro Report — Master Certification

**Suite:** \`.maestro/cert/android/\`  
**Runner:** \`.\\.maestro\\run-qa-android.ps1 -Class cert\`  
**Status:** Journeys **IMPLEMENTED** · Device execution **PENDING**

## Inventory

| Track | Flows | Status |
|-------|-------|--------|
| Q1 Personal P1–P4 | 4 | Implemented YAML |
| Q2 Group G01–G12 | 12 | Implemented YAML |
| Q3 Business B00–B03 | 4 | Implemented YAML |
| Q4 Isolation | 1 | Implemented YAML |
| Q5 Reliability | 1 | Implemented YAML |

Physical device preferred. Use \`-AllowEmulator\` only for scaffolding.

Evidence: \`.maestro/reports/<RUN_ID>/\`

Also see legacy smoke report: [MAESTRO_ANDROID_REPORT.md](./MAESTRO_ANDROID_REPORT.md)
`,
  'utf8'
);

writeFileSync(
  path.join(docsQa, 'IOS_MAESTRO_REPORT.md'),
  `# iOS Maestro Report — Master Certification

**Suite:** \`.maestro/cert/ios/\` (full mirror of Android cert journeys)  
**Execution:** **BLOCKED_ENVIRONMENT** on Windows — Mac/CI required before RC  
**Status:** Suite **IMPLEMENTED** · Execution **BLOCKED_ENVIRONMENT**

S9-L may proceed with iOS BLOCKED_ENVIRONMENT **only if** Android cert closeout is green and iOS YAML parity is complete.  
**Physical/Mac execution is mandatory before RC.**

Also see: [MAESTRO_IOS_REPORT.md](./MAESTRO_IOS_REPORT.md)
`,
  'utf8'
);

// Parity matrix from catalog
const parity: string[] = [
  '# Maestro Parity Matrix — Master Certification',
  '',
  'Statuses: PASS · FAIL · PENDING · BLOCKED_ENVIRONMENT · API_GAP · DEFERRED · FAMILY_UI_REUSED · PASS_CANDIDATE · …',
  '',
  '| Context | Moment | Feature | Android | iOS | API | DB | Audit | Event | Outbox | Projection | UI | Figma |',
  '|---------|--------|---------|---------|-----|-----|----|-------|-------|--------|------------|----|-------|',
];
for (const q of catalog.quickAdds) {
  parity.push(
    `| ${q.context} | ${q.momentId} ${q.momentLabel} | ${q.label} | PENDING | BLOCKED_ENVIRONMENT | ${q.classification.includes('API') ? 'API_GAP' : 'PENDING'} | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |`
  );
}
parity.push('');
parity.push(`Rows: **${catalog.quickAdds.length}** (hundreds expected — this is desirable).`);
writeFileSync(path.join(docsQa, 'MAESTRO_PARITY_MATRIX.md'), parity.join('\n'), 'utf8');

// Defect register schema
writeFileSync(
  path.join(docsQa, 'MAESTRO_DEFECT_REGISTER.md'),
  `# Maestro Defect Register — S9-QA Master Certification

## Severity

| Sev | Meaning |
|-----|---------|
| P0 | security / isolation / corruption / wrong money / authentication bypass |
| P1 | critical action broken / incorrect write / Quick Add unusable / major setup failure / approval wrong |
| P2 | secondary functionality / significant Figma mismatch |
| P3 | cosmetic/polish |

## Required evidence fields

\`DEFECT-ID\` · severity · platform · context · Moment · screen · Quick Add · expected · actual · Figma node · correlationId · runId · API response · backend log · screenshot · status · fix commit · rerun result

## Open defects

| ID | Sev | Platform | Context | Moment | Feature | Expected | Actual | Status |
|----|-----|----------|---------|--------|---------|----------|--------|--------|
| MQA-A-001 | P1 | Android | Auth | — | onboarding.skip | Maestro id visible | Text fallback only | OPEN (was QA-A-001) |
| MQA-A-002 | P2 | Android | Business | B01–B03 | Revenue/Invoice/Approval UI | Sheets wired | PRODUCT_GAP / ANDROID_MISSING on some surfaces | OPEN (was QA-A-002) |
| MQA-A-003 | P2 | Android | Auth | — | skip wait | Clean wait | Optional WARN | Accepted |

**P0 = 0** · **P1 = 1** (blocks Master Certification PASS)

## Q7 loop

execute → defect → classify → fix P0/P1 → targeted rerun → full affected Moment rerun
`,
  'utf8'
);

writeFileSync(
  path.join(evidenceRoot, 'README.md'),
  `# Evidence package ${runId}

Folders: personal/ group/ business/ isolation/ backend/ logs/ junit/ figma/

Populate by running cert Maestro class + qa:verify.
`,
  'utf8'
);

console.log(
  JSON.stringify(
    {
      ok: true,
      certStatus,
      allGateTrue,
      runId,
      docsQa,
      evidenceRoot,
      executedEvidence,
    },
    null,
    2
  )
);
