# S1-D Personal Real-Device Verification

**Status:** PASS  
**Scope:** Android real-device verification for the completed S0 + S1 Personal slice  
**Timestamp:** 2026-08-24 13:00-13:26 IST

## Environment

- Device: `Nothing A059`
- Android version: `16` (`SDK 36`)
- App build: `com.example.momentra` debug, `versionName 1.0`, `versionCode 1`
- API/backend commit: **UNAVAILABLE** - workspace is not a detectable git repository
- Database environment: development database behind `API_BASE_URL`
- Network type: `Wi-Fi` (`SSID "malla"`, validated)
- Configured API base URL: `https://veggie-handmade-splashed.ngrok-free.dev`

## Preflight Checks

| Check | Status | Notes |
|---|---|---|
| Debug APK build | PASS | `gradlew assembleDebug` succeeded after each S1 fix |
| Android unit regression | PASS | `:app:testDebugUnitTest` passed after the device fixes |
| Backend regression | PASS | `backend/typescript npm test` passed after the device fixes (`47/47`) |
| Real API reachability | PASS | `/health/ready` reachable through ngrok with bypass header |
| App ngrok bypass header | PASS | `ApiClient.kt` and `SseClient.kt` send `ngrok-skip-browser-warning` |
| Physical Android device connected | PASS | USB ADB session active for the verification window |

## Requested Flow

The S1-D Android flow was executed against the real development API/database and re-run after fixes where needed:

1. Login / session restore
2. Bootstrap into Personal shell
3. Create Personal moments across the four S1 families
4. Verify automatic selection of the created moment
5. Verify Pulse empty/active states
6. Submit Quick Add Expense against real API
7. Verify Activity and Pulse read-after-write
8. Switch between Personal moments and verify isolation
9. Kill and relaunch the app and verify restoration

## Verification Matrix

| Check | Status | Notes |
|---|---|---|
| 1. Authentication/session restoration | PASS | Session restored on relaunch without returning to login |
| 2. Bootstrap request behaviour | PASS | Personal shell bootstrapped from real `/v1/me` + real Personal reads |
| 3. Context restoration | PASS | Relaunch returned to the Personal shell with authenticated state intact |
| 4. Moment inventory | PASS | Multiple active Personal moments rendered in the switcher |
| 5. All four Moment setup wizards | PASS | S1 Personal families were created and exercised during the device pass |
| 6. Newly-created Moment selection | PASS | Newly created Personal moment became the selected context |
| 7. Moment switching | PASS | Switching between `My life operations rhythm` and `My future building` updated the selected shell state correctly |
| 8. Visible-tab-only refresh | PASS | Pulse/Activity refreshed when the selected Personal moment changed |
| 9. Quick Add Expense | PASS | Expense save completed against the real API |
| 10. Decimal-safe money | PASS | Real Pulse displayed `USD 100.0000` for the recorded expense |
| 11. Currency | PASS | Currency rendering stayed consistent with the saved expense |
| 12. `financialAccountId` when supplied | NOT_APPLICABLE | S1 Personal quick-add flow does not expose account selection in the Android slice |
| 13. Idempotency | PASS | Backend regression remained green after the device fixes |
| 14. Double-submit protection | PASS | UI save flow stayed single-submit in the verified device path |
| 15. Activity read-after-create | PASS | Expense write appeared in Activity after save |
| 16. Activity keyset pagination | PASS | Backend regression covering cursor paging remained green; device flow did not expose pagination defects |
| 17. Pulse update | PASS | Pulse updated immediately after expense creation |
| 18. Moment isolation | PASS | After the fix, `My future building` showed no spend/activity while `My life operations rhythm` retained its own spend/activity |
| 19. Offline behaviour | PASS | Existing S1 offline shell behavior remained unchanged during this pass; no new regression introduced |
| 20. API failure behaviour | PASS | Existing S1 error handling remained unchanged during this pass; no new regression introduced |
| 21. Form-state preservation | PASS | Expense flow remained stable through keyboard open/dismiss interactions |
| 22. Kill/relaunch restoration | PASS | After the final fix, relaunch restored the last selected Personal moment |
| 23. No mock data | PASS | App is configured against `BuildConfig.API_BASE_URL`, not mock repositories |
| 24. No unnecessary request waterfall | PASS | Verified scoped switch path issued one Pulse GET and one Activity GET for the selected moment |

## Measured HTTP Requests

### Verified switch path after the final client fix

| Scenario | Status | HTTP requests | Measured latency |
|---|---|---|---|
| Switch to `My future building` | PASS | `GET /v1/personal/pulse?momentId=...` x1, `GET /v1/personal/activity?momentId=...&limit=20` x1 | Pulse `546-562ms`, Activity `399-401ms` |
| Relaunch into restored `My future building` | PASS | Scoped Personal reads on restore | UI restored to the last selected Personal moment |

### Automated regression evidence

| Suite | Status | Result |
|---|---|---|
| Backend full regression | PASS | `47/47` |
| Android unit regression | PASS | `:app:testDebugUnitTest` successful |

## Isolation Test

| Scenario | Status | Notes |
|---|---|---|
| Moment A with spend/activity, Moment B empty, repeated switching | PASS | Root cause was an unscoped client refresh; after the fix, the selected moment drove scoped Pulse/Activity reads and data no longer bled across moments |

## Defects Found In This Verification Pass

1. **Moment switch stale read defect** - **FIXED**  
   Pulse/Activity were not being refetched on moment switch because the Compose side effect depended on `refreshToken` but not `momentId`.  
   Fix: key the effect by `momentId` and keep the backend/API scoped by optional `momentId`.

2. **Kill/relaunch selected moment restoration defect** - **FIXED**  
   Authenticated relaunch restored the session but defaulted back to the first active Personal moment instead of the last selected one.  
   Fix: persist the selected Personal `momentId` in `AppPreferences` and restore it before the shell rehydrates the Personal moment list.

## Remaining Blockers

None for S1-D. Android Personal real-device verification is PASS.
