-- Momentra V049 - GROUP_VENDOR_MANAGE (COLLABORATION) + map onto ATTENDANCE_RECORD moment types
-- Distinct from BUSINESS VENDOR_MANAGE. Unlocks Group vendor writers under authorize().

BEGIN;

INSERT INTO core.capability (
  capability_id,
  code,
  display_name,
  description,
  owning_service,
  resource_type,
  action_type,
  sensitivity_level,
  approval_eligible,
  status
)
VALUES (
  'a1b2c3d4-e5f6-5789-a012-3456789abcde'::uuid,
  'GROUP_VENDOR_MANAGE',
  'Manage Group Vendor',
  'Manage vendors on Group moments (collaboration).',
  'COLLABORATION',
  'GROUP_VENDOR',
  'MANAGE',
  'SENSITIVE',
  false,
  'ACTIVE'
)
ON CONFLICT (capability_id) DO NOTHING;

INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mtc.moment_type_id::text || ':GROUP_VENDOR_MANAGE')::uuid,
  mtc.moment_type_id,
  'a1b2c3d4-e5f6-5789-a012-3456789abcde'::uuid,
  true,
  23,
  'ACTIVE'
FROM core.moment_type_capability mtc
WHERE mtc.capability_id = 'e23a7487-040c-555a-802f-af27accf9acc'::uuid
  AND mtc.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM core.moment_type_capability x
    WHERE x.moment_type_id = mtc.moment_type_id
      AND x.capability_id = 'a1b2c3d4-e5f6-5789-a012-3456789abcde'::uuid
  );

COMMENT ON TABLE core.moment_type_capability IS
  'Includes GROUP_VENDOR_MANAGE for moment types that already allow ATTENDANCE_RECORD (V049).';

COMMIT;
