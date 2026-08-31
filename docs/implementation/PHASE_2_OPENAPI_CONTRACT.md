# Phase 2 — `/v1` OpenAPI Contract Freeze

**Status:** COMPLETE  
**Date:** 2026-08-20

## 1. Starting state

Before Phase 2 the repository had:

| Artifact | Location | Role |
|----------|----------|------|
| Partial OpenAPI 3.1 | `backend/typescript/openapi/openapi.yaml` | Mapping Freeze v4 draft (~705 lines); incomplete error responses |
| Legacy stub OpenAPI 3.0 | `backend/typescript/openapi/v1.yaml` | Wrong paths (`POST /v1/personal/moments`, etc.) |
| Express `/v1` router | `backend/typescript/src/api/v1/router.ts` | ~58 product routes already wired |
| Hand-maintained Android DTOs | `apk/.../data/api/ApiService.kt`, `Dto.kt` | Aligned to old `openapi.yaml` |
| iOS bootstrap client | `momentra/momentra/API/APIClient.swift` | Only `GET /v1/me` |
| Figma mapping docs | `docs/mapping/*` | Capability matrix, field contracts |

No single authoritative contract, no reproducible client generation pipeline, no contract tests.

## 2. Contract decisions

| Topic | Decision |
|-------|----------|
| **Authoritative spec** | `backend/typescript/openapi/momentra-v1.yaml` (+ `schemas/common.yaml`, `schemas/responses.yaml`) |
| **Health spec** | `backend/typescript/openapi/health.yaml` (not under `/v1`) |
| **API prefix** | `/v1` for product endpoints |
| **JSON casing** | `camelCase` throughout transport DTOs |
| **Resource IDs** | UUID strings where canonical schema uses UUID |
| **Date/time** | ISO 8601 / RFC 3339 (`format: date-time`) |
| **Money** | `{ "amount": "1250.50", "currencyCode": "INR" }` — decimal **string**, never JSON number |
| **Auth** | `Authorization: Bearer <Firebase ID token>`; 401 = unauthenticated, 403 = governance/forbidden |
| **Success envelope (commands)** | `{ data, correlationId, resourceVersion?, projectionHints? }` |
| **Success envelope (reads)** | `{ data, correlationVersion?, updatedAt?, status?, nextCursor?, correlationId }` |
| **Error envelope** | **Flat** `{ code, message, correlationId, details? }` — matches existing Node `errorHandler` (not nested `{ error: {} }`) |
| **Correlation** | Optional `X-Correlation-Id` request header; always echoed in responses |
| **OCC** | `expectedVersion` on mutable commands; `409` + `VERSION_CONFLICT` |
| **Idempotency** | `Idempotency-Key` header on sensitive creates/commands; `409` + `IDEMPOTENCY_CONFLICT` |
| **Pagination** | Cursor `?cursor=&limit=` (max 100); `nextCursor` + `hasMore` in projection pages |
| **Work ownership** | Single Work engine — commands are **moment-scoped** (`POST /v1/moments/{id}/goals|milestones|tasks`) |
| **Finance ownership** | Single Finance engine — moment-scoped (`/expenses`, `/movements`, `/contributions`) |
| **Poll** | Shared engine — create on moment; read/vote/close on `/v1/polls/{pollId}` |
| **Group entity** | No separate `/v1/groups` CRUD — Group = GROUP-domain **moments** + facet projections |
| **Circle** | Full Circle CRUD **CONTRACT_DEFERRED**; read projection `GET /v1/life360` contracted |
| **Projection hints** | OpenAPI defines typed `{ projection, action }`; runtime still emits `string[]` until Phase 3 alignment |
| **Versioning policy** | `/v1` additive-compatible; breaking changes require `/v2` |

## 3. Domains covered

| Domain | Status | Notes |
|--------|--------|-------|
| Auth / Me | CONTRACTED | `GET /v1/me` |
| Devices | CONTRACTED | Register / revoke |
| Moments | CONTRACTED | CRUD + lifecycle (archive/cancel) |
| Work | CONTRACTED | Goal, Milestone, Task (moment-scoped) |
| Finance | CONTRACTED | Expense, Movement, Contribution (moment-scoped) |
| Poll | CONTRACTED | Create + get/vote/close (vote/close CONTRACT_ONLY impl) |
| Personal reads | CONTRACTED | pulse, moments, life, memory, attention, activity |
| Group reads | CONTRACTED | moments list + facet projections |
| Business | CONTRACTED | companies, locations, teams + business moment facets |
| Company Location | CONTRACTED | List/create/update under company |
| Collaboration | CONTRACTED | planning, bookings, updates, participants, etc. |
| Activity | CONTRACTED | Unified timeline item schema + personal/moment activity reads |
| Media | CONTRACTED | Upload intent + complete |
| Circle | PARTIAL | `GET /life360` only; CRUD deferred |
| Health | CONTRACTED | `/health/live`, `/health/ready` |
| AI | CONTRACTED | Action proposal execute (re-enters command path) |

