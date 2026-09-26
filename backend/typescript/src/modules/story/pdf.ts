import fs from 'fs';
import path from 'path';
import PDFDocument from 'pdfkit';
import type { StorySnapshot } from './snapshot';

const NIGHT = '#120F20';
const INDIGO = '#2D1F5E';
const INDIGO_DEEP = '#160C38';
const CARD = '#1A1430';
const EMBER = '#E8621A';
const GOLD = '#F5A623';
const VIOLET = '#6C4EF2';
const GREEN = '#0EC97F';
const LIGHT = '#F4EFFF';
const MUTED = '#6B5EA0';
const LAVENDER = '#C3B5FD';
const ZONE = 'Asia/Kolkata';
const SLICE = [VIOLET, EMBER, GOLD, GREEN, '#4B3EA8', '#3D6B9A', '#B45F3D', '#8A6A4A'];
const MX = 79.4;
const RIGHT = 515.9;
const CONTENT_W = RIGHT - MX;
const LOGO_ASPECT = 270 / 100;

type Expense = StorySnapshot['money']['expenses'][number];
type PdfCtx = { doc: PDFKit.PDFDocument; pageNumber: number };

function formatMoney(amount: number, currency: string): string {
  const code = /^[A-Z]{3}$/.test(currency) ? currency : 'INR';
  const rounded = Math.round(amount);
  const sign = rounded < 0 ? '-' : '';
  const body = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(Math.abs(rounded));
  if (code === 'INR') return `${sign}Rs ${body}`;
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: code,
      maximumFractionDigits: 0,
    }).format(rounded);
  } catch {
    return `${sign}${code} ${body}`;
  }
}

function moneyIsEmpty(money: StorySnapshot['money']): boolean {
  return (
    (money.contributed || 0) === 0 &&
    (money.spent || 0) === 0 &&
    (money.remaining || 0) === 0 &&
    (money.expenses?.length ?? 0) === 0
  );
}

function parseInstant(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

function zoneParts(iso: string | null | undefined): { day: string; month: number; year: string } | null {
  const d = parseInstant(iso);
  if (!d) return null;
  const parts = new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'numeric',
    year: 'numeric',
    timeZone: ZONE,
  }).formatToParts(d);
  const read = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? '';
  const month = Number(read('month'));
  if (!month) return null;
  return { day: read('day'), month, year: read('year') };
}

function dayKey(iso: string | null | undefined): string {
  const d = parseInstant(iso);
  if (!d) return '';
  return new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: ZONE,
  }).format(d);
}

function dateRange(start: string | null, end: string | null): string {
  const a = zoneParts(start);
  const b = zoneParts(end);
  if (!a && !b) return '';
  if (a && b && a.month === b.month && a.year === b.year) {
    return `${a.day}–${b.day} ${MONTHS[b.month - 1]} ${b.year}`;
  }
  const left = a ? `${a.day} ${MONTHS[a.month - 1]} ${a.year}` : '';
  const right = b ? `${b.day} ${MONTHS[b.month - 1]} ${b.year}` : '';
  return [left, right].filter(Boolean).join(' – ');
}

function formatDayMonth(iso: string): string {
  const p = zoneParts(iso);
  if (!p) return '';
  return `${p.day} ${MONTHS[p.month - 1]}`;
}

function formatActivitySpan(earliest: string, latest: string): string {
  const a = zoneParts(earliest);
  const b = zoneParts(latest);
  if (!a || !b) return '';
  if (a.month === b.month && a.year === b.year) return `${a.day}–${b.day} ${MONTHS[a.month - 1]}`;
  if (a.year === b.year) return `${a.day} ${MONTHS[a.month - 1]} – ${b.day} ${MONTHS[b.month - 1]}`;
  return `${a.day} ${MONTHS[a.month - 1]} ${a.year} – ${b.day} ${MONTHS[b.month - 1]} ${b.year}`;
}

function categoryPercents(amounts: number[], spent: number): number[] {
  if (spent <= 0 || amounts.length === 0) return amounts.map(() => 0);
  const raw = amounts.map((amount) => Math.round((amount / spent) * 100));
  const drift = 100 - raw.reduce((sum, n) => sum + n, 0);
  if (drift === 0) return raw;
  let largest = 0;
  amounts.forEach((amount, index) => {
    if (amount > amounts[largest]!) largest = index;
  });
  raw[largest] = Math.max(0, (raw[largest] ?? 0) + drift);
  return raw;
}

function formatStamp(iso: string | null | undefined): string {
  const p = zoneParts(iso);
  if (!p) return 'Undated';
  return `${p.day} ${MONTHS[p.month - 1]!.slice(0, 3).toUpperCase()}`;
}

