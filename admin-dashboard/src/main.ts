import './styles.css';
import {
  api,
  clearApiKey,
  getApiKey,
  setApiKey,
  type EventRow,
  type Overview,
  type PersonalSetupReport,
  type BusinessSetupReport,
  type ScreenTimeRow,
  type SessionRow,
  type UserRow,
  type UserSnapshot,
  type WidgetRow,
  type GroupExperienceDetail,
  type GroupExperienceRow,
} from './api';

type Tab = 'overview' | 'users' | 'screens' | 'stuck' | 'widgets' | 'events' | 'sessions' | 'setups' | 'businessSetups' | 'groupExperiences';

const TABS: { id: Tab; label: string; ico: string }[] = [
  { id: 'overview', label: 'Playground', ico: '✨' },
  { id: 'setups', label: 'Personal setups', ico: '🧭' },
  { id: 'businessSetups', label: 'Business setups', ico: '💼' },
  { id: 'groupExperiences', label: 'Group Experiences', ico: '🧡' },
  { id: 'users', label: 'People', ico: '👋' },
  { id: 'screens', label: 'Screen time', ico: '⏱️' },
  { id: 'stuck', label: 'Stuck spots', ico: '🧊' },
  { id: 'widgets', label: 'Hot taps', ico: '👆' },
  { id: 'events', label: 'Live feed', ico: '📡' },
  { id: 'sessions', label: 'Sessions', ico: '🚀' },
];

let activeTab: Tab = 'overview';
let refreshTimer: number | undefined;

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function formatTs(iso: string): string {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const s = Math.max(0, Math.floor(diff / 1000));
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return `${Math.floor(s / 86400)}d ago`;
}

function platformBadge(platform: string): HTMLElement {
  return el('span', `badge badge-${platform}`, platform);
}

