# S8 AI + Analytics Audit

**Date:** 2026-08-26  
**Scope:** Deterministic analytics, optional FastAPI compute, AI narratives, consent, async pipeline  
**Figma file:** `TzLvwVwlPbeVB8ug1zB3GM`  
**Rule:** DET without FastAPI where practical. LLM not required for PASS. S8-I Life360/Circle fully DEFERRED. V030 untouched.

```text
S0–S7 PASS / CLOSED
S8-A AUDIT — THIS DOCUMENT
→ no FastAPI feature code before matrix proves surfaces
```

---

## Executive summary

| Area | Classification |
|------|----------------|
| `analytics.*` schema (V009) + metric seeds (V022) | **REUSE** / SCHEMA live; computation **MISSING** |
| `ai.*` schema (V013) | **REUSE** schema; writers **MISSING** (except action-proposal execute stub) |
| Client telemetry `analytics.client_*` (V035) | **REUSE** / live ingest |
| Outbox → BullMQ → projection-worker | **REUSE** transport; projection rebuild **STUB** |
| Analytics-worker | **STUB** (D11 count poll) |
| FastAPI `fastapi-ai` | **STUB** `/health/live` + `/v1/inference` |
| TS → FastAPI client | **MISSING** |
| Deterministic metric engine | **MISSING** → IMPLEMENT in TS/SQL |
| `deterministic_insight` / `attention_item` writers | **MISSING** |
| `ai.ai_insight` writers | **MISSING**; LLM **NOT_REQUIRED** for PASS (rule/template OK) |
| Consent grant/withdraw (S7) | **REUSE**; execute-time assert **MISSING** |
| Pulse finance totals (Personal/Group/Business) | **REAL_DATA** (projection/finance) — DET |
| Life `aiInsights` / Pulse “AI Insights” cards | **FIGMA_GAP** / Coming Soon / **API_GAP** |
| Life360 / Circle intelligence | **DEFERRED** (S5/S6 Coming Soon) |
| Business forecast sheets | **DEFERRED** / **API_GAP** (S4) |
| Core `/health/ready` | **REUSE** — DB only; must **not** gate on FastAPI |

---

## Architecture locks (from approved plan)

```text
Mobile → TypeScript API → PostgreSQL / Redis / BullMQ
                         → Analytics Worker
                              ├ DET metrics in TS/SQL (preferred)
                              └ optional FastAPI (heavy / forecast / anomaly / narrative)
                         → persist metric_* / deterministic_insight / ai_insight
                         → scoped Redis cache
Mobile ← GET last-known + computedAt/dataThrough/status/version
```

- FastAPI never CRUD; no unrestricted Postgres initially  
- Consent re-check at **execute** time  
- Narrative ≠ command  
- LLM not required for S8 PASS  

---

## SQL inventory (V001–V040)

### Analytics (V009)
`metric_definition`, `metric_version`, `metric_input_definition`, `metric_dependency`, `threshold_definition`, `calculation_run`, `metric_observation`, `metric_current`, `attention_item`, `deterministic_insight`

### AI (V013)
`context_session`, `context_item`, `inference_run`, `ai_insight`, `recommendation`, `action_proposal`, `action_proposal_parameter`, `provenance`

### Seeds (V022) — metric codes
`RECOVERY_SCORE`, `MOOD_STATE`, `RHYTHM_CONSISTENCY`, `WELLBEING_STATE`, `GOAL_PROGRESS`, `MILESTONE_PROGRESS`, `BUDGET_UTILIZATION`, `GROUP_CONTRIBUTION_COMPLETION`, `PARTICIPATION_RATE`, `BUSINESS_RUNWAY_MONTHS`, `BUSINESS_BURN_RATE`, `SLA_COMPLIANCE`, `RELATIONSHIP_INVESTMENT`, `MEMORY_STRENGTH`

### Telemetry (V035)
`analytics.client_session`, `analytics.client_event` — first-party UX analytics (not finance intelligence)

### Consent purposes (V021) relevant to S8
`PERSONAL_ANALYTICS`, `BUSINESS_ANALYTICS`, `AI_INSIGHT_GENERATION`, `AI_RECOMMENDATION_GENERATION`, `AI_ACTION_ASSISTANCE`, `MEMORY_PATTERN_ANALYSIS`, `CROSS_DOMAIN_LIFE360`

### Projection links (V014)
Pulse/Life/Memory projections carry optional `latest_ai_insight_id`; Business pulse `runway_months` **must** come from `analytics.metric_current` (projection must not invent runway).

