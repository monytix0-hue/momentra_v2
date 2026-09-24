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

function formatRs(amount: number): string {
  const grouped = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(Math.round(amount));
  return `Rs ${grouped}`;
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
  if (key === 'spent') return formatRs(snapshot.money.spent);
  if (key === 'raised' || key === 'contributed') return formatRs(snapshot.money.contributed);
  if (key === 'remaining') return formatRs(snapshot.money.remaining);
  const raw = snapshot.metrics[key];
  return raw == null || raw === '' ? '—' : String(raw);
}

function ensure(doc: PDFKit.PDFDocument, y: number, needed: number): number {
  if (y + needed <= doc.page.height - 48) return y;
  doc.addPage();
  return 48;
}

function coverPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  doc.fillColor(EMBER).fontSize(11).text(snapshot.display.coverEyebrow.toUpperCase(), 48, 72, {
    characterSpacing: 1.2,
    lineBreak: false,
  });
  doc.fillColor('#F5F0FF').fontSize(32).text(snapshot.identity.title, 48, 100, { width: 500 });
  const place = snapshot.places[0]?.label;
  const dates = dateRange(snapshot.identity.startAt, snapshot.identity.endAt);
  doc.fillColor('#C4BDEE').fontSize(12).text([place, dates].filter(Boolean).join('   '), 48, 180, { width: 500 });
  if (snapshot.narrative.opening) {
    doc.fillColor('#F5F0FF').fontSize(13).text(snapshot.narrative.opening, 48, 220, { width: 480 });
  }
  const tiles = snapshot.display.metricKeys.slice(0, 6);
  let x = 48;
  let y = 320;
  tiles.forEach((metric, index) => {
    if (index === 3) {
      x = 48;
      y += 78;
    }
    doc.roundedRect(x, y, 150, 64, 8).fill('#4B3EA8');
    doc.fillColor('#F5F0FF').fontSize(16).text(metricValue(snapshot, metric.key), x + 12, y + 14, {
      width: 126,
      lineBreak: false,
    });
    doc.fontSize(10).fillColor('#C4BDEE').text(metric.label, x + 12, y + 38, { width: 126, lineBreak: false });
    x += 166;
  });
}

function togetherPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, 88).fill(CREAM);
  doc.fillColor(INDIGO).fontSize(22).text('Together', 48, 40, { lineBreak: false });
  let y = 110;
  doc.fontSize(12).fillColor(EMBER).text('People', 48, y, { lineBreak: false });
  y += 18;
  const people = snapshot.people;
  if (people.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No people recorded.', 48, y, { lineBreak: false });
    y += 20;
  } else {
    people.forEach((person) => {
      y = ensure(doc, y, 20);
      doc.fillColor(INK).fontSize(11).text(`${person.displayName}  ·  ${person.roleCode}`, 48, y, { width: 500 });
      y += 16;
    });
  }
  y += 12;
  y = ensure(doc, y, 36);
  doc.fillColor(EMBER).fontSize(12).text('Timeline', 48, y, { lineBreak: false });
  y += 18;
  snapshot.timeline.forEach((item) => {
    y = ensure(doc, y, 36);
    doc.rect(48, y + 4, 8, 8).fill(EMBER);
    doc.fillColor(INK).fontSize(11).text(item.label, 66, y, { width: 460 });
    const when = formatHumanDate(item.at) ?? item.at.slice(0, 10);
    doc.fillColor(MUTED).fontSize(9).text(`${when}${item.detail ? `  ·  ${item.detail}` : ''}`, 66, y + 14, { width: 460 });
    y += 32;
  });
  if (snapshot.decisions.length > 0) {
    y += 8;
    y = ensure(doc, y, 36);
    doc.fillColor(EMBER).fontSize(12).text('Decisions', 48, y, { lineBreak: false });
    y += 18;
    snapshot.decisions.forEach((decision) => {
      y = ensure(doc, y, 20);
      doc.fillColor(INK).fontSize(11).text(`${decision.title}  ·  ${decision.status}`, 48, y, { width: 500 });
      y += 16;
    });
  }
}

function drawDonut(
  doc: PDFKit.PDFDocument,
  cx: number,
  cy: number,
  radius: number,
  slices: Array<{ amount: number }>
): void {
  const total = slices.reduce((sum, slice) => sum + slice.amount, 0) || 1;
  if (slices.length === 1) {
    doc.circle(cx, cy, radius).fill(SLICE[0]!);
  } else {
    let angle = -Math.PI / 2;
    slices.forEach((slice, index) => {
      const sweep = (slice.amount / total) * Math.PI * 2;
      if (sweep <= 0) return;
      const end = angle + sweep;
      const x1 = cx + radius * Math.cos(angle);
      const y1 = cy + radius * Math.sin(angle);
      const x2 = cx + radius * Math.cos(end);
      const y2 = cy + radius * Math.sin(end);
      const large = sweep > Math.PI ? 1 : 0;
      doc
        .fillColor(SLICE[index % SLICE.length]!)
        .path(`M ${cx} ${cy} L ${x1} ${y1} A ${radius} ${radius} 0 ${large} 1 ${x2} ${y2} Z`)
        .fill();
      angle = end;
    });
  }
  doc.circle(cx, cy, radius * 0.58).fill('#FFFFFF');
}

function moneyPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  const money = snapshot.money;
  doc.addPage();
  doc.fillColor(INDIGO).fontSize(22).text('Money', 48, 48, { lineBreak: false });
  const figures: Array<[string, number]> = [
    ['Contributed', money.contributed],
    ['Spent', money.spent],
    ['Remaining', money.remaining],
    ['Unsettled', money.unsettled],
  ];
  figures.forEach(([label, amount], index) => {
    const x = 48 + (index % 2) * 250;
    const y = 90 + Math.floor(index / 2) * 58;
    doc.roundedRect(x, y, 230, 48, 8).fill(CREAM);
    doc.fillColor(INDIGO).fontSize(14).text(formatRs(amount), x + 12, y + 8, { width: 206, lineBreak: false });
    doc.fillColor(MUTED).fontSize(10).text(label, x + 12, y + 28, { lineBreak: false });
  });

  let y = 220;
  const categories = money.categories.filter((c) => c.amount > 0);
  const spent = money.spent || categories.reduce((sum, c) => sum + c.amount, 0);
  if (categories.length > 0 && spent > 0) {
    doc.fillColor(EMBER).fontSize(12).text('Where the money went', 48, y, { lineBreak: false });
    y += 16;
    const pieTop = y;
    drawDonut(doc, 120, pieTop + 70, 62, categories);
    const percents = categoryPercents(
      categories.map((c) => c.amount),
      spent
    );
    let legendY = pieTop;
    categories.forEach((category, index) => {
      legendY = ensure(doc, legendY, 18);
      doc.rect(210, legendY + 2, 10, 10).fill(SLICE[index % SLICE.length]!);
      doc
        .fillColor(INK)
        .fontSize(10)
        .text(
          `${category.name}   ${formatRs(category.amount)}   ${percents[index] ?? 0}%`,
          226,
          legendY,
          { width: 320, lineBreak: false }
        );
      legendY += 18;
    });
    y = Math.max(pieTop + 150, legendY + 12);
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

  y = ensure(doc, y, 28);
  doc.fillColor(EMBER).fontSize(12).text('Expenses', 48, y, { lineBreak: false });
  y += 18;
  if (ordered.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No expenses recorded.', 48, y, { lineBreak: false });
    return;
  }
  ordered.forEach(([key, expenses]) => {
    const heading = key ? formatDayHeading(expenses[0]?.at ?? key) : 'Undated';
    const dayTotal = expenses.reduce((sum, expense) => sum + (expense.amount || 0), 0);
    y = ensure(doc, y, 48);
    doc.fillColor(INDIGO).fontSize(12).text(heading, 48, y, { width: 320, lineBreak: false });
    doc.fillColor(MUTED).fontSize(11).text(formatRs(dayTotal), 360, y, { width: 180, align: 'right', lineBreak: false });
    y += 18;
    expenses.forEach((expense) => {
      y = ensure(doc, y, 32);
      doc.fillColor(INK).fontSize(11).text(expense.description || expense.category, 48, y, { width: 280 });
      doc.fillColor(MUTED).fontSize(9).text(`${expense.category}  ·  ${expense.payer}`, 48, y + 13, { width: 280 });
      doc.fillColor(INDIGO).fontSize(11).text(formatRs(expense.amount), 360, y, { width: 180, align: 'right', lineBreak: false });
      y += 30;
    });
    y += 8;
  });
}

function closePage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  doc.fillColor(EMBER).fontSize(12).text('TOGETHER  ·  FORWARD', 48, 220, { characterSpacing: 1.4, lineBreak: false });
  const celebration = ['HOUSE_PARTY', 'WEDDING', 'SHARED_EXPERIENCE'].includes(snapshot.identity.familyProfile);
  const headline = celebration ? 'The celebration ended.\nThe Moment stayed.' : snapshot.display.closeLine;
  doc.fillColor('#F5F0FF').fontSize(26).text(headline, 48, 260, { width: 500 });
  if (celebration) {
    doc.fillColor('#C4BDEE').fontSize(14).text(snapshot.display.closeLine, 48, 360, { width: 500 });
  }
  doc.fillColor('#F5F0FF').fontSize(16).text(snapshot.identity.title, 48, 430, { width: 500 });
}

export function renderStoryPdf(snapshot: StorySnapshot): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 48, autoFirstPage: true });
    const chunks: Buffer[] = [];
    doc.on('data', (chunk: Buffer) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);
    coverPage(doc, snapshot);
    togetherPage(doc, snapshot);
    if (!moneyIsEmpty(snapshot.money)) moneyPage(doc, snapshot);
    closePage(doc, snapshot);
    doc.end();
  });
}
