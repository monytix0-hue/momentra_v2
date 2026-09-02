/**
 * Build iOS native layout audit register (layout-only; keep icons/artwork).
 *
 * Output:
 *   docs/qa/IOS_NATIVE_LAYOUT_AUDIT.csv
 *   docs/qa/IOS_NATIVE_LAYOUT_AUDIT_SUMMARY.md
 *
 * Usage: npx tsx scripts/qa/build-ios-native-layout-audit.ts
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync, readdirSync } from 'fs';
import path from 'path';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..');
const PARITY_CSV = path.join(ROOT, 'docs/qa/IOS_APK_DESIGN_PARITY_MATRIX.csv');
const OUT_CSV = path.join(ROOT, 'docs/qa/IOS_NATIVE_LAYOUT_AUDIT.csv');
const OUT_SUMMARY = path.join(ROOT, 'docs/qa/IOS_NATIVE_LAYOUT_AUDIT_SUMMARY.md');
const IOS_ROOT = path.join(ROOT, 'momentra/momentra');
const APK_UI = path.join(ROOT, 'apk/app/src/main/java/com/example/momentra/ui');

type Wave = 'A' | 'B' | 'C' | 'D';
type Container = 'scroll_vstack' | 'list' | 'form' | 'mixed' | 'none';
type StickyCta = 'none' | 'scroll_away' | 'safe_area_inset' | 'toolbar';
type SheetChrome = 'native' | 'duplicate_header' | 'custom_chrome' | 'n_a';
type SafeArea = 'ok' | 'magic_padding' | 'ignores_safe_area';

interface ParityRow {
  context: string;
  moment: string;
  screen: string;
  apk_path: string;
  ios_path: string;
  layout: string;
  priority: string;
  gap_notes: string;
}

interface AuditRow {
  screen_id: string;
  wave: Wave;
  context: string;
  screen: string;
  apk_path: string;
  ios_path: string;
  apk_sections: number;
  ios_sections: number;
  section_overlap_pct: number;
  container: Container;
  sticky_cta: StickyCta;
  sheet_chrome: SheetChrome;
  safe_area: SafeArea;
  native_layout_score: 0 | 1 | 2;
  content_parity_score: 0 | 1 | 2;
  priority: string;
  remediation: string;
  screenshot_ios_before: string;
}

function readText(p: string): string {
  return readFileSync(p, 'utf8');
}

function parseCsvLine(line: string): string[] {
  const out: string[] = [];
  let cur = '';
  let inQ = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (inQ && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else inQ = !inQ;
    } else if (c === ',' && !inQ) {
      out.push(cur);
      cur = '';
    } else cur += c;
  }
  out.push(cur);
  return out;
}

function slugify(context: string, screen: string): string {
  return `${context}_${screen}`.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
}

function extractApkSections(text: string): string[] {
  const names: string[] = [];
  const re = /@Composable\s+(?:private\s+)?fun\s+(\w+)/g;
  let m;
  while ((m = re.exec(text)) !== null) names.push(m[1]);
  return names;
}

function extractIosSections(text: string): string[] {
  const names: string[] = [];
  const re = /private var (\w+):\s*some View/g;
  let m;
  while ((m = re.exec(text)) !== null) names.push(m[1]);
  return names;
}

function sectionOverlap(apk: string[], ios: string[]): number {
  if (apk.length === 0 && ios.length === 0) return 100;
  const apkNorm = apk.map((s) => s.toLowerCase().replace(/content|card|section/g, ''));
  const iosNorm = ios.map((s) => s.toLowerCase().replace(/card|section/g, ''));
  let hits = 0;
  for (const a of apkNorm) {
    if (iosNorm.some((i) => i.includes(a.slice(0, 6)) || a.includes(i.slice(0, 6)))) hits++;
  }
  return apk.length ? Math.round((hits / apk.length) * 100) : 100;
}

function inferWave(context: string, screen: string, priority: string): Wave {
  if (context === 'SHELL' || screen === 'App Shell') return 'A';
  if (context === 'AUTH' || screen === 'Account Hub') return 'B';
  if (priority === 'P1') return 'C';
  return 'D';
}

function analyzeIosLayout(iosPath: string, screen: string): Pick<
  AuditRow,
  'container' | 'sticky_cta' | 'sheet_chrome' | 'safe_area' | 'native_layout_score' | 'remediation'
> {
  if (!iosPath || !existsSync(path.join(ROOT, iosPath))) {
    return {
      container: 'none',
      sticky_cta: 'none',
      sheet_chrome: 'n_a',
      safe_area: 'ok',
      native_layout_score: 0,
      remediation: 'Missing iOS file',
    };
  }

  const text = readText(path.join(ROOT, iosPath));
  const hasList = /\bList\s*\{/.test(text);
  const hasNativeDashboard = /NativeDashboardScaffold/.test(text);
  const hasForm = /\bForm\s*\{/.test(text);
  const hasScroll = /ScrollView/.test(text);
  const hasSafeInset = /safeAreaInset/.test(text);
  const hasNavStack = /NavigationStack/.test(text);
  const hasMagicPadding = /padding\(\.bottom,\s*(72|84|56|28)\)/.test(text);
  const ignoresSafe = /\.ignoresSafeArea\(/.test(text);
  const isSheet = /Sheet|sheet\(/.test(text) || screen.includes('Sheet') || iosPath.includes('Sheet');

  let container: Container = 'scroll_vstack';
  if (hasForm && !hasScroll) container = 'form';
  else if (hasNativeDashboard || hasList) container = hasScroll ? 'mixed' : 'list';
  else if (hasList && hasScroll) container = 'mixed';
  else if (hasList) container = 'list';
  else if (hasScroll || /VStack/.test(text)) container = 'scroll_vstack';

  let sticky_cta: StickyCta = 'none';
  if (hasSafeInset) sticky_cta = 'safe_area_inset';
  else if (/Button\(.*Continue|Step Inside|Log Expense|Settle/.test(text) && hasScroll) sticky_cta = 'scroll_away';
  else if (/\.toolbar/.test(text)) sticky_cta = 'toolbar';

  let sheet_chrome: SheetChrome = 'n_a';
  if (isSheet || iosPath.includes('QuickAdd') || iosPath.includes('Sheet')) {
    if (hasNavStack && /Text\("[^"]+"\)\s*\n\s*\.font\(\.system\(size:\s*1[78]/.test(text)) {
      sheet_chrome = 'duplicate_header';
    } else if (hasNavStack) sheet_chrome = 'native';
    else sheet_chrome = 'custom_chrome';
  }

  let safe_area: SafeArea = 'ok';
  if (ignoresSafe && hasMagicPadding) safe_area = 'ignores_safe_area';
  else if (hasMagicPadding) safe_area = 'magic_padding';

  let native_layout_score: 0 | 1 | 2 = 0;
  if (container === 'form' || (container === 'list' && sticky_cta === 'safe_area_inset')) native_layout_score = 2;
  else if (container === 'list' || hasSafeInset || (hasNavStack && sheet_chrome === 'native')) native_layout_score = 1;
  else if (container === 'scroll_vstack' && !hasMagicPadding) native_layout_score = 1;

  // Manual overrides from code review
  if (iosPath.includes('AccountHubView')) {
    native_layout_score = 2;
    container = 'form';
    sticky_cta = 'none';
    sheet_chrome = 'native';
  }
  if (iosPath.includes('GroupFinanceScreens')) {
    native_layout_score = 2;
    sticky_cta = 'safe_area_inset';
  }
  if (iosPath.includes('AppShellView')) {
    native_layout_score = 2;
    safe_area = 'ok';
    remediation = 'NavigationStack toolbar + native TabView + safeAreaInset FAB';
  } else if (iosPath.includes('ShellChrome')) {
    if (screen === 'Bottom Nav') {
      native_layout_score = 2;
      remediation = 'Native TabView with per-tab content roots';
    } else if (screen === 'TopBar' || screen === 'Context Switcher' || screen === 'Moment Switcher') {
      native_layout_score = 2;
      remediation = 'ShellToolbarContent + context inset for NavigationStack shell';
    }
  } else if (iosPath.includes('LoginView')) {
    native_layout_score = hasForm ? 2 : 1;
    remediation = hasForm ? 'Maintain Form login pattern' : 'Form + segmented auth picker';
  } else if (iosPath.includes('NativeScaffolds')) {
    native_layout_score = 2;
    remediation = 'Shared native scaffolds';
  } else if (iosPath.includes('ConsentGateView') || iosPath.includes('AppLockGateView')) {
    native_layout_score = hasForm && hasSafeInset ? 2 : 0;
    remediation = hasForm && hasSafeInset
      ? 'Maintain Form + safeAreaInset gate pattern'
      : 'Form + Section; pin Continue via safeAreaInset(edge: .bottom)';
  } else if (iosPath.includes('ProductOnboardingView') || iosPath.includes('OnboardingView')) {
    native_layout_score = hasSafeInset ? 2 : 0;
    remediation = hasSafeInset
      ? 'Maintain carousel art + safeAreaInset CTAs'
      : 'Keep carousel art; pin Next/Step Inside with safeAreaInset';
  } else if (iosPath.includes('QuickAddHub')) {
    native_layout_score = hasList ? 2 : 0;
    remediation = hasList
      ? 'Maintain List sections + QA icon assets'
      : 'List + Section for hero and tile grid; keep Nav/QA icon assets';
  } else if (iosPath.includes('PersonalPulseEmptyView')) {
    native_layout_score = hasList ? 2 : 0;
    remediation = hasList
      ? 'Maintain List sections for empty Pulse scaffold'
      : 'List sections for hero/KPI/activity cards; safeAreaInset for primary CTA';
  } else if (iosPath.includes('PulseActiveView') || iosPath.includes('PulseEmptyView') ||
             iosPath.includes('MomentsActiveView') || iosPath.includes('MemoryActiveView') ||
             iosPath.includes('LifeActiveView') || iosPath.includes('EmptyView')) {
    native_layout_score = (hasNativeDashboard || container === 'list') && (hasSafeInset || sticky_cta === 'safe_area_inset') ? 2
      : (hasNativeDashboard || container === 'list') ? 1 : 0;
    remediation = hasNativeDashboard || container === 'list'
      ? 'Maintain NativeDashboardScaffold sections; verify sticky CTA'
      : 'NativeDashboardScaffold + safeAreaInset for primary CTA';
  } else if (iosPath.includes('PersonalLifeActiveView')) {
    native_layout_score = hasList ? 2 : 1;
    remediation = hasList
      ? 'Maintain List sections for Life dashboard cards'
      : 'Split monolithic VStack into List sections';
  } else if (iosPath.includes('PersonalCreateEmptyView')) {
    native_layout_score = hasList ? 2 : 1;
    remediation = hasList
      ? 'Maintain List sections for create chooser'
      : 'List + Section for life cards and quick start';
  } else if (iosPath.includes('SetupWizardScaffold') || iosPath.includes('SetupView')) {
    native_layout_score = hasSafeInset ? 2 : 0;
    remediation = hasSafeInset
      ? 'Maintain safeAreaInset wizard footer'
      : 'SetupWizardScaffold: safeAreaInset footer; Form sections inside scroll';
  } else if (sheet_chrome === 'duplicate_header') {
    remediation = 'NavigationStack toolbar only; remove in-scroll title row';
  } else if (native_layout_score === 0) {
    remediation = 'Replace ScrollView+VStack with List+Section; pin CTAs with safeAreaInset';
  } else if (native_layout_score === 1) {
    remediation = 'Add safeAreaInset for sticky CTAs; split monolithic VStack into List sections';
  } else {
    remediation = 'Maintain native patterns; verify on device';
  }

  return { container, sticky_cta, sheet_chrome, safe_area, native_layout_score, remediation };
}

function contentParityScore(overlapPct: number, apkSections: number): 0 | 1 | 2 {
  if (apkSections <= 1) return 2;
  if (overlapPct >= 70) return 2;
  if (overlapPct >= 35) return 1;
  return 0;
}

function wave1BeforeShot(context: string, screen: string): string {
  const map: Record<string, string> = {
    'SHELL_App Shell': 'docs/qa/screenshots/device-audit/wave1/app_shell_personal/ios.png',
    'SHELL_TopBar': 'docs/qa/screenshots/device-audit/wave1/topbar_personal/ios.png',
    'SHELL_Context Switcher': 'docs/qa/screenshots/device-audit/wave1/context_switcher/ios.png',
    'SHELL_Moment Switcher': 'docs/qa/screenshots/device-audit/wave1/moment_switcher/ios.png',
    'SHELL_Company Switcher': 'docs/qa/screenshots/device-audit/wave1/company_switcher/ios.png',
    'SHELL_Bottom Nav': 'docs/qa/screenshots/device-audit/wave1/bottom_nav/ios.png',
    'AUTH_Consent Gate': 'docs/qa/screenshots/device-audit/wave1/consent_gate/ios.png',
    'AUTH_Product Onboarding': 'docs/qa/screenshots/device-audit/wave1/product_onboarding/ios.png',
    'AUTH_Cinematic Onboarding': 'docs/qa/screenshots/device-audit/wave1/cinematic_onboarding/ios.png',
    'AUTH_App Lock': 'docs/qa/screenshots/device-audit/wave1/app_lock/ios.png',
    'AUTH_Login': 'docs/qa/screenshots/device-audit/wave1/login/ios.png',
    'AUTH_Splash': 'docs/qa/screenshots/device-audit/wave1/splash/ios.png',
  };
  const key = `${context}_${screen}`;
  return map[key] ?? '';
}

function csvEscape(s: string): string {
  if (s.includes(',') || s.includes('"') || s.includes('\n')) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

const parityLines = readText(PARITY_CSV).trim().split('\n').slice(1);
const parityRows: ParityRow[] = parityLines.map((line) => {
  const c = parseCsvLine(line);
  return {
    context: c[0],
    moment: c[1],
    screen: c[2],
    apk_path: c[3],
    ios_path: c[4],
    layout: c[5],
    priority: c[15],
    gap_notes: c[14],
  };
});

const auditRows: AuditRow[] = [];

for (const p of parityRows) {
  const apkFull = path.join(ROOT, p.apk_path);
  const apkText = existsSync(apkFull) ? readText(apkFull) : '';
  const apkSections = extractApkSections(apkText);
  const iosFull = p.ios_path ? path.join(ROOT, p.ios_path) : '';
  const iosText = iosFull && existsSync(iosFull) ? readText(iosFull) : '';
  const iosSections = extractIosSections(iosText);
  const overlapPct = sectionOverlap(apkSections, iosSections);
  const wave = inferWave(p.context, p.screen, p.priority);
  const screenId = slugify(p.context, p.screen);
  const analysis = analyzeIosLayout(p.ios_path, p.screen);
  const before = wave1BeforeShot(p.context, p.screen);
  const beforePath =
    before && existsSync(path.join(ROOT, before))
      ? before
      : `docs/qa/screenshots/ios-native-layout/${wave}/${screenId}/before.png`;

  auditRows.push({
    screen_id: screenId,
    wave,
    context: p.context,
    screen: p.screen,
    apk_path: p.apk_path,
    ios_path: p.ios_path,
    apk_sections: apkSections.length,
    ios_sections: iosSections.length,
    section_overlap_pct: overlapPct,
    content_parity_score: contentParityScore(overlapPct, apkSections.length),
    priority: p.priority,
    screenshot_ios_before: beforePath,
    ...analysis,
  });
}

mkdirSync(path.join(ROOT, 'docs/qa/screenshots/ios-native-layout'), { recursive: true });
for (const w of ['A', 'B', 'C', 'D'] as Wave[]) {
  mkdirSync(path.join(ROOT, 'docs/qa/screenshots/ios-native-layout', w), { recursive: true });
}

const header =
  'screen_id,wave,context,screen,apk_path,ios_path,apk_sections,ios_sections,section_overlap_pct,container,sticky_cta,sheet_chrome,safe_area,native_layout_score,content_parity_score,priority,remediation,screenshot_ios_before';
const csv = [
  header,
  ...auditRows.map((r) =>
    [
      r.screen_id,
      r.wave,
      r.context,
      r.screen,
      r.apk_path,
      r.ios_path,
      r.apk_sections,
      r.ios_sections,
      r.section_overlap_pct,
      r.container,
      r.sticky_cta,
      r.sheet_chrome,
      r.safe_area,
      r.native_layout_score,
      r.content_parity_score,
      r.priority,
      r.remediation,
      r.screenshot_ios_before,
    ]
      .map((v) => csvEscape(String(v)))
      .join(',')
  ),
].join('\n');
writeFileSync(OUT_CSV, csv + '\n');

const scoreCounts = auditRows.reduce(
  (acc, r) => {
    acc[r.native_layout_score] = (acc[r.native_layout_score] ?? 0) + 1;
    return acc;
  },
  {} as Record<number, number>
);

const topQueue = [...auditRows]
  .filter((r) => r.native_layout_score <= 1)
  .sort((a, b) => {
    const pri = (p: string) => (p === 'P0' ? 0 : p === 'P1' ? 1 : p === 'P2' ? 2 : 3);
    return pri(a.priority) - pri(b.priority) || a.native_layout_score - b.native_layout_score;
  })
  .slice(0, 20);

let summary = `# iOS Native Layout Audit — Summary

**Date:** 2026-09-01  
**Register:** [\`IOS_NATIVE_LAYOUT_AUDIT.csv\`](IOS_NATIVE_LAYOUT_AUDIT.csv)  
**Regenerator:** \`npx tsx scripts/qa/build-ios-native-layout-audit.ts\`  
**Capture:** [\`scripts/qa/capture-native-layout-screenshot.sh\`](../scripts/qa/capture-native-layout-screenshot.sh)

## Scope

Layout structure only — **keep** custom nav icons (\`NavPulse\`, \`NavMoments\`, \`ShellPlus\`), hero artwork, and brand tokens. APK defines **what sections exist**; iOS HIG defines **containers** (\`List\`, \`Form\`, \`safeAreaInset\`, \`NavigationStack\`).

## Score counts (\`native_layout_score\` 0–2)

| Score | Count | Meaning |
|-------|-------|---------|
| 0 | ${scoreCounts[0] ?? 0} | ScrollView+VStack; magic padding; no native containers |
| 1 | ${scoreCounts[1] ?? 0} | Partial native (NavStack or List without sticky CTA) |
| 2 | ${scoreCounts[2] ?? 0} | Form/List + safeAreaInset or reference pattern |

**Total screens:** ${auditRows.length}

## Wave checklist

| Wave | Screens | Focus |
|------|---------|-------|
| A | ${auditRows.filter((r) => r.wave === 'A').length} | Shell chrome — TabView, top chrome, FAB inset |
| B | ${auditRows.filter((r) => r.wave === 'B').length} | Auth/gates — Form, pinned CTAs |
| C | ${auditRows.filter((r) => r.wave === 'C').length} | P1 body — QuickAdd hubs, pulse dashboards, PersonalLife |
| D | ${auditRows.filter((r) => r.wave === 'D').length} | Remaining PARTIAL backlog |

## Top 20 remediation queue

| screen_id | wave | native | content | remediation |
|-----------|------|--------|---------|-------------|
`;

for (const r of topQueue) {
  summary += `| ${r.screen_id} | ${r.wave} | ${r.native_layout_score} | ${r.content_parity_score} | ${r.remediation.slice(0, 80)}${r.remediation.length > 80 ? '…' : ''} |\n`;
}

summary += `
## Reference implementations

- [\`AccountHubView.swift\`](../momentra/momentra/Account/AccountHubView.swift) — \`NavigationStack\` + \`Form\`
- [\`GroupFinanceScreens.swift\`](../momentra/momentra/Shell/GroupActive/GroupFinanceScreens.swift) — \`safeAreaInset\` sticky CTA

## Verification

\`\`\`bash
./scripts/qa/capture-native-layout-screenshot.sh A app_shell ios after
cd momentra && xcodebuild -scheme momentra -destination 'generic/platform=iOS' build
\`\`\`
`;

writeFileSync(OUT_SUMMARY, summary);
console.log(`Wrote ${auditRows.length} rows to ${OUT_CSV}`);
console.log(`Wrote summary to ${OUT_SUMMARY}`);
console.log(`native_layout_score: 0=${scoreCounts[0] ?? 0}, 1=${scoreCounts[1] ?? 0}, 2=${scoreCounts[2] ?? 0}`);
