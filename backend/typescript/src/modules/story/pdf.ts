import PDFDocument from 'pdfkit';
import type { StorySnapshot } from './snapshot';

const INDIGO = '#2D1F5E';
const EMBER = '#E8621A';
const INK = '#1A0F3D';
const MUTED = '#5C5670';
const CREAM = '#F7F4EF';

function formatMoney(amount: number, currencyCode: string): string {
  const code = /^[A-Z]{3}$/.test(currencyCode) ? currencyCode : 'INR';
  try {
    return new Intl.NumberFormat('en', {
      style: 'currency',
      currency: code,
      maximumFractionDigits: 0,
    }).format(amount);
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

function footer(doc: PDFKit.PDFDocument, label: string): void {
  doc.fillColor(MUTED).fontSize(9).text(label, 48, 800, { width: 500, align: 'left' });
}

function coverPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  doc.fillColor(EMBER).fontSize(11).text(snapshot.display.coverEyebrow.toUpperCase(), 48, 72, { characterSpacing: 1.2 });
  doc.fillColor('#F5F0FF').fontSize(32).text(snapshot.identity.title, 48, 100, { width: 500 });
  const place = snapshot.places[0]?.label;
  const dates = [snapshot.identity.startAt?.slice(0, 10), snapshot.identity.endAt?.slice(0, 10)]
    .filter(Boolean)
    .join('  ·  ');
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
    const raw = snapshot.metrics[metric.key];
    const value =
      metric.key === 'spent' || metric.key === 'raised' || metric.key === 'remaining'
        ? formatMoney(Number(raw) || 0, snapshot.identity.currencyCode)
        : String(raw ?? '—');
    doc.fillColor('#F5F0FF').fontSize(16).text(value, x + 12, y + 14, { width: 126 });
    doc.fontSize(10).fillColor('#C4BDEE').text(metric.label, x + 12, y + 38, { width: 126 });
    x += 166;
  });
  footer(doc, snapshot.display.displayLabel);
}

function togetherPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, 88).fill(CREAM);
  doc.fillColor(INDIGO).fontSize(22).text('Together', 48, 40);
  let y = 110;
  doc.fontSize(12).fillColor(EMBER).text('People', 48, y);
  y += 18;
  const people = snapshot.people.slice(0, 12);
  doc.fillColor(INK).fontSize(11);
  if (people.length === 0) {
    doc.fillColor(MUTED).text('No people recorded.', 48, y);
    y += 20;
  } else {
    people.forEach((person) => {
      doc.fillColor(INK).text(`${person.displayName}  ·  ${person.roleCode}`, 48, y, { width: 500 });
      y += 16;
    });
  }
  y += 12;
  doc.fillColor(EMBER).fontSize(12).text('Timeline', 48, y);
  y += 18;
  snapshot.timeline.slice(0, 8).forEach((item) => {
    doc.rect(48, y + 4, 8, 8).fill(EMBER);
    doc.fillColor(INK).fontSize(11).text(item.label, 66, y, { width: 460 });
    doc.fillColor(MUTED).fontSize(9).text(`${item.at.slice(0, 10)}${item.detail ? `  ·  ${item.detail}` : ''}`, 66, y + 14, { width: 460 });
    y += 32;
  });
  if (snapshot.decisions.length > 0 && y < 720) {
    y += 8;
    doc.fillColor(EMBER).fontSize(12).text('Decisions', 48, y);
    y += 18;
    snapshot.decisions.slice(0, 6).forEach((decision) => {
      doc.fillColor(INK).fontSize(11).text(`${decision.title}  ·  ${decision.status}`, 48, y, { width: 500 });
      y += 16;
    });
  }
}

function moneyPage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  const money = snapshot.money;
  const currency = snapshot.identity.currencyCode || 'INR';
  doc.addPage();
  doc.fillColor(INDIGO).fontSize(22).text('Money', 48, 48);
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
    doc.fillColor(INDIGO).fontSize(14).text(formatMoney(amount, currency), x + 12, y + 8, { width: 206 });
    doc.fillColor(MUTED).fontSize(10).text(label, x + 12, y + 28);
  });

  let y = 220;
  const categories = money.categories.filter((c) => c.amount > 0).slice(0, 6);
  if (categories.length > 0) {
    doc.fillColor(EMBER).fontSize(12).text('By category', 48, y);
    y += 20;
    const max = Math.max(...categories.map((c) => c.amount), 1);
    categories.forEach((category) => {
      const width = Math.max(8, (category.amount / max) * 280);
      doc.roundedRect(48, y, width, 14, 4).fill('#4B3EA8');
      doc.fillColor(INK).fontSize(10).text(`${category.name}   ${formatMoney(category.amount, currency)}`, 340, y, { width: 210 });
      y += 22;
    });
    y += 10;
  }

  doc.fillColor(EMBER).fontSize(12).text('Expenses', 48, y);
  y += 18;
  const expenses = money.expenses.slice(0, 12);
  if (expenses.length === 0) {
    doc.fillColor(MUTED).fontSize(11).text('No expenses recorded.', 48, y);
    return;
  }
  expenses.forEach((expense) => {
    if (y > 760) return;
    doc.fillColor(INK).fontSize(11).text(expense.description || expense.category, 48, y, { width: 280 });
    doc.fillColor(MUTED).fontSize(9).text(`${expense.category}  ·  ${expense.payer}`, 48, y + 13, { width: 280 });
    doc.fillColor(INDIGO).fontSize(11).text(formatMoney(expense.amount, currency), 360, y, { width: 180, align: 'right' });
    y += 30;
  });
}

function closePage(doc: PDFKit.PDFDocument, snapshot: StorySnapshot): void {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(INDIGO);
  doc.fillColor(EMBER).fontSize(12).text('TOGETHER  ·  FORWARD', 48, 220, { characterSpacing: 1.4 });
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
    const doc = new PDFDocument({ size: 'A4', margin: 48 });
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
