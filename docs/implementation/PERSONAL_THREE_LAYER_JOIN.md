# Personal Three-Layer Join — Excel ↔ Backend ↔ iOS/Android

**Date:** 2026-08-30  
**Authority:** [`docs/contracts/Momentra_Personal_1979_Widget_CLEAN_Master_UI_Contract_v1.xlsx`](../contracts/Momentra_Personal_1979_Widget_CLEAN_Master_UI_Contract_v1.xlsx)  
**Matrix:** [`PERSONAL_FIELD_MATRIX.csv`](./PERSONAL_FIELD_MATRIX.csv) (1979 widgets, zero UNKNOWN)  
**Live code:** [`router.ts`](../../backend/typescript/src/api/v1/router.ts) + [`ApiService.kt`](../../apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt) + [`APIClient.swift`](../../momentra/momentra/API/APIClient.swift)

## Status rollup

| Status | Count |
|---|---:|
| WIRED | 1322 |
| CLIENT_FIX | 25 |
| LOCAL_ONLY | 298 |
| DEFERRED | 334 |
| API_GAP | 0 |
| FIGMA_GAP | 0 |
| SCHEMA_GAP | 0 |
| UNKNOWN | 0 |

## By family

| Family | Widgets | WIRED | CLIENT_FIX | LOCAL_ONLY | DEFERRED | API_GAP | FIGMA_GAP |
|---|---:|---:|---:|---:|---:|---:|---:|
| LO | 419 | 247 | 0 | 88 | 84 | 0 | 0 |
| FB | 408 | 284 | 0 | 88 | 36 | 0 | 0 |
| LS | 425 | 295 | 0 | 73 | 57 | 0 | 0 |
| REL | 455 | 265 | 0 | 49 | 141 | 0 | 0 |
| GL | 272 | 231 | 25 | 0 | 16 | 0 | 0 |

## Excel note

The UI contract freezes screens/widgets/commands. **`API Route` is empty for all 1,979 rows.** Join status is derived by mapping logical `API / Command` values onto live `/v1` mounts.

## Known join closures (this program)

1. **Life** — `GET /personal/life` returns `dataQuality: REAL` with honest empties + live `activeAreaCount` / journey from `projection.recent_activity` (no invented Figma scores)
2. **Memory** — `GET /personal/memory` projects `memory.memory` for the user
3. **Attention** — `GET /personal/attention` projects `analytics.attention_capture`
4. **Capability fail-closed** — PersonalActionRegistry empty caps disable destinations (Android + iOS)
5. **Transfer/Savings** — WIRED via `POST …/movements`; Reflect stays DEFERRED; Manage Moment API live but UI CLIENT_FIX
6. **Proof harness** — `tests/personal-three-layer-join.test.ts` (LO / FB / LS / REL)

## Explicitly out

AI Reflect / invented Memory intelligence / redesign of 66 screens / V030 / Money as 5th setup family.
