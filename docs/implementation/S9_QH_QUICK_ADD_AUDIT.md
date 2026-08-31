# S9-QH Quick Add Audit

**Gate:** S9-QH (implementation gate before S9-QA Master Certification)  
**Figma file:** [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra) (`TzLvwVwlPbeVB8ug1zB3GM`)  
**Generated:** 2026-08-29 (QH-REFRESH post V046–V047)  
**Scope:** 19/19 Moment variants — Personal 4 + Group 12 + Business 3  
**See also:** [`S9_QH_REFRESH.md`](S9_QH_REFRESH.md)

## Four clarity outcomes

| # | Question | This document section |
|---|----------|----------------------|
| 1 | What Figma designed | Per-variant hub + destination screen inventory below |
| 2 | What each Moment should expose | Tile sets per family/subtype |
| 3 | Where every tile navigates | Destination screen mapping |
| 4 | Whether destination works E2E | See [`QUICK_ADD_IMPLEMENTATION_MATRIX.md`](QUICK_ADD_IMPLEMENTATION_MATRIX.md) |

**Rule:** Audit **destination screens**, not hub tiles alone. A linked tile with an incomplete sheet is a QH gap.

**Known gaps — do not expand product scope to green the matrix:**

- **Group Settle (12×):** `PASS_CANDIDATE` (was `API_GAP`) — **backend LIVE (V047)**; client hub still `SETTLEMENT_DEFERRED` until QH wiring; lifecycle QA required
- **Business Revenue/Invoice on B01/B03 (4×):** `CAPABILITY_GAP` — V019 maps only to `BUSINESS_RUNWAY`
- **Personal Transfer/Savings/Reflect (3×):** Transfer/Savings live money sheets; Reflect `DEFERRED`

**Precision (not gaps):**

- **P2 Future / P3 Lifestyle / P4 Relationships:** PX family precision **LIVE** (V046 + family modules) — no longer heuristic
- **P1 Life Ops:** V042–V045 precision unchanged

---

## Reuse classification vocabulary

| Class | Meaning |
|-------|---------|
| `FIGMA_UNIQUE` | Subtype needs distinct hub chrome or tile set |
| `FAMILY_UI_REUSED` | Family hub equivalent; per-subtype theme/navigation row still required |
| `FIGMA_STALE` | Native already matches design intent |
| `API_GAP` | Tile visible but backend not mapped — disabled |
| `CAPABILITY_GAP` | Capability not on moment type — disabled |
| `DEFERRED` | Product deferred — disabled |

---

## Personal (P1–P4)

### P1 — Life Operations (`LIFE_RHYTHM`)

| Item | Figma node | Native destination | Reuse |
|------|-----------|-------------------|-------|
| Hub | `353:6809` setup; hub nodes `353:8893`, `353:11361`… | `PersonalQuickAddHub` | EXACT |
| Expense | expense sheet | `PersonalExpenseSheet` | EXACT |
| Recovery | life-ops sheet | `PersonalLifeOpsQuickAddSheet(RECOVERY)` | EXACT |
| Mood | life-ops sheet | `PersonalLifeOpsQuickAddSheet(MOOD)` | EXACT |
| Attention | life-ops sheet | `PersonalLifeOpsQuickAddSheet(ATTENTION)` | EXACT |
| Adjust | life-ops sheet | `PersonalLifeOpsQuickAddSheet(ADJUST)` | EXACT |
| Transfer | — | — | DEFERRED |
| Savings | — | — | DEFERRED |
| Reflect | — | — | DEFERRED |

Theme: `#7C5CFC` per [`MOMENTRA_THEME_MATRIX.md`](../design/MOMENTRA_THEME_MATRIX.md)

### P2 — Future Building (`FUTURE_GOAL`)

| Item | Figma | Native destination | Reuse |
|------|-------|-------------------|-------|
| Hub | `353:6905` | `PersonalQuickAddHub` (future family) | EXACT |
| Milestone / Opportunity / Pivot / Progress / Learning | future sheets | `PersonalFutureQuickAddSheet` → `POST .../future-items` (**PX-1 LIVE**) | EXACT |
| Expense | expense sheet | `PersonalExpenseSheet` | EXACT |
| Precision reads | — | `GET .../future-axis-snapshot` (+ runtime/inventory/journey) | EXACT |

Theme: `#10B981`

### P3 — Lifestyle (`LIFESTYLE`)

| Item | Figma | Native destination | Reuse |
|------|-------|-------------------|-------|
| Hub | `353:7075` | `PersonalQuickAddHub` (lifestyle family) | EXACT |
| Experience / Wellbeing / Discovery / Expression / Adjust | lifestyle sheets | `PersonalLifestyleQuickAddSheet` → `POST .../lifestyle-activities` (**PX-2 LIVE**) | EXACT |
| Expense | expense sheet | `PersonalExpenseSheet` | EXACT |
| Precision reads | — | `GET .../lifestyle-vitality-snapshot` (+ runtime/inventory/journey) | EXACT |

