# S9 Performance Audit (S9-A)

**Status:** COMPLETE — measure/audit only  
**Authorized:** S9-A only (S9-B→P not started)  
**Product scope:** FROZEN  
**V030:** DO NOT RUN / DO NOT MODIFY  
**Captured:** 2026-08-26T16:36:10Z  
**Harness:** `backend/typescript/scripts/s9-a-baseline-measure.ts`  
**Companion matrix:** [`S9_BASELINE_MATRIX.md`](./S9_BASELINE_MATRIX.md)

---

## Hard rules observed

- **No optimization code** shipped during S9-A.
- Obvious defects → `PERF_FINDING` only; fixes deferred to S9-B→O.
- Every proposed S9-B→O change below maps to a measured finding ID.

---

## Methodology

| Aspect | Detail |
|--------|--------|
| Runner | In-process `supertest` + `ALLOW_DEV_AUTH=1` |
| DB | Remote Postgres (pool max 10, idle/statement timeout 30s) |
| Auth cost | Dev UID path + **always-on** `provisionUserProfile` upsert (not production Firebase `verifyIdToken`) |
| Inventory under test | Fresh user: **0** personal/group/business moments, **0** companies |
| Native | Android TTCS **instrumented** (`ttcsMs` on cache hit); iOS TTCS **instrumented**; **iOS runtime blocked on Windows**; Android device timings **not collected this run** |
| Scale fixtures | Small/Med/Large **intent recorded**; seeds **NOT_SEEDED_THIS_RUN** (current DB is mid-size global, not controlled per-moment fixtures) |

Floor signal: `GET /health/ready` warm **p50 ≈ 112 ms** ≈ one Postgres RTT under this environment.

---

## Headline result: why `/v1/me` is ~1s

S9-A fresh warm baseline (n=20, empty inventory):

| Metric | S0 | S1 sample | **S9-A** |
|--------|----|-----------|----------|
| p50 | 995 ms | — | **791 ms** |
| p95 | 1023 ms | ~1087 ms | **836 ms** |
| p99 | — | — | **848 ms** |
| cold (1st) | — | — | **1427 ms** |

### Decomposition (empty inventory — RTT-dominated)

```text
User action (app open / refresh)
  → native auth restore + optional BootstrapCache paint (TTCS if cache hit)
  → network GET /v1/me
  → TS middleware (request id, auth)
  → auth: resolveDevIdentity / (prod: Firebase verify — NOT measured here)
  → auth: provisionUserProfile UPSERT          ~111 ms   (separate pool query)
  → withDb → getMeBootstrap (6 sequential SQL, no Redis, no Promise.all):
       1 profile                               ~111 ms
       2 personal moments ≤20                  ~109 ms
       3 group moments ≤20                     ~113 ms
       4 business moments ≤20                  ~112 ms
       5 companies (UNBOUNDED list)            ~111 ms
       6 capabilities JOIN/EXISTS ≤200         ~113 ms
     sum ≈ 670–680 ms handler SQL
  → serialization (~0.6 KB body)
  → network
  → native merge + render (not measured this run)
```

**Verdict:** On an empty account, each SQL is cheap but **pays full remote RTT**. Six sequential bootstrap queries + one provision upsert ≈ **7 × ~110 ms ≈ 770 ms**, matching the ~790 ms endpoint p50. This is **not** an N+1 of inventory size yet; it is **sequential round-trip amplification**. Production Firebase verify will add more on top.

**Implication for S9-B:** Prefer reducing **round trips / parallelization / selective cache / skip redundant provision on warm path** over rewriting response shape. Arbitrary rewrite without addressing RTT stacking will miss the bottleneck.

---

## Flow waterfalls (other hot paths)

### Personal Pulse / Activity (warm, empty)

| Flow | p50 | p95 | Notes |
|------|-----|-----|-------|
| `GET /v1/personal/pulse` | 227 ms | 250 ms | Fixed query count; no Redis |
| `GET /v1/personal/activity?limit=20` | 227 ms | 247 ms | Cursor pagination; page2 N/A (empty) |
| `GET /v1/personal/life` | 337 ms | 345 ms | Larger seeded payload (~3.3 KB) |
| `GET /v1/analytics/insights` | 343 ms | 369 ms | Only Redis `cacheGetJson` consumer |
| Group / Business pulse | **NOT MEASURED** | — | No moments on harness user |

