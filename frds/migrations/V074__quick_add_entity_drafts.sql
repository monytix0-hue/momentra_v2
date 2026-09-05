-- Quick Add entity drafts: allow DRAFT status on entities that lacked it.

ALTER TABLE collaboration.planning_item
  DROP CONSTRAINT IF EXISTS ck_planning_item__status;
ALTER TABLE collaboration.planning_item
  ADD CONSTRAINT ck_planning_item__status
  CHECK (status IN ('DRAFT','OPEN','IN_PROGRESS','DONE','CANCELLED'));

ALTER TABLE memory.memory
  DROP CONSTRAINT IF EXISTS ck_memory__status;
ALTER TABLE memory.memory
  ADD CONSTRAINT ck_memory__status
  CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED','INVALIDATED','DELETED'));

ALTER TABLE personal.life_operation_observation
  DROP CONSTRAINT IF EXISTS ck_life_operation_observation__status;
ALTER TABLE personal.life_operation_observation
  ADD CONSTRAINT ck_life_operation_observation__status
  CHECK (status IN ('DRAFT','ACTIVE','CORRECTED','VOIDED'));

ALTER TABLE collaboration.booking
  DROP CONSTRAINT IF EXISTS ck_booking__status;
ALTER TABLE collaboration.booking
  ADD CONSTRAINT ck_booking__status
  CHECK (status IN ('DRAFT','PLANNED','BOOKED','CONFIRMED','CANCELLED','COMPLETED'));

COMMENT ON CONSTRAINT ck_planning_item__status ON collaboration.planning_item IS
  'DRAFT = Quick Add save-draft; OPEN+ = live planning.';
