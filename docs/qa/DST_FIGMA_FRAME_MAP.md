# DST Figma Frame Map

Distribution · Metro Distribution frames in file `rknfN9EikSCdqmqXogR29o`.

Authority: [Moment Story v3 Unified Design + Backend Contract](../../reference/Momentra_Trading_Distribution_Moment_Story_v3_Unified_Design_Backend_Contract%20(1).docx) — Pulse / Moments / Life / Memory / Story share M01–M18; Dist changes labels and §06 emphasis only (UAT T17).

| Surface | Node | Frame | Code path (Android / iOS) |
|---|---|---|---|
| Pulse | `5:4890` | DST-PULSE-v12 · Metro Distribution | `DistributionActive`+`OperatingV12Screens` / `OperatingMomentHubView`+`OperatingV12Screens` |
| Moments | `5:5198` | DST-MOMENTS-v12 · Metro Distribution | same |
| Life | `5:5483` | DST-LIFE-v12 · Metro Distribution | same |
| Memory | `5:5777` | DST-MEMORY-v12 · Metro Distribution (+ Generate Story) | same → Story v3 `mode=distribution` |
| Hub | `5:6336` | DST-HUB-v11 · Metro Distribution | `DistributionQuickAddHub` / Dist hub in `OperatingMomentHubView` |

Tokens: `color.domain.distribution.screens.{pulse,moments,life,memory,quickAdd}` in `design/tokens/business.tokens.json`.

Contract §06 Dist emphasis:

- P1–P2: supplier procurement → receipt → channel deliveries → settlement
- P3: multi-buyer / channel mix and order execution
- P4: supplier payments + customer collections
- P5: PO, inbound receipts, multi-product outward
- P6–P7: principals/suppliers, buyers/channels, inbound/outbound queues

Story entry (Memory): Generate Business Story → options → preview → P1–P8 → save → share (same APIs as Trading).