function animateCount(node: HTMLElement, target: number, duration = 900): void {
  const start = performance.now();
  const from = 0;
  const tick = (now: number) => {
    const t = Math.min(1, (now - start) / duration);
    const eased = 1 - Math.pow(1 - t, 3);
    node.textContent = String(Math.round(from + (target - from) * eased));
    if (t < 1) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}

function emptyState(emoji: string, title: string, body: string): HTMLElement {
  const wrap = el('div', 'empty panel');
  wrap.appendChild(el('span', 'big', emoji));
  wrap.appendChild(el('h3', '', title));
  wrap.appendChild(el('p', '', body));
  return wrap;
}

function loadingBlock(msg = 'Fetching vibes…'): HTMLElement {
  const wrap = el('div', 'loading');
  wrap.appendChild(el('div', 'spinner'));
  wrap.appendChild(document.createTextNode(msg));
  return wrap;
}

function userDisplay(snapshot: UserSnapshot, userId: string | null, anonymousId: string): HTMLElement {
  const wrap = el('div', 'user-cell');
  const name = snapshot.userName || (userId ? 'User' : 'Anonymous explorer');
  const initial = name.charAt(0).toUpperCase();

  if (snapshot.photoUrl && snapshot.hasPhoto) {
    const img = el('img', 'avatar') as HTMLImageElement;
    img.src = snapshot.photoUrl;
    img.alt = name;
    wrap.appendChild(img);
  } else {
    wrap.appendChild(el('div', 'avatar-placeholder', initial));
  }

  const meta = el('div');
  meta.appendChild(el('strong', '', name));
  meta.appendChild(el('div', 'mono', userId ?? `${anonymousId.slice(0, 8)}…`));
  wrap.appendChild(meta);
  return wrap;
}

function sprayConfetti(): void {
  const layer = el('div', 'confetti');
  document.body.appendChild(layer);
  const colors = ['#ff7a1a', '#2dd4bf', '#38bdf8', '#a3e635', '#fb7185', '#ffb347'];
  for (let i = 0; i < 42; i++) {
    const piece = document.createElement('i');
    piece.style.left = `${Math.random() * 100}%`;
    piece.style.background = colors[i % colors.length];
    piece.style.animationDelay = `${Math.random() * 0.4}s`;
    piece.style.animationDuration = `${1.1 + Math.random() * 0.8}s`;
    layer.appendChild(piece);
  }
  window.setTimeout(() => layer.remove(), 2200);
}

function ensureAmbient(root: HTMLElement): void {
  if (document.querySelector('.ambient')) return;
  const ambient = el('div', 'ambient');
  ambient.appendChild(el('div', 'orb a'));
  ambient.appendChild(el('div', 'orb b'));
  ambient.appendChild(el('div', 'orb c'));
  root.prepend(ambient);
}

function barChart(
  items: { label: string; value: number }[],
  opts?: { fillClass?: string; onClick?: (label: string) => void }
): HTMLElement {
  const panel = el('div', 'panel');
  if (!items.length) {
    panel.appendChild(emptyState('📭', 'Nothing here yet', 'Open the app and poke around — bars will grow.'));
    return panel;
  }
  const max = Math.max(...items.map((i) => i.value), 1);
  for (const item of items.slice(0, 12)) {
    const row = el('div', 'bar-row');
    row.title = `${item.label}: ${item.value}`;
    row.appendChild(el('div', 'bar-label', item.label));
    const track = el('div', 'bar-track');
    const fill = el('div', `bar-fill ${opts?.fillClass ?? ''}`.trim());
    track.appendChild(fill);
    row.appendChild(track);
    row.appendChild(el('div', 'bar-value', String(item.value)));
    if (opts?.onClick) row.onclick = () => opts.onClick!(item.label);
    panel.appendChild(row);
    requestAnimationFrame(() => {
      fill.style.width = `${(item.value / max) * 100}%`;
    });
  }
  return panel;
}

function goTab(tab: Tab, root: HTMLElement): void {
  activeTab = tab;
  renderApp(root);
}

function renderLogin(root: HTMLElement): void {
  root.innerHTML = '';
  ensureAmbient(root);
  const screen = el('div', 'login-screen');
  const card = el('div', 'login-card');
  card.appendChild(el('div', 'login-mark', 'M'));
  card.appendChild(el('h1', '', 'Telemetry Playground'));
  card.appendChild(
    el(
      'p',
      '',
      'Watch where people wander, linger, and get stuck — live from your own database. Drop in your ADMIN_API_KEY to unlock the room.'
    )
  );

  const input = el('input') as HTMLInputElement;
  input.type = 'password';
  input.placeholder = 'Paste your secret key…';
  input.autocomplete = 'off';

  const err = el('div', 'error');
  err.style.display = 'none';

  const btn = el('button', 'btn', 'Let’s play →') as HTMLButtonElement;
  btn.type = 'button';
  btn.onclick = async () => {
    err.style.display = 'none';
    const key = input.value.trim();
    if (!key) {
      err.textContent = 'Need a key first — peek in backend/.env for ADMIN_API_KEY.';
      err.style.display = 'block';
      return;
    }
    btn.disabled = true;
    btn.textContent = 'Unlocking…';
    setApiKey(key);
    try {
      await api.overview();
      sprayConfetti();
      renderApp(root);
    } catch (e) {
      clearApiKey();
      err.textContent = e instanceof Error ? e.message : 'Connection failed';
      err.style.display = 'block';
      btn.disabled = false;
      btn.textContent = 'Let’s play →';
    }
  };

  input.addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter') btn.click();
  });

  card.appendChild(input);
  card.appendChild(err);
  card.appendChild(btn);
  screen.appendChild(card);
  root.appendChild(screen);
  input.focus();
}

function renderApp(root: HTMLElement): void {
  root.innerHTML = '';
  ensureAmbient(root);
  const layout = el('div', 'layout');

  const sidebar = el('aside', 'sidebar');
  const brand = el('div', 'brand');
  brand.innerHTML = 'momentra <span>play</span>';
  sidebar.appendChild(brand);
  sidebar.appendChild(el('div', 'brand-tag', 'first-party telemetry'));

  for (const tab of TABS) {
    const btn = el('button', `nav-btn${activeTab === tab.id ? ' active' : ''}`);
    btn.appendChild(el('span', 'nav-ico', tab.ico));
    btn.appendChild(document.createTextNode(tab.label));
    btn.onclick = () => goTab(tab.id, root);
    sidebar.appendChild(btn);
  }

  const logout = el('button', 'nav-btn');
  logout.style.marginTop = 'auto';
  logout.appendChild(el('span', 'nav-ico', '🚪'));
  logout.appendChild(document.createTextNode('Sign out'));
  logout.onclick = () => {
    clearApiKey();
    if (refreshTimer) window.clearInterval(refreshTimer);
    renderLogin(root);
  };
  sidebar.appendChild(logout);

  const main = el('main', 'main');
  layout.appendChild(sidebar);
  layout.appendChild(main);
  root.appendChild(layout);
  loadTab(main, root);
}

