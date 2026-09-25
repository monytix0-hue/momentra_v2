import fs from 'fs';
import path from 'path';
import PDFDocument from 'pdfkit';
import type { StorySnapshot } from './snapshot';

const INDIGO = '#2D1F5E';
const EMBER = '#E8621A';
const INK = '#1A0F3D';
const MUTED = '#5C5670';
const CREAM = '#F7F4EF';
const ZONE = 'Asia/Kolkata';
const SLICE = ['#4B3EA8', '#E8621A', '#0F7A6A', '#C4893A', '#6C4EF2', '#B45F3D', '#3D6B9A', '#8A6A4A'];

type Expense = StorySnapshot['money']['expenses'][number];

function formatMoney(amount: number, currency: string): string {
  const code = /^[A-Z]{3}$/.test(currency) ? currency : 'INR';
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: code,
      maximumFractionDigits: 0,
    }).format(Math.round(amount));
  } catch {
    return `${code} ${Math.round(amount)}`;
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

function formatHumanDate(iso: string | null | undefined): string | null {
  const d = parseInstant(iso);
  if (!d) return null;
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: ZONE,
  }).format(d);
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
  const a = parseInstant(start);
  const b = parseInstant(end);
  if (!a && !b) return '';
  if (a && b) {
    const same =
      new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', timeZone: ZONE }).format(a) ===
      new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', timeZone: ZONE }).format(b);
    if (same) {
      const dayA = new Intl.DateTimeFormat('en-GB', { day: 'numeric', timeZone: ZONE }).format(a);
      const rest = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: ZONE }).format(b);
      return `${dayA}–${rest}`;
    }
  }
  return [formatHumanDate(start), formatHumanDate(end)].filter(Boolean).join(' – ');
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
  const d = parseInstant(iso);
  if (!d) return 'Undated';
  const day = new Intl.DateTimeFormat('en-GB', { day: 'numeric', timeZone: ZONE }).format(d);
  const month = new Intl.DateTimeFormat('en-GB', { month: 'short', timeZone: ZONE }).format(d).toUpperCase();
  return `${day} ${month}`;
}

function formatStampYear(iso: string | null | undefined): string | null {
  const d = parseInstant(iso);
  if (!d) return null;
  const year = new Intl.DateTimeFormat('en-GB', { year: 'numeric', timeZone: ZONE }).format(d);
  return `${formatStamp(iso)} ${year}`;
}

function formatCompact(amount: number, currency: string): string {
  const code = /^[A-Z]{3}$/.test(currency) ? currency : 'INR';
  let symbol = `${code} `;
  try {
    const parts = new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: code,
      currencyDisplay: 'narrowSymbol',
      maximumFractionDigits: 0,
    }).formatToParts(0);
    symbol = parts.find((part) => part.type === 'currency')?.value ?? symbol;
  } catch {
    symbol = `${code} `;
  }
  const rounded = Math.round(amount);
  const abs = Math.abs(rounded);
  const body =
    abs >= 100000 ? `${(rounded / 100000).toFixed(1)}L` : abs >= 1000 ? `${(rounded / 1000).toFixed(1)}k` : String(rounded);
  return `${symbol}${body}`;
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

function drawWordmark(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  height: number,
  maxWidth: number
): boolean {
  const logo = storyLogo();
  if (!logo) return false;
  try {
    doc.image(logo, x, y, { fit: [maxWidth, height] });
    return true;
  } catch {
    return false;
  }
}

function room(doc: PDFKit.PDFDocument, y: number, needed: number): boolean {
  return y + needed <= doc.page.height - 36;
}

/** Next page of the same screen. A short label, not a new chapter. */
function continuePage(doc: PDFKit.PDFDocument, label: 'Photos' | 'Expenses' | 'The moment' | 'The money'): number {
  doc.addPage();
  if (label === 'Photos' || label === 'The moment') {
    doc.rect(0, 0, doc.page.width, doc.page.height).fill(CREAM);
  }
  doc.fillColor(EMBER).fontSize(12).text(label, 48, 40, { lineBreak: false });
  return 68;
}

const CONTENT_W = 499;

function coverPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  drawWordmark(doc, 48, 48, 56, 180);
  doc.fillColor(EMBER).fontSize(12).text('MEMORY', 48, 140, { characterSpacing: 2.4, lineBreak: false });
  doc.fillColor('#F5F0FF').fontSize(32).text(snapshot.identity.title, 48, 168, {
    width: 500,
    height: 80,
    ellipsis: true,
  });
  if (snapshot.narrative.opening) {
    doc.fillColor('#C4BDEE').fontSize(14).text(snapshot.narrative.opening, 48, 268, {
      width: 460,
      height: 72,
    });
  }
  const dates = dateRange(snapshot.identity.startAt, snapshot.identity.endAt);
  if (dates) {
    doc.fillColor('#F5F0FF').fontSize(13).text(dates, 48, 360, { width: 500, lineBreak: false });
  }
  doc.fillColor('#C4BDEE').fontSize(12).text(summaryLine(snapshot, false), 48, doc.page.height - 72, {
    width: 500,
    lineBreak: false,
  });
}

type PdfPhoto = { image: Buffer; title: string | null; at: string | null };

type ImageMeta = { width: number; height: number; orientation: number };

function momentPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot, photos: PdfPhoto[]): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(CREAM);
  doc.fillColor(EMBER).fontSize(11).text('01  /  THE MOMENT', 48, 40, { characterSpacing: 1.1, lineBreak: false });
  doc.fillColor(INDIGO).fontSize(26).text('What stayed', 48, 60, { lineBreak: false });
  let y = 108;
  const currency = snapshot.identity.currencyCode;
  const beats: Array<{ when: string; label: string }> = [];
  if (snapshot.identity.startAt) beats.push({ when: formatStamp(snapshot.identity.startAt), label: 'Moment began' });
  if (snapshot.money.target != null) {
    beats.push({ when: formatMoney(snapshot.money.target, currency), label: 'Budget set' });
  }
  const ended = snapshot.identity.completedAt ?? snapshot.identity.endAt;
  if (ended) beats.push({ when: formatStamp(ended), label: 'Moment completed' });
  beats.forEach((beat) => {
    doc.fillColor(EMBER).fontSize(12).text(beat.when, 48, y, { width: 150, lineBreak: false });
    doc.fillColor(INK).fontSize(12).text(beat.label, 210, y, { width: 300, lineBreak: false });
    y += 24;
  });
  if (photos.length > 0) {
    y += 16;
    drawStraightPhotos(doc, photos, y);
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

function photoFrame(meta: ImageMeta, maxHeight: number): { width: number; height: number } {
  const swapped = meta.orientation >= 5;
  const widthPx = swapped ? meta.height : meta.width;
  const heightPx = swapped ? meta.width : meta.height;
  const aspect = widthPx / Math.max(heightPx, 1);
  let width = CONTENT_W;
  let height = width / aspect;
  if (height > maxHeight) {
    height = maxHeight;
    width = height * aspect;
  }
  return { width, height };
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

function maxPhotoHeight(doc: PDFKit.PDFDocument): number {
  return doc.page.height - 68 - 36 - 22;
}

function drawStraightPhotos(doc: PDFKit.PDFDocument, photos: PdfPhoto[], startY: number): void {
  let y = startY;
  photos.forEach((photo) => {
    const meta = readImageMeta(photo.image);
    const frame = photoFrame(meta, maxPhotoHeight(doc));
    const title = photo.title?.trim() || '';
    const titleH = title ? 18 : 0;
    const needed = frame.height + titleH + 16;
    if (!room(doc, y, needed)) y = continuePage(doc, 'Photos');
    const x = 48 + (CONTENT_W - frame.width) / 2;
    paintOriented(doc, photo.image, meta, x, y, frame.width, frame.height);
    if (title) {
      doc.fillColor(INK).fontSize(9).text(title, x, y + frame.height + 2, {
        width: frame.width,
        height: 16,
        ellipsis: true,
        lineBreak: false,
      });
    }
    y += needed;
  });
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
  doc.circle(cx, cy, radius * 0.58).fill('#FFFFFF');
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
  const momentSpan = dateRange(start, end);
  return `Recorded expenses run ${formatHumanDate(earliest)} – ${formatHumanDate(latest)}, while the Moment runs ${momentSpan}.`;
}

function moneyPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode;
  const payments = money.expenses.length;
  doc.addPage();
  doc.fillColor(EMBER).fontSize(11).text('02  /  WHAT IT TOOK', 48, 40, { characterSpacing: 1.1, lineBreak: false });
  doc.fillColor(INDIGO).fontSize(26).text('The financial story', 48, 60, { lineBreak: false });
  doc.fillColor(INDIGO).fontSize(28).text(formatMoney(money.spent, currency), 48, 104, { lineBreak: false });
  doc.fillColor(MUTED).fontSize(12).text(
    payments === 1 ? 'spent across 1 payment' : `spent across ${payments} payments`,
    48,
    140,
    { lineBreak: false }
  );

  const over = money.target != null && money.spent > money.target;
  const figures: Array<[string, string]> = [
    ['Budget', money.target != null ? formatMoney(money.target, currency) : '—'],
    [over ? 'Over budget' : 'Remaining', formatMoney(over ? money.spent - (money.target ?? 0) : money.remaining, currency)],
    ['Unsettled', formatMoney(money.unsettled, currency)],
  ];
  const figureGap = 10;
  const figureW = (CONTENT_W - figureGap * 2) / 3;
  figures.forEach(([label, value], index) => {
    const x = 48 + index * (figureW + figureGap);
    doc.roundedRect(x, 172, figureW, 58, 8).fill(CREAM);
    doc.fillColor(INDIGO).fontSize(13).text(value, x + 8, 182, { width: figureW - 16, lineBreak: false });
    doc.fillColor(MUTED).fontSize(9).text(label, x + 8, 204, { width: figureW - 16, lineBreak: false });
  });

  let y = 250;
  const categories = money.categories.filter((category) => category.amount > 0);
  const spent = money.spent || categories.reduce((sum, category) => sum + category.amount, 0);
  if (categories.length > 0 && spent > 0) {
    if (!room(doc, y, 160)) y = continuePage(doc, 'The money');
    const pieTop = y;
    drawDonut(doc, 118, pieTop + 70, 62, categories);
    doc.fillColor(INDIGO).fontSize(11).text(formatCompact(spent, currency), 78, pieTop + 58, {
      width: 80,
      align: 'center',
      lineBreak: false,
    });
    doc.fillColor(MUTED).fontSize(8).text('TOTAL', 78, pieTop + 74, { width: 80, align: 'center', lineBreak: false });
    const percents = categoryPercents(
      categories.map((category) => category.amount),
      spent
    );
    let legendY = pieTop;
    let legendContinued = false;
    categories.forEach((category, index) => {
      if (!room(doc, legendY, 18)) {
        legendY = continuePage(doc, 'The money');
        legendContinued = true;
      }
      doc.rect(210, legendY + 2, 10, 10).fill(SLICE[index % SLICE.length]!);
      doc.fillColor(INK).fontSize(10).text(
        `${category.name}   ${formatMoney(category.amount, currency)}   ${percents[index] ?? 0}%`,
        226,
        legendY,
        { width: 320, lineBreak: false }
      );
      legendY += 18;
    });
    y = (legendContinued ? legendY : Math.max(pieTop + 150, legendY)) + 12;
  }

  const largest = [...money.expenses].sort((a, b) => (b.amount || 0) - (a.amount || 0))[0];
  if (largest && largest.amount > 0) {
    if (!room(doc, y, 78)) y = continuePage(doc, 'The money');
    doc.roundedRect(48, y, CONTENT_W, 70, 8).fill(CREAM);
    doc.fillColor(EMBER).fontSize(9).text('ONE PAYMENT STOOD ABOVE THE REST', 60, y + 10, { lineBreak: false });
    doc.fillColor(INK).fontSize(13).text(largest.description || largest.category, 60, y + 26, {
      width: 320,
      height: 16,
      ellipsis: true,
      lineBreak: false,
    });
    const when = largest.at ? formatStamp(largest.at) : 'Undated';
    doc.fillColor(MUTED).fontSize(10).text(`${largest.category}  ·  ${when}`, 60, y + 46, { lineBreak: false });
    doc.fillColor(INDIGO).fontSize(14).text(formatMoney(largest.amount, currency), 360, y + 28, {
      width: 170,
      align: 'right',
      lineBreak: false,
    });
    y += 86;
  }

  const note = expenseOutsideNote(snapshot);
  if (note) {
    if (!room(doc, y, 36)) y = continuePage(doc, 'The money');
    doc.fillColor(MUTED).fontSize(10).text(note, 48, y, { width: CONTENT_W });
  }
}

function drawExpenseCell(
  doc: PDFKit.PDFDocument,
  expense: Expense,
  index: number,
  x: number,
  y: number,
  width: number,
  currency: string
): void {
  const number = String(index + 1).padStart(2, '0');
  doc.fillColor(EMBER).fontSize(9).text(`${number}   ${formatStamp(expense.at)}`, x, y, { width, lineBreak: false });
  doc.fillColor(INK).fontSize(10).text(expense.description || expense.category, x, y + 14, {
    width: width - 72,
    height: 14,
    ellipsis: true,
    lineBreak: false,
  });
  doc.fillColor(INDIGO).fontSize(10).text(formatMoney(expense.amount, currency), x, y + 14, {
    width,
    align: 'right',
    lineBreak: false,
  });
  doc.fillColor(MUTED).fontSize(8).text(expense.category.toUpperCase(), x, y + 30, { width, lineBreak: false });
}

function expenseRecordPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode;
  const ordered = byTime(money.expenses);
  doc.addPage();
  doc.fillColor(EMBER).fontSize(11).text('03  /  EXPENSE RECORD', 48, 40, { characterSpacing: 1.1, lineBreak: false });
  const countLabel = ordered.length === 1 ? 'All 1 payment' : `All ${ordered.length} payments`;
  doc.fillColor(INDIGO).fontSize(22).text(countLabel, 48, 60, { lineBreak: false });
  doc.fillColor(MUTED).fontSize(11).text('The complete record, side by side.', 48, 90, { lineBreak: false });
  let y = 118;
  if (ordered.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No payments recorded.', 48, y, { lineBreak: false });
    return;
  }
  const colGap = 16;
  const colW = (CONTENT_W - colGap) / 2;
  const rowH = 52;
  for (let index = 0; index < ordered.length; index += 2) {
    if (!room(doc, y, rowH)) y = continuePage(doc, 'Expenses');
    const left = ordered[index];
    const right = ordered[index + 1];
    if (left) drawExpenseCell(doc, left, index, 48, y, colW, currency);
    if (right) drawExpenseCell(doc, right, index + 1, 48 + colW + colGap, y, colW, currency);
    y += rowH;
  }
  if (!room(doc, y, 28)) y = continuePage(doc, 'Expenses');
  doc.fillColor(INDIGO).fontSize(12).text('TOTAL RECORDED', 48, y, { lineBreak: false });
  doc.fillColor(INDIGO).fontSize(12).text(formatMoney(money.spent, currency), 300, y, {
    width: 247,
    align: 'right',
    lineBreak: false,
  });
}

