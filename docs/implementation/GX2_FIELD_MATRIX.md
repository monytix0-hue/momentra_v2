# GX2 Field Matrix

**Authority:** Figma `575:7980` + Excel masters.  
**Reclassified:** 2026-08-30 against live `router.ts` (see [`THREE_LAYER_JOIN.md`](./THREE_LAYER_JOIN.md)).

**Rows:** 4259 widgets. **Statuses:** no UNKNOWN.

**Full CSV:** [`GX2_FIELD_MATRIX.csv`](GX2_FIELD_MATRIX.csv)

## Status rollup

| Status | Count | Meaning |
|---|---:|---|
| `WIRED` | 933 | Figma widget ↔ client ↔ live API ↔ table.column |
| `CLIENT_FIX` | 1386 | Live API + SQL exist; client binding/nav/state incomplete |
| `API_GAP` | 139 | Table exists; live router missing read and/or write |
| `SCHEMA_GAP` | 261 | Widget needs column/table that does not exist |
| `FIGMA_GAP` | 15 | Designed; no product/schema contract |
| `LOCAL_ONLY` | 1502 | Device/chrome only; must not pretend to persist |
| `DEFERRED` | 23 | Explicitly out of join program |

## Live API overrides (current)

Group collab mounts on live router — planning-items, bookings, polls, updates, purchase-items, residents, memories — plus finance/settlements/invites. Rows that previously counted those as `API_GAP` are now `CLIENT_FIX`.

Still `API_GAP`: group_vendor, attendance, living_rule, shared_asset, maintenance_record, ownership_record, delivery_handover.

## Column contract

```text
moment_family → moment_subtype → figma_screen → figma_widget → field
→ table_column → api_read → api_write → android_binding → ios_binding
→ projection → gx2_status
```
