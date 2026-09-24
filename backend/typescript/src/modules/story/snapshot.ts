import type { PoolClient } from 'pg';
import {
  getStoryComposer,
  resolveStoryFamilyProfile,
  type StoryChapterId,
  type StoryFamilyProfile,
} from './profiles';
import { trySignedDownloadUrl } from '../media/service';

export interface StorySnapshot {
  identity: {
    momentId: string;
    title: string;
    momentTypeCode: string | null;
    groupFamily: string | null;
    familyProfile: StoryFamilyProfile;
    startAt: string | null;
    endAt: string | null;
    completedAt: string | null;
    currencyCode: string;
  };
  people: Array<{ userId: string | null; displayName: string; roleCode: string }>;
  metrics: Record<string, string | number>;
  timeline: Array<{ at: string; label: string; detail?: string }>;
  money: {
    contributed: number;
    spent: number;
    remaining: number;
    unsettled: number;
    target: number | null;
    categories: Array<{ name: string; amount: number }>;
    contributors: Array<{ name: string; amount: number }>;
    payers: Array<{ name: string; amount: number; payments: number }>;
    expenses: Array<{ description: string; category: string; payer: string; amount: number; at: string | null }>;
  };
  places: Array<{ label: string; startAt: string | null; endAt: string | null }>;
  decisions: Array<{ title: string; status: string }>;
  memories: Array<{ text: string | null; mediaUrl: string | null; at: string | null }>;
  photos: Array<{ url: string; at: string | null; title: string | null; mediaId?: string | null }>;
  highlights?: Array<{ id: string; title: string; detail: string }>;
  narrative: { opening: string; insights: string[] };
  chapters: StoryChapterId[];
  display: ReturnType<typeof getStoryComposer>;
}

/** Resolve category from category_code or client-embedded "note | Category" description. */
export function resolveExpenseCategory(
  categoryCode: string | null | undefined,
  description: string | null | undefined
): { category: string; cleanDescription: string } {
  const raw = description?.trim() ?? '';
  const sep = ' | ';
  const idx = raw.lastIndexOf(sep);
  let parsedCat: string | null = null;
  let note = raw;
  if (idx >= 0) {
    parsedCat = raw.slice(idx + sep.length).trim() || null;
    note = raw.slice(0, idx).trim();
  }
  const code = categoryCode?.trim();
  const category =
    code && code.toLowerCase() !== 'other'
      ? code
      : parsedCat || code || 'Other';
  return {
    category,
    cleanDescription: note || category || 'Expense',
  };
}

function asIso(value: Date | string | null | undefined): string | null {
  if (!value) return null;
  const parsed = value instanceof Date ? value : new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString();
}

