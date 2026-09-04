import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { getPool, getPoolStats } from '../../platform/database/pool';
import { provisionUserProfile } from '../../platform/auth';
import * as projectionService from '../projection/service';
import * as businessService from '../business/service';

export type AppContextCode = 'PERSONAL' | 'GROUP' | 'BUSINESS' | 'CIRCLE';

export interface BootstrapMomentSummary {
  momentId: string;
  title: string;
  status: string;
  momentTypeCode?: string | null;
  domainCode: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  /** Required for BUSINESS moments so clients can scope Company → Moment. */
  companyId?: string | null;
}

export interface BootstrapCompanySummary {
  companyId: string;
  displayName: string;
}

export interface MeBootstrap {
  userId: string;
  email?: string | null;
  displayName?: string | null;
  firebaseUid: string;
  timezone: string;
  locale: string | null;
  status: string;
  roles: string[];
  permissions: string[];
  capabilities: string[];
  supportedContexts: AppContextCode[];
  currentlySelectedContext: AppContextCode;
  activeMoments: {
    personal: BootstrapMomentSummary[];
    group: BootstrapMomentSummary[];
    business: BootstrapMomentSummary[];
  };
  companies: BootstrapCompanySummary[];
  selectedCompany: BootstrapCompanySummary | null;
  preferences: {
    timezone: string;
    locale: string | null;
    pushNotificationsEnabled: boolean;
  };
  featureFlags: Record<string, boolean | string | number>;
}

const BOOTSTRAP_MOMENT_LIMIT = 20;

function featureFlagsFromEnv(): Record<string, boolean | string | number> {
  const raw = process.env.FEATURE_FLAGS?.trim();
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const out: Record<string, boolean | string | number> = {};
    for (const [k, v] of Object.entries(parsed)) {
      if (typeof v === 'boolean' || typeof v === 'string' || typeof v === 'number') {
        out[k] = v;
      }
    }
    return out;
  } catch {
    return {};
  }
}

/** True wall-clock concurrency: one pool checkout per query (single client serializes). */
async function withPoolClient<T>(fn: (client: PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    return await fn(client);
  } finally {
    client.release();
  }
}

/** Run tasks with bounded concurrency to reuse idle sockets instead of stampeding new TLS connects. */
async function mapWithConcurrency<T>(tasks: Array<() => Promise<T>>, concurrency: number): Promise<T[]> {
  const results: T[] = new Array(tasks.length);
  let next = 0;
  const workers = Math.max(1, Math.min(concurrency, tasks.length));
  async function worker(): Promise<void> {
    for (;;) {
      const i = next++;
      if (i >= tasks.length) return;
      results[i] = await tasks[i]!();
    }
  }
  await Promise.all(Array.from({ length: workers }, () => worker()));
  return results;
}

function bootstrapConcurrency(): number {
  const { totalCount, max } = getPoolStats();
  // Empty pool: open at most 2 TLS connects (Supabase pooler ~450ms each; width≠faster).
  if (totalCount === 0) return 2;
  // Thereafter allow full width so the pool can grow to 5 and warm stays ~1 RTT.
  return Math.min(5, max);
}

const CAPABILITIES_SQL = `SELECT DISTINCT c.code
     FROM core.capability c
     JOIN core.moment_type_capability mtc ON mtc.capability_id = c.capability_id
     JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id
     JOIN core.moment m ON m.moment_type_id = mt.moment_type_id
     WHERE m.status = 'ACTIVE'
       AND (
         EXISTS (
           SELECT 1 FROM personal.personal_moment_context pmc
           WHERE pmc.moment_id = m.moment_id AND pmc.user_id = $1
         )
         OR EXISTS (
           SELECT 1 FROM collaboration.moment_participant mp
           WHERE mp.moment_id = m.moment_id AND mp.user_id = $1 AND mp.status = 'ACTIVE'
         )
         OR EXISTS (
           SELECT 1 FROM business.business_moment_context bmc
           JOIN business.company_membership cm ON cm.company_id = bmc.company_id
           WHERE bmc.moment_id = m.moment_id AND cm.user_id = $1 AND cm.status = 'ACTIVE'
         )
       )
     ORDER BY c.code
     LIMIT 200`;

/**
 * Shell bootstrap — one server fan-in for inventory.
 * Does NOT load Pulse / Life / Memory / Activity tab datasets.
 *
 * S9-B/C: pool-backed parallel reads with concurrency capped to idle sockets
 * (avoids 5× TLS connect stampede to Supabase transaction pooler on cold start).
 * Capabilities query is skipped when inventory is empty (baseline caps only).
 * `_client` retained for call-site compatibility; unused (would serialize concurrency).
 */
