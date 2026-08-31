# S8 AI + Analytics Parity Matrix

**Date:** 2026-08-26  
**Verdict:** S8 PASS (LLM NOT_REQUIRED)

| Capability | Backend | Android | iOS | Notes |
|------------|---------|---------|-----|-------|
| Audit + matrix docs | PASS | — | — | S8-A |
| FastAPI live/ready | PASS | N/A | N/A | Own plane; does not gate TS ready |
| Versioned AnalyticsInput | PASS | N/A | N/A | contractVersion=1 |
| Internal auth header | PASS | N/A | N/A | `X-Momentra-Internal-Key` |
| TS↔FastAPI client + circuit | PASS | N/A | N/A | Optional; fail-open |
| Outbox → analytics queue | PASS | N/A | N/A | Fan-out from dispatcher |
| Analytics worker | PASS | N/A | N/A | DET + optional narrative |
| Idempotent dedupe | PASS | N/A | N/A | calculation_run input_snapshot |
| DET metrics TS/SQL | PASS | N/A | N/A | Not forced through FastAPI |
| metric_current / deterministic_insight | PASS | via GET | via GET | Physical separation |
| GET `/analytics/metrics` | PASS | ApiService | — | Stale metadata |
| GET `/analytics/insights` | PASS | ApiService | APIClient | Stale metadata |
| POST `/analytics/refresh` | PASS | ApiService | APIClient | Consent-gated |
| Consent at execute | PASS | S7 hub | S7 hub | PERSONAL/BUSINESS/AI |
| AI consent OFF retains prior | PASS | — | — | Tested |
| Core ready ≠ FastAPI | PASS | — | — | Tested |
| Personal Life insights | PASS | Life GET | Life GET | Last-known / honest empty |
| Group contribution metric | IMPLEMENT | — | — | On refresh GROUP |
| Business burn/runway metric | IMPLEMENT | — | — | On refresh BUSINESS |
| Life360 / Circle intelligence | DEFERRED | Coming Soon | Coming Soon | No pipelines |
| LLM provider | NOT_REQUIRED | — | — | Template narrative OK |
| Narrative ≠ command | PASS | — | — | Insights only |

## Acceptance scenarios

| Scenario | Result |
|----------|--------|
| Happy path refresh + read | PASS (tests) |
| FastAPI DOWN / unset | PASS — DET + local template; CRUD unaffected |
| AI consent OFF | PASS — no new narrative; prior readable |
| Core `/health/ready` | PASS — DB only |
