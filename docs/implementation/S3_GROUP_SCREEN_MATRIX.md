# S3 Group Screen Matrix

**Date:** 2026-08-26  
**Figma section:** [`575:7980`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=575-7980)  
**Statuses:** PASS | REUSE | REFACTOR | MISSING | REAL_DATA | EMPTY_SUPPORTED | API_GAP | SCHEMA_GAP | FIGMA_GAP | DEFERRED | NOT_REQUIRED | BLOCKED_ENVIRONMENT

---

## Empty (`575:8552`)

| Screen | Figma | Android | iOS | API | SQL | Status |
|--------|-------|---------|-----|-----|-----|--------|
| Moments empty | `575:8553` | `GroupMomentsEmptyContent.kt` | `GroupMomentsEmptyView.swift` | bootstrap | — | **REUSE** |
| Life empty | `575:8660` | `GroupLifeEmptyContent.kt` | `GroupLifeEmptyView.swift` | — | — | **REUSE** |
| Memory empty | `575:8838` | `GroupMemoryEmptyContent.kt` | `GroupMemoryEmptyView.swift` | — | — | **REUSE** |
| Create chooser | `575:8894` | `GroupCreateMomentContent.kt` | `GroupCreateMomentView.swift` | — | — | **REUSE** |
| Pulse empty / story | `575:8967` etc | `GroupPulseEmptyContent.kt` | `GroupPulseEmptyView.swift` | — | — | **REUSE** |
| Goal / Community cards | Coming Soon | GeMomentTypeGrid | GroupEmptyMomentTypeGrid | — | — | **NOT_REQUIRED** |

---

## Setup — Shared Experience (`575:13516`)

| Screen | Figma | Android | iOS | API | Status |
|--------|-------|---------|-----|-----|--------|
| Shared Experience / Trip | `575:9761` | `GroupExperienceSetupContent.kt` | `GroupExperienceSetupView.swift` | `POST /moments` GROUP | **REUSE** |
| Wedding | `575:9270` | same | same | same | **REUSE** |
| House Party | `575:9435` | same | same | same | **REUSE** |
| Office Outing | `575:9590` | same | same | same | **REUSE** |
| Budget/split prefs | in setup | local cycle UI | local | — | **API_GAP** / LOCAL_ONLY |

---

## Setup — Shared Purchase (`575:9918`)

| Screen | Figma | Catalog code | Status |
|--------|-------|--------------|--------|
| Shared Purchase | `575:11376` | GROUP_PURCHASE | **REUSE** |
| Gift Pool | `575:11214` | GIFT_POOL | **REUSE** |
| Shared Asset | `575:11538` | SHARED_ASSET | **REUSE** |
| Custom Purchase | `575:11700` | CUSTOM→COMMUNITY_PURCHASE | **REUSE** |

Android: `GroupPurchaseSetupContent` / `GroupSectionSetupContent.kt`. iOS: `GroupPurchaseSetupView.swift`.

---

## Setup — Shared Living (`575:10566`)

| Screen | Figma | Catalog code | Status |
|--------|-------|--------------|--------|
| Shared Living | `634:13345` | CO_LIVING / SHARED_LIVING | **REUSE** |
| Flatmates | `629:8535` | FLATMATES | **REUSE** |
| Family Household | `634:13170` | FAMILY_HOUSEHOLD | **REUSE** |
| Custom Living | `634:13530` | CUSTOM→COMMUNITY_LIVING | **REUSE** |

---

## Populated Experience (representative)

| Screen | Figma | Native | API | Status |
|--------|-------|--------|-----|--------|
| Pulse | `575:14165` / `575:14939` | **MISSING** (later phases) | facet empty / writers | **MISSING** |
| Moments | `575:14327` | **MISSING** | — | **MISSING** |
| Memory | `575:14470` | **MISSING** | — | **MISSING** |
| Life (populated) | under empty Life + gap | **MISSING** | — | **MISSING** / section honesty |
| Action Center | `575:14655` | **MISSING** | — | **MISSING** |

Purchase/Living populated: expand in S3-E from Figma clusters; same **MISSING** native pattern.

---

## Finance / Quick Add sheets

| Screen | Figma | API | SQL | Status |
|--------|-------|-----|-----|--------|
| Add Expense | `581:12789` (Split Between, Split Type chips) | Group expense **MISSING** | `expense` + `group_expense_context` + `expense_share` | **MISSING** |
| Equal split | Split Type | server calc | `expense_share` | **MUST PASS** |
| Percentage / Exact / Shares | Split Type | server or API_GAP | `expense_share` | PASS or **API_GAP** |
| Add Contribution | `581:13613` | product-only | `finance.contribution` | **MISSING** mount |
| Obligations (read) | Pulse/finance UI | — | `participant_obligation` | **MISSING** |
| Settlements | — | none | `settlement` + allocation | **MISSING** or **API_GAP** |
| Invite People | `581:13699` | mint live; redeem unwired | `moment_invite*` | **REFACTOR** |

Collab sheets (Poll, Booking, Vendor, Resident, Rules, …): **DEFERRED** unless mandatory PASS requires.

---

## Participants / isolation

| Concern | Status |
|---------|--------|
| Organizer on create | **REUSE** (`ORGANIZER`) |
| Redeem → PARTICIPANT | API **REUSE**; UI **MISSING** |
| `participant_id` on finance | Required (K1 before J) |
| U2 ↛ Group B finance/pulse/activity | **MISSING** test gate |
| Cache keys include momentId | **MISSING** verify |

---

## Backend route plan (post-audit)

| Method | Path | S3 action |
|--------|------|-----------|
| GET | `/group/moments/:id/pulse\|life\|memory\|finance\|activity` | Mount live; real projection or EMPTY |
| POST | `/moments/:id/participants` | Mount for K1 if needed |
| POST | `/moments/:id/group-expenses` or domain-aware expenses | **New Group command** (not Personal) |
| POST | `/moments/:id/contributions` | Mount |
| POST | `/moments/:id/settlements` | Mount or API_GAP |
| POST | `/group/invites/:code/redeem` | Already live — wire clients |

---

## Split acceptance matrix (tracking)

| Case | S3 target |
|------|-----------|
| Equal | PASS |
| Percentage | PASS or API_GAP |
| Exact | PASS or API_GAP |
| Shares | PASS or API_GAP |
| Single participant | validation defined |
| Payer excluded | validation defined |
| Rounding remainder | deterministic server rule |
| Zero / negative / mismatch / non-member / cross-Moment | reject |

---

## Multi-currency

| Question | Finding |
|----------|---------|
| baseCurrency on Group moment | **Absent** V001–V029 |
| FX columns | **Absent** |
| S3 rule | Per-row `currency_code` only; no FX/netting; settle same currency |

---

## S3-A exit

- [x] `S3_GROUP_AUDIT.md`
- [x] This matrix
- [x] Finance SQL relationships proven before endpoint design
- [x] S3-B→N executed — see `S3_IMPLEMENTATION_REPORT.md` + `S3_GROUP_PARITY_MATRIX.md`
- **STOP** before S4
