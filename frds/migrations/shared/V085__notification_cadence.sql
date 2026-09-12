BEGIN;

-- Per-participant notification cadence (Wave 1). Kept in sync with notify_on_changes for mute.
ALTER TABLE collaboration.moment_participant
  ADD COLUMN IF NOT EXISTS notification_cadence TEXT NOT NULL DEFAULT 'ALL';

ALTER TABLE collaboration.moment_participant
  DROP CONSTRAINT IF EXISTS ck_moment_participant__notification_cadence;

ALTER TABLE collaboration.moment_participant
  ADD CONSTRAINT ck_moment_participant__notification_cadence
  CHECK (notification_cadence IN ('ALL', 'IMPORTANT', 'DIGEST_ONLY', 'MUTED'));

COMMENT ON COLUMN collaboration.moment_participant.notification_cadence IS
  'ALL | IMPORTANT (HIGH only immediate) | DIGEST_ONLY | MUTED. MUTED implies notify_on_changes=false.';

-- Backfill mute compatibility
UPDATE collaboration.moment_participant
SET notification_cadence = 'MUTED'
WHERE notify_on_changes = false
  AND notification_cadence = 'ALL';

COMMIT;
