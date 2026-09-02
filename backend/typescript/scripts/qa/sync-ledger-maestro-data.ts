/**
 * S9-QA-B — Sync 3500 certification ledger CSVs into platform shards + joined Maestro rows.
 *
 * Inputs (source of truth copies):
 *   .maestro/data/android/maestro_input_3500.csv
 *   .maestro/data/ios/maestro_input_3500.csv
 *   .maestro/input-catalog/catalog.json
 *   .maestro/input-catalog/ledger-join-map.json
 *
 * Outputs:
 *   .maestro/data/{android,ios}/{personal,group,business}.csv
 *   .maestro/data/{android,ios}/joined_3500.csv
 *   .maestro/data/join-report.json
 *   .maestro/data/pilot/{android,ios}_pilot_150.csv
 *
 * Usage:
 *   QA_FIXTURES_ENABLED=true npx tsx scripts/qa/sync-ledger-maestro-data.ts
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';
import { assertQaFixturesSafe } from './qa-env-guard';

type Platform = 'android' | 'ios';

interface JoinHub {
  catalogQuickAdd: string;
  tileMaestroId: string | null;
  categoryField: string | null;
  notes: string;
}

interface JoinMap {
  momentMap: Record<
    string,
    { momentId: string; momentTypeCode: string; context: string; catalogLabel?: string }
  >;
  semanticToHub: Record<string, Record<string, Record<string, JoinHub>>>;
  splitMethodMap: Record<string, string>;
  fieldDefaults: Record<string, Record<string, Record<string, string | null>>>;
  pilotBatches: string[];
  knownGaps: string[];
  overrides?: Array<{
    when: { momentId: string; semantic: string };
    joinStatus: string;
    catalogQuickAdd?: string;
    notes?: string;
  }>;
}

interface CatalogQuickAdd {
  id: string;
  momentId: string;
  quickAdd: string;
  status: string;
  tileMaestroId: string | null;
  androidSupported: boolean;
  iosSupported: boolean;
}

function parseCsv(text: string): Record<string, string>[] {
  const lines: string[] = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '"') {
      if (inQuotes && text[i + 1] === '"') {
        cur += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if ((ch === '\n' || ch === '\r') && !inQuotes) {
      if (ch === '\r' && text[i + 1] === '\n') i++;
      if (cur.length || lines.length === 0) lines.push(cur);
      cur = '';
    } else {
      cur += ch;
    }
  }
  if (cur.length) lines.push(cur);

  if (!lines.length) return [];
  const headers = splitCsvLine(lines[0]);
  return lines.slice(1).filter(Boolean).map((line) => {
    const cols = splitCsvLine(line);
    const row: Record<string, string> = {};
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
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === ',' && !inQuotes) {
      out.push(cur);
      cur = '';
    } else {
      cur += ch;
    }
  }
  out.push(cur);
  return out;
}

function toCsv(rows: Record<string, string>[], headers: string[]): string {
  const esc = (v: string) => {
    if (v == null) return '';
    const s = String(v);
    if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
    return s;
  };
  return [headers.join(','), ...rows.map((r) => headers.map((h) => esc(r[h] ?? '')).join(','))].join(
    '\n'
  ) + '\n';
}

function resolveHub(
  join: JoinMap,
  mode: string,
  momentId: string,
  semantic: string
): JoinHub | null {
  const byMode = join.semanticToHub[mode];
  if (!byMode) return null;
  const bySemantic = byMode[semantic];
  if (!bySemantic) return null;
  if (bySemantic[momentId]) return bySemantic[momentId];
  if (bySemantic['*']) return bySemantic['*'];
  return null;
}

function enrich(
  platform: Platform,
  row: Record<string, string>,
  join: JoinMap,
  catalogById: Map<string, CatalogQuickAdd>
): Record<string, string> {
  const momentName = row.Moment;
  const mode = row.Mode;
  const semantic = row.Semantic_Type;
  const mm = join.momentMap[momentName];
  const modeKey = mode.toUpperCase();
  const hubResolved = mm ? resolveHub(join, modeKey, mm.momentId, semantic) : null;

  const catalogId =
    hubResolved && mm ? `${mm.momentId}:${hubResolved.catalogQuickAdd}` : '';
  const catalog = catalogId ? catalogById.get(catalogId) : undefined;

  const fields = join.fieldDefaults[modeKey]?.[semantic] ?? {};
  const splitId = row.Split_Method ? join.splitMethodMap[row.Split_Method] ?? '' : '';

  const override =
    mm &&
    join.overrides?.find((o) => o.when.momentId === mm.momentId && o.when.semantic === semantic);

  let joinStatus = 'JOINED';
  let joinNotes = hubResolved?.notes ?? '';
  if (override) {
    joinStatus = override.joinStatus;
    joinNotes = override.notes ?? joinNotes;
  } else if (!mm) {
    joinStatus = 'UNMAPPED_MOMENT';
    joinNotes = `Unknown moment label: ${momentName}`;
  } else if (!hubResolved) {
    joinStatus = 'UNMAPPED_SEMANTIC';
    joinNotes = `No hub for ${modeKey}/${mm.momentId}/${semantic}`;
  } else if (catalog?.status === 'CAPABILITY_GAP') {
    joinStatus = 'SKIP_CAPABILITY_GAP';
  } else if (catalog?.status === 'DEFERRED') {
    joinStatus = 'SKIP_DEFERRED';
  } else if (catalog?.status === 'BROKEN') {
    joinStatus = 'FAIL_BROKEN';
  } else if (!catalog) {
    joinStatus = 'CATALOG_MISS';
    joinNotes = `${joinNotes} | catalog id ${catalogId} not found`.trim();
  } else if (joinNotes.includes('REVIEW')) {
    joinStatus = 'JOINED_REVIEW';
  }

  const platformSupported =
    platform === 'android' ? catalog?.androidSupported !== false : catalog?.iosSupported !== false;
  if (catalog && !platformSupported && joinStatus.startsWith('JOINED')) {
    joinStatus = 'SKIP_PLATFORM';
  }

  return {
    ...row,
    Platform: platform,
    catalog_moment_id: mm?.momentId ?? '',
    catalog_moment_type: mm?.momentTypeCode ?? '',
    catalog_quick_add: hubResolved?.catalogQuickAdd ?? '',
    catalog_id: catalogId,
    catalog_status: catalog?.status ?? '',
    tile_maestro_id: hubResolved?.tileMaestroId ?? catalog?.tileMaestroId ?? '',
    amount_maestro_id: fields.amountId ?? '',
    note_maestro_id: fields.noteId ?? '',
    submit_maestro_id: fields.submitId ?? '',
    split_maestro_id: splitId,
    category_value: row.Quick_Add,
    correlation_note: row.Description,
    join_status: joinStatus,
    join_notes: joinNotes,
  };
}

function ensureDir(p: string) {
  if (!existsSync(p)) mkdirSync(p, { recursive: true });
}

function processPlatform(
  platform: Platform,
  join: JoinMap,
  catalogById: Map<string, CatalogQuickAdd>,
  dataRoot: string
) {
  const src = path.join(dataRoot, platform, 'maestro_input_3500.csv');
  if (!existsSync(src)) throw new Error(`Missing ${src}`);
  const rows = parseCsv(readFileSync(src, 'utf8'));
  const joined = rows.map((r) => enrich(platform, r, join, catalogById));

  const headers = Object.keys(joined[0] ?? {});
  writeFileSync(path.join(dataRoot, platform, 'joined_3500.csv'), toCsv(joined, headers), 'utf8');

  const byMode: Record<string, Record<string, string>[]> = {
    personal: [],
    group: [],
    business: [],
  };
  for (const r of joined) {
    const key = r.Mode.toLowerCase();
    if (byMode[key]) byMode[key].push(r);
  }
  for (const [key, list] of Object.entries(byMode)) {
    writeFileSync(path.join(dataRoot, platform, `${key}.csv`), toCsv(list, headers), 'utf8');
  }

  const pilot = joined.filter((r) => join.pilotBatches.includes(r.Run_Batch));
  const pilotDir = path.join(dataRoot, 'pilot');
  ensureDir(pilotDir);
  writeFileSync(path.join(pilotDir, `${platform}_pilot_150.csv`), toCsv(pilot, headers), 'utf8');

  const statusCounts: Record<string, number> = {};
  for (const r of joined) {
    statusCounts[r.join_status] = (statusCounts[r.join_status] ?? 0) + 1;
  }

  return {
    platform,
    total: joined.length,
    byMode: {
      personal: byMode.personal.length,
      group: byMode.group.length,
      business: byMode.business.length,
    },
    pilot: pilot.length,
    statusCounts,
  };
}

function main() {
  assertQaFixturesSafe('sync-ledger-maestro-data');

  const repoRoot = path.resolve(__dirname, '../../../..');
  const dataRoot = path.join(repoRoot, '.maestro', 'data');
  const catalogPath = path.join(repoRoot, '.maestro', 'input-catalog', 'catalog.json');
  const joinPath = path.join(repoRoot, '.maestro', 'input-catalog', 'ledger-join-map.json');

  const catalog = JSON.parse(readFileSync(catalogPath, 'utf8')) as {
    quickAdds: CatalogQuickAdd[];
  };
  const join = JSON.parse(readFileSync(joinPath, 'utf8')) as JoinMap;

  const catalogById = new Map(catalog.quickAdds.map((q) => [q.id, q]));

  // Fix resolveHub for Mode casing — enrich already uses modeKey
  const android = processPlatform('android', join, catalogById, dataRoot);
  const ios = processPlatform('ios', join, catalogById, dataRoot);

  const report = {
    gate: 'S9-QA-B',
    generatedAt: new Date().toISOString(),
    sources: {
      androidLedger: 'docs/qa/ledgers/Momentra_APK_3500_Certification_Ledger.xlsx',
      iosLedger: 'docs/qa/ledgers/Momentra_IOS_3500_Certification_Ledger.xlsx',
      joinMap: '.maestro/input-catalog/ledger-join-map.json',
      catalog: '.maestro/input-catalog/catalog.json',
    },
    knownGaps: join.knownGaps,
    android,
    ios,
  };

  writeFileSync(path.join(dataRoot, 'join-report.json'), JSON.stringify(report, null, 2) + '\n', 'utf8');
  writeFileSync(
    path.join(dataRoot, 'schema.md'),
    `# Maestro ledger data (S9-QA-B)

## Source of truth

Excel ledgers in \`docs/qa/ledgers/\`:

- \`Momentra_APK_3500_Certification_Ledger.xlsx\`
- \`Momentra_IOS_3500_Certification_Ledger.xlsx\`

CSV exports under \`.maestro/data/\` are **generated** — regenerate after editing Excel:

\`\`\`powershell
cd backend\\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:sync-ledger-data
\`\`\`

## Files

| Path | Rows | Purpose |
|------|-----:|---------|
| \`android/maestro_input_3500.csv\` | 3500 | Flat pack export |
| \`ios/maestro_input_3500.csv\` | 3500 | Flat pack export |
| \`{platform}/joined_3500.csv\` | 3500 | + catalog join columns |
| \`{platform}/personal.csv\` | ~1167 | Mode=Personal |
| \`{platform}/group.csv\` | ~1167 | Mode=Group |
| \`{platform}/business.csv\` | ~1166 | Mode=Business |
| \`pilot/{platform}_pilot_150.csv\` | 150 | Run_Batch B01–B03 (S9-QA-E) |

## Join columns added

\`catalog_moment_id\`, \`catalog_quick_add\`, \`catalog_id\`, \`catalog_status\`, \`tile_maestro_id\`, \`amount_maestro_id\`, \`note_maestro_id\`, \`submit_maestro_id\`, \`split_maestro_id\`, \`category_value\`, \`correlation_note\`, \`join_status\`, \`join_notes\`

## Architecture

\`catalog.json\` = capabilities · Excel = scenarios · CSV = Maestro exports · \`qa:verify\` = backend truth
`,
    'utf8'
  );

  console.log('[sync-ledger-maestro-data] android', android.statusCounts);
  console.log('[sync-ledger-maestro-data] ios', ios.statusCounts);
  console.log(`[sync-ledger-maestro-data] wrote report → ${path.join(dataRoot, 'join-report.json')}`);
}

main();
