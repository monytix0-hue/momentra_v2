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

function formatDayHeading(iso: string): string {
  const d = parseInstant(iso);
  if (!d) return 'Undated';
  return new Intl.DateTimeFormat('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
    timeZone: ZONE,
  }).format(d);
}

function dayKey(iso: string | null): string {
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

function metricValue(snapshot: StorySnapshot, key: string): string {
  const currency = snapshot.identity.currencyCode;
  if (key === 'spent') return formatMoney(snapshot.money.spent, currency);
  if (key === 'raised' || key === 'contributed') return formatMoney(snapshot.money.contributed, currency);
  if (key === 'remaining') return formatMoney(snapshot.money.remaining, currency);
  const raw = snapshot.metrics[key];
  return raw == null || raw === '' ? '—' : String(raw);
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

function drawWordmarkChip(doc: PDFKit.PDFDocument, x: number, y: number): void {
  doc.save();
  doc.roundedRect(x, y, 156, 48, 8).fill(INDIGO);
  drawWordmark(doc, x + 12, y + 8, 32, 132);
  doc.restore();
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

function coverPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  const branded = drawWordmark(doc, 48, 40, 56, 180);
  const top = branded ? 112 : 72;
  doc.fillColor(EMBER).fontSize(11).text(snapshot.display.coverEyebrow.toUpperCase(), 48, top, {
    characterSpacing: 1.2,
    lineBreak: false,
  });
  doc.fillColor('#F5F0FF').fontSize(32).text(snapshot.identity.title, 48, top + 28, {
    width: 500,
    height: 70,
    ellipsis: true,
  });
  const place = snapshot.places[0]?.label;
  const dates = dateRange(snapshot.identity.startAt, snapshot.identity.endAt);
  doc.fillColor('#C4BDEE').fontSize(12).text([place, dates].filter(Boolean).join('   '), 48, top + 108, { width: 500 });
  if (snapshot.narrative.opening) {
    doc.fillColor('#F5F0FF').fontSize(13).text(snapshot.narrative.opening, 48, top + 148, {
      width: 480,
      height: 72,
    });
  }
  const tiles = snapshot.display.metricKeys.slice(0, 6);
  if (tiles.length === 0) return;
  const bandH = 92;
  const bandY = doc.page.height - 48 - bandH;
  doc.roundedRect(32, bandY, doc.page.width - 64, bandH, 14).fill('#4B3EA8');
  const cellW = (doc.page.width - 64) / tiles.length;
  tiles.forEach((metric, index) => {
    const x = 32 + index * cellW;
    doc.fillColor('#F5F0FF').fontSize(13).text(metricValue(snapshot, metric.key), x + 6, bandY + 24, {
      width: cellW - 12,
      align: 'center',
      lineBreak: false,
    });
    doc.fillColor('#C4BDEE').fontSize(9).text(metric.label, x + 6, bandY + 48, {
      width: cellW - 12,
      align: 'center',
      lineBreak: false,
    });
  });
}

type PdfPhoto = { image: Buffer; title: string | null };

function momentPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot, photos: PdfPhoto[]): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(CREAM);
  drawWordmarkChip(doc, 48, 24);
  doc.fillColor(INDIGO).fontSize(22).text('The moment', 220, 36, { lineBreak: false });
  let y = 92;
  doc.fontSize(11).fillColor(EMBER).text('People', 48, y, { lineBreak: false });
  y += 18;
  y = drawPeopleChips(doc, snapshot.people, y);
  y += 10;
  if (!room(doc, y, 36)) y = continuePage(doc, 'The moment');
  doc.fillColor(EMBER).fontSize(11).text('Timeline', 48, y, { lineBreak: false });
  y += 16;
  if (snapshot.timeline.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No timeline recorded.', 48, y, { lineBreak: false });
    y += 18;
  } else {
    y = drawTimelineRail(doc, snapshot.timeline, y);
  }
  if (snapshot.decisions.length > 0) {
    y += 8;
    if (!room(doc, y, 32)) y = continuePage(doc, 'The moment');
    doc.fillColor(EMBER).fontSize(11).text('Decisions', 48, y, { lineBreak: false });
    y += 16;
    snapshot.decisions.forEach((decision) => {
      if (!room(doc, y, 18)) y = continuePage(doc, 'The moment');
      doc.fillColor(INK).fontSize(11).text(`${decision.title}  ·  ${decision.status}`, 48, y, {
        width: 500,
        lineBreak: false,
      });
      y += 16;
    });
  }
  if (photos.length > 0) {
    y += 12;
    if (!room(doc, y, 210)) {
      y = continuePage(doc, 'Photos');
    } else {
      doc.fillColor(EMBER).fontSize(11).text('Photos', 48, y, { lineBreak: false });
      y += 18;
    }
    drawPhotoCollage(doc, photos, y);
  }
}