### Workers / roles (V029)
`momentra_analytics_worker`, `momentra_ai_worker`, `momentra_outbox_worker`, `momentra_projection_worker`

### Outbox (V011)
`events.outbox_event` — live dispatcher exists

### Retention when AI consent withdrawn
**Documented policy:** Block **new** AI inference/jobs. **Retain** prior `ai.ai_insight` / recommendations for read until natural `valid_until` / user dismiss. **Do not invent** hard-delete cascade of AI rows in S8.

---

## TypeScript / workers inventory

| Component | Path | Status |
|-----------|------|--------|
| Health live/ready | `app.ts` | Ready = DB only |
| Outbox insert | `platform/events/outbox.ts` | Live |
| Outbox dispatcher | `workers/outbox-dispatcher` | Live |
| Projection worker | `workers/projection-worker` | Stub ack only |
| Analytics worker | `workers/analytics-worker` | Stub poll |
| Projection reads | `modules/projection/service.ts` | Live; Life AI fields API_GAP |
| AI action execute | `modules/ai/action-proposal.command.ts` | Live status flip only |
| Telemetry | `modules/telemetry/*` | Live |
| Consent | `modules/account/service.ts` | list/grant/withdraw; no assert helper |
| Redis cache helpers | `platform/redis/client.ts` | Defined, unused |
| FastAPI client | — | **MISSING** |

---

## FastAPI inventory

| Path | Status |
|------|--------|
| `backend/python/fastapi-ai/main.py` | Stub live + inference |
| `requirements.txt` | fastapi, uvicorn only |
| Compose `fastapi-ai` | Port 8000; shared `.env` |

**S8-A decision:** No LLM provider selected. FastAPI will expose versioned compute endpoints for optional heavy/narrative work; DET metrics compute in TypeScript.

---

## Figma / mobile intelligence surfaces

| Surface | Figma / notes | Client today | Class |
|---------|---------------|--------------|-------|
| Personal Pulse money snapshot | Pulse finance | REAL_DATA totals | DET |
| Personal Pulse “AI Insights” | Coming Soon (no node id) | Stub | FIGMA_GAP / DEFERRED narrative |
| Personal Life `aiInsights` | `1047:7689` / `1047:7707` | FIGMA_SEEDED | API_GAP → IMPLEMENT DET facts + template narrative |
| Rel Pulse INTELLIGENCE | Coming Soon | Stub | FIGMA_GAP |
| Moments/Memory AI Insights | Coming Soon | Stub | FIGMA_GAP / DEFERRED |
| Group Pulse finance | Real finance projection | REAL_DATA | DET |
| Business Pulse runway/health | Fields exist; runway from metrics required | Partial | DET IMPLEMENT |
| Business forecast sheets | `700:9789`… | DEFERRED S4 | DEFERRED |
| Life360 | `1075:7637` | Coming Soon | **DEFERRED** S8-I |
| Circle | `1075:7556` | Coming Soon | **DEFERRED** |

---

## Deterministic vs AI (decision guide)

| Example | Class | Compute plane |
|---------|-------|---------------|
| Spend this month / MoM % / category share | DET | TS/SQL |
| Group contribution % | DET | TS/SQL |
| Business burn / runway months | DET | TS/SQL → `metric_current` |
| “Three recurring expenses drove the increase” | DET fact + optional template | TS (+ optional FastAPI narrative) |
| Free-form LLM essay over ledger | AI | **NOT for S8 PASS** |

---

## S8-A → implementation implications

1. Implement DET engine writing `metric_observation` / `metric_current` / `deterministic_insight` in TypeScript.  
2. Promote analytics-worker for outbox-driven refresh with idempotent dedupe keys.  
3. FastAPI foundation + optional narrative endpoint; never gate core ready.  
4. Read APIs with stale metadata; wire Personal/Group/Business surfaces that have REAL_DATA.  
5. Life360/Circle: document DEFERRED only — no pipelines.  
6. LLM: skip unless a later matrix row requires generative copy after DET+templates.

See [`S8_AI_ANALYTICS_MATRIX.md`](S8_AI_ANALYTICS_MATRIX.md).

## S8-I Life360 / Circle

**DEFERRED.** Figma `1075:7637` / `1075:7556` Coming Soon. No analytics enqueue, metrics, or FastAPI calls for these scopes. No hidden pipelines.
