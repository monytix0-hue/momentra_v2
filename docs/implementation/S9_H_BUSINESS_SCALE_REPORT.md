# S9-H — Business Scale Validation

**Status:** COMPLETE (diagnose-only; no optimization code)  
**Captured:** 2026-08-26T17:19:38Z  
**Harness:** `backend/typescript/scripts/s9-gh-scale-measure.ts`  
**Companion:** [`S9_G_GROUP_SCALE_REPORT.md`](./S9_G_GROUP_SCALE_REPORT.md)  
**V030 / S9-F+:** not started

---

## Fixtures

| Tier | Companies | Moments | Finance/activity rows | Seed mode | Seed time |
|------|-----------|---------|----------------------|-----------|-----------|
| Small | 1 | 2 | ~100 | API expense/revenue/invoice mix | ~100 s |
| Medium | 3 | 10 | ~2,000 | Bulk SQL + API samples | ~24 s |
| Large | 10 | 50 | ~10,000 | Bulk SQL + API samples | ~110 s |

---

## Results (p95 ms)

| Measure | Small | Medium | Large | Growth |
|---------|-------|--------|-------|--------|
| Business Pulse | **475** | **460** | **455** | **flat** |
| Activity page 1 | 233 | 230 | 257 | flat |
| Activity page 2 | 231 | 228 | 245 | flat |
| Expense submit | **2017** | **2043** | **2013** | flat |
| Revenue submit | 1813 | 1792 | 1794 | flat |
| Invoice submit | 1884 | 1900 | 1870 | flat |
| Company members list | 351 | 362 | 345 | flat |
| Teams list | 228 | 227 | 239 | flat |
| Companies list | 167 | 115 | 114 | flat |
| `GET /v1/me` (with inventory) | **959** | **778** | **759** | flat (capped inventory) |
| Moment switch (2× pulse) | 899 | 905 | 906 | flat |
| Company switch | — | 1361 | **1378** | ~ company get + 2 pulses |

Approval decide: invoice responses in this harness did **not** surface `approvalRequestId` (keys vary); decide path not latency-baselined. Vendor create used in seed; no GET vendor list API.

---

## Diagnosis

### SCALE_BLOCKER-H1 — P1 — Business write path ~2 s

| Field | Value |
|-------|--------|
| Severity | **P1** user-visible |
| Flow | business-expense / revenue / invoice create |
| Evidence | p95 ≈ **1.8–2.0 s** at all tiers (not data-size driven) |
| Suspected cause | **Sequential RTT stack** (governance + inserts + outbox + audit + activity), same class as Personal/Group writes — not Large-row scans |
| Recommended next | Waterfall decompose in S9-F/H fix pass; batch where safe |

### SCALE_BLOCKER-H2 — P1 — `/v1/me` with Business inventory ~760–960 ms

| Field | Value |
|-------|--------|
| Severity | **P1** vs empty-account S9-B floor (~116 ms) |
| Evidence | Small 959 / Med 778 / Large 759 — **flat** as company/moment counts grow (bootstrap **caps** moments at 20) |
| Suspected cause | Capabilities JOIN runs when inventory non-empty (**2nd RTT layer**) + listing companies/moments; not unbounded at Large because of LIMIT 20 |
| Recommended next | Already partially addressed in S9-B; measure capabilities skip/cache when authorizing further bootstrap work |

### SCALE_BLOCKER-H3 — P3 — Invoice number uniqueness on seed

| Field | Value |
|-------|--------|
| Severity | **P3** (harness / API error mapping) |
| Evidence | `uq_invoice__company_number` violations during Small API seed when numbers collide; surfaced as `INFRASTRUCTURE_UNAVAILABLE` |
| Suspected cause | Seed reused patterns; also error mapping should be VALIDATION not infra |
| Recommended next | Unique invoice numbers in clients; map unique violations → 409/400 |

### What stayed healthy

- Pulse / activity **do not degrade** from Small → Large row counts → **projection-bounded** reads ✓  
- Company/member/team lists stay near RTT floor ✓  
- Multi-company `/v1/me` stays capped (no unbounded company explosion in this fixture; companies still unbounded in SQL — watch H-large+ with ≫10 companies)

---

## Cross-check vs Group

| Concern | Group | Business |
|---------|-------|----------|
| Read growth with volume | Flat | Flat |
| Write growth with members | **Super-linear (P1)** | Flat vs company/moment count |
| Unbounded projection list | Positions ∝ members | Less acute on pulse |
| Switch UX | ~1.3 s (2× pulse) | ~0.9–1.4 s |

---

## Hard rule observance

Diagnose only. **No optimization patches** in S9-H.

---

## STOP

S9-H report complete. Bring findings back before authorizing S9-F / D / E / J / L–P.
