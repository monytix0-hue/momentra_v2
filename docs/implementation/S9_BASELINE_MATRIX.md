# S9 Baseline Matrix (S9-A)

**Status:** COMPLETE — measure/audit only  
**Captured:** 2026-08-26T16:36:10Z (S9-A) · **S9-B re-measure:** 2026-08-26T16:45:31Z  
**Harness:** `backend/typescript/scripts/s9-a-baseline-measure.ts` · `scripts/s9-b-remeasure.ts`  
**Audit narrative:** [`S9_PERFORMANCE_AUDIT.md`](./S9_PERFORMANCE_AUDIT.md) · [`S9_B_BOOTSTRAP_REPORT.md`](./S9_B_BOOTSTRAP_REPORT.md)  
**Environment:** Dev-auth in-process; remote Postgres RTT; pool max=10; empty inventory user unless noted.

### S9-B after (same harness class)

| Metric | S9-A | S9-B |
|--------|------|------|
| Warm `/v1/me` p95 | 835.55 ms | **115.82 ms** (≤400 PASS) |
| Cold `/v1/me` | 1427 ms | 1350 ms |
| Provision warm | ~111 ms upsert | **~0 ms cached** |
| Bootstrap model | 6 sequential | 5 parallel pool checkouts |

---

## 1. Area × measure

| Area | Measure | Baseline (S9-A) | Notes / gaps |
|------|---------|-----------------|--------------|
| App bootstrap | cold TTCS | **NOT MEASURED** (device) | Android/iOS set `ttcsMs` on **cache hit** only |
| App bootstrap | warm TTCS | **NOT MEASURED** (device) | iOS runtime blocked on Windows |
| App bootstrap | cold `GET /v1/me` | **1427 ms** (n=1) | First request after process start |
| App bootstrap | warm `GET /v1/me` | p50 **791** / p95 **836** / p99 **848** ms (n=20) | Empty inventory |
| `/v1/me` | HTTP/network (supertest local) | Included in endpoint times | No separate WAN hop |
| `/v1/me` | auth verification | Dev path only | **Prod Firebase verify NOT measured** |
| `/v1/me` | `provisionUserProfile` | **~111 ms** | Every authenticated request |
| `/v1/me` | identity/profile SQL | p50 **~111 ms** | Stage over 5 runs |
| `/v1/me` | personal moments ≤20 | p50 **~109 ms** | 0 rows |
| `/v1/me` | group moments ≤20 | p50 **~113 ms** | 0 rows |
| `/v1/me` | business moments ≤20 | p50 **~112 ms** | 0 rows |
| `/v1/me` | companies | p50 **~111 ms** | **UNBOUNDED** query; 0 rows |
| `/v1/me` | capabilities ≤200 | p50 **~113 ms** | JOIN/EXISTS; 0 codes |
| `/v1/me` | device/bootstrap payload | ~**0.6–0.65 KB** JSON | Shell inventory only |
| `/v1/me` | DB query count | **6** in handler + **1** provision | Sequential; no Redis |
| `/v1/me` | Redis | **miss / unused** | Not consulted |
| `/v1/me` | serialization | negligible vs RTT | |
| `/v1/me` | full `getMeBootstrap` | p50 **~673 ms** | Sum of 6 stages |
| Personal Pulse | cold/warm/cache | warm p50 **227** / p95 **250** ms | No server cache |
| Group Pulse | cold/warm/cache | **NOT MEASURED** | No group moment on harness user |
| Business Pulse | cold/warm/cache | **NOT MEASURED** | No business moment on harness user |
| Moment switch | visible-content-ready | **NOT MEASURED** | Client+network+render E2E |
| Company switch | visible-content-ready | **NOT MEASURED** | Same |
| Expense submit | Personal/Group/Business | **NOT TIMED** | Code: ~8 sequential awaits (PERF_FINDING-006) |
| Activity | first page | p50 **227** / p95 **247** ms | limit=20 |
| Activity | next page | **N/A** | Empty dataset / no cursor |
| DB | query latency (RTT floor) | **~110–115 ms** | Via `/health/ready` + stages |
| DB | pool | max **10**, idle 30s, stmt 30s | |
| DB | slow queries | None flagged on empty path | Need Large fixtures + `pg_stat` |
| Redis | hit/miss / latency | Insights path only; not instrumented | PERF_FINDING-007 |
| Redis | invalidation | Analytics-scoped | |
| Outbox | dispatch latency | Floor ≥ **OUTBOX_POLL_MS=2000** | batch 10 |
| BullMQ | queue delay / processing / retry | **NOT MEASURED** live | S9-E |
| Analytics | DET compute latency | insights p50 **343** / p95 **369** ms | Includes auth+DB |
| FastAPI | compute latency | **NOT MEASURED** | Optional; circuit exists |
| FastAPI | unavailable behavior | By design: DET/stale path (S8) | Ready = DB only |
| Android | cold/warm launch + nav | **Source only** | `ttcsMs` on cache hit |
| iOS | source instrumentation | Present (`AppShellModel.ttcsMs`) | **Runtime blocked (Windows)** |

