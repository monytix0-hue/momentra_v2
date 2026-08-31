import type { PoolClient } from 'pg';

export async function getTelemetryOverview(client: PoolClient) {
  const result = await client.query<{
    total_sessions: string;
    total_events: string;
    unique_visitors: string;
    stuck_events: string;
    events_last_24h: string;
  }>(`
    SELECT
      (SELECT COUNT(*)::bigint FROM analytics.client_session) AS total_sessions,
      (SELECT COUNT(*)::bigint FROM analytics.client_event) AS total_events,
      (SELECT COUNT(DISTINCT COALESCE(user_id::text, anonymous_id::text))::bigint FROM analytics.client_session) AS unique_visitors,
      (SELECT COUNT(*)::bigint FROM analytics.client_event WHERE event_name = 'screen_stuck') AS stuck_events,
      (SELECT COUNT(*)::bigint FROM analytics.client_event WHERE client_occurred_at >= now() - interval '24 hours') AS events_last_24h
  `);
  const row = result.rows[0];
  return {
    totalSessions: Number(row?.total_sessions ?? 0),
    totalEvents: Number(row?.total_events ?? 0),
    uniqueVisitors: Number(row?.unique_visitors ?? 0),
    stuckEvents: Number(row?.stuck_events ?? 0),
    eventsLast24h: Number(row?.events_last_24h ?? 0),
  };
}

export async function listTelemetrySessions(
  client: PoolClient,
  opts: { limit: number; platform?: string }
) {
  const limit = Math.min(Math.max(opts.limit, 1), 200);
  const params: unknown[] = [limit];
  let platformFilter = '';
  if (opts.platform) {
    params.push(opts.platform);
    platformFilter = `AND cs.platform = $${params.length}`;
  }
  const result = await client.query(
    `SELECT
       cs.client_session_id,
       cs.anonymous_id,
       cs.user_id,
       cs.platform,
       cs.app_version,
       cs.device_model,
       cs.started_at,
       cs.ended_at,
       cs.user_snapshot,
       (SELECT COUNT(*)::int FROM analytics.client_event ce WHERE ce.client_session_id = cs.client_session_id) AS event_count
     FROM analytics.client_session cs
     WHERE 1=1 ${platformFilter}
     ORDER BY cs.started_at DESC
     LIMIT $1`,
    params
  );
  return { items: result.rows };
}

export async function listTelemetryEvents(
  client: PoolClient,
  opts: { limit: number; screenName?: string; eventName?: string; userId?: string }
) {
  const limit = Math.min(Math.max(opts.limit, 1), 500);
  const params: unknown[] = [limit];
  const filters: string[] = [];

  if (opts.screenName) {
    params.push(opts.screenName);
    filters.push(`ce.screen_name = $${params.length}`);
  }
  if (opts.eventName) {
    params.push(opts.eventName);
    filters.push(`ce.event_name = $${params.length}`);
  }
  if (opts.userId) {
    params.push(opts.userId);
    filters.push(`ce.user_id = $${params.length}::uuid`);
  }

  const where = filters.length ? `WHERE ${filters.join(' AND ')}` : '';
  const result = await client.query(
    `SELECT
       ce.client_event_id,
       ce.client_session_id,
       ce.anonymous_id,
       ce.user_id,
       ce.event_name,
       ce.screen_name,
       ce.widget_name,
       ce.properties,
       ce.client_occurred_at,
       cs.platform,
       cs.user_snapshot
     FROM analytics.client_event ce
     JOIN analytics.client_session cs ON cs.client_session_id = ce.client_session_id
     ${where}
     ORDER BY ce.client_occurred_at DESC
     LIMIT $1`,
    params
  );
  return { items: result.rows };
}

export async function getScreenTimeReport(client: PoolClient) {
  const result = await client.query(`
    SELECT
      ce.screen_name,
      COUNT(*) FILTER (WHERE ce.event_name = 'screen_tick')::int AS seconds_on_screen,
      COUNT(DISTINCT ce.user_id)::int AS logged_in_users,
      COUNT(DISTINCT ce.anonymous_id)::int AS anonymous_devices,
      ROUND(AVG((ce.properties->>'idle_sec')::numeric) FILTER (WHERE ce.event_name = 'screen_tick'), 1) AS avg_idle_sec,
      COUNT(*) FILTER (WHERE ce.event_name = 'screen_enter')::int AS enter_count,
      COUNT(*) FILTER (WHERE ce.event_name = 'screen_exit')::int AS exit_count
    FROM analytics.client_event ce
    WHERE ce.screen_name IS NOT NULL
    GROUP BY ce.screen_name
    ORDER BY seconds_on_screen DESC
  `);
  return { items: result.rows };
}

