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
    expenses: Array<{ description: string; category: string; payer: string; amount: number }>;
  };
  places: Array<{ label: string; startAt: string | null; endAt: string | null }>;
  decisions: Array<{ title: string; status: string }>;
  memories: Array<{ text: string | null; mediaUrl: string | null; at: string | null }>;
  photos: Array<{ url: string; at: string | null }>;
  narrative: { opening: string; insights: string[] };
  chapters: StoryChapterId[];
  display: ReturnType<typeof getStoryComposer>;
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
    category: string | null;
  }>(
    `SELECT e.description, e.amount::text,
            COALESCE(up.display_name, ep.display_name, mp.metadata->>'displayName', 'Someone') AS paid_by_name,
            COALESCE(e.category_code, 'Other') AS category
     FROM finance.expense e
     INNER JOIN finance.group_expense_context g ON g.expense_id = e.expense_id AND g.moment_id = e.moment_id
     LEFT JOIN collaboration.moment_participant mp
       ON mp.participant_id = g.paid_by_participant_id AND mp.moment_id = e.moment_id
     LEFT JOIN core.user_profile up ON up.user_id = mp.user_id
     LEFT JOIN core.external_party ep ON ep.external_party_id = mp.external_party_id
     WHERE e.moment_id = $1::uuid
       AND e.domain_code = 'GROUP'
       AND e.status = 'POSTED'
     ORDER BY e.effective_at DESC
     LIMIT 50`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ description: string | null; amount: string; paid_by_name: string | null; category: string | null }> }));

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

  const spent = expenseRows.rows.reduce((s, e) => s + (parseFloat(e.amount) || 0), 0);
  const contributed = contribRows.rows.reduce((s, c) => s + (parseFloat(c.amount) || 0), 0);
  const target = budgetRow.rows[0] ? parseFloat(budgetRow.rows[0].amount) || null : null;
  const remaining = target != null ? Math.max(0, target - spent) : Math.max(0, contributed - spent);

  const categoryMap = new Map<string, number>();
  for (const e of expenseRows.rows) {
    const cat = (e.category ?? 'Other').trim() || 'Other';
    categoryMap.set(cat, (categoryMap.get(cat) ?? 0) + (parseFloat(e.amount) || 0));
  }
  const categories = [...categoryMap.entries()]
    .map(([name, amount]) => ({ name, amount }))
    .sort((a, b) => b.amount - a.amount);

  const payerMap = new Map<string, { amount: number; payments: number }>();
  for (const e of expenseRows.rows) {
    const name = e.paid_by_name ?? 'Someone';
    const cur = payerMap.get(name) ?? { amount: 0, payments: 0 };
    cur.amount += parseFloat(e.amount) || 0;
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
    bucket: string | null;
    object_key: string | null;
    created_at: Date | null;
  }>(
    `SELECT mu.bucket, mu.object_key, me.created_at
     FROM memory.memory_evidence me
     JOIN memory.memory m ON m.memory_id = me.memory_id
     JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
     WHERE m.moment_id = $1
       AND m.status = 'ACTIVE'
       AND me.source_type = 'MEDIA'
       AND mu.status = 'COMPLETED'
       AND mu.bucket IS NOT NULL
       AND mu.object_key IS NOT NULL
     ORDER BY me.created_at DESC
     LIMIT 5`,
    [momentId]
  ).catch(() => ({ rows: [] as Array<{ bucket: string | null; object_key: string | null; created_at: Date | null }> }));

  const photos: StorySnapshot['photos'] = [];
  for (const row of photoMedia.rows) {
    if (!row.bucket || !row.object_key) continue;
    const url = await trySignedDownloadUrl(row.bucket, row.object_key);
    if (url) {
      photos.push({ url, at: row.created_at?.toISOString() ?? null });
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
      currencyCode: budgetRow.rows[0]?.currency_code ?? 'INR',
    },
    people,
    metrics,
    timeline,
    money: {
      contributed,
      spent,
      remaining,
      unsettled: Math.max(0, spent - contributed),
      target,
      categories,
      contributors: [...contribMap.entries()].map(([name, amount]) => ({ name, amount })),
      payers: [...payerMap.entries()].map(([name, v]) => ({ name, amount: v.amount, payments: v.payments })),
      expenses: expenseRows.rows.slice(0, 8).map((e) => ({
        description: e.description ?? 'Expense',
        category: e.category ?? 'Other',
        payer: e.paid_by_name ?? 'Someone',
        amount: parseFloat(e.amount) || 0,
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
