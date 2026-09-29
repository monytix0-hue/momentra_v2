-- Master expense chips that V045 did not seed. subcategory_code is the primary key,
-- so Cafe coffee cannot reuse FOOD's COFFEE row.

INSERT INTO finance.expense_subcategory(subcategory_code, category_code, label, sort_order) VALUES
  ('FUEL', 'TRANSPORT', 'Fuel', 20),
  ('RIDE', 'TRANSPORT', 'Ride share', 30),
  ('CLOTHING', 'SHOPPING', 'Clothing', 20),
  ('ELECTRONICS', 'SHOPPING', 'Electronics', 30),
  ('CAFE_COFFEE', 'CAFE', 'Coffee', 20),
  ('PHARMACY', 'HEALTH', 'Pharmacy', 20),
  ('FITNESS', 'HEALTH', 'Fitness', 30),
  ('STREAMING', 'ENTERTAINMENT', 'Streaming', 20),
  ('EVENTS', 'ENTERTAINMENT', 'Events', 30),
  ('UTILITIES', 'BILLS', 'Utilities', 20),
  ('RENT', 'BILLS', 'Rent', 30),
  ('HOUSING', 'BILLS', 'Housing', 40),
  ('MISC', 'OTHER', 'Miscellaneous', 20)
ON CONFLICT (subcategory_code) DO NOTHING;