## 4. Endpoint count

| Metric | Count |
|--------|------:|
| **Total `/v1` operations** | 61 |
| GET | 27 |
| POST | 30 |
| PATCH | 3 |
| DELETE | 1 |
| **Commands** | 34 |
| **Reads** | 27 |
| Health (separate spec) | 2 |

Compact by design: projection-oriented reads, moment-scoped commands — not per-Figma-card routes.

## 5. Generated clients

| Platform | Output | Generator | Status |
|----------|--------|-----------|--------|
| Android | `apk/openapi-generated/` | OpenAPI Generator `kotlin` + Retrofit2/Gson | PASS (generation) |
| iOS | `momentra/momentra/API/Generated/` | OpenAPI Generator `swift5` + URLSession | PASS (generation) |

**Commands:** `npm run openapi:generate` (build → bundle → kotlin → swift)

Hand-maintained `ApiService.kt` remains active in the app until Phase 3 wires the generated module. Android `assembleDebug` PASS with generated output outside `app/src`.

## 6. Contract tests

```text
npm test
→ OpenAPI contract (Phase 2): 9/9 PASS
→ Existing unit tests: 4/4 PASS
Total: 13/13 PASS
```

Validates: parse/$ref resolve, unique operationIds, bearer auth, error responses on commands, idempotency extensions, OCC extensions, decimal-safe money, cursor pagination extensions, health spec.

## 7. Figma mapping coverage

See `PHASE_2_FIGMA_API_MAPPING.md`: **22 CONTRACTED / 25 mapped** (3 GAP: Settlement, Budget, Vendor).

## 8. Known gaps

| Gap | Status | Notes |
|-----|--------|-------|
| Circle CRUD | CONTRACT_DEFERRED | Product model not frozen; only Life360 read |
| Poll vote/close routes | CONTRACT_ONLY | In OpenAPI; router impl Phase 3 |
| Settlement / Budget / Vendor commands | GAP | In capability matrix; no router/spec yet |
| `projectionHints` runtime shape | GAP | Spec typed; backend emits string codes today |
| Split rounding formula | GAP | Strategies enumerated; sum validation formula still placeholder |
| Standalone `/finance/expenses` list | NOT_REQUIRED | Moment-scoped + projections cover UI |
| `/v1/groups` CRUD | NOT_REQUIRED | Group moments architecture |

## 9. Legacy API classification

| Artifact | Classification |
|----------|----------------|
| `openapi/openapi.yaml` | **ADAPT** — superseded by `momentra-v1.yaml`; keep for reference |
| `openapi/v1.yaml` | **DEPRECATE_LATER** — wrong personal-scoped paths |
| `src/api/v1/router.ts` (existing) | **KEEP** — implementation baseline for Phase 3 hardening |
| `/v1/telemetry` | **KEEP** — operational; excluded from product OpenAPI |

## 10. Files changed

- `backend/typescript/openapi/momentra-v1.yaml` (generated)
- `backend/typescript/openapi/momentra-v1.bundled.yaml` (generated)
- `backend/typescript/openapi/schemas/common.yaml`
- `backend/typescript/openapi/schemas/responses.yaml`
- `backend/typescript/openapi/health.yaml`
- `backend/typescript/openapi/endpoint-inventory.json`
- `backend/typescript/scripts/build-openapi.ts`
- `backend/typescript/scripts/bundle-openapi.ts`
- `backend/typescript/scripts/openapi-validate.ts`
- `backend/typescript/scripts/generate-kotlin.ps1`
- `backend/typescript/scripts/generate-swift.ps1`
- `backend/typescript/tests/openapi-contract.test.ts`
- `backend/typescript/package.json`
- `apk/openapi-generated/**` (generated)
- `momentra/momentra/API/Generated/**` (generated)
- `apk/app/src/main/java/com/example/momentra/MomentraApp.kt` (missing import fix)
- `docs/implementation/PHASE_2_*`
- `docs/implementation/IMPLEMENTATION_STATUS.md`
