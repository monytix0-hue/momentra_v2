# S8 AI + Analytics Matrix

**Date:** 2026-08-26  
**Companion:** [`S8_AI_ANALYTICS_AUDIT.md`](S8_AI_ANALYTICS_AUDIT.md)  
**Statuses:** `REAL_DATA | IMPLEMENT | REUSE | API_GAP | SCHEMA_GAP | FIGMA_GAP | DEFERRED | NOT_REQUIRED`

Legend for **Class:** `DET` = deterministic analytics · `AI` = narrative/explanation (template OK) · `DEFERRED`

---

## Pipeline / platform rows

| Surface | Output | Class | Inputs | SQL ownership | FastAPI? | TS endpoint | Android/iOS | Status |
|---------|--------|-------|--------|---------------|----------|-------------|-------------|--------|
| Outbox transport | Event delivery | — | Domain events | `events.outbox_event` | No | Internal | N/A | **REUSE** |
| Analytics worker | Job execution | — | Outbox/BullMQ | `calculation_run` | Optional | Worker | N/A | **IMPLEMENT** (from stub) |
| FastAPI health | live/ready | — | Process deps | — | Self | Internal only | N/A | **IMPLEMENT** |
| TS FastAPI client | Compute call | — | AuthorizedFacts | — | Yes (optional) | Internal | N/A | **IMPLEMENT** |
| Core API ready | DB ready | — | Postgres | — | **No** | `GET /health/ready` | N/A | **REUSE** |
| Consent assert | Granted bool | — | `governance.consent` | Consent tables | No | Internal helper | Account hub | **IMPLEMENT** |
| Insight read metadata | computedAt/dataThrough/status/version | — | metric/insight rows | analytics/ai | No | Analytics GETs | Consumers | **IMPLEMENT** |
| Redis insight cache | Scoped JSON | — | user/context/company/moment | — | No | Internal | Transparent | **IMPLEMENT** |
| LLM provider | Tokens | AI | — | `inference_run` | Yes | — | — | **NOT_REQUIRED** (PASS) |
| Life360 intelligence | — | DEFERRED | — | — | No | — | Coming Soon | **DEFERRED** |
| Circle intelligence | — | DEFERRED | — | — | No | — | Coming Soon | **DEFERRED** |

---

## Personal

| Figma / surface | Required output | Class | Canonical inputs | SQL | FastAPI? | TS endpoint | Android/iOS | Status |
|-----------------|-----------------|-------|------------------|-----|----------|-------------|-------------|--------|
| Pulse money snapshot | Spend by currency, totals | DET | Expense projections / finance | Finance + pulse `widgetPayload` | No | Existing pulse GET | Pulse actives | **REAL_DATA** / **REUSE** |
| Pulse MoM / trend chips | % vs prior window | DET | Expense sums by window | `metric_current` (new codes or BUDGET_UTILIZATION) | No | `GET …/analytics/metrics` | Pulse | **IMPLEMENT** |
| Pulse AI Insights card | Coming Soon / last insight | AI optional | DET facts | `deterministic_insight` / `ai_insight` | Optional narrative | Analytics insights GET | Pulse stub → wire | **FIGMA_GAP** → show last-known or honest empty |
| Life score / areaScores | Seeded today | DET later | Activity scores | Personal pulse scores | No | Life GET | Life actives | **API_GAP** → prefer REAL activity scores; no fake AI |
| Life `aiInsights` list | Titles/bodies + metadata | DET→AI | Metrics + templates | `deterministic_insight` then optional `ai_insight` | Optional | Life GET enriched | Life actives | **IMPLEMENT** |
| LifeOps recovery/mood/rhythm | Scores | DET | Activity / wellbeing fields | Pulse DTO + optional `RECOVERY_SCORE` etc. | No | Pulse GET | Pulse | **REUSE** scores; metric persist **IMPLEMENT** |
| Rel Pulse intelligence | Copy | — | — | — | No | — | Stub | **FIGMA_GAP** / **DEFERRED** |
| Moments/Memory AI Insights | Coming Soon | — | — | — | No | — | Stub | **FIGMA_GAP** / **DEFERRED** |

---

## Group