Theme: `#0EA5A4`

### P4 — Relationships (`RELATIONSHIP_CONNECTION`)

| Item | Figma | Native destination | Reuse |
|------|-------|-------------------|-------|
| Hub | `1006:8274` | `PersonalQuickAddHub` (relationships) | EXACT |
| Connection / Support / Shared Exp / Investment / Adjust | relationship sheets | `PersonalRelationshipsQuickAddSheet` → `POST .../relationship-activities` (**PX-3 LIVE**) | EXACT |
| Precision reads | — | `GET .../relationships-bond-snapshot` (+ runtime/connections/journey) | EXACT |

Theme: `#E91E63` — no Expense tile in hub.

---

## Group (G01–G12)

**Hub strategy:** Single `GroupQuickAddHub` / `GroupQuickAddHubView` with **per-subtype MomentTheme** from `momentTypeCode`.

### Shared Experience family (G01–G04)

| ID | Label | Type code | Figma setup | Theme primary |
|----|-------|-----------|-------------|---------------|
| G01 | Trip | TRIP | `575:9761` | `#E8744F` |
| G02 | Wedding | WEDDING | `575:9761` | `#EC4899` |
| G03 | House Party | HOUSE_PARTY | `575:9761` | `#3B82F6` |
| G04 | Office Outing | OFFICE_OUTING | `575:9761` | `#14B8A6` |

### Shared Purchase family (G05–G08)

| ID | Label | Type code | Figma setup |
|----|-------|-----------|-------------|
| G05 | Gift Pool | GIFT_POOL | `575:9919` |
| G06 | Group Purchase | GROUP_PURCHASE | `575:9919` |
| G07 | Shared Asset | SHARED_ASSET | `575:9919` |
| G08 | Custom Purchase | COMMUNITY_PURCHASE | `575:9919` |

### Shared Living family (G09–G12)

| ID | Label | Type code | Figma setup |
|----|-------|-----------|-------------|
| G09 | Flatmates | FLATMATES | `634:13345` |
| G10 | Family Household | FAMILY_HOUSEHOLD | `634:13345` |
| G11 | Co-living | CO_LIVING | `634:13345` |
| G12 | Custom Living | COMMUNITY_LIVING | `634:13345` |

### Group tile → destination (all 12 variants)

| Tile | Capability | Destination screen | Notes |
|------|------------|-------------------|-------|
| Expense | EXPENSE_CREATE | `GroupExpenseSheet` | Wired |
| Contribute | CONTRIBUTION_RECORD | `GroupContributionSheet` | Wired |
| Settle | SETTLEMENT_RECORD | — | API_GAP — disabled |
| People | PARTICIPANT_MANAGE | `GroupParticipantsSheet` | Read-only list + invite path deferred |

---

## Business (B01–B03)

**Context chain:** BUSINESS → COMPANY → MOMENT → QUICK ADD HUB

| ID | Label | Figma hub | Type code |
|----|-------|-----------|-----------|
| B01 | Team Operations | `692:34735` | TEAM_OPERATIONS |
| B02 | Business Runway | `692:36689` | BUSINESS_RUNWAY |
| B03 | Business Operations | `692:37187` | BUSINESS_OPERATIONS |

### Business tile → destination

| Tile | Capability | B02 Runway | B01/B03 | Destination |
|------|------------|------------|---------|-------------|
| Expense | EXPENSE_CREATE | ✓ | ✓ | `BusinessExpenseSheet` |
| Revenue | REVENUE_RECORD | ✓ | CAPABILITY_GAP | `BusinessRevenueSheet` |
| Invoice | INVOICE_CREATE | ✓ | CAPABILITY_GAP | `BusinessInvoiceSheet` |
| People | MEMBER_MANAGE | ✓ | ✓ | `BusinessMembersSheet` (company-scoped) |

---

## Baseline gaps closed in S9-QH

1. Android Business hub mounted on CREATE tab (was unmounted)
2. Android Revenue + Invoice sheets added
3. Android Group People tile → `GroupParticipantsSheet` (was miswired to create-moment)
4. Android Business People → `BusinessMembersSheet`
5. iOS hubs receive bootstrap `capabilityCodes`
6. Hubs use `MomentThemes.resolve()` instead of hardcoded accent colors
7. Implementation matrix distinguishes HUB_TILE / SCREEN / SUBMIT / REFRESH

---

## Related artifacts

- [`QUICK_ADD_IMPLEMENTATION_MATRIX.md`](QUICK_ADD_IMPLEMENTATION_MATRIX.md) — screen-level wiring status
- [`S9_QH_NAVIGATION_MATRIX.md`](S9_QH_NAVIGATION_MATRIX.md) — launcher → hub → sheet → refresh
- [`S9_QH_IMPLEMENTATION_REPORT.md`](S9_QH_IMPLEMENTATION_REPORT.md) — gate signoff
- [`docs/qa/QUICK_ADD_COVERAGE_MATRIX.md`](../qa/QUICK_ADD_COVERAGE_MATRIX.md) — classification catalog