function formatShort(iso: string | null | undefined): string {
  const p = zoneParts(iso);
  if (!p) return 'Undated';
  return `${p.day} ${MONTHS[p.month - 1]!.slice(0, 3)}`;
}

function formatStampYear(iso: string | null | undefined): string | null {
  const p = zoneParts(iso);
  if (!p) return null;
  return `${formatStamp(iso)} ${p.year}`;
}

function formatCompact(amount: number, currency: string): string {
  const code = /^[A-Z]{3}$/.test(currency) ? currency : 'INR';
  const rounded = Math.round(amount);
  const abs = Math.abs(rounded);
  const compact =
    abs >= 100000 ? `${(rounded / 100000).toFixed(1)}L` : abs >= 1000 ? `${(rounded / 1000).toFixed(1)}k` : String(rounded);
  const sign = compact.startsWith('-') ? '-' : '';
  const body = sign ? compact.slice(1) : compact;
  if (code === 'INR') return `${sign}Rs ${body}`;
  return `${sign}${code} ${body}`;
}

function titleCase(value: string): string {
  return value.toLowerCase().replace(/(^|[\s/&-])(\w)/g, (_match, lead: string, ch: string) => `${lead}${ch.toUpperCase()}`);
}

function memoryCount(snapshot: StorySnapshot): number {
  return snapshot.photos.filter((photo) => photo.url).length;
}

function summaryLine(snapshot: StorySnapshot, includePayments: boolean): string {
  const people = snapshot.people.length;
  const memories = memoryCount(snapshot);
  const parts = [
    people === 1 ? '1 person' : `${people} people`,
    memories === 1 ? '1 memory' : `${memories} memories`,
    formatMoney(snapshot.money.spent, snapshot.identity.currencyCode),
  ];
  if (includePayments) {
    const payments = snapshot.money.expenses.length;
    parts.push(payments === 1 ? '1 payment' : `${payments} payments`);
  }
  return parts.join(' · ');
}

function byTime<T extends { at: string | null }>(items: T[]): T[] {
  return [...items].sort((a, b) => {
    if (!a.at && !b.at) return 0;
    if (!a.at) return 1;
    if (!b.at) return -1;
    return a.at < b.at ? -1 : a.at > b.at ? 1 : 0;
  });
}

let logoBytes: Buffer | null | undefined;

function storyLogo(): Buffer | null {
  if (logoBytes !== undefined) return logoBytes;
  const file = path.join(process.cwd(), 'src/modules/story/assets/momentra-official-logo.png');
  try {
    logoBytes = fs.readFileSync(file);
  } catch {
    logoBytes = null;
  }
  return logoBytes;
}

function drawWordmark(doc: PDFKit.PDFDocument, x: number, y: number, height: number): boolean {
  const logo = storyLogo();
  if (!logo) return false;
  try {
    doc.image(logo, x, y, { height });
    return true;
  } catch {
    return false;
  }
}

function drawWordmarkCentered(doc: PDFKit.PDFDocument, y: number, height: number): void {
  const width = height * LOGO_ASPECT;
  drawWordmark(doc, (doc.page.width - width) / 2, y, height);
}

function drawMark(doc: PDFKit.PDFDocument, x: number, y: number, size: number): void {
  const logo = storyLogo();
  if (!logo) return;
  doc.save();
  doc.rect(x, y, size, size).clip();
  try {
    doc.image(logo, x, y, { height: size });
  } catch {
    // Logo is optional chrome.
  }
  doc.restore();
}

function resetInk(doc: PDFKit.PDFDocument): void {
  doc.fillOpacity(1);
  doc.strokeOpacity(1);
  doc.font('Helvetica');
}

function paintRings(
  doc: PDFKit.PDFDocument,
  cx: number,
  cy: number,
  color: string,
  outer: number,
  step: number,
  count: number,
  peak: number
): void {
  for (let i = 0; i < count; i += 1) {
    const radius = outer - i * step;
    if (radius <= 0) continue;
    doc.fillColor(color, peak * (1 - i / count)).circle(cx, cy, radius).fill();
  }
}

function paintInterior(doc: PDFKit.PDFDocument, pageNumber: number, footerLabel: string): void {
  resetInk(doc);
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(NIGHT);
  doc.rect(0, 0, 17, doc.page.height).fill(INDIGO);
  doc.save();
  doc.strokeColor(INDIGO).lineWidth(0.6).moveTo(MX, 790.9).lineTo(RIGHT, 790.9).stroke();
  doc.restore();
  drawWordmark(doc, MX, 795.1, 25.5);
  doc.font('Helvetica').fontSize(7).fillColor(MUTED).text(footerLabel, 209.8, 804.6, {
    lineBreak: false,
    height: 12,
  });
  doc.font('Helvetica').fontSize(9).fillColor(LIGHT).text(String(pageNumber).padStart(2, '0'), RIGHT - 28, 802.5, {
    width: 28,
    align: 'right',
    lineBreak: false,
    height: 14,
  });
}

