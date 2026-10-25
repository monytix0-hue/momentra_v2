BEGIN;

-- One posted operating event per client idempotency key on a moment.
-- A repeated save returns the original row instead of a second spend or result.

CREATE UNIQUE INDEX ix_operating_event__idempotency
    ON business.operating_event (moment_id, (payload->>'idempotencyKey'))
    WHERE payload->>'idempotencyKey' IS NOT NULL
      AND payload->>'idempotencyKey' <> '';

COMMIT;
