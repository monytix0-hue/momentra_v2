# S2 Group Implementation Plan

**Authority order:** Figma → FRD → Architecture → OpenAPI → SQL → existing code.  
**Rule:** Reuse S0/S1 canonical engines. No `GroupMomentEngine` / `GroupFinanceEngine` duplicates.  
**Stop:** Do not implement Business, Life360, Memory, or AI in this phase.

## Subphase status

| Slice | Status | Notes |
|-------|--------|-------|
| **S2A** Group shell + Shared Experience / Trip | **PASS** | Canonical `POST /v1/moments` (`domainCode=GROUP`, `momentTypeCode=TRIP`); invite mint/bind; Android create unstubbed; iOS already wired; `GET /v1/group/moments` inventory |
| **S2B** Participants / invitations / membership / roles | **PASS** (invite lifecycle) / **NEEDS_PRODUCT_DECISION** (full RBAC matrix) | Live: mint / preview / redeem / bind-on-create / organizer membership. Full role→permission matrix not authoritative enough to invent |
| **S2C** Group Expense + Splits | **API_GAP** | Live expense write is PERSONAL-only; OpenAPI `splits` not implemented in finance service (“splits write deferred”) |
| **S2D** Obligations / Balances / Settlements | **API_GAP** | SQL concepts exist in FRD mapping; no live settlement write/read APIs mounted |
| **S2E** Group Activity + Pulse | **PARTIAL** | Inventory list OK; facet GET returns stub payload; no Group activity keyset / Pulse projection write path yet |
| **S2F** Remaining Group Moment types | **PARTIAL** | UI catalogs + same create engine for Shared Experience / Purchase / Living types; no separate engines. Capability matrix / Quick-Add gating incomplete |

## Vertical-slice order (frozen)

1. S2A → Trip golden path end-to-end  
2. S2B → only after S2A PASS  
3. S2C → only after membership/authz proven  
4. S2D → only where contract supports  
5. S2E → reuse S1 Activity/Pulse patterns  
6. S2F → remaining types via configuration, not new engines  

## Shared reuse (mandatory)

Auth, RequestContext, Moment engine, transactions, idempotency, decimal money, audit, domain events, outbox, projectionHints, AppShell / ContextSwitcher / MomentSwitcher, error/offline infrastructure.

## This delivery (2026-08-25)

### S2A completed

- Mounted on live `router.ts`:
  - `POST /v1/group/invites`
  - `GET /v1/group/invites/:code`
  - `POST /v1/group/invites/:code/redeem`
- Android: `MomentCreateRepository.createGroupMoment` + `mintGroupInvite`; ViewModel unstubbed
- iOS: already called same contracts
- `onMomentCreated` uses current context (Group-safe)
- Backend `tests/group-invite.test.ts` PASS against live router

### Explicitly not done (gaps)

- Group expense + split write path  
- Settlement APIs  
- Authoritative Group Pulse / Activity projections  
- Invented RBAC beyond existing governance checks  

## Next work when unblocked

1. Product decision on Group role/permission codes (S2B remainder)  
2. OpenAPI + SQL-aligned Group expense/split write (S2C) — do not fake in clients  
3. Settlement command surface (S2D) or keep `API_GAP`  
4. Group Activity keyset + Pulse delta (S2E) mirroring Personal  
5. Capability matrix for remaining types / Quick Adds (S2F)
