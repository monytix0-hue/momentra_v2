// Shared helpers for the Momentra design pipeline (tokens + assets).
// Pure Node (>=20), no dependencies.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const PIPELINE_DIR = path.dirname(fileURLToPath(import.meta.url));
export const REPO_ROOT = path.resolve(PIPELINE_DIR, '..', '..');
export const rel = (p) => path.relative(REPO_ROOT, p).split(path.sep).join('/');
export const repo = (...parts) => path.join(REPO_ROOT, ...parts);

export const PATHS = {
  tokens: repo('design', 'tokens', 'personal.tokens.json'),
  businessTokens: repo('design', 'tokens', 'business.tokens.json'),
  taxonomy: repo('design', 'taxonomy', 'personal-taxonomy.v1.json'),
  assetManifest: repo('design', 'assets', 'personal', 'manifest.json'),
  assetDir: repo('design', 'assets', 'personal'),
  businessAssetManifest: repo('design', 'assets', 'business', 'manifest.json'),
  businessAssetDir: repo('design', 'assets', 'business'),
  iosApp: repo('momentra', 'momentra'),
  iosAssets: repo('momentra', 'momentra', 'Assets.xcassets'),
  iosGenerated: repo('momentra', 'momentra', 'Design', 'Generated'),
  androidRes: repo('apk', 'app', 'src', 'main', 'res'),
  androidGenerated: repo('apk', 'app', 'src', 'main', 'java', 'com', 'example', 'momentra', 'ui', 'theme', 'generated'),
  backendGenerated: repo('backend', 'typescript', 'src', 'modules', 'personal'),
  seeds: repo('frds', 'seeds', 'personal'),
};

export const readJson = (p) => JSON.parse(fs.readFileSync(p, 'utf8'));

/** Collects planned writes so the run can either apply them or verify them (--check). */
export class Plan {
  constructor({ check }) {
    this.check = check;
    this.files = new Map(); // abs path -> Buffer
    this.ownedDirs = []; // { dir, match(relName) } files under these that are not planned are stale
    this.changed = [];
    this.removed = [];
  }
  text(p, s) { this.files.set(path.resolve(p), Buffer.from(s.replace(/\r\n/g, '\n'), 'utf8')); }
  json(p, obj) { this.text(p, JSON.stringify(obj, null, 2) + '\n'); }
  bytes(p, buf) { this.files.set(path.resolve(p), buf); }
  own(dir, match) { this.ownedDirs.push({ dir: path.resolve(dir), match }); }

  static sameContent(abs, buf) {
    if (!fs.existsSync(abs)) return false;
    const cur = fs.readFileSync(abs);
    if (cur.equals(buf)) return true;
    // Treat CRLF/LF differences as equal for text (git core.autocrlf on Windows).
    const isText = !buf.includes(0);
    if (isText) return cur.toString('utf8').replace(/\r\n/g, '\n') === buf.toString('utf8');
    return false;
  }

  apply() {
    for (const [abs, buf] of this.files) {
      if (Plan.sameContent(abs, buf)) continue;
      this.changed.push(rel(abs));
      if (!this.check) {
        fs.mkdirSync(path.dirname(abs), { recursive: true });
        fs.writeFileSync(abs, buf);
      }
    }
    for (const { dir, match } of this.ownedDirs) {
      if (!fs.existsSync(dir)) continue;
      for (const f of walk(dir)) {
        const r = path.relative(dir, f).split(path.sep).join('/');
        if (!match(r) || this.files.has(path.resolve(f))) continue;
        this.removed.push(rel(f));
        if (!this.check) fs.rmSync(f);
      }
      if (!this.check) pruneEmptyDirs(dir);
    }
    return this.changed.length + this.removed.length;
  }

  report(label) {
    const n = this.changed.length + this.removed.length;
    if (this.check) {
      if (n === 0) { console.log(`[${label}] OK: ${this.files.size} generated files are up to date.`); return 0; }
      console.error(`[${label}] OUT OF DATE (${n}). Run \`npm run ${label}\` in tools/design-pipeline.`);
      for (const c of this.changed) console.error(`  ~ ${c}`);
      for (const r of this.removed) console.error(`  - ${r} (stale)`);
      return 1;
    }
    console.log(`[${label}] ${this.files.size} files planned, ${this.changed.length} written, ${this.removed.length} stale removed.`);
    for (const c of this.changed) console.log(`  ~ ${c}`);
    for (const r of this.removed) console.log(`  - ${r}`);
    return 0;
  }
}

export function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) yield* walk(p);
    else yield p;
  }
}

function pruneEmptyDirs(dir) {
  if (!fs.existsSync(dir)) return;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.isDirectory()) pruneEmptyDirs(path.join(dir, e.name));
  }
  if (fs.readdirSync(dir).length === 0) fs.rmdirSync(dir);
}

// ---------- naming ----------
export const camel = (parts) => parts
  .flatMap((p) => String(p).split(/[^A-Za-z0-9]+/))
  .filter(Boolean)
  .map((w, i) => (i === 0 ? w[0].toLowerCase() + w.slice(1) : w[0].toUpperCase() + w.slice(1)))
  .join('');
export const pascal = (parts) => { const c = camel(parts); return c[0].toUpperCase() + c.slice(1); };
export const snake = (s) => String(s).replace(/[^A-Za-z0-9]+/g, '_').replace(/^_|_$/g, '').toLowerCase();

// ---------- colors ----------
/** Parses "#RRGGBB", "#RRGGBBAA" or "rgba(r,g,b,a)" into {r,g,b,a} (0-255, a 0-1). */
export function parseColor(v) {
  let m = /^#([0-9a-f]{6})([0-9a-f]{2})?$/i.exec(v);
  if (m) {
    const n = parseInt(m[1], 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255, a: m[2] ? parseInt(m[2], 16) / 255 : 1 };
  }
  m = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)$/i.exec(v);
  if (m) return { r: +m[1], g: +m[2], b: +m[3], a: m[4] === undefined ? 1 : +m[4] };
  throw new Error(`Unsupported color value: ${v}`);
}
export const hex2 = (n) => n.toString(16).toUpperCase().padStart(2, '0');
export const argbHex = ({ r, g, b, a }) => `0x${hex2(Math.round(a * 255))}${hex2(r)}${hex2(g)}${hex2(b)}`;
export const rgbHex = ({ r, g, b }) => `#${hex2(r)}${hex2(g)}${hex2(b)}`;
export const fmt = (n, d = 3) => Number(n.toFixed(d)).toString();
