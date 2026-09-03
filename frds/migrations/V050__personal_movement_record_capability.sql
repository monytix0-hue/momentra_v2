-- Momentra V050 - MOVEMENT_RECORD capability + map onto Personal types with EXPENSE_CREATE
-- Unlocks Transfer/Savings Quick Add under fail-closed PersonalActionRegistry (MOVEMENT_RECORD).

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
SELECT
  md5('momentra:capability:MOVEMENT_RECORD')::uuid,
  'MOVEMENT_RECORD',
  'Record Movement',
  'Record Transfer / Savings movement capability.',
  'FINANCE',
  'MOVEMENT',
  'RECORD',
  'SENSITIVE',
  false,
  'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM core.capability WHERE code = 'MOVEMENT_RECORD');

INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mtc.moment_type_id::text || ':MOVEMENT_RECORD')::uuid,
  mtc.moment_type_id,
  c.capability_id,
  true,
  21,
  'ACTIVE'
FROM core.moment_type_capability mtc
JOIN core.moment_type mt ON mt.moment_type_id = mtc.moment_type_id AND mt.domain_code = 'PERSONAL'
JOIN core.capability c ON c.code = 'MOVEMENT_RECORD' AND c.status = 'ACTIVE'
WHERE mtc.capability_id = 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid
  AND mtc.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM core.moment_type_capability x
    WHERE x.moment_type_id = mtc.moment_type_id
      AND x.capability_id = c.capability_id
  );

COMMENT ON TABLE core.moment_type_capability IS
  'Includes MOVEMENT_RECORD for PERSONAL expense-capable moment types (V050).';

COMMIT;
