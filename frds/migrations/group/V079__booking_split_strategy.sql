-- V079: Persist split_strategy on collaboration.booking (linked expense remains canonical for shares).

BEGIN;

DO $$
BEGIN
  IF to_regclass('collaboration.booking') IS NULL THEN
    RAISE EXCEPTION 'V079 requires collaboration.booking';
  END IF;
END $$;

ALTER TABLE collaboration.booking
  ADD COLUMN IF NOT EXISTS split_strategy TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_booking__split_strategy'
  ) THEN
    ALTER TABLE collaboration.booking
      ADD CONSTRAINT ck_booking__split_strategy
      CHECK (
        split_strategy IS NULL
        OR split_strategy IN ('EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'POOLED')
      );
  END IF;
END $$;

-- Backfill from linked group expense context when present.
UPDATE collaboration.booking b
SET split_strategy = g.split_strategy
FROM finance.group_expense_context g
WHERE b.linked_expense_id = g.expense_id
  AND b.split_strategy IS NULL
  AND g.split_strategy IS NOT NULL;

COMMENT ON COLUMN collaboration.booking.split_strategy IS
  'Split strategy used when creating linked_expense_id; shares live on finance.expense_share.';

COMMIT;