export async function getStuckPointsReport(client: PoolClient) {
  const result = await client.query(`
    SELECT
      ce.screen_name,
      ce.widget_name,
      COUNT(*)::int AS stuck_count,
      MAX((ce.properties->>'idle_sec')::int) AS max_idle_sec,
      ROUND(AVG((ce.properties->>'screen_elapsed_sec')::numeric), 1) AS avg_screen_elapsed_sec
    FROM analytics.client_event ce
    WHERE ce.event_name = 'screen_stuck'
    GROUP BY ce.screen_name, ce.widget_name
    ORDER BY stuck_count DESC
  `);
  return { items: result.rows };
}

export async function listTelemetryUsers(client: PoolClient, limit: number) {
  const capped = Math.min(Math.max(limit, 1), 200);
  const result = await client.query(
    `SELECT DISTINCT ON (COALESCE(cs.user_id::text, cs.anonymous_id::text))
       cs.user_id,
       cs.anonymous_id,
       cs.platform,
       cs.user_snapshot,
       cs.started_at AS last_seen_at,
       cs.app_version,
       cs.device_model
     FROM analytics.client_session cs
     ORDER BY COALESCE(cs.user_id::text, cs.anonymous_id::text), cs.started_at DESC
     LIMIT $1`,
    [capped]
  );
  return { items: result.rows };
}

export async function getWidgetInteractions(client: PoolClient, limit: number) {
  const capped = Math.min(Math.max(limit, 1), 100);
  const result = await client.query(
    `SELECT
       ce.widget_name,
       ce.screen_name,
       COUNT(*)::int AS tap_count,
       COUNT(DISTINCT COALESCE(ce.user_id::text, ce.anonymous_id::text))::int AS unique_users
     FROM analytics.client_event ce
     WHERE ce.event_name = 'widget_interaction' AND ce.widget_name IS NOT NULL
     GROUP BY ce.widget_name, ce.screen_name
     ORDER BY tap_count DESC
     LIMIT $1`,
    [capped]
  );
  return { items: result.rows };
}

const SETUP_SCREENS = [
  'screen_personal_setup_life_ops',
  'screen_personal_setup_future',
  'screen_personal_setup_lifestyle',
  'screen_personal_setup_relationships',
  'screen_personal_create',
] as const;

export async function getPersonalSetupReport(client: PoolClient) {
  const activations = await client.query<{
    system_code: string;
    activation_count: string;
    last_activated_at: Date | null;
  }>(
    `SELECT system_code,
            COUNT(*)::text AS activation_count,
            MAX(created_at) AS last_activated_at
     FROM personal.life_system_setup
     GROUP BY system_code
     ORDER BY system_code`
  ).catch(() => ({ rows: [] as Array<{ system_code: string; activation_count: string; last_activated_at: Date | null }> }));

  const recent = await client.query<{
    life_system_setup_id: string;
    system_code: string;
    title: string;
    moment_id: string;
    user_id: string;
    created_at: Date;
  }>(
    `SELECT life_system_setup_id, system_code, title, moment_id, user_id, created_at
     FROM personal.life_system_setup
     ORDER BY created_at DESC
     LIMIT 30`
  ).catch(() => ({ rows: [] as Array<{
    life_system_setup_id: string;
    system_code: string;
    title: string;
    moment_id: string;
    user_id: string;
    created_at: Date;
  }> }));

  const screenTime = await client.query<{
    screen_name: string;
    seconds_on_screen: string;
    enter_count: string;
  }>(
    `SELECT
       ce.screen_name,
       COALESCE(SUM(CASE WHEN ce.event_name = 'screen_tick' THEN 1 ELSE 0 END), 0)::text AS seconds_on_screen,
       COALESCE(SUM(CASE WHEN ce.event_name = 'screen_enter' THEN 1 ELSE 0 END), 0)::text AS enter_count
     FROM analytics.client_event ce
     WHERE ce.screen_name = ANY($1::text[])
     GROUP BY ce.screen_name
     ORDER BY ce.screen_name`,
    [SETUP_SCREENS as unknown as string[]]
  );

  return {
    catalog: [
      {
        systemCode: 'LIFE_OPERATIONS',
        title: 'Life Operations Setup',
        figmaNodeId: '353:6809',
        previewAsset: '/personal-setup/personal_setup_life_ops_scroll.png',
        analyticsScreen: 'screen_personal_setup_life_ops',
      },
      {
        systemCode: 'FUTURE_BUILDING',
        title: 'Future Building Setup',
        figmaNodeId: '353:6905',
        previewAsset: '/personal-setup/personal_setup_future_scroll.png',
        analyticsScreen: 'screen_personal_setup_future',
      },
      {
        systemCode: 'LIFESTYLE',
        title: 'Lifestyle Setup',
        figmaNodeId: '353:7075',
        previewAsset: '/personal-setup/personal_setup_lifestyle_scroll.png',
        analyticsScreen: 'screen_personal_setup_lifestyle',
      },
      {
        systemCode: 'RELATIONSHIPS',
        title: 'Relationships Setup',
        figmaNodeId: '353:7217',
        previewAsset: '/personal-setup/personal_setup_relationships_scroll.png',
        analyticsScreen: 'screen_personal_setup_relationships',
      },
    ],
    activations: activations.rows.map((r) => ({
      systemCode: r.system_code,
      activationCount: Number(r.activation_count),
      lastActivatedAt: r.last_activated_at?.toISOString() ?? null,
    })),
    recent: recent.rows.map((r) => ({
      setupId: r.life_system_setup_id,
      systemCode: r.system_code,
      title: r.title,
      momentId: r.moment_id,
      userId: r.user_id,
      createdAt: r.created_at.toISOString(),
    })),
    screenTime: screenTime.rows.map((r) => ({
      screenName: r.screen_name,
      secondsOnScreen: Number(r.seconds_on_screen),
      enterCount: Number(r.enter_count),
    })),
  };
}