async function loadTab(main: HTMLElement, root: HTMLElement): Promise<void> {
  main.innerHTML = '';
  const errBox = el('div', 'error');
  errBox.style.display = 'none';
  main.appendChild(errBox);

  const content = el('div', 'page-enter');
  main.appendChild(content);
  content.appendChild(loadingBlock());

  try {
    content.innerHTML = '';
    switch (activeTab) {
      case 'overview':
        await renderOverview(content, root);
        break;
      case 'users':
        await renderUsers(content);
        break;
      case 'screens':
        await renderScreenTime(content, root);
        break;
      case 'stuck':
        await renderStuck(content, root);
        break;
      case 'widgets':
        await renderWidgets(content);
        break;
      case 'events':
        await renderEvents(content);
        break;
      case 'sessions':
        await renderSessions(content);
        break;
      case 'setups':
        await renderPersonalSetups(content);
        break;
      case 'businessSetups':
        await renderBusinessSetups(content);
        break;
      case 'groupExperiences':
        await renderGroupExperiences(content);
        break;
    }
  } catch (e) {
    content.innerHTML = '';
    errBox.textContent = e instanceof Error ? e.message : 'Failed to load';
    errBox.style.display = 'block';
    if (errBox.textContent.includes('admin key') || errBox.textContent.includes('Not authenticated')) {
      renderLogin(document.getElementById('app')!);
    }
  }
}

async function renderOverview(parent: HTMLElement, root: HTMLElement): Promise<void> {
  const [data, screens, stuck, widgets] = await Promise.all([
    api.overview(),
    api.screenTime(),
    api.stuckPoints(),
    api.widgets(8),
  ]);

  parent.appendChild(el('h1', 'page-title', 'What’s happening?'));
  parent.appendChild(
    el('p', 'page-sub', 'Tap a card to dive in. Bars grow with real seconds, idle time, and taps from your apps.')
  );

  const live = el('div', 'live-banner');
  live.appendChild(el('span', 'pulse-dot'));
  live.appendChild(document.createTextNode('Live from PostgreSQL · refreshes every 20s'));
  parent.appendChild(live);

  parent.appendChild(buildStatCards(data, root));

  const grid = el('div');
  grid.style.display = 'grid';
  grid.style.gridTemplateColumns = 'repeat(auto-fit, minmax(280px, 1fr))';
  grid.style.gap = '16px';

  const screenPanel = barChart(
    screens.items.map((r) => ({ label: r.screen_name, value: r.seconds_on_screen })),
    {
      fillClass: 'teal',
      onClick: () => goTab('screens', root),
    }
  );
  screenPanel.prepend(el('div', 'panel-title', '⏱️ Hottest screens'));
  grid.appendChild(screenPanel);

  const stuckPanel = barChart(
    stuck.items.map((r) => ({ label: r.screen_name, value: r.stuck_count })),
    {
      fillClass: 'rose',
      onClick: () => goTab('stuck', root),
    }
  );
  stuckPanel.prepend(el('div', 'panel-title', '🧊 Where they freeze'));
  grid.appendChild(stuckPanel);

  const widgetPanel = barChart(
    widgets.items.map((r) => ({ label: r.widget_name, value: r.tap_count })),
    { fillClass: 'sky', onClick: () => goTab('widgets', root) }
  );
  widgetPanel.prepend(el('div', 'panel-title', '👆 Most poked widgets'));
  grid.appendChild(widgetPanel);

  parent.appendChild(grid);

  const toolbar = el('div', 'toolbar');
  const refresh = el('button', 'btn secondary', 'Shuffle refresh');
  refresh.onclick = () => loadTab(parent.parentElement as HTMLElement, root);
  toolbar.appendChild(refresh);
  parent.appendChild(toolbar);
}

