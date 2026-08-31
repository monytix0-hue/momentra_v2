BEGIN;

CREATE OR REPLACE FUNCTION platform.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- Attach only to tables that physically expose updated_at. This function is deliberately technical only.
DO $$
DECLARE
    r RECORD;
    trigger_name TEXT;
BEGIN
    FOR r IN
        SELECT table_schema, table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema IN ('core','personal','collaboration','business','work','finance','governance','analytics','memory','events','platform','ai','projection')
        GROUP BY table_schema, table_name
        ORDER BY table_schema, table_name
    LOOP
        trigger_name := 'trg_' || r.table_name || '__updated_at';
        EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE ON %I.%I FOR EACH ROW EXECUTE FUNCTION platform.set_updated_at()', trigger_name, r.table_schema, r.table_name);
    END LOOP;
END
$$;

COMMENT ON FUNCTION platform.set_updated_at() IS 'Technical timestamp maintenance only. Domain lifecycle, event publication, analytics, memory, AI and projection behavior must not be implemented through database triggers.';

COMMIT;
