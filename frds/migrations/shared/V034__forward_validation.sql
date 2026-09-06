BEGIN;

-- Post-V031 validation: ensure forward schema objects exist.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'business' AND table_name = 'company_location'
    ) THEN
        RAISE EXCEPTION 'V034: business.company_location missing — apply V031 first';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'shared' AND table_name = 'poll'
    ) THEN
        RAISE EXCEPTION 'V034: shared.poll missing — apply V032 first';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'core' AND table_name = 'moment' AND column_name = 'custom_type_label'
    ) THEN
        RAISE EXCEPTION 'V034: core.moment.custom_type_label missing — apply V031 first';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'platform' AND table_name = 'user_device'
    ) THEN
        RAISE EXCEPTION 'V034: platform.user_device missing — apply V033 first';
    END IF;
END $$;

-- RLS enabled on forward-pack tables
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(n.nspname || '.' || c.relname, ', ' ORDER BY n.nspname, c.relname)
    INTO v_missing
    FROM (VALUES
        ('business', 'company_location'),
        ('shared', 'poll'),
        ('shared', 'poll_option'),
        ('shared', 'poll_vote'),
        ('platform', 'user_device'),
        ('platform', 'media_upload')
    ) AS req(schema_name, table_name)
    JOIN pg_namespace n ON n.nspname = req.schema_name
    JOIN pg_class c ON c.relnamespace = n.oid AND c.relname = req.table_name
    WHERE NOT c.relrowsecurity;

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'V034: RLS not enabled on: %', v_missing;
    END IF;
END $$;

-- RLS policies exist on forward-pack tables
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(r.qualified_name, ', ' ORDER BY r.qualified_name)
    INTO v_missing
    FROM (VALUES
        ('business.company_location'),
        ('shared.poll'),
        ('shared.poll_option'),
        ('shared.poll_vote'),
        ('platform.user_device'),
        ('platform.media_upload')
    ) AS r(qualified_name)
    WHERE NOT EXISTS (
        SELECT 1 FROM pg_policy p
        JOIN pg_class c ON c.oid = p.polrelid
        WHERE c.oid = to_regclass(r.qualified_name)
    );

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'V034: missing RLS policies on: %', v_missing;
    END IF;
END $$;

-- Frozen taxonomy: SHARED_LIVING group moment type
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM core.moment_type
        WHERE domain_code = 'GROUP' AND code = 'SHARED_LIVING' AND status = 'ACTIVE'
    ) THEN
        RAISE EXCEPTION 'V034: frozen GROUP moment type SHARED_LIVING missing';
    END IF;
END $$;

-- No duplicate stable moment type codes per domain
DO $$
DECLARE v_dupes BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_dupes
    FROM (
        SELECT domain_code, code, COUNT(*) n
        FROM core.moment_type
        GROUP BY domain_code, code
        HAVING COUNT(*) > 1
    ) x;
    IF v_dupes > 0 THEN
        RAISE EXCEPTION 'V034: duplicate moment type codes = %', v_dupes;
    END IF;
END $$;

-- No duplicate capability codes
DO $$
DECLARE v_dupes BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_dupes
    FROM (
        SELECT code, COUNT(*) n FROM core.capability GROUP BY code HAVING COUNT(*) > 1
    ) x;
    IF v_dupes > 0 THEN
        RAISE EXCEPTION 'V034: duplicate capability codes = %', v_dupes;
    END IF;
END $$;

-- Canonical finance amounts use NUMERIC, not float types
DO $$
DECLARE v_bad BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_bad
    FROM information_schema.columns
    WHERE table_schema = 'finance'
      AND column_name ~ '(amount|balance|total|paid|share|opening|quantity|subtotal|tax|original|settled)'
      AND data_type IN ('real', 'double precision', 'float4', 'float8');
    IF v_bad > 0 THEN
        RAISE EXCEPTION 'V034: finance columns using float types = %', v_bad;
    END IF;
END $$;

-- Audit / outbox structures intact
DO $$
BEGIN
    IF to_regclass('audit.audit_record') IS NULL THEN
        RAISE EXCEPTION 'V034: audit.audit_record missing';
    END IF;
    IF to_regclass('events.outbox_event') IS NULL THEN
        RAISE EXCEPTION 'V034: events.outbox_event missing';
    END IF;
    IF to_regclass('events.domain_event') IS NULL THEN
        RAISE EXCEPTION 'V034: events.domain_event missing';
    END IF;
END $$;

-- Work domain owns goal/milestone/task (no duplicate engines)
DO $$
BEGIN
    IF to_regclass('personal.goal') IS NOT NULL
        OR to_regclass('collaboration.goal') IS NOT NULL
        OR to_regclass('business.goal') IS NOT NULL THEN
        RAISE EXCEPTION 'V034: duplicate goal engine detected outside work schema';
    END IF;
    IF to_regclass('personal.task') IS NOT NULL
        OR to_regclass('collaboration.task') IS NOT NULL
        OR to_regclass('business.task') IS NOT NULL THEN
        RAISE EXCEPTION 'V034: duplicate task engine detected outside work schema';
    END IF;
END $$;

COMMIT;
