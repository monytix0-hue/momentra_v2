BEGIN;

-- Phase 7 Personal Analytics pack (IMP-01 + Pulse curation).
-- Window / Evidence / Threshold catalogues + freeze Pulse-surface metric versions.
-- Authority map: docs/implementation/PERSONAL_PHASE7_PULSE_METRIC_MAP.json

CREATE TABLE IF NOT EXISTS analytics.catalogue_window (
    window_code TEXT PRIMARY KEY,
    iso_duration TEXT,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_catalogue_window__code CHECK (window_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_catalogue_window__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE IF NOT EXISTS analytics.catalogue_evidence_gate (
    evidence_gate_code TEXT PRIMARY KEY,
    min_evidence_count INTEGER NOT NULL,
    min_span_days INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_catalogue_evidence__code CHECK (evidence_gate_code ~ '^E[0-9]+$'),
    CONSTRAINT ck_catalogue_evidence__count CHECK (min_evidence_count >= 0),
    CONSTRAINT ck_catalogue_evidence__span CHECK (min_span_days >= 0),
    CONSTRAINT ck_catalogue_evidence__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

CREATE TABLE IF NOT EXISTS analytics.catalogue_threshold_band (
    threshold_set_code TEXT NOT NULL,
    band_code TEXT NOT NULL,
    comparator TEXT NOT NULL,
    lower_value NUMERIC(24,8),
    upper_value NUMERIC(24,8),
    severity TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (threshold_set_code, band_code),
    CONSTRAINT ck_catalogue_threshold_band__set CHECK (threshold_set_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_catalogue_threshold_band__band CHECK (band_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_catalogue_threshold_band__cmp CHECK (comparator IN ('LT','LTE','EQ','GTE','GT','BETWEEN','CATEGORY_EQ')),
    CONSTRAINT ck_catalogue_threshold_band__sev CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_catalogue_threshold_band__status CHECK (status IN ('ACTIVE','INACTIVE','RETIRED'))
);

COMMENT ON TABLE analytics.catalogue_window IS 'Phase 7 Window Config seed (IMP-01).';
COMMENT ON TABLE analytics.catalogue_evidence_gate IS 'Phase 7 Evidence Config seed (IMP-01).';
COMMENT ON TABLE analytics.catalogue_threshold_band IS 'Phase 7 Threshold Config seed (IMP-01).';

INSERT INTO analytics.catalogue_window (window_code, iso_duration, description) VALUES
  ('TODAY', 'P1D', 'Calendar day as-of'),
  ('ROLLING_7D', 'P7D', 'Rolling 7 days'),
  ('ROLLING_30D', 'P30D', 'Rolling 30 days'),
  ('ROLLING_90D', 'P90D', 'Rolling 90 days'),
  ('LIFETIME', NULL, 'All eligible history'),
  ('CURRENT_VS_PRIOR_7D', 'P7D', 'Current 7d vs prior 7d')
ON CONFLICT (window_code) DO UPDATE SET
  iso_duration = EXCLUDED.iso_duration,
  description = EXCLUDED.description,
  status = 'ACTIVE';

INSERT INTO analytics.catalogue_evidence_gate (evidence_gate_code, min_evidence_count, min_span_days, description) VALUES
  ('E1', 1, 0, 'Single eligible observation'),
  ('E2', 3, 7, 'Light rolling evidence'),
  ('E3', 5, 14, 'Standard Pulse evidence'),
  ('E5', 8, 21, 'Pattern confidence floor'),
  ('E7', 20, 60, 'Longitudinal memory floor')
ON CONFLICT (evidence_gate_code) DO UPDATE SET
  min_evidence_count = EXCLUDED.min_evidence_count,
  min_span_days = EXCLUDED.min_span_days,
  description = EXCLUDED.description,
  status = 'ACTIVE';

INSERT INTO analytics.catalogue_threshold_band (threshold_set_code, band_code, comparator, lower_value, upper_value, severity, sort_order) VALUES
  ('SCORE_0_100', 'CRITICAL', 'BETWEEN', 0, 39.999, 'CRITICAL', 1),
  ('SCORE_0_100', 'LOW', 'BETWEEN', 40, 59.999, 'MEDIUM', 2),
  ('SCORE_0_100', 'MODERATE', 'BETWEEN', 60, 74.999, 'LOW', 3),
  ('SCORE_0_100', 'GOOD', 'BETWEEN', 75, 89.999, 'INFO', 4),
  ('SCORE_0_100', 'EXCELLENT', 'BETWEEN', 90, 100, 'INFO', 5),
  ('HEALTH_0_100', 'CRITICAL', 'BETWEEN', 0, 39.999, 'CRITICAL', 1),
  ('HEALTH_0_100', 'LOW', 'BETWEEN', 40, 59.999, 'MEDIUM', 2),
  ('HEALTH_0_100', 'MODERATE', 'BETWEEN', 60, 74.999, 'LOW', 3),
  ('HEALTH_0_100', 'GOOD', 'BETWEEN', 75, 89.999, 'INFO', 4),
  ('HEALTH_0_100', 'EXCELLENT', 'BETWEEN', 90, 100, 'INFO', 5)
ON CONFLICT (threshold_set_code, band_code) DO UPDATE SET
  comparator = EXCLUDED.comparator,
  lower_value = EXCLUDED.lower_value,
  upper_value = EXCLUDED.upper_value,
  severity = EXCLUDED.severity,
  sort_order = EXCLUDED.sort_order,
  status = 'ACTIVE';

-- New Pulse-axis metric definitions
INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES
  ('75800001-0001-4000-8000-000000000001'::uuid, 'FUTURE_VISION_SCORE', 'Future Vision Score', 'Future Building Vision axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000002'::uuid, 'FUTURE_GROWTH_SCORE', 'Future Growth Score', 'Future Building Growth axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000003'::uuid, 'FUTURE_MOMENTUM_SCORE', 'Future Momentum Score', 'Future Building Momentum axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000004'::uuid, 'FUTURE_DISCIPLINE_SCORE', 'Future Discipline Score', 'Future Building Discipline axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000005'::uuid, 'LIFESTYLE_JOY_SCORE', 'Lifestyle Joy Score', 'Lifestyle Joy axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000006'::uuid, 'LIFESTYLE_FULFILLMENT_SCORE', 'Lifestyle Fulfillment Score', 'Lifestyle Fulfillment axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000007'::uuid, 'LIFESTYLE_VITALITY_SCORE', 'Lifestyle Vitality Score', 'Lifestyle Vitality axis / Vitality Index.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000008'::uuid, 'LIFESTYLE_EXPLORATION_SCORE', 'Lifestyle Exploration Score', 'Lifestyle Exploration axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-000000000009'::uuid, 'RELATIONSHIP_TRUST_SCORE', 'Relationship Trust Score', 'Relationships Trust axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-00000000000a'::uuid, 'RELATIONSHIP_CARE_SCORE', 'Relationship Care Score', 'Relationships Care axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-00000000000b'::uuid, 'RELATIONSHIP_SUPPORT_SCORE', 'Relationship Support Score', 'Relationships Support axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE'),
  ('75800001-0001-4000-8000-00000000000c'::uuid, 'RELATIONSHIP_PRESENCE_SCORE', 'Relationship Presence Score', 'Relationships Presence axis from precision writers.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Retire prior ACTIVE versions before inserting Phase 7 ACTIVE rows (uq_metric_version__active).
UPDATE analytics.metric_version mv
SET status = 'RETIRED'
WHERE mv.status = 'ACTIVE'
  AND mv.metric_definition_id IN (
    SELECT metric_definition_id FROM analytics.metric_definition
    WHERE code IN (
      'RECOVERY_SCORE', 'WELLBEING_STATE', 'RHYTHM_CONSISTENCY', 'BUDGET_UTILIZATION',
      'RELATIONSHIP_INVESTMENT',
      'FUTURE_VISION_SCORE', 'FUTURE_GROWTH_SCORE', 'FUTURE_MOMENTUM_SCORE', 'FUTURE_DISCIPLINE_SCORE',
      'LIFESTYLE_JOY_SCORE', 'LIFESTYLE_FULFILLMENT_SCORE', 'LIFESTYLE_VITALITY_SCORE', 'LIFESTYLE_EXPLORATION_SCORE',
      'RELATIONSHIP_TRUST_SCORE', 'RELATIONSHIP_CARE_SCORE', 'RELATIONSHIP_SUPPORT_SCORE', 'RELATIONSHIP_PRESENCE_SCORE'
    )
  )
  AND COALESCE(mv.formula_definition->>'phase7', '') <> 'true';

-- Freeze ACTIVE version with Phase 7 formula contract + time_window + evidence.
-- Existing Pulse codes (V022 UUIDs) get next version_number; new codes get version 1.

-- RECOVERY_SCORE phase7
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT
  '75800002-0001-4000-8000-000000000001'::uuid,
  d.metric_definition_id,
  COALESCE((SELECT MAX(v.version_number) FROM analytics.metric_version v WHERE v.metric_definition_id = d.metric_definition_id), 0) + 1,
  'COMPOSITE',
  '{"phase7":true,"source":"projection.personal_pulse.recovery_score","pmetCodes":["PERSONAL.FUTURE_BUILDING.HELPING_HURTING"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2","semantic_contract":"Life Ops Recovery axis; synced from personal_pulse by analytics DET."}'::jsonb,
  'NO_RESULT',
  '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb,
  3,
  'ACTIVE'
FROM analytics.metric_definition d WHERE d.code = 'RECOVERY_SCORE'
  AND NOT EXISTS (
    SELECT 1 FROM analytics.metric_version mv
    WHERE mv.metric_definition_id = d.metric_definition_id
      AND mv.formula_definition->>'phase7' = 'true'
  );

-- WELLBEING_STATE v2 (numeric score stored in metric_current.numeric_value)
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT
  '75800002-0001-4000-8000-000000000002'::uuid,
  d.metric_definition_id,
  COALESCE((SELECT MAX(v.version_number) FROM analytics.metric_version v WHERE v.metric_definition_id = d.metric_definition_id), 0) + 1,
  'COMPOSITE',
  '{"phase7":true,"source":"projection.personal_pulse.wellbeing_score","pmetCodes":["PERSONAL.FUTURE_BUILDING.FUTURE_SCORE"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2","semantic_contract":"Pulse hero / composite wellbeing; synced from personal_pulse."}'::jsonb,
  'NO_RESULT',
  '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb,
  3,
  'ACTIVE'
FROM analytics.metric_definition d WHERE d.code = 'WELLBEING_STATE'
  AND NOT EXISTS (
    SELECT 1 FROM analytics.metric_version mv
    WHERE mv.metric_definition_id = d.metric_definition_id
      AND mv.formula_definition->>'phase7' = 'true'
  );

-- RHYTHM_CONSISTENCY v2
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT
  '75800002-0001-4000-8000-000000000003'::uuid,
  d.metric_definition_id,
  COALESCE((SELECT MAX(v.version_number) FROM analytics.metric_version v WHERE v.metric_definition_id = d.metric_definition_id), 0) + 1,
  'COMPOSITE',
  '{"phase7":true,"source":"projection.personal_pulse.rhythm_score","pmetCodes":["PERSONAL.FUTURE_BUILDING.DISCIPLINE"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2","semantic_contract":"Rhythm / discipline column; synced from personal_pulse."}'::jsonb,
  'NO_RESULT',
  '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb,
  3,
  'ACTIVE'
FROM analytics.metric_definition d WHERE d.code = 'RHYTHM_CONSISTENCY'
  AND NOT EXISTS (
    SELECT 1 FROM analytics.metric_version mv
    WHERE mv.metric_definition_id = d.metric_definition_id
      AND mv.formula_definition->>'phase7' = 'true'
  );

-- BUDGET_UTILIZATION v2 (spend snapshot)
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT
  '75800002-0001-4000-8000-000000000004'::uuid,
  d.metric_definition_id,
  COALESCE((SELECT MAX(v.version_number) FROM analytics.metric_version v WHERE v.metric_definition_id = d.metric_definition_id), 0) + 1,
  'COMPOSITE',
  '{"phase7":true,"source":"finance.spend_window","pmetCodes":["PERSONAL.FUTURE_BUILDING.INVESTMENT_SNAPSHOT"],"thresholdSet":null,"evidenceGate":"E1","semantic_contract":"Money snapshot spend proxy for Pulse."}'::jsonb,
  'NO_RESULT',
  '{"windowCode":"ROLLING_30D","isoDuration":"P30D"}'::jsonb,
  1,
  'ACTIVE'
FROM analytics.metric_definition d WHERE d.code = 'BUDGET_UTILIZATION'
  AND NOT EXISTS (
    SELECT 1 FROM analytics.metric_version mv
    WHERE mv.metric_definition_id = d.metric_definition_id
      AND mv.formula_definition->>'phase7' = 'true'
  );

-- RELATIONSHIP_INVESTMENT v2 (bond index)
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT
  '75800002-0001-4000-8000-000000000005'::uuid,
  d.metric_definition_id,
  COALESCE((SELECT MAX(v.version_number) FROM analytics.metric_version v WHERE v.metric_definition_id = d.metric_definition_id), 0) + 1,
  'COMPOSITE',
  '{"phase7":true,"source":"widget_payload.bondIndex","pmetCodes":["PERSONAL.RELATIONSHIPS.BOND_INDEX"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2","semantic_contract":"Relationships Bond Index; synced from personal_pulse.widget_payload."}'::jsonb,
  'NO_RESULT',
  '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb,
  3,
  'ACTIVE'
FROM analytics.metric_definition d WHERE d.code = 'RELATIONSHIP_INVESTMENT'
  AND NOT EXISTS (
    SELECT 1 FROM analytics.metric_version mv
    WHERE mv.metric_definition_id = d.metric_definition_id
      AND mv.formula_definition->>'phase7' = 'true'
  );

-- New axis metrics: version 1 each
INSERT INTO analytics.metric_version (
  metric_version_id, metric_definition_id, version_number, formula_type, formula_definition,
  null_behavior, time_window_definition, minimum_evidence_count, status
)
SELECT v.metric_version_id, d.metric_definition_id, 1, 'COMPOSITE', v.formula_definition,
       'NO_RESULT', v.time_window_definition, 3, 'ACTIVE'
FROM analytics.metric_definition d
JOIN (VALUES
  ('FUTURE_VISION_SCORE', '75800002-0001-4000-8000-000000000011'::uuid,
   '{"phase7":true,"source":"widget_payload.visionScore","pmetCodes":["PERSONAL.FUTURE_BUILDING.CAREER_RISING"],"thresholdSet":"HEALTH_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('FUTURE_GROWTH_SCORE', '75800002-0001-4000-8000-000000000012'::uuid,
   '{"phase7":true,"source":"widget_payload.growthScore","pmetCodes":["PERSONAL.FUTURE_BUILDING.GROWTH_74"],"thresholdSet":"HEALTH_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('FUTURE_MOMENTUM_SCORE', '75800002-0001-4000-8000-000000000013'::uuid,
   '{"phase7":true,"source":"widget_payload.momentumScore","pmetCodes":["PERSONAL.FUTURE_BUILDING.MOMENTUM"],"thresholdSet":"SCORE_0_100","evidenceGate":"E3"}'::jsonb,
   '{"windowCode":"CURRENT_VS_PRIOR_7D","isoDuration":"P7D"}'::jsonb),
  ('FUTURE_DISCIPLINE_SCORE', '75800002-0001-4000-8000-000000000014'::uuid,
   '{"phase7":true,"source":"widget_payload.disciplineScore","pmetCodes":["PERSONAL.FUTURE_BUILDING.DISCIPLINE"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('LIFESTYLE_JOY_SCORE', '75800002-0001-4000-8000-000000000015'::uuid,
   '{"phase7":true,"source":"widget_payload.joyScore","pmetCodes":["PERSONAL.LIFESTYLE.JOY"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('LIFESTYLE_FULFILLMENT_SCORE', '75800002-0001-4000-8000-000000000016'::uuid,
   '{"phase7":true,"source":"widget_payload.fulfillmentScore","pmetCodes":["PERSONAL.LIFESTYLE.FULFILLMENT"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('LIFESTYLE_VITALITY_SCORE', '75800002-0001-4000-8000-000000000017'::uuid,
   '{"phase7":true,"source":"widget_payload.vitalityScore","pmetCodes":["PERSONAL.LIFESTYLE.VITALITY"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('LIFESTYLE_EXPLORATION_SCORE', '75800002-0001-4000-8000-000000000018'::uuid,
   '{"phase7":true,"source":"widget_payload.explorationScore","pmetCodes":["PERSONAL.LIFESTYLE.EXPLORATION"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('RELATIONSHIP_TRUST_SCORE', '75800002-0001-4000-8000-000000000019'::uuid,
   '{"phase7":true,"source":"widget_payload.trustScore","pmetCodes":["PERSONAL.RELATIONSHIPS.TRUST"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('RELATIONSHIP_CARE_SCORE', '75800002-0001-4000-8000-00000000001a'::uuid,
   '{"phase7":true,"source":"widget_payload.careScore","pmetCodes":["PERSONAL.RELATIONSHIPS.CARE"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('RELATIONSHIP_SUPPORT_SCORE', '75800002-0001-4000-8000-00000000001b'::uuid,
   '{"phase7":true,"source":"widget_payload.supportScore","pmetCodes":["PERSONAL.RELATIONSHIPS.SUPPORT"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb),
  ('RELATIONSHIP_PRESENCE_SCORE', '75800002-0001-4000-8000-00000000001c'::uuid,
   '{"phase7":true,"source":"widget_payload.presenceScore","pmetCodes":["PERSONAL.RELATIONSHIPS.PRESENCE"],"thresholdSet":"SCORE_0_100","evidenceGate":"E2"}'::jsonb,
   '{"windowCode":"ROLLING_7D","isoDuration":"P7D"}'::jsonb)
) AS v(code, metric_version_id, formula_definition, time_window_definition)
  ON d.code = v.code
WHERE NOT EXISTS (
  SELECT 1 FROM analytics.metric_version mv WHERE mv.metric_version_id = v.metric_version_id
);

-- Attach SCORE_0_100 / HEALTH_0_100 threshold_definition rows to phase7 ACTIVE versions.
INSERT INTO analytics.threshold_definition (
  threshold_definition_id, metric_version_id, code, comparator, lower_value, upper_value,
  severity, creates_attention, creates_deterministic_insight, message_template, sort_order, status
)
SELECT
  md5(mv.metric_version_id::text || ':' || b.band_code)::uuid,
  mv.metric_version_id,
  b.band_code,
  b.comparator,
  b.lower_value,
  b.upper_value,
  b.severity,
  false,
  false,
  'Phase 7 ' || b.threshold_set_code || ' band ' || b.band_code,
  b.sort_order,
  'ACTIVE'
FROM analytics.metric_version mv
JOIN analytics.catalogue_threshold_band b
  ON b.threshold_set_code = COALESCE(mv.formula_definition->>'thresholdSet', 'SCORE_0_100')
 AND b.status = 'ACTIVE'
WHERE mv.formula_definition->>'phase7' = 'true'
  AND mv.status = 'ACTIVE'
  AND mv.formula_definition->>'thresholdSet' IS NOT NULL
ON CONFLICT (metric_version_id, code) DO NOTHING;

COMMIT;
