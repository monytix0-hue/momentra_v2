/**
 * Cross-platform parity for the India States & UTs registry.
 *
 * The picker is bundled on each client so it opens instantly offline, which means three copies of
 * the same table exist: the backend module, the Android catalog, and the iOS catalog. A drift
 * between them is a silent GST bug — a state the phone can pick but the server cannot resolve
 * becomes an untaxed invoice.
 *
 * This test reads the two client source files and compares their `(code, name)` pairs against the
 * server table, so a one-sided edit fails here instead of in production.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { GST_STATES } from '../src/modules/business/operating-gst-states';

const REPO_ROOT = join(__dirname, '..', '..', '..');
const ANDROID_CATALOG = join(
  REPO_ROOT,
  'apk/app/src/main/java/com/example/momentra/ui/shell/business/shared/IndiaStateCatalog.kt'
);
const IOS_CATALOG = join(REPO_ROOT, 'momentra/momentra/Design/IndiaStateCatalog.swift');

/** `IndiaState("27", "Maharashtra", false)` — Android. */
const ANDROID_ROW = /IndiaState\(\s*"(\d{2})"\s*,\s*"([^"]+)"\s*,\s*(true|false)\s*\)/g;
/** `IndiaState(code: "27", name: "Maharashtra", isUnionTerritory: false)` — iOS. */
const IOS_ROW = /IndiaState\(\s*code:\s*"(\d{2})"\s*,\s*name:\s*"([^"]+)"\s*,\s*isUnionTerritory:\s*(true|false)\s*\)/g;

interface ParsedRow {
  code: string;
  name: string;
  isUnionTerritory: boolean;
}

function parseAll(pattern: RegExp, source: string): ParsedRow[] {
  const rows: ParsedRow[] = [];
  for (const match of source.matchAll(pattern)) {
    rows.push({ code: match[1], name: match[2], isUnionTerritory: match[3] === 'true' });
  }
  return rows;
}

function readCatalog(path: string, pattern: RegExp): ParsedRow[] {
  const rows = parseAll(pattern, readFileSync(path, 'utf8'));
  // A parse that silently matched nothing would make the comparison vacuously true.
  assert.ok(rows.length > 0, `no IndiaState rows parsed from ${path}`);
  return rows;
}

function expectedRows(): ParsedRow[] {
  return GST_STATES.map((state) => ({
    code: state.code,
    name: state.name,
    isUnionTerritory: state.type === 'UT',
  }));
}

function compare(label: string, actual: ParsedRow[]): void {
  const expected = expectedRows();
  assert.equal(actual.length, expected.length, `${label} has ${actual.length} rows, expected ${expected.length}`);

  const sorted = [...actual].sort((a, b) => a.code.localeCompare(b.code));
  for (const want of expected) {
    const got = sorted.find((row) => row.code === want.code);
    assert.ok(got, `${label} is missing state code ${want.code} (${want.name})`);
    assert.equal(got.name, want.name, `${label} names ${want.code} "${got.name}", expected "${want.name}"`);
    assert.equal(
      got.isUnionTerritory,
      want.isUnionTerritory,
      `${label} marks ${want.code} ${got.isUnionTerritory ? 'a UT' : 'a state'}, expected ${want.isUnionTerritory ? 'a UT' : 'a state'}`
    );
  }
}

describe('India state catalog parity across backend, Android, and iOS', () => {
  it('Android lists exactly the backend table', () => {
    compare('IndiaStateCatalog.kt', readCatalog(ANDROID_CATALOG, ANDROID_ROW));
  });

  it('iOS lists exactly the backend table', () => {
    compare('IndiaStateCatalog.swift', readCatalog(IOS_CATALOG, IOS_ROW));
  });

  it('all three agree on the retired code 25', () => {
    for (const [label, rows] of [
      ['backend', expectedRows()],
      ['IndiaStateCatalog.kt', readCatalog(ANDROID_CATALOG, ANDROID_ROW)],
      ['IndiaStateCatalog.swift', readCatalog(IOS_CATALOG, IOS_ROW)],
    ] as const) {
      assert.equal(
        rows.some((row) => row.code === '25'),
        false,
        `${label} must not carry retired code 25`
      );
    }
  });

  it('the backend table itself has 37 unique codes', () => {
    const rows = expectedRows();
    assert.equal(rows.length, 37);
    assert.equal(new Set(rows.map((row) => row.code)).size, 37);
  });
});