/** Posted group expenses, matching Group Finance (`expense_total`). */
export async function loadFreshStoryMoney(
  client: PoolClient,
  momentId: string
): Promise<StorySnapshot['money']> {
  const expenseRows = await client.query<{
    description: string | null;
    amount: string;
    paid_by_name: string | null;
    category_code: string | null;
    occurred_at: Date | string | null;
  }>(
    `SELECT e.description, e.amount::text,
            COALESCE(e.effective_at, e.posted_at, e.created_at) AS occurred_at,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Someone') AS paid_by_name,
            e.category_code
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g
       ON g.expense_id = e.expense_id AND g.moment_id = e.moment_id
     LEFT JOIN collaboration.moment_participant mp
       ON mp.participant_id = g.paid_by_participant_id AND mp.moment_id = e.moment_id
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE e.moment_id = $1::uuid
       AND e.domain_code = 'GROUP'
       AND e.status = 'POSTED'
     ORDER BY COALESCE(e.effective_at, e.posted_at, e.created_at) ASC`,
    [momentId]
  );

  const expenses = expenseRows.rows.map((row) => {
    const { category, cleanDescription } = resolveExpenseCategory(row.category_code, row.description);
    return {
      description: cleanDescription,
      category,
      payer: row.paid_by_name ?? 'Someone',
      amount: parseFloat(row.amount) || 0,
      at: asIso(row.occurred_at),
    };
  });

  const contribRows = await client.query<{ amount: string; name: string | null }>(
    `SELECT c.amount::text,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Someone') AS name
     FROM finance.contribution c
     LEFT JOIN collaboration.moment_participant mp
       ON mp.participant_id = c.participant_id AND mp.moment_id = c.moment_id
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE c.moment_id = $1 AND c.status = 'RECORDED'`,
    [momentId]
  );

  const budgetRow = await client.query<{ amount: string }>(
    `SELECT amount::text FROM finance.budget
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY updated_at DESC
     LIMIT 1`,
    [momentId]
  );

  const outstandingRow = await client.query<{ outstanding: string }>(
    `SELECT COALESCE(SUM(outstanding_total), 0)::text AS outstanding
     FROM projection.group_finance_snapshot
     WHERE moment_id = $1`,
    [momentId]
  );

  const spent = expenses.reduce((sum, expense) => sum + expense.amount, 0);
  const contributed = contribRows.rows.reduce((sum, row) => sum + (parseFloat(row.amount) || 0), 0);
  const outstanding = parseFloat(outstandingRow.rows[0]?.outstanding ?? '0') || 0;
  const target = budgetRow.rows[0] ? parseFloat(budgetRow.rows[0].amount) || null : null;
  const remaining = target != null ? Math.max(0, target - spent) : outstanding;

  const categoryMap = new Map<string, number>();
  const payerMap = new Map<string, { amount: number; payments: number }>();
  for (const expense of expenses) {
    const category = expense.category.trim() || 'Other';
    categoryMap.set(category, (categoryMap.get(category) ?? 0) + expense.amount);
    const payer = payerMap.get(expense.payer) ?? { amount: 0, payments: 0 };
    payer.amount += expense.amount;
    payer.payments += 1;
    payerMap.set(expense.payer, payer);
  }
  const contribMap = new Map<string, number>();
  for (const row of contribRows.rows) {
    const name = row.name ?? 'Someone';
    contribMap.set(name, (contribMap.get(name) ?? 0) + (parseFloat(row.amount) || 0));
  }

  return {
    contributed,
    spent,
    remaining,
    unsettled: outstanding,
    target,
    categories: [...categoryMap.entries()]
      .map(([name, amount]) => ({ name, amount }))
      .sort((a, b) => b.amount - a.amount),
    contributors: [...contribMap.entries()].map(([name, amount]) => ({ name, amount })),
    payers: [...payerMap.entries()].map(([name, value]) => ({ name, amount: value.amount, payments: value.payments })),
    expenses,
  };
}

/** Swap frozen money for the live Group Finance figures and refresh spend lines. */
export function applyFreshStoryMoney(snapshot: StorySnapshot, money: StorySnapshot['money']): StorySnapshot {
  const spentLine = money.spent > 0 ? `Together you moved ${formatInr(money.spent)} through the moment.` : null;
  const insights = (snapshot.narrative?.insights ?? []).filter(
    (line) => !line.startsWith('Together you moved ') && !line.includes('accounted for more than half')
  );
  if (spentLine) insights.splice(Math.min(1, insights.length), 0, spentLine);
  if (money.categories.length >= 2 && money.spent > 0) {
    const topShare = (money.categories[0]!.amount + money.categories[1]!.amount) / money.spent;
    if (topShare >= 0.5) {
      insights.push(
        `${money.categories[0]!.name} and ${money.categories[1]!.name} together accounted for more than half of the shared spend.`
      );
    }
  }
  return {
    ...snapshot,
    metrics: {
      ...snapshot.metrics,
      bills: money.expenses.length,
      spent: formatInr(money.spent),
      spentRaw: money.spent,
      contributed: formatInr(money.contributed),
      raised: formatInr(money.contributed > 0 ? money.contributed : money.target ?? money.spent),
      target: money.target != null ? formatInr(money.target) : '—',
      remaining: formatInr(money.remaining),
    },
    money,
    highlights: buildStoryHighlights(snapshot, money),
    narrative: {
      opening: snapshot.narrative?.opening ?? '',
      insights,
    },
  };
}

/** Re-parse frozen expense categories (fixes "Other" when note embeds "| Food"). */
export function hydrateStoryMoneyCategories(snapshot: StorySnapshot): StorySnapshot {
  const money = snapshot.money;
  if (!money?.expenses?.length) return snapshot;
  const expenses = money.expenses.map((e) => {
    const { category, cleanDescription } = resolveExpenseCategory(
      e.category === 'Other' ? null : e.category,
      e.description
    );
    return {
      ...e,
      description: cleanDescription,
      category,
    };
  });
  const categoryMap = new Map<string, number>();
  for (const e of expenses) {
    const cat = e.category.trim() || 'Other';
    categoryMap.set(cat, (categoryMap.get(cat) ?? 0) + (e.amount || 0));
  }
  const categories = [...categoryMap.entries()]
    .map(([name, amount]) => ({ name, amount }))
    .sort((a, b) => b.amount - a.amount);
  return {
    ...snapshot,
    money: {
      ...money,
      expenses,
      categories: categories.length > 0 ? categories : money.categories,
    },
  };
}

