# Golden Scenarios — Pulse / Moments / Life / Memory

Functional validation catalog: **Input → Moments → Pulse → Life → Memory**.

Surface doctrine: [MOMENTRA_SURFACE_DOCTRINE.md](./MOMENTRA_SURFACE_DOCTRINE.md).

Status:

- `automated` — covered by `backend/typescript/tests/golden-scenarios.test.ts`
- `manual` — beta / follow-on automation

---

## Personal

| id | Input | Expected Moments | Expected Pulse | Expected Life | Expected Memory | Status |
|----|-------|------------------|----------------|---------------|-----------------|--------|
| P-01 | Dinner ₹2500 Self (FOOD/DINING_OUT) | LO ACTIVE; Lifestyle ACTIVE; Rel INACTIVE; Future INACTIVE | LO spend INR ≥ 2500; Lifestyle axes may move; Rel axes not required; financial ledger one row ₹2500 | Overall score `null`; Lifestyle/LO chips may reflect axes | Unchanged (no auto Memory) | automated |
| P-02 | Dinner with wife ₹1500 (SPOUSE) | LO + Rel + Lifestyle ACTIVE; Future INACTIVE | Rel + Lifestyle axes move; LO spend ≥ 1500; wellbeing recomputed; financial truth ₹1500 once | Overall `null`; Rel/Lifestyle chips may show axes | Unchanged | automated |
| P-03 | Dinner with family ₹3000 (FAMILY) | LO + Rel + Lifestyle ACTIVE | Rel axes move; spend once | Overall `null` | Unchanged | manual |
| P-04 | Dinner with friends ₹2000 (FRIEND) | LO + Rel + Lifestyle ACTIVE | Rel axes move; spend once | Overall `null` | Unchanged | manual |
| P-05 | Self groceries ₹3000 (GROCERIES) | LO ACTIVE; Lifestyle INACTIVE; Rel INACTIVE | LO spend; no Lifestyle eligibility | Overall `null` | Unchanged | automated |
| P-06 | Bills / utilities (BILLS) | LO ACTIVE; Lifestyle INACTIVE | LO spend only | Overall `null` | Unchanged | manual |
| P-07 | Edit SELF→FRIEND on dining expense | Rel toggles INACTIVE→ACTIVE; Lifestyle stays ACTIVE | Rel axes refresh | Overall `null` | Unchanged | manual |
| P-08 | Edit dining→BILLS category | Lifestyle → INACTIVE; LO stays | Lifestyle axes not required to drop immediately | Overall `null` | Unchanged | manual |
| P-09 | Four dinners (FAMILY+FRIEND+SPOUSE+SELF) | Dimensional routing per shared code | Ledger SUM = 9000; no double-count | Overall `null` | Unchanged | automated |
| P-10 | Expense posted via Future moment | Canonical home LO; Future dim INACTIVE | Spend on LO | Overall `null` | Unchanged | manual |
| P-11 | Void expense | All dim contributions INACTIVE | Spend reverse / inactive | Overall `null` | Unchanged | manual |
| P-12 | Idempotent expense retry | Single expense; 4 contribution rows | No duplicate amount | Overall `null` | Unchanged | manual |
| P-13 | LO observation (RECOVERY) | LO Moment manages observation | `recovery_score` set; Future payload axes untouched | Area chip may show recovery; overall `null` | Unchanged | manual |
| P-14 | Future item create | Future Moment | Payload vision/growth/… only; recovery/rhythm columns not overwritten by Future | Future chip may show; overall `null` | Unchanged | manual |
| P-15 | Relationship activity | Rel Moment | Bond/trust/… axes; wellbeing blend includes Rel | Rel chip | Unchanged | manual |
| P-16 | Lifestyle activity | Lifestyle Moment | Joy/vitality/… | Lifestyle chip | Unchanged | manual |
| P-17 | Fresh user, no setups | No Moments | Pulse empty / null wellbeing | Status “No areas yet”; score `null` | Empty | automated |
| P-18 | Setups active, no events | Four Moments ACTIVE | Pulse may exist with nulls | Status Active; score `null`; chips empty/null | Empty | automated |
| P-19 | Explicit Memory create | Linked Moment optional | Pulse unchanged by Memory write | Life unchanged | Memory list includes new item | automated |
| P-20 | Expense does not create Memory | Dimensional Moments as above | Pulse as expense rules | Life score still `null` | Memory count unchanged | automated |
| P-21 | Wellbeing blend with Rel+Lifestyle expense | LO+Rel+Lifestyle | `wellbeing_score` > 0 from equal-weight families | Overall Life still `null` (not Pulse copy) | Unchanged | automated |
| P-22 | Multi-currency personal expenses | LO home | Spend map per currency; no FX merge | Overall `null` | Unchanged | manual |

---

## Group