function buildStatCards(data: Overview, root: HTMLElement): HTMLElement {
  const cards = el('div', 'cards');
  const items: {
    label: string;
    value: number;
    emoji: string;
    tint: string;
    tab: Tab;
    hint: string;
  }[] = [
    { label: 'Sessions', value: data.totalSessions, emoji: '🚀', tint: '#ff7a1a', tab: 'sessions', hint: 'Peek sessions →' },
    { label: 'Events', value: data.totalEvents, emoji: '⚡', tint: '#38bdf8', tab: 'events', hint: 'Open live feed →' },
    { label: 'Visitors', value: data.uniqueVisitors, emoji: '👋', tint: '#2dd4bf', tab: 'users', hint: 'Meet the people →' },
    { label: 'Last 24h', value: data.eventsLast24h, emoji: '🔥', tint: '#ffb347', tab: 'events', hint: 'Recent noise →' },
    { label: 'Stuck', value: data.stuckEvents, emoji: '🧊', tint: '#fb7185', tab: 'stuck', hint: 'Find freezes →' },
  ];

  for (const item of items) {
    const card = el('div', 'stat-card');
    card.style.setProperty('--card-tint', item.tint);
    card.appendChild(el('div', 'emoji', item.emoji));
    card.appendChild(el('div', 'card-label', item.label));
    const value = el('div', 'card-value', '0');
    card.appendChild(value);
    card.appendChild(el('div', 'card-hint', item.hint));
    card.onclick = () => goTab(item.tab, root);
    cards.appendChild(card);
    animateCount(value, item.value);
  }
  return cards;
}

async function renderUsers(parent: HTMLElement): Promise<void> {
  const { items } = await api.users(100);
  parent.appendChild(el('h1', 'page-title', 'People in the room'));
  parent.appendChild(
    el('p', 'page-sub', 'Demographics snapshot — name, email, age, sex, photo when available.')
  );

  if (!items.length) {
    parent.appendChild(emptyState('🕵️', 'Ghost town', 'No sessions yet. Launch the Android or iOS app and bounce around.'));
    return;
  }

  const search = el('input') as HTMLInputElement;
  search.placeholder = 'Filter by name, email, age…';
  search.style.minWidth = '220px';
  const toolbar = el('div', 'toolbar');
  toolbar.appendChild(search);
  parent.appendChild(toolbar);

  const host = el('div');
  parent.appendChild(host);

  const paint = (list: UserRow[]) => {
    host.innerHTML = '';
    if (!list.length) {
      host.appendChild(emptyState('🔎', 'No matches', 'Try another filter.'));
      return;
    }
    const grid = el('div', 'user-grid');
    for (const row of list) {
      const s = row.user_snapshot ?? {};
      const card = el('div', 'user-card');
      card.appendChild(userDisplay(s, row.user_id, row.anonymous_id));
      const meta = el('div', 'meta-row');
      meta.appendChild(el('span', '', s.userEmail ?? 'no email'));
      meta.appendChild(platformBadge(row.platform));
      card.appendChild(meta);
      const meta2 = el('div', 'meta-row');
      meta2.appendChild(el('span', '', `Age ${s.userAge ?? '?'} · ${s.userSex ?? 'unknown'}`));
      meta2.appendChild(el('span', '', relativeTime(row.last_seen_at)));
      card.appendChild(meta2);
      grid.appendChild(card);
    }
    host.appendChild(grid);
  };

  search.oninput = () => {
    const q = search.value.trim().toLowerCase();
    paint(
      items.filter((row) => {
        const s = row.user_snapshot ?? {};
        return [s.userName, s.userEmail, s.userAge, s.userSex, row.platform]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
          .includes(q);
      })
    );
  };
  paint(items);
}

async function renderScreenTime(parent: HTMLElement, root: HTMLElement): Promise<void> {
  const { items } = await api.screenTime();
  parent.appendChild(el('h1', 'page-title', 'Where the seconds go'));
  parent.appendChild(el('p', 'page-sub', 'Each second on a screen becomes a taller bar. Click a bar to jump to the live feed.'));

  if (!items.length) {
    parent.appendChild(emptyState('⏳', 'No screen time yet', 'screen_tick events will fill this race track.'));
    return;
  }

  parent.appendChild(
    barChart(
      items.map((r) => ({ label: r.screen_name, value: r.seconds_on_screen })),
      {
        fillClass: 'teal',
        onClick: (label) => {
          activeTab = 'events';
          renderApp(root);
          window.setTimeout(() => {
            const input = document.querySelector('input[data-filter="screen"]') as HTMLInputElement | null;
            if (input) {
              input.value = label;
              input.dispatchEvent(new Event('input'));
            }
          }, 100);
        },
      }
    )
  );
  parent.appendChild(buildScreenTable(items));
}

