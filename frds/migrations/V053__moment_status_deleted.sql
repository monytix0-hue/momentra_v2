BEGIN;

-- Product hard-delete (DELETE /v1/moments/:id) sets status=DELETED.
-- V002 only allowed DRAFT|ACTIVE|COMPLETED|CANCELLED|ARCHIVED, which caused
-- ck_moment__status violations surfaced as INFRASTRUCTURE_UNAVAILABLE.

ALTER TABLE core.moment DROP CONSTRAINT IF EXISTS ck_moment__status;

ALTER TABLE core.moment
  ADD CONSTRAINT ck_moment__status
  CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ARCHIVED', 'DELETED'));

COMMIT;
