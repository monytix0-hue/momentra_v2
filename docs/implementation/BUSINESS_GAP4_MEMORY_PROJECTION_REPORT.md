# Gap 4 — Business Memory projection integrity

Date: 2026-09-30.

Classification: **PASS**

A Money refresh no longer replaces Daily or Team memories. Opening Memory from another moment in the same company returns the same company set. B4 Memory UI was not started.

B3 Life remains **PASS CANDIDATE — runtime verification pending**. No device smoke was run.

## What changed

`projection.business_memory` is still one row per company. `recent_memory_payload` now stores family slices under `families`, keyed by `BUSINESS_RUNWAY`, `BUSINESS_OPERATIONS`, and `TEAM_OPERATIONS`. A refresh writes only the family of the moment that refreshed. The slice is every active memory on every active moment of that family, not only the refreshing moment. `memory_count` and `learning_count` are the sum of those slices. Pattern and playbook counts are left alone.

`GET /v1/business/moments/:id/memory` merges the slices in a fixed family order: Money, then Daily, then Team. Each item carries `businessFamily`. The caller moment is only the membership handle. A legacy blob that has no `families` key is not served. Until the first family refresh, the read uses a company-wide live query in that same order, not the caller moment’s rows.

## Tests

`npx tsx --test tests/business-gap4-memory.test.ts` — 2 passed.

- Money, Daily, and Team memories all remain after a later Money refresh. `GET` through Money and through Daily returns the same titles, in family order.
- Posting Team, then Daily, then Money still returns Money, Daily, Team order.
- Company B does not contain Company A’s titles.
- Two Money moments both contribute. Refreshing one does not drop the other Money memory or the Daily memory. `GET` through the other Money moment matches `GET` through Daily.

## Still untouched

Realtime, location-scoped totals, and device runtime smoke. B4 Memory UI starts only after this report. It was not started here.