| Figma / surface | Required output | Class | Inputs | SQL | FastAPI? | TS | Clients | Status |
|-----------------|-----------------|-------|--------|-----|----------|----|---------|--------|
| Pulse finance totals | Expense/contribution/outstanding | DET | Group finance projection | Finance tables | No | Group pulse GET | Group Pulse | **REAL_DATA** / **REUSE** |
| Contribution completion % | % | DET | Positions / budgets | `GROUP_CONTRIBUTION_COMPLETION` → `metric_current` | No | Analytics metrics | Group Pulse | **IMPLEMENT** |
| Participation rate | % | DET | Member activity | `PARTICIPATION_RATE` | No | Analytics metrics | Optional | **IMPLEMENT** if evidence ≥ minimum |
| Narrative insight | Template over DET | AI optional | Metrics | `deterministic_insight` / `ai_insight` | Optional | Insights GET | Group | **IMPLEMENT** template |

---

## Business

| Figma / surface | Required output | Class | Inputs | SQL | FastAPI? | TS | Clients | Status |
|-----------------|-----------------|-------|--------|-----|----------|----|---------|--------|
| Pulse expense/revenue/invoice | Totals | DET | Business finance | Finance | No | Business pulse | Business Pulse | **REAL_DATA** / **REUSE** |
| Burn rate | Currency/period | DET | Expenses window | `BUSINESS_BURN_RATE` | No | Metrics GET | Business | **IMPLEMENT** |
| Runway months | Number | DET | Burn + runway context | `BUSINESS_RUNWAY_MONTHS` → `metric_current` → pulse `runwayMonths` | No | Pulse + metrics | Business | **IMPLEMENT** |
| Forecast sheets | Forecast UI | — | — | — | — | — | — | **DEFERRED** (S4 FIGMA) |
| Narrative burn/runway | Explanation | AI optional | DET facts | insights | Optional | Insights GET | Business | **IMPLEMENT** template |

---

## Metric seed → S8 compute plan

| Metric code | Class | Plane | S8 action |
|-------------|-------|-------|-----------|
| `BUDGET_UTILIZATION` | DET | TS | IMPLEMENT when budget+spend exist |
| `GROUP_CONTRIBUTION_COMPLETION` | DET | TS | IMPLEMENT from group finance |
| `PARTICIPATION_RATE` | DET | TS | IMPLEMENT if evidence |
| `BUSINESS_BURN_RATE` | DET | TS | IMPLEMENT |
| `BUSINESS_RUNWAY_MONTHS` | DET | TS | IMPLEMENT (depends burn) |
| `RECOVERY_SCORE` / `MOOD_STATE` / `RHYTHM_CONSISTENCY` / `WELLBEING_STATE` | DET | TS | Mirror existing pulse scores into `metric_current` when present |
| `GOAL_PROGRESS` / `MILESTONE_PROGRESS` | DET | TS | DEFER if insufficient evidence |
| `SLA_COMPLIANCE` | DET | TS | DEFER (weak product surface) |
| `RELATIONSHIP_INVESTMENT` / `MEMORY_STRENGTH` | DET | TS | DEFER / FIGMA_GAP |

---

## Consent mapping

| Processing | Purpose code | When |
|------------|--------------|------|
| Personal DET metrics persist | `PERSONAL_ANALYTICS` | Enqueue + execute |
| Business DET metrics | `BUSINESS_ANALYTICS` | Enqueue + execute |
| Template/AI narrative write to `ai.*` | `AI_INSIGHT_GENERATION` | Execute re-check |
| Recommendations | `AI_RECOMMENDATION_GENERATION` | Execute re-check |
| Action proposals | `AI_ACTION_ASSISTANCE` | Existing path; not expanded as auto-command |
| Life360 cross-domain | `CROSS_DOMAIN_LIFE360` | **DEFERRED** — no pipeline |

---

## Idempotency / versioning (implementation contract)

- Dedupe key: `userId|context|companyId|momentId|metricCode|timeWindow|sourceVersion`  
- Contracts: `AnalyticsInput.contractVersion`, result `version` aligned to `metric_version.version_number`  
- Reads return: `computedAt`, `dataThrough`, `status` (`READY`|`STALE`|`UNAVAILABLE`|`PENDING`), `version`

---

## LLM decision (S8-A)

**No LLM provider.** S8 PASS uses DET metrics + rule/template narratives. FastAPI may host template/anomaly helpers later; generative LLM is **NOT_REQUIRED**.