/** Fresh signed photo URLs for a moment (story read path). */
export async function loadFreshStoryPhotos(
  client: PoolClient,
  momentId: string
): Promise<StorySnapshot['photos']> {
  const photoMedia = await client.query<{
    media_upload_id: string;
    bucket: string | null;
    object_key: string | null;
    created_at: Date | null;
    title: string | null;
  }>(
    `SELECT mu.media_upload_id, mu.bucket, mu.object_key, me.created_at, m.title
     FROM memory.memory_evidence me
     JOIN memory.memory m ON m.memory_id = me.memory_id
     JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
     WHERE m.moment_id = $1
       AND m.status = 'ACTIVE'
       AND me.source_type = 'MEDIA'
       AND mu.status = 'COMPLETED'
       AND mu.bucket IS NOT NULL
       AND mu.object_key IS NOT NULL
     ORDER BY me.created_at DESC`,
    [momentId]
  ).catch(() => ({
    rows: [] as Array<{
      media_upload_id: string;
      bucket: string | null;
      object_key: string | null;
      created_at: Date | null;
      title: string | null;
    }>,
  }));

  const photos: StorySnapshot['photos'] = [];
  for (const row of photoMedia.rows) {
    if (!row.bucket || !row.object_key) continue;
    const url = await trySignedDownloadUrl(row.bucket, row.object_key);
    if (url) {
      photos.push({
        url,
        at: row.created_at?.toISOString() ?? null,
        title: row.title?.trim() || null,
        mediaId: row.media_upload_id,
      });
    }
  }
  return photos;
}

function daysBetween(start: string | null, end: string | null): number {
  if (!start || !end) return 0;
  const a = Date.parse(start);
  const b = Date.parse(end);
  if (!Number.isFinite(a) || !Number.isFinite(b) || b < a) return 0;
  return Math.max(1, Math.round((b - a) / (24 * 3600 * 1000)) + 1);
}

function monthsBetween(start: string | null, end: string | null): number {
  if (!start || !end) return 0;
  const a = new Date(start);
  const b = new Date(end);
  if (Number.isNaN(a.getTime()) || Number.isNaN(b.getTime())) return 0;
  return Math.max(1, (b.getFullYear() - a.getFullYear()) * 12 + (b.getMonth() - a.getMonth()) + 1);
}

function hoursBetween(start: string | null, end: string | null): number {
  if (!start || !end) return 0;
  const a = Date.parse(start);
  const b = Date.parse(end);
  if (!Number.isFinite(a) || !Number.isFinite(b) || b < a) return 0;
  return Math.max(1, Math.round((b - a) / 3600000));
}

const STORY_ZONE = 'Asia/Kolkata';

function formatStoryAmount(amount: number, currency: string): string {
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

function storyDayKey(iso: string): string {
  return new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: STORY_ZONE,
  }).format(new Date(iso));
}

function storyDayLabel(iso: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
    timeZone: STORY_ZONE,
  }).format(new Date(iso));
}

