#!/usr/bin/env node
// Momentra Personal v1.1 asset importer: icons + artwork for iOS and Android.
//
//   node import-assets.mjs           import / refresh every asset (idempotent; only changed files are written)
//   node import-assets.mjs --check   verify everything is imported and up to date (exit 1 if not)
//
// Icons
//   Source: the pinned `lucide-static` package (ISC). Keys = design/assets/personal/manifest.json `icons`
//   + every icon referenced by design/taxonomy/personal-taxonomy.v1.json (areas, subcategories, moods).
//   Geometry is normalised to the Figma spec: 24x24 viewBox, stroke 1.7, round caps and joins, no fill.
//   iOS      Assets.xcassets/PersonalIcons/<key>.imageset/<key>.svg   (template rendering, preserves vector)
//   Android  res/drawable/ic_pers_<key_snake>.xml                     (VectorDrawable via svg2vectordrawable,
//            white stroke, tint at the call site like ic_nav_*)
//   Swift / Kotlin key indexes: PersonalIcons.generated.swift, PersonalAssets.kt
//
// Artwork (Figma image fills, not personal photos)
//   Source: design/assets/personal/artwork/<key>.jpg (masters, >= 1024 px)
//   iOS      Assets.xcassets/PersonalArtwork/<key>.imageset  (@3x = master bytes, @2x = 2/3 resize, JPEG)
//   Android  res/drawable-nodpi/<key>.webp                    (WebP, master size)
//   Raster outputs are recorded in assets.lock.json so re-runs on any OS do not re-encode unchanged art.
//
// Exact duplicates: an icon whose geometry, or an artwork whose bytes, already exist elsewhere in the
// iOS asset catalog / Android res is reported; manifest-only icons that duplicate are skipped.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { PATHS, PIPELINE_DIR, Plan, readJson, rel, snake, camel, walk } from './lib.mjs';

const require = createRequire(import.meta.url);
const check = process.argv.includes('--check');
const plan = new Plan({ check });
const manifest = readJson(PATHS.assetManifest);
const taxonomy = readJson(PATHS.taxonomy);
const STROKE = manifest.lucide.strokeWidth;
const sha = (b) => crypto.createHash('sha256').update(b).digest('hex');

let svg2vd, sharp, lucideNodes, lucideVersion;
try {
  svg2vd = require('svg2vectordrawable');
  sharp = require('sharp');
  lucideNodes = require('lucide-static/icon-nodes.json');
  lucideVersion = require('lucide-static/package.json').version;
} catch (e) {
  console.error('Missing dependencies. Run `npm ci` in tools/design-pipeline first.\n' + e.message);
  process.exit(2);
}

// ------------------------------------------------------------ icon key set
const uses = new Map();
const addUse = (key, use) => { if (!uses.has(key)) uses.set(key, new Set()); uses.get(key).add(use); };
for (const i of manifest.icons) addUse(i.key, 'ui');
for (const a of taxonomy.areas) addUse(a.icon, `area ${a.code}`);
for (const s of taxonomy.subcategories) addUse(s.icon, `subcategory ${s.code}`);
for (const m of taxonomy.moods) addUse(m.icon, `mood ${m.code}`);
const keys = [...uses.keys()].sort();
const missing = keys.filter((k) => !lucideNodes[k]);
if (missing.length) { console.error(`Unknown Lucide icon key(s) in lucide-static@${lucideVersion}: ${missing.join(', ')}`); process.exit(2); }
const taxonomyKeys = new Set([...taxonomy.areas, ...taxonomy.subcategories, ...taxonomy.moods].map((x) => x.icon));

// ------------------------------------------------------------ SVG building
const fmtAttr = (v) => String(v).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
function svgFor(key, stroke) {
  const els = lucideNodes[key].map(([tag, attrs]) => {
    // Lucide draws a few dots as filled circles (fill="currentColor"); keep them filled in the ink color.
    const fill = attrs.fill && attrs.fill !== 'none' ? stroke : 'none';
    const a = Object.entries(attrs).filter(([k]) => k !== 'key' && k !== 'fill').map(([k, v]) => `${k}="${fmtAttr(v)}"`).join(' ');
    return `  <${tag} ${a} fill="${fill}" stroke="${stroke}" stroke-width="${STROKE}" stroke-linecap="round" stroke-linejoin="round"/>`;
  });
  return `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">\n${els.join('\n')}\n</svg>\n`;
}
const geometry = (key) => lucideNodes[key].map(([tag, attrs]) => `${tag}:${Object.entries(attrs).filter(([k]) => k !== 'key').sort().map(([k, v]) => `${k}=${v}`).join(',')}`).sort().join('|');

