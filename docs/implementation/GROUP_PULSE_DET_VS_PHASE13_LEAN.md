# Group Pulse DET (GRP-002) vs Phase 13 Lean

## Separate tracks

| Track | Gap ID | What it closes | Status |
|-------|--------|----------------|--------|
| **Phase 13 Lean product analytics** | (new Lean stack V059–V068) | Founder/Product/VC KPIs: WAM, invite virality, participation | Schema + Group emit + KPI 20/30–35 refresh **landed** |
| **Group Pulse DET freeze** | **GRP-002** | In-app Group Pulse scores / contribution / participation rate via `analytics.metric_*` + projection | **Still OPEN** — not closed by Phase 13 |

## Why separate

- Personal Phase 7 (`V058`) freezes **Pulse-surface** DET metrics into `analytics.metric_version`.
- Phase 13 Lean is a **different warehouse** (`analytics_raw` / `_core` / `_mart`) for product dashboards.
- Closing Lean invite→join KPIs does **not** authorize inventing Group Pulse health rings or replacing finance-first Pulse reads.

## GRP-002 remaining work (explicitly deferred)

1. Freeze Group metric catalogue (parallel to `PERSONAL_PHASE7_PULSE_METRIC_MAP.json`)
2. ACTIVE `metric_version` rows + golden vectors
3. DET jobs writing `GROUP_CONTRIBUTION_COMPLETION` / `PARTICIPATION_RATE` with evidence gates
4. Bind server-authoritative values into `projection.group_pulse` / client Pulse (no invented “Coming soon” scores)

See also: [PHASE13_GROUP_LEAN_ANALYTICS_GAP.md](PHASE13_GROUP_LEAN_ANALYTICS_GAP.md), [MASTER_GAP_REGISTER.csv](../audit/MASTER_GAP_REGISTER.csv) `GRP-002`.