/** Awards for the money chapter and the close page. Skip any fact that has no data. */
export function buildStoryHighlights(
  snapshot: StorySnapshot,
  money: StorySnapshot['money']
): NonNullable<StorySnapshot['highlights']> {
  const currency = snapshot.identity?.currencyCode || 'INR';
  const highlights: NonNullable<StorySnapshot['highlights']> = [];

  const byDay = new Map<string, { total: number; sample: string }>();
  for (const expense of money.expenses) {
    if (!expense.at || expense.amount <= 0) continue;
    const key = storyDayKey(expense.at);
    const current = byDay.get(key) ?? { total: 0, sample: expense.at };
    current.total += expense.amount;
    byDay.set(key, current);
  }
  let busiest: { total: number; sample: string } | null = null;
  for (const day of byDay.values()) {
    if (!busiest || day.total > busiest.total) busiest = day;
  }
  if (busiest && busiest.total > 0) {
    highlights.push({
      id: 'busiest-day',
      title: 'Busiest day',
      detail: `${storyDayLabel(busiest.sample)} · ${formatStoryAmount(busiest.total, currency)}`,
    });
  }

  const largest = money.expenses.reduce<(typeof money.expenses)[number] | null>(
    (best, expense) => (expense.amount > (best?.amount ?? 0) ? expense : best),
    null
  );
  if (largest && largest.amount > 0) {
    highlights.push({
      id: 'largest-expense',
      title: 'The one they will remember',
      detail: `${largest.description} · ${largest.payer} · ${formatStoryAmount(largest.amount, currency)}`,
    });
  }

  const topCategory = money.categories[0];
  if (topCategory && topCategory.amount > 0 && money.spent > 0) {
    const share = Math.round((topCategory.amount / money.spent) * 100);
    highlights.push({
      id: 'top-category',
      title: 'Where it went',
      detail: `${topCategory.name} · ${share}% of the spend`,
    });
  }

  const topPayer = [...money.payers].sort((a, b) => b.amount - a.amount)[0];
  if (topPayer && topPayer.amount > 0) {
    const payments = topPayer.payments === 1 ? '1 payment' : `${topPayer.payments} payments`;
    highlights.push({
      id: 'top-payer',
      title: 'Who carried it',
      detail: `${topPayer.name} · ${formatStoryAmount(topPayer.amount, currency)} · ${payments}`,
    });
  }

  const people = snapshot.people?.length ?? 0;
  const photos = (snapshot.photos ?? []).filter((photo) => photo.url).length;
  if (people > 0 || photos > 0) {
    const parts: string[] = [];
    if (people > 0) parts.push(people === 1 ? '1 person' : `${people} people`);
    if (photos > 0) parts.push(photos === 1 ? '1 photo' : `${photos} photos`);
    highlights.push({
      id: 'crew',
      title: 'The crew',
      detail: parts.join(' · '),
    });
  }

  return highlights;
}

function formatInr(n: number): string {
  if (!Number.isFinite(n)) return '₹0';
  if (Math.abs(n) >= 100000) return `₹${(n / 100000).toFixed(1)}L`;
  if (Math.abs(n) >= 1000) return `₹${Math.round(n / 1000)}k`;
  return `₹${Math.round(n)}`;
}

