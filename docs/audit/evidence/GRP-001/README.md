# GRP-001 closure evidence

## Vertical slice

1. **UI:** Group poll Quick Add creates poll (existing)
2. **API:** `POST /v1/moments/:id/polls`, `GET /v1/polls/:id`, `POST /v1/polls/:id/votes`, `POST /v1/polls/:id/close`
3. **DB:** `shared.poll`, `shared.poll_option`, `shared.poll_vote`
4. **Events:** `PollCreated`, `PollVoted`, `PollClosed` via outbox
5. **Test:** `backend/typescript/tests/group-gx2-collab.test.ts` — vote persist + post-close 409

## Status

CLOSED — poll lifecycle implemented; `shared.poll_vote` in use.
