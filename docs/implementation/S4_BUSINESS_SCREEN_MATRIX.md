# S4 Business Screen Matrix

**Date:** 2026-08-30  
**Figma section:** [`649:20260`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=649-20260)  
**Statuses:** PASS | REUSE | REFACTOR | MISSING | REAL_DATA | EMPTY_SUPPORTED | API_GAP | SCHEMA_GAP | FIGMA_GAP | DEFERRED | NOT_REQUIRED | LOCAL_ONLY | PERSISTED | BLOCKED_ENVIRONMENT
**Ops join:** [`BUSINESS_OPS_THREE_LAYER_JOIN.md`](./BUSINESS_OPS_THREE_LAYER_JOIN.md)

---

## Empty (`657:9979`)

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| Pulse empty | `657:9980` | `BusinessPulseEmptyContent.kt` | `BusinessPulseEmptyView.swift` | bootstrap | **REUSE** |
| Moments empty | `657:10043` | `BusinessMomentsEmptyContent.kt` | `BusinessMomentsEmptyView.swift` | — | **REUSE** |
| Create empty | `657:10100` | `BusinessCreateEmptyContent.kt` | `BusinessCreateEmptyView.swift` | — | **REUSE** |
| Life empty | `657:10151` | `BusinessLifeEmptyContent.kt` | `BusinessLifeEmptyView.swift` | — | **REUSE** |
| Memory empty | `657:10208` | `BusinessMemoryEmptyContent.kt` | `BusinessMemoryEmptyView.swift` | — | **REUSE** |

---

## Create chooser

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| Create Moment | `658:9451` | `BusinessCreateMomentContent.kt` (unwired) | `BusinessCreateMomentView.swift` | `POST /moments` BUSINESS | Android **REFACTOR**; iOS **REUSE** |
| Memory chooser | `658:9573` | present | present | — | **DEFERRED** content |

---

## Company Setup (`695:4455`)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Welcome | `692:38403` | `CompanySetupContent.kt` | `CompanySetupFlowView.swift` | — | — | **REUSE** |
| Company identity | `692:38453` | same | same | `POST /companies` live | `business.company` | **REUSE** |
| Locations | `692:38549` | same | same | `POST …/locations` live | `company_location` | **REUSE** |
| Team launch | `692:38635` | same | same | teams product-only | `business.team` | **REFACTOR** promote |
| Company settings | `702:9524` | partial | partial | get/patch product-only | company | **REFACTOR** promote |
| Default company | CompanySwitcher | shell | shell | bootstrap companies | membership | **REUSE** / harden restore |

---

## Company Life (`695:9782`)

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| Company Life | `695:9782` | `CompanyLifeActiveContent.kt` | `BusinessLifeActiveView.swift` | enriched `GET …/life` + Wave 3 share/report | **PASS** |

---

## Setup — Team Operations (`692:34735`)

| Screen | Figma | Catalog | Prefs | Status |
|--------|-------|---------|-------|--------|
| Setup single | `692:34736` | `TEAM_OPERATIONS` | JSONB **PERSISTED** (allowlisted) | Android wire **REFACTOR**; iOS **REUSE** |
| Pulse v2 | `692:34967` | — | — | **MISSING** populated |
| Moments v2 | `692:35199` | — | — | **MISSING** |
| Life | `708:9524` | — | — | **REUSE** company Life `695:9782` |
| Memory v2 | `692:35410` | — | — | **MISSING** / honest empty OK |
| Quick-add hub | `649:26162` | ActionRegistry | — | **MISSING** |
| Sheets (update, decision, blocker, …) | `692:35623`… | — | — | core subset **MISSING**; rest **DEFERRED** |

---

## Setup — Business Runway (`692:36689`)

| Screen | Figma | Catalog | Status |
|--------|-------|---------|--------|
| Setup | `692:36690` | `BUSINESS_RUNWAY` | Android **REFACTOR**; iOS **REUSE** |
| Pulse | `692:36956` | — | **MISSING** |
| Moments | `692:37078` | — | **MISSING** |
| Life | `700:10521` | — | **REUSE** company Life `695:9782` |
| Memory | `698:9970` | — | **MISSING** / honesty |
| Action Center | `692:44440` | — | **MISSING** |
| Log revenue | `700:9639` | revenue API | **MISSING** |
| Log expense | `700:9711` | business expense | **MISSING** |
| Invoice track | `700:10078` | invoice API | **MISSING** |
| Tax / investor / forecast sheets | `700:9789`… | — | **DEFERRED** / **API_GAP** tax invent |

---

## Setup — Business Operations (`692:37187`)

