# Maestro ledger data (S9-QA-B)

## Source of truth

Excel ledgers in `docs/qa/ledgers/`:

- `Momentra_APK_3500_Certification_Ledger.xlsx`
- `Momentra_IOS_3500_Certification_Ledger.xlsx`

CSV exports under `.maestro/data/` are **generated** — regenerate after editing Excel:

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:sync-ledger-data
```

## Files

| Path | Rows | Purpose |
|------|-----:|---------|
| `android/maestro_input_3500.csv` | 3500 | Flat pack export |
| `ios/maestro_input_3500.csv` | 3500 | Flat pack export |
| `{platform}/joined_3500.csv` | 3500 | + catalog join columns |
| `{platform}/personal.csv` | ~1167 | Mode=Personal |
| `{platform}/group.csv` | ~1167 | Mode=Group |
| `{platform}/business.csv` | ~1166 | Mode=Business |
| `pilot/{platform}_pilot_150.csv` | 150 | Run_Batch B01–B03 (S9-QA-E) |

## Join columns added

`catalog_moment_id`, `catalog_quick_add`, `catalog_id`, `catalog_status`, `tile_maestro_id`, `amount_maestro_id`, `note_maestro_id`, `submit_maestro_id`, `split_maestro_id`, `category_value`, `correlation_note`, `join_status`, `join_notes`

## Architecture

`catalog.json` = capabilities · Excel = scenarios · CSV = Maestro exports · `qa:verify` = backend truth
