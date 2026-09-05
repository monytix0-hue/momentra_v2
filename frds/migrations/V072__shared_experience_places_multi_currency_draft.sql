BEGIN;

-- Trip / Shared Experience: multi-place destinations, multi-currency setup prefs, draft-friendly context status.

CREATE TABLE IF NOT EXISTS collaboration.shared_experience_place (
  place_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  moment_id UUID NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  label TEXT NOT NULL,
  start_at TIMESTAMPTZ NULL,
  end_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_shared_experience_place__moment
    FOREIGN KEY (moment_id) REFERENCES collaboration.shared_experience_context(moment_id) ON DELETE CASCADE,
  CONSTRAINT ck_shared_experience_place__time
    CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE INDEX IF NOT EXISTS ix_shared_experience_place__moment_sort
  ON collaboration.shared_experience_place (moment_id, sort_order);

ALTER TABLE collaboration.shared_experience_context
  DROP CONSTRAINT IF EXISTS ck_shared_experience_context__status;

ALTER TABLE collaboration.shared_experience_context
  ADD CONSTRAINT ck_shared_experience_context__status
  CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ARCHIVED'));

ALTER TABLE collaboration.shared_experience_context
  ADD COLUMN IF NOT EXISTS multi_currency_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE collaboration.shared_experience_context
  ADD COLUMN IF NOT EXISTS split_style TEXT NULL;

ALTER TABLE collaboration.shared_experience_context
  ADD COLUMN IF NOT EXISTS primary_goal TEXT NULL;

-- Setup prefs JSON for join approval / payment rhythm / update cadence (optional).
ALTER TABLE collaboration.shared_experience_context
  ADD COLUMN IF NOT EXISTS setup_preferences JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE collaboration.group_moment_context
  DROP CONSTRAINT IF EXISTS ck_group_moment_context__status;

-- Recreate status check if present under another name — group family contexts historically ACTIVE-only.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_schema = 'collaboration' AND constraint_name LIKE '%group_moment_context%status%'
  ) THEN
    NULL; -- already dropped above if named exactly
  END IF;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

-- Allow DRAFT on group_moment_context when constraint exists with known name from V004.
ALTER TABLE collaboration.group_moment_context
  DROP CONSTRAINT IF EXISTS ck_gmc__status;
ALTER TABLE collaboration.group_moment_context
  DROP CONSTRAINT IF EXISTS ck_group_moment_context_status;

-- Discover and drop status check on group_moment_context
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE n.nspname = 'collaboration'
      AND t.relname = 'group_moment_context'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) ILIKE '%status%'
  LOOP
    EXECUTE format('ALTER TABLE collaboration.group_moment_context DROP CONSTRAINT %I', r.conname);
  END LOOP;
  ALTER TABLE collaboration.group_moment_context
    ADD CONSTRAINT ck_group_moment_context__status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ARCHIVED'));
EXCEPTION WHEN duplicate_object THEN
  NULL;
END $$;

GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.shared_experience_place TO momentra_app;
GRANT SELECT ON collaboration.shared_experience_place TO momentra_projection_worker, momentra_analytics_worker;

COMMENT ON TABLE collaboration.shared_experience_place IS 'Ordered destinations/legs for Shared Experience (Trip) moments.';
COMMENT ON COLUMN collaboration.shared_experience_context.multi_currency_enabled IS 'Setup preference: allow multiple budget currencies.';
COMMENT ON COLUMN collaboration.shared_experience_context.split_style IS 'Default split style EQUAL|PERCENTAGE|EXACT|SHARES|POOLED.';

COMMIT;