export async function buildMomentStorySnapshot(
  client: PoolClient,
  momentId: string
): Promise<{ snapshot: StorySnapshot; sourceManifest: Record<string, number> }> {
  const moment = await client.query<{
    title: string;
    status: string;
    start_at: Date | null;
    end_at: Date | null;
    completed_at: Date | null;
    moment_type_code: string | null;
    group_family: string | null;
  }>(
    `SELECT m.title, m.status, m.start_at, m.end_at, m.completed_at,
            mt.code AS moment_type_code, gmc.group_family
     FROM core.moment m
     LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     WHERE m.moment_id = $1`,
    [momentId]
  );
  const row = moment.rows[0];
  if (!row) {
    throw new Error('Moment not found for story snapshot');
  }

  const familyProfile = resolveStoryFamilyProfile({
    groupFamily: row.group_family,
    momentTypeCode: row.moment_type_code,
  });
  const composer = getStoryComposer(familyProfile);

  const peopleRows = await client.query<{
    user_id: string | null;
    display_name: string | null;
    participant_role: string;
  }>(
    `SELECT mp.user_id, mp.participant_role,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Guest') AS display_name
     FROM collaboration.moment_participant mp
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE mp.moment_id = $1 AND mp.status = 'ACTIVE'`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ user_id: string | null; display_name: string | null; participant_role: string }> }));

  const people = peopleRows.rows.map((p) => ({
    userId: p.user_id,
    displayName: p.display_name ?? 'Guest',
    roleCode: p.participant_role,
  }));

  const expenseRows = await client.query<{
    description: string | null;
    amount: string;
    paid_by_name: string | null;
    category_code: string | null;
    occurred_at: Date | null;
    currency_code: string;
  }>(
    `SELECT e.description, e.amount::text, e.currency_code,
            COALESCE(e.effective_at, e.posted_at, e.created_at) AS occurred_at,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Someone') AS paid_by_name,
            e.category_code
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g
       ON g.expense_id = e.expense_id AND g.moment_id = e.moment_id
     LEFT JOIN collaboration.moment_participant mp
       ON mp.participant_id = g.paid_by_participant_id AND mp.moment_id = e.moment_id
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE e.moment_id = $1::uuid
       AND e.domain_code = 'GROUP'
       AND e.status = 'POSTED'
     ORDER BY COALESCE(e.effective_at, e.posted_at, e.created_at) ASC`,
    [momentId]
  );

  const resolvedExpenses = expenseRows.rows.map((e) => {
    const { category, cleanDescription } = resolveExpenseCategory(e.category_code, e.description);
    return {
      description: cleanDescription,
      amount: parseFloat(e.amount) || 0,
      paid_by_name: e.paid_by_name,
      category,
      at: asIso(e.occurred_at),
    };
  });

  const contribRows = await client.query<{
    amount: string;
    name: string | null;
  }>(
    `SELECT c.amount::text,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Someone') AS name
     FROM finance.contribution c
     LEFT JOIN collaboration.moment_participant mp
       ON mp.participant_id = c.participant_id AND mp.moment_id = c.moment_id
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE c.moment_id = $1 AND c.status = 'RECORDED'`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ amount: string; name: string | null }> }));

  const budgetRow = await client.query<{ amount: string; currency_code: string }>(
    `SELECT amount::text, currency_code FROM finance.budget
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY updated_at DESC
     LIMIT 1`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ amount: string; currency_code: string }> }));

  const outstandingRow = await client.query<{ outstanding: string }>(
    `SELECT COALESCE(SUM(outstanding_total), 0)::text AS outstanding
     FROM projection.group_finance_snapshot
     WHERE moment_id = $1`,
    [momentId]
  );
  const spent = resolvedExpenses.reduce((s, e) => s + e.amount, 0);
  const contributed = contribRows.rows.reduce((s, c) => s + (parseFloat(c.amount) || 0), 0);
  const outstanding = parseFloat(outstandingRow.rows[0]?.outstanding ?? '0') || 0;
  const target = budgetRow.rows[0] ? parseFloat(budgetRow.rows[0].amount) || null : null;
  const remaining = target != null ? Math.max(0, target - spent) : outstanding;

  const categoryMap = new Map<string, number>();
  for (const e of resolvedExpenses) {
    const cat = e.category.trim() || 'Other';
    categoryMap.set(cat, (categoryMap.get(cat) ?? 0) + e.amount);
  }
  const categories = [...categoryMap.entries()]
    .map(([name, amount]) => ({ name, amount }))
    .sort((a, b) => b.amount - a.amount);

  const payerMap = new Map<string, { amount: number; payments: number }>();
  for (const e of resolvedExpenses) {
    const name = e.paid_by_name ?? 'Someone';
    const cur = payerMap.get(name) ?? { amount: 0, payments: 0 };
    cur.amount += e.amount;
    cur.payments += 1;
    payerMap.set(name, cur);
  }

  const contribMap = new Map<string, number>();
  for (const c of contribRows.rows) {
    const name = c.name ?? 'Someone';
    contribMap.set(name, (contribMap.get(name) ?? 0) + (parseFloat(c.amount) || 0));
  }

  const plans = await client.query<{ n: string; done: string }>(
    `SELECT COUNT(*)::text AS n,
            COUNT(*) FILTER (WHERE status = 'DONE')::text AS done
     FROM collaboration.planning_item
     WHERE moment_id = $1
       AND status IN ('OPEN', 'IN_PROGRESS', 'DONE')`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0', done: '0' }] }));

  const polls = await client.query<{ title: string; status: string }>(
    `SELECT question AS title, status FROM shared.poll WHERE moment_id = $1 ORDER BY created_at DESC LIMIT 8`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ title: string; status: string }> }));

  const places = composer.omitPlaces
    ? []
    : (
        await client.query<{ label: string; start_at: Date | null; end_at: Date | null }>(
          `SELECT label, start_at, end_at FROM collaboration.shared_experience_place
           WHERE moment_id = $1 ORDER BY sort_order ASC`,
          [momentId]
        ).catch(() => ({ rows: [] as Array<{ label: string; start_at: Date | null; end_at: Date | null }> }))
      ).rows.map((p) => ({
        label: p.label,
        startAt: p.start_at?.toISOString() ?? null,
        endAt: p.end_at?.toISOString() ?? null,
      }));

  const memoryRows = await client.query<{
    body_text: string | null;
    created_at: Date | null;
  }>(
    `SELECT body_text, created_at FROM memory.memory
     WHERE moment_id = $1 AND status = 'ACTIVE'
     ORDER BY created_at DESC LIMIT 12`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ body_text: string | null; created_at: Date | null }> }));

  const photoCount = await client.query<{ n: string }>(
    `SELECT COUNT(*)::text AS n
     FROM memory.memory_evidence me
     JOIN memory.memory m ON m.memory_id = me.memory_id
     JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
     WHERE m.moment_id = $1
       AND m.status = 'ACTIVE'
       AND me.source_type = 'MEDIA'
       AND mu.status = 'COMPLETED'`,
    [momentId]
  ).catch(() => ({ rows: [{ n: '0' }] }));

  const photoMedia = await client.query<{
    media_upload_id: string;
    bucket: string | null;
    object_key: string | null;
    created_at: Date | null;
    title: string | null;
  }>(
    `SELECT mu.media_upload_id, mu.bucket, mu.object_key, me.created_at, m.title
     FROM memory.memory_evidence me
     JOIN memory.memory m ON m.memory_id = me.memory_id
     JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
     WHERE m.moment_id = $1
       AND m.status = 'ACTIVE'
       AND me.source_type = 'MEDIA'
       AND mu.status = 'COMPLETED'
       AND mu.bucket IS NOT NULL
       AND mu.object_key IS NOT NULL
     ORDER BY me.created_at DESC`,
    [momentId]
  ).catch(() => ({
    rows: [] as Array<{
      media_upload_id: string;
      bucket: string | null;
      object_key: string | null;
      created_at: Date | null;
      title: string | null;
    }>,
  }));

  const photos: StorySnapshot['photos'] = [];
  for (const row of photoMedia.rows) {
    if (!row.bucket || !row.object_key) continue;
    const url = await trySignedDownloadUrl(row.bucket, row.object_key);
    if (url) {
      photos.push({
        url,
        at: row.created_at?.toISOString() ?? null,
        title: row.title?.trim() || null,
        mediaId: row.media_upload_id,
      });
    }
  }

  const planItems = await client.query<{ title: string; status: string; created_at: Date | null }>(
    `SELECT title, status, created_at
     FROM collaboration.planning_item
     WHERE moment_id = $1
       AND status IN ('OPEN', 'IN_PROGRESS', 'DONE')
     ORDER BY
       CASE status WHEN 'DONE' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END,
       created_at ASC
     LIMIT 3`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ title: string; status: string; created_at: Date | null }> }));

  const startAt = row.start_at?.toISOString() ?? null;
  const endAt = row.end_at?.toISOString() ?? null;
  const completedAt = row.completed_at?.toISOString() ?? null;

  const metrics: Record<string, string | number> = {
    people: people.length,
    days: daysBetween(startAt, endAt),
    hours: hoursBetween(startAt, endAt),
    months: monthsBetween(startAt, endAt),
    plans: parseInt(plans.rows[0]?.n ?? '0', 10),
    decisions: polls.rows.length,
    photos: parseInt(photoCount.rows[0]?.n ?? '0', 10),
    bills: expenseRows.rows.length,
    spent: formatInr(spent),
    spentRaw: spent,
    contributed: formatInr(contributed),
    raised: formatInr(contributed > 0 ? contributed : target ?? spent),
    target: target != null ? formatInr(target) : '—',
    remaining: formatInr(remaining),
  };

  const experienceLike =
    familyProfile === 'TRIP' ||
    familyProfile === 'SHARED_EXPERIENCE' ||
    familyProfile === 'WEDDING' ||
    familyProfile === 'HOUSE_PARTY';
  const startedLabel =
    familyProfile === 'TRIP'
      ? 'Trip started'
      : familyProfile === 'HOUSE_PARTY'
        ? 'Night kicked off'
        : familyProfile === 'WEDDING'
          ? 'Celebration began'
          : experienceLike
            ? 'Chapter began'
            : 'Moment activated';
  const wrappedLabel =
    familyProfile === 'TRIP'
      ? 'Trip wrapped up'
      : familyProfile === 'HOUSE_PARTY'
        ? 'Night saved'
        : familyProfile === 'WEDDING'
          ? 'Celebration closed'
          : experienceLike
            ? 'Wrapped up'
            : 'Moment completed';

  const timeline: StorySnapshot['timeline'] = [];
  if (startAt) {
    timeline.push({
      at: startAt,
      label: startedLabel,
      detail: 'The chapter began.',
    });
  }
  if (target) {
    timeline.push({
      at: startAt ?? completedAt ?? new Date().toISOString(),
      label: `Budget set ${formatInr(target)}`,
    });
  }
  for (const place of places.slice(0, 4)) {
    timeline.push({
      at: place.startAt ?? startAt ?? completedAt ?? new Date().toISOString(),
      label: place.label,
      detail: 'Place on the itinerary',
    });
  }
  for (const item of planItems.rows) {
    timeline.push({
      at: item.created_at?.toISOString() ?? startAt ?? completedAt ?? new Date().toISOString(),
      label: item.title,
      detail: item.status === 'DONE' ? 'Done' : item.status === 'IN_PROGRESS' ? 'In progress' : 'Planned',
    });
  }
  for (const poll of polls.rows.slice(0, 3)) {
    timeline.push({
      at: completedAt ?? endAt ?? new Date().toISOString(),
      label: poll.title,
      detail: poll.status,
    });
  }
  if (completedAt) {
    timeline.push({
      at: completedAt,
      label: wrappedLabel,
      detail: 'Ready to relive.',
    });
  }

  const insights: string[] = [];
  if (people.length >= 3) insights.push(`${people.length} people showed up for this moment.`);
  if (spent > 0) insights.push(`Together you moved ${formatInr(spent)} through the moment.`);
  if (parseInt(plans.rows[0]?.n ?? '0', 10) > 0) {
    insights.push(`Plans met reality: ${plans.rows[0]?.done ?? 0} of ${plans.rows[0]?.n ?? 0} closed.`);
  }
  if (categories.length >= 2 && spent > 0) {
    const topShare = (categories[0]!.amount + categories[1]!.amount) / spent;
    if (topShare >= 0.5) {
      insights.push(
        `${categories[0]!.name} and ${categories[1]!.name} together accounted for more than half of the shared spend.`
      );
    }
  }

  const chapters: StoryChapterId[] = ['cover', 'moment', 'together', 'money', 'close'];

  const opening =
    familyProfile === 'HOUSE_PARTY'
      ? `${people.length} people turned a plan into a night worth keeping.`
      : familyProfile === 'SHARED_PURCHASE'
        ? `A shared goal became something you own together.`
        : familyProfile === 'SHARED_LIVING'
          ? `A chapter of living — bills, decisions, and home.`
          : `${people.length} people made ${row.title} real.`;

  const snapshot: StorySnapshot = {
    identity: {
      momentId,
      title: row.title,
      momentTypeCode: row.moment_type_code,
      groupFamily: row.group_family,
      familyProfile,
      startAt,
      endAt,
      completedAt,
      currencyCode: budgetRow.rows[0]?.currency_code ?? expenseRows.rows[0]?.currency_code ?? 'INR',
    },
    people,
    metrics,
    timeline,
    money: {
      contributed,
      spent,
      remaining,
      unsettled: outstanding,
      target,
      categories,
      contributors: [...contribMap.entries()].map(([name, amount]) => ({ name, amount })),
      payers: [...payerMap.entries()].map(([name, v]) => ({ name, amount: v.amount, payments: v.payments })),
      expenses: resolvedExpenses.map((e) => ({
        description: e.description,
        category: e.category,
        payer: e.paid_by_name ?? 'Someone',
        amount: e.amount,
        at: e.at,
      })),
    },
    places,
    decisions: polls.rows.map((p) => ({ title: p.title, status: p.status })),
    memories: memoryRows.rows.map((m) => ({
      text: m.body_text,
      mediaUrl: null,
      at: m.created_at?.toISOString() ?? null,
    })),
    photos,
    narrative: { opening, insights: insights.slice(0, 4) },
    chapters,
    display: composer,
  };

  const sourceManifest = {
    people: people.length,
    expenses: expenseRows.rows.length,
    contributions: contribRows.rows.length,
    plans: parseInt(plans.rows[0]?.n ?? '0', 10),
    polls: polls.rows.length,
    memories: memoryRows.rows.length,
    places: places.length,
  };

  return { snapshot, sourceManifest };
}
