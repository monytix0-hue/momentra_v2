# S4 Business Audit

**Date:** 2026-08-26  
**Scope:** Business vertical before S4-B…O production edits  
**Figma:** file `TzLvwVwlPbeVB8ug1zB3GM` / section [`649:20260`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=649-20260) (MCP metadata verified)  
**Rule:** Prefer REUSE/REFINE/COMPLETE. **No Business finance or approval endpoints until this audit + screen matrix exist and SQL relationships are proven.**  
**Guardrails:** AuthZ = membership → company → moment → capability; server-authoritative finance; two-stage approval; Pulse = read model; J → L1 → K → L2 order.

---

## Executive summary

| Finding | Classification |
|---------|----------------|
| Empty tabs + Company setup wired both platforms | **REUSE** |
| iOS Business create/setup live; Android create **stubbed** | **REFACTOR** wire Android |
| Active Business Pulse/Moments/Life/Memory = “later phases” stubs | **MISSING** |
| Live company list/create + locations create | **REUSE** |
| Company get/patch, location patch, teams, setups, facets | product-only → **PROMOTE** |
| `business_expense_context` / revenue / invoice SQL ready; **no TS writers** | **MISSING** after L1 |
| `expense.status` includes **DRAFT** (default); personal/group writers use POSTED | Two-stage via DRAFT + approval |
| `approval_request` / step / decision SQL ready; **no TS** | **MISSING** after L1/K |
| `projection.business_*` tables exist; GET returns `payload: {}` | **API_GAP** / writers needed |
| Vendor / vendor_contract SQL ready; no writers | PASS link-or-skip; Vendor Ops Moment **DEFERRED** |
| Dual-router: product NON-RUNTIME; promote onto live only | **G1** |
| Multi-location dashboard Figma | **DEFERRED** |
| No FX in V001–V029 | **No invented FX** |

---

## Hierarchy invariant (proven)

```text
BUSINESS (domain)
  → business.company
  → business.company_membership (ACTIVE OWNER|ADMIN|MEMBER|…)
  → business.business_moment_context (moment_id, company_id, business_family)
  → DATA / TEAM / FINANCE / WORK (company_id + moment_id where applicable)
```

**AuthZ chain (every mutation):**

```text
actor → ACTIVE company_membership → company
  → moment belongs to company (business_moment_context)
  → capability / Governance role mapping
```

Never authorize Business finance/work from Moment participant tables alone.

---

## Finance SQL relationships (proven — V007)

```text
finance.expense (domain_code=BUSINESS, status DRAFT|POSTED|VOIDED|REVERSED)
  → finance.business_expense_context
       (expense_id, moment_id, company_id, vendor_id?, vendor_contract_id?)
  → FK (moment_id, company_id) → business.business_moment_context
  → optional vendor → business.vendor (vendor_id, company_id)
```

```text
finance.revenue (company_id, moment_id?, amount, currency, status DRAFT|POSTED|…)
  → FK (moment_id, company_id) → business_moment_context when moment set
```

```text
finance.invoice (company_id, moment_id?, subtotal_amount, tax_amount, total_amount, status…)
  → finance.invoice_line (quantity, unit_price, tax_amount, line_total)
  → finance.invoice_payment
```

**Purchases:** no `purchase` table. S4 uses `expense.category_code = 'PURCHASE'` (canonical). No shadow mobile purchase object.

**Tax:** invoice has `tax_amount` columns; expense has no tax column (optional `expense_split` TAX line). **Do not invent GST/VAT rules** — server sets `tax_amount` from client-supplied line tax only if provided; otherwise `0` and classify advanced tax as **NOT_REQUIRED** / **API_GAP**.

**FX:** none in V001–V029. Single currency per write; no cross-currency invent.

### Approval lifecycle (chosen from schema)

Schema supports **both**:

1. `expense.status = DRAFT` until approved → `POSTED`
2. `governance.approval_request` soft-link via `(resource_type, resource_id)` + steps/decisions

**S4 model (locked):**

```text
submit expense/PURCHASE
  → if amount < threshold (or no threshold): insert expense POSTED + business_expense_context + project
  → if amount ≥ threshold:
       insert expense DRAFT + business_expense_context
       insert approval_request PENDING (resource_type=EXPENSE, resource_id=expense_id, scope=COMPANY)
       → approve: decision + expense.status POSTED + projection upsert
       → reject: decision + expense VOIDED (or stay DRAFT+REJECTED request — prefer VOIDED for audit clarity)
```

Never create POSTED then cosmetically approve.

Threshold source: `business_system_setup.preferences.approvalThreshold` / `approvalAlarm` (parsed server-side) when `spendingApproval` / `approvalModel` requires approval; else post directly.

Approve rights: **Governance capability mappings** (e.g. COMPANY_UPDATE / finance approve), not native `OWNER` enum alone — map OWNER/ADMIN membership_type through `assertGovernanceAllowed` / explicit capability check documented in L1.

---

## Projection tables (V014) — read models