// ------------------------------------------------------------ duplicate scan of existing (non-pipeline) assets
const iosIconDir = path.join(PATHS.iosAssets, 'PersonalIcons');
const iosArtDir = path.join(PATHS.iosAssets, 'PersonalArtwork');
const drawableDir = path.join(PATHS.androidRes, 'drawable');
const nodpiDir = path.join(PATHS.androidRes, 'drawable-nodpi');
const isOwnedIos = (f) => f.startsWith(iosIconDir + path.sep) || f.startsWith(iosArtDir + path.sep) ||
  f.startsWith(path.join(PATHS.iosAssets, 'BusinessIcons') + path.sep) ||
  f.includes(`${path.sep}PersonalTokens${path.sep}`) || f.includes(`${path.sep}BusinessTokens${path.sep}`);
const isOwnedAndroid = (f) => /^(ic_pers_|art_pers_|ic_biz_)/.test(path.basename(f));
const existingGeom = new Map(); // normalised path-data signature -> file
const existingRaster = new Map(); // sha -> file
const normD = (d) => d.replace(/,/g, ' ').replace(/\s+/g, ' ').trim();
for (const f of walk(PATHS.iosAssets)) {
  if (isOwnedIos(f)) continue;
  if (f.endsWith('.svg')) {
    const ds = [...fs.readFileSync(f, 'utf8').matchAll(/\sd="([^"]+)"/g)].map((m) => normD(m[1])).sort().join('|');
    if (ds) existingGeom.set(ds, rel(f));
  } else if (/\.(png|jpe?g|webp|heic)$/i.test(f)) existingRaster.set(sha(fs.readFileSync(f)), rel(f));
}
for (const dir of fs.readdirSync(PATHS.androidRes).filter((d) => d.startsWith('drawable')).map((d) => path.join(PATHS.androidRes, d))) {
  for (const f of walk(dir)) {
    if (isOwnedAndroid(f)) continue;
    if (f.endsWith('.xml')) {
      const ds = [...fs.readFileSync(f, 'utf8').matchAll(/android:pathData="([^"]+)"/g)].map((m) => normD(m[1])).sort().join('|');
      if (ds) existingGeom.set(ds, rel(f));
    } else if (/\.(png|jpe?g|webp)$/i.test(f)) existingRaster.set(sha(fs.readFileSync(f)), rel(f));
  }
}
const pathOnlySig = (key) => lucideNodes[key].every(([t]) => t === 'path') ? lucideNodes[key].map(([, a]) => normD(a.d)).sort().join('|') : null;
const duplicates = [];
const skipped = new Set();
for (const key of keys) {
  const sig = pathOnlySig(key);
  const hit = sig && existingGeom.get(sig);
  if (!hit) continue;
  duplicates.push(`${key} == ${hit}`);
  if (!taxonomyKeys.has(key)) skipped.add(key);
}
const iconKeys = keys.filter((k) => !skipped.has(k));

// ------------------------------------------------------------ icons: iOS + Android
plan.own(iosIconDir, () => true);
plan.json(path.join(iosIconDir, 'Contents.json'), { info: { author: 'xcode', version: 1 }, properties: { 'provides-namespace': true } });
plan.own(drawableDir, (r) => /^ic_pers_.*\.xml$/.test(r));
// Business drawables are owned per-key only: the repo also ships hand-authored ic_biz_* glyphs
// for the create flow and memory timeline, which this pipeline must never delete.
const businessManifestEarly = readJson(PATHS.businessAssetManifest);
const ownedBizDrawables = new Set(businessManifestEarly.icons.map((i) => `ic_biz_${snake(i.key)}.xml`));
plan.own(drawableDir, (r) => ownedBizDrawables.has(r));
const vdHeader = (key) => `<!-- GENERATED by tools/design-pipeline/import-assets.mjs from lucide-static@${lucideVersion} "${key}" (ISC). Do not edit. -->\n`;
for (const key of iconKeys) {
  const set = path.join(iosIconDir, `${key}.imageset`);
  plan.text(path.join(set, `${key}.svg`), svgFor(key, '#000000'));
  plan.json(path.join(set, 'Contents.json'), {
    images: [{ filename: `${key}.svg`, idiom: 'universal' }],
    info: { author: 'xcode', version: 1 },
    properties: { 'preserves-vector-representation': true, 'template-rendering-intent': 'template' },
  });
  let vd = await svg2vd(svgFor(key, '#FFFFFF'), { floatPrecision: 3, xmlTag: false });
  vd = vd.replace(/\r\n/g, '\n').trim() + '\n';
  if (!/^<vector[\s\S]*<\/vector>\n$/.test(vd) || !vd.includes('android:pathData')) throw new Error(`VectorDrawable conversion failed for ${key}`);
  plan.text(path.join(drawableDir, `ic_pers_${snake(key)}.xml`), vdHeader(key) + vd);
}

