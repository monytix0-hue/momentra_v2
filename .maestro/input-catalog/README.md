# S9-QA-A Input Catalog

Generated: `2026-09-02T07:15:56.850Z`

## Role

- **This catalog** = capabilities (what can be tested)
- **Excel (S9-QA-B)** = scenarios + expected math (3,500 × 2 platforms) — [`docs/qa/ledgers/`](../../docs/qa/ledgers/)
- **Join map** = [`ledger-join-map.json`](ledger-join-map.json) (ledger category → hub tile)
- **Platform CSVs** = [`.maestro/data/`](../data/) exports (`npm run qa:sync-ledger-data`)
- **qa:verify** = backend truth

## Summary

| Context | Count |
|---------|------:|
| PERSONAL | 23 |
| GROUP | 50 |
| BUSINESS | 34 |
| **Total** | **107** |

### By status

- **ACTIVE:** 97
- **DEFERRED:** 5
- **BROKEN:** 1
- **CAPABILITY_GAP:** 4

## Rebuild

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:build-input-catalog
npm run qa:sync-ledger-data
```

Do not hand-edit `catalog.json`. Fix sources and regenerate.
