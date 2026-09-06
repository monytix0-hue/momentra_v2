BEGIN;

-- Verified phone for invite stub matching on redeem.
ALTER TABLE core.user_profile
    ADD COLUMN IF NOT EXISTS phone TEXT NULL;

CREATE INDEX IF NOT EXISTS ix_user_profile__phone_digits
    ON core.user_profile (
        regexp_replace(COALESCE(phone, ''), '[^0-9]', '', 'g')
    )
    WHERE phone IS NOT NULL AND phone <> '';

-- Persist split strategy (including POOLED household spend without IOUs).
ALTER TABLE finance.group_expense_context
    ADD COLUMN IF NOT EXISTS split_strategy TEXT NOT NULL DEFAULT 'EQUAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_group_expense_context__split_strategy'
    ) THEN
        ALTER TABLE finance.group_expense_context
            ADD CONSTRAINT ck_group_expense_context__split_strategy
            CHECK (split_strategy IN ('EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'POOLED'));
    END IF;
END $$;

COMMIT;