| Table | PK | Role |
|-------|-----|------|
| `projection.business_pulse` | `company_id` | company-level pulse widgets |
| `projection.business_life` | `company_id` | family payloads |
| `projection.business_memory` | `company_id` | memory counts/payload |
| `projection.business_finance_snapshot` | `(company_id, currency_code)` | expense/revenue/invoice aggregates |
| `projection.pending_approval_summary` | per user+request | approval inbox |

**Rule:** writers upsert on finance/approval/work events. `getBusinessMomentProjection` **reads** stored payloads (filter/assemble by moment where needed) — **never** recomputes finance aggregates on GET.

Note: pulse/life/memory/finance_snapshot are **company-scoped** PKs. Moment-scoped UI must combine company snapshot + moment-filtered activity/expense lists without scanning all expenses on every GET (use snapshot_payload / recent_activity keyed by moment).

---

## Membership / team / vendor

| Entity | SQL | TS writer |
|--------|-----|-----------|
| `company_membership` | OWNER on create | **EXISTS** create only; list/add **MISSING** |
| `team` / `team_membership` | ready | createTeam **EXISTS**; membership **MISSING** |
| `vendor` / `vendor_contract` | ready | **MISSING** (minimal create for expense link in K) |

---

## Setup preferences honesty (per-field)

Persisted store: `business.business_system_setup.preferences` JSONB (Path B allowlisted).

Family context tables store **title only** — preference keys are **not** column-mapped there.

| Family | Key | Classification |
|--------|-----|----------------|
| TEAM_OPERATIONS | teamName, size, workMode, country, currency, timezone, language, financialYear, taxSystem, coordination, reviewCycle, monitoring | **PERSISTED** (JSONB) |
| TEAM_OPERATIONS | spendingApproval, approvalThreshold | **PERSISTED** (JSONB; used as approval policy input in K) |
| BUSINESS_RUNWAY | businessStage, goalHorizon, multiCurrency, availableCash, monthlySpending, revenueStage, monthlyRevenue, revenueModel, warningThreshold, fundingSource | **PERSISTED** (JSONB) |
| BUSINESS_RUNWAY | availableCash / monthly* as live runway math | **NOT** live finance — display prefs only until projection writers use events |
| BUSINESS_OPERATIONS | coreOps, scope, model, cadence, monthlyBudget, allocationMethod, monitoringStyle, approvalModel, approvalAlarm | **PERSISTED** (JSONB) |
| Any key outside catalog (Path B) | — | rejected |
| Path A activate without allowlist | — | **REFACTOR** to same allowlist as Path B |
| Family context objective columns | title only | **PERSISTED** partial |

Do **not** claim “setup fully persisted” beyond JSONB + title.

---

## Backend classification

| Surface | Status |
|---------|--------|
| Live `router.ts` | sole mount — companies list/create, locations list/create, `GET /business/moments` |
| Product `router-product.ts` | NON-RUNTIME — get/patch company, patch location, teams, setups, facets |
| `POST /moments` BUSINESS + businessSetup | **REUSE** live |
| Business expense / revenue / invoice | **MISSING** |
| Approvals | **MISSING** |
| Membership list/add | **MISSING** |
| Facet GET live + real payload | **MISSING** |
| Projection writers | **MISSING** |

---

## Android / iOS classification

| Surface | Android | iOS |
|---------|---------|-----|
| Empty tabs | **REUSE** | **REUSE** |
| Company setup | **REUSE** | **REUSE** |
| Create + setup wizard | files **EXIST**, AppShell/**submit stub** | **REUSE** live |
| Ready populated tabs | **MISSING** | **MISSING** |
| Quick Add / Finance | **MISSING** | **MISSING** |
| Slice repository / facet clients | **MISSING** | **MISSING** |
| Theme BUSINESS `#818CF8` | **REUSE** shell | **REUSE** |

---

## Isolation matrix (test gate)

| Actor | Company | Moment | Must not see |
|-------|---------|--------|--------------|
| U1 OWNER | C1 | M1 (Runway) | C2 finance/team/activity/pulse |
| U2 MEMBER | C1 | M1 | approve if capability denies; C2 anything |
| U1 | C2 | M2 | C1 Moment inventory/caches under C2 chrome |
| Any | C1 | M_A | M_B expenses/activity within C1 |

Entities to assert: expenses, revenue, invoices, vendors, work/actions, Pulse, Activity, team, approvals, Moment inventory, cache keys, optimistic updates.

---

## Mandatory S4 PASS vs allowed gaps

**Must PASS:** Company create/select; Moment create ×3; Android+iOS populated shell; expense + PURCHASE category; revenue; invoice create/track (server totals); threshold approval two-stage; membership/role enforcement; Pulse/Activity after writes; Company/Moment isolation; scoped refresh; no fake runway/finance; no production mocks.

**May classify:** Vendor Ops Moment, multi-location dashboard, advanced tax/GST, FX, Life/Memory secondary richness, sophisticated team workflows, iOS `BLOCKED_ENVIRONMENT`.

---

## Effective implementation order

```text
A → B → C → D → E → F → G → H → I → J → L1 → K → L2 → M → N → O → STOP
```

---

## Non-goals

S5–S9, V030, inventing FX/tax, client-authoritative money, Pulse-as-calculator, remounting product router, Group equal-split on Business, finance/approval endpoints before this audit, Personal/Group regression.