function drawPeopleChips(
  doc: PDFKit.PDFDocument,
  people: StorySnapshot['people'],
  startY: number
): number {
  if (people.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No people recorded.', 48, startY, { lineBreak: false });
    return startY + 18;
  }
  let x = 48;
  let y = startY;
  const maxX = doc.page.width - 48;
  doc.fontSize(9);
  people.forEach((person) => {
    const label = `${person.displayName}  ·  ${person.roleCode}`;
    const chipW = Math.min(Math.max(doc.widthOfString(label) + 20, 48), 240);
    if (x + chipW > maxX) {
      x = 48;
      y += 26;
    }
    if (!room(doc, y, 22)) {
      y = continuePage(doc, 'The moment');
      x = 48;
    }
    doc.roundedRect(x, y, chipW, 20, 10).fill(INDIGO);
    doc.fillColor('#F5F0FF').fontSize(9).text(label, x + 8, y + 5, { width: chipW - 16, lineBreak: false });
    x += chipW + 8;
  });
  return y + 26;
}

function drawTimelineRail(
  doc: PDFKit.PDFDocument,
  timeline: StorySnapshot['timeline'],
  startY: number
): number {
  let y = startY;
  timeline.forEach((item, index) => {
    if (!room(doc, y, 20)) y = continuePage(doc, 'The moment');
    if (index < timeline.length - 1) {
      doc.moveTo(56, y + 8).lineTo(56, y + 20).lineWidth(1.5).strokeColor(EMBER).stroke();
    }
    doc.circle(56, y + 6, 3.5).fill(EMBER);
    const when = formatHumanDate(item.at) ?? item.at.slice(0, 10);
    const detail = item.detail ? `  ·  ${item.detail}` : '';
    doc.fillColor(INK).fontSize(10).text(item.label, 72, y, { width: 300, lineBreak: false });
    doc.fillColor(MUTED).fontSize(9).text(`${when}${detail}`, 376, y + 1, {
      width: 168,
      align: 'right',
      lineBreak: false,
    });
    y += 20;
  });
  return y;
}

function drawPdfPhoto(
  doc: PDFKit.PDFDocument,
  photo: PdfPhoto,
  x: number,
  y: number,
  width: number,
  height: number
): number {
  try {
    doc.image(photo.image, x, y, { fit: [width, height], align: 'center', valign: 'center' });
  } catch {
    // Skip a photo the PDF engine cannot decode.
  }
  const title = photo.title?.trim();
  if (!title) return height;
  doc.fillColor(INK).fontSize(9).text(title, x, y + height + 2, {
    width,
    height: 16,
    ellipsis: true,
    lineBreak: false,
  });
  return height + 18;
}

