import type { NotificationCategory } from './allowlist';

export type RecipientPrefs = {
  user_id: string;
  push_notifications_enabled: boolean;
  notification_categories: Record<string, boolean> | null;
  quiet_hours_start: string | null;
  quiet_hours_end: string | null;
  digest_enabled: boolean;
  timezone: string;
  /** Wave 1 cadence; defaults to ALL when absent. */
  notification_cadence?: string | null;
  notify_on_changes?: boolean;
};

export function asCategoryMap(raw: unknown): Record<string, boolean> {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};
  const out: Record<string, boolean> = {};
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof v === 'boolean') out[k] = v;
  }
  return out;
}

export function categoryEnabled(
  cats: Record<string, boolean> | null | undefined,
  category: NotificationCategory
): boolean {
  if (!cats || Object.keys(cats).length === 0) return true;
  if (category === 'system') return true;
  return cats[category] !== false;
}

/** Quiet hours use profile local time (HH:MM[:SS]). Cross-midnight supported. */
export function inQuietHours(
  now: Date,
  timezone: string,
  start: string | null,
  end: string | null
): boolean {
  if (!start || !end) return false;
  try {
    const local = new Intl.DateTimeFormat('en-GB', {
      timeZone: timezone || 'UTC',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(now);
    const [hh, mm] = local.split(':').map((x) => parseInt(x, 10));
    const nowMins = hh * 60 + mm;
    const parse = (t: string) => {
      const parts = t.split(':').map((x) => parseInt(x, 10));
      return (parts[0] ?? 0) * 60 + (parts[1] ?? 0);
    };
    const s = parse(start);
    const e = parse(end);
    if (s === e) return false;
    if (s < e) return nowMins >= s && nowMins < e;
    return nowMins >= s || nowMins < e;
  } catch {
    return false;
  }
}

export function shouldDigest(
  prefs: Pick<RecipientPrefs, 'digest_enabled' | 'quiet_hours_start' | 'quiet_hours_end' | 'timezone'>,
  priority: import('./allowlist').NotificationPriority,
  now = new Date()
): boolean {
  if (priority === 'HIGH') return false;
  if (prefs.digest_enabled) return true;
  return inQuietHours(now, prefs.timezone, prefs.quiet_hours_start, prefs.quiet_hours_end);
}