function addInterior(ctx: PdfCtx, footer: string): void {
  ctx.doc.addPage();
  ctx.pageNumber += 1;
  paintInterior(ctx.doc, ctx.pageNumber, footer);
}

function centerLine(
  doc: PDFKit.PDFDocument,
  text: string,
  y: number,
  size: number,
  color: string,
  font: 'Helvetica' | 'Helvetica-Bold',
  width = 460
): void {
  doc.font(font).fontSize(size).fillColor(color).text(text, (doc.page.width - width) / 2, y, {
    width,
    align: 'center',
    lineBreak: false,
    height: size + 6,
  });
}

function coverPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  resetInk(doc);
  const pageW = doc.page.width;
  const pageH = doc.page.height;
  doc.rect(0, 0, pageW, pageH).fill(INDIGO_DEEP);
  doc.rect(0, 0, pageW, 530.1).fill(INDIGO);
  doc.save();
  doc.rect(0, 0, pageW, pageH).clip();
  paintRings(doc, 99.2, 127.6, EMBER, 141.7, 11.81, 12, 0.098);
  paintRings(doc, 524.4, 240.9, GOLD, 184.3, 15.35, 12, 0.07);
  paintRings(doc, 297.6, 70.9, VIOLET, 113.4, 9.45, 12, 0.077);
  doc.restore();
  resetInk(doc);
  doc.rect(0, 0, 12.8, pageH).fill(EMBER);
  doc.rect(12.8, 0, 3.1, pageH).fill(GOLD);
  doc.rect(0, 544.3, pageW, pageH - 544.3).fill(NIGHT);
  for (const dot of [
    { x: 195.6, r: 1.7, color: GOLD },
    { x: 246.6, r: 1.7, color: GOLD },
    { x: 297.6, r: 2.8, color: EMBER },
    { x: 348.7, r: 1.7, color: GOLD },
    { x: 399.7, r: 1.7, color: GOLD },
  ]) {
    doc.fillColor(dot.color).circle(dot.x, 453.5, dot.r).fill();
  }
  drawWordmarkCentered(doc, 283.5, 119);
  centerLine(doc, 'M E M O R Y', 418, 12, GOLD, 'Helvetica-Bold', 200);
  doc.save();
  doc.strokeColor(EMBER).lineWidth(2).moveTo(212.6, 569.8).lineTo(382.7, 569.8).stroke();
  doc.restore();
  doc.font('Helvetica-Bold').fontSize(32).fillColor(LIGHT).text(snapshot.identity.title.toUpperCase(), (pageW - 360) / 2, 598, {
    width: 360,
    align: 'center',
    height: 88,
    ellipsis: true,
  });
  if (snapshot.narrative.opening) {
    doc.font('Helvetica').fontSize(12).fillColor(LIGHT).text(snapshot.narrative.opening, 70, 701, {
      width: pageW - 140,
      align: 'center',
      height: 32,
      ellipsis: true,
    });
  }
  const dates = dateRange(snapshot.identity.startAt, snapshot.identity.endAt);
  if (dates) centerLine(doc, dates, 737, 13, GOLD, 'Helvetica-Bold');
  centerLine(doc, summaryLine(snapshot, false), 774, 10, LAVENDER, 'Helvetica', 400);
  drawMark(doc, 515.9, 773.9, 40);
}

const COLLAGE: Array<{ x: number; y: number; w: number; h: number }> = [
  { x: 79.4, y: 164.4, w: 204.1, h: 272.1 },
  { x: 328.3, y: 164.4, w: 151.2, h: 113.4 },
  { x: 292.0, y: 288.3, w: 107.5, h: 143.4 },
  { x: 81.3, y: 445.0, w: 136.0, h: 102.1 },
  { x: 229.6, y: 445.0, w: 136.1, h: 102.1 },
  { x: 378.0, y: 445.0, w: 136.0, h: 102.1 },
];

type PdfPhoto = { image: Buffer; title: string | null; at: string | null };
type ImageMeta = { width: number; height: number; orientation: number };

