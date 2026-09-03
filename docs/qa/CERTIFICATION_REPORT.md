# Momentra S9-QA Certification Report

Living report for Maestro 3,500 certification. Updated by `qa:generate-certification-report` and `qa:run-certification-waves`.

## Gates

| Gate | Criterion | Status |
|------|-----------|--------|
| UI | ACTIVE Quick Adds open, accept input, submit | PENDING_DEVICE |
| Data | Backend records exist exactly once | PENDING_DEVICE |
| Math | Balances / splits reconcile with Excel | OFFLINE_PASS (group splits + signed amounts); Actual_Results PENDING_DEVICE |
| Projection | Pulse / Activity / finance match | PENDING_DEVICE |
| Isolation | No cross-account leakage | PENDING_DEVICE |
| Idempotency | No duplicate money movements | PENDING_DEVICE |
| Performance | No regression 100 to 3500 | PENDING_DEVICE |
| Parity | Same business rule despite platform inputs | PENDING_DEVICE |
| Recovery | Relaunch retains state | PENDING_DEVICE |

## Hard gate (S9-QA-E)

Pilot (150/platform) must PASS the full verification chain before F/G/H/I scale runs.

## Wave orchestration status

Generated: `pending-refresh`

iOS device runs BLOCKED_ENVIRONMENT on Windows; Android requires USB device or -AllowEmulator

| Wave | Artifact | Device execution |
|------|----------|------------------|
| A-D | READY | n/a (codegen) |
| E Pilot | READY | PENDING_DEVICE |
| F Personal | READY | BLOCKED_UNTIL_E |
| G Group | READY | BLOCKED_UNTIL_E |
| H Business | READY | BLOCKED_UNTIL_E |
| I Stress 3500 | READY | BLOCKED_UNTIL_FGH |
| J Reconcile | READY | OFFLINE_MATH_READY |

## Device environment (this host)

- Android: `adb devices` empty - cannot execute Maestro pilot
- iOS: Windows - `BLOCKED_ENVIRONMENT` (run on macOS via `run-qa-ios.sh` or record via `run-qa-ios.ps1`)

### Execute when devices are available

```powershell
.\.maestro\run-qa-android.ps1 -Class pilot -PrepareFixtures
.\.maestro\run-qa-android.ps1 -Class input -Context personal -Shard 1
.\.maestro\run-qa-android.ps1 -Class stress -Shard 1
```

```bash
./.maestro/run-qa-ios.sh pilot
```

## Run log

| When (UTC) | Platform | Class | Run ID | Exit | Status |
|------------|----------|-------|--------|------|--------|
| 2026-09-02T08:10:35.670Z | android | pilot | pending-device | 1 | FAIL |
| 2026-09-02T08:10:36.987Z | ios | pilot | blocked-env | 2 | BLOCKED_ENVIRONMENT |

| 2026-09-02T13:00:29.200Z | android | pilot | 20260902182027 | 1 | FAIL |


| 2026-09-02T13:10:03.808Z | android | pilot | 20260902183111 | 1 | FAIL |


| 2026-09-02T13:27:21.491Z | android | pilot | 20260902184404 | 1 | FAIL |


| 2026-09-02T14:11:16.202Z | android | pilot | 20260902190051 | 1 | FAIL |


| 2026-09-02T16:10:00.740Z | android | pilot | 20260902202604 | 1 | FAIL |


| 2026-09-02T16:33:10.366Z | android | input | 20260902214000 | 0 | PASS |


| 2026-09-02T16:48:42.833Z | android | input | 20260902220310 | 0 | PASS |


| 2026-09-02T17:04:16.595Z | android | input | 20260902221842 | 0 | PASS |


| 2026-09-02T17:19:37.462Z | android | input | 20260902223416 | 0 | PASS |


| 2026-09-02T17:35:26.992Z | android | input | 20260902224937 | 0 | PASS |


| 2026-09-02T17:51:29.866Z | android | input | 20260902230527 | 0 | PASS |


| 2026-09-02T18:06:52.149Z | android | input | 20260902232129 | 0 | PASS |


| 2026-09-02T18:22:16.503Z | android | input | 20260902233652 | 0 | PASS |


| 2026-09-02T18:37:42.455Z | android | input | 20260902235216 | 0 | PASS |


| 2026-09-02T18:53:08.158Z | android | input | 20260903000742 | 0 | PASS |


| 2026-09-02T19:09:13.227Z | android | input | 20260903002308 | 0 | PASS |


| 2026-09-02T19:25:01.283Z | android | input | 20260903003913 | 0 | PASS |


| 2026-09-02T19:40:33.743Z | android | input | 20260903005501 | 0 | PASS |


| 2026-09-02T19:56:01.338Z | android | input | 20260903011033 | 0 | PASS |


| 2026-09-02T20:11:33.607Z | android | input | 20260903012601 | 0 | PASS |


| 2026-09-02T20:26:57.523Z | android | input | 20260903014133 | 0 | PASS |


| 2026-09-02T20:43:18.656Z | android | input | 20260903015657 | 0 | PASS |


| 2026-09-02T20:59:07.569Z | android | input | 20260903021318 | 0 | PASS |


| 2026-09-02T21:14:40.364Z | android | input | 20260903022907 | 0 | PASS |


| 2026-09-03T05:48:37.365Z | android | input | 20260903024440 | 0 | PASS |


## Join summary

- Android: {"JOINED":2614,"JOINED_REVIEW":866,"SKIP_CAPABILITY_GAP":20}
- iOS: {"JOINED":2614,"JOINED_REVIEW":866,"SKIP_CAPABILITY_GAP":20}
- Pilot rows/platform: 150

## Flow manifest

- Generated: 2026-09-02T16:12:11.034Z
- Android writable: 3480
- iOS writable: 3480

## Ledger

- APK: `docs/qa/ledgers/Momentra_APK_3500_Certification_Ledger.xlsx`
- iOS: `docs/qa/ledgers/Momentra_IOS_3500_Certification_Ledger.xlsx`
- Reconciliation sheet: fill Actual_Results after `qa:verify` batches

## Cross-device sync (S9-QA-J)

Separate from platform isolation. Use shared Group/Business workspace with one APK + one iOS device after per-platform PASS.

## Defects

See `docs/qa/MAESTRO_DEFECT_REGISTER.md`.