function closePage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  drawWordmark(doc, 48, 48, 56, 180);
  let y = 140;
  const began = formatStampYear(snapshot.identity.startAt);
  if (began) {
    doc.fillColor(EMBER).fontSize(13).text(began, 48, y, { lineBreak: false });
    doc.fillColor('#F5F0FF').fontSize(16).text('This Moment began.', 200, y, { lineBreak: false });
    y += 32;
  }
  const ended = formatStampYear(snapshot.identity.completedAt ?? snapshot.identity.endAt);
  if (ended) {
    doc.fillColor(EMBER).fontSize(13).text(ended, 48, y, { lineBreak: false });
    doc.fillColor('#F5F0FF').fontSize(16).text('It ended.', 200, y, { lineBreak: false });
    y += 48;
  }
  const celebration = ['HOUSE_PARTY', 'WEDDING', 'SHARED_EXPERIENCE'].includes(snapshot.identity.familyProfile);
  const headline = celebration ? 'The celebration ended.\nThe Moment stayed.' : snapshot.display.closeLine;
  doc.fillColor('#F5F0FF').fontSize(26).text(headline, 48, y, { width: 500, height: 72 });
  y += celebration ? 80 : 48;
  doc.fillColor('#C4BDEE').fontSize(16).text(snapshot.identity.title, 48, y, { width: 500 });
  doc.fillColor('#C4BDEE').fontSize(12).text(summaryLine(snapshot, true), 48, doc.page.height - 72, {
    width: 500,
    lineBreak: false,
  });
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
    const doc = new PDFDocument({ size: 'A4', margin: 48, autoFirstPage: true });
    const chunks: Buffer[] = [];
    doc.on('data', (chunk: Buffer) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);
    coverPage(doc, snapshot);
    momentPage(doc, snapshot, photos);
    if (!moneyIsEmpty(snapshot.money)) {
      moneyPage(doc, snapshot);
      expenseRecordPage(doc, snapshot);
    }
    closePage(doc, snapshot);
    doc.end();
  });
}
