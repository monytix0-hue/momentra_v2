# Notification soak + calibration playbook

Wave 2 signal types are **frozen**. Do not start Wave 3 until two calibration cycles complete.

## Versions

Bump these in [`decision-versions.ts`](../../src/platform/notifications/decision-versions.ts) whenever thresholds, cadence rules, or priority maps change:

- `NOTIFICATION_DECISION_VERSION` — engine shape (currently `v2.1`)
- `NOTIFICATION_POLICY_VERSION` — soak policy label (currently `2026-09-soak-01`)

Always compare outcomes **within the same `policy_version`**.

## Observability surfaces

| Surface | Purpose |
|---|---|
| `platform.notification_decision_audit` | Append-only: what the engine decided (never update with opens/actions) |
| `platform.user_notification` | Delivered inbox + `read_at` (open proxy) |
| `scripts/qa/notification-soak/*.sql` | Offline calibration queries |
| `GET /admin/api/notifications/soak?windowDays=14` | Same rollups as JSON |

## Attribution windows (useful-action proxies)

| Family | Window |
|---|---|
| Settlement / balance | 72h |
| Poll vote | 48h |
| Task completion | 72h |
| Approval action | 48h |
| Bill action | 24–72h |
| Goal / budget reset | 7d |

Core success metric: **useful actions / notification** (and / active user-day). Open rate is secondary.

## Two-cycle exit gate

```
Cycle 1: observe → adjust thresholds/priorities → bump policy_version
Cycle 2: verify reduced noise + preserved useful-action rate
Then: freeze thresholds → allow Wave 3
```

### Cycle checklist

1. Run `01`–`09` SQL (or admin `/soak`) for the current `policy_version`
2. Flag noisy codes (high volume, low useful-action rate)
3. Flag quiet codes (almost never fire but high action when they do)
4. Check hysteresis `reemit_lt_1h` / `reemit_lt_6h`
5. Split finance vs social/tasks p50/p90 volume
6. Confirm digests cut immediate pushes without material action degradation
7. Adjust Wave 1 knobs only (priority, cadence, signal thresholds) — no new signal types
8. Bump `NOTIFICATION_POLICY_VERSION` and re-soak

## Exit criteria (all required)

- Per-`explanationCode` useful-action vs noise is known
- Digests reduce pushes without material useful-action degradation
- Hysteresis clear→re-emit `<1h` / `<6h` within acceptable bounds
- Finance vs social/tasks daily volume bands acceptable
- Outcomes compared within the same `policy_version`
- **Two calibration cycles completed** and thresholds frozen

## Out of scope until exit

- New Wave 2 signal types
- Wave 3 adaptive personalization
- Wave 4 AI wording
- Mutating decision audit rows with open/action