function momentPages(ctx: PdfCtx, snapshot: StorySnapshot, photos: PdfPhoto[]): void {
  const doc = ctx.doc;
  const beats: Array<{ when: string; label: string }> = [];
  const currency = snapshot.identity.currencyCode;
  if (snapshot.identity.startAt) beats.push({ when: formatStamp(snapshot.identity.startAt), label: 'Moment began' });
  if (snapshot.money.target != null) {
    beats.push({ when: formatMoney(snapshot.money.target, currency), label: 'Budget set' });
  }
  const ended = snapshot.identity.completedAt ?? snapshot.identity.endAt;
  if (ended) beats.push({ when: formatStamp(ended), label: 'Moment completed' });

  const pages = Math.max(1, Math.ceil(photos.length / COLLAGE.length));
  for (let page = 0; page < pages; page += 1) {
    addInterior(ctx, 'MEMORY');
    doc.font('Helvetica-Bold').fontSize(8).fillColor(GOLD).text('01  /  THE MOMENT', MX, 53.8, {
      lineBreak: false,
      height: 12,
    });
    doc.font('Helvetica-Bold').fontSize(28).fillColor(LIGHT).text('What stayed', MX, 77.8, {
      lineBreak: false,
      height: 36,
    });
    if (page === 0 && snapshot.narrative.opening) {
      doc.font('Helvetica').fontSize(10).fillColor(MUTED).text(snapshot.narrative.opening, MX, 125.3, {
        width: CONTENT_W,
        height: 32,
        ellipsis: true,
      });
    }
    photos.slice(page * COLLAGE.length, (page + 1) * COLLAGE.length).forEach((photo, index) => {
      const slot = COLLAGE[index];
      if (!slot) return;
      const meta = readImageMeta(photo.image);
      paintCover(doc, photo.image, meta, slot.x, slot.y, slot.w, slot.h);
    });
    if (page === 0) {
      const columns = [79.4, 224.9, 370.4];
      beats.forEach((beat, index) => {
        const x = columns[index];
        if (x == null) return;
        doc.font('Helvetica-Bold').fontSize(12).fillColor(GOLD).text(beat.when, x, 715.7, {
          width: 140,
          lineBreak: false,
          height: 16,
        });
        doc.font('Helvetica').fontSize(9).fillColor(LIGHT).text(beat.label, x, 741.5, {
          width: 140,
          lineBreak: false,
          height: 14,
        });
      });
    }
  }
}

function readImageMeta(buf: Buffer): ImageMeta {
  if (buf.length >= 24 && buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4e && buf[3] === 0x47) {
    return { width: buf.readUInt32BE(16), height: buf.readUInt32BE(20), orientation: 1 };
  }
  if (buf.length >= 4 && buf[0] === 0xff && buf[1] === 0xd8) return readJpegMeta(buf);
  return { width: 4, height: 3, orientation: 1 };
}

function readJpegMeta(buf: Buffer): ImageMeta {
  let width = 4;
  let height = 3;
  let orientation = 1;
  let offset = 2;
  while (offset + 4 < buf.length) {
    if (buf[offset] !== 0xff) {
      offset += 1;
      continue;
    }
    const marker = buf[offset + 1]!;
    if (marker === 0xd8 || marker === 0xd9) {
      offset += 2;
      continue;
    }
    if (marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
      offset += 2;
      continue;
    }
    const size = buf.readUInt16BE(offset + 2);
    if (size < 2 || offset + 2 + size > buf.length) break;
    if (marker === 0xe1) {
      const found = readExifOrientation(buf.subarray(offset + 4, offset + 2 + size));
      if (found) orientation = found;
    }
    if (marker === 0xc0 || marker === 0xc1 || marker === 0xc2) {
      height = buf.readUInt16BE(offset + 5);
      width = buf.readUInt16BE(offset + 7);
    }
    offset += 2 + size;
  }
  if (!width || !height) return { width: 4, height: 3, orientation };
  return { width, height, orientation };
}

function readExifOrientation(app1: Buffer): number | null {
  if (app1.length < 16 || app1.toString('ascii', 0, 4) !== 'Exif') return null;
  const tiff = 6;
  const little = app1.toString('ascii', tiff, tiff + 2) === 'II';
  const u16 = (at: number) => (little ? app1.readUInt16LE(at) : app1.readUInt16BE(at));
  const u32 = (at: number) => (little ? app1.readUInt32LE(at) : app1.readUInt32BE(at));
  if (tiff + 8 > app1.length || u16(tiff + 2) !== 42) return null;
  let ifd = tiff + u32(tiff + 4);
  if (ifd + 2 > app1.length) return null;
  const count = u16(ifd);
  ifd += 2;
  for (let i = 0; i < count; i += 1) {
    const entry = ifd + i * 12;
    if (entry + 12 > app1.length) break;
    if (u16(entry) === 0x0112) {
      const value = u16(entry + 8);
      return value >= 1 && value <= 8 ? value : 1;
    }
  }
  return null;
}