function buildScreenTable(items: ScreenTimeRow[]): HTMLElement {
  const wrap = el('div', 'table-wrap');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['Screen', 'Seconds', 'Enters', 'Exits', 'Avg idle', 'Users', 'Devices']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of items) {
    const tr = el('tr');
    tr.appendChild(el('td', 'mono', row.screen_name));
    tr.appendChild(el('td', '', String(row.seconds_on_screen)));
    tr.appendChild(el('td', '', String(row.enter_count)));
    tr.appendChild(el('td', '', String(row.exit_count)));
    tr.appendChild(el('td', '', row.avg_idle_sec ?? '—'));
    tr.appendChild(el('td', '', String(row.logged_in_users)));
    tr.appendChild(el('td', '', String(row.anonymous_devices)));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  return wrap;
}

async function renderStuck(parent: HTMLElement, _root: HTMLElement): Promise<void> {
  const { items } = await api.stuckPoints();
  parent.appendChild(el('h1', 'page-title', 'Frozen in time'));
  parent.appendChild(
    el('p', 'page-sub', 'Idle 30+ seconds with no taps — these are friction hotspots.')
  );

  if (!items.length) {
    parent.appendChild(emptyState('🏖️', 'Nobody stuck', 'Either the UX is silky… or nobody’s online yet.'));
    return;
  }

  parent.appendChild(
    barChart(items.map((r) => ({ label: r.screen_name, value: r.stuck_count })), { fillClass: 'rose' })
  );

  const wrap = el('div', 'table-wrap');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['Screen', 'Widget', 'Stuck count', 'Max idle', 'Avg elapsed']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of items) {
    const tr = el('tr');
    tr.appendChild(el('td', 'mono', row.screen_name));
    tr.appendChild(el('td', 'mono', row.widget_name ?? '—'));
    tr.appendChild(el('td', '', String(row.stuck_count)));
    tr.appendChild(el('td', '', String(row.max_idle_sec)));
    tr.appendChild(el('td', '', row.avg_screen_elapsed_sec ?? '—'));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  parent.appendChild(wrap);
}

async function renderWidgets(parent: HTMLElement): Promise<void> {
  const { items } = await api.widgets();
  parent.appendChild(el('h1', 'page-title', 'Tap leaderboard'));
  parent.appendChild(el('p', 'page-sub', 'Named buttons and tabs — see what people actually press.'));

  if (!items.length) {
    parent.appendChild(emptyState('🫥', 'No taps logged', 'Instrument widgets and they’ll race up this board.'));
    return;
  }

  parent.appendChild(
    barChart(items.map((r) => ({ label: r.widget_name, value: r.tap_count })), { fillClass: 'sky' })
  );
  parent.appendChild(buildWidgetTable(items));
}

function buildWidgetTable(items: WidgetRow[]): HTMLElement {
  const wrap = el('div', 'table-wrap');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['Widget', 'Screen', 'Taps', 'Unique users']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of items) {
    const tr = el('tr');
    tr.appendChild(el('td', 'mono', row.widget_name));
    tr.appendChild(el('td', 'mono', row.screen_name));
    tr.appendChild(el('td', '', String(row.tap_count)));
    tr.appendChild(el('td', '', String(row.unique_users)));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  return wrap;
}

async function renderEvents(parent: HTMLElement): Promise<void> {
  parent.appendChild(el('h1', 'page-title', 'Live feed'));
  parent.appendChild(el('p', 'page-sub', 'Click a card to peek at raw properties. Chip filters update instantly.'));

  const chips = el('div', 'chip-row');
  const filters = ['all', 'screen_enter', 'screen_tick', 'screen_stuck', 'widget_interaction', 'session_start', 'auth_success'];
  let activeFilter = 'all';

  const eventFilter = el('input') as HTMLInputElement;
  eventFilter.dataset.filter = 'event';
  eventFilter.placeholder = 'Custom event name';
  const screenFilter = el('input') as HTMLInputElement;
  screenFilter.dataset.filter = 'screen';
  screenFilter.placeholder = 'Screen name';

  const toolbar = el('div', 'toolbar');
  toolbar.appendChild(eventFilter);
  toolbar.appendChild(screenFilter);
  const loadBtn = el('button', 'btn', 'Pull fresh');
  toolbar.appendChild(loadBtn);

  parent.appendChild(chips);
  parent.appendChild(toolbar);

  const feedHost = el('div');
  parent.appendChild(feedHost);

  const paintChips = () => {
    chips.innerHTML = '';
    for (const f of filters) {
      const chip = el('button', `chip${activeFilter === f ? ' active' : ''}`, f === 'all' ? 'Everything' : f);
      chip.onclick = () => {
        activeFilter = f;
        eventFilter.value = f === 'all' ? '' : f;
        paintChips();
        void load();
      };
      chips.appendChild(chip);
    }
  };

  const load = async () => {
    feedHost.innerHTML = '';
    feedHost.appendChild(loadingBlock('Listening…'));
    const { items } = await api.events(120, {
      eventName: eventFilter.value.trim() || undefined,
      screenName: screenFilter.value.trim() || undefined,
    });
    feedHost.innerHTML = '';
    if (!items.length) {
      feedHost.appendChild(emptyState('🦗', 'Crickets', 'No events match those filters yet.'));
      return;
    }
    feedHost.appendChild(buildEventFeed(items));
  };

  loadBtn.onclick = load;
  eventFilter.oninput = () => {
    activeFilter = filters.includes(eventFilter.value.trim()) ? eventFilter.value.trim() : 'all';
    paintChips();
  };
  screenFilter.oninput = () => void load();
  paintChips();
  await load();
}

