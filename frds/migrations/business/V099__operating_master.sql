BEGIN;

-- Stable master identity for operating Business Moments (Trading, Distribution,
-- Retail, Marketing, Manufacturing).
--
-- Before this table, `masters` in the operating projection was derived from
-- `business.operating_event` rows that happened to carry a `master` payload, with
-- the operating event id doubling as the master id. Free-text names became identity,
-- nothing could be edited or archived, and a rename silently forked the record.
--
-- A master is now its own company-scoped record. Events reference it by
-- `payload.masterId`; event-derived masters remain readable as a legacy fallback so
-- existing moments keep loading unchanged.

CREATE TABLE business.operating_master (
    master_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    kind TEXT NOT NULL,
    name TEXT NOT NULL,
    code TEXT,
    -- Unit / measurement
    unit TEXT,
    dimension TEXT,
    conversion_to_base NUMERIC(18, 4),
    tracked BOOLEAN,
    barcode TEXT,
    -- Pricing (effective-dated rates live on business.operating_price_list_line)
    sell_price NUMERIC(18, 4),
    mrp NUMERIC(18, 4),
    purchase_price NUMERIC(18, 4),
    -- GST
    hsn TEXT,
    gst_applicable TEXT,
    gst_classification TEXT,
    gst_rate NUMERIC(6, 2),
    price_includes_gst TEXT,
    sale_price_includes_gst TEXT,
    purchase_price_includes_gst TEXT,
    gst_effective_from DATE,
    gstin TEXT,
    business_state TEXT,
    billing_state TEXT,
    delivery_state TEXT,
    -- Relations (dealer -> principal, route -> warehouse, batch -> SKU)
    parent_master_id UUID,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_operating_master__company
        FOREIGN KEY (company_id) REFERENCES business.company(company_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_operating_master__moment
        FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_operating_master__parent
        FOREIGN KEY (parent_master_id) REFERENCES business.operating_master(master_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_operating_master__kind CHECK (
        kind IN (
            'SKU',
            'PRODUCT',
            'MATERIAL',
            'CONSUMABLE',
            'SUPPLIER',
            'BUYER',
            'CUSTOMER',
            'PRINCIPAL',
            'DEALER',
            'CHANNEL',
            'LOCATION',
            'WAREHOUSE',
            'ROUTE',
            'EXPENSE_CATEGORY',
            'UOM',
            'BATCH',
            'PRICE_LIST',
            'SERVICE',
            'STAFF',
            'STATION',
            'METRIC',
            'BUDGET_CATEGORY',
            'PARTY',
            'VENDOR',
            'MENU_DISH'
        )
    ),
    CONSTRAINT ck_operating_master__status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_operating_master__dimension CHECK (
        dimension IS NULL OR dimension IN ('count', 'mass', 'volume', 'length', 'area')
    ),
    CONSTRAINT ck_operating_master__gst_applicable CHECK (
        gst_applicable IS NULL OR gst_applicable IN ('YES', 'NO', 'NOT_SURE')
    ),
    CONSTRAINT ck_operating_master__gst_classification CHECK (
        gst_classification IS NULL OR gst_classification IN ('TAXABLE', 'EXEMPT', 'NIL', 'NON_GST')
    ),
    CONSTRAINT ck_operating_master__gst_include_flags CHECK (
        (price_includes_gst IS NULL OR price_includes_gst IN ('YES', 'NO'))
        AND (sale_price_includes_gst IS NULL OR sale_price_includes_gst IN ('YES', 'NO'))
        AND (purchase_price_includes_gst IS NULL OR purchase_price_includes_gst IN ('YES', 'NO'))
    ),
    CONSTRAINT ck_operating_master__name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_operating_master__version CHECK (version > 0),
    CONSTRAINT ck_operating_master__not_self_parent CHECK (
        parent_master_id IS NULL OR parent_master_id <> master_id
    )
);

-- Never derive identity from free-text name: the name is unique per kind, and the
-- comparison is case-insensitive because operators type names casually.
CREATE UNIQUE INDEX uq_operating_master__company_kind_name
    ON business.operating_master (company_id, kind, lower(btrim(name)));

CREATE INDEX ix_operating_master__company_kind_status
    ON business.operating_master (company_id, kind, status);

CREATE INDEX ix_operating_master__moment
    ON business.operating_master (moment_id)
    WHERE moment_id IS NOT NULL;

CREATE INDEX ix_operating_master__parent
    ON business.operating_master (parent_master_id)
    WHERE parent_master_id IS NOT NULL;

-- Effective-dated price lists. A price list is a master write: Sales Orders pick
-- rates from it, but stock and the ledger only move on dispatch and invoice.
CREATE TABLE business.operating_price_list (
    price_list_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    name TEXT NOT NULL,
    channel TEXT,
    price_unit TEXT,
    margin_pct NUMERIC(6, 2),
    scheme_pct NUMERIC(6, 2),
    gst_rate NUMERIC(6, 2),
    valid_from DATE NOT NULL,
    valid_to DATE,
    warehouse_master_id UUID,
    min_order NUMERIC(18, 4),
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_operating_price_list__company
        FOREIGN KEY (company_id) REFERENCES business.company(company_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_operating_price_list__moment
        FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_operating_price_list__warehouse
        FOREIGN KEY (warehouse_master_id) REFERENCES business.operating_master(master_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_operating_price_list__status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_operating_price_list__validity CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT ck_operating_price_list__name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_operating_price_list__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_operating_price_list__company_name_from
    ON business.operating_price_list (company_id, lower(btrim(name)), valid_from);

CREATE INDEX ix_operating_price_list__company_status
    ON business.operating_price_list (company_id, status, valid_from DESC);

CREATE TABLE business.operating_price_list_line (
    price_list_id UUID NOT NULL,
    master_id UUID NOT NULL,
    price NUMERIC(18, 4) NOT NULL,
    scheme_pct NUMERIC(6, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (price_list_id, master_id),
    CONSTRAINT fk_operating_price_list_line__list
        FOREIGN KEY (price_list_id) REFERENCES business.operating_price_list(price_list_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_operating_price_list_line__master
        FOREIGN KEY (master_id) REFERENCES business.operating_master(master_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_operating_price_list_line__price CHECK (price >= 0)
);

CREATE INDEX ix_operating_price_list_line__master
    ON business.operating_price_list_line (master_id);

-- RLS and grants mirror business.operating_event so the API process can read and
-- write with the same coarse policy while Node re-authorizes every command.
GRANT SELECT, INSERT, UPDATE, DELETE ON business.operating_master TO momentra_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON business.operating_price_list TO momentra_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON business.operating_price_list_line TO momentra_app;
GRANT SELECT ON business.operating_master TO momentra_projection_worker;
GRANT SELECT ON business.operating_price_list TO momentra_projection_worker;
GRANT SELECT ON business.operating_price_list_line TO momentra_projection_worker;

ALTER TABLE business.operating_master ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.operating_price_list ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.operating_price_list_line ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_operating_master__select_member ON business.operating_master
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_projection_worker()
);

CREATE POLICY rls_operating_master__backend_write ON business.operating_master
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_operating_price_list__select_member ON business.operating_price_list
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_projection_worker()
);

CREATE POLICY rls_operating_price_list__backend_write ON business.operating_price_list
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_operating_price_list_line__select_member ON business.operating_price_list_line
FOR SELECT USING (
    EXISTS (
        SELECT 1
        FROM business.operating_price_list pl
        WHERE pl.price_list_id = business.operating_price_list_line.price_list_id
          AND (
              security.is_active_company_member(pl.company_id)
              OR security.is_backend_app()
              OR security.is_projection_worker()
          )
    )
);

CREATE POLICY rls_operating_price_list_line__backend_write ON business.operating_price_list_line
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

-- A master may only nest under another master of the same company.
CREATE OR REPLACE FUNCTION business.operating_master_parent_same_company()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    parent_company uuid;
BEGIN
    IF NEW.parent_master_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT company_id INTO parent_company
    FROM business.operating_master
    WHERE master_id = NEW.parent_master_id;
    IF parent_company IS NULL OR parent_company <> NEW.company_id THEN
        RAISE EXCEPTION 'operating master parent must belong to the same company';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_operating_master_parent_same_company
BEFORE INSERT OR UPDATE OF parent_master_id, company_id ON business.operating_master
FOR EACH ROW EXECUTE FUNCTION business.operating_master_parent_same_company();

COMMENT ON TABLE business.operating_master IS
    'Stable company-scoped master data (SKU, party, location, route, channel) for operating Business Moments.';

COMMENT ON TABLE business.operating_price_list IS
    'Effective-dated buyer price lists. Rates are master data; GST posts on invoice only.';

COMMENT ON COLUMN business.operating_master.parent_master_id IS
    'Nesting: dealer -> principal, route -> warehouse, batch -> SKU.';

COMMIT;
