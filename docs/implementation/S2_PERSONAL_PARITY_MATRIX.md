# S2 Personal Parity Matrix

**Date:** 2026-08-30  
**Figma:** `TzLvwVwlPbeVB8ug1zB3GM`  
**Join authority:** [`PERSONAL_FIELD_MATRIX.csv`](PERSONAL_FIELD_MATRIX.csv) · [`PERSONAL_THREE_LAYER_JOIN.md`](PERSONAL_THREE_LAYER_JOIN.md)  
**Statuses:** PASS | PARTIAL | FAIL | API_GAP | EMPTY_SUPPORTED | FIGMA_GAP | DEFERRED | BLOCKED_ENVIRONMENT | NOT_REQUIRED | CLIENT_FIX

| Surface | Backend | Android | iOS | Notes |
|---------|---------|---------|-----|-------|
| Personal empty (S1 shell) | — | PASS | PASS | Educational preview ≠ metrics |
| Offline / retry | — | PASS | PASS | S1 shell states |
| Setup ×4 + create | PASS | PASS | PASS | `POST /moments` + `personalSetup`; activate NOT_REQUIRED |
| Create → auto-select Pulse | — | PASS | PASS | `onMomentCreated` |
| Pulse LifeOps/Future | PASS | PASS | PASS | Real projection |
| Pulse Lifestyle | PASS | PASS | PASS | Dedicated Vitality Index; axis from `widgetPayload` |
| Pulse Relationships | PASS | PASS | PASS | Bond Index + axes from activity writes |
| Moments LifeOps/Future | PASS | PASS | PASS | |
| Moments Lifestyle/Relationships | PASS | PASS | PASS | pulse + activity fetch |
| Life tab wire | PASS | PASS | PASS | `dataQuality=REAL`; honest empties; drift/leverage still API_GAP |
| Life Ops precision (V042–V045) | PASS | PASS | PASS | See `PERSONAL_WIDGET_CLOSURE.md` |
| Future/Lifestyle/Relationships precision (V046) | PASS | PASS | PASS | Family modules; deterministic axes |
| Life sections | PARTIAL | PARTIAL | PARTIAL | `activeAreaCount` + journey REAL; scores EMPTY_SUPPORTED |
| Memory GET | PASS | PASS | PASS | Projects `memory.memory`; honest empty when none |
| Memory Lifestyle/Relationships | PASS | PASS | PASS | Prefer GET /memory when non-empty; pulse/activity fallback OK |
| Attention GET | PASS | PASS | PASS | Projects `analytics.attention_capture` |
| Activity keyset | PASS | PASS | PASS | |
| Expense QA | PASS | PASS | PASS | Critical path |
| LifeOps/Future/Lifestyle QA | PASS | PASS | PASS | |
| Relationships QA write | PASS | PASS | PASS | Bond axes from activity_type counts |
| Transfer / Savings | PASS | PASS | PASS | `POST …/movements` + MOVEMENT_RECORD |
| Reflect | DEFERRED | DEFERRED | DEFERRED | Hub tile disabled; no invented AI |
| Manage Moment | PASS routes | CLIENT_FIX | CLIENT_FIX | PATCH/archive/cancel live; dedicated Manage UI unbound |
| PersonalActionRegistry (V019) | V019 seed | PASS fail-closed | PASS fail-closed | Empty caps disable destinations |
| Future theme emerald | — | PASS | PASS | `#10B981` |
| Dual router G1 | PASS | — | — | Live authoritative |
| Multi-Moment activity scope | PASS | PASS | PASS | `?momentId=` |
| Scoped tab refresh | hints | PASS | PASS | Quick Add → Pulse/Moments/Memory refresh |
| Family golden-path proof | PASS | — | — | `personal-three-layer-join.test.ts` |
| E2E journey (device) | API covered | PARTIAL | BLOCKED_ENVIRONMENT | Android unit + API; full device journey follow-up |
| iOS Xcode runtime | — | — | BLOCKED_ENVIRONMENT | Windows host |

**Join note (2026-08-30):** Excel UI contract is the widget authority; empty Excel `API Route` is not “no backend.” Status comes from live `/v1` + client bind. Life no longer invents FIGMA_SEEDED scores.
