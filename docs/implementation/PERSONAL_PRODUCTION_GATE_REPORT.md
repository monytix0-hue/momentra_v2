# Personal production gate

Date: 1 October 2026. API host `https://api.momentra.tech`. Android emulator `emulator-5556`. Life-count commit `07eacee` on `origin/main`.

**Verdict: NOT PRODUCTION PASS**

Android exercised the deployed API. iOS runtime was not run on this host. One offline Pulse path showed an empty day instead of today's logs. That is enough to withhold production sign-off.

## Deploy

`07eacee` skips `source = MASTER_EXPENSE` and `activity_code = LIFESTYLE_EXPERIENCE` in the Life period total, family counts, and highlights. Money queries and the dimension writer were not changed.

`GET /health/live` and `GET /health/ready` returned `{"status":"ok"}` (HTTP 200). Unauthenticated `GET /v1/me` returned 401. `X-Dev-Firebase-Uid` on `GET /v1/me` returned 401.

The GitHub deployment object for `07eacee` is the Vercel site, not the API. There is no repo webhook. The API behavior below is the proof the skip is what the app called: before the new Food spend, Lifestyle had already fallen from 2 activities to 1, which is the earlier Food mirror leaving the count.

## Food spend and explicit Experience

Life before ProdFood1111: Everyday 5, Future 3, Lifestyle 1, People 1. Spent ₹12,222, all on Everyday.

ProdFood1111, ₹1,111, category Food, shared experience Self:

- Everyday 6 (+1)
- Lifestyle 1 (+0)
- Future 3, People 1
- Week sum 10 → 11
- Spent ₹13,333, Everyday ₹13,333, no Lifestyle rupee line

ProdExperience1111 from Add → More families → Experience:

- Lifestyle 2 (+1)
- Everyday stayed 6

Void of ProdFood1111 removed that activity and returned the month line to ₹12,222. A later offline Food probe, OfflineProbe ₹1, synced after reconnect: Everyday 6, Lifestyle still 2. That second Food row also did not increment Lifestyle.

## Android checklist

| Check | Result |
| --- | --- |
| Pulse nudge | PASS. With logs today, the card says "Log recovery if you want it on today's record." The word busy is absent. Zero-log omission remains `PersonalPulseNudgeTest`. |
| Add save and refresh | PASS. Food and Experience both appeared on Life after confirm. |
| Moments card opens the spend | PASS. ProdFood1111 opened Edit Transaction for that Food spend, not the Walk recovery. |
| Life attribution and money | PASS. See the Food and Experience rows above. |
| Memory | PASS for honesty of sections. October shows real highlights (QAworked, QAdelay, QAincident, QAissue, ProdExperience1111). No Relive, pattern, or Then→Now block. Memory's "12 activities" did not match the Life week sum of 11 at that moment. |
| Edit | The rename was typed. Save did not close. The sheet later showed `INFRASTRUCTURE_UNAVAILABLE`. The new title was not confirmed on the server. |
| Delete | PASS. Delete Transaction → Void removed ProdFood1111 from Life and from the month money line. |
| Force-stop and relaunch | PASS. Pulse returned to YOUR DAY with the recovery nudge. About 10s until that text, including UI dumps. |
| Family lock | PASS. The Lifestyle chip on Life did not move Pulse off YOUR DAY. |
| Business leakage | PASS. The visible Business pulse did not show ProdFood1111, ProdExperience1111, GapCheck1111, or GateCheck1111. |
| One confirm, one activity | PASS. ProdFood1111 added one Everyday activity. |
| Offline save | PASS for honesty of the message: "Saved on this device. Changes will sync when you reconnect." After reconnect, OfflineProbe counted once. |
| Offline Pulse | FAIL. With wifi and mobile data off, Pulse showed "Nothing logged yet today" even though today already had logs. Unavailable was presented as zero. |
| 5xx on the editor | The update surfaced `INFRASTRUCTURE_UNAVAILABLE` on the sheet and did not show a successful save. Life was not replaced by an empty state. |
| Emulator timings | Cold launch to YOUR DAY about 13s. Add hub about 3s. Confirm until the sheet closed about 3s. Moments, Life, and Memory each about 3s. These include accessibility dumps. Not a physical-device performance pass. |

## iOS

Code parity is in the tree and was not run.

- Everyday nudge body is now "Log recovery if you want it on today's record."
- `visibleNudge(todayLogCount:)` omits the Everyday card at zero logs and never uses a body that claims busy. People still say "before the next busy stretch."
- `PersonalPulseActiveView` renders that helper.
- `PersonalPulseNudgeTests` covers the helper. This host has no Xcode, so the tests were not executed.

iOS device rows stay unchecked: false nudge absent, clustered spend opens the spend, Food mirror does not increment Lifestyle, Add refresh, relaunch, Memory/Relive.

## Backend and security this run

| Check | Result |
| --- | --- |
| Activity counts and family attribution on the deployed API | PASS for the Food spend and the Experience. |
| Life summary matches those counts | PASS on the Life screen. |
| Memory sectionQuality field | Not fetched as JSON. The screen did not invent Relive or a pattern. |
| Idempotent replay of the same key | Not issued. One confirm produced one activity. |
| Unauthenticated and dev-header calls | PASS, both 401 this run. |
| Revoked Firebase token replay | Not repeated this run. |
| Second user cannot read this user's Personal rows | Not run. |
| Stale write versus a newer save | Not proven. The edit update returned infrastructure unavailable and did not save. No version token was added. |
| Production logs free of tokens, PIN, and expense payloads | Not run. No production log drain from this host. |

## Sign-off

**Personal — PRODUCTION PASS** is not signed.

Android runtime is not a full pass because offline Pulse showed an empty day. iOS runtime is blocked. Auth isolation, idempotency replay, stale-write rejection, and production log review were not all passed in this run. Family attribution for an inferred Food mirror did pass on the deployed API.