| id | Input | Expected Moments | Expected Pulse | Expected Life | Expected Memory | Status |
|----|-------|------------------|----------------|---------------|-----------------|--------|
| G-01 | Trip created + 2 members | SHARED_EXPERIENCE / TRIP Moment ACTIVE | Participant counters; finance EMPTY until spend | Life provisional domains; may be EMPTY-ish | Empty | automated |
| G-02 | Equal-split expense ₹100 (org pays) | Same Moment | expense_total +100; contribution_total +100; positions nets | Finance-influenced domains/bars | Unchanged | automated |
| G-03 | Second expense same currency | Same Moment | Totals accumulate; contribution tracks paid | Life updates live | Unchanged | manual |
| G-04 | Multi-currency expenses | Same Moment | Separate snapshot rows per currency; no FX | Per-currency finance signals | Unchanged | manual |
| G-05 | Settlement reducing outstanding | Same Moment | Nets move; outstanding down | Life via finance snapshot | Unchanged | manual |
| G-06 | Non-member cannot read finance | — | 403 on finance GET | — | — | manual |
| G-07 | Cross-moment participant in split | Rejected 400 | No ledger change | — | — | manual |
| G-08 | Explicit Memory on trip | Moment owns Memory | Pulse counters may bump memory_count | Life does not invent Memory | List includes Memory | automated |
| G-09 | Expense does not auto-Memory | Finance Moment write | Finance Pulse only | No new Memory | Count unchanged | automated |
| G-10 | Shared purchase Moment family | PURCHASE Moment | Finance Pulse | Purchase domain signal | Unchanged | manual |
| G-11 | Shared living Moment | LIVING Moment | Finance / people | Living domain | Unchanged | manual |
| G-12 | Shared goal Moment | GOAL Moment | Progress / finance | Goal domain | Unchanged | manual |
| G-13 | Community coordination | COMMUNITY Moment | Collab counters | Community domain | Unchanged | manual |
| G-14 | Life health avg of non-null domains | Activity + finance | Pulse finance SoT | Health = avg domains | Not Memory | manual |
| G-15 | Empty Moment Life | No writes | EMPTY-ish Pulse | EMPTY domains / honest empty | Empty | manual |

---

## Business

| id | Input | Expected Moments | Expected Pulse | Expected Life | Expected Memory | Status |
|----|-------|------------------|----------------|---------------|-----------------|--------|
| B-01 | Company + BUSINESS_OPERATIONS Moment | Ops Moment ACTIVE | Pulse payload has attentionCount / financialHealthScore; **no** openRiskCount | Life enrich on GET | Empty | automated |
| B-02 | Business expense | Ops Moment | Finance totals; attention may bump | Ops signals | Unchanged | automated |
| B-03 | Open issue create | Ops Moment | openIssueCount / ops extras; still no openRiskCount in API | Watch/Action signals possible | Unchanged | manual |
| B-04 | Runway prefs + cash/burn | Runway Moment | runwayMonths + financial_health when computable | Runway signals | Unchanged | manual |
| B-05 | Team ops Moment | TEAM_OPERATIONS | Capacity / issues heuristics | Team module scores | Unchanged | manual |
| B-06 | Vendor + SLA | Ops Moment | Ops pulse extras | Vendor / SLA signals | Unchanged | manual |
| B-07 | Overdue invoice signal | Ops/Runway | Finance outstanding | Watch signal | Unchanged | manual |
| B-08 | Explicit business Memory | Moment | Projection memory list | Life not auto-history | Memory present | manual |
| B-09 | Expense ≠ Memory | Finance write | Pulse finance | — | Count unchanged | automated |
| B-10 | API never returns open_risk_count / openRiskCount | Any pulse GET | Keys absent | — | — | automated |
| B-11 | Placeholder DB open_risk_count stays 0 | After pulse refresh | Column 0 in DB; not in API | — | — | automated |
| B-12 | attention_count preserved on refresh | Refresh after bump | Attention not wiped to null | — | — | manual |
| B-13 | Budget overage ops adherence | Ops budget spend | Health/ops heuristics | Watch | Unchanged | manual |
| B-14 | Non-member denied | Stranger GET | 403 | — | — | manual |
| B-15 | Multiple Moments same company | Shared company Pulse | One company pulse row | Per-Moment Life enrich | Per-Moment Memory | manual |

---

## Automation map (first wave)

| Test id in suite | Catalog ids |
|------------------|-------------|
| P dinners + dims + spend + life null + memory | P-01, P-02, P-05, P-09, P-17, P-18, P-19, P-20, P-21 |
| Group expense + contribution + memory | G-01, G-02, G-08, G-09 |
| Business pulse field guard | B-01, B-02, B-09, B-10, B-11 |

---

## How to extend

1. Add a row above with expected surface outcomes.
2. Prefer API-level assertions (GET pulse/life/memory/finance) over UI.
3. Keep Life overall Personal score `null` until pattern intelligence ships.
4. Never assert “Open Risks: 0” as a positive user outcome.
