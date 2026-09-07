# Momentra surface doctrine

Contract between the four major surfaces. Mode engines (Personal / Group / Business) may use different mathematics; they share meaning.

| Surface | Core question |
|---------|---------------|
| **Pulse** | What needs my attention **now**? |
| **Moments** | What is happening and what am I **managing**? |
| **Life** | What **pattern** is emerging across areas? |
| **Memory** | What happened that is **worth remembering**? |

## Architecture (not a roll-up chain)

```
Current events/data
  → Moments
  → derived family state / observations
  → Pulse (NOW)

Broader accumulated evidence
  → Life (PATTERN)

Meaningful completed events / milestones
  → Memory (HISTORY)
```

Do **not** treat Life as a mechanical copy of Pulse, or Memory as every raw transaction.

## Surface time orientation

| Surface | Time orientation | Meaning |
|---------|------------------|---------|
| **Pulse** | Now / recent | What needs attention now? |
| **Moments** | Active | What am I currently managing? |
| **Life** | Medium / long-term | What pattern is emerging? |
| **Memory** | Historical | What happened that matters? |

Example (Relationships): Pulse = limited meaningful connection recently; Life = consistency improved over 3 months; Memory = Anniversary trip — Goa — August 2026. Three different pieces of intelligence, not three screens of the same score.

## Mode engines

Personal, Group, and Business Pulse implementations differ by design:

- **Personal** — family health axes blended into overall wellbeing (equal-weight of available families).
- **Group** — finance positions + coordination counters (ledger is SoT for money).
- **Business** — runway / team / ops heuristics.

Keep different engines; keep one semantic doctrine: **Pulse = what matters now**.

## Time windows (intent)

- **Pulse** prioritizes current / recent state for attention.
- **Life** may use broader pattern windows once scoring exists.
- **Memory** owns long-history curated records.

Exact window tables evolve; this doc is the product contract, not a formula freeze.

## Business `open_risk_count` (placeholder)

`projection.business_pulse.open_risk_count` is written as `0` until a real risk engine exists. It is **not** selected into Business Pulse API payloads and must **not** be exposed in OpenAPI or client DTOs/UI. Never show a false “Open Risks: 0”. Compute properly or keep hidden.

## Beta refinement backlog (not blocking)

Ship and improve during beta; do not reopen closed Pulse correctness fixes for these:

- Personal Pulse time horizons (lifetime counts → recency-aware “now”)
- Personal family scores: recency × quality × consistency × meaning/completion (not volume alone)
- Personal inactivity / freshness decay
- Personal Life overall pattern score (broader than Pulse; keep overall `null` until ready)
- Group Life provisional threshold tuning against real users
- Business overall health composite beyond runway-first
- Business `attention_count` recompute from actionable conditions
- Curated automatic Memory (not raw event dumping)
- Client/server label mapping contract (Stable / Attention / …)

## Golden scenarios

Functional proof lives in [GOLDEN_SCENARIOS.md](./GOLDEN_SCENARIOS.md) and `backend/typescript/tests/golden-scenarios.test.ts`: Input → Moments → Pulse → Life → Memory.