/** EXIF display matrices in y-down space, the same ones a canvas uses to stand a photo upright. */
function paintOriented(
  doc: PDFKit.PDFDocument,
  image: Buffer,
  meta: ImageMeta,
  x: number,
  y: number,
  drawW: number,
  drawH: number
): void {
  const swap = meta.orientation >= 5;
  const imageW = swap ? drawH : drawW;
  const imageH = swap ? drawW : drawH;
  doc.save();
  doc.rect(x, y, drawW, drawH).clip();
  doc.translate(x, y);
  switch (meta.orientation) {
    case 2:
      doc.transform(-1, 0, 0, 1, drawW, 0);
      break;
    case 3:
      doc.transform(-1, 0, 0, -1, drawW, drawH);
      break;
    case 4:
      doc.transform(1, 0, 0, -1, 0, drawH);
      break;
    case 5:
      doc.transform(0, 1, 1, 0, 0, 0);
      break;
    case 6:
      doc.transform(0, 1, -1, 0, drawH, 0);
      break;
    case 7:
      doc.transform(0, -1, -1, 0, drawH, drawW);
      break;
    case 8:
      doc.transform(0, -1, 1, 0, 0, drawW);
      break;
    default:
      break;
  }
  try {
    doc.image(image, 0, 0, { width: imageW, height: imageH });
  } catch {
    // Skip a photo the PDF engine cannot decode.
  }
  doc.restore();
}

function paintCover(
  doc: PDFKit.PDFDocument,
  image: Buffer,
  meta: ImageMeta,
  x: number,
  y: number,
  drawW: number,
  drawH: number
): void {
  const swap = meta.orientation >= 5;
  const srcW = swap ? meta.height : meta.width;
  const srcH = swap ? meta.width : meta.height;
  const scale = Math.max(drawW / Math.max(srcW, 1), drawH / Math.max(srcH, 1));
  const dw = srcW * scale;
  const dh = srcH * scale;
  doc.save();
  doc.rect(x, y, drawW, drawH).clip();
  paintOriented(doc, image, meta, x + (drawW - dw) / 2, y + (drawH - dh) / 2, dw, dh);
  doc.restore();
}

type PdfArc = PDFKit.PDFDocument & {
  arc(
    x: number,
    y: number,
    radius: number,
    startAngle: number,
    endAngle: number,
    anticlockwise?: boolean
  ): PDFKit.PDFDocument;
};

function drawDonut(
  doc: PDFKit.PDFDocument,
  cx: number,
  cy: number,
  radius: number,
  hole: number,
  slices: Array<{ amount: number }>
): void {
  const total = slices.reduce((sum, slice) => sum + slice.amount, 0) || 1;
  let angle = -Math.PI / 2;
  slices.forEach((slice, index) => {
    const sweep = (slice.amount / total) * Math.PI * 2;
    if (sweep <= 0) return;
    const end = angle + sweep;
    doc.save();
    doc.fillColor(SLICE[index % SLICE.length]!);
    doc.moveTo(cx, cy);
    (doc as PdfArc).arc(cx, cy, radius, angle, end, false);
    doc.lineTo(cx, cy);
    doc.fill();
    doc.restore();
    angle = end;
  });
  doc.circle(cx, cy, hole).fill(NIGHT);
}

function expenseOutsideNote(snapshot: StorySnapshot): string | null {
  const dated = snapshot.money.expenses.map((expense) => expense.at).filter((at): at is string => Boolean(at));
  if (dated.length === 0) return null;
  const start = snapshot.identity.startAt;
  const end = snapshot.identity.endAt ?? snapshot.identity.completedAt;
  if (!start && !end) return null;
  const earliest = dated.reduce((a, b) => (a < b ? a : b));
  const latest = dated.reduce((a, b) => (a > b ? a : b));
  const before = Boolean(start && dayKey(earliest) && dayKey(earliest) < dayKey(start));
  const after = Boolean(end && dayKey(latest) && dayKey(latest) > dayKey(end));
  if (!before && !after) return null;
  const span = formatActivitySpan(earliest, latest);
  const activated = start ? formatDayMonth(start) : null;
  const completed = end ? formatDayMonth(end) : null;
  if (activated && completed) {
    return `Recorded expense activity spans ${span}, while the Moment itself\nwas activated on ${activated} and completed on ${completed}.`;
  }
  if (activated) {
    return `Recorded expense activity spans ${span}, while the Moment itself was activated on ${activated}.`;
  }
  if (completed) {
    return `Recorded expense activity spans ${span}, while the Moment itself was completed on ${completed}.`;
  }
  return null;
}

