# S9-J — Native Android / iOS Performance

**Status:** PASS (instrumentation + source parity; Android device timings NOT_MEASURED; iOS runtime BLOCKED_ENVIRONMENT on Windows)  
**Authorized block:** S9-F + S9-J  
**Companion:** [`S9_F_PERSONAL_PERFORMANCE_REPORT.md`](./S9_F_PERSONAL_PERFORMANCE_REPORT.md)  
**V030:** NOT RUN  

---

## Acceptance rule

A backend endpoint can be “fast” while the app still feels slow. Measure **user-visible ready** (shell paint, tab content after moment/tab switch), not only API latency.

API half of visible-ready (S9-F harness):

| User-visible proxy | p95 |
|--------------------|-----|
| Pulse+activity sequential | ~335 ms |
| Pulse+activity **parallel** (shipped on Android; already on iOS) | ~223 ms (p50) |
| Moment switch parallel tab fetch | **223 ms** (≤500 target) |

---

## What shipped

### Android (`apk/`)

| Mark / surface | Location |
|----------------|----------|
| `ShellPerf` marks store | `ui/shell/perf/ShellPerf.kt` |
| Cold launch / recreate | `MainActivity` → `cold_launch` / `warm_process_recreate` |
| Foreground resume | `foreground_resume` |
| TTCS cache paint | `AppShellViewModel.bindIdentity` → `ttcs_cache_paint` + existing `ttcsMs` |
| Context / tab / moment switch | `context_switch`, `tab_switch`, `moment_switch` |
| Quick Add presentation | `quick_add_presentation` on CREATE |
| Scoped refresh | `scoped_refresh_personal` |
| Personal waterfall fix | Parallel `getPulse` + `getActivity` across Personal Pulse / Moments / Memory family screens |

### iOS (`momentra/`) — source parity

| Mark / surface | Location |
|----------------|----------|
| `ShellPerf` | `Shell/ShellPerf.swift` |
| TTCS / context / tab / moment / Quick Add / scoped refresh | `AppShellModel.swift` (mirrors Android) |
| Pulse loads | Already `async let` parallel (unchanged) |

### Tests

- `ShellPerfTest` — mark recording  
- `AppShellViewModelTest.momentSwitchBumpsScopedRefreshWithoutClearingIdentity` — scoped refresh invariant  
- Existing shell unit suite green with ShellPerf wired  

---

## Required metrics matrix

| Metric | Android | iOS |
|--------|---------|-----|
| TTCS | Instrumented; **shell TTCS needs signed-in session** (Google auth gate on emulator) | Instrumented; **BLOCKED_ENVIRONMENT** (Windows host) |
| Cold launch | **Measured on emulator** `Pixel_10_Pro_XL`: `am start -W` TotalTime **6317 ms** (COLD); `ShellPerf cold_launch→setContent` **120–220 ms** | Source parity only |
| Warm / resume | `foreground_resume` logged on launcher return | Source parity only |
| Foreground resume | Instrumented + emulator confirmed | Source parity only |
| Context switch | Instrumented | Instrumented |
| Moment switch | Instrumented + scoped refresh test | Instrumented |
| Tab switch | Instrumented | Instrumented |
| Quick Add presentation | Instrumented (local) | Instrumented (local) |
| Scroll / jank | Not collected (no device) — mark for S9-L/N device pass | BLOCKED_ENVIRONMENT |
| Memory footprint | Not collected (no device) | BLOCKED_ENVIRONMENT |
| Network waterfall | Client: Personal pulse+activity now parallel; API timings in S9-F JSON | iOS already parallel |

---

## Poor network / offline / resume

| Behavior | Status |
|----------|--------|
| BootstrapCache SWR | Unchanged — cache paints first (`CACHED`), network refresh merges |
| Offline with cache | Shell remains on cached inventory; tab fetches fail gracefully (existing) |
| Resume | `foreground_resume` mark; App Lock re-check unchanged |
| No full-context reload on tab/moment | Proven by unit test + token-only refresh |

---

## Explicit non-goals honored

- No new product features  
- No FCM / notification expansion  
- No reopen of S9-B/C/G/H  
- iOS runtime not forced on Windows  

---

## Carry-forward

| Item | Owner |
|------|-------|
| Collect device TTCS / launch / jank / memory on Android emulator or hardware | Later device pass (S9-L/N or dedicated) |
| iOS runtime numbers | Mac / Xcode host |
| Personal expense API ~1.4 s preferred ≤1 s | Shared P1 with Business write |

---

## Gate

Instrumentation + Personal visible-ready waterfall fix + scoped-refresh invariant **PASS**.  
Device runtime numbers: **NOT_MEASURED** / **BLOCKED_ENVIRONMENT** (not FAIL).

**STOP.** Do not start S9-L–P. Do not run V030.