function drawPhotoCollage(doc: PDFKit.PDFDocument, photos: PdfPhoto[], startY: number): number {
  const fullW = 492;
  const colW = 240;
  const gap = 12;
  let y = startY;
  const place = (needed: number): void => {
    if (!room(doc, y, needed)) y = continuePage(doc, 'Photos');
  };
  const first = photos[0];
  if (!first) return y;
  place(200);
  const firstH = drawPdfPhoto(doc, first, 48, y, fullW, 180);
  y += firstH + gap;
  let index = 1;
  let wide = true;
  while (index < photos.length) {
    const photo = photos[index];
    if (!photo) break;
    if (wide || index === photos.length - 1) {
      place(160);
      const drawn = drawPdfPhoto(doc, photo, 48, y, fullW, 130);
      y += drawn + gap;
      index += 1;
    } else {
      const next = photos[index + 1];
      place(140);
      const left = drawPdfPhoto(doc, photo, 48, y, colW, 110);
      const right = next ? drawPdfPhoto(doc, next, 48 + colW + gap, y, colW, 110) : 0;
      y += Math.max(left, right) + gap;
      index += 2;
    }
    wide = !wide;
  }
  return y;
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

const MONEY_CARD_IDS = new Set(['busiest-day', 'largest-expense', 'top-category']);

function drawMoneyHighlightCards(doc: PDFKit.PDFDocument, snapshot: StorySnapshot, y: number): number {
  const cards = (snapshot.highlights ?? []).filter((item) => MONEY_CARD_IDS.has(item.id));
  if (cards.length === 0) return y;
  const gap = 8;
  const cardW = (499 - gap * (cards.length - 1)) / cards.length;
  const cardH = 78;
  cards.forEach((card, index) => {
    const x = 48 + index * (cardW + gap);
    doc.roundedRect(x, y, cardW, cardH, 8).fill(CREAM);
    doc.fillColor(EMBER).fontSize(9).text(card.title, x + 8, y + 8, { width: cardW - 16, lineBreak: false });
    doc.fillColor(INK).fontSize(10).text(card.detail, x + 8, y + 26, { width: cardW - 16, height: 44 });
  });
  return y + cardH + 16;
}

function moneyPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode;
  doc.addPage();
  drawWordmarkChip(doc, 48, 28);
  doc.fillColor(INDIGO).fontSize(22).text('The money', 220, 40, { lineBreak: false });
  const figuresTop = drawMoneyHighlightCards(doc, snapshot, 92);
  const figures: Array<[string, number]> = [
    ['Contributed', money.contributed],
    ['Spent', money.spent],
    ['Remaining', money.remaining],
    ['Unsettled', money.unsettled],
  ];
  const figureGap = 8;
  const figureW = (499 - figureGap * 3) / 4;
  figures.forEach(([label, amount], index) => {
    const x = 48 + index * (figureW + figureGap);
    doc.roundedRect(x, figuresTop, figureW, 52, 8).fill(CREAM);
    doc.fillColor(INDIGO).fontSize(11).text(formatMoney(amount, currency), x + 6, figuresTop + 8, {
      width: figureW - 12,
      lineBreak: false,
    });
    doc.fillColor(MUTED).fontSize(8).text(label, x + 6, figuresTop + 30, {
      width: figureW - 12,
      lineBreak: false,
    });
  });

  let y = figuresTop + 68;
  const categories = money.categories.filter((c) => c.amount > 0);
  const spent = money.spent || categories.reduce((sum, c) => sum + c.amount, 0);
  if (categories.length > 0 && spent > 0) {
    if (!room(doc, y, 160)) y = continuePage(doc, 'The money');
    doc.fillColor(EMBER).fontSize(12).text('Where the money went', 48, y, { lineBreak: false });
    y += 16;
    const pieTop = y;
    drawDonut(doc, 120, pieTop + 70, 62, categories);
    const percents = categoryPercents(
      categories.map((c) => c.amount),
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
      doc
        .fillColor(INK)
        .fontSize(10)
        .text(
          `${category.name}   ${formatMoney(category.amount, currency)}   ${percents[index] ?? 0}%`,
          226,
          legendY,
          { width: 320, lineBreak: false }
        );
      legendY += 18;
    });
    y = (legendContinued ? legendY : Math.max(pieTop + 150, legendY)) + 12;
  }

  const groups = new Map<string, Expense[]>();
  for (const expense of money.expenses) {
    const key = dayKey(expense.at);
    const bucket = groups.get(key) ?? [];
    bucket.push(expense);
    groups.set(key, bucket);
  }
  const ordered = [...groups.entries()].sort((a, b) => {
    if (a[0] === '') return 1;
    if (b[0] === '') return -1;
    return a[0] < b[0] ? -1 : 1;
  });

  if (!room(doc, y, 56)) {
    y = continuePage(doc, 'Expenses');
  } else {
    doc.fillColor(EMBER).fontSize(12).text('Expenses', 48, y, { lineBreak: false });
    y += 18;
  }
  if (ordered.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No expenses recorded.', 48, y, { lineBreak: false });
    return;
  }
  ordered.forEach(([key, expenses]) => {
    const heading = key ? formatDayHeading(expenses[0]?.at ?? key) : 'Undated';
    const dayTotal = expenses.reduce((sum, expense) => sum + (expense.amount || 0), 0);
    if (!room(doc, y, 40)) y = continuePage(doc, 'Expenses');
    doc.roundedRect(48, y, 499, 28, 6).fill(CREAM);
    doc.fillColor(INDIGO).fontSize(11).text(heading, 60, y + 8, { width: 280, lineBreak: false });
    doc.fillColor(INDIGO).fontSize(11).text(formatMoney(dayTotal, currency), 340, y + 8, {
      width: 190,
      align: 'right',
      lineBreak: false,
    });
    y += 36;
    expenses.forEach((expense) => {
      if (!room(doc, y, 32)) y = continuePage(doc, 'Expenses');
      doc.fillColor(INK).fontSize(11).text(expense.description || expense.category, 48, y, {
        width: 280,
        height: 14,
        ellipsis: true,
        lineBreak: false,
      });
      doc.fillColor(MUTED).fontSize(9).text(`${expense.category}  ·  ${expense.payer}`, 48, y + 13, { width: 280 });
      doc.fillColor(INDIGO).fontSize(11).text(formatMoney(expense.amount, currency), 360, y, { width: 180, align: 'right', lineBreak: false });
      y += 30;
    });
    y += 14;
  });
}

