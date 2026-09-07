-- Business expense category catalogue (Team Ops / Runway / Ops chips + tests).
-- finance.expense.category_code FK → finance.expense_category; personal V045 only seeded
-- FOOD/TRANSPORT/… so SOFTWARE/TRAVEL/PURCHASE inserts failed with fk_expense__category.

INSERT INTO finance.expense_category(category_code, label, sort_order) VALUES
  ('SOFTWARE', 'Software', 100),
  ('TRAVEL', 'Travel', 110),
  ('OFFICE', 'Office', 120),
  ('EQUIPMENT', 'Equipment', 130),
  ('SERVICES', 'Services', 140),
  ('PURCHASE', 'Purchase', 150),
  ('OPS', 'Ops', 160),
  ('SALARIES', 'Salaries', 170),
  ('MARKETING', 'Marketing', 180),
  ('INFRASTRUCTURE', 'Infrastructure', 190),
  ('OPERATIONS_LOGISTICS', 'Operations & Logistics', 200),
  ('SAAS_SOFTWARE', 'SaaS & Software', 210),
  ('SAAS_AND_SOFTWARE', 'SaaS & Software', 211),
  ('PROFESSIONAL_SERVICES', 'Professional Services', 220)
ON CONFLICT (category_code) DO NOTHING;

COMMENT ON TABLE finance.expense_category IS
  'Shared Finance canonical expense category catalogue (personal + business).';
