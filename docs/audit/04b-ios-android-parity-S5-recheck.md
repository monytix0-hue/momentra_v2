# iOS ↔ Android API parity — S5 re-check

Date: 2026-10-06 (Asia/Calcutta) · Baseline: `04-ios-android-parity.csv` (generated 2026-09-03)

## Method

- Android: every Retrofit `@GET/@POST/@PATCH/@PUT/@DELETE("…")` in `apk/app/src/main/**/ApiService.kt` → 237 method+path pairs.
- iOS: every `path: "…"` call in `momentra/momentra/**/*.swift` with its HTTP verb (`authorizedGet/Post/Patch/Delete` or explicit `method:`), plus direct `URLRequest` callers → 234 pairs.
- Path params normalised to `{}`; query strings dropped.

## Result vs the 95 `ANDROID_ONLY` rows in the baseline

| Outcome | Count | Notes |
|---|---|---|
| Now called on iOS (closed since baseline) | 94 | incl. consents grant/withdraw, devices, media uploads, invites, companies/vendors, all moment sub-resources, approvals decide, analytics refresh, telemetry (`Analytics/BackendTelemetry.swift`). |
| Still missing on iOS | 1 | `DELETE /v1/moments/{id}` — **Android no longer calls it either**. Not added: product rule is *no deletes of user data*. |

## New Android-only routes (not in the baseline)

| Route | Android usage | S5 action |
|---|---|---|
| `POST /v1/moments/{id}/stories` | Moment Story viewer Generate / Retry | **Closed** — `APIClient.createMomentStory` + Generate (not started) / Retry (failed) in `MomentStoryViewerView.swift`. Additive new story version; never deletes READY stories. |
| `GET /v1/me/notifications/metrics` | Declared in `ApiService.kt`, no UI caller | Deferred (no user-facing gap). |

## Non-API parity gaps still open

| Gap | Platform | Notes |
|---|---|---|
| Phone (SMS) sign-in | iOS missing | Android has Firebase phone auth; iOS needs APNs-backed phone auth + reCAPTCHA fallback. Not S5. |
| Sign in with Apple | Android gated | Client done, hidden behind `APPLE_SIGNIN_ENABLED` until Firebase Services ID/key are set — `docs/legal/APPLE_SIGNIN_ANDROID.md`. |
| Developer / API server override screen | iOS only | Debug tool; Android uses `local.properties`. Intentional. |

The baseline CSV is left unchanged as the historical audit artefact; this file supersedes it for parity status.
