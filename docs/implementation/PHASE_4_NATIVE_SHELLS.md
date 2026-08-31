# Phase 4 — Native Authenticated Application Shells

## Starting state

Android (`apk/`) already had Compose, Firebase Auth, splash/onboarding/login, hand-maintained Retrofit `ApiService`/`ApiClient`, and a brand placeholder `HomeScreen`. Generated OpenAPI client remains under `apk/openapi-generated/` (transport artifact; app uses Retrofit wrappers).

iOS (`momentra/`) already had SwiftUI, Firebase Auth + Google Sign-In, splash/onboarding/login, `APIClient.bootstrapMe()`, and `HomePlaceholderView`. Generated client under `API/Generated/`. Xcode uses synchronized root groups — new `Shell/` files are auto-included.

## Android architecture

```
MainActivity → AppRoot
  AuthViewModel (AuthPhase + ShellIdentity via MeRepository.getMe)
  AppShellViewModel (context / company / tab / content state)
  AppShellScreen
    MomentraTopBar (+ Business company selector)
    ContextSwitcher (Personal | Group | Business | Circle)
    MomentSwitcher (only when designed / non-setup)
    destination empty/loading/error panels
    ShellBottomNavigation (Pulse | Moments | Create | Life | Memory)
```

Transport: Retrofit `ApiService` + centralized `ApiResultException` / `mapHttpFailure`.  
401: OkHttp `Authenticator` forces one Firebase `getIdToken(true)` retry.  
403: shell `Forbidden` content — session preserved.

## iOS architecture

```
momentraApp → AuthOnlyView
  AuthViewModel (AuthPhase + ShellIdentity)
  AppShellModel + AppShellView
    same chrome hierarchy as Android
```

Transport: `APIClient` authorized GET helpers with one forced-token retry on 401.  
Shell loads: Personal = empty chrome only; Group = `GET /v1/group/moments?limit=1`; Business = `GET /v1/companies`; Circle = `GET /v1/life360` (deferred when absent).

## Auth lifecycle

```
Launch → Firebase session check
  → RestoringSession | SignedOut
  → GET /v1/me (only bootstrap call)
  → Authenticated + shell paint
```

No Pulse/Moments/Life/Memory/Finance domain loads during bootstrap.

## Application state

| Global | Feature-scoped (not in Phase 4) |
|--------|----------------------------------|
| AuthPhase, ShellIdentity | Pulse cards, Moments lists |
| Selected AppContext | Life/Memory datasets |
| Selected company (Business) | Finance balances |
| Bottom tab (+ per-context preserve) | Create commands |

## Context switching

Switching context updates selection, cancels in-flight shell loads (`generation` guard), clears invalid Moment selection, does **not** re-run Firebase or `/v1/me`.

## Business hierarchy

```
Context = Business
  → Company selector (real companies only)
  → Moment Switcher only when a company exists (empty/deferred; no fake Moments)
```

## Navigation

Canonical bottom destinations: Pulse, Moments, Create, Life, Memory.  
Create = deferred empty shell (Phase 5 owns MOMENT.CREATE).

## Loading / error / offline

- Content-level loading inside shell chrome
- Offline panel with Retry
- 403 → No access (still authenticated)
- 401 after refresh failure → SessionExpired + logout

## Figma parity

See `PHASE_4_FIGMA_SHELL_MAPPING.md`. Shell chrome matched from Business Top Bar / Context Switcher nodes; feature interiors deferred.

## API gaps (unchanged)

- Settlement command
- Budget command
- Vendor command
- Circle full CRUD (Life360 read only)
- Poll vote/close CONTRACT_ONLY
- FIGMA_API_GAP — Budget / Create product actions without commands
- ONBOARDING_PERSISTENCE_GAP — still local prefs (no V035)

## Performance baseline

Measured qualitatively on Android debug assemble path (Windows agent):

| Metric | Notes |
|--------|--------|
| App launch → first frame | Splash covers startup; no domain waterfall |
| Authenticated → shell | After `/v1/me` only |
| `/v1/me` duration | Network-bound; single call |
| Context switch → visible | Local state + at most one scoped list call |

Startup waterfall check: **PASS** (Firebase → `/v1/me` → shell).

## Tests

### Android

```text
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Result: **22/22** unit tests PASS; `assembleDebug` PASS.

Covers: API error mapping, AuthPhase/nav enums, AppShellViewModel context/company/empty/logout/isolation/403.

### iOS

```text
xcodebuild test -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16'
```

`ShellModelTests.swift` covers error mapping, labels, logout, company→moment clear (6 tests authored).  
**Note:** Full Xcode build/test cannot run on this Windows agent — **iOS build = FAIL (environment)** until verified on macOS.
