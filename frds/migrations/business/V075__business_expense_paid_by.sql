-- Business expense payer label (Figma Add Expense "Paid By").
ALTER TABLE finance.business_expense_context
  ADD COLUMN IF NOT EXISTS paid_by_label TEXT;

COMMENT ON COLUMN finance.business_expense_context.paid_by_label IS
  'Display label for who paid (e.g. You, teammate name). Not a FK.';
