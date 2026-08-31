# Maestro E2E — Momentra (S9-QA Master Product Certification)

**Phase:** S9-QA Master Certification (before S9-L→P). See [`docs/qa/S9_QA_GATE.md`](../docs/qa/S9_QA_GATE.md).  
**V030:** UNTOUCHED. Smoke/critical under `android/` / `ios/` are a **non-gating** fast subset.

## Hard rule

Nothing is PASS because a screen opened or a POST returned 201. Writable features need UI → request → DB → audit → event → outbox → projection → UI → persist.

## Layout

```
.maestro/
├── android/          # legacy smoke/critical (non-gating)
├── ios/
├── cert/
│   ├── catalog.json  # Q0 authoritative inventory (UNKNOWN=0)
│   ├── android/      # Q1–Q5 Master Certification journeys
│   └── ios/          # iOS mirror (BLOCKED_ENVIRONMENT on Windows)
├── shared/           # test-data-contract + accessibility-ids
├── reports/<RUN_ID>/ # evidence package
├── run-qa-android.ps1
└── run-qa-ios.sh
```

## Classes

| Tag | Purpose |
|-----|---------|
| `smoke` | Fast launch → login → shell (non-gating) |
| `critical` | Representative money journeys (non-gating) |
| `isolation` | Legacy isolation |
| `cert` | **Master Certification** — P1–P4, G01–G12, B00–B03, Q4, Q5 |

## Q0 — Catalog first

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:build-catalog
npm run qa:generate-flows
npm run qa:generate-reports
```

## Run Android Master Cert (physical preferred)

```powershell
.\.maestro\run-qa-android.ps1 -Class cert -PrepareFixtures
.\.maestro\run-qa-android.ps1 -Class cert -AllowEmulator   # scaffolding only
```

Pin correlation before a write:

```powershell
adb shell am broadcast -a com.example.momentra.QA_SET_CORRELATION `
  --es correlation_id qa-20260827-personal-lifeops-expense-001 `
  --es run_id $env:MAESTRO_RUN_ID
```

Backend proof:

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"; $env:ALLOW_DEV_AUTH="1"
npm run qa:verify -- --run-id $env:MAESTRO_RUN_ID --expect personal-expense --alias QA_EMPTY
```

## Run iOS

```bash
./.maestro/run-qa-ios.sh cert   # Mac only; Windows → exit 2 BLOCKED_ENVIRONMENT
```

## Credentials

```bash
cp .maestro/.env.maestro.example .maestro/.env.maestro.local
```

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
npm run qa:prepare-fixtures
```

## Reports

See `docs/qa/MASTER_QA_SUMMARY.md`. Certification stays **OPEN** until every closeout checkbox is true. S9-L→P remains **BLOCKED**.
