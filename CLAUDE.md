Momentra
Life-and-work product: one Firebase account across Personal, Group, and Business. Each context is a set of moments, always read through four surfaces: Pulse, Moments, Life, Memory.

This is a single git repo (origin: https://github.com/monytix0-hue/momentra_v2.git), not a package monorepo (no root package.json). Current work often lives on feat/business-v4-greenfield.

Native apps talk to one TypeScript API over Postgres. Writes are commands. Reads are projections. Public API: https://api.momentra.tech. Invites: https://momentra.tech/j/{code} and /c/{code}.

Deeper architecture: docs/architecture/ARCHITECTURE.md and docs/architecture/BUSINESS_BACKEND.md. Surface contract: docs/implementation/MOMENTRA_SURFACE_DOCTRINE.md.

Directory map
Path	What it is
backend/typescript/	Live API (momentra-api): Express, /v1 + /admin/api. Entry: src/index.ts, src/app.ts. Modules under src/modules/ (account, ai, analytics, business, collaboration, device, finance, governance, media, memory, moment, notifications, personal, projection, story, telemetry, work). Platform kit: src/platform/. Tests: tests/.
backend/workers/	BullMQ workers: outbox-dispatcher, projection, notification, scheduler, memory, analytics. Run via npm scripts in backend/typescript.
backend/python/fastapi-ai/	Optional FastAPI compute plane (narrative/forecast). No CRUD, no unrestricted DB. Internal key: MOMENTRA_AI_INTERNAL_KEY.
backend/deploy/dokploy/	VPS Compose notes. Root docker-compose.yml + root Dockerfile are what Dokploy uses.
frds/migrations/	Postgres schema source of truth (V001–V105+), buckets personal/, group/, business/, shared/. Apply order: frds/manifest/MIGRATION_ORDER.txt.
momentra/	iOS SwiftUI app (momentra.xcodeproj). App code: momentra/momentra/ (momentraApp.swift, API/APIClient.swift, Shell/). Widget: MomentraWidgetExtension/. Tests: momentraTests/.
apk/	Android Jetpack Compose app (namespace com.example.momentra, applicationId resolvingpoint.momentra). UI: apk/app/src/main/java/com/example/momentra/.
admin-dashboard/	Vite telemetry admin (port 5180). Auth is X-Admin-Key, not user JWT. src/api.ts, src/main.ts.
web/invite/	Static invite + Universal/App Link site for momentra.tech (Vercel). Firebase Hosting config also points here.
design/	Token/taxonomy sources (tokens/, taxonomy/, assets/). Pipeline: tools/design-pipeline/ (npm run all / npm run check). Generated outputs are committed.
docs/	Architecture, implementation, legal, QA reports. Prefer docs/architecture/ over screenshot dumps.
reference/	Product RFC/docx/xlsx source docs. Read only when a requirement is unclear.
scripts/	QA/Figma helper scripts (scripts/qa/, scripts/figma/).
firestore.rules, firebase.json	Firebase project momentra-v2. Apps also have GoogleService-Info.plist / google-services.json (tracked client config).
Commands
API (from backend/typescript/; loads backend/.env):


npm install
npm run dev                 # tsx watch, port 3000
npx tsc --noEmit            # typecheck (also hooked after TS edits)
npm test                    # tsx --test tests/**/*.test.ts
npm run migrate:check-order
npm run migrate:install     # apply pending FRDS migrations
npm run openapi:build && npm run openapi:bundle && npm run openapi:validate
npm run worker:outbox       # also: worker:projection, worker:notification,
                            # worker:scheduler, worker:analytics, worker:memory
Full stack (repo root): docker compose up — API published at localhost:3001 → container 3000; FastAPI exposed 8000; Redis included.

Admin: cd admin-dashboard && npm install && npm run dev → http://localhost:5180

Android (from apk/): ./gradlew :app:assembleDebug or :app:installDebug; unit tests :app:testDebugUnitTest.

iOS: open momentra/momentra.xcodeproj, run target momentra (shared scheme may only list the widget). Helper scripts in momentra/scripts/.

Design: cd tools/design-pipeline && npm ci && npm run all (or npm run check in CI).

Conventions
Point at the files you will change. Do not explore the whole repo first.
Do not edit .env, local.properties, keystores, *firebase-adminsdk*.json, or anything under backend/.secrets/. Use .env.example / apk/local.properties.example for required names only.
Migrations live in frds/migrations/{bucket}/V###__name.sql and must be added to frds/manifest/MIGRATION_ORDER.txt. Never apply V030 (prod validation gate) or V103 (drops business schema) via the normal migrator. V103 requires an explicit ALLOW_DESTRUCTIVE_V103=1 on a throwaway DB.
API routes: /v1 (and /api/v1) for apps; /admin/api for the dashboard; /health/live and /health/ready. New business logic goes in src/modules/, not a one-off script.
The API uses pg + SQL, not Prisma (ignore stale reviewer notes). Firebase Auth for users; ALLOW_DEV_AUTH / X-Dev-Firebase-Uid is local-only and can be disabled with ALLOW_DEV_AUTH=0.
Do not hand-edit generated design files (**/Design/Generated/**, apk/.../ui/theme/generated/**) or OpenAPI codegen (momentra/OpenAPI/Generated/, apk/openapi-generated/). Change sources or the generator, then regenerate.
Keep iOS and Android shells in parity when you change a user-facing surface. Figma file: see figma.config.json.
Prefer git commit only when asked. Do not force-push, and do not schedule/deploy.
Env files (names only)
Local .env files exist at backend/.env, backend/typescript/.env, and admin-dashboard/.env (all gitignored). Typical names:

PORT, NODE_ENV, MOMENTRA_ENV, DATABASE_URL, DATABASE_URL_DIRECT, DB_POOL_*, SUPABASE_URL, SUPABASE_SECRET_KEY, MEDIA_BUCKET, REDIS_URL, FIREBASE_PROJECT_ID, FIREBASE_SERVICE_ACCOUNT_JSON, FIREBASE_WEB_API_KEY, ALLOW_DEV_AUTH, CORS_ORIGINS, ADMIN_API_KEY, ADMIN_CORS_ORIGINS, SCHEMA_RELEASE, FEATURE_FLAGS, SENTRY_DSN, GOVERNANCE_FAIL_OPEN, MOMENTRA_AI_INTERNAL_KEY, MOMENTRA_IDENTITY_NAMESPACE / MOMENTRA_UUID_NAMESPACE, PUBLIC_APP_ORIGIN, INVITE_DISPLAY_ORIGIN.

Admin UI (dev only): VITE_API_BASE_URL, VITE_ADMIN_API_KEY (never a production admin key — Vite inlines VITE_*). Android apk/local.properties: sdk.dir, API_BASE_URL, GOOGLE_WEB_CLIENT_ID.

Do not read (heavy / generated / secrets)
Skip these unless a task truly needs a specific file:

.dd/ (Xcode DerivedData, tens of GB), .worktrees/, .git/, .idea/, .tmp/, .firebase/
**/node_modules/, **/dist/, **/build/, **/.gradle/, **/.kotlin/, **/.spm-cache/, **/.build/, **/venv/, **/.venv/, **/__pycache__/
momentra/momentra/Assets.xcassets/, apk/app/src/main/res/drawable-nodpi/, mipmaps (multi‑MB PNGs)
docs/qa/screenshots/, docs/qa/*-walkthrough-*/ (tens of MB of device photos)
momentra/OpenAPI/Generated/, apk/openapi-generated/, design Generated/ outputs
Root leftover node_modules/ (no root package.json)
Any .env, local.properties, *.jks / *.keystore, *firebase-adminsdk*.json, backend/.secrets/
