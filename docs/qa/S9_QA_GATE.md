# S9-QA Gate — Master Product Certification

**Inserted before:** S9-L → S9-P  
**V030:** NOT RUN / UNTOUCHED  
**Stop line:** After Master Certification PASS → **STOP** — do not start S9-L until separately authorized.

## Board (Q0–Q8)

| Track | Scope | Status |
|-------|--------|--------|
| S0–S8 / S9 A–K | Prior blocks | PASS / CLOSED |
| **Q0** Foundation + authoritative inventory | catalog.json, correlation, qa:verify, MaestroIds, cert flows | **DONE** (inventory) |
| **Q1** Personal — P1–P4 | Independent Moment certifications | Journeys ready · execution PENDING |
| **Q2** Group — G01–G12 | Independent Moment certifications | Journeys ready · execution PENDING |
| **Q3** Business — B00 + B01–B03 | Company then Moments | Journeys ready · execution PENDING |
| **Q4** Isolation | User / Moment / Group / Company | Journey ready · execution PENDING |
| **Q5** Reliability / security | Double-submit, lifecycle, lock | Journey ready · execution PENDING |
| **Q6** Figma completeness + visual | Every frame classified | Classification DONE · EXACT PENDING screenshots |
| **Q7** Defect fix + rerun | P0/P1 loop | P1 open (MQA-A-001) |
| **Q8** Master report | docs/qa/* | Generated · Certification **OPEN** |
| S9-L → P | Chaos / performance | **BLOCKED** |

## Hard rule

**Nothing is PASS because a screen opened or a POST returned 201.**

Writable features require: UI → request → canonical DB → audit → domain event → outbox → projection/activity → resulting UI → persistence.

A representative Personal/Group/Business expense is **not** context certification.

## Acceptance (unblock S9-L)

See [MASTER_QA_SUMMARY.md](./MASTER_QA_SUMMARY.md) closeout checklist. All must be true:

- PERSONAL 4/4 executed + Quick Adds classified  
- GROUP 12/12 executed + Quick Adds classified + invite/redeem + finance correctness  
- BUSINESS Company + 3/3 Moments + Quick Adds classified  
- BACKEND critical writes verified (audit/event/outbox/projection/duplicates)  
- ISOLATION pass  
- FIGMA every relevant frame classified (no silent missing)  
- ANDROID physical critical certification executed  
- IOS suite implemented (`BLOCKED_ENVIRONMENT` OK for S9-L; Mac required before RC)  
- P0 = 0, P1 = 0, UNKNOWN = 0  

## Suite layout

```
.maestro/
  android/          # legacy smoke/critical (non-gating fast subset)
  ios/
  cert/
    catalog.json    # Q0 authoritative inventory
    android/        # Master Certification journeys
    ios/            # iOS mirror
  shared/
```

## Run

```powershell
# Q0 catalog + reports
cd backend\typescript
$env:QA_FIXTURES_ENABLED='true'
npm run qa:build-catalog
npm run qa:generate-flows
npm run qa:generate-reports

# Android Master Cert (physical preferred)
.\.maestro\run-qa-android.ps1 -Class cert -PrepareFixtures
# optional emulator scaffolding:
.\.maestro\run-qa-android.ps1 -Class cert -AllowEmulator

# Backend proof after a write
npm run qa:verify -- --run-id $env:MAESTRO_RUN_ID --expect personal-expense --alias QA_EMPTY --note MAESTRO-$env:MAESTRO_RUN_ID
```

```bash
# iOS (Mac only)
./.maestro/run-qa-ios.sh cert
```

Pin correlation before a write (debug Android):

```powershell
adb shell am broadcast -a com.example.momentra.QA_SET_CORRELATION --es correlation_id qa-20260827-personal-lifeops-expense-001 --es run_id $env:MAESTRO_RUN_ID
```

## Required artifacts

- [MASTER_QA_SUMMARY.md](./MASTER_QA_SUMMARY.md)
- [PERSONAL_MAESTRO_CERTIFICATION.md](./PERSONAL_MAESTRO_CERTIFICATION.md)
- [GROUP_MAESTRO_CERTIFICATION.md](./GROUP_MAESTRO_CERTIFICATION.md)
- [BUSINESS_MAESTRO_CERTIFICATION.md](./BUSINESS_MAESTRO_CERTIFICATION.md)
- [ANDROID_MAESTRO_REPORT.md](./ANDROID_MAESTRO_REPORT.md)
- [IOS_MAESTRO_REPORT.md](./IOS_MAESTRO_REPORT.md)
- [QUICK_ADD_COVERAGE_MATRIX.md](./QUICK_ADD_COVERAGE_MATRIX.md)
- [FIGMA_IMPLEMENTATION_COVERAGE.md](./FIGMA_IMPLEMENTATION_COVERAGE.md)
- [BACKEND_WRITE_VERIFICATION.md](./BACKEND_WRITE_VERIFICATION.md)
- [MAESTRO_PARITY_MATRIX.md](./MAESTRO_PARITY_MATRIX.md)
- [MAESTRO_DEFECT_REGISTER.md](./MAESTRO_DEFECT_REGISTER.md)

Machine evidence: `.maestro/reports/<RUN_ID>/`
