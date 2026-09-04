# Personal Phase 7 Pulse curation (PER-008 subset)

**Source pack:** `Momentra_Personal_Analytics_Phase7_Implementation_Pack_v1.xlsx`  
**Machine map:** [`PERSONAL_PHASE7_PULSE_METRIC_MAP.json`](PERSONAL_PHASE7_PULSE_METRIC_MAP.json)  
**SQL:** `frds/migrations/V058__personal_phase7_pulse_metric_freeze.sql`

## Decision

Keep the live Pulse UI + precision writers. Adopt Phase 7 as metric authority **only** for Pulse-surface scores that already appear on hero / axis / money widgets.

Do **not** implement ZERO_GAP fillers, Coming Soon / AI narrative PMETs, or Memory Emotional DNA from the pack.

## Honesty

Canonical facts remain source of truth. Precision writers update `projection.personal_pulse`. Analytics DET syncs those scores into `analytics.metric_current`. Clients continue to read Pulse DTO / `widget_payload`; `widget_payload.phase7Metrics` is an additive server bundle.

## Apply

1. Migrate through **V058**
2. Ensure `PERSONAL_ANALYTICS` consent for DET refresh
3. `POST /v1/analytics/refresh` (PERSONAL) syncs curated metrics from pulse projection
