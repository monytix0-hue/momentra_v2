# S3 Group Audit

**Date:** 2026-08-26  
**Scope:** Group vertical before S3-B…N production edits  
**Figma:** file `TzLvwVwlPbeVB8ug1zB3GM` / section [`575:7980`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=575-7980) (MCP verified)  
**Rule:** Prefer REUSE/REFINE/COMPLETE. **No Group finance endpoints until this audit + screen matrix exist and SQL relationships are proven.**  
**Guardrails:** Server-authoritative finance; K1 membership before J; Pulse = read model; Equal split mandatory PASS; settlement = separate command or API_GAP.

---

## Executive summary

| Finding | Classification |
|---------|----------------|
| Empty + Create + 12 setup variants wired both platforms | **REUSE** |
| Invite mint live; redeem API live, shell/QR **unwired** | **REFACTOR** redeem wire |
| Active Group Pulse/Moments/Life/Memory = “later phases” stubs | **MISSING** |
| Live expense write is PERSONAL-only | **API_GAP** until Group command |
| `expense_share` / obligations / settlements **SQL ready**; no write APIs | **MISSING** mount after K1 |
| `expense_split` = CATEGORY/ITEM/TAX/TIP line items — **not** Equal/Percentage strategy | Do not confuse |
| No FX / baseCurrency / exchangeRate in V001–V029 | **Explicit rule:** no invented FX; per-currency recording only |
| Group facet projections return `payload: {}` | **API_GAP** / writers needed |
| Dual-router: product NON-RUNTIME; promote onto live only | **G1** |
| Goal/Community empty cards “Coming Soon” | **NOT_REQUIRED** S3 |

---

## Finance SQL relationships (proven — V007)

```text
finance.expense (domain_code GROUP|PERSONAL|BUSINESS)
  → finance.group_expense_context (paid_by_participant_id, moment_id)
  → finance.expense_share (participant_id, share_amount, share_percent?)
  → soft link → finance.participant_obligation (source_type=EXPENSE_SHARE, source_id)
  → finance.settlement + settlement_allocation (reduces obligation.settled_amount)
```

All participant FKs are composite `(participant_id, moment_id)` → `collaboration.moment_participant`.

**Identity chain:**

```text
Momentra userId (never Firebase UID as finance FK)
  → moment_participant.participant_id
  → membership (moment_id, status ACTIVE|…)
```

### `expense_split` vs strategy

| Object | Purpose |
|--------|---------|
| `finance.expense_split` | Line items: `CATEGORY\|ITEM\|TAX\|TIP\|OTHER` |
| `finance.expense_share` | Who owes what (`share_amount`, optional `share_percent`) |
| OpenAPI EQUAL/PERCENTAGE/EXACT/SHARES | **Strategy** — compute server-side into `expense_share`; no strategy enum column required |

### Settlement as separate command

SQL supports POST settlement + allocations updating `participant_obligation.settled_amount` **without** mutating `expense_share`. Historical shares stay immutable. Capability `SETTLEMENT_RECORD` is fail-closed in governance but **unmapped** to moment types and has **no write service** today → implement in S3-J or document **API_GAP** if governance mapping blocks.

### Multi-currency (S3-A decision)

**No** `baseCurrency` / `exchangeRate` / `baseAmount` in V001–V029.  
**S3 rule:** single currency per expense and per settlement; multiple currencies may be **recorded** on separate rows; **no cross-currency netting or invented FX**. Setup multi-currency chip = **DEFERRED**.

### Projection tables (V014)

`projection.group_finance_snapshot` / `group_finance_position` exist (moment + currency / participant). Pulse must **read** these (or honest empty), never scan expenses on GET.

---

## Backend classification

| Surface | Path | Status |
|---------|------|--------|
| Live router | `api/v1/router.ts` | **PASS** sole mount |
| Product router | `router-product.ts` | **NON-RUNTIME** |
| `GET /group/moments` | live | **REUSE** |
| Invite mint/preview/redeem | live | **REUSE** API; client redeem **MISSING** |
| `POST /moments` GROUP | live | **REUSE** |
| `POST …/expenses` | live | **PERSONAL only** — Group needs new command |
| Group pulse/life/memory/finance facets | product | **MISSING** live + empty payload |
| `POST …/participants` | product | **MISSING** live (K1) |
| `POST …/contributions` | product | **MISSING** live (J) |
| Group expense + shares + obligations | — | **MISSING** |
| Settlements | — | **MISSING** or **API_GAP** |
| Group activity keyset | — | **MISSING** |

---

## Android / iOS classification

| Surface | Status | Notes |
|---------|--------|-------|
| Empty tabs + Create | **REUSE** | `empty/group/*`, `GroupEmpty/*` |
| Setup ×12 | **REUSE** | Prefs LOCAL_ONLY |
| Active populated tabs | **MISSING** | “later phases” |
| Invite mint + share/QR | **REUSE** | |
| Invite redeem UI | **MISSING** | API exists |
| Group Quick Add / Expense | **MISSING** | Figma `Sheet / Add Expense` `581:12789` |
| Theme | **REUSE** | Group `#E8621A`; family accents |

---

## Split acceptance matrix (S3-J target)

| Case | Required |
|------|----------|
| Equal | **PASS** (mandatory) |
| Percentage / Exact / Shares | PASS or explicit **API_GAP** |
| Single participant / payer excluded / rounding | validation defined server-side |
| Zero / negative / total mismatch / non-member / cross-Moment | **reject** |

---

## Mandatory S3 PASS vs allowed gaps

**Must PASS:** create, membership, invite mint+redeem, Moment switch/isolation, Group expense, Equal split, expense shares, obligation derivation, Activity after expense, Pulse finance effect or honest projection, idempotency/auth/audit/event/outbox/hints.

**May classify:** Percentage/Exact/Shares, advanced settlements, Life/Memory secondary, polls/bookings, FX/netting, iOS `BLOCKED_ENVIRONMENT`.

---

## Effective implementation order

```text
A → B → C → D → E → F → G → H → I → K1 (membership) → J (finance) → K2 (invites lifecycle) → L → M → N → STOP
```

---

## Non-goals

S4–S9, V030, inventing FX, client-authoritative money, Pulse-as-calculator, remounting product router, Personal regression.