const BUSINESS_SETUP_SCREENS = [
  'screen_business_setup_team_ops',
  'screen_business_setup_runway',
  'screen_business_setup_ops',
  'screen_business_create',
  'screen_company_setup',
] as const;

export async function getBusinessSetupReport(client: PoolClient) {
  const activations = await client
    .query<{
      family_code: string;
      activation_count: string;
      last_activated_at: Date | null;
    }>(
      `SELECT family_code,
              COUNT(*)::text AS activation_count,
              MAX(created_at) AS last_activated_at
       FROM business.business_system_setup
       GROUP BY family_code
       ORDER BY family_code`
    )
    .catch(
      () =>
        ({
          rows: [] as Array<{ family_code: string; activation_count: string; last_activated_at: Date | null }>,
        }) as const
    );

  const recent = await client
    .query<{
      business_system_setup_id: string;
      family_code: string;
      title: string;
      moment_id: string;
      company_id: string;
      user_id: string;
      created_at: Date;
    }>(
      `SELECT business_system_setup_id, family_code, title, moment_id, company_id, user_id, created_at
       FROM business.business_system_setup
       ORDER BY created_at DESC
       LIMIT 30`
    )
    .catch(
      () =>
        ({
          rows: [] as Array<{
            business_system_setup_id: string;
            family_code: string;
            title: string;
            moment_id: string;
            company_id: string;
            user_id: string;
            created_at: Date;
          }>,
        }) as const
    );

  const screenTime = await client.query<{
    screen_name: string;
    seconds_on_screen: string;
    enter_count: string;
  }>(
    `SELECT
       ce.screen_name,
       COALESCE(SUM(CASE WHEN ce.event_name = 'screen_tick' THEN 1 ELSE 0 END), 0)::text AS seconds_on_screen,
       COALESCE(SUM(CASE WHEN ce.event_name = 'screen_enter' THEN 1 ELSE 0 END), 0)::text AS enter_count
     FROM analytics.client_event ce
     WHERE ce.screen_name = ANY($1::text[])
     GROUP BY ce.screen_name
     ORDER BY ce.screen_name`,
    [BUSINESS_SETUP_SCREENS as unknown as string[]]
  );

  return {
    catalog: [
      {
        familyCode: 'TEAM_OPERATIONS',
        title: 'Set up Team Operations',
        figmaNodeId: '692:34736',
        previewAsset: '/business-setups/business_setup_team_ops_scroll.png',
        analyticsScreen: 'screen_business_setup_team_ops',
      },
      {
        familyCode: 'BUSINESS_RUNWAY',
        title: 'Set up Business Runway',
        figmaNodeId: '692:36690',
        previewAsset: '/business-setups/business_setup_runway_scroll.png',
        analyticsScreen: 'screen_business_setup_runway',
      },
      {
        familyCode: 'BUSINESS_OPERATIONS',
        title: 'Set up Business Operations',
        figmaNodeId: '692:37188',
        previewAsset: '/business-setups/business_setup_ops_scroll.png',
        analyticsScreen: 'screen_business_setup_ops',
      },
    ],
    activations: activations.rows.map((r) => ({
      familyCode: r.family_code,
      activationCount: Number(r.activation_count),
      lastActivatedAt: r.last_activated_at?.toISOString() ?? null,
    })),
    recent: recent.rows.map((r) => ({
      setupId: r.business_system_setup_id,
      familyCode: r.family_code,
      title: r.title,
      momentId: r.moment_id,
      companyId: r.company_id,
      userId: r.user_id,
      createdAt: r.created_at.toISOString(),
    })),
    screenTime: screenTime.rows.map((r) => ({
      screenName: r.screen_name,
      secondsOnScreen: Number(r.seconds_on_screen),
      enterCount: Number(r.enter_count),
    })),
  };
}

