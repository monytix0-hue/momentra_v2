# Mobile ↔ Backend API gap check (corrected)

Cross-check of iOS (`momentra/`), Android (`apk/`), and TypeScript backend (`backend/typescript`).  
Date context: 2026-09.

## Verdict

**No high-priority DELETE body fixes are required.** Both mobile clients already match backend contracts for moment delete and group-expense void. Admin routes belong on the dashboard only. Remaining product gaps addressed in code: wire SSE on APK + iOS, and iOS LAN API base URL override.

## Claim matrix

| Claim | Backend | iOS | APK | Status |
|-------|---------|-----|-----|--------|
| Moment delete needs `expectedVersion` | `POST /v1/moments/:id/delete` (+ `DELETE` with body); `parseVersion` requires ≥1 | `authorizedPost(…/delete, { expectedVersion })` | `POST …/delete` + `MomentVersionBody` | **False alarm — aligned** |
| Group expense DELETE needs version body | Empty `{}`; Idempotency-Key only | Empty-body DELETE | Header-only DELETE | **False alarm — aligned** |
| Expense attachment DELETE | Path only | `EmptyBody()` | Declared, unused in UI | OK |
| Company invites | `/v1/company/invites` mint/get/redeem | Same | Same | Aligned |
| Admin telemetry / lean / group-experiences | `/admin/api/*` | Not used | Not used | Dashboard only |
| AI compute narrative | TS → Python FastAPI | No direct calls | No direct calls | Correct |
| SSE `GET /v1/realtime/sse` | Emits `PROJECTION_UPDATED` | Client added | `SseClient` wired into shell | Remediated |
| Base URL on physical device | N/A | UserDefaults override + Account UI | `local.properties` / BuildConfig | Remediated (iOS) |

## Do not change

- Moment / group-expense delete payloads on either client  
- Group-expense contract to require `expectedVersion`  
- Admin routes on mobile  

## Smoke checklist

- [ ] Delete moment with current `expectedVersion` → 200  
- [ ] Void group expense with Idempotency-Key only → 200  
- [ ] After a write on another session/tab, shell refreshes via SSE  
- [ ] iOS Account → set LAN API base URL → API calls hit that host  
