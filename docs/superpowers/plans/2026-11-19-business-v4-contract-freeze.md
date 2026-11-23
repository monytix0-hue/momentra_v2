# Business V4 — contract freeze (B0)

Locked contracts for the Business V4 rebuild. Rebuild is underway on `feat/business-v4-greenfield`.

## Delivery order (locked)

| Phase | Scope |
|-------|--------|
| B0 | This freeze + Figma map |
| B1 | Workspace, Group-parity invites, catalog |
| B2 | Events, journals, payments, parties, inventory, projection infra |
| B3 | Trading / Wholesale + first Pulse/Moments/Life/Memory |
| B4 | Distribution / Dealership |
| B5 | Marketing Client + Own (v5.1) |
| B6 | Retail Store, Restaurant, Consumer Service |
| B7 | Standard Production + MTO |
| B8 | Cross-industry reconcile |
| B9 | Security, perf, mobile parity, E2E |

One shared backend. Industries are delivery phases, not separate services.

## Operating families and subtypes (9)

| Family | Subtype codes | Phase |
|--------|----------------|-------|
| `TRADING_DISTRIBUTION` | `TRADING_WHOLESALE` | B3 |
| `TRADING_DISTRIBUTION` | `DISTRIBUTION_DEALERSHIP` | B4 |
| `MARKETING_CAMPAIGNS` | `CLIENT_CAMPAIGN`, `OWN_BUSINESS_CAMPAIGN` | B5 |
| `RETAIL_CONSUMER` | `RETAIL_STORE`, `RESTAURANT_FOOD`, `CONSUMER_SERVICE` | B6 |
| `MANUFACTURING_PRODUCTION` | `STANDARD_PRODUCTION`, `MADE_TO_ORDER` | B7 |

Legacy (not operating create path): `TEAM_OPERATIONS`, `BUSINESS_RUNWAY`, `BUSINESS_OPERATIONS`.

## Figma mapping (Obsidian Emerald)

**File:** `rknfN9EikSCdqmqXogR29o` · section `2:67`

| Surface | Node |
|---------|------|
| Section | `2:67` |
| Pulse / Moments / Life / Memory Empty | `2:221` / `2:319` / `2:417` / `2:515` |
| Hub Quick Adds | `2:613` |
| NoCompany | `2:721` |
| Loading / Error / Offline | `2:822` / `2:907` / `2:996` |
| Guided create | `2:1514` |
| Nine setup shells | `5:*` (see `docs/qa/BUSINESS_V4_APPSTATE_SMOKE.md`) |

Operating Quick Add kits (historical): `TzLvwVwlPbeVB8ug1zB3GM`. Specs under `reference/`.

## Hub IA (north-star)

- Hub = CREATE tab / Quick Adds.
- Sections: **Core · Support · Utilities** (Retail-family SoT: Figma file `rknf`).
- Tiles gated by projection `quickAdds` (fail-closed when empty).
- Registry pattern (Group `GroupActionRegistry` shape): tile id → section → capture / masters / utility destination.
- Hubs emit destinations; host mounts sheets. No Wedding-body reuse. No process-global chrome.

## Event write shape (outline)

- `recordOperatingEvent(momentId, payload)` — idempotent ledger write on `business.operating_event` (logical `business_event`).
- Payload includes: subtype, event type / quickAdd code, amounts, parties, line items, GST fields, due/receipt state as applicable.
- After write: refresh operating projection for the moment.

## Projection DTO (outline)

- Single `getOperating(momentId)` → Pulse / Moments / Life / Memory / Hub figures.
- Includes: `quickAdds`, metrics, boards, masters refs, role-scoped visibility.
- Company optional on moment; finance/khata require company when reintroduced.
- Ship four-screen projections from **B3 Trading** onward; later industries extend widgets.

## Invariants

- Spend ≠ payment; payment ≠ second expense; invoice ≠ collection; attributed sales ≠ accounting revenue.
- Moment type immutable after financial posts.
- Invites: Group parity (`inviteCode` + `inviteUrl` for QR, roles on mint/redeem).
- Missing denominator → unavailable, never fake `0%`.

## Architecture target

- Shell → `OperatingMomentHost` (page-scoped chrome, single sheet router, single toast host).
- `OperatingHubRegistry` + catalog for all 9 subtypes.
- Backend modules under `backend/typescript/src/modules/business/`.
- Product SoT: `docs/architecture/BUSINESS_BACKEND.md`.

## Schema note

Reuse `frds/migrations/business/V005`–`V102`. Apply `V103` DROP only on confirmed V4/dev DBs. Additive `V104+` extends foundations without rewriting industry catalogs.