// ------------------------------------------------------------ business icons (Hub capture tiles)
// Business only needs the Hub tile glyphs, so the key set comes straight from the manifest — no taxonomy.
const businessManifest = businessManifestEarly;
const businessKeys = businessManifest.icons.map((i) => i.key).sort();
const missingBiz = businessKeys.filter((k) => !lucideNodes[k]);
if (missingBiz.length) { console.error(`Unknown Lucide icon key(s) in business manifest: ${missingBiz.join(', ')}`); process.exit(2); }
const iosBizIconDir = path.join(PATHS.iosAssets, 'BusinessIcons');
plan.own(iosBizIconDir, () => true);
plan.json(path.join(iosBizIconDir, 'Contents.json'), { info: { author: 'xcode', version: 1 }, properties: { 'provides-namespace': true } });
for (const key of businessKeys) {
  const set = path.join(iosBizIconDir, `${key}.imageset`);
  plan.text(path.join(set, `${key}.svg`), svgFor(key, '#000000'));
  plan.json(path.join(set, 'Contents.json'), {
    images: [{ filename: `${key}.svg`, idiom: 'universal' }],
    info: { author: 'xcode', version: 1 },
    properties: { 'preserves-vector-representation': true, 'template-rendering-intent': 'template' },
  });
  let vd = await svg2vd(svgFor(key, '#FFFFFF'), { floatPrecision: 3, xmlTag: false });
  vd = vd.replace(/\r\n/g, '\n').trim() + '\n';
  if (!/^<vector[\s\S]*<\/vector>\n$/.test(vd) || !vd.includes('android:pathData')) throw new Error(`VectorDrawable conversion failed for business icon ${key}`);
  plan.text(path.join(drawableDir, `ic_biz_${snake(key)}.xml`), vdHeader(key) + vd);
}

// ------------------------------------------------------------ artwork
const LOCK = path.join(PIPELINE_DIR, 'assets.lock.json');
const lock = fs.existsSync(LOCK) ? readJson(LOCK) : { artwork: {} };
const newLock = { $description: 'Raster outputs of import-assets.mjs keyed by source hash + settings. Lets re-runs skip re-encoding on any OS. Generated; do not edit.', artwork: {} };
const out = manifest.artworkOutput;
plan.own(iosArtDir, () => true);
plan.json(path.join(iosArtDir, 'Contents.json'), { info: { author: 'xcode', version: 1 }, properties: { 'provides-namespace': true } });
plan.own(nodpiDir, (r) => /^art_pers_.*\.webp$/.test(r));
const artStale = [];
for (const art of manifest.artwork) {
  const srcPath = path.join(PATHS.assetDir, art.source);
  const src = fs.readFileSync(srcPath);
  const srcHash = sha(src);
  if (existingRaster.has(srcHash)) duplicates.push(`${art.key} == ${existingRaster.get(srcHash)}`);
  const meta = await sharp(src).metadata();
  const settings = { ios2xScale: 2 / 3, iosJpegQuality: out.iosJpegQuality, androidWebpQuality: out.androidWebpQuality };
  const set = path.join(iosArtDir, `${art.key}.imageset`);
  const files = {
    ios3x: path.join(set, `${art.key}@3x.jpg`),
    ios2x: path.join(set, `${art.key}@2x.jpg`),
    android: path.join(nodpiDir, `${art.key}.webp`),
  };
  const prev = lock.artwork?.[art.key];
  const reusable = prev && prev.source === srcHash && JSON.stringify(prev.settings) === JSON.stringify(settings) &&
    Object.entries(files).every(([k, f]) => fs.existsSync(f) && sha(fs.readFileSync(f)) === prev.outputs[k]);
  let bufs;
  if (reusable) {
    bufs = Object.fromEntries(Object.entries(files).map(([k, f]) => [k, fs.readFileSync(f)]));
  } else if (check) {
    artStale.push(art.key);
    continue;
  } else {
    const w2 = Math.round(meta.width * settings.ios2xScale);
    bufs = {
      ios3x: src,
      ios2x: await sharp(src).resize({ width: w2 }).jpeg({ quality: settings.iosJpegQuality, mozjpeg: true }).toBuffer(),
      android: await sharp(src).webp({ quality: settings.androidWebpQuality, effort: 6 }).toBuffer(),
    };
  }
  for (const [k, f] of Object.entries(files)) plan.bytes(f, bufs[k]);
  plan.json(path.join(set, 'Contents.json'), {
    images: [
      { idiom: 'universal', scale: '1x' },
      { filename: path.basename(files.ios2x), idiom: 'universal', scale: '2x' },
      { filename: path.basename(files.ios3x), idiom: 'universal', scale: '3x' },
    ],
    info: { author: 'xcode', version: 1 },
  });
  newLock.artwork[art.key] = {
    source: srcHash, sourceFile: `design/assets/personal/${art.source}`, size: [meta.width, meta.height], figmaNode: art.figmaNode, settings,
    outputs: Object.fromEntries(Object.entries(bufs).map(([k, b]) => [k, sha(b)])),
  };
}
if (!artStale.length) plan.json(LOCK, newLock);

