# Three-Layer Join — SQL ↔ Backend ↔ iOS/Android

**Date:** 2026-08-30  
**Authority:** live [`router.ts`](../../backend/typescript/src/api/v1/router.ts) + [`ApiService.kt`](../../apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt) + [`APIClient.swift`](../../momentra/momentra/API/APIClient.swift) + [`GX2_FIELD_MATRIX.csv`](./GX2_FIELD_MATRIX.csv)  
**Rule:** zero UNKNOWN. Reclassified after GX2-C mounts.  
**Schema:** `V001-V049` (V049 = `GROUP_VENDOR_MANAGE` + map onto ATTENDANCE_RECORD moment types).

## Status rollup (reclassified)

| Status | Before | After | Delta |
|---|---:|---:|---:|
| WIRED | 933 | 933 | 0 |
| CLIENT_FIX | 1184 | 1386 | 202 |
| API_GAP | 341 | 139 | -202 |
| SCHEMA_GAP | 261 | 261 | 0 |
| FIGMA_GAP | 15 | 15 | 0 |
| LOCAL_ONLY | 1502 | 1502 | 0 |
| DEFERRED | 23 | 23 | 0 |
| UNKNOWN | 0 | 0 | 0 |

**Reclass actions:** {'API_GAP→CLIENT_FIX': 202, 'annotated iOS GET': 260}

## Live join inventory

### Mounted Group collab (SQL + live `/v1`)

| Table | Write | Read | Android | iOS |
|---|---|---|---|---|
| collaboration.planning_item | POST …/planning-items | GET …/planning-items + life | GET+POST | GET+POST |
| collaboration.booking | POST …/bookings | GET …/bookings + life | GET+POST | GET+POST |
| shared.poll | POST …/polls | GET …/polls | GET+POST | GET+POST |
| collaboration.group_update | POST …/updates | GET …/updates + life | GET+POST | GET+POST |
| collaboration.purchase_item | POST …/purchase-items | GET …/purchase-items | GET+POST | GET+POST |
| collaboration.resident | POST …/residents | GET …/residents | GET+POST | GET+POST |
| memory.memory | POST …/memories | GET …/memories + memory facet | GET+POST | GET+POST |
| collaboration.group_vendor | POST …/vendors | GET …/vendors | POST (Wedding Quick Add) | POST (Wedding Quick Add) |
| collaboration.attendance | POST …/attendance | GET …/attendance | POST (Wedding Quick Add) | POST (Wedding Quick Add) |
| collaboration.living_rule | POST …/living-rules | GET …/living-rules | API ready | API ready |
| collaboration.shared_asset | POST …/shared-assets | GET …/shared-assets | API ready | API ready |
| collaboration.maintenance_record | POST …/maintenance-records | GET …/maintenance-records | API ready | API ready |
| finance.* (expense/contribution/settlement/budget) | live | live | live | live |

### Still API_GAP (table exists, no live Group route)

| Table | Figma surface |
|---|---|
| collaboration.ownership_record | Transfer ownership |
| collaboration.delivery_handover | Delivery & handover |

### iOS GET parity

iOS now has list GET clients for planning-items, bookings, polls, updates, purchase-items, residents, and memories. Moments screens prefer list GETs with Life fallback. Wedding Vendor / Attendance Quick Add writes are live on both clients. Living-rule / shared-asset / maintenance list+write APIs are mounted; client sheets deferred to family bind.

### Capability contract

- **V049** seeds `GROUP_VENDOR_MANAGE` (`a1b2c3d4-e5f6-5789-a012-3456789abcde`, owning_service `COLLABORATION`) — distinct from BUSINESS `VENDOR_MANAGE`.
- Mapped onto every moment type that already has `ATTENDANCE_RECORD` (same pattern as V047↔EXPENSE).
- Live `authorize()` (after membership) enforces `resolveCapabilityForMomentType` whenever `input.actionCode` exists in `core.capability` and `momentId` is set. Unmapped → deny with `Capability not enabled for this moment type.` Non-catalog action codes stay membership-only.
- Collab writers that already call `assertGovernanceAllowed` therefore enforce V019 moment-type capability maps.
- `/v1/me` capabilities remain the distinct V019 codes for the user's active moments (bootstrap).
- Android + iOS Quick Add hubs **fail closed** when capability list is empty (do not enable all tiles).

### Product routes — MOUNTED vs DEFERRED

**MOUNTED onto live `router.ts`:**

| Route | Notes |
|---|---|
| PATCH /v1/moments/:id | updateMoment |
| POST …/archive, …/cancel | OCC via expectedVersion |
| POST …/goals, …/milestones, …/tasks | work schema |
| GET /v1/moments/:id/activity | getMomentActivity |
| POST /v1/personal/setups/:code/activate | personal setup |
| GET /v1/personal/attention | getPersonalAttention |
| POST /v1/ai/action-proposals/:id/execute | AI command path |

**Still DEFERRED / not on live (OpenAPI `CONTRACT_DEFERRED` where listed):**

| Route | Notes |
|---|---|
| GET/POST /v1/polls/:pollId… | poll detail/close/vote — contract only |
| ownership_record / delivery_handover writers | SQL exists; no live Group routes |
| Circle full CRUD | GET /life360 only |

## CLIENT_FIX entity rollup (top 15)

| Entity | Widgets |
|---|---:|
| `projection.group_life` | 295 |
| `projection.group_pulse` | 165 |
| `projection.group_memory` | 149 |
| `collaboration.poll` | 99 |
| `projection.available_action` | 96 |
| `collaboration.poll_option` | 89 |
| `collaboration.moment_participant` | 80 |
| `memory.memory` | 80 |
| `collaboration.planning_item` | 72 |
| `projection.group_moments` | 59 |
| `memory.memory_evidence` | 53 |
| `collaboration.purchase_item` | 52 |
| `finance.contribution` | 47 |
| `core.external_party` | 44 |
| `finance.settlement` | 37 |

## Remaining API_GAP entity rollup (top)

| Entity | Widgets |
|---|---:|
| `collaboration.ownership_record` | 26 |
| `collaboration.delivery_handover` | 4 |
| `governance.policy_version` | 4 |
| `governance.policy` | 4 |
| `governance.role_permission` | 4 |
| `finance.expense_resource_link` | 4 |

## Join sequence (this program)

1. **Proof harness** — Trip / Gift Pool / Flatmates golden path (create → SQL → GET list → GET life/pulse).
2. **iOS GET parity** — seven list GETs Android already has (+ new five when clients bind).
3. **Bind Experience** — Wedding/Trip Pulse/Moments/Memory/Quick Add; fix registry destinies; quarantine demo.
4. **Mount remaining SQL** — vendor, attendance, living_rule, shared_asset, maintenance (**done** on live router).
5. **Capability contract** — V019/V049 on writes via `authorize()`; client tiles next.
6. **router-product inventory** — promoted routes listed above; remainder DEFERRED.
7. **Fix stale SQL tooling** — verify-maestro-cert table names; reset-ledger → `momentra_migration_ledger`.

## Explicitly out

UPI/GPay, chat, FX, invented health/AI, fake gallery, Goal/Community product, SET ROLE / RLS rewrite, V030.
