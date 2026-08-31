# S9-H-OPT — Business write RTT + populated `/v1/me` + invoice conflict

**Status:** PASS (H2, H3) / PARTIAL (H1 vs 750 ms stretch)  
**Captured:** 2026-08-26T17:40:34Z (me) + business remeasure ~17:45Z  
**Harness:** `backend/typescript/scripts/s9-opt-remeasure.ts` (`S9_OPT_ONLY=business` for write remeasure)  
**Baseline:** [`S9_H_BUSINESS_SCALE_REPORT.md`](./S9_H_BUSINESS_SCALE_REPORT.md)

---

## Changes

| Area | Fix |
|------|-----|
| `/v1/me` | Capabilities moved into the **same parallel inventory wave** (no second RTT wave) |
| Expense gate | Membership + fail-closed policies + approval prefs in **one** query |
| Expense write | Expense + context (+ snapshot when posted) CTE; side effects coalesced |
| Revenue / invoice | Snapshot/lines batched; side effects via `recordCommandSideEffects` |
| Idempotency acquire | Common path: advisory lock + insert in **one** RTT |
| Invoice conflict | `23505` / `uq_invoice__company_number` → **409 `INVOICE_NUMBER_CONFLICT`** |

Transaction correctness preserved (all statements on the command client / `runCommand` tx).

---

## Before → After (p95 ms)

| Measure | Before (S9-H) | After (OPT) | Target |
|---------|---------------|-------------|--------|
| `/v1/me` populated inventory | **~760–960** | **267–331** | one parallel wave ✓ |
| Business expense | **~1800–2000** | **924** | &lt; 750 (stretch) — **partial** |
| Business revenue | **~1800–2000** | **921** | &lt; 750 — **partial** |
| Business invoice | **~1800–2000** | **912** | &lt; 750 — **partial** |
| Duplicate invoice number | raw infra / 500-class | **409 `INVOICE_NUMBER_CONFLICT`** | ✓ |

---

## Invoice conflict check

```
firstStatus: 201
secondStatus: 409
secondErrorCode: INVOICE_NUMBER_CONFLICT
```

Idempotency keys remain distinct per attempt; conflict is uniqueness, not idempotency replay.

---

## Verdict

| ID | Result |
|----|--------|
| S9-H1 | **PARTIAL** — ~**2×** faster (~1.9 s → ~0.92 s); remaining ~floor from idempotency + auth + domain RTTs (~110 ms × ~8). Further &lt;750 needs another coalesce pass or pool geography change — not a scale bug. |
| S9-H2 | **PASS** — populated `/v1/me` p95 **~300 ms** |
| S9-H3 | **PASS** — stable `INVOICE_NUMBER_CONFLICT` |

---

## Stop line

P1 OPT for G/H complete for authorization block. **Do not** start S9-D/E/F/J/L–P without re-authorization. V030 remains last.
