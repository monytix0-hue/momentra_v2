# Business Deployment Closure Status

Authority: runtime audit plan + `Momentra_Business_Deployment_Closure_Pack.xlsx`.  
**Release rule:** READY when every P0/P1 BLOCKER is CLOSED. P2 AI may stay intentionally deferred / honest-empty.

## Wave 0 — P0 client rewire / validate existing routes — COMPLETE

- Milestone Quick-Add → `POST /v1/moments/:momentId/milestones` (`work.milestone`; optional `goalId` auto-ensures default goal).
- Blocker stays on `POST …/issues`.
- Risk Flag → `POST …/risks` (`business.risk`).
- Poll: `createPollCommand` allows BUSINESS company membership (not group-only); activity domain `BUSINESS`.
- Vendor/SLA routes confirmed; no duplicate invent.

## Wave 1 — P0 canonical writers — COMPLETE

Schema `V054__business_deployment_closure.sql` + `business-closure-writers.ts` + client rewires:

| Feature | Route |
|---------|-------|
| Tax | `POST …/tax-obligations` |
| Forecast | `POST …/forecast-scenarios` |
| Investor Update | `POST …/investor-updates` |
| Budget Alert | `POST …/budget-alerts` |
| Budget Review | `POST …/business-reviews` |
| Decision | `POST …/decisions` |
| Meeting | `POST …/meeting-records` |
| Recognition | `POST …/recognitions` |
| Retrospective | `POST …/retrospectives` |
| Activity Log | `POST …/activity-log-entries` |

## Wave 2 — P0 cross-cutting correctness — COMPLETE

Remediated POSTs use auth middleware, `requireIdempotencyKey`, `runCommand`, stable error envelopes, outbox via `insertDomainEventAndOutbox`, `projectionHints` + business projection refresh. Milestone BUSINESS domain refreshes business projections after write.

## Wave 3 — P1 reads / sharing — COMPLETE

- `GET …/capacity`, `/workload`, `/mom-deltas`, `/progress-snapshot`, `/roster`, `/weekly-report`
- `POST …/share-link`, `POST …/issues/:issueId/evidence`
- Team Ops Pulse/Moments and Runway Moments bind capacity/workload/MoM/progress where UI already had gauges.
- Escalate/reminder affordances: honest “API not mounted” (no fake network).

## Wave 4 — P1 UX + docs — COMPLETE

- Ops Vendor + SLA: select-existing vendor before create (Android + iOS).
- FIGMA coverage + Runway/Team Ops field matrices refreshed to WIRED canonical routes.

## Wave 5 — P2 deferred AI — PASS WITH DEFERRAL

Team / Financial / Ops Intelligence, Pattern Network, Playbook remain honest-empty shells until AI workers + provenance. No fabricated insights.
