# Momentra Dokploy deploy (Hostinger VPS)

## Create Compose app

| Field | Value |
|--------|--------|
| Type | **Docker Compose** |
| Repository | `momentra_v2` |
| Branch | `main` |
| **Compose file** | `docker-compose.yml` |
| Build / base path | leave empty |

Use **forward slashes only**. Never enter `backend\deploy\...` — Linux VPS will not find that path.

## What gets deployed

- `momentra-api` — REST API port 3000
- `outbox-dispatcher`, `projection-worker`, `notification-worker`, `scheduler`, `memory-worker`, `analytics-worker`
- `redis`
- `fastapi-ai`

## Environment

```env
NODE_ENV=production
PORT=3000
DATABASE_URL=...
DATABASE_URL_DIRECT=...
FIREBASE_PROJECT_ID=momentra-v2
FIREBASE_SERVICE_ACCOUNT_JSON=...
# FCM: paste service account JSON as one line. Restart notification-worker only.
# Verify: worker log should include "fcm":true / outcome fcm_ready
# Ops scripts (from backend/typescript): check-fcm-activation.mjs, fcm-smoke-send.mjs
ALLOW_DEV_AUTH=0
GOVERNANCE_FAIL_OPEN=0
SUPABASE_URL=...
SUPABASE_SECRET_KEY=...
MEDIA_BUCKET=momentra-media
MOMENTRA_IDENTITY_NAMESPACE=a1b2c3d4-e5f6-7890-abcd-ef1234567890
CORS_ORIGINS=https://momentra.tech,https://momentra.app
PUBLIC_APP_ORIGIN=https://momentra.tech
SCHEMA_RELEASE=V001-V058
MOMENTRA_AI_INTERNAL_KEY=<random-secret>
ADMIN_API_KEY=<long-random-secret>
ADMIN_CORS_ORIGINS=https://admin.momentra.tech
```

Optional invite override (defaults to `PUBLIC_APP_ORIGIN` / `https://momentra.tech`):

```
INVITE_DISPLAY_ORIGIN=https://momentra.tech
```

`ADMIN_CORS_ORIGINS` gates the telemetry admin dashboard (`/admin/api/*`) and is separate from
`CORS_ORIGINS`. List origins comma-separated with **no trailing slash** — browsers never send one
in the `Origin` header, and unmatched origins fail preflight with no `Access-Control-Allow-Origin`.

## Domain

Canonical API host: **`https://api.momentra.tech`**.

This hostname is **not** a Vercel app. Apex `momentra.tech` stays on Vercel (invite/marketing).
In Vercel DNS, `api` is an **A record** to the Hostinger VPS (`200.141.7.52`). Do **not** add
`api.momentra.tech` as a domain on the Vercel project.

In Dokploy (Compose app → Domains):

| Field | Value |
|--------|--------|
| Host | `api.momentra.tech` |
| Service | **`momentra-api`** |
| Port | **`3000`** (container listen port) |
| HTTPS | enabled (Let's Encrypt) |

Do **not** use host port **`3001`**. Compose publishes `3001:3000` only because host `3000` is
the Dokploy UI. Traefik/Caddy talks to the container on the Docker network, so domain port
`3001` produces HTTPS **502 Bad Gateway** while `http://<vps>:3001/health/live` still returns
`{"status":"ok"}`.

Verify after saving the domain:

```bash
curl -sS https://api.momentra.tech/health/live
# expect: {"status":"ok"}
curl -sS https://api.momentra.tech/health/ready
# expect: {"status":"ok"}
```

Clients use `https://api.momentra.tech/` as `API_BASE_URL` / `MomentraAPIBaseURL`.

### Direct IP fallback (HTTP)

While HTTPS on `api.momentra.tech` is broken, the API is reachable at:

```text
http://200.141.7.52:3001/
```

- **Android:** set `API_BASE_URL=http://200.141.7.52:3001/` in `apk/local.properties` (see `local.properties.example`).
- **iOS:** Account → Developer → **Use Dokploy direct (200.141.7.52:3001)**.
- **Do not ship** this as the production default — use only for testing until TLS works.
