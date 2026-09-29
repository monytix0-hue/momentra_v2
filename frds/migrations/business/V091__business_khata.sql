-- Khata lite: party ledger entries against business.vendor (CUSTOMER / SUPPLIER).
-- Balance = sum(CREDIT) - sum(PAYMENT) per party.

CREATE TABLE IF NOT EXISTS business.khata_entry (
    entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    party_vendor_id UUID NOT NULL,
    entry_type TEXT NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'INR',
    payment_method_code TEXT,
    note TEXT,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_khata_entry__type CHECK (entry_type IN ('CREDIT', 'PAYMENT')),
    CONSTRAINT ck_khata_entry__amount CHECK (amount > 0),
    CONSTRAINT fk_khata_entry__company
      FOREIGN KEY (company_id) REFERENCES business.company (company_id),
    CONSTRAINT fk_khata_entry__party
      FOREIGN KEY (party_vendor_id, company_id)
      REFERENCES business.vendor (vendor_id, company_id)
);

CREATE INDEX IF NOT EXISTS ix_khata_entry__company_party_time
  ON business.khata_entry (company_id, party_vendor_id, effective_at DESC);

CREATE INDEX IF NOT EXISTS ix_khata_entry__company_time
  ON business.khata_entry (company_id, effective_at DESC);

ALTER TABLE business.khata_entry ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_khata_entry__select_member ON business.khata_entry
FOR SELECT USING (
  security.is_active_company_member(company_id)
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);

CREATE POLICY rls_khata_entry__backend_write ON business.khata_entry
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMENT ON TABLE business.khata_entry IS
  'Khata lite ledger: CREDIT = udhaar sale/purchase; PAYMENT = collection/payout against vendor party.';