### Historical carry (do not treat as S9-A)

| Source | `/v1/me` |
|--------|----------|
| S0 | p50 995 / p95 **1023** ms (n=20) |
| S1 | ~**1087** ms single sample |

---

## 2. `/v1/me` waterfall detail

| Stage | Layer | p50 ms (approx) | Finding |
|-------|-------|-----------------|---------|
| Client cache paint | native | — | TTCS if cache; else wait network |
| HTTP | network | in endpoint | |
| Middleware | TS | small | |
| Auth resolve | TS | small (dev) | Prod Firebase TBD |
| Provision upsert | DB | **111** | PERF_FINDING-002 |
| Profile | DB | **111** | |
| Personal moments | DB | **109** | |
| Group moments | DB | **113** | |
| Business moments | DB | **112** | |
| Companies | DB | **111** | PERF_FINDING-005 |
| Capabilities | DB | **113** | PERF_FINDING-010 |
| Serialize + respond | TS | small | |
| Client merge/render | native | — | S9-J |

**Bottleneck class:** sequential remote round trips (empty account), not heavy compute.

---

## 3. Endpoint percentile table (S9-A harness)

| Endpoint | n | p50 | p95 | p99 | min | max | status |
|----------|---|-----|-----|-----|-----|-----|--------|
| `GET /v1/me` (warm) | 20 | 790.51 | 835.55 | 847.92 | 782.01 | 847.92 | 200 |
| `GET /v1/me` (cold) | 1 | 1427.06 | — | — | — | — | 200 |
| `GET /v1/personal/pulse` | 10 | 227.12 | 250.17 | 250.17 | 223.49 | 250.17 | 200 |
| `GET /v1/personal/activity?limit=20` | 10 | 227.44 | 247 | 247 | 224.37 | 247 | 200 |
| `GET /v1/personal/life` | 10 | 336.77 | 344.98 | 344.98 | 332.48 | 344.98 | 200 |
| `GET /v1/analytics/insights` | 5 | 342.5 | 369.38 | 369.38 | 335.13 | 369.38 | 200 |
| `GET /health/ready` | 10 | 111.92 | 115.37 | 115.37 | 111.55 | 115.37 | 200 |

---

## 4. Scale fixture matrix

| Tier | Members | Finance / activity | Multi-company Moments | Seeded? | Pulse/activity measured? |
|------|---------|--------------------|------------------------|---------|---------------------------|
| Small | 5 | 25 | optional | **NO** | NO |
| Medium | 25 | 500 | yes (Business) | **NO** | NO |
| Large | 100 | 5,000 | yes | **NO** | NO |

**Global DB probe (uncontrolled):** members 84 · expenses 64 · activity 82 · group_finance_position 18 · companies 29.

Purpose when seeded (S9-G/H): detect **data-size-dependent** degradation (especially unbounded group finance positions), not full load testing (that is S9-N).

---

## 5. Finding priority board

| Pri | ID | One-line | Next step |
|-----|----|----------|-----------|
| P0 | PERF_FINDING-003 | Rate limit not mounted on `/v1` | S9-K |
| P1 | PERF_FINDING-001 | `/v1/me` warm p95 ~836 ms — sequential RTT stack | S9-B |
| P1 | PERF_FINDING-002 | Provision upsert every request ~111 ms | S9-B |
| P1 | PERF_FINDING-011 | Device TTCS/nav not measured | S9-J |
| P2 | PERF_FINDING-004 | Unbounded group finance positions | S9-G |
| P2 | PERF_FINDING-005 | Unbounded companies on bootstrap | S9-B/H |
| P2 | PERF_FINDING-006 | Expense create sequential awaits | S9-F |
| P2 | PERF_FINDING-012 | Scale fixtures missing | S9-G/H |
| P3 | PERF_FINDING-007 | Redis underused outside analytics | S9-D |
| P3 | PERF_FINDING-008 | Outbox poll floor 2s | S9-E |
| P3 | PERF_FINDING-009 | No continuous latency SLOs | S9-M |
| P4 | PERF_FINDING-010 | Capabilities query complexity | S9-B (after RTT fix) |

---

## 6. Re-run instructions

```bash
cd backend/typescript
npx tsx scripts/s9-a-baseline-measure.ts
```

Compare new p95 to this matrix before claiming S9-B+ wins. Do not modify V030.

---

## Gate

S9-A baseline matrix **COMPLETE**. Optimization work requires explicit authorization of the next S9 step.
