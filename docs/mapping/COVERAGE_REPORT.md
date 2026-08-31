# Mapping coverage report

Release gate: domain sign-off requires all metrics below.

| Metric | Target | Current (implementation baseline) |
|---|---|---|
| Figma nodes classified | 100% | Personal/Group/Business/Auth shells mapped in plan |
| Writable forms field-mapped | 100% | See [FIELD_CONTRACTS.md](./FIELD_CONTRACTS.md) |
| Executable actions mapped | 100% | See [CAPABILITY_MATRIX.md](./CAPABILITY_MATRIX.md) |
| SCHEMA_GAP | 0 | V031–V034 applied; custom_type_label + company_location + shared.poll live |
| Unresolved routes | 0 | OpenAPI `/v1` + Retrofit `ApiService.kt` aligned |
| STUB_PHASE1 / NOT_SUPPORTED | explicit | Events/Vendor Ops setup families; iOS Hello World (out of scope) |

**Implementation baseline (2026-08-19):** Backend builds; V031–V033 applied to Supabase; V034 validation passed; V030 PRE-RC blockers logged only. APK `assembleDebug` succeeds. Personal/Group/Business shells wired to `/v1`.

## Status taxonomy

| Status | Meaning |
|---|---|
| LOCAL_ONLY | No backend |
| PROJECTION_READ | GET projection |
| COMMAND_WRITE | Governed mutation |
| EXTERNAL_SERVICE | Firebase, Storage, FCM |
| STUB_PHASE1 | Visible, non-live |
| NOT_SUPPORTED | Hidden/disabled |
| SCHEMA_GAP | No canonical target |

## Domain summary

- **Auth:** LOCAL_ONLY + EXTERNAL_SERVICE (Firebase)
- **Personal:** PROJECTION_READ + COMMAND_WRITE (Moment, Expense, Goal, observations, lifestyle, relationships)
- **Group:** PROJECTION_READ + COMMAND_WRITE + shared Poll
- **Business:** PROJECTION_READ + COMMAND_WRITE + locations + shared Poll
- **Circle/Life360:** PROJECTION_READ (`GET /v1/life360`)
