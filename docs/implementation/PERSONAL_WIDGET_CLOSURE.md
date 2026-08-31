# Personal Widget Closure — All Four Families (Life Ops + PX)

**Sources:** Life Ops V042–V045; Personal family freeze [`PERSONAL_FAMILY_PRECISION_FREEZE.md`](PERSONAL_FAMILY_PRECISION_FREEZE.md); migrations V046–V047; three-layer join [`PERSONAL_THREE_LAYER_JOIN.md`](PERSONAL_THREE_LAYER_JOIN.md).

## Freeze boundary

| Scope | Widgets / contracts | Status |
|-------|---------------------|--------|
| Life Operations `PER-LO-*` | 182 | **Precision live** (V042–V045) |
| Future Building `PER-FU-*` | S01–S05 + writers | **Precision live** (V046 + `future-precision.ts`) |
| Lifestyle `PER-LS-*` | S01–S05 + writers | **Precision live** (V046 + `lifestyle-precision.ts`) |
| Relationships `PER-RE-*` | S01–S05 + writers | **Precision live** (V046 + `relationships-precision.ts`) |
| Group settlements | GX-1 | **Capability mapped** (V047); `POST .../settlements` live |
| Excel UI contract | 1,979 widgets / 66 screens | **Join matrix** [`PERSONAL_FIELD_MATRIX.csv`](PERSONAL_FIELD_MATRIX.csv) |

## Family modules (no mega-API)

| Family | Module | Key routes |
|--------|--------|------------|
| Life Ops | `life-ops-precision.ts` | RP-01..05, profile, attention, adjust |
| Future | `future-precision.ts` | future-runtime/axis/inventory/journey, future-profile; enriched `future-items` |
| Lifestyle | `lifestyle-precision.ts` | lifestyle-runtime/vitality/inventory/journey, lifestyle-profile; enriched lifestyle-activities |
| Relationships | `relationships-precision.ts` | relationships-runtime/bond/connections/journey, relationships-profile; enriched relationship-activities |

Shared shell (`/personal/pulse`, setups, activity, life, memory, attention) remains generic. Axis scores are **deterministic from canonical counts** (null when empty) — not fixed-72 heuristics.

## Three-layer join statuses (2026-08-30)

| Surface | Join status | Notes |
|---------|-------------|-------|
| `GET /personal/life` | **WIRED** | `dataQuality: REAL`; honest empties; area counts + journey from activity |
| `GET /personal/memory` | **WIRED** | Projects `memory.memory` |
| `GET /personal/attention` | **WIRED** | Projects `analytics.attention_capture` |
| Transfer / Savings | **WIRED** | `POST …/movements` on Android + iOS |
| Reflect / AI | **DEFERRED** | No invented contract |
| Manage Moment UI | **WIRED** | Gear → Manage Moment (Figma 546:30147); rename `PATCH`; pause `POST …/archive`; complete `POST …/cancel`; delete `DELETE …/moments/:id` (status=DELETED); Edit setup reopens family wizard |
| Empty capabilities | **Fail-closed** | PersonalActionRegistry (Android + iOS) |

Proof: `backend/typescript/tests/personal-three-layer-join.test.ts`.

## Apply order

1. `npm run migrate:install` through **V050** (MOVEMENT_RECORD for Personal Transfer/Savings)  
2. Restart API  
3. Clients use enriched writers + optional axis snapshot GETs; Pulse still consumes `widget_payload` refreshed by precision writers  

## Honesty rule

Every widget is **MAPPED**, **STATIC**, or **honest empty / Coming soon**. Do not invent bond/vitality/future scores on-device. Do not treat Excel empty `API Route` as “no backend needed” for PROJECTION_READ / CANONICAL_WRITE rows.