### Expense submit (code inventory — not timed)

`createExpense` is **~8 sequential awaits**: governance → moment SELECT → expense INSERT → personal context → outbox → audit → activity → pulse bump (`SELECT FOR UPDATE`). Suspect latency stack similar to `/v1/me` (RTT × step count). Time under load in S9-F after authorization.

### Moment / Company switch (visible-content-ready)

Client: context/company change → inventory from cached `/v1/me` → tab fetch (pulse/activity). Server: pulse paths fixed-query. **End-to-end visible-content-ready not timed on device this run.**

---

## Infrastructure inventory

| Area | Current state | Risk |
|------|---------------|------|
| DB pool | max **10**, idle 30s, statement_timeout 30s | Contended under concurrent mobile + workers |
| Redis | Used by **analytics insights only**; `/v1/me` / pulse **uncached** | Miss opportunities; no hit/miss metrics shipped |
| Outbox | Poll **2000 ms**, batch **10** | Dispatch latency floor ≥ poll interval |
| BullMQ / analytics worker | Present (S8); depth/retry metrics not baselined live | S9-E |
| Rate limit | `rateLimitMiddleware` **exists but not mounted** on `/v1` | Production abuse risk |
| FastAPI | Optional; circuit/fallback exist; core ready = DB only | Unavailable behavior OK by design |
| Indexes | V017 covers expense/moment/company paths | Validate with EXPLAIN under Large fixtures in S9-C/G/H |

---

## Scale fixtures (intent vs measured)

| Tier | Target | Status |
|------|--------|--------|
| Small | 5 members / 25 finance | **NOT_SEEDED_THIS_RUN** |
| Medium | 25 members / 500 finance | **NOT_SEEDED_THIS_RUN** |
| Large | 100 members / 5,000 finance+activity; multi-company Moments | **NOT_SEEDED_THIS_RUN** |

**Current global DB probe (not controlled fixture):** participants 84, expenses 64, activity 82, `group_finance_position` 18, companies 29.

**Code risk without Large run:** Group pulse embeds **`group_finance_position` with no LIMIT** (participants × currencies) — size-dependent degradation likely; measure in S9-G before optimizing elsewhere.

---

## Mobile

| Platform | Instrumentation | Runtime this run |
|----------|-----------------|------------------|
| Android | `AppShellViewModel.ttcsMs` on **BootstrapCache hit** only | **Not measured** (no device/emulator harness in S9-A) |
| iOS | `AppShellModel.ttcsMs` on cache hit | **Blocked** — agent host is Windows |

Cold/warm launch + navigation timings → S9-J when authorized + device available.

---

## Prioritized findings