function moneyPage(ctx: PdfCtx, snapshot: StorySnapshot): void {
  const doc = ctx.doc;
  addInterior(ctx, 'MEMORY');
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode;
  const payments = money.expenses.length;
  doc.font('Helvetica-Bold').fontSize(8).fillColor(GOLD).text('02  /  WHAT IT TOOK', MX, 53.8, {
    lineBreak: false,
    height: 12,
  });
  doc.font('Helvetica-Bold').fontSize(28).fillColor(LIGHT).text('The financial story', MX, 77.8, {
    lineBreak: false,
    height: 36,
  });
  doc.font('Helvetica-Bold').fontSize(22).fillColor(GOLD).text(formatMoney(money.spent, currency), MX, 123.9, {
    lineBreak: false,
    height: 28,
  });
  doc.font('Helvetica').fontSize(10).fillColor(MUTED).text(
    payments === 1 ? 'spent across 1 payment' : `spent across ${payments} payments`,
    MX,
    159.3,
    { lineBreak: false, height: 14 }
  );

  const over = money.target != null && money.spent > money.target;
  const figures: Array<{ label: string; value: string; stripe: string }> = [
    { label: 'BUDGET', value: money.target != null ? formatMoney(money.target, currency) : '—', stripe: INDIGO },
    {
      label: over ? 'OVER BUDGET' : 'REMAINING',
      value: formatMoney(over ? money.spent - (money.target ?? 0) : money.remaining, currency),
      stripe: over ? EMBER : INDIGO,
    },
    { label: 'UNSETTLED', value: formatMoney(money.unsettled, currency), stripe: GREEN },
  ];
  const cardY = 198.4;
  const cardW = 136.1;
  const cardH = 79.4;
  [79.4, 232.4, 385.5].forEach((x, index) => {
    const figure = figures[index];
    if (!figure) return;
    doc.rect(x, cardY, cardW, cardH).fill(CARD);
    doc.rect(x, cardY, cardW, 5.7).fill(figure.stripe);
    doc.font('Helvetica').fontSize(7).fillColor(MUTED).text(figure.label, x + 11.3, cardY + 20.8, {
      width: cardW - 22,
      lineBreak: false,
      height: 10,
    });
    doc.font('Helvetica-Bold').fontSize(14).fillColor(LIGHT).text(figure.value, x + 11.3, cardY + 41.7, {
      width: cardW - 22,
      lineBreak: false,
      height: 18,
    });
  });

  const categories = money.categories.filter((category) => category.amount > 0);
  const spent = money.spent || categories.reduce((sum, category) => sum + category.amount, 0);
  if (categories.length > 0 && spent > 0) {
    drawDonut(doc, 198.4, 507.4, 108, 62.5, categories);
    doc.font('Helvetica-Bold').fontSize(11).fillColor(LIGHT).text(formatCompact(spent, currency), 158.4, 492.6, {
      width: 80,
      align: 'center',
      lineBreak: false,
      height: 16,
    });
    doc.font('Helvetica').fontSize(7).fillColor(MUTED).text('TOTAL', 158.4, 508.9, {
      width: 80,
      align: 'center',
      lineBreak: false,
      height: 10,
    });
    const percents = categoryPercents(
      categories.map((category) => category.amount),
      spent
    );
    const legendTop = 420;
    const step = Math.min(28.4, 190 / categories.length);
    categories.forEach((category, index) => {
      const y = legendTop + index * step;
      doc.fillColor(SLICE[index % SLICE.length]!).circle(340.2, y + 6, 3.5).fill();
      doc.font('Helvetica').fontSize(10).fillColor(LIGHT).text(category.name, 362.8, y, {
        width: 72,
        ellipsis: true,
        lineBreak: false,
        height: 14,
      });
      doc.font('Helvetica').fontSize(9).fillColor(MUTED).text(formatMoney(category.amount, currency), 430, y + 1, {
        width: 62,
        align: 'right',
        lineBreak: false,
        height: 12,
      });
      doc.font('Helvetica-Bold').fontSize(9).fillColor(GOLD).text(`${percents[index] ?? 0}%`, 492, y + 1, {
        width: RIGHT - 492,
        align: 'right',
        lineBreak: false,
        height: 12,
      });
    });
  }

  const largest = [...money.expenses].sort((a, b) => (b.amount || 0) - (a.amount || 0))[0];
  if (largest && largest.amount > 0) {
    const y = 626.5;
    doc.rect(MX, y, CONTENT_W, 79.4).fill(CARD);
    doc.rect(MX, y, CONTENT_W, 5.7).fill(EMBER);
    doc.font('Helvetica-Bold').fontSize(8).fillColor(GOLD).text('ONE PAYMENT STOOD ABOVE THE REST', MX + 17, y + 14.1, {
      lineBreak: false,
      height: 12,
    });
    doc.font('Helvetica-Bold').fontSize(12).fillColor(LIGHT).text(largest.description || largest.category, MX + 17, y + 38, {
      width: 280,
      height: 16,
      ellipsis: true,
      lineBreak: false,
    });
    const when = largest.at ? formatShort(largest.at) : 'Undated';
    doc.font('Helvetica').fontSize(9).fillColor(MUTED).text(`${titleCase(largest.category)}  ·  ${when}`, MX + 17, y + 58, {
      width: 260,
      lineBreak: false,
      height: 12,
    });
    doc.font('Helvetica-Bold').fontSize(14).fillColor(GOLD).text(formatMoney(largest.amount, currency), MX, y + 36, {
      width: CONTENT_W - 17,
      align: 'right',
      lineBreak: false,
      height: 18,
    });
  }

  const note = expenseOutsideNote(snapshot);
  if (note) {
    doc.font('Helvetica').fontSize(7.5).fillColor(MUTED).text(note, MX, 731.8, {
      width: CONTENT_W,
      height: 36,
    });
  }
}

