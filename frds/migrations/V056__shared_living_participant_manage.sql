-- Map PARTICIPANT_MANAGE onto Shared Living moment types for invite link + add people parity with Trip.
-- V019 seeded RESIDENT_MANAGE only; bound invite mint and participant add require PARTICIPANT_MANAGE
-- unless the backend accepts RESIDENT_MANAGE as a fallback (see assertGroupPeopleManageAllowed).

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM core.capability WHERE capability_id = 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid
  ) THEN
    RAISE EXCEPTION 'V056 requires PARTICIPANT_MANAGE capability from V019';
  END IF;
END $$;

INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mtc.moment_type_id::text || ':PARTICIPANT_MANAGE')::uuid,
  mtc.moment_type_id,
  'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid,
  true,
  22,
  'ACTIVE'
FROM core.moment_type_capability mtc
JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id
JOIN core.moment_category mc ON mc.moment_category_id = mt.moment_category_id
WHERE mtc.capability_id = '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid
  AND mtc.status = 'ACTIVE'
  AND mc.code = 'SHARED_LIVING'
  AND mt.domain_code = 'GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM core.moment_type_capability x
    WHERE x.moment_type_id = mtc.moment_type_id
      AND x.capability_id = 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid
  );

COMMENT ON TABLE core.moment_type_capability IS
  'Includes PARTICIPANT_MANAGE on Shared Living types for invite/people flows (V056).';

COMMIT;
