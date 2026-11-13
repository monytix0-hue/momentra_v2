/** Pick push targets so one event does not fan out to every stale device row. */

export type DeviceTokenRow = {
  push_token: string;
  platform: string;
  last_seen_at: Date | string | null;
};

/**
 * Deduplicate by token, then keep at most one token per platform
 * (newest last_seen_at wins). Prevents 3× trays from reinstall churn.
 */
export function selectPushDevices(
  rows: DeviceTokenRow[]
): Array<{ push_token: string; platform: string }> {
  const byToken = new Map<string, { push_token: string; platform: string; seen: number }>();
  for (const row of rows) {
    const token = row.push_token?.trim();
    if (!token) continue;
    const seen = row.last_seen_at ? new Date(row.last_seen_at).getTime() : 0;
    const prev = byToken.get(token);
    if (!prev || seen >= prev.seen) {
      byToken.set(token, { push_token: token, platform: row.platform, seen });
    }
  }
  const byPlatform = new Map<string, { push_token: string; platform: string; seen: number }>();
  for (const row of byToken.values()) {
    const key = row.platform.toUpperCase();
    const prev = byPlatform.get(key);
    if (!prev || row.seen >= prev.seen) {
      byPlatform.set(key, row);
    }
  }
  return [...byPlatform.values()].map(({ push_token, platform }) => ({ push_token, platform }));
}
