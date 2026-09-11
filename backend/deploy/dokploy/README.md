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
CORS_ORIGINS=https://momentra.app
PUBLIC_APP_ORIGIN=https://momentra.app
SCHEMA_RELEASE=V001-V058
MOMENTRA_AI_INTERNAL_KEY=<random-secret>
ADMIN_API_KEY=<long-random-secret>
ADMIN_CORS_ORIGINS=https://admin.momentra.tech
```

`ADMIN_CORS_ORIGINS` gates the telemetry admin dashboard (`/admin/api/*`) and is separate from
`CORS_ORIGINS`. List origins comma-separated with **no trailing slash** — browsers never send one
in the `Origin` header, and unmatched origins fail preflight with no `Access-Control-Allow-Origin`.

## Domain

Service **`momentra-api`**, port **`3000`**.
