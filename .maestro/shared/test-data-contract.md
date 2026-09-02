# S9-QA Maestro — Test Data Contract

Shared behavioral contract for Android (`com.example.momentra`) and iOS (`resolvingpoint.momentra`).
Platform YAML may differ; **outcomes must match**.

## Five-element Quick Add verification chain (frozen — S9-QA-A)

Every ACTIVE Quick Add certification row must declare and prove all five:

| # | Element | What must be true |
|---|---------|-------------------|
| 1 | **UI tile** | Hub tile opens via Maestro ID (preferred) or stable label |
| 2 | **Writable fields** | All required fields filled; optional fields may vary |
| 3 | **API / backend write** | Canonical POST succeeds; DB/domain row exists **exactly once** |
| 4 | **Expected calculation** | Balance / split sum / approval state matches Excel `Expected_Results` |
| 5 | **Post-write surfaces** | Pulse / Activity / Memory / Finance update with correlation key |

```
Input → Maestro UI action → API persistence → calculated expectation → Pulse/Activity/Memory/Finance verification
```

**Never PASS on "form submitted" alone.**

### Catalog status taxonomy (no fake PASSes)

| Status | Meaning | Report treatment |
|--------|---------|------------------|
| `ACTIVE` | Runnable end-to-end on both platforms (or platform-tagged) | Must PASS verification chain |
| `CAPABILITY_GAP` | Tile present but V019/backend denies or moment-gated off | **SKIP** with reason |
| `LOCAL_ONLY` | UI-only; no canonical persistence | **SKIP** |
| `DEFERRED` | Product decision (e.g. Reflect) | **SKIP** |
| `BROKEN` | Should work; currently fails | **FAIL** (not skip) |

Capability catalog (capabilities + fields): [`.maestro/input-catalog/catalog.json`](../input-catalog/catalog.json).  
Scenario rows + expected math: Excel ledgers in [`docs/qa/ledgers/`](../../docs/qa/ledgers/) (S9-QA-B).  
Platform CSVs: [`.maestro/data/`](../data/) (generated via `npm run qa:sync-ledger-data`).

### Architecture roles

| Artifact | Role |
|----------|------|
| `input-catalog/catalog.json` | Defines **capabilities** (what can be tested) |
| `docs/qa/ledgers/*_3500_Certification_Ledger.xlsx` | Defines **scenarios + expected outcomes** (3,500/platform) |
| `input-catalog/ledger-join-map.json` | Maps ledger Moment/Semantic_Type → catalog hub tiles |
| `.maestro/data/{android,ios}/*.csv` | Generated exports for Maestro |
| Maestro YAML | Executes UI writes (S9-QA-D+) |
| `qa:verify` | Checks backend truth |
| Excel `Reconciliation` | Expected vs actual |

### Hard gate (S9-QA-E)

No 1,100-row certification (S9-QA-F/G/H) until the **300-run pilot** passes this chain on both platforms.

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

Platform-isolated aliases (`QA_APK_*` / `QA_IOS_*`) are seeded by **S9-QA-C** (`npm run qa:prepare-fixtures`). During platform certification Android and iOS never share these accounts. Cross-device sync uses a separate shared workspace test in S9-QA-J.

| Alias | Platform | Bootstrap |
|-------|----------|-----------|
| `QA_APK_PERSONAL` / `QA_IOS_PERSONAL` | Android / iOS | Empty Personal inventory (Maestro creates P1–P4) |
| `QA_APK_GROUP_OWNER` (+ MEMBER) | Android | Group A + invite; **no expenses** |
| `QA_IOS_GROUP_OWNER` (+ MEMBER) | iOS | Separate Group A; **no expenses** |
| `QA_APK_BUSINESS_OWNER` (+ MEMBER) | Android | Company A; no moments yet |
| `QA_IOS_BUSINESS_OWNER` (+ MEMBER) | iOS | Separate Company A; no moments yet |
| `*_GROUP_OUTSIDER` / `*_BUSINESS_OUTSIDER` | Both | Isolation denial |

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

Correlation key pattern for scale runs (S9-QA-F+): `MAESTRO-{platform}-{moment}-{seq}` e.g. `MAESTRO-A-P1-000428`.

## Test classes

| Class | Tag | Cadence | Gate |
|-------|-----|---------|------|
| Smoke | `smoke` | Every build | Block release |
| Critical path | `critical` | Every build / PR | Block release |
| Full regression | `regression` | RC / nightly | Block RC |
| Destructive / isolation | `isolation` | RC / nightly | **P0 if fail** |
| Pilot | `pilot` | After S9-QA-D | **Hard gate before 1100** |
| Input cert | `input` | After pilot PASS | Block RC |
| Stress | `stress` | After F/G/H PASS | Performance gate |

## Assertion rule

Never end a flow only with “tap Submit”. Assert:

- success UI dismisses / sheet closes
- Activity contains the entry (note / amount)
- Pulse / positions updated where applicable
- second user / other moment / other company does **not** see leaked data
- for Group splits: Σ shares = total; net balances ≈ 0 within currency precision

Backend QA verify helper after Maestro write (exactly one expense / event / obligation).

## Failure artifacts

On fail, Maestro debug output must retain:

- screenshot
- hierarchy / logs
- flow name, platform, app build
- backend commit (CI env)
- test user alias

## Status vocabulary (reports)

`PASS` | `FAIL` | `BLOCKED_ENVIRONMENT` | `PRODUCT_GAP` | `BACKEND_GAP` | `FLAKY` | `NOT_REQUIRED` | `PENDING_FIXTURES` | `SKIP`

No release-critical journey may ship as `FLAKY`.
