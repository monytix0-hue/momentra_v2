/**
 * Build iOS vs APK design parity matrix (APK = visual reference).
 *
 * Output:
 *   docs/qa/IOS_APK_DESIGN_PARITY_MATRIX.csv
 *   docs/qa/IOS_APK_THEME_TOKEN_DIFF.md
 *
 * Usage: npx tsx scripts/qa/build-ios-apk-parity-matrix.ts
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync, readdirSync } from 'fs';
import path from 'path';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..');

const APK_UI = path.join(ROOT, 'apk/app/src/main/java/com/example/momentra/ui');
const IOS_ROOT = path.join(ROOT, 'momentra/momentra');
const OUT_CSV = path.join(ROOT, 'docs/qa/IOS_APK_DESIGN_PARITY_MATRIX.csv');
const OUT_TOKENS = path.join(ROOT, 'docs/qa/IOS_APK_THEME_TOKEN_DIFF.md');

type Score = 0 | 1 | 2;
type Overall = 'MATCH' | 'PARTIAL' | 'GAP' | 'MISSING_IOS' | 'INTENTIONAL_DEFER' | 'PENDING';

interface Row {
  context: string;
  moment: string;
  screen: string;
  apk_path: string;
  ios_path: string;
  layout: Score;
  colors: Score;
  typography: Score;
  spacing: Score;
  icons: Score;
  empty_state: Score;
  hero: Score;
  animation: Score;
  overall: Overall;
  gap_notes: string;
  priority: string;
  screenshot_apk: string;
  screenshot_ios: string;
}

const CHROME_IOS: Record<string, string> = {
  MomentraTopBar: 'momentra/momentra/Shell/Components/ShellChrome.swift',
  ContextSwitcher: 'momentra/momentra/Shell/Components/ShellChrome.swift',
  MomentSwitcher: 'momentra/momentra/Shell/Components/ShellChrome.swift',
  ShellBottomNavigation: 'momentra/momentra/Shell/Components/ShellChrome.swift',
  CompanySwitcher: 'momentra/momentra/Shell/Components/ShellChrome.swift',
};

const SPECIAL: Record<string, string> = {
  LoginScreen: 'LoginView',
  SplashScreen: 'LaunchScreenView',
  OnboardingScreen: 'OnboardingView',
  ProductOnboardingScreen: 'ProductOnboardingView',
  ConsentGateScreen: 'ConsentGateView',
  AppLockGate: 'AppLockGateView',
  AccountHubSheet: 'AccountHubView',
  AppShellScreen: 'AppShellView',
  PersonalRecentActivityScreen: 'PersonalRecentActivitySheets',
  CircleComingSoonContent: 'CircleComingSoonView',
  Life360ComingSoon: 'Life360ComingSoonView',
  BusinessQuickAddHub: 'BusinessQuickAddHub',
  GroupQuickAddHub: 'GroupQuickAddHubView',
  PersonalQuickAddHub: 'PersonalQuickAddHubView',
  WeddingQuickAddHub: 'WeddingQuickAddHubView',
  ExperienceQuickAddHub: 'ExperienceQuickAddHubView',
  PurchaseQuickAddHub: 'PurchaseQuickAddHubView',
  LivingQuickAddHub: 'LivingQuickAddHubView',
  CompanyLifeActiveContent: 'BusinessLifeActiveView',
  ShellBottomNavigation: 'ShellBottomNavigationView',
};

const SCREEN_META: Record<string, { context: string; moment: string; screen: string }> = {
  AppShellScreen: { context: 'SHELL', moment: '—', screen: 'App Shell' },
  MomentraTopBar: { context: 'SHELL', moment: '—', screen: 'TopBar' },
  ContextSwitcher: { context: 'SHELL', moment: '—', screen: 'Context Switcher' },
  MomentSwitcher: { context: 'SHELL', moment: '—', screen: 'Moment Switcher' },
  ShellBottomNavigation: { context: 'SHELL', moment: '—', screen: 'Bottom Nav' },
  CompanySwitcher: { context: 'SHELL', moment: '—', screen: 'Company Switcher' },
  SplashScreen: { context: 'AUTH', moment: '—', screen: 'Splash' },
  OnboardingScreen: { context: 'AUTH', moment: '—', screen: 'Cinematic Onboarding' },
  ProductOnboardingScreen: { context: 'AUTH', moment: '—', screen: 'Product Onboarding' },
  ConsentGateScreen: { context: 'AUTH', moment: '—', screen: 'Consent Gate' },
  LoginScreen: { context: 'AUTH', moment: '—', screen: 'Login' },
  AppLockGate: { context: 'AUTH', moment: '—', screen: 'App Lock' },
  AccountHubSheet: { context: 'GLOBAL', moment: '—', screen: 'Account Hub' },
  Life360ComingSoon: { context: 'GLOBAL', moment: '—', screen: 'Life360 Coming Soon' },
  CircleComingSoonContent: { context: 'GLOBAL', moment: '—', screen: 'Circle Coming Soon' },
};

function readText(p: string): string {
  return readFileSync(p, 'utf8');
}

function lineCount(p: string): number {
  return readText(p).split('\n').length;
}

function findIos(name: string): string | null {
  const walk = (dir: string): string | null => {
    for (const ent of readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) {
        const hit = walk(full);
        if (hit) return hit;
      } else if (ent.name === `${name}.swift`) {
        return full;
      }
    }
    return null;
  };
  return walk(IOS_ROOT);
}

function apkToIosName(stem: string): string {
  if (SPECIAL[stem]) return SPECIAL[stem];
  let s = stem;
  s = s.replace(/ActiveContent$/, 'ActiveView');
  s = s.replace(/EmptyContent$/, 'EmptyView');
  s = s.replace(/Screen$/, 'View');
  s = s.replace(/Content$/, 'View');
  return s;
}

function inferMeta(stem: string, rel: string): { context: string; moment: string; screen: string } {
  if (SCREEN_META[stem]) return SCREEN_META[stem];
  const lower = rel.toLowerCase();
  let context = 'UNKNOWN';
  if (lower.includes('/personal/') || lower.includes('personalempty')) context = 'PERSONAL';
  else if (lower.includes('/group/') || lower.includes('groupempty') || lower.includes('groupactive')) context = 'GROUP';
  else if (lower.includes('/business/') || lower.includes('business')) context = 'BUSINESS';
  else if (lower.includes('/auth/') || lower.includes('/onboarding/') || lower.includes('/splash/')) context = 'AUTH';
  else if (lower.includes('/security/')) context = 'AUTH';
  else if (lower.includes('/circle/')) context = 'GLOBAL';

  const tabMatch = stem.match(/(Pulse|Moments|Life|Memory|Create|Setup|Login|Splash|Onboarding|Consent|Lock|Hub)/i);
  const screen = tabMatch ? tabMatch[0] : stem.replace(/(Active|Empty)Content$/, '').replace(/Screen$/, '');

  return { context, moment: stem.replace(/(Active|Empty)Content$/, '').replace(/Screen$/, ''), screen };
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
  if (apk.length === 0 && ios.length === 0) return 1;
  const apkNorm = apk.map((s) => s.toLowerCase().replace(/content|card|section/g, ''));
  const iosNorm = ios.map((s) => s.toLowerCase().replace(/card|section/g, ''));
  let hits = 0;
  for (const a of apkNorm) {
    if (iosNorm.some((i) => i.includes(a.slice(0, 6)) || a.includes(i.slice(0, 6)))) hits++;
  }
  return apk.length ? hits / apk.length : 1;
}

function hasSfSymbolFallback(text: string): boolean {
  return /systemName:\s*"/.test(text) || /Image\(systemName/.test(text);
}

function hasPlusJakarta(text: string): boolean {
  return /plusJakarta|PlusJakarta/.test(text);
}

function scoreLayout(ratio: number, overlap: number): Score {
  if (overlap >= 0.7 || ratio >= 0.85) return 2;
  if (overlap >= 0.45 || ratio >= 0.55) return 1;
  return 0;
}

function extractHexColors(text: string): Set<string> {
  const set = new Set<string>();
  const re = /#([0-9A-Fa-f]{6})/g;
  let m;
  while ((m = re.exec(text)) !== null) set.add(m[1].toUpperCase());
  const re2 = /0x([0-9A-Fa-f]{6,8})/gi;
  while ((m = re2.exec(text)) !== null) {
    const h = m[1].length >= 6 ? m[1].slice(0, 6) : m[1];
    set.add(h.toUpperCase());
  }
  return set;
}

function collectApkScreens(): string[] {
  const out: string[] = [];
  const walk = (dir: string) => {
    for (const ent of readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) walk(full);
      else if (ent.name.endsWith('.kt')) {
        const stem = ent.name.replace('.kt', '');
        if (
          /ActiveContent|EmptyContent|Screen$|ComingSoon|Gate$|Hub$/.test(stem) &&
          !/Theme|Maestro|Test|QuickAddSheets|FinanceScreens|Collab/.test(stem)
        ) {
          out.push(full);
        }
      }
    }
  };
  walk(APK_UI);
  // Shell chrome (not always *Screen)
  const chrome = [
    path.join(APK_UI, 'shell/components/MomentraTopBar.kt'),
    path.join(APK_UI, 'shell/components/ContextSwitcher.kt'),
    path.join(APK_UI, 'shell/components/MomentSwitcher.kt'),
    path.join(APK_UI, 'shell/components/ShellBottomNavigation.kt'),
    path.join(APK_UI, 'shell/components/CompanySwitcher.kt'),
  ];
  for (const c of chrome) if (existsSync(c)) out.push(c);
  return [...new Set(out)].sort();
}

function buildTokenDiff(): string {
  const apkShellPath = path.join(ROOT, 'apk/app/src/main/java/com/example/momentra/ui/theme/ShellTokens.kt');
  const apkShell = readText(apkShellPath);
  const iosShell = readText(path.join(IOS_ROOT, 'Design/Shell/MomentraShellTheme.swift'));

  const apkThemeFiles = readdirSync(path.join(ROOT, 'apk/app/src/main/java/com/example/momentra/ui'), { recursive: true })
    .filter((f): f is string => typeof f === 'string' && f.endsWith('Theme.kt'));
  const iosThemeFiles = readdirSync(IOS_ROOT, { recursive: true })
    .filter((f): f is string => typeof f === 'string' && f.endsWith('Theme.swift'));

  let md = '# iOS vs APK Theme Token Diff\n\n**Date:** auto-generated by `build-ios-apk-parity-matrix.ts`\n\n## ShellTokens vs MomentraShellTheme\n\n| Token | APK (ShellTokens.kt) | iOS (MomentraShellTheme.swift) | Match |\n|-------|----------------------|--------------------------------|-------|\n';
  const pairs: [string, string, string][] = [
    ['TopBarBackground', '0xFF0C0F15', '0x0C0F15'],
    ['BottomBarBackground', '0xFF0C0F15', '0x0C0F15'],
    ['ContextSelectedPersonal', '0xFF7C5CFC', '0x7C5CFC'],
    ['ContextSelectedGroup', '0xFFE8621A', '0xE8621A'],
    ['ContextSelectedBusiness', '0xFF818CF8', '0x818CF8'],
    ['ContextSelectedCircle', '0xFFFC6A8B', '0xE86BA3/FC6A8B'],
    ['BottomUnselected', '0xFFC9C4D8', '0xC9C4D8'],
    ['BottomSelected', '0xFFC9BFFF', '0xC9BFFF'],
    ['CompanyChipBackground', '0xFF1A2030', '0x1A2030'],
    ['StatusOnline', '0xFF10B981', '0x10B981'],
  ];
  for (const [name, apk, ios] of pairs) {
    const match = apkShell.includes(apk.slice(2)) && iosShell.toLowerCase().includes(ios.split('/')[0].slice(2).toLowerCase()) ? 'YES' : 'VERIFY';
    md += `| ${name} | ${apk} | ${ios} | ${match} |\n`;
  }

  md += '\n## Per-family theme files\n\n| APK | iOS | Notes |\n|-----|-----|-------|\n';
  for (const af of apkThemeFiles.slice(0, 40)) {
    const base = path.basename(af as string, '.kt');
    const iosName = base.replace('Theme', 'Theme.swift');
    const iosHit = iosThemeFiles.find((f) => path.basename(f as string) === iosName);
    md += `| ${af} | ${iosHit ?? '—'} | ${iosHit ? 'paired' : 'MISSING_IOS'} |\n`;
  }
  return md;
}

function csvEscape(s: string): string {
  if (s.includes(',') || s.includes('"') || s.includes('\n')) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

function overallFromScores(scores: Score[], notes: string): Overall {
  if (notes.includes('INTENTIONAL_DEFER')) return 'INTENTIONAL_DEFER';
  const avg = scores.reduce((a, b) => a + b, 0) / scores.length;
  if (avg >= 1.85) return 'MATCH';
  if (avg >= 1.2) return 'PARTIAL';
  return 'GAP';
}

const rows: Row[] = [];

for (const apkPath of collectApkScreens()) {
  const stem = path.basename(apkPath, '.kt');
  const rel = path.relative(APK_UI, apkPath);
  const meta = inferMeta(stem, rel);
  const iosName = apkToIosName(stem);
  let iosPath: string | null = null;
  if (CHROME_IOS[stem]) {
    iosPath = path.join(ROOT, CHROME_IOS[stem]);
  } else {
    iosPath = findIos(iosName);
  }
  const apkText = readText(apkPath);
  const apkLines = lineCount(apkPath);

  if (!iosPath) {
    if (stem === 'HomeScreen') continue;
    rows.push({
      context: meta.context,
      moment: meta.moment,
      screen: meta.screen,
      apk_path: path.relative(ROOT, apkPath),
      ios_path: '',
      layout: 0,
      colors: 0,
      typography: 0,
      spacing: 0,
      icons: 0,
      empty_state: 0,
      hero: 0,
      animation: 0,
      overall: 'MISSING_IOS',
      gap_notes: `No iOS counterpart for ${iosName}`,
      priority: 'P2',
      screenshot_apk: '',
      screenshot_ios: '',
    });
    continue;
  }

  const iosText = readText(iosPath);
  const iosLines = lineCount(iosPath);
  const ratio = iosLines / apkLines;
  const apkSections = extractApkSections(apkText);
  const iosSections = extractIosSections(iosText);
  const overlap = sectionOverlap(apkSections, iosSections);

  let layout = scoreLayout(ratio, overlap);
  let colors: Score = 2;
  const apkColors = extractHexColors(apkText);
  const iosColors = extractHexColors(iosText);
  const sharedColors = [...apkColors].filter((c) => iosColors.has(c)).length;
  if (apkColors.size > 3 && sharedColors / apkColors.size < 0.5) colors = 1;
  if (apkColors.size > 3 && sharedColors / apkColors.size < 0.25) colors = 0;

  let typography: Score = hasPlusJakarta(iosText) || hasPlusJakarta(apkText) ? (hasPlusJakarta(iosText) ? 2 : 1) : 1;
  let spacing: Score = ratio < 0.55 ? 1 : 2;
  let icons: Score = hasSfSymbolFallback(iosText) && !stem.includes('Shell') ? 1 : 2;
  let empty_state: Score = /ProgressView|CircularProgress|loading/.test(iosText) ? 2 : 1;
  let hero: Score = /Hero|hero|HubHero|Gradient/.test(apkText)
    ? /Hero|hero|HubHero|Gradient/.test(iosText)
      ? 2
      : 1
    : 2;
  let animation: Score = /Animation|animate|particle|Animated/.test(apkText)
    ? /Animation|animate|particle|withAnimation/.test(iosText)
      ? 2
      : 1
    : 2;

  let gap_notes = '';
  let priority = 'P3';
  if (ratio < 0.65) {
    gap_notes += `THIN_IOS: APK ${apkLines} vs iOS ${iosLines} lines. `;
    priority = 'P1';
  }
  if (overlap < 0.5 && apkSections.length > 2) {
    gap_notes += `Section overlap ${(overlap * 100).toFixed(0)}% (APK ${apkSections.length} composables, iOS ${iosSections.length} sections). `;
  }
  if (hasSfSymbolFallback(iosText) && stem.includes('Shell') === false) {
    gap_notes += 'SF Symbol fallbacks present on iOS. ';
  }

  // Manual overrides from code audit
  if (stem === 'ShellBottomNavigation') {
    layout = 2;
    icons = 2;
    spacing = 2;
    colors = 2;
    typography = 2;
    empty_state = 2;
    hero = 2;
    animation = 2;
    gap_notes = 'ShellBottomNavigationView mirrors APK custom bar + accent FAB (post-remediation)';
    priority = 'P0';
  }
  if (stem === 'PersonalLifestylePulseActiveContent') {
    gap_notes += 'APK router does not wire Lifestyle pulse; iOS wires PersonalLifestylePulseActiveView. ';
  }

  const scores = [layout, colors, typography, spacing, icons, empty_state, hero, animation];
  const overall = overallFromScores(scores, gap_notes);

  rows.push({
    context: meta.context,
    moment: meta.moment,
    screen: meta.screen,
    apk_path: path.relative(ROOT, apkPath),
    ios_path: path.relative(ROOT, iosPath),
    layout,
    colors,
    typography,
    spacing,
    icons,
    empty_state,
    hero,
    animation,
    overall,
    gap_notes: gap_notes.trim(),
    priority,
    screenshot_apk: `docs/qa/screenshots/ios-apk-parity/apk/${stem}.png`,
    screenshot_ios: `docs/qa/screenshots/ios-apk-parity/ios/${iosName}.png`,
  });
}

mkdirSync(path.dirname(OUT_CSV), { recursive: true });
mkdirSync(path.join(ROOT, 'docs/qa/screenshots/ios-apk-parity/apk'), { recursive: true });
mkdirSync(path.join(ROOT, 'docs/qa/screenshots/ios-apk-parity/ios'), { recursive: true });

const header =
  'context,moment,screen,apk_path,ios_path,layout,colors,typography,spacing,icons,empty_state,hero,animation,overall,gap_notes,priority,screenshot_apk,screenshot_ios';
const csv = [header, ...rows.map((r) => Object.values(r).map((v) => csvEscape(String(v))).join(','))].join('\n');
writeFileSync(OUT_CSV, csv + '\n');
writeFileSync(OUT_TOKENS, buildTokenDiff());

console.log(`Wrote ${rows.length} rows to ${OUT_CSV}`);
console.log(`Wrote token diff to ${OUT_TOKENS}`);
console.log(
  'Summary:',
  Object.entries(
    rows.reduce((acc, r) => {
      acc[r.overall] = (acc[r.overall] ?? 0) + 1;
      return acc;
    }, {} as Record<string, number>)
  )
    .map(([k, v]) => `${k}=${v}`)
    .join(', ')
);
