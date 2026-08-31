# Momentra Telemetry Admin

**Completely separate** from the Momentra mobile apps and the main user API.

| | Mobile app | User API | **This admin** |
|---|---|---|---|
| Folder | `apk/`, `momentra/` | `backend/typescript/` | `admin-dashboard/` |
| Port | — | 3000 | **5180** |
| Auth | Firebase user token | Bearer JWT | **X-Admin-Key** |
| Routes | — | `/v1/*` | **`/admin/api/*`** |

## Setup

1. Apply migration `frds/migrations/V035__client_telemetry.sql` to PostgreSQL.

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

## Admin API (backend)

All routes require header `X-Admin-Key: <ADMIN_API_KEY>`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/api/telemetry/overview` | Totals & 24h stats |
| GET | `/admin/api/telemetry/users` | Users + demographics |
| GET | `/admin/api/telemetry/screen-time` | Time per screen |
| GET | `/admin/api/telemetry/stuck-points` | Where users get stuck |
| GET | `/admin/api/telemetry/widgets` | Widget tap counts |
| GET | `/admin/api/telemetry/personal-setups` | Personal Create setup catalog, activations, screen time |
| GET | `/admin/api/telemetry/events` | Raw event stream |
| GET | `/admin/api/telemetry/sessions` | Session list |

Mobile ingest remains at `POST /v1/telemetry/events` (unchanged).

Personal setup activate (user API): `POST /v1/personal/setups/{LIFE_OPERATIONS|FUTURE_BUILDING|LIFESTYLE|RELATIONSHIPS}/activate` (requires migration `V036__personal_life_system_setup.sql`).

## Production

Build static admin site:

```bash
cd admin-dashboard
npm run build
```

Serve `dist/` on its own subdomain (e.g. `telemetry-admin.yourdomain.com`) — not bundled with the mobile app or public API docs.

Set `VITE_API_BASE_URL` to your production API URL at build time.