function buildEventFeed(items: EventRow[]): HTMLElement {
  const feed = el('div', 'event-feed');
  items.forEach((row, i) => {
    const card = el('div', 'event-card');
    card.style.animationDelay = `${Math.min(i, 12) * 0.03}s`;
    const top = el('div', 'event-top');
    if (i < 3) top.appendChild(el('span', 'pulse-dot'));
    top.appendChild(el('span', 'event-name', row.event_name));
    if (row.screen_name) top.appendChild(el('span', 'mono', row.screen_name));
    if (row.widget_name) top.appendChild(el('span', 'mono', row.widget_name));
    top.appendChild(platformBadge(row.platform));
    top.appendChild(el('span', 'mono', relativeTime(row.client_occurred_at)));
    card.appendChild(top);

    const who = el('div');
    who.style.marginTop = '10px';
    who.appendChild(userDisplay(row.user_snapshot ?? {}, row.user_id, row.anonymous_id));
    card.appendChild(who);

    const props = el('div', 'event-props', JSON.stringify(row.properties ?? {}, null, 2));
    card.appendChild(props);
    card.onclick = () => card.classList.toggle('open');
    feed.appendChild(card);
  });
  return feed;
}

async function renderSessions(parent: HTMLElement): Promise<void> {
  parent.appendChild(el('h1', 'page-title', 'Session runway'));
  parent.appendChild(el('p', 'page-sub', 'Every app foreground is a flight. Filter by platform and watch them land.'));

  const chips = el('div', 'chip-row');
  let platform = '';
  for (const opt of [
    { v: '', l: 'All' },
    { v: 'android', l: 'Android' },
    { v: 'ios', l: 'iOS' },
    { v: 'web', l: 'Web' },
  ]) {
    const chip = el('button', `chip${!platform && !opt.v ? ' active' : ''}`, opt.l);
    chip.onclick = () => {
      platform = opt.v;
      [...chips.children].forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      void load();
    };
    chips.appendChild(chip);
  }
  parent.appendChild(chips);

  const host = el('div');
  parent.appendChild(host);

  const load = async () => {
    host.innerHTML = '';
    host.appendChild(loadingBlock());
    const { items } = await api.sessions(80, platform || undefined);
    host.innerHTML = '';
    if (!items.length) {
      host.appendChild(emptyState('🛫', 'No flights', 'Sessions appear when the app comes to the foreground.'));
      return;
    }
    host.appendChild(buildSessionTable(items));
  };
  await load();
}

function buildSessionTable(items: SessionRow[]): HTMLElement {
  const wrap = el('div', 'table-wrap');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['Started', 'Platform', 'Device', 'User', 'Events', 'Status']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of items) {
    const tr = el('tr');
    tr.appendChild(el('td', '', formatTs(row.started_at)));
    tr.appendChild(el('td')).appendChild(platformBadge(row.platform));
    tr.appendChild(el('td', '', row.device_model ?? '—'));
    tr.appendChild(el('td')).appendChild(userDisplay(row.user_snapshot ?? {}, row.user_id, row.anonymous_id));
    tr.appendChild(el('td', '', String(row.event_count)));
    tr.appendChild(el('td', '', row.ended_at ? `Landed · ${relativeTime(row.ended_at)}` : '🟢 Active'));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  return wrap;
}

