-- Business authz isolation: operating_role on invite/membership;
-- align can_access_moment + business_moment_context SELECT with company-less moments.

BEGIN;

ALTER TABLE business.company_invite
  ADD COLUMN IF NOT EXISTS operating_role TEXT NULL;

ALTER TABLE business.company_membership
  ADD COLUMN IF NOT EXISTS operating_role TEXT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'ck_company_invite__operating_role'
      AND conrelid = 'business.company_invite'::regclass
  ) THEN
    ALTER TABLE business.company_invite
      ADD CONSTRAINT ck_company_invite__operating_role
      CHECK (
        operating_role IS NULL
        OR operating_role IN ('FINANCE', 'APPROVER', 'OPERATIONS_LEAD', 'CONTRIBUTOR')
      );
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'ck_company_membership__operating_role'
      AND conrelid = 'business.company_membership'::regclass
  ) THEN
    ALTER TABLE business.company_membership
      ADD CONSTRAINT ck_company_membership__operating_role
      CHECK (
        operating_role IS NULL
        OR operating_role IN ('FINANCE', 'APPROVER', 'OPERATIONS_LEAD', 'CONTRIBUTOR')
      );
  END IF;
END $$;

CREATE OR REPLACE FUNCTION security.can_access_moment(p_moment_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path = pg_catalog, core, personal, collaboration, business, security
AS $$
DECLARE
  v_domain TEXT;
  v_company UUID;
BEGIN
  SELECT m.domain_code INTO v_domain
  FROM core.moment m
  WHERE m.moment_id = p_moment_id;

  IF v_domain = 'PERSONAL' THEN
    RETURN security.owns_personal_moment(p_moment_id);
  ELSIF v_domain = 'GROUP' THEN
    RETURN security.is_active_group_participant(p_moment_id);
  ELSIF v_domain = 'BUSINESS' THEN
    SELECT bmc.company_id INTO v_company
    FROM business.business_moment_context bmc
    WHERE bmc.moment_id = p_moment_id;

    IF v_company IS NOT NULL THEN
      RETURN security.is_active_company_member(v_company);
    END IF;

    RETURN EXISTS (
      SELECT 1
      FROM core.moment m
      LEFT JOIN collaboration.moment_participant mp
        ON mp.moment_id = m.moment_id
       AND mp.user_id = security.current_user_id()
       AND mp.status = 'ACTIVE'
      WHERE m.moment_id = p_moment_id
        AND (m.created_by_user_id = security.current_user_id() OR mp.participant_id IS NOT NULL)
    );
  END IF;

  RETURN FALSE;
END;
$$;

DROP POLICY IF EXISTS rls_business_moment_context__select_member ON business.business_moment_context;
CREATE POLICY rls_business_moment_context__select_member ON business.business_moment_context
FOR SELECT USING (
  (
    company_id IS NOT NULL
    AND security.is_active_company_member(company_id)
  )
  OR (
    company_id IS NULL
    AND EXISTS (
      SELECT 1
      FROM core.moment m
      LEFT JOIN collaboration.moment_participant mp
        ON mp.moment_id = m.moment_id
       AND mp.user_id = security.current_user_id()
       AND mp.status = 'ACTIVE'
      WHERE m.moment_id = business_moment_context.moment_id
        AND (m.created_by_user_id = security.current_user_id() OR mp.participant_id IS NOT NULL)
    )
  )
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);

COMMIT;
