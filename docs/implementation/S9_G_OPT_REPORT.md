# S9-G-OPT — Group expense batching + Group Pulse

**Status:** PASS  
**Captured:** 2026-08-26T17:40:34Z (expense/pulse)  
**Harness:** `backend/typescript/scripts/s9-opt-remeasure.ts`  
**Baseline:** [`S9_G_GROUP_SCALE_REPORT.md`](./S9_G_GROUP_SCALE_REPORT.md)

---

## Changes

| Area | Fix |
|------|-----|
| Expense shares | Single `INSERT … SELECT UNNEST` (was N inserts) |
| Obligations | One statement from shares (was N) |
| Activity | One moment-scoped `recent_activity` row (was per-member fan-out) |
| Positions | Bulk `UNNEST` upsert + snapshot in one RTT |
| Side effects | `recordCommandSideEffects` — event+outbox+audit+activity in one RTT |
| Pre-checks | Fail-closed policies + membership (dropped redundant moment EXISTS stack) |
| Pulse / finance | Membership + meta + bounded finance in **one** RTT |
| G3 positions | Pulse: viewer + top 20 by `\|net\|`; finance: top 50; `positionsTruncated` + `positionsSemantics` |

No broad cache. No schema redesign.

---

## Before → After (p95 ms)

| Measure | Before (S9-G) | After (OPT) | Target |
|---------|---------------|-------------|--------|
| Expense EQUAL 5 members | **3646** | **1766** | — |
| Expense EQUAL 25 members | **10361** | **1798** | — |
| Expense EQUAL 100 members | **35431** | **1819** | **&lt; 2000** ✓ |
| Pulse warm 5 / 25 / 100 | 712 / 691 / 800 | **144 / 146 / 146** | **≤ 300–400** ✓ |
| Finance `positionRows` @100 | 100 (= members) | **20** pulse / **50** finance (truncated) | bounded ✓ |

Expense latency is now **flat with member count** (~1.7–1.8 s), not O(members).

---

## Verdict

| ID | Result |
|----|--------|
| S9-G1 | **PASS** — 100-member EQUAL p95 **1819 ms** (&lt; 2 s); ~19× faster than 35 s baseline |
| S9-G2 | **PASS** — Pulse warm p95 **~146 ms** (near DB floor) |
| S9-G3 | **PASS** (product semantics) — `viewer_plus_top_by_abs_net` / `top_by_abs_net_paginated_default_50` |

Remaining write cost is a small fixed RTT stack (idempotency + auth + ~6 domain statements), not fan-out.