function drawExpenseRow(
  doc: PDFKit.PDFDocument,
  expense: Expense,
  index: number,
  x: number,
  y: number,
  width: number,
  currency: string
): void {
  const number = String(index + 1).padStart(2, '0');
  doc.font('Helvetica').fontSize(7).fillColor(MUTED).text(number, x, y, { lineBreak: false, height: 10 });
  doc.font('Helvetica').fontSize(7).fillColor(GOLD).text(formatStamp(expense.at), x + 22.6, y, {
    lineBreak: false,
    height: 10,
  });
  doc.font('Helvetica').fontSize(7.5).fillColor(LIGHT).text(expense.description || expense.category, x + 62.3, y, {
    width: width - 96,
    height: 10,
    ellipsis: true,
    lineBreak: false,
  });
  doc.font('Helvetica-Bold').fontSize(7.5).fillColor(LIGHT).text(formatMoney(expense.amount, currency), x, y, {
    width,
    align: 'right',
    lineBreak: false,
    height: 10,
  });
  doc.font('Helvetica').fontSize(5.5).fillColor(MUTED).text(expense.category.toUpperCase(), x + 62.3, y + 11, {
    width: width - 70,
    lineBreak: false,
    height: 8,
  });
}

function expenseRecordPages(ctx: PdfCtx, snapshot: StorySnapshot): void {
  const doc = ctx.doc;
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode;
  const ordered = byTime(money.expenses);
  const rowH = 20.4;
  const firstY = 156.9;
  const rowsPerCol = Math.floor((720 - firstY) / rowH);
  const perPage = rowsPerCol * 2;
  const footer = 'MEMORY  ·  FINANCIAL DETAILS';
  const pages = Math.max(1, Math.ceil(ordered.length / perPage));
  for (let page = 0; page < pages; page += 1) {
    addInterior(ctx, footer);
    doc.font('Helvetica-Bold').fontSize(8).fillColor(GOLD).text('03  /  EXPENSE RECORD', MX, 53.8, {
      lineBreak: false,
      height: 12,
    });
    const countLabel = ordered.length === 1 ? 'All 1 payment' : `All ${ordered.length} payments`;
    doc.font('Helvetica-Bold').fontSize(26).fillColor(LIGHT).text(countLabel, MX, 79.9, {
      lineBreak: false,
      height: 32,
    });
    doc.font('Helvetica').fontSize(10).fillColor(MUTED).text('The complete record, side by side.', MX, 119.6, {
      lineBreak: false,
      height: 14,
    });
    const slice = ordered.slice(page * perPage, (page + 1) * perPage);
    if (ordered.length === 0) {
      doc.font('Helvetica').fontSize(11).fillColor(MUTED).text('No payments recorded.', MX, firstY, {
        lineBreak: false,
        height: 16,
      });
    }
    const leftCount = Math.ceil(slice.length / 2);
    const leftW = 289.1 - MX;
    const rightX = 306.1;
    const rightW = RIGHT - rightX;
    slice.forEach((expense, index) => {
      const column = index < leftCount ? 0 : 1;
      const row = column === 0 ? index : index - leftCount;
      const x = column === 0 ? MX : rightX;
      const width = column === 0 ? leftW : rightW;
      drawExpenseRow(doc, expense, page * perPage + index, x, firstY + row * rowH, width, currency);
    });
    if (page === pages - 1) {
      doc.rect(MX, 734.2, CONTENT_W, 34).fill(INDIGO);
      doc.font('Helvetica-Bold').fontSize(9).fillColor(LIGHT).text('TOTAL RECORDED', MX + 17, 747.2, {
        lineBreak: false,
        height: 12,
      });
      doc.font('Helvetica-Bold').fontSize(9).fillColor(LIGHT).text(formatMoney(money.spent, currency), MX, 747.2, {
        width: CONTENT_W - 17,
        align: 'right',
        lineBreak: false,
        height: 12,
      });
    }
  }
}

