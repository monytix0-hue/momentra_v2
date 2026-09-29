-- Revenue can link to a Khata party (business.vendor) and record how cash was taken.
ALTER TABLE finance.revenue
  ADD COLUMN IF NOT EXISTS party_vendor_id UUID,
  ADD COLUMN IF NOT EXISTS payment_method_code TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_revenue__party_vendor'
  ) THEN
    ALTER TABLE finance.revenue
      ADD CONSTRAINT fk_revenue__party_vendor
      FOREIGN KEY (party_vendor_id) REFERENCES business.vendor (vendor_id) ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_revenue__company_party
  ON finance.revenue (company_id, party_vendor_id)
  WHERE party_vendor_id IS NOT NULL;

COMMENT ON COLUMN finance.revenue.party_vendor_id IS
  'Optional Khata party (business.vendor) for cash sale linkage; does not post khata_entry.';
COMMENT ON COLUMN finance.revenue.payment_method_code IS
  'CASH / UPI / CARD / … when recorded at sale.';
