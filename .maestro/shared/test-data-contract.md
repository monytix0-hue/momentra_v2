# S9-QA Maestro — Test Data Contract

Shared behavioral contract for Android (`com.example.momentra`) and iOS (`resolvingpoint.momentra`).
Platform YAML may differ; **outcomes must match**.

## Environments

| Key | Purpose |
|-----|---------|
| `MAESTRO_EMAIL` / `MAESTRO_PASSWORD` | Default smoke user (prefer `QA_MULTI_CONTEXT`) |
| `QA_EMPTY_*` | Authenticated, no Personal/Group/Business inventory |
| `QA_PERSONAL_*` | ≥1 Personal moments; no test expense yet |
| `QA_GROUP_OWNER_*` / `QA_GROUP_MEMBER_*` | Group A owner / member |
| `QA_GROUP_OUTSIDER_*` | Owns Group B only (no Group A) |
| `QA_BUSINESS_OWNER_*` / `QA_BUSINESS_MEMBER_*` | Company A owner / restricted member |
| `QA_BUSINESS_OUTSIDER_*` | Owns Company B only |
| `QA_MULTI_CONTEXT_*` | Personal + Group + 2 companies |
| `MAESTRO_RUN_ID` | Unique run token for finance notes |
| `QA_FIXTURES_ENABLED` | Required `true` for reset/seed scripts |

Copy `.env.maestro.example` → `.env.maestro.local` (gitignored). Never commit secrets.

## Dedicated identities (deterministic fixtures)

| Alias | Expected bootstrap | Release critical |
|-------|--------------------|------------------|
| `QA_EMPTY` | 0 moments, 0 companies | Smoke / isolation |
| `QA_PERSONAL` | ≥3 Personal moments | Critical |
| `QA_GROUP_OWNER` | Owns Group A; invite minted; **no expenses** | Critical |
| `QA_GROUP_MEMBER` | Member of Group A | Critical / isolation |
| `QA_GROUP_OUTSIDER` | Owns Group B only | Isolation |
| `QA_BUSINESS_OWNER` | Owns Company A + ops moment | Critical |
| `QA_BUSINESS_MEMBER` | MEMBER on Company A | Critical / isolation |
| `QA_BUSINESS_OUTSIDER` | Owns Company B only | Isolation |
| `QA_MULTI_CONTEXT` | Personal + Group + 2 companies | Smoke |

### Backend fixture lifecycle

```powershell
cd backend\typescript
$env:QA_FIXTURES_ENABLED="true"
$env:ALLOW_DEV_AUTH="1"
npm run qa:prepare-fixtures   # reset → seed → updates .env.maestro.local
```

Guards (hard refuse):
- `NODE_ENV=production`
- missing `QA_FIXTURES_ENABLED=true`
- non-local `DATABASE_URL` heuristics

Seed **prerequisites only**. The transaction under test is still created by Maestro.

## Finance idempotency

```
amount: 137.41 (or locale equivalent)
description / note: MAESTRO-${MAESTRO_RUN_ID}
```

Assert Activity/Pulse contains that exact note. Reruns generate a new `MAESTRO_RUN_ID`.

## Test classes

| Class | Tag | Cadence | Gate |
|-------|-----|---------|------|
| Smoke | `smoke` | Every build | Block release |
| Critical path | `critical` | Every build / PR | Block release |
| Full regression | `regression` | RC / nightly | Block RC |
| Destructive / isolation | `isolation` | RC / nightly | **P0 if fail** |

## Assertion rule

Never end a flow only with “tap Submit”. Assert:

- success UI dismisses / sheet closes
- Activity contains the entry (note / amount)
- Pulse / positions updated where applicable
- second user / other moment / other company does **not** see leaked data

Optional: backend QA verify helper after Maestro write (exactly one expense / event / obligation).

## Failure artifacts

On fail, Maestro debug output must retain:

- screenshot
- hierarchy / logs
- flow name, platform, app build
- backend commit (CI env)
- test user alias

## Status vocabulary (reports)

`PASS` | `FAIL` | `BLOCKED_ENVIRONMENT` | `PRODUCT_GAP` | `BACKEND_GAP` | `FLAKY` | `NOT_REQUIRED` | `PENDING_FIXTURES`

No release-critical journey may ship as `FLAKY`.
