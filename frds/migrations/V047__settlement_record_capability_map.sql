-- Momentra V047 - Map SETTLEMENT_RECORD onto GROUP moment types that already allow EXPENSE_CREATE
-- Unlocks POST /v1/moments/:id/settlements (removes 501 API_GAP). Requires V019 capability seed.

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM core.capability WHERE capability_id = '4730fee0-af11-552b-b05b-556f4f33f616'::uuid
  ) THEN
    RAISE EXCEPTION 'V047 requires SETTLEMENT_RECORD capability from V019';
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
  md5(mtc.moment_type_id::text || ':SETTLEMENT_RECORD')::uuid,
  mtc.moment_type_id,
  '4730fee0-af11-552b-b05b-556f4f33f616'::uuid,
  true,
  20,
  'ACTIVE'
FROM core.moment_type_capability mtc
WHERE mtc.capability_id = 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid
  AND mtc.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM core.moment_type_capability x
    WHERE x.moment_type_id = mtc.moment_type_id
      AND x.capability_id = '4730fee0-af11-552b-b05b-556f4f33f616'::uuid
  );

COMMENT ON TABLE core.moment_type_capability IS 'Includes SETTLEMENT_RECORD for GROUP expense-capable moment types (V047).';

COMMIT;
