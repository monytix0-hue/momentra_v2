BEGIN;

-- Phase 6: MOMENT_CREATE capability referenced by runtime governance but missing from V019 catalogue.
-- This migration adds ONLY that verified gap — no unrelated capabilities.

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
  'a8f3c2e1-4b5d-5a6f-9c0e-1d2e3f4a5b6c'::uuid,
  'MOMENT_CREATE',
  'Create Moment',
  'Create Moment capability.',
  'CORE',
  'MOMENT',
  'CREATE',
  'STANDARD',
  false,
  'ACTIVE'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  uuid_generate_v5(
    'a8f3c2e1-4b5d-5a6f-9c0e-1d2e3f4a5b6c'::uuid,
    'MOMENT_CREATE:' || mt.moment_type_id::text
  ),
  mt.moment_type_id,
  c.capability_id,
  true,
  5,
  'ACTIVE'
FROM core.moment_type mt
CROSS JOIN core.capability c
WHERE mt.status = 'ACTIVE'
  AND c.code = 'MOMENT_CREATE'
ON CONFLICT (moment_type_capability_id) DO NOTHING;

COMMIT;