| ID | Sev | Flow | Evidence | Suspected cause | Recommended S9 step |
|----|-----|------|----------|-----------------|---------------------|
| PERF_FINDING-001 | **P1** | `GET /v1/me` | Warm p95 **836 ms** → **S9-B 116 ms** | 6 sequential bootstrap SQLs + provision upsert; RTT stacking | **S9-B — CLOSED** (≤400 PASS; see `S9_B_BOOTSTRAP_REPORT.md`) |
| PERF_FINDING-002 | **P1** | Auth → `/v1/me` | `provisionUserProfile` **~111 ms every request** → warm **~0 ms** | Upsert on every authenticated call | **S9-B — CLOSED** (`ensureUserProfile` + known-user skip) |
| PERF_FINDING-013 | **P1** | Cold `/v1/me` | Cold **~1350 ms** → **S9-C ~666 ms** (prewarm min=2) | Supabase pooler TLS ~450 ms/connect; empty-pool stampede | **S9-C — CLOSED** (see `S9_C_DATABASE_REPORT.md`) |
| SCALE_BLOCKER-G1 | **P1** | Group expense submit | p95 3.6s → 10s → **35s** (5/25/100 members) | Sync fan-out shares + per-member activity | **S9-G documented**; fix later |
| SCALE_BLOCKER-G2 | **P1** | Group pulse | ~700–800 ms flat | Sequential RTT in projection, not expense count | **S9-G documented** |
| SCALE_BLOCKER-G3 | **P2** | Group finance positions | rows = members; no LIMIT | Unbounded read | **S9-G documented** |
| SCALE_BLOCKER-H1 | **P1** | Business writes | ~2 s p95 flat | Sequential command stack | **S9-H documented** |
| PERF_FINDING-003 | **P0** | API security | `rateLimitMiddleware` not mounted on `/v1` | Hardening gap | **S9-K — CLOSED** (mounted + memory/Redis; see `S9_K_RATE_LIMIT_REPORT.md`) |
| PERF_FINDING-004 | **P2** | Group pulse/finance | Unbounded `group_finance_position` read | Payload grows with members×currencies | **S9-G** (+ measure Small/Med/Large) |
| PERF_FINDING-005 | **P2** | Companies on `/v1/me` | `listCompanies` has **no LIMIT** | Large multi-company users inflate bootstrap | **S9-B** / **S9-H** |
| PERF_FINDING-006 | **P2** | Expense create | ~8 sequential awaits | RTT × steps; lock on pulse bump | **S9-F** |
| PERF_FINDING-007 | **P3** | Redis | Only analytics uses cache | Missed read caching for pulse/`/me` | **S9-D** after B targets set |
| PERF_FINDING-008 | **P3** | Outbox | Default poll 2s / batch 10 | Dispatch latency floor | **S9-E** |
| PERF_FINDING-009 | **P3** | Observability | No continuous p50/p95/p99 export for hot routes | Can't gate regressions in prod | **S9-M** |
| PERF_FINDING-010 | **P4** | Capabilities SQL | Heavy JOIN/EXISTS even when empty (~RTT) | Complexity vs value for shell | **S9-B** if still hot after parallelization |
| PERF_FINDING-011 | **P1** | App bootstrap TTCS | Instrument only; no device numbers | Unknown cold-start UX vs `/v1/me` | **S9-J** (+ Android measure; iOS on Mac) |
| PERF_FINDING-012 | **P2** | Scale unknown | Fixtures not seeded | Can't prove/refute data-size degradation | **S9-G/H** seed+measure before broad query rewrites |

Severity key: **P0** production blocker (security/reliability) · **P1** user-visible > target · **P2** scaling risk · **P3** efficiency/cost · **P4** nice-to-have.

---

## Proposed S9-B→O mapping (do not start until authorized)

| Step | Drive from findings | Suggested focus (evidence-based) |
|------|---------------------|----------------------------------|
| **S9-B** | 001, 002, 005, 010 | `/v1/me` round-trip reduction; freeze numeric SLA after first improved re-measure |
| **S9-C** | 001, 004, 012 | EXPLAIN under fixtures; pool; indexes only where evidence shows |
| **S9-D** | 007 | Redis for bootstrap/pulse if B proves need |
| **S9-E** | 008 | Outbox/BullMQ depth, delay, retry |
| **S9-F** | 006 + Personal pulse/activity | Personal domain latency |
| **S9-G** | 004, 012 | Group scale fixtures Small→Large |
| **S9-H** | 005, 012 | Business multi-company/Moment scale |
| **S9-I** | (S8 inventory) | FastAPI down / circuit — resilience not latency primary |
| **S9-J** | 011 | Mobile TTCS cold/warm + nav |
| **S9-K** | 003 | Rate limits, headers, secrets |
| **S9-L** | — | Chaos after observability |
| **S9-M** | 009 | p50/p95/p99 + queue/DB/cache metrics |
| **S9-N** | 012 | Load/soak (after G/H baselines) |
| **S9-O** | — | Deploy/backup (not V030) |
| **S9-P** | — | Report + STOP before V030 |

---

## Recommended `/v1/me` target freeze (for decision after S9-A)

Do **not** treat “arbitrary rewrite” as the goal. Proposed decision frame for the next authorization:

1. **Primary bottleneck confirmed:** sequential remote round trips (empty account).
2. **Interim SLA candidate (to confirm post S9-B):** warm p95 **≤ 400 ms** in this same harness (≈ half of S9-A / clear win vs S0 1023 ms), without inventing product fields.
3. Re-measure with Medium/Large inventory before declaring B done.

---

## Gate

**S9-A COMPLETE** when this file + `S9_BASELINE_MATRIX.md` exist with measured baselines and prioritized findings.

**STOP.** Do not start S9-B→P or V030 until product owner authorizes the next step from this evidence.
