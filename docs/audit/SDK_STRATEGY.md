# Mobile SDK Strategy (R7 / SUPP-009)

**Decision:** Keep hand-maintained `APIClient.swift` and `ApiService.kt` as V1 authoritative clients with CI drift checks against OpenAPI.

## Rationale

- Generated stubs under `momentra/OpenAPI/Generated/` and `apk/openapi-generated/` are not compiled into app targets today.
- Rewiring build graphs for both platforms mid-remediation adds release risk without closing functional gaps.
- Hand clients already cover ~95% of live routes; drift is measurable via `npm run openapi:coverage`.

## Enforcement

1. `npm run openapi:build` regenerates `endpoint-inventory.json` from `build-openapi.ts`.
2. `npm run audit:deployment` produces `03-openapi-backend-reconciliation.csv`.
3. CI gate: zero `IMPLEMENTED_UNDOCUMENTED` for V1 command routes; `CONTRACT_ONLY` must match router absence.
4. New routes: add to `build-openapi.ts`, `router.ts`, both clients, and integration test in same PR.

## Future (post-RC)

Evaluate adopting generated clients behind a facade once OpenAPI ↔ router convergence hits 0 unexplained drift (R8 exit gate).
