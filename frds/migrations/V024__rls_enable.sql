BEGIN;

CREATE SCHEMA security;
COMMENT ON SCHEMA security IS 'Internal database security helper functions for Momentra RLS. Contains no product data.';

CREATE OR REPLACE FUNCTION security.current_user_id()
RETURNS UUID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog
AS $$
DECLARE
    v_user_id UUID;
    v_sub TEXT;
BEGIN
    IF to_regprocedure('auth.uid()') IS NOT NULL THEN
        EXECUTE 'SELECT auth.uid()' INTO v_user_id;
        IF v_user_id IS NOT NULL THEN
            RETURN v_user_id;
        END IF;
    END IF;

    v_sub := current_setting('request.jwt.claim.sub', true);
    IF v_sub IS NOT NULL AND btrim(v_sub) <> '' THEN
        BEGIN
            RETURN v_sub::UUID;
        EXCEPTION WHEN invalid_text_representation THEN
            RETURN NULL;
        END;
    END IF;

    BEGIN
        v_sub := (current_setting('request.jwt.claims', true)::jsonb ->> 'sub');
        IF v_sub IS NOT NULL AND btrim(v_sub) <> '' THEN
            RETURN v_sub::UUID;
        END IF;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION security.has_database_role(p_role_name TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SET search_path = pg_catalog
AS $$
SELECT COALESCE(
    (
        SELECT pg_has_role(current_user, r.oid, 'member')
        FROM pg_roles r
        WHERE r.rolname = p_role_name
    ), false
);
$$;

CREATE OR REPLACE FUNCTION security.is_backend_app()
RETURNS BOOLEAN LANGUAGE sql STABLE SET search_path=pg_catalog,security AS $$
SELECT security.has_database_role('momentra_app');
$$;

CREATE OR REPLACE FUNCTION security.is_analytics_worker()
RETURNS BOOLEAN LANGUAGE sql STABLE SET search_path=pg_catalog,security AS $$
SELECT security.has_database_role('momentra_analytics_worker');
$$;
CREATE OR REPLACE FUNCTION security.is_memory_worker()
RETURNS BOOLEAN LANGUAGE sql STABLE SET search_path=pg_catalog,security AS $$
SELECT security.has_database_role('momentra_memory_worker');
$$;
CREATE OR REPLACE FUNCTION security.is_projection_worker()
RETURNS BOOLEAN LANGUAGE sql STABLE SET search_path=pg_catalog,security AS $$
SELECT security.has_database_role('momentra_projection_worker');
$$;

CREATE OR REPLACE FUNCTION security.owns_personal_moment(p_moment_id UUID)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path=pg_catalog,personal,security
AS $$
SELECT EXISTS (
    SELECT 1
    FROM personal.personal_moment_context pmc
    WHERE pmc.moment_id = p_moment_id
      AND pmc.user_id = security.current_user_id()
);
$$;

CREATE OR REPLACE FUNCTION security.is_active_group_participant(p_moment_id UUID)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path=pg_catalog,collaboration,security
AS $$
SELECT EXISTS (
    SELECT 1
    FROM collaboration.moment_participant mp
    WHERE mp.moment_id = p_moment_id
      AND mp.user_id = security.current_user_id()
      AND mp.status = 'ACTIVE'
);
$$;

CREATE OR REPLACE FUNCTION security.is_active_company_member(p_company_id UUID)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path=pg_catalog,business,security
AS $$
SELECT EXISTS (
    SELECT 1
    FROM business.company_membership cm
    WHERE cm.company_id = p_company_id
      AND cm.user_id = security.current_user_id()
      AND cm.status = 'ACTIVE'
);
$$;

CREATE OR REPLACE FUNCTION security.can_access_moment(p_moment_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path=pg_catalog,core,personal,collaboration,business,security
AS $$
DECLARE
    v_domain TEXT;
    v_company UUID;
BEGIN
    SELECT m.domain_code INTO v_domain
    FROM core.moment m WHERE m.moment_id = p_moment_id;
    IF v_domain = 'PERSONAL' THEN
        RETURN security.owns_personal_moment(p_moment_id);
    ELSIF v_domain = 'GROUP' THEN
        RETURN security.is_active_group_participant(p_moment_id);
    ELSIF v_domain = 'BUSINESS' THEN
        SELECT bmc.company_id INTO v_company
        FROM business.business_moment_context bmc
        WHERE bmc.moment_id = p_moment_id;
        RETURN v_company IS NOT NULL AND security.is_active_company_member(v_company);
    END IF;
    RETURN FALSE;
END;
$$;

CREATE OR REPLACE FUNCTION security.can_access_scope(p_scope_type TEXT, p_scope_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path=pg_catalog,core,collaboration,business,security
AS $$
DECLARE v_company UUID;
BEGIN
    IF p_scope_type = 'USER' THEN
        RETURN p_scope_id = security.current_user_id();
    ELSIF p_scope_type = 'MOMENT' THEN
        RETURN security.can_access_moment(p_scope_id);
    ELSIF p_scope_type = 'COMPANY' THEN
        RETURN security.is_active_company_member(p_scope_id);
    ELSIF p_scope_type = 'PARTICIPANT' THEN
        RETURN EXISTS (
            SELECT 1 FROM collaboration.moment_participant mp
            WHERE mp.participant_id = p_scope_id
              AND mp.user_id = security.current_user_id()
              AND mp.status = 'ACTIVE'
        );
    ELSIF p_scope_type = 'TEAM' THEN
        SELECT t.company_id INTO v_company FROM business.team t WHERE t.team_id = p_scope_id;
        RETURN v_company IS NOT NULL AND security.is_active_company_member(v_company);
    END IF;
    RETURN FALSE;
END;
$$;

REVOKE ALL ON SCHEMA security FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA security FROM PUBLIC;

-- RLS is enabled before policies/grants. No protected table receives an open fallback policy.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT n.nspname AS schema_name, c.relname AS table_name
        FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
        WHERE c.relkind='r'
          AND n.nspname IN ('personal','collaboration','business','work','finance','memory','projection')
    LOOP
        EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', r.schema_name, r.table_name);
    END LOOP;
END $$;

ALTER TABLE core.user_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE core.moment ENABLE ROW LEVEL SECURITY;

-- Foundational Core policies. Taxonomy/Capability tables stay non-RLS and are read-only catalogue surfaces.
CREATE POLICY rls_user_profile__select_self ON core.user_profile
FOR SELECT USING (user_id = security.current_user_id() OR security.is_backend_app());
CREATE POLICY rls_user_profile__backend ON core.user_profile
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_moment__select_scope ON core.moment
FOR SELECT USING (security.can_access_moment(moment_id) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_moment__backend ON core.moment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMIT;
