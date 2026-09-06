-- V070: planning item category/location/priority + group update urgency

ALTER TABLE collaboration.planning_item
  ADD COLUMN IF NOT EXISTS category_code TEXT,
  ADD COLUMN IF NOT EXISTS location TEXT,
  ADD COLUMN IF NOT EXISTS priority_code TEXT;

ALTER TABLE collaboration.planning_item
  DROP CONSTRAINT IF EXISTS ck_planning_item__priority_code;

ALTER TABLE collaboration.planning_item
  ADD CONSTRAINT ck_planning_item__priority_code
  CHECK (priority_code IS NULL OR priority_code IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE collaboration.group_update
  ADD COLUMN IF NOT EXISTS urgency_code TEXT NOT NULL DEFAULT 'NORMAL';

ALTER TABLE collaboration.group_update
  DROP CONSTRAINT IF EXISTS ck_group_update__urgency_code;

ALTER TABLE collaboration.group_update
  ADD CONSTRAINT ck_group_update__urgency_code
  CHECK (urgency_code IN ('NORMAL', 'URGENT'));
