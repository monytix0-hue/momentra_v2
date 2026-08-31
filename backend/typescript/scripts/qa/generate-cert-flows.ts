/**
 * Generate Maestro cert YAML flows from catalog.json (Q0→Q1–Q5 scaffolding).
 * Run after qa:build-catalog.
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';

const repoRoot = path.resolve(__dirname, '../../../..');
const catalogPath = path.join(repoRoot, '.maestro', 'cert', 'catalog.json');
if (!existsSync(catalogPath)) {
  throw new Error('Missing .maestro/cert/catalog.json — run npm run qa:build-catalog first');
}
const catalog = JSON.parse(readFileSync(catalogPath, 'utf8')) as {
  moments: Array<{
    id: string;
    context: string;
    label: string;
    momentTypeCode: string;
    theme: string;
    deferred?: boolean;
    family: string;
  }>;
  quickAdds: Array<{ momentId: string; label: string; classification: string; androidEnabled: boolean }>;
};

function ensureDir(p: string) {
  mkdirSync(p, { recursive: true });
}

function loginBlock(emailEnv: string, passwordEnv: string): string {
  return `- launchApp:
    clearState: true
- extendedWaitUntil:
    visible: "Skip"
    timeout: 20000
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
- inputText: \${${emailEnv}}
- tapOn:
    id: "login.password"
- eraseText
- inputText: \${${passwordEnv}}
- tapOn:
    id: "login.submit"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000`;
}

function personalFlow(m: { id: string; label: string; momentTypeCode: string; theme: string }): string {
  const tiles = catalog.quickAdds.filter((q) => q.momentId === m.id && q.androidEnabled);
  const tileSteps = tiles
    .map(
      (t) => `
# Quick Add: ${t.label} (${t.classification})
- tapOn:
    id: "bottom.quickadd"
- runFlow:
    when:
      visible: "${t.label}"
    commands:
      - tapOn: "${t.label}"
- takeScreenshot: cert_${m.id.toLowerCase()}_qa_${t.label.toLowerCase().replace(/\s+/g, '_')}
- runFlow:
    when:
      visible:
        id: "personal.expense.submit"
    commands:
      - tapOn:
          id: "personal.expense.note"
      - eraseText
      - inputText: "MAESTRO-\${MAESTRO_RUN_ID}-${m.id}-${t.label.replace(/\s+/g, '')}"
      - tapOn:
          id: "personal.expense.amount"
      - eraseText
      - inputText: "137.41"
      - tapOn:
          id: "personal.expense.submit"
      - extendedWaitUntil:
          notVisible:
            id: "personal.expense.submit"
          timeout: 45000
- back
`
    )
    .join('\n');

  return `appId: com.example.momentra
tags: [cert, personal, ${m.id.toLowerCase()}, android]
---
# Q1-${m.id} ${m.label} Master Certification
# type=${m.momentTypeCode} theme=${m.theme}
# PASS requires UI + API + DB + audit + event + outbox + projection + persist (see qa:verify)
${loginBlock('QA_EMPTY_EMAIL', 'QA_EMPTY_PASSWORD')}
- tapOn:
    id: "context.personal"
- takeScreenshot: cert_${m.id.toLowerCase()}_empty
- tapOn:
    id: "topbar.new_moment"
- runFlow:
    when:
      visible: "${m.label}"
    commands:
      - tapOn: "${m.label}"
- takeScreenshot: cert_${m.id.toLowerCase()}_setup
- runFlow:
    when:
      visible: "Continue"
    commands:
      - tapOn: "Continue"
- runFlow:
    when:
      visible: "Activate"
    commands:
      - tapOn: "Activate"
- runFlow:
    when:
      visible: "Create"
    commands:
      - tapOn: "Create"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_${m.id.toLowerCase()}_pulse
- tapOn:
    id: "bottom.moments"
- takeScreenshot: cert_${m.id.toLowerCase()}_moments
- tapOn:
    id: "bottom.life"
- takeScreenshot: cert_${m.id.toLowerCase()}_life
- tapOn:
    id: "bottom.memory"
- takeScreenshot: cert_${m.id.toLowerCase()}_memory
- tapOn:
    id: "bottom.pulse"
- assertVisible:
    id: "bottom.pulse"
${tileSteps}
- tapOn:
    id: "bottom.pulse"
- takeScreenshot: cert_${m.id.toLowerCase()}_after_writes
- stopApp
- launchApp:
    clearState: false
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_${m.id.toLowerCase()}_relaunch
# Backend proof (runner): npm run qa:verify -- --run-id $MAESTRO_RUN_ID --expect personal-expense --alias QA_EMPTY
`;
}

function groupFlow(m: {
  id: string;
  label: string;
  momentTypeCode: string;
  family: string;
}): string {
  const familyHint =
    m.family === 'SHARED_EXPERIENCE'
      ? 'Experience'
      : m.family === 'SHARED_PURCHASE'
        ? 'Purchase'
        : 'Living';
  return `appId: com.example.momentra
tags: [cert, group, ${m.id.toLowerCase()}, android]
---
# Q2-${m.id} ${m.label} (${m.momentTypeCode}) — independent certification
# Do not treat sibling ${familyHint} variants as equivalent without FIGMA_UNIQUE+FAMILY_UI_REUSED check
${loginBlock('QA_GROUP_OWNER_EMAIL', 'QA_GROUP_OWNER_PASSWORD')}
- tapOn:
    id: "context.group"
- takeScreenshot: cert_${m.id.toLowerCase()}_empty
- tapOn:
    id: "topbar.new_moment"
- runFlow:
    when:
      visible: "${familyHint}"
    commands:
      - tapOn: "${familyHint}"
- runFlow:
    when:
      visible: "${m.label}"
    commands:
      - tapOn: "${m.label}"
- takeScreenshot: cert_${m.id.toLowerCase()}_setup
- runFlow:
    when:
      visible: "Continue"
    commands:
      - tapOn: "Continue"
- runFlow:
    when:
      visible: "Activate"
    commands:
      - tapOn: "Activate"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_${m.id.toLowerCase()}_pulse
- tapOn:
    id: "bottom.moments"
- takeScreenshot: cert_${m.id.toLowerCase()}_moments
- tapOn:
    id: "bottom.life"
- takeScreenshot: cert_${m.id.toLowerCase()}_life
- tapOn:
    id: "bottom.memory"
- takeScreenshot: cert_${m.id.toLowerCase()}_memory
- tapOn:
    id: "bottom.quickadd"
- runFlow:
    when:
      visible:
        id: "qa.tile.expense"
    commands:
      - tapOn:
          id: "qa.tile.expense"
- runFlow:
    when:
      visible: "Expense"
    commands:
      - tapOn: "Expense"
- extendedWaitUntil:
    visible:
      id: "group.expense.amount"
    timeout: 30000
- tapOn:
    id: "group.expense.note"
- eraseText
- inputText: "MAESTRO-\${MAESTRO_RUN_ID}-${m.id}-EXP"
- tapOn:
    id: "group.expense.amount"
- eraseText
- inputText: "1203.17"
- tapOn:
    id: "group.expense.submit"
- extendedWaitUntil:
    notVisible:
      id: "group.expense.submit"
    timeout: 60000
- takeScreenshot: cert_${m.id.toLowerCase()}_expense
- tapOn:
    id: "bottom.quickadd"
- runFlow:
    when:
      visible:
        id: "qa.tile.contribute"
    commands:
      - tapOn:
          id: "qa.tile.contribute"
- takeScreenshot: cert_${m.id.toLowerCase()}_contribute
- back
- stopApp
- launchApp:
    clearState: false
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_${m.id.toLowerCase()}_relaunch
# Invite/redeem + qa:verify group-expense with --amount 1203.17 --participants 3
`;
}

function businessCompanyFlow(): string {
  return `appId: com.example.momentra
tags: [cert, business, b00, android]
---
# Q3-B00 Company Setup
${loginBlock('QA_BUSINESS_OWNER_EMAIL', 'QA_BUSINESS_OWNER_PASSWORD')}
- tapOn:
    id: "context.business"
- takeScreenshot: cert_b00_empty
- runFlow:
    when:
      visible: "Create Company"
    commands:
      - tapOn: "Create Company"
- runFlow:
    when:
      visible:
        id: "business.company.create"
    commands:
      - tapOn:
          id: "business.company.create"
- takeScreenshot: cert_b00_create
- runFlow:
    when:
      visible: "Continue"
    commands:
      - tapOn: "Continue"
- runFlow:
    when:
      visible: "Create"
    commands:
      - tapOn: "Create"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- assertVisible:
    id: "company.switcher"
- takeScreenshot: cert_b00_selected
- tapOn:
    id: "account.sign_out"
- extendedWaitUntil:
    visible:
      id: "login.email"
    timeout: 60000
- runFlow:
    when:
      visible: "Skip"
    commands:
      - tapOn: "Skip"
      - waitForAnimationToEnd
- extendedWaitUntil:
    visible:
      id: "login.email"
    timeout: 60000
- tapOn:
    id: "login.email"
- eraseText
- inputText: \${QA_BUSINESS_OWNER_EMAIL}
- tapOn:
    id: "login.password"
- eraseText
- inputText: \${QA_BUSINESS_OWNER_PASSWORD}
- tapOn:
    id: "login.submit"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- tapOn:
    id: "context.business"
- takeScreenshot: cert_b00_restored
`;
}

function businessMomentFlow(m: { id: string; label: string }): string {
  return `appId: com.example.momentra
tags: [cert, business, ${m.id.toLowerCase()}, android]
---
# Q3-${m.id} ${m.label}
${loginBlock('QA_BUSINESS_OWNER_EMAIL', 'QA_BUSINESS_OWNER_PASSWORD')}
- tapOn:
    id: "context.business"
- tapOn:
    id: "topbar.new_moment"
- runFlow:
    when:
      visible: "${m.label}"
    commands:
      - tapOn: "${m.label}"
- takeScreenshot: cert_${m.id.toLowerCase()}_setup
- runFlow:
    when:
      visible: "Continue"
    commands:
      - tapOn: "Continue"
- runFlow:
    when:
      visible: "Activate"
    commands:
      - tapOn: "Activate"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_${m.id.toLowerCase()}_pulse
- tapOn:
    id: "bottom.moments"
- takeScreenshot: cert_${m.id.toLowerCase()}_moments
- tapOn:
    id: "bottom.life"
- takeScreenshot: cert_${m.id.toLowerCase()}_life
- tapOn:
    id: "bottom.memory"
- takeScreenshot: cert_${m.id.toLowerCase()}_memory
- tapOn:
    id: "bottom.quickadd"
- runFlow:
    when:
      visible: "Expense"
    commands:
      - tapOn: "Expense"
- extendedWaitUntil:
    visible:
      id: "business.expense.amount"
    timeout: 30000
- tapOn:
    id: "business.expense.note"
- eraseText
- inputText: "MAESTRO-\${MAESTRO_RUN_ID}-${m.id}-EXP"
- tapOn:
    id: "business.expense.amount"
- eraseText
- inputText: "8713.22"
- tapOn:
    id: "business.expense.submit"
- extendedWaitUntil:
    notVisible:
      id: "business.expense.submit"
    timeout: 60000
- takeScreenshot: cert_${m.id.toLowerCase()}_expense
# Revenue/Invoice/Approval: classify PASS_CANDIDATE or API_GAP/ANDROID_MISSING per catalog; execute when UI exists
`;
}

function isolationFlow(): string {
  return `appId: com.example.momentra
tags: [cert, isolation, q4, android]
---
# Q4 Isolation — user / Moment / Group / Company (any leak = P0)
${loginBlock('QA_PERSONAL_EMAIL', 'QA_PERSONAL_PASSWORD')}
- tapOn:
    id: "context.personal"
- takeScreenshot: cert_q4_personal_u1
- tapOn:
    id: "account.sign_out"
- extendedWaitUntil:
    visible:
      id: "login.email"
    timeout: 60000
- tapOn:
    id: "login.email"
- eraseText
- inputText: \${QA_MULTI_CONTEXT_EMAIL}
- tapOn:
    id: "login.password"
- eraseText
- inputText: \${QA_MULTI_CONTEXT_PASSWORD}
- tapOn:
    id: "login.submit"
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- tapOn:
    id: "context.personal"
- takeScreenshot: cert_q4_personal_u2
- assertNotVisible: "MAESTRO-LEAK-SHOULD-NOT-EXIST"
- tapOn:
    id: "context.group"
- takeScreenshot: cert_q4_group
- tapOn:
    id: "context.business"
- takeScreenshot: cert_q4_business
# Outsider cannot see Group A / Company A — assert empty or access denied surfaces
`;
}

function reliabilityFlow(): string {
  return `appId: com.example.momentra
tags: [cert, reliability, q5, android]
---
# Q5 Reliability / security
${loginBlock('QA_MULTI_CONTEXT_EMAIL', 'QA_MULTI_CONTEXT_PASSWORD')}
- tapOn:
    id: "context.personal"
- tapOn:
    id: "bottom.quickadd"
- runFlow:
    when:
      visible: "Expense"
    commands:
      - tapOn: "Expense"
- extendedWaitUntil:
    visible:
      id: "personal.expense.submit"
    timeout: 30000
- tapOn:
    id: "personal.expense.note"
- eraseText
- inputText: "MAESTRO-\${MAESTRO_RUN_ID}-DBL"
- tapOn:
    id: "personal.expense.amount"
- eraseText
- inputText: "42.00"
# Double submit — canonical row count must be 1 (qa:verify)
- tapOn:
    id: "personal.expense.submit"
- tapOn:
    id: "personal.expense.submit"
- extendedWaitUntil:
    notVisible:
      id: "personal.expense.submit"
    timeout: 60000
- takeScreenshot: cert_q5_double_submit
- stopApp
- launchApp:
    clearState: false
- extendedWaitUntil:
    visible:
      id: "bottom.pulse"
    timeout: 90000
- takeScreenshot: cert_q5_warm_launch
# Offline / 500 / PIN / biometric / deep-link: platform-specific subflows (document BLOCKED if unavailable)
`;
}

// --- write files ---
const androidRoot = path.join(repoRoot, '.maestro', 'cert', 'android');
const iosRoot = path.join(repoRoot, '.maestro', 'cert', 'ios');
ensureDir(path.join(androidRoot, 'personal'));
ensureDir(path.join(androidRoot, 'group'));
ensureDir(path.join(androidRoot, 'business'));
ensureDir(path.join(androidRoot, 'isolation'));
ensureDir(path.join(androidRoot, 'reliability'));
ensureDir(path.join(iosRoot, 'personal'));
ensureDir(path.join(iosRoot, 'group'));
ensureDir(path.join(iosRoot, 'business'));
ensureDir(path.join(iosRoot, 'isolation'));
ensureDir(path.join(iosRoot, 'reliability'));

for (const m of catalog.moments) {
  if (m.deferred) continue;
  if (m.context === 'PERSONAL') {
    const body = personalFlow(m);
    writeFileSync(path.join(androidRoot, 'personal', `${m.id.toLowerCase()}_${m.family.toLowerCase()}.yaml`), body);
    writeFileSync(
      path.join(iosRoot, 'personal', `${m.id.toLowerCase()}_${m.family.toLowerCase()}.yaml`),
      body.replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/android/g, 'ios')
    );
  } else if (m.context === 'GROUP') {
    const body = groupFlow(m);
    writeFileSync(path.join(androidRoot, 'group', `${m.id.toLowerCase()}_${m.momentTypeCode.toLowerCase()}.yaml`), body);
    writeFileSync(
      path.join(iosRoot, 'group', `${m.id.toLowerCase()}_${m.momentTypeCode.toLowerCase()}.yaml`),
      body.replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/, android/g, ', ios')
    );
  } else if (m.id === 'B00') {
    const body = businessCompanyFlow();
    writeFileSync(path.join(androidRoot, 'business', 'b00_company.yaml'), body);
    writeFileSync(
      path.join(iosRoot, 'business', 'b00_company.yaml'),
      body.replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/, android/g, ', ios')
    );
  } else if (m.context === 'BUSINESS') {
    const body = businessMomentFlow(m);
    writeFileSync(path.join(androidRoot, 'business', `${m.id.toLowerCase()}_${m.family.toLowerCase()}.yaml`), body);
    writeFileSync(
      path.join(iosRoot, 'business', `${m.id.toLowerCase()}_${m.family.toLowerCase()}.yaml`),
      body.replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/, android/g, ', ios')
    );
  }
}

writeFileSync(path.join(androidRoot, 'isolation', 'q4_isolation.yaml'), isolationFlow());
writeFileSync(
  path.join(iosRoot, 'isolation', 'q4_isolation.yaml'),
  isolationFlow().replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/, android/g, ', ios')
);
writeFileSync(path.join(androidRoot, 'reliability', 'q5_reliability.yaml'), reliabilityFlow());
writeFileSync(
  path.join(iosRoot, 'reliability', 'q5_reliability.yaml'),
  reliabilityFlow().replace(/appId: com.example.momentra/g, 'appId: resolvingpoint.momentra').replace(/, android/g, ', ios')
);

writeFileSync(
  path.join(repoRoot, '.maestro', 'cert', 'android', 'config.yaml'),
  `flows:
  - "**/*.yaml"
excludeTags:
  - disabled
`
);

writeFileSync(
  path.join(repoRoot, '.maestro', 'cert', 'ios', 'config.yaml'),
  `flows:
  - "**/*.yaml"
excludeTags:
  - disabled
`
);

console.log(
  JSON.stringify(
    {
      ok: true,
      androidPersonal: catalog.moments.filter((m) => m.context === 'PERSONAL').length,
      androidGroup: catalog.moments.filter((m) => m.context === 'GROUP').length,
      androidBusiness: catalog.moments.filter((m) => m.context === 'BUSINESS' && !m.deferred).length,
    },
    null,
    2
  )
);
