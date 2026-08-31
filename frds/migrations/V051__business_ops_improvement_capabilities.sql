-- Momentra V051 - Business Operations: operational_improvement + Ops capability maps
-- CL-26 improvement table; map ISSUE_CREATE / SLA_MANAGE / VENDOR_MANAGE onto BUSINESS_OPERATIONS types.

BEGIN;

CREATE TABLE IF NOT EXISTS business.operational_improvement (
    operational_improvement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    title TEXT NOT NULL,
    description TEXT,
    category_code TEXT,
    impact_estimate TEXT,
    status TEXT NOT NULL DEFAULT 'LOGGED',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_operational_improvement__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_operational_improvement__moment_company
        FOREIGN KEY (moment_id, company_id)
        REFERENCES business.business_moment_context(moment_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_operational_improvement__author
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_operational_improvement__status
        CHECK (status IN ('LOGGED','IN_PROGRESS','DONE','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS ix_operational_improvement__company_moment
    ON business.operational_improvement (company_id, moment_id, created_at DESC);

-- Map Ops capabilities onto Business moment types that already have EXPENSE_CREATE
INSERT INTO core.moment_type_capability (
  moment_type_capability_id,
  moment_type_id,
  capability_id,
  is_default,
  sort_order,
  status
)
SELECT
  md5(mt.moment_type_id::text || ':' || c.code)::uuid,
  mt.moment_type_id,
  c.capability_id,
  true,
  30,
  'ACTIVE'
FROM core.moment_type mt
CROSS JOIN core.capability c
WHERE mt.domain_code = 'BUSINESS'
  AND mt.status = 'ACTIVE'
  AND (
    mt.code ILIKE '%OPERATIONS%'
    OR EXISTS (
      SELECT 1 FROM core.moment_type_capability mtc0
      WHERE mtc0.moment_type_id = mt.moment_type_id
        AND mtc0.capability_id = 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid
        AND mtc0.status = 'ACTIVE'
    )
  )
  AND c.code IN ('ISSUE_CREATE', 'SLA_MANAGE', 'VENDOR_MANAGE')
  AND c.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM core.moment_type_capability x
    WHERE x.moment_type_id = mt.moment_type_id AND x.capability_id = c.capability_id
  );

COMMENT ON TABLE business.operational_improvement IS
  'Business Operations Log Improvement writes (Excel CL-26 / V051).';

COMMIT;
