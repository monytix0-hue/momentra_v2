# Phase 2 — Figma / Product Action → API Mapping

Maps **executable** Figma Action Center capabilities and shell loads to the frozen `/v1` contract.  
Does not map every visual card — projection reads cover screen assembly.

Status key: **CONTRACTED** | **DEFERRED** | **NOT_REQUIRED** | **GAP**

## Shell / bootstrap

| Screen | User action | Contract | Status |
|--------|-------------|----------|--------|
| Auth | Sign in (Firebase) | External Firebase SDK | NOT_REQUIRED |
| App bootstrap | Load identity | `GET /v1/me` | CONTRACTED |
| Personal shell | Load Pulse | `GET /v1/personal/pulse` | CONTRACTED |
| Personal shell | Load Moments list | `GET /v1/personal/moments` | CONTRACTED |
| Personal shell | Load Life | `GET /v1/personal/life` | CONTRACTED |
| Personal shell | Load Memory | `GET /v1/personal/memory` | CONTRACTED |
| Personal shell | Load Activity | `GET /v1/personal/activity` | CONTRACTED |
| Group shell | Load group moments | `GET /v1/group/moments` | CONTRACTED |
| Group moment | Load facet (pulse/life/memory/finance/actions) | `GET /v1/group/moments/{id}/{facet}` | CONTRACTED |
| Business shell | Load business moments | `GET /v1/business/moments` | CONTRACTED |
| Business moment | Load facet | `GET /v1/business/moments/{id}/{facet}` | CONTRACTED |
| Business | Company selector | `GET /v1/companies` | CONTRACTED |
| Circle / Life360 | Load circle view | `GET /v1/life360` | CONTRACTED |

## Action Center commands (capability matrix)

| Screen / chip | User action | Contract | Status |
|---------------|-------------|----------|--------|
| Personal → Create Moment | Save | `POST /v1/moments` | CONTRACTED |
| Personal → Life system setup activate | Activate | `POST /v1/personal/setups/{systemCode}/activate` | CONTRACTED |
| Personal → List setups | Load | `GET /v1/personal/setups` | CONTRACTED |
| Moment → Rename | Save | `PATCH /v1/moments/{momentId}` | CONTRACTED |
| Moment → Expense / Money | Save | `POST /v1/moments/{momentId}/expenses` | CONTRACTED |
| Moment → Task | Save | `POST /v1/moments/{momentId}/tasks` | CONTRACTED |
| Moment → Goal / Investment | Save | `POST /v1/moments/{momentId}/goals` | CONTRACTED |
| Moment → Poll | Save | `POST /v1/moments/{momentId}/polls` | CONTRACTED |
| Poll | Vote | `POST /v1/polls/{pollId}/votes` | CONTRACTED |
| Poll | Close | `POST /v1/polls/{pollId}/close` | CONTRACTED |
| Moment → Planning item | Save | `POST /v1/moments/{momentId}/planning-items` | CONTRACTED |
| Moment → Booking | Save | `POST /v1/moments/{momentId}/bookings` | CONTRACTED |
| Moment → Contribution | Save | `POST /v1/moments/{momentId}/contributions` | CONTRACTED |
| Moment → Settlement | Save | — | **GAP** |
| Moment → Memory | Save | `POST /v1/moments/{momentId}/memories` | CONTRACTED |
| Moment → Update | Post | `POST /v1/moments/{momentId}/updates` | CONTRACTED |
| Moment → Add people | Save | `POST /v1/moments/{momentId}/participants` | CONTRACTED |
| Moment → Resident | Save | `POST /v1/moments/{momentId}/residents` | CONTRACTED |
| Moment → Purchase item | Save | `POST /v1/moments/{momentId}/purchase-items` | CONTRACTED |
| Moment → Budget | Save | — | **GAP** |
| Business → Vendor | Save | — | **GAP** |
| Business → Company location | Create | `POST /v1/companies/{companyId}/locations` | CONTRACTED |
| Media (receipt/avatar) | Upload | `POST /v1/media/uploads` → complete | CONTRACTED |
| Device | Register for push | `POST /v1/me/devices` | CONTRACTED |

## Personal life commands (non–Action Center)

| Action | Contract | Status |
|--------|----------|--------|
| Record observation | `POST /v1/moments/{momentId}/observations` | CONTRACTED |
| Future item | `POST /v1/moments/{momentId}/future-items` | CONTRACTED |
| Lifestyle activity | `POST /v1/moments/{momentId}/lifestyle-activities` | CONTRACTED |
| Relationship activity | `POST /v1/moments/{momentId}/relationship-activities` | CONTRACTED |
| Record movement | `POST /v1/moments/{momentId}/movements` | CONTRACTED |

## Deferred / not required

| Item | Status | Reason |
|------|--------|--------|
| Circle create/manage | DEFERRED | Canonical Circle model not frozen beyond Life360 read |
| Standalone `/v1/groups` CRUD | NOT_REQUIRED | Group = GROUP-domain moments |
| Standalone `/v1/work/goals` list | NOT_REQUIRED | Moment-scoped Work engine + projections |
| Standalone `/v1/finance/expenses` list | NOT_REQUIRED | Moment-scoped Finance + facet reads |
| Per-card Pulse API | NOT_REQUIRED | Single projection read per shell |

## Coverage count

| Metric | Value |
|--------|------:|
| Actions mapped | 25 |
| CONTRACTED | 22 |
| GAP | 3 |
| DEFERRED | 1 (Circle CRUD) |
| NOT_REQUIRED | (see table) |

**Contract coverage:** 22/25 executable matrix actions = 88% (gaps are Settlement, Budget, Vendor — require product/schema decision before contracting).
