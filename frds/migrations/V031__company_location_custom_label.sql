BEGIN;

ALTER TABLE core.moment
    ADD COLUMN IF NOT EXISTS custom_type_label TEXT;

COMMENT ON COLUMN core.moment.custom_type_label IS 'User-entered custom type label for round-trip when nearest V018 type is used for behavior.';

CREATE TABLE business.company_location (
    company_location_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name TEXT NOT NULL,
    address_text TEXT,
    timezone TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_company_location__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_company_location__status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT ck_company_location__version CHECK (version > 0),
    CONSTRAINT uq_company_location__company_name UNIQUE (company_id, name)
);

CREATE INDEX ix_company_location__company_status
    ON business.company_location (company_id, status);

COMMIT;