// ------------------------------------------------------------ typed key indexes
const SWIFT_KEYWORDS = new Set(['repeat', 'import', 'class', 'struct', 'enum', 'case', 'default', 'func', 'let', 'var', 'in', 'is', 'as', 'return', 'switch', 'where', 'while', 'for', 'if', 'else', 'do', 'try', 'throw', 'break', 'continue', 'guard', 'defer', 'self', 'super', 'init', 'protocol', 'extension', 'operator', 'static', 'public', 'private', 'internal', 'fileprivate', 'open', 'true', 'false', 'nil', 'subscript', 'typealias', 'associatedtype', 'inout', 'deinit', 'catch', 'rethrows', 'throws', 'fallthrough', 'Any', 'Type', 'package', 'some', 'any', 'actor', 'macro', 'await', 'async', 'consume', 'borrowing', 'consuming', 'each']);
const swiftCase = (key) => { const c = camel([key]); return SWIFT_KEYWORDS.has(c) ? `\`${c}\`` : c; };
{
  const L = [];
  L.push('// GENERATED by tools/design-pipeline/import-assets.mjs from design/assets/personal/manifest.json');
  L.push('// and design/taxonomy/personal-taxonomy.v1.json. DO NOT EDIT: run `npm run assets` in tools/design-pipeline.');
  L.push(`// Icons: Lucide (ISC) via lucide-static@${lucideVersion}, stroke ${STROKE}, template-rendered (tint with .foregroundStyle).`);
  L.push('');
  L.push('import SwiftUI');
  L.push('');
  L.push('enum PersonalIcon: String, CaseIterable, Hashable {');
  for (const k of iconKeys) L.push(`    case ${swiftCase(k)} = "${k}"`);
  L.push('');
  L.push('    var assetName: String { "PersonalIcons/" + rawValue }');
  L.push('    var image: Image { Image(assetName).renderingMode(.template) }');
  L.push('}');
  L.push('');
  L.push('/// Moment artwork (illustrations, never personal photos). Crop per surface with .resizable().scaledToFill().clipped().');
  L.push('enum PersonalArtwork: String, CaseIterable, Hashable {');
  for (const a of manifest.artwork) L.push(`    /// ${a.use} (Figma ${a.figmaNode})`, `    case ${swiftCase(a.key.replace(/^art_pers_/, ''))} = "${a.key}"`);
  L.push('');
  L.push('    var assetName: String { "PersonalArtwork/" + rawValue }');
  L.push('    var image: Image { Image(assetName) }');
  L.push('}');
  plan.text(path.join(PATHS.iosGenerated, 'PersonalIcons.generated.swift'), L.join('\n') + '\n');
}
{
  const L = [];
  L.push('// GENERATED by tools/design-pipeline/import-assets.mjs from design/assets/business/manifest.json');
  L.push('// DO NOT EDIT: run `npm run assets` in tools/design-pipeline.');
  L.push(`// Icons: Lucide (ISC) via lucide-static@${lucideVersion}, stroke ${STROKE}, template-rendered (tint with .foregroundStyle).`);
  L.push('');
  L.push('import SwiftUI');
  L.push('');
  L.push('/// Business Hub capture-tile glyphs (Figma node 2666:31852).');
  L.push('enum BusinessIcon: String, CaseIterable, Hashable {');
  for (const key of businessKeys) L.push(`    case ${swiftCase(key)} = "${key}"`);
  L.push('');
  L.push('    var assetName: String { "BusinessIcons/" + rawValue }');
  L.push('    var image: Image { Image(assetName).renderingMode(.template) }');
  L.push('}');
  plan.text(path.join(PATHS.iosGenerated, 'BusinessIcons.generated.swift'), L.join('\n') + '\n');
}
{
  const L = [];
  L.push('// GENERATED by tools/design-pipeline/import-assets.mjs from design/assets/personal/manifest.json');
  L.push('// and design/taxonomy/personal-taxonomy.v1.json. DO NOT EDIT: run `npm run assets` in tools/design-pipeline.');
  L.push(`// Icons: Lucide (ISC) via lucide-static@${lucideVersion}, stroke ${STROKE}; tint at the call site (Icon(painterResource(..), tint = ..)).`);
  L.push('');
  L.push('package com.example.momentra.ui.theme.generated');
  L.push('');
  L.push('import androidx.annotation.DrawableRes');
  L.push('import com.example.momentra.R');
  L.push('');
  L.push('object PersonalIcons {');
  L.push('    /** Lucide key -> VectorDrawable. */');
  L.push('    val byKey: Map<String, Int> = mapOf(');
  for (const k of iconKeys) L.push(`        "${k}" to R.drawable.ic_pers_${snake(k)},`);
  L.push('    )');
  L.push('');
  L.push('    @DrawableRes');
  L.push('    fun res(key: String): Int? = byKey[key]');
  L.push('}');
  L.push('');
  L.push('/** Moment artwork (illustrations, never personal photos). Crop per surface with ContentScale.Crop. */');
  L.push('enum class PersonalArtwork(val key: String, @param:DrawableRes val res: Int) {');
  L.push(manifest.artwork.map((a) => `    ${snake(a.key.replace(/^art_pers_/, '')).toUpperCase()}("${a.key}", R.drawable.${a.key})`).join(',\n') + ',');
  L.push('}');
  plan.text(path.join(PATHS.androidGenerated, 'PersonalAssets.kt'), L.join('\n') + '\n');
}
{
  const L = [];
  L.push('// GENERATED by tools/design-pipeline/import-assets.mjs from design/assets/business/manifest.json');
  L.push('// DO NOT EDIT: run `npm run assets` in tools/design-pipeline.');
  L.push(`// Icons: Lucide (ISC) via lucide-static@${lucideVersion}, stroke ${STROKE}; tint at the call site.`);
  L.push('');
  L.push('package com.example.momentra.ui.theme.generated');
  L.push('');
  L.push('import androidx.annotation.DrawableRes');
  L.push('import com.example.momentra.R');
  L.push('');
  L.push('/** Business Hub capture-tile glyphs (Figma node 2666:31852). */');
  L.push('object BusinessIcons {');
  L.push('    /** Lucide key -> VectorDrawable. */');
  L.push('    val byKey: Map<String, Int> = mapOf(');
  for (const key of businessKeys) L.push(`        "${key}" to R.drawable.ic_biz_${snake(key)},`);
  L.push('    )');
  L.push('');
  L.push('    @DrawableRes');
  L.push('    fun res(key: String): Int? = byKey[key]');
  L.push('}');
  plan.text(path.join(PATHS.androidGenerated, 'BusinessIcons.kt'), L.join('\n') + '\n');
}

plan.apply();
let code = plan.report(check ? 'assets:check' : 'assets');
console.log(`[assets] ${iconKeys.length} personal icons + ${businessKeys.length} business icons (lucide-static@${lucideVersion}), ${manifest.artwork.length} artworks.`);
if (duplicates.length) console.log(`[assets] exact duplicates of existing assets:\n  ${duplicates.join('\n  ')}${skipped.size ? `\n  skipped: ${[...skipped].join(', ')}` : ''}`);
else console.log('[assets] no exact duplicates of existing repo assets found.');
if (artStale.length) { console.error(`[assets:check] artwork out of date: ${artStale.join(', ')}`); code = 1; }
process.exit(code);
