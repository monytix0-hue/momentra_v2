# Phase 13 Lean Analytics — Group gap map

**Source pack:** `Momentra_Analytics_Implementation_Pack_Phase13.docx` (Lean Analytics V1)  
**Repo migrations:** `V059`–`V068` (pack’s V031–V040 renumbered; live FRDS already occupied V031–V058)

## Distinction (do not conflate)

| Track | Purpose | Authority |
|-------|---------|-----------|
| **Phase 13 Lean** | Product dashboards: WAM, invite virality, activation | `analytics_raw` → `analytics_core` → `analytics_mart` |
| **GRP-002 Pulse DET** | In-app Group Pulse health / contribution metrics | `analytics.metric_*` + `projection.group_pulse` (separate; not closed by Lean pack) |

Personal Phase 7 (`V058`) freezes **Pulse-surface** metrics only. Group has **no** Pulse Phase-7 parallel yet — that remains **GRP-002**.

## Group-critical Lean items

| Pack ID | Item | Status after V059–V068 |
|---------|------|-------------------------|
| KPI 20 | Avg participants / Group Moment | Mart view + refresh job |
| KPI 30 | Invitations / Group Moment | Mart view + refresh job |
| KPI 31–33 | Invite open / join / invited activation | Mart views; open rate needs client `invite_opened` |
| KPI 34–35 | Participant→Creator + viral coefficient | Mart views |
| KPI 11 | Join Activation Rate | Core `participant_fact` path |
| E10 | `moment_created` (domain=group) | Backend emit on Group create |
| E15–E18 | invite / open / join / exit | Backend mint+redeem; client open ingest; exit deferred P1 |
| Envelope | `moment_domain=group` | Required on Lean Moment/invite events |

## Migration renumber

| Pack file | Repo file |
|-----------|-----------|
| V031 schemas/roles | `V059__lean_analytics_schemas_and_roles.sql` |
| V032 raw events | `V060__lean_analytics_raw_events.sql` |
| V033 registries | `V061__lean_analytics_registries.sql` (+ event/activity seeds) |
| V034 identity/sessions | `V062__lean_analytics_identity_sessions.sql` |
| V035 user facts | `V063__lean_analytics_user_facts.sql` |
| V036 moment facts | `V064__lean_analytics_moment_facts.sql` |
| V037 cohort marts | `V065__lean_analytics_cohort_marts.sql` |
| V038 KPI mart | `V066__lean_analytics_kpi_mart.sql` (+ 42 KPI seeds) |
| V039 indexes | `V067__lean_analytics_indexes.sql` |
| V040 views | `V068__lean_analytics_views_group_kpis.sql` (+ Group KPI views + grants) |

## Instrumentation entry points

- `modules/analytics/lean-events.ts` — non-blocking write to `analytics_raw.events` + light core upserts
- Group moment create → `moment_created`
- Invite mint (bound) → `participant_invited`
- Invite redeem / join → `participant_joined`
- Group expense → `expense_added`
- `POST /v1/analytics/lean/events` — client `invite_opened` / `moment_viewed` (optional auth)

## Apply

```bash
cd backend/typescript && npm run migrate:install
```

Refresh Group Lean KPIs into `analytics_mart.kpi_period`:

```bash
npx tsx scripts/refresh-group-lean-kpis.ts
```
