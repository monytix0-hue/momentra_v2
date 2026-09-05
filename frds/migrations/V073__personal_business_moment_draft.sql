-- Allow DRAFT on personal/business moment contexts and setup rows (parity with Group V072).

ALTER TABLE personal.personal_moment_context
  DROP CONSTRAINT IF EXISTS ck_personal_moment_context__status;
ALTER TABLE personal.personal_moment_context
  ADD CONSTRAINT ck_personal_moment_context__status
  CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','CANCELLED','ARCHIVED'));

ALTER TABLE business.business_moment_context
  DROP CONSTRAINT IF EXISTS ck_business_moment_context__status;
ALTER TABLE business.business_moment_context
  ADD CONSTRAINT ck_business_moment_context__status
  CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','CANCELLED','ARCHIVED'));

ALTER TABLE personal.life_system_setup
  DROP CONSTRAINT IF EXISTS ck_life_system_setup__status;
ALTER TABLE personal.life_system_setup
  ADD CONSTRAINT ck_life_system_setup__status
  CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED'));

ALTER TABLE business.business_system_setup
  DROP CONSTRAINT IF EXISTS ck_business_system_setup__status;
ALTER TABLE business.business_system_setup
  ADD CONSTRAINT ck_business_system_setup__status
  CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED'));

COMMENT ON COLUMN personal.personal_moment_context.status IS
  'DRAFT = setup saved but not activated; ACTIVE = live system.';
COMMENT ON COLUMN business.business_moment_context.status IS
  'DRAFT = setup saved but not activated; ACTIVE = live system.';