| Screen | Figma | Catalog | Status |
|--------|-------|---------|--------|
| Setup | `692:37188` | `BUSINESS_OPERATIONS` | Android **REFACTOR**; iOS **REUSE** |
| Pulse | `692:43993` | Ops pack + pulse `operations` extras | **PASS** / EMPTY_SUPPORTED |
| Moments | `692:44116` | activity timeline | **PASS** / EMPTY_SUPPORTED |
| Life | `700:11150` | — | **REUSE** company Life `695:9782` (ops join out) |
| Memory | `696:9450` | Ops Memory fidelity | **PASS** / EMPTY_SUPPORTED |
| Action Center | `692:37745` | hub + Ops sheets | **PASS** |
| Log Spend | `697:9425` | business-expenses | **PASS** |
| Update Vendor | `697:9490` | vendor PATCH/contracts | **PASS** |
| Request Approval | `697:9554` | approval-requests | **PASS** |
| Report Issue | `697:9619` | issues | **PASS** |
| Log Improvement | `697:9681` | V051 improvements | **PASS** |
| Budget Review | `697:9747` | finance read-only | **PASS** |
| SLA Check | `697:9804` | sla definition/check | **PASS** |
| General Update | `697:9870` | business-updates | **PASS** |
| Save to Memory | `697:9934` | business-scoped (not Group) | **PASS** |

Authority: [`BUSINESS_OPS_FIELD_MATRIX.csv`](./BUSINESS_OPS_FIELD_MATRIX.csv) / [`BUSINESS_OPS_THREE_LAYER_JOIN.md`](./BUSINESS_OPS_THREE_LAYER_JOIN.md).

---

## Multi-location / Vendor Ops

| Screen | Figma | Status |
|--------|-------|--------|
| Multi-location suite | `692:33733` | **DEFERRED** |
| Vendor Operations canvas | `1124:*` | **DEFERRED** (not create-family) |

---

## Finance / approvals (post L1)

| Concern | Figma | API | SQL | Status |
|---------|-------|-----|-----|--------|
| Business expense | `700:9711` | **MISSING** | `expense` + `business_expense_context` | **MISSING** |
| PURCHASE category | Purchase label | same expense path | `category_code=PURCHASE` | taxonomy locked |
| Revenue | `700:9639` | **MISSING** | `finance.revenue` | **MISSING** |
| Invoice create/track | `700:10078` | **MISSING** | `invoice` + `invoice_line` | **MISSING**; tax = server `tax_amount` or 0 |
| Vendor link | — | minimal | `business.vendor` | link-or-skip |
| Approval threshold | setup prefs | **MISSING** | `approval_request` + expense DRAFT→POSTED | **MISSING** |
| FX | — | — | none | **NOT_REQUIRED** |

---

## Isolation / cache

| Concern | Status |
|---------|--------|
| C1/C2 Moment inventory | **MISSING** test gate |
| Atomic company switch clears Moment cache | **MISSING** |
| Cache keys include companyId+momentId | **MISSING** verify |
| Optimistic updates scoped | **MISSING** |

---

## Backend route plan (post-audit)

**Promote to live `router.ts`:**

- `GET|PATCH /companies/:companyId`
- `PATCH /companies/:companyId/locations/:locationId`
- `GET|POST /companies/:companyId/teams`
- `GET /business/setups`, `POST /business/setups/:familyCode/activate` (allowlist prefs)
- `GET /business/moments/:momentId/{pulse,life,memory,finance,actions}`

**Build after L1:**

- Membership list/add under company
- `POST …/business-expenses` (DRAFT|POSTED + context)
- `POST …/revenues`, `POST …/invoices`
- Approval submit already embedded in expense; `POST …/approvals/:id/decide`
- Minimal vendor create for link
- Projection upserts on write path

**Do not** mount duplicate product-router handlers as a second live tree.

---

## Setup prefs field matrix (summary)

| Classification | Keys |
|----------------|------|
| **PERSISTED** (JSONB allowlist) | All catalog keys for 3 families (see audit) |
| **PERSISTED** partial | Family context `title` / objective only |
| **NOT** live finance | Runway cash/revenue prefs until events update projections |
| **API_GAP** | Path A activate without allowlist until REFACTOR |

---

## Theme

| Token | Value | Status |
|-------|-------|--------|
| Context BUSINESS | `#818CF8` / `#A5B4FC` | **REUSE** |
| Team Ops / Runway / Ops MomentTheme | matrix + FIGMA_GAP notes | refine in S4-N |

---

## STOP

After O: **S5 LIFE360 — NOT STARTED**. No V030.
