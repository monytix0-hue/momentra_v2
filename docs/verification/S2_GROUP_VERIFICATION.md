# S2 Group Verification

**Date:** 2026-08-25  
**Gate prerequisite:** [S0_S1_RELEASE_GATE.md](../verification/S0_S1_RELEASE_GATE.md) — S0/S1 PASS.

---

## Subphase results

| Slice | Result | Evidence |
|-------|--------|----------|
| S2A Trip | **PASS** | Create Trip via `POST /v1/moments`; invite mint/bind/redeem mounted; Android create wired; iOS source parity; `group-invite.test.ts` PASS |
| S2B Membership | **PASS** (invite + organizer) / **BLOCKED** (full RBAC) | Redeem membership proven in test; role matrix **NEEDS_PRODUCT_DECISION** |
| S2C Expense/Splits | **API_GAP** | PERSONAL-only expense; splits deferred |
| S2D Obligations/Settlement | **API_GAP** | No live settlement API |
| S2E Activity/Pulse | **PARTIAL** | Group moments list OK; Pulse/Activity projections incomplete |
| S2F Remaining setups | **PARTIAL** | Catalogs + shared create engine; Quick-Add capability matrix incomplete |

---

## Client parity

| Platform | Result | Notes |
|----------|--------|-------|
| Android | **PASS** | `compileDebugKotlin` + `AppShellViewModelTest`; Group create/mint no longer stubbed |
| iOS source | **PASS** | `MomentCreateRepository` / `APIClient` invite + create already present |
| iOS runtime | **BLOCKED_ENVIRONMENT** | No Xcode on Windows verification host |

---

## Backend tests

| Suite | Result |
|-------|--------|
| Full `npm test` | 55/55 PASS (includes S0/S1 regression) |
| `tests/group-invite.test.ts` | PASS against live router (mint → create TRIP → redeem) |

---

## Performance (S2A scope)

| Check | Result |
|-------|--------|
| Create uses single command transaction | PASS |
| No synchronous full projection rebuild on create GET path | PASS |
| Inventory list is membership-scoped SQL (bounded) | PASS |
| Numeric p50/p95 under load | Not re-benchmarked this run — architecture checks PASS |

---

## Architecture

| Rule | Result |
|------|--------|
| No duplicate Group engines | PASS |
| Reuse Moment / invite / command / audit / outbox | PASS |
| No invented Group expense/settlement writes | PASS (explicit API_GAP) |
| S0/S1 not rewritten | PASS |

---

## Remaining product / API gaps (genuine)

1. Publish OpenAPI for `/v1/group/invites*` (G1)  
2. Group-domain expense write + transactional splits (G2–G4)  
3. Settlement / obligation APIs (G5)  
4. Group Activity keyset + Pulse projections (G6–G7)  
5. Authoritative Group role/permission / removal product rules (G8)
