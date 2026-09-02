# SP-008 / SP-009 pipeline evidence

Existing integration coverage:

- `tests/s9-e-outbox-delivery.test.ts` — outbox dispatch
- `tests/outbox-bullmq.test.ts` — worker queue wiring
- `tests/group-gx2-collab.test.ts` — write → SQL → projection read (life/pulse)

Representative domain writes propagate to read models in Group collab golden path.
