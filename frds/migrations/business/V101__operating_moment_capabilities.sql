-- V094 seeded operating Business moment types with MOMENT_CREATE only.
-- Start-this-business on Android/iOS renames the draft via MOMENT_UPDATE before
-- activate; without that map, governance returns
-- "Capability not enabled for this moment type." for every operating vertical.
--
-- Map the lifecycle + shared write caps every BUSINESS type already gets, onto
-- Marketing / Trading / Retail / Manufacturing operating types.

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM core.capability WHERE code = 'MOMENT_UPDATE' AND status = 'ACTIVE'
  ) THEN
    RAISE EXCEPTION 'V101 requires MOMENT_UPDATE capability from V019';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM core.moment_type WHERE code = 'TRADING_WHOLESALE' AND status = 'ACTIVE'
  ) THEN
    RAISE EXCEPTION 'V101 requires operating moment types from V094';
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
  uuid_generate_v5(
    'a8f3c2e1-4b5d-5a6f-9c0e-1d2e3f4a5b6c'::uuid,
    c.code || ':' || mt.moment_type_id::text
  ),
  mt.moment_type_id,
  c.capability_id,
  true,
  CASE c.code
    WHEN 'MOMENT_VIEW' THEN 10
    WHEN 'MOMENT_UPDATE' THEN 11
    WHEN 'TASK_CREATE' THEN 12
    WHEN 'EXPENSE_CREATE' THEN 13
    WHEN 'BUDGET_MANAGE' THEN 14
    WHEN 'REVENUE_RECORD' THEN 15
    WHEN 'INVOICE_CREATE' THEN 16
    WHEN 'UPDATE_CREATE' THEN 17
    WHEN 'MEMORY_CREATE' THEN 18
    ELSE 30
  END,
  'ACTIVE'
FROM core.moment_type mt
CROSS JOIN core.capability c
WHERE mt.domain_code = 'BUSINESS'
  AND mt.status = 'ACTIVE'
  AND mt.code IN (
    'CLIENT_CAMPAIGN',
    'OWN_BUSINESS_CAMPAIGN',
    'TRADING_WHOLESALE',
    'DISTRIBUTION_DEALERSHIP',
    'RETAIL_STORE',
    'RESTAURANT_FOOD',
    'CONSUMER_SERVICE',
    'STANDARD_PRODUCTION',
    'MADE_TO_ORDER'
  )
  AND c.status = 'ACTIVE'
  AND c.code IN (
    'MOMENT_VIEW',
    'MOMENT_UPDATE',
    'TASK_CREATE',
    'EXPENSE_CREATE',
    'BUDGET_MANAGE',
    'REVENUE_RECORD',
    'INVOICE_CREATE',
    'UPDATE_CREATE',
    'MEMORY_CREATE'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM core.moment_type_capability x
    WHERE x.moment_type_id = mt.moment_type_id
      AND x.capability_id = c.capability_id
  );

COMMENT ON TABLE core.moment_type_capability IS
  'Includes lifecycle + finance write maps for V094 operating Business moment types (V101).';

COMMIT;
