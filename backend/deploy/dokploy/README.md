# Momentra Dokploy deploy (Hostinger VPS)

## Create app

| Field | Value |
|--------|--------|
| Type | **Docker Compose** (not Nixpacks) |
| Repository | `momentra_v2` |
| Branch | `main` |
| Compose file | `backend/deploy/dokploy/docker-compose.yml` |
| Build path | repo root `/` (leave empty / default) |

Do **not** set Build Path to `/backend` with Nixpacks.

## What gets deployed

One Node image (`backend/Dockerfile`) shared by:

- `momentra-api` — REST API (`node dist/index.js`)
- `outbox-dispatcher`, `projection-worker`, `notification-worker`, `scheduler`, `memory-worker`, `analytics-worker`
- `redis` — Redis 7
- `fastapi-ai` — Python AI sidecar

## Environment (Dokploy → Environment)

Required for production:

```env
NODE_ENV=production
PORT=3000
DATABASE_URL=postgresql://...pooler.supabase.com:6543/postgres
DATABASE_URL_DIRECT=postgresql://...:5432/postgres
FIREBASE_PROJECT_ID=momentra-v2
FIREBASE_SERVICE_ACCOUNT_JSON={"type":"service_account",...}
ALLOW_DEV_AUTH=0
GOVERNANCE_FAIL_OPEN=0
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_SECRET_KEY=...
MEDIA_BUCKET=momentra-media
MOMENTRA_IDENTITY_NAMESPACE=a1b2c3d4-e5f6-7890-abcd-ef1234567890
CORS_ORIGINS=https://momentra.app
PUBLIC_APP_ORIGIN=https://momentra.app
SCHEMA_RELEASE=V001-V056
MOMENTRA_AI_INTERNAL_KEY=<random-secret>
```

Compose sets `REDIS_URL=redis://redis:6379` and `FASTAPI_AI_URL=http://fastapi-ai:8000` automatically.

**Fail-closed:** if `ALLOW_DEV_AUTH=1` or `GOVERNANCE_FAIL_OPEN=1` with `NODE_ENV=production`, the API will not start.

## Domain

Point your public domain (e.g. `api.momentra.app`) at service **`momentra-api`**, port **3000**.

Do not expose Redis `6379` or FastAPI `8000` publicly.

## Health

```bash
curl https://api.YOUR_DOMAIN/health/live
curl https://api.YOUR_DOMAIN/health/ready
```

## Migrations

Run against Supabase (not inside Dokploy by default):

```bash
cd backend/typescript
npm run migrate:install
```

Ensure ledger includes **V056**.

## Local smoke

```bash
cd backend
docker compose -f deploy/dokploy/docker-compose.yml build
docker compose -f deploy/dokploy/docker-compose.yml up -d
```
