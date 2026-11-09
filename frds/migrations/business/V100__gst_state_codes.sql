BEGIN;

-- Canonical GST state codes.
--
-- `preferences->>'gstState'` and the three state columns on `business.operating_master` were
-- free text. GST splits a supply into CGST+SGST or IGST by comparing the supplier's state with
-- the place of supply, and the comparison was a case-insensitive string equality — so `"MH"`
-- against `"Maharashtra"` read as two different states and charged IGST on an intra-state sale.
--
-- This migration rewrites every known spelling to the canonical state name and stores the
-- two-digit code alongside it, so jurisdiction is a code comparison from here on.
--
-- An unrecognised value is left exactly as it was. Nulling it would destroy a real (if
-- misspelled) state and a stale string is the safer failure: `resolveJurisdiction` still falls
-- back to a name compare for it.

-- Spellings that appear in existing rows: names in mixed case, two-letter postal codes, city
-- names that leaked into a free-text field, and the pre-merger Daman & Diu code 25.
CREATE TEMP TABLE gst_state_map (
  spelling TEXT PRIMARY KEY,
  code TEXT NOT NULL,
  name TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO gst_state_map (spelling, code, name) VALUES
  ('01', '01', 'Jammu & Kashmir'),
  ('02', '02', 'Himachal Pradesh'),
  ('03', '03', 'Punjab'),
  ('04', '04', 'Chandigarh'),
  ('05', '05', 'Uttarakhand'),
  ('06', '06', 'Haryana'),
  ('07', '07', 'Delhi'),
  ('08', '08', 'Rajasthan'),
  ('09', '09', 'Uttar Pradesh'),
  ('10', '10', 'Bihar'),
  ('11', '11', 'Sikkim'),
  ('12', '12', 'Arunachal Pradesh'),
  ('13', '13', 'Nagaland'),
  ('14', '14', 'Manipur'),
  ('15', '15', 'Mizoram'),
  ('16', '16', 'Tripura'),
  ('17', '17', 'Meghalaya'),
  ('18', '18', 'Assam'),
  ('19', '19', 'West Bengal'),
  ('20', '20', 'Jharkhand'),
  ('21', '21', 'Odisha'),
  ('22', '22', 'Chhattisgarh'),
  ('23', '23', 'Madhya Pradesh'),
  ('24', '24', 'Gujarat'),
  ('26', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('27', '27', 'Maharashtra'),
  ('29', '29', 'Karnataka'),
  ('30', '30', 'Goa'),
  ('31', '31', 'Lakshadweep'),
  ('32', '32', 'Kerala'),
  ('33', '33', 'Tamil Nadu'),
  ('34', '34', 'Puducherry'),
  ('35', '35', 'Andaman & Nicobar Islands'),
  ('36', '36', 'Telangana'),
  ('37', '37', 'Andhra Pradesh'),
  ('38', '38', 'Ladakh'),
  ('97', '97', 'Other Territory'),
  ('jammu & kashmir', '01', 'Jammu & Kashmir'),
  ('jammu and kashmir', '01', 'Jammu & Kashmir'),
  ('jammu', '01', 'Jammu & Kashmir'),
  ('kashmir', '01', 'Jammu & Kashmir'),
  ('jk', '01', 'Jammu & Kashmir'),
  ('j&k', '01', 'Jammu & Kashmir'),
  ('himachal pradesh', '02', 'Himachal Pradesh'),
  ('himachal', '02', 'Himachal Pradesh'),
  ('hp', '02', 'Himachal Pradesh'),
  ('punjab', '03', 'Punjab'),
  ('pb', '03', 'Punjab'),
  ('chandigarh', '04', 'Chandigarh'),
  ('ch', '04', 'Chandigarh'),
  ('uttarakhand', '05', 'Uttarakhand'),
  ('uttaranchal', '05', 'Uttarakhand'),
  ('uk', '05', 'Uttarakhand'),
  ('haryana', '06', 'Haryana'),
  ('hr', '06', 'Haryana'),
  ('gurgaon', '06', 'Haryana'),
  ('gurugram', '06', 'Haryana'),
  ('delhi', '07', 'Delhi'),
  ('new delhi', '07', 'Delhi'),
  ('nct of delhi', '07', 'Delhi'),
  ('nct', '07', 'Delhi'),
  ('dl', '07', 'Delhi'),
  ('ncr', '07', 'Delhi'),
  ('rajasthan', '08', 'Rajasthan'),
  ('rj', '08', 'Rajasthan'),
  ('uttar pradesh', '09', 'Uttar Pradesh'),
  ('up', '09', 'Uttar Pradesh'),
  ('noida', '09', 'Uttar Pradesh'),
  ('bihar', '10', 'Bihar'),
  ('br', '10', 'Bihar'),
  ('sikkim', '11', 'Sikkim'),
  ('sk', '11', 'Sikkim'),
  ('arunachal pradesh', '12', 'Arunachal Pradesh'),
  ('arunachal', '12', 'Arunachal Pradesh'),
  ('ar', '12', 'Arunachal Pradesh'),
  ('nagaland', '13', 'Nagaland'),
  ('nl', '13', 'Nagaland'),
  ('manipur', '14', 'Manipur'),
  ('mn', '14', 'Manipur'),
  ('mizoram', '15', 'Mizoram'),
  ('mz', '15', 'Mizoram'),
  ('tripura', '16', 'Tripura'),
  ('tr', '16', 'Tripura'),
  ('meghalaya', '17', 'Meghalaya'),
  ('ml', '17', 'Meghalaya'),
  ('assam', '18', 'Assam'),
  ('as', '18', 'Assam'),
  ('west bengal', '19', 'West Bengal'),
  ('wb', '19', 'West Bengal'),
  ('calcutta', '19', 'West Bengal'),
  ('kolkata', '19', 'West Bengal'),
  ('jharkhand', '20', 'Jharkhand'),
  ('jh', '20', 'Jharkhand'),
  ('odisha', '21', 'Odisha'),
  ('orissa', '21', 'Odisha'),
  ('or', '21', 'Odisha'),
  ('chhattisgarh', '22', 'Chhattisgarh'),
  ('chattisgarh', '22', 'Chhattisgarh'),
  ('cg', '22', 'Chhattisgarh'),
  ('madhya pradesh', '23', 'Madhya Pradesh'),
  ('mp', '23', 'Madhya Pradesh'),
  ('gujarat', '24', 'Gujarat'),
  ('gj', '24', 'Gujarat'),
  ('dadra & nagar haveli and daman & diu', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('dadra and nagar haveli and daman and diu', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('dadra and nagar haveli', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('dadra', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('nagar haveli', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('daman', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('diu', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('daman and diu', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('daman & diu', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('dd', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('dn', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  -- The retired Daman & Diu code was merged into 26.
  ('25', '26', 'Dadra & Nagar Haveli and Daman & Diu'),
  ('maharashtra', '27', 'Maharashtra'),
  ('mh', '27', 'Maharashtra'),
  ('bombay', '27', 'Maharashtra'),
  ('mumbai', '27', 'Maharashtra'),
  ('pune', '27', 'Maharashtra'),
  ('karnataka', '29', 'Karnataka'),
  ('ka', '29', 'Karnataka'),
  ('bangalore', '29', 'Karnataka'),
  ('bengaluru', '29', 'Karnataka'),
  ('mysore', '29', 'Karnataka'),
  ('mysuru', '29', 'Karnataka'),
  ('goa', '30', 'Goa'),
  ('ga', '30', 'Goa'),
  ('lakshadweep', '31', 'Lakshadweep'),
  ('ld', '31', 'Lakshadweep'),
  ('kerala', '32', 'Kerala'),
  ('kl', '32', 'Kerala'),
  ('cochin', '32', 'Kerala'),
  ('ernakulam', '32', 'Kerala'),
  ('trivandrum', '32', 'Kerala'),
  ('tamil nadu', '33', 'Tamil Nadu'),
  ('tamilnadu', '33', 'Tamil Nadu'),
  ('tn', '33', 'Tamil Nadu'),
  ('chennai', '33', 'Tamil Nadu'),
  ('puducherry', '34', 'Puducherry'),
  ('pondicherry', '34', 'Puducherry'),
  ('py', '34', 'Puducherry'),
  ('andaman & nicobar islands', '35', 'Andaman & Nicobar Islands'),
  ('andaman and nicobar islands', '35', 'Andaman & Nicobar Islands'),
  ('andaman and nicobar', '35', 'Andaman & Nicobar Islands'),
  ('andaman', '35', 'Andaman & Nicobar Islands'),
  ('nicobar', '35', 'Andaman & Nicobar Islands'),
  ('telangana', '36', 'Telangana'),
  ('tg', '36', 'Telangana'),
  ('ts', '36', 'Telangana'),
  ('hyderabad', '36', 'Telangana'),
  ('andhra pradesh', '37', 'Andhra Pradesh'),
  ('ap', '37', 'Andhra Pradesh'),
  ('visakhapatnam', '37', 'Andhra Pradesh'),
  ('vizag', '37', 'Andhra Pradesh'),
  ('ladakh', '38', 'Ladakh'),
  ('la', '38', 'Ladakh'),
  ('other territory', '97', 'Other Territory');

-- One spelling → one row. The lookup key is the lowercased, trimmed value, which is how the
-- service normalizes before comparing.
CREATE TEMP TABLE gst_state_lookup ON COMMIT DROP AS
SELECT DISTINCT ON (lower(btrim(spelling))) lower(btrim(spelling)) AS key, code, name
FROM gst_state_map
ORDER BY lower(btrim(spelling)), code;

-- --- business.business_system_setup: preferences->>'gstState' ---

WITH resolved AS (
  SELECT bss.business_system_setup_id,
         l.code,
         l.name
  FROM business.business_system_setup bss
  JOIN gst_state_lookup l
    ON l.key = lower(btrim(bss.preferences->>'gstState'))
  WHERE btrim(coalesce(bss.preferences->>'gstState', '')) <> ''
)
UPDATE business.business_system_setup bss
SET preferences = jsonb_set(
      jsonb_set(bss.preferences, '{gstState}', to_jsonb(resolved.name), true),
      '{gstStateCode}', to_jsonb(resolved.code), true
    ),
    updated_at = now()
FROM resolved
WHERE bss.business_system_setup_id = resolved.business_system_setup_id;

-- A row whose gstState is already canonical only needs the code added.
UPDATE business.business_system_setup bss
SET preferences = jsonb_set(
      bss.preferences,
      '{gstStateCode}',
      to_jsonb(l.code),
      true
    ),
    updated_at = now()
FROM gst_state_lookup l
WHERE btrim(coalesce(bss.preferences->>'gstState', '')) <> ''
  AND l.key = lower(btrim(bss.preferences->>'gstState'))
  AND NOT (bss.preferences ? 'gstStateCode');

-- --- business.operating_master: business_state / billing_state / delivery_state ---

UPDATE business.operating_master m
SET business_state = l.name,
    updated_at = now()
FROM gst_state_lookup l
WHERE btrim(coalesce(m.business_state, '')) <> ''
  AND l.key = lower(btrim(m.business_state))
  AND m.business_state IS DISTINCT FROM l.name;

UPDATE business.operating_master m
SET billing_state = l.name,
    updated_at = now()
FROM gst_state_lookup l
WHERE btrim(coalesce(m.billing_state, '')) <> ''
  AND l.key = lower(btrim(m.billing_state))
  AND m.billing_state IS DISTINCT FROM l.name;

UPDATE business.operating_master m
SET delivery_state = l.name,
    updated_at = now()
FROM gst_state_lookup l
WHERE btrim(coalesce(m.delivery_state, '')) <> ''
  AND l.key = lower(btrim(m.delivery_state))
  AND m.delivery_state IS DISTINCT FROM l.name;

-- Report what could not be resolved, so the values are visible rather than silently dropped.
DO $$
DECLARE
  unresolved_setups INTEGER;
  unresolved_masters INTEGER;
BEGIN
  SELECT count(*) INTO unresolved_setups
  FROM business.business_system_setup bss
  LEFT JOIN gst_state_lookup l
    ON l.key = lower(btrim(bss.preferences->>'gstState'))
  WHERE btrim(coalesce(bss.preferences->>'gstState', '')) <> ''
    AND l.key IS NULL;

  SELECT count(*) INTO unresolved_masters
  FROM business.operating_master m
  LEFT JOIN gst_state_lookup l1
    ON l1.key = lower(btrim(coalesce(m.business_state, '')))
  LEFT JOIN gst_state_lookup l2
    ON l2.key = lower(btrim(coalesce(m.billing_state, '')))
  LEFT JOIN gst_state_lookup l3
    ON l3.key = lower(btrim(coalesce(m.delivery_state, '')))
  WHERE (btrim(coalesce(m.business_state, '')) <> '' AND l1.key IS NULL)
     OR (btrim(coalesce(m.billing_state, '')) <> '' AND l2.key IS NULL)
     OR (btrim(coalesce(m.delivery_state, '')) <> '' AND l3.key IS NULL);

  RAISE NOTICE 'gst state codes: % setup preference rows and % master rows kept an unrecognised state', unresolved_setups, unresolved_masters;
END $$;

COMMIT;
