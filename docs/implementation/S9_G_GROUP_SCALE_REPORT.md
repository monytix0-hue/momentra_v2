# S9-G — Group Scale Validation

**Status:** COMPLETE (diagnose-only; no optimization code)  
**Captured:** 2026-08-26T17:19:38Z  
**Harness:** `backend/typescript/scripts/s9-gh-scale-measure.ts`  
**Companion:** [`S9_H_BUSINESS_SCALE_REPORT.md`](./S9_H_BUSINESS_SCALE_REPORT.md)  
**V030 / S9-F+:** not started

---

## Fixtures

| Tier | Members | Expenses | Seed mode | Seed time |
|------|---------|----------|-----------|-----------|
| Small | 5 | 25 (+5 live) | Full API EQUAL split | ~90 s |
| Medium | 25 | 500 (+5 live) | Bulk SQL volume + API samples | ~4 s |
| Large | 100 | 5,000 (+3 live) | Bulk SQL volume + API samples | ~16 s |

Counts verified (Large): members **100**, expenses **5005**, `group_finance_position` **100**, activity **5005**.

---

## Results (p95 ms unless noted)

| Measure | Small | Medium | Large | Growth |
|---------|-------|--------|-------|--------|
| Pulse | **712** | **691** | **800** | **flat** (1.12×) |
| Finance facet | 475 | 454 | 464 | flat |
| Finance `positionRows` | 5 | 25 | **100** | **= members** (unbounded read) |
| Finance payload bytes | ~1.7 KB | (grows) | (grows w/ members) | payload ∝ members×currency |
| Expense submit (EQUAL, all members) | **3646** | **10361** | **35431** | **super-linear ~9.7×** |
| Activity page 1 | 244 | 231 | 233 | **flat** (projection-bounded) |
| Activity page 2 | 230 | 1527* | 224 | flat (*one med outlier / cursor path) |
| Participants list | 284 | 238 | 241 | flat |
| Moment switch (2× pulse) | 1378 | 1372 | 1342 | flat (~2× pulse) |
| Invite redeem | mint 201 / redeem 200 in **1136 ms** | ok | skipped | — |

\*Medium activity page-2 p95 1527 ms — re-check if cursor decode/index; page-1 stayed flat.

---

## Diagnosis (before any fix)

### SCALE_BLOCKER-G1 — P1 — Expense submit O(members) fan-out

| Field | Value |
|-------|--------|
| Severity | **P1** user-visible (3.6 s → **35 s** at 100 members) |
| Flow | `POST …/group-expenses` EQUAL split |
| Evidence | p95 3646 / 10361 / 35431; ratio Large/Small **9.72** |
| Suspected cause | **Synchronous fan-out**: sequential share inserts + obligation inserts + **recent_activity row per member** + projection upserts; each step pays remote RTT |
| Not | Heavy SQL per row (EXPLAIN on empty paths was fine); not expense-count growth (write cost tracks **member count**) |
| Recommended next | S9-F/G fix pass: batch inserts, single activity row (or async outbox), fewer round trips — **do not** paper over with unbounded cache |

### SCALE_BLOCKER-G2 — P1 — Pulse latency high even at Small

| Field | Value |
|-------|--------|
| Severity | **P1** (~700–800 ms p95 vs Personal pulse ~227 ms) |
| Flow | `GET …/pulse` |
| Evidence | flat across tiers — **not** expense-count driven |
| Suspected cause | **Query/index + sequential RTT stack** inside `getGroupMomentProjection` (member assert + moment + snapshot + positions + …), not Large data scan |
| Recommended next | Decompose pulse waterfall (same method as S9-A `/v1/me`); parallelize independent reads |

### SCALE_BLOCKER-G3 — P2 — Unbounded `group_finance_position` read

| Field | Value |
|-------|--------|
| Severity | **P2** unbounded memory/payload growth |
| Flow | Pulse / finance facet |
| Evidence | `positionRows` 5 → 25 → **100** (= members); SQL has **no LIMIT** |
| Suspected cause | **Unbounded read** by design (participants × currencies) |
| Latency today | Still flat at 100 members (rows small) |
| Risk | Multi-currency × hundreds of members → large JSON; client cost |
| Recommended next | Paginate / summarize positions; keep totals projection-bounded |

### SCALE_BLOCKER-G4 — P3 — Write amplification to `recent_activity`

| Field | Value |
|-------|--------|
| Severity | **P3** efficiency/cost |
| Evidence | One activity insert **per active member** per expense in `createGroupExpense` |
| Cause | Synchronous fan-out (related to G1) |

---

## What stayed healthy

- **Activity pagination** stays ~RTT floor across 30 → 5k rows → **projection-bounded** ✓  
- **Pulse does not degrade with expense count** (projection snapshot/positions) ✓  
- Invite redeem path works (small/medium)

---

## Hard rule observance

Diagnose only. **No optimization patches** in S9-G. Fixes deferred until authorized (likely with S9-F or a focused Group write pass).

---

## STOP

S9-G report complete. Await authorization before S9-F / D / E / J / L–P.