async function renderPersonalSetups(parent: HTMLElement): Promise<void> {
  const data = await api.personalSetups();
  parent.appendChild(el('h1', 'page-title', 'Personal life-system setups'));
  parent.appendChild(
    el(
      'p',
      'page-sub',
      'Figma Create setups (Life Ops · Future · Lifestyle · Relationships) — previews, activations, and screen time.'
    )
  );

  const countByCode = new Map(data.activations.map((a) => [a.systemCode, a]));
  const grid = el('div', 'setup-grid');
  for (const item of data.catalog) {
    const card = el('div', 'setup-card panel');
    const img = el('img', 'setup-preview') as HTMLImageElement;
    img.src = item.previewAsset;
    img.alt = item.title;
    img.loading = 'lazy';
    card.appendChild(img);
    card.appendChild(el('h3', '', item.title));
    card.appendChild(el('div', 'mono', `Figma ${item.figmaNodeId}`));
    const stats = countByCode.get(item.systemCode);
    card.appendChild(
      el(
        'p',
        '',
        stats
          ? `${stats.activationCount} activation${stats.activationCount === 1 ? '' : 's'}${
              stats.lastActivatedAt ? ` · last ${relativeTime(stats.lastActivatedAt)}` : ''
            }`
          : 'No activations yet'
      )
    );
    card.appendChild(el('div', 'mono', item.analyticsScreen));
    grid.appendChild(card);
  }
  parent.appendChild(grid);

  parent.appendChild(el('h2', 'section-title', 'Setup screen time'));
  parent.appendChild(
    barChart(
      data.screenTime.map((r) => ({ label: r.screenName, value: r.secondsOnScreen })),
      { fillClass: 'teal' }
    )
  );

  parent.appendChild(el('h2', 'section-title', 'Recent activations'));
  if (!data.recent.length) {
    parent.appendChild(emptyState('🧭', 'No setups activated yet', 'Activate from Personal → Create in the apps.'));
    return;
  }
  const wrap = el('div', 'table-wrap panel');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['When', 'System', 'Title', 'Moment', 'User']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of data.recent) {
    const tr = el('tr');
    tr.appendChild(el('td', '', formatTs(row.createdAt)));
    tr.appendChild(el('td', '', row.systemCode));
    tr.appendChild(el('td', '', row.title));
    tr.appendChild(el('td', 'mono', row.momentId.slice(0, 8) + '…'));
    tr.appendChild(el('td', 'mono', row.userId.slice(0, 8) + '…'));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  parent.appendChild(wrap);
}

