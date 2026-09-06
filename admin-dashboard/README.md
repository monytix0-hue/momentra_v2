# Momentra Telemetry Admin

**Completely separate** from the Momentra mobile apps and the main user API.

| | Mobile app | User API | **This admin** |
|---|---|---|---|
| Folder | `apk/`, `momentra/` | `backend/typescript/` | `admin-dashboard/` |
| Port | — | 3000 | **5180** |
| Auth | Firebase user token | Bearer JWT | **X-Admin-Key** |
| Routes | — | `/v1/*` | **`/admin/api/*`** |

## What it shows

1. **Phase 13 Lean dashboards** (Founder / Product / VC) — product KPIs from `analytics_mart` (WAM, activation, completion, invite→join, participant→creator, etc.). Distinct from GRP-002 Pulse metrics.
2. **Client telemetry playground** — sessions, screen time, stuck points, widget taps (from `analytics.client_*` / V035).
3. **Setup reports** — personal life-system and business family activations.
4. **Group Experiences** — SHARED_EXPERIENCE moment list/detail.

## Setup

1. Apply Lean migrations `V059`–`V068` and client telemetry `V035` / personal setups `V036` as needed:

```bash
cd backend/typescript && npm run migrate:install
```

2. Set admin key on the API server (`backend/.env`):

```env
ADMIN_API_KEY=your-long-random-secret-here
ADMIN_CORS_ORIGINS=http://localhost:5180
CORS_ORIGINS=http://localhost:3000,http://localhost:5180
```

3. Install and run the admin UI:

```bash
cd admin-dashboard
npm install
cp .env.example .env
npm run dev
```

4. Open **http://localhost:5180** and paste the same `ADMIN_API_KEY`.

5. On the **Founder** tab, click **Refresh KPIs** (or run the CLI below) to materialize mart rows.

### Refresh Lean KPIs (CLI)

```bash
cd backend/typescript
npm run analytics:refresh-founder-lean-kpis
# or group-only:
npm run analytics:refresh-group-lean-kpis
```

## Admin API (backend)

All routes require header `X-Admin-Key: <ADMIN_API_KEY>`.

### Phase 13 Lean

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/api/lean/founder` | Founder 12 KPI cards (`v_founder_latest`) |
| GET | `/admin/api/lean/wam-trend` | WAM last 12 weeks |
| GET | `/admin/api/lean/second-moment-cohorts` | Second Moment cohort table |
| GET | `/admin/api/lean/product` | Product sections + decision hints |
| GET | `/admin/api/lean/vc` | VC traction metrics (9) |
| GET | `/admin/api/lean/group-kpis` | Group Lean KPIs 20, 30–35 |
| POST | `/admin/api/lean/refresh` | Materialize Founder + Group KPIs into `kpi_period` |

### Client telemetry

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/api/telemetry/overview` | Totals & 24h stats |
| GET | `/admin/api/telemetry/users` | Users + demographics |
| GET | `/admin/api/telemetry/screen-time` | Time per screen |
| GET | `/admin/api/telemetry/stuck-points` | Where users get stuck |
| GET | `/admin/api/telemetry/widgets` | Widget tap counts |
| GET | `/admin/api/telemetry/personal-setups` | Personal Create setup catalog, activations, screen time |
| GET | `/admin/api/telemetry/business-setups` | Business family setups |
| GET | `/admin/api/telemetry/events` | Raw event stream |
| GET | `/admin/api/telemetry/sessions` | Session list |

### Group experiences

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/api/group-experiences` | SHARED_EXPERIENCE list |
| GET | `/admin/api/group-experiences/:momentId` | Detail + participants |

Mobile ingest remains at `POST /v1/telemetry/events` (unchanged). Lean client events: `POST /v1/analytics/lean/events`.

Personal setup activate (user API): `POST /v1/personal/setups/{LIFE_OPERATIONS|FUTURE_BUILDING|LIFESTYLE|RELATIONSHIPS}/activate` (requires migration `V036__personal_life_system_setup.sql`).

## Production

Build static admin site:

```bash
cd admin-dashboard
npm run build
```

Serve `dist/` on its own subdomain (e.g. `telemetry-admin.yourdomain.com`) — not bundled with the mobile app or public API docs.

Set `VITE_API_BASE_URL` to your production API URL at build time.
