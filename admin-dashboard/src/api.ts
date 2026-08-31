const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:3000';
const STORAGE_KEY = 'momentra_admin_api_key';

export function getApiKey(): string | null {
  return sessionStorage.getItem(STORAGE_KEY);
}

export function setApiKey(key: string): void {
  sessionStorage.setItem(STORAGE_KEY, key);
}

export function clearApiKey(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}

async function adminFetchPath<T>(path: string): Promise<T> {
  const key = getApiKey();
  if (!key) throw new Error('Not authenticated');

  const res = await fetch(`${API_BASE.replace(/\/$/, '')}/admin/api${path}`, {
    headers: {
      Accept: 'application/json',
      'X-Admin-Key': key,
      'ngrok-skip-browser-warning': 'true',
    },
  });

  if (res.status === 401) {
    clearApiKey();
    throw new Error('Invalid admin key');
  }
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  const json = await res.json();
  return json.data as T;
}

async function adminFetch<T>(path: string): Promise<T> {
  return adminFetchPath<T>(`/telemetry${path}`);
}

export const api = {
  overview: () => adminFetch<Overview>('/overview'),
  sessions: (limit = 50, platform?: string) => {
    const q = new URLSearchParams({ limit: String(limit) });
    if (platform) q.set('platform', platform);
    return adminFetch<{ items: SessionRow[] }>(`/sessions?${q}`);
  },
  events: (limit = 100, filters?: { screenName?: string; eventName?: string }) => {
    const q = new URLSearchParams({ limit: String(limit) });
    if (filters?.screenName) q.set('screenName', filters.screenName);
    if (filters?.eventName) q.set('eventName', filters.eventName);
    return adminFetch<{ items: EventRow[] }>(`/events?${q}`);
  },
  screenTime: () => adminFetch<{ items: ScreenTimeRow[] }>('/screen-time'),
  stuckPoints: () => adminFetch<{ items: StuckRow[] }>('/stuck-points'),
  users: (limit = 50) => adminFetch<{ items: UserRow[] }>(`/users?limit=${limit}`),
  widgets: (limit = 30) => adminFetch<{ items: WidgetRow[] }>(`/widgets?limit=${limit}`),
  personalSetups: () => adminFetch<PersonalSetupReport>('/personal-setups'),
  businessSetups: () => adminFetch<BusinessSetupReport>('/business-setups'),
  groupExperiences: () =>
    adminFetchPath<{ items: GroupExperienceRow[] }>('/group-experiences'),
  groupExperience: (momentId: string) =>
    adminFetchPath<GroupExperienceDetail>(`/group-experiences/${momentId}`),
};

export interface Overview {
  totalSessions: number;
  totalEvents: number;
  uniqueVisitors: number;
  stuckEvents: number;
  eventsLast24h: number;
}

export interface SessionRow {
  client_session_id: string;
  anonymous_id: string;
  user_id: string | null;
  platform: string;
  app_version: string | null;
  device_model: string | null;
  started_at: string;
  ended_at: string | null;
  user_snapshot: UserSnapshot;
  event_count: number;
}

export interface EventRow {
  client_event_id: string;
  event_name: string;
  screen_name: string | null;
  widget_name: string | null;
  properties: Record<string, unknown>;
  client_occurred_at: string;
  platform: string;
  user_id: string | null;
  anonymous_id: string;
  user_snapshot: UserSnapshot;
}

export interface ScreenTimeRow {
  screen_name: string;
  seconds_on_screen: number;
  logged_in_users: number;
  anonymous_devices: number;
  avg_idle_sec: string | null;
  enter_count: number;
  exit_count: number;
}

export interface StuckRow {
  screen_name: string;
  widget_name: string | null;
  stuck_count: number;
  max_idle_sec: number;
  avg_screen_elapsed_sec: string | null;
}

export interface UserSnapshot {
  userName?: string;
  userEmail?: string;
  userPhone?: string;
  userAge?: string;
  userSex?: string;
  hasPhoto?: boolean;
  photoUrl?: string;
  authProviders?: string;
}

export interface UserRow {
  user_id: string | null;
  anonymous_id: string;
  platform: string;
  user_snapshot: UserSnapshot;
  last_seen_at: string;
  app_version: string | null;
  device_model: string | null;
}

export interface WidgetRow {
  widget_name: string;
  screen_name: string;
  tap_count: number;
  unique_users: number;
}

export interface PersonalSetupReport {
  catalog: Array<{
    systemCode: string;
    title: string;
    figmaNodeId: string;
    previewAsset: string;
    analyticsScreen: string;
  }>;
  activations: Array<{
    systemCode: string;
    activationCount: number;
    lastActivatedAt: string | null;
  }>;
  recent: Array<{
    setupId: string;
    systemCode: string;
    title: string;
    momentId: string;
    userId: string;
    createdAt: string;
  }>;
  screenTime: Array<{
    screenName: string;
    secondsOnScreen: number;
    enterCount: number;
  }>;
}

export interface BusinessSetupReport {
  catalog: Array<{
    familyCode: string;
    title: string;
    figmaNodeId: string;
    previewAsset: string;
    analyticsScreen: string;
  }>;
  activations: Array<{
    familyCode: string;
    activationCount: number;
    lastActivatedAt: string | null;
  }>;
  recent: Array<{
    setupId: string;
    familyCode: string;
    title: string;
    momentId: string;
    companyId: string;
    userId: string;
    createdAt: string;
  }>;
  screenTime: Array<{
    screenName: string;
    secondsOnScreen: number;
    enterCount: number;
  }>;
}

export interface GroupExperienceRow {
  momentId: string;
  title: string;
  status: string;
  momentTypeCode: string;
  experienceKind: string | null;
  startAt: string | null;
  endAt: string | null;
  organizerUserId: string;
  organizerName: string | null;
  participantCount: number;
  createdAt: string;
}

export interface GroupExperienceDetail extends GroupExperienceRow {
  description: string | null;
  destinationText: string | null;
  venueText: string | null;
  timezone: string;
  updatedAt: string;
  participants: Array<{
    participantId: string;
    roleCode: string;
    status: string;
    userId: string | null;
    displayName: string | null;
  }>;
}
