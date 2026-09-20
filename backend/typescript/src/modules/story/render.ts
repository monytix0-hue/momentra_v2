import type { StorySnapshot } from './snapshot';
import type { StoryChapterId } from './profiles';

const BRAND = {
  indigo700: '#2D1F5E',
  indigo500: '#4B3EA8',
  indigo100: '#C4BDEE',
  ember: '#E8621A',
  amber: '#F5A623',
  teal: '#1D9E75',
  text: '#F5F0FF',
  lightBg: '#F7F5FC',
  lightText: '#1A0F3D',
};

function esc(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function metricTiles(snapshot: StorySnapshot): string {
  return snapshot.display.metricKeys
    .map((m, i) => {
      const val = snapshot.metrics[m.key] ?? '—';
      const ring = i === 3 ? `border-bottom:3px solid ${BRAND.ember};` : '';
      return `<div class="tile" style="${ring}"><div class="num">${esc(String(val))}</div><div class="lbl">${esc(m.label)}</div></div>`;
    })
    .join('');
}

function chapterHtml(snapshot: StorySnapshot, chapter: StoryChapterId, screen: boolean): string {
  const bg = screen ? BRAND.indigo700 : BRAND.lightBg;
  const fg = screen ? BRAND.text : BRAND.lightText;
  const muted = screen ? BRAND.indigo100 : '#3C3489';
  const id = snapshot.identity;
  const logo = `<div class="logo">momentra<span class="a">a</span></div>`;

  if (chapter === 'cover') {
    return `<section class="page" style="background:${bg};color:${fg}">
      ${logo}
      <div class="eyebrow">${esc(snapshot.display.coverEyebrow)}</div>
      <h1>${esc(id.title)}</h1>
      <p class="meta">${esc([id.startAt?.slice(0, 10), id.endAt?.slice(0, 10)].filter(Boolean).join(' · '))}</p>
      <p class="open">${esc(snapshot.narrative.opening)}</p>
      <div class="tiles">${metricTiles(snapshot)}</div>
    </section>`;
  }
  if (chapter === 'alive') {
    const items = snapshot.timeline
      .map((t) => `<li><strong>${esc(t.at.slice(0, 10))}</strong> — ${esc(t.label)}${t.detail ? `<span>${esc(t.detail)}</span>` : ''}</li>`)
      .join('');
    const decisions = snapshot.decisions
      .slice(0, 4)
      .map((d) => `<div class="chip">${esc(d.title)}</div>`)
      .join('');
    return `<section class="page" style="background:${bg};color:${fg}">
      ${logo}
      <h2>How it came alive</h2>
      <ol class="timeline">${items || '<li>The moment unfolded together.</li>'}</ol>
      <div class="chips">${decisions}</div>
    </section>`;
  }
  if (chapter === 'money') {
    const cats = snapshot.money.categories
      .slice(0, 5)
      .map((c) => `<li>${esc(c.name)} — ₹${Math.round(c.amount)}</li>`)
      .join('');
    const flow = `<div class="flow">
      <div>Contributed<br/><b>₹${Math.round(snapshot.money.contributed)}</b></div>
      <div>Spent<br/><b>₹${Math.round(snapshot.money.spent)}</b></div>
      <div>Remaining<br/><b>₹${Math.round(snapshot.money.remaining)}</b></div>
      <div>Unsettled<br/><b>₹${Math.round(snapshot.money.unsettled)}</b></div>
    </div>`;
    return `<section class="page" style="background:${bg};color:${fg}">
      ${logo}
      <h2>Money & fairness</h2>
      ${flow}
      <ul class="cats">${cats || '<li>No expenses recorded.</li>'}</ul>
    </section>`;
  }
  if (chapter === 'memories') {
    const quotes = snapshot.memories
      .filter((m) => m.text)
      .slice(0, 3)
      .map((m) => `<blockquote>${esc(m.text!)}</blockquote>`)
      .join('');
    const insights = snapshot.narrative.insights.map((i) => `<div class="insight">${esc(i)}</div>`).join('');
    return `<section class="page" style="background:${bg};color:${fg}">
      ${logo}
      <h2>Memories that stayed</h2>
      ${quotes || `<p style="color:${muted}">Photos and memories will glow here next time.</p>`}
      ${insights}
    </section>`;
  }
  // close
  return `<section class="page close" style="background:${bg};color:${fg}">
    ${logo}
    <h2 class="tag">TOGETHER · FORWARD</h2>
    <p class="close-line">${esc(snapshot.display.closeLine)}</p>
    <p class="watermark">${esc(id.title)}</p>
  </section>`;
}

const BASE_CSS = `
  * { box-sizing: border-box; }
  body { margin: 0; font-family: 'Plus Jakarta Sans', system-ui, sans-serif; }
  .page { min-height: 100vh; padding: 32px 28px 48px; }
  .logo { font-weight: 800; letter-spacing: 0.02em; margin-bottom: 24px; }
  .logo .a { color: ${BRAND.ember}; }
  .eyebrow { text-transform: uppercase; font-size: 12px; letter-spacing: 0.12em; color: ${BRAND.ember}; }
  h1 { font-size: 36px; margin: 8px 0 12px; }
  h2 { font-size: 28px; margin: 0 0 20px; }
  .meta { opacity: 0.85; }
  .open { font-size: 16px; line-height: 1.5; max-width: 36em; }
  .tiles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 28px; }
  .tile { background: rgba(75,62,168,0.45); border-radius: 12px; padding: 14px; }
  .num { font-size: 22px; font-weight: 800; }
  .lbl { font-size: 11px; opacity: 0.8; margin-top: 4px; }
  .timeline { list-style: none; padding: 0; }
  .timeline li { border-left: 3px solid ${BRAND.ember}; padding: 0 0 16px 14px; margin-left: 6px; }
  .timeline span { display: block; opacity: 0.75; font-size: 13px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
  .chip { background: rgba(232,98,26,0.2); border: 1px solid ${BRAND.ember}; border-radius: 999px; padding: 6px 12px; font-size: 13px; }
  .flow { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 20px 0; }
  .flow > div { background: rgba(75,62,168,0.4); border-radius: 12px; padding: 12px; text-align: center; font-size: 13px; }
  .cats { line-height: 1.7; }
  blockquote { border-left: 3px solid ${BRAND.ember}; margin: 12px 0; padding-left: 14px; font-style: italic; }
  .insight { background: rgba(75,62,168,0.35); border-radius: 10px; padding: 12px; margin: 8px 0; }
  .close { display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; }
  .tag { letter-spacing: 0.2em; font-size: 14px; color: ${BRAND.ember}; }
  .close-line { font-size: 22px; }
  .watermark { opacity: 0.35; font-size: 28px; font-weight: 800; margin-top: 40px; }
`;

export function renderInteractiveStoryHtml(snapshot: StorySnapshot): string {
  const pages = snapshot.chapters.map((c) => chapterHtml(snapshot, c, true)).join('\n');
  return `<!DOCTYPE html><html><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>${esc(snapshot.identity.title)} · Moment Story</title>
<style>${BASE_CSS}
  .pager { position: sticky; top: 0; z-index: 2; display: flex; gap: 6px; padding: 10px 16px; background: ${BRAND.indigo700}; }
  .pager a { color: ${BRAND.indigo100}; text-decoration: none; font-size: 12px; }
</style></head><body>
<nav class="pager">${snapshot.chapters.map((c, i) => `<a href="#c${i}">${i + 1}. ${c}</a>`).join('')}</nav>
${snapshot.chapters.map((c, i) => `<div id="c${i}">${chapterHtml(snapshot, c, true)}</div>`).join('\n')}
</body></html>`;
}

export function renderBookletHtml(snapshot: StorySnapshot): string {
  const pages = snapshot.chapters.map((c) => chapterHtml(snapshot, c, false)).join('\n');
  return `<!DOCTYPE html><html><head><meta charset="utf-8"/>
<title>${esc(snapshot.identity.title)} · Printable Story</title>
<style>${BASE_CSS}
  @page { size: A4; margin: 14mm; }
  .page { min-height: auto; page-break-after: always; border: 1px solid #e5e0ee; border-radius: 8px; margin-bottom: 16px; }
  .tile { background: #eee9ff; }
  .flow > div { background: #eee9ff; }
</style></head><body>${pages}</body></html>`;
}

/** SVG "PNG pack" chapter cards (vector; clients can rasterize). */
export function renderChapterSvg(snapshot: StorySnapshot, chapter: StoryChapterId): string {
  const title =
    chapter === 'cover'
      ? snapshot.identity.title
      : chapter === 'alive'
        ? 'How it came alive'
        : chapter === 'money'
          ? 'Money & fairness'
          : chapter === 'memories'
            ? 'Memories'
            : 'TOGETHER · FORWARD';
  const subtitle =
    chapter === 'cover'
      ? snapshot.narrative.opening
      : chapter === 'money'
        ? `Spent ₹${Math.round(snapshot.money.spent)}`
        : snapshot.display.closeLine;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1920" viewBox="0 0 1080 1920">
  <rect width="1080" height="1920" fill="${BRAND.indigo700}"/>
  <text x="64" y="100" fill="#F5F0FF" font-size="36" font-family="Arial,sans-serif" font-weight="700">momentra</text>
  <text x="210" y="100" fill="${BRAND.ember}" font-size="36" font-family="Arial,sans-serif" font-weight="700">a</text>
  <text x="64" y="280" fill="${BRAND.ember}" font-size="28" font-family="Arial,sans-serif">${esc(snapshot.display.coverEyebrow)}</text>
  <text x="64" y="380" fill="#F5F0FF" font-size="64" font-family="Arial,sans-serif" font-weight="800">${esc(title).slice(0, 40)}</text>
  <foreignObject x="64" y="460" width="952" height="400">
    <div xmlns="http://www.w3.org/1999/xhtml" style="color:#C4BDEE;font:28px sans-serif;line-height:1.4">${esc(subtitle).slice(0, 180)}</div>
  </foreignObject>
  <rect x="64" y="1700" width="200" height="8" fill="${BRAND.ember}"/>
  <text x="64" y="1780" fill="#C4BDEE" font-size="24" font-family="Arial,sans-serif">${esc(snapshot.display.displayLabel)}</text>
</svg>`;
}

export function renderVideoReelSpec(snapshot: StorySnapshot): Record<string, unknown> {
  const scenes = snapshot.chapters.map((chapter, index) => ({
    index,
    chapter,
    durationSec: chapter === 'cover' || chapter === 'close' ? 4 : 5,
    title:
      chapter === 'cover'
        ? snapshot.identity.title
        : chapter === 'alive'
          ? 'How it came alive'
          : chapter === 'money'
            ? 'Money & fairness'
            : chapter === 'memories'
              ? 'Memories'
              : 'TOGETHER · FORWARD',
    subtitle:
      chapter === 'cover'
        ? snapshot.narrative.opening
        : chapter === 'money'
          ? `₹${Math.round(snapshot.money.spent)} moved together`
          : snapshot.display.closeLine,
    kenBurns: true,
    brandSting: chapter === 'cover' || chapter === 'close',
  }));
  return {
    format: 'momentra-story-reel-v1',
    durationSec: scenes.reduce((s, x) => s + x.durationSec, 0),
    fps: 30,
    resolution: { width: 1080, height: 1920 },
    audio: { bed: 'soft-ember-pulse', duckOnCopy: true },
    scenes,
    logo: { asset: 'momentra-official-logo', tagline: 'TOGETHER · FORWARD' },
    notes: 'Client or worker may encode MP4 from this spec + selected photos; Phase 3 ships the reel spec + shareable cover.',
  };
}

export function renderShareCoverSvg(snapshot: StorySnapshot): string {
  return renderChapterSvg(snapshot, 'cover');
}
