-- Business V4 B2 foundations: obligations, settlements, inventory movement ledger.
-- Canonical Quick Add rows remain business.operating_event (logical business_event).
-- Apply after V102 (listed before V103 in MIGRATION_ORDER). Skip/re-run only after V103 rebuild on dedicated V4/dev DBs.

BEGIN;

COMMENT ON TABLE business.operating_event IS
  'Business V4 canonical event ledger (logical business_event). Append-only Quick Add / operating posts.';

CREATE TABLE IF NOT EXISTS business.business_obligation (
    obligation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES business.company(company_id) ON DELETE RESTRICT,
    moment_id UUID NOT NULL REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
    party_id UUID NULL,
    obligation_kind TEXT NOT NULL,
    source_event_id UUID NULL REFERENCES business.operating_event(operating_event_id) ON DELETE SET NULL,
    currency_code TEXT NOT NULL DEFAULT 'INR',
    amount NUMERIC(18, 4) NOT NULL,
    outstanding NUMERIC(18, 4) NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN',
    due_at TIMESTAMPTZ NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_business_obligation__kind CHECK (
        obligation_kind IN ('PAYABLE', 'RECEIVABLE')
    ),
    CONSTRAINT ck_business_obligation__status CHECK (
        status IN ('OPEN', 'PARTIAL', 'SETTLED', 'VOID')
    ),
    CONSTRAINT ck_business_obligation__amounts CHECK (
        amount >= 0 AND outstanding >= 0 AND outstanding <= amount
    ),
    CONSTRAINT ck_business_obligation__version CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS ix_business_obligation__moment
    ON business.business_obligation (moment_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_business_obligation__company
    ON business.business_obligation (company_id, obligation_kind, status);

CREATE TABLE IF NOT EXISTS business.business_settlement_allocation (
    settlement_allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    obligation_id UUID NOT NULL REFERENCES business.business_obligation(obligation_id) ON DELETE RESTRICT,
    payment_event_id UUID NOT NULL REFERENCES business.operating_event(operating_event_id) ON DELETE RESTRICT,
    allocated_amount NUMERIC(18, 4) NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_business_settlement_allocation__amount CHECK (allocated_amount > 0),
    CONSTRAINT uq_business_settlement_allocation__payment_obligation
        UNIQUE (payment_event_id, obligation_id)
);

CREATE INDEX IF NOT EXISTS ix_business_settlement_allocation__obligation
    ON business.business_settlement_allocation (obligation_id);

CREATE TABLE IF NOT EXISTS business.business_inventory_movement (
    inventory_movement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES business.company(company_id) ON DELETE RESTRICT,
    moment_id UUID NOT NULL REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
    product_id UUID NULL,
    location_id UUID NULL,
    source_event_id UUID NULL REFERENCES business.operating_event(operating_event_id) ON DELETE SET NULL,
    movement_kind TEXT NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL,
    unit TEXT NULL,
    note TEXT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_business_inventory_movement__kind CHECK (
        movement_kind IN ('RECEIPT', 'ISSUE', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT', 'RETURN')
    ),
    CONSTRAINT ck_business_inventory_movement__qty CHECK (quantity <> 0)
);

CREATE INDEX IF NOT EXISTS ix_business_inventory_movement__moment
    ON business.business_inventory_movement (moment_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_business_inventory_movement__company_product
    ON business.business_inventory_movement (company_id, product_id, occurred_at DESC);

ALTER TABLE business.business_obligation ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.business_settlement_allocation ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.business_inventory_movement ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_business_obligation__select ON business.business_obligation
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_projection_worker()
);
CREATE POLICY rls_business_obligation__backend_write ON business.business_obligation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_settlement_allocation__select ON business.business_settlement_allocation
FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM business.business_obligation o
      WHERE o.obligation_id = obligation_id
        AND (security.is_active_company_member(o.company_id) OR security.is_backend_app() OR security.is_projection_worker())
    )
    OR security.is_backend_app()
);
CREATE POLICY rls_business_settlement_allocation__backend_write ON business.business_settlement_allocation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_inventory_movement__select ON business.business_inventory_movement
FOR SELECT USING (
    security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_projection_worker()
);
CREATE POLICY rls_business_inventory_movement__backend_write ON business.business_inventory_movement
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT ON business.business_obligation, business.business_settlement_allocation, business.business_inventory_movement
    TO momentra_app, momentra_projection_worker;
GRANT INSERT, UPDATE, DELETE ON business.business_obligation, business.business_settlement_allocation, business.business_inventory_movement
    TO momentra_app;

COMMIT;
