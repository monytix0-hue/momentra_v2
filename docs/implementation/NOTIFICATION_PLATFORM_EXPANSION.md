# Notification platform expansion (1–11)

## What shipped

| # | Capability | Where |
|---|------------|--------|
| 1 | Scheduler reminders | `backend/workers/scheduler` → `modules/notifications/reminders.ts` |
| 2 | Richer FCM data | `momentId`, `deepLink`, `actorDisplayName`, `category`, `eventName` |
| 3 | Category prefs | `core.user_profile.notification_categories` + PATCH `/me/notification-preferences` |
| 4 | Invalid token revoke | notification-worker on FCM not-registered |
| 5 | Quiet hours / digest | profile quiet hours + `digest_enabled`; HIGH priority bypasses |
| 6 | In-app inbox | `platform.user_notification` + `/me/notifications` |
| 7 | Targeted recipients | `targetUserIds` / assignee / approval organizers |
| 8 | Setup reminder prefs | weekly personal prefs + group `reminder_preferences` |
| 9 | Delivery metrics | `platform.notification_delivery_stats` + `/me/notifications/metrics` |
| 10 | Priority queue | BullMQ `priority` on `momentra-notifications` |
| 11 | Verification | `tests/notification-allowlist.test.ts`, `tests/notification-pipeline.test.ts` |

Migration: `frds/migrations/V071__notification_platform_expansion.sql`

## Live E2E checklist

1. Apply V071; set `DATABASE_URL`, `REDIS_URL`, Firebase Admin creds.
2. Run `outbox-dispatcher`, `notification-worker`, `scheduler`.
3. `POST /v1/me/devices` with a real FCM token.
4. Record a group expense as user A → user B gets push + inbox row + `notification_dispatch`.
5. `PATCH /me/notification-preferences` `{ "digestEnabled": true }` → next low/normal event is inbox-only until scheduler digest flush.
6. Enable `remindWeekly` in personal setup → scheduler emits `WeeklyReminder` once per ISO week.
7. Tap push on iOS/Android → opens `momentra://moment/{id}` via deep-link stores.

## Commands

```bash
cd backend/typescript
node --import tsx --test tests/notification-allowlist.test.ts tests/notification-pipeline.test.ts
npm run worker:scheduler
npm run worker:notification
```