function closePage(ctx: PdfCtx, snapshot: StorySnapshot, photos: PdfPhoto[]): void {
  const doc = ctx.doc;
  doc.addPage();
  ctx.pageNumber += 1;
  resetInk(doc);
  const background = photos[0];
  if (background) {
    const meta = readImageMeta(background.image);
    paintCover(doc, background.image, meta, 0, 0, doc.page.width, doc.page.height);
    doc.fillColor(NIGHT, 0.78).rect(0, 0, doc.page.width, doc.page.height).fill();
  } else {
    doc.rect(0, 0, doc.page.width, doc.page.height).fill(NIGHT);
  }
  resetInk(doc);
  doc.rect(0, 0, 8.5, doc.page.height).fill(EMBER);

  const began = formatStampYear(snapshot.identity.startAt);
  const ended = formatStampYear(snapshot.identity.completedAt ?? snapshot.identity.endAt);
  const fixed = Boolean(began && ended);
  if (began) {
    centerLine(doc, began, fixed ? 143 : 143, 12, GOLD, 'Helvetica-Bold', 240);
    centerLine(doc, 'This Moment began.', fixed ? 167 : 167, 11, LIGHT, 'Helvetica', 280);
  }
  if (ended) {
    const y = began ? 337 : 220;
    centerLine(doc, ended, y, 12, GOLD, 'Helvetica-Bold', 240);
    centerLine(doc, 'It ended.', y + 24, 11, LIGHT, 'Helvetica', 240);
  }
  const celebration = ['HOUSE_PARTY', 'WEDDING', 'SHARED_EXPERIENCE'].includes(snapshot.identity.familyProfile);
  const headY = fixed ? 418 : ended || began ? 280 : 180;
  if (celebration) {
    centerLine(doc, 'The celebration ended.', headY, 16, LIGHT, 'Helvetica', 400);
    centerLine(doc, 'The Moment stayed.', headY + 24, 18, LIGHT, 'Helvetica-Bold', 400);
  } else {
    centerLine(doc, snapshot.display.closeLine, headY, 18, LIGHT, 'Helvetica-Bold', 420);
  }
  const titleY = fixed ? 632 : headY + 70;
  centerLine(doc, snapshot.identity.title.toUpperCase(), titleY, 11, GOLD, 'Helvetica-Bold', 420);
  centerLine(doc, summaryLine(snapshot, true), titleY + 30, 9, MUTED, 'Helvetica', 460);
  drawWordmarkCentered(doc, fixed ? 711.5 : titleY + 70, 51);
}

async function loadPhotoBuffers(snapshot: StorySnapshot): Promise<PdfPhoto[]> {
  const loaded = await Promise.all(
    snapshot.photos.map(async (photo): Promise<PdfPhoto | null> => {
      if (!photo.url) return null;
      try {
        const response = await fetch(photo.url);
        if (!response.ok) return null;
        return {
          image: Buffer.from(await response.arrayBuffer()) as Buffer,
          title: photo.title?.trim() || null,
          at: photo.at,
        };
      } catch {
        return null;
      }
    })
  );
  return byTime(loaded.filter((photo): photo is PdfPhoto => photo != null));
}

export async function renderStoryPdf(snapshot: StorySnapshot): Promise<Buffer> {
  const photos = await loadPhotoBuffers(snapshot);
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 0, autoFirstPage: true });
    const ctx: PdfCtx = { doc, pageNumber: 1 };
    const chunks: Buffer[] = [];
    doc.on('data', (chunk: Buffer) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);
    coverPage(doc, snapshot);
    momentPages(ctx, snapshot, photos);
    if (!moneyIsEmpty(snapshot.money)) {
      moneyPage(ctx, snapshot);
      expenseRecordPages(ctx, snapshot);
    }
    closePage(ctx, snapshot, photos);
    doc.end();
  });
}
