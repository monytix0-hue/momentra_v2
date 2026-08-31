# S2 Group Gap Matrix

Contradictions and missing contracts recorded without silent resolution.

| ID | Kind | Area | Finding | Impact |
|----|------|------|---------|--------|
| G1 | API_GAP | Invites vs OpenAPI | Live invite routes exist; generated OpenAPI GroupAPI docs do not document `/v1/group/invites*` | Clients use hand-maintained paths; publish OpenAPI when freezing |
| G2 | API_GAP | Group expense | `createExpense` enforces PERSONAL domain | Blocks S2C |
| G3 | API_GAP | Splits write | OpenAPI models splits/strategies; finance service marks splits write deferred | EQUAL/PERCENTAGE/EXACT/SHARES **NOT_SUPPORTED** live |
| G4 | SQL_GAP | Split strategy enum | OpenAPI strategy strings vs SQL persistence of strategy not verified as first-class enum on write path | Align migration before implementing S2C |
| G5 | RESOLVED (GX-1) | Settlements | V047 maps `SETTLEMENT_RECORD` onto expense-capable GROUP types; `POST /v1/moments/:id/settlements` live; snapshot outstanding decremented | Was blocking S2D |
| G6 | API_GAP | Group Activity | No Group activity list on live router | Blocks S2E timeline |
| G7 | PARTIAL | Group Pulse | Facet endpoint returns empty payload shell | Display-only until projection writers exist |
| G8 | NEEDS_PRODUCT_DECISION | Roles / permissions | Organizer + PARTICIPANT exist; full permission matrix / removal rules not authoritative | Do not invent `isAdmin`-style shortcuts |
| G9 | FIGMA_GAP | Budget fields | Prior Phase 6 noted purchase/living budget UI deferred | Keep deferred |
| G10 | ARCHITECTURE_CONFLICT | — | None found for Personal Pulse write path (bounded delta). Group full-rebuild anti-pattern not introduced | — |

## Resolution policy

- Prefer OpenAPI + SQL alignment before client simulation.
- Prefer mounting existing `invite-service` / `moment/service` over new Group engines.
- Mark unsupported split methods as `NOT_SUPPORTED` in UI until write path lands.
