# S9-C — Database / Pool / Cold-Start Hardening

**Status:** PASS  
**Authorized:** S9-C only (S9-G/H not started)  
**V030:** untouched  
**Harness:** `scripts/s9-c-cold-measure.ts`, `scripts/s9-c-remeasure.ts`

---

## Diagnosis

| Signal | Measurement |
|--------|-------------|
| DB endpoint | Supabase **transaction pooler** `*.pooler.supabase.com:6543` |
| Fresh `Client.connect` | p50 **~473 ms** (TLS/DNS/pooler handshake) |
| `SELECT 1` after connect | **~112 ms** (RTT floor) |
| Warm pool checkout | **~0.01 ms** |
| Parallel 5× connect wall | **~448 ms** ≈ single connect (width does not hide handshake) |
| Critical EXPLAIN | `user_profile`, `personal_moments`, companies — **Index Scan** (exec ≪1 ms) |

**Cold `/v1/me` ~1350 ms was connection establishment**, not SQL. Opening five sockets on an empty pool paid ~450 ms handshake plus ensure + query layers.

---

## Tradeoff (prewarm)

| `DB_POOL_MIN` | Idle pressure | First `/v1/me` after boot (approx) |
|---------------|---------------|-------------------------------------|
| 0 | none | ~1150–1350 ms |
| **2 (default)** | **2 pooler sockets** | **~666 ms** |
| 5 | 5 sockets | lower still (not default — avoid excess idle) |

Chose **min=2**: material cold cut without parking five idle connections permanently at boot. Pool still **grows to 5** on first bootstrap after prewarm so warm stays ~1 RTT.

---

## What shipped

1. **`DB_POOL_MIN` (default 2)** + `prewarmPool()` at `startServer` listen  
2. **`DB_CONNECTION_TIMEOUT_MS` (default 10s)** + pool error logging + `getPoolStats()`  
3. **Cold stampede guard** — if `totalCount===0`, bootstrap concurrency capped at **2**; otherwise width **5** so warm can grow  
4. **`ensureUserProfile`** — single `INSERT … ON CONFLICT DO NOTHING` (1 RTT when unknown; warm still in-process skip)  
5. EXPLAIN inventory recorded (indexes in use; no blind new indexes)

---

## Before / after

| Metric | S9-B / pre-C | S9-C (prewarm min=2) |
|--------|--------------|----------------------|
| Warm `/v1/me` p95 | ~116 ms | **122 ms** (≤150 ✓) |
| Cold `/v1/me` | **~1350 ms** | **~666 ms** (−51%) |
| Cold without prewarm | — | ~1153 ms (stampede guard helps some) |
| Prewarm cost (boot) | — | ~1.1 s for 2 sockets (off request path) |

---

## Residual

- Sub-~500 ms cold would need **min≈5** or colocating API nearer `ap-south-1` / session pooler evaluation — not done (idle-pressure tradeoff).  
- After `idleTimeout` (30s) drains the pool, next request can re-pay connect cost until traffic or re-prewarm.  
- Scale/query shape for Group/Business → **S9-G/H** (not started).

---

## Gate

Warm ≤150 ms p95 **PASS**. Cold materially reduced **PASS**.  

**STOP before S9-G/H** until authorized.
