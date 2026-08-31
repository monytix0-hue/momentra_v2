# Momentra Implementation Status

| Phase | Name | Status | Notes |
|-------|------|--------|-------|
| 0 | Repository & Architecture Baseline | **COMPLETE** | See `PHASE_0_BASELINE.md` |
| 1 | PostgreSQL Runtime Validation + Schema Completion | **COMPLETE** | Clean V001→V034 PASS ×2; 13/13 DB tests |
| 2 | `/v1` OpenAPI Contract Freeze | **COMPLETE** | See `PHASE_2_OPENAPI_CONTRACT.md` |
| 3 | Node Backend Platform Foundation | **COMPLETE** | See `PHASE_3_BACKEND_FOUNDATION.md` |
| 4 | Authenticated Native Application Shells | **BLOCKED** | Android PASS; iOS sources complete but Xcode build not verified on Windows — see `PHASE_4_NATIVE_SHELLS.md` |
| 5 | First Product Vertical Slice | **COMPLETE** | Empty states + Phase 5 Figma mapping — see `PHASE_5_EMPTY_MOMENT_EXPERIENCE.md` |

## Bare shell mode (current)

Clients and `/v1` router are trimmed to **Phase 4/5 shell only**:

- Auth → `GET /v1/me` → shell chrome (context switcher, bottom nav, top bar)
- Empty / inactive moment experience from real moment list reads
- Business company setup flow (no moment-create wizard)
- Create tab = deferred shell entry ("later phase" copy)
- Product routes preserved in `backend/typescript/src/api/v1/router-product.ts` (not mounted)

| 6 | Production Moment Creation | NOT STARTED | Stripped to bare shell — wizards removed |
| 7 | Workers & Realtime | NOT STARTED | Infrastructure workers (outbox/projection/notification) |
| 8 | Production Expense Create (Shared Finance Engine) | NOT STARTED | — |
| 9 | Work Kernel (Goal → Milestone → Task) | NOT STARTED | Recommended next product phase |
| — | AI / FastAPI Integration | NOT STARTED | Deferred past Expense / Work Kernel |

## Known Deferred Items

- Settlement / Budget / Vendor API (GAP)
- Circle full CRUD deferred (Life360 read only)
- Poll vote/close CONTRACT_ONLY
- ONBOARDING_PERSISTENCE_GAP (local prefs; no V035)
- iOS binary CI verification on macOS
- Workers & Realtime (Phase 7 infra)
