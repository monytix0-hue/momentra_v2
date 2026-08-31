# GX-2 Group Certification

**Status:** CERTIFIED (GX2-A … GX2-L complete for in-scope Group work)  
**Authority:** Figma `575:7980` + Excel field masters + live `router.ts`  
**Philosophy:** REUSE → CONNECT → COMPLETE → VERIFY — no Group UI rebuild.

## Exit checklist

| # | Criterion | Evidence |
|---|---|---|
| 1 | GX2-A matrix has zero UNKNOWN | `docs/implementation/GX2_FIELD_MATRIX.md` / `.csv` |
| 2 | In-scope CLIENT_FIX / API_GAP closed or DEFERRED with reason | GX2-B settlement+splits; GX2-C collab mounts; deferred list below |
| 3 | Golden path per family | Experience Trip / Purchase Gift Pool / Living Flatmates covered by API suite + client Quick Add |
| 4 | Android ↔ iOS matrix parity | Same destinations enabled; same collab commands; same facet payloads |
| 5 | STOP | No Business / Goal / Community / chat / FX / payment rails started |

## What shipped

### GX2-A
- Canonical field matrix published (`GX2_FIELD_MATRIX.*`, `GX2_A_GAP_BACKLOG.md`).

### GX2-B
- Settlement CTA live; payment-method chips LOCAL_ONLY (record intent UI only).
- Split strategies EQUAL | PERCENTAGE | EXACT | SHARES submit to live `computeGroupShares`.

### GX2-C
Live on `backend/typescript/src/api/v1/router.ts`:

| Write | Read |
|---|---|
| `POST /v1/moments/:id/planning-items` | `GET /v1/group/moments/:id/planning-items` |
| `POST /v1/moments/:id/bookings` | `GET /v1/group/moments/:id/bookings` |
| `POST /v1/moments/:id/polls` | `GET /v1/group/moments/:id/polls` |
| `POST /v1/moments/:id/updates` | `GET /v1/group/moments/:id/updates` |
| `POST /v1/moments/:id/purchase-items` | `GET /v1/group/moments/:id/purchase-items` |
| `POST /v1/moments/:id/residents` | `GET /v1/group/moments/:id/residents` |
| `POST /v1/moments/:id/memories` | `GET /v1/group/moments/:id/memories` |

Each writer: membership → capability → txn → canonical table → audit → domain event → outbox → activity → `projectionHints`.

### GX2-D / E / F
- Shared Experience: Pulse tasks, Moments itinerary/bookings/updates, Life/Memory facets bind real data.
- Shared Purchase: purchase-item APIs + Quick Add path available for Gift Pool family moments.
- Shared Living: resident APIs for Flatmates family.
- No fake itinerary / gallery / health scores.

### GX2-G
- Moments list / Pulse / Finance / Activity reuse existing inventory screens; open tasks from planning; AI card remains FIGMA_GAP.

### GX2-H
- Quick Add tiles planning / booking / poll / update / memory: `apiGap = false` when capability present (Android + iOS).
- Wedding demo tiles remain demo unless same live command is behind them.

### GX2-I
- Projection writers touch `group_pulse.task_open_count`, `group_life.planning_payload`, `group_memory.recent_memory_payload`.
- Commands return scoped `projectionHints`; clients refresh via `groupTabRefreshToken` / `refreshVisibleGroupTab`.

### GX2-J
- Android `GroupCollabSheet` + iOS `GroupCollabSheet` mirror the same command set and registry codes.

### GX2-K
- Suite: `backend/typescript/tests/group-gx2-collab.test.ts` (+ existing `group-s3-finance.test.ts` for settlements/isolation).
- Covers membership isolation, audit, event, outbox, purchase + resident family paths.

### GX2-L
- This certification document. **STOP.**

## Explicitly still out of GX-2 (DEFERRED / FIGMA_GAP / LOCAL_ONLY)

| Item | Status |
|---|---|
| Real UPI / GPay / bank processing | DEFERRED — method chips LOCAL_ONLY |
| Chat | DEFERRED |
| FX / multi-currency engine | DEFERRED / SCHEMA_GAP |
| Invented Pulse health-score % | FIGMA_GAP |
| Invented AI Group insights | FIGMA_GAP |
| Fake itinerary / gallery | forbidden (honest empty) |
| Setup prefs pretending to persist | LOCAL_ONLY |
| Unsupported RBAC / remove-participant | DEFERRED |
| Shared Goal / Community product | DEFERRED |
| New schema without Excel proof | forbidden |

## Verification commands

```bash
cd backend/typescript
npm test -- tests/group-gx2-collab.test.ts
npm test -- tests/group-s3-finance.test.ts
```

Android: Quick Add → Planning / Booking / Poll / Update / Memory → Pulse/Moments/Life/Memory refresh.  
iOS: same destinations via `GroupCollabSheet`.

## STOP

GX-2 Group Integration is complete for the frozen sequence. Do not start Business, Goal, Community, chat, FX, or payment rails under GX-2.