async function renderBusinessSetups(parent: HTMLElement): Promise<void> {
  const data = await api.businessSetups();
  parent.appendChild(el('h1', 'page-title', 'Business family setups'));
  parent.appendChild(
    el(
      'p',
      'page-sub',
      'Figma Business Create setups (Team Ops · Runway · Operations) — previews, activations, and screen time.'
    )
  );

  const countByCode = new Map(data.activations.map((a) => [a.familyCode, a]));
  const grid = el('div', 'setup-grid');
  for (const item of data.catalog) {
    const card = el('div', 'setup-card panel');
    const img = el('img', 'setup-preview') as HTMLImageElement;
    img.src = item.previewAsset;
    img.alt = item.title;
    img.loading = 'lazy';
    card.appendChild(img);
    card.appendChild(el('h3', '', item.title));
    card.appendChild(el('div', 'mono', `Figma ${item.figmaNodeId}`));
    const stats = countByCode.get(item.familyCode);
    card.appendChild(
      el(
        'p',
        '',
        stats
          ? `${stats.activationCount} activation${stats.activationCount === 1 ? '' : 's'}${
              stats.lastActivatedAt ? ` · last ${relativeTime(stats.lastActivatedAt)}` : ''
            }`
          : 'No activations yet'
      )
    );
    card.appendChild(el('div', 'mono', item.analyticsScreen));
    grid.appendChild(card);
  }
  parent.appendChild(grid);

  parent.appendChild(el('h2', 'section-title', 'Setup screen time'));
  parent.appendChild(
    barChart(
      data.screenTime.map((r) => ({ label: r.screenName, value: r.secondsOnScreen })),
      { fillClass: 'teal' }
    )
  );

  parent.appendChild(el('h2', 'section-title', 'Recent activations'));
  if (!data.recent.length) {
    parent.appendChild(emptyState('💼', 'No setups activated yet', 'Activate from Business → Create after company setup.'));
    return;
  }
  const wrap = el('div', 'table-wrap panel');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['When', 'Family', 'Title', 'Company', 'Moment', 'User']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  for (const row of data.recent) {
    const tr = el('tr');
    tr.appendChild(el('td', '', formatTs(row.createdAt)));
    tr.appendChild(el('td', '', row.familyCode));
    tr.appendChild(el('td', '', row.title));
    tr.appendChild(el('td', 'mono', row.companyId.slice(0, 8) + '…'));
    tr.appendChild(el('td', 'mono', row.momentId.slice(0, 8) + '…'));
    tr.appendChild(el('td', 'mono', row.userId.slice(0, 8) + '…'));
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  wrap.appendChild(table);
  parent.appendChild(wrap);
}

async function renderGroupExperiences(parent: HTMLElement): Promise<void> {
  const list = await api.groupExperiences();
  parent.appendChild(el('h1', 'page-title', 'Group Experiences'));
  parent.appendChild(
    el(
      'p',
      'page-sub',
      'SHARED_EXPERIENCE moments activated from Group → Create → Experience (TRIP · WEDDING · HOUSE_PARTY · OFFICE_OUTING).'
    )
  );

  if (!list.items.length) {
    parent.appendChild(
      emptyState('🧡', 'No group experiences yet', 'Activate one from Group → Choose a Moment → Experience in the apps.')
    );
    return;
  }

  const layout = el('div', 'split');
  const listWrap = el('div', 'table-wrap panel');
  const table = el('table');
  const hr = el('tr');
  for (const h of ['When', 'Title', 'Type', 'Status', 'People', 'Organizer']) {
    hr.appendChild(el('th', '', h));
  }
  table.appendChild(el('thead')).appendChild(hr);
  const tbody = el('tbody');
  const detailHost = el('div', 'panel');
  detailHost.appendChild(el('p', 'page-sub', 'Select a row for detail.'));

  const showDetail = async (row: GroupExperienceRow) => {
    detailHost.innerHTML = '';
    detailHost.appendChild(loadingBlock('Loading experience…'));
    try {
      const detail: GroupExperienceDetail = await api.groupExperience(row.momentId);
      detailHost.innerHTML = '';
      detailHost.appendChild(el('h2', 'section-title', detail.title));
      detailHost.appendChild(
        el(
          'p',
          '',
          `${detail.momentTypeCode}${detail.experienceKind ? ` · ${detail.experienceKind}` : ''} · ${
            detail.status === 'ACTIVE' ? 'Activated' : detail.status
          }`
        )
      );
      detailHost.appendChild(el('p', 'mono', detail.momentId));
      if (detail.description) detailHost.appendChild(el('p', '', detail.description));
      detailHost.appendChild(
        el(
          'p',
          '',
          `Dates: ${detail.startAt ? formatTs(detail.startAt) : '—'} → ${detail.endAt ? formatTs(detail.endAt) : '—'}`
        )
      );
      detailHost.appendChild(
        el('p', '', `Organizer: ${detail.organizerName ?? detail.organizerUserId.slice(0, 8) + '…'}`)
      );
      detailHost.appendChild(el('h3', 'section-title', 'People'));
      const plist = el('ul');
      for (const p of detail.participants) {
        plist.appendChild(
          el('li', '', `${p.displayName ?? 'Unknown'} · ${p.roleCode} · ${p.status}`)
        );
      }
      detailHost.appendChild(plist);
    } catch (e) {
      detailHost.innerHTML = '';
      detailHost.appendChild(el('p', '', e instanceof Error ? e.message : 'Failed to load detail'));
    }
  };

  for (const row of list.items) {
    const tr = el('tr');
    tr.style.cursor = 'pointer';
    tr.appendChild(el('td', '', formatTs(row.createdAt)));
    tr.appendChild(el('td', '', row.title));
    tr.appendChild(el('td', '', row.momentTypeCode));
    tr.appendChild(el('td', '', row.status === 'ACTIVE' ? 'Activated' : row.status));
    tr.appendChild(el('td', '', String(row.participantCount)));
    tr.appendChild(el('td', '', row.organizerName ?? row.organizerUserId.slice(0, 8) + '…'));
    tr.addEventListener('click', () => {
      void showDetail(row);
    });
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  listWrap.appendChild(table);
  layout.appendChild(listWrap);
  layout.appendChild(detailHost);
  parent.appendChild(layout);
}

const root = document.getElementById('app')!;
if (getApiKey()) {
  renderApp(root);
} else {
  renderLogin(root);
}

refreshTimer = window.setInterval(() => {
  if (activeTab === 'overview' && getApiKey()) {
    const main = document.querySelector('.main');
    if (main) loadTab(main as HTMLElement, root);
  }
}, 20000);
