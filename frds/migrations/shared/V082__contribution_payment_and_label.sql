-- V082: Contribution label, payment method, and PENDING status for Shared Experience contributions.

ALTER TABLE finance.contribution
  ADD COLUMN IF NOT EXISTS label TEXT,
  ADD COLUMN IF NOT EXISTS payment_method_code TEXT;

ALTER TABLE finance.contribution
  DROP CONSTRAINT IF EXISTS ck_contribution__status;

ALTER TABLE finance.contribution
  ADD CONSTRAINT ck_contribution__status
    CHECK (status IN ('RECORDED', 'PENDING', 'REVERSED', 'VOIDED'));

ALTER TABLE finance.contribution
  DROP CONSTRAINT IF EXISTS ck_contribution__payment_method_code;

ALTER TABLE finance.contribution
  ADD CONSTRAINT ck_contribution__payment_method_code
    CHECK (
      payment_method_code IS NULL
      OR payment_method_code IN ('UPI', 'BANK_TRANSFER', 'CASH', 'CARD')
    );

ALTER TABLE finance.contribution
  DROP CONSTRAINT IF EXISTS ck_contribution__label_len;

ALTER TABLE finance.contribution
  ADD CONSTRAINT ck_contribution__label_len
    CHECK (label IS NULL OR char_length(label) <= 200);

CREATE INDEX IF NOT EXISTS ix_contribution__moment_contributed_at
  ON finance.contribution (moment_id, contributed_at DESC);