export async function getMeBootstrap(_client: PoolClient | null, ctx: RequestContext): Promise<MeBootstrap> {
  const concurrency = bootstrapConcurrency();
  // S9-H-OPT: one parallel inventory wave including capabilities (no second RTT wave).
  const wave = await mapWithConcurrency(
      [
        () =>
          withPoolClient((client) =>
            client.query<{
              email: string | null;
              display_name: string | null;
              timezone: string;
              locale: string | null;
              status: string;
              push_notifications_enabled: boolean;
            }>(
              `SELECT email, display_name, timezone, locale, status, push_notifications_enabled
             FROM core.user_profile WHERE user_id = $1`,
              [ctx.userId]
            )
          ),
        () =>
          withPoolClient((client) =>
            projectionService.listPersonalMoments(client, ctx.userId, undefined, BOOTSTRAP_MOMENT_LIMIT)
          ),
        () =>
          withPoolClient((client) =>
            projectionService.listGroupMoments(client, ctx, undefined, BOOTSTRAP_MOMENT_LIMIT)
          ),
        () =>
          withPoolClient((client) =>
            projectionService.listBusinessMoments(client, ctx, undefined, BOOTSTRAP_MOMENT_LIMIT)
          ),
        () => withPoolClient((client) => businessService.listCompanies(client, ctx)),
        () =>
          withPoolClient((client) => client.query<{ code: string }>(CAPABILITIES_SQL, [ctx.userId])),
      ] as Array<() => Promise<unknown>>,
      concurrency
    );

  const profile = wave[0] as {
    rows: Array<{
      email: string | null;
      display_name: string | null;
      timezone: string;
      locale: string | null;
      status: string;
      push_notifications_enabled: boolean;
    }>;
  };
  const personalPage = wave[1] as Awaited<ReturnType<typeof projectionService.listPersonalMoments>>;
  const groupPage = wave[2] as Awaited<ReturnType<typeof projectionService.listGroupMoments>>;
  const businessPage = wave[3] as Awaited<ReturnType<typeof projectionService.listBusinessMoments>>;
  const companies = wave[4] as Awaited<ReturnType<typeof businessService.listCompanies>>;
  const capabilities = wave[5] as { rows: Array<{ code: string }> };

  let p = profile.rows[0];
  // Recovery: valid auth identity with no Momentra profile (auth warm-skip / race).
  if (!p) {
    await provisionUserProfile(ctx.userId, ctx.email, ctx.displayName);
    const recovered = await getPool().query<{
      email: string | null;
      display_name: string | null;
      timezone: string;
      locale: string | null;
      status: string;
      push_notifications_enabled: boolean;
    }>(
      `SELECT email, display_name, timezone, locale, status, push_notifications_enabled
       FROM core.user_profile WHERE user_id = $1`,
      [ctx.userId]
    );
    p = recovered.rows[0];
  }

  const capabilityCodes = capabilities.rows.map((r) => r.code);

  // Shell contexts are always navigable; empty inventories use ContextEmptyExperience.
  // Do not gate GROUP/BUSINESS on prior membership — that blocks first Moment create (S3 empty → setup).
  const supportedContexts: AppContextCode[] = ['PERSONAL', 'GROUP', 'BUSINESS', 'CIRCLE'];

  const currentlySelectedContext: AppContextCode = 'PERSONAL';

  const companyItems = companies.items;
  const selectedCompany = companyItems[0] ?? null;

  // Baseline shell capabilities always present for authenticated users.
  const baseline = ['SHELL_CONTEXT_SWITCH', 'SHELL_PROFILE'];
  const mergedCapabilities = Array.from(new Set([...baseline, ...capabilityCodes]));

  return {
    userId: ctx.userId,
    email: p?.email ?? ctx.email ?? null,
    displayName: p?.display_name ?? ctx.displayName ?? null,
    firebaseUid: ctx.firebaseUid,
    timezone: p?.timezone ?? 'UTC',
    locale: p?.locale ?? null,
    status: p?.status ?? 'ACTIVE',
    roles: ctx.roles.length ? ctx.roles : ['USER'],
    permissions: ctx.permissions,
    capabilities: mergedCapabilities,
    supportedContexts,
    currentlySelectedContext,
    activeMoments: {
      personal: personalPage.items.map((m) => ({
        momentId: m.momentId,
        title: m.title,
        status: m.status,
        momentTypeCode: m.momentTypeCode ?? null,
        domainCode: 'PERSONAL' as const,
      })),
      group: groupPage.items.map((m) => ({
        momentId: m.momentId,
        title: m.title,
        status: m.status,
        momentTypeCode: m.momentTypeCode ?? null,
        domainCode: 'GROUP' as const,
      })),
      business: businessPage.items.map((m) => ({
        momentId: m.momentId,
        title: m.title,
        status: m.status,
        momentTypeCode: m.businessFamily ?? null,
        domainCode: 'BUSINESS' as const,
        companyId: m.companyId,
      })),
    },
    companies: companyItems,
    selectedCompany,
    preferences: {
      timezone: p?.timezone ?? 'UTC',
      locale: p?.locale ?? null,
      pushNotificationsEnabled: p?.push_notifications_enabled ?? true,
    },
    featureFlags: featureFlagsFromEnv(),
  };
}
