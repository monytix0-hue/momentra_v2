# Momentra 3500 Certification Ledgers (S9-QA-B)

**Source of truth for scenarios + expected outcomes.**  
Capabilities remain in [`.maestro/input-catalog/catalog.json`](../../.maestro/input-catalog/catalog.json).

## Files

| File | Platform | Role |
|------|----------|------|
| [`Momentra_APK_3500_Certification_Ledger.xlsx`](Momentra_APK_3500_Certification_Ledger.xlsx) | Android | Excel ledger (12 sheets) |
| [`Momentra_IOS_3500_Certification_Ledger.xlsx`](Momentra_IOS_3500_Certification_Ledger.xlsx) | iOS | Excel ledger (12 sheets) |
| `Momentra_*_3500_Test_Pack.zip` | — | Original packs (xlsx + flat CSV) |

## Sheets (each workbook)

README · Config · QuickAdd_Catalog · Members · Transactions · Group_Splits · Expected_Summary · Reconciliation · Maestro_Run · Performance_Log · Test_Cases · Dashboard

## Export → Maestro

Flat CSVs and joined shards live under [`.maestro/data/`](../../.maestro/data/):

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:sync-ledger-data
```

Produces:

- `android|ios/maestro_input_3500.csv` (raw 3500)
- `android|ios/joined_3500.csv` (+ catalog join)
- `android|ios/{personal,group,business}.csv`
- `pilot/{android,ios}_pilot_150.csv` (batches B01–B03 for S9-QA-E)
- `join-report.json`

Join map: [`.maestro/input-catalog/ledger-join-map.json`](../../.maestro/input-catalog/ledger-join-map.json)

## Counts (per platform)

| Mode | Rows |
|------|-----:|
| Personal | 1,167 |
| Group | 1,167 |
| Business | 1,166 |
| **Total** | **3,500** |

Pilot: 150 rows/platform · Stress band: `STRESS_3001_3500`

## Architecture

```
Excel ledger (this folder)
    ↓ export / sync
.maestro/data CSVs
    ↓ S9-QA-D generate-input-flows
Maestro YAML
    ↓ device run
qa:verify + Excel Reconciliation
```
