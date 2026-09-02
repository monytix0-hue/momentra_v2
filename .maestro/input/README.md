# Maestro input flows (S9-QA-D)

Generated from joined ledger CSVs. Do not hand-edit YAML.

## Hard gate

**S9-QA-E** pilot (`pilot/*.yaml`) must PASS the five-element verification chain before running `personal|group|business` shards or `stress/`.

## Regenerate

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:sync-ledger-data
npm run qa:generate-input-flows
```

## Run (Android)

```powershell
.\.maestro\run-qa-android.ps1 -Class pilot -PrepareFixtures
.\.maestro\run-qa-android.ps1 -Class input -Context personal -Shard 1
.\.maestro\run-qa-android.ps1 -Class stress -Shard 1
```

## Run (iOS — macOS only)

```bash
./.maestro/run-qa-ios.sh pilot
./.maestro/run-qa-ios.sh input personal 1
```
