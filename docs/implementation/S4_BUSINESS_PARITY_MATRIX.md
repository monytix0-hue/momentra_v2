# S4 Business Parity Matrix

**Date:** 2026-08-30  
**Figma:** [`649:20260`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=649-20260)  
**Ops join:** [`BUSINESS_OPS_THREE_LAYER_JOIN.md`](./BUSINESS_OPS_THREE_LAYER_JOIN.md)  
**Statuses:** PASS | PARTIAL | REUSE | API_GAP | DEFERRED | FIGMA_GAP | BLOCKED_ENVIRONMENT

| Surface | Figma | Backend | Android | iOS | Status |
|---------|-------|---------|---------|-----|--------|
| Empty tabs | `657:*` | bootstrap | REUSE | REUSE | **PASS** |
| Company setup | `695:4455` | live companies/locations/members | REUSE + members API | REUSE | **PASS** |
| Create Moment ×3 | `658:9451`, setups | `POST /moments` + businessSetup | wired | wired | **PASS** |
| Populated Pulse | Runway/Ops/Team | facet + finance + Ops `operations` extras | Runway/Ops/TeamOps packs | same | **PASS** |
| Populated Moments | family screens | activity keyset | Ops/TeamOps/Runway | same | **PASS** |
| Life | family life / company `695:9782` | enriched life facet + trends/scores | CompanyLife pack | same | **PASS** (full API parity) |
| Memory | memory v2 / Ops `696:9450` | honest empty + list | OpsMemory / TeamOps / Runway | same | **PARTIAL** / EMPTY OK |
| Activity / Work | sheets | activity keyset | via slice | via slice | **PASS** core |
| Quick Adds | Action Centers | fail-closed ActionRegistry | hub + Ops sheets | hub + Ops sheets | **PASS** Ops |
| Expense / PURCHASE | `700:9711` / `697:9425` | business-expenses | sheet | sheet | **PASS** |
| Revenue | `700:9639` | revenues | API wired | API wired | **PASS** |
| Invoice | `700:10078` | invoices + server totals | API wired | API wired | **PASS** |
| Approvals decide | threshold | DRAFT→PENDING→POSTED/VOIDED | decide API | decide API | **PASS** |
| Approval request create | `697:9554` | `POST …/approval-requests` | Ops sheet | Ops sheet | **PASS** |
| Vendor create/patch/contract | `697:9490` | company vendors + contracts | Ops sheet | Ops sheet | **PASS** |
| Issue / Improvement / SLA | `697:9619`…`9804` | V005 + V051 writers | Ops sheets | Ops sheets | **PASS** |
| General Update / Memory | `697:9870` / `9934` | `business-updates` (not Group) | Ops sheets | Ops sheets | **PASS** |
| Membership | team launch | members list/add | API | API | **PASS** |
| Company switch isolation | — | C1/C2 tests | atomic selectCompany | atomic switch | **PASS** |
| Theme BUSINESS Ops | `#818CF8` | — | indigo Ops pack | indigo Ops pack | **PASS** |
| Vendor Ops Moment | `1124:*` | — | — | — | **DEFERRED** |
| Multi-location dashboard | `692:33733` | locations CRUD only | — | — | **DEFERRED** |
| Ops AI intelligence | Pulse/Memory | DEFERRED sectionQuality | honest empty | honest empty | **DEFERRED** |
| Advanced tax/GST | — | tax_amount passthrough | — | — | **API_GAP** / NOT invent |
| FX | — | none | — | — | **NOT_REQUIRED** |
| iOS device runtime | — | — | — | — | **BLOCKED_ENVIRONMENT** (Windows) |

---

## Isolation (verified in `business-s4-finance.test.ts` + `business-ops-three-layer-join.test.ts`)

| Case | Result |
|------|--------|
| C1 expense invisible on C2 finance | **PASS** |
| Non-member pulse 403 | **PASS** |
| MEMBER cannot approve; OWNER can | **PASS** |
| Ops spend/vendor/issue/SLA → pulse extras + activity | **PASS** |
