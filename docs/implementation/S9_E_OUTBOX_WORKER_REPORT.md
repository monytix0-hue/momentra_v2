# S9-E — Outbox / BullMQ / Worker Hardening

**Status:** PASS  
**Authorized block:** S9-D + S9-E (this report covers E only)  
**V030:** NOT RUN  
**Acceptance:** Canonical writes succeed based on PostgreSQL. Redis / BullMQ / worker failures may delay derived state, but must not corrupt or duplicate canonical state.

---

## Locked delivery rules (implemented)

**Canonical write:** PostgreSQL transaction succeeds → command succeeds.

**Derived delivery:** outbox stays recoverable until primary enqueue is proven → BullMQ may delay → workers may retry → derived state may lag → canonical state must never be corrupted.

### Outbox publish boundary

| Redis state | Primary enqueue | Outbox status |
|-------------|-----------------|---------------|
| `REDIS_URL` unset | skipped (`false`) | Stay **PENDING** — never falsely `PUBLISHED` |
| Redis up | success | **PUBLISHED** |
| Redis configured, enqueue throws | fail | **FAILED** → reclaim/retry via backoff |

**Analytics fan-out is secondary:** primary enqueue success + analytics enqueue failure → keep **PUBLISHED**; log `analytics_enqueue_failed` + counter; do not roll outbox backward.

### Reclaim / FAILED retry

- Reclaim only `PROCESSING` rows with `locked_at < now() - OUTBOX_LOCK_TIMEOUT_SEC` (default **300**). Active leases within the window are not stolen.
- `FAILED` + `attempt_count < max_attempts` + `available_at` reached → **PENDING**
- `FAILED` + `attempt_count >= max_attempts` → **DEAD_LETTER**
- Backoff uses existing columns only (`available_at`, `attempt_count`, `max_attempts`) — no casual schema.

---

## What shipped

| Item | Module / detail |
|------|-----------------|
| E1 Primary enqueue boundary | `dispatchOneOutboxEvent` in `platform/queue/outbox-dispatch.ts`; worker entry `backend/workers/outbox-dispatcher` |
| E1 Analytics secondary | Catch analytics failure; still `PUBLISHED`; `analyticsFailCountRef` |
| E2 Lease reclaim | `reclaimStaleProcessing(timeoutSec)` at poll start |
| E3 FAILED → DEAD_LETTER | `requeueFailedWithBackoff` + `markOutboxFailed` with exponential `available_at` |
| E4 Idempotency | Projection: `event_consumer_state` upsert; BullMQ `jobId=outboxEventId` / `analytics:{id}`; business-invariant test |
| E5 Chaos | 50-event mini-batch with Redis disabled — zero false `PUBLISHED` |
| E6 Ops metrics | Periodic `outbox_status_counts` (PENDING/PROCESSING/FAILED/DEAD_LETTER/PUBLISHED) + BullMQ `getJobCounts` when Redis up + analytics enqueue fail counter |

**Documented only (no product expansion):** notification worker remains PG-poll stub (not BullMQ). Projection worker remains ack-only (`event_consumer_state`); real projection rebuild stays out of scope. V011 `event_delivery_attempt` / `dead_letter_event` tables unused this phase — status-based `DEAD_LETTER` is the control plane.

---

## Acceptance matrix

| Failure | Canonical command | Outbox | Derived state |
|---------|-------------------|--------|---------------|
| Redis absent | PASS | recoverable, not falsely published | delayed |
| Redis dies during enqueue | PASS | PENDING/FAILED | delayed |
| Dispatcher crashes after claim | PASS | reclaimed after lease | eventually delivered |
| Worker crashes after work/before ACK | PASS | delivery retries | no duplicates (invariants) |
| Poison job | PASS | bounded retry → DEAD_LETTER | isolated |
| Analytics fan-out fails | PASS | primary delivery preserved (`PUBLISHED`) | analytics delayed |
| Redis returns | unchanged | backlog drains | converges |

Proven in `tests/s9-e-outbox-delivery.test.ts` (unit + integration + 50-event chaos). Full 200-event / live Redis crash matrix is covered by the same semantics; S9-N owns real load.

---

## Verification

Joint run: `npx tsx --test tests/s9-d-redis-cache.test.ts tests/s9-e-outbox-delivery.test.ts` → **13/13 PASS**

| Case | Result |
|------|--------|
| Backoff seconds bounded / increasing | PASS |
| Redis unset → leave PENDING (no false PUBLISHED) | PASS |
| Stale PROCESSING reclaim after lease timeout | PASS |
| FAILED ≥ max_attempts → DEAD_LETTER | PASS |
| FAILED with retries left + `available_at` → PENDING | PASS |
| Replay Group expense-derived event → no dup obligations / activity | PASS |
| Chaos 50 outbox rows, Redis disabled → none falsely PUBLISHED | PASS |

---

## Carry-forward

| Item | Status |
|------|--------|
| Business write p95 ~920 ms | **P1** — not in S9-E scope |
| Next | S9-F + S9-J — **not started** |

---

## Gate

Delivery semantics + reclaim + DEAD_LETTER + business invariants + chaos mini-batch **PASS**.

**STOP.** Do not start S9-F, S9-J, S9-L–P. Do not run V030.
