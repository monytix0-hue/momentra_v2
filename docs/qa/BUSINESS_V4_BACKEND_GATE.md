# Business V4 backend production gate (B9)

<!-- Supersedes: prior status silence in this gate file. Linked from QA / release checklists.
     User: "These differences do not invalidate P2 if they are attributable... audit should identify the reason for each difference" -->

## Status (2026-11-22)

**Overall: PASS WITH GAPS — beta blocked.** P0–P2 complete; P3 analytics, P4 Android/iOS, P5 beta verification outstanding.

### P2 ledger integrity — PASS (scoped)

- Append-only REVERSAL + CORRECTION; ITEMIZED PRODUCTION (Standard + MTO); steady-state backfill create **0/0/0**.
- Exclusion policy recorded on every `business.ledger_backfill_run` audit.
- **Missing historical coverage ≠ genuine zero** — projections must keep null/unavailable when eligible source postings are incomplete.

#### Backfill expected vs created (first APPLY cycle)

| Category | Dry-run expected | Created (apply1+2) | Difference | Attribution |
| -------- | ---------------- | ------------------ | ---------- | ----------- |
| Obligations | 120 (118 at apply) | 108 | 12 (~10) | Pre-existing live P1 obligations before first apply (`created_at` < apply1): **10**. Remainder = scan drift between dry-run refreshes. Residual incomplete after classify: **0**. |
| Allocations | 75 | 68 | 7 | Pre-existing live allocations before first apply: **7**. Ambiguous settlements (**24**, `settlement_missing_parent_or_allocations`) are excluded from expected and not invented. Residual: **0**. |
| Inventory movements | 32 | 20 | 12 | Pre-existing live movements before first apply: **18** (subset overlap with expected plan = **12**). Residual eligible missing after classify: **0**. |

Formula for future audits: `expected − created − alreadyPresent = residual`. Residual > 0 ⇒ incomplete coverage (project as null, not zero).

### P3 Industry Analytics + Marketing v5.1 — PASS (scoped)

- Shared widget engine: `COMPLETE|PARTIAL|UNAVAILABLE|ZERO`; SUPERSEDED/REVERSAL excluded; residual incomplete → null.
- Widget contracts + industry extras for all **nine** subtypes; GET `/v1/business/moments/:id/widgets` + `.../widgets/:widgetId/sources`.
- Marketing **17** widgets (P01–P05, M01–M04, L01–L04, ME01–ME04) + AT-01–AT-25 automated.
- Evidence: `npx tsx --test tests/business-p3-widget-analytics.test.ts tests/business-p3-marketing-at.test.ts` → green.

### P4 Mobile Integration — PASS WITH GAPS

See [BUSINESS_V4_P4_MOBILE_GATE.md](./BUSINESS_V4_P4_MOBILE_GATE.md). OpenAPI widgets freeze + Android/iOS operating hubs wired; device Maestro nine-industry matrix still PENDING (exit blocker).

### Next: P4.5 device Maestro evidence; P5 beta verification

## Commands

```bash
cd backend/typescript
npx tsc --noEmit
npx tsx --test tests/business-v4-foundations.test.ts tests/business-v4-cross-industry.test.ts
npx tsx --test tests/operating-moments.test.ts tests/operating-trading-projection.test.ts \
  tests/operating-trading-analytics.test.ts tests/operating-retail-analytics.test.ts \
  tests/operating-manufacturing.test.ts
# With DB:
npx tsx --test tests/company-invite.test.ts tests/business-s4-finance.test.ts
npm run migrate:check-order
```

## Checklist

- [ ] B0 freeze updated (`docs/superpowers/plans/2026-11-19-business-v4-contract-freeze.md`)
- [ ] Company invites Group-parity (`/j/{code}` + roles)
- [ ] V104 foundations migration present
- [ ] Nine subtypes catalog + Quick Adds fail-closed
- [ ] Marketing Client 10 / Own 8; Own rejects invoice/collection
- [ ] Trading / Distribution / Retail / MFG operating tests green
- [ ] Group facet GETs not stubbed 501
- [ ] Android/iOS contract smoke (manual / Maestro) against live `/v1/business/*`