function closePage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  drawWordmark(doc, 48, 48, 56, 180);
  doc.fillColor(EMBER).fontSize(12).text('TOGETHER  ·  FORWARD', 48, 120, { characterSpacing: 1.4, lineBreak: false });
  const celebration = ['HOUSE_PARTY', 'WEDDING', 'SHARED_EXPERIENCE'].includes(snapshot.identity.familyProfile);
  const headline = celebration ? 'The celebration ended.\nThe Moment stayed.' : snapshot.display.closeLine;
  doc.fillColor('#F5F0FF').fontSize(26).text(headline, 48, 148, { width: 500 });
  let awardTop = celebration ? 248 : 230;
  if (celebration) {
    doc.fillColor('#C4BDEE').fontSize(13).text(snapshot.display.closeLine, 48, 230, { width: 500, height: 32 });
    awardTop = 276;
  }
  const awards = snapshot.highlights ?? [];
  const cardW = 242;
  const cardH = 70;
  const gap = 12;
  awards.forEach((award, index) => {
    const col = index % 2;
    const row = Math.floor(index / 2);
    const x = 48 + col * (cardW + gap);
    const y = awardTop + row * (cardH + gap);
    doc.roundedRect(x, y, cardW, cardH, 10).fill('#3F2F78');
    doc.fillColor(EMBER).fontSize(11).text(award.title, x + 12, y + 12, {
      width: cardW - 24,
      height: 16,
      ellipsis: true,
      lineBreak: false,
    });
    doc.fillColor('#F5F0FF').fontSize(10).text(award.detail, x + 12, y + 32, {
      width: cardW - 24,
      height: 28,
    });
  });
  const rows = Math.ceil(awards.length / 2);
  const titleY = awards.length === 0 ? awardTop : awardTop + rows * (cardH + gap) + 8;
  doc.fillColor('#F5F0FF').fontSize(16).text(snapshot.identity.title, 48, titleY, { width: 500 });
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
        };
      } catch {
        return null;
      }
    })
  );
  return loaded.filter((photo): photo is PdfPhoto => photo != null);
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
    if (!moneyIsEmpty(snapshot.money)) moneyPage(doc, snapshot);
    closePage(doc, snapshot);
    doc.end();
  });
}
