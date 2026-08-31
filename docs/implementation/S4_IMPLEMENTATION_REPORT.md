# S4 Business Implementation Report

**Date:** 2026-08-26  
**Verdict:** S4 BUSINESS — **PASS WITH DOCUMENTED GAPS**  
**Next:** S5 LIFE360 — **NOT STARTED**

```text
S0 PASS · S1 PASS · S2 PERSONAL PASS WITH DOCUMENTED GAPS / CLOSED
S3 GROUP PASS WITH DOCUMENTED GAPS / CLOSED
S4 BUSINESS — COMPLETE (A→O)
STOP — do not start S5
```

---

## Execution

| Step | Outcome |
|------|---------|
| S4-A Audit + matrix | `S4_BUSINESS_AUDIT.md`, `S4_BUSINESS_SCREEN_MATRIX.md`; Figma `649:20260`; SQL relationships proven |
| S4-B Empty | REUSE S1 Business empties; no fake runway cash |
| S4-C Company | live get/patch, locations patch, teams, members list/add, vendors |
| S4-D Moment setup ×3 | Android create wired; iOS already live; prefs JSONB PERSISTED (allowlisted on create path) |
| S4-E–H Populated | Pulse/Moments/Life/Memory Active UIs; projection reads `business_*` snapshots |
| S4-I Activity | `GET …/activity` company-membership gated |
| S4-J Quick Adds | `BusinessActionRegistry` + hubs/sheets |
| S4-L1 Membership | `business/membership.ts` AuthZ chain before finance |
| S4-K Finance | expense (+PURCHASE), revenue, invoice server totals; vendor create |
| S4-L2 Approvals | DRAFT expense + PENDING `approval_request` → APPROVE→POSTED / REJECT→VOIDED |
| S4-M Isolation | atomic company switch; C1/C2 tests |
| S4-N Theme | BUSINESS `#818CF8`; Moment themes FIGMA_GAP as matrix |
| S4-O Tests + docs | `business-s4-finance.test.ts` **4/4 PASS**; parity + this report |

---

## Core finance + approval path

```text
AuthZ: actor → ACTIVE company_membership → company → moment∈company → capability
submit expense
  → amount < threshold: POSTED + business_expense_context + finance_snapshot
  → amount ≥ threshold: DRAFT + approval_request PENDING + SYSTEM step
       → MEMBER decide → 403
       → OWNER/ADMIN + COMPANY_UPDATE → POSTED or VOIDED
revenue / invoice: server computes line → subtotal → tax (passthrough) → total
GET pulse/finance: read projection.business_* only (no calculator)
```

---

## Key files

**Backend:** `modules/business/membership.ts`, `modules/finance/business-finance.ts`, `projection/service.ts` (business facets + activity), `api/v1/router.ts` mounts, `tests/business-s4-finance.test.ts`

**Android:** `ui/shell/business/*`, `BusinessSliceRepository`, AppShell BUSINESS Ready + `businessTabRefreshToken` + atomic `selectCompany`

**iOS:** `Shell/BusinessActive/*`, APIClient business methods, AppShellView/Model company switch

---

## Documented gaps (allowed)

| Gap | Class |
|-----|-------|
| Vendor Operations Moment | DEFERRED |
| Multi-location dashboard | DEFERRED |
| Advanced GST/VAT invent | NOT_REQUIRED / API_GAP |
| FX / multi-currency netting | NOT_REQUIRED |
| Life/Memory secondary richness | EMPTY_SUPPORTED / PARTIAL |
| Sophisticated team workflows | DEFERRED |
| iOS device runtime on Windows | BLOCKED_ENVIRONMENT |
| Path A activate allowlist parity | PARTIAL (createMoment Path B allowlisted) |

---

## E2E journey coverage

```text
Empty → Create Company → select Company → Create Runway Moment
→ expense/PURCHASE → Pulse/finance update
→ add MEMBER → threshold expense DRAFT → MEMBER cannot approve → OWNER approves → POSTED
→ C2 company → no C1 finance leak
→ logout/login restore via bootstrap companies + moments
```

Backend tests cover finance, approval U1/U2, C1/C2 isolation, non-member 403.

---

## Perf notes (S4-M)

Command path uses single DB transaction via `runCommand`; pulse GET reads snapshot rows only. Sample test timings ~2s write / ~0.5s facet read under local DB (not production SLA).

---

## STOP

**Do not start S5 Life360** until explicitly requested.
